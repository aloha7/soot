package fault;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import soot.jimple.Stmt;
import dua.Options;
import dua.global.ProgramFlowGraph;
import dua.method.CFG;
import dua.method.CFG.CFGNode;
import dua.unit.StmtTag;
import dua.util.Util;

public class StmtMapper {
	private static Map<Stmt, Integer> sToId = null;
	private static Stmt[] idToS = null;
	
	public static final class GlobalIdNodeComparator implements Comparator<CFGNode> {
		private GlobalIdNodeComparator() {}
		public final static GlobalIdNodeComparator inst = new GlobalIdNodeComparator();
		public int compare(CFGNode n1, CFGNode n2) {
			final int id1 = sToId.get(n1.getStmt());
			final int id2 = sToId.get(n2.getStmt());
			return (id1 < id2)? -1 : (id1 == id2)? 0 : 1;
		}
	}
	public static final class GlobalIdStmtComparator implements Comparator<Stmt> {
		private GlobalIdStmtComparator() {}
		public final static GlobalIdStmtComparator inst = new GlobalIdStmtComparator();
		public int compare(Stmt s1, Stmt s2) {
			final int id1 = sToId.get(s1);
			final int id2 = sToId.get(s2);
			return (id1 < id2)? -1 : (id1 == id2)? 0 : 1;
		}
	}
	
	/** Assigns global id to each stmt and writes file STMTIDs */
	public static Map<Stmt, Integer> getWriteGlobalStmtIds() {
		// compute only once
		if (sToId != null)
			return sToId;
		
		// assign global id to each stmt and write file STMTIDs
		sToId = new HashMap<Stmt, Integer>();
		
		File fStmtIds = new File(Util.getCreateBaseOutPath() + "stmtids.out");
		try {
			// Write always a new file, deleting previous contents (if any)
			BufferedWriter writer = new BufferedWriter(new FileWriter(fStmtIds));
			
			// Enumerate all stmts in CFG, including catch blocks
			int sId = 0;
			for (CFG cfg : ProgramFlowGraph.inst().getCFGs()) {
				for (CFGNode n : cfg.getNodes()) {
					Stmt s = n.getStmt();
					if (n.isInCatchBlock() || s == null)
						continue; // skip ENTRY and EXIT
					assert s.hasTag(StmtTag.TAG_NAME);
					
					// add stmt to global list
					writer.write(cfg.getMethod() + " - " + s + "\n");
					sToId.put(s, sId++);
				}
			}
			
			writer.flush();
			writer.close();
		}
		catch (FileNotFoundException e) { System.err.println("Couldn't write STMTIDS file: " + e); }
		catch (SecurityException e) { System.err.println("Couldn't write STMTIDS file: " + e); }
		catch (IOException e) { System.err.println("Couldn't write STMTIDS file: " + e); }
		
		return sToId;
	}
	
	public static Stmt[] getCreateInverseMap() {
		if (idToS == null) {
			getWriteGlobalStmtIds();
			idToS = new Stmt[sToId.size()];
			for (Stmt s : sToId.keySet())
				idToS[sToId.get(s)] = s;
		}
		
		return idToS;
	}
	
	/**
	 * Writes stmt-stmt identity map file.
	 * Optionally, writes stmt pair map file, for all pairs of statements in the program.
	 */
	public static void writeEntityStmtFiles() {
		Map<Stmt, Integer> stmtIds = getWriteGlobalStmtIds();
		
		// Write stmt-stmt identity map file
		try {
			File fStmtStmts = new File(Util.getCreateBaseOutPath() + "entitystmt.out.stmt");
			
			// Write always a new file, deleting previous contents (if any)
			BufferedWriter writer = new BufferedWriter(new FileWriter(fStmtStmts));
			
			for (int sId = 0; sId < stmtIds.size(); ++sId)
				writer.write(sId + "\n");
			
			writer.flush();
			writer.close();
		}
		catch (FileNotFoundException e) { System.err.println("Couldn't write STMT entitystmt file: " + e); }
		catch (SecurityException e) { System.err.println("Couldn't write STMT entitystmt  file: " + e); }
		catch (IOException e) { System.err.println("Couldn't write STMT entitystmt  file: " + e); }
		
		// create id->stmt inverse map
		Stmt[] idToStmt = new Stmt[stmtIds.size()];
		for (Stmt s : stmtIds.keySet())
			idToStmt[stmtIds.get(s)] = s;
		
		// Optionally, writes stmt pair map file, for all pairs of statements in the program
		if (Options.stmtPairs()) {
			try {
				File fStmtPairs = new File(Util.getCreateBaseOutPath() + "entitystmt.out.stmtpair");
				
				// Write always a new file, deleting previous contents (if any)
				BufferedWriter writer = new BufferedWriter(new FileWriter(fStmtPairs));
				
				for (int sFirstId = 0; sFirstId < stmtIds.size(); ++sFirstId) {
					for (int sSecondId = 0; sSecondId < stmtIds.size(); ++sSecondId)
						writer.write(sFirstId + " " + sSecondId + "\n");
				}
				
				writer.flush();
				writer.close();
			}
			catch (FileNotFoundException e) { System.err.println("Couldn't write STMT entitystmt file: " + e); }
			catch (SecurityException e) { System.err.println("Couldn't write STMT entitystmt  file: " + e); }
			catch (IOException e) { System.err.println("Couldn't write STMT entitystmt  file: " + e); }
		}
	}
	
	/** Returns the set of all statements in the BB of the given stmt */
	public static Collection<Stmt> getAllBBStmts(CFGNode n) {
		List<Stmt> bbStmts = new ArrayList<Stmt>();
		
		// find top of BB
		while (n.getPreds().size() == 1) {
			CFGNode nNext = n.getPreds().get(0);
			if (nNext.getSuccs().size() != 1)
				break;
			n = nNext;
		}
		
		// add all stmts from top to bottom
		do {
			Stmt s = n.getStmt();
			if (s != null)
				bbStmts.add(s);
			
			// move to next stmt
			if (n.getSuccs().size() != 1)
				break;
			n = n.getSuccs().get(0);
		} while (n.getPreds().size() == 1);
		
		return bbStmts;
	}
	
	public static int getGlobalStmtId(Stmt s) { return sToId.get(s); }
	public static int getGlobalNodeId(CFGNode n) { return getGlobalStmtId(n.getStmt()); }
	public static Stmt getStmtFromGlobalId(int sId) { return idToS[sId]; }
	public static CFGNode getNodeFromGlobalId(int nId) { return ProgramFlowGraph.inst().getNode(getStmtFromGlobalId(nId)); }
	
}
