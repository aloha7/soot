package ccr.test;

import java.util.*;

public class Oracle {
	
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
