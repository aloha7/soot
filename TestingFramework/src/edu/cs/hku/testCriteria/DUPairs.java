package edu.cs.hku.testCriteria;

import java.util.HashMap;
import java.util.HashSet;

public class DUPairs implements Cloneable {
	/**
	 * def-use*
	 */
	public HashMap<Integer, HashSet<Integer>> duPairs = null;
	
	public DUPairs(HashMap<Integer, HashSet<Integer>> duPairs){
		this.duPairs = duPairs;
	}
	
	public DUPairs clone(){
		try {
			DUPairs dupairs_copy = (DUPairs)super.clone();
			dupairs_copy.duPairs = (HashMap<Integer, HashSet<Integer>>)this.duPairs.clone();
			for(Integer def_line: dupairs_copy.duPairs.keySet()){
				dupairs_copy.duPairs.put(def_line, (HashSet<Integer>)this.duPairs.get(def_line).clone());
			}
			return dupairs_copy;
		} catch (CloneNotSupportedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}
	
	
}
