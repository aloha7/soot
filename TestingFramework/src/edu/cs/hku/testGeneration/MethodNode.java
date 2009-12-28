package edu.cs.hku.testGeneration;

import static edu.cs.hku.testGeneration.Assertion.check;
import static edu.cs.hku.testGeneration.Assertion.notNull;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

public class MethodNode<T> extends ClassUnderTest<T>{
	protected Method meth = null;
	
	public MethodNode(final Method pMeth, int pMaxRecursion){
		notNull(pMeth);
		check(pMaxRecursion >= 1);
		
		meth = pMeth;
		List<ClassUnderTest<?>> depNodes = new ArrayList<ClassUnderTest<?>>();
		
		//first dimension: receiver instance
		if(Modifier.isStatic(pMeth.getModifiers()) == false){
			Class<?> decClass = pMeth.getDeclaringClass();
			ClassWrapper vW = ClassAnalyzer.getInstance().getWrapper(decClass);
			depNodes.add(new ClassUnderTest(vW, pMaxRecursion -1));
		}
		
		//second, ... n-th dimension: add each parameter
		for(Class<?> paramType: pMeth.getParameterTypes()){
			ClassWrapper pW = ClassAnalyzer.getInstance().getWrapper(paramType);
			depNodes.add(new ClassUnderTest(pW, pMaxRecursion -1));
		}
		
		setParams(depNodes.toArray(new ClassUnderTest[depNodes.size()]));
	}
	
	public Method getMeth(){
		return this.meth;
	}
}
