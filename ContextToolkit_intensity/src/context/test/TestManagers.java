package context.test;

import java.util.Vector;

import context.test.contextIntensity.Manipulator;
import context.test.util.Constant;

public class TestManagers {

	
	
	public static void main(String[] args) {
		TestSetManager manager_TS = new TestSetManager();
		//1. get Adequacy test sets from files
		String file_TS = Constant.baseFolder
		+ "ContextIntensity/AdequateTestSet/TestSet_CA.txt";
		Vector testSets =  manager_TS.getAdequateTestSetsFromFile(file_TS);
		
		//2. set the Drivers before execution
		String criteriaFile = Constant.baseFolder
		+ "ContextIntensity/Drivers/Drivers_CA.txt";
		Manipulator manager = Manipulator.getInstance(criteriaFile);
		
		//2009/1/18: use Ant can cause unexpected exceptions, we use reflections to have a  
		//3.generate Ant file according to testSets and run it
		int min_Version = 0;
		int max_Version = 0;
		String criteria = "CA";
		OutputProducer manager_Output = new OutputProducer();
		for(int versionNumber = min_Version; versionNumber <= max_Version; versionNumber ++){			
			//2. generate Ant files
			String build_file = manager_Output.generateAntScript(testSets, criteria, versionNumber);
			//3. run Ant files
			new ExecAntThread(build_file).start();
		}
	}

}
