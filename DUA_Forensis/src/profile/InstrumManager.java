package profile;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import soot.PatchingChain;
import soot.SootMethod;
import soot.UnitBox;
import soot.jimple.IdentityStmt;
import soot.jimple.Jimple;
import soot.jimple.Stmt;
import dua.global.ProgramFlowGraph;
import dua.method.CFG;
import dua.method.Edge;
import dua.method.CFGDefUses.Branch;

public class InstrumManager {
	/** Points to first stmt AFTER entry probe. Guaranteed to execute upon method entry, before any other probe, if it exists (i.e., if map entry for method exists) */
	private HashMap<SootMethod,Stmt> entryProbeEnds = new HashMap<SootMethod, Stmt>();
	/** All existing "stmt" probes, guaranteed to remain just before target stmt. All jumps to tgt go to probe's top. Maps tgt stmt to first stmt of stmt's probe */
	private HashMap<Stmt,Stmt> stmtProbes = new HashMap<Stmt, Stmt>();
	/** Maps edge to first stmt of edge's probe */
	private HashMap<Edge,Stmt> edgeProbes = new HashMap<Edge, Stmt>();
	
	private InstrumManager() {}
	private static InstrumManager inst = new InstrumManager();
	public static InstrumManager v() { return inst; }
	
	/** Just calls PatchingChain.insertBefore; internal pointers in code list are not altered. DOES NOT take probe maps into account. */
	public void insertBeforeNoRedirect(PatchingChain pchain, List instrumCode, Stmt sTarget) {
		for (Object oS : instrumCode)
			pchain.insertBeforeNoRedirect(oS, sTarget);
	}
	
	/** Alternative to PatchingChain.insertBefore, where internal pointers in code list are not altered. DOES NOT take probe maps into account. */
	public void insertBeforeRedirect(PatchingChain pchain, List instrumCode, Stmt sTarget) {
		for (Object oS : instrumCode)
			pchain.insertBeforeNoRedirect(oS, sTarget);
		sTarget.redirectJumpsToThisTo((Stmt)instrumCode.get(0));
	}
	
	/** Just calls PatchingChain.insertAfter. DOES NOT take probe maps into account. */
	public void insertAfter(PatchingChain pchain, List instrumCode, Stmt sTarget) {
		pchain.insertAfter(instrumCode, sTarget);
	}
	
	/** Insert code just before target stmt, inside probe for given stmt s, at the top of that probe. */
	public void insertAtProbeRightBefore(PatchingChain pchain, List instrumCode, Stmt s, Stmt sTarget) {
		assert !(s instanceof IdentityStmt);
		insertBeforeRedirect(pchain, instrumCode, sTarget);
		
		// update probe top, if necessary
		Stmt sProbeTop = stmtProbes.get(s);
		assert sProbeTop != null;
		if (sProbeTop == sTarget)
			stmtProbes.put(s, (Stmt) instrumCode.get(0));
	}
	
	/** Insert code just before target stmt, IGNORING any existing probe mappings. */
	public void insertRightBeforeNoRedirect(PatchingChain pchain, List instrumCode, Stmt s) {
		assert !(s instanceof IdentityStmt);
		for (Object stmt : instrumCode)
			pchain.insertBeforeNoRedirect(stmt, s);
	}
	
	/**
	 * Adds code that will always execute before the specified stmt.
	 * If probe for stmt already existed, insert code at the beginning of probe.
	 * Note: this allows def probes to be placed *after* use probes.
	 */
	public void insertAtProbeTop(PatchingChain pchain, List instrumCode, Stmt s) {
		assert !(s instanceof IdentityStmt);
		Stmt sStmtProbeBegin = stmtProbes.get(s);
		if (sStmtProbeBegin == null) {
			insertBeforeRedirect(pchain, instrumCode, s);
			stmtProbes.put(s, (Stmt) instrumCode.get(0));
		}
		else {
			insertBeforeRedirect(pchain, instrumCode, sStmtProbeBegin);
			stmtProbes.put(s, (Stmt) instrumCode.get(0));
		}
	}
	public void insertAtProbeBottom(PatchingChain pchain, List instrumCode, Stmt s) {
		assert !(s instanceof IdentityStmt);
		insertBeforeRedirect(pchain, instrumCode, s);
		if (stmtProbes.get(s) == null)
			stmtProbes.put(s, (Stmt) instrumCode.get(0));  // code also becomes top of probe
	}
	
	/**
	 * If edge probe doesn't exist, create it:
	 *     Add code just before tgt, without redirecting jumps to that tgt.
	 *     If edge is not fall-through, redirect edge goto to edge's probe start.
	 *     If the predecessor of tgt was not the src (regardless of whether it's a probe or not), and it falls through, add jump over inserted probe.
	 * 
	 * Add code at beginning of edge probe to reduce risk of changing semantics of previous probe code.
	 * If edge is null->EN (i.e., virtual entry to ENTRY node), places code AFTER special method entry probe.
	 * 
	 * Note that an edge-to-Exit probe is placed *before* the edge's src, so if this edge's target has an edge to EXIT (i.e., tgt is a return/throw),
	 * then we need to place the probe *before* that "displaced" edge-to-Exit probe
	 */
	public void insertProbeAt(Edge e, List instrumCode) {
		assert !instrumCode.isEmpty();
		
		// find edge's method; either src or tgt might be null, so use non-null edge end
		// if tgt is null (i.e., special end->EXIT edge), use src as actual tgt
		CFG cfg = ProgramFlowGraph.inst().getContainingCFG( (e.getTgt() == null)? e.getSrc() : e.getTgt() );
		PatchingChain pchain = cfg.getMethod().retrieveActiveBody().getUnits();
		
		// If edge probe doesn't exist, create it with provided instrum code:
		Stmt sEdgeProbeBegin = edgeProbes.get(e);
		if (sEdgeProbeBegin == null) {  // probe for edge didn't exist; we need to create one
			// find actual insertion point
			Stmt sEdgeProbeTgt;
			if (e.getTgt() == cfg.EXIT) {  // the source is a return or throw stmt
				// probe for this edge needs to be placed before edge's src stmt
				assert !(e.getSrc().getStmt() instanceof IdentityStmt);
				// place before src stmt's stmt-level probe, if there is such a probe
				sEdgeProbeTgt = stmtProbes.get(e.getSrc().getStmt());
				if (sEdgeProbeTgt == null)  // in case there is no stmt probe for src
					sEdgeProbeTgt = e.getSrc().getStmt();
			}
			else {
				// *** note that there can't be an edge ENTRY->EXIT, since a method has at least one statement: a return
				// ensure we skip special ENTRY probe; remember that special ENTRY probe is for initialization, not for null->ENTRY edge, for example
				Stmt sNonIdTgtStmt;// = e.getTgt().getStmt();
				if (e.getTgt() == cfg.ENTRY)
					sNonIdTgtStmt = getEntryProbeEnd(cfg.getMethod()); // move from EN to first stmt (AFTER special entry probe, if it exists)
				else
					sNonIdTgtStmt = e.getTgt().getStmt(); // != null, since we already determined that it's not EXIT
				assert sNonIdTgtStmt != null;
				
				// ensure tgt stmt is non-id stmt
				while (sNonIdTgtStmt instanceof IdentityStmt)
					sNonIdTgtStmt = (Stmt) pchain.getSuccOf(sNonIdTgtStmt);
				
				// place at top of intended tgt's stmt probe, if exists, or intended tgt itself otherwise
				sEdgeProbeTgt = stmtProbes.get(sNonIdTgtStmt);
				if (sEdgeProbeTgt == null)  // in case there is no stmt probe for tgt
					sEdgeProbeTgt = sNonIdTgtStmt;
				
				// Note that an edge-to-Exit probe is placed *before* the edge's src, so if this edge's target has an edge to EXIT (i.e., tgt is a return/throw),
				// then we need to place the probe *before* that "displaced" edge-to-Exit probe
				Stmt sExitProbeTop = edgeProbes.get(new Edge(e.getTgt(), cfg.EXIT));
				if (sExitProbeTop != null)
					sEdgeProbeTgt = sExitProbeTop;
			}
			assert sEdgeProbeTgt != null;
			assert !(sEdgeProbeTgt instanceof IdentityStmt);
			
			// handle special cases first
			if (e.getSrc() == cfg.ENTRY || e.getTgt() == cfg.ENTRY)  // edge EN->firststmt or EX->EN (i.e., entry branch)
				insertRightBeforeNoRedirect(pchain, instrumCode, sEdgeProbeTgt);
			else if (e.getTgt() == cfg.EXIT)  // edge to EXIT from return or throw stmt
				insertBeforeRedirect(pchain, instrumCode, sEdgeProbeTgt); // redirect any jump to tgt to edge's probe
			else if (e.fallsThrough()) {  // src falls through to tgt in original program
				// move src to first non-id stmt, if necessary
				Stmt sNonIdSrc = e.getSrc().getStmt();
				while (sNonIdSrc instanceof IdentityStmt)
					sNonIdSrc = (Stmt) pchain.getSuccOf(sNonIdSrc);
				// place probe immediately after src
				// NOTE: we don't place probe directly before tgt because other existing edge probes might execute this probe afterwards,
				//       and then we would need to insert a goto
				pchain.insertAfter(instrumCode, sNonIdSrc);
			}
			else {  // jumping edge (doesn't "fall-through" in original program)
				// save predecessor of edge probe-tgt before inserting edge probe
				Stmt sPred = (Stmt) pchain.getPredOf(sEdgeProbeTgt);
				
				// Add probe just before probe-tgt's stmt probe-start (if any), without 
				// redirecting existing jumps to that 'start'.
				// -- the reason is that there might be multiple incoming edges to that tgt
				// -- we will redirect the correct source unit box next
				insertRightBeforeNoRedirect(pchain, instrumCode, sEdgeProbeTgt);
				
				// redirect all boxes to probe-tgt at edge src, to point to edge's probe start
				// (we assume that only one directed edge exists from src to tgt)
				boolean found = false;
				assert !(e.getSrc().getStmt() instanceof IdentityStmt);
				for (Iterator itUB = e.getSrc().getStmt().getUnitBoxes().iterator(); itUB.hasNext(); ) {
					UnitBox ub = (UnitBox) itUB.next();
					if (ub.getUnit() == sEdgeProbeTgt) {
						// redirect src->tgt's-stmt-probe-start jump to start of edge probe
						assert ub.isBranchTarget();
						ub.setUnit((Stmt)instrumCode.get(0));
						found = true;
					}
				}
				assert found;
				
				// note: the predecessor of probe-tgt's stmt probe-start was not the src
				// Add jump from pred to tgt-start, over probe just inserted
				if (sPred.fallsThrough()) {
					Stmt sGoto = Jimple.v().newGotoStmt(sEdgeProbeTgt);
					pchain.insertAfter(sGoto, sPred);
				}
			}
		}
		else {
			// Add code at beginning of edge probe, to remove the risk of changing semantics of 
			// previously inserted code at the edge probe.
			insertBeforeRedirect(pchain, instrumCode, sEdgeProbeBegin); // redirects edges to start of inserted code at probe
		}
		
		// add top stmt of inserted probe to edge probes map
		edgeProbes.put(e, (Stmt) instrumCode.get(0));
	}
	
	/** Inserts code at probe in branch, which is a particular type of edge */
	public void insertProbeAt(Branch br, List instrumCode) {
		insertProbeAt(new Edge(br.getSrc(), br.getTgt()), instrumCode);
	}
	
	/** Inserts code at BOTTOM of special entry probe. Creates that probe if it didn't exist. */
	public void insertProbeAtEntry(SootMethod m, List instrumCode) {
		// retrieve bottom of entry probe for given method, creating entry probe if it doesn't exist
		Stmt sEntryProbeEnd = entryProbeEnds.get(m);
		PatchingChain pchain = m.retrieveActiveBody().getUnits();
		if (sEntryProbeEnd == null) {
			// first non-id stmt is entry probe's end stmt
			sEntryProbeEnd = (Stmt) pchain.getFirst();
			while (sEntryProbeEnd instanceof IdentityStmt)
				sEntryProbeEnd = (Stmt) pchain.getSuccOf(sEntryProbeEnd);
			
			// add end stmt to map
			entryProbeEnds.put(m, sEntryProbeEnd);
		}
		
		// at code at BOTTOM of entry probe; avoid redirecting any jumps to end stmt
		insertRightBeforeNoRedirect(pchain, instrumCode, sEntryProbeEnd);
	}
	
	/** Returns stmt immediately AFTER entry probe, if such probe exists; otherwise, returns first stmt in chain. */
	private Stmt getEntryProbeEnd(SootMethod m) {
		Stmt sEntryProbeEnd = entryProbeEnds.get(m);
		if (sEntryProbeEnd != null)
			return sEntryProbeEnd;
		
		return (Stmt) m.retrieveActiveBody().getUnits().getFirst();
	}
	
}
