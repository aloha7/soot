package ccr.incubator;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class Hello {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		System.out.println("A");
		long start = System.currentTimeMillis();
		SimpleDateFormat sdf = new SimpleDateFormat("", Locale.SIMPLIFIED_CHINESE);
		sdf.applyPattern("yyyy-MM-dd HH-mm-ss");
		
		System.out.println(System.currentTimeMillis()-start);
	}

}
