package ccr.stat;

import java.util.*;

public class VariableMap {
	
	private HashMap map;
	
	public VariableMap() {
		
		map = new HashMap();
	}
	
	public VariableMap(String names) {
		
		map = new HashMap();
		addAll(names);
	}
	
	public void add(String name) {
		
		if (!map.containsKey(name)) {
			map.put(name, new Variable(name));
		}
	}
	
	public void addContext(String name) {
		
		if (!map.containsKey(name)) {
			map.put(name, new ContextVariable(name));
		}
	}
	
	public void addAll(String names) {
		
		StringTokenizer st = new StringTokenizer(names, ", ");
		while (st.hasMoreTokens()) {
			add(st.nextToken());
		}
	}
	
	public boolean contains(String name) {
		
		return map.containsKey(name);
	}
	
	public Variable get(String name) {
		
		if (!map.containsKey(name)) {
			map.put(name, new Variable(name));
		}
		return (Variable) map.get(name);
	}
	
	public VariableSet getVariables(String names) {
		
		VariableSet set = new VariableSet();
		StringTokenizer st = new StringTokenizer(names, ", ");
		while (st.hasMoreTokens()) {
			set.add(get(st.nextToken()));
		}
		return set;
	}
	
	public Set keySet() {
		
		return map.keySet();
	}

}
