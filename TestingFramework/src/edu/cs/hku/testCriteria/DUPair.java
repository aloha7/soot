package edu.cs.hku.testCriteria;

import soot.Unit;

public class DUPair implements Cloneable{
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
	
	public Object clone(){
		try {
			DUPair dupair = (DUPair)super.clone();
			dupair.defUnit = (Unit)this.defUnit.clone();
			dupair.useUnit = (Unit)this.useUnit.clone();
			return dupair;
		} catch (CloneNotSupportedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new InternalError(); 
		}
	}
}
