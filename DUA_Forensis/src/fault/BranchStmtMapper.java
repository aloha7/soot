package fault;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import soot.jimple.IdentityStmt;
import soot.jimple.Stmt;
import dua.DUAAnalysis;
import dua.global.ProgramFlowGraph;
import dua.global.ReqBranchAnalysis;
import dua.method.CFG;
import dua.method.DominatorRelation;
import dua.method.CFG.CFGNode;
import dua.method.CFGDefUses.Branch;
import dua.method.CFGDefUses.Def;
import dua.method.DominatorRelation.NodeDomData;
import dua.util.Util;

public class BranchStmtMapper {
	/** 
	 * Writes a file in which each row (line) is the list of node ids related to the entity (branch in this case) whose index is the row number.
	 * VARIANT 1: RULES to relate stmts to branch, for "brtgt":
	 *   1) Stmt that are directly control-dependent on the branch, ordered from closer to farther from branch (i.e., by idx in CFG)
	 *   2) If there are no control-dependent stmts for a branch, pick branches on which this branch's tgt is control-dependent and
	 *      select stmts associated to those branches that postdominate the tgt stmt.
	 * VARIANT 2: RULES for "brsrc":
	 *   1) Source stmt of branch, plus all other statements in source's basic block, from bottom to top.
	 *   OLD - 2) For unassigned stmts (in blocks without outgoing branches), associate statements to branches on which they are directly 
	 *      control-dependent (top to bottom).
	 *   NEW - Unassigned stmts are NOT mapped to any branch
	 * VARIANT 3: "brsrctgt" rules:
	 *   1) If a stmt is in a source block of a branch, then only use outgoing branches, ignoring incoming branches
	 *   2) If stmt's block has no outgoing branches, map stmt to all incoming branches
	 */
	public static void writeEntityStmtFiles(List<Branch> branches, ReqBranchAnalysis reqBrAnalysis) {
		// Assign global id to each stmt and write file STMTIDs
		Map<Stmt, Integer> stmtIds = StmtMapper.getWriteGlobalStmtIds();
		
		// ***** VARIANT 1: 'brtgt' *****
		Map<Branch, List<Stmt>> branchToCDStmts = buildBranchToCDStmtsMap(branches, reqBrAnalysis, stmtIds);
		writeStmtMapFile(branches, stmtIds, branchToCDStmts, "branch");
		
		// ***** VARIANT 2: 'brsrc' *****
		Map<Branch, List<Stmt>> branchSrcRelated = buildBranchSrcMap(branches, stmtIds);
		writeStmtMapFile(branches, stmtIds, branchSrcRelated, "brsrc");
		
		// ***** VARIANT 3: 'brsrctgt' *****
		Map<Branch, List<Stmt>> branchToSrcTgt = buildBranchSrcTgtMap(branches, reqBrAnalysis, stmtIds);
		writeStmtMapFile(branches, stmtIds, branchToSrcTgt, "brsrctgt");
	}
	
	/** Writes to file related stmts for each branch */
	private static void writeStmtMapFile(List<Branch> branches, Map<Stmt,Integer> stmtIds, Map<Branch, List<Stmt>> brStmts, String suffix) {
		File fBranchStmt = new File(Util.getCreateBaseOutPath() + "entitystmt.out." + suffix);
		try {
			// write always a new file, deleting previous contents (if any)
			BufferedWriter writer = new BufferedWriter(new FileWriter(fBranchStmt));
			
			// branches are assumed to be ordered by id
			for (Branch br : branches) {
				List<Stmt> relStmts = brStmts.get(br);
				for (Stmt s : relStmts)
					writer.write(stmtIds.get(s) + " ");
				writer.write("\n");
			}
			
			writer.flush();
			writer.close();
		}
		catch (FileNotFoundException e) { System.err.println("Couldn't write ENTITYSTMT '" + suffix + "' file: " + e); }
		catch (SecurityException e) { System.err.println("Couldn't write ENTITYSTMT '" + suffix + "' file: " + e); }
		catch (IOException e) { System.err.println("Couldn't write ENTITYSTMT '" + suffix + "' file: " + e); }
	}
	
	/**
	 * VARIANT 1: RULES to relate stmts to branch, for "branch":
	 *   1) Stmt that are directly control-dependent on the branch, ordered from closer to farther from branch (i.e., by idx in CFG)
	 *   2) If there are no control-dependent stmts for a branch, pick branches on which this branch's tgt is control-dependent and
	 *      select stmts associated to those branches that postdominate the tgt stmt.
	 */
	private static Map<Branch,List<Stmt>> buildBranchToCDStmtsMap(List<Branch> branches, ReqBranchAnalysis reqBrAnalysis, Map<Stmt,Integer> stmtIds) {
		Map<Branch, List<Stmt>> branchToCDStmts = new HashMap<Branch, List<Stmt>>();
		BitSet bsAssignedStmts = new BitSet(stmtIds.size()); // keeps track of which stmts have not been associated
		
		// Build maps branch->stmt by visiting each cfg node
		// Associate each stmt to control-dependencies
		for (CFG cfg : ProgramFlowGraph.inst().getCFGs()) {
			for (CFGNode n : cfg.getNodes()) {
				if (n.isInCatchBlock())
					continue; // skip catch blocks
				
				Stmt s = n.getStmt();
				if (s == null)
					continue; // skip ENTRY and EXIT
				
				// Rule 1: Associate stmt to branches on which stmt is control-dependent
				Set<Branch> reqBrs = reqBrAnalysis.getReqBranches(cfg, n);
				for (Branch br : reqBrs) {
					// get/create list of stmts for branch
					List<Stmt> relStmts = branchToCDStmts.get(br);
					if (relStmts == null) {
						relStmts = new ArrayList<Stmt>();
						branchToCDStmts.put(br, relStmts);
					}
					// add stmt to list associated to branch
					relStmts.add(s);
					bsAssignedStmts.set(stmtIds.get(s));
				}
			}
		}
		
		// Some branches don't get any stmt associated with rule 1; use rule 2 in such cases
		for (Branch br : branches) {
			List<Stmt> relStmts = branchToCDStmts.get(br);
			if (relStmts == null) {
				// create new list for branch
				relStmts = new ArrayList<Stmt>();
				branchToCDStmts.put(br, relStmts);
				
				// get control-dependencies and local dominator data for branch's tgt
				CFGNode nTgt = br.getTgt();
				NodeDomData domDataTgt = DominatorRelation.inst().getDomData(nTgt);
				CFG cfg = ProgramFlowGraph.inst().getContainingCFG(nTgt);
				
				// Rule 2: For each required branch of tgt, select associated stmts that postdominate the tgt
				Set<Branch> tgtReqBrs = reqBrAnalysis.getReqBranches(nTgt);
				for (Branch brReq : tgtReqBrs) {
					List<Stmt> reqTgtBrRelStmts = branchToCDStmts.get(brReq);
					for (Stmt relS : reqTgtBrRelStmts) {
						final int relNodeId = cfg.getNodeId(cfg.getNode(relS));
						if (domDataTgt.getLocalPDom().get(relNodeId)) {
							relStmts.add(relS);
							bsAssignedStmts.set(stmtIds.get(relS));
						}
					}
				}
			}
			assert !relStmts.isEmpty();
		}
		
		// DEBUG - check that all stmts were associated to some branch
		assert bsAssignedStmts.cardinality() == stmtIds.size();
		
		return branchToCDStmts;
	}
	
	/**
	 * VARIANT 2: RULES for "brsrc":
	 *   1) Source stmt of branch, plus all other statements in source's BB that *influence* predicate.
	 *   
	 *   Unassigned stmts are NOT mapped to any branch
	 */
	private static Map<Branch, List<Stmt>> buildBranchSrcMap(List<Branch> branches, Map<Stmt,Integer> stmtIds) {
		// create map to fill and return
		Map<Branch, List<Stmt>> branchSrcRelated = new HashMap<Branch, List<Stmt>>();
		BitSet bsAssignedStmts = new BitSet(stmtIds.size()); // keeps track of which stmts have not been associated
		
		// create id->stmt inverse map
		Stmt[] idToStmt = new Stmt[stmtIds.size()];
		for (Stmt s : stmtIds.keySet())
			idToStmt[stmtIds.get(s)] = s;
		
		// RULE 1: Source stmt of branch, plus all other statements in source's BB that *influence* predicate.
		Map<CFGNode,List<Def>> miniBackNodeSlices = DUAAnalysis.inst().getDUASet().getMiniBackNodeSlices();
		for (Branch br : branches) {
			// create new stmt list for branch
			List<Stmt> relStmts = new ArrayList<Stmt>();
			branchSrcRelated.put(br, relStmts);
			
			// add source stmt (if not null)
			CFGNode n = br.getSrc();
			if (n != null) {
				Stmt s = n.getStmt();
				relStmts.add(s);
				bsAssignedStmts.set(stmtIds.get(s));
				
//				// add source basic block stmts in reverse order
//				while (n.getPreds().size() == 1) {  // continue only if there is exactly one predecessor
//					// move to unique predecessor
//					n = n.getPreds().get(0);
//					s = n.getStmt();
//					
//					// stop if reached cfg entry or id stmt
//					if (s == null || s instanceof IdentityStmt)
//						break;
//					// also, stop if passed basic block's top
//					if (n.getSuccs().size() > 1)
//						break;
//					
//					// add same-BB statement
//					relStmts.add(s);
//					bsAssignedStmts.set(stmtIds.get(s));
//				}
				// add pred node's back mini-slice
				List<Def> miniBackNodeSlice = miniBackNodeSlices.get(n);
				if (miniBackNodeSlice != null) {
					for (Def d : miniBackNodeSlice) {
						Stmt sDef = d.getN().getStmt();
						relStmts.add(sDef);
						bsAssignedStmts.set(stmtIds.get(sDef));
					}
				}
			}
		}
		
		return branchSrcRelated;
	}
	
	/** Joins src and tgt maps, ensuring that no statement is repeated in a list */
	private static Map<Branch, List<Stmt>> buildBranchSrcTgtMap(List<Branch> branches, ReqBranchAnalysis reqBrAnalysis, Map<Stmt,Integer> stmtIds) {
		// create 'brsrc' map first, to complete it with 'tgt' later for unmapped stmts
		Map<Branch, List<Stmt>> brsrctgtMap = buildBranchSrcMap(branches, stmtIds);
		
		// create id->stmt inverse map
		Stmt[] idToStmt = new Stmt[stmtIds.size()];
		for (Stmt s : stmtIds.keySet())
			idToStmt[stmtIds.get(s)] = s;
		
		// build bitset of mapped/unmapped stmts after 'brsrc'
		BitSet bsAssignedStmts = new BitSet(stmtIds.size());
		for (Branch br : brsrctgtMap.keySet()) {
			List<Stmt> stmts = brsrctgtMap.get(br);
			for (Stmt s : stmts)
				bsAssignedStmts.set(stmtIds.get(s));
		}
		
		// for each branch, map branch to target block's stmts if stmts were unmapped for 'brsrc'
		// note: we don't update bitset here; we want to map all branches targetting an unmapped stmt
		final int numStmts = stmtIds.size();
		for (int i = 0; i < numStmts; ++i) {
			if (bsAssignedStmts.get(i))
				continue; // skip stmts already assigned
			
			// associate unmapped stmt to each branch on which it is dependent
			Stmt s = idToStmt[i];
			for (Branch br : reqBrAnalysis.getReqBranches(ProgramFlowGraph.inst().getNode(s)))
				brsrctgtMap.get(br).add(s);
		}
		
		return brsrctgtMap;
	}
	
}
