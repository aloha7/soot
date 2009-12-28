package edu.cs.hku.testGeneration;

import static edu.cs.hku.testGeneration.Assertion.notNull;

public abstract class AbstractExpression<T> implements Expression<T>{
	protected Class<?> returnType;
	protected Class<?> testeeType;
	
//	protected AbstractExpression(){
//		
//	}
	
	public AbstractExpression(Class<?> returnType, Class<?> testeeType){
		this.returnType = notNull(returnType);
		this.testeeType = notNull(testeeType);
	}
	
	public Class<?> getReturnType(){
		return this.returnType;
	}
	
	public String toString(){
		return text();
	}
}
