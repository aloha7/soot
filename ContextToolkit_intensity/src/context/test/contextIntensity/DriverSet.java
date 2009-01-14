package context.test.contextIntensity;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Vector;

public class DriverSet {
	private Vector driverSet;
	
	public DriverSet(){
		driverSet = new Vector();	
	}
	
	public void addDriver(Driver driver){
		driverSet.add(driver);
	}
	
	public String toString(){
		StringBuilder sb = new StringBuilder();
		for(int i = 0 ; i < driverSet.size(); i ++){
			Driver driver = (Driver)driverSet.get(i);
			sb.append(driver.toString()+"\n");
		}
		return sb.toString();
	}
	
	public void writeToFile(String filename){
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(filename));
			bw.write(this.toString());
			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}
