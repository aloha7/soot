package swapCurve;

import java.util.GregorianCalendar;
import java.util.HashMap;

public class Cash extends Instrument {
	public Cash(){
		super();
	}
	
	public Cash(String subType, double rate) {
		this(subType, TimeHandler.getBaseDate(), rate);
	}

	public Cash(String subType, GregorianCalendar startDay, double rate) {
		super(subType, startDay, rate);
		this.endDate = TimeHandler.getEndDay(this.startDate, subType);
		this.t = (double)TimeHandler.getDeltaDays(this.startDate, this.endDate)/(double)365;
	}

	public double getDF(HashMap zeroCouponList) {
		this.discountFactor = 1/(double)(1+this.rate * this.t);		
		
		//Keep the instrument into the zeroCouponList
		if(!zeroCouponList.containsKey(this.endDate)){
			zeroCouponList.put(this.endDate, this);	
		}else{
//			System.out.println("Warning:Different instruments at the same date:" 
//					+ this.getClass().getSimpleName()+"("+this.subType + ") vs." 
//					+ zeroCouponList.get(this.endDate).getClass().getSimpleName() + "(" 
//					+ ((Instrument)zeroCouponList.get(this.endDate)).subType + ")");	
		}
		
		return this.discountFactor;
	}
	
	
	public double getZ(HashMap zeroCouponList) {
		this.zeroCouponRate = this.dfToZ(this.discountFactor, this.t);
		
//		if(!zeroCouponList.containsKey(this.endDate)){
//			zeroCouponList.put(this.endDate, this);	
//		}else{
//			System.out.println("Warning:you have different instruments at the same date:" + this.subType + " vs." 
//					+ ((Instrument)zeroCouponList.get(this.endDate)).subType);	
//		}
		return this.zeroCouponRate;
	}
	

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Cash" + "\t" + "From Date" + "\t" + "To Date" + "\t"
				+ "t(act/365)" + "\t" + "Rate" + "\t" + "df" + "\t" + "z"
				+ "\n");
		
		sb.append(this.subType + "\t" + TimeHandler.dateToString(this.startDate)
				+"\t" + TimeHandler.dateToString(this.endDate) + "\t" + this.t + "\t"
				+ this.rate + "\t" + this.discountFactor + "\t" + this.zeroCouponRate + "\n");

		return sb.toString();
	}
}
