package ccr.stat;

import java.util.*;

public class NodeIndex {
	
	private TreeMap record = null;
	private static NodeIndex singleton = null;
	
	public static NodeIndex getInstance() {
		
		if (singleton == null) {
			singleton = new NodeIndex();
		}
		return singleton;
	}
	
	private NodeIndex() {
		
		record = new TreeMap();
	}
	
	public void add(Node node) {
		
		if (!record.containsKey(node.index)) {
			record.put(node.index, node);
		}
	}
	
	public Node get(String index) {
		
		return (Node) record.get(index);
	}
	
	public boolean has(String index) {
		
		return record.containsKey(index);
	}
	
	public Node process(Node node) {
		
		if (has(node.index)) {
			return get(node.index);
		}
		add(node);
		return node;
	}
	
	public String toString() {
		
		return record.toString();
	}

}
