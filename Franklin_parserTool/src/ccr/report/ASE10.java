package ccr.report;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import ccr.help.DataAnalyzeManager;
import ccr.help.DataDescriptionResult;
import ccr.help.TestSetComparator;
import ccr.help.TestSetStatistics;
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
	
	public static void saveEffectivenessDifference(String date, String size_ART, double[] diffs){
		String testSetFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/" +
		""+date+"/" + size_ART + "/PerValidTS.txt";
		
		try {
			BufferedReader br = new BufferedReader(new FileReader(testSetFile));
			String str = null;
			
			br.readLine(); //ignore headers
			
			HashMap<String, HashMap<Double, Integer>> criterion_diff_num =
				new HashMap<String, HashMap<Double,Integer>>();
			
			ArrayList<Double> AS_diffs = new ArrayList<Double>();
			ArrayList<Double> ASU_diffs = new ArrayList<Double>();
			ArrayList<Double> A2SU_diffs = new ArrayList<Double>();
			
			while((str = br.readLine())!= null){
				String[] strs = str.split("\t");
				if(strs.length == 26){
					String fault = strs[0];
					double AS_CA = Double.parseDouble(strs[3]);
					double AS_RA_H = Double.parseDouble(strs[4]);
					double AS_diff = AS_RA_H - AS_CA;
					AS_diffs.add(AS_diff);
					
					double ASU_CA = Double.parseDouble(strs[8]);
					double ASU_RA_H = Double.parseDouble(strs[9]);
					double ASU_diff = ASU_RA_H - ASU_CA;
					ASU_diffs.add(ASU_diff);
					
					double A2SU_CA = Double.parseDouble(strs[13]);
					double A2SU_RA_H = Double.parseDouble(strs[14]);
					double A2SU_diff = A2SU_RA_H - A2SU_CA;
					A2SU_diffs.add(A2SU_diff);	
				}
				
			}
			
			DecimalFormat format = new DecimalFormat("0.00");
			String[] criteria = {"AS", "ASU", "A2SU"};
			
			StringBuilder sb = new StringBuilder();
			
			sb.append("Criterion\t");			
			for(int k = 0; k < diffs.length; k ++){
				double diff = diffs[k];
				sb.append(">"+diff+"\t").append("-" + diff + " to " + diff +"\t").append("<-"+ diff + "\t");			
			}
			sb.append("\n");

			for(int i = 0; i < criteria.length; i ++){
				String criterion = criteria[i];
				sb.append( criterion+"\t");
				for(int k = 0; k < diffs.length; k ++){
					double diff = diffs[k];
					ArrayList criterion_diffs = null;
					if(criterion.equals("AS"))
						criterion_diffs = AS_diffs;
					else if(criterion.equals("ASU"))
						criterion_diffs = ASU_diffs;
					else if(criterion.equals("A2SU"))
						criterion_diffs = A2SU_diffs;
					
					HashMap<Double, Integer> threshold_counter = classifyDiff(criterion_diffs, diff);
					sb.append(threshold_counter.get(diff)+" (" + format.format((double)threshold_counter.get(diff)*100/(double)AS_diffs.size()) + "%)\t").
					append(threshold_counter.get(0.0)+" (" + format.format((double)threshold_counter.get(0.0)*100/(double)AS_diffs.size()) + "%)\t").
					append(threshold_counter.get(-diff)+" (" + format.format((double)threshold_counter.get(-diff)*100/(double)AS_diffs.size()) + "%)\t");
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
		
		//2010-03-15: list the detailed generation time and descriptions of these time
		//list the generation time for each testing criterion
//		for(int j = 0; j < testSetNum; j ++){
//			
//			for(int i = 0; i < criteria.length; i ++){
//				String criterion = criteria[i];
//				ArrayList<TestSet> testSets = criterion_TestSets.get(criterion);
//				sb.append(testSets.get(j).geneTime).append("\t");
//			}		
//			sb.append("\n");
//		}
//		
//		
//		sb.append("Criterion\t").append("min").append("\t").append("mean").append("\t").
//		append("median").append("\t").append("max").append("\t").append("std").append("\n");
//		
//		for(int i = 0; i < criteria.length; i ++){
//			String criterion = criteria[i];
//			
//			
//			ArrayList<TestSet> testSets = criterion_TestSets.get(criterion);
//			
//			double[] generationTime = new double[testSets.size()];
//			for(int j = 0; j < generationTime.length; j ++){
//				generationTime[j] = testSets.get(j).geneTime;
//			}
//			
//			DataDescriptionResult description = DataAnalyzeManager.getDataDescriptive(generationTime);
//			sb.append(rename[i]).append("\t").append(""+ description.min).append("\t").
//			append(description.mean).append("\t").append(""+description.median).append("\t").
//			append(description.max).append("\t").append(description.std).append("\n");						
//		}
		
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
			//2010-03-01:get adequate test sets
			//Typical input: getAdequateTestSet 100 0.7 0.9 20090919 AllPolicies -1 new random L 20
			int testSetNum = 100;
			double min_CI = 0.7;
			double max_CI = 0.9;
			String date = "20100315";
			String[] criteria = {"AllPolicies", "All1ResolvedDU", "All2ResolvedDU"};
			int testSuiteSize = -1 ;
			String[] oldOrNews = {"new", "old"};
			String randomOrCriterion = "random";
			String[] H_L_Rs = {"L", "H", "R"};
			int[] size_ARTs = {1, 8, 16, 32, 64};
			
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
			if(args.length == 11){ 
				//2010-03-16: when all parameters are given
				testSetNum = Integer.parseInt(args[1]);
				min_CI = Double.parseDouble(args[2]);
				max_CI = Double.parseDouble(args[3]);

				date = args[4];
				criterion = args[5];
				testSuiteSize = Integer.parseInt(args[6]);
				oldOrNew = args[7];
				randomOrCriterion = args[8];
				H_L_R = args[9];
				size_ART = Integer.parseInt(args[10]);
				
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
				
			}else if(args.length == 1){
				//2010-03-16: if none parameters are given, then generate adequate test suites for all criteria and all size_ART range 
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
			}else if(args.length == 2){
				//2010-03-16: generate test suites for a given testing criterion with respect to
				//all size_ART. This can facilitate the concurrent generation.
				
				criterion = args[1];
				
				for(int i = 0; i < size_ARTs.length; i++){ //for each ART_size
					size_ART = size_ARTs[i];
//					for(int j = 0; j < criteria.length; j ++){ //for each testing criterion
//						criterion = criteria[j];
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
//					}
				}
			}
			
			
						
			

			
			
		}else if(instruction.equals("saveTestingResults_TestSet")){
			//2010-03-01: save the testing effectiveness of test sets into "x_limit_load.txt"
			//Typical input: saveTestingResults_TestSet 20100301 AllPolicies -1 new random H 20 20100301 20100301
			
			String date_testSets = "20100301";
			String criterion = "AllPolicies";
			int testSuiteSize = -1;
			String oldOrNew = "old";
			String randomOrCriterion = "random";
			String H_L_R = "H";
			int size_ART = 64;
			String date_testPool = "20100301";
			String date_faultList = "20100301";
			if(args.length > 8){
				date_testSets = args[1];
				criterion = args[2];
				testSuiteSize = Integer.parseInt(args[3]);
				oldOrNew = args[4];
				randomOrCriterion = args[5];
				H_L_R = args[6];
				size_ART = Integer.parseInt(args[7]);
				date_testPool = args[8];
				date_faultList = args[9];
			}
			
			String testSetFile = TestSetStatistics.getTestSetFile(date_testSets, criterion, testSuiteSize, 
					oldOrNew, randomOrCriterion, H_L_R, size_ART);
			TestSet testSets[][] = new TestSet[1][];
			testSets[0] = Adequacy.getTestSets(testSetFile);
			
			String date_execHisotry = args[8];
			
			String saveFile = testSetFile.substring(0, testSetFile
					.indexOf("."))
					+ "_limited_load.txt";
			String faultListFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
					+ date_testPool+ "/FaultList.txt";

			// 1. load the fault list
			ArrayList faultList = new ArrayList();
			try {
				BufferedReader br = new BufferedReader(new FileReader(
						faultListFile));
				String line = null;
				while ((line = br.readLine()) != null) {
					faultList.add(line.trim());
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			// 2. test the specified faults
			TestDriver
					.test_load(testSets, faultList, date_execHisotry, saveFile);
			
		}else if(instruction.equals("saveTestingEffectiveness_FixARTSize")){
			//2010-03-01: save the min/max/mean fault detection rate of adequate test set
			//with respect to a ART size
			//Typical input: saveTestingEffectiveness_FixARTSize 20100301 64
			
			String date = args[1];
			String size_ART = args[2];
			ResultAnalyzer.saveTestingPerfomanceOfAdequateTestSet(
					date, size_ART);
		}else if(instruction.equals("saveGenTime")){
			String date = args[1];
			String size_ART = args[2];
			saveGenTime(date, size_ART);
		}else if(instruction.equals("saveEffectivenessDifference")){

			String date = "20100314";
			String size_ART = "64";
			
			if(args.length == 3){
				date = args[1];
				size_ART = args[2];	
			}
			
			double[] threshold = new double[]{0.1, 0.15, 0.2}; 
			saveEffectivenessDifference(date, size_ART, threshold);
		}
	}

}
