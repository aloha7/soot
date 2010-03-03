package edu.cs.hku.instrument;

import static edu.cs.hku.util.Constants.FS;
import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import residue.Instrumenter;

public class InstrumentCtrller_StmtCov {
	protected final static String usage= 
		"Usage: java cs.hku.hk.testCoverage.Instrumenter OPTION* -d directory (CLASS)\n" +
		"Instrument each Class to collect statement coverage information \n" +
		"Example: \n java java cs.hku.hk.testCoverage.Instrumenter -d C:\\p C:\\p\\example\\Sorter.class\n\n" +
		"-b --breakloops \n" +
		"-n --noreduce   \n" +
		"-d --Gretel_dir    the directory to save gretel files\n" +
		"-h --help          print this help message";
	
	public void parse(String[] args){
		try {
			LongOpt[] longopts = new LongOpt[]{
					new LongOpt("breakloops", LongOpt.NO_ARGUMENT, null, 'b'),
					new LongOpt("noreduce", LongOpt.NO_ARGUMENT, null, 'n'),
					new LongOpt("Gretel_dir", LongOpt.REQUIRED_ARGUMENT, null, 'd'),
					new LongOpt("Help", LongOpt.NO_ARGUMENT, null, 'h')
			};
			Getopt g = new Getopt("Instrumenter", args, "bnd:h;", longopts);
			Instrumenter in = new Instrumenter();
			int opt = 0;
			
			String gretel_path = "";		
			while((opt = g.getopt())!= -1){
				switch(opt){
					case 'b':
						in.setBreakLoops(true);
						break;
					case 'n':
						in.setReduceNodes(false);
						break;
					case 'h':
						System.out.println(usage);
						System.exit(1);
					case 'd':
						File gretel_dir = new File(g.getOptarg());
						if(!gretel_dir.exists()){
							System.out.println(usage);
							System.exit(1);
						}
						gretel_path = gretel_dir.getCanonicalPath() + FS;
						break;
					default:
						in.setBreakLoops(true);
						in.setBreakLoops(false);
						break;
				}
			}
			
			Set classes = new HashSet(); //classes to be instrumented
			if(args.length <= g.getLongind()){
				System.out.println("no classes are specified to instrument");
				System.out.println(usage);
				System.exit(1);
			}else{
				String[] temp = new String[args.length - g.getOptind()];
				System.arraycopy(args, g.getOptind(), temp, 0, temp.length);
				for(int i = 0; i < temp.length; i ++){
					if(!new File(temp[i]).exists()){
						//second try: cancat the path of class files with the gretel path
						temp[i] = gretel_path + temp[i];
						if(! new File(temp[i]).exists()){
							System.out.println(temp[i] + 
									" does not exist, please specify it with absolute path");
							System.out.println(usage);
							System.exit(1);
						}
					}
				}
				classes.addAll(Arrays.asList(temp));
			}
			in.instrument(classes, gretel_path);
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args){
		new InstrumentCtrller_StmtCov().parse(args);
	}
		
}
