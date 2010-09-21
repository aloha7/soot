package swapCurve;

import java.math.BigDecimal;

public class RoundTool {
	public static double roundOff(double value, int scale){
		return roundOff(value, scale, BigDecimal.ROUND_HALF_DOWN);
	}
	
	public static double roundOff(double value, int scale, int roundingMode){
		BigDecimal bd = new BigDecimal(value);
		bd = bd.setScale(scale, roundingMode);
		return bd.doubleValue();
	}
}
