package ccr.help;

import java.text.DecimalFormat;

import ccr.test.TestSet;

public class ILPOutput {

	public long time;
	public TestSet reducedTestSet = new TestSet();
	public double objectiveValue;
	
	public ILPOutput(){
		
	}
	
	public ILPOutput(long time, 
			TestSet reducedTestSet, double objectiveValue){
	
		this.time = time;
		this.reducedTestSet = reducedTestSet;
		this.objectiveValue = objectiveValue;
	}
	
	public ILPOutput(String str){
		String[] strs = str.split("\t");
		this.time = Long.parseLong(strs[0]);
		this.objectiveValue = Double.parseDouble(strs[1]);
		String[] tcs = strs[2].split(",");
		for(int i = 0; i < tcs.length; i ++){
			this.reducedTestSet.testcases.add(tcs[i]);
		}
	}
	
	public String toString(){
		DecimalFormat format = new DecimalFormat("0.0000");
		
		StringBuilder sb = new StringBuilder();		
		sb.append(format.format(this.time)).append("\t").append(format.format(this.objectiveValue)).append("\t");
		
		for(int i = 0; i < reducedTestSet.testcases.size(); i ++){
			String tc = reducedTestSet.testcases.get(i);			
			sb.append(tc).append(",");
		}
		sb.append("\n");
		
		return sb.toString();
	}
}
