package edu.cs.hku.testSet;

import edu.cs.hku.instrument.InstrumentMain;
import edu.cs.hku.instrument.Probe_block;
import edu.cs.hku.testCriteria.CheckCov;
import edu.cs.hku.testCriteria.DUExtractor;
import edu.cs.hku.testCriteria.DUPairs;
import edu.cs.hku.util.Constants;
import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;

public class TestSetMain {
	protected final static String USAGE = "java edu.cs.hku.test.TestSetMain [option] java_to_analyze\n"
			+ "option:\n"
			+ "-c      --classpath=String     classpath to load java files and related library\n"
			+ "-o      --outputDir=String     directory names to save du-pairs\n"
			+ "-h      --help                 print this help message\n\n"
			+ "Example: java edu.cs.hku.testCriteria.DUExtractor -c c:\\Soot\\bin -O c:\\temp trivia.TestClass\n";
	protected final static String copyright = "(C) copyright 2007-2011 Wang Huai";
	protected final static String hint = "Try 'java edu.cs.hku.test.TestSetMain -h' for more information";

	String classpath = null;
	String savePath = null;
	String classes = null;

	public void parse(String[] args) {
		LongOpt[] opts = new LongOpt[] {
				new LongOpt("classpath", LongOpt.REQUIRED_ARGUMENT, null, 'c'),
				new LongOpt("outputFile", LongOpt.REQUIRED_ARGUMENT, null, 'o'),
				new LongOpt("help", LongOpt.NO_ARGUMENT, null, 'h') };
		Getopt g = new Getopt("TestMain", args, "c:o:h;", opts);
		
		int opt = 0;

		while ((opt = g.getopt()) != -1) {
			switch (opt) {
			case 'c':
				classpath = g.getOptarg();
				break;
			case 'o':
				savePath = g.getOptarg();
				break;
			case 'h':
				System.out.println(USAGE);
				System.exit(0);
			default:
				System.out.println(hint);
				System.exit(0);
			}
		}

		if (args.length < g.getOptind()) {
			System.out.println("no java files are specified!");
		} else {
			classes = args[g.getOptind()];
		}
		this.getClassTestSets(args);
	}

	/**Get data-flow adequate test sets for a set of classes 
	 * 
	 * @param args: the class names to generate test sets for
	 * @return
	 */
	private HashMap<String, HashSet<TestSet>> getClassTestSets(String[] args){

		// 1. extract the DUPairs from the source file before it is instrumented 
		DUExtractor duExtractor = new DUExtractor();
		HashMap<String, HashMap<Integer, HashSet<Integer>>> clz_dus = duExtractor
				.parse(args);
		System.out.println("\nFinish to extract DUPairs\n");
		soot.G.reset(); //reset options and loaded classes
//		Scene.v().removeClass(c) //remove class c and load it again		
		
		//2. instrument the CUT(Class under test), and save the results into sootOutput
		String[] classList = classes.split(";");
		
		/*2009-09-05: invoke the instrument component through command-line
		 * 
		String cmdStr = "";
		if(classpath != null){
			cmdStr = "java -cp " + Constants.CLASSPATH +";" + classpath
					+ " edu.cs.hku.instrument.InstrumentMain ";
			cmdStr += "-cp " + ".;" + Constants.CLASSPATH +";" + classpath;
		}else{
			cmdStr = "java -classpath " + Constants.CLASSPATH + " edu.cs.hku.instrument.InstrumentMain ";			
		}
		for (String clz : classList) {
			cmdStr += " " + clz;
		}
		JavaRunner.runCommand(cmdStr);*/
		
		new InstrumentMain().parse(args);	
		System.out.println("\nFinish to instrument\n");
		soot.G.reset(); // this is required to reset all options otherwise the following code cannot run successfully
		
		//3. get criterion-adequate test sets
		HashMap<String, HashSet<TestSet>> clz_testSets = new HashMap<String, HashSet<TestSet>>();
		for(String clazz: classList){
			for(int i = 0; i < Constants.NUM_TESTSET; i ++){
				System.out.println(i);
				DUPairs dupair_clone = new DUPairs(clz_dus.get(clazz)).clone(); 
				TestSet testSet = this.getTestSet(clazz, dupair_clone.duPairs);
				System.out.println("Def-Use pairs:" + clz_dus.get(clazz).size());
				HashSet<TestSet> testSets = clz_testSets.get(clazz);
				if(testSets == null){
					testSets = new HashSet<TestSet>();				
				}
				testSets.add(testSet);	
			}
		}
		return clz_testSets;
	}
	
	/**Get data-flow-adequate test sets for a specified class
	 * 
	 * @param className: the name of class under test
	 * @param duPairs: def-use pairs of class under test
	 * @return
	 */
	public TestSet getTestSet(String className, HashMap<Integer, HashSet<Integer>> duPairs){
		HashSet<TestCase> testCases = new HashSet<TestCase>();
		
		StringBuilder sb = new StringBuilder();		
		HashSet<Object> visitedInputs = new HashSet<Object>();
		try{
			Class clz = Class.forName(className);					
			Method m = clz.getDeclaredMethod("main", new Class[]{String[].class});
			
			int trial_time = 0;
			while(trial_time < Constants.MAX_TRIAL_TIME 
					&& duPairs.size() != 0){				
				trial_time ++;
				
				TestCase tc = TestCase.getRandomTestCase(visitedInputs);
				if(tc != null){
					visitedInputs.add(tc.input);
				}else{
					break;
				}				
				m.invoke(null, new Object[]{new String[]{((Integer)tc.input).toString()}});
				
//				Probe_block.reportBlock();
				
				HashMap<String, HashMap<Integer, Integer>> clz_lineCovers = Probe_block.blockToStmt();						
				HashMap<Integer, HashMap<Integer, Integer>> coverage = CheckCov.modifyCoverage(duPairs, 
						clz_lineCovers.get(className));
				
//				Probe_block.reportStmt();
				
				if(coverage!= null && coverage.size() != 0){
//					CheckCov.report_multiClass(coverage);
					testCases.add(tc);
				}
			}
			sb.append("Trial Time:").append(trial_time).append("\n");
			sb.append("Uncovered du-pairs:").append("\n").append(DUExtractor.toString(duPairs)).append("\n");
			
			sb.append("TestCase in TestSet:").append("\n");		
			for(TestCase tc: testCases){
				sb.append(tc + "\n");
			}
			System.out.println(sb);
		}catch(Exception e){
			e.printStackTrace();
		}
		
		return new TestSet(testCases);
	}
	
	
	

	public static void main(String[] args) {
//		args = new String[] { "-o", "c:\\", "trivia.TestClass", };
		new TestSetMain().parse(args);
	}

}
