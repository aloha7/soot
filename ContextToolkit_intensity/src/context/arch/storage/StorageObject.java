package context.arch.storage;

import java.util.Hashtable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.DriverManager;

import gwe.sql.gweMysqlDriver;

import context.arch.comm.DataObject;
import context.arch.util.Constants;

/**
 * This class allows storage and retrieval of data in String, Integer, Long, Float,
 * Double, or Short format.  It uses a default storage class 
 * (context.arch.storage.VectorStorage), but can use any given storage class that
 * implements the Storage interface.
 * 
 * @see context.arch.storage.Storage
 */
public class StorageObject {

  private AttributeNameValues lastStored = null;
  private String storageClass = "";
  private Storage storage = null;

  /**
   * Tag for debugging.
   */
  private static final boolean DEBUG = false;

  /**
   * Tag for retrieving data
   */
  public static final String RETRIEVE_DATA = "retrieveData";

  /**
   * Tag for reply to retrieve data request
   */
  public static final String RETRIEVE_DATA_REPLY = "retrieveDataReply";

  /**
   * The default protocol to use is JDBC
   */
  public static final String PROTOCOL = "jdbc";
  
  /**
   * The default subprotocol to use is msql
   */
  public static final String SUBPROTOCOL = "mysql";

  /**
   * The default storage server
   */
  //private static final String SERVER = "server.name.goes.here";
  private static final String SERVER = "127.0.0.1";
  /**
   * The default storage port
   */
  //private static final String PORT = "9999";
  private static final String PORT = "3306";
  /**
   * The default database
   */
  //private static final String DATABASE = "databaseNameGoesHere";
  private static final String DATABASE = "ContextBase";
  /**
   * The default url to use for storage
   */
  public static final String URL = PROTOCOL+":"+SUBPROTOCOL+"://"+SERVER+":"+PORT+"/"+DATABASE;

  /**
   * The default username
   */
  //public static final String USER = "databaseUserNameGoesHere";
  public static final String USER = "root";
  /**
   * The default password
   */
  //public static final String PASSWORD = "DataBasePasswordGoesHere";
  public static final String PASSWORD = "wh19830";
  /**
   * The default storage class is context.arch.storage.VectorStorage
   */
  public static final String DEFAULT_STORAGE_CLASS = "context.arch.storage.VectorStorage";

  /**
   * Basic constructor that uses the default storage class
   *
   * @param table Name of the table to use - should be the id of the calling object
   * @exception InvalidStorageException when the default storage class can't be created  
   * @see #DEFAULT_STORAGE_CLASS
   */
  public StorageObject(String table) throws InvalidStorageException {
    this(DEFAULT_STORAGE_CLASS, table);
  }

  /**
   * Constructor that sets the storage class to use and creates the 
   * storage class.  If the class is null, the default class is used.
   * If the table is null, an exception is thrown.
   *
   * @param storageClass Class to use for storage
   * @param table Name of the table to use - should be the id of the calling object
   * @exception InvalidStorageException thrown when there are any errors creating the 
   *    storage class object or connecting to the persistent storage
   * @exception InvalidStorageException when the given storage class can't be created  
   * @see #DEFAULT_STORAGE_CLASS
   */
  public StorageObject(String storeClass, String table) throws InvalidStorageException {
    if (storeClass == null) {
      storageClass = DEFAULT_STORAGE_CLASS;
    }
    else {
      storageClass = storeClass;
    }
    if (table == null) {
      throw new InvalidStorageException(table);
    }
    
    if (DEBUG) {
      System.out.println("Storage table is: "+table+storageClass);
    }

    try {
      if (storageClass.equals("context.arch.storage.VectorStorage")) {
    	 String DRIVER = "com.mysql.jdbc.Driver";
        //Class.forName("gwe.sql.gweMysqlDriver");
    	Class.forName(DRIVER);
        Connection con = DriverManager.getConnection(URL, USER, PASSWORD);
        con.close();
      }

      if (System.getProperty("os.name").equals(Constants.WINCE)) {
        storage = (Storage)new VectorStorage(table);
      }
      else {
        Class[] classes = new Class[1];
        classes[0] = Class.forName("java.lang.String");
        Constructor constructor = Class.forName(storageClass).getConstructor(classes);  
        Object[] objects = new Object[1];
        objects[0] = table;
        storage = (Storage)constructor.newInstance(objects);
      }
    } catch (NoSuchMethodException nsme) {
        System.out.println("StorageObject NoSuchMethod: "+nsme);
        throw new InvalidStorageException(storageClass);
    } catch (InvocationTargetException ite) {
        System.out.println("StorageObject InvocationTarget: "+ite);
        throw new InvalidStorageException(storageClass);
    } catch (IllegalAccessException iae) {
        System.out.println("StorageObject IllegalAccess: "+iae);
        throw new InvalidStorageException(storageClass);
    } catch (InstantiationException ie) {
        System.out.println("StorageObject Instantiation: "+ie);
        throw new InvalidStorageException(storageClass);
    } catch (ClassNotFoundException cnfe) {
        System.out.println("StorageObject ClassNotFound: "+cnfe);
        throw new InvalidStorageException(storageClass);
    } catch(SQLException sqle) {
        System.out.println("StorageObject SQL: "+sqle);
        throw new InvalidStorageException(storageClass);
    }
  }

  /**
   * Constructor that sets the storage class to use
   *
   * @param storageClass Class to use for storage
   * @param table Name of the table to use - should be the id of the calling object
   * @param flushType Flush to database based on TIME or DATA
   * @param flushCondition Condition to flush local storage to database
   * @exception InvalidStorageException when the given storage class can't be created  
   */
  public StorageObject(String storageClass, String table, Integer flushType, Long flushCondition) throws InvalidStorageException {
    try {
      Class.forName("gwe.sql.gweMysqlDriver");
      Connection con = DriverManager.getConnection(URL, USER, PASSWORD);
      con.close();

      Class[] classes = new Class[3];
      classes[0] = Class.forName("java.lang.String");
      classes[1] = Class.forName("java.lang.Integer");
      classes[2] = Class.forName("java.lang.Long");
      Constructor constructor = Class.forName(storageClass).getConstructor(classes);  
      Object[] objects = new Object[3];
      objects[0] = table;
      objects[1] = flushType;
      objects[2] = flushCondition;
      storage = (Storage)constructor.newInstance(objects);
    } catch (NoSuchMethodException nsme) {
        System.out.println("StorageObject NoSuchMethod: "+nsme);
        throw new InvalidStorageException(storageClass);
    } catch (InvocationTargetException ite) {
        System.out.println("StorageObject InvocationTarget: "+ite);
        throw new InvalidStorageException(storageClass);
    } catch (IllegalAccessException iae) {
        System.out.println("StorageObject IllegalAccess: "+iae);
        throw new InvalidStorageException(storageClass);
    } catch (InstantiationException ie) {
        System.out.println("StorageObject Instantiation: "+ie);
        throw new InvalidStorageException(storageClass);
    } catch (ClassNotFoundException cnfe) {
        System.out.println("StorageObject ClassNotFound: "+cnfe);
        throw new InvalidStorageException(storageClass);
    } catch(SQLException sqle) {
        System.out.println("StorageObject SQL: "+sqle);
        throw new InvalidStorageException(storageClass);
    }
  }

  /**
   * This method stores the given AttributeNameValues object and checks whether the locally
   * stored data should be flushed to persistent storage.  It is a stub method that
   * simply calls the store(), checkFlushCondition() and flushStorage() methods in the 
   * Storage interface.
   *
   * @param atts AttributeNameValues to store
   * @see context.arch.storage.Storage#store(context.arch.storage.AttributeNameValues)
   * @see context.arch.storage.Storage#flushStorage()
   * @see context.arch.storage.Storage#checkFlushCondition()
   */
  public void store(AttributeNameValues atts) {
	  //2009/1/16:there is no need to store atts
	  /*
    storage.store(atts); //just keep AttributeNameValues in the StorageVector
    if (storage.checkFlushCondition()) {
      storage.flushStorage(); //flush data in the StorageVector to the DataBase one by one
    }
    lastStored = atts;
    */
  }

  /**
   * This method stores the attributes in the given DataObject
   * and checks whether the locally stored data should be flushed to persistent storage.
   * It assumes that the DataObject has the starting tag <ATTRIBUTENAMEVALUES>.  It is 
   * a stub method that converts the DataObject to an AttributeNameValues object and calls
   * the store(AttributeNameValues) method in this class.
   *
   * @param data DataObject containing the attributes to store
   * @see #store(context.arch.storage.AttributeNameValues)
   */
  public void store(DataObject data) {
    AttributeNameValues atts = new AttributeNameValues(data);
    store(atts);
  }

  /**
   * Returns the last AttributeNameValues object stored
   *
   * @return the last AttributeNameValues object stored
   */
  public AttributeNameValues retrieveLastAttributes() {
    return lastStored;
  }

  /**
   * Flushes the locally stored data to persistent storage
   */
  public void flushStorage() {
    storage.flushStorage();
  }

  /**
   * This method returns a vector containing AttributeNameValues objects that matches
   * the given conditions in the Retrieval object  
   * 
   * @param retrieval Retrieval object that contains conditions for retrievalcompare Flag that dictates the type of comparison
   * @return RetrievalResults containing AttributeNameValues objects that matches the given compare
   *         flag and value
   */
  public RetrievalResults retrieveAttributes(Retrieval retrieval) {
    return storage.retrieveAttributes(retrieval);
  }

  /**
   * This method returns a vector containing AttributeNameValues objects that matches
   * the given conditions in the Retrieval object, and that the given requestorId
   * is allowed to have access to
   * 
   * @param accessorId Id of the "user" trying to retrieve data
   * @param retrieval Retrieval object that contains conditions for retrievalcompare Flag that dictates the type of comparison
   * @return RetrievalResults containing AttributeNameValues objects that matches the given compare
   *         flag and value
   */
  public RetrievalResults retrieveAttributes(String requestorId, Retrieval retrieval) {
    return storage.retrieveAttributes(requestorId, retrieval);
  }

  /**
   * This method sets the attributes to use for storage.  
   *
   * @param attributes AttributeNameValues containing attribute info for this object
   * @param attributeTypes Hashtable containing attributes and type info
   */
  public void setAttributes(Attributes attributes, Hashtable attributeTypes) {
    storage.setAttributes(attributes, attributeTypes);
  }

}
