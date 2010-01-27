package ccr.report;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import ccr.help.MutantStatistics;
import ccr.help.TestSetStatistics;
import ccr.test.TestDriver;
import ccr.test.TestSet;

public class Reporter_Reduction {
	
	public static HashMap<String, ArrayList<String>> getValidTestCases_mutant(String mutantFile_date, boolean containHeader_mutant, 
			String mutantDetailDir, boolean containHeader_testing){
		HashMap<String, ArrayList<String>> validTestCases_mutant = new HashMap<String, ArrayList<String>>();
		
		ArrayList<String> mutantArray = MutantStatistics.loadMutants_offline(mutantFile_date, containHeader_mutant);
		String mutantDetailFile = null; 
		int tc_min = -10000;
		int tc_max = 10000;
		
		long start = System.currentTimeMillis();
		//1.get all valid test cases to kill a given mutant
		for(int i = 0; i < mutantArray.size(); i ++){
			String mutantID = mutantArray.get(i);
			System.out.println("Processing faulty version:" + mutantID);
			mutantDetailFile = mutantDetailDir + "/detailed_" + mutantID 
			+ "_" + (Integer.parseInt(mutantID) + 1) + ".txt";
			ArrayList<String> validTestCases = MutantStatistics.loadValidTestCases(mutantDetailFile, 
					containHeader_testing, tc_min, tc_max);
			
			validTestCases_mutant.put(mutantID, validTestCases);
		}		
		long duration = (System.currentTimeMillis() - start)/(1000*60);
		System.out.println("It takes " + duration + " mins to process " + mutantArray.size() + " faults");
		
		return validTestCases_mutant;
	}
	
	/**2010-01-26: given a mutant and adequate test sets, it returns
	 * mutant -> test sets -> valid test cases
	 * 
	 * @param testSetFile
	 * @param containHeader_testSet
	 * @param mutantFile_date
	 * @param containHeader_mutant
	 * @param mutantDetailDir
	 * @param containHeader_testing
	 * @return
	 */
	public static HashMap<String, HashMap<String, ArrayList<String>>> getValidTestCases_testSet_mutant(String testSetFile, boolean containHeader_testSet,
			String mutantFile_date, boolean containHeader_mutant, 
			String mutantDetailDir, boolean containHeader_testing){
	
		HashMap<String, HashMap<String, ArrayList<String>>> mutant_testSet_validTestCases = new 
				HashMap<String, HashMap<String,ArrayList<String>>>();
		
		HashMap<String, ArrayList<String>> mutant_validTestCases = getValidTestCases_mutant(mutantFile_date, 
				containHeader_mutant, mutantDetailDir, containHeader_testing);
		ArrayList<TestSet> testSetArray = TestSetStatistics.loadTestSet_offline(testSetFile, containHeader_testSet);

		Iterator<String> ite_mutant = mutant_validTestCases.keySet().iterator();
		while(ite_mutant.hasNext()){
			String mutantID = ite_mutant.next();
			ArrayList<String> validTestCases = mutant_validTestCases.get(mutantID);
			
			//1. get the valid test cases in each test set to kill a given mutant
			HashMap<String, ArrayList<String>> testSet_validTC = new HashMap<String, ArrayList<String>>();
			for(int j = 0; j < testSetArray.size(); j++){
				TestSet ts = testSetArray.get(j);
				ArrayList<String> testCases = ts.testcases;
				ArrayList<String> validTCArray = TestDriver.getSharedTestCases(validTestCases, testCases);				
				testSet_validTC.put(ts.index, validTCArray);
				mutant_testSet_validTestCases.put(mutantID, testSet_validTC);
			}
		}
		
		return mutant_testSet_validTestCases;
	}
	
	public static HashMap<String, Double> getFaultDetectionRate_detailed(String testSetFile, boolean containHeader_testSet,
			String mutantFile_date, boolean containHeader_mutant, 
			String mutantDetailDir, boolean containHeader_testing){
		HashMap<String, Double> mutant_FDR = new HashMap<String, Double>();
		HashMap<String, HashMap<String, ArrayList<String>>> mutant_testSet_validTestCases = 
			getValidTestCases_testSet_mutant(testSetFile, containHeader_testSet, 
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
	
	public static double getFaultDetectionRate_average(String testSetFile, boolean containHeader_testSet,
			String mutantFile_date, boolean containHeader_mutant, 
			String mutantDetailDir, boolean containHeader_testing){
		double FDR_average = 0.0;
		HashMap<String, Double> mutant_FDR = getFaultDetectionRate_detailed(testSetFile,
				containHeader_testSet, mutantFile_date, containHeader_mutant,
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
	
	public static void main(String[] args) {
		String instruction = args[0];
		if(instruction.equals("getFDR")){
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
			
			double FDR = getFaultDetectionRate_average(testSetFile, 
					containHeader_testSet, mutantFile_date, containHeader_mutant, mutantDetailDir, containHeader_testing);
			System.out.println("the averaged fault detection rate:" + FDR);
		}
	}

}
