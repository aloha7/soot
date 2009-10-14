package ccr.test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import ccr.app.Application;
import ccr.app.ApplicationResult;

public class TestDriver_concurrent {
	public static final int TEST_POOL_SIZE = 20000;
	public static final int TEST_POOL_START_LABEL = -10000;
	public static final String APPLICATION_FOLDER = "src/ccr/app";
	public static final String APPLICATION_PACKAGE = "ccr.app";
	public static final String VERSION_PACKAGE_NAME = "version";

	public static Object run(Application app, String testcase) {

		return app.application(testcase);
	}

	public static Object run(String appClassName, String testcase) {

		Object result = null;
		try {
			Application app = (Application) Class.forName(appClassName)
					.newInstance();
			result = run(app, testcase);
		} catch (Exception e) {
			System.out.println(e);
		}
		return result;
	}
	
	
	public static void getFailureRate(String versionPackageName, String oracleClassName, TestSet testpool, String reportDir,
			int startVersion, int endVersion){
		try {
			Oracle oracle = Oracle.getInstance(APPLICATION_PACKAGE + "."
					+ oracleClassName, testpool);
			String versionFolder = APPLICATION_FOLDER + "/"
					+ versionPackageName;
			File versions = new File(versionFolder);
			
			StringBuilder sb = new StringBuilder();
			sb.append("FaultyVersion" + "\t" + "TestCase" +"\t"+ "PorF" + "\t"+"CI"+"\n");
			
			HashMap failureRate = new HashMap(); //keep all valid test cases for a specified fault
			for(int i = 0 ; i < versions.list().length; i ++){					
				if((versions.listFiles()[i]).isFile()){
					String appClassName = versions.list()[i];
					appClassName = appClassName.substring(appClassName.indexOf("_") + "_".length(), appClassName.indexOf(".java"));

					if(Integer.parseInt(appClassName) >= startVersion && Integer.parseInt(appClassName) < endVersion)
						failureRate.put(appClassName, new ArrayList());
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
					StringBuilder tmp = new StringBuilder();
					tmp.append(APPLICATION_PACKAGE);
					tmp.append(".");
					tmp.append(versionPackageName);
					tmp.append(".");
					tmp.append(appClassName.substring(0, appClassName.indexOf(".java")));
					appClassName = tmp.toString();
					
					//2009-02-22: add this statement to support concurrent execution
					if(!failureRate.containsKey(appClassName.substring(appClassName.indexOf("_") + "_".length())))
						continue;
					
					int detected = 0;
					System.out.println("Start version:" + appClassName.substring(appClassName.indexOf("_")+"_".length()));
					for (int j = 0; j < testpool.size(); j++) {
						long startTime1 = System.currentTimeMillis();
						ApplicationResult result = (ApplicationResult) run(
								appClassName, testpool.get(j));
						long last = System.currentTimeMillis() - startTime1;
						
						//fault version
						sb.append(appClassName.substring(appClassName.indexOf("_")+"_".length()));
						sb.append("\t");
						//test case
						sb.append(testpool.get(j));
						sb.append("\t");
								
						if (!result.equals(oracle.getOutcome(testpool.get(j)))) {
							detected = detected + 1;
							String faultName = appClassName.substring(appClassName.indexOf("_")+"_".length());
							((ArrayList)failureRate.get(faultName)).add(testpool.get(j));
							((ArrayList)validTestCase.get(testpool.get(j))).add(appClassName.substring(appClassName.indexOf("_")+"_".length()));
							// Pass or Fail
							sb.append("F");
							sb.append("\t");
							
							// CI
							sb.append(((TestCase)Adequacy.testCases.get(testpool.get(j))).CI);
							sb.append("\n");
						} else
							//Pass or Fail
							sb.append("P");
							sb.append("\t");
							
							//CI
							sb.append(((TestCase)Adequacy.testCases.get(testpool.get(j))).CI);
							sb.append("\n");
					}
				}
			}
			
			//detailed result
			BufferedWriter bw = new BufferedWriter(new FileWriter(reportDir + "/detailed_"+startVersion+ "_"+ endVersion+".txt"));
			bw.write(sb.toString());
			bw.close();
			
			//failure rate of faulty version
			bw = new BufferedWriter(new FileWriter(reportDir + "/failureRate_"+startVersion+ "_"+ endVersion + ".txt"));
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
			bw = new BufferedWriter(new FileWriter(reportDir + "/validTestCases_"+startVersion+ "_"+ endVersion + ".txt"));
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
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		//2009-02-22: for concurrent purpose
		String date = "20090223";
		boolean containHeader = true;
		
		String testcaseFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"+date+"/TestPool.txt";
		TestSet testpool = Adequacy.getTestPool(testcaseFile, containHeader);
		int startVersion = 0;
		int endVersion = 140;
		if(args.length == 2){
			startVersion = Integer.parseInt(args[0]);
			endVersion = Integer.parseInt(args[1]);
		}else if(args.length ==3){
			startVersion = Integer.parseInt(args[0]);
			endVersion = Integer.parseInt(args[1]);
			date = args[2];
		}
		
		getFailureRate("testversion", "TestCFG2", testpool,
				"src/ccr/experiment/Context-Intensity_backup/TestHarness/"+date, startVersion, endVersion);
	}

}
