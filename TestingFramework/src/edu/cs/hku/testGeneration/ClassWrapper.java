package edu.cs.hku.testGeneration;

import static edu.cs.hku.testGeneration.Assertion.check;
import static edu.cs.hku.testGeneration.Assertion.notNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;

public class ClassWrapper<T> implements ClassWrapperInterface<T>{
	
	protected Class<T> wrappedClass = null;
	protected List<Method> constrMethod = new ArrayList<Method>(); //Methods can construct this "wrappedClass"
	protected List<Expression<T>> presetPlans = new ArrayList<Expression<T>>();
	protected Hashtable<String, Class<? extends T>> children = new Hashtable<String, Class<? extends T>>(); // subclasses;
	protected boolean isSearched = false;
	protected boolean isNeed = false;
	
	public ClassWrapper(final Class<T> pClass){
		notNull(pClass);
		check(presetPlans.isEmpty());
		
		presetPlans.addAll(Arrays.asList((Expression<T>[])PresetValues.getPreset(pClass)));
	}
	
	
	public void setIsSearched(){
		isSearched = true;
	}
	
	public void setIsNeeded(){
		isNeed = true;
	}
	
	public boolean isNeeded(){
		return this.isNeed;
	}
	
	public boolean isInnerClass(){
		notNull(wrappedClass);
		if(Modifier.isStatic(wrappedClass.getModifiers())){
			//Static member class is never an inner class
			return false;
		}
		
		try {
			return (wrappedClass.getDeclaringClass() != null);
		} catch (RuntimeException e) {
			// TODO Auto-generated catch block
			return true;
		}
	}

	public boolean isLibraryType(){
		notNull(wrappedClass);
		
		if(wrappedClass.isPrimitive() || wrappedClass.isArray()){
			return true;
		}
		for(String libraryType: Constants.LIBRARY_TYPES){
			if(wrappedClass.getName().contains(libraryType)){
				return true;
			}
		}
		return false;
	}
	
	public Class<T> getWrappedClass(){
		return this.wrappedClass;
	}
	
	public List<Expression<T>> getPresetPlans(){
		return presetPlans;
	}
	
	public List<Class<? extends T>> getChildren(){
		final List<Class<? extends T>> res = new ArrayList<Class<? extends T>>();
		for(Class<? extends T> child: children.values()){
			res.add(child);
		}
		return res;
	}
	
	public List<Constructor<T>> getConstrs(){
		List<Constructor<T>> res = new ArrayList<Constructor<T>>();
		
		if(Modifier.isAbstract(wrappedClass.getModifiers())){
			return notNull(res);
		}
		
		Constructor[] constructors = new Constructor[0];
		try {
			constructors = wrappedClass.getDeclaredConstructors();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			return notNull(res);
		}
		
		for(Constructor<T> con: constructors){
			res.add(con);
		}
		return notNull(res);
	}
	
	public List<Method> getConMeths(){
		return notNull(constrMethod);
	}
	
	public void addConstrMeth(final Method pMeth){
		notNull(pMeth);
		check(Modifier.isAbstract(pMeth.getModifiers())==false);
		
		constrMethod.add(pMeth);
	}
	
	public void addChild(final Class<? extends T> pClass){
		children.put(pClass.getName(), pClass);
	}

}
