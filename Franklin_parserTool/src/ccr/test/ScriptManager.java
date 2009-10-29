package ccr.test;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class ScriptManager {
	
	public static void save(String str, String saveFile){
		Logger.getInstance().setPath(saveFile, false);
		Logger.getInstance().write(str);
		Logger.getInstance().close();	
	}
	
	
	public static String exeRandomTS_Script(int min_TestSuiteSize, int max_TestSuiteSize, String instruction,
			String date){
//		int interval = 18;
//		int sleep_sed = 60; // sleep time in terms of seconds
//		int incremental = 30;
//		int total = 0;
		
		StringBuilder sb = new StringBuilder();
		for(int testSuiteSize = min_TestSuiteSize; testSuiteSize < max_TestSuiteSize; testSuiteSize ++){
			sb.append("java ccr.test.ExecutionManager "+ instruction +" " +testSuiteSize + " " + date + " \n");
//			if (testSuiteSize % interval == 0) { // sleep a fixed time if a specified
//				// interval is reached
//				sb.append("sleep " + sleep_sed + "\n");
//				total += sleep_sed;
//				sleep_sed += incremental;
//				
//			}
		}	
		
//		System.out.println("Time required:" + (double)total/(double)60 + " minutes");
		return sb.toString();
	}
	
	public static String exeRandomTS_Script(int min_TestSuiteSize, int max_TestSuiteSize,
			String date){
//		int interval = 18;
//		int sleep_sed = 60; // sleep time in terms of seconds
//		int incremental = 30;
//		int total = 0;
		
		StringBuilder sb = new StringBuilder();
		for(int testSuiteSize = min_TestSuiteSize; testSuiteSize < max_TestSuiteSize; testSuiteSize ++){
			sb.append("java -Xmx1500m ccr.test.ExecutionManager Load "+ testSuiteSize + " " + date + " \n");
//			if (testSuiteSize % interval == 0) { // sleep a fixed time if a specified
//				// interval is reached
//				sb.append("sleep " + sleep_sed + "\n");
//				total += sleep_sed;
//				sleep_sed += incremental;
//				
//			}
		}	
		
//		System.out.println("Time required:" + (double)total/(double)60 + " minutes");
		return sb.toString();
	}
	
	/**2009-09-19: run CA, RA-H, RA-L, RA-R test suites
	 * 
	 * @param testSetNum
	 * @param date
	 * @return
	 */
	public static String exeAdequateTS_Script(int testSetNum, String date){

		int interval = 18;
		int sleep_sed = 180; // sleep time in terms of seconds(60, 5 for [1, 130]->118min, 180, 10 for[130, 200]->112min)
		int incremental = 10;
		int counter = 0; 
		int total = 0;
		
		String[] criteria = new String[] { 
				"AllPolicies",
				"All1ResolvedDU",
				"All2ResolvedDU",
		};
		
		//Load 20090918 AllPolicies -1 new random H
		StringBuilder sb = new StringBuilder();
		for(int j = 0; j < criteria.length; j ++){			
			//for CA
			sb.append("java -Xmx1500m ccr.test.TestingEffectiveManager " 
					+ "Load " + date + " " + criteria[j]
					+ " -1 " + "old " + "random " + "R " + "\n");			
			//for RA_H
			sb.append("java -Xmx1500m ccr.test.TestingEffectiveManager " 
					+ "Load " + date + " " + criteria[j]
					+ " -1 " + "new " + "random " + "H " + "\n");
			//for RA_L
			sb.append("java -Xmx1500m ccr.test.TestingEffectiveManager " 
					+ "Load " + date + " " + criteria[j]
					+ " -1 " + "new " + "random " + "L " + "\n");
			//for RA_R
			sb.append("java -Xmx1500m ccr.test.TestingEffectiveManager " 
					+ "Load " + date + " " + criteria[j]
					+ " -1 " + "new " + "random " + "R " + "\n");			
		}
		
		return sb.toString();
	}
	
	/**2009-10-13: add another parameter: size_ART
	 * 
	 * @param testSetNum
	 * @param date
	 * @param size_ART: the size of Adaptive Random Testing_generated test sets
	 * @return
	 */
	public static String exeAdequateTS_Script(int testSetNum, String date, String size_ART){

		String[] criteria = new String[] { 
				"AllPolicies",
				"All1ResolvedDU",
				"All2ResolvedDU",
		};
		
		//Load 20090918 AllPolicies -1 new random H
		StringBuilder sb = new StringBuilder();
		for(int j = 0; j < criteria.length; j ++){			
			//for CA
			sb.append("java ccr.test.TestingEffectiveManager " 
					+ "Load_large " + date + " " + criteria[j]
					+ " -1 " + "old " + "random " + "R " + size_ART + "&\n");			
			//for RA_H
			sb.append("java ccr.test.TestingEffectiveManager " 
					+ "Load_large " + date + " " + criteria[j]
					+ " -1 " + "new " + "random " + "H " + size_ART + "&\n");
			//for RA_L
			sb.append("java ccr.test.TestingEffectiveManager " 
					+ "Load_large " + date + " " + criteria[j]
					+ " -1 " + "new " + "random " + "L "  + size_ART+ "&\n");
			//for RA_R
			sb.append("java ccr.test.TestingEffectiveManager " 
					+ "Load_large " + date + " " + criteria[j]
					+ " -1 " + "new " + "random " + "R " + size_ART + "&\n");			
		}
		
		return sb.toString();
	}
	
	public static String exeAdequateTS_Script(int testSetNum, String instruction, double min_CI,
			double max_CI, String date, int min_TestSuiteSize, int max_TestSuiteSize){

		int interval = 18;
		int sleep_sed = 180; // sleep time in terms of seconds(60, 5 for [1, 130]->118min, 180, 10 for[130, 200]->112min)
		int incremental = 10;
		int counter = 0; 
		int total = 0;
		
		String[] criteria = new String[] { 
				"AllPolicies_new_random",

				"All1ResolvedDU_new_random",
				
				"All2ResolvedDU_new_random",
				
				"AllPolicies_old_random",  
				
				"All1ResolvedDU_old_random",  
				
				"All2ResolvedDU_old_random", 

				"AllPolicies_new_criteria",
				
				"All1ResolvedDU_new_criteria",
				
				"All2ResolvedDU_new_criteria",
				
				"AllPolicies_old_criteria", 
				
				"All1ResolvedDU_old_criteria", 
				
				"All2ResolvedDU_old_criteria", 
		};
		StringBuilder sb = new StringBuilder();
		for(int testSuiteSize = min_TestSuiteSize; testSuiteSize < max_TestSuiteSize; testSuiteSize ++){
			for(int i = 0; i < criteria.length; i ++){
				String[] strs = criteria[i].split("_");

				String criterion = strs[0];// AllPolicies, All1ResolvedDU, All2ResolvedDU
				String oldOrNew = strs[1];
				String randomOrCriterion = strs[2];
				
				// 2009-03-07: have a parameter: sleep time to fix, thus it is not a good idea
//				sb.append("java ccr.test.ExecutionManager "+ testSetNum + " Load " 
//						+ min_CI + " " + max_CI + " " + date + " " + criterion + " " + testSuiteSize
//						+" " + oldOrNew + " " + randomOrCriterion +" &\n");
//				
//				counter ++;
//				if (counter % interval == 0) { // sleep a fixed time when reaching a interval
//					// interval is reached
//					sb.append("sleep " + sleep_sed + "\n");
//					total += sleep_sed;
//					sleep_sed += incremental;
//					
//				}
				
				//2009-03-07: have no parameters to fix, but we need 20 shells to run at the same time
				//and for each shell, all commands should execute sequentially.
				sb.append("java -Xmx1500m ccr.test.ExecutionManager "+ testSetNum + " " + instruction + " " 
						+ min_CI + " " + max_CI + " " + date + " " + criterion + " " + testSuiteSize
						+" " + oldOrNew + " " + randomOrCriterion +" \n");
			}
		}
		
//		System.out.println("Time required:" + (double)total/(double)60 + " minutes");
	  return sb.toString();
	}
	
	public static String exeAdequateTS_withoutFixSize_Script(int testSetNum, double min_CI,
			double max_CI, String instruction, String date ){

		String[] criteria = new String[] { 
				"AllPolicies_new",

				"All1ResolvedDU_new",

				"All2ResolvedDU_new",
				
				"AllPolicies_old",

				"All1ResolvedDU_old",

				"All2ResolvedDU_old",
		};

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < criteria.length; i++) {
			String[] strs = criteria[i].split("_");

			String criterion = strs[0];// AllPolicies, All1ResolvedDU,
										// All2ResolvedDU
			String oldOrNew = strs[1];
		

			sb.append("java ccr.test.ExecutionManager " + testSetNum + " " + instruction + " "
					+ min_CI + " " + max_CI + " " + date + " " + criterion
					+ " -1 " + oldOrNew + " random"
					+ " \n");
		}

		return sb.toString();
	}
	
	public static String exeAdequateTS_withoutFixSize_Script(int testSetNum, double min_CI,
			double max_CI, String date ){

		String[] criteria = new String[] { 
				"AllPolicies_new",

				"All1ResolvedDU_new",

				"All2ResolvedDU_new",
				
				"AllPolicies_old",

				"All1ResolvedDU_old",

				"All2ResolvedDU_old",
		};

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < criteria.length; i++) {
			String[] strs = criteria[i].split("_");

			String criterion = strs[0];// AllPolicies, All1ResolvedDU,
										// All2ResolvedDU
			String oldOrNew = strs[1];
		

			sb.append("java ccr.test.ExecutionManager " + testSetNum + " Load "
					+ min_CI + " " + max_CI + " " + date + " " + criterion
					+ " -1 " + oldOrNew + " random"
					+ " \n");
		}

		return sb.toString();
	}
	
	public static String exeTestSet(String criterionString, int testSetNum, String instruction, double min_CI,
			double max_CI, String date, int testSuiteSize){
		StringBuffer sb = new StringBuffer();
		
		String[] strs = criterionString.split("_");

		String criterion = strs[0];
		if(criterion.equals("RandomTestSets")){
			sb.append("java -Xmx1500m ccr.test.ExecutionManager "+ instruction +" " 
					+testSuiteSize + " " + date + " \n");			
		}else{
			criterion = strs[0].substring(0, strs[0].indexOf("TestSets"));
			String oldOrNew = strs[1];
			String randomOrCriterion = strs[2];
			
			sb.append("java -Xmx1500m ccr.test.ExecutionManager "+ testSetNum + " " + instruction + " " 
					+ min_CI + " " + max_CI + " " + date + " " + criterion + " " + testSuiteSize
					+" " + oldOrNew + " " + randomOrCriterion +" \n");	
		}
		
		
		
		return sb.toString();
	}
	
	public static String genTestSet(String criterionString, int testSetNum, double min_CI,
			double max_CI, String date, int testSuiteSize) {

		StringBuilder sb = new StringBuilder();
		
		String[] strs = criterionString.split("_");
		String criterion = strs[0];
		if(criterion.equals("RandomTestSets")){
			sb.append("java ccr.test.TestSetManager " + testSetNum + " " + testSuiteSize
					+ " " + date + " \n");
		}else{
			criterion = strs[0].substring(0, strs[0].indexOf("TestSets"));
			String oldOrNew = strs[1];
			String randomOrCriterion = strs[2];
			sb.append("java ccr.test.TestSetManager " + testSetNum
					+ " Context_Intensity " + min_CI + " " + max_CI + " "
					+ date + " " + criterion + " " + testSuiteSize + " " + oldOrNew
					+ " " + randomOrCriterion + " \n");
		}
		
		return sb.toString();
		
	}


		
		
	
	/**
	 * To generate random test sets with fixed size
	 * 
	 * @param testSetNum
	 * @param min_TestSuiteSize
	 * @param max_TestSuiteSize
	 * @param date
	 * @param saveFile
	 */
	public static String genRandomTS_Script(int testSetNum,
			int min_TestSuiteSize, int max_TestSuiteSize, String date) {
//		int interval = 18;
//		int sleep_sed = 10; // sleep time in terms of seconds
//		int incremental = 5;
//		int total = 0;
		StringBuilder sb = new StringBuilder();
		
		for (int i = min_TestSuiteSize; i < max_TestSuiteSize; i++) {
			sb.append("java ccr.test.TestSetManager " + testSetNum + " " + i
					+ " " + date + " &\n");
//			if (i % interval == 0) { // sleep a fixed time if a specified
//				// interval is reached
//				sb.append("sleep " + sleep_sed + "\n");
//				total += sleep_sed;
//				sleep_sed += incremental;
//				
//			}
		}
//		System.out.println("Time required:" + (double)total/(double)60 + " minutes");
		return sb.toString();
	}

	/**2009-09-19: using CA, RA_H, RA_L, RA_R to construct adequate test suites
	 * 
	 * @param testSetNum
	 * @param date
	 * @return
	 */
	public static String genAdequateTS_Script(int testSetNum, String date){
		StringBuilder sb = new StringBuilder();
		
		//5 Context_Intensity 0.7 0.9 20090918 AllPolicies -1 new random R
		String[] criteria = new String[] { 
				"AllPolicies",
				"All1ResolvedDU",
				"All2ResolvedDU",
		};
		
		for(int j = 0; j < criteria.length; j ++){			
			//for CA
			sb.append("java ccr.test.TestSetManager " + testSetNum 
					+ " Context_Intensity 0.7 0.9 " + date + " " + criteria[j]
					+ " -1 " + "old " + "random " + "R " + "&\n");			
			//for RA_H
			sb.append("java ccr.test.TestSetManager " + testSetNum 
					+ " Context_Intensity 0.7 0.9 " + date + " " + criteria[j]
					+ " -1 " + "new " + "random " + "H " + "&\n");
			//for RA_L
			sb.append("java ccr.test.TestSetManager " + testSetNum 
					+ " Context_Intensity 0.7 0.9 " + date + " " + criteria[j]
					+ " -1 " + "new " + "random " + "L " + "&\n");
			//for RA_R
			sb.append("java ccr.test.TestSetManager " + testSetNum 
					+ " Context_Intensity 0.7 0.9 " + date + " " + criteria[j]
					+ " -1 " + "new " + "random " + "R " + "&\n");			
		}
		
		return sb.toString();
	}
	
	/**2009-10-13: using CA, RA_H, RA_L, RA_R to construct adequate test suites
	 * 
	 * @param testSetNum
	 * @param date
	 * @param size_ART: the size of Adaptive Random Testing_generated test set
	 * @return
	 */
	public static String genAdequateTS_Script(int testSetNum, String date, String size_ART){
		StringBuilder sb = new StringBuilder();
		
		//5 Context_Intensity 0.7 0.9 20090918 AllPolicies -1 new random R
		String[] criteria = new String[] { 
				"AllPolicies",
				"All1ResolvedDU",
				"All2ResolvedDU",
		};
		
		for(int j = 0; j < criteria.length; j ++){			
			//for CA
			sb.append("java ccr.test.TestSetManager " + testSetNum 
					+ " Context_Intensity 0.7 0.9 " + date + " " + criteria[j]
					+ " -1 " + "old " + "random " + "R " + size_ART + "&\n");			
			//for RA_H
			sb.append("java ccr.test.TestSetManager " + testSetNum 
					+ " Context_Intensity 0.7 0.9 " + date + " " + criteria[j]
					+ " -1 " + "new " + "random " + "H " + size_ART + "&\n");
			//for RA_L
			sb.append("java ccr.test.TestSetManager " + testSetNum 
					+ " Context_Intensity 0.7 0.9 " + date + " " + criteria[j]
					+ " -1 " + "new " + "random " + "L " + size_ART + "&\n");
			//for RA_R
			sb.append("java ccr.test.TestSetManager " + testSetNum 
					+ " Context_Intensity 0.7 0.9 " + date + " " + criteria[j]
					+ " -1 " + "new " + "random " + "R " + size_ART + "&\n");			
		}
		
		return sb.toString();
	}
	/**
	 * To generate adequate test sets with fixed size
	 * 
	 * @param testSetNum
	 * @param instruction
	 * @param min_CI
	 * @param max_CI
	 * @param date
	 * @param min_TestSuiteSize
	 * @param max_TestSuiteSize
	 * @param saveFile
	 */
	public static String genAdequateTS_Script(int testSetNum, double min_CI,
			double max_CI, String date, int min_TestSuiteSize,
			int max_TestSuiteSize) {

		int interval = 24;
		int sleep_sed = 90; // sleep time in terms of seconds(90 , 10 for [1, 130]->130min, 360, 10 for [130, 200]->124 mins)
		int incremental = 10;
		int counter = 0; 
		int total = 0;
		
		String[] criteria = new String[] { 
				"AllPolicies_new_random",
				
				"All1ResolvedDU_new_random",
				
				"All2ResolvedDU_new_random",
				
				"AllPolicies_old_random",  
				
				"All1ResolvedDU_old_random",  
				
				"All2ResolvedDU_old_random", 

				"AllPolicies_new_criteria",
				
				"All1ResolvedDU_new_criteria",
				
				"All2ResolvedDU_new_criteria",
				
				"AllPolicies_old_criteria", 
				
				"All1ResolvedDU_old_criteria", 
				
				"All2ResolvedDU_old_criteria", 
		};
		
		StringBuilder sb = new StringBuilder();
		for (int i = min_TestSuiteSize; i < max_TestSuiteSize; i++) {
			for (int j = 0; j < criteria.length; j++) {
				String[] strs = criteria[j].split("_");

				String criterion = strs[0];// AllPolicies, All1ResolvedDU,
				// All2ResolvedDU
				String oldOrNew = strs[1];
				String randomOrCriterion = strs[2];
				
				// 2009-03-07: have a parameter: sleep time to fix, thus it is not a good idea 
//				sb.append("java ccr.test.TestSetManager " + testSetNum
//						+ " Context_Intensity " + min_CI + " " + max_CI + " "
//						+ date + " " + criterion + " " + i + " " + oldOrNew
//						+ " " + randomOrCriterion + " &\n");
//				
//				counter ++;
//				if (counter % interval == 0) { // sleep a fixed time when reaching a interval
//					// interval is reached
//					sb.append("sleep " + sleep_sed + "\n");
//					total += sleep_sed;
//					sleep_sed += incremental;
//					
//				}
				
				//2009-03-07: have no parameters to fix, but we need 20 shells to run at the same time
				//and for each shell, all commands should execute sequentially.
				sb.append("java ccr.test.TestSetManager " + testSetNum
						+ " Context_Intensity " + min_CI + " " + max_CI + " "
						+ date + " " + criterion + " " + i + " " + oldOrNew
						+ " " + randomOrCriterion + " &\n");
				
			}
		}
//		System.out.println("Time required:" + (double)total/(double)60 + " minutes");
		return sb.toString();
	}
	
	
	/**
	 * To generate adequate test sets without fixed size
	 * 
	 * @param testSetNum
	 * @param instruction
	 * @param min_CI
	 * @param max_CI
	 * @param date
	 * @param min_TestSuiteSize
	 * @param max_TestSuiteSize
	 * @param saveFile
	 */
	public static String genAdequateTS_WithoutFixSize_Script(int testSetNum, double min_CI,
			double max_CI, String date) {
		
		String[] criteria = new String[] {
				"AllPolicies_new",

				"All1ResolvedDU_new",

				"All2ResolvedDU_new", 
				
				"AllPolicies_old",

				"All1ResolvedDU_old",

				"All2ResolvedDU_old",
		};

		StringBuilder sb = new StringBuilder();

		for (int j = 0; j < criteria.length; j++) {
			String[] strs = criteria[j].split("_");

			String criterion = strs[0];// AllPolicies, All1ResolvedDU, All2ResolvedDU

			String oldOrNew = strs[1];
			

			sb.append("java ccr.test.TestSetManager " + testSetNum
					+ " Context_Intensity " + min_CI + " " + max_CI + " "
					+ date + " " + criterion + " -1 " + oldOrNew + " random"
					 + " \n");

		}
		return sb.toString();
	}

	public static void genFailureRate_Sequential(String date, String saveFile, 
			ArrayList faultList, int startVersion, int endVersion){
		StringBuilder sb = new StringBuilder();
		for(int i = startVersion; i < endVersion-1; i ++){
			sb.append("java ccr.test.TestDriver getFailureRate " + date + " " + faultList.get(i) + " "
					+ faultList.get(i+1) + " a\n");
		}
		
		if(startVersion == (endVersion - 1)){ //the last element in the faultList
			int version = (Integer)faultList.get(startVersion);
			sb.append("java ccr.test.TestDriver getFailureRate " + date + " " + version + " "
					+ (version + 1) + " a\n");
		}
		
		Logger.getInstance().setPath(saveFile, false);
		Logger.getInstance().write(sb.toString());
		Logger.getInstance().close();
	}
	
	/**2009-03-17: we must execute these faulty version one by one since some faults may 
	 * cause program crashed
	 * @param date
	 * @param saveFile
	 * @param startVersion
	 * @param endVersion
	 */
	public static void genFailureRate_Sequential(String date, String saveFile, 
			int startVersion, int endVersion){
		StringBuilder sb = new StringBuilder();
		for(int i = startVersion; i < endVersion; i ++){
			sb.append("java ccr.test.TestDriver getFailureRate " + date + " " + i + " "
					+ (i+1) + "\n");
		}
		
		Logger.getInstance().setPath(saveFile, false);
		Logger.getInstance().write(sb.toString());
		Logger.getInstance().close();
	}
	
	public static void genFailureRate_TS(int startVersion, int endVersion, int concurrentNumber, String date, String saveFile){
		int start = startVersion;
		int end = endVersion; //[140, 2600][2600, 5024]
		int concurrent = concurrentNumber;
		
		int interval = (end - start + 1)/concurrent ;
		if( (end - start + 1) > interval * concurrent )
			interval ++;
		
		
//		String instruction = argv[0];
//		String date = argv[1];
//		int startVersion = Integer.parseInt(argv[2]);
//		int endVersion = Integer.parseInt(argv[3]);
//		getFailureRate
		StringBuilder sb = new StringBuilder();
		
		for(int i = start; i < end; i = i + interval){
			int min = i;
			int max = i + interval;
			if(max >= end)
				max = end;
			
			sb.append("java ccr.test.TestDriver getFailureRate " +date + " "+ min + " " 
					+ max + " &\n");
			
		}
		
		Logger.getInstance().setPath(saveFile, false);
		Logger.getInstance().write(sb.toString());
		Logger.getInstance().close();
	}
	
	/**2009-10-26: 
	 * 
	 * @param date
	 * @param saveFile
	 */
	public static void getTestingEffectiveness_Classified(String date, String saveFile){
		String[] H_M_E = new String[] {
				"H",
				"M",
				"E" 
		};//fault category
		int[] sizes_ART = new int[]{
				1, 2, 4, 8, 16, 32, 64, 70, 80
		};
		
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < H_M_E.length; i ++){
			//Load_Classified 20091026 1 H
			for(int j = 0; j < sizes_ART.length; j++){
				sb.append("java ccr.test.ResultAnalyzer Load_Classified " 
						+ date + " " + sizes_ART[j] + " "+ H_M_E[i] + "&").append("\n");	
			}			
		}
		Logger.getInstance().setPath(saveFile, false);
		Logger.getInstance().write(sb.toString());
		Logger.getInstance().close();
	}
	
	/**2009-10-29:study the correlation between testing effectiveness and 
	 * the candidate test set size
	 * 
	 * @param date
	 * @param saveFile
	 */
	public static void getTestingEffectivenessRegression(String date, String saveFile){
		StringBuilder sb = new StringBuilder();
		int testSetNum = 100;
		
		String[] criteria = new String[] { 
				"AllPolicies",
				"All1ResolvedDU",
				"All2ResolvedDU",
		};
		
		int[] size_ARTs = new int[]{
			30, 40, 50, 60, 65, 66, 67, 68, 69, 70	
		};
		
		int interval = 12;
		int sum = 0;
		//1.get adequate test sets for RA-H, RA-L, RA-R
//		getTestSet_Refined 20091026 2 AllPolicies -1 new random H 10
		for(int j = 0; j < criteria.length; j ++){
			for(int i = 0; i < size_ARTs.length; i ++){
				//for CA
				sum ++;
				if(sum % interval != 0 && (sum + 1) % interval != 0){
					sb.append("java ccr.test.TestSetManager getTestSet_Refined " 
							+ date + " " + testSetNum + " " + criteria[j]
							+ " -1 " + "old " + "random " + "R " +  size_ARTs[i]+ "&\n");	
				}else{
					sb.append("java ccr.test.TestSetManager getTestSet_Refined " 
							+ date + " " + testSetNum + " " + criteria[j]
							+ " -1 " + "old " + "random " + "R " +  size_ARTs[i]+ "\n");
				}
							
				//for RA_H
				sum ++;
				if(sum % interval != 0&& (sum + 1) % interval != 0){
					sb.append("java ccr.test.TestSetManager getTestSet_Refined " 
							+ date + " " + testSetNum + " " + criteria[j]
							+ " -1 " + "new " + "random " + "H " +  size_ARTs[i]+ "&\n");	
				}else{
					sb.append("java ccr.test.TestSetManager getTestSet_Refined " 
							+ date + " " + testSetNum + " " + criteria[j]
							+ " -1 " + "new " + "random " + "H " +  size_ARTs[i]+ "\n");
				}
				
				//for RA_L
				sum ++;
				if(sum % interval != 0&& (sum + 1) % interval != 0){
					sb.append("java ccr.test.TestSetManager getTestSet_Refined "  
							+ date + " " + testSetNum + " " + criteria[j]
							+ " -1 " + "new " + "random " + "L " +  size_ARTs[i]+ "&\n");	
				}else{
					sb.append("java ccr.test.TestSetManager getTestSet_Refined "  
							+ date + " " + testSetNum + " " + criteria[j]
							+ " -1 " + "new " + "random " + "L " +  size_ARTs[i]+ "\n");
				}
				
				//for RA_R
				sum ++;
				if(sum % interval != 0&& (sum + 1) % interval != 0){
					sb.append("java ccr.test.TestSetManager getTestSet_Refined " 
							+ date + " " + testSetNum + " " + criteria[j]
							+ " -1 " + "new " + "random " + "R " +  size_ARTs[i]+ "&\n");	
				}else{
					sb.append("java ccr.test.TestSetManager getTestSet_Refined " 
							+ date + " " + testSetNum + " " + criteria[j]
							+ " -1 " + "new " + "random " + "R " +  size_ARTs[i]+ "\n");
				}
								
			}
		}
		
		Logger.getInstance().setPath(saveFile, false);
		Logger.getInstance().write(sb.toString());
		Logger.getInstance().close();
	}
	
	
	public static void main(String[] args) {
		System.out
				.println("USAGE: java ccr.test.ScriptManager <testSetNum(100)> <Context_Intensity> <min_CI(0.7)> "
						+ "<max_CI(0.9)> <directory(20090222)> <testing criteria(AllPolicies, All1ResolvedDU, All2ResolvedDU)>"
						+ "<Min_TestSuiteSize(1)><Max_TestSuiteSize(58)> <oldOrNew(old, new)> <randomOrCriteria(random, criteria)>");
		String instruction = args[0];
		String saveFile = null;
		if (instruction.equals("GenRandomTS")) {
			int testSetNum = Integer.parseInt(args[1]);
			String date = args[2];
			int min_TestSuiteSize = Integer.parseInt(args[3]);
			int max_TestSuiteSize = Integer.parseInt(args[4]);

			//generate random test sets
			saveFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
					+ date + "/Script/GenRandomTestSet.sh";

			ScriptManager.save(genRandomTS_Script(testSetNum, min_TestSuiteSize,
					max_TestSuiteSize, date), saveFile);
			
		} else if (instruction.equals("GenAdequateTS")) {
			int testSetNum = Integer.parseInt(args[1]);
			String date = args[2];
			String size_ART = args[7];
			
			//generate adequate test sets
			saveFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
					+ date + "/Script/GenAdequateTestSet.sh";
//			ScriptManager.save(genAdequateTS_Script(testSetNum, min_CI, max_CI,
//					date, min_TestSuiteSize, max_TestSuiteSize), saveFile);
			
			//2009-09-19:
//			ScriptManager.save(genAdequateTS_Script(testSetNum, date), saveFile);
			
			//2009-10-13: add another parameter: size_ART 
			ScriptManager.save(genAdequateTS_Script(testSetNum, date, size_ART), saveFile);
			
		} else if (instruction.equals("ExeRandomTS")) {
			String date = args[1];
			int min_TestSuiteSize = Integer.parseInt(args[2]);
			int max_TestSuiteSize = Integer.parseInt(args[3]);
			//execute random test sets
			saveFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
				+ date + "/Script/ExeRandomTS.sh";
			ScriptManager.save(exeRandomTS_Script(min_TestSuiteSize, 
					max_TestSuiteSize, date), saveFile);
			
		} else if(instruction.equals("ExeAdequateTS")){
			int testSetNum = Integer.parseInt(args[1]);
			String date = args[2];
			String size_ART = args[3];
			
			//execute adequate test sets
			saveFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
				+ date + "/Script/ExeAdequateTS.sh";
			
//			ScriptManager.save(exeAdequateTS_Script(testSetNum, instruction, min_CI, max_CI, 
//					date, min_TestSuiteSize, max_TestSuiteSize), saveFile);
			
			//2009-09-19:
//			ScriptManager.save(exeAdequateTS_Script(testSetNum, date), saveFile);
			
			//2009-10-13: add another parameter
			ScriptManager.save(exeAdequateTS_Script(testSetNum, date, size_ART), saveFile);
		}else if(instruction.equals("AllInOne")){//getSizePerformance
			int testSetNum = Integer.parseInt(args[1]);
			double min_CI = Double.parseDouble(args[2]);
			double max_CI = Double.parseDouble(args[3]);

			String date = args[4];

			int min_TestSuiteSize = Integer.parseInt(args[5]);
			int max_TestSuiteSize = Integer.parseInt(args[6]);

			
			//This scripts is used to investigate the correlations between test suite size and testing performances
			//generate all random, adequate test sets, then execute them and analyze them
			//2009-03-07: we set a interval to separate all tasks into several shells
			int interval = 30;
			int start = 80; //(1-127:7, 127-200:5); 20090323:(1-80:40, 80-200:30)
			int end = 200;
			
			StringBuilder sb1 = new StringBuilder();
			for(int i = start; i < end; i = i + interval){
				min_TestSuiteSize = i;
				max_TestSuiteSize = i + interval;
				if(max_TestSuiteSize > end)
					max_TestSuiteSize = end;
				
				instruction = "Load";
				saveFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
					+ date + "/Script/AllInOne_"+min_TestSuiteSize
					+"_"+max_TestSuiteSize+".sh";
				
				sb1.append("./AllInOne_"+min_TestSuiteSize
					+"_"+max_TestSuiteSize+".sh &" + "\n");
				
				StringBuilder sb = new StringBuilder();
//				sb.append(genRandomTS_Script(testSetNum, min_TestSuiteSize,
//						max_TestSuiteSize, date));
//				
//				sb.append(genAdequateTS_Script(testSetNum, min_CI, max_CI,
//						date, min_TestSuiteSize, max_TestSuiteSize));
				
				sb.append(exeRandomTS_Script(min_TestSuiteSize, 
						max_TestSuiteSize, date));
				
				sb.append(exeAdequateTS_Script(testSetNum, instruction, min_CI, max_CI, 
						date, min_TestSuiteSize, max_TestSuiteSize));
				
				ScriptManager.save(sb.toString(), saveFile);

			}
			saveFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
				+ date + "/Script/AllInOne_Executor_"+ start +"_" +end+".sh";
			
//			sb1.append("java ccr.test.ResultAnalyzer getSizePerformance " + date + "\n");
			ScriptManager.save(sb1.toString(), saveFile);
		}else if(instruction.equals("AllInOne_Residual")){
			int testSetNum = Integer.parseInt(args[1]);
			double min_CI = Double.parseDouble(args[2]);
			double max_CI = Double.parseDouble(args[3]);
			String date = args[4];

			//2009-03-29: an method to handle residual tests
			String srcDir = "src/ccr/experiment/Context-Intensity_backup/TestHarness/" + date;
			
			//load residual test sets to execute
			String lostTestFile = srcDir +"//LostTest.txt";
			boolean containHeader = false;
			String line = null;
			
			StringBuilder sb = new StringBuilder();
			
			int interval = 23;
			
			try {
				BufferedReader br = new BufferedReader(new FileReader(lostTestFile));
				if(containHeader)
					br.readLine();
				
				//2.generate the test set execution scripts
				instruction = "Load";
				
				int counter = 0; //count the script files generated
				int count = 1;
				while((line = br.readLine())!=null){
					String[] strs = line.split("\t");
					String criterionString = strs[0];
					for(int i = 1; i < strs.length; i ++){
						int testSuiteSize = Integer.parseInt(strs[i]);
						sb.append(ScriptManager.genTestSet(criterionString, testSetNum, min_CI, max_CI, date, testSuiteSize));
						sb.append(ScriptManager.exeTestSet(criterionString, testSetNum, instruction, min_CI, max_CI, date, testSuiteSize));
						if(count % interval == 0){//save it 
							saveFile = srcDir + "/Script/ResidualTests_"+ (count - interval)+ "_" + count+".sh";					
							ScriptManager.save(sb.toString(), saveFile);
							sb = new StringBuilder();
							counter ++;
						}
						count ++;
					}
				}
				if(sb.toString().length()!=0){//save the last one
					count = (counter + 1) * interval ;
					saveFile = srcDir + "/Script/ResidualTests_"+ (count - interval)+ "_" + count+".sh";
					ScriptManager.save(sb.toString(), saveFile);
				}
				//a master script
				sb = new StringBuilder();
				count =interval;
				for(int i =0; i < counter; i ++){					
					sb.append("./ResidualTests_"+(count-interval) + "_" + count +".sh &\n");
					count += interval;
				}
				
				saveFile = srcDir + "/Script/ResidualTests.sh";
				ScriptManager.save(sb.toString(), saveFile);
				
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
		}else if(instruction.equals("3GroupComparison")){
			int testSetNum = Integer.parseInt(args[1]);
			double min_CI = Double.parseDouble(args[2]);
			double max_CI = Double.parseDouble(args[3]);
			String date = args[4];

			
			//2009-03-09:we are only interest in comparing three groups of testing criteria
			saveFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
				+ date + "/Script/3GroupComparison.sh";
			
			StringBuilder sb = new StringBuilder();
			instruction = "Load";
			//Group 1
			sb.append(genRandomTS_Script(testSetNum, 27,
					28,  date)); //to compare AllPolicies			
			sb.append(genRandomTS_Script(testSetNum, 42,
					43, date)); //to compare All1ResovledDU
			sb.append(genRandomTS_Script(testSetNum, 50,
					51, date)); //to compare All2ResolvedDU			
			sb.append(genAdequateTS_WithoutFixSize_Script(testSetNum, min_CI, max_CI,
					date));
									
			//Group 2 & Group 3
			sb.append(genRandomTS_Script(testSetNum, 62,
					63, date));

			sb.append(genAdequateTS_Script(testSetNum, min_CI, max_CI,
					date, 62, 63 ));
			
			//Execute test sets
			sb.append(exeRandomTS_Script(27, 
					28,instruction ,date));
			sb.append(exeRandomTS_Script(42, 
					43, instruction,date));
			sb.append(exeRandomTS_Script(50, 
					51, instruction,date));
			sb.append(exeAdequateTS_withoutFixSize_Script(testSetNum, min_CI, max_CI, instruction,
					date));
			
			sb.append(exeRandomTS_Script(62, 
					63, instruction,date));
			
			
			
			sb.append(exeAdequateTS_Script(testSetNum, instruction, min_CI, max_CI, 
					date, 62, 63));
			
//			sb.append("java ccr.test.ResultAnalyzer Load " + date + " \n");
			ScriptManager.save(sb.toString(), saveFile);
		}else if(instruction.equals("getCIPerformance")){
			int testSetNum = Integer.parseInt(args[1]);
			StringBuilder sb = new StringBuilder();
			
			double min_CI;
			double max_CI;
			
			double start_CI = Double.parseDouble(args[2]);
			double end_CI = Double.parseDouble(args[3]);
			double interval  = Double.parseDouble(args[4]);
			String date = args[5];
			
//			//1. generate adequate test sets
			for(double i = start_CI; i < end_CI; i = i + interval){
				min_CI = i;
				max_CI = i + interval;
				
				if(max_CI > end_CI)
					max_CI = end_CI;
				
				sb.append("java ccr.test.TestSetManager " + min_CI + " " 
					+ max_CI + " " + interval + " " + testSetNum +
					" " + date + " &\n");
			}
			
			saveFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
				+ date + "/Script/CIPerformance.sh";
			ScriptManager.save(sb.toString(), saveFile);
			
//			//2. 2009-03-31: execute these test sets: get the relationships between CI of adequate test sets and testing effectiveness
//			String srcDir ="src/ccr/experiment/Context-Intensity_backup/TestHarness/"
//				+ date +"/";
//			String testSetFile = null;
//			for(double i = start_CI; i < end_CI; i = i + interval){
//				min_CI = i;
//				max_CI = i + interval;
//				
//				if(max_CI > end_CI)
//					max_CI = end_CI;
//				
//				String criteria[] = {
//						"AllPolicies",
//						"All1ResolvedDU",
//						"All2ResolvedDU",
//				};
//				for(int j = 0; j < criteria.length; j ++){
//					String criterion = criteria[j];
//					testSetFile = srcDir + criterion+"_"+min_CI + "_"+max_CI+".txt";
//					sb.append("java ccr.test.ExecutionManager " + date + " " 
//						+ testSetFile+ " \n");	
//				}
//			}
//			saveFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
//				+ date + "/Script/CIPerformance.sh";
//			ScriptManager.save(sb.toString(), saveFile);
			
			//2. 2009-03-31: execute these test sets: get the correlations between CI of test cases and testing effectiveness
			String srcDir ="src/ccr/experiment/Context-Intensity_backup/TestHarness/"
				+ date +"/";
			String testSetFile = null;
			for(double i = start_CI; i < end_CI; i = i + interval){
				min_CI = i;
				max_CI = i + interval;
				
				if(max_CI > end_CI)
					max_CI = end_CI;
				
					
				testSetFile = srcDir + "TestCases_"+min_CI + "_"+max_CI+".txt";
				sb.append("java ccr.test.ExecutionManager " + date + " " 
						+ testSetFile+ " \n");	
				
			}
			saveFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
				+ date + "/Script/CIPerformance.sh";
			ScriptManager.save(sb.toString(), saveFile);
			
		}else if(instruction.equals("getFailureRate")){
			String date = args[1];
			int start = 4369;
			int end = 4527; //[140, 3600][3600, 5024]
			int concurrent = 10;
			
			//2.concurrent execution
//			saveFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
//				+ date + "/Script/GetFailureRate_Executor_"+start + "_" + end +".sh";
//			
//			ScriptManager.genFailureRate_TS(start, end, concurrent, date, saveFile);
			
			//1. sequential execution
			int interval = (end - start + 1)/concurrent ;
			if( (end - start + 1) > interval * concurrent )
				interval ++;
			
			StringBuilder sb1 = new StringBuilder();
			for(int i = start; i < end; i = i + interval){
				int startVersion = i;
				int endVersion = i + interval;
				if(endVersion > end)
					endVersion = end;
				
				instruction = "getFailurRate";
				saveFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
					+ date + "/Script/GetFailureRate_"+startVersion
					+"_"+endVersion+".sh";
				
				sb1.append("./GetFailureRate_"+startVersion
					+"_"+endVersion+".sh &" + "\n");
				
				ScriptManager.genFailureRate_Sequential(date, saveFile, startVersion, endVersion);
			}

			saveFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
				+ date + "/Script/GetFailureRate_Executor_"+start + "_" + end +".sh";
			ScriptManager.save(sb1.toString(), saveFile);
			
			//3. 2009-03-20: residual execution
//			int[] residualVersions = new int[]{
//					2341, 2402,
//					3434, 3446,
//					1509, 1532,
//					3090, 3098,
//					3249, 3272
//			};
//			ArrayList faultList = new ArrayList(); 
//			for(int i = 0; i < residualVersions.length; i = i +2){
//				int start = residualVersions[i];
//				int end  = residualVersions[i + 1];
//				for(int j = start; j < end; j ++){
//					faultList.add(j);
//				}
//			}
//			
//			int start = 0;
//			int end = faultList.size();
//			int concurrent = 20;
//			int counter = 0;
//			
//			int interval = (end - start )/concurrent ;
//			if( (end - start) > interval * concurrent )
//				interval ++;
//			
//			
//			for(int i = start; i < end; i = i + interval){
//				int startVersion = i;
//				int endVersion = i + interval + 1;
//				if(endVersion > end)
//					endVersion = end;
//				
//				instruction = "getFailurRate";
//				saveFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
//					+ date + "/Script/GetFailureRate_"+counter +".sh";
//				
//				
//				
//				StringBuilder sb = new StringBuilder();
//				ScriptManager.genFailureRate_Sequential(date, saveFile, faultList, startVersion, endVersion);
//				counter = (counter + 1) % concurrent;
//			}
//			
//			StringBuilder sb1 = new StringBuilder(); 
//			for(int i = 0; i < counter; i ++){
//				sb1.append("./GetFailureRate_"+ i + ".sh &" + "\n");
//			}
//
//			saveFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
//				+ date + "/Script/GetFailureRate_Executor_"+start + "_" + end +".sh";
//			ScriptManager.save(sb1.toString(), saveFile);
			
		}else if(instruction.equals("getEffectiveness_Classified")){
			//2009-10-26: get testing effectiveness based on classified faults.
			String date = args[1];
			
			saveFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
				+ date + "/Script/GetEffectiveness_Classified.sh";
			getTestingEffectiveness_Classified(date, saveFile);
		}else if(instruction.equals("getEffectivenessRegression")){
			//2009-10-29:get the correlation between testing effectiveness 
			//and the candidate test set size, include the upper improvement
			//brought by context diversity
			String date = args[1];
			
			saveFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
				+ date + "/Script/getEffectivenessRegression.sh";
			
			getTestingEffectivenessRegression(date, saveFile);
			
		}
	}
}
