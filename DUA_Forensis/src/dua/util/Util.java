package dua.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import soot.AnySubType;
import soot.ArrayType;
import soot.Body;
import soot.FastHierarchy;
import soot.Local;
import soot.PatchingChain;
import soot.PointsToSet;
import soot.RefLikeType;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootFieldRef;
import soot.SootMethod;
import soot.SootMethodRef;
import soot.Type;
import soot.Value;
import soot.jimple.ArrayRef;
import soot.jimple.Constant;
import soot.jimple.FieldRef;
import soot.jimple.IdentityStmt;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InterfaceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.NewArrayExpr;
import soot.jimple.NewExpr;
import soot.jimple.SpecialInvokeExpr;
import soot.jimple.StaticFieldRef;
import soot.jimple.StaticInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;
import soot.jimple.VirtualInvokeExpr;
import soot.jimple.internal.JRetStmt;
import soot.jimple.internal.JReturnStmt;
import soot.jimple.internal.JReturnVoidStmt;
import soot.toolkits.graph.Block;
import soot.util.Chain;
import soot.util.NumberedString;
import dua.Options;
import dua.cls.ClassTag;
import dua.global.ProgramFlowGraph;
import dua.method.CFG;
import dua.method.MethodTag;
import dua.method.ReachableUsesDefs;
import dua.unit.StmtTag;

public class Util {
	/** A value represents a var if it's a constant, local, field ref (for field itself), or array elem ref (any element in it).
	 *  Objects are *not* considered here. */
	public static boolean valuesEqual(Value v1, Value v2) {
		// case 1: both null refs or constants
		if (v1 == null || v2 == null)
			return v1 == v2;
		if (v1 instanceof Constant) {
			if (v2 instanceof Constant)
				return v1.equals(v2);
			return false;
		}
		
		// case 2: locals
		if (v1 instanceof Local)
			return v1 == v2;
		// case 3: field refs (base is ignored)
		if (v1 instanceof FieldRef) {
			SootFieldRef sfr1 = ((FieldRef) v1).getFieldRef();
			if (!(v2 instanceof FieldRef))
				return false;
			SootFieldRef sfr2 = ((FieldRef) v2).getFieldRef();
			return sfr1.declaringClass() == sfr2.declaringClass() && sfr1.name().equals(sfr2.name());
		}
		// case 4: array elems (index compared only if constant)
		//         they are the same if elem types are equal and index values are not definitely different
		assert (v1 instanceof ArrayRef);
		
		ArrayRef ae1 = (ArrayRef)v1;
		if (!(v2 instanceof ArrayRef))
			return false;
		ArrayRef ae2 = (ArrayRef)v2;
		if (!ae1.getBase().getType().equals(ae2.getBase().getType()))
			return false;
		// *** FOR NOW, we avoid distinguishing array elems with constant indices as different variables,
		//  because it leads to too much instrumentation at defs and uses of array elem with unknown index.
//		Value vIdx1 = ae1.getIndex();
//		Value vIdx2 = ae2.getIndex();
//		if (vIdx1 instanceof Constant && vIdx2 instanceof Constant && !vIdx1.equals(vIdx2))
//			return false;
		return true;
	}
	
	/** Uses intersection of possible runtime types referenced by values.
	 *  These types of Value can represent an object:
	 *    1. NewExpr/NewArrayExpr (instance)
	 *    2. InvokeExpr           (static or instance)
	 *    3. Local/statfield ref var  (instance)
	 *    4. StringConstant       (instance)
	 */
	public static boolean objValuesMayEqual(Value v1, Value v2) {
		// case 5: (lib) object, either static or instance
		//         objects are (possibly) the same if they are both static or both instance,
		//         and there is at least one object type that v1 and v2 can refer to in their call.
		// NOTE: includes automatically constructed Strings, represented by StringConstant values
		
		// first, handle special case with more precision: both values are string constants
		if (v1 instanceof StringConstant && v2 instanceof StringConstant)
			return ((StringConstant)v1).value.equals(((StringConstant)v2).value);
		
		// get list of ref types of instances to which each value can refer
		// (null if val refers to class itself, not instances)
		Set<RefLikeType> clsTargets1 = getAllPossibleRuntimeRefTypes(v1);
		Set<RefLikeType> clsTargets2 = getAllPossibleRuntimeRefTypes(v2);
		
		// first, handle special case of values referring to class object (static)
		if (clsTargets1 == null) {
			if (clsTargets2 != null)
				return false;
			SootClass cls1 = ((StaticInvokeExpr)v1).getMethod().getDeclaringClass();
			SootClass cls2 = ((StaticInvokeExpr)v2).getMethod().getDeclaringClass();
			return cls1.equals(cls2);
		}
		else if (clsTargets2 == null)
			return false;
		
		// now, just check for intersection of returned lists of possible target classes
		return clsTargets1.removeAll(clsTargets2);
//		// *** Strengthened to be completely equal, respecting TRANSITIVITY of equality
//		return clsTargets1.equals(clsTargets2);
	}
	/** Uses EXACT of possible runtime types referenced by values.
	 *  These types of Value can represent an object:
	 *    1. NewExpr/NewArrayExpr (instance)
	 *    2. InvokeExpr           (static or instance)
	 *    3. Local/statfield ref var  (instance)
	 *    4. StringConstant       (instance)
	 */
	public static boolean objValuesMustEqual(Value v1, Value v2) {
		// case 5: (lib) object, either static or instance
		//         objects are (possibly) the same if they are both static or both instance,
		//         and there is at least one object type that v1 and v2 can refer to in their call.
		// NOTE: includes automatically constructed Strings, represented by StringConstant values
		
		// first, handle special case with more precision: both values are string constants
		if (v1 instanceof StringConstant && v2 instanceof StringConstant)
			return ((StringConstant)v1).value.equals(((StringConstant)v2).value);
		
		// get list of ref types of instances to which each value can refer
		// (null if val refers to class itself, not instances)
		Set<RefLikeType> clsTargets1 = getAllPossibleRuntimeRefTypes(v1);
		Set<RefLikeType> clsTargets2 = getAllPossibleRuntimeRefTypes(v2);
		
		// first, handle special case of values referring to class object (static)
		if (clsTargets1 == null) {
			if (clsTargets2 != null)
				return false;
			SootClass cls1 = ((StaticInvokeExpr)v1).getMethod().getDeclaringClass();
			SootClass cls2 = ((StaticInvokeExpr)v2).getMethod().getDeclaringClass();
			return cls1.equals(cls2);
		}
		else if (clsTargets2 == null)
			return false;
		
//		// now, just check for intersection of returned lists of possible target classes
//		return clsTargets1.removeAll(clsTargets2);
		// *** Strengthened to be completely equal, respecting TRANSITIVITY of equality
		return clsTargets1.equals(clsTargets2);
	}
	/** Returns all possible ref/array types of instances that Value can represent as variables.
	 *  Returns null if no instance obj can be represented by Value (e.g., static method call). */
	private static Set<RefLikeType> getAllPossibleRuntimeRefTypes(Value val) {
		Set<RefLikeType> typeTargets = new HashSet<RefLikeType>();
		// 1.a) NewExpr        (instance)
		if (val instanceof NewExpr)
			typeTargets.add((RefLikeType)((NewExpr)val).getType());
		// 1.b) NewArrayExpr   (instance)
		else if (val instanceof NewArrayExpr)
			typeTargets.add((RefLikeType)((NewArrayExpr)val).getType());
		// 2. InvokeExpr     (static or instance)
		else if (val instanceof InvokeExpr) {
			if (val instanceof StaticInvokeExpr)
				typeTargets = null; // special case
			else if (val instanceof SpecialInvokeExpr)
				typeTargets.add((RefType)((SpecialInvokeExpr)val).getBase().getType());
			else {
				assert val instanceof InstanceInvokeExpr;
				SootClass declCls = ((InvokeExpr)val).getMethod().getDeclaringClass();
				typeTargets.add(declCls.getType());
				
				for (SootClass clsSub : getAllSubtypes(declCls))
					typeTargets.add(clsSub.getType());
			}
		}
		// 3. Local/statfield ref var  (instance)
		else if (val instanceof Local || val instanceof StaticFieldRef) {
			RefLikeType lType = (RefLikeType) val.getType();
			typeTargets.add(lType);
			if (lType instanceof RefType) {
				// add all possible subtypes of ref type
				for (SootClass clsSub : getAllSubtypes(((RefType)lType).getSootClass()))
					typeTargets.add(clsSub.getType());
			}
			else
				assert lType instanceof ArrayType; // array type has no subtypes
		}
		// 4. StringConstant (instance)
		else {
			assert (val instanceof StringConstant);
			typeTargets.add(Scene.v().getRefType("java.lang.String"));
		}
		return typeTargets;
	}
	
	/** Hash code for Value representing a variable if it's a constant, local, field ref (for field itself), or array elem ref
	 *  (for all elements of array of that type). */
	public static int valueHashCode(Value v) {
		// case 1: null or constant
		if (v == null)
			return 0;
		if (v instanceof Constant)
			return v.hashCode();
		
		// case 2: local
		if (v instanceof Local)
			return v.hashCode();
		// case 3: field ref (base is ignored)
		if (v instanceof FieldRef) {
			SootFieldRef sfr = ((FieldRef) v).getFieldRef();
			return sfr.resolve().hashCode();
		}
		// case 4: array elems
		assert (v instanceof ArrayRef);
		ArrayRef ae1 = (ArrayRef)v;
		// *** FOR NOW, we avoid distinguishing array elems with constant indices as different variables,
		//  because it leads to too much instrumentation at defs and uses of array elem with unknown index.
		return ae1.getBase().getType().hashCode();// + ((ae1.getIndex() instanceof Constant)? ae1.getIndex().hashCode() : 0);
	}
	/** Hash code for Value representing an object (static or instance) */
	public static int objValueHashCode(Value v) {
		// case 5: (lib) object, either static or instance
		if (v instanceof StaticInvokeExpr)
			return ((StaticInvokeExpr)v).getMethod().getDeclaringClass().hashCode();
		else {
			// no good solution that guarantees that equal (may-point-to-same-instance) refs will get same hash code,
			// so just use constant value
			return 1;
		}
	}
	
	public static boolean isValueInCollection(Value v, Collection<Value> c) {
		for (Value v2 : c)
			if (valuesEqual(v, v2))
				return true;
		return false;
	}
	
	/** Returns true if value is "statically-definite", that is, it can represent only
	 *  one memory location at a time during an execution. */
	public static boolean isValueDefinite(Value v) {
		// TODO: determine if local is in a recursive method (in which case, it is NOT definite)
		// case 1 or 2: constant or local
		if (v instanceof Constant || v instanceof Local)
			return true;
		
		// case 3: field is definite only if it's static
		if (v instanceof FieldRef)
			return ((FieldRef) v).getFieldRef().isStatic();
		
		// case 4: array elems are all treated, for now, as not definite (conservative assumption)
		// case 5: only class objects (due to static invoke expr) are definite
		assert v instanceof ArrayRef || v instanceof InvokeExpr;
		return v instanceof StaticInvokeExpr;
	}
	
	public static boolean isReturnStmt(Stmt s) {
		return s instanceof JRetStmt || s instanceof JReturnStmt || s instanceof JReturnVoidStmt;
	}
	public static boolean isCtorCall(Stmt s) {
		return s.containsInvokeExpr() &&
			(s.getInvokeExpr() instanceof SpecialInvokeExpr) && s.getInvokeExpr().getMethod().getName().equals("<init>");
	}
	
	public static Collection<Type> getP2Nodes(Value v) {
		PointsToSet p2sTypes;
		if (v instanceof Local)
			p2sTypes = Scene.v().getPointsToAnalysis().reachingObjects((Local)v);
		else
			p2sTypes = Scene.v().getPointsToAnalysis().reachingObjects(((FieldRef)v).getField());
		
		return (Collection<Type>) p2sTypes.possibleTypes();
	}
	
	/** 
	 * Finds in class hierarchy and returns all app and lib concrete methods possibly referenced by method ref.
	 * Method is assumed to be virtual (not special or static).
	 * Returns true if there are library methods among targets found.
	 */
	public static boolean getConcreteCallTargets(InvokeExpr instInvExpr, /*OUT*/ Set<SootMethod> appTargets, /*OUT*/ Set<SootMethod> libTargets) {
		// get class of method ref; we start searching from this class
		SootMethodRef mref = instInvExpr.getMethodRef();
		SootClass cls = mref.declaringClass(); // starting class
		final NumberedString subsignature = mref.getSubSignature(); // signature to search for
		
		// CASE 1: object is of declared class type or inherited from some superclass
		//         find first superclass, starting from current cls, that declares method; there HAS to be such a class
		// note that if cls is interface, superclass if java.lang.Object
		// note that we don't check if there is indeed an interface declaring the method; we assume this is the case if no superclass declares it
		while (!cls.declaresMethod(subsignature) && cls.hasSuperclass())
			cls = cls.getSuperclass(); // never an interface
		// now, method might not be in superclass, or might be abstract; in that case, it's not a target
		SootMethod m;
		if (cls.declaresMethod(subsignature)) {
			m = cls.getMethod(subsignature);
			if (!m.isAbstract()) {
				if (cls.hasTag(ClassTag.TAG_NAME))
					appTargets.add(m); // add app method
				else
					libTargets.add(m); // add lib method
			}
		}
		
		// (only for virtual/interface calls)
		// CASE 2: object's actual type is a subclass; any subclass declaring the method is a possible target
		//         we have to check all superclasses of implementers, because starting cls might be interface
		if (instInvExpr instanceof VirtualInvokeExpr || instInvExpr instanceof InterfaceInvokeExpr) {
			cls = mref.declaringClass(); // start again from declaring class
			List<SootClass> allSubclasses = getAllSubtypes(cls);
			for (SootClass subCls : allSubclasses) {
				m = getMethodInClassOrSuperclass(subCls, subsignature);
				if (m != null && !m.isAbstract()) {
					if (m.getDeclaringClass().hasTag(ClassTag.TAG_NAME))
						appTargets.add(m); // add app method
					else
						libTargets.add(m); // add lib method
				}
			}
		}
		
		return !libTargets.isEmpty();
	}
	
	/** Returns the transitive closure of subinterfaces and subclasses of given class or interface (excluding given class). */
	public static List<SootClass> getAllSubtypes(SootClass cls) {
		// TODO store (cache) all subclasses in Class Tag
		List<SootClass> subclasses = new ArrayList<SootClass>();
		FastHierarchy hierarchy = Scene.v().getOrMakeFastHierarchy();
		for (Iterator itSubCls = hierarchy.getSubclassesOf(cls).iterator(); itSubCls.hasNext(); ) {
			SootClass subCls = (SootClass) itSubCls.next();
			subclasses.add(subCls);
			subclasses.addAll(getAllSubtypes(subCls));
		}
		for (Iterator itSubCls = hierarchy.getAllImplementersOfInterface(cls).iterator(); itSubCls.hasNext(); ) {
			SootClass subCls = (SootClass) itSubCls.next();
			subclasses.add(subCls);
			subclasses.addAll(getAllSubtypes(subCls));
		}
		return subclasses;
	}
	/** Returns all transitive subinterfaces and subclasses of given class/interface, including that class/interface. */
	public static List<SootClass> getTypeAndSubtypes(SootClass cls) {
		List<SootClass> allClasses = getAllSubtypes(cls);
		allClasses.add(cls);
		return allClasses;
	}
	
	/** Returns method in given class or first upwards superclass,
	 *  or null if not found in any class (no interface checked) */
	private static SootMethod getMethodInClassOrSuperclass(SootClass cls, NumberedString subsignature) {
		if (cls.declaresMethod(subsignature))
			return cls.getMethod(subsignature);
		if (cls.hasSuperclass())
			return getMethodInClassOrSuperclass(cls.getSuperclass(), subsignature);
		return null;
	}
	
	/** For instance invokes */
	public static ArrayList<SootMethod> resolveAppCall(Type tgtType, SootMethodRef methodRef) {
		final NumberedString mSubsignature = methodRef.getSubSignature();
		if (tgtType instanceof RefType) {
			// find first class upwards in hierarchy, starting from cls, that implements method (i.e., *concrete* method)
			SootClass cls = ((RefType) tgtType).getSootClass();
			while (!cls.declaresMethod(mSubsignature))
				cls = cls.getSuperclass(); // if method not in this class, it HAS to be in a superclass, so a superclass must exist
			
			if (!cls.hasTag(ClassTag.TAG_NAME))
				return null; // not an app method
			
			// finally, store resolved app method
			SootMethod m = cls.getMethod(mSubsignature);
			assert m.hasTag(MethodTag.TAG_NAME);
			
			ArrayList<SootMethod> methods = new ArrayList<SootMethod>();
			methods.add(m); // just one element, directly resolved
			return methods;
		}
		
		if (tgtType instanceof AnySubType) {
			// return set of all app subtypes that implement referenced method
			SootClass baseCls = ((AnySubType)tgtType).getBase().getSootClass();
			List subClasses = baseCls.isInterface()?
					Scene.v().getActiveHierarchy().getImplementersOf(baseCls) :
					Scene.v().getActiveHierarchy().getSubclassesOf(baseCls);
			ArrayList<SootMethod> methods = new ArrayList<SootMethod>();
			for (Object oSubCls : subClasses) {
				SootClass subCls = (SootClass) oSubCls;
				if (subCls.hasTag(ClassTag.TAG_NAME)) {
					try {
						SootMethod m = subCls.getMethod(mSubsignature);
						assert m.hasTag(MethodTag.TAG_NAME);
						if (!m.isAbstract())
							methods.add(m);
					}
					catch (RuntimeException e) {}
				}
			}
			
			return methods;
		}
		
		assert tgtType instanceof ArrayType; // only other case observed so far
		return new ArrayList(); // no array class/method is in app
	}
	
	/** Returns basic block that contains the statement, or null. If id stmt, it looks in tag of first non-id stmt. */
	public static Block getBB(Stmt s) {
		Block bb;
		//2010-02-26: return null when s == null
		if(s!= null){			
			// move to first non-id stmt
			Stmt sNonId = s;
			if (sNonId instanceof IdentityStmt) {
				PatchingChain pchain = ProgramFlowGraph.inst().getContainingMethod(sNonId).retrieveActiveBody().getUnits();
				do {
					sNonId = (Stmt) pchain.getSuccOf(sNonId);
				} while (sNonId instanceof IdentityStmt);
			}
			
			// retrieve basic block for non-id stmt	
			StmtTag sTag = (StmtTag) sNonId.getTag(StmtTag.TAG_NAME);
			bb = sTag.getBasicBlock();
		}else{
			bb = null;
		}
		
		
		
		
		
		return bb;
	}
	
	/** Helper to convert map value type */
	public static Map<SootMethod, ReachableUsesDefs> convertToRUMap(Map<SootMethod, CFG> mToCFGs) {
		Map<SootMethod, ReachableUsesDefs> mToRUs = new HashMap<SootMethod, ReachableUsesDefs>();
		
		for (SootMethod m : mToCFGs.keySet())
			mToRUs.put(m, (ReachableUsesDefs) mToCFGs.get(m));
		
		return mToRUs;
	}
	
	/** Returns base path, ending in '\', creating path if it didn't exist */
	public static String getCreateBaseOutPath() {
		String baseOutPath = Options.getOutPath();
		final char sepChar = File.separatorChar;
		if (!baseOutPath.isEmpty() && baseOutPath.charAt(baseOutPath.length()-1) != sepChar)
			baseOutPath += sepChar;
		(new File(baseOutPath)).mkdir(); // ensure directory is created
		
		return baseOutPath;
	}
	
	/** Parses a comma-separated list of strings into a list of strings. Trims leading/trailing whitespace. */
	public static List<String> parseStringList(String s) {
		List<String> strList = new ArrayList<String>();
		
		if (s.isEmpty())
			return strList;
		
		int i = 0;
		while (true) {
			final int end = s.indexOf(',', i);
			if (end == -1) { // last string in list
				strList.add(s.substring(i).trim());
				break;
			}
			strList.add(s.substring(i, end).trim());
			i = end + 1; // past comma
		}
		
		return strList;
	}
	
	/** Parses a comma-separated list of integers into a list of integers. List might optionally by surrounded by square brackets. */
	public static List<Integer> parseIntList(String s) {
		// remove brackets, if present
		if (s.charAt(0) == '[') {
			assert s.charAt(s.length()-1) == ']';
			s = s.substring(1, s.length()-1);
		}
		
		List<String> strList = parseStringList(s);
		
		List<Integer> intList = new ArrayList<Integer>();
		for (String sInt : strList)
			intList.add(Integer.valueOf(sInt));
		
		return intList;
	}
	
	/** Returns index of local in body, or negative value if not found. */
	public static int getIndexOfLocal(Local l, Body b) {
		int i = 0;
		Chain locals = b.getLocals();
		for (Iterator itLoc = locals.iterator(); itLoc.hasNext(); ) {
			if (l == (Local)itLoc.next())
				return i;
			++i;
		}
		return Integer.MIN_VALUE;
	}
	
}
