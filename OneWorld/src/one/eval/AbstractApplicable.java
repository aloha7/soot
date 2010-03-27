/*
 * Copyright (c) 1999, 2000, Robert Grimm.
 *    All rights reserved.
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
 * 3. Neither name of Robert Grimm nor the names of his contributors
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

package one.eval;

/**
 * Abstract base class that provides a skeletal implementation of the
 * <code>Applicable</code> interface. The intent behind this class is
 * to simplify the implementation effort for applicable entities.
 *
 * <p>This class defines fields for the name of an applicable entity,
 * the minimum and maximum number of arguments, an opcode, and the
 * debugging status. It also implements all methods defined in the
 * <code>Applicable</code> interface, though the implementation of the
 * <code>apply()</code> method relies on the abstract
 * <code>apply1()</code> method to perform any real work.</p>
 *
 * <p>Consequently, a concrete subclass of this class must implement
 * the <code>apply1()</code> method to perform the actual
 * application. It also needs to define a constructor and, optionally,
 * an installation function.</p>
 *
 * <p>The value of the <code>opcode</code> field determines the
 * category for an actual applicable entity and thus the result of the
 * <code>isSyntactic()</code>, <code>getCategory()</code>, and
 * <code>toString()</code> methods. A concrete subclass must choose
 * its opcodes so that they fall into the opcode range corresponding
 * to the desired category.</p>
 *
 * @see      #apply(Pair,Evaluator)
 * @see      #apply1(Pair,int,Evaluator)
 * @see      #opcode
 * @see      #MAX_OPCODE_PRIMITIVE_SYNTAX
 * @see      #MAX_OPCODE_MACRO
 * @see      #MAX_OPCODE_COMPILED_MACRO
 * @see      #MAX_OPCODE_PRIMITIVE_OPERATOR
 * @see      #MAX_OPCODE_COMPOUND_PROCEDURE
 * @see      #MAX_OPCODE_COMPILED_PROCEDURE
 * @see      #MAX_OPCODE_PRIMITIVE_CONTINUATION
 * @see      #MAX_OPCODE_CONTINUATION
 *
 * @author   &copy; Copyright 1998-2000 by Robert Grimm.
 *           Written by <a href="mailto:rgrimm@alum.mit.edu">Robert
 *           Grimm</a>.
 * @version  1.0
 */
public abstract class AbstractApplicable implements Applicable {

  // ======================================================================
  //                               Constants
  // ======================================================================

  /** The maximal opcode for primitive syntax. Defined to be 10,000. */
  protected static final int MAX_OPCODE_PRIMITIVE_SYNTAX       = 10000;

  /** The maximal opcode for macros. Defined to be 20,000. */
  protected static final int MAX_OPCODE_MACRO                  = 20000;

  /** The maximal opcode for compiled macros. Defined to be 30,000. */
  protected static final int MAX_OPCODE_COMPILED_MACRO         = 30000;

  /** The maximal opcode for primitive operators. Defined to be 40,000. */
  protected static final int MAX_OPCODE_PRIMITIVE_OPERATOR     = 40000;

  /** The maximal opcode for compound procedures. Defined to be 50,000. */
  protected static final int MAX_OPCODE_COMPOUND_PROCEDURE     = 50000;

  /** The maximal opcode for compiled procedures. Defined to be 60,000. */
  protected static final int MAX_OPCODE_COMPILED_PROCEDURE     = 60000;

  /** The maximal opcode for primitive continuations. Defined to be 70,000. */
  protected static final int MAX_OPCODE_PRIMITIVE_CONTINUATION = 70000;

  /** The maximal opcode for continuations. Defined to be 80,000. */
  protected static final int MAX_OPCODE_CONTINUATION           = 80000;

  // ======================================================================
  //                                Fields
  // ======================================================================

  /**
   * The name of this applicable entity.
   *
   * @serial
   */
  protected String  name;

  /**
   * The minimum number of arguments for this applicable entity.
   *
   * @serial  The non-negative minimum number of arguments for this
   *          applicable entity.
   */
  protected int     minArgs;

  /**
   * The maximum number of arguments for this applicable entity.
   *
   * @serial  If the maximum number of arguments for this applicable
   *          entity is limited, <code>minArgs <= maxArgs</code>
   *          must be <code>true</code>. If it is unlimited,
   *          <code>maxArgs</code> must be -1.
   */
  protected int     maxArgs;

  /**
   * The opcode of this applicable entity.
   *
   * @serial  Must be one of the opcodes defined by a concrete subclass
   *          of this class.
   */
  protected int     opcode;

  // ======================================================================
  //                              Constructor
  // ======================================================================

  /**
   * Create a new abstract applicable object. This constructor does
   * nothing.
   */
  protected AbstractApplicable() {
    // Nothing to construct.
  }

  // ======================================================================
  //                               Methods
  // ======================================================================

  /**
   * Return the minimum number of arguments for this applicable
   * entity.
   *
   * @see     #minArgs
   *
   * @return  The non-negative minimum number of arguments for this
   *          applicable entity.
   */
  public int getMinArgs() {
    return minArgs;
  }

  /**
   * Return the maximum number of arguments for this applicable
   * entity.
   *
   * @see     #maxArgs
   * 
   * @return  The non-negative maximum number of arguments for
   *          this applicable entity, or -1 if it accepts an
   *          unlimited maximum number of arguments.
   */
  public int getMaxArgs() {
    return maxArgs;
  }

  /**
   * Determine whether this applicable entity is a syntactic
   * applicable entity.
   *
   * @return  <code>true</code> iff this applicable entity
   *          represents primitive syntax, a macro, or a compiled
   *          macro.
   */
  public boolean isSyntactic() {
    return (MAX_OPCODE_COMPILED_MACRO >= opcode);
  }

  /**
   * Apply this applicable entity to the specified arguments. This
   * method verifies that the number of actual arguments is a valid
   * number of arguments. It relies on <code>apply1()</code> to
   * implement any actual functionality.
   *
   * @see     #apply1(Pair,int,Evaluator)
   *
   * @param   l  The actual arguments for this method in a proper
   *             list.
   * @param   evaluator
   *             The calling evaluator.
   * @return     The result of applying this applicable entity to the
   *             specified arguments.
   * @throws  EvaluatorException
   *             Signals an exceptional condition during the application
   *             of this applicable entity, inculding that this applicable
   *             entity was applied to the wrong number of arguments.
   */
  public Object apply(Pair l, Evaluator evaluator)
    throws EvaluatorException {

    // Check that the number of arguments is valid.
    int numArgs = 0;

    if (Pair.EMPTY_LIST != l) {
      numArgs = l.length();
    }

    if (minArgs > numArgs) {
      if (MAX_OPCODE_COMPILED_MACRO >= opcode) {
        throw new BadSyntaxException("Too few forms (" + numArgs + ") for",
                                     this);
      } else {
        throw new BadArgumentException("Too few arguments (" + numArgs +
                                       ") for", this);
      }
    } else if ((-1 != maxArgs) && (maxArgs < numArgs)) {
      if (MAX_OPCODE_COMPILED_MACRO >= opcode) {
        throw new BadSyntaxException("Too many forms (" + numArgs + ") for",
                                     this);
      } else {
        throw new BadArgumentException("Too many arguments (" + numArgs +
                                       ") for", this);
      }
    }

    // Perform the actual application.
    Object result = apply1(l, numArgs, evaluator);

    // Return the result.
    return result;
  }

  /**
   * Apply this applicable entity on the specified arguments and return
   * the result.
   *
   * <p>A concrete subclass of this class must implement this method
   * to provide actual functionality.</p>
   *
   * @param   l  The actual arguments for this method in a proper list.
   * @param   numArgs
   *             The number of actual arguments in <code>l</code>.
   * @param   evaluator
   *             The calling evaluator.
   * @return     The result of applying this applicable entity to the
   *             specified arguments.
   * @throws  EvaluatorException
   *             Signals an exceptional condition during the application
   *             of this applicable entity.
   */
  protected abstract Object apply1(Pair l, int numArgs, Evaluator evaluator)
    throws EvaluatorException;

  /**
   * Return the category identifier for this applicable entity. The
   * category identifier returned by this method depends on the value
   * of <code>opcode</code>.
   *
   * @see     #opcode
   *
   * @return  The category identifier for this applicable entity.
   */
  public int getCategory() {
    if (MAX_OPCODE_PRIMITIVE_SYNTAX >= opcode) {
      return CATEGORY_PRIMITIVE_SYNTAX;
    } else if (MAX_OPCODE_MACRO >= opcode) {
      return CATEGORY_MACRO;
    } else if (MAX_OPCODE_COMPILED_MACRO >= opcode) {
      return CATEGORY_COMPILED_MACRO;
    } else if (MAX_OPCODE_PRIMITIVE_OPERATOR >= opcode) {
      return CATEGORY_PRIMITIVE_OPERATOR;
    } else if (MAX_OPCODE_COMPOUND_PROCEDURE >= opcode) {
      return CATEGORY_COMPOUND_PROCEDURE;
    } else if (MAX_OPCODE_COMPILED_PROCEDURE >= opcode) {
      return CATEGORY_COMPILED_PROCEDURE;
    } else if (MAX_OPCODE_PRIMITIVE_CONTINUATION >= opcode) {
      return CATEGORY_PRIMITIVE_CONTINUATION;
    } else if (MAX_OPCODE_CONTINUATION >= opcode) {
      return CATEGORY_CONTINUATION;
    } else {
      return CATEGORY_USER_DEFINED_APPLICABLE;
    }
  }

  /**
   * Return the name for this applicable entity.
   *
   * <p>If <code>null == name</code>, the name is synthesized from the
   * name of the Java class and the identity hash code for this
   * applicable entity, as described for
   * <code>Object.toString()</code>. Otherwise, <code>name</code> is
   * returned.</p>
   *
   * @see     #name
   *
   * @return  The name of this applicable entity.
   */
  public String getName() {
    if (null == name) {
      return (getClass().getName() + '@' +
              Integer.toHexString(System.identityHashCode(this)));
    } else {
      return name;
    }
  }

  /**
   * Return a string representing this applicable entity.
   *
   * @return  A string representing this applicable entity.
   */
  public String toString() {
    StringBuffer buf = new StringBuffer(32);

    buf.append("#[");

    if (MAX_OPCODE_PRIMITIVE_SYNTAX >= opcode) {
      buf.append("primitive-syntax ");
    } else if (MAX_OPCODE_MACRO >= opcode) {
      buf.append("macro ");
    } else if (MAX_OPCODE_COMPILED_MACRO >= opcode) {
      buf.append("compiled-macro ");
    } else if (MAX_OPCODE_PRIMITIVE_OPERATOR >= opcode) {
      buf.append("primitive-operator ");
    } else if (MAX_OPCODE_COMPOUND_PROCEDURE >= opcode) {
      buf.append("compound-procedure ");
    } else if (MAX_OPCODE_COMPILED_PROCEDURE >= opcode) {
      buf.append("compiled-procedure ");
    } else if (MAX_OPCODE_PRIMITIVE_CONTINUATION >= opcode) {
      buf.append("primitive-continuation ");
    } else if (MAX_OPCODE_CONTINUATION >= opcode) {
      buf.append("continuation ");
    } else {
      buf.append("user-defined-applicable ");
    }

    if (null == name) {
      buf.append(getClass().getName());
      buf.append('@');
      buf.append(Integer.toHexString(System.identityHashCode(this)));
    } else {
      buf.append(name);
    }

    buf.append(']');

    return buf.toString();
  }

}
