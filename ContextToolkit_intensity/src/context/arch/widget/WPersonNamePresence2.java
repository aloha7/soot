package context.arch.widget;

import context.arch.generator.PersonNameUI;
import context.arch.comm.DataObject;
import context.arch.service.Services;
import context.arch.subscriber.Callbacks;
import context.arch.storage.Attributes;
import context.arch.storage.Attribute;
import context.arch.storage.AttributeNameValues;
import context.arch.util.ContextTypes;

import java.util.Date;

/**
 * This class is a context widget that provides information on 
 * the presence of a person in a particular location.  The
 * information is in the form of a location and a user id.  It
 * has the following callbacks: UPDATE. It supports polling and
 * subscriptions.  It uses a graphical menu to input presence info.
 *
 * @see context.arch.widget.Widget
 */
public class WPersonNamePresence2 extends Widget {

  /**
   * Debug flag. Set to true to see debug messages.
   */
  private static final boolean DEBUG = false;

  /**
   * Widget version number
   */
  public String VERSION_NUMBER = "1.0.0";

  /**
   * Tag for user id
   */
  public static final String USERNAME = ContextTypes.USERNAME;

  /**
   * Tag for user location 
   */
  public static final String LOCATION = ContextTypes.LOCATION;

  /**
   * Name of widget
   */
  public static final String CLASSNAME = "PersonNamePresence2";

  private String name = "Futakawa";
  private String location;
  private PersonNameUI ui = null;		// pointer to our UI
	
  /**
   * Constructor that creates the widget at the given location and
   * monitors communications on the DEFAULT_PORT and
   * creates the graphical UI.  It also
   * sets the id of this widget to CLASSNAME_<location value>, with
   * storage enabled.
   *  
   * @param location Location the widget is "monitoring"
   */
  public WPersonNamePresence2 (String location) {
    this(location, DEFAULT_PORT, true);
  }

  /**
   * Constructor that creates the widget at the given location and
   * monitors communications on the DEFAULT_PORT and
   * creates the graphical UI.  It also
   * sets the id of this widget to CLASSNAME_<location value> and sets
   * storage functionality to storageFlag.
   *  
   * @param location Location the widget is "monitoring"
   * @param storageFlag Flag to indicate whether or not to enable storage functionality
   */
  public WPersonNamePresence2 (String location, boolean storageFlag) {
    this(location, DEFAULT_PORT, storageFlag);
  }

  /**
   * Constructor that creates the widget at the given location and 
   * monitors communications on the given port and
   * creates the graphical UI.  It also
   * sets the id of this widget to CLASSNAME_<location value>, with
   * storage enabled.
   *  
   * @param location Location the widget is "monitoring"
   * @param port Port to run the widget on
   */
  public WPersonNamePresence2 (String location, int port) {
    this(location,port,CLASSNAME+SPACER+location, true);
  }

  /**
   * Constructor that creates the widget at the given location and 
   * monitors communications on the given port and
   * creates the graphical UI.  It also
   * sets the id of this widget to CLASSNAME_<location value> and
   * sets the storage functionality to storageFlag.
   *  
   * @param location Location the widget is "monitoring"
   * @param port Port to run the widget on
   * @param storageFlag Flag to indicate whether or not to enable storage
   */
  public WPersonNamePresence2 (String location, int port, boolean storageFlag) {
    this(location,port,CLASSNAME+SPACER+location, storageFlag);
  }
  
  /**
   * Constructor that creates the widget at the given location and 
   * monitors communications on the given port and
   * creates the graphical UI.  It also
   * sets the id of this widget to the given id and sets storage
   * functionality to storageFlag
   *  
   * @param location Location the widget is "monitoring"
   * @param port Port to run the widget on
   * @param id Widget id
   * @param storageFlag Flag to indicate whether or not to enable storage
   */
  public WPersonNamePresence2 (String location, int port, String id, boolean storageFlag) {
    super(port,id,storageFlag);  //Create widget means starting a monitoring server and a ParserObject, restart the subscriber list.
    setVersion(VERSION_NUMBER);
    this.location = location;
    try {
      Thread.sleep (1000);
    } catch (InterruptedException ie) {
    }
    ui = new PersonNameUI(this);
    ui.show();
    ui.hide();
    ui.setUI();
    ui.show();
  }

  /**
   * This method implements the abstract method Widget.setAttributes().
   * It defines the attributes for the widget as:
   *    TIMESTAMP, USERNAME, and LOCATION 
   *
   * @return the Attributes used by this widget
   */
  protected Attributes setAttributes() {
    Attributes atts = new Attributes();
    atts.addAttribute(TIMESTAMP,Attribute.LONG);
    atts.addAttribute(USERNAME);
    atts.addAttribute(LOCATION);
    return atts;
  }

  protected Services setServices() {
    return new Services();
  }
  
  /**
   * This method implements the abstract method Widget.setCallbacks().
   * It defines the callbacks for the widget as:
   *    UPDATE with the attributes TIMESTAMP, USERNAME, LOCATION
   *
   * @return the Callbacks used by this widget
   */
  protected Callbacks setCallbacks() {  //WPersonNamePresence just provide the CallBack of update 
    Callbacks calls = new Callbacks();
    calls.addCallback(UPDATE,setAttributes());
    return calls;
  }

  /**
   * Called by the generator class when a significant event has
   * occurred.  It creates a DataObject, sends it to its subscribers and
   * stores the data.
   *
   * @param event Name of the event that has occurred
   * @param data Object containing relevant event data
   * @see context.arch.widget.Widget#sendToSubscribers(String, AttributeNameValues)
   * @see context.arch.widget.Widget#store(AttributeNameValues)
   */
  public void notify(String event, Object data) {
    AttributeNameValues atts = queryGenerator();
    if (atts != null) {      
      store(atts);
      if (subscribers.numSubscribers() > 0) {
        sendToSubscribers(event, atts);
      }
    }
  }
    
  /**
   * This method sets the name of the user who is present, and is called by the UI
   *
   * @param username Name of the user
   */
  public void setUser(String username) {
    name = username;
  }

  /**
   * This method returns an AttributeNameValues object with the latest presence info
   *
   * @return AttributeNameValues containing the latest data
   */
  protected AttributeNameValues queryGenerator() {
    AttributeNameValues atts = new AttributeNameValues();
    atts.addAttributeNameValue(USERNAME, name);
    atts.addAttributeNameValue(LOCATION,location);
    atts.addAttributeNameValue(TIMESTAMP,getCurrentTime(), Attribute.LONG);
    return atts;
  }

  /**
   * Main method to create a widget with location and port specified by 
   * command line arguments
   */
  public static void main(String argv[]) {
    if (argv.length == 1) {
      if (DEBUG) {
        System.out.println("Attempting to create a WPersonNamePresence2 on "+DEFAULT_PORT+" at " +argv[0]+" with storage enabled");
      }
      WPersonNamePresence2 wpp = new WPersonNamePresence2(argv[0]);
    }
    else if (argv.length == 2) {
      if ((argv[1].equals("false")) || (argv[1].equals("true"))) {
        if (DEBUG) {
          System.out.println("Attempting to create a WPersonNamePresence2 on "+DEFAULT_PORT+" at " +argv[0]+ "with storage set to "+argv[1]);
        }
        WPersonNamePresence2 wpp = new WPersonNamePresence2(argv[0], Boolean.valueOf(argv[1]).booleanValue());
      }
      else {
        if (DEBUG) {
          System.out.println("Attempting to create a WPersonNamePresence2 on "+argv[1]+" at " +argv[0]+ " with storage enabled");
        }
        WPersonNamePresence2 wpp = new WPersonNamePresence2(argv[0], Integer.parseInt(argv[1]));
      }
    }
    else if (argv.length == 3) {
      if (DEBUG) {
        System.out.println("Attempting to create a WPersonNamePresence2 on "+argv[1]+" at " +argv[0]+" with storage set to "+argv[2]);
      }
      WPersonNamePresence2 wpp = new WPersonNamePresence2(argv[0], Integer.parseInt(argv[1]), Boolean.valueOf(argv[2]).booleanValue());
    }
    else {
      System.out.println("USAGE: java context.arch.widget.WPersonNamePresence2 <location> [port] [storageFlag]");
    }
  }

}
