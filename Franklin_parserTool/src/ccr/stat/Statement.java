package ccr.stat;

import java.util.*;

public class Statement {
	
	private Vector tokens;
	private HashSet Def, Use;
	private boolean definesContext;
	
	public Statement() {	
		tokens = new Vector();
		Def = new HashSet();
		Use = new HashSet();
		definesContext = false;
	}
	
	public Statement(String token) {
		
		tokens = new Vector();
		add(token);
	}
		
	
	public void add(String token) {
		
		tokens.add(token);
	}
	
	public String get(int i) {
		
		return (String) tokens.get(i);
	}
	
	
	
	public String prefix() { // the first token is the prefix
		
		String result = "";
		if (tokens.size() > 0) {
			result = (String) tokens.get(0);
		}
		return result;
	}
	
	public boolean hasContextDef() {
		
		return definesContext;
	}
	
	public boolean isNode() {
		
		if (prefix().equals("") || prefix().equals("else") || prefix().equals("}")) {
			return false;
		}
		return true;
	}
	
	public int size() {
		
		return tokens.size();
	}
	
	public void analyzeDefUse(HashSet variables, HashSet contexts, HashSet assignments) {
		
		int i = size() - 1;
		for (; i >= 0; i--) {
			if (assignments.contains(get(i))) {
				break;
			}
		}
		for (int j = i - 1; j >= 0; j--) {
			String token = get(j);
			if (variables.contains(token)) {
				Def.add(token);
				break;
			} else if (contexts.contains(token)) {
				Def.add(token);
				definesContext = true;
				break;
			}
		}
		for (int j = i + 1; j < size(); j++) {
			String token = get(j);
			if (variables.contains(token)) {
				Use.add(token);
			} else if (contexts.contains(token)) {
				Use.add(token);
			}
		}
	}
	
	public HashSet getDef() {
		
		return Def;
	}
	
	public HashSet getUse() {
		
		return Use;
	}
	
	public String toString() {
		
		String result = "";
		for (int i = 0; i < tokens.size(); i++) {
			result = result + (String) tokens.get(i) + " ";
		}
		if (result.charAt(result.length() - 1) == ' ') {
			result = result.substring(0, result.length() - 1);
		}
		return result;
	}

}
