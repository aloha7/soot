package context.arch;

import context.arch.comm.CommunicationsObject;
import context.arch.comm.CommunicationsHandler;
import context.arch.comm.protocol.InvalidProtocolException;
import context.arch.comm.language.ParserObject;
import context.arch.comm.language.InvalidEncoderException;
import context.arch.comm.language.InvalidDecoderException;
import context.arch.comm.language.EncodeException;
import context.arch.comm.language.DecodeException;
import context.arch.comm.language.MessageHandler;
import context.arch.comm.DataObject;
import context.arch.handler.Handlers;
import context.arch.handler.Handler;
import context.arch.handler.HandlerInfo;
import context.arch.handler.AsyncServiceHandlers;
import context.arch.handler.AsyncServiceHandler;
import context.arch.handler.AsyncServiceHandlerInfo;
import context.arch.comm.protocol.ProtocolException;
import context.arch.comm.protocol.RequestData;
import context.arch.service.Service;
import context.arch.service.helper.ServiceInput;
import context.arch.service.helper.FunctionDescription;
import context.arch.storage.StorageObject;
import context.arch.storage.Retrieval;
import context.arch.storage.Conditions;
import context.arch.storage.Condition;
import context.arch.storage.AttributeFunctions;
import context.arch.storage.Attributes;
import context.arch.storage.Attribute;
import context.arch.storage.AttributeNameValues;
import context.arch.storage.AttributeNameValue;
import context.arch.subscriber.Subscriber;
import context.arch.util.Error;
import context.arch.util.Constants;

import context.arch.comm.language.ParserObject;
	
import java.io.Reader;
import java.util.Enumeration;
import java.util.Vector;
import java.util.Hashtable;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * This class is the base object for the context-aware infrastructure.
 * It is able to poll and subscribe to other components and can be
 * polled and subscribed to by other components.  It also can generate
 * and handle RPC-style requests.  It consists of 2 main objects, the 
 * CommunicationsObject and ParserObject.  It also maintains a list of 
 * subscribers and a list of handlers.
 *
 * @see context.arch.comm.CommunicationsObject
 * @see context.arch.comm.language.ParserObject
 * @see context.arch.handler.Handler
 */
public class BaseObject implements MessageHandler, CommunicationsHandler {

  /**
   * Debug flag. Set to true to see debug messages.
   */
  private static final boolean DEBUG = false;

  /**
   * Indicates that the exit condition is normal
   */
  public static final int EXIT_OK = 1;

  /**
   * Tag for id of this component
   */
  public static final String ID = Constants.ID;

  /**
   * Tag for a ping
   */
  public static final String PING = "ping";

  /**
   * Tag for a ping reply
   */
  public static final String PING_REPLY = "pingReply";

  /**
   * Object to handle communications between components
   *
   * @see context.arch.comm.CommunicationsObject
   */
  public CommunicationsObject communications;

  /**
   * Object to handle the encoding and decoding of communications
   *
   * @see context.arch.comm.language.ParserObject
   */
  public ParserObject parser;

  /**
   * Object to keep track of context widget handlers
   *
   * @see context.arch.handler.Handlers
   * @see context.arch.handler.Handler
   */
  public Handlers handlers;

  /**
   * Object to keep track of asynchronous service handlers
   *
   * @see context.arch.handler.AsyncServiceHandlers
   * @see context.arch.handler.AsyncServiceHandler
   */
  public AsyncServiceHandlers serviceHandlers;

  private String host = null;
  
  private String id;
  private String version = "undefined";

  /**
   * Basic constructor that creates a CommunicationsObject
   * with the given port and protocol, and creates a 
   * ParserObject with the given encoder and decoder.  It also
   * creates a Handlers object to keep track of context widget handlers.
   *
   * @param clientClass Class to use for client communications
   * @param serverClass Class to use for server communications
   * @param serverPort Port to use for server communications
   * @param encoderClass Class to use for communications encoding
   * @param decoderClass Class to use for communications decoding
   * @see context.arch.comm.CommunicationsObject
   * @see context.arch.comm.CommunicationsObject#start()
   * @see context.arch.comm.language.ParserObject
   * @see context.arch.handler.Handlers
   */
  public BaseObject(String clientClass, String serverClass, int serverPort, String encoderClass,
                    String decoderClass) {
    try {
      communications = new CommunicationsObject(this,clientClass,serverClass,serverPort);
System.out.println(this.getClass().getName() + ":\r\nstart a server");

//I think that if serverPort is less than 0, then there is 
//no any necessarity to start a local server. No, it still needs a server to receive asynchronous context update.  
      communications.start();
      parser = new ParserObject(encoderClass,decoderClass);
      handlers = new Handlers();
      serviceHandlers = new AsyncServiceHandlers();
    } catch (InvalidProtocolException ipe) {
        System.out.println("BaseObject InvalidProtocolException: "+ipe);
    }
  }

  /**
   * Basic constructor that creates a CommunicationsObject,
   * ParserObject and Handlers object.
   *
   * @see context.arch.comm.CommunicationsObject
   * @see context.arch.comm.CommunicationsObject#start()
   * @see context.arch.comm.language.ParserObject
   * @see context.arch.handler.Handlers
   */
  public BaseObject() {
    this(null,null,-1,null,null);
  }

  /**
   * Constructor that just creates a CommunicationsObject 
   * with the given port and ParserObject.
   * It also creates a Handlers 
   * object to keep track of context widget handlers.
   *
   * @param port Port number to communicate on
   *
   * @see context.arch.comm.CommunicationsObject
   * @see context.arch.comm.CommunicationsObject#start()
   * @see context.arch.comm.language.ParserObject
   * @see context.arch.handler.Handlers
   */
  public BaseObject(int port) {
    this(null,null,port,null,null);
  }

  /**
   * Constructor that just creates a CommunicationsObject
   * with the given protocol handler class, and
   * a ParserObject.  It also creates a Handlers object to keep track
   * of context widget handlers.
   *
   * @param protocol Protocol handler class to communicate with
   *
   * @see context.arch.comm.CommunicationsObject
   * @see context.arch.comm.CommunicationsObject#start()
   * @see context.arch.comm.language.ParserObject
   * @see context.arch.handler.Handlers
   */
  public BaseObject(String protocol) {
    this(null,protocol,-1,null,null);
  }

  /**
   * Constructor that just creates a CommunicationsObject
   * with the given port and protocol handler class, and 
   * ParserObject.  It also creates a Handlers object to keep track 
   * of context widget handlers.
   *
   * @param port Port number to communicate on
   * @param protocol Protocol handler class name to communicate with
   *
   * @see context.arch.comm.CommunicationsObject
   * @see context.arch.comm.CommunicationsObject#start()
   * @see context.arch.comm.language.ParserObject
   * @see context.arch.handler.Handlers
   */
  public BaseObject(int port, String protocol) {
    this(null,protocol,port,null,null);
  }
  
  /**
   * Stub method that decodes the given string using ParserObject
   *
   * @param commData String to be decoded
   * @return the decoded data
   * @exception context.arch.comm.language.DecodeException thrown if the parser can't decode the given string
   * @exception context.arch.comm.language.InvalidDecoderException thrown if the parser can't create the necessary decoder
   * @see context.arch.comm.language.ParserObject#decodeData(java.io.Reader)
   */
  public DataObject decodeData(Reader commData) throws DecodeException, InvalidDecoderException { 
    return parser.decodeData(commData);
  }

  /**
   * Stub method that encodes the given string using ParserObject
   *
   * @param commData String to be decoded
   * @return the encoded data
   * @exception context.arch.comm.language.EncodeException thrown if the parser can't encode the given string
   * @exception context.arch.comm.language.InvalidEncoderException thrown if the parser can't create the necessary encoder
   * @see context.arch.comm.language.ParserObject#encodeData(context.arch.comm.DataObject)
   */
  public String encodeData(DataObject commData) throws EncodeException, InvalidEncoderException {
    return parser.encodeData(commData);
  }

  /**
   * Method that submits a user request for polling/subscription.  The request
   * is in the form of a DataObject.  It is encoded, sent out and the reply is
   * decoded, if necessary, and returned.  
   *
   * @param data DataObject that contains the request
   * @param url RPC tag that indicates the type of request
   * @return DataObject containing the reply to the request
   * @exception EncodeException when the encoding can't be completed successfully
   * @exception DecodeException when the decoding can't be completed successfully
   * @exception InvalidEncoderException when the encoder can't be created
   * @exception InvalidDecoderException when the decoder can't be created
   * @exception ProtocolException when the request can't be sent successfully
   * @exception InvalidProtocolException when the request can't be sent successfully due to invalid protocol use
   */
  public DataObject userRequest(DataObject data, String url) throws EncodeException, InvalidProtocolException, ProtocolException, DecodeException, InvalidDecoderException, InvalidEncoderException {
    DataObject decoded = null;
    
    String encoded = encodeData(data);
    RequestData replydata = communications.sendRequest(encoded, url);
    if (replydata.getType().equals(RequestData.DECODE)) {
      decoded = parser.decodeData(replydata.getData());
    }
    return decoded;
  }

  /**
   * Method that submits a user request for polling/subscription.  The request
   * is in the form of a DataObject.  It is encoded, sent out and the reply is
   * decoded, if necessary, and returned.  
   *
   * @param data DataObject that contains the request
   * @param url RPC tag that indicates the type of request
   * @param server Hostname of the component the request is being sent to 
   * @return DataObject containing the reply to the request
   * @exception EncodeException when the encoding can't be completed successfully
   * @exception DecodeException when the decoding can't be completed successfully
   * @exception InvalidEncoderException when the encoder can't be created
   * @exception InvalidDecoderException when the decoder can't be created
   * @exception ProtocolException when the request can't be sent successfully
   * @exception InvalidProtocolException when the request can't be sent successfully due to invalid protocol use
   */
  public DataObject userRequest(DataObject data, String url, String server) throws EncodeException, InvalidProtocolException, ProtocolException, DecodeException, InvalidDecoderException, InvalidEncoderException {
    DataObject decoded = null;
    
    String encoded = encodeData(data);
    RequestData replydata = communications.sendRequest(encoded, url, server);
    if (replydata.getType().equals(RequestData.DECODE)) {
      decoded = parser.decodeData(replydata.getData());
    }
    return decoded;
  }

  /**
   * Method that submits a user request for polling/subscription.  The request
   * is in the form of a DataObject.  It is encoded, sent out and the reply is
   * decoded, if necessary, and returned.  
   *
   * @param data DataObject that contains the request
   * @param url RPC tag that indicates the type of request
   * @param server Hostname of the component the request is being sent to 
   * @param port Port number of the component the request is being sent to 
   * @return DataObject containing the reply to the request
   * @exception EncodeException when the encoding can't be completed successfully
   * @exception DecodeException when the decoding can't be completed successfully
   * @exception InvalidEncoderException when the encoder can't be created
   * @exception InvalidDecoderException when the decoder can't be created
   * @exception ProtocolException when the request can't be sent successfully
   * @exception InvalidProtocolException when the request can't be sent successfully due to invalid protocol use
   */
  public DataObject userRequest(DataObject data, String url, String server, int port) throws EncodeException, ProtocolException, InvalidProtocolException, DecodeException, InvalidDecoderException, InvalidEncoderException {
    DataObject decoded = null;
    
    String encoded = encodeData(data);
//System.out.println(this.getClass().getName() + "(send):\r\n" + encoded);    
    RequestData replydata = communications.sendRequest(encoded, url, server, port);
    if (replydata.getType().equals(RequestData.DECODE)) {
      decoded = parser.decodeData(replydata.getData());      
    }
//System.out.println(this.getClass().getName() + "(receive):\r\n" + encodeData(decoded));    
    return decoded;
  }

  /**
   * This method allows a component to subscribe to changes in other components.
   * The subscription includes the handler that will handle the callbacks,
   * the subscriber's hostname and port, the subscription id, the remote component's
   * hostname, port, and id, the remote component's callback, and the name of the
   * subscriber's method that will handle the callback.
   *
   * @param handler Object that handles context widget callbacks
   * @param port Port number the subscriber receives communications on
   * @param subId Subscription id
   * @param remoteHost Hostname of the context widget being subscribed to
   * @param remotePort Port number the context widget receives communication on
   * @param remoteId Id of the context widget being subscribed to
   * @param callback Callback defined by the context widget
   * @param url Name of the subscriber method that handles the callback being subscribed to
   * @return Error to indicate success of subscription
   * @see context.arch.handler.Handlers
   * @see context.arch.handler.Handlers#addHandler(context.arch.handler.HandlerInfo)
   * @see #userRequest(context.arch.comm.DataObject, String, String, int)
   */
  public Error subscribeTo(Handler handler, int port, String subid, String remoteHost, 
                 int remotePort, String remoteId, String callback, String url) {
    return subscribeTo(handler,remoteId,remoteHost,remotePort, 
                       new Subscriber(subid,getHostAddress(),port,callback,url,new Conditions(),new Attributes()));
  }

  /**
   * This method allows a component to subscribe to changes in other components.
   * The subscription includes the handler that will handle the callbacks,
   * the subscriber's hostname and port, the subscription id, the remote component's
   * hostname, port, and id, the remote component's callback, and the name of the
   * subscriber's method that will handle the callback.
   *
   * @param handler Object that handles context widget callbacks
   * @param port Port number the subscriber receives communications on
   * @param subId Subscription id
   * @param remoteHost Hostname of the context widget being subscribed to
   * @param remotePort Port number the context widget receives communication on
   * @param remoteId Id of the context widget being subscribed to
   * @param callback Callback defined by the context widget
   * @param url Name of the subscriber method that handles the callback being subscribed to
   * @param conditions Any conditions to put on the subscription
   * @return Error to indicate success of subscription
   * @see context.arch.handler.Handlers
   * @see context.arch.handler.Handlers#addHandler(context.arch.handler.HandlerInfo)
   * @see #userRequest(context.arch.comm.DataObject, String, String, int)
   */
  public Error subscribeTo(Handler handler, int port, String subid, String remoteHost, 
                 int remotePort, String remoteId, String callback, String url, Conditions conditions) {
    return subscribeTo(handler,remoteId,remoteHost,remotePort, 
                       new Subscriber(subid,getHostAddress(),port,callback,url,conditions,new Attributes()));
  }

  /**
   * This method allows a component to subscribe to changes in other components.
   * The subscription includes the handler that will handle the callbacks,
   * the subscriber's hostname and port, the subscription id, the remote component's
   * hostname, port, and id, the remote component's callback, and the name of the
   * subscriber's method that will handle the callback.
   *
   * @param handler Object that handles context widget callbacks
   * @param port Port number the subscriber receives communications on
   * @param subId Subscription id
   * @param remoteHost Hostname of the context widget being subscribed to
   * @param remotePort Port number the context widget receives communication on
   * @param remoteId Id of the context widget being subscribed to
   * @param callback Callback defined by the context widget
   * @param url Name of the subscriber method that handles the callback being subscribed to
   * @param attributes Attributes to return to the subscriber
   * @return Error to indicate success of subscription
   * @see context.arch.handler.Handlers
   * @see context.arch.handler.Handlers#addHandler(context.arch.handler.HandlerInfo)
   * @see #userRequest(context.arch.comm.DataObject, String, String, int)
   */
  public Error subscribeTo(Handler handler, int port, String subid, String remoteHost, 
                 int remotePort, String remoteId, String callback, String url, Attributes attributes) {
    return subscribeTo(handler,remoteId,remoteHost,remotePort, 
                       new Subscriber(subid,getHostAddress(),port,callback,url,new Conditions(),attributes));
  }

  /**
   * This method allows a component to subscribe to changes in other components.
   * The subscription includes the handler that will handle the callbacks,
   * the subscriber's hostname and port, the subscription id, the remote component's
   * hostname, port, and id, the remote component's callback, and the name of the
   * subscriber's method that will handle the callback.
   *
   * @param handler Object that handles context widget callbacks
   * @param port Port number the subscriber receives communications on
   * @param subId Subscription id
   * @param remoteHost Hostname of the context widget being subscribed to
   * @param remotePort Port number the context widget receives communication on
   * @param remoteId Id of the context widget being subscribed to
   * @param callback Callback defined by the context widget
   * @param url Name of the subscriber method that handles the callback being subscribed to
   * @param conditions Any conditions to put on the subscription
   * @param attributes Attributes to return to the subscriber
   * @return Error to indicate success of subscription
   * @see context.arch.handler.Handlers
   * @see context.arch.handler.Handlers#addHandler(context.arch.handler.HandlerInfo)
   * @see #userRequest(context.arch.comm.DataObject, String, String, int)
   */
  public Error subscribeTo(Handler handler, int port, String subid, String remoteHost, 
                 int remotePort, String remoteId, String callback, String url, Conditions conditions, Attributes attributes) {
    return subscribeTo(handler,remoteId,remoteHost,remotePort, 
                       new Subscriber(subid,getHostAddress(),port,callback,url,conditions,attributes));
  }

  /**
   * This method allows a component to subscribe to changes in other components.
   * The subscription includes the handler that will handle the callbacks,
   * the subscriber's hostname and port, the subscription id, the remote component's
   * hostname, port, and id, the remote component's callback, and the name of the
   * subscriber's method that will handle the callback.
   *
   * @param handler Object that handles context widget callbacks
   * @param remoteId Id of the context widget being subscribed to
   * @param remoteHost Hostname of the widget being subscribed to
   * @param remotePort Port number of the widget being subscribed to
   * @param subscriber Subscriber object holding the subscription info
   * @return Error to indicate success of subscription
   * @see context.arch.handler.Handlers
   * @see context.arch.handler.Handlers#addHandler(context.arch.handler.HandlerInfo)
   * @see #userRequest(context.arch.comm.DataObject, String, String, int)
   */
  private Error subscribeTo(Handler handler, String remoteId, String remoteHost, int remotePort, Subscriber subscriber) {
    Vector v = new Vector();
    v.addElement(new DataObject(ID, remoteId));
    v.addElement(subscriber.toDataObject());
    DataObject sub = new DataObject(Subscriber.ADD_SUBSCRIBER, v);
    try {
      DataObject result = userRequest(sub, Subscriber.ADD_SUBSCRIBER, remoteHost, remotePort);
      Error error = new Error(result);
      if (error.getError().equals(Error.NO_ERROR)) {
        handlers.addHandler(new HandlerInfo(handler, subscriber.getId(), remoteId, subscriber.getCallback(), subscriber.getTag()));
      }
      return error;
    } catch (EncodeException ee) {
        System.out.println("BaseObject subscribeTo EncodeException: "+ee);
    } catch (DecodeException de) {
        System.out.println("BaseObject subscribeTo DecodeException: "+de);
    } catch (InvalidEncoderException iee) {
        System.out.println("BaseObject subscribeTo InvalidEncoderException: "+iee);
    } catch (InvalidDecoderException ide) {
        System.out.println("BaseObject subscribeTo InvalidDecoderException: "+ide);
    } catch (InvalidProtocolException ipe) {
        System.out.println("BaseObject subscribeTo InvalidProtocolException: "+ipe);
    } catch (ProtocolException pe) {
        System.out.println("BaseObject subscribeTo ProtocolException: "+pe);
    }
    return null;
  }

  /**
   * This method allows a component to unsubscribe from another component.
   *
   * @param handler Object that handles context widget callbacks
   * @param remoteHost Hostname of the widget being unsubscribed from
   * @param remotePort Port number of the widget being unsubscribed from
   * @param remoteId Id of the context widget being unsubscribed from
   * @param subscriber Subscriber object holding the subscription info
   * @return Error to indicate success of unsubscription
   * @see context.arch.handler.Handlers
   * @see context.arch.handler.Handlers#removeHandler(context.arch.handler.HandlerInfo)
   * @see #userRequest(context.arch.comm.DataObject, String, String, int)
   */
  public Error unsubscribeFrom(Handler handler, String remoteHost, int remotePort, String remoteId, Subscriber subscriber) {
    Vector v = new Vector();
    v.addElement(new DataObject(ID, remoteId));
    v.addElement(subscriber.toDataObject());
    DataObject sub = new DataObject(Subscriber.REMOVE_SUBSCRIBER, v);
    
    try {
      DataObject result = userRequest(sub, Subscriber.REMOVE_SUBSCRIBER, remoteHost, remotePort);
      Error error = new Error(result);
      if (error.getError().equals(Error.NO_ERROR)) {
        handlers.removeHandler(new HandlerInfo(handler, subscriber.getId(), remoteId, subscriber.getCallback(), subscriber.getTag()));
      }
      return error;
    } catch (EncodeException ee) {
        System.out.println("BaseObject subscribeTo EncodeException: "+ee);
    } catch (DecodeException de) {
        System.out.println("BaseObject subscribeTo DecodeException: "+de);
    } catch (InvalidEncoderException iee) {
        System.out.println("BaseObject subscribeTo InvalidEncoderException: "+iee);
    } catch (InvalidDecoderException ide) {
        System.out.println("BaseObject subscribeTo InvalidDecoderException: "+ide);
    } catch (InvalidProtocolException ipe) {
        System.out.println("BaseObject subscribeTo InvalidProtocolException: "+ipe);
    } catch (ProtocolException pe) {
        System.out.println("BaseObject subscribeTo ProtocolException: "+pe);
    }
    return null;
  }

  /**
   * This method allows a component to poll a remote widget for its attribute
   * values.  A list of attributes is provided to dictate which attribute 
   * values are wanted.
   *
   * @param widgetHost Hostname of the context widget being polled
   * @param widgetPort Port number of the context widget being polled
   * @param widgetId Id of the context widget being polled
   * @param attributes Attributes being requested
   * @return DataObject containing results of poll
   * @see #userRequest(context.arch.comm.DataObject, String, String, int)
   */
  public DataObject pollWidget(String widgetHost, int widgetPort, String widgetId, Attributes attributes) {
    Vector v = new Vector();
    v.addElement(new DataObject(ID, widgetId));
    v.addElement(attributes.toDataObject());
    DataObject poll = new DataObject(Constants.QUERY, v);

    try {
      return userRequest(poll, Constants.QUERY, widgetHost, widgetPort);
    } catch (EncodeException ee) {
        System.out.println("BaseObject pollWidget EncodeException: "+ee);
    } catch (DecodeException de) {
        System.out.println("BaseObject pollWidget DecodeException: "+de);
    } catch (InvalidEncoderException iee) {
        System.out.println("BaseObject pollWidget InvalidEncoderException: "+iee);
    } catch (InvalidDecoderException ide) {
        System.out.println("BaseObject pollWidget InvalidDecoderException: "+ide);
    } catch (InvalidProtocolException ipe) {
        System.out.println("BaseObject pollWidget InvalidProtocolException: "+ipe);
    } catch (ProtocolException pe) {
        System.out.println("BaseObject pollWidget ProtocolException: "+pe);
    }
    return null;
  }

  /**
   * This method allows a component to force a remote widget to update its data
   * and return it.  A list of attributes is provided to dictate which attribute 
   * values are wanted.
   *
   * @param widgetHost Hostname of the context widget being polled
   * @param widgetPort Port number of the context widget being polled
   * @param widgetId Id of the context widget being polled
   * @param attributes Attributes being requested
   * @return DataObject containing results of poll
   * @see #userRequest(context.arch.comm.DataObject, String, String, int)
   */
  public DataObject updateAndPollWidget(String widgetHost, int widgetPort, String widgetId, Attributes attributes) {
    Vector v = new Vector();
    v.addElement(new DataObject(ID, widgetId));
    v.addElement(attributes.toDataObject());
    DataObject poll = new DataObject(Constants.UPDATE_AND_QUERY, v);

    try {
      return userRequest(poll, Constants.UPDATE_AND_QUERY, widgetHost, widgetPort);
    } catch (EncodeException ee) {
        System.out.println("BaseObject pollWidget EncodeException: "+ee);
    } catch (DecodeException de) {
        System.out.println("BaseObject pollWidget DecodeException: "+de);
    } catch (InvalidEncoderException iee) {
        System.out.println("BaseObject pollWidget InvalidEncoderException: "+iee);
    } catch (InvalidDecoderException ide) {
        System.out.println("BaseObject pollWidget InvalidDecoderException: "+ide);
    } catch (InvalidProtocolException ipe) {
        System.out.println("BaseObject pollWidget InvalidProtocolException: "+ipe);
    } catch (ProtocolException pe) {
        System.out.println("BaseObject pollWidget ProtocolException: "+pe);
    }
    return null;
  }

  /**
   * This method allows a component to put context data in a remote 
   * widget.  It is really intended for components that don't use
   * the context toolkit, but is being included here for possible future
   * use.  The method takes a callback and an AttributeNameValues object.
   * The callback is not necessary (can have a null value), and is only 
   * used by the remote widget to determine which of the widget's subscribers 
   * need to be updated.
   *
   * @param widgetHost Hostname of the context widget to use
   * @param widgetPort Port number of the context widget to use 
   * @param widgetId Id of the context widget to use
   * @param callback Callback of the context widget to associate the data with
   * @param attributes AttributeNameValues to put in the widget
   * @return DataObject containing results of put data
   * @see #userRequest(context.arch.comm.DataObject, String, String, int)
   */
  public DataObject putDataInWidget(String widgetHost, int widgetPort, String widgetId, 
                                    String callback, AttributeNameValues attributes) {
    Vector v = new Vector();
    v.addElement(new DataObject(ID, widgetId));
    if (callback != null) {
      v.addElement(new DataObject(Subscriber.CALLBACK_NAME, callback));
    }
    v.addElement(attributes.toDataObject());
    DataObject put = new DataObject(Constants.PUT_DATA, v);

    try {
      return userRequest(put, Constants.PUT_DATA, widgetHost, widgetPort);
    } catch (EncodeException ee) {
        System.out.println("BaseObject putDataInWidget EncodeException: "+ee);
    } catch (DecodeException de) {
        System.out.println("BaseObject putDataInWidget DecodeException: "+de);
    } catch (InvalidEncoderException iee) {
        System.out.println("BaseObject putDataInWidget InvalidEncoderException: "+iee);
    } catch (InvalidDecoderException ide) {
        System.out.println("BaseObject putDataInWidget InvalidDecoderException: "+ide);
    } catch (InvalidProtocolException ipe) {
        System.out.println("BaseObject putDataInWidget InvalidProtocolException: "+ipe);
    } catch (ProtocolException pe) {
        System.out.println("BaseObject putDataInWidget ProtocolException: "+pe);
    }
    return null;
  }

  /**
   * This method gets the version of the given component (server, widget, interpreter).
   *
   * @param remoteHost Hostname of the component being queried
   * @param remotePort Port number of the component being queried
   * @param remoteId Id of the component being queried
   * @return DataObject containing version
   * @see #userRequest(context.arch.comm.DataObject, String, String, int)
   */
  public DataObject getVersion(String remoteHost, int remotePort, String remoteId) {
    Vector v = new Vector();
    v.addElement(new DataObject(ID, remoteId));
    DataObject query = new DataObject(Constants.QUERY_VERSION, v);

    try {
      return userRequest(query, Constants.QUERY_VERSION, remoteHost, remotePort);
    } catch (EncodeException ee) {
        System.out.println("BaseObject getVersion EncodeException: "+ee);
    } catch (DecodeException de) {
        System.out.println("BaseObject getVersion DecodeException: "+de);
    } catch (InvalidEncoderException iee) {
        System.out.println("BaseObject getVersion InvalidEncoderException: "+iee);
    } catch (InvalidDecoderException ide) {
        System.out.println("BaseObject getVersion InvalidDecoderException: "+ide);
    } catch (InvalidProtocolException ipe) {
        System.out.println("BaseObject getVersion InvalidProtocolException: "+ipe);
    } catch (ProtocolException pe) {
        System.out.println("BaseObject getVersion ProtocolException: "+pe);
    }
    return null;
  }

  /**
   * This method gets the callbacks of the given widget
   *
   * @param widgetHost Hostname of the widget being queried
   * @param widgetPort Port number of the widget being queried
   * @param widgetId Id of the widget being queried
   * @return DataObject containing callbacks
   * @see #userRequest(context.arch.comm.DataObject, String, String, int)
   */
  public DataObject getWidgetCallbacks(String widgetHost, int widgetPort, String widgetId) {
    Vector v = new Vector();
    v.addElement(new DataObject(ID,widgetId));
    DataObject query = new DataObject(Constants.QUERY_CALLBACKS, v);
    try {
      return userRequest(query, Constants.QUERY_CALLBACKS, widgetHost, widgetPort);      
    } catch (EncodeException ee) {
        System.out.println("BaseObject getWidgetCallbacks EncodeException: "+ee);
    } catch (DecodeException de) {
        System.out.println("BaseObject getWidgetCallbacks DecodeException: "+de);
    } catch (InvalidEncoderException iee) {
        System.out.println("BaseObject getWidgetCallbacks InvalidEncoderException: "+iee);
    } catch (InvalidDecoderException ide) {
        System.out.println("BaseObject getWidgetCallbacks InvalidDecoderException: "+ide);
    } catch (InvalidProtocolException ipe) {
        System.out.println("BaseObject getWidgetCallbacks InvalidProtocolException: "+ipe);
    } catch (ProtocolException pe) {
        System.out.println("BaseObject getWidgetCallbacks ProtocolException: "+pe);
    }
    return null;
  }

  /**
   * This method gets the services of the given widget
   *
   * @param widgetHost Hostname of the widget being queried
   * @param widgetPort Port number of the widget being queried
   * @param widgetId Id of the widget being queried
   * @return DataObject containing services of the widget
   * @see #userRequest(context.arch.comm.DataObject, String, String, int)
   */
  public DataObject getWidgetServices(String widgetHost, int widgetPort, String widgetId) {
    Vector v = new Vector();
    v.addElement(new DataObject(ID,widgetId));
    DataObject query = new DataObject(Constants.QUERY_SERVICES, v);
    try {
      return userRequest(query, Constants.QUERY_SERVICES, widgetHost, widgetPort);
    } catch (EncodeException ee) {
        System.out.println("BaseObject getWidgetServices EncodeException: "+ee);
    } catch (DecodeException de) {
        System.out.println("BaseObject getWidgetServices DecodeException: "+de);
    } catch (InvalidEncoderException iee) {
        System.out.println("BaseObject getWidgetServices InvalidEncoderException: "+iee);
    } catch (InvalidDecoderException ide) {
        System.out.println("BaseObject getWidgetServices InvalidDecoderException: "+ide);
    } catch (InvalidProtocolException ipe) {
        System.out.println("BaseObject getWidgetServices InvalidProtocolException: "+ipe);
    } catch (ProtocolException pe) {
        System.out.println("BaseObject getWidgetServices ProtocolException: "+pe);
    }
    return null;
  }

  /**
   * This method gets the attributes of the given widget
   *
   * @param widgetHost Hostname of the widget being queried
   * @param widgetPort Port number of the widget being queried
   * @param widgetId Id of the widget being queried
   * @return DataObject containing callbacks
   * @see #userRequest(context.arch.comm.DataObject, String, String, int)
   */
  public DataObject getWidgetAttributes(String widgetHost, int widgetPort, String widgetId) {
    Vector v = new Vector();
    v.addElement(new DataObject(ID,widgetId));
    DataObject query = new DataObject(Constants.QUERY_ATTRIBUTES, v);
    try {
      return userRequest(query, Constants.QUERY_ATTRIBUTES, widgetHost, widgetPort);
    } catch (EncodeException ee) {
        System.out.println("BaseObject getWidgetCallbacks EncodeException: "+ee);
    } catch (DecodeException de) {
        System.out.println("BaseObject getWidgetCallbacks DecodeException: "+de);
    } catch (InvalidEncoderException iee) {
        System.out.println("BaseObject getWidgetCallbacks InvalidEncoderException: "+iee);
    } catch (InvalidDecoderException ide) {
        System.out.println("BaseObject getWidgetCallbacks InvalidDecoderException: "+ide);
    } catch (InvalidProtocolException ipe) {
        System.out.println("BaseObject getWidgetCallbacks InvalidProtocolException: "+ipe);
    } catch (ProtocolException pe) {
        System.out.println("BaseObject getWidgetCallbacks ProtocolException: "+pe);
    }
    return null;
  }

  /**
   * This method allows a component to retrieve data from other components.
   *
   * @param remoteHost Hostname of the component being asked for data
   * @param remotePort Port number of the component being asked for data
   * @param remoteId Id of the component being asked for data
   * @param retrieval Description of data to retrieve with any conditions
   * @return DataObject containing data requested
   * @see #userRequest(context.arch.comm.DataObject, String, String, int)
   */
  public DataObject retrieveDataFrom(String remoteHost, int remotePort, String remoteId, Retrieval retrieval) {
    Vector v = new Vector();
    v.addElement(new DataObject(ID, remoteId));
    v.addElement(retrieval.toDataObject());
    DataObject retrieve = new DataObject(StorageObject.RETRIEVE_DATA, v);

    try {
      return userRequest(retrieve, StorageObject.RETRIEVE_DATA, remoteHost, remotePort);      
    } catch (EncodeException ee) {
        System.out.println("BaseObject retrieveDataFrom EncodeException: "+ee);
    } catch (DecodeException de) {
        System.out.println("BaseObject retrieveDataFrom DecodeException: "+de);
    } catch (InvalidEncoderException iee) {
        System.out.println("BaseObject retrieveDataFrom InvalidEncoderException: "+iee);
    } catch (InvalidDecoderException ide) {
        System.out.println("BaseObject retrieveDataFrom InvalidDecoderException: "+ide);
    } catch (InvalidProtocolException ipe) {
        System.out.println("BaseObject retrieveDataFrom InvalidProtocolException: "+ipe);
    } catch (ProtocolException pe) {
        System.out.println("BaseObject retrieveDataFrom ProtocolException: "+pe);
    }
    return null;
  }

  /**
   * This method allows a component to retrieve data from other components.
   *
   * @param remoteHost Hostname of the component being asked for data
   * @param remotePort Port number of the component being asked for data
   * @param remoteId Id of the component being asked for data
   * @param myId Id of the "user" trying to access the data
   * @param retrieval Description of data to retrieve with any conditions
   * @return DataObject containing data requested
   * @see #userRequest(context.arch.comm.DataObject, String, String, int)
   */
  public DataObject retrieveDataFrom(String remoteHost, int remotePort, String remoteId, String myId, Retrieval retrieval) {
    Vector v = new Vector();
    v.addElement(new DataObject(ID, remoteId));
    v.addElement(new DataObject("requestorId", myId));
    v.addElement(retrieval.toDataObject());
    DataObject retrieve = new DataObject(StorageObject.RETRIEVE_DATA, v);

    try {
      return userRequest(retrieve, StorageObject.RETRIEVE_DATA, remoteHost, remotePort);      
    } catch (EncodeException ee) {
        System.out.println("BaseObject retrieveDataFrom EncodeException: "+ee);
    } catch (DecodeException de) {
        System.out.println("BaseObject retrieveDataFrom DecodeException: "+de);
    } catch (InvalidEncoderException iee) {
        System.out.println("BaseObject retrieveDataFrom InvalidEncoderException: "+iee);
    } catch (InvalidDecoderException ide) {
        System.out.println("BaseObject retrieveDataFrom InvalidDecoderException: "+ide);
    } catch (InvalidProtocolException ipe) {
        System.out.println("BaseObject retrieveDataFrom InvalidProtocolException: "+ipe);
    } catch (ProtocolException pe) {
        System.out.println("BaseObject retrieveDataFrom ProtocolException: "+pe);
    }
    return null;
  }


  /**
   * This method returns a vector containing AttributeNameValues objects for all the 
   * the data of a given attribute.  
   * e.g. SQL query would be "SELECT attribute FROM table"
   * 
   * @param remoteHost Hostname of the component being asked for data
   * @param remotePort Port number of the component being asked for data
   * @param remoteId Id of the component being asked for data
   * @param attribute Attribute to retrieve
   * @return DataObject containing data requested
   */
  public DataObject retrieveDataFrom(String remoteHost, int remotePort, String remoteId, String attribute) {
    AttributeFunctions attributes = new AttributeFunctions();
    attributes.addAttributeFunction(attribute);
    return retrieveDataFrom(remoteHost,remotePort,remoteId,attributes);
  }

  /**
   * This method returns a vector containing AttributeNameValues objects for all the 
   * the data of the given attributes.
   * e.g. SQL query would be "SELECT attribute1,attribute2,...,attributeN FROM table"
   * 
   * @param remoteHost Hostname of the component being asked for data
   * @param remotePort Port number of the component being asked for data
   * @param remoteId Id of the component being asked for data
   * @param attributes Names of the attributes to retrieve data for
   * @return DataObject containing data requested
   */
  public DataObject retrieveDataFrom(String remoteHost, int remotePort, String remoteId, AttributeFunctions attributes) {
    return retrieveDataFrom(remoteHost,remotePort,remoteId,new Retrieval(attributes, new Conditions()));    
  }

  /**
   * This method returns a vector containing AttributeNameValues objects for all the 
   * the data of the given attributes and a single condition.
   * e.g. SQL query would be "SELECT attribute1,attribute2,...,attributeN FROM table
   *      WHERE attributeX > valueY"
   * 
   * @param remoteHost Hostname of the component being asked for data
   * @param remotePort Port number of the component being asked for data
   * @param remoteId Id of the component being asked for data
   * @param attributes Names of the attributes to retrieve data for
   * @param attribute Name of the attribute to do conditional on
   * @param compare Comparison flag
   * @param value Comparison value to use
   * @return DataObject containing data requested
   */
  public DataObject retrieveDataFrom(String remoteHost, int remotePort, String remoteId,
                                     AttributeFunctions attributes, String attribute, int compare, Object value) {
    Conditions conditions = new Conditions();
    conditions.addCondition(attribute,compare,value);
    return retrieveDataFrom(remoteHost,remotePort,remoteId,new Retrieval(attributes, conditions));
  }

  /**
   * This method returns a vector containing AttributeNameValues objects for all the 
   * the data.
   * e.g. SQL query would be "SELECT * FROM table
   * 
   * @param remoteHost Hostname of the component being asked for data
   * @param remotePort Port number of the component being asked for data
   * @param remoteId Id of the component being asked for data
   * @return DataObject containing data requested
   */
  public DataObject retrieveDataFrom(String remoteHost, int remotePort, String remoteId) {
    return retrieveDataFrom(remoteHost,remotePort,remoteId,Attributes.ALL);
  }

  /**
   * This method returns a vector containing AttributeNameValues objects for all the 
   * the data and a single condition.
   * e.g. SQL query would be "SELECT * FROM table WHERE attributeX > valueY
   * 
   * @param remoteHost Hostname of the component being asked for data
   * @param remotePort Port number of the component being asked for data
   * @param remoteId Id of the component being asked for data
   * @param attribute Name of the attribute to do conditional on
   * @param compare Comparison flag
   * @param value Comparison value to use
   * @return DataObject containing data requested
   */
  public DataObject retrieveDataFrom(String remoteHost, int remotePort, String remoteId,
                                     String attribute, int compare, Object value) {
    AttributeFunctions attributes = new AttributeFunctions();
    attributes.addAttributeFunction(Attributes.ALL);
    Conditions conditions = new Conditions();
    conditions.addCondition(attribute,compare,value);
    return retrieveDataFrom(remoteHost,remotePort,remoteId,new Retrieval(attributes, conditions));
  }

  /**
   * This method asks an interpreter to interpret some data.  It passes the 
   * data to be interpreted and gets back the interpreted data.
   * 
   * @param remoteHost Hostname of the component being asked for data
   * @param remotePort Port number of the component being asked for data
   * @param remoteId Id of the component being asked for data
   * @param data AttributeNameValues object containing the data to be interpreted
   * @return DataObject containing the interpreted data
   */
  public DataObject askInterpreter(String remoteHost, int remotePort, String remoteId,
                                   AttributeNameValues data) {
    Vector v = new Vector();    
    DataObject interpret = new DataObject(Constants.INTERPRET, v);
    v.addElement(new DataObject (ID, remoteId));
    v.addElement(data.toDataObject());
    try {
      return userRequest(interpret, Constants.INTERPRET, remoteHost, remotePort);
    } catch (DecodeException de) {
        System.out.println("BaseObject askInterpreter() Decode: "+de);
    } catch (EncodeException ee) {
        System.out.println("BaseObject askInterpreter() Encode: "+ee);
    } catch (InvalidDecoderException ide) {
        System.out.println("BaseObject askInterpreter() InvalidDecoder: "+ide);
    } catch (InvalidEncoderException iee) {
        System.out.println("BaseObject askInterpreter() InvalidEncoder: "+iee);
    } catch (InvalidProtocolException ipe) {
        System.out.println("BaseObject askInterpreter() InvalidProtocol: "+ipe);
    } catch (ProtocolException pe) {
        System.out.println("BaseObject askInterpreter() Protocol: "+pe);
    }
    return null;
  }

  /**
   * This method asks an component to run some non-standard method.  It passes 
   * id of the component, attributes, and parameters and gets back the result
   * of the method.
   * 
   * @param remoteHost Hostname of the component
   * @param remotePort Port number of the component
   * @param remoteId Id of the component
   * @param methodName Name of the method to run
   * @param parameters AttributeNameValues object that is parameters with values
   * @param attributes Attributes object that is parameters with values
   * @return DataObject containing the interpreted data
   */
  public DataObject runComponentMethod(String remoteHost, int remotePort, String remoteId,
                                   String methodName,AttributeNameValues parameters, Attributes attributes) {
    Vector v = new Vector();
    DataObject method = new DataObject(methodName, v);
    v.addElement(new DataObject (ID, remoteId));
    if (parameters != null) {
      v.addElement(parameters.toDataObject());
    }
    if (attributes != null) {
      v.addElement(attributes.toDataObject());
    }
    try {
      return userRequest(method, methodName, remoteHost, remotePort);
    } catch (DecodeException de) {
        System.out.println("BaseObject runComponentMethod() Decode: "+de);
    } catch (EncodeException ee) {
        System.out.println("BaseObject runComponentMethod() Encode: "+ee);
    } catch (InvalidDecoderException ide) {
        System.out.println("BaseObject runComponentMethod() InvalidDecoder: "+ide);
    } catch (InvalidEncoderException iee) {
        System.out.println("BaseObject runComponentMethod() InvalidEncoder: "+iee);
    } catch (InvalidProtocolException ipe) {
        System.out.println("BaseObject runComponentMethod() InvalidProtocol: "+ipe);
    } catch (ProtocolException pe) {
        System.out.println("BaseObject runComponentMethod() Protocol: "+pe);
    }
    return null;
  }

 
  //2008.7.2: here is an important bug here: id confused with serviceID
  /**
   * This method requests that a widget execute an asynchronous service
   * 
   * @param handler Handler to handle the results of the service
   * @param serviceHost Hostname of the widget with the service
   * @param servicePort Port number of the widget with the service
   * @param serviceId Id of the widget with the service
   * @param service Name of the widget service to run
   * @param function Name of the particular service function to run
   * @param input AttributeNameValues object to use to execute the service
   * @param requestTag Unique tag provided by caller to identify result
   * @return DataObject containing the results of the execution request
   */
  public DataObject executeAsynchronousWidgetService(AsyncServiceHandler handler, String serviceHost, int servicePort, 
                           String serviceId, String service, String function, AttributeNameValues input, String requestTag) {
	  /*Vector v = new Vector();
	    DataObject request = new DataObject(Service.SERVICE_REQUEST, v);
	    v.addElement(new DataObject (ID, serviceId));
	    v.addElement(new DataObject (FunctionDescription.FUNCTION_TIMING, Service.ASYNCHRONOUS));
	    v.addElement(new ServiceInput(serviceId,service,function,input,getHostAddress(),communications.getServerPort(),
	                                  getId(),requestTag).toDataObject());*/
	  
	Vector v = new Vector();
    DataObject request = new DataObject(Service.SERVICE_REQUEST, v);
    
    v.addElement(new DataObject (ID, serviceId));
 
    
    // 2008.7.2: This is a mistake here, it should be id of the invoker rather of provider, since it needs this id to return results asynchronously    
    //v.addElement(new DataObject("InvokerID", handler.getClass().getSimpleName()));
    
    v.addElement(new DataObject (FunctionDescription.FUNCTION_TIMING, Service.ASYNCHRONOUS));
    v.addElement(new ServiceInput(serviceId,service,function,input,getHostAddress(),communications.getServerPort(),
                                  getId(),requestTag).toDataObject());
    try {
      DataObject result = userRequest(request, Service.SERVICE_REQUEST, serviceHost, servicePort);
      Error error = new Error(result);
      if (error.getError().equals(Error.NO_ERROR)) {
        serviceHandlers.addHandler(new AsyncServiceHandlerInfo(handler,getId(),serviceId,service,function,requestTag));
      }
      return result;
    } catch (DecodeException de) {
        System.out.println("BaseObject executeWidgetService() Decode: "+de);
    } catch (EncodeException ee) {
        System.out.println("BaseObject executeWidgetService() Encode: "+ee);
    } catch (InvalidDecoderException ide) {
        System.out.println("BaseObject executeWidgetService() InvalidDecoder: "+ide);
    } catch (InvalidEncoderException iee) {
        System.out.println("BaseObject executeWidgetService() InvalidEncoder: "+iee);
    } catch (InvalidProtocolException ipe) {
        System.out.println("BaseObject executeWidgetService() InvalidProtocol: "+ipe);
    } catch (ProtocolException pe) {
        System.out.println("BaseObject executeWidgetService() Protocol: "+pe);
    }
    return null;
  }

  
  
  /**
   * This method requests that a widget execute a synchronous service
   * 
   * @param remoteHost Hostname of the widget
   * @param remotePort Port number of the widget
   * @param remoteId Id of the widget
   * @param service Name of the widget service to run
   * @param function Name of the particular service function to run
   * @param input AttributeNameValues object to use to execute the service
   * @return DataObject containing the results of the execution request
   */
  public DataObject executeSynchronousWidgetService(String remoteHost, int remotePort, String remoteId,
                                  String service, String function, AttributeNameValues input) {
    Vector v = new Vector();
    DataObject request = new DataObject(Service.SERVICE_REQUEST, v);
    v.addElement(new DataObject (ID, remoteId));
    v.addElement(new DataObject (FunctionDescription.FUNCTION_TIMING, Service.SYNCHRONOUS));
    v.addElement(new ServiceInput(service,function,input).toDataObject());
    try {
      return userRequest(request, Service.SERVICE_REQUEST, remoteHost, remotePort);
    } catch (DecodeException de) {
        System.out.println("BaseObject executeWidgetService() Decode: "+de);
    } catch (EncodeException ee) {
        System.out.println("BaseObject executeWidgetService() Encode: "+ee);
    } catch (InvalidDecoderException ide) {
        System.out.println("BaseObject executeWidgetService() InvalidDecoder: "+ide);
    } catch (InvalidEncoderException iee) {
        System.out.println("BaseObject executeWidgetService() InvalidEncoder: "+iee);
    } catch (InvalidProtocolException ipe) {
        System.out.println("BaseObject executeWidgetService() InvalidProtocol: "+ipe);
    } catch (ProtocolException pe) {
        System.out.println("BaseObject executeWidgetService() Protocol: "+pe);
    }
    return null;
  }

  /**
   * This method pings a component (widget,server, or interpreter)
   * to make sure that it is functioning ok.
   * 
   * @param remoteHost Hostname of the component being asked for data
   * @param remotePort Port number of the component being asked for data
   * @param remoteId Id of the component being asked for data
   * @return DataObject containing the results of the ping
   */
  public DataObject pingComponent(String remoteHost, int remotePort, String remoteId) {
    Vector v = new Vector();
    DataObject ping = new DataObject(PING, v);
    v.addElement(new DataObject (ID, remoteId));
    try {
      return userRequest(ping, PING, remoteHost, remotePort);
    } catch (DecodeException de) {
        System.out.println("BaseObject pingComponent() Decode: "+de);
    } catch (EncodeException ee) {
        System.out.println("BaseObject pingComponent() Encode: "+ee);
    } catch (InvalidDecoderException ide) {
        System.out.println("BaseObject pingComponent() InvalidDecoder: "+ide);
    } catch (InvalidEncoderException iee) {
        System.out.println("BaseObject pingComponent() InvalidEncoder: "+iee);
    } catch (InvalidProtocolException ipe) {
        System.out.println("BaseObject pingComponent() InvalidProtocol: "+ipe);
    } catch (ProtocolException pe) {
        System.out.println("BaseObject pingComponent() Protocol: "+pe);
    }
    return null;
  }

  /**
   * This method should be called when the object is going to exit
   * under normal conditions. It stops the CommunicationsObject from
   * receiving any more requests and exits.
   *
   * @see context.arch.comm.CommunicationsObject#quit()
   */
  public void quit() {
    communications.quit();
    System.exit(EXIT_OK);
  }

  /**
   * This is an empty method that should be overridden by the object that
   * extends this class.  It will contain the user-defined RPCs.
   *
   * @param data DataObject containing data for user-defined RPC
   * @return result of RPC
   * @exception context.arch.InvalidMethodException thrown if specified RPC couldn't be found
   * @exception context.arch.MethodException thrown if specified RPC had an error
   * @see #runMethod(String, DataObject)
   */
  public DataObject runUserMethod(DataObject data) throws InvalidMethodException, MethodException {
    return new DataObject();
  }

  /**
   * This method handles both the system-defined, callbacks and user-defined RPCs.  
   * If a user-defined RPC is called, this method calls runUserMethods.  If a
   * callback is specified, this method runs userCallback.  Currently, the only
   * system-defined methods are queryVersion, and userCallback.
   *
   * @param methodType Name of method to run
   * @param data DataObject containing data for the method call
   * @exception context.arch.InvalidMethodException thrown if specified RPC couldn't be found
   * @exception context.arch.MethodException thrown if specified RPC had an error
   * @see #userCallback(context.arch.comm.DataObject)
   * @see #runUserMethod(context.arch.comm.DataObject)
   * @see #queryVersion(context.arch.comm.DataObject)
   */
  public DataObject runMethod(String methodType, DataObject data) throws InvalidMethodException, MethodException {
//System.out.println(this.getClass().getName() + "(runMethod):\r\n" + "methodType:" + methodType);	  
    if (methodType.equals(Subscriber.SUBSCRIPTION_CALLBACK)) {
      return userCallback(data);
    }
    if (methodType.equals(Constants.QUERY_VERSION)) {
      return queryVersion(data);
    }
    else if (methodType.equals(PING)) {
      return returnPing(data);
    }
    else if (methodType.equals(Service.SERVICE_RESULT)) {
      return serviceResult(data);
    }  
    else {
      return runUserMethod(data);
    }
  }
  
  /** Handle with the "GET" request of HTTP.  
   * @param data: URL to get files 
   * @return desirable files 
   */
  public DataObject getFile(DataObject data){
	  AttributeNameValues atts = new AttributeNameValues(data);
	  AttributeNameValue att = atts.getAttributeNameValueAt(0);
	  
	  return new DataObject();
  }

  /**
   * This method is called when a callback message is received.  It determines which
   * of its registered handlers should receive the callback message and passes it on
   * accordingly.  It creates a reply message to the callback request in the form
   * of a DataObject.  It handles all error checking.
   *
   * @param data DataObject containing the callback request
   * @return DataObject containing the callback reply
   * @see context.arch.handler.Handlers#getHandler(String)
   * @see context.arch.handler.Handler#handle(String, context.arch.comm.DataObject)
   */
  public DataObject userCallback(DataObject data) {
    Vector v = new Vector();
    Error error = new Error();
    String subId = null;
    DataObject subIdObj = data.getDataObject(Subscriber.SUBSCRIBER_ID);
    DataObject callbackObj = data.getDataObject(Subscriber.CALLBACK_TAG);
    DataObject value = data.getDataObject(AttributeNameValues.ATTRIBUTE_NAME_VALUES);
    if ((subIdObj == null) || (callbackObj == null) || (value == null)) {
      if (subIdObj != null) {
        v.addElement(new DataObject(Subscriber.SUBSCRIBER_ID, (String)subIdObj.getValue().firstElement()));
      }
      error.setError(Error.MISSING_PARAMETER_ERROR);
    }
    else {
      subId = (String)subIdObj.getValue().firstElement();
      String callback = (String)callbackObj.getValue().firstElement();
      Handler handler = handlers.getHandler(subId+callback);
      DataObject result = null;	// the result returned by the handler
      
      if (handler != null) {
        try {
          result = handler.handle(callback,value);
        } catch (InvalidMethodException ime) {
            System.out.println("BaseObject userCallback InvalidMethod: "+ime);
            error.setError(Error.UNKNOWN_CALLBACK_ERROR);
            return new DataObject(Subscriber.SUBSCRIPTION_CALLBACK_REPLY, v);
        } catch (MethodException me) {
            System.out.println("BaseObject userCallback Method: "+me);
            error.setError(Error.MISSING_PARAMETER_ERROR);
        }
        if (error.getError() == null) {
          error.setError(Error.NO_ERROR);
          if (result != null) {
            v.addElement(result);
          }
        }
      }
      else {
        error.setError(Error.UNKNOWN_SUBSCRIBER_ERROR);
      }
      v.addElement(new DataObject(Subscriber.SUBSCRIBER_ID,subId));
    }
    v.addElement(error.toDataObject());
    return new DataObject(Subscriber.SUBSCRIPTION_CALLBACK_REPLY,v);
  }

  /**
   * This method returns the version number of this component.
   *
   * @param query DataObject containing the query
   * @return DataObject containing the results of the query
   */
  public DataObject queryVersion(DataObject query) {
    DataObject component = query.getDataObject(ID);
    Error error = new Error();
    
    if (component == null) {
      error.setError(Error.INVALID_ID_ERROR);
    }
    else {
      String queryId = (String)(component.getValue().firstElement());
      if (!queryId.equals(getId())) {
        error.setError(Error.INVALID_ID_ERROR);
      }
    }

    Vector v = new Vector();
    if (error.getError() == null) {
      v.addElement(new DataObject(Constants.VERSION, getVersion()));
      error.setError(Error.NO_ERROR);
    }
    v.addElement(error.toDataObject());
    return new DataObject(Constants.QUERY_VERSION_REPLY, v);
  }

  /**
   * This method returns an error message as an answer to a ping.
   *
   * @param ping DataObject containing the ping request
   * @return DataObject containing the results of the ping
   */
  public DataObject returnPing(DataObject ping) {
    DataObject component = ping.getDataObject(ID);
    Error error = new Error();
    
    if (component == null) {
      error.setError(Error.INVALID_ID_ERROR);
    }
    else {
      String pingId = (String)(component.getValue().firstElement());
      if (!pingId.equals(getId())) {
        error.setError(Error.INVALID_ID_ERROR);
      }
    }

    Vector v = new Vector();
    if (error.getError() == null) {
      error.setError(Error.NO_ERROR);
    }
    v.addElement(error.toDataObject());
    return new DataObject(PING_REPLY, v);
  }

  /**
   * This method handles the results of an asynchronous service request.
   *
   * @param result DataObject containing the results of the aysnchronous service request
   * @return DataObject containing a reply to the results message
   */
  public DataObject serviceResult(DataObject result) {
    DataObject component = result.getDataObject(ID);
    Error error = new Error();
    Vector v = new Vector();
    DataObject result2 = null;

    if (component == null) {
      error.setError(Error.INVALID_ID_ERROR);
    }
    else {
      String resultId = (String)(component.getValue().firstElement());
      if (!resultId.equals(getId())) {
        error.setError(Error.INVALID_ID_ERROR);
      }
      else {
        ServiceInput si = new ServiceInput(result);
        AsyncServiceHandler handler = serviceHandlers.getHandler(resultId+si.getServiceId()+si.getServiceName()+si.getFunctionName()+si.getRequestTag());
        if (handler != null) {
          try {
            result2 = handler.asynchronousServiceHandle(si.getRequestTag(),result);
          } catch (InvalidMethodException ime) {
              System.out.println("BaseObject serviceResult InvalidMethod: "+ime);
              error.setError(Error.INVALID_REQUEST_ERROR); 
          } catch (MethodException me) {
              System.out.println("BaseObject serviceResult Method: "+me);
              error.setError(Error.MISSING_PARAMETER_ERROR);
          }
          if (error.getError() == null) {
            error.setError(Error.NO_ERROR);
            if (result2 != null) {
              v.addElement(result2);
            }
          }
          serviceHandlers.removeHandler(new AsyncServiceHandlerInfo(handler,resultId,si.getServiceId(),
                          si.getServiceName(),si.getFunctionName(),si.getRequestTag()));
        }
        else {
          error.setError(Error.INVALID_REQUEST_ID_ERROR);
        }
      }
    }
    v.addElement(error.toDataObject());
    return new DataObject(Service.SERVICE_RESULT_REPLY, v);
  }

  /**
   * This method returns the version number of this object.
   *
   * @return version number
   */
  public String getVersion() {
    return version;
  }

  /**
   * This method sets the version number of this object.
   *
   * @param version of the object
   */
  public void setVersion(String version) {
    this.version = version;
  }

  /**
   * This method sets the id of classes that subclass this object, for use
   * in sending messages.
   *
   * @param id ID of the class
   */
  public void setId(String id) {
    this.id = id;
  }

  /**
   * This method returns the id of the class that subclass this object, for use
   * in sending messages.
   *
   * @return id of the class
   */
  public String getId() {
    return id;
  }
  /**
   * This method gets the address of the machine this component is running on.
   *
   * @return the address of the machine this component is running on
   */
  public String getHostAddress() {
    if (host != null) {
      return host;
    }
    
    try {
      host = InetAddress.getLocalHost().getHostAddress();      
    } catch (UnknownHostException uhe) {
        System.out.println("BaseObject UnknownHost: "+uhe);
    }
    if (host == null) {
      return new String("127.0.0.1");
    }
    return host;
  }
  
  
  /**
   * Temporary main method to test the BaseObject class
   */
  public static void main(String argv[]) {
     BaseObject server = new BaseObject (4000);
    
    // BaseObject client = new BaseObject (5556);
    
    /*
    <query>
      <widget>
        userLocation
      </widget>
      <data>
        currentLocation
      </data>
    </query>
    

    DataObject widget = new DataObject ("widget", "userLocation");
    DataObject data = new DataObject ("data", "currentLocation");
    Vector v = new Vector();
    v.addElement(widget);
    v.addElement(data);
    DataObject query = new DataObject ("query", v);
    
System.out.println("\n\n\nquery\n"+query);

	ParserObject parser = new ParserObject();
	try{
		String str = parser.encodeData(query);
		System.out.println(str);			
	}catch(Exception e){
		System.out.println(e);
	}
	*/


/*
    try {
      DataObject reply = server.userRequest (query);
System.out.println("\n\n\n"+reply);
    } catch (EncodeException ee) {
        System.out.println("main subscribeTo EncodeException: "+ee);
    } catch (DecodeException de) {
        System.out.println("main subscribeTo DecodeException: "+de);
    } catch (InvalidProtocolException ipe) {
        System.out.println("main subscribeTo InvalidProtocolException: "+ipe);
    }
    
    System.out.println ("Done");
  */ 
  }
}
