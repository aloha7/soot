package context.test.contextIntensity;

import java.util.*;

public class VariableSet {
	
	private Vector variables;
	
	public VariableSet() {
		
		variables = new Vector();
	}
	
	public VariableSet(Variable variable) {
		
		variables = new Vector();
		variables.add(variable);
	}
	
	public VariableSet add(Variable variable) {
		
		if (!variables.contains(variable)) {
			variables.add(variable);
		}
		return this;
	}
	
	public VariableSet add(VariableSet set) {
		
		for (int i = 0; i < set.size(); i++) {
			add(set.get(i));
		}
		return this;
	}
	
	public boolean contains(Variable variable) {
		
		return variables.contains(variable);
	}
	
	public boolean equals(Object object) {
		
		if (!(object instanceof VariableSet)) {
			return false;
		}
		VariableSet set = (VariableSet) object;
		boolean equal = true;
		if (size() != set.size()) {
			equal = false;
		} else {
			for (int i = 0; i < set.size(); i++) {
				if (!contains(set.get(i))) {
					equal = false;
					break;
				}
			}
		}
		return equal;
	}
	
	public Variable get(int i) {
		
		if (i < 0 || i >= variables.size()) {
			return null;
		}
		return (Variable) variables.get(i);
	}
	
	public VariableSet remove(Variable variable) {
		
		variables.remove(variable);
		return this;
	}
	
	public int size() {
		
		return variables.size();
	}
	
	public String toString() {
		
		return variables.toString();
	}

}
