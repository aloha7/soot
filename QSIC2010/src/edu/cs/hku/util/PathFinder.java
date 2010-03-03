package edu.cs.hku.util;

import java.io.File;

import soot.Scene;

public class PathFinder {
	
	/**Look for the absolute directory of a given class in the default "java.class.path"
	 * 
	 * @param className: the name of the class to look for 
	 * @return: the absolute directory for a given class if it exists, otherwise null
	 */
	public static String findAbsoluteClassDir(String className){
		String classpath = System.getProperty("java.class.path");
		String clzName = className.replace('.', '/') + ".class";
		for(String clzpath: classpath.split(";")){
			File temp  = new File(clzpath + Constants.FS + clzName);
			if(temp.exists()){
//				System.out.println("Absolute directory for " + className + ":\n" + clzpath);
				return clzpath;
			}
		}
		
		System.out.println("Cannot find absolute directory for " + className);
		return null;
	}
	
	/**Look for the absolute path of a given class in the default "java.class.path"
	 * 
	 * @param className: the name of the class to look for 
	 * @return: the absolute path for a given class if it exists, otherwise null
	 */
	public static String findAbsoluteClassPath(String className){
		String clzDir = findAbsoluteClassDir(className);
		if(clzDir != null){
			return clzDir + Constants.FS + className.replace('.', '/') + ".class";
		}else{
			return null;
		}
	}
	
	/**Look for the absolute directory of a given class in the default "java.class.path"
	 * 
	 * @param fileName: the name of the file to look for
	 * @return: the absolute directory for a given class if it exists, otherwise null
	 */
	public static String findAbsoluteFileDir(String fileName){
		String classpath = System.getProperty("java.class.path");
		for(String clzPath: classpath.split(";")){
			File temp = new File(clzPath + Constants.FS + fileName);
			if(temp.exists()){
//				System.out.println("Absolute path for " + fileName + ":\n" + clzPath);
				return clzPath;
			}
		}
		return null;
	}
	
	
	/**Look for the absolute path of a given class in the default "java.class.path"
	 * 
	 * @param fileName: the name of the file to look for
	 * @return: the absolute path for a given class if it exists, otherwise null
	 */
	public static String findAbsoluteFilePath(String fileName){
		String clzDir = findAbsoluteFileDir(fileName);
		if(clzDir != null){
			return clzDir + Constants.FS + fileName;
		}else{
			return null;
		}
	}
	
}
