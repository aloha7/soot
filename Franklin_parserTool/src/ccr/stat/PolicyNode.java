package ccr.stat;


public class PolicyNode extends Node {
	
	public final Policy policy;
	
	public PolicyNode(String id, String s, VariableSet d, VariableSet u, Policy p) {
		
		super(id, s, d, u);
		policy = p;
	}
	
/*	public PolicyNode(Node node, Policy policy) {
		
		index = node.index + Application.POLICY_NODE_DELIMITER + policy.index;
		statement = policy.constraint;
		if (policy.isDiscard()) {
			Def = new VariableSet();
		} else {
			Def = (new VariableSet()).add(node.getDef(0));
		}
		Use = new VariableSet();
	}*/
	
	public boolean equals(Object object) {
		
		if (!(object instanceof PolicyNode)) {
			return false;
		}
		PolicyNode node = (PolicyNode) object;
		boolean result = index.equals(node.index) && statement.equals(node.statement) && 
				Def.equals(node.Def) && Use.equals(node.Use) && policy.equals(node.policy);
		return result;
	}

}
