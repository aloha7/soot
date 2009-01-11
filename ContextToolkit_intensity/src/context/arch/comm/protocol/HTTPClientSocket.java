package context.arch.comm.protocol;

import java.util.StringTokenizer;
import java.util.Date;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.io.BufferedReader;
import java.io.StringReader;
import java.io.InputStreamReader;
import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import context.arch.comm.CommunicationsClient;
import context.arch.comm.CommunicationsObject;
import context.arch.comm.DataObject;

import context.arch.comm.language.MessageHandler;

/**
 * This class subclasses TCPClientSocket, creating and sending HTTP requests.
 * It implements the CommunicationsClient interface
 *
 * @see context.arch.comm.protocol.TCPClientSocket
 * @see context.arch.comm.CommunicationsClient
 */
public class HTTPClientSocket extends TCPClientSocket implements CommunicationsClient {

  /**
   * Debug flag. Set to true to see debug messages.
   */
  private static final boolean DEBUG = false;

  /**
   * The protocol being used is HTTP 
   */
  public static final String PROTOCOL = "HTTP";

  /**
   * HTTP GET request type 
   */
  public static final String GET = "GET";
  
  /**
   * HTTP POST request type
   */
  public static final String POST = "POST";

  /**
   * Default port to use is 5555
   */
  public static final int DEFAULT_PORT = 5555;

  private CommunicationsObject commObject;

  /**2008.6.30: for testing
   * 
   */
  public HTTPClientSocket(){
	  super(DEFAULT_PORT);
	  portNumber = DEFAULT_PORT;
  }
  
  /** 
   * Basic constructor for HTTPClientSocket that calls TCPClientSocket
   * 
   * @param object Handle of the generic instantiating communications object
   * @see #DEFAULT_PORT
   * @see context.arch.comm.protocol.TCPClientSocket
   */
  public HTTPClientSocket(CommunicationsObject object) { 
    super(DEFAULT_PORT);
    commObject = object;
    portNumber = DEFAULT_PORT;
  }

 
  /** 
   * Constructor for HTTPClientSocket that calls TCPClientSocket with the
   * given port
   *
   * @param object Handle of the generic instantiating communications object
   * @param server Hostname of the remote server to connect to
   * @param port Port to use to receive communications on
   * @see context.arch.comm.protocol.TCPServerSocket
   */
  public HTTPClientSocket(CommunicationsObject object, String server, Integer port) { 
    super(server, port.intValue());
    commObject = object;
  }


  /** 
   * This method adds the HTTP protocol for a POST request
   * (POST is the default)
   * 
   * @param content The request to send
   * @return socket for the connection
   */
   
  public String addRequestProtocol(String data, String url) throws ProtocolException {
    return addRequestProtocol (data, url, POST);
  }
  
  /** 
   * Method that adds the HTTP protocol to a request message
   *
   * @param data Request message to add HTTP protocol to
   * @param url Tag/URL to add to message
   * @return the request with the HTTP protocol added
   * @exception context.arch.comm.protocol.ProtocolException if the protocol
   *		can not be added
   */
  public String addRequestProtocol(String data, String url, String type) throws ProtocolException {
    int xmlLen = data.length();
    String eol = "\r\n";
    StringBuffer text = new StringBuffer();
    String thisMachine;
    try { // get our machine name
      InetAddress thisInet = InetAddress.getLocalHost();
      thisMachine = thisInet.getHostName();
    } catch (UnknownHostException e) {
	    thisMachine = "localhost";
    }
		
    xmlLen += 2*eol.length(); // add length of end of lines at the end
    if (type.equals (POST)) {
      text.append(POST +  " " + url + " HTTP/1.0" + eol);
    } 
    else {
      text.append (GET + " " + url + " HTTP/1.0" + eol);
    }
    text.append("User-Agent: Context Client" + eol);
    text.append("Host: " + thisMachine + eol);
    if (type.equals (POST)) {
      text.append("Content-Type: text/xml" + eol);
      text.append("Content-Length: " + xmlLen + eol);
      text.append(eol);
      text.append(data + eol);
    }
    text.append(eol);
System.out.println(this.getClass().getName()+":" + eol + text);    
    return text.toString();
  }

 
  /** 
   * Method that strips away the HTTP protocol from a reply message
   *
   * @param socket Socket on which reply is coming from
   * @return the reply with the HTTP protocol stripped away
   * @exception context.arch.comm.protocol.ProtocolException if the protocol
   *		can not be stripped away
   */
  public RequestData stripReplyProtocol(Socket data) throws ProtocolException {
  
    BufferedReader bufferedReader = null;
    try {
      bufferedReader = new BufferedReader(new InputStreamReader(data.getInputStream()));
      String get = bufferedReader.readLine();
      StringTokenizer tokenizer = new StringTokenizer(get);
      int bytesRead = 0;
      int bytesReadThisTime = 0;
      char [] tempdata;

      while (!(get.toLowerCase().startsWith("content-length:"))) {
        get = bufferedReader.readLine();
      }
      int length = 0;
      try {
        length = new Integer(get.substring(15).trim()).intValue();
      } catch (NumberFormatException nfe) {
        System.out.println("RequestServerSocket run error: "+nfe);
        throw new ProtocolException();
      }

      if (DEBUG) {
        System.out.println ("Content-Length is: " + length);
      }
					
      while (!(get.trim().equals(""))) {
        get = bufferedReader.readLine();
      }

      char[] postdata = new char[length];
       
      tempdata = new char [length];

      while (bytesRead < length) {
        if (bufferedReader.ready()){
          int ix = bytesRead;	// index to current end of tempdata
          bytesReadThisTime = bufferedReader.read(postdata, 0, length);  // DS: check we've read what we should
          bytesRead += bytesReadThisTime;
        		
          if (DEBUG) {
            System.out.println ("read " + bytesReadThisTime + " more bytes, "+bytesRead);
          }

          for (int i = 0; (i < bytesReadThisTime) && (ix + i < length); i++) {
            tempdata[ix + i] = postdata[i];
          }

          postdata = new char[length];

        }
      }

      String readerData = new String(tempdata);
      StringReader sreader = new StringReader(readerData);
      return new RequestData(RequestData.DECODE,null,sreader);
    } catch (IOException ioe) {
        System.out.println("HTTPClientSocket stripReplyProtocol IOException: "+ioe);
        throw new ProtocolException();
    } catch (Exception e) {		// DS, 9/1/98: catch all (the request failed)
        System.out.println("HTTPClientSocket stripReplyProtocol Exception: "+e);
        throw new ProtocolException();
    }
  }

  /** 
   * This method generates an error message if a request can't
   * be handled properly, to the point where a contextual error message 
   * can still be sent as the reply.  CURRENTLY RETURNS EMPTY DATAOBJECT - 
   * NEEDS WORK (AKD).
   *
   * @return error message in the form of a DataObject
   * @see #getFatalMessage()
   */
  public DataObject getErrorMessage() {
    return new DataObject();
  }

  /** 
   * This method generates an fatal message if a request can't
   * be handled properly, to the point where no contextual error message 
   * can be sent as the reply.  CURRENTLY RETURNS EMPTY STRING - 
   * NEEDS WORK (AKD).
   *
   * @return fatal error message
   * @see #getErrorMessage()
   */
  public String getFatalMessage() {
    return new String("");
  }


  /** 
   * This method sends a request to a remote server
   * 
   * @param content The request to send
   * @return socket for the connection
   */
  public Socket sendRequest(String content) {
  	
    OutputStream rawOut = null;
			
    // if (content == null)
    //   throw new RPCException("No XML content set for request");
		
    if (DEBUG) {		
      System.out.println ("about to create socket " + remoteServer + " " + portNumber);
      System.out.println ("CONTENT is:\n" + content);
    }
    
    Socket s = null;
    try {
      s = new Socket(remoteServer, portNumber);
    } catch (Exception e) {
        System.out.println ("While creating socket in sendRequest: " + e);
    }

    try {
      rawOut = s.getOutputStream();
    } catch (Exception e) {
        System.out.println ("While getting OutputStream in sendRequest: " + e);
    }

    BufferedOutputStream buffOut = new BufferedOutputStream(rawOut);
    DataOutputStream out = new DataOutputStream(buffOut);

    try {
      out.writeBytes(content);
System.out.println(this.getClass().getName() + ":\r\n" + content);      
      out.flush();
    } catch (Exception e) {
        System.out.println ("While creating socket in sendRequest: " + e);
    }

    return s;
  }

  /** 
   * Method to get the communications protocol being used
   *
   * @return communications protocol being used
   * @see #PROTOCOL
   */
  public String getProtocol() {
    return PROTOCOL;
  }
  
 /* public static void main(String[] args){
	  try{
		  HTTPClientSocket client = new HTTPClientSocket();
		  //String request = client.addRequestProtocol("HelloWorld", "www.hku.hk", "GET");
		  String request = client.addRequestProtocol("HelloWorld", "www.hku.hk", "POST");
		  
		  client.sendRequest(request);
		 
//		  
  
	  }catch(Exception e){
		  System.out.println(e);
	  }
	  	  
  }
*/
}
