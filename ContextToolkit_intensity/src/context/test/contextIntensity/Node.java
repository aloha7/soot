package context.test.contextIntensity;

public class Node {
	
	public final String index;
	protected final String statement;
	protected final VariableSet Def, Use;
	public boolean traversed;
	
	
	public Node(String id){
		this(id, null, null, null);
	}
	
	public Node(String id, String s, VariableSet d, VariableSet u) {
		
		index = id;
		statement = s;
		Def = d;
		Use = u;
		traversed = false;
	}
	
	
	public boolean containsDef(Variable v) {
		
		return Def.contains(v);
	}
	
	public boolean containsUse(Variable v) {
		
		return Use.contains(v);
	}
	
/*	public boolean equals(Object object) {
		
		if (!(object instanceof Node)) {
			return false;
		}
		Node node = (Node) object;
		boolean result = index.equals(node.index) && statement.equals(node.statement) && 
				Def.equals(node.Def) && Use.equals(node.Use);
		return result;
	}*/
	
	public boolean hasSameDef(Node node) {
		
		return node.sizeDef() > 0 && containsDef(node.getDef(0));
	}
	
	
	public Variable getDef(int i) {
		
		return (Variable) Def.get(i);
	}
	
	public Variable getUse(int i) {
		
		return (Variable) Use.get(i);
	}
	
	public int sizeDef() {
		
		return Def.size();
	}
	
	public int sizeUse() {
		
		return Use.size();
	}
	
	public String display() {
		
		return index + ": " + statement + /*"  Pred" + Pred + " Succ" + Succ + */
				" Def" + Def + " Use" + Use;
	}
	
	public String toString() {
		
		return index;
	}

}
