package context.arch.widget;

import context.arch.comm.DataObject;
import context.arch.generator.IButtonData;
import context.arch.subscriber.Subscriber;
import context.arch.subscriber.Callbacks;
import context.arch.storage.Attributes;
import context.arch.storage.Attribute;
import context.arch.storage.AttributeNameValues;
import context.arch.storage.AttributeNameValue;
import context.arch.util.ContextTypes;
import context.arch.util.Configuration;
import context.arch.util.ConfigObjects;
import context.arch.util.ConfigObject;
import context.arch.util.XMLURLReader;
import context.arch.comm.language.DecodeException;
import context.arch.service.DisplayChoiceService;
import context.arch.service.DisplayChoiceFrame;
import context.arch.handler.AsyncServiceHandler;
import context.apps.Tour.DemoFile;

import java.util.Enumeration;
import java.net.MalformedURLException;

import java.util.Vector;
import context.arch.comm.language.XMLEncoder;

import context.arch.generator.PositionIButton;
import java.util.Date;

/**
 * This class is a context widget that provides information on 
 * the presence of a person at a particular demo and their level of 
 * interest in the demo.  The information is in the form of 
 * a demo and a user id.  It has the following callbacks: VISIT and INTEREST. 
 * It supports polling and subscriptions.  Currently it uses 
 * WPersonPresence as a means of providing presence.  When a user is
 * present at a particular demo, the name of the demo, and relevant demo
 * information is sent to subscribers. As well, a service is called to
 * display a choices menu to the user, to allow them to select their level
 * of interest in the demo.  This information is also passed to interested
 * subscribers.
 *
 * @see context.arch.widget.WPersonPresence
 * @see AsyncServiceHandler
 */
public class WTourDemo extends WPersonPresence implements AsyncServiceHandler {

	/**
	 * Debug flag. Set to true to see debug messages.
	 */
	private static final boolean DEBUG = false;

	/**
	 * Widget version number
	 */
	public String VERSION_NUMBER = "1.0.0";

	/**
	 * Tag for interest level
	 */
	public static final String INTEREST_LEVEL = ContextTypes.INTEREST_LEVEL;

	/**
	 * Tag for demo
	 */
	public static final String DEMO_NAME = ContextTypes.DEMO_NAME;

	/**
	 * Tag for demo url
	 */
	public static final String DEMO_URL = ContextTypes.DEMO_URL;

	/**
	 * Tag for demoer url
	 */
	public static final String DEMOER_URL = ContextTypes.DEMOER_URL;

	/**
	 * Tag for display device
	 */
	public static final String DISPLAY_DEVICE = ContextTypes.DISPLAY_DEVICE;

	/**
	 * Tag for minimum interest 
	 */
	public static final String MINIMUM_INTEREST = "minimumInterest";

	/**
	 * Tag for medium interest 
	 */
	public static final String MEDIUM_INTEREST = "mediumInterest";

	/**
	 * Tag for maximum interest 
	 */
	public static final String MAXIMUM_INTEREST = "maximumInterest";

	/**
	 * Label for minimum interest 
	 */
	public static final String MIN_LABEL = "Low";

	/**
	 * Label for medium interest 
	 */
	public static final String MED_LABEL = "Medium";

	/**
	 * Label for maximum interest 
	 */
	public static final String MAX_LABEL = "High";

	/**
	 * Tag for visit callback
	 */
	public static final String VISIT = "visit";

	/**
	 * Tag for interest callback
	 */
	public static final String INTEREST = "interest";

	/**
	 * Name of widget
	 */
	public static final String CLASSNAME = "TourDemo";

	/**
	 * Default port this widget is running on
	 */
	public static final int DEFAULT_PORT = 5800;

	private String demo;
	private String userid;
	private String interest;
	private String demoUrl;
	private String demoerUrl;
	private ConfigObjects servers;
	private DemoFile demoInfo;

	/**
	 * Constructor that creates the widget at the given demo and
	 * monitors communications on the DEFAULT_PORT.  It also
	 * sets the id of this widget to CLASSNAME_<demo value>.  It reads
	 * the configFile to determine where a relevant user's server is,
	 * so it knows where to display the choices menu.  It reads the 
	 * demoFile to get relevant information about the demo it's monitoring with 
	 * storage enabled.
	 *  
	 * @param demo Demo the widget is "monitoring"
	 * @param configFile File to use for configuration
	 * @param demoFile File to use for configuration
	 */
	public WTourDemo(String demo, String configFile, String demoFile) {
		this(demo, DEFAULT_PORT, configFile, demoFile, true);
	}

	/**
	 * Constructor that creates the widget at the given demo and
	 * monitors communications on the DEFAULT_PORT.  It also
	 * sets the id of this widget to CLASSNAME_<demo value>.  It reads
	 * the configFile to determine where a relevant user's server is,
	 * so it knows where to display the choices menu.  It reads the 
	 * demoFile to get relevant information about the demo it's monitoring with 
	 * storage functionality set to storageFlag.
	 *  
	 * @param demo Demo the widget is "monitoring"
	 * @param configFile File to use for configuration
	 * @param demoFile File to use for configuration
	 * @param storageFlag Flag to indicate whether storage is enabled
	 */
	public WTourDemo(String demo, String configFile, String demoFile,
			boolean storageFlag) {
		this(demo, DEFAULT_PORT, configFile, demoFile, storageFlag);
	}

	/**
	 * Constructor that creates the widget at the given demo and 
	 * monitors communications on the given port .  It also
	 * sets the id of this widget to CLASSNAME_<demo value>.  It reads
	 * the configFile to determine where a relevant user's server is,
	 * so it knows where to display the choices menu.  It reads the 
	 * demoFile to get relevant information about the demo it's monitoring
	 * with storage enabled.
	 *  
	 * @param demo Demo the widget is "monitoring"
	 * @param port Port to run the widget on
	 * @param configFile File to use for configuration
	 * @param demoFile File to use for configuration
	 */
	public WTourDemo(String demo, int port, String configFile, String demoFile) {
		this(demo, port, configFile, demoFile, CLASSNAME + SPACER + demo, true);
	}

	/**
	 * Constructor that creates the widget at the given demo and 
	 * monitors communications on the given port .  It also
	 * sets the id of this widget to CLASSNAME_<demo value>.  It reads
	 * the configFile to determine where a relevant user's server is,
	 * so it knows where to display the choices menu.  It reads the 
	 * demoFile to get relevant information about the demo it's monitoring
	 * with storage functionality set to storageFlag.
	 *  
	 * @param demo Demo the widget is "monitoring"
	 * @param port Port to run the widget on
	 * @param configFile File to use for configuration
	 * @param demoFile File to use for configuration
	 * @param storageFlag Flag to indicate whether storage is enabled
	 */
	public WTourDemo(String demo, int port, String configFile, String demoFile,
			boolean storageFlag) {
		this(demo, port, configFile, demoFile, CLASSNAME + SPACER + demo,
				storageFlag);
	}

	/**
	 * Constructor that creates the widget at the given demo and 
	 * monitors communications on the given port It also
	 * sets the id of this widget to the given id.  It reads
	 * the configFile to determine where a relevant user's server is,
	 * so it knows where to display the choices menu.  It reads the 
	 * demoFile to get relevant information about the demo it's monitoring
	 * with storage functionality set to storageFlag
	 *  
	 * @param demo Demo the widget is "monitoring"
	 * @param port Port to run the widget on
	 * @param configFile File to use for configuration
	 * @param demoFile File to use for demo information
	 * @param id Widget id
	 * @param storageFlag Flag to indicate whether storage is enabled
	 */
	public WTourDemo(String demo, int port, String configFile, String demoFile,
			String id, boolean storageFlag) {
		super(demo, port, id, storageFlag);
		this.demo = demo; //this.demo is the name of the demo
		this.demoUrl = demoUrl;
		this.demoerUrl = demoerUrl;
		try {
			XMLURLReader reader = new XMLURLReader(configFile);
			DataObject data = reader.getParsedData();
			Configuration config = new Configuration(data);
			servers = config.getServerConfigurations();
		} catch (MalformedURLException mue) {
			System.out.println("TourApp MalformedURL: " + mue);
		} catch (DecodeException de) {
			System.out.println("TourApp Decode: " + de);
			de.printStackTrace();
		}
		demoInfo = new DemoFile(demoFile);
		demoUrl = demoInfo.getDemoUrl(demo);
		demoerUrl = demoInfo.getDemoerUrl(demo);

		/*
		//2008/7/7: add a script to test notify functions correctly.
		PositionIButton sensor = new PositionIButton();
		String location = "test";
		int[] iButtonId = new int[]{1,2,3,4};
		String currentid = sensor.toHexString(iButtonId);
		long currentTime = new Date().getTime();   
		//notify(Widget.UPDATE, new IButtonData(location, currentid, Long.toString(currentTime)));
		 * 
		 */
	}

	/**
	 * This method implements the abstract method Widget.setCallbacks().
	 * It defines the callbacks for the widget as:
	 *    VISIT with the attributes TIMESTAMP, USERID, DEMO_NAME, DEMO_URL, DEMOER_URL
	 *    INTEREST with the attributes TIMESTAMP, USERID, DEMO_NAME, INTEREST_LEVEL
	 *
	 * @return the Callbacks used by this widget
	 */
	protected Callbacks setCallbacks() {
		Callbacks calls = new Callbacks();
		Attributes atts = new Attributes();
		atts.addAttribute(TIMESTAMP);
		atts.addAttribute(USERID);
		atts.addAttribute(DEMO_NAME);
		atts.addAttribute(DEMO_URL);
		atts.addAttribute(DEMOER_URL);
		calls.addCallback(VISIT, atts);

		atts = super.setAttributes();
		atts.addAttribute(INTEREST_LEVEL);
		calls.addCallback(INTEREST, setAttributes());
		return calls;
	}

	/**
	 * This method implements the abstract method Widget.setAttributes().
	 * It defines the attributes for the widget as:
	 *    TIMESTAMP, USERID, DEMO_NAME, and INTEREST_LEVEL
	 *
	 * @return the Attributes used by this widget
	 */
	protected Attributes setAttributes() {
		Attributes atts = new Attributes();
		atts.addAttribute(TIMESTAMP);
		atts.addAttribute(USERID);
		atts.addAttribute(DEMO_NAME);
		atts.addAttribute(INTEREST_LEVEL);
		atts.addAttribute(DEMO_URL);
		atts.addAttribute(DEMOER_URL);
		return atts;
	}

	/**
	 * Called by the generator class when a significant event has
	 * occurred.  It creates a DataObject, sends it to its subscribers and
	 * stores the data.  When a user is present at a demo, the VISIT callback
	 * is activated.  Then, the display device for this user is determined 
	 * and a service to display a choices menu is called.
	 *
	 * @param event Name of the event that has occurred
	 * @param data Object containing relevant event data
	 * @see context.arch.widget.Widget#sendToSubscribers(String, AttributeNameValues)
	 * @see context.arch.widget.Widget#store(AttributeNameValues)
	 */
	public void notify(String event, Object data) {
		//2008/7/9
		if (event.equals(WTourDemo.VISIT)) {
//			System.out.println(this.getClass().getName() + " gets a context:"
//					+ WTourDemo.VISIT);

			IButtonData ibutton = (IButtonData) data;
			AttributeNameValues atts = IButtonData2Attributes(ibutton);
			atts.getAttributeNameValue(LOCATION).setName(DEMO_NAME);
			atts.addAttributeNameValue(DEMO_URL, demoUrl);
			atts.addAttributeNameValue(DEMOER_URL, demoerUrl);
			if (subscribers.numSubscribers() > 0) {
				sendToSubscribers(VISIT, atts);
			}
			store(atts); //We need to add some scripts to make generators sample data automatically 

			if (servers != null) {
				ConfigObject serverObj = null;
				for (Enumeration e = servers.getEnumeration(); e
						.hasMoreElements();) {
					ConfigObject configObj = (ConfigObject) e.nextElement();
					if (configObj.getId().indexOf(ibutton.getId()) != -1) {
						serverObj = configObj;
					}
				}
				String serverId = serverObj.getId();
				int serverPort = Integer.parseInt(serverObj.getPort());
				String serverHost = serverObj.getHost();
				Attributes attributes = new Attributes();
				attributes.addAttribute(DISPLAY_DEVICE);
				//2008/7/7:We should make widgetId = serverId instead of widgetId = serverId+SPACER+ibutton.getId() 
				//DataObject poll = pollWidget(serverHost, serverPort, serverId+SPACER+ibutton.getId(), attributes);

				DataObject poll = pollWidget(serverHost, serverPort, serverId,
						attributes);
				AttributeNameValues pollAtts = new AttributeNameValues(poll);
				
				String display = (String) pollAtts.getAttributeNameValue(
						DISPLAY_DEVICE).getValue();

				AttributeNameValues input = new AttributeNameValues();
				input.addAttributeNameValue(DisplayChoiceService.TITLE,
						"Question for demo " + demo);
				input.addAttributeNameValue(DisplayChoiceService.QUESTION,
						"Please indicate your interest level for " + demo);
				input.addAttributeNameValue(DisplayChoiceService.CHOICE,
						MIN_LABEL);
				input.addAttributeNameValue(DisplayChoiceService.CHOICE,
						MED_LABEL);
				input.addAttributeNameValue(DisplayChoiceService.CHOICE,
						MAX_LABEL);

				//WDisplay should be WDisplay.CLASSNAME+SPACER+"test" instead of WDisplay.CLASSNAME+SPACER+"here"      
				//          DataObject service = executeAsynchronousWidgetService(this,display,8000, 
				//                        WDisplay.CLASSNAME+SPACER+"here", DisplayChoiceService.DISPLAY_CHOICES, 
				//                        DisplayChoiceService.DISPLAY, input, CLASSNAME+ibutton.getId());

				//2009/1/15: this will cause an Exception since no services available
//				DataObject service = executeAsynchronousWidgetService(this,
//						display, 8000, WDisplay.CLASSNAME + SPACER + "test",
//						DisplayChoiceService.DISPLAY_CHOICES,
//						DisplayChoiceService.DISPLAY, input, CLASSNAME
//								+ ibutton.getId());

				//        DataObject service = executeAsynchronousWidgetService(this,"127.0.0.1",8000, 
				//                          "SUser_Anind", "SUser_Anind"+DisplayChoiceService.DISPLAY_CHOICES, 
				//                          DisplayChoiceService.DISPLAY, input, "TourDemo 1");

			}
		} else if (event.equals(WTourDemo.INTEREST)) {//2009/1/16:
//			System.out.println(this.getClass().getName() + " gets a context:"
//					+ WTourDemo.INTEREST);

			IButtonData ibutton = (IButtonData) data;
			AttributeNameValues atts = new AttributeNameValues();
			atts.addAttributeNameValue(DisplayChoiceService.CHOICE, MED_LABEL);
			this.asynchronousServiceHandle(CLASSNAME + ibutton.getId(), atts
					.toDataObject());

		}

	}

	/**
	 * Method for the AsyncServiceHandle interface.  It is called when
	 * the DISPLAY_CHOICES service returns some results.  This method activates
	 * the INTEREST callback and sends subscribers the demo name, user id, 
	 * timestamp of the callback and level of interest info.
	 *
	 * @param id CLASSNAME+userid string
	 * @param result DataObject containing the result of the service execution
	 * @return null
	 */
	public DataObject asynchronousServiceHandle(String id, DataObject result) {
		AttributeNameValues values = new AttributeNameValues(result);
		AttributeNameValue att = values
				.getAttributeNameValue(DisplayChoiceService.CHOICE);
		if (!att.getValue().equals(DisplayChoiceFrame.NO_CHOICE)) {
			AttributeNameValues atts = new AttributeNameValues();
			atts.addAttributeNameValue(DEMO_NAME, demo);
			atts
					.addAttributeNameValue(USERID, id.substring(CLASSNAME
							.length()));
			atts.addAttributeNameValue(TIMESTAMP, getCurrentTime(),
					Attribute.LONG);
			String interest = (String) att.getValue();
			if (interest.equals(MIN_LABEL)) {
				atts.addAttributeNameValue(INTEREST_LEVEL, MINIMUM_INTEREST);
			} else if (interest.equals(MED_LABEL)) {
				atts.addAttributeNameValue(INTEREST_LEVEL, MEDIUM_INTEREST);
			} else if (interest.equals(MAX_LABEL)) {
				atts.addAttributeNameValue(INTEREST_LEVEL, MAXIMUM_INTEREST);
			} else {
				atts.addAttributeNameValue(INTEREST_LEVEL, att.getValue());
			}
			if (subscribers.numSubscribers() > 0) {
				sendToSubscribers(INTEREST, atts);
			}
			store(atts);
		}
		return null;
	}

	/**
	 * Temporary main method to create a widget with demo and port specified by 
	 * command line arguments
	 */
	public static void main(String argv[]) {
		if (argv.length == 3) {
			if (DEBUG) {
				System.out.println("Attempting to create a WTourDemo on "
						+ DEFAULT_PORT + " at " + argv[0]
						+ " with storage enabled");
			}

			WTourDemo wtd = new WTourDemo(argv[0], argv[1], argv[2]);

			/*//2008/7/5:Create a Configuration file
			 Vector v = new Vector();
			 v.addElement(new DataObject(Configuration.VERSION, "1"));
			 v.addElement(new DataObject(Configuration.DESCRIPTION,"hello"));
			 v.addElement(new DataObject(Configuration.AUTHOR,"hwang"));
			 
			 Vector m = new Vector();
			 m.addElement(new DataObject(WPersonNamePresence2.USERNAME,"hwang"));
			 m.addElement(new DataObject(WPersonNamePresence2.LOCATION,"test"));
			 m.addElement(new DataObject(WPersonNamePresence2.TIMESTAMP,"2"));
			       
			 v.addElement(new DataObject(Configuration.PARAMETERS,m));
			
			
			 Vector m5 = new Vector();
			 
			 Vector m1 = new Vector();
			 m1.addElement(new DataObject(ConfigObjects.ID, "WPersonNamePresence2_test"));
			 m1.addElement(new DataObject(ConfigObjects.HOSTNAME,"127.0.0.1"));
			 m1.addElement(new DataObject(ConfigObjects.PORT,"5000"));
			 m1.addElement(new DataObject(ConfigObjects.TYPE,"Widget"));      
			 m5.addElement(new DataObject("WIDGET", m1));
			 
			 Vector m6 = new Vector();
			 m6.addElement(new DataObject(ConfigObjects.ID, "WPersonNamePresence_test"));
			 m6.addElement(new DataObject(ConfigObjects.HOSTNAME,"127.0.0.1"));
			 m6.addElement(new DataObject(ConfigObjects.PORT,"5100"));
			 m6.addElement(new DataObject(ConfigObjects.TYPE,"Widget"));      
			 m5.addElement(new DataObject("WIDGET", m6));           
			 v.addElement(new DataObject(Configuration.WIDGETS, m5));
			 
			 Vector m7 = new Vector();
			 Vector m2 = new Vector();
			 m2.addElement(new DataObject(ConfigObjects.ID,"IDemoRecommender"));
			 m2.addElement(new DataObject(ConfigObjects.HOSTNAME,"127.0.0.1"));
			 m2.addElement(new DataObject(ConfigObjects.PORT,"5200"));
			 m2.addElement(new DataObject(ConfigObjects.TYPE,"Interpreter"));
			 m7.addElement(new DataObject("Interpreter", m2));
			 
			 Vector m3 = new Vector();
			 m3.addElement(new DataObject(ConfigObjects.ID,"IGroup2URL"));
			 m3.addElement(new DataObject(ConfigObjects.HOSTNAME,"127.0.0.1"));
			 m3.addElement(new DataObject(ConfigObjects.PORT,"5200"));
			 m3.addElement(new DataObject(ConfigObjects.TYPE,"Interpreter"));
			 m7.addElement(new DataObject("Interpreter", m3));
			            
			 v.addElement(new DataObject(Configuration.INTERPRETERS,m7));
			 
			 Vector m55 = new Vector();
			 Vector m21 = new Vector();
			 m21.addElement(new DataObject(ConfigObjects.ID,"AsynchronousService"));
			 m21.addElement(new DataObject(ConfigObjects.HOSTNAME, "127.0.0.1"));
			 m21.addElement(new DataObject(ConfigObjects.PORT,"5003"));
			 m21.addElement(new DataObject(ConfigObjects.TYPE,"Service"));
			 m55.addElement(new DataObject("Server",m21));
			 
			 Vector m22 = new Vector();
			 m22.addElement(new DataObject(ConfigObjects.ID,"HandleService"));
			 m22.addElement(new DataObject(ConfigObjects.HOSTNAME, "127.0.0.1"));
			 m22.addElement(new DataObject(ConfigObjects.PORT,"5004"));
			 m22.addElement(new DataObject(ConfigObjects.TYPE,"Service"));
			 m55.addElement(new DataObject("Server", m22));
			 v.addElement(new DataObject(Configuration.SERVERS, m55));
			 
			 Vector m8 = new Vector();
			 Vector m4 = new Vector();
			 m4.addElement(new DataObject(ConfigObjects.ID, "Handler"));
			 m4.addElement(new DataObject(ConfigObjects.HOSTNAME,"127.0.0.1"));
			 m4.addElement(new DataObject(ConfigObjects.PORT, "5002"));
			 m4.addElement(new DataObject(ConfigObjects.TYPE, "Component"));
			 m8.addElement(new DataObject("Component", m4));
			 
			 Vector m11 = new Vector();
			 m11.addElement(new DataObject(ConfigObjects.ID, "Handler"));
			 m11.addElement(new DataObject(ConfigObjects.HOSTNAME,"127.0.0.1"));
			 m11.addElement(new DataObject(ConfigObjects.PORT, "5002"));
			 m11.addElement(new DataObject(ConfigObjects.TYPE, "Component"));
			 m8.addElement(new DataObject("Component",m11));
			       
			 v.addElement(new DataObject(Configuration.OTHER_COMPONENTS,m8));
			 
			 
			 
			 
			 DataObject config = new DataObject(Configuration.CONFIGURATION,v);
			 XMLEncoder encoder = new XMLEncoder();
			 try{
			  String str = encoder.encodeData(config);  
			  System.out.println(str);
			 }catch(Exception e){
			  
			 }
			 */

		} else if (argv.length == 4) {
			if ((argv[3].equals("true")) || (argv[3].equals("false"))) {
				if (DEBUG) {
					System.out.println("Attempting to create a WTourDemo on "
							+ DEFAULT_PORT + " at " + argv[0]
							+ " with storage set to " + argv[3]);
				}
				WTourDemo wtd = new WTourDemo(argv[0], argv[1], argv[2],
						Boolean.valueOf(argv[3]).booleanValue());
			} else {
				if (DEBUG) {
					System.out.println("Attempting to create a WTourDemo on "
							+ argv[1] + " at " + argv[0]
							+ " with storage enabled");
				}
				WTourDemo wtd = new WTourDemo(argv[0], Integer
						.parseInt(argv[1]), argv[2], argv[3]);
			}
		} else if (argv.length == 5) {
			if (DEBUG) {
				System.out.println("Attempting to create a WTourDemo on "
						+ argv[1] + " at " + argv[0] + "with storage set to "
						+ argv[4]);
			}
			WTourDemo wtd = new WTourDemo(argv[0], Integer.parseInt(argv[1]),
					argv[2], argv[3], Boolean.valueOf(argv[4]).booleanValue());
		} else {
			System.out
					.println("USAGE: java context.arch.widget.WTourDemo <demo> [port] <config file> <info file> [storageFlag]");
		}
	}
}
