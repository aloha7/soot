package context.apps.Tour;

import context.arch.storage.AttributeNameValues;
import context.arch.util.ContextTypes;

/**
 *
 */
public class DemoInterest {

  public static final String DEMO_NAME = ContextTypes.DEMO_NAME;
  public static final String INTEREST_LEVEL = ContextTypes.INTEREST_LEVEL;
  public static final String DEMO_INTEREST = ContextTypes.DEMO_INTEREST;

  private String demo;
  private String interest;

  /**
   * Basic constructor that creates a DemoInterest object.
   */
  public DemoInterest(String demo, String interest) {
    this.demo = demo;
    this.interest = interest;
  }

  public DemoInterest(AttributeNameValues atts) {
    demo = (String)atts.getAttributeNameValue(DEMO_NAME).getValue();
    interest = (String)atts.getAttributeNameValue(INTEREST_LEVEL).getValue();
  }

  public String getInterest() {
    return interest;
  }

  public String getDemo() {
    return demo;
  }

  public void setInterest(String interest) {
    this.interest = interest;
  }

  public void setDemo(String demo) {
    this.demo = demo;
  }

  public AttributeNameValues toAttributeNameValues() {
    AttributeNameValues atts = new AttributeNameValues();
    atts.addAttributeNameValue(DEMO_NAME,demo);
    atts.addAttributeNameValue(INTEREST_LEVEL,interest);
    return atts;
  }

}
