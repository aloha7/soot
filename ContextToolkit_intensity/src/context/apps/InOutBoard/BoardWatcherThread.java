package context.apps.InOutBoard;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;


public class BoardWatcherThread extends Thread {

	private InOutBoard iob = null;			// for communication with the main app
	
	private final int kWakeUpHour = 2;		// hour at which this thread wakes up
	
	private final int kTwentyFour = 24*60*60*1000;
											// 24 hrs in milliseconds
											
	public BoardWatcherThread (InOutBoard iob) {
		super ();
		this.iob = iob;
		
	}
	
	public void run () {
	
		// compute how long we want to sleep
		GregorianCalendar wakeUp = new GregorianCalendar ();
		wakeUp.setTime (new Date ());		// set to now
		
		wakeUp.set (Calendar.MINUTE, 0);
		wakeUp.set (Calendar.HOUR_OF_DAY, kWakeUpHour);
											// we use a 24hr clock
		
		long now = System.currentTimeMillis();
											// what time is it?
											
		if (wakeUp.getTime ().before (new Date (now))) {
											// no point waking up in the past
			wakeUp.roll (Calendar.DAY_OF_WEEK, true);
											// sleep another day instead
		}
		
		long sleepTime = wakeUp.getTime ().getTime () - now;
											// we may have missed a few ms. No big deal.
		
		try {
			sleep (sleepTime);
		}
		catch (InterruptedException ie)	{
			System.out.println ("Someone woke up BoardWatcherThread! This is not normal: " + ie);
		}
		
		while (true) {
		
			// do our jobs
			checkOvertime ();
			
			forceRedraw ();
			
			// and go back to sleep
			try {
				sleep (kTwentyFour);
			}
			catch (InterruptedException ie)	{
				System.out.println ("Someone woke up BoardWatcherThread! This is not normal: " + ie);
			}
		}
	}
	
	private void checkOvertime () {
		iob.checkOvertime ();
	}
	
	private void forceRedraw () {
	
		iob.midnightRefresh ();
	}
	
}
