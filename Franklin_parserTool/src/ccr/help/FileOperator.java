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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;
import java.util.Vector;

import ccr.test.Logger;
import ccr.test.ResultAnalyzer;
import ccr.test.TestCase;
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
	
	/**2010-01-21: a new version to merge files
	 * 
	 * @param date
	 * @param prefix
	 */
	public static void mergeFiles(String date, String prefix){
		boolean containHeader = true;		
		String pattern = prefix + "\\_-?[0-9]+\\_-?[0-9]+\\.txt";
		String srcDir = "src/ccr/experiment/Context-Intensity_backup/TestHarness/" 
			+ date + "//TestPool_Alpha//" ;	
		String saveFile = srcDir + prefix + ".txt";
		ResultAnalyzer.mergeFiles(srcDir, containHeader, pattern, saveFile); 
	}
	
	public static void mapIDInputs_left(String date, boolean containHeader, String alpha, 
			String tc_min, String tc_max){
		String testPool_old = "src/ccr/experiment/Context-Intensity_backup/TestHarness/" 
			+ date + "/TestPool_Alpha/TestPool_old.txt" ;
		
		String testPool_new  = "src/ccr/experiment/Context-Intensity_backup/TestHarness/" 
			+ date + "/TestPool_Alpha/TestPool_"+ alpha + "_" + tc_min + "_" + tc_max+".txt" ;
		
		String testCase_left = "src/ccr/experiment/Context-Intensity_backup/TestHarness/" 
			+ date + "/TestPool_Alpha/MissedID.txt" ;
		
		File tmp_1 = new File(testPool_old);
		File tmp_2 = new File(testPool_new);
		File tmp_3 = new File(testCase_left);
		if(tmp_1.exists() && tmp_2.exists() && tmp_3.exists()){			
			//ID of test cases-> test cases
			HashMap<Integer, TestCase> id_tc = new HashMap<Integer, TestCase>();
			try {
				//1. read the left test case
				ArrayList<Integer> tc_left = new ArrayList<Integer>();
				
				BufferedReader br_2 = new BufferedReader(new FileReader(testCase_left));
				if(containHeader){
					br_2.readLine();
				}
				String str = null;
				while((str = br_2.readLine())!= null){
					String[] strs = str.split("\t");
					int id = Integer.parseInt(strs[0]);
					tc_left.add(id);
				}
				br_2.close();
				System.out.println("missed test cases before processing:" + tc_left.size());
				
				//2. read the old test pool
				BufferedReader br = new BufferedReader(new FileReader(testPool_old));		
				if(containHeader)
					br.readLine();				
				str = null;
				while((str = br.readLine())!= null){
					String[] strs = str.split("\t");
					if(strs.length > 2){
						int id = Integer.parseInt(strs[0]);
						if(tc_left.contains(id)){ //only care about the left test cases
							String length = strs[1];
							String cd = strs[2];
							TestCase tc = new TestCase();
							tc.index = "" + id;
							tc.length = "" + Double.parseDouble(length);
							tc.CI = Double.parseDouble(cd);
							id_tc.put(id, tc);	
						}
					}
				}
				br.close();
				
				
				//length -> CD* -> TestCases*
				HashMap<String, HashMap<Double, ArrayList<String>>> length_CD_inputList = new 
					HashMap<String, HashMap<Double,ArrayList<String>>>();
								
				//2. read the new test pool
				int counter = 0;
				BufferedReader br_1 = new BufferedReader(new FileReader(testPool_new));
				if(containHeader)
					br_1.readLine();
				
				while((str = br_1.readLine())!= null){
					String[] strs = str.split("\t");
					if(strs.length > 2){
						String input = strs[0];
						String length = "" + Double.parseDouble(strs[1]);
						double cd = Double.parseDouble(strs[2]);
						
						if(length_CD_inputList.containsKey(length)){
							HashMap<Double, ArrayList<String>> CD_inputList = length_CD_inputList.get(length);
							ArrayList<String> inputList = null;
							
							if(CD_inputList.containsKey(cd)){
								inputList = CD_inputList.get(cd);
							}else{
								inputList = new ArrayList<String>();
							}	
							
							inputList.add(input);
							CD_inputList.put(cd, inputList);
							length_CD_inputList.put(length, CD_inputList);
							counter ++;
						}else{
							ArrayList<String> inputList = new ArrayList<String>();
							inputList.add(input);
							
							HashMap<Double, ArrayList<String>> CD_inputList = new 
							HashMap<Double, ArrayList<String>>();
							CD_inputList.put(cd, inputList);
							length_CD_inputList.put(length, CD_inputList);
							counter ++;
						}
					}
				}
				br_1.close();
				System.out.println(counter);
				
				//3. map the id of test cases and inputs
				HashMap<Integer, String> id_input = new HashMap<Integer, String>(); //keep the mapping between ids of test cases and inputs
				ArrayList<Integer> missedID = new ArrayList<Integer>(); //keep all the missed test cases
				
				Integer[] idArray = id_tc.keySet().toArray(new Integer[0]);
				Arrays.sort(idArray);
				for(int i = 0; i < idArray.length; i ++){
					int id = idArray[i];
					TestCase tc = id_tc.get(id);
					String length = tc.length;
					double CD = tc.CI;
					
					String input = getInput(length_CD_inputList, length, CD);
					if(input == null){
						missedID.add(id);
					}else{
						id_input.put(id, input);
					}
				}
				
				System.out.println("missed test cases after processing:" + missedID.size());
				
				StringBuilder sb = new StringBuilder();
				sb.append("ID\tInput\n");
				for(int i = 0; i < idArray.length; i ++){
					int id = idArray[i];
					String input = id_input.get(id);
					if(input != null){
						sb.append(id).append("\t").append(input).append("\n");	
					}					
				}				
				
				//5. save the id and input mappings
				String saveFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/" 
					+ date + "/TestPool_Alpha/ID_Input_Left.txt";
				Logger.getInstance().setPath(saveFile, false);
				Logger.getInstance().write(sb.toString());
				Logger.getInstance().close();				
				sb.setLength(0);				
				
				//6. save the missed ids
				saveFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/" 
					+ date + "/TestPool_Alpha/MissedID_Left.txt";
				for(int i = 0; i < missedID.size(); i ++){
					sb.append(missedID.get(i)).append("\n");
				}								
				Logger.getInstance().setPath(saveFile, false);
				Logger.getInstance().write(sb.toString());
				Logger.getInstance().close();				
			} catch (NumberFormatException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public static void mapIDInputs(String date, boolean containHeader, String alpha, 
			String tc_min, String tc_max){
		String testPool_old = "src/ccr/experiment/Context-Intensity_backup/TestHarness/" 
			+ date + "/TestPool_Alpha/TestPool_old.txt" ;
		
		String testPool_new  = "src/ccr/experiment/Context-Intensity_backup/TestHarness/" 
			+ date + "/TestPool_Alpha/TestPool_"+ alpha + "_" + tc_min + "_" + tc_max+".txt" ;
		
		File tmp_1 = new File(testPool_old);
		File tmp_2 = new File(testPool_new);
		if(tmp_1.exists() && tmp_2.exists()){
			//ID of test cases-> test cases
			HashMap<Integer, TestCase> id_tc = new HashMap<Integer, TestCase>();
			try {
				
				//1. read the old test pool
				BufferedReader br = new BufferedReader(new FileReader(testPool_old));		
				if(containHeader)
					br.readLine();
				
				String str = null;
				while((str = br.readLine())!= null){
					String[] strs = str.split("\t");
					if(strs.length > 2){
						int id = Integer.parseInt(strs[0]);
						String length = strs[1];
						String cd = strs[2];
						
						TestCase tc = new TestCase();
						tc.index = "" + id;
						tc.length = "" + Double.parseDouble(length);
						tc.CI = Double.parseDouble(cd);
						id_tc.put(id, tc);	
					}
				}
				br.close();
				
				
				//length -> CD* -> TestCases*
				HashMap<String, HashMap<Double, ArrayList<String>>> length_CD_inputList = new 
					HashMap<String, HashMap<Double,ArrayList<String>>>();
								
				//2. read the new test pool
				int counter = 0;
				BufferedReader br_1 = new BufferedReader(new FileReader(testPool_new));
				if(containHeader)
					br_1.readLine();
				
				while((str = br_1.readLine())!= null){
					String[] strs = str.split("\t");
					if(strs.length > 2){
						String input = strs[0];
						String length = "" + Double.parseDouble(strs[1]);
						double cd = Double.parseDouble(strs[2]);
						
						if(length_CD_inputList.containsKey(length)){
							HashMap<Double, ArrayList<String>> CD_inputList = length_CD_inputList.get(length);
							ArrayList<String> inputList = null;
							
							if(CD_inputList.containsKey(cd)){
								inputList = CD_inputList.get(cd);
							}else{
								inputList = new ArrayList<String>();
							}	
							
							inputList.add(input);
							CD_inputList.put(cd, inputList);
							length_CD_inputList.put(length, CD_inputList);
							counter ++;
						}else{
							ArrayList<String> inputList = new ArrayList<String>();
							inputList.add(input);
							
							HashMap<Double, ArrayList<String>> CD_inputList = new 
							HashMap<Double, ArrayList<String>>();
							CD_inputList.put(cd, inputList);
							length_CD_inputList.put(length, CD_inputList);
							counter ++;
						}
					}
				}
				br_1.close();
				System.out.println(counter);
				
				//3. map the id of test cases and inputs
				HashMap<Integer, String> id_input = new HashMap<Integer, String>(); //keep the mapping between ids of test cases and inputs
				ArrayList<Integer> missedID = new ArrayList<Integer>(); //keep all the missed test cases
				
				Integer[] idArray = id_tc.keySet().toArray(new Integer[0]);
				Arrays.sort(idArray);
				for(int i = 0; i < idArray.length; i ++){
					int id = idArray[i];
					TestCase tc = id_tc.get(id);
					String length = tc.length;
					double CD = tc.CI;
					
					String input = getInput(length_CD_inputList, length, CD);
					if(input == null){
						missedID.add(id);
					}else{
						id_input.put(id, input);
					}
				}
				
				System.out.println("missed test cases before processing:" + missedID.size());

//				//4. for these missed test cases
//				for(int i = 0; i < missedID.size(); i ++){
//					int id = missedID.get(i);
//					TestCase tc = id_tc.get(id);
//					String length = tc.length;
//					double CD = tc.CI;
//					
//					String input = null;
//					//first attempt : fix the CD but increasing CD
//					double len = Double.parseDouble(length);
//					double k = 1.0;
//					do{
//						input = getInput(length_CD_inputList, ""+ (len + k) , CD);
//						k ++;
//					}while((len + k) < 31.0 && input == null);
//					
//					if(input != null){
//						id_input.put(id, input);
//						missedID.remove(i);
//						i --; //2010-01-21: keep this index unchanged
//					}else{
//						//second attempt: fix the CD but decreasing CD
//						k = 1.0;
//						do{
//							input = getInput(length_CD_inputList, "" + (len - k), CD);
//							k ++;
//						}while((len - k) >= CD && input == null);
//						
//						if(input != null){
//							id_input.put(id, input);
//							missedID.remove(i);
//							i --;
//						}
//					}
//				}
//				System.out.println("missed ids after processing:" + missedID.size());
				
				StringBuilder sb = new StringBuilder();
				sb.append("ID\tInput\n");
				for(int i = 0; i < idArray.length; i ++){
					int id = idArray[i];
					String input = id_input.get(id);
					if(input != null){
						sb.append(id).append("\t").append(input).append("\n");	
					}					
				}				
				
				
				//5. save the id and input mappings
				String saveFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/" 
					+ date + "/TestPool_Alpha/ID_Input.txt";
				Logger.getInstance().setPath(saveFile, false);
				Logger.getInstance().write(sb.toString());
				Logger.getInstance().close();				
				sb.setLength(0);				
				
				//6. save the missed ids
				saveFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/" 
					+ date + "/TestPool_Alpha/MissedID.txt";
				for(int i = 0; i < missedID.size(); i ++){
					sb.append(missedID.get(i)).append("\n");
				}								
				Logger.getInstance().setPath(saveFile, false);
				Logger.getInstance().write(sb.toString());
				Logger.getInstance().close();				
			} catch (NumberFormatException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public static String getInput(HashMap<String, HashMap<Double, ArrayList<String>>> length_CD_inputList, String length, double CD){
		String input = null;
		
		if(length_CD_inputList.containsKey(length)){
			HashMap<Double, ArrayList<String>>  CD_inputList = length_CD_inputList.get(length);
			if(CD_inputList.containsKey(CD)){
				ArrayList<String> inputList = CD_inputList.get(CD);
				if(inputList.size() > 0){	
					Random rand = new Random();
					int k = rand.nextInt(inputList.size());
					input = inputList.get(k);					
					inputList.remove(k); //remove the used inputs
				}
			}
		}
		return input;
	}
	
	public static void main(String[] args){
		String instruction = args[0];
		if(instruction.equals("getMissingFault")){
			String date = args[1]; //20091230
			String detectedFaultFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
				+ date + "/detectedFaultList.txt";
			boolean containHeader = true;
			String saveFile ="src/ccr/experiment/Context-Intensity_backup/TestHarness/"
				+ date + "/MissingFaultList.txt";
			FileOperator.getMissingFault(detectedFaultFile, containHeader, saveFile);			
		}else if(instruction.equals("saveFaultList")){
			String date = args[1];
			String srcFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
				+ date + "/NonEquivalentFaults.txt";
			String saveFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
				+ date + "/NonEquivalentFaults.txt";
			boolean containHeader = true;
			FileOperator.saveFaultList(srcFile, containHeader, saveFile);
		}else if(instruction.equals("mergeFiles")){
			String date = args[1];
			String alpha = args[2];
			boolean containHeader = true;
			String prefix = "TestPool_" + alpha;
			FileOperator.mergeFiles(date, prefix);
		}else if(instruction.equals("mapIDInputs")){
			String date = args[1];
			boolean containHeader = true;
			String alpha = args[2];
			String tc_min = args[3];
			String tc_max = args[4];
//			FileOperator.mapIDInputs(date, containHeader, alpha, tc_min, tc_max);
			FileOperator.mapIDInputs_left(date, containHeader, alpha, tc_min, tc_max);
		}
		
		
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
	}
}
