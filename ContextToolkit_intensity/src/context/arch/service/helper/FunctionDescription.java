package context.arch.service.helper;

import context.arch.comm.DataObject;

import java.util.Vector;

/**
 * This class implements a service function description object.
 *
 * @see context.arch.service.Services
 */
public class FunctionDescription {

  /**
   * Tag for a service function
   */
  public static final String FUNCTION = "function";

  /**
   * Tag for a service function name
   */
  public static final String FUNCTION_NAME = "functionName";

  /**
   * Tag for a service function description
   */
  public static final String FUNCTION_DESCRIPTION = "functionDescription";

  /**
   * Tag for a service function timing - synchronous or asynchronous
   */
  public static final String FUNCTION_TIMING = "functionTiming";

  private String name;
  private String description;
  private String timing;

  /**
   * Basic constructor that creates a function description object.
   * @param name Name of the function
   * @param description Text description of the function
   * @param timing ASYNCHRONOUS or SYNCHRONOUS 
   */
  public FunctionDescription(String name, String description, String timing) {
    this.name = name;
    this.description = description;
    this.timing = timing;
  }

  /**
   * Basic constructor that creates a function description object from a DataObject.
   * The dataObject is expected to have a <FUNCTION> tag as the top level.
   *
   * @param data DataObject containing function description info
   */
  public FunctionDescription(DataObject data) {
    DataObject nameObj = data.getDataObject(FUNCTION_NAME);
    name = (String)nameObj.getValue().firstElement();
    DataObject descriptionObj = data.getDataObject(FUNCTION_DESCRIPTION);
    description = (String)descriptionObj.getValue().firstElement();
    DataObject timingObj = data.getDataObject(FUNCTION_TIMING);
    timing = (String)timingObj.getValue().firstElement();
  }

  /**
   * This method converts the service function info to a DataObject
   *
   * @return FunctionDescription object converted to a <FUNCTION> DataObject
   */
  public DataObject toDataObject() {
    Vector v = new Vector();
    v.addElement(new DataObject(FUNCTION_NAME,name));
    v.addElement(new DataObject(FUNCTION_DESCRIPTION,description));
    v.addElement(new DataObject(FUNCTION_TIMING,timing));
    return new DataObject(FUNCTION, v);
  }

  /**
   * Sets the Name of the service function
   *
   * @param name Name of the service function
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Returns the name of the service function
   *
   * @return Name of the service function
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the description of the service functions
   *
   * @param description Description of the service function
   */
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * Returns the description of the service function
   *
   * @return description of the service function
   */
  public String getDescription() {
    return description;
  }

  /**
   * Sets the timing of the service function - ASYNCHRONOUS or SYNCHRONOUS
   *
   * @param timing Timing of the service function
   */
  public void setTiming(String timing) {
    this.timing = timing;
  }

  /**
   * Return the timing of the service function
   *
   * @return timing of the service function
   */
  public String getTiming() {
    return timing;
  }

}
