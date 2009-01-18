package context.arch.generator;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
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

import context.test.contextIntensity.Manipulator;
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
	private static IButtonData data = null;
	private TestCase eventSequences = null;
	private int mutantVersion;
	private int testCaseIndex;
	private String testCase;
	
	//2009/1/18: used to stop these servers
	private WTourRegistration tourStart;
	private WTourDemo tourDemo;
	private WTourEnd tourEnd;
	private IDemoRecommender recommender;
	private WDisplay display;
	private STourId server;
	private Class obj;
	private Object tour;
	
	//2009/1/17: we need no Ant to do experiments
	public void set(int versionNumber, int testCaseNumber, String testCaseInstance){
		this.mutantVersion = versionNumber;
		this.testCaseIndex = testCaseNumber;
		this.testCase = testCaseInstance;
	}
	
	//2009/1/17: by this way, we can get adequate test sets without Ant
	public void runTestCase(){
		int versionNumber = this.mutantVersion;
		int testCaseNumber = this.testCaseIndex;
		String testCaseInstance = this.testCase;
		
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

		tourStart = new WTourRegistration("test",
				port_Registration, false);

		tourDemo = new WTourDemo("test", port_Demo,
				"http://127.0.0.1:" + port_Demo + "/" + configFilePath,
				"file:///" + System.getProperty("user.dir")
						+ "/DemoInfoFile.txt", false);

		tourEnd = new WTourEnd("test", port_End, false);

		recommender = new IDemoRecommender(
				port_Recommender);

		display = new WDisplay("test", port_Display, "100",
				"200", "graphics", false);

		server = new STourId(port_IDServer, "01020304",
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
		try {
			obj = Class.forName(className);
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
			tour = obj.getConstructor(types).newInstance(values);
			
			sensor.startSimulate(port_IDServer, testCaseInstance);
			
			try {
				Thread.sleep((3)*1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityException e) {
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
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}
	
	public void stopRunning(){
		tourStart.quit();
		tourDemo.quit();
		tourEnd.quit();
		recommender.quit();
		display.quit();
		server.quit();
		try {
			Method quitMethod = obj.getMethod("quit", null);
			quitMethod.invoke(tour, null);
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void startSimulate(int serverPort, String testCase) {
		//2009/1/18:
		int index = testCase.indexOf("\t");
		while(index > -1){
			String event = testCase.substring(0, index);
			new TestCaseFeedThread( event, data, "127.0.0.1", serverPort).run();
			testCase = testCase.substring(index + "\t".length());
			index = testCase.indexOf("\t");
		}
		if(testCase.length() > 0){			
			new TestCaseFeedThread(testCase, data, "127.0.0.1", serverPort).run();
		}
	}
	
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
		new TestCaseFeedThread( event.context, data, "127.0.0.1", serverPort).run();

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
			 if (args.length == 3) {
				int versionNumber = Integer.parseInt(args[0]);
				int testCaseNumber = Integer.parseInt(args[1]);
				String testCaseInstance = args[2];
				System.err.println(testCaseNumber + " begins to execution");
				PositionIButton.getInstance().set(versionNumber, testCaseNumber, testCaseInstance);
				PositionIButton.getInstance().runTestCase();
				PositionIButton.getInstance().stopRunning();
				System.err.println(testCaseNumber + " finishes its execution");
//				System.exit(0);
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
