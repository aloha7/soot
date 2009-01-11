package context.arch.widget;

import context.arch.comm.DataObject;
import context.arch.generator.IButtonData;
import context.arch.interpreter.IIButton2Name;
import context.arch.subscriber.Callbacks;
import context.arch.storage.Attributes;
import context.arch.storage.Attribute;
import context.arch.storage.AttributeNameValues;

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
public class WPersonNamePresence extends WPersonPresence {

  /**
   * Debug flag. Set to true to see debug messages.
   */
  private static final boolean DEBUG = false;

  /**
   * Widget version number
   */
  public String VERSION_NUMBER = "1.0.0";

  /**
   * Name of widget
   */
  public static final String CLASSNAME = "PersonNamePresence";

  /**
   * Tag to indicate the location is Mobile SMARTBoard #1
   */
  public static final String MOBILE_SMARTBOARD1 = "Mobile SMARTBoard #1";

  /**
   * The default port this widget runs on is 5200
   */
  public static final int DEFAULT_PORT = 5200;

  /**
   * Tag to indicate user name
   */ 
  public static final String USERNAME = IIButton2Name.USERNAME;

  private String ibnihost;
  private int ibniport;

  /**
   * Constructor that creates the widget at the given location and
   * monitors communications on the DEFAULT_PORT and
   * creates an instance of the IButton position generator.  It also
   * sets the id of this widget to CLASSNAME_<location value> and enables storage.
   *  
   * @param location Location the widget is "monitoring"
   * @param ihost Hostname/ip the IIButton2Name is running on
   * @param iport Port the IIButton2Name is running on
   *
   * @see context.arch.generator.PositionIButton
   */
  public WPersonNamePresence (String location, String ihost, int iport) {
    this(location, DEFAULT_PORT, ihost, iport, true);
  }

  /**
   * Constructor that creates the widget at the given location and
   * monitors communications on the DEFAULT_PORT and
   * creates an instance of the IButton position generator.  It also
   * sets the id of this widget to CLASSNAME_<location value> and sets
   * storage functionality to storageFlag
   *  
   * @param location Location the widget is "monitoring"
   * @param ihost Hostname/ip the IIButton2Name is running on
   * @param iport Port the IIButton2Name is running on
   * @param storageFlag Flag to indicate whether storage is enabled or not
   *
   * @see context.arch.generator.PositionIButton
   */
  public WPersonNamePresence (String location, String ihost, int iport, boolean storageFlag) {
    this(location, DEFAULT_PORT, ihost, iport, storageFlag);
  }

  /**
   * Constructor that creates the widget at the given location and 
   * monitors communications on the given port and
   * creates an instance of the IButton position generator.  It also
   * sets the id of this widget to CLASSNAME_<location value> and enables storage
   *  
   * @param location Location the widget is "monitoring"
   * @param port Port to run the widget on
   * @param ihost Hostname/ip the IIButton2Name is running on
   * @param iport Port the IIButton2Name is running on
   *
   * @see context.arch.generator.PositionIButton
   */
  public WPersonNamePresence (String location, int port, String ihost, int iport) {
    this(location,port,ihost,iport,CLASSNAME+SPACER+location, true);
  }

  /**
   * Constructor that creates the widget at the given location and 
   * monitors communications on the given port and
   * creates an instance of the IButton position generator.  It also
   * sets the id of this widget to CLASSNAME_<location value> and 
   * sets storage functionality to storageFlag
   *  
   * @param location Location the widget is "monitoring"
   * @param port Port to run the widget on
   * @param ihost Hostname/ip the IIButton2Name is running on
   * @param iport Port the IIButton2Name is running on
   * @param storageFlag Flag to indicate whether storage is enabled or not
   *
   * @see context.arch.generator.PositionIButton
   */
  public WPersonNamePresence(String location, int port, String ihost, int iport, boolean storageFlag) {
    this(location,port,ihost,iport,CLASSNAME+SPACER+location, storageFlag);
  }

  /**
   * Constructor that creates the widget at the given location and 
   * monitors communications on the given port and
   * creates an instance of the IButton position generator.  It also
   * sets the id of this widget to CLASSNAME_<location value>, and sets
   * storage functionality to storageFlag.
   *  
   * @param location Location the widget is "monitoring"
   * @param port Port to run the widget on
   * @param ihost Hostname/ip the IIButton2Name is running on
   * @param iport Port the IIButton2Name is running on
   * @param id Widget id
   * @param storageFlag Flag to indicate whether storage is enabled or not
   *
   * @see context.arch.generator.PositionIButton
   */
  public WPersonNamePresence (String location, int port, String ihost, int iport, String id, boolean storageFlag) {
    super(location,port,id, storageFlag);
    setVersion(VERSION_NUMBER);
    ibnihost = ihost;
    ibniport = iport;
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
  
  /**
   * This method implements the abstract method Widget.setCallbacks().
   * It defines the callbacks for the widget as:
   *    UPDATE with the attributes TIMESTAMP, USERNAME, LOCATION
   *
   * @return the Callbacks used by this widget
   */
  protected Callbacks setCallbacks() {
    Callbacks calls = new Callbacks();
    calls.addCallback(UPDATE,setAttributes()); //One CallBack can trace more than one attribute
    return calls;
  }

  /**
   * This method converts the IButtonData object to an AttributeNameValues
   * object.  It overrides the method in WPersonPresence, basically
   * doing the same thing, except it also uses an interpreter to 
   * convert the USERID to a USERNAME
   *
   * @param data IButtonData object to be converted
   * @return AttributeNameValues object containing the data in the IButtonData object
   */
  protected AttributeNameValues IButtonData2Attributes(IButtonData data) {
    AttributeNameValues atts = new AttributeNameValues();
    DataObject nameObject = askIButton2Name(data.getId());
    if (nameObject == null) {
      return null;
    }

    AttributeNameValues anvs = new AttributeNameValues(nameObject);
    String name = (String)anvs.getAttributeNameValue(USERNAME).getValue();
    if (name == null) {
      return null;
    }
    atts.addAttributeNameValue(USERNAME,name);
    atts.addAttributeNameValue(LOCATION,data.getLocation());
    atts.addAttributeNameValue(TIMESTAMP,data.getTimestamp(), Attribute.LONG);
    return atts;
  }

  /**
   * This private method asks the IIButton2Name for a name for the given iButton id.
   *
   * @param buttonid iButton id that needs translating
   * @return DataObject containing the results of the interpretation
   */
  private DataObject askIButton2Name(String buttonid) {
    AttributeNameValues data = new AttributeNameValues();
    data.addAttributeNameValue(IIButton2Name.IBUTTONID, buttonid);
    return askInterpreter(ibnihost,ibniport,IIButton2Name.CLASSNAME,data);
  }


  /**
   * Main method to create a widget with location and port specified by 
   * command line arguments
   */
  public static void main(String argv[]) {
    if (argv.length == 3) {
      if (DEBUG) {
        System.out.println("Attempting to create a WPersonNamePresence on "+DEFAULT_PORT+" at " +argv[0]+" with storage enabled");
      }
      WPersonNamePresence wpnp = new WPersonNamePresence(argv[0], argv[1], Integer.parseInt(argv[2]));
    }
    else if (argv.length == 4) {
      if ((argv[3].equals("false")) || (argv[3].equals("true"))) {
        if (DEBUG) {
          System.out.println("Attempting to create a WPersonNamePresence on "+DEFAULT_PORT+" at " +argv[0]+" with storage set to "+argv[3]);
        }
        WPersonNamePresence wpnp = new WPersonNamePresence(argv[0], argv[1], Integer.parseInt(argv[2]), Boolean.valueOf(argv[3]).booleanValue());
      }
      else {
        if (DEBUG) {
          System.out.println("Attempting to create a WPersonNamePresence on "+argv[1]+" at " +argv[0]+ "with storage enabled");
        }
        WPersonNamePresence wpnp = new WPersonNamePresence(argv[0], Integer.parseInt(argv[1]), argv[2], Integer.parseInt(argv[3]));
      }
    }
    else if (argv.length == 5) {
      if (DEBUG) {
        System.out.println("Attempting to create a WPersonNamePresence on "+argv[1]+" at " +argv[0]+ "with storage set to: "+argv[4]);
      }
      WPersonNamePresence wpnp = new WPersonNamePresence(argv[0], Integer.parseInt(argv[1]), argv[2], Integer.parseInt(argv[3]), Boolean.valueOf(argv[4]).booleanValue());
    }
    else {
      System.out.println("USAGE: java context.arch.widget.WPersonNamePresence <location> [port] <IIButton2Name-host/ip> <IIButton2Name-port> [storageFlag]");
    }
  }

}
