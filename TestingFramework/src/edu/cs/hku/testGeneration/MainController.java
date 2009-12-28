package edu.cs.hku.testGeneration;



import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;

import java.lang.reflect.Array;
import java.util.LinkedHashSet;
import java.util.Set;

public class MainController {
	protected static String usage = "Usage: java cs.hku.hk.testGeneration OPTION* (CLASS|PACKAGE)+\n" + 
	"Generate JUnit test case sources for every CLASS and all classes within\n" +
	"every PACKAGE and their sub-packages.\n" +
	"Example: java cs.hku.hk.testGeneration.Generator p1.C p2\n\n" +

	"  -e, --execute        execute test cases while generating to suppress boring ones\n" +
	"  -d, --depth=INT      maximal depth of method chaining (default 3)\n" +
"  -f, --files=INT      maximal nr of test files created (default 4000)\n" +
	"  -h, --help           print these instructions\n" +
	"  -j, --junitFiltering make generated test cases extend FilteringTestCase\n" +
	"  -l, --log            generate detailed log\n" +		
	"  -o, --outdir=DIR     where JCrasher writes test case sources to (default .)\n" +
"  -s, --suppressNull   do not include any null literals in generated test cases.\n" +
	"  -v, --version        print version number\n";
	
	protected static String hint =
		"Try `java cs.hku.hk.testGeneration --help' for more information.";
	
	public Class<?>[] parse(String[] args){
//		Class<?> clazzes = new Class<?>();
		LongOpt[] options = new LongOpt[]{
				new LongOpt("execute", LongOpt.NO_ARGUMENT, null, 'e'),
				new LongOpt("depth", LongOpt.REQUIRED_ARGUMENT, null, 'd'),
				new LongOpt("files", LongOpt.REQUIRED_ARGUMENT, null, 'f'),
				new LongOpt("help", LongOpt.NO_ARGUMENT, null, 'h'),
				new LongOpt("junitFiltering", LongOpt.NO_ARGUMENT, null, 'j'),
				new LongOpt("log", LongOpt.NO_ARGUMENT, null, 'l'),
				new LongOpt("outdir", LongOpt.REQUIRED_ARGUMENT, null, 'o'),
				new LongOpt("suppressNull", LongOpt.NO_ARGUMENT, null, 's'),
				new LongOpt("version", LongOpt.NO_ARGUMENT, null, 'v'),
		};
		Getopt g = new Getopt("Generator 1", args, "ed:f:hjlo:sv;", options);
		int opt = 0;
		while((opt = g.getopt())!= -1){
			switch(opt){
				case 'e':
					
				case 'd':
					
				case 'f':
					
				case 'h':
					System.out.println(usage);
					break;
				case 'j':
					
				case 'l':
					
				case 'o':
					
				case 's':
					
				case 'v':
					
				default:
					break;
			}
		}
		
		if(args.length <= g.getOptind()){
			die("No classes are specified");
		}
		
		String[] classesFromUser = new String[args.length - g.getOptind()];
		System.arraycopy(args, g.getOptind(), classesFromUser, 0, classesFromUser.length);
		
		return parseClasses(classesFromUser);
	}
	
	public Class<?>[] parseClasses(String[] classNames){
		Set<Class<?>> clazzes = new LinkedHashSet<Class<?>>();
		for(String className: classNames){
			
			try {
				clazzes.add(Class.forName(className));
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		return  clazzes.toArray(new Class[clazzes.size()]);
	}
	
	public void die(String msg){
		System.out.println(msg + "\n" + hint + "\n");
		exit();
	}
	
	public void exit(){
		System.exit(0);
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		MainController gen = new MainController();
		Class<?>[] classes = gen.parse(args);
		
		ClassAnalyzer analyzer = new ClassAnalyzer(classes);
		analyzer.crawl();
		
	}

}
