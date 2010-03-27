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
 * Implementation of the Scheme standard procedures supporting
 * explicit evaluation as defined in &sect;6.5 of R<sup><font
 * size="-1">5</font></sup>RS.
 *
 * @author   &copy; Copyright 1998-2000 by Robert Grimm.
 *           Written by <a href="mailto:rgrimm@alum.mit.edu">Robert
 *           Grimm</a>.
 * @version  1.0
 */
public final class Eval extends AbstractApplicable {

  // =========================== Constants =============================

  /** Opcodes. */
  private static final int
    EVAL = 30001, SCHEME_ENV = 30002, NULL_ENV = 30003,
    INTERACTION_ENV = 30004;

  // =========================== Constructor ===========================

  /**
   * Create a new eval operator with the specified name, opcode,
   * and number of arguments.
   *
   * @param  name     The name of the eval operator.
   * @param  opcode   The opcode of the eval operator.
   * @param  minArgs  The non-negative minimum number of arguments
   *                  for this eval operator.
   * @param  maxArgs  The non-negative maximum number of arguments
   *                  for this eval operator, or -1 if this
   *                  operator takes an unlimited maximum number
   *                  of arguments. In the former case,
   *                  <code>maxArgs >= minArgs</code> must be
   *                  <code>true</code>.
   */
  private Eval(String name, int opcode, int minArgs, int maxArgs) {
    this.name    = name;
    this.opcode  = opcode;
    this.minArgs = minArgs;
    this.maxArgs = maxArgs;
  }

  // ============================ Equality ================================

  /**
   * Determine whether this eval operator equals the specified
   * object.
   *
   * @param   o  The object with which to compare.
   * @return     <code>true</code> if this eval operator equals
   *             the specified object.
   */
  public boolean equals(Object o) {
    if (this == o) return true;
    if (! (o instanceof Eval)) return false;

    Eval other = (Eval)o;

    return (   (name.equals(other.name))
            && (opcode  == other.opcode)
            && (minArgs == other.minArgs)
            && (maxArgs == other.maxArgs));
  }

  /**
   * Return a hash code for this eval operator.
   *
   * @return  A hash code for this eval operator.
   */
  public int hashCode() {
    return name.hashCode();
  }

  // =========================== Application ===========================

  /**
   * Apply this eval operator on the specified arguments.
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

    if (1 <= numArgs) {
      o1 = l.getListRef(0);
    }
    if (2 <= numArgs) {
      o2 = l.getListRef(1);
    }

    // Do the deed.
    int i;

    switch (opcode) {
    case EVAL:
      if (! (o2 instanceof Environment)) {
        throw new BadTypeException("Not an environment", o2);
      }
      evaluator.setCurrentEnvironment((Environment)o2);
      evaluator.returnExpression();
      evaluator.setTopLevel(true);
      return o1;
    case SCHEME_ENV:
      i = Cast.toInt(o1);
      if (5 == i) {
        return new HashEnvironment(TopLevelEnvironment.R5RS_REPORT);
      } else {
        throw new BadArgumentException("Unrecognized Scheme report version",
                                       o1);
      }
    case NULL_ENV:
      i = Cast.toInt(o1);
      if (5 == i) {
        return new HashEnvironment(TopLevelEnvironment.R5RS_NULL);
      } else {
        throw new BadArgumentException("Unrecognized Scheme report version",
                                       o1);
      }
    case INTERACTION_ENV:
      return evaluator.getTopLevelEnvironment();

    default:
      throw new Bug("Invalid opcode " + opcode + " for eval operator" +
                    toString());
    }
  }

  // ========================== Installation ===========================

  /**
   * Install the eval operators defined in &sect;6.5 of R<sup><font
   * size="-1">5</font></sup>RS into the specified environment.
   *
   * @param   env     The environment to install the eval
   *                  operators into.
   */
  public static void install(Environment env) {
    add(env, "eval",                      EVAL,            2,  2);
    add(env, "scheme-report-environment", SCHEME_ENV,      1,  1);
    add(env, "null-environment",          NULL_ENV,        1,  1);
    add(env, "interaction-environment",   INTERACTION_ENV, 0,  0);
  }

  /**
   * Create a new eval operator as specified and add a binding
   * for it to the specified environment.
   *
   * @param   env       The environment for the new eval operator.
   * @param   name      The name of the new eval operator.
   * @param   opcode    The opcode of the new eval operator.
   * @param   minArgs   The non-negative minimum number of arguments for
   *                    the new eval operator.
   * @param   maxArgs   The non-negative maximum number of arguments for
   *                    the new eval operator, or -1 if it takes an
   *                    unlimited maximum number of arguments.
   */
  private static void add(Environment env, String name, int opcode,
                          int minArgs, int maxArgs) {
    name     = name.intern();
    Symbol s = Symbol.intern(name);
    Eval   v = new Eval(name, opcode, minArgs, maxArgs);

    env.bind(s, v);
  }

}
