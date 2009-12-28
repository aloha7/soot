package edu.cs.hku.testExecution;

import java.util.Enumeration;
import java.util.Vector;

import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.framework.TestResult;

public class GroupedTestResult extends TestResult {
	protected boolean equal20(StackTraceElement[] stack1, StackTraceElement[] stack2){
		if( stack1 == null && stack2==null)
			return true;
		
		if(stack1 == null || stack2 == null)
			return false;
		
		if(stack1.length < 20 || stack2.length < 20)
			return false;
		
		for(int i = 0; i < 20; i ++){
			if(!stack1[i].equals(stack2[i])){
				return false;
			}			
		}
		
		return true;
	}
	
	protected boolean equal(StackTraceElement[] stack1, StackTraceElement[] stack2){
		if((stack1 == null) && (stack2 == null))
			return true;
		
		if((stack1 == null)||(stack2 == null))
			return false;
		
		if(stack1.length != stack2.length)
			return false;
		
		for(int i = 0; i < stack1.length; i ++){
			if(!stack1[i].equals(stack2[i])){
				return false;
			}
		}
		return true;
	}
	
	
	protected GroupedTestFailure getPrototype(Throwable throwable){

		for(GroupedTestFailure prototypeFailure: (Vector<GroupedTestFailure>)fErrors){
			Throwable prototypeThrowable = prototypeFailure.thrownException();
			
			if(prototypeThrowable.getClass().equals(throwable.getClass()))
				continue;
			
			if(prototypeThrowable.getStackTrace() == null){
				//some throwable do not have a stacktrace
				return prototypeFailure;
			}
			
			if(StackOverflowError.class.equals(throwable.getClass()) && equal20(prototypeThrowable.getStackTrace(), throwable.getStackTrace())){
				return prototypeFailure;
			}
			
			if(equal(prototypeThrowable.getStackTrace(), throwable.getStackTrace())){
				return prototypeFailure;
			}
		}
		
		return null; //could not find a prototype exception in failure list
		
	}
	
	public synchronized void addError(Test test, Throwable throwable){
		GroupedTestFailure prototype = this.getPrototype(throwable);
		GroupedTestFailure failure = new GroupedTestFailure(test, throwable, prototype);
		fErrors.addElement(failure);
		for(Enumeration e = fListeners.elements(); e.hasMoreElements();){
			GroupedTestListener temp = (GroupedTestListener)e.nextElement();
			temp.addError(test, throwable, prototype);
		}
	}
	
	public synchronized int prototypeFailureCount(){
		int count = 0;
		for(int i = 0; i < fErrors.size(); i ++){
			if(((GroupedTestFailure)fErrors.elementAt(i)).isPrototype()){
				count += 1;
			}
		}
		return count;
	}
	
	public synchronized void addFailure(Test test, AssertionFailedError t){
		addError(test, t);
	}
}
