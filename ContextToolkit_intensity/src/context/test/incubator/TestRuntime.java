package context.test.incubator;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class TestRuntime {

	public static void main(String args[]) {
//		testWinCmd();
//		dirOpt();
		
		try {
			Process p = Runtime.getRuntime().exec("java context.test.incubator.Reflection", null, new File("C:\\WangHuai\\Martin\\Eclipse3.3.1\\ContextToolkit_intensity\\bin"));
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	public static void testWinCmd() {
		System.out
				.println("------------------testWinCmd()--------------------");
		Runtime runtime = Runtime.getRuntime();
		System.out.println(runtime.totalMemory());
		System.out.println(runtime.freeMemory());
		System.out.println(runtime.maxMemory());
		System.out.println(runtime.availableProcessors());
		try {

			runtime.exec("notepad");
//			runtime
//					.exec("C:\\Program Files\\Microsoft Office\\OFFICE11\\winword.exe c:\\test.doc");
//		
//			runtime.exec("c:\\x.bat");
//		
			runtime.exec("cmd /c dir ");
			runtime.exec("cmd /c dir c:\\");

		

			runtime.exec("cmd /c copy c:\\x.bat d:\\x.txt"); 
			runtime.exec("cmd /c rename d:\\x.txt x.txt.bak"); 
			runtime.exec("cmd /c move d:\\x.txt.bak c:\\"); 
			runtime.exec("cmd /c del c:\\x.txt.bak"); 


			runtime.exec("cmd /c md c:\\_test"); 
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void dirOpt() {
		System.out.println("------------------dirOpt()--------------------");
		Process process;
		try {
			
			process = Runtime.getRuntime().exec("dir c:\\");
			
			InputStream fis = process.getInputStream();
			
			BufferedReader br = new BufferedReader(new InputStreamReader(fis));
			String line = null;
			
			while ((line = br.readLine()) != null) {
				System.out.println(line);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
}
