package hku.cs.seg.experiment.qsic2010.io;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import hku.cs.seg.experiment.core.ITestCase;
import hku.cs.seg.experiment.qsic2010.CBRInput;
import hku.cs.seg.experiment.qsic2010.CBRTestSuite;

public class CalcCD {
	public static final double MaxMovingSpeedDegree = 0.0000009 * 30 * 10;
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		CBRTestSuite ts = new CBRTestSuite();
		ts.readFromTextFile("C:\\jack\\workspace\\QSIC2010\\Testsuites\\100.pool");
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter("C:\\jack\\workspace\\QSIC2010\\Testsuites\\100.info"));
			
			for (ITestCase i : ts) {
				CBRInput input = (CBRInput)i.getTestInput();
				 if (input.size() <= 1) {
					 bw.write("0\n");
					 continue;
				 }
				 
				 String gpsLocation = input.get(0).getUserPreference().getGpsLocation();
				 String[] tmp = gpsLocation.split(",");
				 double lon1 = Double.parseDouble(tmp[0]);
				 double lat1 = Double.parseDouble(tmp[1]);
				 int cd = 0;
				 for (int j = 1; j < input.size(); j++) {
					 gpsLocation = input.get(j).getUserPreference().getGpsLocation();
					 tmp = gpsLocation.split(",");
					 double lon2 = Double.parseDouble(tmp[0]);
					 double lat2 = Double.parseDouble(tmp[1]);
					 if (Math.sqrt((lon1 - lon2) *(lon1 - lon2) + (lat1 - lat2) * (lat1 - lat2)) / MaxMovingSpeedDegree > 0.3) {
						 cd++;
					 }
				 }
				 
				 bw.write(cd + "\n");
			}
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

}
