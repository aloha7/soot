package hku.cs.seg.experiment.qsic2010;

import hku.cs.seg.experiment.core.ExecutionTimer;
import hku.cs.seg.experiment.core.LogWriter;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TestMain {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String testsuiteFilename = args[0];
		String logname = "C:\\Jack\\workspace\\QSIC2010\\logs\\" + new SimpleDateFormat("[yyyy-MM-dd] HH-mm-ss").format(new Date(System.currentTimeMillis())) + ".log";
		LogWriter.me().init(logname);
		
		ExecutionTimer.me().startCounter("Total");
		TestSuiteManager.loadAndRun(testsuiteFilename);
		
		System.out.println("Program init:" + ExecutionTimer.me().endCounter("prog_init") / 1000.0);
		System.out.println("Query result:" + ExecutionTimer.me().endCounter("query_result") / 1000.0);
		System.out.println("User confirm:" + ExecutionTimer.me().endCounter("user_confirm") / 1000.0);
		System.out.println("Similarity computing:" + ExecutionTimer.me().endCounter("siml_comp") / 1000.0);
		System.out.println("Total:" + ExecutionTimer.me().endCounter("Total") / 1000.0);
	}

}
