package context.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import context.test.util.Constant;
import context.test.util.Logger;

public class OutputManager {

	/**get the list of failed test cases for golden versions, save it
	 * to a specified file 
	 * 
	 * @param saveFile
	 */
	public void saveFailedTestCase(String saveFile){
		
		StringBuffer sb = new StringBuffer();
		File sourceDir = new File(Constant.baseFolder + "/test/output/FailureRate/0");		
		File[] outputs = sourceDir.listFiles();
		for(File output: outputs){
			if(output.length() > 0){
				String name = output.getName();
				sb.append(name.substring(0, name.indexOf("."))+"\t");
			}
		}
		
		Logger.getInstance().setPath(saveFile, false);
		Logger.getInstance().write(sb.toString());
		Logger.getInstance().close();
	}
	
	public String[] getFailedTestCase(String saveFile){
		String[] list = null;

		try {
			BufferedReader br = new BufferedReader(new FileReader(saveFile));
			StringBuilder sb = new StringBuilder();
			String str = null;
			while((str = br.readLine())!=null){
				sb.append(str);
			}
			
			str = sb.toString();
			list = str.split("\t");  
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return list;
	}
	
	
	/**Compare the stability of faults detected by test cases  
	 * 
	 * @param srcPath: contain less files
	 * @param destPath: contain more files
	 * @return
	 */
	public ArrayList changeList(String srcPath, String destPath){
		ArrayList diffList = new ArrayList();

		File[] srcDir = new File(srcPath).listFiles();
		
		File[] destDir = new File(destPath).listFiles();		
		ArrayList destList = new ArrayList();
		for(File file: destDir)
			destList.add(file.getName());
		
		int i = 0; 
		for( ; i < srcDir.length; i++){
			File file = srcDir[i];
			int index = destList.indexOf(file.getName());
			if(index > -1){
				long srcLength = file.length();
				long destLength = destDir[index].length();
				if(srcLength!=destLength){
					diffList.add(file.getName());
				}
			}
		}
		
			
		return diffList;
	}
	
	public static void main(String[] args){
		OutputManager manager = new OutputManager();
//		String saveFile = Constant.baseFolder + "/test/output/failedTestCase.txt";
//		manager.saveFailedTestCase(saveFile);
//		String[] strs = manager.getFailedTestCase(saveFile);
//		System.out.println("0");

		String srcPath = Constant.baseFolder + "/test/output/FailureRate/backup/oracle_100";
		String destPath = Constant.baseFolder + "/test/output/FailureRate/0";
		
		StringBuilder sb = new StringBuilder();
		ArrayList changeList = manager.changeList(srcPath, destPath);
		sb.append("change list:" + changeList.size()+"\n");
		
		for(Object file: changeList){
			sb.append(file+"\n");
		}
		
	}
}
