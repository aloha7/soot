package edu.cs.hku.testGeneration;

import static edu.cs.hku.testGeneration.Assertion.notNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;






public class ClassAnalyzer {
	protected Hashtable<String, ClassWrapper<?>> class2Wrapper = new Hashtable<String, ClassWrapper<?>>();
	Class<?>[] classes = null; //global visibility
	
	private static ClassAnalyzer instance;
	
	public static ClassAnalyzer getInstance(){
		if(instance == null){
			instance = new ClassAnalyzer();
		}
		return instance;
	}
	
	protected ClassAnalyzer(){
		
	}
	
	
	
	public ClassAnalyzer(Class<?>[] classes){
		this.classes = notNull(classes);
		
		//this transformation can eliminate duplications
		HashSet<Class<?>> classSet = new HashSet<Class<?>>();
		for(Class<?> clazz: classes){
			classSet.add(clazz);
		}
		
		for(Class<?> clazz: classSet){
			getWrapper(clazz); //get preset values for to clazz
		}
		crawl(classSet);
	}
	
	/**initialize the clazz and its sub-classes
	 * 
	 * @param clazz
	 */
	public boolean initializeDeep(Class<?> clazz){
		notNull(clazz);
		try {
			clazz.forName(clazz.getName());
			Class<?> innerClazz = clazz.getEnclosingClass();
			if(innerClazz == null){
				return true;
			}else{
				return initializeDeep(innerClazz);	
			}
		} catch (ClassNotFoundException e) {
			return false;
		}
	}
	
	
	public <T> ClassWrapper<T> getWrapper(final Class<T> clazz){
		notNull(clazz);
		ClassWrapper<T> classWrapper = (ClassWrapper<T>)class2Wrapper.get(clazz.getName());
		if(classWrapper == null){
			classWrapper = new ClassWrapper(clazz);
			class2Wrapper.put(clazz.getName(), classWrapper);
		}
		return classWrapper;
	}
	
	public ClassWrapper<?>[] getWrappers(){
		Collection<ClassWrapper<?>> wrappers = class2Wrapper.values();
		return wrappers.toArray(new ClassWrapper<?>[wrappers.size()]);
	}
	
	
	public void crawl(final Set<Class<?>> classes){
		for(Class<?> clazz: classes){
			initializeDeep(clazz);
		}
		
		boolean maybeMore = true;
		while(maybeMore){
			maybeMore = false;
			
			for(ClassWrapper<?> cw: getWrappers()){
				if(cw.isSearched == false){
					//add all methods to other wrappers
					findRules(cw);
					maybeMore = true;
				}
			}
		}
	}
	
	protected void findRules(final ClassWrapper<?> cw){
		notNull(cw);
		cw.setIsSearched();
		
		if(cw.isLibraryType()){
			return;
		}
		
		queueMethParams(cw);
		queueFamily(cw);
	}
	
	/**Find types of parameters and return values(constrMeth) 
	 * 
	 * @param cw
	 */
	public void queueMethParams(final ClassWrapper<?> cw){
		Method[] methods = new Method[0];
		
		try {
			methods = cw.getWrappedClass().getDeclaredMethods();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		for(Method meth: methods){
			Class<?> returnType = meth.getReturnType();
			if(!initializeDeep(returnType)){
				continue; //skip classes we cannot fully initialize
			}
			
			ClassWrapper rW = getWrapper(returnType);
			if(Modifier.isAbstract(meth.getModifiers()) == false &&
					rW.getWrappedClass().isPrimitive() == false){
				rW.addConstrMeth(meth);
			}
			
			for(Class<?> paramType: meth.getParameterTypes()){
				if(initializeDeep(paramType)){
					getWrapper(paramType);
				}
			}
		}
		
		Constructor[] constructors = new Constructor[0];
		
		try {
			constructors = cw.getWrappedClass().getConstructors();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		for(Constructor con: constructors){
			for(Class<?> paramType: con.getParameterTypes()){
				if(initializeDeep(paramType)){
					getWrapper(paramType);
				}
			}
		}
	}
	
	public void queueFamily(final ClassWrapper<?> cw){
		for(Class<?> superInterface:cw.getWrappedClass().getInterfaces()){
			if(initializeDeep(superInterface)){
				ClassWrapper superIW = getWrapper(superInterface);
				superIW.addChild(cw.getWrappedClass());
			}
		}
		
		Class<?> superClass = cw.getWrappedClass().getSuperclass();
		if(initializeDeep(superClass)){
			ClassWrapper superCW = getWrapper(superClass);
			superCW.addChild(cw.getWrappedClass());
		}
		
		Class<?>[] nestedClasses = new Class[0];
		try {
			nestedClasses = cw.getWrappedClass().getDeclaredClasses();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		for(Class<?> nestedClass: nestedClasses){
			if(initializeDeep(nestedClass)){
				getWrapper(nestedClass);
			}
		}
		
		Class<?> nestingClass = null;
		
		try {
			nestingClass = cw.getWrappedClass().getDeclaringClass();
		} catch (RuntimeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(initializeDeep(nestingClass)){
			getWrapper(nestingClass);
		}
	}
	
	public void crashClasses(){
		for(Class<?> clazz: classes){
			
		}
	}
	
	
	
}
