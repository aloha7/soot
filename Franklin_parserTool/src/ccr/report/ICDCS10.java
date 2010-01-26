package ccr.report;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;

import polyglot.ast.Do;

import ccr.app.Application;
import ccr.test.Adequacy;
import ccr.test.Logger;
import ccr.test.TestCase;
import ccr.test.TestSet;
import ccr.test.TestSetManager;

public class ICDCS10 {
	
	/**2010-01-14: load test sets from existing files
	 * 
	 * @param date
	 * @param criterion
	 * @param testSuiteSize
	 * @param oldOrNew
	 * @param randomOrCriterion
	 * @param H_L_R
	 * @param size_ART
	 * @param containHeader
	 * @return
	 */
	private ArrayList<TestSet> loadTestSet_offline(String date, String criterion, int testSuiteSize, 
			String oldOrNew, String randomOrCriterion, String H_L_R, int size_ART, boolean containHeader){
		
		ArrayList<TestSet> testSets = new ArrayList<TestSet>();		
		String testSetFile = null;
		
		if (testSuiteSize < 0) {
			if (oldOrNew.equals("old")) {// Conventional test suite
											// construction algorithm
				testSetFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
						+ date + "/" + size_ART + "/" // 2009-10-29:we
														// change the saving
														// folder here
						+ criterion + "_CA_" + size_ART + ".txt";

			} else if (oldOrNew.equals("new")) {
				if (H_L_R.equals("H")) { // Refined test suite
											// construction algorithm with
											// high context diversity
					testSetFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
							+ date + "/" + size_ART + "/"// 2009-10-29:we
															// change the
															// saving folder
															// here
							+ criterion + "_RA-H_" + size_ART + ".txt";
				} else if (H_L_R.equals("L")) {// Refined test suite
												// construction algorithm
												// with low context
												// diversity
					testSetFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
							+ date + "/" + size_ART + "/"// 2009-10-29:we
															// change the
															// saving folder
															// here
							+ criterion + "_RA-L_" + size_ART + ".txt";
				} else if (H_L_R.equals("R")) {// Refined test suite
												// construction algorithm
												// with random context
												// diversity
					testSetFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
							+ date + "/" + size_ART + "/"// 2009-10-29:we
															// change the
															// saving folder
															// here
							+ criterion + "_RA-R_" + size_ART + ".txt";
				}
			}
		} else {
			if (oldOrNew.equals("old")) {
				testSetFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
						+ date
						+ "/"
						+ size_ART
						+ "/"// 2009-10-29:we change the saving folder
								// here
						+ criterion
						+ "_CA_"
						+ randomOrCriterion
						+ "_"
						+ testSuiteSize + "_" + size_ART + ".txt"; // 							
			} else if (oldOrNew.equals("new")) {
				if (H_L_R.equals("H")) {
					testSetFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
							+ date
							+ "/"
							+ size_ART
							+ "/"// 2009-10-29:we change the saving
									// folder here
							+ criterion
							+ "_RA-H_"
							+ randomOrCriterion
							+ "_" + testSuiteSize + "_" + size_ART + ".txt";
				} else if (H_L_R.equals("L")) {
					testSetFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
							+ date
							+ "/"
							+ size_ART
							+ "/"// 2009-10-29:we change the saving
									// folder here
							+ criterion
							+ "_RA-L_"
							+ randomOrCriterion
							+ "_" + testSuiteSize + "_" + size_ART + ".txt";
				} else if (H_L_R.equals("R")) {
					testSetFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
							+ date
							+ "/"
							+ size_ART
							+ "/"// 2009-10-29:we change the saving
									// folder here
							+ criterion
							+ "_RA-R_"
							+ randomOrCriterion
							+ "_" + testSuiteSize + "_" + size_ART + ".txt";
				}
			}
		}
		
		File tmp = new File(testSetFile);
		if(tmp.exists()){
			try {
				BufferedReader br = new BufferedReader(new FileReader(testSetFile));
				if(containHeader)
					br.readLine();
				
				String str = null;
				while((str = br.readLine())!= null){
					String[] strs = str.split("\t");
					if(strs.length > 9){
						TestSet ts = new TestSet();
						ts.index = strs[0];
						ts.coverage = Double.parseDouble(strs[4]);
						ts.geneTime = Long.parseLong(strs[5].substring(
								strs[5].indexOf("Time:") + "Time:".length()));
						
						int i = strs[6].indexOf(Application.SET_PREFIX);
						int j = strs[6].indexOf(Application.SET_POSTFIX);
						String testCases = strs[6].substring(i + Application.SET_PREFIX.length(), j);
						StringTokenizer st = new StringTokenizer(testCases, Application.SET_DELIMITER);					
						while(st.hasMoreTokens()){
							ts.testcases.add(st.nextToken());
						}
						ts.replaceCounter = Integer.parseInt(strs[8]);
						ts.tie_activation_CI = Integer.parseInt(strs[9].substring(strs[9].indexOf("Tie_activation_CI:") 
								+ "Tie_activation_CI:".length()));
						
						
						testSets.add(ts);	
					}
					
				}
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}else{
			System.out.println("Test set file does not exist:" + tmp.getAbsolutePath());
		}
		
		return testSets;
	}
	
	private HashMap<String, Integer> getTestPool(String date,
			boolean containHeader, double alpha){
		
		HashMap<String, Integer> tc_CD = new HashMap<String, Integer>();
		String testPoolFile =  "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
			+ date + "/TestPool_Alpha/TestPool_"+ new DecimalFormat("0.0000").format(alpha) + ".txt";
		File tmp = new File(testPoolFile);
		if(tmp.exists()){
			try {
				BufferedReader br = new BufferedReader(new FileReader(testPoolFile));
				if(containHeader)
					br.readLine();
				
				String str = null;
				while((str = br.readLine())!= null){
					String[] strs = str.split("\t");
					if(strs.length >= 3){
						String tc = strs[0];						
						int CD = (int)Double.parseDouble(strs[2]);
						tc_CD.put(tc, CD);
					}					
				}
			} catch (NumberFormatException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}else{
			System.out.println("Test pool file does not exist:" + tmp.getAbsolutePath());
		}
		
		return tc_CD;
	}
	
	private void saveTestSet_CD(ArrayList<TestSet> testSets, 
			HashMap<String, Integer> tc_CD, String saveFile){
		StringBuilder sb = new StringBuilder();
		sb.append("TestSet" + "\t" + "CD" + "\n");
		
		double sum_CD = 0.0;
		double min_CD = Double.MAX_VALUE;
		double max_CD = Double.MIN_VALUE;
		
		for (int i = 0; i < testSets.size(); i++) {
			TestSet ts = testSets.get(i);
			double CD = getAverageCD(ts, tc_CD);
			sum_CD += CD;
			if(CD > max_CD){
				max_CD = CD;
			}else if(CD < min_CD){
				min_CD = CD;
			}
			
			sb.append(ts.index + "\t" + CD + "\n");
		}
		
		double avg_CD = sum_CD/testSets.size();
		double tmp = 0.0;
		for(int i = 0; i < testSets.size(); i ++){
			TestSet ts = testSets.get(i);
			double CD = getAverageCD(ts, tc_CD);
			tmp += (CD - avg_CD)*(CD - avg_CD);
		}
		double std_CD = Math.sqrt(tmp/testSets.size());
		
		sb.append("Min\t").append("Mean\t").append("Max\t").append("Std\n");
		sb.append(min_CD).append("\t").append(avg_CD).append("\t").append(max_CD).
		append("\t").append(std_CD).append("\n");
		
		Logger.getInstance().setPath(saveFile, false);
		Logger.getInstance().write(sb.toString());
		Logger.getInstance().close();
	}
	
	private double getAverageCD(TestSet ts, HashMap<String, Integer> tc_CD){
		double avg_CD = 0.0;
		double sum_CD = 0.0;
		for(int i = 0; i < ts.testcases.size(); i ++){
			String index_testcase = (String)ts.testcases.get(i);
			sum_CD += tc_CD.get(index_testcase);
		}
		avg_CD = sum_CD/(double)ts.testcases.size();
		return avg_CD;
	}
	
	/**2010-01-22: get the context diversity distribution of each adequate 
	 * test sets
	 * @param date_testSets
	 * @param criterion
	 * @param testSuiteSize
	 * @param oldOrNew
	 * @param randomOrCriterion: specify the way to supplement the adequate test sets whose sizes do not reach to the upper bound
	 * @param H_L_R
	 * @param size_ART
	 * @param date_testPool
	 */
	public void getTestSet_CD(String date_testSets, String criterion, int testSuiteSize, 
			String oldOrNew, String randomOrCriterion, String H_L_R, int size_ART, 
			String date_testPool){
		//1.load the test sets
		boolean containHeader = false;
		ArrayList<TestSet> testSets = this.loadTestSet_offline(
				date_testSets, criterion, testSuiteSize, oldOrNew, 
				randomOrCriterion, H_L_R, size_ART, containHeader);
		
		//2.load the test pool					
		double alpha = 0.43;
		String testPoolFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
				+ date_testPool + "/TestPool_Alpha/TestPool_"
				+new DecimalFormat("0.0000").format(alpha)+".txt";
		containHeader = true;
		
		HashMap<String, Integer>  tc_CD= this.getTestPool(date_testPool, containHeader, alpha);
		String saveFile = null;
		if(oldOrNew.equals("new")){
			saveFile =  "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
				+ date_testPool + "/TestSet/" + criterion + "_RA-" + H_L_R + "_"  
				 + size_ART + "_CD_" + alpha + ".txt";	
		}else if(oldOrNew.equals("old")){
			saveFile =  "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
				+ date_testPool + "/TestSet/" + criterion + "_CA_"  
				 + size_ART + "_CD_" + alpha + ".txt";	
		}
		
		//3. get the context diversity distribution of adequate test sets  
		this.saveTestSet_CD(testSets, tc_CD, saveFile);
	}
	
	
	public static void main(String[] args) {
//		String instruction  = args[0];
		String instruction  = "getTestSet_CD";
		
		ICDCS10 ins = new ICDCS10();
		if(instruction.equals("getTestSet_CD")){
//			String date_testSets = args[1];
//			String criterion = args[2];
//			int testSuiteSize = Integer.parseInt(args[3]);
//			String oldOrNew = args[4];
//			String randomOrCriterion = args[5];
//			String H_L_R = args[6];
//			int size_ART = Integer.parseInt(args[7]);
//			String date_testPool = args[8];
			
			String date_testSets = "20091026";
			String criterion = "AllPolicies"; //AllPolicies, All1ResolvedDU, All2ResolvedDU
			int testSuiteSize = -1;
			String oldOrNew = "new";
			String randomOrCriterion = "random";
			String H_L_R = "L";
			int size_ART = 64;
			String date_testPool = "20091026";
			ins.getTestSet_CD(date_testSets, criterion, testSuiteSize, oldOrNew, 
					randomOrCriterion, H_L_R, size_ART, date_testPool);
		}else if(instruction.equals("getTestSets_CD")){
			
		}

	}

}
