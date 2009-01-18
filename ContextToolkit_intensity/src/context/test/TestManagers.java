package context.test;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Vector;

import context.arch.generator.PositionIButton;
import context.test.contextIntensity.Manipulator;
import context.test.util.Constant;
import context.test.util.Logger;

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
		Vector drivers = manager.driverMatrix;
		
		//2009/1/18: use Ant can cause unexpected exceptions, we use reflections to have a  
		//3.generate Ant file according to testSets and run it
		int min_Version = 0;
		int max_Version = 0;
		String criteria = "CA";
		OutputProducer manager_Output = new OutputProducer();
//		for(int versionNumber = min_Version; versionNumber <= max_Version; versionNumber ++){			
//			//2. generate Ant files
//			String build_file = manager_Output.generateAntScript(testSets, criteria, versionNumber);
//			//3. run Ant files
//			new ExecAntThread(build_file).start();
//		}
		
		for(int versionNumber = min_Version; versionNumber <= max_Version; versionNumber++){
			for(int i = 0; i < testSets.size(); i ++){
				//4.prepare the directory: delete it when it exists and recreate it
				String directory = Constant.baseFolder + "test/output/" + criteria +"/" + versionNumber + "/" +i ;
				Logger.getInstance().delete(directory);
				new File(directory).mkdirs();
				
				Vector testSet = (Vector)testSets.get(i);
				for(int j = 0; j < testSet.size(); j ++){
					int testCaseIndex = Integer.parseInt((String)testSet.get(j));
					String testCaseInstance = manager_TS.getTestCaseInstance(testCaseIndex);
					String file = directory + "/" + testCaseIndex+".txt";
					
					try {
						//5.redirect the output before execution
						PrintStream ps =new PrintStream(new BufferedOutputStream(new FileOutputStream(file))); 
						System.setOut(ps);
						System.setErr(ps);
						//6. execute the test case
						PositionIButton.getInstance().set(0, testCaseIndex, testCaseInstance);
						PositionIButton.getInstance().runTestCase();
						PositionIButton.getInstance().stopRunning();
						
						//7. close the output
						ps.close();
						//8. reset the drivers
						
					} catch (FileNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				manager.setDrivers(drivers);
			}
		}
		System.exit(0);
	}

}
