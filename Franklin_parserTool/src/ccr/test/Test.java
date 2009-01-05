package ccr.test;

import ccr.stat.*;
import java.util.*;

public class Test {
	
	public static void main(String argv[]) {
		
	/*	Vector a = new Vector();
		a.add("a1");
		Vector b = (Vector) a.clone();
		b.add("b1");
		System.out.println("a: "+a);
		System.out.println("b: "+b);*/
		
		HashMap c = new HashMap();
		c.put("c1", "c2");
		HashMap d = (HashMap) c.clone();
		d.put("c1", "d2");
		System.out.println("c: "+c);
		System.out.println("d: "+d);
		
		Node node1 = new Node("1", "node1statement", new VariableSet(), new VariableSet());
		Node node2 = new Node("1", "node1statement", new VariableSet(), new VariableSet());
	//	Node node3 = new Node("1", "node1statement", new VariableSet(), new VariableSet());
		System.out.println("" + node1.equals(node2));
		NodeSet set = (new NodeSet()).add(node1);
		System.out.println("" + set.contains(node2));
		NodeMap map1 = new NodeMap();
		map1.add(node1, set);
		System.out.println("" + map1.containsKey(node2));
	//	HashMap map2 = new HashMap();
		TreeMap map2 = new TreeMap();
		map2.put(node1, set);
		System.out.println("" + map2.containsKey(node2));
	/*	map2.put(node2, set);
		map2.put(node3, set);
		String s1 = new String("1");
		String s2 = new String("1");
		map2.put(s1, set);
		map2.put(s2, set);
		System.out.println(map2.keySet());
		System.out.println("" + map2.keySet().contains(node2));*/
	}

}
