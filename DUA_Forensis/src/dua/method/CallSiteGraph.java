package dua.method;

import java.util.ArrayList;
import java.util.Collection;

import soot.SootMethod;
import soot.jimple.Stmt;
import dua.unit.Location;
import dua.unit.StmtTag;

/**
 * Intra-procedural graph representing control-flow for method's entry, exit, and call sites.
 * Starts creating a stmt-level CFG, and removes non-CS nodes to obtain CS graph
 */
public class CallSiteGraph extends CFG {
	/** Basic initialization */
	public CallSiteGraph(SootMethod m) {
		super(m);
	}
	/** Must be called after all CFGs have been created. Performs initial specialized analysis that might require existence of all CFGs. */
	@Override
	public void analyze() {
		super.analyze(); // should always perform superclass's initial analysis first
		removeNonCSNodes(method);
	}
	
	/** Computes intra-proc call site graph for each method, by reducing stmt-level CFG */
	private void removeNonCSNodes(SootMethod m) {
		// create new node list for entry, CS nodes, and exit
		ArrayList<CFGNode> csNodes = new ArrayList<CFGNode>();
		csNodes.add(ENTRY);
		
		// remove non-callsite/exit nodes
		// for now, remove all nodes in catch blocks
		for (CFGNode n : nodes) {
			Stmt s = n.getStmt();
			if (s == null)
				continue; // it's ENTRY or EXIT; they are added separately, before and after this loop
			
			// if call site, add to new node list; if not, remove node (relink preds to succs)
			if (!n.isInCatchBlock() && n.getAppCallSite() != null)
				csNodes.add(n);
			else {
				// "remove" node 'n' by re-linking predecessors with successors
				for (CFGNode predN : n.getPreds())
					predN.getSuccs().remove(n);
				for (CFGNode succN : n.getSuccs())
					succN.getPreds().remove(n);
				for (CFGNode predN : n.getPreds()) {
					if (predN != n) {
						for (CFGNode succN : n.getSuccs()) {
							if (succN != n) {
								predN.addSucc(succN);
								succN.addPred(predN);
							}
						}
					}
				}
			}
		}
		csNodes.add(EXIT); // add exit at the very end
		
		nodes = csNodes; // update nodes list
	}
	
}
