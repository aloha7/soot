package context.test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Random;
import java.util.Vector;



import context.arch.generator.PositionIButton;
import context.arch.widget.WTourDemo;
import context.arch.widget.WTourEnd;
import context.arch.widget.WTourRegistration;
import context.test.contextIntensity.Manipulator;
import context.test.util.Constant;
import context.test.util.ContextStream;
import context.test.util.Logger;

public class TestSetManager {

	public static Hashtable testPool; //ID-context stream pair
	public static Vector testSets;
	public static int SIZE_TESTPOOL = 1000;
	public static int NUMBER_TESTSET = 100; // 2009/1/19: this number must be
											// big
	// enough to get equal-sized test sets
	public static int MAX_LENGTH_TESTCASE = 20;
	public static int MIN_LENGTH_TESTCASE = 4;
	public static int FIX_LENGTH_TESTCASE = -19;
	public static HashMap intensities;

	public static Vector generateAllTestSets(String criteriaFile) {
		Vector testSets = new Vector();
		do {
			Vector testSet = TestSetManager.generateAdequateTestSet(criteriaFile);
			// testSets.add(testSet);
			if (!testSets.contains(testSet)) {
				testSets.add(testSet);
			}
		} while (testSets.size() < NUMBER_TESTSET);
		
		return testSets;
	}

	
	/**
	 * select same sized test sets to be a candidate
	 * 
	 * @param size:
	 *            the size of candidate test set
	 * @param testSetFile
	 * @param saveFile:
	 *            a record in this file: time + event sequences + intensity of
	 *            test set
	 */
	public static void analysisTestSets(String size_TS, String testSetFile,
			String saveFile) {
		try {
			BufferedReader br = new BufferedReader(new FileReader(testSetFile));
			String line = null;
			StringBuilder sb = new StringBuilder();
			while ((line = br.readLine()) != null) {
				// line = time + size + event sequence
				String[] strs = line.split("\t");
				String size = strs[3];
				if (size.equals(size_TS)) {
					sb.append(strs[0] + "\t" + strs[1] + "\t");

					double sumIntensity = 0;
					for (int i = 0; i < Integer.parseInt(size); i++) {
						String testCaseIndex = strs[i + 4];
						sumIntensity += TestSetManager.getIntensity(testCaseIndex);
						sb.append(testCaseIndex + "\t");
					}
					double averageIntensity = sumIntensity
							/ Integer.parseInt(size);
					sb.append(averageIntensity + "\n");
				}

			}
			br.close();
			BufferedWriter bw = new BufferedWriter(new FileWriter(saveFile));
			bw.write(sb.toString());
			bw.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

/**2009-02-27:interprete the CI of adequate test sets
 * 
 * @param containHeader
 * @param testSetFile
 * @param saveFile
 */
	public static void attachTSWithCI(boolean containHeader, String testSetFile, String saveFile) {
		try {
			BufferedReader br = new BufferedReader(new FileReader(testSetFile));

			String str = null;
			StringBuilder sb = new StringBuilder();
			if(containHeader)
				br.readLine();
			
			while ((str = br.readLine()) != null) {
				String temp = str;
				String[] testCases = temp.substring(
						temp.indexOf("[") + "[".length(), temp.indexOf("]"))
						.split(",");

				double sumIntensity = 0;
				for (String testCaseIndex : testCases) {
					sumIntensity += ((ContextStream)TestSetManager.testPool.get(testCaseIndex.trim())).CI;
				}
				double averageIntensity = sumIntensity / (double)testCases.length;
				sb.append(str + "\t" + "CI:" + "\t" + averageIntensity + "\n");
			}
			br.close();

			Logger.getInstance().setPath(saveFile, false);
			Logger.getInstance().write(sb.toString());
			Logger.getInstance().close();

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
//		try {
//			BufferedReader br = new BufferedReader(new FileReader(testSetFile));
//
//			String str = null;
//			StringBuilder sb = new StringBuilder();
//			while ((str = br.readLine()) != null) {
//				String[] testCases = str.substring(
//						str.indexOf("[") + "[".length(), str.indexOf("]"))
//						.split(",");
//				// String[] testCases = str.substring(0, str.indexOf("time:"))
//				// .split("\t");
//
//				double sumIntensity = 0;
//				for (String testCaseIndex : testCases) {
//					sumIntensity += this.getIntensity(testCaseIndex);
//				}
//				double averageIntensity = sumIntensity / testCases.length;
//				sb.append("ContextIntensity:" + "\t" + averageIntensity + "\t"
//						+ str + "\n");
//			}
//			br.close();
//
//			Logger.getInstance().setPath(saveFile, false);
//			Logger.getInstance().write(sb.toString());
//			Logger.getInstance().close();
//
//		} catch (FileNotFoundException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
	}

	public static double getIntensity(String testCaseIndex) {
		if (TestSetManager.intensities == null) {
			String testPoolFile = Constant.baseFolder
					+ "ContextIntensity/TestPool.txt";
			TestSetManager.getIntensities(testPoolFile);
		}
		return Double.parseDouble((String) TestSetManager.intensities.get(testCaseIndex));
	}

	/**
	 * facilitate the analysis of data
	 * 
	 * @param filePath
	 * @param savePath
	 */
	private static void tranverseData(String filePath, String savePath) {
		try {
			BufferedReader br = new BufferedReader(new FileReader(filePath));
			String line = null;

			Vector testPool = new Vector();

			StringBuffer sb = new StringBuffer();
			while ((line = br.readLine()) != null) {
				String[] flags = line.split("\t");
				for (int i = 2; i < flags.length; i++) {
					sb.append(flags[i] + "\t");
				}
				sb.append(flags[0] + "\t" + flags[1] + "\t\n");
			}
			br.close();

			BufferedWriter bw = new BufferedWriter(new FileWriter(savePath));
			bw.write(sb.toString());
			bw.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static double getCI(Vector eventSequences){
		if(eventSequences.size() == 1){
			return 0.0;
		}else{
			int HammingDistance = 0;
			for (int i = 0; i < eventSequences.size() - 1; i++) {
				String previous = (String) eventSequences.get(i);
				String latest = (String) eventSequences.get(i + 1);
				if (!previous.equals(latest)) {
					HammingDistance++;
				}
			}
			return (double) HammingDistance / (eventSequences.size() - 1);
		}
	}
	
	public static double analysisIntensity(String testCaseInstance) {
		// 2009-02-26:
		Vector eventSequences = new Vector();

		String[] events = testCaseInstance.split("\t");
		for (int i = 0; i < events.length; i++) {
			eventSequences.add(events[i]);
		}
		return TestSetManager.getCI(eventSequences);

		// int index = testCaseInstance.indexOf("\t");
		// while(index > -1){
		// String event = testCaseInstance.substring(0, index);
		// eventSequences.add(event);
		// testCaseInstance = testCaseInstance.substring(index + "\t".length());
		// index = testCaseInstance.indexOf("\t");
		// }
		// if(testCaseInstance.length() >0){
		// eventSequences.add(testCaseInstance);
		// }
		// if(eventSequences.size() == 1){
		// return 0;
		// }else{
		// double HammingDistance = 0;
		// for(int i = 0; i < eventSequences.size() - 1; i ++){
		// String before = (String)eventSequences.get(i);
		// String after = (String)eventSequences.get(i +1);
		// if(!before.equals(after)){
		// HammingDistance ++;
		// }
		// }
		// return HammingDistance/(eventSequences.size()-1);
		// }
	}

	public static HashMap getIntensities(String testPoolFile) {
		TestSetManager.intensities = new HashMap();
//		if (testPool == null) {
//			this.loadTestPoolFromFile(testPoolFile);
//		}
//
//		for (int i = 0; i < testPool.size(); i++) {
//			String testCaseInstance = (String) testPool.get(i);
//			intensities.put("" + i, ""
//					+ this.analysisIntensity(testCaseInstance));
//		}
		Iterator ite = TestSetManager.testPool.values().iterator();
		while(ite.hasNext()){
			ContextStream cs = (ContextStream)ite.next();
			TestSetManager.intensities.put(cs.ID, cs.CI);
		}
		
		return TestSetManager.intensities;
	}

	/**
	 * 
	 * @param testSetNum
	 * @param maxTrial
	 * @return
	 */
	public static Vector generateAllTestSetsAndSave_refine(int testSetNum, int maxTrial,
			String criteriaFile, String saveFile){
		Vector testSets = new Vector();
		StringBuilder sb = new StringBuilder();
		do{
			long start = System.currentTimeMillis();
			Vector testSet = TestSetManager.generateAdequateTestSet_refine(criteriaFile, maxTrial);
			long enduration = System.currentTimeMillis() - start;
			if(!testSets.contains(testSet)){
				//index + size + time + test cases
				sb.append(testSets.size() + "\t" + testSet.size() + "\t" +
						"time:\t" + enduration + "\t" + testSet + "\n");
				testSets.add(testSet);
				System.out.println(testSets.size() + "th test set");
			}
		}while(testSets.size() < testSetNum);
		
		Logger.getInstance().setPath(saveFile, false);
		Logger.getInstance().write(sb.toString());
		Logger.getInstance().close();
		
		return testSets;
	}
	
	/**A refined approach to generate adequate test sets: a test case is added into a test set if
	 * (1). it increases the coverage of test drivers; or (2).it does not decrease not the coverage
	 * but has a higher CI value than existing one   
	 * 
	 * @param criteriaFile
	 * @param maxTrial
	 * @return
	 */
	public static Vector generateAdequateTestSet_refine(String criteriaFile, int maxTrial){
		// 2009/1/17:
		Vector testSet = new Vector();
		long start = System.currentTimeMillis();
		// 1.load all drivers for a specified criteria
		Manipulator manager = Manipulator.getInstance(criteriaFile);
		Vector drivers = manager.getAllUncoveredDrivers();
		Vector leftDrivers;
		Hashtable tc_drivers = new Hashtable(); //keep all drivers  covered by a test case
		
		int trial = 0;
		int replaceCounter = 0;
		
		do {
			trial ++;
			// 2. random select a test case which is not in testSet
			int testCaseIndex = TestSetManager.getTestCaseIndex(testSet);
			// int testCaseIndex = 0;
			ContextStream testCase = (ContextStream) TestSetManager.testPool.get(""+testCaseIndex);

			// 3.feed test case to SUT: faulty version index(must be golden
			// version 0) + test case index
			StringBuilder sb = new StringBuilder();
			for(int i = 0; i < testCase.eventSequence.size(); i++){
				sb.append(((String)testCase.eventSequence.get(i)).trim()+"\t");
			}
			
			PositionIButton.getInstance().set(0, testCaseIndex, sb.toString());
			PositionIButton.getInstance().runTestCase();
			PositionIButton.getInstance().stopRunning();

			// 4.judge whether this test case increases the coverage
			leftDrivers = manager.getAllUncoveredDrivers();
			if (leftDrivers.size() < drivers.size()
					&& !testSet.contains(testCaseIndex)) {
				// testSet.add(testCase);
				// 2009/1/18: it is better to keep index instead of contents
				testSet.add(testCaseIndex);
				
				Vector coveredDrivers = new Vector();
				for(int i = 0; i < drivers.size(); i ++){
					if(!leftDrivers.contains(drivers.get(i))){
						coveredDrivers.add(drivers.get(i)); 
					}
				}
				tc_drivers.put(testCaseIndex, coveredDrivers);
			} else if(!testSet.contains(testCaseIndex)){

				//do not increase the coverage, replace the one has the lowest CI values
				int replaceIndex = -1;
				double minCI = Double.MAX_VALUE; 
				for(int i = 0; i < testSet.size(); i ++){
					ContextStream cs = (ContextStream)TestSetManager.testPool.get(""+(Integer)testSet.get(i));
					if(cs.CI < minCI){
						minCI = cs.CI;
						replaceIndex = i;
					}
				}
				if(((ContextStream)TestSetManager.testPool.get(testCaseIndex)).CI > minCI){
					testSet.remove(replaceIndex);
					testSet.add(testCaseIndex);
					replaceCounter ++;
				}
			}

			// 5.re-initial the Manipulator
			drivers = leftDrivers;
			manager.setDrivers(drivers);
		} while (leftDrivers.size() > 0 ||trial < maxTrial);// 6.feed more test cases if there
		// are some uncovered drivers
		long enduration = System.currentTimeMillis() - start;
		System.err.println("Get an adequate test set (time: " + enduration
				+ "):" + "\n" + TestSetManager.toString(testSet) + " replaced:" + replaceCounter);
		return testSet;
	}
	
	
	public static Vector generateTestSetsAndSave(int testSetNum, int testSuiteSize, 
			int maxTrial, String date, String criteria, String oldOrNew, 
			String randomOrCriteria, String saveFile){
		Vector testSets  = new Vector();
		String driverFile = "ContextIntensity/" + date + "/Drivers_" + criteria
		+ ".txt";
		StringBuilder sb = new StringBuilder();
		if(testSuiteSize < 0){
			//do not fix the test set size
			if(oldOrNew.equals("new")){
				do{
					long start = System.currentTimeMillis();
					
					Vector testSet = TestSetManager.generateAdequateTestSet_refine(
							driverFile, maxTrial);
					
					long enduration = System.currentTimeMillis() -start;
					if(!testSets.contains(testSet)){
						sb.append(testSets.size() + "\t" + testSet.size() + "\t"
								+ "time:\t" + enduration + "\t" + testSet + "\n");
						testSets.add(testSet);
						System.out.println(testSets.size() + "th test set");
					}
				}while(testSets.size() < testSetNum);
				
			}else if(oldOrNew.equals("old")){
				do{
					long start = System.currentTimeMillis();
					
					Vector testSet = TestSetManager.generateAdequateTestSet(driverFile);
					
					long enduration = System.currentTimeMillis() -start;
					if(!testSets.contains(testSet)){
						sb.append(testSets.size() + "\t" + testSet.size() + "\t"
								+ "time:\t" + enduration + "\t" + testSet + "\n");
						testSets.add(testSet);
						System.out.println(testSets.size() + "th test set");
					}
				}while(testSets.size() < testSetNum);
			}
		}else{
			//fix the test set size
			if(oldOrNew.equals("new")){
				
			}else if(oldOrNew.equals("old")){
				
			}
		}
		

		
		// 3. save testSets into files
		Logger.getInstance().setPath(saveFile, false);
		Logger.getInstance().write(sb.toString());
		Logger.getInstance().close();

		return testSets;
	}
	
	public static Vector generateAllTestSetsAndSave(int testSetNum,
			String criteriaFile, String saveFile) {

		StringBuilder sb = new StringBuilder();
		TestSetManager.testSets = new Vector();

		do {
			long start = System.currentTimeMillis();
			Vector testSet = TestSetManager.generateAdequateTestSet(criteriaFile);

			long enduration = System.currentTimeMillis() - start;

			// 1. judge whether duplicated
			if (!testSets.contains(testSet)) {
				// 2. 2009/1/19:save test case(change the format)
				// sb.append("time:\t" + enduration + "\t");
				// sb.append("size:\t" + testSet.size() + "\t");
				// for (int i = 0; i < testSet.size(); i++) {
				// sb.append(testSet.get(i) + "\t");
				// }
				// sb.append("\n");

				// 2. 2009/2/13:save test case(change the format)(index + size +
				// time + index)
				sb.append(testSets.size() + "\t" + testSet.size() + "\t"
						+ "time:\t" + enduration + "\t" + testSet + "\n");
				// for (int i = 0; i < testSet.size(); i++) {
				// sb.append(testSet.get(i) + "\t");
				// }
				// sb.append("time:\t" + enduration + "\t");
				// sb.append("\n");

				testSets.add(testSet);
				System.out.println(testSets.size() + "th test set");
			}
			
		} while (testSets.size() < testSetNum);

		// 3. save testSets into files
		Logger.getInstance().setPath(saveFile, false);
		Logger.getInstance().write(sb.toString());
		Logger.getInstance().close();
		return testSets;
	}

	public static Vector getAdequateTestSetsFromFile(boolean containHeader, String filePath) {
		Vector testSets = new Vector();
		try {
			BufferedReader br = new BufferedReader(new FileReader(filePath));
			String line = null;
			if(containHeader)
				br.readLine();
			
			while ((line = br.readLine()) != null) {
				Vector testSet = new Vector();
				
				//2009-02-27:
				String str = line.substring(line.indexOf("[")+"[".length(), line.indexOf("]"));
				String[] testCaseIndex =str.split(",");
				for(int i = 0; i < testCaseIndex.length; i ++)
					testSet.add(testCaseIndex[i].trim());
				
//				line = line.substring(0, line.indexOf("time:"));
//				int index = line.indexOf("\t");
//				while (index > -1) {
//					String testcase = line.substring(0, index);
//					testSet.add(testcase);
//					line = line.substring(index + "\t".length());
//					index = line.indexOf("\t");
//				}
				testSets.add(testSet);
			}
			br.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return testSets;
	}

	public static Vector generateAdequateTestSet_fixSize(String criteriaFile, String randomOrCriteria, 
			int testSuiteSize, int maxTrial){
		Vector testSet = new Vector();
		long start = System.currentTimeMillis();
		// 1.load all drivers for a specified criteria
		Manipulator manager = Manipulator.getInstance(criteriaFile);
		Vector drivers = manager.getAllUncoveredDrivers();
		Vector leftDrivers;

		int trial = 0;
		do {
			trial ++;
			// 2. random select a test case which is not in testSet
			int testCaseIndex = TestSetManager.getTestCaseIndex(testSet);
			ContextStream testCase = (ContextStream) TestSetManager.testPool.get(""+testCaseIndex);

			// 3.feed test case to SUT: faulty version index(must be golden
			// version 0) + test case index
			StringBuilder sb = new StringBuilder();
			for(int i = 0; i < testCase.eventSequence.size(); i++){
				sb.append(((String)testCase.eventSequence.get(i)).trim()+"\t");
			}
			
			PositionIButton.getInstance().set(0, testCaseIndex, sb.toString());
			PositionIButton.getInstance().runTestCase();
			PositionIButton.getInstance().stopRunning();

			// 4.judge whether this test case increases the coverage
			leftDrivers = manager.getAllUncoveredDrivers();
			if (leftDrivers.size() < drivers.size()
					&& !testSet.contains(testCaseIndex)) {
				// testSet.add(testCase);
				// 2009/1/18: it is better to keep index instead of contents
				testSet.add(testCaseIndex);
			}

			// 5.re-initial the Manipulator
			drivers = leftDrivers;
			manager.setDrivers(drivers);
		} while (leftDrivers.size() > 0 && trial < maxTrial);
		
		
		if(randomOrCriteria.equals("random")){
			while(testSet.size() < testSuiteSize){
				int testCaseIndex =  TestSetManager.getTestCaseIndex(testSet);
				if(!testSet.contains(testCaseIndex))
					testSet.add(testCaseIndex);
				
				if(drivers.size() > 0){ //still some drivers not been covered
					ContextStream testCase = (ContextStream) TestSetManager.testPool.get(""+testCaseIndex);

					// 3.feed test case to SUT: faulty version index(must be golden
					// version 0) + test case index
					StringBuilder sb = new StringBuilder();
					for(int i = 0; i < testCase.eventSequence.size(); i++){
						sb.append(((String)testCase.eventSequence.get(i)).trim()+"\t");
					}
					
					PositionIButton.getInstance().set(0, testCaseIndex, sb.toString());
					PositionIButton.getInstance().runTestCase();
					PositionIButton.getInstance().stopRunning();

					drivers = manager.getAllUncoveredDrivers();
				}
			}
		}else if(randomOrCriteria.equals("criteria")){
			
		}
		
		long enduration = System.currentTimeMillis() - start;
		System.err.println("Get an adequate test set (time: " + enduration
				+ "):" + "\n" + TestSetManager.toString(testSet));
		return testSet;
		
	}
	
	public static Vector generateAdequateTestSet(String criteriaFile) {
		// 2009/1/17:
		Vector testSet = new Vector();
		long start = System.currentTimeMillis();
		// 1.load all drivers for a specified criteria
		Manipulator manager = Manipulator.getInstance(criteriaFile);
		Vector drivers = manager.getAllUncoveredDrivers();
		Vector leftDrivers;

		do {
			// 2. random select a test case which is not in testSet
			int testCaseIndex = TestSetManager.getTestCaseIndex(testSet);
			// int testCaseIndex = 0;
			ContextStream testCase = (ContextStream) TestSetManager.testPool.get(""+testCaseIndex);

			// 3.feed test case to SUT: faulty version index(must be golden
			// version 0) + test case index
			StringBuilder sb = new StringBuilder();
			for(int i = 0; i < testCase.eventSequence.size(); i++){
				sb.append(((String)testCase.eventSequence.get(i)).trim()+"\t");
			}
			
			PositionIButton.getInstance().set(0, testCaseIndex, sb.toString());
			PositionIButton.getInstance().runTestCase();
			PositionIButton.getInstance().stopRunning();

			// 4.judge whether this test case increases the coverage
			leftDrivers = manager.getAllUncoveredDrivers();
			if (leftDrivers.size() < drivers.size()
					&& !testSet.contains(testCaseIndex)) {
				// testSet.add(testCase);
				// 2009/1/18: it is better to keep index instead of contents
				testSet.add(testCaseIndex);
			}

			// 5.re-initial the Manipulator
			drivers = leftDrivers;
			manager.setDrivers(drivers);
		} while (leftDrivers.size() > 0);// 6.feed more test cases if there
		// are some uncovered drivers
		long enduration = System.currentTimeMillis() - start;
		System.err.println("Get an adequate test set (time: " + enduration
				+ "):" + "\n" + TestSetManager.toString(testSet));
		return testSet;
	}

	public static String getTestCaseInstance(int testCaseIndex) {	
		ContextStream cs =((ContextStream)TestSetManager.testPool.get(testCaseIndex));
		
		StringBuilder sb = new StringBuilder();
		for(int i =0; i < cs.length; i ++){
			sb.append(cs.eventSequence.get(i) + "\t");
		}
		return sb.toString();
	}

	
	public static Hashtable generateTestPool(Vector events) {
		Hashtable testPool = new Hashtable();
		while (testPool.size() < SIZE_TESTPOOL) {
			// generate a test case---a context stream
			Vector eventSequences = TestSetManager.generateEventSequence(events);
			int i = 0;
			for(; i < testPool.size(); i ++){
				if(((ContextStream)testPool.get(i)).equalTo(eventSequences))
					break;
			}
			if(i ==  testPool.size()){
				// add it to the test pool if not duplicated
				ContextStream cs = new ContextStream(testPool.size(), eventSequences.size(), TestSetManager.getCI(eventSequences), eventSequences);
				testPool.put(cs.ID, cs);
			}
		}
		return testPool;
	}

	/**
	 * generate a test pool whose intensities distributed evenly from 0 to 1
	 * 
	 * @param events
	 * @return
	 */
	public static Vector generateRestrictedTestPool(Vector events) {
		Random diffRand = new Random();
		Random chooseRand = new Random();

		Vector testPool = new Vector();
		while (testPool.size() < SIZE_TESTPOOL) {
			// generate a test case
			String testCase = TestSetManager.generateRestrictedTestCase(events, diffRand,
					chooseRand);
			// add it to the test pool if not duplicated
			if (!testPool.contains(testCase)) {

				double intensity = TestSetManager.analysisIntensity(testCase);
				String completeTS = "intensity:\t" + intensity + "\t"
						+ testCase + "\n";
				// testCase += "\t" + "intensity:\t" + intensity + "\n";
				if (intensity <= 0.0 && intensity >= 0.0) {
					// testPool.add(testCase);
					testPool.add(completeTS);
				}
			}
		}
		return testPool;
	}

	public static String generateRestrictedTestCase(Vector events, Random diffRand,
			Random chooseRand) {

		ArrayList testCase = new ArrayList();

		Random rand = new Random();
		int length_TS;

		if (FIX_LENGTH_TESTCASE > 0) {
			length_TS = FIX_LENGTH_TESTCASE;
		} else {
			// length of test cases:[MIN_LENGTH_TESTCASE, MAX_LENGTH_TESTCASE]
			do {
				length_TS = rand.nextInt(TestSetManager.MAX_LENGTH_TESTCASE);
			} while (length_TS < MIN_LENGTH_TESTCASE);
		}

		testCase.add((String) events.get(0));

		// for (int i = 0; i < events.size(); i++) {
		// testCase.add((String)events.get(i));
		// }
		//
		length_TS = length_TS - testCase.size();
		for (int i = 0; i < length_TS; i++) {
			String latestEvent = (String) testCase.get(testCase.size() - 1);
			if (diffRand.nextBoolean()) {// different
				testCase.add(latestEvent);

			} else { // duplicate
				String nextEvent;
				do {
					int index = chooseRand.nextInt(events.size());
					nextEvent = (String) events.get(index);
				} while (nextEvent.equals(latestEvent));
				testCase.add(nextEvent);
			}
		}

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < testCase.size(); i++) {
			sb.append((String) testCase.get(i) + "\t");
		}

		return sb.toString();
	}

	public static String generateRestrictedTestCase(Vector events) {
		StringBuilder sb = new StringBuilder();
		Random rand = new Random();
		int length_TS;

		// length of test cases:[MIN_LENGTH_TESTCASE, MAX_LENGTH_TESTCASE]
		do {
			length_TS = rand.nextInt(TestSetManager.MAX_LENGTH_TESTCASE);
		} while (length_TS < MIN_LENGTH_TESTCASE);

		for (int i = 0; i < events.size(); i++) {
			sb.append((String) events.get(i) + "\t");
		}

		length_TS = length_TS - events.size();
		for (int i = 0; i < length_TS - 1; i++) {
			int event_index = rand.nextInt(events.size());
			sb.append((String) events.get(event_index) + "\t");
		}
		sb.append((String) events.get(rand.nextInt(events.size())));
		return sb.toString();
	}

	public static Vector generateEventSequence(Vector events) {
		Vector eventSequences = new Vector();
		
		Random rand = new Random();

		// length of test cases:[MIN_LENGTH_TESTCASE, MAX_LENGTH_TESTCASE]
		int length_TS = (Math.abs(rand.nextInt()))
				% (MAX_LENGTH_TESTCASE - MIN_LENGTH_TESTCASE)
				+ MIN_LENGTH_TESTCASE;
		
		eventSequences.add((String)events.get(0));
		for (int i = 0; i < length_TS - 1; i++) {
			int event_index = rand.nextInt(events.size());
			eventSequences.add((String)events.get(event_index));
		}

		return eventSequences;
	}

	/**
	 * get a test case randomly from a test pool which is different from testSet
	 * 
	 * @param testSet
	 * @return
	 */
	public static String getTestCase(Vector testSet) {
		String testCase;
		do {
			testCase = (String) testPool.get((new Random()).nextInt(testPool
					.size()));
		} while (testSet.contains(testCase));

		return testCase;
	}

	/**
	 * randomly get a test case which is not contained in testSet
	 * 
	 * @param testSet
	 * @return
	 */
	public static int getTestCaseIndex(Vector testSet) {
		int testCaseIndex;
		ContextStream testCase;
		Random rand = new Random();

		do {
			testCaseIndex = rand.nextInt(testPool.size());
			testCase = (ContextStream) testPool.get(testCaseIndex);
		} while (testSet.contains(testCase));

		return testCaseIndex;
	}

	public static  void saveTestArtifacts(Hashtable data, String saveFile){
		StringBuilder sb = new StringBuilder();
		sb.append("TestCase" + "\t" + "Length" + "\t" + "CI" + "\t" + "Sequences" + "\n");
		
		Iterator ite = data.values().iterator();
		while(ite.hasNext()){
			ContextStream cs = (ContextStream)ite.next();
			sb.append(cs.ID + "\t" + cs.length + "\t" + cs.CI + "\t" + cs.eventSequence + "\n");
		}
		
		Logger.getInstance().setPath(saveFile, false);
		Logger.getInstance().write(sb.toString());
		Logger.getInstance().close();
	}
	
	public static void saveTestArtifacts(String path, Vector data) {
		Logger.getInstance().setPath(path, false);
		Logger.getInstance().write(TestSetManager.toString(data));
		Logger.getInstance().close();
	}

	private static String toString(Vector data) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < data.size(); i++) {
			if (data.get(i) instanceof Vector) {
				sb.append(TestSetManager.toString((Vector) data.get(i)));
			} else {
				sb.append(data.get(i) + "\t");
			}
		}
		return sb.toString();
	}

	public static Hashtable loadTestPoolFromFile(boolean containHeader, String testPoolFile){
		Hashtable testpool = new Hashtable();
		
		try {
			BufferedReader br = new BufferedReader(new FileReader(testPoolFile));
			if(containHeader)
				br.readLine();
			String str = null;
			while((str = br.readLine())!= null){
				String[] strs = str.split("\t");
				
				int ID = Integer.parseInt(strs[0]);
				int length = Integer.parseInt(strs[1]);
				double CI = Double.parseDouble(strs[2]);
				String[] events =  strs[3].substring(strs[3].indexOf("[")+"[".length(), strs[3].indexOf("]")).split(",");			
				ContextStream cs = new ContextStream(ID, length, CI, events);
				testpool.put(""+cs.ID, cs);
			}
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return testpool;
	}
	
//	public void loadTestPoolFromFile(String t estPoolFile) {
//		try {
//			BufferedReader br = new BufferedReader(new FileReader(testPoolFile));
//			String testCase = null;
//			this.testPool = new Hashtable();
//			while ((testCase = br.readLine()) != null) {
//				testCase = testCase.substring(0, testCase.indexOf("intensity"));
//				this.testPool.add(testCase);
//			}
//		} catch (FileNotFoundException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	}

	public static void main(String[] args) {

		System.out.println("USAGE:<date(20090226)><criteria(CA, Stoc1, Stoc1FromCapp)><testSetNum(100)>" +
				"<TestSuiteSize(4)><oldOrNew(old, new)><randomOrCriteria(random, Criteria)>");
		int maxTrial = 100;
		
		
		String date = "20090226";
		String criteria = "CA";
		int testSetNum = NUMBER_TESTSET;
		int testSuiteSize = 0;
		String oldOrNew = "old";
		String randomOrCriteria = "random";
		if(args.length== 6){
			date = args[0];
			criteria = args[1];
			testSetNum = Integer.parseInt(args[2]);
			testSuiteSize = Integer.parseInt(args[3]);
			oldOrNew = args[4];
			randomOrCriteria = args[5];
		}

		Vector events = new Vector();
		events.add(WTourRegistration.UPDATE);
		events.add(WTourDemo.INTEREST);
		events.add(WTourDemo.VISIT);
		events.add(WTourEnd.END);

//		TestSetManager manager = new TestSetManager();

		String testPoolFile = "ContextIntensity/" + date + "/TestPool.txt";

		// 1. [option]generate test pools, each test case is attached with CI
//		TestSetManager.testPool = TestSetManager.generateTestPool(events);
//		TestSetManager.saveTestArtifacts(TestSetManager.testPool, testPoolFile);
		
		// manager.generateRestrictedTestPool(events);
//		manager.tranverseData(testPoolFile, testPoolFile.substring(0,
//				testPoolFile.indexOf(".txt"))
//				+ "_b.txt");

		// 2. [mandatory]load test pools from files
		boolean containHeader = true;
		TestSetManager.testPool = TestSetManager.loadTestPoolFromFile(containHeader, testPoolFile);

		// 3. [mandatory]get adequacy test sets
		String driverFile = "ContextIntensity/" + date + "/Drivers_" + criteria
				+ ".txt";
		
		String saveFile = null;
		if(testSuiteSize < 0 ){ 
			// do not fix the test suite size
			saveFile = "ContextIntensity/" +  date + "/" + criteria
			+ "TestSets_"+oldOrNew+".txt";
		}else{
			saveFile = "ContextIntensity/" + date + "/" + criteria
			+"TestSets_"+ oldOrNew+"_"+ randomOrCriteria + "_"+ testSuiteSize + ".txt";
		}

		//4. [mandatory]generate and save adequate test sets
		Vector testSets = TestSetManager.generateAllTestSetsAndSave(testSetNum,
				 driverFile, saveFile);
		
		//5. [mandatory]attach test sets with CI
		containHeader = false;
		TestSetManager.attachTSWithCI(containHeader, saveFile, saveFile.substring(0, saveFile.indexOf(".txt"))+"_CI.txt");
		System.exit(0);
		
		// manager.generateAdequateTestSet(driverFile);
		// manager.generateAllTestSets(driverFile);
		// manager.generateAllTestSetsAndSave(Constant.baseFolder
		// + "ContextIntensity/Drivers/Drivers_Stoc1.txt",
		// Constant.baseFolder
		// + "ContextIntensity/AdequateTestSet/TestSet_Stoc1.txt");

		// manager.analysisTestSets("3", saveFile, saveFile);

		// Vector testSets =
		// manager.getAdequateTestSetsFromFile(Constant.baseFolder
		// + "ContextIntensity/AdequateTestSet/TestSet_CA.txt");
		// System.out.println();

		// String str_testPool = manager.toString(manager.testPool);
		// String str_testPool = manager.toString(all);

		// System.out.println(str_testPool);

		// .2009/2/14:4.[option]interpret context intensity of test sets
		// if(args.length != 0){
		// criteria = args[0];
		// testSetFile = Constant.baseFolder
		// +"ContextIntensity/AdequateTestSet/TestSet_"+criteria + ".txt";
		// saveFile = Constant.baseFolder +
		// "ContextIntensity/AdequateTestSet/TestSet_" + criteria + "_CI.txt";
		// }
		//	
		
//		TestSetManager.attachTSWithCI(saveFile, saveFile);
//		System.exit(0);
	}
}
