package edu.cs.hku.testWriter;

import static edu.cs.hku.testGeneration.Assertion.notNull;
import static edu.cs.hku.testGeneration.Constants.FS;
import static edu.cs.hku.testGeneration.Constants.TAB;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Constructor;

import edu.cs.hku.testGeneration.Block;



public class JUnitTestCaseWriter<T>{
	
	protected final Class<T> testeeClass;
	protected final String comment;
	protected final boolean doAnnotate = false;
	
	
	protected final boolean doFilter;
	protected final int fileNr; //-1 = no number;
	protected final Block<?>[] blocks;
	
	protected Class<? extends Throwable> expectedThrown = null;
	protected int expectedThrowingLineNumber = 0;
	
	
	public JUnitTestCaseWriter(final Class<T> testeeClass, final String comment,
			boolean doFilter, final Block<?>[] blocks){
		this.testeeClass = notNull(testeeClass);
		this.comment = notNull(comment);
		this.doFilter = doFilter;
		this.blocks = notNull(blocks);
		this.fileNr = -1;		
	}
	
	
	
	public File write(){
		notNull(testeeClass);
		notNull(blocks);
		
		String fileName = testeeClass.getSimpleName() + "Test";
		
		StringBuilder sb = new StringBuilder();
		sb.append(System.getProperty("user.dir") + "/");
		
		String[] packagePaths = testeeClass.getName().split("\\.");
		for(int i = 0; i < packagePaths.length-1; i ++){
			sb.append(packagePaths[i] + "/");
		}
		sb.append(fileName + ".java");
		
		final File outFile = new File(sb.toString());
		if(outFile.getParentFile().exists() == false){
			outFile.getParentFile().mkdirs();
		}
		
		FileWriter outWriter = null;
		try {
			outWriter = new FileWriter(notNull(outFile));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//clear the StringBuilder
		sb.delete(0, sb.length());
		
		//the content of the file
		//1.the header--- default package		
		sb.append("package ");
		for(int i = 0; i < packagePaths.length - 1; i ++){
			if(i > 0){
				sb.append(".");
			}
			sb.append(packagePaths[i]);
		}
		sb.append(";" + FS + FS);
		
		//2. JavaDocComment
		String content = this.comment.trim();
		if(content.equals("")){
			content = "TODO: comment";
		}
		sb.append("/**" 							+ FS 
				+content.replaceAll("(?m)^", " * ") + FS +
				"*/"								+ FS);
		
		//3. Header
		String qualSuperClassName = "junit.framework.TestCase";
		if(doFilter){
			qualSuperClassName = "edu.gatech.cc.junit.FilteringTestCase";
		}
		String reinitCode = "";
		if(doFilter){
			reinitCode = TAB + TAB + "/* Re-initialize static fields of loaded classes. */" 	+ FS
						+TAB + TAB + "edu.gatech.cc.junit.reinit.ClassRegistry.resetClasses();" + FS;
		}
		
		content = "Executed before each testXXX().";
		sb.append("public class " + fileName + " extends " + qualSuperClassName + " {" + FS 
				+ TAB + TAB + "/**" 								+ FS 
				+ content.replaceAll("(?m)^", TAB + " * ") 			+ FS
				+ TAB + " */" 										+ FS);
		
		content = "Executed after each testXXX().";
		sb.append(this.doAnnotate?TAB + "@Override" + FS: "" 		+ FS
			+ TAB + "protected void setUp() {" + reinitCode 		+ FS
			+ TAB + TAB + "//TODO: my setup code goes here."		+ FS
			+ TAB + "}" 											+ FS
			+ TAB + TAB + "/**" 									+ FS
			+ content.replaceAll("(?m)^", TAB + " * ") 				+ FS 
			+ TAB + " */" 											+ FS);
		sb.append(this.doAnnotate? TAB + "@Override" + FS: ""		+ FS
			+ TAB + "protected void tearDown() throws Exception {" 	+ FS
			+ TAB + TAB + "super.tearDown();" 						+ FS
			+ TAB + TAB + "//TODO: my tear down code goes here."	+ FS
			+ TAB + "}" 											+ FS);

		
		//4. Test cases
		content = "JCrasher-generated test cases.";
		for(int i = 0; i < blocks.length; i ++){
			sb.append( 													  FS
					+ TAB + TAB + "/**" 								+ FS 
					+ content.replaceAll("(?m)^", TAB + " * ") 			+ FS
					+ TAB + " */" 										+ FS
					+ TAB + "public void test" + i + "() throws Throwable ");
			if(doFilter){
				sb.append("{" + TAB + TAB + "try");
			}
			sb.append(blocks[i].text() + FS);
			if(doFilter){
				sb.append(TAB + TAB + "catch (Throwable throwable){throwIf(throwable);}" + FS
						+ TAB + "}" +													   FS);
			}
		}
		sb.append(TAB + TAB);
		
		//5. Footer
		final String qualTesteeName = testeeClass.getName();
		String testedMethName = null;
		switch(blocks.length){
			case 0: 
				testedMethName = null; 
				break;
			case 1:
				testedMethName = this.getTestedMethName(blocks[0]);
				break;
			default:
				String temp = this.getTestedMethName(blocks[0]);
				for(Block<?> block: blocks){
					if(!this.getTestedMethName(block).equals(testedMethName)){
						testedMethName = null;
						break;
					}
				}
				testedMethName = temp;
				break;
		}
		
		if(doFilter && (testedMethName != null)){
			sb.append(this.doAnnotate? TAB+ "@Override" + FS: ""  + FS
					+ TAB + "protected String getNameOfTestedMeth() {" + FS
					+ TAB + TAB + "return \"" + qualTesteeName + "." + testedMethName + "\";" + FS
					+ TAB + "}" +FS
					+ TAB +      FS);
		}
		
		if(doFilter && (expectedThrown != null)){
			sb.append(this.doAnnotate? TAB+ "@Override" + FS: ""  + FS
					+ TAB + "protected Class<? extends Throwable> getExpectedThrowable() {" + FS
					+ TAB + TAB + "return " + expectedThrown.getName() + ".class;" 		+ FS
					+ TAB + "}" +FS
					+ TAB +      FS);
		}
		
		if(doFilter && (expectedThrowingLineNumber > 0 )){
			sb.append(this.doAnnotate? TAB+ "@Override" + FS: ""  + FS
					+ TAB + "protected int getExpectedThrowingLineNumber() {" + FS
					+ TAB + TAB + "return " + expectedThrowingLineNumber + ";" 		+ FS
					+ TAB + "}" +FS
					+ TAB +      FS);
		}
		
		content = "Constructor";
		sb.append( 													  FS
				+ TAB + TAB + "/**" 								+ FS 
				+ content.replaceAll("(?m)^", TAB + " * ") 			+ FS
				+ TAB + " */" 										+ FS
				+ TAB + "public " +  fileName + "(String pName) {"  + FS
				+ TAB + TAB + "super(pName);" 						+ FS
				+ TAB + "}"											+ FS											
				);
		
		content = "Easy access for aggregating test suite.";
		sb.append( 													  FS
				+ TAB + TAB + "/**" 								+ FS 
				+ content.replaceAll("(?m)^", TAB + " * ") 			+ FS
				+ TAB + " */" 										+ FS
				+ TAB + "public static junit.framework.Test Suite() {" + FS 
				+ TAB + TAB + "return new junit.framework.TestSuite(" + fileName +".class);" + FS 
				+ TAB + "}" 
				+ TAB);
		
		content = "Main";
		sb.append( 													  FS
				+ TAB + TAB + "/**" 								+ FS 
				+ content.replaceAll("(?m)^", TAB + " * ") 			+ FS
				+ TAB + " */" 										+ FS
				+ TAB + "public static void main(String[] args) {" + FS 
				+ TAB + TAB + "junit.textui.TestRunner.run(" + fileName +".class);" + FS 
				+ TAB + "}" + FS + "}");
		
		try {
			outWriter.write(sb.toString());
			outWriter.flush();
			outWriter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return outFile;
		
	}
	
	protected String getTestedMethName(Block block){
		if(block.getTestee() instanceof Constructor)
			return "<init>";
		return block.getTestee().getName();
	}
	
	
}
