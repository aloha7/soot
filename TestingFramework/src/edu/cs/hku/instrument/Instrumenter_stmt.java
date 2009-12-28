package edu.cs.hku.instrument;

import java.util.ArrayList;
import java.util.HashSet;
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
import soot.jimple.ReturnStmt;
import soot.jimple.ReturnVoidStmt;
import soot.tagkit.LineNumberTag;
import soot.toolkits.graph.Block;
import soot.toolkits.graph.BlockGraph;
import soot.toolkits.graph.ExceptionalBlockGraph;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;

public class Instrumenter_stmt extends BodyTransformer {
	SootClass probeClass;
	SootMethod coverProbe, reportProbe;
	
	
	private Block getBlock(Body body, Unit u){
		BlockGraph graph = new ExceptionalBlockGraph(body);
		Iterator<Block> bIt = graph.iterator();
		while(bIt.hasNext()){
			Block b = bIt.next();
			Iterator<Unit> uIt = b.iterator();
			while(uIt.hasNext()){
				if(uIt.next().equals(u)){
					return b;
				}
			}
		}
		return null;
	}
	
	private int getLineNumber(Body b, Unit from){
		UnitGraph g = new ExceptionalUnitGraph(b);
		Iterator<Unit> uIt = g.iterator();
			
		ArrayList<Unit> uList = new ArrayList<Unit>();
		while(uIt.hasNext()){
			uList.add(uIt.next());
		}
		
		for(int i = uList.indexOf(from); i < uList.size() ;i ++){
			if(uList.get(i).getTag("LineNumberTag") != null){
				return Integer.parseInt(uList.get(i).getTag("LineNumberTag").toString());
			}
		}
		
		return -1;
	}

	private int getLineNumber(Body b, Unit from, Unit to){
		UnitGraph g = new ExceptionalUnitGraph(b);		
		List<Unit> units = g.getExtendedBasicBlockPathBetween(from, to);
		Iterator<Unit> uIt = units.iterator();
		while(uIt.hasNext()){
			Unit un = uIt.next();
			
			if(un.getTag("LineNumberTag") != null){
				return Integer.parseInt(un.getTag("LineNumberTag").toString());
			}
		}
		return -1;
	}
	
	@Override
	protected void internalTransform(Body b, String phaseName, Map options) {		
		probeClass = Scene.v().getSootClass("edu.cs.hku.instrument.Probe");
		coverProbe = probeClass.getMethodByName("cover");
		HashSet<Integer> coverLines = new HashSet<Integer>();
		reportProbe = probeClass.getMethodByName("report");
		HashSet<Integer> reportLines = new HashSet<Integer>();
		
		ExceptionalBlockGraph graph = new ExceptionalBlockGraph(b);

		//add the probe to each the end statement of each block
		Iterator<Block> blockIt = graph.iterator();
		while(blockIt.hasNext()){
			Block block = blockIt.next();
			
			Iterator<Unit> uIt = b.getUnits().iterator(
					block.getHead(), block.getTail());

			//get all lineNumbers in this block
			HashSet<Integer> lines = new HashSet<Integer>();
			while(uIt.hasNext()){
				Unit u = uIt.next();
				LineNumberTag lnt = (LineNumberTag)u.getTag("LineNumberTag");
				if(lnt != null && !coverLines.contains(lnt.getLineNumber())){
					lines.add(lnt.getLineNumber());	
				}
			}
			
			//rigth statement to insert probes
			Unit uh = block.getTail();
			if(uh instanceof IdentityStmt){
				for(Integer line: lines){
					coverLines.add(line);
					
					List args = new ArrayList();
					args.add(IntConstant.v(line.intValue()));
					
					InvokeExpr coverExpr = Jimple.v().newStaticInvokeExpr(
							coverProbe.makeRef(), args);
					InvokeStmt coverStmt = Jimple.v().newInvokeStmt(coverExpr);
					
					block.insertAfter(coverStmt, uh);					
				}				
			}else{
				for(Integer line: lines){
					coverLines.add(line);
					
					List args = new ArrayList();
					args.add(IntConstant.v(line.intValue()));
					
					InvokeExpr coverExpr = Jimple.v().newStaticInvokeExpr(
							coverProbe.makeRef(), args);
					InvokeStmt coverStmt = Jimple.v().newInvokeStmt(coverExpr);
					
					block.insertBefore(coverStmt, uh);
				}
			}
			
			//right statement to insert report
			if(uh instanceof ReturnStmt || uh instanceof ReturnVoidStmt){
				int blk = block.getIndexInMethod();
				if(!reportLines.contains(blk)){
					reportLines.add(blk);
					
					InvokeExpr reportExpr = Jimple.v().newStaticInvokeExpr(reportProbe.makeRef());
					InvokeStmt reportStmt = Jimple.v().newInvokeStmt(reportExpr);
					
					block.insertBefore(reportStmt, uh);
					uh.redirectJumpsToThisTo(reportStmt);					
				}
			}
		}
	}
}
