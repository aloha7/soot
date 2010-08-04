package ccr.report;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import ccr.help.DataAnalyzeManager;
import ccr.help.DataDescriptionResult;
import ccr.test.Logger;

public class ASE10_journal {
	
	
	public static String formatResults(HashMap<Double, Integer> threshold_counter, ArrayList<Double> criterion_diffs, double diff){
		StringBuilder sb = new StringBuilder();
		DecimalFormat format = new DecimalFormat("0.00");
		
		sb.append(threshold_counter.get(diff)+"(");
		sb.append(format.format((double)threshold_counter.get(diff)*100/(double)criterion_diffs.size()) + "%)\t");
		sb.append(threshold_counter.get(0.0)+"(");
		sb.append(format.format((double)threshold_counter.get(0.0)*100/(double)criterion_diffs.size()) + "%)\t");
		sb.append(threshold_counter.get(-diff)+"(");
		sb.append(format.format((double)threshold_counter.get(-diff)*100/(double)criterion_diffs.size()) + "%)\t");
		
		return sb.toString();
	}
	
	/**2010-06-18: we are interested in "<-diff",
	 *  ">diff" and "-diff<= <=diff"
	 * 
	 * @param date
	 * @param size_ART
	 * @param diffs
	 * @return
	 */
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
			
//			DecimalFormat format = new DecimalFormat("0.00");
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
				sb.append("-"+diff+" to " + diff + "\t");
				sb.append("< -" + diff +"\t");
			}
			sb.append("\n");
			
			//compare RA-H with CA
			for(int i = 0; i < criteria.length; i ++){
				String criterion = criteria[i];
				sb.append( criterion+"_RAH-CA\t");
				for(int k = 0; k < diffs.length; k ++){
					double diff = diffs[k];
					ArrayList<Double> criterion_diffs = null;
					if(criterion.equals("AS"))
						criterion_diffs = AS_RAH_CA_diffs;
					else if(criterion.equals("ASU"))
						criterion_diffs = ASU_RAH_CA_diffs;
					else if(criterion.equals("A2SU"))
						criterion_diffs = A2SU_RAH_CA_diffs;
					
					HashMap<Double, Integer> threshold_counter = ASE10.classifyDiff(criterion_diffs, diff);
					
					sb.append(formatResults(threshold_counter, criterion_diffs, diff));		
					
				}
				sb.append("\n");
			}
			
			//compare RA-H with RA-L
			for(int i = 0; i < criteria.length; i ++){
				String criterion = criteria[i];
				sb.append( criterion+"_RAH-RAL\t");
				for(int k = 0; k < diffs.length; k ++){
					double diff = diffs[k];
					ArrayList<Double> criterion_diffs = null;
					if(criterion.equals("AS"))
						criterion_diffs = AS_RAH_RAL_diffs;
					else if(criterion.equals("ASU"))
						criterion_diffs = ASU_RAH_RAL_diffs;
					else if(criterion.equals("A2SU"))
						criterion_diffs = A2SU_RAH_RAL_diffs;
					
					HashMap<Double, Integer> threshold_counter = ASE10.classifyDiff(criterion_diffs, diff);
					sb.append(formatResults(threshold_counter, criterion_diffs, diff));
				}
				sb.append("\n");
			}
			
			//compare RA-H with RA-R
			for(int i = 0; i < criteria.length; i ++){
				String criterion = criteria[i];
				sb.append( criterion+"_RAH-RAR\t");
				for(int k = 0; k < diffs.length; k ++){
					double diff = diffs[k];
					ArrayList<Double> criterion_diffs = null;
					if(criterion.equals("AS"))
						criterion_diffs = AS_RAH_RAR_diffs;
					else if(criterion.equals("ASU"))
						criterion_diffs = ASU_RAH_RAR_diffs;
					else if(criterion.equals("A2SU"))
						criterion_diffs = A2SU_RAH_RAR_diffs;
					
					HashMap<Double, Integer> threshold_counter = ASE10.classifyDiff(criterion_diffs, diff);
					sb.append(formatResults(threshold_counter, criterion_diffs, diff));

				}
				sb.append("\n");
			}
			
			//compare RA-H with Random
			for(int i = 0; i < criteria.length; i ++){
				String criterion = criteria[i];
				sb.append( criterion+"_RAH-Random\t");
				for(int k = 0; k < diffs.length; k ++){
					double diff = diffs[k];
					ArrayList<Double> criterion_diffs = null;
					if(criterion.equals("AS"))
						criterion_diffs = AS_RAH_Random_diffs;
					else if(criterion.equals("ASU"))
						criterion_diffs = ASU_RAH_Random_diffs;
					else if(criterion.equals("A2SU"))
						criterion_diffs = A2SU_RAH_Random_diffs;
					
					HashMap<Double, Integer> threshold_counter = ASE10.classifyDiff(criterion_diffs, diff);
					sb.append(formatResults(threshold_counter, criterion_diffs, diff));

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
	
	public static String getContextDiversity(String date, String[] size_ARTs){
		String testSetDir = "src/ccr/experiment/Context-Intensity_backup/TestHarness/" +
		""+date+"/";
		
		String[] criteria = {"AllPolicies", "All1ResolvedDU", "All2ResolvedDU"};
		String[] strategies = {"CA", "RA-H", "RA-L", "RA-R"};
		
		
		String str = null;
		
		HashMap<String, HashMap<String, Double>> criterion_sizeART_mean = 
			new HashMap<String, HashMap<String,Double>>();
		
		HashMap<String, HashMap<String, Double>> criterion_sizeART_std= 
			new HashMap<String, HashMap<String,Double>>();
		
		String criterion = null;
		for(int k = 0; k < size_ARTs.length; k ++){
			String size_ART = size_ARTs[k];
			
			
			for(int i = 0; i < criteria.length; i ++){
				for(int j = 0; j < strategies.length; j ++){
					criterion = criteria[i] + "_" +strategies[j];
					
					String testSetFile = null;
					if(size_ART.equals("128")){
						testSetFile = testSetDir + "/" + size_ART +
						"/" + criterion + "_70_CI.txt";
					}else if(size_ART.equals("Max")){
						testSetFile = testSetDir + "/" + size_ART +
						"/" + criterion + "_80_CI.txt";
					}else{
						testSetFile = testSetDir + "/" + size_ART +
						"/" + criterion + "_" + size_ART +"_CI.txt";			
					}
							
					
					try {
						BufferedReader br = new BufferedReader(new FileReader(testSetFile));
						br.readLine();//ignore headers
						
						ArrayList<Double> activations = new ArrayList<Double>();
						//extract the "context diversity"
						while((str = br.readLine())!= null){
							String[] strs = str.split("\t");						
							activations.add(Double.parseDouble(strs[3]));
						}
						
						DataDescriptionResult result = 
							DataAnalyzeManager.getDataDescriptive(activations);
						
						//record the results;
						HashMap<String, Double> sizeART_mean = null;
						if(criterion_sizeART_mean.containsKey(criterion)){
							sizeART_mean = criterion_sizeART_mean.get(criterion);
						}else{
							sizeART_mean = new HashMap<String, Double>();
						}						 
						sizeART_mean.put(size_ART, result.mean);
						criterion_sizeART_mean.put(criterion, sizeART_mean);
						
						HashMap<String, Double> sizeART_std = null;
						if(criterion_sizeART_std.containsKey(criterion)){
							sizeART_std = criterion_sizeART_std.get(criterion);
						}else{
							sizeART_std = new HashMap<String, Double>();
						}
						sizeART_std.put(size_ART, result.std);
						criterion_sizeART_std.put(criterion, sizeART_std);
						
					} catch (FileNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
		
	
			//save results for each criterion
		StringBuilder sb = new StringBuilder();
		
		sb.append("\t").append("Mean Context Diversity").append("\t\t\t\t\t\t").
			append("Standard Derivation of Context Diversity").append("\n");
		sb.append("\t");
		for(int k = 0; k < size_ARTs.length; k ++){
			sb.append(size_ARTs[k]).append("\t");	
		}
		
		for(int k = 0; k < size_ARTs.length; k ++){
			sb.append(size_ARTs[k]).append("\t");	
		}
		sb.append("\n");
		
		
		DecimalFormat mean_formatter = new DecimalFormat("0.0");
		DecimalFormat std_formatter = new DecimalFormat("0.00");
			for(int i = 0; i < criteria.length; i ++){
				for(int j = 0; j < strategies.length; j ++){
					
					//for each criterion
					criterion = criteria[i] + "_" + strategies[j];
					
					sb.append(criterion).append("\t");
					
					
					
					if(criterion_sizeART_mean.containsKey(criterion)){
						HashMap<String, Double> sizeART_meanActivations = 
							criterion_sizeART_mean.get(criterion);
						
						
						
						for(int k = 0; k < size_ARTs.length; k ++){
							String size_ART = size_ARTs[k];
							double mean = sizeART_meanActivations.get(size_ART);
							sb.append(mean_formatter.format(mean)).append("\t");
						}	
					}else{
						System.out.println("missing mean situation activation of:"+ criterion);
					}
					
					if(criterion_sizeART_std.containsKey(criterion)){
						HashMap<String, Double> sizeART_stdActivations = 
							criterion_sizeART_std.get(criterion);
						
						for(int k = 0; k < size_ARTs.length; k ++){
							String size_ART = size_ARTs[k];
							double std = sizeART_stdActivations.get(size_ART);
							sb.append(std_formatter.format(std)).append("\t");
						}	
					}else{
						System.out.println("missing std. situation activation of:"+ criterion);
					}
					sb.append("\n");
				}
			}
		
		return sb.toString();
	}
	
	public static String getSituationActivation(String date, String[] size_ARTs){
		String testSetDir = "src/ccr/experiment/Context-Intensity_backup/TestHarness/" +
		""+date+"/";
		
		String[] criteria = {"AllPolicies", "All1ResolvedDU", "All2ResolvedDU"};
		String[] strategies = {"CA", "RA-H", "RA-L", "RA-R"};
		
		
		String str = null;
		
		HashMap<String, HashMap<String, Double>> criterion_sizeART_meanActivations = 
			new HashMap<String, HashMap<String,Double>>();
		
		HashMap<String, HashMap<String, Double>> criterion_sizeART_stdActivations = 
			new HashMap<String, HashMap<String,Double>>();
		
		String criterion = null;
		for(int k = 0; k < size_ARTs.length; k ++){
			String size_ART = size_ARTs[k];
//			String testSetFile = testSetDir + "/" + size_ART + "/";
			
			
			for(int i = 0; i < criteria.length; i ++){
				for(int j = 0; j < strategies.length; j ++){
					criterion = criteria[i] + "_" +strategies[j];
					
					String testSetFile = null;
					if(size_ART.equals("128")){
						testSetFile = testSetDir + "/" + size_ART +
						"/" + criterion + "_70_CI.txt";
					}else if(size_ART.equals("Max")){
						testSetFile = testSetDir + "/" + size_ART +
						"/" + criterion + "_80_CI.txt";
					}else{
						testSetFile = testSetDir + "/" + size_ART +
						"/" + criterion + "_" + size_ART +"_CI.txt";			
					}
							
					
					try {
						BufferedReader br = new BufferedReader(new FileReader(testSetFile));
						br.readLine();//ignore headers
						
						ArrayList<Double> activations = new ArrayList<Double>();
						//extract the "Situation Activations"
						while((str = br.readLine())!= null){
							String[] strs = str.split("\t");						
							activations.add(Double.parseDouble(strs[4]));
						}
						
						DataDescriptionResult result = 
							DataAnalyzeManager.getDataDescriptive(activations);
						
						//record the results;
						HashMap<String, Double> sizeART_meanActivations = null;
						if(criterion_sizeART_meanActivations.containsKey(criterion)){
							sizeART_meanActivations = criterion_sizeART_meanActivations.get(criterion);
						}else{
							sizeART_meanActivations = new HashMap<String, Double>();
						}						 
						sizeART_meanActivations.put(size_ART, result.mean);
						criterion_sizeART_meanActivations.put(criterion, sizeART_meanActivations);
						
						HashMap<String, Double> sizeART_stdActivations = null;
						if(criterion_sizeART_stdActivations.containsKey(criterion)){
							sizeART_stdActivations = criterion_sizeART_stdActivations.get(criterion);
						}else{
							sizeART_stdActivations = new HashMap<String, Double>();
						}
						sizeART_stdActivations.put(size_ART, result.std);
						criterion_sizeART_stdActivations.put(criterion, sizeART_stdActivations);
						
					} catch (FileNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
		
	
			//save results for each criterion
		StringBuilder sb = new StringBuilder();
		
		sb.append("\t").append("Mean Situation Activations").append("\t\t\t\t\t\t").
			append("Standard Derivation of Situation Activation").append("\n");
		sb.append("\t");
		for(int k = 0; k < size_ARTs.length; k ++){
			sb.append(size_ARTs[k]).append("\t");	
		}
		
		for(int k = 0; k < size_ARTs.length; k ++){
			sb.append(size_ARTs[k]).append("\t");	
		}
		sb.append("\n");
		
		
		DecimalFormat mean_formatter = new DecimalFormat("0.0");
		DecimalFormat std_formatter = new DecimalFormat("0.00");
			for(int i = 0; i < criteria.length; i ++){
				for(int j = 0; j < strategies.length; j ++){
					
					//for each criterion
					criterion = criteria[i] + "_" + strategies[j];
					
					sb.append(criterion).append("\t");
					
					
					
					if(criterion_sizeART_meanActivations.containsKey(criterion)){
						HashMap<String, Double> sizeART_meanActivations = 
							criterion_sizeART_meanActivations.get(criterion);
						
						
						
						for(int k = 0; k < size_ARTs.length; k ++){
							String size_ART = size_ARTs[k];
							double mean = sizeART_meanActivations.get(size_ART);
							sb.append(mean_formatter.format(mean)).append("\t");
						}	
					}else{
						System.out.println("missing mean situation activation of:"+ criterion);
					}
					
					if(criterion_sizeART_stdActivations.containsKey(criterion)){
						HashMap<String, Double> sizeART_stdActivations = 
							criterion_sizeART_stdActivations.get(criterion);
						
						for(int k = 0; k < size_ARTs.length; k ++){
							String size_ART = size_ARTs[k];
							double std = sizeART_stdActivations.get(size_ART);
							sb.append(std_formatter.format(std)).append("\t");
						}	
					}else{
						System.out.println("missing std. situation activation of:"+ criterion);
					}
					sb.append("\n");
				}
			}
		
		return sb.toString();
		
	}
	
	public static void motivationExample(int[] locationTrace){
		if(locationTrace.length ==4){
			int[] statements = new int[4];
			if(locationTrace[0] == 1){
				statements[0]=6;
			}else if(locationTrace[0]==0){
				statements[0]=7;
			}
			
			
			if(locationTrace[1] == 2){
				statements[1]=11;
			}else if(locationTrace[1]==1){
				statements[1]=13;
			}else if(locationTrace[1]==0){
				statements[1]=14;
			}
			
			if(locationTrace[2] == 3){
				statements[2]=18;
			}else if(locationTrace[2]==2){
				statements[2]=20;
			}else if(locationTrace[2]==1){
				statements[2]=21;
			}
			
			
			if(locationTrace[3] == 3){
				statements[3]=25;
			}else if(locationTrace[3]==2){
				statements[3]=27;
			}else if(locationTrace[3]==1){
				statements[3]=28;
			}
			
			for(int i =0; i < locationTrace.length; i++){
				System.out.println(statements[i]);
			}
		}
	}

	
	
	public static void main(String[] args) {
		
		String instruction;
		if(args.length ==0){
			instruction = "motivationExample";
		}else{
			instruction = args[0];
		}
		
		if(instruction.equals("saveEffectivenessDifference")){
			String date = "20100618_ASEJournal";
			String size_ART = "1";
			
			if(args.length == 3){
				date = args[1];
				size_ART = args[2];	
			}
	
			String[] size_ARTs = {"1", "2", "4", "8", "16", "32", "64", "128","Max"};
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
		}else if(instruction.equals("motivationExample")){
			int[] locationTrace = new int[]{1, 0, 1, 1};
			motivationExample(locationTrace);
		}else if(instruction.equals("getSituationActivations")){

			String date = "20100618_ASEJournal";
			String[] size_ARTs = {"1", "2", "4", "8", "16", "32", "64", "128","Max"};
			
			StringBuilder sb = new StringBuilder();
			sb.append(getSituationActivation(date, size_ARTs));
			
			String filename = "src/ccr/experiment/Context-Intensity_backup/TestHarness/" +
			""+date+"/situationActivations.txt";
			Logger.getInstance().setPath(filename, false);
			Logger.getInstance().write(sb.toString());
			Logger.getInstance().close();
		}else if(instruction.equals("getContextDiversity")){

			String date = "20100618_ASEJournal";
			String[] size_ARTs = {"1", "2", "4", "8", "16", "32", "64", "128","Max"};
			
			StringBuilder sb = new StringBuilder();
			sb.append(getContextDiversity(date, size_ARTs));
			
			String filename = "src/ccr/experiment/Context-Intensity_backup/TestHarness/" +
			""+date+"/ContextDiversity.txt";
			Logger.getInstance().setPath(filename, false);
			Logger.getInstance().write(sb.toString());
			Logger.getInstance().close();
		}
	}

}
