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
 * Implementation of a primitive continuation for macros. As the name
 * implies, the intended use for this class is in the implementation
 * of simple macros. A macro continuation is a primitive continuation
 * which is applied to at least one expression. The application of the
 * macro continuation forces the expressions to be evaluated, which
 * are then passed as arguments to the macro continuation's
 * <code>apply()</code> method. The macro continuation then forces the
 * last argument to be evaluated in its environment.
 *
 * <p>An implementation of a simple macro, such as a syntactic {@link
 * Closure}, creates a new macro continuation with the dynamic
 * environment of the macro application. It then prepends the newly
 * created macro continuation to the body of the macro and returns
 * that expression to the evaluator for evaluation in the definition
 * environment of the macro. The macro continuation, in turn, restores
 * the dynamic environment of the macro application and returns the
 * resulting syntactic expression for evaluation in the dynamic
 * environment of the macro application.</p>
 *
 * @author   &copy; Copyright 1998-2000 by Robert Grimm.
 *           Written by <a href="mailto:rgrimm@alum.mit.edu">Robert
 *           Grimm</a>.
 * @version  1.0
 */
public final class MacroContinuation extends AbstractApplicable {

  /**
   * The environment of the macro continuation.
   *
   * @serial  Must not be <code>null</code>.
   */
  private Environment env;

  /**
   * Flag for whether the expression returned by this macro
   * continuation should be evaluated as a top-level expression.
   *
   * @serial
   */
  private boolean     topLevel;

  /**
   * Create a new macro continuation with the specified name,
   * specified environment, and specified top-level flag.
   *
   * @param  name  The name of the new macro continuation, which
   *               may be <code>null</code>.
   * @param  env   The environment of the macro continuation.
   * @param  topLevel
   *               Flag to indicate whether the expression returned
   *               by the new macro continuation's
   *               <code>apply()</code> method is to be evaluated
   *               as a top-level expression.
   */
  MacroContinuation(String name, Environment env, boolean topLevel) {
    this.name     = name;
    opcode        = MAX_OPCODE_PRIMITIVE_CONTINUATION;
    minArgs       = 1;
    maxArgs       = -1;
    this.env      = env;
    this.topLevel = topLevel;
  }

  /**
   * Apply this macro continuation on the specified arguments.
   *
   * @param   l          The arguments as a proper list.
   * @param   numArgs    The number of arguments in <code>l</code>
   * @param   evaluator  The calling evaluator.
   * @return             The last of the specified arguments as
   *                     an expression to be evaluated in the
   *                     environment of the macro continuation.
   */
  protected Object apply1(Pair l, int numArgs, Evaluator evaluator)
    throws EvaluatorException {

    while (Pair.EMPTY_LIST != l.cdr) {
      l = (Pair)l.cdr;
    }

    evaluator.setCurrentEnvironment(env);
    evaluator.returnExpression();
    evaluator.setTopLevel(topLevel);
    return l.car;
  }
}
