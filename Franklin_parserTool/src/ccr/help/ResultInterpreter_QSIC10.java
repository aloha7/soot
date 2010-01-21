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

public class ResultInterpreter_QSIC10 {

	/**2010-01-13:load the all-level mutant number killed 
	 * by each test case
	 * 
	 * @param date
	 * @param containHeader
	 * @param mutantNumber
	 * @return:testcase(String) -> mutation score (Double)
	 */
	public static HashMap<String, Double> loadMutantScore(String date, boolean containHeader, int mutantNumber){		
		HashMap<String, Double> tc_ms = new HashMap<String, Double>();
		String file = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
			+ date + "/TestCaseDetails/TestCaseDetails_"+mutantNumber+".txt";
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
			+ date + "/TestPool_Alpha/TestPool_"+ new DecimalFormat("0.0000").format(alpha) + ".txt";
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
	public static HashMap<Integer, HashMap<Integer, Double>> getCorr_CD_MS_FixLen(HashMap<Integer, HashMap<Integer, ArrayList>> len_CD_tcList,
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
	 * sharing the same CD (length-equivalent set), but also need to solve the slope of linear regression
	 * between CD and mutant score. When the size of length-equivalent set is lower than
	 * size_min, then this set cannot be considered 
	 * 
	 * @param len_CD_MS
	 * @param len_CD_tcList
	 * @param size_min
	 * @return
	 */
	public static OutputFormat print(HashMap<Integer, HashMap<Integer, Double>> len_CD_MS, 
			HashMap<Integer, HashMap<Integer, ArrayList>> len_CD_tcList, int size_min){
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
				if(size >= size_min){
					sum_CD += CD;
					sum_MS +=  MS;
					sum_CD_Squre += CD *CD;
					sum_CDByMS += CD * MS;
					n ++;
				}
			}					
			double a =  (n *sum_CDByMS - sum_CD * sum_MS )/(n*sum_CD_Squre - sum_CD*sum_CD);
			double b = sum_MS/n - a* sum_CD/n;
			format.slopeSequence.add(a);
			
			if(a < 0){
				format.negativeLength.add(length);
			}
			sb.append(a).append("\t").append(b).append("\n");
		}
//		System.out.println(sb.toString());
		format.content = sb.toString();
		return format;
	}
	
	
	/**
	 * 
	 * @param x
	 * @param y
	 * @return
	 */
	public double getPearsonCorrelationTest(double[] x, double[] y){
		double PearsonCorrelationEfficient = 0.0;
		double sum_x = 0.0;
		double sum_y = 0.0;
		
		for(int i = 0; i < x.length; i ++){
			sum_x += x[i];
			sum_y += y[i];			
		}
		double avg_x = sum_x /x.length;
		double avg_y = sum_y /x.length;
		
		double sum_xy = 0.0;
		double sum_xx = 0.0;
		double sum_yy = 0.0;
		for(int i = 0; i < x.length; i ++){
			sum_xy += (x[i] - avg_x)*(y[i] - avg_y);
			sum_xx += (x[i] - avg_x)*(x[i] - avg_x);
			sum_yy += (y[i] - avg_y)*(y[i] - avg_y);
		}
		PearsonCorrelationEfficient = (double)sum_xx / (double)Math.sqrt(sum_xx*sum_yy);
		
		return PearsonCorrelationEfficient;
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
	
	public static void saveToFile_CD_MS_FixLen(String date, double alpha, 
			int lengthEquivalentSetSize_min){
		StringBuilder sb = new StringBuilder();
		StringBuilder sb_slope = new StringBuilder();
		
		boolean containHeader = true;
		int mutantNumber = 1934;
		HashMap<String, Double> tc_mutationscore = 
			ResultInterpreter_QSIC10.loadMutantScore(date, containHeader, mutantNumber);
		
	
		HashMap<Integer, HashMap<Integer, ArrayList>> len_CD_tcList = 
			ResultInterpreter_QSIC10.groupTestCase_Length(date, containHeader, alpha);
		HashMap<Integer, HashMap<Integer, Double>> len_CD_MS = 
			ResultInterpreter_QSIC10.getCorr_CD_MS_FixLen(len_CD_tcList, tc_mutationscore);
		if(len_CD_tcList.size() > 0  && len_CD_MS.size() >0){
			OutputFormat format= ResultInterpreter_QSIC10.print(len_CD_MS, len_CD_tcList, lengthEquivalentSetSize_min);
			format.alpah = alpha;
			
			if(format.negativeLength.size() == 0){
				sb.append("*********************************************");
				sb.append("Find a feasible alpha:" + alpha);
				sb.append("*********************************************").append("\n");

				//record the slope of each length curve for this alpha
//				sb_slope.append(format.alpah).append("\t");
//				for(int i = 0; i < format.slopeSequence.size(); i ++){
//					sb_slope.append(format.slopeSequence.get(i)).append("\t");
//				}
//				sb_slope.append("\n");
			
			}else{

				sb.append("Alpha:" + 
						new DecimalFormat("0.0000").format(format.alpah)
						+ ";"+ format.negativeLength.size() 
						+" lengths sharing negative slopes:");
				
				for(int k = 0; k < format.negativeLength.size(); k ++){
					sb.append(format.negativeLength.get(k) + "\t");
				}
				sb.append("\n");
				
			}
			
			//2010-01-14:record the slope of each length curve for this alpha
			sb_slope.append(format.alpah).append("\t");
			for(int i = 0; i < format.slopeSequence.size(); i ++){
				sb_slope.append(format.slopeSequence.get(i)).append("\t");
			}
			sb_slope.append("\n");
			
			String file =  "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
				+ date + "/Result/CD_MS_FixedLen/FixLen_CI_MS_"+ new DecimalFormat("0.0000").format(alpha) 
				+"_"+ lengthEquivalentSetSize_min +".txt";
			
			Logger.getInstance().setPath(file, false);
			Logger.getInstance().write(format.content);
			Logger.getInstance().close();	
			
			String summaryFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
				+ date + "/Result/CD_MS_FixedLen/CI_MS_CorrelationSummary_" + 
				lengthEquivalentSetSize_min + ".txt"; 
						
			Logger.getInstance().setPath(summaryFile, true);
			Logger.getInstance().write(sb.toString());
			Logger.getInstance().close();
			
			String slopeFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
				+ date + "/Result/CD_MS_FixedLen/CI_MS_CorrelationSlope_"+lengthEquivalentSetSize_min+".txt";
			
			Logger.getInstance().setPath(slopeFile, true);
			Logger.getInstance().write(sb_slope.toString());
			Logger.getInstance().close();	
			
		}else{
			System.out.println("Missing alpha:" + new DecimalFormat("0.0000").format(alpha));
		}
	}
	
	/**2010-01-13: group test cases by their CD
	 * 
	 * @param date_testpool
	 * @param containHeader_testpool
	 * @param alpha
	 * @return: CD(Integer) -> test cases(String)
	 */
	public static HashMap<Integer, ArrayList> getTestPool(String date_testpool, boolean containHeader_testpool, double alpha){
		String file =  "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
			+ date_testpool + "/TestPool_Alpha/TestPool_"+ new DecimalFormat("0.0000").format(alpha) + ".txt";
		HashMap<Integer, ArrayList> CD_tcList = new HashMap<Integer, ArrayList>();
		
		File tmp = new File(file);
		if(tmp.exists()){
			try {
				BufferedReader br = new BufferedReader(new FileReader(file));
				if(containHeader_testpool)
					br.readLine();
				
				String str = null;
				while((str = br.readLine())!= null){
					String[] strs = str.split("\t");
					if(strs.length >= 3){
						String tc = strs[0];						
						int CD = (int)Double.parseDouble(strs[2]);
						if(CD_tcList.containsKey(CD)){
							ArrayList tcList = CD_tcList.get(CD);
							tcList.add(tc);
							CD_tcList.put(CD, tcList);
						}else{
							ArrayList tcList = new ArrayList();
							tcList.add(tc);
							CD_tcList.put(CD, tcList);
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
		return CD_tcList;
	}
	
	public static void getCorr_CD_MS(String date_MutationScore, boolean containHeader_MutationScore, int mutantNumber
			, String date_testpool, boolean containHeader_testpool, double alpha){
		HashMap<String, Double> tc_MS = loadMutantScore(date_MutationScore, containHeader_MutationScore, mutantNumber);
		HashMap<Integer, ArrayList> CD_tcList = getTestPool(date_testpool, containHeader_testpool, alpha);		
		StringBuilder sb = new StringBuilder();
		sb.append("Size").append("\t").append("CD").append("\t").append("MutationScore").append("\n");
		
		Integer[] CD_ordered = (Integer[])CD_tcList.keySet().toArray(new Integer[0]);
		Arrays.sort(CD_ordered);
		DecimalFormat formatter = new DecimalFormat("0.000000000");
		for(int i = 0; i < CD_ordered.length; i ++){
			int CD = CD_ordered[i];
			ArrayList tcList = CD_tcList.get(CD);
			
			double sum_MS = 0.0;
			for(int j = 0; j < tcList.size(); j ++){
				String tc = (String)tcList.get(j);
				double MS = tc_MS.get(tc);
				sum_MS += MS;
			}
			double avg_MS = sum_MS/tcList.size();			
			
			sb.append(tcList.size()).append("\t").append(CD).append("\t").
				append(formatter.format(avg_MS)).append("\n");
		}
		
		String file =  "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
			+ date_testpool + "/Result/CD_MS/CD_MS_"+ mutantNumber + "_"+ new DecimalFormat("0.00000").format(alpha) + ".txt";
		Logger.getInstance().setPath(file, false);
		Logger.getInstance().write(sb.toString());
		Logger.getInstance().close();
	}
	
	public static double getAvgMutationScore(ArrayList<Double> msList){
		double sum_ms = 0.0;
		for(int i = 0; i < msList.size(); i ++){
			sum_ms += msList.get(i);
		}
		return sum_ms/(double)msList.size();
	}
	
	/**
	 * 
	 * @param date
	 * @param containHeader: indicate whether there is a header in the mutant killing file
	 * @param mutantNum_all
	 * @param mutantNum_1
	 * @param mutantNum_2
	 * @param mutantNum_3
	 */
	public static void orderScoreEquivalentSet(String date, boolean containHeader, 
			int mutantNum_all, int mutantNum_1, int mutantNum_2, int mutantNum_3){
		HashMap<String, Double> tc_ms_all = loadMutantScore(date, containHeader, mutantNum_all);
		HashMap<String, Double> tc_ms_1 = loadMutantScore(date, containHeader, mutantNum_1);
		HashMap<String, Double> tc_ms_2 = loadMutantScore(date, containHeader, mutantNum_2);
		HashMap<String, Double> tc_ms_3 = loadMutantScore(date, containHeader, mutantNum_3);
		
		//1. sort tc_ms_all by mutation score
		HashMap<Double, ArrayList> CD_tcList = new HashMap<Double, ArrayList>();
		String[] tcArray = tc_ms_all.keySet().toArray(new String[0]);
		Arrays.sort(tcArray);
		for(int i = 0; i < tcArray.length; i ++){
			String tc = tcArray[i];
			double CD = tc_ms_all.get(tc);
			
			if(CD_tcList.containsKey(CD)){
				ArrayList tcList = CD_tcList.get(CD);
				tcList.add(tc);
				CD_tcList.put(CD, tcList);
			}else{
				ArrayList tcList = new ArrayList();
				tcList.add(tc);
				CD_tcList.put(CD, tcList);
			}			
		}
		
		//2. Given ordered mutant score, count the score-equivalent 
		//set size/mutation score on other levels/fault types
		StringBuilder sb = new StringBuilder();
		sb.append("Size").append("\t").append(mutantNum_all).append("\t").
		append(mutantNum_1).append("\t").append(mutantNum_2).append("\t").append(mutantNum_3).append("\n");
		
		Double[] CDList = (Double[])CD_tcList.keySet().toArray(new Double[0]);
		Arrays.sort(CDList);
		
		DecimalFormat formatter = new DecimalFormat("0.000000000");
		for(int i = 0; i < CDList.length; i ++){
			double CD = CDList[i];
			
			//3.get the score-equivalent set
			ArrayList tcList_all = CD_tcList.get(CD);
			ArrayList mutationScore_all = new ArrayList();
			ArrayList mutationScore_1 = new ArrayList();
			ArrayList mutationScore_2 = new ArrayList();
			ArrayList mutationScore_3 = new ArrayList();
			
			//3.1. get the averaged mutation score in all-level
			for(int j = 0; j < tcList_all.size(); j ++){
				String tc = (String)tcList_all.get(j);
				double ms_all = tc_ms_all.get(tc);
				mutationScore_all.add(ms_all);
				
				double ms_1 = tc_ms_1.get(tc);
				mutationScore_1.add(ms_1);
				
				double ms_2 = tc_ms_2.get(tc);
				mutationScore_2.add(ms_2);
				
				double ms_3 = tc_ms_3.get(tc);
				mutationScore_3.add(ms_3);
			}
			
			int size = CD_tcList.get(CD).size();
			double avg_ms_all = getAvgMutationScore(mutationScore_all);
			double avg_ms_1 = getAvgMutationScore(mutationScore_1);
			double avg_ms_2 = getAvgMutationScore(mutationScore_2);
			double avg_ms_3 = getAvgMutationScore(mutationScore_3);
			sb.append(size).append("\t").append(formatter.format(avg_ms_all)).append("\t").
			append(formatter.format(avg_ms_1)).append("\t").append(avg_ms_2).append("\t").
			append(formatter.format(avg_ms_3)).append("\n");
		}
		
		String filename = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
			+ date + "/Result/ScoreEquivalentSet/ScoreEquivalentSet_"+ mutantNum_all+"_"+mutantNum_1+"_"+ mutantNum_2 + "_"+ mutantNum_3 + ".txt";;
			
		Logger.getInstance().setPath(filename, false);
		Logger.getInstance().write(sb.toString());
		Logger.getInstance().close();
		
	}
	
	public static void main(String[] args) {
		String instruction = args[0];
		
		if(instruction.equals("getCorre_CD_MS_fixedLen_single")){
			String date = args[1];
			double alpha = Double.parseDouble(args[2]);
			int lengthEquivalentSetSize_min = Integer.parseInt(args[3]);
			
			//delete the summary file before continuing
			String summaryFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
				+ date + "/Result/CD_MS_FixedLen/CI_MS_CorrelationSummary.txt";			
			File tmp = new File(summaryFile);			
			if(tmp.exists()){
				tmp.delete();
			}
			
			ResultInterpreter_QSIC10.saveToFile_CD_MS_FixLen(date, alpha, lengthEquivalentSetSize_min);
			
		}else if(instruction.equals("getCorre_CD_MS_fixedLen_multiple")){
			String date = args[1];
			double alpha_min = Double.parseDouble(args[2]);//inclusive
			double alpha_max = Double.parseDouble(args[3]);//inclusive
			double alpha_interval = Double.parseDouble(args[4]);
			int lengthEquivalentSetSize_min = Integer.parseInt(args[5]);
			
			//delete the summary file before continuing
			String summaryFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
				+ date + "/Result/CD_MS_FixedLen/CI_MS_CorrelationSummary.txt";			
			File tmp = new File(summaryFile);			
			if(tmp.exists()){
				tmp.delete();
			}
			
			String slopeFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
				+ date + "/Result/CD_MS_FixedLen/CI_MS_CorrelationSlope.txt";
			tmp = new File(slopeFile);			
			if(tmp.exists()){
				tmp.delete();
			}
			
			for(double alpha = alpha_max; alpha >= alpha_min; alpha = alpha - alpha_interval){
				ResultInterpreter_QSIC10.saveToFile_CD_MS_FixLen(date, alpha, lengthEquivalentSetSize_min);
			}
			
		}else if(instruction.equals("getCorre_CD_MS")){
			String date = args[1];
			int mutantNumber = Integer.parseInt(args[2]);
			double alpha = Double.parseDouble(args[3]);
			String date_MutationScore = date;
			boolean containHeader_MutationScore = true;
			String date_testpool = date;
			boolean containHeader_testpool = true;
			
			getCorr_CD_MS(date_MutationScore, containHeader_MutationScore, 
					mutantNumber, date_testpool, containHeader_testpool, alpha);
		}else if(instruction.equals("orderScoreEquivalentSet")){
			String date = args[1];
			boolean containHeader = true;
			int mutantNum_all = Integer.parseInt(args[2]);
			int mutantNum_1 = Integer.parseInt(args[3]);
			int mutantNum_2 = Integer.parseInt(args[4]);
			int mutantNum_3 = Integer.parseInt(args[5]);
			orderScoreEquivalentSet(date, containHeader, mutantNum_all, mutantNum_1, mutantNum_2, mutantNum_3);
		}
	}

}
