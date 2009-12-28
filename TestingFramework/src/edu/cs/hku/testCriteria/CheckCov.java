package edu.cs.hku.testCriteria;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

public class CheckCov {
	
	/**This method returns a set of du-pairs(and corresponding covering times)covered in this execution, 
	 * given a set of du-pairs of a java class and a execution history of a test case. Note that this method
	 * did not modify inputs 
	 * 
	 * @param duPairs: du-pairs of a java class(represented as: def-use*)
	 * @param exeHistory: execution history of a test case(represented as: lineNum-> coverTimes)
	 * @return a set of du-pairs(and corresponding covering times)covered in this execution(represented as: def-(use,coverTimes)*)
	 */
	public static HashMap<Integer, HashMap<Integer, Integer>> checkCoverage(HashMap<Integer, HashSet<Integer>> duPairs, 
			HashMap<Integer, Integer> exeHistory){
		HashMap<Integer, HashMap<Integer, Integer>> coverage = new HashMap<Integer, HashMap<Integer, Integer>>();
		
		for(Integer def_line: duPairs.keySet()){			
			if(exeHistory.containsKey(def_line)){ // if the def_line is executed
				for(Integer use_line: duPairs.get(def_line)){
					if(exeHistory.containsKey(use_line)){ // if the use_line also is executed						
						HashMap<Integer, Integer> use_covers = coverage.get(def_line);
						if(use_covers == null){
							use_covers = new HashMap<Integer, Integer>();
						}
						use_covers.put(use_line, exeHistory.get(use_line)); //record the cover times of this use
						coverage.put(def_line, use_covers); //record the cover times of this du-pair
					}
				}
			}
		}		
		return coverage;
	}
	
	/**This method differs from checkCoverage only in that it delete covered du-pairs from duPairs
	 * 
	 * @param duPairs: du-pairs of a java class(represented as: def-use*)
	 * @param exeHistory: execution history of a test case(represented as: lineNum -> coverTimes)
	 * @return: a set of du-pairs(and corresponding covering times)covered in this execution(represented as: def-(use,coverTimes)*)
	 */
	public static HashMap<Integer, HashMap<Integer, Integer>> modifyCoverage(HashMap<Integer, HashSet<Integer>> duPairs, 
			HashMap<Integer, Integer> exeHistory){
		
		HashMap<Integer, HashMap<Integer, Integer>> coverage = checkCoverage(duPairs, exeHistory);
		
		for(Integer def_line: coverage.keySet()){			
			HashMap<Integer, Integer> use_hits = coverage.get(def_line); // all uses covered by execution
			HashSet<Integer> uses = duPairs.get(def_line); //all uses of def_line
			
			for(Integer use_line: use_hits.keySet()){
				uses.remove(use_line);
			}
			
			if(uses.size() == 0){ //no uses fro def_line any more
				duPairs.remove(def_line);
			}			
		}
		
		return coverage;
	}

	
	/**This method gets covered du-pairs(and the cover times) for each class checkCoverage and it 
	 * deletes covered du-pairs from duPairs accordingly
	 * 
	 * @param duPairs: du-pairs of a java class(represented as: def-use*)
	 * @param exeHistory: execution history of a test case(represented as: lineNum-> coverTimes)
	 * @return: For each class, a set of its du-pairs(and corresponding covering times)covered
	 *  in this execution(represented as: className -> (def-> (use,coverTimes)*)*)
	 */
	public static HashMap<String, HashMap<Integer, HashMap<Integer, Integer>>> modifyCoverage_multiClass(
			HashMap<String, HashMap<Integer, HashSet<Integer>>> clz_dus, 
			HashMap<String, HashMap<Integer, Integer>> clz_lineCovers){
		
		HashMap<String, HashMap<Integer, HashMap<Integer, Integer>>> clz_du_hits = new 
		 HashMap<String, HashMap<Integer, HashMap<Integer, Integer>>>();
		
		for(String clz: clz_dus.keySet()){
			if(clz_lineCovers.containsKey(clz)){
				HashMap<Integer, HashSet<Integer>> duPairs = clz_dus.get(clz);
				HashMap<Integer, Integer> exeHistory = clz_lineCovers.get(clz);
				clz_du_hits.put(clz,modifyCoverage(duPairs, exeHistory));				
			}
		}
		
		return clz_du_hits;
	}

	
	public static String report(HashMap<Integer, HashMap<Integer, Integer>> coverage){
		StringBuilder sb = new StringBuilder();

		Integer[] def_lines =  coverage.keySet().toArray(new Integer[coverage.size()]);
		Arrays.sort(def_lines);
		for(Integer def_line: def_lines){
			HashMap<Integer, Integer> use_Covers = coverage.get(def_line);
			Integer[] use_lines = use_Covers.keySet().toArray(new Integer[use_Covers.size()]);
			Arrays.sort(use_lines);
			for(Integer use_line: use_lines){
				sb.append(def_line).append("\t").append(use_line).append("\t").
				append(use_Covers.get(use_line)).append("\n");				
			}
		}
		
		return sb.toString();		
	}
	
	public static String report_multiClass(HashMap<String, HashMap<Integer, HashMap<Integer, Integer>>> clz_du_hits){
		StringBuilder sb = new StringBuilder();
		
		boolean printHeader = true;
		
		for(String clz: clz_du_hits.keySet()){
			sb.append("class:").append(clz).append("\n");
			if(printHeader){
				sb.append("Def").append("\t").append("Use").append("\t").append("CoverTimes").append("\n");
				printHeader = false;
			}
			
			sb.append(report(clz_du_hits.get(clz))).append("\n");			
		}
		String result = sb.toString();
		System.out.println(result);
		return result;
	}
}
