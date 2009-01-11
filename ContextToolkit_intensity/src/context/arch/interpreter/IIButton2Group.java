package context.arch.interpreter;

import context.arch.comm.DataObject;
import context.arch.storage.Attributes;
import context.arch.storage.AttributeNameValues;
import context.arch.util.ContextTypes;
	
import java.util.Hashtable;
import java.util.Vector;

/**
 * This class is an interpreter that converts iButton ids to groups.
 * Another component (app/widget/interpreter) sends an interpret command
 * to it with an ibutton id, and this class returns the corresponding group.
 *
 * @see context.arch.interpreter.Interpreter
 */
public class IIButton2Group extends Interpreter {
  
  /**
   * Debug flag. Set to true to see debug messages.
   */
  private static final boolean DEBUG = false;

  /**
   * Name of interpreter
   */
  public static final String CLASSNAME = "IButton2Group";

  /**
   * Tag for iButton id
   */
  public static final String IBUTTONID = ContextTypes.IBUTTONID;

  /**
   * Tag for group
   */
  public static final String GROUP = ContextTypes.GROUP;

  /**
   * String for FCE
   */
  public static final String FCE = "FCE";
  
  /**
   * String for VE
   */
  public static final String VE = "VE";
  
  /**
   * String for IS
   */
  public static final String IS = "IS";
  
  /**
   * String for CRB
   */
  public static final String CRB = "CRB";
  
  private Hashtable hash = new Hashtable();

  /**
   * Constructor that creates the interpreter at the default port.  It sets
   * the id of the this interpreter to CLASSNAME.
   */
  public IIButton2Group() {
    this(DEFAULT_PORT);
  }

  /**
   * Constructor that creates the interpreter at the given port.  It sets
   * the id of the this interpreter to CLASSNAME.
   */
  public IIButton2Group(int port) {
    super(port);
    setId(CLASSNAME);
    hash.put("16AC850600000044", FCE);
    hash.put("166F3C060000003E", FCE);
    hash.put("16F78206000000C3", VE);
    hash.put("161B4206000000B5", VE);
    hash.put("1681400600000048", IS);
    hash.put("16A640060000007B", CRB);
  }

  /**
   * This method performs the actual interpretation of this component.
   * It takes an iButton id and returns a group name.
   *
   * @param data AttributeNameValues containing data to be interpreted
   * @return AttributeNameValues object containing the interpreted data
   */
  protected AttributeNameValues interpretData(AttributeNameValues data) {
    String buttonid = (String)data.getAttributeNameValue(IBUTTONID).getValue();
    String group = (String)hash.get(buttonid);
    if (group == null) {
      return null;
    }
    AttributeNameValues atts = new AttributeNameValues();
    atts.addAttributeNameValue(GROUP,group);
    return atts;
  }

  /**
   * Sets the incoming attributes for the interpreter.  It has only
   * one: IBUTTONID
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
   * @return the outgoing attributes for this interpreter
   */
  protected Attributes setOutAttributes() {
    Attributes atts = new Attributes();
    atts.addAttribute(GROUP);
    return atts;
  }


  /**
   * Main method to create this interpreter with port specified by 
   * command line arguments
   */
  public static void main(String argv[]) {
    if (argv.length == 0) {
      if (DEBUG) {
        System.out.println("Attempting to create a IIButton2Group on "+DEFAULT_PORT);
      }
      IIButton2Group iib2g = new IIButton2Group();
    }
    else if (argv.length == 1) {
      if (DEBUG) {
        System.out.println("Attempting to create a IIButton2Group on "+argv[0]);
      }
      IIButton2Group iib2g = new IIButton2Group(Integer.parseInt(argv[0]));
    }
    else {
      System.out.println("USAGE: java context.arch.interpreter.IIButton2Group [port]");
    }
  }

}
