package ccr.help;

public class DataAnalyzeManager {	
	
	
	public static CurveFittingResult getLinearCurveFitting(double[] x, double[] y){
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

		CurveFittingResult result = new CurveFittingResult();
		result.coefficient = coefficient;
		result.intercept = intercept;
		
		double sum_inaccuracy = 0.0;
		for(int i = 0; i < n; i ++){
			sum_inaccuracy += (result.coefficient*x[i]+result.intercept - y[i])
							*(result.coefficient*x[i]+ result.intercept - y[i]);			
		}
		
		result.inaccuracy = Math.sqrt(sum_inaccuracy/(n));
		return result;
	}
	
	public static double getPearsonCorrelationTest(double[] x, double[] y){
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
		
		System.out.println(DataAnalyzeManager.getPearsonCorrelationTest(x, y));
		
	}
}
