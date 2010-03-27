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
 * Definition of an applicable entity. Examples of applicable entities
 * include Scheme procedures (lambdas), primitive operators, as well
 * as macros. The core evaluator has no notion of these particular
 * entities and interacts with all applicable entities through this
 * interface. This design allows for a very simple and extensible
 * interpreter as all applicable entities present a uniform interface.
 *
 * <p>Applicable entites generally must be serializable. However, some
 * instances of an implementation of this interface, in particular,
 * primitive continuations, may contain references to unserializable
 * objects, such as readers and writers. Consequently, such an
 * instance may not be serializable.</p>
 *
 * @see      Evaluator
 *
 * @author   &copy; Copyright 1998-2000 by Robert Grimm.
 *           Written by <a href="mailto:rgrimm@alum.mit.edu">Robert
 *           Grimm</a>.
 * @version  1.0
 */
public interface Applicable extends java.io.Serializable {

  // ========================== Constants ============================

  /**
   * The category constant for "<code>primitive-syntax</code>", that
   * is, basic language syntax. For Scheme, basically all primitive
   * and derived expression types, such as <code>if</code> or
   * <code>let</code> defined in &sect;4 and &sect;5 of R<sup><font
   * size="-1">5</font></sup>RS are considered primitive operators.
   */
  int CATEGORY_PRIMITIVE_SYNTAX        = 0;

  /**
   * The category constant for "<code>primitive-operator</code>", that
   * is, basic library procedures. For Scheme, all standard procedures
   * defined in &sect;6 of R<sup><font size="-1">5</font></sup>RS,
   * such as <code>+</code> and <code>list</code> are considered
   * primitive operators.
   */
  int CATEGORY_PRIMITIVE_OPERATOR      = 1;

  /**
   * The category constant for "<code>primitive-continuation</code>",
   * that is, continuation objects used to implement primitive syntax
   * or primitive operators.
   */
  int CATEGORY_PRIMITIVE_CONTINUATION  = 2;

  /**
   * The category constant for "<code>continuation</code>", that is,
   * first class continuations. In Scheme, first class continuations
   * are typically captured by the
   * <code>call-with-current-continuation</code> procedure defined in
   * &sect;6.4 of R<sup><font size="-1">5</font></sup>RS.
   */
  int CATEGORY_CONTINUATION            = 3;

  /**
   * The category constant for "<code>compound-procedure</code>", that
   * is, user-defined interpreted procedures. For Scheme, compound
   * procedures are the result of evaluating the lambda form defined
   * in &sect;4.1.4 of R<sup><font size="-1">5</font></sup>RS.
   */
  int CATEGORY_COMPOUND_PROCEDURE      = 4;

  /**
   * The category constant for "<code>macro</code>", that is,
   * user-defined syntactic constructs. For Scheme, applicable
   * entities in this category are the result of the
   * <code>syntax-rules</code> primitive syntax construct described in
   * &sect;4.3.2 of R<sup><font size="-1">5</font></sup>RS, and are
   * defined using <code>let-syntax</code>,
   * <code>letrec-syntax</code>, and <code>define-syntax</code> (see
   * &sect;4.3.1 and &sect;5.3 of R<sup><font
   * size="-1">5</font></sup>RS).
   */
  int CATEGORY_MACRO                   = 5;

  /**
   * The category constant for "<code>compiled-procedure</code>", that
   * is, compiled compound procedures.
   */
  int CATEGORY_COMPILED_PROCEDURE      = 6;

  /**
   * The category constant for "<code>compiled-macro</code>", that
   * is, compiled macros.
   */
  int CATEGORY_COMPILED_MACRO          = 7;

  /**
   * The category constant for "<code>user-defined-applicable</code>",
   * that is, anythings that does not fit into one of the other
   * categories.
   */
  int CATEGORY_USER_DEFINED_APPLICABLE = Integer.MAX_VALUE;

  // =========================== Methods =============================

  /**
   * Return the minimum number of arguments for this applicable
   * entity. Every applicable entity accepts at least this
   * non-negative minimum number of arguments.
   *
   * @see     #getMaxArgs()
   * 
   * @return  The non-negative minimum number of arguments for this
   *          applicable entity.
   */
  int getMinArgs();

  /**
   * Return the maximum number of arguments for this applicable
   * entity. Every applicable entity accepts a maximum number of
   * arguments that is at least as large as the number returned by
   * <code>getMinArgs()</code>. If the maximum number is unlimited,
   * this method returns -1.
   *
   * @see     #getMinArgs()
   *
   * @return  The non-negative maximum number of arguments for this
   *          applicable entity, or -1 if this applicable entity
   *          accepts an unlimited maximum number of arguments.
   */
  int getMaxArgs();

  /**
   * Determine whether this applicable entity is a syntactic
   * applicable entity.
   *
   * <p>On application, that is, before actually invoking an
   * applicable entity's <code>apply()</code> method, the evaluator
   * calls this method. If this applicable entity is syntactic, that
   * is, if this method returns <code>true</code>, this applicable
   * entity's <code>apply()</code> method is invoked on the
   * unevaluated arguments. If this applicable entity is a procedure,
   * that is, if this method returns <code>false</code>, the evaluator
   * first evaluates the actual arguments and invokes this applicable
   * entity's <code>apply()</code> method on the evaluated
   * arguments.</p>
   *
   * @see     #apply(Pair,Evaluator)
   *
   * @return  <code>true</code> iff this applicable entity is
   *          a syntactic entity.
   */
  boolean isSyntactic();

  /**
   * Apply this applicable entity on the specified arguments.
   *
   * <p>To apply an applicable entity on its arguments, the evaluator
   * calls this method with the actual arguments in a proper
   * list. This list may be the empty list if there are no
   * arguments. Generally, the actual arguments are not evaluated
   * before calling this method, if this applicable entity is
   * syntactic, and they are evaluated, if this applicable entity is a
   * procedure.</p>
   *
   * <p>This method must verify that is has been invoked with a legal
   * number of actual arguments, that is, whether the number of
   * arguments is consistent with the numbers returned by
   * <code>getMinArgs()</code> and <code>getMaxArgs()</code>.  If the
   * number of arguments is illegal and this applicable entity is
   * syntactic, this method should throw a
   * <code>BadSyntaxException</code>.  If the number of arguments is
   * illegal and this applicable entity is a procedure, this method
   * should throw a <code>BadArgumentException</code>.</p>
   *
   * <p>If this applicable entity is a syntactic applicable entity,
   * the evaluator treats the result returned from this method as an
   * expression to be evaluated in the current activation frame.  If
   * this applicable entity is a procedure, the evaluator treats the
   * result returned from this method as a value to be returned from
   * the current activation frame. An implementation of this method
   * can override this default behavior through the evaluator object
   * passed to this method.</p>
   *
   * @see     #isSyntactic()
   * @see     Evaluator
   *
   * @param   l  The actual arguments for this method in a proper
   *             list.
   * @param   evalutor
   *             The calling evaluator.
   * @return     The result of applying this applicable entity to the
   *             specified arguments.
   * @throws  EvaluatorException
   *             Signals an exceptional condition during the application
   *             of this applicable entity, inculding that this applicable
   *             entity was applied to the wrong number of arguments.
   */
  Object apply(Pair l, Evaluator evaluator) throws EvaluatorException;

  /**
   * Determine the category of this applicable. The returned number
   * must be one of the category constants defined in this interface.
   *
   * @see     #CATEGORY_PRIMITIVE_SYNTAX
   * @see     #CATEGORY_PRIMITIVE_OPERATOR
   * @see     #CATEGORY_PRIMITIVE_CONTINUATION
   * @see     #CATEGORY_CONTINUATION
   * @see     #CATEGORY_COMPOUND_PROCEDURE
   * @see     #CATEGORY_MACRO
   * @see     #CATEGORY_COMPILED_PROCEDURE
   * @see     #CATEGORY_COMPILED_MACRO
   * @see     #CATEGORY_USER_DEFINED_APPLICABLE
   * 
   * @return  The category of this applicable, which must be one of the
   *          category constants defined in this interface.
   */
  int getCategory();

  /**
   * Return the name of this applicable entity. If this applicable
   * entity has no name, the returned string must be constructed as
   * specified for <code>Object.toString()</code>, using
   * <code>System.identityHashCode()</code> to determine the hash code
   * for this applicable entity.
   *
   * @return  The name of this applicable entity.
   */
  String getName();

  /**
   * Return a string representation for this applicable
   * entity. Strings returned by this method must have the format
   * <blockquote><pre>
   * "#[&lt;<i>category</i>&gt; &lt;<i>name</i>&gt;]"
   * </pre></blockquote> where &lt;<i>category</i>&gt; is a general
   * description of this applicable entity's category, such as
   * "<code>primitive-operator</code>" for primitive operators or
   * "<code>compound-procedure</code>" for user defined lambdas, and
   * &lt;<i>name</i>&gt; is that particular applicable entity's
   * name.
   *
   * @see     #getCategory()
   * @see     #getName()
   *
   * @return A string representing this applicable entity.
   */
  String toString();

}
