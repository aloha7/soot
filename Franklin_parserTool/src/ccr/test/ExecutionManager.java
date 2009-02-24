package ccr.test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import ccr.app.Application;
import ccr.app.ApplicationResult;
import ccr.stat.CFG;
import ccr.stat.Criterion;

public class ExecutionManager {
	
	
	/**args[0]: the number of adequate test sets(100); args[1]:instruction(Context_Intensity);
	 * args[2]: min CI of test cases in test sets(0.7); args[3]: maximum CI of test cases in test sets(0.9);
	 * args[4]: the directory to save output; args[5]: testing criteria;
	 * args[6]: fix the size of test sets; args[7]: old or new test case selection strategy;
	 * args[8]: random-enhanced or repetition-enhanced; args[9]:minimum faulty version;	
	 * args[10]: maximum faulty version
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		System.out
				.println("USAGE: java ccr.test.TestSetManager <testSetNum(100)> <Context_Intensity> <min_CI(0.7)> "
						+ "<max_CI(0.9)> <directory(20090222)> <testing criteria(AllPolicies, All1ResolvedDU, All2ResolvedDU)>"
						+ "<TestSuiteSize(58)> <oldOrNew(old, new)> <randomOrCriteria(random, criteria)>[min_FaultyVersion][max_FaultyVersion]");


		int testSetNum = Integer.parseInt(args[0]);
		String instruction = args[1];
		double min_CI = Double.parseDouble(args[2]);
		double max_CI = Double.parseDouble(args[3]);

		String date = args[4];
		String criterion = args[5];
		int testSuiteSize = Integer.parseInt(args[6]);
		String oldOrNew = args[7];
		String randomOrCriterion = args[8];
		int start = 1;
		int end = 141;
		if (args.length > 8) {
			start = Integer.parseInt(args[9]);
			end = Integer.parseInt(args[10]);
		}

		CFG g = new CFG(System.getProperty("user.dir")
				+ "/src/ccr/app/TestCFG2.java");
		Criterion c;

		// 2009-2-21: revise the test case selection strategies: add a test case
		// into a test set
		// if it can increase the cumulative coverage or it has higher CI value
		// than existing one
		// while not decrease the coverage

		String testPoolFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
			+ date + "/TestPool.txt";
		TestSet testpool = TestSetManager.getTestPool(testPoolFile, true);

		String testSetFile = null; 
		int maxTrials = 2000;
		if (instruction.equals("Context_Intensity")) {
			Adequacy.loadTestCase(testPoolFile);

			// 2009-02-22: fix the size of test suite to be 58, using
			// random-repetition to compensate the small test sets
			
			TestSet[][] testSets = new TestSet[1][];

			if(testSuiteSize < 0){	
				testSetFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
					+ date
					+ "/"+criterion+"TestSets_"
					+ oldOrNew
					+ ".txt";	
			}else{
				testSetFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
					+ date
					+ "/"+criterion+"TestSets_"
					+ oldOrNew+"_"+randomOrCriterion + "_" + testSuiteSize
					+ ".txt";
			}
		}
		
		TestSet testSets[][] = new TestSet[1][];
		testSets[0] = Adequacy.getTestSets(testSetFile);
		
		String versionPackageName = "testversion";
		
		String saveFile = testSetFile.substring(0, testSetFile.indexOf(".")) + "_" + start + "_" + end + ".txt";
		
		TestDriver.test(versionPackageName, "TestCFG2", testSets, 
				saveFile, start, end);
		
	}

}
