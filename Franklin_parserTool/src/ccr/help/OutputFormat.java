package ccr.help;

import java.util.ArrayList;

public class OutputFormat {
	public String content;
	public double alpah;
	public ArrayList negativeLength = new ArrayList(); //the length which has the negative correlation
	public ArrayList slopeSequence = new ArrayList();
	
	//between CD and mutation score
	public OutputFormat(){
		
	}
	
	public OutputFormat(String content, double alpha, ArrayList negativeLength){
		this.content = content;
		this.alpah = alpha;
		this.negativeLength = negativeLength;
	}
}
