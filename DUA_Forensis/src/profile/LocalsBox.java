package profile;

/** 
 * Represents activation frame at the change point with locals of interest (i.e., inputs).
 *   *** planned for near future: link to boxes of callers, to access their respective locals
 * 
 * We need this class because Java reflection doesn't access locals.
 * 
 * First local in list is either 'this' (local 0, instance method), or a Class object for
 * static methods (in which case, locals start at position 1).
 * 
 * CAREFUL: this class is analyzed by Soot for instrumentation, so it's safer to keep
 * it compatible with Java 1.4
 */
public class LocalsBox {
	/** Used only to force Soot to load this class when analyzing */
	public static void __link() { }
	
	private LocalsBox prevLB = null; // link to calling activation frame (locals box)
	private int sCallerId; // id of caller stmt for next box; -1 if initial box
	private Object[] locals; // first param is either 'this' or Class for static method
	
	public LocalsBox getPrevLB() { return prevLB; }
	public void setPrevLB(LocalsBox lbPrev) { prevLB = lbPrev; }
	
	public int getStmtCallerId() { return sCallerId; }
	public Object[] getLocals() { return locals; }
	public Object getLocalVal(int idx) { return locals[idx + (isStatic()? 1 : 0)]; }
	
//	@Deprecated
	public Class getStaticClass() { return isStatic()? (Class)locals[0] : null; }
//	@Deprecated
	public boolean isStatic() { return (locals.length == 0)? false : locals[0] instanceof Class; }
	
	public LocalsBox(int size, int sCallerId) {
//		System.out.println("LocalsBox: created size " + size + " called id " + sCallerId);
		
		this.sCallerId = sCallerId;
		this.locals = new Object[size];
	}
	
	public void setLocal(int idx, Object o) {
//		System.out.println("LocalsBox: set idx " + idx + " obj type " + ((o==null)?null:o.getClass()));
		locals[idx] = o;
	}
	
//	@Override
	public String toString() {
		// first, print called id, if any
		String str = ((sCallerId == -1)? "START" : "" + sCallerId) + ": ";
		
		// second, print locals
		for (int i = 0; i < locals.length; ++i)
			str += locals[i] + ", ";
		if (locals.length > 0)
			str = str.substring(0, str.length() - 2);
		
		// third, print prev linked LB, if any
		if (prevLB != null)
			str += " | " + prevLB.toString();
		
		return str;
	}
	
}
