/**
 * 
 */
package dua.method;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;

import dua.method.CFG.CFGNode;

public final class Edge extends AbstractEdge {
	/** Orders by Src node id first, and Tgt node id second */
	public static class EdgeComparator implements Comparator<Edge> {
		public int compare(Edge e1, Edge e2) {
			final int srcId1 = e1.getSrc().getIdInMethod();
			final int srcId2 = e2.getSrc().getIdInMethod();
			if (srcId1 < srcId2)
				return -1;
			else if (srcId1 > srcId2)
				return 1;
			else {
				final int tgtId1 = e1.getTgt().getIdInMethod();
				final int tgtId2 = e2.getTgt().getIdInMethod();
				if (tgtId1 < tgtId2)
					return -1;
				else if (tgtId1 > tgtId2)
					return 1;
				else
					return 0;
			}
		}
	}
	
	private final CFGNode tgt;
	//@ invariant src != null && tgt != null;
	
	@Override
	public CFGNode getTgt() { return tgt; }
	
	/** Source can be null, but target must not */
	public Edge(CFGNode src, CFGNode tgt) {
		super(src);
		
		assert tgt != null; // pre-condition
		this.tgt = tgt;
	}
	
	/** Whether src falls through to tgt in original program */
	public boolean fallsThrough() { return src.getFallThroughTgt() == tgt; }
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Edge))
			return false;
		Edge other = (Edge)obj;
		return this.src == other.src && this.tgt == other.tgt;
	}
	@Override
	public int hashCode() { return ((src == null)? 0 : src.hashCode()) + tgt.hashCode(); }
	
	@Override
	public String toString() {
		return src.getIdStringInMethod() + "-" + tgt.getIdStringInMethod();
	}
	
	/**
	 * Creates edges for given CFG (method), including maps of in/out edges per node.
	 * Includes EXIT->ENTRY special edge.
	 * Position in returned list is implicit global ID of an edge.
	 */
	public static ArrayList<Edge> createEdges(CFG cfg, Map<CFGNode,ArrayList<Edge>> inEdges, Map<CFGNode,ArrayList<Edge>> outEdges) {
		ArrayList<Edge> allEdges = new ArrayList<Edge>();
		
		// create edge EX->EN, first, and make sure it's processed first in max span tree phase
		createEdge(cfg.EXIT, cfg.ENTRY, allEdges, inEdges, outEdges);
		
		// create edges for CFG
		for (CFGNode n : cfg.getNodes()) {
			if (n.isInCatchBlock())
				continue;
			
			// create one edge from n to each successor
			for (CFGNode nSucc : n.getSuccs()) {
				assert !nSucc.isInCatchBlock();
				createEdge(n, nSucc, allEdges, inEdges, outEdges);
			}
			// End nodes (throws) must have a one-elem out edges list {(n->EX)} rather than a null out list
			if (!outEdges.containsKey(n)) {
				assert n != cfg.EXIT;
				createEdge(n, cfg.EXIT, allEdges, inEdges, outEdges);
			}
		}
		
		return allEdges;
	}
	
	public static Edge createEdge(CFGNode nSrc, CFGNode nTgt, ArrayList<Edge> allEdges, Map<CFGNode, ArrayList<Edge>> inEdges, Map<CFGNode, ArrayList<Edge>> outEdges) {
		Edge e = new Edge(nSrc, nTgt);
		allEdges.add(e);
		
		ArrayList<Edge> inList = inEdges.get(nTgt);
		if (inList == null) {
			inList = new ArrayList<Edge>();
			inEdges.put(nTgt, inList);
		}
		inList.add(e);
		
		ArrayList<Edge> outList = outEdges.get(nSrc);
		if (outList == null) {
			outList = new ArrayList<Edge>();
			outEdges.put(nSrc, outList);
		}
		outList.add(e);
		
		return e;
	}
	
}