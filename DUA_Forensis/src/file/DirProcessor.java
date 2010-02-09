package file;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Performs the function of -process-dir in Soot */
public class DirProcessor {
	private List<String> classNames = new ArrayList<String>();
	
	public List<String> getClassNames() { return classNames; }
	
	/** @param dirPath base path into which to look for .class files */
	public void processDir(String dirPath) {
		processDirHelper(dirPath, "");
	}
	
	private void processDirHelper(String dirPath, String clsPathPrefix) {
		// open dir
		File dir = new File(dirPath);
		assert dir.isDirectory();
		
		String[] files = dir.list();
		for (String fname : files) {
			String fullName = clsPathPrefix.isEmpty()? fname : clsPathPrefix + "." + fname;
			if (fname.endsWith(".class")) {
				// add file name with package prefix, but removing .class extension
				classNames.add(fullName.substring(0, fullName.length() - ".class".length()));
			}
			else {
				// recursively process if sub-directory
				File f = new File(dirPath + File.separator + fname);
				if (f.isDirectory())
					processDirHelper(dirPath + File.separator + fname, fullName);
			}
		}
	}
	
}
