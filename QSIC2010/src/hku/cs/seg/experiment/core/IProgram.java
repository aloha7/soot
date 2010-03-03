/**
 * 
 */
package hku.cs.seg.experiment.core;

/**
 * @author Jack
 *
 */
public interface IProgram {
	void init();
	void preRun();
	ITestOutput run(ITestInput testInput);
}