package context.arch.subscriber;

import context.arch.storage.Conditions;
import context.arch.storage.Attributes;
import context.arch.comm.DataObject;

import java.util.Vector;

/**
 * This class implements a subscriber object, encapsulating the information
 * needed to create a subscriber and send information to it.
 *
 * @see context.arch.subscriber.Subscribers
 */
public class Subscriber {

  /**
   * Tag for a subscriber
   */
  public static final String SUBSCRIBER = "subscriber";

  /**
   * Tag for host machine of component 
   */
  public static final String HOSTNAME = "hostname";

  /**
   * Tag for port number of component
   */
  public static final String PORT = "port";

  /**
   * Tag for subscription id
   */
  public static final String SUBSCRIBER_ID = "subscriberId";

  /**
   * Tag to indicate message is a subscription reply
   */
  public static final String SUBSCRIPTION_REPLY = "subscriptionReply";

  /**
   * Tag for callback tag (on subscriber side)
   */
  public static final String CALLBACK_TAG = "callbackTag";

  /**
   * Tag for callback (on widget side)
   */
  public static final String CALLBACK_NAME = Callback.CALLBACK_NAME;

  /**
   * Tag to indicate message is a subscription callback
   */
  public static final String SUBSCRIPTION_CALLBACK = "subscriptionCallback";

  /**
   * Tag to indicate message is for adding a subscriber
   */
  public static final String ADD_SUBSCRIBER = "addSubscriber";

  /**
   * Tag to indicate message is for removing a subscriber
   */
  public static final String REMOVE_SUBSCRIBER = "removeSubscriber";

  /**
   * Tag to indicate message is the reply to a subscription callback message
   */
  public static final String SUBSCRIPTION_CALLBACK_REPLY = "subscriptionCallbackReply";

  /**
   * Maximum number of consecutive communication errors to be tolerated 
   */
  public static final int MAX_ERRORS = 5;

  private String id;
  private String host;
  private int port;
  private String callback;
  private String tag;
  private Conditions conditions;
  private Attributes attributes;
  int errors;

  /**
   * Basic constructor that creates a subscriber object.
   *
   * @param id ID of the subscriber
   * @param hostname Name of the subscriber's host computer
   * @param port Port number to send information to
   * @param callback Callback the subscriber will implement
   * @param tag Widget callback the subscriber is subscribing to
   * @param conditions Any subscription conditions to use
   * @param attributes Attributes to return to subscriber
   */
  public Subscriber(String id, String hostname, int port, String callback, 
                    String tag, Conditions conditions, Attributes attributes) {
    this.id = id;
    host = hostname;
    this.port = port;
    this.callback = callback;
    this.tag = tag;
    this.conditions = conditions;
    this.attributes = attributes;
    errors = 0;
  }

  /**
   * Basic constructor that creates a subscriber object.
   *
   * @param id ID of the subscriber
   * @param hostname Name of the subscriber's host computer
   * @param port Port number to send information to
   * @param callback Callback the subscriber will implement
   * @param tag Widget callback the subscriber is subscribing to
   * @param conditions Any subscription conditions to use
   * @param attributes Attributes to return to subscriber
   */
  public Subscriber(String id, String hostname, String port, String callback, 
                    String tag, Conditions conditions, Attributes attributes) {
    this(id,hostname,new Integer(port).intValue(),callback,tag,conditions,attributes);
  }

  /**
   * Basic constructor that creates a subscriber object from a DataObject.
   * The DataObject must contain a <SUBSCRIBER> tag
   *
   * @param data DataObject containing the subscriber info
   */
  public Subscriber(DataObject data) {
    DataObject sub = data.getDataObject(SUBSCRIBER);
    id = (String)sub.getDataObject(SUBSCRIBER_ID).getValue().firstElement();
    host = (String)sub.getDataObject(HOSTNAME).getValue().firstElement();
    port = new Integer(((String)sub.getDataObject(PORT).getValue().firstElement())).intValue();
    callback = (String)sub.getDataObject(CALLBACK_NAME).getValue().firstElement();
    tag = (String)sub.getDataObject(CALLBACK_TAG).getValue().firstElement();
    conditions = new Conditions(sub);
    attributes = new Attributes(sub);
    errors = 0;
  }

  /**
   * This method converts the subscriber info to a DataObject
   *
   * @return Subscriber object converted to a <SUBSCRIBER> DataObject
   */
  public DataObject toDataObject() {
    Vector v = new Vector();
    v.addElement(new DataObject(SUBSCRIBER_ID, id));
    v.addElement(new DataObject(HOSTNAME, host));
    v.addElement(new DataObject(PORT, Integer.toString(port)));
    v.addElement(new DataObject(CALLBACK_NAME, callback));
    v.addElement(new DataObject(CALLBACK_TAG, tag));
    v.addElement(conditions.toDataObject());
    v.addElement(attributes.toDataObject());
    return new DataObject(SUBSCRIBER, v);
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
    return host;
  }

  /**
   * Sets the name of the subscriber's host computer
   *
   * @param hostname Name of the subscriber's host computer
   */
  public void setHostName(String hostname) {
    host = hostname;
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

  /**
   * Returns the subscriber callback that the subscriber registered
   *
   * @return the callback of the subscriber
   */
  public String getCallback() {
    return callback;
  }

  /**
   * Sets the subscriber callback that the subscriber wants to register for
   *
   * @param String Widget callback being registered for
   */
  public void setCallback(String callback) {
    this.callback = callback;
  }

  /**
   * Returns the widget callback that the subscriber registered for
   *
   * @return the widget callback
   */
  public String getTag() {
    return tag;
  }

  /**
   * Sets the widget callback that the subscriber wants to register for
   */
  public void setTag(String tag) {
    this.tag = tag;
  }

  /**
   * Sets the subscription conditions, under which the subscriber will be notified
   *
   * @param conditions Subscription conditions used for notification
   */
  public void setConditions(Conditions conditions) {
    this.conditions = conditions;
  }

  /**
   * Returns the subscription conditions, under which the subscriber will be notified
   *
   * @return subscription conditions used for notification
   */
  public Conditions getConditions() {
    return conditions;
  }

  /**
   * Sets the attributes to return to the subscriber
   *
   * @param attributes Attributes to return to the subscriber
   */
  public void setAttributes(Attributes attributes) {
    this.attributes = attributes;
  }

  /**
   * Returns the subscription attributes to be returned
   *
   * @return subscription attributes to return to subscriber
   */
  public Attributes getAttributes() {
    return attributes;
  }

  /**
   * Increment the error counter
   */
  public void addError() {
    errors++;
  }

  /**
   * Reset the error counter
   */
  public void resetErrors() {
    errors = 0;
  }

  /**
   * Returns the number of consecutive errors in trying to communicate with
   * this subscriber
   *
   * @return number of consecutive communications errors for this subscriber
   */
  public int getErrors() {
    return errors;
  }

}
