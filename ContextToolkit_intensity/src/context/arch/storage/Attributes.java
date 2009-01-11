package context.arch.storage;

import java.util.Vector;
import java.util.Hashtable;
import java.util.Enumeration;

import context.arch.comm.DataObject;

/**
 * This class is a container for a group of related attributes.
 * Attributes can be added, removed, and found in the container.
 */
public class Attributes extends Vector {

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
  public static final String ATTRIBUTES = "attributes";

  /**
   * Tag to indicate all attributes are to be used
   */
  public static final String ALL = "allAttributes";

  /**
   * Empty constructor 
   */
  public Attributes() {
    super();
  }

  /**
   * Constructor that takes a DataObject as a parameter.  The DataObject
   * is expected to contain an <ATTRIBUTES> tag.
   * The constructor stores the encoded data in an Attributes object.
   *
   * @param data DataObject that contains the attribute name (and possibly type) info
   */
  public Attributes(DataObject data) {
    super();
    DataObject atts = data.getDataObject(ATTRIBUTES);
    if (atts == null) {
      return;
    }
    Vector v = atts.getChildren();
    for (int i=0; i<v.size(); i++) {
      addAttribute(new Attribute((DataObject)v.elementAt(i)));
    }
  }

  /**
   * Converts to a DataObject.
   *
   * @return Attributes object converted to an <ATTRIBUTES> DataObject
   */
  public DataObject toDataObject() {
    Vector v = new Vector();
    for (int i=0; i<numAttributes(); i++) {
      v.addElement(getAttributeAt(i).toDataObject());
    }   
    return new DataObject(ATTRIBUTES,v);
  }

  /**
   * Adds the given Attribute object to the container.
   *
   * @param att Attribute to add
   */
  public void addAttribute(Attribute att) {
    addElement(att);
  }

  /**
   * Adds the given attribute name.  
   *
   * @param name Name of the attribute to add
   */
  public void addAttribute(String name) {
    addAttribute(name,null,Attribute.DEFAULT_TYPE);
  }

  /**
   * Adds the given attribute name and data type
   *
   * @param name Name of the attribute to add
   * @param type Datatype of the attribute to add
   */
  public void addAttribute(String name, String type) {
    addAttribute(name,null,type);
  }

  /**
   * Adds the given attribute name and value to the container.  It uses a default
   * datatype.
   *
   * @param name Name of the attribute to add
   * @param attributes SubAttributes of the attribute to add
   */
  public void addAttribute(String name, Attributes attributes) {
    addAttribute(name,attributes,Attribute.DEFAULT_TYPE);
  }

  /**
   * Adds the given attribute name, subAttributes and type to the container
   *
   * @param name Name of the attribute to add
   * @param attributes SubAttributes of the attribute to add
   * @param type Datatype of the attribute to add
   */
  public void addAttribute(String name, Attributes attributes, String type) {
    addElement(new Attribute(name,attributes,type));
  }

  /**
   * Adds the given Attributes object to the container.
   *
   * @param atts Attributes to add
   */
  public void addAttributes(Attributes atts) {
    for (int i=0; i<atts.numAttributes(); i++) {
      addElement(atts.getAttributeAt(i));
    }
  }

  /**
   * Returns the Attribute object at the given index
   *
   * @param index Index into the container
   * @return Attribute at the specified index
   */
  public Attribute getAttributeAt(int index) {
    return (Attribute)elementAt(index);
  }

  /**
   * Removes the Attribute object at the given index
   *
   * @param index Index into the container
   */
  public void removeAttributeAt(int index) {
    removeElementAt(index);
  }

  /**
   * Determines whether the given Attribute object is in the container
   *
   * @param att Attribute to check
   * @return whether Attribute is in the container
   */
  public boolean hasAttribute(Attribute att) {
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
  public boolean hasAttribute(String name, Attributes attributes) {
    return hasAttribute(name,attributes,Attribute.DEFAULT_TYPE);
  }

  /**
   * Determines whether the given attribute name, subAttributes and type are in the container,
   *
   * @param name Name of the attribute to check
   * @param attributes SubAttributes of the attribute to check
   * @param type Datatype of the attribute to check
   * @return whether the given attribute name, subAttributes and type are in the container
   */
  public boolean hasAttribute(String name, Attributes attributes, String type) {
    return contains(new Attribute(name,attributes,type));
  }

  /**
   * Returns the index at which the Attribute object occurs
   *
   * @param att Attribute to look for
   * @return index of the specified Attribute
   */
  public int indexOfAttribute(Attribute att) {
    return indexOf(att);
  }

  /**
   * Returns the index at which the given attribute name and subAttributes 
   * occurs, using the default datatype
   *
   * @param name Name of the attribute to look for
   * @param attributes SubAttributes of the attribute to look for
   * @return index of the specified Attribute
   */
  public int indexOfAttribute(String name, Attributes attributes) {
    return indexOfAttribute(name,attributes,Attribute.DEFAULT_TYPE);
  }

  /**
   * Returns the index at which the given attribute name, subAttributes and type occurs.
   *
   * @param name Name of the attribute to look for
   * @param attributes SubAttributes of the attribute to look for
   * @param type Datatype of the attribute to look for
   * @return index of the specified Attribute
   */
  public int indexOfAttribute(String name, Attributes attributes, String type) {
    return indexOf(new Attribute(name,attributes,type));
  }

  /**
   * Returns the number of Attributes in the container
   *
   * return the number of Attributes in the container
   */
  public int numAttributes() {
    return size();
  }

  /**
   * This method returns the Attribute with the given name
   * from this list of Attributes.
   *
   * @param name of the Attribute to return
   * @return Attribute with the given name
   */
  public Attribute getAttribute(String name) {
    return getAttribute(name,"");
  }

  /**
   * This method returns the Attribute with the given name
   * from this list of Attributes.
   *
   * @param name of the Attribute to return
   * @param prefix Structure name to use
   * @return Attribute with the given name
   */
  public Attribute getAttribute(String name, String prefix) {
    prefix = prefix.trim();
    if ((prefix.length() != 0) && (!(prefix.endsWith(SEPARATOR_STRING)))) {
      prefix = prefix +SEPARATOR_STRING;
    }
    for (int i=0; i<numAttributes(); i++) {
      Attribute att = getAttributeAt(i);
      if ((prefix+att.getName()).equals(name)) {
        Attribute attribute = new Attribute(name, att.getSubAttributes(), att.getType());
        return attribute;
      }
      else if (att.getType().equals(Attribute.STRUCT)) {
        Attributes atts = att.getSubAttributes();
        att = atts.getAttribute(name,prefix+att.getName());
        if (att != null) {
          return att;
        }
      }
    }
    return null;
  }

  /**
   * This method removes the Attribute with the given name
   * from this list of Attributes. It's up to the caller
   * to make sure the Attribute exists.
   *
   * @param name of the Attribute to remove
   */
  public void removeAttribute(String name) {
    removeAttribute(name,"");
  }

  /**
   * This method removes the Attribute with the given name
   * from this list of Attributes. It's up to the caller
   * to make sure the Attribute exists.
   *
   * @param name of the Attribute to remove
   * @param prefix Structure name to use
   */
  public Attribute removeAttribute(String name, String prefix) {
    prefix = prefix.trim();
    if ((prefix.length() != 0) && (!(prefix.endsWith(SEPARATOR_STRING)))) {
      prefix = prefix +SEPARATOR_STRING;
    }
    for (int i=0; i<numAttributes(); i++) {
      Attribute att = getAttributeAt(i);
      if ((prefix+att.getName()).equals(name)) {
				removeAttributeAt(i);
      }
      else if (att.getType().equals(Attribute.STRUCT)) {
        Attributes atts = att.getSubAttributes();
        atts.removeAttribute(name,prefix+att.getName());
      }
    }
    return null;
  }

  /**
   * This method takes a DataObject containing the list of attributes
   * (names) wanted and it filters all the rest out from this Attributes
   * object.
   *
   * @param atts Attributes object containing the attributes to return
   * @return filtered Attributes object
   */
  public Attributes getSubset(Attributes atts) {
    if (atts.numAttributes() == 0) {
      return this;
    }
    
    Attribute att = atts.getAttributeAt(0);
    if (!(att.getName().equals(ALL))) {
      Attributes subset = new Attributes();
      for (int i=0; i<atts.numAttributes(); i++) {
        att = atts.getAttributeAt(i);
        Attribute subAtt = getAttribute(att.getName());
        if (subAtt != null) {
          subset.addAttribute(subAtt);
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
    for (int i=0; i<numAttributes(); i++) {
      Attribute att = getAttributeAt(i);
      if (att.getType().equals(Attribute.STRUCT)) {
        Hashtable hash2 = att.getSubAttributes().toTypesHashtable(prefix+att.getName()+SEPARATOR);
        //STRUCT: prefix is used to identify the tag
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
    for (int i=0; i<numAttributes(); i++) {
      sb.append(((Attribute)getAttributeAt(i)).toString());
    }
    return sb.toString();
  }
  
}
