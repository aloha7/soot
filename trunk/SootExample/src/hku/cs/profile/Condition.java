package hku.cs.profile;

import soot.Value;

public class Condition {
	public Value cond;
	public int times;
	public int target;
	
	public Condition(Value c, int t, int tl){
		cond = c;
		times = t;
		target = tl;
		
	}

	public String toString(){
		StringBuffer sb = new StringBuffer();
		sb.append("condition: ");
		sb.append(cond);
		sb.append(" times: ");
		sb.append(times);
		sb.append(" target: ");
		sb.append(target);
		return sb.toString();
	}
	
}
