package dua.method;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import soot.Local;
import soot.PatchingChain;
import soot.SootMethod;
import soot.Unit;
import soot.UnitBox;
import soot.jimple.IdentityStmt;
import soot.jimple.Stmt;
import soot.jimple.ThrowStmt;
import soot.tagkit.AttributeValueException;
import soot.tagkit.Tag;
import soot.toolkits.graph.Block;
import soot.toolkits.graph.BlockGraph;
import soot.toolkits.graph.BriefBlockGraph;
import dua.Options;
import dua.global.ProgramFlowGraph;
import dua.unit.Location;
import dua.unit.StmtTag;
import dua.util.Util;


public class MethodTag implements Tag {
	/** Compares method according to their global method id. */
	public static class MethodComparator implements Comparator<SootMethod> {
		public int compare(SootMethod m1, SootMethod m2) {
			if (!m1.hasTag(MethodTag.TAG_NAME) || !m2.hasTag(MethodTag.TAG_NAME))
				return m1.toString().compareTo(m2.toString());
			
			if (m1 == m2)
				return 0;
			
			return (ProgramFlowGraph.inst().getMethodIdx(m1) < 
						ProgramFlowGraph.inst().getMethodIdx(m2))? -1 : 1;
		}
	}
	
    public static String TAG_NAME = "mdf";
	
	// Fields (instance)
	private SootMethod m;
	private ArrayList<Stmt> stmtList;
	private HashMap<Stmt, Integer> revStmtMap;
    
    /** Stores exits (return nodes); the OUT sets are the out sets of the method */
	private List<Location> exitPoints;
	
	/** For now, doesn't includes call sites in catch blocks */
	private ArrayList<CallSite> callSites = new ArrayList<CallSite>(); // holds all call sites inside method, which represent call locations and callees
	private ArrayList<CallSite> callers = new ArrayList<CallSite>();
	
	private Local[] formalParams;
	private BlockGraph blockGraph;
	
	/** True if reachability analysis disabled; otherwise, initially false (may be set to true later) */
	private boolean reachableFromEntry = !Options.reachability();
	private BitSet bsForwardReachedAppMtds = new BitSet(); // 1 bit per app method
	private HashSet<CallSite> backwardReachedCallSites = new HashSet<CallSite>();
	private BitSet bsBackwardReachedAppMtds = new BitSet(); // 1 bit per app method
	
	private BitSet globalEDomSetEntry; // method entries that dominate this method's entry
	private BitSet globalEDomSetExit; // method entries that dominate this method's (virtual) exit
	private BitSet globalXDomSetEntry; // method (virtual) exits that dominate this method's entry
	private BitSet globalXDomSetExit; // method (virtual) exits that dominate this method's (virtual) exit
	
	private BitSet globalEPDomSetEntry; // method entries that pdom this method's entry
	private BitSet globalEPDomSetExit; // method entries that pdom this method's (virtual) exit
	private BitSet globalXPDomSetEntry; // method (virtual) exits that pdom this method's entry
	private BitSet globalXPDomSetExit; // method (virtual) exits that pdom this method's (virtual) exit
	
	private static boolean interprocReachabilityComputed = false;
	
	public SootMethod getMethod() { return m; }
	
	/** Provides list of exits (return stmts) in method. */
	public List<Location> getExits() { return exitPoints; }
	public ArrayList<CallSite> getCallSites() { return callSites; }
	public ArrayList<CallSite> getCallerSites() { return callers; }
	public void addCallerSite(CallSite cs) { callers.add(cs); }
	
	public Stmt getEntryStmt() { return (Stmt) m.retrieveActiveBody().getUnits().getFirst(); }
	public Stmt getStmt(int sId) { return stmtList.get(sId); }
	public int getStmtId(Stmt s) { return revStmtMap.get(s); }
	public Local[] getFormalParams() { return formalParams; }
	
	public static boolean isInterprocReachabilityComputed() { return interprocReachabilityComputed; }
	public static void setInterprocReachabilityComputed(boolean interprocReachabilityComputed) { MethodTag.interprocReachabilityComputed = interprocReachabilityComputed; }
	public boolean isLocalReachabilityComputed() { return ((StmtTag)getEntryStmt().getTag(StmtTag.TAG_NAME)).isLocalReachabilityComputed(); }
	public void markReachableFromEntry() { this.reachableFromEntry = true; }
	public boolean isReachableFromEntry() { return reachableFromEntry; }
//	public boolean addForwardReachedAppMtd(SootMethod mReachable) { return forwardReachedAppMtds.add(mReachable); }
	public boolean addForwardReachedAppMtds(BitSet bsMtdsReachable) {
		final int prevCardinality = bsForwardReachedAppMtds.cardinality();
		bsForwardReachedAppMtds.or(bsMtdsReachable);
		return prevCardinality < bsForwardReachedAppMtds.cardinality();
	}
	public BitSet getForwardReachedAppMtds() { return bsForwardReachedAppMtds; }
	public boolean addBackwardReachedCallSite(CallSite csReachable) { return backwardReachedCallSites.add(csReachable); }
	public boolean addBackwardReachedCallSites(Collection<CallSite> csReachable) { return backwardReachedCallSites.addAll(csReachable); }
	public HashSet<CallSite> getBackwardReachedCallSites() { return backwardReachedCallSites; }
	public void addBackwardReachedAppMtd(int mReachableId) { bsBackwardReachedAppMtds.set(mReachableId); }
	public boolean addBackwardReachedAppMtds(BitSet bsMtdReachable) {
		final int prevCardinality = bsBackwardReachedAppMtds.cardinality();
		bsBackwardReachedAppMtds.or(bsMtdReachable);
		return prevCardinality < bsBackwardReachedAppMtds.cardinality();
	}
	public BitSet getBackwardReachedAppMtds() { return bsBackwardReachedAppMtds; }
	/** Always creates and returns a new set containing all reachable methods (forward and backward) */
	public BitSet getAllReachedAppMtds() {
		BitSet reachableSet = new BitSet();
		reachableSet.or(bsForwardReachedAppMtds);
		reachableSet.or(bsBackwardReachedAppMtds);
		return reachableSet;
	}
	
	public BitSet getGlobalEDomSetEntry() { return globalEDomSetEntry; }
	public void setGlobalEDomSetEntry(BitSet globalDomSet) { this.globalEDomSetEntry = globalDomSet; }
	public BitSet getGlobalEDomSetExit() { return globalEDomSetExit; }
	public void setGlobalEDomSetExit(BitSet globalDomSet) { this.globalEDomSetExit = globalDomSet; }
	public BitSet getGlobalXDomSetEntry() { return globalXDomSetEntry; }
	public void setGlobalXDomSetEntry(BitSet globalDomSet) { this.globalXDomSetEntry = globalDomSet; }
	public BitSet getGlobalXDomSetExit() { return globalXDomSetExit; }
	public void setGlobalXDomSetExit(BitSet globalDomSet) { this.globalXDomSetExit = globalDomSet; }
	
	public BitSet getGlobalEPDomSetEntry() { return globalEPDomSetEntry; }
	public void setGlobalEPDomSetEntry(BitSet globalPDomSet) { this.globalEPDomSetEntry = globalPDomSet; }
	public BitSet getGlobalEPDomSetExit() { return globalEPDomSetExit; }
	public void setGlobalEPDomSetExit(BitSet globalPDomSet) { this.globalEPDomSetExit = globalPDomSet; }
	public BitSet getGlobalXPDomSetEntry() { return globalXPDomSetEntry; }
	public void setGlobalXPDomSetEntry(BitSet globalPDomSet) { this.globalXPDomSetEntry = globalPDomSet; }
	public BitSet getGlobalXPDomSetExit() { return globalXPDomSetExit; }
	public void setGlobalXPDomSetExit(BitSet globalPDomSet) { this.globalXPDomSetExit = globalPDomSet; }
	
//	public boolean callSiteReaches(CallSite from, CallSite to) {
//		return reachability.reaches(from.getLoc().getStmt(), to.getLoc().getStmt());
//	}
	
	public MethodTag(SootMethod m) {
		this.m = m;
		this.stmtList = new ArrayList<Stmt>(); // maps id->stmt
		for (Unit u : m.retrieveActiveBody().getUnits())
			stmtList.add((Stmt)u);
		assert !this.stmtList.isEmpty();
		
		this.exitPoints = null; // will be created later
		
		// create reverse map stmt->id
		this.revStmtMap = new HashMap<Stmt, Integer>();
		for (int sId = 0; sId < stmtList.size(); ++sId)
			revStmtMap.put(stmtList.get(sId), sId);
		
		createStmtTags();
		findPredsAndExits(false); //verbose);
		findFormalParams();
//		reachability = new ReachabilityAnalysis(m); // computes reachability right away
		
		computeBasicBlocks();
	}
	
	/** These are only APP methods */
	public Collection<SootMethod> getCallerMethods() {
		HashSet<SootMethod> callerMethods = new HashSet<SootMethod>();
		for (CallSite cs : callers)
			callerMethods.add(cs.getLoc().getMethod());
		
		return callerMethods;
	}
	
	public Collection<SootMethod> getAppCalleeMethods() {
		HashSet<SootMethod> appCalleeMethods = new HashSet<SootMethod>();
		for (CallSite cs : callSites)
			appCalleeMethods.addAll(cs.getAppCallees());
		
		return appCalleeMethods;
	}
	
	public void createStmtTags() {
		int sIdx = 0;
		for (Iterator itStmt = m.retrieveActiveBody().getUnits().iterator(); itStmt.hasNext(); ) {
			Stmt s = (Stmt) itStmt.next();
			s.addTag(new StmtTag(m, s, sIdx++));
		}
	}
	
	/** Computes stmt predecessors, exit nodes and call sites */
	private void findPredsAndExits(boolean verbose) {
		if (verbose)
			System.out.println("    Computing predecessors for each unit");
		
		PatchingChain pchain = m.retrieveActiveBody().getUnits();
		assert !pchain.isEmpty();
		
		// exit stmt(s) and OUT set(s) initialization
		exitPoints = new ArrayList<Location>(); // contains all exit points, holding out sets of method
		
		// compute set of predecessors and successors for each Stmt in Body
		for (Iterator itStmt = pchain.iterator(); itStmt.hasNext(); ) {
			Stmt s = (Stmt) itStmt.next();
			StmtTag sTag = (StmtTag) s.getTag(StmtTag.TAG_NAME);
			
			// add return stmts to exit points set as we discover them
			boolean isExit = Util.isReturnStmt(s);
			if (isExit)  // store exit and create new, empty OUT RD set for it
				exitPoints.add(sTag.getLocation());
			else {
				// TODO: handle throw-to-catch edges to identify missing duas and killings 
				
				// add unit as predecessor of each of its successors (if not an exit unit)
				List lUBoxes = s.getUnitBoxes(); // branch targets, if branch stmt
				if (lUBoxes.isEmpty()) { // no target(s); successor is next unit in code
					if (s.fallsThrough()) {
						Stmt sSucc = (Stmt) pchain.getSuccOf(s);
						StmtTag sTagSucc = (StmtTag) sSucc.getTag(StmtTag.TAG_NAME);
						sTagSucc.addPredecessorStmt(s);
						sTag.addSuccessorStmt(sSucc);
					}
					else
						assert s instanceof ThrowStmt;
				}
				else { // there are branch target(s)
					if (s.fallsThrough()) { // there is also an immediate successor
						Stmt sSucc = (Stmt) pchain.getSuccOf(s);
						StmtTag sTagSucc = (StmtTag) sSucc.getTag(StmtTag.TAG_NAME);
						sTagSucc.addPredecessorStmt(s);
						sTag.addSuccessorStmt(sSucc);
					}
					for (Iterator itUBox = lUBoxes.iterator(); itUBox.hasNext(); ) {
						UnitBox ubTarget = (UnitBox) itUBox.next();
						Stmt sSucc = (Stmt) ubTarget.getUnit();
						StmtTag sTagSucc = (StmtTag) sSucc.getTag(StmtTag.TAG_NAME);
						sTagSucc.addPredecessorStmt(s);
						sTag.addSuccessorStmt(sSucc);
					}
				}
			}
			// print predecessors of current unit
			if (verbose) {
				System.out.print("    Preds of " + s + ": ");
				for (Iterator<Stmt> itPredUnit = ((StmtTag)s.getTag(StmtTag.TAG_NAME)).getPredecessorStmts().iterator();
					 itPredUnit.hasNext(); )
					System.out.print("[" + itPredUnit.next() + "] ");
				System.out.println();
			}
		}
		
		// mark stmts in catch blocks; initial value is true for all stmts, except entry
		Stmt sEntry = (Stmt) pchain.getFirst();
		StmtTag sEntryTag = (StmtTag) sEntry.getTag(StmtTag.TAG_NAME);
		sEntryTag.setInCatchBlock(false);
		
		boolean fixedPoint = false;
		while (!fixedPoint) {
			fixedPoint = true;
			
			for (Iterator itStmt = pchain.iterator(); itStmt.hasNext(); ) {
				Stmt s = (Stmt) itStmt.next();
				StmtTag sTag = (StmtTag) s.getTag(StmtTag.TAG_NAME);
				
				if (sTag.isInCatchBlock() && sTag.hasPredecessorStmts()) {
					boolean allPredsInCatchBlock = true;
					for (Stmt sPred : sTag.getPredecessorStmts()) {
						StmtTag sPredTag = (StmtTag) sPred.getTag(StmtTag.TAG_NAME);
						if (!sPredTag.isInCatchBlock()) {
							allPredsInCatchBlock = false;
							break;
						}
					}
					if (!allPredsInCatchBlock) {
						sTag.setInCatchBlock(false);
						fixedPoint = false;
					}
				}
			}
		}
	}
	
	/** Constructs the list of locals corresponding to the method's formal parameters; doesn't include 'this' */
	private void findFormalParams() {
		ArrayList<Local> formals = new ArrayList<Local>();
		PatchingChain pc = m.retrieveActiveBody().getUnits();
		if (!m.isStatic()) {  // sanity check for 'this' local (first formal parameter in instance methods)
			IdentityStmt idS = (IdentityStmt) pc.getFirst();
			Local t = (Local) idS.getLeftOp();
//			final boolean usingOrigNames = soot.options.Options.getDeclaredOptionsForPhase("jb").contains("enabled use-original-names");
			assert t.getName().equals("this") || t.getName().equals("r0") || t.getName().equals("l0");
		}
		Stmt s;
		for (Iterator itS = pc.iterator(); itS.hasNext() && (s = (Stmt)itS.next()) instanceof IdentityStmt; ) {
			IdentityStmt is = (IdentityStmt) s;
			formals.add((Local)is.getLeftOp());
		}
		
		formalParams = new Local[formals.size()];
		formalParams = formals.toArray(formalParams);
	}
	
//	/** Returns the 0-based index of local (value) in formal param list, or -1 if not a formal param. */
//	public int getFormalParamIdx(Local l) {
//		for (int i = 0; i < formalParams.length; ++i)
//			if (formalParams[i] == l)
//				return i;
//		return -1; // not found
//	}
	
	/** Asks statements to compute call information and return call site, if any */
	public void initCallSites() {
		PatchingChain pchain = m.retrieveActiveBody().getUnits();
		
		for (Iterator itStmt = pchain.iterator(); itStmt.hasNext(); ) {
			Stmt s = (Stmt) itStmt.next();
			StmtTag sTag = (StmtTag) s.getTag(StmtTag.TAG_NAME);
			
			// make statement find its call targets, create call branches, and provide call site
			CallSite cs = sTag.initCallSite();
			
			// store call site provided by this statement, but only if not in catch block
			if (!sTag.isInCatchBlock() && cs != null) {
				callSites.add(cs);
				
				// add this method to callees' caller sites set
				for (SootMethod mCallee : cs.getAppCallees()) {
					MethodTag mCalleeTag = (MethodTag) mCallee.getTag(MethodTag.TAG_NAME);
					mCalleeTag.addCallerSite(cs);
				}
			}
		}
	}
	
	private void computeBasicBlocks() {
		// create and store BB graph 
		blockGraph = new BriefBlockGraph(m.retrieveActiveBody());
		
		// set basic block for each stmt
		assert !blockGraph.getHeads().isEmpty();
		Block bb = (Block) blockGraph.getHeads().get(0); // assume first head is regular entry to method
		setStmtBB(bb);
	}
	
	/** Recursive: after setting BB for stmts in it, it calls itself for block's successors*/
	private void setStmtBB(Block bb) {
		Stmt s = (Stmt) bb.getHead();
		StmtTag sTag = (StmtTag) s.getTag(StmtTag.TAG_NAME);
		if (sTag.getBasicBlock() != null) {
			assert sTag.getBasicBlock() == bb;
			return; // BB already visited
		}
		
		do {
			sTag.setBasicBlock(bb);
			if (!sTag.hasSuccessorStmts() || s == bb.getTail())
				break;
			
			// advance s and sTag to first and only successor
			assert sTag.getSuccessorStmts().size() == 1;
			s = sTag.getSuccessorStmts().get(0);
			sTag = (StmtTag) s.getTag(StmtTag.TAG_NAME);
		} while (true); 
		
		for (Iterator itBBSucc = bb.getSuccs().iterator(); itBBSucc.hasNext(); ) {
			Block bbSucc = (Block) itBBSucc.next();
			setStmtBB(bbSucc);
		}
	}
	
	// Tag interface implementation
	public String getName() { return TAG_NAME; }
	public byte[] getValue() throws AttributeValueException { return null; }
	
	/**
	 * Computes stmt-to-stmt intraproc reachability, storing results in stmt tags.
	 * Also propagates and stores reachable method calls at each stmt.
	 */
	public void computeLocalStmtReachability() {
		if (isLocalReachabilityComputed())
			return;
		
		// simple data-flow forward propagation of reaching stmts
		// every stmt reaches itself
//		PatchingChain pchain = m.retrieveActiveBody().getUnits();
		
		// init reachable stmts and app methods
		for (int sId = 0; sId < stmtList.size(); ++sId) {
			Stmt s = (Stmt) stmtList.get(sId);
			StmtTag sTag = (StmtTag) s.getTag(StmtTag.TAG_NAME);
			
			sTag.initLocalReachedStmtsMtds();
			sTag.addLocalReachedStmt(sId); // stmt reaches itself, for a start
			if (sTag.hasAppCallees())
				sTag.addLocalReachedAppMtd(sTag.getAppCallees());
		}
		
		boolean fixedPoint = false;
		while (!fixedPoint) {
			fixedPoint = true;
			// propagate reachable stmts from successors, for each stmt
			for (int sId = stmtList.size()-1; sId >= 0; --sId) {
				Stmt s = stmtList.get(sId);
				StmtTag sTag = (StmtTag) s.getTag(StmtTag.TAG_NAME);
				for (Stmt sSucc : sTag.getSuccessorStmts()) {
					StmtTag sSuccTag = (StmtTag) sSucc.getTag(StmtTag.TAG_NAME);
					if (sTag.addLocalReachedStmts(sSuccTag.getLocalReachedStmts()))
						fixedPoint = false;
					if (sTag.addLocalReachedAppMtd(sSuccTag.getLocalReachedAppMtds()))
						fixedPoint = false;
				}
			}
		}
	}
	
	/** Ensure we don't store backward reachable caller sites whose containing methods are already backward reachable */
	public void removeRedundantBackReachCallSites() {
		for (CallSite cs : (HashSet<CallSite>)backwardReachedCallSites.clone()) {
			final int containingMtdId = ProgramFlowGraph.inst().getMethodIdx(cs.getLoc().getMethod());
			if (bsBackwardReachedAppMtds.get(containingMtdId))
				backwardReachedCallSites.remove(cs);
		}
	}
	
}
