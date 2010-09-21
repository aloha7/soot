package incubator;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class DateExample {
	public static void main(String[] args){
		Date today = new Date();
		
		//1.
		GregorianCalendar cal = new GregorianCalendar();
//		cal.add(Calendar.WEEK_OF_YEAR, 1);
		displayDate(cal);
		
		//2.
		GregorianCalendar cal_1 = new GregorianCalendar();
		String subType = "1M";
		int addWeek = Integer.parseInt(subType.substring(0, subType.indexOf("M"))); 
		cal_1.roll(Calendar.MONTH, addWeek);
		displayDate(cal_1);
		
		long deltaT = (cal_1.getTimeInMillis() - cal.getTimeInMillis())/(24 * 60 * 60 * 1000);
		System.out.println(deltaT);
	}
	
	public static void displayDate(GregorianCalendar cal){
		Date day = cal.getTime();
		displayDate(day);
	}
	
	public static void displayDate(Date day){
		SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
		String s = formatter.format(day);
		System.out.println(s);
	}
}
