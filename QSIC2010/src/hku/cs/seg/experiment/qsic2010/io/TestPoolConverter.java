package hku.cs.seg.experiment.qsic2010.io;

import hku.cs.seg.experiment.core.ITestCase;
import hku.cs.seg.experiment.core.SimpleTestCase;
import hku.cs.seg.experiment.qsic2010.CBRInput;
import hku.cs.seg.experiment.qsic2010.CBRTestSuite;
import hku.cs.seg.experiment.qsic2010.TestContextVariable;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class TestPoolConverter {

	public static void convertToNonConfirm(String poolFilename) {
		CBRTestSuite ts = new CBRTestSuite();
		ts.readFromTextFile(poolFilename + ".pool");
		
		for (ITestCase item : ts) {
			CBRInput input = (CBRInput)((SimpleTestCase)item).getTestInput();
			for (TestContextVariable cv : input) {
				cv.setIsUserConfirmed(false);
			}
		}
		
		ts.writeToTextFile(poolFilename + "-noconf.pool", false);
	}
	
	public static void extractGpsSequence(String poolFilename) {
		String poolname = poolFilename + ".pool";
		String gpsname = poolFilename + ".gpseq";
		
		CBRTestSuite ts = new CBRTestSuite();
		ts.readFromTextFile(poolname);

		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(gpsname));
			
			for (ITestCase item : ts) {
				SimpleTestCase c = (SimpleTestCase)item;
				CBRInput input = (CBRInput)c.getTestInput();
				bw.write(c.getTestCaseId() + "\t" + input.size());
				for (TestContextVariable cv : input) {
					String[] gps = cv.getUserPreference().getGpsLocation().split(",");					
					bw.write("\t" + gps[0] + "\t" + gps[1]);
				}
				bw.write("\n");
			}
			
			bw.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void extractTestCase2TestPool(String inpFile, String outFile, Set<Integer> caseIds) {
		try {
			BufferedReader br = new BufferedReader(new FileReader(inpFile));
			BufferedWriter bw = new BufferedWriter(new FileWriter(outFile));
			
			String str = null;
			while ((str = br.readLine()) != null) {
				String[] strs = str.split("\t");
				if (!caseIds.contains(Integer.parseInt(strs[0]))) continue;
				
				bw.write(strs[0]);
				for (int i = 1; i < strs.length; i++) {
					bw.write("\t" + strs[i]);
				}
				bw.write("\n");
			}
			
			br.close();
			bw.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
//		extractGpsSequence("C:\\Jack\\workspace\\QSIC2010_PRI\\Testsuites\\100_ABT_test");
//		extractGpsSequence("C:\\Jack\\workspace\\QSIC2010_PRI\\experiments\\testpool\\1000-ABT\\1000_ABT");
//		extractGpsSequence("C:\\Jack\\workspace\\QSIC2010_PRI\\Testsuites\\1000impori");
//		extractGpsSequence("C:\\jack\\workspace\\QSIC2010_PRI\\experiments\\testpool\\impori-nonkill\\impori-2000nonkill");
//		int[] cids = {632,553,185,273,549,961,15,242,245,430,507,610,680,897,330,460,620,695,861,41,966,967,968,971,972,975,978,979,982,983,985,986,987,990,992,993,994,995,997,998};
		int[] cids = {41, 986};
		HashSet<Integer> caseIds = new HashSet<Integer>();
		for (int i : cids) {
			caseIds.add(i);
		}
		extractTestCase2TestPool("C:\\Jack\\workspace\\QSIC2010_PRI\\experiments\\testpool\\1000-ABT\\1000_ABT.pool",
				"C:\\Jack\\workspace\\QSIC2010_PRI\\experiments\\testpool\\1000-ABT\\1000_ABT_id41.pool",
				caseIds);
	}
}
