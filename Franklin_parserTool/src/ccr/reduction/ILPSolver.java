package ccr.reduction;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;

import lpsolve.LpSolve;
import lpsolve.LpSolveException;
import ccr.help.ILPOutput;
import ccr.help.ThreadManager;
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
	
	public static ArrayList<TestCase> getStatisticsOfTestCase(String testcaseFile, boolean containHeader){
		
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
			String infoFile){
		
		int rowNo = tcArray.get(0).hitSet.size();
		StringBuilder infoRecorder = new StringBuilder();
		
		
		infoRecorder.append("--------------------------------------------------------------------------").append("\n");
		//there is no alpha for single-objective ILP models
		if(alpha != Double.MIN_VALUE){
			infoRecorder.append("Weight factor: " + alpha).append("\n");	
		}		
		infoRecorder.append("Before dimension reduction:").append("\n");
		infoRecorder.append("ProgramElement:").append(rowNo).append("\n");
		infoRecorder.append("TestCase:").append(tcArray.size()).append("\n");
		
		ArrayList<Integer[]> constraints = new ArrayList<Integer[]>();		
		int infeasible_constraint_counter = 0;
		int equivalent_constraint_counter = 0;
		StringBuffer sb_constraints = new StringBuffer();
		
		for(int i = 0; i < rowNo; i ++){ //for each constraint of a program element				
			Integer[] constraint = new Integer[tcArray.size()];
			for(int j = 0; j < constraint.length; j ++){
				ArrayList<Integer> hitSet= tcArray.get(j).hitSet; 
				constraint[j] = hitSet.get(i); // we list all coverage of test cases with respect to each program element(constraint)  
			}
			
			//eliminate all infeasible and equivalent program elements				
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
					sb_constraints.append(" + " + constraint[j] + " x" + j);	
				}
			}
			sb_constraints.append(" >= 1;\n");
		}	
		
		//2010-01-30: decouple this due to the disabled constraints
//		sb_constraints.append("bin "); //specify the binary variables			
//		for(int i = 0; i < tcArray.size()-1; i ++){
//			sb_constraints.append("x"+ i + ", ");
//		}
//		sb_constraints.append("x" + (tcArray.size()-1) +";");
		
		String constraintString = sb_constraints.toString();
	
	
		//save the information file
		Logger.getInstance().setPath(infoFile, false);
		Logger.getInstance().write(infoRecorder.toString());
		Logger.getInstance().close();
		
		return constraintString;
	}
	
	public static String buildCoverageConstraints(double alpha, ArrayList<TestCase> tcArray, 
			String infoFile, String constraintFile){
		
		int rowNo = tcArray.get(0).hitSet.size();
		StringBuilder infoRecorder = new StringBuilder();
		
		
		infoRecorder.append("--------------------------------------------------------------------------").append("\n");
		//there is no alpha for single-objective ILP models
		if(alpha != Double.MIN_VALUE){
			infoRecorder.append("Weight factor: " + alpha).append("\n");	
		}		
		infoRecorder.append("Before dimension reduction:").append("\n");
		infoRecorder.append("ProgramElement:").append(rowNo).append("\n");
		infoRecorder.append("TestCase:").append(tcArray.size()).append("\n");
		
		ArrayList<Integer[]> constraints = new ArrayList<Integer[]>();		
		int infeasible_constraint_counter = 0;
		int equivalent_constraint_counter = 0;
		StringBuffer sb_constraints = new StringBuffer();
		
		for(int i = 0; i < rowNo; i ++){ //for each constraint of a program element				
			Integer[] constraint = new Integer[tcArray.size()];
			for(int j = 0; j < constraint.length; j ++){
				ArrayList<Integer> hitSet= tcArray.get(j).hitSet; 
				constraint[j] = hitSet.get(i); // we list all coverage of test cases with respect to each program element(constraint)  
			}
			
			//eliminate all infeasible and equivalent program elements				
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
					sb_constraints.append(" + " + constraint[j] + " x" + j);	
				}
			}
			sb_constraints.append(" >= 1;\n");
		}	
		
		//2010-01-30: decouple this due to the disabled constraints
//		sb_constraints.append("bin "); //specify the binary variables			
//		for(int i = 0; i < tcArray.size()-1; i ++){
//			sb_constraints.append("x"+ i + ", ");
//		}
//		sb_constraints.append("x" + (tcArray.size()-1) +";");
		
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
	
	public static ArrayList<TestCase> buildILPModel_SingleObj(String testcaseFile, boolean containHeader,
			String modelFile, String infoFile, ArrayList<TestSet> testSets){
	
		ArrayList<TestCase> tcArray = null; 
		//1. read info of all test cases
		tcArray = getStatisticsOfTestCase(testcaseFile, containHeader);
		int rowNo = tcArray.get(0).hitSet.size();
		
		//2010-01-30: 
		//2. exclude disabled test cases which have been used before;			
		if(testSets.size() > 0){ //exclude all used test cases
			
			ArrayList<String> testCases_disabled = new ArrayList<String>();
			//a. get all indexes of disabled test cases which are used before 
			for(int i = 0; i < testSets.size(); i ++){
				ArrayList<String> testcases = testSets.get(i).testcases;									
				for(int j = 0; j < testcases.size(); j ++){
					String testCase = testcases.get(j);
					if(!testCases_disabled.contains(testCase)){
						testCases_disabled.add(testCase);	
					}
				}
			}
			
			ArrayList<TestCase> tcArray_enabled = new ArrayList<TestCase>();
			//b. exclude disable test cases from current test pool				
			for(int i = 0; i < tcArray.size(); i ++){
				TestCase tc = tcArray.get(i);
				if(!testCases_disabled.contains(tc.index)){
					tcArray_enabled.add(tc);
				}
			}
			
			//c.update the tcArray
			tcArray = tcArray_enabled;
		}
		
		//2.create LP file: convert program element(row) * testcases(column) 
		//into the matrix of testcases(row)* program elements(column)
		StringBuilder sb = new StringBuilder();

		//2.1. build the objective function
		sb.append("min:");			
		for(int i = 0; i < tcArray.size(); i ++){			
			sb.append(" + " + " x"+ i);
		}
		sb.append(";\n");

		//2.2. no size constraints
		

		//2010-01-27: 
		//2.3. coverage constraint: check whether the constraint file exists or not
		String coverageConstraints = buildCoverageConstraints(Double.MIN_VALUE, tcArray, infoFile);						
		sb.append(coverageConstraints);
		
		
		//2.4: build binary variable constraints
		sb.append("bin "); //specify the binary variables			
		for(int i = 0; i < tcArray.size()-1; i ++){
			sb.append("x"+ i + ", ");
		}
		sb.append("x" + (tcArray.size()-1) +";");
		sb.append("\n");	
		
		
		Logger.getInstance().setPath(modelFile, false);
		Logger.getInstance().write(sb.toString());
		Logger.getInstance().close();	
	
		return tcArray;
		
	}
	
	/**2010-03-18: different from buildILPModel_BiCriteria() which tries to 
	 * maximize CR(the normalized CD), this method tries to maximize CD.
	 * 
	 * @param testcaseFile
	 * @param containHeader
	 * @param alpha
	 * @param modelFile
	 * @param infoFile
	 * @param maxSize
	 * @param testSets
	 * @return
	 */
	public static ArrayList<TestCase> buildILPModel_BiCriteria_CD(String testcaseFile, boolean containHeader, 
			double alpha, String modelFile, String infoFile, int maxSize, ArrayList<TestSet> testSets){
		ArrayList<TestCase> tcArray = new ArrayList<TestCase>(); 
		//1. read info of all test cases
		tcArray = getStatisticsOfTestCase(testcaseFile, containHeader);
		int rowNo = tcArray.get(0).hitSet.size();
		
		//2010-01-30: 
		//2. exclude disabled test cases which have been used before;			
		if(testSets.size() > 0){ //exclude all used test cases
			
			ArrayList<String> testCases_disabled = new ArrayList<String>();
			//a. get all indexes of disabled test cases which are used before 
			for(int i = 0; i < testSets.size(); i ++){
				ArrayList<String> testcases = testSets.get(i).testcases;									
				for(int j = 0; j < testcases.size(); j ++){
					String testCase = testcases.get(j);
					if(!testCases_disabled.contains(testCase)){
						testCases_disabled.add(testCase);	
					}
				}
			}
			
			ArrayList<TestCase> tcArray_enabled = new ArrayList<TestCase>();
			//b. exclude disable test cases from current test pool				
			for(int i = 0; i < tcArray.size(); i ++){
				TestCase tc = tcArray.get(i);
				if(!testCases_disabled.contains(tc.index)){
					tcArray_enabled.add(tc);
				}
			}
			
			//c.update the tcArray
			tcArray = tcArray_enabled;
		}
		
		//2.create LP file: convert program element(row) * testcases(column) 
		//into the matrix of testcases(row)* program elements(column)
		StringBuilder sb = new StringBuilder();
		
		//2.1. build the objective function
		sb.append("min:");			
		for(int i = 0; i < tcArray.size(); i ++){
			//2009-12-20: use CR rather than CD for scaling purpose
			TestCase tc = tcArray.get(i);
			double CD = tc.CI; //2010-03-18(big difference here):double CR = tc.CI/Double.parseDouble(tc.length);
			double weight = alpha - (1-alpha)*CD;
			sb.append(" + " + new DecimalFormat("0.0000").format(weight) + " x"+ i);
		}
		sb.append(";\n");

		//2009-12-21: 
		//2.2. size constraint: it specifies that the upper bound 
		//of reduced test suite size
		for(int i = 0; i < tcArray.size(); i ++){				
			sb.append(" + 1 x" + i);
		}
		sb.append(" <= " + maxSize + ";\n");

		//2010-01-27: 
		//2.3. coverage constraint: check whether the constraint file exists or not
		String coverageConstraints = buildCoverageConstraints(alpha, tcArray, infoFile);						
		sb.append(coverageConstraints);
		
		
		//2.4: build binary variable constraints
		sb.append("bin "); //specify the binary variables			
		for(int i = 0; i < tcArray.size()-1; i ++){
			sb.append("x"+ i + ", ");
		}
		sb.append("x" + (tcArray.size()-1) +";");
		sb.append("\n");	
		
		
		Logger.getInstance().setPath(modelFile, false);
		Logger.getInstance().write(sb.toString());
		Logger.getInstance().close();	
	
		return tcArray;
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
	public static ArrayList<TestCase> buildILPModel_BiCriteria(String testcaseFile, boolean containHeader, 
			double alpha, String modelFile, String infoFile, int maxSize, ArrayList<TestSet> testSets){
		
			
			ArrayList<TestCase> tcArray = new ArrayList<TestCase>(); 
			//1. read info of all test cases
			tcArray = getStatisticsOfTestCase(testcaseFile, containHeader);
			int rowNo = tcArray.get(0).hitSet.size();
			
			//2010-01-30: 
			//2. exclude disabled test cases which have been used before;			
			if(testSets.size() > 0){ //exclude all used test cases
				
				ArrayList<String> testCases_disabled = new ArrayList<String>();
				//a. get all indexes of disabled test cases which are used before 
				for(int i = 0; i < testSets.size(); i ++){
					ArrayList<String> testcases = testSets.get(i).testcases;									
					for(int j = 0; j < testcases.size(); j ++){
						String testCase = testcases.get(j);
						if(!testCases_disabled.contains(testCase)){
							testCases_disabled.add(testCase);	
						}
					}
				}
				
				ArrayList<TestCase> tcArray_enabled = new ArrayList<TestCase>();
				//b. exclude disable test cases from current test pool				
				for(int i = 0; i < tcArray.size(); i ++){
					TestCase tc = tcArray.get(i);
					if(!testCases_disabled.contains(tc.index)){
						tcArray_enabled.add(tc);
					}
				}
				
				//c.update the tcArray
				tcArray = tcArray_enabled;
			}
			
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
				sb.append(" + " + new DecimalFormat("0.0000").format(weight) + " x"+ i);
			}
			sb.append(";\n");

			//2009-12-21: 
			//2.2. size constraint: it specifies that the upper bound 
			//of reduced test suite size
			for(int i = 0; i < tcArray.size(); i ++){				
				sb.append(" + 1 x" + i);
			}
			sb.append(" <= " + maxSize + ";\n");

			//2010-01-27: 
			//2.3. coverage constraint: check whether the constraint file exists or not
			String coverageConstraints = buildCoverageConstraints(alpha, tcArray, infoFile);						
			sb.append(coverageConstraints);
			
			
			//2.4: build binary variable constraints
			sb.append("bin "); //specify the binary variables			
			for(int i = 0; i < tcArray.size()-1; i ++){
				sb.append("x"+ i + ", ");
			}
			sb.append("x" + (tcArray.size()-1) +";");
			sb.append("\n");	
			
			
			Logger.getInstance().setPath(modelFile, false);
			Logger.getInstance().write(sb.toString());
			Logger.getInstance().close();	
		
		return tcArray;
	}

	/**2010-01-31: we use thread to solve the ILP model so that we
	 * can control the execution time
	 * 
	 * @param modelFile
	 * @param tcArray
	 * @param infoFile
	 * @param timeLimit
	 * @param sleepTime
	 * @return
	 */
	public static ILPOutput solveILPModel_TimeLimited(String modelFile, ArrayList<TestCase> tcArray,
			String infoFile, long timeLimit, long sleepTime){
		
		ILPOutput output = new ILPOutput();
		
		ThreadManager manager = new ThreadManager(timeLimit, sleepTime);
		ILPSolver_Thread programRunnable = new ILPSolver_Thread(modelFile, tcArray, infoFile);
		manager.startThread(programRunnable);
		
		if(manager.finished){
			output = programRunnable.output;
		}
		
		return output;
	}
	
	/**2009-12-21:solve the ILP model with LPSolve and
	 * save the results into a file. This method returns
	 * the reduced test suite.
	 * @param modelFile: the file to load the ILP model 
	 * @param tcArray: the array of all-used test cases(used to map from variables to test case index ) 
	 * @param infoFile: the file to save info collected during solving the model
	 * @return: the test case indexes within the reduced test set
	 */
	public static ILPOutput solveILPModel(String modelFile, ArrayList<TestCase> tcArray, String infoFile){
		ILPOutput output = new ILPOutput();
		
		ArrayList<String> selectedTestCases = new ArrayList<String>();
		
		StringBuilder infoRecorder = new StringBuilder();
		File tmp = new File(modelFile);
		if(tmp.exists()){
			try {										
				String[] strs = tmp.getName().substring(0, tmp.getName().lastIndexOf(".")).split("_");
				output.criterion = strs[1];
				if(strs[2].equals("SingleObj")){//single-objective ILP model
					output.alpha = Double.MIN_VALUE;
					output.testSetLimit = 0;
					output.testSetId = Integer.parseInt(strs[3]);
				}else{
					output.alpha = Double.parseDouble(strs[2]);
					output.testSetLimit = Integer.parseInt(strs[3]);
					output.testSetId = Integer.parseInt(strs[4]);
				}
				
				
				
				LpSolve solver = LpSolve.readLp(modelFile, LpSolve.CRITICAL, null);

				long startTime = System.currentTimeMillis();
				int ret = solver.solve();
				long exeTime = System.currentTimeMillis() - startTime;
				infoRecorder.append("Execute Time:").append(exeTime/(double)1000).append(" s\n");
				
				output.time = (double)exeTime/(double)1000;
				
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
					
					output.reducedTestSet.testcases = selectedTestCases;
					
					//2010-02-01: fix an important bug here
					output.reducedTestSet.index = "" +output.testSetId;
					
					infoRecorder.append("Objective value:").append(new DecimalFormat("0.0000").format(solver.getObjective())).append("\n");
					
					output.objectiveValue = solver.getObjective();
					
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

				String outputFile = tmp.getParent() + "/" + tmp.getName().
					substring(0, tmp.getName().lastIndexOf("."))+"_output.txt";
				
				Logger.getInstance().setPath(outputFile, false);
				Logger.getInstance().write(output.toString());
				Logger.getInstance().close();
			} catch (LpSolveException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}		
		}
		
		return output;
	}
	
	/**2010-01-31: solve an ILP model within a time limit
	 * 
	 * @param date
	 * @param criterion
	 * @param tcArray
	 * @param testSetId
	 * @param timeLimit: time limit (10 min by default)
	 * @param sleepTime: query time of a thread controller (half of timeLimit by default)
	 * @return
	 */
	public static TestSet solveILPModel_SingleObj_Manager_TimeLimited(String date, String criterion,
			ArrayList<TestCase> tcArray, int testSetId, long timeLimit, long sleepTime){
		
		String modelFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
			+ date +"/ILPModel/"+ criterion +"/Model_" + criterion + "_SingleObj_" + testSetId + ".lp";
		String infoFile =  "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
			+ date +"/ILPModel/"+ criterion + "/Result_" + criterion + "_SingleObj_" + testSetId + ".txt";
		
		System.out.println("\n[ILPSolver.solveILPModel_SingleObje_Manager_TimeLimited]Start to solve the model with the single objective:(Criterion:" +criterion +", TestSetID:"+ testSetId+")");
		
		ILPOutput output = solveILPModel_TimeLimited(modelFile, tcArray, 
				infoFile, timeLimit, sleepTime);
		
		TestSet testSet = output.reducedTestSet;
		//2010-02-01: fix an important bug here
		testSet.index = ""+output.testSetId;
		
		System.out.println("[ILPSolver.solveILPModel_SingleObje_Manager_TimeLimited]Finish to solve the model with the single objective:(Criterion:" +criterion +", TestSetID:"+ testSetId+")"+ "\n" );
		
		return testSet;
	}

	
	
	
	public static TestSet solveILPModel_SingleObj_Manager(String date, String criterion,
			ArrayList<TestCase> tcArray, int testSetId){
		
		String modelFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
			+ date +"/ILPModel/"+ criterion +"/Model_" + criterion + "_SingleObj_" + testSetId + ".lp";
		String infoFile =  "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
			+ date +"/ILPModel/"+ criterion + "/Result_" + criterion + "_SingleObj_" + testSetId + ".txt";
		
		System.out.println("\n[ILPSolver.solveILPModel_SingleObje_Manager]Start to solve the model with the single objective:(Criterion:" +criterion +", TestSetID:"+ testSetId+")");
		ILPOutput output = solveILPModel(modelFile, tcArray, infoFile);
		TestSet testSet = output.reducedTestSet;
		//2010-02-01: fix an important bug here
		testSet.index = ""+output.testSetId;
		
		System.out.println("[ILPSolver.solveILPModel_SingleObje_Manager]Finish to solve the model with the single objective:(Criterion:" +criterion +", TestSetID:"+ testSetId+")"+ "\n" );
		
		return testSet;
	}
	
	/**2010-01-30: build single-objective ILP model. The objective function tries to minimize the
	 * reduced test suite size; Three constraints include: coverage constraint, disabled constraint, 
	 * and binary variable constraint
	 * @param date
	 * @param criterion: use to build coverage constraint
	 * @param testSets: the test cases indexes used to build disabled constraint
	 * @return: test case indexes to be referred by the model solver
	 */
	public static ArrayList<TestCase> buildILPModel_SingleObj_Manager(String date, String criterion, ArrayList<TestSet> testSets){
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

		String modelFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
			+ date +"/ILPModel/"+ criterion +"/Model_" + criterion + "_SingleObj_" + +testSets.size()+ ".lp";
		String infoFile =  "src/ccr/experiment/Context-Intensity_backup/TestHarness/" 
			+ date +"/ILPModel/"+ criterion + "/Result_" + criterion + "_SingleObj_" + testSets.size() + ".txt";			
		testSet = buildILPModel_SingleObj(testcaseFile, containHeader, modelFile, infoFile, testSets);
		
		return testSet;
	}
	
	/**2009-12-21: build multiple ILP models with respect to different weighting factors
	 * 
	 * @param date: the directory to get the test case statistics
	 * @param criterion: the testing criterion
	 * @param alpha: weighting factor
	 * @param maxSize: the upper bound of the reduced test suite
	 * @param testSets: the ordered test cases to be referred by model solver
	 * @return
	 */
	public static ArrayList<TestCase> buildILPModels_BiCriteria_Manager(String date, String criterion,
			double alpha, int maxSize, ArrayList<TestSet> testSets){
		
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

		String alpha_str = format.format(alpha);
		String modelFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
			+ date +"/ILPModel/"+ criterion +"/Model_" + criterion + "_" + alpha_str + "_" + maxSize + "_"+ testSets.size() +".lp";
		String infoFile =  "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
			+ date +"/ILPModel/"+ criterion + "/Result_" + criterion + "_" + alpha_str +"_" + maxSize + "_"+ testSets.size() +".txt";			
		testSet = buildILPModel_BiCriteria(testcaseFile, containHeader, alpha, modelFile,
				infoFile, maxSize, testSets);
		
		return testSet;
	}
	
	/**2010-03-18: different from buildILPModels_BiCriteria_Manager() which tries to 
	 * maximize CR(the normalized CD), this method tries to maximize CD.
	 * 
	 * @param date
	 * @param criterion
	 * @param alpha
	 * @param maxSize
	 * @param testSets
	 * @return
	 */
	public static ArrayList<TestCase> buildILPModels_BiCriteria_Manager_CD(String date, String criterion,
			double alpha, int maxSize, ArrayList<TestSet> testSets){
		
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

		String alpha_str = format.format(alpha);
		String modelFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
			+ date +"/ILPModel/"+ criterion +"/Model_" + criterion + "_" + alpha_str + "_" + maxSize + "_"+ testSets.size() +".lp";
		String infoFile =  "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
			+ date +"/ILPModel/"+ criterion + "/Result_" + criterion + "_" + alpha_str +"_" + maxSize + "_"+ testSets.size() +".txt";			
		testSet = buildILPModel_BiCriteria_CD(testcaseFile, containHeader, alpha, modelFile,
				infoFile, maxSize, testSets);
		
		return testSet;
	}
	

	public static TestSet solveILPModels_BiCriteria_Manager_TimeLimited(String date, String criterion,
			ArrayList<TestCase> tcArray, double alpha, int maxSize, int testSetId, 
			long timeLimit, long sleepTime){
		
		DecimalFormat format = new DecimalFormat("0.0");
		String alpha_str = format.format(alpha);
		
		String modelFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
			+ date +"/ILPModel/"+ criterion +"/Model_" + criterion + "_" + alpha_str + "_" + maxSize + "_"+ testSetId +".lp";
		String infoFile =  "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
			+ date +"/ILPModel/"+ criterion + "/Result_" + criterion + "_" + alpha_str +"_" + maxSize + "_"+ testSetId +".txt";
		
		System.out.println("\n[ILPSolver.solveILPModels_BiCriteria_Manager_TimeLimited]Start to solve the model:(Criterion:" + criterion 
				+ " ,Alpha:" + alpha_str + ",testSetSize:" + maxSize+ ",testSetID:"+ testSetId + ")");

		ILPOutput output = solveILPModel_TimeLimited(modelFile, tcArray, 
				infoFile, timeLimit, sleepTime);
		
		TestSet testSet = output.reducedTestSet;
		//2010-02-01: fix an important bug here
		testSet.index = ""+output.testSetId;
		
		System.out.println("[ILPSolver.solveILPModels_BiCriteria_Manager_TimeLimited]Finish to solve the model(Criterion:" + criterion 
				+ ",Alpha:" + alpha_str + ", testSetSize:"+ maxSize+", testSetID:"+ testSetId + ")"+ "\n");
		
		return testSet;
	}

	public static TestSet solveILPModels_BiCriteria_Manager(String date, String criterion,
			ArrayList<TestCase> tcArray, double alpha, int maxSize, int testSetId){
		
		DecimalFormat format = new DecimalFormat("0.0");
		String alpha_str = format.format(alpha);
		
		String modelFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
			+ date +"/ILPModel/"+ criterion +"/Model_" + criterion + "_" + alpha_str + "_" + maxSize + "_"+ testSetId +".lp";
		String infoFile =  "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
			+ date +"/ILPModel/"+ criterion + "/Result_" + criterion + "_" + alpha_str +"_" + maxSize + "_"+ testSetId +".txt";
		
		System.out.println("\n[ILPSolver.solveILPModels_BiCriteria_Manager]Start to solve the model:(Criterion:" + criterion 
				+ " ,Alpha:" + alpha_str + ",testSetSize:" + maxSize+ ",testSetID:"+ testSetId + ")");
		ILPOutput output = solveILPModel(modelFile, tcArray, infoFile);		
		TestSet testSet = output.reducedTestSet;
		//2010-02-01: fix an important bug here
		testSet.index = ""+output.testSetId;
		
		System.out.println("[ILPSolver.solveILPModels_BiCriteria_Manager]Finish to solve the model(Criterion:" + criterion 
				+ ",Alpha:" + alpha_str + ", testSetSize:"+ maxSize+", testSetID:"+ testSetId + ")"+ "\n");		
		return testSet;
	}
	
	/**2010-03-18: different from buildAndSolveILPs_BiCriteria_Manager() which tries to 
	 * maximize CR(the normalized CD), this method tries to maximize CD.
	 * 
	 * 
	 * @param date
	 * @param criterion
	 * @param alpha_min
	 * @param alpha_max
	 * @param alpha_interval
	 * @param maxSize
	 * @param testSetNum
	 * @param timeLimit
	 * @param sleepTime
	 * @return
	 */
	public static HashMap<Double, ArrayList<TestSet>> buildAndSolveILPs_BiCriteria_Manager_CD(String date, String criterion, 
			double alpha_min, double alpha_max, double alpha_interval, int maxSize, int testSetNum, long timeLimit,	long sleepTime){
		
		HashMap<Double, ArrayList<TestSet>> alpha_testSets = new HashMap<Double, ArrayList<TestSet>>();
		
		for(double alpha = alpha_min;  Math.abs(alpha_max - alpha) > 0.0001; alpha = alpha + alpha_interval){
			ArrayList<TestCase> tcArray = new ArrayList<TestCase>();
			ArrayList<TestSet> testSets = new ArrayList<TestSet>();
			for(int i = 0; i < testSetNum; i ++){
				tcArray = buildILPModels_BiCriteria_Manager_CD(date, criterion, alpha, maxSize, testSets);
				
				//2010-03-18: sequential version
//				TestSet testSet = solveILPModels_BiCriteria_Manager(
//						date, criterion, tcArray, alpha, maxSize, i);
				
				//2010-01-31: a concurrent version
				
				TestSet testSet = solveILPModels_BiCriteria_Manager_TimeLimited(date, 
						criterion, tcArray, alpha, maxSize, i, timeLimit, sleepTime);
				
				if(testSet.size() == 0){ //fail to solve the ILP model
					break;
				}
				testSets.add(testSet);
			}
			alpha_testSets.put(alpha, testSets);
		}
		
		return alpha_testSets;
	}
	
	/**2009-12-21: create and solve multiple binary ILP model 
	 * with respect to different weighting factors. 
	 * This method returns the list of reduced test suite with respect
	 * to different weighting factors.
	 * 
	 * @param date
	 * @param criterion
	 * @param alpha_min
	 * @param alpha_max
	 * @param alpha_interval
	 * @param maxSize
	 * @param testSetNum
	 * @param timeLimit
	 * @param sleepTime
	 * @return
	 */
	public static HashMap<Double, ArrayList<TestSet>> buildAndSolveILPs_BiCriteria_Manager(String date, String criterion, 
			double alpha_min, double alpha_max, double alpha_interval, int maxSize, int testSetNum, long timeLimit,	long sleepTime){
		
		HashMap<Double, ArrayList<TestSet>> alpha_testSets = new HashMap<Double, ArrayList<TestSet>>();
		
		for(double alpha = alpha_min;  Math.abs(alpha_max - alpha) > 0.0001; alpha = alpha + alpha_interval){
			ArrayList<TestCase> tcArray = new ArrayList<TestCase>();
			ArrayList<TestSet> testSets = new ArrayList<TestSet>();
			for(int i = 0; i < testSetNum; i ++){
				tcArray = buildILPModels_BiCriteria_Manager_CD(date, criterion, alpha, maxSize, testSets);
				
				//2010-01-30: a sequential version
//				TestSet testSet = solveILPModels_BiCriteria_Manager(date, criterion, tcArray,
//						alpha, maxSize, i);
				
				//2010-01-31: a concurrent version
				TestSet testSet = solveILPModels_BiCriteria_Manager_TimeLimited(date, 
						criterion, tcArray, alpha, maxSize, i, timeLimit, sleepTime);
				
				if(testSet.size() == 0){ //fail to solve the ILP model
					break;
				}
				testSets.add(testSet);
			}
			alpha_testSets.put(alpha, testSets);
		}
		
		return alpha_testSets;
	}
		
	/**2010-01-31: build and solve an ILP model within a time limit
	 * 
	 * @param date
	 * @param criterion
	 * @param testSetNum
	 * @return
	 */
	public static ArrayList<TestSet> buildAndSolveILP_SingleObj_Manager(String date, 
			String criterion, int testSetNum, long timeLimit, long sleepTime){
		
		ArrayList<TestSet> testSets = new ArrayList<TestSet>();		
		for(int i = 0; i < testSetNum; i ++){
			ArrayList<TestCase> tcArray = buildILPModel_SingleObj_Manager(date, criterion, testSets);
			
			//2010-01-30:sequential version
//			TestSet testSet = solveILPModel_SingleObj_Manager(date, criterion, 
//					tcArray, i);
			
			//2010-01-31:Concurrent version
		
			TestSet testSet = solveILPModel_SingleObj_Manager_TimeLimited(date, 
					criterion, tcArray, i, timeLimit, sleepTime);
			
			if(testSet.size() == 0){ //fail to solve the ILP model
				break;
			}
			
			testSets.add(testSet);
		}
		return testSets;
	}
	
	
	/**2010-01-27: judge whether a given constraint is a member of a constraint array.
	 * This method can be used to get an equivalent constraint
	 * 
	 * @param constraints
	 * @param constraint
	 * @return
	 */
	private static boolean isMemberOf(ArrayList<Integer[]> constraints, Integer[] constraint){
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
		if(instruction.equals("buildILP")){
			String date = args[1];
			String criterion = args[2];
			int maxSize = Integer.parseInt(args[3]);
			int testSetNum = Integer.parseInt(args[4]);
			long timeLimit = Long.parseLong(args[5]); //60*60*1000
			long sleepTime = timeLimit/10;
			
			double alpha_min = 0.0;
			double alpha_max = 1.1;
			double alpha_interval = 0.1;
			if(args.length == 9){
				alpha_min = Double.parseDouble(args[6]);
				alpha_max = Double.parseDouble(args[7]);
				alpha_interval = Double.parseDouble(args[8]);
			}
			
			buildAndSolveILPs_BiCriteria_Manager(date, criterion, alpha_min, alpha_max, 
					alpha_interval, maxSize, testSetNum, timeLimit, sleepTime);
			
//			buildAndSolveILPs_BiCriteria_Manager(date, criterion, alpha_min, alpha_max, 
//					alpha_interval, maxSize, testSetNum);
		}else if(instruction.equals("solveILP")){
			String date = args[1];
			String criterion = args[2];
			int maxSize = Integer.parseInt(args[3]);			
			double alpha = Double.parseDouble(args[4]);
			
			double alpha_min = 0.0;
			double alpha_max = 1.1;
			double alpha_interval = 0.1;
			if(args.length == 8){
				alpha_min = Double.parseDouble(args[5]);
				alpha_max = Double.parseDouble(args[6]);
				alpha_interval = Double.parseDouble(args[7]);
			}
			
			String testcaseFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
				+ date + "/"+ criterion + "/TestCaseStatistics_" + criterion + ".txt";
			boolean containHeader = true;
			ArrayList<TestCase> tcArray = getStatisticsOfTestCase(testcaseFile, containHeader);
			
			solveILPModels_BiCriteria_Manager(date, criterion, tcArray, alpha, maxSize, 0);
			
//			solveILPModels_BiCriteria_Manager(date, criterion,
//					tcArray, alpha_min, alpha_max, alpha_interval, maxSize);
			
		}else if(instruction.equals("buildAndSolveILP")){
			String date = args[1];
			String criterion = args[2];
			int maxSize = Integer.parseInt(args[3]);
			int testSetNum = Integer.parseInt(args[4]);
			long timeLimit = Long.parseLong(args[5]); //60*60*1000
			long sleepTime = timeLimit/10;
			
			double alpha_min = 0.0;
			double alpha_max = 1.1;
			double alpha_interval = 0.1;
			if(args.length == 9){
				alpha_min = Double.parseDouble(args[6]);
				alpha_max = Double.parseDouble(args[7]);
				alpha_interval = Double.parseDouble(args[8]);
			}
			
			buildAndSolveILPs_BiCriteria_Manager(date, criterion, alpha_min, alpha_max, 
					alpha_interval, maxSize, testSetNum, timeLimit, sleepTime);
			
//			buildAndSolveILPs_BiCriteria_Manager(date, criterion, alpha_min, alpha_max,
//					alpha_interval, maxSize, testSetNum);			
		}else if(instruction.equals("buildAndSolveILP_SingleObj")){
			String date = args[1];
			String criterion = args[2];
			int testSetNum = Integer.parseInt(args[3]);
			long timeLimit = Long.parseLong(args[4]); //60*60*1000
			long sleepTime = timeLimit/10;
			
			buildAndSolveILP_SingleObj_Manager(date, criterion, testSetNum, timeLimit, sleepTime);
//			buildAndSolveILP_SingleObj_Manager(date, criterion, testSetNum);			
		}
	}

}
