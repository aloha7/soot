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

import ccr.help.CurveFittingResult;
import ccr.help.DataAnalyzeManager;
import ccr.help.DataDescriptionResult;
import ccr.help.ILPOutput;
import ccr.help.MutantStatistics;
import ccr.help.TestSetStatistics;
import ccr.reduction.ILPSolver;
import ccr.test.Logger;
import ccr.test.TestCase;
import ccr.test.TestDriver;
import ccr.test.TestSet;

public class Reporter_Reduction {
	//2010-01-27: here is the cache
	public static HashMap<String, ArrayList<String>> mutant_validTestCases_cache = new HashMap<String, ArrayList<String>>();
	
	
	/**2010-01-27: decouple this from getValidTestCases_testSet_mutant()
	 * to derive the failure rates of mutants
	 * 
	 * @param mutantFile_date
	 * @param containHeader_mutant
	 * @param mutantDetailDir
	 * @param containHeader_testing
	 * @return
	 */
	public static HashMap<String, ArrayList<String>> getMutant_ValidTestCases_offline(
			String mutantFile_date, boolean containHeader_mutant, 
			String mutantDetailDir, boolean containHeader_testing){
		HashMap<String, ArrayList<String>> validTestCases_mutant = new HashMap<String, ArrayList<String>>();
		
		ArrayList<String> mutantArray = MutantStatistics.loadMutants_offline(mutantFile_date, containHeader_mutant);
		String mutantDetailFile = null; 
		int tc_min = -10000;
		int tc_max = 10000;
		
		
		//1.get all valid test cases to kill a given mutant
		for(int i = 0; i < mutantArray.size(); i ++){
			String mutantID = mutantArray.get(i);
			
			System.out.println("[Reporter_Reduction.getMutant_ValidTestCases_offline]Processing faulty version:" + mutantID);
			ArrayList<String> validTestCases = null;
			
			//2010-01-27: use the cache mechanism to save execution time
			if(!mutant_validTestCases_cache.containsKey(mutantID)){
				
				mutantDetailFile = mutantDetailDir + "/detailed_" + mutantID 
				+ "_" + (Integer.parseInt(mutantID) + 1) + ".txt";
				
				validTestCases = MutantStatistics.loadValidTestCases(mutantDetailFile, 
						containHeader_testing, tc_min, tc_max);
				//save it to cache
				mutant_validTestCases_cache.put(mutantID, validTestCases);
				
			}else{
				validTestCases = mutant_validTestCases_cache.get(mutantID);
			}
			
			validTestCases_mutant.put(mutantID, validTestCases);
		}
		
		return validTestCases_mutant;
	}
	
	/**2010-01-27: decouple this method from from getValidTestCases_testSet_mutant()
	 * to reused by test suite reduction experiments. For a given mutant and a set of test sets,
	 * return all valid test cases within a test set to kill it
	 * 
	 * @param testSetArray
	 * @param mutantFile_date
	 * @param containHeader_mutant
	 * @param mutantDetailDir
	 * @param containHeader_testing
	 * @return
	 */
	public static HashMap<String, HashMap<String, ArrayList<String>>> getMutant_testSet_ValidTestCases_online(
			ArrayList<TestSet> testSetArray, 
			String mutantFile_date, boolean containHeader_mutant, 
			String mutantDetailDir, boolean containHeader_testing){
		
		HashMap<String, HashMap<String, ArrayList<String>>> mutant_testSet_validTestCases = new 
		HashMap<String, HashMap<String,ArrayList<String>>>();

		HashMap<String, ArrayList<String>> mutant_validTestCases = getMutant_ValidTestCases_offline(mutantFile_date, 
		containHeader_mutant, mutantDetailDir, containHeader_testing);
		
		//2010-03-18: for debugging purpose
//		StringBuilder sb = new StringBuilder();
//		Iterator<String> mutants = mutant_validTestCases.keySet().iterator();
//		while(mutants.hasNext()){
//			String mutant = mutants.next();
//			ArrayList<String> validTestCases = mutant_validTestCases.get(mutant);
//			if(validTestCases.size() < 10){
//				sb.append(mutant).append("\t").append(validTestCases.size()).append("\t");
//				for(int i = 0; i < validTestCases.size(); i ++){
//					sb.append(validTestCases.get(i)).append("\t");
//				}
//				sb.append("\n");
//			}
//		}
//		String filename = "c:\\a.txt";
//		Logger.getInstance().setPath(filename, false);
//		Logger.getInstance().write(sb.toString());
//		Logger.getInstance().close();
		
		Iterator<String> ite_mutant = mutant_validTestCases.keySet().iterator();
		while(ite_mutant.hasNext()){
			String mutantID = ite_mutant.next();
			
			//2010-01-27: use the cached mechanism to save execution time;
			HashMap<String, ArrayList<String>> testSet_validTC = new HashMap<String, ArrayList<String>>();
			
			//2010-01-28: cannot use cache here since TestSet can change frequently
				//1. get the valid test cases in each test set to kill a given mutant
				ArrayList<String> validTestCases = mutant_validTestCases.get(mutantID);
				for(int j = 0; j < testSetArray.size(); j++){
					TestSet ts = testSetArray.get(j);
					ArrayList<String> testCases = ts.testcases;
					ArrayList<String> validTCArray = TestDriver.getSharedTestCases(validTestCases,
							testCases);
					testSet_validTC.put(ts.index, validTCArray);
				}				
			
			mutant_testSet_validTestCases.put(mutantID, testSet_validTC);
		}
		
		return mutant_testSet_validTestCases;
	}
	
	/**2010-01-26: given a mutant and adequate test sets, it returns
	 * mutant -> test sets -> valid test cases (offline way is useful for 
	 * unit testing)
	 * 
	 * @param testSetFile
	 * @param containHeader_testSet
	 * @param mutantFile_date
	 * @param containHeader_mutant
	 * @param mutantDetailDir
	 * @param containHeader_testing
	 * @return
	 */
	public static HashMap<String, HashMap<String, ArrayList<String>>> getMutant_testSet_ValidTestCases_offline(
			String testSetFile, boolean containHeader_testSet,
			String mutantFile_date, boolean containHeader_mutant, 
			String mutantDetailDir, boolean containHeader_testing){
			
		ArrayList<TestSet> testSetArray = TestSetStatistics.loadTestSet_offline(
				testSetFile, containHeader_testSet);
		
		return getMutant_testSet_ValidTestCases_online(testSetArray, mutantFile_date, 
				containHeader_mutant, mutantDetailDir, containHeader_testing);
		
	}
	
	
	/**2010-01-27: given some test sets and some mutants, return the fault detection rate
	 * of each test set with respect to each mutant (online way is useful for test suite reduction)
	 * 
	 * @param testSets
	 * @param mutantFile_date
	 * @param containHeader_mutant
	 * @param mutantDetailDir
	 * @param containHeader_testing
	 * @return
	 */
	public static HashMap<String, Double> getFaultDetectionRate_detailed_online(ArrayList<TestSet> testSets,
			String mutantFile_date, boolean containHeader_mutant, 
			String mutantDetailDir, boolean containHeader_testing){
		
		HashMap<String, Double> mutant_FDR = new HashMap<String, Double>();
		HashMap<String, HashMap<String, ArrayList<String>>> mutant_testSet_validTestCases = 
			getMutant_testSet_ValidTestCases_online(testSets,
					mutantFile_date, containHeader_mutant, mutantDetailDir,
					containHeader_testing);
		
		//1.calculate the FDR with respect to each mutant
		Iterator<String> ite_mutant = mutant_testSet_validTestCases.keySet().iterator();
		while(ite_mutant.hasNext()){
			String mutant = ite_mutant.next();
			HashMap<String, ArrayList<String>> testSet_validTestCases = 
				mutant_testSet_validTestCases.get(mutant);
			
			//2010-03-18: when the number of test sets is larger than 1, then FDR = percentage of valid test sets 
			//which contain at least one test case to kill a mutant
//			if(testSet_validTestCases.size() > 1){
				Iterator<String> ite_testSet = testSet_validTestCases.keySet().iterator();
				//2. if there is one valid test case with a test set, then this set is valid to kill this mutant
				int validTestSet_counter = 0;
				while(ite_testSet.hasNext()){
					String testSet = ite_testSet.next();
					ArrayList<String> validTestCases = testSet_validTestCases.get(testSet);
					if(validTestCases.size() > 0){
						validTestSet_counter ++;
					}
				}
				
				//3. FDR = the percentage of valid test sets
				double FDR = (double)validTestSet_counter/(double)testSet_validTestCases.size();
				mutant_FDR.put(mutant, FDR);	
//			}else if(testSet_validTestCases.size() == 1){
//				//2010-03-18: when the number of test sets is 1, then FDR = percentage of valid test cases 
//				//which can kill a mutant
//				Iterator<String> ite_testSet = testSet_validTestCases.keySet().iterator();
//				String testSet = ite_testSet.next();
//				ArrayList<String> validTestCases = testSet_validTestCases.get(testSet);
//				double FDR = (double)validTestCases.size()/(double)testSets.get(0).size();
//					
//				mutant_FDR.put(mutant, FDR);
//			}
		}		
		return mutant_FDR;
	}
	

	/**2010-01-27: given some test sets and some mutants, return the fault detection rate
	 * of each test set with respect to each mutant (load adequate test sets from the file, 
	 * which is useful for unit testing)
	 * 
	 * @param testSetFile
	 * @param containHeader_testSet
	 * @param mutantFile_date
	 * @param containHeader_mutant
	 * @param mutantDetailDir
	 * @param containHeader_testing
	 * @return
	 */
	public static HashMap<String, Double> getFaultDetectionRate_detailed_offline(String testSetFile, boolean containHeader_testSet,
			String mutantFile_date, boolean containHeader_mutant, 
			String mutantDetailDir, boolean containHeader_testing){
		
		ArrayList<TestSet> testSets = TestSetStatistics.loadTestSet_offline(testSetFile, containHeader_testSet);
		return getFaultDetectionRate_detailed_online(testSets, mutantFile_date, containHeader_mutant, 
				mutantDetailDir, containHeader_testing);
	}
	
	/**2010-01-27: given some criterion-adequate test sets and some mutants, 
	 * return the averaged fault detection rate of the criterion with respect 
	 * to these mutants (load test sets from the file, which is useful for unit testing) 
	 * 
	 * @param testSetFile
	 * @param containHeader_testSet
	 * @param mutantFile_date
	 * @param containHeader_mutant
	 * @param mutantDetailDir
	 * @param containHeader_testing
	 * @return
	 */
	public static double getFaultDetectionRate_average_offline(String testSetFile, boolean containHeader_testSet,
			String mutantFile_date, boolean containHeader_mutant, 
			String mutantDetailDir, boolean containHeader_testing){
		
		ArrayList<TestSet> testSets = TestSetStatistics.loadTestSet_offline(testSetFile, containHeader_testSet);
		return getFaultDetectionRate_average_online(testSets, mutantFile_date,
				containHeader_mutant, mutantDetailDir, containHeader_testing);
	}
	
	
	
	/**2010-01-27: given some criterion-adequate test sets and some mutants, 
	 * return the averaged fault detection rate of the criterion with respect 
	 * to these mutants (online way is useful for test suite reduction)
	 * 
	 * @param testSets
	 * @param mutantFile_date
	 * @param containHeader_mutant
	 * @param mutantDetailDir
	 * @param containHeader_testing
	 * @return
	 */
	public static double getFaultDetectionRate_average_online(ArrayList<TestSet> testSets, 
			String mutantFile_date, boolean containHeader_mutant, 
			String mutantDetailDir, boolean containHeader_testing){
		
		double FDR_average = 0.0;
		HashMap<String, Double> mutant_FDR = getFaultDetectionRate_detailed_online(testSets,
				mutantFile_date, containHeader_mutant,
				mutantDetailDir, containHeader_testing);
		
		Iterator<String> ite_mutant = mutant_FDR.keySet().iterator();
		
		double FDR_sum = 0.0;
		while(ite_mutant.hasNext()){
			String mutant = ite_mutant.next();			
			FDR_sum += mutant_FDR.get(mutant);
		}
		FDR_average = FDR_sum/mutant_FDR.size();
		
		return FDR_average; 
	}
	
	public static double getReductionSize_average_online(ArrayList<ILPOutput> outputs){

		double size_sum = 0.0;
		for(int i = 0; i < outputs.size(); i ++){
			ILPOutput output = outputs.get(i);
			size_sum += output.reducedTestSet.size();
		}
		double size_average = size_sum/outputs.size();
		
		return size_average;
	}
	
	public static double getTimeCost_average_online(ArrayList<ILPOutput> outputs){

		double time_sum = 0.0;
		for(int i = 0; i < outputs.size(); i ++){
			ILPOutput output = outputs.get(i);
			time_sum += output.time;
		}
		double time_average = time_sum/outputs.size();
		
		return time_average;
	}
	
	/**2010-02-03: get the reduced test suite size generated by the ILP model with respect to various 
	 * testing criteria and weighting factors 
	 * 
	 * @param date
	 * @param criterion
	 * @param alpha_min
	 * @param alpha_max
	 * @param alpha_interval
	 * @param maxSize
	 * @param single_enabled
	 * @return
	 */
	public static HashMap<Double, Double> getReductionSize_averaged_BILP_offline(String date, 
			String criterion, double alpha_min, double alpha_max, 
			double alpha_interval, int maxSize, boolean single_enabled){

		HashMap<Double, Double> alpha_size = new HashMap<Double, Double>();
		
		HashMap<Double, ArrayList<ILPOutput>> alpha_outputs  = new HashMap<Double, ArrayList<ILPOutput>>();
		
		ArrayList<ILPOutput> outputs = new ArrayList<ILPOutput>();
		
		String testSetDir = "";
		boolean containHeader = false;
		String pattern = "";
		if(single_enabled){

			testSetDir =  "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
				+ date + "/ILPModel/"+ criterion + "/";
			containHeader = false;

//			pattern = "Model\\_" + criterion +"\\_SingleObj\\_[0-9]+\\_output\\.txt";
			//2010-02-01:only interest in the first reduced test set
			pattern = "Model\\_" + criterion +"\\_SingleObj\\_0\\_output\\.txt";
			outputs = TestSetStatistics.loadILPOutput_offline(testSetDir, containHeader, pattern);			
		}
		
		
		//2. reduced test sets of bi-criteria ILP
		//2010-02-01: for two double we  use "|alpha_max - alpha| > 0.0001" rather than "alpha_max - alpha"
		// to determine whether alpha_max is larger than alpha or not
		for(double alpha = alpha_min; Math.abs(alpha_max - alpha) > 0.0001; alpha = alpha + alpha_interval){
			DecimalFormat format = new DecimalFormat("0.0");
			String alpha_str = format.format(alpha);
			
			testSetDir =  "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
				+ date + "/ILPModel/"+ criterion + "/";
			containHeader = false;
			
//			pattern = "Model\\_" + criterion +"\\_"+ alpha_str+"\\_[0-9]+" +
//			"\\_[0-9]+\\_output\\.txt";
			
			//2010-02-01:only interest in the first reduced test set
//			pattern = "Model\\_" + criterion +"\\_"+ alpha_str+"\\_[0-9]+" +
//			"\\_0\\_output\\.txt";
			
			//2010-02-03: test set size- sensitive 
//			pattern = "Model\\_" + criterion +"\\_"+ alpha_str+"\\_[0-9]+" +
//			"\\_0\\_output\\.txt";
			
			//2010-02-01:only interest in the first reduced test set
			pattern = "Model\\_" + criterion +"\\_"+ alpha_str+"\\_" + maxSize +
			"\\_0\\_output\\.txt";
			
			ArrayList<ILPOutput> output_biCriteria = TestSetStatistics.loadILPOutput_offline(testSetDir, containHeader, pattern);
			
			alpha_outputs.put(alpha, output_biCriteria);			
		}
		
		
		//3. combine all test sets
		if(outputs != null){ //single-objective ILP is enabled
			alpha_outputs.put(Double.MIN_VALUE, outputs);			
		}
		
		Double[] alphas = alpha_outputs.keySet().toArray(new Double[0]);
		Arrays.sort(alphas);		
		for(int i = 0; i < alphas.length; i ++){
			double alpha = alphas[i];
			ArrayList<ILPOutput> ILPoutputs = alpha_outputs.get(alpha);
			
			double size = getReductionSize_average_online(ILPoutputs);
			alpha_size.put(alpha, size);
		}
		
		return alpha_size;
	}
	
	/**2010-03-19: save the time cost of each test suite constructed by
	 * ILP model and save the corresponding data description(e.g., min/mean/median/max/std)
	 * 
	 * @param date
	 * @param criterion
	 * @param alpha_min
	 * @param alpha_max
	 * @param alpha_interval
	 * @param sizeConstraint_min
	 * @param sizeConstraint_max
	 * @param single_enabled
	 * @return: alpha->times
	 */
	public static HashMap<Double, ArrayList<Double>> getTimeCost_detailed_BILP_offline(String date, 
			String criterion, double alpha_min, double alpha_max, 
			double alpha_interval, int sizeConstraint_min, int sizeConstraint_max, 
			String H_L_D, boolean single_enabled){
		
		HashMap<Double, ArrayList<Double>> alpha_times = new HashMap<Double, ArrayList<Double>>();
		
		HashMap<Double, ArrayList<ILPOutput>> alpha_outputs  = new HashMap<Double, ArrayList<ILPOutput>>();
		
		ArrayList<ILPOutput> outputs = new ArrayList<ILPOutput>();
		
		String testSetDir = "";
		boolean containHeader = false;
		String pattern = "";
		if(single_enabled){

			testSetDir =  "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
				+ date + "/ILPModel/"+ criterion + "/";
			containHeader = false;

//			pattern = "Model\\_" + criterion +"\\_SingleObj\\_[0-9]+\\_output\\.txt";
			//2010-02-01:only interest in the first reduced test set
			pattern = "Model\\_" + criterion +"\\_SingleObj\\_0\\_output\\.txt";
			outputs = TestSetStatistics.loadILPOutput_offline(testSetDir, containHeader, pattern);			
		}
		
		
		//2. reduced test sets of bi-criteria ILP
		//2010-02-01: for two double we  use "|alpha_max - alpha| > 0.0001" rather than "alpha_max - alpha"
		// to determine whether alpha_max is larger than alpha or not
		for(double alpha = alpha_min; Math.abs(alpha_max - alpha) > 0.0001; alpha = alpha + alpha_interval){
			
			DecimalFormat format = new DecimalFormat("0.0");
			String alpha_str = format.format(alpha);
			
			testSetDir =  "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
				+ date + "/ILPModel/"+ criterion + "/";
			containHeader = false;
			
//			pattern = "Model\\_" + criterion +"\\_"+ alpha_str+"\\_[0-9]+" +
//			"\\_[0-9]+\\_output\\.txt";
			for(int i = sizeConstraint_min; i < sizeConstraint_max; i ++){
				int sizeConstraint = i;
				
				//2010-03-18: interest in multiple reduced test sets with a 
				pattern = "Model\\_" + criterion +"\\_"+ alpha_str+"\\_" + sizeConstraint +
				"\\_[0-9]+\\_RA_"+H_L_D+"\\_output\\.txt";

				//2010-02-01:only interest in the first reduced test set
//				pattern = "Model\\_" + criterion +"\\_"+ alpha_str+"\\_" + sizeConstraint +
//				"\\_0\\_output\\.txt";
			
				ArrayList<ILPOutput> output_biCriteria = TestSetStatistics.loadILPOutput_offline(testSetDir, containHeader, pattern);
			
				alpha_outputs.put(alpha, output_biCriteria);
			}
		}
		
		
		//3. combine all test sets
		if(outputs.size() != 0){ //single-objective ILP is enabled
			alpha_outputs.put(Double.MIN_VALUE, outputs);			
		}
		
		Double[] alphas = alpha_outputs.keySet().toArray(new Double[0]);
		Arrays.sort(alphas);	
		
		
		for(int i = 0; i < alphas.length; i ++){
			double alpha = alphas[i];
			ArrayList<ILPOutput> ILPoutputs = alpha_outputs.get(alpha);
			
			ArrayList<Double> times = new ArrayList<Double>();
			for(int j = 0; j < ILPoutputs.size(); j ++){
				times.add(ILPoutputs.get(j).time);
			}
			alpha_times.put(alpha, times);
		}
		
		return alpha_times;
	}
	
	/**2010-02-01: get the time cost to solve ILP model with respect to various 
	 * testing criteria and weighting factors
	 * 
	 * @param date
	 * @param criterion
	 * @param alpha_min
	 * @param alpha_max
	 * @param alpha_interval
	 * @param maxSize
	 * @param single_enabled
	 * @return
	 */
	public static HashMap<Double, Double> getTimeCost_averaged_BILP_offline(String date, 
			String criterion, double alpha_min, double alpha_max, 
			double alpha_interval, int maxSize, boolean single_enabled){

		HashMap<Double, Double> alpha_time = new HashMap<Double, Double>();
		
		HashMap<Double, ArrayList<ILPOutput>> alpha_outputs  = new HashMap<Double, ArrayList<ILPOutput>>();
		
		ArrayList<ILPOutput> outputs = new ArrayList<ILPOutput>();
		
		String testSetDir = "";
		boolean containHeader = false;
		String pattern = "";
		if(single_enabled){

			testSetDir =  "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
				+ date + "/ILPModel/"+ criterion + "/";
			containHeader = false;

//			pattern = "Model\\_" + criterion +"\\_SingleObj\\_[0-9]+\\_output\\.txt";
			//2010-02-01:only interest in the first reduced test set
			pattern = "Model\\_" + criterion +"\\_SingleObj\\_0\\_output\\.txt";
			outputs = TestSetStatistics.loadILPOutput_offline(testSetDir, containHeader, pattern);			
		}
		
		
		//2. reduced test sets of bi-criteria ILP
		//2010-02-01: for two double we  use "|alpha_max - alpha| > 0.0001" rather than "alpha_max - alpha"
		// to determine whether alpha_max is larger than alpha or not
		for(double alpha = alpha_min; Math.abs(alpha_max - alpha) > 0.0001; alpha = alpha + alpha_interval){
			DecimalFormat format = new DecimalFormat("0.0");
			String alpha_str = format.format(alpha);
			
			testSetDir =  "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
				+ date + "/ILPModel/"+ criterion + "/";
			containHeader = false;
			
//			pattern = "Model\\_" + criterion +"\\_"+ alpha_str+"\\_[0-9]+" +
//			"\\_[0-9]+\\_output\\.txt";
			
			//2010-02-01:only interest in the first reduced test set
			pattern = "Model\\_" + criterion +"\\_"+ alpha_str+"\\_" + maxSize +
			"\\_0\\_output\\.txt";
			
			ArrayList<ILPOutput> output_biCriteria = TestSetStatistics.loadILPOutput_offline(testSetDir, containHeader, pattern);
			
			alpha_outputs.put(alpha, output_biCriteria);			
		}
		
		
		//3. combine all test sets
		if(outputs != null){ //single-objective ILP is enabled
			alpha_outputs.put(Double.MIN_VALUE, outputs);			
		}
		
		Double[] alphas = alpha_outputs.keySet().toArray(new Double[0]);
		Arrays.sort(alphas);		
		for(int i = 0; i < alphas.length; i ++){
			double alpha = alphas[i];
			ArrayList<ILPOutput> ILPoutputs = alpha_outputs.get(alpha);
			
			//2010-02-01: fix another important bug here
			double time = getTimeCost_average_online(ILPoutputs); 
			
			alpha_time.put(alpha, time);
		}
		
		return alpha_time;
	}

	public static HashMap<Double, HashMap<Integer, ArrayList<ILPOutput>>> loadCostAndPerformance(String date, 
			String criterion, boolean containHeader){

		HashMap<Double, HashMap<Integer, ArrayList<ILPOutput>>> alpha_size_outputs = new 
		HashMap<Double, HashMap<Integer,ArrayList<ILPOutput>>>();
				
		
		String saveFile = System.getProperty("user.dir") + "/src/ccr"
		+ "/experiment/Context-Intensity_backup/TestHarness/" + date
		+ "/ILPModel/"+ criterion+"/VaryBeta_fixedAlpha_First.txt";
		File tmp = new File(saveFile);
		if(tmp.exists()){
			try {
				BufferedReader br = new BufferedReader(new FileReader(saveFile));
				if(containHeader){
					br.readLine();
				}
				
				String str = null;
				while((str = br.readLine())!= null){
					String[] strs = str.split("\t");
					double alpha = Double.parseDouble(strs[0]);
					int size = Integer.parseInt(strs[1]);
					ILPOutput output = new ILPOutput();
					output.time = Double.parseDouble(strs[2]);
					output.fdr = Double.parseDouble(strs[3]);
					
					if(output.fdr > 0.0001){//we are not interested in ILPOutput with fdr = 0
						HashMap<Integer, ArrayList<ILPOutput>> size_outputs = alpha_size_outputs.get(alpha);
						if(size_outputs == null){
							size_outputs = new HashMap<Integer, ArrayList<ILPOutput>>();
						}
						ArrayList<ILPOutput> outputs = size_outputs.get(size);
						if(outputs == null){
							outputs = new ArrayList<ILPOutput>();
						}
						outputs.add(output);
						size_outputs.put(size, outputs);
						alpha_size_outputs.put(alpha, size_outputs);	
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
			System.out.println("[Reporter_Reduction.loadCostAndPerformance]The file:" + saveFile 
					+ " does not exist!");
		}
		
		return alpha_size_outputs;
	}
	
	public static void saveCostAndPerformance_AllInOne_offline(String date, 
			double alpha_min, double alpha_max, 
			double alpha_interval){
		
		HashMap<String, HashMap<Double, HashMap<Integer, ArrayList<ILPOutput>>>> criterion_alpha_size_outputs
		 = new HashMap<String, HashMap<Double,HashMap<Integer,ArrayList<ILPOutput>>>>();
		
		//1.load all the data for each testing criterion
		String[] criteria = new String[]{"AllPolicies", "All1ResolvedDU", "All2ResolvedDU"};
		
		for(int i = 0; i < criteria.length; i ++){
			String criterion = criteria[i];
			boolean containHeader = true;
			HashMap<Double, HashMap<Integer, ArrayList<ILPOutput>>> alpha_size_outputs =
				loadCostAndPerformance(date, criterion, containHeader);
			criterion_alpha_size_outputs.put(criterion, alpha_size_outputs);
		}
		
		DecimalFormat format = new DecimalFormat("0.0");	
		
		StringBuilder sb_beta_fdr = new StringBuilder();
		StringBuilder sb_beta_time = new StringBuilder();
		
		StringBuilder sb_curveFitting_beta_fdr = new StringBuilder();
		sb_curveFitting_beta_fdr.append("Criterion\tAlpha\tCoefficient\tIntercept\tError\n");
		
		StringBuilder sb_PearsonCoefficient_beta_fdr = new StringBuilder();
		sb_PearsonCoefficient_beta_fdr.append("Criterion\tAlpha\tPearsonCorrelationCoefficient\n");
		
		//2. save the data to generate figures with Excel (fixing the alpha and vary the beta) 
		for(double alpha = alpha_min; Math.abs(alpha_max - alpha) > 0.0001; alpha = alpha + alpha_interval){			
			String alpha_str = format.format(alpha);

			//1. get the beta value range
			int size_min = Integer.MAX_VALUE;
			int size_max = Integer.MIN_VALUE;
			
			for(int i =0 ; i < criteria.length; i++){
				String criterion = criteria[i];
				
				System.out.println("Criterion:" + criterion + ",Alpha:" + alpha_str);
				
				HashMap<Integer, ArrayList<ILPOutput>> size_outputs = 
					criterion_alpha_size_outputs.get(criterion).get(Double.parseDouble(alpha_str));
				
				if(size_outputs!= null){
					Integer[] sizes = size_outputs.keySet().toArray(new Integer[0]);
					Arrays.sort(sizes);		
					if(sizes[0] < size_min){
						size_min = sizes[0];
					}
					if(sizes[sizes.length -1] > size_max){
						size_max = sizes[sizes.length - 1];
					}
					
					//curve fitting & Pearson correlation test between beta and FDR 
					//for each testing criterion and alpha
					double[] size_x = new double[sizes.length];
					double[] fdr_y = new double[sizes.length];
					for(int k = 0; k < sizes.length; k ++){
						size_x[k] = sizes[k];
						ArrayList<ILPOutput> outputs = size_outputs.get(sizes[k]);
						fdr_y[k] = outputs.get(0).fdr;
					}
					
					CurveFittingResult result = DataAnalyzeManager.getLinearCurveFitting(size_x, fdr_y);
					sb_curveFitting_beta_fdr.append(criterion).append("\t").append(alpha_str).append("\t").
					append(result.coefficient).append("\t").append(result.intercept).append("\t").
					append(result.inaccuracy).append("\n");
					
					double PearsonCorrelationCoefficient = 
						DataAnalyzeManager.getPearsonCorrelationTest(size_x, fdr_y);
					sb_PearsonCoefficient_beta_fdr.append(criterion).append("\t").append(alpha_str).append("\t").
					append(PearsonCorrelationCoefficient).append("\n");
				}
			}
			
			//2. save the correlation between beta and FDR/time
			sb_beta_fdr.append("Alpha\t\t");
			sb_beta_time.append("Alpha\t\t");
			for(int i = 0; i < criteria.length; i ++){
				sb_beta_fdr.append(criteria[i]).append("\t");
				sb_beta_time.append(criteria[i]).append("\t");
			}
			sb_beta_fdr.append("\n");
			sb_beta_time.append("\n");
			
			
			DecimalFormat format_fdr = new DecimalFormat("0.00000");
			for(int i = size_min; i <= size_max; i++ ){
				sb_beta_fdr.append(alpha_str).append("\t").append(i).append("\t");
				sb_beta_time.append(alpha_str).append("\t").append(i).append("\t");
				
				for(int j =0; j < criteria.length; j ++){
					String criterion = criteria[j];
					boolean exists = true;
					
					HashMap<Double, HashMap<Integer, ArrayList<ILPOutput>>> alpha_size_outputs = 
						criterion_alpha_size_outputs.get(criterion);
					
					if(!alpha_size_outputs.containsKey(Double.parseDouble(alpha_str))){
						exists = false;
					}else{
						HashMap<Integer, ArrayList<ILPOutput>> size_outputs = 
							alpha_size_outputs.get(Double.parseDouble(alpha_str));
						if(!size_outputs.containsKey(i)){
							exists = false;
						}
					}
					
					if(!exists){
						sb_beta_fdr.append("0\t");
						sb_beta_time.append("0\t");
					}else{
						ArrayList<ILPOutput> outputs = criterion_alpha_size_outputs.get(criterion).
						get(Double.parseDouble(alpha_str)).get(i);
						sb_beta_fdr.append(format_fdr.format(outputs.get(0).fdr)).append("\t");
						sb_beta_time.append(format_fdr.format(outputs.get(0).time)).append("\t");
					}
				}
				sb_beta_fdr.append("\n");
				sb_beta_time.append("\n");
			}
			sb_beta_fdr.append("\n");
			sb_beta_time.append("\n");
		}
		
		String saveFile = System.getProperty("user.dir") + "/src/ccr"
		+ "/experiment/Context-Intensity_backup/TestHarness/" + date
		+ "/ILPModel/Beta_FDR_First.txt";
		
		Logger.getInstance().setPath(saveFile, false);
		Logger.getInstance().write(sb_beta_fdr.toString());
		Logger.getInstance().close();
		
		saveFile = System.getProperty("user.dir") + "/src/ccr"
		+ "/experiment/Context-Intensity_backup/TestHarness/" + date
		+ "/ILPModel/Beta_Time_First.txt";
		
		Logger.getInstance().setPath(saveFile, false);
		Logger.getInstance().write(sb_beta_time.toString());
		Logger.getInstance().close();
		
		saveFile = System.getProperty("user.dir") + "/src/ccr"
		+ "/experiment/Context-Intensity_backup/TestHarness/" + date
		+ "/ILPModel/CurveFitting_Size_FDR_First.txt";
		
		Logger.getInstance().setPath(saveFile, false);
		Logger.getInstance().write(sb_curveFitting_beta_fdr.toString());
		Logger.getInstance().close();
		
		saveFile = System.getProperty("user.dir") + "/src/ccr"
		+ "/experiment/Context-Intensity_backup/TestHarness/" + date
		+ "/ILPModel/PearsonCorrelationCoefficient_Size_FDR_First.txt";
		
		Logger.getInstance().setPath(saveFile, false);
		Logger.getInstance().write(sb_PearsonCoefficient_beta_fdr.toString());
		Logger.getInstance().close();
	}
	
	/**2010-02-04: get the correlation between testing effectiveness/time
	 * and test suite sizes by fixing the alpha, the file will save the info
	 * such as "Alpha Beta Time FDR". 
	 * 
	 * @param date
	 * @param criterion
	 * @param mutantFile_date
	 * @param containHeader_mutant
	 * @param mutantDetailDir
	 * @param containHeader_testing
	 * @param alpha_min
	 * @param alpha_max
	 * @param alpha_interval
	 * @param single_enabled
	 * @return: alpha -> size -> ILPOuputs
	 */
	public static HashMap<Double, HashMap<Integer, ArrayList<ILPOutput>>> saveCostAndPerformance_sizeConstraint(String date, String criterion,
			String mutantFile_date, boolean containHeader_mutant, 
			String mutantDetailDir, boolean containHeader_testing, double alpha_min, double alpha_max, 
			double alpha_interval, boolean single_enabled){
		
		HashMap<Double, HashMap<Integer, ArrayList<ILPOutput>>> alpha_size_outputs  = 
			new HashMap<Double, HashMap<Integer, ArrayList<ILPOutput>>>();
		
		ArrayList<TestSet> testSets = new ArrayList<TestSet>();
		
		String testSetDir =  "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
			+ date + "/ILPModel/"+ criterion + "/";;
		boolean containHeader = false;
		String pattern = "";
		
		if(single_enabled){
			//2010-02-01:only interest in the first reduced test set
			pattern = "Model\\_" + criterion +"\\_SingleObj\\_0\\_output\\.txt";
			testSets = TestSetStatistics.loadReducedTestSet_offline(testSetDir, containHeader, pattern);
		}
		
		DecimalFormat format = new DecimalFormat("0.0");		
		//2. reduced test sets of bi-criteria ILP
		for(double alpha = alpha_min; Math.abs(alpha_max - alpha) > 0.0001; alpha = alpha + alpha_interval){			
			String alpha_str = format.format(alpha);
			
			//2010-02-01:only interest in the first reduced test set
			pattern = "Model\\_" + criterion +"\\_"+ alpha_str+"\\_[0-9]+" +
			"\\_0\\_output\\.txt";
			
			ArrayList<ILPOutput> output_biCriteria = TestSetStatistics.loadILPOutput_offline(
					testSetDir, containHeader, pattern);
			
			for(int i = 0; i < output_biCriteria.size(); i ++){
				ILPOutput output = output_biCriteria.get(i);
				
				ArrayList<TestSet> reducedTestSets = new ArrayList<TestSet>();
				reducedTestSets.add(output.reducedTestSet);
				
				output.fdr = getFaultDetectionRate_average_online(reducedTestSets, 
						mutantFile_date, containHeader_mutant, mutantDetailDir, 
						containHeader_testing);
				
				HashMap<Integer,ArrayList<ILPOutput>> size_outputs = alpha_size_outputs.get(alpha);
				if(size_outputs == null){
					size_outputs = new HashMap<Integer, ArrayList<ILPOutput>>();
				}
				ArrayList<ILPOutput> outputs = size_outputs.get(output.testSetLimit);
				if(outputs == null){
					outputs = new ArrayList<ILPOutput>();
				}
				outputs.add(output);
				size_outputs.put(output.testSetLimit, outputs);
				alpha_size_outputs.put(alpha, size_outputs);
			}
		}
		

		
		StringBuilder sb = new StringBuilder();
		sb.append("Alpha").append("\t").append("Beta").append("\t").
		append("Time").append("\t").append("FDR").append("\n");
		
		Double[] alphas = alpha_size_outputs.keySet().toArray(new Double[0]);
		Arrays.sort(alphas);		
		for(int i = 0; i < alphas.length; i ++){
			double alpha = alphas[i];			
			HashMap<Integer,ArrayList<ILPOutput>> size_outputs = alpha_size_outputs.get(alpha);
			
			Integer[] sizes = size_outputs.keySet().toArray(new Integer[0]);
			Arrays.sort(sizes);			
			for(int j = 0; j < sizes.length; j++){
				int size = sizes[j];
				ArrayList<ILPOutput> outputs = size_outputs.get(size);
				for(int k = 0; k < outputs.size(); k ++){
					ILPOutput output = outputs.get(k);
					sb.append(format.format(alpha)).append("\t").append(output.testSetLimit).
					append("\t").append(output.time).append("\t").append(output.fdr).append("\n");	
				}
			}
		}

		String saveFile = System.getProperty("user.dir") + "/src/ccr"
		+ "/experiment/Context-Intensity_backup/TestHarness/" + date
		+ "/ILPModel/"+ criterion+"/VaryBeta_fixedAlpha_First.txt";
		
		Logger.getInstance().setPath(saveFile, false);
		Logger.getInstance().write(sb.toString());
		Logger.getInstance().close();
		
		return alpha_size_outputs;
	}
	
	/**2010-03-19: get detailed fdr w.r.t each mutant for a given set of adequate test sets
	 * 
	 * @param date
	 * @param criterion
	 * @param mutantFile_date
	 * @param containHeader_mutant
	 * @param mutantDetailDir
	 * @param containHeader_testing
	 * @param alpha_min
	 * @param alpha_max
	 * @param alpha_interval
	 * @param sizeConstraint_min
	 * @param sizeConstraint_max
	 * @param single_enabled
	 * @return
	 */
	public static HashMap<Double, HashMap<String, Double>> getFaultDetectionRate_detailed_BILP_offline(String date, String criterion,
			String mutantFile_date, boolean containHeader_mutant, 
			String mutantDetailDir, boolean containHeader_testing, double alpha_min, double alpha_max, 
			double alpha_interval, int sizeConstraint_min, int sizeConstraint_max, String H_L_D, boolean single_enabled){

		HashMap<Double, HashMap<String, Double>> alpha_mutant_fdr = new 
			HashMap<Double, HashMap<String,Double>>();
		
		
		HashMap<Double, ArrayList<TestSet>> alpha_testSets  = new HashMap<Double, ArrayList<TestSet>>();
		
		ArrayList<TestSet> testSets = new ArrayList<TestSet>();
		
		String testSetDir = "";
		boolean containHeader = false;
		String pattern = "";
		if(single_enabled){

			testSetDir =  "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
				+ date + "/ILPModel/"+ criterion + "/";
			containHeader = false;
//			pattern = "Model\\_" + criterion +"\\_SingleObj\\_[0-9]+\\_output\\.txt";
			//2010-02-01:only interest in the first reduced test set
			pattern = "Model\\_" + criterion +"\\_SingleObj\\_0\\_RA\\_"+H_L_D+"\\_output\\.txt";
			testSets = TestSetStatistics.loadReducedTestSet_offline(testSetDir, containHeader, pattern);
	}
		
		
		//2. reduced test sets of bi-criteria ILP
		//2010-02-01: for two double we  use "|alpha_max - alpha| > 0.0001" rather than "alpha_max - alpha"
		// to determine whether alpha_max is larger than alpha or not
		for(double alpha = alpha_min; Math.abs(alpha_max - alpha) > 0.0001; alpha = alpha + alpha_interval){
			DecimalFormat format = new DecimalFormat("0.0");
			String alpha_str = format.format(alpha);
			
			testSetDir =  "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
				+ date + "/ILPModel/"+ criterion + "/";
			containHeader = false;
			
			//2010-03-18: interest in multiple reduced test sets with varying sizeConstraint
//			pattern = "Model\\_" + criterion +"\\_"+ alpha_str+"\\_[0-9]+" +
//			"\\_[0-9]+\\_output\\.txt";
			
			//2010-02-01:only interest in the first reduced test set with varying sizeConstraint
//			pattern = "Model\\_" + criterion +"\\_"+ alpha_str+"\\_[0-9]+" +
//			"\\_0\\_output\\.txt";
			
			//2010-02-07: only interest in the first reduced test set with some size constraint;
			ArrayList<TestSet> testSet_biCriteria = new ArrayList<TestSet>();
			for(int i = sizeConstraint_min; i < sizeConstraint_max; i ++){
				int sizeConstraint = i;
				//2010-02-07: only interest in the first reduced test set with a given sizeConstraint
//				pattern = "Model\\_" + criterion +"\\_"+ alpha_str+"\\_" + sizeConstraint +
//				"\\_0\\_output\\.txt";	
				
				//2010-03-18: interest in multiple reduced test sets with a 
				pattern = "Model\\_" + criterion +"\\_"+ alpha_str+"\\_" + sizeConstraint +
				"\\_[0-9]+\\_RA_"+H_L_D+"\\_output\\.txt";
				
				 ArrayList<TestSet> testsets = TestSetStatistics.loadReducedTestSet_offline(
						testSetDir, containHeader, pattern);
				 for(int j = 0; j < testsets.size(); j ++){
					 testSet_biCriteria.add(testsets.get(j));
				 }
			}
			alpha_testSets.put(alpha, testSet_biCriteria);
		}
		
		
		//3. combine all test sets
		if(testSets.size()!=0){ //single-objective ILP is enabled
			alpha_testSets.put(Double.MIN_VALUE, testSets);
		}
		
		Double[] alphas = alpha_testSets.keySet().toArray(new Double[0]);
		Arrays.sort(alphas);		
		for(int i = 0; i < alphas.length; i ++){
			double alpha = alphas[i];
			ArrayList<TestSet> reducedTestSets = alpha_testSets.get(alpha);
			
			//2010-02-01: fix another important bug here
			HashMap<String, Double> mutant_fdr = getFaultDetectionRate_detailed_online(
					reducedTestSets, mutantFile_date, containHeader_mutant, mutantDetailDir, 
					containHeader_testing);
			
			alpha_mutant_fdr.put(alpha, mutant_fdr);
		}
		
		return alpha_mutant_fdr;
	}
	
	
	/**2010-02-01: get averaged fault detection rates of reduced test sets
	 * with respect to a set of mutants in an offline way
	 * 
	 * @param date
	 * @param criterion
	 * @param mutantFile_date
	 * @param containHeader_mutant
	 * @param mutantDetailDir
	 * @param containHeader_testing
	 * @param alpha_min: inclusive
	 * @param alpha_max: exclusive
	 * @param alpha_interval
	 * @param sizeConstraint_min: inclusive
	 * @param sizeConstraint_max: exclusive
	 * @param single_enabled
	 * @return
	 */
	public static HashMap<Double, Double> getFaultDetectionRate_averaged_BILP_offline(String date, String criterion,
			String mutantFile_date, boolean containHeader_mutant, 
			String mutantDetailDir, boolean containHeader_testing, double alpha_min, double alpha_max, 
			double alpha_interval, int sizeConstraint_min, int sizeConstraint_max, boolean single_enabled){

		HashMap<Double, Double> alpha_fdr = new HashMap<Double, Double>();
		
		HashMap<Double, ArrayList<TestSet>> alpha_testSets  = new HashMap<Double, ArrayList<TestSet>>();
		
		ArrayList<TestSet> testSets = new ArrayList<TestSet>();
		
		String testSetDir = "";
		boolean containHeader = false;
		String pattern = "";
		if(single_enabled){
//			testSets = ILPSolver.buildAndSolveILP_SingleObj_Manager(date, criterion, testSetNum, timeLimit, sleepTime);

			testSetDir =  "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
				+ date + "/ILPModel/"+ criterion + "/";
			containHeader = false;
//			pattern = "Model\\_" + criterion +"\\_SingleObj\\_[0-9]+\\_output\\.txt";
			//2010-02-01:only interest in the first reduced test set
			pattern = "Model\\_" + criterion +"\\_SingleObj\\_0\\_output\\.txt";
			testSets = TestSetStatistics.loadReducedTestSet_offline(testSetDir, containHeader, pattern);
	}
		
		
		//2. reduced test sets of bi-criteria ILP
		//2010-02-01: for two double we  use "|alpha_max - alpha| > 0.0001" rather than "alpha_max - alpha"
		// to determine whether alpha_max is larger than alpha or not
		for(double alpha = alpha_min; Math.abs(alpha_max - alpha) > 0.0001; alpha = alpha + alpha_interval){
			DecimalFormat format = new DecimalFormat("0.0");
			String alpha_str = format.format(alpha);
			
			testSetDir =  "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
				+ date + "/ILPModel/"+ criterion + "/";
			containHeader = false;
			
			//2010-03-18: interest in multiple reduced test sets with varying sizeConstraint
//			pattern = "Model\\_" + criterion +"\\_"+ alpha_str+"\\_[0-9]+" +
//			"\\_[0-9]+\\_output\\.txt";
			
			//2010-02-01:only interest in the first reduced test set with varying sizeConstraint
//			pattern = "Model\\_" + criterion +"\\_"+ alpha_str+"\\_[0-9]+" +
//			"\\_0\\_output\\.txt";
			
			//2010-02-07: only interest in the first reduced test set with some size constraint;
			ArrayList<TestSet> testSet_biCriteria = new ArrayList<TestSet>();
			for(int i = sizeConstraint_min; i < sizeConstraint_max; i ++){
				int sizeConstraint = i;
				//2010-02-07: only interest in the first reduced test set with a given sizeConstraint
//				pattern = "Model\\_" + criterion +"\\_"+ alpha_str+"\\_" + sizeConstraint +
//				"\\_0\\_output\\.txt";	
				
				//2010-03-18: interest in multiple reduced test sets with a 
				pattern = "Model\\_" + criterion +"\\_"+ alpha_str+"\\_" + sizeConstraint +
				"\\_[0-9]+\\_output\\.txt";
				
				 ArrayList<TestSet> testsets = TestSetStatistics.loadReducedTestSet_offline(
						testSetDir, containHeader, pattern);
				 for(int j = 0; j < testsets.size(); j ++){
					 testSet_biCriteria.add(testsets.get(j));
				 }
			}
			alpha_testSets.put(alpha, testSet_biCriteria);
		}
		
		
		//3. combine all test sets
		if(testSets.size()!=0){ //single-objective ILP is enabled
			alpha_testSets.put(Double.MIN_VALUE, testSets);
		}
		
		Double[] alphas = alpha_testSets.keySet().toArray(new Double[0]);
		Arrays.sort(alphas);		
		for(int i = 0; i < alphas.length; i ++){
			double alpha = alphas[i];
			ArrayList<TestSet> reducedTestSets = alpha_testSets.get(alpha);
			
			//2010-02-01: fix another important bug here
			double fdr = getFaultDetectionRate_average_online(reducedTestSets,
					mutantFile_date, containHeader_mutant, mutantDetailDir, containHeader_testing);
			
			alpha_fdr.put(alpha, fdr);
		}
		
		return alpha_fdr;
	}
	
	
	/**2010-01-27: get fault detection rates of reduced test sets 
	 * with respect to a set of mutants
	 * 
	 * @param date
	 * @param criterion
	 * @param mutantFile_date
	 * @param containHeader_mutant
	 * @param mutantDetailDir
	 * @param containHeader_testing
	 * @param alpha_min: inclusive
	 * @param alpha_max: exclusive
	 * @param alpha_interval
	 * @param sizeConstraint_min: inclusive
	 * @param sizeConstraint_max: exclusive
	 * @param testSetNum:less than 1 means to use its 
	 * reduced test set sizes solved by single-objective ILP model
	 * as the beta value
	 * @param single_enabled: true to enable the single-objective model
	 * @param timeLimit: the maximum time to solve the ILP model 
	 * @param sleepTime
	 * @return
	 */
	public static HashMap<Double, Double> getFaultDetectionRate_averaged_BILP(String date, String criterion,
			String mutantFile_date, boolean containHeader_mutant, 
			String mutantDetailDir, boolean containHeader_testing, double alpha_min, double alpha_max, 
			double alpha_interval, int sizeConstraint_min, int sizeConstraint_max, int testSetNum, boolean single_enabled, long timeLimit, long sleepTime){

		HashMap<Double, Double> alpha_fdr = new HashMap<Double, Double>();
		
		HashMap<Double, ArrayList<TestSet>> alpha_testSets  = new HashMap<Double, ArrayList<TestSet>>();
		
		ArrayList<TestSet> testSets = null;
		if(single_enabled){
			testSets = ILPSolver.buildAndSolveILP_SingleObj_Manager(date, criterion, testSetNum, timeLimit, sleepTime);
//			testSets = ILPSolver.buildAndSolveILP_SingleObj_Manager(date, criterion, testSetNum);			
		}
		
		
		if(sizeConstraint_max < 1 &&  testSets != null){ 
			//1. reduced test sets of single-objective ILP							
			sizeConstraint_max = testSets.get(0).size();
		}
		
		//2. reduced test sets of bi-criteria ILP
		for(int i = sizeConstraint_min; i < sizeConstraint_max; i ++){
			int sizeConstraint = i;
			alpha_testSets = ILPSolver.buildAndSolveILPs_BiCriteria_Manager(date, criterion, alpha_min, alpha_max, 
					alpha_interval, sizeConstraint, testSetNum, timeLimit, sleepTime);		
		}
			

		
		
//		alpha_testSets = ILPSolver.buildAndSolveILPs_BiCriteria_Manager(date, criterion, alpha_min, alpha_max, 
//				alpha_interval, maxSize, testSetNum);
		
		
		//3. combine all test sets
		if(testSets != null){ //single-objective ILP is enabled
			alpha_testSets.put(Double.MIN_VALUE, testSets);
		}
		
		Double[] alphas = alpha_testSets.keySet().toArray(new Double[0]);
		Arrays.sort(alphas);		
		for(int i = 0; i < alphas.length; i ++){
			double alpha = alphas[i];
			ArrayList<TestSet> reducedTestSets = alpha_testSets.get(alpha);
			
			double fdr = getFaultDetectionRate_average_online(reducedTestSets,
					mutantFile_date, containHeader_mutant, mutantDetailDir, containHeader_testing);
			
			alpha_fdr.put(alpha, fdr);
		}
		
		return alpha_fdr;
	}
	
	
	
	public static void saveToFile_alpha_mutant_fdrs(HashMap<Double, HashMap<String, Double>> alpha_mutant_fdr, String saveFile){
		
		Double[] alphas = alpha_mutant_fdr.keySet().toArray(new Double[0]);
		Arrays.sort(alphas);

		StringBuilder sb = new StringBuilder();
//		sb.append("Alpha\tMutant\tFaultDetectionRate\n");
		sb.append("alpha\tmin\tmean\tmedian\tmax\tstd\n");
		DecimalFormat format = new DecimalFormat("0.00000");
		
		for(int i = 0; i < alphas.length; i ++){
			double alpha = alphas[i];			
			HashMap<String, Double> mutant_fdr = alpha_mutant_fdr.get(alpha);
			String[] mutants = mutant_fdr.keySet().toArray(new String[0]);
			Arrays.sort(mutants);
			
			double[] fdrs = new double[mutants.length];
			
			if(alpha == Double.MIN_VALUE){ //2010-03-23:for single-objective ILP models
				for(int j = 0; j < mutants.length; j ++){
					String mutant = mutants[j];
					double fdr = mutant_fdr.get(mutant);
					fdrs[j] = fdr;
//					sb.append("SingleObj").append("\t").append(mutant).append("\t").
//					append(format.format(fdr)).append("\n");
				}
				DataDescriptionResult result = DataAnalyzeManager.getDataDescriptive(fdrs);
				
				sb.append(format.format(alpha)).append("\t").append(result.min).append("\t").append(result.mean).append("\t").
				append(result.median).append("\t").append(result.max).append("\t").append(result.std).append("\n");
			}else{
				for(int j = 0; j < mutants.length; j ++){
					String mutant = mutants[j];
					double fdr = mutant_fdr.get(mutant);
					fdrs[j] = fdr;
//					sb.append(format.format(alpha)).append("\t").append(mutant).append("\t").
//					append(format.format(fdr)).append("\n");	
				}				
				DataDescriptionResult result = DataAnalyzeManager.getDataDescriptive(fdrs);
				sb.append(format.format(alpha)).append("\t").append(result.min).append("\t").append(result.mean).append("\t").
				append(result.median).append("\t").append(result.max).append("\t").append(result.std).append("\n");
			}
		}
		
		Logger.getInstance().setPath(saveFile, false);
		Logger.getInstance().write(sb.toString());
		Logger.getInstance().close();
	}
	
	public static void saveToFile_alpha_fdr(HashMap<Double, Double> alpha_fdr, String saveFile){
		
		Double[] alphas = alpha_fdr.keySet().toArray(new Double[0]);
		Arrays.sort(alphas);

		StringBuilder sb = new StringBuilder();
		sb.append("Alpha\tFaultDetectionRate\n");
		
		DecimalFormat format = new DecimalFormat("0.00000");
		
		for(int i = 0; i < alphas.length; i ++){
			double alpha = alphas[i];			
			double fdr = alpha_fdr.get(alpha);
			if(alpha == Double.MIN_VALUE){
				sb.append("SingleObj").append("\t").append(format.format(fdr)).append("\n");
			}else{
				sb.append(format.format(alpha)).append("\t").append(format.format(fdr)).append("\n");	
			}
		}
		
		Logger.getInstance().setPath(saveFile, false);
		Logger.getInstance().write(sb.toString());
		Logger.getInstance().close();
	}
	

	public static void saveToFile_alpha_times(HashMap<Double, ArrayList<Double>> alpha_times, String saveFile){
		
		Double[] alphas = alpha_times.keySet().toArray(new Double[0]);
		Arrays.sort(alphas);

		StringBuilder sb = new StringBuilder();
		
		DecimalFormat format = new DecimalFormat("0.00000");
	
		sb.append("Alpha\tTestSuiteNum\t").append("min\tmean\tmedian\tmax\tstd\n");;
		for(int i = 0; i < alphas.length; i ++){
			double alpha = alphas[i];			
			ArrayList<Double> times = alpha_times.get(alpha);
			double[] times_tmp = new double[times.size()];
			for(int j =0; j < times.size(); j ++){
				times_tmp[j] = times.get(j);
			}
			DataDescriptionResult result = DataAnalyzeManager.getDataDescriptive(times_tmp);
		
			
			if(alpha == Double.MIN_VALUE){
				sb.append("SingleObject\t");
			}else{
				sb.append(format.format(alpha)).append("\t");	
			}
			
			sb.append(result.min).append("\t").append(times_tmp.length).append("\t").append(result.mean).append("\t").
			append(result.median).append("\t").append(result.max).append("\t").append(result.std).append("\n");
		}
		
		Logger.getInstance().setPath(saveFile, false);
		Logger.getInstance().write(sb.toString());
		Logger.getInstance().close();
	}
	
	public static void main(String[] args) {
		String instruction = args[0];
		if(instruction.equals("getFDR_offline")){
			String date_testSets = "20091026";
			String criterion = "AllPolicies";
			int testSuiteSize = -1;
			String oldOrNew = "old";
			String randomOrCriterion = "random";
			String H_L_R = "H";
			int size_ART = 64;
			String date_testPool = "20100121";
			if(args.length > 8){
				date_testSets = args[1];
				criterion = args[2];
				testSuiteSize = Integer.parseInt(args[3]);
				oldOrNew = args[4];
				randomOrCriterion = args[5];
				H_L_R = args[6];
				size_ART = Integer.parseInt(args[7]);
				date_testPool = args[8];
			}
			
			String testSetFile = TestSetStatistics.getTestSetFile(date_testSets, criterion, testSuiteSize, 
					oldOrNew, randomOrCriterion, H_L_R, size_ART);
			
			String mutantFile_date = "20100121";
			String mutantDetail_date = "20100121";
			
			if(args.length > 10){
				mutantFile_date = args[9];
				mutantDetail_date = args[10];
			}
			
			String mutantDetailDir = System.getProperty("user.dir") + "/src/ccr"
			+ "/experiment/Context-Intensity_backup/TestHarness/" + mutantDetail_date
			+ "/Mutant/";
			
			boolean containHeader_testSet = false;
			boolean containHeader_testing = true;
			boolean containHeader_mutant = false;
			
			long start = System.currentTimeMillis();
			double FDR = getFaultDetectionRate_average_offline(testSetFile, 
					containHeader_testSet, mutantFile_date, containHeader_mutant, mutantDetailDir, containHeader_testing);
			long duration = (System.currentTimeMillis() - start)/(1000*60);
			System.out.println("It takes " + duration + " mins to process all faults");
			System.out.println("the averaged fault detection rate:" + FDR);
		}else if(instruction.equals("getFDR_reduction")|| instruction.equals("getFDR_reduction_offline")
				||instruction.equals("saveCostAndPerformance")||
				instruction.equals("saveCostAndPerformance_AllInOne")){
			
			long start = System.currentTimeMillis();
			String date = args[1];			 
			//AllPolicies,All1ResolvedDU,All2ResolvedDU,AllStatement
			String criterion = args[2];
			int sizeConstraint_min = Integer.parseInt(args[3]);
			int sizeConstraint_max = Integer.parseInt(args[4]);
			int testSetNum = Integer.parseInt(args[5]);
			boolean single_enabled = Boolean.parseBoolean(args[6]);
			long timeLimit = Long.parseLong(args[7]);
			long sleepTime = Long.parseLong(args[8]); //1000 = 1 second
			if(timeLimit == 0){ //"0" means no time limit
				timeLimit = Long.MAX_VALUE;
//				sleepTime = 60 * 60 * 1000; //1 hour
			}
			
			
			String mutantFile_date = "20100121";
			String mutantDetail_date = "20100121";
			boolean containHeader_mutant = false;
			boolean containHeader_testing = true;
			
			String mutantDetailDir = System.getProperty("user.dir") + "/src/ccr"
			+ "/experiment/Context-Intensity_backup/TestHarness/" + mutantDetail_date
			+ "/Mutant/";
			
			if(args.length == 11){
				mutantFile_date = args[9];
				mutantDetail_date = args[10];
			}
			
			double alpha_min = 0.0;
			double alpha_max = 1.1;
			double alpha_interval = 0.1;
			if(args.length == 12){
				alpha_min = Double.parseDouble(args[9]);
				alpha_max = Double.parseDouble(args[10]);
				alpha_interval = Double.parseDouble(args[11]);
			}
			
			if(instruction.equals("getFDR_reduction")){
				//2010-01-31: solve the ILP model within a time limit
				HashMap<Double, Double> alpha_fdr = new HashMap<Double, Double>();
				alpha_fdr = getFaultDetectionRate_averaged_BILP(date, 
						criterion, mutantFile_date, containHeader_mutant, mutantDetailDir, containHeader_testing, 
						alpha_min, alpha_max, alpha_interval, sizeConstraint_min, sizeConstraint_max, 
						testSetNum, single_enabled, timeLimit, sleepTime);
				
				String saveFile = System.getProperty("user.dir") + "/src/ccr"
				+ "/experiment/Context-Intensity_backup/TestHarness/" + date
				+ "/ILPModel/"+ criterion+"/FaultDetectionRate_First.txt";
				saveToFile_alpha_fdr(alpha_fdr, saveFile);
				
				long duration = System.currentTimeMillis() - start;
				System.out.println("[Reporter_Reduction.Main]It takes " + duration/(1000*60) + " mins for " + criterion);
				System.exit(0);
			}else if(instruction.equals("getFDR_reduction_offline")){
				HashMap<Double, Double> alpha_fdr = new HashMap<Double, Double>();
				alpha_fdr = getFaultDetectionRate_averaged_BILP_offline(date, 
						criterion, mutantFile_date, containHeader_mutant, mutantDetailDir, containHeader_testing, 
						alpha_min, alpha_max, alpha_interval, sizeConstraint_min,
						sizeConstraint_max, single_enabled);
				
				String saveFile = System.getProperty("user.dir") + "/src/ccr"
				+ "/experiment/Context-Intensity_backup/TestHarness/" + date
				+ "/ILPModel/"+ criterion+"/FaultDetectionRate_First.txt";
				saveToFile_alpha_fdr(alpha_fdr, saveFile);
				
				long duration = System.currentTimeMillis() - start;
				System.out.println("[Reporter_Reduction.Main]It takes " + duration/(1000*60) + " mins for " + criterion);
				System.exit(0);
			}else if(instruction.equals("saveCostAndPerformance")){
				saveCostAndPerformance_sizeConstraint(date, criterion, mutantFile_date, containHeader_mutant, 
						mutantDetailDir, containHeader_testing, alpha_min, alpha_max, 
						alpha_interval, single_enabled);				
			}else if(instruction.equals("saveCostAndPerformance_AllInOne")){				
				saveCostAndPerformance_AllInOne_offline(date, alpha_min, alpha_max, alpha_interval);
			}
		}else if(instruction.equals("getTimeCost_reduction")){
			
			String date = args[1];			 
			//AllPolicies,All1ResolvedDU,All2ResolvedDU,AllStatement
			String criterion = args[2];
			int sizeConstraint = Integer.parseInt(args[3]);
			
			boolean single_enabled = Boolean.parseBoolean(args[6]);
			
			double alpha_min = 0.0;
			double alpha_max = 1.1;
			double alpha_interval = 0.1;
			if(args.length == 12){
				alpha_min = Double.parseDouble(args[9]);
				alpha_max = Double.parseDouble(args[10]);
				alpha_interval = Double.parseDouble(args[11]);
			}
			
			HashMap<Double, Double> alpha_time = new HashMap<Double, Double>();
			alpha_time = getTimeCost_averaged_BILP_offline(date, criterion, 
					alpha_min, alpha_max, alpha_interval, sizeConstraint, single_enabled);
			
			String saveFile = System.getProperty("user.dir") + "/src/ccr"
			+ "/experiment/Context-Intensity_backup/TestHarness/" + date
			+ "/ILPModel/"+ criterion+"/TimeCost_First.txt";
			saveToFile_alpha_fdr(alpha_time, saveFile);
		}else if(instruction.equals("getReductionSize")){
			
			String date = args[1];			 
			//AllPolicies,All1ResolvedDU,All2ResolvedDU,AllStatement
			String criterion = args[2];
			int sizeConstraint = Integer.parseInt(args[3]);
			
			boolean single_enabled = Boolean.parseBoolean(args[6]);
			
			double alpha_min = 0.0;
			double alpha_max = 1.1;
			double alpha_interval = 0.1;
			if(args.length == 11){
				alpha_min = Double.parseDouble(args[9]);
				alpha_max = Double.parseDouble(args[10]);
				alpha_interval = Double.parseDouble(args[11]);
			}
			
			HashMap<Double, Double> alpha_size = new HashMap<Double, Double>();
			alpha_size = getReductionSize_averaged_BILP_offline(date, criterion, 
					alpha_min, alpha_max, alpha_interval, sizeConstraint, single_enabled); 
				
			
			String saveFile = System.getProperty("user.dir") + "/src/ccr"
			+ "/experiment/Context-Intensity_backup/TestHarness/" + date
			+ "/ILPModel/"+ criterion+"/ReductionSize_First.txt";
			saveToFile_alpha_fdr(alpha_size, saveFile);
		}
	}

}
