package ccr.test;

import java.io.BufferedWriter;
import java.util.*;

public class Oracle {
	
	private static Oracle m_Oracle;

	public static Oracle getInstance(String appClassName, TestSet t) {
		if (m_Oracle == null) {
			m_Oracle = new Oracle(appClassName, t);
		}
		return m_Oracle;
	}
	
	
	private final TestSet testpool;
	private HashMap outcome;
	
	public Oracle(String appClassName, TestSet t) {
		
		testpool = t;
		outcome = new HashMap();
		for (int i = 0; i < testpool.size(); i++) {
			String testcase = testpool.get(i);
			outcome.put(testcase, TestDriver.run(appClassName, testcase));
		}
	}
	
	public Object getOutcome(String testcase) {
		
		return outcome.get(testcase);
	}

}
