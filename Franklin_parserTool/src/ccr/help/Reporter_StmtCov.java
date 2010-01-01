package ccr.help;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import residue.ListHits;

public class Reporter_StmtCov {
	
	private class LinePair {
		private int from;
		private int to;

		public LinePair(String linePairStr) {
			String[] strs = linePairStr.split("-");
			from = Integer.parseInt(strs[0]);
			if (strs.length < 2) {
				to = from;
			} else {
				to = Integer.parseInt(strs[1]);
			}
		}
		
		public LinePair(int f, int t) {
			from = f;
			to = t;
		}
		
		public boolean isInside(int line) {
			return (line >= from && line <= to);
		}
	}
	/**
	 * 
	 * @param gretelFile: the full name of gretel files  
	 * @return statement coverage of class under testing
	 */
	public double getCoverageReport(String gretelFile, String exclFile, String logFile, String testCaseId){
		double percentage = 0.0;
		try {
			Map<String, Collection<LinePair>> exclusion = generateExclusion(exclFile);
			
			ListHits.setConfigFile(gretelFile);
			Map<String, HashMap<Integer, Integer>> hits = ListHits.getHits();
			Map<String, Set<Integer>> misses = ListHits.getMisses();
			
			//get coverage
			Map<String, Set<Integer>> all = new HashMap();
			Map<String, Map<Integer, Integer>> covered = new HashMap();
			
			
			for(Map.Entry<String, HashMap<Integer, Integer>> entry : hits.entrySet()){
				String file = entry.getKey();
				HashMap<Integer, Integer> lines = entry.getValue();
				
				if(!all.containsKey(file)){
					all.put(file, new HashSet<Integer>());
				}
				Set<Integer> allFile = all.get(file);
				if(!covered.containsKey(file)){
					covered.put(file, new HashMap<Integer, Integer>());
				}				
				Map<Integer, Integer> coveredFile = covered.get(file);
				
				
//				Collection<LinePair> excludedLinePairs = exclusion.containsKey(file) ? exclusion.get(file) : null;
				Collection<LinePair> excludedLinePairs = exclusion.get(file);
				for (Map.Entry<Integer, Integer> coveredEntry : lines.entrySet()) {
					Integer line = coveredEntry.getKey();
					if (excludedLinePairs != null && this.isExcluded(excludedLinePairs, line)) continue;
					allFile.add(line);
					
					Integer count = coveredEntry.getValue();
					if (coveredFile.containsKey(line)) {
						coveredFile.put(line, coveredFile.get(line) + count);
					} else {
						coveredFile.put(line, count);
					}
				}
			}
			
			for(Map.Entry<String, Set<Integer>> entry: misses.entrySet()){
				String file = entry.getKey();
				Set<Integer> lines = entry.getValue();
				
				if(!all.containsKey(file)){
					all.put(file, new HashSet<Integer>());
				}
				Set<Integer> allFile = all.get(file);
				
				Collection<LinePair> excludedLinePairs = exclusion.get(file);
				for (Integer line : lines) {
					if (excludedLinePairs != null && this.isExcluded(excludedLinePairs, line)) continue;
					allFile.add(line);
				}
			}

			//show the coverage
			int coveredLines_sum = 0;
			int totalLines_sum = 0;
			int totalCoverTimes_sum = 0;
			for(String file: all.keySet()){
				int totalLines = all.get(file).size();
				int coveredLines = covered.containsKey(file)?covered.get(file).size():0;
//				System.out.println(file + " covered on " + coveredLines + " of " 
//						+  totalLines + " lines");
				if (covered.containsKey(file)) {
					for (Map.Entry<Integer, Integer> entry : covered.get(file).entrySet()) {
//						System.out.print("[" + entry.getKey() + "]:" + entry.getValue() + ";");
						totalCoverTimes_sum += entry.getValue();
					}
//					System.out.println();
				}
				coveredLines_sum += coveredLines;
				totalLines_sum += totalLines;
			}
			percentage = coveredLines_sum * 100.0 / totalLines_sum;
//			System.out.println("Total coverage:" + coveredLines_sum + " of " + totalLines_sum 
//					+ " lines ==> " + (new DecimalFormat("0.00").format(percentage)) + " %   Average coverage per line:" + (new DecimalFormat("0.00").format(totalCoverTimes_sum * 1.0 / coveredLines_sum)));
			
			BufferedWriter bw = new BufferedWriter(new FileWriter(logFile));
			bw.write(testCaseId + "\t" + totalCoverTimes_sum + "\t" + coveredLines_sum + "\t" + (new DecimalFormat("0.00").format(percentage)) + "\n");
			
//			for(String file: all.keySet()){
//				int totalLines = all.get(file).size();
//				int coveredLines = covered.containsKey(file)?covered.get(file).size():0;
//				if (covered.containsKey(file)) {
//					for (Map.Entry<Integer, Integer> entry : covered.get(file).entrySet()) {
//						bw.write(file + "\t" + entry.getKey() + "\t" + entry.getValue() + "\n");
//					}
//				}
//				else if (misses.containsKey(file)) {
//					for 
//				}
//			}			
			
			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return percentage; 
	}
	
	
	private Map<String, Collection<LinePair>> generateExclusion(String exclFile) {
		Map<String, Collection<LinePair>> exclusion = new HashMap<String, Collection<LinePair>>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(exclFile));
			String filename = null;
			while((filename = br.readLine()) != null){
				if (filename.equals("")) break;
				String[] linePairStrs = br.readLine().split(",");
				ArrayList<LinePair> linePairs = new ArrayList<LinePair>();
				for (String linePairStr : linePairStrs) {
					linePairs.add(new LinePair(linePairStr));
				}
				exclusion.put(filename, linePairs);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return exclusion;
	}
	
	private boolean isExcluded(Collection<LinePair> linePairs, int line) {
		for (LinePair linePair : linePairs) {
			if (linePair.isInside(line)) {
				return true;
			}
		}
		return false;
	}


	public static void main(String[] args){
		if(args.length == 0){
			System.out.println("Please specify the .gretel files");			
		}else{
			new Reporter_StmtCov().getCoverageReport(args[0], args[1], args[2], args[3]);	
		}
		
	}	
}
