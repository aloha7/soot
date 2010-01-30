package ccr.help;

import java.text.DecimalFormat;

import ccr.test.TestSet;

public class ILPOutput {
	public String criterion;
	public double alpha;
	public int testSetSize;
	public int testSetId;
	public double time; //in seconds
	public TestSet reducedTestSet = new TestSet();
	public double objectiveValue;
	
	public ILPOutput(){
		
	}
	
	public ILPOutput(String criterion, double alpha, 
			int testSetSize, int testSetId, double time, 
			TestSet reducedTestSet, double objectiveValue){
		this.criterion = criterion;
		this.alpha = alpha;
		this.testSetId = testSetId;
		this.testSetSize = testSetSize;
		this.time = time;
		this.reducedTestSet = reducedTestSet;
		this.objectiveValue = objectiveValue;
	}
	
	public ILPOutput(String str){
		String[] strs = str.split("\t");
		this.criterion = strs[0];
		this.alpha = Double.parseDouble(strs[1]);
		this.testSetSize = Integer.parseInt(strs[2]);
		this.testSetId = Integer.parseInt(strs[3]);
		this.time = Long.parseLong(strs[4]);
		this.objectiveValue = Double.parseDouble(strs[5]);
		String[] tcs = strs[6].split(",");
		for(int i = 0; i < tcs.length; i ++){
			this.reducedTestSet.testcases.add(tcs[i]);
		}
	}
	
	public String toString(){
		DecimalFormat format = new DecimalFormat("0.0000");
		
		StringBuilder sb = new StringBuilder();		
		sb.append(criterion).append("\t").append(format.format(this.alpha)).append("\t");
		sb.append(testSetSize).append("\t").append(testSetId).append("\t");
		sb.append(format.format(this.time)).append("\t").append(format.format(this.objectiveValue)).append("\t");
		
		for(int i = 0; i < reducedTestSet.testcases.size(); i ++){
			String tc = reducedTestSet.testcases.get(i);			
			sb.append(tc).append(",");
		}
		sb.append("\n");
		
		return sb.toString();
	}
}
