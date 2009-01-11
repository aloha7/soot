package context.arch.handler;

/**
 * This class acts as a container for handler info.  It holds a reference to
 * a widget handler, the subscription id, the id of the widget, the name of
 * the callback on the subscriber side, and the name of the callback on the 
 * widget side.
 *
 * @see context.arch.handler.Handler
 */
public class HandlerInfo {

  private Handler handler;
  private String subId;
  private String remoteId;
  private String callback;
  private String remoteCallback;

  /**
   * Basic empty constructor
   */
  public HandlerInfo() {
  }

  /**
   * Full constructor that takes all input parameters
   *
   * @param handler Reference to a context widget handler
   * @param subId Subscription id of a subscriber. This is returned anytime a callback 
   *		message is sent between the subscriber to the widget (in either direction).
   * @param remoteId Id of the widget object
   * @param callback Name of the callback on the subscriber side
   * @param remoteCallback Name of the callback on the widget side
   */
  public HandlerInfo(Handler handler, String subId, String remoteId, String remoteCallback, String callback) {
    this.handler = handler;
    this.subId = subId;
    this.remoteId = remoteId;
    this.remoteCallback = remoteCallback;
    this.callback = callback;
  }

  /**
   * Returns the subscription id
   *
   * @return the subscription id
   */
  public String getSubId() {
    return subId;
  }

  /**
   * Sets the subscription id
   *
   * @param the subscription id
   */
  public void setSubId(String id) {
    subId = id;
  }

  /**
   * Returns the widget id
   *
   * @return the widget id
   */
  public String getRemoteId() {
    return remoteId;
  }

  /**
   * Sets the widget id
   *
   * @param the widget id
   */
  public void setRemoteId(String id) {
    remoteId = id;
  }

  /**
   * Returns the name of the subscription callback
   *
   * @return the name of the subscription callback
   */
  public String getCallback() {
    return callback;
  }

  /**
   * Sets the name of the subscription callback
   *
   * @param the name of the subscription callback
   */
  public void setCallback(String callback) {
    this.callback = callback;
  }

  /**
   * Returns the name of the widget callback
   *
   * @return the name of the widget callback
   */
  public String getRemoteCallback() {
    return remoteCallback;
  }

  /**
   * Sets the name of the widget callback
   *
   * @param the name of the widget callback
   */
  public void setRemoteCallback(String remote) {
    remoteCallback = remote;
  }

  /**
   * Returns the context widget handler
   *
   * @return the context widget handler
   */
  public Handler getHandler() {
    return handler;
  }

  /**
   * Sets the context widget handler
   *
   * @param the context widget handler
   */
  public void setHandler(Handler handler) {
    this.handler = handler;
  }
}
