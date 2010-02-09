package dua.method;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import soot.Local;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.Value;
import soot.ValueBox;
import soot.VoidType;
import soot.jimple.ArrayRef;
import soot.jimple.AssignStmt;
import soot.jimple.Constant;
import soot.jimple.DefinitionStmt;
import soot.jimple.FieldRef;
import soot.jimple.IdentityStmt;
import soot.jimple.InstanceFieldRef;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.Jimple;
import soot.jimple.NewArrayExpr;
import soot.jimple.NewExpr;
import soot.jimple.ReturnStmt;
import soot.jimple.StaticFieldRef;
import soot.jimple.StaticInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;
import dua.Options;
import dua.global.ProgramFlowGraph;
import dua.global.p2.P2Analysis;
import dua.method.CFGDefUses.Variable.VariableComparator;
import dua.method.model.ObjDefUseModelManager;
import dua.unit.StmtTag;

/**
 * Stmt-level CFG that identifies defs and uses.
 * Distinguishes c-uses from p-uses.
 * 
 * Identifies defs and uses of fields, array elements, and objects.
 * A library method call is treated as a use and def of the heap object.
 * The length field of arrays is treated abstractly as a use of (any element in) the array object.
 */
public class CFGDefUses extends CFG {
	public static class NodeDefUses extends CFGNode {
		protected int[] localDefsIds;// = Def.NODEF; // NODEF if no def
		protected int[] localUsesIds;
		
		public void setLocalDefsIds(int[] localDefsIds) { this.localDefsIds = localDefsIds; }
		public int[] getLocalDefsIds() { return localDefsIds; }
		public void setLocalUsesIds(int[] localUsesIds) { this.localUsesIds = localUsesIds; }
		public int[] getLocalUsesIds() { return localUsesIds; }
		
		public NodeDefUses(Stmt s) { super(s); }
		
		/** Includes ALL uses of NON-constants: locals, fields, array elems, objects (incl. str-const lib params) */
		public List<Variable> getUsedVars() {
			Set<Variable> varsSet = new HashSet<Variable>();
			// get list of local variables used
			CFGDefUses cfgDU = (CFGDefUses) ProgramFlowGraph.inst().getContainingCFG(this);
			for (int uId : getLocalUsesIds()) {
				Variable v = cfgDU.getUses().get(uId).getVar();
				varsSet.add(v);
			}
			// add used field and array-element variables
			for (Use u : cfgDU.getFieldUses())
				if (u.getN() == this)
					varsSet.add(u.getVar());
			for (Use u : cfgDU.getArrayElemUses())
				if (u.getN() == this)
					varsSet.add(u.getVar());
			for (Use u : cfgDU.getLibObjUses())
				if (u.getN() == this)
					varsSet.add(u.getVar());
			
			List<Variable> sortedVarsList = new ArrayList<Variable>(varsSet);
			Collections.sort(sortedVarsList, new VariableComparator());
			return sortedVarsList;
		}
		
		/** Includes ALL defs of NON-constants: locals, fields, array elems, objects (incl. str-const lib params) */
		public List<Variable> getDefinedVars() {
			Set<Variable> varsSet = new HashSet<Variable>();
			// get list of local variables defined
			CFGDefUses cfgDU = (CFGDefUses) ProgramFlowGraph.inst().getContainingCFG(this);
			for (int dId : getLocalDefsIds()) {
				Variable v = cfgDU.getDefs().get(dId).getVar();
				varsSet.add(v);
			}
			// add defined field and array-element variables
			for (Def d : cfgDU.getFieldDefs())
				if (d.getN() == this)
					varsSet.add(d.getVar());
			for (Def d : cfgDU.getArrayElemDefs())
				if (d.getN() == this)
					varsSet.add(d.getVar());
			for (Def d : cfgDU.getLibObjDefs())
				if (d.getN() == this)
					varsSet.add(d.getVar());
			
			List<Variable> sortedVarsList = new ArrayList<Variable>(varsSet);
			Collections.sort(sortedVarsList, new VariableComparator());
			return sortedVarsList;
		}
	}
	
	public static class Branch {
		protected CFGNode src;
		protected CFGNode tgt; //@ invariant tgt != null
		
		public CFGNode getSrc() { return src; }
		public CFGNode getTgt() { return tgt; }
		
		public Branch(CFGNode src, CFGNode tgt) {
			this.src = src;
			this.tgt = tgt;
		}
		
		@Override
		public int hashCode() { return ((src == null)? 0 : src.hashCode()) + tgt.hashCode(); }
		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof Branch))
				return false;
			
			Branch brOther = (Branch) obj;
			return this.src == brOther.src && this.tgt == brOther.tgt;
		}
		@Override
		public String toString() {
			String str;
			if (src == null)
				str = "MTD_EN";
			else {
				final String srcSId = src.getIdStringInMethod();
				final int srcMIdx = ProgramFlowGraph.inst().getContainingMethodIdx(src);
				str = srcMIdx + "[" + srcSId + "]";
			}
			final String tgtSId = tgt.getIdStringInMethod();
			final int tgtMIdx = ProgramFlowGraph.inst().getContainingMethodIdx(tgt);
			return str + "->" + tgtMIdx + "[" + tgtSId + "]";
		}
		
		public static class BranchComparator implements Comparator<Branch> {
			public int compare(Branch br1, Branch br2) {
				// compare containing methods first
				CFG cfg1 = ProgramFlowGraph.inst().getContainingCFG(br1.getTgt());
				CFG cfg2 = ProgramFlowGraph.inst().getContainingCFG(br2.getTgt());
				if (cfg1 != cfg2)
					return (ProgramFlowGraph.inst().getMethodIdx(cfg1.getMethod()) < ProgramFlowGraph.inst().getMethodIdx(cfg2.getMethod()))? -1 : 1;
				
				return br1.toString().compareTo(br2.toString());
			}
		}
	}
	
	/** Wrapper to provide equals and hashCode for regular vars (i.e., locals, fields, array elems, and lib objects) */
	public static abstract class Variable {
		/** Constant for constant call argument; Local for local var; FieldRef for field (base is ignored); ArrayRef for
		 *  array-element (base is ignored); InvokeExpr for lib object (instance or static/class) (base is ignored). */
		protected final Value val; // value
		public Value getValue() { return val; }
		public Variable(Value val) { this.val = val; }
		
		public abstract boolean isConstant();
		public abstract boolean isLocal();
		public abstract boolean isLocalOrConst();
		public abstract boolean isFieldRef();
		public abstract boolean isArrayRef();
		public abstract boolean isObject();
		public abstract boolean isStrConstObj();
		public abstract boolean isReturnedVar();
		
		public boolean isDefinite() { return dua.util.Util.isValueDefinite(val); }
		
		/** Whether vars may "equal" (as vars, regardless of possible non-aliasing). */
		public abstract boolean mayEqual(Variable vOther);
		/** Whether vars may "equal" and in addition may point to same object (when applicable). */
		public boolean mayEqualAndAlias(Variable vOther) { return mayEqual(vOther) && mayAlias(vOther); }
		protected abstract boolean mayAlias(Variable vOther);
		
		public static class VariableComparator implements Comparator<Variable> {
			public int compare(Variable v1, Variable v2) { return v1.toString().compareTo(v2.toString()); }
		}
	}
	/** Represents all variables other than (lib) objects. */
	public static class StdVariable extends Variable {
		public StdVariable(Value val) { super(val); } //assert isLocalOrConst() || isFieldRef() || isArrayRef(); }
		@Override
		public boolean equals(Object obj) { if (!(obj instanceof StdVariable)) return false;
											StdVariable vOther = (StdVariable)obj;
											return dua.util.Util.valuesEqual(val, vOther.val); }
		@Override
		public int hashCode() { return dua.util.Util.valueHashCode(val); }
		@Override
		public String toString() {
			if (val instanceof Local) return val.toString();
			if (val instanceof FieldRef) { return "F" + ((FieldRef) val).getField().getName(); }
			
			assert val instanceof ArrayRef;
			
			ArrayRef a = (ArrayRef) val;
			Value vIdx = a.getIndex();
			String arrType = a.getBase().getType().toString();
			arrType = arrType.replace('/', '.').replace("[]", "");
			return "A" + arrType + ((vIdx instanceof Constant)? "I" + vIdx : "");
		}
		@Override public boolean isConstant() { return val instanceof Constant; }
		@Override public boolean isLocal() { return val instanceof Local; }
		@Override public boolean isLocalOrConst() { return isLocal() || isConstant(); }
		@Override public boolean isFieldRef() { return val instanceof FieldRef; }
		@Override public boolean isArrayRef() { return val instanceof ArrayRef; }
		@Override public boolean isObject() { return false; }
		@Override public boolean isStrConstObj() { return false; }
		@Override public boolean isReturnedVar() { return false; }
		
		@Override public boolean isDefinite() { return dua.util.Util.isValueDefinite(val); }
		
		/** Whether vars may "equal" (as vars, regardless of possible non-aliasing). */
		@Override public boolean mayEqual(Variable vOther) { return equals(vOther); }
		/** Whether bases can point to same object (alloc site), if applicable. */
		@Override protected boolean mayAlias(Variable vOther) {
			// compare p2 sets of bases, if they have bases
			if (val instanceof InstanceFieldRef) {
				Local lBaseThis = (Local) ((InstanceFieldRef)this.val).getBase();
				BitSet p2BaseThis = P2Analysis.inst().getP2Set(lBaseThis);
				Local lBaseOther = (Local) ((InstanceFieldRef)vOther.val).getBase();
				BitSet p2BaseOther = P2Analysis.inst().getP2Set(lBaseOther);
				return p2BaseThis.intersects(p2BaseOther) ||
					   (p2BaseThis.isEmpty() && p2BaseOther.isEmpty() && lBaseThis == lBaseOther);
			}
			else if (val instanceof ArrayRef) {
				Local lBaseThis = (Local) ((ArrayRef)this.val).getBase();
				BitSet p2BaseThis = P2Analysis.inst().getP2Set(lBaseThis);
				Local lBaseOther = (Local) ((ArrayRef)vOther.val).getBase();
				BitSet p2BaseOther = P2Analysis.inst().getP2Set(lBaseOther);
				return p2BaseThis.intersects(p2BaseOther) ||
					   (p2BaseThis.isEmpty() && p2BaseOther.isEmpty() && lBaseThis == lBaseOther);
			}
			return true;
		}
	}
	/** Distinguishes heap object variable from ref var (represented by superclass Variable).
	 *  These types of Value can represent an object var:
	 *    1. NewExpr/NewArrayExpr (instance)
	 *    2. InvokeExpr           (static or instance)
	 *    3. Local/statfield ref var  (instance)
	 *    4. StringConstant       (instance)
	 *  The last two occur only as (lib) call arguments.
	 */
	public static class ObjVariable extends Variable {
		/** Might appear counter-intuitive to the notion of location-less var,
		 *  but node actually allows to distinguish different vars (e.g., string constants). */
		protected final CFGNode n;
		public ObjVariable(Value val, CFGNode n) { super(val); this.n = n; }
		/** Returns true only if objects MUST NECESSARILY be equal. */
		@Override public boolean equals(Object obj) {
			if (!(obj instanceof ObjVariable))
				return false;
//			// uses EXACT set match of possible runtime types (needed for transitivity of 'equals')
			ObjVariable vOther = (ObjVariable)obj;
//			if (!dua.util.Util.objValuesMustEqual(val, vOther.val))
//				return false;
			// class set that objects represent is NOT A SUFFICIENT condition for equality
			if (val instanceof NewExpr || val instanceof NewArrayExpr)
				return val == vOther.val; //this.n == vOther.n; // location distinguishes new-object expressions
			else if (val instanceof StaticInvokeExpr)
				return vOther instanceof StaticInvokeExpr &&
					   ((StaticInvokeExpr)val).getMethod().getDeclaringClass() == ((StaticInvokeExpr)vOther.val).getMethod().getDeclaringClass();
			else if (val instanceof InstanceInvokeExpr)
				return vOther instanceof InstanceInvokeExpr &&
					   ((InstanceInvokeExpr)val).getBase() == ((InstanceInvokeExpr)vOther.val).getBase();  // discard equality if bases are not the same
			else if (val instanceof Local)
				return val == vOther.val; // discard equality if locals are not the same
			else if (val instanceof StaticFieldRef)
				return vOther.val instanceof StaticFieldRef &&
					   ((StaticFieldRef)val).getField() == ((StaticFieldRef)vOther.val).getField(); // discard equality if static fields are not the same
			else {
				assert val instanceof StringConstant;
				return val.equals(vOther.val);
			}
		}
		@Override
		public int hashCode() { return dua.util.Util.objValueHashCode(val); }
		@Override
		public String toString() {
			final boolean instance = !(val instanceof StaticInvokeExpr);
			Type objType; // can be RefType or ArrayType
			if (val instanceof NewExpr)
				objType = ((NewExpr)val).getType();
			else if (val instanceof NewArrayExpr)
				objType = ((NewArrayExpr)val).getType(); // provides array type with type of element inside
			else if (val instanceof InvokeExpr)
				objType = ((InvokeExpr)val).getMethod().getDeclaringClass().getType();
			else if (val instanceof Local || val instanceof StaticFieldRef)
				objType = val.getType();
			else {
				assert (val instanceof StringConstant);
				objType = Scene.v().getSootClass("java.lang.String").getType();
			}
			
			return "O" + (instance? "I" : "C") + objType;
		}
		
		@Override public boolean isConstant() { return false; }
		@Override public boolean isLocal() { return false; }
		@Override public boolean isLocalOrConst() { return false; }
		@Override public boolean isFieldRef() { return false; }
		@Override public boolean isArrayRef() { return false; }
		@Override public boolean isObject() { return true; }
		@Override public boolean isStrConstObj() { return val instanceof StringConstant; }
		@Override public boolean isReturnedVar() { return false; }
		
		/** Whether vars may "equal" (as vars, regardless of possible non-aliasing). */
		@Override public boolean mayEqual(Variable vOther) {
			if (!(vOther instanceof ObjVariable))
				return false;
			return dua.util.Util.objValuesMayEqual(val, vOther.val);
		}
		/** Whether, in addition to basic equality, bases can point to same object (alloc site). */
		@Override protected boolean mayAlias(Variable vOther) {
			ObjVariable objVar = (ObjVariable) vOther;
			
			// handle non-instance case
			if (val instanceof StaticInvokeExpr) {
				if (!(objVar.val instanceof StaticInvokeExpr))
					return false;
				SootClass clsThis = ((StaticInvokeExpr)this.val).getMethodRef().declaringClass();
				SootClass clsOther = ((StaticInvokeExpr)objVar.val).getMethodRef().declaringClass();
				return clsThis.equals(clsOther);
			}
			if (objVar.val instanceof StaticInvokeExpr)
				return false;
			
			// instances: compare p2 sets
			BitSet p2This = this.getP2Set();
			BitSet p2Other = objVar.getP2Set();
			// handle degenerate case in which P2 analysis does NOT provide info, but vars match
			if (p2This.isEmpty() && p2Other.isEmpty()) {
				// special case: both are str constants
				if (this.val instanceof StringConstant && objVar.val instanceof StringConstant)
					return this.n == objVar.n && this.val.equals(objVar.val);
				// special case: both are static fld refs
				if (this.val instanceof StaticFieldRef && objVar.val instanceof StaticFieldRef)
					return this.val.equals(objVar.val);
				// if only one is a str const, that local will be null and the result will be false
				Local lBaseThis = getBaseLocal();
				Local lBaseOther = objVar.getBaseLocal();
				return lBaseThis == lBaseOther;
			}
			else
				return p2This.intersects(p2Other);
		}
		
		protected final static BitSet emptyBitset = new BitSet();
		/** Helper for mayAlias. */
		private BitSet getP2Set() {
			if (val instanceof NewExpr || val instanceof NewArrayExpr) {
				if (n.getStmt() instanceof DefinitionStmt) {
					Local lLeft = (Local) ((DefinitionStmt)(n.getStmt())).getLeftOp();
					return P2Analysis.inst().getP2Set(lLeft);
				}
				return emptyBitset; // WE ASSUME THAT OBJECT IS "LOST" - not assigned to a lhs reference
			}
			else if (val instanceof InstanceInvokeExpr) {
				Local lBase = (Local) ((InstanceInvokeExpr)n.getStmt().getInvokeExpr()).getBase();
				return P2Analysis.inst().getP2Set(lBase);
			}
			else if (val instanceof Local || val instanceof FieldRef) {
				return P2Analysis.inst().getP2Set(val);
			}
			else {
				assert val instanceof StringConstant;
				return P2Analysis.inst().getP2SetForAllocSite(n, val);
			}
		}
		public Local getBaseLocal() {
			// 1. NewExpr/NewArrayExpr (instance)
			if (val instanceof NewExpr || val instanceof NewArrayExpr)
				return (Local) ((DefinitionStmt)n.getStmt()).getLeftOp();
			// 2. InvokeExpr           (static or instance)
			else if (val instanceof StaticInvokeExpr)
				return null;
			else if (val instanceof InstanceInvokeExpr)
				return (Local) ((InstanceInvokeExpr)val).getBase();
			// 3.1 Local ref var (instance)
			else if (val instanceof Local)
				return (Local)val;
			// 3.2 statfield ref var  (instance)
			// 4.  StringConstant       (instance)
			else {
				assert val instanceof StaticFieldRef || val instanceof StringConstant;
				return null;
			}
		}
	}
	/** Represents special constant actual-argument as a "variable" at method call */
	public static final class CSConstParamVar extends StdVariable {
		private final CallSite cs; // location of call where this var is a param
		private final int argIdx; // position in argument list at call site
		public CSConstParamVar(Constant constVal, CallSite cs, int argIdx) { super(constVal); this.cs = cs; this.argIdx = argIdx; }
		@Override
		/** cs and argIdx are enough; don't use parent's val equality because of StringConstant's duality
		 *  (represents string ref and string itself created automatically for raw-string param) */
		public boolean equals(Object obj) { return
			((obj instanceof CSConstParamVar) && ((CSConstParamVar)(obj)).cs.equals(this.cs) && ((CSConstParamVar)(obj)).argIdx == this.argIdx); }
		@Override
		/** cs and argIdx are enough; don't use parent's hashCode because of StringConstant's duality
		 *  (represents string ref and string itself created automatically for raw-string param) */
		public int hashCode() { return cs.hashCode() + argIdx; }
		@Override
		public String toString() { return "constparC" + val + "I" + argIdx + "M" + ProgramFlowGraph.inst().getMethodIdx(cs.getLoc().getMethod()) + 
			"S" + ((StmtTag)cs.getLoc().getStmt().getTag(StmtTag.TAG_NAME)).getIdxInMethod(); }
	}
	/** Represents special kind of "variable" returned by method call at caller site, to facilitate data-dependence analysis */
	public static final class CSReturnedVar extends StdVariable {
		private CallSite cs; // location of call at which var is returned
		public CSReturnedVar(InvokeExpr vInvExpr, CallSite cs) { super(vInvExpr); this.cs = cs; }
		@Override
		public boolean equals(Object obj) { return ((obj instanceof CSReturnedVar) && ((CSReturnedVar)(obj)).cs.equals(this.cs)); }
		@Override
		public int hashCode() { return cs.hashCode(); }
		@Override
		public String toString() { return "retvarM" + ProgramFlowGraph.inst().getMethodIdx(cs.getLoc().getMethod()) + 
			"S" + ((StmtTag)cs.getLoc().getStmt().getTag(StmtTag.TAG_NAME)).getIdxInMethod(); }
		
		@Override
		public boolean isReturnedVar() { return true; }
		@Override
		public boolean isDefinite() { return true; }
	}
	/** Represents constant at return statement as a special kind of var (for data-dependence analysis purposes) */
	public static final class ConstReturnVar extends StdVariable {
		private final CFGNode retNode; // id within containing CFG
		public ConstReturnVar(Constant constVal, CFGNode retNode) { super(constVal); this.retNode = retNode; }
		@Override
		public boolean equals(Object obj) {
			return obj instanceof ConstReturnVar && super.equals(obj) && ((ConstReturnVar)(obj)).retNode == this.retNode; }
		@Override
		public int hashCode() { return super.hashCode() + retNode.hashCode(); }
		@Override
		public String toString() { return "constretC" + val +
			"S" + ((StmtTag)retNode.getStmt().getTag(StmtTag.TAG_NAME)).getIdxInMethod(); }
	}
	
	public static abstract class Use {
		protected Variable var; // variable that wraps value
		public Variable getVar() { return var; }
		public Value getValue() { return var.getValue(); }
		
		/** Default: creates StdVar, not ObjVar. */
		public Use(Value v) { this.var = new StdVariable(v); }
		protected Use(Variable var) { this.var = var; }
		/** Returns use's node (or starting node) */
		public abstract CFGNode getSrcNode();
		/** Returns use's node if use corresponds to a node; null if use's src is actually a branch. */
		public abstract CFGNode getN();
		/** Returns use's branch if use corresponds to a branch; null otherwise */
		public abstract Branch getBranch();
		public boolean isRetUse() { return var.val instanceof InvokeExpr; }
		public boolean isInCatchBlock() { return getSrcNode().isInCatchBlock(); }
		
		/** The purpose is to produce always the uses (and DUAs) in the same order */
		public static class UseComparator implements Comparator<Use> {
			public int compare(Use o1, Use o2) {
				// compare containing methods first
				SootMethod m1 = ProgramFlowGraph.inst().getContainingMethod(o1.getSrcNode().getStmt());
				SootMethod m2 = ProgramFlowGraph.inst().getContainingMethod(o2.getSrcNode().getStmt());
				if (m1 != m2)
					return (ProgramFlowGraph.inst().getMethodIdx(m1) < ProgramFlowGraph.inst().getMethodIdx(m2))? -1 : 1;
				
				return o1.toString().compareTo(o2.toString());
			}
		}
	}
	
	public static class CUse extends Use {
		private NodeDefUses n; // location
		
		/** Default: creates StdVar, not ObjVar. */
		public CUse(Value v, NodeDefUses n) { super(v); this.n = n; }
		/** Takes var as param, so var can be ObjVar instead of StdVar. */
		protected CUse(Variable var, NodeDefUses n) { super(var); this.n = n; }
		
		@Override
		public CFGNode getSrcNode() { return n; }
		@Override
		public CFGNode getN() { return n; }
		@Override
		public Branch getBranch() { return null; }
		
		@Override
		public String toString() {
			return var + "@" + ProgramFlowGraph.inst().getContainingMethodIdx(n.s) +
				"[" + ((StmtTag)n.s.getTag(StmtTag.TAG_NAME)).getIdxInMethod() + "]";
		}
	}
	public static class CSConstParamCUse extends CUse {
		public CSConstParamCUse(Constant constVal, CallSite cs, int argIdx, NodeDefUses n) {
			super(new CSConstParamVar(constVal, cs, argIdx), n); }
	}
	public static class CSReturnedVarCUse extends CUse {
		public CSReturnedVarCUse(InvokeExpr vInvExpr, CallSite cs, NodeDefUses n) {
			super(new CSReturnedVar(vInvExpr, cs), n); }
	}
	
	public static class PUse extends Use {
		private Branch br; // location
		
		public PUse(Value v, Branch br) { super(v); this.br = br; }
		
		@Override
		public CFGNode getSrcNode() { return br.src; }
		@Override
		public CFGNode getN() { return null; }
		@Override
		public Branch getBranch() { return br; }
		
		@Override
		public String toString() {
			return var + "@" + br;
		}
	}
	
	public static class Def {
		private Variable var; // value
		private CFGNode n; // location
		
		public Value getValue() { return var.getValue(); }
		public Variable getVar() { return var; }
		public CFGNode getN() { return n; }
		
		/** Tells whether uses or constants at def node are used to compute def value.
		 *  Note: in 'a = 1', 'a' is computed, because it's assigned; in 'm(1)', '1' is NOT computed. */
		public boolean isComputed() { return true; }
		
		/** Default: creates StdVar, not ObjVar. */
		public Def(Value v, CFGNode n) { this.var = new StdVariable(v); this.n = n; }
		/** Takes var as param, so var can be ObjVar instead of StdVar. */
		protected Def(Variable var, CFGNode n) { this.var = var; this.n = n; }
		
		@Override
		public String toString() {
			return var + "@" + ProgramFlowGraph.inst().getContainingMethodIdx(n) +
				"[" + ((n.s == null)? "EN" : ((StmtTag)n.s.getTag(StmtTag.TAG_NAME)).getIdxInMethod()) + "]";
		}
		public boolean isInCatchBlock() { return n.isInCatchBlock(); }
		
		/** The purpose is to produce always the defs (and DUAs) in the same order */
		public static class DefComparator implements Comparator<Def> {
			public int compare(Def o1, Def o2) {
				// compare containing methods first
				CFG cfg1 = ProgramFlowGraph.inst().getContainingCFG(o1.getN());
				CFG cfg2 = ProgramFlowGraph.inst().getContainingCFG(o2.getN());
				if (cfg1 != cfg2)
					return (ProgramFlowGraph.inst().getMethodIdx(cfg1.getMethod()) < ProgramFlowGraph.inst().getMethodIdx(cfg2.getMethod()))? -1 : 1;
				
				return o1.toString().compareTo(o2.toString());
			}
		}
	}
	public static class ConstCSParamDef extends Def {
		@Override
		public boolean isComputed() { return false; }
		public ConstCSParamDef(Constant constVal, CallSite cs, int argIdx, CFGNode n) {
			super(new CSConstParamVar(constVal, cs, argIdx), n); }
	}
	public static class ConstReturnDef extends Def {
		@Override
		public boolean isComputed() { return false; }
		public ConstReturnDef(Constant constVal, NodeDefUses n) {
			super(new ConstReturnVar(constVal, n), n); }
	}
	
	protected ArrayList<Use> idsToUses = new ArrayList<Use>(); // intra-procedural id->use map
	protected HashMap<Use, Integer> usesToIds = new HashMap<Use, Integer>(); // use->id map (reverse of previous map)
	protected ArrayList<Def> idsToDefs = new ArrayList<Def>(); // intra-procedural id->def map
	protected HashMap<Variable,BitSet> varsToUses = new HashMap<Variable,BitSet>(); // associates each var with all uses for it
	protected HashMap<Variable,BitSet> varsToDefs = new HashMap<Variable,BitSet>(); // associates each var with all defs for it
	protected List<Use> fieldUses = new ArrayList<Use>();
	protected List<Def> fieldDefs = new ArrayList<Def>();
	protected List<Use> arrElemUses = new ArrayList<Use>();
	protected List<Def> arrElemDefs = new ArrayList<Def>();
	/** All uses of class/static and instance (lib) objects. */
	protected List<Use> libObjUses = new ArrayList<Use>();
	/** All defs of class/static and instance (lib) objects. */
	protected List<Def> libObjDefs = new ArrayList<Def>();
	/** Uses of values returned from app method called from this CFG */
	protected Map<CFGNode,Use> callRetUses = new HashMap<CFGNode, Use>();
	/** Special defs of constant arguments to app method calls from this CFG */
	protected List<ConstCSParamDef> argConstDefs = new ArrayList<ConstCSParamDef>();
	/** Special defs of constant return-values to app method calling this CFG */
	protected List<ConstReturnDef> retConstDefs = new ArrayList<ConstReturnDef>();
	
	public List<Use> getUses() { return idsToUses; }
	public List<Def> getDefs() { return idsToDefs; }
	public int getUseId(Use use) { return usesToIds.get(use); }
	public List<Use> getFieldUses() { return fieldUses; }
	public List<Def> getFieldDefs() { return fieldDefs; }
	public List<Use> getArrayElemUses() { return arrElemUses; }
	public List<Def> getArrayElemDefs() { return arrElemDefs; }
	public List<Use> getLibObjUses() { return libObjUses; }
	public List<Def> getLibObjDefs() { return libObjDefs; }
	public List<Def> getConstDefs() { return new ArrayList<Def>(argConstDefs.isEmpty()? retConstDefs : argConstDefs); }
	
	public CFGDefUses(SootMethod m) {
		super(m);
	}
	@Override
	public void analyze() {
		super.analyze(); // should always perform superclass's initial analysis first
		
		System.out.println("CFG for " + ProgramFlowGraph.inst().getMethodIdx(method) + ": " + method);
		
		identifyDefsUses();
	}
	
	/** Factory method */
	@Override
	protected NodeDefUses instantiateNode(Stmt s) { return new NodeDefUses(s); }
	
	/** identify defs and uses per node */
	protected void identifyDefsUses() {
		final boolean allowParmsRetUseDefs = Options.allowParmsRetUseDefs();
		
		// find uses -- includes actual params in calls
		for (CFGNode _n : nodes) {
			if (_n instanceof CFGNodeSpecial)
				continue; // ENTRY or EXIT
			
			NodeDefUses n = (NodeDefUses)_n;
			Stmt s = n.getStmt();
			if (!(s instanceof IdentityStmt)) {
				// create uses for all local/field/array accesses
				List<Branch> outBrs = n.getOutBranches();
				List useBoxes = s.getUseBoxes();
				ArrayList<Use> uses = new ArrayList<Use>();
				// consider special uses: constant parameters in app calls, and returned values
				StmtTag sTag = (StmtTag) s.getTag(StmtTag.TAG_NAME);
				CallSite cs = sTag.getCallSite();
				for (Iterator itUse = useBoxes.iterator(); itUse.hasNext(); ) {
					Value useV = ((ValueBox) itUse.next()).getValue();
					if (useV instanceof Local) {
						if (outBrs != null) {  // create one p-use per successor of n
							for (Branch outBr : outBrs)
								uses.add(new PUse(useV, outBr));
						}
						else
							uses.add(new CUse(useV, n));
					}
					else if (useV instanceof FieldRef) {
						assert outBrs == null; // jimple does not have field refs in branching stmts
						fieldUses.add(new CUse(useV, n));
					}
					else if (useV instanceof ArrayRef) {
						assert outBrs == null; // jimple does not have array refs in branching stmts
						arrElemUses.add(new CUse(useV, n));
					}
					else if (useV instanceof Constant) {
						if (cs != null && cs.hasAppCallees()) {
							// NOTE: *includes* StringConstant, which here represents ref var for automatically-created string
							uses.add(new CSConstParamCUse((Constant)useV, cs, uses.size(), n));
						}
					}
//					else
//						System.out.println("Unsupported use value type " + useV.getClass());
				}
				
				if (cs != null) {
					InvokeExpr invExpr = s.getInvokeExpr();
					if (cs.hasAppCallees()) {
//						// constant parameters in app calls
//						int argIdx = 0;
//						for (Object arg : invExpr.getArgs()) {
//							// NOTE: *includes* StringConstant, which here represents ref var for automatically-created string
//							if (arg instanceof Constant) // constant actual param in app call
//								uses.add(new CSConstParamCUse((Constant)arg, cs, argIdx, n));
//							++argIdx;
//						}
						// *** IMPORTANT ***
						// Const args are now handled above, at iteration on use boxes, to ensure args keep their original order in local uses list
						
						// returned-value use
						if (!invExpr.getType().equals(VoidType.v())) {
							// we assume that rhs of return can't be a call (i.e., ret values in Jimple are not chained)
							assert !(s instanceof ReturnStmt);
							if (s instanceof AssignStmt) {
								Use uCallRet = new CSReturnedVarCUse(invExpr, cs, n);
								callRetUses.put(n, uCallRet);
							}
							else
								assert s instanceof InvokeStmt; // call that ignores returned value
						}
					}
					if (cs.hasLibCallees()) {
						// special uses occurring *inside* library call
						for (Value valObjUse : ObjDefUseModelManager.getInternalObjUses(invExpr))
							libObjUses.add(new CUse(new ObjVariable(valObjUse, n), n));
					}
				}
				
				// add to CFG all uses in this node, obtaining incremental new ids
				int[] usesIds = new int[uses.size()]; // maps node use id to CFG use id
				int i = 0;
				for (Use u : uses) {
					int useId = idsToUses.size(); // CFG use id
					idsToUses.add(u);
					usesToIds.put(u, useId); // store also in reverse map
					usesIds[i] = useId;
					++i;
				}
				n.setLocalUsesIds(usesIds);
			}
			else
				n.setLocalUsesIds(new int[0]);
		}
		
		// find defs and params
		final boolean isEntryCFG = ProgramFlowGraph.inst().getEntryMethods().contains(method);
		final boolean isInstanceInit = method.getName().equals("<init>");
		for (CFGNode _n : nodes) {
			if (_n instanceof CFGNodeSpecial)
				continue; // ENTRY or EXIT
			
			NodeDefUses n = (NodeDefUses) _n;
			Stmt s = n.getStmt();
			if (!(s instanceof IdentityStmt) || isEntryCFG || allowParmsRetUseDefs ||
					(isInstanceInit && ((Local)((IdentityStmt)s).getLeftOp()).getName().equals("this")))
			{
				List defBoxes = s.getDefBoxes();
				ArrayList<Def> defs = new ArrayList<Def>();
				if (!defBoxes.isEmpty()) {
					assert defBoxes.size() == 1;
					Value defV = ((ValueBox) defBoxes.iterator().next()).getValue();
					if (defV instanceof Local) { // create local var def
						defs.add(new Def(defV, n));
					}
					else if (defV instanceof FieldRef) {
						fieldDefs.add(new Def(defV, n));
					}
					else if (defV instanceof ArrayRef) {
						arrElemDefs.add(new Def(defV, n));
					}
					else
						System.out.println("Unsupported def value type " + defV.getClass());
				}
				
				// add special defs: constant parameters in app calls
				//                   and defined lib objects
				StmtTag sTag = (StmtTag) s.getTag(StmtTag.TAG_NAME);
				CallSite cs = sTag.getCallSite();
				if (cs != null) {
					InvokeExpr invExpr = s.getInvokeExpr();
					if (cs.hasAppCallees()) {
						// constant actual params
						int argIdx = 0;
						for (Object arg : invExpr.getArgs()) {
							// constant actual param in app call
							// NOTE: for raw string "..." param, only represents ref to string that gets created
							if (arg instanceof Constant)
								argConstDefs.add(new ConstCSParamDef((Constant)arg, cs, argIdx, n));
							++argIdx;
						}
					}
					if (cs.hasLibCallees()) {
						// objects (potentially) defined (modified) *inside* lib call
						for (Value valObjDef : ObjDefUseModelManager.getInternalObjDefs(invExpr))
							libObjDefs.add(new Def(new ObjVariable(valObjDef, n), n));
					}
					// Special defs of java.lang.String objects created automatically because of raw-string parameters "..."
					// These strings are created *before* call (need to explicitly define) and *outside* call (not created by lib model)
					for (Object arg : invExpr.getArgs()) {
						// represents actual java.lang.String that gets created, not the ref var that is the parameter itself
						// TODO: def of auto-string obj actually occurs *before* call (model puts it first? what about app call?)
						if (arg instanceof StringConstant)
							libObjDefs.add(new Def(new ObjVariable((StringConstant)arg, n), n));
					}
				}
				// add special defs: constant return values
				if (s instanceof ReturnStmt) {
					List useBoxes = ((ReturnStmt)s).getUseBoxes();
					if (!useBoxes.isEmpty()) {
						assert useBoxes.size() == 1;
						Value vRet = ((ValueBox)useBoxes.iterator().next()).getValue();
						if (vRet instanceof Constant)
							retConstDefs.add(new ConstReturnDef((Constant)vRet, n));
					}
				}
				// add special defs: new array and new array elem
				//   NOTE: by convention new lib object is NOT a definition -- it is defined by <init> def/use model!
				if (s instanceof AssignStmt) {
					Value vRight = ((AssignStmt)s).getRightOp();
					if (vRight instanceof NewArrayExpr) {
						// define array object
						libObjDefs.add(new Def(new ObjVariable(vRight, n), n));
						// create definition for ANY elem in this array (and, for now, for all elems of array-elem's type)
						Value vSize = ((NewArrayExpr)vRight).getSize();
						arrElemDefs.add(new Def(new StdVariable(Jimple.v().newArrayRef(((AssignStmt)s).getLeftOp(), vSize)), n));
					}
				}
				
				// register defs, obtaining incremental new id
				int[] defsIds = new int[defs.size()];
				int i = 0;
				for (Def d : defs) {
					int defId = idsToDefs.size();
					idsToDefs.add(d);
					defsIds[i++] = defId;
				}
				n.setLocalDefsIds(defsIds);
			}
			else
				n.setLocalDefsIds(new int[0]);
		}
		// add obj defs for entry method's arguments and static library fields
		if (isEntryCFG) {
			// NOTE: we only support 'String[] args' obj defs for entry method 'main(String[])', for now
			if (method.toString().equals("<"+method.getDeclaringClass()+": void main(java.lang.String[])>")) {
				// def: array itself (local is already defined in code); use local as base of array ref (i.e., array elem)
				NodeDefUses nDUArgArrParam = (NodeDefUses) nodes.get(1); // first id stmt: args := @parameter0: java.lang.String[]
				RefType typeStr = Scene.v().getRefType("java.lang.String");
				libObjDefs.add(new Def(  // need to create array object ourselves (actual array created by JVM before entry to 'main')
						new ObjVariable(Jimple.v().newNewArrayExpr(typeStr, IntConstant.v(1)), nDUArgArrParam), // dim 1 shouldn't matter
						nDUArgArrParam)); // occurs at very first id stmt: args := @parameter0: java.lang.String[]
				// def: elements of 'args' array (i.e., String objects)
				libObjDefs.add(new Def(
						new ObjVariable(Jimple.v().newNewExpr(typeStr), nDUArgArrParam),
						nDUArgArrParam)); // occurs at very first id stmt: args := @parameter0: java.lang.String[]
			}
			
			// def: static field System.out (and obj pointed by field) of type java.io.PrintStream
			SootClass clsSystem = Scene.v().getRefType("java.lang.System").getSootClass();
			FieldRef fldRefOut = Jimple.v().newStaticFieldRef(clsSystem.getFieldByName("out").makeRef());
			fieldDefs.add(new Def(new StdVariable(fldRefOut), ENTRY));
//			SootClass clsPrintStream = Scene.v().getRefType("java.io.PrintStream").getSootClass();
			libObjDefs.add(new Def(new ObjVariable(fldRefOut, ENTRY), ENTRY));
//					new ObjVariable(Jimple.v().newNewExpr(clsPrintStream.getType()), ENTRY), ENTRY));
		}
		
		// map values to uses (in the form of bitsets)
		final int numUses = idsToUses.size();
		int useIdx = 0;
		for (Use use : idsToUses) {
			// get/create bitset for use's value
			BitSet bset = varsToUses.get(use.var);
			if (bset == null) {
				bset = new BitSet(numUses);
				varsToUses.put(use.var, bset);
			}
			bset.set(useIdx); // mark use for this value
			++useIdx;
		}
		final int numDefs = idsToDefs.size();
		int defIdx = 0;
		for (Def def : idsToDefs) {
			// get/create bitset for def's value
			BitSet bset = varsToDefs.get(def.var);
			if (bset == null) {
				bset = new BitSet(numDefs);
				varsToDefs.put(def.var, bset);
			}
			bset.set(defIdx); // mark def for this value
			++defIdx;
			
			// ensure creation of 0-filled value-use bitsets for defined values without uses
			if (varsToUses.get(def.var) == null)
				varsToUses.put(def.var, new BitSet(numUses));
		}
//		assert valuesToUses.size() == valuesToDefs.size(); // all values should be mapped to bitsets
		
		// DEBUG
		System.out.println("  Method defs " + idsToDefs.size() + ", uses " + idsToUses.size() + ", values " + varsToUses.size());
		
		System.out.print("    Defs: ");
		int id = 0;
		for (Def d : idsToDefs)
			System.out.print((id++) + "=" + d + ",");
		System.out.println();
		
		System.out.print("    Uses: ");
		id = 0;
		for (Use u : idsToUses)
			System.out.print((id++) + "=" + u + ",");
		System.out.println();
		
		System.out.print("    Var uses: ");
		for (Variable var : varsToUses.keySet())
			System.out.print(var + "=" + varsToUses.get(var) + ",");
		System.out.println();
		
		System.out.print("    Var defs: ");
		for (Variable var : varsToDefs.keySet())
			System.out.print(var + "=" + varsToDefs.get(var) + ",");
		System.out.println();
	}
	
}
