package hku.cs.seg.experiment.core;

/**
 * 
 * @author Jack
 *
 */
public class TestReportItem {
	private ITestCase m_TestCase;
	private ITestOutput m_TestOutput;
	private boolean m_IsPassed;
	
	public TestReportItem(ITestCase testCase, ITestOutput testOutput) {
		m_TestCase = testCase;
		m_TestOutput = testOutput;
		m_IsPassed = testCase.getTestOracle().isPassed(testOutput);
	}
	
//	public void setTestCase(ITestCase testCase) {
//		this.m_TestCase = m_TestCase;
//	}
	
	public ITestCase getTestCase() {
		return m_TestCase;
	}
	
//	public void setTestOutput(ITestOutput m_TestOutput) {
//		this.m_TestOutput = m_TestOutput;
//	}
	
	public ITestOutput getTestOutput() {
		return m_TestOutput;
	}
	
//	public void setIsPassed(boolean m_IsPassed) {
//		this.m_IsPassed = m_IsPassed;
//	}
	
	public boolean isIsPassed() {
		return m_IsPassed;
	}
}
