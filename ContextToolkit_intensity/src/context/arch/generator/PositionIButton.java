package context.arch.generator;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.util.Date;
import java.util.HashMap;
import java.util.Vector;
import java.util.Hashtable;

import context.arch.widget.Widget;

import context.arch.widget.WTourRegistration;
import context.arch.widget.WTourDemo;
import context.arch.widget.WTourEnd;
import context.arch.widget.WDisplay;
import context.arch.widget.WidgetHandles;
import context.arch.server.STourId;
import context.apps.Tour.TourApp;

import context.arch.interpreter.IDemoRecommender;

import context.test.util.*;
import context.test.*;

/**
 * This class acts as a wrapper around a Java iButton reader.  Whenever
 * an iButton docks with the reader, the location of the reader, the
 * iButton id and the docking timestamp are stored and made available
 * to the context-aware infrastructure for polling, and can notify 
 * context widgets when data changes.
 *
 * @see com.ibutton.oc.JibMultiListener
 * @see context.arch.widget.Widget
 */
public class PositionIButton {

	private String location = "";
	private String currentid = "";
	private long currentTime = 0;

	//2008/7/9	
	private static PositionIButton sensor = null;
	private Vector widgets = new Vector();
	//private Thread runner = null;  
	private static IButtonData data = null;
	private TestCase eventSequences = null;
//	private static String serverHost;
//	private static int serverPort;
	
	private PositionIButton() {

	}

	public static PositionIButton getInstance() {
		if (sensor == null) {
			sensor = new PositionIButton();
			String currentid = toHexString(new int[] { 1, 2, 3, 4 });
			String location = "test";
			data = new IButtonData(location, currentid, Long
					.toString(new Date().getTime()));
		}
		return sensor;
	}

	public void addListensor(Widget widget) {
		widgets.add(widget);
	}
	
	
	
	private void startSimulate() {
		Widget widget = null;

		ContextEvent event = new ContextEvent(0, 1, WTourRegistration.UPDATE);

		String info = event.context;
		int duration = event.duration;

		for (int j = 0; j < widgets.size(); j++) {
			widget = (Widget) widgets.get(j);
			widget.notify(info, data);
		}
		

		event = new ContextEvent(1, 1, WTourDemo.VISIT);
		info = event.context;
		duration = event.duration;
		for (int j = 0; j < widgets.size(); j++) {
			widget = (Widget) widgets.get(j);
			widget.notify(info, data);
		}
		
		event = new ContextEvent(1, 1, WTourDemo.INTEREST);
		info = event.context;
		duration = event.duration;
		for (int j = 0; j < widgets.size(); j++) {
			widget = (Widget) widgets.get(j);
			widget.notify(info, data);
		}

		event = new ContextEvent(1, 1, WTourEnd.END);
		info = event.context;
		duration = event.duration;
		for (int j = 0; j < widgets.size(); j++) {
			widget = (Widget) widgets.get(j);
			widget.notify(info, data);
		}
	}
	
	//2009/1/16:this approach may make TourApp run sequential instead of concurrently
	//we need to send data via HTTPClient instead of invoke notify() directly
	private void startSimulate(int serverPort) {
		//2009/1/16:
		ContextEvent event = new ContextEvent(0, 1, WTourRegistration.UPDATE);
//		new TestCaseFeedThread( event.context, data, "127.0.0.1", serverPort).run();

		event = new ContextEvent(1, 1, WTourDemo.VISIT);
		new TestCaseFeedThread( event.context, data, "127.0.0.1", serverPort).run();
		
		event = new ContextEvent(1, 1, WTourDemo.INTEREST);
		new TestCaseFeedThread( event.context, data, "127.0.0.1", serverPort).run();
		
		event = new ContextEvent(1, 1, WTourEnd.END);
		new TestCaseFeedThread( event.context, data, "127.0.0.1", serverPort).run();
	}

	
	//notify widgets about every event in sequences
	public void startSampling() {
		Widget widget = null;
		for (int i = 0; i < eventSequences.length; i++) {
			ContextEvent event = (ContextEvent) eventSequences.get(i);
			String info = event.context;
			int duration = event.duration;

			for (int j = 0; j < widgets.size(); j++) {
				widget = (Widget) widgets.get(j);
				widget.notify(info, data);
			}
			/*try{
			  Thread.sleep((long)duration*1000);  			  
			}catch(Exception e){
			System.out.println(e);  
			}	*/
		}
	}

	/*//2008/7/9: a thread version
	public void quit(){	  
	  runner.stop();
	}
	 */

	public static String toHexString(int[] arr) {
		String str = "";
		for (int i = 0; i < arr.length; i++) {
			if (arr[i] < 0x10) {
				str += "0";
			}
			str += Integer.toHexString(arr[i]).toUpperCase();
		}
		return str;
	}

	public IButtonData pollData() {
		IButtonData data = new IButtonData(location, currentid, Long
				.toString(currentTime));
		return data;
	}

	public static void main(String[] args) {
		try {
//			System.out.println("PositionIButton has been executed");
			if (args.length == 1) {
				//1.retrieve test cases firstly
				TestCaseGenerator maker = new TestCaseGenerator();
				String file = Constant.baseFolder + "TestCase.txt";
				Vector testSuite = maker.retrieveTestCases(file);

				int testcaseNumber = Integer.parseInt(args[0]);
				String configFilePath = Logger.getInstance()
						.generateConfigFile(testcaseNumber);

				//2.start widgets(This can done by ant, but it will make different outputs for different test cases)		  		
				WTourRegistration tourStart = new WTourRegistration("test",
						5000 + testcaseNumber, false);

				WTourDemo tourDemo = new WTourDemo("test",
						6000 + testcaseNumber, "http://127.0.0.1:"
								+ (6000 + testcaseNumber) + "/"
								+ configFilePath,/*"C:/WangHuai/Martin/Eclipse3.3/eclipse/ICSE'09/ConfigFile.txt"*/
						"file:///" + System.getProperty("user.dir")
								+ "/DemoInfoFile.txt", false);

				WTourEnd tourEnd = new WTourEnd("test", 7000 + testcaseNumber,
						false);

				IDemoRecommender recommender = new IDemoRecommender(
						8000 + testcaseNumber);

				WDisplay display = new WDisplay("test", 9000 + testcaseNumber,
						"100", "200", "graphics", false);

				STourId server = new STourId(10000 + testcaseNumber,
						"01020304", new WidgetHandles(), "http://127.0.0.1:"
								+ (5000 + testcaseNumber) + "/"
								+ configFilePath);

				TourApp tour = new TourApp(11000 + testcaseNumber, "01020304",
						"http://127.0.0.1:" + (5000 + testcaseNumber) + "/"
								+ configFilePath, "file:///"
								+ System.getProperty("user.dir")
								+ "/DemoInfoFile.txt");

				//3.start to simulate event sequences			  
				TestCase testCase = (TestCase) testSuite.get(testcaseNumber);
				sensor = PositionIButton.getInstance();
				sensor.eventSequences = testCase;
				sensor.startSampling();
				//sensor.startSimulate();

				//4.stop widgets		  	
				tourStart.quit();
				tourDemo.quit();
				tourEnd.quit();
				recommender.quit();
				display.quit();
				server.quit();
				tour.quit();
			} else if (args.length == 2) {
				int versionNumber = Integer.parseInt(args[0]);
				int testCaseNumber = Integer.parseInt(args[1]);

				//2.start widgets(This can done by ant, but it will make different outputs for different test cases)
				int port_Registration = PositionIButton.getAvailablePort();
				int port_End = PositionIButton.getAvailablePort();
				int port_Demo = PositionIButton.getAvailablePort();
				int port_Recommender = PositionIButton.getAvailablePort();
				int port_Display = PositionIButton.getAvailablePort();
				int port_IDServer = PositionIButton.getAvailablePort();
				int port_App = PositionIButton.getAvailablePort();
				HashMap map = new HashMap();
				map.put("TourRegistration", port_Registration);
				map.put("TourDemo", port_Demo);
				map.put("TourEnd", port_End);
				map.put("IDServer", port_IDServer);
				map.put("TourApp", port_App);
				map.put("DisplayServer", port_Display);
				map.put("RecommenderServer", port_Recommender);
				
				//2009/1/16: 
				
				
				String configFilePath = Logger.getInstance()
						.generateConfigFile(testCaseNumber, map);

				WTourRegistration tourStart = new WTourRegistration("test",
						port_Registration, false);

				WTourDemo tourDemo = new WTourDemo("test", port_Demo,
						"http://127.0.0.1:" + port_Demo + "/" + configFilePath,
						"file:///" + System.getProperty("user.dir")
								+ "/DemoInfoFile.txt", false);

				WTourEnd tourEnd = new WTourEnd("test", port_End, false);

				IDemoRecommender recommender = new IDemoRecommender(
						port_Recommender);

				WDisplay display = new WDisplay("test", port_Display, "100",
						"200", "graphics", false);

				STourId server = new STourId(port_IDServer, "01020304",
						new WidgetHandles(), "http://127.0.0.1:"
								+ port_Registration + "/" + configFilePath);

				//2009/1/14:use reflection to initialize an object
				String className = null;
				if (versionNumber == 0) { //invoke the golden version
					className = "context.apps.Tour.TourApp";
				} else {
					className = "context.apps.Tour.mutants.TourApp_"
							+ versionNumber;
				}
				Class obj = Class.forName(className);
				Class[] types = new Class[4];
				types[0] = new Integer(port_App).TYPE;
				types[1] = "01020304".getClass();
				types[2] = ("http://127.0.0.1:" + port_Registration + "/" + configFilePath)
						.getClass();
				types[3] = ("file:///" + System.getProperty("user.dir") + "/DemoInfoFile.txt")
						.getClass();

				Object[] values = new Object[4];
				values[0] = port_App;
				values[1] = "01020304";
				values[2] = "http://127.0.0.1:" + port_Registration + "/"
						+ configFilePath;
				values[3] = "file:///" + System.getProperty("user.dir")
						+ "/DemoInfoFile.txt";
				Object tour = obj.getConstructor(types).newInstance(values);
				
				//3.start to simulate event sequences
				TestCaseGenerator maker = new TestCaseGenerator();
				String file = Constant.baseFolder + "TestCase.txt";
				Vector testSuite = maker.retrieveTestCases(file);
				TestCase testCase = (TestCase) testSuite.get(testCaseNumber);
				sensor = PositionIButton.getInstance();
				sensor.eventSequences = testCase;
//				sensor.startSampling();
				//2009/1/16:used to generate test cases 
				sensor.startSimulate(port_IDServer);
				
				
				//4.stop widgets	
				tourStart.quit();
				tourDemo.quit();
				tourEnd.quit();
				recommender.quit();
				display.quit();
				server.quit();

				// 2009/1/14: use reflection to quit the method
//				Object tour = obj.getConstructor(types).newInstance(values);
				Method quitMethod = obj.getMethod("quit", null);
				quitMethod.invoke(tour, null);

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static int getAvailablePort() {

		int port = -1;
		try {

			ServerSocket socket = new ServerSocket(0); // A port of 0 creates a
			// socket on any free port
			port = socket.getLocalPort();
			socket.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return port;

	}

}
