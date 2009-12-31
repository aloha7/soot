package ccr.help;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Vector;

import ccr.app.ApplicationResult;
import ccr.app.TestCFG2_CI;
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
public class TestCaseStatistics {
		
	/**2009-12-14: label the program elements with sequential numbers
	 * 
	 * @param criterion:can be "AllPolicies", "All1ResolvedDU" or "All2ResolvedDU"
	 * @param filename:file name to save the labeling results.
	 * @return
	 */
	public Vector saveProgramEles(Criterion criterion, String filename){
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
		else if (criterion.equals("AllStatement"))
			c = g.getAllStmt();
		
		//2.construct the program elements specified by a criterion
		String filename = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
			+ date + "/" + "ElemLabels_" + criterion +".txt";	
		Vector programEles = this.saveProgramEles(c, filename);
		
		//3.load all test cases in the test pool
		String testPoolFile = "src/ccr/experiment/Context-Intensity_backup/" +
				"TestHarness/TestPool.txt";
		TestSet testpool = Adequacy.getTestPool(testPoolFile, true);
		
		//4.get the statistics of all test cases in the test pool		
		Vector<TestCase> statistics_TestCase = new Vector<TestCase>(); 
		for(int i = 0; i < testpool.size(); i++){
			String index = testpool.get(i);
			System.out.println("Process test case:" + index);
			TestCase t = this.getStaticsOfTestCase(appClassName, index, c, programEles);
			statistics_TestCase.add(t);
		}
		
		
		//5.save and return the statistics of all test cases
		filename = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
			+ date + "/" + "TestCaseStatistics_" + criterion + ".txt";
		boolean writeHeader = true;
		this.saveTestCases(statistics_TestCase, programEles, filename, writeHeader);
		statistics_TestCase.clear();
				
		return statistics_TestCase;
	}

//	/**2009-12-30: get CI/activation, executionTime/output, 
//	 * hitCounter with respect to the statement coverage
//	 * 
//	 * @param appClassName
//	 * @param index
//	 * @return
//	 */
//	public TestCase getStatisticsOfTestCase(String appClassName, String index){
//		TestCase t = new TestCase();
//		t.index = index;
//		
//		TestCFG2_CI ins = new TestCFG2_CI();
//		
//		long startTime = System.currentTimeMillis();
//		Object output = ins.application(t.index);
//		t.execTime = System.currentTimeMillis() - startTime;
//		t.output = output;
//		
//		t.length = "" + ins.PositionQueue.size();
//		t.CI = ins.PositionQueue.size() - ins.getChanges(ins.PositionQueue);
//		t.activation = ins.activation;
//		t.execTrace = TestDriver.getTrace(appClassName, t.index);
//		
//		
//		return t;
//	}
	
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
		
		TestCFG2_CI ins = new TestCFG2_CI();
		
		long startTime = System.currentTimeMillis();
		Object output = ins.application(t.index);
		t.execTime = System.currentTimeMillis() - startTime;
		t.output = output;
		
		t.length = "" + ins.PositionQueue.size();		
		t.CI = ins.PositionQueue.size() - ins.getChanges(ins.PositionQueue);
		t.activation = ins.activation;
		
		t.execTrace = TestDriver.getTrace(appClassName, t.index);
		
		t.coverFreq = TestSetManager.countDUCoverage(t.execTrace, c);		 
		Vector hitSet = new Vector(programEles.size());
		
//		//2009-12-30: it may miss the PolicyNode takes the form of c33:P0
//		int hitCounter = 0; //count how many elements has been hit
//		for(int i = 0; i < programEles.size(); i++){
//			//check the coverage of each program elements one by one
//			if(t.coverFreq.containsKey(programEles.get(i))){
//				hitSet.add((Integer)t.coverFreq.get(programEles.get(i)));
//				hitCounter ++;
//			}else{
//				hitSet.add(0);
//			}
//		}	
		
		//2009-12-30: for debugging purpose
		int hitCounter = 0;
		int hitSum = 0;
		HashMap hitSet_ele_coverTimes = new HashMap(); 
		String[] elems = (String[])t.coverFreq.keySet().toArray(new String[0]);
		for(int i = 0; i < programEles.size(); i ++){
			String elem = (String)programEles.get(i);
			
			boolean contain = false;
			
			for(int j = 0; j < elems.length; j ++){
				if(elems[j].equals(elem) || (elem.contains("P") && elems[j].contains(elem))){ //elems[j] is a Node like "c72"
					int cover_now = (Integer)t.coverFreq.get(elems[j]);
					
					if(hitSet_ele_coverTimes.containsKey(elem)){// this element has been counted before
						int cover_before = (Integer)hitSet_ele_coverTimes.get(elem);						
						hitSet_ele_coverTimes.put(elem, cover_before + cover_now);
					}else{//this element is the first time to count
						hitSet_ele_coverTimes.put(elem, cover_now);
						hitCounter ++;
					}					
					contain = true;					
				}
			}
			if(!contain){
				hitSet_ele_coverTimes.put(elem, 0);
			}
			int coverTime = (Integer)hitSet_ele_coverTimes.get(elem);
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
	
	public void saveTestCases(Vector testCases, Vector programEles, String filename, boolean writeHeader){
		StringBuilder sb = new StringBuilder();
		
		//Header
		if(writeHeader){
			sb.append("TestCase").append("\t");
			sb.append("CI").append("\t").append("Activation").append("\t").
			append("Length").append("\t").append("Time").append("\t").
			append("HitCounter").append("\t").append("HitSum").append("\t").
			append("Coverage").append("\t").append("Oracle").append("\t");
			for(int i = 0; i < programEles.size(); i++){ //index for hitSet
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
				append(t.hitCounter).append("\t").append(t.hitSum).append("\t").
				append(t.coverage).append("\t");
			sb.append(((ApplicationResult)t.output).toString()).append("\t");
			for(int j = 0; j < t.hitSet.size(); j++){
				sb.append(t.hitSet.get(j)).append("\t");
			}
			sb.append("\n");
		}
		
		Logger.getInstance().setPath(filename, false); // write the data in bundles
		Logger.getInstance().write(sb.toString());
		Logger.getInstance().close();
	}

	
		
	public void saveToDB_TestCaseStatistic(String testCaseFile, boolean containHeader){
		File testpool = new File(testCaseFile);
		if(testpool.exists()){
			StringBuilder sql = new StringBuilder();
			try {
				BufferedReader br = new	BufferedReader(new FileReader(testCaseFile));
				if(containHeader){
					br.readLine();
				}
				
				int counter = 0; 
				String str = null;
				
				sql.append("INSERT INTO testcasedetail (input, oracle, " +
						"contextdiversity, activation, length, time, hitcounter, hitsum, coverage) VALUES ");
				while((str = br.readLine())!= null){
					String[] strs = str.split("\t");
					
					String index = strs[0];
					String CI = strs[1];
					String activation = strs[2];
					String length = strs[3];
					String time = strs[4];
					String hitCounter = strs[5];
					String hitSum = strs[6];
					String coverage = strs[7];
					String oracle = strs[8];
					
					sql.append("(\'").append(index).append("\'").append(",");
					sql.append("\'").append(oracle).append("\'").append(",");
					sql.append("\'").append(CI).append("\'").append(",");
					sql.append("\'").append(activation).append("\'").append(",");
					sql.append("\'").append(length).append("\'").append(",");
					sql.append("\'").append(time).append("\'").append(",");
					sql.append("\'").append(hitCounter).append("\'").append(",");
					sql.append("\'").append(hitSum).append("\'").append(",");				
					sql.append("\'").append(coverage).append("\'").append("),");
					counter ++;
					if(counter% 500 == 0){
						String sqlStmt = sql.substring(0, sql.lastIndexOf(","));
						DatabaseManager.getInstance().update(sqlStmt);
						sql.setLength(0); //clear the sql
						sql.append("INSERT INTO testcasedetail (input, oracle, " +
						"contextdiversity, activation, length, time, hitcounter, hitsum, coverage) VALUES ");
					}
				}
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if(sql.substring(sql.lastIndexOf("VALUES ") + "VALUES ".length()).length() > 0){
				//still have some values need to save
				String sqlStmt = sql.substring(0, sql.lastIndexOf(","));
				DatabaseManager.getInstance().update(sqlStmt);
				sql.setLength(0); //clear the sql
			}
			
			System.out.println("save test case statistics into the Database");
			
		}else{
			System.out.println("the test case file does not exist at all");
		}
		 
	}
	
	
	public static void main(String[] args) {
		if(args.length == 0){
			System.out.println("Please specify the date to save the results");
		}else{
			//1. get the test case statistic and save them into the file
			String date = args[0];
			String criterion = args[1];

			TestCaseStatistics ins = new TestCaseStatistics();
//			String appClassName = "TestCFG2_ins";			
//			ins.getStatisticsOfTestPool(appClassName, date, criterion);
			
			//2. save the test case statistic and save them into the Database
			String testCaseFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
				+ date + "/" + "TestCaseStatistics_" + criterion + ".txt";
			boolean containHeader = true;			
			ins.saveToDB_TestCaseStatistic(testCaseFile, containHeader);
			
		}
		
	}

}