package edu.cs.hku.testGeneration;

import static edu.cs.hku.testGeneration.Assertion.check;
import static edu.cs.hku.testGeneration.Assertion.notNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

public class ConstructorCall<T> extends AbstractExpression<T>{
	protected Expression<?>[] paramPlans;
	protected Expression<?> enclosedBy = null;
	protected Constructor<T> constructor = null;
	
	//Constructor---not an inner class
	public ConstructorCall(Class<?> testeeType, 
			Constructor<T> pConstructor, Expression<?>[] pConstrParams){
		//returnType == pConstructor.getDeclaringClass()
		super(pConstructor.getDeclaringClass(), testeeType);
		initBase(pConstructor, pConstrParams);
		
		if(needsEnclosingInstance()){
			throw new IllegalArgumentException(
					"Parameter implies that an enclosing instance is needed");
		}
		
		if(constructor.getParameterTypes().length != pConstrParams.length){
			throw new IllegalArgumentException(
					"wrong number of arguments");
		}
	}
	
	//Constructor-- an inner class
	public ConstructorCall(Class<?> testeeType, Constructor<T> pConstructor,
			Expression<?>[] pConstrParams, Expression<?> pEnclosing){
		super(pConstructor.getDeclaringClass(), testeeType);
		initBase(pConstructor, pConstrParams);
		enclosedBy = pEnclosing;
		
		if(Modifier.isStatic(returnType.getModifiers())){
			throw new IllegalArgumentException(
					"not for a static class");
		}
		
		Class<?> myOuterClass = returnType.getEnclosingClass();
		if(myOuterClass == null){
			throw new IllegalArgumentException(
					"Constructor is not member of an inner type");
		}
		
		check(needsEnclosingInstance());
		
		Class<?> planOuterClass = enclosedBy.getReturnType();
		if(!myOuterClass.isAssignableFrom(planOuterClass)){
			throw new IllegalArgumentException(
					"Plan for enclosing class is not for our encosing class");
		}
		
		if(constructor.getParameterTypes().length != pConstrParams.length){
			throw new IllegalArgumentException(
					"wrong number of arguments");
		}
	}
	
	public T execute() throws InstantiationException,
	IllegalAccessException, InvocationTargetException{
		final Object[] args = new Object[paramPlans.length];
		for(int i = 0; i < args.length; i ++){
			args[i] = paramPlans[i].execute();
		}
		Object[] allArgs = args; //optional enclosing instance included
		
		//if inner class: add an enclosing instance
		if(needsEnclosingInstance()){
			notNull(enclosedBy);
			allArgs = new Object[args.length + 1];
			allArgs[0] = enclosedBy.execute();
			for(int i = 0; i < args.length; i ++){
				allArgs[i+1] = args[i];
			}
		}
		
		constructor.setAccessible(true);
		return notNull(constructor.newInstance(allArgs));
	}
	
	public String text(){
		notNull(testeeType);
		final StringBuilder res = new StringBuilder();
		
		//Add enclosing instance for inner class
		if(needsEnclosingInstance()){
			notNull(enclosedBy);
			res.append("(" + enclosedBy.text() + ").");
		}
		
		res.append("new ");
		final String className = CodeGenFct.getName(constructor.getDeclaringClass(), testeeType);
		//Enc.Nested -- fully qualified class-name;
		
		if(needsEnclosingInstance()){ //Nested -- member name only
			final String memberName = 
				className.substring(className.lastIndexOf('.') + 1);
			res.append(memberName);
		}else{ //Enc.Nested -- entire simple name
			res.append(className);
		}
		
		//Parameter tail: recurse
		res.append("(");
		for(int i = 0; i < paramPlans.length; i ++){
			if(i >0){
				res.append(", ");
			}
			res.append(paramPlans[i].text());
		}
		res.append(")");
		
		return notNull(res.toString());
	}
	
	protected void initBase(Constructor<T> pConstructor,
			Expression<?>[] pConstrParams){
		this.constructor = notNull(pConstructor);
		this.paramPlans = notNull(pConstrParams);
	}
	
	protected boolean needsEnclosingInstance(){
		Class<?> myOuterClass = returnType.getEnclosingClass();
		boolean isStatic = Modifier.isStatic(returnType.getModifiers());
		return ((myOuterClass!=null) && !isStatic);
	}
	
}
