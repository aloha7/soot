package ccr.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class ResultAnalyzer {
	public static final int TESTPOOL_SIZE = 20000;
	public static HashMap failureRate = new HashMap();
	
	/**2009-02-25: get the CI summary information for a given criterion
	 * criterion + minCI + meanCI + maxCI + stdCI
	 * 
	 * @param srcDir
	 * @param containHeader
	 * @param criterion
	 * @return
	 */
	public static String getCriteriaCI(String srcDir, boolean containHeader, String criterion){
		File[] files = new File(srcDir).listFiles();
		StringBuilder sb = new StringBuilder();
		
		for(File file: files){
			String fileName = file.getName();
			if(fileName.matches(criterion + "_CI.txt")){
				double minCI = Double.MAX_VALUE;
				double maxCI = Double.MIN_VALUE;
				double meanCI =0.0;
				double stdCI = 0.0;
				
				String str = null;
				double sumCI = 0.0;
				ArrayList CIs = new ArrayList();
				try {
					BufferedReader br = new BufferedReader(new FileReader(file));
					if(containHeader)
						br.readLine();
					
					while((str=br.readLine())!= null){
						String[] strs = str.split("\t");
						CIs.add(Double.parseDouble(strs[3]));
					}
					for(int i = 0; i < CIs.size(); i ++){
						double CI = (Double)CIs.get(i);
						
						sumCI += CI;
						if(CI > maxCI)
							maxCI = CI;
						if(CI < minCI)
							minCI = CI;
						
					}
					
					meanCI = sumCI/(double)CIs.size();
					double tempCI = 0.0;
					for(int i =0; i < CIs.size(); i ++){
						double CI = (Double)CIs.get(i);
						tempCI += (CI - meanCI) * (CI -meanCI);
					}
					stdCI = Math.sqrt(tempCI/(double)CIs.size());
					
					sb.append(criterion + "\t");
					sb.append(minCI + "\t");
					sb.append(meanCI + "\t");
					sb.append(maxCI +"\t");
					sb.append(stdCI +"\t"+"\n");
					
					
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		return sb.toString();
	}
	
	
	public static HashMap getCriterialCI(String srcDir, boolean containHeader, String criterion, String pattern){
		
		HashMap CIRange_CIStatistic = new HashMap();
		
		File[] files = new File(srcDir).listFiles();
	
		
		for(File file: files){
			String fileName = file.getName();
			if(fileName.matches(pattern)){
				String[] strs =fileName.split("_");
				String min = strs[1];
				String max = strs[2];
				String CIRange = min + "_" + max;
				
				
				double minCI = Double.MAX_VALUE;
				double maxCI = Double.MIN_VALUE;
				double meanCI =0.0;
				double stdCI = 0.0;
				
				String str = null;
				double sumCI = 0.0;
				ArrayList CIs = new ArrayList();
				try {
					BufferedReader br = new BufferedReader(new FileReader(file));
					if(containHeader)
						br.readLine();
					
					while((str=br.readLine())!= null){
						strs = str.split("\t");
						CIs.add(Double.parseDouble(strs[3]));
					}
					
					for(int i = 0; i < CIs.size(); i ++){
						double CI = (Double)CIs.get(i);
						
						sumCI += CI;
						if(CI > maxCI)
							maxCI = CI;
						if(CI < minCI)
							minCI = CI;
						
					}
					
					meanCI = sumCI/(double)CIs.size();
					double tempCI = 0.0;
					for(int i =0; i < CIs.size(); i ++){
						double CI = (Double)CIs.get(i);
						tempCI += (CI - meanCI) * (CI -meanCI);
					}
					stdCI = Math.sqrt(tempCI/(double)CIs.size());
					
					ArrayList CIStatistics = new ArrayList();
					CIStatistics.add(minCI);
					CIStatistics.add(meanCI);
					CIStatistics.add(maxCI);
					CIStatistics.add(stdCI);
					
					CIRange_CIStatistic.put(CIRange, CIStatistics);
					
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		return CIRange_CIStatistic;
	}
	
	
	/**2009-03-17: filter faults whose failure rate is below threshHold 
	 * 
	 * @param srcFaultFile
	 * @param threshHold
	 * @param saveFile
	 */
	public static ArrayList filterFaults(String srcFaultFile, boolean containHeader, double threshHold){
		ArrayList faults = new ArrayList();
		try {
			
			BufferedReader br = new BufferedReader(new FileReader(srcFaultFile));
			String line = null;
			
			if(containHeader)
				br.readLine();
			
			while((line = br.readLine())!= null){
				String[] strs = line.split("\t");
				String fault = strs[0];
				double failureRate = Double.parseDouble(strs[1]);
				if(failureRate <= threshHold){
					faults.add(fault);
				}
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return faults;
	}
	
	
	/**2009-03-17: filter faults whose failure rate is below threshHold, save results into a file 
	 * 
	 * @param srcFaultFile
	 * @param threshHold
	 * @param saveFile
	 */
	public static ArrayList filterFaults(String srcFaultFile, boolean containHeader, double threshHold, String saveFile){
		ArrayList faults = new ArrayList();
		try {
			
			BufferedReader br = new BufferedReader(new FileReader(srcFaultFile));
			String line = null;
			StringBuilder sb = new StringBuilder();
			if(containHeader)
				br.readLine();
			
			while((line = br.readLine())!= null){
				String[] strs = line.split("\t");
				String fault = strs[0];
				double failureRate = Double.parseDouble(strs[1]);
				if(failureRate <= threshHold){
					sb.append(fault+"\t" + failureRate + "\n");
					faults.add(fault);
				}
			}
			
			Logger.getInstance().setPath(saveFile, false);
			Logger.getInstance().write(sb.toString());
			Logger.getInstance().close();
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return faults;
	}
	
	/**2009-03-17: a powerful method to merge all files generated distributedly
	 * 
	 * @param srcDir
	 * @param containHeader
	 * @param pattern
	 * @param saveFile
	 */
	public static void mergeFiles(String srcDir, boolean containHeader, String pattern, String saveFile){
		File[] files = new File(srcDir).listFiles();
		StringBuilder sb = new StringBuilder();
		boolean writeHeader = false;
		for(File file: files){
			
			String fileName = file.getName();
			if(fileName.matches(pattern)){
				try {
					BufferedReader br = new BufferedReader(new FileReader(file));
					String str = null;
					
					
					if(containHeader && writeHeader) //write the header only once
						br.readLine();
					else{
						sb.append( br.readLine()+ "\n");
						writeHeader = true;
					}
					
					while((str = br.readLine())!= null){
						sb.append(str+"\n");
					}
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		Logger.getInstance().setPath(saveFile, false);
		Logger.getInstance().write(sb.toString());
		Logger.getInstance().close();
	}
	
	/**2009-02-24: due to the concurrent execution, we need to merge those partial results into a complete one 
	 * 
	 * @param srcDir
	 * @param criteria
	 * @param saveFile 
	 */
	public static void mergeTestResultFiles(String srcDir, String criteria, boolean containHeader, String saveFile){
		File[] files = new File(srcDir).listFiles();
		StringBuilder sb = new StringBuilder();
		
		for(File file: files){
			
			String fileName = file.getName();
			if(fileName.matches(criteria + "\\_[0-9]+\\_[0-9]+\\.txt")){
				try {
					BufferedReader br = new BufferedReader(new FileReader(file));
					String str = null;
					if(containHeader)
						br.readLine();
					
					while((str = br.readLine())!= null){
						sb.append(str+"\n");
					}
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		Logger.getInstance().setPath(saveFile, false);
		Logger.getInstance().write(sb.toString());
		Logger.getInstance().close();
	}
	
	/**2009-02-22: load the failure rate of each fault from the file
	 * 
	 * @param failureRateFile
	 * @param containHeader
	 */
	public static void loadFailureRate(String failureRateFile, boolean containHeader){
		try {
			BufferedReader br = new BufferedReader(new FileReader(failureRateFile));
			if(containHeader)
				br.readLine();
			
			String str = null;
			while((str = br.readLine())!= null){
				String[] strs = str.split("\t");
				String fault = strs[0];
				String failureRate = strs[1];
				ResultAnalyzer.failureRate.put(fault, failureRate);
			}
			br.close();
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
			
	}
	
	/**2009-2-18: the result is the a table with faulty version as columns, and CI of each test case
	 * as rows
	 * 
	 * @param detailFile:records of executions of each test case in a test pool
	 * @param saveFile:
	 * @param containHeader:whether the first row of detailFile is a header or not
	 */
	public static void translateFormat(String detailFile, String saveFile, boolean containHeader){
		try {
			BufferedReader br =new BufferedReader(new FileReader(detailFile));
			String str = null;
			HashMap faults = new HashMap();
			if(containHeader)
				br.readLine();
			
			while((str = br.readLine())!= null){
				String[] strs = str.split("\t");
				if(strs[2].equals("F")){
					double CI = ((TestCase)Adequacy.testCases.get(strs[1])).CI;
					if(faults.containsKey(strs[0]))
						((ArrayList)faults.get(strs[0])).add(CI);
					else{
						ArrayList temp = new ArrayList();
						temp.add(CI);
						faults.put(strs[0], temp);
					}
				}
			}
			br.close();
			
			Iterator ite = faults.keySet().iterator();
			StringBuilder sb = new StringBuilder();
			while(ite.hasNext()){
				String fault = (String)ite.next();
				sb.append(fault.substring(fault.indexOf("_")+"_".length()) + "\t");
				ArrayList CIs = (ArrayList)faults.get(fault);
				for(int i = 0; i < CIs.size(); i++){
					sb.append(CIs.get(i)+"\t");
				}
				sb.append("\n");
			}
			
			Logger.getInstance().setPath(saveFile, false);
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
	
	/**2009-02-19: the result is a table with faults as rows and valid test cases to expose such a fault as columns 
	 * 
	 * @param testDetailFile
	 * @param containHeader
	 * @return
	 */
	public static HashMap getFaultsWithTestCase(String testDetailFile, boolean containHeader){
		HashMap faults = new HashMap();
		
		try {
			BufferedReader br = new BufferedReader(new FileReader(testDetailFile));
			String str = null;
			if(containHeader)
				br.readLine();
			
			while((str = br.readLine())!= null){
				String[] strs = str.split("\t");
				String fault = strs[0].substring(strs[0].indexOf("_")+"_".length());

				if(strs[2].equals("F")){//if a test case exposes a fault
					ArrayList testCases;
					if(faults.containsKey(fault))
						testCases = (ArrayList)faults.get(fault);
					else
						testCases = new ArrayList();
					
					String testCase = strs[1];
					if(!testCases.contains(strs[1])){
						testCases.add(testCase); //add this valid test case	
					}
					faults.put(fault, testCases);
				}else{ //
					if(!faults.containsKey(fault)){
						faults.put(fault, new ArrayList());
					}
				}
			}
			br.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return faults;
	}
	
	/**2009-02-19: get failure rate of faults according to the execution records of test pool
	 * 
	 * @param testDetailFile: the execution records of test cases in a test pool
	 * @param saveFile:
	 */
	public static void getFailureRate(String testDetailFile, boolean containHeader, String saveFile){
		HashMap faults = ResultAnalyzer.getFaultsWithTestCase(testDetailFile, containHeader);
		Iterator ite = faults.keySet().iterator();
		StringBuilder sb = new StringBuilder();
		sb.append("Fault" + "\t" + "FailureRate" + "\n");
		while(ite.hasNext()){
			String fault = (String)ite.next();
			double detected = ((ArrayList)faults.get(fault)).size();
			sb.append(fault + "\t" + detected/(double)ResultAnalyzer.TESTPOOL_SIZE + "\n");
		}
		
		Logger.getInstance().setPath(saveFile, false);
		Logger.getInstance().write(sb.toString());
		Logger.getInstance().close();
		
	}
	
	/**2009-02-19: the result is a table with test cases as rows and 
	 * its exposed faults as columns
	 * 
	 * @param detailFile: the file recording the execution of test pool
	 * @return
	 */
	public static HashMap getTestCaseWithFaults(String testDetailFile, boolean containHeader){
		HashMap testCases = new HashMap();
		
		try {
			BufferedReader br = new BufferedReader(new FileReader(testDetailFile));
			String str = null;
			if(containHeader)
				br.readLine();
			
			while((str=br.readLine())!= null){
				String[] strs = str.split("\t");
				
				if(strs[2].equals("F")){//if a test case exposes a fault
					String fault = strs[0].substring(strs[0].indexOf("_")+"_".length());
					String testCase = strs[1];
					
					ArrayList faults;
					if(testCases.containsKey(testCase))
						faults = (ArrayList)testCases.get(testCase);
					else
						faults = new ArrayList();
					
					if(!faults.contains(fault))
						faults.add(fault);
					testCases.put(testCase, faults);
				}
			}
			br.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return testCases;
	}
	
	
	
	/**2009-02-19: the result is a table with test set as rows and all its test cases as columns
	 * 
	 * @param testSetFile
	 * @param containHeader
	 * @return
	 */
	public static HashMap getTestCaseFromTestSet(String testSetFile, boolean containHeader){
		HashMap testSets = new HashMap();//testSet-TestCases
		try {
			BufferedReader br = new BufferedReader(new FileReader(testSetFile));
			String str = null;
			if(containHeader)
				br.readLine();
			
			ArrayList lines = new ArrayList();
			while((str = br.readLine())!= null){
				lines.add(str);
			}
			
			
			for(int i = 0; i < lines.size()-3; i ++){
				str = (String)lines.get(i);
				String[] strs = str.split("\t");
				String testCases = strs[6];
				testCases= testCases.substring(testCases.indexOf("[")+"[".length());
				testCases= testCases.substring(0, testCases.indexOf("]"));
				String[] testCaseList = testCases.split(",");
				ArrayList tcList = new ArrayList();
				for(String tc: testCaseList){
					tcList.add(tc);
				}
				testSets.put(strs[0], tcList);
			}
			br.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return testSets;
	}
	
	/**analysis valid test cases within a test set, percentage of validTestCase can be one dimension to compare two testing criteria
	 * the result is a table with fault as rows and averaged-%validTestCases in all testSet as columns
	 * @param testSetExecutionFile
	 * @param containHeader
	 * @param saveFile
	 */
	public static HashMap perValidTestCaseWithinTestSet(String testSetExecutionFile, boolean containHeader, String saveFile){
		HashMap fault_perValidTC = new HashMap();
		StringBuilder sb = new StringBuilder();
		try {
			BufferedReader br = new BufferedReader(new FileReader(testSetExecutionFile));
			String str = null;
			if(containHeader)
				br.readLine();
			
			HashMap fault_PerValidTestCases = new HashMap();
			while((str = br.readLine())!= null){
				String[] strs = str.split("\t");
				String fault = strs[0];
				double perValidTestCase = Double.parseDouble(strs[4]);
				
				ArrayList perValidTestCases; 
				if(fault_PerValidTestCases.containsKey(fault))
					perValidTestCases = (ArrayList)fault_PerValidTestCases.get(fault);
				else
					perValidTestCases = new ArrayList();
				
				perValidTestCases.add(perValidTestCase);
				fault_PerValidTestCases.put(fault, perValidTestCases);
			}
			
			
			sb.append("Fault" + "\t" + "Avg.PerValidTestCases" + "\t"+ "FailureRate"+ "\t"+"Std.PerValidTestCases" + "\n");
			Iterator ite = fault_PerValidTestCases.keySet().iterator();
			while(ite.hasNext()){
				String fault = (String)ite.next();
				ArrayList perValidTestCases = (ArrayList)fault_PerValidTestCases.get(fault);
				double sum = 0.0;
				for(int i = 0; i < perValidTestCases.size(); i ++){
					sum += (Double)perValidTestCases.get(i);
				}
				double avg = sum/(double)perValidTestCases.size();
				
				sum = 0.0;
				for(int i = 0; i < perValidTestCases.size(); i ++){
					double temp = (Double)perValidTestCases.get(i);
//					sum +=  Math.pow(temp-avg, 2.0);
					sum += (temp-avg)*(temp-avg); 
				}
				double std = Math.sqrt(sum/(double)perValidTestCases.size());
				sb.append(fault + "\t" + avg+"\t" +ResultAnalyzer.failureRate.get(fault) + "\t"+ std + "\n");
				fault_perValidTC.put(fault, avg);
			}
			
			br.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		Logger.getInstance().setPath(saveFile, false);
		Logger.getInstance().write(sb.toString());
		Logger.getInstance().close();
		
		return fault_perValidTC;
		
	}
	
	/**2009-02-19: analysis faults number and faults type detected by an adequate test set
	 * the result is a table with test set as rows and faults number and types it exposed as columns
	 * @param testSetExecutionFile
	 * @param containHeader
	 * @param saveFile
	 */
	public static HashMap faultsExposedByTestSet(String testSetExecutionFile, boolean containHeader, String saveFile){
		HashMap testSet_fault = new HashMap();
		StringBuilder sb = new StringBuilder();
		try {
			BufferedReader br = new BufferedReader(new FileReader(testSetExecutionFile));
			String str = null;
			if(containHeader)
				br.readLine();
			
			HashMap testSet_FaultType = new HashMap();
			while((str = br.readLine())!= null){
				String[] strs = str.split("\t");
				String fault = strs[0];
				String testSet = strs[1];
				String validTestSet = strs[5];
				if(validTestSet.equals("1")){ //this test set can expose this fault
					ArrayList faultTypes;
					if(testSet_FaultType.containsKey(testSet))
						faultTypes = (ArrayList)testSet_FaultType.get(testSet);
					else
						faultTypes = new ArrayList();
					if(!faultTypes.contains(fault))
						faultTypes.add(fault);
					testSet_FaultType.put(testSet, faultTypes);
				}else{
					if(!testSet_FaultType.containsKey(testSet))
						testSet_FaultType.put(testSet, new ArrayList());
				}
			}
			br.close();
			
			sb.append("TestSet" + "\t" + "FaultTypes" + "\n");
			Iterator ite = testSet_FaultType.keySet().iterator();
			while(ite.hasNext()){
				String testSet = (String)ite.next();
				ArrayList faultTpes = (ArrayList)testSet_FaultType.get(testSet);
				sb.append(testSet + "\t" + faultTpes.size() + "\n");
				testSet_fault.put(testSet, faultTpes.size());
			}
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		Logger.getInstance().setPath(saveFile, false);
		Logger.getInstance().write(sb.toString());
		Logger.getInstance().close();
		return testSet_fault;
	}
	
	/**return the size_averagePerValidTestSet for a given testing criteria
	 * 
	 * @param testSetExecutionFile
	 * @param containHeader
	 * @param saveFile
	 * @return
	 */
	public static HashMap getTSPerValidTestSet(String srcDir, String criterion, boolean containHeader){
		HashMap size_perValidTestSet = new HashMap();
		
		File[] files = new File(srcDir).listFiles();
		
		ArrayList faults = new ArrayList();
		//1.get all interested faults
		try {
			BufferedReader br_temp = new BufferedReader(new FileReader(srcDir + "FaultList.txt"));
			String line = null;
			
			while((line=br_temp.readLine())!= null){
				faults.add(line);
			}
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		for(File file: files){
			String fileName = file.getName();
			if(fileName.matches(criterion + "\\_[0-9]+\\_limited_load.txt")){
				try {
					BufferedReader br = new BufferedReader(new FileReader(file));
					String str = null;
					if(containHeader)
						br.readLine();
					
					//get fault-test sets pair
					HashMap fault_TestSets = new HashMap(); 
					while((str = br.readLine())!= null){
						String[] strs = str.split("\t");
						String fault = strs[0];
						
						if(!faults.contains(fault))
								continue;
						
						String validTestSet = strs[5];
						
						ArrayList testSets;
						if(fault_TestSets.containsKey(fault))
							testSets = (ArrayList)fault_TestSets.get(fault);
						else
							testSets = new ArrayList();
						
						testSets.add(validTestSet);
						fault_TestSets.put(fault, testSets);
					}
					br.close();
					
					//count the valid test sets for each fault
					double sum_perValidTestSet = 0.0;
					Iterator ite = fault_TestSets.keySet().iterator();
					while(ite.hasNext()){ // for each fault
						String fault = (String)ite.next();
						ArrayList testSets = (ArrayList)fault_TestSets.get(fault);
						int validCounter = 0;
						for(int i = 0; i < testSets.size(); i ++){
							String testSet = (String)testSets.get(i);
							if(testSet.equals("1"))
								validCounter ++;
						}
						sum_perValidTestSet += (double)validCounter/(double)testSets.size();
					}
					double mean_perValidTestSet = sum_perValidTestSet/(double)fault_TestSets.size();
					String size = fileName.substring(((String)(criterion+"_")).length(), fileName.indexOf("_limited_load.txt"));
					size_perValidTestSet.put(size, mean_perValidTestSet);
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return size_perValidTestSet;
	}
	
	/**2009-03-10: get the relationship between CI and testing performance of a specified criterion
	 * 
	 * @param srcDir
	 * @param criterion
	 * @param containHeader
	 * @return
	 */
	public static HashMap getCIPerValidTestSet(String srcDir, String criterion, boolean containHeader){
		HashMap CIRange_perValidTestSet = new HashMap();
		
		File[] files = new File(srcDir).listFiles();
		
		ArrayList faults = new ArrayList();
		//1.get all interested faults
		try {
			BufferedReader br_temp = new BufferedReader(new FileReader(srcDir + "/FaultList.txt"));
			String line = null;
			
			while((line=br_temp.readLine())!= null){
				faults.add(line);
			}
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		for(File file: files){
			String fileName = file.getName();
			if(fileName.matches(criterion + "\\_0\\.[0-9]+\\_0\\.[0-9]+\\_limited_load.txt")){
				try {
					BufferedReader br = new BufferedReader(new FileReader(file));
					String str = null;
					if(containHeader)
						br.readLine();
					
					//get fault-test sets pair
					HashMap fault_TestSets = new HashMap(); 
					while((str = br.readLine())!= null){
						String[] strs = str.split("\t");
						String fault = strs[0];
						
						if(!faults.contains(fault))
								continue;
						
						String validTestSet = strs[5];
						
						ArrayList testSets;
						if(fault_TestSets.containsKey(fault))
							testSets = (ArrayList)fault_TestSets.get(fault);
						else
							testSets = new ArrayList();
						
						testSets.add(validTestSet);
						fault_TestSets.put(fault, testSets);
					}
					br.close();
					
					//count the valid test sets for each fault
					double sum_perValidTestSet = 0.0;
					Iterator ite = fault_TestSets.keySet().iterator();
					while(ite.hasNext()){ // for each fault
						String fault = (String)ite.next();
						ArrayList testSets = (ArrayList)fault_TestSets.get(fault);
						int validCounter = 0;
						for(int i = 0; i < testSets.size(); i ++){
							String testSet = (String)testSets.get(i);
							if(testSet.equals("1"))
								validCounter ++;
						}
						sum_perValidTestSet += (double)validCounter/(double)testSets.size();
					}
					double mean_perValidTestSet = sum_perValidTestSet/(double)fault_TestSets.size();
					String[] strs = fileName.split("_");
					
					String CIRange = strs[1] + "_" + strs[2];
					
					CIRange_perValidTestSet.put(CIRange, mean_perValidTestSet);
					
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return CIRange_perValidTestSet;
	}
	
	
	/**return the fault-perValidTestSet(String->double) for a given testing criterion
	 * 
	 * @param testSetExecutionFile
	 * @param containHeader
	 * @param saveFile
	 * @return
	 */
	public static HashMap perValidTestSet(String testSetExecutionFile, boolean containHeader, String saveFile){
		HashMap fault_perValidTestSet = new HashMap();
		StringBuilder sb = new StringBuilder();
		
		try {
			BufferedReader br = new BufferedReader(new FileReader(testSetExecutionFile));
			String str = null;
			if(containHeader)
				br.readLine();
			
			
			HashMap fault_TestSets = new HashMap();
			while((str = br.readLine())!= null){
				String[] strs = str.split("\t");
				String fault = strs[0];
				String validTestSet = strs[5];
				
				ArrayList testSets;
				if(fault_TestSets.containsKey(fault))
					testSets = (ArrayList)fault_TestSets.get(fault);
				else
					testSets = new ArrayList();
				
				testSets.add(validTestSet);
				fault_TestSets.put(fault, testSets);
			}
			br.close();
			
			
//			sb.append("Fault" + "\t" + "%ValidTestSet" + "\t" + "FailureRate" + "\n");
			sb.append("FailureRate" + "\t" + "%ValidTestSet" + "\t" + "Fault" + "\n");
			Iterator ite = fault_TestSets.keySet().iterator();
			while(ite.hasNext()){
				String fault = (String)ite.next();
				ArrayList testSets = (ArrayList)fault_TestSets.get(fault);
				int validCounter = 0;
				for(int i = 0; i < testSets.size(); i ++){
					String testSet = (String)testSets.get(i);
					if(testSet.equals("1"))
						validCounter ++;
				}
				double perValidTestSet = (double)validCounter/(double)testSets.size();
				sb.append(ResultAnalyzer.failureRate.get(fault) + "\t" + perValidTestSet + "\t" + fault+"\n");
				fault_perValidTestSet.put(fault, perValidTestSet);
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//2009-03-08: no need to save the file
//		Logger.getInstance().setPath(saveFile, false);
//		Logger.getInstance().write(sb.toString());
//		Logger.getInstance().close();
		return fault_perValidTestSet;
	}
	
	/**2009-03-27: we wish to rename the original criteria
	 * 
	 * @param criteria
	 * @param rename_criteria
	 * @param criterion_Metric
	 * @param date
	 * @param saveFile
	 */
	public static void mergeHashMap(String[] criteria, String[] rename_criteria, HashMap criterion_Metric, String date, String saveFile){
		StringBuilder sb = new StringBuilder();
		
		
		//Header
		sb.append("Fault" + "\t" + "FailureRate" + "\t");
		
		for(int i =0; i < criteria.length; i ++){
			//2009-03-27: we wish to shorten the name of criterion
			String criterion = rename_criteria[i];
			sb.append(criterion + "\t");

//			sb.append(criteria[i] + "\t");
		}
		sb.append("\n");
		
		//2009-03-08: we may not interest in all faults
//		Iterator ite = ResultAnalyzer.failureRate.keySet().iterator();
		
		//2009-03-08: we may only interest in faults in fault list
		ArrayList faultList = new ArrayList();
		
		try {
			BufferedReader br = new BufferedReader(new FileReader("src/ccr/experiment" +
					"/Context-Intensity_backup/TestHarness/"+date+"/FaultList.txt")); 
			String str = null;
			while((str = br.readLine())!=null){
				faultList.add(str);
			}
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		String criterion = null;
		for(int j = 0; j < faultList.size(); j ++){
			String fault = (String)faultList.get(j);
			sb.append(fault + "\t" + ResultAnalyzer.failureRate.get(fault)+"\t");
			for(int i = 0; i < criteria.length; i++){
				criterion = criteria[i];
				HashMap metric = (HashMap)criterion_Metric.get(criterion);
				
				if(!metric.containsKey(fault))
					System.out.println("No existing");
				
				Object item = metric.get(fault);
				if(item == null){
					item = metric.get(Integer.parseInt(fault) + "");
				}
				sb.append(item+"\t");
			}
			sb.append("\n");	
		}
		
		
		
		//2009-03-07: we need a more abstract information, for example: min, max, mean 
		//and SD for each criterion 
		//another header
		sb.append("\nCriterion\tMin\tMean\tMax\tSD\n");
		for(int i = 0; i < criteria.length; i ++){
			criterion = criteria[i];
			
			//2009-03-27: we wish to rename the original criteria
			sb.append(rename_criteria[i] + "\t");
//			sb.append(criterion + "\t");
			
			HashMap metric = (HashMap)criterion_Metric.get(criterion);
			
			double[] performances = new double[faultList.size()];
			for(int j = 0; j < faultList.size(); j ++){
				String fault = (String)faultList.get(j);
				Double performance = (Double)metric.get(fault);
				performances[j] = performance;
			}
			
			
//			
//			int index = 0;
//			Iterator ite1 = metric.keySet().iterator();
//			while(ite1.hasNext()){
//				String fault = (String)ite1.next();
//				if(!faultList.contains(fault))
//					continue;
//					
//				Double  performance= (Double)metric.get(fault);
//				if(performance == null){
//					performance = (Double)metric.get(Integer.parseInt(fault) + "");
//				}
//				
//				performances[index] =  performance;
//				index++;
//			}
//			
			double min = Double.MAX_VALUE;
			double max = Double.MIN_VALUE;
			double sum = 0;
			
			for(int j = 0; j < performances.length; j ++){
				sum += performances[j];
				
				if(performances[j] > max)
					max = performances[j];
				if(performances[j] < min)
					min = performances[j];
			}
			double mean = sum/(double)performances.length;
			
			sum = 0.0;
			for(int j = 0; j < performances.length; j ++){
				sum += (performances[j]-mean)*(performances[j]-mean);
			}
			double SD = Math.sqrt(sum/performances.length);
			sb.append(min + "\t" + mean +"\t" + max+"\t" + SD+ "\n");
		}
		
		
		Logger.getInstance().setPath(saveFile, false);
		Logger.getInstance().write(sb.toString());
		Logger.getInstance().close();
	}

	
	public static void mergeHashMap(String[] criteria, HashMap criterion_Metric, String date, String saveFile){
		StringBuilder sb = new StringBuilder();
		
		
		//Header
		sb.append("Fault" + "\t" + "FailureRate" + "\t");
		
		for(int i =0; i < criteria.length; i ++){
			//2009-03-27: we wish to shorten the name of criterion
			String criterion = criteria[i].replaceAll("TestSets", "");
			sb.append(criterion + "\t");

//			sb.append(criteria[i] + "\t");
		}
		sb.append("\n");
		
		//2009-03-08: we may not interest in all faults
//		Iterator ite = ResultAnalyzer.failureRate.keySet().iterator();
		
		//2009-03-08: we may only interest in faults in fault list
		ArrayList faultList = new ArrayList();
		
		try {
			BufferedReader br = new BufferedReader(new FileReader("src/ccr/experiment" +
					"/Context-Intensity_backup/TestHarness/"+date+"/FaultList.txt")); 
			String str = null;
			while((str = br.readLine())!=null){
				faultList.add(str);
			}
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		String criterion = null;
		for(int j = 0; j < faultList.size(); j ++){
			String fault = (String)faultList.get(j);
			sb.append(fault + "\t" + ResultAnalyzer.failureRate.get(fault)+"\t");
			for(int i = 0; i < criteria.length; i++){
				criterion = criteria[i];
				HashMap metric = (HashMap)criterion_Metric.get(criterion);
				
				if(!metric.containsKey(fault))
					System.out.println("No existing");
				
				Object item = metric.get(fault);
				if(item == null){
					item = metric.get(Integer.parseInt(fault) + "");
				}
				sb.append(item+"\t");
			}
			sb.append("\n");	
		}
		
		
		
		//2009-03-07: we need a more abstract information, for example: min, max, mean 
		//and SD for each criterion 
		//another header
		sb.append("\nCriterion\tMin\tMean\tMax\tSD\n");
		for(int i = 0; i < criteria.length; i ++){
			criterion = criteria[i];
			sb.append(criterion + "\t");
			HashMap metric = (HashMap)criterion_Metric.get(criterion);
			
			double[] performances = new double[faultList.size()];
			for(int j = 0; j < faultList.size(); j ++){
				String fault = (String)faultList.get(j);
				Double performance = (Double)metric.get(fault);
				performances[j] = performance;
			}
			
			
//			
//			int index = 0;
//			Iterator ite1 = metric.keySet().iterator();
//			while(ite1.hasNext()){
//				String fault = (String)ite1.next();
//				if(!faultList.contains(fault))
//					continue;
//					
//				Double  performance= (Double)metric.get(fault);
//				if(performance == null){
//					performance = (Double)metric.get(Integer.parseInt(fault) + "");
//				}
//				
//				performances[index] =  performance;
//				index++;
//			}
//			
			double min = Double.MAX_VALUE;
			double max = Double.MIN_VALUE;
			double sum = 0;
			
			for(int j = 0; j < performances.length; j ++){
				sum += performances[j];
				
				if(performances[j] > max)
					max = performances[j];
				if(performances[j] < min)
					min = performances[j];
			}
			double mean = sum/(double)performances.length;
			
			sum = 0.0;
			for(int j = 0; j < performances.length; j ++){
				sum += (performances[j]-mean)*(performances[j]-mean);
			}
			double SD = Math.sqrt(sum/performances.length);
			sb.append(min + "\t" + mean +"\t" + max+"\t" + SD+ "\n");
		}
		
		
		Logger.getInstance().setPath(saveFile, false);
		Logger.getInstance().write(sb.toString());
		Logger.getInstance().close();
	}
	
	public static void getCorrelationTSPeformance(String[] criteria, HashMap criterion_size_performance, String saveFile){

		//2. Header, keeping the order
		StringBuilder sb = new StringBuilder();
		sb.append("Size" + "\t");
		for(int i = 0; i < criteria.length; i ++){
			sb.append(criteria[i] + "\t");
		}
		sb.append("\n");
		
		String criterion = (String)criteria[0]; // standard criterion
		HashMap size_performance = (HashMap)criterion_size_performance.get(criterion);
		
		//3.summary all results
		Iterator it1 = size_performance.keySet().iterator();
		while(it1.hasNext()){
			String size = (String)it1.next();
			sb.append(size + "\t");
			
			ArrayList criterion_performance = new ArrayList(); 
			for(int i = 0; i < criteria.length; i ++){
				double performance = (Double)((HashMap)criterion_size_performance.get(criteria[i])).get(size);
				sb.append(performance + "\t");
			}
			sb.append("\n");
		}
		
		Logger.getInstance().setPath(saveFile, false);
		Logger.getInstance().write(sb.toString());
		Logger.getInstance().close();
		
	}
	
	/*public static HashMap getMatchPair(String[] criteria, String[] replace_name){
		HashMap matchPair = new HashMap();
		if(criteria.length ==  replace_name.length){
			for(int i = 0; i < criteria.length;  i++){
				matchPair.put(criteria[i], replace_name[i]);
			}
		}
		
		return matchPair;
	}*/
	
	public static void getCorrelationCIPeformance(String[] criteria, HashMap criterion_CIs, HashMap criterion_CIRange_performance, String saveFile){

		//2. Header, keeping the order
		StringBuilder sb = new StringBuilder();
		sb.append("CIRange\t");
		for(int i = 0; i < criteria.length; i ++){
			sb.append("CI\t" +criteria[i] + "\t");
		}
		sb.append("\n");
		
		String criterion = (String)criteria[0]; // standard criterion
		HashMap CIRange_performance = (HashMap)criterion_CIRange_performance.get(criterion);
		
		//3.summary all results
		Iterator it1 = CIRange_performance.keySet().iterator();
		while(it1.hasNext()){
			String CIRange = (String)it1.next();
			sb.append(CIRange + "\t");
			
			ArrayList criterion_performance = new ArrayList(); 
			for(int i = 0; i < criteria.length; i ++){
				criterion = criteria[i];
				double meanCI = (Double)((ArrayList)((HashMap)criterion_CIs.get(criterion)).get(CIRange)).get(1); 
				CIRange_performance = (HashMap)criterion_CIRange_performance.get(criterion);				
				double performance = (Double)CIRange_performance.get(CIRange);
				sb.append(meanCI + "\t" +performance + "\t");
			}
			sb.append("\n");
		}
		
		Logger.getInstance().setPath(saveFile, false);
		Logger.getInstance().write(sb.toString());
		Logger.getInstance().close();
		
	}
	
	/**2009-03-28:
	 * 
	 * @param criteria
	 * @param start
	 * @param end
	 * @return
	 */
	public static String getLostTest(String[] criteria, int start, int end){
		
		StringBuilder sb = new StringBuilder();
		HashMap criterion_LostTest = new HashMap();
		
		
		
		return sb.toString();
	}
	
	public static void main(String[] args){
		System.out
		.println("USAGE: java ccr.test.ResultAnalyzer <Context_Intensity,Limited,Load>"
				+ "<directory(20090305)>");

		String instruction = args[0];
		String date = args[1];
		
		String testcaseFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"+date+"/TestPool.txt";
		
		//2009-02-18: load CI of each test case from a file
		boolean containHeader = true;
		Adequacy.getTestPool(testcaseFile, containHeader);
		
		//2009-02-22: load failure rates from a file
		containHeader = false;
		String failureRateFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"+date+"/failureRate.txt";
		ResultAnalyzer.loadFailureRate(failureRateFile, containHeader);

		//2009-02-24: merge files
		String srcDir = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"+date+"/";
		containHeader = true;
		String[] criteria = new String[]{
				//Group 1
				"RandomTestSets_27",
				"AllPoliciesTestSets_old",
				"AllPoliciesTestSets_new",
				"RandomTestSets_42",
				"All1ResolvedDUTestSets_old",
				"All1ResolvedDUTestSets_new",
				"RandomTestSets_50",
				"All2ResolvedDUTestSets_old",
				"All2ResolvedDUTestSets_new",
				
				//Group 2
				"RandomTestSets_62",
				"AllPoliciesTestSets_old_random_62",
				"AllPoliciesTestSets_new_random_62",
				"All1ResolvedDUTestSets_old_random_62",
				"All1ResolvedDUTestSets_new_random_62",
				"All2ResolvedDUTestSets_old_random_62",
				"All2ResolvedDUTestSets_new_random_62",

				//Group 3
//				"RandomTestSets_62",
				"AllPoliciesTestSets_old_criteria_62",
				"AllPoliciesTestSets_new_criteria_62",
				"All1ResolvedDUTestSets_old_criteria_62",
				"All1ResolvedDUTestSets_new_criteria_62",
				"All2ResolvedDUTestSets_old_criteria_62",
				"All2ResolvedDUTestSets_new_criteria_62",
		};
		
		
		if(instruction.equals("Context_Intensity") ||instruction.equals("Limited")||instruction.equals("Load")){
//			HashMap criterion_faultNum = new HashMap();
//			HashMap criterion_perValidTC = new HashMap();
			HashMap criterion_perValidTS = new HashMap();

			String saveFile = null;
			for(int i = 0; i < criteria.length; i++){
				saveFile = srcDir + criteria[i] + "_limited_load.txt";
//				ResultAnalyzer.mergeTestResultFiles(srcDir, criteria[i], containHeader, saveFile);
				
				String testSetExecutionFile = saveFile;
				boolean containHeader1 = true;
				
//				String faultTypeFile = saveFile.substring(0, saveFile.indexOf("_testing.txt")) + "_FaultType.txt";
//				HashMap faultNum = ResultAnalyzer.faultsExposedByTestSet(testSetExecutionFile, containHeader1, faultTypeFile);
//				criterion_faultNum.put(criteria[i], faultNum);
//				
//				String perValidTCFile = saveFile.substring(0, saveFile.indexOf("_testing.txt")) + "_PerValidTestCase.txt";
//				HashMap perValidTC = ResultAnalyzer.perValidTestCaseWithinTestSet(testSetExecutionFile, containHeader1, perValidTCFile);
//				criterion_perValidTC.put(criteria[i], perValidTC);
				
				String perValidTSFile = saveFile.substring(0, saveFile.indexOf("_limited_load.txt")) + "_PerValidTestSet.txt";
				HashMap perValidTS = ResultAnalyzer.perValidTestSet(testSetExecutionFile, containHeader1, perValidTSFile);
				criterion_perValidTS.put(criteria[i], perValidTS);
			}
			
			//2009-02-24: to summarize all three views to evaluate the performance of testing criteria
//			saveFile = srcDir +"/FaultNum.txt";
//			ResultAnalyzer.mergeHashMap(criteria, criterion_faultNum, saveFile);
//			saveFile = srcDir + "/PerValidTC.txt";
//			ResultAnalyzer.mergeHashMap(criteria, criterion_perValidTC, saveFile);
			saveFile = srcDir + "/PerValidTS.txt";
			
			//2009-03-27: rename the default criterion
			String[] rename_criteria = new String[]{
					//rename the criteria of Group 1
					"Random-27",
					"All-Services",
					"All-Services-Refined",
					"Random-42",
					"All-Services-Uses",
					"All-Services-Uses-Refined",
					"Random-50",
					"All-2-Services-Uses",
					"All-2-Services-Uses-Refined",
					
					//rename the criteria of Group 2
					"Random-62",
					"All-Services-random",
					"All-Services-random-Refined",				
					"All-Services-Uses-random",
					"All-Services-Uses-random-Refined",				
					"All-2-Services-Uses-random",
					"All-2-Services-Uses-random-Refined",
					
					//rename the criteria of Group 3
					"All-Services-criterion",
					"All-Services-criterion-Refined",				
					"All-Services-Uses-criterion",
					"All-Services-Uses-criterion-Refined",				
					"All-2-Services-Uses-criterion",
					"All-2-Services-Uses-criterion-Refined",
					
			};
			ResultAnalyzer.mergeHashMap(criteria, rename_criteria, criterion_perValidTS, date, saveFile);
//			ResultAnalyzer.mergeHashMap(criteria, criterion_perValidTS, date, saveFile);
			
			//2009-02-25: to explore the CI distributions of different testing criteria
			StringBuilder sb = new StringBuilder();
			sb.append("criteria" + "\t" + "minCI" + "\t" + "meanCI" + "\t" + "maxCI" + "\t" + "stdCI" + "\n" );
			for(int i =0; i < criteria.length; i ++){
				sb.append(ResultAnalyzer.getCriteriaCI(srcDir, containHeader, criteria[i]));
			}
			
			saveFile = srcDir + "/CI.txt";
			Logger.getInstance().setPath(saveFile, false);
			Logger.getInstance().write(sb.toString());
			Logger.getInstance().close();
		}else if(instruction.equals("getSizePerformance")){
			//2009-03-06: get the correlations between test set size and fault detection rate of each criterion
			
			//refine the criteria
			criteria = new String[]{
//					//Group 1
//					"AllPoliciesTestSets_old",
//					"AllPoliciesTestSets_new",
//					"All1ResolvedDUTestSets_old",
//					"All1ResolvedDUTestSets_new",
//					"All2ResolvedDUTestSets_old",
//					"All2ResolvedDUTestSets_new",
					
					//Group 2
					"RandomTestSets",
					"AllPoliciesTestSets_old_random",
					"AllPoliciesTestSets_new_random",
					"All1ResolvedDUTestSets_old_random",
					"All1ResolvedDUTestSets_new_random",
					"All2ResolvedDUTestSets_old_random",
					"All2ResolvedDUTestSets_new_random",

					//Group 3
					"RandomTestSets",
					"AllPoliciesTestSets_old_criteria",
					"AllPoliciesTestSets_new_criteria",
					"All1ResolvedDUTestSets_old_criteria",
					"All1ResolvedDUTestSets_new_criteria",
					"All2ResolvedDUTestSets_old_criteria",
					"All2ResolvedDUTestSets_new_criteria",

					
					
			};
			
			HashMap criterion_sizePerformance = new HashMap();
//			srcDir += "/oldNewRegression/";
			String saveFile = srcDir +"/Size_Performance.txt";
			for(int i = 0; i < criteria.length; i ++){
				String criterion = criteria[i];
				containHeader = true;
				criterion_sizePerformance.put(criteria[i], ResultAnalyzer.getTSPerValidTestSet(srcDir, criterion, containHeader));
			}
			
			ResultAnalyzer.getCorrelationTSPeformance(criteria, criterion_sizePerformance, saveFile);
		}else if(instruction.equals("getCIPerformance")){
			//2009-03-10: get the correlations between CI of test sets and fault detection rate of each criterion
			criteria = new String[]{
					"AllPolicies",
					"All1ResolvedDU",
					"All2ResolvedDU",
			};
			
			//2009-02-25: to explore the CI distributions of different testing criteria
			HashMap criterion_CIs = new HashMap();
			for(int i =0; i < criteria.length; i ++){
				String pattern = criteria[i] + "\\_0\\.[0-9]+\\_0\\.[0-9]+" +"\\_CI.txt"; 
				criterion_CIs.put(criteria[i], ResultAnalyzer.getCriterialCI(srcDir, containHeader, criteria[i], pattern));
			}
			
			HashMap criterion_CIPerformance = new HashMap();
			String saveFile = srcDir + "/CI_Performance.txt";
			for(int i = 0; i < criteria.length; i++){
				String criterion = criteria[i];
				containHeader = true;
				criterion_CIPerformance.put(criterion, ResultAnalyzer.getCIPerValidTestSet(srcDir, criterion, containHeader));
			}
			ResultAnalyzer.getCorrelationCIPeformance(criteria, criterion_CIs, criterion_CIPerformance, saveFile);
		}else if(instruction.equals("mergeFiles")){
			containHeader = true;
			String prefix = args[2];
			String pattern = prefix + "\\_[0-9]+\\_[0-9]+\\.txt";
			String saveFile = srcDir + prefix + ".txt"; 
			ResultAnalyzer.mergeFiles(srcDir, containHeader, pattern, saveFile);
		}
	}
}
