//Author: Martin
//Time: 2007/12/28

package ccr.help;

import java.io.*;

import ccr.test.*;

public class FileOperator {
		
	public static void copyFile(String fileName){
		copyFile(fileName, TestDriver.APPLICATION_FOLDER, TestDriver.WORK_FOLDER);	
	}
		
	public static void copyFile(String fileName, String src, String dest){
		try{
			InputStream in = new FileInputStream(new File( src + File.separator + fileName));
			OutputStream out = new FileOutputStream(new File( dest + File.separator +  fileName));
			
			byte[] buff = new byte[1024];
			int len;
			while((len = in.read(buff)) > 0){
				out.write(buff, 0, len);
			}
			in.close();
			out.close();
		}catch(Exception e){
			System.out.print(e);
		}	
	}
	
	//Copy all files from one directory to another directory
	public static void copyFile( String src, String dest){
		try{
			File srcFile = new File(src);
			if(srcFile.isDirectory()){
				String dest_temp = null;				
				String[] children = srcFile.list();			
				for(String s : children){					 					 					
					if(new File(srcFile, s).isDirectory())
					{
						dest_temp = dest + File.separator + s;
						new File(dest_temp).mkdir();																		
						copyFile(src + File.separator + s, dest_temp);
					}else{
						copyFile(src + File.separator + s, dest);
					}						
				}					
			}else{			
				copyFile(srcFile.getName(), srcFile.getParent(), dest);
			}
		}catch(Exception e){
			System.out.print(e);
		}		
	}
	
	public static boolean deleteFile(String fileName){
		boolean succ = false;
		
		try{
			succ = new File(TestDriver.WORK_FOLDER + fileName).delete();	
		}catch(Exception e){
			System.out.print(e);
		}		
		return succ;		
	}
	
	//How to make folder?
	
	//
	public static boolean compare(String src, String dest){
		boolean result = true;
		File src_file = new File(src);
		File dest_file = new File(dest);
	
		try{
			if(src_file.length()!= dest_file.length()){
				result = false;			
			}
			else{
				
				//Method1
				InputStream in_src = new FileInputStream(src_file);
				InputStream in_dest = new FileInputStream(dest_file);
				byte[] bytes_src = new byte[1024];
				byte[] bytes_dest = new byte[1024];
				int length = 0;
				while((length = in_src.read(bytes_src))!= 0){
					String s = new String(bytes_src);
					in_dest.read(bytes_dest);
					String s1 = new String(bytes_dest);
					if(!s.equals(s1)){
						result = false;
						break;
					}
				}
				
				//Method2
				BufferedReader bs_src = new BufferedReader(new FileReader(src_file));
				BufferedReader bs_dest = new BufferedReader(new FileReader(dest_file));
				String br_src = bs_src.readLine();
				String br_dest = bs_dest.readLine();
				while(br_src != null){
					if(!br_src.equals(br_dest)){
						result = false;
						break;
					}
					br_src = bs_src.readLine();
					br_dest = bs_dest.readLine();
				}				
			}					
		}catch(Exception e){
			System.out.println(e);
		}
		return result;
			
	}
	
	public static void main(String[] args){
	//	FileOperator.copyFile("B.java");
	//	System.out.println(FileOperator.deleteFile("B.java"));
		
	//	String src = System.getProperty("user.dir")+ File.separator +"Test" + File.separator + "A";
	//	String dest = System.getProperty("user.dir")+ File.separator +"Test" + File.separator  +"B";
	//	FileOperator.copyFile(src, dest);
		String src = "src/ccr/app/TestCFG2.java";
		String dest = "src/ccr/app/testversion/TestCFG2_1010.java";
		System.out.print(FileOperator.compare(src, dest));
	}
}
