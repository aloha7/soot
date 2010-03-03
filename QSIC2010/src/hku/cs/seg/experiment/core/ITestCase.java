/**
 * 
 */
package hku.cs.seg.experiment.core;

/**
 * @author Jack
 *
 */
public interface ITestCase {
	int getTestCaseId();
	
	ITestInput getTestInput();
	ITestOracle getTestOracle();
}
