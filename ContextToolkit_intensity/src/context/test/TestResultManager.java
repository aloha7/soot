package context.test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Vector;

import context.test.util.Constant;

public class TestResultManager {

	public void analysisResult(String criteria, String analysisPath, String savePath){
		StringBuilder sb = new StringBuilder();
		File faultDir = new File(analysisPath);
		
		TestSetManager manager = new TestSetManager();
		manager.getIntensities(Constant.baseFolder
				+ "ContextIntensity/TestPool.txt");
		
		if(faultDir.exists()){
			File[] faults = faultDir.listFiles();
			for(File fault:faults){
				File[] testSets = fault.listFiles();
				
				for(File testSet: testSets){
					File[] testCases  =testSet.listFiles();
					
					for(File testCase: testCases){
						String testCaseNumber = testCase.getName().substring(0, testCase.getName().indexOf("."));
						if(testCase.length() ==0){ //faultyVersion + testSetNumber + testCaseNumber + 0/1 + intensity
							sb.append(fault.getName() + "\t" + testSet.getName() + "\t" + testCaseNumber + "\t0\t" + manager.getIntensity(testCaseNumber)+"\n");
						}else{
							sb.append(fault.getName() + "\t" + testSet.getName() + "\t" + testCaseNumber + "\t1\t" + manager.getIntensity(testCaseNumber)+"\n");
						}
					}
				}
			}
		}
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(analysisPath));
			bw.write(sb.toString());
			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	public HashMap getValidResult(String criteria, String analysisPath) {
		HashMap histogram = new HashMap();
		File faultDir = new File(analysisPath);
		if (faultDir.exists()) {
			File[] faults = faultDir.listFiles();
			for (File fault : faults) {

				int TotalTestCase = 0;
				File[] testSets = fault.listFiles();
				HashMap validTestSet = new HashMap();

				for (File testSet : testSets) {
					File[] testCases = testSet.listFiles();

					for (File testCase : testCases) {
						if (testCase.length() == 0) { // a valid test case

							String testCaseIndex = testCase.getName().substring(0, testCase.getName().indexOf("."));
							if (validTestSet.get(testSet.getName()) == null) { 
								Vector validTestCase = new Vector();
								validTestCase.add(testCaseIndex);
								validTestSet.put(testSet.getName(), validTestCase);
							} else {
								Vector validTestCases = (Vector)validTestSet.get(testSet.getName());
								validTestCases.add(testCaseIndex);
								validTestSet.put(testSet.getName(), validTestCases);
							}
						}
					}
				}

				histogram.put(fault.getName(), validTestSet);
			}
		}
		return histogram;
	}
	
	
	
	/**
	 * analysis the fault detection performance of a specified criteria
	 * 
	 * @param dir
	 * @param criteria
	 */
	public void getTestPerformance(String dir, String criteria) {

	}

	/**Failure rate of fault versions are calculated as rate of validTestSet and validTestCase 
	 * 
	 * @param result
	 * @param criteria
	 * @param savePath
	 */
//	public void getFailureRate(String analysisPath, String criteria, String savePath){
//		File faultDir = new File(analysisPath);
//		if (faultDir.exists()) {
//			File[] faults = faultDir.listFiles();
//			for (File fault : faults) {
//
//				int TotalTestCase = 0;
//				File[] testSets = fault.listFiles();
//				HashMap validTestSet = new HashMap();
//
//				for (File testSet : testSets) {
//					File[] testCases = testSet.listFiles();
//
//					for (File testCase : testCases) {
//						if (testCase.length() != 0) { // a valid test case
//
//							String testCaseIndex = testCase.getName().substring(0, testCase.getName().indexOf("."));
//							if (validTestSet.get(testSet.getName()) == null) { 
//								Vector validTestCase = new Vector();
//								validTestCase.add(testCaseIndex);
//								validTestSet.put(testSet.getName(), validTestCase);
//							} else {
//								Vector validTestCases = (Vector)validTestSet.get(testSet.getName());
//								validTestCases.add(testCaseIndex);
//								validTestSet.put(testSet.getName(), validTestCases);
//							}
//						}
//					}
//				}
//
//				histogram.put(fault.getName(), validTestSet);
//			}
//		}
//
//		
//	}
	
	
	
	public static void main(String[] args){
		//4. analysis results
		TestResultManager manager = new TestResultManager();
		String analysisPath = "C:\\WangHuai\\Martin\\Eclipse3.3.1\\ContextToolkit_intensity\\test\\output\\CA";
		String criteria = "CA";
//		HashMap result = manager.analysisResult(criteria, analysisPath);
		System.out.println("A");
	}
}
