package context.arch.comm;

import context.arch.comm.protocol.InvalidProtocolException;
import context.arch.comm.protocol.ProtocolException;
import context.arch.comm.protocol.RequestData;
import context.arch.comm.protocol.HTTPServerSocket;
import context.arch.comm.protocol.HTTPClientSocket;
import context.arch.comm.language.MessageHandler;
import context.arch.comm.language.DecodeException;
import context.arch.comm.language.EncodeException;
import context.arch.comm.language.InvalidDecoderException;
import context.arch.comm.language.InvalidEncoderException;
import context.arch.util.Constants;
import context.arch.MethodException;
import context.arch.InvalidMethodException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.Socket;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.Vector;

import java.io.FileReader;
import java.io.BufferedReader;

/**
 * This class handles the network communications for the calling class.
 *
 * @see context.arch.comm.language.MessageHandler
 */
public class CommunicationsObject {

  /**
   * Debug flag. Set to true to see debug messages.
   */
  private static final boolean DEBUG = false;

  /**
   * The default port number to use is port 5555.
   */
  public static final int DEFAULT_PORT = 5555;

  /**
   * The default remote port number to use is port 5555.
   */
  public static final int DEFAULT_REMOTE_PORT = 5555;

  /**
   * The default remote server is localhost.
   */
  public static final String DEFAULT_REMOTE_SERVER = "127.0.0.1";

  /**
   * The default server protocol class to use is "context.arch.comm.protocol.HTTPServerSocket".
   */
  public static final String DEFAULT_SERVER = "context.arch.comm.protocol.HTTPServerSocket";

  /**
   * The default client protocol class to use is "context.arch.comm.protocol.HTTPClientSocket".
   */
  public static final String DEFAULT_CLIENT = "context.arch.comm.protocol.HTTPClientSocket";

  /**
   * The HTTP server protocol class.
   */
  public static final String HTTP_SERVER = "context.arch.comm.protocol.HTTPServerSocket";
  
  /**
   * The HTTP client protocol class.
   */
  public static final String HTTP_CLIENT = "context.arch.comm.protocol.HTTPClientSocket";
  
  private MessageHandler handler;
  private int serverPort;
  private String serverClass;
  private CommunicationsServer server;
  private String clientClass;

  /** 
   * Basic constructor for CommunicationsObject using given
   * port and protocol server class, and client class.  If either
   * class is null, the default class is used, and if the port < 0,
   * the default port is used.
   *
   * @param handler Basic message handling object
   * @param clientClass Class to use for client communications
   * @param serverClass Class to use for server communications
   * @param serverPort Port the server recieves communications on
   * @see #DEFAULT_CLIENT
   * @see #DEFAULT_PORT
   * @see #DEFAULT_SERVER
   */

  public CommunicationsObject(MessageHandler handler, String clientClass, String serverClass, int serverPort) {
    this.handler = handler;
    if (clientClass == null) {
      this.clientClass = DEFAULT_CLIENT;
    }
    else {
      this.clientClass = clientClass;
    }
    if (serverClass == null) {
      this.serverClass = DEFAULT_SERVER;
    }
    else {
      this.serverClass = serverClass;
    }
    if (serverPort < 0) { 
    //I think that if serverPort is less than 0, then there is 
    //no any necessarity to start a local server 
      this.serverPort = DEFAULT_PORT;
    }
    else {
      this.serverPort = serverPort;
    }
  }

  /** 
   * Basic constructor for CommunicationsObject using default 
   * port and protocol server class, and default client class, 
   *
   * @param handler Basic message handling object
   * @see #DEFAULT_PORT
   * @see #DEFAULT_SERVER
   * @see #DEFAULT_CLIENT
   */
  public CommunicationsObject(MessageHandler handler) { 
    this(handler,DEFAULT_CLIENT,DEFAULT_SERVER,DEFAULT_PORT);
  }

  public int getPort(){
	  return this.serverPort;
  }
  /** 
   * Constructor for CommunicationsObject using given 
   * port. It uses the default server and client class.
   *
   * @param handler Basic message handling object
   * @param port Port number to communicate on
   * @see #DEFAULT_SERVER
   * @see #DEFAULT_CLIENT
   */
  public CommunicationsObject(MessageHandler handler, int port) { 
    this(handler,DEFAULT_CLIENT,DEFAULT_SERVER,port);
  }

  /** 
   * Constructor for CommunicationsObject using default 
   * port, default client class and given protocol server.  
   *
   * @param handler Basic message handling object
   * @param serverClass Name of server class to use for communications 
   * @see #DEFAULT_PORT
   * @see #DEFAULT_CLIENT
   */
  public CommunicationsObject(MessageHandler handler, String serverClass) {
    this(handler,DEFAULT_CLIENT,serverClass,DEFAULT_PORT);  
  }

  /** 
   * Constructor for CommunicationsObject using the given 
   * port and given protocol.  It uses the port for both the 
   * server object and for the client object.  The default client
   * class and default remote hostname is used.
   *
   * @param handler Basic message handling object
   * @param port Port number to communicate on
   * @param serverClass Class to use for server communications
   * @see #DEFAULT_CLIENT
   */
  public CommunicationsObject(MessageHandler handler, int port, String serverClass) { 
    this(handler,DEFAULT_CLIENT,serverClass,port);  
  }

  /**
   * This method creates and starts an instance of the class that deals with 
   * the underlying communications protocol being used.  This new class implements 
   * the CommunicationsServer interface.
   * Now it also creates an instance of the client class.
   *
   * @exception context.arch.comm.protocol.InvalidProtocolException if the
   * class implementing the communications protocol can't be created
   * @see context.arch.comm.CommunicationsServer
   * @see context.arch.comm.CommunicationsServer#start()
   */
  public void start() throws InvalidProtocolException {
    if (System.getProperty("os.name").equals(Constants.WINCE)) {
      server = (CommunicationsServer) new HTTPServerSocket(this,new Integer(serverPort));
    }
    else {
    //	System.getProperty("os.name") = "WINDOWS XP";
      try {
        Class[] classes = new Class[2];
        classes[0] = Class.forName("context.arch.comm.CommunicationsObject");
        classes[1] = Class.forName("java.lang.Integer");
        Constructor constructor = Class.forName(serverClass).getConstructor(classes);  
        Object[] objects = new Object[2];
        objects[0] = this;
        objects[1] = new Integer(serverPort);
        server = (CommunicationsServer)constructor.newInstance(objects);
      } catch (NoSuchMethodException nsme) {
          System.out.println("CommunicationsObject NoSuchMethod: "+nsme);
          throw new InvalidProtocolException(serverClass);
      } catch (InvocationTargetException ite) {
          System.out.println("CommunicationsObject InvocationTarget: "+ite);
          throw new InvalidProtocolException(serverClass);
      } catch (IllegalAccessException iae) {
          System.out.println("CommunicationsObject IllegalAccess: "+iae);
          throw new InvalidProtocolException(serverClass);
      } catch (InstantiationException ie) {
          System.out.println("CommunicationsObject Instantiation: "+ie);
          throw new InvalidProtocolException(serverClass);
      } catch (ClassNotFoundException cnfe) {
          System.out.println("CommunicationsObject ClassNotFound: "+cnfe);
          throw new InvalidProtocolException(serverClass);
      }
    }
    server.start();
  }

  /**
   * This stub method calls the decodeData method in MessageHandler. The end result
   * is a decoded message.
   *
   * @param message Message to be decoded
   * @return decoded message in a DataObject
   * @exception context.arch.comm.language.DecodeException if the message can't be decoded
   * @exception context.arch.comm.language.InvalidDecoderException if a decoder can't be created
   * @see context.arch.comm.language.MessageHandler#decodeData(Reader)
   */
  public DataObject decodeData(Reader message) throws DecodeException, InvalidDecoderException {
    return handler.decodeData(message);
  }

  /**
   * This stub method calls the encodeData method in MessageHandler. The end result
   * is a encoded message.
   *
   * @param message Message to be encoded in the form of a DataObject
   * @return encoded message in the form of a DataObject
   * @exception context.arch.comm.language.EncodeException if the message can't be encoded
   * @exception context.arch.comm.language.InvalidEncoderException if the encoder can't be created
   * @see context.arch.comm.language.MessageHandler#encodeData(DataObject)
   */
  public String encodeData(DataObject message) throws EncodeException, InvalidEncoderException {
    return handler.encodeData(message);
  }

  /**
   * This method gets the communications protocol being used by the 
   * object that implements the CommunicationsServer interface
   *
   * @return the communications protocol being used
   * @see context.arch.comm.CommunicationsServer#getProtocol()
   */
  public String getServerProtocol() {
    return server.getProtocol();
  }

  /**
   * This stub method adds the communications protocol to the given reply using
   * the CommunicationsServer object
   *
   * @param reply The reply that needs the protocol added
   * @return the reply with added protocol
   * @exception context.arch.comm.protocol.ProtocolException if protocol can't 
   *		be added to the given reply
   * @see context.arch.comm.CommunicationsServer#addReplyProtocol(String)
   */
  public String addReplyProtocol(String reply) throws ProtocolException {
    return server.addReplyProtocol(reply);
  }

  /**
   * This stub method strips the communications protocol from the given request using
   * the CommunicationsServer object
   *
   * @param socket The socket the request is being received on
   * @return the request with the protocol stripped away
   * @exception context.arch.comm.protocol.ProtocolException thrown if protocol can't 
   *		be stripped from the given request
   * @see context.arch.comm.CommunicationsServer#stripRequestProtocol(java.net.Socket)
   */
  public RequestData stripRequestProtocol(Socket socket) throws ProtocolException {
    return server.stripRequestProtocol(socket);
  }

  /**
   * This private class creates and instantiates a CommunicationsClient object with 
   * the given hostname and port.
   *
   * @param host Hostname the client should be connecting to
   * @param port Port number the client should be connecting to
   * @return CommunicationsClient just created
   * @exception context.arch.comm.protocol.InvalidProtocolException if CommunicationsClient
   *		can't be instantiated 
   */
  private CommunicationsClient createCommunicationsClient(String host, int port) throws InvalidProtocolException {
    CommunicationsClient client = null;
    if (System.getProperty("os.name").equals(Constants.WINCE)) {
      client = (CommunicationsClient)new HTTPClientSocket(this,host,new Integer(port));
    }
    else {
     try {
        Class[] classes = new Class[3];
        classes[0] = Class.forName("context.arch.comm.CommunicationsObject");
        classes[1] = Class.forName("java.lang.String");
        classes[2] = Class.forName("java.lang.Integer");
        Constructor constructor = Class.forName(clientClass).getConstructor(classes);  
        Object[] objects = new Object[3];
        objects[0] = this;
        objects[1] = host;
        objects[2] = new Integer(port);
        client = (CommunicationsClient)constructor.newInstance(objects);
      } catch (NoSuchMethodException nsme) {
          System.out.println("CommunicationsObject NoSuchMethod: "+nsme);
          throw new InvalidProtocolException(clientClass);
      } catch (InvocationTargetException ite) {
          System.out.println("CommunicationsObject InvocationTarget: "+ite);
          throw new InvalidProtocolException(clientClass);
      } catch (IllegalAccessException iae) {
          System.out.println("CommunicationsObject IllegalAccess: "+iae);
          throw new InvalidProtocolException(clientClass);
      } catch (InstantiationException ie) {
          System.out.println("CommunicationsObject Instantiation: "+ie);
          throw new InvalidProtocolException(clientClass);
      } catch (ClassNotFoundException cnfe) {
          System.out.println("CommunicationsObject ClassNotFound: "+cnfe);
          throw new InvalidProtocolException(clientClass);
      }
    }
    return client;
  }

  /**
   * This method sends the given request using the CommunicationsClient object.
   * It does so by adding the request protocol to the given request and request type, sending
   * the request, gets the reply, strips the protocol from the reply and returns it.
   *
   * @param client CommunicationsClient object to use to send the given request
   * @param request The request to send
   * @param url The request type
   * @return the result of the request
   * @see context.arch.comm.CommunicationsClient#addRequestProtocol(String, String)
   * @see context.arch.comm.CommunicationsClient#sendRequest(String)
   * @see context.arch.comm.CommunicationsClient#stripRequestProtocol(String, String)
   * @exception context.arch.comm.protocol.ProtocolException thrown when request fails due to mistake in protocol
   */
  private RequestData sendRequest(CommunicationsClient client, String request, String url) throws ProtocolException {
    String fullRequest = null;
    RequestData reply = null;
    Socket s;
    try {
   	fullRequest = client.addRequestProtocol (request, url);
    } catch (Exception e) {
	  System.out.println ("While addRequestProtocol in sendRequest: " + e);
    }

    s = client.sendRequest(fullRequest); 
    reply = client.stripReplyProtocol(s);
    return reply;
  }

  /**
   * This method sends the given request using the CommunicationsClient object.
   * It does so by adding the request protocol to the given request and request type, sending
   * the request, gets the reply, strips the protocol from the reply and returns it.
   *
   * @param client CommunicationsClient object to use to send the given request
   * @param request The request to send
   * @param url The request type
   * @param type Whether the request is a GET or a POST
   * @return the result of the request
   * @see context.arch.comm.CommunicationsClient#addRequestProtocol(String, String,String)
   * @see context.arch.comm.CommunicationsClient#sendRequest(String)
   * @see context.arch.comm.CommunicationsClient#stripRequestProtocol(String, String)
   * @exception context.arch.comm.protocol.ProtocolException thrown when request fails due to mistake in protocol
   */
  private RequestData sendRequest(CommunicationsClient client, String request, String url, String type) throws ProtocolException {
    String fullRequest = null;
    RequestData reply = null;
    try {
      fullRequest = client.addRequestProtocol(request, url, type);
    } catch(Exception e) {
        System.out.println("While addRequestProtocol in sendRequest: " + e);
    }
    Socket s = client.sendRequest(fullRequest);
    reply = client.stripReplyProtocol(s);
    return reply;
  }

  /**
   * This method creates a communications client with the default hostname and port
   * and sends the given request using the newly created CommunicationsClient object
   *
   * @param request The request to send
   * @param url The request type
   * @return the result of the request
   * @exception context.arch.comm.protocol.InvalidProtocolException if request can't be sent successfully
   * @exception context.arch.comm.protocol.ProtocolException thrown when request fails due to mistake in protocol
   */
  public RequestData sendRequest(String request, String url) throws InvalidProtocolException, ProtocolException {
    return sendRequest(request,url,DEFAULT_REMOTE_SERVER,DEFAULT_REMOTE_PORT);
  }

  /**
   * This method creates a communications client with the given hostname and default port
   * and sends the given request using the newly created CommunicationsClient object
   *
   * @param request The request to send
   * @param url The request type
   * @param host Hostname of the server to connect to
   * @return the result of the request
   * @exception context.arch.comm.protocol.InvalidProtocolException if request can't be sent successfully
   * @exception context.arch.comm.protocol.ProtocolException thrown when request fails due to mistake in protocol
   */
  public RequestData sendRequest(String request, String url, String host) throws InvalidProtocolException, ProtocolException {
    return sendRequest(request, url, host, DEFAULT_REMOTE_PORT);
  }

  /**
   * This method creates a communications client with the given hostname and port
   * and sends the given request using the newly created CommunicationsClient object
   *
   * @param request The request to send
   * @param url The request type
   * @param host Hostname of the server to connect to
   * @param port Port number of the server to connect to
   * @return the result of the request
   * @exception context.arch.comm.protocol.InvalidProtocolException if request can't be sent successfully
   * @exception context.arch.comm.protocol.ProtocolException thrown when request fails due to mistake in protocol
   */
  public RequestData sendRequest(String request, String url, String host, int port) throws InvalidProtocolException, ProtocolException {
    CommunicationsClient client = createCommunicationsClient(host, port);
    return sendRequest (client, request, url);
  }

  /**
   * This method creates a communications client with the given hostname and port
   * and sends the given request using the newly created CommunicationsClient object
   *
   * @param request The request to send
   * @param url The request type
   * @param host Hostname of the server to connect to
   * @param port Port number of the server to connect to
   * @param type Whether the request is a GET or a POST
   * @return the result of the request
   * @exception context.arch.comm.protocol.InvalidProtocolException if request can't be sent successfully
   * @exception context.arch.comm.protocol.ProtocolException thrown when request fails due to mistake in protocol
   */
  public RequestData sendRequest(String request, String url, String host, int port, String type) throws InvalidProtocolException, ProtocolException {
    CommunicationsClient client = createCommunicationsClient(host, port);
    return sendRequest (client, request, url, type);
  }

  /**
   * This stub method stops the communications server from receiving more data by
   * using the CommunicationsServer object 
   *
   * @see context.arch.comm.CommunicationsServer#quit()
   */
  public void quit() {
    server.quit();
  }

  /**
   * This stub method runs the specified request using the MessageHandler
   *
   * @param line Single line specifying the type of request
   * @param data Data for the specified RPC
   * @exception context.arch.InvalidMethodException thrown if specified RPC can't be found
   * @exception context.arch.MethodException thrown if specified RPC can't be successfully executed
   * @see context.arch.comm.language.MessageHandler#runMethod(String, DataObject)
   */
  public DataObject runMethod(String line, DataObject data) throws InvalidMethodException, MethodException {
	//2008/7/5: CommunicationObject should handle with the "GET" request
	  if(data == null){ //is a "GET" request
		  StringBuffer buff = new StringBuffer();
		  try{
			  BufferedReader bs = new BufferedReader(new FileReader(line));			  
			  while((line = bs.readLine())!=null){
				  buff.append(line + "\n");
			  }			  
		  }catch(Exception e){
			  
		  }		  		  
		  return new DataObject("GetReply", buff.toString());
	  }else{
		  return handler.runMethod(line, data);	  
	  }
    
  }

  /**
   * This method handles an incoming request on the given socket and sends 
   * a reply.  It should only be called by the underlying CommunicationsServer
   * object
   *
   * @param socket Socket on which the request is being received
   * @see context.arch.comm.language.MessageHandler#runMethod(String, DataObject)
   */
  public void handleIncomingRequest(Socket socket) {

    String reply = null;
    DataObject results = null;
    String encoded = null;
    try {
      RequestData data = server.stripRequestProtocol(socket);      
      DataObject decoded = null;           
      if (data.getType().equals(RequestData.DECODE)) {
        decoded = decodeData(data.getData());
      }       
      if (DEBUG) {
      	System.out.println("decoded = "+decoded);
      }
//System.out.println(this.getClass().getName()+"(handleIncomingRequest):\r\n" + decoded);      
      results = runMethod(data.getLine(), decoded);
      if (DEBUG) {
        System.out.println("result is "+results);
      }
    } catch (ProtocolException pe) {
        System.out.println("CommunicationsObject handleIncomingRequest Protocol: "+pe);
        results = server.getErrorMessage();
    } catch (DecodeException de) {
        System.out.println("CommunicationsObject handleIncomingRequest Decode: "+de);
        results = server.getErrorMessage();
    } catch (InvalidDecoderException ide) {
        System.out.println("CommunicationsObject handleIncomingRequest InvalidDecoder: "+ide);
        results = server.getErrorMessage();
    } catch (InvalidMethodException ime) {
        System.out.println("CommunicationsObject handleIncomingRequest InvalidMethod: "+ime);
        results = server.getErrorMessage();
    } catch (MethodException me) {
        System.out.println("CommunicationsObject handleIncomingRequest Method: "+me);
        results = server.getErrorMessage();
    }

    try {
      encoded = encodeData(results);
      reply = server.addReplyProtocol(encoded);
      if (DEBUG) {
        System.out.println("reply = "+reply);
      }
    } catch (EncodeException ee) {
        System.out.println("CommunicationsObject handleIncomingRequest Encode: "+ee);
        reply = server.getFatalMessage();
    } catch (InvalidEncoderException iee) {
        System.out.println("CommunicationsObject handleIncomingRequest InvalidEncoder: "+iee);
        reply = server.getFatalMessage();
    } catch (ProtocolException ee) {
        System.out.println("CommunicationsObject handleIncomingRequest Protocol: "+ee);
        reply = server.getFatalMessage();
    }
    try {
      PrintWriter printWriter = new PrintWriter(socket.getOutputStream());
      printWriter.println(reply);
      printWriter.flush();
//System.out.println(this.getClass().getName() + "(reply):\r\n" + reply);      
      if (DEBUG) {
        System.out.println("wrote this reply to socket: " +reply);
      }
    } catch (IOException ioe) {
        System.out.println("CommunicationsObject handleIncomingRequest IO: "+ioe);
        // what do I do here??
    }
  }

  /**
   * Sets the class to use as the communications client.
   *
   * @param client Class to use as the communications client.
   */
  public void setClientClass(String client) {
    this.clientClass = client;
  }

  /**
   * Returns the class being used as the communications client.
   *
   * @return being used as the communications client.
   */
  public String getClientClass() {
    return clientClass;
  }

  /**
   * Sets the class to use as the communications server.
   *
   * @param server Class to use as the communications server.
   */
  public void setServerClass(String server) {
    this.serverClass = server;
  }

  /**
   * Returns the class being used as the communications client.
   *
   * @return class being used as the communications server.
   */
  public String getServerClass() {
    return serverClass;
  }

  /**
   * Sets the port to use for incoming communications (server).
   *
   * @param port Port to use for incoming communications (server).
   */
  public void setServerPort(int port) {
    this.serverPort = port;
  }

  /**
   * Returns the port being used for incoming communications (server).
   *
   * @return port being used for incoming communications (server).
   */
  public int getServerPort() {
    return serverPort;
  }

}
