package dua.method;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import soot.PatchingChain;
import soot.SootMethod;
import soot.UnitBox;
import soot.jimple.AssignStmt;
import soot.jimple.IdentityStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.Stmt;
import soot.jimple.ThrowStmt;
import dua.Options;
import dua.global.ProgramFlowGraph;
import dua.method.CFGDefUses.Branch;
import dua.unit.StmtTag;
import dua.util.Util;

public class CFG {
	/**
	 * CFG node containing an statement (can be null for special cases).
	 * Links to predecessors and successors in graph.
	 * Contains outgoing branches, if any. An outgoing edge is a branch if there is more than one outgoing edge.
	 * Provides access to distinguished successor to which this node falls-through in the original code, if there is any such successor.
	 */
	public static class CFGNode {
		protected Stmt s;
		protected ArrayList<CFGNode> preds = new ArrayList<CFGNode>();
		protected ArrayList<CFGNode> succs = new ArrayList<CFGNode>();
		protected CFGNode fallThroughTgt = null;
		private ArrayList<Branch> outBranches = null; // created on demand
		
		public Stmt getStmt() { return s; }
		public ArrayList<CFGNode> getPreds() { return preds; }
		public ArrayList<CFGNode> getSuccs() { return succs; }
		public CFGNode getFallThroughTgt() { return fallThroughTgt; }
		
		public void addPred(CFGNode n) { if (!preds.contains(n)) preds.add(n); }  // avoid multiple connections to a predecessor
		public void addSucc(CFGNode n) { if (!succs.contains(n)) succs.add(n); }  // avoid multiple connections to a successor
		/** Returns successor of this node that is located right after this node in the original code, or null if there is no such successor. */
		public void setFallThroughTgt(CFGNode n) { fallThroughTgt = n; }
		
		public boolean isInCatchBlock() { return s != null && ((StmtTag)s.getTag(StmtTag.TAG_NAME)).isInCatchBlock(); }
		
		public void addOutBranch(Branch outBr) {
			if (outBranches == null)
				outBranches = new ArrayList<Branch>();
			outBranches.add(outBr);
		}
		public List<Branch> getOutBranches() { return outBranches; }
		
		public boolean hasAppCallees() {
			if (s == null)
				return false;
			return ((StmtTag) s.getTag(StmtTag.TAG_NAME)).hasAppCallees();
		}
		public CallSite getAppCallSite() {
			if (s == null)
				return null;
			return ((StmtTag) s.getTag(StmtTag.TAG_NAME)).getAppCallSite();
		}
		public CallSite getCallSite() {
			if (s == null)
				return null;
			return ((StmtTag) s.getTag(StmtTag.TAG_NAME)).getCallSite();
		}
		
		/** String containing short id of node in CFG. This implementation uses 0-based index in node list */
		public String getIdStringInMethod() {
			return Integer.toString( getIdInMethod() );
		}
		
		/** String containing short id of node in CFG. This implementation uses 0-based index in node list */
		public int getIdInMethod() {
			return ((StmtTag) s.getTag(StmtTag.TAG_NAME)).getIdxInMethod();
		}
		
		public CFGNode(Stmt s) { this.s = s; }
		
		@Override
		public String toString() { return s.toString(); }
//		@Override
//		public int hashCode() { return (s==null)? 0 : StmtMapper.getWriteGlobalStmtIds().get(s); }
		
		/** Whether it's a "special" node (no stmt). Meant to be overridden by special subclass. */
		public boolean isSpecial() { assert s != null; return false; }
		
		public static class NodeComparator implements Comparator<CFGNode> {
			public int compare(CFGNode n1, CFGNode n2) { return _compare(n1, n2); }
			public static int _compare(CFGNode n1, CFGNode n2) {
				// compare containing CFGs first
				CFG cfg1 = ProgramFlowGraph.inst().getContainingCFG(n1);
				CFG cfg2 = ProgramFlowGraph.inst().getContainingCFG(n2);
				if (cfg1 != cfg2)
					return (ProgramFlowGraph.inst().getMethodIdx(cfg1.getMethod()) < 
							ProgramFlowGraph.inst().getMethodIdx(cfg2.getMethod()))? -1 : 1;
				final int id1 = cfg1.getNodeId(n1);
				final int id2 = cfg2.getNodeId(n2);
				return (id1 < id2)? -1 : (id1 == id2)? 0 : 1;
			}
		}
	}
	public static class CFGNodeSpecial extends CFGNode {
		private String name;
		public CFGNodeSpecial(String name) { super(null); this.name = name; }
		
		@Override public String toString() { return name; }
		/** This implementation uses the special name itself as id of node in method */
		@Override public String getIdStringInMethod() { return name; }
		/** Whether it's a "special" node (no stmt). */
		@Override public boolean isSpecial() { assert s == null; return true; }
	}
	
	protected SootMethod method;
	protected ArrayList<CFGNode> nodes = new ArrayList<CFGNode>();
	protected Map<Stmt,CFGNode> sToNode = new HashMap<Stmt, CFGNode>();
	protected Map<CFGNode,Integer> nodeIds = new HashMap<CFGNode, Integer>();
	/** Register of created branches for this CFG */
	protected HashSet<Branch> branches = new HashSet<Branch>();
	protected Branch entryBranch = null;
	
	private static String ENTRY_NAME = "EN";
	private static String EXIT_NAME = "EX";
	public CFGNode ENTRY; // special ENTRY node, unique for this CFG
	public CFGNode EXIT; // special EXIT node, unique for this CFG
	private Set<CFG> callgraphSuccs = new HashSet<CFG>();
	private Set<CFG> callgraphPreds = new HashSet<CFG>();
	
	public SootMethod getMethod() { return method; }
	public List<CFGNode> getNodes() { return nodes; }
	public CFGNode getNode(Stmt s) { return sToNode.get(s); }
	public int getNodeId(CFGNode n) { return nodeIds.get(n); }
	public int getNodeId(Stmt s) { return getNodeId(getNode(s)); }
	public Branch getEntryBranch() { return entryBranch; }
	public Set<CFG> getCallgraphSuccs() { return callgraphSuccs; }
	public Set<CFG> getCallgraphPreds() { return callgraphPreds; }
	
	public boolean isReachableFromEntry() { return ((MethodTag)method.getTag(MethodTag.TAG_NAME)).isReachableFromEntry(); }
	
	private static class CFGComparator implements Comparator<CFG> {
		public int compare(CFG arg0, CFG arg1) {
			final int idx0 = ProgramFlowGraph.inst().getMethodIdx(arg0.method);
			final int idx1 = ProgramFlowGraph.inst().getMethodIdx(arg1.method);
			return (idx0 < idx1)? -1 : ((idx0 == idx1)? 0 : 1);
		}
	}
	public static final CFGComparator comp = new CFGComparator();
	
	/** Standard constructor for a CFG. Must not perform any special analysis until all CFGs are constructed. */
	public CFG(SootMethod m) {
		this.method = m;
		createNodesPredsSuccs(method);
	}
	
	/** Must be called for each CFG after all CFGs have been created. Performs initial specialized analysis that might require existence of all CFGs.
	 	Creates all branches, except for nodes in catch blocks (for now, at least).
	 	Determines successors and predecessors in method-level call graph. */
	public void analyze() {
		// add special call branch for method
		entryBranch = getCreateBranch(null, ENTRY);
		
		// look for branches and callees
		for (CFGNode n : getNodes()) {
			if (n.isInCatchBlock())
				continue; // for now, at least...
			
			// create branches
			if (n.succs.size() > 1) {
				// create and store branches to successors (avoid multiple brs for same successor)
				List<CFGNode> succList;
				if (Options.removeRepeatedBranches()) {
					succList = new ArrayList<CFGNode>(); // remove repeated successor nodes
					for (CFGNode nSucc : n.succs)
						if (!succList.contains(nSucc))
							succList.add(nSucc);
				}
				else
					succList = n.succs;
				
				for (CFGNode nSucc : succList) {
					// create branch to successor, adding it to out list of this node
					Branch brToSucc = getCreateBranch(n, nSucc);
					n.addOutBranch(brToSucc);
				}
			}
			
			// link to callees (both directions)
			if (n.s instanceof InvokeStmt || (n.s instanceof AssignStmt && ((AssignStmt)n.s).getRightOp() instanceof InvokeExpr)) {
//				InvokeExpr invExpr = n.s.getInvokeExpr();
//				Set<SootMethod> mAppTargets = new HashSet<SootMethod>();
//				Set<SootMethod> mLibTargets = new HashSet<SootMethod>();
//				Util.getConcreteCallTargets(invExpr, mAppTargets, mLibTargets);
				CallSite cs = ((StmtTag)n.s.getTag(StmtTag.TAG_NAME)).getCallSite();
				if (cs != null) {
					for (SootMethod mAppCallee : cs.getAppCallees()) {
						CFG cfgCallee = ProgramFlowGraph.inst().getCFG(mAppCallee);
						callgraphSuccs.add(cfgCallee);
						cfgCallee.callgraphPreds.add(this);
					}
				}
			}
		}
	}
	
	/** Returns first real node (i.e., the successor of ENTRY); EXIT if there are no real nodes */
	public CFGNode getFirstRealNode() { return ENTRY.getSuccs().get(0); }
	/** Returns first real node that is not an ID node; EXIT if there no non-id node */
	public CFGNode getFirstRealNonIdNode() {
		for (CFGNode n : nodes) {
			if (n != ENTRY && !(n.s instanceof IdentityStmt))
				return n;
		}
		assert false; // at least there should be EXIT
		return null;
	}
	
	/**
	 * Creates all CFG nodes, and sets successors and predecessors for each node.
	 * 
	 * *** Right now, THROW statements are NOT considered as returns, hence they don't have an edge to EXIT.
	 * *** THROWs just have no successors.
	 * 
	 *  
	 * @param m Method for which to create nodes
	 */
	protected void createNodesPredsSuccs(SootMethod m) {
		this.ENTRY = new CFGNodeSpecial(ENTRY_NAME); // special ENTRY node
		this.EXIT = new CFGNodeSpecial(EXIT_NAME); // special EXIT node
		
		PatchingChain pchain = m.retrieveActiveBody().getUnits();
		
		// first of all, create a node for each stmt
		int nId = 0;
		nodes.add(this.ENTRY);
		nodeIds.put(this.ENTRY, nId++);
		for (Iterator itStmt = pchain.iterator(); itStmt.hasNext(); ) {
			Stmt s = (Stmt) itStmt.next();
			CFGNode n = instantiateNode(s); // new node using factory method
			nodes.add(n);
			sToNode.put(s, n);
			nodeIds.put(n, nId++);
		}
		nodes.add(this.EXIT);
		nodeIds.put(this.EXIT, nId);
		
		// compute set of predecessors and successors for each Stmt in Body
		if (pchain.isEmpty()) {  // if no stmts, just link ENTRY and EXIT
			ENTRY.addSucc(EXIT);
			EXIT.addPred(ENTRY);
		}
		else {
			// first, link virtual ENTRY to first stmt
			CFGNode nFirst = sToNode.get(pchain.getFirst());
			ENTRY.addSucc(nFirst);
			nFirst.addPred(ENTRY);
			
			// link each remaining node to its successors (or EXIT if no successors)
			for (Iterator itStmt = pchain.iterator(); itStmt.hasNext(); ) {
				// create CFG node for stmt
				Stmt s = (Stmt) itStmt.next();
				CFGNode n = sToNode.get(s);
				
				// add return stmts to exit points set as we discover them
				if (Util.isReturnStmt(s)) {
					n.addSucc(EXIT);
					EXIT.addPred(n);
				}
				else {
					// TODO: handle throw-to-catch edges to identify missing duas and kills
					
					// add unit as predecessor of each of its successors (if not an exit unit)
					List lUBoxes = s.getUnitBoxes(); // branch targets, if branch stmt
					if (lUBoxes.isEmpty()) { // no target(s); successor is next unit in code
						if (s.fallsThrough()) {
							Stmt sSucc = (Stmt) pchain.getSuccOf(s);
							CFGNode nSucc = sToNode.get(sSucc);
							nSucc.addPred(n);
							n.addSucc(nSucc);
							n.setFallThroughTgt(nSucc);
						}
						else
							assert s instanceof ThrowStmt;
					}
					else { // there are branch target(s)
						if (s.fallsThrough()) { // there is also an immediate successor
							Stmt sSucc = (Stmt) pchain.getSuccOf(s);
							CFGNode nSucc = sToNode.get(sSucc);
							nSucc.addPred(n);
							n.addSucc(nSucc);
							n.setFallThroughTgt(nSucc);
						}
						for (Iterator itUBox = lUBoxes.iterator(); itUBox.hasNext(); ) {
							UnitBox ubTarget = (UnitBox) itUBox.next();
							assert ubTarget.isBranchTarget();
							Stmt sSucc = (Stmt) ubTarget.getUnit();
							CFGNode nSucc = sToNode.get(sSucc);
							nSucc.addPred(n);
							n.addSucc(nSucc);
						}
					}
				}
			}
		}
	}
	
	/** Factory method */
	protected CFGNode instantiateNode(Stmt s) { return new CFGNode(s); }
	
	/** Creates, registers and returns new branch */
	private Branch getCreateBranch(CFGNode src, CFGNode tgt) {
		Branch brNew = new Branch(src, tgt);
		
		if (branches.contains(brNew)) {
			// find existing branch object in list, and return it
			for (Branch br : branches)
				if (br.equals(brNew))
					return br;
			assert false; // shouldn't get here
		}
		
		branches.add(brNew);
		return brNew;
	}
	
}
