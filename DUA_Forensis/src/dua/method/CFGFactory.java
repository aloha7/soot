package dua.method;

import soot.SootMethod;

public interface CFGFactory {
	/** Just instantiates CFG. Does not perform any special analysis. */
	CFG createCFG(SootMethod m);
	/** Required for a CFG after instantiation of all CFGs. */
	void analyze(CFG cfg);
}
