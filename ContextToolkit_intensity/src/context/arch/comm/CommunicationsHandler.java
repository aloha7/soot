package context.arch.comm;

import context.arch.comm.DataObject;
import context.arch.comm.language.DecodeException;
import context.arch.comm.language.InvalidDecoderException;
import context.arch.comm.language.EncodeException;
import context.arch.comm.language.InvalidEncoderException;
import context.arch.comm.protocol.ProtocolException;
import context.arch.comm.protocol.InvalidProtocolException;
import context.arch.handler.AsyncServiceHandler;
import context.arch.storage.AttributeNameValues;

/**
 * This interface specifies all the basic methods to allow communications with
 * other components.  Currently, this means calling userRequest, 
 * executeAsynchronousWidgetService and executeSynchronousWidgetService.
 */
public interface CommunicationsHandler {

  /**
   * Method that allows a component to communicate with another component.
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
  public abstract DataObject userRequest(DataObject data, String url) throws EncodeException, InvalidProtocolException, ProtocolException, DecodeException, InvalidDecoderException, InvalidEncoderException;

  /**
   * Method that allows a component to communicate with another component.
   *
   * @param data DataObject that contains the request
   * @param url RPC tag that indicates the type of request
   * @param server Hostname of the component to communicate with
   * @return DataObject containing the reply to the request
   * @exception EncodeException when the encoding can't be completed successfully
   * @exception DecodeException when the decoding can't be completed successfully
   * @exception InvalidEncoderException when the encoder can't be created
   * @exception InvalidDecoderException when the decoder can't be created
   * @exception ProtocolException when the request can't be sent successfully
   * @exception InvalidProtocolException when the request can't be sent successfully due to invalid protocol use
   */
  public abstract DataObject userRequest(DataObject data, String url, String server) throws EncodeException, InvalidProtocolException, ProtocolException, DecodeException, InvalidDecoderException, InvalidEncoderException;

  /**
   * Method that allows a component to communicate with another component.
   *
   * @param data DataObject that contains the request
   * @param url RPC tag that indicates the type of request
   * @param server Hostname of the component to communicate with
   * @param port Port number of the component to communicate with
   * @return DataObject containing the reply to the request
   * @exception EncodeException when the encoding can't be completed successfully
   * @exception DecodeException when the decoding can't be completed successfully
   * @exception InvalidEncoderException when the encoder can't be created
   * @exception InvalidDecoderException when the decoder can't be created
   * @exception ProtocolException when the request can't be sent successfully
   * @exception InvalidProtocolException when the request can't be sent successfully due to invalid protocol use
   */
  public DataObject userRequest(DataObject data, String url, String server, int port) throws EncodeException, ProtocolException, InvalidProtocolException, DecodeException, InvalidDecoderException, InvalidEncoderException;

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
                           String serviceId, String service, String function, AttributeNameValues input, String requestTag);

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
                                  String service, String function, AttributeNameValues input);

}
