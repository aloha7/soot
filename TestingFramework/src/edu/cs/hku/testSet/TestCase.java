package edu.cs.hku.testSet;

import java.util.HashSet;
import java.util.Random;

import edu.cs.hku.util.Constants;

public class TestCase {
	public Object input;
	public Object output;
	public double context_diversity = 0.0;
	
	
	/**A set of faults which can be exposed by this test case
	 * 
	 */
	public HashSet<Integer> validFaults = new HashSet<Integer>(); 
	
	
	public TestCase(Object input){
		this(input, null);
	}
	
	public TestCase(Object input, Object output){
		this(input, output, 0.0);
	}
	
	public TestCase(Object input, Object output, double CD){
		this.input = input;
		this.output = output;
		this.context_diversity = CD;
	}
	
	public String toString(){
		return input + "->" + output;
	}
	
	public static TestCase getRandomTestCase(){
		return new TestCase(getRandomInteger());
	}
	
	private static Integer getRandomInteger(){
		return new Random().nextInt(Constants.SIZE_TESTPOOL) + Constants.START_TESTPOOL;
	}
	
	/**Get a test case whose input is different from all in "inputs" 
	 * while trial time must be less than MAX_TRIAL_TIME defined in 
	 * edu.cs.hku.util.Constants
	 * 
	 * @param inputs: existing inputs
	 * @return: a test case whose input is different from all in "inputs"
	 */	
	public static TestCase getRandomTestCase(HashSet<Object> inputs){
		Integer input = null;
		int trial = 0;
		do{
			input = getRandomInteger();
			trial ++;
		}while(inputs.contains(input) && trial < Constants.MAX_TRIAL_TIME);
		if(inputs.contains(input)){
			System.out.println("Cannot find a non-visited test input within " 
					+ Constants.MAX_TRIAL_TIME);
			return null;
		}{
			return new TestCase(input);
		}
	}
}
