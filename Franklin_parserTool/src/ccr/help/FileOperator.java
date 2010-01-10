//Author: Martin
//Time: 2007/12/28

package ccr.help;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Vector;

import ccr.test.Logger;
import ccr.test.TestDriver;

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
	
	public static void getMissingFault(String detectedFaultFile, boolean containHeader, String saveFile){		
		Vector detectedFaults  = new Vector();
		
		try {
			BufferedReader br = new BufferedReader(new FileReader(detectedFaultFile));
			if(containHeader){
				br.readLine();
			}
			
			String str = null;
		
			while((str = br.readLine())!= null){
				detectedFaults.add(Integer.parseInt(str));
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		StringBuilder sb = new StringBuilder();
		int startVersion = 0;
		int endVersion = 5024;		
		for(int i = startVersion; i < endVersion; i ++){
			if(!detectedFaults.contains(i)){
				sb.append(i).append("\n");
			}
		}
		
		Logger.getInstance().setPath(saveFile, false);
		Logger.getInstance().write(sb.toString());
		Logger.getInstance().close();
	}
	
	public static File getFile(String classPath, String fileName){
//		fileName = fileName.replace('.', '/');
		for(String clzPath: classPath.split(";")){
			File temp = new File(clzPath + File.separator + fileName);
			if(temp.exists()){
				return temp;
			}
		}
		return null;
	}
	
	/**2009-10-14: filter faulty versions
	 * 
	 * @param src_file
	 * @param containHeader
	 * @param dest_file
	 */
	public static void filterFiles(String src_file, boolean containHeader, double max_failure_rate, String dest_file){
		StringBuilder sb = new StringBuilder();
		String line = null;

		
		try {
			BufferedReader br = new BufferedReader(new FileReader(src_file));
			if(containHeader)
				br.readLine();
			
			while((line=br.readLine())!= null){
				String[] str = line.split("\t");
				String faultyVersion;
				double failureRate, P_AS_CA, P_AS_RA_H, P_ASU_CA, P_ASU_RA_H, P_A2SU_CA, P_A2SU_RA_H;
				
				if(str.length ==  14){
					faultyVersion = str[0];
					failureRate = Double.parseDouble(str[1]);
					P_AS_CA = Double.parseDouble(str[2]);
					P_AS_RA_H = Double.parseDouble(str[3]);
					
					P_ASU_CA = Double.parseDouble(str[6]);
					P_ASU_RA_H = Double.parseDouble(str[7]);
					
					P_A2SU_CA = Double.parseDouble(str[10]);
					P_A2SU_RA_H = Double.parseDouble(str[11]);
					
					if(failureRate < max_failure_rate && P_AS_CA <= P_AS_RA_H 
							&& P_ASU_CA < P_ASU_RA_H && P_A2SU_CA <= P_A2SU_RA_H){
						sb.append(faultyVersion + "\n");
					}	
				}
				
			}
			
			br.close();
			
			BufferedWriter bw = new BufferedWriter(new FileWriter(dest_file));
			bw.write(sb.toString());
			bw.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void saveFaultList(String srcFile, boolean containHeader, String saveFile){
		File tmp = new File(srcFile);
		if(tmp.exists()){
			StringBuilder sb = null;
			try {
				BufferedReader br = new BufferedReader(new FileReader(srcFile));
				if(containHeader){
					br.readLine();				
				}
				
				sb = new StringBuilder();
				String str = null;
				while((str = br.readLine())!= null){
					String[] strs = str.split("\t");
					String fault = strs[0].substring(
							strs[0].indexOf("_") + "_".length(), strs[0].indexOf(".java"));
					sb.append(fault).append("\n");
				}
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			Logger.getInstance().setPath(saveFile, false);
			Logger.getInstance().write(sb.toString());
			Logger.getInstance().close();
		}else{
			System.out.println("The fault list file:" + srcFile + " does not exist");
		}
		
	}
	
	public static void main(String[] args){
	//	FileOperator.copyFile("B.java");
	//	System.out.println(FileOperator.deleteFile("B.java"));
		
	//	String src = System.getProperty("user.dir")+ File.separator +"Test" + File.separator + "A";
	//	String dest = System.getProperty("user.dir")+ File.separator +"Test" + File.separator  +"B";
	//	FileOperator.copyFile(src, dest);
//		String src = "src/ccr/app/TestCFG2.java";
//		String dest = "src/ccr/app/testversion/TestCFG2_1010.java";
//		System.out.print(FileOperator.compare(src, dest));
		
//		String date = args[0];
//		double max_failure_rate = Double.parseDouble(args[1]);
//		
//		String src_file = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
//		+ date + "/PerValidTS.txt";
//		String dest_file = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
//			+ date + "/FaultList_new.txt";
//		boolean containHeader = true;
//		FileOperator.filterFiles(src_file, containHeader, max_failure_rate, dest_file);
		
		String date = "20091230";
		String detectedFaultFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
			+ date + "/detectedFaultList.txt";
		boolean containHeader = true;
		String saveFile ="src/ccr/experiment/Context-Intensity_backup/TestHarness/"
			+ date + "/MissingFaultList.txt";
//		FileOperator.getMissingFault(detectedFaultFile, containHeader, saveFile);
		String srcFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
			+ date + "/NonEquivalentFaults.txt";
		saveFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
			+ date + "/NonEquivalentFaults.txt";
		FileOperator.saveFaultList(srcFile, containHeader, saveFile);
	}
}
