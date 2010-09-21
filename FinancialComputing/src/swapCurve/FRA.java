package swapCurve;

import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;



public class FRA extends Instrument{
	private double deltaT;
	private double discountFactor_start;
	
	public FRA(String subType, double rate){
		this(subType, TimeHandler.getBaseDate(), rate);
	}
	
	public FRA(String subType, GregorianCalendar startDay, double rate){
		super(subType, startDay, rate);
		String subType_start = subType.substring(0, subType.indexOf("x"));
		this.startDate = TimeHandler.getEndDay(this.startDate, subType_start + "M");
		
		String subType_end = subType.substring(subType.indexOf("x") + "x".length());
		this.endDate = TimeHandler.getEndDay( subType_end + "M");
		
		this.deltaT = (double)TimeHandler.getDeltaDays(this.startDate, this.endDate)/(double)365;
		
		this.t = (double)TimeHandler.getDeltaDays(this.startDate)/(double)365;
	}
	
	
	public double getDF(HashMap zeroCouponList){
		if(zeroCouponList.containsKey(this.startDate)){
			double z_temp = ((Instrument)zeroCouponList.get(this.startDate)).zeroCouponRate;
			this.discountFactor_start = this.zToDf(z_temp, this.t);
			this.discountFactor = (1.0/(double)(1+ this.rate* this.deltaT))*this.discountFactor_start;
		}else{
			//interpolate the discount factor in startDate
			System.out.println("Src:" + TimeHandler.dateToString(this.startDate) + "\n"+"List:");
			Iterator ite = zeroCouponList.keySet().iterator();
			while(ite.hasNext()){
				TimeHandler.displayDate((GregorianCalendar)ite.next());
			}
		}
		
		//Keep the instrument into the zeroCouponList
		if(!zeroCouponList.containsKey(this.endDate)){
			zeroCouponList.put(this.endDate, this);	
		}else{
//			System.out.println("Warning:different instruments at the same date:" 
//					+ this.getClass().getSimpleName()+"("+this.subType + ") vs." 
//					+ zeroCouponList.get(this.endDate).getClass().getSimpleName() + "(" 
//					+ ((Instrument)zeroCouponList.get(this.endDate)).subType + ")");	
		}
		
		return this.discountFactor;
	}
	
	public double getZ(HashMap zeroCouponList){
		double deltaDays = (double)TimeHandler.getDeltaDays(this.endDate)/(double)365;
		this.zeroCouponRate = this.dfToZ(this.discountFactor, deltaDays);
		
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
		sb.append("FRA" + "\t" + "From Date" + "\t" + "To Date" + "\t" + "delta T" 
				+ "\t" + "Rate" + "\t" + "t0" + "df(start)" +"\t" + "df" + "\t" + "z" 
				+ "\n");
		
		sb.append(this.subType + "\t" + TimeHandler.dateToString(this.startDate)
				+"\t" + TimeHandler.dateToString(this.endDate) + "\t" + this.deltaT 
				+ "\t" + this.rate + "\t" + this.t + "\t" + this.discountFactor_start 
				+ "\t" + this.discountFactor + "\t" + this.zeroCouponRate + "\n");

		return sb.toString();
	}
}
