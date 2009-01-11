package context.arch.interpreter;

import context.arch.subscriber.Subscriber;
import context.arch.comm.DataObject;
import context.arch.storage.Attributes;
import context.arch.storage.AttributeNameValues;
import context.arch.util.ContextTypes;

import java.util.Hashtable;
import java.util.Vector;

/**
 * This class is an interpreter that converts iButton ids to names.
 * Another component (app/widget/interpreter) sends an interpret command
 * to it with an ibutton id, and this class returns the corresponding name.
 *
 * @see context.arch.interpreter.Interpreter
 */
public class IIButton2Name extends Interpreter {
  
  /**
   * Debug flag. Set to true to see debug messages.
   */
  private static final boolean DEBUG = true;

  /**
   * Name of interpreter
   */
  public static final String CLASSNAME = "IButton2Name";

  /**
   * Tag for iButton id
   */
  public static final String IBUTTONID = ContextTypes.IBUTTONID;

  /**
   * Tag for username
   */
  public static final String USERNAME = ContextTypes.USERNAME;

  /**
   * String for Anind Dey
   */
  public static final String ANIND = "Anind Dey";
  
  /**
   * String for Daniel Salber
   */
  public static final String SALBER = "Daniel Salber";
  
  /**
   * String for Gregory Abowd
   */
  public static final String ABOWD = "Gregory Abowd";
  
  /**
   * String for Jason Brotherton
   */
  public static final String BROTHERT = "Jason Brotherton";
  
  /**
   * String for Khai Truong
   */
  public static final String KHAI = "Khai Truong";
  
  /**
   * String for Jen Mankoff
   */
  public static final String JMANKOFF = "Jen Mankoff";

  /**
   * String for Robert Orr
   */
  public static final String RJO = "Rob Orr";
  
  /**
   * String for David Nguyen
   */
  public static final String DNGUYEN = "David Nguyen";
  
  /**
   * String for Rob Kooper
   */
  public static final String KOOPER = "Rob Kooper";

  /**
   * String for Maria Pimentel
   */
  public static final String MGP = "Maria Pimentel";

  /**
   * String for Futakawa
   */
  public static final String FUTAKAWA = "Futakawa";

  /**
   * String for Ishiguro
   */
  public static final String ISHIGURO = "Ishiguro";

  /**
   * String for Brad Singletary
   */
  public static final String BAS = "Brad Singletary";

  /**
   * String for Kent Lyons
   */
  public static final String KENT = "Kent Lyons";

  /**
   * String for Vishal Dalal
   */
  public static final String VISHAL = "Vishal Dalal";

  /**
   * String for Dummbo
   */
  public static final String DUMMBO = "Dummbo";

  private Hashtable hash = new Hashtable();

  /**
   * Constructor that creates the interpreter at the default port.  It sets
   * the id of the this interpreter to CLASSNAME.
   */
  public IIButton2Name() {
    this(DEFAULT_PORT);
  }

  /**
   * Constructor that creates the interpreter at the given port.  It sets
   * the id of the this interpreter to CLASSNAME.
   */
  public IIButton2Name(int port) {
    super(port);
    setId(CLASSNAME);
    hash.put("16AC850600000044", ANIND);
    hash.put("166F3C060000003E", SALBER);
    hash.put("16F78206000000C3", ABOWD);
    hash.put("161B4206000000B5", KHAI);
    hash.put("1681400600000048", BROTHERT);
    hash.put("16A640060000007B", JMANKOFF);
    hash.put("16C78708000000E9", RJO);
    hash.put("16D78C08000000C2", DNGUYEN);
    hash.put("16748D080000000A", MGP);
    hash.put("165D8E0800000064", FUTAKAWA);
    hash.put("16148B0800000055", ISHIGURO);
    hash.put("16A58E08000000B7", BAS);
    hash.put("16CF8F0800000076",KENT);
    hash.put("16ED870800000090",VISHAL);
    //2008/7/7: add a new id-name pair
    hash.put("01020304", "hwang");
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
    String name = (String)hash.get(buttonid);
    if (name == null) {
      return null;
    }
    AttributeNameValues atts = new AttributeNameValues();
    atts.addAttributeNameValue(USERNAME,name);
    return atts;
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
      IIButton2Name iib2n = new IIButton2Name();
    }
    else if (argv.length == 1) {
      if (DEBUG) {
        System.out.println("Attempting to create a IIButton2Name on "+argv[0]);
      }
      IIButton2Name iib2n = new IIButton2Name(Integer.parseInt(argv[0]));
    }
    else {
      System.out.println("USAGE: java context.arch.interpreter.IIButton2Name [port]");
    }
  }

}
