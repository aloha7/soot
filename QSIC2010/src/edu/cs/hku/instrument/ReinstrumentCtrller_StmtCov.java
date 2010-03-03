package edu.cs.hku.instrument;

import java.io.File;
import java.io.IOException;

import residue.Reinstrumenter;

public class ReinstrumentCtrller_StmtCov {

	public void reInstrument(String gretelFile){
		try {
			if(!new File(gretelFile).exists()){
				System.out.println(gretelFile + " does not exist");
				System.exit(1);
			}else{
				new Reinstrumenter().reinstrument(gretelFile);	
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args){
		if(args.length ==0){
			System.out.println("The first argument should be " +
					"the gretel config file to extract information");			
		}else{
			new ReinstrumentCtrller_StmtCov().reInstrument(args[0]);
		}
	}
}
