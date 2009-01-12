package context.test.util;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.File;

public class Logger {
		
	private static Logger m_Logger;

	private String path;
	
	private static BufferedWriter writer = null;
	
	public static Logger getInstance() {
		if (m_Logger == null) {			
			m_Logger = new Logger();		
		}
		return m_Logger;
	}

	/**
	 * 
	 * @param filename
	 * @param append: false means override, true means append
	 */
	public void setPath(String filename, boolean append) {
		this.path = filename;
		try{
			writer = new BufferedWriter(new FileWriter(path, append));	
		}catch(Exception e){
			System.out.println(e);
		}
	}

	public void write(String log) {
		try {
			if(writer!=null){
				writer.write(log);					
			}			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void close(){
		try{
			if(writer!=null){
				writer.flush();
				writer.close();			
			}	
		}catch(Exception e){
			System.out.println(e);
		}		
	}
	
	public static String generateConfigFile(int testcaseNumber){
	    Logger log = Logger.getInstance();
	    String fileName =Constant.baseFolder + "/Config/ConfigFile_"+testcaseNumber+".txt"; 
		log.setPath(fileName, false);
					
		StringBuilder sb = new StringBuilder();			
		sb.append("<?xml version=\"1.0\"?>\n");
		sb.append("<CONFIGURATION><VERSION>1.0.0</VERSION>\n");
		sb.append("<DESCRIPTION>hello</DESCRIPTION>\n");
		sb.append("<AUTHOR>hwang</AUTHOR>\n");
		sb.append("<PARAMETERS>\n");
		sb.append("   <location>test</location>\n");
		sb.append("  <timestamp>2</timestamp>\n</PARAMETERS>\n");
		sb.append(" <WIDGETS><WIDGET><ID>TourDemo_test</ID>\n");
		sb.append("<HOST>127.0.0.1</HOST>\n");			
		sb.append("   <PORT>"+(6000+testcaseNumber)+"</PORT>\n");
		sb.append("  <TYPE>Widget</TYPE>\n");
		sb.append("  </WIDGET>\n");
		sb.append(" <WIDGET><ID>TourEnd_test</ID>\n");
		sb.append("<HOST>127.0.0.1</HOST>\n");
		sb.append("<PORT>"+(7000 + testcaseNumber)+"</PORT>\n");
		sb.append("<TYPE>Widget</TYPE>\n</WIDGET>\n");
		sb.append("<WIDGET>\n<ID>TourRegistration_test</ID>\n");
		sb.append("<HOST>127.0.0.1</HOST>\n");
		sb.append("     <PORT>"+(5000+ testcaseNumber)+"</PORT>\n<TYPE>Widget</TYPE>\n</WIDGET>\n</WIDGETS>\n");
		sb.append("<SERVERS> <Server><ID>idServer_01020304</ID>\n <HOST>127.0.0.1</HOST>\n<PORT>"+(10000 + testcaseNumber)+"</PORT>\n"
+"<TYPE>Service</TYPE>\n" +"</Server>\n</SERVERS>\n</CONFIGURATION>");
		log.write(sb.toString());	
		log.close();
		return fileName;
  }
 
}
