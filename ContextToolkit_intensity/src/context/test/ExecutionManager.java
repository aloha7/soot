package context.test;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Vector;

import context.arch.generator.PositionIButton;
import context.test.contextIntensity.Manipulator;
import context.test.util.Constant;
import context.test.util.Logger;

public class ExecutionManager {

	public TestSetManager manager_TS;
	public Manipulator manager;

	
	public void getOutput(Vector testPool, int iteration){
		OutputManager manager_OP = new OutputManager();
		String saveFile = Constant.baseFolder + "/test/output/failedTestCase.txt";
//		manager_OP.saveFailedTestCase(saveFile);
		String[] testCaseList = manager_OP.getFailedTestCase(saveFile);

		for(String testCase: testCaseList){
			int testCaseIndex = Integer.parseInt(testCase);
			this.getOutput(0, 0, testPool, iteration, testCaseIndex, testCaseIndex);
		}
	}
	
	/**2009/2/11: due to the non-deterministic execution of concurrent programs,
	 * a test case may produce different output. Thus it requires multiple runnings
	 * of a test case to enumerate all outputs
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
		for (int versionNumber = min_Version; versionNumber <= max_Version; versionNumber++) {
			
			//when multiple threads it is dangerous to delete it
			String directory = Constant.baseFolder
			+ "test/output/FailureRate/" + versionNumber + "/";
//			Logger.getInstance().delete(directory);
			if(!new File(directory).exists()){
				new File(directory).mkdirs();	
			}
			
			//2009/2/11: when deriving oracles, it needs not to separate different executions 
			if(versionNumber == 0){
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
							FileOutputStream outStr = new FileOutputStream(file, true);
							BufferedOutputStream bufStr = new BufferedOutputStream(outStr);
							PrintStream ps = new PrintStream(bufStr);
							
//							if(new File(file).exists()){
//								ps = new PrintStream(
//										new BufferedOutputStream(new FileOutputStream(
//												file, true)));
//							}else{
//								ps = new PrintStream(
//										new BufferedOutputStream(new FileOutputStream(
//												file, false)));
//							}
							
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
			}else{ //for faulty versions, it needs to different executions by different file names
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
							String file = directory + "/" + i +"_" + k +".txt";
							FileOutputStream outStr = new FileOutputStream(file, true);
							BufferedOutputStream bufStr = new BufferedOutputStream(outStr);
							PrintStream ps = new PrintStream(bufStr);
							
//							if(new File(file).exists()){
//								ps = new PrintStream(
//										new BufferedOutputStream(new FileOutputStream(
//												file, true)));
//							}else{
//								ps = new PrintStream(
//										new BufferedOutputStream(new FileOutputStream(
//												file, false)));
//							}
							
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
		this.getOutput(min_Version, max_Version, testPool, iteration, 0, testPool.size()-1);
	}
	
	/**get failure rate of each faulty version, and list all failed test cases to expose it(context intensity)
	 * 
	 * @param min_Version
	 * @param max_Version
	 */
	public void getFailureRate(int min_Version, int max_Version){
		
		
	}
		
	public void executeTestSets(int min_Version, int max_Version,
			String criteria, Vector testSets) {

//		Vector drivers = this.manager.driverMatrix;

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
//					 System.err.println("versionNumber:" + versionNumber +
//					 "\n"
//					 + "testSetNumber:" + i + "\n" +
//					 "testCaseNumber:" + testCaseIndex + "\n" + file + "\n");
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
				+ "ContextIntensity/Drivers/Drivers_CA.txt"); // reinitial it for each test
												// set
//				manager.setDrivers(drivers);
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
		ExecutionManager test = new ExecutionManager();
		test.manager_TS = new TestSetManager();
		
//		//1. 2009/2/11: get output for all faulty versions
//		String testPoolFile = Constant.baseFolder + "ContextIntensity/TestPool.txt";
//		test.manager_TS.loadTestPoolFromFile(testPoolFile);
//		Vector testPool = test.manager_TS.testPool;
//		
//		int min_Version, max_Version, iteration;
//		
//		if(args.length ==0){ //slowest but most automated
//			min_Version = 0;
//			max_Version = 236;
//			iteration = 100;
//			
//			long start = System.currentTimeMillis();
//			test.getOutput(min_Version, max_Version, testPool, iteration);
//			System.out.println(System.currentTimeMillis()-start);
//		}else if(args.length == 1){
//			iteration = Integer.parseInt(args[0]);
//			test.getOutput(testPool, iteration);
//		}else if(args.length ==3){
//			min_Version = Integer.parseInt(args[0]);
//			max_Version = Integer.parseInt(args[1]);
//			iteration = Integer.parseInt(args[2]);
//			
//			long start = System.currentTimeMillis();
//			test.getOutput(min_Version, max_Version, testPool, iteration);
//			System.out.println(System.currentTimeMillis()-start);
//			
//		}else if(args.length == 5){ 
//			//fasted, require about 40 minutes to generate 1000*100 outputs if 10 threads are opened
//			min_Version = Integer.parseInt(args[0]);
//			max_Version = Integer.parseInt(args[1]);
//			iteration = Integer.parseInt(args[2]);
//			
//			int min_TestCase = Integer.parseInt(args[3]);
//			int max_TestCase = Integer.parseInt(args[4]);
//			test.getOutput(min_Version, max_Version, testPool, iteration, min_TestCase, max_TestCase);
//		}
		
		// 1. get Adequacy test sets from files
		String file_TS = Constant.baseFolder
				+ "ContextIntensity/AdequateTestSet/TestSet_CA.txt";
//		
//		String file_TS = Constant.baseFolder
//		+ "ContextIntensity/AdequateTestSet/TestSet_Stoc1.txt";
		
		Vector testSets = test.manager_TS.getAdequateTestSetsFromFile(file_TS);

		// 2. set the Drivers before execution
		String criteriaFile = Constant.baseFolder
				+ "ContextIntensity/Drivers/Drivers_CA.txt";
		
//		String criteriaFile = Constant.baseFolder
//		+ "ContextIntensity/Drivers/Drivers_Stoc1.txt";
//		test.manager = Manipulator.getInstance(criteriaFile);

		// 2009/1/18: 
		// 3.execute adequate test sets to see whether exceptions are thrown
		int min_Version = 0;
		int max_Version = 0;
		String criteria = "CA";
		if(args.length ==2){
			min_Version = Integer.parseInt(args[0]);
			max_Version = Integer.parseInt(args[1]);
		}
		test.executeTestSets(min_Version, max_Version, criteria, testSets);
		System.exit(0);
	}

}
