package ccr.test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.Scanner;
import java.util.Vector;

import ccr.stat.CFG;
import ccr.stat.Criterion;
import ccr.stat.Node;
import ccr.stat.NodeIndex;
import ccr.stat.Policy;
import ccr.stat.PolicyNode;
import ccr.stat.VariableSet;

public class TestSetManager {
	public static HashMap testCases = new HashMap();

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

	//2009-02-25: random test sets serve as the baseline for comparison
	public static TestSet[] getRandomTestSets(String appClassName, TestSet testpool,
			int testSetNum, int testSuiteSize, String saveFile){
		TestSet[] testSets = new TestSet[testSetNum];
		for(int i =0; i < testSetNum; i ++){
			testSets[i] = TestSetManager.getRandomTestSet(appClassName, testpool, testSuiteSize);
			testSets[i].index = ""+i;
			System.out.println("Test set " + i + ": " + testSets[i].toString());
		}
		
		TestSetManager.saveTestSets(testSets, saveFile);
		return testSets;
	}
	
	// 2009-02-22:
	public static TestSet getRandomTestSet(String appClassName,
			TestSet testpool, int testSuiteSize) {
		TestSet testSet = new TestSet();
		TestSet visited = new TestSet();

		while (testSet.size() < testSuiteSize) {
			String testcase = testpool.getByRandom();
			if (!visited.contains(testcase) /*&& !testSet.contains(testcase)*/) {
				visited.add(testcase);
				testSet.add(testcase);
			}
		}
		return testSet;
	}

	public static TestSet getAdequacyTestSet(String appClassName, Criterion c,
			TestSet testpool, int maxTrials, double min_CI, double max_CI,
			String newOrOld) {
		TestSet testSet = null;
		if (newOrOld.equals("new")) {
			testSet = TestSetManager.getAdequacyTestSet_refined(appClassName,
					c, testpool, maxTrials, min_CI, max_CI);
		} else if (newOrOld.equals("old")) {
			testSet = TestSetManager.getAdequacyTestSet_conventional(
					appClassName, c, testpool, maxTrials);
		}
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
	 * @return
	 */
	public static TestSet getAdequacyTestSet_refined(String appClassName,
			Criterion c, TestSet testpool, int maxTrials, double min_CI,
			double max_CI) {

		int replaceCounter = 0; // record the replace times
		Criterion criterion = (Criterion) c.clone();
		TestSet testSet = new TestSet();
		TestSet visited = new TestSet();
		HashMap testcase_uniqueTraces = new HashMap();

		long time = System.currentTimeMillis();

		int originalSize = criterion.size();
		while (visited.size() < maxTrials && visited.size() < testpool.size()
				&& criterion.size() > 0) {
//			String testcase = testpool.getByRandom();
			String testcase = testpool.getByART();
			
			// just for debugging purpose
			// TestCase testCase = (TestCase) Adequacy.testCases.get(testcase);

			if (!visited.contains(testcase)) {
				visited.add(testcase);
				String stringTrace[] = TestDriver.getTrace(appClassName,
						testcase);

				ArrayList uniqueCover = increaseCoverage(stringTrace, criterion);
				if (uniqueCover.size() > 0) {
					testcase_uniqueTraces.put(testcase, uniqueCover);
					testSet.add(testcase);
				} else {
					// 2009-2-21: if the test case does not increase the
					// coverage
					double CI = ((TestCase) (Adequacy.testCases.get(testcase))).CI;

					Vector testcases = testSet.testcases;
					ArrayList replaced = new ArrayList(); // keep all test
					// cases in the test
					// set that has
					// lower CI values
					// in ascending
					// orders

					for (int i = 0; i < testcases.size(); i++) { // check
						// whether
						// some test
						// cases
						// have
						// lower CI
						// than
						// current
						// ones.
						TestCase temp = (TestCase) Adequacy.testCases
								.get((String) testcases.get(i));
						double CI_temp = temp.CI;
						if (CI_temp < CI) { // add temp to replace queue which
							// is sorted by ascending order of
							// CI
							if (replaced.size() > 0) {
								int j = 0;
								boolean added = false;
								for (; j < replaced.size(); j++) {
									TestCase replacedTC = (TestCase) replaced
											.get(j);
									double CI_replacedTC = replacedTC.CI;
									if (CI_replacedTC > CI_temp) {
										// it is a correct place to insert
										// current test case(ascending order):
										// temp
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

					// 2009-02-22: two strategies here: 1).select one and
					// replace it randomly;
					// 2).replace the one who has the lowest CI value
					// 1).select one and replace it randomly
					// ArrayList checked = new ArrayList(); //keep all checked
					// test cases in replace queue
					//
					// Random rand = new Random();
					// int index = -1;
					// while (checked.size() < replaced.size()) {
					//
					// //random select a index which has not been checked
					// do {
					// index = rand.nextInt(replaced.size());
					// } while (checked.contains(index));
					//
					// TestCase temp = (TestCase) replaced.get(index);
					// ArrayList temp_uniqueTraces = (ArrayList)
					// testcase_uniqueTraces
					// .get(temp.index);
					//
					// // if a "testcase" contains all unique statements
					// // covered by "temp",
					// // then we can safely replace "temp" with "testcase"
					//
					// int j = 0;
					// for (; j < temp_uniqueTraces.size(); j++) {
					// String unique = (String) temp_uniqueTraces.get(j);
					// if (!traceList.contains(unique))
					// break;
					// }
					// if (j == temp_uniqueTraces.size()) {
					// // replace "temp" with "testcase"
					// testcase_uniqueTraces.remove(temp.index);
					// testcase_uniqueTraces.put(testcase,
					// temp_uniqueTraces);
					//
					// testSet.remove(temp.index);
					// testSet.add(testcase);
					// replaceCounter++;
					// break;
					// }
					//
					// checked.add(index);
					// }

					// 2009-02-24:2).replace the one who has the lowest CI value
					for (int i = 0; i < replaced.size(); i++) {
						TestCase temp = (TestCase) replaced.get(i);
						ArrayList temp_uniqueTraces = (ArrayList) testcase_uniqueTraces
								.get(temp.index);
						// if a "testcase" contains all unique statements
						// covered by "temp",
						// then we can safely replace "temp" with "testcase"
						int j = 0;
						for (; j < temp_uniqueTraces.size(); j++) {
							String unique = (String) temp_uniqueTraces.get(j);
							if (!traceList.contains(unique))
								break;
						}
						if (j == temp_uniqueTraces.size()) {
							// replace "temp" with "testcase"
							testcase_uniqueTraces.remove(temp.index);
							testcase_uniqueTraces.size();
							testcase_uniqueTraces.put(testcase,
									temp_uniqueTraces);

							testSet.remove(temp.index);
							testSet.add(testcase);
							replaceCounter++;

							//2009-02-25:if one test case can only replace another one, then we need "break;"
							//otherwise, we do not need "break;" 
							break;
						}
					}
					
				}
			}
		}

		int currentSize = criterion.size();
		testSet.setCoverage((float) (originalSize - currentSize)
				/ (float) originalSize);
		testSet.replaceCounter = replaceCounter;
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

		int replaceCounter = 0; // record the replace times
		Criterion criterion = (Criterion) c.clone();
		Criterion finalCriterion = null;
		TestSet testSet = new TestSet();
		TestSet visited = new TestSet();
		HashMap testcase_uniqueTraces = new HashMap();

		long time = System.currentTimeMillis();

		int originalSize = criterion.size();
		while (visited.size() < maxTrials && visited.size() < testpool.size()
				&& criterion.size() > 0) {
			String testcase = testpool.getByRandom();
			
//			String testcase = testpool.getByART();
			
			// just for debugging purpose
			// TestCase testCase = (TestCase) Adequacy.testCases.get(testcase);

			if (!visited.contains(testcase)) {
				visited.add(testcase);
				String stringTrace[] = TestDriver.getTrace(appClassName,
						testcase);

				ArrayList uniqueCover = increaseCoverage(stringTrace, criterion);
				if (uniqueCover.size() > 0) {
					testcase_uniqueTraces.put(testcase, uniqueCover);
					testSet.add(testcase);
				} else {
					// 2009-2-21: if the test case does not increase the
					// coverage
					double CI = ((TestCase) (Adequacy.testCases.get(testcase))).CI;

					Vector testcases = testSet.testcases;
					ArrayList replaced = new ArrayList(); // keep all test
					// cases in the test
					// set that has
					// lower CI values
					// in ascending
					// orders

					for (int i = 0; i < testcases.size(); i++) { // check
						// whether
						// some test
						// cases
						// have
						// lower CI
						// than
						// current
						// ones.
						TestCase temp = (TestCase) Adequacy.testCases
								.get((String) testcases.get(i));
						double CI_temp = temp.CI;
						if (CI_temp < CI) { // add temp to replace queue which
							// is sorted by ascending order of
							// CI
							if (replaced.size() > 0) {
								int j = 0;
								boolean added = false;
								for (; j < replaced.size(); j++) {
									TestCase replacedTC = (TestCase) replaced
											.get(j);
									double CI_replacedTC = replacedTC.CI;
									if (CI_replacedTC > CI_temp) {
										// it is a correct place to insert
										// current test case(ascending order):
										// temp
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

					// 2009-02-22: two strategies here: 1).select one and
					// replace it randomly;
					// 2).replace the one who has the lowest CI value
					// 1).select one and replace it randomly
					// ArrayList checked = new ArrayList(); // keep all checked
					// // test cases in
					// // replace queue
					//
					// Random rand = new Random();
					// int index = -1;
					// while (checked.size() < replaced.size()) {
					//
					// // random select a index which has not been checked
					// do {
					// index = rand.nextInt(replaced.size());
					// } while (checked.contains(index));
					//
					// TestCase temp = (TestCase) replaced.get(index);
					// ArrayList temp_uniqueTraces = (ArrayList)
					// testcase_uniqueTraces
					// .get(temp.index);
					//
					// // if a "testcase" contains all unique statements
					// // covered by "temp",
					// // then we can safely replace "temp" with "testcase"
					//
					// int j = 0;
					// for (; j < temp_uniqueTraces.size(); j++) {
					// String unique = (String) temp_uniqueTraces.get(j);
					// if (!traceList.contains(unique))
					// break;
					// }
					// if (j == temp_uniqueTraces.size()) {
					// // replace "temp" with "testcase"
					// testcase_uniqueTraces.remove(temp.index);
					// testcase_uniqueTraces.put(testcase,
					// temp_uniqueTraces);
					//
					// testSet.remove(temp.index);
					// testSet.add(testcase);
					// replaceCounter++;
					// break;
					// }
					//
					// checked.add(index);
					// }

					// 2009-02-24: 2).replace the one who has the lowest CI
					// value
					for (int i = 0; i < replaced.size(); i++) {
						TestCase temp = (TestCase) replaced.get(i);
						ArrayList temp_uniqueTraces = (ArrayList) testcase_uniqueTraces
								.get(temp.index);
						// if a "testcase" contains all unique statements
						// covered by "temp",
						// then we can safely replace "temp" with "testcase"
						int j = 0;
						for (; j < temp_uniqueTraces.size(); j++) {
							String unique = (String) temp_uniqueTraces.get(j);
							if (!traceList.contains(unique))
								break;
						}
						if (j == temp_uniqueTraces.size()) {
							// replace "temp" with "testcase"
							testcase_uniqueTraces.remove(temp.index);
							testcase_uniqueTraces.put(testcase,
									temp_uniqueTraces);

							testSet.remove(temp.index);
							testSet.add(testcase);
							replaceCounter++;
							
							//2009-02-25:if one test case can only replace another one, then we need "break;"
							//otherwise, we do not need "break;" 
							break;
						}
					}
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
//				String testcase = testpool.getByRandom();
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
				// int trial = 0;
				criterion = (Criterion) c.clone();
				
				while (/* trial < maxTrials && */visited.size() < testpool
						.size()
						&& criterion.size() > 0
						&& testSet.size() < testSuiteSize) {
//					String testcase = testpool.getByRandom();
					String testcase = testpool.getByART();
					
					// trial++;
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
							testcase_uniqueTraces.put(testcase, uniqueCover);
							testSet.add(testcase);

							checkCoverage(stringTrace, finalCriterion);
						} else {
							// 2009-2-21: if the test case does not increase the
							// coverage
							double CI = ((TestCase) (Adequacy.testCases
									.get(testcase))).CI;

							Vector testcases = testSet.testcases;
							ArrayList replaced = new ArrayList(); // keep all
							// test
							// cases in the test
							// set that has
							// lower CI values
							// in ascending
							// orders

							for (int i = 0; i < testcases.size(); i++) { // check
								// whether
								// some test
								// cases
								// have
								// lower CI
								// than
								// current
								// ones.
								TestCase temp = (TestCase) Adequacy.testCases
										.get((String) testcases.get(i));
								double CI_temp = temp.CI;
								if (CI_temp < CI) { // add temp to replace queue
									// which
									// is sorted by ascending order of
									// CI
									if (replaced.size() > 0) {
										int j = 0;
										boolean added = false;
										for (; j < replaced.size(); j++) {
											TestCase replacedTC = (TestCase) replaced
													.get(j);
											double CI_replacedTC = replacedTC.CI;
											if (CI_replacedTC > CI_temp) {
												// it is a correct place to
												// insert
												// current test case(ascending
												// order):
												// temp
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

							// just facilitate to compare
							ArrayList traceList = new ArrayList();
							for (int k = 0; k < stringTrace.length; k++)
								traceList.add(stringTrace[k]);

							// 2009-02-22: two strategies here: 1).select one
							// and replace it randomly;
							// 2).replace the one who has the lowest CI value
							// 1).select one and replace it randomly
//							ArrayList checked = new ArrayList(); // keep all
//							// checked
//							// test
//							// cases in
//							// replace
//							// queue
//
//							Random rand = new Random();
//							int index = -1;
//							while (checked.size() < replaced.size()) {
//
//								// random select a index which has not been
//								// checked
//								do {
//									index = rand.nextInt(replaced.size());
//								} while (checked.contains(index));
//
//								TestCase temp = (TestCase) replaced.get(index);
//								ArrayList temp_uniqueTraces = (ArrayList) testcase_uniqueTraces
//										.get(temp.index);
//
//								// if a "testcase" contains all unique
//								// statements
//								// covered by "temp",
//								// then we can safely replace "temp" with
//								// "testcase"
//
//								int j = 0;
//								for (; j < temp_uniqueTraces.size(); j++) {
//									String unique = (String) temp_uniqueTraces
//											.get(j);
//									if (!traceList.contains(unique))
//										break;
//								}
//								if (j == temp_uniqueTraces.size()) {
//									// replace "temp" with "testcase"
//									testcase_uniqueTraces.remove(temp.index);
//									testcase_uniqueTraces.put(testcase,
//											temp_uniqueTraces);
//
//									testSet.remove(temp.index);
//									testSet.add(testcase);
//									replaceCounter++;
//									break;
//								}
//
//								checked.add(index);
//							}

							// 2009-02-24: 2).replace the one who has the lowest CI value
							for (int i = 0; i < replaced.size(); i++) {
								TestCase temp = (TestCase) replaced.get(i);
								ArrayList temp_uniqueTraces = (ArrayList) testcase_uniqueTraces
										.get(temp.index);
								// if a "testcase" contains all unique
								// statements
								// covered by "temp",
								// then we can safely replace "temp" with
								// "testcase"
								int j = 0;
								for (; j < temp_uniqueTraces.size(); j++) {
									String unique = (String) temp_uniqueTraces
											.get(j);
									if (!traceList.contains(unique))
										break;
								}
								if (j == temp_uniqueTraces.size()) {
									// replace "temp" with "testcase"
									testcase_uniqueTraces.remove(temp.index);
									testcase_uniqueTraces.put(testcase,
											temp_uniqueTraces);

									testSet.remove(temp.index);
									testSet.add(testcase);
									replaceCounter++;
									break;
								}
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
		testSet.replaceCounter = replaceCounter;
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
				// int trial = 0;
				criterion = (Criterion) c.clone();

				while (visited.size() < testpool.size()
						&& testSet.size() < testSuiteSize /*
															 * && trial <
															 * maxTrials
															 */
						&& criterion.size() > 0) {
					// trial++;
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
						if (!effectNodes.contains(trace[i])) {
							effectNodes.add(stringTrace[i]);
						}
						if (!effectNodes.contains(trace[j])) {
							effectNodes.add(stringTrace[j]);
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

	public static TestSet[] getTestSets(String appClassName, Criterion c,
			TestSet testpool, int maxTrials, int testSetNum, double min_CI,
			double max_CI, String newOrOld, String randomOrCriteria,
			int testSuiteSize, String saveFile) {
		TestSet[] testSets = new TestSet[testSetNum];
		if (testSuiteSize < 0) { // do not fix the test set size
			if (newOrOld.equals("new")) {
				for (int i = 0; i < testSetNum; i++) {
					testSets[i] = TestSetManager.getAdequacyTestSet_refined(
							appClassName, c, testpool, maxTrials, min_CI,
							max_CI);

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
		} else {// fix the test set size
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
		
		if(args.length == 9){ //2009-02-25: generate adequate test sets
			int testSetNum = Integer.parseInt(args[0]);
			String instruction = args[1];
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

			// 2009-2-21: revise the test case selection strategies: add a test case
			// into a test set
			// if it can increase the cumulative coverage or it has higher CI value
			// than existing one
			// while not decrease the coverage

			String testPoolFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
					+ date + "/TestPool.txt";
			TestSet testpool = getTestPool(testPoolFile, true);

			int maxTrials = 2000;

			if (instruction.equals("Context_Intensity")) {
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
		}else if(args.length == 3){
			int testSetNum = Integer.parseInt(args[0]);
			int testSuiteSize = Integer.parseInt(args[1]);
			String date = args[2];
			String saveFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
				+ date + "/RandomTestSets_"+testSuiteSize+".txt";
			
			String testPoolFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
				+ date + "/TestPool.txt";
			TestSet testpool = getTestPool(testPoolFile, true);
			Adequacy.loadTestCase(testPoolFile);
			
			TestSet[][] testSets = new TestSet[1][];
			testSets[0] = TestSetManager.getRandomTestSets(appClassName, testpool, testSetNum, testSuiteSize, saveFile);
			
			saveFile = saveFile.substring(0, saveFile.indexOf(".txt")) 
				+"_CI.txt";
			TestSetManager.attachTSWithCI(testSets[0], saveFile);
		}
	}
}