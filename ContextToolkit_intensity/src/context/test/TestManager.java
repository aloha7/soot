package context.test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Vector;



import context.arch.generator.PositionIButton;
import context.arch.service.DisplayChoiceService;
import context.arch.service.Services;
import context.arch.storage.AttributeNameValues;
import context.arch.storage.Attributes;
import context.arch.subscriber.Callbacks;
import context.arch.widget.WTourDemo;
import context.arch.widget.WTourEnd;
import context.arch.widget.WTourRegistration;
import context.arch.widget.Widget;
import context.test.util.TestCase;
import context.test.util.Constant;


public class TestManager extends Widget {
	
	public TestManager(String location, int port){
		super(port, location, true);
	}
	
	protected Attributes setAttributes() {
	    return new Attributes();	    
	}

	  
    protected Callbacks setCallbacks() {
	    return new Callbacks();
	}

	  
	protected Services setServices() {
	    Services services = new Services();
	    services.addService(new DisplayChoiceService(this));
	    return services;
	}

	  
	protected AttributeNameValues queryGenerator() {
	    return new AttributeNameValues();
	}
	  
	public void notify(String event, Object data) {
	}
	
	//2008/7/9: generate test cases
	public Vector generateTestCase(){
		Vector vector = new Vector();
		vector.addElement(WTourRegistration.UPDATE);
		vector.addElement(WTourDemo.VISIT);
		vector.addElement(WTourEnd.END);
		TestCaseGenerator generator = new TestCaseGenerator(vector);		
		int testSuiteSize = 10;
		
		Vector testCases = generator.generateTestCases(testSuiteSize);
		generator.storeTestCases(testCases, System.getProperty("user.dir"));
		
		/*
		Vector cases = generator.retrieveTestCases(System.getProperty("user.dir"));
		
		//Test whether the retrieve method is correct or not
		for(int i = 0; i < testCases.size(); i ++){
			TestCase case1 = (TestCase)testCases.get(i);
			if(!case1.isEqual((TestCase)cases.get(i))){
				System.out.println("Different");
				break;
			}
		}
		*/
		return testCases;
	}
	
	
	
	public static void main(String[] args) {
		try{
				
			int testSuiteSize = 100;
			//Senario setting: how many events are involved in?
			Vector vector = new Vector();
			vector.addElement(WTourRegistration.UPDATE);
			vector.addElement(WTourDemo.VISIT);
			vector.addElement(WTourDemo.INTEREST);
			vector.addElement(WTourEnd.END);
			
		
			//1. produce and store test cases
			//TestCaseGenerator maker = new TestCaseGenerator(vector);			
			//Vector testSuite = maker.generateTestCases(testSuiteSize);
			//maker.storeTestCases(testSuite, Constant.baseFolder + "TestCase.txt");							
			
			
			//2. run test cases on the golden version to produce oracles
			int minVersion = 56;
			int maxVersion = 207;
			boolean visual = true;
			OutputProducer runner = new OutputProducer();
			for(int versionNum = minVersion; versionNum <= maxVersion; versionNum ++){
				runner.produceOutput(testSuiteSize, versionNum,  true);
			}
			
			
//			runner.produceOutput(testSuiteSize, minVersion, maxVersion, visual);
			
			//2009/1/15: we does not need this at all, since all test cases are executed sequentially
		    //3. seed mutants in source codes and outputs for these faulty versions. 
//			minVersion = 0;
//			maxVersion = 2;
//			MutantMaker mutant = new MutantMaker();			
//			mutant.seedMutant(testSuiteSize, minVersion, maxVersion, visual);

			//4. get the failure rate of these mutants
			StatisticFactory sat = new StatisticFactory();
			sat.getFailureRate(minVersion, maxVersion);
			
		}catch(Exception e){
			System.out.println(e);
		}
		
	}

}
