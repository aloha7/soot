package ccr.test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.Vector;

import ccr.stat.CFG;
import ccr.stat.Criterion;
import ccr.stat.Node;
import ccr.stat.NodeIndex;
import ccr.stat.PolicyNode;

public class TestSetManager {

	public static HashMap testCases = new HashMap();

	/**
	 * 2009-03-03: investigate the correlation between CI and coverage
	 * 
	 * @param appClassName
	 * @param testpool
	 * @param c
	 * @param iterations
	 * @param saveFile
	 */
	public static void checkCorrelation(String appClassName, TestSet testpool,
			Vector testCases, Criterion c, int iterations, String saveFile) {
		HashMap testcase_Covered = new HashMap();

		for (int i = 0; i < testCases.size(); i++) {
			String testcase = (String) testCases.get(i);
			Criterion criterion = (Criterion) c.clone();
			String stringTrace[] = TestDriver.getTrace(appClassName, testcase);
			ArrayList uniqueCover = increaseCoverage(stringTrace, criterion);
			testcase_Covered.put(testcase, uniqueCover.size());
		}

		StringBuilder sb = new StringBuilder();
		Iterator ite = testcase_Covered.keySet().iterator();
		sb.append("TestCase\t" + "CI\t" + "Covered(" + c.size() + ")\t\n");
		while (ite.hasNext()) {
			String testcase = (String) ite.next();
			double CI_testcase = ((TestCase) Adequacy.testCases.get(testcase)).CI;
			int covered = (Integer) testcase_Covered.get(testcase);
			sb.append(testcase + "\t" + CI_testcase + "\t" + covered + "\t\n");
		}

		Logger.getInstance().setPath(saveFile, false);
		Logger.getInstance().write(sb.toString());
		Logger.getInstance().close();
	}

	public static void attachTSWithCoveredElements(TestSet[] testSets,
			String appClassName, Criterion c, String saveFile) {

		StringBuilder sb = new StringBuilder();
		sb.append("TestSet" + "\t" + "Size" + "\t" + "CI" + "\t" + "minCovered"
				+ "\t" + "meanCovered" + "\t" + "maxCovered" + "\t"
				+ "SDCovered" + "\n");

		for (int j = 0; j < testSets.length; j++) {

			TestSet testSet = testSets[j];

			ArrayList coveredElements = new ArrayList();
			for (int i = 0; i < testSet.size(); i++) {
				String testcase = testSet.get(i);
				String stringTrace[] = TestDriver.getTrace(appClassName,
						testcase);
				coveredElements.add(countCoveredElements(stringTrace, c));
			}

			int minCovered = Integer.MAX_VALUE;
			int maxCovered = Integer.MIN_VALUE;

			int sum_covered = 0;
			double mean_covered = 0.0;
			double SD_covered = 0.0;

			for (int i = 0; i < coveredElements.size(); i++) {
				int covered = (Integer) coveredElements.get(i);
				if (covered > maxCovered)
					maxCovered = covered;
				if (covered < minCovered)
					minCovered = covered;

				sum_covered += covered;
			}

			mean_covered = (double) sum_covered
					/ (double) coveredElements.size();
			double sum = 0;
			for (int i = 0; i < coveredElements.size(); i++) {
				int covered = (Integer) coveredElements.get(i);
				sum = (covered - mean_covered) * (covered - mean_covered);
			}
			SD_covered = Math.sqrt(sum / (double) coveredElements.size());

			sb.append(j + "\t" + testSet.size() + "\t"
					+ ((TestCase) Adequacy.testCases.get(testSet.get(0))).CI
					+ "\t" + minCovered + "\t" + mean_covered + "\t"
					+ maxCovered + "\t" + SD_covered + "\t" + "\n");
		}
		
		// 2009-10-29:check whether the parent and file exists before
		// dumping the contents
		try {
			File file = new File(saveFile);
			if (!file.exists()) {
				File parentFile = file.getParentFile();
				if (!parentFile.exists()) {
					parentFile.mkdirs();
				}
				file.createNewFile();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		Logger.getInstance().setPath(saveFile, false);
		Logger.getInstance().write(sb.toString());
		Logger.getInstance().close();
		System.out.println(saveFile + " has been generated");
	}

	public static void attachTSWithCI(TestSet[] testSets, String saveFile) {
		StringBuilder sb = new StringBuilder();
		sb.append("TestSet" + "\t" + "Size" + "\t" + "Coverage" + "\t" + "CI"
				+ "\n");

		for (int j = 0; j < testSets.length; j++) {
			TestSet ts = testSets[j];
			double CI = Adequacy.getAverageCI(ts);
			sb.append(ts.index + "\t" + ts.size() + "\t" + ts.coverage + "\t"
					+ CI + "\n");
		}

		try {
			// 2009-10-29:check whether the parent and file exists before
			// dumping the contents
			File file = new File(saveFile);
			if (!file.exists()) {
				File parentFile = file.getParentFile();
				if (!parentFile.exists()) {
					parentFile.mkdirs();
				}
				file.createNewFile();
			}
			
			BufferedWriter bw = new BufferedWriter(new FileWriter(saveFile));
			bw.write(sb.toString());
			bw.close();
			System.out.println(saveFile + " has been generated");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * 2009-10-15: attach test set with CI and activation information
	 * 
	 * @param testSets
	 * @param saveFile
	 */
	public static void attachTSWithCI_Activation(TestSet[] testSets,
			String saveFile) {
		StringBuilder sb = new StringBuilder();
		sb.append("TestSet" + "\t" + "Size" + "\t" + "Coverage" + "\t" + "CI"
				+ "\t" + "Activation" + "\n");

		for (int j = 0; j < testSets.length; j++) {
			TestSet ts = testSets[j];
			double CI = Adequacy.getAverageCI(ts);
			double Activation = Adequacy.getAverageActivation(ts);
			sb.append(ts.index + "\t" + ts.size() + "\t" + ts.coverage + "\t"
					+ CI + "\t" + Activation + "\n");
		}

		try {
			// 2009-10-29:check whether the parent and file exists before
			// dumping the contents
			File file = new File(saveFile);
			if (!file.exists()) {
				File parentFile = file.getParentFile();
				if (!parentFile.exists()) {
					parentFile.mkdirs();
				}
				file.createNewFile();
			}
			
			BufferedWriter bw = new BufferedWriter(new FileWriter(saveFile));
			bw.write(sb.toString());
			bw.close();
			System.out.println(saveFile + " has been generated");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void attachTSWithCI_Activation_replacement(
			TestSet[] testSets, String saveFile) {
		StringBuilder sb = new StringBuilder();

		sb.append("TestSet" + "\t" + "Size" + "\t" + "Coverage" + "\t" + "CI"
				+ "\t" + "Activation" + "\t" + "Replacement" + "\n");

		for (int j = 0; j < testSets.length; j++) {
			TestSet ts = testSets[j];
			double CI = Adequacy.getAverageCI(ts);
			double Activation = Adequacy.getAverageActivation(ts);
			double Replacement = ts.replaceCounter;
			sb.append(ts.index + "\t" + ts.size() + "\t" + ts.coverage + "\t"
					+ CI + "\t" + Activation + "\t" + Replacement + "\n");
		}

		try {
			// 2009-10-29:check whether the parent and file exists before
			// dumping the contents
			File file = new File(saveFile);
			if (!file.exists()) {
				File parentFile = file.getParentFile();
				if (!parentFile.exists()) {
					parentFile.mkdirs();
				}
				file.createNewFile();
			}

			BufferedWriter bw = new BufferedWriter(new FileWriter(saveFile));
			bw.write(sb.toString());
			bw.close();
			System.out.println(saveFile + " has been generated");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/**
	 * 2009-02-22: load test sets from files
	 * 
	 * @param filename
	 * @return
	 */
	public static TestSet[] getTestSets(String filename) {
		Vector lines = new Vector();
		try {
			BufferedReader br = new BufferedReader(new FileReader(filename));
			String line = br.readLine();
			while (line != null) {
				lines.add(line);
				line = br.readLine();
			}
			br.close();
		} catch (IOException e) {
			System.out.println(e);
		}

		// 1/16/2009:Martin: the last three lines are not TestSets
		TestSet testSets[] = new TestSet[lines.size() - 3];
		for (int i = 0; i < lines.size() - 3; i++) {
			testSets[i] = new TestSet((String) lines.get(i));
		}
		return testSets;
	}

	// 2009-02-25: random test sets serve as the baseline for comparison
	public static TestSet[] getRandomTestSets(String appClassName,
			TestSet testpool, int testSetNum, int testSuiteSize, String saveFile) {
		TestSet[] testSets = new TestSet[testSetNum];
		for (int i = 0; i < testSetNum; i++) {
			testSets[i] = TestSetManager.getRandomTestSet(appClassName,
					testpool, testSuiteSize);
			testSets[i].index = "" + i;
			System.out.println("Test set " + i + ": " + testSets[i].toString());
		}

		TestSetManager.saveTestSets(testSets, saveFile);
		return testSets;
	}

	// 2009-10-16: random test sets serve as the baseline for comparison
	public static TestSet[] getRandomTestSets_refined(String appClassName,
			TestSet testpool, int testSetNum, int testSuiteSize, String H_L_R,
			int size_ART, String saveFile) {

		TestSet[] testSets = new TestSet[testSetNum];

		for (int i = 0; i < testSetNum; i++) {
			testSets[i] = TestSetManager.getRandomTestSet_refined(appClassName,
					testpool, testSuiteSize, H_L_R, size_ART);
			testSets[i].index = "" + i;
			System.out.println("Test set " + i + ": " + testSets[i].toString());
		}

		TestSetManager.saveTestSets(testSets, saveFile);
		return testSets;
	}

	/**
	 * 2009-10-16: use ART to select test cases with high, low, or random
	 * context diversity
	 * 
	 * @param appClassName
	 * @param testpool
	 * @param testSuiteSize
	 * @param H_L_R
	 * @param size_ART
	 * @return
	 */
	public static TestSet getRandomTestSet_refined(String appClassName,
			TestSet testpool, int testSuiteSize, String H_L_R, int size_ART) {
		TestSet testSet = new TestSet();
		TestSet visited = new TestSet();

		while (testSet.size() < testSuiteSize) {
			if (H_L_R.equals("R")) {
				while (testSet.size() < testSuiteSize) {
					String testcase = testpool.getByART(testSet, size_ART);// 2009-08-19:
																			// take
																			// care
																			// of
																			// this
					if (!visited.contains(testcase) /*
													 * &&
													 * !testSet.contains(testcase)
													 */) {
						visited.add(testcase);
						testSet.add(testcase);
					}
				}
			} else if (H_L_R.equals("H") || H_L_R.equals("L")) {
				String testcase = testpool.getByART(H_L_R, size_ART); // more
																		// likely
																		// to
																		// sample
																		// test
																		// cases
																		// with
																		// high
																		// CI
				if (!visited.contains(testcase)) {
					visited.add(testcase);
					testSet.add(testcase);
				}
			}
		}

		return testSet;
	}

	// 2009-02-22:
	public static TestSet getRandomTestSet(String appClassName,
			TestSet testpool, int testSuiteSize) {
		TestSet testSet = new TestSet();
		TestSet visited = new TestSet();

		while (testSet.size() < testSuiteSize) {
			String testcase = testpool.getByRandom();
			if (!visited.contains(testcase) /* && !testSet.contains(testcase) */) {
				visited.add(testcase);
				testSet.add(testcase);
			}
		}
		return testSet;
	}

	/**
	 * 2009-09-18: Get adequate test sets with refined or conventional
	 * algorithms, if refined algorithm is chosen, then which context diversity
	 * is favored: high context diversities, low context diversities, or
	 * evenly-distributed context diversities.
	 * 
	 * @param appClassName
	 * @param c
	 * @param testpool
	 * @param maxTrials
	 * @param newOrOld:
	 *            refined or conventional algorithms
	 * @param H_L_R:
	 *            high_low_evenly-distributed context diversity is chosen
	 * @param size_ART:
	 *            the size of ART-constructed test sets
	 * @return
	 */
	public static TestSet getAdequacyTestSet(String appClassName, Criterion c,
			TestSet testpool, int maxTrials, String newOrOld, String H_L_R,
			int size_ART) {
		TestSet testSet = null;
		if (newOrOld.equals("new")) {
			testSet = TestSetManager.getAdequacyTestSet_refined(appClassName,
					c, testpool, maxTrials, H_L_R, size_ART);

		} else if (newOrOld.equals("old")) {
			testSet = TestSetManager.getAdequacyTestSet_conventional(
					appClassName, c, testpool, maxTrials);

		}
		return testSet;
	}

	public static TestSet getAdequacyTestSet(String appClassName, Criterion c,
			TestSet testpool, int maxTrials, String newOrOld) {
		TestSet testSet = null;
		if (newOrOld.equals("new")) {
			testSet = TestSetManager.getAdequacyTestSet_refined(appClassName,
					c, testpool, maxTrials);
		} else if (newOrOld.equals("old")) {
			testSet = TestSetManager.getAdequacyTestSet_conventional(
					appClassName, c, testpool, maxTrials);
		}
		return testSet;
	}

	/**
	 * 2009-03-31: collect all test cases whose CI falls into the specified
	 * range from the test pool to form a test set directly, which is used to
	 * explain why the CI matters to the testing performance
	 * 
	 * 
	 */
	public static TestSet getTestSets_FixedCI(TestSet testpool, double min_CI,
			double max_CI) {
		TestSet testSet = new TestSet();
		for (int i = 0; i < testpool.size(); i++) {
			TestCase testcase = (TestCase) Adequacy.testCases
					.get((String) testpool.get(i));
			if (testcase.CI >= min_CI && testcase.CI < max_CI)
				testSet.add(testcase.index);
		}
		return testSet;
	}

	
	/**2009-10-31:construct test sets with H/L/R context diversities to see how 
	 * black-box CD information can improve the Random Testing
	 * 
	 * @param testpool
	 * @param H_L_R
	 * @param testSuiteSize
	 * @param size_ART
	 * @return
	 */
	public static TestSet getRandomTestSets_CD(TestSet testpool, String H_L_R, int testSuiteSize, int size_ART){		
		TestSet testSet = new TestSet();
		TestSet visited = new TestSet();
		long time = System.currentTimeMillis();

		if (H_L_R.equals("R")) { 
			while (visited.size() < testpool.size() && testSet.size() < testSuiteSize) {
				String testcase = testpool.getByART(testSet,
									size_ART);
				if (!visited.contains(testcase)) {
					visited.add(testcase);
				}
			}
		}else if(H_L_R.equals("H") || H_L_R.equals("L")) {
			while (visited.size() < testpool.size() && testSet.size() < testSuiteSize) {
				String testcase = testpool.getByART(H_L_R, size_ART);
				
				if (!visited.contains(testcase)) {
					visited.add(testcase);
					testSet.add(testcase);
				}
			}
		}
		testSet.geneTime = System.currentTimeMillis() - time;
		return testSet;
	}
	
	
	/**
	 * 2009-10-23:get the upper bound of CD improvement based on random
	 * algorithms
	 * 
	 * @param appClassName
	 * @param c
	 * @param testpool
	 * @param H_L_R
	 * @return
	 */
	public static TestSet getAdequacyTestSet_best(String appClassName,
			Criterion c, TestSet testpool, String H_L_R, int size_ART) {

		Criterion criterion = (Criterion) c.clone();
		TestSet testSet = new TestSet();
		TestSet visited = new TestSet();
		HashMap testcase_uniqueCovers = new HashMap();
		HashMap testcase_traces = new HashMap();

		long time = System.currentTimeMillis();

		int originalSize = criterion.size();

		if (H_L_R.equals("R")) { // RA-R: refined test suite construction
									// algorithms favoring evenly-distributed
									// context diversities
			while (testpool.size() != 0) {

				// 2009-10-25:
				String testcase = testpool.getByART_best(testSet, size_ART);

				if (!visited.contains(testcase)) {
					visited.add(testcase);
					String stringTrace[] = TestDriver.getTrace(appClassName,
							testcase);

					if (checkCoverage(stringTrace, criterion)) {
						testSet.add(testcase);
					}
				}
				testpool.remove(testcase);
			}
		} else if (H_L_R.equals("H") || H_L_R.equals("L")) {// RA-H, RA-L:
															// refined test
															// suite
															// construction
															// algorithms
															// favoring high/low
															// context diversity
			while (testpool.size() != 0) {

				// 2009-10-25:
				String testcase = testpool.getByART_best(H_L_R, size_ART);

				if (!visited.contains(testcase)) {
					visited.add(testcase);
					String stringTrace[] = TestDriver.getTrace(appClassName,
							testcase);

					ArrayList uniqueCover = increaseCoverage(stringTrace,
							criterion);
					if (uniqueCover.size() > 0) {
						testcase_uniqueCovers.put(testcase, uniqueCover);
						testSet.add(testcase);

						// 2009-09-18: execution traces
						ArrayList traces = new ArrayList();
						for (int i = 0; i < stringTrace.length; i++)
							traces.add(stringTrace[i]);
						testcase_traces.put(testcase, traces);
					} else {
						testSet = TestSetManager.replace_CI_ordering_refine(
								testSet, testcase_traces, testcase,
								stringTrace, H_L_R);
					}
				}
				testpool.remove(testcase);
			}
		}

		int currentSize = criterion.size();
		testSet.setCoverage((float) (originalSize - currentSize)
				/ (float) originalSize);
		testSet.geneTime = System.currentTimeMillis() - time;
		return testSet;

	}

	/**
	 * 2009-10-22: 2009-10-22: we enumerate all test cases in the test pool to
	 * simulate a practical upper bound of CD improvement
	 * 
	 * @param appClassName
	 * @param c
	 * @param testpool
	 * @param H_L_R
	 * @return
	 */
	public static TestSet getAdequacyTestSet_refined_best(String appClassName,
			Criterion c, TestSet testpool, String H_L_R) {

		Criterion criterion = (Criterion) c.clone();
		TestSet testSet = new TestSet();
		TestSet visited = new TestSet();
		HashMap testcase_uniqueCovers = new HashMap();
		HashMap testcase_traces = new HashMap();

		long time = System.currentTimeMillis();

		int originalSize = criterion.size();

		if (H_L_R.equals("R")) { // RA-R: refined test suite construction
									// algorithms favoring evenly-distributed
									// context diversities
			for (int i = 0; i < testpool.size(); i++) {
				String testcase = testpool.get(i);
				if (!visited.contains(testcase)) {
					visited.add(testcase);
					String stringTrace[] = TestDriver.getTrace(appClassName,
							testcase);

					if (checkCoverage(stringTrace, criterion)) {
						testSet.add(testcase);
					} else {
						testSet.add(testcase);
						testSet = testSet.removeTestCase_ART(testSet);
					}
				}
			}
		} else if (H_L_R.equals("H") || H_L_R.equals("L")) {// RA-H, RA-L:
															// refined test
															// suite
															// construction
															// algorithms
															// favoring high/low
															// context diversity
			for (int j = 0; j < testpool.size(); j++) {
				String testcase = testpool.get(j);
				if (!visited.contains(testcase)) {
					visited.add(testcase);
					String stringTrace[] = TestDriver.getTrace(appClassName,
							testcase);

					ArrayList uniqueCover = increaseCoverage(stringTrace,
							criterion);
					if (uniqueCover.size() > 0) {
						testcase_uniqueCovers.put(testcase, uniqueCover);
						testSet.add(testcase);

						// 2009-09-18: execution traces
						ArrayList traces = new ArrayList();
						for (int i = 0; i < stringTrace.length; i++)
							traces.add(stringTrace[i]);
						testcase_traces.put(testcase, traces);
					} else {
						testSet = TestSetManager.replace_CI_ordering_refine(
								testSet, testcase_traces, testcase,
								stringTrace, H_L_R);

					}
				}
			}
		}

		int currentSize = criterion.size();
		testSet.setCoverage((float) (originalSize - currentSize)
				/ (float) originalSize);
		testSet.geneTime = System.currentTimeMillis() - time;
		return testSet;

	}

	/**2009-10-30: when ties occur during test suite construction,
	 * we use DUCoverage as the first principle and CI as the second
	 * principle to solve them.
	 * @param appClassName
	 * @param c
	 * @param testpool
	 * @param maxTrials
	 * @param H_L_R
	 * @param size_ART
	 * @return
	 */
	public static TestSet getAdequacyTestSet_refined_DUCoverage(
			String appClassName, Criterion c, TestSet testpool, int maxTrials,
			String H_L_R, int size_ART) {

		Criterion criterion = (Criterion) c.clone();
		TestSet testSet = new TestSet();
		TestSet visited = new TestSet();
		HashMap testcase_uniqueCovers = new HashMap();
		HashMap testcase_traces = new HashMap(); //testcase(String)->HashMap[DUElem(String), CoverTimes(Integer)]

		long time = System.currentTimeMillis();

		int originalSize = criterion.size();

		if (H_L_R.equals("R")) { 
			// RA-R: refined test suite construction
			// algorithms favoring evenly-distributed
			// context diversities
			while (visited.size() < maxTrials
					&& visited.size() < testpool.size() && criterion.size() > 0) {
				
				String testcase = testpool.getByART(testSet,
									size_ART);//2009-08-19: take care of this

				// 2009-10-15: get a test case with the even-distributed
				// activation
//				String testcase = testpool.getByART_activation(testSet,
//						size_ART);

				if (!visited.contains(testcase)) {
					visited.add(testcase);
					String stringTrace[] = TestDriver.getTrace(appClassName,
							testcase);

					if (checkCoverage(stringTrace, criterion)) {
						testSet.add(testcase);
					}else{
						testSet.add(testcase);
						//2009-10-30: remove one existing test case
						//such that it has least Jaccard distance to
						//TestSet in terms of DUCoverage
						
					}
				}
			}
		} else if (H_L_R.equals("H") || H_L_R.equals("L")) {// RA-H, RA-L:
															// refined test
															// suite
															// construction
															// algorithms
															// favoring high/low
															// context diversity
			while (visited.size() < maxTrials
					&& visited.size() < testpool.size() && criterion.size() > 0) {

				// String testcase = testpool.getByART_activation(H_L_R,
				// size_ART);//more likely to sample test cases with high/low
				// activation
				String testcase = testpool.getByRandom();
				
//				String testcase = testpool.getByART(testSet, size_ART);

				if (!visited.contains(testcase)) {
					visited.add(testcase);
					String stringTrace[] = TestDriver.getTrace(appClassName,
							testcase);

					ArrayList uniqueCover = increaseCoverage(stringTrace,
							criterion);
					if (uniqueCover.size() > 0) {
						testcase_uniqueCovers.put(testcase, uniqueCover);
						testSet.add(testcase);

						// 2009-09-18: execution traces
						// ArrayList traces = new ArrayList();
						// for(int i = 0; i < stringTrace.length; i++)
						// traces.add(stringTrace[i]);
						//						
						// testcase_traces.put(testcase, traces);

						// 2009-10-19: execution traces have been translated
						// into DU associations
						testcase_traces.put(testcase, countDUCoverage(
								stringTrace, c));

					} else {
						// 2009-10-19: use coverage diversity to replace test
						// cases
						testSet = TestSetManager.replace_DUCoverage_refine(testSet, 
								testcase_traces, testcase, countDUCoverage(stringTrace, c));						
					}
				}
			}
		}

		int currentSize = criterion.size();
		testSet.setCoverage((float) (originalSize - currentSize)
				/ (float) originalSize);
		testSet.geneTime = System.currentTimeMillis() - time;
		return testSet;

	}
	
	/**
	 * 2009-10-14:activation rather than CI is the principle factor to replace
	 * test sets
	 * 
	 * @param appClassName
	 * @param c
	 * @param testpool
	 * @param maxTrials
	 * @param H_L_R
	 * @param size_ART
	 * @return
	 */
	public static TestSet getAdequacyTestSet_refined_activation(
			String appClassName, Criterion c, TestSet testpool, int maxTrials,
			String H_L_R, int size_ART) {

		Criterion criterion = (Criterion) c.clone();
		TestSet testSet = new TestSet();
		TestSet visited = new TestSet();
		HashMap testcase_uniqueCovers = new HashMap();
		HashMap testcase_traces = new HashMap();

		long time = System.currentTimeMillis();

		int originalSize = criterion.size();

		if (H_L_R.equals("R")) { // RA-R: refined test suite construction
									// algorithms favoring evenly-distributed
									// context diversities
			while (visited.size() < maxTrials
					&& visited.size() < testpool.size() && criterion.size() > 0) {
				 String testcase = testpool.getByART(testSet,
				 size_ART);//2009-08-19: take care of this

				// 2009-10-15: get a test case with the even-distributed
				// activation
//				String testcase = testpool.getByART_activation(testSet,
//						size_ART);

				if (!visited.contains(testcase)) {
					visited.add(testcase);
					String stringTrace[] = TestDriver.getTrace(appClassName,
							testcase);

					if (checkCoverage(stringTrace, criterion)) {
						testSet.add(testcase);
					}
				}
			}
		} else if (H_L_R.equals("H") || H_L_R.equals("L")) {// RA-H, RA-L:
															// refined test
															// suite
															// construction
															// algorithms
															// favoring high/low
															// context diversity
			while (visited.size() < maxTrials
					&& visited.size() < testpool.size() && criterion.size() > 0) {

				 String testcase = testpool.getByART_activation(H_L_R,
				 size_ART);//more likely to sample test cases with high/low activation

				if (!visited.contains(testcase)) {
					visited.add(testcase);
					String stringTrace[] = TestDriver.getTrace(appClassName,
							testcase);

					ArrayList uniqueCover = increaseCoverage(stringTrace,
							criterion);
					if (uniqueCover.size() > 0) {
						testcase_uniqueCovers.put(testcase, uniqueCover);
						testSet.add(testcase);

						// 2009-09-18: execution traces
						 ArrayList traces = new ArrayList();
						 for(int i = 0; i < stringTrace.length; i++)
						 traces.add(stringTrace[i]);
												
						 testcase_traces.put(testcase, traces);


					} else {
						// 2009-10-13: use activation rather than CI to replace
						// test cases
						 testSet =
						 TestSetManager.replace_activation_ordering_refine(
						 testSet, testcase_traces, testcase, stringTrace,
						 H_L_R);
					}
				}
			}
		}

		int currentSize = criterion.size();
		testSet.setCoverage((float) (originalSize - currentSize)
				/ (float) originalSize);
		testSet.geneTime = System.currentTimeMillis() - time;
		return testSet;

	}

	/**
	 * 2009-09-18: construct adequate test sets via RA_H, RA_L, or RA_R
	 * 
	 * @param appClassName
	 * @param c
	 * @param testpool
	 * @param maxTrials
	 * @param H_L_R:high
	 *            context diversities, low context diversities, or
	 *            even-distributed context diversities
	 * @param size_ART:
	 *            the size of ART-constructed random test set(default value=
	 *            10).
	 * @return
	 */
	public static TestSet getAdequacyTestSet_refined(String appClassName,
			Criterion c, TestSet testpool, int maxTrials, String H_L_R,
			int size_ART) {

		Criterion criterion = (Criterion) c.clone();
		TestSet testSet = new TestSet();
		TestSet visited = new TestSet();
		HashMap testcase_uniqueCovers = new HashMap();
		HashMap testcase_traces = new HashMap();

		long time = System.currentTimeMillis();

		int originalSize = criterion.size();

		if (H_L_R.equals("R")) { // RA-R: refined test suite construction
									// algorithms favoring evenly-distributed
									// context diversities
			while (visited.size() < maxTrials
					&& visited.size() < testpool.size() && criterion.size() > 0) {
				String testcase = testpool.getByART(testSet, size_ART);// 2009-08-19:
																		// take
																		// care
																		// of
																		// this
				if (!visited.contains(testcase)) {
					visited.add(testcase);
					String stringTrace[] = TestDriver.getTrace(appClassName,
							testcase);

					if (checkCoverage(stringTrace, criterion)) {
						testSet.add(testcase);
					}
				}
			}
		} else if (H_L_R.equals("H") || H_L_R.equals("L")) {// RA-H, RA-L:
															// refined test
															// suite
															// construction
															// algorithms
															// favoring high/low
															// context diversity
			while (visited.size() < maxTrials
					&& visited.size() < testpool.size() && criterion.size() > 0) {

				String testcase = testpool.getByART(H_L_R, size_ART); // more
																		// likely
																		// to
																		// sample
																		// test
																		// cases
																		// with
																		// high
																		// CI

				if (!visited.contains(testcase)) {
					visited.add(testcase);
					String stringTrace[] = TestDriver.getTrace(appClassName,
							testcase);

					ArrayList uniqueCover = increaseCoverage(stringTrace,
							criterion);
					if (uniqueCover.size() > 0) {
						testcase_uniqueCovers.put(testcase, uniqueCover);
						testSet.add(testcase);

						// 2009-09-18: execution traces
						ArrayList traces = new ArrayList();
						for (int i = 0; i < stringTrace.length; i++)
							traces.add(stringTrace[i]);
						testcase_traces.put(testcase, traces);
					} else {
						// 2009-09-18: general replacement strategy
						testSet = TestSetManager.replace_CI_ordering_refine(
								testSet, testcase_traces, testcase,
								stringTrace, H_L_R);

						// 2009-10-18: CI as the principle factor and activation
						// as the second factor
						// testSet =
						// TestSetManager.replace_CI_Activation_refine(testSet,
						// testcase_traces, testcase, stringTrace, H_L_R);
					}
				}
			}
		}

		int currentSize = criterion.size();
		testSet.setCoverage((float) (originalSize - currentSize)
				/ (float) originalSize);
		testSet.geneTime = System.currentTimeMillis() - time;
		return testSet;

	}

	/**
	 * 2009-02-21: revised test case selection strategy: add a test case if it
	 * increases the cumulative coverage or it has a higher CI value than
	 * existing one while not decrease the coverage. This process continues
	 * until 100% coverage is achieved or a upper bound on selection is achieved
	 * 
	 * @param appClassName
	 * @param c
	 * @param testpool
	 * @param maxTrials
	 * @return
	 */
	public static TestSet getAdequacyTestSet_refined(String appClassName,
			Criterion c, TestSet testpool, int maxTrials) {

		Criterion criterion = (Criterion) c.clone();
		TestSet testSet = new TestSet();
		TestSet visited = new TestSet();
		HashMap testcase_uniqueCovers = new HashMap();
		HashMap testcase_traces = new HashMap();

		long time = System.currentTimeMillis();

		int originalSize = criterion.size();
		while (visited.size() < maxTrials && visited.size() < testpool.size()
				&& criterion.size() > 0) {

			// // String testcase = testpool.getByRandom();

			String testcase = testpool.getByART(); // more likely to sample
													// test cases with high CI

			// String testcase = null;
			// if(testSet.size() == 0){
			// testcase = testpool.getByRandom();
			// }else{
			// testcase = testpool.getByART(testSet);
			// }

			// just for debugging purpose
			// TestCase testCase = (TestCase) Adequacy.testCases.get(testcase);
			if (!visited.contains(testcase)) {
				visited.add(testcase);
				String stringTrace[] = TestDriver.getTrace(appClassName,
						testcase);

				ArrayList uniqueCover = increaseCoverage(stringTrace, criterion);
				if (uniqueCover.size() > 0) {
					testcase_uniqueCovers.put(testcase, uniqueCover);
					testSet.add(testcase);

					ArrayList traces = new ArrayList();
					for (int i = 0; i < stringTrace.length; i++)
						traces.add(stringTrace[i]);

					testcase_traces.put(testcase, traces);
				} else {
					// 2009-03-06: stricted replacement strategy
					// testSet = replace_CI_ordering(testSet,
					// testcase_uniqueCovers, testcase, stringTrace);

					// 2009-03-07: general replacement strategy
					testSet = TestSetManager.replace_CI_ordering_refine(
							testSet, testcase_traces, testcase, stringTrace);
				}
			}
		}
		int currentSize = criterion.size();
		testSet.setCoverage((float) (originalSize - currentSize)
				/ (float) originalSize);
		testSet.geneTime = System.currentTimeMillis() - time;
		return testSet;

	}

	public static TestSet getAdequacyTestSet_refined_favorLowCI(
			String appClassName, Criterion c, TestSet testpool, int maxTrials) {

		Criterion criterion = (Criterion) c.clone();
		TestSet testSet = new TestSet();
		TestSet visited = new TestSet();
		HashMap testcase_uniqueCovers = new HashMap();
		HashMap testcase_traces = new HashMap();

		long time = System.currentTimeMillis();

		int originalSize = criterion.size();
		while (visited.size() < maxTrials && visited.size() < testpool.size()
				&& criterion.size() > 0) {

			String testcase = testpool.getByART(9); // more likely to sample
													// test cases with low CI

			// just for debugging purpose
			// TestCase testCase = (TestCase) Adequacy.testCases.get(testcase);
			if (!visited.contains(testcase)) {
				visited.add(testcase);
				String stringTrace[] = TestDriver.getTrace(appClassName,
						testcase);

				ArrayList uniqueCover = increaseCoverage(stringTrace, criterion);
				if (uniqueCover.size() > 0) {
					testcase_uniqueCovers.put(testcase, uniqueCover);
					testSet.add(testcase);

					ArrayList traces = new ArrayList();
					for (int i = 0; i < stringTrace.length; i++)
						traces.add(stringTrace[i]);

					testcase_traces.put(testcase, traces);
				} else {
					// 2009-03-07:
					testSet = TestSetManager.replace_highCI_ordering_refine(
							testSet, testcase_traces, testcase, stringTrace);
				}
			}
		}
		int currentSize = criterion.size();
		testSet.setCoverage((float) (originalSize - currentSize)
				/ (float) originalSize);
		testSet.geneTime = System.currentTimeMillis() - time;
		return testSet;

	}

	/**
	 * 2009-03-06: using random strategy to replace existing test case temp in
	 * test set with testcase if testcase has larger CI than temp while this
	 * replacement does not decrease the coverage
	 * 
	 * @param testSet
	 * @param testcase
	 * @return
	 */
	public static TestSet replace_CI_random(TestSet testSet,
			HashMap testcase_uniqueCovers, String testcase, String[] stringTrace) {
		// 2009-02-22: two strategies here: 1).select one and
		// replace it randomly;
		// 2).replace the one who has the lowest CI value
		// 1).select one and replace it randomly

		double CI = ((TestCase) (Adequacy.testCases.get(testcase))).CI;
		Vector testcases = testSet.testcases;
		ArrayList replaced = new ArrayList();
		// keep all test cases in the test set that has lower CI values in
		// ascending orders

		for (int i = 0; i < testcases.size(); i++) {
			// check whether some test cases have lower CI than current ones
			TestCase temp = (TestCase) Adequacy.testCases
					.get((String) testcases.get(i));
			double CI_temp = temp.CI;
			if (CI_temp < CI) {
				if (replaced.size() > 0) {
					int j = 0;
					boolean added = false;
					for (; j < replaced.size(); j++) {
						TestCase replacedTC = (TestCase) replaced.get(j);
						double CI_replacedTC = replacedTC.CI;
						if (CI_replacedTC > CI_temp) {
							replaced.add(j, temp);
							added = true;
							break;
						}
					}
					if (!added) { // add it if it has not
						replaced.add(temp);
					}
				} else {
					replaced.add(temp);
				}

			}
		}

		// just faciliate to compare
		ArrayList traceList = new ArrayList();
		for (int k = 0; k < stringTrace.length; k++)
			traceList.add(stringTrace[k]);

		ArrayList checked = new ArrayList();
		// keep all checked test cases in replace queue

		Random rand = new Random();
		int index = -1;
		while (checked.size() < replaced.size()) {
			// random select a index which has not been checked
			do {
				index = rand.nextInt(replaced.size());
			} while (checked.contains(index));

			TestCase temp = (TestCase) replaced.get(index);
			ArrayList temp_uniqueCover = (ArrayList) testcase_uniqueCovers
					.get(temp.index);

			// if a "testcase" contains all unique statements
			// covered by "temp",
			// then we can safely replace "temp" with "testcase"

			int j = 0;
			for (; j < temp_uniqueCover.size(); j++) {
				String unique = (String) temp_uniqueCover.get(j);
				if (!traceList.contains(unique))
					break;
			}
			if (j == temp_uniqueCover.size()) {
				// replace "temp" with "testcase"
				testcase_uniqueCovers.remove(temp.index);
				testcase_uniqueCovers.put(testcase, temp_uniqueCover);
				testSet.remove(temp.index);
				testSet.add(testcase);
				testSet.replaceCounter++;
				break;
			}

			checked.add(index);
		}
		return testSet;
	}

	/**
	 * 2009-03-06: replace existing test case temp in test set with testcase if
	 * t has larger CI than temp while this replacement does not decrease the
	 * coverage, favor to replace temp with lowerest CI
	 * 
	 * @param testSet
	 * @param testcase_uniqueCover:
	 *            test case- unique cover elements
	 * @param testcase
	 * @param stringTrace:
	 *            traces of test case
	 * @return
	 */
	public static TestSet replace_CI_ordering(TestSet testSet,
			HashMap testcase_uniqueCovers, String testcase, String[] stringTrace) {
		double CI = ((TestCase) (Adequacy.testCases.get(testcase))).CI;

		Vector testcases = testSet.testcases;
		ArrayList replaced = new ArrayList(); // keep all test cases in the
												// test set that has lower CI in
												// ascending orders

		for (int i = 0; i < testcases.size(); i++) {
			TestCase temp = (TestCase) Adequacy.testCases
					.get((String) testcases.get(i));
			double CI_temp = temp.CI;
			if (CI_temp < CI) {
				// add temp to replace queue which is sorted by ascending order
				// of CI
				if (replaced.size() > 0) {
					int j = 0;
					boolean added = false;
					for (; j < replaced.size(); j++) {
						TestCase replacedTC = (TestCase) replaced.get(j);
						double CI_replacedTC = replacedTC.CI;
						if (CI_replacedTC > CI_temp) {
							replaced.add(j, temp);
							added = true;
							break;
						}
					}
					if (!added) { // add it if it has not
						replaced.add(temp);
					}
				} else {
					replaced.add(temp);
				}

			}
		}

		// just faciliate to compare
		ArrayList traceList = new ArrayList();
		for (int k = 0; k < stringTrace.length; k++)
			traceList.add(stringTrace[k]);

		// 2009-02-24: replace the one who has the lowest CI value while keeping
		// coverage not decrease
		for (int i = 0; i < replaced.size(); i++) {
			TestCase temp = (TestCase) replaced.get(i);
			ArrayList temp_uniqueCover = (ArrayList) testcase_uniqueCovers
					.get(temp.index);
			// if a "testcase" contains all unique statements
			// covered by "temp",
			// then we can safely replace "temp" with "testcase"
			int j = 0;
			for (; j < temp_uniqueCover.size(); j++) {
				String unique = (String) temp_uniqueCover.get(j);
				if (!traceList.contains(unique))
					break;
			}
			if (j == temp_uniqueCover.size()) {
				// replace "temp" with "testcase"
				testcase_uniqueCovers.remove(temp.index);
				testcase_uniqueCovers.put(testcase, temp_uniqueCover);

				testSet.remove(temp.index);
				testSet.add(testcase);
				testSet.replaceCounter++;

				// 2009-02-25:if one test case can only replace
				// another one, then we need "break;"
				// otherwise, we do not need "break;"
				break;
			}
		}

		return testSet;
	}

	/**
	 * 2009-03-14: replace test cases with high-CI with lower ones
	 * 
	 * @param testSet
	 * @param testcase_traces
	 * @param testcase
	 * @param stringTrace
	 * @return
	 */
	public static TestSet replace_highCI_ordering_refine(TestSet testSet,
			HashMap testcase_traces, String testcase, String[] stringTrace) {

		double CI = ((TestCase) (Adequacy.testCases.get(testcase))).CI;

		Vector testcases = testSet.testcases;
		ArrayList replaced = new ArrayList(); // keep all test cases in the
												// test set that has higher CI
												// in ascending orders

		for (int i = 0; i < testcases.size(); i++) {
			TestCase temp = (TestCase) Adequacy.testCases
					.get((String) testcases.get(i));
			double CI_temp = temp.CI;
			if (CI_temp > CI) {
				// add temp to replace queue which is sorted by ascending order
				// of CI
				if (replaced.size() > 0) {
					int j = 0;
					boolean added = false;
					for (; j < replaced.size(); j++) {
						TestCase replacedTC = (TestCase) replaced.get(j);
						double CI_replacedTC = replacedTC.CI;
						if (CI_replacedTC < CI_temp) {
							replaced.add(j, temp);
							added = true;
							break;
						}
					}
					if (!added) { // add it if it has not
						replaced.add(temp);
					}
				} else {
					replaced.add(temp);
				}

			}
		}

		// just faciliate to compare
		ArrayList traceList = new ArrayList();
		for (int k = 0; k < stringTrace.length; k++)
			traceList.add(stringTrace[k]);

		// 2009-02-24: replace the one who has the highest CI value while
		// keeping coverage not decrease
		for (int i = 0; i < replaced.size(); i++) {
			TestCase temp = (TestCase) replaced.get(i);
			ArrayList temp_traces = (ArrayList) testcase_traces.get(temp.index);

			// keep all traces: testSet + testcase - temp
			ArrayList testSet_otherTraces = new ArrayList();
			testSet_otherTraces.addAll(traceList);
			Iterator ite = testcase_traces.keySet().iterator();
			while (ite.hasNext()) {
				String tc = (String) ite.next();
				if (!tc.equals(temp.index)) {
					testSet_otherTraces.addAll((ArrayList) testcase_traces
							.get(tc));
				}
			}

			int j = 0;
			for (; j < temp_traces.size(); j++) {
				String trace = (String) temp_traces.get(j);
				if (!testSet_otherTraces.contains(trace))
					break;
			}

			if (j == temp_traces.size()) {
				// replace "temp" with "testcase"
				testcase_traces.remove(temp.index);
				testcase_traces.put(testcase, traceList);

				testSet.remove(temp.index);
				testSet.add(testcase);
				testSet.replaceCounter++;

				break;
			}
		}

		return testSet;
	}

	/**
	 * 2009-10-15: sort replacing array according to specified index
	 * 
	 * @param replacing
	 * @param index:
	 *            either "CI" or "activation"
	 * @param H_L:
	 *            either "H" or "L"
	 * @return
	 */
	public static ArrayList sort(ArrayList replacing, String index, String H_L) {
		ArrayList temp = new ArrayList();
		for (int i = 0; i < replacing.size(); i++) {
			temp.add(replacing.get(i));
		}

		ArrayList replaced = new ArrayList();
		if (H_L.equals("L")) {
			if (index.equals("CI")) {
				while (temp.size() > 0) {
					int index_H = -1;
					double CI_H = Double.MIN_VALUE;

					for (int i = 0; i < temp.size(); i++) { // find the largest
															// CI
						if (((TestCase) temp.get(i)).CI > CI_H) {
							CI_H = ((TestCase) temp.get(i)).CI;
							index_H = i;
						}
					}
					replaced.add((TestCase) temp.get(index_H));
					temp.remove(index_H);
				} // order test cases in replaced in descending orders with
					// respect to CI
			} else if (index.equals("activation")) {
				while (temp.size() > 0) {
					int index_H = -1;
					int activation_H = Integer.MIN_VALUE;

					for (int i = 0; i < temp.size(); i++) { // find the largest
															// activation
						if (((TestCase) temp.get(i)).activation > activation_H) {
							activation_H = ((TestCase) temp.get(i)).activation;
							index_H = i;
						}
					}
					replaced.add((TestCase) temp.get(index_H));
					temp.remove(index_H);
				} // order test cases in replaced in descending orders with
					// respect to activation
			}
		} else if (H_L.equals("H")) {
			if (index.equals("CI")) {
				while (temp.size() > 0) {
					int index_L = -1;
					double CI_L = Double.MAX_VALUE;

					for (int i = 0; i < temp.size(); i++) { // find the smallest
															// CI
						if (((TestCase) temp.get(i)).CI < CI_L) {
							index_L = i;
							CI_L = ((TestCase) temp.get(i)).CI;
						}
					}
					replaced.add(temp.get(index_L));
					temp.remove(index_L);
				}
			} else if (index.equals("activation")) {
				while (temp.size() > 0) {
					int index_L = -1;
					int activation_L = Integer.MAX_VALUE;
					for (int i = 0; i < temp.size(); i++) { // find the largest
															// activation
						if (((TestCase) temp.get(i)).activation < activation_L) {
							index_L = i;
							activation_L = ((TestCase) temp.get(i)).activation;
						}
					}
					replaced.add(temp.get(index_L));
					temp.remove(index_L);
				}
			}
		}

		return replaced;
	}

	/**
	 * 2009-10-14: when ties occur, activation is the first principle to solve them
	 * and CI is the second one.
	 * 
	 * @param testSet
	 * @param testcase_traces
	 * @param testcase
	 * @param stringTrace
	 * @param H_L:
	 *            "H" if favoring test cases with high context diversities; "L"
	 *            if favoring test cases with low context diversities.
	 * @return
	 */
	public static TestSet replace_activation_ordering_refine(TestSet testSet,
			HashMap testcase_traces, String testcase, String[] stringTrace,
			String H_L) {

		int activation = ((TestCase) (Adequacy.testCases.get(testcase))).activation;

		Vector testcases = testSet.testcases;
		ArrayList replaced = new ArrayList(); // keep all test cases in the
												// test set that has lower CI in
												// ascending orders

		for (int i = 0; i < testcases.size(); i++) {
			TestCase temp = (TestCase) Adequacy.testCases
					.get((String) testcases.get(i));
			int activation_temp = temp.activation;

			if (H_L.equals("H")) {
				if (activation_temp < activation) {
					// add temp to replace queue which is sorted by ascending
					// order of activation
					replaced.add(temp);
				}
			} else if (H_L.equals("L")) {
				if (activation_temp > activation) {
					// add temp to replace queue which is sorted by descending
					// order of activation
					replaced.add(temp);
				}
			}
		}
		// 1. "replaced" keeps all test cases whose activation are low(for
		// H)/high(for L)
		replaced = sort(replaced, "activation", H_L);

		// just faciliate to compare
		ArrayList traceList = new ArrayList();
		for (int k = 0; k < stringTrace.length; k++)
			traceList.add(stringTrace[k]);

		// 2009-02-24: replace the one who has the lowest/highest activation
		// value while keeping coverage not decrease
		ArrayList candidate = new ArrayList(); // replace test cases within
												// candidate with testcase is
												// safe, but we can have the tie
												// case
		for (int i = 0; i < replaced.size(); i++) {
			TestCase temp = (TestCase) replaced.get(i);
			ArrayList temp_traces = (ArrayList) testcase_traces.get(temp.index);

			// keep all traces: testSet + testcase - temp
			ArrayList testSet_otherTraces = new ArrayList();
			testSet_otherTraces.addAll(traceList);
			Iterator ite = testcase_traces.keySet().iterator();
			while (ite.hasNext()) {
				String tc = (String) ite.next();
				if (!tc.equals(temp.index)) {
					testSet_otherTraces.addAll((ArrayList) testcase_traces
							.get(tc));
				}
			}

			int j = 0;
			for (; j < temp_traces.size(); j++) {
				String trace = (String) temp_traces.get(j);
				if (!testSet_otherTraces.contains(trace))
					break;
			}

			// 2009-10-13: testcase can replace temp without decreasing coverage
			if (j == temp_traces.size()) {
				candidate.add(temp);
			}
		}

		// 2. candidate is a subset of replaced but can replaced by testcase
		// safely in terms of no loss of coverage

		// check the tie case: with the same activation but with the different
		// CI value
		if (candidate.size() > 1) {
			int standard_activation = ((TestCase) candidate.get(0)).activation;

			ArrayList finalReplaced = new ArrayList();
			finalReplaced.add((TestCase) candidate.get(0));
			for (int i = 1; i < candidate.size(); i++) {
				if (((TestCase) candidate.get(i)).activation == standard_activation) {
					finalReplaced.add((TestCase) candidate.get(i));
				}
			}

			finalReplaced = sort(finalReplaced, "CI", H_L);

			TestCase temp = (TestCase) finalReplaced.get(0);
			testcase_traces.remove(temp.index);
			testcase_traces.put(testcase, traceList);

			testSet.remove(temp.index);
			testSet.add(testcase);
			testSet.replaceCounter++;
			if (finalReplaced.size() > 1) {
				testSet.tie_activation_CI++;
			}

		} else if (candidate.size() == 1) {
			TestCase temp = (TestCase) candidate.get(0);
			testcase_traces.remove(temp.index);
			testcase_traces.put(testcase, traceList);

			testSet.remove(temp.index);
			testSet.add(testcase);
			testSet.replaceCounter++;
		}

		return testSet;
	}

	public static ArrayList removeDuplicate(ArrayList src) {
		ArrayList temp = new ArrayList();

		for (int i = 0; i < src.size(); i++) {
			if (!temp.contains(src.get(i))) {
				temp.add(src.get(i));
			}
		}

		return temp;
	}

	/**
	 * 2009-10-19: Jaccard Distance is used to measure the diversity of two
	 * sets(e.g., covered du-sets) Jaccard distance = 1 - (A intersects B)/ (A
	 * unions B)
	 * 
	 * @param cover_elements1
	 * @param cover_elements2
	 * @return
	 */
	public static double getJaccardDistance(ArrayList cover_elements1,
			ArrayList cover_elements2) {

		// 2009-10-19: delete the duplicate elements firstly
//		cover_elements1 = removeDuplicate(cover_elements1);
//		cover_elements2 = removeDuplicate(cover_elements2);

		double JaccardDistance = 0.0;
		ArrayList union_set = new ArrayList();
		ArrayList intersect_set = new ArrayList();

		// 1. get the unions and intersections between two sets
		for (Object du : cover_elements1) {
			du = (String) du;
			if (!union_set.contains(du)) {
				union_set.add(du);
			}
			if (cover_elements2.contains(du)) {
				intersect_set.add(du);
			}
		}

		for (Object du : cover_elements2) {
			du = (String) du;
			if (!union_set.contains(du)) {
				union_set.add(du);
			}
		}

		JaccardDistance = (double)(union_set.size() - intersect_set.size())
				/ (double)union_set.size();

		return JaccardDistance;
	}

	/**
	 * 2009-10-19: use DUCoverageDiverstiy to solve the tie cases(Using
	 * du-coveredTimes)
	 * 
	 * @param testSet
	 * @param testcase_traces
	 * @param testcase
	 * @param du_covered
	 * @return
	 */
	public static TestSet replace_DUCoverage_refine(TestSet testSet,
			HashMap testcase_traces, String testcase,
			HashMap du_covered_testcase) {

		// 1. candidate set consisting of test cases whose replacement by
		// testcase won't decrease the coverage
		ArrayList candidate = new ArrayList();

		for (int i = 0; i < testSet.size(); i++) {
			// 1.1: determine whether temp should be included in candidate
			String temp = testSet.get(i);
			HashMap du_coverTime = (HashMap) testcase_traces.get(temp);

			// 1.2: all dus covered by testSet + testcase - temp;
			ArrayList dus = new ArrayList();
			dus.addAll(Arrays.asList(du_covered_testcase.keySet().toArray()));

			Iterator ite = testcase_traces.keySet().iterator();
			while (ite.hasNext()) {
				String other = (String) ite.next();
				if (!other.equals(temp)) {
					HashMap du_other = (HashMap) testcase_traces.get(other);
					
					//2009-10-30	
					String[] dus_other = (String[])du_other.keySet().toArray(new String[du_other.size()]);
					for (String du : dus_other) {
						if (!dus.contains(du)) {
							dus.add(du);
						}
					}
				}
			}

			// 1.3:temp can be added into candidate if all its dus can be
			// covered by dus
			String[] dus_temp = (String[])(du_coverTime.keySet().toArray(new String[du_coverTime.size()]));					
			int j = 0;
			for (; j < dus_temp.length; j++) {
				if (!dus.contains(dus_temp[j]))
					break;
			}

			if (j == dus_temp.length) {
				candidate.add(temp);
			}
		}
		
		if(candidate.size() > 0){
			// 2. get the Jaccard distance of testcase from testSet
			ArrayList JD_candidate = new ArrayList();

			ArrayList dus_testSet = new ArrayList();// keep all dus covered by the
													// whole test set
			ArrayList dus_testcase = new ArrayList(Arrays
					.asList((String[]) du_covered_testcase.keySet().toArray(new String[du_covered_testcase.size()])));
			Iterator its = testcase_traces.keySet().iterator();
			while(its.hasNext()){
				String temp = (String)its.next();
				HashMap dus_temp = (HashMap) testcase_traces.get(temp);		
				String[] dus = (String[]) dus_temp.keySet().toArray(new String[dus_temp.size()]);
				for (int j = 0; j < dus.length; j++) {
					String du = dus[j];
					if (!dus_testSet.contains(du)) {
						dus_testSet.add(du);
					}
				}
			}
			
			// get the Jaccard distance of testcase with respect to testSet
			JD_candidate.add(getJaccardDistance(dus_testcase, dus_testSet));

			// 3. get the Jaccard distance of test cases in candidate
			for (int i = 0; i < candidate.size(); i++) {
				String temp = (String) candidate.get(i);
				ArrayList dus_temp = new ArrayList(Arrays
						.asList(((HashMap) testcase_traces.get(temp)).keySet()
								.toArray()));

				ArrayList dus_testSet_testcase = new ArrayList(); // keep all dus
																	// covered by
																	// testSet +
																	// testcase -
																	// temp
				dus_testSet_testcase.addAll(dus_testcase);
				
				Iterator ite = testcase_traces.keySet().iterator();
				while (ite.hasNext()) {
					String other = (String) ite.next();
					if (!other.equals(temp)) {
						HashMap du_other = (HashMap) testcase_traces.get(other);
						String[] dus_other = (String[]) du_other.keySet().toArray(new String[du_other.size()]);
						for (String du : dus_other) {
							if (!dus_testSet_testcase.contains(du)) {
								dus_testSet_testcase.add(du);
							}
						}
					}
				}
				// get the Jaccard distance of temp with respect to testSet +
				// testcase - temp
				JD_candidate
						.add(getJaccardDistance(dus_temp, dus_testSet_testcase));
			}

			// 4. find the test case who owns the largest Jaccard distances
			candidate.add(0, testcase); // for consistently searching purpose(the
										// first JD in JD_candidate belongs to
										// testcase)
			int replaced_index = -1;
			double large_JD = Double.MIN_VALUE;
			for (int i = 0; i < JD_candidate.size(); i++) {
				if (((Double) JD_candidate.get(i)) > large_JD) {
					large_JD = (Double) JD_candidate.get(i);
					replaced_index = i;
				}
			}

			// 5. replace occurs between testcase and the one that owns the lowest
			// Jaccard distance
			String temp = (String) candidate.get(replaced_index);
			if (testSet.contains(temp)) { // temp may be testcase which is not a
											// member of testSet
				testSet.remove(temp);
				testSet.add(testcase);
				testcase_traces.remove(temp);
				testcase_traces.put(testcase, du_covered_testcase);
				testSet.replaceCounter++;

				// count the number of test cases who own the low_JD
				int large_JD_owner = 0;
				for (int i = 0; i < JD_candidate.size(); i++) {
					// when the differences within a range, then they are regarded
					// as the same
					if (Math.abs((Double) JD_candidate.get(i) - large_JD) < 0.001) {
						large_JD_owner++;
					}
				}
				if (large_JD_owner > 1) {
					testSet.tie_activation_CI++;
				}
			}
			
		}
		return testSet;
	}

	/**
	 * 2009-10-19: when ties occur, we use CoverageDiversity(Second elements) to
	 * solve them (Using execution traces)
	 * 
	 * @param testSet
	 * @param testcase_traces
	 * @param testcase
	 * @param stringTrace
	 * @return
	 */
	public static TestSet replace_Coverage_refine(TestSet testSet,
			HashMap testcase_traces, String testcase, String[] stringTrace) {

		// 1.keep traces of testcase
		ArrayList traceList = new ArrayList();
		for (int k = 0; k < stringTrace.length; k++)
			traceList.add(stringTrace[k]);

		// 2009-10-19: candidate sets consisting of test cases whose replacement
		// by testcase won't lose of the coverage.
		ArrayList candidate = new ArrayList(); // replace test cases within
												// candidate with testcase is
												// safe, but we can have the tie
												// case
		for (int i = 0; i < testSet.size(); i++) {
			String temp = testSet.get(i);
			ArrayList temp_traces = (ArrayList) testcase_traces.get(temp);

			// keep traces: testSet + testcase - temp
			ArrayList traces = new ArrayList();
			traces.addAll(traceList);
			Iterator ite = testcase_traces.keySet().iterator();
			while (ite.hasNext()) {
				String tc = (String) ite.next();
				if (!tc.equals(temp)) {
					traces.addAll((ArrayList) testcase_traces.get(tc));
				}
			}

			int j = 0;
			String trace;
			for (; j < temp_traces.size(); j++) {
				trace = (String) temp_traces.get(j);
				if (!traces.contains(trace))
					break;
			}

			// 2009-10-19: temp can be replaced by testcase without loss of
			// coverage
			if (j == temp_traces.size()) {
				candidate.add(temp);
			}
		}

		// the Jaccard distance of testcase
		ArrayList JD_candidate = new ArrayList();
		ArrayList traces = new ArrayList();
		Iterator ite = testcase_traces.keySet().iterator();
		while (ite.hasNext()) {
			traces.addAll((ArrayList) testcase_traces.get((String) ite.next()));
		}
		JD_candidate.add(getJaccardDistance(traces, traceList));

		// 2009-10-19: //get the Jaccard distance of candidate[i] with respect
		// to testSet + testcase - candidate[i]
		for (int i = 0; i < candidate.size(); i++) {
			traces = new ArrayList();// the size of candidate must be larger
										// than 1, otherwise testcase much
										// increase the coverage
			traces.addAll(traceList);
			ite = testcase_traces.keySet().iterator();
			while (ite.hasNext()) {
				String tc = (String) ite.next();
				if (!tc.equals(candidate.get(i))) {
					traces.addAll((ArrayList) testcase_traces.get(tc));
				}
			}
			JD_candidate.add(getJaccardDistance(traces,
					(ArrayList) testcase_traces.get(candidate.get(i))));
		}

		// 2.find the test case who owns the lowest Jaccard distances
		candidate.add(0, testcase); // for consistently searching purpose(the
									// first JD in JD_candidate belongs to
									// testcase)
		int replaced_index = -1;
		double low_JD = Double.MAX_VALUE;
		for (int i = 0; i < JD_candidate.size(); i++) {
			if (((Double) JD_candidate.get(i)) < low_JD) {
				low_JD = (Double) JD_candidate.get(i);
				replaced_index = i;
			}
		}

		// 3. replace occurs between testcase and the one that owns the lowest
		// Jaccard distance
		String temp = (String) candidate.get(replaced_index);
		if (testSet.contains(temp)) { // temp may be testcase which is not a
										// member of testSet
			testSet.remove(temp);
			testSet.add(testcase);
			testSet.replaceCounter++;

			// count the number of test cases who own the low_JD
			int low_JD_owner = 0;
			for (int i = 0; i < JD_candidate.size(); i++) {
				// when the differences within a range, then they are regarded
				// as the same
				if (Math.abs((Double) JD_candidate.get(i) - low_JD) < 0.001) {
					low_JD_owner++;
				}
			}
			if (low_JD_owner > 1) {
				testSet.tie_activation_CI++;
			}
		}
		return testSet;
	}

	/**
	 * 2009-10-15: CI as the first factor, Activation as the second factor to
	 * update existing test sets
	 * 
	 * @param testSet
	 * @param testcase_traces
	 * @param testcase
	 * @param stringTrace
	 * @param H_L
	 * @return
	 */
	public static TestSet replace_CI_Activation_refine(TestSet testSet,
			HashMap testcase_traces, String testcase, String[] stringTrace,
			String H_L) {

		double CI = ((TestCase) (Adequacy.testCases.get(testcase))).CI;

		Vector testcases = testSet.testcases;
		ArrayList replaced = new ArrayList(); // keep all test cases in the
												// test set that has lower CI in
												// ascending orders

		for (int i = 0; i < testcases.size(); i++) {
			TestCase temp = (TestCase) Adequacy.testCases
					.get((String) testcases.get(i));
			double CI_temp = temp.CI;

			if (H_L.equals("H")) {
				if (CI_temp < CI) {
					// add temp to replace queue which is sorted by ascending
					// order of activation
					replaced.add(temp);
				}
			} else if (H_L.equals("L")) {
				if (CI_temp > CI) {
					// add temp to replace queue which is sorted by descending
					// order of activation
					replaced.add(temp);
				}
			}
		}
		// 1. "replaced" keeps all test cases whose activation are low(for
		// H)/high(for L)
		replaced = sort(replaced, "CI", H_L);

		// just faciliate to compare
		ArrayList traceList = new ArrayList();
		for (int k = 0; k < stringTrace.length; k++)
			traceList.add(stringTrace[k]);

		// 2009-02-24: replace the one who has the lowest/highest activation
		// value while keeping coverage not decrease
		ArrayList candidate = new ArrayList(); // replace test cases within
												// candidate with testcase is
												// safe, but we can have the tie
												// case
		for (int i = 0; i < replaced.size(); i++) {
			TestCase temp = (TestCase) replaced.get(i);
			ArrayList temp_traces = (ArrayList) testcase_traces.get(temp.index);

			// keep all traces: testSet + testcase - temp
			ArrayList testSet_otherTraces = new ArrayList();
			testSet_otherTraces.addAll(traceList);
			Iterator ite = testcase_traces.keySet().iterator();
			while (ite.hasNext()) {
				String tc = (String) ite.next();
				if (!tc.equals(temp.index)) {
					testSet_otherTraces.addAll((ArrayList) testcase_traces
							.get(tc));
				}
			}

			int j = 0;
			for (; j < temp_traces.size(); j++) {
				String trace = (String) temp_traces.get(j);
				if (!testSet_otherTraces.contains(trace))
					break;
			}

			// 2009-10-13: testcase can replace temp without decreasing coverage
			if (j == temp_traces.size()) {
				candidate.add(temp);
			}
		}

		// 2. candidate is a subset of replaced but can replaced by testcase
		// safely in terms of no loss of coverage

		// check the tie case: with the same activation but with the different
		// CI value
		if (candidate.size() > 1) {
			double standard_CI = ((TestCase) candidate.get(0)).CI;

			ArrayList finalReplaced = new ArrayList();
			finalReplaced.add((TestCase) candidate.get(0));
			for (int i = 1; i < candidate.size(); i++) {
				if (((TestCase) candidate.get(i)).CI == standard_CI) {
					finalReplaced.add((TestCase) candidate.get(i));
				}
			}

			finalReplaced = sort(finalReplaced, "activation", H_L);

			TestCase temp = (TestCase) finalReplaced.get(0);
			testcase_traces.remove(temp.index);
			testcase_traces.put(testcase, traceList);

			testSet.remove(temp.index);
			testSet.add(testcase);
			testSet.replaceCounter++;
			if (finalReplaced.size() > 1) {
				testSet.tie_activation_CI++;
			}

		} else if (candidate.size() == 1) {
			TestCase temp = (TestCase) candidate.get(0);
			testcase_traces.remove(temp.index);
			testcase_traces.put(testcase, traceList);

			testSet.remove(temp.index);
			testSet.add(testcase);
			testSet.replaceCounter++;
		}
		return testSet;
	}

	/**
	 * 2009-09-18: replace existing test cases within TestSet with a TestCase
	 * with High/low CI
	 * 
	 * @param testSet:
	 *            existing test set
	 * @param testcase_traces:
	 *            execution traces of all test cases in the test set
	 * @param testcase:
	 *            test cases to be replaced
	 * @param stringTrace:
	 *            execution traces of the replaced test case
	 * @param H_L:
	 *            "H" if favoring test cases with high context diversities; "L"
	 *            if favoring test cases with low context diversities.
	 * @return
	 */
	public static TestSet replace_CI_ordering_refine(TestSet testSet,
			HashMap testcase_traces, String testcase, String[] stringTrace,
			String H_L) {

		double CI = ((TestCase) (Adequacy.testCases.get(testcase))).CI;

		Vector testcases = testSet.testcases;
		ArrayList replaced = new ArrayList(); // keep all test cases in the
												// test set that has lower CI in
												// ascending orders

		for (int i = 0; i < testcases.size(); i++) {
			TestCase temp = (TestCase) Adequacy.testCases
					.get((String) testcases.get(i));
			double CI_temp = temp.CI;

			if (H_L.equals("H")) {
				if (CI_temp < CI) {
					// add temp to replace queue which is sorted by ascending
					// order of CI
					if (replaced.size() > 0) {
						int j = 0;
						boolean added = false;
						for (; j < replaced.size(); j++) {
							TestCase replacedTC = (TestCase) replaced.get(j);
							double CI_replacedTC = replacedTC.CI;
							if (CI_replacedTC > CI_temp) {
								replaced.add(j, temp);
								added = true;
								break;
							}
						}
						if (!added) { // add it if it has not
							replaced.add(temp);
						}
					} else {
						replaced.add(temp);
					}
				}
			} else if (H_L.equals("L")) {
				if (CI_temp > CI) {
					// add temp to replace queue which is sorted by descending
					// order of CI
					if (replaced.size() > 0) {
						int j = 0;
						boolean added = false;
						for (; j < replaced.size(); j++) {
							TestCase replacedTC = (TestCase) replaced.get(j);
							double CI_replacedTC = replacedTC.CI;
							if (CI_replacedTC < CI_temp) {
								replaced.add(j, temp);
								added = true;
								break;
							}
						}
						if (!added) { // add it if it has not
							replaced.add(temp);
						}
					} else {
						replaced.add(temp);
					}
				}
			}
		}

		// just faciliate to compare
		ArrayList traceList = new ArrayList();
		for (int k = 0; k < stringTrace.length; k++)
			traceList.add(stringTrace[k]);

		// 2009-02-24: replace the one who has the lowest/highest CI value while
		// keeping coverage not decrease
		for (int i = 0; i < replaced.size(); i++) {
			TestCase temp = (TestCase) replaced.get(i);
			ArrayList temp_traces = (ArrayList) testcase_traces.get(temp.index);

			// keep all traces: testSet + testcase - temp
			ArrayList testSet_otherTraces = new ArrayList();
			testSet_otherTraces.addAll(traceList);
			Iterator ite = testcase_traces.keySet().iterator();
			while (ite.hasNext()) {
				String tc = (String) ite.next();
				if (!tc.equals(temp.index)) {
					testSet_otherTraces.addAll((ArrayList) testcase_traces
							.get(tc));
				}
			}

			int j = 0;
			for (; j < temp_traces.size(); j++) {
				String trace = (String) temp_traces.get(j);
				if (!testSet_otherTraces.contains(trace))
					break;
			}

			if (j == temp_traces.size()) {
				// replace "temp" with "testcase"
				testcase_traces.remove(temp.index);
				testcase_traces.put(testcase, traceList);

				testSet.remove(temp.index);
				testSet.add(testcase);
				testSet.replaceCounter++;

				break;
			}
		}

		return testSet;
	}

	/**
	 * 2009-03-07:
	 * 
	 * @param testSet
	 * @param testcase_traces
	 * @param testcase
	 * @param stringTrace
	 * @return
	 */
	public static TestSet replace_CI_ordering_refine(TestSet testSet,
			HashMap testcase_traces, String testcase, String[] stringTrace) {

		double CI = ((TestCase) (Adequacy.testCases.get(testcase))).CI;

		Vector testcases = testSet.testcases;
		ArrayList replaced = new ArrayList(); // keep all test cases in the
												// test set that has lower CI in
												// ascending orders

		for (int i = 0; i < testcases.size(); i++) {
			TestCase temp = (TestCase) Adequacy.testCases
					.get((String) testcases.get(i));
			double CI_temp = temp.CI;
			if (CI_temp < CI) {
				// add temp to replace queue which is sorted by ascending order
				// of CI
				if (replaced.size() > 0) {
					int j = 0;
					boolean added = false;
					for (; j < replaced.size(); j++) {
						TestCase replacedTC = (TestCase) replaced.get(j);
						double CI_replacedTC = replacedTC.CI;
						if (CI_replacedTC > CI_temp) {
							replaced.add(j, temp);
							added = true;
							break;
						}
					}
					if (!added) { // add it if it has not
						replaced.add(temp);
					}
				} else {
					replaced.add(temp);
				}

			}
		}

		// just faciliate to compare
		ArrayList traceList = new ArrayList();
		for (int k = 0; k < stringTrace.length; k++)
			traceList.add(stringTrace[k]);

		// 2009-02-24: replace the one who has the lowest CI value while keeping
		// coverage not decrease
		for (int i = 0; i < replaced.size(); i++) {
			TestCase temp = (TestCase) replaced.get(i);
			ArrayList temp_traces = (ArrayList) testcase_traces.get(temp.index);

			// keep all traces: testSet + testcase - temp
			ArrayList testSet_otherTraces = new ArrayList();
			testSet_otherTraces.addAll(traceList);
			Iterator ite = testcase_traces.keySet().iterator();
			while (ite.hasNext()) {
				String tc = (String) ite.next();
				if (!tc.equals(temp.index)) {
					testSet_otherTraces.addAll((ArrayList) testcase_traces
							.get(tc));
				}
			}

			int j = 0;
			for (; j < temp_traces.size(); j++) {
				String trace = (String) temp_traces.get(j);
				if (!testSet_otherTraces.contains(trace))
					break;
			}

			if (j == temp_traces.size()) {
				// replace "temp" with "testcase"
				testcase_traces.remove(temp.index);
				testcase_traces.put(testcase, traceList);

				testSet.remove(temp.index);
				testSet.add(testcase);
				testSet.replaceCounter++;

				break;
			}
		}
		return testSet;
	}

	public static TestSet getAdequacyTestSet_refined_fixCI(String appClassName,
			Criterion c, TestSet testpool, int maxTrials, double min_CI,
			double max_CI) {
		Criterion criterion = (Criterion) c.clone();
		TestSet testSet = new TestSet();
		TestSet visited = new TestSet();
		HashMap testcase_uniqueCovers = new HashMap();
		HashMap testcase_traces = new HashMap();
		int trials = 0;
		long time = System.currentTimeMillis();

		int originalSize = criterion.size();
		while (trials < maxTrials && visited.size() < maxTrials
				&& visited.size() < testpool.size() && criterion.size() > 0) {

			// String testcase = testpool.getByART(); //more likely to sample
			// test cases with high CI

			// String testcase = testpool.getByRandom(min_CI, max_CI, 100);
			String testcase = testpool.getByRandom(min_CI, max_CI);

			TestCase temp = (TestCase) Adequacy.testCases.get(testcase);

			trials++;
			if (!visited.contains(testcase)) {
				visited.add(testcase);
				String stringTrace[] = TestDriver.getTrace(appClassName,
						testcase);

				ArrayList uniqueCover = increaseCoverage(stringTrace, criterion);
				if (uniqueCover.size() > 0) {
					testcase_uniqueCovers.put(testcase, uniqueCover);
					testSet.add(testcase);

					ArrayList traces = new ArrayList();
					for (int i = 0; i < stringTrace.length; i++)
						traces.add(stringTrace[i]);

					testcase_traces.put(testcase, traces);
				} /*
					 * else { //2009-03-06: testSet =
					 * replace_CI_ordering(testSet, testcase_uniqueCovers,
					 * testcase, stringTrace);
					 * 
					 * //2009-03-07: // testSet =
					 * TestSetManager.replace_CI_ordering_refine(testSet, //
					 * testcase_traces, testcase, stringTrace); }
					 */
			}
		}
		int currentSize = criterion.size();
		testSet.setCoverage((float) (originalSize - currentSize)
				/ (float) originalSize);
		testSet.geneTime = System.currentTimeMillis() - time;
		return testSet;
	}

	public static TestSet getAdequacyTestSet_refined_fixSize_favorLowCI(
			String appClassName, Criterion c, TestSet testpool, int maxTrials,
			int testSuiteSize, String randomOrCriteria) {

		Criterion criterion = (Criterion) c.clone();
		Criterion finalCriterion = null;
		TestSet testSet = new TestSet();
		TestSet visited = new TestSet();
		HashMap testcase_uniqueCovers = new HashMap();
		HashMap testcase_traces = new HashMap();

		long time = System.currentTimeMillis();

		int originalSize = criterion.size();
		while (visited.size() < maxTrials && visited.size() < testpool.size()
				&& criterion.size() > 0 && testSet.size() < testSuiteSize) {

			String testcase = testpool.getByART(9);

			// just for debugging purpose
			// TestCase testCase = (TestCase) Adequacy.testCases.get(testcase);

			if (!visited.contains(testcase)) {
				visited.add(testcase);
				String stringTrace[] = TestDriver.getTrace(appClassName,
						testcase);

				ArrayList uniqueCover = increaseCoverage(stringTrace, criterion);
				if (uniqueCover.size() > 0) {
					testcase_uniqueCovers.put(testcase, uniqueCover);
					testSet.add(testcase);

					ArrayList traces = new ArrayList();
					for (int i = 0; i < stringTrace.length; i++)
						traces.add(stringTrace[i]);

					testcase_traces.put(testcase, traces);

				} else {
					testSet = TestSetManager.replace_highCI_ordering_refine(
							testSet, testcase_traces, testcase, stringTrace);
				}
			}
		}

		finalCriterion = (Criterion) criterion.clone(); // save the criterion to
		if (randomOrCriteria.equals("random")) {
			// 2009-02-22:the stopping rule is very important here:
			while (testSet.size() < testSuiteSize
					&& visited.size() < testpool.size()) {
				String testcase = testpool.getByART(9);

				if (!visited.contains(testcase) && !testSet.contains(testcase)) {
					String stringTrace[] = TestDriver.getTrace(appClassName,
							testcase);
					checkCoverage(stringTrace, finalCriterion); // remove
					// redundant
					// coverage
					visited.add(testcase);
					testSet.add(testcase);
				}
			}
		} else if (randomOrCriteria.equals("criteria")) {
			finalCriterion = (Criterion) criterion.clone(); // save the
			// criterion to
			// calculate the
			// final coverage

			do {
				int trial = 0;
				maxTrials = 100;
				criterion = (Criterion) c.clone();

				while (trial < maxTrials && visited.size() < testpool.size()
						&& criterion.size() > 0
						&& testSet.size() < testSuiteSize) {

					String testcase = testpool.getByART(9);

					trial++;
					// just for debugging purpose
					// TestCase testCase = (TestCase)
					// Adequacy.testCases.get(testcase);

					if (!visited.contains(testcase)) {
						visited.add(testcase);
						String stringTrace[] = TestDriver.getTrace(
								appClassName, testcase);

						ArrayList uniqueCover = increaseCoverage(stringTrace,
								criterion);
						if (uniqueCover.size() > 0) {
							testcase_uniqueCovers.put(testcase, uniqueCover);
							testSet.add(testcase);

							ArrayList traces = new ArrayList();
							for (int i = 0; i < stringTrace.length; i++)
								traces.add(stringTrace[i]);

							testcase_traces.put(testcase, traces);

							checkCoverage(stringTrace, finalCriterion);
						} else {
							testSet = TestSetManager
									.replace_highCI_ordering_refine(testSet,
											testcase_traces, testcase,
											stringTrace);
						}
					}
				}
			} while (testSet.size() < testSuiteSize
					&& visited.size() < testpool.size());
		}
		int currentSize = finalCriterion.size();
		testSet.setCoverage((float) (originalSize - currentSize)
				/ (float) originalSize);
		testSet.geneTime = System.currentTimeMillis() - time;
		return testSet;
	}

	/**
	 * 2009-02-21: revised test case selection strategy: add a test case if it
	 * increases the cumulative coverage or it has a higher CI value than
	 * existing one while not decrease the coverage. This process continues
	 * until 100% coverage is achieved or a upper bound on selection is achieved
	 * 
	 * @param appClassName
	 * @param c
	 * @param testpool
	 * @param maxTrials
	 * @param min_CI
	 * @param max_CI
	 * @param testSuiteSize
	 * @return
	 */
	public static TestSet getAdequacyTestSet_refined_fixSize(
			String appClassName, Criterion c, TestSet testpool, int maxTrials,
			double min_CI, double max_CI, int testSuiteSize,
			String randomOrCriteria) {

		Criterion criterion = (Criterion) c.clone();
		Criterion finalCriterion = null;
		TestSet testSet = new TestSet();
		TestSet visited = new TestSet();
		HashMap testcase_uniqueCovers = new HashMap();
		HashMap testcase_traces = new HashMap();

		long time = System.currentTimeMillis();

		int originalSize = criterion.size();
		while (visited.size() < maxTrials && visited.size() < testpool.size()
				&& criterion.size() > 0 && testSet.size() < testSuiteSize) {
			// String testcase = testpool.getByRandom();

			String testcase = testpool.getByART();

			// String testcase = null;
			// if(testSet.size() == 0){
			// testcase = testpool.getByRandom();
			// }else{
			// testcase = testpool.getByART(testSet);
			// }

			// just for debugging purpose
			// TestCase testCase = (TestCase) Adequacy.testCases.get(testcase);

			if (!visited.contains(testcase)) {
				visited.add(testcase);
				String stringTrace[] = TestDriver.getTrace(appClassName,
						testcase);

				ArrayList uniqueCover = increaseCoverage(stringTrace, criterion);
				if (uniqueCover.size() > 0) {
					testcase_uniqueCovers.put(testcase, uniqueCover);
					testSet.add(testcase);

					ArrayList traces = new ArrayList();
					for (int i = 0; i < stringTrace.length; i++)
						traces.add(stringTrace[i]);

					testcase_traces.put(testcase, traces);

				} else {
					// 2009-03-07: restricted replacement strategy
					// testSet = TestSetManager.replace_CI_ordering(testSet,
					// testcase_uniqueCovers, testcase, stringTrace);

					// //2009-03-07: general replacement strategy
					testSet = TestSetManager.replace_CI_ordering_refine(
							testSet, testcase_traces, testcase, stringTrace);
				}
			}
		}

		finalCriterion = (Criterion) criterion.clone(); // save the criterion to
		// calculate the final
		// coverage
		if (randomOrCriteria.equals("random")) {
			// 2009-02-22:the stopping rule is very important here:
			while (testSet.size() < testSuiteSize
					&& visited.size() < testpool.size()) {
				// String testcase = testpool.getByRandom();
				String testcase = testpool.getByART();

				if (!visited.contains(testcase) && !testSet.contains(testcase)) {
					String stringTrace[] = TestDriver.getTrace(appClassName,
							testcase);
					checkCoverage(stringTrace, finalCriterion); // remove
					// redundant
					// coverage
					visited.add(testcase);
					testSet.add(testcase);
				}
			}
		} else if (randomOrCriteria.equals("criteria")) {
			finalCriterion = (Criterion) criterion.clone(); // save the
			// criterion to
			// calculate the
			// final coverage

			do {
				// reset the criterion and repeat the test case selection
				// process

				// 2009-03-06: we also need a maxTrial here and must be small
				// enough in case of unfeasible du-pairs
				int trial = 0;
				maxTrials = 100;
				criterion = (Criterion) c.clone();

				while (trial < maxTrials && visited.size() < testpool.size()
						&& criterion.size() > 0
						&& testSet.size() < testSuiteSize) {
					// String testcase = testpool.getByRandom();

					String testcase = testpool.getByART();

					// String testcase = testpool.getByART(testSet);

					trial++;
					// just for debugging purpose
					// TestCase testCase = (TestCase)
					// Adequacy.testCases.get(testcase);

					if (!visited.contains(testcase)) {
						visited.add(testcase);
						String stringTrace[] = TestDriver.getTrace(
								appClassName, testcase);

						ArrayList uniqueCover = increaseCoverage(stringTrace,
								criterion);
						if (uniqueCover.size() > 0) {
							testcase_uniqueCovers.put(testcase, uniqueCover);
							testSet.add(testcase);

							ArrayList traces = new ArrayList();
							for (int i = 0; i < stringTrace.length; i++)
								traces.add(stringTrace[i]);

							testcase_traces.put(testcase, traces);

							checkCoverage(stringTrace, finalCriterion);
						} else {
							// 2009-03-07: restricted replacement strategy
							// testSet =
							// TestSetManager.replace_CI_ordering(testSet,
							// testcase_uniqueCovers, testcase, stringTrace);

							// 2009-03-07: general replacement strategy
							testSet = TestSetManager
									.replace_CI_ordering_refine(testSet,
											testcase_traces, testcase,
											stringTrace);
						}
					}
				}
			} while (testSet.size() < testSuiteSize
					&& visited.size() < testpool.size());
		}
		int currentSize = finalCriterion.size();
		testSet.setCoverage((float) (originalSize - currentSize)
				/ (float) originalSize);
		testSet.geneTime = System.currentTimeMillis() - time;
		return testSet;
	}

	public static TestSet getAdequacyTestSet_conventional_fixSize(
			String appClassName, Criterion c, TestSet testpool, int maxTrials,
			int testSuiteSize, String randomOrCriterion) {

		Criterion criterion = (Criterion) c.clone();
		Criterion finalCriterion = null; // use to calculate the final
		// coverage of the test set
		TestSet testSet = new TestSet();
		TestSet visited = new TestSet();
		int originalSize = criterion.size();

		long time = System.currentTimeMillis();

		while (visited.size() < maxTrials && visited.size() < testpool.size()
				&& criterion.size() > 0 && testSet.size() < testSuiteSize) {
			String testcase = testpool.getByRandom();
			if (!visited.contains(testcase)) {
				visited.add(testcase);
				String stringTrace[] = TestDriver.getTrace(appClassName,
						testcase);

				if (checkCoverage(stringTrace, criterion)) {
					testSet.add(testcase);
				}
			}
		}

		finalCriterion = (Criterion) criterion.clone(); // save the criterion to
		// calculate the final
		// coverage
		if (randomOrCriterion.equals("random")) {

			// 2009-02-22:the stopping rule is very important here:
			while (testSet.size() < testSuiteSize
					&& visited.size() < testpool.size()) {
				String testcase = testpool.getByRandom();

				if (!visited.contains(testcase) && !testSet.contains(testcase)) {
					String stringTrace[] = TestDriver.getTrace(appClassName,
							testcase);
					checkCoverage(stringTrace, finalCriterion); // remove
					// redundant
					// coverage
					visited.add(testcase);
					testSet.add(testcase);
				}
			}
		} else if (randomOrCriterion.equals("criteria")) {

			do {
				// reset the criterion and repeat the test case selection
				// process

				criterion = (Criterion) c.clone();
				// 2009-03-06: we also need a maxTrial here and must be small
				// enough in case of unfeasible du-pairs
				maxTrials = 100;
				int trial = 0;

				while (visited.size() < testpool.size()
						&& testSet.size() < testSuiteSize && trial < maxTrials
						&& criterion.size() > 0) {
					trial++;
					String testcase = testpool.getByRandom();
					if (!visited.contains(testcase)) {
						visited.add(testcase);
						String stringTrace[] = TestDriver.getTrace(
								appClassName, testcase);

						if (checkCoverage(stringTrace, criterion)) {
							testSet.add(testcase);
							if (finalCriterion.size() > 0) {
								checkCoverage(stringTrace, finalCriterion);
							}
						}
					}
				}
			} while (testSet.size() < testSuiteSize
					&& visited.size() < testpool.size());
		}

		int currentSize = finalCriterion.size();
		testSet.setCoverage((float) (originalSize - currentSize)
				/ (float) originalSize);
		testSet.geneTime = System.currentTimeMillis() - time;

		return testSet;
	}

	public static TestSet getAdequacyTestSet_ART_fixSize(String appClassName,
			Criterion c, TestSet testpool, int maxTrials, int testSuiteSize,
			String randomOrCriterion) {

		Criterion criterion = (Criterion) c.clone();
		Criterion finalCriterion = null; // use to calculate the final
		// coverage of the test set
		TestSet testSet = new TestSet();
		TestSet visited = new TestSet();
		int originalSize = criterion.size();

		long time = System.currentTimeMillis();

		while (visited.size() < maxTrials && visited.size() < testpool.size()
				&& criterion.size() > 0 && testSet.size() < testSuiteSize) {
			String testcase = null;
			if (testSet.size() == 0) {
				testcase = testpool.getByRandom();
			} else {
				testcase = testpool.getByART(testSet, 10);
			}

			if (!visited.contains(testcase)) {
				visited.add(testcase);
				String stringTrace[] = TestDriver.getTrace(appClassName,
						testcase);

				if (checkCoverage(stringTrace, criterion)) {
					testSet.add(testcase);
				}
			}
		}

		finalCriterion = (Criterion) criterion.clone(); // save the criterion to
		// calculate the final
		// coverage
		if (randomOrCriterion.equals("random")) {

			// 2009-02-22:the stopping rule is very important here:
			while (testSet.size() < testSuiteSize
					&& visited.size() < testpool.size()) {
				String testcase = testpool.getByRandom();

				if (!visited.contains(testcase) && !testSet.contains(testcase)) {
					String stringTrace[] = TestDriver.getTrace(appClassName,
							testcase);
					checkCoverage(stringTrace, finalCriterion); // remove
					// redundant
					// coverage
					visited.add(testcase);
					testSet.add(testcase);
				}
			}
		} else if (randomOrCriterion.equals("criteria")) {

			do {
				// reset the criterion and repeat the test case selection
				// process

				criterion = (Criterion) c.clone();
				// 2009-03-06: we also need a maxTrial here and must be small
				// enough in case of unfeasible du-pairs
				maxTrials = 100;
				int trial = 0;

				while (visited.size() < testpool.size()
						&& testSet.size() < testSuiteSize && trial < maxTrials
						&& criterion.size() > 0) {
					trial++;
					// String testcase = testpool.getByRandom();

					String testcase = testpool.getByART(testSet, 10);

					if (!visited.contains(testcase)) {
						visited.add(testcase);
						String stringTrace[] = TestDriver.getTrace(
								appClassName, testcase);

						if (checkCoverage(stringTrace, criterion)) {
							testSet.add(testcase);
							if (finalCriterion.size() > 0) {
								checkCoverage(stringTrace, finalCriterion);
							}
						}
					}
				}
			} while (testSet.size() < testSuiteSize
					&& visited.size() < testpool.size());
		}

		int currentSize = finalCriterion.size();
		testSet.setCoverage((float) (originalSize - currentSize)
				/ (float) originalSize);
		testSet.geneTime = System.currentTimeMillis() - time;

		return testSet;
	}

	public static TestSet getAdequacyTestSet_ART(String appClassName,
			Criterion c, TestSet testpool, int maxTrials) {

		Criterion criterion = (Criterion) c.clone();
		TestSet testSet = new TestSet();
		TestSet visited = new TestSet();
		int originalSize = criterion.size();

		long time = System.currentTimeMillis();

		while (visited.size() < maxTrials && visited.size() < testpool.size()
				&& criterion.size() > 0) {
			String testcase = null;
			if (testSet.size() == 0) {
				testcase = testpool.getByRandom();
			} else {
				testcase = testpool.getByART(testSet, 10);
			}

			if (!visited.contains(testcase)) {
				visited.add(testcase);
				String stringTrace[] = TestDriver.getTrace(appClassName,
						testcase);

				if (checkCoverage(stringTrace, criterion)) {
					testSet.add(testcase);
				}
			}
		}

		int currentSize = criterion.size();
		testSet.setCoverage((float) (originalSize - currentSize)
				/ (float) originalSize);
		testSet.geneTime = System.currentTimeMillis() - time;

		return testSet;
	}

	/**
	 * conventional strategy to get adequate test set: add a test case if it
	 * increases the cumulative coverage until 100% coverage is achieved or a
	 * upper bound of selection is reached
	 * 
	 * @param appClassName
	 * @param c
	 * @param testpool
	 * @param maxTrials
	 * @return
	 */
	public static TestSet getAdequacyTestSet_conventional(String appClassName,
			Criterion c, TestSet testpool, int maxTrials) {

		Criterion criterion = (Criterion) c.clone();
		TestSet testSet = new TestSet();
		TestSet visited = new TestSet();
		int originalSize = criterion.size();

		long time = System.currentTimeMillis();

		while (visited.size() < maxTrials && visited.size() < testpool.size()
				&& criterion.size() > 0) {
			String testcase = testpool.getByRandom();
			if (!visited.contains(testcase)) {
				visited.add(testcase);
				String stringTrace[] = TestDriver.getTrace(appClassName,
						testcase);

				if (checkCoverage(stringTrace, criterion)) {
					testSet.add(testcase);
				}
			}
		}

		int currentSize = criterion.size();
		testSet.setCoverage((float) (originalSize - currentSize)
				/ (float) originalSize);
		testSet.geneTime = System.currentTimeMillis() - time;

		return testSet;
	}

	// 2009-02-16:load the test pool from the file
	public static TestSet getTestPool(String file, boolean containHeader) {
		TestSet testpool = new TestSet();
		try {
			BufferedReader br = new BufferedReader(new FileReader(file));
			String str = null;

			if (containHeader) { // if the first row is the header
				br.readLine();
			}

			while ((str = br.readLine()) != null) {
				TestCase tc = new TestCase(str);
				testpool.add(tc.index);
				testCases.put(tc.index, tc);
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return testpool;
	}

	private static boolean checkCoverage(String stringTrace[],
			Criterion criterion) {
		Node trace[] = new Node[stringTrace.length];
		int policyNodes = 0;
		for (int i = 0; i < trace.length; i++) {
			trace[i] = NodeIndex.getInstance().get(stringTrace[i]);
			if (stringTrace[i].contains(":")) {
				if (trace[i] instanceof PolicyNode) {
					policyNodes++;
				}
			}
		}
		boolean effective = false;

		for (int i = 0; i < trace.length; i++) {

			if (criterion.remove(trace[i])) { // 2009-02-21:remove the node
				effective = true;
			}

			if (trace[i] instanceof PolicyNode) {// 2009-02-21:remove the
				// policy node

				if (criterion.remove(((PolicyNode) trace[i]).policy)) {
					effective = true;
				}
			}
			if (criterion.containsDefinition(trace[i])) { // 2009-02-21:remove
				// the DU-pair

				for (int j = i + 1; j < trace.length; j++) {
					if (criterion.remove(trace[i], trace[j])) {
						effective = true;
					}
					if (trace[j] != null && trace[i].hasSameDef(trace[j])) {
						break;
					}
				}
			}
		}
		return effective;
	}

	/**
	 * 2009-10-19:increase the covered times of a specified covered element.
	 * 
	 * @param covered: DUElements(String) -> CoverTimes(Integer) 
	 * @param index
	 * @return
	 */
	private static HashMap keepDuplicateElem(HashMap covered, Object index) {
		if (covered.containsKey(index)) {
			int coverTimes = (Integer) covered.get(index);
			coverTimes++;
			covered.put(index, coverTimes);
		} else {
			int coverTimes = 1;
			covered.put(index, coverTimes);
		}
		return covered;
	}

	/**
	 * 2009-10-19:change execution traces of a test case into the meaningful
	 * du-associations
	 * 
	 * @param stringTrace
	 * @param criterion
	 * @return: HashMap[DU(String), CoverTimes(Integer)]
	 */
	public static HashMap countDUCoverage(String[] stringTrace,
			Criterion criterion) {
		HashMap covered = new HashMap();
		Node trace[] = new Node[stringTrace.length];
		for (int i = 0; i < trace.length; i++) {
			trace[i] = NodeIndex.getInstance().get(stringTrace[i]);
		}
		boolean effective = false;

		for (int i = 0; i < trace.length; i++) {
			// 2009-10-19: we do not care about nodes and policy nodes
			if (criterion.containsNode(trace[i])) {
				keepDuplicateElem(covered, trace[i].index);
			}
			if (trace[i] instanceof PolicyNode
					&& criterion.containsPolicy(((PolicyNode) trace[i]).policy)) {
				keepDuplicateElem(covered, ((PolicyNode) trace[i]).index);
			}

			if (criterion.containsDefinition(trace[i])) {
				for (int j = i + 1; j < trace.length; j++) {
					if (criterion.containsAssociation(trace[i], trace[j])) {
						keepDuplicateElem(covered, trace[i].index + ":"
								+ trace[j].index);
					}
					if (trace[j] != null && trace[i].hasSameDef(trace[j])) {
						break;
					}
				}
			}
		}
		return covered;
	}

	/**
	 * 2009-03-31: count the number of elements covered by a specified test case
	 * 
	 * @param stringTrace
	 * @param criterion
	 * @return
	 */
	private static int countCoveredElements(String[] stringTrace,
			Criterion criterion) {
		int counter = 0;
		int policyNodes = 0;
		Node trace[] = new Node[stringTrace.length];
		for (int i = 0; i < trace.length; i++) {
			trace[i] = NodeIndex.getInstance().get(stringTrace[i]);
		}
		boolean effective = false;

		for (int i = 0; i < trace.length; i++) {

			if (criterion.containsNode(trace[i]))
				counter++;

			if (trace[i] instanceof PolicyNode
					&& criterion.containsPolicy(((PolicyNode) trace[i]).policy))
				counter++;

			if (criterion.containsDefinition(trace[i])) {
				for (int j = i + 1; j < trace.length; j++) {
					if (criterion.containsAssociation(trace[i], trace[j])) {
						counter++;
					}

					if (trace[j] != null && trace[i].hasSameDef(trace[j]))
						break;
				}
			}
		}

		return counter;
	}

	/**
	 * 2009-02-21:return the results of all nodes, policy nodes, and DU-pairs
	 * unique covered by this test case
	 * 
	 * @param stringTrace
	 * @param criterion
	 * @return
	 */
	private static ArrayList increaseCoverage(String[] stringTrace,
			Criterion criterion) {
		ArrayList effectNodes = new ArrayList();

		int policyNodes = 0;
		Node trace[] = new Node[stringTrace.length];
		for (int i = 0; i < trace.length; i++) {
			trace[i] = NodeIndex.getInstance().get(stringTrace[i]);
			if (stringTrace[i].contains("c122:P0")) {
				if (trace[i] instanceof PolicyNode) {
					policyNodes++;
				}

			}
		}
		boolean effective = false;

		for (int i = 0; i < trace.length; i++) {
			if (criterion.remove(trace[i])) { // 2009-02-21:remove the node
				if (!effectNodes.contains(trace[i])) {
					effectNodes.add(stringTrace[i]);
				}

				effective = true;
			}
			if (trace[i] instanceof PolicyNode) {
				if (criterion.remove(((PolicyNode) trace[i]).policy)) {// 2009-02-21:remove
					// the
					// policy
					// node
					if (!effectNodes.contains(trace[i])) {
						effectNodes.add(stringTrace[i]);
					}

					effective = true;
				}
			}
			if (criterion.containsDefinition(trace[i])) { // 2009-02-21:remove
				// the DU-pair
				for (int j = i + 1; j < trace.length; j++) {
					if (criterion.remove(trace[i], trace[j])) {
						// if (!effectNodes.contains(trace[i])) {
						// effectNodes.add(stringTrace[i]);
						// }
						// if (!effectNodes.contains(trace[j])) {
						// effectNodes.add(stringTrace[j]);
						// }

						// 2009-03-03: check the correlations between CI and
						// coverage
						if (!effectNodes.contains(trace[i] + ":" + trace[j])) {
							effectNodes.add(trace[i] + ":" + trace[j]);
						}
						effective = true;
					}
					if (trace[j] != null && trace[i].hasSameDef(trace[j])) {
						break;
					}
				}
			}
		}

		return effectNodes;
	}

	public static void saveTestSets(TestSet[] testSets, String saveFile) {
		try {
			// the Size of test set: max, min, average, std
			int maxSize = 0;
			int minSize = Integer.MAX_VALUE;
			int totalSize = 0;
			int averageSize = 0;
			double stdSize = 0.0;

			// the Coverage of test set:
			double maxCoverage = 0.0;
			double minCoverage = Double.MAX_VALUE;
			double totalCoverage = 0.0;
			double averageCoverage = 0.0;
			double stdCoverage = 0.0;

			// the time of test set:
			long maxTime = 0;
			long minTime = Long.MAX_VALUE;
			long totalTime = 0;
			long averageTime = 0;
			double stdTime = 0;

			// 2009-10-29:check whether the folder and files exist
			File file = new File(saveFile);
			if (!file.exists()) {
				File parentFile = file.getParentFile();
				if (!parentFile.exists()) {
					parentFile.mkdirs();
				}
				file.createNewFile();
			}
			BufferedWriter bw = new BufferedWriter(new FileWriter(saveFile));
			for (int i = 0; i < testSets.length; i++) {
				bw.write(i + "\t" + testSets[i].toString());
				bw.flush();
				bw.newLine();

				// size
				int currentSize = testSets[i].size();
				if (maxSize < currentSize)
					maxSize = currentSize;
				if (minSize > currentSize)
					minSize = currentSize;
				totalSize += currentSize;

				// coverage
				double currentCoverage = testSets[i].getCoverage();
				if (maxCoverage < currentCoverage)
					maxCoverage = currentCoverage;
				if (minCoverage > currentCoverage)
					minCoverage = currentCoverage;
				totalCoverage += currentCoverage;

				// time
				long currentTime = testSets[i].geneTime;
				if (maxTime < currentTime)
					maxTime = currentTime;
				if (minTime > currentTime)
					minTime = currentTime;
				totalTime += currentTime;
			}

			// Size, Coverage, time
			averageSize = totalSize / testSets.length;
			averageCoverage = totalCoverage / testSets.length;
			averageTime = totalTime / testSets.length;

			double tempSize = 0.0;
			double tempCoverage = 0.0;
			double tempTime = 0.0;
			for (int i = 0; i < testSets.length; i++) {
				tempSize += (testSets[i].size() - averageSize)
						* (testSets[i].size() - averageSize);
				tempCoverage += (testSets[i].getCoverage() - averageCoverage)
						* (testSets[i].getCoverage() - averageCoverage);
				tempTime += (testSets[i].geneTime - averageTime)
						* (testSets[i].geneTime - averageTime);
			}
			stdSize = Math.sqrt(tempSize / testSets.length);
			stdCoverage = Math.sqrt(tempCoverage / testSets.length);
			stdTime = Math.sqrt(tempTime / testSets.length);

			String line = "maxSize:" + maxSize + "\t" + "minSize:" + minSize
					+ "\t" + "averageSize:" + averageSize + "\t" + "stdSize:"
					+ stdSize + "\n" + "maxCoverage:" + maxCoverage + "\t"
					+ "minCoverage:" + minCoverage + "\t" + "averageCoverage:"
					+ averageCoverage + "\t" + "stdCoverage:" + stdCoverage
					+ "\n" + "maxTime:" + maxTime + "\t" + "minTime:" + minTime
					+ "\t" + "averageTime:" + averageTime + "\t" + "stdTime:"
					+ stdTime;

			bw.write(line);
			bw.flush();
			bw.close();
			System.out.println("Test sets: " + saveFile + " generated");
		} catch (IOException e) {
			System.out.println(e);
		}
	}

	
	
	/**
	 * 2009-10-29: add a controller to get the upper improvement brought by
	 * RA-H,RA-L,RA-R
	 * 
	 * @param appClassName
	 * @param c
	 * @param testpool
	 * @param testSetNum
	 * @param saveFile
	 * @param H_L_R
	 * @param size_ART
	 * @return
	 */
	public static TestSet[] getTestSets_upperImprovement(String appClassName,
			Criterion c, TestSet testpool, int testSetNum, String saveFile,
			String H_L_R, int size_ART) {
		TestSet[] testSets = new TestSet[testSetNum];
		for (int i = 0; i < testSetNum; i++) {
			TestSet testpool_copy = testpool.copy();
			testSets[i] = TestSetManager.getAdequacyTestSet_best(appClassName,
					c, testpool_copy, H_L_R, size_ART);

			testSets[i].index = "" + i;

			System.out.println("Test set " + i + ": " + testSets[i].toString());

		}
		TestSetManager.saveTestSets(testSets, saveFile);
		return testSets;
	}

	/**2009-10-31: use only context diversity information to construct test sets
	 * which to demonstrate how CD can improve random testing
	 * @param appClassName
	 * @param testpool
	 * @param testSetNum
	 * @param testSuiteSize
	 * @param saveFile
	 * @param H_L_R
	 * @param size_ART
	 * @return
	 */
	public static TestSet[] getTestSets_CD(TestSet testpool, 
			int testSetNum, int testSuiteSize, 
			String saveFile, String H_L_R, int size_ART) {
		TestSet[] testSets = new TestSet[testSetNum];
		for(int i = 0; i < testSetNum; i ++){
			testSets[i] = TestSetManager.getRandomTestSets_CD(testpool, 
					H_L_R, testSuiteSize, size_ART);
			testSets[i].index = "" + i;
			System.out.println("Test set " + i + ": " + testSets[i].toString());			
		}
		
		TestSetManager.saveTestSets(testSets, saveFile);
		return testSets;
	}
	
	/**2009-10-30: get test sets when ties occur, we will 
	 * use DUCov as the first factor and CI as the second 
	 * one to solve it
	 * @param appClassName
	 * @param c
	 * @param testpool
	 * @param maxTrials
	 * @param testSetNum
	 * @param saveFile
	 * @param H_L_R
	 * @param size_ART
	 * @return
	 */
	public static TestSet[] getTestSets_DUCovCI(String appClassName,
			Criterion c, TestSet testpool, String oldOrNew, int maxTrials, int testSetNum, 
			String saveFile, String H_L_R, int size_ART) {
		TestSet[] testSets = new TestSet[testSetNum];
		if(oldOrNew.equals("new")){
			for (int i = 0; i < testSetNum; i++) {

				testSets[i] = TestSetManager.getAdequacyTestSet_refined_DUCoverage(appClassName, 
						c, testpool, maxTrials, H_L_R, size_ART);

				testSets[i].index = "" + i;

				System.out.println("Test set " + i + ": " + testSets[i].toString());

			}
		}else if(oldOrNew.equals("old")){
			for (int i = 0; i < testSetNum; i++) {
				testSets[i] = TestSetManager
						.getAdequacyTestSet_conventional(appClassName, c,
								testpool, maxTrials);

				// 2009-02-24: set the index of testSets
				testSets[i].index = "" + i;

				System.out.println("Test set " + i + ": "
						+ testSets[i].toString());
			}
		}
		
		TestSetManager.saveTestSets(testSets, saveFile);
		return testSets;
	}
	/**
	 * 2009-10-29: two redundant parameters are reduced to use RA-H/RA-L/RA-R to
	 * sample test cases
	 * 
	 * @param appClassName
	 * @param c
	 * @param testpool
	 * @param maxTrials
	 * @param testSetNum
	 * @param newOrOld
	 * @param randomOrCriteria
	 * @param testSuiteSize
	 * @param saveFile
	 * @param H_L_R
	 * @param size_ART
	 * @return
	 */
	public static TestSet[] getTestSets(String appClassName, Criterion c,
			TestSet testpool, int maxTrials, int testSetNum, String newOrOld,
			String randomOrCriteria, int testSuiteSize, String saveFile,
			String H_L_R, int size_ART) {
		TestSet[] testSets = new TestSet[testSetNum];
		if (testSuiteSize < 0) { // do not fix the test set size
			if (newOrOld.equals("new")) {
				for (int i = 0; i < testSetNum; i++) {
					// 2009-03-10: we use ART+generalReplacement to favor test
					// cases with higher CI
					testSets[i] = TestSetManager.getAdequacyTestSet_refined(
							appClassName, c, testpool, maxTrials, H_L_R,
							size_ART);

					// 2009-10-15: we use activation rather than CI as the
					// principle factor
					// testSets[i] =
					// TestSetManager.getAdequacyTestSet_refined_activation(
					// appClassName, c, testpool, maxTrials, H_L_R, size_ART);

					// 2009-02-24: set the index of testSets
					testSets[i].index = "" + i;

					System.out.println("Test set " + i + ": "
							+ testSets[i].toString());
				}
			} else if (newOrOld.equals("old")) {
				for (int i = 0; i < testSetNum; i++) {
					testSets[i] = TestSetManager
							.getAdequacyTestSet_conventional(appClassName, c,
									testpool, maxTrials);

					// 2009-02-24: set the index of testSets
					testSets[i].index = "" + i;

					System.out.println("Test set " + i + ": "
							+ testSets[i].toString());
				}
			}
		}

		TestSetManager.saveTestSets(testSets, saveFile);
		return testSets;
	}

	/**
	 * 2009-09-18: use RA_H, RA_L, RA_R to construct adequate test sets
	 * 
	 * @param appClassName
	 * @param c
	 * @param testpool
	 * @param maxTrials
	 * @param testSetNum
	 * @param min_CI
	 * @param max_CI
	 * @param newOrOld
	 * @param randomOrCriteria
	 * @param testSuiteSize
	 * @param saveFile
	 * @param H_L_R:"H"
	 *            if favoring test cases with high context diversities; "L" if
	 *            favoring test cases with low context diversities; "R" if
	 *            favoring test cases with evenly-distributed context
	 *            diversities
	 * @param size_ART:
	 *            the size of ART-constructed test sets
	 * @return
	 */
	public static TestSet[] getTestSets(String appClassName, Criterion c,
			TestSet testpool, int maxTrials, int testSetNum, double min_CI,
			double max_CI, String newOrOld, String randomOrCriteria,
			int testSuiteSize, String saveFile, String H_L_R, int size_ART) {
		TestSet[] testSets = new TestSet[testSetNum];
		if (testSuiteSize < 0) { // do not fix the test set size
			if (newOrOld.equals("new")) {
				for (int i = 0; i < testSetNum; i++) {
					// 2009-03-10: we use ART+generalReplacement to favor test
					// cases with higher CI
					testSets[i] = TestSetManager.getAdequacyTestSet_refined(
							appClassName, c, testpool, maxTrials, H_L_R,
							size_ART);

					// 2009-10-15: we use activation rather than CI as the
					// principle factor
					 testSets[i] =
					 TestSetManager.getAdequacyTestSet_refined_activation(
					 appClassName, c, testpool, maxTrials, H_L_R, size_ART);
					//					
					// 2009-10-22: get the upper bound of CD improvement
					// testSets[i] =
					// TestSetManager.getAdequacyTestSet_refined_best(appClassName,
					// c, testpool, H_L_R);

					// 2009-10-23: get the upper bound of CD improvement by
					// Random Algorithm
					// TestSet testpool_copy = testpool.copy();
					// testSets[i] =
					// TestSetManager.getAdequacyTestSet_best(appClassName, c,
					// testpool_copy, H_L_R, size_ART);

					// 2009-02-24: set the index of testSets
					testSets[i].index = "" + i;

					System.out.println("Test set " + i + ": "
							+ testSets[i].toString());
				}
			} else if (newOrOld.equals("old")) {
				for (int i = 0; i < testSetNum; i++) {
					testSets[i] = TestSetManager
							.getAdequacyTestSet_conventional(appClassName, c,
									testpool, maxTrials);

					// 2009-02-24: set the index of testSets
					testSets[i].index = "" + i;

					System.out.println("Test set " + i + ": "
							+ testSets[i].toString());
				}
			}
		} else if (testSuiteSize > 0) {// fix the test set size
			if (newOrOld.equals("new")) {
				for (int i = 0; i < testSetNum; i++) {

					testSets[i] = TestSetManager
							.getAdequacyTestSet_refined_fixSize(appClassName,
									c, testpool, maxTrials, min_CI, max_CI,
									testSuiteSize, randomOrCriteria);

					// 2009-02-24: set the index of testSets
					testSets[i].index = "" + i;

					System.out.println("Test set " + i + ": "
							+ testSets[i].toString());
				}
			} else if (newOrOld.equals("old")) {
				for (int i = 0; i < testSetNum; i++) {
					testSets[i] = TestSetManager
							.getAdequacyTestSet_conventional_fixSize(
									appClassName, c, testpool, maxTrials,
									testSuiteSize, randomOrCriteria);

					// 2009-02-24: set the index of testSets
					testSets[i].index = "" + i;

					System.out.println("Test set " + i + ": "
							+ testSets[i].toString());
				}
			}
		} else if (testSuiteSize == 0) {
			// 2009-03-09:min_CI, max_CI plays some roles
			for (int i = 0; i < testSetNum; i++) {
				// 2009-03-31: study the correlationships between CI of test
				// cases and testing performance
				testSets[i] = TestSetManager.getTestSets_FixedCI(testpool,
						min_CI, max_CI);
				testSets[i].index = "" + i;
				System.out.println("Test set " + i + ": ");

			}
		}

		TestSetManager.saveTestSets(testSets, saveFile);
		return testSets;
	}

	public static TestSet[] getTestSets(String appClassName, Criterion c,
			TestSet testpool, int maxTrials, int testSetNum, double min_CI,
			double max_CI, String newOrOld, String randomOrCriteria,
			int testSuiteSize, String saveFile) {
		TestSet[] testSets = new TestSet[testSetNum];
		if (testSuiteSize < 0) { // do not fix the test set size
			if (newOrOld.equals("new")) {
				for (int i = 0; i < testSetNum; i++) {

					// 2009-03-14: we favor test cases with low CI
					// testSets[i] =
					// TestSetManager.getAdequacyTestSet_refined_favorLowCI(
					// appClassName, c, testpool, maxTrials);

					// 2009-03-10: we use ART+generalReplacement to favor test
					// cases with higher CI
					testSets[i] = TestSetManager.getAdequacyTestSet_refined(
							appClassName, c, testpool, maxTrials);

					// 2009-03-10: we use ART to generate adequate test sets
					// testSets[i] =
					// TestSetManager.getAdequacyTestSet_ART(appClassName, c,
					// testpool, maxTrials);

					// 2009-02-24: set the index of testSets
					testSets[i].index = "" + i;

					System.out.println("Test set " + i + ": "
							+ testSets[i].toString());
				}
			} else if (newOrOld.equals("old")) {
				for (int i = 0; i < testSetNum; i++) {
					testSets[i] = TestSetManager
							.getAdequacyTestSet_conventional(appClassName, c,
									testpool, maxTrials);

					// 2009-02-24: set the index of testSets
					testSets[i].index = "" + i;

					System.out.println("Test set " + i + ": "
							+ testSets[i].toString());
				}
			}
		} else if (testSuiteSize > 0) {// fix the test set size
			if (newOrOld.equals("new")) {
				for (int i = 0; i < testSetNum; i++) {

					// 2009-03-14: we favor test cases with lower CI
					// testSets[i] =
					// TestSetManager.getAdequacyTestSet_refined_fixSize_favorLowCI(appClassName,
					// c, testpool, maxTrials, testSuiteSize, randomOrCriteria);

					// 2009-03-16: ART+generalReplacement to favor test cases
					// with higher CI
					testSets[i] = TestSetManager
							.getAdequacyTestSet_refined_fixSize(appClassName,
									c, testpool, maxTrials, min_CI, max_CI,
									testSuiteSize, randomOrCriteria);

					// 2009-03-10: we use ART to generate adequate test sets
					// testSets[i] =
					// TestSetManager.getAdequacyTestSet_ART_fixSize(appClassName,
					// c, testpool, maxTrials, testSuiteSize, randomOrCriteria);

					// 2009-02-24: set the index of testSets
					testSets[i].index = "" + i;

					System.out.println("Test set " + i + ": "
							+ testSets[i].toString());
				}
			} else if (newOrOld.equals("old")) {
				for (int i = 0; i < testSetNum; i++) {
					testSets[i] = TestSetManager
							.getAdequacyTestSet_conventional_fixSize(
									appClassName, c, testpool, maxTrials,
									testSuiteSize, randomOrCriteria);

					// 2009-02-24: set the index of testSets
					testSets[i].index = "" + i;

					System.out.println("Test set " + i + ": "
							+ testSets[i].toString());
				}
			}
		} else if (testSuiteSize == 0) {
			// 2009-03-09:min_CI, max_CI plays some roles
			for (int i = 0; i < testSetNum; i++) {

				// 2009-03-31: study the relationships between CI of adequacy
				// test sets and testing performance
				// testSets[i] =
				// TestSetManager.getAdequacyTestSet_refined_fixCI(
				// appClassName, c, testpool, maxTrials,
				// min_CI, max_CI
				// );

				// 2009-03-31: study the correlationships between CI of test
				// cases and testing performance
				testSets[i] = TestSetManager.getTestSets_FixedCI(testpool,
						min_CI, max_CI);
				testSets[i].index = "" + i;
				System.out.println("Test set " + i + ": ");

			}
		}
		TestSetManager.saveTestSets(testSets, saveFile);
		return testSets;
	}

	/**
	 * args[0]: the number of adequate test sets(100);
	 * args[1]:instruction(Context_Intensity); args[2]: min CI of test cases in
	 * test sets(0.7); args[3]: maximum CI of test cases in test sets(0.9)
	 * args[4]: the directory to save output; args[5]: testing criteria args[6]:
	 * fix the size of test sets; args[7]: old or new test case selection
	 * strategy args[8]: random-enhanced or repetition-enhanced
	 * 
	 * @param args
	 */
	public static void main(String[] args) {

		System.out
				.println("USAGE: java ccr.test.TestSetManager <testSetNum(100)> <Context_Intensity> <min_CI(0.7)> "
						+ "<max_CI(0.9)> <directory(20090222)> <testing criteria(AllPolicies, All1ResolvedDU, All2ResolvedDU)>"
						+ "<TestSuiteSize(58)> <oldOrNew(old, new)> <randomOrCriteria(random, criteria)>");

		String appClassName = "TestCFG2_ins";

		int maxTrials = 2000;
		String instruction = args[0];

		if (instruction.equals("Context_Intensity")) {
			// 2009-02-22
			int testSetNum = Integer.parseInt(args[1]);
			double min_CI = Double.parseDouble(args[2]);
			double max_CI = Double.parseDouble(args[3]);

			String date = args[4];
			String criterion = args[5];
			int testSuiteSize = Integer.parseInt(args[6]);
			String oldOrNew = args[7];
			String randomOrCriterion = args[8];

			CFG g = new CFG(System.getProperty("user.dir")
					+ "/src/ccr/app/TestCFG2.java");
			Criterion c = null;

			String testPoolFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
					+ date + "/TestPool.txt";
			TestSet testpool = getTestPool(testPoolFile, true);
			Adequacy.loadTestCase(testPoolFile);

			// 2009-02-22: fix the size of test suite to be 58, using
			// random-repetition to compensate the small test sets

			TestSet[][] testSets = new TestSet[1][];
			String versionPackageName = "testversion";
			String saveFile;

			if (criterion.equals("AllPolicies"))
				c = g.getAllPolicies();
			else if (criterion.equals("All1ResolvedDU"))
				c = g.getAllKResolvedDU(1);
			else if (criterion.equals("All2ResolvedDU"))
				c = g.getAllKResolvedDU(2);

			if (testSuiteSize < 0) {
				saveFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
						+ date
						+ "/"
						+ criterion
						+ "TestSets_"
						+ oldOrNew
						+ ".txt";
			} else {
				saveFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
						+ date
						+ "/"
						+ criterion
						+ "TestSets_"
						+ oldOrNew
						+ "_"
						+ randomOrCriterion + "_" + testSuiteSize + ".txt";
			}

			testSets[0] = TestSetManager.getTestSets(appClassName, c, testpool,
					maxTrials, testSetNum, min_CI, max_CI, oldOrNew,
					randomOrCriterion, testSuiteSize, saveFile);

			saveFile = saveFile.substring(0, saveFile.indexOf(".txt"))
					+ "_CI.txt";
			TestSetManager.attachTSWithCI(testSets[0], saveFile);
		}

		else if (instruction.equals("getRandomTestSet_refined")) {
			// 2009-10-16: use context diversity information to select test cas
			int testSetNum = Integer.parseInt(args[0]);
			int testSuiteSize = Integer.parseInt(args[1]);
			String date = args[2];
			String H_L_R = args[3];
			int size_ART = Integer.parseInt(args[4]);

			String saveFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
					+ date
					+ "/RandomTestSets_RA-"
					+ H_L_R
					+ "_"
					+ testSuiteSize + "_" + size_ART + ".txt";

			String testPoolFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
					+ date + "/TestPool.txt";
			TestSet testpool = getTestPool(testPoolFile, true);
			Adequacy.loadTestCase(testPoolFile);

			TestSet[][] testSets = new TestSet[1][];
			testSets[0] = TestSetManager.getRandomTestSets_refined(
					appClassName, testpool, testSetNum, testSuiteSize, H_L_R,
					size_ART, saveFile);

			saveFile = saveFile.substring(0, saveFile.indexOf(".txt"))
					+ "_CI.txt";
			// 2009-10-20: we interest in both CI and activation replacement
			TestSetManager.attachTSWithCI_Activation_replacement(testSets[0],
					saveFile);

		} else if (instruction.equals("RandomTestSet_Conventional")) {
			// 2009-02-22: get random test sets
			int testSetNum = Integer.parseInt(args[0]);
			int testSuiteSize = Integer.parseInt(args[1]);
			String date = args[2];

			String saveFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
					+ date + "/RandomTestSets_" + testSuiteSize + ".txt";

			String testPoolFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
					+ date + "/TestPool.txt";
			TestSet testpool = getTestPool(testPoolFile, true);
			Adequacy.loadTestCase(testPoolFile);

			TestSet[][] testSets = new TestSet[1][];
			testSets[0] = TestSetManager.getRandomTestSets(appClassName,
					testpool, testSetNum, testSuiteSize, saveFile);

			saveFile = saveFile.substring(0, saveFile.indexOf(".txt"))
					+ "_CI.txt";
			// 2009-10-20:
			TestSetManager.attachTSWithCI_Activation_replacement(testSets[0],
					saveFile);

		} else if (instruction.equals("CI_Coverage")) {
			// 2009-03-03: add this to study correlations between CI and covered
			// elements
			String date = args[0];
			int iterations = Integer.parseInt(args[1]);

			CFG g = new CFG(System.getProperty("user.dir")
					+ "/src/ccr/app/TestCFG2.java");

			String testPoolFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
					+ date + "/TestPool.txt";
			TestSet testpool = getTestPool(testPoolFile, true);
			Adequacy.loadTestCase(testPoolFile);

			Vector testCases = new Vector(); // get random test sets

			Iterator ite = Adequacy.testCases.keySet().iterator();
			while (ite.hasNext()) {
				testCases.add((String) ite.next());
			}

			Criterion c = g.getAllPolicies();
			System.out.println(c.size());
			String saveFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
					+ date + "/" + "AllPolicy_CICoverage.txt";
			TestSetManager.checkCorrelation(appClassName, testpool, testCases,
					c, iterations, saveFile);

			c = g.getAllKResolvedDU(1);
			saveFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
					+ date + "/" + "All1Service_CICoverage.txt";
			System.out.println(c.size());
			TestSetManager.checkCorrelation(appClassName, testpool, testCases,
					c, iterations, saveFile);

			c = g.getAllKResolvedDU(2);
			saveFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
					+ date + "/" + "All2Service_CICoverage.txt";
			System.out.println(c.size());
			TestSetManager.checkCorrelation(appClassName, testpool, testCases,
					c, iterations, saveFile);
		} else if (instruction.equals("getTestSet_FixedCI")) {
			// 2009-03-31: get adequate test sets whose CI are fixed
			double start_CI = Double.parseDouble(args[0]);
			double end_CI = Double.parseDouble(args[1]);
			double interval = Double.parseDouble(args[2]);
			int testSetNum = Integer.parseInt(args[3]);
			String date = args[4];

			CFG g = new CFG(System.getProperty("user.dir")
					+ "/src/ccr/app/TestCFG2.java");
			Criterion c = null;

			String testPoolFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
					+ date + "/TestPool.txt";
			TestSet testpool = getTestPool(testPoolFile, true);
			Adequacy.loadTestCase(testPoolFile);

			// only interest in Group 1
			for (double i = start_CI; i < end_CI; i = i + interval) {
				double min_CI = i;
				double max_CI = i + interval;

				if (max_CI > end_CI)
					max_CI = end_CI;

				if (min_CI == max_CI)
					break;

				System.out.println("Min:" + min_CI + " Max:" + max_CI);
				// //1. for AllPolicies
				// c = g.getAllPolicies();
				// String saveFile =
				// "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
				// + date + "/AllPolicies_" +min_CI + "_"+max_CI +".txt";
				//				
				// TestSet[][] testSets = new TestSet[1][];
				// testSets[0] = TestSetManager.getTestSets(appClassName, c,
				// testpool, maxTrials, testSetNum, min_CI, max_CI,
				// "null", "null", 0, saveFile);
				// saveFile = saveFile.substring(0, saveFile.indexOf(".txt"))
				// + "_CI.txt";
				// TestSetManager.attachTSWithCI(testSets[0], saveFile);
				//				
				// //2. for All1ResolvedDU
				// c = g.getAllKResolvedDU(1);
				// saveFile =
				// "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
				// + date + "/All1ResolvedDU_" +min_CI + "_"+max_CI +".txt";
				//				
				// testSets[0] = TestSetManager.getTestSets(appClassName, c,
				// testpool, maxTrials, testSetNum, min_CI, max_CI,
				// "null", "null", 0, saveFile);
				// saveFile = saveFile.substring(0, saveFile.indexOf(".txt"))
				// + "_CI.txt";
				// TestSetManager.attachTSWithCI(testSets[0], saveFile);
				//				
				// //3. for All2ResolvedDU
				// c = g.getAllKResolvedDU(2);
				// saveFile =
				// "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
				// + date + "/All2ResolvedDU_" +min_CI + "_"+max_CI +".txt";
				//				
				// testSets[0] = TestSetManager.getTestSets(appClassName, c,
				// testpool, maxTrials, testSetNum, min_CI, max_CI,
				// "null", "null", 0, saveFile);
				// saveFile = saveFile.substring(0, saveFile.indexOf(".txt"))
				// + "_CI.txt";
				// TestSetManager.attachTSWithCI(testSets[0], saveFile);

				// Get the covered element information for test cases whose CI
				// within a specified range
				c = g.getAllKResolvedDU(2);
				String saveFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
						+ date + "/TestCases_" + min_CI + "_" + max_CI + ".txt";

				TestSet[][] testSets = new TestSet[1][];
				testSets[0] = TestSetManager.getTestSets(appClassName, c,
						testpool, maxTrials, testSetNum, min_CI, max_CI,
						"null", "null", 0, saveFile);
				saveFile = saveFile.substring(0, saveFile.indexOf(".txt"))
						+ "_coveredElements.txt";
				TestSetManager.attachTSWithCoveredElements(testSets[0],
						appClassName, c, saveFile);
			}

		} else if (instruction.equals("getTestSet_Refined")) {
			// 2009-09-18: generate adequate test sets with RA_H, RA_L, or RA_R
			String date = args[1];
			int testSetNum = Integer.parseInt(args[2]);
			String criterion = args[3];
			int testSuiteSize = Integer.parseInt(args[4]);
			String oldOrNew = args[5];
			String randomOrCriterion = args[6];
			String H_L_R = args[7];
			int size_ART = Integer.parseInt(args[8]);

			CFG g = new CFG(System.getProperty("user.dir")
					+ "/src/ccr/app/TestCFG2.java");
			Criterion c = null;

			String testPoolFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
					+ date + "/TestPool.txt";
			TestSet testpool = getTestPool(testPoolFile, true);

			Adequacy.loadTestCase(testPoolFile);

			// 2009-02-22: fix the size of test suite to be 58, using
			// random-repetition to compensate the small test sets

			TestSet[][] testSets = new TestSet[1][];
			String versionPackageName = "testversion";
			String saveFile = null;

			if (criterion.equals("AllPolicies"))
				c = g.getAllPolicies();
			else if (criterion.equals("All1ResolvedDU"))
				c = g.getAllKResolvedDU(1);
			else if (criterion.equals("All2ResolvedDU"))
				c = g.getAllKResolvedDU(2);

			if (testSuiteSize < 0) {
				if (oldOrNew.equals("old")) {// Conventional test suite
												// construction algorithm
					saveFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
							+ date + "/" + size_ART + "/" // 2009-10-29:we
															// change the saving
															// folder here
							+ criterion + "_CA_" + size_ART + ".txt";

				} else if (oldOrNew.equals("new")) {
					if (H_L_R.equals("H")) { // Refined test suite
												// construction algorithm with
												// high context diversity
						saveFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
								+ date + "/" + size_ART + "/"// 2009-10-29:we
																// change the
																// saving folder
																// here
								+ criterion + "_RA-H_" + size_ART + ".txt";
					} else if (H_L_R.equals("L")) {// Refined test suite
													// construction algorithm
													// with low context
													// diversity
						saveFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
								+ date + "/" + size_ART + "/"// 2009-10-29:we
																// change the
																// saving folder
																// here
								+ criterion + "_RA-L_" + size_ART + ".txt";
					} else if (H_L_R.equals("R")) {// Refined test suite
													// construction algorithm
													// with random context
													// diversity
						saveFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
								+ date + "/" + size_ART + "/"// 2009-10-29:we
																// change the
																// saving folder
																// here
								+ criterion + "_RA-R_" + size_ART + ".txt";
					}
				}
			} else {
				if (oldOrNew.equals("old")) {
					saveFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
							+ date
							+ "/"
							+ size_ART
							+ "/"// 2009-10-29:we change the saving folder
									// here
							+ criterion
							+ "_CA_"
							+ randomOrCriterion
							+ "_"
							+ testSuiteSize + "_" + size_ART + ".txt"; // 							
				} else if (oldOrNew.equals("new")) {
					if (H_L_R.equals("H")) {
						saveFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
								+ date
								+ "/"
								+ size_ART
								+ "/"// 2009-10-29:we change the saving
										// folder here
								+ criterion
								+ "_RA-H_"
								+ randomOrCriterion
								+ "_" + testSuiteSize + "_" + size_ART + ".txt";
					} else if (H_L_R.equals("L")) {
						saveFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
								+ date
								+ "/"
								+ size_ART
								+ "/"// 2009-10-29:we change the saving
										// folder here
								+ criterion
								+ "_RA-L_"
								+ randomOrCriterion
								+ "_" + testSuiteSize + "_" + size_ART + ".txt";
					} else if (H_L_R.equals("R")) {
						saveFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
								+ date
								+ "/"
								+ size_ART
								+ "/"// 2009-10-29:we change the saving
										// folder here
								+ criterion
								+ "_RA-R_"
								+ randomOrCriterion
								+ "_" + testSuiteSize + "_" + size_ART + ".txt";
					}
				}
			}

			testSets[0] = TestSetManager.getTestSets(appClassName, c, testpool,
					maxTrials, testSetNum, oldOrNew, randomOrCriterion,
					testSuiteSize, saveFile, H_L_R, size_ART);

			saveFile = saveFile.substring(0, saveFile.indexOf(".txt"))
					+ "_CI.txt";
			// 2009-10-15: attach test sets with CI and activation information
			TestSetManager.attachTSWithCI_Activation_replacement(testSets[0],
					saveFile);
		} else if (instruction.equals("getUpperImprovement_CI")) {
			// 2009-10-22: get upper bound of improvement brought by CD
			String date = args[1];
			int testSetNum = Integer.parseInt(args[2]);
			int size_ART = Integer.parseInt(args[3]);
			String criterion = args[4];
			String H_L_R = args[5];

			// String[] criteria = new String[] { "AllPolicies", "All1ResolvedDU",
			// "All2ResolvedDU" };
			// String[] H_L_R = new String[] { "R", "H", "L" };
			//
			// CFG g = new CFG(System.getProperty("user.dir")
			// + "/src/ccr/app/TestCFG2.java");
			// Criterion c = null;
			// String testPoolFile =
			// "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
			// + date + "/TestPool.txt";
			// TestSet testpool = getTestPool(testPoolFile, true);
			//
			// Adequacy.loadTestCase(testPoolFile);
			//
			// TestSet[][] testSets = new TestSet[1][];
			// String versionPackageName = "testversion";
			// String saveFile = null;
			//
			// for (int i = 0; i < criteria.length; i++) {
			//
			// for (int j = 0; j < H_L_R.length; j++) {
			// if (criteria[i].equals("AllPolicies"))
			// c = g.getAllPolicies();
			// else if (criteria[i].equals("All1ResolvedDU"))
			// c = g.getAllKResolvedDU(1);
			// else if (criteria[i].equals("All2ResolvedDU"))
			// c = g.getAllKResolvedDU(2);
			// saveFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
			// + date
			// + "/UpperImprove/"
			// + criteria[i]
			// + "_RA-"
			// + H_L_R[j] + "_10.txt";
			//
			// testSets[0] = TestSetManager.getTestSets_upperImprovement(
			// appClassName, c, testpool, testSetNum, saveFile,
			// H_L_R[j], size_ART);
			//
			// saveFile = saveFile.substring(0, saveFile.indexOf(".txt"))
			//							+ "_CI.txt";
			//					TestSetManager.attachTSWithCI_Activation_replacement(
			//							testSets[0], saveFile);
			//				}
			//
			//			}
			//		}
			
			CFG g = new CFG(System.getProperty("user.dir")
					+ "/src/ccr/app/TestCFG2.java");
			Criterion c = null;
			String testPoolFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
					+ date + "/TestPool.txt";
			TestSet testpool = getTestPool(testPoolFile, true);

			Adequacy.loadTestCase(testPoolFile);

			TestSet[][] testSets = new TestSet[1][];
			String versionPackageName = "testversion";
			String saveFile = null;

			if (criterion.equals("AllPolicies"))
				c = g.getAllPolicies();
			else if (criterion.equals("All1ResolvedDU"))
				c = g.getAllKResolvedDU(1);
			else if (criterion.equals("All2ResolvedDU"))
				c = g.getAllKResolvedDU(2);
			
			saveFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
					+ date
					+ "/UpperImprove/"
					+ criterion
					+ "_RA-"
					+ H_L_R + "_10.txt";

			testSets[0] = TestSetManager.getTestSets_upperImprovement(
					appClassName, c, testpool, testSetNum, saveFile,
					H_L_R, size_ART);

			saveFile = saveFile.substring(0, saveFile.indexOf(".txt"))
					+ "_CI.txt";
			TestSetManager.attachTSWithCI_Activation_replacement(
					testSets[0], saveFile);
		}else if(instruction.equals("getDUCovCITestSet")){
			//2009-10-30:when ties occur, we use DUCoverage as the first principle, 
			//CI as the second principle to solve them
			//getDUCovCITestSet 20091029 2 AllPolicies -1 new random H 10
			String date = args[1];
			int testSetNum = Integer.parseInt(args[2]);
			String criterion = args[3];
			int testSuiteSize = Integer.parseInt(args[4]);
			String oldOrNew = args[5];
			String randomOrCriterion = args[6];
			String H_L_R = args[7];
			int size_ART = Integer.parseInt(args[8]);

			CFG g = new CFG(System.getProperty("user.dir")
					+ "/src/ccr/app/TestCFG2.java");
			Criterion c = null;

			String testPoolFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
					+ date + "/TestPool.txt";
			TestSet testpool = getTestPool(testPoolFile, true);

			Adequacy.loadTestCase(testPoolFile);

			TestSet[][] testSets = new TestSet[1][];
			String versionPackageName = "testversion";
			String saveFile = null;

			if (criterion.equals("AllPolicies"))
				c = g.getAllPolicies();
			else if (criterion.equals("All1ResolvedDU"))
				c = g.getAllKResolvedDU(1);
			else if (criterion.equals("All2ResolvedDU"))
				c = g.getAllKResolvedDU(2);

			if (testSuiteSize < 0) {
				if (oldOrNew.equals("old")) {// Conventional test suite
												// construction algorithm
					saveFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
							+ date + "/" + size_ART + "/" // 2009-10-29:we
															// change the saving
															// folder here
							+ criterion + "_CA_" + size_ART + ".txt";

				} else if (oldOrNew.equals("new")) {
					if (H_L_R.equals("H")) { // Refined test suite
												// construction algorithm with
												// high context diversity
						saveFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
								+ date + "/" + size_ART + "/"// 2009-10-29:we
																// change the
																// saving folder
																// here
								+ criterion + "_RA-H_" + size_ART + ".txt";
					} else if (H_L_R.equals("L")) {// Refined test suite
													// construction algorithm
													// with low context
													// diversity
						saveFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
								+ date + "/" + size_ART + "/"// 2009-10-29:we
																// change the
																// saving folder
																// here
								+ criterion + "_RA-L_" + size_ART + ".txt";
					} else if (H_L_R.equals("R")) {// Refined test suite
													// construction algorithm
													// with random context
													// diversity
						saveFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
								+ date + "/" + size_ART + "/"// 2009-10-29:we
																// change the
																// saving folder
																// here
								+ criterion + "_RA-R_" + size_ART + ".txt";
					}
				}
			} else {
				if (oldOrNew.equals("old")) {
					saveFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
							+ date
							+ "/"
							+ size_ART
							+ "/"// 2009-10-29:we change the saving folder
									// here
							+ criterion
							+ "_CA_"
							+ randomOrCriterion
							+ "_"
							+ testSuiteSize + "_" + size_ART + ".txt"; // 							
				} else if (oldOrNew.equals("new")) {
					if (H_L_R.equals("H")) {
						saveFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
								+ date
								+ "/"
								+ size_ART
								+ "/"// 2009-10-29:we change the saving
										// folder here
								+ criterion
								+ "_RA-H_"
								+ randomOrCriterion
								+ "_" + testSuiteSize + "_" + size_ART + ".txt";
					} else if (H_L_R.equals("L")) {
						saveFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
								+ date
								+ "/"
								+ size_ART
								+ "/"// 2009-10-29:we change the saving
										// folder here
								+ criterion
								+ "_RA-L_"
								+ randomOrCriterion
								+ "_" + testSuiteSize + "_" + size_ART + ".txt";
					} else if (H_L_R.equals("R")) {
						saveFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
								+ date
								+ "/"
								+ size_ART
								+ "/"// 2009-10-29:we change the saving
										// folder here
								+ criterion
								+ "_RA-R_"
								+ randomOrCriterion
								+ "_" + testSuiteSize + "_" + size_ART + ".txt";
					}
				}
			}
			
			testSets[0] = TestSetManager.getTestSets_DUCovCI(appClassName, c, 
					testpool, oldOrNew, maxTrials, testSetNum, saveFile, H_L_R, size_ART);

			saveFile = saveFile.substring(0, saveFile.indexOf(".txt"))
					+ "_CI.txt";
			// 2009-10-15: attach test sets with CI and activation information
			TestSetManager.attachTSWithCI_Activation_replacement(testSets[0],
					saveFile);
		}else if(instruction.equals("getRandomTestSet_CD")){
			//2009-10-31:construct test sets using only CD information to study how CD can help 
			//black-box testing
			String date = args[1];
			int testSetNum = Integer.parseInt(args[2]);			
			int testSuiteSize = Integer.parseInt(args[3]);						
			String H_L_R = args[4];
			int size_ART = Integer.parseInt(args[5]);

			CFG g = new CFG(System.getProperty("user.dir")
					+ "/src/ccr/app/TestCFG2.java");
			Criterion c = null;

			String testPoolFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
					+ "/20091019/TestPool.txt";
			TestSet testpool = getTestPool(testPoolFile, true);

			Adequacy.loadTestCase(testPoolFile);

			TestSet[][] testSets = new TestSet[1][];
			String versionPackageName = "testversion";
			String saveFile =  null;

			if(H_L_R.equals("H")){
				saveFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
					+ date + "/" + size_ART + "/"
					+ "CD-H_" + testSuiteSize + "_"+size_ART + ".txt";
			}else if(H_L_R.equals("L")){
				saveFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
					+ date + "/" + size_ART + "/"
					+ "CD-L_" + testSuiteSize + "_"+size_ART + ".txt";
			}else if(H_L_R.equals("R")){
				saveFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
					+ date + "/" + size_ART + "/"
					+ "CD-L_" + testSuiteSize + "_"+size_ART + ".txt";
			}
			
			
			testSets[0] = TestSetManager.getTestSets_CD(testpool, 
					testSetNum, testSuiteSize, saveFile, H_L_R, size_ART);

			saveFile = saveFile.substring(0, saveFile.indexOf(".txt"))
					+ "_CI.txt";
			
			TestSetManager.attachTSWithCI_Activation_replacement(testSets[0],
					saveFile);
		}
		
		
	}
}
