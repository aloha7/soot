package ccr.test;


import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;



public class TestingEffectiveManager {
	//An example input: Load 20090919 AllPolicies -1 new random R 20

	/**2009-09-19: use RA-H, RA-L, RA-R to construct adequate test suites
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		//Load 20090918 AllPolicies -1 new random H
		String instruction = args[0];
		String date = args[1];
		String criterion = args[2];
		int testSuiteSize = Integer.parseInt(args[3]);
		String oldOrNew = args[4];
		String randomOrCriterion = args[5];
		String H_L_R = args[6];	
		String size_ART = args[7];
		
		int start = 0;
		int end = 140;
		if (args.length == 10 && instruction.equals("Context_Intensity")) {
			start = Integer.parseInt(args[8]);
			end = Integer.parseInt(args[9]);
		}
		
		//load the test pool
		String testPoolFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
			+ date + "/TestPool.txt";
		TestSetManager.getTestPool(testPoolFile, true);
		Adequacy.loadTestCase(testPoolFile);
		
		//determine the test set files to load test sets
		String testSetFile = null;
		if (testSuiteSize < 0) {
			if(oldOrNew.equals("old")){//Conventional test suite construction algorithm
				testSetFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
					+ date
					+ "/"
					+ criterion
					+ "_CA_" + size_ART + ".txt";
				
			}else if(oldOrNew.equals("new")){
				if(H_L_R.equals("H")){ //Refined test suite construction algorithm with high context diversity
					testSetFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
						+ date
						+ "/"
						+ criterion
						+ "_RA-H_" + size_ART + ".txt";
				}else if(H_L_R.equals("L")){//Refined test suite construction algorithm with low context diversity
					testSetFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
						+ date
						+ "/"
						+ criterion
						+ "_RA-L_" + size_ART + ".txt";
				}else if(H_L_R.equals("R")){//Refined test suite construction algorithm with random context diversity
					testSetFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
						+ date
						+ "/"
						+ criterion
						+ "_RA-R_" + size_ART + ".txt";
				}
			}
		} else {
			if(oldOrNew.equals("old")){
				testSetFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
					+ date
					+ "/"
					+ criterion
					+ "_CA_"
					+ randomOrCriterion
					+ "_"
					+ testSuiteSize
					+ "_"+ size_ART + ".txt"; 					
			}else if(oldOrNew.equals("new")){
				if(H_L_R.equals("H")){
					testSetFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
						+ date
						+ "/"
						+ criterion
						+ "_RA-H_"
						+ randomOrCriterion
						+ "_"
						+ testSuiteSize
						+ "_" + size_ART + ".txt";
				}else if(H_L_R.equals("L")){
					testSetFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
						+ date
						+ "/"
						+ criterion
						+ "_RA-L_"
						+ randomOrCriterion
						+ "_"
						+ testSuiteSize
						+ "_" + size_ART + ".txt";
				}else if(H_L_R.equals("R")){
					testSetFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
						+ date
						+ "/"
						+ criterion
						+ "_RA-R_"
						+ randomOrCriterion
						+ "_"
						+ testSuiteSize
						+ "_" + size_ART + ".txt";
				}
			}
		}
		
		TestSet testSets[][] = new TestSet[1][];
		testSets[0] = Adequacy.getTestSets(testSetFile);
		
		String versionPackageName = "testversion";
		if(instruction.equals("Context_Intensity")){
			String saveFile = testSetFile.substring(0, testSetFile
					.indexOf("."))
					+ "_" + start + "_" + end + ".txt";

			TestDriver.test(versionPackageName, "TestCFG2", testSets,
					saveFile, start, end);
		}else if(instruction.equals("Limited")){
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
		}else if (instruction.equals("Load")) {
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

		}else if(instruction.equals("Load_large")){
			//2009-10-13: when there are many faults in the fault list, we
			//may not be able to merge so many execution history files into
			//one
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

			// 2. test the specified faults
			TestDriver
					.test_load(testSets, faultList, date, saveFile);
			
		}
		
	}

}
