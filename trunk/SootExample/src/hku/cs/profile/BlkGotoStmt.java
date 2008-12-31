package hku.cs.profile;

public class BlkGotoStmt implements BlkLstStmt{
	int times;
	int target;
	public BlkGotoStmt(int b, int tb){
		times = b;
		target = tb;		
	}
	
	public String toString(){
		return "Goto: times "+ Integer.toString(times)+", target "+Integer.toString(target);
	}
}
