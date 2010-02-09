package dua.method;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import dua.Options;
import dua.method.CFG.CFGNode;
import dua.unit.StmtTag;

public class CFGAnalysis {
	private static class EdgeWeightReverseComparator implements Comparator<AbstractEdge> {
		private Map<AbstractEdge,Float> weights;
		public EdgeWeightReverseComparator(Map<AbstractEdge,Float> weights) { this.weights = weights; }
		public int compare(AbstractEdge o1, AbstractEdge o2) {
			final float w1 = weights.get(o1);
			final float w2 = weights.get(o2);
			return (w1 > w2)? -1 : ((w1 == w2)? 0 : 1); // e1 < e2 iff w1 > w2; e1 > e2 iff w1 < w2
		}
	}
	
	/**
	 * A backedge in a DFS traversal is an out edge of the node being visited, whose tgt was already visited during DFS so far.
	 * Does not add EXIT->ENTRY special edge as backedge.
	 */
	public static void dfsFindBackedges(CFGNode n, CFGNode nExit, Map<CFGNode, ArrayList<Edge>> outEdges, HashSet<CFGNode> visitedNodes, HashSet<CFGNode> ancestors, HashSet<Edge> backedges) {
		assert !visitedNodes.contains(n);
		visitedNodes.add(n);
		
		if (n == nExit)  // EXIT node -- don't continue
			return;
		
		ancestors.add(n);
		for (Edge eOut : outEdges.get(n)) {
			if (ancestors.contains(eOut.getTgt()))
				backedges.add(eOut);
			else if (!visitedNodes.contains(eOut.getTgt()))
				dfsFindBackedges(eOut.getTgt(), nExit, outEdges, visitedNodes, ancestors, backedges);
		}
		ancestors.remove(n);
	}
	
	/**
	 * Recursively builds list of nodes in DAG topological order (i.e., every node appears before its successors).
	 * Does not include EXIT node.
	 */
	public static void insertDFSTopologically(CFGNode n, CFGNode nExit, Map<CFGNode, ArrayList<Edge>> outEdges, Collection<Edge> backedges, LinkedList<CFGNode> orderedNodes) {
		if (n == nExit)  // skip EXIT node
			return;
		
		// insert children, first
		for (Edge eOut : outEdges.get(n)) {
			if (!backedges.contains(eOut) && !orderedNodes.contains(eOut.getTgt()))
				insertDFSTopologically(eOut.getTgt(), nExit, outEdges, backedges, orderedNodes);
		}
		assert !orderedNodes.contains(n);
		
		// find min idx of nodes that are targets of some out-edge of n in DAG
		int minTgtIdx = orderedNodes.size();
		for (Edge eOut : outEdges.get(n)) {
			if (!backedges.contains(eOut)) {
				final int tgtPos = orderedNodes.indexOf(eOut.getTgt());
				if (tgtPos != -1 && tgtPos < minTgtIdx)
					minTgtIdx = tgtPos;
			}
		}
		
		// place n at position just before all of it's DAG out-edge targets
		if (minTgtIdx == orderedNodes.size())
			orderedNodes.add(n); // avoid index-out-of-bounds
		else
			orderedNodes.add(minTgtIdx, n);
	}
	
	/**
	 * Returns a new list of edges, in decreasing weight order according to settings (max-weight, or random).
	 * Edges not in in/out maps are implicitly assigned minimum weight (0).
	 */
	public static ArrayList<Edge> getWeightOrderEdges(CFG cfg, Map<CFGNode, ArrayList<Edge>> inEdges, Map<CFGNode, ArrayList<Edge>> outEdges, ArrayList<Edge> edges) {
		// get edges in proper order
		ArrayList<Edge> orderedEdges = (ArrayList<Edge>) edges.clone();
		if (Options.edgeWeighting()) {
			// use BL94 heuristic to assign weight to edges
			Map<AbstractEdge,Float> edgeWeights = assignWeights(cfg, inEdges, outEdges);
			// complete weight map with weights for edges not in in/out maps
			HashSet<AbstractEdge> weightedEdgesSet = new HashSet<AbstractEdge>(edgeWeights.keySet());
			for (AbstractEdge e : edges)
				if (!weightedEdgesSet.contains(e))
					edgeWeights.put(e, 0f);
			
			// sort edges according to weight, in descending order
			Collections.sort(orderedEdges, new EdgeWeightReverseComparator(edgeWeights));
		}
		// else, leave list as is, that is, a "random" order list
		
		return orderedEdges;
	}
	
	/** Builds a maximal or random spanning tree, according to settings (provided edge order) */
	public static HashSet<Edge> buildSpanningTree(CFGNode nEntry, CFGNode nExit, ArrayList<Edge> orderedEdges)
	{
		// Kruskal's algorithm for max-weight spanning tree
		HashSet<Edge> spanTreeEdges = new HashSet<Edge>();
		
		// assign unique ids to nodes, reserving 0 for EX and 1 for EN
		HashMap<CFGNode, Integer> nodeIds = new HashMap<CFGNode, Integer>();
		nodeIds.put(nExit, 0); // 0 is reserved for EXIT
		nodeIds.put(nEntry, 1); // 1 is reserved for ENTRY
		int nId = 2;
		for (AbstractEdge e : orderedEdges) {
			if (!nodeIds.containsKey(e.getSrc()))
				nodeIds.put(e.getSrc(), nId++);
			if (!nodeIds.containsKey(e.getTgt()))
				nodeIds.put(e.getTgt(), nId++);
		}
		
		// create initial single-node trees, giving unique tree id to each node
		int[] nodeTrees = new int[nodeIds.size()]; // node 0 is virtual EXIT
		for (int i = 0 ; i < nodeTrees.length; ++i)
			nodeTrees[i] = i;
		
		// visit edges in given order
		for (Edge e : orderedEdges) {
			final int srcId = nodeIds.get(e.getSrc()); // 0 is reserved for EXIT
			final int tgtId = nodeIds.get(e.getTgt()); // 0 is reserved for EXIT
			final int srcTreeId = nodeTrees[srcId];
			final int tgtTreeId = nodeTrees[tgtId];
			if (srcTreeId != tgtTreeId) {
				// edge becomes part of spanning tree
				spanTreeEdges.add(e);
				// join node sets (separate trees), taking id of src tree
				for (int i = 0 ; i < nodeTrees.length; ++i)
					if (nodeTrees[i] == tgtTreeId)
						nodeTrees[i] = srcTreeId;
			}
			
			// sanity check: # of different ids
			HashSet<Integer> idsFound = new HashSet<Integer>();
			for (int i = 0; i < nodeTrees.length; ++i)
				idsFound.add(new Integer(nodeTrees[i]));
			assert nodeTrees.length - idsFound.size() == spanTreeEdges.size();
		}
		
		assert spanTreeEdges.size() == nodeIds.size() - 1;
		return spanTreeEdges;
	}
	
	/** Assumes that first edge is EXIT->root */
	private static Map<AbstractEdge,Float> assignWeights(CFG cfg, Map<CFGNode, ArrayList<Edge>> inEdges, Map<CFGNode, ArrayList<Edge>> outEdges) {
		// 1. DFS to find 'backedges' (might be imprecise if it finds cross-edges)
		Edge eExitToRoot = outEdges.get(cfg.EXIT).get(0); // EXIT->root edge
		CFGNode nRoot = eExitToRoot.getTgt();
		HashSet<CFGNode> visitedNodes = new HashSet<CFGNode>();
		HashSet<Edge> backedges = new HashSet<Edge>();
		dfsFindBackedges(nRoot, cfg.EXIT, outEdges, visitedNodes, new HashSet<CFGNode>(), backedges); // start DFS from root
		
		// 2. Find loop exit edges, for each loop entry node
		
		// 2.1 find natural loops for loop heads, using backedges
		HashMap<CFGNode, HashSet<CFGNode>> natLoops = new HashMap<CFGNode, HashSet<CFGNode>>();
		findNaturalLoops(cfg.EXIT, backedges, outEdges, natLoops);
		
		// 2.2 find exits of nat-loops
		//     when an exit is associated with more than one loop, the outermost loop prevails
		HashMap<CFGNode, ArrayList<Edge>> natLoopExits = new HashMap<CFGNode, ArrayList<Edge>>();
		HashSet<Edge> allLoopExits = new HashSet<Edge>(); // collection of all edges that are exits of some loop
		HashMap<Edge, CFGNode> loopForExit = new HashMap<Edge, CFGNode>();
		for (CFGNode loopHead : natLoops.keySet()) {
			ArrayList<Edge> exits = new ArrayList<Edge>();
			HashSet<CFGNode> natLoop = natLoops.get(loopHead);
			for (CFGNode nInLoop : natLoop) {
				for (Edge eOut : outEdges.get(nInLoop)) {
					if (!natLoop.contains(eOut.getTgt())) {
						// this edge is an exit for loop
						// however, we need to check previous ownership of another loop, first
						CFGNode nPrevOwner = loopForExit.get(eOut);
						boolean add = true;
						if (nPrevOwner != null) {
							if (natLoop.contains(nPrevOwner)) {
								natLoopExits.get(nPrevOwner).remove(eOut); // remove ownership by previous owner
								allLoopExits.remove(eOut); // just to do things in proper order, and help safety check below
							}
							else {
								add = false;
								assert natLoops.get(nPrevOwner).contains(loopHead); // ensure the other loop actually encloses this
							}
						}
						
						if (add) {
							exits.add(eOut);
							loopForExit.put(eOut, loopHead);
						}
					}
				}
			}
			natLoopExits.put(loopHead, exits);
			
			final int prevSize = allLoopExits.size();
			allLoopExits.addAll(exits);
			assert allLoopExits.size() == prevSize + exits.size();
		}
		
		// 3. Topological traversal of DAG, to assign weights to vertices and edges
		HashMap<AbstractEdge,Float> edgeWeights = new HashMap<AbstractEdge,Float>();
		edgeWeights.put(eExitToRoot, 1.0f); // EXIT->root edge weights 1
		visitedNodes.clear();
		topologicallyWeight(cfg, inEdges, outEdges, backedges, natLoopExits, allLoopExits, edgeWeights);
		
		// sanity check: warn if weight sum of edges to EXIT is not 1.0
		float weightsToExit = 0.0f;
		for (AbstractEdge e : edgeWeights.keySet()) {
			if (e instanceof Edge && ((Edge)e).getTgt() == cfg.EXIT)
				weightsToExit += edgeWeights.get(e);
		}
		if (Math.abs(weightsToExit - 1.0f) > 0.001f)
			System.out.println("WARNING: weight sum of edges to EXIT is " + weightsToExit);
		
		return edgeWeights;
	}
	
	/** For each backedge, find and store set of nodes that belong to its natural loop */
	private static void findNaturalLoops(CFGNode nExit, Collection<Edge> backedges, Map<CFGNode, ArrayList<Edge>> outEdges, HashMap<CFGNode, HashSet<CFGNode>> natLoops) {
		// ensure method's local stmt reachability is already computed
		assert ((StmtTag)outEdges.get(nExit).get(0).getTgt().getSuccs().get(0).s.getTag(StmtTag.TAG_NAME)).isLocalReachabilityComputed();
		
		// traverse backedges to find nat loop of each loop head
		for (Edge backedge : backedges) {
			// if not created yet, create set where we will store nodes corresponding to nat-loop of loop head (backedge's tgt)
			CFGNode loopHead = backedge.getTgt();
			HashSet<CFGNode> natLoop = natLoops.get(loopHead);
			if (natLoop == null) {
				natLoop = new HashSet<CFGNode>();
				natLoops.put(loopHead, natLoop);
			}
			
			// ASSUMPTION: only one natural loop per loop head exists
			// loop consists of all nodes reachable from the loop head that reach the backedge without going through the loop head
			assert backedge.getSrc() != nExit && backedge.getTgt() != nExit;
			findLoop(nExit, loopHead, loopHead, backedge, outEdges, new HashSet<CFGNode>(), natLoop);
		}
	}
	
	/**
	 * Straightforward algorithm, probably inefficient. Traverses all paths from n, looking for backedge without going through loop head.
	 * All nodes in such paths that reach the backedge become part of the loop.
	 * @return true if backedge is reachable from n without going through loop head
	 */
	private static boolean findLoop(CFGNode nExit, CFGNode n, CFGNode loopHead, Edge backedge, Map<CFGNode, ArrayList<Edge>> outEdges, HashSet<CFGNode> visited, HashSet<CFGNode> natLoop) {
		// start assuming that n does not reach backedge
		boolean reachedBE = false;
		
		// find path that leads to backedge (avoiding loop head)
		for (Edge eOut : outEdges.get(n)) {
			if (eOut == backedge) // reached backedge from n (and from predecessors that recursively called this method)
				reachedBE = true;
			else {
				CFGNode nSucc = eOut.getTgt();
				if (nSucc != nExit && nSucc != loopHead && !visited.contains(nSucc)) {  // path doesn't continue through loop head, and we haven't visited this node on current path
					visited.add(nSucc);
					if (findLoop(nExit, nSucc, loopHead, backedge, outEdges, visited, natLoop))
						reachedBE = true;
					visited.remove(nSucc);
				}
			}
		}
		
		// process result (i.e., found or not)
		if (reachedBE)
			natLoop.add(n);
		return reachedBE;
	}
	
	/**
	 * Topological traversal of DAG, to assign weights to vertices and edges.
	 * Assumes that the given in/out edges, minus the EXIT->root edge and the backedges, constitute a DAG.
	 */
	private static void topologicallyWeight(CFG cfg, Map<CFGNode, ArrayList<Edge>> inEdges, Map<CFGNode, ArrayList<Edge>> outEdges, Collection<Edge> backedges, 
			Map<CFGNode, ArrayList<Edge>> natLoopExits, Set<Edge> allLoopExits, HashMap<AbstractEdge,Float> edgeWeights)
	{
		// create topologically ordered list of nodes
		// -- does not containt EXIT
		LinkedList<CFGNode> orderedNodes = new LinkedList<CFGNode>();
		CFGNode nRoot = outEdges.get(cfg.EXIT).get(0).getTgt();
		insertDFSTopologically(nRoot, cfg.EXIT, outEdges, backedges, orderedNodes);
		
		// check correctness
		for (CFGNode n : outEdges.keySet()) {
			if (n == cfg.EXIT)
				continue;
			
			final int nPos = orderedNodes.indexOf(n);
			assert nPos >= 0;
			for (Edge eOut : outEdges.get(n)) {
				if (!backedges.contains(eOut) && eOut.getTgt() != cfg.EXIT) {
					final int tgtPos = orderedNodes.indexOf(eOut.getTgt());
					assert nPos < tgtPos;
				}
			}
		}
		
		// compute weights of vertices and edges using BL94's weighting heuristic
		HashMap<CFGNode,Float> vertexWeights = new HashMap<CFGNode,Float>();
		for (CFGNode n : orderedNodes) {
			// BL94: Section 5, Rule 1 -- compute vertex's weight
			// vertex n's weight is the sum of incoming edges that are not backedges
			// (EXIT->root is a valid incoming edge for this purpose)
			float weightNode = 0;
			for (Edge eIn : inEdges.get(n)) {
				if (!backedges.contains(eIn))
					weightNode += edgeWeights.get(eIn);
			}
			assert !vertexWeights.containsKey(n);
			vertexWeights.put(n, weightNode);
			
			if (n == cfg.EXIT)  // EXIT node; don't continue
				return;
			
			// BL94: Section 5, Rule 2 -- compute weights of loop exit edges associated to n, if n is a loop head
			ArrayList<Edge> loopExits = natLoopExits.get(n);
			ArrayList<Edge> nodeOutEdges = outEdges.get(n);
			if (loopExits != null && !loopExits.isEmpty()) {
				final float exitWeight = weightNode / loopExits.size();
				for (Edge eLoopExit : loopExits) {
					assert !edgeWeights.containsKey(eLoopExit);
					edgeWeights.put(eLoopExit, exitWeight);
				}
			}
			
			// BL94: Section 5, Rule 3 -- compute weights of non-loop-exit edges outgoing from n
			float weightOutLoopExits = 0; // also compute W_e for rule 3
			int numOutLoopExits = 0;
			for (Edge eOut : nodeOutEdges) {
				if (allLoopExits.contains(eOut)) {
					weightOutLoopExits += edgeWeights.get(eOut);
					++numOutLoopExits;
				}
			}
			final int LOOP_MULT = 10;
			final float weightOutNonLoopExit = (((loopExits == null)? weightNode : weightNode * LOOP_MULT) - weightOutLoopExits) / (nodeOutEdges.size() - numOutLoopExits);
			for (Edge eOut : nodeOutEdges) {
				if (!allLoopExits.contains(eOut)) {
					assert !edgeWeights.containsKey(eOut);
					edgeWeights.put(eOut, weightOutNonLoopExit);
				}
			}
		}
	}
	
}
