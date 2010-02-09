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

import soot.jimple.Stmt;
import dua.global.ProgramFlowGraph;
import dua.method.CFG;
import dua.method.EPPAnalysis;
import dua.method.CFG.CFGNode;
import dua.util.Util;

public class PathMapper {
	
	/**
	 * Writes path->stmt mapping file.
	 * Paths are globally ordered by their CFG and their idx inside the (originating) CFG.
	 */
	public static void writeEntityStmtFiles(Map<CFG, EPPAnalysis> cfgEPPAnalyses) {
		Map<Stmt, Integer> stmtIds = StmtMapper.getWriteGlobalStmtIds();
		
		// Write map file
		try {
			File fPathStmts = new File(Util.getCreateBaseOutPath() + "entitystmt.out.path");
			
			// Write always a new file, deleting previous contents (if any)
			BufferedWriter writer = new BufferedWriter(new FileWriter(fPathStmts));
			
			// Associate statements to paths, for each CFG
			for (CFG cfg : ProgramFlowGraph.inst().getCFGs()) {
				// assign to each node the bitset of paths that traverse it
				EPPAnalysis eppAnalysis = cfgEPPAnalyses.get(cfg);
				Map<CFGNode,BitSet> nodesLocalBLPaths = new HashMap<CFGNode, BitSet>();
				Map<CFGNode,BitSet> nodesStartingBLPaths = new HashMap<CFGNode, BitSet>();
				Map<CFGNode,BitSet> nodesEndingBLPaths = new HashMap<CFGNode, BitSet>();
				eppAnalysis.assignPathsToNodes(nodesLocalBLPaths, nodesStartingBLPaths, nodesEndingBLPaths);
				final int numPaths = eppAnalysis.getNumPaths(0).get(cfg.ENTRY);
				
				List[] pathStmts = new ArrayList[numPaths];
				for (int i = 0; i < pathStmts.length; ++i)
					pathStmts[i] = new ArrayList();
				for (CFGNode n : cfg.getNodes()) {
					Stmt s = n.getStmt();
					if (s == null || n.isInCatchBlock())
						continue; // skip ENTRY, EXIT, and catch blocks
					
					BitSet bsPathsCrossingNode = nodesLocalBLPaths.get(n);
					for (int i = 0; i < numPaths; ++i)
						if (bsPathsCrossingNode.get(i))
							pathStmts[i].add(s);
				}
				
				// write stmts for each path
				for (int i = 0; i < numPaths; ++i) {
					for (Object oS : pathStmts[i])
						writer.write(stmtIds.get(oS) + " ");
					writer.write("\n");
				}
			}
			
			writer.flush();
			writer.close();
		}
		catch (FileNotFoundException e) { System.err.println("Couldn't write STMT entitystmt file: " + e); }
		catch (SecurityException e) { System.err.println("Couldn't write STMT entitystmt  file: " + e); }
		catch (IOException e) { System.err.println("Couldn't write STMT entitystmt  file: " + e); }
	}
	
}
