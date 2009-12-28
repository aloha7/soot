package edu.cs.hku.srcStatistics;

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javancss.Main;

public class JStatistics {
	protected final static String USAGE = 
		"Usage: java edu.cs.hku.SrcStatistics.JStatistics [option]  *.java* | <stdin> \n" +
		"Example: java edu.cs.hku.SrcStatistics.JStatistics -a C:\\WangHuai\\Martin\\Eclipse3.3.1\\TestingFramework\\src\\trivia\\*.java \n\n"+
		"option\n" +
		"-n --ncss:        Counts the program NCSS (default).\n" +
		"-p --package:     Assembles a statistic on package level.\n" +
		"-o --object:      Counts the object NCSS.\n" +
		"-f --function:    Counts the function NCSS.\n" +
		"-a --all:         The same as '-function -object -package'.\n" +
		"-g --gui:         Opens a gui to present the '-all' output in tabbed panels.\n"+
		"-x --xml:         Output in xml format.\n"+
		"-O --Out:         Output file name. By default output goes to standard out.\n"+
		"-r --recursive:   Recurse to subdirs.\n" +
		"-c --check:       Triggers a javancss self test.\n"+
		"-e --encoding:    Encoding used while reading source files (default: platform encoding).\n"+
		"-P --parser15:    Use new experimental Java 1.5 parser.\n" +
		"-v --version:     Prints the version number of the program.\n" +
		"-h --help:        Prints this help message.\n" +
		"-d --debug:       Prints debugging information while running.\n" +
		"-i --inifile:     Starts this application with another ini file than \n" +
					  "\t\t\tthe default one.\n";
	
	public void parse(String[] args){
		LongOpt[] longopts = new LongOpt[]{
				new LongOpt("ncss", LongOpt.NO_ARGUMENT, null, 'n'),
				new LongOpt("package", LongOpt.NO_ARGUMENT, null, 'p'),
				new LongOpt("object", LongOpt.NO_ARGUMENT, null, 'o'),
				new LongOpt("function", LongOpt.NO_ARGUMENT, null, 'f'),
				new LongOpt("all", LongOpt.NO_ARGUMENT, null, 'a'),
				new LongOpt("gui", LongOpt.NO_ARGUMENT, null, 'g'),
				new LongOpt("xml", LongOpt.NO_ARGUMENT, null, 'x'),
				new LongOpt("out", LongOpt.REQUIRED_ARGUMENT, null, 'O'),
				new LongOpt("recursive", LongOpt.NO_ARGUMENT, null, 'r'),
				new LongOpt("check", LongOpt.NO_ARGUMENT, null, 'c'),
				new LongOpt("encoding", LongOpt.NO_ARGUMENT, null, 'e'),
				new LongOpt("parser15", LongOpt.NO_ARGUMENT, null, 'P'),
				new LongOpt("version", LongOpt.NO_ARGUMENT, null, 'v'),
				new LongOpt("help", LongOpt.NO_ARGUMENT, null, 'h'),
				new LongOpt("debug", LongOpt.NO_ARGUMENT, null, 'd'),
				new LongOpt("inifile", LongOpt.NO_ARGUMENT, null, 'i'),
		};
		Getopt g = new Getopt("JStatistics", args, "npofagxO:rcepvhdi", longopts);
		int opt = 0;
		Set<String> argList = new HashSet<String>(); 
		while((opt = g.getopt())!= -1){
			switch(opt){
				case 'n':
					argList.add("-ncss");
					break;
				case 'p':
					argList.add("-package");
					break;
				case 'o':
					argList.add("-object");
					break;
				case 'f':
					argList.add("-function");
					break;
				case 'a':
					argList.add("-all");
					break;
				case 'g':
					argList.add("-gui");
					break;
				case 'x':
					argList.add("-xml");
					break;
				case 'O':
					argList.add("-out " + g.getOptarg());
					break;
				case 'r':
					argList.add("-recursive");
					break;
				case 'c':
					argList.add("-check");
					break;
				case 'e':
					argList.add("-encoding");
					break;
				case 'P':
					argList.add("-parser15");
					break;
				case 'v':
					argList.add("-version");
					break;
				case 'h':
					System.out.println(USAGE);
					System.exit(1);
				case 'd':
					argList.add("-debug");
					break;
				case 'i':
					argList.add("-inifile");
					break;
				default:
					System.out.println(USAGE);
					System.exit(1);
			}
		}
		if(args.length <= g.getOptind()){
			System.out.println("Please specify .java files to do statistics");
			System.exit(1);
		}
		
		for(int i = g.getOptind(); i < args.length;  i ++){
			argList.add(args[i]);
		}
		
		String[] asArgs = argList.toArray(new String[argList.size()]);
		
		try {
			Main.main(asArgs);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new JStatistics().parse(args);
	}

}
