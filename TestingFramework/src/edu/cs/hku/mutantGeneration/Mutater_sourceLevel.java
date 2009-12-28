package edu.cs.hku.mutantGeneration;

import static edu.cs.hku.util.Constants.FS;
import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;

import java.io.File;

import mujava.ClassMutantsGenerator;
import mujava.MutationSystem;
import mujava.OpenJavaException;
import mujava.TraditionalMutantsGenerator;


public class Mutater_sourceLevel {
	
	protected static final String usage = 
		"Usage: java cs.hku.hk.mutantGeneration.Mutater_sourceLevel OPTION* (CLASS) + \n" +
		"Generate mutant versions for each CLASS \n" + 
		"Example: \njava cs.hku.hk.mutantGeneration.Mutater_sourceLevel \n" +
				"-p C:\\WangHuai\\Martin\\Eclipse3.3.1\\TestingFramework \n" +
				"-s C:\\WangHuai\\Martin\\Eclipse3.3.1\\TestingFramework\\src\\trivia \n" +
				"-l C:\\WangHuai\\Martin\\Eclipse3.3.1\\TestingFramework\\bin\\trivia \n" +
				"-m C:\\WangHuai\\Martin\\Eclipse3.3.1\\TestingFramework\\mutants \n" +
				"-j C:\\WangHuai\\Martin\\Eclipse3.3.1\\TestingFramework\\mutants \n" +
				"-t 111111111111111 \n" +
				"-c 1111111111111111111111111111 \n" +
				"trivia\\Mover.java;trivia\\Animal.java\n\n\n" + 
		
		"  -p, --home_path=String			the directory path of the system \n" +
		"  -s, --src_path=String			the directory path of the source file to be mutated\n" + 
		"  -l, --lib_path=String			the directory path of referred library classes\n" +
		"  -m, --mutant_path=String			the directory path to save mutant versions\n" +
		"  -j, --JUnitTestSet_path=String		the directory path to load JUnit test sets\n" +
		"  -t, --traditional_mutant_operators=boolean[15] the boolean vector to specify which traditional mutant operator is applied(1:apply;0:not apply)\n" +
		"  -c, --classLevel_mutant_operators=boolean[28] the boolean vector to specify which classLevel mutant operator is applied(1:apply;0:not apply)\n" +
		"  -h, --help		print this help message";
	
	protected final static String copyright = "(C) Copyright 2007-2011 Wang Huai";
	protected final static String hint = "Try 'java cs.hku.hk.mutantGeneration --help' for more information";
	
	/**The ClassLevel mutant operators
	 * 
	 */
	protected static String[] cm_operators = {
		"IHI","IHD","IOD","IOP","IOR","ISI","ISD","IPC",  
        "PNC","PMD","PPD","PCI","PCC","PCD","PRV",        
        "OMR","OMD","OAN",                    
        "JTI","JTD","JSI","JSD","JID","JDC",  
        "EOA","EOC","EAM","EMM" 
	};
	
	/**The Traditional mutant operators
	 * 
	 */
	protected static String[] tm_operators = {
		"AORB","AORS","AOIU","AOIS","AODU","AODS",
        "ROR","COR","COD","COI","SOR","LOR","LOI","LOD","ASRS"
	};
	
	/**the directory path of the system
	 * 
	 */
	protected String syshome = null;
	
	/*the directory path of the source file to be mutated*/
	protected String source  = null;
	
	/*the directory path of referred library classes*/
	String classes = null;
	
	/*the directory path to save mutant versions*/
	String result = null;
	
	/*the directory path to load JUnit test sets*/
	String tests = null;
	
	/*the boolean vector to specify which traditional mutant operator is applied(1:apply;0:not apply)*/
	String tradList = null;
	
	/*the boolean vector to specify which classLevel mutant operator is applied(1:apply;0:not apply)*/
	String classList = null;
	
	/*the source files to mutate*/
	String mutateFiles = null;
	
	public boolean[] BitStringToBools(String bits){
		boolean[] res = new boolean[bits.length()];
		for(int i = 0; i < bits.length(); i ++){
			res[i] = (bits.charAt(i) =='1');
		}
		return res;
	}
	
	public String[] BitStringToArray(String bitString, String[] ops){
		boolean[] theBools = BitStringToBools(bitString);
		int cnt = 0;
		for(int i = 0; i < theBools.length; i ++){
			//2009-08-24:wrong:
//			cnt = cnt = (theBools[i] == true ? cnt ++: cnt);
			
			cnt = (theBools[i] == true ? ++cnt: cnt); 
		}
		String[] res = new String[cnt];
		int j = 0;
		for(int i = 0; i < theBools.length; i ++){
			if(theBools[i]){
				res[j] = ops[i];
				j ++;
			}
		}
		return res;
	}
	
	public void deleteDirectory(){
		File originalDir = new File(MutationSystem.MUTANT_HOME + FS + MutationSystem.DIR_NAME +
									FS + MutationSystem.ORIGINAL_DIR_NAME);
		while(originalDir.delete()){
		}
		
		File cmDir = new File(MutationSystem.MUTANT_HOME + FS + MutationSystem.DIR_NAME + FS +
									MutationSystem.CM_DIR_NAME);
		while(cmDir.delete()){
		}
		
		File tmDir = new File(MutationSystem.MUTANT_HOME + FS + MutationSystem.DIR_NAME + FS +
									MutationSystem.TM_DIR_NAME);
		while(tmDir.delete()){
		}
		
		File myHomeDir = new File(MutationSystem.MUTANT_HOME + FS + MutationSystem.DIR_NAME);
		while(myHomeDir.delete()){
		}
	}
	
	public void setMutationSystemPathFor(String file_name){
		String temp;
		temp = file_name.substring(0, file_name.length() - ".java".length());
		temp = temp.replace('/', '.');
		temp = temp.replace('\\', '.');
		int separator_index = temp.lastIndexOf(".");
		
		MutationSystem.CLASS_NAME = (separator_index >=0 ? temp.substring(separator_index + 1, temp.length()): temp);
		
		String mutant_dir_path = MutationSystem.MUTANT_HOME + FS + temp;
		File mutant_path = new File(mutant_dir_path);
		mutant_path.mkdir();
		
		String class_mutant_dir_path = mutant_dir_path +  FS + MutationSystem.CM_DIR_NAME;
		File class_mutant_path = new File(class_mutant_dir_path);
		class_mutant_path.mkdir();
		
		String traditional_mutant_dir_path = mutant_dir_path + FS + MutationSystem.TM_DIR_NAME;
		File traditional_mutant_path = new File(traditional_mutant_dir_path);
		traditional_mutant_path.mkdir();
		
		String original_dir_path = mutant_dir_path + FS + MutationSystem.ORIGINAL_DIR_NAME;
		File original_path = new File(original_dir_path);
		original_path.mkdir();
		
		MutationSystem.CLASS_MUTANT_PATH = class_mutant_dir_path;
		MutationSystem.TRADITIONAL_MUTANT_PATH = traditional_mutant_dir_path;
		MutationSystem.ORIGINAL_PATH = original_dir_path;
		MutationSystem.DIR_NAME = temp;
	}
	
	public void mutate(){
		String[] fileList = mutateFiles.split(";");		
		
		String[] tradops = BitStringToArray(this.tradList, tm_operators);
		String[] classops = BitStringToArray(this.classList, cm_operators);
		
		MutationSystem.setJMutationStructure(syshome);
		
		MutationSystem.recordInheritanceRelation();
		
		try {
			for(int i = 0; i < fileList.length; i ++){
				String file_name = fileList[i];
				
				System.out.println(i + " : " + file_name);
				 // [1] Examine if the target class is interface or abstract class
			      //     In that case, we can't apply mutation testing.
				
				//Generate class name from file_name
				String temp = file_name.substring(0, file_name.length() - ".java".length());
				String class_name = "";
				for(int j = 0; j < temp.length(); j ++){
					if((temp.charAt(j)=='\\') || (temp.charAt(j) == '/')){
						class_name = class_name + ".";
					}else{
						class_name = class_name + temp.charAt(j);
					}
				}
				System.out.println();
				File f = new File(MutationSystem.MUTANT_HOME + FS + class_name + FS + "testresults.mjv");
				System.out.println("Debugging: " + MutationSystem.MUTANT_HOME + FS + class_name + FS + "testresults.mjv");
				while(f.delete());
				
				Class c = Class.forName(class_name);
				int class_type = MutationSystem.getClassType(class_name);
				if(class_type == MutationSystem.NORMAL){
					
				}else if(class_type == MutationSystem.MAIN){
					System.out.println(" -- " + file_name + " class contains 'static void main()' method." );
					System.out.println(" Please note that mutants are not generated for the 'static void main() method'");
				}else{
					switch(class_type){
						case MutationSystem.INTERFACE:
							System.out.println("-- Can't apply because " + file_name + " is 'interface'");
							break;
						case MutationSystem.ABSTRACT:
							System.out.println("-- Can't apply because " + file_name + " is 'abstract' class");
							break;
						case MutationSystem.APPLET:
							System.out.println("-- Can't apply because " + file_name + " is 'applet' class");
							break;						
						case MutationSystem.GUI:
							System.out.println("-- Can't apply because " + file_name + " is 'GUI' class");
							break;
					}
					this.deleteDirectory();
					continue;
				}
				
				 // [2] Apply mutation testing
				this.setMutationSystemPathFor(file_name);
				
				File original_file = new File(MutationSystem.SRC_PATH, file_name);
				
				ClassMutantsGenerator cmGenEngine = new ClassMutantsGenerator(original_file, classops);
				cmGenEngine.makeMutants();
				cmGenEngine.compileMutants();
				
				TraditionalMutantsGenerator tmGenEngine = new TraditionalMutantsGenerator(original_file, tradops);
				tmGenEngine.makeMutants();
				tmGenEngine.compileMutants();
			}
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (OpenJavaException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.out.println("--------------------------");
		System.out.println("Mutants have been created!");
	}
	
	/**Parse command line parameters using GNU GetOpt
	 * 
	 * @param args
	 */
	public void parse(String[] args){
		
		
		LongOpt[] longopts = new LongOpt[]{
			new LongOpt("home_path", LongOpt.REQUIRED_ARGUMENT, null, 'p'),
			new LongOpt("src_path", LongOpt.REQUIRED_ARGUMENT, null, 's'),
			new LongOpt("lib_path", LongOpt.REQUIRED_ARGUMENT, null, 'l'),
			new LongOpt("mutant_path", LongOpt.REQUIRED_ARGUMENT, null, 'm'),
			new LongOpt("JUnitTestSet_path", LongOpt.REQUIRED_ARGUMENT, null, 'j'),
			new LongOpt("traditional_mutant_operators", LongOpt.REQUIRED_ARGUMENT, null, 't'),
			new LongOpt("classLevel_mutant_operators", LongOpt.REQUIRED_ARGUMENT, null, 'c'),
			new LongOpt("help", LongOpt.NO_ARGUMENT, null, 'h')
		};
		Getopt g = new Getopt("Mutater", args, "p:s:l:m:j:t:c:h;", longopts);
		int opt = 0;
		while((opt = g.getopt())!= -1){
			switch(opt){
				case 'p':
					this.syshome = g.getOptarg();
					break;
				case 's':
					this.source = g.getOptarg();
					break;
				case 'l':
					this.classes = g.getOptarg();					
					break;
				case 'm':
					this.mutateFiles = g.getOptarg();
					break;
				case 'j':
					this.tests = g.getOptarg();
					break;
				case 't':
					this.tradList = g.getOptarg();
					break;
				case 'c':
					this.classList = g.getOptarg();
					break;
				case 'h':
				default:
					System.out.println(usage);
					System.exit(0);										
			}
		}
		
		if(args.length <= g.getOptind()){
			System.out.println("no mutant java files are specified\n" + hint);			
		}else{
			this.mutateFiles = args[g.getOptind()];
		}
		this.mutate();
	}
	
	/**
	 * The method which actually performs the mutation on
	 * the java classes.
	 * <p />
	 * Takes the following parameters in a String array:<br />
	 * 0: Full name of the project<br />
	 * 1: Location of the source<br />
	 * 2: Corresonding binaries<br />
	 * 3: Desired output folder<br />
	 * 4: Location of the test source<br />
	 * 5: List of traditional operators to run<br />
	 * 6: List of classlevel operators to run<br />
	 * 7: List of files to be mutated<br />
	 * @param args The parameters for this run. See above.
	 */
	public static void main(String[] args){
		try {
			new Mutater_sourceLevel().parse(args);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
