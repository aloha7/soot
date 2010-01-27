package ccr.reduction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import ccr.app.ApplicationResult;
import ccr.app.TestCFG2_CI;
import ccr.help.MutantStatistics;
import ccr.help.TestCaseStatistics;
import ccr.help.TestSetStatistics;
import ccr.stat.CFG;
import ccr.stat.Criterion;
import ccr.stat.Node;
import ccr.stat.NodeMap;
import ccr.stat.NodeSet;
import ccr.stat.Policy;
import ccr.test.Adequacy;
import ccr.test.Logger;
import ccr.test.TestCase;
import ccr.test.TestDriver;
import ccr.test.TestSet;
import ccr.test.TestSetManager;

/**2009-12-14: The main driver for test suite reduction problem of
 * pervasive software. The objective function is to minimize the 
 * test suite size while maximize the context diversity. The constraint
 * is that each program elements specified by a testing criterion must
 * be covered at least once. 
 * 
 * @author hwang
 *
 */
public class TestSuiteReduction {

	/**2009-12-14: label the program elements with sequential numbers
	 * 
	 * @param criterion:can be "AllPolicies", "All1ResolvedDU" or "All2ResolvedDU"
	 * @param filename:file name to save the labeling results.
	 * @return
	 */
	public static ArrayList saveProgramEles(Criterion criterion, String filename){
		ArrayList programEles = new ArrayList();
		
		NodeMap dus = criterion.DUMap;
		NodeSet nodes = criterion.nodes;
		Vector policies = criterion.policies;
		
		StringBuilder sb = new StringBuilder();
		int index = 1;
		//1.label the DU pairs
		for(int i = 0; i < dus.keySize(); i ++){
			Node def = dus.getKey(i);
			NodeSet uses = dus.getValue(i);
			for(int j = 0; j < uses.size(); j ++){
				String du_pair =def.index + ":" + uses.get(j);
				if(!programEles.contains(du_pair)){
					sb.append(def.index).append(":").
					append(uses.get(j)).append("\t").append(index).append("\n") ;		
					index ++;
					programEles.add(du_pair);	
				}
			}
		}	
		
		//2.label the nodes
		for(int i = 0; i < nodes.size(); i++){
			String node = nodes.get(i).index;
			if(!programEles.contains(node)){
				sb.append(nodes.get(i).index).append("\t").append(index).append("\n");
				index ++;
				programEles.add(node);	
			}
		}
		
		//3.label the policies
		for(int i = 0; i < policies.size(); i++){
			String policy = ((Policy)policies.get(i)).index;
			if(!programEles.contains(policy)){
				sb.append(((Policy)policies.get(i)).index).append("\t").append(index).append("\n");
				index ++;
				programEles.add(policy);	
			}
		}
		
		Logger.getInstance().setPath(filename, false);
		Logger.getInstance().write(sb.toString());
		Logger.getInstance().close();
		
		return programEles;
	}
	
	public static ArrayList getStatisticsOfTestPool(String appClassName, String date, String criterion){
		//1.get the testing criterion
		CFG g = new CFG(System.getProperty("user.dir")
				+ "/src/ccr/app/TestCFG2.java");
		Criterion c = null;
		if (criterion.equals("AllPolicies"))
			c = g.getAllPolicies();
		else if (criterion.equals("All1ResolvedDU"))
			c = g.getAllKResolvedDU(1);
		else if (criterion.equals("All2ResolvedDU"))
			c = g.getAllKResolvedDU(2);
		else if (criterion.equals("AllStatement"))
			c = g.getAllStmt();
		
		//2.construct the program elements specified by a criterion
		String filename = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
			+ date + "/ILPModel/" + criterion + "/ElemLabels_" + criterion +".txt";	
		ArrayList programEles = saveProgramEles(c, filename);
		
		//3.load all test cases in the test pool
		String testPoolFile = "src/ccr/experiment/Context-Intensity_backup/" +
				"TestHarness/" + date +"/TestPool.txt";
		boolean containHeader = true;
		ArrayList<TestCase> testpool = TestCaseStatistics.getTestPool(testPoolFile, containHeader);
		
		
		long start = System.currentTimeMillis();
		//4.get the statistics of all test cases in the test pool		
		ArrayList<TestCase> statistics_TestCase = new ArrayList<TestCase>(); 
		for(int i = 0; i < testpool.size(); i++){
			TestCase tc = testpool.get(i);
			System.out.println("Process test case:" + tc.index);
			TestCase t = getStaticsOfTestCase(appClassName, tc, c, programEles);
			statistics_TestCase.add(t);
		}
		long duration = (System.currentTimeMillis() - start)/(1000*60);
		System.out.println("It takes " + duration + " mins to process "+ testpool.size() + " test cases"); 
		
		//5.save and return the statistics of all test cases
		filename = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
			+ date + "/ILPModel/" + criterion + "/TestCaseStatistics_" + criterion + ".txt";
		boolean writeHeader = true;
		saveTestCases(statistics_TestCase, programEles, filename, writeHeader);
		statistics_TestCase.clear();
				
		return statistics_TestCase;
	}

	/**2010-01-27: get the coverage statistics info of test cases:
	 * the execution traces/coverage/hitSum/hitCounter with respect
	 * to a specific testing criterion 
	 * @param appClassName
	 * @param t
	 * @param c
	 * @param programEles
	 * @return
	 */
	public static TestCase getStaticsOfTestCase(String appClassName, TestCase t, Criterion c, ArrayList programEles){		
		TestCFG2_CI ins = new TestCFG2_CI();
		
		t.execTrace = TestDriver.getTrace(appClassName, t.index);
		
		//elems -> cover times
		t.coverFreq = TestSetManager.countDUCoverage(t.execTrace, c);		 
		ArrayList hitSet = new ArrayList(programEles.size());
		
		//2009-12-30: need to organize coverage statistics based on programElems 
		int hitCounter = 0;
		int hitSum = 0;
		HashMap elem_coverTimes_hitSet = new HashMap(); 
		String[] elems = (String[])t.coverFreq.keySet().toArray(new String[0]);
		for(int i = 0; i < programEles.size(); i ++){
			String elem = (String)programEles.get(i);
			
			boolean contain = false;
			for(int j = 0; j < elems.length; j ++){
				if(elems[j].equals(elem) || (elem.contains("P") && elems[j].contains(elem))){ //elems[j] is a Node like "c72"
					int cover_now = (Integer)t.coverFreq.get(elems[j]);
					
					if(elem_coverTimes_hitSet.containsKey(elem)){// this element has been counted before
						int cover_before = (Integer)elem_coverTimes_hitSet.get(elem);						
						elem_coverTimes_hitSet.put(elem, cover_before + cover_now);
					}else{//this element is the first time to count
						elem_coverTimes_hitSet.put(elem, cover_now);
						hitCounter ++;
					}					
					contain = true;					
				}
			}
			if(!contain){ //if this elem has never been covered
				elem_coverTimes_hitSet.put(elem, 0);
			}
			int coverTime = (Integer)elem_coverTimes_hitSet.get(elem);
			hitSet.add(coverTime);
			hitSum += coverTime;
		}
		
//		//2009-12-30(testing purpose): validate the hitCounter
		int counter = 0;
		int sum = 0;
		for(int i = 0; i < hitSet.size(); i ++){
			int cover =(Integer)hitSet.get(i); 
			if( cover > 0){
				counter ++;
				sum += cover; 
			}
		}
		if(hitCounter != counter || hitSum != sum){
			System.out.println("bad data");
		}
		
		
		t.hitSet = hitSet;
		t.hitCounter = hitCounter;
		t.hitSum = hitSum;
		t.coverage = (double)t.hitCounter/(double)programEles.size();
		
		return t;
	}
	
	public static void saveTestCases(ArrayList testCases, ArrayList programEles, String filename, boolean writeHeader){
		StringBuilder sb = new StringBuilder();
		
		//Header		
		if(writeHeader){
			sb.append("TestCaseID").append("\t").append("Length").
			append("\t").append("CD").append("\t").
			append("HitCounter").append("\t").append("HitSum").append("\t").
			append("Coverage").append("\t");
			for(int i = 0; i < programEles.size(); i++){ //index for hitSet
				sb.append(programEles.get(i)).append("\t");
			}
			sb.append("\n");	
		}
		
		//TestCases
		for(int i = 0; i < testCases.size(); i++){
			TestCase t = (TestCase)testCases.get(i);
			sb.append(t.index).append("\t").append(t.length).append("\t").
				append(t.CI).append("\t").		
				append(t.hitCounter).append("\t").append(t.hitSum).append("\t").
				append(t.coverage).append("\t");			
			for(int j = 0; j < t.hitSet.size(); j++){
				sb.append(t.hitSet.get(j)).append("\t");
			}
			sb.append("\n");
		}
		
		Logger.getInstance().setPath(filename, false); // write the data in bundles
		Logger.getInstance().write(sb.toString());
		Logger.getInstance().close();
	}
	
	
	public static void main(String[] args) {
		String instruction = args[0];
		if(instruction.equals("getStatisticsOfTestCase")){
			String date = args[1]; //20101220
			String criterion = args[2];			
			String appClassName = "TestCFG2_ins";			
			getStatisticsOfTestPool(appClassName, date, criterion);
		}
	}

}
