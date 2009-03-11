package ccr.app;

import ccr.app.*;

public class TestCFG1 extends Application {
	
	public Object application(String testcase) {
		
		// Program ID [a]
		
		// Ordinary Variable [a, c, s, u, v, y, z]
		
		// Context Variable [x]
		
		// Assignment [=, ]
		
		// ENTRY // NODE
		int x = 1; // NODE Def[x]
		int c = 2; // NODE Def[c]
		int y = 3;; // NODE Def[y]
		int a = 0;
		if (x > 0 & c > 0) { // NODE Use[x, c]
			if (c == (2 * c / 2)) { // NODE Use[c]
				x = 0; // NODE Def[x]
			}
			y = y + x; // NODE Def[y] Use[x, y]
			
			
			x = 1; // NODE Def[x]
			int u = a * a; // NODE Def[u] Use[a]
			int v = 2 * c; // NODE Def[v] Use[c]
			int z; // NODE
			if (v > 10) { // NODE Def[v]
				z = v - 10; // NODE Def[z] Use[v]
			} else {
				z = v; // NODE Def[z] Use[v]
			}
			y = y - 1; // NODE Def[y] Use[y]
			String s = "string";
			if (true) {
				;
			} else if (true) 
				x=x+2;
			else if (true)
				if (true)
					if (true)
						while (true) ;
						else x=x+5;
			if (true) {
				if (true)
					;
			} else x=x+1;
		}
		return null;
		// EXIT // NODE
	}
	
	protected void resolve() {
		
		// Policy Context[x] Constraint[constraint1 on x] Solution[discard]
		// Policy Context[x] Constraint[constraint2 on x] Solution[revise]
		// Context definition
	}
	
}
