package context.test.contextIntensity;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.Vector;

import context.test.util.Constant;
import context.test.util.Logger;

public class Manipulator {
	private static Manipulator manipulator = null;
	// private Vector drivers;
	// private boolean[] driverFlags;

	// 2009/1/17: cover multiple drivers at each time
	Vector driverMatrix;
	Vector execFlagMatrix;
	int currentDriver = -1;// select it by matching the first capp in one
							// driver, reset it when current driver is
							// satisfied.

	private long timeout = 3 * 1000; // 3 seconds
	private static Logger log; // Cannot use this Logger anymore since it is
								// used in other places
	private static String driverFile = Constant.baseFolder
			+ "/ContextIntensity/Drivers/Drivers_CA.txt";

	private Manipulator() {

	}

	public synchronized static Manipulator getInstance() {
		if (manipulator == null) {
			manipulator = new Manipulator();
			manipulator.loadDrivers(driverFile);

		}
		return manipulator;
	}

	public synchronized static Manipulator getInstance(String driversFile) {
		if (manipulator == null) {
			manipulator = new Manipulator();
			manipulator.loadDrivers(driversFile);
		}
		return manipulator;
	}

	// 2009/1/17: one file may contains many drivers
	public void loadDrivers(String pathFile) {
		try {
			driverMatrix = new Vector();
			execFlagMatrix = new Vector();
			BufferedReader reader = new BufferedReader(new FileReader(pathFile));
			String line = null;

			// each line represents one driver(a execution path):
			// (threadID+statementID)*
			while ((line = reader.readLine()) != null) {
				Vector singleDrive = new Vector();
				Vector execFlag = new Vector();

				int index = line.indexOf("\t");
				while (index > -1) {
					singleDrive.add(line.substring(0, index));
					execFlag.add(false);
					line = line.substring(index + "\t".length());
					index = line.indexOf("\t");
				}
				// the last index
				if (line.length() > 0) {
					singleDrive.add(line);
					execFlag.add(false);
				}
				driverMatrix.add(singleDrive);
				execFlagMatrix.add(execFlag);
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException ep) {
			// TODO Auto-generated catch block
			ep.printStackTrace();
		}
	}

	public void printFlag() {
		System.out.println("start to trace status flag:");
		for (int i = 0; i < execFlagMatrix.size(); i++) {
			Vector execFlag = (Vector) execFlagMatrix.get(i);
			System.err.println("Execution Flag " + i + ":");
			for (int j = 0; j < execFlag.size(); j++)
				System.err.print(j + "\t");
			System.err.println();
		}
	}

	public synchronized int enterScheduler(String threadID, String cappID) {
		int position = this.checkScheduler(threadID, cappID);
		while (position == -1) {// wait
			long start = System.currentTimeMillis();
			try {
				System.err.println(threadID + cappID + " is waiting");
				this.wait(timeout);
				System.err.println(threadID + cappID + " leave waiting status");
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			long enduration = System.currentTimeMillis() - start;
			if (enduration >= timeout) {// timeout occurs, list all uncovered drivers
//				this.printAllUncoveredDrivers();
				System.exit(0);				
			} else
				position = this.checkScheduler(threadID, cappID);
		}
		return position;
	}

	public synchronized int checkScheduler(String threadID, String cappID) {
		int position;

		if (currentDriver == -1) {
			// select an currentDriver as the first driver that contains such an element
			for(int i = 0; i < driverMatrix.size(); i ++){
				Vector driver = (Vector)driverMatrix.get(i);
				int pos  = this.getIndex(i, threadID, cappID);
				if(pos > -1 && pos < driver.size()){ // if "threadID + cappID" is in this driver
					Vector execFlag = (Vector)execFlagMatrix.get(i);
					if(!(Boolean)execFlag.get(pos)){
						currentDriver = i;
						break;	
					}
				}
			}
		}
		
		//if no currentDriver is specified, currentDriver =-1 
		//and it means that threadID+cappID is not specified in drivers and can be executed at will
		position = this.getIndex(currentDriver, threadID, cappID);

		if (position == -1) // if not in drivers, capp will be skipped
			return -2;
		if (canExecute(currentDriver, position)) // all capps before pos are executed
			return position;
		else
			return -1; // copp will wait
	}

	public synchronized int getIndex(int driverIndex, String threadID,
			String cappID) {
		int index = -1;
		if (driverIndex < 0 || driverIndex > driverMatrix.size() - 1)
			return index;
		else {
			Vector drivers = (Vector) driverMatrix.get(driverIndex);
			Vector driverFlags = (Vector) execFlagMatrix.get(driverIndex);
			for (int i = 0; i < drivers.size(); i++) {
				if ((threadID + cappID).equals((String) drivers.get(i))) {
					if (!(Boolean) driverFlags.get(i)) {
						// search for the first one not to be executed
						index = i;
						break;
					}
				}
			}
		}
		return index;
	}

	// 2009/1/15: an updated version of checkScheduler
	// public synchronized int checkScheduler(String threadID, String cappID){
	// int position = this.getFirstUnexecuted();
	// int pos = this.getIndex(threadID, cappID);
	//
	// if(position == -1){
	// //all capps in the driver have been executed
	// return -2;
	// }else {
	// if(pos == position) //exactly turn to this capp to execute
	// return pos ;
	// else{
	// if(pos > -1) //exist but current capp is not the right turn to run
	// return -1;
	// else{
	// if(this.canReachToExitThroughCapps(cappID)){
	// return -2;
	// }else
	// return -1;
	// }
	// }
	// }
	// }
	
	public synchronized void printAllUncoveredDrivers(){
		Logger.getInstance().setPath(
				Constant.baseFolder + "/ContextIntensity/TimeOut.txt",
				false);
		
		StringBuilder sb = new StringBuilder();
		
		//2009/1/17:report all uncovered drivers
		for(int i = 0; i < execFlagMatrix.size(); i ++){
			Vector execFlag = (Vector)execFlagMatrix.get(i);
			int j;
			for( j = 0; j < execFlag.size(); j++){
				if(!(Boolean)execFlag.get(j)){//has not been covered
					break;
				}
			}
			if(j != execFlag.size()){ //the ith driver has not been covered
				Vector driver = (Vector)driverMatrix.get(i); 
				for(int k = 0; k < driver.size(); k ++)
					sb.append(driver.get(k) + "\t");
				sb.append("\n");
			}
		}
		if(sb.length() > 0 ){
			System.err.println("Lists of uncovered drivers:" + "\n" + sb.toString());
		}
	}
	
	public synchronized void exitScheduler(String threadID, String cappID) {
		//1.label currentDriver and reset it if currentDriver has been covered 
		if(currentDriver >-1 && currentDriver < driverMatrix.size()){
			int i = this.getIndex(currentDriver, threadID, cappID);
			if(i > -1){
				Vector driverFlag = (Vector)execFlagMatrix.get(currentDriver);
				driverFlag.set(i, true);
				if(i == driverFlag.size() -1){//reset currentDriver if it has been covered
					currentDriver = -1;
				}
			}
		}
		
		//2. label other drivers in case one test case can cover more than drivers
		for(int i = 0; i < driverMatrix.size() /*&& i !=currentDriver*/; i ++){
			if(i!= currentDriver){
				int j = this.getIndex(i, threadID, cappID);
				Vector execFlag = (Vector)execFlagMatrix.get(i);
				if(j >-1 && j < execFlag.size()){
					if (canExecute(i, j)) // all capps before pos are executed
						execFlag.set(j, true);				
				}	
			}
		}
		System.err.println(threadID + cappID);
		this.notifyAll();
		
		boolean allCovered = true;
		//3. if all drivers in the file has been covered
		for(int i = 0; i < execFlagMatrix.size(); i ++){
			Vector execFlag = (Vector)execFlagMatrix.get(i);
			for(int j = 0; j < execFlag.size(); j ++){
				if(!(Boolean)execFlag.get(j)){
					allCovered = false;
					break;
				}
			}
			if(!allCovered)
				break;
		}
		if(allCovered){
			System.err.println("All drivers in the file has been covered!");
			System.exit(0);
		}
	}

	/*public boolean canReachToExitThroughCapps(String srcCappID) {
		boolean reachable = false;
		if (this.getFirstUnexecuted() < drivers.size() - 1) {
			String first = (String) drivers.get(this.getFirstUnexecuted());
			String destCappID = first.substring(first.indexOf("|")
					+ "|".length());
			String threadID = first.substring(0, first.indexOf("|"));
			CFG cfg = DriverGenerator.getInstance().getCFG(threadID);
			reachable = cfg.canReachToExitThroughCapps(srcCappID, destCappID);
		}
		return reachable;
	}*/

	
//	public synchronized int getFirstUnexecuted() {
//		int index = -1;
//		for (int i = 0; i < driverFlags.length; i++) {
//			if (!driverFlags[i]) {
//				index = i;
//				break;
//			}
//		}
//		return index;
//	}

	public synchronized boolean canExecute(int driverIndex, int position) {
		boolean canExecute = false;
		
		if(driverIndex == -1 || driverIndex >= execFlagMatrix.size()){
			//if not defined by any drivers
			canExecute = true;
		}else{
			Vector driverFlags = (Vector)execFlagMatrix.get(driverIndex);
			int i = 0;
			for (; i < position; i++) {
				if (!(Boolean)driverFlags.get(i))
					break;
			}
			if (i == position)
				canExecute = true;

			if (position == 0) { // if position is the first driver, then it
									// cannot continue to execute since it can only
									// execute once.
				if ((Boolean)driverFlags.get(position)) {// if it has been executed, it means
											// that this is not its turn to run, for example: 1 3 1
					canExecute = false;
				}
			}
		}
		return canExecute;
	}
	
	public synchronized boolean canExecute(int position) {
		boolean canExecute = false;
		
		if(currentDriver == -1 || currentDriver >= execFlagMatrix.size()){
			//if not defined by any drivers
			canExecute = true;
		}else{
			Vector driverFlags = (Vector)execFlagMatrix.get(currentDriver);
			int i = 0;
			for (; i < position; i++) {
				if (!(Boolean)driverFlags.get(i))
					break;
			}
			if (i == position)
				canExecute = true;

			if (position == 0) { // if position is the first driver, then it
									// cannot continue to execute since it can only
									// execute once.
				if ((Boolean)driverFlags.get(position)) {// if it has been executed, it means
											// that this is not its turn to run, for example: 1 3 1
					canExecute = false;
				}
			}
		}
		
		return canExecute;
	}

}
