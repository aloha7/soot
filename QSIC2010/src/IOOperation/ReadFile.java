package IOOperation;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class ReadFile {

	
	public static void main(String[] args) {
		String fileName = "a.txt";
		
		try {
			BufferedReader br = new BufferedReader(new FileReader(fileName));
			String str = null;
			while((str = br.readLine())!= null){
				String[] strs = str.split("\t");
				System.out.println("[0]:" + strs[0] + " [1]:" + strs[1]);
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
