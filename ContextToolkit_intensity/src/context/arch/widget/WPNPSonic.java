
package context.arch.widget;

import java.awt.Label;

import context.arch.subscriber.Subscriber;
import context.arch.comm.DataObject;

/**
 * This class is a context widget that provides information on 
 * the in/out status of a person in a particular location.  The
 * information is in the form of a location, user name and in/out
 * status.  It has the following callbacks: UPDATE. It supports 
 * polling and subscriptions.  Currently it only uses the IButton 
 * as a means of providing presence.  It handles only a single IButton
 * instance. It plays certain sounds when people come in and out,
 * (assuming the distinction can be made)
 *
 * @see context.arch.widget.Widget
 * @see context.arch.widget.WPersonNamePresence
 * @see context.arch.generator.PositionIButton
 */
public class WPNPSonic extends WPersonNamePresence {

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
  public static final String CLASSNAME = "PNPSonic";

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
  private static final String SOUND_KIND_IN = "in";
	
  /**
   * Sound kind for out
   */
  private static final String SOUND_KIND_OUT = "out";
	
	// the expected construct is:
	// <playSound>
	//   <soundFile>
	//     a_url
	//   </soundFile>
	// </playSound>
	// or
	// <playSound>
	//   <soundKind>
	//     in or out
	//   </soundKind>
	// </playSound>

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
  public WPNPSonic(String location, String ihost, int iport) {
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
  public WPNPSonic(String location, String ihost, int iport, boolean storageFlag) {
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
  public WPNPSonic(String location, int port, String ihost, int iport) {
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
  public WPNPSonic(String location, int port, String ihost, int iport, boolean storageFlag) {
    super (location, port, ihost, iport, CLASSNAME+SPACER+location, storageFlag);
    setVersion(VERSION_NUMBER);
  }
  /**
   * This method processes the results of subscription callbacks.  It plays
   * a sound depending on whether a person has arrived or left.
   * 
   * @param result DataObject containing the result
   * @param sub Subscriber that returned this reply
   */
  protected void processCallbackReply (DataObject result, Subscriber sub) {
    if (DEBUG) {
      System.out.println ("processCallbackReply\n");
      // System.out.println ("DataObject is: "+result);
    }

    // first fetch the callback reply from the data object
    result = result.getDataObject (Subscriber.SUBSCRIPTION_CALLBACK_REPLY);
  	
    if (result != null) {
      DataObject request;
      request = result.getDataObject (PLAY_SOUND);
  		
      if (request != null) { // do we have a sound play request?
        DataObject soundRef = request.getDataObject (SOUND_FILE);	
        if (soundRef != null) {
          String soundFile = (String)soundRef.getValue().firstElement();
          playSoundFile (soundFile);
        }
        else {
          soundRef = request.getDataObject (SOUND_KIND);
          if (soundRef != null) {
            String soundKind = (String)soundRef.getValue().firstElement();
            if (soundKind.equals (SOUND_KIND_IN)) {
              playInSound ();
            }
            else {
              if (soundKind.equals (SOUND_KIND_OUT)) {
                playOutSound ();
              } 
              else {
                throw new IllegalArgumentException ("in processCallbackReply, the sound kind value was incorrect");
              }
            }
          } 
          else {
            throw new IllegalArgumentException ("in processCallbackReply, the sound kind was not found");
          }
        }
      }
      else {
        if (DEBUG) {
           System.out.println ("in processCallbackReply, unexpected request"+result.getName()+" (probably no big deal)");
        }
        // Note: this is not a real problem BUT we shouldn't be here
        // our caller should have trapped the reply. It doesn't.
      }
    } 
    else {
      throw new IllegalArgumentException ("in processCallbackReply, the callback reply was not found");
    }
  }
  
  /**
   * This method plays an incoming sound: single beep
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
        System.out.println("Attempting to create a WPNPSonic on "+DEFAULT_PORT+" at " +argv[0]+" with storage enabled");
      }
      WPNPSonic wpnio = new WPNPSonic(argv[0], argv[1], Integer.parseInt(argv[2]));
    }
    else if (argv.length == 4) {
      if ((argv[3].equals("false")) || (argv[3].equals("true"))) {
        if (DEBUG) {
          System.out.println("Attempting to create a WPNPSonic on "+argv[1]+" at " +DEFAULT_PORT+" with storage set to "+argv[3]);
        }
        WPNPSonic wpnio = new WPNPSonic(argv[0], argv[1], Integer.parseInt(argv[2]), Boolean.valueOf(argv[3]).booleanValue());
      }
      else {
        if (DEBUG) {
          System.out.println("Attempting to create a WPNPSonic on "+argv[1]+" at " +argv[0]+ "with storage enabled");
        }
        WPNPSonic wpnio = new WPNPSonic(argv[0], Integer.parseInt(argv[1]), argv[2], Integer.parseInt(argv[3]));
      }
    }
    else if (argv.length == 5) {
      if (DEBUG) {
        System.out.println("Attempting to create a WPNPSonic on "+argv[1]+" at " +argv[0]+ "with storage set to: "+argv[4]);
      }
      WPNPSonic wpnio = new WPNPSonic(argv[0], Integer.parseInt(argv[1]), argv[2], Integer.parseInt(argv[3]), Boolean.valueOf(argv[4]).booleanValue());
    }
    else {
      System.out.println("USAGE: java context.arch.widget.WPNPSonic <location> [port] <IIButton2Name-host/ip> <IIButton2Name-port> [storageFlag]");
    }
  }

}
