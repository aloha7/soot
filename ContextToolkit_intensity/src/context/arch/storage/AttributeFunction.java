package context.arch.storage;

import context.arch.comm.DataObject;

import java.util.Hashtable;
import java.util.Vector;

/**
 * This class is a container for an attribute name, a function, subAttributes (used for
 * structures - STRUCT) and type.
 */
public class AttributeFunction extends Attribute {

  private String name;
  private AttributeFunctions afs;
  private String type;
  private String function;

  /**
   * Tag for an attribute function object
   */
  public static final String ATTRIBUTE_FUNCTION = "attributeFunction";

  /**
   * Tag for an attribute function
   */
  public static final String FUNCTION = "function";

  /**
   * Tag for an attribute
   */
  public static final String ATTRIBUTE = "attribute";

  /**
   * Tag for default function - none
   */
  public static final String FUNCTION_NONE = "none";

  /**
   * Tag for MAX function
   */
  public static final String FUNCTION_MAX = "max";

  /**
   * Tag for MIN function
   */
  public static final String FUNCTION_MIN = "min";

  /**
   * Tag for COUNT function
   */
  public static final String FUNCTION_COUNT = "count";

  /**
   * Tag for AVG function
   */
  public static final String FUNCTION_AVG = "avg";

  /**
   * Tag for SUM function
   */
  public static final String FUNCTION_SUM = "sum";

  /**
   * Empty constructor
   */
  public AttributeFunction() {
  }

  /**
   * Constructor that takes only a name
   *
   * @param name Name of attribute to store
   */
  public AttributeFunction(String name) {
    this.name = name;
    afs = null;
    type = null;
    function = FUNCTION_NONE;
  }

  /**
   * Constructor that takes only a name and a function
   *
   * @param name Name of attribute to store
   * @param function Function to execute on attribute
   */
  public AttributeFunction(String name, String function) {
    this.name = name;
    afs = null;
    type = null;
    this.function = function;
  }

  /**
   * Constructor that takes a name, value, and type
   *
   * @param name Name of attribute to store
   * @param afs subAttributes of this attribute
   * @param type Datatype of attribute to store
   */
  public AttributeFunction(String name, AttributeFunctions afs, String type) {
    this.name = name;
    this.afs = afs;
    this.type = type;
    function = FUNCTION_NONE;
  }

  /**
   * Constructor that takes a name, value, and type
   *
   * @param name Name of attribute to store
   * @param afs subAttributes of this attribute
   * @param type Datatype of attribute to store
   * @param function Function to execute on attribute
   */
  public AttributeFunction(String name, AttributeFunctions afs, String type, String function) {
    this.name = name;
    this.afs = afs;
    this.type = type;
    this.function = function;
  }
  
  /**
   * Constructor that takes a DataObject as input.  The DataObject
   * must have <ATTRIBUTE_FUNCTION> as its top-level tag
   *
   * @param attribute DataObject containing the attribute info
   */
  public AttributeFunction(DataObject af) {
    type = DEFAULT_TYPE;
    Hashtable hash = af.getAttributes();
    if (hash != null) {
      Object o = hash.get(ATTRIBUTE_TYPE);
      if (o != null) {
        type = (String)o;
      }
      function = (String)hash.get(FUNCTION);
    }
    name = (String)af.getValue().firstElement();
    if (type.equals(STRUCT)) {
      afs = new AttributeFunctions((DataObject)af.getValue().elementAt(1));
    }
    else {
      afs = null;
    }
  }

  /**
   * Converts this object to a DataObject.
   *
   * @return Attribute object converted to an <ATTRIBUTE_FUNCTION> DataObject
   */
  public DataObject toDataObject() {
    if ((type == null) || (!(type.equals(STRUCT)))) {
      DataObject dobj = new DataObject(ATTRIBUTE_FUNCTION,name);
      if ((type != null) && (function != null)) {
        Hashtable hash = new Hashtable();
        if (type != null) {
          hash.put(ATTRIBUTE_TYPE,type);
        }
        if (function != null) {
          hash.put(FUNCTION,function);
        }
        dobj.setAttributes(hash);
      }
      return dobj;
    }
    else {
      Vector u = new Vector();
      u.addElement(name);
      u.addElement(afs.toDataObject());
      DataObject dobj = new DataObject(ATTRIBUTE_FUNCTION,u);
      Hashtable hash = new Hashtable();
      hash.put(ATTRIBUTE_TYPE,type);
      hash.put(FUNCTION,function);
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
   * @param afs subAttributes of the attribute to store
   */
  public void setSubAttributes(AttributeFunctions afs) {
    this.afs = afs;
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
   * Sets the function of an attribute 
   *
   * @param function Function to act on the attribute
   */
  public void setFunction(String function) {
    this.function = function;
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
  public AttributeFunctions getSubAttributeFunctions() {
    return afs;
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
   * Returns the function of an attribute 
   *
   * @return function to act on the attribute
   */
  public String getFunction() {
    return function;
  }

  /**
   * A printable version of this class.
   * 
   * @return String version of this class
   */
  public String toString() {
    return new String("[name="+getName()+", atts="+getSubAttributes()+",type="+getType()+",function="+getFunction()+"]");
  }
}
