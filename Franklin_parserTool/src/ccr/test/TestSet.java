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
		return get((int) (Math.random() * (double) size()));
	}
	
	public boolean isEmpty() {
		
		return testcases.isEmpty();
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
		
		return SIZE_TAG + size() + "\t" + COVERAGE_TAG + displayCoverage() + "\t" + "Time:" + String.valueOf(geneTime)
				+ "\t" + testcases.toString();
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
