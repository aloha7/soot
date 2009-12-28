package edu.cs.hku.testGeneration;

import static edu.cs.hku.testGeneration.Assertion.check;
import static edu.cs.hku.testGeneration.Assertion.notNull;


public class Variable<T> implements Expression<T>{
	protected Class<T> returnType; //type for "value"
	protected Class<?> testeeType;
	protected String identifier;
	protected T value = null;
	
	
	public Variable(Class<T> returnType, 
			Class<?> testeeType, String identifier){
		this.returnType = notNull(returnType);
		this.testeeType = notNull(testeeType);
		this.identifier = notNull(identifier);
		this.value = null;
	}
	
	
	public Class<T> getReturnType(){
		return this.returnType;
	}
	
	
	public void assign(T pValue){
		notNull(returnType);
		if(pValue != null && !returnType.isPrimitive()){
			check(returnType.isAssignableFrom(pValue.getClass()));
		}
		value = pValue;
	}
	
	public String text(){
		return notNull(identifier);
	}
	
	public String textDeclaration(){
		return CodeGenFct.getName(returnType, testeeType) + " " + text();
	}
	
	public T execute(){
		return value;
	}
}
