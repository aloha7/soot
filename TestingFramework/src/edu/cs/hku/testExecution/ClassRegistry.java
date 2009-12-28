package edu.cs.hku.testExecution;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Vector;

import trivia.Human;

public class ClassRegistry {
	private static List<Class<?>> classes = new Vector<Class<?>>();
	private final static Class[] zeroFormalParams = new Class[0];
	
	public static void resetClasses(){
		
			try {
				zeroStaticFields();
				classInitializers();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
				
	}
	
	protected static void zeroStaticFields() throws IllegalAccessException{
		for(int i = 0; i < classes.size(); i ++){
			Class<?> c = classes.get(i);
			zeroStaticFields(c);
		}
	}
	
	//reset the values of c's non-final static fields to the default = {null, 0, 0.0, false}
	/** Precond:
		 * 1. f static
		 * 2. f non-final*/
	private static void zeroStaticFields(Class<?> c) throws IllegalAccessException{
		Field[] fields = c.getDeclaredFields();
		for(int  i =0; i < fields.length; i ++){
			int fieldModifiers = fields[i].getModifiers();
			
			if((Modifier.isStatic(fieldModifiers))
					&& (!Modifier.isFinal(fieldModifiers))
					&&(!fields[i].getName().startsWith("class$"))){
				zeroField(fields[i]);
			}
		}
	}
	
	
	/*
	 * Sets a static non-final field to all-zeros = {null, 0, 0.0, false}
	 * 
	 
	 * 
	 * @param Field of any type
	 */
	private static void zeroField(Field f) throws IllegalAccessException{
		f.setAccessible(true);
		
		Class<?> fieldType = f.getType();
		if(!fieldType.isPrimitive()){
			f.set(null, null); // receiver-object = null as static, value = null as reset			
		}else{
			//primitive types
			if(fieldType.getName().equals("boolean")){
				f.setBoolean(null, false);
			}
			if(fieldType.getName().equals("byte")){
				f.setByte(null, (byte)0);
			}
			if(fieldType.getName().equals("char")){
				f.setChar(null, (char)0);
			}
			if(fieldType.getName().equals("short")){
				f.setShort(null, (short)0);
			}
			if(fieldType.getName().equals("int")){
				f.setInt(null, 0);
			}
			if(fieldType.getName().equals("long")){
				f.setLong(null, 0);
			}
			if(fieldType.getName().equals("float")){
				f.setFloat(null, 0.0f);
			}
			if(fieldType.getName().equals("double")){
				f.setDouble(null, 0.0d);
			}
		}
	}
	
	protected static void classInitializers() throws Exception{
		for(int i = 0; i < classes.size(); i ++){
			Class<?> c = classes.get(i);
			Method clreinit = c.getDeclaredMethod("clreinit", zeroFormalParams);
			clreinit.invoke(null, new Object[0]); //static meth, zero params
		}
	}
	
	//BCEL-ClassLoader calls this method to register a loaded class in the runtime.
	//The order in which classes are registered is preserved in the list
	public static void register(Class<?> c){
		assert c!= null;
		classes.add(c);
	}
	
	public static void main(String[] args){
		ClassRegistry.classes.add(Human.class);
		resetClasses();
		Human.print();
		
		Class<?> obj = Human.class.getSuperclass();
		if(obj.isAssignableFrom(Human.class)){
			System.out.println("Super is assignable from sub");			
		}
	}
}
