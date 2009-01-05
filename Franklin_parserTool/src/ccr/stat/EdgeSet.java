package ccr.stat;

import java.util.*;

public class EdgeSet implements Cloneable {
	
	private Vector edges;
	private NodeMap Pred, Succ;
	
	public EdgeSet() {
		
		edges = new Vector();
		Pred = new NodeMap();
		Succ = new NodeMap();
	}
	
	public Object clone() {
		
		try {
			EdgeSet set = (EdgeSet) super.clone();
			set.edges = (Vector) this.edges.clone();
			set.Pred = (NodeMap) this.Pred.clone();
			set.Succ = (NodeMap) this.Succ.clone();
			return set;
		} catch (CloneNotSupportedException e) {
			throw new InternalError();
		}
	}
	
	public void add(Edge edge) {
		
		if (!edges.contains(edge)) {
			edges.add(edge);
			Pred.add(edge.destNode, edge.sourNode);
			Succ.add(edge.sourNode, edge.destNode);
		}
	}
	
	public void add(Node sourNode, Node destNode) {
		
		add(new Edge(sourNode, destNode));
	}
	
	public void add(EdgeSet set) {
		
		for (int i = 0; i < set.size(); i++) {
			add(set.get(i));
		}
	}
	
	public boolean contains(Edge edge) {
		
		return edges.contains(edge);
	}
	
	public Edge get(int i) {
		
		if (i < 0 || i >= edges.size()) {
			return null;
		}
		return (Edge) edges.get(i);
	}
	
	public NodeSet getPred(Node node) {
		
		if (!Pred.containsKey(node)) {
			Pred.add(node, new NodeSet());
		}
		return Pred.get(node);
	}
	
	public NodeSet getSucc(Node node) {

		if (!Succ.containsKey(node)) {
			Succ.add(node, new NodeSet());
		}
		return Succ.get(node);
	}
	
	public void remove(Edge edge) {
		
		if (edges.contains(edge)) {
			edges.remove(edge);
			Pred.get(edge.destNode).remove(edge.sourNode);
			Succ.get(edge.sourNode).remove(edge.destNode);
		}
	}
	
	public void remove(Node sourNode, Node destNode) {
		
		edges.remove(new Edge(sourNode, destNode));
	}
	
	public int size() {
		
		return edges.size();
	}
	
	public String display() {
		
		return Succ.toString();
	}
	
	public String toString() {
		
		return edges.toString();
	}

}
