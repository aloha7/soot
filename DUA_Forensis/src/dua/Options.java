package dua;

import java.util.ArrayList;
import java.util.List;

import file.DirProcessor;

public class Options {
	private static boolean doDuas = true;
	/** Whether to treat parameters and return values as defs/uses, or make locals flow across procedure calls/returns */
	private static boolean allowParmsRetUseDefs = false;
	private static boolean allowThisFlow = false;
	private static boolean allowSameBBDuas = false;
	private static boolean allowPhantomClasses = false;
//	private static boolean allowIdStmtsKills = false;
	private static boolean intraDUAsOnly = false;
	private static boolean localDUAsOnly = false;
	private static String entryClassName = null; // default: find unique 'main' in program
	/** Classes containing test* methods (typically JUnit) */
	private static List<String> entryTestClasses = null; // default: no test classes
	private static String outPath = "";
	private static boolean brInstr = true; // default: br instr is on and uses direct br instrum
	private static boolean countBranches = false;
	private static boolean brSomeProbes = false;
	private static boolean brOptimalInstr = false; // use optimal edge profiling rather that direct br instrum
	private static boolean allBranches = false; // indicates whether to monitor all branches (= true), or only those required by DUAs (= false)
	private static boolean edgeWeighting = true;
	private static boolean eppInstr = false;
	private static int eppDepth = 0; // default: 0 (intra-procedural)
	private static boolean eppCheckBounds = false; // tells whether to insert bounds checking code before array access, for debugging
	private static boolean duaInstrBranches = true;
	private static boolean duaInstrDirect = true;
	private static boolean duaInstrNoProbes = false;
	private static boolean objDUAs = false; // whether to include lib objects as "vars"
	private static boolean hybridInstr = false;
	private static boolean indivCovReg = false;
	private static boolean stmtPairs = false;
	private static boolean reachability = false;
	private static boolean dominance = false;
	private static boolean removeRepeatedBranches = true;
	
	/** Parses argument list (command line args), and gets settings for this program, removing them and returning soot-only args */
	public static String[] parseFilterArgs(String[] args) {
		List<String> argsSoot = new ArrayList<String>();
		DirProcessor dirProc = new DirProcessor();
		// capture, store and filter from args non-Soot arguments
		for (int argIdx = 0; argIdx < args.length; ++argIdx) {
			String arg = args[argIdx];
			
			if (arg.equals("-noduas"))
				doDuas = false;
			else if (arg.equals("-samebbduas"))
				allowSameBBDuas = true;
			else if (arg.equals("-paramdefuses"))
				allowParmsRetUseDefs = true;
			else if (arg.equals("-intraduas")) {
				intraDUAsOnly = true;
				allowParmsRetUseDefs = true;
			}
			else if (arg.equals("-localduas")) {
				localDUAsOnly = true;
			}
			else if (arg.startsWith("-entry:")) {
				entryClassName = new String(arg).substring("-entry:".length());
				System.out.println("Specified main entry class is '" + entryClassName + "'");
			}
			else if (arg.startsWith("-entrytestclasses:")) {
				String sEntryClassesList = new String(arg).substring("-entrytestclasses:".length());
				entryTestClasses = dua.util.Util.parseStringList(sEntryClassesList);
				System.out.println("Entry test classes are " + entryTestClasses);
			}
			else if (arg.equals("-allowthisflow")) {
				allowThisFlow = true;
			}
			else if (arg.startsWith("-brinstr:")) {
				String value = arg.substring("-brinstr:".length());
				if (value.equals("off") || value.equals("none")) {
					brInstr = false;
					duaInstrBranches = false;
				}
				else if (value.equals("count")) {
					countBranches = true;
				}
				else if (value.equals("someprobes")) {
					brSomeProbes = true;
				}
				else if (value.equals("opt")) {
					brOptimalInstr = true;
				}
				else if (value.equals("all")) {
					allBranches = true;
				}
				else
					assert value.equals("on"); // default
			}
			else if (arg.startsWith("-duainstr:")) {
				String duaInstrType = new String(arg).substring("-duainstr:".length());
				if (duaInstrType.equals("off") || duaInstrType.equals("none"))
					duaInstrBranches = duaInstrDirect = false;
				else if (duaInstrType.equals("branch"))
					duaInstrDirect = false;
				else if (duaInstrType.equals("direct"))
					duaInstrBranches = false;
				else if (duaInstrType.equals("noprobes"))
					duaInstrNoProbes = true;
				else
					assert duaInstrType.equals("all") || duaInstrType.equals("on"); // default
			}
			else if (arg.startsWith("-objduas:")) {
				String objDUAsSwitch = new String(arg).substring("-objduas:".length());
				if (objDUAsSwitch.equals("on"))
					objDUAs = true;
				else
					assert objDUAsSwitch.equals("off");
			}
			else if (arg.startsWith("-instrds:")) {
				String dataStruct = new String(arg).substring("-instrds:".length());
				if (dataStruct.equals("indiv"))
					indivCovReg = true;
				else
					assert dataStruct.equals("array"); // default
			}
			else if (arg.equals("-hybinstr")) {
				hybridInstr = true;
			}
			else if (arg.startsWith("-edgeweight:")) {
				String weighting = new String(arg).substring("-edgeweight:".length());
				if (weighting.equals("off"))
					edgeWeighting = false; // disable edge weighting heuristic
				else
					assert edgeWeighting && weighting.equals("on"); // default
			}
			else if (arg.equals("-epp")) {
				eppInstr = true;
			}
			else if (arg.startsWith("-eppdepth:")) {
				eppDepth = Integer.valueOf(arg.substring("-eppdepth:".length()));
			}
			else if (arg.equals("-eppcheckbounds")) {
				eppCheckBounds = true;
			}
			else if (arg.equals("-stmtpairs")) {
				stmtPairs = true;
			}
			else if (arg.equals("-allowphantom")) {
				allowPhantomClasses = true;
			}
			else if (arg.equals("-reachability")) {
				reachability = true;
			}
			else if (arg.equals("-dominance")) {
				dominance = true;
			}
			else if (arg.equals("-keeprepbrs")) {
				removeRepeatedBranches = false;
			}
			else if (arg.equals("-process-dir")) {
				// OVERRIDE of soot option; soot seems to fail with -process-dir in many cases
				dirProc.processDir(args[++argIdx]);
			}
			else {
				if (arg.equals("--help")) {
					System.out.println("DUA Forensic options:");
					System.out.println("  -allowthisflow\tTakes 'this' as formal parameter along which data flows interprocedurally (default: false)");
					System.out.println("  -brinstr:<on|opt|count|someprobes|all|off>\tWhether to instrument for branches (default: on); 'count' increments register instead of setting it");
					System.out.println("  -dominance\tEnables interprocedural dom/pdom analysis (default: false)");
					System.out.println("  -duainstr:<none|branch|direct|noprobes|all>\tType of instrumentation for duas (default: all); brInstr=off disables branch dua instr");
					System.out.println("  -edgeweight:<on|off>\tWhether to perform edge weighting for min-weight edge instrumentation (default: on)");
					System.out.println("  -entry:<class>\tSpecifies the class where to find 'main'");
					System.out.println("  -entrytestclasses:<classlist>\tComma-separated list of classes containint test* methods");
					System.out.println("  -epp\t Enable efficient path profiling");
					System.out.println("  -eppdepth\t Call depth to extend EPP to (default: 0)");
					System.out.println("  -hybinstr\t Hybrid DUA instrumentation (branch + DUA direct) for precise, cheaper DUA monitoring");
					System.out.println("  -instrds:<array|indiv>\tType of coverage data structure: array of elements, or individual elements (default: array)");
					System.out.println("  -noduas\tRuns Soot normally, without computing DUAs at all");
					System.out.println("  -nofieldduas\tDoesn't compute flow-sensitive field DUAs");
					System.out.println("  -noinsensfieldduas\tDoesn't compute flow-insensitive field DUAs");
					System.out.println("  -paramdefuses\tConsiders uses at call site parameters and returns, and defs at method entry parameters (default: false)");
					System.out.println("  -samebbduas\tReports intra-basic-block DUAs (default: false)");
					System.out.println("  -reachability\tEnables interprocedural reachability analysis (default: false)");
				}
				else if (arg.equals("-d")) {
					// soot option; store as data output for Forensic too
					assert argIdx < args.length;
					argsSoot.add(arg); // store -d
					arg = args[++argIdx]; // move arg to next string
					outPath = new String(arg); // copy and store output path
				}
				argsSoot.add(arg);
			}
		}
		
		// add -process-dir classes at the end of soot options
		argsSoot.addAll(dirProc.getClassNames());
		
		// return as raw array
		String[] filteredArgs = new String[argsSoot.size()];
		return argsSoot.toArray(filteredArgs);
	}
	
	public static boolean doDuas() { return doDuas; }
	public static boolean allowSameBBDuas() { return allowSameBBDuas; }
	public static boolean allowParmsRetUseDefs() { return allowParmsRetUseDefs; }
	public static boolean allowThisFlow() { return allowThisFlow; }
//	public static boolean allowIdStmtsKills() { return allowIdStmtsKills; }
	public static boolean intraDUAsOnly() { return intraDUAsOnly; }
	public static boolean localDUAsOnly() { return localDUAsOnly; }
	public static String entryClassName() { return entryClassName; }
	public static String getOutPath() { return outPath; }
	public static boolean brInstr() { return brInstr; }
	public static boolean countBranches() { return countBranches; }
	public static boolean brSomeProbes() { return brSomeProbes; }
	public static boolean brOptimalInstr() { return brOptimalInstr; }
	public static boolean allBranches() { return allBranches; }
	public static boolean eppInstr() { return eppInstr; }
	public static int eppDepth() { return eppDepth; }
	public static boolean eppCheckBounds() { return eppCheckBounds; }
	public static boolean duaInstrBranches() { return duaInstrBranches; }
	public static boolean duaInstrDirect() { return duaInstrDirect; }
	public static boolean anyDuaInstr() { return duaInstrBranches || duaInstrDirect; }
	public static boolean anyInstr() { return brInstr() || anyDuaInstr() || eppInstr(); }
	public static boolean duaInstrNoProbes() { return duaInstrNoProbes; }
	public static boolean includeObjDUAs() { return objDUAs; }
	public static boolean hybridInstr() { return hybridInstr; }
	public static boolean indivCovReg() { return indivCovReg; }
	public static boolean edgeWeighting() { return edgeWeighting; }
	public static boolean stmtPairs() { return stmtPairs; }
	public static boolean allowPhantomClasses() { return allowPhantomClasses; }
	public static boolean reachability() { return reachability; }
	public static boolean dominance() { return dominance; }
	public static List<String> entryTestClasses() { return entryTestClasses; }
	public static boolean removeRepeatedBranches() { return removeRepeatedBranches; }
	
}
