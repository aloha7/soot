package context.arch.widget;

import context.arch.comm.DataObject;
import context.arch.generator.IButtonData;
import context.arch.subscriber.Subscriber;
import context.arch.subscriber.Callbacks;
import context.arch.storage.Attributes;
import context.arch.storage.Attribute;
import context.arch.storage.AttributeNameValues;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * This class is a context widget that provides information on 
 * the presence of a person at tour registration and their contact 
 * info and interest info.  The information is in the form of 
 * a location and a user id.  It has the following callbacks: UPDATE. 
 * It supports polling and subscriptions.  Currently it uses the
 * WPersonPresence as a means of providing presence.
 *
 * @see context.arch.widget.WPersonPresence
 */
public class WTourRegistration extends WPersonPresence implements ActionListener {

  /**
   * Debug flag. Set to true to see debug messages.
   */
  private static final boolean DEBUG = false;

  /**
   * Widget version number
   */
  public String VERSION_NUMBER = "1.0.0";

  /**
   * Tag for contact info 
   */
  public static final String CONTACT_INFO = "contactInfo";

  /**
   * Tag for user name 
   */
  public static final String NAME = "name";

  /**
   * Tag for email address 
   */
  public static final String EMAIL = "email";

  /**
   * Tag for affiliation
   */
  public static final String AFFILIATION = "affiliation";

  /**
   * Tag for INTERESTS 
   */
  public static final String INTERESTS = "interests";

  /**
   * Tag for NO_INTERESTS 
   */
  public static final String NO_INTERESTS = "noInterests";

  /**
   * Tag for Display device
   */
  public static final String DISPLAY_DEVICE = "displayDevice";

  /**
   * Name of widget
   */
  public static final String CLASSNAME = "TourRegistration";

  /**
   * Default port this widget runs on
   */
  public static final int DEFAULT_PORT = 5700;

  private String location;
  private String userid;
  private String interests;
  private String user;
  private String email;
  private String affiliation;
  private RegistrationFrame registration;
  private AttributeNameValues atts;
  	
  /**
   * Constructor that creates the widget at the given location and
   * monitors communications on the DEFAULT_PORT.  It also
   * sets the id of this widget to CLASSNAME_<location value> with
   * storage enabled.
   *  
   * @param location Location the widget is "monitoring"
   */
  public WTourRegistration(String location) {
    this(location, DEFAULT_PORT, true);
  }

  /**
   * Constructor that creates the widget at the given location and
   * monitors communications on the DEFAULT_PORT.  It also
   * sets the id of this widget to CLASSNAME_<location value> with 
   * storage functionality set to storageFlag
   *  
   * @param location Location the widget is "monitoring"
   * @param storageFlag Flag to indicate whether storage is enabled
   */
  public WTourRegistration(String location, boolean storageFlag) {
    this(location, DEFAULT_PORT, storageFlag);
  }


  /**
   * Constructor that creates the widget at the given location and 
   * monitors communications on the given port .  It also
   * sets the id of this widget to CLASSNAME_<location value> with
   * storage enabled.
   *  
   * @param location Location the widget is "monitoring"
   * @param port Port to run the widget on
   */
  public WTourRegistration(String location, int port) {
    this(location,port,CLASSNAME+SPACER+location,true);
  }
  
  /**
   * Constructor that creates the widget at the given location and 
   * monitors communications on the given port .  It also
   * sets the id of this widget to CLASSNAME_<location value> with
   * storage functionality set to storageFlag
   *  
   * @param location Location the widget is "monitoring"
   * @param port Port to run the widget on
   * @param storageFlag Flag to indicate whether storage is enabled
   */
  public WTourRegistration(String location, int port, boolean storageFlag) {
    this(location,port,CLASSNAME+SPACER+location,storageFlag);
  }

  /**
   * Constructor that creates the widget at the given location and 
   * monitors communications on the given port It also
   * sets the id of this widget to the given id with
   * storage functionality set to storageFlag
   *  
   * @param location Location the widget is "monitoring"
   * @param port Port to run the widget on
   * @param id Widget id
   * @param storageFlag Flag to indicate whether storage is enabled
   */
  public WTourRegistration(String location, int port, String id, boolean storageFlag) {
    super(location,port,id, storageFlag);
    this.location = location;
  }

  /**
   * This method implements the abstract method Widget.setAttributes().
   * It defines the attributes for the widget as:
   *    TIMESTAMP, USERID, LOCATION, INTERESTS, and CONTACT_INFO(NAME,EMAIL,AFFILIATION)
   *
   * @return the Attributes used by this widget
   */
  protected Attributes setAttributes() {
    Attributes atts = super.setAttributes();
    atts.addAttribute(INTERESTS);
    Attributes contact = new Attributes();
    contact.addAttribute(NAME);
    contact.addAttribute(EMAIL);
    contact.addAttribute(AFFILIATION);
    atts.addAttribute(CONTACT_INFO,contact,Attribute.STRUCT);
    atts.addAttribute(DISPLAY_DEVICE);
    return atts;
  }

  /**
   * This method implements the abstract method Widget.setCallbacks().
   * It defines the callbacks for the widget as:
   *    UPDATE with the attributes TIMESTAMP, USERID, LOCATION, 
   *                CONTACT_INFO(NAME,EMAIL,AFFILIATION), INTERESTS
   *                and DISPLAY_DEVICE
   *
   * @return the Callbacks used by this widget
   */
  protected Callbacks setCallbacks() {
    Callbacks calls = new Callbacks();
    calls.addCallback(UPDATE,setAttributes());
    return calls;
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
	  //2008/7/9
	  if(event.equals(WTourRegistration.UPDATE)){
//		  System.out.println(this.getClass().getName() + " gets a context:" + event);
		  
		    atts = IButtonData2Attributes((IButtonData)data);
		    
		    //2008/7/13:disable GUI.
//		    registration = new RegistrationFrame(this,",");
//		    registration.setTitle("Tour Registration");
//		    registration.pack();
//		    registration.setResizable(true);
//		    registration.show();
//		    
		    
		    AttributeNameValues subAtts = new AttributeNameValues();
		    subAtts.addAttributeNameValue(NAME,"Wang Huai");
		    subAtts.addAttributeNameValue(AFFILIATION,"HKU");
		    subAtts.addAttributeNameValue(EMAIL,"dragonwanghuai@gmail.com");
		    atts.addAttributeNameValue(CONTACT_INFO,subAtts,Attribute.STRUCT);		  		    			   
		    atts.addAttributeNameValue(INTERESTS,"context,application,capture");
		    
		    atts.addAttributeNameValue(DISPLAY_DEVICE,"127.0.0.1");
		    
		    if (subscribers.numSubscribers() > 0) {
		          sendToSubscribers(UPDATE, atts);
		    }
		        store(atts);		    		    		     		    	
	  }
    
  }

  /**
   * This method is used to implement the ActionListener interface.  It is
   * called when the user submits or cancels the interaction with the 
   * registration frame.  If the user submits their registration information,
   * the information is collected and sent to subscribers.  The frame is
   * disposed of.
   *
   * @param e ActionEvent caused by interaction with a button
   */
  
  public void actionPerformed(ActionEvent e) {	  	  
    if (e.getActionCommand().equals(registration.SUBMIT)) {
      AttributeNameValues subAtts = new AttributeNameValues();
      subAtts.addAttributeNameValue(NAME,registration.getName());
      subAtts.addAttributeNameValue(AFFILIATION,registration.getAffiliation());
      subAtts.addAttributeNameValue(EMAIL,registration.getEmail());
      atts.addAttributeNameValue(CONTACT_INFO,subAtts,Attribute.STRUCT);
      if (registration.getInterests().trim().length() == 0) {
        atts.addAttributeNameValue(INTERESTS,NO_INTERESTS);
      }
      else {
        atts.addAttributeNameValue(INTERESTS,registration.getInterests());
      }
      atts.addAttributeNameValue(DISPLAY_DEVICE,registration.getDevice());
      if (subscribers.numSubscribers() > 0) {
        sendToSubscribers(UPDATE, atts);
      }
      store(atts);
    }
    registration.dispose();
  }    
  
  
  /**
   * Temporary main method to create a widget with location and port specified by 
   * command line arguments
   */
  public static void main(String argv[]) {
    if (argv.length == 1) {
      if (DEBUG) {
        System.out.println("Attempting to create a WTourRegistration on "+DEFAULT_PORT+" at " +argv[0]+" with storage enabled");
      }
      WTourRegistration wtr = new WTourRegistration(argv[0]);                
     
    }
    else if (argv.length == 2) {
      if ((argv[1].equals("true")) || (argv[1].equals("false"))) {
        if (DEBUG) {
          System.out.println("Attempting to create a WTourRegistration on "+DEFAULT_PORT+" at " +argv[0]+" with storage set to "+argv[1]);
        }
        WTourRegistration wtr = new WTourRegistration(argv[0], Boolean.valueOf(argv[1]).booleanValue());
      }
      else {
        if (DEBUG) {
          System.out.println("Attempting to create a WTourRegistration on "+argv[1]+" at " +argv[0]+" with storage enabled");
        }
        WTourRegistration wtr = new WTourRegistration(argv[0], Integer.parseInt(argv[1]));
       
        //wtr.notify("hello", new IButtonData(argv[0],"01020304", "1"));
      }
    }
    else if (argv.length == 3) {
      if (DEBUG) {
        System.out.println("Attempting to create a WTourRegistration on "+argv[1]+" at " +argv[0]+" with storage set to "+argv[2]);
      }
      WTourRegistration wtr = new WTourRegistration(argv[0], Integer.parseInt(argv[1]), Boolean.valueOf(argv[2]).booleanValue());
    }
    else {
      System.out.println("USAGE: java context.arch.widget.WTourRegistration <location> [port] [storageFlag]");
    }
  }

}
