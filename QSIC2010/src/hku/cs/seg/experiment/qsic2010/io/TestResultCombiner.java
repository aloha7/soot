package hku.cs.seg.experiment.qsic2010.io;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class TestResultCombiner {

	public static void combineAll(String resultPath) {
		//String resultPath = args[0];
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(resultPath + "\\binder.log"));
			
			File[] results = new File(resultPath).listFiles();
			for (File result : results) {
				if (result.getName().equals("binder.log")) continue;
				System.out.println("Adding " + result.getName());
				BufferedReader br = new BufferedReader(new FileReader(result.getPath())) ;
				String str = "";
				while ((str = br.readLine()) != null) {
					bw.write(str);
					bw.write("\n");
				}
			}
			
			bw.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	public static void combineSelected(String resultPath, Set<Integer> selectedMutants) {
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(resultPath + "\\binder_sel.log"));
			
			File[] results = new File(resultPath).listFiles();
			for (File result : results) {
				if (result.isDirectory()) continue;
				String name = result.getName();				
				int pos = name.indexOf(".");
				if (pos < 0) continue;
				name = name.substring(0, pos);
				
				try {
					if (!selectedMutants.contains(Integer.parseInt(name))) continue;
				} catch (NumberFormatException e) {
					continue;
				}
				
				System.out.println("Adding " + result.getName());
				BufferedReader br = new BufferedReader(new FileReader(result.getPath())) ;
				String str = "";
				while ((str = br.readLine()) != null) {
					bw.write(str);
					bw.write("\n");
				}
			}
			
			bw.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
//		combineAll("C:\\Jack\\workspace\\QSIC2010_PRI\\experiments\\testpool\\1000-ABT\\result");
//		int[] sel = new int[]{73,78,89,93,144,162,230,237,260,262,271,276,278,403,418,420,421,422};
		int[] sel = new int[]{73,78,89,93,162,230,237,260,262,271,276,278,389,391,403,407,409,410,411,412,413,415,416,417,418,420,421,422,441,449,450,453,460,462,464};
		HashSet<Integer> set = new HashSet<Integer>();
		for (int i : sel) set.add(i);
		
		combineSelected("C:\\Jack\\workspace\\QSIC2010_PRI\\experiments\\testpool\\1000-ABT\\result", set);
	}

}
