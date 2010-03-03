package hku.cs.seg.experiment.qsic2010;

import c7302.CityAcitivyRecommendation.main.UserPreference;

public class TestContextVariable {
	private UserPreference m_UserPreference;
	private boolean m_IsUserConfirmed;
	
	public TestContextVariable(UserPreference userPreference, boolean isUserConfirmed) {
		m_UserPreference = userPreference;
		m_IsUserConfirmed = isUserConfirmed;		
	}
	
//	public void setUserPreference(UserPreference m_UserPreference) {
//		this.m_UserPreference = m_UserPreference;
//	}
	public UserPreference getUserPreference() {
		return m_UserPreference;
	}
	
	public void setIsUserConfirmed(boolean m_IsUserConfirmed) {
		this.m_IsUserConfirmed = m_IsUserConfirmed;
	}
	public boolean getIsUserConfirmed() {
		return m_IsUserConfirmed;
	}
	
	
}
