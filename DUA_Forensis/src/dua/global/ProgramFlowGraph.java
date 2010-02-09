package dua.global;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import soot.PatchingChain;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.Stmt;
import dua.Options;
import dua.cls.ClassTag;
import dua.method.CFG;
import dua.method.CFGFactory;
import dua.method.MethodTag;
import dua.method.CFG.CFGNode;
import dua.unit.StmtTag;

/**
 * Available as a singleton for convenient access from everywhere.
 * Keeps collection of CFGs for all methods, connected interprocedurally.
 * Also keeps track of Soot methods, entry method, and reachable Soot methods. These methods are used to build the CFGs.
 * CFG building is parameterized: client provides a factory object.
 */
public class ProgramFlowGraph {
	public static class EntryNotFoundException extends Exception {
		public EntryNotFoundException(String msg) { super(msg); }
	}
	
	/** Comparator to find unique ordering of methods based on lexical order */
	private static class MethodComparator implements Comparator<SootMethod> {
		public int compare(SootMethod o1, SootMethod o2) {
			return o1.toString().compareTo(o2.toString());
		}
	}
	
	/** Holds singleton instance */
	private static ProgramFlowGraph pfgSingleton = null;
	/** Returns singleton instance */
	public static ProgramFlowGraph inst() { return pfgSingleton; }
	
	//
	// Regular fields
	
	private Map<SootMethod, CFG> mCFGs = new HashMap<SootMethod, CFG>();
	/** All application classes (at Soot level), reachable or not */
	private List<SootClass> appClasses = null;
	/** All application concrete methods, reachable or not */
	public List<SootMethod> allAppMethods = null;
	/** Methods reachable from entry, including clinits. INCLUDES methods called from catch blocks. */
	private List<SootMethod> reachableAppMethods = new ArrayList<SootMethod>();
	private List<SootMethod> entryMethods = null;
	/** Unique index for each method (same order for any execution) */
	private Map<SootMethod,Integer> mToId = new HashMap<SootMethod, Integer>();
	/** Ordered CFGs */
	private List<CFG> reachableCfgs = new ArrayList<CFG>();
	
	/** Only maps reachable stmts and methods */
	private HashMap<Stmt,SootMethod> stmtToMethod = null; // built on demand
	/** Only maps reachable nodes to CFGs */
	private HashMap<CFGNode,CFG> nodeToCFG = null; // built on demand
	
	public CFG getCFG(SootMethod m) { return mCFGs.get(m); }
	public Map<SootMethod, CFG> getMethodToCFGMap() { return mCFGs; }
	public List<SootMethod> getReachableAppMethods() { return reachableAppMethods; }
	public List<CFG> getCFGs() { return reachableCfgs; }
	public List<SootMethod> getEntryMethods() { return entryMethods; }
	public CFG getMethodCFG(SootMethod m) { return  mCFGs.get(m); }
	
	/** Builds singleton instance with given parameters. Allows only one instantiation. */
	public static void createInstance(CFGFactory cfgFactory) throws EntryNotFoundException {
		if (pfgSingleton != null)
			throw new RuntimeException("Instance of ProgramFlowGraph already exists! Can't instantiate more than once...");
		
		// construct in two steps, so CFG construction has method info available
		pfgSingleton = new ProgramFlowGraph();
		pfgSingleton.initMethods();
		pfgSingleton.initCFGs(cfgFactory);
	}
	
	private static final String mainSubsig = "void main(java.lang.String[])";
	private List<SootMethod> findEntryAppMethod() throws EntryNotFoundException {
		if (entryMethods != null)
			return entryMethods;
		entryMethods = new ArrayList<SootMethod>();
		
		// get main class, and test classes
		String entryClassName = Options.entryClassName();
		List<String> entryTestClasses = Options.entryTestClasses();
		
		// search for first app class with a 'main' only if no entry or test classes are provided
		if (entryTestClasses == null && entryClassName == null) {
			for (SootClass cls : ProgramFlowGraph.inst().getAppClasses()) {
				try {
					entryMethods.add(cls.getMethod(mainSubsig));
					break;
				}
				catch (Exception e) { }
			}
			if (entryMethods.isEmpty())
				throw new EntryNotFoundException("No 'main' found in app classes");
		}
		
		// given entry class's 'main' is the first entry method in list
		if (entryClassName != null) {
			// find entry class matching given name
			for (SootClass cls : ProgramFlowGraph.inst().getAppClasses()) {
				if (cls.getName().equals(entryClassName)) {
					// find main method in entry class
					try {
						SootMethod mEntry = cls.getMethod(mainSubsig);
						entryMethods.add(mEntry);
						break;
					}
					catch (Exception e) {
						throw new EntryNotFoundException("Entry class " + entryClassName + " has no 'main'");
					}
				}
			}
			if (entryMethods.isEmpty())
				throw new EntryNotFoundException("Entry class name " + entryClassName + " not found");
		}
		
		// finally, add all 'test*' methods in test classes to entry-methods list
		if (entryTestClasses != null) {
			for (SootClass cls : ProgramFlowGraph.inst().getAppClasses()) {
				if (entryTestClasses.contains(cls.getName())) {
					// find all test* methods inside class
					for (Iterator itM = cls.getMethods().iterator(); itM.hasNext(); ) {
						SootMethod m = (SootMethod) itM.next();
						if (m.getName().startsWith("test"))
							entryMethods.add(m);
					}
				}
			}
 		}
		
		assert !entryMethods.isEmpty();
		return entryMethods;
	}
	
	public int getMethodIdx(SootMethod m) { return mToId.get(m); }
	
	/** Gets/creates collection of all application classes */
	public List<SootClass> getAppClasses() {
		if (appClasses == null) {
			appClasses = new ArrayList<SootClass>();
			for (Iterator itCls = Scene.v().getApplicationClasses().iterator(); itCls.hasNext(); )
				appClasses.add((SootClass) itCls.next());
		}
		
		return appClasses;
	}
	
	/** Gets/creates collection of all concrete methods (i.e., methods with a body) in application classes */
	public List<SootMethod> getAppConcreteMethods() {
		if (allAppMethods == null) {
			allAppMethods = new ArrayList<SootMethod>();
			List<SootClass> appClasses = getAppClasses();
			for (SootClass cls : appClasses) {
				for (Iterator itMthd = cls.getMethods().iterator(); itMthd.hasNext(); ) {
					SootMethod m = (SootMethod) itMthd.next();
					if (!m.isAbstract() && m.toString().indexOf(": java.lang.Class class$") == -1)
						allAppMethods.add(m);
				}
			}
			
			// sort list of all methods
			Collections.sort(allAppMethods, new MethodComparator());
		}
		
		return allAppMethods;
	}
	
	private void initMethods() throws EntryNotFoundException {
		List<SootMethod> appMethods = getAppConcreteMethods();
		List<SootMethod> entryMethods = findEntryAppMethod();
		
		this.entryMethods = entryMethods;
		
		// purely intraprocedural initialization
		// for all application Soot classes:
		//   find contextual defs and uses for each Stmt in each Method
		//   determine local kill summary for each Method
		// for each Soot Method: list defs, uses and kills
		createAppTags();
		
		// initialization requiring previously computed intra-procedural info
		for (SootMethod m : appMethods) {
			// compute initial (local) reaching ctx calls in method
			MethodTag mTag = (MethodTag) m.getTag(MethodTag.TAG_NAME);
			mTag.initCallSites();
		}
		
		// Once method tags and call sites are created, find subset of methods that are reachable from entry
		findReachableAppMethods(entryMethods);
		
		// Fill index map for reachable app methods
		int mIdx = 0;
		for (SootMethod m : reachableAppMethods)
			mToId.put(m, mIdx++);
	}
	private void initCFGs(CFGFactory cfgFactory) {
		// Create CFG for each method using provided factory
		for (SootMethod m : reachableAppMethods) {
			// compute CFG with defs, uses, and reachable uses
			CFG cfg = cfgFactory.createCFG(m);
			mCFGs.put(m, cfg);
			reachableCfgs.add(cfg);
		}
		// Use factory to perform additional required analysis for each CFG
		for (CFG cfg : reachableCfgs)
			cfg.analyze();
		
		System.out.println("Total reachable concrete methods: " + reachableCfgs.size());
	}
	
	private void createAppTags() {
		// create class tags
		for (SootClass c : ProgramFlowGraph.inst().getAppClasses())
			c.addTag(new ClassTag());
		
		// create method tags
		for (SootMethod m : ProgramFlowGraph.inst().getAppConcreteMethods())
			m.addTag(new MethodTag(m));
	}
	
	/**
	 * Finds and stores reachable methods from given entry.
	 * Also determines index for each method, in some execution-invariant ordering.
	 * 
	 * TODO: add <clinit> methods (implicitly called)
	 */
	private void findReachableAppMethods(List<SootMethod> mEntries) {
		// 0. All app methods are reachable if no reachability is to be computed
		if (!Options.reachability()) {
			reachableAppMethods = allAppMethods;
			return;
		}
		
		// 1. Find reachable methods
		//    prepare sets for method reachability algorithm
		Set<SootMethod> reachableSet = new HashSet<SootMethod>();
		Set<SootMethod> toVisit = new HashSet<SootMethod>();
		toVisit.addAll(mEntries); // seed to-visit list with entry method
		
		// visit methods, storing them as reachable, until no more transitively reachable (called) methods are available
		Set<SootClass> processedClinitClasses = new HashSet<SootClass>();
		while (!toVisit.isEmpty()) {
			// remove one element from to-visit list and mark it as 'reachable'
			SootMethod m = toVisit.iterator().next();
			toVisit.remove(m);
			reachableSet.add(m);
			
			// add all callees to to-visit list
			PatchingChain pchain = m.retrieveActiveBody().getUnits();
			for (Iterator it = pchain.iterator(); it.hasNext(); ) {
				Stmt s = (Stmt) it.next();
				StmtTag sTag = (StmtTag) s.getTag(StmtTag.TAG_NAME);
				if (sTag.isInCatchBlock())
					continue; // skip catch blocks, FOR NOW
				if (sTag.hasAppCallees()) {
					for (SootMethod mCallee : sTag.getAppCallees()) {
						if (!reachableSet.contains(mCallee))
							toVisit.add(mCallee);
					}
				}
			}
			
			// add <clinit> for class containing m to toVisit worklist, if not processed yet
			try {
				SootClass cls = m.getDeclaringClass();
				if (!processedClinitClasses.contains(cls)) {  // add to queue if not processed uet
					SootMethod mClinit = cls.getMethodByName("<clinit>");
					toVisit.add(mClinit);
					processedClinitClasses.add(cls);
				}
			}
			catch (RuntimeException e) {} // exception thrown if clinit not found in class
		}
		
		// copy set into list and order it
		reachableAppMethods.addAll(reachableSet);
		Collections.sort(reachableAppMethods, new MethodComparator());
	}
	
	public SootMethod getContainingMethod(Stmt s) {
		// build map on demand
		if (stmtToMethod == null) {
			stmtToMethod = new HashMap<Stmt, SootMethod>();
			for (SootMethod m : reachableAppMethods)
				for (Iterator itS = m.retrieveActiveBody().getUnits().iterator(); itS.hasNext(); )
					stmtToMethod.put((Stmt)itS.next(), m);
		}
		
		return stmtToMethod.get(s);
	}
	public SootMethod getContainingMethod(CFGNode n) {
		return getContainingMethod(n.getStmt());
	}
	
	public int getContainingMethodIdx(Stmt s) {
		return getMethodIdx(getContainingMethod(s));
	}
	public int getContainingMethodIdx(CFGNode n) {
		return getMethodIdx(getContainingCFG(n).getMethod());
	}
	
	public CFG getContainingCFG(Stmt s) {
		SootMethod m = getContainingMethod(s);
		return getMethodCFG(m);
	}
	public CFG getContainingCFG(CFGNode n) {
		// build map on demand
		if (nodeToCFG == null) {
			nodeToCFG = new HashMap<CFGNode, CFG>();
			for (CFG cfg : reachableCfgs) {
				for (CFGNode _n : cfg.getNodes())
					nodeToCFG.put(_n,cfg);
			}
		}
		
		return nodeToCFG.get(n);
	}
	public CFGNode getNode(Stmt s) {
		return getContainingCFG(s).getNode(s);
	}
	
}
