package context.test.contextIntensity;

public class Edge {
	
	public final Node sourNode, destNode;
	public boolean traversed = false;
	public Edge(Node s, Node d) {
		
		sourNode = s;
		destNode = d;
	}
	
	public boolean equals(Edge edge) {
		
		return sourNode == edge.sourNode && destNode == edge.destNode;
	}
	
	public String toString() {
		
		return "(" + sourNode.toString() + ", " + destNode.toString() + "," +traversed +")";
	}

}
