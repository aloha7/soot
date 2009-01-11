package context.arch.widget;

import java.awt.Label;

import context.arch.storage.AttributeNameValues;
import context.arch.generator.IButtonData;

/**
 * This class is a context widget that provides information on 
 * the in/out status of a person in a particular location.  The
 * information is in the form of a location, user name and in/out
 * status.  It has the following callbacks: UPDATE. It supports 
 * polling and subscriptions.  Currently it only uses the IButton 
 * as a means of providing presence.  It handles only a single IButton
 * instance. It plays certain sounds when people come in and out.
 *
 * @see context.arch.widget.Widget
 * @see context.arch.widget.WPersonNameInOut
 * @see context.arch.generator.PositionIButton
 */
public class WPNIOSonic extends WPersonNameInOut {

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
  public static final String CLASSNAME = "PNIOSonic";

  /**
   * The default port this widget runs on is 5300
   */
  public static final int DEFAULT_PORT = 5300;

  /**
   * Tag that indicates a sound should be played
   */
  public static final String PLAY_SOUND = "playSound";
	
  /**
   * Tag that indicates the sound kind to play (in or out)
   */
  public static final String SOUND_KIND = "soundKind";
	
  /**
   * Tag that indicates the sound file to play (URL)
   */
  public static final String SOUND_FILE = "soundFile";
	
  /**
   * Sound kind for in
   */
  private static final String SOUND_KIND_IN = IN;
	
  /**
   * Sound kind for out
   */
  private static final String SOUND_KIND_OUT = OUT;
	
  /**
   * Constructor that creates the widget at the given location and
   * monitors communications on the DEFAULT_PORT and
   * creates an instance of the IButton position generator.  It also
   * sets the id of this widget to CLASSNAME_<location value> and enables storage.
   *  
   * @param location Location the widget is "monitoring"
   * @param ihost Hostname/ip the IIButton2Name is running on
   * @param iport Port the IIButton2Name is running on
   *
   * @see context.arch.generator.PositionIButton
   */
  public WPNIOSonic(String location, String ihost, int iport) {
    this(location, DEFAULT_PORT, ihost, iport, true);
  }

  /**
   * Constructor that creates the widget at the given location and
   * monitors communications on the DEFAULT_PORT and
   * creates an instance of the IButton position generator.  It also
   * sets the id of this widget to CLASSNAME_<location value> and sets
   * storage functionality to storageFlag
   *  
   * @param location Location the widget is "monitoring"
   * @param ihost Hostname/ip the IIButton2Name is running on
   * @param iport Port the IIButton2Name is running on
   * @param storageFlag Flag to indicate whether storage is enabled or not
   *
   * @see context.arch.generator.PositionIButton
   */
  public WPNIOSonic(String location, String ihost, int iport, boolean storageFlag) {
    this(location, DEFAULT_PORT, ihost, iport, storageFlag);
  }

  /**
   * Constructor that creates the widget at the given location and 
   * monitors communications on the given port and
   * creates an instance of the IButton position generator.  It also
   * sets the id of this widget to CLASSNAME_<location value> and enables storage.
   *  
   * @param location Location of the widget
   * @param port Port of the machine the widget is running on
   * @param ihost Hostname of the machine the interpreter is running on
   * @param iport Port of the machine the interpreter is running on
   */
  public WPNIOSonic (String location, int port, String ihost, int iport) {
    this(location, port, ihost, iport, true);
  }
  
  /**
   * Constructor that creates the widget at the given location and 
   * monitors communications on the given port and
   * creates an instance of the IButton position generator.  It also
   * sets the id of this widget to CLASSNAME_<location value> and sets
   * storage to storageFlag.
   *  
   * @param location Location of the widget
   * @param port Port of the machine the widget is running on
   * @param ihost Hostname of the machine the interpreter is running on
   * @param iport Port of the machine the interpreter is running on
   * @param storageFlag Flag to indicate whether storage is enabled or not
   */
  public WPNIOSonic (String location, int port, String ihost, int iport, boolean storageFlag) {
    super (location, port, ihost, iport, CLASSNAME+SPACER+location, storageFlag);
    setVersion(VERSION_NUMBER);
  }

  /**
   * This method converts the IButtonData object to an AttributeNameValues
   * object.  It overrides the method in WPersonNameInOut, basically
   * doing the same thing, except it also plays a sound depending on whether
   * the person's status is in or out
   *
   * @param data IButtonData object to be converted
   * @return AttributeNameValues object containing the data in the IButtonData object
   */
  protected AttributeNameValues IButtonData2Attributes(IButtonData data) {
    AttributeNameValues atts = super.IButtonData2Attributes(data);
    if (atts == null) {
      return null;
    }

    String status = (String)atts.getAttributeNameValue(INOUT).getValue();
    if (status.equals(IN)) {
      playInSound ();
    }
    else {
      playOutSound ();
    }
    return atts;
  }
  
  /**
   * This method plays an incoming sound: a single beep
   */
  private void playInSound () {
    // incredibly, we need a graphic component to play a sound in Java :-(
    Label javaSucks = new Label ();
  	
    if (DEBUG) {
      System.out.println ("Playing in sound");
    }
  	
    javaSucks.getToolkit().beep();
  }
  
  /**
   * This method plays an outgoing sound: two beeps
   */
  private void playOutSound () {
    // incredibly, we need a graphic component to play a sound in Java :-(
    Label javaSucks = new Label ();

    if (DEBUG) {
      System.out.println ("Playing out sound");
    }

    javaSucks.getToolkit().beep();
    try {
      Thread.sleep (1000);
    } catch (InterruptedException ie) {
    }
    javaSucks.getToolkit().beep();
  }
  
  /**
   * This method plays a sound file at the given location
   *
   * @param url Location of the sound file to play
   */
  private void playSoundFile (String url) {
    if (DEBUG) {
      System.out.println ("Playing out sound file: " + url);
    }
  }

  /**
   * Temporary main method to create a widget with location and port specified by 
   * command line arguments
   */
  public static void main(String argv[]) {
     if (argv.length == 3) {
      if (DEBUG) {
        System.out.println("Attempting to create a WPNIOSonic on "+DEFAULT_PORT+" at " +argv[0]+" with storage enabled");
      }
      WPNIOSonic wpnio = new WPNIOSonic(argv[0], argv[1], Integer.parseInt(argv[2]));
    }
    else if (argv.length == 4) {
      if ((argv[3].equals("false")) || (argv[3].equals("true"))) {
        if (DEBUG) {
          System.out.println("Attempting to create a WPNIOSonic on "+argv[1]+" at " +DEFAULT_PORT+" with storage set to "+argv[3]);
        }
        WPNIOSonic wpnio = new WPNIOSonic(argv[0], argv[1], Integer.parseInt(argv[2]), Boolean.valueOf(argv[3]).booleanValue());
      }
      else {
        if (DEBUG) {
          System.out.println("Attempting to create a WPNIOSonic on "+argv[1]+" at " +argv[0]+ "with storage enabled");
        }
        WPNIOSonic wpnio = new WPNIOSonic(argv[0], Integer.parseInt(argv[1]), argv[2], Integer.parseInt(argv[3]));
      }
    }
    else if (argv.length == 5) {
      if (DEBUG) {
        System.out.println("Attempting to create a WPNIOSonic on "+argv[1]+" at " +argv[0]+ "with storage set to: "+argv[4]);
      }
      WPNIOSonic wpnio = new WPNIOSonic(argv[0], Integer.parseInt(argv[1]), argv[2], Integer.parseInt(argv[3]), Boolean.valueOf(argv[4]).booleanValue());
    }
    else {
      System.out.println("USAGE: java context.arch.widget.WPNIOSonic <location> [port] <IIButton2Name-host/ip> <IIButton2Name-port> [storageFlag]");
    }
  }

}
