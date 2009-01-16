package context.arch.widget;

import java.util.Date;

import context.arch.comm.DataObject;
import context.arch.generator.IButtonData;
import context.arch.generator.PositionIButton;
import context.arch.subscriber.Subscriber;
import context.arch.subscriber.Callbacks;
import context.arch.storage.Attributes;
import context.arch.storage.Attribute;
import context.arch.storage.AttributeNameValues;
import context.arch.util.ContextTypes;

/**
 * This class is a context widget that provides information on 
 * the presence of a person in a particular location and that this
 * person has ended their tour.  The information is in the form of 
 * a location and a user id.  It has the following callbacks: UPDATE. 
 * It supports polling and subscriptions.  Currently it uses the
 * WPersonPresence as a means of providing presence.
 *
 * @see context.arch.widget.WPersonPresence
 */
public class WTourEnd extends WPersonPresence {

  /**
   * Debug flag. Set to true to see debug messages.
   */
  private static final boolean DEBUG = false;

  /**
   * Widget version number
   */
  public String VERSION_NUMBER = "1.0.0";

  /**
   * Tag for tour end 
   */
  public static final String TOUR_END = ContextTypes.TOUR_END;

  /**
   * Tag for tour end 
   */
  public static final String FALSE = ContextTypes.FALSE;

  /**
   * Tag for tour end 
   */
  public static final String TRUE = ContextTypes.TRUE;

  /**
   * Tag for callback END 
   */
  public static final String END = "end";

  /**
   * Name of widget
   */
  public static final String CLASSNAME = "TourEnd";

  /**
   * Default port this widget is running on
   */
  public static final int DEFAULT_PORT = 5900;

  private String location;
  private String userid;
	
  /**
   * Constructor that creates the widget at the given location and
   * monitors communications on the DEFAULT_PORT.  It also
   * sets the id of this widget to CLASSNAME_<location value>
   * with storage enabled.
   *  
   * @param location Location the widget is "monitoring"
   */
  public WTourEnd(String location) {
    this(location, DEFAULT_PORT, true);
  }

  /**
   * Constructor that creates the widget at the given location and
   * monitors communications on the DEFAULT_PORT.  It also
   * sets the id of this widget to CLASSNAME_<location value>
   * with storage functionality set to storageFlag
   *  
   * @param location Location the widget is "monitoring"
   * @param storageFlag Flag to indicate whether storage is enabled
   */
  public WTourEnd(String location, boolean storageFlag) {
    this(location, DEFAULT_PORT, storageFlag);
  }

  /**
   * Constructor that creates the widget at the given location and 
   * monitors communications on the given port.  It also
   * sets the id of this widget to CLASSNAME_<location value> with
   * storage enabled.
   *  
   * @param location Location the widget is "monitoring"
   * @param port Port to run the widget on
   */
  public WTourEnd(String location, int port) {
    this(location,port,CLASSNAME+SPACER+location, true);
  }

  /**
   * Constructor that creates the widget at the given location and 
   * monitors communications on the given port.  It also
   * sets the id of this widget to CLASSNAME_<location value> with
   * storage functionality set to storageFlag
   *  
   * @param location Location the widget is "monitoring"
   * @param port Port to run the widget on
   * @param storageFlag Flag to indicate whether storage is enabled
   */
  public WTourEnd(String location, int port, boolean storageFlag) {
    this(location,port,CLASSNAME+SPACER+location, storageFlag);
  }
  
  /**
   * Constructor that creates the widget at the given location and 
   * monitors communications on the given port.  It also
   * sets the id of this widget to the given id with storage functionality
   * set to storageFlag
   *  
   * @param location Location the widget is "monitoring"
   * @param port Port to run the widget on
   * @param id Widget id
   * @param storageFlag Flag to indicate whether storage is enabled
   */
  public WTourEnd(String location, int port, String id, boolean storageFlag) {
    super(location,port,id, storageFlag);
    this.location = location;
    
    /*
    //2008/7/8: add a script to test notify functions correctly.
    PositionIButton sensor = new PositionIButton();
    String locations = "test";
    int[] iButtonId = new int[]{1,2,3,4};
    String currentid = sensor.toHexString(iButtonId);
    long currentTime = new Date().getTime();   
    //notify(Widget.UPDATE, new IButtonData(locations, currentid, Long.toString(currentTime)));
     * 
     */
    
  }

  /**
   * This method implements the abstract method Widget.setAttributes().
   * It defines the attributes for the widget as:
   *    TIMESTAMP, USERID, LOCATION, and TOUR_END
   *
   * @return the Attributes used by this widget
   */
  protected Attributes setAttributes() {
    Attributes atts = super.setAttributes();
    atts.addAttribute(TOUR_END);
    return atts;
  }

  /**
   * This method implements the abstract method Widget.setCallbacks().
   * It defines the callbacks for the widget as:
   *    UPDATE with the attributes TIMESTAMP, USERID, LOCATION, TOUR_END
   *
   * @return the Callbacks used by this widget
   */
  protected Callbacks setCallbacks() {
    Callbacks calls = new Callbacks();
    calls.addCallback(END,setAttributes());
    return calls;
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
  public void notify(String event, Object data) {
	  //2008/7/9
	if(event.equals(WTourEnd.END)){
//		System.out.println(this.getClass().getName() + " gets a context:" + WTourEnd.END);
		
	    AttributeNameValues atts = IButtonData2Attributes((IButtonData)data);
	    if (atts != null) {      
	      if (subscribers.numSubscribers() > 0) {
	        sendToSubscribers(END, atts);
	      }
	      store(atts);
	    }	    
	}
	  
  }

  /**
   * This method converts the IButtonData object to an AttributeNameValues
   * object.  It overrides the method in WPersonPresence, basically
   * doing the same thing, except it also returns TOUR_END info
   *
   * @param data IButtonData object to be converted
   * @return AttributeNameValues object containing the data in the IButtonData object
   */
  protected AttributeNameValues IButtonData2Attributes(IButtonData data) {
    AttributeNameValues atts = new AttributeNameValues();
    atts.addAttributeNameValue(USERID,data.getId());
    atts.addAttributeNameValue(LOCATION,data.getLocation());
    atts.addAttributeNameValue(TIMESTAMP,data.getTimestamp(), Attribute.LONG);
    atts.addAttributeNameValue(TOUR_END,TRUE);
    return atts;
  }
    

  /**
   * Temporary main method to create a widget with location and port specified by 
   * command line arguments
   */
  public static void main(String argv[]) {
    if (argv.length == 1) {
      if (DEBUG) {
        System.out.println("Attempting to create a WTourEnd on "+DEFAULT_PORT+" at " +argv[0]+ " with storage enabled");
      }
      WTourEnd wte = new WTourEnd(argv[0]);      
    }
    else if (argv.length == 2) {
      if ((argv[1].equals("true")) || (argv[1].equals("false"))) {
        if (DEBUG) {
          System.out.println("Attempting to create a WTourEnd on "+DEFAULT_PORT+" at " +argv[0]+ " with storage set to "+argv[1]);
        }
        WTourEnd wte = new WTourEnd(argv[0], Boolean.valueOf(argv[1]).booleanValue());
      }
      else {
        if (DEBUG) {
          System.out.println("Attempting to create a WTourEnd on "+argv[1]+" at " +argv[0]+" with storage enabled");
        }
        WTourEnd wte = new WTourEnd(argv[0], Integer.parseInt(argv[1]));
      }
    }
    else if (argv.length == 3) {
      if (DEBUG) {
        System.out.println("Attempting to create a WTourEnd on "+argv[1]+" at " +argv[0] +" with storage set to "+argv[2]);
      }
      WTourEnd wte = new WTourEnd(argv[0], Integer.parseInt(argv[1]), Boolean.valueOf(argv[2]).booleanValue());
    }
    else {
      System.out.println("USAGE: java context.arch.widget.WTourEnd <location> [port] [storageFlag]");
    }
    
  }

}
