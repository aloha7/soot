package edu.cs.hku.testGeneration;

import static edu.cs.hku.testGeneration.Assertion.notNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class MethodCall<T> extends AbstractExpression<T>{
	protected Expression<?>[] paramPlans = null;
	protected Expression<?> receiverPlan = null;
	
	protected Method meth = null;
	
	
	// for static method
	public MethodCall(Class<?> testeeType, Method pMeth, 
			Expression<?>[] pConstrParams){
		super((Class<T>)pMeth.getReturnType(), testeeType);
		initBase(pMeth, pConstrParams);
		
		if(!Modifier.isStatic(meth.getModifiers())){
			throw new IllegalArgumentException(
					"Not a static method");
		}
	}
	
	//for instance method
	public MethodCall(Class<?> testeeType, Method pMeth, 
			Expression<?>[] pConstrParams, Expression<?> pReceiverPlan){
		super((Class<T>)pMeth.getReturnType(), testeeType);
		initBase(pMeth, pConstrParams);
		this.receiverPlan = pReceiverPlan;
		
		if(Modifier.isStatic(pMeth.getModifiers())){
			throw new IllegalArgumentException("Not an instance method");
		}
		
		Class<?> receiverType = pMeth.getDeclaringClass();
		if(!receiverType.isAssignableFrom(this.receiverPlan.getReturnType())){
			throw new IllegalArgumentException(
					"Receiver instance creation plan is not compatiable with" + 
					"method signature.");
		}
	}
	
	protected void initBase(Method pMeth, Expression<?>[] pConstrParams){
		this.meth = notNull(pMeth);
		this.paramPlans = notNull(pConstrParams);
		
		if(meth.getParameterTypes().length != pConstrParams.length ){
			throw new IllegalArgumentException("wrong number of arguments");
		}
	}
	
	public T execute() throws InstantiationException, 
	IllegalAccessException, InvocationTargetException{
		Object receiver = null;
		//instance method needs a receiver
		if(Modifier.isStatic(this.meth.getModifiers()) == false){
			receiver = receiverPlan.execute();
		}
		
		final Object[] args =  new Object[paramPlans.length];
		for(int i = 0; i < args.length; i ++){
			args[i] = paramPlans[i].execute();
		}
		
		meth.setAccessible(true); // call non-public methods
		return (T)meth.invoke(receiver, args);
	}
	
	public String text(){
		StringBuilder res = new StringBuilder();
		
		final String className = CodeGenFct.getName(meth.getDeclaringClass(), testeeType);
		
		if(Modifier.isStatic(meth.getModifiers())){
			//ClassName.staticMeth(
			res.append(className + "." + meth.getName() + "(");
		}else{
			// (new Receiver()).instanceMeth(
			if(receiverPlan instanceof ConstructorCall){
				res.append("(" + receiverPlan.text() + ")." + meth.getName() + "(");
			}else{
				//A.b().conMeth(
				res.append(receiverPlan.text() + "." + meth.getName() + "(");
				
			}
		}
		
		//Parameter tail: recurse
		for(int i = 0; i < paramPlans.length; i ++){
			if(i > 0){
				res.append(", ");
			}
			res.append(paramPlans[i].text()); //value
		}
		res.append("");
		
		return res.toString();
	}
	
	
	
}
