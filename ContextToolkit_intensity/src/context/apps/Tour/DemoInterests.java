package context.apps.Tour;

import context.arch.storage.AttributeNameValues;
import context.arch.storage.Attribute;
import context.arch.util.ContextTypes;

import java.util.Hashtable;
import java.util.Enumeration;

/**
 * This class maintains a list of DemoInterest objects.
 *
 * @see context.apps.Tour.DemoInterest
 */
public class DemoInterests extends Hashtable {

  public static final String DEMO_INTERESTS = ContextTypes.DEMO_INTERESTS;

  /**
   * Basic empty constructor
   */
  public DemoInterests() {
    super();
  }

  public DemoInterests(AttributeNameValues atts) {
    for (int i=0; i<atts.numAttributeNameValues(); i++) {
      addDemoInterest(new DemoInterest((AttributeNameValues)atts.getAttributeNameValueAt(i).getValue()));
    }
  }

  /**
   * Adds a DemoInterest object to the list
   */
  public synchronized void addDemoInterest(DemoInterest di) {
    put(di.getDemo(),di);
  }

  /**
   * Adds a DemoInterest object to the list
   */
  public synchronized void addDemoInterest(String demo) {
    DemoInterest di = new DemoInterest(demo,null);
    put(demo,di);
  }

  /**
   * Adds a DemoInterest object to the list
   */
  public synchronized void addDemoInterest(String demo, String interest) {
    DemoInterest di = new DemoInterest(demo,interest);
    put(demo,di);
  }

  /**
   * Returns an enumeration of the DemoInterest objects
   */
  public synchronized Enumeration getEnumeration() {
    return elements();
  }

  public AttributeNameValues toAttributeNameValues() {
    AttributeNameValues atts = new AttributeNameValues();
    for (Enumeration e=elements(); e.hasMoreElements(); ) {
      atts.addAttributeNameValue(DemoInterest.DEMO_INTEREST,((DemoInterest)e.nextElement()).toAttributeNameValues(),Attribute.STRUCT);
    }
    return atts;
  }

}
