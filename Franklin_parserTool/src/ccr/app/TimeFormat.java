package ccr.app;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class TimeFormat {

    private static final String FORMAT = "yyyy-MM-dd HH:mm:ss:SSS";

    public static long convert(String sTime) {

        long lTime = 0;

        try {
            SimpleDateFormat df = new SimpleDateFormat(FORMAT);
            Date dTime = df.parse(sTime);
            Calendar cal = Calendar.getInstance();
            cal.setTime(dTime);
            lTime = cal.getTimeInMillis();
        } catch (ParseException e) {
            System.out.println("Cannot convert time: " + sTime);
        }

        return lTime;
    }

    public static String convert(long lTime) {

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(lTime);
        Date dTime = cal.getTime();
        SimpleDateFormat df = new SimpleDateFormat(FORMAT);
        String sTime = df.format(dTime);

        return sTime;
    }

}
