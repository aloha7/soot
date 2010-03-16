package ccr.help;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;

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
	
	/**2010-03-15: return a percentile value for a given data array and a percentile
	 * 
	 * @param data
	 * @param percentile: 50 means 50% percentile (median)
	 * @return
	 */
	public static double getPercentileValue(double[] data, int percentile){
		
		double percentileValue = 0.0;
		
		Arrays.sort(data); //sort data in ascending order
		double size = data.length;
		double rank = percentile*(size + 1)/100;
		
		String rankStr = new DecimalFormat("0.00").format(rank);
		int index = rankStr.indexOf(".");
		int intergerPortion = Integer.parseInt(rankStr.substring(0, index));
		int fractionalPortion = Integer.parseInt(rankStr.substring(index+1));
		if(fractionalPortion == 0){
			percentileValue = data[intergerPortion-1];
		}else{
			//interpolate 
			double value1 = data[intergerPortion-1];
			double value2 = data[intergerPortion];
			
			double percentile_temp = ((double)fractionalPortion/100) * (value2 - value1) + value1;
			for(int i = 0; i < size; i ++){ //find the smallest value greater than or equal to percentile_temp
				double ins = data[i];
				if(ins >= percentile_temp){
					percentileValue = ins;
					break;
				}
			}
		}
		
		return percentileValue;
	}
	
	/**2010-03-14:get the min, max, mean, median, std description of the data Array
	 * 
	 * @param data: 
	 * @return
	 */
	public static DataDescriptionResult getDataDescriptive(double[] data){
		DataDescriptionResult descriptionResult = new DataDescriptionResult();
		
		double min = data[0];
		double max = data[data.length - 1];
		double sum = 0.0;
		
		for(int i =0; i < data.length; i++){
			double ins = data[i];
			sum += ins;											
		}
		
		double mean = sum/data.length;
		
		double temp = 0.0;
		for(int i = 0; i < data.length; i ++){
			double ins = data[i];
			temp += (ins - mean)*(ins-mean);
		}
		double std = Math.sqrt(temp/data.length);
		double median = getPercentileValue(data, 50);
		
		descriptionResult.min = min;
		descriptionResult.max = max;
		descriptionResult.mean = mean;
		descriptionResult.median = median;
		descriptionResult.sum = sum;
		descriptionResult.std = std;
		
		return descriptionResult;
	}
	
	public static void main(String[] args){
		double[] x = new double[]{3, 5, 9};
		double[] y = new double[]{8, 11, 14};
		
//		System.out.println(DataAnalyzeManager.getPearsonCorrelationTest(x, y));
		
//		x = new double[]{5, 3, 7, 9, 8, 11, 13, 15};
		
//		x = new double[]{4, 4, 5, 5, 5, 5, 6, 6, 6, 7, 7,7, 8, 8, 9, 9, 9, 10, 10, 10};
		
		x = new double[]{2, 3, 5, 9, 11};
		
		System.out.println(getPercentileValue(x, 50));
		
	}
	
	
}
