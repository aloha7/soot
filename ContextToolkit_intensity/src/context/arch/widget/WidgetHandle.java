package context.arch.widget;

/**
 * This class implements a widget handler object, encapsulating the information
 * needed to subscribe to a widget.
 *
 * @see context.arch.widget.WidgetHandles
 */
public class WidgetHandle {

  private String id;
  private String hostname;
  private int port;
  
  /**
   * Basic constructor that creates a WidgetHandle object.
   *
   * @param id ID of the widget being subscribed to 
   * @param hostname Name of the widget's host computer
   * @param port Port number of the widget
   */
  public WidgetHandle(String id, String hostname, int port) {
    this.id = id;
    this.hostname = hostname;
    this.port = port;
  }

  /**
   * Basic constructor that creates a WidgetHandle object.
   *
   * @param id ID of the widget being subscribed to 
   * @param hostname Name of the widget's host computer
   * @param port Port number of the widget
   */
  public WidgetHandle(String id, String hostname, String port) {
    this.id = id;
    this.hostname = hostname;
    this.port = new Integer(port).intValue();
  }

  /**
   * Returns the id of the subscriber
   *
   * @return the subscriber id
   */
  public String getId() {
    return id;
  }

  /**
   * Sets the id of the subscriber
   *
   * @param id ID of the subscriber
   */
  public void setId(String id) {
    this.id = id;
  }

  /**
   * Returns the name of the subscriber's host computer
   *
   * @return the host name of the subscriber
   */
  public String getHostName() {
    return hostname;
  }

  /**
   * Sets the name of the subscriber's host computer
   *
   * @param hostname Name of the subscriber's host computer
   */
  public void setHostName(String hostname) {
    this.hostname = hostname;
  }

  /**
   * Returns the port number to send info to
   *
   * @return the port number of the subscriber
   */
  public int getPort() {
    return port;
  }

  /**
   * Sets the port number to send info to
   *
   * @param port Port number to send information to
   */
  public void setPort(int port) {
    this.port = port;
  }


	public String toString () {
	
		return id + "@" + hostname + ":" + port;
		
	}
}
