package dua.path;

import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dua.method.Edge;
import dua.method.CFG.CFGNode;
import dua.util.Pair;

/**
 * A PE contains two parts:
 *   1) ending:  target node and *all* paths that lead to it from current (start) position
 *   2) triples: connection points, along with *all* paths that lead to it from current position, and the PEs the conn points link to
 * 
 * NOTE: for simplicity, paths not starting at a conn point are *not* removed from linked PE.
 */
public class BLPathExpression {
	/**
	 * Ending (target) point.
	 * Not null if there exists a path directly from current position to target; null otherwise.
	 * Path ids in bitset correspond to end node's containing CFG.
	 */
	private Pair<CFGNode,BitSet> ending = null;
	/**
	 * Set of triples <ConnNode,Bitset,PathExpr>:
	 *    connPoint: backedge in CFG, marking end of a path family and the beginning of another path family
	 *    bitset:    paths from 'current position' to connection point
	 *    pathexpr:  BL path expr from conn point to ending
	 * 
	 * Path ids in bitsets correspond to conn-point's containing CFG.
	 * Map is null if there are no triples.
	 */
	private Map<Edge, Pair<BitSet,BLPathExpression>> connectingTriples = null;
	
	public Pair<CFGNode,BitSet> getEnding() { return ending; }
	public Map<Edge, Pair<BitSet,BLPathExpression>> getConnectingTriples() { return connectingTriples; }
	
	/**
	 * @param nTgt Target (ending) CFG node of this PE
	 * @param accPaths set of accepting nodes (BL paths), that is, the nodes that cover the end point of the path family represented by this PE
	 */
	public BLPathExpression(CFGNode nTgt, BitSet accPaths) {
		assert !accPaths.isEmpty();
		// set ending node (which has no continuing PE linked) and bitset of paths to it (i.e., "accepting" paths)
		ending = new Pair<CFGNode,BitSet>(nTgt, accPaths);
	}
//	public BLPathExpression(CFGNode nConnPoint, BitSet pathsToConnPoint, BLPathExpression peConnPoint) {
//		assert !pathsToConnPoint.isEmpty();
//		// set up with single initial triple: provided connection point, paths to it, and PE from conn point to target
//		connectingTriples = new HashMap<Edge, Pair<BitSet,BLPathExpression>>();
//		connectingTriples.put(nConnPoint, new Pair<BitSet, BLPathExpression>(pathsToConnPoint, peConnPoint));
//	}
	private BLPathExpression() {}
	
	/**
	 * Adds/updates ending and triples using given PE and restricting local paths. (Only restricts if given restricting bitset is not null).
	 * When updated, an ending/triple's paths are added (unioned).
	 */
	public boolean mergeRestrict(BLPathExpression peOther, BitSet bsRestrictingPaths) {
		boolean modified = false;
		
		// 1. Merge endings
		if (peOther.ending != null) {
			BitSet bsRestrictedOther = (BitSet) peOther.ending.second().clone();
			if (bsRestrictingPaths != null)
				bsRestrictedOther.and(bsRestrictingPaths);
			if (!bsRestrictedOther.isEmpty()) {
				if (this.ending == null) {
					this.ending = new Pair<CFGNode, BitSet>(peOther.ending.first(), bsRestrictedOther);
					modified = true;
				}
				else {
					if (!bsRestrictedOther.equals(this.ending.second())) {
						this.ending.second().or(bsRestrictedOther);
						modified = true;
					}
				}
			}
		}
		final CFGNode nEnd = (this.ending == null)? null : this.ending.first();
		
		// 2. Merge triples
		//    Check triples in other PE; add triples for nodes not here, and update triples for nodes already here
		if (peOther.connectingTriples != null) {
			// create triples map, if didn't exist
			if (this.connectingTriples == null)
				this.connectingTriples = new HashMap<Edge, Pair<BitSet,BLPathExpression>>();
			// merge-in provided triples
			for (Edge eConnOther : peOther.connectingTriples.keySet()) {
				Pair<BitSet,BLPathExpression> pairOther = peOther.connectingTriples.get(eConnOther);
				Pair<BitSet,BLPathExpression> pairHere = this.connectingTriples.get(eConnOther);
				if (pairHere == null) {
					// create new triple with restricted bitset
					BitSet newTripleBitset = (BitSet)pairOther.first().clone();
					if (bsRestrictingPaths != null)
						newTripleBitset.and(bsRestrictingPaths); // restrict to local paths
					
					// add new triple only if non-empty bitset
					if (!newTripleBitset.isEmpty()) {
						// determine if triple is redundant with ending
						boolean differsFromEnding = true;
						if (eConnOther.getTgt() == nEnd) {  // to be the same, first the ending node and conn point must be the same
							// second, to be the same, ending's bitset must contain all bits in triple's bitset
							BitSet bsEndingClone = (BitSet) this.ending.second().clone();
							bsEndingClone.or(newTripleBitset);
							if (bsEndingClone.equals(this.ending.second()))
								differsFromEnding = false;
						}
						// add triple only if different from ending (i.e., nodes or bitsets differ)
						if (differsFromEnding) {
							pairHere = new Pair<BitSet, BLPathExpression>(newTripleBitset, pairOther.second());
							this.connectingTriples.put(eConnOther, pairHere);
							
							modified = true;
						}
					}
				}
				else {
					// *** we assume that the connected PE object is the same already stored here,
					// *** and that the bitset to that conn point can only grow (i.e., no old bit is reset)
					BitSet bsHere = pairHere.first();
					BitSet bsHereCopy = (BitSet) bsHere.clone();
					
					// just add new bits, if any
					bsHere.or(pairOther.first());
					if (bsRestrictingPaths != null)
						bsHere.and(bsRestrictingPaths); // restrict to local paths
					assert !bsHere.isEmpty();
					if (!bsHere.equals(bsHereCopy))
						modified = true;
					
					// remove resulting triple if redundant with ending
					if (eConnOther.getTgt() == nEnd && bsHere.equals(this.ending.second())) {
						assert modified == true;
						this.connectingTriples.remove(pairHere);
					}
				}
			}
		}
		
		return modified;
	}
	
	/** "Semi-shallow" clone. Clones all internal bitsets and the children PE list, but keeps pointers to children PEs */
	public BLPathExpression clone() {
		BLPathExpression clonedPE = new BLPathExpression();
		
		if (this.ending != null)
			clonedPE.ending = new Pair<CFGNode, BitSet>(this.ending.first(), (BitSet) this.ending.second().clone());
		
		if (this.connectingTriples != null)
			clonedPE.connectingTriples = cloneTriplesMap(this.connectingTriples);
		
		return clonedPE;
	}
	/**
	 * @param triplesMap map of triples to clone
	 * @return cloned map, where nodes and PEs are not cloned, but bitsets are
	 */
	private static Map<Edge, Pair<BitSet,BLPathExpression>> cloneTriplesMap(Map<Edge, Pair<BitSet,BLPathExpression>> triplesMap) {
		Map<Edge, Pair<BitSet,BLPathExpression>> clonedMap = new HashMap<Edge, Pair<BitSet,BLPathExpression>>();
		for (Edge eConn : triplesMap.keySet()) {
			Pair<BitSet,BLPathExpression> bsPEPair = triplesMap.get(eConn);
			// new triple has original node and PE, but cloned bitset
			clonedMap.put(eConn, new Pair<BitSet,BLPathExpression>((BitSet)bsPEPair.first().clone(), bsPEPair.second()));
		}
		return clonedMap;
	}
	
	/**
	 * Clones PE, restricting every path bitset to conn points to given bitset.
	 * Returns null if no ending or conn point is left after restricting to given paths.
	 */
	public BLPathExpression cloneRestrict(BitSet bsRestrictingPaths) {
		BLPathExpression peCloned = this.clone(); // clone
		
		// 1. Clone ending, restricting its bitset
		if (peCloned.ending != null) {
			peCloned.ending.second().and(bsRestrictingPaths);
			if (peCloned.ending.second().isEmpty())
				peCloned.ending = null;
		}
		final CFGNode nEnd = (this.ending == null)? null : this.ending.first();
		
		// 2. Clone triples, restricting their bitsets. Remove any triple "contained" by ending
		if (peCloned.connectingTriples != null) {
			for (Edge eConn : new HashSet<Edge>(peCloned.connectingTriples.keySet())) {
				Pair<BitSet,BLPathExpression> clonedPair = peCloned.connectingTriples.get(eConn);
				clonedPair.first().and(bsRestrictingPaths); // restrict triple's bitset
				// remove triple if restricted bitset is empty
				if (peCloned.connectingTriples.get(eConn).first().isEmpty())
					peCloned.connectingTriples.remove(eConn);
				else {
					// remove triple if it becomes redundant with ending
					if (eConn.getTgt() == nEnd) {
						BitSet bsEndingClone = (BitSet) this.ending.second().clone();
						bsEndingClone.or(clonedPair.first());
						if (bsEndingClone.equals(this.ending.second()))
							peCloned.connectingTriples.remove(eConn);
					}
				}
			}
			if (peCloned.connectingTriples.isEmpty())
				peCloned.connectingTriples = null;
		}
		
		return (peCloned.ending == null && peCloned.connectingTriples == null)? null : peCloned;
	}
	
	/**
	 * Creates an extension of 'this' PE by creating a clone PE that links to 'this' PE. Only paths that do not start at given conn point
	 * (i.e., paths that come from *before* the conn point) are preserved in new ("cloned") PE. Each ending/triple is removed if there are
	 * no paths left in the respective bitset.<br>
	 * <br>
	 * The extended PE will contain a new triple for the given conn point, using the 'in' paths to that conn point that do *not* cover
	 * the ending (if there was an ending in 'this' PE), and linking to old ('this') PE. If there are no such paths, this method
	 * just returns the same PE (a clone, but equivalent to 'this').<br>
	 * <br>
	 * This method is meant to be invoked for a PE at given conn point, when trying to propagate it backwards in CFG.
	 * 
	 * @param nConnPoint connection point to which this method creates an 'extended' PE, linking to old ('this') PE from conn point
	 * @param inOutPaths incoming paths for conn point (used from new PE to 'this' PE) and outgoing paths (to filter out paths from extended PE)
	 */
	public BLPathExpression cloneExtend(CFGNode nConnPoint, BitSet bsConnPointIn, BitSet bsConnPointOut) {
		// 1. Clone this PE
		BLPathExpression peCloned = this.clone();
		
		// compute bitset of paths from new (cloned) PE to conn point
		assert !bsConnPointIn.isEmpty();
		// prevent paths in bitset to conn point from covering past ending, by removing from it all paths to old ending
		BitSet bsRestrictedConnPointIn = (BitSet) bsConnPointIn.clone();
		if (this.ending != null) {
			bsRestrictedConnPointIn.xor(this.ending.second());
			bsRestrictedConnPointIn.and(bsConnPointIn);
			
			// do not extend if no paths can reach conn point without covering ending as well;
			// in that case, just return intact clone, equivalent to 'this' PE
			if (bsRestrictedConnPointIn.isEmpty())
				return peCloned;
		}
		
		// 2. Update ending's path bitset by removing paths to it that start at given conn point
		//    Note that removed paths will still belong to PE through linking to old PE (this) -- see step 4
		if (peCloned.ending != null) {
			// for each path to ending that *starts* at given conn point, remove that path from ending
			peCloned.ending.second().xor(bsConnPointOut);
			peCloned.ending.second().and(this.ending.second()); // bitset in 'this' is the same
			
			// remove ending from clone if all paths in previous PE were removed
			if (peCloned.ending.second().isEmpty())
				peCloned.ending = null;
		}
		
		// 3. Update each triple by removing paths to its conn point that start at given conn point
		//    Note that removed paths will still belong to PE through linking to old PE (this) -- see step 4
		if (peCloned.connectingTriples != null) {
			for (Edge e : new HashSet<Edge>(peCloned.connectingTriples.keySet())) {
				// for each path to linked conn point that *starts* at given conn point, remove that path from linked conn point
				Pair<BitSet,BLPathExpression> linkedConnPointPair = peCloned.connectingTriples.get(e);
				linkedConnPointPair.first().xor(bsConnPointOut);
				linkedConnPointPair.first().and(this.connectingTriples.get(e).first()); // bitset in 'this' is the same
				
				// remove linked conn point from clone if all paths in previous PE were removed
				if (linkedConnPointPair.first().isEmpty())
					peCloned.connectingTriples.remove(e);
			}
		}
		
		// *** SEE HOW TO FIX ***
//		// 4. Add/update top-level triple for conn point
//		if (peCloned.connectingTriples == null)
//			peCloned.connectingTriples = new HashMap<Edge, Pair<BitSet,BLPathExpression>>();
//		Pair<BitSet,BLPathExpression> givenConnPointPair = peCloned.connectingTriples.get(nConnPoint);
//		if (givenConnPointPair == null) {
//			// new triple for given conn point links to old PE
//			givenConnPointPair = new Pair<BitSet, BLPathExpression>(bsRestrictedConnPointIn, this);
//			peCloned.connectingTriples.put(nConnPoint, givenConnPointPair);
//		}
//		else
//			givenConnPointPair.first().or(bsRestrictedConnPointIn);
//		
//		assert !peCloned.connectingTriples.isEmpty();
		
		return peCloned;
	}
	
	@Override
	public String toString() {
		// use internal printer with 'visited' PE list to avoid loops through children PEs
		Set<BLPathExpression> visited = new HashSet<BLPathExpression>();
		return internalToString(visited);
	}
	/** Implements creation of string from this object, visiting children PEs recursively and avoiding running into loops */
	private String internalToString(Set<BLPathExpression> visited) {
		if (visited.contains(this))
			return "[LOOP " + Integer.toHexString(hashCode()) + "]";
		visited.add(this);
		
		// ending
		String str = (ending == null)? "|-|" : ("|" + ending.first().toString() + ", " + ending.second().toString() + "|");
		
		// triples
		if (connectingTriples == null) {
			str += "<->";
		}
		else {
			final String thisId = Integer.toHexString(hashCode());
			str += "<" + thisId + " "; // use id to uniquely identify 
			boolean first = true;
			for (Edge e : connectingTriples.keySet()) {
				// add comma between successive triples
				if (first)
					first = false;
				else
					str += "," + thisId; // ... and id to show comma's level
				
				Pair<BitSet,BLPathExpression> pair = connectingTriples.get(e);
				str += "(" + e + ", " + 
						pair.first() + ", " + 
						((pair.second() == null)? "null" : pair.second().internalToString(visited)) + 
						")";
			}
			str += " " + thisId + ">";
		}
		
		visited.remove(this);
		
		return str;
	}
	
	@Override
	public boolean equals(Object oPEOther) {
		if (!(oPEOther instanceof BLPathExpression))
			return false;
		BLPathExpression peOther = (BLPathExpression) oPEOther;
		
		// 1. Compare ending
		if (this.ending == null) {
			if (peOther.ending != null)
				return false;
		}
		else {
			if (peOther.ending == null)
				return false;
			if (this.ending.first() != peOther.ending.first())
				return false;
			if (!this.ending.second().equals(peOther.ending.second()))
				return false;
		}
		
		// 2. Compare conn nodes and bitsets
		if (this.connectingTriples == null) {
			if (peOther.connectingTriples != null)
				return false;
		}
		else {
			if (peOther.connectingTriples == null)
				return false;
			
			// compare keys (conn nodes) first
			if ( !this.connectingTriples.keySet().equals( peOther.connectingTriples.keySet() ) )
				return false;
			
			// then, compare path bitsets to each conn node
			for (Edge e : this.connectingTriples.keySet()) {
				BitSet bsPathsHere = this.connectingTriples.get(e).first();
				BitSet bsPathsOther = peOther.connectingTriples.get(e).first();
				if (!bsPathsHere.equals(bsPathsOther))
					return false;
				
				// if one connecting PE is null, the other must be null too
				BLPathExpression peConnThis = this.connectingTriples.get(e).second();
				BLPathExpression peConnOther = peOther.connectingTriples.get(e).second();
				
				// *** only compares that both are null or both are non-null
				//     if both are non-null, they should be equal. We don't want to end up recursing infinitely.
				if (peConnThis == null) {
					if (peConnOther != null)
						return false;
				}
				else if (peConnOther == null) {
					if (peConnThis != null)
						return false;
				}
	
			}
		}
		
		return true; // no difference found (note that there is no need to compare linking PEs)
	}
	
	@Override
	public int hashCode() {
		int h = 0;
		
		if (ending != null)
			h += ending.first().hashCode() + ending.second().hashCode();
		
		if (connectingTriples != null) {
			for (Edge e : connectingTriples.keySet()) {
				h += e.hashCode();
				
				Pair<BitSet,BLPathExpression> pair = connectingTriples.get(e);
				h += pair.first().hashCode();
				h += (pair.second() == null)? 0 : 1; // do NOT recurse
			}
		}
		
		return h;
	}
	
}
