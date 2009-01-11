package context.arch.widget;

import context.arch.BaseObject;
import context.arch.comm.DataObject;
import context.arch.comm.language.InvalidDecoderException;
import context.arch.comm.language.DecodeException;
import context.arch.comm.language.InvalidEncoderException;
import context.arch.comm.language.EncodeException;
import context.arch.comm.protocol.ProtocolException;
import context.arch.comm.protocol.InvalidProtocolException;
import context.arch.subscriber.Subscribers;
import context.arch.subscriber.Subscriber;
import context.arch.subscriber.Callbacks;
import context.arch.subscriber.Callback;
import context.arch.service.Services;
import context.arch.service.Service;
import context.arch.service.helper.ServiceInput;
import context.arch.service.helper.FunctionDescriptions;
import context.arch.service.helper.FunctionDescription;
import context.arch.storage.AttributeFunctions;
import context.arch.storage.AttributeFunction;
import context.arch.storage.Attributes;
import context.arch.storage.Attribute;
import context.arch.storage.AttributeNameValues;
import context.arch.storage.AttributeNameValue;
import context.arch.storage.Conditions;
import context.arch.storage.Condition;
import context.arch.storage.Retrieval;
import context.arch.storage.Storage;
import context.arch.storage.StorageObject;
import context.arch.storage.InvalidStorageException;
import context.arch.storage.RetrievalResults;
import context.arch.util.Error;
import context.arch.util.Constants;
import context.arch.util.ContextTypes;

import java.util.Hashtable;
import java.util.Vector;
import java.util.Enumeration;
import java.util.Date;

/**
 * This class is the basic context widget, with attributes and
 * methods that should apply to all context widgets.
 *
 * @see context.arch.BaseObject
 */
public abstract class Widget extends BaseObject {

  /**
   * Debug flag. Set to true to see debug messages.
   */
  private static final boolean DEBUG = false;

  /**
   * Tag for the class file being used by the widget
   */
  public static final String CLASS = "class";

  /**
   * Dummy version number. Subclasses should override this value.
   */
  public String VERSION_NUMBER = "UNDEFINED";

  /**
   * Default port for widgets to use
   */
  public static final int DEFAULT_PORT = 5000;

  /**
   * Tag for version number.
   */
  public static final String VERSION = "version";

  /**
   * Tag for the timestamp of widget data 
   */
  public static final String TIMESTAMP = "timestamp";

  /**
   * Tag to indicate the widget should return the latest stored data
   */
  public static final String QUERY = "query";

  /**
   * Tag to indicate the reply to a QUERY message
   */
  public static final String QUERY_REPLY = "queryReply";

  /**
   * Tag to indicate the widget should get the latest data from the generator and return them
   */
  public static final String UPDATE_AND_QUERY = "updateAndQuery";

  /**
   * Tag to indicate the reply to an UPDATE_AND_QUERY message
   */
  public static final String UPDATE_AND_QUERY_REPLY = "updateAndQueryReply";

  /**
   * Tag to indicate the widget should return its list of attributes
   */
  public static final String QUERY_ATTRIBUTES = "queryAttributes";

  /**
   * Tag to indicate the reply to a QUERY_ATTRIBUTES message
   */
  public static final String QUERY_ATTRIBUTES_REPLY = "queryAttributesReply";

  /**
   * Tag to indicate the widget should return its list of callbacks
   */
  public static final String QUERY_CALLBACKS = "queryCallbacks";

  /**
   * Tag to indicate the reply to a QUERY_CALLBACKS message
   */
  public static final String QUERY_CALLBACKS_REPLY = "queryCallbacksReply";

  /**
   * Tag to indicate the widget should return its list of services
   */
  public static final String QUERY_SERVICES = "queryServices";

  /**
   * Tag to indicate the reply to a QUERY_SERVICES message
   */
  public static final String QUERY_SERVICES_REPLY = "queryServicesReply";

  /**
   * Tag to indicate the widget should return its version number
   */
  public static final String QUERY_VERSION = "queryVersion";

  /**
   * Tag to indicate the reply to a QUERY_VERSION message
   */
  public static final String QUERY_VERSION_REPLY = "queryVersionReply";

  /**
   * Tag to indicate the widget should accept the given data
   */
  public static final String PUT_DATA = "putData";

  /**
   * Tag to indicate the reply to a PUT_DATA message
   */
  public static final String PUT_DATA_REPLY = "putDataReply";

  /**
   * Tag to indicate an update is being sent
   */
  public static final String UPDATE = "update";

  /**
   * Constant for the widget spacer
   */
  public static final String SPACER = Constants.SPACER;

  protected String id;
  protected Attributes attributes;
  protected Hashtable attributeTypes;
  protected Callbacks callbacks;
  protected Services services;
  protected long CurrentOffset = 0;

  /**
   * Object to handle subscriptions to context data
   *
   * @see context.arch.subscriber.Subscribers
   * @see context.arch.subscriber.Subscriber
   */
  public Subscribers subscribers;

  /**
   * Object to keep track of storage
   *
   * @see context.arch.storage.StorageObject
   */
  public StorageObject storage;

  /**
   * Constructor that sets up internal variables for maintaining
   * the list of widget attributes, callbacks, and services and setting up
   * the BaseObject info.
   *
   * @param clientClass Class to use for client communications
   * @param serverClass Class to use for server communications
   * @param serverPort Port to use for server communications
   * @param encoderClass Class to use for communications encoding
   * @param decoderClass Class to use for communications decoding
   * @param storageClass Class to use for storage
   * @param id String to use for widget id and persistent storage
   * @see context.arch.storage.StorageObject
   */
  public Widget(String clientClass, String serverClass, int serverPort, String encoderClass,
                String decoderClass, String storageClass, String id) {
    super(clientClass,serverClass,serverPort,encoderClass,decoderClass);
    try {
      if ((storageClass != null) && (storageClass.equals(Storage.NO_STORAGE))) {
        attributes = setAttributes();
        if (attributes != null) {
          attributeTypes = attributes.toTypesHashtable();
        }
        storage = null;
      }
      else {
        storage = new StorageObject(storageClass,id);
        attributes = setAttributes();
        if (attributes != null) {
          attributeTypes = attributes.toTypesHashtable();
          storage.setAttributes(attributes,attributeTypes);
        }
      }
    } catch (InvalidStorageException ise) {
        System.out.println("Widget InvalidStorageException: "+ise);
    }
    callbacks = setCallbacks();
    subscribers = new Subscribers(this,id);
    services = setServices();
    this.id = id;
    setId(id);
    getNewOffset();
  }    

  /**
   * Constructor that sets up internal variables for maintaining
   * the list of widget attributes, callbacks, and services and setting up
   * the BaseObject info. This version takes a boolean to indicate whether the 
   * default storage class should be used or whether no storage should be 
   * provided.
   *
   * @param clientClass Class to use for client communications
   * @param serverClass Class to use for server communications
   * @param serverPort Port to use for server communications
   * @param encoderClass Class to use for communications encoding
   * @param decoderClass Class to use for communications decoding
   * @param storageFlag Flag to determine whether storage should be used or not
   * @param id String to use for widget id and persistent storage
   * @see context.arch.storage.StorageObject
   */
  public Widget(String clientClass, String serverClass, int serverPort, String encoderClass,
                String decoderClass, boolean storageFlag, String id) {
    super(clientClass,serverClass,serverPort,encoderClass,decoderClass);//Start CommunicationObject, initialize ParserObject
    try {
      if (!storageFlag) {
        attributes = setAttributes(); // A vector keeps (timestamp, username, location)
        if (attributes != null) {
          attributeTypes = attributes.toTypesHashtable(); //A Hashtable keeps name-type pair
        }
        storage = null;
      }
      else {
        storage = new StorageObject(null,id);
        attributes = setAttributes();
        if (attributes != null) {
          attributeTypes = attributes.toTypesHashtable();
          storage.setAttributes(attributes,attributeTypes);
        }
      }
    } catch (InvalidStorageException ise) {
        System.out.println("Widget InvalidStorageException: "+ise);
    }
 
    // A vector keeps name-attributes pair (UPDATE-attributes pair); 
    // for WDisplay widget, there is only services can be polled but no any callbacks can be subscribed to.
    // But for widgets that can get context datas, they provide both callbacks and services.
    callbacks = setCallbacks();  
    subscribers = new Subscribers(this,id);
    services = setServices();
    this.id = id;
    setId(id);
  }    

  /**
   * Constructor that sets up internal variables for maintaining
   * the list of widget attributes, callbacks and services.  It takes a port
   * number as a parameter to indicate which port to listen for
   * messages/connections, the id to use for the widget, and a flag to indicate
   * whether storage functionality should be turned on or off.
   *
   * @param port Port to listen to for incoming messages
   * @param id Widget id
   * @param storageFlag Boolean flag to indicate whether storage should be turned on
   */
  public Widget(int port, String id, boolean storageFlag) {
    this(null,null,port,null,null,storageFlag,id);
  }

  /**
   * Constructor that sets up internal variables for maintaining
   * the list of widget attributes, callbacks and services.  It takes a port
   * number as a parameter to indicate which port to listen for
   * messages/connections.
   *
   * @param port Port to listen to for incoming messages
   * @param id Widget id
   */
  public Widget(int port, String id) {
    this(null,null,port,null,null,null,id);
  }

  /**
   * Constructor that sets up internal variables for maintaining
   * the list of widget attributes, callbacks and services.  It takes 
   * the id to use for the widget, and a flag to indicate
   * whether storage functionality should be turned on or off.
   *
   * @param id Widget id
   * @param storageFlag Boolean flag to indicate whether storage should be turned on
   */
  public Widget(String id, boolean storageFlag) {
    this(null,null,-1,null,null,storageFlag,id);
  }
	
  /**
   * Constructor that sets up internal variables for maintaining
   * the list of widget attributes, callbacks and services.  It takes the 
   * widget id as a parameter
   *
   * @param id ID of the widget
   */
  public Widget(String id) {
    this(null,null,-1,null,null,null,id);
  }

  /**
   * Sets the attributes for the widget
   */
  protected abstract Attributes setAttributes();

  /**
   * Sets the callbacks for the widget
   */
  protected abstract Callbacks setCallbacks();

  /**
   * Sets the services for the widget
   */
  protected abstract Services setServices();
   
  /**
   * Returns the attribute value with the given name
   *
   * @param name Name of the attribute to get
   */   
  protected String getAttributeType(String name) {
    return (String)attributeTypes.get(name);
  }

  /**
   * Sets an attribute
   *
   * @param name Name of the attribute to set
   * @param type Type of the attribute
   */   
  protected void setAttribute(String name, String type) {
    attributeTypes.put(name, type);
    attributes.addAttribute(name,type);
  }

  /**
   * Checks if the given attribute is an attribute of this widget
   *
   * @param name Name of the attribute to check
  */
  protected boolean isAttribute(String name) {
    return attributeTypes.containsKey(name);
  }
	
  /**
   * Removes an attribute
   *
   * @param name Name of the attribute to remove
  protected void removeAttribute(String name) {
    attributes.remove(name);
  }

  /**
   * Returns the callback with the given name
   *
   * @param name Name of the callback to get
  protected String getCallback(String name) {
    return (String)callbacks.get(name);
  }
	
  /**
   * Sets a callback
   *
   * @param name Name of the callback to set
   * @param value Value of the callback
  protected void setCallback(String name, String value) {
    callbacks.put(name, value);
  }
  AKD */ 

  /**
   * Checks if the given callback is an callback of this widget
   *
   * @param name Name of the callback to check
   */
  protected boolean isCallback(String name) {
    // return callbacks.contains(name); -- this was a bug :)  DS, 10/30/1998
    if (callbacks.getCallback(name) == null) {
      return false;
    }
    return true;
  }

  /**
   * Removes a callback
   *
   * @param name Name of the callback to remove
  protected void removeCallback(String name) {
    callbacks.remove(name);
  }
  AKD */

  /**
   * This is an empty method that should be overridden by objects
   * that subclass from this class.  It is intended to be called
   * by generator objects when new data has arrived for the purpose
   * of notifying the widget.
   *
   * @param event Name of the event occurring
   * @param data Object containing the relevant data
   */
  public void notify(String event, Object data) {
  }

  /**
   * This method is called when a remote component sends an UPDATE_AND_QUERY message.
   * It calls the widget's queryGenerator method to get the latest generator info, 
   * and then stores it.
   */
  protected void updateWidgetInformation() {
    AttributeNameValues atts = queryGenerator();
    if (atts != null) {
      if (storage != null) {
        storage.store(atts);
      }
    }  
  }

  /**
   * This abstract method is called when the widget wants to get the latest generator
   * info.
   * 
   * @return AttributeNameValues containing the latest generator information
   */
  protected abstract AttributeNameValues queryGenerator();
 
  /**
   * This is an empty method that should be overridden by objects
   * that subclass from this class.  It is called when another component
   * tries to run a method on the widget, but it's not a query.
   *
   * @param data DataObject containing the data for the method
   * @param error String containing the incoming error value
   * @return DataObject containing the method results
   */
  protected DataObject runWidgetMethod(DataObject data, String error) {
    String name = data.getName();
    Error err = new Error(error);
    if (err.getError() == null) {
      err.setError(Error.UNKNOWN_METHOD_ERROR);
    }
    Vector v = new Vector();
    v.addElement(err.toDataObject());
    return new DataObject(data.getName(),v);
  }

  /**
   * This method is meant to handle any internal methods that the baseObject doesn't
   * handle.  In particular, this method handles the common details for query requests,
   * update and query requests, and version requests that each widget should provide.
   * If the method is not one of these queries, then it calls runWidgetMethod which each widget
   * should provide.
   *
   * @param data DataObject containing the method to run and parameters
   * @return DataObject containing the results of running the method 
   * @see #QUERY
   * @see #QUERY_VERSION
   * @see #UPDATE_AND_QUERY
   */
  public DataObject runUserMethod(DataObject data) {
    DataObject widget = data.getDataObject(ID);
    String error = null;
    
    if (widget == null) {
      error = Error.INVALID_ID_ERROR;
    }
    else {
      String queryId = (String)(widget.getValue().firstElement());
//System.out.println(this.getClass().getName()+"(runUserMethod):\r\nqueryId:"+queryId+"\r\ngetId:"+getId());        
	  if (!queryId.equals(getId())) {
        error = Error.INVALID_ID_ERROR;
      
      }
    }
//System.out.println(this.getClass().getName()+"(runUserMethod):\r\nerror:"+error);   
    String methodType = data.getName();
//System.out.println(this.getClass().getName()+"(runUserMethod):" +
	//	"\r\nMehtodType:"+ methodType);    
    if (methodType.equals(UPDATE_AND_QUERY)) {
      return queryWidget(data,true,error);
    }
    else if (methodType.equals(QUERY)) {
      return queryWidget(data,false,error);
    }
    else if (methodType.equals(QUERY_ATTRIBUTES)) {
      return queryAttributes(data,error);
    }
    else if (methodType.equals(QUERY_CALLBACKS)) {
      return queryCallbacks(data,error);
    }
    else if (methodType.equals(QUERY_SERVICES)) {
      return queryServices(data,error);
    }
    else if (methodType.equals(Subscriber.ADD_SUBSCRIBER)) {
      return addSubscriber(data,error);
    }
    else if (methodType.equals(Subscriber.REMOVE_SUBSCRIBER)) {
      return removeSubscriber(data,error);
    }
    else if (methodType.equals(StorageObject.RETRIEVE_DATA)) {
      return retrieveData(data,error);
    }
    else if (methodType.equals(Widget.PUT_DATA)) {
      return putData(data,error);
    }
    else if (methodType.equals(Service.SERVICE_REQUEST)) {
//System.out.println(this.getClass().getName()+"(runUserMethod):\r\nDataObject:" + data);    	
      return executeService(data,error);
    }
    else {
      return runWidgetMethod(data,error);
    }
  }

  /**
   * This method puts context data in a widget.  It is expected 
   * that widgets will get data from a generator.  But for some
   * widgets, the generator will not use the context toolkit directly,
   * but may use a web CGI script, for example.  For this case, the
   * widget provides this method to collect the data and makes it available
   * to subscribers and for retrieval.
   *
   * @param data DataObject containing the context data to write
   * @param error String containing the incoming error value
   * @return DataObject containing the results of writing the data
   */
  protected DataObject putData(DataObject data, String error) {
    Vector v = new Vector();
    Error err = new Error(error);
    if (err.getError() == null) {
      DataObject callbackObj = data.getDataObject(Subscriber.CALLBACK_NAME);
      String callback = null;
      if (callbackObj != null) {
        callback = (String)callbackObj.getValue().firstElement();
      }
      AttributeNameValues atts = new AttributeNameValues(data);
      if ((atts == null) || (atts.numAttributeNameValues() == 0)) {
        err.setError(Error.INVALID_DATA_ERROR);
      }
      else if (callback != null) {
        Callback call = callbacks.getCallback(callback);
        if (call == null) {
          err.setError(Error.INVALID_CALLBACK_ERROR);
        }
        else {
          Attributes callAtts = call.getAttributes();
          boolean ok = true;
          if (callAtts.numAttributes() == atts.numAttributeNameValues()) {
            for (int i=0; i<callAtts.numAttributes(); i++) {
              Attribute callAtt = callAtts.getAttributeAt(i);
              if (atts.getAttributeNameValue(callAtt.getName()) == null) {
                ok = false;
               }
            }
          }
          if (!ok) {
            err.setError(Error.INVALID_ATTRIBUTE_ERROR);
          }
          else {
            sendToSubscribers(callback,atts);
            store(atts);
            err.setError(Error.NO_ERROR);
          }
        }
      }
      else if (!canHandle(atts)) {
        err.setError(Error.INVALID_ATTRIBUTE_ERROR);
      }
      else {
        store(atts);
        err.setError(Error.NO_ERROR);
      }
    }
    v.addElement(err.toDataObject());
    return new DataObject(PUT_DATA_REPLY, v);
  }

  /**
   * This method queries the callbacks of a widget.
   *
   * @param query DataObject containing the query
   * @param error String containing the incoming error value
   * @return DataObject containing the results of the query
   */
  protected DataObject queryCallbacks(DataObject query, String error) {
    Vector v = new Vector();
    Error err = new Error(error);
    if (err.getError() == null) {
      if (callbacks == null) {
        err.setError(Error.EMPTY_RESULT_ERROR);
      }
      else {
        v.addElement(callbacks.toDataObject());
        err.setError(Error.NO_ERROR);
      }
    }
    v.addElement(err.toDataObject());
    return new DataObject(QUERY_CALLBACKS_REPLY, v);
  }

  /**
   * This method queries the attributes of a widget.
   *
   * @param query DataObject containing the query
   * @param error String containing the incoming error value
   * @return DataObject containing the results of the query
   */
  protected DataObject queryAttributes(DataObject query, String error) {
    Vector v = new Vector();
    Error err = new Error(error);
    if (err.getError() == null) {
      if (attributes == null) {
        err.setError(Error.EMPTY_RESULT_ERROR);
      }
      else {
        err.setError(Error.NO_ERROR);
        v.addElement(attributes.toDataObject());
      }
    }
    v.addElement(err.toDataObject());
    return new DataObject(QUERY_ATTRIBUTES_REPLY, v);
  }

  /**
   * This method queries the services of a widget.
   *
   * @param query DataObject containing the query
   * @param error String containing the incoming error value
   * @return DataObject containing the results of the query
   */
  protected DataObject queryServices(DataObject query, String error) {
    Vector v = new Vector();
    Error err = new Error(error);
    if (err.getError() == null) {
      if (services == null) {
        err.setError(Error.EMPTY_RESULT_ERROR);
      }
      else {
        err.setError(Error.NO_ERROR);
        v.addElement(services.toDataObject());
      }
    }
    v.addElement(err.toDataObject());
    return new DataObject(QUERY_SERVICES_REPLY, v);
  }
     
  /**
   * This method runs a query on a widget, asking for either it's latest
   * acquired data (QUERY) or asking for the widget to acquire and return
   * new data (UPDATE_AND_QUERY)
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
    if (err.getError() != null) {
      v.addElement(err.toDataObject());
      return result;
    }
      
    if (update) {
      updateWidgetInformation();
    }
    if (storage != null) {
      storage.flushStorage();  
    }
    AttributeNameValues values = storage.retrieveLastAttributes();
    
    if (values != null) { 
      AttributeNameValues subset = values.getSubset(atts);
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
    else {
      err.setError(Error.INVALID_DATA_ERROR);
    }
    v.addElement(err.toDataObject());
    return result;
  }
   
  /**
   * This method checks the list of attributes to ensure
   * that the widget contains these attributes.
   * 
   * @param attributes Attributes object containing attributes to check
   * @return whether the list of attributes is valid
   */
  protected boolean canHandle(Attributes attributes) {
    return canHandle(attributes,new Conditions());
  }

  /**
   * This method checks the list of attributes and conditions to ensure
   * that the widget contains these attributes.  
   * 
   * @param atts List of attributes to check
   * @param conds List of Conditions to check
   * @return whether the list of attributes and conditions is valid
   */
  protected boolean canHandle(Attributes atts, Conditions conds) {
    if (atts.numAttributes() > 0) {
      Attribute att = atts.getAttributeAt(0);
      if (!(att.getName().equals(Attributes.ALL))) {
        for (int i=0; i<atts.numAttributes(); i++) {
          att = atts.getAttributeAt(i);
          if (!isAttribute(att.getName())) {
            return false;
          }
        }
      }
    }
    for (int i=0; i<conds.numConditions(); i++) {
      if (!isAttribute(conds.getConditionAt(i).getAttribute())) {
        return false;
      }
    }
    return true;
  }

  /**
   * This method checks the list of attributes and conditions to ensure
   * that the widget contains these attributes.  
   * 
   * @param atts List of attributes to check
   * @param conds List of Conditions to check
   * @return whether the list of attributes and conditions is valid
   */
  protected boolean canHandle(AttributeFunctions atts, Conditions conds) {
    if (atts.numAttributeFunctions() > 0) {
      AttributeFunction att = atts.getAttributeFunctionAt(0);
      if (!(att.getName().equals(Attributes.ALL))) {
        for (int i=0; i<atts.numAttributeFunctions(); i++) {
          att = atts.getAttributeFunctionAt(i);
          if (!isAttribute(att.getName())) {
            return false;
          }
        }
      }
    }
    for (int i=0; i<conds.numConditions(); i++) {
      if (!isAttribute(conds.getConditionAt(i).getAttribute())) {
        return false;
      }
    }
    return true;
  }

  /**
   * This method checks the list of attributes (in an AttributeNameValues object)
   * to ensure that the widget contains these attributes.  
   * 
   * @param atts List of attributes to check
   * @return whether the list of attributes and conditions is valid
   */
  protected boolean canHandle(AttributeNameValues atts) {
    if (atts.numAttributeNameValues() > 0) {
      AttributeNameValue att = atts.getAttributeNameValueAt(0);
      if (!(att.getName().equals(AttributeNameValues.ALL))) {
        for (int i=0; i<atts.numAttributeNameValues(); i++) {
          att = atts.getAttributeNameValueAt(i);
          if (!isAttribute(att.getName())) {
            return false;
          }
        }
      }
    }
    return true;
  }

  /**
   * This method attempts to execute a widget service.
   *
   * @param request DataObject containing the service request
   * @param error String containing the incoming error value
   * @return DataObject containing the results of the service request
   */
  protected DataObject executeService(DataObject request, String error) {
    Vector v = new Vector();
    Error err = new Error(error);
    DataObject result;
    
System.out.println("\r\n" + this.getClass().getName() + "(executeService):\r\n(Request)" + request);
        
    
    if (err.getError() == null) {
      ServiceInput si = new ServiceInput(request);
      
//System.out.println("\r\n" + this.getClass().getName() + "(executeService):\r\n(ServiceName)" + si.toString());
      
      if (si == null) {
        err.setError(Error.MISSING_PARAMETER_ERROR);
      }
      else if (!services.hasService(si.getServiceName())) {
    	  err.setError(Error.UNKNOWN_SERVICE_ERROR);
      }
      else {
        Service service = services.getService(si.getServiceName());//reflection
        FunctionDescriptions fds = service.getFunctionDescriptions();
        if (!fds.hasFunctionDescription(si.getFunctionName())) {
          err.setError(Error.UNKNOWN_FUNCTION_ERROR);
        }
        else {
          String timing = (String)request.getDataObject(FunctionDescription.FUNCTION_TIMING).getValue().firstElement();
          FunctionDescription fd = fds.getFunctionDescription(si.getFunctionName());
          if (!fd.getTiming().equals(timing)) {
            err.setError(Error.INVALID_TIMING_ERROR);
          }
          else {
            result = service.execute(si);
            err.setError(Error.NO_ERROR);
            v.addElement(result);
          }
        }
      }
    }
    v.addElement(err.toDataObject());
    return new DataObject(Service.SERVICE_REQUEST_REPLY,v);
  }
     
  /**
   * This method should be called to send data to subscribers when a context 
   * widget's callback is triggered.  It sends data only to those subscribers
   * that have subscribed to the specified callback.
   * 
   * @param callbackTag Context widget callback that was triggered
   * @param atts AttributeNameValues to send to subscribers
   * @param data DataObject version of atts
   * @see BaseObject#userRequest(DataObject, String, String, int)
   * @see context.arch.subscriber.Subscribers
   */
  protected void sendToSubscribers(String callback, AttributeNameValues atts) {
// this is not thread safe - subscribers could change while accessing it
    for (int i=0; i<subscribers.numSubscribers(); i++) {
      Subscriber sub = subscribers.getSubscriberAt(i);
      DataObject result = null;      // callback reply
      if (callback.equals(sub.getCallback())) {
        if (dataValid(atts, sub.getConditions())) {
          DataObject subid = new DataObject(Subscriber.SUBSCRIBER_ID, sub.getId());
          DataObject callbackTag = new DataObject(Subscriber.CALLBACK_TAG, sub.getTag());
          Vector v = new Vector();
          v.addElement(subid);
          v.addElement(callbackTag);
          v.addElement(atts.getSubset(sub.getAttributes()).toDataObject());
          DataObject send = new DataObject(Subscriber.SUBSCRIPTION_CALLBACK, v);
          String host = sub.getHostName();
          int port = new Integer(sub.getPort()).intValue();
          try {
            result = userRequest(send, Subscriber.SUBSCRIPTION_CALLBACK, host, port);
            sub.resetErrors();
          } catch (EncodeException ee) {
              System.out.println("Widget sendToSubscribers EncodeException: "+ee);
          } catch (DecodeException de) {
              System.out.println("Widget sendToSubscribers DecodeException: "+de);
          } catch (InvalidEncoderException iee) {
              System.out.println("Widget sendToSubscribers InvalidEncoderException: "+iee);
          } catch (InvalidDecoderException ide) {
              System.out.println("Widget sendToSubscribers InvalidDecoderException: "+ide);
          } catch (InvalidProtocolException ipe) {
              System.out.println("Widget sendToSubscribers InvalidProtocolException: "+ipe);
          } catch (ProtocolException pe) {
              System.out.println("Widget sendToSubscribers ProtocolException: "+pe);
              sub.addError();
              if (sub.getErrors() >= Subscriber.MAX_ERRORS) {
                subscribers.removeSubscriber(sub);
              }
          }
          // we pass the result on for processing
          // TBD: pass it on only if it's not an error message?
          try {
            processCallbackReply (result, sub);
          } catch (Exception e) {
              System.out.println ("Widget sendToSubscribers Exception during processCallbackReply: "+e);
          }
        }
      }
    }
  }
  
  /**
   * This private method checks that the given data falls within the given conditions.
   *
   * @param atts AttributeNameValues containing data to validate
   * @param conditions Conditions to validate against
   * @return whether the data falls within the given conditions
   */
  private boolean dataValid(AttributeNameValues atts, Conditions conditions) {
    for (int i=0; i<conditions.numConditions(); i++) {
      Condition condition = conditions.getConditionAt(i);
      String condAtt = condition.getAttribute();
      int condCompare = condition.getCompare();
      Object condValue = condition.getValue();

      if (atts == null) {
        return false;
      }
      AttributeNameValue att = atts.getAttributeNameValue(condAtt);
      String type = att.getType();
      String value = (String)att.getValue();
      
      if (type.equals(Attribute.INT)) {
        if (!longCompare(new Integer(value).longValue(),new Integer(condValue.toString()).longValue(),condCompare)) {
          return false;
        }
      }
      else if (type.equals(Attribute.LONG)) {
        if (!longCompare(new Long(value).longValue(),new Long(condValue.toString()).longValue(),condCompare)) {
          return false;
        }
      }
      else if (type.equals(Attribute.SHORT)) {
        if (!longCompare(new Short(value).longValue(),new Short(condValue.toString()).longValue(),condCompare)) {
          return false;
        }
      }
      else if (type.equals(Attribute.FLOAT)) {
        if (!doubleCompare(new Float(value).doubleValue(),new Float(condValue.toString()).doubleValue(),condCompare)) {
          return false;
        }
      }
      else if (type.equals(Attribute.DOUBLE)) {
        if (!doubleCompare(new Double(value).doubleValue(),new Double(condValue.toString()).doubleValue(),condCompare)) {
          return false;
        }
      }
      else if (type.equals(Attribute.STRING)) {
        if (!stringCompare(value,condValue.toString(),condCompare)) {
          return false;
        }
      }
      else if (type.equals(Attribute.STRUCT)) {
        return false;
      }
    }
    return true;
  }

  /**
   * This private method checks to see whether the given string comparison is true or 
   * not.
   *
   * @param value Actual value of the attribute
   * @param condValue Value to compare against
   * @param compare Type of comparison to do
   * @return whether value falls in condValue and compare values
   */
  private boolean stringCompare(String value, String condValue, int compare) {
    int comparison = value.compareTo(condValue);
    switch(compare) {
      case Storage.EQUAL:              if (comparison == 0) {
                                         return true;
                                       }
                                       break;
      case Storage.LESSTHAN:           if (comparison < 0) {
                                         return true;
                                       }
                                       break;
      case Storage.LESSTHANEQUAL:      if (comparison <= 0) {
                                         return true;
                                       }
                                       break;
      case Storage.GREATERTHAN:        if (comparison > 0) {
                                         return true;
                                       }
                                       break;
      case Storage.GREATERTHANEQUAL:   if (comparison >= 0) {
                                         return true;
                                       }
                                       break;
    }
    return false;
  }
 
  /**
   * This private method checks to see whether the given long comparison is true or 
   * not.
   *
   * @param value Actual value of the attribute
   * @param condValue Value to compare against
   * @param compare Type of comparison to do
   * @return whether value falls in condValue and compare values
   */
  private boolean longCompare(long value, long condValue, int compare) {
    switch(compare) {
      case Storage.EQUAL:              if (value == condValue) {
                                         return true;
                                       }
                                       break;
      case Storage.LESSTHAN:           if (value < condValue) {
                                         return true;
                                       }
                                       break;
      case Storage.LESSTHANEQUAL:      if (value <= condValue) {
                                         return true;
                                       }
                                       break;
      case Storage.GREATERTHAN:        if (value > condValue) {
                                         return true;
                                       }
                                       break;
      case Storage.GREATERTHANEQUAL:   if (value >= condValue) {
                                         return true;
                                       }
                                       break;
    }
    return false;
  }

  /**
   * This private method checks to see whether the given double comparison is true or 
   * not.
   *
   * @param value Actual value of the attribute
   * @param condValue Value to compare against
   * @param compare Type of comparison to do
   * @return whether value falls in condValue and compare values
   */
  private boolean doubleCompare(double value, double condValue, int compare) {
    switch(compare) {
      case Storage.EQUAL:              if (value == condValue) {
                                         return true;
                                       }
                                       break;
      case Storage.LESSTHAN:           if (value < condValue) {
                                         return true;
                                       }
                                       break;
      case Storage.LESSTHANEQUAL:      if (value <= condValue) {
                                         return true;
                                       }
                                       break;
      case Storage.GREATERTHAN:        if (value > condValue) {
                                         return true;
                                       }
                                       break;
      case Storage.GREATERTHANEQUAL:   if (value >= condValue) {
                                         return true;
                                       }
                                       break;
    }
    return false;
  }

  /**
   * This method should be overriden to process the results of subscription callbacks.
   * 
   * @param result DataObject containing the result
   * @param sub Subscriber that returned this reply
   */
  protected void processCallbackReply (DataObject result, Subscriber sub) {
  } 

  /**
   * This method adds a subscriber to this object.  It calls 
   * Subscribers.addSubscriber() if it can add the subscriber.  It returns a 
   * DataObject containing the reply information, including any error information.
   * 
   * @param sub DataObject containing the subscription information
   * @param error String containing the incoming error value
   * @return DataObject with the reply to the subscription request
   * @see context.arch.subscriber.Subscribers#addSubscriber(String,String,int,String,String,Conditions,Attributes)
   */
  protected DataObject addSubscriber(DataObject sub, String error) {
    Vector v = new Vector();
    Error err = new Error(error);
    if (err.getError() == null) {
      Subscriber subscriber = new Subscriber(sub);
      if (subscriber == null) {
        err.setError(Error.MISSING_PARAMETER_ERROR);
      }
      else if (!canHandle(subscriber.getAttributes(),subscriber.getConditions())) {
        err.setError(Error.INVALID_ATTRIBUTE_ERROR);
      }
      else if (!isCallback(subscriber.getCallback())) {
        err.setError(Error.INVALID_CALLBACK_ERROR);
      }
      else {
        subscribers.addSubscriber(subscriber);
        v.addElement(new DataObject(Subscriber.SUBSCRIBER_ID, subscriber.getId()));
        err.setError(Error.NO_ERROR);
      }
    }
    v.addElement(err.toDataObject());
    return new DataObject(Subscriber.SUBSCRIPTION_REPLY, v);
  }

  /**
   * This method removes a subscriber to this object.  It calls 
   * Subscribers.removeSubscriber() if it can remove the subscriber.  It returns a 
   * DataObject containing the reply information, including any error information.
   * 
   * @param sub DataObject containing the subscription information
   * @param error String containing the incoming error value
   * @return DataObject with the reply to the subscription request
   * @see context.arch.subscriber.Subscribers#removeSubscriber(String, String, String, String, String)
   */
  protected DataObject removeSubscriber(DataObject sub, String error) {
    Vector v = new Vector();
    Error err = new Error(error);
    if (err.getError() == null) {
      Subscriber subscriber = new Subscriber(sub);
      if (subscriber == null) {
        err.setError(Error.MISSING_PARAMETER_ERROR);
      }
      else if (!isCallback(subscriber.getCallback())) {
        err.setError(Error.INVALID_CALLBACK_ERROR);
      }
      else {
        boolean done = subscribers.removeSubscriber(subscriber);
        if (!done) {
          err.setError(Error.UNKNOWN_SUBSCRIBER_ERROR);
        }
        else {
          v.addElement(new DataObject(Subscriber.SUBSCRIBER_ID, subscriber.getId()));
          err.setError(Error.NO_ERROR);
        }
      }
    }
    v.addElement(err.toDataObject());
    return new DataObject(Subscriber.SUBSCRIPTION_REPLY, v);
  }

  /**
   * This method retrieves data from the widget's storage.  It returns a 
   * DataObject containing the retrieved data information, including any error information.
   * 
   * @param data DataObject containing the subscription information
   * @param error String containing the incoming error value
   * @return DataObject with the reply to the subscription request
   */
  protected DataObject retrieveData(DataObject data, String error) {
    Vector v = new Vector();
    Error err = new Error(error);
    if (err.getError() == null) {
      Retrieval retrieval = new Retrieval(data);
      if (!canHandle(retrieval.getAttributeFunctions(),retrieval.getConditions())) {
        err.setError(Error.INVALID_ATTRIBUTE_ERROR);
      }
      else {
        if (storage == null) {
          err.setError(Error.EMPTY_RESULT_ERROR);
        }
        else {
          RetrievalResults results = storage.retrieveAttributes(retrieval);
          if (results == null) {
            err.setError(Error.INVALID_REQUEST_ERROR);
          }
          else if (results.size() == 0) {
            err.setError(Error.EMPTY_RESULT_ERROR);
          }
          else {
            err.setError(Error.NO_ERROR);
          }
          if (results != null) {
            v.addElement(results.toDataObject());
          }
        }
      }
    }
    v.addElement(err.toDataObject());
    return new DataObject(StorageObject.RETRIEVE_DATA_REPLY, v);
  }

  /**
   * This stub method stores the data in the given DataObject 
   * 
   * @param data Data to store
   * @see context.arch.storage.StorageObject#store(DataObject)
   */
  protected void store(DataObject data) {
    if (storage != null) {
      storage.store(data);
    }
  }

  /**
   * This stub method stores the data in the given AttributeNameValues object
   * 
   * @param data Data to store
   * @see context.arch.storage.StorageObject#store(AttributeNameValues)
   */
  protected void store(AttributeNameValues data) {
    if (storage != null) {
      storage.store(data);
    }
  }

  /**
   * This method creates a thread that retrieves a global time clock and determines
   * the offset between the local clock and the global clock. It checks this 
   *
   * @return the offset between the global and local clocks
   * @see context.arch.widget.OffsetThread
   */
  protected void getNewOffset() {
    OffsetThread offset = new OffsetThread();
    CurrentOffset = offset.getCurrentOffset();
    offset = new OffsetThread(120);
  }

  /**
   * This method retrieves the offset between the local clock and a global clock
   * with no delay.
   *
   * @return the offset between the global and local clocks
   * @see context.arch.widget.OffsetThread
   */
  protected long getNewOffsetNoDelay() {
    OffsetThread offset = new OffsetThread();
    return offset.getCurrentOffset();
  }

  /**
   * This method returns the current time to use as a timestamp
   *
   * @return the current time, corrected using a global clock offset
   */
  protected Long getCurrentTime() {
    long temp = new Date().getTime();
//    System.out.println("The time plus offset is:  "+(temp + CurrentOffset));
    return new Long(temp + CurrentOffset);
  }
}