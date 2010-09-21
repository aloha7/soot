package swapCurve;

import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;

public class SubSwap extends Instrument{
	private int startMonth;
	private int endMonth;
	private double deltaT;
	private int paymentFre;
	
	public SubSwap(String subType, int startMonth, int endMonth, double rate){
		this(subType, startMonth, endMonth, TimeHandler.getBaseDate(), rate);
	}
	
	public SubSwap(String subType, int startMonth, int endMonth, GregorianCalendar startDay, double rate){
		super(subType, startDay, rate);
		this.startMonth = startMonth;
		this.endMonth = endMonth;
		this.paymentFre = this.endMonth - this.startMonth;
		
		String addedMonth = this.startMonth + "M";
		this.startDate = TimeHandler.getEndDay(addedMonth);
		
		addedMonth = this.endMonth + "M";
		this.endDate = TimeHandler.getEndDay(addedMonth);
		this.deltaT = (double)TimeHandler.getDeltaDays(this.startDate, this.endDate)/(double)365;
	}
	
	public double getDF(HashMap zeroCouponList){
		if(zeroCouponList.containsKey(this.endDate)){
			double z_temp = ((Instrument)zeroCouponList.get(this.endDate)).zeroCouponRate;
			double deltaT_end = (double)TimeHandler.getDeltaDays(TimeHandler.getBaseDate(), this.endDate)/(double)365;
			this.discountFactor = this.zToDf(z_temp, deltaT_end);				
		}else{
//			//interpolate the discount factor in endDate
//			System.out.println("Src:" + TimeHandler.dateToString(this.endDate) + "\n"+"List:");
//			Iterator ite = zeroCouponList.keySet().iterator();
//			while(ite.hasNext()){
//				GregorianCalendar endDate_temp = (GregorianCalendar)ite.next();
//				Instrument temp = (Instrument)zeroCouponList.get(endDate_temp);
//				System.out.println(TimeHandler.dateToString(endDate_temp) + ":" + temp.getClass().getSimpleName() 
//						+ "(" + temp.subType +")");
//			}
			
			double sum = 0.0;
			int endMonth_temp = this.endMonth - paymentFre;
			while(endMonth_temp>0){
				GregorianCalendar endDate_temp = TimeHandler.getEndDay(endMonth_temp + "M");
//				System.out.println("Dest:" + TimeHandler.dateToString(endDate_temp));

				Instrument temp= (Instrument)zeroCouponList.get(endDate_temp);
				sum += (TimeHandler.getDeltaDays(temp.startDate, temp.endDate)/(double)365) * temp.discountFactor;
				endMonth_temp = endMonth_temp - paymentFre;
			}
			
			this.discountFactor = (1 -  this.rate * sum)/(1 + this.rate * this.deltaT);
		}
		
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
	
	public double getZ(HashMap zeroCouponList){
		double deltaDays = (double)TimeHandler.getDeltaDays(this.endDate)/(double)365;
		this.zeroCouponRate = this.dfToZ(this.discountFactor, deltaDays);
		
//		if(!zeroCouponList.containsKey(this.endDate)){
//			zeroCouponList.put(this.endDate, this);	
//		}else{
//			System.out.println("Warning:you have different instruments at the same date:" + this.subType + " vs." 
//					+ zeroCouponList.get(this.endDate).getClass().getSimpleName());	
//		}
		
		return this.zeroCouponRate;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Swap" + "\t" + "From #mth" + "\t" + "To #mth" + "\t" + "Swap Rate" 
				+ "\t" + "From Date" + "\t" + "To Date" + "\t" + "delta t" 
				+ "\t" + "df(end)" + "\t" + "dt*df" + "\t" + "z"
				+ "\n");
		
		sb.append(this.subType + "\t" + this.startMonth + "\t" + this.endMonth 
				+ "\t" + this.rate + "\t" + TimeHandler.dateToString(this.startDate)
				+ "\t" + TimeHandler.dateToString(this.endDate) + "\t" + this.deltaT 
				+ "\t" + this.discountFactor + "\t" + this.deltaT*this.discountFactor + "\t" + this.zeroCouponRate + "\n");

		return sb.toString();
	}
}
