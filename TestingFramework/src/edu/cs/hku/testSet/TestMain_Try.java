package edu.cs.hku.testSet;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import trivia.TestClass;
import edu.cs.hku.instrument.Probe_block;
import edu.cs.hku.util.Constants;

public class TestMain_Try {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		String cp_new = Constants.USER_DIR + Constants.FS + "sootOutput;"
				+ Constants.CLASSPATH;
		System.setProperty("java.class.path", cp_new);

		try {
			URL tempURL;

			for (String clz : args) {
				tempURL = new File(Constants.USER_DIR + Constants.FS
						+ "sootOutput" + Constants.FS + clz.replace('.', '/')
						+ ".class").toURL();

				URLClassLoader loader = new URLClassLoader(
						new URL[] { tempURL });
				Class c = loader.loadClass(clz);
				TestClass ins = (TestClass) c.newInstance();
				args = new String[] { "-1" };
				ins.main(args);

				Probe_block.reportBlock();
				Probe_block.blockToStmt();
				Probe_block.reportStmt();

				args = new String[] { "1" };
				ins.main(args);

				Probe_block.reportBlock();
				Probe_block.blockToStmt();
				Probe_block.reportStmt();
			}
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	// 2. run the class under test
	// String cmd = "";
	// for(String clz: classList){
	// cmd = "java -classpath " + cp_new + " ";
	// cmd += clz + " -1 ";
	// try {
	// this.getClass().getClassLoader().loadClass(clz);
	// } catch (ClassNotFoundException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// }
	// }
	// JavaRunner.runCommand(cmd);
	// for(String clz: classList){
	// try {
	// Class clazz = Class.forName(clz);
	// System.out.println(clazz.getClassLoader().getSystemResource(clz.replace('.',
	// '/') + ".class").getPath());
	// // Object obj = clazz.newInstance();
	// } catch (ClassNotFoundException e) {
	// // TODO Auto-generated catch block
	//				e.printStackTrace();
	//			}
	//		}

}
