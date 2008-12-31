package hku.cs.profile;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LineNode {
	int maxTimes = 0;
	int minTimes = Integer.MAX_VALUE;
	List<BlkLstStmt> predicates = new ArrayList(2);
	
	public void setMaxTimes(int t){
		if (t > maxTimes)
			maxTimes = t;
	}
	public void setMinTimes(int t){
		if(t < minTimes)
			minTimes = t;
	}
	
	public String toString(){
		StringBuffer sb = new StringBuffer(500);
		sb.append("\nmaxTimes: "+maxTimes);
		sb.append("\nminTimes: "+minTimes+"\npredicates: ");
		sb.append(predicates);
		return sb.toString();
	}
}
