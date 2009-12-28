/*
 * DivByZero.java
 * 
 * Copyright 2006 Christoph Csallner and Yannis Smaragdakis.
 */
package trivia;


public class DivByZero {
  public static double inverse(int i) {	
		return 1/i;
	}	
  
  public static double always(int i) {	
		return i/0;
	}	
}
