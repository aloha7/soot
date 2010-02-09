package dua.path;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import soot.SootMethod;
import dua.DUA;
import dua.DUAssocSet;
import dua.global.ProgramFlowGraph;
import dua.method.CFG;
import dua.method.EPPAnalysis;
import dua.method.Edge;
import dua.method.CFG.CFGNode;
import dua.util.Pair;

/**
 * Computes BL path expression for each DUA, interprocedurally.
 */
public class PathDUAAnalysis {
	
	// IMPLEMENTATION step 1: use level-0 BL paths
	
	/** Stores path analysis for each CFG */
	private Map<CFG, EPPAnalysis> cfgEPPAnalyses = null;
	/** For each CFG, the intraprocedural path graph */
	private Map<CFG, BLPathGraph> pathGraphs = new HashMap<CFG, BLPathGraph>();
	/** Builds BL-path expression for each DUA; interprocedural expressions are linked */
	private DUAPathExprBuilder duaPathExprBuilder;
	
	/**
	 * DONE:
	 *  - Compute path expression (PE) for each DUA in terms of BL paths. PE is an automaton.
	 * TODO: 
	 *  - Compute PE for the full program (Entry to Exit).
	 * 
	 * @param cfgs
	 * @param duaSet
	 */
	public PathDUAAnalysis(DUAssocSet duaSet) {
		List<CFG> cfgs = ProgramFlowGraph.inst().getCFGs();
		
		// 1. Compute BL paths
		final int depth = 0;
		cfgEPPAnalyses = EPPAnalysis.computeInterprocEPP(depth, cfgs);
		
		// 2. Associate BL paths to CFG nodes they traverse -- function BLpaths(n), and find BL path connections (edges pred->succ)
		//    Path ids are local to CFG
		System.out.println("DUA Path Analysis"); // DEBUG
		int totalPaths = 0;
		/** Basic information: BL-paths (ids) that cross a node */
		Map<CFG, Map<CFGNode,BitSet>> cfgsNodesLocalBLPaths = new HashMap<CFG, Map<CFGNode,BitSet>>(); // global 'npaths' function
		/** Basic information: BL-paths (ids) that start at a given node */
		Map<CFG, Map<CFGNode,BitSet>> cfgsNodesStartingBLPaths = new HashMap<CFG, Map<CFGNode,BitSet>>();
		/** For each CFG, intraprocedural BL-path to BL-path edges, described by "connection points" (incoming paths bitset x starting paths bitset), implicitly ordered by id */
		Map<CFG, List<Pair<BitSet,BitSet>>> cfgsBLPathIntraEdges = new HashMap<CFG, List<Pair<BitSet,BitSet>>>();
		/** For each CFG, the nodes that start path families, and the bitset of path ids a node starts */
		Map<CFG, Map<Edge, Pair<BitSet,BitSet>>> cfgsBackedgeConnPoints = new HashMap<CFG, Map<Edge, Pair<BitSet,BitSet>>>();
//		/** For each CFG, interprocedural call-site to callee edges, ordered by id (ids start counting after last id of intraprocedural edges) */
//		Map<CFG, ArrayList<Pair<CFGNode,CFG>>> cfgsBLPathInterEdges = new HashMap<CFG, ArrayList<Pair<CFGNode,CFG>>>();
		/** For each CFG, the nodes that start family paths, and the bitset of path ids each node starts */
		Map<CFG, Map<CFGNode,BitSet>> cfgsCallSiteOutgoingEdges = new HashMap<CFG, Map<CFGNode,BitSet>>();
		
		for (CFG cfg : cfgs) {
			// DEBUG
			System.out.print("  BL connections for " + cfg.getMethod() + ": ");
			
			// First, assign to each node the set of path ids that cover it
			Map<CFGNode,BitSet> nodesLocalBLPaths = new HashMap<CFGNode, BitSet>();
			Map<CFGNode,BitSet> nodesStartingBLPaths = new HashMap<CFGNode, BitSet>();
			Map<CFGNode,BitSet> nodesEndingBLPaths = new HashMap<CFGNode, BitSet>();
			EPPAnalysis eppAnalysis = cfgEPPAnalyses.get(cfg);
			final int numCfgPaths = eppAnalysis.assignPathsToNodes(nodesLocalBLPaths, nodesStartingBLPaths, nodesEndingBLPaths);
			assert numCfgPaths == eppAnalysis.getNumPaths(0).get(cfg.ENTRY);
			cfgsNodesLocalBLPaths.put(cfg, nodesLocalBLPaths);
			cfgsNodesStartingBLPaths.put(cfg, nodesStartingBLPaths);
			
			totalPaths += numCfgPaths; // update total paths stat
			
			// DEBUG
			System.out.print("paths " + numCfgPaths + ", ");
			
			// Second, find all BL path connections (i.e., path->path "edges")
			List<Pair<BitSet,BitSet>> blPathIntraEdges = new ArrayList<Pair<BitSet,BitSet>>();
			Map<Edge, Pair<BitSet,BitSet>> backedgeConnPoints = new HashMap<Edge, Pair<BitSet,BitSet>>();
			ArrayList<Pair<CFGNode,CFG>> blPathInterEdges = new ArrayList<Pair<CFGNode,CFG>>();
			Map<CFGNode,BitSet> callSiteOutEdges = new HashMap<CFGNode, BitSet>();
			final int numBLEdges = computeBLPathEdges(cfg, eppAnalysis, nodesLocalBLPaths, nodesStartingBLPaths, nodesEndingBLPaths, blPathIntraEdges,
					backedgeConnPoints, blPathInterEdges, callSiteOutEdges);
			cfgsBLPathIntraEdges.put(cfg, blPathIntraEdges);
			cfgsBackedgeConnPoints.put(cfg, backedgeConnPoints);
			cfgsCallSiteOutgoingEdges.put(cfg, callSiteOutEdges);
			
			// DEBUG
			System.out.println("backedges " + backedgeConnPoints.keySet().size() + ", conns " + numBLEdges);
		}
		System.out.println("Total BL paths in program: " + totalPaths); // DEBUG
		
		// Compute BL-path graphs
		for (CFG cfg : cfgs)
			pathGraphs.put(cfg, new BLPathGraph(cfg, cfgsNodesLocalBLPaths.get(cfg), cfgsBackedgeConnPoints.get(cfg)));
		
		// 3. Compute BL-path expressions for DUAs
		duaPathExprBuilder = new DUAPathExprBuilder(cfgs, duaSet, cfgEPPAnalyses,
				cfgsNodesLocalBLPaths, cfgsBackedgeConnPoints, cfgsCallSiteOutgoingEdges);
	}
	
	/**
	 * Creates map of pred->{succs} edges (BL-path to BL-path ids), for given CFG.
	 * Includes interprocedural path edges to callees.
	 * Due to their potential high number, edges are stored indirectly as a list of "connection points".
	 * 
	 * @param cfg (IN)
	 * @param eppAnalysis (IN) EPP analysis for CFG
	 * @param nodesLocalBLPaths (IN)
	 * @param nodesStartingBLPaths (IN)
	 * @param nodesEndingBLPaths (IN)
	 * @param blPathIntraEdges (OUT) Ordered list of "connection points", each describing the cross-product of BL-paths (incoming x outgoing)
	 * @param startNodeIncomingEdges (OUT)
	 * @param blPathInterEdges (OUT)
	 * @param callSiteOutEdges (OUT)
	 * 
	 * @return number of BL-path edges (connections), intra- and inter-procedural
	 */
	private int computeBLPathEdges(CFG cfg, EPPAnalysis eppAnalysis, Map<CFGNode,BitSet> nodesLocalBLPaths, Map<CFGNode,BitSet> nodesStartingBLPaths, Map<CFGNode,BitSet> nodesEndingBLPaths,
			List<Pair<BitSet,BitSet>> blPathIntraEdges, Map<Edge, Pair<BitSet,BitSet>> backedgeConnPoints,
			ArrayList<Pair<CFGNode,CFG>> blPathInterEdges, Map<CFGNode,BitSet> callSiteOutEdges)
	{
		// 1. Intraprocedural edges
		int pathEdgeId = eppAnalysis.computePathEdges(nodesStartingBLPaths,
				nodesEndingBLPaths, blPathIntraEdges, backedgeConnPoints);
		
		// 2. Interprocedural edges
		Map<SootMethod, CFG> mToCFGs = ProgramFlowGraph.inst().getMethodToCFGMap();
		for (CFGNode n : cfg.getNodes()) {
			if (n.isInCatchBlock())
				continue;
			
			if (n.hasAppCallees()) {  // this node is a call site
				// add an interproc edge from this node to each callee
				BitSet csOutEdges = new BitSet();
				callSiteOutEdges.put(n, csOutEdges);
				for (SootMethod mCallee : n.getAppCallSite().getAppCallees()) {
					blPathInterEdges.add(new Pair<CFGNode, CFG>(n, mToCFGs.get(mCallee)));
					csOutEdges.set(pathEdgeId++);
				}
			}
		}
		
		return pathEdgeId;
	}
	
	/**
	 * Compute inferability for each DUA, using its PE.
	 * NOTE: Intraprocedural, for now.
	 */
	public void computeInferability() {
		Map<DUA, BLPathExpression> duaPEs = duaPathExprBuilder.getDuaPEs();
		
		for (DUA dua : duaPEs.keySet()) {
			// Find departing and incoming edges (DE and IE) for each PE
			// *** For now, we interpret a DUA's PE intraprocedurally from def to param use at call site
			
			BLPathExpression peDUA = duaPEs.get(dua);
			CFG cfg = ProgramFlowGraph.inst().getContainingCFG(dua.getDef().getN()); // *** CFG for def
			
			// get path graph G
			BLPathGraph pathGraph = pathGraphs.get(cfg);
			
			// DE: for each PE node other than exit (accepting nodes), find departing edges in G not in PE
			// IE: for each PE node other than entry (start nodes), find incoming edges in G not in PE
			List<Pair<BitSet,BitSet>> incEdges = new ArrayList<Pair<BitSet,BitSet>>(); // expressed compactly as conn point bitset cross products
			List<Pair<BitSet,BitSet>> depEdges = new ArrayList<Pair<BitSet,BitSet>>(); // expressed compactly as conn point bitset cross products
			pathGraph.findDepartingAndIncomingEdges(peDUA, incEdges, depEdges);
			System.out.println("BL edges for DUA " + dua + ": ie " + incEdges + ", de " + depEdges);
		}
	}
	
}

