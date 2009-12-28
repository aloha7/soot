/*
 * NullDeref.java
 * 
 * Copyright 2006 Christoph Csallner and Yannis Smaragdakis.
 */
package trivia;

/**
 * Throws java.lang.NullPointerException
 * 
 * @author csallner@gatech.edu (Christoph Csallner)
 */
public class NullDeref {

	/**
	 * Crashes for o==null.
	 */
	public static int foo(Object o) {
		return o.hashCode();
	}
}
