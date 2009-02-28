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
import java.util.Random;
import java.util.Vector;

import ccr.test.TestCase;

import context.arch.generator.PositionIButton;
import context.arch.widget.WTourDemo;
import context.arch.widget.WTourEnd;
import context.arch.widget.WTourRegistration;
import context.test.contextIntensity.Manipulator;
import context.test.util.Constant;
import context.test.util.Logger;

public class TestSetManager {

	public Vector testPool; // event sequences
	public Vector testSets; // test case index
	public int SIZE_TESTPOOL = 1000;
	public int NUMBER_TESTSET = 100; //2009/1/19: this number must be big enough to get equal-sized test sets
	public int MAX_LENGTH_TESTCASE = 20;
	public int MIN_LENGTH_TESTCASE = 4;
	public int FIX_LENGTH_TESTCASE = -19;
	public HashMap intensities; //test case index(String)->CI(String)
	
	public void generateAllTestSets(String criteriaFile, int maxTrial) {
		this.testSets = new Vector();
		do {
			Vector testSet = this.generateAdequateTestSet(criteriaFile, maxTrial);
			// testSets.add(testSet);
			if (!testSets.contains(testSet)) {
				testSets.add(testSet);
			}
		} while (testSets.size() < NUMBER_TESTSET);
	}
	
	
	/**2009-02-27:interprete the CI of adequate test sets
	 * 
	 * @param containHeader
	 * @param testSetFile
	 * @param saveFile
	 */
		public void attachTSWithCI(boolean containHeader, String testSetFile, String saveFile) {
			try {
				BufferedReader br = new BufferedReader(new FileReader(testSetFile));

				String str = null;
				StringBuilder sb = new StringBuilder();
				if(containHeader)
					br.readLine();
				
				while ((str = br.readLine()) != null) {
					String temp = str;
					
					String[] testCases = temp.substring(
							0, temp.indexOf("time:"))
							.split("\t");
					
					String events = temp.substring(0, temp.indexOf("time:")).trim();
					String time = temp.substring(temp.indexOf("time:")+"time:".length()).trim();
					
					
					double sumIntensity = 0;
					for (String testCaseIndex : testCases) {
						sumIntensity += this.getIntensity(testCaseIndex);
					}
					double averageIntensity = sumIntensity / (double)testCases.length;
					sb.append("Length:\t"+ testCases.length+"\tCI:\t" + averageIntensity + "\ttime:\t" +time+"\t"+ events+ "\n");
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
			
			
		}
	
	/**select same sized test sets to be a candidate
	 * 
	 * @param size: the size of candidate test set
	 * @param testSetFile
	 * @param saveFile: a record in this file: time  + event sequences + intensity of test set
	 */
	public void analysisTestSets(String size_TS, String testSetFile, String saveFile){
		try {
			BufferedReader br = new BufferedReader(new FileReader(testSetFile));
			String line = null;
			StringBuilder sb = new StringBuilder();
			while((line = br.readLine())!= null){
				//line = time + size + event sequence
				String[] strs = line.split("\t");
				String size = strs[3];
				if(size.equals(size_TS)){					
					sb.append(strs[0] + "\t" + strs[1] + "\t" );
					
					double sumIntensity = 0; 
					for(int i = 0; i < Integer.parseInt(size); i ++ ){
						String testCaseIndex = strs[i + 4];
						sumIntensity += this.getIntensity(testCaseIndex);
						sb.append(testCaseIndex + "\t");
					}
					double averageIntensity = sumIntensity / Integer.parseInt(size);
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

	public double getIntensity(String testCaseIndex){
		if(this.intensities == null){
			String testPoolFile = Constant.baseFolder
				+ "ContextIntensity/TestPool.txt";
			this.getIntensities(testPoolFile);
		}
		return Double.parseDouble((String)this.intensities.get(testCaseIndex));
	}
	
	private void tranverseData(String filePath, String savePath){
		try {
			BufferedReader br = new BufferedReader(new FileReader(filePath));
			String line = null;
		
			Vector testPool = new Vector();
			
			StringBuffer sb = new StringBuffer();
			while((line = br.readLine())!= null){
				String[] flags = line.split("\t");
				for(int i = 2; i < flags.length; i ++){
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
	
	public double analysisIntensity(String testCaseInstance){
		Vector eventSequences = new Vector();
		int index = testCaseInstance.indexOf("\t");
		while(index > -1){
			String event = testCaseInstance.substring(0, index);
			eventSequences.add(event);
			testCaseInstance = testCaseInstance.substring(index + "\t".length());
			index = testCaseInstance.indexOf("\t");
		}
		if(testCaseInstance.length() >0){
			eventSequences.add(testCaseInstance);
		}
		if(eventSequences.size() == 1){
			return 0;
		}else{
			double HammingDistance = 0;
			for(int i = 0; i < eventSequences.size() - 1; i ++){
				String before = (String)eventSequences.get(i);
				String after = (String)eventSequences.get(i +1);
				if(!before.equals(after)){
					HammingDistance ++;
				}
			}
			return HammingDistance/(eventSequences.size()-1);	
		}
	}
	
	public void getIntensities(String testPoolFile){
		this.intensities = new HashMap();
		if(testPool==null){
			this.loadTestPoolFromFile(testPoolFile);
		}
		
		for(int i = 0; i < testPool.size(); i ++){
			String testCaseInstance = (String)testPool.get(i);
			intensities.put(""+ i, ""+this.analysisIntensity(testCaseInstance));
		}
		
	}
	

	
	public void generateAllTestSetsAndSave(String criteriaFile, String savePath, int maxTrial) {

		StringBuilder sb = new StringBuilder();
		this.testSets = new Vector();

		do {
			long start = System.currentTimeMillis();
			Vector testSet = this.generateAdequateTestSet(criteriaFile, maxTrial);

			long enduration = System.currentTimeMillis() - start;

			// 1. judge whether duplicated
			if (!testSets.contains(testSet)) {
				// 2. 2009/1/19:save test case(change the format)
//				sb.append("time:\t" + enduration + "\t");
//				sb.append("size:\t" + testSet.size() + "\t");
//				for (int i = 0; i < testSet.size(); i++) {
//					sb.append(testSet.get(i) + "\t");
//				}
//				sb.append("\n");
				
				// 2. 2009/2/13:save test case(change the format)							
				for (int i = 0; i < testSet.size(); i++) {
					sb.append(testSet.get(i) + "\t");
				}
				sb.append("time:\t" + enduration + "\t");
				sb.append("\n");
				
				testSets.add(testSet);
			}
			System.out.println(testSets.size() + "th test set");
		} while (testSets.size() < NUMBER_TESTSET);

		// 3. save testSets into files
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(savePath,
					false));
			bw.write(sb.toString());
			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public Vector getAdequateTestSetsFromFile(String filePath){
		Vector testSets = new Vector();
		try {
			BufferedReader br = new BufferedReader(new FileReader(filePath));
			String line = null;
			while((line = br.readLine())!=null){
				Vector testSet = new Vector();
				
				line = line.substring(0, line.indexOf("time:"));
				int index = line.indexOf("\t");
				while(index > -1){
					String testcase = line.substring(0, index);
					testSet.add(testcase);
					line = line.substring(index + "\t".length());
					index = line.indexOf("\t");
				}
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
	
	public Vector generateAdequateTestSet_refined(String criteriaFile, int maxTrial){
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
			int testCaseIndex = this.getTestCaseIndex(testSet);
			// int testCaseIndex = 0;
			String testCase = (String) this.testPool.get(testCaseIndex);
			
			PositionIButton.getInstance().set(0, testCaseIndex, testCase);
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
				double testCase_CI = (Double) this.intensities.get(""+testCaseIndex);
				//do not increase the coverage, replace the one has the lowest CI values
				Vector replaced = new Vector();
				for(int i = 0; i < testSet.size(); i ++){
					
					int temp = (Integer)testSet.get(i);
					double temp_CI = this.getIntensity(""+temp);
					
					if(temp_CI < testCase_CI){//add "temp" since it has a lower CI value
						if (replaced.size() > 0) {
							int j = 0;
							boolean added = false;
							for (; j < replaced.size(); j++) {
								int replacedTC = (Integer) replaced
										.get(j);
								double CI_replacedTC = this.getIntensity(""+replacedTC);
								if (CI_replacedTC > temp_CI) {
									// it is a correct place to insert
									// current test case(ascending order):
									// temp
									replaced.add(j, temp);
									added = true;
									break;
								}
							}
							if (!added) { // add it to the end if it has not been add
								replaced.add(temp);
							}
						} else {
							replaced.add(temp);
						}
					}
				}
				
				if(replaced.size() > 0){
					int testCase_replace =  (Integer)replaced.get(0);
					testSet.remove(testCase_replace);
					testSet.add(testCaseIndex);
				}
			}

			// 5.re-initial the Manipulator
			drivers = leftDrivers;
			manager.setDrivers(drivers);
		} while (leftDrivers.size() > 0 ||trial < maxTrial);// 6.feed more test cases if there
		// are some uncovered drivers
		long enduration = System.currentTimeMillis() - start;
		System.err.println("Get an adequate test set (time: " + enduration
				+ "):" + "\n" + this.toString(testSet) + " replaced:" + replaceCounter);
		return testSet;
	}
	
	public Vector generateAdequateTestSet_fixedSize(String criteriaFile, 
			int testSuiteSize, String randomOrCriteria, int maxTrial){
		
		Vector testSet = new Vector();
		long start = System.currentTimeMillis();
		// 1.load all drivers for a specified criteria
		Manipulator manager = Manipulator.getInstance(criteriaFile);
		Vector drivers = manager.getAllUncoveredDrivers();
		Vector leftDrivers;

		int trial = 0;
		do {
			trial ++;
			// 2. random select a test case
			int testCaseIndex = this.getTestCaseIndex(testSet);
			// int testCaseIndex = 0;
			String testCase = (String) this.testPool.get(testCaseIndex);

			// 3.feed test case to SUT: faulty version index(must be golden
			// version 0) + test case index
			PositionIButton.getInstance().set(0, testCaseIndex, testCase);
			PositionIButton.getInstance().runTestCase();
			PositionIButton.getInstance().stopRunning();

			// 4.judge whether this test case increases the coverage
			leftDrivers = manager.getAllUncoveredDrivers();
			if (leftDrivers.size() < drivers.size() && !testSet.contains(testCaseIndex)) {
				// testSet.add(testCase);
				// 2009/1/18: it is better to keep index instead of contents
				testSet.add(testCaseIndex);
			}

			// 5.re-initial the Manipulator
			drivers = leftDrivers;
			manager.setDrivers(drivers);
		} while (leftDrivers.size() > 0 && trial < maxTrial);
		// 6.feed more test cases if there are some uncovered drivers
		
		Vector finalDrivers = leftDrivers;
		if(randomOrCriteria.equals("criteria")){
			while(trial < maxTrial &&  testSet.size()<testSuiteSize){
				
				//reinitialize the drivers
				if(leftDrivers.size() ==0){
					manager = Manipulator.getInstance(criteriaFile);
					drivers = manager.getAllUncoveredDrivers();
					finalDrivers = leftDrivers;
				}else{
					drivers = manager.getAllUncoveredDrivers();
				}
				
				manager.setDrivers(drivers);
				do {
					trial ++;
					// 2. random select a test case
					int testCaseIndex = this.getTestCaseIndex(testSet);
					// int testCaseIndex = 0;
					String testCase = (String) this.testPool.get(testCaseIndex);

					// 3.feed test case to SUT: faulty version index(must be golden
					// version 0) + test case index
					PositionIButton.getInstance().set(0, testCaseIndex, testCase);
					PositionIButton.getInstance().runTestCase();
					PositionIButton.getInstance().stopRunning();

					// 4.judge whether this test case increases the coverage
					leftDrivers = manager.getAllUncoveredDrivers();
					if (leftDrivers.size() < drivers.size() && !testSet.contains(testCaseIndex)) {
						testSet.add(testCaseIndex);
					}

					// 5.re-initial the Manipulator
					drivers = leftDrivers;
					manager.setDrivers(drivers);
				} while (leftDrivers.size() > 0 && trial < maxTrial);
			}
		}else if(randomOrCriteria.equals("random")){
			while(trial < maxTrial && testSet.size() < testSuiteSize){
				int testCaseIndex = this.getTestCaseIndex(testSet);
				if(!testSet.contains(testCaseIndex)){
					testSet.add(testCaseIndex);
				}
				if(finalDrivers.size() > 0){
					//some drivers have not been covered
					String testCase = (String) this.testPool.get(testCaseIndex);
					PositionIButton.getInstance().set(0, testCaseIndex, testCase);
					PositionIButton.getInstance().runTestCase();
					PositionIButton.getInstance().stopRunning();
					finalDrivers = manager.getAllUncoveredDrivers();
				}
			}
		}
		
		double coverage = (double)finalDrivers.size()/((double)Manipulator.getInstance(criteriaFile).getAllUncoveredDrivers().size());
		
		long enduration = System.currentTimeMillis() - start;
		System.err.println("Get an adequate test set (time: " + enduration
				+ "):" + "\n" + this.toString(testSet));
		return testSet;
		
				
	}
	
	public Vector generateAdequateTestSet(String criteriaFile, int maxTrial) {
		// 2009/1/17:

		Vector testSet = new Vector();
		long start = System.currentTimeMillis();
		// 1.load all drivers for a specified criteria
		Manipulator manager = Manipulator.getInstance(criteriaFile);
		Vector drivers = manager.getAllUncoveredDrivers();
		Vector leftDrivers;

		int trial = 0;
		do {
			trial ++;
			// 2. random select a test case
			int testCaseIndex = this.getTestCaseIndex(testSet);
			// int testCaseIndex = 0;
			String testCase = (String) this.testPool.get(testCaseIndex);

			// 3.feed test case to SUT: faulty version index(must be golden
			// version 0) + test case index
			PositionIButton.getInstance().set(0, testCaseIndex, testCase);
			PositionIButton.getInstance().runTestCase();
			PositionIButton.getInstance().stopRunning();

			// 4.judge whether this test case increases the coverage
			leftDrivers = manager.getAllUncoveredDrivers();
			if (leftDrivers.size() < drivers.size() && !testSet.contains(testCaseIndex)) {
				// testSet.add(testCase);
				// 2009/1/18: it is better to keep index instead of contents
				testSet.add(testCaseIndex);
			}

			// 5.re-initial the Manipulator
			drivers = leftDrivers;
			manager.setDrivers(drivers);
		} while (leftDrivers.size() > 0 && trial < maxTrial);// 6.feed more test cases if there
											// are some uncovered drivers
		
		
		double coverage = (double)leftDrivers.size()/(double)Manipulator.getInstance(criteriaFile).getAllUncoveredDrivers().size();
		
		long enduration = System.currentTimeMillis() - start;
		System.err.println("Get an adequate test set (time: " + enduration
				+ "):" + "\n" + this.toString(testSet));
		return testSet;
	}
	
	public String getTestCaseInstance(int testCaseIndex){
		if(this.testPool==null){
			this.loadTestPoolFromFile(Constant.baseFolder
					+ "ContextIntensity/TestPool.txt");
		}
		return (String)this.testPool.get(testCaseIndex);
	}
	
	public Vector generateTestPool(Vector events) {
		testPool = new Vector();
		while (testPool.size() < SIZE_TESTPOOL) {
			// generate a test case
			String testCase = this.generateTestCase(events);

			// add it to the test pool if not duplicated
			
			if (!testPool.contains(testCase)) {
				double intensity = this.analysisIntensity(testCase);
				String completeTS = "intensity:\t"+ intensity + "\t"+testCase + "\n";
				testPool.add(completeTS);
			}
			
		}
		return testPool;
	}


	/**generate a test pool whose intensities distributed evenly from 0 to 1
	 * 
	 * @param events
	 * @return
	 */
	public Vector generateRestrictedTestPool(Vector events) {
		Random diffRand = new Random();
		Random chooseRand = new Random();
		
		testPool = new Vector();
		while (testPool.size() < SIZE_TESTPOOL) {
			// generate a test case
			String testCase = this.generateRestrictedTestCase(events, diffRand, chooseRand);
			// add it to the test pool if not duplicated
			if (!testPool.contains(testCase)) {
				
				double intensity = this.analysisIntensity(testCase);
				String completeTS = "intensity:\t"+ intensity + "\t"+testCase + "\n";
//				testCase += "\t" + "intensity:\t" + intensity + "\n";
				if(intensity <=0.0 && intensity >=0.0){
//					testPool.add(testCase);
					testPool.add(completeTS);
				}
			}
		}
		return testPool;
	}
	
	public String generateRestrictedTestCase(Vector events, Random diffRand, Random chooseRand){
		
		ArrayList testCase = new ArrayList();
		
		Random rand = new Random();
		int length_TS;
		
		if(FIX_LENGTH_TESTCASE > 0){
			length_TS = FIX_LENGTH_TESTCASE;
		}else{
			// length of test cases:[MIN_LENGTH_TESTCASE, MAX_LENGTH_TESTCASE]
			do {
				length_TS = rand.nextInt(this.MAX_LENGTH_TESTCASE);
			} while (length_TS < MIN_LENGTH_TESTCASE);			
		}

		testCase.add((String)events.get(0));
		
//		for (int i = 0; i < events.size(); i++) {
//			testCase.add((String)events.get(i));
//		}
//
		length_TS = length_TS - testCase.size();
		for (int i = 0; i < length_TS; i++) {
			String latestEvent =(String)testCase.get(testCase.size()-1);
			if(diffRand.nextBoolean()){//different
				testCase.add(latestEvent);
				
			}else{ //duplicate
				String nextEvent;
				do{
					int index = chooseRand.nextInt(events.size());
					nextEvent = (String)events.get(index);
				}while(nextEvent.equals(latestEvent));
				testCase.add(nextEvent);
			}
		}
		
		
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < testCase.size(); i ++){
			sb.append((String)testCase.get(i) + "\t");
		}
		
		return sb.toString();
	}

	public String generateRestrictedTestCase(Vector events) {
		StringBuilder sb = new StringBuilder();
		Random rand = new Random();
		int length_TS;

		// length of test cases:[MIN_LENGTH_TESTCASE, MAX_LENGTH_TESTCASE]
		do {
			length_TS = rand.nextInt(this.MAX_LENGTH_TESTCASE);
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

	public String generateTestCase(Vector events) {
		StringBuilder sb = new StringBuilder();
		Random rand = new Random();
		int length_TS;

		// length of test cases:[MIN_LENGTH_TESTCASE, MAX_LENGTH_TESTCASE]
		do {
			length_TS = rand.nextInt(this.MAX_LENGTH_TESTCASE);
		} while (length_TS < MIN_LENGTH_TESTCASE);

		for (int i = 0; i < length_TS -1; i++) {
			int event_index = rand.nextInt(events.size());
			sb.append((String) events.get(event_index) + "\t");
		}
		sb.append((String) events.get(rand.nextInt(events.size())));
		return sb.toString();
	}

	/**get a test case randomly from a test pool which is different from testSet
	 * 
	 * @param testSet
	 * @return
	 */
	public String getTestCase(Vector testSet) {
		String testCase;
		do {
			testCase = (String) testPool.get((new Random()).nextInt(testPool
					.size()));
		} while (testSet.contains(testCase));

		return testCase;
	}

	/**
	 * randomly get a test case which is different from any elements in testSet
	 * 
	 * @param testSet
	 * @return
	 */
	public int getTestCaseIndex(Vector testSet) {
		int testCaseIndex;
		String testCase;
		Random rand = new Random();

		do {
			testCaseIndex = rand.nextInt(testPool.size());
			testCase = (String) testPool.get(testCaseIndex);
		} while (testSet.contains(testCase));

		return testCaseIndex;
	}

	public void saveTestArtifacts(String path, Vector data) {
		Logger.getInstance().setPath(path, false);
		Logger.getInstance().write(this.toString(data));
		Logger.getInstance().close();
	}

	private String toString(Vector data) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < data.size(); i++) {
			if (data.get(i) instanceof Vector) {
				sb.append(this.toString((Vector) data.get(i)));
			} else {
				sb.append(data.get(i));
			}
		}
		return sb.toString();
	}

	public void loadTestPoolFromFile(String testPoolFile) {
		try {
			BufferedReader br = new BufferedReader(new FileReader(testPoolFile));
			String line = null;
			
			this.testPool = new Vector();
			this.intensities = new HashMap();
			
			int i = 0;
			while ((line = br.readLine()) != null) {
				
				String testCase = line.substring(0, line.indexOf("intensity:\t"));
				this.testPool.add(testCase);

				String intensity = line.substring(line.indexOf("intensity:\t")+"intensity:\t".length()); 
				//2009-02-28:also load the intensity information 
				this.intensities.put(""+i, intensity);
				i++;
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {

		int maxTrial = 100;
		String date = "20090226";
		String criteria = "CA";
		
		Vector events = new Vector();
		events.add(WTourRegistration.UPDATE);
		events.add(WTourDemo.INTEREST);
		events.add(WTourDemo.VISIT);
		events.add(WTourEnd.END);

		TestSetManager manager = new TestSetManager();

		// 1. [option]generate test pools
//		 manager.generateTestPool(events);
//		 manager.generateRestrictedTestPool(events);
		 
		 String testpoolFile = Constant.baseFolder +
		 "ContextIntensity/"+date+"/TestPool.txt";
//		 manager.saveTestArtifacts(testpoolFile, manager.testPool);

		 String traverseFile = testpoolFile.substring(0, testpoolFile.indexOf(".txt"))+"_traverse.txt";
//		 manager.tranverseData(testpoolFile, traverseFile);
		 
		// 2. [mandatory]load test pools from files
		manager.loadTestPoolFromFile(traverseFile);


		
		// 3. [mandatory]get adequacy test sets
		String driverFile = Constant.baseFolder
		+ "ContextIntensity/"+date+"/Drivers_"+criteria+".txt";
		
		String testSetFile =Constant.baseFolder
		+ "ContextIntensity/"+date+"/TestSet_"+criteria+".txt";
		
		manager.generateAllTestSetsAndSave( driverFile, testSetFile, maxTrial);

		//4.[mandatory]get CI of test sets
		boolean containHeader = false;
		String saveFile =  testSetFile.substring(0, testSetFile.indexOf(".txt")) + "_CI.txt";
		manager.attachTSWithCI(containHeader, testSetFile, saveFile);
		
//		manager.generateAllTestSetsAndSave(Constant.baseFolder
//				+ "ContextIntensity/Drivers/Drivers_Stoc1.txt",
//				Constant.baseFolder
//						+ "ContextIntensity/AdequateTestSet/TestSet_Stoc1.txt");

//		String testSetFile = "C:\\WangHuai\\Martin\\Eclipse3.3.1\\ContextToolkit_intensity\\ContextIntensity\\AdequateTestSet\\TestSet_Stoc1.txt";
//		String saveFile =  "C:\\WangHuai\\Martin\\Eclipse3.3.1\\ContextToolkit_intensity\\ContextIntensity\\AdequateTestSet\\TestSet_Stoc1_a.txt";
//		manager.analysisTestSets("3", testSetFile, saveFile);
		
//		Vector testSets = manager.getAdequateTestSetsFromFile(Constant.baseFolder
//		+ "ContextIntensity/AdequateTestSet/TestSet_CA.txt");
//System.out.println();
		
		// String str_testPool = manager.toString(manager.testPool);
		// String str_testPool = manager.toString(all);

		// System.out.println(str_testPool);
		System.exit(0);
	}
}
