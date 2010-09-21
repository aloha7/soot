package BinominalTree;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.GregorianCalendar;

import swapCurve.Configuration;
import swapCurve.SwapCurveConstructor;
import swapCurve.TimeHandler;
import Util.Newton;

public class Simulator {
	// configured variables
	private double S; // the present stock price
	private double strikePrice; 
	private GregorianCalendar expireDate;//date to expire
	private boolean isCall; // call or put
	private double callOrPutPrice;
	
	
	private double rate; // zero coupon rate
	private double sigma; // volatility of the stock price
	private double deltaT; // time interval
	
	private BinominalNode[][] binominalTree;
	private SwapCurveConstructor curveCtrl;
	
	// derived variables
	public double a; // a = exp(rate * deltaT)
	public double u; // up move rate
	public double d; // down move rate
	public double p; // probability of up move: (a-d)/(u-d)

	public Simulator(double S, double strikePrice, GregorianCalendar expireDate, boolean isCall, double callOrPutPrice){
		this.S = S;
		this.strikePrice = strikePrice;
		this.expireDate = expireDate;
		this.isCall = isCall;
		this.callOrPutPrice = callOrPutPrice;
		
		//1.Derive the zero coupon rate in the expire date
		this.curveCtrl = new SwapCurveConstructor();
		curveCtrl.generateZeroCouponCurve();
		this.rate = curveCtrl.getZeroCouponRate(expireDate);
		
		//2.Derive the sigma
		double timeToExpire = TimeHandler.getDeltaDays(expireDate)/365.0;
		Newton newton = new Newton(S, strikePrice, rate, timeToExpire, callOrPutPrice);
		this.sigma = 1.0; //the guessed initial value		
		this.sigma = newton.getRoot(sigma, Math.pow(10, -9));
//		System.out.println("Volatility is:" + this.sigma);
	}
	
	public Simulator(double S, double strikePrice, GregorianCalendar expireDate, boolean isCall, double callOrPutPrice, double rate, double sigma){
		this.S = S;
		this.strikePrice = strikePrice;
		this.expireDate = expireDate;
		this.isCall = isCall;
		this.callOrPutPrice = callOrPutPrice;
		this.rate = rate;
		this.sigma = sigma;

	}
	public Simulator(double S, double rate, double sigma, double deltaT,
			double strikePrice, boolean isCall) {
		
		this.S = S;
		this.rate = rate;
		this.sigma = sigma;
		this.deltaT = deltaT;
		this.strikePrice = strikePrice;
		this.isCall = isCall;

		this.a = Math.exp(this.rate * this.deltaT);
		this.u = Math.exp(this.sigma * Math.sqrt(this.deltaT));
		this.d = 1.0 / this.u;
		this.p = (this.a - this.d) / (this.u - this.d);
	}

	private void setSteps(int steps){
		//3.Set other parameters
		this.deltaT = TimeHandler.getDeltaDays(this.expireDate)/((double)steps*365);
		this.a = Math.exp(this.rate * this.deltaT);
		this.u = Math.exp(this.sigma * Math.sqrt(this.deltaT));
		this.d = Math.exp(-this.sigma * Math.sqrt(this.deltaT));
		this.p = (this.a - this.d) / (this.u - this.d);
	}
	
	/**Construct an European-Style binomial tree with a given steps, 
	 * no pay off values required for each node
	 * 
	 * 	@param steps
	 * @param benchMark: whether use a stardard function to 
	 * initialize pay outs of nodes in the last level 
	 * @return
	 */
	public double generateBinomialTree_EuropeanStyle(int steps, boolean benchMark){
		double result = 0.0;
		if (steps > 0) {
			this.setSteps(steps);

			this.binominalTree = new BinominalNode[steps + 1][steps + 1];
			// 1. derive the strike price for each node
			binominalTree[0][0] = new BinominalNode(S); 
			// initial nodes at the 0th level
			for (int i = 1; i < binominalTree.length; i++) {
				// construct nodes in the ith level
				for (int j = 0; j < i; j++) {
					// for the jth node(except the last node) at ith level, it
					// down moves from the jth node at the (i-1)th level
					binominalTree[i][j] = new BinominalNode(
							binominalTree[i - 1][j].S * this.d);
				}
				// for the last node(actually ith node) at ith level, it up
				// moves from the last node(actually the (i-1)th node)
				// at the (i-1) level
				binominalTree[i][i] = new BinominalNode(
						binominalTree[i - 1][i - 1].S * this.u);
			}

			// 2. derive the PayOut for each node
			int i = binominalTree.length - 1;
			if (this.isCall) {
				// for Call options
				// for the last level, its premium is based on strike price 
				for (int j = 0; j <= i; j++) { // the index of the last node
					// for ith level is also i
					// for each node at the last level
					if(benchMark){
						double diff = binominalTree[i][j].S - strikePrice;
						if (diff > 0) {
							binominalTree[i][j].PayOut = diff;
						} else {
							binominalTree[i][j].PayOut = 0;
						}	
					}else{ //a new approach: if 75 <= S <= 125, then payOut = |S-100|; else payOut = 0;
						if(binominalTree[i][j].S >= 75 && binominalTree[i][j].S <=125){
							binominalTree[i][j].PayOut = Math.abs(binominalTree[i][j].S - 100);
						}else{
							binominalTree[i][j].PayOut = 0;
						}
					}
				}
			} else {
				// for Put options
				// for the last level, its premium is based on strike price 
				for (int j = 0; j <= i; j++) {
					if(benchMark){
						double diff = strikePrice - binominalTree[i][j].S;
						if (diff > 0) {
							binominalTree[i][j].PayOut = diff;
						} else {
							binominalTree[i][j].PayOut = 0;
						}	
					}else{
						if(binominalTree[i][j].S >= 75 && binominalTree[i][j].S <=125){
							binominalTree[i][j].PayOut = Math.abs(binominalTree[i][j].S - 100);
						}else{
							binominalTree[i][j].PayOut = 0;
						}
					}
				}
			}
			
			//for other levels, the premium is generated based on the previous premiums
			for (i = binominalTree.length - 2; i > -1; i--) { 
				for (int j = 0; j <= i; j++) {// the index of the last node in
					// the ith level is i
					// Premium: the premium of the jth node at the ith level should be
					// combined by node[i+1][j] and node[i+1][j+1]					
					binominalTree[i][j].PayOut = ((1 - this.p)
							* binominalTree[i + 1][j].PayOut + this.p
							* binominalTree[i + 1][j + 1].PayOut)
							* Math.exp(-this.rate * this.deltaT);
				}
			}
			
			//the pay out is the premium of the first node
			result = binominalTree[0][0].PayOut;
		} else {
			System.out
					.println("Warning: the steps of binominal trees should be at least 1");
		}
		return result;
	}
	
	/**Construct an American-Style binomial tree with a given steps
	 * 
	 * @param steps
	 * @param benchMark: whether use a standard function to derive 
	 * pay outs of nodes in the last level
	 * @return
	 */
	public double generateBinomialTree_AmericanStyle(int steps, boolean benchMark) {
		double result = 0.0;
		if (steps > 0) {
			this.setSteps(steps);

			this.binominalTree = new BinominalNode[steps + 1][steps + 1];
			// 1. derive the strike price for each node
			binominalTree[0][0] = new BinominalNode(S); // initial
			// nodes at
			// the first
			// level

			for (int i = 1; i < binominalTree.length; i++) {
				// construct nodes in the ith level
				for (int j = 0; j < i; j++) {
					// for the jth node(except the last node) at ith level, it
					// down moves from the jth node at the (i-1)th level
					binominalTree[i][j] = new BinominalNode(
							binominalTree[i - 1][j].S * this.d);
				}
				// for the last node(actually ith node) at ith level, it up
				// moves from the last node(actually the (i-1)th node)
				// at the (i-1) level
				binominalTree[i][i] = new BinominalNode(
						binominalTree[i - 1][i - 1].S * this.u);
			}

			// 2. derive the Premium and PayOut for each node
			int i = binominalTree.length -1;
			if (this.isCall) {
				// for Call options
				// for the last level, its premium is based on strike price 
				for (int j = 0; j <= i; j++) { // the index of the last node
					// for ith level is also i
					// for each node at the last level
					if(benchMark){
						double diff = binominalTree[i][j].S - strikePrice;
						if (diff > 0) {
							binominalTree[i][j].PayOut = diff;
						} else {
							binominalTree[i][j].PayOut = 0;
						}	
					}else{
						if(binominalTree[i][j].S >= 75 && binominalTree[i][j].S <=125){
							binominalTree[i][j].PayOut = Math.abs(binominalTree[i][j].S - 100);
						}else{
							binominalTree[i][j].PayOut = 0;
						}
					}
				}
			} else {
				// for Put options
				// for the last level, its premium is based on strike price 
				for (int j = 0; j <= i; j++) {
					if(benchMark){
						double diff = strikePrice - binominalTree[i][j].S;
						if (diff > 0) {
							binominalTree[i][j].Premium = diff;
						} else {
							binominalTree[i][j].Premium = 0;
						}	
					}else{
						if(binominalTree[i][j].S >= 75 && binominalTree[i][j].S <=125){
							binominalTree[i][j].PayOut = Math.abs(binominalTree[i][j].S - 100);
						}else{
							binominalTree[i][j].PayOut = 0;
						}
					}
				}
			}

			//for other levels, their premium is generated based on the previous pay offs
			for (i = binominalTree.length - 2; i > -1; i--) { 
				for (int j = 0; j <= i; j++) {// the index of the last node in
					// the ith level is i
					// Premium: the premium of the jth node at the ith level should be
					// combined by node[i+1][j] and node[i+1][j+1]
					
					binominalTree[i][j].Premium = ((1 - this.p)
							* binominalTree[i + 1][j].PayOut + this.p
							* binominalTree[i + 1][j + 1].PayOut)
							* Math.exp(-this.rate * this.deltaT);

					// PayOut:
					double diff = strikePrice - binominalTree[i][j].S;
					if(diff > binominalTree[i][j].Premium){
						binominalTree[i][j].PayOut = diff;
					}else{
						binominalTree[i][j].PayOut = binominalTree[i][j].Premium;
					}
				}
			}
			
			//return the pay out of the node [0][0] as the result
			result = binominalTree[0][0].PayOut;
		} else {
			System.out
					.println("Warning: the steps of binominal trees should be at least 1");
		}
		
		return result;
	}

	public void printBinominalTree(){
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < this.binominalTree.length; i ++){
			// the ith level
			BinominalNode[] binominalNodes = this.binominalTree[i];
			for(int j = 0; j < binominalNodes.length; j ++){
				//the index of last node in the ith level is i				
				BinominalNode node = binominalNodes[j];
				if(node != null){
					sb.append(node.toString() + "\t");	
				}
			}
			sb.append("\n");
		}
		System.out.println(sb.toString());
	}
	
	public static void main(String[] args) {
//		double S = 89.31;
//		double strikePrice = 95.0;
//		boolean isCall = false;
//		double callOrPutPrice = 25.0;
//		int dayOfMonth =22; 
//		int month = 0;
//		int year = 2011;
//		GregorianCalendar expireDay = new GregorianCalendar(year, month, dayOfMonth);
////		System.out.println(TimeHandler.dateToString(expireDay));
//		Simulator sim = new Simulator(S, strikePrice, expireDay, isCall, callOrPutPrice);
//		int steps = 1000;
//		System.out.println(sim.generateBinomialTree_EuropeanStyle(steps));
		

		double S = 100;
		double strikePrice = 90.0;
		GregorianCalendar expireDate = TimeHandler.getEndDay("3M");
		System.out.println(TimeHandler.dateToString(expireDate));
		boolean isCall = true;
		double callOrPutPrice = 15;
		double rate = 0.04;
		double sigma = 0.35;

		Simulator sim = new Simulator(S, strikePrice, expireDate, isCall, callOrPutPrice, rate, sigma);
		
		StringBuilder sb = new StringBuilder();
		boolean benchMark = true;
		
		long start = System.currentTimeMillis();
		sb.append("N" + "\t" + "PayOut_European" + "\n");
		
		int[] steps = null;
		if(args.length > 0){
			steps = new int[args.length];
			for(int i = 0; i < args.length; i++){
				steps[i] = Integer.parseInt(args[i]);
			}
		}else{
			steps = new int[]{
					10000, 20000, 30000, 50000, 100000	
			};	
		}
		
		
		double[] results_European = new double[steps.length];
		for(int i = 0; i < steps.length; i++){
			int step = steps[i];
			results_European[i] = sim.generateBinomialTree_EuropeanStyle(step, benchMark);
			sb.append(step + "\t" + results_European[i] + "\n");
		}
		
		
		sb.append("N" + "\t" + "PayOut_American" + "\n");
		double[] results_American = new double[steps.length];
		for(int i = 0; i < steps.length; i++){
			int step = steps[i];
			results_American[i] = sim.generateBinomialTree_AmericanStyle(step, benchMark);
			sb.append(step + "\t" + results_American[i] + "\n");
		}
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(Configuration.PAYOUT_FILE, true));
			bw.write(sb.toString());
			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println(sb.toString() + "\n" + "Time cost(min):" + ((double)(System.currentTimeMillis()-start))/(double)(1000*60));
	}
}
