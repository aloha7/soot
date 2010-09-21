package swapCurve;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;

public class Swap extends Instrument{
	private int startMonth;	
	private double deltaT;
	private int paymentFre;
	private ArrayList subSwaps = new ArrayList();
	
	public ArrayList getSubSwaps(){
		return this.subSwaps;
	}
	
	public Swap(String subType, double rate){
		this(subType, Configuration.PAYOUTFRE_DEFAULT, rate);
	}
	
	public Swap(String subType, int paymentFre, double rate){
		this(subType, paymentFre, TimeHandler.getBaseDate(), rate);
	}
	
	public Swap(String subType, int paymentFre, GregorianCalendar startDay, double rate){
		this(subType, 0, paymentFre, startDay, rate);
	}
	
	public Swap(String subType, int startMonth, int paymentFre, GregorianCalendar startDay, double rate){
		super(subType, startDay, rate);
		this.startMonth = startMonth;
		
		int subSwaps_num =  Integer.parseInt(subType.substring(0, subType.indexOf("Y"))) * 12 / paymentFre;
		for(int i = 0 ; i < subSwaps_num; i ++){
			int startMonth_sub = i * paymentFre;
			int endMonth_sub = startMonth_sub + paymentFre;
			
			SubSwap subSwap = new SubSwap(subType, startMonth_sub, endMonth_sub, rate );
			subSwaps.add(subSwap);
		}
	}
	
	public Swap(String subType, double rate, HashMap zeroCouponList){
		this(subType, 0, Configuration.PAYOUTFRE_DEFAULT, TimeHandler.getBaseDate(), rate, zeroCouponList);
	}
	
	public Swap(String subType, int startMonth, int paymentFre, GregorianCalendar startDay, double rate, HashMap zeroCouponList){
		super(subType, startDay, rate);
		this.startMonth = startMonth;		
		
		int subSwaps_num =  Integer.parseInt(subType.substring(0, subType.indexOf("Y"))) * 12 / paymentFre;
		int endMonth = startMonth + paymentFre* subSwaps_num;
		
		for(int i = 0 ; i < subSwaps_num; i ++){
			int startMonth_sub = i * paymentFre;
			int endMonth_sub = startMonth_sub + paymentFre;
			SubSwap subSwap;
			double rate_subSwap = 0.0;
			if(endMonth_sub <= 12){
				rate_subSwap = rate;
			}else{
				//interpolate the swap rate for SubSwap if its endMonth is larger than 1Y
				int x1 = endMonth_sub/12 * 12; // the nearest precedent year 
				int x2 = (endMonth_sub/12 + 1) * 12; //the nearest succedent year
				double y1, y2;	
				
				//find a precedent year that exists
				Instrument ins_pre =(Instrument)zeroCouponList.get(TimeHandler.getEndDay(x1+"M"));
				while(ins_pre == null && x1 >= this.startMonth ){
					x1 = x1 - 12;
					ins_pre = (Instrument)zeroCouponList.get(TimeHandler.getEndDay(x1+"M"));
				}
				if(x1 < this.startMonth){ // no precedent year is found
					x1 = endMonth;
					y1 = this.rate;
					
					
				}else{
					y1 = ins_pre.rate;
				}
				
				
				//find a successive year that exists
				Instrument ins_succ =(Instrument)zeroCouponList.get(TimeHandler.getEndDay(x2+"M"));
				while(ins_succ == null && x2 <= endMonth){
					x2 = x2 + 12;
					ins_succ =(Instrument)zeroCouponList.get(TimeHandler.getEndDay(x2+"M"));
				}
				if(x2 > endMonth){
					// no successive year exists 
					x2 = endMonth;
					y2 = this.rate;
					
				}else{
					y2 = ins_succ.rate;
				}
				
				rate_subSwap = this.interpolate(x1, y1, x2, y2, endMonth_sub);
			}
			
			subSwap = new SubSwap(subType, startMonth_sub, endMonth_sub, rate_subSwap );
			subSwaps.add(subSwap);
		}
	}
	
	public double getDF(HashMap zeroCouponList){
		for(int i = 0; i < subSwaps.size(); i ++){
			SubSwap subSwap = (SubSwap)subSwaps.get(i);
			subSwap.getDF(zeroCouponList);
		}
		
		return this.discountFactor;
	}
	
	public double getZ(HashMap zeroCouponList){
		for(int i = 0; i < subSwaps.size(); i ++){
			SubSwap subSwap = (SubSwap)subSwaps.get(i);
			subSwap.getZ(zeroCouponList);
		}
		
		return this.zeroCouponRate;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < subSwaps.size(); i ++){
			SubSwap subSwap = (SubSwap)subSwaps.get(i);
			sb.append(subSwap.toString());
		}
		
		return sb.toString();
	}
}
