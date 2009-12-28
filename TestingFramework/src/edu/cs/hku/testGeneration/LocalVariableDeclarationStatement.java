package edu.cs.hku.testGeneration;

import static edu.cs.hku.testGeneration.Assertion.check;
import static edu.cs.hku.testGeneration.Assertion.notNull;

import java.lang.reflect.InvocationTargetException;


public class LocalVariableDeclarationStatement<V> extends Block<V>{
	protected Variable<V> var = null;
	protected Expression<? extends V> varInitPlan = null;
	
	public LocalVariableDeclarationStatement(Variable<V> pID, Expression<? extends V> plan){
		notNull(pID);
		notNull(plan);
		
		final Class<V> idType =  pID.getReturnType();
		final Class<? extends V> planType = plan.getReturnType();
		check(idType.isAssignableFrom(planType) || idType.isPrimitive());
		var = pID;
		varInitPlan = plan;
	}
	
	public Variable<V> getVariable(){
		return var;
	}
	
	public V execute() throws InstantiationException, 
	IllegalAccessException, InvocationTargetException{
		V value = varInitPlan.execute();
		var.assign(value);
		return value;
	}
	
	//A a = new A(null)
	// B b = a.m(0)
	public String text(){
		return var.textDeclaration() + " = " + varInitPlan.text() + ";";
	}
	
	public String toString(){
		return text();
	}
	
	
}
