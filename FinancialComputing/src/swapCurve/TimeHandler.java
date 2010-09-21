package swapCurve;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class TimeHandler {
	private static GregorianCalendar baseDate;
	
	public static GregorianCalendar getBaseDate(){
		if(baseDate == null){
			baseDate = new GregorianCalendar();
		}
		return baseDate;
	}
	
	
	public static void main(String[] args){
		String[] subTypes = new String[]{"ON", "1W", "1M", "2M", "3M",
				"1Y", "2Y", "3Y", "4Y", "5Y", "7Y", "10Y" 
				};
		
		for(String subType: subTypes){
			System.out.print(subType + "\t" + "From:" );
			displayDate(new GregorianCalendar()); 
			System.out.print("\t To :");
			GregorianCalendar endDay = getEndDay(subType);
			displayDate(endDay);
			System.out.print("\t t(act):" + getDeltaDays(getEndDay(subType)) + "\n");
		}
	}
	
	public static double getDeltaDays(GregorianCalendar endDay){
		//2009-5-13: a bug here: forget to round off the days
		return RoundTool.roundOff(getDeltaDays(TimeHandler.getBaseDate(), endDay), 0, BigDecimal.ROUND_UP);
	}
	
	public static double getDeltaDays(GregorianCalendar startDay, GregorianCalendar endDay){
		return (double)(endDay.getTimeInMillis()-startDay.getTimeInMillis())/(double)(1000*60*60*24);
	}
	
	
	public static GregorianCalendar getEndDay(String subType){
		return getEndDay(TimeHandler.getBaseDate(), subType);
	}
	
	public static GregorianCalendar getEndDay(GregorianCalendar startDay, String subType){
		GregorianCalendar endDay = (GregorianCalendar)startDay.clone();
//		GregorianCalendar endDay =  new GregorianCalendar(startDay.YEAR, startDay.MONTH, startDay.DAY_OF_YEAR);
		
		if(subType.equals("ON")){
//			endDay.roll(Calendar.DAY_OF_YEAR, 1);
			endDay.add(Calendar.DAY_OF_YEAR, 1);
		}else if(subType.contains("W")){
			int addWeek = Integer.parseInt(subType.substring(0, subType.indexOf("W")));
//			endDay.roll(Calendar.WEEK_OF_YEAR, addWeek);
			endDay.add(Calendar.WEEK_OF_YEAR, addWeek);
		}else if(subType.contains("M")){
			int addMonth = Integer.parseInt(subType.substring(0, subType.indexOf("M")));
//			endDay.roll(Calendar.MONTH, addMonth);
			endDay.add(Calendar.MONTH, addMonth);
		}else if(subType.contains("Y")){
			int addYear = Integer.parseInt(subType.substring(0, subType.indexOf("Y")));
//			endDay.roll(Calendar.YEAR, addYear);
			endDay.add(Calendar.YEAR, addYear);
		}else if(subType.contains("D")){
			int addDay = Integer.parseInt(subType.substring(0, subType.indexOf("D")));
			endDay.add(Calendar.DAY_OF_YEAR, addDay);
		}
		
//		else if(subType.contains("*")){
//			int addMonth = Integer.parseInt(subType.substring(subType.indexOf("*")+ "*".length()));
//			endDay.roll(Calendar.MONTH, addMonth);
////			endDay.add(Calendar.MONTH, addMonth);
//		}
		
		return endDay;
	}
	
	
	public static void displayDate(GregorianCalendar cal){
		System.out.println(dateToString(cal));
	}
	
	public static String dateToString(GregorianCalendar cal){
		Date day = cal.getTime();
		SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
		return formatter.format(day);
	}
}
