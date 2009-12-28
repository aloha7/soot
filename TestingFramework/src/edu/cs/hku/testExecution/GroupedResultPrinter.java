package edu.cs.hku.testExecution;

import java.io.PrintStream;

import junit.framework.Test;
import junit.framework.TestFailure;
import junit.textui.ResultPrinter;

public class GroupedResultPrinter extends ResultPrinter implements GroupedTestListener {
	
	public GroupedResultPrinter(PrintStream writer){
		super(writer);
	}
	
	public void addError(Test test, Throwable t, GroupedTestFailure parent){
		if(parent == null){ //first, prototye
			this.getWriter().print("E");
		}else{
			this.getWriter().print("e");
		}
	}
	
	public void printDefect(TestFailure testFailure, int count){
		GroupedTestFailure gTestFailure = (GroupedTestFailure)testFailure;
		if(gTestFailure.isPrototype()){
			this.printDefectHeader(gTestFailure, count);
			this.printDefectTrace(gTestFailure);
		}else{
			
		}
	}
	
}
