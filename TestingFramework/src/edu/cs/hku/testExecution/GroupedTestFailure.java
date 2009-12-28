package edu.cs.hku.testExecution;

import java.io.PrintWriter;
import java.io.StringWriter;

import junit.framework.Test;
import junit.framework.TestFailure;

public class GroupedTestFailure extends TestFailure {
	protected GroupedTestFailure parent = null;
	
	public GroupedTestFailure(Test failedTest, Throwable thrownException, GroupedTestFailure parent){
		super(failedTest, thrownException);
		this.parent = parent;
	}
	
	public boolean isPrototype(){
		return (parent == null);
	}
	
	protected GroupedTestFailure getParent(){
		return parent;
	}
	
	public String trace(){
		StringWriter stringWriter = new StringWriter();
		this.thrownException().printStackTrace(new PrintWriter(stringWriter));
		StringBuffer trace = stringWriter.getBuffer();
		
		int pos = trace.lastIndexOf("at sun.reflect.NativeMethodAccessorImpl.invoke0");
		if(pos > 0)
			return trace.substring(0, pos).trim();
		
		return trace.toString();
	}
}
