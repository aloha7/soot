package context.apps.Tour;

import context.arch.storage.AttributeNameValues;
import context.arch.storage.Attribute;
import context.arch.util.ContextTypes;

import java.util.Hashtable;
import java.util.Enumeration;

/**
 * This class maintains a list of demos.
 *
 * @see context.apps.Tour.Demo
 */
public class Demos extends Hashtable {

  public static final String DEMOS = ContextTypes.DEMOS;

  /**
   * Basic empty constructor
   */
  public Demos() {
    super();
  }

  public Demos(AttributeNameValues atts) {
    for (int i=0; i<atts.numAttributeNameValues(); i++) {
      addDemo(new Demo((AttributeNameValues)atts.getAttributeNameValueAt(i).getValue()));
    }
  }

  /**
   * Adds a demo to the demo list
   */
  public synchronized void addDemo(Demo demo) {
    put(demo.getDemoName(),demo);
  }

  /**
   * Adds a demo to the demo list
   */
  public synchronized void addDemo(String name,String url,String demoerUrl,String keywords,String description) {
    put(name,new Demo(name,url,demoerUrl,keywords,description));
  }

  /**
   * Retrieves a Demo from the Demo list
   */
  public synchronized Demo getDemo(String key) {
    return (Demo)get(key);
  }

  /**
   * Returns an enumeration of the Demo objects
   */
  public synchronized Enumeration getEnumeration() {
    return elements();
  }

  public AttributeNameValues toAttributeNameValues() {
    AttributeNameValues atts = new AttributeNameValues();
    for (Enumeration e = elements(); e.hasMoreElements(); ) {
      atts.addAttributeNameValue(Demo.DEMO,((Demo)e.nextElement()).toAttributeNameValues(),Attribute.STRUCT);
    }
    return atts;
  }

      
}
