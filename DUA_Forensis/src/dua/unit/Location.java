package dua.unit;

import soot.SootMethod;
import soot.jimple.IdentityStmt;
import soot.jimple.Stmt;

/** Location in the program given by method, stmt pair */
public class Location {
	private SootMethod m;
	private Stmt s;
	public Location(SootMethod m, Stmt s) {
		this.m = m;
		this.s = s;
	}
	public SootMethod getMethod() {
		return m;
	}
	public Stmt getStmt() {
		return s;
	}
	
	public boolean isParameterDefLoc() { return s instanceof IdentityStmt; }
	
	@Override
	public String toString() {
		return "LOC(" + m + ", " + s + ")";
	}
}
