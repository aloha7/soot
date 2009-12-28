package edu.cs.hku.instrument;

import java.io.IOException;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import residue.ListHits;

public class Reporter_StmtCov {
	
	/**
	 * 
	 * @param gretelFile: the full name of gretel files  
	 * @return statement coverage of class under testing
	 */
	public double getCoverageReport(String gretelFile){
		double percentage = 0.0;
		try {
			ListHits.setConfigFile(gretelFile);
			Map<String, Set<Integer>> hits = ListHits.getHits();
			Map<String, Set<Integer>> misses = ListHits.getMisses();
			
			//get coverage
			Map<String, HashSet<Integer>> all = new HashMap();
			Map<String, HashSet<Integer>> covered = new HashMap();
			for(Map.Entry<String, Set<Integer>> entry : hits.entrySet()){
				String file = entry.getKey();
				Set<Integer> lines = entry.getValue();
				
				if(!all.containsKey(file)){
					all.put(file, new HashSet<Integer>());
				}
				all.get(file).addAll(lines);
				if(!covered.containsKey(file)){
					covered.put(file, new HashSet<Integer>());
				}
				covered.get(file).addAll(lines);
			}
			
			for(Map.Entry<String, Set<Integer>> entry: misses.entrySet()){
				String file = entry.getKey();
				Set<Integer> lines = entry.getValue();
				
				if(!all.containsKey(file)){
					all.put(file, new HashSet<Integer>());
				}
				all.get(file).addAll(lines);
			}

			//show the coverage
			int coveredLines_sum = 0;
			int totalLines_sum = 0;
			for(String file: all.keySet()){
				int totalLines = all.get(file).size();
				int coveredLines = covered.containsKey(file)?covered.get(file).size():0;
				System.out.println(file + " covered on " + coveredLines + " of " 
						+  totalLines + " lines");
				coveredLines_sum += coveredLines;
				totalLines_sum += totalLines;
			}
			percentage = ((double)coveredLines_sum)/totalLines_sum;
			System.out.println("Total coverage:" + coveredLines_sum + " of " + totalLines_sum 
					+ " lines ==> " + (new DecimalFormat("0.00").format(percentage)) + " %");
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return percentage; 
	}
	
	
	public static void main(String[] args){
		if(args.length == 0){
			System.out.println("Please specify the .gretel files");			
		}else{
			new Reporter_StmtCov().getCoverageReport(args[0]);	
		}
		
	}
	
	
}
