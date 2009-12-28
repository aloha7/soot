package edu.cs.hku.util;

public class Constants {
	public final static String CLASSPATH = System.getProperty("java.class.path");
	
	public final static String USER_DIR = System.getProperty("user.dir");
	
	/*Default exclude library types*/
	public final static String[] LIBRARY_TYPES = new String[]{
	    "java.", 
	    "javax.", 
	    "sun.", 
	    "com.sun.", 
	    "org.apache.", 
	    "org.ietf.",
	    "org.omg.", 
	    "org.w3c.", 
	    "sunw."};
	
	/*Line separator*/
	public final static String LS = System.getProperty("line.separator");
	
	/*File separator*/
	public final static String FS = System.getProperty("file.separator");
	
	/*Path separator*/
	public final static String PS = System.getProperty("path.separator");
	
	/* Spaces= two spaces*/
	public final static String Spaces = "  ";
	
	/*One tab*/
	public final static String TAB = "\t";
	
	/**
	 * the required number of criterion-adequate test set
	 */
	public final static int NUM_TESTSET = 10;
	
	/**
	 * the trial time to select test cases when constructing test sets
	 */
	public final static int MAX_TRIAL_TIME = 5;
	
	public final static int SIZE_TESTPOOL = 20000;
	
	public final static int START_TESTPOOL = -10000;
}
