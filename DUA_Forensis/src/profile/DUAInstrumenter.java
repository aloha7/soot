package profile;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import soot.ArrayType;
import soot.Body;
import soot.ByteType;
import soot.IntType;
import soot.Local;
import soot.Modifier;
import soot.PatchingChain;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Value;
import soot.jimple.ArrayRef;
import soot.jimple.AssignStmt;
import soot.jimple.IdentityStmt;
import soot.jimple.InstanceFieldRef;
import soot.jimple.IntConstant;
import soot.jimple.Jimple;
import soot.jimple.Stmt;
import dua.DUA;
import dua.Options;
import dua.global.ProgramFlowGraph;
import dua.global.ReqBranchAnalysis;
import dua.method.CFGDefUses.Branch;
import dua.method.CFGDefUses.CSReturnedVarCUse;
import dua.method.CFGDefUses.CUse;
import dua.method.CFGDefUses.Def;
import dua.method.CFGDefUses.PUse;
import dua.method.CFGDefUses.Use;
import dua.method.CFGDefUses.Variable;

public class DUAInstrumenter {
	
	private static final String DUA_REP_LOCAL_NAME = "<dua_rep>";
	private static final String DUA_BYTEARR_LOCALNAME = "<duaCovArr>";
	private static final String LD_LOCAL_NAME = "<ld>";
	private static final String REPORT_FROMBR_METHOD = "void reportFromBranches(int[])";
	
	private BranchInstrumenter brInstr;
	private SootClass clsAuxData = null; // initialized only if direct instrumenation 
	private SootField duaArrayGlobalField = null; // initialized only if direct instrumenation
	
	private SootClass clsDUAReporter;
	private SootMethod defEventMethod;
	private SootMethod useEventMethod;
	
	public DUAInstrumenter(BranchInstrumenter brInstr) {
		this.brInstr = brInstr;
		
		this.clsDUAReporter = Scene.v().getSootClass("profile.DUAReporter");
		// varId, defIdx, objContainer, arrIdx
		this.defEventMethod = clsDUAReporter.getMethod("void defEvent(int,int,java.lang.Object,int)");
		// varId, objContainer, arrIdx
		this.useEventMethod = clsDUAReporter.getMethod("void useEvent(int,int,byte[],java.lang.Object,int)");
	}
	
	/** 
	 * Instruments for DUAs directly and/or from branches, as specified by option.
	 * Inserts probe at end of entry method that instantiates and runs DUA reporter.
	 */
	public void instrument(Collection<DUA> duas, ReqBranchAnalysis reqBrAnalysis, List<SootMethod> entryMethods) {
		List probe = new ArrayList();
		
		// prepare to instrument each entry method, by instantiating DUA reporter at END of method
		Map<SootMethod,Local> duaRepLocals = new HashMap<SootMethod, Local>();
		for (SootMethod entryMethod : entryMethods) {
			probe.clear();
			
			Body bodyEntry = entryMethod.retrieveActiveBody();
			PatchingChain pchainEntry = bodyEntry.getUnits();
			Stmt sEntryLast = (Stmt) pchainEntry.getLast();
			
			// create local var to hold DUAReporter instance
			Local duaRepLocal = Jimple.v().newLocal(DUA_REP_LOCAL_NAME, clsDUAReporter.getType());
			duaRepLocals.put(entryMethod, duaRepLocal);
			bodyEntry.getLocals().add(duaRepLocal);
			
			// insert code to instantiate DUA reporter at end of entry method
	        //   dua_rep = new profile.DUAReporter;
	        //   specialinvoke dua_rep.<profile.DUAReporter: void <init>()>();
			SootMethod duaRepCtorMethod = clsDUAReporter.getMethod("void <init>()");
			Stmt newDUARepStmt = Jimple.v().newAssignStmt(duaRepLocal,
					Jimple.v().newNewExpr(clsDUAReporter.getType()));
			probe.add(newDUARepStmt);
			Stmt initDUARepStmt = Jimple.v().newInvokeStmt(
				Jimple.v().newSpecialInvokeExpr(duaRepLocal, duaRepCtorMethod.makeRef()));
			probe.add(initDUARepStmt);
			
			InstrumManager.v().insertAtProbeBottom(pchainEntry, probe, sEntryLast);
		}
		
		// instrument to derive DUA coverage from branches
		if (Options.duaInstrBranches()) {
			// generate file with dua branches info
			genDUAFile(duas, reqBrAnalysis);
			
			// insert reporting code at END of each entry method
			for (SootMethod entryMethod : entryMethods) {
				probe.clear();
				PatchingChain pchainEntry = entryMethod.retrieveActiveBody().getUnits();
				Stmt sEntryLast = (Stmt) pchainEntry.getLast();
				
				// insert code to get global branch cov array from branch reporter
				//   brarrtodua = virtualinvoke br.<profile.BranchReporter: elem[] getBrCovArray()>();
				SootClass clsBranchReporter = Scene.v().getSootClass("profile.BranchReporter");
				SootMethod getBrCovArrayMethod = clsBranchReporter.getMethod(brInstr.getBrCovArrayMethodName());
				SootClass clsBitSet = Scene.v().getSootClass("java.util.BitSet");
				Local baLocal = Jimple.v().newLocal("<brarrtodua>", ArrayType.v(brInstr.getBrCovArrayElemType(), 1));
				entryMethod.retrieveActiveBody().getLocals().add(baLocal);
				Local brRepLocal = brInstr.getEntryReporterLocal(entryMethod);
				Stmt getBrCovArrayStmt = Jimple.v().newAssignStmt(
						baLocal, Jimple.v().newVirtualInvokeExpr(brRepLocal, getBrCovArrayMethod.makeRef()));
				probe.add(getBrCovArrayStmt);
				
				// insert code to invoke DUAReporter's report-from-branches method
				//   virtualinvoke dr.<profile.DUAReporter: void reportFromBranches(elem[])>(gl_bs);
				SootMethod repFromBrMethod = clsDUAReporter.getMethod(REPORT_FROMBR_METHOD);
				Stmt repFromBrInvokeStmt = Jimple.v().newInvokeStmt(
						Jimple.v().newVirtualInvokeExpr(duaRepLocals.get(entryMethod), repFromBrMethod.makeRef(), baLocal));
				probe.add(repFromBrInvokeStmt);
				
				InstrumManager.v().insertAtProbeBottom(pchainEntry, probe, sEntryLast);
			}
			
		}
		
		// instrument to track DUA coverage directly
		if (Options.duaInstrDirect()) {
			// pick first entry method's class as holder of direct DUA tracking fields
			clsAuxData = AuxClassInstrumenter.getCreateAuxAppClass();
			
			// directly instrument ALL duas in program
			instrumentDirectly(duas);
			
			// insert at end of each entry method code to invoke DUAReporter's report-directly method
			for (SootMethod entryMethod : entryMethods) {
				probe.clear();
				PatchingChain pchainEntry = entryMethod.retrieveActiveBody().getUnits();
				Stmt sEntryLast = (Stmt) pchainEntry.getLast();
				
				//   duaArrLocal = duaArrField;
				//   virtualinvoke dr.<profile.DUAReporter: void reportDirectly(byte[])>(duaArrLocal);
				Local baLocal = getCreateByteArrayLocal(entryMethod);
				Stmt duaArrFieldToLocalStmt = Jimple.v().newAssignStmt(
						baLocal, Jimple.v().newStaticFieldRef(duaArrayGlobalField.makeRef()));
				probe.add(duaArrFieldToLocalStmt);
				
				SootMethod repDirectlyMethod = clsDUAReporter.getMethod("void reportDirectly(byte[])");
				Stmt repDirectlyInvokeStmt = Jimple.v().newInvokeStmt(
						Jimple.v().newVirtualInvokeExpr(duaRepLocals.get(entryMethod), repDirectlyMethod.makeRef(), baLocal));
				probe.add(repDirectlyInvokeStmt);
				
				InstrumManager.v().insertAtProbeBottom(pchainEntry, probe, sEntryLast);
			}
		}
	}
	
	private void genDUAFile(Collection<DUA> duas, ReqBranchAnalysis reqBrAnalysis) {
		// build file name
		String fileName = Options.getOutPath();
		final char sepChar = File.separatorChar;
		if (!fileName.isEmpty() && fileName.charAt(fileName.length()-1) != sepChar)
			fileName += sepChar;
		(new File(fileName)).mkdir(); // ensure directory is created
		fileName += "duas";
		
		// write duas, one by one
		File fDUA = new File(fileName);
		try {
			FileOutputStream oDUA = new FileOutputStream(fDUA);
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(oDUA));
			writer.write(Options.hybridInstr()? "H\n" : "N\n"); // first, indicate if this is hybrid instrumentation
			for (DUA dua : duas) {
				// DUA file format (line sequence): 
				//   1. in-order DU char (I|N) + dua name string
				writer.write(dua.isInferrableOrCondInf()? "I " : "N ");
				writer.write(dua.toString() + "\n");
				//   2. rdf branch ids for def
				writer.write(toDUAFileString(reqBrAnalysis.getReqBranches(dua.getDef().getN())) + "\n");
				//   3. rdf branch ids for use
				writer.write(toDUAFileString(reqBrAnalysis.getUseReqBranches(dua.getUse())) + "\n");
				//   4. rdf branch ids for in-order kills
				writer.write(toDUAFileString(reqBrAnalysis.getReqBranches(dua.getInOrderKillReqNodes())) + "\n");
				//   5. rdf branch ids for not-in-order kills
				writer.write(toDUAFileString(reqBrAnalysis.getReqBranches(dua.getNotInOrderKillReqNodes())) + "\n");
			}
			writer.flush();
		}
		catch (FileNotFoundException e) {
			System.err.println("Couldn't write DUA file: " + e);
		}
		catch (SecurityException e) {
			System.err.println("Couldn't write DUA file: " + e);
		}
		catch (IOException e) {
			System.err.println("Couldn't write DUA file: " + e);
		}
	}
	
	private String toDUAFileString(Collection<Branch> brs) {
		String brIdxStr = "";
		HashMap<Branch, Integer> brIdxMap = brInstr.getBrGlobalIndices();
		
		for (Branch br : brs) {
			assert brIdxMap.containsKey(br);
			brIdxStr += brIdxMap.get(br) + " ";
		}
		
		return brIdxStr;
	}
	
	/**
	 * Instruments given DUAs directly, using last-def register for each var.
	 * A last-def register is a static field of the class that contains the var (as field or in a method).
	 * Each def has a global ID, and duas are tracked in a global bitset.
	 * 
	 * ASSUMES that all defs for a given var are in the DUA set; no kills are taken into account.
	 * TODO: problem: there might be a def with no reachable use that kills a DUA...
	 */
	private void instrumentDirectly(Collection<DUA> duas) {
		// 1. Create map use->global_idx; for each var, assign idx to each def having
		//    a use of that var
		int nextUseGlobalIdx = 0;
		int maxDefsPerVar = 0;
		HashMap<Use, Integer> useIdxs = new HashMap<Use, Integer>();
		HashMap<Variable, Integer> varIds = new HashMap<Variable, Integer>();
		HashMap<Variable, Integer> numDefsPerVar = new HashMap<Variable, Integer>();
		HashMap<Use, Integer> numDefsPerUse = new HashMap<Use, Integer>();
		HashMap<Variable, Set<SootMethod>> varMethodMap = new HashMap<Variable, Set<SootMethod>>();
		HashMap<Def, HashMap<Variable,Integer>> varDefIds = new HashMap<Def, HashMap<Variable,Integer>>();
		HashMap<Use, Def> useUniqueDef = new HashMap<Use, Def>();
		BitSet usesRequiringProbe = new BitSet(); // used in hybrid instrumentation
		int nextVarId = 0;
		for (DUA dua : duas) {
			Use use = dua.getUse();
//			if (!(use.getValue() instanceof Local))
//				continue; // TODO: we don't support instr of non-locals, yet
			
			// add use to global idx map, if met for the first time
			Integer useIdx = useIdxs.get(use);
			if (useIdx == null) {  // first time this def is found
				// create index for use and add entry to use->idx map
				useIdx = new Integer(nextUseGlobalIdx++); // increment idx counter, also
				useIdxs.put(use, useIdx);
			}
			
			// determine if use requires probe
			if (!dua.isDefinitelyInferrable())
				usesRequiringProbe.set(useIdx);
			
			// assign id to var, if var found for the first time
			Variable var = use.getVar();
			if (!varIds.containsKey(var))
				varIds.put(var, nextVarId++);
			
			// map def to use's var, if not mapped yet, assigning idx to def for that var
			Def def = dua.getDef();
			HashMap<Variable,Integer> idsForDef = varDefIds.get(def);
			if (idsForDef == null) {  // found def for the first time; create def id map
				idsForDef = new HashMap<Variable, Integer>();
				varDefIds.put(def, idsForDef);
			}
			if (!idsForDef.containsKey(var)) {  // need to assign id to def for this var
				// get id
				Integer defId = numDefsPerVar.get(var);
				if (defId == null)  // found var for the first time
					defId = 0;
				// update id count for def and var, number of defs per var, and max # defs per var
				idsForDef.put(var, defId);
				++defId;
				numDefsPerVar.put(var, defId);
				if (defId > maxDefsPerVar)
					maxDefsPerVar = defId;
			}
			
			// update count of defs for this use, and max defs per use counter
			Integer numDefs = numDefsPerUse.get(use);
			if (numDefs == null) {  // found use for the first time
				numDefs = 1;
				numDefsPerUse.put(use, numDefs);
				
				useUniqueDef.put(use, def);
			}
			else {
				++numDefs;
				numDefsPerUse.put(use, numDefs);
				
				useUniqueDef.remove(use);
			}
			
			// link var to methods containing its uses
			Set<SootMethod> usingMtds = varMethodMap.get(var);
			if (usingMtds == null) {
				usingMtds = new HashSet<SootMethod>();
				varMethodMap.put(var, usingMtds);
			}
			usingMtds.add(ProgramFlowGraph.inst().getContainingMethod(use.getSrcNode().getStmt()));
		}
		
		// now we can determine which vars have uses with multiple defs
		HashSet<Variable> varsWithMultiDefUses = new HashSet<Variable>();
		for (Use use : numDefsPerUse.keySet()) {
//			if (!(use.getValue() instanceof Local))
//				continue; // TODO: we don't support instr of non-locals, yet
			if (numDefsPerUse.get(use) > 1)
				varsWithMultiDefUses.add(use.getVar());
		}
		
		// compute set of defs of vars requiring a probe, only if hybrid instr is selected
		HashMap<Def, HashSet<Variable>> defsVarsRequiringProbe = new HashMap<Def, HashSet<Variable>>();
		if (Options.hybridInstr()) {
			for (DUA dua : duas) {
				Use use = dua.getUse();
				
//				if (!(use.getValue() instanceof Local))
//					continue; // TODO: we don't support instr of non-locals, yet
				
				if (usesRequiringProbe.get( useIdxs.get(use) )) {
					// add def of var to "requiring-def-probe" set
					Def def = dua.getDef();
					HashSet<Variable> varsForDefReqProbe = defsVarsRequiringProbe.get(def);
					if (varsForDefReqProbe == null) {
						varsForDefReqProbe = new HashSet<Variable>();
						defsVarsRequiringProbe.put(def, varsForDefReqProbe);
					}
					varsForDefReqProbe.add(use.getVar());
				}
			}
		}
		
		// 2. Add global dua array of bytes indexed by global use id times max # defs per use
		//    (*** Does this hold anymore?) We are assuming no more than 32 defs per use; otherwise, we can use long,
		//    or sequences of N bytes per use
        // At clinit in entry's class:
		//   $r0 = newarray (byte)[sizeconstant];
        //   duaByteArrayField = $r0;
		
		// round defs per use value to first highest power of 2
		int entriesPerUse = 1;
		int globalIdxShiftLefts = 0;
		while (entriesPerUse < maxDefsPerVar) {
			entriesPerUse *= 2;
			++globalIdxShiftLefts;
		}
		System.out.println("DUA instr: max defs per var " + maxDefsPerVar + ", entries per use " + entriesPerUse);
		
		// create public static field in entry class
		duaArrayGlobalField = new SootField(
						"<gl_duaArray>", ArrayType.v(ByteType.v(), 1), Modifier.PUBLIC|Modifier.STATIC);
		clsAuxData.addField(duaArrayGlobalField);
		
		// assign, at the beginning of each method for which there is some use, array field to array local
		for (Set<SootMethod> usingMethods : varMethodMap.values()) {
			for (SootMethod m : usingMethods) {
				PatchingChain pchain = m.retrieveActiveBody().getUnits();
				Stmt sFirstNonId = (Stmt) pchain.getFirst();
				while (sFirstNonId instanceof IdentityStmt)
					sFirstNonId = (Stmt) pchain.getSuccOf(sFirstNonId);
				
				List probe = new ArrayList();
				Local byteArrayLocal = getCreateByteArrayLocal(m);
				Stmt arrayToLocalStmt = Jimple.v().newAssignStmt(byteArrayLocal, 
						Jimple.v().newStaticFieldRef(duaArrayGlobalField.makeRef()));
				probe.add(arrayToLocalStmt);
				InstrumManager.v().insertRightBeforeNoRedirect(pchain, probe, sFirstNonId);
			}
		}
		
		// init array field at the beginning of clinit; use 1 byte per def
		SootMethod clinit = UtilInstrum.getCreateClsInit(clsAuxData);
		PatchingChain clinitPchain = clinit.retrieveActiveBody().getUnits();
		List probe = new ArrayList();
		Local byteArrayLocal = getCreateByteArrayLocal(clinit);
		Stmt newArrStmt = Jimple.v().newAssignStmt(byteArrayLocal,
				Jimple.v().newNewArrayExpr(ByteType.v(), IntConstant.v(nextUseGlobalIdx * entriesPerUse)));
		probe.add(newArrStmt);
		Stmt byteArrLocalToFieldStmt = Jimple.v().newAssignStmt(
				Jimple.v().newStaticFieldRef(duaArrayGlobalField.makeRef()), byteArrayLocal);
		probe.add(byteArrLocalToFieldStmt);
		
		InstrumManager.v().insertRightBeforeNoRedirect(clinitPchain, probe, (Stmt) clinitPchain.getFirst());
		
		// 3. At each def, insert code to update all its related last-def indices
		//    ... except for vars where all uses have only one def
		//    ... and EXCEPT for vars that are "not definite" (i.e., depend on dynamic container,
		//        such as instance fields, array elements, and objects).
		//    In the last exception case, use call to monitor to deal with it.
		//    Create last-def registers for each var on demand
		HashMap<Variable, SootField> lastDefFields = new HashMap<Variable, SootField>();
		int defProbeCount = 0;
		for (Def def : varDefIds.keySet()) {
			// get set of vars defined by def that require instr, in hybrid case
			HashSet<Variable> varsForDefRequiringProbe = defsVarsRequiringProbe.get(def);
			if (Options.hybridInstr() && varsForDefRequiringProbe == null)
				continue;  // no probes at all for this def in hybrid case
			
			HashMap<Variable,Integer> varToDefId = varDefIds.get(def);
			
			// find insertion point for def probe
			Stmt insertStmt = def.getN().getStmt();
			SootMethod m = ProgramFlowGraph.inst().getContainingMethod(insertStmt);
			PatchingChain pchain = m.retrieveActiveBody().getUnits();
			while (insertStmt instanceof IdentityStmt)  // move first non-identify stmt
				insertStmt = (Stmt) pchain.getSuccOf(insertStmt);
			
			// for each related var, add code to update last-def register with this def's id
			// ... only if var has at least one use with more than one def
			// ... and var is "definite" (statically guaranteed to be the same everywhere)
			for (Variable var : varToDefId.keySet()) {
				if (!varsWithMultiDefUses.contains(var))
					continue;  // var has no use with more than one def
				
				if (Options.hybridInstr() && !varsForDefRequiringProbe.contains(var))
					continue;  // this particular var def does not require probe in hybrid case
				
				final int varId = varIds.get(var);
				final int defIdx = varToDefId.get(var);
				if (var.isDefinite())
					insertLastDefCode(lastDefFields, varId, defIdx, insertStmt, pchain, var);
				else
					insertDefMonitorCall(insertStmt, pchain, var, varId, defIdx);
				
				++defProbeCount;
			}
		}
		
		// 4. For each use, add code to check last def and mark DUA bit
		//    If use has only one def, don't check last-def
		int[] useProbeCount = new int[2]; // small, normal
		for (Use use : useIdxs.keySet()) {
			final int useId = useIdxs.get(use);
			
			if (Options.hybridInstr() && !usesRequiringProbe.get(useId))
				continue;  // use doesn't need probe in hybrid mode
			
			// get probe insert point
			Stmt useStmt = use.getSrcNode().getStmt();
			SootMethod m = ProgramFlowGraph.inst().getContainingMethod(useStmt);
			PatchingChain pchain = m.retrieveActiveBody().getUnits();
			
			// create code to set corresponding byte in array to 1, or to call reporter if var is not "definite"
			Variable var = use.getVar();
			final int baseByteIdx = useId << globalIdxShiftLefts;
			if (var.isDefinite())
				probe = createDUAProbeAtUse(numDefsPerUse, varDefIds, useUniqueDef, baseByteIdx, lastDefFields, useProbeCount, use, m);
			else
				probe = createUseMonitorCall(var.getValue(), varIds.get(var), baseByteIdx, m);
			
			// insert probe at use, according to the type of the use
			if (use instanceof CSReturnedVarCUse)  // execute *after* call returns, not before!
				InstrumManager.v().insertAfter(pchain, probe, useStmt);
			else if (use instanceof CUse)  // insert at top of tgt's probe, so use probe is executed before def probe
				InstrumManager.v().insertAtProbeTop(pchain, probe, useStmt);
			else
				InstrumManager.v().insertProbeAt(((PUse)use).getBranch(), probe); // insert probe at branch
		}
		
		System.out.println("Def probes inserted: " + defProbeCount);
		System.out.println("Use probes inserted: all " + (useProbeCount[0] + useProbeCount[1])
				+ ", small " + useProbeCount[0] + ", normal " + useProbeCount[1]);
		
		// 5. Build map of bytes in global dua array to dua idxs in original collection
		int[] duaArrayIdxs = new int[duas.size()];
		int duaIdx = 0;
		for (DUA dua : duas) {
			Use use = dua.getUse();
//			if (!(use.getValue() instanceof Local))
//				continue; // TODO: we don't support instr of non-locals, yet
			
			HashMap<Variable,Integer> localsForDef = varDefIds.get(dua.getDef());
			Variable useVar = use.getVar();
			// find use local in def->(local->int) map
			int defId = -1;
			for (Variable var : localsForDef.keySet()) {
				if (var.equals(useVar)) {
					defId = localsForDef.get(var);
					break;
				}
			}
			assert defId != -1;
			final int byteIdx = defId + (useIdxs.get(use) << globalIdxShiftLefts);
			duaArrayIdxs[duaIdx++] = byteIdx;
		}
		genDUAMapFile(duaArrayIdxs); // store map in file
	}
	
	/** @return probe that needs to be inserted */
	private List createDUAProbeAtUse(HashMap<Use, Integer> numDefsPerUse, HashMap<Def, HashMap<Variable, Integer>> varDefIds,
			HashMap<Use, Def> useUniqueDef, int baseByteIdx, HashMap<Variable, SootField> lastDefFields, 
			int[] useProbeCount, Use use, SootMethod m)
	{
		List probe = new ArrayList();
		if (numDefsPerUse.get(use) == 1) {
			// simplified code; just one possible def, determinable statically
			// constant: USE_IDX = useId << shiftleftConst
			// constant: DUA_IDX = USE_IDX + UNIQUE_DEF_ID
			//   arrLocal[DUA_IDX] = 1;
			
			// find use var in def->(var->int) map
			Def def = useUniqueDef.get(use);
			HashMap<Variable,Integer> varsForDef = varDefIds.get(def);
			Variable useVar = use.getVar();
			int defId = -1;
			for (Variable var : varsForDef.keySet()) {
				if (var.equals(useVar)) {
					defId = varsForDef.get(var);
					break;
				}
			}
			assert defId != -1;
			
			// insert code:  arrLocal[DUA_IDX] = 1;
			final int byteIdx = defId + (baseByteIdx);
			Local arrayLocal = getCreateByteArrayLocal(m);
			Stmt byteSetStmt = Jimple.v().newAssignStmt(
					Jimple.v().newArrayRef(arrayLocal, IntConstant.v(byteIdx)), IntConstant.v(1));
			probe.add(byteSetStmt);
			
			++useProbeCount[0]; // small probe
		}
		else {
			// constant: USE_IDX = useId << shiftleftConst
			//   idxLocal = ldField; // field for var
			//   idxLocal = idxLocal + USE_IDX;
			//   arrLocal[idxLocal] = 1;
			Variable var = use.getVar();
			SootField lastDefField = lastDefFields.get(var);
			Local idxLocal = getCreateLastDefLocal(m);
			
			Stmt idFieldToLocalStmt = Jimple.v().newAssignStmt(idxLocal, 
					Jimple.v().newStaticFieldRef(lastDefField.makeRef()));
			probe.add(idFieldToLocalStmt);
			
			Stmt useIdPlusLDStmt = Jimple.v().newAssignStmt(
					idxLocal, Jimple.v().newAddExpr(idxLocal, IntConstant.v(baseByteIdx)));
			probe.add(useIdPlusLDStmt);
			
			Local arrayLocal = getCreateByteArrayLocal(m);
			Stmt byteSetStmt = Jimple.v().newAssignStmt(
					Jimple.v().newArrayRef(arrayLocal, idxLocal), IntConstant.v(1));
			probe.add(byteSetStmt);
			
			++useProbeCount[1]; // normal probe
		}
		
		return probe;
	}
	
	/** Instead of using last-def register, this method calls monitor with:
	 *    var id, containing obj (field, array, obj), and array-index (if appropriate).
	 *  ASSUMES, for now, that var is an instance field or an array element. */
	private List createUseMonitorCall(Value vUse, int varId, int baseByteIdx, SootMethod m) {
		// obtain container object and index values
		Value vBase = (vUse instanceof InstanceFieldRef)? ((InstanceFieldRef)vUse).getBase() :
						((ArrayRef)vUse).getBase();
		Value vIndex = (vUse instanceof ArrayRef)? ((ArrayRef)vUse).getIndex() : IntConstant.v(-1);
		
		// build args list for reporter call
		List args = new ArrayList();
		args.add(IntConstant.v(varId));
		args.add(IntConstant.v(baseByteIdx));
		Local arrayLocal = getCreateByteArrayLocal(m);
		args.add(arrayLocal);
		args.add(vBase);
		args.add(vIndex);
		
		// call reporter's def event method
		List probe = new ArrayList();
		Stmt sCallUseEvent = Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(this.useEventMethod.makeRef(), args));
		probe.add(sCallUseEvent);
		
		return probe;
	}
	
	private void insertLastDefCode(
			HashMap<Variable, SootField> lastDefFields, int varId, int defIdx, 
			Stmt insertStmt, PatchingChain pchain, Variable var)
	{
		// get/create last-def field for this var
		SootField lastDefField = lastDefFields.get(var);
		if (lastDefField == null) {
			// create public field of type int and add it aux class
			lastDefField = new SootField(
					"<ld_" + varId + "_" + var + ">", IntType.v(),
					Modifier.PUBLIC | Modifier.STATIC);
			clsAuxData.addField(lastDefField);
			// store field in map
			lastDefFields.put(var, lastDefField);
		}
		// add code to update var's last-def field with this def's idx
		List probe = new ArrayList();
		Stmt updateLastDefStmt = Jimple.v().newAssignStmt(
				Jimple.v().newStaticFieldRef(lastDefField.makeRef()), 
				IntConstant.v(defIdx));
		probe.add(updateLastDefStmt);
		InstrumManager.v().insertAtProbeTop(pchain, probe, insertStmt);
	}
	
	/** Instead of updating a last-def field, this method calls monitor with:
	 *    var id, def's idx for var, containing obj (field, array, obj), and array-index (if appropriate).
	 *  ASSUMES, for now, that var is an instance field or an array element. */
	private void insertDefMonitorCall(Stmt insertStmt, PatchingChain pchain, Variable var, int varId, int defIdx) {
		// obtain container object and index values
		Value vLeft = ((AssignStmt)insertStmt).getLeftOp();
		Value vBase = (vLeft instanceof InstanceFieldRef)? ((InstanceFieldRef)vLeft).getBase() :
						((ArrayRef)vLeft).getBase();
		Value vIndex = (vLeft instanceof ArrayRef)? ((ArrayRef)vLeft).getIndex() : IntConstant.v(-1);
		
		// build args list for reporter call
		List args = new ArrayList();
		args.add(IntConstant.v(varId));
		args.add(IntConstant.v(defIdx));
		args.add(vBase);
		args.add(vIndex);
		
		// call reporter's def event method
		List probe = new ArrayList();
		Stmt sCallDefEvent = Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(this.defEventMethod.makeRef(), args));
		probe.add(sCallDefEvent);
		InstrumManager.v().insertAtProbeTop(pchain, probe, insertStmt);
	}
	
	private Local getCreateByteArrayLocal(SootMethod m) {
		// look for existing byte array local
		Body b = m.retrieveActiveBody();
		Local baLocal = UtilInstrum.getLocal(b, DUA_BYTEARR_LOCALNAME);
		if (baLocal == null) {  // create new local, if not found
			// create new byte array local and add it to method
			baLocal = Jimple.v().newLocal(DUA_BYTEARR_LOCALNAME, ArrayType.v(ByteType.v(), 1));
			m.retrieveActiveBody().getLocals().add(baLocal);
		}
		
		return baLocal;
	}
	
	private Local getCreateLastDefLocal(SootMethod m) {
		// look for existing bs local
		Body b = m.retrieveActiveBody();
		Local ldLocal = UtilInstrum.getLocal(b, LD_LOCAL_NAME);
		if (ldLocal == null) {  // create new local, if not found
			// create new int local and add it to method
			ldLocal = Jimple.v().newLocal(LD_LOCAL_NAME, IntType.v());
			m.retrieveActiveBody().getLocals().add(ldLocal);
		}
		
		return ldLocal;
	}
	
	/** Creates file with map of 0-based dua id to idx of dua in byte array */
	private void genDUAMapFile(int[] duaArrayIdxs) {
		// build file name
		String fileName = Options.getOutPath();
		final char sepChar = File.separatorChar;
		if (!fileName.isEmpty() && fileName.charAt(fileName.length()-1) != sepChar)
			fileName += sepChar;
		(new File(fileName)).mkdir(); // ensure directory is created
		fileName += "duasidxs";
		
		// write indices, separated by newlines
		File fDUAIdxs = new File(fileName);
		try {
			FileOutputStream oDUAIdxs = new FileOutputStream(fDUAIdxs);
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(oDUAIdxs));
			writer.write(Options.hybridInstr()? "H\n" : "N\n"); // first, indicate if this is hybrid instrumentation
			for (int i = 0; i < duaArrayIdxs.length; ++i)
				writer.write(duaArrayIdxs[i] + "\n");
			writer.flush();
		}
		catch (FileNotFoundException e) {
			System.err.println("Couldn't write DUAIdxs file: " + e);
		}
		catch (SecurityException e) {
			System.err.println("Couldn't write DUAIdxs file: " + e);
		}
		catch (IOException e) {
			System.err.println("Couldn't write DUAIdxs file: " + e);
		}
	}
	
}
