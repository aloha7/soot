package edu.cs.hku.instrument;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import soot.Body;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.options.Options;
import soot.tagkit.LineNumberTag;
import soot.toolkits.graph.Block;
import soot.toolkits.graph.BlockGraph;
import soot.toolkits.graph.ExceptionalBlockGraph;

public class Probe_block {
	
	//if we interest in the cover time of a block in a specified execution,
	//we can use this data structure: className -> (method ->(BlockIndex, HitTimes)*)*
	private static HashMap<String, HashMap<String, HashMap<Integer, Integer>>> blockTable = 
		new HashMap<String, HashMap<String, HashMap<Integer, Integer>>>();
	
	//2009-09-06:most of time we are not interested in the cover times of a block at all. In this case,
	//we can use this data structure directly: className -> (method -> (BlockIndex)*)*
//	private static HashMap<String, HashMap<String, HashSet<Integer>>> block_hits = 
//		new HashMap<String, HashMap<String, HashSet<Integer>>>();
	
	//className -> (lineNumber, HitTimes)*
	private static HashMap<String, HashMap<Integer, Integer>> stmts = 
		new HashMap<String, HashMap<Integer, Integer>>();

	public static void coverBlock(String className, String methodName, int blockIndex){	
		HashMap<String, HashMap<Integer, Integer>> methods = blockTable.get(className);
		if(methods == null){
			methods = new HashMap<String, HashMap<Integer, Integer>>();
		}
		HashMap<Integer, Integer> blocks = methods.get(methodName);
		if(blocks == null){
			blocks = new HashMap<Integer, Integer>();
		}
		Integer hitTimes = blocks.get(blockIndex);
		if(hitTimes == null){
			hitTimes = 0;
		}
		hitTimes ++;
		
		blocks.put(blockIndex, hitTimes);
		methods.put(methodName, blocks);
		blockTable.put(className, methods);
	}
	
	/**increase the line hit by 1
	 * 
	 * @param className
	 * @param lineNumber
	 */
	private static void coverStmt(String className, int lineNumber){
		HashMap<Integer, Integer> line_hits = stmts.get(className);
		if(line_hits == null){
			line_hits = new HashMap<Integer, Integer>();
		}		
		Integer hitTimes = line_hits.get(lineNumber);
		if(hitTimes == null){
			hitTimes = 0;
		}
		hitTimes ++;
		
		line_hits.put(lineNumber, hitTimes);		
		stmts.put(className, line_hits);
	}
	
	/**increase the line hit by a specified coverTimes
	 * 
	 * @param className
	 * @param lineNumber
	 * @param coverTimes
	 */
	private static void coverStmt(String className, int lineNumber, int coverTimes){
		HashMap<Integer, Integer> line_hits = stmts.get(className);
		if(line_hits == null){
			line_hits = new HashMap<Integer, Integer>();
		}		
		Integer hitTimes = line_hits.get(lineNumber);
		if(hitTimes == null){
			hitTimes = 0;
		}
		hitTimes += coverTimes;
		
		line_hits.put(lineNumber, hitTimes);
		stmts.put(className, line_hits);
	}
	
	/**set the lint hit by a specified coverTimes
	 * 
	 * @param className
	 * @param lineNumber
	 * @param coverTimes
	 */
	private static void setStmtCover(String className,  int lineNumber, int coverTimes){
		HashMap<Integer, Integer> line_hits = stmts.get(className);
		if(line_hits == null){
			line_hits = new HashMap<Integer, Integer>();
		}		
		Integer hitTimes = line_hits.get(lineNumber);
		hitTimes = coverTimes;
		
		line_hits.put(lineNumber, hitTimes);
		stmts.put(className, line_hits);
	}
	
	
	private static int getStmtCover(String className, int lineNumber){
		HashMap<Integer, Integer>  line_hits = stmts.get(className);
		if(line_hits != null){
				Integer coverTimes = line_hits.get(lineNumber);
				if(coverTimes != null){
					return coverTimes;
				}
		}
		return 0;
	}
	
	public static HashMap<String, HashMap<Integer, Integer>> blockToStmt(){
		Options.v().set_keep_line_number(true);
		
		for(String className: blockTable.keySet()){
			
			Scene.v().loadClassAndSupport(className);	
			SootClass sClass = Scene.v().getSootClass(className);
			HashMap<String, HashMap<Integer, Integer>> methods = blockTable.get(className);								
			
			for(String methodName: methods.keySet()){
				HashMap<Integer, Integer> blocks = methods.get(methodName);
				SootMethod sMethod = sClass.getMethodByName(methodName);
				Body b = sMethod.retrieveActiveBody();
				BlockGraph graph = new ExceptionalBlockGraph(b);
				Iterator<Block> bIt = graph.iterator();
				
				while(bIt.hasNext()){
					Block block = bIt.next();				
					int blockIndex = block.getIndexInMethod();
					if(blocks.containsKey(blockIndex)){//this block is covered
						//1.extract all lineNumbers in this block
						HashSet<Integer> lineNums = new HashSet<Integer>();
						Iterator<Unit> uIt = block.iterator();
						while(uIt.hasNext()){
							LineNumberTag lnt = (LineNumberTag)uIt.next().getTag("LineNumberTag");
							if(lnt!= null){
								lineNums.add(lnt.getLineNumber());	
							}							
						}
						
						//2. add all these lineNumbers to stmtTable						
						for(Integer lineNum: lineNums){
							Integer coverTimes = blocks.get(blockIndex);							
							//since one unit may belong to several blocks. 
							//For these units, its cover time is determined by the maximum value
							int defaultCover = getStmtCover(className, lineNum);
							if(defaultCover != 0){ //this unit is covered before, then we set the max cover value as its cover times
								if(defaultCover < coverTimes){
									setStmtCover(className, lineNum, coverTimes);	
								}								
							}else{
								coverStmt(className,  lineNum, coverTimes);	
							}							
						}
					}
				}
			}
		}
		
		return stmts;
	}
	
	public static String reportBlock(){
		StringBuilder sb = new StringBuilder();
		sb.append("ClassName").append("\t").append("MethodName").append("\t").
			append("Block").append("\t").append("coverTimes").append("\n");
		for(String className: blockTable.keySet()){
			
			HashMap<String, HashMap<Integer, Integer>> methods = blockTable.get(className);
			for(String methodName: methods.keySet()){
				
				HashMap<Integer, Integer> blocks = methods.get(methodName);
				Integer[] blks = blocks.keySet().toArray(new Integer[blocks.size()]);
				Arrays.sort(blks);
				for(Integer blkIdx: blks){
					sb.append(className).append("\t");
					sb.append(methodName).append("\t");
					sb.append(blkIdx).append("\t").append(blocks.get(blkIdx)).append("\n");
				}
			}
		}
		sb.append("\n");
		String result = sb.toString();
		System.out.println(result);
		return result;
	}
	
	public static String reportStmt(){
		StringBuilder sb = new StringBuilder();
		sb.append("ClassName").append("\t").append("LineNumber").append("\t").append("coverTimes").append("\n");
		for(String className: stmts.keySet()){
				HashMap<Integer, Integer> line_hits = stmts.get(className);
				Integer[] lineArray = line_hits.keySet().toArray(new Integer[line_hits.size()]);
				Arrays.sort(lineArray);
				for(Integer lineNum: lineArray){
					sb.append(className).append("\t");
					sb.append(lineNum).append("\t");
					sb.append(line_hits.get(lineNum)).append("\n");
				}
		}
		sb.append("\n");
		String result = sb.toString();
		System.out.println(result);
		return result;
	}
	
	public static void clearAll(){
		System.out.println("Tables have been cleared");
		blockTable.clear();
		stmts.clear();
	}
}
