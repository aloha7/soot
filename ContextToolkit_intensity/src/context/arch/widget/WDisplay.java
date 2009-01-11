package context.arch.widget;

import context.arch.comm.DataObject;
import context.arch.storage.AttributeNameValues;
import context.arch.storage.Attributes;
import context.arch.storage.Attribute;
import context.arch.subscriber.Callbacks;
import context.arch.service.DisplayChoiceService;
import context.arch.service.Services;
import context.arch.util.ContextTypes;

import java.util.Vector;

/**
 * This class is a context widget that represents a display device.
 * It currently doesn't provide any context but offers a single 
 * service DISPLAY_CHOICES, with a single function DISPLAY.
 *
 * @see Widget
 */
public class WDisplay extends Widget {

  /**
   * Debug flag. Set to true to see debug messages.
   */
  private static final boolean DEBUG = false;

  /**
   * Widget version number
   */
  public String VERSION_NUMBER = "1.0.0";

  /**
   * Name of widget
   */
  public static final String CLASSNAME = "Display";

  /**
   * Default port this widget runs on
   */
  public static final int DEFAULT_PORT = 5800;

  private String device;

  /**
   * Constructor that creates the widget for the give device and
   * monitors communications on the DEFAULT_PORT.  It also
   * sets the id of this widget to CLASSNAME_<device> and uses storage
   * functionality.
   *
   * @param device Tag for the display device
   * @param width Width of the display in pixels
   * @param height Height of the display in pixels
   * @param graphics Type of display: text or graphics
   */
  public WDisplay(String device, String width, String height, String graphics) {
    this(device, DEFAULT_PORT, width, height, graphics);
  }

  /**
   * Constructor that creates the widget for the give device and
   * monitors communications on the DEFAULT_PORT.  It also
   * sets the id of this widget to CLASSNAME_<device> and sets storage
   * functionality to storageFlag.
   *
   * @param device Tag for the display device
   * @param width Width of the display in pixels
   * @param height Height of the display in pixels
   * @param graphics Type of display: text or graphics
   * @param storageFlag Flag to indicate whether or not to use storage
   */
  public WDisplay(String device, String width, String height, String graphics, boolean storageFlag) {
    this(device, DEFAULT_PORT, width, height, graphics, storageFlag);
  }

  /**
   * Constructor that creates the widget for the given device and 
   * monitors communications on the given port .  It also
   * sets the id of this widget to CLASSNAME_<device> and uses storage
   * functionality.
   *  
   * @param device Tag for the display device
   * @param port Port to run the widget on
   * @param width Width of the display in pixels
   * @param height Height of the display in pixels
   * @param graphics Type of display: text or graphics
   */
  public WDisplay(String device, int port, String width, String height, String graphics) {
    this(device,port,CLASSNAME+SPACER+device, width, height, graphics);
  }

  /**
   * Constructor that creates the widget for the given device and 
   * monitors communications on the given port .  It also
   * sets the id of this widget to CLASSNAME_<device> and sets storage
   * functionality to storageFlag.
   *  
   * @param device Tag for the display device
   * @param port Port to run the widget on
   * @param width Width of the display in pixels
   * @param height Height of the display in pixels
   * @param graphics Type of display: text or graphics
   * @param storageFlag Flag to indicate whether or not to use storage.
   */
  public WDisplay(String device, int port, String width, String height, String graphics, boolean storageFlag) {
    this(device,port,CLASSNAME+SPACER+device, width, height, graphics, storageFlag);
  }
  
  /**
   * Constructor that creates the widget for the given device and 
   * monitors communications on the given port It also
   * sets the id of this widget to the given id.
   *  
   * @param device Tag for the display device
   * @param port Port to run the widget on
   * @param id Widget id
   * @param width Width of the display in pixels
   * @param height Height of the display in pixels
   * @param graphics Type of display: text or graphics
   */
  public WDisplay(String device, int port, String id, String width, String height, String graphics) {
    super(port,id);
    this.device = device;
    AttributeNameValues atts = new AttributeNameValues();
    atts.addAttributeNameValue(ContextTypes.WIDTH,width);
    atts.addAttributeNameValue(ContextTypes.HEIGHT,height);
    atts.addAttributeNameValue(ContextTypes.GRAPHICS,graphics);
    store(atts);
  }

  /**
   * Constructor that creates the widget for the given device and 
   * monitors communications on the given port It also
   * sets the id of this widget to the given id and sets storage functionality
   * to storageFlag
   *  
   * @param device Tag for the display device
   * @param port Port to run the widget on
   * @param id Widget id
   * @param width Width of the display in pixels
   * @param height Height of the display in pixels
   * @param graphics Type of display: text or graphics
   * @param storageFlag Flag to indicate whether or not to use storage
   */
  public WDisplay(String device, int port, String id, String width, String height, String graphics, boolean storageFlag) {
    super(port,id,storageFlag);
    this.device = device;
    AttributeNameValues atts = new AttributeNameValues();
    atts.addAttributeNameValue(ContextTypes.WIDTH,width);
    atts.addAttributeNameValue(ContextTypes.HEIGHT,height);
    atts.addAttributeNameValue(ContextTypes.GRAPHICS,graphics);
    store(atts);
  }

  /**
   * This method implements the abstract method Widget.setAttributes().
   * It defines the attributes as HEIGHT, WIDTH, and GRAPHICS.
   *
   * @return the Attributes used by this widget
   */
  protected Attributes setAttributes() {
    Attributes atts = new Attributes();
    atts.addAttribute(ContextTypes.HEIGHT);
    atts.addAttribute(ContextTypes.WIDTH);
    atts.addAttribute(ContextTypes.GRAPHICS);
    return atts;
  }

  /**
   * This method implements the abstract method Widget.setCallbacks().
   * This has no callbacks.
   *
   * @return the Callbacks used by this widget
   */
  protected Callbacks setCallbacks() {
    return new Callbacks();
  }

  /**
   * This method implements the abstract method Widget.setServices().
   * It has a single service: DISPLAY_CHOICES
   *
   * @return the Services provided by this widget
   * @see DisplayChoiceService
   */
  protected Services setServices() {
    Services services = new Services();
    services.addService(new DisplayChoiceService(this));
    return services;
  }

  /**
   * This method returns an empty AttributeNameValues object.
   *
   * @return empty AttributeNameValues
   */
  protected AttributeNameValues queryGenerator() {
    return new AttributeNameValues();
  }

  
  /**
   * Called by the generator class when a significant event has
   * occurred.  It creates a frame to collect data from the registrant.
   *
   * @param event Name of the event that has occurred
   * @param data Object containing relevant event data
   * @see context.arch.widget.Widget#sendToSubscribers(String, AttributeNameValues)
   * @see context.arch.widget.Widget#store(AttributeNameValues)
   */
  public void notify(String event, Object data) {
  }

  /**
   * Temporary main method to create a widget with location and port specified by 
   * command line arguments
   */
  public static void main(String argv[]) {
    if (argv.length == 4) {
      if (DEBUG) {
        System.out.println("Attempting to create a WDisplay on "+DEFAULT_PORT+" at "+argv[0]+" with width="+argv[1]+" and height="+argv[2]+" and display capability is "+argv[3]+" with storage enabled");
      }
      if ((argv[3].equals("text")) || (argv[3].equals("graphics"))) {
        WDisplay wd = new WDisplay(argv[0], argv[1], argv[2], argv[3], true);
        //wd.setServices();
      }
      else {
        System.out.println("USAGE: java context.arch.widget.WDisplay <location> [port] <width> <height> <text | graphics> [storageFlag]");
      }
    }
    else if (argv.length == 5) {
      if ((argv[4].equals("text")) || (argv[4].equals("graphics"))) {
        WDisplay wd = new WDisplay(argv[0], Integer.parseInt(argv[1]), argv[2], argv[3], argv[4]);
        if (DEBUG) {
          System.out.println("Attempting to create a WDisplay on "+argv[1]+" at " +argv[0]+" with width="+argv[2]+" and height="+argv[3]+" and display capability is "+argv[4]+ " with storage enabled");
        }
      }
      else if ((argv[4].equals("true")) || (argv[4].equals("false"))) {
        WDisplay wd = new WDisplay(argv[0], argv[1], argv[2], argv[3], Boolean.valueOf(argv[4]).booleanValue());
        if (DEBUG) {
          System.out.println("Attempting to create a WDisplay on "+DEFAULT_PORT+" at " +argv[0]+" with width="+argv[1]+" and height="+argv[2]+" and display capability is "+argv[3]+" with storage set to "+argv[4]);
        }
      }
      else {
        System.out.println("USAGE: java context.arch.widget.WDisplay <location> [port] <width> <height> <text | graphics> [storageFlag]");
      }
    }
    else if (argv.length == 6) {
      if (DEBUG) {
        System.out.println("Attempting to create a WDisplay on "+argv[1]+" at " +argv[0]+" with width="+argv[2]+" and height="+argv[3]+" and display capability is "+argv[4]+ " with storage set to "+argv[5]);
      }
      WDisplay wd = new WDisplay(argv[0], Integer.parseInt(argv[1]), argv[2], argv[3], argv[4], Boolean.valueOf(argv[5]).booleanValue());
    }
    else {
      System.out.println("USAGE: java context.arch.widget.WDisplay <location> [port] <width> <height> <text | graphics>");
    }
  }

}
