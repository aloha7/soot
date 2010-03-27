/*
 * Copyright (c) 2001, University of Washington, Department of
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

import one.world.core.InvalidTupleException;
import one.world.core.Tuple;
import one.world.core.TupleException;

/**
 * Implementation of a query. Queries are used to select specific
 * tuples when performing input operations. Queries are either empty,
 * unary, binary, or comparison queries. Empty queries are always
 * true. Unary queries apply a unary operator on another query. Binary
 * queries apply a binary operator on two other queries. Finally,
 * comparison queries compare the field of a tuple to a value. The
 * field of a tuple is specified as a string, giving the field's
 * name. The tuple itself can be specified by using the empty
 * string. Nested fields of nested tuples can be specified by using
 * the corresponding chain of field names, where the individual field
 * names are separated by dots ('<code>.</code>').
 * 
 * <p>Whether a comparison query is valid for a given field, depends
 * on the type of the field. For the tuple itself, only queries for
 * its type, subtype, or declared type are valid. For arbitrary
 * objects, only queries for the type, subtype, declared type,
 * equality, or inequality are valid. For numeric types, either in
 * their primitive or in their boxed form, all comparisons besides
 * begins-with, contains, ends-with are valid. Finally, for strings
 * and binary data, all comparisons are valid.</p>
 *
 * @version   $Revision: 1.11 $
 * @author    Robert Grimm
 */
public class Query extends Tuple {

  /** The serial version ID for this class. */
  static final long serialVersionUID = -6453421205458074025L;

  /** The empty type. */
  public static final int TYPE_EMPTY                =  1;

  /** The unary type. */
  public static final int TYPE_UNARY                =  2;

  /** The binary type. */
  public static final int TYPE_BINARY               =  3;

  /** The comparison type. */
  public static final int TYPE_COMPARE              =  4;
  
  /** The not unary operator. */
  public static final int UNARY_NOT                 = 10;
  
  /** The and binary operator. */
  public static final int BINARY_AND                = 20;
  
  /** The or binary operator. */
  public static final int BINARY_OR                 = 21;
  
  /** The equal comparison operator. */
  public static final int COMPARE_EQUAL             = 30;
  
  /** The not equal comparison operator. */
  public static final int COMPARE_NOT_EQUAL         = 31;
  
  /** The greater than comparison operator. */
  public static final int COMPARE_GREATER           = 32;
  
  /** The greater equal comparison operator. */
  public static final int COMPARE_GREATER_EQUAL     = 33;
  
  /** The less than comparison operator. */
  public static final int COMPARE_LESS              = 34;
  
  /** The less equal comparison operator. */
  public static final int COMPARE_LESS_EQUAL        = 35;
  
  /** The begins with comparison operator. */
  public static final int COMPARE_BEGINS_WITH       = 36;
  
  /** The contains comparison operator. */
  public static final int COMPARE_CONTAINS          = 37;
  
  /** The ends with comparison operator. */
  public static final int COMPARE_ENDS_WITH         = 38;
  
  
  /** 
   * The has type comparison operator. Has type comparisons compare
   * the actual value of a tuple or field with the specified type.
   */
  public static final int COMPARE_HAS_TYPE          = 39;

  /** 
   * The has subtype comparison operator. Has subtype comparisons
   * compare the actual value of a tuple or field with the specified
   * type.
   */
  public static final int COMPARE_HAS_SUBTYPE       = 40;

  /**
   * The has declared type comparison operation. Has declared type
   * comparisons compare the declared type of a field with the
   * specified type. The declared type of a tuple is the type of the
   * tuple. The declared type of the dynamically typed fields of a
   * dynamic tuple is <code>java.lang.Object</code>.
   */
  public static final int COMPARE_HAS_DECLARED_TYPE = 41;

  /** 
   * The has field comparison operator. Has field comparisons return
   * true if the tuple has a field with the given fieldname.  The
   * value parameter is unused and should be set to <code>null</code>.
   */
  public static final int COMPARE_HAS_FIELD         = 42;

  /**
   * The type of this query.
   *
   * @serial  Must be one of the types defined by this class.
   */
  public int type;

  /**
   * The operator for unary, binary, and comparison queries.
   *
   * @serial  Must be a valid unary operator for unary queries,
   *          a valid binary operator for binary queries, and
   *          a valid comparison operator for comparison queries.
   */
  public int op;

  /**
   * The first query for unary and binary queries.
   *
   * @serial  Must not be <code>null</code> for unary and binary
   *          queries.
   */
  public Query query1;

  /**
   * The second query for binary queries.
   *
   * @serial  Must not be <code>null</code> for binary queries.
   */
  public Query query2;

  /**
   * The field specifier for comparison queries.
   *
   * @serial  Must not be <code>null</code> for comparison queries.
   */
  public String field;

  /**
   * The value for comparison queries.
   *
   * @serial
   */
  public Object value;

  /**
   * Create a new empty query.
   */
  public Query() {
    type = TYPE_EMPTY;
  }
 
  /**
   * Create a new unary query.
   *
   * @param   op      The unary operator.
   * @param   query1  The query to operate on.
   */
  public Query(int op, Query query1) {
    type        = TYPE_UNARY;
    this.op     = op;
    this.query1 = query1;
  }
  
  /**
   * Create a new binary query.
   * 
   * @param   query1  The first query to operate on.
   * @param   op      The binary operator.
   * @param   query2  The second query to operate on.
   */
  public Query(Query query1, int op, Query query2) {
    type        = TYPE_BINARY;
    this.query1 = query1;
    this.op     = op;
    this.query2 = query2;
  }
  
  /**
   * Create a new comparison query.
   *
   * @param   field  The field for the comparison.
   * @param   op     The comparison operator.
   * @param   value  The value for the comparison.
   */
  public Query(String field, int op, Object value) {
    type       = TYPE_COMPARE;
    this.field = field;
    this.op    = op;
    this.value = value;
  }

  /** Validate this query. */
  public void validate() throws TupleException {
    super.validate();

    switch (type) {
    case TYPE_EMPTY:
      // Ignore all other fields.
      break;

    case TYPE_UNARY:
      if (null ==  query1) {
        throw new InvalidTupleException("Null subquery for unary query (" +
                                        this + ")");
      } else if (UNARY_NOT != op) {
        throw new InvalidTupleException("Illegal unary operator (" + op +
                                        ") for query (" + this + ")");
      }
      query1.validate();
      break;

    case TYPE_BINARY:
      if ((null == query1) || (null == query2)) {
        throw new InvalidTupleException("Null subquery for binary query (" +
                                        this + ")");
      } else if ((BINARY_AND != op) && (BINARY_OR != op)) {
        throw new InvalidTupleException("Illegal binary operator (" + op +
                                        ") for query (" + this + ")");
      }
      query1.validate();
      query2.validate();
      break;

    case TYPE_COMPARE:
      if (null == field) {
        throw new InvalidTupleException("Null field for comparison query (" +
                                        this + ")");
      } else if ((COMPARE_EQUAL > op) || (COMPARE_HAS_FIELD < op)) {
        throw new InvalidTupleException("Illegal comparison operator (" + op +
                                        ") for query (" + this + ")");
      }
      break;

    default:
      throw new InvalidTupleException("Illegal query type (" + type +
                                      ") for query (" + this + ")");
    }
  }

}
