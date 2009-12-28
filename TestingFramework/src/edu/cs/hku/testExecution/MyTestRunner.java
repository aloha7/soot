package edu.cs.hku.testExecution;

import java.io.PrintStream;

import junit.framework.TestResult;
import junit.textui.ResultPrinter;
import junit.textui.TestRunner;



public class MyTestRunner extends TestRunner{
	public MyTestRunner(){
		super(new GroupedResultPrinter(System.out));
	}
	
	protected GroupedTestResult createTestResult(){
		//2009-08-18: very important to introduce GroupedTestResult to junit
		return new GroupedTestResult();
	}
	
	protected PrintStream getExceptionStream(){
		return System.out;
	}

	protected PrintStream getResultsStream(){
		return System.out;
	}
	
	
	
	public void run(String[] args){
//		System.setSecurityManager(new NoExitSecurityManager());
		GroupedTestResult r = null;
		try {
			r = (GroupedTestResult)this.start(args);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			System.err.println(e.getMessage());
		}
		
		this.getResultsStream().println("Suite Name:" + args[0]);
		if(r!= null){
			this.getResultsStream().println("Exceptions and Errors after filtering (E):" + r.prototypeFailureCount());
			this.getResultsStream().println("Exceptions and Errors total (e): " + r.errorCount());			
		}
	}
	
	public static void main(String[] args){
		new MyTestRunner().run(args);
	}
}
