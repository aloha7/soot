package context.arch.subscriber;

import java.util.Vector;

import context.arch.comm.DataObject;
import context.arch.storage.Attributes;

/**
 * This class is a container for a group of callbacks.
 * Callbacks can be added, removed, and found in the container.
 */
public class Callbacks extends Vector {

  /**
   * Tag for a widget's callbacks
   */
  public static final String CALLBACKS = "callbacks";

  /**
   * Empty constructor 
   */
  public Callbacks() {
    super();
  }

  /**
   * Constructor that takes a DataObject as a parameter.  The DataObject
   * must contain the tag <CALLBACKS>.  It stores the encoded data.
   *
   * @param data DataObject that contains the callback info
   */
  public Callbacks(DataObject data) {
    super();
    DataObject calls = data.getDataObject(CALLBACKS);
    Vector v = calls.getChildren();
    for (int i=0; i<v.size(); i++) {
      addCallback(new Callback((DataObject)v.elementAt(i)));
    }
  }

  /**
   * Converts to a DataObject.
   *
   * @return Callbacks object converted to an <CALLBACKS> DataObject
   */
  public DataObject toDataObject() {
    Vector v = new Vector();
    for (int i=0; i<numCallbacks(); i++) {
      v.addElement(getCallbackAt(i).toDataObject());
    }   
    return new DataObject(CALLBACKS,v);
  }

  /**
   * Adds the given Callback object to the container.
   *
   * @param callback Callback to add
   */
  public void addCallback(Callback callback) {
    addElement(callback);
  }

  /**
   * Adds the given callback name and attributes to the container.  
   *
   * @param name Name of the callback to add
   * @param attributes Attributes of the callback being added
   */
  public void addCallback(String name, Attributes attributes) {
    addElement(new Callback(name,attributes));
  }

  /**
   * Adds the given Callbacks object to the container.
   *
   * @param callbacks Callbacks to add
   */
  public void addCallbacks(Callbacks callbacks) {
    for (int i=0; i<callbacks.numCallbacks(); i++) {
      addCallback(callbacks.getCallbackAt(i));
    }
  }

  /**
   * Returns the Callback object at the given index
   *
   * @param index Index into the container
   * @return Callback at the specified index
   */
  public Callback getCallbackAt(int index) {
    return (Callback)elementAt(index);
  }

  /**
   * Determines whether the given Callback object is in the container
   *
   * @param call Callback to check
   * @return whether Callback is in the container
   */
  public boolean hasCallback(Callback callback) {
    return contains(callback);
  }

  /**
   * Determines whether the given callback name and attributes are in the container.
   *
   * @param name Name of the callback to check
   * @param attributes Attributes of the callback to check
   * @return whether the given callback name and attributes are in the container
   */
  public boolean hasCallback(String name, Attributes attributes) {
    return contains(new Callback(name,attributes));
  }

  /**
   * Determines whether a callback with the given name is in the container
   *
   * @param name Name of the callback to look for
   * @return whether a callback with the given name is in the container
   */
  public boolean hasCallback(String name) {
    for (int i=0; i<numCallbacks(); i++) {
      if (getCallbackAt(i).getName().equals(name)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns the index at which the Callback object occurs
   *
   * @param callback Callback to look for
   * @return index of the specified Callback
   */
  public int indexOfCallback(Callback callback) {
    return indexOf(callback);
  }

  /**
   * Returns the index at which the given callback name and attributes occurs
   *
   * @param name Name of the callback to look for
   * @param attributes Attributes of the callback to look for
   */
  public int indexOfCallback(String name, Attributes attributes) {
    return indexOf(new Callback(name,attributes));
  }

  /**
   * Returns the number of Callbacks in the container
   *
   * return the number of Callbacks in the container
   */
  public int numCallbacks() {
    return size();
  }

  /**
   * This method returns the Callback with the given name
   * from this list of Callbacks.
   *
   * @param name of the Callback to return
   * @return Callback with the given name
   */
  public Callback getCallback(String name) {
    for (int i=0; i<numCallbacks(); i++) {
      Callback callback = getCallbackAt(i);
      if (callback.getName().equals(name)) {
        return callback;
      }
    }
    return null;
  }
  
  /**
   * A printable version of this class.
   *
   * @return String version of this class
   */
  public String toAString() {
    StringBuffer sb = new StringBuffer();
    for (int i=0; i<numCallbacks(); i++) {
      sb.append(((Callback)getCallbackAt(i)).toString());
    }
    return sb.toString();
  }
  
}
