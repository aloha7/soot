package context.test;

import java.util.Random;
import java.util.Vector;

import context.arch.generator.PositionIButton;
import context.arch.widget.WTourDemo;
import context.arch.widget.WTourEnd;
import context.arch.widget.WTourRegistration;
import context.test.contextIntensity.Manipulator;
import context.test.util.Constant;
import context.test.util.Logger;

public class TestManagers {

	public Vector testPool;
	public Vector testSets;
	public int SIZE_TESTPOOL = 100;
	public int NUMBER_TESTSET = 100;
	public int MAX_LENGTH_TESTCASE = 10;
	public int MIN_LENGTH_TESTCASE = 4;
	
	
	public void generateAllTestSets(String criteriaFile){
		this.testSets = new Vector();
		do{
			Vector testSet = this.generateAdequateTestSet(criteriaFile);
			if(!testSets.contains(testSet)){
				testSets.add(testSet);
			}
		}while(testSets.size()<NUMBER_TESTSET);
	}
	
	public Vector generateAdequateTestSet(String criteriaFile){
		//2009/1/17:

		Vector testSet = new Vector();
		
		//1.load all drivers for a specified criteria
		Manipulator manager = Manipulator.getInstance(criteriaFile);
		Vector drivers = manager.getAllUncoveredDrivers();
		Vector leftDrivers;
		
		do{
			//2. random select a test case
			String testCase = this.getTestCase(testSet);
			//3.feed test case to SUT: faulty version index(must be golden version 0) + test case index		
//			PositionIButton program = new PositionIButton("0", "0");
			
			//4.judge whether this test case increases the coverage
			leftDrivers = manager.getAllUncoveredDrivers();
			if(leftDrivers.size() < drivers.size()){
				testSet.add(testCase);
			}
			
			//5.re-initial the Manipulator 
			drivers = leftDrivers;
			manager.setDrivers(drivers);
		}while(leftDrivers.size() > 0);//6.feed more test cases if there are some uncovered drivers

		return testSet;
	}
	
	public Vector generateTestPool(Vector events){
		testPool = new Vector();
		while(testPool.size() < SIZE_TESTPOOL){
			//generate a test case
			String testCase = this.generateTestCase(events);  
			//add it to the test pool if not duplicated
			if(!testPool.contains(testCase)){
				testPool.add(testCase);
			}
		}
		return testPool;
	}
	
	
	public String generateTestCase(Vector events){
		StringBuilder sb = new StringBuilder();
		Random rand = new Random();
		int length_TS;
		
		//length of test cases:[MIN_LENGTH_TESTCASE, MAX_LENGTH_TESTCASE]
		do{
		 length_TS = rand.nextInt(this.MAX_LENGTH_TESTCASE); 
		}while(length_TS < MIN_LENGTH_TESTCASE);
		
		for(int i = 0; i < length_TS - 1; i ++){
			int event_index = rand.nextInt(events.size());
			sb.append((String)events.get(event_index) + "\t");
		}
		sb.append((String)events.get(rand.nextInt(events.size())));
		return sb.toString();
	}
	
	/**
	 * randomly get a test case which is different from any elements in testSet
	 * @param testSet
	 * @return
	 */
	public String getTestCase(Vector testSet){
		String testCase;
		
		do{
			testCase = (String)testPool.get((new Random()).nextInt(testPool.size()));
		}while(testSet.contains(testCase));
		
		return testCase;
	}
	
	public void saveTestArtifacts(String path, Vector data){
		Logger.getInstance().setPath(path, false);
		Logger.getInstance().write(this.toString(data));
		Logger.getInstance().close();
	}
	
	private String toString(Vector data){
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < data.size(); i ++){
			if(data.get(i) instanceof Vector){
				sb.append(this.toString((Vector)data.get(i)));
			}else{
				sb.append(data.get(i) + "\n");
			}
		}
		return sb.toString();
	}
	
	public static void main(String[] args){
		Vector events = new Vector();
		events.add(WTourRegistration.UPDATE);
		events.add(WTourDemo.INTEREST);
		events.add(WTourDemo.VISIT);
		events.add(WTourEnd.END);
		
		Vector test = new Vector();
		test.add("a");
		test.add("b");
		test.add("c");
		test.add("d");
//		System.out.println(test.contains("c"));
		
		Vector test1 = new Vector();
		test1.add("c");
		test1.add("d");
		test1.add("e");
		test1.add("f");
		
		Vector all = new Vector();
		all.add(test);
		all.add(test1);
//		System.out.println(all.contains(test1));
		
		TestManagers manager = new TestManagers();
		manager.generateTestPool(events);
		manager.saveTestArtifacts(Constant.baseFolder + "ContextIntensity/TestPool.txt", manager.testPool);
//		String str_testPool = manager.toString(manager.testPool);
//		String str_testPool = manager.toString(all);
		
//		System.out.println(str_testPool);
	}
}
