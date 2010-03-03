package edu.cs.hku.instrument;

import soot.Value;
import soot.jimple.internal.JInvokeStmt;

public class MyInvokeStmt extends JInvokeStmt {
	public MyInvokeStmt(Value v){
		super(v);
	}
	
	
}
