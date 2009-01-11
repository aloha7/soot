package context.test;


import java.util.Vector;
import java.io.File;
import context.test.util.*;
import context.arch.generator.*;
import context.arch.widget.*;
import java.util.HashMap;



public class OutputProducer {	
	
	
	public void produceOutput(int testSuiteSize, int minVersion, int maxVersion, boolean visual){
		//1.generate a script to invoke ant 
		String script = Constant.baseFolder + "test/script/runTestCase";
		
		this.generateScript(script);
		
		//2.prepare build.xml
		this.generateAntScript(testSuiteSize, minVersion, maxVersion);
		
		//3.invoke ant to produce outputs automatically
		try{
			if(visual){
				//run bat file
				String m_Run = "cmd /c start " + script + ".cmd";			
				Runtime.getRuntime().exec(m_Run);	
			}else{
				//vbs script can stop dos windows from popping up 
				String m_Run = "cmd /c start " + script + ".vbs" ;			
				Runtime.getRuntime().exec(m_Run);	
			}
							
			//2008/7/11: does it need to wait for ant to complete?
			Thread.sleep((120*60)*1000);//120 minutes
			//Thread.sleep(3*1000);
		}catch(Exception e){
			System.out.println(e);
		}		
	}
	
	//2008/7/10: make a build.xml to run test cases
	private void generateAntScript(int testSuiteSize, int minVersion, int maxVersion){		
			Logger log = Logger.getInstance();
			log.setPath(Constant.baseFolder + "build.xml", false);
						
			StringBuilder sb = new StringBuilder();			
			sb.append("<project name=\"contexttoolkit\" default=\"run-TestManager\" basedir=\".\">" + Constant.LINESHIFTER);
			sb.append(" <path id=\"project.class.path\">" + Constant.LINESHIFTER);
			sb.append("  <pathelement location=\"bin\"/>" + Constant.LINESHIFTER);
			sb.append("  <fileset dir=\"lib\">" + Constant.LINESHIFTER);
			sb.append("   <include name=\"**/*.jar\"/>" + Constant.LINESHIFTER);
			sb.append("  </fileset>" + Constant.LINESHIFTER);
			sb.append(" </path>" + Constant.LINESHIFTER);
			
			sb.append(Constant.LINESHIFTER + Constant.LINESHIFTER + Constant.LINESHIFTER);
			
			sb.append(" <target name=\"build\">" + Constant.LINESHIFTER);			
			sb.append("     <mkdir dir=\"bin\"/>" + Constant.LINESHIFTER);
			sb.append("  <javac srcdir=\"src\" destdir=\"bin\" failonerror=\"true\">" + Constant.LINESHIFTER);
			sb.append("  </javac>" + Constant.LINESHIFTER);
			sb.append(" </target>" + Constant.LINESHIFTER);
			
			sb.append(Constant.LINESHIFTER + Constant.LINESHIFTER + Constant.LINESHIFTER);
			
			
			sb.append(" <target name=\"run-TestManager\" depends=\"build\" >" + Constant.LINESHIFTER);
			
			sb.append("     <echo message=\"starting TestManager...\"/> " + Constant.LINESHIFTER);
			for(int i = minVersion; i <= maxVersion; i ++){															
				sb.append("     <echo message=\"start Version " + i + "...\"/> " + Constant.LINESHIFTER);
				sb.append("     <delete dir=\"test/output/" + i + "\"/>" + Constant.LINESHIFTER);
				sb.append("     <mkdir dir=\"test/output/" + i + "\"/>" + Constant.LINESHIFTER);
				for(int j = 0; j < testSuiteSize; j ++){
					sb.append("     <java fork=\"true\" taskname=\"TestManager\"" + Constant.LINESHIFTER);
					sb.append("           classname=\"context.arch.generator.PositionIButton\" output=\"test/output/" + i + "/" + j +".txt\">" + Constant.LINESHIFTER);
					sb.append("       <arg value=\""+j+"\"/>" + Constant.LINESHIFTER);
					sb.append("      <classpath refid=\"project.class.path\"/>" + Constant.LINESHIFTER);
					sb.append("     </java>" + Constant.LINESHIFTER);				
				}												
				
				//make file directory, leave this job to ant 
				//who will not create such a directory if mutants cannot be compiled
				File dir = new File(Constant.baseFolder + "test/output/" + i);
				if(!dir.exists())
					dir.mkdirs();
											
			}
			
			sb.append(" </target>" + Constant.LINESHIFTER);
			
			
			sb.append(Constant.LINESHIFTER + Constant.LINESHIFTER + Constant.LINESHIFTER);			
			
			sb.append("</project>");
			
			log.write(sb.toString());	
			log.close();
	}
	
	/**This script is used to invoke build.xml
	 * 
	 * @param testSuiteSize
	 * @param versionNumber
	 */
	private void generateScript(String path){
			
			
			// vbs script			
			Logger log = Logger.getInstance();
			log.setPath(path + ".vbs", false);
			log.write("Set ws = CreateObject(\"Wscript.Shell\")" + Constant.LINESHIFTER);
			log.write("ws.run \"" + path +" /start\", vbhide");
			log.close();
									
			//cmd script
			log.setPath(path + ".cmd", false);
			log.write("chdir "  + Constant.baseFolder + Constant.LINESHIFTER);
			log.write("ant" + Constant.LINESHIFTER);
			log.write("exit");
			log.close();
			
	}
	
	public static void main(String[] args){
		/*
		OutputProducer prod = new OutputProducer();
		prod.produceOutput(10, 1, 1, true);
		*/
		HashMap map = new HashMap();
		map.put("1", "1");
		map.put("1", "2");
		map.put("1", "3");
	}

}
