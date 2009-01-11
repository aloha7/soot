package context.arch.storage;

import java.util.Vector;
import java.util.Hashtable;
import java.util.Enumeration;

import context.arch.comm.DataObject;

/**
 * This class is a container for a group of related attributes and functions.
 * AttributeFunctions can be added, removed, and found in the container.
 */
public class AttributeFunctions extends Vector {

  /**
   * Connector for nested attributes
   */
  public static final char SEPARATOR = '.';

  /**
   * Connector for nested attributes - String
   */
  public static final String SEPARATOR_STRING = new Character(SEPARATOR).toString();

  /**
   * Tag for attributes
   */
  public static final String ATTRIBUTE_FUNCTIONS = "attributeFunctions";

  /**
   * Tag to indicate all attributes are to be used
   */
  public static final String ALL = "allAttributes";

  /**
   * Empty constructor 
   */
  public AttributeFunctions() {
    super();
  }

  /**
   * Constructor that takes a DataObject as a parameter.  The DataObject
   * is expected to contain an <ATTRIBUTEFUNCTIONS> tag.
   * The constructor stores the encoded data in an AttributeFunctions object.
   *
   * @param data DataObject that contains the attribute name (and possibly type and function) info
   */
  public AttributeFunctions(DataObject data) {
    super();
    DataObject atts = data.getDataObject(ATTRIBUTE_FUNCTIONS);
    if (atts == null) {
      return;
    }
    Vector v = atts.getChildren();
    for (int i=0; i<v.size(); i++) {
      addAttributeFunction(new AttributeFunction((DataObject)v.elementAt(i)));
    }
  }

  /**
   * Converts to a DataObject.
   *
   * @return AttributeFunctions object converted to an <ATTRIBUTE_FUNCTIONS> DataObject
   */
  public DataObject toDataObject() {
    Vector v = new Vector();
    for (int i=0; i<numAttributeFunctions(); i++) {
      v.addElement(getAttributeFunctionAt(i).toDataObject());
    }   
    return new DataObject(ATTRIBUTE_FUNCTIONS,v);
  }

  /**
   * Adds the given AttributeFunction object to the container.
   *
   * @param att AttributeFunction to add
   */
  public void addAttributeFunction(AttributeFunction att) {
    addElement(att);
  }

  /**
   * Adds the given attribute name.  
   *
   * @param name Name of the attribute to add
   */
  public void addAttributeFunction(String name) {
    addAttributeFunction(name,(AttributeFunctions)null,AttributeFunction.DEFAULT_TYPE);
  }

  /**
   * Adds the given attribute name and data type
   *
   * @param name Name of the attribute to add
   * @param type Datatype of the attribute to add
   */
  public void addAttributeFunction(String name, String type) {
    addAttributeFunction(name,(AttributeFunctions)null,type);
  }

  /**
   * Adds the given attribute name and data type and function
   *
   * @param name Name of the attribute to add
   * @param type Datatype of the attribute to add
   * @param function Function of the attribute to add
   */
  public void addAttributeFunction(String name, String type, String function) {
    addAttributeFunction(name,null,type,function);
  }

  /**
   * Adds the given attribute name and value to the container.  It uses a default
   * datatype.
   *
   * @param name Name of the attribute to add
   * @param attributes SubAttributes of the attribute to add
   */
  public void addAttributeFunction(String name, AttributeFunctions attributes) {
    addAttributeFunction(name,attributes,Attribute.DEFAULT_TYPE);
  }

  /**
   * Adds the given attribute name, subAttributes and type to the container
   *
   * @param name Name of the attribute to add
   * @param attributes SubAttributes of the attribute to add
   * @param type Datatype of the attribute to add
   */
  public void addAttributeFunction(String name, AttributeFunctions attributes, String type) {
    addElement(new AttributeFunction(name,attributes,type));
  }

  /**
   * Adds the given attribute name, subAttributes and type to the container
   *
   * @param name Name of the attribute to add
   * @param attributes SubAttributes of the attribute to add
   * @param type Datatype of the attribute to add
   * @param function Function of the attribute to add
   */
  public void addAttributeFunction(String name, AttributeFunctions attributes, String type, String function) {
    addElement(new AttributeFunction(name,attributes,type, function));
  }

  /**
   * Adds the given AttributeFunctions object to the container.
   *
   * @param atts Attributes to add
   */
  public void addAttributeFunctions(AttributeFunctions atts) {
    for (int i=0; i<atts.numAttributeFunctions(); i++) {
      addElement(atts.getAttributeFunctionAt(i));
    }
  }

  /**
   * Returns the AttributeFunction object at the given index
   *
   * @param index Index into the container
   * @return AttributeFunction at the specified index
   */
  public AttributeFunction getAttributeFunctionAt(int index) {
    return (AttributeFunction)elementAt(index);
  }

  /**
   * Determines whether the given AttributeFunction object is in the container
   *
   * @param att AttributeFunction to check
   * @return whether AttributeFunction is in the container
   */
  public boolean hasAttributeFunction(AttributeFunction att) {
    return contains(att);
  }

  /**
   * Determines whether the given attribute name and subAttributes are in the container,
   * using the default datatype.
   *
   * @param name Name of the attribute to check
   * @param attributes SubAttributes of the attribute to check
   * @return whether the given attribute name and subAttributes are in the container
   */
  public boolean hasAttributeFunction(String name, AttributeFunctions attributes) {
    return hasAttributeFunction(name,attributes,Attribute.DEFAULT_TYPE);
  }

  /**
   * Determines whether the given attribute name, subAttributes and type are in the container,
   *
   * @param name Name of the attribute to check
   * @param attributes SubAttributes of the attribute to check
   * @param type Datatype of the attribute to check
   * @return whether the given attribute name, subAttributes and type are in the container
   */
  public boolean hasAttributeFunction(String name, AttributeFunctions attributes, String type) {
    return contains(new AttributeFunction(name,attributes,type));
  }

  /**
   * Returns the index at which the AttributeFunction object occurs
   *
   * @param att AttributeFunction to look for
   * @return index of the specified AttributeFunction
   */
  public int indexOfAttributeFunction(AttributeFunction att) {
    return indexOf(att);
  }

  /**
   * Returns the index at which the given attribute name and subAttributes 
   * occurs, using the default datatype
   *
   * @param name Name of the attribute to look for
   * @param attributes SubAttributes of the attribute to look for
   * @return index of the specified AttributeFunction
   */
  public int indexOfAttributeFunction(String name, AttributeFunctions attributes) {
    return indexOfAttributeFunction(name,attributes,AttributeFunction.DEFAULT_TYPE);
  }

  /**
   * Returns the index at which the given attribute name, subAttributes and type occurs.
   *
   * @param name Name of the attribute to look for
   * @param attributes SubAttributes of the attribute to look for
   * @param type Datatype of the attribute to look for
   * @return index of the specified Attribute
   */
  public int indexOfAttributeFunction(String name, AttributeFunctions attributes, String type) {
    return indexOf(new AttributeFunction(name,attributes,type));
  }

  /**
   * Returns the number of AttributeFunctions in the container
   *
   * return the number of AttributeFunctions in the container
   */
  public int numAttributeFunctions() {
    return size();
  }

  /**
   * This method returns the AttributeFunction with the given name
   * from this list of AttributeFunctions.
   *
   * @param name of the AttributeFunction to return
   * @return AttributeFunction with the given name
   */
  public AttributeFunction getAttributeFunction(String name) {
    return getAttributeFunction(name,"");
  }

  /**
   * This method returns the AttributeFunction with the given name
   * from this list of AttributeFunctions.
   *
   * @param name of the AttributeFunction to return
   * @param prefix Structure name to use
   * @return AttributeFunction with the given name
   */
  public AttributeFunction getAttributeFunction(String name, String prefix) {
    prefix = prefix.trim();
    if ((prefix.length() != 0) && (!(prefix.endsWith(SEPARATOR_STRING)))) {
      prefix = prefix +SEPARATOR_STRING;
    }
    for (int i=0; i<numAttributeFunctions(); i++) {
      AttributeFunction att = getAttributeFunctionAt(i);
      if ((prefix+att.getName()).equals(name)) {
        AttributeFunction attribute = new AttributeFunction(name, att.getSubAttributeFunctions(), att.getType());
        return attribute;
      }
      else if (att.getType().equals(Attribute.STRUCT)) {
        AttributeFunctions atts = att.getSubAttributeFunctions();
        att = atts.getAttributeFunction(name,prefix+att.getName());
        if (att != null) {
          return att;
        }
      }
    }
    return null;
  }

  /**
   * This method takes a DataObject containing the list of attributes
   * (names) wanted and it filters all the rest out from this AttributeFunctions
   * object.
   *
   * @param atts AttributeFunctions object containing the attributes to return
   * @return filtered Attributes object
   */
  public AttributeFunctions getSubset(AttributeFunctions atts) {
    if (atts.numAttributeFunctions() == 0) {
      return this;
    }
    
    AttributeFunction att = atts.getAttributeFunctionAt(0);
    if (!(att.getName().equals(ALL))) {
      AttributeFunctions subset = new AttributeFunctions();
      for (int i=0; i<atts.numAttributeFunctions(); i++) {
        att = atts.getAttributeFunctionAt(i);
        AttributeFunction subAtt = getAttributeFunction(att.getName());
        if (subAtt != null) {
          subset.addAttributeFunction(subAtt);
        }
      }
      return subset;
    }
    return this;
  }
   
  /**
   * A printable version of this class.
   *
   * @return String version of this class
   */
  public String toAString() {
    StringBuffer sb = new StringBuffer();
    for (int i=0; i<numAttributeFunctions(); i++) {
      sb.append(((AttributeFunction)getAttributeFunctionAt(i)).toString());
    }
    return sb.toString();
  }
  
}
