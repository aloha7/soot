package ccr.help;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
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
	
	/**2010-01-22: load the test pool from the existing file in the 
	 * offline way
	 * @param date
	 * @param containHeader
	 * @param alpha
	 * @return
	 */
	public static HashMap<String, TestCase> getTestPool(String date,
			boolean containHeader, double alpha){
		HashMap<String, TestCase> id_tc = new HashMap<String, TestCase>();
		String testPoolFile =  "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
			+ date + "/TestPool_Alpha/TestPool_"+ new DecimalFormat("0.0000").format(alpha) + ".txt";
		File tmp = new File(testPoolFile);
		if(tmp.exists()){
			try {
				BufferedReader br = new BufferedReader(new FileReader(testPoolFile));
				if(containHeader)
					br.readLine();
				
				String str = null;
				while((str = br.readLine())!= null){
					String[] strs = str.split("\t");
					if(strs.length >= 3){
						String index = strs[0];
						TestCase tc = new TestCase(str);						
						id_tc.put(index, tc);
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
		}else{
			System.out.println("Test pool file does not exist:" + tmp.getAbsolutePath());
		}
		
		return id_tc;
	}
	
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

	public void saveToDB_IDInputMaps(String mapFile, boolean containHeader){		
		File tmp = new File(mapFile);
		if(tmp.exists()){
			StringBuilder sql = new StringBuilder();
			try {
				BufferedReader br = new	BufferedReader(new FileReader(mapFile));
				if(containHeader){
					br.readLine();
				}
				
				String str = null;
				sql.append("INSERT INTO id_input_maps (id, input) VALUES ");
				while((str = br.readLine())!= null){
					String[] strs = str.split("\t");
					
					String id = strs[0];
					String input = strs[1];
					
					sql.append("(\'").append(id).append("\'").append(",");							
					sql.append("\'").append(input).append("\'").append("),");
					if(sql.length() > 768*1024){
						String sqlStmt = sql.substring(0, sql.lastIndexOf(","));
						DatabaseManager.getInstance().update(sqlStmt);
						sql.setLength(0); //clear the sql
						sql.append("INSERT INTO id_input_maps (id, input) VALUES ");
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
			
			System.out.println("save id input mappings into the Database");
			
		}else{
			System.out.println("the id input mapping file does not exist at all");
		}
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
					if(sql.length() > 768*1024){
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
	
	public static void saveToDB_TestCaseKillMutants(String testCaseFile, boolean containHeader){
		File tmp = new File(testCaseFile);
		if(tmp.exists()){
			StringBuilder sql = new StringBuilder();
			try {
				BufferedReader br = new	BufferedReader(new FileReader(testCaseFile));
				if(containHeader){
					br.readLine();
				}
				
				String str = null;
				
				sql.append("INSERT INTO testcasekillmutant (testcase, killmutantnum) VALUES ");
				while((str = br.readLine())!= null){
					String[] strs = str.split("\t");
					
					String index = strs[0];
					String killNum = strs[1];
					sql.append("(\'").append(index).append("\'").append(",");								
					sql.append("\'").append(killNum).append("\'").append("),");
					if(sql.length() > 768*1024){ //when the size > 768KB
						String sqlStmt = sql.substring(0, sql.lastIndexOf(","));
						DatabaseManager.getInstance().update(sqlStmt);
						sql.setLength(0); //clear the sql
						sql.append("INSERT INTO testcasekillmutant (testcase, killmutantnum) VALUES ");
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
			
			System.out.println("save test case kill mutants into the Database");
		}else{
			System.out.println("the test case file:" + testCaseFile +" does not exist at all");
		}
	}
	
	public static void updateDB_Length(String testCaseFile, boolean containHeader){
		File tmp = new File(testCaseFile);
		if(tmp.exists()){
			StringBuilder sql = new StringBuilder();
			try {
				BufferedReader br = new	BufferedReader(new FileReader(testCaseFile));
				if(containHeader){
					br.readLine();
				}
				
				int counter = 0; 
				String str = null;
				
				
				while((str = br.readLine())!= null){
					String[] strs = str.split("\t");
					
					String testcase = ""+(Integer.parseInt(strs[0])-10000);
					System.out.println("Processing test case:" + testcase);
					String Length = strs[2];
					sql.append("update testcasedetail D set D.length = \'").append(Length).append("\'").
					append(" where D.input = \'").append(testcase).append("\';").append("\n");	
					String sqlStmt = sql.toString();
					DatabaseManager.getInstance().update(sqlStmt);
					sql.setLength(0); //clear the sql
					
//					counter ++;
//					if(counter% 2 == 0){
//						String sqlStmt = sql.toString();
//						DatabaseManager.getInstance().update(sqlStmt);
//						sql.setLength(0); //clear the sql					
//					}
				}
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
//			if(sql.substring(sql.lastIndexOf("set")).length() > 0){
//				String sqlStmt = sql.toString();
//				DatabaseManager.getInstance().update(sqlStmt);
//				sql.setLength(0); //clear the sql
//			}
			
			System.out.println("save test case coverage into the Database");
		}else{
			System.out.println("the test case file:" + testCaseFile +" does not exist at all");
		}
	}
	
	public static void saveToDB_RenewTestCase(String testCaseFile, boolean containHeader){
		File tmp = new File(testCaseFile);
		if(tmp.exists()){
			StringBuilder sql = new StringBuilder();
			try {
				BufferedReader br = new	BufferedReader(new FileReader(testCaseFile));
				if(containHeader){
					br.readLine();
				}
				
				String str = null;
				
				sql.append("INSERT INTO testcaserenew (input, length, contextdiversity) VALUES ");
				while((str = br.readLine())!= null){
					String[] strs = str.split("\t");
					
					if(strs.length > 2){
						String input = strs[0];
						System.out.println("Processing test case:" + input);
						int length = (int)Double.parseDouble(strs[1]);
						String contextDiversity = strs[2];
											
						sql.append("(\'").append(input).append("\'").append(",");
						sql.append("\'").append(""+length).append("\'").append(",");
						sql.append("\'").append(contextDiversity).append("\'").append("),");
						
						if(sql.length() > 768*1024){
							String sqlStmt = sql.substring(0, sql.lastIndexOf(","));
							DatabaseManager.getInstance().update(sqlStmt);
							sql.setLength(0); //clear the sql
							sql.append("INSERT INTO testcaserenew (input, length, contextdiversity) VALUES ");
						}
					}
				}
				
//				while((str = br.readLine())!= null){
//					String[] strs = str.split("\t");
//					
//					String input = strs[0];
//					System.out.println("Processing test case:" + input);
//					String length = strs[1];
//					String contextDiversity = strs[2];
//					sql.append("update testcaserenew R set R.length = \'").append(length).append("\'").
//					append(", R.contextdiversity = \'").append(contextDiversity).append("\'").
//					append(" where R.input = \'").append(input).append("\',").append("\n");	
//					
//					
//					counter ++;
//					if(counter% 500 == 0){
//						String sqlStmt = sql.toString();
//						DatabaseManager.getInstance().update(sqlStmt);
//						sql.setLength(0); //clear the sql					
//					}
//				}
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
			
			System.out.println("save renewed test case into the Database");
		}else{
			System.out.println("the test case file:" + testCaseFile +" does not exist at all");
		}
	}
	
	public static void saveToDB_TestCaseCoverage(String testCaseCovFile, boolean containHeader){
		File tmp = new File(testCaseCovFile);
		if(tmp.exists()){
			StringBuilder sql = new StringBuilder();
			try {
				BufferedReader br = new	BufferedReader(new FileReader(testCaseCovFile));
				if(containHeader){
					br.readLine();
				}
				
				String str = null;
				
				sql.append("INSERT INTO testcasecoverage (testcase, hitsum, hitcounter, coverage ) VALUES ");
				while((str = br.readLine())!= null){
					String[] strs = str.split("\t");
					
					String testcase = strs[0];
					String hitsum = strs[1];
					String hitcounter = strs[2];
					String coverage = strs[3];
					
					sql.append("(\'").append(testcase).append("\'").append(",");
					sql.append("\'").append(hitsum).append("\'").append(",");
					sql.append("\'").append(hitcounter).append("\'").append(",");					
					sql.append("\'").append(coverage).append("\'").append("),");
					if(sql.length() > 768 * 1024){ // when the size > 768 KB
						String sqlStmt = sql.substring(0, sql.lastIndexOf(","));
						DatabaseManager.getInstance().update(sqlStmt);
						sql.setLength(0); //clear the sql
						sql.append("INSERT INTO testcasecoverage (testcase, hitsum, hitcounter, coverage ) VALUES ");
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
			
			System.out.println("save test case coverage into the Database");
		}else{
			System.out.println("the test case file:" + testCaseCovFile +" does not exist at all");
		}
	}
	
	public static void main(String[] args) {
		String instruction = args[0];
		if(instruction.equals("saveTestCaseStatistics")){
			String date = args[1];
			String criterion = args[2];

			TestCaseStatistics ins = new TestCaseStatistics();
//			String appClassName = "TestCFG2_ins";			
//			ins.getStatisticsOfTestPool(appClassName, date, criterion);
			
			//2. save the test case statistic and save them into the Database
			String testCaseFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
				+ date + "/" + "TestCaseStatistics_" + criterion + ".txt";
			boolean containHeader = true;			
			ins.saveToDB_TestCaseStatistic(testCaseFile, containHeader);	
		}else if(instruction.equals("saveTestCaseKillMutant")){
			String date = args[1];
			String faultNumber = args[2];
			
			String testCaseFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
				+ date + "/FailureRate_nonEuqivalent/" + "TestCaseDetails_" + faultNumber + ".txt";
			boolean containHeader = true;
			TestCaseStatistics ins = new TestCaseStatistics();
			ins.saveToDB_TestCaseKillMutants(testCaseFile, containHeader);
		}else if(instruction.equals("saveTestCaseCov")){
			String date = args[1];			
			
			String testCaseCovFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
				+ date + "/" + "TestCaseCov.txt";
			boolean containHeader = false;
			TestCaseStatistics ins = new TestCaseStatistics();
			ins.saveToDB_TestCaseCoverage(testCaseCovFile, containHeader);
			
		}else if(instruction.equals("updateLength")){
			String date = args[1];
			String testCaseFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
				+ date + "/" + "Length.txt";
			boolean containHeader = false;
			TestCaseStatistics ins = new TestCaseStatistics();
			ins.updateDB_Length(testCaseFile, containHeader);			
		}else if(instruction.equals("saveRenewedTestCase")){
			String date = args[1];
			String alpha = args[2];
			
//			String tc_min = args[3];
//			String tc_max = args[4];
//			String testCaseFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
//				+ date + "/TestPool_Alpha/TestPool_" + new DecimalFormat("0.0000").format(Double.parseDouble(alpha))
//				+ "_"+ tc_min + "_" + tc_max + ".txt";
			
			String testCaseFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
				+ date + "/TestPool_Alpha/TestPool_" + 
				new DecimalFormat("0.0000").format(Double.parseDouble(alpha)) + ".txt";
			
			boolean containHeader = true;
			TestCaseStatistics ins = new TestCaseStatistics();
			ins.saveToDB_RenewTestCase(testCaseFile, containHeader);			
			
		}else if(instruction.equals("saveIdInputMaps")){
			String date = args[1];
			String mapFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
				+ date + "/TestPool_Alpha/ID_Input.txt";
			boolean containHeader = true;
			TestCaseStatistics ins = new TestCaseStatistics();
			ins.saveToDB_IDInputMaps(mapFile, containHeader);
			
		}
		
	}

}
