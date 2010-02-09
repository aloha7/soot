package dua;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dua.method.CFG.CFGNode;
import dua.method.CFGDefUses.Branch;
import dua.method.CFGDefUses.Def;
import dua.method.CFGDefUses.PUse;
import dua.method.CFGDefUses.Use;

public class DUAssocSet {
	private List<DUA> duas = new ArrayList<DUA>();
	private Map<Def, Set<Use>> sameBBDUs = new HashMap<Def, Set<Use>>();
	private Map<Def, List<Def>> miniBackDefSlices = null; // built on demand
	private Map<CFGNode, List<Def>> miniBackNodeSlices = null; // built on demand
	
	public void addDUA(DUA dua) { duas.add(dua); }
	public List<DUA> getAllDUAs() { return duas; }
	
	public void addSameBBDUs(Map<Def, Set<Use>> sameBBDUsToAdd) { sameBBDUs.putAll(sameBBDUsToAdd); }
	public Map<Def, Set<Use>> getSameBBDUs() { return sameBBDUs; }
	
	/** Updates inferrability of duas using pUse->numDefs info */
	public void updateInferrability() {
		// find non-field duas with p-use and multiple defs
		HashMap<PUse, Integer> pUseNumDefs = new HashMap<PUse, Integer>();
		for (DUA dua : duas) {
			if (!dua.isField() && dua.getUse() instanceof PUse) {
				PUse pUse = (PUse) dua.getUse();
				Integer numDefs = pUseNumDefs.get(pUse);
				if (numDefs == null) {
					numDefs = new Integer(1);
					pUseNumDefs.put(pUse, numDefs);
				}
				else
					pUseNumDefs.put(pUse, ++numDefs);
			}
		}
		
		// update inferrability of currently inferrable duas
		for (DUA dua : duas) {
			if (dua.isInferrableOrCondInf()) {
//				// if it has possible kills, then it's conditionally inferrable
//				if (!dua.killsNotInOrder.isEmpty())
//					dua.setInferrable(false);
//				else 
				if (dua.getUse() instanceof PUse){
					// if it's a p-use with > 1 defs, then it's not inferrable
					final int numDefs = pUseNumDefs.get(dua.getUse());
					if (numDefs > 1)
						dua.setInferrableOrCondInf(false);
				}
			}
		}
	}
	
	/** Returns "mini-back-def-slice" (i.e., defs influencing defs within BB) for all definitions (whether they flow inter-block or not).
	 *  Constructs map on demand. */
	public Map<Def,List<Def>> getMiniBackDefSlices()  {
		if (miniBackDefSlices == null)
			buildMiniBackSlices();
		return miniBackDefSlices;
	}
	public Map<CFGNode,List<Def>> getMiniBackNodeSlices()  {
		if (miniBackNodeSlices == null)
			buildMiniBackSlices();
		return miniBackNodeSlices;
	}
	private void buildMiniBackSlices()  {
		miniBackDefSlices = new HashMap<Def, List<Def>>();
		miniBackNodeSlices = new HashMap<CFGNode, List<Def>>();
		
		// 1. map to each node the same-bb duas used at node
		Map<CFGNode, List<Def>> nodesToSameBBDefs = new HashMap<CFGNode, List<Def>>();
		for (Def intraDef : sameBBDUs.keySet()) {
			assert intraDef.isComputed();
			for (Use intraUse : sameBBDUs.get(intraDef)) {
				// get/create intra-def list for intra use
				CFGNode nIntraUse = intraUse.getSrcNode();
				List<Def> sameBBDefsToIntraUseNode = nodesToSameBBDefs.get(nIntraUse);
				if (sameBBDefsToIntraUseNode == null) {
					sameBBDefsToIntraUseNode = new ArrayList<Def>();
					nodesToSameBBDefs.put(nIntraUse, sameBBDefsToIntraUseNode);
				}
				sameBBDefsToIntraUseNode.add(intraDef);
			}
		}
		
		// 2. for each interblock def, recursively associate all def nodes with same-bb use to interblock def
		for (DUA dua : duas) {
			// build mini-slice for def, if not built before (that's because there can be multiple duas for def)
			Def interDef = dua.getDef();
			if (!interDef.isComputed())
				continue; // uses do not link to def (they is not mini-slice)
			if (!miniBackDefSlices.containsKey(interDef)) {
				// create initially empty mini-slice for inter def
				List<Def> miniBackSliceForInterDef = new ArrayList<Def>();
				miniBackDefSlices.put(interDef, miniBackSliceForInterDef);
				
				// recursively build list of intra defs for inter def
				// init intra-def worklist with intra defs for inter-def node
				CFGNode nInterDef = interDef.getN();
				if (nodesToSameBBDefs.containsKey(nInterDef)) {
					Set<Def> worklist = new HashSet<Def>();
					worklist.addAll(nodesToSameBBDefs.get(nInterDef));
					while (!worklist.isEmpty()) {
						// extract one element from intra-def worklist
						Def intraDef = worklist.iterator().next();
						worklist.remove(intraDef);
						
						// add element to mini-slice, if not there before
						//  (it can repeat because there can be multiple intra-paths from def to def)
						if (!miniBackSliceForInterDef.contains(intraDef)) {
							miniBackSliceForInterDef.add(intraDef);
							
							// add intra-defs for this intra-def to worklist, if any
							if (nodesToSameBBDefs.containsKey(intraDef))
								worklist.addAll(nodesToSameBBDefs.get(intraDef));
						}
					}
				}
			}
			
			// create/update mini-slice for p-use, if not built before (that's because there can be multiple duas for p-use)
			Branch brUse = dua.getUse().getBranch();
			if (brUse != null) { // it's a p-use
				CFGNode nPred = brUse.getTgt();
				List<Def> defsSameBBForNode = miniBackNodeSlices.get(nPred);
				if (defsSameBBForNode == null) {
					defsSameBBForNode = new ArrayList<Def>();
					miniBackNodeSlices.put(nPred, defsSameBBForNode);
				}
				// only if DUA's def is in same BB, add mini-slice for def of DUA, plus def itself
				if (dua.isPUseIntraBlock()) {
					if (!defsSameBBForNode.contains(interDef))
						defsSameBBForNode.add(interDef);
					for (Def defMiniBackSlice : miniBackDefSlices.get(interDef)) {
						if (!defsSameBBForNode.contains(defMiniBackSlice))
							defsSameBBForNode.add(defMiniBackSlice);
					}
				}
			}
		}
	}
	
}
