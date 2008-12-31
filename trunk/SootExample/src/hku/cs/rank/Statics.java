package hku.cs.rank;

import java.util.HashMap;
import java.util.Set;

public class Statics {
	private class HelpStatics {
		int trueCases = 0;
		int falseCases = 0;
	}
	
	private int testTrue = 0;
	private int testFalse = 0;
	private HashMap<String, HashMap<Integer, HelpStatics>> instatics 
		= new HashMap<String, HashMap<Integer, HelpStatics>>();
	
	public int getTestTrue() {
		return testTrue;
	}
	public void addTestTrue() {
		this.testTrue ++;
	}
	public int getTestFalse() {
		return testFalse;
	}
	public void addTestFalse() {
		this.testFalse ++;
	}
	
	public void add(String klass, int line, boolean b){
		HashMap<Integer, HelpStatics> hms = instatics.get(klass);
		if(null == hms)
			hms = new HashMap<Integer, HelpStatics>();
		HelpStatics hs = hms.get(line);
		if(null == hs)
			hs = new HelpStatics();
		if(b)
			hs.trueCases++;
		else
			hs.falseCases++;
		hms.put(line, hs);
		instatics.put(klass, hms);
	}
	
	public Set<String> getClassNames(){
		return instatics.keySet();
	}
	
	public Set<Integer> getClassLines(String className){
		return instatics.get(className).keySet();
	}
	
	public int getClassLinesTrue(String className, int line){
		return instatics.get(className).get(line).trueCases;
	}
	
	public int getClassLinesFalse(String className, int line){
		return instatics.get(className).get(line).falseCases;
	}
	
	
	
	
	
}
