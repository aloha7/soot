package edu.cs.hku.testGeneration;

import static edu.cs.hku.testGeneration.Assertion.notNull;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

public class ConstructorNode<T> extends ClassUnderTest<T>{
	protected Constructor<T> con = null;
	
	
	public ConstructorNode(Constructor<T> pCon, int pMaxRecursion){
		notNull(pCon);
		
		con = pCon;
		List<ClassUnderTest<T>> depNodes = new ArrayList<ClassUnderTest<T>>();
		
		Class<?>[] paramsTypes = con.getParameterTypes();
		for(int i = 0 ; i < paramsTypes.length; i ++){
			ClassWrapper paramWrapper = ClassAnalyzer.getInstance().getWrapper(paramsTypes[i]);
			depNodes.add(new ClassUnderTest(paramWrapper, pMaxRecursion - 1));
		}
	}

	public Constructor<T> getCon(){
		return this.con;
	}
	
}
