/**
 * 
 */
package dua;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import soot.SootFieldRef;
import soot.toolkits.graph.Block;

import dua.global.ProgramFlowGraph;
import dua.method.CFG;
import dua.method.CFG.CFGNode;
import dua.method.CFGDefUses.Branch;
import dua.method.CFGDefUses.Def;
import dua.method.CFGDefUses.Use;
import dua.util.Util;

public final class DUA {
	protected Def def;
	protected Use use; // real use
	protected Use[] localUses; // local use for def; different from real use if DUA is interprocedural (in which case this use is a call site param use)
	/** Whether covering def and use nodes, without kills, guarantees that dua is indeed covered */
	protected boolean inferrableOrCondInf;
	protected List<Def> killsInOrder;
	protected List<Def> killsNotInOrder;
	
	public boolean isField() { return def.getValue() instanceof SootFieldRef; }
	public Def getDef() { return def; }
	public Use getUse() { return use; }
	public Use[] getLocalUses() { return localUses; }
	public List<Def> getKillsInOrder() { return killsInOrder; }
	public void addKillInOrder(Def kill) { killsInOrder.add(kill); }
	public List<Def> getKillsNotInOrder() { return killsNotInOrder; }
	public void addKillNotInOrder(Def kill) { killsNotInOrder.add(kill); }
	public void addKillsNotInOrder(List<Def> kills) { killsNotInOrder.addAll(kills); }
	
	void setInferrableOrCondInf(boolean inf) { this.inferrableOrCondInf = inf; }
	public boolean isInferrableOrCondInf() { return inferrableOrCondInf; }
	public boolean isDefinitelyInferrable() {
		return inferrableOrCondInf && killsNotInOrder.isEmpty();
	}
	
	/** Inferrability might be partial, and updated later */
	public DUA(Def def, Use use, Use[] localUses, boolean inferrableOrCondInf) {
		this.def = def;
		this.use = use;
		this.localUses = localUses;
		this.inferrableOrCondInf = inferrableOrCondInf;
		this.killsInOrder = new ArrayList<Def>();
		this.killsNotInOrder = new ArrayList<Def>();
	}
	
	/** A DUA is intrablock if it's use is a c-use and the def is in the same block as the use, located before the use. */
	public boolean isIntraBlock() {
		CFGNode nDef = def.getN();
		CFGNode nUse = use.getN(); // null if p-use
		
		Block bbDef = Util.getBB(nDef.getStmt());
		assert bbDef != null;
		Block bbUse = (nUse == null)? null : Util.getBB(nUse.getStmt());
		CFG cfgDef = ProgramFlowGraph.inst().getContainingCFG(nDef);
		
		return bbDef == bbUse && def.getVar().mayEqual(use.getVar()) && cfgDef.getNodeId(nDef) < cfgDef.getNodeId(nUse);
	}
	
	/** A DUA is p-use-intrablock if it's use is a p-use and the def is in the same block as the use. */
	public boolean isPUseIntraBlock() {
		if (use.getBranch() == null)
			return false;
		
		CFGNode nDef = def.getN();
		CFGNode nUse = use.getSrcNode();
		
		Block bbDef = Util.getBB(nDef.getStmt());
		assert bbDef != null;
		Block bbUse = Util.getBB(nUse.getStmt());
		
		return bbDef == bbUse && def.getVar().mayEqual(use.getVar());
	}
	
	/** Provides all nodes involved (i.e., def, c-use, and kills) */
	public HashSet<CFGNode> retrieveAllRelatedNodes() {
		// use hash map to prevent repeated branches
		HashSet<CFGNode> nodes = new HashSet<CFGNode>();
		
		nodes.add(def.getN());
		CFGNode nUse = use.getN();
		if (nUse != null)
			nodes.add(nUse);
		nodes.addAll(getInOrderKillReqNodes());
		nodes.addAll(getNotInOrderKillReqNodes());
		
		return nodes;
	}
	
	/** Provides all branches involved (i.e., p-use branch, if any) */
	public Set<Branch> retrieveAllRelatedBranches() {
		// use hash map to prevent repeated branches
		HashSet<Branch> branches = new HashSet<Branch>();
		
		Branch brUse = use.getBranch();
		if (brUse != null)
			branches.add(brUse);
		
		return branches;
	}
	
	public ArrayList<CFGNode> getInOrderKillReqNodes() {
		ArrayList<CFGNode> killNodes = new ArrayList<CFGNode>();
		for (Def k : killsInOrder)
			killNodes.add(k.getN());
		return killNodes;
	}
	public ArrayList<CFGNode> getNotInOrderKillReqNodes() {
		ArrayList<CFGNode> killNodes = new ArrayList<CFGNode>();
		for (Def k : killsNotInOrder)
			killNodes.add(k.getN());
		return killNodes;
	}
	
	public List<CFGNode> createAllKillNodesList() {
		List<CFGNode> killNodes = new ArrayList<CFGNode>();
		for (Def k : killsInOrder)
			killNodes.add(k.getN());
		for (Def k : killsNotInOrder)
			killNodes.add(k.getN());
		return killNodes;
	}
	
	@Override
	public String toString() {
		return "( " + def + " , " + use + " )";
	}
}