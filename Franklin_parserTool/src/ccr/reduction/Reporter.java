package ccr.report;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

import ccr.help.ILPOutput;
import ccr.help.MutantStatistics;
import ccr.help.TestSetStatistics;
import ccr.reduction.ILPSolver;
import ccr.test.Logger;
import ccr.test.TestDriver;
import ccr.test.TestSet;

public class Reporter_Reduction {
	//2010-01-27: here is the cache
	public static HashMap<String, ArrayList<String>> mutant_validTestCases_cache = new HashMap<String, ArrayList<String>>();
	
	
	/**2010-01-27: decouple this from getValidTestCases_testSet_mutant()
	 * to derive the failure rates of mutants
	 * 
	 * @param mutantFile_date
	 * @param containHeader_mutant
	 * @param mutantDetailDir
	 * @param containHeader_testing
	 * @return
	 */
	public static HashMap<String, ArrayList<String>> getMutant_ValidTestCases_offline(
			String mutantFile_date, boolean containHeader_mutant, 
			String mutantDetailDir, boolean containHeader_testing){
		HashMap<String, ArrayList<String>> validTestCases_mutant = new HashMap<String, ArrayList<String>>();
		
		ArrayList<String> mutantArray = MutantStatistics.loadMutants_offline(mutantFile_date, containHeader_mutant);
		String mutantDetailFile = null; 
		int tc_min = -10000;
		int tc_max = 10000;
		
		
		//1.get all valid test cases to kill a given mutant
		for(int i = 0; i < mutantArray.size(); i ++){
			String mutantID = mutantArray.get(i);
			
			System.out.println("[Reporter_Reduction.getMutant_ValidTestCases_offline]Processing faulty version:" + mutantID);
			ArrayList<String> validTestCases = null;
			
			//2010-01-27: use the cache mechanism to save execution time
			if(!mutant_validTestCases_cache.containsKey(mutantID)){
				
				mutantDetailFile = mutantDetailDir + "/detailed_" + mutantID 
				+ "_" + (Integer.parseInt(mutantID) + 1) + ".txt";
				
				validTestCases = MutantStatistics.loadValidTestCases(mutantDetailFile, 
						containHeader_testing, tc_min, tc_max);
				//save it to cache
				mutant_validTestCases_cache.put(mutantID, validTestCases);
				
			}else{
				validTestCases = mutant_validTestCases_cache.get(mutantID);
			}
			
			validTestCases_mutant.put(mutantID, validTestCases);
		}
		
		return validTestCases_mutant;
	}
	
	/**2010-01-27: decouple this method from from getValidTestCases_testSet_mutant()
	 * to reused by test suite reduction experiments
	 * 
	 * @param testSetArray
	 * @param mutantFile_date
	 * @param containHeader_mutant
	 * @param mutantDetailDir
	 * @param containHeader_testing
	 * @return
	 */
	public static HashMap<String, HashMap<String, ArrayList<String>>> getMutant_testSet_ValidTestCases_online(
			ArrayList<TestSet> testSetArray, 
			String mutantFile_date, boolean containHeader_mutant, 
			String mutantDetailDir, boolean containHeader_testing){
		
		HashMap<String, HashMap<String, ArrayList<String>>> mutant_testSet_validTestCases = new 
		HashMap<String, HashMap<String,ArrayList<String>>>();

		HashMap<String, ArrayList<String>> mutant_validTestCases = getMutant_ValidTestCases_offline(mutantFile_date, 
		containHeader_mutant, mutantDetailDir, containHeader_testing);
		

		Iterator<String> ite_mutant = mutant_validTestCases.keySet().iterator();
		while(ite_mutant.hasNext()){
			String mutantID = ite_mutant.next();
			
			//2010-01-27: use the cached mechanism to save execution time;
			HashMap<String, ArrayList<String>> testSet_validTC = new HashMap<String, ArrayList<String>>();
			
			//2010-01-28: cannot use cache here since TestSet can change frequently
				//1. get the valid test cases in each test set to kill a given mutant
				ArrayList<String> validTestCases = mutant_validTestCases.get(mutantID);
				for(int j = 0; j < testSetArray.size(); j++){
					TestSet ts = testSetArray.get(j);
					ArrayList<String> testCases = ts.testcases;
					ArrayList<String> validTCArray = TestDriver.getSharedTestCases(validTestCases,
							testCases);
					testSet_validTC.put(ts.index, validTCArray);
				}				
			
			mutant_testSet_validTestCases.put(mutantID, testSet_validTC);
		}
		
		return mutant_testSet_validTestCases;
	}
	
	/**2010-01-26: given a mutant and adequate test sets, it returns
	 * mutant -> test sets -> valid test cases (offline way is useful for 
	 * unit testing)
	 * 
	 * @param testSetFile
	 * @param containHeader_testSet
	 * @param mutantFile_date
	 * @param containHeader_mutant
	 * @param mutantDetailDir
	 * @param containHeader_testing
	 * @return
	 */
	public static HashMap<String, HashMap<String, ArrayList<String>>> getMutant_testSet_ValidTestCases_offline(
			String testSetFile, boolean containHeader_testSet,
			String mutantFile_date, boolean containHeader_mutant, 
			String mutantDetailDir, boolean containHeader_testing){
			
		ArrayList<TestSet> testSetArray = TestSetStatistics.loadTestSet_offline(
				testSetFile, containHeader_testSet);
		
		return getMutant_testSet_ValidTestCases_online(testSetArray, mutantFile_date, 
				containHeader_mutant, mutantDetailDir, containHeader_testing);
		
	}
	
	/**2010-01-27: given some test sets and some mutants, return the fault detection rate
	 * of each test set with respect to each mutant (online way is useful for test suite reduction)
	 * 
	 * @param testSets
	 * @param mutantFile_date
	 * @param containHeader_mutant
	 * @param mutantDetailDir
	 * @param containHeader_testing
	 * @return
	 */
	public static HashMap<String, Double> getFaultDetectionRate_detailed_online(ArrayList<TestSet> testSets,
			String mutantFile_date, boolean containHeader_mutant, 
			String mutantDetailDir, boolean containHeader_testing){
		
		HashMap<String, Double> mutant_FDR = new HashMap<String, Double>();
		HashMap<String, HashMap<String, ArrayList<String>>> mutant_testSet_validTestCases = 
			getMutant_testSet_ValidTestCases_online(testSets,
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
	

	/**2010-01-27: given some test sets and some mutants, return the fault detection rate
	 * of each test set with respect to each mutant (load adequate test sets from the file, 
	 * which is useful for unit testing)
	 * 
	 * @param testSetFile
	 * @param containHeader_testSet
	 * @param mutantFile_date
	 * @param containHeader_mutant
	 * @param mutantDetailDir
	 * @param containHeader_testing
	 * @return
	 */
	public static HashMap<String, Double> getFaultDetectionRate_detailed_offline(String testSetFile, boolean containHeader_testSet,
			String mutantFile_date, boolean containHeader_mutant, 
			String mutantDetailDir, boolean containHeader_testing){
		
		ArrayList<TestSet> testSets = TestSetStatistics.loadTestSet_offline(testSetFile, containHeader_testSet);
		return getFaultDetectionRate_detailed_online(testSets, mutantFile_date, containHeader_mutant, 
				mutantDetailDir, containHeader_testing);
	}
	
	/**2010-01-27: given some criterion-adequate test sets and some mutants, 
	 * return the averaged fault detection rate of the criterion with respect 
	 * to these mutants (load test sets from the file, which is useful for unit testing) 
	 * 
	 * @param testSetFile
	 * @param containHeader_testSet
	 * @param mutantFile_date
	 * @param containHeader_mutant
	 * @param mutantDetailDir
	 * @param containHeader_testing
	 * @return
	 */
	public static double getFaultDetectionRate_average_offline(String testSetFile, boolean containHeader_testSet,
			String mutantFile_date, boolean containHeader_mutant, 
			String mutantDetailDir, boolean containHeader_testing){
		
		ArrayList<TestSet> testSets = TestSetStatistics.loadTestSet_offline(testSetFile, containHeader_testSet);
		return getFaultDetectionRate_average_online(testSets, mutantFile_date,
				containHeader_mutant, mutantDetailDir, containHeader_testing);
	}
	
	/**2010-01-27: given some criterion-adequate test sets and some mutants, 
	 * return the averaged fault detection rate of the criterion with respect 
	 * to these mutants (online way is useful for test suite reduction)
	 * 
	 * @param testSets
	 * @param mutantFile_date
	 * @param containHeader_mutant
	 * @param mutantDetailDir
	 * @param containHeader_testing
	 * @return
	 */
	public static double getFaultDetectionRate_average_online(ArrayList<TestSet> testSets, 
			String mutantFile_date, boolean containHeader_mutant, 
			String mutantDetailDir, boolean containHeader_testing){
		
		double FDR_average = 0.0;
		HashMap<String, Double> mutant_FDR = getFaultDetectionRate_detailed_online(testSets,
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
	
	public static double getReductionSize_average_online(ArrayList<ILPOutput> outputs){

		double size_sum = 0.0;
		for(int i = 0; i < outputs.size(); i ++){
			ILPOutput output = outputs.get(i);
			size_sum += output.reducedTestSet.size();
		}
		double size_average = size_sum/outputs.size();
		
		return size_average;
	}
	
	public static double getTimeCost_average_online(ArrayList<ILPOutput> outputs){

		double time_sum = 0.0;
		for(int i = 0; i < outputs.size(); i ++){
			ILPOutput output = outputs.get(i);
			time_sum += output.time;
		}
		double time_average = time_sum/outputs.size();
		
		return time_average;
	}
	
	public static HashMap<Double, Double> getReductionSize_averaged_BILP_offline(String date, 
			String criterion, double alpha_min, double alpha_max, 
			double alpha_interval, boolean single_enabled){

		HashMap<Double, Double> alpha_size = new HashMap<Double, Double>();
		
		HashMap<Double, ArrayList<ILPOutput>> alpha_outputs  = new HashMap<Double, ArrayList<ILPOutput>>();
		
		ArrayList<ILPOutput> outputs = new ArrayList<ILPOutput>();
		
		String testSetDir = "";
		boolean containHeader = false;
		String pattern = "";
		if(single_enabled){

			testSetDir =  "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
				+ date + "/ILPModel/"+ criterion + "/";
			containHeader = false;

//			pattern = "Model\\_" + criterion +"\\_SingleObj\\_[0-9]+\\_output\\.txt";
			//2010-02-01:only interest in the first reduced test set
			pattern = "Model\\_" + criterion +"\\_SingleObj\\_0\\_output\\.txt";
			outputs = TestSetStatistics.loadILPOutput_offline(testSetDir, containHeader, pattern);			
		}
		
		
		//2. reduced test sets of bi-criteria ILP
		//2010-02-01: for two double we  use "|alpha_max - alpha| > 0.0001" rather than "alpha_max - alpha"
		// to determine whether alpha_max is larger than alpha or not
		for(double alpha = alpha_min; Math.abs(alpha_max - alpha) > 0.0001; alpha = alpha + alpha_interval){
			DecimalFormat format = new DecimalFormat("0.0");
			String alpha_str = format.format(alpha);
			
			testSetDir =  "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
				+ date + "/ILPModel/"+ criterion + "/";
			containHeader = false;
			
//			pattern = "Model\\_" + criterion +"\\_"+ alpha_str+"\\_[0-9]+" +
//			"\\_[0-9]+\\_output\\.txt";
			
			//2010-02-01:only interest in the first reduced test set
			pattern = "Model\\_" + criterion +"\\_"+ alpha_str+"\\_[0-9]+" +
			"\\_0\\_output\\.txt";
			
			ArrayList<ILPOutput> output_biCriteria = TestSetStatistics.loadILPOutput_offline(testSetDir, containHeader, pattern);
			
			alpha_outputs.put(alpha, output_biCriteria);			
		}
		
		
		//3. combine all test sets
		if(outputs != null){ //single-objective ILP is enabled
			alpha_outputs.put(Double.MIN_VALUE, outputs);			
		}
		
		Double[] alphas = alpha_outputs.keySet().toArray(new Double[0]);
		Arrays.sort(alphas);		
		for(int i = 0; i < alphas.length; i ++){
			double alpha = alphas[i];
			ArrayList<ILPOutput> ILPoutputs = alpha_outputs.get(alpha);
			
			double size = getReductionSize_average_online(ILPoutputs);
			alpha_size.put(alpha, size);
		}
		
		return alpha_size;
	}
	
	/**2010-02-01: get the time cost to solve ILP model with respect to various 
	 * testing criteria and weighting factors
	 * @param date
	 * @param criterion
	 * @param alpha_min
	 * @param alpha_max
	 * @param alpha_interval
	 * @param single_enabled
	 * @return
	 */
	public static HashMap<Double, Double> getTimeCost_averaged_BILP_offline(String date, 
			String criterion, double alpha_min, double alpha_max, 
			double alpha_interval, boolean single_enabled){

		HashMap<Double, Double> alpha_time = new HashMap<Double, Double>();
		
		HashMap<Double, ArrayList<ILPOutput>> alpha_outputs  = new HashMap<Double, ArrayList<ILPOutput>>();
		
		ArrayList<ILPOutput> outputs = new ArrayList<ILPOutput>();
		
		String testSetDir = "";
		boolean containHeader = false;
		String pattern = "";
		if(single_enabled){

			testSetDir =  "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
				+ date + "/ILPModel/"+ criterion + "/";
			containHeader = false;

//			pattern = "Model\\_" + criterion +"\\_SingleObj\\_[0-9]+\\_output\\.txt";
			//2010-02-01:only interest in the first reduced test set
			pattern = "Model\\_" + criterion +"\\_SingleObj\\_0\\_output\\.txt";
			outputs = TestSetStatistics.loadILPOutput_offline(testSetDir, containHeader, pattern);			
		}
		
		
		//2. reduced test sets of bi-criteria ILP
		//2010-02-01: for two double we  use "|alpha_max - alpha| > 0.0001" rather than "alpha_max - alpha"
		// to determine whether alpha_max is larger than alpha or not
		for(double alpha = alpha_min; Math.abs(alpha_max - alpha) > 0.0001; alpha = alpha + alpha_interval){
			DecimalFormat format = new DecimalFormat("0.0");
			String alpha_str = format.format(alpha);
			
			testSetDir =  "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
				+ date + "/ILPModel/"+ criterion + "/";
			containHeader = false;
			
//			pattern = "Model\\_" + criterion +"\\_"+ alpha_str+"\\_[0-9]+" +
//			"\\_[0-9]+\\_output\\.txt";
			
			//2010-02-01:only interest in the first reduced test set
			pattern = "Model\\_" + criterion +"\\_"+ alpha_str+"\\_[0-9]+" +
			"\\_0\\_output\\.txt";
			
			ArrayList<ILPOutput> output_biCriteria = TestSetStatistics.loadILPOutput_offline(testSetDir, containHeader, pattern);
			
			alpha_outputs.put(alpha, output_biCriteria);			
		}
		
		
		//3. combine all test sets
		if(outputs != null){ //single-objective ILP is enabled
			alpha_outputs.put(Double.MIN_VALUE, outputs);			
		}
		
		Double[] alphas = alpha_outputs.keySet().toArray(new Double[0]);
		Arrays.sort(alphas);		
		for(int i = 0; i < alphas.length; i ++){
			double alpha = alphas[i];
			ArrayList<ILPOutput> ILPoutputs = alpha_outputs.get(alpha);
			
			//2010-02-01: fix another important bug here
			double time = getTimeCost_average_online(ILPoutputs); 
			
			alpha_time.put(alpha, time);
		}
		
		return alpha_time;
	}
	
	
	/**2010-02-01: get averaged fault detection rates of reduced test sets
	 * with respect to a set of mutants in an offline way
	 * @param date
	 * @param criterion
	 * @param mutantFile_date
	 * @param containHeader_mutant
	 * @param mutantDetailDir
	 * @param containHeader_testing
	 * @param alpha_min
	 * @param alpha_max
	 * @param alpha_interval
	 * @param single_enabled
	 * @return
	 */
	public static HashMap<Double, Double> getFaultDetectionRate_averaged_BILP_offline(String date, String criterion,
			String mutantFile_date, boolean containHeader_mutant, 
			String mutantDetailDir, boolean containHeader_testing, double alpha_min, double alpha_max, 
			double alpha_interval, boolean single_enabled){

		HashMap<Double, Double> alpha_fdr = new HashMap<Double, Double>();
		
		HashMap<Double, ArrayList<TestSet>> alpha_testSets  = new HashMap<Double, ArrayList<TestSet>>();
		
		ArrayList<TestSet> testSets = new ArrayList<TestSet>();
		
		String testSetDir = "";
		boolean containHeader = false;
		String pattern = "";
		if(single_enabled){
//			testSets = ILPSolver.buildAndSolveILP_SingleObj_Manager(date, criterion, testSetNum, timeLimit, sleepTime);

			testSetDir =  "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
				+ date + "/ILPModel/"+ criterion + "/";
			containHeader = false;
//			pattern = "Model\\_" + criterion +"\\_SingleObj\\_[0-9]+\\_output\\.txt";
			//2010-02-01:only interest in the first reduced test set
			pattern = "Model\\_" + criterion +"\\_SingleObj\\_0\\_output\\.txt";
			testSets = TestSetStatistics.loadReducedTestSet_offline(testSetDir, containHeader, pattern);
	}
		
		
		//2. reduced test sets of bi-criteria ILP
		//2010-02-01: for two double we  use "|alpha_max - alpha| > 0.0001" rather than "alpha_max - alpha"
		// to determine whether alpha_max is larger than alpha or not
		for(double alpha = alpha_min; Math.abs(alpha_max - alpha) > 0.0001; alpha = alpha + alpha_interval){
			DecimalFormat format = new DecimalFormat("0.0");
			String alpha_str = format.format(alpha);
			
			testSetDir =  "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
				+ date + "/ILPModel/"+ criterion + "/";
			containHeader = false;
//			pattern = "Model\\_" + criterion +"\\_"+ alpha_str+"\\_[0-9]+" +
//			"\\_[0-9]+\\_output\\.txt";
			
			//2010-02-01:only interest in the first reduced test set
			pattern = "Model\\_" + criterion +"\\_"+ alpha_str+"\\_[0-9]+" +
			"\\_0\\_output\\.txt";
			
			ArrayList<TestSet> testSet_biCriteria = TestSetStatistics.loadReducedTestSet_offline(
					testSetDir, containHeader, pattern);
			
			alpha_testSets.put(alpha, testSet_biCriteria);
		}
		
		
		//3. combine all test sets
		if(testSets != null){ //single-objective ILP is enabled
			alpha_testSets.put(Double.MIN_VALUE, testSets);
		}
		
		Double[] alphas = alpha_testSets.keySet().toArray(new Double[0]);
		Arrays.sort(alphas);		
		for(int i = 0; i < alphas.length; i ++){
			double alpha = alphas[i];
			ArrayList<TestSet> reducedTestSets = alpha_testSets.get(alpha);
			
			//2010-02-01: fix another important bug here
			double fdr = getFaultDetectionRate_average_online(reducedTestSets,
					mutantFile_date, containHeader_mutant, mutantDetailDir, containHeader_testing);
			
			alpha_fdr.put(alpha, fdr);
		}
		
		return alpha_fdr;
	}
	
	/**2010-01-27: get fault detection rates of reduced test sets 
	 * with respect to a set of mutants
	 * 
	 * @param date
	 * @param criterion
	 * @param mutantFile_date
	 * @param containHeader_mutant
	 * @param mutantDetailDir
	 * @param containHeader_testing
	 * @param alpha_min
	 * @param alpha_max
	 * @param alpha_interval
	 * @param maxSize
	 * @param testSetNum:less than 1 means to use its 
	 * reduced test set sizes solved by single-objective ILP model 
	 * as the beta value
	 * @param single_enabled: true to enable the single-objective model
	 * @param timeLimit: the maximum time to solve the ILP model 
	 * @param sleepTime
	 * @return
	 */
	public static HashMap<Double, Double> getFaultDetectionRate_averaged_BILP(String date, String criterion,
			String mutantFile_date, boolean containHeader_mutant, 
			String mutantDetailDir, boolean containHeader_testing, double alpha_min, double alpha_max, 
			double alpha_interval, int maxSize, int testSetNum, boolean single_enabled, long timeLimit, long sleepTime){

		HashMap<Double, Double> alpha_fdr = new HashMap<Double, Double>();
		
		HashMap<Double, ArrayList<TestSet>> alpha_testSets  = new HashMap<Double, ArrayList<TestSet>>();
		
		ArrayList<TestSet> testSets = null;
		if(single_enabled){
			testSets = ILPSolver.buildAndSolveILP_SingleObj_Manager(date, criterion, testSetNum, timeLimit, sleepTime);
//			testSets = ILPSolver.buildAndSolveILP_SingleObj_Manager(date, criterion, testSetNum);			
		}
		
		
		if(maxSize < 1 &&  testSets != null){ 
			//1. reduced test sets of single-objective ILP							
			maxSize = testSets.get(0).size();
		}
		
		//2. reduced test sets of bi-criteria ILP
		for(int i = 1; i < maxSize; i ++){
			int maxSize_ins = i;
			alpha_testSets = ILPSolver.buildAndSolveILPs_BiCriteria_Manager(date, criterion, alpha_min, alpha_max, 
					alpha_interval, maxSize_ins, testSetNum, timeLimit, sleepTime);		
		}
			

		
		
//		alpha_testSets = ILPSolver.buildAndSolveILPs_BiCriteria_Manager(date, criterion, alpha_min, alpha_max, 
//				alpha_interval, maxSize, testSetNum);
		
		
		//3. combine all test sets
		if(testSets != null){ //single-objective ILP is enabled
			alpha_testSets.put(Double.MIN_VALUE, testSets);
		}
		
		Double[] alphas = alpha_testSets.keySet().toArray(new Double[0]);
		Arrays.sort(alphas);		
		for(int i = 0; i < alphas.length; i ++){
			double alpha = alphas[i];
			ArrayList<TestSet> reducedTestSets = alpha_testSets.get(alpha);
			
			double fdr = getFaultDetectionRate_average_online(reducedTestSets,
					mutantFile_date, containHeader_mutant, mutantDetailDir, containHeader_testing);
			
			alpha_fdr.put(alpha, fdr);
		}
		
		return alpha_fdr;
	}
	
	public static void saveToFile_alpha_fdr(HashMap<Double, Double> alpha_fdr, String saveFile){
		
		Double[] alphas = alpha_fdr.keySet().toArray(new Double[0]);
		Arrays.sort(alphas);

		StringBuilder sb = new StringBuilder();
		sb.append("Alpha\tFaultDetectionRate\n");
		
		DecimalFormat format = new DecimalFormat("0.00000");
		
		for(int i = 0; i < alphas.length; i ++){
			double alpha = alphas[i];			
			double fdr = alpha_fdr.get(alpha);
			if(alpha == Double.MIN_VALUE){
				sb.append("SingleObj").append("\t").append(format.format(fdr)).append("\n");
			}else{
				sb.append(format.format(alpha)).append("\t").append(format.format(fdr)).append("\n");	
			}
		}
		
		Logger.getInstance().setPath(saveFile, false);
		Logger.getInstance().write(sb.toString());
		Logger.getInstance().close();
	}
	
	public static void main(String[] args) {
		String instruction = args[0];
		if(instruction.equals("getFDR_offline")){
			String date_testSets = "20091026";
			String criterion = "AllPolicies";
			int testSuiteSize = -1;
			String oldOrNew = "old";
			String randomOrCriterion = "random";
			String H_L_R = "H";
			int size_ART = 64;
			String date_testPool = "20100121";
			if(args.length > 8){
				date_testSets = args[1];
				criterion = args[2];
				testSuiteSize = Integer.parseInt(args[3]);
				oldOrNew = args[4];
				randomOrCriterion = args[5];
				H_L_R = args[6];
				size_ART = Integer.parseInt(args[7]);
				date_testPool = args[8];
			}
			
			String testSetFile = TestSetStatistics.getTestSetFile(date_testSets, criterion, testSuiteSize, 
					oldOrNew, randomOrCriterion, H_L_R, size_ART);
			
			String mutantFile_date = "20100121";
			String mutantDetail_date = "20100121";
			
			if(args.length > 10){
				mutantFile_date = args[9];
				mutantDetail_date = args[10];
			}
			
			String mutantDetailDir = System.getProperty("user.dir") + "/src/ccr"
			+ "/experiment/Context-Intensity_backup/TestHarness/" + mutantDetail_date
			+ "/Mutant/";
			
			boolean containHeader_testSet = false;
			boolean containHeader_testing = true;
			boolean containHeader_mutant = false;
			
			long start = System.currentTimeMillis();
			double FDR = getFaultDetectionRate_average_offline(testSetFile, 
					containHeader_testSet, mutantFile_date, containHeader_mutant, mutantDetailDir, containHeader_testing);
			long duration = (System.currentTimeMillis() - start)/(1000*60);
			System.out.println("It takes " + duration + " mins to process all faults");
			System.out.println("the averaged fault detection rate:" + FDR);
		}else if(instruction.equals("getFDR_reduction")|| instruction.equals("getFDR_reduction_offline")){
			
			long start = System.currentTimeMillis();
			String date = args[1];			 
			//AllPolicies,All1ResolvedDU,All2ResolvedDU,AllStatement
			String criterion = args[2];
			int sizeConstraint = Integer.parseInt(args[3]);
			int testSetNum = Integer.parseInt(args[4]);
			boolean single_enabled = Boolean.parseBoolean(args[5]);
			long timeLimit = Long.parseLong(args[6]);
			long sleepTime = 1000; //1 second
			
			String mutantFile_date = "20100121";
			String mutantDetail_date = "20100121";
			boolean containHeader_mutant = false;
			boolean containHeader_testing = true;
			
			String mutantDetailDir = System.getProperty("user.dir") + "/src/ccr"
			+ "/experiment/Context-Intensity_backup/TestHarness/" + mutantDetail_date
			+ "/Mutant/";
			
			if(args.length == 9){
				mutantFile_date = args[7];
				mutantDetail_date = args[8];
			}
			
			double alpha_min = 0.0;
			double alpha_max = 1.1;
			double alpha_interval = 0.1;
			if(args.length == 10){
				alpha_min = Double.parseDouble(args[7]);
				alpha_max = Double.parseDouble(args[8]);
				alpha_interval = Double.parseDouble(args[9]);
			}

			//2010-01-31: solve the ILP model within a time limit
			HashMap<Double, Double> alpha_fdr = new HashMap<Double, Double>();
			if(instruction.equals("getFDR_reduction")){
				alpha_fdr = getFaultDetectionRate_averaged_BILP(date, 
						criterion, mutantFile_date, containHeader_mutant, mutantDetailDir, containHeader_testing, 
						alpha_min, alpha_max, alpha_interval, sizeConstraint, testSetNum, single_enabled, timeLimit, sleepTime);			
			}else{
				alpha_fdr = getFaultDetectionRate_averaged_BILP_offline(date, 
						criterion, mutantFile_date, containHeader_mutant, mutantDetailDir, containHeader_testing, 
						alpha_min, alpha_max, alpha_interval, single_enabled);
			}
			
			String saveFile = System.getProperty("user.dir") + "/src/ccr"
			+ "/experiment/Context-Intensity_backup/TestHarness/" + date
			+ "/ILPModel/"+ criterion+"/FaultDetectionRate_First.txt";
			saveToFile_alpha_fdr(alpha_fdr, saveFile);
			
			long duration = System.currentTimeMillis() - start;
			System.out.println("[Reporter_Reduction.Main]It takes " + duration/(1000*60) + " mins for " + criterion);
			System.exit(0);
		}else if(instruction.equals("getTimeCost_reduction")){
			
			String date = args[1];			 
			//AllPolicies,All1ResolvedDU,All2ResolvedDU,AllStatement
			String criterion = args[2];
			boolean single_enabled = Boolean.parseBoolean(args[5]);
			
			double alpha_min = 0.0;
			double alpha_max = 1.1;
			double alpha_interval = 0.1;
			if(args.length == 10){
				alpha_min = Double.parseDouble(args[7]);
				alpha_max = Double.parseDouble(args[8]);
				alpha_interval = Double.parseDouble(args[9]);
			}
			
			HashMap<Double, Double> alpha_time = new HashMap<Double, Double>();
			alpha_time = getTimeCost_averaged_BILP_offline(date, criterion, 
					alpha_min, alpha_max, alpha_interval, single_enabled);
			
			String saveFile = System.getProperty("user.dir") + "/src/ccr"
			+ "/experiment/Context-Intensity_backup/TestHarness/" + date
			+ "/ILPModel/"+ criterion+"/TimeCost_First.txt";
			saveToFile_alpha_fdr(alpha_time, saveFile);
		}else if(instruction.equals("getReductionSize")){
			
			String date = args[1];			 
			//AllPolicies,All1ResolvedDU,All2ResolvedDU,AllStatement
			String criterion = args[2];
			boolean single_enabled = Boolean.parseBoolean(args[5]);
			
			double alpha_min = 0.0;
			double alpha_max = 1.1;
			double alpha_interval = 0.1;
			if(args.length == 10){
				alpha_min = Double.parseDouble(args[7]);
				alpha_max = Double.parseDouble(args[8]);
				alpha_interval = Double.parseDouble(args[9]);
			}
			
			HashMap<Double, Double> alpha_time = new HashMap<Double, Double>();
			alpha_time = getTimeCost_averaged_BILP_offline(date, criterion, 
					alpha_min, alpha_max, alpha_interval, single_enabled);
			
			String saveFile = System.getProperty("user.dir") + "/src/ccr"
			+ "/experiment/Context-Intensity_backup/TestHarness/" + date
			+ "/ILPModel/"+ criterion+"/ReductionSize_First.txt";
			saveToFile_alpha_fdr(alpha_time, saveFile);
		}
	}

}
