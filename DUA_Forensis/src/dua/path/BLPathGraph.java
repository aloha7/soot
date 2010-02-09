package dua.path;

import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dua.method.CFG;
import dua.method.Edge;
import dua.method.CFG.CFGNode;
import dua.util.Pair;

/**
 * Directed graph associated to a single CFG (intraproc), in which a node represents a BL path, and an edge is a "successor" relation.
 */
public class BLPathGraph {
	private Map<CFGNode,BitSet> nodePaths;
	private Map<Edge,Pair<BitSet,BitSet>> startNodeIncomingEdges;
	
	public BLPathGraph(CFG cfg, Map<CFGNode,BitSet> nodePaths, Map<Edge,Pair<BitSet,BitSet>> startNodeIncomingEdges) {
		this.nodePaths = nodePaths;
		this.startNodeIncomingEdges = startNodeIncomingEdges;
	}
	
	/**
	 * @param pe (IN) The BL-path expression (PE) to look for IEs/DEs. All linked PEs are also visited.
	 * @param incEdges (OUT) Place in which to put new found IEs
	 * @param depEdges (OUT) Place in which to put new found DEs
	 */
	public void findDepartingAndIncomingEdges(BLPathExpression pe, List<Pair<BitSet,BitSet>> incEdges, List<Pair<BitSet,BitSet>> depEdges) {
		// IE: for each PE node other than entry (start nodes), find incoming edges in G not in PE
		// DE: for each PE node other than exit (accepting nodes), find departing edges in G not in PE
		
		// traverse PE and linked PEs, avoiding re-visits
		Set<BLPathExpression> visited = new HashSet<BLPathExpression>(); // initially, no visited PEs
		findDepIncEdges(pe, incEdges, depEdges, visited);
	}
	
	/**
	 * Helper for findDepartingAndIncomingEdges, traversing linked PEs and carrying visited set to avoid cycling forever.
	 * 
	 * @param pe (IN) The BL-path expression to visit and find IEs/DEs for
	 * @param incEdges (OUT) IEs found so far, and the place in which to put new found IEs
	 * @param depEdges (OUT) DEs found so far, and the place in which to put new found DEs
	 * @param visited (IN/OUT) Set of PEs visited so far
	 */
	private void findDepIncEdges(BLPathExpression pe, List<Pair<BitSet,BitSet>> incEdges, List<Pair<BitSet,BitSet>> depEdges, Set<BLPathExpression> visited) {
		// check/update PE in visited list
		if (visited.contains(pe))
			return; // don't revisit
		visited.add(pe);
		
		// *** look only at connection points (where BL-path edges are found), not at PE endings
		// IE: for each triple, look for graph edges <inPath,outPath> such that outPath is in conn's linked PE's bitsets, but inPath is not
		// DE: for each triple, look for graph edges <inPath,outPath> such that inPath is in PE's bitsets, but inPath is not in conn's linked PE's bitsets
		Map<Edge, Pair<BitSet,BLPathExpression>> peTriples = pe.getConnectingTriples();
		if (peTriples != null) {
			for (Edge ePEConn : peTriples.keySet()) {
				Pair<BitSet,BLPathExpression> bsToPEPair = peTriples.get(ePEConn);
				BLPathExpression linkedPE = bsToPEPair.second();
				// get triple's linked PE's out paths (union of bitsets at linked PE's level)
				BitSet peOutConnPaths = getUnionOfTopLevelBitsets(linkedPE);
				
				// 1. IEs in BL-path edges from current position to linked PE
				// get bitset of paths incoming this conn point in PE 
				BitSet peConnInPaths = bsToPEPair.first();
				BitSet allGraphOnlyInPaths = new BitSet(); // will store all in-paths for conn point that exist in graph, but not in PE
				Pair<BitSet,BitSet> graphConnBSPair = startNodeIncomingEdges.get(ePEConn);
				// traverse list of in-out path bitset pairs at conn point in graph, finding in-paths in graph but not in PE
				BitSet graphOutPaths = graphConnBSPair.second(); // out bitset should always be the same!
//				for (Pair<BitSet,BitSet> graphConnBSPair : graphConnBSPairs) {
					assert graphOutPaths.equals(graphConnBSPair.second()); // out bitset should always be the same!
					
					// get bitset of paths incoming this conn point in BL graph 
					BitSet graphConnInPaths = graphConnBSPair.first();
					
					// find incoming paths at conn point that are *not* in PE's bitset of incoming paths to conn point
					BitSet graphOnlyInPaths = (BitSet) graphConnInPaths.clone();
					BitSet aux = (BitSet) peConnInPaths.clone();
					aux.and(graphOnlyInPaths); // leave only PE-in paths that are in this pair of graph's list of bs pairs for conn point
					graphOnlyInPaths.xor(aux); // end result: flips to 0 paths that exist in PE, leaving only graph paths *not* in PE
					allGraphOnlyInPaths.or(graphOnlyInPaths); // accumulate results in one bitmap for graph
//				}
				// if there are any in-paths in graph and not in PE, match out-paths in both to identify IEs
				if (!allGraphOnlyInPaths.isEmpty()) {
					// create bitset of out-paths in common
					BitSet matchedOutPaths = (BitSet) graphOutPaths.clone();
					matchedOutPaths.and(peOutConnPaths);
					if (!matchedOutPaths.isEmpty())
						incEdges.add(new Pair<BitSet, BitSet>(allGraphOnlyInPaths, matchedOutPaths));
				}
				
				// 2. DEs in BL-path edges from current position to linked PE
				// build bitset of all out-paths for conn point that exist in graph, but not in PE
				BitSet allGraphOnlyOutPaths = (BitSet) graphOutPaths.clone();
				aux = (BitSet) peOutConnPaths.clone();
				aux.and(allGraphOnlyOutPaths); // leave only PE-out paths that are in this pair of graph's list of bs pairs for conn point
				allGraphOnlyOutPaths.xor(aux); // end result: flips to 0 paths that exist in PE, leaving only graph paths *not* in PE
				// if there are any out-paths in graph and not in PE, match in-paths in both to identify DEs
				if (!allGraphOnlyInPaths.isEmpty()) {
					BitSet matchedInPaths = new BitSet();
					matchedInPaths.or(graphConnBSPair.first());
					matchedInPaths.and(peConnInPaths);
					if (!matchedInPaths.isEmpty())
						depEdges.add(new Pair<BitSet, BitSet>(matchedInPaths, allGraphOnlyOutPaths));
				}
				
				// 3. Visit linked PE
				findDepIncEdges(linkedPE, incEdges, depEdges, visited);
			}
		}
	}
	
	/** Returns union of all top-level bitsets in PE */
	private static BitSet getUnionOfTopLevelBitsets(BLPathExpression pe) {
		// start cloning ending bitset (or creating new bitset if no ending)
		BitSet resUnion = (pe.getEnding() == null)? new BitSet() : (BitSet) pe.getEnding().second().clone();
		
		// add triples' bitsets
		Map<Edge, Pair<BitSet,BLPathExpression>> triples = pe.getConnectingTriples();
		if (triples != null) {
			for (Edge e : triples.keySet())
				resUnion.or( triples.get(e).first() );
		}
		
		return resUnion;
	}
	
}
