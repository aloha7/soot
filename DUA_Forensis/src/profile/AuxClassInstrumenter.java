package profile;

import soot.Modifier;
import soot.Scene;
import soot.SootClass;

/** Adds a new app class for auxiliary tasks, such as containing runtime profiling data. */
public class AuxClassInstrumenter {
	private static SootClass clsAux = null;
	
	public static SootClass getCreateAuxAppClass() {
		if (clsAux == null) {
			clsAux = new SootClass("AuxInstr", Modifier.PUBLIC);
			clsAux.setSuperclass(Scene.v().getSootClass("java.lang.Object"));
			Scene.v().addClass(clsAux);
			clsAux.setApplicationClass();
		}
		
		return clsAux;
	}
	
}
