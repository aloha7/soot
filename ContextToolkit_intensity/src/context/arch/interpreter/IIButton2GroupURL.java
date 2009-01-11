package context.arch.interpreter;

import context.arch.comm.DataObject;
import context.arch.interpreter.IIButton2Group;	
import context.arch.interpreter.IGroup2URL;	
import context.arch.util.Error;
import context.arch.storage.Attributes;
import context.arch.storage.AttributeNameValues;

import java.util.Hashtable;
import java.util.Vector;

/**
 * This class is an interpreter that converts iButton ids to groups.
 * Another component (app/widget/interpreter) sends an interpret command
 * to it with an ibutton id, and this class returns the corresponding group.
 *
 * @see context.arch.interpreter.Interpreter
 */
public class IIButton2GroupURL extends Interpreter {
  
  /**
   * Debug flag. Set to true to see debug messages.
   */
  private static final boolean DEBUG = false;

  /**
   * Name of interpreter
   */
  public static final String CLASSNAME = "IButton2GroupURL";

  /**
   * Tag for iButton id
   */
  public static final String IBUTTONID = IIButton2Group.IBUTTONID;

  /**
   * Tag for group
   */
  public static final String GROUP = IIButton2Group.GROUP;

  /**
   * Tag for group url
   */
  public static final String URL = IGroup2URL.URL;

  private String rgurlHost;
  private String ibrgHost;
  private int rgurlPort;
  private int ibrgPort;

  /**
   * Constructor that creates the interpreter at the default port.  It sets
   * the id of the this interpreter to CLASSNAME.
   *
   * @param ibrghost Hostname of the machine IIButton2Group is running on
   * @param ibrgport Port of the machine IIButton2Group is running on
   * @param rgurlhost Hostname of the machine IGroup2URL is running on
   * @param rgurlport Port of the machine IGroup2URL is running on
   */
  public IIButton2GroupURL(String ibrghost, int ibrgport, String rgurlhost, int rgurlport) {
    this(DEFAULT_PORT, ibrghost, ibrgport, rgurlhost, rgurlport);
  }

  /**
   * Constructor that creates the interpreter at the given port.  It sets
   * the id of the this interpreter to CLASSNAME.
   *
   * @param port Port this interpreter is running on
   * @param ibrghost Hostname of the machine IIButton2Group is running on
   * @param ibrgport Port of the machine IIButton2Group is running on
   * @param rgurlhost Hostname of the machine IGroup2URL is running on
   * @param rgurlport Port of the machine IGroup2URL is running on
   */
  public IIButton2GroupURL(int port, String ibrghost, int ibrgport, String rgurlhost, int rgurlport) {
    super(port);
    setId(CLASSNAME);
    ibrgHost = ibrghost;
    ibrgPort = ibrgport;
    rgurlHost = rgurlhost;
    rgurlPort = rgurlport;
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
    DataObject tmp = askIButton2Group(buttonid);
    if (tmp != null) {
      String error = new Error(tmp).getError();
      if (error.equals(Error.NO_ERROR)) {
        AttributeNameValues tmpAtts = new AttributeNameValues(tmp);
        if (tmpAtts != null) {
          String group = (String)tmpAtts.getAttributeNameValue(GROUP).getValue();
          tmp = askGroup2URL(group);
          if (tmp != null) {
            error = new Error(tmp).getError();
            if (error.equals(Error.NO_ERROR)) {
              tmpAtts = new AttributeNameValues(tmp);
              String url = (String)tmpAtts.getAttributeNameValue(URL).getValue();
              AttributeNameValues result = new AttributeNameValues();
              result.addAttributeNameValue(URL,url);
              return result;
            }
          }
        }
      }
    }
    return null;
  }

  /**
   * This private method asks the IIButton2Group for a 
   * group for the given iButton id.
   *
   * @param buttonid IButton ID to get the group for
   * @return DataObject containing the results of the interpretation
   */
  private DataObject askIButton2Group(String buttonid) {
    AttributeNameValues data = new AttributeNameValues();
    data.addAttributeNameValue(IIButton2Group.IBUTTONID, buttonid);
    return askInterpreter(ibrgHost,ibrgPort,IIButton2Group.CLASSNAME,data);
  }

  /**
   * This private method asks the IGroup2URL for a url for the
   * for the given group.
   *
   * @param group Group to get the url for
   * @return DataObject containing the results of the interpretation
   */
  private DataObject askGroup2URL(String group) {
    AttributeNameValues data = new AttributeNameValues();
    data.addAttributeNameValue(GROUP,group);
    return askInterpreter(rgurlHost,rgurlPort,IGroup2URL.CLASSNAME,data);
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
   * one: GROUP
   *
   * @return the outgoing attributes for this interpreter.  
   */
  protected Attributes setOutAttributes() {
    Attributes atts = new Attributes();
    atts.addAttribute(URL);
    return atts;
  }


  /**
   * Main method to create this interpreter with port specified by 
   * command line arguments
   */
  public static void main(String argv[]) {
    if (argv.length == 4) {
      if (DEBUG) {
        System.out.println("Attempting to create a IIButton2GroupURL on "+DEFAULT_PORT);
      }
      IIButton2GroupURL iib2gu = new IIButton2GroupURL(argv[0], Integer.parseInt(argv[1]), argv[2], Integer.parseInt(argv[3]));
    }
    else if (argv.length == 5) {
      if (DEBUG) {
        System.out.println("Attempting to create a IIButton2GroupURL on "+argv[0]);
      }
      IIButton2GroupURL iib2gu = new IIButton2GroupURL(Integer.parseInt(argv[0]), argv[1], Integer.parseInt(argv[2]), argv[3], Integer.parseInt(argv[4]));
    }
    else {
      System.out.println("USAGE: java context.arch.interpreter.IIButton2GroupURL [port] <IIButton2GroupHost/ip> <IIButton2GroupPort> <IGroup2URLHost> <IGroup2URLPort>");
    }
  }

}
