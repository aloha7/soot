package dua;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import soot.RefType;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Type;
import soot.Value;
import soot.jimple.ArrayRef;
import soot.jimple.FieldRef;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.StaticInvokeExpr;
import dua.global.ProgramFlowGraph;
import dua.global.ReachabilityAnalysis;
import dua.method.CFG;
import dua.method.CFGFactory;
import dua.method.DominatorRelation;
import dua.method.MethodTag;
import dua.method.ReachableUsesDefs;
import dua.method.CFG.CFGNode;
import dua.method.CFGDefUses.Branch;
import dua.method.CFGDefUses.Def;
import dua.method.CFGDefUses.PUse;
import dua.method.CFGDefUses.Use;
import dua.method.CFGDefUses.StdVariable;
import dua.method.CFGDefUses.Variable;
import dua.method.CFGDefUses.Def.DefComparator;
import dua.method.CFGDefUses.Use.UseComparator;
import dua.util.Pair;

/**
 * Reachable-uses based interprocedural DUA analysis, for local variables only.
 * Used reachability and dom/pdom analyses to determine "in-order" property of def-use, 
 * as well as def-kill and kill-use.
 * Constructs all DUAs found, associating kills to it. Kills for a DUA (d,u) are found 
 * by matching def d with all defs of var(d) that are reachable from d, and that 
 * also happen to reach the same use u.
 * Note that this method for finding kills is overly conservative. If a kill k for (d,u)
 * occurs always after d-u is covered (i.e., k "postdominates u" respect to d), it will
 * never really kill (d,u). This case happens in loops, where k can reach u by taking a
 * backedge. Also, note that (k,u) is not necessarily a DUA. It is only a DUA if there
 * is a def-free path from k to u.
 */
public class DUAAnalysis {
	/** CFG factory class that creates CFGs to work with DUAAnalysis */
	private static class RUCFGFactory implements CFGFactory {
		public CFG createCFG(SootMethod m) { return new ReachableUsesDefs(m); }
		public void analyze(CFG cfg) { cfg.analyze(); }
	}
	/** Holds CFG factory singleton instance */
	private static CFGFactory cfgFactorySingleton = new RUCFGFactory();
	/** Returns CFG factory singleton instance */
	public static CFGFactory getCFGFactory() { return cfgFactorySingleton; }
	
	/** Main helper object, which stores and manages DUAs created here */
	private DUAssocSet duaSet = new DUAssocSet();
	public DUAssocSet getDUASet() { return duaSet; }
	
	private static DUAAnalysis duaAnInstance = null;
	public static void createDUAAnalysis() { duaAnInstance = new DUAAnalysis(); }
	public static DUAAnalysis inst() { return duaAnInstance; }
	
	private static final boolean verbose = false; //true;
	private DUAAnalysis() {
		// prepare required collection and map
		List<SootMethod> allReachableMethods = ProgramFlowGraph.inst().getReachableAppMethods();
		Map<SootMethod, ReachableUsesDefs> methodsToReachUseDefs = dua.util.Util.convertToRUMap(ProgramFlowGraph.inst().getMethodToCFGMap());
		
		if (!Options.localDUAsOnly())
			computeFieldArrayObjDUAs(allReachableMethods, methodsToReachUseDefs);
		
		// goal: find inter-proc uses for each param of each method (reachable from entry)
		
		// ReachableUses already inited param to local real uses, and linked params to local call uses
		
		// iteratively:
		// for each method, add to each param all real uses of call uses for that param
		ArrayList<SootMethod> worklist = new ArrayList<SootMethod>(allReachableMethods);
		int propCount = 1;
		while (!worklist.isEmpty()) {
			System.out.println("Inter-procedural reachable uses to params iteration #" + propCount++);			
			for (SootMethod m : (ArrayList<SootMethod>) worklist.clone()) {
				worklist.remove(m);
				
				ReachableUsesDefs ru = methodsToReachUseDefs.get(m);
				if (ru.propagateAllUsesDefsToParams(methodsToReachUseDefs)) {
					MethodTag mTag = (MethodTag) m.getTag(MethodTag.TAG_NAME);
					for (SootMethod mCaller : mTag.getCallerMethods()) {
						MethodTag mCallerTag = (MethodTag) mCaller.getTag(MethodTag.TAG_NAME);
						if (mCallerTag.isReachableFromEntry())
							worklist.add(mCaller);
					}
				}
			}
		}
		
		for (SootMethod m : allReachableMethods) {
			ReachableUsesDefs ru = methodsToReachUseDefs.get(m);
			
			// DEBUG
			ru.dumpReachUsesDefs();
			
			ru.findLocalUsesDefsForDefs(methodsToReachUseDefs);
		}
		
		// for each def, create duas for local real uses and real uses of params in call uses
		// also, collect statistics
		// first, determine # interproc defs per use
		HashMap<Use, Integer> interProcDefsPerUse = new HashMap<Use, Integer>(); // maps use -> # interproc defs
		for (SootMethod m : allReachableMethods) {
			ReachableUsesDefs ru = methodsToReachUseDefs.get(m);
			HashMap<Def, HashMap<Use, ArrayList<Use>>> dus = ru.getDUs();
			for (Def def : dus.keySet()) {
				Map<Use, ArrayList<Use>> usesMap = dus.get(def);
				for (Use use : usesMap.keySet()) {
					// determine and update # of interproc defs for use
					Integer numInterDefs = interProcDefsPerUse.get(use);
					if (numInterDefs == null) {
						numInterDefs = 0;
						interProcDefsPerUse.put(use, numInterDefs); // just init entry in map
					}
				}
			}
		}
		
		// create local var DUAs
		int cDuas = 0, pDuas = 0, cIntraOnly = 0, cInterOnly = 0, pIntraOnly = 0, pInterOnly = 0;
		int numKills = 0, minKills = Integer.MAX_VALUE, maxKills = 0;
		int possibleKills = 0, subsDuasWithPossKill = 0;
		ArrayList<Integer> useCallDepthsCount = new ArrayList<Integer>();
		for (SootMethod m : allReachableMethods) {  // methods are sorted
			if (verbose)
				System.out.print("Local-var duas for " + m + ": ");
			
			// build map stmt->idx
			ReachableUsesDefs ru = methodsToReachUseDefs.get(m);
			HashMap<Def, HashMap<Use, ArrayList<Use>>> dus = ru.getDUs();
			HashMap<Def, HashSet<Def>> dds = ru.getDDs();
			
			// determine subsumability and update stats for these DUAs / DDAs
			List<Def> sortedDefs = new ArrayList<Def>(dus.keySet());
			Collections.sort(sortedDefs, new DefComparator());
			for (Def def : sortedDefs) {
				HashSet<Def> defKills = dds.get(def);
				
				if (verbose)
					System.out.print(def + "={");
				
				Map<Use, ArrayList<Use>> realToLocalUsesMap = dus.get(def);
				List<Use> sortedUses = new ArrayList<Use>(realToLocalUsesMap.keySet());
				Collections.sort(sortedUses, new UseComparator());
				for (Use use : sortedUses) {
					// determine type of use (c or p; intra or inter)
					PUse pUse = (use instanceof PUse)? (PUse)use : null;
					
					// determine D-U node order, taking use's src statement first, and tgt if puse
					CFGNode nDef = def.getN();
					CFGNode nUseSrc = use.getSrcNode();
					CFGNode nUseTgt = (pUse == null)? null : use.getBranch().getTgt();
					Branch brUse = use.getBranch();
					final boolean useReachesDef = Options.reachability()?
						((pUse == null)? ReachabilityAnalysis.reachesFromTop(nUseSrc, nDef, true) :
							ReachabilityAnalysis.reachesFromTop(nUseTgt, nDef, true)) :
						true; // assume use reaches def, if no reachability analysis available
					final boolean duInNodeOrder = (pUse == null)?
							orderGuaranteed(useReachesDef, nDef, nUseSrc) :
							orderGuaranteed(useReachesDef, nDef, brUse);
					
					// get # interproc defs for use
					final int numInterProcDefs = interProcDefsPerUse.get(use);
					
					// create and store DUA(s)
					// d-u event order is guaranteed if d-u node order is guaranteed, 
					// and if use doesn't have multiple inter-proc defs
					// (p-uses with > 1 defs will be considered later)
					final boolean duEventOrderGuaranteed = duInNodeOrder && numInterProcDefs <= 1;
					ArrayList<Use> localUses = realToLocalUsesMap.get(use);
					Use[] localUsesArr = new Use[localUses.size()];
					int uId = 0;
					for (Use u : localUses)
						localUsesArr[uId++] = u;
					DUA dua = new DUA(def, use, localUsesArr, duEventOrderGuaranteed);
					duaSet.addDUA(dua);
					
					// DEBUG
					if (verbose)
						System.out.print((duEventOrderGuaranteed? "" : "(P)") + use + "[");
					
					// increment c-use/p-use counter
					if (pUse == null)
						++cDuas;
					else
						++pDuas;
					
					// determine kills for this DUA, and update kill stats
					int killsForThisDua = 0;
					int possKillsForThisDua = 0;
					if (defKills != null) {
						for (Def kill : defKills) {
							if (kill != def) {
								// get dus for kill's method
								SootMethod mKill = ProgramFlowGraph.inst().getContainingMethod(kill.getN().getStmt());
								HashMap<Def, HashMap<Use, ArrayList<Use>>> killDUs = methodsToReachUseDefs.get(mKill).getDUs();
								Set<Use> usesForKill = null;
								if (killDUs.get(kill) != null)
									usesForKill = killDUs.get(kill).keySet();
								if (usesForKill != null && usesForKill.contains(use)) {  // kills this DUA
									CFGNode nKill = kill.getN();
									final boolean killAligned = orderGuaranteed(nDef, nKill, true) &&
										((pUse == null)? orderGuaranteed(nKill, nUseSrc, true) : orderGuaranteed(nKill, brUse, true));
									
									if (verbose)
										System.out.print((killAligned? "" : "(P)") + kill + ",");
									
									++killsForThisDua;
									// not only kill must be aligned, but use must not reach def
									if (!useReachesDef && killAligned)
										dua.addKillInOrder(kill);
									else {
										dua.addKillNotInOrder(kill);
										++possKillsForThisDua;
									}
								}
							}
						}
					}
					numKills += killsForThisDua;
					if (killsForThisDua < minKills)
						minKills = killsForThisDua;
					if (killsForThisDua > maxKills)
						maxKills = killsForThisDua;
					possibleKills += possKillsForThisDua;
					if (duEventOrderGuaranteed)
						subsDuasWithPossKill += possKillsForThisDua;
					
					if (verbose)
						System.out.print("],");
				}
				
				if (verbose)
					System.out.print("}, ");
			}
			
			// after regular DUAs, store same-BB dus
			duaSet.addSameBBDUs(ru.getSameBBDUs());
			
			if (verbose)
				System.out.println();
		}
		duaSet.updateInferrability();
		
		// inferrability stats
		final int numAllDuas = duaSet.getAllDUAs().size();
		int inferrDuas = 0, condInfDuas = 0, nonInfDuas = 0;
		int inferrDuasIntra = 0, condInfDuasIntra = 0, nonInfDuasIntra = 0;
		for (DUA dua : duaSet.getAllDUAs()) {
			SootMethod m = ProgramFlowGraph.inst().getContainingMethod(dua.getDef().getN().getStmt());
			ReachableUsesDefs ru = methodsToReachUseDefs.get(m);
			
			if (dua.isInferrableOrCondInf()) {
				if (dua.isDefinitelyInferrable()) {
					++inferrDuas;
				}
				else {
					++condInfDuas;
				}
			}
			else {
				++nonInfDuas;
			}
		}
		
		final int totalDuas = cDuas + pDuas;
		final int totalIntra = cIntraOnly + pIntraOnly;
		final int totalInter = cInterOnly + pInterOnly;
		
		System.out.println("INFERRABILITY: inf " + inferrDuas + ", cond " + condInfDuas 
				+ ", non-inf " + (numAllDuas - inferrDuas - condInfDuas));
		System.out.println("INF: intra " + inferrDuasIntra + ", inter " + (inferrDuas - inferrDuasIntra));
		System.out.println("COND-INF: intra " + condInfDuasIntra + ", inter " + (condInfDuas - condInfDuasIntra));
		assert nonInfDuasIntra == (totalIntra - inferrDuasIntra - condInfDuasIntra);
		System.out.println("NON-INF: intra " + nonInfDuasIntra + ", inter " + (nonInfDuas - nonInfDuasIntra));
		for (int i = 1; i < useCallDepthsCount.size(); ++i)
			System.out.print(i + ":" + useCallDepthsCount.get(i) + " ");
		System.out.println();
		if (!useCallDepthsCount.isEmpty() && useCallDepthsCount.get(0) != totalDuas - totalInter)
			System.out.println("NOTE -- DUAs at depth 0 are " + useCallDepthsCount.get(0) + ", but intra duas are " + (totalDuas - totalInter));
		
//		// TEST
//		reportPaths(methodsToReachUseDefs);
		
		System.out.println("DUA totals: " + totalDuas + "; c-duas " + cDuas + ", p-duas " + pDuas
				+ "; intra " + totalIntra + ", inter " + totalInter + ", both " + (totalDuas - totalIntra - totalInter));
		System.out.println("c-DUAs intra " + cIntraOnly + ", inter " + cInterOnly + ", both " + (cDuas - cIntraOnly - cInterOnly)
				+ "; p-DUAs intra " + pIntraOnly + ", inter " + pInterOnly + ", both " + (pDuas - pIntraOnly - pInterOnly));
	}
	
	private boolean orderGuaranteed(boolean secondReachesFirst, CFGNode n1, CFGNode n2) {
		if (!Options.dominance())
			return false; // worst-case assumption if no dom/pdom analysis available
		return !secondReachesFirst || DominatorRelation.inst().properlyDominates(n1.getStmt(), n2.getStmt())
				 || DominatorRelation.inst().properlyPostdominates(n2.getStmt(), n1.getStmt());
	}
	private boolean orderGuaranteed(CFGNode n1, CFGNode n2, boolean interproc) {
		return orderGuaranteed(Options.reachability()? ReachabilityAnalysis.reachesFromTop(n2, n1, interproc) : true,
				n1, n2);
	}
	private boolean orderGuaranteed(boolean secondReachesFirst, CFGNode n1, Branch br2) {
		return orderGuaranteed(secondReachesFirst, n1, br2.getSrc()) && orderGuaranteed(secondReachesFirst, n1, br2.getTgt());
	}
	private boolean orderGuaranteed(CFGNode n1, Branch br2, boolean interproc) {
		return orderGuaranteed(Options.reachability()? ReachabilityAnalysis.reachesFromTop(br2.getTgt(), n1, interproc) : true,
				n1, br2);
	}
	
	/** Rough and simple overapproximation: cross product of all defs against all uses of a field/arrayElem.
	 *  This is a static approximation: only one data-dependence per field/arrayElem def x use.
	 *  (At runtime, there might be multiple dependences of fields of different objects or different array elements.) */
	private void computeFieldArrayObjDUAs(List<SootMethod> allReachableMethods, Map<SootMethod, ReachableUsesDefs> methodsToReachUseDefs) {
		assert methodsToReachUseDefs.keySet().size() == allReachableMethods.size();
		assert methodsToReachUseDefs.keySet().containsAll(allReachableMethods);
		
		// collect all uses from cfgs
		List<Use> allFieldUses = new ArrayList<Use>();
		List<Use> allArrElemUses = new ArrayList<Use>();
		List<Use> allObjUses = new ArrayList<Use>();
		for (SootMethod m : allReachableMethods) {
			ReachableUsesDefs ru = methodsToReachUseDefs.get(m);
			allFieldUses.addAll(ru.getFieldUses());
			allArrElemUses.addAll(ru.getArrayElemUses());
			allObjUses.addAll(ru.getLibObjUses());
		}
		
		// sort all these uses
		List<Use> sortedFldUses = new ArrayList<Use>(allFieldUses);
		Collections.sort(sortedFldUses, new UseComparator());
		List<Use> sortedArrElemUses = new ArrayList<Use>(allArrElemUses);
		Collections.sort(sortedArrElemUses, new UseComparator());
		List<Use> sortedObjUses = new ArrayList<Use>(allObjUses);
		Collections.sort(sortedObjUses, new UseComparator());
		
		// match uses to defs; resolve fields for comparison (field refs are not necessarily "equal"!)
		int numFieldDUAs = 0;
		int numArrayElemDUAs = 0;
		int numObjectDUAs = 0;
		Map<SootField,List<DUA>> duasPerField = new HashMap<SootField, List<DUA>>();
		Map<Type,List<DUA>> duasPerArrElem = new HashMap<Type, List<DUA>>();
		Map<Pair<RefType,Boolean>,List<DUA>> duasPerObjType = new HashMap<Pair<RefType,Boolean>, List<DUA>>();
		for (SootMethod m : allReachableMethods) {
			ReachableUsesDefs ru = methodsToReachUseDefs.get(m);
			// 1. Match fields, but only if neither def or use is in catch block
			List<Def> sortedFldDefs = new ArrayList<Def>(ru.getFieldDefs());
			Collections.sort(sortedFldDefs, new DefComparator());
			for (Def fldDef : sortedFldDefs) {
				if (fldDef.isInCatchBlock())
					continue;
				
				SootField fldD = ((FieldRef)fldDef.getValue()).getField();
				// exclude DUAs for some special fields: class (ref to Class of object), and this$0 (link to container obj of nested class obj)
				if (fldD.getName().equals("class$0"))
					continue; // causes trouble when instrumenting <clinit>, where class$0 gets defined (i.e., Class loaded on demand)
				if (fldD.getName().equals("this$0"))
					continue; // causes trouble when instrumenting code that links nested class to outer object
				
				for (Use fldUse : sortedFldUses) {
					if (fldUse.isInCatchBlock())
						continue;
					
					SootField fldU = ((FieldRef)fldUse.getValue()).getField();
					if (fldD == fldU) {
						// create and store field DUA
						DUA fldDUA = new DUA(fldDef, fldUse, new Use[0], false);
						duaSet.addDUA(fldDUA);
						
						// associate DUA to field
						List<DUA> duasForFld = duasPerField.get(fldD);
						if (duasForFld == null) {
							duasForFld = new ArrayList<DUA>();
							duasPerField.put(fldD, duasForFld);
						}
						duasForFld.add(fldDUA);
						
						// update count of field DUAs
						++numFieldDUAs;
					}
				}
			}
			// 2. Match array elements, simply by element type
			List<Def> sortedArrDefs = new ArrayList<Def>(ru.getArrayElemDefs());
			Collections.sort(sortedArrDefs, new DefComparator());
			for (Def arrElemDef : sortedArrDefs) {
				if (arrElemDef.isInCatchBlock())
					continue;
				
				Variable varDef = arrElemDef.getVar();
				Type defArrElemType = ((ArrayRef)arrElemDef.getValue()).getType();
				for (Use arrElemUse : sortedArrElemUses) {
					if (arrElemUse.isInCatchBlock())
						continue;
					
//					Type useArrElemType = ((ArrayRef)arrElemUse.getValue()).getType();
					if (arrElemUse.getVar().mayEqual(varDef)) { //useArrElemType.equals(defArrElemType)) {
						// create and store array-elem DUA
						DUA arrElemDUA = new DUA(arrElemDef, arrElemUse, new Use[0], false);
						duaSet.addDUA(arrElemDUA);
						
						// associate DUA to array elem type
						List<DUA> duasForAE = duasPerArrElem.get(defArrElemType);
						if (duasForAE == null) {
							duasForAE = new ArrayList<DUA>();
							duasPerArrElem.put(defArrElemType, duasForAE);
						}
						duasForAE.add(arrElemDUA);
						
						// update count of array-elem DUAs
						++numArrayElemDUAs;
					}
				}
			}
			
			// 3. Match object use/defs, simply by element type
			if (Options.includeObjDUAs()) {
				List<Def> sortedObjDefs = new ArrayList<Def>(ru.getLibObjDefs());
				Collections.sort(sortedObjDefs, new DefComparator());
				for (Def objDef : sortedObjDefs) {
					if (objDef.isInCatchBlock())
						continue;
					
					// boolean in pair indicates whether it's instance obj (true) or class (false)
					List<Pair<RefType,Boolean>> defObjTypes = getObjTypes(objDef.getValue());
					for (Use objUse : sortedObjUses) {
						if (objUse.isInCatchBlock())
							continue;
						
						List<Pair<RefType,Boolean>> useObjTypes = getObjTypes(objUse.getValue());
						for (Pair<RefType,Boolean> useType : useObjTypes) {
							if (defObjTypes.contains(useType)) {
								// create and store obj DUA
								DUA objDUA = new DUA(objDef, objUse, new Use[0], false);
								duaSet.addDUA(objDUA);
								
								// associate one DUA to each matching object type
								List<Pair<RefType,Boolean>> matchingObjTypes = getMatchingTypes(defObjTypes, useObjTypes);
								for (Pair<RefType,Boolean> objType : matchingObjTypes) {
									List<DUA> duasForObjType = duasPerObjType.get(objType);
									if (duasForObjType == null) {
										duasForObjType = new ArrayList<DUA>();
										duasPerObjType.put(objType, duasForObjType);
									}
									duasForObjType.add(objDUA);
									
									// update count of array-elem DUAs
									++numObjectDUAs;
								}
							}
						}
					}
				}
			}
		}
		
		// add possible kills to field and array-elem DUAs
//		for (SootField fld : duasPerField.keySet()) {
//			List<DUA> duasForField = duasPerField.get(fld);
//			addAllOthersAsKills(duasForField);
//		}
//		for (Type t : duasPerArrElem.keySet()) {
//			List<DUA> duasForAE = duasPerArrElem.get(t);
//			addAllOthersAsKills(duasForAE);
//		}
		// TODO: kills for obj DUAs
		
		System.out.println("Total field DUAs: " + numFieldDUAs);
		System.out.println("Total array elem DUAs: " + numArrayElemDUAs);
		System.out.println("Total object DUAs: " + numObjectDUAs);
	}
	
	/** Return all possible types that can receive call (value invoke). Boolean is true if instance obj, false if static (class). */
	private List<Pair<RefType,Boolean>> getObjTypes(Value vInvoke) {
		List<Pair<RefType,Boolean>> objTypes = new ArrayList<Pair<RefType,Boolean>>();
		if (vInvoke instanceof InstanceInvokeExpr) {
			// receiver is instance: make list including referred class and all its subclasses
			// discard from list non-concrete classes
			RefType baseType = (RefType)((InstanceInvokeExpr)vInvoke).getBase().getType();
			SootClass baseCls = baseType.getSootClass();
			if (baseCls.isConcrete())
				objTypes.add(new Pair<RefType, Boolean>(baseType, true));
			
			for (SootClass subCls : dua.util.Util.getAllSubtypes(baseCls)) {
				if (subCls.isConcrete())
					objTypes.add(new Pair<RefType, Boolean>(subCls.getType(), true));
			}
		}
		else {
			// receiver is class: make list with receiver class only
			RefType recType = ((StaticInvokeExpr)vInvoke).getMethod().getDeclaringClass().getType();
			objTypes.add(new Pair<RefType, Boolean>(recType, false));
		}
		
		return objTypes;
	}
	
	/** Just return the intersection of both lists */
	private List<Pair<RefType,Boolean>> getMatchingTypes(
			List<Pair<RefType,Boolean>> types1, List<Pair<RefType,Boolean>> types2) {
		List<Pair<RefType,Boolean>> matchingTypes = new ArrayList<Pair<RefType,Boolean>>();
		for (Pair<RefType,Boolean> objType : types1) {
			if (types2.contains(objType))
				matchingTypes.add(objType);
		}
		
		return matchingTypes;
	}
	
	private void addAllOthersAsKills(List<DUA> allDuas) {
		// for each DUA, add defs of all other DUAs as kills
		for (DUA dua : allDuas) {
			List<Def> kills = new ArrayList<Def>();
			for (DUA duaOther : allDuas)
				if (duaOther != dua)
					kills.add(duaOther.getDef());
			
			dua.addKillsNotInOrder(kills);
		}
	}
	
}
