package MonteCarlo;

public class ErrorAnalysis {
	private double confidenceLevel;
	private double statisticValue;
	private double confidenceInterval_lowerEnd;
	private double confidenceInterval_upperEnd;
	private double errorRange;
	
	public ErrorAnalysis(double confidenceLevel, double statisticValue){
		this.confidenceLevel = confidenceLevel;
		this.statisticValue = statisticValue;		
	}
	
	public void estimateErrors(double Mean, double Variance, double SampleSize){
		this.confidenceInterval_lowerEnd = Mean - this.statisticValue * Variance/Math.sqrt(SampleSize);
		this.confidenceInterval_upperEnd = Mean + this.statisticValue * Variance/Math.sqrt(SampleSize);
		this.errorRange = this.confidenceInterval_upperEnd - this.confidenceInterval_lowerEnd;
	}
	
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append("ConfidenceLevel:(" + this.confidenceLevel +")\t" +"StatisticValue:("+this.statisticValue + ")\t" + "LowerEnd:("+this.confidenceInterval_lowerEnd
				+ ")\t" + "UpperEnd:("+this.confidenceInterval_upperEnd + ")\t" + "ErrorRange:(" +this.errorRange + ")");
		return sb.toString();
	}
}
