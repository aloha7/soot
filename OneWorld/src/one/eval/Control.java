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

import one.util.Bug;

/**
 * Implementation of the Scheme standard procedures providing control
 * features. This class implements the operators defined in &sect;6.4
 * of R<sup><font size="-1">5</font></sup>RS.
 *
 * @author   &copy; Copyright 1998-2000 by Robert Grimm.
 *           Written by <a href="mailto:rgrimm@alum.mit.edu">Robert
 *           Grimm</a>.
 * @version  1.0
 */
public final class Control extends AbstractApplicable {

  // =========================== Constants =============================

  private static final int
    PROCEDURE_Q = 30001, APPLY = 30002, MAP = 30003, FOR_EACH = 30004,
    FORCE = 30005, CALL_CC = 30006, VALUES = 30007, CALL_VALUES = 30008,
    DYNAMIC_WIND = 30009;

  // Primitive continuations.
  static final int
    CONT_MAP = 60003, CONT_FOR_EACH = 60004, CONT_FORCE = 60005,
    CONT_DYNAMIC_WIND = 60009;

  // =================== Primitive Continuation ========================

  /** Implementation of a primitive control continuation. */
  static final class Continuation extends AbstractApplicable {
    /**
     * Internal helper object 1.
     *
     * @serial  Used by this control continuation to store its internal
     *          state. The invariants for <code>one</code> depend on the
     *          type of control continuation an instance of this class
     *          represents and therefore on <code>opcode</code>.
     *
     *          <p>The following invariants for this field,
     *          <code>two</code>, and <code>three</code>,
     *          as well as for <code>minArgs</code>
     *          and <code>maxArgs</code> must hold for the specified
     *          opcodes:
     *          <ul>
     *          <li><code>CONT_MAP</code> (60003): The <code>map</code>
     *          continuation takes exactly one argument which is
     *          the next result for the list of results.
     *          <code>one</code> must be a pair whose car is the
     *          procedure being mapped and whose cdr is a
     *          non-null, non-empty array of lists of equal length
     *          representing the arguments for future applications
     *          of the procedure. <code>two</code> is the head
     *          of the result list, and <code>three</code> is
     *          the tail of that same list.
     *          </li>
     *
     *          <li><code>CONT_FOR_EACH</code> (60004): The
     *          <code>for-each</code>
     *          continuation takes exactly one argument which it
     *          throws away. <code>one</code> is the procedure being
     *          mapped and <code>two</code> is a non-null, non-empty
     *          array of lists of equal length representing the
     *          arguments for future applications of the procedure.
     *          </li>
     *
     *          <li><code>CONT_FORCE</code> (60005): The <code>force</code>
     *          continuation takes exactly one argument which is the
     *          value for the promise it forces. <code>one</code> must
     *          be the promise. Note that a promise may have a reference
     *          to a non-serializable object. In this case, this
     *          primitive operator is not serializable.
     *          </li>
     *
     *          <li><code>CONT_DYNAMIC_WIND</code> (60009): The
     *          <code>dynamic-wind</code> continuation takes no arguments.
     *          <code>one</code> must be the before procedure,
     *          <code>two</code> must be the main thunk, and
     *          <code>three</code> must be the after procedure.
     *          </li>
     *
     *          </ul>
     *          If <code>opcode</code> has a value not specified in
     *          this list, the primitive operator object must not use
     *          the fields <code>one</code>, <code>two</code>,
     *          and <code>three</code> in any way.</p>
     */
    Object      one;
    
    /**
     * Internal helper object 2.
     *
     * @serial  Used by this control continuation to store its internal
     *          state. The invariants for <code>two</code> depend on the
     *          type of control continuation an instance of this class
     *          represents and therefore on <code>opcode</code>. See
     *          description for <code>one</code>.
     */
    Object      two;
    
    /**
     * Internal helper object 3.
     *
     * @serial  Used by this control continuation to store its internal
     *          state. The invariants for <code>three</code> depend on the
     *          type of control continuation an instance of this class
     *          represents and therefore on <code>opcode</code>. See
     *          description for <code>one</code>.
     */
    Object      three;
    
    /**
     * Create a new control continuation with the specified name,
     * opcode, and number of arguments.
     *
     * @param  name     The name of the control continuation.
     * @param  opcode   The opcode of the control continuation.
     * @param  minArgs  The non-negative minimum number of arguments
     *                  for this control continuation.
     * @param  maxArgs  The non-negative maximum number of arguments
     *                  for this control continuation, or -1 if this
     *                  continuation takes an unlimited maximum number
     *                  of arguments. In the former case,
     *                  <code>maxArgs >= minArgs</code> must be
     *                  <code>true</code>.
     */
    Continuation(String name, int opcode, int minArgs, int maxArgs) {
      this.name    = name;
      this.opcode  = opcode;
      this.minArgs = minArgs;
      this.maxArgs = maxArgs;
    }

    /**
     * Apply this control operator on the specified arguments.
     *
     * @param   l          The arguments as a proper list.
     * @param   numArgs    The number of arguments in <code>l</code>.
     * @param   evaluator  The calling evaluator.
     * @return             The result of applying this operator
     *                     on the specified arguments.
     * @throws  EvaluatorException
     *                     Signals an exceptional condition when
     *                     applying this operator to the specified
     *                     arguments.
     */
    protected Object apply1(Pair l, int numArgs, Evaluator evaluator)
      throws EvaluatorException {


      switch (opcode) {

      case CONT_MAP:
        {
          // Collect result.
          Pair p = Pair.cons(l.car, Pair.EMPTY_LIST);
          if (Pair.EMPTY_LIST == two) {
            two   = p;
            three = p;
          } else {
            ((Pair)three).cdr = p;
            three             = p;
          }
          
          Applicable proc = (Applicable)((Pair)one).car;
          Object[]   a    = (Object[])((Pair)one).cdr;
          
          // Are we done yet.
          if (Pair.EMPTY_LIST == a[0]) {
            return two;
          }
          
          // Cons up arguments and adjust array of lists.
          Pair args = Pair.EMPTY_LIST;
          for (int i=a.length-1; i>=0; i--) {
            Pair   q = (Pair)a[i];
            args     = Pair.cons(q.car, args);
            a[i]     = q.cdr;
          }
          
          evaluator.returnApplication(proc, args, this);
          return Boolean.FALSE;
        }
      case CONT_FOR_EACH:
        {
          // Throw away argument.
          
          Applicable proc = (Applicable)one;
          Object[]   a    = (Object[])two;
          
          // Are we done yet.
          if (Pair.EMPTY_LIST == a[0]) {
            return Boolean.FALSE;
          }
          
          // Cons up arguments and adjust array of lists.
          Pair args = Pair.EMPTY_LIST;
          for (int i=a.length-1; i>=0; i--) {
            Pair   q = (Pair)a[i];
            args     = Pair.cons(q.car, args);
            a[i]     = q.cdr;
          }
          
          evaluator.returnApplication(proc, args, this);
          return Boolean.FALSE;
        }
      case CONT_FORCE:
        {
          Promise promise = (Promise)one;
          promise.force(l.car);
          return promise.getResult(); // Handle races correctly.
        }
      case CONT_DYNAMIC_WIND:
        evaluator.setUpBeforeAfter(one, three);
        evaluator.returnExpression();
        return two;
        
      default:
        throw new Bug("Invalid opcode " + opcode +
                      " for control continuation" + toString());
      }
    }
  }

  // =========================== Constructor ===========================

  /**
   * Create a new control operator with the specified name, opcode,
   * and number of arguments.
   *
   * @param  name     The name of the control operator.
   * @param  opcode   The opcode of the control operator.
   * @param  minArgs  The non-negative minimum number of arguments
   *                  for this control operator.
   * @param  maxArgs  The non-negative maximum number of arguments
   *                  for this control operator, or -1 if this
   *                  operator takes an unlimited maximum number
   *                  of arguments. In the former case,
   *                  <code>maxArgs >= minArgs</code> must be
   *                  <code>true</code>.
   */
  private Control(String name, int opcode, int minArgs, int maxArgs) {
    this.name    = name;
    this.opcode  = opcode;
    this.minArgs = minArgs;
    this.maxArgs = maxArgs;
  }

  // ============================ Equality ================================

  /**
   * Determine whether this control operator equals the specified
   * object.
   *
   * @param   o  The object with which to compare.
   * @return     <code>true</code> if this control operator equals
   *             the specified object.
   */
  public boolean equals(Object o) {
    if (this == o) return true;
    if (! (o instanceof Control)) return false;

    Control other = (Control)o;

    return (   (name.equals(other.name))
            && (opcode  == other.opcode)
            && (minArgs == other.minArgs)
            && (maxArgs == other.maxArgs));
  }

  /**
   * Return a hash code for this control operator.
   *
   * @return  A hash code for this control operator.
   */
  public int hashCode() {
    return name.hashCode();
  }

  // =========================== Application ===========================

  /**
   * Apply this control operator on the specified arguments.
   *
   * @param   l          The arguments as a proper list.
   * @param   numArgs    The number of arguments in <code>l</code>.
   * @param   evaluator  The calling evaluator.
   * @return             The result of applying this operator
   *                     on the specified arguments.
   * @throws  EvaluatorException
   *                     Signals an exceptional condition when
   *                     applying this operator to the specified
   *                     arguments.
   */
  protected Object apply1(Pair l, int numArgs, Evaluator evaluator)
    throws EvaluatorException {

    // Set up arguments.
    Object o1 = null;
    Object o2 = null;
    Object o3 = null;

    if (1 <= numArgs) {
      o1 = l.getListRef(0);
    }
    if (2 <= numArgs) {
      o2 = l.getListRef(1);
    }
    if (3 <= numArgs) {
      o3 = l.getListRef(2);
    }
    
    // Do the deed.
    switch (opcode) {

    case PROCEDURE_Q:
      if ((o1 instanceof Applicable)?
          (! ((Applicable)o1).isSyntactic()) : false) {
        return Boolean.TRUE;
      } else {
        return Boolean.FALSE;
      }
    case APPLY:
      return doApply(l, evaluator);
    case MAP:
      return map(l, evaluator);
    case FOR_EACH:
      return forEach(l, evaluator);
    case FORCE:
      return force(o1, evaluator);
    case CALL_CC:
      {
        Applicable proc = Cast.toProcedure(o1);
        Applicable cont = evaluator.getCurrentContinuation();

        evaluator.returnExpression();
        return Pair.cons(proc, Pair.cons(cont, Pair.EMPTY_LIST));
      }
    case VALUES:
      if (1 == numArgs) { 
        // 1 argument is simple one value.
        return o1;

      } else if (evaluator.acceptsValues()) {
        evaluator.returnValues();
        return l;

      } else if (0 == numArgs) {
        throw new BadArgumentException(
          "Too few (0) arguments for current continuation", l);
      } else {
        throw new BadArgumentException("Too many (" + numArgs +
          ") arguments for current continuation", l);
      }
    case CALL_VALUES:
      {
        Applicable source = Cast.toProcedure(o1);
        Applicable sink   = Cast.toProcedure(o2);

        evaluator.setUpValues(sink);
        evaluator.returnExpression();
        return Pair.cons(source, Pair.EMPTY_LIST);
      }
    case DYNAMIC_WIND:
      {
        Applicable   before = Cast.toProcedure(o1);
        Applicable   thunk  = Cast.toProcedure(o2);
        Applicable   after  = Cast.toProcedure(o3);

        Continuation cont   = new Continuation("dynamic-wind",
                                               CONT_DYNAMIC_WIND, 0, 0);

        cont.one            = Pair.cons(before, Pair.EMPTY_LIST);
        cont.two            = Pair.cons(thunk,  Pair.EMPTY_LIST);
        cont.three          = Pair.cons(after,  Pair.EMPTY_LIST);

        evaluator.returnSequence();
        return Pair.cons(cont.one, Pair.cons(Pair.cons(cont, Pair.EMPTY_LIST),
                                             Pair.EMPTY_LIST));
      }

    default:
      throw new Bug("Invalid opcode " + opcode + " for control operator" +
                    toString());
    }
  }

  /**
   * Execute the <code>apply</code> operator.
   *
   * @param   l  The list of arguments for <code>apply</code>.
   * @param   evaluator
   *             The calling evaluator.
   * @return     The result of executing <code>apply</code>.
   * @throws  BadTypeException
   *             Signals that the first argument is not an
   *             applicable entity, or that the last argument
   *             is not a pair.
   * @throws  BadArgumentException
   *             Signals that the first argument is a syntactic
   *             applicable entity, or that the last argument is
   *             not a proper list.
   */
  private Object doApply(Pair l, Evaluator evaluator)
    throws BadTypeException, BadArgumentException {

    Applicable proc = Cast.toProcedure(l.car);

    l = (Pair)l.cdr;

    Object o;
    Pair   head = Pair.EMPTY_LIST;
    Pair   tail = Pair.EMPTY_LIST;

    // Process the arg_i arguments.
    Pair lCdr;
    while ((lCdr = (Pair)l.cdr) != Pair.EMPTY_LIST) {
      o = l.car;

      Pair p = Pair.cons(o, Pair.EMPTY_LIST);

      if (Pair.EMPTY_LIST == head) {
        head = p;
        tail = p;
      } else {
        tail.cdr = p;
        tail     = p;
      }

      l = lCdr;
    }

    // Process the args argument.
    o = l.car;
    if (null == o) {
      // Nothing to add.

    } else if (! (o instanceof Pair)) {
      throw new BadTypeException("Not a list", o);

    } else if (! ((Pair)o).isList()) {
      throw new BadArgumentException("Not a list", o);

    } else {
      l = (Pair)o;
      if (Pair.EMPTY_LIST == head) {
        head = l;
      } else {
        tail.cdr = l;
      }
    }

    // Return the result as an application.
    evaluator.returnApplication(proc, head, null);
    return Boolean.FALSE;
  }

  /**
   * Execute the <code>map</code> operator.
   *
   * @param   l  The list of arguments for <code>map</code>.
   * @param   evaluator
   *             The calling evaluator.
   * @return     The result of executing <code>map</code>.
   * @throws  BadTypeException
   *             Signals that the first argument is not
   *             an applicable entity, or that another
   *             argument is not a pair.
   * @throws  BadArgumentException
   *             Signals that the first argument is a syntactic
   *             applicable entity, or that an argument besides
   *             the first argument is not a proper list, or that
   *             the lists do not have the same length.
   */
  private Object map(Pair l, Evaluator evaluator)
    throws BadTypeException, BadArgumentException {

    Applicable proc = Cast.toProcedure(l.car);

    l            = (Pair)l.cdr;     // The list of lists.
    Object[] a   = l.toArray();     // Now, an array of lists.
    int length   = verifyLists(a);  // Verify the array of lists.

    // Handle trivial case.
    if (0 == length) {
      return Pair.EMPTY_LIST;
    }

    // Cons up arguments and adjust array of lists.
    Pair args = Pair.EMPTY_LIST;
    for (int i=a.length-1; i>=0; i--) {
      Pair   q = (Pair)a[i];
      args     = Pair.cons(q.car, args);
      a[i]     = q.cdr;
    }

    // Work it.
    Continuation cont = new Continuation("map", CONT_MAP, 1, 1);
    cont.one          = Pair.cons(proc, a);  // Pair of proc and args
    cont.two          = Pair.EMPTY_LIST;     // Head of result
    cont.three        = Pair.EMPTY_LIST;     // Tail of result

    evaluator.returnApplication(proc, args, cont);
    return Boolean.FALSE;
  }

  /**
   * Execute the <code>for-each</code> operator.
   *
   * @param   l  The list of arguments for <code>for-each</code>.
   * @param   evaluator
   *             The calling evaluator.
   * @return     The result of executing <code>for-each</code>.
   * @throws  BadTypeException
   *             Signals that the first argument is not
   *             an applicable entity, or that another
   *             argument is not a pair.
   * @throws  BadArgumentException
   *             Signals that the first argument is a syntactic
   *             applicable entity, or that an argument besides
   *             the first argument is not a proper list, or
   *             that the lists do not have the same length.
   */
  private Object forEach(Pair l, Evaluator evaluator)
    throws BadTypeException, BadArgumentException {

    Applicable proc = Cast.toProcedure(l.car);

    l            = (Pair)l.cdr;     // The list of lists.
    Object[] a   = l.toArray();     // Now, an array of lists.
    int length   = verifyLists(a);  // Verify the array of lists.

    // Handle trivial case.
    if (0 == length) {
      return Boolean.FALSE;
    }

    // Cons up arguments and adjust array of lists.
    Pair args = Pair.EMPTY_LIST;
    for (int i=a.length-1; i>=0; i--) {
      Pair   q = (Pair)a[i];
      args     = Pair.cons(q.car, args);
      a[i]     = q.cdr;
    }

    // Work it.
    Continuation cont = new Continuation("for-each", CONT_FOR_EACH, 1, 1);
    cont.one          = proc; // The procedure.
    cont.two          = a;    // The array of lists.

    evaluator.returnApplication(proc, args, cont);
    return Boolean.FALSE;
  }

  /**
   * Verify that the elements in the specified array of objects are
   * all lists of the same length and return that length. The
   * specified array must be of length at least one.
   *
   * @param   a  The array of objects to check.
   * @return     The length of the lists in the specified array
   *             of objects.
   * @throws  BadTypeException
   *             Signals that an array element is not a pair.
   * @throws  BadArgumentException
   *             Signals that an array element is not a proper
   *             list or that the lists have different lengths.
   */
  private static int verifyLists(Object[] a)
    throws BadTypeException, BadArgumentException {

    int    length = -1;

    // Check first element.
    Object o      = a[0];
    if (Pair.EMPTY_LIST == o) {
      length = 0;
    } else if (! (o instanceof Pair)) {
      throw new BadTypeException("Not a list", o);
    } else {
      length = ((Pair)o).length();
    }

    // Make sure all other elements are lists of the same length as the first.
    for (int i=1; i<a.length; i++) {
      o = a[i];

      int l;

      if (Pair.EMPTY_LIST == o) {
        l = 0;
      } else if (! (o instanceof Pair)) {
        throw new BadTypeException("Not a list", o);
      } else {
        l = ((Pair)o).length();
      }

      if (l != length) {
        throw new BadArgumentException("List not of same length as first list",
                                       o);
      }
    }

    return length;
  }

  /**
   * Execute <code>force</code>
   *
   * @param   o          The object to force.
   * @param   evaluator  The calling evaluator.
   * @return             The specified object if it is not a promise.
   *                     Otherwise, the value of the promise if it has
   *                     been forced, or the expression of the promise
   *                     if it has not been forced.
   * @throws  EvaluatorException
   *                     Signals an exceptional condition when during the
   *                     evaluation of the promise.
   */
  private Object force(Object o, Evaluator evaluator)
    throws EvaluatorException {

    if (o instanceof Promise) {
      Promise promise = (Promise)o;
      int     state   = promise.getState();

      // If promise is threaded, wait for thread to join.
      while (Promise.STATE_THREAD == state) {
        // Sleep and dream of sheep.
        try {
          Thread.sleep(1000); // I get antsy after about a second.
        } catch (InterruptedException x) {
          // Ignore.
        }

        state = promise.getState(); // Try again.
      }

      // Now, deal with values, throwables, and expressions.
      if ((Promise.STATE_VALUE == state) ||
          (Promise.STATE_THROWABLE == state)) {
        // Promise has been forced.
        return promise.getResult();

      } else {
        // Force promise.
        Object      expr;
        Environment env;
        try {
          expr = promise.getExpression();
          env  = promise.getEnvironment();
        } catch (IllegalStateException x) {
          // Race on promise, return result.
          return promise.getResult();
        }

        // Environment must be set for both simple and non-simple
        // expressions.
        evaluator.setCurrentEnvironment(env);

        if (evaluator.isSimpleExpression(expr)) {
          // Evaluate simple expresion directly.
          Object value = evaluator.evalSimpleExpression(expr);
          promise.force(value);
          return promise.getResult();

        } else {
          evaluator.returnExpression();
          Continuation cont = new Continuation("force", CONT_FORCE, 1, 1);
          cont.one          = promise;
          return Pair.cons(cont, Pair.cons(expr, Pair.EMPTY_LIST));
        }
      }

    } else {
      // Forcing something that is not a promise returns that something.
      return o;
    }
  }

  // ========================== Installation ===========================

  /**
   * Install the control operators in the specified environment.
   *
   * @param   env     The environment to install the control
   *                  operators into.
   */
  public static void install(Environment env) {
    add(env, "procedure?",           PROCEDURE_Q,          1,  1);
    add(env, "apply",                APPLY,                2, -1);
    add(env, "map",                  MAP,                  2, -1);
    add(env, "for-each",             FOR_EACH,             2, -1);
    add(env, "force",                FORCE,                1,  1);
    add(env, "call-with-current-continuation", CALL_CC,    1,  1);
    add(env, "values",               VALUES,             0, -1);
    add(env, "call-with-values",     CALL_VALUES,        2,  2);
    add(env, "dynamic-wind",         DYNAMIC_WIND,       3,  3);
  }

  /**
   * Create a new control operator as specified and add a binding
   * for it to the specified environment.
   *
   * @param   env       The environment for the new control operator.
   * @param   name      The name of the new control operator.
   * @param   opcode    The opcode of the new control operator.
   * @param   minArgs   The non-negative minimum number of arguments for
   *                    the new control operator.
   * @param   maxArgs   The non-negative maximum number of arguments for
   *                    the new control operator, or -1 if it takes an
   *                    unlimited maximum number of arguments.
   */
  private static void add(Environment env, String name, int opcode,
                          int minArgs, int maxArgs) {
    name      = name.intern();
    Symbol  s = Symbol.intern(name);
    Control v = new Control(name, opcode, minArgs, maxArgs);

    env.bind(s, v);
  }

}
