package ccr.report;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;

import ccr.help.DataAnalyzeManager;
import ccr.help.DataDescriptionResult;
import ccr.help.MutantStatistics;
import ccr.help.TestSetStatistics;
import ccr.reduction.ILPSolver;
import ccr.stat.CFG;
import ccr.stat.Criterion;
import ccr.test.Adequacy;
import ccr.test.Logger;
import ccr.test.Oracle;
import ccr.test.ResultAnalyzer;
import ccr.test.TestDriver;
import ccr.test.TestSet;
import ccr.test.TestSetManager;

public class ASE10 {
	
	public static HashMap<Double, Integer> classifyDiff(ArrayList<Double> diffs, double threshold){
		HashMap<Double, Integer> threshold_counter = new HashMap<Double, Integer>();
		
		int counter1 = 0, counter2 = 0, counter3 = 0; 
		for(int i = 0; i < diffs.size(); i ++){
			Double diff = diffs.get(i);
			if(diff < - threshold ){
				counter1 ++;
			}else if( diff > threshold){
				counter2 ++;
			}else{
				counter3 ++;
			}
		}
		threshold_counter.put(-threshold, counter1); //< -threshold
		threshold_counter.put(threshold, counter2); //> threshold
		threshold_counter.put(0.0, counter3); // -threshold <= <= threshold
		
		return threshold_counter;
	}
	
	public static String saveEffectivenessDifference(String date, String size_ART, double[] diffs){
		String testSetFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/" +
		""+date+"/" + size_ART + "/PerValidTS.txt";
		StringBuilder sb = new StringBuilder();
		
		try {
			BufferedReader br = new BufferedReader(new FileReader(testSetFile));
			String str = null;
			
			br.readLine(); //ignore headers
			
			HashMap<String, HashMap<Double, Integer>> criterion_diff_num =
				new HashMap<String, HashMap<Double,Integer>>();
			
			ArrayList<Double> AS_RAH_CA_diffs = new ArrayList<Double>();
			ArrayList<Double> AS_RAH_RAL_diffs = new ArrayList<Double>();
			ArrayList<Double> AS_RAH_RAR_diffs = new ArrayList<Double>();
			ArrayList<Double> AS_RAH_Random_diffs = new ArrayList<Double>();
			
			ArrayList<Double> ASU_RAH_CA_diffs = new ArrayList<Double>();
			ArrayList<Double> ASU_RAH_RAL_diffs = new ArrayList<Double>();
			ArrayList<Double> ASU_RAH_RAR_diffs = new ArrayList<Double>();
			ArrayList<Double> ASU_RAH_Random_diffs = new ArrayList<Double>();
			
			ArrayList<Double> A2SU_RAH_CA_diffs = new ArrayList<Double>();
			ArrayList<Double> A2SU_RAH_RAL_diffs = new ArrayList<Double>();
			ArrayList<Double> A2SU_RAH_RAR_diffs = new ArrayList<Double>();
			ArrayList<Double> A2SU_RAH_Random_diffs = new ArrayList<Double>();
			
			while((str = br.readLine())!= null){
				String[] strs = str.split("\t");
				if(strs.length == 26){
					String fault = strs[0];

					double random_27 = Double.parseDouble(strs[2]); 
					double AS_CA = Double.parseDouble(strs[3]);
					double AS_RAH = Double.parseDouble(strs[4]);
					double AS_RAL = Double.parseDouble(strs[5]);
					double AS_RAR = Double.parseDouble(strs[6]);
					
					double AS_RAH_CA_diff = AS_RAH - AS_CA;
					double AS_RAH_RAL_diff = AS_RAH - AS_RAL;
					double AS_RAH_RAR_diff = AS_RAH - AS_RAR;
					double AS_RAH_Random_diff = AS_RAH - random_27;
					AS_RAH_CA_diffs.add(AS_RAH_CA_diff);
					AS_RAH_RAL_diffs.add(AS_RAH_RAL_diff);
					AS_RAH_RAR_diffs.add(AS_RAH_RAR_diff);
					AS_RAH_Random_diffs.add(AS_RAH_Random_diff);
					
					double random_42 = Double.parseDouble(strs[7]);
					double ASU_CA = Double.parseDouble(strs[8]);
					double ASU_RAH = Double.parseDouble(strs[9]);
					double ASU_RAL = Double.parseDouble(strs[10]);
					double ASU_RAR = Double.parseDouble(strs[11]);
					
					double ASU_RAH_CA_diff = ASU_RAH - ASU_CA;
					double ASU_RAH_RAL_diff = ASU_RAH - ASU_RAL;
					double ASU_RAH_RAR_diff = ASU_RAH - ASU_RAR;
					double ASU_RAH_Random_diff = ASU_RAH - random_42;
					ASU_RAH_CA_diffs.add(ASU_RAH_CA_diff);
					ASU_RAH_RAL_diffs.add(ASU_RAH_RAL_diff);
					ASU_RAH_RAR_diffs.add(ASU_RAH_RAR_diff);
					ASU_RAH_Random_diffs.add(ASU_RAH_Random_diff);
					
					double random_50 = Double.parseDouble(strs[12]);
					double A2SU_CA = Double.parseDouble(strs[13]);
					double A2SU_RAH = Double.parseDouble(strs[14]);
					double A2SU_RAL = Double.parseDouble(strs[15]);
					double A2SU_RAR = Double.parseDouble(strs[16]);
					
					double A2SU_RAH_CA_diff = A2SU_RAH - A2SU_CA;
					double A2SU_RAH_RAL_diff = A2SU_RAH - A2SU_RAL;
					double A2SU_RAH_RAR_diff = A2SU_RAH - A2SU_RAR;
					double A2SU_RAH_Random_diff = A2SU_RAH - random_50;
					A2SU_RAH_CA_diffs.add(A2SU_RAH_CA_diff);
					A2SU_RAH_RAL_diffs.add(A2SU_RAH_RAL_diff);
					A2SU_RAH_RAR_diffs.add(A2SU_RAH_RAR_diff);
					A2SU_RAH_Random_diffs.add(A2SU_RAH_Random_diff);
				}
			}
			
			DecimalFormat format = new DecimalFormat("0.00");
			String[] criteria = {"AS", "ASU", "A2SU"};
			
			//2010-03-18: reformulate the header
			
			sb.append("\t");
			for(int k = 0; k < diffs.length; k++){
				sb.append("n=" + size_ART + "\t");	
			}
			sb.append("\n");
			
			sb.append("Criterion\t");	
			//2010-03-18: only interest in ">" items
			for(int k = 0; k < diffs.length; k ++){
				double diff = diffs[k];
				sb.append(">"+diff+"\t");
			}
			sb.append("\n");
			
			//compare RA-H with CA
			for(int i = 0; i < criteria.length; i ++){
				String criterion = criteria[i];
				sb.append( criterion+"_RAH-CA\t");
				for(int k = 0; k < diffs.length; k ++){
					double diff = diffs[k];
					ArrayList criterion_diffs = null;
					if(criterion.equals("AS"))
						criterion_diffs = AS_RAH_CA_diffs;
					else if(criterion.equals("ASU"))
						criterion_diffs = ASU_RAH_CA_diffs;
					else if(criterion.equals("A2SU"))
						criterion_diffs = A2SU_RAH_CA_diffs;
					
					HashMap<Double, Integer> threshold_counter = classifyDiff(criterion_diffs, diff);
					sb.append(format.format((double)threshold_counter.get(diff)*100/(double)AS_RAH_CA_diffs.size()) + "%\t");					
				}
				sb.append("\n");
			}
			
			//compare RA-H with RA-L
			for(int i = 0; i < criteria.length; i ++){
				String criterion = criteria[i];
				sb.append( criterion+"_RAH-RAL\t");
				for(int k = 0; k < diffs.length; k ++){
					double diff = diffs[k];
					ArrayList criterion_diffs = null;
					if(criterion.equals("AS"))
						criterion_diffs = AS_RAH_RAL_diffs;
					else if(criterion.equals("ASU"))
						criterion_diffs = ASU_RAH_RAL_diffs;
					else if(criterion.equals("A2SU"))
						criterion_diffs = A2SU_RAH_RAL_diffs;
					
					HashMap<Double, Integer> threshold_counter = classifyDiff(criterion_diffs, diff);
					sb.append( format.format((double)threshold_counter.get(diff)*100/(double)AS_RAH_RAL_diffs.size()) + "%\t");
				}
				sb.append("\n");
			}
			
			//compare RA-H with RA-R
			for(int i = 0; i < criteria.length; i ++){
				String criterion = criteria[i];
				sb.append( criterion+"_RAH-RAR\t");
				for(int k = 0; k < diffs.length; k ++){
					double diff = diffs[k];
					ArrayList criterion_diffs = null;
					if(criterion.equals("AS"))
						criterion_diffs = AS_RAH_RAR_diffs;
					else if(criterion.equals("ASU"))
						criterion_diffs = ASU_RAH_RAR_diffs;
					else if(criterion.equals("A2SU"))
						criterion_diffs = A2SU_RAH_RAR_diffs;
					
					HashMap<Double, Integer> threshold_counter = classifyDiff(criterion_diffs, diff);
					sb.append(format.format((double)threshold_counter.get(diff)*100/(double)AS_RAH_RAR_diffs.size()) + "%\t");

				}
				sb.append("\n");
			}
			
			//compare RA-H with Random
			for(int i = 0; i < criteria.length; i ++){
				String criterion = criteria[i];
				sb.append( criterion+"_RAH-Random\t");
				for(int k = 0; k < diffs.length; k ++){
					double diff = diffs[k];
					ArrayList criterion_diffs = null;
					if(criterion.equals("AS"))
						criterion_diffs = AS_RAH_Random_diffs;
					else if(criterion.equals("ASU"))
						criterion_diffs = ASU_RAH_Random_diffs;
					else if(criterion.equals("A2SU"))
						criterion_diffs = A2SU_RAH_Random_diffs;
					
					HashMap<Double, Integer> threshold_counter = classifyDiff(criterion_diffs, diff);
					sb.append(format.format((double)threshold_counter.get(diff)*100/(double)AS_RAH_Random_diffs.size()) + "%\t");

				}
				sb.append("\n");
			}
			
			String filename = "src/ccr/experiment/Context-Intensity_backup/TestHarness/" +
			""+date+"/" + size_ART + "/effectivenessDiff.txt";
			Logger.getInstance().setPath(filename, false);
			Logger.getInstance().write(sb.toString());
			Logger.getInstance().close();
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return sb.toString();
	}
	
	
	
	public static void saveGenTime(String date, String size_ART){
		String[] criteria = new String[] { "AllPolicies_CA",
				"AllPolicies_RA-H",  "AllPolicies_RA-R","AllPolicies_RA-L",

				"All1ResolvedDU_CA", "All1ResolvedDU_RA-H",
				 "All1ResolvedDU_RA-R","All1ResolvedDU_RA-L",

				"All2ResolvedDU_CA", "All2ResolvedDU_RA-H",
				"All2ResolvedDU_RA-R","All2ResolvedDU_RA-L",  
		};
		String[] rename =	new String[] { "AS_CA",
						"AS_RA-H", "AS_RA-R", "AS_RA-L",

						"ASU_CA", "ASU_RA-H",
						 "ASU_RA-R","ASU_RA-L",

						"A2SU_CA", "A2SU_RA-H",
						 "A2SU_RA-R","A2SU_RA-L", 
				};
		
		boolean containHeader = false;
		StringBuilder sb = new StringBuilder();
		
//		for(int i = 0; i < rename.length; i ++){
//			sb.append(rename[i]).append("\t");	
//		}
//		sb.append("\n");
		
		HashMap<String, ArrayList<TestSet>> criterion_TestSets = new HashMap<String, ArrayList<TestSet>>();
		int testSetNum = 0;
		for(int i = 0; i < criteria.length; i ++){
			String criterion = criteria[i];
			String testSetFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/" +
			""+date+"/" + size_ART + "/" + criterion + "_"+ size_ART +".txt";			
			ArrayList<TestSet> testSets = TestSetStatistics.loadTestSet_offline(testSetFile, containHeader);
			testSetNum = testSets.size();
			criterion_TestSets.put(criterion, testSets);
		}
		
		
		//2010-03-16: only list the mean generation time
		sb.append("Criterion\t").append("mean\n");
		for(int i = 0; i < criteria.length; i ++){
			String criterion = criteria[i];
			
			ArrayList<TestSet> testSets = criterion_TestSets.get(criterion);
			
			double[] generationTime = new double[testSets.size()];
			for(int j = 0; j < generationTime.length; j ++){
				generationTime[j] = testSets.get(j).geneTime;
			}
			DataDescriptionResult description = DataAnalyzeManager.getDataDescriptive(generationTime);
			sb.append(rename[i]).append("\t").append(description.mean).append("\n");			
		}
		
		String filename = "src/ccr/experiment/Context-Intensity_backup/TestHarness/" +
		""+date+"/" + size_ART + "/genTime.txt";
		Logger.getInstance().setPath(filename, false); 
		Logger.getInstance().write(sb.toString());
		Logger.getInstance().close();
	}
	
	/**2010-03-18: load test suite generation configurations from files, then construct
	 * test suite corresponding
	 * @param date_configurationFile: directory of test suite generation configuration file
	 */
	public static void getAdequateTestSets(String date_configurationFile){
		String configFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/" +
		""+date_configurationFile+"/TestSuiteGenConfig.txt";
		try {
			
			//1. load test suite generation parameters from configuration files 
			BufferedReader br = new BufferedReader(new FileReader(configFile));
			String str = br.readLine();
			int testSuiteNum = Integer.parseInt(str.substring(str.indexOf(":")+1));
			
			str = br.readLine();
			double min_CI = Double.parseDouble(str.substring(str.indexOf(":")+1));
			
			str = br.readLine();
			double max_CI = Double.parseDouble(str.substring(str.indexOf(":")+1));
			
			str = br.readLine();
			String date = str.substring(str.indexOf(":")+1);
			
			str = br.readLine();
			String tmp = str.substring(str.indexOf(":")+1);			
			String[] criteria = tmp.split(",");
			
			str = br.readLine();
			int testSuiteSize = Integer.parseInt(str.substring(str.indexOf(":")+1));
			
			str = br.readLine();
			tmp = str.substring(str.indexOf(":")+1);			
			String[] oldOrNews = tmp.split(",");
			
			str = br.readLine();
			String randomOrCriterion = str.substring(str.indexOf(":")+1);
			
			str = br.readLine();
			tmp = str.substring(str.indexOf(":")+1);			
			String[] H_L_Rs = tmp.split(",");
			
			str = br.readLine();
			tmp = str.substring(str.indexOf(":")+1);			
			String[] sizes = tmp.split(",");
			int[] size_ARTs = new int[sizes.length];
			for(int i = 0; i < size_ARTs.length; i ++){
				size_ARTs[i] = Integer.parseInt(sizes[i]);
			}						
			br.close();
			
			//2. generate adequate test suites
			getAdequateTestSets(testSuiteNum, min_CI, max_CI, date, criteria, 
					testSuiteSize, oldOrNews, randomOrCriterion, H_L_Rs, size_ARTs);
			
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
	}
	
	public static void getAdequateTestSets(int testSetNum, double min_CI, double max_CI, String date,
			String[] criteria, int testSuiteSize, String[] oldOrNews, String randomOrCriterion, 
			String[] H_L_Rs, int[] size_ARTs){
		
		CFG g = new CFG(System.getProperty("user.dir")
				+ "/src/ccr/app/TestCFG2.java");
		Criterion c = null;

		String testPoolFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
				+ date + "/TestPool.txt";
		TestSet testpool = TestSetManager.getTestPool(testPoolFile, true);
		Adequacy.loadTestCase(testPoolFile);
		
		TestSet[][] testSets = new TestSet[1][];
		String versionPackageName = "testversion";
		
		String criterion;
		String oldOrNew;
		int size_ART;
		String H_L_R;
		
		for(int i = 0; i < size_ARTs.length; i++){ //for each ART_size
			size_ART = size_ARTs[i];
			for(int j = 0; j < criteria.length; j ++){ //for each testing criterion
				criterion = criteria[j];
				for(int k = 0; k < oldOrNews.length; k ++){
					oldOrNew = oldOrNews[k];
					if(oldOrNew.equals("new")){
						for(int m = 0; m < H_L_Rs.length; m ++){
							H_L_R = H_L_Rs[m];
							
							if (criterion.equals("AllPolicies"))
								c = g.getAllPolicies();
							else if (criterion.equals("All1ResolvedDU"))
								c = g.getAllKResolvedDU(1);
							else if (criterion.equals("All2ResolvedDU"))
								c = g.getAllKResolvedDU(2);

							String saveFile = TestSetStatistics.getTestSetFile(date, criterion, testSuiteSize, 
									oldOrNew, randomOrCriterion, H_L_R, size_ART);

							String appClassName = "TestCFG2_ins";
							int maxTrials = 2000;
							testSets[0] =  TestSetManager.getTestSets(appClassName, c, testpool,
									maxTrials, testSetNum, oldOrNew, randomOrCriterion,
									testSuiteSize, saveFile, H_L_R, size_ART);
							

							saveFile = saveFile.substring(0, saveFile.indexOf(".txt"))
									+ "_CI.txt";
							
							TestSetManager.attachTSWithCI_Activation_replacement(testSets[0],
									saveFile);
						}	
					}else{
						H_L_R = "R";
						if (criterion.equals("AllPolicies"))
							c = g.getAllPolicies();
						else if (criterion.equals("All1ResolvedDU"))
							c = g.getAllKResolvedDU(1);
						else if (criterion.equals("All2ResolvedDU"))
							c = g.getAllKResolvedDU(2);

						String saveFile = TestSetStatistics.getTestSetFile(date, criterion, testSuiteSize, 
								oldOrNew, randomOrCriterion, H_L_R, size_ART);
						

						String appClassName = "TestCFG2_ins";
						int maxTrials = 2000;
						testSets[0] =  TestSetManager.getTestSets(appClassName, c, testpool,
								maxTrials, testSetNum, oldOrNew, randomOrCriterion,
								testSuiteSize, saveFile, H_L_R, size_ART);
						

						saveFile = saveFile.substring(0, saveFile.indexOf(".txt"))
								+ "_CI.txt";
						
						TestSetManager.attachTSWithCI_Activation_replacement(testSets[0],
								saveFile);
						
					}
				}
			}
		}
	}
	
	/**2010-03-18: load complex parameters from configuration files and 
	 * save fault detection rate of adequate test sets for each
	 * fault w.r.t a given size_ART, and get the descriptions(e.g., min/mean/median/max/std/)
	 * of fault detection rates
	 * 
	 * @param date_configurationFile
	 */
	public static void saveTestingResults_TestSets(String date_configurationFile){
		
		String configFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/" +
		""+date_configurationFile+"/saveTestResult_TestSet.txt";
		try {
			
			//1. load parameters from configuration files 
			BufferedReader br = new BufferedReader(new FileReader(configFile));
			String str = br.readLine();
			String date_testSets = str.substring(str.indexOf(":")+1);

			str = br.readLine();
			String tmp = str.substring(str.indexOf(":")+1);			
			String[] criteria = tmp.split(",");

			str = br.readLine();
			int testSuiteSize = Integer.parseInt(str.substring(str.indexOf(":")+1));
			
			str = br.readLine();
			tmp = str.substring(str.indexOf(":")+1);			
			String[] oldOrNews = tmp.split(",");

			str = br.readLine();
			String randomOrCriterion = str.substring(str.indexOf(":")+1);
			
			str = br.readLine();
			tmp = str.substring(str.indexOf(":")+1);			
			String[] H_L_Rs = tmp.split(",");
			
			str = br.readLine();
			tmp = str.substring(str.indexOf(":")+1);			
			String[] sizes = tmp.split(",");
			int[] size_ARTs = new int[sizes.length];
			for(int i = 0; i < size_ARTs.length; i ++){
				size_ARTs[i] = Integer.parseInt(sizes[i]);
			}						
			
			str = br.readLine();
			String date_TestPool = str.substring(str.indexOf(":")+1);
			
			str = br.readLine();
			String date_FaultList = str.substring(str.indexOf(":")+1);
			
			br.close();

			//2. save fault detection rate of adequate test sets for each
			//fault w.r.t a given size_ART, and get the descriptions(e.g., min/mean/median/max/std/)
			//of fault detection rates
			saveTestingResults_TestSets(date_testSets, criteria, testSuiteSize, 
					oldOrNews, randomOrCriterion, H_L_Rs, size_ARTs,
					date_TestPool, date_FaultList);
			
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
	}

	
	
	public static void saveTestingResults_TestSets(String date_testSets, String[] criteria, int testSuiteSize,
			String[] oldOrNews, String randomOrCriterion, String[] H_L_Rs, int[] size_ARTs, 
			String date_TestPool, String date_FaultList){
	
		//1. load the fault list
		String mutantFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
			+ date_FaultList+ "/FaultList.txt";
		boolean containHeader = false;
		ArrayList<String> faultList = MutantStatistics.loadFaults_offline(mutantFile, containHeader);
		
		//2. save test results
		int size_ART;
		String criterion;
		String oldOrNew;
		String H_L_R;
		for(int i = 0; i < size_ARTs.length; i++){ //for each ART_size
			size_ART = size_ARTs[i];
			for(int j = 0; j < criteria.length; j ++){ //for each testing criterion
				criterion = criteria[j];
				for(int k = 0; k < oldOrNews.length; k ++){
					oldOrNew = oldOrNews[k];
					if(oldOrNew.equals("new")){
						for(int m = 0; m < H_L_Rs.length; m ++){
							H_L_R = H_L_Rs[m];

							//2.1.load the test sets
							String testSetFile = TestSetStatistics.getTestSetFile(date_testSets, criterion, testSuiteSize, 
									oldOrNew, randomOrCriterion, H_L_R, size_ART);
							TestSet testSets[][] = new TestSet[1][];
							testSets[0] = Adequacy.getTestSets(testSetFile);
							String saveFile = testSetFile.substring(0, testSetFile
									.indexOf("."))
									+ "_limited_load.txt";
							
							String date_execHisotry =  date_FaultList;

							//2.2. test the specified faults
							TestDriver
									.test_load(testSets, faultList, date_execHisotry, saveFile);

						}	
					}else{
						H_L_R = "R"; //this paramter does not matter
						//2.1.load the test sets
						String testSetFile = TestSetStatistics.getTestSetFile(date_testSets, criterion, testSuiteSize, 
								oldOrNew, randomOrCriterion, H_L_R, size_ART);
						TestSet testSets[][] = new TestSet[1][];
						testSets[0] = Adequacy.getTestSets(testSetFile);
						String saveFile = testSetFile.substring(0, testSetFile
								.indexOf("."))
								+ "_limited_load.txt";
						
						String date_execHisotry =  date_FaultList;

						//2.2. test the specified faults
						TestDriver
								.test_load(testSets, faultList, date_execHisotry, saveFile);
					}
				}
			}
		}
	}
	
	
	/**2010-03-18:load complex parameters from configuration files and save 
	 * testing effectiveness of adequate test suites generated by ILP model
	 * 
	 * @param date_configurationFile
	 * @param criterion
	 */
	public static void saveFDR_cost_ILPModel(String date_configurationFile, String criterion){
		
		String configFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/" +
		""+date_configurationFile+"/saveFDR_ILPModel_"+criterion+".txt";		
		try {
			
			//1. load parameters from configuration files 
			BufferedReader br = new BufferedReader(new FileReader(configFile));
			String str = br.readLine();
			String date = str.substring(str.indexOf(":")+1);

			str = br.readLine();
			String tmp = str.substring(str.indexOf(":")+1);			
			String[] criteria = tmp.split(",");

			str = br.readLine();
			tmp = str.substring(str.indexOf(":")+1);			
			String[] H_L_Ds = tmp.split(",");
			
			str = br.readLine();
			tmp = str.substring(str.indexOf(":")+1);			
			String[] beta_min_str = tmp.split(",");
			
			str = br.readLine();
			tmp = str.substring(str.indexOf(":")+1);			
			String[] beta_max_str = tmp.split(",");
			
			if(beta_max_str.length == criteria.length && 
					beta_min_str.length ==  criteria.length){
				
				int[] betas_min = new int[beta_min_str.length];
				for(int i = 0; i < betas_min.length; i ++){
					betas_min[i] = Integer.parseInt(beta_min_str[i]);
				}
				
				int[] betas_max = new int[beta_max_str.length];
				for(int i = 0; i < betas_max.length; i ++){
					betas_max[i] = Integer.parseInt(beta_max_str[i]);
				}						
				
				str = br.readLine();
				int testSetNum = Integer.parseInt(str.substring(str.indexOf(":")+1));
				
				str = br.readLine();
				long timeLimit = Long.parseLong(str.substring(str.indexOf(":")+1));
				if(timeLimit == 0){
					timeLimit = Long.MAX_VALUE;
				}
				
				str = br.readLine();
				long sleepTime = Long.parseLong(str.substring(str.indexOf(":")+1));
				
				str = br.readLine();
				double alpha_min = Double.parseDouble(str.substring(str.indexOf(":")+1));			
				
				str = br.readLine();
				double alpha_max = Double.parseDouble(str.substring(str.indexOf(":")+1));
				
				str = br.readLine();
				double alpha_interval = Double.parseDouble(str.substring(str.indexOf(":")+1));
			
				
				str = br.readLine();
				String mutantFile_date = str.substring(str.indexOf(":")+1);			
				
				str = br.readLine();
				String mutantDetailDir = System.getProperty("user.dir") + "/src/ccr"
				+ "/experiment/Context-Intensity_backup/TestHarness/" + str.substring(str.indexOf(":")+1);				
				br.close();
				
				saveFDR_cost_ILPModel_offline(date, criteria, alpha_min, alpha_max, alpha_interval, 
						betas_min, betas_max, testSetNum, H_L_Ds, timeLimit, sleepTime, 
						mutantFile_date, mutantDetailDir);
				
			
			}else{
				System.out.println("Invalid paramters: " +
						"numbers of criteria and betas do not match with each other!");
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
	}
	
	
	
	/**2010-03-18:build and solve ILP models, then save the 
	 *fault detection rate of reduced test suites in offline way
	 * and the required time to finish this
	 * 
	 * @param date
	 * @param criteria
	 * @param alpha_min
	 * @param alpha_max
	 * @param alpha_interval
	 * @param betas_min
	 * @param betas_max
	 * @param testSetNum
	 * @param timeLimit
	 * @param sleepTime
	 * @param mutantFile_date
	 * @param mutantDetailDir
	 */
	public static void saveFDR_cost_ILPModel_offline(String date, String[] criteria, double alpha_min,
			double alpha_max, double alpha_interval, int[] betas_min, 
			int[] betas_max, int testSetNum, String[] H_L_Ds, long timeLimit, 
			long sleepTime, String mutantFile_date, String mutantDetailDir){
		
//		//1. build and solve bi-criteria ILP models via sequential rather than concurrent model 
//		buildAndSolveILPs_BiCriteria_concurrent(date, criteria, 
//				alpha_min, alpha_max, alpha_interval, betas_min, betas_max, 
//				testSetNum, H_L_Ds, timeLimit, sleepTime);
		
		//2. save the fault detection rate of reduced test suites in offline way
		String H_L_D ="";
		for(int k = 0; k < H_L_Ds.length; k ++){
			H_L_D = H_L_Ds[k];
			for(int i = 0; i < criteria.length; i ++){
				long start = System.currentTimeMillis();
				String criterion = criteria[i];
				boolean containHeader_mutant = false;
				boolean containHeader_testing = true;
				boolean single_enabled = false;
				
				HashMap<Double, HashMap<String, Double>> alpha_mutant_fdr = new 
					HashMap<Double, HashMap<String,Double>>();
				int sizeConstraint_min =  betas_min[i];
				int sizeConstraint_max =  betas_max[i];
				
				alpha_mutant_fdr = Reporter_Reduction.getFaultDetectionRate_detailed_BILP_offline(date, 
						criterion, mutantFile_date, containHeader_mutant, mutantDetailDir, containHeader_testing, 
						alpha_min, alpha_max, alpha_interval, sizeConstraint_min, sizeConstraint_max, 
						H_L_D, single_enabled);
				
				
				String saveFile = System.getProperty("user.dir") + "/src/ccr"
				+ "/experiment/Context-Intensity_backup/TestHarness/" + date
				+ "/ILPModel/"+ criterion+"/RA_"+H_L_D+"_FaultDetectionRate_AllEquivalencies.txt";
				Reporter_Reduction.saveToFile_alpha_mutant_fdrs(alpha_mutant_fdr, saveFile);
				
				HashMap<Double, ArrayList<Double>> alpha_times = Reporter_Reduction.getTimeCost_detailed_BILP_offline(date, 
						criterion, alpha_min, alpha_max, alpha_interval, sizeConstraint_min, sizeConstraint_max, 
						H_L_D, single_enabled);
				saveFile = System.getProperty("user.dir") + "/src/ccr"
				+ "/experiment/Context-Intensity_backup/TestHarness/" + date
				+ "/ILPModel/"+ criterion+"/RA_"+H_L_D+"_TimeCost_AllEquivalencies.txt";
				
				Reporter_Reduction.saveToFile_alpha_times(alpha_times, saveFile);
				long duration = System.currentTimeMillis() - start;
				System.out.println("[Reporter_Reduction.Main]It takes " + duration/(1000*60) + " mins for " + criterion);
			}	
		}
	}
	

	/**2010-03-18: build and solve the bi-criteria ILP model which tries to maximize CD while minimize the 
	 * test suite size
	 * 
	 * @param date
	 * @param criteria
	 * @param alpha_min: the max size of reduced test suites. Note that it should have the same size as that of criteria.
	 * @param alpha_max: the min size of reduced test suites. Note that it should have the same size as that of criteria.
	 * @param alpha_interval
	 * @param betas_min
	 * @param betas_max
	 * @param testSetNum
	 * @param timeLimit
	 * @param sleepTime
	 */
	public static void buildAndSolveILPs_BiCriteria_concurrent(String date_configurationFile, String criterion){
		String configFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/" +
		""+date_configurationFile+"/saveFDR_ILPModel_"+criterion+".txt";		
		try {
			
			//1. load parameters from configuration files 
			BufferedReader br = new BufferedReader(new FileReader(configFile));
			String str = br.readLine();
			String date = str.substring(str.indexOf(":")+1);

			str = br.readLine();
			String tmp = str.substring(str.indexOf(":")+1);			
			String[] criteria = tmp.split(",");

			str = br.readLine();
			tmp = str.substring(str.indexOf(":")+1);			
			String[] H_L_Ds = tmp.split(",");
			
			str = br.readLine();
			tmp = str.substring(str.indexOf(":")+1);			
			String[] beta_min_str = tmp.split(",");
			
			str = br.readLine();
			tmp = str.substring(str.indexOf(":")+1);			
			String[] beta_max_str = tmp.split(",");
			
			if(beta_max_str.length == criteria.length && 
					beta_min_str.length ==  criteria.length){
				
				int[] betas_min = new int[beta_min_str.length];
				for(int i = 0; i < betas_min.length; i ++){
					betas_min[i] = Integer.parseInt(beta_min_str[i]);
				}
				
				int[] betas_max = new int[beta_max_str.length];
				for(int i = 0; i < betas_max.length; i ++){
					betas_max[i] = Integer.parseInt(beta_max_str[i]);
				}						
				
				str = br.readLine();
				int testSetNum = Integer.parseInt(str.substring(str.indexOf(":")+1));
				
				str = br.readLine();
				long timeLimit = Long.parseLong(str.substring(str.indexOf(":")+1));
				if(timeLimit == 0){
					timeLimit = Long.MAX_VALUE;
				}
				
				str = br.readLine();
				long sleepTime = Long.parseLong(str.substring(str.indexOf(":")+1));
				
				str = br.readLine();
				double alpha_min = Double.parseDouble(str.substring(str.indexOf(":")+1));			
				
				str = br.readLine();
				double alpha_max = Double.parseDouble(str.substring(str.indexOf(":")+1));
				
				str = br.readLine();
				double alpha_interval = Double.parseDouble(str.substring(str.indexOf(":")+1));
			
				
//				str = br.readLine();
//				String mutantFile_date = str.substring(str.indexOf(":")+1);			
//				
//				str = br.readLine();
//				String mutantDetailDir = System.getProperty("user.dir") + "/src/ccr"
//				+ "/experiment/Context-Intensity_backup/TestHarness/" + str.substring(str.indexOf(":")+1);
				
				br.close();
				
				String H_L_D = "";
				for(int i = 0; i < criteria.length; i ++){
					String criterion_tmp = criteria[i];
					int beta_min = betas_min[i];
					int beta_max = betas_max[i];
					for(int j = beta_min; j < beta_max; j ++){
						int beta = j;
						for(int k = 0; k < H_L_Ds.length; k ++){
							H_L_D = H_L_Ds[k];
							ILPSolver.buildAndSolveILPs_BiCriteria_Manager_CD(date, criterion_tmp, alpha_min, alpha_max,
									alpha_interval, beta, testSetNum, H_L_D, timeLimit, sleepTime);
						}
					}
				}
				
			
			}else{
				System.out.println("Invalid paramters: " +
						"numbers of criteria and betas do not match with each other!");
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
	}

	
	public static void main(String[] args) {
		String instruction = args[0];
		
		if(instruction.equals("saveOracles")){
			//2010-03-01: save oracles into files
			//Typical input: saveOracles 20100301
			
			String date_testPool = args[1];
			String testcaseFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/" +
					""+date_testPool+"/TestPool.txt";
			TestSet testpool = Adequacy.getTestPool(testcaseFile, true);
			String _oracleFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/" + 
				   date_testPool + "/Oracle.txt";
			Oracle.getInstance("ccr.app.TestCFG2", testpool, _oracleFile);
			
		}else if(instruction.equals("getFailureRate")){
			//2010-03-01: get failure rate of specified faults. 
			//Usually, this method is invoked by a script. 
			//Typical inputs: getFailureRate 20091005 0 1
			
			String date = args[1];
			int startVersion = Integer.parseInt(args[2]);
			int endVersion = Integer.parseInt(args[3]);
			
			String versionPackageName = "testversion";
			String oracleClassName = "TestCFG2";
			
			String testcaseFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"+date+"/TestPool.txt";
			TestSet testpool = Adequacy.getTestPool(testcaseFile, true);
			String reportDir = "src/ccr/experiment/Context-Intensity_backup/TestHarness/" + date + "/";
			
			TestDriver.getFailureRate_efficient(versionPackageName, oracleClassName, 
					testpool, reportDir, startVersion, endVersion);
			
		}else if(instruction.equals("getAdequateTestSet")){
			//2010-03-18: load complex test suite construction parameters from configuration files
			//Typical input: getAdequateTestSet 20100315
			String date_configurationFile = "20100315";
			if(args.length == 2){
				date_configurationFile = args[1];
			}
			getAdequateTestSets(date_configurationFile);
			
		}else if(instruction.equals("saveTestingResults_TestSet")){
			//2010-03-01: save the testing effectiveness of test sets into "x_limit_load.txt"
			//Typical input: saveTestingResults_TestSet 20100301
			String date_configurationFile = "20100301";
			if(args.length == 2){
				date_configurationFile = args[1];
			}
			saveTestingResults_TestSets(date_configurationFile);
		}else if(instruction.equals("saveTestingEffectiveness_FixARTSize")){
			//2010-03-01: save the fault detection rate of adequate test suites 
			//for each fault and the min/max/mean fault detection rate of adequate test set
			//w.r.t a ART size
			//Typical input: saveTestingEffectiveness_FixARTSize 20100301 64
			
			String date = args[1];
			String size_ART = args[2];
			ResultAnalyzer.saveTestingPerfomanceOfAdequateTestSet(
					date, size_ART);
		}else if(instruction.equals("saveGenTime")){
			String date = "20100316";
			String size_ART = "8";
			
			if(args.length == 3){
				date = args[1];
				size_ART = args[2];	
			}
			
			saveGenTime(date, size_ART);
		}else if(instruction.equals("saveEffectivenessDifference")){
			String date = "20100314";
			String size_ART = "1";
			
			if(args.length == 3){
				date = args[1];
				size_ART = args[2];	
			}
			
			String[] size_ARTs = {"1", "2", "4", "8", "16", "32", "64", "70"};
			StringBuilder sb = new StringBuilder();
			for(int i = 0; i < size_ARTs.length; i ++){
				size_ART = size_ARTs[i];
				double[] threshold = new double[]{0.05, 0.1}; 
				sb.append(saveEffectivenessDifference(date, size_ART, threshold));	
			}
			String filename = "src/ccr/experiment/Context-Intensity_backup/TestHarness/" +
			""+date+"/effectivenessDiff.txt";
			Logger.getInstance().setPath(filename, false);
			Logger.getInstance().write(sb.toString());
			Logger.getInstance().close();
			
		}else if(instruction.equals("saveFDR_cost_ILPModel_offline")){
			//2010-03-18:build and solve ILP models, and then save the 
			//fault detection rate of reduced test suites in offline way 		
			String date_configurationFile = "20100315";
			String criterion = "AllPolicies";
			if(args.length == 2){
				date_configurationFile = args[1];
			}else if(args.length == 3){
				date_configurationFile = args[1];
				criterion = args[2];
			}
			
			saveFDR_cost_ILPModel(date_configurationFile, criterion);
//			String[] criteria = {"AllPolicies", "All1ResolvedDU", "All2ResolvedDU"};
//			for(int i = 0; i < criteria.length; i ++){
//				criterion = criteria[i];
//				saveFDR_cost_ILPModel(date_configurationFile, criterion);	
//			}
			
			
			System.exit(0);
		}else if(instruction.equals("buildAndSolveILPModels")){
			String date_configurationFile = "20100315";
			String criterion = "AllPolicies";
			if(args.length == 2){
				date_configurationFile = args[1];
			}else if(args.length == 3){
				date_configurationFile = args[1];
				criterion = args[2];
			}

			buildAndSolveILPs_BiCriteria_concurrent(date_configurationFile, criterion);
			
			String[] criteria = {"AllPolicies", "All1ResolvedDU", "All2ResolvedDU"};
//			for(int i = 0; i < criteria.length; i ++){
//				criterion = criteria[i];
//				buildAndSolveILPs_BiCriteria_concurrent(date_configurationFile, criterion);	
//			}
		}
	}

}
