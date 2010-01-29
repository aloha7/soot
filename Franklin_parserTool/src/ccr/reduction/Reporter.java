package ccr.report;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

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
	
	/**2010-01-27: get fault detection rates of reduced test sets with respect to a set of mutants
	 * 
	 * @param date
	 * @param criterion
	 * @param mutantFile_date
	 * @param containHeader_mutant
	 * @param mutantDetailDir
	 * @param containHeader_testing
	 * @return
	 */
	public static HashMap<Double, Double> getFaultDetectionRate_averaged_BILP(String date, String criterion,
			String mutantFile_date, boolean containHeader_mutant, 
			String mutantDetailDir, boolean containHeader_testing){
		
		HashMap<Double, Double> alpha_fdr = new HashMap<Double, Double>();
		
		//1. reduced test sets of single-objective ILP
//		TestSet testSet = ILPSolver.buildAndSolveILP_SingleObj_Manager(date, criterion);
		
		//2. reduced test sets of bi-criteria ILP		
//		int maxSize = testSet.size();
		int maxSize = 60;
		HashMap<Double, TestSet> alpha_testSets = ILPSolver.buildAndSolveILPs_BiCriteria_Manager(date, criterion, maxSize);
		
		//3. combine all test sets
//		alpha_testSets.put(Double.MIN_VALUE, testSet);
		
		Double[] alphas = alpha_testSets.keySet().toArray(new Double[0]);
		Arrays.sort(alphas);
		ArrayList<TestSet> testSets = null; 
		for(int i = 0; i < alphas.length; i ++){
			double alpha = alphas[i];
			
			testSets = new ArrayList<TestSet>();
			testSets.add(alpha_testSets.get(alpha));
			
			double fdr = getFaultDetectionRate_average_online(testSets,
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
		}else if(instruction.equals("getFDR_reduction")){
			long start = System.currentTimeMillis();
			String date = args[1];
			
			//AllPolicies,All1ResolvedDU,All2ResolvedDU,AllStatement
			String criterion = args[2];
			
			String mutantFile_date = "20100121";
			String mutantDetail_date = "20100121";
			boolean containHeader_mutant = false;
			boolean containHeader_testing = true;
			
			String mutantDetailDir = System.getProperty("user.dir") + "/src/ccr"
			+ "/experiment/Context-Intensity_backup/TestHarness/" + mutantDetail_date
			+ "/Mutant/";
			
			if(args.length > 4){
				mutantFile_date = args[3];
				mutantDetail_date = args[4];
			}
			
			HashMap<Double, Double> alpha_fdr = getFaultDetectionRate_averaged_BILP(date, criterion,
					mutantFile_date,
					containHeader_mutant, mutantDetailDir, containHeader_testing);
			String saveFile = System.getProperty("user.dir") + "/src/ccr"
			+ "/experiment/Context-Intensity_backup/TestHarness/" + date
			+ "/ILPModel/"+ criterion+"/FaultDetectionRate.txt";
			saveToFile_alpha_fdr(alpha_fdr, saveFile);
			
			long duration = System.currentTimeMillis() - start;
			System.out.println("It takes " + duration/(1000*60) + " mins for " + criterion);
		}
	}

}
