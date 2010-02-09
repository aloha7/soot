package dua.unit;

import soot.Context;
import soot.jimple.spark.pag.AllocNode;

/** An allocation site with a 1-level-depth context (i.e. 'this') under which it is invoked */
public class ContextualAllocNode {
	Context ctx;
	AllocNode allocNode;
	
	static ContextualAllocNode nullCtxAllocNode = new ContextualAllocNode(null, null);
	public static ContextualAllocNode getNullCtxAllocNode() { return nullCtxAllocNode; }
	
	public AllocNode getAllocNode() {
		return allocNode;
	}
	public Context getCtx() {
		return ctx;
	}
	public ContextualAllocNode(Context ctx, AllocNode allocNode) {
		this.ctx = ctx;
		this.allocNode = allocNode;
	}
	@Override
	public String toString() {
		return "CTX_ALLOC_NODE( " + ctx + "." + allocNode + " )";
	}
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ContextualAllocNode) {
			ContextualAllocNode rhs = (ContextualAllocNode) obj;
			return ((ctx == null)? (rhs.getCtx() == null) : ctx.equals(rhs.getCtx())) &&
				   ((allocNode == null)? (rhs.getAllocNode() == null) : allocNode.equals(rhs.getAllocNode()));
		}
		else
			return false;
	}
	@Override
	public int hashCode() {
		return ((ctx==null)? 0 : ctx.hashCode()) * 31 + ((allocNode==null)? 0 : allocNode.hashCode());
	}
}

