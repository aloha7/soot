package profile;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import soot.ArrayType;
import soot.Body;
import soot.IntType;
import soot.Local;
import soot.Modifier;
import soot.PatchingChain;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Type;
import soot.jimple.IntConstant;
import soot.jimple.Jimple;
import soot.jimple.Stmt;
import dua.Options;
import dua.global.ProgramFlowGraph;
import dua.global.ReqBranchAnalysis;
import dua.method.AbstractEdge;
import dua.method.CFG;
import dua.method.CFGAnalysis;
import dua.method.Edge;
import dua.method.CFG.CFGNode;
import dua.method.CFGDefUses.Branch;
import dua.method.CFGDefUses.NodeDefUses;

/** Edge/branch instrumenter */
public class BranchInstrumenter {
	
	private static final String BRCOV_ARR_LOCALNAME = "<brCovArr>";
	
	/** For each entry method, the local referencing the branch reporter. */
	private Map<SootMethod,Local> entryReporterLocals = new HashMap<SootMethod, Local>();
	private HashMap<Branch, Integer> brGlobalIndices = new HashMap<Branch, Integer>();
	
	private List<SootField> brCovFields = null; // when using indiv registers
	private SootField brArrayGlobalField = null; // when using array of registers
	private SootClass clsReporter = Scene.v().getSootClass("profile.BranchReporter");
	
	private Type covElemType = IntType.v();
	private static final String REPORT_ARR_METHOD_SIG = "void report(int[])";
	private static final String REPORT_INDIV_METHOD_SIG = "void report(java.lang.Class)";
	private static final String REPORT_FROM_EDGES_METHOD_SIG = "void reportFromEdges(java.lang.Class)";
	
	// DEBUG
	public static Local[] debugLocals = null;
	
	public String getBrCovArrayMethodName() { return "int[] getBrCovArray()"; }
	public Type getBrCovArrayElemType() { return covElemType; }
	
	public Local getEntryReporterLocal(SootMethod mEntry) { return entryReporterLocals.get(mEntry); }
	public HashMap<Branch, Integer> getBrGlobalIndices() { return brGlobalIndices; }
	
	public BranchInstrumenter() {}
	
	/** 
	 *  Branches in a method are indexed from 0 to N-1, according to their order in 
	 *  the method's CFG node list. For a branch node, each outgoing branch is numbered
	 *  as i, i+1, ...
	 */
	public void instrumentDirect(List<Branch> branchesList, List<SootMethod> entryMethods) {
		// pick one class as repository of coverage registers (indiv or array)
//		SootClass covRegClass = entryMethods.get(0).getDeclaringClass();
		SootClass covRegClass = AuxClassInstrumenter.getCreateAuxAppClass();
		
		// create coverage flags (indiv or array) in entry class, if not done yet for class
		createInitBranchCovRegisters(covRegClass, branchesList);
		for (SootMethod entryMethod : entryMethods) {
			// instantiate reporter at end of each entry method, and call reporter
			Local lEntryReporter = UtilInstrum.insertReporterInstantiateCode(entryMethod, "profile.BranchReporter");
			entryReporterLocals.put(entryMethod, lEntryReporter); // associate entry mtd to local referencing reporter
			insertDirectBranchReportCode(entryMethod, covRegClass);
		}
		
		buildBrIndices(branchesList);
		
		// instrument all branches in list
		for (Branch br : brGlobalIndices.keySet()) {
			SootMethod m = ProgramFlowGraph.inst().getContainingCFG(br.getTgt()).getMethod();
			final int brIdx = brGlobalIndices.get(br);
			
			// insert probe at branch
			List probe = new ArrayList();
			if (Options.indivCovReg()) {
				// Set/increment individual cov register
				//   brCovField_BRIDX = 1
				Stmt sSetCovField = Jimple.v().newAssignStmt(
						Jimple.v().newStaticFieldRef(brCovFields.get(brIdx).makeRef()), IntConstant.v(1));
				probe.add(sSetCovField);
			}
			else {
				// Set/increment array (indexed) cov register
				//   brCovArrLocal[BRIDX] = 1
				Local arrayLocal = getCreateCovArrayLocal(m);
				Stmt covSetStmt = Jimple.v().newAssignStmt(
						Jimple.v().newArrayRef(arrayLocal, IntConstant.v(brIdx)), IntConstant.v(1));
				probe.add(covSetStmt);
			}
			
			InstrumManager.v().insertProbeAt(br, probe);
		}
	}
	
	private void buildBrIndices(List<Branch> branchesList) {
		// build global map of br indices
		int globBrIdx = 0;
		System.out.println("Branches to instrument:");
		for (Branch br : branchesList) {
			// store branch global index in map
			System.out.println("  " + globBrIdx + ": " + br);
			brGlobalIndices.put(br, globBrIdx++);
		}
	}
	
	private static final String REPORTER_LOCAL_NAME = "<br_rep>";
	
	/**
	 * Creates global cov array or individual cov registers to track branch coverage
	 */
	private void createInitBranchCovRegisters(SootClass entryClass, List<Branch> branchesList) {
		List probe = new ArrayList();
		SootMethod mClinit = UtilInstrum.getCreateClsInit(entryClass);
		PatchingChain clinitPchain = mClinit.retrieveActiveBody().getUnits();
		
		// Create and instantiate br cov registers at entry class, if not created/instantiated before for class
		if (Options.indivCovReg()) {
			// Create one static field for each branch; they will be automatically initialized by the JVM to 0
			for (int brId = 0; brId < branchesList.size(); ++brId) {
				// create field and add it to entry class
				SootField brCovRegisterFld = new SootField(
						"<br_" + brId + ">", IntType.v(), Modifier.PUBLIC|Modifier.STATIC);
				entryClass.addField(brCovRegisterFld);
				brCovFields.add(brCovRegisterFld);
				
				// init field to 0 at clinit of entry class
				Stmt stmtResetCovFld = Jimple.v().newAssignStmt(
						Jimple.v().newStaticFieldRef(brCovRegisterFld.makeRef()), IntConstant.v(0));
				probe.add(stmtResetCovFld);
			}
		}
		else {
			// Create and instantiate br cov array global field at entry method's class clinit
			brArrayGlobalField = new SootField(
							"<gl_brArray>", ArrayType.v(covElemType, 1), Modifier.PUBLIC|Modifier.STATIC);
			entryClass.addField(brArrayGlobalField);
			
			// assign, at the beginning of each method, array field to array local
			for (SootMethod m : ProgramFlowGraph.inst().getReachableAppMethods()) {
				Local covArrayLocal = getCreateCovArrayLocal(m);
				Stmt arrayToLocalStmt = Jimple.v().newAssignStmt(covArrayLocal, 
						Jimple.v().newStaticFieldRef(brArrayGlobalField.makeRef()));
				probe.clear();
				probe.add(arrayToLocalStmt);
				InstrumManager.v().insertProbeAtEntry(m, probe);
			}
			
			// init array field at the beginning of entry clinit method; use 1 cell per branch
			probe.clear();
			Local covArrayLocal = getCreateCovArrayLocal(mClinit);
			Stmt newArrStmt = Jimple.v().newAssignStmt(covArrayLocal,
					Jimple.v().newNewArrayExpr(covElemType, IntConstant.v( branchesList.size() )));
			probe.add(newArrStmt);
			Stmt covArrLocalToFieldStmt = Jimple.v().newAssignStmt(
					Jimple.v().newStaticFieldRef(brArrayGlobalField.makeRef()), covArrayLocal);
			probe.add(covArrLocalToFieldStmt);
		}
		
		InstrumManager.v().insertRightBeforeNoRedirect(clinitPchain, probe, (Stmt) UtilInstrum.getFirstNonIdStmt(clinitPchain));
	}
	
	/** Inserts code to invoke reporter at END of given entry method, taking direct branch info (indiv or array). */
	private void insertDirectBranchReportCode(SootMethod entryMethod, SootClass covRegClass) {
		List probe = new ArrayList();
		
		Body entryBody = entryMethod.retrieveActiveBody();
		PatchingChain pchainEntry = entryBody.getUnits();
		Stmt sEntryLast = (Stmt) pchainEntry.getLast();
		
		Local lReporter = entryReporterLocals.get(entryMethod);
		
		// Insert code to invoke report method
		if (Options.indivCovReg()) { // Report from individual cov registers found in entry class
			UtilInstrum.insertClassReport(entryMethod, covRegClass, lReporter, clsReporter.getMethod(REPORT_INDIV_METHOD_SIG));
		}
		else {
			//   covArrayLocal = global_covarray
			//   virtualinvoke localBR.<void report(elem[])>(covArrayLocal)
			SootMethod repBrCovMethod = clsReporter.getMethod(REPORT_ARR_METHOD_SIG);
			Local entryCovArrayLocal = getCreateCovArrayLocal(entryMethod);
			Stmt reportCallStmt = Jimple.v().newInvokeStmt(
					Jimple.v().newVirtualInvokeExpr(lReporter, repBrCovMethod.makeRef(), entryCovArrayLocal));
			probe.add(reportCallStmt);
			
			InstrumManager.v().insertAtProbeBottom(pchainEntry, probe, sEntryLast);
			
			// DEBUG
			List<SootMethod> rm = ProgramFlowGraph.inst().getReachableAppMethods();
			debugLocals = new Local[rm.size()];
			for (int i = 0; i < debugLocals.length; ++i)
				debugLocals[i] = getCreateCovArrayLocal(rm.get(i));
		}
	}
	
	private void insertEdgeReportCode(SootMethod entryMethod, SootClass covRegClass) {
		// Report from individual edge cov registers found in entry class
		UtilInstrum.insertClassReport(entryMethod, covRegClass,
				entryReporterLocals.get(entryMethod), clsReporter.getMethod(REPORT_FROM_EDGES_METHOD_SIG));
	}
	
	private Local getCreateCovArrayLocal(SootMethod m) {
		// look for existing array local
		Body b = m.retrieveActiveBody();
		Local baLocal = UtilInstrum.getLocal(b, BRCOV_ARR_LOCALNAME);
		if (baLocal == null) {  // create new local, if not found
			// create new cov array local and add it to method
			baLocal = Jimple.v().newLocal(BRCOV_ARR_LOCALNAME, ArrayType.v(covElemType, 1));
			m.retrieveActiveBody().getLocals().add(baLocal);
		}
		
		return baLocal;
	}
	
	/** 
	 * Profiles edges with one counter per edge, using Ball-Larus '94 optimal method.
	 * Generates 'edges' file with edge info and a mapping of branches to edges.
	 */
	public void instrumentEdgesOptimal(List<Branch> branchesList, ReqBranchAnalysis reqBrAnalysis, List<SootMethod> entryMethods) {
		List<CFG> cfgs = ProgramFlowGraph.inst().getCFGs();
		
		// pick one class as repository of coverage registers (indiv or array)
//		SootClass covRegClass = entryMethods.get(0).getDeclaringClass();
		SootClass covRegClass = AuxClassInstrumenter.getCreateAuxAppClass();
		
		for (SootMethod entryMethod : entryMethods) {
			// at end of each entry method, instantiate reporter and invoke edge-based branch reporter method
			Local lReporter = UtilInstrum.insertReporterInstantiateCode(entryMethod, "profile.BranchReporter");
			entryReporterLocals.put(entryMethod, lReporter);
			insertEdgeReportCode(entryMethod, covRegClass);
		}
		
		buildBrIndices(branchesList);
		
//		SootMethod clinit = UtilInstrum.getCreateClsInit(covRegClass);
//		PatchingChain clinitPchain = clinit.retrieveActiveBody().getUnits();
		
		// build file name
		String fileName = Options.getOutPath();
		final char sepChar = File.separatorChar;
		if (!fileName.isEmpty() && fileName.charAt(fileName.length()-1) != sepChar)
			fileName += sepChar;
		(new File(fileName)).mkdir(); // ensure directory is created
		fileName += "edges";
		
		// write edges for each CFG (method) as we go
		File fEdges = new File(fileName);
		try {
			FileOutputStream oEdges = new FileOutputStream(fEdges);
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(oEdges));
				
			// Optimally instrument each method (CFG) m:
			// - find edges in CFG: E
			// - find max spanning tree of edges: E_t
			// - create coverage registers for E - E_t, as needed
			int mID = 0; // keeps track of unique global method id
			int instrEdges = 0, totalEdges = 0; // track fraction of instrumented edges
			List codeToInsert = new ArrayList();
			for (CFG cfg : cfgs) {
				SootMethod m = cfg.getMethod();
				
				// WRITE: method id
				assert mID == ProgramFlowGraph.inst().getMethodIdx(m); // sanity check
				writer.write("method " + mID + "\n");
				
				// create set E of all edges; position in list is implicit global ID of an edge
				HashMap<CFGNode, ArrayList<Edge>> inEdges = new HashMap<CFGNode, ArrayList<Edge>>();
				HashMap<CFGNode, ArrayList<Edge>> outEdges = new HashMap<CFGNode, ArrayList<Edge>>();
				ArrayList<Edge> allCFGEdges = Edge.createEdges(cfg, inEdges, outEdges);
				
				// link branches to their corresponding edges
				// special entry branch is associated to EXIT->root edge, which represents the count of executions of method
				HashMap<AbstractEdge, Integer> edgeBrIDs = new HashMap<AbstractEdge, Integer>();
				Set<Branch> entryBranches = reqBrAnalysis.getReqBranches(cfg, cfg.ENTRY);
				assert entryBranches.size() == 1;
				edgeBrIDs.put(allCFGEdges.get(0), brGlobalIndices.get(entryBranches.iterator().next()));
				
				for (CFGNode n : cfg.getNodes()) {
					if (n.isInCatchBlock())
						continue;
					
					// associate edge to branch id, if it's a branch
					if (n.getSuccs().size() > 1) {
						// find matching branch
						NodeDefUses nDU = (NodeDefUses)n;
						Branch assocBr = null;
						for (Edge eOut : outEdges.get(n)) {
							for (Branch br : nDU.getOutBranches()) {  // find branch corresponding to edge
								if (br.getTgt() == eOut.getTgt()) {
									assocBr = br;
									break;
								}
							}
							assert assocBr != null;
							Integer assocBrId = brGlobalIndices.get(assocBr);
							if (assocBrId != null)
								edgeBrIDs.put(eOut, assocBrId);
						}
					}
				}
				
				// find maximum spanning tree E_t of edges
				// EXIT->root edge must be in spanning tree, so that it's never instrumented. It is the first edge in list.
				ArrayList<Edge> orderedEdges = CFGAnalysis.getWeightOrderEdges(cfg, inEdges, outEdges, allCFGEdges);
				HashSet<Edge> spanTreeEdges = CFGAnalysis.buildSpanningTree(cfg.ENTRY, cfg.EXIT, orderedEdges);
				
				totalEdges += allCFGEdges.size();
				instrEdges += allCFGEdges.size() - spanTreeEdges.size();
				
				// instrument edges in E - E_t, creating one register for each of them at entry class
				// all edges are written into file; each edge is marked as instrumented or not-instrumented
				// an edge that is a branch is also marked in the file to the associated branch id
				int edgeId = 0;
				for (AbstractEdge _e : allCFGEdges) {
					Edge e = (Edge)_e;
					Integer assocBrId = edgeBrIDs.get(e);
					if (spanTreeEdges.contains(e)) {
						// write edge in file as non-instrumented edge
						writer.write(((assocBrId == null)? "" : "B " + assocBrId + " ") + 
								"N " + e + "\n");
					}
					else {
						// write edge in file as instrumented edge
						writer.write(((assocBrId == null)? "" : "B " + assocBrId + " ") +
								"I " + e + "\n");
						
						// add cov register for edge, initializing it to 0 at clinit of entry class
						SootField edgeCovReg = new SootField("<ed_" + mID + "_" + edgeId + ">", IntType.v(), Modifier.PUBLIC|Modifier.STATIC);
						covRegClass.addField(edgeCovReg);
						
						// instrument edge
						//   countLocal = edgeField;  // getstatic
						//   countLocal = countLocal + 1; // iconst_1, iadd
						//   edgeField = countLocal; // putstatic
						codeToInsert.clear();
						Local countLocal = getCreateEdgeCountLocal(m);
						Stmt sCountFieldToLocal = Jimple.v().newAssignStmt(countLocal, Jimple.v().newStaticFieldRef(edgeCovReg.makeRef()));
						codeToInsert.add(sCountFieldToLocal);
						Stmt sIncCount = Jimple.v().newAssignStmt(countLocal, Jimple.v().newAddExpr(countLocal, IntConstant.v(1)));
						codeToInsert.add(sIncCount);
						Stmt sCountLocalToField = Jimple.v().newAssignStmt(Jimple.v().newStaticFieldRef(edgeCovReg.makeRef()), countLocal);
						codeToInsert.add(sCountLocalToField);
						InstrumManager.v().insertProbeAt(e, codeToInsert);
					}
					
					++edgeId;
				}
				
				// next method
				++mID;
			}
			
			writer.flush();
			
			System.out.println("Edges: total " + totalEdges + ", instr " + instrEdges + ", non-instr " + (totalEdges - instrEdges));
		}
		catch (FileNotFoundException e) {
			System.err.println("Couldn't write EDGES file: " + e);
		}
		catch (SecurityException e) {
			System.err.println("Couldn't write EDGES file: " + e);
		}
		catch (IOException e) {
			System.err.println("Couldn't write EDGES file: " + e);
		}
	}
	
	private static final String EDGE_COUNT_LOCAL_NAME = "<edgecount>";
	private static Local getCreateEdgeCountLocal(SootMethod m) {
		// look for existing array local
		Body b = m.retrieveActiveBody();
		Local ecLocal = UtilInstrum.getLocal(b, EDGE_COUNT_LOCAL_NAME);
		if (ecLocal == null) {  // create new local, if not found
			ecLocal = Jimple.v().newLocal(EDGE_COUNT_LOCAL_NAME, IntType.v());
			b.getLocals().add(ecLocal);
		}
		
		return ecLocal;
	}
	
}
