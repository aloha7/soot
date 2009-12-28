package edu.cs.hku.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Reader;
import java.io.Writer;

public class StreamRedirectThread extends Thread {
	private final Reader in;
	private final Writer out;
	private final PrintStream outWriter;
	
	private final int BUFFER_SIZE = 2048;
	
	public StreamRedirectThread(String name, InputStream in, OutputStream out){
		super(name);
		this.in = new InputStreamReader(in);
		this.out = new OutputStreamWriter(out);
		this.outWriter = new PrintStream(out);
	}
	
	public void run(){
		BufferedReader br = new BufferedReader(in, BUFFER_SIZE);
		String line = null;
		try {
			while((line = br.readLine())!= null){
				outWriter.println(line);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
