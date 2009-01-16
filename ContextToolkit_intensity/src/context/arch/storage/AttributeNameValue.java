package context.arch.storage;

import context.arch.comm.DataObject;

import java.util.Hashtable;
import java.util.Vector;

/**
 * This class is a container for an attribute name, value and type.
 */
public class AttributeNameValue extends Attribute {

  private String name;
  private Object value;
  private String type;

  /**
   * Tag for an attribute name/value pair
   */
  public static final String ATTRIBUTE_NAME_VALUE = "attributeNameValue";

  /**
   * Tag for an attribute name
   */
  public static final String ATTRIBUTE_NAME = "attributeName";

  /**
   * Tag for an attribute value
   */
  public static final String ATTRIBUTE_VALUE = "attributeValue";

  /**
   * Empty constructor
   */
  public AttributeNameValue() {
  }

  /**
   * Constructor that takes only a name
   *
   * @param name Name of attribute to store
   */
  public AttributeNameValue(String name) {
    this.name = name;
    value = null;
    type = null;
  }

  /**
   * Constructor that takes a name, value and type
   *
   * @param name Name of attribute to store
   * @param value Value of attribute to store
   * @param type Datatype of attribute to store
   */
  public AttributeNameValue(String name, Object value, String type) {
    this.name = name;
    this.value = value;
    this.type = type;
  }

  /**
   * Constructor that takes a DataObject as input.  The DataObject
   * must have <ATTRIBUTE_NAME_VALUE> as its top-level tag
   *
   * @param attribute DataObject containing the attribute info
   */
  public AttributeNameValue(DataObject attribute) {
	  String name1 = (String)attribute.getDataObject(ATTRIBUTE_NAME).getValue().firstElement();
    type = DEFAULT_TYPE;
    Hashtable hash = attribute.getAttributes();
    if (hash != null) {
      Object o = hash.get(ATTRIBUTE_TYPE);
      if (o != null) {
        type = (String)o;
      }
    }
    if (type.equals(STRUCT)) {
      value = new AttributeNameValues(attribute.getDataObject(AttributeNameValues.ATTRIBUTE_NAME_VALUES));
    }
    else {
      Vector val = attribute.getDataObject(ATTRIBUTE_VALUE).getValue();
      value = val.firstElement();
//      value = (String)attribute.getDataObject(ATTRIBUTE_VALUE).getValue().firstElement();
    }
    name = (String)attribute.getDataObject(ATTRIBUTE_NAME).getValue().firstElement();
  }

  /**
   * Converts this object to a DataObject.
   *
   * @return AttributeNameValue object converted to an <ATTRIBUTE_NAME_VALUE> DataObject
   */
  public DataObject toDataObject() {
    if (type.equals(STRUCT)) {
      Vector u = new Vector();
      u.addElement(new DataObject(ATTRIBUTE_NAME,name));
      u.addElement(((AttributeNameValues)value).toDataObject());
      DataObject dobj = new DataObject(ATTRIBUTE_NAME_VALUE,u);
      Hashtable hash = new Hashtable();
      hash.put(ATTRIBUTE_TYPE,STRUCT);
      dobj.setAttributes(hash);
      return dobj;
    }
    else {
      Vector vec = new Vector();
      vec.addElement(new DataObject(ATTRIBUTE_NAME, name));
      Vector v = new Vector();
      //2008/7/7: how to do it if value is "null"?
      if(value == null){
    	  v.addElement(null);  
      }else{
    	  v.addElement(value.toString());  
      }           
      vec.addElement(new DataObject(ATTRIBUTE_VALUE, v));
      DataObject dobj = new DataObject(ATTRIBUTE_NAME_VALUE, vec);
      Hashtable hash = new Hashtable();
      if (!(type.equals(STRING))) {
        hash.put(ATTRIBUTE_TYPE,type);
        dobj.setAttributes(hash);
      }
      return dobj;
    }
  }  

  /**
   * Sets the name of an attribute 
   *
   * @param name Name of the attribute to store
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Sets the value of an attribute
   *
   * @param value Value of the attribute to store
   */
  public void setValue(Object value) {
    this.value = value;
  }

  /**
   * Sets the datatype of an attribute 
   *
   * @param type Datatype of the attribute to store
   */
  public void setType(String type) {
    this.type = type;
  }

  /**
   * Returns the name of the stored attribute
   *
   * @return name of the stored attribute
   */
  public String getName() {
    return name;
  }
  
  /**
   * Returns the value of the stored attribute
   *
   * @return value of the stored attribute
   */
  public Object getValue() {
    return value;
  }

  /**
   * Returns the datatype of the attribute
   *
   * @return name of the attribute
   */
  public String getType() {
    return type;
  }

  /**
   * A printable version of this class.
   *
   * @return String version of this class
   */
  public String toString() {
    return new String("[name="+getName()+",value="+getValue()+",type="+getType()+"]");
  }
}
