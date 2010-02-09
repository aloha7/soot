package dua.path;

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
import soot.jimple.Constant;
import soot.jimple.IdentityStmt;
import soot.jimple.Stmt;
import dua.DUA;
import dua.DUAssocSet;
import dua.global.ProgramFlowGraph;
import dua.method.CFG;
import dua.method.CFGDefUses;
import dua.method.EPPAnalysis;
import dua.method.Edge;
import dua.method.MethodTag;
import dua.method.ReachableUsesDefs;
import dua.method.CFG.CFGNode;
import dua.method.CFG.CFGNodeSpecial;
import dua.method.CFGDefUses.CUse;
import dua.method.CFGDefUses.NodeDefUses;
import dua.method.CFGDefUses.PUse;
import dua.method.CFGDefUses.Use;
import dua.method.ReachableUsesDefs.NodeReachDefsUses;
import dua.util.Pair;
import dua.util.Util;

public class DUAPathExprBuilder {
	/** Computed PE for each DUA */
	private Map<DUA, BLPathExpression> duaPEs = new HashMap<DUA, BLPathExpression>();
	
	public Map<DUA, BLPathExpression> getDuaPEs() { return duaPEs; }
	
	/**
	 * Computes PE for each DUA, following RUs.
	 * Uses iterative any-path backward data-flow algorithm along RUs, updating at each node the BL path expressions
	 * from the node to each RU. Initially, each node is assigned one BL path expression per node's use, which in
	 * turn contains the bitset of BL-path edges that cover that node.
	 * 
	 * For each reachable-use u in a node n, there is a bitvector of BL paths that covers n and u (without killing u)
	 * Paths have a global id, so the bitvector might get too big...
	 * We compute and use function npaths(n), which lists BL paths that include n; npaths returns a bitvector of BL paths based on corresponding CFG
	 * 
	 */
	public DUAPathExprBuilder(
			Collection<CFG> cfgs, DUAssocSet duaSet, Map<CFG, EPPAnalysis> cfgEPPAnalyses,
			Map<CFG, Map<CFGNode,BitSet>> cfgsNodesLocalBLPaths, 
			Map<CFG, Map<Edge,Pair<BitSet,BitSet>>> cfgsBackedgeConnPoints,
			Map<CFG, Map<CFGNode,BitSet>> cfgsCallSiteOutgoingEdges)
	{
		Map<SootMethod, ReachableUsesDefs> mToRUs = Util.convertToRUMap(ProgramFlowGraph.inst().getMethodToCFGMap());
		
		// 1. Init PE for each use with bitset of paths covering use's node
		Map<CFG, Map<CFGNode, BLPathExpression[]>> cfgsNodeOutPEs = new HashMap<CFG, Map<CFGNode,BLPathExpression[]>>();
		Map<CFG, Map<CFGNode, BLPathExpression[]>> cfgsNodeInPEs = new HashMap<CFG, Map<CFGNode,BLPathExpression[]>>();
		for (CFG cfg : cfgs) {
			Map<CFGNode, BLPathExpression[]> nodesInPEs = new HashMap<CFGNode, BLPathExpression[]>();
			Map<CFGNode, BLPathExpression[]> nodesOutPEs = new HashMap<CFGNode, BLPathExpression[]>();
			Map<CFGNode,BitSet> nodesBLPaths = cfgsNodesLocalBLPaths.get(cfg);
//			SootMethod m = cfg.getMethod();
			List<Use> uses = ((CFGDefUses) cfg).getUses();
			final int numCFGUses = uses.size();
			for (CFGNode _n : cfg.getNodes()) {
				if (_n instanceof CFGNodeSpecial)
					continue; // ENTRY or EXIT
				
				if (_n.isInCatchBlock())  // discard nodes in catch blocks, for now
					continue;
				
				// create OUT PEs for uses originated in this node
				NodeDefUses n = (NodeDefUses) _n;
				final int[] usesIds = n.getLocalUsesIds();
				BLPathExpression[] pesIn = new BLPathExpression[numCFGUses]; // id corresponds to use id
				BLPathExpression[] pesOut = new BLPathExpression[numCFGUses]; // id corresponds to use id
				for (int i = 0; i < pesIn.length; ++i) {
					pesIn[i] = null; // initially no PE for use
					pesOut[i] = null; // initially no PE for use
				}
				
				// for each use originated in this node, generate PE
				BitSet accBLPaths = nodesBLPaths.get(n);
				for (int i = 0; i < usesIds.length; ++i) {
					// use paths for node if C-USE
					// for P-USE, use intersection of paths covering source and dest of branch
					Use u = uses.get(usesIds[i]);
					BitSet bsAccUsePaths = (BitSet) accBLPaths.clone();
					if (u instanceof PUse)
						bsAccUsePaths.and( nodesBLPaths.get(u.getBranch().getTgt()) );
					
					// create PE for use, using corresponding bitset of "accepting" (use-traversing) paths
					pesIn[usesIds[i]] = new BLPathExpression(n, bsAccUsePaths);
				}
				
				nodesInPEs.put(n, pesIn);
				nodesOutPEs.put(n, pesOut);
			}
			cfgsNodeInPEs.put(cfg, nodesInPEs);
			cfgsNodeOutPEs.put(cfg, nodesOutPEs);
		}
		
		// 2. Propagate uses' PEs intraprocedurally, along RU paths, linking PEs interprocedurally
		HashSet<CFG> cfgWorkSet = new HashSet<CFG>(cfgs);
		while (!cfgWorkSet.isEmpty()) {
			// Extract from worklist next CFG to process
			CFG cfg = cfgWorkSet.iterator().next();
			cfgWorkSet.remove(cfg);
			Map<CFGNode,BitSet> nodesBLPaths = cfgsNodesLocalBLPaths.get(cfg);
			
			// Create set of BL path start nodes (i.e., backedge targets), other than entry
			EPPAnalysis eppAnalysis = cfgEPPAnalyses.get(cfg);
			HashSet<CFGNode> pathStartNodes = new HashSet<CFGNode>();
			for (Edge eFromEntry : eppAnalysis.getDummyEdgesFromEntry())
				pathStartNodes.add(eFromEntry.getTgt());
			
			
			// Prepare worklist for iterative intra-CFG flow algorithm
			LinkedList<CFGNode> nodeWorklist = new LinkedList<CFGNode>(cfg.getNodes()); // in lexicographical order
			CFGNode special = nodeWorklist.removeFirst(); // remove ENTRY
			assert special == cfg.ENTRY;
			special = nodeWorklist.removeLast(); // remove EXIT
			assert special == cfg.EXIT;
			
			HashSet<CFGNode> nodeWorkSet = new HashSet<CFGNode>(nodeWorklist); // to keep track of elements in worklist and avoid repeated elements in it
			Collections.reverse(nodeWorklist); // in reverse topological order
			
			// Perform iterative PE flow inside cfg, until convergence
			Map<CFGNode, BLPathExpression[]> nodesInPEs = cfgsNodeInPEs.get(cfg);
			Map<CFGNode, BLPathExpression[]> nodesOutPEs = cfgsNodeOutPEs.get(cfg);
			Map<Edge,Pair<BitSet,BitSet>> backedgeConnPoints = cfgsBackedgeConnPoints.get(cfg);
			Map<CFGNode,BitSet> callSiteOutEdges = cfgsCallSiteOutgoingEdges.get(cfg);
			ReachableUsesDefs ru = (ReachableUsesDefs) cfg;
			List<Use> uses = ((CFGDefUses) cfg).getUses();
			Set<Edge> backedges = eppAnalysis.getBackedges();
			while (!nodeWorklist.isEmpty()) {
				for (CFGNode _n : (List<CFGNode>)nodeWorklist.clone()) {
//					if (!(_n instanceof NodeReachDefsUses))
//						continue; // ENTRY or EXIT
					
					// Extract node from worklist
					NodeReachDefsUses n = (NodeReachDefsUses) nodeWorklist.remove(0);
					nodeWorkSet.remove(n);
					
					if (n.isInCatchBlock())  // discard nodes in catch blocks, for now
						continue;
					
					// Propagate PEs from successors
					boolean modNode = false;
					if (n.getSuccs().size() > 0 && n.getSuccs().get(0) != cfg.EXIT) {
						BLPathExpression[] pesOut = nodesOutPEs.get(n);
						final int[] usesIds = n.getLocalUsesIds(); // use-GEN
						
						// For each ru PE in out-array, merge (union) corresponding in-PEs from successors,
						// ... except for "blocked" PEs (whose use is located at this node) or "killed" PEs (whose use is killed at this node)
						boolean modOut = false;
						for (CFGNode nSucc : n.getSuccs()) {
							if (nSucc instanceof CFGNodeSpecial)
								continue; // avoid EXIT
							
							assert !nSucc.isInCatchBlock();
							
							// get in-PEs from succ, and bitset of paths to restrict those PE's to when propagating them to this node ('n')
							BLPathExpression[] pesInSucc = nodesInPEs.get(nSucc);
							BitSet bsRestrictingPaths = nodesBLPaths.get(n);
							
							// if we are propagating along a backedge, "extend" in-PEs first
							Edge edgeToPropagate = new Edge(n,nSucc);
							if (backedges.contains(edgeToPropagate)) {
								// create new array for PEs extended across backedge
								BLPathExpression[] pesInSuccExtended = new BLPathExpression[pesInSucc.length];
								Pair<BitSet,BitSet> connPoint = backedgeConnPoints.get(edgeToPropagate);
//								BitSet bsConnPointIn = getAllInPaths(startNodeIncomingEdges.get(nSucc));
//								BitSet bsConnPointOut = getAllOutPaths(startNodeIncomingEdges.get(nSucc));
								for (int i = 0; i < pesInSuccExtended.length; ++i) {
									if (pesInSucc[i] == null)
										pesInSuccExtended[i] = null; // no PE in succ for use 'i'
									else  // create extension of succ's PE
										pesInSuccExtended[i] = pesInSucc[i].cloneExtend(nSucc, connPoint.first(), connPoint.second()); // bsConnPointIn, bsConnPointOut);
								}
								pesInSucc = pesInSuccExtended; // replace in-PEs array with extended in-PEs
								
//								// if propagating through backedge, further restrict n's path bitset to paths *starting* at succ
//								
//									bsRestrictingPaths.and(bsConnPointOut);
							}
							
							// Merge succ's IN-PEs into this node's IN-PEs sets
							for (int i = 0; i < pesOut.length; ++i) {
								// also before merging, block any p-use along this edge (n->nSucc)
								Use u = uses.get(i);
								if (pesInSucc[i] != null &&
									!(u instanceof PUse && u.getBranch().getSrc() == n && u.getBranch().getTgt() == nSucc))
								{
									if (pesOut[i] == null) {
										// out-PE didn't exist before; clone succ's PE
										pesOut[i] = pesInSucc[i].cloneRestrict(bsRestrictingPaths);
										if (pesOut[i] != null)
											modOut = true;
									}
									else {
										// merge succ's PE into out-PE, and see if out-PE was modified as a result
										// DON'T re-create OUT-PE object, since upper PEs might be linking to this object
										if (pesOut[i].mergeRestrict(pesInSucc[i], bsRestrictingPaths))
											modOut = true;
									}
								}
							}
						}
						
						// Get use-GEN and use-KILL in this node for uses, which prevent PE propagation by "blocking" and "killing", resp.
						BitSet uKillComp = n.getUKillComp(); // use-KILL
						
						// Compute IN sets from OUT, GEN and KILL in this node
						if (modOut) {  // ... only if out-PE sets were modified
							BLPathExpression[] pesIn = nodesInPEs.get(n);
							int nodeUseId = 0; // index within this node's uses
							for (int i = 0; i < pesIn.length; ++i) {
								// block c-uses only; p-uses are blocked earlier, when trying to propagate along branch
								boolean nodeBlocksUse = false;
								if (nodeUseId < usesIds.length && i == usesIds[nodeUseId]) {  // skip ("block") PE of use generated in this node
									++nodeUseId; // move to next use in this node
									if (uses.get(i) instanceof CUse)
										nodeBlocksUse = true;
								}
								if (!nodeBlocksUse && uKillComp.get(i)) {  // propagate use i's PE from OUT to IN only if *not* killed (or blocked) in this node
									if (pesOut[i] != null) {
										if (pesIn[i] == null) {
											// IN-PE didn't exist before; just copy OUT-PE
											pesIn[i] = pesOut[i].clone();
											modNode = true;
										}
										else {
											// merge OUT-PE into IN-PE, and see if IN-PE was modified as a result
											// DON'T re-create IN-PE object, since upper PEs might be linking to this object
											if (pesIn[i].mergeRestrict(pesOut[i], null))
												modNode = true;
										}
									}
								}
							}
						}
					}
					
/*					// Link IN-PEs to interprocedural uses reachable through call site params
					if (n.hasAppCallees()) {  // this node is a call site
						// Iterate over the uses at call site
						CallSite cs = n.getCallSite();
						int[] callSiteUses = n.getUsesIds();
						BLPathExpression[] pesIn = nodesInPEs.get(n);
						for (int uId : callSiteUses) {
							// Make sure that use is considered an actual param
							final int uParamIdx = ru.getUseParamIdx(uId);
							if (uParamIdx < 0)
								continue;  // use is not considered a parameter
							
							// Construct list of callee PEs for this particular use, from the set of all callees
							HashSet<BLPathExpression> pesAtCalleeForUse = new HashSet<BLPathExpression>();
							for (SootMethod mCallee : cs.getCallees()) {
								// get access to this callee method's RU analysis
								ReachableUsesDefs ruCallee = mToRUs.get(mCallee);
								Map<CFGNode,BLPathExpression[]> calleeNodeOutPEs = cfgsNodeOutPEs.get(ruCallee);
								
								// get corresponding formal parameter at callee for use at call site
								FormalParam fp = ruCallee.getFormalParam(uParamIdx);
								BitSet bsCalleeUsesForParam = ruCallee.getUsesOfVal(fp.getV());
								
								// for all local uses of formal param at callee, get PEs for those that reach method's entry (i.e., id stmt for formal param)
								if (bsCalleeUsesForParam != null) {
									BLPathExpression[] pesAtCallee = calleeNodeOutPEs.get(ruCallee.getNode(fp.getIdStmt()));
									for (int i = 0; i < bsCalleeUsesForParam.length(); ++i) {
										if (bsCalleeUsesForParam.get(i)) {
											if (pesAtCallee[i] != null)
												pesAtCalleeForUse.add(pesAtCallee[i]);
										}
									}
								}
							}
							
							// Link callee-PEs to IN-PE of use at this node
							assert pesIn[uId] != null;
							if (!pesAtCalleeForUse.isEmpty()) {
								// add callee PEs as children
								if (pesIn[uId].addChildrenPEs(pesAtCalleeForUse))
									modNode = true;
								// add to this method's use PE's the interproc edges to callee
								pesIn[uId].addEdges(callSiteOutEdges.get(n));
							}
						}
					}*/
					
					// Update worklists of affected nodes/cfgs, if needed
					if (modNode) {
						// Update nodes worklist with affected nodes (i.e., predecessors), if this node was modified
						for (CFGNode nPred : n.getPreds()) {
							if (nPred == cfg.ENTRY || nPred.isInCatchBlock())
								continue;
							
							if (!nodeWorkSet.contains(nPred)) {
								nodeWorklist.add(nPred);
								nodeWorkSet.add(nPred);
							}
						}
						
						// Also, update cfgs worklist with affected nodes (i.e., callers), if this node was modified and it's a formal param declaration node
						Stmt s = n.getStmt();
						if (s instanceof IdentityStmt) {
							// DEBUG: check that stmt corresponds to a formal param
							boolean found = false;
							for (int fpIdx = 0; fpIdx < ru.getNumFormalParams(); ++fpIdx) {
								if (ru.getFormalParam(fpIdx).getIdStmt() == s) {
									found = true;
									break;
								}
							}
							assert found;
							
							// add all caller cfgs to CFG workset
							MethodTag mTag = (MethodTag) ru.getMethod().getTag(MethodTag.TAG_NAME);
							for (SootMethod mCaller : mTag.getCallerMethods()) {
								MethodTag mCallerTag = (MethodTag) mCaller.getTag(MethodTag.TAG_NAME);
								if (mCallerTag.isReachableFromEntry())
									cfgWorkSet.add(mToRUs.get(mCaller));
							}
						}
					}
				}
			}
		}
		
		// 3. Identify PEs for all DUAs (i.e., match ru PEs with defs)
		System.out.println("PEs:"); // DEBUG
		for (DUA dua : duaSet.getAllDUAs()) {
			// identify DUA's def node and container method/CFG
			CFGNode nDef = dua.getDef().getN();
			ReachableUsesDefs ruForDef = (ReachableUsesDefs) ProgramFlowGraph.inst().getContainingCFG(nDef);
//			BitSet accPathsForDef = cfgsNodesLocalBLPaths.get(ruForDef).get(nDef);
			
			// identify PE id for local ru in DUA
			Map<CFG, Map<CFGNode, BLPathExpression[]>> cfgNodePEs = dua.getDef().getVar().isConstant()? cfgsNodeInPEs : cfgsNodeOutPEs;
			BLPathExpression[] pes = cfgNodePEs.get(ruForDef).get(nDef);
			BLPathExpression peDUA;
			Use[] localUses = dua.getLocalUses();
			// *** FOR NOW, assume only one connecting local use
//			if (localUses.length == 1) {
				final int useId = ruForDef.getUseId(localUses[0]); // get id of unique local use associated to DUA
				assert pes[useId] != null;
				peDUA = pes[useId].clone(); // create a new PE object, so we can add start nodes without altering RU's PE
//			}
//			else {  // unusual case, but nonetheless possible
//				// Create new PE to "virtually merge" (link to) all local uses associated to DUA's real use
//				peDUA = new BLPathExpression(ruForDef.getMethod(), accPathsForDef); // all paths for def node are "accepting" for root PE
//				ArrayList<BLPathExpression> sameMtdChildren = new ArrayList<BLPathExpression>();
//				for (Use lu : localUses) {
//					final int useId = ruForDef.getUseId(lu);
//					sameMtdChildren.add(pes[useId]);
//				}
//				peDUA.addChildrenPEs(sameMtdChildren);
//			}
			
//			// add start nodes to PE (i.e., paths that cover the def), and assign it to DUA
//			peDUA.calculateStartNodes();
			duaPEs.put(dua, peDUA);
			
			// DEBUG
			System.out.println(" " + dua + ": " + peDUA);
		}
	}
	
//	private static BitSet getAllInPaths(List<Pair<BitSet,BitSet>> inOutPaths) {
//		BitSet bsIn = new BitSet();
//		for (Pair<BitSet,BitSet> pair : inOutPaths)
//			bsIn.or(pair.first());
//		return bsIn;
//	}
//	private static BitSet getAllOutPaths(List<Pair<BitSet,BitSet>> inOutPaths) {
//		BitSet bsConnPointOut = (BitSet) inOutPaths.get(0).second().clone();
//		for (Pair<BitSet,BitSet> inOut : inOutPaths)
//			assert bsConnPointOut.equals(inOut.second()); // we assume all out bitsets in lists are the same
//		return bsConnPointOut;
//	}
	
}
