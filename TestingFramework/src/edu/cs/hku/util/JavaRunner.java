package edu.cs.hku.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

public class JavaRunner {
	
	public static void runCommand(String cmd){		
		System.out.println("Cmd String:" + cmd);
		Runtime rt = Runtime.getRuntime();	
		Process p = null;
		try {
			p = rt.exec(cmd);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		StreamRedirectThread err_Thread = new StreamRedirectThread("stderr",
				p.getErrorStream(), System.err);
		
		StreamRedirectThread out_Thread = new StreamRedirectThread("stdout",
				p.getInputStream(), System.out);
		
		err_Thread.start();
		out_Thread.start();
		
		int result = -1;
		try {
			result = p.waitFor();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			err_Thread.join();
			out_Thread.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
