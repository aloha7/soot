package edu.cs.hku.testGeneration;

import static edu.cs.hku.testGeneration.Assertion.notNull;


import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

public class Generator<T> {
	Hashtable<Class<T>, ClassUnderTest<T>> plans = new Hashtable<Class<T>, ClassUnderTest<T>>();
	
	public ClassUnderTest<T> getPlanSpace(Class<T> pClass){
		if(plans.contains(pClass)){
			return plans.get(pClass);
		}else{
			final ClassUnderTest<T> classNode = new ClassUnderTest<T>() ; 
			
			plans.put(pClass, classNode);
			return  classNode;
		}
			
		
		
	}
	
	public <T> List<String> getBlocks(final Class<T> classUnderTest){
		notNull(classUnderTest);
		
		List<String> res = new ArrayList<String>(); 
		
		
		
		return res;
	}
}
