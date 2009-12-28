package edu.cs.hku.testGeneration;

import static edu.cs.hku.testGeneration.Assertion.notNull;

public class CodeGenFct {
	
	public static String getName(final Class<?> type, final Class<?> testee){
		notNull(type);
		notNull(testee);
		
		String classTopLevel = getLeafTopLevelName(type).getCanonicalName();
		String classPackage = "";
		if(classTopLevel.indexOf('.') > 0){
			classPackage = classTopLevel.substring(0, classTopLevel.lastIndexOf('.'));
		}
		
		String testeeTopLevel = getLeafTopLevelName(testee).getCanonicalName();
		String testeePackage = "";
		if(testeeTopLevel.indexOf('.') > 0){
			testeePackage = testeeTopLevel.substring(0, testeeTopLevel.lastIndexOf('.'));
		}
		
		String className = getName(type);
		if(classPackage.equals(testeePackage) && classPackage.length() >0){
			className = className.substring(classPackage.length() + 1); //suppress package
		}
		
		return className;
	}
	
	public static String getName(final Class<?> pClass){
		Class<?> myClass = notNull(pClass);
		String res = null;
		
		int arrayDepth = 0;
		while(myClass.isArray()){
			myClass = myClass.getComponentType();
			arrayDepth += 1;
		}
		
		StringBuilder className = new StringBuilder(myClass.getName());
		
		 /* Remove "p.Enc$" from "Enc$Nested" in class "p.Enc.Nested" */
		if(myClass.getDeclaringClass()!= null){
			int enclosingNameLength = myClass.getDeclaringClass().getName().length();
			className.setCharAt(enclosingNameLength, '.');
		}
		
		for(int i = 0; i < arrayDepth; i ++){
			className.append("[]");
		}
		
		return notNull(res.toString());
	}
	
	public static Class<?> getLeafTopLevelName(final Class<?> pType){
		Class<?> type = notNull(pType);
		
		while(type.isArray()){
			type = type.getComponentType();
		}
		
		while(type.getDeclaringClass()!= null){
			type = type.getDeclaringClass();
		}
		return type;
	}
}
