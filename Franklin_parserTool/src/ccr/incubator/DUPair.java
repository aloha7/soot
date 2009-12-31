package ccr.incubator;

import soot.Unit;

public class DUPair{
	public Unit defUnit;
	public Unit useUnit;	
	
	DUPair(Unit def, Unit use){
		this.defUnit = def;
		this.useUnit = use;
	}
	
	public String toString(){
		return "def:" + defUnit +"(" + defUnit.getTag("LineNumberTag") + ")" 
					+ "use:" + useUnit + "(" + useUnit.getTag("LineNumberTag") +")";
	}
}
