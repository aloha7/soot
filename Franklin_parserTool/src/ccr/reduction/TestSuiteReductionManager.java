package ccr.reduction;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.Format;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

import ccr.help.DataAnalyzeManager;
import ccr.help.DataDescriptionResult;
import ccr.help.ILPOutput;
import ccr.help.MutantStatistics;
import ccr.help.ThreadManager;
import ccr.report.ICDCS10;
import ccr.test.Logger;
import ccr.test.ReducedTestSet;
import ccr.test.TestCase;
import ccr.test.TestDriver;
import ccr.test.TestSet;

public class TestSuiteReductionManager {
	
	//2010-11-15: get all coverage statistic for the test pool w.r.t a testing criterion
	public static void getStatisticsOfTestPool(String date, String criterion){
		String appClassName = "TestCFG2_ins";
		TestSuiteReduction.getStatisticsOfTestPool(appClassName, date, criterion);
	}
	
	//2010-11-15: load test sets from existing files
	public static ArrayList<TestSet> loadTestSet_offline(String date, String criterion, int testSuiteSize, 
			String oldOrNew, String randomOrCriterion, String H_L_R, int size_ART, boolean containHeader){
		return new ICDCS10().loadTestSet_offline(date, criterion, testSuiteSize, oldOrNew, 
				randomOrCriterion, H_L_R, size_ART, containHeader);
	}
	
	/**2010-11-15: load offline test sets whose indice range from testSet_startIndex(inclusive)
	 * to testSet_endIndex(exclusive)
	 * 
	 * @param date
	 * @param criterion
	 * @param testSuiteSize
	 * @param oldOrNew
	 * @param randomOrCriterion
	 * @param H_L_R
	 * @param size_ART
	 * @param containHeader
	 * @param testSet_startIndex
	 * @param testSet_endIndex
	 * @return
	 */
	public static ArrayList<TestSet> loadTestSet_offline_range(String date, String criterion, int testSuiteSize, 
			String oldOrNew, String randomOrCriterion, String H_L_R, int size_ART, boolean containHeader, 
			int testSet_startIndex, int testSet_endIndex){
		ArrayList<TestSet> testSets = new ICDCS10().loadTestSet_offline(date, criterion, testSuiteSize, oldOrNew, 
				randomOrCriterion, H_L_R, size_ART, containHeader);
		
		ArrayList<TestSet> testSets_range = new ArrayList<TestSet>();
		for(int i = testSet_startIndex; i < testSet_endIndex; i ++){
			testSets_range.add(testSets.get(i));
		}
		return testSets_range;
	}
	
	
	//2010-11-15:the array keeps all coverage information of test cases in the pool w.r.t a testing criterion
	static HashMap<String, HashMap<String, TestCase>> criterion_testpool = 
		new HashMap<String, HashMap<String,TestCase>>();

	
	public static HashMap<Integer, HashMap<Double, ArrayList<TestSet>>> buildAndSolveILPs_BiCriteria_Manager(String date, String criterion, 
			double alpha_min, double alpha_max, double alpha_interval, int beta_min, int beta_max, int beta_interval,
			ArrayList<TestSet> testSets_beforeReduction, long timeLimit,	long sleepTime){		
	
		HashMap<Integer, HashMap<Double, ArrayList<TestSet>>> beta_alpha_reducedTestSets = new 
			HashMap<Integer, HashMap<Double,ArrayList<TestSet>>>();

		for(int sizeConstraint = beta_min; sizeConstraint < beta_max || (sizeConstraint>beta_max && 
				(sizeConstraint - beta_interval) < beta_max);sizeConstraint += beta_interval){
			
			if((sizeConstraint>beta_max && 
					(sizeConstraint - beta_interval) < beta_max)){
				sizeConstraint = beta_max;
			}
			
			beta_alpha_reducedTestSets.put(sizeConstraint, new HashMap<Double, ArrayList<TestSet>>());
			
			for(double alpha = alpha_min;  Math.abs(alpha_max - alpha) > 0.0001; alpha = alpha + alpha_interval){
				
				
				ArrayList<TestSet> reducedTestSets = new ArrayList<TestSet>();
				for(int i = 0; i < testSets_beforeReduction.size(); i ++){
					TestSet testSet_beforeReduction = testSets_beforeReduction.get(i);
					long start = System.currentTimeMillis();					
					buildILPModels_BiCriteria_Manager(date, criterion, alpha, sizeConstraint, testSet_beforeReduction);					
					long modelBuildTime = System.currentTimeMillis() - start;
					
					TestSet reducedTestSet = solveILPModels_BiCriteria_Manager_TimeLimited(date, criterion, testSet_beforeReduction, alpha,
							sizeConstraint, Integer.parseInt(testSet_beforeReduction.index), timeLimit, sleepTime, modelBuildTime);
					
					reducedTestSet.add(reducedTestSet);
				}
				
				HashMap<Double, ArrayList<TestSet>> alpha_reducedTestSets = 
					beta_alpha_reducedTestSets.get(sizeConstraint);
				alpha_reducedTestSets.put(alpha, reducedTestSets);
			}
		}
		return beta_alpha_reducedTestSets;
	}
	
	/**2010-11-15: build multiple ILP models with respect to different weighting factors
	 * 
	 * @param date: the directory to get the test case statistics
	 * @param criterion: the testing criterion
	 * @param alpha: weighting factor
	 * @param maxSize: the upper bound of the reduced test suite
	 * @param testSets: the ordered test cases to be referred by model solver
	 * @return
	 */
	public static ArrayList<TestCase> buildILPModels_BiCriteria_Manager(String date, String criterion,
			double alpha, int sizeConstraint, TestSet testSet_beforeReduction){
		
		ArrayList<TestCase> testSet = null;
		boolean containHeader = true;
		String testcaseFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
			+ date + "/ILPModel/"+ criterion + "/TestCaseStatistics_" + criterion + ".txt";
		
		DecimalFormat format = new DecimalFormat("0.0");

		String alpha_str = format.format(alpha);
		String modelFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
			+ date +"/ILPModel/"+ criterion +"/Model_" + criterion + "_" + alpha_str + "_" + sizeConstraint + "_"
			+ testSet_beforeReduction.index +".lp";
		String infoFile =  "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
			+ date +"/ILPModel/"+ criterion + "/Result_" + criterion + "_" + alpha_str +"_" + sizeConstraint + "_"
			+ testSet_beforeReduction.index +".txt";	
				
		testSet = buildILPModel_BiCriteria(criterion, testcaseFile, containHeader, alpha, sizeConstraint, 
				modelFile, infoFile, testSet_beforeReduction);
		
		return testSet;
	}
	
	/**
	 * 2010-11-15: build an ILP model to reduce the adequate test suite w.r.t a given testing criterion
	 * The objective function tries to minimize the test suite size while maximizing the context diversity,
	 * the constraint system consists of two parts:size constraint and coverage constraint
	 * @param criterion: testing criterion
	 * @param testcaseFile: the coverage of the test pool w.r.t a testing criterion
	 * @param containHeader: whether the first line is a header or not
	 * @param alpha: weighting factor between
	 * @param sizeConstraints: the upper bound of the size of reduced test suites 
	 * @param modelFile: the file to save generated ILP model file
	 * @param infoFile: the file to save all info generated during constructing the model
	 * @param testSet_BeforeReduce: the adequate test suite with respect to a testing criterion before reduction
	 * @return: the test set with detailed coverage information 
	 */
	public static ArrayList<TestCase> buildILPModel_BiCriteria(String criterion, String testcaseFile, boolean containHeader, double alpha, 
			int sizeConstraints, String modelFile, String infoFile, TestSet testSet_BeforeReduce){
		
		//1. read info of all test cases
		HashMap<String, TestCase> index_tcCov = new HashMap<String, TestCase>();
		if(criterion_testpool.containsKey(criterion)){
			index_tcCov = criterion_testpool.get(criterion);
		}else{
			index_tcCov = Helper.getStatisticsOfTestCase(testcaseFile, containHeader);
			criterion_testpool.put(criterion, index_tcCov);
		}
		
		//2. only interested in coverage of test cases within the test set
		ArrayList<TestCase> testSet_cover = new ArrayList<TestCase>();
		for(String index: testSet_BeforeReduce.testcases){
			TestCase tc = index_tcCov.get(index);
			testSet_cover.add(tc);
		}
		
		//3.create LP file: convert program element(row) * testcases(column) 
		//into the matrix of testcases(row)* program elements(column)
		StringBuilder sb = new StringBuilder();
		
		//3.1. build the objective function
		sb.append("min:");			
		for(int i = 0; i < testSet_cover.size(); i ++){
			//2010-11-15: integrate CD into the objective function 
			TestCase tc = testSet_cover.get(i);		
			double CD = tc.CI;
			double weight = alpha - (1-alpha)*CD;
			sb.append(" + " + new DecimalFormat("0.0000").format(weight) + " x"+ i);
		}
		sb.append(";\n");
		
		//3.2. size constraint: it specifies that the upper bound 
		//of reduced test suite size
		for(int i = 0; i < testSet_cover.size(); i ++){				
			sb.append(" + 1 x" + i);
		}
		sb.append(" <= " + sizeConstraints + ";\n");
		
		//2010-11-15: 
		//3.3. coverage constraint: check whether the constraint file exists or not
		String coverageConstraints = Helper.buildCoverageConstraints(alpha, testSet_cover, infoFile);						
		sb.append(coverageConstraints);
		
		//3.4: build binary variable constraints
		sb.append("bin "); //specify the binary variables			
		for(int i = 0; i < testSet_cover.size()-1; i ++){
			sb.append("x"+ i + ", ");
		}
		
		sb.append("x" + (testSet_cover.size()-1) +";");
		sb.append("\n");	
		
		Logger.getInstance().setPath(modelFile, false);
		Logger.getInstance().write(sb.toString());
		Logger.getInstance().close();	
	
		return testSet_cover;
	}
	
	
	
	
	public static TestSet solveILPModels_BiCriteria_Manager_TimeLimited(String date, String criterion,
			TestSet testSet_beforeReduction, double alpha, int maxSize, int testSetId, 
			long timeLimit, long sleepTime, long modelBuildTime){
		
		DecimalFormat format = new DecimalFormat("0.0");
		String alpha_str = format.format(alpha);
		
		String modelFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
			+ date +"/ILPModel/"+ criterion +"/Model_" + criterion + "_" + alpha_str + "_" + maxSize + "_"+ testSetId +".lp";
		String infoFile =  "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
			+ date +"/ILPModel/"+ criterion + "/Result_" + criterion + "_" + alpha_str +"_" + maxSize + "_"+ testSetId +".txt";
		
		System.out.println("\n[ILPSolver.solveILPModels_BiCriteria_Manager_TimeLimited]Start to solve the model:(Criterion:" + criterion 
				+ " ,Alpha:" + alpha_str + ",testSetSize:" + maxSize+ ",testSetID:"+ testSetId + ")");

		ILPOutput output = solveILPModel_TimeLimited(modelFile, testSet_beforeReduction, 
				infoFile, timeLimit, sleepTime, modelBuildTime);
		
		TestSet testSet = output.reducedTestSet;
		//2010-02-01: fix an important bug here
		testSet.index = ""+output.testSetId;
		
		System.out.println("[ILPSolver.solveILPModels_BiCriteria_Manager_TimeLimited]Finish to solve the model(Criterion:" + criterion 
				+ ",Alpha:" + alpha_str + ", testSetSize:"+ maxSize+", testSetID:"+ testSetId + ")"+ "\n");
		
		return testSet;
	}
	
	
	public static ILPOutput solveILPModel_TimeLimited(String modelFile, TestSet testSet_beforeReduction,
			String infoFile, long timeLimit, long sleepTime, long modelBuildTime){
		
		ILPOutput output = new ILPOutput();
		
		ThreadManager manager = new ThreadManager(timeLimit, sleepTime);
		ILPSolver_Thread programRunnable = new ILPSolver_Thread(modelFile, testSet_beforeReduction, infoFile, modelBuildTime);
		manager.startThread(programRunnable);
		
		if(manager.finished){
			output = programRunnable.output;
		}
		
		return output;
	}
	
	
	/**2010-11-16: save all info of test sets before reduction and  
	 * after reduction; return the name of saved file. During this process,
	 * it also keeps all cases where no optimal solutions are found
	 * 
	 * @param date
	 * @param criterion
	 * @param alpha_min: inclusive
	 * @param alpha_max: exclusive
	 * @param alpha_interval
	 * @param beta_min: inclusive
	 * @param beta_max: exclusive
	 * @param beta_interval
	 * @param testSet_startIndex: inclusive
	 * @param testSet_endIndex: exclusive
	 * @return
	 */
	public static String saveReductionResult(String date, String criterion, double alpha_min, double alpha_max, 
			double alpha_interval, int beta_min, int beta_max, int beta_interval, int testSet_startIndex,
			int testSet_endIndex){
		
		String saveFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
			+ date +"/ILPModel/" + criterion + "_" + alpha_min +"_" + alpha_max + "_" 
			+ beta_min + "_" + beta_max + "_"+ testSet_startIndex+ "_"+ testSet_endIndex + ".txt";	
		
		StringBuilder sb = new StringBuilder();
		
		sb.append("Criterion").append("\t").append("TestSetIndex").append("\t").append("Size_beforeReduction").append("\t").
		append("UsedProgramElems").append("\t").append("InfeasibleProgramElems").append("\t").append("EquivalentProgramElems").append("\t").
		append("Alpha").append("\t").append("Beta").append("\t").append("ModelBuildTime").append("\t").append("SolvingTime").append("\t").
		append("ObjectiveValue").append("\t").append("Size_reduction").append("\t").append("SelectedTestCases").append("\n");//13 items
		
		DecimalFormat format = new DecimalFormat("0.0");
		String infoFile = null;
		
		//the case where no optimal test set is found
		String retryFile =  "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
			+ date +"/ILPModel/retryList.txt";
		StringBuilder retry = new StringBuilder();
		
//		if(!file.exists()){
//			retry.append("Criterion\t").append("Alpha\t").append("Beta\t").append("TestSetIndex\n");	
//		}
		
		for(int sizeConstraint = beta_min; sizeConstraint < beta_max || (sizeConstraint>beta_max && 
				(sizeConstraint - beta_interval) < beta_max);sizeConstraint += beta_interval){
			
			if((sizeConstraint>beta_max && 
					(sizeConstraint - beta_interval) < beta_max)){
				sizeConstraint = beta_max;
			}
			
			
			for(double alpha = alpha_min;  Math.abs(alpha_max - alpha) > 0.0001; alpha = alpha + alpha_interval){
				for(int testSetIndex = testSet_startIndex; testSetIndex < testSet_endIndex; testSetIndex ++){
					
					infoFile =  "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
						+ date +"/ILPModel/"+ criterion + "/Result_" + criterion + "_" + format.format(alpha) +"_" + sizeConstraint + "_"
						+ testSetIndex +".txt";
					
					System.out.println("Processing file:" + criterion + "_" + format.format(alpha) +"_" + sizeConstraint + "_"
							+ testSetIndex +".txt");
					
					try {
						BufferedReader br = new BufferedReader(new FileReader(infoFile));
						StringBuilder tmp = new StringBuilder();
						String str = null;
						while((str = br.readLine())!= null){
							tmp.append(str).append("\n");
						}
						str = tmp.toString();
						
						
						String objectiveValue = "NoSolution";
						String size_reduction = "NoSolution";
						String selectedTestCases = "NoSolution";
						if(str.contains("cannot get the solution")){
							//Retry: no optimal solution is found 
							retry.append(criterion).append("\t").append(format.format(alpha)).append("\t").
							append(sizeConstraint).append("\t").append(testSetIndex).append("\n");
						}else{
							//optimal solution is available
							sb.append(criterion).append("\t").append(testSetIndex).append("\t");
							
							String size_beforeReduction = str.substring(str.indexOf("TestCase:")+"TestCase:".length(), 
									str.indexOf("\nAfter dimension reduction:"));
					
							sb.append(size_beforeReduction).append("\t");
					
							String usedProgramElems = str.substring(
									str.indexOf("After dimension reduction:\nProgramElement:")+
									"After dimension reduction:\nProgramElement:".length(),
									str.indexOf("("));
							String infeasibleProgramElems = str.substring(str.indexOf("exclude infeasible:") 
									+ "exclude infeasible:".length(), str.indexOf(","));
							String equivalentProgramElems = str.substring(str.indexOf("equivalent:")
									+"equivalent:".length(), str.indexOf(")"));
							
							sb.append(usedProgramElems).append("\t").append(infeasibleProgramElems).append("\t").
							append(equivalentProgramElems).append("\t").append(format.format(alpha)).append("\t").
							append(sizeConstraint).append("\t");
					
							String modelBuildeTime = str.substring(str.indexOf("ModelBuildTime:") + "ModelBuildTime:".length(),
									str.indexOf(" s\nExecute Time:"));
							String executeTime = str.substring(str.indexOf("Execute Time:")+ "Execute Time:".length(),
									str.indexOf(" s\nTotal Time:"));
					
							sb.append(modelBuildeTime).append("\t").append(executeTime).append("\t");
								
							
							objectiveValue = str.substring(str.indexOf("Objective value:")+ "Objective value:".length(),
									str.indexOf("\nReduced Test Suite Size:"));						
							size_reduction = str.substring(str.indexOf("Reduced Test Suite Size:")+ "Reduced Test Suite Size:".length(),
									str.indexOf("\nSelected Test Cases:"));
							selectedTestCases = str.substring(str.indexOf("Selected Test Cases:\n") + "Selected Test Cases:\n".length());
							
							sb.append(objectiveValue).append("\t").append(size_reduction).append("\t").append(selectedTestCases);
						}
					} catch (FileNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			
		}
		
		Logger.getInstance().setPath(retryFile, true);//append rather than overwritten
		Logger.getInstance().write(retry.toString());
		Logger.getInstance().close();
		
		Logger.getInstance().setPath(saveFile, false);
		Logger.getInstance().write(sb.toString());
		Logger.getInstance().close();
		
		return saveFile;
	}
	
	//2010-11-22: save information(size, CDs, and effectiveness) of test suites before reduction
	public static void saveTestSetInfo_beforeReduction(String date, String[] criteria, int testSuiteSize, String oldOrNew,
			String randomOrCriterion, String H_L_R, int size_ART, boolean containHeader, 
			int testSet_startIndex, int testSet_endIndex){

		//1.load information of all test cases in a test suite
		String testPoolFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
			+ date + "/TestPool.txt";
		HashMap<String, Integer> tc_CD = Helper.getCDs(testPoolFile, true);
		ICDCS10 ins = new ICDCS10();
		
		//2. derive the size, time, CD, and FDR of reduced test sets with respect to each alpha and beta
		String mutantFile_date = "20100121";
		String mutantDetail_date = "20100121";
		String mutantDetailDir = System.getProperty("user.dir") + "/src/ccr"
		+ "/experiment/Context-Intensity_backup/TestHarness/" + mutantDetail_date
		+ "/Mutant/";
		
		
		DecimalFormat intFormatter = new DecimalFormat("0");
		
		DecimalFormat doubleFormatter = new DecimalFormat("0.0");
		DecimalFormat doubleFormatter_2 = new DecimalFormat("0.00");
		
		DecimalFormat fdrFormatter = new DecimalFormat("0.000");
		DecimalFormat fdrFormatter_2 = new DecimalFormat("0.0000");
		StringBuilder sb = new StringBuilder();
		
		for(String criterion: criteria){
			System.out.println("Processing criterion:" + criterion);
			ArrayList<TestSet> testSets_beforeReduction = loadTestSet_offline_range(date, criterion, 
					testSuiteSize, oldOrNew, randomOrCriterion, 
					H_L_R, size_ART, containHeader, testSet_startIndex, testSet_endIndex);
			
			//3.1: get the statistics of size, time, CD, and fdr of reduced test sets
			ArrayList<Double> sizeArray = new ArrayList<Double>();
			ArrayList<Double> contextDiversityArray = new ArrayList<Double>();
			ArrayList<Double> faultDetectionRateArray = new ArrayList<Double>();
			
			for(TestSet testSet: testSets_beforeReduction){
				sizeArray.add(new Double(testSet.testcases.size()));			
				contextDiversityArray.add(new Double(ins.getAverageCD(testSet, tc_CD)));
			}
			
			boolean containHeader_testing = true;
			boolean containHeader_mutant = false;
			
			HashMap<String, Double> mutant_fdr = Helper.getFaultDetectionRate_detailed(testSets_beforeReduction, 
					mutantFile_date, containHeader_mutant, mutantDetailDir, containHeader_testing);
			Iterator ite = mutant_fdr.values().iterator();
			while(ite.hasNext()){
				faultDetectionRateArray.add((Double)ite.next());
			}
			
			DataDescriptionResult sizeDes = DataAnalyzeManager.getDataDescriptive(sizeArray);
			DataDescriptionResult CDDes = DataAnalyzeManager.getDataDescriptive(contextDiversityArray);
			DataDescriptionResult FDRDes = DataAnalyzeManager.getDataDescriptive(faultDetectionRateArray);
			
			
			
			sb.append("Criterion\t").
				append("Size_Min\t").append("Size_Mean\t").append("Size_Median\t").append("Size_Std\t").append("Size_Max\t").
				append("CD_Min\t").append("CD_Mean\t").append("CD_Median\t").append("CD_Std\t").append("CD_Max\t").
				append("FDR_Min\t").append("FDR_Mean\t").append("FDR_Median\t").append("FDR_Std\t").append("FDR_Max\n");
			
			sb.append(criterion).
			append(intFormatter.format(sizeDes.min)).append("\t").append(intFormatter.format(sizeDes.mean)).append("\t").
			append(intFormatter.format(sizeDes.median)).append("\t").append(doubleFormatter.format(sizeDes.std)).append("\t").
			append(intFormatter.format(sizeDes.max)).append("\t").
		
			append(doubleFormatter.format(CDDes.min)).append("\t").append(doubleFormatter.format(CDDes.mean)).append("\t").
			append(doubleFormatter.format(CDDes.median)).append("\t").append(doubleFormatter_2.format(CDDes.std)).append("\t").
			append(doubleFormatter.format(CDDes.max)).append("\t").
			
			append(fdrFormatter.format(FDRDes.min)).append("\t").append(fdrFormatter.format(FDRDes.mean)).append("\t").
			append(fdrFormatter.format(FDRDes.median)).append("\t").append(fdrFormatter_2.format(FDRDes.std)).append("\t").
			append(fdrFormatter.format(FDRDes.max)).append("\n");
		}
		
		
		String comparisonFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
			+ date +"/ILPModel/Comparison_beforeReduction.txt";
		
		Logger.getInstance().setPath(comparisonFile, false);
		Logger.getInstance().write(sb.toString());
		Logger.getInstance().close();
		
	}
	
	/**2010-11-17: compare the test set before and after reduction in terms of
	 * size, context diversity, time, and test effectiveness
	 * 
	 * @param date
	 * @param criterion
	 * @param testSets_beforeReduction
	 * @param alpha_min
	 * @param alpha_max
	 * @param alpha_interval
	 * @param beta_min
	 * @param beta_max
	 * @param beta_interval
	 * @param testSet_startIndex
	 * @param testSet_endIndex
	 */
	public static void compareTestSets(String date, String criterion, ArrayList<TestSet> testSets_beforeReduction, 
			 double alpha_min, double alpha_max, double alpha_interval, 
				int beta_min, int beta_max, int beta_interval, int testSet_startIndex, int testSet_endIndex){
		
		//2010-11-17:the file to keep all reduced test sets without effectiveness information
		String reducedTestSetFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
			+ date +"/ILPModel/" + criterion + "_" + alpha_min +"_" + alpha_max + "_" 
			+ beta_min + "_" + beta_max + "_"+ testSet_startIndex+ "_"+ testSet_endIndex + ".txt";
		
		StringBuilder sb = new StringBuilder();
		sb.append("Alpha\t").append("Beta\t").
			append("Size_Min\t").append("Size_Mean\t").append("Size_Median\t").append("Size_Std\t").append("Size_Max\t").
			append("Time_Min\t").append("Time_Mean\t").append("Time_Median\t").append("Time_Std\t").append("Time_Max\t").
			append("CD_Min\t").append("CD_Mean\t").append("CD_Median\t").append("CD_Std\t").append("CD_Max\t").
			append("FDR_Min\t").append("FDR_Mean\t").append("FDR_Median\t").append("FDR_Std\t").append("FDR_Max\n");
		
		try {
			
			BufferedReader br = new BufferedReader(new FileReader(reducedTestSetFile));
			String str = null;
			
			br.readLine(); //ignore the file header
		
//			sb.append("Criterion").append("\t").append("TestSetIndex").append("\t").append("Size_beforeReduction").append("\t").
//			append("UsedProgramElems").append("\t").append("InfeasibleProgramElems").append("\t").append("EquivalentProgramElems").append("\t").
//			append("Alpha").append("\t").append("Beta").append("\t").append("ModelBuildTime").append("\t").append("SolvingTime").append("\t").
//			append("ObjectiveValue").append("\t").append("Size_reduction").append("\t").append("SelectedTestCases").append("\n");//13 items
			
			//1.load information of all test cases in a test suite
			String testPoolFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
				+ date + "/TestPool.txt";
			boolean containHeader = true;
			HashMap<String, Integer> tc_CD = Helper.getCDs(testPoolFile, containHeader);
			ICDCS10 ins = new ICDCS10();
			
			//2. parse all reduced test sets			
			HashMap<String, HashMap<Integer, ArrayList<ReducedTestSet>>> alpha_beta_reducedTestSets 
				= new HashMap<String, HashMap<Integer,ArrayList<ReducedTestSet>>>();
			
			while((str = br.readLine())!= null){
				String[] tmp = str.split("\t");
								
				String index = tmp[1];
				double time = (Double.parseDouble(tmp[8]) + Double.parseDouble(tmp[9]))*1000;
				String size_afterReduction = tmp[11];
				
				String[] testcase_afterReduction = tmp[12].split(",");
				
				
				ReducedTestSet reducedTestSet = new ReducedTestSet();

				for(String testcase: testcase_afterReduction){
					reducedTestSet.testcases.add(testcase);	
				}
				
//				Arrays.asList(testcase_afterReduction);
				
				TestSet testSet_beforeReduction=testSets_beforeReduction.get(Integer.parseInt(index));				
				
				reducedTestSet.index = testSet_beforeReduction.index;
				
				reducedTestSet.size_beforeReduction = testSet_beforeReduction.testcases.size();
				reducedTestSet.size_afterReduction = Integer.parseInt(size_afterReduction);
				
				reducedTestSet.constructingTime =  time;
				
				reducedTestSet.CD_beforeReduction = ins.getAverageCD(testSet_beforeReduction, tc_CD);
				reducedTestSet.CD_afterReduction = ins.getAverageCD(reducedTestSet, tc_CD);
				
				String alpha = tmp[6];
				int beta = Integer.parseInt(tmp[7]);
				
				HashMap<Integer, ArrayList<ReducedTestSet>> beta_reducedTestSets =alpha_beta_reducedTestSets.get(alpha);
				if(beta_reducedTestSets == null){
					beta_reducedTestSets = new HashMap<Integer, ArrayList<ReducedTestSet>>();
					alpha_beta_reducedTestSets.put(alpha, beta_reducedTestSets);
				}
				ArrayList<ReducedTestSet> testSets_afterReduction = beta_reducedTestSets.get(beta);
				if(testSets_afterReduction == null){
					testSets_afterReduction = new ArrayList<ReducedTestSet>();
					beta_reducedTestSets.put(beta, testSets_afterReduction);
				}
				testSets_afterReduction.add(reducedTestSet);				
			}
			
			//3. derive the size, time, CD, and FDR of reduced test sets with respect to each alpha and beta
			String mutantFile_date = "20100121";
			String mutantDetail_date = "20100121";
			String mutantDetailDir = System.getProperty("user.dir") + "/src/ccr"
			+ "/experiment/Context-Intensity_backup/TestHarness/" + mutantDetail_date
			+ "/Mutant/";
			
			
			DecimalFormat intFormatter = new DecimalFormat("0");
			
			DecimalFormat doubleFormatter = new DecimalFormat("0.0");
			DecimalFormat doubleFormatter_2 = new DecimalFormat("0.00");
			
			DecimalFormat fdrFormatter = new DecimalFormat("0.000");
			DecimalFormat fdrFormatter_2 = new DecimalFormat("0.0000");
			
			for(double alpha = alpha_min;  Math.abs(alpha_max - alpha) > 0.0001; alpha = alpha + alpha_interval){
				for(int beta = beta_min; beta < beta_max || (beta>beta_max && 
						(beta - beta_interval) < beta_max);beta += beta_interval){
					
					if((beta>beta_max && (beta - beta_interval) < beta_max)){
						beta = beta_max;
					}
					System.out.println("Process: Alpha("+ doubleFormatter.format(alpha)+"); Beta("+beta+")");
					
					ArrayList<ReducedTestSet> reducedTestSets = alpha_beta_reducedTestSets.get(
							doubleFormatter.format(alpha)).get(beta); 
					
					//3.1: get the statistics of size, time, CD, and fdr of reduced test sets
					ArrayList<Double> sizeArray = new ArrayList<Double>();
					ArrayList<Double> timeArray = new ArrayList<Double>();
					ArrayList<Double> contextDiversityArray = new ArrayList<Double>();
					ArrayList<Double> faultDetectionRateArray = new ArrayList<Double>();
					
					for(ReducedTestSet reducedTestSet: reducedTestSets){
						sizeArray.add(new Double(reducedTestSet.size_afterReduction));
						timeArray.add(reducedTestSet.constructingTime);
						contextDiversityArray.add(reducedTestSet.CD_afterReduction);
					}
					
					boolean containHeader_testing = true;
					boolean containHeader_mutant = false;

					HashMap<String, Double> mutant_fdr = Helper.getFaultDetectionRate_detailed(reducedTestSets, 
							mutantFile_date, containHeader_mutant, mutantDetailDir, containHeader_testing);
					Iterator ite = mutant_fdr.values().iterator();
					while(ite.hasNext()){
						faultDetectionRateArray.add((Double)ite.next());
					}
					
					DataDescriptionResult sizeDes = DataAnalyzeManager.getDataDescriptive(sizeArray);
					DataDescriptionResult timeDes = DataAnalyzeManager.getDataDescriptive(timeArray);
					DataDescriptionResult CDDes = DataAnalyzeManager.getDataDescriptive(contextDiversityArray);
					DataDescriptionResult FDRDes = DataAnalyzeManager.getDataDescriptive(faultDetectionRateArray);

//					sb.append("Alpha\t").append("Beta\t").
//					append("Size_Min\t").append("Size_Mean\t").append("Size_Median\t").append("Size_Std\t").append("Size_Max\t").
//					append("Time_Min\t").append("Time_Mean\t").append("Time_Median\t").append("Time_Std\t").append("Time_Max\t").
//					append("CD_Min\t").append("CD_Mean\t").append("CD_Median\t").append("CD_Std\t").append("CD_Max\t").
//					append("FDR_Min\t").append("FDR_Mean\t").append("FDR_Median\t").append("FDR_Std\t").append("FDR_Max\n");
				
					sb.append(doubleFormatter.format(alpha)).append("\t").append(beta).append("\t").
					
					append(intFormatter.format(sizeDes.min)).append("\t").append(intFormatter.format(sizeDes.mean)).append("\t").
					append(intFormatter.format(sizeDes.median)).append("\t").append(doubleFormatter.format(sizeDes.std)).append("\t").
					append(intFormatter.format(sizeDes.max)).append("\t").
				
					append(intFormatter.format(timeDes.min)).append("\t").append(intFormatter.format(timeDes.mean)).append("\t").
					append(intFormatter.format(timeDes.median)).append("\t").append(doubleFormatter.format(timeDes.std)).append("\t").
					append(intFormatter.format(timeDes.max)).append("\t").
					
					append(doubleFormatter.format(CDDes.min)).append("\t").append(doubleFormatter.format(CDDes.mean)).append("\t").
					append(doubleFormatter.format(CDDes.median)).append("\t").append(doubleFormatter_2.format(CDDes.std)).append("\t").
					append(doubleFormatter.format(CDDes.max)).append("\t").
					
					append(fdrFormatter.format(FDRDes.min)).append("\t").append(fdrFormatter.format(FDRDes.mean)).append("\t").
					append(fdrFormatter.format(FDRDes.median)).append("\t").append(fdrFormatter_2.format(FDRDes.std)).append("\t").
					append(fdrFormatter.format(FDRDes.max)).append("\n");
					
				}
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		String comparisonFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
			+ date +"/ILPModel/Comparison_" + criterion + "_" + alpha_min +"_" + alpha_max + "_" 
			+ beta_min + "_" + beta_max + "_"+ testSet_startIndex+ "_"+ testSet_endIndex + ".txt";
		
		Logger.getInstance().setPath(comparisonFile, false);
		Logger.getInstance().write(sb.toString());
		Logger.getInstance().close();
	}
	
	/**
	 * @param 2010/11/15: a wrapper for test suite reduction research.
	 */
	public static void main(String[] args) {
		String instruction = args[0];
		if( instruction.equals("createAndBuildILPModel")||instruction.equals("saveReductionResult")
				||instruction.equals("compareTestSets") || instruction.equals("saveTestSets_beforeReduction")){
			
			String date = "20101115";
			String criterion = "AllPolicies"; //AllPolicies, All1ResolvedDU, All2ResolvedDU
			int testSuiteSize = -1;
			String oldOrNew = "new";
			String randomOrCriterion = "random";
			String H_L_R = "H";
			int size_ART = 80;
			int testSet_startIndex = 0;
			int testSet_endIndex = 100;
			boolean containHeader = false;
			
			double alpha_min = 0.0;
			double alpha_max = 1.0;
			double alpha_interval = 0.1;
			
			int beta_min = 16;
			int beta_max = 33;
			int beta_interval = 1;
			long timeLimit = 3600000; //60*60*1000: 1 hour
			long sleepTime = 1000; //1 second
			
			
			if(args.length == 6){
				criterion = args[1];
				testSet_startIndex = Integer.parseInt(args[2]); //0
				testSet_endIndex = Integer.parseInt(args[3]); //100
				beta_min = Integer.parseInt(args[4]); //16 for AllPolicies, All1ResolvedDU, All2ResolvedDU
				beta_max = Integer.parseInt(args[5]); //33, 53, 60 for AllPolicies, All1ResolvedDU, All2ResolvedDU
			}
			
			
		
			if(instruction.equals("createAndBuildILPModel")){
				
				ArrayList<TestSet> testSets_beforeReduction = loadTestSet_offline_range(date, criterion, 
						testSuiteSize, oldOrNew, randomOrCriterion, 
						H_L_R, size_ART, containHeader, testSet_startIndex, testSet_endIndex);
				
				buildAndSolveILPs_BiCriteria_Manager(date, criterion,
						alpha_min, alpha_max, alpha_interval, beta_min, 
						beta_max, beta_interval, testSets_beforeReduction, timeLimit, sleepTime);
				
			}else if(instruction.equals("saveReductionResult")){				

				saveReductionResult(date, criterion, alpha_min, alpha_max, alpha_interval, 
						beta_min, beta_max, beta_interval, testSet_startIndex, testSet_endIndex);	
			}else if(instruction.equals("compareTestSets")){
				
				ArrayList<TestSet> testSets_beforeReduction = loadTestSet_offline_range(date, criterion, 
						testSuiteSize, oldOrNew, randomOrCriterion, 
						H_L_R, size_ART, containHeader, testSet_startIndex, testSet_endIndex);
				
				compareTestSets(date, criterion, testSets_beforeReduction, alpha_min, alpha_max, 
						alpha_interval, beta_min, beta_max, beta_interval, testSet_startIndex, testSet_endIndex);
			}else if(instruction.equals("saveTestSets_beforeReduction")){				
				String[] criteria = {"AllPolicies", "All1ResolvedDU", "All2ResolvedDU"};
				saveTestSetInfo_beforeReduction(date, criteria, testSuiteSize, oldOrNew, randomOrCriterion, H_L_R, size_ART, containHeader, testSet_startIndex, testSet_endIndex);
			}
		}
	}
	
	static class Helper{
		
		//2010-11-17: this cache is necessary since this data will be used for each combination of alpha and beta
		static HashMap<String, ArrayList<String>> mutant_validTestCases = new HashMap<String, ArrayList<String>>();
		
		public static HashMap<String, HashMap<String, ArrayList<String>>> getValidTestCases_testSet_mutant(
				ArrayList testSetArray,
				String mutantFile_date, boolean containHeader_mutant, 
				String mutantDetailDir, boolean containHeader_testing){
		
			HashMap<String, HashMap<String, ArrayList<String>>> mutant_testSet_validTestCases = new 
					HashMap<String, HashMap<String,ArrayList<String>>>();
			
			ArrayList<String> mutantArray = MutantStatistics.loadMutants_offline(mutantFile_date, containHeader_mutant);			
			String mutantDetailFile = null; 
			int tc_min = -10000;
			int tc_max = 10000;
			
			for(int i = 0; i < mutantArray.size(); i ++){
				String mutantID = mutantArray.get(i);
				
				ArrayList<String> validTestCases = new ArrayList<String>();
				if(mutant_validTestCases.containsKey(mutantID)){
					validTestCases = mutant_validTestCases.get(mutantID);
				}else{
					mutantDetailFile = mutantDetailDir + "/detailed_" + mutantID 
					+ "_" + (Integer.parseInt(mutantID) + 1) + ".txt";
					validTestCases = MutantStatistics.loadValidTestCases(mutantDetailFile, 
							containHeader_testing, tc_min, tc_max);
					mutant_validTestCases.put(mutantID, validTestCases);
				}
				
				
				//get the valid test cases in each test set to kill a given mutant
				HashMap<String, ArrayList<String>> testSet_validTC = new HashMap<String, ArrayList<String>>();
				
				for(int j = 0; j < testSetArray.size(); j++){
					TestSet ts = (TestSet)testSetArray.get(j);
					ArrayList<String> testCases = ts.testcases;
					ArrayList<String> validTCArray = TestDriver.getSharedTestCases(validTestCases, testCases);					
					testSet_validTC.put(ts.index, validTCArray);
					mutant_testSet_validTestCases.put(mutantID, testSet_validTC);
				}
			}		
			return mutant_testSet_validTestCases;
		}
		
		
		
		/**the fault detection rate of adequate test sets for each mutant.
		 * 
		 * 
		 * @param testSets
		 * @param mutantFile_date
		 * @param containHeader_mutant
		 * @param mutantDetailDir
		 * @param containHeader_testing
		 * @return: mutantID -> FDR
		 */
		public static HashMap<String, Double> getFaultDetectionRate_detailed(ArrayList testSets,
				String mutantFile_date, boolean containHeader_mutant, 
				String mutantDetailDir, boolean containHeader_testing){
			HashMap<String, Double> mutant_FDR = new HashMap<String, Double>();
			HashMap<String, HashMap<String, ArrayList<String>>> mutant_testSet_validTestCases = 
				getValidTestCases_testSet_mutant(testSets, 
						mutantFile_date, containHeader_mutant, mutantDetailDir,
						containHeader_testing);
			
			//1.calculate the FDR with respect to each mutant
			Iterator<String> ite_mutant = mutant_testSet_validTestCases.keySet().iterator();
			while(ite_mutant.hasNext()){
				String mutant = ite_mutant.next();
				HashMap<String, ArrayList<String>> testSet_validTestCases = 
					mutant_testSet_validTestCases.get(mutant);
				
				Iterator<String> ite_testSet = testSet_validTestCases.keySet().iterator();
				//2. if there is one valid test case with a test set, then this set is valid to kill this mutant
				int validTestSet_counter = 0;
				while(ite_testSet.hasNext()){
					String testSet = ite_testSet.next();
					ArrayList<String> validTestCases = testSet_validTestCases.get(testSet);
					if(validTestCases.size() > 0){
						validTestSet_counter ++;
					}
				}
				//3. FDR = the percentage of valid test sets
				double FDR = (double)validTestSet_counter/(double)testSet_validTestCases.size();
				mutant_FDR.put(mutant, FDR);
			}		
			return mutant_FDR;
		}
		
		public static double getFaultDetectionRate_average(ArrayList testSets,
				String mutantFile_date, boolean containHeader_mutant, 
				String mutantDetailDir, boolean containHeader_testing){
			
			double FDR_average = 0.0;
			
			HashMap<String, Double> mutant_FDR = getFaultDetectionRate_detailed(testSets, 
					mutantFile_date, containHeader_mutant,
					mutantDetailDir, containHeader_testing);
			
			Iterator<String> ite_mutant = mutant_FDR.keySet().iterator();
			
			double FDR_sum = 0.0;
			while(ite_mutant.hasNext()){
				String mutant = ite_mutant.next();			
				FDR_sum += mutant_FDR.get(mutant);
			}
			FDR_average = FDR_sum/mutant_FDR.size();
			
			return FDR_average; 
		}
		
		public static HashMap<String, Integer> getCDs(String testPoolFile,
				boolean containHeader){
			
			HashMap<String, Integer> tc_CD = new HashMap<String, Integer>();
			File tmp = new File(testPoolFile);
			if(tmp.exists()){
				try {
					BufferedReader br = new BufferedReader(new FileReader(testPoolFile));
					if(containHeader)
						br.readLine();
					
					String str = null;
					while((str = br.readLine())!= null){
						String[] strs = str.split("\t");
						if(strs.length >= 3){
							String tc = strs[0];						
							int CD = (int)Double.parseDouble(strs[2]);
							tc_CD.put(tc, CD);
						}					
					}
				} catch (NumberFormatException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}else{
				System.out.println("Test pool file does not exist:" + tmp.getAbsolutePath());
			}
			
			return tc_CD;
		}
		
		/**2010-11-15: return the <index,test case> pair for all test cases 
		 * in the test pool
		 * @param testcaseFile
		 * @param containHeader
		 * @return
		 */
		public static HashMap<String, TestCase> getStatisticsOfTestCase(String testcaseFile, boolean containHeader){
			HashMap<String, TestCase> id_tc = new HashMap<String, TestCase>();
			
			try {
				BufferedReader br = new BufferedReader(new FileReader(testcaseFile));
				String str = null;
				int rowNo = 0; //No. of program elements 
				if(containHeader){
					rowNo = br.readLine().split("\t").length - 6;
				}
				
				while((str=br.readLine())!= null){
					TestCase tc = new TestCase(str, true);
					id_tc.put(tc.index, tc);				
				}
				
				br.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
			return id_tc;
		}
		
		
		/**2010-11-15:
		 * 
		 * @param alpha
		 * @param tcArray
		 * @param infoFile
		 * @return
		 */
		public static String buildCoverageConstraints(double alpha, ArrayList<TestCase> tcArray, 
				String infoFile){
			
			int rowNo = tcArray.get(0).hitSet.size(); //the number of constraints is equal to the number of program elements
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
				for(int j = 0; j < tcArray.size(); j ++){
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
//			sb_constraints.append("bin "); //specify the binary variables			
//			for(int i = 0; i < tcArray.size()-1; i ++){
//				sb_constraints.append("x"+ i + ", ");
//			}
//			sb_constraints.append("x" + (tcArray.size()-1) +";");
			
			String constraintString = sb_constraints.toString();
		
		
			//save the information file
			Logger.getInstance().setPath(infoFile, false);
			Logger.getInstance().write(infoRecorder.toString());
			Logger.getInstance().close();
			
			return constraintString;
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
		
	}

}
