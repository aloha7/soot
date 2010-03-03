package hku.cs.seg.experiment.qsic2010;

import java.io.*;
import java.util.List;

import c7302.CityAcitivyRecommendation.Accommodation.PoiCase;

import hku.cs.seg.experiment.core.*;
import hku.cs.seg.experiment.qsic2010.CBRProgram.QueryResultInspector;

public class OutputGetter {
	
	private class SimpleInspector implements QueryResultInspector {
		private BufferedWriter bw;
		
		public SimpleInspector(BufferedWriter bw) {
			this.bw = bw;
		}

		public void doInspect(List<PoiCase> poiCases)  {
			if (poiCases == null || poiCases.size() == 0) return;
			try {
				bw.write("\t");
				
				boolean flag = false;
				for (PoiCase c : poiCases) {
					if (flag) {
						bw.write(",");
					}
					flag = true;
					bw.write(String.valueOf(c.getPoiID()));					
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void doit(String[] args) {
		String mutInfo = args[0];
		String testpoolFile = args[1]; // + ".pool";
		String outputFile = args[2];// + ".output";
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile));
			
			CBRTestSuite ts = new CBRTestSuite();
			ts.readFromTextFile(testpoolFile);
			
			CBRProgram program = new CBRProgram();
			
			SimpleInspector si = new SimpleInspector(bw);
			program.setQueryResultInspector(si);
			
			program.init();
			for (ITestCase testCase : ts) {
				try {
					bw.write(mutInfo + "\t" + String.valueOf(testCase.getTestCaseId()));
					
					program.preRun();
					program.run(testCase.getTestInput());
					
					bw.write("\n");
				} catch (Exception e) {
	
				}
			}	
			
			bw.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	public static void main(String[] args) {
//		new OutputGetter().doit(args);
		new OutputGetter().doit(new String[]{
				"403",
				"C:\\Jack\\workspace\\QSIC2010_PRI\\experiments\\testpool\\1000-ABT\\1000_ABT_id41.pool",
				"C:\\Jack\\workspace\\QSIC2010_PRI\\experiments\\testpool\\1000-ABT\\1000_ABT_id41_m403.output"
		});
//		new OutputGetter().doit(new String[]{
//		"C:\\Jack\\workspace\\QSIC2010_PRI\\Testsuites\\1000impori_mutsco.pool",
//		"C:\\Jack\\workspace\\QSIC2010_PRI\\logs\\1000impori_mutsco.output"
//});
	}
}
