package edu.cs.hku.testGeneration;

import java.math.BigInteger;

public class Assertion {
	public static <T> T notNull(final T t){
		if(t == null){
			throw new NullPointerException();
		}
		return t;
	}
	
	public static void check(final boolean b){
		if(b != true){
			throw new IllegalStateException();			
		}
	}
	
	public static boolean isNonNeg(BigInteger bigInteger){
		return (bigInteger.compareTo(BigInteger.ZERO) > 0);
	}
	
	public static boolean isArrayIndex(BigInteger bigInteger){
		return isNonNeg(bigInteger) && bigInteger.bitLength() <= 30;
	}
}
