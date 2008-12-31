package hku.cs.instrument;

import hku.cs.tools.DirTools;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import soot.Body;
import soot.Local;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.VoidType;
import soot.jimple.AssignStmt;
import soot.jimple.IdentityStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.JasminClass;
import soot.jimple.Jimple;
import soot.jimple.ReturnStmt;
import soot.jimple.ReturnVoidStmt;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;
import soot.options.Options;
import soot.util.Chain;
import soot.util.JasminOutputStream;

public class TestCaseInstrument {
	

	 SootClass counterClass;
	 SootMethod reportCounter;
	
	public void modifyTests(String[] theTests, String outputDir, String outputBinDir) throws FileNotFoundException, IOException{
				
		Options.v().set_keep_line_number(true);
		Scene.v().addBasicClass("hku.cs.instrument.Counter", SootClass.SIGNATURES);
		Scene.v().addBasicClass("junit.framework.TestCase");
		Scene.v().addBasicClass("java.lang.Object");
		Scene.v().addBasicClass("java.lang.Class");
		Scene.v().loadBasicClasses();
		SootClass testcase = Scene.v().getSootClass("junit.framework.TestCase");
		SootClass object = Scene.v().getSootClass("java.lang.Object");
		SootClass klass = Scene.v().getSootClass("java.lang.Class");
		
		counterClass = Scene.v().getSootClass("hku.cs.instrument.Counter");
		reportCounter = counterClass.getMethod("void report(java.lang.String,java.lang.String,java.lang.String)");
		
		for(String strSc : theTests){
			SootClass sc = Scene.v().loadClassAndSupport(strSc);
			if (sc.hasSuperclass()&&sc.getSuperclass().equals(testcase)){

				List<SootMethod> ms = sc.getMethods();
				for(SootMethod sm : ms)
					sm.retrieveActiveBody();
				
				SootMethod m = null;
				try{
					m = sc.getMethod("tearDown", new ArrayList(0), VoidType.v());
				}catch(RuntimeException e){
					
				}finally{

					Body body;
					Boolean hasM = false;
					if (m != null){
						hasM = true;
					}
					if (!hasM){
						m = new SootMethod("tearDown",                 
						        new ArrayList(0),
						        VoidType.v(), Modifier.PROTECTED);
						body = Jimple.v().newBody(m);
					    m.setActiveBody(body);
					    sc.addMethod(m);
					}else{
						body = m.retrieveActiveBody();
					}
					    
					
				    Local tmpRef1;
				    
				    if(!hasM){
				    	tmpRef1 = Jimple.v().newLocal("tmpRef1", sc.getType());
		                body.getLocals().add(tmpRef1);
	                }else{
	                	tmpRef1 = body.getLocals().getFirst();
	                }
	                
	                
	                IdentityStmt is = Jimple.v().newIdentityStmt(tmpRef1,
	                        Jimple.v().newThisRef(RefType.v(m.getDeclaringClass())));
					
					Local arg2, arg3;
					
					arg2 = Jimple.v().newLocal("l0", RefType.v("java.lang.String"));
	                body.getLocals().add(arg2);	
	                arg3 = Jimple.v().newLocal("l1", RefType.v("java.lang.String"));
	                body.getLocals().add(arg3);                
 
	                Local tmpClass;
	                
	                tmpClass = Jimple.v().newLocal("tmpClass", RefType.v("java.lang.Class"));
	                body.getLocals().add(tmpClass);
	                
	                
	                SootMethod toCall1 = object.getMethodByName("getClass");
	                AssignStmt tmpAssign = Jimple.v().newAssignStmt(tmpClass,
	        				Jimple.v().newVirtualInvokeExpr
	                        (tmpRef1, toCall1.makeRef(), new ArrayList(0)));
	                
	                
	                SootMethod toCall2 = klass.getMethodByName("toString");
	                AssignStmt arg2Assign = Jimple.v().newAssignStmt(arg2,
	        				Jimple.v().newVirtualInvokeExpr
	                        (tmpClass, toCall2.makeRef(), new ArrayList(0)));
	                
	                
	                
	                SootMethod toCall3 = testcase.getMethodByName("getName");
	                AssignStmt arg3Assign = Jimple.v().newAssignStmt(arg3,
	                				Jimple.v().newVirtualInvokeExpr
	                                (tmpRef1, toCall3.makeRef(), new ArrayList(0)));
	                
	                
	                List margs = new ArrayList();
	                margs.add(StringConstant.v(outputDir));
	                margs.add(arg2);
	                margs.add(arg3);
	                
	                InvokeExpr incExpr = Jimple.v().newStaticInvokeExpr(
	    					reportCounter.makeRef(), margs);
	    			InvokeStmt insStmt = Jimple.v().newInvokeStmt(incExpr);
	    			
	    			Chain<Unit> us = body.getUnits();
	    			if(!hasM){
		    			us.add(is);
		    			us.add(tmpAssign);
		    			us.add(arg2Assign);
		    			us.add(arg3Assign);
		    			us.add(insStmt);
		    			us.add(Jimple.v().newReturnVoidStmt()); 
	    			}else{
						List<Unit> ls = new ArrayList<Unit>(4);
						ls.add(tmpAssign);
						ls.add(arg2Assign);		    			
		    			ls.add(arg3Assign);
		    			ls.add(insStmt);
		    			
		    			Iterator stmtIt = body.getUnits().snapshotIterator();

		    			while (stmtIt.hasNext())
		    		    {
		    		        Stmt s = (Stmt) stmtIt.next();
		    				if (s instanceof ReturnStmt 
		    	                     || s instanceof ReturnVoidStmt)
		    					us.insertBefore(ls, s);
		    			}
					}
					
//					String fileName = SourceLocator.v().getFileNameFor(sc, Options.output_format_class);
//			        OutputStream streamOut = new JasminOutputStream(
//			                                    new FileOutputStream(fileName));

					String name = sc.getName();
					name = name.replace('.', File.separatorChar);
					String outputBinFile = outputBinDir+name;
					DirTools.prepare(outputBinFile.substring(0, outputBinFile.lastIndexOf(File.separatorChar)));
					File fz = new File(outputBinFile+".class");
			        OutputStream streamOut = new JasminOutputStream(
			                                    new FileOutputStream(fz));
			        System.out.println("write result file to " + fz.toString());
			        
			        PrintWriter writerOut = new PrintWriter(
			                                    new OutputStreamWriter(streamOut));
			        JasminClass jasminClass = new soot.jimple.JasminClass(sc);
			        jasminClass.print(writerOut);
			        writerOut.flush();
			        streamOut.close();
			        
			        Scene.v().removeClass(sc);

				}
			}
		}
		
				
	}
	

}


