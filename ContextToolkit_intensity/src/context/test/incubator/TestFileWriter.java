package context.test.incubator;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class TestFileWriter {

	public static void main(String[] args){
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter("c:\\a.txt"));
			bw.write("a");
			bw.close();
			
			bw = new BufferedWriter(new FileWriter("c:\\a.txt"));
			bw.write("b");
			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
