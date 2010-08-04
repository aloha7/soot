package ccr.stat;

public class Node {
	
	public final String index;
	public final String statement;
	public final VariableSet Def, Use;
//	public NodeSet Pred, Succ;
	
	public Node(String id, String s, VariableSet d, VariableSet u) {
		
		index = id;
		statement = s;
		Def = d;
		Use = u;
	//	Pred = new NodeSet();
	//	Succ = new NodeSet();
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
	
	public boolean hasContextDef() {
		
		return sizeDef() > 0 && getDef(0) instanceof ContextVariable;
	}
	
	public VariableSet getContextDef(){
		VariableSet ctxDefs = new VariableSet();
		
		for(int i = 0; i < Def.size(); i ++){
			if(Def.get(i) instanceof ContextVariable){
				ctxDefs.add(Def.get(i));
			}
		}
		
		return ctxDefs;
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
	
	public boolean equals(Node tmp){
		return index.equals(tmp.index);
	}
	
	
	public String toString() {
		
		return index;
	}

}
