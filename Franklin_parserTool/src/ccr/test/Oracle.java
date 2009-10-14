package ccr.test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

import ccr.app.ApplicationResult;

public class Oracle {

	private static Oracle m_Oracle;

	public static Oracle getInstance(String appClassName, TestSet t) {
		if (m_Oracle == null) {
			m_Oracle = new Oracle(appClassName, t);
		}
		return m_Oracle;
	}

	
	/**2009-10-06: read oracles from the cache 
	 * 
	 * @param appClassName
	 * @param t
	 * @param date
	 * @return
	 */
	public static Oracle getInstance(String appClassName, TestSet t, String _oracleFile) {

		File oracleFile = new File(_oracleFile);
		if (oracleFile.exists()) {
			//read the oracle from files
			m_Oracle = new Oracle(oracleFile, t);
		} else {
			//save oracles into files
			try {
				m_Oracle = Oracle.getInstance(appClassName, t);

				StringBuilder sb = new StringBuilder();
				for (Iterator ite = m_Oracle.outcome.keySet().iterator(); ite
						.hasNext();) {
					String testcase = (String) ite.next();
					sb.append(testcase + "\t"
							+ m_Oracle.outcome.get(testcase).toString() + "\n");
				}

				BufferedWriter bw = new BufferedWriter(new FileWriter(
						oracleFile));
				bw.write(sb.toString());
				bw.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return m_Oracle;

	}

	private TestSet testpool;
	private HashMap outcome;

	private Oracle(String appClassName, TestSet t) {

		testpool = t;
		outcome = new HashMap();
		for (int i = 0; i < testpool.size(); i++) {
			String testcase = testpool.get(i);
			outcome.put(testcase, TestDriver.run(appClassName, testcase));
		}
	}
	
	/**2009-10-06: read oracles from cached files
	 * 
	 * @param oracleFile
	 * @param t
	 */
	private Oracle(File oracleFile, TestSet t){
		testpool = t;
		outcome = new HashMap();
		
		try {
			BufferedReader br = new BufferedReader(new FileReader(
					oracleFile));
			String line = null;
			while((line = br.readLine())!=null){
				String[] tmp = line.split("\t");
				String testcase = tmp[0];

				String[] outputs = tmp[1].split(" ");
				int moved = Integer.parseInt(outputs[0].substring(outputs[0].indexOf(":")+ ":".length()));
				int reliable = Integer.parseInt(outputs[1].substring(outputs[1].indexOf(":")+ ":".length()));
				ApplicationResult result = new ApplicationResult(moved, reliable);
				
				outcome.put(testcase, result);
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public Object getOutcome(String testcase) {

		return outcome.get(testcase);
	}

	public int getOracleSize() {
		return outcome.size();
	}

}
