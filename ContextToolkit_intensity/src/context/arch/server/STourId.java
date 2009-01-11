package context.arch.server;

import context.arch.comm.DataObject;
import context.arch.subscriber.Callbacks;
import context.arch.service.Services;
import context.arch.storage.Attributes;
import context.arch.storage.AttributeNameValues;
import context.arch.storage.Conditions;
import context.arch.storage.Storage;
import context.arch.widget.WidgetHandle;
import context.arch.widget.WidgetHandles;
import context.arch.util.Error;
import context.arch.util.ContextTypes;
import context.arch.util.Configuration;
import context.arch.util.ConfigObjects;
import context.arch.util.ConfigObject;
import context.arch.util.XMLURLReader;
import context.arch.comm.language.DecodeException;

import java.util.Enumeration;
import java.net.MalformedURLException;

/**
 * This class implements a user id server for a tour guide app.  It should 
 * subscribe to all the widgets that could provide information about its 
 * user with regards to a tour.
 *
 * @see Server
 */
public class STourId extends Server {

  /**
   * Debug flag. Set to true to see debug messages.
   */
  private static final boolean DEBUG = true;

  /**
   * Name of server
   */
  public static final String CLASSNAME = "idServer";

  /**
   * Tag to indicate user id (our key) 2008/7/6
   */ 
  public static final String USERID = ContextTypes.USERID;
  

  private String userid;

  /**
   * Constructor that creates a user server for user id
   * on the specified port. This server monitors the set of widgets
   * in widgets and the tour guide widgets specified in configFile.
   * This latter set of widgets include TourRegistration, TourDemo, and
   * TourEnd widgets with storage enabled.
   *  
   * @param port Port this server is listening on
   * @param userid User id this server is attached to
   * @param widgets Set of widgets this server monitors
   * @param configFile File to use for configuration
   *
   */
  public STourId (int port, String userid, WidgetHandles widgets, String configFile) {
	  this(port, userid, false, widgets, configFile);
    //this(port, userid, true, widgets, configFile);
  }

  /**
   * Constructor that creates a user server for user id
   * on the specified port. This server monitors the set of widgets
   * in widgets and the tour guide widgets specified in configFile.
   * This latter set of widgets include TourRegistration, TourDemo, and
   * TourEnd widgets with storage functionality set to storageFlag
   *  
   * @param port Port this server is listening on
   * @param userid User id this server is attached to
   * @param widgets Set of widgets this server monitors
   * @param configFile File to use for configuration
   * @param storageFlag Flag to indicate whether storage is enabled or not
   *
   */
  public STourId (int port, String userid, boolean storageFlag, WidgetHandles widgets, String configFile) {
    super(port, CLASSNAME+SPACER+userid, storageFlag, widgets);
    this.userid = userid;
    setVersion(VERSION_NUMBER);
    try {
      XMLURLReader reader = new XMLURLReader(configFile);
      DataObject data = reader.getParsedData();
System.out.println(this.getClass().getName() +  ":getConfigurationFile:" + parser.encodeData(data));      
      Configuration config = new Configuration(data);
      ConfigObjects widgetObjs = config.getWidgetConfigurations();
      if (widgetObjs != null) { //If extending is necessary, add widgets in the configuration files, then the system will load it automatically
        for (Enumeration e=widgetObjs.getEnumeration(); e.hasMoreElements(); ) {
          ConfigObject widgetObj = (ConfigObject)e.nextElement();
          widgets.addWidgetHandle(widgetObj.getId(),widgetObj.getHost(),Integer.parseInt(widgetObj.getPort()));
        }
      }
      setWidgets(widgets);
      serverSetup(); //Get Attributes, Services from Widgets
      startSubscriptions(); //Get callbacks from Subscriptions, and subscribe the widgets' according attributes  
      System.out.println(CLASSNAME+SPACER+userid + " is running");
    } catch (MalformedURLException mue) {
        System.out.println("TourApp MalformedURL: "+mue);
    } catch (DecodeException de) {
        System.out.println("TourApp Decode: "+de); 
    } catch(Exception e){
    	System.out.println(e);
    }
  }

  /**
   * Constructor that creates a user server for user id
   * on the DEFAULT_PORT. This server monitors the set of widgets
   * in widgets and the tour guide widgets specified in configFile.
   * This latter set of widgets include TourRegistration, TourDemo, and
   * TourEnd widgets with storage enabled.
   *  
   * @param userid User id this server is attached to
   * @param widgets Set of widgets this server monitors
   * @param configFile File to use for configuration
   *
   */
  public STourId (String userid, WidgetHandles widgets, String configFile) {
    this(DEFAULT_PORT, userid, true, widgets, configFile);
  }

  /**
   * Constructor that creates a user server for user id
   * on the DEFAULT_PORT. This server monitors the set of widgets
   * in widgets and the tour guide widgets specified in configFile.
   * This latter set of widgets include TourRegistration, TourDemo, and
   * TourEnd widgets with storage functionality set to storageFlag.
   *  
   * @param userid User id this server is attached to
   * @param widgets Set of widgets this server monitors
   * @param configFile File to use for configuration
   * @param storageFlag Flag to indicate whether storage is enabled or not
   *
   */
  public STourId (String userid, boolean storageFlag, WidgetHandles widgets, String configFile) {
    this(DEFAULT_PORT, userid, storageFlag, widgets, configFile);
  }

  /**
   * This abstract method is called when the widget wants to get the latest generator
   * info.  Maybe we should query our widgets here?
   * 
   * @return Attributes containing the latest generator information
   */
  protected AttributeNameValues queryGenerator() {
  	return null;
  }
 
  /**
   * This method sets the attributes for the server - those
   * that are specific to the server, and not contained in the widgets
   * it subscribes to.  Currently, there are none.
   *
   * @return Attributes object containing the server-specific attributes
   */
  protected Attributes setServerAttributes() {
    return new Attributes();
  }

  /**
   * This method set the callbacks for a server - those
   * that are specific to the server, and not contained in the widgets
   * it subscribes to.  Currently, there are none.
   *
   * @return Callbacks object containing the server-specific callbacks
   */
  protected Callbacks setServerCallbacks() {
    return new Callbacks();
  }

  /**
   * This method set the services for a server - those
   * that are specific to the server, and not contained in the widgets
   * it subscribes to.  Currently, there are none.
   *
   * @return Services object containing the server-specific services
   */
  protected Services setServerServices() {
    return new Services();
  }

  /**
   * This method sets the conditions to apply to the
   * server's subscriptions.  The condition for the User Server is
   * USERID='userid'
   *
   * @return Conditions containing condition info for server subscriptions
   */
  protected Conditions setConditions() {
    Conditions conds = new Conditions();
    conds.addCondition(USERID,Storage.EQUAL,userid);
    return conds;
  }

  /**
   * Main method to create a server with name, port and widgets specified by 
   * command line arguments. Each widget is specified by: host, port, id
   */
  public static void main(String argv[]) {
    if (argv.length == 2) {
      if (DEBUG) {
        System.out.println("Attempting to create a STourId on "+DEFAULT_PORT+" for " +argv[0]+" with storage enabled");
      }
      STourId sti = new STourId(argv[0], new WidgetHandles(), argv[1]);
    }
    else if ((argv.length != 0) & ((argv.length - 3) % 3 == 0)) {
      WidgetHandles widgets = new WidgetHandles ();
      for (int i=0; i<(argv.length-3)/3; i++) {
        String rhost = argv[3+3*i];
        int rport = Integer.parseInt(argv[3+3*i+1]);
        String rid = argv[3+3*i+2];

        WidgetHandle w = new WidgetHandle(rid, rhost, rport);
        widgets.addWidgetHandle(rid,rhost,rport);
      }

      if ((argv[2].equals("true")) || (argv[2].equals("false"))) {
        if (DEBUG) {
          System.out.println("Attempting to create a STourId on "+DEFAULT_PORT+" for " +argv[0]+" with storage set to "+argv[2]+" with widgets "+widgets);
        }
     	  STourId sti = new STourId(argv[0], Boolean.valueOf(argv[2]).booleanValue(), widgets, argv[1]);
      }
      else {
        if (DEBUG) {
          System.out.println("Attempting to create a STourId on "+argv[1]+" for " +argv[0]+" with storage enabled, with widgets "+widgets);
        }
     	  STourId sti = new STourId(Integer.parseInt(argv[1]), argv[0], widgets, argv[2]);
      }
    }
    else if ((argv.length != 0) & ((argv.length - 4) % 3 == 0)) {
      WidgetHandles widgets = new WidgetHandles ();
      for (int i=0; i<(argv.length-4)/3; i++) {
        String rhost = argv[4+3*i];
        int rport = Integer.parseInt(argv[4+3*i+1]);
        String rid = argv[4+3*i+2];

        WidgetHandle w = new WidgetHandle(rid, rhost, rport);
        widgets.addWidgetHandle(rid,rhost,rport);
      }
      		
      if (DEBUG) {
        System.out.println("Attempting to create a STourId on "+argv[1]+" for " +argv[0]+" with storage set to "+argv[3]+" with widgets "+widgets);
      }
     	STourId sti = new STourId (Integer.parseInt(argv[1]), argv[0], Boolean.valueOf(argv[3]).booleanValue(), widgets, argv[2]);
    }
    else {
      System.out.println("USAGE: java context.arch.server.STourId <userid> <port> <config file> [storageFlag] {widget_host widget_port, widget_id}");
    }
  }

}
