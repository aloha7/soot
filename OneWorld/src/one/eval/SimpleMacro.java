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
 * Implementation of a simple macro facility. This class implements a
 * simple macro facility, providing the ability to define syntactic
 * closures at the top level.
 *
 * <p>This class defines the following extension to the Scheme
 * standard:</p>
 *
 * <p><code>(defmacro</code>&nbsp;&nbsp;&lt;<i>variable</i>&gt;&nbsp;&nbsp;&lt;<i>formals</i>&gt;&nbsp;&nbsp;&lt;<i>body</i>&gt;<code>)</code><br />
 * The <code>defmacro</code> form creates a syntactic closure and
 * binds <i>variable</i> to that closure. <i>formals</i> and
 * <i>body</i> have the same syntax as for a <code>lambda</code> form.
 * On application of a syntactic closure, the arguments are not
 * evaluated, and the body is evaluated in the definition environment
 * of the macro, with the formals bound to the unevaluated
 * arguments. The result of evaluating the body, in turn, is treated
 * as an expression that is evaluated in the dynamic environment of
 * the macro call.</p>
 *
 * @author   &copy; Copyright 1998-2000 by Robert Grimm.
 *           Written by <a href="mailto:rgrimm@alum.mit.edu">Robert
 *           Grimm</a>.
 * @version  1.0
 */
public final class SimpleMacro extends AbstractApplicable {

  // ============================ Constants ===============================

  /** The <code>define</code> symbol. */
  private static final Symbol SYM_DEFINE = Symbol.intern("define");

  /** The <code>begin</code> symbol. */
  private static final Symbol SYM_BEGIN  = Symbol.intern("begin");

  /** The <code>defmacro</code> symbol. */
  private static final Symbol SYM_DEFMACRO = Symbol.intern("defmacro");

  private static final int
    DEFMACRO = 1;

  // =========================== Constructor ============================

  /**
   * Create a new simple macro form with the specified name, opcode and
   * argument number restrictions.
   *
   * @param   name     The name for the new simple macro form.
   * @param   opcode   The opcode for the new simple macro form.
   * @param   minArgs  The non-negative minimum number of arguments
   *                   for the new simple macro form.
   * @param   maxArgs  The non-negative maximum number of arguments
   *                   for the new simple macro form, or -1 if the new
   *                   simple macro form takes an unlimited number of
   *                   arguments.
   */
  private SimpleMacro(String name, int opcode, int minArgs, int maxArgs) {
    this.name    = name;
    this.opcode  = opcode;
    this.minArgs = minArgs;
    this.maxArgs = maxArgs;
  }

  // ============================ Equality ================================

  /**
   * Determine whether this simple macro form equals the specified
   * object.
   *
   * @param   o  The object with which to compare.
   * @return     <code>true</code> if this simple macro form equals
   *             the specified object.
   */
  public boolean equals(Object o) {
    if (this == o) return true;
    if (! (o instanceof SimpleMacro)) return false;

    SimpleMacro other = (SimpleMacro)o;

    return (   (name.equals(other.name))
            && (opcode  == other.opcode)
            && (minArgs == other.minArgs)
            && (maxArgs == other.maxArgs));
  }

  /**
   * Return a hash code for this simple macro form.
   *
   * @return  A hash code for this simple macro form.
   */
  public int hashCode() {
    return name.hashCode();
  }

  // ============================= Methods ==============================

  /**
   * Apply this simple macro form on the specified arguments.
   *
   * @param   l          The arguments as a proper list.
   * @param   numArgs    The number of arguments in <code>l</code>.
   * @param   evaluator  The calling evaluator.
   * @return             The result of applying this simple
   *                     macro form on the specified arguments.
   * @throws  EvaluatorException
   *                     Signals an exceptional condition when
   *                     applying this simple macro form on the
   *                     specified arguments.
   */
  protected Object apply1(Pair l, int numArgs, Evaluator evaluator)
    throws EvaluatorException {

    Object       o;
    Symbol       s;
    
    switch(opcode) {

    case DEFMACRO:                                            // defmacro
      if (! evaluator.isTopLevel()) {
        throw new BadSyntaxException(
          "Illegal context for macro definition", Pair.cons(SYM_DEFMACRO, l));

      } else {
        o = l.car;
        if (! (o instanceof Symbol)) {
          throw new BadSyntaxException("Variable not a symbol for defmacro", o);
        }
        s = (Symbol)o;

        // define and begin can't be redefined.
        if (SYM_DEFINE == s) {
          throw new BadSyntaxException("Illegal to redefine define",
                                       Pair.cons(SYM_DEFINE, l));
        } else if (SYM_BEGIN == s) {
          throw new BadSyntaxException("Illegal to redefine begin",
                                       Pair.cons(SYM_DEFINE, l));
        }
        
        Environment env = evaluator.getCurrentEnvironment();
        
        // Create the syntactic closure.
        Pair    p       = (Pair)l.cdr;
        Object  formals = p.car;
        Pair    body    = (Pair)p.cdr;
        Closure closure = new Closure(formals, body, env, true);
        
        // Set its name.
        closure.setName(s.toString());
        
        // Bind it.
        try {
          env.bind(s, closure);
        } catch (UnsupportedOperationException x) {
          throw new BindingException(
            "Unable to create binding in sealed environment", env);
        }
        evaluator.returnValue();
        return s;
      }
    default:
      throw new Bug("Invalid opcode " + opcode + " for simple macro form "
                    + toString());
    }
  }

  // =========================== Initialization =========================

  /**
   * Install the simple macro forms in the specified environment.
   *
   * @param   env     The environment to install the simple
   *                  macro forms into.
   */
  public static void install(Environment env) {
    add(env, "defmacro",                   DEFMACRO,             3, -1);
  }

  /**
   * Create a new simple macro form as specified and add a binding
   * for it to the specified environment.
   *
   * @param   env       The environment for the new simple macro form.
   * @param   name      The name of the new simple macro form.
   * @param   opcode    The opcode of the new simple macro form.
   * @param   minArgs   The non-negative minimum number of arguments for
   *                    the new simple macro form.
   * @param   maxArgs   The non-negative maximum number of arguments for
   *                    the new simple macro form, or -1 if it takes an
   *                    unlimited maximum number of arguments.
   */
  private static void add(Environment env, String name, int opcode,
                          int minArgs, int maxArgs) {
    name          = name.intern();
    Symbol      s = Symbol.intern(name);
    SimpleMacro v = new SimpleMacro(name, opcode, minArgs, maxArgs);

    env.bind(s, v);
  }

}
