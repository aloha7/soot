package swapCurve;


import java.util.GregorianCalendar;
import java.util.HashMap;

public abstract class Instrument {
	public String subType;
	public GregorianCalendar startDate;
	public GregorianCalendar endDate;
	public double t;
	public double rate;
	public double discountFactor;
	public double zeroCouponRate;
		
	public Instrument(){
		
	}
	
	public Instrument(String type, double rate){
//		this(type, new GregorianCalendar(), rate);//2009-04-22: it's wrong since it cause not all instrument start at the same date
		this(type, TimeHandler.getBaseDate(), rate);
	}
	
	public Instrument(String type, GregorianCalendar startDate, double rate){
		this.subType = type;
		this.startDate = startDate;
		this.rate = rate;
	}
	
	
	public abstract double getDF(HashMap zeroCouponList);
	
	public abstract double getZ(HashMap zeroCouponList);
	
	public abstract String toString();
	
	
	public static double interpolate(double x1, double y1, double x2, double y2, double x){
		double result = 0.0;
		if(Configuration.INTERPOLATION_TYPE.equals("linear")){
			result = interpolate_linear(x1, y1, x2, y2, x);
		}
		
		return result;
	}
	
	/**	liner-interpolate the y value 
	 * 
	 * @param x1
	 * @param y1
	 * @param x2
	 * @param y2
	 * @param x
	 * @return
	 */
	private static double interpolate_linear(double x1, double y1, double x2, double y2, double x){
		double result = 0.0;
		if(x2 == x1){
			if(x == x1){
				result = y1;
			}
		}else{
			result = ((y2-y1)/(x2-x1)) * (x-x1) + y1;
		}
		return result;
	}
	

	/**	translate discount factor to zero coupon rate
	 * 
	 * @param z
	 * @param t
	 * @return
	 */
	public static double zToDf(double z, double t){
		return Math.pow(1/(1+z/2), 2*t);
	}
	
	/**translate zero coupon rate to discount factor
	 * 
	 * @param df
	 * @param t
	 * @return
	 */
	public static double dfToZ(double df, double t){
		return (1/Math.pow(df, 1/(2*t))-1)*2;
	}
}
