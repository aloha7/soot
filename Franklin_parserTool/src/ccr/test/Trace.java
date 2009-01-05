package ccr.test;

import java.util.*;

public class Trace {
	
	private Vector record = null;
	private static Trace singleton = null;
	
	public static Trace getInstance() {
		
		if (singleton == null) {
			singleton = new Trace();
		}
		return singleton;
	}
	
	private Trace() {
		
		initialize();
	}
	
	public void add(String index) {
		
		record.add(index);
	}
	
	public String[] getTrace() {
		
		String result[] = new String[record.size()];
		for (int i = 0; i < record.size(); i++) {
			result[i] = (String) record.get(i);
		}
		return result;
	}
	
	public void initialize() {
		
		if (record == null) {
			record = new Vector();
		} else {
			record.clear();
		}
	}
	
	public String toString() {
		
		return record.toString();
	}

}
