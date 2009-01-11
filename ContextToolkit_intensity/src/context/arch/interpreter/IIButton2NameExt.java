package context.arch.interpreter;

import context.arch.comm.DataObject;
import context.arch.comm.language.DecodeException;
import context.arch.storage.Attributes;
import context.arch.storage.AttributeNameValues;
import context.arch.util.Error;
import context.arch.util.ContextTypes;
import context.arch.util.XMLURLReader;
import context.arch.util.ContextUsers;
import context.arch.util.ContextUser;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import java.net.MalformedURLException;

/**
 * This class is an interpreter that converts iButton ids to names.
 * It looks up correspondences between ids and names from an XML web page
 * given as an argument. It expects the XML page to comply with the 
 * context-users DTD.
 * Another component (app/widget/interpreter) sends an interpret command
 * to it with an ibutton id, and this class returns the corresponding name.
 *
 * @see context.arch.interpreter.Interpreter
 */
public class IIButton2NameExt extends Interpreter {
  
  /**
   * Debug flag. Set to true to see debug messages.
   */
  private static final boolean DEBUG = false;

  /**
   * Name of interpreter
   */
  public static final String CLASSNAME = "IButton2Name";

  /**
   * Tag for requesting the list of all users names
   */
  public static final String LISTUSERSNAMES = "ListUsersNames";

  /**
   * Tag for reply to request for the list of all users names
   */
  public static final String LISTUSERSNAMES_REPLY = "ListUsersNamesReply";

  /**
   * Tag for iButton id
   */
  public static final String IBUTTONID = ContextTypes.IBUTTONID;

  /**
   * Tag for username
   */
  public static final String USERNAME = ContextTypes.USERNAME;

  /**
   * Default port to use to receive communications
   */
  public static final int DEFAULT_PORT = 8888;

  /**
   * Default port to use to receive communications
   */
  public static final String DEFAULT_USERSLIST = "http://www.cc.gatech.edu/fce/contexttoolkit/config/global/fcl-users.xml";

  /**
   * String for Dummbo
   */
  public static final String DUMMBO = "Dummbo";

  private ContextUsers users = null;
  private Hashtable ibuttonsHash = new Hashtable();

  /**
   * Constructor that creates the interpreter at the default port and using
   * the default users list.  
   */
  public IIButton2NameExt() {
    this(DEFAULT_PORT,DEFAULT_USERSLIST);
  }

  /**
   * Constructor that creates the interpreter at the given port and using
   * the default users list.
   */
  public IIButton2NameExt(int port) {
    this(port,DEFAULT_USERSLIST);
  }

  /**
   * Constructor that creates the interpreter at the default port and using
   * the given url for the users list.
   */
  public IIButton2NameExt(String url) {
    this(DEFAULT_PORT,url);
  }

  /**
   * Constructor that creates the interpreter at the given port and with the given
   * url for the users list.  It sets the id of the this interpreter to CLASSNAME.
   */
  public IIButton2NameExt(int port, String url) {
    super(port);
    setId(CLASSNAME);
    if (DEBUG) {
      System.out.println ("IIButton2NameExt constructor called. About to parse...");
    }
    // grab and parse the list of users as a XML file
    try {
      XMLURLReader reader = new XMLURLReader(url);
      DataObject data = reader.getParsedData();
      users = new ContextUsers(data);
      for (Enumeration e=users.getEnumeration(); e.hasMoreElements(); ) {
        ContextUser user = (ContextUser)e.nextElement();
        ibuttonsHash.put(user.getIButtonId(),user.getName());
      }  
    } catch (MalformedURLException mue) {
        System.out.println ("while parsing the users list IIButton2NameExt got a MalformedURL: " + mue);
    } catch (DecodeException de) {
        System.out.println ("while parsing the users list IIButton2NameExt got a DecodeException: " + de);
    }
  }

  /**
   * This method performs the actual interpretation of this component.
   * It takes an iButton id and returns a user name.
   *
   * @param data AttributeNameValues containing data to be interpreted
   * @return AttributeNameValues object containing the interpreted data
   */
  protected AttributeNameValues interpretData(AttributeNameValues data) {
    String buttonid = (String)data.getAttributeNameValue(IBUTTONID).getValue();
    String name = (String)ibuttonsHash.get(buttonid);
    if (DEBUG) {
      System.out.println ("Converted " + buttonid + " to " + name);
    }
    if (name == null) {
      return null;
    }
    AttributeNameValues atts = new AttributeNameValues();
    atts.addAttributeNameValue(USERNAME,name);
    return atts;
  }

  /**
   * This is method that handles methods for this Interpreter that
   * are no interpreter requests.
   *
   * @param data DataObject containing the data for the method
   * @param error String containing the incoming error value
   * @return DataObject containing the method results
   */
  protected DataObject runInterpreterMethod(DataObject data, String error) {
    String name = data.getName();
    Error err = new Error(error);
    if (name.equals(LISTUSERSNAMES)) {
      Vector v = new Vector ();
      DataObject result = new DataObject(LISTUSERSNAMES_REPLY,v);
      if (err.getError() == null) {
        AttributeNameValues names = new AttributeNameValues();
        for (Enumeration e = ibuttonsHash.elements (); e.hasMoreElements ();) {
          names.addAttributeNameValue(USERNAME,(String)e.nextElement());
        }
        v.addElement(names.toDataObject());
        err.setError(Error.NO_ERROR);
      }
      v.addElement(err.toDataObject());
      return result;
    }
    else {
      if (err.getError() == null) {
        err.setError(Error.UNKNOWN_METHOD_ERROR);
      }
      Vector v = new Vector();
      v.addElement(err.toDataObject());
      return new DataObject(data.getName());
    }
  }

  /**
   * Sets the incoming attributes for the interpreter.  It has only
   * one: IBUTTONID.
   *
   * @return the incoming attributes for this interpreter
   */
  protected Attributes setInAttributes() {
    Attributes atts = new Attributes();
    atts.addAttribute(IBUTTONID);
    return atts;
  }

  /**
   * Sets the outgoing attributes for the interpreter.  It has only
   * one: USERNAME.
   *
   * @return the outgoing attributes for this interpreter
   */
  protected Attributes setOutAttributes() {
    Attributes atts = new Attributes();
    atts.addAttribute(USERNAME);
    return atts;
  }

  /**
   * Main method to create this interpreter with port specified by 
   * command line arguments
   */
  public static void main(String argv[]) {
    if (argv.length == 0) {
      if (DEBUG) {
        System.out.println("Attempting to create a IIButton2Name on "+DEFAULT_PORT);
      }
      IIButton2NameExt iib2n = new IIButton2NameExt();
    }
    else if (argv.length == 1) {
    	// if there is only one argument, figure out if it's a port # or a url
      try {
        int port = Integer.parseInt(argv[0]);
        // it's a port #
        if (DEBUG) {
          System.out.println("Attempting to create a IIButton2Name on "+argv[0]+" with the default users list");
        }
        IIButton2NameExt iib2n = new IIButton2NameExt(port);
      } catch (NumberFormatException nfe) {
          if (DEBUG) {
            System.out.println("Attempting to create a IIButton2Name on the default port with the users list at "+argv[0]);
          }
          IIButton2NameExt iib2n = new IIButton2NameExt(argv[0]);
	}
    }
    else if (argv.length == 2) {
      if (DEBUG) {
        System.out.println("Attempting to create a IIButton2Name on "+argv[0]+" grabbing users from "+argv[1]);
      }
      IIButton2NameExt iib2n = new IIButton2NameExt(Integer.parseInt(argv[0]), argv[1]);
    }
    else {
      System.out.println("USAGE: java context.arch.interpreter.IIButton2NameExt [port] [name url]");
    }
  }

}
