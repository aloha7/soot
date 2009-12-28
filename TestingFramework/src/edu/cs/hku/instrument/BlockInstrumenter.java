package edu.cs.hku.instrument;

import edu.cs.hku.util.PathFinder;
import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import soot.Body;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.baf.Baf;
import soot.baf.BafBody;
import soot.jimple.IdentityStmt;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.Jimple;
import soot.jimple.JimpleBody;
import soot.jimple.StringConstant;
import soot.options.Options;
import soot.tagkit.Tag;
import soot.toolkits.graph.Block;
import soot.toolkits.graph.BlockGraph;
import soot.toolkits.graph.ExceptionalBlockGraph;
import soot.util.JasminOutputStream;

public class BlockInstrumenter {
	
	protected final static String USAGE = 
		"java edu.cs.hku.instrument.BlockInstrumenter [option] java_to_analyze\n" +
		"option:\n" + 
		"-c      --classpath=String     classpath to load java files and related library\n" +
		"-o      --outputDir=String     directory names to save du-pairs\n" +
		"-h      --help                 print this help message\n\n" +
		"Example: java edu.cs.hku.instrument.BlockInstrumenter -c c:\\Soot\\bin -O c:\\temp trivia.TestClass\n";
	protected final static String copyright = "(C) copyright 2007-2011 Wang Huai";
	protected final static String hint = "Try 'java edu.cs.hku.instrument.BlockInstrumenter -h' for more information";
	
	SootClass probeClass= null;
	SootMethod coverProbe = null;
	SootMethod transferProbe = null;
	SootMethod reportBlockProbe = null;
	SootMethod reportStmtProbe = null;
	
	//2009-09-06: we need a clear method
	SootMethod clearProbe = null;
	
	public void parse(String[] args) {		
		LongOpt[] opts = new LongOpt[]{
				new LongOpt("classpath", LongOpt.REQUIRED_ARGUMENT, null, 'c'),
				new LongOpt("outputFile", LongOpt.REQUIRED_ARGUMENT, null, 'o'),
				new LongOpt("help", LongOpt.NO_ARGUMENT, null, 'h')
		};
		Getopt g = new Getopt("InstrumentMain", args, "c:o:h;", opts);
		int opt = 0;
		
		String classes =  null;
		String savePath = null;
		
		//2009-09-05:reset the sootClassPath firstly in case of looking for classes 
		//outside of default classpath while no path is specified by "-c"
		Scene.v().setSootClassPath( System.getProperty("java.class.path") 
				+ ";"+ Scene.v().getSootClassPath());
		
		while((opt = g.getopt())!= -1){
			switch(opt){
				case 'c':
					String classPath = g.getOptarg();
					Scene.v().setSootClassPath(classPath + ";" + Scene.v().getSootClassPath());
					break;
				case 'o':
					savePath = g.getOptarg();
					break;
				case 'h':
					System.out.println(USAGE);
					System.exit(0);
				default:
					System.out.println(hint);
					System.exit(0);						
			}
		}
		
		if(args.length < g.getOptind()){
			System.out.println("no java files are specified!");
		}else{
			classes = args[g.getOptind()];
		}
		
		instrument_classes(classes.split(";"));
//		for(String className: classes.split(";")){
//			this.removeInstrument(className);
//		}
	}
	
	public void removeInstrument(String className){
		Scene.v().loadClassAndSupport(className);
		SootClass sClass = Scene.v().getSootClass(className);

		for(SootMethod m: sClass.getMethods()){
			m.retrieveActiveBody();
			if(m.hasActiveBody()){
				Body b = m.getActiveBody();
				BlockGraph graph = new ExceptionalBlockGraph(b);
				Iterator<Block> bIt = graph.iterator();
				while(bIt.hasNext()){
					Block block = bIt.next();
					Iterator<Unit> uIt = block.iterator();
					
					while(uIt.hasNext()){
						Unit unit = uIt.next();
						Tag tag = unit.getTag("Instrument");
						if(tag != null){
							block.remove(unit);					
						}
					}
				}			
			}
		}
	}
	
	public void instrument_classes(String[] classes){
		
		
		
		for(String className: classes){			
			String savePath = PathFinder.findAbsoluteClassDir(className);
			if(savePath != null){
				instrument(className, savePath);				
			}			
		}
	}
	
	public void instrument(String className, String savePath){
//		soot.G.reset();
		Options.v().set_keep_line_number(true);
//		Options.v().setPhaseOption("jb", "use-original-names:true");// use original variable names
//		Options.v().set_time(false);		
//		Options.v().setPhaseOption("gb.a1", "enabled:false");
//		Options.v().setPhaseOption("gb.cf", "enabled:false");
//		Options.v().setPhaseOption("gb.a2", "enabled:false");
//		Options.v().setPhaseOption("gb.ule", "enabled:false");
//		Options.v().setPhaseOption("gop", "enabled:false");
//		Options.v().setPhaseOption("bb.lso", "enabled:false");
//		Options.v().setPhaseOption("bb.pho", "enabled:false");
//		Options.v().setPhaseOption("bb.ule", "enabled:false");
//		Options.v().setPhaseOption("bb.lp", "enabled:false");
		
		probeClass = Scene.v().loadClassAndSupport("edu.cs.hku.instrument.Probe_block");
		
		clearProbe = probeClass.getMethodByName("clearAll");
		coverProbe = probeClass.getMethodByName("coverBlock");		
		transferProbe = probeClass.getMethodByName("blockToStmt");
		reportBlockProbe = probeClass.getMethodByName("reportBlock");
		reportStmtProbe = probeClass.getMethodByName("reportStmt");
		
		//load the instrumented class 	
		SootClass sClass = Scene.v().loadClassAndSupport(className);
		for(SootMethod m: sClass.getMethods()){
			Body b = m.retrieveActiveBody();
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
				InvokeStmt coverStmt = Jimple.v().newInvokeStmt(coverExpr);
//				coverStmt.addTag(new MyTag("Instrument"));
				
				InvokeExpr clearExpr = Jimple.v().newStaticInvokeExpr(clearProbe.makeRef());
				InvokeStmt clearStmt = Jimple.v().newInvokeStmt(clearExpr);
//				clearStmt.addTag(new MyTag("Instrument"));
				
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
				}
			}	

//			m.setActiveBody(Baf.v().newBody((JimpleBody)b));
			
//			MyTagAggregator mta = new MyTagAggregator();
//			mta.transform(m.getActiveBody());
			
		}
		
		//save the file
		try {
			OutputStream streamOut = new JasminOutputStream(new FileOutputStream(PathFinder.findAbsoluteClassPath(className)));
			PrintWriter writerOut = new PrintWriter(new OutputStreamWriter(streamOut));
			soot.jimple.JasminClass jasminClass = new soot.jimple.JasminClass(sClass);
			jasminClass.print(writerOut);
			writerOut.flush();
			streamOut.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		new BlockInstrumenter().parse(args);
	}

}
