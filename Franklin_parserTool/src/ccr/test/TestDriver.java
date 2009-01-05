package ccr.test;

import ccr.app.*;

import java.io.*;

public class TestDriver {
	
	public static final int TEST_POOL_SIZE = 10000;
	public static final int TEST_POOL_START_LABEL = -5000;
	public static final String APPLICATION_FOLDER = "src/ccr/app";
	public static final String APPLICATION_PACKAGE = "ccr.app";
	public static final String VERSION_PACKAGE_NAME = "version";
	
	public static final String WORK_FOLDER = System.getProperty("user.dir") + File.separator + "src" + File.separator
	+ "ccr" + File.separator + "app" + File.separator;
	
	public static Object run(Application app, String testcase) {
		
		return app.application(testcase);
	}
	
	public static Object run(String appClassName, String testcase) {
		
		Object result = null;
		try {
			Application app = (Application) Class.forName(appClassName).newInstance();
			result = run(app, testcase);
		} catch (Exception e) {
			System.out.println(e);
		}
		return result;
	}
	
	public static String[] getTrace(String appClassName, String testcase) {
		
		Trace.getInstance().initialize();
		run(APPLICATION_PACKAGE + "." + appClassName, testcase);
		return Trace.getInstance().getTrace();
	}
	
	public static double test(String appClassName, Oracle oracle, TestSet testSets[]) {
		
		int detected = 0;
		for (int i = 0; i < testSets.length; i++) {
			boolean equivalent = true;
			for (int j = 0; j < testSets[i].size(); j++) {
				String testcase = testSets[i].get(j);
				if (!run(appClassName, testcase).equals(oracle.getOutcome(testcase))) {
					equivalent = false;
					break;
				}
			}
			if (!equivalent) {
				detected = detected + 1;
			}
		}
		return (double) detected / (double) testSets.length;
	}
	
	public static double test(String appClassName, String oracleClassName, 
			TestSet testSets[]) {
		
		int detected = 0;
		for (int i = 0; i < testSets.length; i++) {
			boolean equivalent = true;
			for (int j = 0; j < testSets[i].size(); j++) {
				String testcase = testSets[i].get(j); //we need the pass/fail information for each test case.
				if (!run(appClassName, testcase).equals(run(oracleClassName, testcase))) {
					equivalent = false;
					break;
				}
			}
			if (!equivalent) {
				detected = detected + 1;
			}
		}
		double result = (double) detected / (double) testSets.length;
		return result;
	}
	
	public static void test(
			String versionPackageName, String oracleClassName, 
			TestSet testSets[], String reportFile) {
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(reportFile));
			String versionFolder = APPLICATION_FOLDER + "/" + versionPackageName;
			File versions = new File(versionFolder);
			for (int i = 0; i < versions.list().length; i++) {
				String appClassName = versions.list()[i];
				appClassName = APPLICATION_PACKAGE + "." + versionPackageName + "." + 
						appClassName.substring(0, appClassName.indexOf(".java"));
				String line = "Testing " + appClassName + " Fault detection rate\t" + 
						test(appClassName, APPLICATION_PACKAGE + "." + 
								oracleClassName, testSets);
				bw.write(line);
				bw.newLine();
			}
			bw.flush();
			bw.close();
		} catch (IOException e) {
			System.out.println(e);
		}
	}
	
	public static void test(
			String versionPackageName, String oracleClassName, 
			TestSet testSets[][], String reportFile) {
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(reportFile));
			String versionFolder = APPLICATION_FOLDER + "/" + versionPackageName;
			File versions = new File(versionFolder);
			
			for (int i = 0; i < versions.list().length; i++) {
				
				System.out.println("faulty versions number:" + versions.list().length );
				System.out.println("test sets size:" + testSets.length);
				

				String appClassName = versions.list()[i];
				appClassName = APPLICATION_PACKAGE + "." + versionPackageName + "." + 
						appClassName.substring(0, appClassName.indexOf(".java"));
				
			
				
				
			//	1/14/2008 Martin	
			//	String line = appClassName + "\tFault detection rate";
				for (int j = 0; j < testSets.length; j++) { //the length is 50
					String line = appClassName + "\tFault detection rate";
					System.out.println("");
					long startTime = System.currentTimeMillis();
					line = line + "\t" + test(
							appClassName, APPLICATION_PACKAGE + "." + 
							oracleClassName, testSets[j]);
				//	System.out.println(System.currentTimeMillis() - startTime);
					line = line +  "\t" + "time\t" + String.valueOf((System.currentTimeMillis() - startTime));
					
					System.out.println(line);
					bw.write(line);
				//	1/14/2008 Martin
					bw.flush();
					bw.newLine();
					
				}
				
			/*	//1/14/2008:Martin
				System.out.println(line);
				bw.write(line);
				bw.newLine();
				*/
				
			}
		//	bw.flush();
			bw.close();
		} catch (IOException e) {
			System.out.println(e);
		}
	}
	
	public static void getFailureRate(
			String versionPackageName, String oracleClassName, 
			TestSet testpool, String reportFile) {
		
		try {
			Oracle oracle = new Oracle(APPLICATION_PACKAGE + "." + oracleClassName, testpool);
			BufferedWriter bw = new BufferedWriter(new FileWriter(reportFile));
			String versionFolder = APPLICATION_FOLDER + "/" + versionPackageName;
			File versions = new File(versionFolder);
			long startTime;
			for (int i = 0; i < versions.list().length; i++) {
				startTime = System.currentTimeMillis();
				String appClassName = versions.list()[i];
				appClassName = APPLICATION_PACKAGE + "." + versionPackageName + "." + 
						appClassName.substring(0, appClassName.indexOf(".java"));
				int detected = 0;
				for (int j = 0; j < testpool.size(); j++) {
					
				//	if (!run(appClassName, testpool.get(j)).equals(
				//			run(APPLICATION_PACKAGE + "." + 
				//					oracleClassName, testpool.get(j)))) {
					if (!run(appClassName, testpool.get(j)).equals(
							oracle.getOutcome(testpool.get(j)))) {
						detected = detected + 1;
					}
				}
				String line = appClassName + "\tFailure rate\t" + 
						((double) detected / (double) testpool.size() + "\tTime:" + String.valueOf(System.currentTimeMillis()-startTime));
				System.out.println(line);
				bw.write(line);
				bw.newLine();
				bw.flush();
			}
			
			bw.close();
		} catch (IOException e) {
			System.out.println(e);
		}
	}
	
	public static void main(String argv[]) {
	
	/*	TestSet testpool = new TestSet();
		for (int i = 0; i < 10000; i++) { // A test set is a vector 
			testpool.add("" + i);
		}
	//	Oracle oracle = new Oracle("ccr.app.TestCFG2", testpool); //apply all test cases in test pools to get oracles 
	//	TestSet testSets[] = Adequacy.getTestSets("experiment/testsets.txt");
	//	System.out.println("Fault detection rate: " + test("TestCFG2", oracle, testSets));*/
	//	TestSet testSets[] = Adequacy.getTestSets("experiment/testsets.txt");
	//	System.out.println(
	//			"Fault detection rate: " + test("TestCFG2", "TestCFG2_ins", testSets));
	//	TestSet testSets[] = Adequacy.getTestSets("experiment/testsets.txt");
	//	test("src/ccr/app/version", "TestCFG2", testSets, "experiment/report.txt");
		
	/*	TestSet testSets[] = Adequacy.getTestSets("result/allUsesTestSet.txt");
		test("src/ccr/app/version", "TestCFG2", testSets, "experiment/allUsesReport.txt");
		
		testSets = Adequacy.getTestSets("result/allPoliciesTestSet.txt");
		test("src/ccr/app/version", "TestCFG2", testSets, "experiment/allPoliciesReport.txt");
		
		testSets = Adequacy.getTestSets("result/all1ResolvedDUTestSet.txt");
		test("src/ccr/app/version", "TestCFG2", testSets, 
				"experiment/all1ResolvedDUReport.txt");
		
		testSets = Adequacy.getTestSets("result/all2ResolvedDUTestSet.txt");
		test("src/ccr/app/version", "TestCFG2", testSets, 
				"experiment/all2ResolvedDUReport.txt");
		
		testSets = Adequacy.getTestSets("result/allFullResolvedDUTestSet.txt");
		test("src/ccr/app/version", "TestCFG2", testSets, 
				"experiment/allFullResolvedDUReport.txt");*/
		
	//	String[] traces = TestDriver.getTrace("TestCFG2_ins", "-1782");
	//	System.out.print("b");
	//	System.out.println(TestDriver.run("ccr.app.testversion.TestCFG2_01", "5"));
	//	System.out.println(TestDriver.run("ccr.app.testversion.TestCFG2", "100"));
	/*	
		TestSet testSets[][] = new TestSet[5][];
		testSets[0] = Adequacy.getTestSets("src/ccr/experiment/allUsesTestSets.txt");
		testSets[1] = Adequacy.getTestSets("src/ccr/experiment/allPoliciesTestSets.txt");
		testSets[2] = Adequacy.getTestSets("src/ccr/experiment/all1ResolvedDUTestSets.txt");
		testSets[3] = Adequacy.getTestSets("src/ccr/experiment/all2ResolvedDUTestSets.txt");
		testSets[4] = Adequacy.getTestSets("src/ccr/experiment/allFullResolvedDUTestSets.txt");
	//	long startTime = System.currentTimeMillis();
		String versionPackageName = "testversion";
		test(versionPackageName, "TestCFG2", testSets, 
				"src/ccr/experiment/test-report-" + versionPackageName + ".txt");
	//	System.out.println(System.currentTimeMillis() - startTime);
	*/
		
//		TestSet testSets[][] = new TestSet[1][];
//		testSets[0] = Adequacy.getTestSets("src/ccr/experiment/allUsesTestSets.txt");
	//	testSets[1] = Adequacy.getTestSets("src/ccr/experiment/allPoliciesTestSets_noOrdinary.txt");
	//	testSets[2] = Adequacy.getTestSets("src/ccr/experiment/all1ResolvedDUTestSets_noOrdinary.txt");
	//	testSets[3] = Adequacy.getTestSets("src/ccr/experiment/all2ResolvedDUTestSets_noOrdinary.txt");
	//	testSets[4] = Adequacy.getTestSets("src/ccr/experiment/allFullResolvedDUTestSets_noOrdinary.txt");
		
		
	//	long startTime = System.currentTimeMillis();
//		String versionPackageName = "testversion";
//		test(versionPackageName, "TestCFG2", testSets, 
//				"src/ccr/experiment/test-report-" + versionPackageName + ".txt");
	/*	TestSet testSets[][] = new TestSet[5][];
		testSets[0] = Adequacy.getTestSets("src/ccr/experiment/allUsesTestSets.txt");
		testSets[1] = Adequacy.getTestSets("src/ccr/experiment/allPoliciesTestSets.txt");
		testSets[2] = Adequacy.getTestSets("src/ccr/experiment/all1ResolvedDUTestSets.txt");
		testSets[3] = Adequacy.getTestSets("src/ccr/experiment/all2ResolvedDUTestSets.txt");
		testSets[4] = Adequacy.getTestSets("src/ccr/experiment/allFullResolvedDUTestSets.txt");
	//	test("version", "TestCFG2", testSets, "experiment/test-report.txt");
		long startTime = System.currentTimeMillis();
	//	TestSet testSets[][] = new TestSet[2][];
	//	testSets[0] = Adequacy.getTestSets("experiment/allUsesTestSet.txt");
	//	testSets[1] = Adequacy.getTestSets("experiment/allPoliciesTestSet.txt");
		String versionPackageName = "testversion";
		test(versionPackageName, "TestCFG2", testSets, 
				"src/ccr/experiment/test-report-" + versionPackageName + ".txt");
		System.out.println(System.currentTimeMillis() - startTime);
	*/	
		//1. Get the failure rate of 140 faulty versions, test pool contains 10000 test cases, each runs 2 mins(in server).
		long startTime = System.currentTimeMillis();
		getFailureRate("testversion", "TestCFG2", Adequacy.getTestPool(
				TestDriver.TEST_POOL_START_LABEL, TestDriver.TEST_POOL_SIZE),
				"src/ccr/experiment/failurerate.txt");		
		System.out.println(System.currentTimeMillis() - startTime);
		
		
	}

}
