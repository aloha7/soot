package context.arch.interpreter;

import context.arch.comm.DataObject;
import context.arch.interpreter.IIButton2Group;
import context.arch.storage.Attributes;
import context.arch.storage.AttributeNameValues;
import context.arch.util.ContextTypes;
	
import java.util.Hashtable;
import java.util.Vector;

/**
 * This class is an interpreter that converts groups to URLs.
 * Another component (app/widget/interpreter) sends an interpret command
 * to it with a group name, and this class returns the corresponding URL.
 *
 * @see context.arch.interpreter.Interpreter
 */
public class IGroup2URL extends Interpreter {
  
  /**
   * Debug flag. Set to true to see debug messages.
   */
  private static final boolean DEBUG = false;

  /**
   * Name of interpreter
   */
  public static final String CLASSNAME = "IGroup2URL";

  /**
   * Tag for URL
   */
  public static final String URL = ContextTypes.URL;

  /**
   * Tag for GROUP
   */
  public static final String GROUP = ContextTypes.GROUP;

  /**
   * String for FCE URL
   */
  public static final String FCEURL = "http://www.cc.gatech.edu/fce";
  
  /**
   * String for VE URL
   */
  public static final String VEURL= "http://www.cc.gatech.edu/ve";
  
  /**
   * String for IS
   */
  public static final String ISURL = "http://www.cc.gatech.edu/is";
  
  /**
   * String for CRB
   */
  public static final String CRBURL = "http://www.cc.gatech.edu/crb";
  
  private Hashtable hash = new Hashtable();

  /**
   * Constructor that creates the interpreter at the default port.  It sets
   * the id of the this interpreter to CLASSNAME.
   */
  public IGroup2URL() {
    this(DEFAULT_PORT);
  }

  /**
   * Constructor that creates the interpreter at the given port.  It sets
   * the id of the this interpreter to CLASSNAME.
   */
  public IGroup2URL(int port) {
    super(port);
    setId(CLASSNAME);
    hash.put(IIButton2Group.FCE, FCEURL);
    hash.put(IIButton2Group.VE, VEURL);
    hash.put(IIButton2Group.IS, ISURL);
    hash.put(IIButton2Group.CRB, CRBURL);
  }

  /**
   * This method performs the actual interpretation of this component.
   * It takes a group and returns a url.
   *
   * @param data AttributeNameValues containing data to be interpreted
   * @return AttributeNameValues object containing the interpreted data
   */
  protected AttributeNameValues interpretData(AttributeNameValues data) {
    String group = (String)data.getAttributeNameValue(GROUP).getValue();
    String url = (String)hash.get(group);
    if (url == null) {
      return null;
    }
    AttributeNameValues atts = new AttributeNameValues();
    atts.addAttributeNameValue(URL,url);
    return atts;
  }

  /**
   * Sets the incoming attributes for the interpreter.  It has only
   * one: GROUP
   *
   * @return the incoming attributes for this interpreter
   */
  protected Attributes setInAttributes() {
    Attributes atts = new Attributes();
    atts.addAttribute(GROUP);
    return atts;
  }

  /**
   * Sets the outgoing attributes for the interpreter.  It has only
   * one: URL
   *
   * @return the outgoing attributes for this interpreter
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
    if (argv.length == 0) {
      if (DEBUG) {
        System.out.println("Attempting to create a IGroup2URL on "+DEFAULT_PORT);
      }
      IGroup2URL ig2u = new IGroup2URL();
    }
    else if (argv.length == 1) {
      if (DEBUG) {
        System.out.println("Attempting to create a IGroup2URL on "+argv[0]); 
      }
      IGroup2URL ig2u = new IGroup2URL(Integer.parseInt(argv[0]));
    }
    else {
      System.out.println("USAGE: java context.arch.interpreter.IGroup2URL [port]");
    }
  }

}
