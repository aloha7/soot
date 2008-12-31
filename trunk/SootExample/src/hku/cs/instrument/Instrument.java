package hku.cs.instrument;

import soot.PackManager;
import soot.Scene;
import soot.SootClass;
import soot.Transform;
import soot.options.Options;

public class Instrument {

	public static void main(String[] args) {
		Options.v().set_keep_line_number(true);
		PackManager.v().getPack("jap").add(
				new Transform("jap.instrumenter", StmtInstrument.v()));
		Options.v().setPhaseOption("gb.a1", "enabled:false");
		Options.v().setPhaseOption("gb.cf", "enabled:false");
		Options.v().setPhaseOption("gb.a2", "enabled:false");
		Options.v().setPhaseOption("gb.ule", "enabled:false");
		Options.v().setPhaseOption("gop", "enabled:false");
		Options.v().setPhaseOption("bb.lso", "enabled:false");
		Options.v().setPhaseOption("bb.pho", "enabled:false");
		Options.v().setPhaseOption("bb.ule", "enabled:false");
		Options.v().setPhaseOption("bb.lp", "enabled:false");
		Scene.v().addBasicClass("hku.cs.instrument.Counter", SootClass.SIGNATURES);
//		Options.v().set_app(true);
		soot.Main.main(args);
		System.out.println("Over --------- ");
	}
}