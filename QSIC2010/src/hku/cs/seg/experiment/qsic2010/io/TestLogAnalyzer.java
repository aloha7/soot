package hku.cs.seg.experiment.qsic2010.io;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.HashMap;

public class TestLogAnalyzer {

	public static void countFailureRatePerMutant(String logFilename, String outFilename) {
		// Count, Pass, Fail, Fail-N, Fail-I
		HashMap<Integer, int[]> data = new HashMap<Integer, int[]>();
		
		try {
			BufferedReader br = new BufferedReader(new FileReader(logFilename));
			String str = null;
			
			while ((str = br.readLine()) != null) {
				String[] strs = str.split("\t");
				int mutId = Integer.parseInt(strs[0]);
				String rslt = strs[2];
				
				if (!data.containsKey(mutId)) {
					data.put(mutId, new int[5]);
				}
				int[] datum = data.get(mutId);
				datum[0]++;
				if (rslt.equalsIgnoreCase("Pass")) {
					datum[1]++;
				} else if (rslt.equalsIgnoreCase("Fail-N")) {
					datum[2]++;
				} else if (rslt.equalsIgnoreCase("Fail-I")) {
					datum[3]++;
				} else {
					datum[4]++;
				}
				//System.out.println(mutId);
			}
			
			br.close();
			
			Integer[] ids = data.keySet().toArray(new Integer[0]);
			Arrays.sort(ids);
			BufferedWriter bw = new BufferedWriter(new FileWriter(outFilename));
			for (int i = 0; i < ids.length; i++) {
				int[] datum = data.get(ids[i]);
				bw.write(ids[i] + "\t" + datum[0] + "\t" + datum[1] + "\t" + datum[2] + "\t" + datum[3] + "\t" + datum[4] + "\n");
			}
			
			bw.close();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		countFailureRatePerMutant("C:\\Jack\\workspace\\QSIC2010_PRI\\experiments\\testpool\\1000-ABT\\result\\binder.log", "C:\\Jack\\workspace\\QSIC2010_PRI\\experiments\\testpool\\1000-ABT\\result\\binder.report");
//		countFailureRatePerMutant("C:\\Jack\\workspace\\QSIC2010\\result\\3\\binder.log", "C:\\Jack\\workspace\\QSIC2010\\result\\3\\binder.report");
//		countFailureRatePerMutant("C:\\jack\\workspace\\QSIC2010_PRI\\testresult\\impori_1000\\binder.log", "C:\\jack\\workspace\\QSIC2010_PRI\\testresult\\impori_1000\\binder.report");
	}

}
