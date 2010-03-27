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
 * Implementation of a closure. A closure consists of a definition
 * environment, a body, and a possibly empty list of formals. On
 * application of a closure to some arguments, the formals are bound
 * to the arguments in an extension of the definition environment, and
 * the body of the closure is evaluated in this extended environment.
 *
 * <p>A closure can be either syntactic or not. The arguments to a
 * syntactic closure are not evaluated at application time, and the
 * result of evaluating the body is treated as an expression that is
 * evaluated in the dynamic environment of the closure
 * application. The arguments to a non-syntactic closure are evaluated
 * at application time, and the result of evaluating the body is the
 * value of applying the closure.</p>
 *
 * @author   &copy; Copyright 1998-2000 by Robert Grimm.
 *           Written by <a href="mailto:rgrimm@alum.mit.edu">Robert
 *           Grimm</a>.
 * @version  1.0
 */
public final class Closure extends AbstractApplicable {

  // ============================ Constants ===============================

  /** The <code>define</code> symbol. */
  private static final Symbol DEFINE = Symbol.intern("define");

  /** The <code>begin</code> symbol. */
  private static final Symbol BEGIN  = Symbol.intern("begin");

  // ========================== Internal state ============================

  /**
   * The formals for this closure.
   *
   * @serial  Must be <code>null</code>, if this closure takes no
   *          arguments. Otherwise, it must be a non-empty array of
   *          unique symbols whose size is<blockquote><pre>
   * minArgs + ((maxArgs == -1)? 1 : 0)
   * </pre></blockquote>
   */
  private Symbol[]    formals;

  /**
   * The body of this closure.
   *
   * @serial  Must be a proper list of length at least one.
   */
  private Pair        body;

  /**
   * The definition environment of this closure.
   *
   * @serial  Must not be <code>null</code>.
   */
  private Environment env;

  /**
   * Flag for whether internal bindings in the body of this closure
   * have been desugared.
   *
   * @serial
   */
  private boolean     bodyProcessed;

  // ========================== Constructors ============================

  /**
   * Create a new closure from the specified formals, body, and
   * environment. Parses the external, syntactic representation of the
   * formals in <code>lambda</code> syntax, and combines the result
   * with the specified body and the specified environment to create a
   * closure. The body is only checked for consistency, that is, it
   * must be a proper list of length at least one.
   *
   * <p>Note that this constructor correctly parses formals in the
   * three forms specified in &sect;4.1.4 of R<sup><font
   * size="-1">5</font></sup>RS.</p>
   * 
   * <p>Also note, that internal definitions in the body are correctly
   * desugared into an equivalent <code>letrec</code> form.</p>
   *
   * @param   f    The external representation of the formals in
   *               <code>lambda</code> syntax.
   * @param   b    The body for the new closure.
   * @param   env  The definition environment for the new closure.
   * @param   syntactic
   *               <code>true</code> iff the newly created closure
   *               is a syntactic closure.
   * @throws  BadSyntaxException
   *               Signals that <code>f</code> or <code>b</code> does
   *               not have the expected form, including that a formal
   *               is not a symbol or is duplicate, or that the body
   *               is the empty list.
   * @throws  NullPointerException
   *               Signals that<code>null == env</code>.
   */
  public Closure(Object f, Pair b, Environment env, boolean syntactic)
    throws BadSyntaxException {

    // Simple error checking.
    if (null == env) {
      throw new NullPointerException("Null environment");
    } else if (Pair.EMPTY_LIST == b) {
      throw new BadSyntaxException("Empty body", b);
    } else if (! b.isList()) {
      throw new BadSyntaxException("Body not a list", b);
    }

    // Store away environment.
    this.env = env;

    // Process formals.
    boolean defineOrBegin = false; // Have we seen define or begin?

    if (Pair.EMPTY_LIST == f) {
      // No formals, no arguments.
      this.formals = null;
      this.minArgs = 0;
      this.maxArgs = 0;

    } else if (f instanceof Symbol) {
      // One formal, variable number of arguments.
      this.formals    = new Symbol[1];
      this.formals[0] = (Symbol)f;
      this.minArgs    = 0;
      this.maxArgs    = -1;
      if ((DEFINE == f) || (BEGIN == f)) {
        defineOrBegin = true;
      }

    } else if (f instanceof Pair) {
      // Some formals, some arguments.
      Pair f1 = (Pair)f;
      if (f1.isCircular()) {
        throw new BadSyntaxException("Circular list for formals",
                                     f);
      }

      int l1 = f1.estimateLength();
      this.formals = new Symbol[l1];

      int i  = 0;
      do {
        Object o = f1.car;
        if (! (o instanceof Symbol)) {
          throw new BadSyntaxException("Formal not a symbol", o);
        } else {
          Symbol s = (Symbol)o;
          if ((DEFINE == s) || (BEGIN == s)) {
            defineOrBegin = true;
          }
          for (int j=0; j<i; j++) {
            if (this.formals[j] == s) {
              throw new BadSyntaxException("Duplicate formal", s);
            }
          }
          this.formals[i] = s;
        }

        o = f1.cdr;
        if (Pair.EMPTY_LIST == o) {
          // Some number of formals, same number of arguments.
          this.minArgs = i + 1;
          this.maxArgs = this.minArgs;
          break;

        } else if (o instanceof Pair) {
          // Continue to collect formals.
          f1 = (Pair)o;
          i++;
          continue;

        } else {
          // Some number of formals, the last for a variable number of args.
          if (! (o instanceof Symbol)) {
            throw new BadSyntaxException("Formal not a symbol", o);
          }
          Symbol s = (Symbol)o;
          if ((DEFINE == s) || (BEGIN == s)) {
            defineOrBegin = true;
          }
          for (int j=0; j<=i; j++) {
            if (this.formals[j] == s) {
              throw new BadSyntaxException("Duplicate formal", s);
            }
          }
          this.formals[i + 1] = s;
          this.minArgs = i + 1;
          this.maxArgs = -1;
          break;
        }
      } while (true);

    } else {
      throw new BadSyntaxException("Formals not a symbol/list", f);
    }

    // Process body.
    if (defineOrBegin) {
      this.body     = b;
      bodyProcessed = false;
    } else {
      this.body     = Syntax.processBody(b, env);
      bodyProcessed = true;
    }

    // Set up opcode.
    if (syntactic) {
      opcode  = MAX_OPCODE_MACRO;
    } else {
      opcode  = MAX_OPCODE_COMPOUND_PROCEDURE;
    }
  }

  /**
   * Create a new closure with the specified formals, body, and
   * environment. If <code>varArgs</code> is <code>true</code>, the
   * array of formals must contain at least one formal, and the newly
   * created closure takes an unlimited maximum number of arguments.
   *
   * <p>This constructor is intended to be used by syntactic
   * applicable entities that desugar some syntactic form into a
   * closure. Such applicable entities can thus avoid consing up the
   * particular syntax of a <code>lambda</code> form to invoke {@link
   * #Closure(Object,Pair,Environment,boolean)}.</p>
   *
   * <p>Note, that internal definitions in the body are correctly
   * desugared into an equivalent <code>letrec</code> form.</p>
   *
   * @param   formals  The array of formals for the new closure, or
   *                   <code>null</code> if the new closure takes no
   *                   arguments.
   * @param   body     The body of the new closure.
   * @param   env      The definition environment for the new
   *                   closure.
   * @param   varArgs  If <code>true</code>, the newly created
   *                   closure takes an unlimited maximum number
   *                   of arguments.
   * @param   syntactic
   *                   <code>true</code> iff the newly created
   *                   closure is a syntactic closure.
   * @throws  BadSyntaxException
   *                   Signals that a formal appears more than once
   *                   in <code>formals</code>, that the body is
   *                   empty or not a proper list.
   * @throws  IllegalArgumentException
   *                   Signals that <code>formals</code> is <code>null</code>
   *                   or not of length at least one if <code>varArgs</code>
   *                   is <code>true</code>.
   * @throws  NullPointerException
   *                   Signals that <code>null&nbsp;==&nbsp;env</code>, or
   *                   that an entry in a non-null <code>formals</code>
   *                   is <code>null</code>.
   */
  public Closure(Symbol[] formals, Pair body, Environment env,
                 boolean varArgs, boolean syntactic)
    throws BadSyntaxException {

    if (null == env) {
      throw new NullPointerException("Null environment");
    } else if (Pair.EMPTY_LIST == body) {
      throw new BadSyntaxException("Empty body", body);
    } else if (! body.isList()) {
      throw new BadSyntaxException("Body not a list", body);
    } else if (varArgs && ((null == formals) || (0 == formals.length))) {
      throw new IllegalArgumentException("Variable arguments without a formal");
    }

    // Store away environment.
    this.env  = env;

    // Process formals.
    boolean defineOrBegin = false; // Have we seen define or begin?

    if ((null == formals) || (0 == formals.length)) {
      // No formals, no arguments.
      this.formals = null;
      this.minArgs = 0;
      this.maxArgs = 0;

    } else {
      // Some formals, some arguments.
      this.formals   = (Symbol[])formals.clone();
      if (varArgs) {
        this.minArgs = formals.length - 1;
        this.maxArgs = -1;
      } else {
        this.minArgs = formals.length;
        this.maxArgs = formals.length;
      }

      // Check that formals are non-null and not duplicate.
      for (int i=0; i<formals.length; i++) {
        Symbol s = this.formals[i];
        if ((DEFINE == s) || (BEGIN == s)) {
          defineOrBegin = true;
        }
        if (null == s) {
          throw new NullPointerException("Null formal");
        } else {
          for (int j=0; j<i; j++) {
            if (s == this.formals[j]) {
              throw new BadSyntaxException("Duplicate formal", s);
            }
          }
        }
      }
    }

    // Set up body.
    if (defineOrBegin) {
      this.body     = body;
      bodyProcessed = false;
    } else {
      this.body     = Syntax.processBody(body, env);
      bodyProcessed = true;
    }

    // Set up opcode.
    if (syntactic) {
      opcode  = MAX_OPCODE_MACRO;
    } else {
      opcode  = MAX_OPCODE_COMPOUND_PROCEDURE;
    }
  }

  // ========================== Internal state ============================

  /**
   * Apply this closure on the specified arguments.
   *
   * @param   l          The arguments as a proper list.
   * @param   numArgs    The number of arguments in <code>l</code>
   * @param   evaluator  The calling evaluator.
   * @return             The result of applying this closure
   *                     on the specified arguments.
   * @throws  EvaluatorException
   *                     Signals an exceptional condition when
   *                     applying this closure to the specified
   *                     arguments.
   */
  protected Object apply1(Pair l, int numArgs, Evaluator evaluator)
    throws EvaluatorException {

    // Save dynamic environment.
    // We only need it for syntactic closures, but this is easier.
    Environment dynEnv = evaluator.getCurrentEnvironment();

    // Set up definition environment with bindings for formals...
    Environment e = env;

    if ((0 < minArgs) || (-1 == maxArgs)) {
      // Note: A new environment is guaranteed to not be sealed.
      e = new SimpleEnvironment(env, 
                                minArgs + ((-1 == maxArgs)? 1 : 0));

      int i=0;
      for ( ; i<minArgs; i++) {
        e.bind(formals[i], l.car);
        l = (Pair)l.cdr;
      }

      if (-1 == maxArgs) {
        e.bind(formals[i], l);
      }
    }

    // ...and make it the current environment.
    evaluator.setCurrentEnvironment(e);

    // Set up body.
    Pair body;
    if (bodyProcessed) {
      body = this.body;
    } else {
      body = Syntax.processBody(this.body, e);
    }

    // Return the desugared body for evaluation.
    if (MAX_OPCODE_MACRO == opcode) {
      boolean topLevel = evaluator.isTopLevel();
      evaluator.setTopLevel(false);
      return Pair.cons(new MacroContinuation(name, dynEnv, topLevel), body);
    } else if (Pair.EMPTY_LIST == body.cdr) {
      if (evaluator.isSimpleExpression(body.car)) {
        return evaluator.evalSimpleExpression(body.car);
      } else {
        evaluator.returnExpression();
        return body.car;
      }
    } else {
      evaluator.returnSequence();
      return body;
    }
  }

  /**
   * Set the name of this closure to the specified name. This method
   * only sets the name of this closure to the specified name, if the
   * name has not been set before, that is, if this method has not
   * been called before with a non-null argument.
   *
   * @param  name  The name for this closure.
   */
  public void setName(String name) {
    if (null == this.name) {
      this.name = name;
    }
  }

}
