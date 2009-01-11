package context.arch.storage;

import context.arch.comm.DataObject;

import java.util.Hashtable;
import java.util.Vector;

/**
 * This class is a container for an attribute name, subAttributes (used for
 * structures - STRUCT) and type.
 */
public class Attribute {

  private String name;
  private Attributes attributes;
  private String type;

  /**
   * Tag for an attribute data type
   */
  public static final String ATTRIBUTE_TYPE = "attributeType";

  /**
   * Tag for an attribute
   */
  public static final String ATTRIBUTE = "attribute";

  /**
   * Tag for default attribute type
   */
  public static final String DEFAULT_TYPE = "string";

  /**
   * Tag for structure type
   */
  public static final String STRUCT = "struct";

  /**
   * Tag for LONG datatype
   */
  public static final String LONG = "long";

  /**
   * Tag for DOUBLE datatype
   */
  public static final String DOUBLE = "double";

  /**
   * Tag for FLOAT datatype
   */
  public static final String FLOAT = "float";

  /**
   * Tag for SHORT datatype
   */
  public static final String SHORT = "short";

  /**
   * Tag for INT datatype
   */
  public static final String INT = "int";

  /**
   * Tag for STRING datatype
   */
  public static final String STRING = "string";

  /**
   * Empty constructor
   */
  public Attribute() {
  }

  /**
   * Constructor that takes only a name
   *
   * @param name Name of attribute to store
   */
  public Attribute(String name) {
    this.name = name;
    attributes = null;
    type = null;
  }

  /**
   * Constructor that takes a name, value, and type
   *
   * @param name Name of attribute to store
   * @param attributes subAttributes of this attribute
   * @param type Datatype of attribute to store
   */
  public Attribute(String name, Attributes attributes, String type) {
    this.name = name;
    this.attributes = attributes;
    this.type = type;
  }
  
  /**
   * Constructor that takes a DataObject as input.  The DataObject
   * must have <ATTRIBUTE> as its top-level tag
   *
   * @param attribute DataObject containing the attribute info
   */
  public Attribute(DataObject attribute) {
    type = DEFAULT_TYPE;
    Hashtable hash = attribute.getAttributes();
    if (hash != null) {
      Object o = hash.get(ATTRIBUTE_TYPE);
      if (o != null) {
        type = (String)o;
      }
    }
    name = (String)attribute.getValue().firstElement();
    if (type.equals(STRUCT)) {
      attributes = new Attributes((DataObject)attribute.getValue().elementAt(1));
    }
    else {
      attributes = null;
    }
  }

  /**
   * Converts this object to a DataObject.
   *
   * @return Attribute object converted to an <ATTRIBUTE> DataObject
   */
  public DataObject toDataObject() {
    if ((type == null) || (!(type.equals(STRUCT)))) {
      DataObject dobj = new DataObject(ATTRIBUTE,name);
      if (type != null) {
        Hashtable hash = new Hashtable();
        hash.put(ATTRIBUTE_TYPE,type);
        dobj.setAttributes(hash);
      }
      return dobj;
    }
    else {
      Vector u = new Vector();
      u.addElement(name);
      u.addElement(attributes.toDataObject());
      DataObject dobj = new DataObject(ATTRIBUTE,u);
      Hashtable hash = new Hashtable();
      hash.put(ATTRIBUTE_TYPE,type);
      dobj.setAttributes(hash);
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
   * Sets the subAttributes of this attribute 
   *
   * @param attributes subAttributes of the attribute to store
   */
  public void setSubAttributes(Attributes attributes) {
    this.attributes = attributes;
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
   * Returns the subAttributes of the stored attribute
   *
   * @return subAttributes of the stored attribute
   */
  public Attributes getSubAttributes() {
    return attributes;
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
    return new String("[name="+getName()+", atts="+getSubAttributes()+",type="+getType()+"]");
  }
}
