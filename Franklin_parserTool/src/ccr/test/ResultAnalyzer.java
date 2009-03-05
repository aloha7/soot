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
						else if(CI < minCI)
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
		
		Logger.getInstance().setPath(saveFile, false);
		Logger.getInstance().write(sb.toString());
		Logger.getInstance().close();
		return fault_perValidTestSet;
	}
	
	public static void mergeHashMap(String[] criteria, HashMap criterion_Metric, String saveFile){
		StringBuilder sb = new StringBuilder();
		sb.append("Fault" + "\t" + "FailureRate" + "\t");
		
		for(int i =0; i < criteria.length; i ++){
			sb.append(criteria[i] + "\t");
		}
		sb.append("\n");
		
		//list all faults
		Iterator ite = ResultAnalyzer.failureRate.keySet().iterator();
		String criterion = null;
		while(ite.hasNext()){
			String fault = (String)ite.next();
			sb.append(fault + "\t" + ResultAnalyzer.failureRate.get(fault)+"\t");
			for(int i = 0; i < criteria.length; i++){
				criterion = criteria[i];
				HashMap metric = (HashMap)criterion_Metric.get(criterion);
				Object item = metric.get(fault);
				if(item == null){
					item = metric.get(Integer.parseInt(fault) + "");
				}
				sb.append(item+"\t");
			}
			sb.append("\n");
		}		
		
		Logger.getInstance().setPath(saveFile, false);
		Logger.getInstance().write(sb.toString());
		Logger.getInstance().close();
	}
	
	public static void main(String[] args){
		
//		String date = "20090216";
//		String date = "20090217";
//		String date= "20090219";
//		String date= "20090220";
		String date= "20090226";
//		String date= "20090222";
		String criterion = "allPolicies";
//		String criterion = "all1ResolvedDU";
//		String criterion = "all2ResolvedDU";
//		String criterion = "allFullResolvedDU";
		
		//2009-02-22:
//		String criterion = "allPolicies_old";
//		String criterion = "all1ResolvedDU_old";
//		String criterion = "all2ResolvedDU_old";
//		String criterion = "allFullResolvedDU_old";
		
		String testcaseFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"+date+"/TestPool.txt";
		
		//2009-02-18: load CI of each test case from a file
//		long startTime = System.currentTimeMillis();
		boolean containHeader = true;
		Adequacy.getTestPool(testcaseFile, containHeader);
		
		//2009-02-22: load failure rates from a file, this
		String failureRateFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"+date+"/failureRate.txt";
		ResultAnalyzer.loadFailureRate(failureRateFile, containHeader);
		
//		String detailFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/20090217/detailed.txt";
//		String saveFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/20090217/detailed_faults.txt";
//		ResultAnalyzer.translateFormat(detailFile, saveFile, containHeader);
//		System.out.println(System.currentTimeMillis() - startTime);

		//2009-02-19: get the failure rate of each fault
		String testDetailFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"+date+"/detailed.txt";
		String saveFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"+date+"/FailureRate_temp.txt";		
//		ResultAnalyzer.getFailureRate(testDetailFile, containHeader, saveFile);
		
		//2009-02-19: analysis faults number and faults type detected by an adequate test set
//		String testSetFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"+date+"/"+criterion +"TestSets.txt";
//		String testSetExecutionFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"+date+"/"+criterion+".txt";
//		boolean containHeader1 = false;
//		saveFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"+date+"/"+criterion+"_FaultType.txt";
//		boolean containHeader2 = true;

//		ResultAnalyzer.faultsExposedByTestSet(testSetFile, containHeader1, testSetExecutionFile, containHeader2, saveFile);
		
//		saveFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"+date+"/all1ResolvedDU_PerValidTestCase.txt";
//		ResultAnalyzer.validTestCaseWithinTestSet(testSetFile, containHeader1, testSetExecutionFile, containHeader2, saveFile);
		
		//2009-02-19: analysis faults number and faults type detected by an adequate test set
		String testSetExecutionFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"+date+"/"+criterion+".txt";
		boolean containHeader1 = false;
		saveFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"+date+"/"+criterion+"_FaultType.txt";
//		ResultAnalyzer.faultsExposedByTestSet(testSetExecutionFile, containHeader, saveFile);
		
		//2009-02-19: analysis percentage of fault-exposing test cases in a test set with respect to a fault
		saveFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"+date+"/"+criterion+"_perValidTestCase.txt";
//		ResultAnalyzer.perValidTestCaseWithinTestSet(testSetExecutionFile, containHeader, saveFile);
		
		//2009-02-22: analysis percentage of fault-exposing test sets with respect to each fault
		saveFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"+date+"/"+criterion+"_perValidTestSet.txt";
//		ResultAnalyzer.perValidTestSet(testSetExecutionFile, containHeader, saveFile);
		
		//2009-02-24: merge files
		String srcDir = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"+date+"/";
		
		String[] criteria = new String[]{
				"AllPoliciesTestSets_old","AllPoliciesTestSets_new",
				"All1ResolvedDUTestSets_old","All1ResolvedDUTestSets_new",
				"All2ResolvedDUTestSets_old","All2ResolvedDUTestSets_new",
				
				"AllPoliciesTestSets_old_random_58","AllPoliciesTestSets_new_random_58",
				"All1ResolvedDUTestSets_old_random_58","All1ResolvedDUTestSets_new_random_58",
				"All2ResolvedDUTestSets_old_random_58","All2ResolvedDUTestSets_new_random_58",

				"AllPoliciesTestSets_old_criteria_58","AllPoliciesTestSets_new_criteria_58",
				"All1ResolvedDUTestSets_old_criteria_58","All1ResolvedDUTestSets_new_criteria_58",
				"All2ResolvedDUTestSets_old_criteria_58","All2ResolvedDUTestSets_new_criteria_58",
				
				"RandomTestSets_21",
				"RandomTestSets_39",
				"RandomTestSets_47",
				"RandomTestSets_58",
				
		};
				
		HashMap criterion_faultNum = new HashMap();
		HashMap criterion_perValidTC = new HashMap();
		HashMap criterion_perValidTS = new HashMap();
		containHeader = true;
		for(int i = 0; i < criteria.length; i++){
			saveFile = srcDir + criteria[i] + "_testing.txt";
			ResultAnalyzer.mergeTestResultFiles(srcDir, criteria[i], containHeader, saveFile);
			
			testSetExecutionFile = saveFile;
			String faultTypeFile = saveFile.substring(0, saveFile.indexOf("_testing.txt")) + "_FaultType.txt";
			HashMap faultNum = ResultAnalyzer.faultsExposedByTestSet(testSetExecutionFile, containHeader1, faultTypeFile);
			criterion_faultNum.put(criteria[i], faultNum);
			
			String perValidTCFile = saveFile.substring(0, saveFile.indexOf("_testing.txt")) + "_PerValidTestCase.txt";
			HashMap perValidTC = ResultAnalyzer.perValidTestCaseWithinTestSet(testSetExecutionFile, containHeader1, perValidTCFile);
			criterion_perValidTC.put(criteria[i], perValidTC);
			
			String perValidTSFile = saveFile.substring(0, saveFile.indexOf("_testing.txt")) + "_PerValidTestSet.txt";
			HashMap perValidTS = ResultAnalyzer.perValidTestSet(testSetExecutionFile, containHeader1, perValidTSFile);
			criterion_perValidTS.put(criteria[i], perValidTS);
		}
		
		//2009-02-24: to summarize all three views to evaluate the performance of testing criteria
		saveFile = "C:/FaultNum.txt";
		ResultAnalyzer.mergeHashMap(criteria, criterion_faultNum, saveFile);
		saveFile = "C:/PerValidTC.txt";
		ResultAnalyzer.mergeHashMap(criteria, criterion_perValidTC, saveFile);
		saveFile = "C:/PerValidTS.txt";
		ResultAnalyzer.mergeHashMap(criteria, criterion_perValidTS, saveFile);
		
		//2009-02-25: to explore the CI distributions of different testing criteria
		StringBuilder sb = new StringBuilder();
		sb.append("criteria" + "\t" + "minCI" + "\t" + "meanCI" + "\t" + "maxCI" + "\t" + "stdCI" + "\n" );
		for(int i =0; i < criteria.length; i ++){
			sb.append(ResultAnalyzer.getCriteriaCI(srcDir, containHeader, criteria[i]));
		}
		saveFile = "C:/CI.txt";
		Logger.getInstance().setPath(saveFile, false);
		Logger.getInstance().write(sb.toString());
		Logger.getInstance().close();
		
	}
}
