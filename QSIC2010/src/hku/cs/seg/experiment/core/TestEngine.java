package hku.cs.seg.experiment.core;

/**
 * @author Jack
 * 
 */
public class TestEngine {
	private final int IntervalScalingFactor = 80;
	
	private IProgram m_Program;
	private ITestSuite m_TestSuite;
	private ProgramRunnable m_ProgramRunnable;
	private long m_MaxRunningTime;
	private long m_SleepInterval;
	
	
	public enum ProgramTerminationType {
		Normal, InternalFailure, Timeout
	}
	
	public interface ProgramRunningCallback {
		boolean invoke(ITestCase testCase, ITestOutput testOutput, ProgramTerminationType programTerminationType);
	}
	
	private class TestReportGenerator implements ProgramRunningCallback {
		private TestReport m_TestReport = new TestReport();
		
		public boolean invoke(ITestCase testCase, ITestOutput testOutput, ProgramTerminationType programTerminationType) {
			// TODO Auto-generated method stub
			
			
			return false;
		}
		
	}
	
	private class ProgramRunnable implements Runnable {
		private IProgram m_Program;
		private ITestInput m_TestInput;
		private ITestOutput m_TestOutput;
		
		public ProgramRunnable() {
			
		}
		public void setProgram(IProgram program) {
			m_Program = program;
		}
		public void setInput(ITestInput testInput) {
			m_TestInput = testInput;
		}
		public ITestOutput getOutput() {
			return m_TestOutput;
		}		
		public void run() {
			try {
				m_TestOutput = m_Program.run(m_TestInput);
			} catch (Exception e) {
				m_TestOutput = null;
			}
		}		
	}
	
	public TestEngine() {
		m_ProgramRunnable = new ProgramRunnable();
		
	}
	
	public IProgram getProgram() {
		return m_Program;
	}
	
	public void setProgram(IProgram program) {
		m_Program = program;
		m_ProgramRunnable.setProgram(program);
	}
	
	public ITestSuite getTestSuite() {
		return m_TestSuite;
	}
	
	public void setTestSuite(ITestSuite testSuite) {
		m_TestSuite = testSuite;
	}
	
	public void setMaxRunningTime(long maxRunningTime) {
		m_MaxRunningTime = maxRunningTime;
		//m_SleepInterval = (maxRunningTime > IntervalScalingFactor) ? maxRunningTime / IntervalScalingFactor : 1;
		m_SleepInterval = 5;
	}
	
	public TestReport runProgram() {
		TestReport testReport = new TestReport();
		
		return testReport;		
	}

	public void runProgram(long maxRunningTime, ProgramRunningCallback programRunningCallback) {
		ITestOutput testOutput;
		long startTime = 0;		
		ProgramTerminationType ptt;
		
		try {
			m_Program.init();
		} catch (Exception e) {
			System.out.println(e.getMessage());
			for (ITestCase testCase : m_TestSuite) {
				if (programRunningCallback != null) {
					programRunningCallback.invoke(testCase, null, ProgramTerminationType.InternalFailure);
				}
			}
			return;
		}
		for (ITestCase testCase : m_TestSuite) {
			m_Program.preRun();
			testOutput = null;
			ptt = ProgramTerminationType.InternalFailure;
			try {
				testOutput = m_Program.run(testCase.getTestInput());
				ptt = ProgramTerminationType.Timeout.Normal;
			} catch (Exception e) {
				System.out.println(e.getMessage());
			}
			
//			m_ProgramRunnable.setInput(testCase.getTestInput());
//			Thread t = new Thread(m_ProgramRunnable);
//			t.start();
//			startTime = System.currentTimeMillis();
//			while (t.isAlive() && System.currentTimeMillis() - startTime < maxRunningTime) {
//				try {
//					Thread.sleep(m_SleepInterval);
//				} catch (InterruptedException e) {
//					break;
//				}
//			}
//			
//			if (t.isAlive()) {
//				t.stop();
//				testOutput = null;
//				ptt = ProgramTerminationType.Timeout;
//			} else {
//				testOutput = m_ProgramRunnable.getOutput();
//				ptt = (testOutput == null) ?  ProgramTerminationType.InternalFailure : ProgramTerminationType.Normal; 
//			}

			if (programRunningCallback != null) {
				programRunningCallback.invoke(testCase, testOutput, ptt);
			}
		}	
	}

//	public void runProgram(long maxRunningTime, ProgramRunningCallback programRunningCallback) {
//		ITestOutput testOutput;		
//		m_Program.init();
//		for (ITestCase testCase : m_TestSuite) {
//			m_Program.preRun();
//			try {
//				testOutput = m_Program.run(testCase.getTestInput());
//			} catch (Throwable e) {
//				testOutput = null;
//			}
//			if (programRunningCallback != null) {
//				programRunningCallback.invoke(testCase, testOutput);
//			}
//		}		
//	}
}