package hku.cs.seg.experiment.qsic2010;

import java.util.ArrayList;
import java.util.List;

import c7302.CityAcitivyRecommendation.Accommodation.PoiCase;

import hku.cs.seg.experiment.core.ITestOracle;
import hku.cs.seg.experiment.core.ITestOutput;

@SuppressWarnings("serial")
public class CBROracle extends ArrayList<Integer> implements ITestOracle {
//	private List<Integer> m_PoiIDs;
//	
//	public CBROracle(List<Integer> poiIDs) {
//		m_PoiIDs = poiIDs;
//	}
	
	public boolean isOutputApplicable(ITestOutput output) {
		return output instanceof CBROutput;
	}

	public boolean isPassed(ITestOutput testOutput) {
		if (testOutput == null) return false;
		
		CBROutput output = (CBROutput)testOutput;
		ArrayList<PoiCase> poiCases = output.getAccommodationList();
		
		if (poiCases.size() != this.size()) return false;
		
		boolean appear = true;;
		for (int id : this) {
			appear = false;
			for (int i = 0; i < poiCases.size(); i++) {
				if (id == poiCases.get(i).getPoiID()) {
					appear = true;
					break;
				}
			}
			if (!appear) return false;
		}
		return true;
	}
}
