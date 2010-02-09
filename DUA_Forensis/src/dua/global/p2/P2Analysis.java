package dua.global.p2;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import soot.ArrayType;
import soot.Local;
import soot.RefLikeType;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.jimple.ArrayRef;
import soot.jimple.CastExpr;
import soot.jimple.DefinitionStmt;
import soot.jimple.FieldRef;
import soot.jimple.IdentityStmt;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.Jimple;
import soot.jimple.NewArrayExpr;
import soot.jimple.NewExpr;
import soot.jimple.NullConstant;
import soot.jimple.ReturnStmt;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;
import dua.global.ProgramFlowGraph;
import dua.method.CFG;
import dua.method.MethodTag;
import dua.method.CFG.CFGNode;
import dua.unit.Location;
import dua.unit.StmtTag;
import dua.util.Pair;
import dua.util.Util;

/** Rather simple algorithm: abstract allocation sites, field-insensitive, transmission via assignments
 *  between locals and/or fields and/or array elements.
 *  
 *  An abstract points-to class is a set of allocation sites, represented by a bitset.
 */
public class P2Analysis {
	/** Wraps a soot value so that we can use Util.valuesEqual for an abstract equality comparison of different field and array refs. */
	public static class PointerVar {
		/** Helper for toString(), so that locals are shown along with containing method. */
		private static Map<Local,SootMethod> localToMethod = new HashMap<Local, SootMethod>();
		
		/** A variable of ref type, which can be a local, field ref, array element ref, or object ref. */
		private Value val;
		/** Null if local; signature of field or type of array element or object expression otherwise. */
		private String strNonLocalTypeName = null;
		public PointerVar(Value val) {
			assert val instanceof Local || val instanceof FieldRef || val instanceof ArrayRef; // sanity check
			this.val = val;
		}
		public PointerVar(SootMethod m, Value val) {
			this(val);
			if (val instanceof Local) localToMethod.put((Local)val, m);
		}
		@Override public int hashCode() { return (val instanceof Local)? val.hashCode() : getFldArrObjName().hashCode(); }
		@Override public boolean equals(Object other) { return Util.valuesEqual(this.val, ((PointerVar)other).val); }
		@Override public String toString() {
			return (val instanceof Local)? localToMethod.get((Local)val).toString() + "." + val :
										   strNonLocalTypeName;
		}
		
		/** Gets/creates cache string name for field or array elem type or object. */
		private String getFldArrObjName() {
			if (strNonLocalTypeName == null) {
				if (val instanceof FieldRef) {
					FieldRef fld = (FieldRef)val;
					strNonLocalTypeName = fld.getField().getSignature();
				}
				else {
					assert val instanceof ArrayRef;
					strNonLocalTypeName = ((ArrayRef)val).getType() + "[]";
				}
			}
			
			return strNonLocalTypeName;
		}
	}
	
	public static class AllocSite {
		private final CFGNode n; // location
		private final Value val; // which value within stmt
		public AllocSite(CFGNode n, Value val) { this.n = n; this.val = val; }
		@Override public int hashCode() { return n.hashCode() + val.hashCode(); }
		@Override public boolean equals(Object other) {
			AllocSite siteOther = (AllocSite) other;
			return this.n == siteOther.n && this.val.equals(siteOther.val);
		}
		@Override public String toString() { return "SITE(" + ProgramFlowGraph.inst().getContainingMethod(n) + "," + n + "," + val + ")"; }
	}
	
	/** Listed so that idx in this list will represent an alloc site during propagation.
	 *  First alloc site (idx 0) is 'null'. */
	private List<AllocSite> allocSites = new ArrayList<AllocSite>();
	/** Reverse global map site->idx. */
	private Map<AllocSite,Integer> siteToSiteIdx = new HashMap<AllocSite, Integer>();
	/** Listed so that idx in this list will represent a ptr var during propagation.
	 *  A ptr var is just a Value of ref type that can be a local, field ref, or array element ref. */
	private List<PointerVar> pointers = new ArrayList<PointerVar>();
	/** Reverse global map ptr->idx. */
	private Map<PointerVar,Integer> ptrToPtrIdx = new HashMap<PointerVar, Integer>();
	/** Maps each ptr idx (array's idx) to all lhs ptr idxs (bitset) to which it is assigned. */
	private List<BitSet> assignedToSets = new ArrayList<BitSet>();
	/** Alloc class pointed by each ptr, indexed by ptr index. Index 0 in class bitset is null class. */
	private List<BitSet> ptrIdxToClass = new ArrayList<BitSet>();
	
	// SINGLETON
	private P2Analysis() {}
	private static final P2Analysis inst = new P2Analysis();
	public static P2Analysis inst() { return inst; }
	
	// QUERIES
	/** Returns all possible alloc sites to which the given value can point to. */
	public BitSet getP2Set(Value val) {
		final int ptrIdx = ptrToPtrIdx.get(new PointerVar(val));
		return ptrIdxToClass.get(ptrIdx);
	}
	/** Returns class with just alloc site for given stmt and value within that stmt. */
	public BitSet getP2SetForAllocSite(CFGNode n, Value val) {
//		assert s.getUseBoxes().contains(val);
		BitSet clsSingleSite = new BitSet();
		Integer siteIdx = siteToSiteIdx.get(new AllocSite(n, val));
//		assert siteIdx != null;
		// TODO: site idx should always exist in a complete p2 analysis
		if (siteIdx != null)
			clsSingleSite.set(siteIdx);
		return clsSingleSite;
	}
	
	// ANALYSIS
	public void compute() {
		final long startTime = System.currentTimeMillis();
		System.out.println("Starting P2 analysis");
		
		// 1. List all alloc sites and pointers
		BitSet modPtrs = new BitSet();
		for (CFG cfg : ProgramFlowGraph.inst().getCFGs()) {
			assert cfg.isReachableFromEntry();
			SootMethod m = cfg.getMethod();
			final boolean isEntry = ProgramFlowGraph.inst().getEntryMethods().contains(m);
			final boolean isEntryOrNotCalledMethod = isEntry || ((MethodTag)m.getTag(MethodTag.TAG_NAME)).getCallerSites().isEmpty();
			for (CFGNode n : cfg.getNodes()) {
				// special cases: 'special' or catch block node
				if (n.isSpecial()) {
					if (isEntry) {  // only at ENTRY define pre-existing ptrs, such as System.out
						// obtain System.out field and its type for fake instantiation
						SootClass clsSystem = Scene.v().getRefType("java.lang.System").getSootClass();
						FieldRef fldOut = Jimple.v().newStaticFieldRef(clsSystem.getFieldByName("out").makeRef());
						SootClass clsPrintStream = Scene.v().getRefType("java.io.PrintStream").getSootClass();
						// init seed for System.out static field (new PrintStream object assigned to System.out)
						initSeed(m, fldOut, n, Jimple.v().newNewExpr(clsPrintStream.getType()), modPtrs); // 'null' stmt
					}
					continue;
				}
				if (n.isInCatchBlock())
					continue; // ignore, for now
				
				Stmt s = n.getStmt();
				if (s.containsInvokeExpr()) {
					/// Process parameter-linking part of statement
					//   (return stmt transfer/seed is handled later at def assignment)
					InvokeExpr invExpr = s.getInvokeExpr();
					// create a "transfer" from each arg to each matching formal param (including base->'this') on each app callee
					// if arg is null or a string const, create "seed" instead
					for (SootMethod mAppCallee : n.getCallSite().getAppCallees()) {
						// create "transfer" from base to 'this', if instance call; base is necessarily of ref type
						Iterator<Unit> itCalleeStmt = mAppCallee.retrieveActiveBody().getUnits().iterator();
						if (invExpr instanceof InstanceInvokeExpr) {
							IdentityStmt sCallee = (IdentityStmt) itCalleeStmt.next();
							initTransfer(mAppCallee, m, sCallee.getLeftOp(), (Local)((InstanceInvokeExpr)invExpr).getBase());
						}
						// create "transfer" for each argument-formalparam link
						for (int idxArg = 0; idxArg < invExpr.getArgCount(); ++idxArg) {
							Value arg = invExpr.getArg(idxArg);
							IdentityStmt sCallee = (IdentityStmt) itCalleeStmt.next();
							if (arg instanceof Local) {
								if (arg.getType() instanceof RefLikeType)  // only consider obj/arr ref locals
									initTransfer(mAppCallee, m, sCallee.getLeftOp(), (Local)invExpr.getArg(idxArg));
							}
							else if (arg instanceof NullConstant || arg instanceof StringConstant) {
								// init seed (assignment of null or string const)
								initSeed(mAppCallee, sCallee.getLeftOp(), n, arg, modPtrs);
							}
						}
					}
					// if there are lib callees, use model to create transfers and seeds, if any
					//  IMPORTANT: include special case in which no app or lib targets was found
					if (n.getCallSite().hasLibCallees() || !n.getCallSite().hasAppCallees()) {
						for (Pair<Value,Value> transferValPair : P2ModelManager.getTransfers(invExpr)) {
							// null lhs indicates variable on the lhs of assignment of return value
							Value valLhs = transferValPair.first();
							if (valLhs == null) {
								if (s instanceof DefinitionStmt)
									valLhs = ((DefinitionStmt)s).getLeftOp();
								else
									continue; // return value is not assigned
							}
							Value valRhs = transferValPair.second();
							if (valRhs instanceof StringConstant || valRhs instanceof NullConstant)
								initSeed(m, valLhs, n, valRhs, modPtrs); // special case! rhs happens to be a seed value rather than a ptr
							else
								initTransfer(m, m, valLhs, valRhs);
						}
						for (Pair<Value,Value> seedValPair : P2ModelManager.getSeeds(invExpr)) {
							// null lhs indicates variable on the lhs of assignment of return value
							Value valLhs = seedValPair.first();
							if (valLhs == null) {
								if (s instanceof DefinitionStmt)
									valLhs = ((DefinitionStmt)s).getLeftOp();
								else
									continue; // return value is not assigned
							}
							initSeed(m, valLhs, n, seedValPair.second(), modPtrs);
						}
					}
				}
				if (s instanceof DefinitionStmt) {  // process assignment part of statement
					// only consider formal params for entry methods
					if (!isEntryOrNotCalledMethod && s instanceof IdentityStmt)
						continue;
					
					// determine whether lhs is a ptr
					Value valLhs = ((DefinitionStmt)s).getLeftOp();
					if (!(valLhs.getType() instanceof RefLikeType))
						continue; // only process assignments to refs to objects or arrays
					
					// process rhs: new expr or invoke or null-const or ptr var (possibly casted)
					Value valRhs = ((DefinitionStmt)s).getRightOp();
					if ((isEntryOrNotCalledMethod && s instanceof IdentityStmt) ||  // def in id stmt at entry method is always a seed
						valRhs instanceof NewExpr || valRhs instanceof NewArrayExpr ||
						valRhs instanceof NullConstant || valRhs instanceof StringConstant)
					{
						// init seed (initial assignment to left ptr of new object or null or string const)
						initSeed(m, valLhs, n, valRhs, modPtrs);
						
						if ((isEntry && s instanceof IdentityStmt) || valRhs instanceof NewArrayExpr) {
							RefType refTypeElem;
							// create mock array elem lhs occurring at same place as array creation
							ArrayRef arrRefStrElemLeft = Jimple.v().newArrayRef((Local)((DefinitionStmt)s).getLeftOp(), IntConstant.v(0));
							if (isEntry && s instanceof IdentityStmt) {
								if (!m.toString().equals("<"+m.getDeclaringClass()+": void main(java.lang.String[])>"))
									continue; // TODO: FOR NOW, we don't support seeds for other entry method signatures
								// special args array string element for 'main(String[] args)'
								assert n == cfg.getNodes().get(1); // should be first id stmt (just after ENTRY)
								refTypeElem = Scene.v().getRefType("java.lang.String");
							}
							else {
								// only consider creation of ref types
								Type typeElem = ((NewArrayExpr)valRhs).getBaseType();
								if (!(typeElem instanceof RefLikeType))
									continue;
								// separate treatment for new array of arrays
								if (typeElem instanceof ArrayType) {
									initSeed(m, arrRefStrElemLeft, n, Jimple.v().newNewArrayExpr(typeElem, IntConstant.v(0)), modPtrs);
									continue;
								}
								refTypeElem = (RefType) typeElem;  // NOTE: will fail if it's an array of arrays!
							}
							// add seed from virtual "new ElemType" to mock array elem lhs occurring at same place as array creation
//							ArrayRef arrRefStrElemLeft = Jimple.v().newArrayRef((Local)((DefinitionStmt)s).getLeftOp(), IntConstant.v(0));
							initSeed(m, arrRefStrElemLeft, n, Jimple.v().newNewExpr(refTypeElem), modPtrs);
						}
					}
					else {  // handle "transfer" stmt
						if (valRhs instanceof InvokeExpr) {  // process ret-val assignment part of stmt (param-linking is handled above)
							// ... for all returns from all possible call targets
							for (SootMethod mCallee : n.getCallSite().getAppCallees()) {
								MethodTag mCalleeTag = (MethodTag) mCallee.getTag(MethodTag.TAG_NAME);
								for (Location retLoc : mCalleeTag.getExits()) {
									// get ret stmt and ret val
									ReturnStmt sRetCallee = (ReturnStmt) retLoc.getStmt();
									StmtTag sRetTag = (StmtTag) sRetCallee.getTag(StmtTag.TAG_NAME);
									if (sRetTag.isInCatchBlock())
										continue;
									Value retVal = sRetCallee.getOp();
									// create "transfer" if retval is local ref, or "seed" if retval is null of string constant
									if (retVal instanceof Local) {
										if (retVal.getType() instanceof RefLikeType)  // only consider obj/arr ref locals
											initTransfer(m, mCallee, valLhs, (Local)retVal);
									}
									else if (retVal instanceof NullConstant || retVal instanceof StringConstant) {
										// init seed (assignment of null or string const)
										CFGNode nRetCallee = ProgramFlowGraph.inst().getNode(sRetCallee);
										initSeed(m, valLhs, nRetCallee, retVal, modPtrs);
									}
								}
							}
						}
						else {
							// "de-cast" right value (we ignore effects of cast in analysis, for now)
							if (valRhs instanceof CastExpr)
								valRhs = ((CastExpr)valRhs).getOp();
							if (valRhs instanceof NullConstant)
								initSeed(m, valLhs, n, valRhs, modPtrs);
							else {
								assert valRhs instanceof Local || valRhs instanceof FieldRef || valRhs instanceof ArrayRef;
								initTransfer(m, m, valLhs, valRhs);
							}
						}
					}
				}
			}
		}
		
		// 2. Iteratively merge p2 sets of vars for assignments
		// ... until no more changes
		while (!modPtrs.isEmpty()) {  // don't finish while there are still ptrs marked as mod
			System.out.print(".");
			
			BitSet modPtrsClone = (BitSet) modPtrs.clone(); // use clone of mod set so we can safely update mod set
			modPtrs.clear(); // start from scratch collecting modified ptrs
			int ptrIdxRight = -1;
			while ((ptrIdxRight = modPtrsClone.nextSetBit(ptrIdxRight + 1)) != -1) {
				// prepare to propagate right-ptr's class to its assigned-to set of lhs ptrs
				BitSet rhsCls = ptrIdxToClass.get(ptrIdxRight);
				BitSet lhsPtrs = assignedToSets.get(ptrIdxRight);
				
				// iterate over lhs ptrs for this rhs ptr
				int idxLhsPtr = -1;
				while ((idxLhsPtr = lhsPtrs.nextSetBit(idxLhsPtr + 1)) != -1) {
					// get old (prev) cls for lhs, and clone it to create updated cls for lhs
					BitSet lhsClsOld = ptrIdxToClass.get(idxLhsPtr);
					BitSet lhsClsUpdated = (BitSet) lhsClsOld.clone();
					lhsClsUpdated.or(rhsCls); // update lhs cls with rhs's cls
					
					// update lhs's cls if it changed, and update every other ptr associated to old cls to point to updated cls
					if (!lhsClsUpdated.equals(lhsClsOld)) {
						ptrIdxToClass.set(idxLhsPtr, lhsClsUpdated);
						modPtrs.set(idxLhsPtr);
					}
				}
			}
		}
		
		System.out.println();
		System.out.println("Finished P2 analysis");
		System.out.println("P2 analysis took " + (System.currentTimeMillis() - startTime) + " ms");
		
//		dumpResults();
	}
	
	/** Creates/registers rhs and lhs ptr vars and registers "transfer" (assignment) of rhs (arg) in caller to lhs in callee. */
	private void initTransfer(SootMethod mLeft, SootMethod mRight, Value valLhs, Value valRhs) {
		// get/create ptr var for rhs
		PointerVar ptrRight = new PointerVar(mRight, valRhs);
		final int ptrIdxRight = getRegisterPtrIdx(ptrRight);
		// get/create ptr var for lhs
		PointerVar ptrLeft = new PointerVar(mLeft, valLhs);
		final int ptrIdxLeft = getRegisterPtrIdx(ptrLeft);
		// update right ptr's assigned-to set with lhs ptr
		assignedToSets.get(ptrIdxRight).set(ptrIdxLeft);
	}
	
	/** Creates new alloc site based on method and stmt. Also creates and registers left ptr var, adding alloc site to its p2 class. */
	private void initSeed(SootMethod mLeft, Value valLhs, CFGNode nRight, Value valRhs, BitSet modPtrs) {
		// register alloc site (which can be a new expr or null)
		final int idxAllocSite = allocSites.size();
		AllocSite locAlloc = new AllocSite(nRight, valRhs);
		allocSites.add(locAlloc);
		siteToSiteIdx.put(locAlloc, idxAllocSite);
		
		// get/create ptr var for lhs
		PointerVar ptrLeft = new PointerVar(mLeft, valLhs);
		final int ptrIdxLeft = getRegisterPtrIdx(ptrLeft);
		// add alloc site to lhs ptr's p2 set
		BitSet clsLeft = ptrIdxToClass.get(ptrIdxLeft);
		if (clsLeft.isEmpty()) {
			// add alloc site as first elem of ptr's associated class
			clsLeft.set(idxAllocSite);
		}
		else {
			// update cls with this new alloc site
			clsLeft.set(idxAllocSite);
			assert clsLeft.cardinality() > 1;
		}
		
		// finally, mark seeded ptr for propagation
		modPtrs.set(ptrIdxLeft);
	}
	
	/** Gets existing ptr based on method and var. If it doesn't exist yet, creates new ptr*/
	private Integer getRegisterPtrIdx(PointerVar ptr) {
		Integer idxPtr = ptrToPtrIdx.get(ptr);
		if (idxPtr == null) {
			idxPtr = pointers.size();
			pointers.add(ptr);
			ptrToPtrIdx.put(ptr, idxPtr);
			
			assignedToSets.add(new BitSet());
			ptrIdxToClass.add(new BitSet());
		}
		return idxPtr;
	}
	
	private void dumpResults() {
		BitSet[] siteToPtrs = new BitSet[allocSites.size()];
		
		System.out.println("Ptrs to alloc sites:");
		int maxSitesPerPtr = 0;
		for (int ptrIdx = 0; ptrIdx < pointers.size(); ++ptrIdx) {
			BitSet clsForPtr = ptrIdxToClass.get(ptrIdx);
			final int numSites = clsForPtr.cardinality();
			if (maxSitesPerPtr < numSites)
				maxSitesPerPtr = numSites;
			System.out.print("  " + pointers.get(ptrIdx) + ": " + numSites);
			int siteIdx = -1;
			while ((siteIdx = clsForPtr.nextSetBit(siteIdx + 1)) != -1) {
				System.out.print(" " + allocSites.get(siteIdx));
				
				// fill map site->ptrs
				BitSet ptrsForSite = siteToPtrs[siteIdx];
				if (ptrsForSite == null) {
					ptrsForSite = new BitSet();
					siteToPtrs[siteIdx] = ptrsForSite;
				}
				ptrsForSite.set(ptrIdx);
			}
			System.out.println();
		}
		System.out.println("Max alloc sites per ptr: " + maxSitesPerPtr);
		
		System.out.println("Alloc sites to ptrs:");
		int maxPtrsPerSite = 0;
		for (int siteIdx = 0; siteIdx < siteToPtrs.length; ++siteIdx) {
			BitSet ptrsForSite = siteToPtrs[siteIdx];
			final int numPtrs = ptrsForSite.cardinality();
			if (maxPtrsPerSite < numPtrs)
				maxPtrsPerSite = numPtrs;
			
			System.out.print("  " + allocSites.get(siteIdx) + ": " + numPtrs);
			
			int ptrIdx = -1;
			while ((ptrIdx = ptrsForSite.nextSetBit(ptrIdx + 1)) != -1)
				System.out.print(" " + pointers.get(ptrIdx));
			System.out.println();
		}
		System.out.println("Max ptrs per alloc site: " + maxPtrsPerSite);
		
	}
	
}
