package ccr.reduction;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Vector;

import lpsolve.LpSolve;
import lpsolve.LpSolveException;
import ccr.test.Logger;
import ccr.test.TestCase;
import ccr.test.TestSet;

/**
 * then construct the reduced test suites from the ILP solutions
 * 
 * @author hwang
 *
 */
public class ILPSolver {
	
	private static ArrayList<TestCase> getStatisticsOfTestCase(String testcaseFile, boolean containHeader){		
		ArrayList<TestCase> tcArray = new ArrayList<TestCase>();
		
		try {
			
			BufferedReader br = new BufferedReader(new FileReader(testcaseFile));
			String str = null;
			int rowNo = 0;
			if(containHeader){
				rowNo = br.readLine().split("\t").length - 6;
			}
			
//			StringBuilder infoRecorder = new StringBuilder();
			while((str=br.readLine())!= null){
				tcArray.add(new TestCase(str, true));				
			}
			br.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return tcArray;
	}
	
	public static String loadCoverageConstraints(String constraintsFile, boolean containHeader){
		File tmp = new File(constraintsFile);
		StringBuilder sb = new StringBuilder();
		if(tmp.exists()){
			try {
				BufferedReader br = new BufferedReader(new FileReader(constraintsFile));
				if(containHeader)
					br.readLine();
				
				String str = null;
				while((str = br.readLine())!= null){
					sb.append(str).append("\n");
				}
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}else{
			System.out.println("The file:" + tmp.getName() + " doen not exist!");
		}
		return sb.toString();
	}
	
	public static String buildCoverageConstraints(double alpha, ArrayList<TestCase> tcArray, 
			String infoFile, String constraintFile){
		
		int rowNo = tcArray.get(0).hitSet.size();
		StringBuilder infoRecorder = new StringBuilder();
		
		
		infoRecorder.append("--------------------------------------------------------------------------").append("\n");		
		infoRecorder.append("Weight factor: " + alpha).append("\n");
		infoRecorder.append("Before dimension reduction:").append("\n");
		infoRecorder.append("ProgramElement:").append(rowNo).append("\n");
		infoRecorder.append("TestCase:").append(tcArray.size()).append("\n");
		
		Vector<Integer[]> constraints = new Vector<Integer[]>();		
		int infeasible_constraint_counter = 0;
		int equivalent_constraint_counter = 0;
		StringBuffer sb_constraints = new StringBuffer();
		
		for(int i = 0; i < rowNo; i ++){ //for each constraint of a program element				
			Integer[] constraint = new Integer[tcArray.size()];
			for(int j = 0; j < constraint.length; j ++){
				ArrayList<Integer> hitSet= tcArray.get(j).hitSet; 
				constraint[j] = hitSet.get(i); // we list all coverage of test cases with respect to each program element(constraint)  
			}
			
			//eliminate all infeasible program elements				
			int sumHit = 0;
			for(int j = 0; j < constraint.length; j++){
				sumHit += constraint[j];
			}
			if(sumHit != 0){ //a program element is infeasible if no test case can cover it
				//2009-12-20: note that we should remove any equivalent constraints here
				if(!isMemberOf(constraints, constraint)){
					constraints.add(constraint);	
				}else{
					equivalent_constraint_counter ++;
				}
			}else{
				infeasible_constraint_counter ++;
			}
		}
		
		infoRecorder.append("After dimension reduction:").append("\n");			
		//2010-01-27:the number of constraints is (constraints.size + 1)
		//rather than (constraints.size()) 
		infoRecorder.append("ProgramElement:").append(constraints.size() + 1).
			append("(").append(rowNo + 1).append(" exclude infeasible:").append(infeasible_constraint_counter).append(",").
			append("equivalent:").append(equivalent_constraint_counter).append(")").append("\n");
		infoRecorder.append("TestCase:").append(tcArray.size()).append("\n");
		infoRecorder.append("--------------------------------------------------------------------------").append("\n");
		
		//save all into (coverage matrix of all test cases with respect to program elements)
		for(int i = 0; i < constraints.size(); i ++){
			Integer[] constraint = constraints.get(i);
			for(int j = 0; j < constraint.length; j ++){
				//2010-01-27: ignore the constraint when the coefficient is 0
				if(constraint[j]!=0){
					sb_constraints.append(" + " + constraint[j] + " x" + (j+1));	
				}
			}
			sb_constraints.append(" >= 1;\n");
		}
		sb_constraints.append("\n");
		
		sb_constraints.append("bin "); //specify the binary variables			
		for(int i = 0; i < tcArray.size()-1; i ++){
			sb_constraints.append("x"+ (i + 1) + ", ");
		}
		sb_constraints.append("x" + (tcArray.size()) +";");
		
		String constraintString = sb_constraints.toString();
		//save the constraint file
		Logger.getInstance().setPath(constraintFile, false);
		Logger.getInstance().write(constraintString);
		Logger.getInstance().close();
		
		//save the information file
		Logger.getInstance().setPath(infoFile, false);
		Logger.getInstance().write(infoRecorder.toString());
		Logger.getInstance().close();
		
		return constraintString;
	}
	
	/**2009-12-18:create a LP file from existing info files,
	 * then save this LP file. For a bi-criteria ILP model, 
	 * the objective function considers both coverage and Context
	 * Diversity of each test case. This method returns all test
	 * cases in the test pool
	 * 
	 * @param testcaseFile: the file to load test cases
	 * @param containHeader: indicate whether there is any headers of the test case file
	 * @param alpha: weighting factors between test suite size and context diversity
	 * @param modelFile: the file to save constructed ILP model
	 * @param infoFile: the file to save all info generated during constructing the model
	 * @param maxSize: the upper bound of the reduced test suite size
	 * @return
	 */
	public static ArrayList<TestCase> buildILPModel_biCriteria(String testcaseFile, boolean containHeader, 
			double alpha, String modelFile, String infoFile, int maxSize){
		
			ArrayList<TestCase> tcArray = null; 
			//1. read info of all test cases
			tcArray = getStatisticsOfTestCase(testcaseFile, containHeader);
			int rowNo = tcArray.get(0).hitSet.size();
			
			//2.create LP file: convert program element(row) * testcases(column) 
			//into the matrix of testcases(row)* program elements(column)
			StringBuilder sb = new StringBuilder();
			
			//2.1. build the objective function
			sb.append("min:");			
			for(int i = 0; i < tcArray.size(); i ++){
				//2009-12-20: use CR rather than CD for scaling purpose
				TestCase tc = tcArray.get(i);		
				double CR = tc.CI/Double.parseDouble(tc.length);
				double weight = alpha - (1-alpha)*CR;
				sb.append(" + " + new DecimalFormat("0.0000").format(weight) + " x"+ (i + 1));
			}
			sb.append(";\n");

			//2009-12-21: 2.2. size constraint: it specifies that the upper bound 
			//of reduced test suite size
			for(int i = 0; i < tcArray.size(); i ++){				
				sb.append(" + x" + (i+1));
			}
			sb.append(" <= " + maxSize + ";\n");

			//2010-01-27: check whether the constraint file exists or not
			String coverageConstraints = null;
			File constraintsFile = new File(new File(modelFile).getParent() + "/Constraints.txt");
			if(constraintsFile.exists()){	
				//2.3: load the coverage constraints
				coverageConstraints = loadCoverageConstraints(constraintsFile.getPath(), false);
			}else{
				//2.create constraints if the constraints file does not exist, 								 								
				coverageConstraints = buildCoverageConstraints(alpha, tcArray, infoFile, constraintsFile.getPath());
			}
			sb.append(coverageConstraints);
			
			Logger.getInstance().setPath(modelFile, false);
			Logger.getInstance().write(sb.toString());
			Logger.getInstance().close();	
		
		return tcArray;
	}

	/**2009-12-21:solve the ILP model with LPSolve and
	 * save the results into a file. This method returns
	 * the reduced test suite.
	 * @param modelFile: the file to load the ILP model 
	 * @param tcArray: the array of all-used test cases(used to map from variables to test case index ) 
	 * @param infoFile: the file to save info collected during solving the model
	 * @return
	 */
	public static TestSet solveILPModel(String modelFile, ArrayList<TestCase> tcArray, String infoFile){
		
		TestSet testSet = new TestSet();
		ArrayList<String> selectedTestCases = new ArrayList<String>();
		
		StringBuilder infoRecorder = new StringBuilder();
		try {						
			LpSolve solver = LpSolve.readLp(modelFile, LpSolve.CRITICAL, null);

			long startTime = System.currentTimeMillis();
			int ret = solver.solve();
			long exeTime = System.currentTimeMillis() - startTime;
			infoRecorder.append("Execute Time:").append(exeTime/(double)1000).append(" s\n");
			
			if(ret == LpSolve.OPTIMAL)
				ret = 0;
			else
				ret = 5;
			
			if(ret == 0){ //solve the problem successfully				
				double[] var = solver.getPtrVariables();
				for(int i = 0; i < var.length; i ++){
					if(var[i]==1){
						String index = tcArray.get(i).index;
						selectedTestCases.add(index);
						
					}
				}
				
				infoRecorder.append("Reduced Test Suite Size:").append(selectedTestCases.size()).append("\n");
				infoRecorder.append("Selected Test Cases:").append("\n");
				for(int i = 0; i < selectedTestCases.size(); i ++){
					infoRecorder.append(selectedTestCases.get(i) + ",");
				}
				
			}else{
				infoRecorder.append("cannot get the solution");
			}
			
			Logger.getInstance().setPath(infoFile, true);//append execution info into the file
			Logger.getInstance().write(infoRecorder.toString());
			Logger.getInstance().close();

		} catch (LpSolveException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
		
		testSet.testcases = selectedTestCases;
		return testSet;
	}
	
	/**2009-12-21: build multiple ILP models with respect to different weighting factors
	 * 
	 * @param date: the directory to get the test case statistics 
	 * @param criterion: the testing criterion
	 * @param maxSize: the upper bound of the reduced test suite
	 * @return
	 */
	public static ArrayList<TestCase> buildILPModels(String date, String criterion, int maxSize){
		
		ArrayList<TestCase> testSet = null;
		boolean containHeader = true;
		String testcaseFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
			+ date + "/ILPModel/"+ criterion + "/TestCaseStatistics_" + criterion + ".txt";
		File tmp = new File(testcaseFile);
		
		//2010-01-27: create the test pool statistics when it is unavailable
		if(!tmp.exists()){
			String appClassName = "TestCFG2_ins";			
			TestSuiteReduction.getStatisticsOfTestPool(appClassName, date, criterion);			
		}
		
		DecimalFormat format = new DecimalFormat("0.0");
	
		for(double alpha = 0.0; alpha <=1.0; alpha = alpha + 0.1){
			String alpha_str = format.format(alpha);
			String modelFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
				+ date +"/ILPModel/"+ criterion +"/Model_" + criterion + "_" + alpha_str + "_" + maxSize +".lp";
			String infoFile =  "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
				+ date +"/ILPModel/"+ criterion + "/Result_" + criterion + "_" + alpha_str +"_" + maxSize + ".txt";			
			testSet = buildILPModel_biCriteria(testcaseFile, containHeader, alpha, modelFile, infoFile, maxSize);
		}
		
		return testSet;
	}
	
	
	/**2009-12-21: solve the ILP models with respect to different weighting factors
	 * 
	 * @param date:the directory to get the test case statistics
	 * @param criterion: the testing criterion
	 * @param tcArray: the list of test cases
	 * @param maxSize: the size constraint
	 * @return
	 */
	public static HashMap<Double, TestSet> solveILPModels(String date, String criterion,
			ArrayList<TestCase> tcArray, int maxSize){
		
		HashMap<Double, TestSet> alpha_testSet = new HashMap<Double, TestSet>();
				
		DecimalFormat format = new DecimalFormat("0.0");
		
		for(double alpha = 0.0; alpha <=1.0; alpha = alpha + 0.1){
			String alpha_str = format.format(alpha);
			String modelFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
				+ date +"/ILPModel/"+ criterion +"/Model_" + criterion + "_" + alpha_str + "_" + maxSize +".lp";
			String infoFile =  "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
				+ date +"/ILPModel/"+ criterion + "/Result_" + criterion + "_" + alpha_str +"_" + maxSize + ".txt";
			
			System.out.println("\nStart to solve the model with weighting factor " + alpha);
			TestSet testSet = solveILPModel(modelFile, tcArray, infoFile);
			System.out.println("Finish to construct the model with weighting factor " + alpha + "\n");
			
			alpha_testSet.put(alpha, testSet);
		}
		
		Double[] keys = alpha_testSet.keySet().toArray(new Double[1]); 
		Arrays.sort(keys);
		
		StringBuilder sb = new StringBuilder();
		sb.append("Weigth").append("\t").append("ReducedTestSuiteSize").append("\n");
		
		for(int i = 0; i < keys.length; i ++){
			sb.append(format.format(keys[i])).append("\t").append(alpha_testSet.get(keys[i]).size()).append("\n");
		}
		System.out.println(sb.toString());
		
		return alpha_testSet;
	}
	
	/**2009-12-21: create and solve multiple binary ILP model 
	 * with respect to different weighting factors. 
	 * This method returns the list of reduced test suite with respect
	 * to different weighting factors.
	 * 
	 * @param date
	 * @param criterion
	 * @param maxSize
	 * @return
	 */
	public static HashMap<Double, TestSet> buildAndSolveILPs(String date, String criterion, int maxSize){
		ArrayList<TestCase> tcArray = buildILPModels(date, criterion, maxSize);
		return solveILPModels(date, criterion, tcArray, maxSize);
	}
		
	/**2010-01-27: judge whether a given constraint is a member of a constraint array.
	 * This method can be used to get an equivalent constraint
	 * @param constraints
	 * @param constraint
	 * @return
	 */
	private static boolean isMemberOf(Vector<Integer[]> constraints, Integer[] constraint){
		boolean isMember = false;
		for(int i = 0; i < constraints.size() && !isMember; i ++){
			Integer[] constraint_temp = constraints.get(i);
			int j = 0;
			for(; j < constraint_temp.length; j ++){
				if(constraint_temp[j]!= constraint[j]){
					break; //break the loop if any element is different
				}
			}
			if(j == constraint_temp.length){//find an equivalent constraint
				isMember = true;
			}
		}
		
		return isMember;
	}
	
	public static void main(String[] args) {
		String instruction = args[0];
		if(instruction.equals("buildILPModel")){
			String date = args[1];
			String criterion = args[2];
			int maxSize = Integer.parseInt(args[3]);
			
			buildILPModels(date, criterion, maxSize);
		}else if(instruction.equals("solveILPModel")){
			String date = args[1];
			String criterion = args[2];
			int maxSize = Integer.parseInt(args[3]);
			
			String testcaseFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
				+ date + "/"+ criterion + "/TestCaseStatistics_" + criterion + ".txt";
			boolean containHeader = true;
			ArrayList<TestCase> tcArray = getStatisticsOfTestCase(testcaseFile, containHeader);
			
			solveILPModels(date, criterion, tcArray, maxSize);
		}else if(instruction.equals("buildAndSolveILPModel")){
			String date = args[1];
			String criterion = args[2];
			int maxSize = Integer.parseInt(args[3]);
			
			buildAndSolveILPs(date, criterion, maxSize);
		}
	}

}
