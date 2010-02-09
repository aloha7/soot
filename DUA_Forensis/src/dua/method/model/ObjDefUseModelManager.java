
package dua.method.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import soot.NullType;
import soot.RefLikeType;
import soot.Scene;
import soot.Value;
import soot.jimple.InvokeExpr;
import soot.jimple.Jimple;

/** Provides objects defined and used at modeled method calls, for specific methods. */
public class ObjDefUseModelManager {
	
	public static abstract class AbstractDefUseModel {
		protected static List<Value> emptyList = new ArrayList<Value>();
		public List<Value> getDefs(InvokeExpr invExp) { return emptyList; }
		public List<Value> getUses(InvokeExpr invExp) { return emptyList; }
	}
	
	public static final class EmptyModel extends AbstractDefUseModel {
		private EmptyModel() {}
		public static final AbstractDefUseModel inst = new EmptyModel();
	}
	
	public static final class DefThisModel extends AbstractDefUseModel {
		private DefThisModel() {}
		public static final AbstractDefUseModel inst = new DefThisModel();
		@Override public List<Value> getDefs(InvokeExpr invExp) { List<Value> defs = new ArrayList<Value>(); defs.add(invExp); return defs; }
	}
	public static final class UseThisModel extends AbstractDefUseModel {
		private UseThisModel() {}
		public static final AbstractDefUseModel inst = new UseThisModel();
		@Override public List<Value> getUses(InvokeExpr invExp) { List<Value> uses = new ArrayList<Value>(); uses.add(invExp); return uses; }
	}
	public static final class UseThisArgModel extends AbstractDefUseModel {
		private UseThisArgModel() {}
		public static final AbstractDefUseModel inst = new UseThisArgModel();
		@Override public List<Value> getUses(InvokeExpr invExp) { List<Value> uses = new ArrayList<Value>(); uses.add(invExp); uses.add(invExp.getArg(0)); return uses; }
	}
	public static final class DefThisUseThisModel extends AbstractDefUseModel {
		private DefThisUseThisModel() {}
		public static final AbstractDefUseModel inst = new DefThisUseThisModel();
		@Override public List<Value> getDefs(InvokeExpr invExp) { List<Value> defs = new ArrayList<Value>(); defs.add(invExp); return defs; }
		@Override public List<Value> getUses(InvokeExpr invExp) { List<Value> uses = new ArrayList<Value>(); uses.add(invExp); return uses; }
	}
	public static final class DefThisUseArgModel extends AbstractDefUseModel {
		private DefThisUseArgModel() {}
		public static final AbstractDefUseModel inst = new DefThisUseArgModel();
		@Override public List<Value> getDefs(InvokeExpr invExp) { List<Value> defs = new ArrayList<Value>(); defs.add(invExp); return defs; }
		@Override public List<Value> getUses(InvokeExpr invExp) { List<Value> uses = new ArrayList<Value>(); uses.add(invExp.getArg(0)); return uses; }
	}
	public static final class DefThisUseThisArgModel extends AbstractDefUseModel {
		private DefThisUseThisArgModel() {}
		public static final AbstractDefUseModel inst = new DefThisUseThisArgModel();
		@Override public List<Value> getDefs(InvokeExpr invExp) { List<Value> defs = new ArrayList<Value>(); defs.add(invExp); return defs; }
		@Override public List<Value> getUses(InvokeExpr invExp) { List<Value> uses = new ArrayList<Value>(); uses.add(invExp); uses.add(invExp.getArg(0)); return uses; }
	}
	public static final class DefThisUseThisAllArgsModel extends AbstractDefUseModel {
		private DefThisUseThisAllArgsModel() {}
		public static final AbstractDefUseModel inst = new DefThisUseThisAllArgsModel();
		@Override public List<Value> getDefs(InvokeExpr invExp) { List<Value> defs = new ArrayList<Value>(); defs.add(invExp); return defs; }
		@Override public List<Value> getUses(InvokeExpr invExp) {
			List<Value> uses = new ArrayList<Value>();
			uses.add(invExp);
			for (int i = 0; i < invExp.getArgCount(); ++i) {
				Value val = (Value) invExp.getArg(i);
				if (val.getType() instanceof RefLikeType && !(val.getType() instanceof NullType))
					uses.add(val);
			}
			return uses;
		}
	}
	public static final class UseArgModel extends AbstractDefUseModel {
		private UseArgModel() {}
		public static final AbstractDefUseModel inst = new UseArgModel();
		@Override public List<Value> getUses(InvokeExpr invExp) { List<Value> uses = new ArrayList<Value>(); uses.add(invExp.getArg(0)); return uses; }
	}
	public static class DefNewObjModel extends AbstractDefUseModel {
		protected final String clsName;
		private DefNewObjModel(String clsName) { this.clsName = clsName; }
		private static final Map<String,AbstractDefUseModel> instMap = new HashMap<String, AbstractDefUseModel>();
		public static AbstractDefUseModel inst(String clsName) {
			AbstractDefUseModel model = instMap.get(clsName);
			if (model == null) {
				model = new DefNewObjModel(clsName);
				instMap.put(clsName, model);
			}
			return model;
		}
		@Override public List<Value> getDefs(InvokeExpr invExp) {
			List<Value> defs = new ArrayList<Value>();
			defs.add( Jimple.v().newNewExpr(Scene.v().getSootClass(clsName).getType()) );
			return defs;
		}
	}
	public static final class DefNewObjUseThisModel extends DefNewObjModel {
		private DefNewObjUseThisModel(String clsName) { super(clsName); }
		private static final Map<String,AbstractDefUseModel> instMap = new HashMap<String, AbstractDefUseModel>();
		public static AbstractDefUseModel inst(String clsName) {
			AbstractDefUseModel model = instMap.get(clsName);
			if (model == null) {
				model = new DefNewObjUseThisModel(clsName);
				instMap.put(clsName, model);
			}
			return model;
		}
		@Override public List<Value> getUses(InvokeExpr invExp) { List<Value> uses = new ArrayList<Value>(); uses.add(invExp); return uses; }
	}
	public static final class DefNewObjUseArgModel extends DefNewObjModel {
		private DefNewObjUseArgModel(String clsName) { super(clsName); }
		private static final Map<String,AbstractDefUseModel> instMap = new HashMap<String, AbstractDefUseModel>();
		public static AbstractDefUseModel inst(String clsName) {
			AbstractDefUseModel model = instMap.get(clsName);
			if (model == null) {
				model = new DefNewObjUseArgModel(clsName);
				instMap.put(clsName, model);
			}
			return model;
		}
		@Override public List<Value> getUses(InvokeExpr invExp) { List<Value> uses = new ArrayList<Value>(); uses.add(invExp.getArg(0)); return uses; }
	}
	public static final class DefNewObjUseThisArgModel extends DefNewObjModel {
		private DefNewObjUseThisArgModel(String clsName) { super(clsName); }
		private static final Map<String,AbstractDefUseModel> instMap = new HashMap<String, AbstractDefUseModel>();
		public static AbstractDefUseModel inst(String clsName) {
			AbstractDefUseModel model = instMap.get(clsName);
			if (model == null) {
				model = new DefNewObjUseThisArgModel(clsName);
				instMap.put(clsName, model);
			}
			return model;
		}
		@Override public List<Value> getUses(InvokeExpr invExp) { List<Value> uses = new ArrayList<Value>(); uses.add(invExp); uses.add(invExp.getArg(0)); return uses; }
	}
	
	/** Maps method signature to corresponding def/use model. */
	private static Map<String, AbstractDefUseModel> sigToModel = null; // will be constructed when first accessed
	private static Map<String, AbstractDefUseModel> getCreateSigToModelMap() { if (sigToModel == null) { buildSigToModelMap(); } return sigToModel; }
	
	private static void buildSigToModelMap() {
		assert sigToModel == null;
		sigToModel = new HashMap<String, AbstractDefUseModel>();
		
		///
		/// CONVENTION: new expressions SHOULD NOT define object; ctors define object for the first time
		///             In this way, uses can be associated to defined object at the same node
		
		// my own reporter classes
		sigToModel.put("<change.ChainReporter: void __link()>", EmptyModel.inst);
		sigToModel.put("<change.ChangeReporter: void __link()>", EmptyModel.inst);
		sigToModel.put("<change.SPAReporter: void __link()>", EmptyModel.inst);
		sigToModel.put("<change.StateReporter: void __link()>", EmptyModel.inst);
		sigToModel.put("<profile.BranchReporter: void __link()>", EmptyModel.inst);
		sigToModel.put("<profile.CommonReporter: void __link()>", EmptyModel.inst);
		sigToModel.put("<profile.DUAReporter: void __link()>", EmptyModel.inst);
		sigToModel.put("<profile.PathReporter: void __link()>", EmptyModel.inst);
		sigToModel.put("<profile.TimeReporter: void __link()>", EmptyModel.inst);
		// java library classes
		sigToModel.put("<java.io.PrintStream: void println(int)>", DefThisUseThisModel.inst); // uses and modifies stream (this) object
		sigToModel.put("<java.io.PrintStream: void println(java.lang.String)>", DefThisUseThisArgModel.inst); // uses and modifies stream (this) object, and uses arg
		sigToModel.put("<java.io.PushbackReader: void <init>(java.io.Reader)>", DefThisUseArgModel.inst);
		sigToModel.put("<java.io.PushbackReader: void <init>(java.io.Reader,int)>", DefThisUseArgModel.inst);
		sigToModel.put("<java.io.PushbackReader: void close()>", DefThisUseThisModel.inst);
		sigToModel.put("<java.io.PushbackReader: int read()>", DefThisUseThisModel.inst);
		sigToModel.put("<java.io.PushbackReader: void unread(int)>", DefThisUseThisModel.inst);
		sigToModel.put("<java.lang.Integer: int parseInt(java.lang.String)>", UseArgModel.inst);
		sigToModel.put("<java.lang.Integer: java.lang.String toHexString(int)>", DefNewObjModel.inst("java.lang.String"));
		sigToModel.put("<java.lang.Integer: java.lang.String toString(int)>", DefNewObjModel.inst("java.lang.String"));
		sigToModel.put("<java.lang.Math: double abs(double)>", EmptyModel.inst);
		sigToModel.put("<java.lang.Math: double exp(double)>", EmptyModel.inst);
		sigToModel.put("<java.lang.Math: double log(double)>", EmptyModel.inst);
		sigToModel.put("<java.lang.Math: double sin(double)>", EmptyModel.inst);
		sigToModel.put("<java.lang.Object: void <init>()>", EmptyModel.inst); // IMPORTANT: no def because this is normally called from APP ctor!
		sigToModel.put("<java.lang.Runtime: void exit(int)>", EmptyModel.inst);
		sigToModel.put("<java.lang.Runtime: java.lang.Runtime getRuntime()>", EmptyModel.inst); // just ignore Runtime object, for now at least
		sigToModel.put("<java.lang.String: char charAt(int)>", UseThisModel.inst);
		sigToModel.put("<java.lang.String: java.lang.String concat(java.lang.String)>", DefNewObjUseThisArgModel.inst("java.lang.String"));
		sigToModel.put("<java.lang.String: boolean equals(java.lang.Object)>", UseThisArgModel.inst);
		sigToModel.put("<java.lang.String: int length()>", UseThisModel.inst);
		sigToModel.put("<java.lang.String: java.lang.String valueOf(java.lang.Object)>", DefNewObjUseArgModel.inst("java.lang.String"));
		sigToModel.put("<java.lang.StringBuffer: void <init>(java.lang.String)>", DefThisUseArgModel.inst);
		sigToModel.put("<java.lang.StringBuffer: java.lang.StringBuffer append(java.lang.Object)>", DefThisUseThisArgModel.inst);
		sigToModel.put("<java.lang.StringBuffer: java.lang.StringBuffer append(java.lang.String)>", DefThisUseThisArgModel.inst); 
		sigToModel.put("<java.lang.StringBuffer: java.lang.StringBuffer append(boolean)>", DefThisUseThisModel.inst);
		sigToModel.put("<java.lang.StringBuffer: java.lang.StringBuffer append(char)>", DefThisUseThisModel.inst);
		sigToModel.put("<java.lang.StringBuffer: java.lang.StringBuffer append(char[])>", DefThisUseThisArgModel.inst);
		sigToModel.put("<java.lang.StringBuffer: java.lang.StringBuffer append(char[],int,int)>", DefThisUseThisArgModel.inst);
		sigToModel.put("<java.lang.StringBuffer: java.lang.StringBuffer append(double)>", DefThisUseThisModel.inst);
		sigToModel.put("<java.lang.StringBuffer: java.lang.StringBuffer append(float)>", DefThisUseThisModel.inst);
		sigToModel.put("<java.lang.StringBuffer: java.lang.StringBuffer append(int)>", DefThisUseThisModel.inst);
		sigToModel.put("<java.lang.StringBuffer: java.lang.StringBuffer append(long)>", DefThisUseThisModel.inst);
		sigToModel.put("<java.lang.StringBuffer: java.lang.String toString()>", DefNewObjUseThisModel.inst("java.lang.String"));
		sigToModel.put("<java.lang.System: void exit(int)>", EmptyModel.inst);
		sigToModel.put("<java.util.Enumeration: boolean hasMoreElements()>", UseThisModel.inst);
		sigToModel.put("<java.util.Enumeration: java.lang.Object nextElement()>", DefThisUseThisModel.inst);
		// NOTE: the following really uses first arg obj, but only (potentially) stores ref to second arg
		sigToModel.put("<java.util.Hashtable: java.lang.Object get(java.lang.Object)>", UseThisArgModel.inst);
		sigToModel.put("<java.util.Hashtable: java.lang.Object put(java.lang.Object,java.lang.Object)>", DefThisUseThisArgModel.inst);
		sigToModel.put("<java.util.Hashtable: java.util.Enumeration keys()>", DefNewObjUseThisModel.inst("java.util.Enumeration"));
		sigToModel.put("<java.util.Stack: boolean empty()>", UseThisModel.inst);
		sigToModel.put("<java.util.Stack: java.lang.Object pop()>", DefThisUseThisModel.inst);
		// TODO: fix -- the following doesn't really use arg object! (just stores ref to it)
		sigToModel.put("<java.util.Stack: java.lang.Object push(java.lang.Object)>", DefThisUseThisArgModel.inst);
	}
	
	/** Returns list of objects defined *inside* model for method(s) called by invoke expression.
	 *  Does *not* include any argument-string automatically created from raw-string constant (because it occurs outside and before lib call).
	 *  Primitive parameters are *not* used or defined (only referred obj, if param is a ref). */
	public static List<Value> getInternalObjDefs(InvokeExpr invExp) {
		final String tgtMtd = invExp.getMethod().toString();
		
		AbstractDefUseModel duModel = getCreateSigToModelMap().get(tgtMtd);
		if (duModel == null) {
			System.out.println("Warning: def model not found for " + tgtMtd);
			return DefThisUseThisAllArgsModel.inst.getDefs(invExp);
		}
		return duModel.getDefs(invExp);
	}
	
	/** Returns list of objects used *inside* model for method(s) called by invoke expression.
	 *  Auto-created param string (occurs outside and before lib call) might be implicitly used if String arg is regarded as a use inside lib call.
	 *  Primitive parameters are *not* used or defined (only referred obj, if param is a ref). */
	public static List<Value> getInternalObjUses(InvokeExpr invExp) {
		final String tgtMtd = invExp.getMethod().toString();
		
		AbstractDefUseModel duModel = getCreateSigToModelMap().get(tgtMtd);
		if (duModel == null) {
			System.out.println("Warning: use model not found for " + tgtMtd);
			return DefThisUseThisAllArgsModel.inst.getUses(invExp);
		}
		return duModel.getUses(invExp);
	}
	
}
