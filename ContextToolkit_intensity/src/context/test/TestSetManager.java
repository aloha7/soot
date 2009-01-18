package context.test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;
import java.util.Vector;

import context.arch.generator.PositionIButton;
import context.arch.widget.WTourDemo;
import context.arch.widget.WTourEnd;
import context.arch.widget.WTourRegistration;
import context.test.contextIntensity.Manipulator;
import context.test.util.Constant;
import context.test.util.Logger;

public class TestSetManager {

	public Vector testPool;
	public Vector testSets;
	public int SIZE_TESTPOOL = 1000;
	public int NUMBER_TESTSET = 1;
	public int MAX_LENGTH_TESTCASE = 10;
	public int MIN_LENGTH_TESTCASE = 4;

	public void generateAllTestSets(String criteriaFile) {
		this.testSets = new Vector();
		do {
			Vector testSet = this.generateAdequateTestSet(criteriaFile);
			// testSets.add(testSet);
			if (!testSets.contains(testSet)) {
				testSets.add(testSet);
			}
		} while (testSets.size() < NUMBER_TESTSET);
	}

	public void generateAllTestSetsAndSave(String criteriaFile, String savePath) {

		StringBuilder sb = new StringBuilder();
		this.testSets = new Vector();

		do {
			long start = System.currentTimeMillis();
			Vector testSet = this.generateAdequateTestSet(criteriaFile);

			long enduration = System.currentTimeMillis() - start;

			// 1. judge whether duplicated
			if (!testSets.contains(testSet)) {
				// 2. save test case
				for (int i = 0; i < testSet.size(); i++) {
					sb.append(testSet.get(i) + "\t");
				}
				sb.append("time:\t" + enduration + "\n");
				testSets.add(testSet);
			}
			System.out.println(testSets.size() + "th test set");
		} while (testSets.size() < NUMBER_TESTSET);

		// 3. save testSets into files
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(savePath,
					true));
			bw.write(sb.toString());
			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public Vector getAdequateTestSetsFromFile(String filePath){
		Vector testSets = new Vector();
		try {
			BufferedReader br = new BufferedReader(new FileReader(filePath));
			String line = null;
			while((line = br.readLine())!=null){
				Vector testSet = new Vector();
				
				line = line.substring(0, line.indexOf("time:"));
				int index = line.indexOf("\t");
				while(index > -1){
					String testcase = line.substring(0, index);
					testSet.add(testcase);
					line = line.substring(index + "\t".length());
					index = line.indexOf("\t");
				}
				testSets.add(testSet);
			}
			br.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return testSets;
	}
	
	public Vector generateAdequateTestSet(String criteriaFile) {
		// 2009/1/17:

		Vector testSet = new Vector();
		long start = System.currentTimeMillis();
		// 1.load all drivers for a specified criteria
		Manipulator manager = Manipulator.getInstance(criteriaFile);
		Vector drivers = manager.getAllUncoveredDrivers();
		Vector leftDrivers;

		do {
			// 2. random select a test case
			int testCaseIndex = this.getTestCaseIndex(testSet);
			// int testCaseIndex = 0;
			String testCase = (String) this.testPool.get(testCaseIndex);

			// 3.feed test case to SUT: faulty version index(must be golden
			// version 0) + test case index
			PositionIButton.getInstance().set(0, testCaseIndex, testCase);
			PositionIButton.getInstance().runTestCase();
			PositionIButton.getInstance().stopRunning();

			// 4.judge whether this test case increases the coverage
			leftDrivers = manager.getAllUncoveredDrivers();
			if (leftDrivers.size() < drivers.size() && !testSet.contains(testCaseIndex)) {
				// testSet.add(testCase);
				// 2009/1/18: it is better to keep index instead of contents
				testSet.add(testCaseIndex);
			}

			// 5.re-initial the Manipulator
			drivers = leftDrivers;
			manager.setDrivers(drivers);
		} while (leftDrivers.size() > 0);// 6.feed more test cases if there
											// are some uncovered drivers
		long enduration = System.currentTimeMillis() - start;
		System.err.println("Get an adequate test set (time: " + enduration
				+ "):" + "\n" + this.toString(testSet));
		return testSet;
	}
	
	public String getTestCaseInstance(int testCaseIndex){
		if(this.testPool==null){
			this.loadTestPoolFromFile(Constant.baseFolder
					+ "ContextIntensity/TestPool.txt");
		}
		return (String)this.testPool.get(testCaseIndex);
	}
	
	public Vector generateTestPool(Vector events) {
		testPool = new Vector();
		while (testPool.size() < SIZE_TESTPOOL) {
			// generate a test case
			String testCase = this.generateTestCase(events);
			// add it to the test pool if not duplicated
			if (!testPool.contains(testCase)) {
				testPool.add(testCase);
			}
		}
		return testPool;
	}

	// 2009/1/18: it can generate lots of unvalid test cases if no restrictions
	// at all
	public Vector generateRestrictedTestPool(Vector events) {
		testPool = new Vector();
		while (testPool.size() < SIZE_TESTPOOL) {
			// generate a test case
			String testCase = this.generateRestrictedTestCase(events);
//			System.out.println(testPool.size() + "th test case");
			// add it to the test pool if not duplicated
			if (!testPool.contains(testCase)) {
				testPool.add(testCase);
			}
		}
		return testPool;
	}

	public String generateRestrictedTestCase(Vector events) {
		StringBuilder sb = new StringBuilder();
		Random rand = new Random();
		int length_TS;

		// length of test cases:[MIN_LENGTH_TESTCASE, MAX_LENGTH_TESTCASE]
		do {
			length_TS = rand.nextInt(this.MAX_LENGTH_TESTCASE);
		} while (length_TS < MIN_LENGTH_TESTCASE);

		for (int i = 0; i < events.size(); i++) {
			sb.append((String) events.get(i) + "\t");
		}

		length_TS = length_TS - events.size();
		for (int i = 0; i < length_TS - 1; i++) {
			int event_index = rand.nextInt(events.size());
			sb.append((String) events.get(event_index) + "\t");
		}
		sb.append((String) events.get(rand.nextInt(events.size())));
		return sb.toString();
	}

	public String generateTestCase(Vector events) {
		StringBuilder sb = new StringBuilder();
		Random rand = new Random();
		int length_TS;

		// length of test cases:[MIN_LENGTH_TESTCASE, MAX_LENGTH_TESTCASE]
		do {
			length_TS = rand.nextInt(this.MAX_LENGTH_TESTCASE);
		} while (length_TS < MIN_LENGTH_TESTCASE);

		for (int i = 0; i < length_TS - 1; i++) {
			int event_index = rand.nextInt(events.size());
			sb.append((String) events.get(event_index) + "\t");
		}
		sb.append((String) events.get(rand.nextInt(events.size())));
		return sb.toString();
	}

	/**get a test case randomly from a test pool which is different from testSet
	 * 
	 * @param testSet
	 * @return
	 */
	public String getTestCase(Vector testSet) {
		String testCase;
		do {
			testCase = (String) testPool.get((new Random()).nextInt(testPool
					.size()));
		} while (testSet.contains(testCase));

		return testCase;
	}

	/**
	 * randomly get a test case which is different from any elements in testSet
	 * 
	 * @param testSet
	 * @return
	 */
	public int getTestCaseIndex(Vector testSet) {
		int testCaseIndex;
		String testCase;
		Random rand = new Random();

		do {
			testCaseIndex = rand.nextInt(testPool.size());
			testCase = (String) testPool.get(testCaseIndex);
		} while (testSet.contains(testCase));

		return testCaseIndex;
	}

	public void saveTestArtifacts(String path, Vector data) {
		Logger.getInstance().setPath(path, false);
		Logger.getInstance().write(this.toString(data));
		Logger.getInstance().close();
	}

	private String toString(Vector data) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < data.size(); i++) {
			if (data.get(i) instanceof Vector) {
				sb.append(this.toString((Vector) data.get(i)));
			} else {
				sb.append(data.get(i) + "\n");
			}
		}
		return sb.toString();
	}

	private void loadTestPoolFromFile(String testPoolFile) {
		try {
			BufferedReader br = new BufferedReader(new FileReader(testPoolFile));
			String testCase = null;
			this.testPool = new Vector();
			while ((testCase = br.readLine()) != null) {
				this.testPool.add(testCase);
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {

		Vector events = new Vector();
		events.add(WTourRegistration.UPDATE);
		events.add(WTourDemo.INTEREST);
		events.add(WTourDemo.VISIT);
		events.add(WTourEnd.END);

		TestSetManager manager = new TestSetManager();

		// 1. [option]generate test pools
//		 manager.generateTestPool(events);
//		 manager.generateRestrictedTestPool(events);
//		 manager.saveTestArtifacts(Constant.baseFolder +
//		 "ContextIntensity/TestPool.txt", manager.testPool);

		// 2. [mandatory]load test pools from files
		manager.loadTestPoolFromFile(Constant.baseFolder
				+ "ContextIntensity/TestPool.txt");


		
		// 3. [mandatory]get adequacy test sets
//		 manager.generateAdequateTestSet(Constant.baseFolder +
//		 "ContextIntensity/Drivers/Drivers_CA.txt");
		// manager.generateAllTestSets(Constant.baseFolder +
		// "ContextIntensity/Drivers/Drivers_CA.txt");
		manager.generateAllTestSetsAndSave(Constant.baseFolder
				+ "ContextIntensity/Drivers/Drivers_CA.txt",
				Constant.baseFolder
						+ "ContextIntensity/AdequateTestSet/TestSet_CA.txt");

//		Vector testSets = manager.getAdequateTestSetsFromFile(Constant.baseFolder
//		+ "ContextIntensity/AdequateTestSet/TestSet_CA.txt");
//System.out.println();
		
		// String str_testPool = manager.toString(manager.testPool);
		// String str_testPool = manager.toString(all);

		// System.out.println(str_testPool);
		System.exit(0);
	}
}
