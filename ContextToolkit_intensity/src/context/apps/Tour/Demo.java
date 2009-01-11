package context.apps.Tour;

import context.arch.util.ContextTypes;
import context.arch.storage.AttributeNameValues;

/**
 * This class implements a demo object.
 *
 * @see context.apps.Tour.Demos
 */
public class Demo {

  public static final String DEMO = ContextTypes.DEMO;
  public static final String DEMO_NAME = ContextTypes.DEMO_NAME;
  public static final String DEMO_URL = ContextTypes.DEMO_URL;
  public static final String DEMOER_URL = ContextTypes.DEMOER_URL;
  public static final String KEYWORDS = ContextTypes.KEYWORDS;
  public static final String DESCRIPTION = ContextTypes.DESCRIPTION;

  private String name;
  private String url;
  private String demoerUrl;
  private String keywords;
  private String description;

  /**
   * Basic constructor that creates a demo object.
   */
  public Demo(String name,String url,String demoerUrl,String keywords,String description) {
    this.name = name;
    this.url = url;
    this.demoerUrl = demoerUrl;
    this.keywords = keywords;
    this.description = description;
  }

  public Demo(AttributeNameValues atts) {
    name = (String)atts.getAttributeNameValue(DEMO_NAME).getValue();
    url = (String)atts.getAttributeNameValue(DEMO_URL).getValue();
    demoerUrl = (String)atts.getAttributeNameValue(DEMOER_URL).getValue();
    keywords = (String)atts.getAttributeNameValue(KEYWORDS).getValue();
    description = (String)atts.getAttributeNameValue(DESCRIPTION).getValue();
  }
    
  public String getDemoName() {
    return name;
  }

  public String getDemoUrl() {
    return url;
  }

  public String getDemoerUrl() {
    return demoerUrl;
  }

  public String getKeywords() {
    return keywords;
  }

  public String getDescription() {
    return description;
  }

  public String toString() {
    return new String("name="+name+", url="+url+", demoerUrl="+demoerUrl+", keywords="+keywords+", description="+description);
  }

  public AttributeNameValues toAttributeNameValues() {
    AttributeNameValues atts = new AttributeNameValues();
    atts.addAttributeNameValue(DEMO_NAME,name);
    atts.addAttributeNameValue(DEMO_URL,url);
    atts.addAttributeNameValue(DEMOER_URL,demoerUrl);
    atts.addAttributeNameValue(KEYWORDS,keywords);
    atts.addAttributeNameValue(DESCRIPTION,description);
    return atts;
  }

}
