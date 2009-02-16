package ccr.test;

public class TestCase {
	
	public String index;
	public String length;
	public String changes;
	public double CI;
	
	public TestCase(String testcase){
		String[] ts = testcase.split("\t");
		this.index = ts[0];
		this.length = ts[1];
		this.changes = ts[2];
		this.CI = Double.parseDouble(ts[3]);
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
