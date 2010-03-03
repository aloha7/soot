package hku.cs.seg.experiment.qsic2010;

import java.util.ArrayList;

import c7302.CityAcitivyRecommendation.Accommodation.ListOfPoiCase;
import c7302.CityAcitivyRecommendation.Accommodation.PoiCase;
import hku.cs.seg.experiment.core.ITestOracle;
import hku.cs.seg.experiment.core.ITestOutput;

public class CBROutput extends ListOfPoiCase implements ITestOutput {
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		ArrayList<PoiCase> list = getAccommodationList();
		sb.append("Number of cases: " + list.size() + "\n");
		
		
		for (PoiCase c : list) {
			
			String[] values = c.getGpsLocation().split(",");
			
			sb.append("---------------------\n");
			sb.append("CaseID: " + c.getCaseId() + "\n");
			//sb.append("ActivityType: " + c.getActivityType().toString() + "\n");
			sb.append("Name: " + c.getAddress() + "\n");
			sb.append("Longitude: " + values[0] + "\n");
			sb.append("Latitude: " + values[1] + "\n");
			sb.append("Timestamp: " + c.getTimestamp().getTime() + "\n");
			sb.append("Expenditure: " + c.getExpenditure() + "\n");
			sb.append("PoiID: " + c.getPoiID() + "\n");
			sb.append("PaymentMethod: " + c.getPaymentMethod().toString() + "\n");
			sb.append("AccomType: " + c.getAccomType().toString() + "\n");
			sb.append("Facilities: " + c.getFacility() + "\n");
			sb.append("Rating: " + c.getRate() + "\n");

		}
		
		return sb.toString();
	}
}
