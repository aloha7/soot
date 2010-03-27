/*
 * Copyright (c) 1999, 2000, 2001 University of Washington, Department of
 * Computer Science and Engineering.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided
 * with the distribution.
 *
 * 3. Neither name of the University of Washington, Department of
 * Computer Science and Engineering nor the names of its contributors
 * may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * REGENTS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package one.world.io;

import one.world.io.*;
import one.world.core.*;
import one.util.Guid;

import one.util.Bug;

/**
 * <p>Implementation of a tuple filter, used to process queries.</p>  
 *
 * </p>The filter constructs itself based on a one.world.io.Query 
 * tuple.  One may check if arbitrary one.world.Tuples can pass though
 * the filter.<p>
 *
 * @version  $Revision: 1.12 $
 * @author   Ben Hendrickson 
 * @author   Eric Lemar 
 */

public class TupleFilter {
  /** The top node in the filter tree.*/
  Node top;

  /** 
   * If the filter can only match tuples with a single Guid,
   * this field holds that Guid.  If not(or if we couldn't 
   * determine this is the case) this field is null.  
   *
   * Used by TupleStore to speed Guid queries using.  
   */
  Guid matchGuid;

  private static class FieldInfo {
    /** The object stored in this field */
    public Object obj;
 
    /** The declared type of the field */
    public Class  fieldType;

    /** The declared type of the field */
    public Class  actualType;

    public FieldInfo(Object obj,Class actualType,Class fieldType) {
      this.obj        = obj;
      this.fieldType  = fieldType;
      this.actualType = actualType;
    }

    public FieldInfo() {
    }

    /** 
     * Given a tuple and a field name, create a FieldInfo structure
     * Describing that field in the Tuple.
     *
     * @param t The tuple to look at
     * @param fieldname The field to investigate
     * @return The FieldInfo structure representing this field.  Null
     *         If no such field.  The obj and actualType members will
     *         be null if the field is null.
     */
    public static FieldInfo matchFieldInfo(Tuple t,String fieldPath[]) {
      FieldInfo fieldInfo;
      Class fieldType;
      Class actualType;
      

      if (fieldPath.length == 0) {
        fieldInfo = new FieldInfo(t,t.getClass(),t.getClass());        
      } else {
        int i;
        Tuple newTuple = t;

        for (i = 0; i < (fieldPath.length-1); i++) {
          if (newTuple.hasField(fieldPath[i])) {
            Object child = newTuple.get(fieldPath[i]);
            try {
              newTuple = (Tuple)child;
            } catch (ClassCastException x) {
              return null;
            }
          }
        }

        if (newTuple.hasField(fieldPath[fieldPath.length-1])) {
          Object obj;

          obj       = newTuple.get(fieldPath[fieldPath.length-1]);          
          fieldType = newTuple.getType(fieldPath[fieldPath.length-1]);

          if (obj == null) {
            actualType = null;
          } else {
            if (fieldType.isPrimitive()) {
              actualType = fieldType;
            } else {
              actualType = obj.getClass();
            }
          }
 
          fieldInfo = new FieldInfo(obj,actualType,fieldType);  
        } else {
          return null;
        }
      } 
      return fieldInfo;
    }
  }

  /**
   * Create a new filter for tuples based on a query request.
   * 
   * @param    query  Query that filter should implement.
   */
  public TupleFilter(Query query) {
    try {
      top       = buildBranch(query);
      matchGuid = top.nodeMatchGuid;
    } catch (IllegalArgumentException x) {
      //Just to speed up searchs if it can't possibly match 
      matchGuid = new Guid();
      top       = null;
    }
  }
  
  /**
   * Recursively constructs a filter tree of nodes from a Query 
   * object.
   *
   * @param    query   Query to convert to a tree
   * @return           The top node of the newly constructed tree.
   * @throws IllegalArgumentException
   */
  static Node buildBranch(Query query) throws IllegalArgumentException {
    Node node;

    switch (query.type) {
    case Query.TYPE_EMPTY:
      return new EmptyNode();
      
    case Query.TYPE_UNARY:
      node = new UnaryNot(buildBranch(query.query1));
      return node;

    case Query.TYPE_BINARY:
      switch (query.op) {
      case Query.BINARY_AND:

        node = new BinaryAnd(buildBranch(query.query1), 
                             buildBranch(query.query2));
        return node; 
      case Query.BINARY_OR:
        node = new BinaryOr(buildBranch(query.query1), 
                            buildBranch(query.query2));
        return node;
      }

    case Query.TYPE_COMPARE:
      {
        int position;
        MatchableBase match = null;
/*
        position = query.field.indexOf(".");
          
        if (position != -1) {
          throw new IllegalArgumentException("Sorry, nested queries are not supported.");
        }
*/
        if (query.value instanceof Double || 
            query.value instanceof Float) {
          switch(query.op) {
            case Query.COMPARE_EQUAL:
            case Query.COMPARE_NOT_EQUAL:
            case Query.COMPARE_BEGINS_WITH:
            case Query.COMPARE_ENDS_WITH:
            case Query.COMPARE_CONTAINS:
            case Query.COMPARE_GREATER_EQUAL:
            case Query.COMPARE_GREATER:
            case Query.COMPARE_LESS_EQUAL:
            case Query.COMPARE_LESS:
              match = new MatchableDouble(query.value);
              break;
            default:  
              throw new IllegalArgumentException("Illegal Comparison type for argument");
          } 

        } else if (query.value instanceof Integer || 
                   query.value instanceof Long || 
                   query.value instanceof Short ||
                   query.value instanceof Character) {

          switch(query.op) {
            case Query.COMPARE_EQUAL:
            case Query.COMPARE_NOT_EQUAL:
            case Query.COMPARE_BEGINS_WITH:
            case Query.COMPARE_ENDS_WITH:
            case Query.COMPARE_CONTAINS:
            case Query.COMPARE_GREATER_EQUAL:
            case Query.COMPARE_GREATER:
            case Query.COMPARE_LESS_EQUAL:
            case Query.COMPARE_LESS:
              match = new  MatchableLong(query.value);
              break;
            default:  
              throw new IllegalArgumentException("Illegal Comparison type for argument");
          } 
        } else if (query.value instanceof String) {
          switch(query.op) {
            case Query.COMPARE_EQUAL:
            case Query.COMPARE_NOT_EQUAL:
            case Query.COMPARE_BEGINS_WITH:
            case Query.COMPARE_ENDS_WITH:
            case Query.COMPARE_CONTAINS:
            case Query.COMPARE_GREATER_EQUAL:
            case Query.COMPARE_GREATER:
            case Query.COMPARE_LESS_EQUAL:
            case Query.COMPARE_LESS:
              match = new MatchableString(query.value);
              break;
            default:  
              throw new IllegalArgumentException("Illegal Comparison type for argument");
          } 
        } else if (query.value instanceof Guid) {
          switch(query.op) {
            case Query.COMPARE_EQUAL:
            case Query.COMPARE_NOT_EQUAL:
              match = new MatchableGuid(query.value);
              break;
            default:  
              throw new IllegalArgumentException("Illegal Comparison type for argument");
          } 

          match = new MatchableGuid(query.value);
        } else if (query.value instanceof byte[]) {
          switch(query.op) {
            case Query.COMPARE_EQUAL:
            case Query.COMPARE_NOT_EQUAL:
            case Query.COMPARE_BEGINS_WITH:
            case Query.COMPARE_ENDS_WITH:
            case Query.COMPARE_CONTAINS:
            case Query.COMPARE_GREATER_EQUAL:
            case Query.COMPARE_GREATER:
            case Query.COMPARE_LESS_EQUAL:
            case Query.COMPARE_LESS:
              match = new MatchableByteArray(query.value);
              break;
            default:  
              throw new IllegalArgumentException("Illegal Comparison type for argument");
          } 
        } else if (query.value instanceof Boolean) {
          switch(query.op) {
            case Query.COMPARE_EQUAL:
            case Query.COMPARE_NOT_EQUAL:
              match = new MatchableBoolean(query.value);
              break;
            default:  
              throw new IllegalArgumentException("Illegal Comparison type for argument");
          } 
        } else if (query.value == null) {
          switch(query.op) {
            case Query.COMPARE_EQUAL:
            case Query.COMPARE_NOT_EQUAL:
            case Query.COMPARE_HAS_FIELD:
              match = new MatchableObject(query.value);
              break;
            default:  
              throw new IllegalArgumentException("Illegal Comparison type for argument");
          } 
        } else {
          switch(query.op) {
            case Query.COMPARE_EQUAL:
            case Query.COMPARE_NOT_EQUAL:
              match = new MatchableObject(query.value);
              break;
            case Query.COMPARE_HAS_SUBTYPE:
            case Query.COMPARE_HAS_TYPE:
            case Query.COMPARE_HAS_DECLARED_TYPE:
              if (!(query.value instanceof Class)) {
                throw new IllegalArgumentException("Illegal Comparison type for argument");
              }
              break;
            default:  
              throw new IllegalArgumentException("Illegal Comparison type for argument");
          } 
        }
        
        switch (query.op) {
        case Query.COMPARE_EQUAL:
          node = new EqualsComparator(query.field, match);
          return node;
        case Query.COMPARE_NOT_EQUAL:
          return new NotEqualsComparator(query.field, match);
        
        case Query.COMPARE_GREATER:
          return new GreaterComparator(query.field, match);
        
        case Query.COMPARE_GREATER_EQUAL:
          return new GreaterOrEqualsComparator(query.field, match);
        
        case Query.COMPARE_LESS:
          return new LessComparator(query.field, match);
        
        case Query.COMPARE_LESS_EQUAL:
          return new LessOrEqualsComparator(query.field, match);
        
        case Query.COMPARE_BEGINS_WITH:
          return new BeginsWithComparator(query.field, 
                                          (MatchablePatternBase) match);
        
        case Query.COMPARE_CONTAINS:
          return new ContainsComparator(query.field, 
                                        (MatchablePatternBase) match);
        case Query.COMPARE_ENDS_WITH:
          return new EndsWithComparator(query.field, 
                                        (MatchablePatternBase) match);

        case Query.COMPARE_HAS_SUBTYPE:
          return new HasSubTypeComparator(query.field,(Class)query.value);
 
        case Query.COMPARE_HAS_TYPE:
          return new HasTypeComparator(query.field,(Class)query.value);
           
        case Query.COMPARE_HAS_DECLARED_TYPE:
          return new HasDeclaredTypeComparator(query.field,(Class)query.value);
        
        case Query.COMPARE_HAS_FIELD:
          return new HasFieldComparator(query.field);
        }
      }
    }
    throw new IllegalArgumentException("");
  }
  
  /** 
   * Tries passing the given tuple through the filter.  
   *
   * @param   t  Tuple to be run through the filter.
   * @return     True if the tuple passed though, otherwise false.
   */
  public boolean check(Tuple t)  {
    if (top == null) {
      return false;
    } else {
      return top.check(t);
    }
  }
  
  /** 
   * A node of a filter tree.
   */
  private static abstract class Node {
    Guid nodeMatchGuid;
    /**
     * Checks if a tuple passes this branch of the filter.  
     *
     * @param  t  Tuple to be run though this branch of the filter.
     * @return    True if the tuple passed though, otherwise false.
     */
    public abstract boolean check(Tuple t) ;
  }
  
  /**
   * A node that always returns true.  It ends a branch.
   */
  private static class EmptyNode extends Node {

    /** Constructs an empty node */
    public EmptyNode(){}
    
    /** Always returns true */
    public boolean check(Tuple t)  {
      return true;
    }
  }
  
  /**
   * A node that reverses the return value being passed up
   * a branch.  It extends a branch.
   */
  private static class UnaryNot extends Node {
    /* query to invert results from */
    Node lhs;
 
    /**
     * Constructs a node who's truth value is the opposite of the
     * branch extending from it.
     *
     * @param  lhs  Branch to invert the true or false value from. 
     */
    public UnaryNot(Node lhs){
      this.lhs = lhs;
    }
    
    /**
     * Checks if a tuple passes this branch of the filter.  
     *
     * @param  t  Tuple to be run though this branch of the filter.
     * @return    True if the tuple passed though, otherwise false.
     */
    public boolean check(Tuple t) {
      return (!lhs.check(t));
    }
  }


  /**
   * A node that performs the logical AND of two nodes.  It 
   * forks a branch.
   */
  private static class BinaryAnd extends Node { 
    /** First branch extending from this node. */
    Node lhs;
    
    /** Second branch extending from this node. */
    Node rhs;
    
    /**
     * Constructs a node which is true if and only if both branch extending
     * from it are true.
     *
     * @param  lhs  One branch for the and.
     * @param  rhs  Other branch for the and.
     */  
    public BinaryAnd(Node lhs, Node rhs) {
      this.lhs = lhs;
      this.rhs = rhs;

      if (lhs.nodeMatchGuid == null) {
        nodeMatchGuid   = rhs.nodeMatchGuid;
      } else if (rhs.nodeMatchGuid == null) {
        nodeMatchGuid   = lhs.nodeMatchGuid;
      } else {
        if (lhs.nodeMatchGuid.equals(rhs.nodeMatchGuid)) {
          nodeMatchGuid = lhs.nodeMatchGuid;
        }
      }
    }
    
    /**
     * Checks if a tuple passes this branch of the filter.  
     *
     * @param  t  Tuple to be run though this branch of the filter.
     * @return    True if the tuple passed though, otherwise false.
     */
    public boolean check(Tuple t)  {
      return (lhs.check(t) && rhs.check(t));
    }
  }
  
  /**
   * A node that performs the logical OR of two nodes.  It
   * forks a branch.
   */ 
  private static class BinaryOr extends Node {
    /** First branch extending from this node. */
    Node lhs;
    
    /** Second branch extending from this node. */
    Node rhs;
    
    /**
     * Constructs a node which is true if and only if the inclusive or
     * both branch is true.
     *
     * @param  lhs  One branch for the and.
     * @param  rhs  Other branch for the and.
     */
    public BinaryOr(Node lhs, Node rhs) {
      this.lhs = lhs;
      this.rhs = rhs;

      if (lhs.nodeMatchGuid==null) {
        nodeMatchGuid   = null;
      } else if (rhs.nodeMatchGuid==null) {
        nodeMatchGuid   = null;
      } else {
        if (lhs.nodeMatchGuid.equals(rhs.nodeMatchGuid)) {
          nodeMatchGuid = lhs.nodeMatchGuid;
        }
      }
      this.lhs = lhs;
      this.rhs = rhs;
    }

    /**
     * Checks if a tuple passes this branch of the filter.  
     *
     * @param  t  Tuple to be run though this branch of the filter.
     * @return    True if the tuple passed though, otherwise false.
     */ 
    public boolean check(Tuple t)  {    
      return (lhs.check(t) || rhs.check(t));
    }
  }

  /** 
   * LhsNullException is thrown by MatchableBases' compareWith
   * methods whenever a tuple has the correct name and type as a 
   * match but the value is null.  While this is the exceptional case, 
   * it may happen frequently enough speed is an issue.  As a 
   * result LhsNullException does not generate or record a stacktrace. 
   */
  private static class LhsNullException extends Exception {

    /** 
     * Create a new lhs is null exception.  
     */
    public LhsNullException () {
      // skip the traditional call to super - we want
      // speed, and don't care about the stacktrace.
    }
    // no need for constructor with detailed message.
  }

  /** 
   */
  private static class NoSuchFieldException extends Exception {

    /** 
     * Create a new lhs is null exception.  
     */
    public NoSuchFieldException () {
      // skip the traditional call to super - we want
      // speed, and don't care about the stacktrace.
    }
    // no need for constructor with detailed message.
  }


  
  /**
   * <p>Values to be matched against by the filter in any fashion are
   * represented as MatchableBase in the tree.</p>
   *
   * <p>Types which may be matched against each other must be
   * represented by the same MatchableBase.  By the same token, all
   * types represented by a MatchableBase are always implicitly
   * converted between.</p>
   *
   * <p>All matchable objects must be able to compare themselves to a
   * field on a tuple of any type they represent.  From this single
   * method, evaluating nodes are able to decided if >, >=, <, <=, or
   * == are met.</p>
   */
  static abstract class MatchableBase{

    /**
     * <p>Compares the value of this matchable object with the value of 
     * a type it represents contained in a field.</p>  
     *
     * <p> It should be implemented so it returns opposite what a call 
     * to compareTo would be for the values (or would logically be 
     * if the values were to implemented comparable).  This keeps the  
     * logic consistent with the MatchableBase being the right hand 
     * side. </p>
     *
     * <p> It is undefined what happens if field f reference to an a 
     * data type not handled by this MatchableBase.  Most likely 
     * a runtime exception of some sort.  The caller may assume a
     * true result from the method <code> verifyType </code> on the
     * same MatchableBase for the same field avoids this.
     *
     * @param    f  The field of the relevant value.
     * @param    t  The tuple which has the relevant field.
     * @return      Refer to above comments.
     */
    abstract int compareWith(FieldInfo f) throws LhsNullException;
    abstract boolean compareEqual(FieldInfo f) throws LhsNullException;
    
    /** 
     * Checks if the field f represents a object which this 
     * MatchableBase could handle.
     */
    abstract boolean verifyType(Class f);
  }

  /**
   * MatchableBases for which the notions of matching the beginning,
   * some arbitrary region inside, and the end of the value makes sense.
   *
   */
  static abstract class MatchablePatternBase extends MatchableBase {

    /**
     * What a stringified version of a different object with the same 
     * type as this would be.
     * 
     * @param    f  The field of the relevant value.
     * @param    t  The tuple which has the relevant field.
     * @return      String found
     */

    abstract boolean contains(FieldInfo f);
    abstract boolean startsWith(FieldInfo f);
    abstract boolean endsWith(FieldInfo f);
  }
  
  /** 
   * Wraps a string as a MatchablePatternBase.
   */
  private static class MatchableString extends MatchablePatternBase{
    String rhs;
    
    /** 
     * Constructs a new MatchableString object.
     *
     * @param    o  String which to wrap.  
     */
    MatchableString (Object o) {
      rhs = (String) o;
    }
    
    /** 
     * Compares this string with the one referred to by the parameters.
     * This method conforms to the contract given for compareWith()
     * of MatchableBase.
     *
     * @param    f  The field of the relevant value.
     * @param    t  The tuple which has the relevant field.
     * @return      Refer to above comments.
     */
    int compareWith(FieldInfo f) throws LhsNullException{
      if (f.obj == null) {
        throw new LhsNullException();
      }
      String lhs = (String)f.obj;
      return -rhs.compareTo(lhs);
    }

    boolean compareEqual(FieldInfo f) throws LhsNullException{
      return 0 == compareWith(f);
    }

    boolean startsWith(FieldInfo f)
    {
      if ((rhs == null) || (f.obj == null)) {
        return false;
      }
      String lhs = (String)f.obj;
      return lhs.startsWith(rhs);
    }

    boolean endsWith(FieldInfo f)
    {
      if ((rhs == null) || (f.obj == null)) {
        return false;
      }
      String lhs = (String)f.obj;
      return lhs.endsWith(rhs);
    }

    boolean contains(FieldInfo f)
    {
      if ((rhs == null) || (f.obj == null)) {
        return false;
      }
      String lhs  = (String)f.obj;
      return (-1  != lhs.indexOf(rhs));
    }

    boolean verifyType(Class f) {
      return (f == rhs.getClass());
    }
  }
  
  /**
   * Wraps various sizes of integers and chars as a MatchableBases.
   */
  private static class MatchableLong extends MatchableBase{
    long rhs;

    /**
     * Constructs a new MatchableLong object.
     *
     * @param    o  Object to match against.
     */
    MatchableLong(Object o) {
      if (o instanceof Character) {
        //        rhs = Character.getNumericValue(((Character)o).charValue());
        rhs = (long)((Character)o).charValue();
      } else {
        rhs = ((Number)o).longValue();  // XXX and if not?
      }
    }
    
    boolean verifyType(Class f) {
      //  return true;
      
      return (f == Long.TYPE ||
              f == Integer.TYPE ||
              f == Short.TYPE || 
              f == Character.TYPE ||
              f == Long.class ||
              f == Integer.class ||
              f == Short.class || 
              f == Character.class);
    
    }  

    /**
     * Compares this object with the one referred to by the parameters.
     * This method conforms to the contract given for compareWith()
     * of MatchableBase.
     *
     * @param    f  The field of the relevant value.
     * @param    t  The tuple which has the relevant field.
     * @return      Result of comparison.
     */
    int compareWith(FieldInfo f) throws LhsNullException {
      Long lhs;

      if (f.obj==null) {
        throw new LhsNullException();
      }

      if (f.obj instanceof Character) {
        lhs = new Long((long)((Character)f.obj).charValue());
      } else {
        lhs = new Long(((Number)f.obj).longValue());  
      }

      return -(new Long(rhs)).compareTo(lhs);
    }
    boolean compareEqual(FieldInfo f) throws LhsNullException{
      return 0 == compareWith(f);
    }
  }
  
  
  /**
   * Wraps various sizes of floating point numbers as a MatchableBase.
   */
  private static class MatchableDouble extends MatchableBase{
    double rhs;
    
    boolean verifyType(Class f)  {
      return (f == Float.TYPE ||
              f == Double.TYPE ||
              f == Double.class ||
              f == Float.class);
    }
    
    /**
     * Constructs a new MatchableDouble
     *
     * @param   o  Value to be.
     */
    MatchableDouble(Object o) {
      rhs = ((Number)o).doubleValue(); 
    }

    /** */
    int compareWith(FieldInfo f) throws LhsNullException {
      if (f.obj == null) {
        throw new LhsNullException();
      }
      return -(new Double(rhs)).compareTo( new Double(((Number)f.obj).doubleValue()));
    }
    boolean compareEqual(FieldInfo f) throws LhsNullException{
      return 0 == compareWith(f);
    }
  }

  /**
   * Wraps any object or primitive type.
   */
  private static class MatchableObject extends MatchableBase{
    Object rhs;
    
    boolean verifyType(Class f)  {
      return true;
    }
    
    /**
     * Constructs a new MatchableObject
     *
     * @param   o  Value to be.
     */
    MatchableObject(Object o) {
      rhs = o; 
    }

    /** */
    int compareWith(FieldInfo f) throws LhsNullException {
      throw new Bug("Cannot call compareWith on MatchableObject");
    }
    boolean compareEqual(FieldInfo f) throws LhsNullException{
      try {
        if (rhs == null) {
          return (null == f.obj);
        } 

        return rhs.equals(f.obj);
      } catch (Exception x) {
        return false;
      }
    }
  }
  

  
  /**
   * Wraps a byte[] as a MatchableBase
   */
  private static class MatchableByteArray extends MatchablePatternBase{
    byte[] rhs;

    /**
     * Constructs a new MatchableByteArray
     *
     * @param   o  Value to be.
     */
    MatchableByteArray(Object o) {
      rhs = (byte[])o;
    }

    /** */
    boolean verifyType(Class f)  {
      if (f == null) {
        return false;
      }
      return (f.isArray() && f.getComponentType() == Byte.TYPE);
    }

    /**
     * Compares this object with the one referred to by the parameters.
     * This method conforms to the contract given for compareWith()
     * of MatchableBase.
     *
     * @param    f  The field of the relevant value.
     * @param    t  The tuple which has the relevant field.
     * @return      Result of comparison.
     */
    int compareWith(FieldInfo f) throws LhsNullException{
      byte[] lhs;

      lhs = (byte[])f.obj;

      if (lhs == null) {
        throw new LhsNullException();
      }
      
      // rest of method verbatim from SerializedTupleFilter.
      int minLength = Math.min(lhs.length, rhs.length);
     
      for (int i = 0; i < minLength; i++) {
        int comp = rhs[i] - lhs[i];
        if (comp != 0) {
          return comp;
        }
      }
      return rhs.length-lhs.length;  //XXX order right?  
    }
    boolean compareEqual(FieldInfo f) throws LhsNullException{
      return (0 == compareWith(f));
    }
    
    boolean startsWith(FieldInfo f) {
      int i;

      if (f.obj == null)
        return false;

      byte[] lhs = (byte[])f.obj;

      if (lhs.length<rhs.length)
        return false;      

      for (i = 0; i < rhs.length; i++) {
        if (rhs[i] != lhs[i])
          return false;
      }
      return true;
    }
    boolean endsWith(FieldInfo f) {
      int i;

      if (f.obj == null)
        return false;

      byte[] lhs = (byte[])f.obj;

      if (rhs.length > lhs.length)
        return false;      

      for (i = 0; i < rhs.length; i++) {
        if (rhs[rhs.length-i-1] != lhs[lhs.length-i-1])
          return false;
      }
      return true;
    }
    boolean contains(FieldInfo f) {
      //Should use a more efficient algorithm(sort of a
      //silly comparison anyway)
      int i,j;
      boolean matched;

      if (f.obj == null)
        return false;

      byte[] lhs = (byte[])f.obj;

      if (rhs.length > lhs.length)
        return false;      

      for (i = 0; i < (lhs.length-rhs.length+1); i++) {
        matched = true;
        for (j = 0; j < rhs.length; j++) {
          if (lhs[i+j] != rhs[j]) {
            matched = false;
            break;
          }
        }
        if (matched) {
          return true;
        }
      }
      return false;
    }
  }

  /**
   * Wraps a Guid as a MatchableBase
   */
  private static class MatchableGuid extends MatchableBase{
    Guid rhs;
    MatchableGuid(Object o) {
      rhs = (Guid)o;
    }

   /** */
    boolean verifyType(Class f) {
      return (f == rhs.getClass());
    } 

    /**
     * Compares this object with the one referred to by the parameters.
     * This method conforms to the contract given for compareWith()
     * of MatchableBase.
     *
     * @param    f  The field of the relevant value.
     * @param    t  The tuple which has the relevant field.
     * @return      Result of comparison.
     */ 
    int compareWith(FieldInfo f) throws LhsNullException{
      Guid lhs;

      lhs  = (Guid)f.obj;        
      if (lhs == null) {
        throw new LhsNullException();
      }
      
      int modifier = 1;
      if ((lhs.getHigh() > 0) || (rhs.getHigh() > 0)) {
        modifier = -1;
      }

      int comp = (new Long(lhs.getHigh())).compareTo(new Long(rhs.getHigh()));

      if (comp != 0) {
        return comp*modifier;
      }

      modifier = 1;

      if (lhs.getLow()>0 || rhs.getLow()>0) {
        modifier = -1;
      }
   
      return (new Long(lhs.getLow()).compareTo(new Long(lhs.getLow())) 
        * modifier);
    }
    boolean compareEqual(FieldInfo f) throws LhsNullException{
      return 0 == compareWith(f);
    }
  }

  /**
   * Wraps a boolean as a MatchableBase
   */
  private static class MatchableBoolean extends MatchableBase{
    boolean rhs;

    /** 
     * Constructs a MatchableBoolean object. 
     *
     * @param   o  Value to be.
     */
    MatchableBoolean(Object o) {
      rhs = ((Boolean)o).booleanValue();
    }

    /** */
    boolean verifyType(Class f) {
      return (f == Boolean.TYPE ||
              f == Boolean.class);
    }

    /**
     * Compares this object with the one referred to by the parameters.
     * This method conforms to the contract given for compareWith()
     * of MatchableBase.
     *
     * @param    f  The field of the relevant value.
     * @param    t  The tuple which has the relevant field.
     * @return      Result of comparison.
     */ 
    int compareWith(FieldInfo f) throws LhsNullException{
      throw new Bug("Attempted compareWith on boolean");
    }

    boolean compareEqual(FieldInfo f) throws LhsNullException {
      boolean lhs;
      if (f.obj == null) {
        throw new LhsNullException();
      }
      lhs = ((Boolean)f.obj).equals(Boolean.TRUE);
    
      return (rhs==lhs); 
    }
  }
  
  /** 
   * The abstract static class representation of a comparison of two objects.
   * Usually implemention's constructors will take a Matchable Object
   * that serves as the value to use as the right hand side of the 
   * Comparison test, though not always.
   */
  private abstract static class Comparer extends Node{
    String fieldname;
    String fieldPath[];

    FieldInfo getFieldInfo(Tuple t) {
      return FieldInfo.matchFieldInfo(t,fieldPath);
    }

    Comparer(String fieldname) {
      int numTok;
      java.util.StringTokenizer stok;
      int i;

      this.fieldname = fieldname;

      if (fieldname.equals("")) {
        fieldPath = new String[0];
      } else {
        if (-1 == fieldname.indexOf('.')) {
          fieldPath    = new String[1];
          fieldPath[0] = fieldname;
        } else {
          stok   = new java.util.StringTokenizer(fieldname,".");
          numTok = stok.countTokens(); 
        
          fieldPath = new String[numTok];
          for (i = 0; i < fieldPath.length; i++) {
            try {
              fieldPath[i] = stok.nextToken();   
            } catch (java.util.NoSuchElementException x) {
              throw new Bug(""+x);
            }
          }
        }
      }
    }
  }

  /**
   * Represents an equals comparison in the filter tree.  
   */
  private static class EqualsComparator extends Comparer{
    MatchableBase match;
    
    /**
     * Constructs a new equals comparison node for the filter tree.
     *
     * @param    fieldname      The fieldname of the value to use as 
     *                          the left hand side of the equality check.
     * @param    objectToMatch  The object to use as the right hand 
     *                          side the equality checks.
     */
    public EqualsComparator(String fieldname, MatchableBase objectToMatch) {
      super(fieldname);
      match = objectToMatch;

      if (fieldname.equals("id")) {
        if (objectToMatch instanceof MatchableGuid) {
          MatchableGuid mg = (MatchableGuid)objectToMatch;
          nodeMatchGuid    = mg.rhs;
        }
      }
    }
    
    /**
     * Performs the equality check.
     *
     * @param   t  The tuple to retrieve the left hand side value from.
     * @return     If the comparison was true.
     */
    public boolean check(Tuple t)  {
      FieldInfo fieldInfo = getFieldInfo(t);

      if (fieldInfo == null) {
        return false;
      }

      try {
        if (((fieldInfo.obj == null) || 
            match.verifyType(fieldInfo.actualType)) && 
            (match.compareEqual(fieldInfo))) {
          return true;
        }
      } catch (LhsNullException e) {}
      return false;
    }
  }
  
  /**
   * Represents a not equals comparison in the filter tree.  This is 
   * logical different than a unary not node connected to a equals 
   * node in such that a NotEqualsComparator will return false if 
   * the intended field doesn't exist.
   */
  private static class NotEqualsComparator extends Comparer{
    MatchableBase match;
    
    /**
     * Constructs a new not equals comparison node for the filter tree.
     *
     * @param    fieldname      The fieldname of the value to use as the 
     *                          left hand side of the equality check.
     * @param    objectToMatch  The object to use as the right hand side 
     *                          the equality checks.
     */
    public NotEqualsComparator(String fieldname, 
                               MatchableBase objectToMatch){
      super(fieldname); 
      match = objectToMatch;
    } 
    
    /**
     * Performs the not equal check.
     *
     * @param   t  The tuple to retrieve the left hand side value from.
     * @return     If the comparison was true.
     */
    public boolean check(Tuple t)  {
      FieldInfo fieldInfo = getFieldInfo(t);

      if (fieldInfo == null) {
        return true;
      }

      try {
        if (((fieldInfo.obj == null) || 
            match.verifyType(fieldInfo.actualType)) && 
            (match.compareEqual(fieldInfo))) {
          return false;
        }
      } catch (LhsNullException e) {}

      return true;
    }
  }
  
  /**
   * Represents a less than comparison in the filter tree.
   */
  private static class LessComparator extends Comparer {
    MatchableBase match;
    
    /**
     * Constructs a new less than comparison node for the filter tree.
     *
     * @param    fieldname      The fieldname of the value to use as the 
     *                          left hand side of the equality check.
     * @param    objectToMatch  The object to use as the right hand side 
     *                          the equality checks.
     */
    public LessComparator(String fieldname, MatchableBase objectToMatch) {
      super(fieldname); 
      match = objectToMatch;
    } 

    /**
     * Performs the less than comparison check.
     *
     * @param   t  The tuple to retrieve the left hand side value from.
     * @return     If the comparison was true.
     */
    public boolean check(Tuple t)  {
      FieldInfo fieldInfo = getFieldInfo(t);

      if ((fieldInfo == null) || (fieldInfo.obj == null)) {
        return false;
      }

      try {
        if ((match.verifyType(fieldInfo.actualType)) && 
            (match.compareWith(fieldInfo) < 0)) {
          return true;
        }
      } catch (LhsNullException e) {}

      return false;
    }
  }
  
  /** 
   * Represents a less than or equal to comparison node in the filter tree.
   */
  private static class LessOrEqualsComparator extends Comparer{
    MatchableBase match;
    
    /**
     * Constructs a new less than comparison node for the filter tree.
     *
     * @param    fieldname      The fieldname of the value to use as 
     *                          the left hand side of the equality check.
     * @param    objectToMatch  The object to use as the right hand side 
     *                          the equality checks.
     */    
    public LessOrEqualsComparator(String fieldname, 
                                  MatchableBase objectToMatch) {
      super(fieldname);
      match = objectToMatch;
    } 

    /**
     * Performs the less than or equal to comparison check.
     *
     * @param   t  The tuple to retrieve the left hand side value from.
     * @return     If the comparison was true.
     */  
    public boolean check(Tuple t)  {
      FieldInfo fieldInfo = getFieldInfo(t);

      if ((fieldInfo == null) || (fieldInfo.obj == null)) {
        return false;
      }

      try {
        if ((match.verifyType(fieldInfo.actualType)) && 
            (match.compareWith(fieldInfo) <= 0)) {
          return true;
        }
      } catch (LhsNullException e) {}

      return false;
    }
  }
  
  /**
   *  Represents a greater than comparison node in the filter tree.
   */
  private static class GreaterComparator extends Comparer{
    MatchableBase match;
    
    /**
     * Constructs a grater than comparison node for the filter tree.
     *
     * @param    fieldname      The fieldname of the value to use as 
     *                          the left hand side of the equality check.
     * @param    objectToMatch  The object to use as the right hand side 
     *                          the equality checks.
     */    
    public GreaterComparator(String fieldname, 
                             MatchableBase objectToMatch) {
      super(fieldname);
      match = objectToMatch;
    } 

    /**
     * Performs the greater than comparison check.
     *
     * @param   t  The tuple to retrieve the left hand side value from.
     * @return     If the comparison was true.
     */  
    public boolean check(Tuple t)  {
      FieldInfo fieldInfo = getFieldInfo(t);

      if ((fieldInfo == null) || (fieldInfo.obj == null)) {
        return false;
      }

      try {
        if ((match.verifyType(fieldInfo.actualType)) && 
            (match.compareWith(fieldInfo) > 0)) {
          return true;
        }
      } catch (LhsNullException e) {}

      return false;
    }
  }

  /**
   * Represents a Has Type comparison node.
   */
  private static class HasTypeComparator extends Comparer{
    Class match;
    
    /**
     * Constructs a Has Type node for the filter tree.
     *
     * @param    fieldname      The field whose type we are checking
     *                          
     * @param    objectToMatch  Class object for the type we are checking  
     *                          
     */    
    public HasTypeComparator(String fieldname, 
                             Class match) {
      super(fieldname);
      this.match = match;
    } 

    /**
     * Performs the Has Type check.
     *
     * @param   t  The tuple to retrieve the field from.
     * @return     If the comparison was true.
     */  
    public boolean check(Tuple t)  {
      FieldInfo fieldInfo= getFieldInfo(t);

      if ((fieldInfo == null) || (fieldInfo.obj == null)) {
        return false;
      }

      return (fieldInfo.actualType).equals(match);
    }
  }

  /**
   * Represents a Has Subype comparison node.
   */
  private static class HasSubTypeComparator extends Comparer{
    Class match;
    
    /**
     * Constructs a Has Subype node for the filter tree.
     *
     * @param    fieldname      The field whose subtype we are checking
     *                          
     * @param    objectToMatch  Class object for the type we are checking  
     *                          
     */    
    public HasSubTypeComparator(String fieldname, 
                             Class match) {
      super(fieldname);
      this.match = match;
    } 

    /**
     * Performs the Has Type check.
     *
     * @param   t  The tuple to retrieve the field from.
     * @return     If the comparison was true.
     */  
    public boolean check(Tuple t)  {
      FieldInfo fieldInfo = getFieldInfo(t);

      if ((fieldInfo == null) || (fieldInfo.obj == null)) {
        return false;
      }

      return match.isAssignableFrom(fieldInfo.actualType);
    }
  }

  /**
   * Represents a Has Type comparison node.
   */
  private static class HasDeclaredTypeComparator extends Comparer{
    Class match;
    
    /**
     * Constructs a Has Type node for the filter tree.
     *
     * @param    fieldname      The field whose type we are checking
     *                          
     * @param    objectToMatch  Class object for the type we are checking  
     *                          
     */    
    public HasDeclaredTypeComparator(String fieldname, 
                             Class match) {
      super(fieldname);
      this.match = match;
    } 

    /**
     * Performs the Has Type check.
     *
     * @param   t  The tuple to retrieve the field from.
     * @return     If the comparison was true.
     */  
    public boolean check(Tuple t)  {
      FieldInfo fieldInfo= getFieldInfo(t);

      if ((fieldInfo == null) || (fieldInfo.fieldType == null)) {
        return false;
      }

      return (fieldInfo.fieldType).equals(match);
    }
  }

  /**
   * Represents a Has Field comparison node.
   */
  private static class HasFieldComparator extends Comparer{
    /**
     * Constructs a Has Type node for the filter tree.
     *
     * @param    fieldname      The field whose type we are checking
     */    
    public HasFieldComparator(String fieldname) {
      super(fieldname);
    } 

    /**
     * Performs the Has Type check.
     *
     * @param   t  The tuple to retrieve the field from.
     * @return     If the comparison was true.
     */  
    public boolean check(Tuple t)  {
      return (null != getFieldInfo(t));
    }
  }

  /**
   * Represents a greater than or equal to comparison node in the filter tree.
   */
  private static class GreaterOrEqualsComparator extends Comparer{
    MatchableBase match;
    
    /**
     * Constructs a greater than or equal to comparison node for the filter tree.
     *
     * @param    fieldname      The fieldname of the value to use as the 
     *                          left hand
     *                          side of the equality check.
     * @param    objectToMatch  The object to use as the right hand side 
     *                          the equality
     *                          checks.
     */    
    public GreaterOrEqualsComparator(String fieldname, 
                                     MatchableBase objectToMatch) {
      super(fieldname);
      match = objectToMatch;
    } 
    
    /** 
     * Performs the greater than or equal to comparison check.
     *
     * @param   t  The tuple to retrieve the left hand side value from.
     * @return     If the comparison was true.
     */      
    public boolean check(Tuple t)  {
      FieldInfo fieldInfo = getFieldInfo(t);
      if ((fieldInfo == null) || (fieldInfo.obj == null)) {
        return false;
      }
      try {
        if (match.verifyType(fieldInfo.actualType) && 
            (match.compareWith(fieldInfo) >= 0)) {
          return true;
        }
      } catch (LhsNullException e) {}
      return false;
    }
  }
  
  /**
   * Represents a "does it begin with" verification node in the filter tree.
   */
  private static class BeginsWithComparator extends Comparer{
    MatchablePatternBase match;
    
    /**
     * Constructs a "does it begin with" verification node for the
     * filter tree.
     *
     * @param fieldname     The fieldname of the value to use as the left
     *                      hand side of the equality check.
     * @param objectToMatch The object to use as the right hand side
     *                      the equality checks.  
     */
   public BeginsWithComparator(String fieldname, 
                               MatchablePatternBase objectToMatch) {
      super(fieldname);
      match = objectToMatch;
    } 

    /**
     * Performs the "does it begin with" check. 
     *
     * @param   t  The tuple to retrieve the left hand side value from.
     * @return     If the comparison was true.
     */  
    public boolean check (Tuple t)  {
      FieldInfo fieldInfo = getFieldInfo(t);

      if ((fieldInfo == null) || !match.verifyType(fieldInfo.actualType)) 
        return false;

      return match.startsWith(fieldInfo);
    }
  }
  
  /**
   * Represents a "does it contain" verification node in the filter tree.
   */
  private static class ContainsComparator extends Comparer{
    MatchablePatternBase match;
    
    /**
     * Constructs a "does it contain" verification node for the filter tree.
     *
     * @param fieldname     The fieldname of the value to use as the left
     *                      hand side of the equality check.
     * @param objectToMatch The object to use as the right hand side
     *                      the equality checks.  
     */
    public ContainsComparator(String fieldname, 
                              MatchablePatternBase objectToMatch) { 
      super(fieldname);
      match = objectToMatch;
    }

    /**
     * Performs the "does it contain" check. 
     *
     * @param   t  The tuple to retrieve the left hand side value from.
     * @return     If the comparison was true.
     */     
    public boolean check (Tuple t)  {
      FieldInfo fieldInfo= getFieldInfo(t);

      if ((fieldInfo == null) || !match.verifyType(fieldInfo.actualType)) {
        return false;
      }

      return match.contains(fieldInfo);
    }
  }

  /**
   * Represents a "does it end with" verification node in the filter tree.
   */
  private static class EndsWithComparator extends Comparer{
    MatchablePatternBase match;

    /**
     * Constructs a "does it end with" verification node for the filter tree.
     *
     * @param fieldname     The fieldname of the value to use as the left
     *                      hand side of the equality check.
     * @param objectToMatch The object to use as the right hand side
     *                      the equality checks.  
     */

    public EndsWithComparator(String fieldname, 
                              MatchablePatternBase objectToMatch) {
      super(fieldname);

      match = objectToMatch;
    } 
    
    /**
     * Performs the "does it end with" check. 
     *
     * @param   t  The tuple to retrieve the left hand side value from.
     * @return     If the comparison was true.
     */    
    public boolean check (Tuple t)  {
      FieldInfo fieldInfo= getFieldInfo(t);

      if ((fieldInfo == null) || !match.verifyType(fieldInfo.actualType)) {
        return false;
      }

      return match.endsWith(fieldInfo);
    }
  }
}



