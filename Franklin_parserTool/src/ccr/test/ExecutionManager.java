package ccr.test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import ccr.app.Application;
import ccr.app.ApplicationResult;
import ccr.stat.CFG;
import ccr.stat.Criterion;

public class ExecutionManager {

	/**
	 * args[0]: the number of adequate test sets(100);
	 * args[1]:instruction(Context_Intensity); args[2]: min CI of test cases in
	 * test sets(0.7); args[3]: maximum CI of test cases in test sets(0.9);
	 * args[4]: the directory to save output; args[5]: testing criteria;
	 * args[6]: fix the size of test sets; args[7]: old or new test case
	 * selection strategy; args[8]: random-enhanced or repetition-enhanced;
	 * args[9]:minimum faulty version; args[10]: maximum faulty version
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		System.out
				.println("USAGE: java ccr.test.ExecutionManager <testSetNum(100)> <Context_Intensity,Limited,Load> <min_CI(0.7)> "
						+ "<max_CI(0.9)> <directory(20090222)> <testing criteria(AllPolicies, All1ResolvedDU, All2ResolvedDU)>"
						+ "<TestSuiteSize(58)> <oldOrNew(old, new)> <randomOrCriteria(random, criteria)>[min_FaultyVersion][max_FaultyVersion]");
		for(int i = 0; i < args.length; i ++){
			System.out.println(args[i]);	
		}
		//2009-09-19
		if(args.length == 10){
			
		}
		
		if (args.length >= 9) {
			int testSetNum = Integer.parseInt(args[0]);
			String instruction = args[1];
			double min_CI = Double.parseDouble(args[2]);
			double max_CI = Double.parseDouble(args[3]);

			String date = args[4];
			String criterion = args[5];
			int testSuiteSize = Integer.parseInt(args[6]);
			String oldOrNew = args[7];
			String randomOrCriterion = args[8];
			int start = 0;
			int end = 140;
			if (args.length == 11 && instruction.equals("Context_Intensity")) {
				start = Integer.parseInt(args[9]);
				end = Integer.parseInt(args[10]);
			}

			CFG g = new CFG(System.getProperty("user.dir")
					+ "/src/ccr/app/TestCFG2.java");
			Criterion c;

			// 2009-2-21: revise the test case selection strategies: add a test
			// case
			// into a test set
			// if it can increase the cumulative coverage or it has higher CI
			// value
			// than existing one
			// while not decrease the coverage

			String testPoolFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
					+ date + "/TestPool.txt";
			TestSet testpool = TestSetManager.getTestPool(testPoolFile, true);

			String testSetFile = null;

			Adequacy.loadTestCase(testPoolFile);

			// 2009-02-22: fix the size of test suite to be 58, using
			// random-repetition to compensate the small test sets
			if (testSuiteSize < 0) {
				testSetFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
						+ date
						+ "/"
						+ criterion
						+ "TestSets_"
						+ oldOrNew
						+ ".txt";
			} else {
				testSetFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
						+ date
						+ "/"
						+ criterion
						+ "TestSets_"
						+ oldOrNew
						+ "_"
						+ randomOrCriterion + "_" + testSuiteSize + ".txt";
			}

			TestSet testSets[][] = new TestSet[1][];
			testSets[0] = Adequacy.getTestSets(testSetFile);

			String versionPackageName = "testversion";

			if (instruction.equals("Context_Intensity")) {
				String saveFile = testSetFile.substring(0, testSetFile
						.indexOf("."))
						+ "_" + start + "_" + end + ".txt";

				TestDriver.test(versionPackageName, "TestCFG2", testSets,
						saveFile, start, end);
			} else if (instruction.equals("Limited")) {
				// 2009-03-05: only interest in faults specified in a list
				String saveFile = testSetFile.substring(0, testSetFile
						.indexOf("."))
						+ "_limited_load.txt";

				String faultListFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
						+ date + "/FaultList.txt";

				// 1. load the fault list
				ArrayList faultList = new ArrayList();
				try {
					BufferedReader br = new BufferedReader(new FileReader(
							faultListFile));
					String line = null;
					while ((line = br.readLine()) != null) {
						faultList.add(line.trim());
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				// 2.run the test
				TestDriver.test(versionPackageName, "TestCFG2", testSets,
						saveFile, faultList);
			} else if (instruction.equals("Load")) {
				// 2009-03-05:load the testing result from execution history,
				// and only interest in faults specified in a list

				String saveFile = testSetFile.substring(0, testSetFile
						.indexOf("."))
						+ "_limited_load.txt";
				String faultListFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
						+ date + "/FaultList.txt";

				// 1. load the fault list
				ArrayList faultList = new ArrayList();
				try {
					BufferedReader br = new BufferedReader(new FileReader(
							faultListFile));
					String line = null;
					while ((line = br.readLine()) != null) {
						faultList.add(line.trim());
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				boolean containHeader = true;
				// 2. load the test result from history
				HashMap execHistory = new HashMap();
				String historyFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
						+ date + "/detailed.txt";
				try {
					BufferedReader br = new BufferedReader(new FileReader(
							historyFile));
					String line = null;

					if (containHeader)
						br.readLine();

					while ((line = br.readLine()) != null) {
						String[] strs = line.split("\t");
						String fault = strs[0].trim();

						if (!faultList.contains(fault)) // this can save memory
														// significantly
							continue;

						String testcase = strs[1].trim();
						String POrF = strs[2].trim();

						// save data into execHistory
						HashMap tc_pf;
						if (execHistory.containsKey("" + fault)) {
							tc_pf = (HashMap) execHistory.get("" + fault);
						} else {
							tc_pf = new HashMap();
						}

						// if(!tc_pf.containsKey(""+testcase))//this is not
						// necessary since it never happens
						tc_pf.put(testcase, POrF);
						execHistory.put(fault, tc_pf);
					}
				} catch (NumberFormatException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				// 3. test the specified faults
				TestDriver
						.test_load(testSets, faultList, execHistory, saveFile);

			}
		} else if (args.length == 3) { 
			// Test the random test set
			String instruction = args[0];
			int testSuiteSize = Integer.parseInt(args[1]);
			String date = args[2];
			
			String testPoolFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
				+ date + "/TestPool.txt";
			TestSet testpool = TestSetManager.getTestPool(testPoolFile, true);


			Adequacy.loadTestCase(testPoolFile);
			
			String testSetFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
				+ date
				+ "/RandomTestSets_"
				+  testSuiteSize
				+ ".txt";
			TestSet testSets[][] = new TestSet[1][];
			testSets[0] = Adequacy.getTestSets(testSetFile);
			
			
			
			String saveFile = testSetFile.substring(0, testSetFile
					.indexOf(".txt"))
					+ "_limited_load.txt";
			
			if(instruction.equals("Load")){
				
				String faultListFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
					+ date + "/FaultList.txt";

				// 1. load the fault list
				ArrayList faultList = new ArrayList();
				try {
					BufferedReader br = new BufferedReader(new FileReader(
							faultListFile));
					String line = null;
					while ((line = br.readLine()) != null) {
						faultList.add(line.trim());
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				// 2. load the test result from history
				boolean containHeader = true;
				HashMap execHistory = new HashMap();
				String historyFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
						+ date + "/detailed.txt";
				try {
					BufferedReader br = new BufferedReader(new FileReader(
							historyFile));
					String line = null;

					if (containHeader)
						br.readLine();
					String[] strs;
					String fault;
					String testcase;
					String POrF;
					
					while ((line = br.readLine()) != null) {
						strs = line.split("\t");
						fault = strs[0].trim();

						if (!faultList.contains(fault)) // this can save memory
														// significantly
							continue;

						testcase = strs[1].trim();
						POrF = strs[2].trim();

						// save data into execHistory
						HashMap tc_pf;
						if (execHistory.containsKey("" + fault)) {
							tc_pf = (HashMap) execHistory.get("" + fault);
						} else {
							tc_pf = new HashMap();
						}

						// if(!tc_pf.containsKey(""+testcase))//this is not
						// necessary since it never happens
						tc_pf.put(testcase, POrF);
						execHistory.put(fault, tc_pf);
					}
				} catch (NumberFormatException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				// 3. test the specified faults
				TestDriver.test_load(testSets, faultList, execHistory, saveFile);
				
			}else if (instruction.equals("Limited")) {
				// 2009-03-05: only interest in faults specified in a list
				saveFile = testSetFile.substring(0, testSetFile
						.indexOf("."))
						+ "_limited_load.txt";

				String faultListFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
						+ date + "/FaultList.txt";

				// 1. load the fault list
				ArrayList faultList = new ArrayList();
				try {
					BufferedReader br = new BufferedReader(new FileReader(
							faultListFile));
					String line = null;
					while ((line = br.readLine()) != null) {
						faultList.add(line.trim());
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				// 2.run the test
				String versionPackageName = "testversion";
				TestDriver.test(versionPackageName, "TestCFG2", testSets,
						saveFile, faultList);
			}
		}else if(args.length == 2){
			//specify the test set file directly, used to execute test sets whose CI are fixed in a small range
			
			
			String date = args[0];
			String testSetFile = args[1];

			String testPoolFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
					+ date + "/TestPool.txt";
			TestSet testpool = TestSetManager.getTestPool(testPoolFile, true);
			Adequacy.loadTestCase(testPoolFile);

			TestSet testSets[][] = new TestSet[1][];
			testSets[0] = Adequacy.getTestSets(testSetFile);

			String saveFile = testSetFile.substring(0, testSetFile
					.indexOf(".txt"))
					+ "_limited_load.txt";

			String faultListFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
					+ date + "/FaultList.txt";

			// 1. load the fault list
			ArrayList faultList = new ArrayList();
			try {
				BufferedReader br = new BufferedReader(new FileReader(
						faultListFile));
				String line = null;
				while ((line = br.readLine()) != null) {
					faultList.add(line.trim());
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			// 2. load the test result from history
			boolean containHeader = true;
			HashMap execHistory = new HashMap();
			String historyFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
					+ date + "/detailed.txt";
			try {
				BufferedReader br = new BufferedReader(new FileReader(
						historyFile));
				String line = null;

				if (containHeader)
					br.readLine();

				while ((line = br.readLine()) != null) {
					String[] strs = line.split("\t");
					String fault = strs[0].trim();

					if (!faultList.contains(fault)) // this can save memory
						// significantly
						continue;

					String testcase = strs[1].trim();
					String POrF = strs[2].trim();

					// save data into execHistory
					HashMap tc_pf;
					if (execHistory.containsKey("" + fault)) {
						tc_pf = (HashMap) execHistory.get("" + fault);
					} else {
						tc_pf = new HashMap();
					}

					// if(!tc_pf.containsKey(""+testcase))//this is not
					// necessary since it never happens
					tc_pf.put(testcase, POrF);
					execHistory.put(fault, tc_pf);
				}
			} catch (NumberFormatException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			// 3. test the specified faults
			TestDriver.test_load(testSets, faultList, execHistory, saveFile);
				
			
		}
	}
}
