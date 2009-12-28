package edu.cs.hku.util;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import trivia.TestClass;


public class MyClassLoader extends ClassLoader {
	public static String dir;

	public MyClassLoader(String dir) {
		super();
		this.dir = dir;
	}

	public MyClassLoader(ClassLoader arg0) {
		super(arg0);
	}

	public Class loadClass(String name, boolean resolved) {
//		Class result = null;
//		try {
//			String className = name.replace('.', '/') + ".class";
//			
//			FileInputStream fis = null;
//			byte[] data = null;
//			fis = new FileInputStream(new File(dir + Constants.FS + className));
//			ByteArrayOutputStream baos = new ByteArrayOutputStream();
//			int ch = 0;
//			while ((ch = fis.read()) != -1) {
//				baos.write(ch);
//			}
//			data = baos.toByteArray();
//			
//			result = defineClass(name, data, 0, data.length);
//			//System.out.println();
//			
//		} catch (Exception e) { //cannot find it via MyClassLoader
//			try {
//				result = super.loadClass(name, resolved);
//			} catch (ClassNotFoundException e1) {
//				// TODO Auto-generated catch block
//				e1.printStackTrace();
//			}
//		}
//		return result;
		
		Class result = null; // use this method to load class which is not in the classpath at runtime 		
		try {
			String className = name.replace('.', '/') + ".class";			
			URL tempURL = new File(dir + Constants.FS + className).toURL();
			
			String cp_new = Constants.USER_DIR + Constants.FS  + "sootOutput;" + 
			Constants.CLASSPATH;		
			System.setProperty("java.class.path", cp_new);
			
			URLClassLoader loader = new URLClassLoader(new URL[]{tempURL}, TestClass.class.getClassLoader());
			result =  loader.loadClass(name);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
//		
//		Class result = null;
//		String cp_new = Constants.USER_DIR + Constants.FS  + "sootOutput;" + 
//		Constants.CLASSPATH;		
//		System.setProperty("java.class.path", cp_new);
//		ClassLoader loader = new ClassLoader(MyClassLoader.class.getClassLoader());
		
		
		return result;
	}

//	public Class findClass(String name){
//		byte[] data = loadClassData(name);
//		return defineClass(name, data, 0, data.length);
//	}
//	
//	public byte[] loadClassData(String name){
//		FileInputStream fis = null;
//		byte[] data = null;
//		String className = name.replace('.', '/') + ".class";
//		try {
//			fis = new FileInputStream(new File(dir + Constants.FS + className));
//			ByteArrayOutputStream baos = new ByteArrayOutputStream();
//			int ch = 0;
//			while ((ch = fis.read()) != -1) {
//				baos.write(ch);
//			}
//			data = baos.toByteArray();
//		} catch (FileNotFoundException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		return data;
//	}

	public static void main(String[] args) {

		MyClassLoader loader = new MyClassLoader(Constants.USER_DIR
				+ Constants.FS + "sootOutput");

		try {
			Class objClass = loader.loadClass("trivia.TestClass", false);
			Object obj = objClass.newInstance();
			System.out.println(objClass.getClassLoader());
			System.out.println(objClass.getClassLoader().getSystemResource(
					"trivia/TestClass.class").getPath());
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
