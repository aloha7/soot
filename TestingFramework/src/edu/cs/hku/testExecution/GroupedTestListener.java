package edu.cs.hku.testExecution;

import junit.framework.Test;
import junit.framework.TestListener;

public interface GroupedTestListener extends TestListener{
	
	public void addError(Test test, Throwable t, GroupedTestFailure parent);
}
