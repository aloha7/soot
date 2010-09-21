package swapCurve;

public class Configuration {
	public static final String SPECIFICATION_FILE = "Specification.csv";
	public static final String MARKETDATA_FILE = "MarketData.csv";
	public static final String DISCOUNTFACTOR_FILE = "DF.csv";
	public static final String PAYOUT_FILE = "PayOut.csv";
	
	public static final int PAYOUTFRE_DEFAULT = 3; //compounding frequency
	public static final int SCALE_ZEROCOUPONRATE = 6; //default:4
	public static final int SCALE_DISCOUNTFACTOR = 6;
	public static final String INTERPOLATION_TYPE = "linear";//interpolation types
}
