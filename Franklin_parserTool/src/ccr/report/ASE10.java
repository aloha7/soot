package ccr.report;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import ccr.help.TestSetStatistics;
import ccr.stat.CFG;
import ccr.stat.Criterion;
import ccr.test.Adequacy;
import ccr.test.Oracle;
import ccr.test.ResultAnalyzer;
import ccr.test.TestDriver;
import ccr.test.TestSet;
import ccr.test.TestSetManager;

public class ASE10 {
	
	public static void main(String[] args) {
		
		String instruction = args[0]; 
		if(instruction.equals("saveOracles")){
			//2010-03-01: save oracles into files
			//Typical input: saveOracles 20100301
			
			String date_testPool = args[1];
			String testcaseFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/" +
					""+date_testPool+"/TestPool.txt";
			TestSet testpool = Adequacy.getTestPool(testcaseFile, true);
			String _oracleFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/" + 
				   date_testPool + "/Oracle.txt";
			Oracle.getInstance("ccr.app.TestCFG2", testpool, _oracleFile);
			
		}else if(instruction.equals("getFailureRate")){
			//2010-03-01: get failure rate of specified faults. 
			//Usually, this method is invoked by a script. 
			//Typical inputs: getFailureRate 20091005 0 1
			
			String date = args[1];
			int startVersion = Integer.parseInt(args[2]);
			int endVersion = Integer.parseInt(args[3]);
			
			String versionPackageName = "testversion";
			String oracleClassName = "TestCFG2";
			
			String testcaseFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"+date+"/TestPool.txt";
			TestSet testpool = Adequacy.getTestPool(testcaseFile, true);
			String reportDir = "src/ccr/experiment/Context-Intensity_backup/TestHarness/" + date + "/";
			
			TestDriver.getFailureRate_efficient(versionPackageName, oracleClassName, 
					testpool, reportDir, startVersion, endVersion);
			
		}else if(instruction.equals("getAdequateTestSet")){
			//2010-03-01:get adequate test sets
			//Typical input: getAdequateTestSet 100 0.7 0.9 20090919 AllPolicies -1 new random L 20
			
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
			TestSet testpool = TestSetManager.getTestPool(testPoolFile, true);
			Adequacy.loadTestCase(testPoolFile);
			
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

			String appClassName = "TestCFG2_ins";
			int maxTrials = 2000;
			
			testSets[0] = TestSetManager.getTestSets(appClassName, c, testpool,
					maxTrials, testSetNum, min_CI, max_CI, oldOrNew,
					randomOrCriterion, testSuiteSize, saveFile);

			saveFile = saveFile.substring(0, saveFile.indexOf(".txt"))
					+ "_CI.txt";
			TestSetManager.attachTSWithCI(testSets[0], saveFile);
			
		}else if(instruction.equals("saveTestingResults_TestSet")){
			//2010-03-01: save the testing effectiveness of test sets into "x_limit_load.txt"
			//Typical input: saveTestingResults_TestSet 20100301 AllPolicies -1 new random H 20 20100301 20100301
			
			String date_testSets = "20100301";
			String criterion = "AllPolicies";
			int testSuiteSize = -1;
			String oldOrNew = "old";
			String randomOrCriterion = "random";
			String H_L_R = "H";
			int size_ART = 64;
			String date_testPool = "20100301";
			String date_faultList = "20100301";
			if(args.length > 8){
				date_testSets = args[1];
				criterion = args[2];
				testSuiteSize = Integer.parseInt(args[3]);
				oldOrNew = args[4];
				randomOrCriterion = args[5];
				H_L_R = args[6];
				size_ART = Integer.parseInt(args[7]);
				date_testPool = args[8];
				date_faultList = args[9];
			}
			
			String testSetFile = TestSetStatistics.getTestSetFile(date_testSets, criterion, testSuiteSize, 
					oldOrNew, randomOrCriterion, H_L_R, size_ART);
			TestSet testSets[][] = new TestSet[1][];
			testSets[0] = Adequacy.getTestSets(testSetFile);
			
			String date_execHisotry = args[8];
			
			String saveFile = testSetFile.substring(0, testSetFile
					.indexOf("."))
					+ "_limited_load.txt";
			String faultListFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
					+ date_testPool+ "/FaultList.txt";

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
					.test_load(testSets, faultList, date_execHisotry, saveFile);
			
		}else if(instruction.equals("saveTestingEffectiveness_FixARTSize")){
			//2010-03-01: save the min/max/mean fault detection rate of adequate test set
			//with respect to a ART size
			//Typical input: saveTestingEffectiveness_FixARTSize 20100301 64
			
			String date = args[1];
			String size_ART = args[2];
			ResultAnalyzer.saveTestingPerfomanceOfAdequateTestSet(
					date, size_ART);
		}

	}

}
