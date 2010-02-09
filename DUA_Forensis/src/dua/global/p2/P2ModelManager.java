package dua.global.p2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import soot.ArrayType;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.Value;
import soot.jimple.InstanceFieldRef;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.Jimple;
import dua.util.Pair;
import dua.util.Util;

public class P2ModelManager {
	public static abstract class AbstractP2Model {
		protected static List<Pair<Value,Value>> emptyList = new ArrayList<Pair<Value,Value>>();
		public List<Pair<Value,Value>> getTransfers(InvokeExpr invExp) { return emptyList; }
		public List<Pair<Value,Value>> getSeeds(InvokeExpr invExp) { return emptyList; }
	}
	public static class EmptyModel extends AbstractP2Model {
		private EmptyModel() {}
		public static final AbstractP2Model inst = new EmptyModel();
		@Override public List<Pair<Value,Value>> getSeeds(InvokeExpr invExp) {
			// if there is a return value and it's a ref type, "create" new object of that type as default return behavior
			List<Pair<Value,Value>> seeds = new ArrayList<Pair<Value,Value>>();
			if (invExp.getType() instanceof RefType) {
				Value valRhs = Jimple.v().newNewExpr((RefType)invExp.getType());
				seeds.add(new Pair<Value, Value>(null, valRhs));
			}
			else if (invExp.getType() instanceof ArrayType) {
				Value valRhs = Jimple.v().newNewArrayExpr(((ArrayType)invExp.getType()).getElementType(), IntConstant.v(0));
				seeds.add(new Pair<Value, Value>(null, valRhs));
			}
			
			return seeds;
		}
	}
	public static class TransferThisModel extends AbstractP2Model {
		private TransferThisModel() {}
		public static final AbstractP2Model inst = new TransferThisModel();
		@Override final public List<Pair<Value,Value>> getTransfers(InvokeExpr invExp) {
			List<Pair<Value,Value>> transfList = new ArrayList<Pair<Value,Value>>();
			transfList.add(new Pair<Value, Value>(null, ((InstanceInvokeExpr)invExp).getBase()));
			return transfList;
		}
	}
	public static class SeedThisModel extends AbstractP2Model {
		private final Value valRhs;
		public SeedThisModel(String refTypeSig) { valRhs = Jimple.v().newNewExpr(Scene.v().getRefType(refTypeSig)); }
		@Override final public List<Pair<Value,Value>> getSeeds(InvokeExpr invExp) {
			List<Pair<Value,Value>> seedList = new ArrayList<Pair<Value,Value>>();
			seedList.add(new Pair<Value, Value>(null, valRhs));
			return seedList;
		}
	}
	public static class TransferAndSeedThisModel extends AbstractP2Model {
		private final Value valRhs;
		public TransferAndSeedThisModel(String refTypeSig) { valRhs = Jimple.v().newNewExpr(Scene.v().getRefType(refTypeSig)); }
		@Override final public List<Pair<Value,Value>> getTransfers(InvokeExpr invExp) {
			List<Pair<Value,Value>> transfList = new ArrayList<Pair<Value,Value>>();
			transfList.add(new Pair<Value, Value>(null, ((InstanceInvokeExpr)invExp).getBase()));
			return transfList;
		}
		@Override final public List<Pair<Value,Value>> getSeeds(InvokeExpr invExp) {
			List<Pair<Value,Value>> seedList = new ArrayList<Pair<Value,Value>>();
			seedList.add(new Pair<Value, Value>(null, valRhs));
			return seedList;
		}
	}
	
	// CONTAINER MODELS
	public static abstract class AbstractContainerModel extends AbstractP2Model {
		private static final String DUMMY_P2_CLS_NAME = "DummyP2Container";
		protected static SootClass clsDummyP2 = null; // dummy class that will contain special fields for this type of model
		protected final SootClass clsContainerRoot; // top class of hierarchy of all possible classes referred by ref to top class
		// special fields for root class and all subclasses, to represent elements of container
		protected Map<String, List<SootField>> ptrNameToFieldsForHierarchy = new Hashtable<String, List<SootField>>();
		protected AbstractContainerModel(String nameContainerCls) { this.clsContainerRoot = Scene.v().getSootClass(nameContainerCls); }
		/** Get/creates list of fields in dummy class, one for given class and one for each subclass of it. */
		protected List<SootField> getCreatePtrFields(String fldNamePrefix) {
			List<SootField> fieldsForHierarchy = ptrNameToFieldsForHierarchy.get(fldNamePrefix);
			if (fieldsForHierarchy == null) {
				fieldsForHierarchy = new ArrayList<SootField>();
				ptrNameToFieldsForHierarchy.put(fldNamePrefix, fieldsForHierarchy);
				// get create dummy class that will contain special fields for this type of model
				if (clsDummyP2 == null)
					clsDummyP2 = new SootClass(DUMMY_P2_CLS_NAME);
				// get/create fields list
				for (SootClass clsInHierarchy : Util.getTypeAndSubtypes(clsContainerRoot)) {
					final String elemFldName = fldNamePrefix + clsInHierarchy;
					SootField fldElem;
					if (clsDummyP2.declaresFieldByName(elemFldName))
						fldElem = clsDummyP2.getField(elemFldName, Scene.v().getSootClass("java.lang.Object").getType());
					else {
						fldElem = new SootField(elemFldName, Scene.v().getSootClass("java.lang.Object").getType());
						clsDummyP2.addField(fldElem);
					}
					fieldsForHierarchy.add(fldElem);
				}
				// DEBUG
				if (fieldsForHierarchy.size() > 10)
					System.out.println("P2 Warning: container model - number fields for " + clsContainerRoot + " is " + fieldsForHierarchy.size());
			}
			return fieldsForHierarchy;
		}
	}
	private static final String ELEM_FLD_NAME = "__elem_p2__";
	/** Models a transfer from arg0 rhs to baseContainer.fakeField lhs. */
	public static class ContainerPutModel extends AbstractContainerModel {
		public ContainerPutModel(String nameContainerCls) { super(nameContainerCls); }
		@Override final public List<Pair<Value,Value>> getTransfers(InvokeExpr invExp) {
			List<Pair<Value,Value>> transfList = new ArrayList<Pair<Value,Value>>();
			Value vBase = ((InstanceInvokeExpr)invExp).getBase();
			Value vArg0 = ((InstanceInvokeExpr)invExp).getArg(0);
			// for lhs, create instance field where base is the provided container ref
			for (SootField elemFld : getCreatePtrFields(ELEM_FLD_NAME)) {
				InstanceFieldRef elemFldRef = Jimple.v().newInstanceFieldRef(vBase, elemFld.makeRef());
				transfList.add(new Pair<Value, Value>(elemFldRef, vArg0));
			}
			return transfList;
		}
	}
	/** Models a transfer from baseContainer.fakeField rhs to lhs (not provided, i.e., null). */
	public static class ContainerGetModel extends AbstractContainerModel {
		public ContainerGetModel(String nameContainerCls) { super(nameContainerCls); }
		@Override final public List<Pair<Value,Value>> getTransfers(InvokeExpr invExp) {
			List<Pair<Value,Value>> transfList = new ArrayList<Pair<Value,Value>>();
			Value vBase = ((InstanceInvokeExpr)invExp).getBase();
			// for rhs, create instance field where base is the provided container ref
			for (SootField elemFld : getCreatePtrFields(ELEM_FLD_NAME)) {
				InstanceFieldRef elemFldRef = Jimple.v().newInstanceFieldRef(vBase, elemFld.makeRef());
				transfList.add(new Pair<Value, Value>(null, elemFldRef));
			}
			return transfList;
		}
	}
	
	// MAP MODELS
	protected static final String KEY_FLD_NAME = "__key_p2__";
	protected static final String VAL_FLD_NAME = "__val_p2__";
	/** Models two transfers: (1) arg0 rhs -> baseMap.fakeKeyField lhs, and (2) arg1 rhs -> baseMap.fakeValField lhs. */
	public static class MapPutModel extends AbstractContainerModel {
		public MapPutModel(String nameMapCls) { super(nameMapCls); }
		@Override final public List<Pair<Value,Value>> getTransfers(InvokeExpr invExp) {
			List<Pair<Value,Value>> transfList = new ArrayList<Pair<Value,Value>>();
			Value vBase = ((InstanceInvokeExpr)invExp).getBase();
			Value vArg0 = ((InstanceInvokeExpr)invExp).getArg(0);
			Value vArg1 = ((InstanceInvokeExpr)invExp).getArg(1);
			// for key and val lhs, resp., create instance fields where base is the provided map ref
			for (SootField ptrFld : getCreatePtrFields(KEY_FLD_NAME)) {
				InstanceFieldRef keyFldRef = Jimple.v().newInstanceFieldRef(vBase, ptrFld.makeRef());
				transfList.add(new Pair<Value, Value>(keyFldRef, vArg0));
			}
			for (SootField ptrFld : getCreatePtrFields(VAL_FLD_NAME)) {
				InstanceFieldRef valFldRef = Jimple.v().newInstanceFieldRef(vBase, ptrFld.makeRef());
				transfList.add(new Pair<Value, Value>(valFldRef, vArg1));
			}
			// also, remember that old value for key is returned and assigned to (unknown) lhs
			for (SootField ptrFld : getCreatePtrFields(VAL_FLD_NAME)) {
				InstanceFieldRef valFldRef = Jimple.v().newInstanceFieldRef(vBase, ptrFld.makeRef());
				transfList.add(new Pair<Value, Value>(null, valFldRef));
			}
			return transfList;
		}
	}
	/** Models a transfer from baseMap.valField rhs to lhs (not provided, i.e., null). */
	public static class MapGetValModel extends AbstractContainerModel {
		public MapGetValModel(String nameMapCls) { super(nameMapCls); }
		@Override final public List<Pair<Value,Value>> getTransfers(InvokeExpr invExp) {
			List<Pair<Value,Value>> transfList = new ArrayList<Pair<Value,Value>>();
			Value vBase = ((InstanceInvokeExpr)invExp).getBase();
			// for rhs, create instance field where base is the provided map ref
			for (SootField ptrFld : getCreatePtrFields(VAL_FLD_NAME)) {
				InstanceFieldRef valFldRef = Jimple.v().newInstanceFieldRef(vBase, ptrFld.makeRef());
				transfList.add(new Pair<Value, Value>(null, valFldRef));
			}
			return transfList;
		}
	}
	/** Models a transfer from enum's associated map's keyField rhs to lhs (not provided, i.e., null). */
	public static class EnumOfMapKeysGetModel extends AbstractContainerModel {
		/** IMPORTANT: use name of underlying map class over which this enum iterates. */
		public EnumOfMapKeysGetModel(String nameAssocMapCls) { super(nameAssocMapCls); }
		@Override final public List<Pair<Value,Value>> getTransfers(InvokeExpr invExp) {
			List<Pair<Value,Value>> transfList = new ArrayList<Pair<Value,Value>>();
			Value vBase = ((InstanceInvokeExpr)invExp).getBase();
			// for rhs, create instance field where base is the provided map ref
			for (SootField ptrFld : getCreatePtrFields(KEY_FLD_NAME)) {
				InstanceFieldRef keyFldRef = Jimple.v().newInstanceFieldRef(vBase, ptrFld.makeRef());
				transfList.add(new Pair<Value, Value>(null, keyFldRef));
			}
			return transfList;
		}
	}
	
	/** Maps method signature to corresponding def/use model. */
	private static Map<String, AbstractP2Model> sigToModel = null; // will be constructed when first accessed
	private static Map<String, AbstractP2Model> getCreateSigToModelMap() { if (sigToModel == null) { buildSigToModelMap(); } return sigToModel; }
	
	private static void buildSigToModelMap() {
		assert sigToModel == null;
		sigToModel = new HashMap<String, AbstractP2Model>();
		
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
		sigToModel.put("<java.io.PrintStream: void println(int)>", EmptyModel.inst);
		sigToModel.put("<java.io.PrintStream: void println(java.lang.String)>", EmptyModel.inst);
//		sigToModel.put("<java.io.PushbackReader: void <init>(java.io.Reader)>", EmptyModel.inst);
//		sigToModel.put("<java.io.PushbackReader: void <init>(java.io.Reader,int)>", EmptyModel.inst);
		sigToModel.put("<java.io.PushbackReader: void close()>", EmptyModel.inst);
		sigToModel.put("<java.io.PushbackReader: int read()>", EmptyModel.inst);
		sigToModel.put("<java.io.PushbackReader: void unread(int)>", EmptyModel.inst);
		sigToModel.put("<java.lang.Integer: int parseInt(java.lang.String)>", EmptyModel.inst);
		sigToModel.put("<java.lang.Integer: java.lang.String toHexString(int)>",
				new SeedThisModel("java.lang.String"));
		sigToModel.put("<java.lang.Integer: java.lang.String toString(int)>",
				new SeedThisModel("java.lang.String"));
		sigToModel.put("<java.lang.Object: void <init>()>", EmptyModel.inst);
		sigToModel.put("<java.lang.Runtime: void exit(int)>", EmptyModel.inst);
		sigToModel.put("<java.lang.Runtime: java.lang.Runtime getRuntime()>", EmptyModel.inst);
		sigToModel.put("<java.lang.String: char charAt(int)>", EmptyModel.inst);
		sigToModel.put("<java.lang.String: java.lang.String concat(java.lang.String)>",
				new TransferAndSeedThisModel("java.lang.String")); // may copy this if arg is ""
		sigToModel.put("<java.lang.StringBuffer: java.lang.StringBuffer append(java.lang.Object)>", TransferThisModel.inst);
		sigToModel.put("<java.lang.StringBuffer: java.lang.StringBuffer append(java.lang.String)>", TransferThisModel.inst); 
		sigToModel.put("<java.lang.StringBuffer: java.lang.StringBuffer append(boolean)>", TransferThisModel.inst);
		sigToModel.put("<java.lang.StringBuffer: java.lang.StringBuffer append(char)>", TransferThisModel.inst);
		sigToModel.put("<java.lang.StringBuffer: java.lang.StringBuffer append(char[])>", TransferThisModel.inst);
		sigToModel.put("<java.lang.StringBuffer: java.lang.StringBuffer append(char[],int,int)>", TransferThisModel.inst);
		sigToModel.put("<java.lang.StringBuffer: java.lang.StringBuffer append(double)>", TransferThisModel.inst);
		sigToModel.put("<java.lang.StringBuffer: java.lang.StringBuffer append(float)>", TransferThisModel.inst);
		sigToModel.put("<java.lang.StringBuffer: java.lang.StringBuffer append(int)>", TransferThisModel.inst);
		sigToModel.put("<java.lang.StringBuffer: java.lang.StringBuffer append(long)>", TransferThisModel.inst);
		sigToModel.put("<java.lang.StringBuffer: java.lang.String toString()>",
				new SeedThisModel("java.lang.String"));
		sigToModel.put("<java.lang.System: void exit(int)>", EmptyModel.inst);
		
		sigToModel.put("<java.util.Enumeration: java.lang.Object nextElement()>", new EnumOfMapKeysGetModel("java.util.Hashtable"));
		sigToModel.put("<java.util.Enumeration: boolean hasMoreElements()>", EmptyModel.inst);
		sigToModel.put("<java.util.Hashtable: java.lang.Object get(java.lang.Object)>", new MapGetValModel("java.util.Hashtable"));
		sigToModel.put("<java.util.Hashtable: java.lang.Object put(java.lang.Object,java.lang.Object)>", new MapPutModel("java.util.Hashtable"));
		sigToModel.put("<java.util.Stack: boolean empty()>", EmptyModel.inst);
		sigToModel.put("<java.util.Stack: java.lang.Object peek()>", new ContainerGetModel("java.util.Stack"));
		sigToModel.put("<java.util.Stack: java.lang.Object pop()>", new ContainerGetModel("java.util.Stack"));
		sigToModel.put("<java.util.Stack: java.lang.Object push(java.lang.Object)>", new ContainerPutModel("java.util.Stack"));
		sigToModel.put("<java.util.Vector: boolean add(java.lang.Object)>", new ContainerPutModel("java.util.Vector"));
		sigToModel.put("<java.util.Vector: java.lang.Object get(int)>", new ContainerGetModel("java.util.Vector"));
		sigToModel.put("<java.util.Vector: java.lang.Object lastElement()>", new ContainerGetModel("java.util.Vector"));
		sigToModel.put("<java.util.Vector: java.lang.Object remove(int)>", new ContainerGetModel("java.util.Vector"));
		sigToModel.put("<java.util.Vector: void addElement(java.lang.Object)>", new ContainerPutModel("java.util.Vector"));
	}
	
	public static List<Pair<Value,Value>> getTransfers(InvokeExpr invExp) {
		final String mtdSig = invExp.getMethod().toString();
		AbstractP2Model p2Model = getCreateSigToModelMap().get(mtdSig);
		if (p2Model == null) {
			System.out.println("Warning: p2 transfer model not found for " + mtdSig);
			return EmptyModel.inst.getTransfers(invExp);
		}
		return p2Model.getTransfers(invExp);
	}
	public static List<Pair<Value,Value>> getSeeds(InvokeExpr invExp) {
		final String mtdSig = invExp.getMethod().toString();
		AbstractP2Model p2Model = getCreateSigToModelMap().get(mtdSig);
		if (p2Model == null) {
			System.out.println("Warning: p2 seed model not found for " + mtdSig);
			return EmptyModel.inst.getSeeds(invExp);
		}
		return p2Model.getSeeds(invExp);
	}
	
}
