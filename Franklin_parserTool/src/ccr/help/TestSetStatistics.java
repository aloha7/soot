package ccr.help;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.StringTokenizer;

import ccr.app.Application;
import ccr.test.TestSet;

public class TestSetStatistics {
	
	public static ArrayList<TestSet> loadTestSet_offline(boolean containHeader, String testSetFile){		
		ArrayList<TestSet> testSets = new ArrayList<TestSet>();		
		
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
	
	/**2010-01-22: get the name of the test set file 
	 * 
	 * @param date: the directory to get test set files
	 * @param criterion: AllPolicies, All1ResolvedDU, All2ResolvedDU
	 * @param testSuiteSize: "-1" means does not specify the test suite size (study the correlation between test suite size and testing effectiveness)
	 * @param oldOrNew: conventional or refined test suite construction approach
	 * @param randomOrCriterion: (applicable when testSuiteSize > 0) the way to supplement the test suite when the size is not large enough
	 * @param H_L_R: H: favor higher context diversity; L: favor lower context diversity; R: favor random context diversity
	 * @param size_ART: the candidate test suite size when ART sampling strategy is applied
	 * @return
	 */
	public static String getTestSetFile(String date, String criterion, int testSuiteSize, 
			String oldOrNew, String randomOrCriterion, String H_L_R, int size_ART){
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
		return testSetFile;
	}

	/**2010-01-22: save test sets into database, 
	 * note that test cases will not be saved.
	 * 
	 * @param append:true: append the test sets; false: delete the table before appending
	 * @param testSetFile
	 * @param containHeader
	 * @param database: the name of database
	 */
	public static void saveToDB_TestSet(boolean append, String testSetFile, boolean containHeader, String database){
		ArrayList<TestSet> testSets = loadTestSet_offline(containHeader, testSetFile);		
		if(testSets.size() > 0){
			String tableName = "testset_"+new File(testSetFile).getName();
			tableName = tableName.substring(0, tableName.indexOf("."));
			StringBuilder sql = new StringBuilder();
			String sqlStmt = null;
			
			DatabaseManager.setDatabase(database);
			if(!append){
				//1. delete the existing table								
				sqlStmt = "drop table if exists " + tableName;
				DatabaseManager.getInstance().update(sqlStmt);
			}
			
			
					//2. create the table if it is empty
			sqlStmt = "create table if not exists " + tableName + " " +
						"(id varchar(45), size varchar(45), coverage varchar(45)," +
						" gentime varchar(45), replacecounter varchar(45), " +
						"tieactivationCI varchar(45))";
			DatabaseManager.getInstance().update(sqlStmt);
			
			DecimalFormat format = new DecimalFormat("0.0000000");
			
			
			sql.append("INSERT INTO ").append(tableName).append(" (id, size, " +
					"coverage, gentime, replacecounter,tieactivationCI ) VALUES ");			
			for(int i = 0; i < testSets.size(); i ++){
				TestSet ts = testSets.get(i);
				String id = ts.index;
				String size = ""+ts.testcases.size();
				String coverage = format.format(ts.coverage);
				String gentime = "" + ts.geneTime;
				String replaceCounter = "" + ts.replaceCounter;
				String tie_activation_CI = "" + ts.tie_activation_CI;
				
				sql.append("(\'").append(id).append("\'").append(",");
				sql.append("\'").append(size).append("\'").append(",");
				sql.append("\'").append(coverage).append("\'").append(",");					
				sql.append("\'").append(gentime).append("\'").append(",");
				sql.append("\'").append(replaceCounter).append("\'").append(",");				
				sql.append("\'").append(tie_activation_CI).append("\'").append("),");			
			}			
			sqlStmt = sql.substring(0, sql.lastIndexOf(","));
			DatabaseManager.getInstance().update(sqlStmt);
		}		
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String instruction = args[0];
		
		if(instruction.equals("saveTestSetToDB")){
			String date_testSets = args[1];
			String criterion = args[2];
			int testSuiteSize = Integer.parseInt(args[3]);
			String oldOrNew = args[4];
			String randomOrCriterion = args[5];
			String H_L_R = args[6];
			int size_ART = Integer.parseInt(args[7]);
			String date_testPool = args[8];
			
			//1. parse the test set file
			String testSetFile = getTestSetFile(date_testSets, criterion, testSuiteSize, 
					oldOrNew, randomOrCriterion, H_L_R, size_ART);
			
			//2. save test sets into database (Note that test cases are not included)			
			boolean append = false;
			boolean containHeader = false;
			String dbName = "icdcs_10";
			saveToDB_TestSet(append, testSetFile, containHeader, dbName);
		}

	}

}
