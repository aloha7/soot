package edu.cs.hku.instrument;

import edu.cs.hku.util.PathFinder;
import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;
import soot.PackManager;
import soot.Scene;
import soot.SootClass;
import soot.Transform;
import soot.options.Options;


public class InstrumentMain {	
	protected final static String USAGE = 
		"java edu.cs.hku.instrument.InstrumentMain [option] java_to_analyze\n" +
		"option:\n" + 
		"-c      --classpath=String     classpath to load java files and related library\n" +
		"-o      --outputDir=String     directory names to save du-pairs\n" +
		"-h      --help                 print this help message\n\n" +
		"Example: java edu.cs.hku.instrument.InstrumentMain -c c:\\Soot\\bin -O c:\\temp trivia.TestClass\n";
	protected final static String copyright = "(C) copyright 2007-2011 Wang Huai";
	protected final static String hint = "Try 'java edu.cs.hku.testCriteria.DUExtractor -h' for more information";
	
	static String classPath;
	
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
					classPath = g.getOptarg();
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
		
		instrument(classes.split(";"));
	}
	
	private void instrument(String[] classes){
		Options.v().set_keep_line_number(true);
		
		//2009-09-06
		Options.v().setPhaseOption("jb", "use-original-names:true");		
		Options.v().set_time(false);		
		PackManager.v().getPack("jap").add(
				new Transform("jap.instrumenter", new Instrumenter_block()));
		
		//2009-09-08: very important here
		Options.v().set_print_tags_in_output(true);		
		PackManager.v().getPack("tag").add(
				new Transform("tag.myTag", new MyTagAggregator()));		
		Options.v().setPhaseOption("gb.a1", "enabled:false");
		Options.v().setPhaseOption("gb.cf", "enabled:false");
		Options.v().setPhaseOption("gb.a2", "enabled:false");
		Options.v().setPhaseOption("gb.ule", "enabled:false");
		Options.v().setPhaseOption("gop", "enabled:false");
		Options.v().setPhaseOption("bb.lso", "enabled:false");
		Options.v().setPhaseOption("bb.pho", "enabled:false");
		Options.v().setPhaseOption("bb.ule", "enabled:false");
		Options.v().setPhaseOption("bb.lp", "enabled:false");
		Scene.v().addBasicClass("edu.cs.hku.instrument.Probe_block", SootClass.SIGNATURES);
		
		
		for(String className: classes){
			String clzPath = PathFinder.findAbsoluteClassDir(className);
			if(clzPath != null){
				Options.v().set_output_dir(clzPath);
				soot.Main.main(new String[]{className});				
			}			
		}
	}
	
	public static void main(String[] args){
		new InstrumentMain().parse(args);
	}
}
