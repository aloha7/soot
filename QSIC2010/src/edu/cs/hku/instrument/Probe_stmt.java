package edu.cs.hku.instrument;

import java.util.Arrays;
import java.util.HashMap;

public class Probe_stmt {
	
	/**
	 * lineNumber -> cover times
	 */
	private static HashMap<Integer, Integer> hitTable = new HashMap<Integer, Integer>();
	
	private static Probe_stmt instance = null;
	
	public static Probe_stmt getInstance(){
		if(instance == null){
			instance = new Probe_stmt();					
		}
		return instance;
	}
	
	private Probe_stmt(){
		hitTable = new HashMap<Integer, Integer>();
	}
	
	public static void cover(int lineNumber){
		Integer coverTimes = hitTable.get(lineNumber);
		if(coverTimes == null){
			coverTimes = 0;
		}
		coverTimes ++;
		hitTable.put(lineNumber, coverTimes);
	}
	
	

	public static String report(){
		StringBuilder sb = new StringBuilder().append("Coverage report: LineNumber:CoverTimes\n");
		
		Integer[] lineNumbers = hitTable.keySet().toArray(new Integer[hitTable.keySet().size()]);
		Arrays.sort(lineNumbers);
		
		for(Integer lineNumber: lineNumbers){			
			sb.append(lineNumber + ":" + hitTable.get(lineNumber) + "\n");			
		}
		
		System.out.println(sb.toString());
		
		return sb.toString();
	}
}
