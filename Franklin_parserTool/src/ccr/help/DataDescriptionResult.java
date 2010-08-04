package ccr.help;

public class DataDescriptionResult {
	public double min;
	public double max;
	public double median;
	public double mean;
	public double std;
	
	public double sum;
	
	public String toString(){
		return "min:" + min + "\t" + "median:" + median + "\t" + "mean:" + "\t"
		 	+ "max: " + max + "\t" + "std:" + std + "\t" + "sum:" + "\t";
	}
}
