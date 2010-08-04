package ccr.stat;

import java.util.*;

public class NodeSet implements Cloneable {
	
	private Vector nodes;
	
	public NodeSet() {
		
		nodes = new Vector();
	}
	
	public NodeSet(Node node) {
		
		nodes = new Vector();
		nodes.add(node);
	}
	
	public Object clone() {
		
		try {
			NodeSet set = (NodeSet) super.clone();
			set.nodes = (Vector) this.nodes.clone();
			return set;
		} catch (CloneNotSupportedException e) {
			throw new InternalError();
		}
	}
	
	public NodeSet add(Node node) {
		
		if (!nodes.contains(node)) {
			nodes.add(node);
		}
		return this;
	}
	
	public NodeSet add(NodeSet set) {
		
		for (int i = 0; i < set.size(); i++) {
			add(set.get(i));
		}
		return this;
	}
	
	public void clear() {
		
		nodes.clear();
	}
	
	public boolean contains(Node node) {
		
		return nodes.contains(node);
	}
	
	public boolean equals(Object object) {
		
		if (!(object instanceof NodeSet)) {
			return false;
		}
		NodeSet set = (NodeSet) object;
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
	
	public Node get(int i) {
		
		if (i < 0 || i >= nodes.size()) {
			return null;
		}
		return (Node) nodes.get(i);
	}
	
	public boolean isEmpty() {
		
		return nodes.isEmpty();
	}
	
	public NodeSet remove(Node node) {
		
		nodes.remove(node);
		return this;
	}
	
	public NodeSet remove(NodeSet set) {
		
		for (int i = 0; i < set.size(); i++) {
			remove(set.get(i));
		}
		return this;
	}
	
	public NodeSet remove(int i){
		if(i >= 0 && i < size() ){
			nodes.remove(i);
		}
		return this;
	}
	
	public NodeSet removeAll() {
		
		nodes.clear();
		return this;
	}
	
	public int size() {
		
		return nodes.size();
	}
	
	
	public String display() {
		
		String result = "";
		for (int i = 0; i < size(); i++) {
			result = result + get(i).display() + "\n";
		}
		return result;
	}
	
	public String toString() {
		
		return nodes.toString();
	}

}
