package hku.cs.tools;

import java.io.File;

public class DirTools {
	
	public static String SEPERATOR = "/";
	public static char SEPERATORCHAR = '/';
	
	public static void delete(File path) {
		if (!path.exists())
			return;
		if (path.isFile()) {
			path.delete();
			return;
		}
		File[] files = path.listFiles();
		for (int i = 0; i < files.length; i++) {
			delete(files[i]);
		}
		path.delete();
	}
	
	public static void delete(String path) {	
		delete(new File(path));
	}
	
	public static File prepare(String path){
		if ("/".equals(path))
			return null;
		File f = new File(path);
		if (!f.exists())
			f.mkdirs();
		else if(!f.isDirectory()){
			f.delete();
			f.mkdirs();
		}
		return f;
	}

}
