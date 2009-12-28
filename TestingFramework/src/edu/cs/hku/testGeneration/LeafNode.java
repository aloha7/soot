package edu.cs.hku.testGeneration;

import static edu.cs.hku.testGeneration.Assertion.check;
import static edu.cs.hku.testGeneration.Assertion.isArrayIndex;
import static edu.cs.hku.testGeneration.Assertion.notNull;


import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class LeafNode<T> extends ClassUnderTest<T>{
	protected final List<Expression<T>> plans = 
		new ArrayList<Expression<T>>();
	
	public LeafNode(List<Expression<T>> plans){
		notNull(plans);
		this.plans.addAll(plans);
	}
	
	public BigInteger getPlanSpaceSize(){
		return BigInteger.valueOf(plans.size());
	}
	
	public Expression<T> getPlan(BigInteger planIndex, Class<?> testeeType){
		check(isArrayIndex(planIndex));
		return plans.get(planIndex.intValue());
	}
	
	public String toString(){
		return plans.toString();
	}
}
