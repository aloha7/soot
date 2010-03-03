package hku.cs.seg.experiment.qsic2010.io;

import java.io.*;

public class MutantsExtractor {
	
	private ClassFilenameFilter classFilenameFilter = new ClassFilenameFilter();
	private int mutantId;
	private String source;
	private String target;
	private BufferedWriter bw;
	private String pkgName;
	
	public static void exec(String command) {
		try {
			Process p = Runtime.getRuntime().exec("cmd /c " + command);
			p.waitFor();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public static void copy(String source, String target) {
		try {
			
			InputStream in = new BufferedInputStream(new FileInputStream(source));		
			OutputStream out = new BufferedOutputStream(new FileOutputStream(target));		
			for (int b = in.read(); b != -1; b = in.read()){
				out.write(b);
			}
			in.close();
			out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void handleMutants(File[] mutantDirs) throws IOException {
		for (File dir : mutantDirs) {
			if (!dir.isDirectory()) continue;
			File[] classes = dir.listFiles(classFilenameFilter);
			if (classes == null || classes.length == 0) continue;
			
			if (mutantId == 90) {
				System.out.print("");
			}
			
			String mutantDir = target + "\\" + mutantId;
			exec("md " + mutantDir);
			//exec("copy " + dir.getPath() + "\\*.class " + mutantDir);
			
			for (File classFile : classes) {
				copy(classFile.getPath(), mutantDir + "\\" + classFile.getName());
			}
			
			String mutantType = dir.getName();
			int pos = mutantType.indexOf("_");
			if (pos > -1) {
				mutantType = mutantType.substring(0, pos);
			}
			bw.write(mutantId + "\t" + mutantType + "\t" + dir.getName() + "\t" + pkgName + "\n");
			bw.flush();
			
			System.out.println("Mutant #" + mutantId + " extracted.");
			
			mutantId++;
		}
	}
	
	public void doit(String[] args) {
		try {
			source = "C:\\Jack\\MuJava\\result";
			target = "C:\\Jack\\workspace\\QSIC2010_PRI\\mutants\\instances";
			String listfile = "C:\\Jack\\workspace\\QSIC2010_PRI\\mutants\\mutants.list";
			
			if (new File(target).exists()) {
				exec("rd " + target + " /s /q");
			}
			
	        pkgName = null;
			bw = new BufferedWriter(new FileWriter(listfile));

			
        	File[] mutantDirs = null;
			mutantId = 1;
	        for (File dir : new File(source).listFiles()) {
	            if (dir.isDirectory()) {
	            	pkgName = dir.getName();
	            	//pkgName = pkgName.substring(0, pkgName.lastIndexOf("."));
	            	
	            	File[] children = dir.listFiles();
	            	for (File dir1 : children) {
	            		if (!dir1.isDirectory()) continue;
	            		if (dir1.getName().equals("traditional_mutants")) {
	            			for (File methodDir : dir1.listFiles()) {
	            				if (methodDir.isDirectory())
	            					handleMutants(methodDir.listFiles());
	            			}	
	            		} 
	            		else if (dir1.getName().equals("class_mutants")) {
	            			handleMutants(dir1.listFiles());
	            		}
	            	}
	            	
	            }
	        }
			//bw.write(sb.toString());
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

        
	}
	
	public static void main(String[] args) {
		new MutantsExtractor().doit(args);
//		copy("C:\\jack\\workspace\\HelloWorld\\bin\\HelloWorld.class", "C:\\jack\\HelloWorld.class");
	}
}
