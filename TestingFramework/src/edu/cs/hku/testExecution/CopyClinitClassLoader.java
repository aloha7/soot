package edu.cs.hku.testExecution;


import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Iterator;

import org.apache.bcel.Constants;
import org.apache.bcel.classfile.ConstantCP;
import org.apache.bcel.classfile.ConstantNameAndType;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.generic.CPInstruction;
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.InstructionConstants;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.Type;




public class CopyClinitClassLoader 
	extends org.apache.bcel.util.ClassLoader
	implements junit.runner.TestSuiteLoader{
	
	private final static String[] ignoredPackages = new String[]{
		"java.", "sun.", "junit.",
		"edu.gatech.cc.jcrasher.testall.runtime."
	};
	
	public CopyClinitClassLoader(){
		super(ignoredPackages);
	}
	
	public Class<?> load(String className) throws ClassNotFoundException{
		return super.loadClass(className, true);
	}
	
	public Class reload(Class aClass){
		return aClass;
	}
	
	protected boolean isExcludedClass(JavaClass pClass){
		if(!pClass.isPublic()){
			return true;
		}
		if(pClass.isInterface()){
			return true;
		}
		
		try {
			JavaClass[] classes = pClass.getSuperClasses();
			for(int i = 0; i < classes.length; i ++){
				if(classes[i].getClassName().equals("junit.framework.TestCase")){
					return true;
				}
			}
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			
		}
		
		for(int i = 0; i < ignoredPackages.length; i ++){
			if(pClass.getPackageName() == ignoredPackages[i]){
				return true;
			}
		}
		return false;
	}

	private boolean isExcludedClass(Class<?> c){
		if(!Modifier.isPublic(c.getModifiers())){
			return true;
		}
		if(c.isInterface()){
			return true;
		}
		if(junit.framework.TestCase.class.isAssignableFrom(c)){
			//the former is the super class of the latter
			return true;
		}
		for(int i = 0; i < ignoredPackages.length; i ++){
			if(c.getName().startsWith(ignoredPackages[i])){
				return true;
			}
		}
		return false;
	}

	private MethodGen get_clinit(ClassGen clazz){
		MethodGen _clinit = null;
		for(int i = 0; i < clazz.getMethods().length; i ++){
			if(clazz.getMethods()[i].getName().equals("<clinit>")){
				_clinit = new MethodGen(
						clazz.getMethods()[i],
						clazz.getClassName(),
						clazz.getConstantPool()
				);
			}
		}
		return _clinit;
	}
	
	private MethodGen add_clinit(ClassGen clazz){
		InstructionList instrList = new InstructionList();
		instrList.append(InstructionConstants.RETURN);
		MethodGen _clinit = new MethodGen(
			Constants.ACC_STATIC,
			Type.VOID,
			new Type[0],
			new String[0],
			"<clinit>",
			clazz.getClassName(),
			instrList,
			clazz.getConstantPool()
		);
		_clinit.setMaxLocals();
		_clinit.setMaxStack();
		clazz.addMethod(_clinit.getMethod());
		return _clinit;
	}
	
	private void copy_clinit(MethodGen _clinit, ClassGen clazz){
		MethodGen clinit = new MethodGen(
				_clinit.getMethod(),
				clazz.getClassName(),
				clazz.getConstantPool());
		MethodGen clreinit = new MethodGen(
				_clinit.getMethod(),
				clazz.getClassName(),
				clazz.getConstantPool());
		
		clinit.setName("clinit");
		clinit.setAccessFlags(Constants.ACC_PUBLIC + Constants.ACC_STATIC);
		clreinit.setName("clreinit");
		clreinit.setAccessFlags(Constants.ACC_PUBLIC + Constants.ACC_STATIC);
		
		clinit.setMaxLocals();
		clinit.setMaxStack();
		clazz.addMethod(clinit.getMethod());
		
		this.modifyclreinit(clreinit, clazz);
		clreinit.setMaxLocals();
		clreinit.setMaxStack();
		clazz.addMethod(clreinit.getMethod());
	}
	
	private void modifyclreinit(MethodGen clreinit, ClassGen clazz){
		for(Iterator i = clreinit.getInstructionList().iterator(); i.hasNext();){
			InstructionHandle instrHandle = (InstructionHandle)i.next();
			short opCode = instrHandle.getInstruction().getOpcode();
			if((opCode == Constants.PUTSTATIC)
				||(opCode == Constants.PUTFIELD2_QUICK)
				||(opCode == Constants.PUTSTATIC_QUICK)){
				
				CPInstruction instr = (CPInstruction)instrHandle.getInstruction();
				ConstantCP fieldConst = (ConstantCP)clazz.getConstantPool().getConstant(instr.getIndex());
				ConstantNameAndType fieldSig = (ConstantNameAndType)clazz.getConstantPool().getConstant(fieldConst.getNameAndTypeIndex());
				Field field = clazz.containsField(fieldSig.getName(clazz.getConstantPool().getConstantPool()));
				
				if((field.getAccessFlags() & Constants.ACC_FINAL) == Constants.ACC_FINAL){
					instrHandle.setInstruction(InstructionConstants.NOP);
				}
			}
		}
	}
	
	
	protected JavaClass modifyClass(JavaClass pClazz){
		if(isExcludedClass(pClazz)){
			return pClazz;
		}
		
		ClassGen clazz = new ClassGen(pClazz);
		MethodGen _clinit = get_clinit(clazz);
		if(_clinit == null){
			_clinit = add_clinit(clazz);
		}
		
		copy_clinit(_clinit, clazz);
		
		clazz.update();
		return clazz.getJavaClass();
		
	}
	
	protected Class<?> loadClass(String className, boolean resolve) throws ClassNotFoundException{
		Class<?> res = super.loadClass(className, resolve);
		
		if(isExcludedClass(res)){
			return res;
		}
		
		
			try {
				Method clinit = res.getDeclaredMethod("clinit", new Class[0]);
				clinit.invoke(null, new Object[0]);
			} catch (SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			ClassRegistry.register(res);
			return res;
		
		
	}
	
	
}

