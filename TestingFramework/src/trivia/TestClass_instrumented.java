package trivia;

import edu.cs.hku.instrument.Probe_stmt;


public class TestClass_instrumented {

	public static void main(String[] args) {		
		int a = -1, b= 2, c=0, d=-1;Probe_stmt.getInstance().cover(9);		
		if(a > 0){			Probe_stmt.getInstance().cover(10);
			c = a + b;Probe_stmt.getInstance().cover(11);			
			d = a - b;Probe_stmt.getInstance().cover(12);
		}else{
			c = a - b;Probe_stmt.getInstance().cover(14);
			d = a + b;Probe_stmt.getInstance().cover(15);
		}
		
		d = c * d;Probe_stmt.getInstance().cover(18);
		c = c * d;Probe_stmt.getInstance().cover(19);
		
		Probe_stmt.getInstance().report();
	}

}
