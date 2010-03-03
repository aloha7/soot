package hku.cs.seg.experiment.qsic2010;

import hku.cs.seg.experiment.core.ExecutionTimer;
import hku.cs.seg.experiment.core.LogWriter;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;


import residue.ListHits;

public class TestGenerator {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String tsFilename = args[2] + ".pool";
		String infoFilename = args[2] + ".info";
		String exFilename = args[3];
		ExecutionTimer.me().startCounter("Total");
		String logname = "C:\\Jack\\workspace\\QSIC2010\\logs\\" + new SimpleDateFormat("[yyyy-MM-dd] HH-mm-ss").format(new Date(System.currentTimeMillis())) + ".log";
//		LogWriter.me().init(logname);
		
		TestSuiteManager.NubmerOfTestCases = Integer.parseInt(args[1]);
		IGpsLocationGenerator glg = null;
		
		if (args[5].equals("DR")) {
			glg = new DirectedRandomGpsLocationGenerator();			
		} else if (args[5].equalsIgnoreCase("GPX")) {
			glg = new GpxGpsLocationGenerator();
		} else if (args[5].equalsIgnoreCase("ABT")) {
			glg = new ArbitraryGpsLocationGenerator();
		}
		
		TestSuiteManager.generateAndPersist(Integer.parseInt(args[0]), tsFilename, infoFilename, exFilename, args[4].equals("1"), glg);
		System.out.println("Program init:" + ExecutionTimer.me().endCounter("prog_init") / 1000.0);
		System.out.println("Query result:" + ExecutionTimer.me().endCounter("query_result") / 1000.0);
		System.out.println("User confirm:" + ExecutionTimer.me().endCounter("user_confirm") / 1000.0);
		System.out.println("Similarity computing:" + ExecutionTimer.me().endCounter("siml_comp") / 1000.0);
		System.out.println("Total:" + ExecutionTimer.me().endCounter("Total") / 1000.0);
	}
}
