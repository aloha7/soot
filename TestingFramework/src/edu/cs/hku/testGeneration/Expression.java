package edu.cs.hku.testGeneration;

import java.lang.reflect.InvocationTargetException;

public interface Expression<T> {
	
	
	public T execute() throws InstantiationException, 
	IllegalAccessException, InvocationTargetException;
	
	public String text();
	
	public Class<?> getReturnType();
	
}
