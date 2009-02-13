package context.test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import context.test.util.Constant;
import context.test.util.Logger;

public class TestResultManager {

	/**
	 * 2009/2/12:list the faulty version, failure rate, fault-exposing test
	 * cases, and total valid test cases
	 * 
	 * @param savePath
	 */
	public void getFailureRate(String savePath) {
		StringBuilder sb = new StringBuilder();
		sb.append("FaultyVersion" + "\t" + "TotalTestCases" + "\t"
				+ "ValidTestCases" + "\t" + "FailureRate" + "\n");
		File[] faultyVersions = new File(Constant.baseFolder
				+ "test/output/FailureRate/").listFiles();
		for (File faultyVersion : faultyVersions) {

			// list all fault-detected test cases(file size larger than 0)
			File[] testCases = faultyVersion.listFiles();
			ArrayList validTestCases = new ArrayList();
			for (File testCase : testCases) {
				if (testCase.length() > 0) {
					String name = testCase.getName();
					validTestCases.add(name.substring(0, name.indexOf(".")));
				}
			}

			sb.append(faultyVersion.getName() + "\t" + testCases.length + "\t"
					+ validTestCases.size() + "\t"
					+ (double) validTestCases.size()
					/ (double) testCases.length + "\t" + "\n");
		}

		Logger.getInstance().setPath(savePath, false);
		Logger.getInstance().write(sb.toString());
		Logger.getInstance().close();
	}

	/**2009/2/13:Just record some information rather than doing any calculation
	 * 
	 * @param criteria
	 * @param savePath
	 */
	public void getFaultDetectionRate(String criteria, String savePath) {
		StringBuffer sb = new StringBuffer();

		sb.append("FaultyVersion" + "\t" + "TestSetIndex" + "\t" + "#TestCase"
				+ "\t" + "#ValidTestCase" + "\t"+"#TestSet" + "\t" +"#ValidTestSet"+ "\t" +"Avg.%ValidTestCase" +"\n");

		
		
		File[] faultyVersions = new File(Constant.baseFolder + "test/output/"
				+ criteria).listFiles();

		ArrayList validTestCases = new ArrayList();

		for (File faultyVersion : faultyVersions) {
			File[] testSets = faultyVersion.listFiles();

			HashMap testSetHash = new HashMap();
			int totalTestCase = 0;
			int validTestCase = 0;

			for (File testSet : testSets) { // for each test set
				
				int testCase_TestSet = 0;
				int validTestCase_TestSet = 0;
				
				boolean validTestSet = false;
				validTestCases.clear();
				File[] testCases = testSet.listFiles();
				for (File testCase : testCases) {
					testCase_TestSet++;
					if (testCase.length() > 0) {
						String name = testCase.getName();
						validTestCases
								.add(name.substring(0, name.indexOf(".")));
						validTestSet = true;
						validTestCase_TestSet++;
					}

				}

				totalTestCase += testCase_TestSet;
				validTestCase += validTestCase_TestSet;
				
				testSetHash.put(testSet.getName(), (double)validTestCase_TestSet/(double)testCase_TestSet);
//				if (validTestSet) {
//					testSetHash.put(testSet.getName(), 1);
//				} else {
//					testSetHash.put(testSet.getName(), 0);
//				}

				sb.append(faultyVersion.getName() + "\t" + testSet.getName()
						+ "\t" + testCases.length + "\t"
						+ validTestCases.size() + "\t" +"1"+ "\t");
				if(validTestCases.size() > 0)
					sb.append("1" + "\t");
				else
					sb.append("0" + "\t");
				sb.append((double)validTestCases.size()/(double)testCases.length+"\n");				
			}

			int validTestSet = 0;
			Iterator ite = testSetHash.values().iterator();
			double sum_PercentageOfValidTestCasesInTestSet = 0;
			while (ite.hasNext()) {
				double percentageOfValidTestCasesInTestSet = (Double)ite.next();
				
				if (percentageOfValidTestCasesInTestSet != 0.0) {
					validTestSet++;
				}
				sum_PercentageOfValidTestCasesInTestSet += percentageOfValidTestCasesInTestSet;
			}

		}

		Logger.getInstance().setPath(savePath, false);
		Logger.getInstance().write(sb.toString());
		Logger.getInstance().close();
	}
	
	
	/**2009/2/12: something wrong when conducting statistics
	 * 
	 * @param criteria
	 * @param savePath
	 */
//	public void getFaultDetectionRate(String criteria, String savePath) {
//		StringBuffer sb = new StringBuffer();
//		StringBuffer sb_sum = new StringBuffer();
//
//		
//		sb.append("FaultyVersion" + "\t" + "TestSet" + "\t" + "#TestCase"
//				+ "\t" + "#ValidTestCase" + "\n");
//		sb_sum.append("FaultyVersion" + "\t" + "#TestSet" + "\t"+ "#ValidTestSet"+ "\t"+"%ValidTestSet" 
//				+ "\t" +"#TestCase" + "\t" + "#ValidTestCase"+ "\t"+"%ValidTestCase" + "\t"+ "Avg.%ValidTestCaseInTestSet"+"\n");
//
//		File[] faultyVersions = new File(Constant.baseFolder + "test/output/"
//				+ criteria).listFiles();
//
//		ArrayList validTestCases = new ArrayList();
//
//		for (File faultyVersion : faultyVersions) {
//			File[] testSets = faultyVersion.listFiles();
//
//			HashMap testSetHash = new HashMap();
//			int totalTestCase = 0;
//			int validTestCase = 0;
//
//			for (File testSet : testSets) { // for each test set
//				
//				int testCase_TestSet = 0;
//				int validTestCase_TestSet = 0;
//				
//				boolean validTestSet = false;
//				validTestCases.clear();
//				File[] testCases = testSet.listFiles();
//				for (File testCase : testCases) {
//					testCase_TestSet++;
//					if (testCase.length() > 0) {
//						String name = testCase.getName();
//						validTestCases
//								.add(name.substring(0, name.indexOf(".")));
//						validTestSet = true;
//						validTestCase_TestSet++;
//					}
//
//				}
//
//				totalTestCase += testCase_TestSet;
//				validTestCase += validTestCase_TestSet;
//				
//				testSetHash.put(testSet.getName(), (double)validTestCase_TestSet/(double)testCase_TestSet);
////				if (validTestSet) {
////					testSetHash.put(testSet.getName(), 1);
////				} else {
////					testSetHash.put(testSet.getName(), 0);
////				}
//
//				sb.append(faultyVersion.getName() + "\t" + testSet.getName()
//						+ "\t" + testCases.length + "\t"
//						+ validTestCases.size() + "\n");
//			}
//
//			int validTestSet = 0;
//			Iterator ite = testSetHash.values().iterator();
//			double sum_PercentageOfValidTestCasesInTestSet = 0;
//			while (ite.hasNext()) {
//				double percentageOfValidTestCasesInTestSet = (Double)ite.next();
//				
//				if (percentageOfValidTestCasesInTestSet != 0.0) {
//					validTestSet++;
//				}
//				sum_PercentageOfValidTestCasesInTestSet += percentageOfValidTestCasesInTestSet;
//			}
//
//			sb_sum.append(faultyVersion.getName() + "\t" 
//					+ testSetHash.size() + "\t" + validTestSet + "\t" 
//					+ (double) validTestSet / (double) testSetHash.size() + "\t"
//					+ totalTestCase + "\t" + validTestSet + "\t"
//					+ (double) validTestCase / (double) totalTestCase + "\t"
//					+ sum_PercentageOfValidTestCasesInTestSet/(double)testSetHash.size()
//					+ "\n");
//		}
//
//		Logger.getInstance().setPath(savePath, false);
//		Logger.getInstance().write(sb.toString() + "\n" + sb_sum.toString());
//		Logger.getInstance().close();
//	}

	/**2009/2/13: for each test set, get its # of detected faults
	 * 
	 * @param sourcePath
	 * @param savePath
	 */
	public void getFaultPerformance(String sourcePath, String savePath){
		try {
			BufferedReader br = new BufferedReader(new FileReader(sourcePath));
			String str = null;
			HashMap testSetLists = new HashMap(); // TestSet-#detected faults
			
			
			//sum up #exposedFaults by each testSet
			while((str = br.readLine())!= null){
				String[] results = str.split("\t");
				if(!results[3].equals("0")){ //#validTestCase!=0
					String testSet = results[1];
					Object testCaseCount =testSetLists.get(testSet); 
					if(testCaseCount == null ){ // if not exist
						testSetLists.put(testSet, 1);	
					}else{
						testSetLists.put(testSet, (Integer)testCaseCount +1);
					}					
				}else{
					String testSet = results[1];
					Object testCaseCount =testSetLists.get(testSet);
					if(testCaseCount == null){//if not exist
						testSetLists.put(testSet, 0);
					}
				}
			}
			
			StringBuilder sb = new StringBuilder();
			sb.append("TestSet" + "\t" + "#ExposedFault" + "\n");
			Iterator ite = testSetLists.keySet().iterator();
			while(ite.hasNext()){
				String testSet = (String)ite.next();
				sb.append(testSet + "\t" + testSetLists.get(testSet) + "\n");
			}
			
			br.close();
			Logger.getInstance().setPath(savePath, false);
			Logger.getInstance().write(sb.toString());
			Logger.getInstance().close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	// public void analysisResult(String criteria, String analysisPath,
	// String savePath) {
	// StringBuilder sb = new StringBuilder();
	// File faultDir = new File(analysisPath);
	//
	// TestSetManager manager = new TestSetManager();
	// manager.getIntensities(Constant.baseFolder
	// + "ContextIntensity/TestPool.txt");
	//
	// if (faultDir.exists()) {
	// File[] faults = faultDir.listFiles();
	// for (File fault : faults) {
	// File[] testSets = fault.listFiles();
	//
	// for (File testSet : testSets) {
	// File[] testCases = testSet.listFiles();
	//
	// for (File testCase : testCases) {
	// String testCaseNumber = testCase.getName().substring(0,
	// testCase.getName().indexOf("."));
	// if (testCase.length() == 0) { // faultyVersion +
	// // testSetNumber +
	// // testCaseNumber + 0/1
	// // + intensity
	// sb.append(fault.getName() + "\t"
	// + testSet.getName() + "\t" + testCaseNumber
	// + "\t0\t"
	// + manager.getIntensity(testCaseNumber)
	// + "\n");
	// } else {
	// sb.append(fault.getName() + "\t"
	// + testSet.getName() + "\t" + testCaseNumber
	// + "\t1\t"
	// + manager.getIntensity(testCaseNumber)
	// + "\n");
	// }
	// }
	// }
	// }
	// }
	//
	// try {
	// BufferedWriter bw = new BufferedWriter(new FileWriter(savePath));
	// bw.write(sb.toString());
	// bw.close();
	// } catch (IOException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// }
	// }
	//
	// public HashMap getValidResult(String criteria, String analysisPath) {
	// HashMap histogram = new HashMap();
	// File faultDir = new File(analysisPath);
	// if (faultDir.exists()) {
	// File[] faults = faultDir.listFiles();
	// for (File fault : faults) {
	//
	// int TotalTestCase = 0;
	// File[] testSets = fault.listFiles();
	// HashMap validTestSet = new HashMap();
	//
	// for (File testSet : testSets) {
	// File[] testCases = testSet.listFiles();
	//
	// for (File testCase : testCases) {
	// if (testCase.length() == 0) { // a valid test case
	//
	// String testCaseIndex = testCase.getName()
	// .substring(0,
	// testCase.getName().indexOf("."));
	// if (validTestSet.get(testSet.getName()) == null) {
	// Vector validTestCase = new Vector();
	// validTestCase.add(testCaseIndex);
	// validTestSet.put(testSet.getName(),
	// validTestCase);
	// } else {
	// Vector validTestCases = (Vector) validTestSet
	// .get(testSet.getName());
	// validTestCases.add(testCaseIndex);
	// validTestSet.put(testSet.getName(),
	// validTestCases);
	// }
	// }
	// }
	// }
	//
	// histogram.put(fault.getName(), validTestSet);
	// }
	// }
	// return histogram;
	// }

	public static void main(String[] args) {
		// 1. analysis results
		TestResultManager manager = new TestResultManager();
		// String analysisPath =
		// "C:\\WangHuai\\Martin\\Eclipse3.3.1\\ContextToolkit_intensity\\test\\output\\data\\hwang\\bin\\test\\output\\CA";
		// String criteria = "CA";
		// // HashMap result = manager.analysisResult(criteria, analysisPath);
		 String savePath = Constant.baseFolder + "test/output/summary.txt";
		// manager.analysisResult(criteria, analysisPath, savePath);
		// System.out.println("A");

		// 1.2009/2/12: Analyze failure rates
//		String savePath = Constant.baseFolder + "test/output/failureRate.txt";
//		manager.getFailureRate(savePath);

		// 2.2009/2/12: Analyze fault detection rate of a specified testing
		// criterion
		String criteria = "CA";
		if (args.length == 1) {
			criteria = args[0];
		}
		savePath = Constant.baseFolder + "test/output/FaultDetectionRate_"
				+ criteria + ".txt";
		manager.getFaultDetectionRate(criteria, savePath);
		
		//3.2009/2/13
//		String sourcePath = Constant.baseFolder + "ContextIntensity/StoC1.txt";
//		savePath = Constant.baseFolder + "ContextIntensity/#FaultAnalysis_StoC1.txt";
//		manager.getFaultPerformance(sourcePath, savePath);
	}
}
