package hku.cs.seg.experiment.qsic2010.db;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Statement;

public class RawDataDumper {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String resultFile = args[0];
		
		try {
			BufferedReader br = new BufferedReader(new FileReader(resultFile));
			String str = null;
			
			Statement stmt = MySQLDatabase.Me().getStatement();
			while ((str = br.readLine()) != null) {
				String[] strs = str.split("\t");
				
				
			}
			
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		

	}

}
