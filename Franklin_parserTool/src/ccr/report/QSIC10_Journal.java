package ccr.report;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

import ccr.help.DataAnalyzeManager;
import ccr.help.DataDescriptionResult;
import ccr.test.Logger;

public class QSIC10_Journal {

	int testPoolSize = 20000;
	HashMap<String, MutantOperator> MOList;
	HashMap<String, Fault> fault_interest;
	
//	public void getCDStatistics(String mutantFile, double lower_fr, double upper_fr, 
//			String testDetailDir, String saveDir){
//		
//		loadMutants(mutantFile, lower_fr, upper_fr);
//		loadFailedTestCases(testDetailDir);
//		
//		StringBuilder sb = new StringBuilder();
//		StringBuilder sb_nonOverlapping = new StringBuilder();
//		
//		sb.append("MutantOperator").append("\t").append("FaultNo").
//				append("\t").append("FailedTestCaseNo").append("\t").
//				append("Min").append("\t").append("Median").append("\t").
//				append("Mean").append("\t").append("Max").append("Std").append("\n");
//		
//		sb_nonOverlapping.append("MutantOperator").append("\t").append("FaultNo").
//		append("\t").append("FailedTestCaseNo").append("\t").
//		append("Min").append("\t").append("Median").append("\t").
//		append("Mean").append("\t").append("Max").append("Std").append("\n");
//		
//		Iterator<MutantOperator> ite_MO = MOList.values().iterator();
//		while(ite_MO.hasNext()){
//			
//			MutantOperator MO = ite_MO.next();
//			sb.append(MO.id).append("\t");
//			sb.append(MO.mutants.size()).append("\t");
//			ArrayList<Double> failedTestCases = MO.getCD_FailedTestCases();
//			sb.append(failedTestCases.size()).append("\t");
//			
//			DataDescriptionResult result = DataAnalyzeManager.getDataDescriptive(failedTestCases);
//			sb.append(result.min).append("\t");
//			sb.append(result.median).append("\t");
//			sb.append(result.mean).append("\t");
//			sb.append(result.max).append("\t");
//			sb.append(result.std).append("\n");
//			
//			sb_nonOverlapping.append(MO.id).append("\t");
//			sb_nonOverlapping.append(MO.mutants.size()).append("\t");
//			ArrayList<Double> failedTestCases_nonOverlapping = MO.getCD_FailedTestCases();
//			sb_nonOverlapping.append(failedTestCases_nonOverlapping.size()).append("\t");
//			
//			DataDescriptionResult result = DataAnalyzeManager.getDataDescriptive(failedTestCases);
//			sb.append(result.min).append("\t");
//			sb.append(result.median).append("\t");
//			sb.append(result.mean).append("\t");
//			sb.append(result.max).append("\t");
//			sb.append(result.std).append("\n");
//		}
//		
//		String filename = saveDir + "\\" + "mutantOperator_CD.txt";
//		Logger.getInstance().setPath(filename, false);
//		Logger.getInstance().write(sb.toString());
//		Logger.getInstance().close();
//		
//		filename = saveDir + "\\" + "mutantOperator_CD_nonOverlapping.txt";
//		Logger.getInstance().setPath(filename, false);
//		Logger.getInstance().write(sb_nonOverlapping.toString());
//		Logger.getInstance().close();
//	}
	
	public void analyzeCD(String mutantFile, double lower_fr, double upper_fr, 
			String testDetailDir, String saveDir){
		
		loadMutants(mutantFile, lower_fr, upper_fr);
		loadFailedTestCases(testDetailDir);
		
		StringBuilder sb = new StringBuilder();
		StringBuilder sb_nonOverlapping = new StringBuilder();
		
		sb.append("MutantOperator").append("\t").append("FaultNo").
				append("\t").append("FailedTestCaseNo").append("\t").append("CDs").append("\n");
		sb_nonOverlapping.append("MutantOperator").append("\t").append("FaultNo").
				append("\t").append("FailedTestCaseNo").append("\t").append("CDs").append("\n");
		
		Iterator<MutantOperator> ite_MO = MOList.values().iterator();
		while(ite_MO.hasNext()){
			MutantOperator MO = ite_MO.next();
			sb.append(MO.toString()).append("\n");
			sb_nonOverlapping.append(MO.toString_nonOverlapping()).append("\n");
		}
		
		DecimalFormat format = new DecimalFormat("0.00");
		String filename = saveDir + "/" + "mutantOperator_CD_" +format.format(lower_fr)
			+"_" + format.format(upper_fr)+ ".txt";
		Logger.getInstance().setPath(filename, false);
		Logger.getInstance().write(sb.toString());
		Logger.getInstance().close();
		
		
		filename = saveDir + "/" + "mutantOperator_CD_nonOverlapping_"+format.format(lower_fr)
			+"_" + format.format(upper_fr)+".txt";
		Logger.getInstance().setPath(filename, false);
		Logger.getInstance().write(sb_nonOverlapping.toString());
		Logger.getInstance().close();
	}
	
	/**
	 * 
	 * @param mutantFile
	 * @param lower_fr: exclusive
	 * @param upper_fr: exclusive
	 */
	public void loadMutants(String mutantFile, double lower_fr, double upper_fr){
		File file = new File(mutantFile);
		if(file.exists()){
			MOList = new HashMap<String, MutantOperator>();			
			fault_interest = new HashMap<String, Fault>();
			
			try {
				BufferedReader br = new BufferedReader(new FileReader(file));
				String line;
				while((line = br.readLine())!= null){
					String[] strs = line.split("\t");
					String id = strs[0].substring(0,3); //we use the first chars as the ID of mutation operator
					
					if(!MOList.containsKey(id)){
						MOList.put(id, new MutantOperator(id));
					}
					
					String faultId = strs[1];
					double failureRate = Double.parseDouble(strs[2]);
					
					if(failureRate < upper_fr && failureRate > lower_fr){
						Fault fault = new Fault(faultId, failureRate);
						MOList.get(id).addMutant(fault);
						
						if(!fault_interest.containsKey(faultId)){
							fault_interest.put(faultId, fault);
						}
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
			System.out.println("MutantFile does not exist: " + mutantFile);
		}
	}
	
	public void loadFailedTestCases(String testDetailDir){		
		if(fault_interest != null){
			
			File dir = new File(testDetailDir);
			
			if(dir.exists()){
				  
				Iterator<String> ite = fault_interest.keySet().iterator();
				
				while(ite.hasNext()){
					String faultId = ite.next();
					File file = new File(dir + "/" + "detailed_" + faultId + "_" 
						+ (Integer.parseInt(faultId) + 1) + ".txt");
					if(file.exists()){
						try {
							
							BufferedReader br = new BufferedReader(new FileReader(file));
							String line = null;
							
							//ignore file headers
							br.readLine();
							
							while((line = br.readLine())!= null){
								
								String[] strs = line.split("\t");
								String testcase = strs[1];
								String PorF = strs[2];
								double CD = Double.parseDouble(strs[3]);
								if(!PorF.equals("P")){ //failed test cases
									Fault fault = fault_interest.get(faultId);
									fault.addTestCase(testcase, CD);
								}
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
			}else{
				System.out.println("test detail directory does not exist: " + testDetailDir);
			}
		}else{
			System.out.println("invoke loadMutants before loading testDetail");
		}
	}
	
	
	
	
	class MutantOperator{ //group mutants by the mutant operators
		String id;
		HashMap<String, Fault> mutants;
		
		public MutantOperator(String id){
			this.id = id;
			mutants = new HashMap<String, Fault>();
		}
		
		public boolean addMutant(Fault fault){
			if(!mutants.containsKey(fault.id)){
				mutants.put(fault.id, fault);
				return true;
			}
			return false;
		}
		
		public ArrayList<Double> getCD_FailedTestCases(){
			
			ArrayList<Double> cdList = new ArrayList<Double>();			
			Iterator<Fault> faults = mutants.values().iterator();
			while(faults.hasNext()){
				Fault fault = faults.next();
				if(fault.validateData(testPoolSize)){
					cdList.addAll(fault.testCase_CD.values());	
				}				
			}
			return cdList;
		}
		
		public ArrayList<Double> getCD_FailedTestCases_NonOverlapping(){
			
			HashMap<String, Double> uniqueTestCases_CD = new HashMap<String, Double>();
			Iterator<Fault> faults = mutants.values().iterator();
			while(faults.hasNext()){
				Fault fault = faults.next();
				if(fault.validateData(testPoolSize)){
					Iterator<String> failedTestCases = fault.testCase_CD.keySet().iterator();
					while(failedTestCases.hasNext()){
						String failedTestCase = failedTestCases.next();
						if(!uniqueTestCases_CD.containsKey(failedTestCase)){
							uniqueTestCases_CD.put(failedTestCase, fault.testCase_CD.get(failedTestCase));
						}
					}	
				}				
			}
			return new ArrayList(Arrays.asList(uniqueTestCases_CD.values().toArray(new Double[0])));
		}

		public String toString(){
			StringBuilder sb = new StringBuilder();
			sb.append(id).append("\t").append(mutants.size()).append("\t");
			
			ArrayList<Double> validateCDs = getCD_FailedTestCases();
			sb.append(validateCDs.size()).append("\t");
			for(int i = 0; i < validateCDs.size(); i ++){
				sb.append(validateCDs.get(i)).append("\t");
			}			
			return sb.toString();
		}
		
		public String toString_nonOverlapping(){
			StringBuilder sb = new StringBuilder();
			sb.append(id).append("\t").append(mutants.size()).append("\t");
			
			ArrayList<Double> validateCDs = getCD_FailedTestCases_NonOverlapping();
			sb.append(validateCDs.size()).append("\t");
			for(int i = 0; i < validateCDs.size(); i ++){
				sb.append(validateCDs.get(i)).append("\t");
			}			
			return sb.toString();
		}
	}
	
	class Fault{
		String id;
		double failureRate;
		HashMap<String, Double> testCase_CD;//keep CD values of failed test cases
		
		public Fault(String id, double failureRate){
			this.id = id;
			this.failureRate = failureRate;
			testCase_CD = new HashMap<String, Double>();
		}
		
		public boolean addTestCase(String testcase, double CD){
			if(!testCase_CD.containsKey(testcase)){
				testCase_CD.put(testcase, CD);
				return true;
			}else{
				return false;
			}
		}
		
		public boolean validateData(int testPoolSize){
			if(Math.abs(failureRate * testPoolSize - testCase_CD.size()) > 0.0001){
				System.out.println("Inconsistent failed test case number");
				return false;
			}
			return true;
		}
	}
	
	
	public static void main(String[] args) {
		if(args.length <2){
			System.out.println("Please specify the upper and lower bound of failure rates of mutants");			
		}else{
			String saveDir = System.getProperty("user.dir") + "/src/ccr" +
			"/experiment/Context-Intensity_backup/TestHarness" +
			"/QSIC_Journal/MutantOperator_CD/";
			
			
			String mutantFile = saveDir + "MO_fault_FR.txt";
			
			double lower_fr = Double.parseDouble(args[0]);
			double upper_fr = Double.parseDouble(args[1]);
			
			String testDetailDir = System.getProperty("user.dir") + "/src/ccr" +
					"/experiment/Context-Intensity_backup/TestHarness" +
					"/20100121/Mutant/";
			
			new QSIC10_Journal().analyzeCD(mutantFile, lower_fr, upper_fr, testDetailDir, saveDir);			
		}
		
	}

}
