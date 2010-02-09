package dua.method;

import java.util.List;

import soot.SootMethod;
import dua.method.CFG.CFGNode;
import dua.unit.StmtTag;

public final class CallEdge extends AbstractEdge {
	public CallEdge(CFGNode src) {
		super(src);
	}
	
	public List<SootMethod> getCallees() {
		StmtTag sTag = (StmtTag) src.s.getTag(StmtTag.TAG_NAME);
		return sTag.getAppCallSite().getAppCallees();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof CallEdge))
			return false;
		CallEdge other = (CallEdge)obj;
		return this.src == other.src;
	}
	@Override
	public int hashCode() { return src.hashCode(); }
	
	@Override
	public String toString() {
		return "calledge:"+ src.getIdStringInMethod();
	}
}
