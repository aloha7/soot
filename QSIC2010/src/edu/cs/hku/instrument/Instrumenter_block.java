package edu.cs.hku.instrument;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import soot.Body;
import soot.BodyTransformer;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.IdentityStmt;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.Jimple;
import soot.jimple.StringConstant;
import soot.tagkit.StringTag;
import soot.tagkit.Tag;
import soot.toolkits.graph.Block;
import soot.toolkits.graph.BlockGraph;
import soot.toolkits.graph.ExceptionalBlockGraph;

public class Instrumenter_block extends BodyTransformer {
	SootClass probeClass= null;
	SootMethod coverProbe = null;
	SootMethod transferProbe = null;
	SootMethod reportBlockProbe = null;
	SootMethod reportStmtProbe = null;
	
	//2009-09-06: we need a clear method
	SootMethod clearProbe = null;
	
	public Body removeInstrument(Body b){
		probeClass = Scene.v().getSootClass("edu.cs.hku.instrument.Probe_block");
		clearProbe = probeClass.getMethodByName("clearAll");
		coverProbe = probeClass.getMethodByName("coverBlock");
		
		BlockGraph graph = new ExceptionalBlockGraph(b);
		Iterator<Block> bIt = graph.iterator();
		while(bIt.hasNext()){
			Block block = bIt.next();
			
			Iterator<Unit> uIt = block.iterator();
			
			while(uIt.hasNext()){
				Unit unit = uIt.next();
//				if(unit instanceof MyInvokeStmt){
//					block.remove(unit);
//				}
				
				Tag tag = unit.getTag("MyTag");
				if(tag != null){
					block.remove(unit);					
				}
			}
		}
		return b;
		
	}
	
	@Override
	protected void internalTransform(Body b, String phaseName, Map options) {
		b = this.removeInstrument(b);
		probeClass = Scene.v().getSootClass("edu.cs.hku.instrument.Probe_block");
		
		clearProbe = probeClass.getMethodByName("clearAll");
		coverProbe = probeClass.getMethodByName("coverBlock");		
		transferProbe = probeClass.getMethodByName("blockToStmt");
		reportBlockProbe = probeClass.getMethodByName("reportBlock");
		reportStmtProbe = probeClass.getMethodByName("reportStmt");
				
		BlockGraph graph = new ExceptionalBlockGraph(b);		
		Iterator<Block> bIt = graph.iterator();
		while(bIt.hasNext()){
			Block block = bIt.next();					
			Unit uh = block.getTail(); //rigth statement to insert probe
			
			int blIdx = block.getIndexInMethod();
			
			List args = new ArrayList();
			args.add(StringConstant.v(b.getMethod().getDeclaringClass().getName()));
			args.add(StringConstant.v(b.getMethod().getName()));
			args.add(IntConstant.v(blIdx));
			InvokeExpr coverExpr = Jimple.v().newStaticInvokeExpr(
					coverProbe.makeRef(), args);
//			InvokeStmt coverStmt = new MyInvokeStmt(coverExpr);
			InvokeStmt coverStmt = Jimple.v().newInvokeStmt(coverExpr);
			coverStmt.addTag(new MyCodeAttribute("Instrument"));
			
			
			InvokeExpr clearExpr = Jimple.v().newStaticInvokeExpr(clearProbe.makeRef());
//			InvokeStmt clearStmt = new MyInvokeStmt(clearExpr);
			InvokeStmt clearStmt = Jimple.v().newInvokeStmt(clearExpr);
			clearStmt.addTag(new MyCodeAttribute("Instrument"));
			
			//right place to insert probes 
			if(uh instanceof IdentityStmt){

				//2009-09-06:Each time main method is invoked, we must 
				//clear all history of last executions in the probes
				block.insertAfter(coverStmt, uh);
				
				if(b.getMethod().getName().equals("main") && blIdx == 0){				
					block.insertAfter(clearStmt, uh);
				} //clear first then cover
				
				
			}else{
				if(b.getMethod().getName().equals("main") && blIdx == 0){
					block.insertBefore(clearStmt, uh);
					
					uh.redirectJumpsToThisTo(clearStmt);
				} //clear first then cover
				
				block.insertBefore(coverStmt, uh);
//				uh.redirectJumpsToThisTo(coverStmt);
			}
			
//			//for testing purpose
//			if(uh instanceof ReturnStmt || uh instanceof ReturnVoidStmt){			
//				InvokeExpr reportBlockExpr = Jimple.v().newStaticInvokeExpr(reportBlockProbe.makeRef());
//				InvokeStmt reportBlockStmt = Jimple.v().newInvokeStmt(reportBlockExpr);
//				
//				InvokeExpr transferExpr = Jimple.v().newStaticInvokeExpr(transferProbe.makeRef());
//				InvokeStmt transferStmt = Jimple.v().newInvokeStmt(transferExpr);
//				
//				InvokeExpr reportStmtExpr = Jimple.v().newStaticInvokeExpr(reportStmtProbe.makeRef());
//				InvokeStmt reportStmt = Jimple.v().newInvokeStmt(reportStmtExpr);
//				
//				block.insertBefore(reportBlockStmt, uh);
//				block.insertBefore(transferStmt, uh);
//				block.insertBefore(reportStmt, uh);
//			}
			
		}

	}

}
