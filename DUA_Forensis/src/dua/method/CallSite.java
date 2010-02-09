package dua.method;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import soot.SootMethod;
import soot.Value;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import dua.unit.Location;
import dua.unit.StmtTag;

/** Represents a call site by a location (method, stmt) and the list of all possible target methods */
public class CallSite {
	private Location loc;
	private List<SootMethod> mAppCallees;
	private List<SootMethod> mLibCallees;
	
	private ArrayList<CallSite> csLocalProperDomSet;
	private ArrayList<CallSite> csLocalProperPDomSet;
	
	private BitSet mGlobalEDomSetCall; // at "call" (entry) point of call site
	private BitSet mGlobalEDomSetRet; // at "ret" (exit) point of call site
	private BitSet mGlobalXDomSetCall; // at "call" (entry) point of call site
	private BitSet mGlobalXDomSetRet; // at "ret" (exit) point of call site
	
	private BitSet mGlobalEPDomSetCall; // at "call" (entry) point of call site
	private BitSet mGlobalEPDomSetRet; // at "ret" (exit) point of call site
	private BitSet mGlobalXPDomSetCall; // at "call" (entry) point of call site
	private BitSet mGlobalXPDomSetRet; // at "ret" (exit) point of call site
	
	public Location getLoc() { return loc; }
	public List<SootMethod> getAppCallees() { return mAppCallees; }
	public List<SootMethod> getLibCallees() { return mLibCallees; }
	public List<SootMethod> getAllCallees() {
		ArrayList<SootMethod> allCallees = (ArrayList<SootMethod>) ((ArrayList<SootMethod>) mAppCallees).clone();
		allCallees.addAll(mLibCallees);
		return allCallees;
	}
	public boolean hasAppCallees() { return !mAppCallees.isEmpty(); }
	public boolean hasLibCallees() { return !mLibCallees.isEmpty(); }
	
	public ArrayList<CallSite> getCSLocalProperDomSet() { return csLocalProperDomSet; }
	public void setCSLocalProperDomSet(ArrayList<CallSite> csDomSet) { this.csLocalProperDomSet = csDomSet; }
	public ArrayList<CallSite> getCSLocalProperPDomSet() { return csLocalProperPDomSet; }
	public void setCSLocalProperPDomSet(ArrayList<CallSite> csPDomSet) { this.csLocalProperPDomSet = csPDomSet; }
	
	public BitSet getGlobalEDomSetCall() { return mGlobalEDomSetCall; }
	public void setGlobalEDomSetCall(BitSet domSet) { this.mGlobalEDomSetCall = domSet; }
	public BitSet getGlobalEDomSetRet() { return mGlobalEDomSetRet; }
	public void setGlobalEDomSetRet(BitSet domSet) { this.mGlobalEDomSetRet = domSet; }
	public BitSet getGlobalXDomSetCall() { return mGlobalXDomSetCall; }
	public void setGlobalXDomSetCall(BitSet domSet) { this.mGlobalXDomSetCall = domSet; }
	public BitSet getGlobalXDomSetRet() { return mGlobalXDomSetRet; }
	public void setGlobalXDomSetRet(BitSet domSet) { this.mGlobalXDomSetRet = domSet; }
	
	public BitSet getGlobalEPDomSetCall() { return mGlobalEPDomSetCall; }
	public void setGlobalEPDomSetCall(BitSet pdomSet) { this.mGlobalEPDomSetCall = pdomSet; }
	public BitSet getGlobalEPDomSetRet() { return mGlobalEPDomSetRet; }
	public void setGlobalEPDomSetRet(BitSet pdomSet) { this.mGlobalEPDomSetRet = pdomSet; }
	public BitSet getGlobalXPDomSetCall() { return mGlobalXPDomSetCall; }
	public void setGlobalXPDomSetCall(BitSet pdomSet) { this.mGlobalXPDomSetCall = pdomSet; }
	public BitSet getGlobalXPDomSetRet() { return mGlobalXPDomSetRet; }
	public void setGlobalXPDomSetRet(BitSet pdomSet) { this.mGlobalXPDomSetRet = pdomSet; }
	
	public boolean isInstanceCall() { return loc.getStmt().getInvokeExpr() instanceof InstanceInvokeExpr; }
	public boolean isReachableFromEntry() { return ((MethodTag)loc.getMethod().getTag(MethodTag.TAG_NAME)).isReachableFromEntry(); }
	public boolean isInCatchBlock() { return ((StmtTag)loc.getStmt().getTag(StmtTag.TAG_NAME)).isInCatchBlock(); }
	
	/** Expects lists of methods --NOT ANY LONGER: sorted by global method id-- sorted by string signature. */
	public CallSite(Location loc, List<SootMethod> mAppCallees, List<SootMethod> mLibCallees) {
//		assert !mAppCallees.isEmpty() || !mLibCallees.isEmpty();
		this.loc = loc;
		this.mAppCallees = mAppCallees;
		this.mLibCallees = mLibCallees;
	}
	
	@Override
	public String toString() {
		return "CS( " + loc + " , APP" + mAppCallees + " LIB" + mLibCallees + " )";
	}
	
	/** Returns the actual param by index; 'this' is param 0 on instance calls. */
	public Value getActualParam(int idx) {
		InvokeExpr invExpr = loc.getStmt().getInvokeExpr();
		if (invExpr instanceof InstanceInvokeExpr) {
			if (idx == 0)
				return ((InstanceInvokeExpr)invExpr).getBase();
			return invExpr.getArg(idx - 1); // adjust this-including index to argument index
		}
		return invExpr.getArg(idx);
	}
	
}
