package context.arch.comm;

import java.util.Hashtable;
import java.util.Vector;

/**
 * This class implements the DataObject class.  It stores the data used for
 * sending messages between components.
 */
public class DataObject {

  private String name;
  private Hashtable attributes;
  private Vector value;
  private DataObject currentObject;
  private DataObject parent;
  private DataObject attribute;
  private String currentElement;
  
  /**
   * Debug flag. Set to true to see debug messages.
   */
  private static final boolean DEBUG = false;

  /** 
   * Basic constructor.  Sets up necessary internal variables.
   */
  public DataObject() { 
    currentObject = this;
    currentObject.parent = null;
    this.value = new Vector();
  }

  /**
   * Constructor that sets the name of the DataObject element
   *
   * @param name Name of the DataObject element
   */
  public DataObject(String name) {
    this();
    this.name = name;
  }

  /**
   * Constructor that sets the name of the DataObject element and a single
   * value for the element
   *
   * @param name Name of the DataObject element
   * @param value Value of the DataObject element
   */
  public DataObject(String name, String value) {
    this(name);
    this.value.addElement(value);  
  }
	
  /**
   * Constructor that sets the name of the DataObject element and a vector
   * of values for the element
   *
   * @param name Name of the DataObject element
   * @param value Vector of values for the DataObject element
   */
  public DataObject(String name, Vector value) {
    this(name);
    this.value = value;	
  }
	
  /**
   * Constructor that sets the name of the DataObject element, a vector
   * of values for the element, and a list of attributes
   *
   * @param name Name of the DataObject element
   * @param atts Hashtable of attributes for the DataObject element
   * @param value Vector of values for the DataObject element
   */
  public DataObject(String name, Hashtable atts, Vector value) {
    this(name,value);
    this.attributes = atts;
  }

  /**
   * Returns the name of the DataObject element
   *
   * @return Name of the DataObject element
   */	
  public String getName() {
    return name;
  }

  /**
   * Returns the list of attributes for the DataObject element
   *
   * @return Hashtable of attributes for the DataObject element
   */	
  public Hashtable getAttributes() {
    return attributes;
  }

  /**
   * Returns the values for the DataObject element
   *
   * @return Vector of values for the DataObject element
   */	
  public Vector getValue() {
    return value;
  }

  /**
   * Sets the name of the DataObject element
   *
   * @param name Name of the DataObject element
   */	
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Sets the list of attributes for the DataObject element
   *
   * @param atts Hashtable of attributes for the DataObject element
   */	
  public void setAttributes(Hashtable atts) {
    this.attributes = atts;
  }

  /**
   * Sets a value for the DataObject element
   *
   * @param value Value for the DataObject element
   */	
  public void setValue(String value) {
    this.value = new Vector();
    this.value.addElement(value);
  }

  /**
   * Sets the values for the DataObject element
   *
   * @param value Vector of values for the DataObject element
   */	
  public void setValue(Vector value) {
    this.value = value;
  }

  /**
   * Counts the children of the current DataObject
   *
   * @return int the number of children
   */	
  public int countChildren() {
    Vector v = getValue();
    int ct = 0; // the result
    
    for (int i = 0; i < v.size(); i++) {
      if (v.elementAt(i).getClass().getName().equals("context.arch.comm.DataObject")) {
        ct++;
      }
    }
    
    return ct;
  }


  /**
   * Returns the specified child of the current DataObject
   *
   * @param int Index of the child to return
   * @return Vector the requested child
   */	
  public Vector getChild(int i) {
    Vector v = getValue();
    Vector r = new Vector (); // result
    
  	if (v.elementAt(i).getClass().getName().equals("context.arch.comm.DataObject")) {
      r.addElement (v.elementAt(i));
    }
  
    return r;
  }


  /**
   * Returns the children of the current DataObject
   *
   * @return Vector a vector containing the children
   */	
  public Vector getChildren() {
    Vector v = getValue();
    Vector r = new Vector (); // result
    
    for (int i = 0; i < v.size(); i++) {
    	if (v.elementAt(i).getClass().getName().equals("context.arch.comm.DataObject")) {
        r.addElement (v.elementAt(i));
      }
    }
    
    return r;
  }

  /**
   * Returns the DataObject element/sub-element with the specified name
   *
   * @param string Name of the element to return
   * @return DataObject with the specified name or null, if not found
   */ 
  public DataObject getDataObject(String string) {
    DataObject result = null;

    if (!(this.name.equals(string))) { 
      Vector v = getValue();
      for (int i=0;i<v.size(); i++) {
        if (v.elementAt(i).getClass().getName().equals("context.arch.comm.DataObject")) {
          DataObject obj = ((DataObject)(v.elementAt(i)));
          if ((result = obj.getDataObject(string)) != null) { //height-first search
            return result;
          }
        }
      }
    }
    else {
      return this;
    }
    return null;
  }
  
  
  /**
   * Returns the Nth DataObject element/sub-element with the specified name
   * NB: we assume the current DataObject has 1 level of children. Thus,
   * getNthDataObject (x, 1) is not equivalent to getDataObject (x)
   * I'll fix this later. --DS
   *
   * @param string Name of the element to return
   * @return DataObject with the specified name or null, if not found
   */ 
  public DataObject getNthDataObject(String string, int n) {
    DataObject result = null;
    int ct = 0;

    Vector v = getValue();
    // NB: optimization: compute v.size () in advance and return null if n is <
    for (int i=0;i<v.size(); i++) {
      if (v.elementAt(i).getClass().getName().equals("context.arch.comm.DataObject")) {
        DataObject obj = ((DataObject)(v.elementAt(i)));
        if (DEBUG) {
          System.out.println ("Found a DataObject: "+obj);
        }
        if (obj.name.equals (string)) {  // found one
          ct++;
          if (DEBUG) {
            System.out.println ("It has the right name! ct = "+ct);
          }
          if (ct >= n) {  // it's the nth!
            return obj;
          }
        }
      }
    }
    return null;
  }

  /**
   * This method looks for an element in this DataObject.
   *
   * @param name Name of an element
   * @return boolean true if there is an element by that name, else false
   */
  public boolean existsElement(String name) {
    int n = this.countChildren();
    boolean result = false;

    if (n > 0) {
      Vector children = this.getChildren();
      for (int i = 0; i < n; i++) {
        DataObject currentChild = (DataObject)children.elementAt (i);
        String currentName = currentChild.getName();
				
        if (currentName.equals (name)) {
          // sounds like we've found it
          result = true;
        }
        if (result == false) { // carry on searching the children
          if (currentChild.countChildren () > 0) {
            result = currentChild.existsElement(name);
          }
          if (result) { // did we find it?
            break;	// then stop here
          }
        }
      } // for
    }
    return result;
  }
	

  /**
   * This method adds an element to this DataObject
   *
   * @param name Name of an element
   */
  public void addElement(String name) {
    currentObject.name = name;
    DataObject tmp = new DataObject();
    tmp.parent = currentObject;
    if (currentObject.parent != null) {
      currentObject.parent.getValue().addElement(currentObject);
    }
    currentObject = tmp;
  }

  /**
   * This method adds an element and list of attributes to this DataObject
   *
   * @param name Name of an element
   * @param atts Hashtable list of attributes
   */
  public void addElement(String name, Hashtable atts) {
    if (atts.size() != 0) {
    	currentObject.attributes = atts;
    }
    currentObject.name = name;
    DataObject tmp = new DataObject();
    tmp.parent = currentObject;
    if (currentObject.parent != null) {
      currentObject.parent.getValue().addElement(currentObject);
    }
    currentObject = tmp;
  }

  /**
   * This method closes the currently open/added element in order to do some 
   * internal housecleaning.
   *
   * @param name Name of the element being closed 
   */
  public void closeElement(String name) {
    DataObject tmp = new DataObject();
    if (currentObject.parent != null) {
      tmp.parent = currentObject.parent.parent;
    }
    else {
      tmp.parent = null;
    }
    currentObject = tmp;
  }

  /**
   * Adds a value to the current element
   *
   * @param value Value being added to the current element
   */
  public void addValue(String value) {
    currentObject.parent.getValue().addElement(value);
  } 

  /**
   * Returns the first value of the DataObject element/sub-element with 
   * the specified name, if it exists.  Returns null otherwise.
   *
   * @param string Name of the element to return
   * @return 1st value of the DataObject with the specified name or null, if not found
   */ 
  public Object getDataObjectFirstValue(String string) {
    DataObject dobj = getDataObject(string);
    if (dobj != null) {
      Vector v = dobj.getValue();
      if (v != null) {
        return v.firstElement();
      }
    }
    return null;
  }

  /**
   * This method creates a string version of the recursive DataObject
   *
   * @return String version (printable) version of the DataObject
   */
  public String toString() {
    StringBuffer sb = new StringBuffer(getClass().getName());
    sb.append("[name="+getName());
    for (int i=0; i<getValue().size(); i++) {
      sb.append(","+name+"-value "+i+"=");
      if (getValue().elementAt(i).getClass().getName().equals("java.lang.String")) {
        sb.append((String)(getValue().elementAt(i)));
      }
    	else {
        sb.append(((DataObject)(getValue().elementAt(i))).toString());
      }
    }
    sb.append ("]");
    return sb.toString();
  }

}
