package dua.method;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import soot.SootMethod;
import dua.global.ProgramFlowGraph;
import dua.method.CFG.CFGNode;
import dua.util.Pair;
import dua.util.Util;

/**
 * Analysis for finding the Ball-Larus (BL) paths and optimal edge increments in a single CFG.
 * Algorithm was extended for interprocedural BL paths (left-right only), with a client-specificed call depth.
 * Also provides intraprocedural path-path edge (predecessor-successor) computation.
 */
public class EPPAnalysis {
	private CFG cfg;
	private ArrayList<Edge> regularEdges;
	private HashMap<CFGNode, ArrayList<Edge>> inEdges; // contains ALL edges, including backedges and dummy edges
	private HashMap<CFGNode, ArrayList<Edge>> outEdges; // contains ALL edges, including backedges and dummy edges
	private HashMap<CFGNode, ArrayList<Edge>> inNoDummyEdges;
	private HashMap<CFGNode, ArrayList<Edge>> outNoDummyEdges;
	private HashSet<Edge> backedges;
	private HashSet<Edge> dummyEdgesFromEntry;
	private HashSet<Edge> dummyEdgesToExit;
	private HashMap<CFGNode, CallEdge> callEdges = new HashMap<CFGNode, CallEdge>();
	private LinkedList<CFGNode> orderedNodes = null;
	private Map<CFG, EPPAnalysis> cfgEPPAnalyses = null;
	private ArrayList<HashMap<CFGNode, Integer>> numPathsPerDepth = new ArrayList<HashMap<CFGNode,Integer>>(); // list ordered per depth, starting with depth 0 (which is intra only)
	private ArrayList<HashMap<AbstractEdge,Integer>> edgeValuesPerDepth = new ArrayList<HashMap<AbstractEdge,Integer>>(); // list ordered per depth, starting with depth 0 (which is intra only)
	private ArrayList<HashMap<AbstractEdge, Integer>> optEdgeValues; // list ordered per depth, starting with depth 0 (which is intra only)
	
	public ArrayList<Edge> getRegularEdges() { return regularEdges; }
	public Collection<CallEdge> getCallEdges() { return callEdges.values(); }
	public HashMap<CFGNode, ArrayList<Edge>> getInEdges() { return inEdges; }
	public HashMap<CFGNode, ArrayList<Edge>> getOutEdges() { return outEdges; }
	public HashSet<Edge> getBackedges() { return backedges; }
	public HashSet<Edge> getDummyEdgesFromEntry() { return dummyEdgesFromEntry; }
	public HashSet<Edge> getDummyEdgesToExit() { return dummyEdgesToExit; }
	public int getMaxDepth() { assert optEdgeValues.size() == numPathsPerDepth.size(); return optEdgeValues.size(); }
	public HashMap<CFGNode, Integer> getNumPaths(int depth) { return numPathsPerDepth.get(depth); }
	public ArrayList<HashMap<AbstractEdge, Integer>> getOptEdgeValues() { return optEdgeValues; }
	
	/**
	 * Creates DAG from CFG by removing backedges and adding dummy edges from entry to loop heads and from loop ends to exit.
	 * @param cfg CFG to compute DAG from
	 * @param regularEdges Set of all edges, which will be updated when creating DAG
	 * @param inEdges Edges organized by target node; will be updated when creating DAG
	 * @param outEdges Edges organized by source node; will be updated when creating DAG
	 * @return Set of backedges found
	 */
	public HashSet<Edge> buildDAG(CFG cfg, ArrayList<Edge> regularEdges, HashMap<CFGNode, ArrayList<Edge>> inEdges, HashMap<CFGNode, ArrayList<Edge>> outEdges) {
		this.cfg = cfg;
		this.regularEdges = regularEdges;
		this.inEdges = inEdges;
		this.outEdges = outEdges;
		this.inNoDummyEdges = deepClone(inEdges); // save original edges
		this.outNoDummyEdges = deepClone(outEdges); // save original edges
		
		// 1. Identify backedges, using DFS traversal
		HashSet<CFGNode> visitedNodes = new HashSet<CFGNode>();
		backedges = new HashSet<Edge>();
		CFGAnalysis.dfsFindBackedges(cfg.ENTRY, cfg.EXIT, outEdges, visitedNodes, new HashSet<CFGNode>(), backedges); // starts DFS from root; does not consider EXIT->ENTRY
		
		// 2. For each loop head v, add (only one) ENTRY->v dummy edge. For each backedge src w, add (only one) w->EXIT edges; henceforth, backedges are not considered part of DAG
		//     -- note that we might end up with more than one edge between two nodes
		// collect loop heads and backedge srcs, without repetition
		HashSet<CFGNode> loopHeads = new HashSet<CFGNode>();
		HashSet<CFGNode> beSrcs = new HashSet<CFGNode>();
		for (Edge be : backedges) {
			loopHeads.add(be.getTgt());
			beSrcs.add(be.getSrc());
		}
		// create for each loop head one (and only one) dummy edge from ENTRY to loop head
		dummyEdgesFromEntry = new HashSet<Edge>();
		for (CFGNode n : loopHeads) {
			Edge eFromEntry = Edge.createEdge(cfg.ENTRY, n, regularEdges, inEdges, outEdges); // ENTRY->src
			dummyEdgesFromEntry.add(eFromEntry);
		}
		// create for each backedge src one (and only one) dummy edge from backedge src to EXIT
		dummyEdgesToExit = new HashSet<Edge>();
		for (CFGNode n : beSrcs) {
			Edge eToExit = Edge.createEdge(n, cfg.EXIT, regularEdges, inEdges, outEdges); // tgt->EXIT
			dummyEdgesToExit.add(eToExit);
		}
		
		return backedges;
	}
	
	/** Duplicates arrays as well as map */
	private static HashMap<CFGNode, ArrayList<Edge>> deepClone(HashMap<CFGNode, ArrayList<Edge>> edgeMap) {
		HashMap<CFGNode, ArrayList<Edge>> newEdgeMap = new HashMap<CFGNode, ArrayList<Edge>>();
		for (CFGNode n : edgeMap.keySet()) {
			ArrayList<Edge> edgesCopy = (ArrayList<Edge>) edgeMap.get(n).clone();
			newEdgeMap.put(n, edgesCopy);
		}
		
		return newEdgeMap;
	}
	
	/**
	 * @param depth Remaining depth for paths entering this method or starting at this method
	 * @return Number of acyclic (Ball-Larus) paths from each CFG node to the CFG's exit.
	 */
	public HashMap<CFGNode,Integer> computeNumAcyclicPaths(int depth, Map<CFG, EPPAnalysis> _cfgEPPAnalyses) {
		cfgEPPAnalyses = _cfgEPPAnalyses; // store for later
		
		// 1. Ensure creation of list of nodes (excluding EXIT) in reverse topological order
		createRevTopOrderNodeList();
		
		// 2. Compute NumPaths for each node and Value for each DAG edge (BL96, algorithm in Figure 5)
		//    Reverse topological order guarantees that we have computed NumPaths for DAG successors of n before visiting n
		assert depth == numPathsPerDepth.size();
		HashMap<CFGNode, Integer> numPaths = new HashMap<CFGNode, Integer>();
		numPathsPerDepth.add(numPaths);
		
		numPaths.put(cfg.EXIT, 1); // include EXIT node in map; trivially, there is one path from EXIT to itself
		HashMap<AbstractEdge, Integer> edgeValues = new HashMap<AbstractEdge, Integer>();
		edgeValuesPerDepth.add(edgeValues);
		
		edgeValues.put(outEdges.get(cfg.EXIT).get(0), 0); // include EXIT->ENTRY edge, with value 0
		for (CFGNode n : orderedNodes) {
			// init path count
			assert !n.isInCatchBlock();
			int paths = 0;
			// count paths for intra-cfg edges first: 
			// Val(e) = Numpaths(v); Numpaths(v) += Numpaths(e.tgt)
			for (Edge eOut : outEdges.get(n)) {
				if (!backedges.contains(eOut)) {
					edgeValues.put(eOut, paths);
					paths += numPaths.get(eOut.getTgt());
				}
			}
			// then count paths for callees, if this is a call node and depth > 0
			// link to each callee is treated as a special edge from node to EXIT, since we are only interested in "left-right" paths
			if (depth > 0 && n.hasAppCallees()) {
				CallSite cs = n.getAppCallSite();
				for (SootMethod mCallee : cs.getAppCallees()) {
					CFG cfgCallee = ProgramFlowGraph.inst().getCFG(mCallee);
					EPPAnalysis eppAnalysisCallee = cfgEPPAnalyses.get(cfgCallee);
					HashMap<CFGNode,Integer> numPathsCallee = eppAnalysisCallee.getNumPaths(depth - 1);
					edgeValues.put(getCreateCallEdge(n), paths);
					paths += numPathsCallee.get(cfgCallee.ENTRY); // num paths for method is num paths for entry node, at depth - 1
				}
			}
			
			// store num paths for this node
			numPaths.put(n, paths);
		}
		
		return numPaths;
	}
	
	/** Call edge is a virtual edge from call site node to exit */
	private CallEdge getCreateCallEdge(CFGNode n) {
		CallEdge e = callEdges.get(n);
		if (e == null) {
			e = new CallEdge(n);
			callEdges.put(n, e);
		}
		
		return e;
	}
	
	/** Creates list of nodes (excluding EXIT) in reverse topological order, storing it in 'orderedNodes' field */
	private void createRevTopOrderNodeList() {
		if (orderedNodes != null)  // avoid re-computation
			return;
		
		orderedNodes = new LinkedList<CFGNode>();
		CFGAnalysis.insertDFSTopologically(cfg.ENTRY, cfg.EXIT, outEdges, backedges, orderedNodes); // list without EXIT node
		Collections.reverse(orderedNodes);
		
		// DEBUG - verify that node DAG successors precede node in list
		for (CFGNode n : cfg.getNodes()) {
			if (n == cfg.EXIT)
				continue;
			if (n.isInCatchBlock())
				continue;
			
			final int nPos = orderedNodes.indexOf(n);
			assert nPos >= 0;
			for (Edge eOut : outEdges.get(n)) {
				if (eOut.getTgt() != cfg.EXIT && !backedges.contains(eOut)) {
					final int nSuccPos = orderedNodes.indexOf(eOut.getTgt());
					assert nSuccPos >= 0;
					assert nSuccPos < nPos;
				}
			}
		}
	}
	
	/**
	 * @param inNoDummyEdges In edges in original CFG
	 * @param outNoDummyEdges Out edges in original CFG
	 * @param depth Call depth for which to find increments
	 * @return Optimal assignment of increments to edges
	 */
	public void findMinCostIncrements(int depth) {
		// 1. Find max-cost spanning tree in DAG, including all dummy edges
		//    Weight edges from original CFG, using in/out edge maps without dummy edges; dummy edges get min weight
		ArrayList<Edge> orderedRegularEdges = CFGAnalysis.getWeightOrderEdges(cfg, inNoDummyEdges, outNoDummyEdges, regularEdges);
		orderedRegularEdges.removeAll(backedges); // make it DAG edges only
		HashSet<Edge> spanTreeEdges = CFGAnalysis.buildSpanningTree(cfg.ENTRY, cfg.EXIT, orderedRegularEdges);
		
		// 2. For each chord (i.e., edge not in tree), find circuit it closes in tree, and compute increment
		//    First, convert tree to map node->edges, where edges are both incoming and outgoing in tree
		HashMap<CFGNode, ArrayList<Edge>> treeEdges = new HashMap<CFGNode, ArrayList<Edge>>();
		for (Edge e : spanTreeEdges) {
			ArrayList<Edge> srcNodeEdges = treeEdges.get(e.getSrc());
			if (srcNodeEdges == null) {
				srcNodeEdges = new ArrayList<Edge>();
				treeEdges.put(e.getSrc(), srcNodeEdges);
			}
			srcNodeEdges.add(e);
			
			ArrayList<Edge> tgtNodeEdges = treeEdges.get(e.getTgt());
			if (tgtNodeEdges == null) {
				tgtNodeEdges = new ArrayList<Edge>();
				treeEdges.put(e.getTgt(), tgtNodeEdges);
			}
			tgtNodeEdges.add(e);
		}
		//    Then, for each chord, find circuit and compute optimal increments, for all depths
		optEdgeValues = new ArrayList<HashMap<AbstractEdge,Integer>>();
		for (int d = 0; d <= depth; ++d) {  // instantiate optimal edge->inc maps for each depth, before proceeding
			HashMap<AbstractEdge, Integer> optDepthVals = new HashMap<AbstractEdge, Integer>();
			optEdgeValues.add(optDepthVals);
		}
		HashSet<AbstractEdge> allEdges = new HashSet<AbstractEdge>(regularEdges);
		allEdges.addAll(getCallEdges());
		for (AbstractEdge e : allEdges) {
			if (spanTreeEdges.contains(e) || backedges.contains(e))
				continue;
			ArrayList<Edge> edgesCircuit = findTreeCircuit(treeEdges, e); // resulting circuit starts with edge from chord's target
			final int startD = (e instanceof CallEdge)? 1 : 0; // call edges only exist for interproc paths (d >= 1)
			for (int d = startD; d <= depth; ++d) {
				// traverse circuit in the direction of the chord edge, starting with chord's target node
				CFGNode currNode = e.getTgt();
				HashMap<AbstractEdge, Integer> edgeValues = edgeValuesPerDepth.get(d);
				int inc = edgeValues.get(e);
				for (Edge eInCircuit : edgesCircuit) {
					if (eInCircuit.getSrc() == currNode) {
						inc += edgeValues.get(eInCircuit);
						currNode = eInCircuit.getTgt();
					}
					else {  // edge is "against the current"
						assert eInCircuit.getTgt() == currNode;
						inc -= edgeValues.get(eInCircuit);
						currNode = eInCircuit.getSrc();
					}
				}
				assert currNode == e.getSrc();
				
				if (inc != 0)
					optEdgeValues.get(d).put(e, inc);
			}
		}
	}
	
	/**
	 * Finds circuit in given spanning tree of intraproc edges (no call edges) for given chord (edge outside tree). Chord can be a call edge.
	 * @param treeEdges All edges incident to each node
	 * @param chord Edge that joins two nodes, but it's not in the tree; can be a call edge, which virtually joins src with exit (null)
	 * @return Ordered list of edges from chord tgt, excluding chord, in the unique tree circuit for chord
	 */
	private static ArrayList<Edge> findTreeCircuit(HashMap<CFGNode, ArrayList<Edge>> treeEdges, AbstractEdge chord) {
		// DFS from chord tgt to chord src
		HashSet<CFGNode> visited = new HashSet<CFGNode>();
		ArrayList<Edge> edgeList = new ArrayList<Edge>();
		final boolean found = findTreeNodeDFS(chord.getTgt(), treeEdges, chord.getSrc(), visited, edgeList);
		assert found;
		
		return edgeList;
	}
	private static boolean findTreeNodeDFS(CFGNode n, HashMap<CFGNode, ArrayList<Edge>> treeEdges, CFGNode nToFind, HashSet<CFGNode> visited, ArrayList<Edge> edgeList) {
		// see if we found target node
		if (n == nToFind)
			return true;
		visited.add(n);
		
		// visit children
		for (Edge e : treeEdges.get(n)) {
			CFGNode nChild = (e.getSrc() == n)? e.getTgt() : e.getSrc();
			if (!visited.contains(nChild)) {
				edgeList.add(e);
				if (findTreeNodeDFS(nChild, treeEdges, nToFind, visited, edgeList))
					return true;
				else
					edgeList.remove(edgeList.size()-1);
			}
		}
		return false; // no child leads to target node
	}
	
	/**
	 * @param depthFromCaller Depth of paths, counting from caller; must be greater than 0, or -1 to use (max_depth - 1) for each callee
	 */
	public int getCallNumPaths(CFGNode nCall, int depthFromCaller) {
		assert depthFromCaller != 0;
		
		int numPaths = 0;
		for (SootMethod mCallee : nCall.getAppCallSite().getAppCallees()) {
			CFG cfgCallee = ProgramFlowGraph.inst().getCFG(mCallee);
			EPPAnalysis eppAnalysisCallee = cfgEPPAnalyses.get(cfgCallee);
			final int dFromCallee = (depthFromCaller == -1)? eppAnalysisCallee.getMaxDepth() - 1: depthFromCaller - 1;
			HashMap<CFGNode,Integer> numPathsCallee = eppAnalysisCallee.getNumPaths(dFromCallee);
			numPaths += numPathsCallee.get(cfgCallee.ENTRY); // num paths for method is num paths for entry node, at depth - 1
		}
		
		return numPaths;
	}
	
	/** Traverses all paths, including call sites up to given depth, producing a list of paths in the order of the ids they get */
	public void enumPaths(BufferedWriter writer, Map<CFGNode, Integer> globalStmtIdxs, int depth) throws IOException {
		SootMethod m = cfg.getMethod();
		writer.write(ProgramFlowGraph.inst().getMethodIdx(m) + ": " + m + "\n");
		
		// for each node, visit children in increasing edge val order
		ArrayList<Integer> path = new ArrayList<Integer>();
		
		final int foundPaths = followNodePaths(writer, cfg.ENTRY, globalStmtIdxs, path, 0, depth);
		
		assert foundPaths == numPathsPerDepth.get(depth).get(cfg.ENTRY);
	}
	
	/** Returns next path id available, after paths encountered through current call */
	private int followNodePaths(BufferedWriter writer, CFGNode n, Map<CFGNode, Integer> globalStmtIdxs, ArrayList<Integer> path, int pathId, int dLeft) throws IOException {
		// if exit, add exit (-1) to path and finish traversal
		if (n == cfg.EXIT) {
			path.add(-1);
			
			// store path
			writer.write("  " + pathId + ": " + path + "\n");
			
			return pathId + 1;
		}
		
		// store non-exit node
		path.add(globalStmtIdxs.get(n)); // add this node to current path
		
		// visit successors
		int nextPathId = pathId;
		for (Edge eOut : outEdges.get(n)) {
			// skip backedges; paths ending at backedge sources are identified by dummy "to-EXIT" edge set
			if (backedges.contains(eOut))
				continue;
			
			// store path copy at current node if we find a dummy-to-exit edge
			// otherwise, continue path through this edge, or start new path if edge is dummy-from-entry
			if (dummyEdgesToExit.contains(eOut)) {
				// store path ending at this node; do not recurse
				writer.write("  " + pathId + ": " + path + "\n");
				++nextPathId;
			}
			else {
				// start new path from this edge's target, if this is an ENTRY->succ dummy edge
				ArrayList<Integer> nextPath = (dummyEdgesFromEntry.contains(eOut))? new ArrayList<Integer>() : path;
				
				final int prevLen = nextPath.size();
				
				nextPathId = followNodePaths(writer, eOut.getTgt(), globalStmtIdxs, nextPath, nextPathId, dLeft);
				
				// reset current path to old size
				assert nextPath.size() == prevLen + 1;
				nextPath.remove(prevLen);
			}
		}
		
		// continue path through callees, if this node is a call site; these paths are always counted *after* intra-proc edges
		if (dLeft > 0 && n.hasAppCallees()) {
			CallSite cs = n.getAppCallSite();
			for (SootMethod mCallee : cs.getAppCallees()) {
				final int prevLen = path.size();
				
				CFG cfgCallee = ProgramFlowGraph.inst().getCFG(mCallee);
				nextPathId = cfgEPPAnalyses.get(cfgCallee).followNodePaths(writer, cfgCallee.ENTRY, globalStmtIdxs, path, nextPathId, dLeft - 1);
				
				// reset current path to old size
				assert path.size() == prevLen + 1;
				path.remove(prevLen);
			}
		}
		
		return nextPathId;
	}
	
	/**
	 * Recursively enumerates BL-path ids on acyclic graph (CFG w/o backedges), creating bitsets of BL paths crossing, starting and ending at each node.
	 * @param nodesLocalBLPaths (OUT) paths crossing each CFG node 
	 * @param nodesStartingBLPaths (OUT) maps to each path-family starting node the numbers of paths starting at that node
	 * @param nodesEndingBLPaths (OUT) maps to each path-family ending node the numbers of paths ending at that node
	 * @return ID of next path, after id of last identified path
	 */
	public int assignPathsToNodes(Map<CFGNode,BitSet> nodesLocalBLPaths, Map<CFGNode,BitSet> nodesStartingBLPaths, Map<CFGNode,BitSet> nodesEndingBLPaths) {
		// start at CFG entry, with path id 0
		return assignPathsToNodes(cfg.ENTRY, cfg.ENTRY, nodesLocalBLPaths, nodesStartingBLPaths, nodesEndingBLPaths, 0);
	}
	
	/**
	 * Recursively enumerates BL-path ids on acyclic graph (CFG w/o backedges), creating bitsets of BL paths crossing, starting and ending at each node.
	 * @param nFrom previous visited node
	 * @param n current node being visited
	 * @param nEntry node distinguished as CFG entry
	 * @return ID of next path, after id of last identified path
	 */
	private int assignPathsToNodes(CFGNode nFrom, CFGNode n, Map<CFGNode,BitSet> nodesLocalBLPaths, Map<CFGNode,BitSet> nodesStartingBLPaths, Map<CFGNode,BitSet> nodesEndingBLPaths, int pathId) {
		// Handle EXIT node: path ends here
		if (n == cfg.EXIT) {
			updatePathBS(n, nodesLocalBLPaths, pathId, pathId + 1);
			return pathId + 1;
		}
		
		assert !n.isInCatchBlock();
		
		// Visit successors of n
		// Note that some paths start with Entry->N dummy edge; those are assumed to be all edges out of ENTRY, except the first edge
		int nextPathId = pathId;
		Set<Edge> backedges = getBackedges();
		Set<Edge> dummyEdgesFromEntry = getDummyEdgesFromEntry();
		Set<Edge> dummyEdgesToExit = getDummyEdgesToExit();
		for (Edge eOut : getOutEdges().get(n)) {
			// skip backedges; paths ending at backedge sources are identified by dummy "to-EXIT" edge set
			if (backedges.contains(eOut))
				continue;
			
			// path ends at current node if we find a dummy-to-exit edge
			// otherwise, continue path through this edge, or start new path if edge is dummy-from-entry
			if (dummyEdgesToExit.contains(eOut)) {
				// add path id to bitset of paths ending at this node
				BitSet bsNodeEnding = nodesEndingBLPaths.get(n);
				if (bsNodeEnding == null) {
					bsNodeEnding = new BitSet();
					nodesEndingBLPaths.put(n, bsNodeEnding);
				}
				bsNodeEnding.set(pathId);
				
				++nextPathId;
			}
			else
				nextPathId = assignPathsToNodes(n, eOut.getTgt(), nodesLocalBLPaths, nodesStartingBLPaths, nodesEndingBLPaths, nextPathId);
		}
		
		// If this visit came from ENTRY, set path id range as bitset of paths starting at this node
		if (nFrom == cfg.ENTRY)
			updatePathBS(n, nodesStartingBLPaths, pathId, nextPathId);
		
		// Add range of paths visited from this node to bitset of paths crossing this node
		updatePathBS(n, nodesLocalBLPaths, pathId, nextPathId);
		
		return nextPathId;
	}
	
	/** Set range of bits from startId to endId (exclusive) for given node; creates bitset on demand */
	private static void updatePathBS(CFGNode n, Map<CFGNode,BitSet> nodeToBS, int startId, int endId) {
		// get/create bitset for node
		BitSet bs = nodeToBS.get(n);
		if (bs == null) {
			bs = new BitSet();
			nodeToBS.put(n, bs);
		}
		// set range [startId,endId) in bitset
		bs.set(startId, endId);
	}
	
	/**
	 * Computes intraprocedural BL-path edges (connections) compactly as a list of pairs (bitset,bitset) called "connection points".
	 * Each connection point represents the cross product of all ending paths with all starting paths that occurs at every loop head.
	 * 
	 * @param nodesStartingBLPaths (IN)
	 * @param nodesEndingBLPaths (IN)
	 * @param blPathIntraEdges (OUT) Ordered list of "connection points", each describing the cross-product of BL-paths (incoming x outgoing)
	 * @param backedgeConnPoints (OUT) incoming edges to start nodes, as list of conn points for each start node (loop head)
	 * 
	 * @return total number of BL-path edges (connections) found
	 */
	public int computePathEdges(Map<CFGNode, BitSet> nodesStartingBLPaths, Map<CFGNode, BitSet> nodesEndingBLPaths, 
			List<Pair<BitSet,BitSet>> blPathIntraEdges, Map<Edge, Pair<BitSet,BitSet>> backedgeConnPoints)
	{
		// Find all edges among BL-path families; intraprocedurally, they correspond to the backedges
		int nextPathId = 0;
		List<Edge> sortedBackedges = new ArrayList<Edge>(getBackedges());
		Collections.sort(sortedBackedges, new Edge.EdgeComparator());
		for (Edge backEdge : sortedBackedges) {  // src is path family end; tgt is path family start (loop head)
			CFGNode nSrc = backEdge.getSrc();
			CFGNode nTgt = backEdge.getTgt();
			
			// get two bitsets: paths ending at backedge's src, and paths starting and backedge's tgt
			BitSet bsEndingPathsForSrc = nodesEndingBLPaths.get(nSrc);
			BitSet bsStartingPathsForTgt = nodesStartingBLPaths.get(nTgt);
			
			// add bitset pair as a new "connection point" in the list
			Pair<BitSet,BitSet> connPoint = new Pair<BitSet, BitSet>(bsEndingPathsForSrc, bsStartingPathsForTgt);
			blPathIntraEdges.add(connPoint);
			
			// also, associate "connection point" to backedge
			assert !backedgeConnPoints.containsKey(backEdge);
			backedgeConnPoints.put(backEdge, connPoint);
			
			// finally, update count of path edges
			nextPathId += bsEndingPathsForSrc.cardinality() * bsStartingPathsForTgt.cardinality();
		}
		
		return nextPathId;
	}

	/**
	 * Performs full interprocedural (if depth > 0) EPP analysis on given CFGs, obtaining edge values and optimal increments.
	 * 
	 * @param depth (IN) interproc depth of acyclic paths
	 * @param cfgs (IN) methods to analyze
	 * @return EPP analysis results for each CFG
	 */
	public static Map<CFG, EPPAnalysis> computeInterprocEPP(int depth, List<CFG> cfgs) {
		Map<CFG, EPPAnalysis> cfgEPPAnalyses = new HashMap<CFG, EPPAnalysis>();
		
		// 1. Create DAGs (save for EXIT->ENTRY special edges), stripping CFGs of backedges and adding special edges to/from backedge src/tgt from/to ENTRY/EXIT
		for (CFG cfg : cfgs) {
			// create set E of all edges; position in list is implicit global ID of an edge
			HashMap<CFGNode, ArrayList<Edge>> inEdges = new HashMap<CFGNode, ArrayList<Edge>>();
			HashMap<CFGNode, ArrayList<Edge>> outEdges = new HashMap<CFGNode, ArrayList<Edge>>();
			ArrayList<Edge> allEdges = Edge.createEdges(cfg, inEdges, outEdges); // includes EXIT->ENTRY edge
			
			EPPAnalysis eppAnalysis = new EPPAnalysis();
			eppAnalysis.buildDAG(cfg, allEdges, inEdges, outEdges);
			cfgEPPAnalyses.put(cfg, eppAnalysis);
		}
		
		// 2. Compute edge values in resulting DAG, from depth 0 up to specified depth
		for (int d = 0; d <= depth; ++d) {
			// DEBUG
			int totalPaths = 0, maxPaths = 0;
			for (CFG cfg : cfgs) {
				cfgEPPAnalyses.get(cfg).computeNumAcyclicPaths(d, cfgEPPAnalyses);
				
				// DEBUG
				final int cfgPaths = cfgEPPAnalyses.get(cfg).getNumPaths(d).get(cfg.ENTRY);
				totalPaths += cfgPaths;
				if (maxPaths < cfgPaths)
					maxPaths = cfgPaths;
			}
			System.out.println("NumPaths for d=" + d + ": avg " + (((float)totalPaths) / cfgs.size()) + ", max " + maxPaths + ", total " + totalPaths);
		}
		
		// 3. Compute optimal increments from edge values in resulting DAG
		for (CFG cfg : cfgs)
			cfgEPPAnalyses.get(cfg).findMinCostIncrements(depth); // for depths 0 to 'depth'
		
		return cfgEPPAnalyses;
	}
	
}
