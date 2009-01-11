package context.arch.subscriber;

import context.arch.storage.Conditions;
import context.arch.storage.Attributes;
import context.arch.comm.language.MessageHandler;
import context.arch.util.FileRead;
import context.arch.comm.language.DecodeException;
import context.arch.comm.language.InvalidDecoderException;
import context.arch.comm.language.EncodeException;
import context.arch.comm.language.InvalidEncoderException;

import java.util.Hashtable;
import java.util.Vector;
import java.util.Enumeration;
import java.io.StringReader;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.IOException;

/**
 * This class maintains a list of subscribers, allows additions, removals and
 * updates to individual subscribers.
 *
 * @see context.arch.subscriber.Subscriber
 */
public class Subscribers extends Vector {

  private final static String ENTRY_STRING = "entry:";
  private final static String ADD_SUB = "addSub:";
  private final static String REMOVE_SUB = "removeSub:";
  private final static String UPDATE_SUB = "updateSub:";

  private Hashtable hash;
  private MessageHandler mh;
  private String filename;

  /**
   * Basic constructor that takes an object that implements the MessageHandler
   * interface and an id to create a logfile name from.
   */
  public Subscribers(MessageHandler mh, String id) {
    super(20);
    this.mh = mh;
    hash = new Hashtable();

    filename = new String(id+"-subscription.log");
    restartSubscriptions(); //re-do all subscription in the log file
  }

  /** 
   * This method reads in the subscription log, restarts all the subscriptions
   * that were valid at the time of this object being shut down and writes
   * out the valid subscriptions to the log.  It deletes the old log and
   * creates a new one, so that it can clear out entries in the log for
   * corresponding unsubscribes and subscribes.
   */
  private void restartSubscriptions() {
    String log = new FileRead(filename).read();
    int index = log.indexOf(ENTRY_STRING);
    while (index != -1) {
      String entry1 = null;
      int index2 = log.indexOf(ENTRY_STRING,index+1); 
      if (index2 == -1) {  //There is no ENTRY_STRING left 
        entry1 = log.substring(index+ENTRY_STRING.length()); //delete ENTRY_STRING
      }
      else { //Otherwise the next one
        entry1 = log.substring(index+ENTRY_STRING.length(),index2);
      }
      try {
        if (entry1.indexOf(ADD_SUB) != -1) {
          index = entry1.indexOf(">");      //delete XML header:<?xml version="1.0"?>         
          String entry = entry1.substring(index+1); 
          Subscriber sub = new Subscriber(mh.decodeData(new StringReader(entry))); 
          addSubscriber(sub,false); 
        }
        else if (entry1.indexOf(REMOVE_SUB) != -1) {
          index = entry1.indexOf(">");
          String entry = entry1.substring(index+1);
          Subscriber sub = new Subscriber(mh.decodeData(new StringReader(entry)));
          removeSubscriber(sub,false);
        }
        else if (entry1.indexOf(UPDATE_SUB) != -1) {
          index = entry1.indexOf(">");
          String entry = entry1.substring(index+1);
          Subscriber sub = new Subscriber(mh.decodeData(new StringReader(entry)));
          updateSubscriber(sub,false);
        }
      } catch (DecodeException de) {
          System.out.println("Subscribers Decode: "+de);
      } catch (InvalidDecoderException ide) {
          System.out.println("Subscribers InvalidDecoder: "+ide);
      }
      index = index2;
    }

    try {
      BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
      for (int i=0; i<numSubscribers(); i++) {
        Subscriber sub = getSubscriberAt(i);
        String header = new String(ENTRY_STRING+ADD_SUB);
        writeLog(header,sub);
      }
      writer.close();
    } catch (IOException ioe) {
        System.out.println("Subscribers IO: "+ioe);
    }
  }

  /**
   * This private method writes an entry to the logfile.
   *
   * @param header Header of the entry to append to the logfile
   * @param sub Subscriber information to put in the entry
   */
  private void writeLog(String header, Subscriber sub) {
    try {
      BufferedWriter writer = new BufferedWriter(new FileWriter(filename,true)); //append rather rather overwrite
      String out = new String(header+mh.encodeData(sub.toDataObject())+"\n");
      writer.write(out,0,out.length());
      writer.flush();
      writer.close();
    } catch (IOException ioe) {
        System.out.println("Subscribers writeLog() IO: "+ioe);
    } catch (EncodeException ee) {
        System.out.println("Subscribers writeLog() Encode: "+ee);
    } catch (InvalidEncoderException iee) {
        System.out.println("Subscribers writeLog() InvalidEncoder: "+iee);
    }
  }

  /**
   * Adds a subscriber to the subscriber list
   *
   * @param id ID of the subscriber
   * @param hostname Name of the subscriber's host computer
   * @param port Port number to send information to
   * @param callback Callback the subscriber will implement
   * @param tag Widget callback the subscriber is subscribing to
   * @param conditions Conditions under which subscriber will receive data
   * @param attributes Attributes to return to subscriber
   */
  public synchronized void addSubscriber(String id, String hostname, int port, String callback,
                                         String tag, Conditions conditions, Attributes attributes) {
    addSubscriber(new Subscriber(id, hostname, port, callback, tag, conditions, attributes));
  }

  /**
   * Adds a subscriber to the subscriber list
   *
   * @param id ID of the subscriber
   * @param hostname Name of the subscriber's host computer
   * @param port Port number to send information to
   * @param callback Callback the subscriber will implement
   * @param tag Widget callback the subscriber is subscribing to
   * @param conditions Conditions under which subscriber will receive data
   * @param attributes Attributes to return to subscriber
   */
  public synchronized void addSubscriber(String id, String hostname, String port, String callback,
                                         String tag, Conditions conditions, Attributes attributes) {
    addSubscriber(id,hostname,new Integer(port).intValue(),callback,tag,conditions,attributes);
  }

  /**
   * Adds a subscriber to the subscriber list
   *
   * @param sub Subscriber object to add
   * @param log Whether to log the subscribe or not
   */
  public synchronized void addSubscriber(Subscriber sub, boolean log) {
    if (hash.get(sub.getHostName()+sub.getPort()+sub.getCallback()) != null) {//remove duplicated subscriber
      removeSubscriber(sub);
    }
    addElement(sub);
    hash.put(sub.getHostName()+sub.getPort()+sub.getCallback(),sub);

    if (log) {
      writeLog(ENTRY_STRING+ADD_SUB,sub);
    }
  }

  /**
   * Adds a subscriber to the subscriber list
   *
   * @param sub Subscriber object to add
   */
  public synchronized void addSubscriber(Subscriber sub) {
    addSubscriber(sub,true);
  }

  /**
   * Removes a subscriber from the subscriber list
   *
   * @param id ID of the subscriber
   * @param hostname Name of the subscriber's host computer
   * @param port Port number to send information to
   * @param callback Callback the subscriber will implement
   * @param tag Widget callback the subscriber is subscribing to
   * @return whether the removal was successful or not
   */
  public synchronized boolean removeSubscriber(String id, String host, int port, String callback, String tag) {
    Subscriber sub = (Subscriber)(hash.get(host+port+callback));
    if (sub != null) {
      return removeSubscriber(sub);
    }
    return false;
  }

  /**
   * Removes a subscriber from the subscriber list
   *
   * @param id ID of the subscriber
   * @param hostname Name of the subscriber's host computer
   * @param port Port number to send information to
   * @param callback Callback the subscriber will implement
   * @param tag Widget callback the subscriber is subscribing to
   * @return whether the removal was successful or not
   */
  public synchronized boolean removeSubscriber(String id, String host, String port, String callback, String tag) { 
    return removeSubscriber(id,host,new Integer(port).intValue(),callback,tag);
  }

  /**
   * Removes a subscriber from the subscriber list
   *
   * @param sub Subscriber object to remove
   * @return whether the removal was successful or not
   */
  public synchronized boolean removeSubscriber(Subscriber sub) {
    return removeSubscriber(sub,true);
  }

  /**
   * Removes a subscriber from the subscriber list
   *
   * @param sub Subscriber object to remove
   * @param log Whether to log the subscribe or not
   * @return whether the removal was successful or not
   */
  public synchronized boolean removeSubscriber(Subscriber sub, boolean log) {
    Subscriber sub2 = (Subscriber)hash.get(sub.getHostName()+sub.getPort()+sub.getCallback());
    if (sub2 != null) {
      removeElement(sub2);
      hash.remove(sub.getHostName()+sub.getPort()+sub.getCallback());
      if (log) {
        writeLog(ENTRY_STRING+REMOVE_SUB,sub);
      }
      return true;
    }
    return false;
  }

  /**
   * Updates a subscriber in the subscriber list.  The subscriber name is  
   * retrieved from the subscriber object and the old subscriber entry with
   * this name is replaced by the given one.
   *
   * @param sub Subscriber object to update
   */
  public synchronized void updateSubscriber(Subscriber sub) {
    updateSubscriber(sub,true);
  }

  /**
   * Updates a subscriber in the subscriber list.  The subscriber name is  
   * retrieved from the subscriber object and the old subscriber entry with
   * this name is replaced by the given one.
   *
   * @param sub Subscriber object to update
   * @param log Whether to log the subscribe or not
   */
  public synchronized void updateSubscriber(Subscriber sub, boolean log) {
    removeElement((Subscriber)(hash.get(sub.getHostName()+sub.getPort()+sub.getCallback())));
    addElement(sub);
    hash.put(sub.getHostName()+sub.getPort()+sub.getCallback(), sub);
    if (log) {
      writeLog(ENTRY_STRING+UPDATE_SUB,sub);
    }
  }
    
  /**
   * Updates a subscriber in the subscriber list.  The old subscriber entry 
   * with the given name is replaced by a new subscriber with the given info.
   *
   * @param name Name of the subscriber
   * @param hostname Name of the subscriber's host computer
   * @param port Port number to send information to
   * @param callback Callback the subscriber will implement
   * @param tag Widget callback the subscriber is subscribing to
   * @param conditions Conditions under which subscriber will receive data
   * @param attributes Attributes to return to subscriber
   */
  public synchronized void updateSubscriber(String name, String hostname, int port, String callback, 
                                            String tag, Conditions conditions, Attributes attributes) {
    updateSubscriber(new Subscriber(name, hostname, port, callback, tag, conditions, attributes));
  }

  /**
   * Updates a subscriber in the subscriber list.  The old subscriber entry 
   * with the given name is replaced by a new subscriber with the given info.
   *
   * @param name Name of the subscriber
   * @param hostname Name of the subscriber's host computer
   * @param port Port number to send information to
   * @param callback Callback the subscriber will implement
   * @param tag Widget callback the subscriber is subscribing to
   * @param conditions Conditions under which subscriber will receive data
   * @param attributes Attributes to return to subscriber
   */
  public synchronized void updateSubscriber(String name, String hostname, String port, String callback, 
                                            String tag, Conditions conditions, Attributes attributes) {
    updateSubscriber(new Subscriber(name, hostname, new Integer(port).intValue(), callback, tag, conditions, attributes));
  }

  /**
   * Returns the subscriber at the given index.  Do not assume that a given
   * subscriber's index will stay constant throughout its lifetime.  When
   * other subscribers are added and removed, a given subscriber's index
   * may change.
   *
   * @param index index value of the Subscriber object to retrieve
   */
  public synchronized Subscriber getSubscriberAt(int index) {
    return (Subscriber)(elementAt(index));
  }

  /**
   * Returns the subscriber with the given name.  
   *
   * @param id ID of the Subscriber object to retrieve
   * @param hostname Name of the subscriber's host computer
   * @param port Port number to send information to
   * @param callback Callback the subscriber will implement
   * @param tag Widget callback the subscriber is subscribing to
   */
  public synchronized Subscriber getSubscriber(String id, String hostname, int port, String callback, String tag) {
    return (Subscriber)(hash.get(hostname+port+callback));
  }

  /**
   * Returns the subscriber with the given name.  
   *
   * @param id ID of the Subscriber object to retrieve
   * @param hostname Name of the subscriber's host computer
   * @param port Port number to send information to
   * @param callback Callback the subscriber will implement
   * @param tag Widget callback the subscriber is subscribing to
   */
  public synchronized Subscriber getSubscriber(String id, String hostname, String port, String callback, String tag) {
    return (Subscriber)(hash.get(hostname+port+callback));
  }

  /**
   * Returns an enumeration containing all the subscribers in the list
   */
  public synchronized Enumeration getSubscribers() {
    return hash.elements();
  }

  /**
   * Returns an enumeration containing all the subscriber names in the list
   */
  public synchronized Enumeration getSubscriberNames() {
    return hash.keys();
  }

  /**
   * Returns the number of subscribers in the list
   */
  public synchronized int numSubscribers() {
    return size();
  }
}
