package ccr.app;

import ccr.app.*;

public class TestCFG extends Application {
	
	public Object application(String testcase) {
		
		// Program ID [a]
		
		// Ordinary Variable [a, c, s, u, v, y, z]
		
		// Context Variable [x]
		
		// Assignment [=, ]
		
		// ENTRY
		int x = 1;
		int c = 2;
		int y = 3;;
		int a = 0;
		if (x > 0 & c > 0) {
			if (c == (2 * c / 2)) {
				x = 0;
			}
			y = y + x;
			
			
			x = 1;
			int u = a * a;
			int v = 2 * c;
			int z;
			if (v > 10) {
				z = v - 10;
			} else {
				z = v;
			}
			y = y - 1;
			String s = "string";
			if (true) {
				;
			} else if (true) 
				x=x+2;
			else if (true) //a23
				if (true)  //There is no "{" here, so we have to add it in the following
					if (true)
						while (true) ;
						else x=x+5;
			if (true) { //29
				if (true)
					;
			} else x=x+1;
		}
		return new Integer(x);
		// EXIT
	}
	
	protected void resolve() {
		
		// Policy Context[x] Constraint[constraint1 on x] Solution[discard]
		// Policy Context[x] Constraint[constraint2 on x] Solution[revise]
		// Context definition
	}
	
	//Should think about how to handle with if(A){} else{} 
}
