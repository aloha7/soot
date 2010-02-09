package profile;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

public class TimeReporter {
	/** Set to default for Windows*/
	private static String[] timeCmd = new String[] {"C:\\ARG\\Soot\\workspace\\Subjects\\Tools\\time\\mydate.exe"};
	private static Process pBegin;
	
	public static void begin() throws Exception {
		try { timeCmd = new String[]{(new BufferedReader(new InputStreamReader(new FileInputStream(new File("time.cmd"))))).readLine()}; } catch (Exception e) {}
		pBegin = Runtime.getRuntime().exec(timeCmd);
		pBegin.waitFor();
	}
	
	public static void end() throws Exception {
		Process pEnd = Runtime.getRuntime().exec(timeCmd);
		pEnd.waitFor();
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(pBegin.getInputStream()));
		String timeBegin = reader.readLine();
		reader = new BufferedReader(new InputStreamReader(pEnd.getInputStream()));
		String timeEnd= reader.readLine();
		
		System.out.println("Time taken: " + 
				(Long.valueOf(timeEnd).longValue() - Long.valueOf(timeBegin).longValue()));
	}
	
	/** Although it's not used, this method exists so that the reporter classes are loaded by Soot! */
	public static void __link() {
		BranchReporter.__link();
		DUAReporter.__link();
		PathReporter.__link();
	}
	
}
