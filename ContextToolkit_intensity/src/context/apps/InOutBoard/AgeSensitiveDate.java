package context.apps.InOutBoard;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;


public class AgeSensitiveDate extends GregorianCalendar {

  private static final int ONE_DAY = 60*60*24;		// one day in seconds
	private static final int ONE_WEEK = 7*ONE_DAY;
	
	
	public AgeSensitiveDate () {
		super ();
	}

	public AgeSensitiveDate (long d) {
		super ();
		setTime (new Date (d));
	}
	
	public AgeSensitiveDate (String d) {
		
		this ((new Long(d)).longValue ());
	}
	
	public String getShortDateTime () {
	
		// return either:
		// HH:MM
		// or if date is more than 1 day old
		// Day HH:MM
		// or if date is more than 7 days old
		// MM/DD HH:MM
		
		int hhInt = get(Calendar.HOUR_OF_DAY);
		String postfix = null;
		
		// fix for AM/PM display
		if (hhInt < 12) {
			postfix = "am";
			if (hhInt == 0)		// 0am is 12am
				hhInt = 12;
		} else {
			postfix = "pm";
			if (hhInt > 12)		// 12pm is not 0 pm
				hhInt -= 12;
		}

		String hh = (new Integer (hhInt)).toString ();
		String mn = (new Integer (get(Calendar.MINUTE))).toString();
		if (mn.length() < 2) { // add 0 if only one digit
			mn = "0" + mn;
		}
		String t = new String (hh+":"+mn+postfix);
		String d = null;
		
		GregorianCalendar dayAgo = new GregorianCalendar ();
		GregorianCalendar weekAgo = new GregorianCalendar ();
		// the following two lines should be changed as follows to work on WinCE:
		// dayAgo.roll (dayAgo.DATE, false); 			// roll back one day
		// weekAgo.roll (weekAgo.DATE*7, false); 	// roll back one week

		dayAgo.roll (DAY_OF_YEAR, false);	// roll back one day
		weekAgo.roll (WEEK_OF_YEAR, false);	// roll back one week
		
		if (before (weekAgo)) {
			String mm = (new Integer (get(Calendar.MONTH) + 1)).toString(); // months start at 0?!!
			String dd = (new Integer (get(Calendar.DAY_OF_MONTH))).toString();
			d = new String (mm+"/"+dd);
		} else {
			if (before (dayAgo)) {
				int v = get(Calendar.DAY_OF_WEEK);
				switch (v) {
					case 1:
						d = "Sun";
						break;
					case 2:
						d = "Mon";
						break;
					case 3:
						d = "Tue";
						break;
					case 4:
						d = "Wed";
						break;
					case 5:
						d = "Thu";
						break;
					case 6:
						d = "Fri";
						break;
					case 7:
						d = "Sat";
						break;
						
				}
			} else {
				d = null;
			}
		}
		
		if (d != null)
			return new String (d + " " + t);
		else
			return new String (t);
	}
	
}
