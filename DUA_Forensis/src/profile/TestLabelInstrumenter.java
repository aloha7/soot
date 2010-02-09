package profile;

import java.util.ArrayList;
import java.util.List;

import soot.Body;
import soot.Local;
import soot.PatchingChain;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.jimple.Jimple;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;
import dua.Options;

/** Inserts at the beginning of each 'test*' method code to print a string identifying test case.
 *  Excludes "entry" method. */
public class TestLabelInstrumenter {
	
	public void instrument(List<SootMethod> entryMethods) {
		List<Stmt> probe = new ArrayList<Stmt>();
		
		// get access to System.out field
		SootClass clsSystem = Scene.v().getSootClass("java.lang.System");
		SootClass clsPrintStream = Scene.v().getSootClass("java.io.PrintStream");
		Type printStreamType = clsPrintStream.getType();
		SootField fldSysOut = clsSystem.getField("out", printStreamType);
		SootMethod mPrintln = clsPrintStream.getMethod("void println(java.lang.String)");
		
		SootMethod mEntryToExclude = (Options.entryClassName() == null)? null : entryMethods.get(0);
		for (SootMethod m : entryMethods) {
			if (m == mEntryToExclude)
				continue;
			
			Body b = m.retrieveActiveBody();
			PatchingChain<Unit> pchain = b.getUnits();
			probe.clear();
			
			Local lSysOut = UtilInstrum.getCreateLocal(b, "<sysout>", printStreamType);
			Stmt sGetSysOutToLocal = Jimple.v().newAssignStmt(lSysOut, Jimple.v().newStaticFieldRef(fldSysOut.makeRef()));
			probe.add(sGetSysOutToLocal);
			Stmt sCallPrintln = Jimple.v().newInvokeStmt(Jimple.v().newVirtualInvokeExpr(lSysOut, mPrintln.makeRef(),
					StringConstant.v("Test: " + m.getName() + "(" + m.getDeclaringClass().getName() + ")")));
			probe.add(sCallPrintln);
			
			InstrumManager.v().insertRightBeforeNoRedirect(pchain, probe, (Stmt) UtilInstrum.getFirstNonIdStmt(pchain));
		}
	}
	
}
