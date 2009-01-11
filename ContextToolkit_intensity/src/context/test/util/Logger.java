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
}
