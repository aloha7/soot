package context.arch.widget;

import context.arch.comm.DataObject;
import context.arch.generator.IButtonData;
import context.arch.interpreter.IIButton2NameExt;
import context.arch.subscriber.Callbacks;
import context.arch.storage.Conditions;
import context.arch.storage.AttributeFunctions;
import context.arch.storage.AttributeFunction;
import context.arch.storage.Retrieval;
import context.arch.storage.RetrievalResults;
import context.arch.storage.Attributes;
import context.arch.storage.Attribute;
import context.arch.storage.AttributeNameValues;
import context.arch.storage.AttributeNameValue;
import context.arch.storage.Storage;
import context.arch.util.Error;
import context.arch.util.ContextTypes;

import java.util.Hashtable;

/**
 * This class is a context widget that provides information on 
 * the in/out status of a person in a particular location.  The
 * information is in the form of a location, user name and in/out
 * status.  It has the following callbacks: UPDATE. It supports 
 * polling and subscriptions.  Currently it only uses the IButton 
 * as a means of providing presence.  It handles only a single IButton
 * instance. 
 *
 * @see context.arch.widget.Widget
 * @see context.arch.widget.WPersonNamePresence
 * @see context.arch.generator.PositionIButton
 */
public class WPersonNameInOut extends WPersonNamePresence {

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
  public static final String CLASSNAME = "PersonNameInOut";

  /**
   * The default port this widget runs on is 5200
   */
  public static final int DEFAULT_PORT = 5200;

  /**
   * Tag to indicate user name
   */ 
  public static final String USERNAME = IIButton2NameExt.USERNAME;

  /**
   * Tag to indicate in/out status
   */ 
  public static final String INOUT = ContextTypes.INOUT;

  /**
   * Tag to indicate out
   */ 
  public static final String OUT = ContextTypes.OUT;

  /**
   * Tag to indicate in
   */ 
  public static final String IN = ContextTypes.IN;

  private Hashtable inoutHash;

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
  public WPersonNameInOut (String location, String ihost, int iport) {
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
  public WPersonNameInOut (String location, String ihost, int iport, boolean storageFlag) {
    this(location, DEFAULT_PORT, ihost, iport, storageFlag);
  }

  /**
   * Constructor that creates the widget at the given location and 
   * monitors communications on the given port and
   * creates an instance of the IButton position generator.  It also
   * sets the id of this widget to CLASSNAME_<location value> and enables storage.
   *  
   * @param location Location the widget is "monitoring"
   * @param port Port to run the widget on
   * @param ihost Hostname/ip the IIButton2Name is running on
   * @param iport Port the IIButton2Name is running on
   *
   * @see context.arch.generator.PositionIButton
   */
  public WPersonNameInOut (String location, int port, String ihost, int iport) {
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
  public WPersonNameInOut (String location, int port, String ihost, int iport, boolean storageFlag) {
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
  public WPersonNameInOut (String location, int port, String ihost, int iport, String id, boolean storageFlag) {
    super(location,port,ihost,iport,id, storageFlag);
    setVersion(VERSION_NUMBER);
    inoutHash = new Hashtable();
    
    // get data from database
    AttributeFunctions afs = new AttributeFunctions();
    afs.addAttributeFunction(WPNIOSonic.TIMESTAMP,Attribute.LONG,AttributeFunction.FUNCTION_MAX);
    afs.addAttributeFunction(WPNIOSonic.INOUT);

    DataObject ul = runComponentMethod(ihost,iport,IIButton2NameExt.CLASSNAME,IIButton2NameExt.LISTUSERSNAMES,null,null);
    AttributeNameValues atts = new AttributeNameValues(ul);
    for (int i=0; i<atts.numAttributeNameValues(); i++) {
      AttributeNameValue anv = atts.getAttributeNameValueAt(i);
      String name = (String)atts.getAttributeNameValueAt(i).getValue();
      Conditions conds = new Conditions();
      conds.addCondition(WPNIOSonic.USERNAME,Storage.EQUAL,name);
      Retrieval retrieval = new Retrieval(afs,conds);
      DataObject retrieve = retrieveDataFrom("127.0.0.1", port, id, retrieval);
      String retrieveError = new Error(retrieve).getError();
      if (retrieveError.equals(Error.NO_ERROR)) {
        RetrievalResults retrieveData = new RetrievalResults(retrieve);
        if (retrieveData != null) {
          AttributeNameValues anvs = retrieveData.getAttributeNameValuesAt(0);
          AttributeNameValue status = anvs.getAttributeNameValue(WPNIOSonic.INOUT);
          inoutHash.put(name,(String)status.getValue());
          if (DEBUG) {
            System.out.println(name+": "+(String)status.getValue());
          }
        }
      }
    }
    System.out.println("Done loading widget");
  }

  /**
   * This method implements the abstract method Widget.setAttributes().
   * It defines the attributes for the widget as:
   *    TIMESTAMP, USERNAME, and LOCATION
   *
   * @return the Attributes used by this widget
   */
  protected Attributes setAttributes() {
    Attributes atts = super.setAttributes();
    atts.addAttribute(ContextTypes.INOUT);
    return atts;
  }
  
  /**
   * This method converts the IButtonData object to an AttributeNameValues
   * object.  It overrides the method in WPersonNamePresence, basically
   * doing the same thing, except it also determines whether the person's
   * status is in or out.
   *
   * @param data IButtonData object to be converted
   * @return AttributeNameValues object containing the data in the IButtonData object
   */
  protected AttributeNameValues IButtonData2Attributes(IButtonData data) {
    AttributeNameValues atts = super.IButtonData2Attributes(data);
    if (atts == null) {
      return null;
    }

    String name = (String)atts.getAttributeNameValue(USERNAME).getValue();
    if (inoutHash.containsKey(name)) {
      if (((String)inoutHash.get(name)).equals(IN)) {
        inoutHash.put(name,OUT);
        atts.addAttributeNameValue(INOUT,OUT);
      }
      else {
        inoutHash.put(name,IN);
        atts.addAttributeNameValue(INOUT,IN);
      }
    }
    else {
      inoutHash.put(name,IN);
      atts.addAttributeNameValue(INOUT,IN);
    }

    return atts;
  }

  /**
   * Main method to create a widget with location and port specified by 
   * command line arguments
   */
  public static void main(String argv[]) {
    if (argv.length == 3) {
      if (DEBUG) {
        System.out.println("Attempting to create a WPersonNameInOut on "+DEFAULT_PORT+" at " +argv[0]+" with storage enabled");
      }
      WPersonNameInOut wpnio = new WPersonNameInOut(argv[0], argv[1], Integer.parseInt(argv[2]));
    }
    else if (argv.length == 4) {
      if ((argv[3].equals("false")) || (argv[3].equals("true"))) {
        if (DEBUG) {
          System.out.println("Attempting to create a WPersonNameInOut on "+DEFAULT_PORT+" at " +argv[0]+" with storage set to "+argv[3]);
        }
        WPersonNameInOut wpnio = new WPersonNameInOut(argv[0], argv[1], Integer.parseInt(argv[2]), Boolean.valueOf(argv[3]).booleanValue());
      }
      else {
        if (DEBUG) {
          System.out.println("Attempting to create a WPersonNameInOut on "+argv[1]+" at " +argv[0]+ "with storage enabled");
        }
        WPersonNameInOut wpnio = new WPersonNameInOut(argv[0], Integer.parseInt(argv[1]), argv[2], Integer.parseInt(argv[3]));
      }
    }
    else if (argv.length == 5) {
      if (DEBUG) {
        System.out.println("Attempting to create a WPersonNameInOut on "+argv[1]+" at " +argv[0]+ "with storage set to: "+argv[4]);
      }
      WPersonNameInOut wpnio = new WPersonNameInOut(argv[0], Integer.parseInt(argv[1]), argv[2], Integer.parseInt(argv[3]), Boolean.valueOf(argv[4]).booleanValue());
    }
    else {
      System.out.println("USAGE: java context.arch.widget.WPersonNameInOut <location> [port] <IIButton2Name-host/ip> <IIButton2Name-port> [storageFlag]");
    }
  }

}
