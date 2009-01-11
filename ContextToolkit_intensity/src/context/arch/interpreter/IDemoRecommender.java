package context.arch.interpreter;

import context.arch.subscriber.Subscriber;
import context.arch.comm.DataObject;
import context.arch.storage.Attributes;
import context.arch.storage.AttributeNameValues;
import context.arch.storage.AttributeNameValue;
import context.arch.util.ContextTypes;
import context.apps.Tour.DemoInterests;
import context.apps.Tour.Demos;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/**
 * This class is an interpreter that converts information about a tour into
 * possible interesting tour demos to visit. Another component (app/widget/interpreter) 
 * sends an interpret command to it with a list of visited demos and associated
 * user interests, a list of user interests, and a complete list of possible demos 
 * and associated information (demo name, demo text description), and this interpreter 
 * returns potentially interesting demos that the user has not visited.
 *
 * @see context.arch.interpreter.Interpreter
 */
public class IDemoRecommender extends Interpreter {
  
  /**
   * Debug flag. Set to true to see debug messages.
   */
  private static final boolean DEBUG = false;

  /**
   * Name of interpreter
   */
  public static final String CLASSNAME = "DemoRecommender";

  /**
   * Tag for demo
   */
  public static final String DEMO_NAME = ContextTypes.DEMO_NAME;

  /**
   * Tag for interests
   */
  public static final String INTERESTS = ContextTypes.INTERESTS;

  /**
   * Constructor that creates the interpreter at the default port.  It sets
   * the id of the this interpreter to CLASSNAME.
   */
  public IDemoRecommender() {
    this(DEFAULT_PORT);
  }

  /**
   * Constructor that creates the interpreter at the given port.  It sets
   * the id of the this interpreter to CLASSNAME.
   */
  public IDemoRecommender(int port) {
    super(port);
    setId(CLASSNAME);
  }

  /**
   * This method performs the actual interpretation of this component.
   * It takes user interest info, visited demos and associated interest level,
   * and available demo information, and returns the names of potentially
   * interesting demos not yet visited.
   *
   * @param data AttributeNameValues containing data to be interpreted
   * @return AttributeNameValues object containing the interpreted data
   */
  protected AttributeNameValues interpretData(AttributeNameValues data) {
    String interests = (String)data.getAttributeNameValue(INTERESTS).getValue();
    DemoInterests dis = new DemoInterests();
    Demos demos = new Demos();
    AttributeNameValue att = data.getAttributeNameValue(DemoInterests.DEMO_INTERESTS);
    if (att != null) {
      dis = new DemoInterests((AttributeNameValues)att.getValue());
    }
    att = data.getAttributeNameValue(Demos.DEMOS);
    if (att != null) {
      demos = new Demos((AttributeNameValues)att.getValue());
    }
    // do interpretation here with dis, demos and interests - AKD

    AttributeNameValues atts = new AttributeNameValues();
    // put result in atts - AKD  atts.addAttributeNameValue(DEMO_NAME,demoName);
    /*//2008/7/8:    
    for(Enumeration e = demos.keys(); e.hasMoreElements();){
    	atts.addAttributeNameValue(DEMO_NAME, (String)e.nextElement());
    }
    */
    atts = data;
    
    //atts.addAttributeNameValue(att);
    return atts;
  }

  /**
   * Sets the incoming attributes for the interpreter.  They are:
   * DEMO_INTERESTS, DEMOS, and INTERESTS
   *
   * @return the incoming attributes for this interpreter
   */
  protected Attributes setInAttributes() {
    Attributes atts = new Attributes();
    atts.addAttribute(DemoInterests.DEMO_INTERESTS);
    atts.addAttribute(Demos.DEMOS);
    atts.addAttribute(INTERESTS);
    return atts;
  }

  /**
   * Sets the outgoing attributes for the interpreter.  They are 
   * DEMO_NAME.
   *
   * @return the outgoing attributes for this interpreter.  
   */
  protected Attributes setOutAttributes() {
    Attributes atts = new Attributes();
    atts.addAttribute(DEMO_NAME);
    return atts;
  }


  /**
   * Main method to create this interpreter with port specified by 
   * command line arguments
   */
  public static void main(String argv[]) {
    if (argv.length == 0) {
      if (DEBUG) {
        System.out.println("Attempting to create a IDemoRecommender on "+DEFAULT_PORT);
      }
      IDemoRecommender idm = new IDemoRecommender();
    }
    else if (argv.length == 1) {
      if (DEBUG) {
        System.out.println("Attempting to create a IDemoRecommender on "+argv[0]);
      }
      IDemoRecommender idm = new IDemoRecommender(Integer.parseInt(argv[0]));
    }
    else {
      System.out.println("USAGE: java context.arch.interpreter.IDemoRecommender [port]");
    }
  }

}
