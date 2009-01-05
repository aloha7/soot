package ccr.stat;

import java.util.*;

public class Criterion implements Cloneable {
	
	private NodeMap DUMap;
	private NodeSet nodes;
//	private Vector resolutions;
	private Vector policies;
	
	public Criterion() {
		
		DUMap = new NodeMap();
		nodes = new NodeSet();
	//	resolutions = new Vector();
		policies = new Vector();
	}
	
	public void add(Node node, Node node1) {
		
		DUMap.add(node, node1);
	}
	
	public void add(Node node, NodeSet set) {
		
		if (!set.isEmpty()) {
			DUMap.add(node, set);
		}
	}
	
	public void add(NodeSet set, Node node) {
		
		for (int i = 0; i < set.size(); i++) {
			add(set.get(i), node);
		}
	}
	
	public void add(NodeSet set, NodeSet set1) {
		
		for (int i = 0; i < set.size(); i++) {
			add(set.get(i), set1);
		}
	}
	
	public void add(Node node) {
		
		nodes.add(node);
	}
	
	public void add(NodeSet set) {
		
		nodes.add(set);
	}
	
	public void add(Policy policy) {
		
		if (!policies.contains(policy)) {
			policies.add(policy);
		}
	}
	
	public void add(Resolution resolution) {
		
	//	if (!resolutions.contains(resolution)) {
	//		resolutions.add(resolution);
	//	}
		for (int i = 0; i < resolution.size(); i++) {
			add(resolution.get(i));
		}
	}
	
	public void add(Criterion criterion) {
		
		DUMap.add(criterion.DUMap);
		nodes.add(criterion.nodes);
	//	resolutions.addAll(criterion.resolutions);
		for (int i = 0; i < criterion.policies.size(); i++) {
			add((Policy) criterion.policies.get(i));
		}
	}
	
	public Object clone() {
		
		try {
			Criterion criterion = (Criterion) super.clone();
			criterion.DUMap = (NodeMap) this.DUMap.clone();
			criterion.nodes = (NodeSet) this.nodes.clone();
		//	criterion.resolutions = (Vector) this.resolutions.clone();
			criterion.policies = (Vector) this.policies.clone();
			return criterion;
		} catch (CloneNotSupportedException e) {
			throw new InternalError();
		}
	}
	
	//1/16/2008:Martin
	public Object cloneWithoutNodeMap() {
		
		try {
			Criterion criterion = (Criterion) super.clone();
			criterion.DUMap = new NodeMap();
			criterion.nodes = (NodeSet) this.nodes.clone();		
			criterion.policies = (Vector) this.policies.clone();
			return criterion;
		} catch (CloneNotSupportedException e) {
			throw new InternalError();
		}
	}
	
	public boolean containsDefinition(Node node) {
		
		return DUMap.containsKey(node) && !DUMap.get(node).isEmpty();
	}
	
	public boolean containsNode(Node node) {
		
		return nodes.contains(node);
	}
	
	public boolean containsPolicy(Policy policy) {
		
		return policies.contains(policy);
	}
	
	public boolean remove(Node node, Node node1) {
		
		boolean result = false;
		if (DUMap.containsAssociation(node, node1)) {
			DUMap.remove(node, node1);
			result = true;
		}
		return result;
	}
	
	//1/16/2008:Martin
	public boolean removeAll(Node node) {
						
		return DUMap.removeAll(node);
	}
	
	//1/16/2008:Martin
	public NodeMap getDuMaps(){
		return this.DUMap;
	}
	
	public TreeMap getDUMap() {
		
		TreeMap result = new TreeMap();
		for (int i = 0; i < DUMap.keySize(); i++) {
			Node node = DUMap.getKey(i);
			result.put(node.index, getNodes(DUMap.get(node)));
		}
		return result;
	}
	
	public boolean remove(Node node) {
		
		boolean result = false;
		if (nodes.contains(node)) {
			nodes.remove(node);
			result = true;
		}
		return result;
	}
	
	public boolean remove(Policy policy) {
		
		boolean result = false;
		if (policies.contains(policy)) {
			policies.remove(policy);
			result = true;
		}
		return result;
	}
	
	
	
	public TreeSet getNodes() {
		
		return getNodes(nodes);
	}
	
	private TreeSet getNodes(NodeSet set) {
		
		TreeSet result = new TreeSet();
		for (int i = 0; i < set.size(); i++) {
			result.add(set.get(i).index);
		}
		return result;
	}
	
	public TreeSet getPolicies() {
		
		TreeSet result = new TreeSet();
	//	for (int i = 0; i < resolutions.size(); i++) {
	//		Resolution resolution = (Resolution) resolutions.get(i);
	//		for (int j = 0; j < resolution.size(); j++) {
	//			result.add(resolution.get(j).index);
	//		}
	//	}
		for (int i = 0; i < policies.size(); i++) {
			result.add(((Policy) policies.get(i)).index);
		}
		return result;
	}
	
	public Criterion normalize(EdgeSet E) {
		
		for (int i = 0; i < DUMap.keySize(); i++) {
			Node defNode = DUMap.getKey(i);
			NodeSet set = new NodeSet();
			for (int j = 0; j < DUMap.getValue(i).size(); j++) {
				Node useNode = DUMap.getValue(i).get(j);
				if (E.getSucc(useNode).size() > 1/* && !defNode.hasSameDef(useNode)*/) {
					set.add(E.getSucc(useNode));
				} else {
					set.add(useNode);
				}
			}
			DUMap.put(defNode, set);
		}
		return this;
	}
	
	public int size() {
		
	//	int policySize = 0;
	//	for (int i = 0; i < resolutions.size(); i++) {
	//		policySize = policySize + ((Resolution) resolutions.get(i)).size();
	//	}
	//	return DUMap.size() + nodes.size() + policySize;
		return DUMap.size() + nodes.size() + policies.size();
	}
	
	public String toString() {
		
		return getDUMap() + " " + getNodes() + " " + getPolicies();
	}

}
