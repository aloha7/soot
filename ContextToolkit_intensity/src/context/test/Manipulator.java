package context.test;

import java.util.ArrayList;

public class Manipulator {
	private static Manipulator manipulator = null;
	
	private Manipulator(){
	}
	
	public Manipulator getInstance(){
		if(manipulator==null){
			manipulator = new Manipulator();
		}		
		return new Manipulator();
	}
	
	public synchronized int enterScheduler(long threadID, int cappID, String[][] driver){
		int position = -3;
		
		return position;
	}
	
	public synchronized int checkScheduler(long threadID, int cappID, String[][] driver){
		int position = -3; 
//		String index = 
		return position;
	}
}
