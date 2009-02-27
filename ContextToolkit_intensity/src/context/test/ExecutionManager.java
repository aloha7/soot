package context.test;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import context.arch.generator.PositionIButton;
import context.test.contextIntensity.Manipulator;
import context.test.util.Constant;
import context.test.util.ContextStream;
import context.test.util.Logger;

public class ExecutionManager {

	public TestSetManager manager_TS;
	public Manipulator manager;

	/**2009-02-27:
	 * 
	 * @param testSet:a test set only contains the index of a test case.
	 * @param testpool
	 * @param driverFile
	 * @param iteration
	 */
	public static void getOracleWithDrivers(Vector testSets, Hashtable testpool, String date, String criteria, int iteration){
		for(int i = 0; i < testSets.size(); i ++){
			System.out.println("Test Set " + i + " has been executed!");
			ExecutionManager.getOracleWithDrivers((Vector)testSets.get(i), i, testpool, date, criteria, iteration);	
		}
	}
	
	
	
	/**20090227: get the oracle of the golden versions when drivers are available, we need to run it
	 * multiple times to iterate all its output
	 * 
	 * @param testSet
	 * @param testSetIndex
	 * @param testpool
	 * @param date
	 * @param criteria
	 * @param iteration
	 */
	public static void getOracleWithDrivers(Vector testSet, int testSetIndex, Hashtable testpool, String date, String criteria, int iteration ){
		
		// 1.load all drivers for a specified criteria
		String driverFile =  "ContextIntensity/" + date + "/Drivers_" + criteria + ".txt";
		Manipulator manager = Manipulator.getInstance(driverFile);
		Vector drivers = manager.getAllUncoveredDrivers();
		Vector leftDrivers;

		for(int i = 0; i < testSet.size(); i ++){
			String testCaseIndex = (String)testSet.get(i);
			ContextStream testCase = (ContextStream)testpool.get(testCaseIndex);
			StringBuilder sb = new StringBuilder();
			
			for(int k = 0; k < testCase.eventSequence.size(); k ++){
				sb.append(((String)testCase.eventSequence.get(k)).trim() + "\t");
			}
			
			for(int j = 0; j < iteration; j ++){
				String saveFile = "ContextIntensity/" + date + "/Output/0/";
				if(!new File(saveFile).exists()){
					new File(saveFile).mkdirs();
				}
				
				//we do not keep intermediate results dynamically, if we keep output in a Hashtable, then the result will be improved
				saveFile += testSetIndex + "_" + testCaseIndex + ".txt";
				try {
					FileOutputStream outStr = new FileOutputStream(saveFile, true);
					BufferedOutputStream bufStr = new BufferedOutputStream(outStr);
					PrintStream ps = new PrintStream(bufStr);
					System.setOut(ps);
					System.setErr(ps);
					// 6. execute the test case
					int versionNumber = 0; //golden version
					PositionIButton.getInstance().set(versionNumber, Integer.parseInt(testCaseIndex), sb.toString());
					PositionIButton.getInstance().runTestCase();
					PositionIButton.getInstance().stopRunning();

					// 7. close the output
					outStr.close();
					bufStr.close();
					ps.close();
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				manager.setDrivers(drivers); // reset the drivers
			}
		}
		
	}
	
	
	/**
	 * 2009-02-27: get the oracle of the golden versions when no drivers are available, we need to run it
	 * multiple times to iterate all its output
	 * 
	 * @param testpool
	 * @param min_TestCase
	 * @param max_TestCase
	 * @param iteration
	 */
	public static void getOracle(Hashtable testpool, int min_TestCase,
			int max_TestCase, int iteration) {
		Vector events = new Vector();

		for (int i = min_TestCase; i < max_TestCase; i++) {
			Vector cs = ((ContextStream) testpool.get("" + i)).eventSequence;
			StringBuilder event = new StringBuilder();
			for (int j = 0; j < cs.size(); j++) {
				event.append(((String) cs.get(j)).trim() + "\t");
			}
			events.add(event.toString());
		}

		// directory to keep all oracles of the golden versions
		String directory = "test/output/FailureRate/0/";

		// Logger.getInstance().delete(directory);
		if (!new File(directory).exists()) {
			new File(directory).mkdirs();
		}

		// 2009/2/11: when deriving oracles, it needs not to separate
		// different executions
		for(int i = 0; i < events.size(); i ++){
			String testCaseInstance = (String)events.get(i);
			String file = directory + "/" + (min_TestCase + i) + ".txt";	
			
			long start = System.currentTimeMillis();
			
			try {
				for(int k = 0; k < iteration; k ++){
						FileOutputStream outStr = new FileOutputStream(file, false);
						BufferedOutputStream bufStr = new BufferedOutputStream(outStr);
						PrintStream ps = new PrintStream(bufStr);
						System.setOut(ps);
						System.setErr(ps);
						// 6. execute the test case
						PositionIButton.getInstance().set(0, i,
								testCaseInstance);
						PositionIButton.getInstance().runTestCase();
						PositionIButton.getInstance().stopRunning();

						// 7. close the output
						outStr.close();
						bufStr.close();
						ps.close();
				}
				// 2009/2/11: for debugging purpose
				 System.out.println("Test case:" + (min_TestCase + i) + " has been executed in (" + (System.currentTimeMillis()-start) + ")");
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public void getOutput(Vector testPool, int iteration) {
		OutputManager manager_OP = new OutputManager();
		String saveFile = Constant.baseFolder
				+ "/test/output/failedTestCase.txt";
		// manager_OP.saveFailedTestCase(saveFile);
		String[] testCaseList = manager_OP.getFailedTestCase(saveFile);

		for (String testCase : testCaseList) {
			int testCaseIndex = Integer.parseInt(testCase);
			this.getOutput(0, 0, testPool, iteration, testCaseIndex,
					testCaseIndex);
		}
	}

	/**
	 * 2009/2/11: due to the non-deterministic execution of concurrent programs,
	 * a test case may produce different output. Thus it requires multiple
	 * runnings of a test case to enumerate all outputs
	 * 
	 * @param min_Version
	 * @param max_Version
	 * @param testPool
	 * @param iteration
	 * @param min_TestCase
	 * @param max_TestCase
	 */
	public void getOutput(int min_Version, int max_Version, Vector testPool,
			int iteration, int min_TestCase, int max_TestCase) {
		for (int versionNumber = min_Version; versionNumber < max_Version; versionNumber++) {

			// when multiple threads it is dangerous to delete it
			String directory = "test/output/FailureRate/" + versionNumber + "/";
			// Logger.getInstance().delete(directory);
			if (!new File(directory).exists()) {
				new File(directory).mkdirs();
			}

			// 2009/2/11: when deriving oracles, it needs not to separate
			// different executions
			if (versionNumber == 0) {
				for (int i = min_TestCase; i <= max_TestCase; i++) {
					// 1.prepare the directory: delete it when it exists and
					// recreate it

					String testCaseInstance = (String) testPool.get(i);
					String file = directory + "/" + i + ".txt";

					// 2009/2/11: for debugging purpose
					// System.err.println("versionNumber:" + versionNumber +
					// "\n"
					// + "testSetNumber:" + i + "\n" +
					// "testCaseNumber:" + testCaseIndex + "\n" + file + "\n");

					for (int k = 0; k < iteration; k++) {
						try {
							// 5.redirect the output before execution
							FileOutputStream outStr = new FileOutputStream(
									file, true);
							BufferedOutputStream bufStr = new BufferedOutputStream(
									outStr);
							PrintStream ps = new PrintStream(bufStr);

							// if(new File(file).exists()){
							// ps = new PrintStream(
							// new BufferedOutputStream(new FileOutputStream(
							// file, true)));
							// }else{
							// ps = new PrintStream(
							// new BufferedOutputStream(new FileOutputStream(
							// file, false)));
							// }

							System.setOut(ps);
							System.setErr(ps);
							// 6. execute the test case
							PositionIButton.getInstance().set(0, i,
									testCaseInstance);
							PositionIButton.getInstance().runTestCase();
							PositionIButton.getInstance().stopRunning();

							// 7. close the output
							outStr.close();
							bufStr.close();
							ps.close();

						} catch (FileNotFoundException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (IOException ex) {
							ex.printStackTrace();
						}
					}
				}
			} else { // for faulty versions, it needs to different executions
				// by different file names
				for (int i = min_TestCase; i <= max_TestCase; i++) {
					// 1.prepare the directory: delete it when it exists and
					// recreate it

					String testCaseInstance = (String) testPool.get(i);

					// 2009/2/11: for debugging purpose
					// System.err.println("versionNumber:" + versionNumber +
					// "\n"
					// + "testSetNumber:" + i + "\n" +
					// "testCaseNumber:" + testCaseIndex + "\n" + file + "\n");

					for (int k = 0; k < iteration; k++) {
						try {
							// 5.redirect the output before execution
							String file = directory + "/" + i + "_" + k
									+ ".txt";
							FileOutputStream outStr = new FileOutputStream(
									file, true);
							BufferedOutputStream bufStr = new BufferedOutputStream(
									outStr);
							PrintStream ps = new PrintStream(bufStr);

							// if(new File(file).exists()){
							// ps = new PrintStream(
							// new BufferedOutputStream(new FileOutputStream(
							// file, true)));
							// }else{
							// ps = new PrintStream(
							// new BufferedOutputStream(new FileOutputStream(
							// file, false)));
							// }

							System.setOut(ps);
							System.setErr(ps);
							// 6. execute the test case
							PositionIButton.getInstance().set(0, i,
									testCaseInstance);
							PositionIButton.getInstance().runTestCase();
							PositionIButton.getInstance().stopRunning();

							// 7. close the output
							outStr.close();
							bufStr.close();
							ps.close();

						} catch (FileNotFoundException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (IOException ex) {
							ex.printStackTrace();
						}
					}
				}
			}

		}
	}

	public void getOutput(int min_Version, int max_Version, Vector testPool,
			int iteration) {
		this.getOutput(min_Version, max_Version, testPool, iteration, 0,
				testPool.size() - 1);
	}

	// 2009-02-27:reshape the format of a test pool
	public void getOutput(int min_Version, int max_Version, Hashtable testpool,
			int iteration) {
		Vector events = new Vector();
		Iterator ite = testpool.values().iterator();
		while (ite.hasNext()) {
			ContextStream cs = (ContextStream) ite.next();
			events.add(cs.eventSequence);
		}

		this.getOutput(min_Version, max_Version, events, iteration);
	}

	public void executeTestSets(int min_Version, int max_Version,
			String criteria, Vector testSets) {

		// Vector drivers = this.manager.driverMatrix;

		for (int versionNumber = min_Version; versionNumber <= max_Version; versionNumber++) {
			for (int i = 0; i < testSets.size(); i++) {

				// 4.prepare the directory: delete it when it exists and
				// recreate it
				String directory = Constant.baseFolder + "test/output/"
						+ criteria + "/" + versionNumber + "/" + i;
				Logger.getInstance().delete(directory);
				new File(directory).mkdirs();

				Vector testSet = (Vector) testSets.get(i);
				for (int j = 0; j < testSet.size(); j++) {
					int testCaseIndex = Integer.parseInt((String) testSet
							.get(j));
					String testCaseInstance = manager_TS
							.getTestCaseInstance(testCaseIndex);
					String file = directory + "/" + testCaseIndex + ".txt";
					// System.err.println("versionNumber:" + versionNumber +
					// "\n"
					// + "testSetNumber:" + i + "\n" +
					// "testCaseNumber:" + testCaseIndex + "\n" + file + "\n");
					try {
						// 5.redirect the output before execution
						PrintStream ps = new PrintStream(
								new BufferedOutputStream(new FileOutputStream(
										file)));
						System.setOut(ps);
						System.setErr(ps);
						// 6. execute the test case
						PositionIButton.getInstance().set(0, testCaseIndex,
								testCaseInstance);
						PositionIButton.getInstance().runTestCase();
						PositionIButton.getInstance().stopRunning();

						// 7. close the output
						ps.close();
						// 8. reset the drivers

					} catch (FileNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				manager = Manipulator.getInstance(Constant.baseFolder
						+ "ContextIntensity/Drivers/Drivers_CA.txt"); // reinitial
				// it
				// for
				// each
				// test
				// set
				// manager.setDrivers(drivers);
			}
		}
	}

	/**
	 * load valid test cases and test sets
	 * 
	 * @param criteria
	 * @param analysisPath
	 * @return faultVersion->(TestSet)*-> (TestCase)*
	 */

	public static void main(String[] args) {
		
		//2009-02-27:
		System.out
				.println("USAGE:<Date(20090227)><MinFaultyVersion(0)><MaxFaultyVersion(237)><ExecutionTimes(100)><Criteria(CA)>");

		String date = args[0];
		int min_TestCase = Integer.parseInt(args[1]);
		int max_TestCase = Integer.parseInt(args[2]);
		int iteration = Integer.parseInt(args[3]);
		String criteria = args[4];
		

		String testPoolFile = "ContextIntensity/" + date + "/TestPool.txt";
		boolean containHeader = true;
		Hashtable testpool = TestSetManager.loadTestPoolFromFile(containHeader,
				testPoolFile);
		
		//1. get oracles of golden versions under the condition when no drivers are available
		String testDriverFile = "ContextIntensity/" + date + "";
//		TestManagers.getOracle(testpool, min_TestCase, max_TestCase, iteration);

		//2. get oracles of golden versions under the condition that drivers are present
		String testSetFile = "ContextIntensity/" + date + "/" + criteria + "TestSets.txt";
		containHeader = false;
		Vector testSets = TestSetManager.getAdequateTestSetsFromFile(containHeader, testSetFile);
		ExecutionManager.getOracleWithDrivers(testSets, testpool, date, criteria, iteration);
		
		
//		test.getOutput(min_Version, max_Version, testpool, iteration);

		// // //1. 2009/2/11: get output for all faulty versions
		// String testPoolFile = Constant.baseFolder
		// + "ContextIntensity/TestPool.txt";
		// test.manager_TS.loadTestPoolFromFile(testPoolFile);
		// Vector testPool = test.manager_TS.testPool;
		// //
		// int min_Version, max_Version, iteration;
		//
		// if (args.length == 0) { // slowest but most automated
		// min_Version = 0;
		// max_Version = 236;
		// iteration = 100;
		//
		// long start = System.currentTimeMillis();
		// test.getOutput(min_Version, max_Version, testPool, iteration);
		// System.out.println(System.currentTimeMillis() - start);
		// } else if (args.length == 1) {
		// iteration = Integer.parseInt(args[0]);
		// test.getOutput(testPool, iteration);
		// } else if (args.length == 3) {
		// min_Version = Integer.parseInt(args[0]);
		// max_Version = Integer.parseInt(args[1]);
		// iteration = Integer.parseInt(args[2]);
		//
		// long start = System.currentTimeMillis();
		// test.getOutput(min_Version, max_Version, testPool, iteration);
		// System.out.println(System.currentTimeMillis() - start);
		//
		// } else if (args.length == 5) {
		// // fasted, require about 40 minutes to generate 1000*100 outputs if
		// // 10 threads are opened
		// min_Version = Integer.parseInt(args[0]);
		// max_Version = Integer.parseInt(args[1]);
		// iteration = Integer.parseInt(args[2]);
		//
		// int min_TestCase = Integer.parseInt(args[3]);
		// int max_TestCase = Integer.parseInt(args[4]);
		// test.getOutput(min_Version, max_Version, testPool, iteration,
		// min_TestCase, max_TestCase);
		// }
		//
		// // 1. get Adequacy test sets from files
		// String file_TS = Constant.baseFolder
		// + "ContextIntensity/AdequateTestSet/TestSet_CA.txt";
		// //
		// // String file_TS = Constant.baseFolder
		// // + "ContextIntensity/AdequateTestSet/TestSet_Stoc1.txt";
		//
		// Vector testSets =
		// test.manager_TS.getAdequateTestSetsFromFile(file_TS);
		//
		// // 2. set the Drivers before execution
		// String criteriaFile = Constant.baseFolder
		// + "ContextIntensity/Drivers/Drivers_CA.txt";
		//
		// // String criteriaFile = Constant.baseFolder
		// // + "ContextIntensity/Drivers/Drivers_Stoc1.txt";
		// // test.manager = Manipulator.getInstance(criteriaFile);
		//
		// // 2009/1/18:
		// // 3.execute adequate test sets to see whether exceptions are thrown
		// int min_Version = 0;
		// int max_Version = 0;
		// String criteria = "CA";
		// if (args.length == 2) {
		// min_Version = Integer.parseInt(args[0]);
		// max_Version = Integer.parseInt(args[1]);
		//		}
		//		test.executeTestSets(min_Version, max_Version, criteria, testSets);
				System.exit(0);
	}

}
