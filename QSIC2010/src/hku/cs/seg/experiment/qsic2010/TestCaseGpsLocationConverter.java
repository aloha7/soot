package hku.cs.seg.experiment.qsic2010;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashSet;

public class TestCaseGpsLocationConverter {
	
	public static void shatterTestCase(String iFilename, int caseId) {
		try {
			BufferedReader br = new BufferedReader(new FileReader(iFilename + ".pool"));
			String str = null;
			String[] strs = null;
			while ((str = br.readLine()) != null) {
				strs = str.split("\t");
				if (Integer.parseInt(strs[0]) == caseId) 
					break;
			}
			br.close();
			
			if (str != null) {			
				BufferedWriter bw = new BufferedWriter(new FileWriter(iFilename + "-sc" + caseId + ".pool"));
				int seqNum = Integer.parseInt(strs[1]);
				
				for (int i = 0; i < seqNum; i++) {
					bw.write(String.valueOf(i + 1) + "\t1");
					for (int j = 2 + i * 13; j < 15 + i * 13; j++) {
						bw.write("\t" + strs[j]);
					}
					bw.write("\t0\n");
				}
				bw.close();				
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void combineAndShatterFirstLocation(String iFilename, HashSet<Integer> caseIds) {
		try {
			BufferedReader br = new BufferedReader(new FileReader(iFilename + ".pool"));
			BufferedWriter bw = new BufferedWriter(new FileWriter(iFilename + "-cbs" + caseIds.size() + ".pool"));

			String str = null;
			String[] strs = null;
			while ((str = br.readLine()) != null) {
				strs = str.split("\t");
				if (!caseIds.contains(Integer.parseInt(strs[0]))) 
					continue;
				bw.write(strs[0] + "\t1");
				for (int j = 2; j < 15; j++) {
					bw.write("\t" + strs[j]);
				}
				bw.write("\t0\n");				
			}
			br.close();
			bw.close();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void combineFirstLocation(String iFilename, HashSet<Integer> caseIds) {
		try {
			BufferedReader br = new BufferedReader(new FileReader(iFilename + ".pool"));
			BufferedWriter bw = new BufferedWriter(new FileWriter(iFilename + "-cb" + caseIds.size() + ".pool"));
			bw.write("1\t" + caseIds.size());

			String str = null;
			String[] strs = null;
			while ((str = br.readLine()) != null) {
				strs = str.split("\t");
				if (!caseIds.contains(Integer.parseInt(strs[0]))) 
					continue;
				for (int j = 2; j < 15; j++) {
					bw.write("\t" + strs[j]);
				}
			}
			br.close();
			
			bw.write("\t0\n");
			bw.close();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	
	public static void main(String[] args) {
//		shatterTestCase("C:\\jack\\workspace\\QSIC2010_PRI\\Testsuites\\100gpx", 1);
		int[] ids = new int[]{1, 3, 5, 6};
		HashSet<Integer> hs = new HashSet<Integer>();
		for (int id : ids) {
			hs.add(id);	
		}		
		combineFirstLocation("C:\\jack\\workspace\\QSIC2010_PRI\\Testsuites\\100gpx", hs);
	}

}
