package hku.cs.cfg;

import java.util.Iterator;

import soot.Body;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.LookupSwitchStmt;
import soot.options.Options;
import soot.tagkit.LineNumberTag;
import soot.toolkits.graph.Block;
import soot.toolkits.graph.ExceptionalBlockGraph;
import soot.util.Chain;

public class Inspect {
	
	public static void main(String[] args){
//		args = new String[]{"tester.TestClass"};		
		Options.v().set_keep_line_number(true);
//		Options.v().setPhaseOption("jb", "use-original-names:true");
		Scene.v().loadBasicClasses();
		SootClass sClass = Scene.v().loadClassAndSupport(args[0]);
		sClass.setApplicationClass();
		Iterator<SootMethod> methodIt = sClass.getMethods().iterator();
		
		while(methodIt.hasNext()){
			SootMethod m = (SootMethod)methodIt.next();
			Body b = m.retrieveActiveBody();
			
			if (m.isAbstract())
				continue;
			
			System.out.println(b.getLocals());
			
			System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
			System.out.println(m.toString());
			System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n");

			ExceptionalBlockGraph bGraph = new ExceptionalBlockGraph(b);
//			BriefBlockGraph bGraph = new BriefBlockGraph(b);
			
			Iterator<Block> blockIt = bGraph.getBlocks().iterator();
			while(blockIt.hasNext()){
				Block block = (Block)blockIt.next();
				
				Chain<Unit> units = b.getUnits();			
				Iterator<Unit> uIt = units.iterator(block.getHead(), block.getTail());
				
				
				System.out.println("Block "+block.getIndexInMethod()+":");
				System.out.print("[preds:");
				Iterator<Block> bIt = block.getPreds().iterator();
				while(bIt.hasNext())
					System.out.print(" "+bIt.next().getIndexInMethod());
				System.out.print("] [succs:");
				bIt = block.getSuccs().iterator();
				while(bIt.hasNext())
					System.out.print(" "+bIt.next().getIndexInMethod());
				System.out.println("]");
				while(uIt.hasNext()){
					Unit u = uIt.next();
					LineNumberTag lnt = (LineNumberTag)u.getTag("LineNumberTag");
					System.out.print(lnt.getLineNumber()+": "+u.toString());
					if(u instanceof LookupSwitchStmt ){
						System.out.println(((LookupSwitchStmt)u).getLookupValues());
						System.out.println(((LookupSwitchStmt)u).getTargetCount());
						System.out.println(((LookupSwitchStmt)u).getDefaultTarget());
//						System.out.println(((LookupSwitchStmt)u).getFieldRef());
						System.out.println(((LookupSwitchStmt)u).getKey());
						System.out.println(((LookupSwitchStmt)u).getTarget(1));
					}
					System.out.println();
				}
				System.out.println();
			}
			
			
			
		}
	}

}
