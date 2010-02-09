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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import soot.ArrayType;
import soot.Body;
import soot.IntType;
import soot.Local;
import soot.Modifier;
import soot.PatchingChain;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.SootMethodRef;
import soot.Type;
import soot.jimple.AssignStmt;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.Jimple;
import soot.jimple.NewArrayExpr;
import soot.jimple.Stmt;
import soot.jimple.internal.JInterfaceInvokeExpr;
import soot.jimple.internal.JSpecialInvokeExpr;
import soot.jimple.internal.JStaticInvokeExpr;
import soot.jimple.internal.JVirtualInvokeExpr;
import dua.Options;
import dua.global.ProgramFlowGraph;
import dua.method.AbstractEdge;
import dua.method.CFG;
import dua.method.CallEdge;
import dua.method.CallSite;
import dua.method.EPPAnalysis;
import dua.method.Edge;
import dua.method.MethodTag;
import dua.method.CFG.CFGNode;
import dua.unit.StmtTag;
import dua.util.Pair;

public class EPPInstrumenter {
	
	private static class BaseCalleeNotFound extends Exception { }
	
	/** Maps non-app classes to intermediate classes created to allow instrumentation of calls to their methods */
	private Map<SootClass,SootClass> intermediateClasses = new HashMap<SootClass, SootClass>();
	private Map<CallSite, SootMethod> resolvedMethods = new HashMap<CallSite, SootMethod>();
	private NewArrayExpr dummyArrInitExpr = null;
	
	/** Applies BL96 "efficient path profiling" algorithm to each cfg */
	public void instrument(Map<CFG, EPPAnalysis> cfgEPPAnalyses) {
		final int depth = Options.eppDepth();
		
		/// TODO fix !!!
		///      instrument for ALL entry methods, not just the first one
		List<CFG> cfgs = ProgramFlowGraph.inst().getCFGs();
		SootMethod entryMethod = ProgramFlowGraph.inst().getEntryMethods().get(0);
		SootClass entryClass = entryMethod.getDeclaringClass();
		
		// DEBUG: generate list of paths
//		outputEnumeratePaths(cfgs, depth, cfgEPPAnalyses);
		
		// Insert needed r and cov-arr formal parameters.
		Map<SootMethod,List<Pair<Local, Local>>> eppLocalsPerMethod = addNeededFormalParams(ProgramFlowGraph.inst().getReachableAppMethods(), depth);
		
		// Instrument CFG with min-cost increment placements found in DAG
		//    For now, at least, we use the same path depth for each method
		for (CFG cfg : cfgs)
			instrumentDAG(entryClass, cfg, cfgEPPAnalyses.get(cfg), eppLocalsPerMethod, depth);
		
		// Instrument subject's entry method to invoke path reporter
		final String pathReporterClsName = "profile.PathReporter";
		final String reporterMethodSig = "void reportPaths(java.lang.Class)";
		Local lPathReporter = UtilInstrum.insertReporterInstantiateCode(entryMethod, pathReporterClsName);
		SootClass pathReporterClass = Scene.v().getSootClass(pathReporterClsName);
		UtilInstrum.insertClassReport(entryMethod, entryMethod.getDeclaringClass(), lPathReporter, pathReporterClass.getMethod(reporterMethodSig));
	}
	
	private Map<SootMethod,List<Pair<Local, Local>>> addNeededFormalParams(Collection<SootMethod> mtds, int depth) {
		// 1. Resolve methods before changing their signatures
		//    Build map cs->baseTarget before we modify targets and alter behavior of mref.resolve()
		for (SootMethod m : mtds) {
			MethodTag mTag = (MethodTag) m.getTag(MethodTag.TAG_NAME);
			for (CallSite cs : mTag.getCallSites()) {
				if (!cs.hasAppCallees())
					continue;
				InvokeExpr invExpr = cs.getLoc().getStmt().getInvokeExpr();
				SootMethodRef mRef = invExpr.getMethodRef();
				SootMethod mResolved = mRef.resolve();
				assert mResolved != null;
				resolvedMethods.put(cs, mResolved);
			}
		}
		
		// 2. Compute caller distances to each method: associate to each method its callers (up to given depth), and the call distances from those callers
		//    A caller has a set of distances, since call paths might have different lenghts
		//    This map will include keys that are abstract call-site target methods 
		HashMap<SootMethod, HashMap<SootMethod, Set<Integer>>> callerDistances = getAllCallerDistances(mtds, depth);
		
		// 3. Add r and cov-array formal params to methods in list, plus call-site base targets, for every caller distance
		//    TODO: in the future, see how to handle separate r's and arr's when there is a variable depth for each caller
		return createExtraFormalParams(mtds, callerDistances);
	}
	
	/**
	 * Associates to each method its callers (up to given depth), and the call distances from those callers.
	 * A caller has not a single distance, but a set of distances, since call paths might have different lenghts.
	 * Recursive callers are allowed in search (within call distance, of course).
	 * Keys in returned map INCLUDE call-site targets that are library methods (concrete or abstract), or abstract app methods.
	 * Uses resolvedMethods field to ensure that bases and overridden methods share the same parameter sets.
	 * @return Map method->(caller->dist_set), for all provided methods plus call base target methods (library and/or abstract)
	 */
	private HashMap<SootMethod, HashMap<SootMethod, Set<Integer>>> getAllCallerDistances(Collection<SootMethod> methods, int maxDepth) {
		HashMap<SootMethod, HashMap<SootMethod, Set<Integer>>> allCallerDistances = new HashMap<SootMethod, HashMap<SootMethod, Set<Integer>>>();
		
		// 1. Propagate each concrete app method backwards to callers
		for (SootMethod m : methods) {
			HashMap<SootMethod, Set<Integer>> callerDistances = new HashMap<SootMethod, Set<Integer>>();
			allCallerDistances.put(m, callerDistances);
			
			findCallersBack(m, callerDistances, 1, maxDepth);
		}
		
		// 2. Find distances for library or abstract resolved methods
		
		// 2.1 Associate each base target with all its overrider methods, visiting all call sites pointing to that tgt
		Map<SootMethod, Set<SootMethod>> baseToOverriders = new HashMap<SootMethod, Set<SootMethod>>();
		Map<SootMethod, SootMethod> ovToBase = new HashMap<SootMethod, SootMethod>(); // DEBUG (see below)
		for (CallSite cs : resolvedMethods.keySet()) {
			// determine if cs base target qualifies as library or abstract
			SootMethod mBaseTarget = resolvedMethods.get(cs);
			if (cs.getAppCallees().contains(mBaseTarget))
				continue; // only look for base targets that are not concrete callees
			
			// get/create set of overrider methods for given call-site base target
			Set<SootMethod> overriders = baseToOverriders.get(mBaseTarget);
			if (overriders == null) {
				overriders = new HashSet<SootMethod>();
				baseToOverriders.put(mBaseTarget, overriders);
			}
			
			// associate callees in this cs to base tgt
			overriders.addAll(cs.getAppCallees());
			
			// DEBUG: each overrider method should override only one base tgt method
			for (SootMethod mOv : cs.getAppCallees()) {
				SootMethod mOldBase = ovToBase.put(mOv, mBaseTarget);
				assert mOldBase == null || mOldBase == mBaseTarget;
			}
		}
		
		// 2.2 Make required distances consistent for each base and its overriders
		for (SootMethod mBaseTarget : baseToOverriders.keySet()) {
			// merge method->dist maps of all overriders
			HashMap<SootMethod, Set<Integer>> mergedDists = new HashMap<SootMethod, Set<Integer>>();
			for (SootMethod mOverrider : baseToOverriders.get(mBaseTarget)) {
				HashMap<SootMethod, Set<Integer>> ovDists = allCallerDistances.get(mOverrider);
				for (SootMethod mPred : ovDists.keySet()) {
					// get/create dist set for caller (predecessor in call graph) in merged map
					Set<Integer> callerDists = mergedDists.get(mPred);
					if (callerDists == null) {
						callerDists = new HashSet<Integer>();
						mergedDists.put(mPred, callerDists);
					}
					// add, to the merged set, the set of distances for caller associated to overrider
					callerDists.addAll(ovDists.get(mPred));
				}
			}
			
			// assign merged map to base tgt and all overriders in main distances map
			allCallerDistances.put(mBaseTarget, mergedDists);
			for (SootMethod mOverrider : baseToOverriders.get(mBaseTarget))
				allCallerDistances.put(mOverrider, mergedDists);
		}
		
		return allCallerDistances;
	}
	
	/** Note: in the future, for variable-distance per method, we will need to check if each individual caller is within calling range of tgt method */
	private void findCallersBack(SootMethod m, HashMap<SootMethod, Set<Integer>> callerDistances, int depth, int maxDepth) {
		if (depth > maxDepth)
			return;
		
		// build collection of caller methods from collection of caller sites, so we visit each method back once
		MethodTag mTag = (MethodTag) m.getTag(MethodTag.TAG_NAME);
		HashSet<SootMethod> callerMethods = new HashSet<SootMethod>();
		for (CallSite csCaller : mTag.getCallerSites()) {
//			// resolve directly referred method in call
//			SootMethodRef methodRef = csCaller.getLoc().getStmt().getInvokeExpr().getMethodRef();
//			SootMethod mBaseCallee;
//			try { mBaseCallee = methodRef.resolve(); }
//			catch (RuntimeException e) { mBaseCallee = null; }
//			assert mBaseCallee != null;
//			if (isSuitable(mBaseCallee)) // consider call site for distances only if "suitable"
			callerMethods.add(csCaller.getLoc().getMethod());
		}
		
		// TODO: in the future, for variable-distance per method, we will need to check if each individual caller is within calling range of tgt method
		// add each caller and distance to resulting map, recursing on caller along the way
		for (SootMethod mCaller : callerMethods) {
			// get/create distances set for caller
			Set<Integer> dists = callerDistances.get(mCaller);
			if (dists == null) {
				dists = new HashSet<Integer>();
				callerDistances.put(mCaller, dists);
			}
			// add distance to set for caller
			dists.add(depth);
			// move into caller
			findCallersBack(mCaller, callerDistances, depth + 1, maxDepth);
		}
	}
	
//	/**
//	 * Right now, a call site is "suitable" if it refers to app method (direct call, or overriden/abstract base).
//	 * This is only a convenience, for now, since if target method is non-app, it requires more complex instrumentation.
//	 */
//	private static boolean isSuitable(SootMethod m) {
//		return Util.getReachableAppMethods().contains(m);
//	}
	
	private final static String REG_PREFIX = "<rd";
	private final static String REG_SUFFIX = ">";
	private final static String PCOVARR_PREFIX = "<pcovarr";
	private final static String PCOVARR_SUFFIX = ">";
	
	private static String getRegLocalName(int rIdx) {
		return REG_PREFIX + rIdx + REG_SUFFIX;
	}
	private static String getRegCopyLocalName(Local r) {
		return r + "_copy";
	}
	private static String getCovArrLocalName(int rIdx) {
		return PCOVARR_PREFIX + rIdx + PCOVARR_SUFFIX;
	}
	private static String getD0CovArrLocalName() {
		return PCOVARR_PREFIX + PCOVARR_SUFFIX;
	}
	
	/** Creates formal parameters and corresponding locals for interproc BL r's and arr's, for all methods */
	private Map<SootMethod,List<Pair<Local, Local>>> createExtraFormalParams(Collection<SootMethod> mtds, HashMap<SootMethod, HashMap<SootMethod, Set<Integer>>> callerDistances) {
		// build map for method, where value is list of locals (for formal params), one reg-covarr pair per depth level (null if depth is skipped)
		Map<SootMethod,List<Pair<Local, Local>>> eppLocalsPerMethod = new HashMap<SootMethod, List<Pair<Local,Local>>>();
		for (SootMethod m : mtds) {
			// determine which depths are needed as formal params in this method
			Map<SootMethod, Set<Integer>> callerDistancesForMethod = callerDistances.get(m);
			BitSet distsBS = new BitSet();
			for (SootMethod mCaller : callerDistancesForMethod.keySet()) {
				Set<Integer> dists = callerDistancesForMethod.get(mCaller);
				for (Integer d : dists)
					distsBS.set(d);
			}
			
			// add formal params to method, only for needed depths
			// create new formal param list, starting with original params
			ArrayList<Type> newParamTypeList = new ArrayList<Type>(m.getParameterTypes());
			// create r-arr locals list, where null indicates that a depth is not needed
			List<Pair<Local,Local>> localParams = new ArrayList<Pair<Local,Local>>();
			
			// get position of last id stmt, or null if there are no id stmts (i.e., formal param init)
			Body b = m.retrieveActiveBody();
			PatchingChain pchain = b.getUnits();
			Stmt sFirstNonIdStmt = UtilInstrum.getFirstNonIdStmt(pchain);
			
			// add r and cov-arr for each unique non-zero distance from callers
			for (int d = 1; d < distsBS.length(); ++d) {
				if (distsBS.get(d)) {
					// add types to param list
					final int paramNumber = newParamTypeList.size(); // index of next reg parameter to add
					newParamTypeList.add(IntType.v());
					newParamTypeList.add(ArrayType.v(IntType.v(), 1));
					// create reg and array locals for params
					Local lReg = Jimple.v().newLocal(getRegLocalName(d), IntType.v());
					b.getLocals().add(lReg);
					Local lArr = Jimple.v().newLocal(getCovArrLocalName(d), ArrayType.v(IntType.v(), 1));
					b.getLocals().add(lArr);
					Pair<Local,Local> rArrPair = new Pair<Local, Local>(lReg, lArr);
					localParams.add(rArrPair);
					// add id stmts always at end of id stmt list (beginning of method)
					Stmt sFormalRegInit = Jimple.v().newIdentityStmt(lReg, Jimple.v().newParameterRef(IntType.v(), paramNumber));
					pchain.insertBeforeNoRedirect(sFormalRegInit, sFirstNonIdStmt);
					Stmt sFormalArrInit = Jimple.v().newIdentityStmt(lArr, Jimple.v().newParameterRef(IntType.v(), paramNumber + 1));
					pchain.insertAfter(sFormalArrInit, sFormalRegInit);
				}
				else {
					localParams.add(null); // special value indicating that this depth is not needed in this method
				}
			}
			
			// set new param list in method
			m.setParameterTypes(newParamTypeList);
			
			// associate to method list of locals corresponding
			eppLocalsPerMethod.put(m, localParams);
		}
		
		updateCSBaseTargets(mtds, resolvedMethods);
		
		return eppLocalsPerMethod;
	}
	
	/**
	 * Instruments call-target methods when such methods are non-app and/or abstract.
	 * When the method is non-app, it creates a new intermediate class where the new target is placed.
	 */
	private void updateCSBaseTargets(Collection<SootMethod> instrMtds, Map<CallSite, SootMethod> resolvedMethods) {
		// 1. Build list of call sites whose referenced methods are not in instrumented methods list
		//    A method in this category is the target of a virtual call; if it is not in provided list, it is non-app and/or abstract
		HashSet<CallSite> nonInstrTargetCSs = new HashSet<CallSite>();
		for (SootMethod m : instrMtds) {
			MethodTag mTag = (MethodTag) m.getTag(MethodTag.TAG_NAME);
			for (CallSite cs : mTag.getCallSites()) {
				assert cs.hasAppCallees();
				SootMethod mCalleeBase = resolvedMethods.get(cs);
				assert mCalleeBase != null;
				if (!instrMtds.contains(mCalleeBase))
					nonInstrTargetCSs.add(cs);
			}
		}
		
		// 2. For each target call site, update base target method (directly or through intermediate class) to include extra EPP params
		HashSet<SootMethod> instrumentedBaseMethods = new HashSet<SootMethod>(); // keeps track of methods already instrumented (directly or through intermediate class), to avoid repetition
		for (CallSite cs : nonInstrTargetCSs) {
			// 2.1 Get method that is base target of call site
			SootMethod mCalleeBase = resolvedMethods.get(cs);
			assert mCalleeBase != null;
			
			// 2.2 Avoid processing same lib target method more than once (because more than one call site might point to it)
			if (instrumentedBaseMethods.contains(mCalleeBase))
				continue; // don't repeat
			
			// 2.3 Determine param list for update procedure depends on whether it's an app method (directly modifiable) or not (need new intermediate class)
			List targetParamTypeList = cs.getAppCallees().get(0).getParameterTypes(); // get param list from a sample real target, which includes EPP params
			// CASE 2.3.1 : app method (abstract)
			if (ProgramFlowGraph.inst().getAppClasses().contains(mCalleeBase.getDeclaringClass())) {  // abstract application method
				// Update param list for abstract app method, using list of a sample real target of call site
				assert mCalleeBase.isAbstract();
				mCalleeBase.setParameterTypes(targetParamTypeList);
			}
			// CASE 2.3.2 : lib method (concrete or abstract); note that we can't change a non-app class and method, so we use an intermediate class
			else {
				// 2.3.2.1 Get/create INTERMEDIATE class for lib method's class. Intermediate class extends lib method (or implements it, if lib class is an interface)
				SootClass clsNonApp = mCalleeBase.getDeclaringClass();
				final String intermClsName = "instrum-." + clsNonApp.getName() + "-";
				SootClass clsInterm = intermediateClasses.get(clsNonApp);
				if (clsInterm == null) {
					// 2.3.2.1.1 Create new "intermediate" class that extends/implements original class/interface
					clsInterm = new SootClass(intermClsName, Modifier.PUBLIC | Modifier.ABSTRACT | (clsNonApp.isInterface()? Modifier.INTERFACE : 0));
					if (clsNonApp.isInterface())
						clsInterm.addInterface(clsNonApp);
					else {
						clsInterm.setSuperclass(clsNonApp);
						UtilInstrum.duplicateSuperCtors(clsInterm);
					}
					intermediateClasses.put(clsNonApp, clsInterm);
					Scene.v().getApplicationClasses().add(clsInterm);
				}
				
				// 2.3.2.2 Change base class/interface of concrete app targets that directly extend/implement lib cls, to the new INTERMEDIATE class/interface
				for (SootMethod mCallee : cs.getAppCallees()) {
					SootClass clsCallee = mCallee.getDeclaringClass();
					if (clsCallee.getSuperclass() == clsNonApp) {
						if (clsInterm.isInterface()) {
							clsCallee.removeInterface(clsNonApp);
							clsCallee.addInterface(clsInterm);
						}
						else {
							clsCallee.setSuperclass(clsInterm);
							UtilInstrum.redirectCtors(clsCallee, clsNonApp);
						}
					}
				}
				
				// 2.3.2.3 Add to INTERMEDIATE class a method with extra EPP formal params. This method will be called instead of original method in non-app class
				Type retType = mCalleeBase.getReturnType();
				SootMethod mNew = new SootMethod(mCalleeBase.getName(), targetParamTypeList, retType, mCalleeBase.getModifiers() | Modifier.ABSTRACT, mCalleeBase.getExceptions());
				clsInterm.addMethod(mNew);
			}
			
			instrumentedBaseMethods.add(mCalleeBase); // keep track of updated base target method, to avoid repetition
		}
	}

	/**
	 * Instruments a CFG's DAG with computed increments.
	 * @param maxDepth Fixed depth (used for now, until we switch to variable depths), to guide r and arr param passing to callees.
	 */
	private void instrumentDAG(SootClass entryClass, CFG cfg, EPPAnalysis eppAnalysis, Map<SootMethod,List<Pair<Local, Local>>> eppLocalsPerMethod, int maxDepth) {
		// 1. Retrieve edge and EPP data for this cfg
		HashMap<CFGNode, ArrayList<Edge>> inEdges = eppAnalysis.getInEdges();
		ArrayList<Edge> regularEdges = eppAnalysis.getRegularEdges();
		HashSet<Edge> backedges = eppAnalysis.getBackedges();
		HashSet<Edge> dummyEdgesFromEntry = eppAnalysis.getDummyEdgesFromEntry();
		HashSet<Edge> dummyEdgesToExit = eppAnalysis.getDummyEdgesToExit();
		ArrayList<HashMap<AbstractEdge, Integer>> optEdgeValues = eppAnalysis.getOptEdgeValues();
		final int totalAcyclicPaths = eppAnalysis.getNumPaths(maxDepth).get(cfg.ENTRY);
		
		// 2. Create cov array for paths that begin at this method; init array at clinit of entry class
		SootMethod m = cfg.getMethod();
		Type arrType = ArrayType.v(IntType.v(), 1);
		SootField pathCountArrayField = new SootField("<p_" + ProgramFlowGraph.inst().getMethodIdx(m) + ">", arrType, Modifier.PUBLIC|Modifier.STATIC);
		entryClass.addField(pathCountArrayField);
		
		SootMethod clinit = UtilInstrum.getCreateClsInit(entryClass);
		PatchingChain clinitPchain = clinit.retrieveActiveBody().getUnits();
		Local arrClinitLocal = getCreateInitArrLocal(clinit, null); // null tells method not to init array var, just create it
		List initCode = new ArrayList();
		Stmt sNewArr = Jimple.v().newAssignStmt(arrClinitLocal, 
				Jimple.v().newNewArrayExpr(IntType.v(), IntConstant.v(totalAcyclicPaths)));
		initCode.add(sNewArr);
		Stmt sPutArrInField = Jimple.v().newAssignStmt(Jimple.v().newStaticFieldRef(pathCountArrayField.makeRef()), arrClinitLocal);
		initCode.add(sPutArrInField);
		InstrumManager.v().insertRightBeforeNoRedirect(clinitPchain, initCode, (Stmt) clinitPchain.getLast());
		
		// 3. Create path id register local r at CFG's method, for paths beginning here, and init r at method's entry
		Local r = Jimple.v().newLocal("<r>", IntType.v());
		Body b = m.retrieveActiveBody();
		b.getLocals().add(r);
		//    init r at method's entry (after initial id stmts), using inc for EN->firstnode edge (or 0 if inc not found)
		PatchingChain pchain = b.getUnits();
		Stmt sFirstNonId = UtilInstrum.getFirstNonIdStmt(pchain);
		List entryCode = new ArrayList();
		Local localCounter = Jimple.v().newLocal("<cnt>", IntType.v());
		b.getLocals().add(localCounter);
		Integer valEdgeEntry = optEdgeValues.get(0).get(new Edge(cfg.ENTRY, cfg.ENTRY.getSuccs().get(0))); 
		final int entryInitialInc = (valEdgeEntry == null)? 0 : valEdgeEntry;
		Stmt sInitReg = Jimple.v().newAssignStmt(r, IntConstant.v(entryInitialInc));
		entryCode.add(sInitReg);
		InstrumManager.v().insertRightBeforeNoRedirect(pchain, entryCode, sFirstNonId);
		
		// 4. At each backedge and at EXIT, update cov-arrays indexed r's; at backedges, also reset r's
		HashMap<CFGNode, ArrayList<Edge>> nodeOutBackedges = new HashMap<CFGNode, ArrayList<Edge>>(); // associate src node to backedges, along the way
		Local localArray = getCreateInitArrLocal(m, pathCountArrayField);
		List<Pair<Local, Local>> localEPPparams = eppLocalsPerMethod.get(m);
		for (Edge be : backedges) {
			// update cov-arrays; also, reset r's to dummy ENTRY->be.tgt's inc value (0 if there is no inc for that edge)
			// for each depth:
			//   cov_arr[r]++ 
			//   r = ... 
			List backedgeCode = createCovArrayUpdateCode(b, pathCountArrayField, r, localArray, localEPPparams);
			int[] beResetVals = new int[maxDepth + 1];
			Edge entryToBETgt = new Edge(cfg.ENTRY, be.getTgt()); // just to look up for dummy entry->loophead edge's inc value
			for (int d = 0; d <= maxDepth; ++d) {
				Integer valEdgeDummyFromEntry = optEdgeValues.get(d).get(entryToBETgt); 
				beResetVals[d] = (valEdgeDummyFromEntry == null)? 0 : valEdgeDummyFromEntry.intValue();
			}
			backedgeCode.addAll(createRegResetCode(b, r, localEPPparams, beResetVals)); // reset r's
			InstrumManager.v().insertProbeAt(be, backedgeCode);
			
			// along the way, collect backedges emanating from each node
			ArrayList<Edge> besForNode = nodeOutBackedges.get(be.getSrc());
			if (besForNode == null)
				nodeOutBackedges.put(be.getSrc(), besForNode = new ArrayList<Edge>());
			besForNode.add(be);
		}
		//    update all cov-arrays at virtual exit (i.e., at all return->EXIT edges)
		//    note: probe at return->EXIT differs from backedge probes in that "r" is not reinitialized
		for (Edge x : inEdges.get(cfg.EXIT)) {
			if (!dummyEdgesToExit.contains(x)) {
				// update cov-arrays, indexed by r's
				List exitCode = createCovArrayUpdateCode(b, pathCountArrayField, r, localArray, localEPPparams);
				InstrumManager.v().insertProbeAt(x, exitCode);
			}
		}
		
		// 5. Apply r's increments at each edge selected for instrumentation (i.e., edge with non-zero increment)
		Integer eVal;
		for (AbstractEdge e : regularEdges) {
			if (!backedges.contains(e) && e.getSrc() != null && (eVal = optEdgeValues.get(0).get(e)) != null && eVal != 0) {
				// build list of values per received-params depth, for this edge
				// increments are ordered by depth starting from this method, but depth incs array is in order of parameter (lowest to highest *incoming* depths),
				//   so we invert order to get increment for *remaining* distance from lower to higher
				int[] incomingDepthIncs = new int[optEdgeValues.size()];
				for (int d = 0; d < incomingDepthIncs.length; ++d)
					incomingDepthIncs[d] = optEdgeValues.get(incomingDepthIncs.length - d - 1).get(e);
				
				if (dummyEdgesFromEntry.contains(e)) {
					// special case 1: edge corresponds to reinitializing r's at each backedge arriving at tgt, with inc values of this edge
					//                 just after cov-array's updates in already existing backedge probes
					// we do nothing because r's were already reinited to corresponding inc value in previous step
				}
				else if (dummyEdgesToExit.contains(e)) {
					// special case 2: edge corresponds to incrementing r's at each backedge emanating from src,
					//                 just before cov-array's updates and r's resets in already existing backedge probes
					for (Edge beFromEdgeSrc : nodeOutBackedges.get(e.getSrc())) {
						List backedgeRegIncCode = createRegIncCode(b, r, localEPPparams, incomingDepthIncs);
						InstrumManager.v().insertProbeAt(beFromEdgeSrc, backedgeRegIncCode); // reg increments are placed before cov-arr updates
					}
				}
				else if (e.getSrc() == null) {  // EXIT->ENTRY special edge
					// special case 3: increment r's at virtual EXIT node (i.e., at each edge to EXIT)
					for (Edge eToExit : inEdges.get(null)) {
						List exitCode = createRegIncCode(b, r, localEPPparams, incomingDepthIncs);
						InstrumManager.v().insertProbeAt(eToExit, exitCode); // reg increments are placed before cov-arr updates
					}
				}
				else {
					// regular edge from DAG: increment r's at edge
					List edgeCode = createRegIncCode(b, r, localEPPparams, incomingDepthIncs);
					InstrumManager.v().insertProbeAt((Edge)e, edgeCode);
				}
			}
		}
		
		// 6. Instrument all calls to include r and cov-arr params
		if (maxDepth > 0 && !eppAnalysis.getCallEdges().isEmpty()) {
			//    instrument calls
			for (CallEdge ce : eppAnalysis.getCallEdges()) {
				assert optEdgeValues.get(0).get(ce) == null;
				CFGNode nCall = ce.getSrc();
				Stmt sCall = nCall.getStmt();
				
				// build list of values per *remaining* depth, for this call edge
				// note that depth 0 is meaningless for a call edge
				assert optEdgeValues.size() == maxDepth + 1;
				int[] remainingDepthIncs = new int[optEdgeValues.size()];
				for (int d = 1; d < remainingDepthIncs.length; ++d) {
					remainingDepthIncs[d] = optEdgeValues.get(d).get(ce);
					assert remainingDepthIncs[d] != 0; 
				}
				
				// update call with additional arguments (r's and cov-arrays)
				StmtTag sCallTag = (StmtTag) sCall.getTag(StmtTag.TAG_NAME);
				CallSite cs = sCallTag.getAppCallSite();
				SootMethod mCalleeSample = cs.getAppCallees().get(0);
				List<Pair<Local, Local>> localEPPparamsCallee = eppLocalsPerMethod.get(mCalleeSample);
				try {
					instrumentCall(b, r, localArray, nCall, eppAnalysis, localEPPparams, localEPPparamsCallee, remainingDepthIncs, mCalleeSample.getParameterTypes(), entryClass);
				}
				catch (BaseCalleeNotFound e) { assert false; }
			}
			//    instrument calls in catch block, to prevent class loading errors
			for (CFGNode n : cfg.getNodes()) {
				if (n.isInCatchBlock()) {
					// update call with additional arguments (r's and cov-arrays), but no increments (by passing null inc array)
					Stmt sCall = n.getStmt();
					StmtTag sCallTag = (StmtTag) sCall.getTag(StmtTag.TAG_NAME);
					CallSite cs = sCallTag.getAppCallSite();
					if (cs != null) {
						SootMethod mCalleeSample = cs.getAppCallees().get(0);
						List<Pair<Local, Local>> localEPPparamsCallee = eppLocalsPerMethod.get(mCalleeSample);
						try {
							// We want call from catch block to match target signature, so loader won't fail, but we don't want r's and cov-arr's altered
							// 'null' local params caller indicate that we only want dummy params passed at call
							// 'null' "remaining-depth" incs array indicates that we don't want increment code added before call
							instrumentCall(b, r, localArray, n, eppAnalysis, null, localEPPparamsCallee, null, mCalleeSample.getParameterTypes(), entryClass);
						}
						catch (BaseCalleeNotFound e) {} // let it continue; call didn't directly target an app method
					}
				}
			}
		}
	}
	
	/**
	 * 'null' for localEPPparamsCaller indicate that we only want dummy params passed at call
	 * 'null' for "remaining-depth" incs array indicates that we don't want increment code added before call
	 * @param localEPPparamsCaller null if only dummy params passed, otherwise it's list of locals linked to EPP parameters in caller; organized in pairs (r,cov-arr), from d=1, in ascending d order; null pair indicates not needed d (i.e., use dummy params)
	 * @param localEPPparamsCallee Locals linked to EPP parameters in callee; organized in pairs (r,cov-arr), from d1 in order; null pair indicates not needed d
	 * @param remainingDepthIncs Increments for call edge, ordered from lowest REMAINING distance (last formal param) to highest (r-arr starting at this method)
	 */
	private void instrumentCall(Body bCaller, Local r, Local covArr, CFGNode nCall, EPPAnalysis eppAnalysis, List<Pair<Local, Local>> localEPPparamsCaller, 
			List<Pair<Local, Local>> localEPPparamsCallee, int[] remainingDepthIncs, List calleeParamTypes, SootClass entryClass)
		throws BaseCalleeNotFound
	{
		// start building new call-args list with original args
		Stmt sCall = nCall.getStmt();
		InvokeExpr origInvExpr = sCall.getInvokeExpr();
		List callArgs = new ArrayList(origInvExpr.getArgs());
		
		// add needed reg and arr parameters for callee(s), creating copy regs and adding increments to them
		// -- dummy arr param local on demand (for dummy reg, we use a 0)
		// complete call-args list as we find reg and arr params;
		List callIncCode = new ArrayList(); // reg copy-increment code, to insert before call
		
		// NOTE: d0 arguments should always be needed by callee, if this method was invoked (call required to be instrumented)
		assert !localEPPparamsCallee.isEmpty() && localEPPparamsCallee.get(0) != null;
		// start with r-arr locals for paths starting at this method (i.e., d = 0), which are used for d1 at callee
		// -- d = 0 implies that we start with "remaining-depth" sizeof(remainingDepthIncs)
		int dLeftAtCallee = -1;
		Local lRegCopy = getCreateRegCopyLocal(bCaller, r);
		if (remainingDepthIncs != null) {
			dLeftAtCallee = remainingDepthIncs.length - 1; // depth left at caller, to access correct increment
			Stmt sCopyIncReg = Jimple.v().newAssignStmt(lRegCopy, Jimple.v().newAddExpr(r, IntConstant.v(remainingDepthIncs[dLeftAtCallee])));
			callIncCode.add(sCopyIncReg);
		}
		
		// add args for "remaining-depth" 0
		if (localEPPparamsCaller == null) {
			final int numPaths = eppAnalysis.getCallNumPaths(nCall, -1); // -1 means "find max depth for each callee"
			Local lDummyCovArrCaller = getCreateInitDummyArrLocal(bCaller, numPaths, entryClass);
			callArgs.add(IntConstant.v(0));
			callArgs.add(lDummyCovArrCaller);
		}
		else {
			callArgs.add(lRegCopy);
			callArgs.add(covArr);
		}
		
		--dLeftAtCallee;
		
		// continue passing r-arr params from caller to callee, for remaining depths (if any) needed by callee
		Iterator<Pair<Local,Local>> itCalleeParam = localEPPparamsCallee.iterator();
		itCalleeParam.next(); // skip first r-arr param pair, which were taken care of above
		Iterator<Pair<Local,Local>> itCallerParam = (localEPPparamsCaller == null)? null : localEPPparamsCaller.iterator(); // start from first r-arr pair (d=1) received by caller
		while (itCalleeParam.hasNext()) {
			Pair<Local,Local> depthLocalsCallee = itCalleeParam.next(); // just to see if they are needed or not (i.e., non-null or null)
			Pair<Local,Local> depthLocalsCaller = (itCallerParam != null && itCallerParam.hasNext())? itCallerParam.next() : null;
			if (depthLocalsCallee != null) {  // r-arr for this depth are needed by callee
				if (depthLocalsCaller != null) {  // r-arr for required depth is available in caller
					// create copy-inc reg and add copy-inc code
					lRegCopy = getCreateRegCopyLocal(bCaller, depthLocalsCaller.first());
					if (remainingDepthIncs != null) {
						Stmt sCopyIncReg = Jimple.v().newAssignStmt(lRegCopy, Jimple.v().newAddExpr(depthLocalsCaller.first(), IntConstant.v(remainingDepthIncs[dLeftAtCallee])));
						callIncCode.add(sCopyIncReg);
					}
					// add args for this "remaining-depth" level
					callArgs.add(lRegCopy);
					callArgs.add(depthLocalsCaller.second());
				}
				else {  // required r-arr not available in caller; use DUMMY parameters
					final int numPaths = eppAnalysis.getCallNumPaths(nCall, dLeftAtCallee + 1);
					Local lDummyCovArrCaller = getCreateInitDummyArrLocal(bCaller, numPaths, entryClass);
					
					callArgs.add(IntConstant.v(0)); // dummy reg is just a 0
					callArgs.add(lDummyCovArrCaller);
				}
			}
			
			--dLeftAtCallee;
		}
		// insert inc-copy code just before call
		PatchingChain pchain = bCaller.getUnits();
		if (!callIncCode.isEmpty())
			InstrumManager.v().insertAtProbeBottom(pchain, callIncCode, sCall);
		
		// create new instance of the same type of invoke expr, with enhanced args list
		SootMethodRef mRefOrig = origInvExpr.getMethodRef();
		SootClass calleeClass = mRefOrig.declaringClass();
		Local base = (Local) ((origInvExpr instanceof InstanceInvokeExpr)? ((InstanceInvokeExpr)origInvExpr).getBase() : null);
		
		// if callee class is a lib class, replace it with corresponding intermediate class
		if (intermediateClasses.containsKey(calleeClass)) {
			// replace lib class with corresponding intermediate class
			calleeClass = intermediateClasses.get(calleeClass);
			
			// add stmt to cast base from original lib class to intermediate class
			// NOTE: might be unsafe, if in runtime the base can also point to a lib class or null
			if (origInvExpr instanceof InstanceInvokeExpr) {
				List intermClassCastCode = new ArrayList();
				InstanceInvokeExpr origInstInvExpr = (InstanceInvokeExpr) origInvExpr;
				base = getCreateCastLocal(bCaller, calleeClass.getType()); // change base to subtype local
				Stmt sCastToIntermClass = Jimple.v().newAssignStmt(base, Jimple.v().newCastExpr(origInstInvExpr.getBase(), calleeClass.getType()));
				intermClassCastCode.add(sCastToIntermClass);
				InstrumManager.v().insertAtProbeBottom(pchain, intermClassCastCode, sCall);
			}
		}
		
		SootMethod mNew;
		try { mNew = calleeClass.getMethod(mRefOrig.name(), calleeParamTypes); }
		catch (RuntimeException e) { throw new BaseCalleeNotFound(); } // call targets lib method that didn't get an intermediate class
		InvokeExpr invExpr;
		if (origInvExpr instanceof JInterfaceInvokeExpr)
			invExpr = Jimple.v().newInterfaceInvokeExpr(base, mNew.makeRef(), callArgs);
		else if (origInvExpr instanceof JSpecialInvokeExpr)
			invExpr = Jimple.v().newSpecialInvokeExpr(base, mNew.makeRef(), callArgs);
		else if (origInvExpr instanceof JVirtualInvokeExpr)
			invExpr = Jimple.v().newVirtualInvokeExpr(base, mNew.makeRef(), callArgs);
		else {
			assert origInvExpr instanceof JStaticInvokeExpr; // we shouldn't be missing a case
			invExpr = Jimple.v().newStaticInvokeExpr(mNew.makeRef(), callArgs);
		}
		
		if (sCall instanceof InvokeStmt)
			((InvokeStmt)sCall).setInvokeExpr(invExpr);
		else
			((AssignStmt)sCall).setRightOp(invExpr);
	}
	
	private Local getCreateCastLocal(Body b, RefType t) {
		// try to find existing local with special name
		final String castLocalName = "<cast_" + t.getClassName() + ">";
		Local l;
		for (Object oL : b.getLocals()) {
			l = (Local) oL;
			if (l.getName().equals(castLocalName))
				return l;
		}
		
		// not found; create and return new local with special name
		l = Jimple.v().newLocal(castLocalName, t);
		b.getLocals().add(l);
		return l;
	}
	
	private final static String DUMMYARR_LOCAL = "<dummy_pcovarr>";
	private final static String DUMMYARR_FIELD = "<dummyfield_pcovarr>";
	
	/**
	 * Returns dummy arr local, if it exists; if not, creates dummy local and dummy field (if field doesn't exist either), and inits dummy local at beginning of body.
	 * Also, resizes dummy field array with given num paths, if bigger that current size.
	 */
	private Local getCreateInitDummyArrLocal(Body b, int numPaths, SootClass entryClass) {
		// look for existing dummy arr local
		Local lDummyArr = getDummyArrLocal(b);
		if (lDummyArr == null) {
			// dummy arr local doesn't exist; get/create-init field
			SootField fDummyArr;
			try { fDummyArr = entryClass.getFieldByName(DUMMYARR_FIELD); }
			catch (RuntimeException re){
				assert dummyArrInitExpr == null;
				
				// create field
				fDummyArr = new SootField(DUMMYARR_FIELD, ArrayType.v(IntType.v(), 1), Modifier.STATIC | Modifier.PUBLIC);
				entryClass.addField(fDummyArr);
				
				// init field at entry class's clinit
				SootMethod mClinitEntry = UtilInstrum.getCreateClsInit(entryClass);
				Local lClinitDummyArr = getCreateDummyArrLocal(mClinitEntry.retrieveActiveBody());
				// instantiate arr at local
				// TODO: fix this big constant to the size needed, which can be bigger...
				List fieldInitCode = new ArrayList();
				dummyArrInitExpr = Jimple.v().newNewArrayExpr(IntType.v(), IntConstant.v(numPaths));
				Stmt sLocalArrNew = Jimple.v().newAssignStmt(lClinitDummyArr, dummyArrInitExpr);
				fieldInitCode.add(sLocalArrNew);
				// store local in field
				Stmt sLocalArrToField = Jimple.v().newAssignStmt(Jimple.v().newStaticFieldRef(fDummyArr.makeRef()), lClinitDummyArr);
				fieldInitCode.add(sLocalArrToField);
				
				PatchingChain pchainClinit = mClinitEntry.retrieveActiveBody().getUnits();
				pchainClinit.insertBefore(fieldInitCode, pchainClinit.getFirst());
			}
			
			// create local and add code to bring field to local
			lDummyArr = createDummyArrLocal(b);
			List fetchArrCode = new ArrayList();
			Stmt sFetchArr = Jimple.v().newAssignStmt(lDummyArr, Jimple.v().newStaticFieldRef(fDummyArr.makeRef()));
			fetchArrCode.add(sFetchArr);
			
			PatchingChain pchain = b.getUnits();
			Stmt sFirstNonId = UtilInstrum.getFirstNonIdStmt(pchain);
			InstrumManager.v().insertRightBeforeNoRedirect(pchain, fetchArrCode, sFirstNonId);
		}
		else {
			// enlarge dummy array, if necessary
			final int dummyArrSize = ((IntConstant)dummyArrInitExpr.getSize()).value;
			if (dummyArrSize < numPaths)
				dummyArrInitExpr.setSize(IntConstant.v(numPaths));
		}
		
		return lDummyArr;
	}
	/** Gets/creates the local only, not the field or the field init code. Doesn't add fetch code either. */
	private Local getCreateDummyArrLocal(Body b) {
		// try to find it first
		Local lDummyArr = getDummyArrLocal(b);
		if (lDummyArr != null)
			return lDummyArr;
		
		// failed; create it
		return createDummyArrLocal(b);
	}
	private Local createDummyArrLocal(Body b) {
		Local lDummyArr = Jimple.v().newLocal(DUMMYARR_LOCAL, ArrayType.v(IntType.v(), 1));
		b.getLocals().add(lDummyArr);
		return lDummyArr;
	}
	private Local getDummyArrLocal(Body b) {
		Local lDummyArr;
		for (Object oLocal : b.getLocals()) {
			lDummyArr = (Local) oLocal;
			if (lDummyArr.getName().equals(DUMMYARR_LOCAL))
				return lDummyArr;
		}
		return null;
	}
	
	/** Checks if corresponding reg-copy local is in array (not in any method's body, for now). Creates a new one if not. */
	private Local getCreateRegCopyLocal(Body b, Local reg) {
		// get name for copy reg
		final String copyName = getRegCopyLocalName(reg);
		
		// look in existing locals
		for (Object oLocal : b.getLocals()) {
			Local l = (Local) oLocal;
			if (l.getName().equals(copyName))
				return l;
		}
		
		// not found; create it
		Local lRegCopy = Jimple.v().newLocal(copyName, IntType.v());
		b.getLocals().add(lRegCopy);
		
		return lRegCopy;
	}
	
	/**
	 * The standard profiling operation in BL96 would be to fetch counter indexed by r, increment it, and put it back into memory.
	 * However, we are only concerned here with coverage, so we just set to 1 the counter indexed by r.
	 * This code is for interprocedural EPP, so cov-arr params are also updated.
	 */
	private List createCovArrayUpdateCode(Body b, SootField pathCountArrayField, Local r, Local lArray, List<Pair<Local, Local>> localEPPparams) {
		// create list that will contain code
		List covArrUpdateCode = new ArrayList();
		
		Stmt sSetCovFlag = Jimple.v().newAssignStmt(Jimple.v().newArrayRef(lArray, r), IntConstant.v(1));
		covArrUpdateCode.add(sSetCovFlag);
		
		// DEBUG: just before arr access, insert code that prints a message if r is out of bounds
		if (Options.eppCheckBounds())
			covArrUpdateCode.addAll(covArrUpdateCode.indexOf(sSetCovFlag), createBoundsCheckCode(b, sSetCovFlag, lArray, r));
		
		// update cov-arrays received as formal params (locals)
		for (Pair<Local,Local> depthLocals : localEPPparams) {
			if (depthLocals != null) {
				// get r and arr formal param locals for given depth
				Stmt sSetReceivedCovFlag = Jimple.v().newAssignStmt(Jimple.v().newArrayRef(depthLocals.second(), depthLocals.first()), IntConstant.v(1));
				covArrUpdateCode.add(sSetReceivedCovFlag);
				
				// DEBUG: just before arr access, insert code that prints a message if r is out of bounds
				if (Options.eppCheckBounds())
					covArrUpdateCode.addAll(covArrUpdateCode.indexOf(sSetCovFlag), createBoundsCheckCode(b, sSetReceivedCovFlag, depthLocals.second(), depthLocals.first()));
			}
		}
		
		return covArrUpdateCode;
	}
	
	private List createBoundsCheckCode(Body b, Stmt sArrAccess, Local lArray, Local r) {
		List checkBoundsCode = new ArrayList();
		
		//    len = lArray.lenght
		//    if (r < len) goto 1:
		//      System.out.println("out of bounds etc...");
		// 1: sArrAccess
		
		// check and conditional jump part
		Local lLen = getCreateLenghtLocal(b);
		Stmt sArrLenToLocal = Jimple.v().newAssignStmt(lLen, Jimple.v().newLengthExpr(lArray));
		checkBoundsCode.add(sArrLenToLocal);
		Stmt sIfRTooBig = Jimple.v().newIfStmt(Jimple.v().newLtExpr(r, lLen), sArrAccess);
		checkBoundsCode.add(sIfRTooBig);
		
		// message print part
		checkBoundsCode.addAll(UtilInstrum.createPrintlnCode(b, "r " + r + " out of bounds for arr " + lArray));
//		checkBoundsCode.addAll(UtilInstrum.createPrintlnCode(b, "r " + r + " for arr " + lArray));
		
		return checkBoundsCode;
	}
	
	private static final String EPP_LEN_LOCAL_NAME = "<epplen>";
	private Local getCreateLenghtLocal(Body b) {
		Local lLen = UtilInstrum.getLocal(b, EPP_LEN_LOCAL_NAME);
		if (lLen == null) { // create local
			lLen = Jimple.v().newLocal(EPP_LEN_LOCAL_NAME, IntType.v());
			b.getLocals().add(lLen);
		}
		return lLen;
	}
	
	private List createRegResetCode(Body b, Local r, List<Pair<Local, Local>> localEPPparams, int[] incomingDepthIncs) {
		List code = new ArrayList();
		
		// reset this method's r
		Stmt sResetR = Jimple.v().newAssignStmt(r, IntConstant.v(incomingDepthIncs[0]));
		code.add(sResetR);
		
		// reset r's received as params
		int d = 1;
		for (Pair<Local,Local> depthLocals : localEPPparams) {
			if (depthLocals != null) {
				// get r and arr formal param locals for given depth
				Stmt sResetReceivedR = Jimple.v().newAssignStmt(depthLocals.first(), IntConstant.v(incomingDepthIncs[d]));
				code.add(sResetReceivedR);
			}
			++d;
		}
		
		return code;
	}
	
	private List createRegIncCode(Body b, Local r, List<Pair<Local, Local>> localEPPparams, int[] incomingDepthIncs) {
		List code = new ArrayList();
		// inc this method's r
		Stmt sIncR = Jimple.v().newAssignStmt(r, Jimple.v().newAddExpr(r, IntConstant.v(incomingDepthIncs[0])));
		code.add(sIncR);
		
		// inc r's received as params
		int d = 1;
		for (Pair<Local, Local> depthLocals : localEPPparams) {
			if (depthLocals != null) {
				// get r and arr formal param locals for given depth
				Stmt sIncReceivedR = Jimple.v().newAssignStmt(depthLocals.first(), Jimple.v().newAddExpr(depthLocals.first(), IntConstant.v(incomingDepthIncs[d])));
				code.add(sIncReceivedR);
			}
			++d;
		}
		
		return code;
	}
	
	/**
	 * Returns cov-array variable for method, if exists, or creates and inits it and returns the new local
	 * @param m Method on which to create local variable for coverage-array
	 * @param arrField Array field to which variable must be assigned to at beginning of method
	 * @return The existing or newly created cov-array local var
	 */
	private Local getCreateInitArrLocal(SootMethod m, SootField arrField) {
		// look for existing array local
		Body b = m.retrieveActiveBody();
		Local arrLocal = UtilInstrum.getLocal(b, getD0CovArrLocalName());
		if (arrLocal == null) {  // create new local, if not found
			// create new array local and add it to method
			arrLocal = Jimple.v().newLocal(getD0CovArrLocalName(), ArrayType.v(IntType.v(), 1));
			b.getLocals().add(arrLocal);
			
			// add code at beginning of method to init arr local to field arr
			if (arrField != null) {
				PatchingChain pchain = b.getUnits();
				Stmt sGetCounterArray = Jimple.v().newAssignStmt(arrLocal, Jimple.v().newStaticFieldRef(arrField.makeRef()));
				pchain.insertBeforeNoRedirect(sGetCounterArray, UtilInstrum.getFirstNonIdStmt(pchain));
			}
		}
		
		return arrLocal;
	}
	
	// DEBUG: generate list of paths
	private void outputEnumeratePaths(Collection<CFG> cfgs, int depth, Map<CFG, EPPAnalysis> cfgEPPAnalyses) {
		// create global idx for nodes, including catch blocks; assume cfgs are in some deterministic order (i.e., always the same)
		Map<CFGNode, Integer> globalStmtIdxs = new HashMap<CFGNode, Integer>();
		int globalStmtIdx = 0;
		for (CFG cfg : cfgs)
			for (CFGNode n : cfg.getNodes()) {
				assert globalStmtIdxs.size() < Integer.MAX_VALUE;
				globalStmtIdxs.put(n, globalStmtIdx++);
			}
		
		// build file name
		String fileName = Options.getOutPath();
		final char sepChar = File.separatorChar;
		if (!fileName.isEmpty() && fileName.charAt(fileName.length()-1) != sepChar)
			fileName += sepChar;
		(new File(fileName)).mkdir(); // ensure directory is created
		fileName += "paths";
		
		// write edges for each CFG (method) as we go
		File fPaths = new File(fileName);
		try {
			FileOutputStream oEdges = new FileOutputStream(fPaths);
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(oEdges));
			
			// enumerate paths for each cfg, in order
			for (CFG cfg : cfgs) {
				// ArrayList<ArrayList<Integer>> allPaths =
				cfgEPPAnalyses.get(cfg).enumPaths(writer, globalStmtIdxs, depth);
//				int pathIdx = 0;
//				for (ArrayList<Integer> path : allPaths)
//					writer.write(" " + (pathIdx++) + ": " + path + "\n");
			}
			
			writer.flush();
		}
		catch (FileNotFoundException e) {
			System.err.println("Couldn't write PATHS file: " + e);
		}
		catch (SecurityException e) {
			System.err.println("Couldn't write PATHS file: " + e);
		}
		catch (IOException e) {
			System.err.println("Couldn't write PATHS file: " + e);
		}
	}
	
}
