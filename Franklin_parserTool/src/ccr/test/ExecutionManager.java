package ccr.test;

import ccr.stat.CFG;
import ccr.stat.Criterion;

public class ExecutionManager {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		System.out
				.println("USAGE: java ccr.test.TestSetManager <testSetNum(100)> <Context_Intensity> <min_CI(0.7)> "
						+ "<max_CI(0.9)> <directory(20090222)><testing criteria(AllPolicies, All1ResolvedDU, All2ResolvedDU)>"
						+ "<TestSuiteSize(58)> <randomOrCriteria(random, criteria)> [min_FaultyVersion][max_FaultyVersion]");
		String randomOrCriterion = "";


		int testSetNum = Integer.parseInt(args[0]);
		String instruction = args[1];
		double min_CI = Double.parseDouble(args[2]);
		double max_CI = Double.parseDouble(args[3]);

		String date = args[4];
		String criterion = args[5];
		int testSuiteSize = Integer.parseInt(args[6]);
		randomOrCriterion = args[7];
		int start = 1;
		int end = 141;
		if (args.length > 8) {
			start = Integer.parseInt(args[8]);
			end = Integer.parseInt(args[9]);
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

		String[] newOrOlds = new String[] { "old", "new" };
		String[] randomOrCriteria = new String[] { "random", "critera" };
		int maxTrials = 2000;

		if (instruction.equals("Context_Intensity")) {
			Adequacy.loadTestCase(testPoolFile);

			// 2009-02-22: fix the size of test suite to be 58, using
			// random-repetition to compensate the small test sets
			String appClassName = "TestCFG2_ins";
			TestSet[][] testSets = new TestSet[1][];
			String versionPackageName = "testversion";
			String saveFile;

			if (criterion.equals("AllPolicies")) {
				c = g.getAllPolicies();
				if (testSuiteSize < 0) {
					saveFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
							+ date
							+ "/allPoliciesTestSets_"
							+ newOrOlds[0]
							+ ".txt";
				} else {
					saveFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
							+ date
							+ "/allPoliciesTestSets_"
							+ newOrOlds[0]
							+ "_"
							+ randomOrCriterion
							+ "_"
							+ testSuiteSize
							+ ".txt";
				}

				// old: select test cases based on old selection strategies
				TestSetManager.getTestSets(appClassName, c, testpool,
						maxTrials, testSetNum, min_CI, max_CI, newOrOlds[0],
						randomOrCriterion, testSuiteSize, saveFile);

				// 2009-02-22: execute the generated test set
				testSets[0] = Adequacy.getTestSets(saveFile);
				Adequacy.attachTSWithCI(testSets[0], saveFile.substring(0,
						saveFile.indexOf("."))
						+ "_CI.txt");
				TestDriver.test(versionPackageName, "TestCFG2", testSets,
						saveFile.substring(0, saveFile.indexOf(".")) + "_"
								+ start + "_" + end + ".txt", start, end);

				if (testSuiteSize < 0) {
					saveFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
							+ date
							+ "/allPoliciesTestSets_"
							+ newOrOlds[1]
							+ ".txt";
				} else {
					saveFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
							+ date
							+ "/allPoliciesTestSets_"
							+ newOrOlds[1]
							+ "_"
							+ randomOrCriterion
							+ "_"
							+ testSuiteSize
							+ ".txt";
				}
				// new selection strategy
				TestSetManager.getTestSets(appClassName, c, testpool,
						maxTrials, testSetNum, min_CI, max_CI, newOrOlds[1],
						randomOrCriterion, testSuiteSize, saveFile);

				// 2009-02-22: execute the generated test set
				testSets[0] = Adequacy.getTestSets(saveFile);
				Adequacy.attachTSWithCI(testSets[0], saveFile.substring(0,
						saveFile.indexOf("."))
						+ "_CI.txt");
				TestDriver.test(versionPackageName, "TestCFG2", testSets,
						saveFile.substring(0, saveFile.indexOf(".")) + "_"
								+ start + "_" + end + ".txt", start, end);

			} else if (criterion.equals("All1ResolvedDU")) {
				c = g.getAllKResolvedDU(1);

				if (testSuiteSize < 0) {
					saveFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
							+ date
							+ "/all1ResolvedDUTestSets_"
							+ newOrOlds[0]
							+ ".txt";
				} else {
					saveFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
							+ date
							+ "/all1ResolvedDUTestSets_"
							+ newOrOlds[0]
							+ "_"
							+ randomOrCriterion
							+ "_"
							+ testSuiteSize
							+ ".txt";
				}
				// old selection strategies
				TestSetManager.getTestSets(appClassName, c, testpool,
						maxTrials, testSetNum, min_CI, max_CI, newOrOlds[0],
						randomOrCriterion, testSuiteSize, saveFile);

				// 2009-02-22: execute the generated test set
				testSets[0] = Adequacy.getTestSets(saveFile);
				Adequacy.attachTSWithCI(testSets[0], saveFile.substring(0,
						saveFile.indexOf("."))
						+ "_CI.txt");
				TestDriver.test(versionPackageName, "TestCFG2", testSets,
						saveFile.substring(0, saveFile.indexOf(".")) + "_"
								+ start + "_" + end + ".txt", start, end);

				if (testSuiteSize < 0) {
					saveFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
							+ date
							+ "/all1ResolvedDUTestSets_"
							+ newOrOlds[1]
							+ ".txt";
				} else {
					saveFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
							+ date
							+ "/all1ResolvedDUTestSets_"
							+ newOrOlds[1]
							+ "_"
							+ randomOrCriterion
							+ "_"
							+ testSuiteSize
							+ ".txt";
				}
				// new selection strategy
				TestSetManager.getTestSets(appClassName, c, testpool,
						maxTrials, testSetNum, min_CI, max_CI, newOrOlds[1],
						randomOrCriterion, testSuiteSize, saveFile);

				// 2009-02-22: execute the generated test set
				testSets[0] = Adequacy.getTestSets(saveFile);
				Adequacy.attachTSWithCI(testSets[0], saveFile.substring(0,
						saveFile.indexOf("."))
						+ "_CI.txt");
				TestDriver.test(versionPackageName, "TestCFG2", testSets,
						saveFile.substring(0, saveFile.indexOf(".")) + "_"
								+ start + "_" + end + ".txt", start, end);

			} else if (criterion.equals("All2ResolvedDU")) {
				c = g.getAllKResolvedDU(2);
				if (testSuiteSize < 0) {
					saveFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
							+ date
							+ "/all2ResolvedDUTestSets_"
							+ newOrOlds[0]
							+ ".txt";
				} else {
					saveFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
							+ date
							+ "/all2ResolvedDUTestSets_"
							+ newOrOlds[0]
							+ "_"
							+ randomOrCriterion
							+ "_"
							+ testSuiteSize
							+ ".txt";
				}

				// old selection strategies
				TestSetManager.getTestSets(appClassName, c, testpool,
						maxTrials, testSetNum, min_CI, max_CI, newOrOlds[0],
						randomOrCriterion, testSuiteSize, saveFile);
				// 2009-02-22: execute the generated test set
				testSets[0] = Adequacy.getTestSets(saveFile);
				Adequacy.attachTSWithCI(testSets[0], saveFile.substring(0,
						saveFile.indexOf("."))
						+ "_CI.txt");
				TestDriver.test(versionPackageName, "TestCFG2", testSets,
						saveFile.substring(0, saveFile.indexOf(".")) + "_"
								+ start + "_" + end + ".txt", start, end);

				if (testSuiteSize < 0) {
					saveFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
							+ date
							+ "/all2ResolvedDUTestSets_"
							+ newOrOlds[1]
							+ ".txt";
				} else {
					saveFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
							+ date
							+ "/all2ResolvedDUTestSets_"
							+ newOrOlds[1]
							+ "_"
							+ randomOrCriterion
							+ "_"
							+ testSuiteSize
							+ ".txt";
				}

				//new selection strategies
				TestSetManager.getTestSets(appClassName, c, testpool,
						maxTrials, testSetNum, min_CI, max_CI, newOrOlds[1],
						randomOrCriterion, testSuiteSize, saveFile);
				//2009-02-22: execute the generated test set
				testSets[0] = Adequacy.getTestSets(saveFile);
				Adequacy.attachTSWithCI(testSets[0], saveFile.substring(0,
						saveFile.indexOf("."))
						+ "_CI.txt");
				TestDriver.test(versionPackageName, "TestCFG2", testSets,
						saveFile.substring(0, saveFile.indexOf(".")) + "_"
								+ start + "_" + end + ".txt", start, end);

			}
		}

	}

}
