package ccr.test;

import ccr.app.*;

import java.util.*;

public class TestSet {
	
	public static String COVERAGE_TAG = "Coverage:\t";
	public static String SIZE_TAG = "Size:\t";
	public Vector testcases;
	public double coverage = 0.0;
	
	//1/15/2008
	public long geneTime; //the cost of test sets
	
	//2009-2-15
	public String index;
	
	//2009-2-21: replace counters
	public int replaceCounter=0;
	
	//2009-10-15: the tie between activation and CI. 
	public int tie_activation_CI = 0;
	
	public TestSet() {
		
		testcases = new Vector();
	}
	
	public TestSet(String s) {
		index = s.substring(0, s.indexOf("\t"));
		
		s = s.substring(s.indexOf("\t")+"\t".length());
		testcases = new Vector();
		int i = s.indexOf(Application.SET_PREFIX);
		int j = s.indexOf(Application.SET_POSTFIX, i);
		String set = s.substring(i + Application.SET_PREFIX.length(), j);
		StringTokenizer st = new StringTokenizer(set, Application.SET_DELIMITER);
		while (st.hasMoreTokens()) {
			add(st.nextToken());
		}
		i = s.indexOf(COVERAGE_TAG) + COVERAGE_TAG.length();
		j = s.indexOf("\t", i);
		coverage = Double.parseDouble(s.substring(i, j));
		
	}
	
	public TestSet copy(){
		TestSet testpool  = new TestSet();
		for(int i = 0; i < this.size(); i ++){
			testpool.add(this.get(i));
		}		
		return testpool;
	}
	
	public void add(String testcase) {
		if (!testcases.contains(testcase)) {
			testcases.add(testcase);
		}
	}
	
	public void add(TestSet set) {
		
		for (int i = 0; i < set.size(); i++) {
			add(set.get(i));
		}
	}
	
	public void clear() {
		
		testcases.clear();
	}
	
	public boolean contains(String testcase) {
		
		return testcases.contains(testcase);
	}
	
	public boolean equals(Object object) {
		
		if (!(object instanceof TestSet)) {
			return false;
		}
		TestSet set = (TestSet) object;
		boolean equal = true;
		if (size() != set.size()) {
			equal = false;
		} else {
			for (int i = 0; i < set.size(); i++) {
				if (!contains(set.get(i))) {
					equal = false;
					break;
				}
			}
		}
		return equal;
	}
	
	public String get(int i) {
		
		if (i < 0 || i >= testcases.size()) {
			return null;
		}
		return (String) testcases.get(i);
	}
	
	public String getByRandom() {
		
		if (isEmpty()) {
			return null;
		}
		//2009-03-11: more likely to return a uniform distributed test case
		return get(new Random().nextInt(size()));
//		return get((int) (Math.random() * (double) size()));
	}

	/**2009-10-22: remove one test case in the test set which has the nearest distances
	 * to reminding test cases.
	 * 
	 * @param testSet
	 * @return
	 */
	public TestSet removeTestCase_ART(TestSet testSet){		
		double[] CIs = new double[testSet.size()]; //keep CIs of all test cases in the test set
		for(int i = 0; i < testSet.size(); i ++){
			String index_testcase = (String)testSet.get(i);
			CIs[i] = ((TestCase)Adequacy.testCases.get(index_testcase)).CI;
		}
		
		double[] distances = new double[testSet.size()]; //keep distances of a test case to reminding ones 
		for(int i = 0; i < distances.length; i ++){
			double CI_testcase = CIs[i];			
			double distance = 0.0;
			
			for(int j = 0 ; j < CIs.length ; j ++){
				if(j != i){
					double CI_temp = CIs[j];
					distance += Math.abs(CI_testcase - CI_temp);	
				}				
			}			
			distances[i] = distance;
		}
		
		//get the test case with minimum distance from reminding test cases
		double min_dist = Double.MAX_VALUE;
		int min_index = -1;
		for(int i = 0; i < distances.length; i ++){
			double dis = distances[i];
			if(dis < min_dist){
				min_dist = dis;
				min_index = i;
			}
		}
		testSet.remove(min_index); // remove the test case with minimum distances from reminding test cases
		testSet.replaceCounter ++;
		return testSet;
	}
	
	/**2009-10-25:used by RA-R to sample test cases with ART, but since the test pool size
	 * can be reduced, it needs to check the size before sampling 
	 * 
	 * @param testset
	 * @param size_TestSet
	 * @return
	 */
	public String getByART_best(TestSet testset, int size_TestSet){
		//1.get CIs of test cases in the test set
		ArrayList<Double> CI_testcases = new ArrayList<Double>();
		for(int i = 0; i < testset.size(); i ++){
			String index_testcase = (String)testset.get(i);
			double CI_testcase = ((TestCase)Adequacy.testCases.get(index_testcase)).CI;
			CI_testcases.add(CI_testcase);
		}
		
		//2.get 10 random candidate test sets, and their distances to test cases in testset
		ArrayList<String> tmpTestSet = new ArrayList<String>();
		ArrayList<Double> dis_TempTestSet = new ArrayList<Double>();
		
		if(this.size() <= size_TestSet){
			//2009-10-25:if the size is too small, then add all test cases to tmpTestSet
			for(int i = 0; i < this.size(); i ++){
				String testcase = this.get(i);
				tmpTestSet.add(testcase);
				double CI_testcase = ((TestCase)Adequacy.testCases.get(testcase)).CI;
				double distance = 0.0;
				for(Double CI: CI_testcases){
					distance += Math.abs(CI - CI_testcase);
				}
				dis_TempTestSet.add(distance);
			}
		}else{//2009-10-25:
			while(tmpTestSet.size() < size_TestSet){
				String testcase = this.getByRandom();
				if(!tmpTestSet.contains(testcase) && !testset.contains(testcase)){
					tmpTestSet.add(testcase);
					double CI_testcase = ((TestCase)Adequacy.testCases.get(testcase)).CI;
					double distance = 0.0;
					for(Double CI: CI_testcases){
						distance += Math.abs(CI - CI_testcase);
					}
					dis_TempTestSet.add(distance);
				}
			}	
		}
		
		//3. get the test case with max distance from existing test cases
		double max_dist = Double.MIN_VALUE;
		int max_index = 0;
		for(int i = 0; i < dis_TempTestSet.size(); i ++){
			double dis = dis_TempTestSet.get(i);
			if(dis > max_dist){
				max_dist = dis;
				max_index = i;
			}
		}
		
		return tmpTestSet.get(max_index);
	}
	
	/**2009-08-19: RA_R which sample a test case which has the largest distance from the existing test set
	 * @param testset
	 * @param size_TestSet: the size of ART-constructed test sets(default value = 10)
	 * @return
	 */
	public String getByART(TestSet testset, int size_TestSet){
		//1.get CIs of test cases in the test set
		ArrayList<Double> CI_testcases = new ArrayList<Double>();
		for(int i = 0; i < testset.size(); i ++){
			String index_testcase = (String)testset.get(i);
			double CI_testcase = ((TestCase)Adequacy.testCases.get(index_testcase)).CI;
			CI_testcases.add(CI_testcase);
		}
		
		//2.get 10 random candidate test sets, and their distances to test cases in testset
		ArrayList<String> tmpTestSet = new ArrayList<String>();
		ArrayList<Double> dis_TempTestSet = new ArrayList<Double>();
		
		while(tmpTestSet.size() < size_TestSet){
			String testcase = this.getByRandom();
			if(!tmpTestSet.contains(testcase) && !testset.contains(testcase)){
				tmpTestSet.add(testcase);
				double CI_testcase = ((TestCase)Adequacy.testCases.get(testcase)).CI;
				double distance = 0.0;
				for(Double CI: CI_testcases){
					distance += Math.abs(CI - CI_testcase);
				}
				dis_TempTestSet.add(distance);
			}
		}
		
		//3. get the test case with max distance from existing test cases
		double max_dist = Double.MIN_VALUE;
		int max_index = 0;
		for(int i = 0; i < dis_TempTestSet.size(); i ++){
			double dis = dis_TempTestSet.get(i);
			if(dis > max_dist){
				max_dist = dis;
				max_index = i;
			}
		}
		
		return tmpTestSet.get(max_index);
	}
	
	/**2009-10-15:RA_R which returns a test case which has the largest distance from 
	 * the given TestSet in terms of activation
	 * @param testset
	 * @param size_TestSet
	 * @return
	 */
	public String getByART_activation(TestSet testset, int size_TestSet){
		//1.get activations of test cases in the test set
		ArrayList<Integer> activation_testcases = new ArrayList<Integer>();
		for(int i = 0; i < testset.size(); i ++){
			String index_testcase = (String)testset.get(i);
			int activation_testcase = ((TestCase)Adequacy.testCases.get(index_testcase)).activation;
			activation_testcases.add(activation_testcase);
		}
		
		//2.get 10 random candidate test sets, and their distances to test cases in testset
		ArrayList<String> tmpTestSet = new ArrayList<String>();
		ArrayList<Double> dis_TempTestSet = new ArrayList<Double>();
		
		while(tmpTestSet.size() < size_TestSet){
			String testcase = this.getByRandom();
			if(!tmpTestSet.contains(testcase) && !testset.contains(testcase)){ // has not been visited
				tmpTestSet.add(testcase);
				int activation_testcase = ((TestCase)Adequacy.testCases.get(testcase)).activation;
				double distance = 0.0;
				for(int activation: activation_testcases){
					distance += Math.abs(activation - activation_testcase);
				}
				dis_TempTestSet.add(distance);
			}
		}
		
		//3. get the test case with max distance from existing test cases
		double max_dist = Double.MIN_VALUE;
		int max_index = 0;
		for(int i = 0; i < dis_TempTestSet.size(); i ++){
			double dis = dis_TempTestSet.get(i);
			if(dis > max_dist){
				max_dist = dis;
				max_index = i;
			}
		}
		
		return tmpTestSet.get(max_index);
	}
	
//	public String getByART(TestSet testset){
//
//		//1. prepare for the ART choosing
//		ArrayList CI_testcases = new ArrayList();
//
//		for (int i = 0; i < testset.size(); i++) {
//			String testcase = (String) testset.get(i);
//			double CI_testcase = ((TestCase) Adequacy.testCases.get(testcase)).CI;
//
//			// insert it into testcases
//			if (CI_testcases.size() != 0) {
//				for (int j = 0; j < CI_testcases.size(); j++) {
//					double CI_tc = (Double)CI_testcases.get(j);					
//					if (CI_tc < CI_testcase) {
//						// right place to insert testcase, 
//						//all CI in test cases are ordered in descending order
//						CI_testcases.add(j, CI_testcase);
//						break;
//					}
//				}
//			} else {
//				CI_testcases.add(CI_testcase);
//			}
//		}
//
//		// 2. use ART-algorithm to choose 10 candidate test cases
//		ArrayList tmp = new ArrayList();
//		int trial = 0;
//		do {
//			String testcase = this.getByRandom();
//			trial ++;
//			if (!tmp.contains(testcase)) {
//				// sort test cases in tmp
//				double CI_testcase = ((TestCase) Adequacy.testCases
//						.get(testcase)).CI;
//				if (tmp.size() != 0) {
//					int k = 0;
//					for (; k < tmp.size(); k++) {
//						TestCase temp = (TestCase)Adequacy.testCases.get((String)tmp.get(k));
//						double CI_temp = temp.CI;
//						if (CI_temp < CI_testcase) {// right place to insert CI
//							tmp.add(k, testcase);
//							break;
//						}
//					}
//					if(k == tmp.size()){ //add it to the tail of sequences
//						tmp.add(testcase);
//					}
//				} else {
//					tmp.add(testcase);
//				}
//			}
//			if(trial > 20){
//				System.out.println("Trail(TestSet):" + trial + "\ttmp.size():" + tmp.size() + "\tsize:" + this.size());
//			}
//		} while (tmp.size() < 10);
//		
//		//3. use nearest neighbors to determine which test case in tmp should be returned
//		ArrayList minDistances = new ArrayList();
//		for(int i = 0; i < tmp.size(); i ++){
//			TestCase tc = (TestCase)Adequacy.testCases.get((String)tmp.get(i));
//			double minDis = Double.MAX_VALUE;
//			for(int j = 0; j < CI_testcases.size(); j ++){
//				double distance = Math.abs(tc.CI-(Double)CI_testcases.get(j));
//				if(minDis > distance)
//					minDis = distance;
//			}
//			minDistances.add(minDis);
//		}
//		
//		
//		double maxDis = Double.MIN_VALUE;
//		int index = 0;
//		
//		for(int i = 0; i < minDistances.size(); i ++){
//			double dis = (Double)minDistances.get(i);
//			if(maxDis < dis){
//				maxDis = dis;
//				index = i;
//			}
//		}
//		
//		return (String)tmp.get(index);
//		
//	}
	
	/**return the test case who has the ith-largest CI value 
	 * 
	 * @param i
	 * @return
	 */
	public String getByART(int i){
		Vector tmp = new Vector();
		//randomly get a test set S containing 10 test cases
		
		int trial = 0;
		do {
			String testcase = this.getByRandom();
			
//			System.out.println("testcase(TestSet):" + testcase);
			
			trial ++;
			if (!tmp.contains(testcase)) {
				
				// sort test cases in tmp in descending orders
				double CI_testcase = ((TestCase) Adequacy.testCases
						.get(testcase)).CI;
				if (tmp.size() != 0) {
					int k = 0;
					for (; k < tmp.size(); k++) {
						TestCase temp = (TestCase)Adequacy.testCases.get((String)tmp.get(k));
						double CI_temp = temp.CI;
						if (CI_temp < CI_testcase) {// right place to insert CI
							tmp.add(k, testcase);
							break;
						}
					}
					if(k == tmp.size()){ //add it to the tail of sequences
						tmp.add(testcase);
					}
				} else {
					tmp.add(testcase);
				}
			}
			if(trial > 20){
				System.out.println("Trail(TestSet):" + trial + "\ttmp.size():" + tmp.size() + "\tsize:" + this.size());
			}
		} while (tmp.size() < 10);
		return (String)tmp.get(i);
//		do{
//			String testcase = this.getByRandom();
//			if(!tmp.contains(testcase)){
//				//sort test cases in tmp
//				double CI_testcase = ((TestCase)Adequacy.testCases.get(testcase)).CI;
//				if(tmp.size()!=0){
//					for(int k = 0; k < tmp.size(); k ++){
//						TestCase temp = (TestCase)tmp.get(k);
//						double CI_temp = temp.CI;
//						if(CI_temp < CI_testcase){//right place to insert CI
//							tmp.add(k, (TestCase)Adequacy.testCases.get(testcase));
//							break;
//						}
//					}
//				}else{
//					tmp.add(testcase);
//				}
//			}
//		}while(tmp.size()< 10);
		
//		return ((TestCase)tmp.get(i)).index;
		
	}
	
	//2008-2-26: use ART-like test case generation algorithm to get a random value
	public String getByART(){
		TestSet tmp = new TestSet();
		//randomly get a test set S containing 10 test cases 
		do{
			String testcase = this.getByRandom();
			if(!tmp.contains(testcase))
				tmp.add(testcase);
		}while(tmp.size()< 10);
		
		String testCaseMaxCI = null;
		double maxCI = 0.0;
		for(int i = 0; i < tmp.size(); i ++){
			TestCase temp = (TestCase) Adequacy.testCases.get((String)tmp.get(i));
			double CI_temp = temp.CI;
			if(CI_temp > maxCI){
				maxCI = CI_temp;
				testCaseMaxCI = temp.index;
			}
		}
		return testCaseMaxCI;
	}
	
	/**2009-10-25:used by RA-H/RA-L to sample test cases. Since the size of test pool 
	 *can be reduced, we need to check test set size before sampling
	 * @param H_L
	 * @param size_TestSet
	 * @return
	 */
	public String getByART_best(String H_L, int size_TestSet){
		TestSet tmp = new TestSet();
		
		//2009-10-25: check the size of test pool before sampling 
		if(this.size() > size_TestSet){
			do{
				String testcase = this.getByRandom();
				if(!tmp.contains(testcase))
					tmp.add(testcase);
			}while(tmp.size()< size_TestSet);	
		}else{
			for(int i = 0; i < this.size(); i ++){
				String testcase = this.get(i);
				tmp.add(testcase);
			}
		}
		
		
		
		String testCaseMaxCI = null;
		
		if(H_L.equals("H")){
			double maxCI =Double.MIN_VALUE;
			for(int i = 0; i < tmp.size(); i ++){
				TestCase temp = (TestCase) Adequacy.testCases.get((String)tmp.get(i));
				double CI_temp = temp.CI;
				if(CI_temp > maxCI){
					maxCI = CI_temp;
					testCaseMaxCI = temp.index;
				}
			}	
		}else if(H_L.equals("L")){
			double minCI = Double.MAX_VALUE;
			for(int i = 0; i < tmp.size(); i ++){
				TestCase temp = (TestCase) Adequacy.testCases.get((String)tmp.get(i));
				double CI_temp = temp.CI;
				if(CI_temp < minCI){
					minCI = CI_temp;
					testCaseMaxCI = temp.index;
				}
			}
		}
		
		return testCaseMaxCI;
	}
	
	
	/**2009-09-18:
	 * 
	 * @param H_L: RA_H or RA_L: the former favors high context diversities 
	 * while the latter favors low context diversities
	 * @param size_TestSet: the size of ART-constructed test sets
	 * @return
	 */
	public String getByART(String H_L, int size_TestSet){
		TestSet tmp = new TestSet();
		
		//randomly get a test set S containing 10 test cases 
		do{
			String testcase = this.getByRandom();
			if(!tmp.contains(testcase))
				tmp.add(testcase);
		}while(tmp.size()< size_TestSet);
		
		
		String testCaseMaxCI = null;
		
		if(H_L.equals("H")){
			double maxCI =Double.MIN_VALUE;
			for(int i = 0; i < tmp.size(); i ++){
				TestCase temp = (TestCase) Adequacy.testCases.get((String)tmp.get(i));
				double CI_temp = temp.CI;
				if(CI_temp > maxCI){
					maxCI = CI_temp;
					testCaseMaxCI = temp.index;
				}
			}	
		}else if(H_L.equals("L")){
			double minCI = Double.MAX_VALUE;
			for(int i = 0; i < tmp.size(); i ++){
				TestCase temp = (TestCase) Adequacy.testCases.get((String)tmp.get(i));
				double CI_temp = temp.CI;
				if(CI_temp < minCI){
					minCI = CI_temp;
					testCaseMaxCI = temp.index;
				}
			}
		}
		
		return testCaseMaxCI;
	}
	
	/**2009-10-30: use ART to sample test cases 
	 * with the largest activation
	 * 
	 * @param H_L
	 * @param size_TestSet
	 * @return
	 */
	public String getByART_activation(String H_L, int size_TestSet){
		TestSet tmp = new TestSet();
		//randomly get a test set S containing 10 test cases 
		do{
			String testcase = this.getByRandom();
			if(!tmp.contains(testcase))
				tmp.add(testcase);
		}while(tmp.size()< size_TestSet);
		
		
		String testCaseMaxActivation = null;
		
		if(H_L.equals("H")){
			int maxActivation =Integer.MIN_VALUE;
			for(int i = 0; i < tmp.size(); i ++){
				TestCase temp = (TestCase) Adequacy.testCases.get((String)tmp.get(i));
				int activation_temp = temp.activation;
				if(activation_temp > maxActivation){
					maxActivation = activation_temp;
					testCaseMaxActivation = temp.index;
				}
			}	
		}else if(H_L.equals("L")){
			int minActivation = Integer.MAX_VALUE;
			for(int i = 0; i < tmp.size(); i ++){
				TestCase temp = (TestCase) Adequacy.testCases.get((String)tmp.get(i));
				int activation_temp = temp.activation;
				if(activation_temp < minActivation){
					minActivation = activation_temp;
					testCaseMaxActivation = temp.index;
				}
			}
		}
		
		return testCaseMaxActivation;
	}
	
	
	public String getByRandom(double min_CI, double max_CI, int maxTrial){
		String randomTC = null;
		double CI;
		int count = 0;
		if(!isEmpty()){
			do{
				count ++;
				randomTC = get((int)(Math.random()*(double)size()));
				CI = ((TestCase)Adequacy.testCases.get(randomTC)).CI;
			}while((CI >= max_CI || CI < min_CI) && count < maxTrial);
		}
		
		return randomTC;
	}
	
	
	//2009-02-17: generate a test case whose CI from min_CI to max_CI
	public String getByRandom(double min_CI, double max_CI){
		String randomTC = null;
		double CI; 
		if(!isEmpty()){
			do{
				randomTC = get((int)(Math.random()*(double)size()));
				CI = ((TestCase)Adequacy.testCases.get(randomTC)).CI;
			}while(CI >= max_CI || CI < min_CI);
		}
		return randomTC;
	}
	
	
	//2009-02-17: generate a test case whose CI are not in min_CI to max_CI
	public String getByRandomExclude(double min_CI, double max_CI){
		String randomTC = null;
		double CI; 
		if(!isEmpty()){
			do{
				randomTC = get((int)(Math.random()*(double)size()));
				CI = ((TestCase)Adequacy.testCases.get(randomTC)).CI;
			}while(CI < max_CI && CI > min_CI);
		}
		return randomTC;
	}
	
	public boolean isEmpty() {
		
		return testcases.isEmpty();
	}
	
	public void remove(int index){
		testcases.remove(index);
	}
	
	public void remove(String testcase) {
		
		testcases.remove(testcase);
	}
	
	public void remove(TestSet set) {
		
		for (int i = 0; i < testcases.size(); i++) {
			remove(set.get(i));
		}
	}
	
	public void setCoverage(double c) {
		
		coverage = c;
	}
	
	public double getCoverage() {
		
		return coverage;
	}
	
	public String displayCoverage() {
		
		String result = Double.toString(coverage);
		int i = result.indexOf(".");
		if (i != -1 && result.length() - i > 6) {
			result = result.substring(0, i + 6);
		}
		return result;
	}
	
	public int size() {
		
		return testcases.size();
	}
	
	public String toString() {
		
//		return SIZE_TAG + size() + "\t" + COVERAGE_TAG + displayCoverage() + "\t" + "Time:" + String.valueOf(geneTime)
//				+ "\t" + testcases.toString();
		
		return "\t" + size() + "\t" + COVERAGE_TAG + displayCoverage() + "\t" + "Time:" + String.valueOf(geneTime)
		+ "\t" + testcases.toString() +"\t"+ "replaceCounter:" + "\t" + replaceCounter + "\t" + "Tie_activation_CI:" + this.tie_activation_CI + "\t";

	}
	
	public static void main(String argv[]) {
		
//		int testPoolStartLabel = TestDriver.TEST_POOL_START_LABEL;
//		int testPoolSize = TestDriver.TEST_POOL_SIZE;
//		String versionPackageName = argv[0];
//		if (argv.length == 3) {
//			testPoolStartLabel = Integer.parseInt(argv[1]);
//			testPoolSize = Integer.parseInt(argv[2]);
//		}
//		TestSet testpool = Adequacy.getTestPool(testPoolStartLabel, testPoolSize);
//		long startTime = System.currentTimeMillis();
//		TestDriver.getFailureRate(versionPackageName, "TestCFG2", testpool, 
//				"experiment/failure-rate-report-" + versionPackageName + ".txt");
//		System.out.println(System.currentTimeMillis() - startTime);
		
	//	System.out.println(testpool);
	//	TestDriver.getFailureRate(TestDriver.VERSION_PACKAGE_NAME, "TestCFG2", testpool, 
	//			"experiment/failure-rate-report.txt");
		
	//	long startTime = System.currentTimeMillis();
	//	TestDriver.getFailureRate("trialversion", "TestCFG2", testpool, 
	//			"experiment/trial-failure-rate-report.txt");
	//	System.out.println(System.currentTimeMillis() - startTime);
		
		//2009-2-15:
		String s = "1	49	Coverage:	0.8135	Time:105797	[-4689, -4658, -3345, 798, 3518, 3988, -2659, -442, -2434, -4749, 1237, 309, 4293, 3476, 2077, -3387, -4367, 4454, 893, 643, -115, -571, 3719, 503, 993, -3104, 3715, 3042, -4965, 4820, 4230, 2001, 1337, 646, 4210, 2604, 738, 2846, 1928, -1883, -3476, 82, -889, -3500, 2857, 468, -3429, -2905, -2502]";
		TestSet ts = new TestSet(s);
		System.out.println("a");
	}

}
