package context.test;

import java.util.Random;
import java.util.Vector;

import context.arch.widget.WTourDemo;
import context.arch.widget.WTourEnd;
import context.arch.widget.WTourRegistration;

public class PerturbationManager {

	private static PerturbationManager manager;
	private static Random perturbateRand;
	private static Random perturbateData;
	private static boolean DEBUG = false;
	private static Vector candidate;
	
	
	private PerturbationManager(){
		
	}
	
	public static synchronized PerturbationManager getInstance(){
		if(manager == null){
			manager = new PerturbationManager();
			perturbateRand = new Random();
			perturbateData = new Random();
			candidate = new Vector();
			candidate.add(WTourRegistration.UPDATE);
			candidate.add(WTourEnd.END);
			candidate.add(WTourDemo.INTEREST);
			candidate.add(WTourDemo.VISIT);
		}
		return manager;
	}
	
//	public static synchronized String dataPertubate(String event){
//		if(!DEBUG){ //enable the data perturbate function
//			if(!perturbateRand.nextBoolean()){//if not allowed to pertubate
//				return event;
//			}else{
//				int index = perturbateData.nextInt(candidate.size());
//				return (String)candidate.get(index);
//			}
//		}
//		
//	}
	
}
