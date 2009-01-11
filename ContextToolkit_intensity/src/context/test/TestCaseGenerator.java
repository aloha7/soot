package context.test;


import java.util.Random;
import java.util.Vector;
import java.util.Hashtable;
import java.util.StringTokenizer;

import context.test.util.ContextEvent;
import context.test.util.Randomer;
import context.test.util.TestCase;
import context.test.util.ContextEvent;
import context.test.util.Constant;

import java.io.*;


public class TestCaseGenerator extends Vector{
	private Vector widgets = null;
	private Hashtable caseHash = null;
	private String SEPERATOR = "\t";
	
	//constructor used to retrieve test cases
	public TestCaseGenerator(){
		
	}
	
	//constructor used to generate test cases
	public TestCaseGenerator(Vector widgets){
		this.widgets = widgets;
		this.caseHash = new Hashtable();
	}
	
	/**2008/7/9
	 * make test cases automatically. One test case consists of a event sequence.Every context event 
	 * in this sequence includes context(such as update, visit, and end), durative(the durative time 
	 * of this event), context number(the serial number in the widgets)
	 * @param TestSuiteSize: number of test cases 
	 * @return
	 */
	public Vector generateTestCases(int testSuiteSize){
		int i = 0;
		while( i < testSuiteSize){
			TestCase testCase = new TestCase();
			testCase.initializeTestCase(widgets.size());
			if(!isDuplicate(testCase)){ //if the test case is not duplicate			
				addElement(translate(testCase)); // store this test case				
				i ++;				
				
				Vector caseCluster = (Vector)caseHash.get(new Integer(testCase.length));
				if(caseCluster == null){
					caseCluster = new Vector();
				}
				caseCluster.add(testCase);
				caseHash.put(new Integer(testCase.length), caseCluster); //just for comparision's convinience				
			}			
		}
		return this;
	}
	
	private boolean isDuplicate(TestCase testCase){
		boolean result = false;
		
		if(caseHash.size() > 0){

			Vector vector = (Vector)caseHash.get(testCase.length);
			if(vector != null){
				for(int i = 0 ; i < vector.size(); i ++){
					TestCase sampleCase = (TestCase)vector.get(i);			
					if(testCase.isEqual(sampleCase)){
						result = true;
						break;
					}
				}				
			}				
		}
		return result;
	}
	
	//get the context's content according to contextNumber
	private TestCase translate(TestCase sample){ 
		for(int i = 0; i < sample.size(); i ++){
			ContextEvent event = (ContextEvent)sample.get(i);
			String context = (String)widgets.get(event.index);
			event.context = context;			
		}
		return sample;
	}
	
	/**keep all test cases into files
	 * 
	 * @param testCases: a vector recording all test cases
	 * @param folder: the directory to store test cases
	 */
	public void storeTestCases(Vector testCases, String path){		
		File dir = new File(path).getParentFile();
		if(!dir.exists())
			dir.mkdirs();
		
		try{
			BufferedWriter writer = new BufferedWriter(new 
					FileWriter(path, false));
			for(int i = 0; i < this.size(); i ++){
				writer.write((TestCase)this.get(i) + Constant.LINESHIFTER);			
			}
			writer.flush();
			writer.close();			
		}catch(Exception e){
			System.out.println(e);
		}
	}
	
	
	public Vector retrieveTestCases(String path){
		Vector caseCluster = new Vector();
		File file = new File(path);
		if(file.exists()){
			try{
				BufferedReader reader = new BufferedReader(new FileReader(path));
				StringBuffer sb = new StringBuffer();
				String line = null;
				while((line = reader.readLine()) != null){
						TestCase testCase = new TestCase(line);
						caseCluster.add(testCase);
						//caseCluster.addElement(testCase);
						//caseCluster.addElement(new TestCase(line));											
				}				
				/*
				StringTokenizer token = new StringTokenizer(sb.toString(), Constant.LINESHIFTER);
				while(token.hasMoreTokens()){
					testCases.addElement(new TestCase(token.nextToken()));
				}*/
			}catch(Exception e){
				System.out.println(e);
			}
		}
		
		
		return caseCluster;
	}
	
	
	
	
	public static void main(String[] args){
		
	}
}
