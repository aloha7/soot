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
 * information is in the form of a location, user id and user
 * contact information.  It
 * has the following callbacks: UPDATE. It supports polling and
 * subscriptions.  Currently it only uses the IButton as a means
 * of providing presence.  It handles only a single IButton
 * instance.
 *
 * @see context.arch.widget.Widget
 * @see context.arch.generator.PositionIButton
 */
public class WContactPresence extends Widget {

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
   * Tag for contact info
   */
  public static final String CONTACT_INFO = ContextTypes.CONTACT_INFO;

  /**
   * Tag for contact info - name
   */
  public static final String NAME = ContextTypes.NAME;

  /**
   * Tag for contact info - email
   */
  public static final String EMAIL = ContextTypes.EMAIL;

  /**
   * Name of widget
   */
  public static final String CLASSNAME = "ContactPresence";

  /**
   * The default port this widget runs on is 5400
   */
  public static final int DEFAULT_PORT = 5400;

  private String location;
  private String userid;
  protected PositionIButton ibutton;
	
  /**
   * Constructor that creates the widget at the given location and
   * monitors communications on the DEFAULT_PORT and
   * creates an instance of the IButton position generator.  It also
   * sets the id of this widget to CLASSNAME_<location value> and enables
   * storage functionality.
   *  
   * @param location Location the widget is "monitoring"
   *
   * @see context.arch.generator.PositionIButton
   */
  public WContactPresence (String location) {
    this(location, DEFAULT_PORT, true);
  }

  /**
   * Constructor that creates the widget at the given location and
   * monitors communications on the DEFAULT_PORT and
   * creates an instance of the IButton position generator.  It also
   * sets the id of this widget to CLASSNAME_<location value> and sets the
   * storage functionality to storageFlag.
   *  
   * @param location Location the widget is "monitoring"
   * @param storageFlag Flag to set whether storage is used or not
   *
   * @see context.arch.generator.PositionIButton
   */
  public WContactPresence (String location, boolean storageFlag) {
    this(location, DEFAULT_PORT, storageFlag);
  }

  /**
   * Constructor that creates the widget at the given location and 
   * monitors communications on the given port and
   * creates an instance of the IButton position generator.  It also
   * sets the id of this widget to CLASSNAME_<location value> and enables
   * storage functionality.
   *  
   * @param location Location the widget is "monitoring"
   * @param port Port to run the widget on
   *
   * @see context.arch.generator.PositionIButton
   */
  public WContactPresence (String location, int port) {
    this(location,port,CLASSNAME+SPACER+location, true);
  }

  /**
   * Constructor that creates the widget at the given location and 
   * monitors communications on the given port and
   * creates an instance of the IButton position generator.  It also
   * sets the id of this widget to CLASSNAME_<location value> and sets the
   * storage functionality to storageFlag.
   *  
   * @param location Location the widget is "monitoring"
   * @param port Port to run the widget on
   * @param storageFlag Flag to set whether storage functionality is enabled or not
   *
   * @see context.arch.generator.PositionIButton
   */
  public WContactPresence (String location, int port, boolean storageFlag) {
    this(location,port,CLASSNAME+SPACER+location, storageFlag);
  }
  
  /**
   * Constructor that creates the widget at the given location and 
   * monitors communications on the given port and
   * creates an instance of the IButton position generator.  It also
   * sets the id of this widget to the given id and sets the
   * storage functionality to storageFlag.
   *  
   * @param location Location the widget is "monitoring"
   * @param port Port to run the widget on
   * @param id Widget id
   * @param storageFlag Flag to set whether storage functionality is enabled or not
   *
   * @see context.arch.generator.PositionIButton
   */
  public WContactPresence (String location, int port, String id, boolean storageFlag) {
    super(port,id,storageFlag);
    setVersion(VERSION_NUMBER);
    this.location = location;
    //2008/7/9
    //ibutton = new PositionIButton (this,location);
    ibutton = PositionIButton.getInstance();
  }

  /**
   * This method implements the abstract method Widget.setAttributes().
   * It defines the attributes for the widget as:
   *    TIMESTAMP, USERID, LOCATION, and CONTACT_INFO(NAME,EMAIL)
   *
   * @return the Attributes used by this widget
   */
  protected Attributes setAttributes() {
    Attributes atts = new Attributes();
    atts.addAttribute(TIMESTAMP,Attribute.LONG);
    atts.addAttribute(USERID);
    atts.addAttribute(LOCATION);
    Attributes contact = new Attributes();
    contact.addAttribute(NAME);
    contact.addAttribute(EMAIL);
    atts.addAttribute(CONTACT_INFO,contact,Attribute.STRUCT);
    return atts;
  }
  
  /**
   * This method implements the abstract method Widget.setCallbacks().
   * It defines the callbacks for the widget as:
   *    UPDATE with the attributes TIMESTAMP, USERNAME, LOCATION, CONTACT_INFO (NAME,EMAIL)
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
   * object, adds the user contact information and returns it
   *
   * @param data IButtonData object to be converted
   * @return AttributeNameValues object containing the data in the IButtonData object and user
   *         contact info
   */
  protected AttributeNameValues IButtonData2Attributes(IButtonData data) {
    AttributeNameValues atts = new AttributeNameValues();
    atts.addAttributeNameValue(USERID,data.getId());
    atts.addAttributeNameValue(LOCATION,data.getLocation());
    atts.addAttributeNameValue(TIMESTAMP,data.getTimestamp(), Attribute.LONG);
    AttributeNameValues contact = new AttributeNameValues();
    contact.addAttributeNameValue(NAME,"Anind");
    contact.addAttributeNameValue(EMAIL,"anind@cc.gatech.edu");
    atts.addAttributeNameValue(CONTACT_INFO,contact,Attribute.STRUCT);
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
        System.out.println("Attempting to create a WContactPresence on "+DEFAULT_PORT+" at " +argv[0]+ " with storage enabled");
      }
      WContactPresence wcp = new WContactPresence(argv[0]);
    }
    else if (argv.length == 2) {
      if ((argv[1].equals("false")) || (argv[1].equals("true"))) {
        if (DEBUG) {
          System.out.println("Attempting to create a WContactPresence on "+DEFAULT_PORT+" at " +argv[0]+ "with storage set to "+argv[1]);
        }
        WContactPresence wcp = new WContactPresence(argv[0], Boolean.valueOf(argv[1]).booleanValue());
      }
      else {
        if (DEBUG) {
          System.out.println("Attempting to create a WContactPresence on "+argv[1]+" at " +argv[0]+ " with storage enabled");
        }
        WContactPresence wcp = new WContactPresence(argv[0], Integer.parseInt(argv[1]));
      }
    }
    else if (argv.length == 3) {
      if (DEBUG) {
        System.out.println("Attempting to create a WContactPresence on "+argv[1]+" at " +argv[0]+" with storage set to "+argv[2]);
      }
      WContactPresence wcp = new WContactPresence(argv[0], Integer.parseInt(argv[1]), Boolean.valueOf(argv[2]).booleanValue());
    }
    else {
      System.out.println("USAGE: java context.arch.widget.WContactPresence <location> [port] [storageFlag]");
    }
  }

}
