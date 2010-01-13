package ccr.help;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import ccr.test.Logger;

public class ResultInterpreter {

	/**2010-01-13:load the all-level mutant number killed 
	 * by each test case
	 * 
	 * @param date
	 * @param containHeader
	 * @param mutantNumber
	 * @return: testcase(String) -> mutation score (Double)
	 */
	public static HashMap<String, Double> loadMutantScore_AllLevel(String date, boolean containHeader, int mutantNumber){		
		HashMap<String, Double> tc_ms = new HashMap<String, Double>();
		String file = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
			+ date + "/TestCaseDetails_"+mutantNumber+".txt";
		File tmp = new File(file);
		if(tmp.exists()){
			try {
				BufferedReader br = new BufferedReader(new FileReader(file));
				if(containHeader)
					br.readLine();
				
				String str = null;
				while((str = br.readLine())!= null){
					String[] strs = str.split("\t");
					String tc = strs[0];
					int mutantKilled = Integer.parseInt(strs[1]);
					double mutantScore = Double.parseDouble(
							new DecimalFormat("0.000000000").format(
									(double)mutantKilled/(double)mutantNumber));
					tc_ms.put(tc, mutantScore);
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
		return tc_ms;
	}
	
	/**2010-01-13:group test cases by the length firstly, then by their context diversity values
	 * 
	 * @param date
	 * @param containHeader
	 * @param alpha
	 * @return: length =>(CD -> test case lists)*
	 */
	public static HashMap<Integer, HashMap<Integer, ArrayList>> groupTestCase_Length(String date, boolean containHeader, double alpha){
		HashMap<Integer, HashMap<Integer, ArrayList>> len_CD_tclist = new 
			HashMap<Integer, HashMap<Integer,ArrayList>>();
		
		String file = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
			+ date + "/TestPool_"+ new DecimalFormat("0.0000").format(alpha) + ".txt";
		File tmp = new File(file);
		if(tmp.exists()){
			try {
				BufferedReader br = new BufferedReader(new FileReader(file));
				if(containHeader)
					br.readLine();
				
				String str = null;
				while((str = br.readLine())!= null){
					String[] strs = str.split("\t");
					if(strs.length >= 3){
						String tc = strs[0];
						int length = (int)Double.parseDouble(strs[1]);
						int CD = (int)Double.parseDouble(strs[2]);
						
						if(len_CD_tclist.containsKey(length)){
							HashMap<Integer, ArrayList> CD_tclist = len_CD_tclist.get(length);
							if(CD_tclist.containsKey(CD)){
								ArrayList tc_list = CD_tclist.get(CD);
								tc_list.add(tc);
								CD_tclist.put(CD, tc_list);
								len_CD_tclist.put(length, CD_tclist);
							}else{
								ArrayList tc_list = new ArrayList();
								if(!tc_list.contains(tc)){
									tc_list.add(tc);
									CD_tclist.put(CD, tc_list);
									len_CD_tclist.put(length, CD_tclist);	
								}					
							}
						}else{
							ArrayList tcList = new ArrayList();
							tcList.add(tc);
							HashMap<Integer, ArrayList> CD_tclist = new HashMap<Integer, ArrayList>();
							CD_tclist.put(CD, tcList);
							len_CD_tclist.put(length, CD_tclist);
						}
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
		}
		
		
		return len_CD_tclist;
	}
	
	/**2010-01-13:Given a set of test cases grouped by length and CD, and mutant score of each test case,
	 * we can get the mutation score of test cases grouped by length and CD. This is used
	 * to study the correlation between CD and mutation score when length is fixed
	 * 
	 * @param len_CD_tcList
	 * @param tc_mutationscore
	 * @return
	 */
	public static HashMap<Integer, HashMap<Integer, Double>> combine(HashMap<Integer, HashMap<Integer, ArrayList>> len_CD_tcList,
					HashMap<String, Double> tc_mutationscore){
		HashMap<Integer, HashMap<Integer, Double>> len_CD_mutationScore = 
										new HashMap<Integer, HashMap<Integer, Double>>();
		if(len_CD_tcList.size() > 0 && tc_mutationscore.size() > 0){
			Integer[] lengths= (Integer[])len_CD_tcList.keySet().toArray(new Integer[0]);
			Arrays.sort(lengths);
			for(int i = 0; i < lengths.length; i ++){
				int length = lengths[i];
				HashMap<Integer, ArrayList> CD_tcList = len_CD_tcList.get(length);
				Integer[] cdList = (Integer[])CD_tcList.keySet().toArray(new Integer[0]);
				Arrays.sort(cdList);
				for(int j = 0; j < cdList.length; j ++){
					//print each CD-> test case size
					int CD = cdList[j];						
					ArrayList tcList = CD_tcList.get(CD);
					double sum = 0.0;
					for(int k = 0; k < tcList.size(); k ++){
						String tc = (String)tcList.get(k);
						sum += tc_mutationscore.get(tc);
					}
					//1.get the avereage mutation score
					double avg_ms = Double.parseDouble(
							new DecimalFormat("0.000000000").format(sum/tcList.size()));
					
					//2.save it into the HashMap
					if(len_CD_mutationScore.containsKey(length)){
						HashMap<Integer, Double> CD_MS = len_CD_mutationScore.get(length);
						if(!CD_MS.containsKey(CD)){
							CD_MS.put(CD, avg_ms);
							len_CD_mutationScore.put(length, CD_MS);						
						}
					}else{					
						HashMap<Integer, Double> CD_MS = new HashMap<Integer, Double>();
						CD_MS.put(CD, avg_ms);
						len_CD_mutationScore.put(length, CD_MS);
					}
				}
			}
		}
		
		return len_CD_mutationScore;
	}
	
	
	
	public static String printTestCaseGroupByLength(HashMap<Integer, HashMap<Integer, ArrayList>> len_CD_tcList){
		StringBuilder sb = new StringBuilder();
		Integer[] lengths= (Integer[])len_CD_tcList.keySet().toArray(new Integer[0]);
		Arrays.sort(lengths);
		for(int i = 0; i < lengths.length; i ++){
			//print each length
			int length = lengths[i];
			sb.append("Length:").append(length).append("\n");
			HashMap<Integer, ArrayList> CD_tcList = len_CD_tcList.get(length);
			Integer[] cdList = (Integer[])CD_tcList.keySet().toArray(new Integer[0]);
			Arrays.sort(cdList);
			for(int j = 0; j < cdList.length; j ++){
				//print each CD-> test case size
				int CD = cdList[j];				
				sb.append(CD).append("\t").append(CD_tcList.get(CD).size()).append("\n");
			}
		}
		System.out.println(sb.toString());
		return sb.toString();
	}
	
	/**2010-01-13:print the length, CD, mutant scores and the number of test cases
	 * sharing the same CD, but also need to solve the slope of linear regression
	 * between CD and mutant score
	 * 
	 * @param len_CD_MS
	 * @param len_CD_tcList
	 * @return
	 */
	public static OutputFormat print(HashMap<Integer, HashMap<Integer, Double>> len_CD_MS, 
			HashMap<Integer, HashMap<Integer, ArrayList>> len_CD_tcList){
		OutputFormat format = new OutputFormat();
		StringBuilder sb = new StringBuilder();
		Integer[] lengths= (Integer[])len_CD_MS.keySet().toArray(new Integer[0]);
		Arrays.sort(lengths);
		for(int i = 0; i < lengths.length; i ++){
			//print each length
			int length = lengths[i];
			sb.append("Length:").append(length).append("\n");
			HashMap<Integer, Double> CD_MS = len_CD_MS.get(length);
			Integer[] cdList = (Integer[])CD_MS.keySet().toArray(new Integer[0]);
			Arrays.sort(cdList);
			
			//get the parameters for linear regression model
			double sum_CD = 0.0;
			double sum_MS = 0.0;
			double sum_CDByMS = 0.0;
			double sum_CD_Squre = 0.0;
			int n = 0;
			for(int j = 0; j < cdList.length; j ++){
				//print each CD-> test case size
				int CD = cdList[j];
				//get the size
				int size = len_CD_tcList.get(length).get(CD).size();
				double MS = CD_MS.get(CD);
				
				
				sb.append(size).append("\t").append(CD).append("\t").append(MS).append("\n");
				if(size >= 5){
					sum_CD += CD;
					sum_MS +=  MS;
					sum_CD_Squre += CD *CD;
					sum_CDByMS += CD * MS;
					n ++;
				}
			}					
			double a =  (n *sum_CDByMS - sum_CD * sum_MS )/(n*sum_CD_Squre - sum_CD*sum_CD);
			if(a < 0){
				format.negativeLength.add(length);
			}
			sb.append(a).append("\n");
		}
//		System.out.println(sb.toString());
		format.content = sb.toString();
		return format;
	}
	
	public static String print(HashMap<Integer, HashMap<Integer, Double>> len_CD_MS){
		StringBuilder sb = new StringBuilder();
		Integer[] lengths= (Integer[])len_CD_MS.keySet().toArray(new Integer[0]);
		Arrays.sort(lengths);
		for(int i = 0; i < lengths.length; i ++){
			//print each length
			int length = lengths[i];
			sb.append("Length:").append(length).append("\n");
			HashMap<Integer, Double> CD_MS = len_CD_MS.get(length);
			Integer[] cdList = (Integer[])CD_MS.keySet().toArray(new Integer[0]);
			Arrays.sort(cdList);
			for(int j = 0; j < cdList.length; j ++){
				//print each CD-> test case size
				int CD = cdList[j];				
				sb.append(CD).append("\t").append(CD_MS.get(CD)).append("\n");
			}
		}
//		System.out.println(sb.toString());
		return sb.toString();
	}
	
	public static void saveCorrelationToFile(String date, double alpha){
		StringBuilder sb = new StringBuilder();		
		boolean containHeader = true;
		int mutantNumber = 1934;
		HashMap<String, Double> tc_mutationscore = 
			ResultInterpreter.loadMutantScore_AllLevel(date, containHeader, mutantNumber);
		
	
		HashMap<Integer, HashMap<Integer, ArrayList>> len_CD_tcList = 
			ResultInterpreter.groupTestCase_Length(date, containHeader, alpha);
		HashMap<Integer, HashMap<Integer, Double>> len_CD_MS = 
			ResultInterpreter.combine(len_CD_tcList, tc_mutationscore);
		if(len_CD_tcList.size() > 0  && len_CD_MS.size() >0){
			OutputFormat format= ResultInterpreter.print(len_CD_MS, len_CD_tcList);
			format.alpah = alpha;
			
			if(format.negativeLength.size() == 0){
				sb.append("*********************************************");
				sb.append("Find a feasible alpha:" + alpha);
				sb.append("*********************************************");
				
//				System.out.println("*********************************************");
//				System.out.println("Find a feasible alpha:" + alpha);
//				System.out.println("*********************************************");
			}else{
				sb.append("Alpha:" + 
						new DecimalFormat("0.0000").format(format.alpah)
						+ ";"+ format.negativeLength.size() 
						+" lengths sharing negative slopes:");
				
//				System.out.print("Alpha:" + 
//						format.alpah + ";"+ format.negativeLength.size() 
//						+" lengths sharing negative slopes:");
				for(int k = 0; k < format.negativeLength.size(); k ++){
					sb.append(format.negativeLength.get(k) + "\t");
//					System.out.print(format.negativeLength.get(k) + "\t");
				}
				sb.append("\n");
//				System.out.println("\n");
			}
			
			
			String file =  "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
				+ date + "/FixLen_CI_MS_"+ new DecimalFormat("0.0000").format(alpha) + ".txt";
			Logger.getInstance().setPath(file, false);
			Logger.getInstance().write(format.content);
			Logger.getInstance().close();	
			
			String summaryFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
				+ date + "/CI_MS_CorrelationSummary.txt";
			Logger.getInstance().setPath(summaryFile, true);
			Logger.getInstance().write(sb.toString());
			Logger.getInstance().close();	
		}
	}
	
	public static void main(String[] args) {
		String instruction = args[0];
		
		if(instruction.equals("getCorrelation_CD_MS_single")){
			String date = args[1];
			double alpha = Double.parseDouble(args[2]);
			ResultInterpreter.saveCorrelationToFile(date, alpha);
		}else if(instruction.equals("getCorrelation_CD_MS_multiple")){
			String date = args[1];
			double alpha_min = Double.parseDouble(args[2]);//inclusive
			double alpha_max = Double.parseDouble(args[3]);//inclusive
			double alpha_interval = Double.parseDouble(args[4]);
			for(double alpha = alpha_max; alpha >= alpha_min; alpha = alpha - alpha_interval){
				ResultInterpreter.saveCorrelationToFile(date, alpha);
			}
		}
	}

}
