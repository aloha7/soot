package fault;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import soot.PatchingChain;
import soot.SootMethod;
import soot.jimple.IdentityStmt;
import soot.jimple.Stmt;
import soot.toolkits.graph.Block;
import dua.DUA;
import dua.DUAssocSet;
import dua.Options;
import dua.global.ProgramFlowGraph;
import dua.global.ReqBranchAnalysis;
import dua.method.CFG;
import dua.method.CFG.CFGNode;
import dua.method.CFGDefUses.Branch;
import dua.method.CFGDefUses.Def;
import dua.method.CFGDefUses.PUse;
import dua.method.CFGDefUses.Use;
import dua.unit.StmtTag;
import dua.util.Util;

public class DUAStmtMapper {
	
	private static class BlockComparator implements Comparator<Block> {
		public int compare(Block bb1, Block bb2) {
			if (bb1 == bb2)
				return 0;
			
			// compare containing methods first
			Stmt s1 = (Stmt) bb1.getHead();
			Stmt s2 = (Stmt) bb2.getHead();
			SootMethod m1 = ProgramFlowGraph.inst().getContainingMethod(s1);
			SootMethod m2 = ProgramFlowGraph.inst().getContainingMethod(s2);
			if (m1 != m2)
				return (ProgramFlowGraph.inst().getMethodIdx(m1) < ProgramFlowGraph.inst().getMethodIdx(m2))? -1 : 1;
			
			// then compare node index of first stmt
			CFG cfg = ProgramFlowGraph.inst().getCFG(m1);
			return (cfg.getNodeId(cfg.getNode(s1)) < cfg.getNodeId(cfg.getNode(s2)))? -1 : 1;
		}
	}

	/**
	 * Writes a file in which each row (line) is the list of node ids related to the entity (DUA in this case) whose index is the row number.
	 * 
	 * VARIANT 1: RULES to relate stmts to DUA:
	 *   1) Def stmt
	 *   2) Use stmt (src of branch if use is p-use)
	 *   3) Target of p-use (if use is p-use)
	 *   4) DUMMY DUA: groups unassigned stmts by basic block
	 *      (a refinement would be a "super-block" -- intraproc set of basic blocks such that each block pair has a dom+pdom relation)
	 * VARIANT 2: RULES for "duasrc":
	 *   1) Def stmt and use stmt (if p-use, src and tgt of branch)
	 *   2) Add statements preceding Def in Def's basic block (VARIANT 2.1) or control-flow region (VARIANT 2.2), connected by data dependences to Def
	 * VARIANT 3: 'dublock' rules:
	 *   1) Each definition is mapped to the whole basic block containing it
	 *   2) Unmapped blocks are mapped to all uses they contain
	 * 
	 * Note that, because DUAs can be interprocedural, DUA entity ids are not ordered by CFG and position in CFG.
	 */
	public static void writeEntityStmtFiles(DUAssocSet duaSet, List<Branch> brs, ReqBranchAnalysis reqBrAnalysis) {
		List<DUA> duas = duaSet.getAllDUAs();
		
		// assign global id to each stmt and write file STMTIDs
		Map<Stmt, Integer> stmtIds = StmtMapper.getWriteGlobalStmtIds();
		
		// 1. DUAs
		// build map DUA->stmt by visiting each DUA
		BitSet bsAssignedStmts = new BitSet(stmtIds.size()); // keeps track of which stmts have not been associated
		Map<DUA, List<Stmt>> entityStmts = buildDUASrcMinisliceMap(duas, stmtIds, bsAssignedStmts, false);
		
		// Create dummy DUA representing basic block
		List<Block> sortedBlocks = new ArrayList<Block>();
		Map<Block, List<Stmt>> dummyDUAsToStmts = buildDummyDUAMap(duas, brs, reqBrAnalysis, stmtIds, bsAssignedStmts, sortedBlocks, "dummyduas");
		
		// write file with this data
		outputDUABlockStmtFile(duas, stmtIds, entityStmts, sortedBlocks, dummyDUAsToStmts, "dua");
		
		// 2. DEFs
		// build map DUA->stmt only for DEFs
		bsAssignedStmts = new BitSet(stmtIds.size()); // keeps track of which stmts have not been associated
		entityStmts = buildDUASrcMinisliceMap(duas, stmtIds, bsAssignedStmts, true);
		
		// Create dummy DUA representing basic block
		sortedBlocks = new ArrayList<Block>();
		dummyDUAsToStmts = buildDummyDUAMap(duas, brs, reqBrAnalysis, stmtIds, bsAssignedStmts, sortedBlocks, "dummydefs");
		
		// write file with this data
		outputDUABlockStmtFile(duas, stmtIds, entityStmts, sortedBlocks, dummyDUAsToStmts, "def");
		
		// 3. DU-BLOCKS
		// build map DU-block->stmt only for DU-blocks
		entityStmts = buildDUBlocks(duaSet, stmtIds);
		
		// write file with this data
		outputDUABlockStmtFile(duas, stmtIds, entityStmts, new ArrayList<Block>(), new HashMap<Block, List<Stmt>>(), "dublock");
	}
	
	/**
	 * ** NOTE: this method is redundant with the new DUAAssocSet.getMiniBack[Def|Node]Slices()
	 * 
	 * Builds and returns map of interblock DUAs to related statements. Intrablock DUAs have no entry in this map.
	 * Updates bitset of assigned statements.
	 * 
	 * @param duas (IN) All DUAs, interblock and intrablock (if available)
	 * @param stmtIds (IN)
	 * @param bsAssignedStmts (OUT) Keeps track of stmts already assigned to some DUA
	 * @return Map of interblock DUAs to related statements. Intrablock DUAs have no entry in this map.
	 */
	private static Map<DUA, List<Stmt>> buildDUASrcMinisliceMap(List<DUA> duas, Map<Stmt,Integer> stmtIds, BitSet bsAssignedStmts, boolean defsOnly) {
		// create map to fill and return
		Map<DUA, List<Stmt>> duaToStmts = new HashMap<DUA, List<Stmt>>();
		
		// assign def and use stmts to non-intrablock duas
		// at the same time, map intrablock (VARIANT 2.1) or intra-control-region (VARIANT 2.2) duas to use's stmt
		Map<Stmt, List<DUA>> sUseToIntraBBDUAs = new HashMap<Stmt, List<DUA>>();
		fillDUAMaps(duas, stmtIds, bsAssignedStmts, defsOnly, duaToStmts, sUseToIntraBBDUAs);
		
		// assign intrablock data-dependences to each non-intrablock DUA
		for (DUA dua : duaToStmts.keySet()) {
			List<Stmt> relStmts = duaToStmts.get(dua);
			if (relStmts != null) {  // DUA is non-intrablock because it has a stmt list already assigned
				// iterate on intrablock DUAs dependency-linked to non-intrablock DUA's def stmt
				// as a result, this code associates the def stmts of the set of intrablock transitive data dependences inside non-intrablock DUA's def stmt
				Set<Stmt> worklist = new HashSet<Stmt>();
				CFGNode nDef = dua.getDef().getN();
				CFG defCFG = ProgramFlowGraph.inst().getContainingCFG(nDef);
				worklist.add( nDef.getStmt() ); // start from DUA's def stmt
				while (!worklist.isEmpty()) {
					// extract next stmt and use-associated intrablock DUAs 
					Stmt s = worklist.iterator().next();
					worklist.remove(s);
					List<DUA> usedIntrablockDUAs = sUseToIntraBBDUAs.get(s);
					if (usedIntrablockDUAs != null) {  // link to DUA the def stmts of existing intrablock DUAs used at current statement
						for (DUA usedDUA : usedIntrablockDUAs) {
							// get intrablock DUA's def stmt, and associate it to original DUA (if not associated yet)
							Stmt sIntrablockDef = usedDUA.getDef().getN().getStmt();
							if (!relStmts.contains(sIntrablockDef)) {  // avoid adding element more than once to the list
								relStmts.add(sIntrablockDef);
								bsAssignedStmts.set(defCFG.getNodeId(sIntrablockDef));
								
								worklist.add(sIntrablockDef); // add to worklist to visit next
							}
						}
					}
				}
			}
		}
		
		return duaToStmts;
	}
	
	/**
	 * @param duas IN
	 * @param stmtIds IN
	 * @param bsAssignedStmts OUT
	 * @param defsOnly IN
	 * @param duaToStmts OUT
	 * @param sUseToIntraBBDUAs OUT
	 */
	private static void fillDUAMaps(List<DUA> duas, Map<Stmt, Integer> stmtIds, BitSet bsAssignedStmts, boolean defsOnly,
			Map<DUA, List<Stmt>> duaToStmts, Map<Stmt, List<DUA>> sUseToIntraBBDUAs)
	{
		for (DUA dua : duas) {
			// check if DUA is intra-block
			// instead of treating it as normal DUA, associate intrablock DUA to stmt at use
			if (dua.isIntraBlock()) {
				if (sUseToIntraBBDUAs != null) {
					Stmt sUse = dua.getUse().getN().getStmt();
					// get/create DUA list for stmt
					List<DUA> usedDuas = sUseToIntraBBDUAs.get(sUse);
					if (usedDuas == null) {
						usedDuas = new ArrayList<DUA>();
						sUseToIntraBBDUAs.put(sUse, usedDuas);
					}
					usedDuas.add(dua);
				}
			}
			else {
				// get/create list of stmts for DUA
				List<Stmt> relStmts = duaToStmts.get(dua);
				if (relStmts == null) {
					relStmts = new ArrayList<Stmt>();
					duaToStmts.put(dua, relStmts);
				}
				
				// Rule 1: def stmt
				Stmt s = dua.getDef().getN().getStmt();
				//2010-02-26: ignore the null statement(for example, the specialNode EN or EX)
				if(s != null){
					relStmts.add(s);
					bsAssignedStmts.set(stmtIds.get(s));
					
					if (!defsOnly) {
						// Rule 2: use (src) stmt
						s = dua.getUse().getSrcNode().getStmt();
						relStmts.add(s);
						bsAssignedStmts.set(stmtIds.get(s));
						
						// Rule 3: use tgt stmt, if p-use
						Branch brPUse = dua.getUse().getBranch();
						if (brPUse != null) {
							s = brPUse.getTgt().getStmt();
							relStmts.add(s);
							bsAssignedStmts.set(stmtIds.get(s));
						}
					}
				}
			}
		}
	}
	
	private static Map<DUA, List<Stmt>> buildDUBlocks(DUAssocSet duaSet, Map<Stmt,Integer> stmtIds) {
		List<DUA> duas = duaSet.getAllDUAs();
		
		BitSet bsAssignedStmts = new BitSet(stmtIds.size());
		Map<DUA, List<Stmt>> duaToStmts = new HashMap<DUA, List<Stmt>>();
		
		// RULE 1: Each definition is mapped to its containing statement and mini-back-slice stmts
		Map<Def,List<Def>> miniBackDefSlices = duaSet.getMiniBackDefSlices();
		for (DUA dua : duas) {
			if (dua.isIntraBlock()) {
				assert !dua.getDef().getVar().isLocalOrConst();
				continue;
			}
			
			// get/create list of stmts for DUA
			List<Stmt> relStmts = duaToStmts.get(dua);
			if (relStmts == null) {
				relStmts = new ArrayList<Stmt>();
				duaToStmts.put(dua, relStmts);
			}
			
			// associate def to def stmt
			Def def = dua.getDef();
			Stmt sDef = def.getN().getStmt();
			
			//2010-02-26: ignore the null statement(for example, the specialNode EN or EX)
			if(sDef != null){
				relStmts.add(sDef);
				bsAssignedStmts.set(stmtIds.get(sDef));
				
				// add mini-back-slice stmts, if any
				List<Def> miniBackDefSlice = miniBackDefSlices.get(def);
				if (miniBackDefSlice != null) {
					for (Def backDef : miniBackDefSlice) {
						Stmt sBackDef = backDef.getN().getStmt();
						relStmts.add(sBackDef);
						bsAssignedStmts.set(stmtIds.get(sBackDef));
					}
				}
			}
			
		}
		
		// RULE 2: For completeness, each use is mapped to all unmapped stmts in use's BB (plus branch target's BB, if p-use)
		BitSet bsPostMappingRule = (BitSet)bsAssignedStmts.clone();
		for (DUA dua : duas) {
			if (dua.isIntraBlock())
				continue;
			// *** HACK for NanoXML v5s8 !!!
			if (ProgramFlowGraph.inst().getContainingCFG( dua.getDef().getN() ).getMethod().toString()
					.equals("<net.n3.nanoxml.XMLEntityResolver: void finalize()>") )
				continue;
			
			// 2.1: use's source BB
			CFGNode nUseSrc = dua.getUse().getSrcNode();
			
			List<Stmt> relStmts = duaToStmts.get(dua);
			Collection<Stmt> allUseBBStmts = StmtMapper.getAllBBStmts(nUseSrc);
			
			for (Stmt sAtUseBB : allUseBBStmts) {
				final int idStmtAtUseBB = stmtIds.get(sAtUseBB);
				if (!bsAssignedStmts.get(idStmtAtUseBB)) {
					relStmts.add(sAtUseBB);
					bsPostMappingRule.set(idStmtAtUseBB);
				}
			}
			
			// 2.2: use's target BB, if p-use
			Branch brUse = dua.getUse().getBranch();
			if (brUse != null) {
				CFGNode nUseBrTgt = brUse.getTgt(); 
				allUseBBStmts = StmtMapper.getAllBBStmts(nUseBrTgt);
				for (Stmt sAtUseBB : allUseBBStmts) {
					final int idStmtAtUseBB = stmtIds.get(sAtUseBB);
					if (!bsAssignedStmts.get(idStmtAtUseBB)) {
						relStmts.add(sAtUseBB);
						bsPostMappingRule.set(idStmtAtUseBB);
					}
				}
			}
		}
		bsAssignedStmts = bsPostMappingRule;
		
		// RULE 3: For completeness, each def is mapped to all unmapped stmts in def's BB
		bsPostMappingRule = (BitSet)bsAssignedStmts.clone();
		for (DUA dua : duas) {
			if (dua.isIntraBlock())
				continue;
			
			// def's source BB
			CFGNode nDef = dua.getDef().getN();
			
			List<Stmt> relStmts = duaToStmts.get(dua);
			Collection<Stmt> allDefBBStmts = StmtMapper.getAllBBStmts(nDef);
			
			for (Stmt sAtDefBB : allDefBBStmts) {
				final int idStmtAtDefBB = stmtIds.get(sAtDefBB);
				if (!bsAssignedStmts.get(idStmtAtDefBB)) {
					relStmts.add(sAtDefBB);
					bsPostMappingRule.set(idStmtAtDefBB);
				}
			}
		}
		
		// warn if stmts remain unmapped
		if (bsPostMappingRule.cardinality() != stmtIds.size())
			System.out.println("WARNING: only " + bsPostMappingRule.cardinality() + "/" + stmtIds.size() +
					" stmts were mapped to dublocks");
		
		return duaToStmts;
	}
	
	/**
	 * For each unassigned stmt, creates dummy DUA representing basic block.
	 */
	private static Map<Block, List<Stmt>> buildDummyDUAMap(List<DUA> duas, List<Branch> brs, ReqBranchAnalysis reqBrAnalysis, Map<Stmt,Integer> stmtIds, BitSet bsAssignedStmts, List<Block> sortedBlocks, String dummyInFilename) {
		Map<Block, List<Stmt>> dummyDUAsToStmts = new HashMap<Block, List<Stmt>>();
		
		// create id->stmt inverse map
		Stmt[] idToStmt = new Stmt[stmtIds.size()];
		for (Stmt s : stmtIds.keySet())
			idToStmt[stmtIds.get(s)] = s;
		
		for (int i = 0; i < stmtIds.size(); ++i) {
			// look for unassigned stmt
			if (!bsAssignedStmts.get(i)) {
				// retrieve stmt from id
				Stmt s = idToStmt[i];
				StmtTag sTag = (StmtTag) s.getTag(StmtTag.TAG_NAME);
				if (sTag.isInCatchBlock())
					continue; // skip catch blocks, for now
				
				Block bb = Util.getBB(s);
				assert bb != null;
				
				// get/create stmt list for block
				List<Stmt> blockStmts = dummyDUAsToStmts.get(bb);
				if (blockStmts == null) {
					blockStmts = new ArrayList<Stmt>();
					dummyDUAsToStmts.put(bb, blockStmts);
				}
				// add stmt to block (i.e., dummy DUA)
				blockStmts.add(s);
			}
		}
		sortedBlocks.addAll(dummyDUAsToStmts.keySet());
		Collections.sort(sortedBlocks, new BlockComparator());
		
		System.out.println("Entity-stmt file for DUAs: " + duas.size() + " duas, " + sortedBlocks.size() + " dummy DUAs (blocks)");
		
		// Write block file so that reporter includes them as dummy DUAs for file report
		// This requires branch instrumentation to 
		// Write dummy duas to special file so that reporter can use them to output exec coverage row
		// File format: for each dummy DUA (i.e., BB), one line with control dependencies (branches)
		if (!Options.allBranches()) {
			System.out.println("PROBLEM: not all branches were enumerated, so we can't generate 'dummyduas' file");
		}
		else {
			File fDummyDUAs = new File(Util.getCreateBaseOutPath() + dummyInFilename); // control dependences for dummy DUAs (BBs)
			try {
				// write always a new file, deleting previous contents (if any)
				BufferedWriter writer = new BufferedWriter(new FileWriter(fDummyDUAs));
				
				// build br->idx map
				Map<Branch, Integer> brIds = new HashMap<Branch, Integer>();
				int brIdx = 0;
				for (Branch br : brs)
					brIds.put(br, brIdx++);
				
				// write required branches for dummy duas
				for (Block bb : sortedBlocks) {
					Stmt sFirst = (Stmt) bb.getHead();
					for (Branch brReq : reqBrAnalysis.getReqBranches( ProgramFlowGraph.inst().getContainingCFG(sFirst).getNode(sFirst) )) {
						Integer brId = brIds.get(brReq);
						assert brId != null;
						writer.write(brId + " ");
					}
					writer.write("\n");
				}
				
				writer.flush();
				writer.close();
			}
			catch (FileNotFoundException e) {
				System.err.println("Couldn't write DUMMYDUAS file: " + e);
			}
			catch (SecurityException e) {
				System.err.println("Couldn't write DUMMYDUAS file: " + e);
			}
			catch (IOException e) {
				System.err.println("Couldn't write DUMMYDUAS file: " + e);
			}
		}
		
		return dummyDUAsToStmts;
	}
	
	private static void outputDUABlockStmtFile(List<DUA> duas, Map<Stmt, Integer> stmtIds, 
			Map<DUA, List<Stmt>> entityStmts, List<Block> sortedBlocks, Map<Block, List<Stmt>> dummyDUAsToStmts, String suffix) {
		// Write to file related stmts for each DUA (real and dummy)
		File fEntityStmt = new File(Util.getCreateBaseOutPath() + "entitystmt.out." + suffix);
		try {
			// write always a new file, deleting previous contents (if any)
			BufferedWriter writer = new BufferedWriter(new FileWriter(fEntityStmt));
			
			// real DUAs are assumed to be ordered by id
			for (DUA dua : duas) {
				List<Stmt> relStmts = entityStmts.get(dua);
				if (relStmts != null) {
					for (Stmt s : relStmts)
						writer.write(stmtIds.get(s) + " ");
				}
				else
					assert dua.isIntraBlock();
				
				writer.write("\n");
			}
			// dummy DUAs (blocks) are also sorted, so their id is implicit
			for (Block bb : sortedBlocks) {
				List<Stmt> relStmts = dummyDUAsToStmts.get(bb);
				for (Stmt s : relStmts)
					writer.write(stmtIds.get(s) + " ");
				writer.write("\n");
			}
			
			writer.flush();
			writer.close();
		}
		catch (FileNotFoundException e) { System.err.println("Couldn't write DUA->STMT file: " + e); }
		catch (SecurityException e) { System.err.println("Couldn't write DUA->STMT file: " + e); }
		catch (IOException e) { System.err.println("Couldn't write DUA->STMT file: " + e); }
	}
	
}
