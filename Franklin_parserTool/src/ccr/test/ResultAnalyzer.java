package ccr.test;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class ResultAnalyzer {
	public static final int TESTPOOL_SIZE = 5000;
	
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
	public static void perValidTestCaseWithinTestSet(String testSetExecutionFile, boolean containHeader, String saveFile){
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
			
			
			sb.append("Fault" + "\t" + "Avg.PerValidTestCases" + "\t" + "Std.PerValidTestCases" + "\n");
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
				sb.append(fault + "\t" + avg+"\t" + std + "\n");
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
		
	}
	
	/**2009-02-19:analysis valid test cases within a test set, percentage of validTestCase can be one dimension to compare two testing criteria
	 * 
	 * @param testSetFile: files storing adequate test sets
	 * @param containHeader1: whether the first row of testSetFile is a header or not
	 * @param testDetailFile:records of executions of test cases in the test pool
	 * @param containHeader2: whether the first row of detailFile is a header or not
	 */
	public static void validTestCaseWithinTestSet(String testSetFile, boolean containHeader1, String testDetailFile, boolean containHeader2,
			String saveFile){
		StringBuilder sb = new StringBuilder();
		sb.append("Fault" + "\t" + "PerValidTestCase" + "\n");
		//1.analyze the testSetFile to extract all test cases in a test set
		HashMap testSets = ResultAnalyzer.getTestCaseFromTestSet(testSetFile, containHeader1);//testSet-TestCases
		
		//2. analyze the testDetailFile to extract all faults a test case can expose
		HashMap faults = ResultAnalyzer.getFaultsWithTestCase(testDetailFile, containHeader2);
		
		//3. given a fault, analyze  percentage of valid test cases in a test set
		HashMap result = new HashMap();
	
		Iterator ite = faults.keySet().iterator();
		while(ite.hasNext()){ //for each fault
			String fault = (String)ite.next();
			ArrayList validTC = (ArrayList)faults.get(fault); //all valid test cases for this fault
			
			ArrayList perValidTC = new ArrayList();
			Iterator ite1 = testSets.keySet().iterator();
			double perValid = 0; //record the percentage of fault-exposing test cases in a test set
			while(ite1.hasNext()){ // for each test set
				String testSet = (String)ite1.next();
				ArrayList testCases = (ArrayList)testSets.get(testSet); 
				int valid = 0; //record the # of fault-exposing test cases in a test set 
				for(int i = 0; i < testCases.size(); i ++){ //
					String testCase = (String)testCases.get(i);
					if(validTC.contains(testCase))
						valid ++;
				}
				perValid = (double)valid/(double)testCases.size();
				perValidTC.add(perValid);	// percentage of valid test cases in a test set			
			}
			double sum = 0.0;
			for(int i =0 ; i < perValidTC.size(); i ++)
				sum += (Double)perValidTC.get(i);
			
			result.put(fault, sum/(double)perValidTC.size());			
		}
		
		Iterator ite2 = result.keySet().iterator();
		while(ite2.hasNext()){
			String fault = (String)ite2.next();
			double perValidTC = (Double)result.get(fault);
			sb.append(fault + "\t" + perValidTC + "\n");
		}
		
		Logger.getInstance().setPath(saveFile, false);
		Logger.getInstance().write(sb.toString());
		Logger.getInstance().close();
		
	}
	
	/**2009-02-19: analysis faults number and faults type detected by an adequate test set
	 * the result is a table with test set as rows and faults number and types it exposed as columns
	 * @param testSetExecutionFile
	 * @param containHeader
	 * @param saveFile
	 */
	public static void faultsExposedByTestSet(String testSetExecutionFile, boolean containHeader, String saveFile){
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
	}
	
	/**2009-02-19:analysis faults number and faults type detected by an adequate test set
	 * 
	 * @param testSetFile: files storing adequate test sets
	 * @param containHeader1: whether the first row of testSetFile is a header or not
	 * @param testDetailFile:records of executions of test cases in the test pool
	 * @param containHeader2: whether the first row of detailFile is a header or not
	 */
	public static void faultsExposedByTestSet(String testSetFile, boolean containHeader1, String testDetailFile, boolean containHeader2
			, String saveFile){
		
			StringBuilder sb = new StringBuilder();
			
			//1.analyze the testSetFile to extract all test cases in a test set
			HashMap testSets = ResultAnalyzer.getTestCaseFromTestSet(testSetFile, containHeader1);//testSet-TestCases
			
			
			//2. analyze the testDetailFile to extract all faults a test case can expose
			HashMap testCases = ResultAnalyzer.getTestCaseWithFaults(testDetailFile, containHeader2);
			
			//3. get all faults detected by a test set
			HashMap perform_TS = new HashMap(); //testSet--all faults detected by it
			Iterator ite = testSets.keySet().iterator();
			while(ite.hasNext()){
				String testSet = (String)ite.next();
				ArrayList tcs = (ArrayList)testSets.get(testSet);
				
				ArrayList faults;
				if(perform_TS.containsKey(testSet)){
					faults = (ArrayList)perform_TS.get(testSet);
				}else{
					faults = new ArrayList();
				}
				
				for(int i = 0; i < tcs.size(); i++){
					String tc = (String)tcs.get(i);
					ArrayList fault = (ArrayList)testCases.get(tc);
					if(fault!=null){//it is possible that a criteria select a test case which cannot expose any faults
						for(int j = 0; j < fault.size(); j++){
							faults.add((String)fault.get(j));
						}	
					}
					
				}
				perform_TS.put(testSet, faults);
				
			}
			
			//4. derive the performance of adequate test set in terms of types and numbers of faults it exposed
			HashMap perform_TSs = new HashMap();
			
			Iterator ite1 = perform_TS.keySet().iterator();
			while(ite1.hasNext()){
				String testSet = (String)ite1.next();
				ArrayList faults = (ArrayList)perform_TS.get(testSet);
				int num = faults.size();
				
				ArrayList temp = new ArrayList(); // keep different faults detected by a test set
				for(int i = 0; i < faults.size(); i ++){
					String fault = (String)faults.get(i);
					if(!temp.contains(fault)){
						temp.add(fault);
					}
				}
				int type = temp.size();
				perform_TSs.put(testSet, num+"\t"+type);
			}
			
			//5.each row of the result is "fault + fault number + fault type"
			Iterator ite2 = perform_TSs.keySet().iterator();
			sb.append("TestSet" +"\t"+ "num" + "\t" + "type" + "\n");
			while(ite2.hasNext()){
				String testSet = (String)ite2.next();
				sb.append(testSet + "\t" + (String)perform_TSs.get(testSet) + "\n");
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
		String date= "20090221";
		String criterion = "allPolicies";
//		String criterion = "all1ResolvedDU";
//		String criterion = "all2ResolvedDU";
//		String criterion = "allFullResolvedDU";
		
		String testcaseFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"+date+"/TestPool.txt";
		
		//2009-02-18: analysis CI distributions of validTestCase for each faulty version
//		long startTime = System.currentTimeMillis();
		boolean containHeader = true;
		Adequacy.getTestPool(testcaseFile, containHeader);
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
		
		ResultAnalyzer.faultsExposedByTestSet(testSetExecutionFile, containHeader, saveFile);
		
		//2009-02-19: analysis percentage of fault-exposing test cases in a test set with respect to a fault
		saveFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"+date+"/"+criterion+"_perValidTestCase.txt";
		ResultAnalyzer.perValidTestCaseWithinTestSet(testSetExecutionFile, containHeader, saveFile);
		
	}
}
