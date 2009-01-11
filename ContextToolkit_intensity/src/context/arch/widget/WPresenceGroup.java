package context.arch.widget;

import context.arch.comm.DataObject;
import context.arch.interpreter.IIButton2Group;
import context.arch.subscriber.Callbacks;
import context.arch.generator.IButtonData;
import context.arch.storage.Attributes;
import context.arch.storage.Attribute; 
import context.arch.storage.AttributeNameValues;

/**
 * This class is a context widget that provides information on 
 * the preferences of a person whose presence has been detected
 * at a specified location.  In particular, this information is
 * the user id (IButton id), location, and group.
 * It has the following callbacks: UPDATE. It supports polling and
 * subscriptions.  It subscribes to a WPersonPresence at the
 * the specified location and uses the IIButton2Group.
 *
 * @see context.arch.widget.Widget
 * @see context.arch.widget.WPersonPresence
 * @see context.arch.interpreter.IIButton2Group
 */
public class WPresenceGroup extends WPersonPresence {
 
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
  public static final String CLASSNAME = "PresenceGroup";

  /**
   * Tag for url of user's homepage
   */
  public static final String GROUP = IIButton2Group.GROUP;

  /**
   * Default port to run this widget on is 5500
   */
  public static final int DEFAULT_PORT = 5500;

  private String iHost;
  private int iPort;
  private String location;
  
  /**
   * Constructor that creates the widget at the given location and
   * monitors communications on the DEFAULT_PORT.  
   * It sets the id of this widget to CLASSNAME_<location value> and enables storage.
   *  
   * @param location Location the widget is "monitoring"
   * @param ihost Name of the machine the IButton2Group interpreter is running on
   * @param iport Port number the IButton2Group interpreter is running on
   *
   * @see context.arch.widget.WPersonPresence
   */
  public WPresenceGroup(String location, String ihost, int iport) {
    this(location,DEFAULT_PORT,ihost,iport,true);
  }

  /**
   * Constructor that creates the widget at the given location and
   * monitors communications on the DEFAULT_PORT.  
   * It sets the id of this widget to CLASSNAME_<location value> and sets storage
   * to storageFlag.
   *  
   * @param location Location the widget is "monitoring"
   * @param ihost Name of the machine the IButton2Group interpreter is running on
   * @param iport Port number the IButton2Group interpreter is running on
   * @param storageFlag Flag to set storage functionality to
   *
   * @see context.arch.widget.WPersonPresence
   */
  public WPresenceGroup(String location, String ihost, int iport, boolean storageFlag) {
    this(location,DEFAULT_PORT,ihost,iport,storageFlag);
  }

  /**
   * Constructor that creates the widget at the given location and
   * monitors communications on the given port.  
   * It sets the id of this widget to CLASSNAME_<location value> and enables storage.
   *  
   * @param location Location the widget is "monitoring"
   * @param localport Port number this widget is running on
   * @param ihost Name of the machine the IIButton2Group is running on
   * @param iport Port number the IIButton2Group is running on
   *
   * @see context.arch.widget.WPersonPresence
   */
  public WPresenceGroup(String location, int localport, String ihost, int iport) {
    this(location, localport, ihost, iport, true);
  }

  /**
   * Constructor that creates the widget at the given location and
   * monitors communications on the given port.  
   * It sets the id of this widget to CLASSNAME_<location value> and sets storage
   * functionality to storageFlag.
   *  
   * @param location Location the widget is "monitoring"
   * @param localport Port number this widget is running on
   * @param ihost Name of the machine the IIButton2Group is running on
   * @param iport Port number the IIButton2Group is running on
   * @param storageFlag Flag to indicate whether or not to enable storage
   *
   * @see context.arch.widget.WPersonPresence
   */
  public WPresenceGroup(String location, int localport, String ihost, int iport, boolean storageFlag) {
    super(location, localport, CLASSNAME+SPACER+location, storageFlag);
    setVersion(VERSION_NUMBER);
    iHost = ihost;
    iPort = iport;
    this.location = location;
  }

  /**
   * This method implements the abstract method Widget.setAttributes().
   * It defines the attributes for the widget as:
   *    TIMESTAMP, USERID, LOCATION, and GROUP
   *
   * @return the Attributes used by this widget
   */
  protected Attributes setAttributes() {
    Attributes atts = super.setAttributes(); //Here it initialized TIMESTAMP, USERID, LOCATION
    atts.addAttribute(GROUP); //Here it initialized GROUP
    return atts;
  }
  
  /**
   * This method implements the abstract method Widget.setCallbacks().
   * It defines the callbacks for the widget as:
   *    UPDATE with the attributes TIMESTAMP, USERID, LOCATION, GROUP
   *
   * @return the Callbacks used by this widget
   */
  protected Callbacks setCallbacks() {
    Callbacks calls = new Callbacks();
    calls.addCallback(UPDATE,setAttributes());
    return calls;
  }

  /**
   * This method converts the IButtonData object to an Attributes
   * object.  It overrides the method in WPersonPresence, basically
   * doing the same thing, except it also uses an interpreter to 
   * convert the USERID to a GROUP
   *
   * @param data IButtonData object to be converted
   * @return Attributes object containing the data in the IButtonData object
   */
  protected AttributeNameValues IButtonData2Attributes(IButtonData data) {
    AttributeNameValues atts = new AttributeNameValues();
    DataObject groupObject = askIButton2Group(data.getId());
    if (groupObject == null) {
      return null;
    }

    AttributeNameValues anvs = new AttributeNameValues(groupObject);
    String group = (String)anvs.getAttributeNameValue(GROUP).getValue();
    if (group == null) {
      return null;
    }

    atts.addAttributeNameValue(USERID,data.getId());
    atts.addAttributeNameValue(LOCATION,location);
    atts.addAttributeNameValue(TIMESTAMP,data.getTimestamp(),Attribute.LONG);
    atts.addAttributeNameValue(GROUP,group);
    return atts;
  }
    
  /**
   * This private method asks the IIButton2Group for a  
   * group for the given iButton id.
   *
   * @param userid iButton id to get the  group for
   * @return DataObject containing the results of the interpretation
   */
  private DataObject askIButton2Group(String userid) {
    AttributeNameValues data = new AttributeNameValues();
    data.addAttributeNameValue(IIButton2Group.IBUTTONID, userid);
    return askInterpreter(iHost,iPort,IIButton2Group.CLASSNAME,data);
  }

  
  /**
   * Temporary main method to create a widget with id and port and connection to
   * other low level widgets specified by command line arguments
   */
  public static void main(String argv[]) {
    if (argv.length == 3) {
      if (DEBUG) {
        System.out.println("Attempting to create a WPresenceGroup on "+DEFAULT_PORT+" at "+argv[0]+" with storage enabled");
      }
      WPresenceGroup wpg = new WPresenceGroup(argv[0], argv[1], Integer.parseInt(argv[2]));
    }
    else if (argv.length == 4) {
      if ((argv[3].equals("false")) || (argv[3].equals("true"))) {
        if (DEBUG) {
          System.out.println("Attempting to create a WPresenceGroup on "+argv[1]+" for user "+argv[0]+" with storage set to" +argv[3]);
        }
        WPresenceGroup wpg = new WPresenceGroup(argv[0], argv[1], Integer.parseInt(argv[2]), Boolean.valueOf(argv[3]).booleanValue());
      }
      else {
        if (DEBUG) {
          System.out.println("Attempting to create a WPresenceGroup on "+argv[1]+" for user "+argv[0]+" with storage enabled");
        }
        WPresenceGroup wpg = new WPresenceGroup(argv[0], Integer.parseInt(argv[1]), argv[2], Integer.parseInt(argv[3]));
      }
    }
    else if (argv.length == 5) {
      if (DEBUG) {
        System.out.println("Attempting to create a WPresenceGroup on "+argv[1]+" for user "+argv[0] +" with storage set to "+argv[4]);
      }
      WPresenceGroup wpg = new WPresenceGroup(argv[0], Integer.parseInt(argv[1]), argv[2], Integer.parseInt(argv[3]), Boolean.valueOf(argv[4]).booleanValue());
    }
    else {
      System.out.println("USAGE: java context.arch.widget.WPresenceGroup <location> [port] <IIButton2GroupHost/ip> <IIButton2GroupPort> [storageflag]");
    }
  }

}
