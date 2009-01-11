package context.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;

import context.test.util.Constant;
import context.test.util.Logger;



public class StatisticFactory {
		
	public void getFailureRate(int minVersion, int maxVersion){
		//1.compare mutant's outputs with oracle's.
		String oracleDir = Constant.baseFolder + "test/output/0";
		String mutantDir = null; 
		for(int i = minVersion; i <= maxVersion; i ++){
			mutantDir = Constant.baseFolder + "test/output/" + i;
			compareFiles(oracleDir, mutantDir);
		}
		
		//2.get failure rates and list all failed test cases
		StringBuffer sb = new StringBuffer();
		for(int i = minVersion; i <= maxVersion; i ++){
			int testSuiteSize = new File(oracleDir).list().length;
			 
			File[] failedTestCase = new File(Constant.baseFolder + "/test/result/" 
					+ i ).listFiles();
			if(failedTestCase!=null){
				int failedNumber = failedTestCase.length;
				
				sb.append(i + Constant.SEPERATOR + (double)failedNumber/(double)testSuiteSize 
						+ Constant.SEPERATOR );
				for(File temp: failedTestCase){
					sb.append(temp.getName().substring(0, temp.getName().lastIndexOf(".")) + 
							Constant.SEPERATOR);
				}
				sb.append(Constant.LINESHIFTER);	
			}
									
		}	
		
		if(sb.length()!=0){
			Logger log = Logger.getInstance();
			log.setPath(Constant.baseFolder + "test/FailureRate.txt" , true);
			log.write(sb.toString());		
			log.close();			
		}			
	}
	
	//Compare files between two different folders 
	private static void compareFiles(String folder1, String folder2){
		 
		 File dir = new File(folder2);
		 if(!dir.exists()){
			 return ;
		 }else{
			 File[] dest = dir.listFiles();			 			
			 Hashtable destHash = new Hashtable();
			 for(File temp:dest){
				 destHash.put(temp.getName(), temp);
			 }	
			 
			 File[] src = new File(folder1).listFiles();
			 Hashtable srcHash = new Hashtable();
			 for(File temp: src){			  
				 srcHash.put(temp.getName(), temp);
			 }
			 			 			
			 for(Enumeration e = srcHash.keys(); e.hasMoreElements();){
				 String fileName = (String)e.nextElement();
				 compareFile((File)srcHash.get(fileName), (File)destHash.get(fileName),
						 Constant.baseFolder + "/test/result/" + dir.getName() + "/" + fileName); 
			 }	
		 }		     	 				
	}

	private static void compareFile(File srcFile, File destFile, String reportFile){				
		 try{
			 Boolean labeled = false;
			 BufferedReader bs_src = new BufferedReader(new FileReader(srcFile));			 
			 BufferedReader bs_dest = new BufferedReader(new FileReader(destFile));
			 
			 String line_src = null;
			 String line_dest = null;
			 int num_line = 0;
			 
			 File report = new File(reportFile);
			 if(!report.getParentFile().exists())
				 report.getParentFile().mkdirs();
			  
			 StringBuffer sb = new StringBuffer();
			 while((line_src = bs_src.readLine())!= null){
				 line_dest = bs_dest.readLine();
				 num_line ++;
				 if(!line_src.equals(line_dest)){
					 
					 sb.append("LineNumber: " + num_line + "\n");
					 sb.append("<" + line_src + "\n");
					 sb.append("---------------------------------------------------------------\n");
					 sb.append(">" + line_dest + "\n");
				 }				 
			 }			
			 if(sb.length()!=0){
				 Logger log = Logger.getInstance();
				 log.setPath(reportFile, false);
				 log.write(sb.toString());
				 log.close();
			 }	
			 if(sb.length()==0){
				 File file = new File(reportFile);
				 file.delete();
			 }
		 }catch(Exception e){
			 System.out.println(e);
		 }		
	} 
	
	
	public static void main(String[] args){
		 
		 int i = 1;
		 int j = 5;
		 //double result = (double)i/(double)j;
		 System.out.println((double)i/(double)j);
		 
		 StatisticFactory sat = new StatisticFactory();
		 sat.getFailureRate(1, 40);
			 		 		 		
	    	 		 					 			 			 			 		
	}
		
}
