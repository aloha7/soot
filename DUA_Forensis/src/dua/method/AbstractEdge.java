package dua.method;

import dua.method.CFG.CFGNode;

/** Defines only edge's src on purpose, so subclasses decide what is the target */
public abstract class AbstractEdge {
	protected final CFGNode src; // null denotes virtual ENTRY node
	
	public CFGNode getSrc() { return src; }
	public CFGNode getTgt() { return null; } // no tgt by default
	
	public AbstractEdge(CFGNode src) {
		this.src = src;
	}
	
}
