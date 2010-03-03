package hku.cs.seg.experiment.core;

public class SimpleTestCase implements ITestCase {
	private int m_TestCaseId;
	private ITestInput m_TestInput;
	private ITestOracle m_TestOracle;
	
	public SimpleTestCase(int testCaseId, ITestInput testInput, ITestOracle testOracle) {
		m_TestCaseId = testCaseId;
		m_TestInput = testInput;
		m_TestOracle = testOracle;
	}
	
	public int getTestCaseId() {
		return m_TestCaseId;
	}

	public ITestInput getTestInput() {
		return m_TestInput;
	}

	public ITestOracle getTestOracle() {
		return m_TestOracle;
	}

}
