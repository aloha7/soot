package ccr.help;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import polyglot.ast.Do;

import soot.jimple.spark.ondemand.genericutil.Averager;

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
			+ date + "/TestPool_"+ alpha + ".txt";
		try {
			BufferedReader br = new BufferedReader(new FileReader(file));
			if(containHeader)
				br.readLine();
			
			String str = null;
			while((str = br.readLine())!= null){
				String[] strs = str.split("\t");
				String tc = strs[0];
				int length = Integer.parseInt(strs[1]);
				int CD = Integer.parseInt(strs[2]);
				
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
		return len_CD_mutationScore;
	}
	
	
	
	public static void printTestCaseGroupByLength(HashMap<Integer, HashMap<Integer, ArrayList>> len_CD_tcList){
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
	}
	
	public static void print(HashMap<Integer, HashMap<Integer, Double>> len_CD_MS){
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
		System.out.println(sb.toString());
		
	}
	
	public static void main(String[] args) {
		String instruction = args[0];
		
		if(instruction.equals("getCorrelation_CD_MS")){
			String date = args[1];		
			boolean containHeader = true;
			int mutantNumber = 1934;
			HashMap<String, Double> tc_mutationscore = 
				ResultInterpreter.loadMutantScore_AllLevel(date, containHeader, mutantNumber);
			
			double alpha = Double.parseDouble(args[2]);
			HashMap<Integer, HashMap<Integer, ArrayList>> len_CD_tcList = 
				ResultInterpreter.groupTestCase_Length(date, containHeader, alpha);
			HashMap<Integer, HashMap<Integer, Double>> len_CD_MS = 
				ResultInterpreter.combine(len_CD_tcList, tc_mutationscore);
			ResultInterpreter.print(len_CD_MS);
			
			
		}
	}

}
