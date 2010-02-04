package ccr.help;

public class DataAnalyzeManager {	
	class CurveFittingResult{
		public double coefficient;
		public double intercept;
		public double inaccuracy;
	}
	
	public CurveFittingResult getLinearCurveFitting(double[] x, double[] y){
		double sum_x = 0.0;
		double sum_y = 0.0;
		double sum_xByy = 0.0;
		double sum_x_Squre = 0.0;
		
		for(int i = 0; i < x.length; i ++){
			sum_x += x[i];
			sum_y += y[i];
			sum_x_Squre += x[i]*x[i];
			sum_xByy += x[i]*y[i];
		}					
		int n = x.length;
		
		double coefficient =  (n *sum_xByy - sum_x * sum_y )/(n*sum_x_Squre - sum_x*sum_x);
		double intercept = sum_y/n - coefficient* sum_x/n;
//		double inaccuracy = ;
		CurveFittingResult result = new CurveFittingResult();
		result.coefficient = coefficient;
		result.intercept = intercept;
		return result;
	}
	
	public double getPearsonCorrelationTest(double[] x, double[] y){
		double PearsonCorrelationCoefficient = 0.0;
		
		double sum_x = 0.0;
		double sum_y = 0.0;
		double sum_xByy = 0.0;
		double sum_x_Square = 0.0;
		double sum_y_Square = 0.0;
		
		for(int i = 0; i < x.length; i ++){
			sum_x += x[i];
			sum_y += y[i];
			sum_x_Square += x[i]*x[i];
			sum_y_Square += y[i]*y[i];
			sum_xByy += x[i]*y[i];
		}
		int n = x.length;
		
		PearsonCorrelationCoefficient = (double)(sum_xByy - (sum_x * sum_y)/n)/(double)
			Math.sqrt((sum_x_Square-sum_x*sum_x/n)*(sum_y_Square - (sum_y*sum_y)/n));
				
		return PearsonCorrelationCoefficient;
	}
	
	public static void main(String[] args){
		double[] x = new double[]{3, 5, 9};
		double[] y = new double[]{8, 11, 14};
		
		DataAnalyzeManager ins = new DataAnalyzeManager();
		System.out.println(ins.getPearsonCorrelationTest(x, y));
		
	}
}
