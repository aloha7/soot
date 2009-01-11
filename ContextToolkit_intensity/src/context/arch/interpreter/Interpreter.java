package context.arch.interpreter;

import context.arch.BaseObject;
import context.arch.comm.DataObject;
import context.arch.storage.Attributes;
import context.arch.storage.AttributeNameValues;
import context.arch.storage.AttributeNameValue;
import context.arch.util.Error;

import java.util.Vector;
import java.util.Hashtable;
import java.util.Enumeration;

/**
 * This class is the basic interpreter.
 *
 * @see context.arch.BaseObject
 */
public abstract class Interpreter extends BaseObject {

  /**
   * Tag for interpreter
   */
  public static final String INTERPRET = "interpret";

  /**
   * Tag for interpreterReply
   */
  public static final String INTERPRET_REPLY = "interpretReply";

  /**
   * Default port to use for interpreters
   */
  public static final int DEFAULT_PORT = 7000;

  protected Attributes inAttributes;
  protected Attributes outAttributes;
  protected Hashtable inAttributeTypes;
  protected Hashtable outAttributeTypes;
  
  /**
   * Constructor that creates a BaseObject with the given port and sets the
   * incoming and outgoing attributes.
   *
   * @param port Port number to create the BaseObject on
   * @see context.arch.BaseObject
   */
  public Interpreter(int port) {
    super(port);
    inAttributes = setInAttributes();
    inAttributeTypes = inAttributes.toTypesHashtable();
    outAttributes = setOutAttributes();
    outAttributeTypes = outAttributes.toTypesHashtable();
  }

  /**
   * This method is meant to handle any internal methods that the baseObject doesn't
   * handle.  In particular, this method handles interpret requests.  It ensures
   * that the ID of the incoming request matches this interpreter.  If the 
   * method is an INTERPRET method, it sends it to the interpreter.  Otherwise
   * runInterpreterMethod() is called.
   *
   * @param data DataObject containing the method to run and parameters
   * @return DataObject containing the results of running the method 
   * @see #runInterpreterMethod(DataObject,String)
   */
  public DataObject runUserMethod(DataObject data) {
    DataObject interpreter = data.getDataObject(ID);
    String error = null;
    
    if (interpreter == null) {
      error = Error.INVALID_ID_ERROR;
    }
    else {
      String queryId = (String)(interpreter.getValue().firstElement());
      if (!queryId.equals(getId())) {
        error = Error.INVALID_ID_ERROR;
      }
    }
    
    String methodType = data.getName();

    if (methodType.equals(INTERPRET)) {
      return callInterpreter(data,error);
    }
    else {
      return runInterpreterMethod(data,error);
    }
  }
    
  /**
   * This method ensures that the incoming attributes are correct and calls
   * interpretData().  It returns the interpreted results.
   *
   * @param data Incoming interpret request
   * @param error Incoming error, if any
   * @return interpreted results
   * @see #interpretData(AttributeNameValues)
   */
  private DataObject callInterpreter(DataObject data, String error) {
    Vector v = new Vector();
    DataObject result = new DataObject(INTERPRET_REPLY,v);
    AttributeNameValues dataToInterpret = null;
    Error err = new Error(error);
    if (err.getError() == null) {
      dataToInterpret = new AttributeNameValues(data);
      if (dataToInterpret == null) {
        err.setError(Error.MISSING_PARAMETER_ERROR);
      }
      else if (!canHandle(dataToInterpret)) { //Key must be IButton2Name
        err.setError(Error.INVALID_ATTRIBUTE_ERROR);
      }
    }

    if (err.getError() == null) {
      AttributeNameValues interpreted = interpretData(dataToInterpret);
      if (interpreted != null) {
        v.addElement(interpreted.toDataObject());
        err.setError(Error.NO_ERROR);
      }
      else {
        err.setError(Error.INVALID_DATA_ERROR);
      }
    }
    v.addElement(err.toDataObject());
    return result;
  }

  /**
   * This abstract method interprets the given data and returns it. 
   *
   * @param data AttributeNameValues containing data to be interpreted
   * @return AttributeNameValues object containing the interpreted data
   */
  protected abstract AttributeNameValues interpretData(AttributeNameValues data);

  /**
   * This is an empty method that should be overridden by objects
   * that subclass from this class.  It is called when another component
   * tries to run a method on the interpreter, but it's not an interpret
   * request.
   *
   * @param data DataObject containing the data for the method
   * @param error String containing the incoming error value
   * @return DataObject containing the method results
   */
  protected DataObject runInterpreterMethod(DataObject data, String error) {
    String name = data.getName();
    Error err = new Error(error);
    if (err.getError() == null) {
      err.setError(Error.UNKNOWN_METHOD_ERROR);
    }
    Vector v = new Vector();
    v.addElement(err.toDataObject());
    return new DataObject(data.getName());
  }

  /**
   * This method checks the list of incoming attributes to ensure that the
   * interpreter can handle these attributes.  
   * 
   * @param inAtts List of incoming attributes to check
   * @return whether the list of attributes is valid
   */
  private boolean canHandle(AttributeNameValues inAtts) {
    for (int i=0; i<inAtts.numAttributeNameValues(); i++) {
      AttributeNameValue inAtt = inAtts.getAttributeNameValueAt(i);
      if (!isInAttribute(inAtt.getName())) {
        return false;
      }
    }
    return true;
  }

  /**
   * Returns the attribute type with the given name for incoming attributes
   *
   * @param name Name of the attribute to get
   */   
  protected String getInAttributeType(String name) {
    return (String)inAttributeTypes.get(name);
  }

  /**
   * Adds an incoming attribute
   *
   * @param name Name of the attribute to set
   * @param type Type of the attribute
   */   
  protected void setInAttribute(String name, String type) {
    inAttributeTypes.put(name, type);
    inAttributes.addAttribute(name,type);
  }

  /**
   * Checks if the given incoming attribute is an attribute of this interpreter
   *
   * @param name Name of the attribute to check
  */
  protected boolean isInAttribute(String name) {
    return inAttributeTypes.containsKey(name);
  }
	
  /**
   * Returns the attribute type with the given name for outgoing attributes
   *
   * @param name Name of the attribute to get
   */   
  protected String getOutAttributeType(String name) {
    return (String)outAttributeTypes.get(name);
  }

  /**
   * Adds an outgoing attribute
   *
   * @param name Name of the attribute to set
   * @param type Type of the attribute
   */   
  protected void setOutAttribute(String name, String type) {
    outAttributeTypes.put(name, type);
    outAttributes.addAttribute(name,type);
  }

  /**
   * Checks if the given outgoing attribute is an attribute of this interpreter
   *
   * @param name Name of the attribute to check
  */
  protected boolean isOutAttribute(String name) {
    return outAttributeTypes.containsKey(name);
  }

  /**
   * Sets the incoming attributes for the interpreter
   */
  protected abstract Attributes setInAttributes();

  /**
   * Sets the outgoing attributes for the interpreter
   */
  protected abstract Attributes setOutAttributes();

}
