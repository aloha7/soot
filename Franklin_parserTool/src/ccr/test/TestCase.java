package ccr.test;

import java.util.HashMap;
import java.util.Vector;

public class TestCase {
	
	public String index; //equivalent to input
	public double CI;
	public int activation;
	public String length;		

	
	//2009-12-14: test suite reduction 
	public long execTime;
	public int hitCounter; //count how many elements are hit(For each element, 1:covered; 0: uncovered)
	public int hitSum; //summary the total numbers of the covered elements(it is an integer rather than binary for each element)
	public double coverage; //coverage with respect to a criterion
	public Object output;
	public String[] execTrace;
	public HashMap coverFreq; //programElem(String)->coverTimes
	public Vector<Integer> hitSet; //hit values for each elements(integer rather than binary values)
	 
	
	
	
	
	public TestCase(){
		
	}
	
	public TestCase(String testcase){
		String[] ts = testcase.split("\t");
		this.index = ts[0];
		this.length = ts[1];
		this.CI = Double.parseDouble(ts[2]);
		this.activation = Integer.parseInt(ts[3]);
	}
	
	/**2009-12-18: for test suite reduction research, 
	 * more info are available for each test case
	 * 
	 * @param testcase
	 * @param reduction
	 */
	public TestCase(String testcase, boolean reduction){
		if(reduction){
			String[] tc = testcase.split("\t");
			this.index = tc[0];
			this.CI = Double.parseDouble(tc[1]);
			this.activation = Integer.parseInt(tc[2]);
			this.length = tc[3];
			this.execTime = Long.parseLong(tc[4]);
			this.hitCounter = Integer.parseInt(tc[5]);
			this.hitSet = new Vector<Integer>();
			for(int i = 6; i < tc.length; i ++){
				this.hitSet.add(Integer.parseInt(tc[i]));
			}
		}
	}
	
//	public String toString(){
//		return "Index:" + this.index + "\tLength:" + this.length +
//		"\tContextDiversity:"+ this.CI + "\tActivation:" + this.activation;
//	}
	
	public String toString(){
		return this.CI+ "-" +this.activation;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generatd method stub
		TestCase testcase = new TestCase("-5000	16	5	0.3125");
		System.out.println("");
	}

}
