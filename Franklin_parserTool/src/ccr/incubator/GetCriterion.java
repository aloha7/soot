package ccr.incubator;

import java.util.HashMap;
import java.util.Vector;

import ccr.stat.CFG;
import ccr.stat.Criterion;
import ccr.stat.Node;
import ccr.stat.NodeMap;
import ccr.stat.NodeSet;

public class GetCriterion {

	public static void main(String[] args) {
		CFG g = new CFG(System.getProperty("user.dir")
				+"/src/ccr/app/TestCFG2.java");		
		Criterion c = g.getAllUses();
		
		StringBuilder sb = new StringBuilder();
		NodeMap duPairs = c.getDuMaps();
		
		HashMap<String, HashMap<Node,Vector<Node>>> temp = new HashMap<String, 
												HashMap<Node,Vector<Node>>>();
		
		
		for(int i = 0; i < duPairs.keySize(); i ++){
			Node defNode = duPairs.getKey(i);
			String defVariable = defNode.getDef(0).name;
			
			HashMap<Node, Vector<Node>> dus = temp.get(defVariable);
			if(dus == null){
				dus = new HashMap<Node, Vector<Node>>();
			}
			
			Vector<Node> useNodes = dus.get(defNode);
			if(useNodes == null){
				useNodes = new Vector<Node>();
			}
			
			NodeSet uses = duPairs.getValue(i);
			for(int j = 0; j < uses.size(); j ++){
				useNodes.add(uses.get(j));
			}
			
			dus.put(defNode, useNodes);
			temp.put(defVariable, dus);
		}
		
		
		for(String variable: temp.keySet()){
			sb.append(variable + "\n");			
		}
		sb.append(temp.size() + "\n\n");
		
		for(String variable: temp.keySet()){			
			HashMap<Node, Vector<Node>> dus = temp.get(variable);
			sb.append("Variable:" + variable + "(" + dus.size()+ ")\n");
			for(Node defNode: dus.keySet()){
				sb.append(defNode.statement + "(" + defNode.index+ ") ->" );
				Vector<Node> useNodes = dus.get(defNode);
				sb.append(useNodes.size() + "\n");
			}
			sb.append("\n\n");
		}
		
		
		
		
		System.out.println(sb.toString());
	}

}
