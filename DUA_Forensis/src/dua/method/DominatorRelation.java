package dua.method;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import soot.SootMethod;
import soot.jimple.Stmt;
import dua.Options;
import dua.global.ProgramFlowGraph;
import dua.global.ReachabilityAnalysis;
import dua.method.CFG.CFGNode;
import dua.unit.StmtTag;

/** 
 * Computes inter-proc dominators and post-dominators at the CFG node level, respect
 * to the program's entry.
 * 
 * FOR NOW: sets to empty dominator sets of nodes in catch blocks
 * 
 * Only computes domination for methods reachable from entry; we are discarding the rest.
 * 
 * Algorithm for global (inter-proc) dominators:
 *   1) compute local (intra-proc) dom node->node, and local dom callsite->callsite
 *   2) compute global dom cfg->cfg; a call site is divided in 'call' and 'ret' nodes
 *      Distinguishes cfg-entry and cfg-exit dom/pdom. For details, see computeInterprocDom.
 *   3) compute global dom cfg->node
 *   4) node->node dom/pdom is computed on-demand -- see algorithm for properly(Post)Dominates
 */ 
public class DominatorRelation {
	/** Provides local and callsite (CS) level (post)dominance info for a node */
	public static class NodeDomData {
		/** Intra-proc stmt dominators, non-strict (reflexive: n D n) */
		private BitSet localDom = null;
		/** Intra-proc stmt postdominators, non-strict (reflexive: n PD n) */
		private BitSet localPDom = null;
		private boolean dominatesExit; // whether this stmt dominates the (virtual) exit of the method
//		private ArrayList<CallSite> entryCSLocalProperPDomSet = null;
//		private ArrayList<CallSite> exitCSLocalProperDomSet = null;
		
		/** Intra-proc stmt dominators, non-strict (reflexive: n D n) */
		public BitSet getLocalDom() { return localDom; }
		/** Intra-proc stmt postdominators, non-strict (reflexive: n PD n) */
		public BitSet getLocalPDom() { return localPDom; }
		public void setLocalDom(BitSet dom) { this.localDom = dom; }
		public void setLocalPDom(BitSet pdom) { this.localPDom = pdom; }
		public boolean getDominatesExit() { return dominatesExit; }
		public void setDominatesExit(boolean domsExit) { this.dominatesExit = domsExit; }
		
//		// for entries and exits only
//		public ArrayList<CallSite> getEntryCSLocalProperPDomSet() { return entryCSLocalProperPDomSet; }
//		public void setEntryCSLocalProperPDomSet(ArrayList<CallSite> csPDomSet) { this.entryCSLocalProperPDomSet = csPDomSet; }
//		public ArrayList<CallSite> getExitCSLocalProperDomSet() { return exitCSLocalProperDomSet; }
//		public void setExitCSLocalProperDomSet(ArrayList<CallSite> csDomSet) { this.exitCSLocalProperDomSet = csDomSet; }
	}
	
	/** Singleton instance */
	private static DominatorRelation singletonInstance = null;
	/** Singleton instance accessor */
	public static DominatorRelation inst() { return singletonInstance; }
	/**
	 * Singleton instance creator. Singleton is not created until all required information has been computed:
	 *   soot analysis, global graph, reachability
	 */
	public static void createInstance() { singletonInstance = new DominatorRelation(); }
	
	/** Attaches to each CFG node a data structure with node- and CS-level dom/pdom info */
	private Map<CFGNode,NodeDomData> nodesDomData = new HashMap<CFGNode, NodeDomData>();
	
	/** Accessor to dom data structure for a node */
	public NodeDomData getDomData(CFGNode n) { return nodesDomData.get(n); }
	
	/** Private default constructor */
	private DominatorRelation() {
		if (Options.dominance())
			computeInterprocDom();
		else
			computeIntraprocDom(ProgramFlowGraph.inst().getCFGs());
	}
	
	private void computeLocalDom(CFG cfg) {
		// get list of stmts so we can traverse it in both directions
		List<CFGNode> nodeList = cfg.getNodes();
		final int numNodes = nodeList.size();
		
		// init dom/pdom sets
		BitSet full = new BitSet(numNodes);
		full.flip(0, numNodes); // all bits are 1
		int nId = 0;
		for (CFGNode n : nodeList) {
			// first of all, create dom/pdom data for this node
			NodeDomData nDomData = new NodeDomData();
			nodesDomData.put(n, nDomData);
			
			if (!n.isInCatchBlock()) {  // exclude stmts in catch blocks, for now
				// initial dom set is full ("top"), except for entry which is seeded with itself only
				if (nId == 0) {  // entry
					BitSet entryDom = new BitSet(numNodes);
					entryDom.set(nId);
					nDomData.setLocalDom(entryDom);
				}
				else
					nDomData.setLocalDom((BitSet)full.clone());
				
				// initial pdom set is full ("top"), except for exit which is seeded with itself only
				if (n.getSuccs().isEmpty())  {  // it's the EXIT or a throw
					BitSet exitDom = new BitSet(numNodes);
					exitDom.set(nId);
					nDomData.setLocalPDom(exitDom);
				}
				else
					nDomData.setLocalPDom((BitSet)full.clone());
			}
			
			++nId;
		}
		
		// iterate dom forward until reaching fixed point
		boolean fixedPoint = false;
		while (!fixedPoint) {
			fixedPoint = true;
			nId = 0;
			for (CFGNode n : nodeList) {
				NodeDomData nDomData = nodesDomData.get(n);
				
				if (!n.isInCatchBlock()) {
					// intersect dom set with each predecessor's dom set
					BitSet domSet = nDomData.getLocalDom();
					BitSet oldDomSet = (BitSet) domSet.clone();
					for (CFGNode nPred : n.getPreds()) {
						NodeDomData nDomDataPred = nodesDomData.get(nPred);
						if (!nPred.isInCatchBlock()) {
							BitSet predDom = nDomDataPred.getLocalDom();
							domSet.and(predDom);
						}
					}
					domSet.set(nId); // gen
					if (!domSet.equals(oldDomSet))
						fixedPoint = false;
				}
				++nId;
			}
		}
		
		// build reverse list for backwards pdom propagation
		List<CFGNode> revNodeList = new ArrayList<CFGNode>(numNodes);
		for (nId = 0; nId < numNodes; ++nId)
			revNodeList.add(nodeList.get(numNodes - nId - 1));
		
		// iterate pdom backwards until reaching fixed point
		fixedPoint = false;
		while (!fixedPoint) {
			fixedPoint = true;
			nId = numNodes - 1;
			for (CFGNode n : revNodeList) {
				NodeDomData nDomData = nodesDomData.get(n);
				
				if (!n.isInCatchBlock()) {
					// intersect pdom set with each successor's pdom set
					BitSet pdomSet = nDomData.getLocalPDom();
					BitSet oldPDomSet = (BitSet) pdomSet.clone();
					for (CFGNode nSucc : n.getSuccs()) {
						NodeDomData nDomDataSucc = nodesDomData.get(nSucc);
						if (!nSucc.isInCatchBlock()) {
							BitSet succPDom = nDomDataSucc.getLocalPDom();
							pdomSet.and(succPDom);
						}
					}
					pdomSet.set(nId); // gen
					if (!pdomSet.equals(oldPDomSet))
						fixedPoint = false;
				}
				--nId;
			}
		}
	}
	
	/**
	 * TODO: fix algorithm using traditional dom/pdom propagation on callsite graph
	 * 
	 * Computes dominators and post-dominators at the interprocedural level.
	 * 
	 * Algorithm for global (inter-proc) dominators:
	 *   1) compute local (intra-proc) dom stmt->stmt, and local dom callsite->callsite
	 *   2) compute global dom method->method; a call site is divided in 'call' and 'ret' nodes
	 *      Distinguish method-entry and method-exit dominators:
	 *        EDom(entry_m) = {m} U inters(EDom(call that invokes m))
	 *        EDom(call), EDom(ret) and EDom(exit_m) obtained by dom algorithm on call site graph
	 *        
	 *        XDom(entry_m) = inters(XDom(call that invokes m))
	 *        XDom(call), XDom(ret) and XDom(exit_m) obtained by dom algorithm on call site graph
	 *        XDom(exit_m) also contains m
	 *        
	 *        -- initialize edom and xdom sets to ALL methods, except for program's entry method
	 *        -- see more details in method-level dominator computation
	 *   3) compute global dom method->stmt
	 */
	private void computeInterprocDom() {
		System.out.println("Computing inter-proc dominance and post-dominance");
		
		// verify that there is an entry method
		// USE FIRST ENTRY METHOD, FOR NOW
		assert ProgramFlowGraph.inst().getEntryMethods().size() == 1;
		SootMethod mEntry = ProgramFlowGraph.inst().getEntryMethods().get(0);
		
		HashSet<CFG> allReachableCFGs = new HashSet<CFG>(ProgramFlowGraph.inst().getCFGs());
		HashSet<SootMethod> allReachableMethods = new HashSet<SootMethod>(ProgramFlowGraph.inst().getReachableAppMethods());
		
		// 1.1) local stmt->stmt
		computeIntraprocDom(allReachableCFGs);
		
		// create global index stmt->callsite
		HashMap<Stmt, CallSite> csIndex = new HashMap<Stmt, CallSite>();
		for (CFG cfg : allReachableCFGs) {
			MethodTag mTag = (MethodTag) cfg.getMethod().getTag(MethodTag.TAG_NAME);
			for (CallSite cs : mTag.getCallSites())
				if (cs.hasAppCallees())
					csIndex.put(cs.getLoc().getStmt(), cs);
		}
		
		// 1.2) cs->cs, local
		computeLocalCGNodesProperDom(csIndex);
		
		// 2) method->method and method->cs, global
		//    -- initialize method (entry) dom sets to ALL methods, except for entry which is dominated only by itself;
		initDomSets(mEntry, allReachableMethods);
		
		HashMap<SootMethod, CallSiteGraph> csGraphs = new HashMap<SootMethod, CallSiteGraph>();
		for (SootMethod m : allReachableMethods) {
			CallSiteGraph csGraph = new CallSiteGraph(m);
			csGraph.analyze();
			csGraphs.put(m, csGraph);
		}
		
		// iteratively apply equations to method, call and ret nodes, for both edom and xdom sets
		// Note: we might want to separate edom and xdom computation in different loops, because a modification to one
		//       set adds affected methods to the worklist to be revisited later, even if the other set converged
		HashSet<SootMethod> worklist = (HashSet<SootMethod>)allReachableMethods.clone();
		int iterCount = 0;
		while (!worklist.isEmpty()) {
			// DEBUG
			System.out.println("DOM iteration # " + iterCount + ", worklist size " + worklist.size());
			++iterCount;
			
			for (SootMethod m : (Collection<SootMethod>)worklist.clone()) {
				// DEBUG
//				System.out.println("DOM visiting " + m);
				
				MethodTag mTag = (MethodTag) m.getTag(MethodTag.TAG_NAME);
				final int mIdx = ProgramFlowGraph.inst().getMethodIdx(m);
				
				// build list of direct caller sites that are reachable from program's entry
				ArrayList<CallSite> csCallers = mTag.getCallerSites();
				ArrayList<CallSite> csReachableCallers = new ArrayList<CallSite>();
				for (CallSite cs : csCallers) {
					if (cs.isReachableFromEntry())
						csReachableCallers.add(cs);
				}
				
				// iterate on this method until convergence, since there might be recursive calls
				// (also, call sites are not necessarily visited in the ideal dom tree order)
				while (worklist.contains(m)) {  // it will be true at least the first time
					worklist.remove(m);
					
					// retrieve entry's global edom and xdom sets, to modify them
					BitSet mGlobalEDomSet = mTag.getGlobalEDomSetEntry();
					BitSet mGlobalXDomSet = mTag.getGlobalXDomSetEntry();
					
					// 2.1) EDom(entry_m) = {m} U inters(EDom(call that invokes m))
					//      XDom(entry_m) = inters(XDom(call that invokes m))
					boolean modified = false;
					if (csReachableCallers.isEmpty()) {  // no callers, so ensure edom and xdom at entry are minimal
						if (mGlobalEDomSet.cardinality() != 1 || !mGlobalXDomSet.isEmpty())
							modified = true;
						
						mGlobalEDomSet.clear();
						mGlobalEDomSet.set(mIdx);
						
						mGlobalXDomSet.clear();
					}
					else {
						BitSet oldmGlobalEDomSet = (BitSet) mGlobalEDomSet.clone();
						BitSet oldmGlobalXDomSet = (BitSet) mGlobalXDomSet.clone();
						
						// intersect edom and xdom sets with sets from callers
						for (CallSite csCaller : csReachableCallers) {
							mGlobalEDomSet.and(csCaller.getGlobalEDomSetCall());
							mGlobalXDomSet.and(csCaller.getGlobalXDomSetCall());
						}
						// for edom, add entry
						mGlobalEDomSet.set(mIdx); 
						
						// check for modifications
						if (!mGlobalEDomSet.equals(oldmGlobalEDomSet) || !mGlobalXDomSet.equals(oldmGlobalXDomSet))
							modified = true;
					}
					if (modified)  // all method's callees are affected by change in edom/xdom of entry
						worklist.addAll(mTag.getAppCalleeMethods()); // all callees are reachable from the entry
					
					// 2.2) EDom(call) = inters(EDom(predecessor ret or entry))
					//      XDom(call) = inters(XDom(predecessor ret or entry))
					//      EDom(exit) = inters(EDom(predecessor ret or entry))
					//      XDom(exit) = {m} U inters(XDom(predecessor ret or entry))
					// 2.3) EDom(ret) = EDom(call) U inters(EDom(exit_m called by cs))
					//      XDom(ret) = XDom(call) U inters(XDom(exit_m called by cs))
					CallSiteGraph csGraph = csGraphs.get(m);
					worklist.addAll(propagateCSGraphDom(mTag, csGraph));
				}
			}
		}
		
		// iteratively apply equations to method, call and ret nodes, for both epdom and xpdom sets
		// Note: we might want to separate epdom and xpdom computation in different loops, because a modification to one
		//       set adds affected methods to the worklist to be revisited later, even if the other set converged
		worklist = (HashSet<SootMethod>)allReachableMethods.clone();
		iterCount = 0;
		while (!worklist.isEmpty()) {
			// DEBUG
			System.out.println("PDOM iteration # " + iterCount + ", worklist size " + worklist.size());
			++iterCount;
			
			for (SootMethod m : (Collection<SootMethod>)worklist.clone()) {
				// DEBUG
//				System.out.println("PDOM visiting " + m);
				
				MethodTag mTag = (MethodTag) m.getTag(MethodTag.TAG_NAME);
				final int mIdx = ProgramFlowGraph.inst().getMethodIdx(m);
				
				// build list of direct caller sites that are reachable from program's entry
				ArrayList<CallSite> csCallers = mTag.getCallerSites();
				ArrayList<CallSite> csReachableCallers = new ArrayList<CallSite>();
				for (CallSite cs : csCallers) {
					if (cs.isReachableFromEntry())
						csReachableCallers.add(cs);
				}
				
				// iterate on this method until convergence, since there might be recursive calls
				// (also, call sites are not necessarily visited in the ideal pdom tree order)
				while (worklist.contains(m)) {  // it will be true at least the first time
					worklist.remove(m);
					
					// retrieve exit's global epdom and xpdom sets, to modify them
					BitSet mGlobalXPDomSet = mTag.getGlobalXPDomSetExit();
					BitSet mGlobalEPDomSet = mTag.getGlobalEPDomSetExit();
					
					// 2.1) XPDom(exit_m) = {m} U inters(XPDom(call that invokes m))
					//      EPDom(exit_m) = inters(EPDom(call that invokes m))
					boolean modified = false;
					if (csReachableCallers.isEmpty()) {  // no callers, so ensure epdom and xpdom at exit are minimal
						if (mGlobalXPDomSet.cardinality() != 1 || !mGlobalEPDomSet.isEmpty())
							modified = true;
						
						mGlobalXPDomSet.clear();
						mGlobalXPDomSet.set(mIdx);
						
						mGlobalEPDomSet.clear();
					}
					else {
						BitSet oldmGlobalXPDomSet = (BitSet) mGlobalXPDomSet.clone();
						BitSet oldmGlobalEPDomSet = (BitSet) mGlobalEPDomSet.clone();
						
						// intersect xpdom and epdom sets with sets from callers' return points
						for (CallSite csCaller : csReachableCallers) {
							mGlobalXPDomSet.and(csCaller.getGlobalXPDomSetRet());
							mGlobalEPDomSet.and(csCaller.getGlobalEPDomSetRet());
						}
						// for xpdom, add exit
						mGlobalXPDomSet.set(mIdx);
						
						// check for modifications
						if (!mGlobalXPDomSet.equals(oldmGlobalXPDomSet) || !mGlobalEPDomSet.equals(oldmGlobalEPDomSet))
							modified = true;
					}
					if (modified)  // all method's callees are affected by change in xpdom/epdom of exit
						worklist.addAll(mTag.getAppCalleeMethods()); // all callees are reachable from the entry
					
					// 2.2) XPDom(ret) = inters(XPDom(successor call or exit))
					//      EPDom(ret) = inters(EPDom(successor call or exit))
					//      XPDom(entry) = inters(XPDom(successor call or exit))
					//      EPDom(entry) = {m} U inters(EPDom(successor call or exit))
					// 2.3) XPDom(call) = XPDom(ret) U inters(XPDom(entry_m called by cs))
					//      EPDom(call) = EPDom(ret) U inters(EPDom(entry_m called by cs))
					CallSiteGraph csGraph = csGraphs.get(m);
					worklist.addAll(propagateCSGraphPDom(mTag, csGraph));
				}
			}
		}
	}
	
	private void computeIntraprocDom(Collection<CFG> allReachableCFGs) {
		for (CFG cfg : allReachableCFGs)
			computeLocalDom(cfg);
	}
	
	/** 
	 * Computes for all methods local proper (post)domination of call sites, entries and exits by call sites.
	 * It does so by simply checking already-computed stmt dominance.
	 */
	private void computeLocalCGNodesProperDom(HashMap<Stmt, CallSite> csIndex) {
		for (SootMethod m : ProgramFlowGraph.inst().getReachableAppMethods()) {
			MethodTag mTag = (MethodTag) m.getTag(MethodTag.TAG_NAME);
			CFG cfg = ProgramFlowGraph.inst().getMethodCFG(m);
			List<CFGNode> nodes = cfg.getNodes();
			final int totalCFGNodes = nodes.size();
			
			// select call sites that dom/pdom other call sites, and store these sets in call sites themselves
			for (CallSite cs : mTag.getCallSites()) {
				if (!cs.hasAppCallees())
					continue;
				
				CFGNode nCS = cfg.getNode( cs.getLoc().getStmt() );
				NodeDomData nDomDataCS = nodesDomData.get(nCS);
				// dominators of call site
				ArrayList<CallSite> csProperDomSet = new ArrayList<CallSite>();
				BitSet localDom = nDomDataCS.getLocalDom();
				for (int nDomId = 0; nDomId < totalCFGNodes; ++nDomId) {
					if (localDom.get(nDomId)) {
						CFGNode nDom = nodes.get(nDomId);
						Stmt sDom = nDom.getStmt();
						if (nDom != nCS && csIndex.containsKey(sDom)) {  // dom is a call-site (but not the same call-site)
							assert sDom != null;
							csProperDomSet.add(csIndex.get(sDom));
						}
					}
				}
				cs.setCSLocalProperDomSet(csProperDomSet);
				// post-dominators of call site
				ArrayList<CallSite> csProperPDomSet = new ArrayList<CallSite>();
				BitSet localPDom = nDomDataCS.getLocalPDom();
				for (int nPDomId = 0; nPDomId < totalCFGNodes; ++nPDomId) {
					if (localPDom.get(nPDomId)) {
						CFGNode nPDom = nodes.get(nPDomId);
						Stmt sPDom = nPDom.getStmt();
						if (nPDom != nCS && csIndex.containsKey(sPDom)) {  // pdom is a call-site (but not the same call-site)
							assert sPDom != null;
							csProperPDomSet.add(csIndex.get(sPDom));
						}
					}
				}
				cs.setCSLocalProperPDomSet(csProperPDomSet);
			}
			
//			// do the same for dom set of exit stmts
//			NodeDomData nDomDataExit = nodesDomData.get( cfg.EXIT );
//			assert !cfg.EXIT.isInCatchBlock();
//			ArrayList<CallSite> csProperDomSet = new ArrayList<CallSite>();
//			BitSet exitLocalDom = nDomDataExit.getLocalDom();
//			for (int nDomId = 0; nDomId < exitLocalDom.size(); ++nDomId) {
//				if (exitLocalDom.get(nDomId)) {
//					CFGNode nDom = nodes.get(nDomId);
//					Stmt sDom = nDom.getStmt();
//					if (nDom != cfg.EXIT && csIndex.containsKey(sDom)) {  // dom is not node itself and is a call site
//						assert sDom != null;
//						csProperDomSet.add(csIndex.get(sDom));
//					}
//				}
//			}
//			nDomDataExit.setExitCSLocalProperDomSet(csProperDomSet);
//			
//			// do the same for pdom set of method's entry stmt
//			NodeDomData nDomDataEntry = nodesDomData.get( cfg.ENTRY );
//			ArrayList<CallSite> csProperPDomSet = new ArrayList<CallSite>();
//			BitSet entryLocalPDomSet = nDomDataEntry.getLocalPDom();
//			for (int nPDomId = 0; nPDomId < entryLocalPDomSet.size(); ++nPDomId) {
//				if (entryLocalPDomSet.get(nPDomId)) {  // stmt post-dominates entry
//					CFGNode nPDom = nodes.get(nPDomId);
//					Stmt sPDom = nPDom.getStmt();
//					if (nPDom != cfg.ENTRY && csIndex.containsKey(sPDom))  // stmt is a call site, and not the entry stmt
//						csProperPDomSet.add(csIndex.get(sPDom));
//				}
//			}
//			nDomDataEntry.setEntryCSLocalProperPDomSet(csProperPDomSet);
		}
	}
	
	/**
	 * Initializes entry/exit dom/pdom sets to over-estimates of actual dom/pdom sets
	 * 
	 * Entry Dom: traditional dominator algorithm at the method-level call graph (i.e., call graph with just methods as nodes)
	 *   EDom(m-entry) = {m} U inters(EDom(m'-entry for m' that calls m))
	 *   EDom(call) = EDom(ret) = EDom(m-exit) = {ALL methods}
	 *   
	 * Exit Dom:
	 *   XDom(m-exit) = { m' | m' is reachable by m }  -- this is a gross over-approximation, but works as seed
	 *   XDom(call) = XDom(ret) = XDom(m-entry) = {ALL methods}
	 *   
	 * Entry PDom:
	 *   EPDom(m-entry) = { m' | m' is reachable by m }  -- this is a gross over-approximation, but works as seed
	 *   EPDom(call) = EPDom(ret) = EPDom(m-exit) = {ALL methods}
	 * 
	 * Exit PDom: traditional pdom algorithm at the method-level call graph (i.e., call graph with just methods as nodes)
	 *   XPDom(m-exit) = {m} U inters(XPDom(m'-exit for m' that calls m))
	 *   XPDom(call) = XPDom(ret) = XPDom(m-entry) = {ALL methods}
	 */
	private void initDomSets(SootMethod entryM, HashSet<SootMethod> allReachableMethods) {
		//
		// First part: entry dominators
		// * Entry Dom: traditional dominator algorithm at the method-level call graph (i.e., call graph with just methods as nodes)
		// *   EDom(m-entry) = {m} U inters(EDom(m'-entry for m' that calls m))
		
		final int numMethods = ProgramFlowGraph.inst().getReachableAppMethods().size();
		final BitSet fullBitset = new BitSet(numMethods);
		fullBitset.set(0, numMethods - 1);
		final int entryMIdx = ProgramFlowGraph.inst().getMethodIdx(entryM);
		
		// initialize edom sets to ALL methods, except for:
		//   program's entry method's edom = {entry_method}, and xdom = {}
		for (SootMethod m : allReachableMethods) {
			BitSet initEDomSet;
			if (m == entryM) {
				initEDomSet = new BitSet(numMethods);
				initEDomSet.set(entryMIdx);
			}
			else
				initEDomSet = (BitSet) fullBitset.clone();
			
			MethodTag mTag = (MethodTag) m.getTag(MethodTag.TAG_NAME);
			mTag.setGlobalEDomSetEntry(initEDomSet);
		}
		
		// propagate edom sets of method-entries until convergence
		HashSet<SootMethod> worklist = (HashSet<SootMethod>) allReachableMethods.clone();
		while (!worklist.isEmpty()) {
			for (SootMethod m : (HashSet<SootMethod>) worklist.clone()) {  // propagate from predecessors in call-graph
				worklist.remove(m);
				
				MethodTag mTag = (MethodTag) m.getTag(MethodTag.TAG_NAME);
				BitSet edomSetEntry = mTag.getGlobalEDomSetEntry();
				BitSet oldedomSetEntry = (BitSet) edomSetEntry.clone();
				
				// intersect edom set at entry with edom sets of callers at their entries
				for (SootMethod mPred : mTag.getCallerMethods()) {
					MethodTag mPredTag = (MethodTag) mPred.getTag(MethodTag.TAG_NAME);
					if (mPredTag.isReachableFromEntry())
						edomSetEntry.and(mPredTag.getGlobalEDomSetEntry());
				}
				edomSetEntry.set(ProgramFlowGraph.inst().getMethodIdx(m)); // add m
				if (!edomSetEntry.equals(oldedomSetEntry))
					worklist.addAll(mTag.getAppCalleeMethods());
			}
		}
		
		// finally, init exit, call and ret edom to the set of all methods
		// *   EDom(call) = EDom(ret) = EDom(m-exit) = {ALL methods}
		for (SootMethod m : allReachableMethods) {
			MethodTag mTag = (MethodTag) m.getTag(MethodTag.TAG_NAME);
			
			for (CallSite cs : mTag.getCallSites()) {
				if (!cs.hasAppCallees())
					continue;
				cs.setGlobalEDomSetCall((BitSet)fullBitset.clone());
				cs.setGlobalEDomSetRet((BitSet)fullBitset.clone());
			}
			mTag.setGlobalEDomSetExit((BitSet)fullBitset.clone());
		}
		
		//
		// Second part: exit dominators
		// * Exit Dom:
		// *   XDom(m-exit) = { m' | m' is reachable by m }  -- this is a gross over-approximation, but works as seed
		// *   XDom(call) = XDom(ret) = XDom(m-entry) = {ALL methods}
		for (SootMethod m : allReachableMethods) {
			MethodTag mTag = (MethodTag) m.getTag(MethodTag.TAG_NAME);
			mTag.setGlobalXDomSetExit(mTag.getAllReachedAppMtds()); // getAllReachedAppMtds always returns a *new* set
			mTag.setGlobalXDomSetEntry((BitSet) fullBitset.clone());
			for (CallSite cs : mTag.getCallSites()) {
				if (!cs.hasAppCallees())
					continue;
				cs.setGlobalXDomSetCall((BitSet) fullBitset.clone());
				cs.setGlobalXDomSetRet((BitSet) fullBitset.clone());
			}
		}
		
		//
		// Third part: entry post-dominators
		// * Entry PDom:
		// *   EPDom(m-entry) = { m' | m' is reachable by m }  -- this is a gross over-approximation, but works as seed
		// *   EPDom(call) = EPDom(ret) = EPDom(m-exit) = {ALL methods}
		for (SootMethod m : allReachableMethods) {
			MethodTag mTag = (MethodTag) m.getTag(MethodTag.TAG_NAME);
			mTag.setGlobalEPDomSetEntry(mTag.getAllReachedAppMtds()); // getAllReachedAppMtds always returns a *new* set
			mTag.setGlobalEPDomSetExit((BitSet) fullBitset.clone());
			for (CallSite cs : mTag.getCallSites()) {
				if (!cs.hasAppCallees())
					continue;
				cs.setGlobalEPDomSetCall((BitSet) fullBitset.clone());
				cs.setGlobalEPDomSetRet((BitSet) fullBitset.clone());
			}
		}
		
		//
		// Fourth part: exit post-dominators
		// * Exit PDom: traditional pdom algorithm at the method-level call graph (i.e., call graph with just methods as nodes)
		// *   XPDom(m-exit) = {m} U inters(XPDom(m'-exit for m' that calls m))
		
		// initialize xpdom sets to ALL methods, except for:
		//   program's exit method's xpdom: {exit_method}
		for (SootMethod m : allReachableMethods) {
			BitSet initXPDomSet;
			if (m == entryM) {
				initXPDomSet = new BitSet(numMethods);
				initXPDomSet.set(entryMIdx);
			}
			else
				initXPDomSet = (BitSet) fullBitset.clone();
			
			MethodTag mTag = (MethodTag) m.getTag(MethodTag.TAG_NAME);
			mTag.setGlobalXPDomSetExit(initXPDomSet);
		}
		
		// propagate until convergence
		//   XPDom of method-exits
		worklist = (HashSet<SootMethod>) allReachableMethods.clone();
		while (!worklist.isEmpty()) {
			for (SootMethod m : (HashSet<SootMethod>) worklist.clone()) {  // propagate from successors in call-graph
				worklist.remove(m);
				
				MethodTag mTag = (MethodTag) m.getTag(MethodTag.TAG_NAME);
				BitSet xpdomSetExit = mTag.getGlobalXPDomSetExit();
				BitSet oldxpdomSetExit = (BitSet) xpdomSetExit.clone();
				
				// intersect xpdom set at exit with xpdom sets of callers at their exits
				for (SootMethod mPred : mTag.getCallerMethods()) {
					MethodTag mPredTag = (MethodTag) mPred.getTag(MethodTag.TAG_NAME);
					if (mPredTag.isReachableFromEntry())
						xpdomSetExit.and(mPredTag.getGlobalXPDomSetExit());
				}
				xpdomSetExit.set(ProgramFlowGraph.inst().getMethodIdx(m)); // add m
				if (!xpdomSetExit.equals(oldxpdomSetExit))
					worklist.addAll(mTag.getAppCalleeMethods());
			}
		}
		
		// finally, init entry, call and ret xpdom to all methods
		// *   XPDom(call) = XPDom(ret) = XPDom(m-entry) = {ALL methods}
		for (SootMethod m : allReachableMethods) {
			MethodTag mTag = (MethodTag) m.getTag(MethodTag.TAG_NAME);
			
			for (CallSite cs : mTag.getCallSites()) {
				if (!cs.hasAppCallees())
					continue;
				cs.setGlobalXPDomSetCall((BitSet)fullBitset.clone());
				cs.setGlobalXPDomSetRet((BitSet)fullBitset.clone());
			}
			mTag.setGlobalXPDomSetEntry((BitSet)fullBitset.clone());
		}
		
		// DEBUG
//		System.out.println("Method-level DOM --");
//		for (SootMethod m : allReachableMethods) {
//			MethodTag mTag = (MethodTag) m.getTag(MethodTag.TAG_NAME);
//			System.out.println("  " + m + ": " + mTag.getGlobalEDomSetEntry().size() + " " + mTag.getGlobalEDomSetEntry());
//		}
	}
	
	/** Always returns a newly created set */
	private HashSet<SootMethod> intersectSets(Collection<HashSet<SootMethod>> sets) {
		if (sets.isEmpty())
			return new HashSet<SootMethod>();
		
		Iterator<HashSet<SootMethod>> itSet = sets.iterator();
		HashSet<SootMethod> inters = (HashSet<SootMethod>) itSet.next().clone();
		
		while (itSet.hasNext()) {
			HashSet<SootMethod> mSet = itSet.next();
			for (SootMethod m : (HashSet<SootMethod>)inters.clone()) {
				if (!mSet.contains(m))
					inters.remove(m);
			}
		}
		
		return inters;
	}
	
	/** Always returns a newly created set */
	private HashSet<SootMethod> unionSets(Collection<HashSet<SootMethod>> sets) {
		HashSet<SootMethod> union = new HashSet<SootMethod>();
		for (HashSet<SootMethod> set : sets)
			union.addAll(set);
		
		return union;
	}
	
	/**
	 * Computes edom/xdom for each call site and exit.
	 * 2.2) EDom(call) = inters(EDom(predecessor ret or entry))
	 * 		XDom(call) = inters(XDom(predecessor ret or entry))
	 * 		EDom(exit) = inters(EDom(predecessor ret or entry))
	 * 		XDom(exit) = {m} U inters(XDom(predecessor ret or entry))
	 * 2.3) EDom(ret) = EDom(call) U inters(EDom(exit_m called by cs))
	 * 		XDom(ret) = XDom(call) U inters(XDom(exit_m called by cs))
	 */
	private Collection<SootMethod> propagateCSGraphDom(MethodTag mTag, CallSiteGraph csGraph) {
		HashSet<SootMethod> mAffected = new HashSet<SootMethod>();
		final int mIdx = ProgramFlowGraph.inst().getMethodIdx(mTag.getMethod());
		
		CFGNode entryN = csGraph.ENTRY;
		CFGNode exitN = csGraph.EXIT;
		
		boolean fixedPoint = false;
		while (!fixedPoint) {
			fixedPoint = true;
			
			for (CFGNode n : csGraph.getNodes()) {
				if (n == entryN)
					continue; // skip entry
				
				CallSite cs;
				BitSet edomCallOrExit, xdomCallOrExit;
				if (n == exitN) {
					cs = null;
					edomCallOrExit = mTag.getGlobalEDomSetExit();
					xdomCallOrExit = mTag.getGlobalXDomSetExit();
				}
				else {
					cs = ((StmtTag) n.getStmt().getTag(StmtTag.TAG_NAME)).getAppCallSite();
					edomCallOrExit = cs.getGlobalEDomSetCall();
					xdomCallOrExit = cs.getGlobalXDomSetCall();
				}
				
				// 2.2) EDom(call) = inters(EDom(predecessor ret or entry))
				//      XDom(call) = inters(XDom(predecessor ret or entry))
				//      EDom(exit) = inters(EDom(predecessor ret or entry))
				//      XDom(exit) = {m} U inters(XDom(predecessor ret or entry))
				BitSet oldedomCall = (BitSet) edomCallOrExit.clone();
				BitSet oldxdomCall = (BitSet) xdomCallOrExit.clone();
				// intersect edom and xdom sets of predecessor nodes
				for (CFGNode predN : n.getPreds()) {
					if (predN == entryN) {
						edomCallOrExit.and(mTag.getGlobalEDomSetEntry());
						xdomCallOrExit.and(mTag.getGlobalXDomSetEntry());
					}
					else {
						CallSite csPred = ((StmtTag)predN.getStmt().getTag(StmtTag.TAG_NAME)).getAppCallSite();
						edomCallOrExit.and(csPred.getGlobalEDomSetRet());
						xdomCallOrExit.and(csPred.getGlobalXDomSetRet());
					}
				}
				// exit's xdom also contains the method's exit itself
				if (n == exitN)
					xdomCallOrExit.set(mIdx);
				
				// check modifications and affected methods
				if (!edomCallOrExit.equals(oldedomCall) || !xdomCallOrExit.equals(oldxdomCall)) {
					fixedPoint = false;
					if (n == exitN) {
						// caller methods are affected; filter out unreachable callers
						for (SootMethod mCaller : mTag.getCallerMethods()) {
							MethodTag mCallerTag = (MethodTag) mCaller.getTag(MethodTag.TAG_NAME);
							if (mCallerTag.isReachableFromEntry())
								mAffected.add(mCaller);
						}
					}
					else
						mAffected.addAll(cs.getAppCallees());
				}
				
				// 2.3) EDom(ret) = EDom(call) U inters(EDom(exit_m called by cs))
				//      XDom(ret) = XDom(call) U inters(XDom(exit_m called by cs))
				if (n != exitN) {
					BitSet edomRet = cs.getGlobalEDomSetRet();
					BitSet xdomRet = cs.getGlobalXDomSetRet();
					BitSet oldedomRet = fixedPoint? (BitSet) edomRet.clone() : null;
					BitSet oldxdomRet = fixedPoint? (BitSet) xdomRet.clone() : null;
					for (SootMethod mCallee : cs.getAppCallees()) {
						MethodTag mCalleeTag = (MethodTag) mCallee.getTag(MethodTag.TAG_NAME);
						edomRet.and(mCalleeTag.getGlobalEDomSetExit());
						xdomRet.and(mCalleeTag.getGlobalXDomSetExit());
					}
					edomRet.or(edomCallOrExit);
					xdomRet.or(xdomCallOrExit);
					
					if (fixedPoint && (!edomRet.equals(oldedomRet) || !xdomRet.equals(oldxdomRet)))
						fixedPoint = false;
				}
			}
		}
		
		return mAffected;
	}
	/**
	 * Computes xpdom/epdom for each call site and entry.
	 * 2.2) XPDom(ret) = inters(XPDom(successor call or exit))
	 * 		EPDom(ret) = inters(EPDom(successor call or exit))
	 * 		XPDom(entry) = inters(XPDom(successor call or exit))
	 * 		EPDom(entry) = {m} U inters(EPDom(successor call or exit))
	 * 2.3) XPDom(call) = XPDom(ret) U inters(XPDom(entry_m called by cs))
	 * 		EPDom(call) = EPDom(ret) U inters(EPDom(entry_m called by cs))
	 */
	private Collection<SootMethod> propagateCSGraphPDom(MethodTag mTag, CallSiteGraph csGraph) {
		HashSet<SootMethod> mAffected = new HashSet<SootMethod>();
		final int mIdx = ProgramFlowGraph.inst().getMethodIdx(mTag.getMethod());
		
		CFGNode entryN = csGraph.ENTRY;
		CFGNode exitN = csGraph.EXIT;
		
		boolean fixedPoint = false;
		while (!fixedPoint) {
			fixedPoint = true;
			
			for (CFGNode n : csGraph.getNodes()) {
				if (n == exitN)
					continue; // skip exit
				
				CallSite cs;
				BitSet xpdomRetOrEntry, epdomRetOrEntry;
				if (n == entryN) {
					cs = null;
					xpdomRetOrEntry = mTag.getGlobalXPDomSetEntry();
					epdomRetOrEntry = mTag.getGlobalEPDomSetEntry();
				}
				else {
					cs = ((StmtTag) n.getStmt().getTag(StmtTag.TAG_NAME)).getAppCallSite();
					xpdomRetOrEntry = cs.getGlobalXPDomSetRet();
					epdomRetOrEntry = cs.getGlobalEPDomSetRet();
				}
				
				// 2.2) XPDom(ret) = inters(XPDom(successor call or exit))
				//		EPDom(ret) = inters(EPDom(successor call or exit))
				// 		XPDom(entry) = inters(XPDom(successor call or exit))
				// 		EPDom(entry) = {m} U inters(EPDom(successor call or exit))
				BitSet oldxpdomRet = (BitSet) xpdomRetOrEntry.clone();
				BitSet oldepdomRet = (BitSet) epdomRetOrEntry.clone();
				// intersect xpdom and epdom sets of successor nodes
				for (CFGNode succN : n.getSuccs()) {
					if (succN == exitN) {
						xpdomRetOrEntry.and(mTag.getGlobalXPDomSetExit());
						epdomRetOrEntry.and(mTag.getGlobalEPDomSetExit());
					}
					else {
						CallSite csSucc = ((StmtTag)succN.getStmt().getTag(StmtTag.TAG_NAME)).getAppCallSite();
						xpdomRetOrEntry.and(csSucc.getGlobalXPDomSetCall());
						epdomRetOrEntry.and(csSucc.getGlobalEPDomSetCall());
					}
				}
				// entry's epdom also contains the method's entry itself
				if (n == entryN)
					epdomRetOrEntry.set(mIdx);
				
				// check modifications and affected methods
				if (!xpdomRetOrEntry.equals(oldxpdomRet) || !epdomRetOrEntry.equals(oldepdomRet)) {
					fixedPoint = false;
					if (n == entryN) {
						// caller methods are affected; filter out unreachable callers
						for (SootMethod mCaller : mTag.getCallerMethods()) {
							MethodTag mCallerTag = (MethodTag) mCaller.getTag(MethodTag.TAG_NAME);
							if (mCallerTag.isReachableFromEntry())
								mAffected.add(mCaller);
						}
					}
					else
						mAffected.addAll(cs.getAppCallees());
				}
				
				// 2.3) XPDom(call) = XPDom(ret) U inters(XPDom(entry_m called by cs))
				// 		EPDom(call) = EPDom(ret) U inters(EPDom(entry_m called by cs))
				if (n != entryN) {
					BitSet xpdomCall = cs.getGlobalXPDomSetCall();
					BitSet epdomCall = cs.getGlobalEPDomSetCall();
					BitSet oldxpdomCall = fixedPoint? (BitSet) xpdomCall.clone() : null;
					BitSet oldepdomCall = fixedPoint? (BitSet) epdomCall.clone() : null;
					for (SootMethod mCallee : cs.getAppCallees()) {
						MethodTag mCalleeTag = (MethodTag) mCallee.getTag(MethodTag.TAG_NAME);
						xpdomCall.and(mCalleeTag.getGlobalXPDomSetEntry());
						epdomCall.and(mCalleeTag.getGlobalEPDomSetEntry());
					}
					xpdomCall.or(xpdomRetOrEntry);
					epdomCall.or(epdomRetOrEntry);
					
					if (fixedPoint && (!xpdomCall.equals(oldxpdomCall) || !epdomCall.equals(oldepdomCall)))
						fixedPoint = false;
				}
			}
		}
		
		return mAffected;
	}
	
	/** 
	 *  Important: in our definition of interprocedural "proper" domination, neither s1 nor methods 
	 *  called by s1 are *properly* dominated by s1. Proper dominance is dominance by s1's bottom (exit point).
	 *  
	 *  First, we check locally whether s1 properly dominates s2. If not, and they are in the same method, then
	 *  s1 definitely does not dominate s2.
	 *  
	 *  Interprocedurally, if s1 properly dominates s2, then s1's method's entry must dominate s2,
	 *  because a method's entry always dominates the nodes inside the method, and dom is transitive.
	 *  Hence, a first requisite is that s1's method's entry is in s2's edom set.
	 *  After the requisite is met, we check whether s1 is in all paths from s1's method's entry to s2.
	 *  There are two cases satisfying this condition:
	 *    1) s1's method's exit dominates s2's method's entry: in this case, s1 must dominate s1's method exit.
	 *    2) s1's method's exit does not dom s2's method's entry: this implies that s2's method is reachable between
	 *    s1's method's entry and exit. Note that for s1 to dom s2, s1 must dom s2's method. Hence, s1 must
	 *    properly dom every local call site that reaches s2's method.
	 *  
	 *  We need reachability information to check 2). Fortunately, we already have computed interproc reachability.
	 */
	public boolean properlyDominates(Stmt s1, Stmt s2) {
		//
		// local case: s1 must locally properly dominate s2
		SootMethod m1 = ProgramFlowGraph.inst().getContainingMethod(s1);
		SootMethod m2 = ProgramFlowGraph.inst().getContainingMethod(s2);
		
		MethodTag m1Tag = (MethodTag) m1.getTag(MethodTag.TAG_NAME);
		MethodTag m2Tag = (MethodTag) m2.getTag(MethodTag.TAG_NAME);
		CFG cfg1 = ProgramFlowGraph.inst().getContainingCFG(s1);
		CFG cfg2 = ProgramFlowGraph.inst().getContainingCFG(s2);
		
		CFGNode n1 = cfg1.getNode(s1);
		CFGNode n2 = cfg2.getNode(s2);
		
		final int n1Id = cfg1.getNodeId(n1);
		
		if (m1 == m2)
			return s1 != s2 && nodesDomData.get(n2).getLocalDom().get(n1Id);
		
		// Inter-procedural case (i.e., m1 != m2)
		
		// TODO: misses case where a group of two or more call sites reach s2 and collectively dom s2, but not individually
		// build s2's edom set: start with s2's method entry edom set, and add properly dominating ret's edom sets
		BitSet properEDomS2 = (BitSet) m2Tag.getGlobalEDomSetEntry().clone();
		BitSet localDomS2 = nodesDomData.get(n2).getLocalDom();
		for (CallSite csMS2 : m2Tag.getCallSites()) {
			if (!csMS2.hasAppCallees())
				continue;
			Stmt csStmt = csMS2.getLoc().getStmt();
			CFGNode csNode = cfg2.getNode(csStmt);
			if (csStmt != s2 && localDomS2.get(cfg2.getNodeId(csNode)))  // cs properly dominates s2 (i.e., cs dominates s2's entry)
				properEDomS2.or(csMS2.getGlobalEDomSetRet());
		}
		
		// requisite is that m1's entry is in s2's edom set
		final int m1Idx = ProgramFlowGraph.inst().getMethodIdx(m1);
		if (!properEDomS2.get(m1Idx))
			return false;
		
		// case 1) m1's exit dominates m2's entry: in this case, s1 must dominate s1's method exit.
		if (m2Tag.getGlobalXDomSetEntry().get(m1Idx)) {
			NodeDomData nDomDataExit = nodesDomData.get(cfg1.EXIT);
			return nDomDataExit.getLocalDom().get(n1Id);
		}
		
		// case 2) m1's exit does not dom m2's entry: this implies that there is a (forward) path to m2 from some 
		//         point between m1's entry and exit. Note that for s1 to dom s2, s1 must dom m2. Hence, s1 must
		//         properly dom every local call site that forward-reaches m2.
		for (CallSite csM1 : m1Tag.getCallSites()) {
			if (!csM1.hasAppCallees())
				continue;
			Stmt sCSM1 = csM1.getLoc().getStmt();
			NodeDomData nDomDataCSM1 = nodesDomData.get( cfg1.getNode(sCSM1) );
			if (sCSM1 == s1 || !nDomDataCSM1.getLocalDom().get(n1Id)) {
				for (SootMethod mCallee : csM1.getAppCallees()) {
					// note that, if m2 does not forward reach itself, for our purposes mCallee == m2 means that we found a path through which s1 does not dom m2
					if (mCallee == m2 || ReachabilityAnalysis.forwardReaches(mCallee, m2))
						return false;  // failed: cs is not properly dominated by s1
				}
			}
		}
		return true;
	}
	
	/** 
	 *  Important: in our definition of interprocedural "proper" postdomination, neither s1 nor methods 
	 *  called by s1 are *properly* postdominated by s1. Proper postdominance is postdominance by s1's top (entry point).
	 *  
	 *  First, we check locally whether s1 properly postdominates s2. If not, and they are in the same method, then
	 *  s1 definitely does not postdominate s2.
	 *  
	 *  Interprocedurally, if s1 properly postdominates s2, then s1's method's exit must postdominate s2,
	 *  because a method's exit always postdominates the nodes inside the method, and pdom is transitive.
	 *  Hence, a first requisite is that s1's method's exit is in s2's xpdom set.
	 *  After the requisite is met, we check whether s1 is in all paths from s2 to s1's method's exit.
	 *  There are two cases satisfying this condition:
	 *    1) s1's method's entry postdominates s2's method's exit: in this case, s1 must postdominate s1's method entry.
	 *    2) s1's method's entry does not pdom s2's method's exit: this implies that s2's method is reachable between
	 *    s1's method's entry and exit. Note that for s1 to pdom s2, s1 must pdom s2's method. Hence, s1 must
	 *    properly pdom every local call site that reaches s2's method.
	 *  
	 *  We need reachability information to check 2). Fortunately, we already have computed interproc reachability.
	 */
	public boolean properlyPostdominates(Stmt s1, Stmt s2) {
		//
		// local: s1 must locally properly postdominate s2
		SootMethod m1 = ProgramFlowGraph.inst().getContainingMethod(s1);
		SootMethod m2 = ProgramFlowGraph.inst().getContainingMethod(s2);
		
		MethodTag m1Tag = (MethodTag) m1.getTag(MethodTag.TAG_NAME);
		MethodTag m2Tag = (MethodTag) m2.getTag(MethodTag.TAG_NAME);
		CFG cfg1 = ProgramFlowGraph.inst().getContainingCFG(s1);
		CFG cfg2 = ProgramFlowGraph.inst().getContainingCFG(s2);
		
		CFGNode n1 = cfg1.getNode(s1);
		CFGNode n2 = cfg2.getNode(s2);
		
		final int n1Id = cfg1.getNodeId(n1);
		
		if (m1 == m2)
			return s1 != s2 && nodesDomData.get(n2).getLocalPDom().get(n1Id);
		
		// Inter-procedural (i.e., m1 != m2)
		
		// TODO: misses case where a group of two or more call site ret's reach s2 and collectively pdom s2, but not individually
		// build s2's xpdom set: start with s2's method exit xpdom set, and add properly postdominating calls' xpdom sets
		BitSet properXPDomS2 = (BitSet) m2Tag.getGlobalXPDomSetExit().clone();
		BitSet localPDomS2 = nodesDomData.get(n2).getLocalPDom();
		for (CallSite csMS2 : m2Tag.getCallSites()) {
			if (!csMS2.hasAppCallees())
				continue;
			Stmt csStmt = csMS2.getLoc().getStmt();
			CFGNode csNode = cfg2.getNode(csStmt);
			if (csStmt != s2 && localPDomS2.get(cfg2.getNodeId(csNode)))  // cs properly pdoms s2 (i.e., cs pdoms s2's exit)
				properXPDomS2.or(csMS2.getGlobalXPDomSetCall());
		}
		
		// first requisite is that s1's method's exit is in s2's xpdom set
		final int m1Idx = ProgramFlowGraph.inst().getMethodIdx(m1);
		if (!properXPDomS2.get(m1Idx))
			return false;
		
		// case 1) s1's method's entry postdominates s2's method's exit: in this case, s1 must postdominate s1's method entry.
		if (m2Tag.getGlobalEPDomSetExit().get(m1Idx)) {
			// to dominate the exit, s1 must postdominate its own method's entry
			NodeDomData nDomDataExit = nodesDomData.get(cfg1.ENTRY);
			return nDomDataExit.getLocalPDom().get(n1Id);
		}
		
		// case 2) s1's method's entry does not pdom s2's method's exit: this implies that s2's method is reachable between
		//         s1's method's entry and exit. Note that for s1 to pdom s2, s1 must pdom s2's method. Hence, s1 must
		//         properly pdom every local call site that forward-reaches s2's method.
		for (CallSite csM1 : m1Tag.getCallSites()) {
			if (!csM1.hasAppCallees())
				continue;
			Stmt sCSM1 = csM1.getLoc().getStmt();
			NodeDomData nDomDataCSM1 = nodesDomData.get( cfg1.getNode(sCSM1) );
			if (sCSM1 == s1 || !nDomDataCSM1.getLocalPDom().get(n1Id)) {
				for (SootMethod mCallee : csM1.getAppCallees()) {
					// note that, if m2 does not forward reach itself, for our purposes mCallee == m2 means that we found a path through which s1 does not pdom m2
					if (mCallee == m2 || ReachabilityAnalysis.forwardReaches(mCallee, m2))
						return false;  // failed: cs is not properly postdominated by s1
				}
			}
		}
		return true;
	}
	
}
