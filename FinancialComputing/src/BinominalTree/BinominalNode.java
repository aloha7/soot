package BinominalTree;

public class BinominalNode {
	public double S;
	public double Premium;
	public double PayOut;
	
	public BinominalNode(double S){
		this.S = S;
	}
	
	public String toString(){
		return "Price:(" + this.S + ")" + "\t" + "Premium:(" + this.Premium + ")"
		 	+ "\t" + "PayOut:(" + this.PayOut + ")\t";
	}
}
