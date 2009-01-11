package context.arch.widget;

import context.arch.comm.DataObject;
import context.arch.subscriber.Callbacks;
import context.arch.service.Services;
import context.arch.storage.Attributes;
import context.arch.storage.Attribute;
import context.arch.storage.AttributeNameValues;
import context.arch.util.ContextTypes;
import context.arch.generator.TemperatureSensor;
import context.arch.generator.TemperatureData;

/**
 * This class is a context widget that provides information on 
 * the temperature in a particular location.  The
 * information is in the form of a location, temperature, and units
 * (Celsius or Fahrenheit).  It has the following callbacks: UPDATE. 
 * It supports polling and subscriptions.  
 *
 * @see context.arch.widget.Widget
 * @see context.arch.generator.TemperatureSensor
 */
public class WTemperature extends Widget{

  /**
   * Debug flag. Set to true to see debug messages.
   */
  private static final boolean DEBUG = true;

  /**
   * Widget version number
   */
  public String VERSION_NUMBER = "1.0.0";

  /**
   * Tag for temperature
   */
  public static final String TEMPERATURE = ContextTypes.TEMPERATURE;

  /**
   * Tag for temperature units
   */
  public static final String UNITS = ContextTypes.UNITS;

  /**
   * Tag for user location 
   */
  public static final String LOCATION = ContextTypes.LOCATION;

  /**
   * Name of widget
   */
  public static final String CLASSNAME = "Temperature";

  /**
   * The default port this widget runs on is 5100
   */
  public static final int DEFAULT_PORT = 5135;

  private String location;
  protected TemperatureSensor temp;

  /**
   * Constructor that creates the widget at the given location and
   * monitors communications on the DEFAULT_PORT and
   * creates an instance of the TemperatureSensor generator.  It also
   * sets the id of this widget to CLASSNAME_<location value> with storage enabled.
   *  
   * @param location Location the widget is "monitoring"
   *
   * @see context.arch.generator.TemperatureSensor
   */
  public WTemperature (String location){
    this(location, DEFAULT_PORT, true);
  }

  /**
   * Constructor that creates the widget at the given location and
   * monitors communications on the DEFAULT_PORT and
   * creates an instance of the TemperatureSensor generator.  It also
   * sets the id of this widget to CLASSNAME_<location value>, and sets
   * storage functionality to storageFlag.
   *  
   * @param location Location the widget is "monitoring"
   * @param storageFlag Flag to indicate whether or not to enable storage functionality
   *
   * @see context.arch.generator.TemperatureSensor
   */
  public WTemperature(String location, boolean storageFlag) {
    this(location, DEFAULT_PORT, storageFlag);
  }

  /**
   * Constructor that creates the widget at the given location and
   * monitors communications on the given port and
   * creates an instance of the TemperatureSensor generator.  It also
   * sets the id of this widget to CLASSNAME_<location value> with storage enabled.
   *  
   * @param location Location the widget is "monitoring"
   * @param port Port to run the widget on
   *
   * @see context.arch.generator.TemperatureSensor
   */
  public WTemperature (String location, int port){
    this(location, port, CLASSNAME+SPACER+location, true);
  }

  /**
   * Constructor that creates the widget at the given location and
   * monitors communications on the given port and
   * creates an instance of the TemperatureSensor generator.  It also
   * sets the id of this widget to CLASSNAME_<location value> with storage functionality
   * set to storageFlag.
   *  
   * @param location Location the widget is "monitoring"
   * @param port Port to run the widget on
   * @param storageFlag Flag to indicate whether or not to enable storage functionality
   *
   * @see context.arch.generator.TemperatureSensor
   */
  public WTemperature(String location, int port, boolean storageFlag) {
    this(location,port,CLASSNAME+SPACER+location,storageFlag);
  }

  /**
   * Constructor that creates the widget at the given location and
   * monitors communications on the given port and
   * creates an instance of the TemperatureSensor generator.  It also
   * sets the id of this widget to CLASSNAME_<location value> with storage functionality
   * set to storageFlag.
   *  
   * @param location Location the widget is "monitoring"
   * @param port Port to run the widget on
   * @param storageFlag Flag to indicate whether or not to enable storage functionality
   *
   * @see context.arch.generator.TemperatureSensor
   */
  public WTemperature (String location, int port, String id, boolean storageFlag){
    super(port, id, storageFlag);
    setVersion(VERSION_NUMBER);
    this.location = location;
    temp = new TemperatureSensor(this);
  }

  /**
   * This method implements the abstract method Widget.setAttributes().
   * It defines the attributes for the widget as:
   *    TIMESTAMP, TEMPERATURE, UNITS, and LOCATION 
   *
   * @return the Attributes used by this widget
   */
  protected Attributes setAttributes(){
    Attributes atts = new Attributes();
    atts.addAttribute(TIMESTAMP,Attribute.LONG);
    atts.addAttribute(TEMPERATURE,Attribute.FLOAT);
    atts.addAttribute(UNITS);
    atts.addAttribute(LOCATION);
    return atts;
  }

  /**
   * This method implements the abstract method Widget.setCallbacks().
   * It defines the callbacks for the widget as:
   *    UPDATE with the attributes TIMESTAMP, TEMPERATURE, UNITS, LOCATION
   *
   * @return the Callbacks used by this widget
   */
  protected Callbacks setCallbacks(){
    Callbacks calls = new Callbacks();
    calls.addCallback(UPDATE,setAttributes());
    return calls;
  }

  /**
   * This method implements the abstract method Widget.setServices().
   * It currently has no services and returns an empty Services object.
   *
   * @return the Services provided by this widget
   */
  protected Services setServices(){
    return new Services();
  }

  /**
   * This method returns an empty AttributeNameValues object. The temperature
   * sensor can not be polled.
   *
   * @return empty AttributeNameValues
   */
  protected AttributeNameValues queryGenerator() {
    return new AttributeNameValues();
  }

  /**
   * Called by the generator class when a significant event has
   * occurred.  It creates a DataObject, sends it to its subscribers and
   * stores the data.
   *
   * @param event Name of the event that has occurred
   * @param data Object containing relevant event data
   * @see context.arch.widget.Widget#sendToSubscribers(String, AttributeNameValues)
   * @see context.arch.widget.Widget#store(AttributeNameValues)
   */
  public void notify(String event, Object data){
    AttributeNameValues atts = TemperatureData2Attributes((TemperatureData)data);
    if (atts!=null){
      if (subscribers.numSubscribers()>0){
        sendToSubscribers(event, atts);
      }
      store (atts);
    }
  }

  /**
   * This method converts the TemperatureData object to an AttributeNameValues
   * object.
   *
   * @param data TemperatureData object to be converted
   * @return AttributeNameValues object containing the data in the TemperatureData object
   */
  protected AttributeNameValues TemperatureData2Attributes(TemperatureData data){
    AttributeNameValues atts = new AttributeNameValues();
    atts.addAttributeNameValue(TEMPERATURE, data.getDegrees(),Attribute.FLOAT);
    atts.addAttributeNameValue(UNITS, data.getUnits());
    atts.addAttributeNameValue(LOCATION, location);
    atts.addAttributeNameValue(TIMESTAMP, data.getTime(),Attribute.LONG);
    return atts;
  }

  /**
   * Main method to create a widget with location and port specified by 
   * command line arguments
   */
  public static void main(String argv[]){
    if (argv.length == 1) {
      if (DEBUG) {
        System.out.println("Attempting to create a WTemperature on "+DEFAULT_PORT+" at " +argv[0]+" with storage enabled");
      }
      WTemperature wt = new WTemperature(argv[0]);
    }
    else if (argv.length == 2) {
      if ((argv[1].equals("false")) || (argv[1].equals("true"))) {
        if (DEBUG) {
          System.out.println("Attempting to create a WTemperature on "+DEFAULT_PORT+" at " +argv[0]+" with storage set to "+argv[1]);
        }
        WTemperature wt = new WTemperature(argv[0], Boolean.valueOf(argv[1]).booleanValue()); 
      }
      else {
        if (DEBUG) {
          System.out.println("Attempting to create a WTemperature on "+argv[1]+" at " +argv[0]+" with storage enabled");
        }
        WTemperature wt = new WTemperature(argv[0], Integer.parseInt(argv[1]));
      }
    }
    else if (argv.length == 3) {
      if (DEBUG) {
        System.out.println("Attempting to create a WTemperature on "+argv[1]+" at " +argv[0]+" with storage set to "+argv[2]);
      }
      WTemperature wt = new WTemperature(argv[0], Integer.parseInt(argv[1]), Boolean.valueOf(argv[2]).booleanValue());
    }
    else {
      System.out.println("USAGE: java context.arch.widget.WTemperature <location> [port] [storageFlag]");
    }
  }
}
