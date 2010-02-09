package dua.cls;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import soot.Local;
import soot.SootClass;
import soot.Type;
import soot.Value;
import soot.jimple.FieldRef;
import soot.jimple.InstanceFieldRef;
import dua.unit.Location;
import dua.util.Util;

public class FieldDefUse {
	private FieldRef fieldRef;
	private Location loc;
	private Collection<Type> p2SetBase;
	
	public FieldRef getFieldRef() { return fieldRef; }
	public Location getLoc() { return loc; }
	public Collection<Type> getP2SetBase() { return p2SetBase; }
	
	public FieldDefUse(FieldRef fieldRef, Location loc) {
		this.fieldRef = fieldRef;
		this.loc = loc;
		
		if (fieldRef instanceof InstanceFieldRef) {
			InstanceFieldRef ifieldRef = (InstanceFieldRef) fieldRef;
			Value base = ifieldRef.getBase();
			this.p2SetBase = Util.getP2Nodes((Local)base);
			// DEBUG
			if (this.p2SetBase.isEmpty())
				System.out.println("P2Set of base is empty!!! " + ifieldRef);
			else
				System.out.println("P2Set of base is not empty!!! " + ifieldRef);
		}
	}
	@Override
	public String toString() {
		return fieldRef + "@" + loc;
	}
	
	public static HashMap<SootClass, ArrayList<FieldDefUse>> fieldDefs = new HashMap<SootClass, ArrayList<FieldDefUse>>();
	public static HashMap<SootClass, ArrayList<FieldDefUse>> fieldUses = new HashMap<SootClass, ArrayList<FieldDefUse>>();
}
