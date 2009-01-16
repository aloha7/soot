package context.test.contextIntensity;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Vector;

import context.test.util.Constant;
import context.test.util.Logger;


public class Manipulator {
	private static Manipulator manipulator = null;
	private Vector drivers;
	private boolean[] driverFlags;
	private long timeout = 3* 1000; //30 seconds
//	private Object lock = new Object();
	private static Logger log; //Cannot use this Logger anymore since it is used in other places
	
	private Manipulator(){

	}
	
	public void loadDrivers(String pathFile){
		try {
			BufferedReader reader = new BufferedReader(new FileReader(pathFile));
			String line = null;
			//each line represents: threadID + "|"+statementID
			while((line = reader.readLine())!= null)
				drivers.add(line);				

			driverFlags = new boolean[drivers.size()];
			for(int i = 0; i < driverFlags.length; i++)
				driverFlags[i] = false;
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}catch (IOException ep) {
			// TODO Auto-generated catch block
			ep.printStackTrace();
		}
	}

	public synchronized static Manipulator getInstance(){
		if(manipulator==null){
//			System.err.println("!!!!");
			manipulator = new Manipulator();
			manipulator.drivers = new Vector();
			manipulator.loadDrivers(Constant.baseFolder + "/ContextIntensity/Drivers/Drivers.txt");
			
		}		
		return manipulator;
	}
	
	public synchronized static Manipulator getInstance(String outputDir){
		if(manipulator == null){
			manipulator = new Manipulator();
			manipulator.drivers = new Vector();
			manipulator.loadDrivers(Constant.baseFolder + "ContextIntensity/Drivers/Drivers_CA.txt");
//			Logger.getInstance().setPath(Constant.baseFolder + outputDir + "/1.txt" , true);
		}
		return manipulator;
	}
	
	public void printFlag(){
		System.out.println("start to trace status flag:");
		for(int i = 0; i < driverFlags.length; i ++){
			System.out.print(driverFlags[i] + "\t");
		}
		System.out.println();
	}
	
	public synchronized int enterScheduler(String threadID, String cappID){
		int position = this.checkScheduler(threadID, cappID);
		while(position == -1){//wait
			long start = System.currentTimeMillis();
			try {
//				this.printFlag();
				this.wait();			
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			long enduration = System.currentTimeMillis()-start;
			if(enduration >= timeout){//timeout occurs
				Logger.getInstance().setPath(Constant.baseFolder + "/ContextIntensity/TimeOut.txt", false);
				StringBuilder sb = new StringBuilder();
				for(int i = 0; i < drivers.size(); i ++){
					sb.append(drivers.get(i)+ "\t");
				}
				Logger.getInstance().write(sb.toString());
				Logger.getInstance().close();
				System.exit(0);
			}else
				position = this.checkScheduler(threadID, cappID);
		}
		return position;
	}
	
	public synchronized int checkScheduler(String threadID, String cappID){
		int position = this.getIndex(threadID, cappID);
		if(position == -1) //if not in drivers, capp will be skipped
			return -2;
		if(canExecute(position)) //all capps before pos are executed
			return position;
		else
			return -1; //copp will wait
	}
	
	//2009/1/15: an updated version of checkScheduler
//	public synchronized int checkScheduler(String threadID, String cappID){
//		int position = this.getFirstUnexecuted();
//		int pos = this.getIndex(threadID, cappID);
//
//		if(position == -1){
//			//all capps in the driver have been executed
//			return -2;
//		}else {
//			if(pos == position) //exactly turn to this capp to execute
//				return pos ;
//			else{
//				if(pos > -1) //exist but current capp is not the right turn to run
//					return -1;
//				else{
//					if(this.canReachToExitThroughCapps(cappID)){
//						return -2;
//					}else
//						return -1;
//				}
//			}
//		}
//	}
	
	public synchronized void exitScheduler(String threadID, String cappID){
		int i = this.getIndex(threadID, cappID);
		if(i > -1){ //some drivers may not exist at all.
			driverFlags[i] = true;
			
		}	
//		Logger.getInstance().write(threadID + "|" + cappID);
//		Logger.getInstance().close();
		this.notifyAll();
	}
	
	public boolean canReachToExitThroughCapps(String srcCappID){
		boolean reachable = false;
		if(this.getFirstUnexecuted() < drivers.size()-1){
			String first = (String) drivers.get(this.getFirstUnexecuted());
			String destCappID = first.substring(first.indexOf("|")+"|".length());
			String threadID = first.substring(0, first.indexOf("|"));
			CFG cfg = DriverGenerator.getInstance().getCFG(threadID);
			reachable = cfg.canReachToExitThroughCapps(srcCappID, destCappID);
		}
		return reachable;
	}
	
	public synchronized int getIndex(String threadID, String cappID){
		int index = -1;  
		for(int i = 0; i < drivers.size(); i ++){ 
			if((threadID +"|" +cappID).equals((String)drivers.get(i))){
						if(!driverFlags[i]){//search for the first one not to be executed
							index = i;	
							break;	
						}						
			}
		}	
		return index;
	}
	
	public synchronized int getFirstUnexecuted(){
		int index = -1;
		for(int i = 0; i < driverFlags.length; i ++){
			if(!driverFlags[i]){
				index = i;
				break;
			}
		}
		return index;
	}
	
	public synchronized boolean canExecute(int position){
		boolean canExecute = false;
		int i = 0;
		for(; i < position; i ++){
			if(!driverFlags[i])
				break;
		}
		if(i == position)
			canExecute = true;
		
		if(position == 0){ //if position is the first driver, then it cannot continue to execute since it can only execute once.
			if(driverFlags[position]){//if it has been executed, it means that this is not its turn to run
				canExecute = false;
			}
		}
		
		
		return canExecute;
	}
	
	
	
}
