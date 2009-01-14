package context.test.contextIntensity;

import java.util.Vector;

public class Driver {
	private Vector drivers = new Vector();
	
	public void add(String driver){
		drivers.add(driver);
	}
	
	public String get(int index){
		return (String)drivers.get(index);
	}
	
	public String toString(){
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < drivers.size(); i ++){
			sb.append(drivers.get(i)+"\t");
		}
		return sb.toString();
	}
}
