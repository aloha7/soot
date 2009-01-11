package context.arch.storage;

import java.util.Vector;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.Date;
import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;

import gwe.sql.gweMysqlDriver;

/**
 * This class allows storage and retrieval of data in String, Integer, Long, Float,
 * Double, or Short format.  It implements the Storage interface, using a Vector to
 * store data temporarily.  It can flush locally stored data to persistent data upon 
 * request.
 */
public class VectorStorage implements Storage {

  private Hashtable attributeTypes;
  private Attributes attributes;
  private String storageClass = "";
  private long lastFlush = 0;
  private long numStored = 0;
  private int flushType;
  private long flushCondition;
  private String table;
  private boolean firstTime = true;
  private Vector data;

  /**
   * Debug flag. Set to true to see debug messages.
   */
  private static final boolean DEBUG = false;

  /**
   * Default flush type is by number of stores
   */
  public static final int DEFAULT_FLUSH_TYPE = DATA; //flush after 2 stores

  /**
   * Default flush condition is 2 (i.e. flush after 2 stores)
   */
  //public static final long DEFAULT_FLUSH_CONDITION = 2;
  public static final long DEFAULT_FLUSH_CONDITION = 1;
  /**
   * Separator used in structured info
   */
  public static final char OLD_SEPARATOR = Attributes.SEPARATOR;

  /**
   * Separator used in structured info - String
   */
  public static final String OLD_SEPARATOR_STRING = new Character(Attributes.SEPARATOR).toString();

  /**
   * Separator used by database in structured info
   */
  public static final char NEW_SEPARATOR = '_';

  /**
   * Separator used by database in structured info - String
   */
  public static final String NEW_SEPARATOR_STRING = new Character(NEW_SEPARATOR).toString();

  /**
   * Basic constructor that uses the default flush condition
   *
   * @param table Name of table to use
   * @exception SQLException if errors in accessing table info 
   */
  public VectorStorage(String table) throws SQLException {
    this(table,new Integer(DEFAULT_FLUSH_TYPE),new Long(DEFAULT_FLUSH_CONDITION));
  }

  /**
   * Basic constructor that uses the given flush type and condition
   *
   * @param table Name of table to use
   * @param flushType Flush to database based on TIME or DATA
   * @param flushCondition Condition to flush local storage to database
   * @exception SQLException if errors in accessing table info 
   */
  public VectorStorage(String tableName, Integer flushType, Long flushCondition) throws SQLException {
    try {
      Class.forName("gwe.sql.gweMysqlDriver");
    } catch (ClassNotFoundException cnfe) {
        System.out.println("VectorStorage constructor ClassNotFound: "+cnfe);
    }
    table = tableName.replace(' ','_');
    data = new Vector();

    this.flushType = flushType.intValue();
    this.flushCondition = flushCondition.longValue();
    if (this.flushType == TIME) {
      lastFlush = new Date().getTime();
    }
  }

  /**
   * This method stores the given AttributeNameValues object
   *
   * @param atts AttributeNameValues to store
   */
  public void store(AttributeNameValues atts) {
    data.addElement(atts);
    numStored++;
  }

  /**
   * This method returns a Vector containing AttributeNameValue objects that match
   * the given conditions in the Retrieval object. It takes in the accessorId of the
   * "user" requesting the information, but does nothing with it currently.
   * 
   * @param accessorId Id of the "user" trying to retrieve the data
   * @param retrieval Retrievals object containing conditions for data retrieval
   * @return RetrievalResults containing AttributeNameValues objects that match the given conditions
   */
  public RetrievalResults retrieveAttributes(String accessorId, Retrieval retrieval) {
    return retrieveAttributes (retrieval);
  }

  /**
   * This method returns a Vector containing AttributeNameValue objects that match
   * the given conditions in the Retrieval object.
   * 
   * @param retrieval Retrievals object containing conditions for data retrieval
   * @return RetrievalResults containing AttributeNameValues objects that match the given conditions
   */
  public RetrievalResults retrieveAttributes(Retrieval retrieval) {
    flushStorage();
    StringBuffer statement = new StringBuffer("SELECT ");
    AttributeFunctions atts = retrieval.getAttributeFunctions();
    AttributeFunctions newAtts = new AttributeFunctions();
    if (atts.numAttributeFunctions() == 0) {
      return null;
    }
    else {
      // if all attributes requested, get name of each and put into atts
      if (atts.getAttributeFunctionAt(0).getName().equals(AttributeFunctions.ALL)) {
        atts = new AttributeFunctions();
        for (int i=0; i<attributes.numAttributes(); i++) {
          atts.addAttributeFunction(attributes.getAttributeAt(i).getName());
        }
      }
      // for each attribute, fix name for database use and check if struct data
      for (int j=0; j<atts.numAttributeFunctions(); j++) {
        AttributeFunction att = atts.getAttributeFunctionAt(j);
        String name = att.getName().replace(OLD_SEPARATOR,NEW_SEPARATOR);
        // if struct data, flatten struct data and put into newAtts
        if (((String)attributeTypes.get(name)).equals(Attribute.STRUCT)) {
          Hashtable flatAtts = ((Attributes)attributes.getAttribute(att.getName()).getSubAttributes()).toTypesHashtable(att.getName());
          for (Enumeration e = flatAtts.keys(); e.hasMoreElements();) {
            name = (String)e.nextElement();
            String type = (String)flatAtts.get(name);
            if (!(type.equals(Attribute.STRUCT))) {
              newAtts.addAttributeFunction(name.replace(OLD_SEPARATOR,NEW_SEPARATOR),AttributeFunction.DEFAULT_TYPE,AttributeFunction.FUNCTION_NONE);
            }
          }
        }
        // els if not struct data, put attribute into newAtts
        else {
          newAtts.addAttributeFunction(name,AttributeFunction.DEFAULT_TYPE,att.getFunction());
        }
      }
    }

    boolean special = false;
    AttributeFunctions specialAtts = new AttributeFunctions();
    for (int i=0; i<newAtts.numAttributeFunctions(); i++) {
      AttributeFunction af = newAtts.getAttributeFunctionAt(i);
      if ((af.getFunction().equals(AttributeFunction.FUNCTION_MAX)) || (af.getFunction().equals(AttributeFunction.FUNCTION_MIN))) {
        if (!special) {
          specialAtts.addAttributeFunction(af.getName(),af.getFunction());
          statement.append(af.getFunction()+"("+af.getName()+")");
          special = true;
        }
      }
    }

    if (special) {
      statement.append(" FROM "+table);
 
      boolean where = false;
      Conditions conditions = retrieval.getConditions();
      for (int i=0; i<conditions.numConditions(); i++) {
        if (i == 0) { 
          statement.append(" WHERE ");
          where = true;
        }
        else {
          statement.append(" AND ");
        }
        Condition condition = conditions.getConditionAt(i);
        statement.append (condition.getAttribute().replace(OLD_SEPARATOR,NEW_SEPARATOR));
        switch (condition.getCompare()) {
          case LESSTHAN:         statement.append("<");
                                 break;
          case LESSTHANEQUAL:    statement.append("<=");
                                 break;
          case GREATERTHAN:      statement.append(">");
                                 break;
          case GREATERTHANEQUAL: statement.append(">=");
                                 break;
          case EQUAL:            statement.append("=");
                                 break;
        }
        if (((String)attributeTypes.get(condition.getAttribute().replace(OLD_SEPARATOR,NEW_SEPARATOR))).equals(Attribute.STRING)) {
          statement.append("'"+condition.getValue().toString()+"'");
        }
        else {
          statement.append(condition.getValue().toString());
        }   
      }

      statement.append("\n");

      Vector preResults = executeRetrieveQuery(specialAtts, statement.toString());

      if (preResults == null) {
        return null;
      }

      RetrievalResults results = new RetrievalResults();
      AttributeFunctions attNames = specialAtts;
      for (int j=0; j<preResults.size(); j++) {
        Hashtable resultAtts = (Hashtable)preResults.elementAt(j);
        AttributeNameValues newAttValues = new AttributeNameValues();
        for (int k=0; k<attNames.numAttributeFunctions(); k++) {
          String attName = attNames.getAttributeFunctionAt(k).getName();
          AttributeNameValue newAttValue = (AttributeNameValue)resultAtts.get(attName);
          if (newAttValue.getValue() != null) {
            newAttValues.addAttributeNameValue(newAttValue);
          }
        }
        if (newAttValues.numAttributeNameValues() != 0) {
          results.addAttributeNameValues(newAttValues);
        }
      }
      if (results.numAttributeNameValues() != 0) {
        AttributeNameValues anvs = results.getAttributeNameValuesAt(0);
        AttributeNameValue anv = anvs.getAttributeNameValueAt(0);
        conditions.addCondition(anv.getName(),EQUAL,anv.getValue());
      }
      else {
        return null;
      }
      statement = new StringBuffer("SELECT ");
    }

    AttributeFunction af = newAtts.getAttributeFunctionAt(0);
    String func = af.getFunction();
    if (func == null) {
      statement.append(af.getName());
    }
    else {
      if ((func.equals(AttributeFunction.FUNCTION_NONE)) || 
          (func.equals(AttributeFunction.FUNCTION_MAX)) || 
          (func.equals(AttributeFunction.FUNCTION_MIN))) {
          statement.append(af.getName());
      }
      else {
        statement.append(func+"("+af.getName()+")");
      }
    }
    for (int i=1; i<newAtts.numAttributeFunctions(); i++) {
      af = newAtts.getAttributeFunctionAt(i);
      func = af.getFunction();
      if (func == null) {
        statement.append(","+af.getName());
      }
      if ((func.equals(AttributeFunction.FUNCTION_NONE)) || 
          (func.equals(AttributeFunction.FUNCTION_MAX)) || 
          (func.equals(AttributeFunction.FUNCTION_MIN))) {
        statement.append(","+af.getName());
      }
      else {
        statement.append(","+func+"("+af.getName()+")");
      }
    }
    statement.append(" FROM "+table);

    boolean where = false;
    Conditions conditions = retrieval.getConditions();
    for (int i=0; i<conditions.numConditions(); i++) {
      if (i == 0) { 
        statement.append(" WHERE ");
        where = true;
      }
      else {
        statement.append(" AND ");
      }
      Condition condition = conditions.getConditionAt(i);
      statement.append (condition.getAttribute().replace(OLD_SEPARATOR,NEW_SEPARATOR));
      switch (condition.getCompare()) {
        case LESSTHAN:         statement.append("<");
                               break;
        case LESSTHANEQUAL:    statement.append("<=");
                               break;
        case GREATERTHAN:      statement.append(">");
                               break;
        case GREATERTHANEQUAL: statement.append(">=");
                               break;
        case EQUAL:            statement.append("=");
                               break;
      }
      if (((String)attributeTypes.get(condition.getAttribute().replace(OLD_SEPARATOR,NEW_SEPARATOR))).equals(Attribute.STRING)) {
        statement.append("'"+condition.getValue().toString()+"'");
      }
      else {
        statement.append(condition.getValue().toString());
      }   
    }

    statement.append("\n");

    Vector preResults = executeRetrieveQuery(newAtts, statement.toString());

    if (preResults == null) {
      return null;
    }

    RetrievalResults results = new RetrievalResults();
    AttributeFunctions attNames = retrieval.getAttributeFunctions();
    if (attNames.getAttributeFunctionAt(0).getName().equals(Attributes.ALL)) {
      attNames = new AttributeFunctions();
      for (int i=0; i<attributes.numAttributes(); i++) {
        attNames.addAttributeFunction(attributes.getAttributeAt(i).getName());
      }
    }
    for (int j=0; j<preResults.size(); j++) {
      Hashtable resultAtts = (Hashtable)preResults.elementAt(j);
      AttributeNameValues newAttValues = new AttributeNameValues();
      for (int k=0; k<attNames.numAttributeFunctions(); k++) {
        String attName = attNames.getAttributeFunctionAt(k).getName();
        if (((String)attributeTypes.get(attName.replace(OLD_SEPARATOR,NEW_SEPARATOR))).equals(Attribute.STRUCT)) {
          AttributeNameValue newAttValue = getAttributeNameValue(attName,resultAtts);
          if (newAttValue.getValue() != null) {
            newAttValues.addAttributeNameValue(newAttValue);
          }
        }
        else {
          AttributeNameValue newAttValue = (AttributeNameValue)resultAtts.get(attName);
          if (newAttValue.getValue() != null) {
            newAttValues.addAttributeNameValue(newAttValue);
          }
        }
      }
      if (newAttValues.numAttributeNameValues() != 0) {
        results.addAttributeNameValues(newAttValues);
      }
    }
    return results;
  }

  /**
   * This method takes an attribute name (whose attribute type is STRUCT)
   * and returns the complete attribute information for it.
   *
   * @param name Name of the attribute
   * @param values Hashtable containing all the values retrieved from a line
   *        in the database table
   * @return AttributeNameValue containing all the STRUCT's values in a hierarchical
   *         format
   */
  private AttributeNameValue getAttributeNameValue(String name, Hashtable values) {
    return getAttributeNameValue(name,values,"");
  }
  
  /**
   * This method takes an attribute name (whose attribute type is STRUCT)
   * and returns the complete attribute information for it.
   *
   * @param name Name of the attribute
   * @param values Hashtable containing all the values retrieved from a line
   *        in the database table
   * @param prefix to use for structure info
   * @return AttributeNameValue containing all the STRUCT's values in a hierarchical
   *         format
   */
  private AttributeNameValue getAttributeNameValue(String name, Hashtable values, String prefix) {
    prefix = prefix.trim();
    if ((prefix.length() != 0) && (!(prefix.endsWith(OLD_SEPARATOR_STRING)))) {
      prefix = prefix + OLD_SEPARATOR_STRING;
    }
      
    Attribute att = attributes.getAttribute(prefix+name);
    if (att.getType() == Attribute.STRUCT) {
      Attributes atts = (Attributes)att.getSubAttributes();
      AttributeNameValues newAtts = new AttributeNameValues();
      for (int i=0; i<atts.numAttributes(); i++) {
        Attribute subAtt = atts.getAttributeAt(i);
        newAtts.addAttributeNameValue(getAttributeNameValue(prefix+name+OLD_SEPARATOR_STRING+subAtt.getName(),values));
      }
      return new AttributeNameValue(prefix+name,newAtts,Attribute.STRUCT);
    }
    else {
      return (AttributeNameValue)values.get(prefix+name);
    }
  }
    
  /**
   * Checks condition under which local data is sent to persistent storage.
   */
  public boolean checkFlushCondition() {
    if (flushType == TIME) {
      long tmp = new Date().getTime();
      if (lastFlush + flushCondition <= tmp) {
        return true;
      }
    }
    else if (flushType == DATA) {
      if (flushCondition <= numStored) {
        if (DEBUG) {
          System.out.println("flush is true");
        }
        return true;
      }
    }   
    return false;
  }

  /**
   * Flushes local data to persistent storage
   */
  public void flushStorage() {
    if (data.size() == 0) {
      return;
    }

    if (DEBUG) {
      System.out.println("flushing");
    }

    try {
      if (firstTime) {
        firstTime = false;
        if (!(tableExists(table))) {
          createTable();
        }
      } 

      Connection con = DriverManager.getConnection(StorageObject.URL, StorageObject.USER, StorageObject.PASSWORD);
      Statement stmt = con.createStatement();
      for (int i=0; i<data.size(); i++) {
        AttributeNameValues atts = (AttributeNameValues)data.elementAt(i);
        String statement = createInsertStatement(atts);
        stmt.executeUpdate(statement);
      }
      stmt.close();
      con.close();

    } catch(SQLException sqle) {
        System.out.println("VectorStorage flushStorage() SQL: "+sqle);
    }
    resetForFlush();
  }

  /**
   * This private method creates and returns a SQL statement for inserting
   * data into a database.
   *
   * @param atts AttributeNameValues to put in the database
   * @return SQL statement for inserting the attributes into a database
   */
  private String createInsertStatement(AttributeNameValues atts) {
    StringBuffer statement = new StringBuffer("insert into "+ table +" (");
    StringBuffer values = new StringBuffer(" values (");
    for (int j=0; j<atts.numAttributeNameValues(); j++) {
      AttributeNameValue att = atts.getAttributeNameValueAt(j);
      if (j != 0) {
        statement.append(",");
        values.append(",");
      }
      statement.append(att.getName());
      if (att.getType().equals(Attribute.STRING)) {
        values.append("'"+att.getValue()+"'");
      }
      else if (att.getType().equals(Attribute.STRUCT)) {
        AttributeNameValues subAtts = (AttributeNameValues)att.getValue();
        values.append(subAtts.numAttributeNameValues());
        String subResult = createSubInsertStatement(subAtts,att.getName()+NEW_SEPARATOR);
        int index = subResult.indexOf("()");
        statement.append(subResult.substring(0,index));
        values.append(subResult.substring(index+2));
      }
      else {
        values.append(att.getValue());
      }
    }
    values.append(")");
    statement.append(")"+values.toString());
    return statement.toString();
  }
  
  /**
   * This private method creates and returns a SQL statement for inserting
   * data into a database.  It uses prefix information for structures.
   *
   * @param atts AttributeNameValues to put in the database
   * @return SQL statement for inserting the attributes into a database
   */
  private String createSubInsertStatement(AttributeNameValues atts, String prefix) {
    StringBuffer statement = new StringBuffer();
    StringBuffer values = new StringBuffer();
    for (int j=0; j<atts.numAttributeNameValues(); j++) {
      AttributeNameValue att = atts.getAttributeNameValueAt(j);
      statement.append(","+prefix+att.getName());
      if (att.getType().equals(Attribute.STRING)) {
        values.append(",'"+att.getValue()+"'");
      }
      else if (att.getType().equals(Attribute.STRUCT)) {
        AttributeNameValues subAtts = (AttributeNameValues)att.getValue();
        values.append(","+subAtts.numAttributeNameValues());
        String subResult = createSubInsertStatement(subAtts,prefix+NEW_SEPARATOR_STRING+att.getName()+NEW_SEPARATOR);
        int index = subResult.indexOf("()");
        statement.append(subResult.substring(0,index));
        values.append(subResult.substring(index+2));
      }
      else {
        values.append(","+att.getValue());
      }
    }
    statement.append("()"+values.toString());
    return statement.toString();
  }
   
  /** 
   * This private method resets the data for local storage.  It should be called
   * after data is flushed to persistent storage.
   */
  private void resetForFlush() {
    numStored = 0;
    lastFlush = new Date().getTime();
    data = new Vector();
  }
   
  /**
   * This method contacts the database with the given query and returns a Vector
   * of AttributeNameValues objects that match the query, if any.  If the query fails for
   * any reason, null is returned
   *
   * @param attnames Vector of attribute names to return
   * @param query SELECT query to execute on the database
   * @return Vector of AttributeNameValues objects that the query returns 
   */
  private Vector executeRetrieveQuery(AttributeFunctions atts, String query) {
    try {
      Connection con = DriverManager.getConnection(StorageObject.URL, StorageObject.USER, StorageObject.PASSWORD);
      Statement stmt = con.createStatement();
      ResultSet rs = stmt.executeQuery(query);

      Vector v = new Vector();
      while (rs.next()) {
        Hashtable returnAtts = new Hashtable();
        for (int i=0; i<atts.numAttributeFunctions(); i++) {
          AttributeFunction att = atts.getAttributeFunctionAt(i);
          String name = att.getName();
          String type = (String)attributeTypes.get(name);
          String value = (String)rs.getString(i+1);
          name = name.replace(NEW_SEPARATOR,OLD_SEPARATOR);
          returnAtts.put(name,new AttributeNameValue(name, value, type));
        }
        v.addElement(returnAtts);
      }
      rs.close();
      stmt.close();
      con.close();
      return v;       
    } catch(SQLException sqle) {
        System.out.println("VectorStorage executeRetrieveQuery SQL: "+sqle);
    }
    return null;  
  }

  /**
   * Checks to see if the given table exists
   *
   * @param stmt SQL statement to use
   * @param tablename Name of the table to check on
   * @return whether the table exists or not
   * @throws SQLException when problems with check occur
   */
  private boolean tableExists(String tablename) throws SQLException {
    Connection con = DriverManager.getConnection(StorageObject.URL, StorageObject.USER, StorageObject.PASSWORD);
    Statement stmt = con.createStatement();

    ResultSet rs = stmt.executeQuery("SHOW TABLES");
    while(rs.next()) {
      String result = rs.getString(1);
      
      //if (result.equals(tablename)) {
      if(result.equals(tablename.toLowerCase())){
    	  return true;
      }
    }
    rs.close();
    stmt.close();
    con.close();
    return false;
  }

  /**
   * This private method returns attribute type information from the database.
   *
   * @param stmt SQL statement to use
   * @param tablename Name of table to get type information from
   * @return hashtable containing type information, with attribute names as keys
   * @throws SQLException when problems with retrieving the type info occur
   */
  private Hashtable getTypeInfo(String tablename) throws SQLException {
    Hashtable hash = new Hashtable();
    Connection con = DriverManager.getConnection(StorageObject.URL, StorageObject.USER, StorageObject.PASSWORD);
    Statement stmt = con.createStatement();
    ResultSet rs = stmt.executeQuery("SELECT * FROM "+tablename);
    while (rs.next()) {
      hash.put(rs.getString(1),rs.getString(2));
    }
    rs.close();
    stmt.close();
    con.close();
    return hash;
  }

  /**
   * This private method creates a database table for storing attribute values.
   *
   * @param stmt SQL statement to use
   * @throws SQLException when problems creating the table occur
   */
  private void createTable() throws SQLException {
    StringBuffer sb = new StringBuffer("CREATE TABLE "+table+" ( \n");
    int size = attributeTypes.size();
    int i = 1;
    for (Enumeration atts = attributeTypes.keys(); atts.hasMoreElements();i++) {
      String att = (String)atts.nextElement();
      sb.append(att+" ");
      if (((String)attributeTypes.get(att)).equals(Attribute.INT)) {
        sb.append("INT");
        //sb.append("INTEGER");
      }
      else if (((String)attributeTypes.get(att)).equals(Attribute.SHORT)) {
        sb.append("SMALLINT");
      }
      else if (((String)attributeTypes.get(att)).equals(Attribute.DOUBLE)) {
        sb.append("DOUBLE");
      }
      else if (((String)attributeTypes.get(att)).equals(Attribute.FLOAT)) {
        sb.append("FLOAT");
      }
      else if (((String)attributeTypes.get(att)).equals(Attribute.LONG)) {
        sb.append("BIGINT");
      }
      else if (((String)attributeTypes.get(att)).equals(Attribute.STRING)) {
        sb.append("TEXT");
      }
      else if (((String)attributeTypes.get(att)).equals(Attribute.STRUCT)) {
        sb.append("INT");
        //sb.append("INTEGER");
      }
      if (i == size) {
        sb.append(")\n");
      }
      else {
        sb.append(",\n");
      }
    }

    Connection con = DriverManager.getConnection(StorageObject.URL, StorageObject.USER, StorageObject.PASSWORD);
    Statement stmt = con.createStatement();
    stmt.executeUpdate(sb.toString());
    stmt.close();
    con.close();
  }

  /**
   * This method sets the attributes to use for storage.  The attributes
   * are used to set up the columns in a database table.
   *
   * @param attributes Attributes object containing  attributes and type info
   * @param attTypes Flattened hashtable version of Attributes
   */
  public void setAttributes(Attributes attributes, Hashtable attTypes) {
    attributeTypes = new Hashtable();
    for (Enumeration e = attTypes.keys(); e.hasMoreElements();) {
      String name = (String)e.nextElement();
      attributeTypes.put(name.replace(OLD_SEPARATOR,NEW_SEPARATOR), attTypes.get(name));
    }
    this.attributes = attributes;
  }
  
}
