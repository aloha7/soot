package dua.global;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dua.DUA;
import dua.Options;
import dua.method.CFG;
import dua.method.DominatorRelation;
import dua.method.CFG.CFGNode;
import dua.method.CFGDefUses.Branch;
import dua.method.CFGDefUses.Branch.BranchComparator;
import dua.method.CFGDefUses.Use;

/**
 * For a CFG, computes and stores required branches information (per node) and control dependencies (CDs).
 * Uses reverse dominance frontier to find required branches for each node.
 * For entry node or nodes post-dominating the entry, uses a special branch representing entrance to CFG.
 * 
 * NOTE: Required branches for a node are a subset of branches on which a node is control dependent:
 *   RDF(n) = { m | n !pdom m, exists succ s of m s.t. n pdom s }
 *   CD(n) = { m | exists succ s1 of m s.t. n pdom s1, exists succ s2 of m s.t. n !pdom s2 }
 *   
 * The definition of CD(n) does not specify whether n pdom m. For example, in a while (cond) loop,
 * 'while (cond)' pdoms itself, but does not require its true branch to get executed at least once.
 * However, 'while (cond)' is control dependent (CD) on itself, because taking the true branch implies
 * that 'while (cond)' will be executed again afterwards (assuming there is no 'break' inside loop).
 * Therefore, RDF(n) is a subset of CD(n).
 */
public class ReqBranchAnalysis {
	
	/** For each node n, branches originating at RDF(n) that guarantee execution of n at least once. */
	private Map<CFG, Map<CFGNode, Set<Branch>>> nodesToReqBranches = new HashMap<CFG, Map<CFGNode, Set<Branch>>>();
	/** For each node n, branches br not in RDF(n) that guarantee execution of n *after* br.
	 *  Complements ReqBranches (RDF) to form CD branches for each node. */
	private Map<CFG, Map<CFGNode, Set<Branch>>> nodesToOtherCDBranches = new HashMap<CFG, Map<CFGNode, Set<Branch>>>();
	
	private List<Branch> reqInstrBranches = null;
	private List<Branch> allBranches = null;
	
	public Set<Branch> getReqBranches(CFG cfg, CFGNode n) { return nodesToReqBranches.get(cfg).get(n); }
	public Set<Branch> getReqBranches(CFGNode n) { return getReqBranches(ProgramFlowGraph.inst().getContainingCFG(n), n); }
	public Set<Branch> getReqBranches(Collection<CFGNode> nodes) {
		Set<Branch> reqBrs = new HashSet<Branch>();
		for (CFGNode n : nodes)
			reqBrs.addAll(getReqBranches(n));
		return reqBrs;
	}
	public Map<CFGNode, Set<Branch>> getNodesReqBranches(CFG cfg) { return nodesToReqBranches.get(cfg); }
	
	public Set<Branch> getCDBranches(CFG cfg, CFGNode n) {
		Set<Branch> cdBrs = nodesToOtherCDBranches.get(cfg).get(n);
		if (cdBrs != null) {
			// CD branches are req branches + remaining CD branches
			cdBrs = new HashSet<Branch>(cdBrs); // avoid modifying stored set
			cdBrs.addAll(getReqBranches(cfg, n));
		}
		else
			cdBrs = getReqBranches(cfg, n);
		return cdBrs;
	}
	public Set<Branch> getCDBranches(CFGNode n) { return getCDBranches(ProgramFlowGraph.inst().getContainingCFG(n), n); }
	
	// singleton
	private static ReqBranchAnalysis reqBrAnInstance = null;
	public static void createInstance() { reqBrAnInstance = new ReqBranchAnalysis(); }
	public static ReqBranchAnalysis inst() { return reqBrAnInstance; }
	private ReqBranchAnalysis() {
		List<CFG> cfgs = ProgramFlowGraph.inst().getCFGs();
		for (CFG cfg : cfgs)
			computeReqAndCDBranches(cfg);
	}
	
	private void computeReqAndCDBranches(CFG cfg) {
		assert !nodesToReqBranches.containsKey(cfg);
		assert !nodesToOtherCDBranches.containsKey(cfg);
		
		Map<CFGNode, Set<Branch>> inBranches = findInBranches(cfg);
		Map<CFGNode, Set<Branch>> reqBranchesForCFG = new HashMap<CFGNode, Set<Branch>>();
		Map<CFGNode, Set<Branch>> otherCDBranchesForCFG = new HashMap<CFGNode, Set<Branch>>();
		identifyRDFBranches(cfg, inBranches, /*OUT*/reqBranchesForCFG, /*OUT*/otherCDBranchesForCFG);
		nodesToReqBranches.put(cfg, reqBranchesForCFG);
		nodesToOtherCDBranches.put(cfg, otherCDBranchesForCFG);
	}
	
	/** Does not create branches for nodes in catch blocks */
	private Map<CFGNode, Set<Branch>> findInBranches(CFG cfg) {
		HashMap<CFGNode, Set<Branch>> inBranches = new HashMap<CFGNode, Set<Branch>>();
		
		// get special in branch for ENTRY
		Set<Branch> entryRDFBranches = new HashSet<Branch>();
		entryRDFBranches.add( cfg.getEntryBranch() );
		inBranches.put(cfg.ENTRY, entryRDFBranches);
		
		// get in branches for remaining (non-ENTRY) nodes
		for (CFGNode n : cfg.getNodes()) {
			if (!n.isInCatchBlock() && n.getSuccs().size() > 1) {
				// store branches to successors as "in branches" (avoid multiple brs for same successor)
				HashSet<Branch> brOutSet = new HashSet<Branch>(n.getOutBranches());
				for (Branch brOut : brOutSet) {
					// add in-branch to successor, creating list of in-branches on demand
					CFGNode nSucc = brOut.getTgt();
					Set<Branch> inBrs = inBranches.get(nSucc);
					if (inBrs == null) {
						inBrs = new HashSet<Branch>();
						inBranches.put(nSucc, inBrs);
					}
					inBrs.add(brOut);
				}
			}
		}
		
		return inBranches;
	}
	
	/** Identifies for each node any branch that, if taken, guarantees that the node is covered
	 *  at least once (RDF branches) or *after* the branch (additional CD branches).
	 *  @param (OUT) 
	 */
	private void identifyRDFBranches(CFG cfg, Map<CFGNode, Set<Branch>> inBranches,
			Map<CFGNode, Set<Branch>> reqBranchesForCFG, Map<CFGNode, Set<Branch>> otherCDBranchesForCFG) {
		// if n is entry: add special call branch for method
		// else: RDF,CD for each node
		
		// find RDF,CD for each node
		List<CFGNode> nodeList = cfg.getNodes();
		final int numNodes = nodeList.size();
		for (CFGNode n : cfg.getNodes()) {
			if (n.isInCatchBlock())
				continue; // no pdom info available
			Set<Branch> inBrs = inBranches.get(n);
			if (inBrs == null)
				continue; // no RDF/CD in-branches to contribute
			BitSet pdom = DominatorRelation.inst().getDomData(n).getLocalPDom();
			// iterate over post-dominators of n
			for (int i = 0; i < numNodes; ++i) {
				if (pdom.get(i)) {
					// get post-dominator i's cfg node
					CFGNode nPDom = nodeList.get(i);
					
					// get/create req branches list for node
					Set<Branch> reqBr = reqBranchesForCFG.get(nPDom);
					if (reqBr == null) {
						reqBr = new HashSet<Branch>();
						reqBranchesForCFG.put(nPDom, reqBr);
					}
					
					// add to n's post-dominator i's RDF all preds of in-branches of n that are not pdom by i (i = nPDom)
					// ... and to i's other-CD all additional in-branches br s.t. br's src has a succ not pdom by i (i = nPDom)
					for (Branch inBr : inBrs) {
						if (inBr.getSrc() == null)
							reqBr.add(inBr); // special branch is requisite for pdom of entry
						else {
							CFGNode nInBrSrc = inBr.getSrc();
							// in-branch is in RDF(nPDom) if br's src is not pdom by i (nPDom)
							if (!DominatorRelation.inst().getDomData( nInBrSrc ).getLocalPDom().get(i))
								reqBr.add(inBr);
							else {  // in-branch is in CD(nPDom) if some succ of br's src is not pdom by i (nPDom)
								for (CFGNode nBrSrcSucc : nInBrSrc.getSuccs()) {
									if (!DominatorRelation.inst().getDomData( nBrSrcSucc ).getLocalPDom().get(i)) {
										// get/create set of other-CD-branches for i (nPDom)
										Set<Branch> otherCDBrs = otherCDBranchesForCFG.get(nPDom);
										if (otherCDBrs == null) {
											otherCDBrs = new HashSet<Branch>();
											otherCDBranchesForCFG.put(nPDom, otherCDBrs);
										}
										// add in-br to set of other-CD-branches for i (nPDom)
										otherCDBrs.add(inBr);
										break; // no need to check more successors of br's src
									}
								}
							}
						}
					}
				}
			}
		}
	}
	
	/** Gets/creates and returns array with all branches in the program. */
	public List<Branch> getAllBranches() {
		if (allBranches == null) {
			// collect in set first, to ensure no branch is repeated
			HashSet<Branch> tmpBrSet = new HashSet<Branch>();
			for (CFG cfg : ProgramFlowGraph.inst().getCFGs()) {
				// add CFG's entry branch
				tmpBrSet.add(cfg.getEntryBranch());
				// add outgoing branches from nodes
				for (CFGNode n : cfg.getNodes()) {
					List<Branch> outBrs = n.getOutBranches();
					if (outBrs != null)
						tmpBrSet.addAll(outBrs);
				}
			}
			
			// transform set into sorted list
			allBranches = new ArrayList<Branch>(tmpBrSet);
			Collections.sort(allBranches, new BranchComparator());
		}
		
		return allBranches;
	}
	
	/** 
	 * Gets/creates and returns array with all required branches for given DUAs.
	 * If hybrid instrumentation, it returns branches only for definitely inferrable DUAs.
	 */
	public List<Branch> getInstrBranches(Collection<DUA> duas) {
		if (reqInstrBranches == null) {
			// collect in set first, to ensure no branch is repeated
			HashSet<Branch> tmpBrSet = new HashSet<Branch>();
			for (DUA dua : duas) {
				if (!Options.hybridInstr() || dua.isDefinitelyInferrable())
					tmpBrSet.addAll(getDUAReqBranches(dua));
			}
			
			// transform set into sorted list
			reqInstrBranches = new ArrayList<Branch>(tmpBrSet);
			Collections.sort(reqInstrBranches, new BranchComparator());
		}
		
		return reqInstrBranches;
	}
	
	public Set<Branch> getDUAReqBranches(DUA dua) {
		Set<Branch> duaReqbranches = new HashSet<Branch>();
		
		// get associated branches
		duaReqbranches.addAll( dua.retrieveAllRelatedBranches() );
		// get required branches for associated nodes (i.e., nodes for def, use, and kills)
		for (CFGNode n : dua.retrieveAllRelatedNodes())
			duaReqbranches.addAll( nodesToReqBranches.get(ProgramFlowGraph.inst().getContainingCFG(n)).get(n) );
		
		return duaReqbranches;
	}
	
	// DEBUG
	public int getNumBranches(CFG cfg) {
		int numBranches = 1; // start by counting entry branch
		for (Set<Branch> brs : nodesToReqBranches.get(cfg).values())
			numBranches += brs.size();
		return numBranches;
	}
	
	// DEBUG
	public void print() {
		int totalBranches = 0;
		for (CFG cfg : nodesToReqBranches.keySet())
			totalBranches += getNumBranches(cfg);
		System.out.println("Total required branches: " + totalBranches);
	}
	
	public Set<Branch> getUseReqBranches(Use use) {
		Branch useBr = use.getBranch();
		if (useBr != null) {
			Set<Branch> useBranches = new HashSet<Branch>();
			useBranches.add(useBr);
			return useBranches;
		}
		
		return getReqBranches(use.getN());
	}
	
}
