package profile;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

public class BranchReporter {
	public static void __link() {}
	
	private int[] brCovArray;
	
	public int[] getBrCovArray() { return brCovArray; }
	
	public BranchReporter() {}
	
	/** Reports from array of coverage registers */
	public void report(int[] brArray) {
		// store first
		this.brCovArray = brArray;
		
		int numCovered = 0;
		System.out.print("Branches covered:");
		for (int i = 0; i < brArray.length; ++i) {
			if (brArray[i] > 0) {
				System.out.print(" " + i);
				++numCovered;
			}
		}
		System.out.println();
		System.out.println("Branches covered: " + numCovered + "/" + brArray.length);
		
		// output stmt coverage row to file
		reportStmtsFromBranchesToCovMatrixFile(brArray);
		
		// output coverage row to file
		CommonReporter.reportToCovMatrixFile(brArray, "branch");
	}
	
	/** Reports branch coverage using reflection to locate individual branch cov registers in main class */
	public void report(Class mainCls) {
		// build map brId->cov_value from individual branch cov registers
		Vector brCovRegs = new Vector();
		Field[] mainFields = mainCls.getFields();
		for (int fldIdx = 0; fldIdx < mainFields.length; ++fldIdx) {
			Field f = mainFields[fldIdx];
			String name = f.getName();
			if (name.startsWith("<br_")) {
				final int idEndPos = name.lastIndexOf('>');
				final String brIdStr = name.substring("<br_".length(), idEndPos);
				final int brId = Integer.valueOf(brIdStr).intValue();
				
				try {
					final Integer brCovValue = new Integer(f.getInt(null));
					if (brCovRegs.size() <= brId)
						brCovRegs.setSize(brId + 1);
					brCovRegs.set(brId, brCovValue);
				}
				catch (IllegalArgumentException e) { System.out.println("SERIOUS PROBLEM: " + e); }
		        catch (IllegalAccessException e) { System.out.println("SERIOUS PROBLEM: " + e); }
			}
		}
		
		// create coverage array from list object
		int[] covArray = new int[brCovRegs.size()];
		for (int brId = 0; brId < covArray.length; ++brId)
			covArray[brId] = ((Integer) brCovRegs.get(brId)).intValue();
		
		report(covArray);
	}
	
	/** Reports branch coverage using reflection to locate edge cov registers in main class,
	 * and deducing counters for non-instrumented edges and branches, using Ball & Larus 94 method */
	public void reportFromEdges(Class mainCls) {
		final Integer ZERO_INT = new Integer(0);
		
		Vector brCovRegs = new Vector();
		int maxBrId = 0; // to resize arraylist appropriately
		
		// prepare to read from edge info file
		File f = new File("edges");
		FileInputStream fin;
		try { fin = new FileInputStream(f); }
		catch (Exception e) { System.out.println("SERIOUS PROBLEM: " + e); return; }
		
		BufferedReader rin = new BufferedReader(new InputStreamReader(fin));
		try {  // process method by method
			String strLine = rin.readLine();
			while (strLine != null) {  // read until end of stream
				// get method id
				if (!strLine.substring(0, "method ".length()).equals("method "))
					throw new RuntimeException("expected 'method ' in edges file");
				
				final int mId = Integer.valueOf(strLine.substring("method ".length())).intValue();
				
				// get edges and their counters for method, along with associated branches
				int edgeId = 0;
				ArrayList edgeCnts = new ArrayList(20);
				Vector nodeFlows = new Vector(20); // flow for nodes; special EX and EN node are the first ones; node id i from file has id i+2 here
				nodeFlows.add(ZERO_INT); // initial count for special EXIT node
				HashMap nodeOutEdges = new HashMap(); // maps node (id+2, where 0=EX, 1=EN) to list of outgoing edge ids
				HashMap nodeInEdges = new HashMap(); // maps node (id+2, where 0=EX, 1=EN) to list of incoming edge ids
				HashMap edgeSrcs = new HashMap(); // map edgeId->srcNodeId (id+2, where 0=EX, 1=EN)
				HashMap edgeTgts = new HashMap(); // map edgeId->srcNodeId (id+2, where 0=EX, 1=EN)
				HashMap edgeBrs = new HashMap(); // map edgeId->globalBrId, for those edges corresponding to branches
				while ((strLine = rin.readLine()) != null && strLine.charAt(0) != 'm') {  // read until end of method section (i.e., new method or end of stream)
					Integer edgeIdInt = new Integer(edgeId);
					
					// get br id for edge, if specified
					int currPos = 0;
					char c = strLine.charAt(0);
					if (c == 'B') {
						if (!(strLine.charAt(1) == ' '))
							throw new RuntimeException("expected ' ' after 'B' in edges file");
						currPos = 2;
						// find end of br id char pos
						while ((c = strLine.charAt(currPos)) >= '0' && c <= '9')
							++currPos;
						if (!(c == ' '))
							throw new RuntimeException("expected ' ' after br id in edges file, line pos " + currPos + ", line " + strLine);
						
						// get global br id and link it to edge
						final Integer brIdInt = Integer.valueOf(strLine.substring(2, currPos));
						edgeBrs.put(edgeIdInt, brIdInt);
						if (brIdInt.intValue() > maxBrId)
							maxBrId = brIdInt.intValue();
						
						// advance pos 
						c = strLine.charAt(++currPos);
					}
					
					// find initial counter for edge
					Integer edgeCnt = null;
					if (c == 'I') {
						// retrieve counter for edge, using reflection
						Field fEdgeCnt = mainCls.getField("<ed_" + mId + "_" + edgeId + ">");
						edgeCnt = (Integer) fEdgeCnt.get(null);
						System.out.println("  instrum edge " + edgeId + " cnt " + edgeCnt); // DEBUG
						edgeCnts.add(edgeCnt);
					}
					else {
						if (!(c == 'N'))
							throw new RuntimeException("expected 'N' in edges file");
						edgeCnts.add(new Integer(-1)); // 'unknown' counter, for now
					}
					
					// get edge's src and tgt node ids
					// node ids are file's ids + 2, since 0=EX and 1=EN
					if (!(strLine.charAt(currPos + 1) == ' '))
						throw new RuntimeException("expected ' ' after I or N in edges file");
					currPos += 2;
					int startPos = currPos;
					while (strLine.charAt(currPos) != '-')  // move pos to node divider
						++currPos;
					final int srcId = 2 + ((strLine.charAt(startPos) == 'E')?
							((strLine.charAt(startPos + 1) == 'X')? -2 : -1) :
								Integer.valueOf(strLine.substring(startPos, currPos)).intValue());
					final int tgtId = 2 + ((strLine.charAt(currPos + 1) == 'E')?
							((strLine.charAt(currPos + 2) == 'X')? -2 : -1) :
								Integer.valueOf(strLine.substring(currPos + 1)).intValue());
					
					// associate edge as outgoing from src node
					Integer srcIdInt = new Integer(srcId);
					List outEdges = (List) nodeOutEdges.get(srcIdInt);
					if (outEdges == null) {
						outEdges = new ArrayList();
						nodeOutEdges.put(srcIdInt, outEdges);
					}
					outEdges.add(edgeIdInt);
					
					// associate edge as incoming to tgt node
					Integer tgtIdInt = new Integer(tgtId);
					List inEdges = (List) nodeInEdges.get(tgtIdInt);
					if (inEdges == null) {
						inEdges = new ArrayList();
						nodeInEdges.put(tgtIdInt, inEdges);
					}
					inEdges.add(edgeIdInt);
					
					// associate src and tgt nodes to edge
					edgeSrcs.put(edgeIdInt, srcIdInt);
					edgeTgts.put(edgeIdInt, tgtIdInt);
					
					// update node flow vector size
					final int oldSize = nodeFlows.size();
					if (srcId >= oldSize || tgtId >= oldSize) {
						// augment size of node flow vector
						final int newSize = Math.max(srcId, tgtId) + 1; // add 1 to translate max idx to size
						nodeFlows.setSize(newSize);
						// init new nodes with flow 0
						for (int i = oldSize; i < newSize; ++i)
							nodeFlows.set(i, ZERO_INT);
					}
					// update node flow vector counts for src and tgt nodes
					if (edgeCnt != null) {
						final int newSrcFlow = ((Integer)nodeFlows.get(srcId)).intValue() - edgeCnt.intValue();
						nodeFlows.set(srcId, new Integer(newSrcFlow));
						
						final int newTgtFlow = ((Integer)nodeFlows.get(tgtId)).intValue() + edgeCnt.intValue();
						nodeFlows.set(tgtId, new Integer(newTgtFlow));
					}
					
					// next edge
					++edgeId;
				}
				
				// derive counters of non-instrumented edges
				// note that each node has at least one in or out edge that needs deriving
				deriveDFS(nodeOutEdges, nodeInEdges, edgeSrcs, edgeTgts, edgeCnts, nodeFlows, 0, -1);
				
				System.out.println("Method " + mId + " edge cnt: " + edgeCnts);
				
				brCovRegs.setSize(maxBrId + 1); // resize to hold max br id found so far
				setBranchCovFromEdgeCnts(edgeBrs, edgeCnts, brCovRegs);
			}
		}
		catch (Exception e) { System.out.println("SERIOUS PROBLEM: " + e); return; }
		
		// create branch coverage array from branch coverage vector
		System.out.println("BR COV FROM EDGES: " + brCovRegs); // DEBUG
		int[] covArray = new int[brCovRegs.size()];
		for (int brId = 0; brId < covArray.length; ++brId)
			covArray[brId] = ((Integer) brCovRegs.get(brId)).intValue();
		
		report(covArray);
	}
	
	/**
	 * @param nodeId 0 for EXIT node, id+1 for the rest of the nodes
	 */
	private static void deriveDFS(Map nodeOutEdges, Map nodeInEdges, Map edgeSrcs, Map edgeTgts, ArrayList edgeCnts, Vector nodeFlows, int nodeId, int fromEdgeId) {
		System.out.println("  visiting node " + (nodeId-2) + " from edge " + fromEdgeId); // DEBUG
		
		// get current flow for this node
		int currNodeFlow = ((Integer) nodeFlows.get(nodeId)).intValue();
		
		// process incoming edges in DFS
		List inEdges = (List) nodeInEdges.get(new Integer(nodeId));
		for (Iterator itInEdge = inEdges.iterator(); itInEdge.hasNext(); ) {
			final Integer inEdgeId = (Integer) itInEdge.next();
			System.out.println("    processing in edge " + inEdgeId + ", curr cnt " + edgeCnts.get(inEdgeId.intValue())); // DEBUG
			if (inEdgeId.intValue() != fromEdgeId) {  // don't consider edge we are coming from in DFS
				// check if edge's counter has been derived
				final int edgeCnt = ((Integer) edgeCnts.get(inEdgeId.intValue())).intValue();
				if (edgeCnt == -1) {  // part of E - Ecnt, which is a tree
					final int srcId = ((Integer) edgeSrcs.get(inEdgeId)).intValue();
					System.out.println("    DFS move to node " + (srcId-2)); // DEBUG
					deriveDFS(nodeOutEdges, nodeInEdges, edgeSrcs, edgeTgts, edgeCnts, nodeFlows, srcId, inEdgeId.intValue());
					System.out.println("    DFS return from node " + (srcId-2)); // DEBUG
					currNodeFlow += ((Integer) edgeCnts.get(inEdgeId.intValue())).intValue();
				}
			}
		}
		
		// process outgoing edges in DFS
		List outEdges = (List) nodeOutEdges.get(new Integer(nodeId));
		for (Iterator itOutEdge = outEdges.iterator(); itOutEdge.hasNext(); ) {
			final Integer outEdgeId = (Integer) itOutEdge.next();
			System.out.println("    processing out edge " + outEdgeId + ", curr cnt " + edgeCnts.get(outEdgeId.intValue())); // DEBUG
			if (outEdgeId.intValue() != fromEdgeId) {  // don't consider edge we are coming from in DFS
				// check if edge's counter has been derived
				final int edgeCnt = ((Integer) edgeCnts.get(outEdgeId.intValue())).intValue();
				if (edgeCnt == -1) {  // part of E - Ecnt, which is a tree
					final int tgtId = ((Integer) edgeTgts.get(outEdgeId)).intValue();
					System.out.println("    DFS move to node " + (tgtId-2)); // DEBUG
					deriveDFS(nodeOutEdges, nodeInEdges, edgeSrcs, edgeTgts, edgeCnts, nodeFlows, tgtId, outEdgeId.intValue());
					System.out.println("    DFS return to node " + (nodeId-2) + " from node " + (tgtId-2)); // DEBUG
					currNodeFlow -= ((Integer) edgeCnts.get(outEdgeId.intValue())).intValue();
				}
			}
		}
		
		// finally, store cnt for 'from' edge, if we are actually coming from an edge (i.e., not -1)
		if (fromEdgeId != -1) {
			System.out.println("    flow for 'from' edge " + fromEdgeId + " is " + Math.abs(currNodeFlow));
			edgeCnts.set(fromEdgeId, new Integer(Math.abs(currNodeFlow)));
		}
	}
	
	private void setBranchCovFromEdgeCnts(HashMap edgeBrs, ArrayList edgeCnts, Vector brCovRegs) {
		for (Iterator itE = edgeBrs.keySet().iterator(); itE.hasNext(); ) {
			Integer eId = (Integer) itE.next();
			Integer brId = (Integer) edgeBrs.get(eId);
			brCovRegs.set(brId.intValue(), edgeCnts.get(eId.intValue()));
		}
	}
	
	private void reportStmtsFromBranchesToCovMatrixFile(int[] brArray) {
		List brToStmts = new ArrayList();
		int maxStmtId = -1;
		
		// read file relating stmts to branches
		try {
			File fBrStmts = new File("entitystmt.out.branch");
			BufferedReader rin = new BufferedReader(new FileReader(fBrStmts));
			while (true) {
				// get next line in file
				String strLine = rin.readLine();
				if (strLine == null)
					break;
				
				// get stmt ids for branch
				int[] stmtIds = CommonReporter.parseIds(strLine);
				brToStmts.add(stmtIds);
				
				// update max stmt id
				for (int i = 0; i < stmtIds.length; ++i) {
					if (maxStmtId < stmtIds[i])
						maxStmtId = stmtIds[i];
				}
			}
		}
		catch (Exception e) { System.out.println("PROBLEM READING BRANCH-STMT FILE: " + e); return; }
		
		// create stmt cov array
		final int numStmts = maxStmtId + 1;
		int[] stmtArray = new int[numStmts];
		for (int i = 0; i < numStmts; ++i)
			stmtArray[i] = 0; // init to 0
		for (int brId = 0; brId < brArray.length; ++brId) {
			if (brArray[brId] > 0) {
				int[] stmtIds = (int[]) brToStmts.get(brId);
				for (int sId = 0; sId < stmtIds.length; ++sId)
					stmtArray[ stmtIds[sId] ] = 1;
			}
		}
		
		// output coverage row to file
		CommonReporter.reportToCovMatrixFile(stmtArray, "stmt");
		
		// create stmt-pairs report, if stmtpair entity file exists
		try {
			File fStmtPairs = new File("entitystmt.out.stmtpair");
			BufferedReader rin = new BufferedReader(new FileReader(fStmtPairs));
			// if we get here, the file exists
			rin.close();
			
			// create stmt pairs cov array; a pair is covered if BOTH stmts are covered
			int[] stmtPairsCov = new int[numStmts * numStmts];
			for (int i = 0; i < numStmts; ++i) {
				final int baseIdx = i * numStmts;
				final boolean firstCov = (stmtArray[i] > 0);
				for (int j = 0; j < numStmts; ++j)
					stmtPairsCov[baseIdx + j] = (firstCov && (stmtArray[j] > 0))? 1 : 0;
			}
			
			// output stmt pair coverage row to file
			CommonReporter.reportToCovMatrixFile(stmtPairsCov, "stmtpair");
		}
		catch (Exception e) { }
	}
	
}
