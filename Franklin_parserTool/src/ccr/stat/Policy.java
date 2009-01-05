package ccr.stat;

import ccr.app.*;

public class Policy {
	
	private final String context;
	public final String index;
	public final String constraint;
	private final String solution;
	
	public Policy(String ctx, String id, String c, String r) {
		
		context = ctx;
		index = id;
		constraint = c;
		solution = r;
	}
	
	public boolean isDiscard() {
		
		return solution.equals(Application.DISCARD_SOLUTION);
	}
	
	public String display() {
		
		return "(" + context + ", " + index + ", " + constraint + ", " + solution + ")";
	}
	
	public String toString() {
		
		return context + ":p" + index; 
	}

}
