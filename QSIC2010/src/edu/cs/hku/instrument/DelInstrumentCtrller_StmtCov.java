package edu.cs.hku.instrument;

import static edu.cs.hku.util.Constants.FS;
import static edu.cs.hku.util.Constants.USER_DIR;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import residue.Remover;

public class DelInstrumentCtrller_StmtCov {
	
	public void removeInstruments(String[] classes){
		Set<File> classSet = new HashSet<File>();
		
		for(int i = 0; i < classes.length; i ++){
			File p = new File(classes[i]);
			if(!p.exists()){
				classes[i] = USER_DIR + FS + "bin" + FS + classes[i];
				p = new File(classes[i]);
				if(!p.exists()){
					System.out.println(classes[i] + " does not exist!");
				}else{
					classSet.add(p);
				}
			}else{
				classSet.add(p);
			}
		}
		Remover r = new Remover();
		try {
			r.remove(classSet);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args){
		if(args.length == 0){
			System.out.println("please specify class files to remove instruments");
		}else{
			new DelInstrumentCtrller_StmtCov().removeInstruments(args);	
		}
	}
		
}
