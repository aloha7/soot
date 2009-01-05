package ccr.stat;

import java.util.*;

public class Resolution {
	
	public final String context;
	private Vector policies;
	
	public Resolution(String ctx) {
		
		context = ctx;
		policies = new Vector();
	}
	
	public void add(Policy p) {
		
		policies.add(p);
	}
	
	public boolean hasDiscard() {
		
		boolean result = false;
		for (int i = 0; i < size(); i++) {
			if (get(i).isDiscard()) {
				result = true;
				break;
			}
		}
		return result;
	}
	
	public Policy get(int i) {
		
		return (Policy) policies.get(i);
	}
	
	public int size() {
		
		return policies.size();
	}
	
	public String toString() {
		
		return policies.toString();
	}

}
