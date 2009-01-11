package context.arch.server;

import context.arch.comm.DataObject;
import context.arch.handler.Handler;
import context.arch.widget.Widget;
import context.arch.widget.WidgetHandle;
import context.arch.widget.WidgetHandles;
import context.arch.storage.Conditions;
import context.arch.storage.Storage;
import context.arch.storage.Attributes;
import context.arch.storage.Attribute;
import context.arch.storage.AttributeNameValues;
import context.arch.storage.AttributeNameValue;
import context.arch.subscriber.Callbacks;
import context.arch.subscriber.Callback;
import context.arch.service.Services;
import context.arch.service.InheritedService;
import context.arch.service.helper.ServiceDescriptions;
import context.arch.service.helper.ServiceDescription;
import context.arch.MethodException;
import context.arch.InvalidMethodException;
import context.arch.util.Error;

import java.util.Hashtable;
import java.util.Vector;
import java.util.Enumeration;

/**
 * This class is the basic context server, with attributes and
 * methods that should apply to all context servers.
 *
 * A server is a widget with added gathering and storage facilities.
 * Servers are attached to people (incl. groups), places and things.
 *
 * Basically, a server subscribes to a set of widgets, stores and
 * updates the attribute values from the widgets.
 *
 * A server has a "key" attribute that identifies the entity it is
 * attached to. For example, a user server's key may be USERNAME. The server
 * will only request and store information that pertains to a give value
 * of USERNAME.
 *
 * @see context.arch.widget.Widget
 */
public abstract class Server extends Widget implements Handler {

  /**
   * Debug flag. Set to true to see debug messages.
   */
  private static final boolean DEBUG = true;

  /**
   * Default port to use for communications.
   */
  public static final int DEFAULT_PORT = 6000;

  /**
   * The set of widgets this server is monitoring.
   */
  protected WidgetHandles widgets;	

  private int serverPort;
  private AttributeNameValues attributesCache;
  private Hashtable attributesTimes;
     
  /**
   * Constructor that sets up internal variables for maintaining
   * the list of server attributes and callbacks, and setting up
   * the Widget info.
   *
   * @param clientClass Class to use for client communications
   * @param serverClass Class to use for server communications
   * @param serverPort Port to use for server communications
   * @param encoderClass Class to use for communications encoding
   * @param decoderClass Class to use for communications decoding
   * @param storageClass Class to use for storage
   * @param id String to use for widget id and persistent storage 
   * @param widgets The set of widgets this server will subscribe to
   */
  public Server(String clientClass, String serverClass, int serverPort, String encoderClass,
                String decoderClass, String storageClass, String id,
                WidgetHandles widgets) {
    super(clientClass,serverClass,serverPort,encoderClass,decoderClass,storageClass,id);
    this.widgets = widgets;
    this.serverPort = serverPort;
    serverSetup();
  }    

  /**
   * Constructor that sets up internal variables for maintaining
   * the list of server attributes and callbacks, and setting up
   * the Widget info. This version takes a boolean to indicate whether the 
   * default storage class should be used or whether not storage should be 
   * provided.
   *
   * @param clientClass Class to use for client communications
   * @param serverClass Class to use for server communications
   * @param serverPort Port to use for server communications
   * @param encoderClass Class to use for communications encoding
   * @param decoderClass Class to use for communications decoding
   * @param storageFlag Flag to determine whether storage should be used or not
   * @param id String to use for widget id and persistent storage 
   * @param widgets The set of widgets this server will subscribe to
   */
  public Server(String clientClass, String serverClass, int serverPort, String encoderClass,
                String decoderClass, boolean storageFlag, String id,
                WidgetHandles widgets) {
    super(clientClass,serverClass,serverPort,encoderClass,decoderClass,storageFlag,id);
    this.widgets = widgets;
    this.serverPort = serverPort;
    serverSetup();
  }    

  /**
   * Constructor that sets up internal variables for maintaining
   * the list of server attributes and callbacks.  It takes a port
   * number as a parameter to indicate which port to listen for
   * messages/connections.
   *
   * @param port Port to listen to for incoming messages
   * @param id Widget id
   * @param widgets The set of widgets this server will subscribe to
   */
  public Server(int port, String id, WidgetHandles widgets) {
    this(null,null,port,null,null,null,id, widgets);
  }

  /**
   * Constructor that sets up internal variables for maintaining
   * the list of server attributes and callbacks.  It takes a port
   * number as a parameter to indicate which port to listen for
   * messages/connections.
   *
   * @param port Port to listen to for incoming messages
   * @param id Widget id
   * @param storageFlag Flag to determine whether storage should be used or not
   * @param widgets The set of widgets this server will subscribe to
   */
  public Server(int port, String id, boolean storageFlag, WidgetHandles widgets) {
    this(null,null,port,null,null,storageFlag,id, widgets);
  }
	
  /**
   * Constructor that sets up internal variables for maintaining
   * the list of server attributes and callbacks.  It takes the 
   * widget id as a parameter
   *
   * @param id ID of the widget
   * @param widgets The set of widgets this server will subscribe to
   */
  public Server(String id, WidgetHandles widgets) {
    this(null,null,-1,null,null,null,id, widgets);
  }

  /**
   * Constructor that sets up internal variables for maintaining
   * the list of server attributes and callbacks.  It takes the 
   * widget id as a parameter with storage functionality set to storageFlag
   *
   * @param id ID of the widget
   * @param widgets The set of widgets this server will subscribe to
   * @param storageFlag Flag to determine whether storage should be used or not
   */
  public Server(String id, boolean storageFlag, WidgetHandles widgets) {
    this(null,null,-1,null,null,storageFlag,id, widgets);
  }

  /** 
   * This method sets up the server for use.  This includes getting the 
   * attributes and services information from relevant widgets.
   *
   * @see #setAttributes()
   * @see #setServices()
   */
  protected void serverSetup() {
    attributes = setAttributes();
    attributesCache = new AttributeNameValues();
    attributeTypes = attributes.toTypesHashtable();
    for (int i=0; i<attributes.numAttributes(); i++) {
      Attribute att = attributes.getAttributeAt(i);
      attributesCache.addAttributeNameValue(att.getName(),att.getSubAttributes(),att.getType());
    }
    attributesTimes = new Hashtable();
    Long long1 = new Long(0);
    for (Enumeration e = attributeTypes.keys(); e.hasMoreElements();) {
      attributesTimes.put(e.nextElement(),long1);
    }
    if (storage != null) {
      storage.setAttributes(attributes,attributeTypes);
    }
    services = setServices();
  }

  /**
   * This method sets the widgets that this server should subscribe to.
   * Either the constructor should include a widgets parameter or this
   * method should be called before startSubscriptions() is called
   *
   * @param widgets Handles of widgets to subscribe to
   */
  public void setWidgets(WidgetHandles widgets) {
    this.widgets = widgets;
  }

  /**
   * This method is called to subscribe to the widgets this server
   * is interested in.  The reason this is not part of the constructor
   * is that the individual server's conditions will not be set yet.
   * This should be called after a constructor sets the widget handles 
   * to use or after setWidgets() has been called.
   *
   * @see #setCallbacks()
   */
  public void startSubscriptions() {
    callbacks = setCallbacks();
  }

  /**
   * This method is called to aggregate the list of attributes that the
   * widgets relevant to this server provide.
   * This should be called after a constructor sets the widget handles 
   * to use or after setWidgets() has been called.
   *
   * @return the server attributes
   */
  protected Attributes setAttributes() {

    // this protects us against the Widget constructor 
    // that calls us too early (we havent' got the widgets yet)
    // it's good practice anyway
    if (widgets == null) {
      return null;
    }

    Attributes atts = new Attributes();
    for (int i = 0; i < widgets.size (); i++) {
      WidgetHandle handle = widgets.getWidgetHandleAt (i);

      DataObject widgetAtts = getWidgetAttributes(handle.getHostName(), handle.getPort(), handle.getId());
      String error = new Error(widgetAtts).getError();
      if (error != null) {
        if (error.equals(Error.NO_ERROR)) {
          Attributes wAtts = new Attributes(widgetAtts);
          for (int j=0; j<wAtts.numAttributes(); j++) {
            Attribute wAtt = wAtts.getAttributeAt(j);
            if (atts.getAttribute(wAtt.getName()) == null) {
              atts.addAttribute(wAtt);
            }
          }
        }
      }
    }

    atts.addAttributes(setServerAttributes());

    return atts;
  }

  /**
   * This abstract method set the attributes for a server - those
   * that are specific to the server, and not contained in the widgets
   * it subscribes to.
   *
   * @return Attributes specific to the server
   */
  protected abstract Attributes setServerAttributes();

  /**
   * This method is called to aggregate the list of services that the
   * widgets relevant to this widget provide.  This allows the server
   * to act as a proxy to the individual widgets' services.
   * This should be called after a constructor sets the widget handles 
   * to use or after setWidgets() has been called.
   *
   * @return the server services
   */
  protected Services setServices() {
    if (widgets == null) {
      return null;
    }

    Services services = new Services();
    for (int i=0; i<widgets.size(); i++) {
      WidgetHandle handle = widgets.getWidgetHandleAt(i);
      DataObject widgetServices = getWidgetServices(handle.getHostName(), handle.getPort(), handle.getId());
      String error = new Error(widgetServices).getError();
      if (error != null) {
        if (error.equals(Error.NO_ERROR)) {
          ServiceDescriptions wServices = new ServiceDescriptions(widgetServices);
          for (int j=0; j<wServices.numServiceDescriptions(); j++) {
            ServiceDescription desc = wServices.getServiceDescriptionAt(j);
            services.addService(new InheritedService(this,getId(),desc.getName(),desc.getFunctionDescriptions(),
                                handle.getHostName(),Integer.toString(handle.getPort()),handle.getId()));
          }
        }
      }
    }
    services.addServices(setServerServices());

    return services;
  }

  /**
   * This abstract method set the services for a server - those
   * that are specific to the server, and not contained in the widgets
   * it subscribes to.
   *
   * @return Services specific to the server
   */
  protected abstract Services setServerServices();

  /**
   * This method is called to subscribe to the widgets this server
   * is interested in.  It allows the server to act as a proxy to
   * the callbacks provided by each individual widget.
   * This should be called after a constructor sets the widget handles 
   * to use or after setWidgets() has been called.
   *
   * @see #setCallbacks()
   */
  protected Callbacks setCallbacks() {
  
    // this protects us against the Widget constructor 
    // that calls us too early (we havent' got the widgets yet)
    // it's good practice anyway
    if (widgets == null) {
      return null;
    }

    Callbacks calls = new Callbacks();
    for (int i = 0; i < widgets.size (); i++) {
      WidgetHandle handle = widgets.getWidgetHandleAt (i);
      //get callbacks from widgets firstly
      DataObject widgetCalls = getWidgetCallbacks(handle.getHostName(), handle.getPort(), handle.getId());
      String error = new Error(widgetCalls).getError();
      if (error != null) {
        if (error.equals(Error.NO_ERROR)) {
          Callbacks cbacks = new Callbacks(widgetCalls);
          for (int j=0; j<cbacks.numCallbacks(); j++) {
            Callback call = cbacks.getCallbackAt(j);
            //subscribe to these widgets
            Error done = subscribeTo((Handler)this, serverPort, getId(), handle.getHostName(),
                     handle.getPort(), handle.getId(), call.getName(), handle.getId()+"_"+call.getName(),
                     setConditions());
            call.setName(handle.getId()+"_"+call.getName());
            calls.addCallback(call);
          }
        }
      }
    }

    calls.addCallbacks(setServerCallbacks());

    return calls;
  }

  /**
   * This abstract method set the callbacks for a server - those
   * that are specific to the server, and not contained in the widgets
   * it subscribes to.
   *
   * @return Callbacks containing callbacks specific to the server
   */
  protected abstract Callbacks setServerCallbacks();

  /**
   * This abstract method sets the conditions to apply to the
   * server's subscriptions.
   *
   * @return Conditions containing condition info for server subscriptions
   */
  protected abstract Conditions setConditions();

  /**
   * This method implements the handle method in the Handler interface.  
   * It handles the subscription information supplied by widgets this
   * server has subscribed to.
   *
   * @param callback The name of the widget callback (on the subscriber side) triggered
   * @param data DataObject containing the data for the widget callback 
   * @return DataObject containing any directives to the widget that created the callback
   * @exception context.arch.InvalidMethodException if the callback specified isn't recognized	
   * @exception context.arch.MethodException if the callback specified can't be handled successfully
   */
  public DataObject handle(String callback, DataObject data) throws InvalidMethodException, MethodException {
    if (callbacks.hasCallback(callback)) {
      AttributeNameValues attsObj = new AttributeNameValues(data);
      if (attsObj != null) {
        sendToSubscribers(callback,attsObj);
        storeAttributeNameValues(attsObj);
      }
    }
    else {
      throw new InvalidMethodException(Error.UNKNOWN_CALLBACK_ERROR);
    }
    return null;
  }
  
  /**
   * This method runs a query on a widget, asking for either it's latest
   * acquired data (QUERY) or asking for the widget to acquire and return
   * new data (UPDATE_AND_QUERY).  Currently, it deals with QUERY and
   * UPDATE_AND_QUERY in exactly the same way.
   *
   * @param query DataObject containing the query request
   * @param update Whether or not to acquire new data
   * @param error String containing the incoming error value
   * @return DataObject containing the reply to the query
   */
  protected DataObject queryWidget(DataObject query, boolean update, String error) {
    DataObject result = null;
    Vector v = new Vector();
    if (update) {
      result = new DataObject(UPDATE_AND_QUERY_REPLY, v);
    }
    else {
      result = new DataObject(QUERY_REPLY, v);
    }

    Attributes atts = new Attributes(query);
    Error err = new Error(error);
    if (err.getError() == null) {
      if (atts == null) {
        err.setError(Error.MISSING_PARAMETER_ERROR);
      }
      else if (!canHandle(atts)) {
        err.setError(Error.INVALID_ATTRIBUTE_ERROR);
      }
    }

    if (err.getError() == null) {
      AttributeNameValues subset = attributesCache.getSubset(atts);
      if (subset.numAttributeNameValues() == 0) {
        err.setError(Error.INVALID_DATA_ERROR);
      }
      else {
        v.addElement(subset.toDataObject());
        if (subset.numAttributeNameValues() >= atts.numAttributes()) {
          err.setError(Error.NO_ERROR);
        }
        else {
          err.setError(Error.INCOMPLETE_DATA_ERROR);
        }
      }
    }
    v.addElement(err.toDataObject());
    return result;
  }

  /**
   * This method stores the attribute name values, both in persistent storage
   * and in local storage.
   *
   * @param atts AttributeNameValues to store
   */
  public void storeAttributeNameValues(AttributeNameValues atts) {
    store(atts);
    long timestamp = new Long((String)atts.getAttributeNameValue(TIMESTAMP).getValue()).longValue();
    for (int i=0; i<atts.numAttributeNameValues(); i++) {
      AttributeNameValue attNew = atts.getAttributeNameValueAt(i);
      String attName = attNew.getName();
      long storedTime = ((Long)attributesTimes.get(attName)).longValue();
      if (storedTime <= timestamp) {
        AttributeNameValue attOld = attributesCache.getAttributeNameValue(attName);
        attOld.setValue(attNew.getValue());
      }
    }
  }
}
