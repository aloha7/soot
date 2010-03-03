/**
 * 
 */
package hku.cs.seg.experiment.core;

import java.util.Collection;


/**
 * @author Jack
 *
 */
public interface ITestSuite extends Collection<ITestCase> {
	boolean readFromTextFile(String filename);
	boolean writeToTextFile(String filename, boolean append);
}
