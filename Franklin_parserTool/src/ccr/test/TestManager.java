package ccr.test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Scanner;

import ccr.app.ApplicationResult;

public class TestManager {
	
	public static final int TEST_POOL_SIZE = 20000;
	public static final int TEST_POOL_START_LABEL = -10000;
	public static final String APPLICATION_FOLDER = "src/ccr/app";
	public static final String APPLICATION_PACKAGE = "ccr.app";
	public static final String VERSION_PACKAGE_NAME = "version";
	
	//2009-2-16: 
	public static void getFailureRate(String versionPackageName, String oracleClassName, TestSet testpool, 
			String reportDir, int minFaultyVersion, int maxFaultyVersion){
		try {
			Oracle oracle = new Oracle(APPLICATION_PACKAGE + "."
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
					appClassName = APPLICATION_PACKAGE
							+ "."
							+ versionPackageName
							+ "."
							+ appClassName.substring(0, appClassName
									.indexOf(".java"));

					int version = Integer.parseInt(appClassName.substring(appClassName.indexOf("_") + "_".length()));
					if(version < minFaultyVersion || version >= maxFaultyVersion)
						continue;
						
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
					int version = Integer.parseInt(appClassName.substring(appClassName.indexOf("_") + "_".length()));
					if(version < minFaultyVersion || version >= maxFaultyVersion)
						continue;
						
					int detected = 0;
					System.out.println("Start version:" + appClassName.substring(appClassName.indexOf("_")+"_".length()));
					for (int j = 0; j < testpool.size(); j++) {
						long startTime1 = System.currentTimeMillis();
						ApplicationResult result = (ApplicationResult) TestDriver.run(
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
			BufferedWriter bw = new BufferedWriter(new FileWriter(reportDir + "/detailed_"+minFaultyVersion+"_"+maxFaultyVersion+".txt"));
			bw.write(sb.toString());
			bw.close();
			
			//failure rate of faulty version
			bw = new BufferedWriter(new FileWriter(reportDir + "/failureRate_"+minFaultyVersion+"_"+ maxFaultyVersion+".txt"));
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
			bw = new BufferedWriter(new FileWriter(reportDir + "/validTestCases_"+minFaultyVersion+"_" + maxFaultyVersion+".txt"));
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
	
	
	
	public static void getFailureRateFromFile(String executionFile, boolean containHeader, TestSet testpool, String saveFile){
		try {
//			sb.append("FaultyVersion" + "\t" + "TestCase" +"\t"+ "PorF" + "\t"+"CI"+"\n");
			HashMap fault_validTestCases = new HashMap();
			BufferedReader br = new BufferedReader(new FileReader(executionFile));
			String line = null;
			if(containHeader)
				br.readLine();
			
			while((line= br.readLine())!= null){
				String[] strs = line.split("\t");
				String fault = strs[0];
				String testcase = strs[1];
				String PorF = strs[2];
				if(!fault_validTestCases.containsKey(fault))
					fault_validTestCases.put(fault, new ArrayList());
				
				if(PorF.equals("F")){
					ArrayList validTestCases;
					validTestCases = (ArrayList)fault_validTestCases.get(fault);
					validTestCases.add(testcase);
					fault_validTestCases.put(fault, validTestCases);
				}else{
					
				}
			}
			
			StringBuilder sb = new StringBuilder();
			sb.append("FaultyVersion" + "\t" + "FailureRate" + "\t" + "Avg.CIOfValidTestCases"+ "\t\n");
			Iterator ite = fault_validTestCases.keySet().iterator();
			while(ite.hasNext()){
				String fault = (String)ite.next();
				ArrayList validTestCases = (ArrayList)fault_validTestCases.get(fault);
				double failureRate = (double)validTestCases.size()/(double)testpool.size();
				
				TestSet ts = new TestSet(); 
				for(int i = 0; i < validTestCases.size(); i ++){
					ts.add(validTestCases.get(i)+"");
				}
				sb.append(fault + "\t" + failureRate + "\t");
				sb.append(Adequacy.getAverageCI(ts)+"\n");
			}
			
			Logger.getInstance().setPath(saveFile, false);
			Logger.getInstance().write(sb.toString());
			Logger.getInstance().close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	//2009-02-23: get failure rate 
	public static void main(String[] args) {
		
		int startVersion = 0;
		int endVersion = 140;
		String date = "debug";
		if(args.length==3){
			startVersion = Integer.parseInt(args[0]);
			endVersion = Integer.parseInt(args[1]);
			date = args[2];
		}
		
		//1.generate test pool
		String testcaseFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"+date+"/TestPool.txt";
		TestSet testpool = Adequacy.getTestPool(testcaseFile, true);

		//2.get failure rate of each faulty versions
		getFailureRate("testversion", "TestCFG2", testpool,
		"src/ccr/experiment/Context-Intensity_backup/TestHarness/"+date+"/", startVersion, endVersion);
		
//		boolean containHeader = true;
//		String executionFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"+date+"/detailed.txt";
//		String saveFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"+date+"/FaulureRateDetail.txt";
//		getFailureRateFromFile(executionFile, containHeader, testpool, saveFile);
	}

}
