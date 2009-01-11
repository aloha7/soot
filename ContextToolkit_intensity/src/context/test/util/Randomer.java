package context.test.util;

import java.util.Random;

public class Randomer {	
	private static Random random = null;
	
	public static Random getInstance(){
		if(random == null){
			 random = new Random();
		}
		return random;
	}
}
