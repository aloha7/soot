package dua.method;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import soot.Local;
import soot.SootMethod;
import soot.Value;
import soot.jimple.Constant;
import soot.jimple.IdentityStmt;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.ReturnStmt;
import soot.jimple.Stmt;
import soot.toolkits.graph.Block;
import dua.Options;
import dua.unit.StmtTag;


/**
 * Reachable uses and definitions.
 * Backwards analysis, intra-procedural, with support for inter-procedural propagation coordinated from outside.
 */
public class ReachableUsesDefs extends CFGDefUses {
	/** Extension of CFG nodes to propagate reachable uses and reachable defs */
	public static class NodeReachDefsUses extends NodeDefUses {
		private BitSet uGen;
		private BitSet uKillComp;
		private BitSet uBackIn;
		private BitSet uBackOut;
		
		private BitSet dGen;
		private BitSet dKillComp;
		private BitSet dBackIn; // reachable defs
		private BitSet dBackOut; // reachable defs
		private BitSet dFwdIn; // reaching defs
		private BitSet dFwdOut; // reaching defs
		
		public BitSet getUGen() { return uGen; }
		public void setUGen(BitSet gen) { this.uGen = gen; }
		public BitSet getUBackIn() { return uBackIn; }
		public void setUBackIn(BitSet in) { this.uBackIn = in; }
		public BitSet getUKillComp() { return uKillComp; }
		public void setUKill(BitSet kill, int size) {
			uKillComp = kill;
			uKillComp.flip(0, size);
		}
		public BitSet getUBackOut() { return uBackOut; }
		public void setUBackOut(BitSet out) { this.uBackOut = out; }
		
		public BitSet getDGen() { return dGen; }
		public void setDGen(BitSet gen) { this.dGen = gen; }
		public BitSet getDBackIn() { return dBackIn; }
		public void setDBackIn(BitSet in) { this.dBackIn = in; }
		public BitSet getDFwdIn() { return dFwdIn; }
		public void setDFwdIn(BitSet in) { this.dFwdIn = in; }
		public BitSet getDKillComp() { return dKillComp; }
		public void setDKill(BitSet kill, int size) {
			dKillComp = kill;
			dKillComp.flip(0, size);
		}
		public BitSet getDBackOut() { return dBackOut; }
		public void setDBackOut(BitSet out) { this.dBackOut = out; }
		public BitSet getDFwdOut() { return dFwdOut; }
		public void setDFwdOut(BitSet out) { this.dFwdOut = out; }
		
		public NodeReachDefsUses(Stmt s) { super(s); }
	}
	
	public static class FormalParam {
		private Value v; // value
		private IdentityStmt idStmt; // location where formal param "enters" method
		
		public Value getV() { return v; }
		public IdentityStmt getIdStmt() { return idStmt; }
		
		public FormalParam(Value v, IdentityStmt idStmt) { this.v = v; this.idStmt = idStmt; }
		
		@Override
		public String toString() { return idStmt.toString(); } //v.toString(); }
	}
	
	public static class CSParam {
		protected CallSite cs;
		protected int paramIdx;
		
		public CallSite getCs() { return cs; }
		public int getParamIdx() { return paramIdx; }
		
		public CSParam(CallSite cs, int paramIdx) { this.cs = cs; this.paramIdx = paramIdx; }
		
		@Override
		public String toString() { return paramIdx + "@" + cs.toString(); }
	}
	
//	public static enum ReachUsePathType { INTRA, INTER, BOTH };
	
	protected ArrayList<FormalParam> formalParams = new ArrayList<FormalParam>(); // order matters; first param is 'this' for instance methods
	protected HashMap<Integer, CSParam> usesToCSParams = new HashMap<Integer, CSParam>(); // uses that are actual params in app calls
	protected BitSet realLocalUses; // indicates which uses are real (incl. actual params and return values if at least one target is lib)
	
	/** Links each formal param in this method to all its reachable real uses in the program */
	protected HashMap<FormalParam, HashSet<Use>> paramsToAllRealUses = new HashMap<FormalParam, HashSet<Use>>();
	/** Links each value-returning node in this method to all real uses in the program reachable after returning */
	protected HashMap<Use, Set<Use>> retUsesToAllRealUses = new HashMap<Use, Set<Use>>();
	/** Links each constant-value-returning node (ret-const-def) in this method to real uses after returning */
	protected HashMap<ConstReturnDef, Set<Use>> retConstDefsToAllRealUses = new HashMap<ConstReturnDef, Set<Use>>();
	protected HashMap<FormalParam, HashSet<Def>> paramsToAllDefs = new HashMap<FormalParam, HashSet<Def>>();
	protected HashMap<FormalParam, HashSet<CSParam>> paramsToLocalCallParams = new HashMap<FormalParam, HashSet<CSParam>>();
	
	/** Maps each local def to its set of (interproc_use,local_use) pairs */
	protected HashMap<Def, HashMap<Use, ArrayList<Use>>> dus = new HashMap<Def, HashMap<Use,ArrayList<Use>>>();
	protected Map<Def, Set<Use>> sameBBdus = new HashMap<Def, Set<Use>>();
	protected HashMap<Def, HashSet<Def>> dds = new HashMap<Def, HashSet<Def>>();
	
	public int getNumFormalParams() { return formalParams.size(); }
	public FormalParam getFormalParam(int paramIdx) { return formalParams.get(paramIdx); }
	public HashMap<Def, HashSet<Def>> getDDs() { return dds; }
	public HashMap<Def, HashMap<Use, ArrayList<Use>>> getDUs() { return dus; }
	public Map<Def, Set<Use>> getSameBBDUs() { return sameBBdus; }
	
	public ReachableUsesDefs(SootMethod m) {
		super(m);
	}
	@Override
	public void analyze() {
		super.analyze(); // should always perform superclass's initial analysis first
		
		identifyParamsAndCalls();
		
		// for each node, compute GEN and KILL, and initial IN and OUT; then, propagate local uses
		initSets();
		localPropagateReachableDefsUses();
		localPropagateReachingDefs();
		
		// init param uses to local real uses; link param uses to local call uses
		initParamDefsUses();
	}
	
	@Override
	protected NodeReachDefsUses instantiateNode(Stmt s) { return new NodeReachDefsUses(s); }
	
	/** Finds formal params and call sites; fills callUses map and realUses bitset */
	private void identifyParamsAndCalls() {
		final int numUses = idsToUses.size();
		realLocalUses = new BitSet(numUses);
		
		boolean paramDeclZone = true; // first stmt(s) are formal param declarations ("identity" stmts)
		for (CFGNode _n : nodes) {
			if (_n instanceof CFGNodeSpecial)
				continue; // ENTRY or EXIT
			
			NodeReachDefsUses n = (NodeReachDefsUses) _n;
			Stmt s = n.getStmt();
			StmtTag sTag = (StmtTag) s.getTag(StmtTag.TAG_NAME);
			
			// formal param?
			if (paramDeclZone) {
				if (s instanceof IdentityStmt) {
					IdentityStmt ids = (IdentityStmt) s;
					formalParams.add(new FormalParam( ids.getLeftOp(), ids ));
				}
				else
					paramDeclZone = false; // end of param decl zone
			}
			
			// call site, return, or regular stmt?
			int[] uses = n.getLocalUsesIds();
			if (sTag.hasCallSite()) {  // general call site (might not have app callees
				CallSite cs = sTag.getCallSite();
				
				// get actual params
				InvokeExpr invExpr = s.getInvokeExpr();
				ArrayList<Value> actualParams = new ArrayList<Value>();
				if (invExpr instanceof InstanceInvokeExpr) {
					Value base = ((InstanceInvokeExpr)invExpr).getBase();
					assert base instanceof Local;
					actualParams.add(base);
				}
				actualParams.addAll( invExpr.getArgs() );
				
				if (cs.hasAppCallees()) {
					// We assume that args are listed in order: base first (if instance), then args in order of appearance in call
					int actualParamIdx = 0;//use.getVar().getParamIdx(actualParams);
					for (int u : uses) {
						// find param index
//						Use use = idsToUses.get(u);
						assert actualParamIdx >= 0;
						assert actualParamIdx < actualParams.size();
						
						// if 'this' flow not allowed, add use as real instead of linking to param
						if (!Options.allowThisFlow() && cs.isInstanceCall() && actualParamIdx == 0)
							realLocalUses.set(u);
						else
							usesToCSParams.put(new Integer(u), new CSParam(cs, actualParamIdx));
						
						++actualParamIdx;
					}
				}
				if (cs.hasLibCallees()) {
					for (int u : uses)
						realLocalUses.set(u);
				}
			}
			else if (s instanceof ReturnStmt) {  // value-returning stmt
				assert uses.length <= 1;
				
				if (uses.length == 1) {
					// check this method's callers
					MethodTag mTag = (MethodTag) getMethod().getTag(MethodTag.TAG_NAME);
					boolean hasAppCaller = false;
					for (SootMethod mCaller : mTag.getCallerMethods()) {
						// we can't check yet if caller is reachable from entry, but at least we can tell if it's an app method
						MethodTag mTagCaller = (MethodTag) mCaller.getTag(MethodTag.TAG_NAME);
						if (mTagCaller != null) {
							hasAppCaller = true;
							break;
						}
					}
					
					// return-use if at least 1 app caller
					if (hasAppCaller)
						retUsesToAllRealUses.put(idsToUses.get(uses[0]), new HashSet<Use>()); // init set of real call-ret-uses for this ret node
					else
						realLocalUses.set(uses[0]);
				}
			}
			else {  // regular stmt
				for (int u : uses)
					realLocalUses.set(u);
			}
		}
		
		// DEBUG
		System.out.print("  Params: ");
		int paramId = 0;
		for (FormalParam p : formalParams)
			System.out.print((paramId++) + ":" + p + ",");
		System.out.println();
		
		System.out.println("  Call/Real uses:");
		System.out.print("    ");
		for (int id = 0; id < numUses; ++id)
			System.out.print(id + ":" + (realLocalUses.get(id)? "R" : "") + (usesToCSParams.containsKey(id)? "C" : "") + ",");
		System.out.println();
	}
	
	/**
	 * Important: we assume that two values representing the same variable (locals only, in this case), 
	 * always point to the same object.
	 */
	private void initSets() {
		final int usesSetSize = idsToUses.size();
		final int defsSetSize = idsToDefs.size();
		
		// DEBUG
		System.out.println("  Bitset sizes " + usesSetSize + ",  " + defsSetSize + " for method " + method);
		System.out.println("  uses " + idsToUses);
		System.out.println("  defs " + idsToDefs);
		
//		final boolean allowIdStmtsKills = Options.allowIdStmtsKills();
		
		for (CFGNode _n : nodes) {
			if (_n instanceof CFGNodeSpecial)
				continue; // ENTRY or EXIT
			
			NodeReachDefsUses n = (NodeReachDefsUses) _n;
			
			// GEN: uses and defs at this node
			BitSet uGen = new BitSet(usesSetSize);
			int[] usesIds = n.getLocalUsesIds();
			for (int useId : usesIds)
				uGen.set(useId);
			n.setUGen(uGen);
			
			BitSet dGen = new BitSet(defsSetSize);
			int[] defsIds = n.getLocalDefsIds();
			for (int defId : defsIds)
				dGen.set(defId);
			n.setDGen(dGen);
			
			// KILL: uses and defs killed at this node
			BitSet uKill = new BitSet(usesSetSize); // all bits are 0
			BitSet dKill = new BitSet(defsSetSize); // all bits are 0
			// find def that is local, if any
			if (!(n.getStmt() instanceof IdentityStmt) /*|| allowIdStmtsKills*/) {
				for (int defId : defsIds) {
					Variable defVar = idsToDefs.get(defId).getVar();
					if (defVar.isLocal()) {
						// Important: we assume that two values representing the same variable (locals only, in this case),
				  		// always point to the same object
						uKill.or( varsToUses.get(defVar) );
						
						// FIX 6-9-2007: defs are not killed, to avoid this problem:
						// If d1->d2->d3->u, and d1->u, d2 is not assoc to u, 
						// so d1 has not detected this kill d3 which is masked by d2.
	//					dKill.or( valuesToDefs.get(defV) );
					}
				}
			}
			n.setUKill(uKill, usesSetSize);
			n.setDKill(dKill, defsSetSize);
			
			// backward analysis
			// IN = GEN; OUT = empty
			n.setUBackIn((BitSet) uGen.clone());
			n.setUBackOut(new BitSet(usesSetSize));
			n.setDBackIn((BitSet) dGen.clone());
			n.setDBackOut(new BitSet(defsSetSize));
			// forward analysis
			// IN = empty; OUT = GEN
			n.setDFwdIn(new BitSet(defsSetSize));
			n.setDFwdOut((BitSet) dGen.clone());
		}
	}
	
	/** Locally propagate backwards reachable uses and defs*/
	private void localPropagateReachableDefsUses() {
		boolean fixedPoint = false;
		int iterCount = 0;
		while (!fixedPoint) {
			fixedPoint = true;
			
			System.out.println("  Reachable uses and defs iteration #" + iterCount++);
			
			// iterate from last real node down to first real node; last node in list is EXIT, and first node is ENTRY
			for (int nodeIdx = nodes.size() - 2; nodeIdx >= 1; --nodeIdx) {
				NodeReachDefsUses n = (NodeReachDefsUses) nodes.get(nodeIdx);
				
				// OUT(n) = U (IN(succ))
				for (CFGNode _nSucc : n.getSuccs()) {
					if (_nSucc == EXIT)
						continue;
					
					NodeReachDefsUses nSucc = (NodeReachDefsUses) _nSucc;
					n.getUBackOut().or(nSucc.getUBackIn());
					n.getDBackOut().or(nSucc.getDBackIn());
				}
				
				// IN(n) = GEN(n) U (OUT(n) - KILL(n))
				BitSet uOutKilled = (BitSet) n.getUBackOut().clone();
				uOutKilled.and( n.getUKillComp() ); // interset OUT with complement of KILL
				BitSet oldUIn = n.getUBackIn();
				BitSet newUIn = (BitSet) oldUIn.clone();
				newUIn.or(uOutKilled);
				if (!newUIn.equals( n.getUBackIn() )) {
					n.setUBackIn(newUIn);
					fixedPoint = false;
				}
				BitSet dOutKilled = (BitSet) n.getDBackOut().clone();
				dOutKilled.and( n.getDKillComp() ); // intersect OUT with complement of KILL
				BitSet oldDIn = n.getDBackIn();
				BitSet newDIn = (BitSet) oldDIn.clone();
				newDIn.or(dOutKilled);
				if (!newDIn.equals( n.getDBackIn() )) {
					n.setDBackIn(newDIn);
					fixedPoint = false;
				}
			}
		}
	}
	
	/** Locally propagate forward reaching definitions */
	private void localPropagateReachingDefs() {
		boolean fixedPoint = false;
		int iterCount = 0;
		while (!fixedPoint) {
			fixedPoint = true;
			
			System.out.println("  Reaching defs iteration #" + iterCount++);
			
			// iterate from first real node down to last real node; first node is ENTRY, and last node in list is EXIT
			for (int nodeIdx = 1; nodeIdx <= nodes.size() - 2; ++nodeIdx) {
				NodeReachDefsUses n = (NodeReachDefsUses) nodes.get(nodeIdx);
				
				// IN(n) = U (OUT(pred))
				for (CFGNode _nPred : n.getPreds()) {
					if (_nPred == ENTRY)
						continue;
					
					NodeReachDefsUses nPred = (NodeReachDefsUses) _nPred;
					n.getDFwdIn().or(nPred.getDFwdOut());
				}
				
				// OUT(n) = GEN(n) U (IN(n) - KILL(n))
				BitSet dInNotKilled = (BitSet) n.getDFwdIn().clone();
				dInNotKilled.and( n.getDKillComp() ); // intersect OUT with complement of KILL
				BitSet oldDOut = n.getDFwdOut();
				BitSet newDOut = (BitSet) oldDOut.clone();
				newDOut.or(dInNotKilled);
				if (!newDOut.equals( oldDOut )) {
					n.setDFwdOut(newDOut);
					fixedPoint = false;
				}
			}
		}
	}
	
	@Override
	public String toString() {
		String str = "CFG " + method + ": ";
		
		int i = 0;
		for (CFGNode _n : nodes) {
			if (_n instanceof CFGNodeSpecial)
				continue; // ENTRY or EXIT
			
			NodeReachDefsUses n = (NodeReachDefsUses) _n;
			str += i + ": " + n + ", ";
			++i;
		}
		
		return str;
	}
	
	/** Init param uses and defs to local real uses and defs, reachable from entry; link param uses to local call uses */
	protected void initParamDefsUses() {
		// param real uses and call uses
		for (FormalParam p : formalParams) {
			// get all uses and defs that match value to param
			StdVariable paramVar = new StdVariable(p.v);
			BitSet entryParamUses = varsToUses.get(paramVar);
			if (entryParamUses != null)
				entryParamUses = (BitSet)entryParamUses.clone(); // clone, so we don't end up modifying valuesToUses bitset
			BitSet entryParamDefs = varsToDefs.get(paramVar);
			if (entryParamDefs != null)
				entryParamDefs = (BitSet)entryParamDefs.clone(); // clone, so we don't end up modifying valuesToDefs bitset
			
			if (entryParamUses == null && entryParamDefs == null)
				continue; // nothing to associate to this param
			
			NodeReachDefsUses n = (NodeReachDefsUses) getFirstRealNode();
			if (entryParamUses != null) {
				// reduce set for param to uses that reach the entry node
				entryParamUses.and(n.uBackIn);
				
				for (int useId = 0; useId < entryParamUses.length(); ++useId) {
					if (entryParamUses.get(useId)) {
						if (realLocalUses.get(useId)) {
							// get/create set of real uses
							HashSet<Use> realUsesForParam = paramsToAllRealUses.get(p);
							if (realUsesForParam == null) {
								realUsesForParam = new HashSet<Use>();
								paramsToAllRealUses.put(p, realUsesForParam);
							}
							realUsesForParam.add(idsToUses.get(useId));
						}
						CSParam csParam = usesToCSParams.get(useId);
						if (csParam != null) {
							// either 'this' is allowed, or it's not param 0 'this'
							assert Options.allowThisFlow() || csParam.paramIdx != 0 || !csParam.cs.isInstanceCall();
							
							// get/create set of call uses
							HashSet<CSParam> callParamsForParam = paramsToLocalCallParams.get(p);
							if (callParamsForParam == null) {
								callParamsForParam = new HashSet<CSParam>();
								paramsToLocalCallParams.put(p, callParamsForParam);
							}
							callParamsForParam.add(csParam);
						}
					}
				}
			}
			if (entryParamDefs != null) {
				// reduce set for param to defs that reach the entry node
				entryParamDefs.and(n.dBackIn);
				
				for (int defId = 0; defId < entryParamDefs.length(); ++defId) {
					if (entryParamDefs.get(defId)) {
						// get/create set of reachable defs for param
						HashSet<Def> defsForParam = paramsToAllDefs.get(p);
						if (defsForParam == null) {
							defsForParam = new HashSet<Def>();
							paramsToAllDefs.put(p, defsForParam);
						}
						defsForParam.add(idsToDefs.get(defId)); // add local reaching def to param def set
					}
				}
			}
		}
	}
	
	/** Links each formal param with all defs and real uses from call sites that use it as actual param */
	public boolean propagateAllUsesDefsToParams(Map<SootMethod, ReachableUsesDefs> methodsToReachDUs) {
		boolean modified = false;
		
		// update real uses reachable from formal params
		for (FormalParam p : formalParams) {
			HashSet<CSParam> callUsesForParam = paramsToLocalCallParams.get(p);
			if (callUsesForParam == null)
				continue; // no call uses for param
			for (CSParam callUse : callUsesForParam) {
				for (SootMethod mCallee : callUse.cs.getAppCallees()) {
					ReachableUsesDefs ruCallee = methodsToReachDUs.get(mCallee);
					
					// propagate current set of real uses in callee for cs-param to formal param here
					FormalParam calleeParam = ruCallee.formalParams.get(callUse.getParamIdx());
					HashSet<Use> calleeRealUsesForParam = ruCallee.paramsToAllRealUses.get(calleeParam);
					if (calleeRealUsesForParam != null) {
						// get/create real use set for this param
						HashSet<Use> currentRealUsesForParam = paramsToAllRealUses.get(p);
						if (currentRealUsesForParam == null) {
							currentRealUsesForParam = new HashSet<Use>();
							paramsToAllRealUses.put(p, currentRealUsesForParam);
						}
						// link to formal param all real uses associated to call param
						for (Use uRealUse : calleeRealUsesForParam) {
							if (currentRealUsesForParam.add(uRealUse))
								modified = true;
						}
					}
					
					// propagate current set of defs in callee for cs-param to formal param here
					HashSet<Def> calleeAllDefs = ruCallee.paramsToAllDefs.get(calleeParam);
					if (calleeAllDefs != null) {
						// get/create real def set for this param
						HashSet<Def> allDefs = paramsToAllDefs.get(p);
						if (allDefs == null) {
							allDefs = new HashSet<Def>();
							paramsToAllDefs.put(p, allDefs);
						}
						if (allDefs.addAll(calleeAllDefs))
							modified = true;
					}
				}
			}
		}
		
		// find real uses (ret-value uses) reachable from return use in this method
		for (Use uRetVal : retUsesToAllRealUses.keySet()) {
			Set<Use> realRetUses = retUsesToAllRealUses.get(uRetVal);
			modified = addCSRetUses(methodsToReachDUs, modified, realRetUses);
		}
		
		// find special uses reachable from const-returns in this method
		for (ConstReturnDef dConstRet : retConstDefs) {
			// create set of call-site ret-uses for ret-const-def
			Set<Use> realRetUses = retConstDefsToAllRealUses.get(dConstRet);
			if (realRetUses != null)
				continue; // already created and filled
			realRetUses = new HashSet<Use>();
			retConstDefsToAllRealUses.put(dConstRet, realRetUses);
			
			modified = addCSRetUses(methodsToReachDUs, modified, realRetUses);
		}
		
		return modified;
	}
	/** @param realRetUses [IN,OUT] set to which to add all cs-ret-uses
	 *  @return Updated 'modified' flag */
	private boolean addCSRetUses(Map<SootMethod, ReachableUsesDefs> methodsToReachDUs, boolean modified, Set<Use> realRetUses) {
		MethodTag mTag = (MethodTag) getMethod().getTag(MethodTag.TAG_NAME);
		for (CallSite csCaller : mTag.getCallerSites()) {
			// find ret-use at call site
			SootMethod mCaller = csCaller.getLoc().getMethod();
			MethodTag mTagCaller = (MethodTag) mCaller.getTag(MethodTag.TAG_NAME);
			if (mTagCaller != null && mTagCaller.isReachableFromEntry()) {
				ReachableUsesDefs ruCaller = methodsToReachDUs.get(mCaller);
				CFGNode nCaller = ruCaller.getNode(csCaller.getLoc().getStmt());
				Use uRetAtCaller = ruCaller.callRetUses.get(nCaller);
				if (uRetAtCaller != null) {
					if (realRetUses.add(uRetAtCaller))
						modified = true;
				}
			}
		}
		return modified;
	}
	
	// DEBUG
	public void dumpReachUsesDefs() {
		System.out.println("Reachable uses and defs for params in " + method);
		for (FormalParam p : formalParams) {
			HashSet<Use> usesForParam = paramsToAllRealUses.get(p);
			HashSet<Def> defsForParam = paramsToAllDefs.get(p);
			System.out.println("  " + p + ": uses " + ((usesForParam == null)? "<none>" : (usesForParam.size() + " " + usesForParam))
					+ ", defs " + ((defsForParam == null)? "<none>" : (defsForParam.size() + " " + defsForParam)));
		}
	}
	
	/** Constructs map of each def to all real uses and all defs (inter-proc) of locals (or cs params)
	 *  Note that if same BB def-uses are not allowed, that does affect def-puses
	 *  For now, it excludes a def-use of def-def association when one of the two is in a catch block
	 *  
	 *  Works by matching each def in method with local 'real' uses/defs first, using local bit vectors,
	 *  and then matches def with 'real' uses/defs flowing back from call sites (parameter uses)
	 */
	public void findLocalUsesDefsForDefs(Map<SootMethod, ReachableUsesDefs> methodsToReachUseDefs) {
		final boolean allowSameBBDuas = Options.allowSameBBDuas();
		final boolean allowParmsRetUseDefs = Options.allowParmsRetUseDefs();
		final boolean intraDUAsOnly = Options.intraDUAsOnly();
		final boolean allowThisFlow = Options.allowThisFlow();
		
		// build stmt->idx map
		HashMap<Stmt, Integer> sToIdx = new HashMap<Stmt, Integer>();
		int nIdx = 0;
		for (CFGNode n : nodes) {
			if (n instanceof CFGNodeSpecial)
				continue; // ENTRY or EXIT
			
			sToIdx.put(n.s, nIdx);
			++nIdx;
		}
		
		// for each node, find matches of def with reachable uses and defs
		for (CFGNode _nDef : nodes) {
			if (_nDef instanceof CFGNodeSpecial)
				continue; // ENTRY or EXIT
			
			if (_nDef.isInCatchBlock())
				continue; // discard defs and uses in catch blocks, for now...
			
			// use propagated local defs plus const-arg defs
			NodeReachDefsUses nDef = (NodeReachDefsUses) _nDef;
			int[] localDefsIds = nDef.getLocalDefsIds();
			List<Def> defs = new ArrayList<Def>(localDefsIds.length + argConstDefs.size());
			for (int defIdx = 0; defIdx < localDefsIds.length; ++defIdx)
				defs.add(idsToDefs.get(localDefsIds[defIdx]));
			defs.addAll(argConstDefs);
			
			for (Def def : defs) {
				Variable defVar = def.getVar();
				if (!defVar.isLocalOrConst())
					continue; // skip
				
				// if def v is a constant (cs param), then we check uIn instead uOut
				BitSet ruBS = defVar.isConstant()? nDef.uBackIn : nDef.uBackOut;
				Block defBB = ((StmtTag) nDef.getStmt().getTag(StmtTag.TAG_NAME)).getBasicBlock();
				
				// Get all local real uses for def first
				// Then, link def to call-site inter-proc real uses/defs
				for (int uIdx = 0; uIdx < ruBS.size(); ++uIdx) {
					// see if local use upward-reaches def
					if (!ruBS.get(uIdx))
						continue; // discard use if doesn't reach here
					Use reachableLocalUse = idsToUses.get(uIdx);
					if (!reachableLocalUse.getVar().equals(defVar))//, defArgIdx))
						continue; // discard use with value that doesn't match def
					
					CFGNode reachUseSrcNode = reachableLocalUse.getSrcNode();
					if (reachUseSrcNode.isInCatchBlock())
						continue; // discard defs and uses in catch blocks, for now...
					
					// variable matched for def and local use
					// check if local use is 'real' (unless the allowParmsRetUseDefs option is set)
					// note that a constant var use (for constant cs params) is not a 'real' use
					// also, consider use of base param to call (if instance call), if allowThisFlow is disabled
					// note that intraDUAsOnly implies allowParmsRetUseDefs
					CSParam csUseParam = usesToCSParams.get(uIdx);
					if (defVar.isLocal() && 
							(allowParmsRetUseDefs || realLocalUses.get(uIdx) ||
							 (!allowThisFlow && csUseParam != null && csUseParam.paramIdx == 0 && csUseParam.cs.isInstanceCall())))
					{
						// check if same-BB def-uses are allowed, or whether the use is a p-use, 
						// or if the use is in another BB, or the use occurs before the def
						Block useBB = ((StmtTag) reachUseSrcNode.s.getTag(StmtTag.TAG_NAME)).getBasicBlock();
						if (allowSameBBDuas || (reachableLocalUse instanceof PUse) || useBB != defBB ||
								sToIdx.get(nDef.s) >= sToIdx.get(reachUseSrcNode.s)) {
							// Get/create arraylist of real uses for def
							HashMap<Use, ArrayList<Use>> realUses = dus.get(def);
							if (realUses == null) {
								realUses = new HashMap<Use, ArrayList<Use>>();
								dus.put(def, realUses);
							}
							// Create/update list of local uses associated to real use
							ArrayList<Use> localUsesForRealUse = realUses.get(reachableLocalUse);
							if (localUsesForRealUse == null) {
								localUsesForRealUse = new ArrayList<Use>();
								realUses.put(reachableLocalUse, localUsesForRealUse);
							}
							assert !localUsesForRealUse.contains(reachableLocalUse);
							localUsesForRealUse.add(reachableLocalUse);
							
//							// Set path type for local real use
//							ReachUsePathType utype = ruPathTypes.get(reachableLocalUse);
//							if (utype == null)
//								ruPathTypes.put(reachableLocalUse, ReachUsePathType.INTRA);
//							else if (utype == ReachUsePathType.INTER)
//								ruPathTypes.put(reachableLocalUse, ReachUsePathType.BOTH);
						}
						else {
							// store SEPARATELY as same-bb du
							Set<Use> realSameBBUses = sameBBdus.get(def);
							if (realSameBBUses == null) {
								realSameBBUses = new HashSet<Use>();
								sameBBdus.put(def, realSameBBUses);
							}
							realSameBBUses.add(reachableLocalUse);
						}
					}
					
					// Check if use is a call-site param
					if (!intraDUAsOnly && csUseParam != null) {
						for (SootMethod mCallee : csUseParam.cs.getAppCallees()) {
							// avoid flowing through 'this' param, if setting is disabled
							if (!allowThisFlow && !mCallee.isStatic() && csUseParam.paramIdx == 0)
								continue;
							
							// associate param to all real uses and defs for callee's corresponding param
							ReachableUsesDefs ruCallee = methodsToReachUseDefs.get(mCallee);
							FormalParam fp = ruCallee.formalParams.get(csUseParam.paramIdx);
							HashSet<Use> realUsesForParam = ruCallee.paramsToAllRealUses.get(fp);
							if (realUsesForParam != null) {  // there exist real uses for this formal param in callee
								// Get/create arraylist of real uses for def
								HashMap<Use, ArrayList<Use>> realUsesForDef = dus.get(def);
								if (realUsesForDef == null) {
									realUsesForDef = new HashMap<Use, ArrayList<Use>>();
									dus.put(def, realUsesForDef);
								}
								// Create/update list of local uses associated to each real use
								for (Use interprocUse : realUsesForParam) {
									if (interprocUse.getSrcNode().isInCatchBlock())
										continue;
									
									ArrayList<Use> localUsesForRealUse = realUsesForDef.get(interprocUse);
									if (localUsesForRealUse == null) {
										localUsesForRealUse = new ArrayList<Use>();
										realUsesForDef.put(interprocUse, localUsesForRealUse);
									}
									if (!localUsesForRealUse.contains(reachableLocalUse))
										localUsesForRealUse.add(reachableLocalUse);
								}
							}
							HashSet<Def> allDefsForParam = ruCallee.paramsToAllDefs.get(fp);
							if (allDefsForParam != null) {  // there are defs for callee's param
								// get/create arraylist of all defs for def
								HashSet<Def> allDefs = dds.get(def);
								if (allDefs == null) {
									allDefs = new HashSet<Def>();
									dds.put(def, allDefs);
								}
								allDefs.addAll(allDefsForParam);
							}
						}
					}
					
					// Check if use is a return value from this CFG
					Set<Use> retUses = retUsesToAllRealUses.get(reachableLocalUse);
					if (retUses != null && !retUses.isEmpty()) {
						// Get/create arraylist of real uses for def
						HashMap<Use, ArrayList<Use>> realUsesForDef = dus.get(def);
						if (realUsesForDef == null) {
							realUsesForDef = new HashMap<Use, ArrayList<Use>>();
							dus.put(def, realUsesForDef);
						}
						for (Use uRet : retUses) {
							ArrayList<Use> localUses = new ArrayList<Use>();
							localUses.add(reachableLocalUse);
							realUsesForDef.put(uRet, localUses);
						}
					}
				}
				
				// Finally, get all local reachable defs that match value with def
				for (int dIdx = 0; dIdx < nDef.dBackOut.size(); ++dIdx) {
					if (!nDef.dBackOut.get(dIdx))
						continue; // discard def if doesn't reach here
					Def reachableLocalDef = idsToDefs.get(dIdx);
					if (!reachableLocalDef.getVar().equals(def.getVar()))
						continue; // discard reachable def with value that doesn't match def
					
					if (reachableLocalDef.getN().isInCatchBlock())
						continue; // discard reachable defs in catch blocks, for now...
					
					// get/create arraylist of all reachable defs for def
					HashSet<Def> allDefs = dds.get(def);
					if (allDefs == null) {
						allDefs = new HashSet<Def>();
						dds.put(def, allDefs);
					}
					allDefs.add(reachableLocalDef);
				}
			}
		}
		
		// in addition to du's found during CFG traversal, add const-ret-def du's
		for (ConstReturnDef def : retConstDefsToAllRealUses.keySet()) {
			if (def.isInCatchBlock())
				continue; // discard defs and uses in catch blocks, for now...
			
			assert !dus.containsKey(def);
			
			Set<Use> retUses = retConstDefsToAllRealUses.get(def);
			for (Use uRet : retUses) {
				if (uRet.isInCatchBlock())
					continue; // discard defs and uses in catch blocks, for now...
				// get/create uses set for def
				HashMap<Use, ArrayList<Use>> realUsesForRetDef = dus.get(def);
				if (realUsesForRetDef == null) {
					realUsesForRetDef = new HashMap<Use, ArrayList<Use>>();
					dus.put(def, realUsesForRetDef);
				}
				realUsesForRetDef.put(uRet, new ArrayList<Use>()); // empty set of "local uses"
			}
		}
	}
	
}
