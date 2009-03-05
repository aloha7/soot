package ccr.test;

import ccr.app.*;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class TestDriver {

	public static final int TEST_POOL_SIZE = 20000;
	public static final int TEST_POOL_START_LABEL = -10000;
	public static final String APPLICATION_FOLDER = "src/ccr/app";
	public static final String APPLICATION_PACKAGE = "ccr.app";
	public static final String VERSION_PACKAGE_NAME = "version";

	//2009-02-26: keep all execution records
	public static HashMap traceTable = new HashMap(); 
	
	//2009-02-26: keep all execution results of a test case with respect to a fault
	public static HashMap resultTable = new HashMap();
	
	public static final String WORK_FOLDER = System.getProperty("user.dir")
			+ File.separator + "src" + File.separator + "ccr" + File.separator
			+ "app" + File.separator;

	public static Object run(Application app, String testcase) {

		return app.application(testcase);
	}

	public static Object run(String appClassName, String testcase) {
		Object result = null;
		
		//2009-02-26:keep all the execution results of a test case with respect to a faulty version
//		if(!resultTable.containsKey(appClassName)){ // the appClassName has not been executed
//			try {
//				Application app = (Application) Class.forName(appClassName)
//						.newInstance();
//				result = run(app, testcase);
//				
//				
//			} catch (Exception e) {
//				System.out.println(e);
//			}
//			HashMap testcase_result = new HashMap();
//			testcase_result.put(testcase, result);
//			resultTable.put(appClassName, testcase_result);
//			
//		}else if(!((HashMap)resultTable.get(appClassName)).containsKey(testcase)){
//			// the test case has not been applied to the faulty version
//			try {
//				Application app = (Application) Class.forName(appClassName)
//						.newInstance();
//				result = run(app, testcase);
//			} catch (Exception e) {
//				System.out.println(e);
//			}
//			
//			HashMap testcase_result = (HashMap)resultTable.get(appClassName);
//			testcase_result.put(testcase, result);
//			resultTable.put(appClassName, testcase_result);
//		}else{ // if this test case has been executed on this faulty version, retrieve the result
//			HashMap testcase_result = (HashMap)resultTable.get(appClassName);
//			result = testcase_result.get(testcase);
//		}
		
		try {
			Application app = (Application) Class.forName(appClassName)
					.newInstance();
			result = run(app, testcase);
			
			
		} catch (Exception e) {
			System.out.println(e);
		}
		
		return result;
	}

	public static String[] getTrace(String appClassName, String testcase) {

		if(traceTable.containsKey(testcase)){
			return (String[])traceTable.get(testcase);
		}else{
			Trace.getInstance().initialize();
			run(APPLICATION_PACKAGE + "." + appClassName, testcase);
			String[] records =Trace.getInstance().getTrace();
			traceTable.put(testcase, records);
			return 	records;
		}
		
	}

	public static double test(String appClassName, Oracle oracle,
			TestSet testSets[]) {

		int detected = 0;
		for (int i = 0; i < testSets.length; i++) {
			boolean equivalent = true;
			for (int j = 0; j < testSets[i].size(); j++) {
				String testcase = testSets[i].get(j);
				if (!run(appClassName, testcase).equals(
						oracle.getOutcome(testcase))) {
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

	// 2009-1-6:for context intensity
	public static double test(String appClassName, String oracleClassName,
			TestSet testSets[]) {

		int detected = 0;
		for (int i = 0; i < testSets.length; i++) {
			boolean equivalent = true;
			for (int j = 0; j < testSets[i].size(); j++) {
				String testcase = testSets[i].get(j); // we need the pass/fail
														// information for each
														// test case.
				if (!run(appClassName, testcase).equals(
						run(oracleClassName, testcase))) {
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

	// 2009-1-6:for context intensity
	public static String test(String appClassName, String oracleClassName,
			String criterion, TestSet testSets[]) {
		StringBuilder sb = new StringBuilder();
		// 2009-2-15:reshape the output

		for (int i = 0; i < testSets.length; i++) {
			int validTestCase = 0;
			TestSet testSet = testSets[i];
			int size_TestSet = testSets[i].size();
			for (int j = 0; j < size_TestSet; j++) {
				String testcase = testSets[i].get(j);
				ApplicationResult result = (ApplicationResult) run(
						appClassName, testcase);
				if (!result.equals(run(oracleClassName, testcase)))
					validTestCase++;
			}
			String line = appClassName.substring(appClassName.indexOf("_")
					+ "_".length())
					+ "\t"
					+ testSet.index
					+ "\t"
					+ size_TestSet
					+ "\t"
					+ validTestCase
					+ "\t"
					+ (double) validTestCase
					/ (double) size_TestSet + "\t";

			if (validTestCase > 0)
				line += "1" + "\t";
			else
				line += "0" + "\t";
			line += "1" + "\n";
			System.out.print(line);
			sb.append(line);
		}

		return sb.toString();

	}

	public static void test(String versionPackageName, String oracleClassName,
			TestSet testSets[], String reportFile) {

		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(reportFile));
			String versionFolder = APPLICATION_FOLDER + "/"
					+ versionPackageName;
			File versions = new File(versionFolder);
			for (int i = 0; i < versions.list().length; i++) {
				String appClassName = versions.list()[i];
				appClassName = APPLICATION_PACKAGE
						+ "."
						+ versionPackageName
						+ "."
						+ appClassName.substring(0, appClassName
								.indexOf(".java"));
				String line = "Testing "
						+ appClassName
						+ " Fault detection rate\t"
						+ test(appClassName, APPLICATION_PACKAGE + "."
								+ oracleClassName, testSets);
				bw.write(line);
				bw.newLine();
			}
			bw.flush();
			bw.close();
		} catch (IOException e) {
			System.out.println(e);
		}
	}

	public static void test(String versionPackageName, String oracleClassName,
			TestSet testSets[][], String reportFile) {

		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(reportFile));
			String versionFolder = APPLICATION_FOLDER + "/"
					+ versionPackageName;
			File versions = new File(versionFolder);
			int versionCounter = 0;

			// 2009-2-15
			StringBuilder sb = new StringBuilder();
			sb.append("FaultyVersion" + "\t" + "TestSet" + "\t" + "#TestCase"
					+ "\t" + "#ValidTestCase" + "\t" + "%ValidTestCase" + "\t"
					+ "\t" + "#ValidTestSet" + "\t" + "#TestSet" + "\n");

			for (int i = 0; i <versions.listFiles().length; i++) {
				if (versions.listFiles()[i].isFile()) {
					versionCounter++;
					String appClassName = versions.list()[i];
					appClassName = APPLICATION_PACKAGE
							+ "."
							+ versionPackageName
							+ "."
							+ appClassName.substring(0, appClassName
									.indexOf(".java"));
					// 1/14/2008 Martin
					// for (int j = 0; j < testSets.length; j++) { //the length
					// is 50
					// String line = appClassName + "\tFault detection rate";
					// System.out.println("");
					// long startTime = System.currentTimeMillis();
					//						
					// //2009-1-6:context intensity
					// // line = line + "\t" + test(
					// // appClassName, APPLICATION_PACKAGE + "." +
					// // oracleClassName, testSets[j]);
					// String criteria = new File(reportFile).getPath();
					// criteria = criteria.substring(0, criteria.indexOf("."));
					// line = line + "\t" + test(
					// appClassName, APPLICATION_PACKAGE + "." +
					// oracleClassName, criteria, testSets[j]);
					// // System.out.println(System.currentTimeMillis() -
					// startTime);
					// line = line + "\t" + "time\t" +
					// String.valueOf((System.currentTimeMillis() - startTime));
					//						
					// System.out.println(line);
					// bw.write(line);
					// // 1/14/2008 Martin
					// bw.flush();
					// bw.newLine();
					//						
					// }

					/*
					 * //1/14/2008:Martin System.out.println(line);
					 * bw.write(line); bw.newLine();
					 */

					// 2009-2-15: re-generate the forms of outputs
					for (int j = 0; j < testSets.length; j++) { // the length is
																// 50
						long startTime = System.currentTimeMillis();
						// 2009-2-15:context intensity
						String criteria = new File(reportFile).getPath();
						criteria = criteria.substring(0, criteria.indexOf("."));
						sb.append(test(appClassName, APPLICATION_PACKAGE + "."
								+ oracleClassName, criteria, testSets[j]));
					}

				}
			}
			bw.write(sb.toString());
			
			bw.close();

			// System.out.println("faulty versions number:" +
			// versions.list().length );
			// System.out.println("test sets size:" + testSets.length);

		} catch (IOException e) {
			System.out.println(e);
		}
	}

	
	// 2009-1-6: for context-intensity
	//2009-2-22: for concurrent purpose
	public static void test(String versionPackageName, String oracleClassName,
			TestSet testSets[][], String reportFile, int startVersion, int endVersion ) {

		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(reportFile));
			String versionFolder = APPLICATION_FOLDER + "/"
					+ versionPackageName;
			File versions = new File(versionFolder);
			int versionCounter = 0;

			// 2009-2-15
			StringBuilder sb = new StringBuilder();
			sb.append("FaultyVersion" + "\t" + "TestSet" + "\t" + "#TestCase"
					+ "\t" + "#ValidTestCase" + "\t" + "%ValidTestCase" + "\t"
					+ "\t" + "#ValidTestSet" + "\t" + "#TestSet" + "\n");

			for (int i = 0; i <versions.listFiles().length; i++) {				
				if (versions.listFiles()[i].isFile()) {
					versionCounter++;
					String appClassName = versions.list()[i];
					appClassName = APPLICATION_PACKAGE
							+ "."
							+ versionPackageName
							+ "."
							+ appClassName.substring(0, appClassName
									.indexOf(".java"));
					int faultyVersion =Integer.parseInt(appClassName.substring(appClassName.indexOf("_")+"_".length())); 
					if(faultyVersion< startVersion || faultyVersion >= endVersion)
						continue;

					// 2009-2-15: re-generate the forms of outputs
					for (int j = 0; j < testSets.length; j++) { // the length is
																// 50
						long startTime = System.currentTimeMillis();
						// 2009-2-15:context intensity
						String criteria = new File(reportFile).getPath();
						criteria = criteria.substring(0, criteria.indexOf("."));
						sb.append(test(appClassName, APPLICATION_PACKAGE + "."
								+ oracleClassName, criteria, testSets[j]));
					}

				}
			}
			bw.write(sb.toString());
			
			bw.close();

			// System.out.println("faulty versions number:" +
			// versions.list().length );
			// System.out.println("test sets size:" + testSets.length);

		} catch (IOException e) {
			System.out.println(e);
		}
	}

	//2009-2-16: 
	public static void getFailureRate(String versionPackageName, String oracleClassName, TestSet testpool, String reportDir){
		try {
			Oracle oracle = new Oracle(APPLICATION_PACKAGE + "."
					+ oracleClassName, testpool);
			String versionFolder = APPLICATION_FOLDER + "/"
					+ versionPackageName;
			File versions = new File(versionFolder);
			
			StringBuilder sb = new StringBuilder();
			sb.append("FaultyVersion" + "\t" + "TestCase" +"\t"+ "PorF" + "CI"+"\n");
			
			HashMap failureRate = new HashMap(); //keep all valid test cases for a specified fault
			for(int i = 0 ; i < versions.list().length; i ++){				
				if((versions.listFiles()[i]).isFile()){
					String appClassName = versions.list()[i];
					appClassName = APPLICATION_PACKAGE
							+ "."
							+ versionPackageName
							+ "."
							+ appClassName.substring(0, appClassName
									.indexOf(".java"));
					//2009-2-17: re-form the appClassName to delete any redundant characters
					failureRate.put(appClassName.substring(appClassName.indexOf("_") + "_".length()), new ArrayList());	
				}
			}
			
			HashMap validTestCase = new HashMap(); //keep all exposed faults for a specified test case
			for(int i = 0; i < testpool.size(); i ++){
				validTestCase.put(testpool.get(i), new ArrayList());
			}
			
			long startTime = System.currentTimeMillis();
			for (int i = 0; i < versions.list().length; i++) {
				//for each faulty version
				if (versions.listFiles()[i].isFile()) {
					String appClassName = versions.list()[i];
					appClassName = APPLICATION_PACKAGE
							+ "."
							+ versionPackageName
							+ "."
							+ appClassName.substring(0, appClassName
									.indexOf(".java"));
					int detected = 0;
					System.out.println("Start version:" + appClassName.substring(appClassName.indexOf("_")+"_".length()));
					for (int j = 0; j < testpool.size(); j++) {
						long startTime1 = System.currentTimeMillis();
						ApplicationResult result = (ApplicationResult) run(
								appClassName, testpool.get(j));
						long last = System.currentTimeMillis() - startTime1;
						sb.append(appClassName.substring(appClassName.indexOf("_")+"_".length()) + "\t" + testpool.get(j) + "\t");
						if (!result.equals(oracle.getOutcome(testpool.get(j)))) {
							detected = detected + 1;
							String faultName = appClassName.substring(appClassName.indexOf("_")+"_".length());
							((ArrayList)failureRate.get(faultName)).add(testpool.get(j));
							((ArrayList)validTestCase.get(testpool.get(j))).add(appClassName.substring(appClassName.indexOf("_")+"_".length()));
							sb.append("F"+"\t"+((TestCase)Adequacy.testCases.get(testpool.get(j))).CI +"\n");
						} else
							sb.append("P"+"\t"+((TestCase)Adequacy.testCases.get(testpool.get(j))).CI +"\n");
					}
				}
			}
			
			//detailed result
			BufferedWriter bw = new BufferedWriter(new FileWriter(reportDir + "/detailed.txt"));
			bw.write(sb.toString());
			bw.close();
			
			//failure rate of faulty version
			bw = new BufferedWriter(new FileWriter(reportDir + "/failureRate.txt"));
			Iterator ite = failureRate.keySet().iterator();
			StringBuilder temp = new StringBuilder();
			temp.append("FaultyVersion" + "\t" + "FailureRate" + "\t" + "Avg.CI.ValidTestCase" + "\n");
			while(ite.hasNext()){ //for each faulty version
				String faultyVersion = (String)ite.next();
				ArrayList validTestCases = (ArrayList)failureRate.get(faultyVersion);
				//get faulty version and failure rate
				temp.append(faultyVersion + "\t" + (double)validTestCases.size()/(double)testpool.size() + "\t");
				
				//get Avg.CI of validTestCase
				if(validTestCases.size() > 0){
					//the failure rate of faults is not 0
					TestSet ts = new TestSet(); 
					for(int i = 0; i < validTestCases.size(); i ++){
						ts.add(validTestCases.get(i)+"");
					}
					temp.append(Adequacy.getAverageCI(ts)+"\n");
				}else{
					temp.append("0.0" + "\n");
				}
				
			}
			bw.write(temp.toString());
			bw.flush();
			bw.close();
			
			//valid test cases exposed faults
			bw = new BufferedWriter(new FileWriter(reportDir + "/validTestCases.txt"));
			ite = validTestCase.keySet().iterator();
			StringBuilder tmp = new StringBuilder();
			tmp.append("TestCase" + "\t" + "#ExposedFault" + "\t" + "CI" + "\t" + "Avg.FailureRate" + "\n");
			while(ite.hasNext()){ //for each faulty version
				String testcase = (String)ite.next();
				ArrayList exposedFault = (ArrayList)validTestCase.get(testcase);
				//get test case and exposed faults
				tmp.append(testcase + "\t" + exposedFault.size() + "\t");
				
				//get CI of test case
				tmp.append(((TestCase)Adequacy.testCases.get(testcase)).CI + "\t");
				
				//get Avg.failure rate of exposed faults
				double sum_failureRate = 0.0;
				if(exposedFault.size() > 0){
					for(int i = 0; i < exposedFault.size(); i ++){
						String fault = (String)exposedFault.get(i);
						sum_failureRate += (double)((ArrayList)failureRate.get(fault)).size()/(double)testpool.size();
					}
					tmp.append(sum_failureRate/(double)exposedFault.size()+"\n");	
				}else{
					tmp.append("0.0"+"\n");
				}
			}
			bw.write(tmp.toString());
			bw.flush();
			bw.close();
		} catch (IOException e) {
			System.out.println(e);
		}

	}
	
	// 2009-1-5: for context-intensity experiment
//	public static void getFailureRate(String versionPackageName,
//			String oracleClassName, TestSet testpool, String reportDir) {
//
//		try {
//			Oracle oracle = new Oracle(APPLICATION_PACKAGE + "."
//					+ oracleClassName, testpool);
//			String versionFolder = APPLICATION_FOLDER + "/"
//					+ versionPackageName;
//			File versions = new File(versionFolder);
//			long startTime = System.currentTimeMillis();
//			for (int i = 0; i < versions.list().length; i++) {
//				if (versions.listFiles()[i].isFile()) {
//
//					String appClassName = versions.list()[i];
//					appClassName = APPLICATION_PACKAGE
//							+ "."
//							+ versionPackageName
//							+ "."
//							+ appClassName.substring(0, appClassName
//									.indexOf(".java"));
//					int detected = 0;
//					String line = "TestCase\t" + "Changes\t" + "Length\t"
//							+ "Time\t" + "Pass/Fail\n";
//					for (int j = 0; j < testpool.size(); j++) {
//						long startTime1 = System.currentTimeMillis();
//						ApplicationResult result = (ApplicationResult) run(
//								appClassName, testpool.get(j));
//						long last = System.currentTimeMillis() - startTime1;
//						line += "" + testpool.get(j) + "\t" + result.moved
//								+ "\t" + result.counter + "\t" + last + "\t";
//						if (!result.equals(oracle.getOutcome(testpool.get(j)))) {
//							detected = detected + 1;
//							line += "F\n";
//						} else
//							line += "P\n";
//					}
//					line += appClassName
//							+ "\tFailure rate\t"
//							+ ((double) detected / (double) testpool.size()
//									+ "\tTime:" + String.valueOf(System
//									.currentTimeMillis()
//									- startTime));
//					System.out.println(line);
//					BufferedWriter bw = new BufferedWriter(new FileWriter(
//							reportDir + File.separator + appClassName + ".txt"));
//					bw.write(line);
//					bw.newLine();
//					bw.flush();
//					bw.close();
//				}
//			}
//
//		} catch (IOException e) {
//			System.out.println(e);
//		}
//	}

	// 2009-1-5: for context-intensity experiment
	/*
	 * public static void getFailureRate( String versionPackageName, String
	 * oracleClassName, TestSet testpool, String reportFile) {
	 * 
	 * try { Oracle oracle = new Oracle(APPLICATION_PACKAGE + "." +
	 * oracleClassName, testpool); BufferedWriter bw = new BufferedWriter(new
	 * FileWriter(reportFile)); String versionFolder = APPLICATION_FOLDER + "/" +
	 * versionPackageName; File versions = new File(versionFolder); long
	 * startTime; for (int i = 0; i < versions.list().length; i++) { startTime =
	 * System.currentTimeMillis(); String appClassName = versions.list()[i];
	 * appClassName = APPLICATION_PACKAGE + "." + versionPackageName + "." +
	 * appClassName.substring(0, appClassName.indexOf(".java")); int detected =
	 * 0; for (int j = 0; j < testpool.size(); j++) {
	 *  // if (!run(appClassName, testpool.get(j)).equals( //
	 * run(APPLICATION_PACKAGE + "." + // oracleClassName, testpool.get(j)))) {
	 * if (!run(appClassName, testpool.get(j)).equals(
	 * oracle.getOutcome(testpool.get(j)))) { detected = detected + 1; } }
	 * String line = appClassName + "\tFailure rate\t" + ((double) detected /
	 * (double) testpool.size() + "\tTime:" +
	 * String.valueOf(System.currentTimeMillis()-startTime));
	 * System.out.println(line); bw.write(line); bw.newLine(); bw.flush(); }
	 * 
	 * bw.close(); } catch (IOException e) { System.out.println(e); } }
	 */
	public static void main(String argv[]) {

		/*
		 * TestSet testpool = new TestSet(); for (int i = 0; i < 10000; i++) { //
		 * A test set is a vector testpool.add("" + i); } // Oracle oracle = new
		 * Oracle("ccr.app.TestCFG2", testpool); //apply all test cases in test
		 * pools to get oracles // TestSet testSets[] =
		 * Adequacy.getTestSets("experiment/testsets.txt"); //
		 * System.out.println("Fault detection rate: " + test("TestCFG2",
		 * oracle, testSets));
		 */
		// TestSet testSets[] = Adequacy.getTestSets("experiment/testsets.txt");
		// System.out.println(
		// "Fault detection rate: " + test("TestCFG2", "TestCFG2_ins",
		// testSets));
		// TestSet testSets[] = Adequacy.getTestSets("experiment/testsets.txt");
		// test("src/ccr/app/version", "TestCFG2", testSets,
		// "experiment/report.txt");
		/*
		 * TestSet testSets[] =
		 * Adequacy.getTestSets("result/allUsesTestSet.txt");
		 * test("src/ccr/app/version", "TestCFG2", testSets,
		 * "experiment/allUsesReport.txt");
		 * 
		 * testSets = Adequacy.getTestSets("result/allPoliciesTestSet.txt");
		 * test("src/ccr/app/version", "TestCFG2", testSets,
		 * "experiment/allPoliciesReport.txt");
		 * 
		 * testSets = Adequacy.getTestSets("result/all1ResolvedDUTestSet.txt");
		 * test("src/ccr/app/version", "TestCFG2", testSets,
		 * "experiment/all1ResolvedDUReport.txt");
		 * 
		 * testSets = Adequacy.getTestSets("result/all2ResolvedDUTestSet.txt");
		 * test("src/ccr/app/version", "TestCFG2", testSets,
		 * "experiment/all2ResolvedDUReport.txt");
		 * 
		 * testSets =
		 * Adequacy.getTestSets("result/allFullResolvedDUTestSet.txt");
		 * test("src/ccr/app/version", "TestCFG2", testSets,
		 * "experiment/allFullResolvedDUReport.txt");
		 */

		// String[] traces = TestDriver.getTrace("TestCFG2_ins", "-1782");
		// System.out.print("b");
		// System.out.println(TestDriver.run("ccr.app.testversion.TestCFG2_01",
		// "5"));
		// System.out.println(TestDriver.run("ccr.app.testversion.TestCFG2",
		// "100"));
		/*
		 * TestSet testSets[][] = new TestSet[5][]; testSets[0] =
		 * Adequacy.getTestSets("src/ccr/experiment/allUsesTestSets.txt");
		 * testSets[1] =
		 * Adequacy.getTestSets("src/ccr/experiment/allPoliciesTestSets.txt");
		 * testSets[2] =
		 * Adequacy.getTestSets("src/ccr/experiment/all1ResolvedDUTestSets.txt");
		 * testSets[3] =
		 * Adequacy.getTestSets("src/ccr/experiment/all2ResolvedDUTestSets.txt");
		 * testSets[4] =
		 * Adequacy.getTestSets("src/ccr/experiment/allFullResolvedDUTestSets.txt"); //
		 * long startTime = System.currentTimeMillis(); String
		 * versionPackageName = "testversion"; test(versionPackageName,
		 * "TestCFG2", testSets, "src/ccr/experiment/test-report-" +
		 * versionPackageName + ".txt"); //
		 * System.out.println(System.currentTimeMillis() - startTime);
		 */

		// TestSet testSets[][] = new TestSet[1][];
		// testSets[0] =
		// Adequacy.getTestSets("src/ccr/experiment/allUsesTestSets.txt");
		// testSets[1] =
		// Adequacy.getTestSets("src/ccr/experiment/allPoliciesTestSets_noOrdinary.txt");
		// testSets[2] =
		// Adequacy.getTestSets("src/ccr/experiment/all1ResolvedDUTestSets_noOrdinary.txt");
		// testSets[3] =
		// Adequacy.getTestSets("src/ccr/experiment/all2ResolvedDUTestSets_noOrdinary.txt");
		// testSets[4] =
		// Adequacy.getTestSets("src/ccr/experiment/allFullResolvedDUTestSets_noOrdinary.txt");

		// long startTime = System.currentTimeMillis();
		// String versionPackageName = "testversion";
		// test(versionPackageName, "TestCFG2", testSets,
		// "src/ccr/experiment/test-report-" + versionPackageName + ".txt");
		/*
		 * TestSet testSets[][] = new TestSet[5][]; testSets[0] =
		 * Adequacy.getTestSets("src/ccr/experiment/allUsesTestSets.txt");
		 * testSets[1] =
		 * Adequacy.getTestSets("src/ccr/experiment/allPoliciesTestSets.txt");
		 * testSets[2] =
		 * Adequacy.getTestSets("src/ccr/experiment/all1ResolvedDUTestSets.txt");
		 * testSets[3] =
		 * Adequacy.getTestSets("src/ccr/experiment/all2ResolvedDUTestSets.txt");
		 * testSets[4] =
		 * Adequacy.getTestSets("src/ccr/experiment/allFullResolvedDUTestSets.txt"); //
		 * test("version", "TestCFG2", testSets, "experiment/test-report.txt");
		 * long startTime = System.currentTimeMillis(); // TestSet testSets[][] =
		 * new TestSet[2][]; // testSets[0] =
		 * Adequacy.getTestSets("experiment/allUsesTestSet.txt"); // testSets[1] =
		 * Adequacy.getTestSets("experiment/allPoliciesTestSet.txt"); String
		 * versionPackageName = "testversion"; test(versionPackageName,
		 * "TestCFG2", testSets, "src/ccr/experiment/test-report-" +
		 * versionPackageName + ".txt");
		 * System.out.println(System.currentTimeMillis() - startTime);
		 */
		// 1. Get the failure rate of 140 faulty versions, test pool contains
		// 10000 test cases, each runs 2 mins(in server).
		long startTime = System.currentTimeMillis();
		// getFailureRate("testversion", "TestCFG2", Adequacy.getTestPool(
		//				TestDriver.TEST_POOL_START_LABEL, TestDriver.TEST_POOL_SIZE),
		//				"src/ccr/experiment/failurerate.txt");			
//		getFailureRate("testversion", "TestCFG2", Adequacy.getTestPool(
//				TestDriver.TEST_POOL_START_LABEL, TestDriver.TEST_POOL_SIZE),
//				"src/ccr/experiment/RQ3");
//		System.out.println(System.currentTimeMillis() - startTime);
		
		//2009-02-16: we re-generate all test pools such that it ensures  CI of all test cases
		// are evenly distributed from 0.0 to 1.0
//		String testcaseFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/20090217/TestPool_20090216.txt";
//		getFailureRate("testversion", "TestCFG2", Adequacy.getTestPool(testcaseFile, true),
//				"src/ccr/experiment/Context-Intensity_backup/TestHarness/");
		
		//2009-02-21:
		String testcaseFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/20090221/TestPool.txt";
		getFailureRate("testversion", "TestCFG2", Adequacy.getTestPool(testcaseFile, true),
				"src/ccr/experiment/Context-Intensity_backup/TestHarness/20090221/");
	}

}
