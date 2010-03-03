package hku.cs.seg.experiment.qsic2010;

import java.util.*;
import c7302.CityAcitivyRecommendation.Accommodation.*;
import hku.cs.seg.experiment.core.*;

public class CBRProgram implements IProgram {

	private QueryAccommodation m_QueryAccom;
	private int m_QueryResultCount;
	private int m_UserConfirmCount;
	
	public interface QueryResultInspector {
		public void doInspect(List<PoiCase> poiCases); 
	}
	
	public int getQueryResultCount() {
		return m_QueryResultCount;
	}
	
	public int getUserConfirmCount() {
		return m_UserConfirmCount;
	}
	
	public CBRProgram() {
	}

	public void init() {
		ExecutionTimer.me().startCounter("prog_init");
		//c7302.CityAcitivyRecommendation.main.MySQLDBserver.init();
		m_QueryAccom = new QueryAccommodation("");	
		ExecutionTimer.me().endCounter("prog_init");
	}

	public void preRun() {
		m_QueryAccom.getRecommender().getCaseBase1().discardConfirmedCases();
		m_QueryResultCount = 0;
		m_UserConfirmCount = 0;
	}
	
	private QueryResultInspector m_QueryResultInspector;
	public void setQueryResultInspector(QueryResultInspector queryResultInspector) {
		m_QueryResultInspector = queryResultInspector;
	}

	public ITestOutput run(ITestInput testInput) {
		CBRInput cbrInput = (CBRInput)testInput;
		CBROutput output = null;
		
		try {
			ArrayList<PoiCase> poiCases = null;
			long time = 0;
			for (TestContextVariable tcv : cbrInput) {
				ExecutionTimer.me().startCounter("query_result");
				poiCases = m_QueryAccom.queryResults(tcv.getUserPreference()).getAccommodationList();

				m_QueryResultCount++;
				ExecutionTimer.me().endCounter("query_result");
				
				if (m_QueryResultInspector != null) {
					m_QueryResultInspector.doInspect(poiCases);
				}
				
				if (tcv.getIsUserConfirmed() && poiCases.size() >= 1) {
					ExecutionTimer.me().startCounter("user_confirm");
					m_QueryAccom.getRecommender().Userconfirm(m_QueryAccom.getQuery(), false, 0, false);
					m_UserConfirmCount++;
					ExecutionTimer.me().endCounter("user_confirm");
				}
			}		
			
			output = new CBROutput();
			output.getAccommodationList().addAll(poiCases);
		} catch (Exception e) {
			System.out.print(e.getMessage());
			output = null;
		}
		
		return output;
	}

}