package hku.cs.seg.experiment.qsic2010.io;

import java.io.File;
import java.io.FilenameFilter;

public class ClassFilenameFilter implements FilenameFilter {

	public boolean accept(File arg0, String arg1) {
		return arg1.endsWith(".class");
	}

}
