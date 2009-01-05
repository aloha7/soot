package ccr.test;

import ccr.app.*;
import ccr.stat.*;

import java.io.*;

public class Instrument {
	
	public static void instrument(
			String sourceFile, String destinationFolder, String className) {
		
		SourceHandler source = new SourceHandler(sourceFile);
		String destinationFile = destinationFolder + "/" + className + ".java";
		try {
			BufferedReader br = new BufferedReader(new FileReader(sourceFile));
			String line = br.readLine();
			BufferedWriter bw = new BufferedWriter(new FileWriter(destinationFile));
			boolean inApp = false;
			int policyIndex = 0;
			while (line != null) {
				if (line.indexOf(Application.ENTRY_TAG) != -1) {
					inApp = true;
					String programID = source.getContent(Application.PROGRAM_ID_TAG);
					int statementIndex = 0;
					int tab = 0;
					for (int i = 0; i < line.length(); i++) {
						if (line.charAt(i) == '\t') {
							tab++;
						} else {
							break;
						}
					}
					for (int i = 0; i < source.sizeStatements(); i++) {
						Statement statement = source.getStatement(i);
						if (statement.prefix().equals("}")) {
							tab--;
						}
						String prefix = "";
						for (int j = 0; j < tab; j++) {
							prefix = prefix + "\t";
						}
						String newline;
						if (statement.isNode()) {
							if (statement.hasContextDef()) {
								newline = prefix + Application.CONTEXT_DEFINE_INDEX + " = " + 
									"\"" + programID + (statementIndex++) + "\";";
							} else {
								newline = prefix + Application.RECORD_METHOD + "(\"" + 
									programID + (statementIndex++) + "\");";
							}
							bw.write(newline);
							bw.newLine();
						}
						newline = prefix + statement.toString();
						if (!statement.prefix().equals("{") && 
								i + 1 < source.sizeStatements() && 
								source.getStatement(i + 1).prefix().equals("{")) {
							newline = newline + " " + source.getStatement(i + 1).toString();
							i++;
							tab++;
						}
						bw.write(newline);
						bw.newLine();
					}
				}
				if (line.indexOf(Application.POLICY_TAG) != -1) {
					String prefix = "";
					for (int i = 0; i < line.length(); i++) {
						if (line.charAt(i) == '\t') {
							prefix = prefix + '\t';
						} else {
							break;
						}
					}
					String newLine = prefix + Application.RECORD_METHOD + "(" + 
						Application.CONTEXT_DEFINE_INDEX + " + \"" + 
						Application.POLICY_NODE_DELIMITER + 
						Application.POLICY_INDEX_PREFIX + (policyIndex++) + "\");";
					bw.write(newLine);
					bw.newLine();
				}
				if (line.indexOf(Application.CONTEXT_DEFINE_TAG) != -1) {
					String prefix = "";
					for (int i = 0; i < line.length(); i++) {
						if (line.charAt(i) == '\t') {
							prefix = prefix + '\t';
						} else {
							break;
						}
					}
					String newLine = prefix + Application.RECORD_METHOD + "(" + 
						Application.CONTEXT_DEFINE_INDEX + ");";
					bw.write(newLine);
					bw.newLine();
				}
				if (!inApp) {
					if (line.indexOf("public class") != -1) {
						int i = line.indexOf("public class") + "public class".length();
						i = line.indexOf(" ", i);
						int j = line.indexOf(" ", i + 1);
						bw.write(line.substring(0, i + 1) + className + line.substring(j));
						bw.newLine();
					} else {
						bw.write(line);
						bw.newLine();
					}
				}
				if (line.indexOf(Application.EXIT_TAG) != -1) {
					inApp = false;
				}
				line = br.readLine();
			}
			br.close();
			bw.flush();
			bw.close();
		} catch (IOException e) {
			System.out.println(e);
		}
	}
	
	public static void generateVersion(
			String sourceFile, String destinationFolder, 
			String packageName, String className) {
		
		String destinationFile = destinationFolder + "/" + packageName + "/" + 
				className + ".java";
		try {
			BufferedReader br = new BufferedReader(new FileReader(sourceFile));
			String line = br.readLine();
			BufferedWriter bw = new BufferedWriter(new FileWriter(destinationFile));
			while (line != null) {
				if (line.indexOf("package ") != -1) {
					int i = line.indexOf(";");
					bw.write(line.substring(0, i) + "." + packageName + line.substring(i));
					bw.newLine();
					bw.newLine();
					bw.write("import ccr.app.*;");
				} else if (line.indexOf("public class") != -1) {
					int i = line.indexOf("public class") + "public class".length();
					i = line.indexOf(" ", i);
					int j = line.indexOf(" ", i + 1);
					bw.write(line.substring(0, i + 1) + className + line.substring(j));
					bw.newLine();
					bw.write("// Fault notes: ");
				} else {
					bw.write(line);
				}
				bw.newLine();
				line = br.readLine();
			}
			br.close();
			bw.flush();
			bw.close();
		} catch (IOException e) {
			System.out.println(e);
		}
	}
	
	public static void generateVersions(
			String sourceClassName, String destinationFolder, 
			String packageName, int numOfVersions, int startNum) {
		
		if (numOfVersions < 1 || startNum < 0) {
			return;
		}
		int labelLength = ("" + (numOfVersions + startNum)).length();
		String sourceFile = TestDriver.APPLICATION_FOLDER + "/" + sourceClassName + ".java";
		for (int i = startNum; i < (numOfVersions + startNum); i++) {
			String appClassName = sourceClassName + "_";
			for (int j = ("" + i).length(); j < labelLength; j++) {
				appClassName = appClassName + "0";
			}
			appClassName = appClassName + i;
			generateVersion(sourceFile, destinationFolder, packageName, appClassName);
		}
	/*	if (numOfVersions > 0 && numOfVersions <= 10) {
			for (int i = 0; i < numOfVersions; i++) {
				generateVersion(
						sourceFile, destinationFolder, packageName, sourceClassName + "_" + i);
			}
		} else if (numOfVersions > 10 && numOfVersions <= 100) {
			for (int i = 0; i < 10; i++) {
				generateVersion(
						sourceFile, destinationFolder, packageName, sourceClassName + "_0" + i);
			}
			for (int i = 10; i < numOfVersions; i++) {
				generateVersion(
						sourceFile, destinationFolder, packageName, sourceClassName + "_" + i);
			}
		} else if (numOfVersions > 100 && numOfVersions <= 1000) {
			for (int i = 0; i < 10; i++) {
				generateVersion(
						sourceFile, destinationFolder, packageName, sourceClassName + "_00" + i);
			}
			for (int i = 10; i < 100; i++) {
				generateVersion(
						sourceFile, destinationFolder, packageName, sourceClassName + "_0" + i);
			}
			for (int i = 100; i < numOfVersions; i++) {
				generateVersion(
						sourceFile, destinationFolder, packageName, sourceClassName + "_" + i);
			}
		}*/
	}
	
	public static void main(String argv[]) {
		
	//  The instrumented version should be laid in the same folder	
	//	instrument("src/ccr/app/TestCFG.java", "src/ccr/app", "TestCFG_ins");
		instrument("src/ccr/app/TestCFG2.java", "src/ccr/app", "TestCFG2_ins");
	//	instrument("src/ccr/app/TestCFG2.java", "experiment", "TestCFG2_ins");
		
	/*	int numOfVersions = 30;
		String sourceFile = "src/ccr/app/TestCFG2.java";
		for (int i = 0; i < numOfVersions; i++) {
			generateVersion(sourceFile, "experiment", "version", "TestCFG2_" + i);
		}*/
		
	//	generateVersions("TestCFG2", "experiment", TestDriver.VERSION_PACKAGE_NAME, 20);
	//	generateVersions("TestCFG2", "experiment", "trialversion", 1, 0);
		generateVersions("TestCFG2", "src/ccr/app", "testversion", 100, 1010);
		
	//	for (int i = 0; i < 10; i++) {
	//		generateVersions("TestCFG2", "src/ccr/app", "testversion/version0" + i, 10, 10 * i);
	//	}
	//	for (int i = 0; i < 5; i++) {
	//		generateVersions("TestCFG2", "experiment", "version1" + i, 10, 100 + 10 * i);
	//	}
	}

}
