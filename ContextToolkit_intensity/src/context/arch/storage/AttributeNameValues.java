package context.arch.storage;

import java.util.Vector;
import java.util.Hashtable;
import java.util.Enumeration;

import context.arch.comm.DataObject;

/**
 * This class is a container for a group of related attributes.  It
 * contains information about attribute names, values and types
 * AttributeNameValues can be added, removed, and found in the container.
 */
public class AttributeNameValues extends Vector {

  /**
   * Tag for a list of attribute name/value pairs
   */
  public static final String ATTRIBUTE_NAME_VALUES = "attributeNameValues";

  /**
   * Connector for nested attributes
   */
  public static final char SEPARATOR = '.';

  /**
   * Connector for nested attributes - String
   */
  public static final String SEPARATOR_STRING = new Character(SEPARATOR).toString();

  /**
   * Tag to indicate all attributes are to be used
   */
  public static final String ALL = "allAttributes";

  /**
   * Empty constructor 
   */
  public AttributeNameValues() {
    super();
  }

  /**
   * Constructor that takes a DataObject as a parameter.  An expected tag 
   * contained within the DataObject is <ATTRIBUTE_NAME_VALUES>.  
   * The constructor stores the encoded data in an AttributeNameValues object.
   *
   * @param data DataObject that contains the attribute name-value (and possibly type) info
   */
  public AttributeNameValues(DataObject data) {
    super();
    DataObject atts = data.getDataObject(ATTRIBUTE_NAME_VALUES);
    if (atts == null) {
      return;
    }
    Vector v = atts.getChildren();
    for (int i=0; i<v.size(); i++) {
      AttributeNameValue anv = new AttributeNameValue((DataObject)v.elementAt(i));
      if (anv.getValue() != null) {
        addAttributeNameValue(anv);
      }
    }
  }

  /**
   * Converts to a DataObject.
   *
   * @return Attributes object converted to an <ATTRIBUTE_NAME_VALUES> DataObject
   */
  public DataObject toDataObject() {
    Vector v = new Vector();
    for (int i=0; i<numAttributeNameValues(); i++) {
      v.addElement(getAttributeNameValueAt(i).toDataObject());
    }
    return new DataObject(ATTRIBUTE_NAME_VALUES, v);
  }

  /**
   * Adds the given AttributeNameValue object to the container.
   *
   * @param att AttributeNameValue to add
   */
  public void addAttributeNameValue(AttributeNameValue att) {
    addElement(att);
  }

  /**
   * Adds the given attribute name.  
   *
   * @param name Name of the attribute to add
   */
  public void addAttributeNameValue(String name) {
    addAttributeNameValue(name,null,null);
  }

  /**
   * Adds the given attribute name and value to the container.  It uses a default
   * datatype.
   *
   * @param name Name of the attribute to add
   * @param value Value of the attribute to add
   */
  public void addAttributeNameValue(String name, Object value) {
    addAttributeNameValue(name,value,AttributeNameValue.DEFAULT_TYPE);
  }

  /**
   * Adds the given attribute name, value and type to the container
   *
   * @param name Name of the attribute to add
   * @param value Value of the attribute to add
   * @param type Datatype of the attribute to add
   */
  public void addAttributeNameValue(String name, Object value, String type) {
    addElement(new AttributeNameValue(name,value,type));
  }

  /**
   * Adds the given AttributeNameValues object to the container.
   *
   * @param atts AttributeNameValues to add
   */
  public void addAttributeNameValues(AttributeNameValues atts) {
    for (int i=0; i<atts.numAttributeNameValues(); i++) {
      addElement(atts.getAttributeNameValueAt(i));
    }
  }

  /**
   * Returns the AttributeNameValue object at the given index
   *
   * @param index Index into the container
   * @return AttributeNameValue at the specified index
   */
  public AttributeNameValue getAttributeNameValueAt(int index) {
    return (AttributeNameValue)elementAt(index);
  }

  /**
   * Determines whether the given AttributeNameValue object is in the container
   *
   * @param att AttributeNameValue to check
   * @return whether AttributeNameValue is in the container
   */
  public boolean hasAttributeNameValue(AttributeNameValue att) {
    return contains(att);
  }

  /**
   * Determines whether the given attribute name and value are in the container,
   * using the default datatype.
   *
   * @param name Name of the attribute to check
   * @param value Value of the attribute to check
   * @return whether the given attribute name and value are in the container
   */
  public boolean hasAttributeNameValue(String name, Object value) {
    return hasAttributeNameValue(name,value,AttributeNameValue.DEFAULT_TYPE);
  }

  /**
   * Determines whether the given attribute name, value and type are in the container,
   *
   * @param name Name of the attribute to check
   * @param value Value of the attribute to check
   * @param type Datatype of the attribute to check
   * @return whether the given attribute name, value and type are in the container
   */
  public boolean hasAttributeNameValue(String name, Object value, String type) {
    return contains(new AttributeNameValue(name,value,type));
  }

  /**
   * Returns the index at which the AttributeNameValue object occurs
   *
   * @param att AttributeNameValue to look for
   * @return index of the specified AttributeNameValue
   */
  public int indexOfAttributeNameValue(AttributeNameValue att) {
    return indexOf(att);
  }

  /**
   * Returns the index at which the given attribute name and value occurs, using
   * the default datatype
   *
   * @param name Name of the attribute to look for
   * @param value Value of the attribute to look for
   * @return index of the specified Attribute
   */
  public int indexOfAttributeNameValue(String name, Object value) {
    return indexOfAttributeNameValue(name,value,AttributeNameValue.DEFAULT_TYPE);
  }

  /**
   * Returns the index at which the given attribute name, value and type occurs.
   *
   * @param name Name of the attribute to look for
   * @param value Value of the attribute to look for
   * @param type Datatype of the attribute to look for
   * @return index of the specified Attribute
   */
  public int indexOfAttributeNameValue(String name, Object value, String type) {
    return indexOf(new AttributeNameValue(name,value,type));
  }

  /**
   * Returns the number of AttributeNameValues in the container
   *
   * return the number of AttributeNameValues in the container
   */
  public int numAttributeNameValues() {
    return size();
  }

  /**
   * Removes the given AttributeNameValue object to the container.
   *
   * @param att AttributeNameValue to remove
   */
  public void removeAttributeNameValue(AttributeNameValue att) {
    removeElement(att);
  }

  /**
   * This method returns the AttributeNameValue with the given name
   * from this list of AttributeNameValues.
   *
   * @param name of the AttributeNameValue to return
   * @return AttributeNameValue with the given name
   */
  public AttributeNameValue getAttributeNameValue(String name) {
    return getAttributeNameValue(name,"");
  }

  /**
   * This method returns the AttributeNameValue with the given name
   * from this list of AttributeNameValue.
   *
   * @param name of the AttributeNameValue to return
   * @param prefix Structure name to use
   * @return AttributeNameValue with the given name
   */
  public AttributeNameValue getAttributeNameValue(String name, String prefix) {
    prefix = prefix.trim();
    if ((prefix.length() != 0) && (!(prefix.endsWith(SEPARATOR_STRING)))) {
      prefix = prefix +SEPARATOR_STRING;
    }
    for (int i=0; i<numAttributeNameValues(); i++) {
      AttributeNameValue att = getAttributeNameValueAt(i);
      if ((prefix+att.getName()).equals(name)) {
        return att;
      }
      else if (att.getType().equals(AttributeNameValue.STRUCT)) {
        AttributeNameValues atts = (AttributeNameValues)att.getValue();
        att = atts.getAttributeNameValue(name,prefix+att.getName());
        if (att != null) {
          return att;
        }
      }
    }
    return null;
  }

  /**
   * This method takes a list of attributes wanted and it filters all 
   * the rest out from this Attributes object.
   *
   * @param atts Attributes object containing the attributes to return
   * @return filtered AttributeNameValues object
   */
  public AttributeNameValues getSubset(Attributes atts) {
    if (atts.numAttributes() == 0) {
      return this;
    }
    
    Attribute att = atts.getAttributeAt(0);
    if (!(att.getName().equals(ALL))) {
      AttributeNameValues subset = new AttributeNameValues();
      for (int i=0; i<atts.numAttributes(); i++) {
        att = atts.getAttributeAt(i);
        AttributeNameValue subAtt = getAttributeNameValue(att.getName());
        if ((subAtt != null)  && (subAtt.getValue() != null)) {
          subset.addAttributeNameValue(subAtt);
        }
      }
      return subset;
    }
    return this;
  }
   
  /**
   * Converts the attributes name-type pairs to a hashtable where the
   * keys are the names and the values are the types.
   * AKD - problem if two attributes have same name but different types
   */
  public Hashtable toTypesHashtable() {
    return this.toTypesHashtable("");
  }

  /**
   * Converts the attributes name-type pairs to a hashtable where the
   * keys are the names and the values are the types.  This method allows
   * the use of a prefix for structures.
   * AKD - problem if two attributes have same name but different types
   */
  public Hashtable toTypesHashtable(String prefix) {
    prefix = prefix.trim();
    if ((prefix.length() != 0) && (!(prefix.endsWith(SEPARATOR_STRING)))) {
      prefix = prefix +SEPARATOR_STRING;
    }
    Hashtable hash = new Hashtable();
    for (int i=0; i<numAttributeNameValues(); i++) {
      AttributeNameValue att = getAttributeNameValueAt(i);
      if (att.getType().equals(AttributeNameValue.STRUCT)) {
        Hashtable hash2 = ((AttributeNameValues)att.getValue()).toTypesHashtable(prefix+att.getName()+SEPARATOR);
        for (Enumeration e=hash2.keys();e.hasMoreElements();) {
          Object key = e.nextElement();
          hash.put((String)key,hash2.get(key));
        }
      }
      hash.put(prefix+att.getName(),att.getType());
    }
    return hash;
  }
  
  /**
   * A printable version of this class.
   *
   * @return String version of this class
   */
  public String toAString() {
    StringBuffer sb = new StringBuffer();
    for (int i=0; i<numAttributeNameValues(); i++) {
      sb.append(((AttributeNameValue)getAttributeNameValueAt(i)).toString());
    }
    return sb.toString();
  }
  
}
