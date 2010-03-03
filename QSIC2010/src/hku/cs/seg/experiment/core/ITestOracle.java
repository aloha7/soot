/**
 * 
 */
package hku.cs.seg.experiment.core;

/**
 * @author Jack
 *
 */
public interface ITestOracle {
	boolean isOutputApplicable(ITestOutput output);	
	boolean isPassed(ITestOutput testOutput);
	//double howSimilar(ITestOutput output);
}