package context.arch.storage;

import context.arch.comm.DataObject;

import java.util.Vector;

/**
 * This class is a container for a condition attribute, comparison and value.
 */
public class Condition {

  private String attribute;
  private Object value;
  private int compare;

  /**
   * Tag for attribute name to use in comparison
   */
  public static final String NAME = "name";

  /**
   * Tag for type of comparison
   */
  public static final String COMPARE = "compare";

  /**
   * Tag for value to use for comparison
   */
  public static final String VALUE = "value";

  /**
   * Tag for AttributeCondition
   */
  public static final String CONDITION = "condition";

  /**
   * Empty constructor
   */
  public Condition() {
  }

  /**
   * Constructor that takes an attribute, value and comparison
   *
   * @param name Name of attribute
   * @param compare Comparison to make
   * @param value Value of attribute to compare to
   */
  public Condition(String attribute, int compare, Object value) {
    this.attribute = attribute;
    this.value = value;
    this.compare = compare;
  }

  /**
   * Constructor that creates a Condition object from a DataObject.
   * The DataObject must have a <CONDITION> tag at the top level.
   *
   * @param data DataObject containing the condition info
   */
  public Condition(DataObject data) {
    this.attribute = (String)data.getDataObject(NAME).getValue().firstElement();
    this.compare = new Integer((String)data.getDataObject(COMPARE).getValue().firstElement()).intValue();
    this.value = data.getDataObject(VALUE).getValue().firstElement();
  }
    
  /**
   * Converts this object to a DataObject
   *
   * @return Condition object converted to an <CONDITION> DataObject
   */
  public DataObject toDataObject() {
    Vector v = new Vector();
    v.addElement(new DataObject(NAME,attribute));
    v.addElement(new DataObject(COMPARE,Integer.toString(compare)));
    v.addElement(new DataObject(VALUE,value.toString()));
    return new DataObject(CONDITION, v);
  }
    
  /**
   * Sets the name of an attribute 
   *
   * @param attribute Name of the attribute
   */
  public void setAttribute(String attribute) {
    this.attribute = attribute;
  }

  /**
   * Sets the value of an attribute
   *
   * @param value Value of the attribute
   */
  public void setValue(Object value) {
    this.value = value;
  }

  /**
   * Sets the comparison to make
   *
   * @param compare Comparison to make
   */
  public void setCompare(int compare) {
    this.compare = compare;
  }

  /**
   * Returns the name of the attribute
   *
   * @return name of the attribute
   */
  public String getAttribute() {
    return attribute;
  }
  
  /**
   * Returns the value of the attribute to use for comparison
   *
   * @return value of the attribute to use for comparison
   */
  public Object getValue() {
    return value;
  }

  /**
   * Returns the type of comparison
   *
   * @return type of comparison
   */
  public int getCompare() {
    return compare;
  }

  /**
   * Returns a printable version of the condition object
   *
   * @return printable version of the condition object
   */
  public String toString() {
    return new String("[name="+getAttribute()+",compare="+getCompare()+",value="+getValue()+"]");
  }
}
