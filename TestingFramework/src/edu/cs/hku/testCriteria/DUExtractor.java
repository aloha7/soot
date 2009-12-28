package edu.cs.hku.testCriteria;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import soot.Body;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.options.Options;
import soot.tagkit.LineNumberTag;
import soot.toolkits.graph.CompleteUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.SimpleLiveLocals;
import soot.toolkits.scalar.SimpleLocalUses;
import soot.toolkits.scalar.SmartLocalDefs;
import soot.toolkits.scalar.UnitValueBoxPair;
import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;

public class DUExtractor implements Cloneable{
	protected final static String USAGE = 
			"java edu.cs.hku.testCriteria.DUExtractor [option] java_to_analyze\n" +
			"option:\n" + 
			"-c      --classpath=String     classpath to load java files and related library\n" +
			"-o      --outputDir=String     directory names to save du-pairs\n" +
			"-h      --help                 print this help message\n\n" +
			"Example: java edu.cs.hku.testCriteria.DUExtractor -c c:\\Soot\\bin -O c:\\temp trivia.TestClass\n";
	protected final static String copyright = "(C) copyright 2007-2011 Wang Huai";
	protected final static String hint = "Try 'java edu.cs.hku.testCriteria.DUExtractor -h' for more information";
	
	
	/** Extract du-pairs for clazz and save these pairs into saveDir.
	 *  The du-pairs are represented by def->uses*. Both defs and uses are represented 
	 *  by their line numbers
	 * @param clazz: class name to analyze
	 * @param saveDir: the directory name to save the du-pairs
	 * @return: def->(use)*. Both defs and uses are represented by their line numbers
	 */
	public HashMap<Integer, HashSet<Integer>> getDUPairs_save(String clazz, String saveDir) {
		
		//def-use*
		HashMap<Integer, HashSet<Integer>> duPairs = new HashMap<Integer, HashSet<Integer>>();

		//1. load the class
		Options.v().set_keep_line_number(true);
		Options.v().setPhaseOption("jb", "use-original-names:true");
		
//		Scene.v().setSootClassPath(Scene.v().getSootClassPath() + ";" 
//				+ "C:\\WangHuai\\Martin\\Eclipse3.3.1\\Franklin_parseTool_100%coverage+random\\Franklin_parserTool\\bin");
		
		Scene.v().loadClassAndSupport(clazz);
		SootClass sClass = Scene.v().getSootClass(clazz);
		sClass.setApplicationClass();

		//2. analysis the intra-data flows in each method
		Iterator<SootMethod> mIt = sClass.getMethods().iterator();
		StringBuilder sb = new StringBuilder();

		while (mIt.hasNext()) {
			SootMethod m = mIt.next();
			Body b = m.retrieveActiveBody();

			UnitGraph graph = new CompleteUnitGraph(b);
			SimpleLiveLocals s = new SimpleLiveLocals(graph);
			SmartLocalDefs des = new SmartLocalDefs(graph, s);
			SimpleLocalUses uses = new SimpleLocalUses(graph, des);
			
			Iterator<Unit> gIt = graph.iterator();
			
			//variable: (defNode->useNode)* for each method
			HashMap<Value, HashSet<DUPair>> dupairs = new HashMap<Value, HashSet<DUPair>>(); 
			
			//generate du-pairs for each method
			while (gIt.hasNext()) {
				Unit defUnit = gIt.next();
				LineNumberTag lnt_def = (LineNumberTag)defUnit.getTag("LineNumberTag");
				if(lnt_def == null){
					continue;
				}
				
				int line_def = lnt_def.getLineNumber();
								
				
				List<UnitValueBoxPair> ul = uses.getUsesOf(defUnit);
				
				if (ul != null && ul.size() != 0) {
					HashSet<Integer> line_uses = duPairs.get(line_def);
					if(line_uses == null){
						line_uses = new HashSet<Integer>();
					}
					
					for (UnitValueBoxPair vbp : ul) {											
						//for return purpose
						LineNumberTag lnt_use = (LineNumberTag)vbp.getUnit().getTag("LineNumberTag");
						if(lnt_use != null){
							line_uses.add(lnt_use.getLineNumber());
						}
						
						
						//for printing purpose
						Value defVariable = vbp.getValueBox().getValue();						
						HashSet<DUPair> dupairList = dupairs.get(defVariable);
						if (dupairList == null) {
							dupairList = new HashSet<DUPair>();
						}
						Unit useUnit = vbp.getUnit();
						dupairList.add(new DUPair(defUnit, useUnit));
						dupairs.put(defVariable, dupairList);						
					}
					
					duPairs.put(line_def, line_uses);
				}
				
			}

			//reorganize du-pairs and print them
			if (dupairs.size() > 0) { // Only methods which have def-use pairs are printed 
				sb.append("\n================================\n");
				sb.append("MethodName:" + m.toString() + "\n");

				//print DU-Chains here
				HashMap<String, HashMap<Unit, HashSet<Unit>>> temp = new HashMap<String, HashMap<Unit, HashSet<Unit>>>();

				for (Value defVariable : dupairs.keySet()) {
					String varStr = defVariable.toString();
					HashMap<Unit, HashSet<Unit>> dus = temp.get(varStr);
					if (dus == null) {
						dus = new HashMap<Unit, HashSet<Unit>>();
					}

					HashSet<DUPair> duList = dupairs.get(defVariable);
					for (DUPair dupair : duList) {
						Unit defUnit = dupair.defUnit;
						HashSet<Unit> useUnits = dus.get(defUnit);
						if (useUnits == null) {
							useUnits = new HashSet<Unit>();
						}
						useUnits.add(dupair.useUnit);
						dus.put(defUnit, useUnits);
					}

					temp.put(varStr, dus);
				}

				int count = 0;
				sb.append("Local Variables:\n");
				String[] vars = temp.keySet().toArray(new String[temp.size()]);
				Arrays.sort(vars);
				for (String var : vars) {
					if (!var.contains("$")) {
						count++;
						sb.append(var + "\n");
					}
				}
				sb.append("Total Variable Num:" + count + "\n\n");

				for (String var : vars) {
					if (!var.contains("$")) { //for any non-intermediate variables

						HashMap<Unit, HashSet<Unit>> dus = temp.get(var);
						sb.append("Variable:" + var + "(" + dus.size() + ")\n");
						for (Unit def : dus.keySet()) {
							HashSet<Unit> _uses = dus.get(def);
							sb.append(def + "(" + def.getTag("LineNumberTag")
									+ ")->(" + _uses.size() + "):");
							for (Unit use : _uses) {
								sb.append(use.getTag("LineNumberTag") + ",");
							}
							sb.append("\n");
						}
						sb.append("\n\n");
					}
				}
			}
		}

		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(
					saveDir + File.separator + clazz));
			bw.write(this.toString(duPairs));
			bw.flush();
			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
//		Scene.v().removeClass(sClass); //release the class for instrumentation purpose
		System.out.println(sb.toString());

		return duPairs;
	}
	
	/**Extract du-pairs for clazz.
	 *  The du-pairs are represented by def->uses*. Both defs and uses are represented 
	 *  by their line numbers
	 * 
	 * @param clazz: class name to analyze
	 * @return
	 */
	public HashMap<Integer, HashSet<Integer>> getDUPairs(String clazz){
		HashMap<Integer, HashSet<Integer>> duPairs = new HashMap<Integer, HashSet<Integer>>();
		
		Options.v().set_keep_line_number(true);
		Options.v().setPhaseOption("jb", "use-original-names:true");// use original variable names	
		
		Scene.v().loadClassAndSupport(clazz);
		SootClass sClass = Scene.v().getSootClass(clazz);
		
		List<SootMethod> methods = sClass.getMethods();
		for(SootMethod m: methods){
			Body b = m.retrieveActiveBody();
			UnitGraph g = new CompleteUnitGraph(b);			
			SimpleLiveLocals s = new SimpleLiveLocals(g);
			SmartLocalDefs des = new SmartLocalDefs(g, s);
			
			Iterator<Unit> uIt = g.iterator();
			while(uIt.hasNext()){
				Unit defUnit = uIt.next();
				LineNumberTag lnt = (LineNumberTag)defUnit.getTag("LineNumberTag");				
				if(lnt == null){
					continue;
				}
				int line_def = lnt.getLineNumber();
				
				
				SimpleLocalUses uses = new SimpleLocalUses(g, des);				
				List<UnitValueBoxPair> ul = uses.getUsesOf(defUnit);
				if(ul != null && ul.size() > 0){
					HashSet<Integer> line_uses = duPairs.get(line_def);
					if(line_uses == null){
						line_uses = new HashSet<Integer>();
					}
					
					for(UnitValueBoxPair uvp: ul){
						LineNumberTag line_dest = (LineNumberTag)uvp.getUnit().getTag("LineNumberTag");
						if(line_dest != null){
							line_uses.add(line_dest.getLineNumber());	
						}
					}
					
					duPairs.put(line_def, line_uses);
				}	
			}
		}
		
//		Scene.v().removeClass(sClass); //release the class for instrumentation purpose 
		return duPairs;
	}
	

	public static String toString(HashMap<Integer, HashSet<Integer>> duPairs) {
		StringBuilder sb = new StringBuilder();
		int total = 0;
		Integer[] defs = duPairs.keySet().toArray(new Integer[duPairs.size()]);
		Arrays.sort(defs);
		for (Integer def : defs) {
			sb.append(def + "->");
			HashSet<Integer> uses_temp = duPairs.get(def);
			Integer[] uses = uses_temp.toArray(new Integer[uses_temp.size()]);
			Arrays.sort(uses);
			for (Integer use : uses) {
				sb.append(use + ",");
				total++;
			}
			sb.append("\n");
		}

		sb.append("Total du-pairs numbers:" + total);
		return sb.toString();
	}

	/** Extract du-pairs for clazz.
	 *  The du-pairs are represented by def->uses*. Both defs and uses are represented 
	 *  by their line numbers
	 * 
	 * @param classes: a string to represent several classes to analyze. Each class is separated by ";"
	 * @return: class -> (duPair)*. Each duPair is represented by def->(use)*. Both defs and uses
	 * are represented by line numbers
	 */
	public HashMap<String, HashMap<Integer, HashSet<Integer>>> getDUPairs_multiClass(String classes){
		HashMap<String, HashMap<Integer, HashSet<Integer>>> clz_DU = 
			new HashMap<String, HashMap<Integer, HashSet<Integer>>>();

		String[] classList  = classes.split(";");
		for(String clz: classList){
			System.out.println("Extracting def-use pairs for " + clz);
			HashMap<Integer, HashSet<Integer>> duPairs = 
				this.getDUPairs(clz);
			clz_DU.put(clz, duPairs);
		}
		return clz_DU;
	}
	
	/**Extract du-pairs for clazz and save these pairs into saveDir.
	 * The du-pairs are represented by def->uses*. Both defs and uses are represented 
	 * by their line numbers
	 * 
	 * @param classes: a string to represent several classes to analyze. Each class is separated by ";"
	 * @param savePath: the directory to save du-pairs for each class
	 * @return:(duPair)*. Each duPair is represented by def->(use)*. Both defs and uses
	 * are represented by line numbers
	 */
	public HashMap<String, HashMap<Integer, HashSet<Integer>>> getDUPairs_save_multiClass(String classes, String savePath){
		String[] classList = classes.split(";");
		HashMap<String, HashMap<Integer, HashSet<Integer>>> clz_DU =
			new HashMap<String, HashMap<Integer, HashSet<Integer>>>();
		
		for(String clz: classList){
			System.out.println("Extracting def-use pairs for " + clz);			
			HashMap<Integer, HashSet<Integer>> duPairs = 
				this.getDUPairs_save(clz, savePath);
			clz_DU.put(clz, duPairs);
		}
		
		return clz_DU;
	}	
	
	public Object cloneDUPairs(HashMap<Integer, HashSet<Integer>> duPairs){
		try {
			HashMap<Integer, HashSet<Integer>> duPairs_copy = (HashMap<Integer, HashSet<Integer>>)super.clone();	
			return duPairs_copy;
		} catch (CloneNotSupportedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new InternalError();
		}
		
	}
	
	/**This method is used to extract dupairs according to the commands.
	 * 
	 * @param args
	 */
	public HashMap<String, HashMap<Integer, HashSet<Integer>>> parse(String[] args) {
		HashMap<String, HashMap<Integer, HashSet<Integer>>> clz_dus = null;
		
		LongOpt[] opts = new LongOpt[]{
				new LongOpt("classpath", LongOpt.REQUIRED_ARGUMENT, null, 'c'),
				new LongOpt("outputFile", LongOpt.REQUIRED_ARGUMENT, null, 'o'),
				new LongOpt("help", LongOpt.NO_ARGUMENT, null, 'h')
		};
		Getopt g = new Getopt("DUPair", args, "c:o:h;", opts);
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
					Scene.v().setSootClassPath(Scene.v().getSootClassPath() + ";"
							+ classPath);
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
		
		if (savePath != null) {
			clz_dus = this.getDUPairs_save_multiClass(classes, savePath);
		} else {
			clz_dus = this.getDUPairs_multiClass(classes);
		}
		
		return clz_dus;
	}

	

	public static void main(String[] args) {
//		args = new String[]{"-o","c:\\","trivia.TestClass"};
//		args = new String[]{"trivia.TestClass"};
		new DUExtractor().parse(args);
//		System.out.println("a");
	}
}
