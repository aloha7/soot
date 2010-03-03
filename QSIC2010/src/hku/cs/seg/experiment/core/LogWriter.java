package hku.cs.seg.experiment.core;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class LogWriter {
	private BufferedWriter m_BufferedWriter;
	
	public void init(String filename) {
		try {
			m_BufferedWriter = new BufferedWriter(new FileWriter(filename));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void write(String str) {
		try {
			m_BufferedWriter.write(str);
			flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void writeln(String str) {
		write(str + "\n");
	}
	
	public void writeln() {
		write("\n");
	}
	
	public void flush() {
		try {
			m_BufferedWriter.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	@Override
	protected void finalize() throws Throwable {
		try {			
			m_BufferedWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	protected LogWriter() {
		
	}
	
	private static LogWriter Me = null;
	public static LogWriter me() {
		if (Me == null) {
			Me = new LogWriter();
		}
		return Me;
	}
}
