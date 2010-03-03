package hku.cs.seg.experiment.core;

import java.util.ArrayList;
import java.util.Collection;

/**
 * 
 * @author Jack
 *
 */
public class TestReport {
	public ArrayList<TestReportItem> m_TestReportItems;
	
	public TestReport() {
		m_TestReportItems = new ArrayList<TestReportItem>();
	}
	
	public Collection<TestReportItem> getPassed() {
		return null;
	}
	
	public Collection<TestReportItem> getFailed() {
		return null;
	}
	
}
