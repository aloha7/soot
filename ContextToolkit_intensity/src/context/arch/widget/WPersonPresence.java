package context.arch.widget;

import context.arch.comm.DataObject;
import context.arch.generator.PositionIButton;
import context.arch.generator.IButtonData;
import context.arch.subscriber.Callbacks;
import context.arch.service.Services;
import context.arch.storage.Attributes;
import context.arch.storage.Attribute;
import context.arch.storage.AttributeNameValues;
import context.arch.util.ContextTypes;

/**
 * This class is a context widget that provides information on 
 * the presence of a person in a particular location.  The
 * information is in the form of a location and a user id.  It
 * has the following callbacks: UPDATE. It supports polling and
 * subscriptions.  Currently it only uses the IButton as a means
 * of providing presence.  It handles only a single IButton
 * instance.
 *
 * @see context.arch.widget.Widget
 * @see context.arch.generator.PositionIButton
 */
public class WPersonPresence extends Widget {

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
  public static final String USERID = ContextTypes.USERID;

  /**
   * Tag for user location 
   */
  public static final String LOCATION = ContextTypes.LOCATION;

  /**
   * Name of widget
   */
  public static final String CLASSNAME = "PersonPresence";

  /**
   * Tag to indicate the location is Anind's office
   */
  public static final String ANIND_OFFICE = "Anind's Office";

  /**
   * Tag to indicate the location is Daniel's office
   */
  public static final String DANIEL_OFFICE = "Daniel's Office";

  /**
   * Tag to indicate the location is Gregory's office
   */
  public static final String GREGORY_OFFICE = "Gregory's Office";

  /**
   * Tag to indicate the location is the FCL
   */
  public static final String FCL = "FCL";

  /**
   * Tag to indicate the location is the Common Area
   */
  public static final String COMMON_AREA = "Common Area";

  /**
   * The default port this widget runs on is 5100
   */
  public static final int DEFAULT_PORT = 5100;

  private String location;
  protected PositionIButton ibutton;
	
  /**
   * Constructor that creates the widget at the given location and
   * monitors communications on the DEFAULT_PORT and
   * creates an instance of the IButton position generator.  It also
   * sets the id of this widget to CLASSNAME_<location value>, with
   * storage enabled.
   *  
   * @param location Location the widget is "monitoring"
   *
   * @see context.arch.generator.PositionIButton
   */
  public WPersonPresence (String location) {
    this(location, DEFAULT_PORT, true);
  }

  /**
   * Constructor that creates the widget at the given location and
   * monitors communications on the DEFAULT_PORT and
   * creates an instance of the IButton position generator.  It also
   * sets the id of this widget to CLASSNAME_<location value>, and sets
   * storage functionality to storageFlag.
   *  
   * @param location Location the widget is "monitoring"
   * @param storageFlag Flag to indicate whether or not to enable storage functionality
   *
   * @see context.arch.generator.PositionIButton
   */
  public WPersonPresence (String location, boolean storageFlag) {
    this(location, DEFAULT_PORT, storageFlag);
  }

  /**
   * Constructor that creates the widget at the given location and 
   * monitors communications on the given port and
   * creates an instance of the IButton position generator.  It also
   * sets the id of this widget to CLASSNAME_<location value>, with
   * storage enabled.
   *  
   * @param location Location the widget is "monitoring"
   * @param port Port to run the widget on
   *
   * @see context.arch.generator.PositionIButton
   */
  public WPersonPresence (String location, int port) {
    this(location,port,CLASSNAME+SPACER+location,true);
  }

  /**
   * Constructor that creates the widget at the given location and 
   * monitors communications on the given port and
   * creates an instance of the IButton position generator.  It also
   * sets the id of this widget to CLASSNAME_<location value> and sets
   * storage functionality to storageFlag.
   *  
   * @param location Location the widget is "monitoring"
   * @param port Port to run the widget on
   * @param storageFlag Flag to indicate whether or not to enable storage functionality
   *
   * @see context.arch.generator.PositionIButton
   */
  public WPersonPresence (String location, int port, boolean storageFlag) {
    this(location,port,CLASSNAME+SPACER+location,storageFlag);
  }
  
  /**
   * Constructor that creates the widget at the given location and 
   * monitors communications on the given port and
   * creates an instance of the IButton position generator.  It also
   * sets the id of this widget to the given id and sets storage
   * functionality to storageFlag
   *  
   * @param location Location the widget is "monitoring"
   * @param port Port to run the widget on
   * @param id Widget id
   * @param storageFlag Flag to indicate whether or not to enable storage functionality
   *
   * @see context.arch.generator.PositionIButton
   */
  public WPersonPresence (String location, int port, String id, boolean storageFlag) {
    super(port,id,storageFlag);
    setVersion(VERSION_NUMBER);
    this.location = location;
    //2008/7/9: bound listensor to the same sensor 
    //ibutton = new PositionIButton (this,location); //bind message handler with the PositionIButton    
    ibutton = PositionIButton.getInstance();
    ibutton.addListensor(this);
  }

  /**
   * This method implements the abstract method Widget.setAttributes().
   * It defines the attributes for the widget as:
   *    TIMESTAMP, USERID, and LOCATION 
   *
   * @return the Attributes used by this widget
   */
  protected Attributes setAttributes() {
    Attributes atts = new Attributes();
    atts.addAttribute(TIMESTAMP,Attribute.LONG);
    atts.addAttribute(USERID);
    atts.addAttribute(LOCATION);
    return atts;
  }
  
  /**
   * This method implements the abstract method Widget.setCallbacks().
   * It defines the callbacks for the widget as:
   *    UPDATE with the attributes TIMESTAMP, USERID, LOCATION
   *
   * @return the Callbacks used by this widget
   */
  protected Callbacks setCallbacks() {
    Callbacks calls = new Callbacks();
    calls.addCallback(UPDATE,setAttributes());
    return calls;
  }

  /**
   * This method implements the abstract method Widget.setServices().
   * It currently has no services and returns an empty Services object.
   *
   * @return the Services provided by this widget
   */
  protected Services setServices() {
    return new Services();
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
    AttributeNameValues atts = IButtonData2Attributes((IButtonData)data);
    if (atts != null) {      
      if (subscribers.numSubscribers() > 0) {
        sendToSubscribers(event, atts);
      }
      store(atts);
    }
  }

  /**
   * This method converts the IButtonData object to an AttributeNameValues
   * object.
   *
   * @param data IButtonData object to be converted
   * @return AttributeNameValues object containing the data in the IButtonData object
   */
  protected AttributeNameValues IButtonData2Attributes(IButtonData data) {
    AttributeNameValues atts = new AttributeNameValues();
    if (data.getId().length() > 0) {
      atts.addAttributeNameValue(USERID,data.getId());
    }
    atts.addAttributeNameValue(LOCATION,data.getLocation());
    atts.addAttributeNameValue(TIMESTAMP,data.getTimestamp(), Attribute.LONG);
    return atts;
  }
    
  /**
   * This method returns an AttributeNameValues object with the latest iButton data.
   *
   * @return AttributeNameValues containing the latest data
   */
  protected AttributeNameValues queryGenerator() {
    IButtonData result = ibutton.pollData();
    if (result == null) {
      return null;
    }
    if (result.getId() == null) {
      return null;
    }
    return IButtonData2Attributes(result);
  }


  /**
   * Temporary main method to create a widget with location and port specified by 
   * command line arguments
   */
  public static void main(String argv[]) {
    if (argv.length == 1) {
      if (DEBUG) {
        System.out.println("Attempting to create a WPersonPresence on "+DEFAULT_PORT+" at " +argv[0]+" with storage enabled");
      }
      WPersonPresence wpp = new WPersonPresence(argv[0]);
    }
    else if (argv.length == 2) {
      if ((argv[1].equals("false")) || (argv[1].equals("true"))) {
        if (DEBUG) {
          System.out.println("Attempting to create a WPersonPresence on "+DEFAULT_PORT+" at " +argv[0]+" with storage set to "+argv[1]);
        }
        WPersonPresence wpp = new WPersonPresence(argv[0], Boolean.valueOf(argv[1]).booleanValue()); 
      }
      else {
        if (DEBUG) {
          System.out.println("Attempting to create a WPersonPresence on "+argv[1]+" at " +argv[0]+" with storage enabled");
        }
        WPersonPresence wpp = new WPersonPresence(argv[0], Integer.parseInt(argv[1]));
      }
    }
    else if (argv.length == 3) {
      if (DEBUG) {
        System.out.println("Attempting to create a WPersonPresence on "+argv[1]+" at " +argv[0]+" with storage set to "+argv[2]);
      }
      WPersonPresence wpp = new WPersonPresence(argv[0], Integer.parseInt(argv[1]), Boolean.valueOf(argv[2]).booleanValue());
    }
    else {
      System.out.println("USAGE: java context.arch.widget.WPersonPresence <location> [port]");
    }
  }

}
