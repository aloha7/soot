package hku.cs.profile;

import soot.Value;

public class BlkIfStmt implements BlkLstStmt {
	private Condition tru;
	private Condition fal;
	public Condition getTru() {
		return tru;
	}
	public void setTru(Condition tru) {
		this.tru = tru;
	}
	public Condition getFal() {
		return fal;
	}
	public void setFal(Condition fal) {
		this.fal = fal;
	}
	
	public String toString(){
		StringBuffer sb = new StringBuffer();
		sb.append(tru);
		sb.append(", ");
		sb.append(fal);
		return sb.toString();
	}
	
}
