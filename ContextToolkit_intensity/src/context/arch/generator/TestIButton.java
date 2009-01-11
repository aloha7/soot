package context.arch.generator;

import com.ibutton.oc.JibMultiListener;
import com.ibutton.oc.JibMultiFactory;
import com.ibutton.oc.JibMultiEvent;
import com.ibutton.oc.terminal.jib.iButtonCardTerminal;
import opencard.core.terminal.SlotChannel;
import opencard.core.terminal.CardTerminalException;

/**
 * This class is sample code to test the Java iButton reader.  Whenever
 * an iButton docks with the reader, the iButton id is written to
 * standard out.
 *
 * @see com.ibutton.oc.JibMultiListener
 */
public class TestIButton implements JibMultiListener {

  /**
   * Constructor that allows this object to handle iButton events and
   * starts listening to the iButton reader.  
   *
   * @see com.ibutton.oc.JibMultiFactory
   * @see com.ibutton.oc.JibMultiFactory#addJiBListener(com.ibutton.oc.terminal.jib.JibMultiListener)
   * @see com.ibutton.oc.JibMultiFactory#startPolling(boolean)
   */
  public TestIButton() {
    JibMultiFactory jibFactory = new JibMultiFactory();
    
    /* Add myself to the listener. */
    jibFactory.addJiBListener(this);

    /* Start the polling thread.
       Set to true to use opencard.properties.
       Set to false to enumerate the ports through JibMultiListener.
    */
    try {
      jibFactory.startPolling(true);
    } catch (CardTerminalException cte) {
        System.out.println ("PositionIButton CardTerminalException: "+cte);
    }
  }

  /** 
   * This method is called by the card terminal driver to let us know that a
   * Button insertion has been detected.  It stores the information locally
   * and notifies the registered Widget object, if available, of the
   * new data.
   *
   * @param event Event that indicates an iButton has been inserted
   * @see context.arch.widget.Widget#notify(java.lang.String,java.lang.Object)
   * @see com.ibutton.oc.terminal.jib.JibMultiEvent
   */
  public void iButtonInserted(JibMultiEvent event) {
        
    /* Get a reference to the communications path the event came from. */
    SlotChannel channel = event.getChannel();

    /* Get a reference to the driver for the terminal. */
    iButtonCardTerminal ct = (iButtonCardTerminal)channel.getCardTerminal();
    
    /* Get the iButton ID from the slot. */
    int[] iButtonId = ct.getiButtonId(event.getSlotID());
    
    String currentid = toHexString(iButtonId);
    System.out.println(currentid +" has been docked");
  }

  /** 
   * This method is called by the card terminal driver to let us know that a
   * Button removal has been detected.  It just prints a message to
   * the screen.
   *
   * @param event Event that indicates an iButton has been removed
   * @see com.ibutton.oc.terminal.jib.JibMultiEvent
   */
  public void iButtonRemoved(JibMultiEvent event) {
    System.out.println("iButton has been removed");
  }

  /** 
   * This private method converts the given int array into a hex 
   * string array.
   *
   * @param arr Int array to convert to hex
   * @return Hex string to convert int array from
   */
  private String toHexString(int[] arr) {
    String str = "";
    for (int i = 0;i < arr.length;i++) {
      if (arr[i] < 0x10) {
        str += "0";
      }
      str += Integer.toHexString(arr[i]).toUpperCase();
    }
    return str;
  }

  public static void main(String argv[]) {
    TestIButton tib = new TestIButton();
    Thread t = new Thread();
    try {
      while (true) {
    	  t.sleep(60000);
      }
    } catch (InterruptedException ie) {
        System.out.println("ie: "+ie);
    }
  }
}

