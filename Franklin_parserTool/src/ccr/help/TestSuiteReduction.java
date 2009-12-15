package ccr.help;

import java.util.Vector;

import ccr.app.TestCFG2;
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
	public Vector getProgramEles(Criterion criterion, String filename){
		Vector programEles = new Vector();
		
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
				sb.append(def.index).append(":").
				append(uses.get(j)).append("\t").append(index).append("\n") ;		
				index ++;
				programEles.add(def.index + ":" + uses.get(j));
			}
		}	
		
		//2.label the nodes
		for(int i = 0; i < nodes.size(); i++){
			sb.append(nodes.get(i).index).append("\t").append(index).append("\n");
			index ++;
			programEles.add(nodes.get(i).index);
		}
		
		//3.label the policies
		for(int i = 0; i < policies.size(); i++){
			sb.append(((Policy)policies.get(i)).index).append("\t").append(index).append("\n");
			index ++;
			programEles.add(((Policy)policies.get(i)).index);
		}
		
		Logger.getInstance().setPath(filename, false);
		Logger.getInstance().write(sb.toString());
		Logger.getInstance().close();
		
		return programEles;
	}
	
	public Vector getStatisticsOfTestPool(String appClassName, String date, String criterion){
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
		
		//2.construct the program elements specified by a criterion
		String filename = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
			+ date + "/" + "labelResult.txt";	
		Vector programEles = this.getProgramEles(c, filename);
		
		//3.load all test cases in the test pool
		String testPoolFile = "src/ccr/experiment/Context-Intensity_backup/" +
				"TestHarness/TestPool.txt";
		TestSet testpool = Adequacy.getTestPool(testPoolFile, true);
		
		//4.get the statistics of all test cases in the test pool
		
		boolean writeHeader = true;
		Vector<TestCase> statistics_TestCase = new Vector<TestCase>(); 
		for(int i = 0; i < testpool.size(); i++){
			String index = testpool.get(i);
			System.out.println("Process test case:" + index);
			TestCase t = this.getStaticsOfTestCase(appClassName, index, c, programEles);
			statistics_TestCase.add(t);
			
			//save the data in bundles 
			if(i % 1000 == 0){
				//5.save and return the statistics of all test cases
				filename = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
					+ date + "/" + "TestCaseStatistics_" + criterion + ".txt";
				
				this.saveTestCases(statistics_TestCase, programEles, filename, writeHeader);
				statistics_TestCase.clear();
				
				if(writeHeader){
					writeHeader = false;
				}
			}
		}
		
				
		return statistics_TestCase;
	}

	/**2009-12-14: get the statistics info of test cases:
	 * index, context stream length, context diversity, 
	 * activation, and the execution time/traces/coverage
	 * with respect to a specific testing criterion
	 * 
	 * @param appClassName
	 * @param index
	 * @return
	 */
	public TestCase getStaticsOfTestCase(String appClassName, String index, Criterion c, Vector programEles){
		TestCase t = new TestCase();
		t.index = index;
		
		TestCFG2 ins = new TestCFG2();
		
		long startTime = System.currentTimeMillis();
		Object output = ins.application(t.index);
		t.execTime = System.currentTimeMillis() - startTime;
		t.output = output;
		
		t.length = "" + ins.PositionQueue.size();		
		t.CI = ins.PositionQueue.size() - ins.getChanges(ins.PositionQueue);
		t.activation = ins.activation;
		
		t.execTrace = TestDriver.getTrace(appClassName, t.index);
		
		t.coverage = TestSetManager.countDUCoverage(t.execTrace, c);		 
		Vector hitSet = new Vector(programEles.size());
		
		int hitCounter = 0; //count how many elements has been hit
		for(int i = 0; i < programEles.size(); i++){
			//check the coverage of each program elements one by one
			if(t.coverage.containsKey(programEles.get(i))){
				hitSet.add((Integer)t.coverage.get(programEles.get(i)));
				hitCounter ++;
			}else{
				hitSet.add(0);
			}
		}		
		t.hitSet = hitSet;
		t.hitCounter = hitCounter;
		
		return t;
	}
	
	public void saveTestCases(Vector testCases, Vector programEles, String filename, boolean writeHeader){
		StringBuilder sb = new StringBuilder();
		
		//Header
		if(writeHeader){
			sb.append("\t");
			sb.append("CI").append("\t").append("Activation").append("\t").
			append("Length").append("\t").append("Time").append("\t").
			append("HitCounter").append("\t");
			for(int i = 0; i < programEles.size(); i++){
				sb.append(programEles.get(i)).append("\t");
			}
			sb.append("\n");	
		}
		
		//TestCases
		for(int i = 0; i < testCases.size(); i++){
			TestCase t = (TestCase)testCases.get(i);
			sb.append(t.index).append("\t");
			sb.append(t.CI).append("\t").append(t.activation).append("\t").
				append(t.length).append("\t").append(t.execTime).append("\t").
				append(t.hitCounter).append("\t");
			for(int j = 0; j < t.hitSet.size(); j++){
				sb.append(t.hitSet.get(j)).append("\t");
			}
			sb.append("\n");
		}
		
		Logger.getInstance().setPath(filename, true); // write the data in bundles
		Logger.getInstance().write(sb.toString());
		Logger.getInstance().close();
	}
	
	
	public static void main(String[] args) {
		if(args.length == 0){
			System.out.println("Please specify the date to save the results");
		}else{
			String date = args[0];
			String criterion = args[1];
			
			TestSuiteReduction ins = new TestSuiteReduction();
			String appClassName = "TestCFG2_ins";			
			ins.getStatisticsOfTestPool(appClassName, date, criterion);
		}
		
	}

}
