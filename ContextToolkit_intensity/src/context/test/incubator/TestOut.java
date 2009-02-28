package context.test.incubator;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

public class TestOut {
	
	
	public static void main(String[] args){
		try {
			PrintStream ps =new PrintStream(new BufferedOutputStream(new FileOutputStream("c:\\a.txt", true))); 
			System.setOut(ps);
			System.setErr(ps);
			System.out.println("GoodMorning");
			
			ps.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
