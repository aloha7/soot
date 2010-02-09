package profile;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import soot.Body;
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
import soot.VoidType;
import soot.jimple.IdentityStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.Jimple;
import soot.jimple.NullConstant;
import soot.jimple.RetStmt;
import soot.jimple.ReturnStmt;
import soot.jimple.SpecialInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;
import soot.util.Chain;
import dua.Options;

public class UtilInstrum {
	
	private static SootClass clsSystem = null;
	private static SootField fldSystemOut = null;
	private static SootClass clsPrintStream = null;
	private static SootMethod mtdPrintln = null;
	
	public static SootMethod getCreateClsInit(SootClass cls) {
		try {
			return cls.getMethod("void <clinit>()");
		}
		catch (Exception e) {
			// create clinit
			SootMethod clinit = new SootMethod("<clinit>", new ArrayList(), VoidType.v(), Modifier.STATIC);
	        Body b = Jimple.v().newBody(clinit);
	        b.getUnits().add(Jimple.v().newReturnVoidStmt());
	        
	        clinit.setActiveBody(b);
	        cls.addMethod(clinit);
			
	        return clinit;
		}
	}
	
	public static Local getLocal(Body b, String localName) {
		// look for existing bs local, and return it if found
		Chain locals = b.getLocals();
		for (Iterator itLoc = locals.iterator(); itLoc.hasNext(); ) {
			Local l = (Local)itLoc.next();
			if (l.getName().equals(localName))
				return l;
		}
		
		return null;
	}
	public static Local getCreateLocal(Body b, String localName, Type t) {
		// try getting it
		Local l = getLocal(b, localName);
		if (l != null) {
			assert l.getType().equals(t); // ensure type is correct
			return l;
		}
		// no luck; create it
		Chain locals = b.getLocals();
		l = Jimple.v().newLocal(localName, t);
		locals.add(l);
		return l;
	}
	
	public static Stmt getFirstNonIdStmt(PatchingChain pchain) {
		Stmt sFirstNonId = null;
		for (Iterator it = pchain.iterator(); it.hasNext(); ) {
			sFirstNonId = (Stmt) it.next();
			if (!(sFirstNonId instanceof IdentityStmt))
				break;
		}
		return sFirstNonId;
	}
	
	/**
	 * Inserts code to instantiate a reporter at end of given method, using default ctor (no params)
	 * @param insertionMethod method at the end of which we insert the reporter instantiation code
	 * @param full name of reporter class to instantiate
	 * @return local that points to reporter
	 */
	public static Local insertReporterInstantiateCode(SootMethod insertionMethod, String reporterClassName) {
		List probe = new ArrayList();
		
		// Insert code to instantiate branch reporter at end of entry method. 
		// create local at entry method to hold reporter object
		SootClass clsReporter = Scene.v().getSootClass(reporterClassName);
		Body insertBody = insertionMethod.retrieveActiveBody();
		Local reporterLocal = Jimple.v().newLocal("<" + reporterClassName + ">", clsReporter.getType());
		insertBody.getLocals().add(reporterLocal);
		
		// get default ctor method
		SootMethod repCtorMethod = clsReporter.getMethod("void <init>()");
		
		PatchingChain pchainEntry = insertBody.getUnits();
		Stmt sEntryLast = (Stmt) pchainEntry.getLast();
		
		// instantiate reporter
		//   localRep = new reporterClassName;
		//   specialinvoke localRep.<reporterClassName: void <init>()>();
		probe.clear();
		Stmt newReporterStmt = Jimple.v().newAssignStmt(
				reporterLocal, Jimple.v().newNewExpr(clsReporter.getType()));
		probe.add(newReporterStmt);
		
		Stmt reporterInitStmt = Jimple.v().newInvokeStmt(
				Jimple.v().newSpecialInvokeExpr(reporterLocal, repCtorMethod.makeRef()));
		probe.add(reporterInitStmt);
		
		InstrumManager.v().insertAtProbeTop(pchainEntry, probe, sEntryLast);
		
		return reporterLocal;
	}
	
	/**
	 * Inserts code at END of method to invoke reporter method, using reflection on fields of given class
	 * @param insertionMethod method at whose end we insert report code, passing the class of the method itself as parameter
	 * @param lReporter holds the instance of the reporter object on which to invoke the report method
	 * @param classReporterMethod reporter method, invoked with given local as base, and which receives insertion method's class as parameter
	 */
	public static void insertClassReport(SootMethod insertionMethod, SootClass classToInspect, Local lReporter, SootMethod classReporterMethod) {
		List probe = new ArrayList();
		
		Body insertionBody = insertionMethod.retrieveActiveBody();
		PatchingChain pchain = insertionBody.getUnits();
		Stmt sEntryLast = (Stmt) pchain.getLast();
		
		// WARNING WARNING WARNING WARNING
		// TODO: WARNING -- there might be another return before the end of the method
		//       FOR NOW, check that there is no other return
		for (Iterator itS = pchain.iterator(); itS.hasNext(); ) {
			Stmt s = (Stmt) itS.next();
			assert !(s instanceof ReturnStmt) || !(s instanceof RetStmt) || s == sEntryLast;
		}
		// WARNING WARNING WARNING WARNING
		
		// Get insertion class Class runtime object, to pass it to report method:
		//   localCls = null
		// label0:
		//   localCls = staticinvoke <java.lang.Class forName(String)>(INSERTION_CLS_NAME)
		// label1:
		//   goto label3
		// label2:
		//   local1 := @caughtexception;
		//   local2 := local1;
		// label3:
		//   virtualinvoke localRep.<void classReporterMethod(Class)>(localCls)
		// ...
		// catch java.lang.Exception from label0 to label1 with label2;
		
		String reporterClsSuffix = classReporterMethod.getDeclaringClass().getName();
		SootClass classCls = Scene.v().getSootClass("java.lang.Class");
		Local lCls = Jimple.v().newLocal("<cls_" + reporterClsSuffix + ">", RefType.v(classCls));
		insertionBody.getLocals().add(lCls);
		// need to init local to something (null) before entering "try" zone (label0 to label1), since Class.forName might throw an exception,
		// so class loader won't complain about a possibly uninitialized use of lCls later at the virtualinvoke
		Stmt sInitClsLocal = Jimple.v().newAssignStmt(lCls, NullConstant.v());
		probe.add(sInitClsLocal);
		
		// label1:
		SootMethod clsFromNameMtd = classCls.getMethod("java.lang.Class forName(java.lang.String)");
		Stmt sGetEntryClass = Jimple.v().newAssignStmt(lCls, 
				Jimple.v().newStaticInvokeExpr(clsFromNameMtd.makeRef(), StringConstant.v(classToInspect.getName())));
		probe.add(sGetEntryClass);
		
		// label3:
		Stmt reportCallStmt = Jimple.v().newInvokeStmt(
				Jimple.v().newVirtualInvokeExpr(lReporter, classReporterMethod.makeRef(), lCls));
		final int reportCallProbeIdx = probe.size();
		probe.add(reportCallStmt);
		
		// insert trap (catch) handler before report call, but after goto
		// label2:
		SootClass excCls = Scene.v().getSootClass("java.lang.Exception");
		Local lException1 = Jimple.v().newLocal("<ex1_" + reporterClsSuffix + ">", RefType.v(excCls));
		Local lException2 = Jimple.v().newLocal("<ex2_" + reporterClsSuffix + ">", RefType.v(excCls));
		insertionBody.getLocals().add(lException1);
		insertionBody.getLocals().add(lException2);
		Stmt sCatch = Jimple.v().newIdentityStmt(lException1, Jimple.v().newCaughtExceptionRef());
		Stmt sExcCopy = Jimple.v().newAssignStmt(lException2, lException1);
		probe.add(reportCallProbeIdx, sExcCopy);
		probe.add(reportCallProbeIdx, sCatch);
		
		InstrumManager.v().insertAtProbeBottom(pchain, probe, sEntryLast);
		
		// insert goto to report call, before catch handler
		// label1:
		probe.clear();
		Stmt sGotoReport = Jimple.v().newGotoStmt(reportCallStmt);
		probe.add(sGotoReport);
		
		InstrumManager.v().insertAtProbeRightBefore(pchain, probe, sEntryLast, sCatch);
		
		// insert trap (catch)
		// end of method:
		insertionBody.getTraps().add(Jimple.v().newTrap(excCls, sGetEntryClass, sGotoReport, sCatch));
	}
	
	/** Takes all ctors from superclass, and creates identical-signature ctors in class; bodies just call superclass' respective ctor */
	public static void duplicateSuperCtors(SootClass cls) {
		// create constructors in class
		for (Object oMSuper : cls.getSuperclass().getMethods()) {
			SootMethod mSuper = (SootMethod) oMSuper;
			if (mSuper.getName().equals("<init>")) {
				assert mSuper.getReturnType().equals(VoidType.v());
				SootMethod mNewCtor = new SootMethod("<init>", mSuper.getParameterTypes(), VoidType.v(), mSuper.getModifiers(), mSuper.getExceptions());
				Body bCtor = Jimple.v().newBody(mNewCtor);
				mNewCtor.setActiveBody(bCtor);
				cls.addMethod(mNewCtor);
				PatchingChain pchain = bCtor.getUnits();
				
				// add id stmt for 'this'
				Local lThis = Jimple.v().newLocal("lThis", cls.getType());
				bCtor.getLocals().add(lThis);
				Stmt sThisParamInit = Jimple.v().newIdentityStmt(
						lThis, Jimple.v().newThisRef(cls.getType()));
				pchain.add(sThisParamInit);
				
				// add id stmts for params
				int localIdx = 0;
				for (Object oFormal : mSuper.getParameterTypes()) {
					Type t = (Type) oFormal;
					Local lParam = Jimple.v().newLocal("l"+localIdx, t);
					bCtor.getLocals().add(lParam);
					Stmt sFormalParamInit = Jimple.v().newIdentityStmt(
							lParam, Jimple.v().newParameterRef(t, localIdx));
					pchain.add(sFormalParamInit);
				}
				
				// call super ctor
				Stmt sSuperCall = Jimple.v().newInvokeStmt(
						Jimple.v().newSpecialInvokeExpr((Local)bCtor.getThisLocal(), mSuper.makeRef()));
				pchain.add(sSuperCall);
				
				// add return stmt
				Stmt sReturn = Jimple.v().newReturnVoidStmt();
				pchain.add(sReturn);
			}
		}
	}
	
	/** Finds calls at ctors in cls to old super ctors, and redirects them to current (new) superclass ctors */
	public static void redirectCtors(SootClass cls, SootClass oldSuperClass) {
		SootClass clsSuper = cls.getSuperclass();
		for (Object oM : cls.getMethods()) {
			SootMethod m = (SootMethod) oM;
			// for every <init> in method
			if (m.getName().equals("<init>")) {
				assert m.getReturnType().equals(VoidType.v());
				
				// find stmt that calls <init> on old superclasss, and redirect call to current superclass' corresponding ctor (same signature, but new superclass)
				Body b = m.retrieveActiveBody();
				PatchingChain pchain = b.getUnits();
				for (Iterator itS = pchain.iterator(); itS.hasNext(); ) {
					Stmt s = (Stmt) itS.next();
					try {
						InvokeExpr invExpr = s.getInvokeExpr();
						if (invExpr instanceof SpecialInvokeExpr && invExpr.getMethodRef().declaringClass() == oldSuperClass) {
							SpecialInvokeExpr spInvExp = (SpecialInvokeExpr) invExpr;
							SootMethodRef tgtMtdRef = spInvExp.getMethodRef();
							if (tgtMtdRef.name().equals("<init>") && spInvExp.getBase() == b.getThisLocal()) {
								// find superclass' corresponding ctor
								SootMethod mSuperCtor = null;
								for (Object oSuperM : clsSuper.getMethods()) {
									SootMethod mSuper = (SootMethod) oSuperM;
									if (mSuper.getSubSignature().equals(tgtMtdRef.getSubSignature().getString())) {
										mSuperCtor = mSuper;
										break;
									}
								}
								assert mSuperCtor != null;
								
								// update <init> call to new superclass
								spInvExp.setMethodRef(mSuperCtor.makeRef());
								break;
							}
						}
					}
					catch (RuntimeException e) {}
				}
			}
		}
	}
	
	public static List createPrintlnCode(Body b, String msg) {
		List printCode = new ArrayList();
		
		if (clsSystem == null) {
			clsSystem = Scene.v().getSootClass("java.lang.System");
			fldSystemOut = clsSystem.getFieldByName("out");
			clsPrintStream = Scene.v().getSootClass("java.io.PrintStream");
			mtdPrintln = clsPrintStream.getMethod("void println(java.lang.String)");
		}
		
		Local lOut = getCreateOutLocal(b);
		Stmt sOutToLocal = Jimple.v().newAssignStmt(lOut, Jimple.v().newStaticFieldRef(fldSystemOut.makeRef()));
		printCode.add(sOutToLocal);
		Stmt sCallPrintln = Jimple.v().newInvokeStmt(Jimple.v().newVirtualInvokeExpr(lOut, mtdPrintln.makeRef(), StringConstant.v(msg)));
		printCode.add(sCallPrintln);
		
		return printCode;
	}
	
	private static final String OUT_LOCAL_NAME = "<prstrm_out>";
	private static Local getCreateOutLocal(Body b) {
		Local lOut = getLocal(b, OUT_LOCAL_NAME);
		if (lOut == null) {
			lOut = Jimple.v().newLocal(OUT_LOCAL_NAME, clsPrintStream.getType());
			b.getLocals().add(lOut);
		}
		return lOut;
	}
	
}
