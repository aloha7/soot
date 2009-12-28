package edu.cs.hku.testGeneration;

import static edu.cs.hku.testGeneration.Assertion.notNull;

import java.lang.reflect.InvocationTargetException;


public class ExpressionStatement<T> extends Block<T>{
	protected Expression<T> fctPlan = null;
	
	public ExpressionStatement(final Expression<T> pFctPlan){
		fctPlan = notNull(pFctPlan);
	}
	
	public T execute() throws InstantiationException, IllegalAccessException,
	InvocationTargetException{
		return fctPlan.execute();
	}
	
	public String text(){
		return fctPlan.toString() +";";
	}
	
	public String toString(){
		return text();
	}
	
}
