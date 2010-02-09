package dua.global;

import java.util.BitSet;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import soot.SootClass;
import soot.SootMethod;
import soot.jimple.RetStmt;
import soot.jimple.ReturnStmt;
import soot.jimple.Stmt;
import dua.method.CallSite;
import dua.method.MethodTag;
import dua.method.CFG.CFGNode;
import dua.unit.Location;
import dua.unit.StmtTag;

/**
 * Interprocedural reachability analysis: all stmts against all stmts.
 * An stmt always reaches itself and the methods it may call.
 */
public class ReachabilityAnalysis {
	private static boolean computed = false;
	public static boolean isComputed() { return computed; }
	
	/** 
	 * Computes method-to-method and stmt-to-stmt reachability.
	 * Also marks all methods as reachable or not reachable from provided entry method.
	 * Divides the problem in two parts: forward and backward reachability.
	 * 
	 * For a method m, the forward reachable methods are those transitively reachable from all call sites in m.
	 * In other words, the forward reachable methods of m are all those for which there is an interprocedural path
	 * from m's entry, before reaching m's exit (at the same call-depth level as m's entry, so re-entrance to m
	 * does not constitute a stopping point for such paths).
	 * 
	 * Backward reachable methods are all those methods reachable from m's exit (i.e., after m returns)
	 */
	public static void computeReachability(List<SootMethod> entryMethods) {
		// don't re-compute
		if (computed)
			return;
		computed = true;
		System.out.println("Computing interprocedural reachability from entry " + entryMethods);
		
		List<SootMethod> indexedAppMethods = ProgramFlowGraph.inst().getReachableAppMethods();
		
		// init forward reached method sets, and compute local stmt reachability along the way
		for (SootMethod m : indexedAppMethods) {
			MethodTag mTag = (MethodTag) m.getTag(MethodTag.TAG_NAME);
			for (CallSite csOut : mTag.getCallSites()) {
				if (csOut.isInCatchBlock())
					continue; // skip catch blocks, FOR NOW
				BitSet bsAppCallees = new BitSet();
				for (SootMethod mAppCallee :  csOut.getAppCallees())
					bsAppCallees.set(ProgramFlowGraph.inst().getMethodIdx(mAppCallee));
				mTag.addForwardReachedAppMtds(bsAppCallees);
			}
			
			mTag.computeLocalStmtReachability();
		}
		
		// iteratively propagate forward reachable methods
		System.out.println("Stage 1: forward reachability");
		boolean fixedPoint = false;
		while (!fixedPoint) {
			fixedPoint = true;
			System.out.print(".");
			
			// find and store forward reachable app methods, for each locally called app method
			for (SootMethod m : indexedAppMethods) {
				MethodTag mTag = (MethodTag) m.getTag(MethodTag.TAG_NAME);
				for (CallSite csOut : mTag.getCallSites()) {
					if (csOut.isInCatchBlock())
						continue; // skip catch blocks, FOR NOW
					for (SootMethod mTgt : csOut.getAppCallees()) {
						MethodTag mTgtTag = (MethodTag) mTgt.getTag(MethodTag.TAG_NAME);
						if (mTgtTag != null) {
							if (mTag.addForwardReachedAppMtds(mTgtTag.getForwardReachedAppMtds()))
								fixedPoint = false;
						}
					}
				}
			}
		}
		System.out.println();
		
		// init backward reached method sets with directly reached calls after return, and respective forward sets
		System.out.println("Stage 2: backward reachability");
		for (SootMethod m : indexedAppMethods) {
			MethodTag mTag = (MethodTag) m.getTag(MethodTag.TAG_NAME);
			// for each caller
			for (CallSite csIn : mTag.getCallerSites()) {
				if (!indexedAppMethods.contains(csIn.getLoc().getMethod()))
					continue; // skip callers not indexed (unreachable from entry)
				if (csIn.isInCatchBlock())
					continue; // skip catch blocks, FOR NOW
				
				// add immediately reachable locally called methods *after* caller site (i.e., take successors of caller site),
				// ... and forward reachable method set from each of these local reachable immediately called methods
				StmtTag sCallTag = (StmtTag) csIn.getLoc().getStmt().getTag(StmtTag.TAG_NAME);
				for (Stmt sCallSucc : sCallTag.getSuccessorStmts()) {
					StmtTag sCallSuccTag = (StmtTag) sCallSucc.getTag(StmtTag.TAG_NAME);
					for (SootMethod mLocalFromCaller : sCallSuccTag.getLocalReachedAppMtds()) {
						// add local reached method, after return
						mTag.addBackwardReachedAppMtd(ProgramFlowGraph.inst().getMethodIdx(mLocalFromCaller));
						
						// add forward reached methods from this local reached method
						MethodTag mAfterRetTag = (MethodTag) mLocalFromCaller.getTag(MethodTag.TAG_NAME);
						mTag.addBackwardReachedAppMtds(mAfterRetTag.getForwardReachedAppMtds());
					}
				}
				
				// add caller site
				mTag.addBackwardReachedCallSite(csIn);
			}
		}
		
		// iteratively propagate backward reachable caller sites and methods from callers
		fixedPoint = false;
		while (!fixedPoint) {
			fixedPoint = true;
			System.out.print(".");
			
			for (SootMethod m : indexedAppMethods) {
				MethodTag mTag = (MethodTag) m.getTag(MethodTag.TAG_NAME);
				for (CallSite csIn : mTag.getCallerSites()) {
					if (csIn.isInCatchBlock())
						continue; // skip catch blocks, FOR NOW
					MethodTag mCallerTag = (MethodTag) csIn.getLoc().getMethod().getTag(MethodTag.TAG_NAME);
					// add new backwards reached methods
					if (mTag.addBackwardReachedAppMtds(mCallerTag.getBackwardReachedAppMtds()))
						fixedPoint = false;
					if (mTag.addBackwardReachedCallSites(mCallerTag.getBackwardReachedCallSites()))
						fixedPoint = false;
				}
			}
		}
		// ensure we don't store backward reachable caller sites whose containing methods are already backward reachable
		for (SootMethod m : indexedAppMethods) {
			MethodTag mTag = (MethodTag) m.getTag(MethodTag.TAG_NAME);
			mTag.removeRedundantBackReachCallSites();
		}
		
		System.out.println();
		
		markReachableFromEntry(entryMethods);
		
		MethodTag.setInterprocReachabilityComputed(true);
	}
	
	/** Includes methods reachable through explicit call graph paths from entry, and clinits of classes with at least one method reached */
	private static void markReachableFromEntry(List<SootMethod> entryMethods) {
		Queue<SootMethod> entries = new LinkedList<SootMethod>();
		entries.addAll(entryMethods);
		
		int reachCount = 0;
		int reachUnits = 0;
		Set<SootClass> processedClinitClasses = new HashSet<SootClass>();
		while (!entries.isEmpty()) {
			SootMethod entry = entries.poll();
			MethodTag entryTag = (MethodTag) entry.getTag(MethodTag.TAG_NAME);
			BitSet bsAllReachableFromEntry = entryTag.getForwardReachedAppMtds();
			
			// explicitly reachable methods
			for (SootMethod m : ProgramFlowGraph.inst().getReachableAppMethods()) {
				final int mId = ProgramFlowGraph.inst().getMethodIdx(m);
				if (m == entry || bsAllReachableFromEntry.get(mId)) {
					MethodTag mTag = (MethodTag) m.getTag(MethodTag.TAG_NAME);
					
					// mark and count only if not already marked and counted
					if (!mTag.isReachableFromEntry()) {
						mTag.markReachableFromEntry();
						assert ProgramFlowGraph.inst().getReachableAppMethods().contains(m);
						++reachCount;
						reachUnits += m.retrieveActiveBody().getUnits().size();
						
						// add implicitly reachable method <clinit> to entries queue, if not processed yet
						try {
							SootClass cls = m.getDeclaringClass();
							if (!processedClinitClasses.contains(cls)) {  // add to queue if not processed uet
								SootMethod mClinit = cls.getMethodByName("<clinit>");
								entries.add(mClinit);
								processedClinitClasses.add(cls);
							}
						}
						catch (RuntimeException e) {} // exception thrown if clinit not found in class
					}
				}
			}
		}
		
		// the following should be equal, except thta PFG includes methods reachable from catch blocks
		assert reachCount <= ProgramFlowGraph.inst().getReachableAppMethods().size();
		int allUnits = 0;
		for (SootMethod m : ProgramFlowGraph.inst().getAppConcreteMethods())
			allUnits += m.retrieveActiveBody().getUnits().size();
		System.out.println("Methods reachable from entry: " + reachCount + "/" + ProgramFlowGraph.inst().getAppConcreteMethods().size()
				+ "; reachable units " + reachUnits + "/" + allUnits);
		int mIdx = 0;
		for (SootMethod m : ProgramFlowGraph.inst().getReachableAppMethods())
			System.out.println("  " + mIdx++ + ": " + m);
	}
	
	/** Looks for to's bottom from from's top (i.e., by convention, a node reaches itself).
	 *  This query is for 'to' stmt's bottom, so if sTo is a call site, all stmts reached during call also reach 'to' */
	public static boolean reachesFromTop(CFGNode nFrom, CFGNode nTo, boolean interproc) {
		if (!isComputed())
			return true;
			
		Stmt sFrom = nFrom.getStmt();
		Stmt sTo = nTo.getStmt();
		StmtTag sFromTag = (StmtTag) sFrom.getTag(StmtTag.TAG_NAME);
		StmtTag sToTag = (StmtTag) sTo.getTag(StmtTag.TAG_NAME);
		
		Location locFrom = sFromTag.getLocation();
		SootMethod toMethod = sToTag.getLocation().getMethod();
		MethodTag mToTag = (MethodTag) toMethod.getTag(MethodTag.TAG_NAME);
		final int toMethodId = ProgramFlowGraph.inst().getMethodIdx(toMethod);
		
		// case: same-method local reachability
		if (locFrom.getMethod() == toMethod) {
			if (sFromTag.getLocalReachedStmts().get(mToTag.getStmtId(sTo)))
				return true;
		}
		if (!interproc)
			return false;
		
		// case: dest in directly called method, for locally reached call
		if (sFromTag.getLocalReachedAppMtds().contains(toMethod))
			return true;
		
		// case: dest in forward reachable methods of directly called methods, for locally reached calls
		for (SootMethod appM : sFromTag.getLocalReachedAppMtds()) {
			MethodTag appMTag = (MethodTag) appM.getTag(MethodTag.TAG_NAME);
			if (appMTag.getForwardReachedAppMtds().get(toMethodId))
				return true;
		}
		
		// case: dest in method reached after return
		MethodTag mFromTag = (MethodTag) locFrom.getMethod().getTag(MethodTag.TAG_NAME);
		if (mFromTag.getBackwardReachedAppMtds().get(toMethodId))
			return true;
		
		// case: dest in same method as and locally reachable from caller site, after return
		for (CallSite csAfter : mFromTag.getBackwardReachedCallSites()) {
			if (csAfter.getLoc().getMethod() == toMethod) {
				StmtTag csAfterTag = (StmtTag) csAfter.getLoc().getStmt().getTag(StmtTag.TAG_NAME);
				if (csAfterTag.getLocalReachedStmts().get(mToTag.getStmtId(sTo)))  // note that this includes csAfterTag itself, if it is sTo
					return true;
			}
		}
		return false;
	}
	/** Looks for to's bottom from from's bottom (i.e., by convention, a node does not reach itself except it it's in a loop/cycle). */
	public static boolean reachesFromBottom(CFGNode nFrom, CFGNode nTo, boolean interproc) {
		if (!isComputed())
			return true;
		
		// look for regular 'from top' reachability for all successors in same CFG or after returning (if 'from' is a ret node)
		if (nFrom.getSuccs().isEmpty()) {
			Stmt sFrom = nFrom.getStmt();
			assert !(sFrom instanceof RetStmt);
			if (interproc && sFrom instanceof ReturnStmt) {
				SootMethod mCaller = ProgramFlowGraph.inst().getContainingMethod(nFrom);
				for (CallSite csIn : ((MethodTag)mCaller.getTag(MethodTag.TAG_NAME)).getCallerSites()) {
					if (!csIn.isReachableFromEntry() || csIn.isInCatchBlock())
						continue; // skip catch blocks, FOR NOW
					if (reachesFromBottom(ProgramFlowGraph.inst().getNode(csIn.getLoc().getStmt()), nTo, interproc))
						return true;
				}
			}
		}
		else
			for (CFGNode nFromSucc : nFrom.getSuccs())
				if (reachesFromTop(nFromSucc, nTo, interproc))
					return true;
		return false;
	}
	
	/** Queries forward reachability at the method level */
	public static boolean forwardReaches(SootMethod mFrom, SootMethod mTo) {
		MethodTag mFromTag = (MethodTag) mFrom.getTag(MethodTag.TAG_NAME);
		final int mToId = ProgramFlowGraph.inst().getMethodIdx(mTo);
		return mFromTag.getForwardReachedAppMtds().get(mToId);
	}
	
}
