package ccr.test;

public class TestCase {
	
	public String index;
	public String length;	
	public double CI;
	public int activation;
	
	public TestCase(String testcase){
		String[] ts = testcase.split("\t");
		this.index = ts[0];
		this.length = ts[1];
		this.CI = Double.parseDouble(ts[2]);
		this.activation = Integer.parseInt(ts[3]);
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
