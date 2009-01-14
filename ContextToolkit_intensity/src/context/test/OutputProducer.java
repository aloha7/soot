package context.test;

import java.util.Vector;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import context.test.util.*;
import context.arch.generator.*;
import context.arch.widget.*;
import java.util.HashMap;

public class OutputProducer {

	public void produceOutput(int testSuiteSize, int minVersion,
			int maxVersion, boolean visual) {
		// 1.generate a script to invoke ant
		String script = Constant.baseFolder + "test/script/runTestCase";
		this.generateScript(script);

		// 2.prepare build.xml
		this.generateAntScript(testSuiteSize, minVersion, maxVersion);

		// 3.invoke ant to produce outputs automatically
		try {
			if (visual) {
				// run bat file
				String m_Run = "cmd /c start " + script + ".cmd";
				Runtime.getRuntime().exec(m_Run);
			} else {
				// vbs script can stop dos windows from popping up
				String m_Run = "cmd /c start " + script + ".vbs";
				Runtime.getRuntime().exec(m_Run);
			}

			// 2008/7/11: does it need to wait for ant to complete?
			// Thread.sleep((3*60)*1000);//120 minutes
			// Thread.sleep((3*60)*1000);
		} catch (Exception e) {
			System.out.println(e);
		}
	}

	// 2008/7/10: make a build.xml to run test cases
	// private void generateAntScript(int testSuiteSize, int minVersion, int
	// maxVersion){
	// Logger log = Logger.getInstance();
	// log.setPath(Constant.baseFolder + "build.xml", false);
	//						
	// StringBuilder sb = new StringBuilder();
	// sb.append("<project name=\"contexttoolkit\" default=\"run-TestManager\"
	// basedir=\".\">" + Constant.LINESHIFTER);
	// sb.append(" <path id=\"project.class.path\">" + Constant.LINESHIFTER);
	// sb.append(" <pathelement location=\"bin\"/>" + Constant.LINESHIFTER);
	// sb.append(" <fileset dir=\"lib\">" + Constant.LINESHIFTER);
	// sb.append(" <include name=\"**.jar\"/>" + Constant.LINESHIFTER);
	// sb.append(" </fileset>" + Constant.LINESHIFTER);
	// sb.append(" </path>" + Constant.LINESHIFTER);
	//			
	// sb.append(Constant.LINESHIFTER + Constant.LINESHIFTER +
	// Constant.LINESHIFTER);
	//			
	// sb.append(" <target name=\"build\">" + Constant.LINESHIFTER);
	// sb.append(" <mkdir dir=\"bin\"/>" + Constant.LINESHIFTER);
	// sb.append(" <javac srcdir=\"src\" destdir=\"bin\" failonerror=\"true\">"
	// + Constant.LINESHIFTER);
	// sb.append(" </javac>" + Constant.LINESHIFTER);
	// sb.append(" </target>" + Constant.LINESHIFTER);
	//			
	// sb.append(Constant.LINESHIFTER + Constant.LINESHIFTER +
	// Constant.LINESHIFTER);
	//			
	//			
	// sb.append(" <target name=\"run-TestManager\" depends=\"build\" >" +
	// Constant.LINESHIFTER);
	//			
	// sb.append(" <echo message=\"starting TestManager...\"/> " +
	// Constant.LINESHIFTER);
	// for(int i = minVersion; i <= maxVersion; i ++){
	// sb.append(" <echo message=\"start Version " + i + "...\"/> " +
	// Constant.LINESHIFTER);
	// sb.append(" <delete dir=\"test/output/" + i + "\"/>" +
	// Constant.LINESHIFTER);
	// sb.append(" <mkdir dir=\"test/output/" + i + "\"/>" +
	// Constant.LINESHIFTER);
	// for(int j = 0; j < testSuiteSize; j ++){
	// sb.append(" <java fork=\"true\" taskname=\"TestManager\"" +
	// Constant.LINESHIFTER);
	// sb.append(" classname=\"context.arch.generator.PositionIButton\"
	// output=\"test/output/" + i + "/" + j +".txt\">" + Constant.LINESHIFTER);
	// sb.append(" <arg value=\""+j+"\"/>" + Constant.LINESHIFTER);
	// sb.append(" <classpath refid=\"project.class.path\"/>" +
	// Constant.LINESHIFTER);
	// sb.append(" </java>" + Constant.LINESHIFTER);
	// }
	//				
	// //make file directory, leave this job to ant
	// //who will not create such a directory if mutants cannot be compiled
	// File dir = new File(Constant.baseFolder + "test/output/" + i);
	// if(!dir.exists())
	// dir.mkdirs();
	//											
	// }
	//			
	// sb.append(" </target>" + Constant.LINESHIFTER);
	//			
	//			
	// sb.append(Constant.LINESHIFTER + Constant.LINESHIFTER +
	// Constant.LINESHIFTER);
	//			
	// sb.append("</project>");
	//			
	// log.write(sb.toString());
	// log.close();
	// }

	// 2009/1/14: it is not necessary to compile the version again while Ant is
	// only used to redirect the output
	private void generateAntScript(int testSuiteSize, int minVersion,
			int maxVersion) {
		Logger log = Logger.getInstance();
		log.setPath(Constant.baseFolder + "build.xml", false);

		StringBuilder sb = new StringBuilder();
		sb
				.append("<project name=\"contexttoolkit\" default=\"run-TestManager\" basedir=\".\">"
						+ Constant.LINESHIFTER);
		sb.append(" <path id=\"project.class.path\">" + Constant.LINESHIFTER);
		sb.append("  <pathelement location=\"bin\"/>" + Constant.LINESHIFTER);
		sb.append("  <fileset dir=\"lib\">" + Constant.LINESHIFTER);
		sb.append("   <include name=\"**/*.jar\"/>" + Constant.LINESHIFTER);
		sb.append("  </fileset>" + Constant.LINESHIFTER);
		sb.append(" </path>" + Constant.LINESHIFTER);

		sb.append(Constant.LINESHIFTER + Constant.LINESHIFTER
				+ Constant.LINESHIFTER);

		sb.append(" <target name=\"run-TestManager\">" + Constant.LINESHIFTER);
		sb.append("     <echo message=\"starting TestManager...\"/> "
				+ Constant.LINESHIFTER);
		for (int i = minVersion; i <= maxVersion; i++) {
			sb.append("     <echo message=\"start Version " + i + "...\"/> "
					+ Constant.LINESHIFTER);
			sb.append("     <delete dir=\"test/output/" + i + "\"/>"
					+ Constant.LINESHIFTER);
			sb.append("     <mkdir dir=\"test/output/" + i + "\"/>"
					+ Constant.LINESHIFTER);
			for (int j = 0; j < testSuiteSize; j++) {
				sb.append("     <java fork=\"true\" taskname=\"TestManager\""
						+ Constant.LINESHIFTER);
				sb
						.append("           classname=\"context.arch.generator.PositionIButton\" output=\"test/output/"
								+ i
								+ "/"
								+ j
								+ ".txt\">"
								+ Constant.LINESHIFTER);
				// 2009/1/14: the first argument--versionNumber;
				// second--testCaseNumber
				sb.append("       <arg value=\"" + i + "\"/>"
						+ Constant.LINESHIFTER);
				sb.append("       <arg value=\"" + j + "\"/>"
						+ Constant.LINESHIFTER);
				sb.append("      <classpath refid=\"project.class.path\"/>"
						+ Constant.LINESHIFTER);
				sb.append("     </java>" + Constant.LINESHIFTER);
			}

			// make file directory, leave this job to ant
			// who will not create such a directory if mutants cannot be
			// compiled
			File dir = new File(Constant.baseFolder + "test/output/" + i);
			if (!dir.exists())
				dir.mkdirs();

		}

		sb.append(" </target>" + Constant.LINESHIFTER);

		sb.append(Constant.LINESHIFTER + Constant.LINESHIFTER
				+ Constant.LINESHIFTER);

		sb.append("</project>");

		log.write(sb.toString());
		log.close();
	}
	
	
	/**
	 * This script is used to invoke build.xml
	 * 
	 * @param testSuiteSize
	 * @param versionNumber
	 */
	private void generateScript(String path) {

		// vbs script
		Logger log = Logger.getInstance();
		log.setPath(path + ".vbs", false);
		log.write("Set ws = CreateObject(\"Wscript.Shell\")"
				+ Constant.LINESHIFTER);
		log.write("ws.run \"" + path + " /start\", vbhide");
		log.close();

		// cmd script
		log.setPath(path + ".cmd", false);
		log.write("chdir " + Constant.baseFolder + Constant.LINESHIFTER);
		log.write("ant" + Constant.LINESHIFTER);
		log.write("exit");
		log.close();

	}

	public void runVersions(int minVersion, int maxVersion, int testSetSize){
		for(int versionNum = minVersion; versionNum <=maxVersion; versionNum++){
			this.runVersion(versionNum, testSetSize);
		}
	}
	
	//2009/1/15: in order to concurrently execute all these faulty versions, we designed two other approaches, however, it seems
	//it does not work at all.
	/**
	 * 
	 * @param versionNumber: 0 is the golden version
	 * @param testCaseSize: 
	 */
	public void runVersion(int versionNumber, int testSetSize){
		try {
			for(int testCaseNumber = 0; testCaseNumber < testSetSize; testCaseNumber ++){
//				//1.generate a .bat file to invoked
//				Logger log = Logger.getInstance();
//				log.setPath(Constant.baseFolder + "test/script/runScript.bat", false);
//				
//				StringBuilder sb = new StringBuilder();
//				sb.append("chdir " + Constant.baseFolder + "bin" + "\n");
//				sb.append("java context.arch.generator.PositionIButton " + versionNumber 
//				+ " " + testCaseNumber + ">" + Constant.baseFolder + "test/output/" + versionNumber + "/" + testCaseNumber + ".txt");
//				log.write(sb.toString());
//				log.close();
//				//2. run the .bat
//				Runtime run = Runtime.getRuntime();
//				String cmd = Constant.baseFolder +"test/script/runScript.bat";
//				Process proc = run.exec(cmd);
//				
//				InputStream inputstream =
//	                proc.getInputStream();
//	            InputStreamReader inputstreamreader =
//	                new InputStreamReader(inputstream);
//	            BufferedReader bufferedreader =
//	                new BufferedReader(inputstreamreader);
//	    
//	    
//	            String line;
//	            while ((line = bufferedreader.readLine()) 
//	                      != null) {
//	                System.out.println(line);
//	            }
//	            try {
//	                if (proc.waitFor() != 0) {
//	                    System.err.println("exit value = " +
//	                        proc.exitValue());
//	                }
//	            }
//	            catch (InterruptedException e) {
//	                System.err.println(e);
//	            }

	            
//	            //3. An alternative method
	            String[] cmds = new String[4];
	            cmds[0] = "C:\\Java\\JDK5.0\\bin\\java.exe";
	            cmds[1] = "context.arch.generator.PositionIButton";
	            cmds[2] = "" + versionNumber;
	            cmds[3] ="" + testCaseNumber;
	            ProcessBuilder pb = new ProcessBuilder(cmds);
	            pb.directory(new File(Constant.baseFolder + "bin"));
	            Process proc1 = pb.start();
	            InputStream inputstream1 =
	                proc1.getInputStream();
	            InputStreamReader inputstreamreader1 =
	                new InputStreamReader(inputstream1);
	            BufferedReader bufferedreader1 =
	                new BufferedReader(inputstreamreader1);
	    
	    
	            String line1;
	            StringBuilder sb = new StringBuilder();
	            
	            while ((line1 = bufferedreader1.readLine()) 
	                      != null) {
	            	sb.append(line1+"\n");
	            }
	            
	            Logger logger = Logger.getInstance();
	            logger.setPath(Constant.baseFolder + "test/output/" + versionNumber + "/" + testCaseNumber + ".txt", false);
	            logger.write(sb.toString());
	            logger.close();
	            try {
	                if (proc1.waitFor() != 0) {
	                    System.err.println("exit value = " +
	                        proc1.exitValue());
	                }
	            }
	            catch (InterruptedException e) {
	                System.err.println(e);
	            }
	            
	            
//				cmd = "C:\\Java\\JDK5.0\\bin\\java.exe";
//				cmd = "C:\\Java\\JDK5.0\\bin\\java.exe -cp "+ Constant.baseFolder + "bin" +"context.arch.generator.PositionIButton " + versionNumber 
//				+ " " + i;
//				proc = run.exec(cmd);
				
				
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	
	public static void main(String[] args) {

		OutputProducer prod = new OutputProducer();
		// prod.generateAntScript(100, 0, 0);
//		prod.produceOutput(100, 0, 1, true);
		long start = System.currentTimeMillis();
		prod.runVersions(1, 5, 100);
		System.out.println("Run Time:" + "\n" + (System.currentTimeMillis()- start));
//		prod.runVersion(0, 10);
	}

}
