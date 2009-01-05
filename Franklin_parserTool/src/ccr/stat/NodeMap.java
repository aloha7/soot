package ccr.stat;

import java.util.*;

public class NodeMap implements Cloneable {
	
//	private NodeSet keySet;
//	private HashMap map;
	private Vector keySet;
	private Vector valueSet;
	
	public NodeMap() {
		
	//	keySet = new NodeSet();
	//	map = new HashMap();
		keySet = new Vector();
		valueSet = new Vector();
	}
	
	public Object clone() {
		
		try {
			NodeMap nodemap = (NodeMap) super.clone();
			nodemap.keySet = (Vector) this.keySet.clone();
			nodemap.valueSet = new Vector();
			for (int i = 0; i < this.keySet.size(); i++) {
				nodemap.valueSet.add(((NodeSet)this.valueSet.get(i)).clone());
			}
		/*	nodemap.keySet = (NodeSet) this.keySet.clone();
			nodemap.map = new HashMap();
			for (int i = 0; i < nodemap.keySet.size(); i++) {
				Node node = nodemap.keySet.get(i);
				nodemap.map.put(node, ((NodeSet) this.get(node)).clone());
			}*/
		/*	nodemap.map = (HashMap) this.map.clone();
			for (Iterator i = iterator(); i.hasNext();) {
				Node node = (Node) i.next();
				nodemap.map.put(node, ((NodeSet) this.map.get(node)).clone());
			}*/
			return nodemap;
		} catch (CloneNotSupportedException e) {
			throw new InternalError();
		}
	}
	
	public void add(Node node, Node node1) {
		
		int i = keySet.indexOf(node);
		if (i != -1) {
			NodeSet set = getValue(i);
			set.add(node1);
		} else {
			keySet.add(node);
			NodeSet set1 = (new NodeSet()).add(node1);
			valueSet.add(set1);
		}
	/*	if (containsKey(node)) {
			NodeSet set = get(node);
			set.add(node1);
		} else {
			keySet.add(node);
			NodeSet set1 = (new NodeSet()).add(node1);
			map.put(node, set1);
		//	put(node, set.add(node1));
		}*/
	}
	
	public void add(Node node, NodeSet set1) {
		
		int i = keySet.indexOf(node);
		if (i != -1) {
			NodeSet set = getValue(i);
			set.add(set1);
		} else {
			keySet.add(node);
			valueSet.add(set1);
		}
	/*	if (containsKey(node)) {
			NodeSet set = get(node);
			set.add(set1);
		} else {
		//	put(node, set1);
			keySet.add(node);
			map.put(node, set1);
		}*/
	}
	
	public void add(NodeMap map1) {
		
	/*	for (Iterator i = map1.iterator(); i.hasNext();) {
			Node node = (Node) i.next();
		//	add(node, map1.get(node));
			if (map.containsKey(node)) {
				get(node).add(map1.get(node));
			} else {
			//	put(node, map1.get(node));
			}
		}*/
		for (int i = 0; i < map1.keySize(); i++) {
			add(map1.getKey(i), map1.getValue(i));
		/*	Node node = map1.getKey(i);
		//	add(node, map1.get(node));
			if (containsKey(node)) {
			//	NodeSet set = get(node);
				NodeSet set = (NodeSet) map.get(node);
				set.add(map1.get(node));
			} else {
				keySet.add(node);
				map.put(node, map1.get(node));
			}*/
		}
	}
	
	public boolean containsKey(Node node) {
		
	//	return map.containsKey(node);
		return keySet.contains(node);
	}
	
	public boolean containsAssociation(Node node, Node node1) {
		
		return containsKey(node) && get(node).contains(node1);
	}
	
	public NodeSet get(Node node) {
		
	//	return (NodeSet) map.get(node);
		int i = keySet.indexOf(node);
		if (i == -1) {
			return null;
		}
		return getValue(i);
	}
	
	public Node getKey(int i) {
		
		if (i < 0 || i >= keySet.size()) {
			return null;
		}
		return (Node) keySet.get(i);
	}
	
	public NodeSet getValue(int i) {
		
		if (i < 0 || i >= valueSet.size()) {
			return null;
		}
		return (NodeSet) valueSet.get(i);
	}
	
/*	public Iterator iterator() {
		
		return map.keySet().iterator();
	}*/
	
	public void put(Node node, NodeSet set1) {
		
		int i = keySet.indexOf(node);
		if (i != -1) {
			NodeSet set = getValue(i);
			set.clear();
			set.add(set1);
		} else {
			keySet.add(node);
			valueSet.add(set1);
		}
	//	map.put(node, set);
	/*	if (containsKey(node)) {
			NodeSet set = get(node);
			set.clear();
			set.add(set1);
		} else {
			keySet.add(node);
			map.put(node, set1);
		}*/
	}
	
	public void remove(Node node, Node node1) {
		
		int i = keySet.indexOf(node);
		if (i != -1) {
			NodeSet set = getValue(i);
			set.remove(node1);
		}
	}
	
	//1/16/2008:Martin
	public boolean removeAll(Node node){
		boolean result = false;
		int i = keySet.indexOf(node);
		if(i != -1){
			valueSet.remove(i);
			keySet.remove(i);
			result = true;
		}
		return result;
	}
	
	public int keySize() {
		
	//	return map.size();
		return keySet.size();
	}
	
	public int size() {
		
		int result = 0;
		for (int i = 0; i < keySize(); i++) {
			result = result + getValue(i).size();
		}
		return result;
	}
	
	public String toString() {
		
		String result = "{";
		for (int i = 0; i < keySize(); i++) {
			result = result + getKey(i) + "=" + getValue(i) + ", ";
		}
		result = result + "}";
		return result;
	//	return map.toString();
	}

}
