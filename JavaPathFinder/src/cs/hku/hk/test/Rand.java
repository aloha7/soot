package cs.hku.hk.test;

import java.util.Random;

public class Rand {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Random random = new Random(42);
		
		int a = random.nextInt(2);
		System.out.println("a = " + a);
		
		int b = random.nextInt(3);
		System.out.println(" b = " + b);
		
		int c = a / (b + a -2);
		System.out.println("   c = " + c);
		
	}

}
