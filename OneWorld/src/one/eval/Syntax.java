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

import java.util.List;
import java.util.ArrayList;

import one.util.Bug;

/**
 * Implementation of Scheme syntax forms. This class implements the
 * primitive expression types defined in &sect;4.1 of R<sup><font
 * size="-1">5</font></sup>RS, the derived expression types defined in
 * &sect;4.2, and definitions as defined in &sect;5.2. This class also
 * implements the primitive continuations necessary to implement some
 * of the other expression types.
 *
 * <p>Note that macro support as specified in &sect;4.3 and &sect;5.3
 * of R<sup><font size="-1">5</font></sup>RS has not been implemented
 * yet.</p>
 *
 * @author   &copy; Copyright 1998-2000 by Robert Grimm.
 *           Written by <a href="mailto:rgrimm@alum.mit.edu">Robert
 *           Grimm</a>.
 * @version  1.0
 */
public final class Syntax extends AbstractApplicable {

  // ============================ Constants ===============================

  /** The <code>else</code> symbol. */
  static final Symbol SYM_ELSE             = Symbol.intern("else");

  /** The <code>=&gt;</code> symbol. */
  static final Symbol SYM_ARROW            = Symbol.intern("=>");

  /** The <code>define</code> symbol. */
  private static final Symbol SYM_DEFINE   = Symbol.intern("define");

  /** The <code>begin</code> symbol. */
  private static final Symbol SYM_BEGIN    = Symbol.intern("begin");

  /** The <code>set!</code> symbol. */
  private static final Symbol SYM_SET      = Symbol.intern("set!");

  /** The <code>lambda</code> symbol. */
  private static final Symbol SYM_LAMBDA   = Symbol.intern("lambda");

  /** The <code>quasiquote</code> symbol. */
  private static final Symbol SYM_BACKTICK = Symbol.intern("quasiquote");

  /** The <code>unquote</code> symbol. */
  private static final Symbol SYM_COMMA    = Symbol.intern("unquote");

  /** The <code>unquote-splicing</code> symbol. */
  private static final Symbol SYM_COMMA_AT = Symbol.intern("unquote-splicing");

  private static final int
    // 4.1
    QUOTE = 1, LAMBDA = 2, IF = 3, SET = 4,

    // 4.2
    COND = 10, CASE = 11, AND = 12, OR = 13, LET = 14, LETSTAR = 15,
    LETREC = 16, BEGIN = 17, DO = 18, DELAY = 19, QUASIQUOTE = 20,

    // 4.3
    LET_SYNTAX = 30, LETREC_SYNTAX = 31, SYNTAX_RULES = 32,

    // 5.2
    DEFINE = 40, DEFINE_SYNTAX = 41;

    // Primitive continuations
  static final int
    CONT_IF = 60003, CONT_SET = 60004,
    CONT_COND = 60010, CONT_CASE = 60011,  CONT_AND = 60012, CONT_OR = 60013,
    CONT_LETSTAR = 60015, CONT_LETREC = 60016, CONT_DO = 60018,
    CONT_QUASIQUOTE = 60020,
    
    CONT_DEFINE = 60040,
    
    CONT_ARROW = 60060;

  /** Syntax <code>letrec</code>. */
  private static final Syntax LETREC_ME =
    new Syntax("letrec", LETREC, 2, -1);

  // ====================== Primitive Continuation ========================

  /** Implementation of a primitive syntax continuation. */
  static final class Continuation extends AbstractApplicable {
    /**
     * Internal helper object 1.
     *
     * @serial  Used by this syntax continuation to store its internal
     *          state. The invariants for <code>one</code> depend on the
     *          type of syntax continuation a specific instance of this
     *          class represents and therefore on <code>opcode</code>.
     *
     *          <p>The following invariants for this field,
     *          <code>two</code>, <code>three</code>, and
     *          <code>flag</code>, as well as for <code>minArgs</code>
     *          and <code>maxArgs</code> must hold for the specified
     *          opcodes:
     *          <ul>
     *          <li><code>CONT_IF</code> (60003): The <code>if</code>
     *          continuation takes exactly one argument which is the
     *          truth value for the test. <code>flag</code> must be
     *          either 1 or 2. <code>one</code> must contain the
     *          expression for the consequent. <code>two</code> must
     *          contain the expression for the alternate if
     *          <code>flag</code> is 2.
     *          </li>
     *
     *          <li><code>CONT_SET</code> (60004): The <code>set!</code>
     *          continuation takes exactly one argument which is the
     *          value for the binding it modifies. <code>one</code>
     *          must be the symbol for the binding.
     *          </li>
     *
     *          <li><code>CONT_COND</code> (60010): The <code>cond</code>
     *          continuation takes exactly one argument which is the
     *          value of the test currently being considered.
     *          <code>one</code> must hold a non-null list of clauses,
     *          where the first clause is the clause containing the
     *          test currently being considerd.
     *          </li>
     *
     *          <li><code>CONT_ARROW</code> (60060): The <code>=></code>
     *          continuation takes exactly one argument which is the
     *          applicable to apply on the value of the test
     *          expression which is held in <code>one</code>.
     *          </li>
     *
     *          <li><code>CONT_CASE</code> (60011): The <code>case</code>
     *          continuation takes exactly one argument which is the
     *          value of the key. <code>one</code> must hold a
     *          non-null list of clauses, which are all clauses of the
     *          <code>case</code> form.
     *          </li>
     *
     *          <li><code>CONT_AND</code> (60012): The <code>and</code>
     *          continuation takes exactly one argument which is the
     *          current expression. <code>one</code> holds the
     *          remainder of the sequences of expressions not yet
     *          evaluated.
     *          </li>
     *
     *          <li><code>CONT_OR</code> (60013): The <code>or</code>
     *          continuation takes exactly one argument which is the
     *          current expression. <code>one</code> holds the
     *          remainder of the sequences of expressions not yet
     *          evaluated.
     *          </li>
     *
     *          <li><code>CONT_LETSTAR</code> (60015): The <code>let*</code>
     *          continuation takes exactly one argument which is the
     *          value for the next binding. <code>one</code> holds
     *          a non-null array of symbols, <code>two</code> holds
     *          the list of expressions still to bind, <code>three</code>
     *          holds the body of the <code>let*</code> form, and
     *          <code>flag</code> is the index into <code>one</code>
     *          for the next symbol to bind.
     *          </li>
     *
     *          <li><code>CONT_LETREC</code> (60016): The <code>letrec</code>
     *          continuation takes as many arguments as there are
     *          bindings it creates. The arguments are the values for
     *          the bindings. <code>one</code> holds a non-null array
     *          of symbols, and <code>two</code> holds the body of the
     *          <code>letrec</code> form.
     *          </li>
     *
     *          <li><code>CONT_DO</code> (60018): The <code>do</code>
     *          continuation takes varying number of arguments, depending
     *          on its current state in <code>flag</code>. <code>one</code>
     *          must be a pair whose car is a non-null array of symbols
     *          representing the variables of the <code>do</code>
     *          expression and whose cdr is the definition environment
     *          of the <code>do</code> form. <code>two</code> holds a
     *          list of step expressions, in order of the variables in
     *          the array of variables and as long as there are variables.
     *          <code>three</code> holds a pair whose car holds the
     *          test expression of the <code>do</code> form and whose
     *          cdr holds the commands of the <code>do</code> form.
     *          If <code>flag</code> is 1, the continuation expects
     *          as many arguments as there are variables which are the
     *          values for these variables on the next iteration. If
     *          <code>flag</code> is 2, the continuation expects exactly
     *          one argument which is the value for the test. If
     *          <code>flag</code> is 3, the continuation expects as
     *          many arguments as there are expressions in the list of
     *          commands.
     *          </li>
     *
     *          <li><code>CONT_QUASIQUOTE</code> (60020): The
     *          <code>quasiquote</code> continuation takes as many
     *          arguments as there are expressions in the
     *          <code>quasiquote</code> template that need to be
     *          evaluated. It is only created and invoked if there is
     *          at least one such expression. <code>one</code> holds
     *          the template, and <code>flag</code> indicates
     *          whether <code>unquote</code> (bit 0 is set) and
     *          <code>unquote-splicing</code> (bit 1 is set) are
     *          unbound.
     *          </li>
     *
     *          <li><code>CONT_DEFINE</code> (60040: The <code>define</code>
     *          continuation takes exactly one argument which is the
     *          value for the binding it creates/modifies. <code>one</code>
     *          must be the symbol for the binding.
     *          </li>
     *          </ul>
     *          If <code>opcode</code> has a value not specified in
     *          this list, the syntax object must not use
     *          the fields <code>one</code>, <code>two</code>,
     *          <code>three</code>, and <code>flag</code> in any way.</p>
     */
    Object      one;
    
    /**
     * Internal helper object 2.
     *
     * @serial  Used by this syntax continuation to store its internal
     *          state. The invariants for <code>two</code> depend on the
     *          type of syntax continuation a specific instance of this
     *          class represents and therefore on <code>opcode</code>.
     *          See description for <code>one</code>.
     */
    Object      two;
    
    /**
     * Internal helper object 3.
     *
     * @serial  Used by this syntax continuation to store its internal
     *          state. The invariants for <code>three</code> depend on
     *          the type of syntax continuation a specific instance of
     *          this class represents and therefore on
     *          <code>opcode</code>. See description for
     *          <code>one</code>.
     */
    Object      three;
    
    /**
     * Internal flag.
     *
     * @serial  Used by this syntax continuation to store its internal
     *          state. The invariants for <code>flag</code> depend on
     *          the type of syntax continuation a specific instance of
     *          this class represents and therefore on
     *          <code>opcode</code>. See description for
     *          <code>one</code>.
     */
    int        flag;
    
    /**
     * Create a new syntax continuation with the specified name,
     * opcode and argument number restrictions.
     *
     * @param   name     The name for the new syntax continuation.
     * @param   opcode   The opcode for the new syntax continuation.
     * @param   minArgs  The non-negative minimum number of arguments
     *                   for the new syntax continuation.
     * @param   maxArgs  The non-negative maximum number of arguments
     *                   for the new syntax continuation, or -1 if the
     *                   new syntax continuation takes an unlimited number
     *                   of arguments.
     */
    Continuation(String name, int opcode, int minArgs, int maxArgs) {
      this.name    = name;
      this.opcode  = opcode;
      this.minArgs = minArgs;
      this.maxArgs = maxArgs;
    }

    /**
     * Apply this syntax continuation on the specified arguments.
     *
     * @param   l          The arguments as a proper list.
     * @param   numArgs    The number of arguments in <code>l</code>.
     * @param   evaluator  The calling evaluator.
     * @return             The result of applying this syntax
     *                     continuation on the specified arguments.
     * @throws  EvaluatorException
     *                     Signals an exceptional condition when
     *                     applying this syntax continuation on the
     *                     specified arguments.
     */
    protected Object apply1(Pair l, int numArgs, Evaluator evaluator)
      throws EvaluatorException {
      
      Object o1 = null;
      if (1 <= numArgs) {
        o1 = l.car;
      }
      
      Environment env;
      
      switch(opcode) {
        
      case CONT_IF:
        if (Cast.toBoolean(o1)) {
          evaluator.returnExpression();
          return one;
        } else {
          if (2 == flag) {
            evaluator.returnExpression();
            return two;
          } else {
            return Boolean.FALSE;
          }
        }
      case CONT_SET:
        env = evaluator.getCurrentEnvironment();
        try {
          o1 = env.modify((Symbol)one, o1);
        } catch (UnsupportedOperationException x) {
          throw new BindingException("Binding sealed for", one);
        }
        if (Environment.Uninitialized.MARKER == o1) {
          return Boolean.FALSE;
        } else {
          return o1;
        }
      case CONT_COND:
        return continueCond(o1, evaluator);
      case CONT_CASE:
        return continueCase(o1, evaluator);
      case CONT_AND:
        return continueAnd(o1, evaluator);
      case CONT_OR:
        return continueOr(o1, evaluator);
      case CONT_LETSTAR:
        return continueLetstar(o1, evaluator);
      case CONT_LETREC:
        return continueLetrec(l, evaluator);
      case CONT_DO:
        return continueDo(l, evaluator);
      case CONT_QUASIQUOTE:
        return continueQuasiquote(l, evaluator);
      case CONT_DEFINE:
        if (o1 instanceof Closure) {
          ((Closure)o1).setName(((Symbol)one).toString());
        }
        env = evaluator.getCurrentEnvironment();
        try {
          env.bind((Symbol)one, o1);
        } catch (UnsupportedOperationException x) {
          throw new BindingException(
            "Unable to create binding in sealed environment", env);
        }
        return one;
      case CONT_ARROW:
        evaluator.returnApplication(Cast.toProcedure(o1),
                                    Pair.cons(one, Pair.EMPTY_LIST),
                                    null);
        return Boolean.FALSE;

      default:
        throw new Bug("Invalid opcode " + opcode +
                      " for syntax continuation " + toString());
      }
    }

    /**
     * Evaluate the <code>cond</code> continuation.
     *
     * @param   o          The argument to the <code>cond</code>
     *                     continuation.
     * @param   evaluator  The calling evaluator.
     * @return             The result of evaluating the <code>cond</code>
     *                     continuation.
     * @throws  EvaluatorException
     *                     Signals an exceptional condition when
     *                     evaluating the <code>cond</code> continuation.
     */
    private Object continueCond(Object o, Evaluator evaluator)
      throws EvaluatorException {
      
      Environment env          = evaluator.getCurrentEnvironment();
      boolean     elseUnbound  = (! env.isBound(SYM_ELSE));
      boolean     arrowUnbound = (! env.isBound(SYM_ARROW));
      Pair        p            = (Pair)one;
      
      // o is the value.
      do {
        // Extract the clause from one.
        Pair clause = (Pair)p.car;
        
        if (Cast.toBoolean(o)) {
          // True.
          
          Pair exprs = (Pair)clause.cdr;
          if (Pair.EMPTY_LIST == exprs) {
            // Return value of test.
            return o;
            
          } else if ((SYM_ARROW == exprs.car) && arrowUnbound) {
            // => expression.
            
            Object proc = exprs.cdr;
            if (Pair.EMPTY_LIST == proc) {
              throw new BadSyntaxException("=> not followed by expression",
                                           clause);
              
            } else if (Pair.EMPTY_LIST != ((Pair)proc).cdr) {
              throw new BadSyntaxException(
                "=> followed by more than one expression", clause);
            
            } else {
              proc = ((Pair)proc).car;
              
              // Create arrow continuation and fire it off.
              Continuation cont = new Continuation("=>", CONT_ARROW, 1, 1);
              cont.one          = o;
              
              evaluator.returnExpression();
              return Pair.cons(cont, Pair.cons(proc, Pair.EMPTY_LIST));
            }
            
          } else if (Pair.EMPTY_LIST == exprs.cdr) {
            // Just one expression.
            evaluator.returnExpression();
            return exprs.car;
            
          } else {
            // A sequence of expressions.
            evaluator.returnSequence();
            return exprs;
          }
        }
        
        // Next clause.
        p = (Pair)p.cdr;
        
        if (Pair.EMPTY_LIST == p) {
          // We are done.
          return Boolean.FALSE;
        }
        
        Object c = p.car;
        if (! (c instanceof Pair)) {
          throw new BadSyntaxException("Cond clause not a list", c);
        } else if (! (clause = (Pair)c).isList()) {
          throw new BadSyntaxException("Cond clause not a list", c);
        }
        
        Object test = clause.car;
        if ((SYM_ELSE == test) && elseUnbound) {
          // Handle else clause.
          
          if (Pair.EMPTY_LIST != p.cdr) {
            // Must be the last clause.
            throw new BadSyntaxException("Else clause not last clause", p);
            
          } else {
            Pair exprs = (Pair)clause.cdr;
            if (Pair.EMPTY_LIST == exprs) {
              // Must be followed by at least one expression.
              throw new BadSyntaxException("Empty else clause", clause);
              
            } if (Pair.EMPTY_LIST == exprs.cdr) {
              evaluator.returnExpression();
              return exprs.car;
              
            } else {
              evaluator.returnSequence();
              return exprs;
            }
          }
          
        } else if (evaluator.isSimpleExpression(test)) {
          // Evaluate simple expression and do another round.
          o = evaluator.evalSimpleExpression(test);
          continue;
          
        } else {
          // Set up this and fire it off again.
          one = p;
          evaluator.returnExpression();
          return Pair.cons(this, Pair.cons(test, Pair.EMPTY_LIST));
        }
        
      } while (true);
    }
    
    /**
     * Evaluate the <code>case</code> continuation.
     *
     * @param   o          The argument to the <code>case</code>
     *                     continuation.
     * @param   evaluator  The calling evaluator.
     * @return             The result of evaluating the
     *                     <code>case</code> continuation.
     * @throws  BadSyntaxException
     *                     Signals an invalid <code>case</code>
     *                     clause.
     */
    private Object continueCase(Object o, Evaluator evaluator)
      throws BadSyntaxException {
      
      boolean elseUnbound =
        (! evaluator.getCurrentEnvironment().isBound(SYM_ELSE));
      Pair    p           = (Pair)one;

      do {
        Object c = p.car;
        Pair   clause;
        if (! (c instanceof Pair)) {
          throw new BadSyntaxException("Case clause not a list", c);
        } else if (! (clause = (Pair)c).isList()) {
          throw new BadSyntaxException("Case clause not a list", c);
        }
        
        Object d = clause.car;
        Pair   datum;
        if (Pair.EMPTY_LIST == d) {
          // Nothing to do.
          
        } else if ((SYM_ELSE == d) && elseUnbound) {
          // Handle else clause.
          if (Pair.EMPTY_LIST != p.cdr) {
            // Must be the last clause.
            throw new BadSyntaxException("Else clause not last clause", p);
            
          } else {
            Pair exprs = (Pair)clause.cdr;
            if (Pair.EMPTY_LIST == exprs) {
              // Must be followed by at least one expression.
              throw new BadSyntaxException("Empty else clause", clause);
              
            } else if (Pair.EMPTY_LIST == exprs.cdr) {
              evaluator.returnExpression();
              return exprs.car;
              
            } else {
              evaluator.returnSequence();
              return exprs;
            }
          }
          
        } else if (! (d instanceof Pair)) {
          throw new BadSyntaxException("Datums not a list", d);
          
        } else if (! (datum = (Pair)d).isList()) {
          throw new BadSyntaxException("Datums not a list", d);
          
        } else {
          // Does one datum match?
          Object r;
          try {
            r = datum.member(o, Equivalence.EQV);
          } catch (BadPairStructureException x) {
            throw new Bug("Unexpected exception: " + x.toString());
          }
          
          if (Pair.EMPTY_LIST != r) {
            // Fire off expressions.
            Pair exprs = (Pair)clause.cdr;
            
            if (Pair.EMPTY_LIST == exprs) {
              // Must be followed by at least one expression.
              throw new BadSyntaxException("No expression in case clause",
                                           clause);
              
            } else if (Pair.EMPTY_LIST == exprs.cdr) {
              evaluator.returnExpression();
              return exprs.car;
              
            } else {
              evaluator.returnSequence();
              return exprs;
            }
          }
        }
        
        // Next clause.
        p = (Pair)p.cdr;
      } while (Pair.EMPTY_LIST != p);
      
      return Boolean.FALSE;
    }

    /**
     * Evaluate <code>and</code> continuation.
     *
     * @param   o          The argument to the <code>and</code>
     *                     continuation.
     * @param   evaluator  The calling evaluator.
     * @return             The result of evaluating the
     *                     <code>and</code> continuation.
     * @throws  EvaluatorException
     *                     Signals an exceptional condition when
     *                     evaluating the <code>and</code>
     *                     continuation.
     */
    private Object continueAnd(Object o, Evaluator evaluator)
      throws EvaluatorException {
      
      do {
        if (! Cast.toBoolean(o)) {
          // Return first #f value.
          return o;
        } else if (Pair.EMPTY_LIST == one) {
          // Last #t value.
          return o;
        }
        
        // o must be a #t value.
        
        Pair p = (Pair)one;
        o      = p.car;
        one    = p.cdr;
        
        if (evaluator.isSimpleExpression(o)) {
          // Evaluate simple expression right here.
          o = evaluator.evalSimpleExpression(o);
          continue;
          
        } else {
          // Apply the continuation again.
          evaluator.returnExpression();
          return Pair.cons(this, Pair.cons(o, Pair.EMPTY_LIST));
        }
      } while (true);
    }
        
    /**
     * Evaluate <code>or</code> continuation.
     *
     * @param   o          The argument to the <code>or</code>
     *                     continuation.
     * @param   evaluator  The calling evaluator.
     * @return             The result of evaluating the
     *                     <code>or</code> continuation.
     * @throws  EvaluatorException
     *                     Signals an exceptional condition when
     *                     evaluating the <code>or</code> continuation.
     */
    private Object continueOr(Object o, Evaluator evaluator)
      throws EvaluatorException {
      
      do {
        if (Cast.toBoolean(o)) {
          // Return first #t value.
          return o;
        } else if (Pair.EMPTY_LIST == one) {
          // Last #f value.
          return o;
        }
        
        // o must be a #f value.
        
        Pair p = (Pair)one;
        o      = p.car;
        one    = p.cdr;
        
        if (evaluator.isSimpleExpression(o)) {
          // Evaluate simple expression right here.
          o = evaluator.evalSimpleExpression(o);
          continue;
          
        } else {
          // Apply the continuation again.
          evaluator.returnExpression();
          return Pair.cons(this, Pair.cons(o, Pair.EMPTY_LIST));
        }
      } while (true);
    }
    
    /**
     * Evaluate the <code>let*</code> continuation.
     *
     * @param   o          The argument to the <code>let*</code>
     *                     continuation.
     * @param   evaluator  The calling evaluator.
     * @return             The result of evaluating the
     *                     <code>let*</code> continuation.
     * @throws  BadSyntaxException
     *                     Signals a malformed body.
     */
    private Object continueLetstar(Object o, Evaluator evaluator)
      throws BadSyntaxException {
      
      Symbol[] formals     = (Symbol[])one;
      Pair     expressions = (Pair)two;
      
      // Process current binding.
      Environment env1 = evaluator.getCurrentEnvironment();
      Environment env2 = new SimpleEnvironment(env1, 1);
      env2.bind(formals[flag], o);
      evaluator.setCurrentEnvironment(env2);
      
      // More bindings to work on?
      if (Pair.EMPTY_LIST == expressions) {
        // Evaluate body.
        Pair body = (Pair)three;
        
        body = Syntax.processBody(body, env2);
        
        if (Pair.EMPTY_LIST == body.cdr) {
          evaluator.returnExpression();
          return body.car;
        } else {
          evaluator.returnSequence();
          return body;
        }
        
      } else {
        // Process next binding.
        two = expressions.cdr;
        flag++;
        evaluator.returnExpression();
        return Pair.cons(this, Pair.cons(expressions.car, Pair.EMPTY_LIST));
      }
    }
    
    /**
     * Evaluate the <code>letrec</code> continuation.
     *
     * @param   l          The arguments to the <code>letrec</code>
     *                     continuation.
     * @param   evaluator  The calling evaluator.
     * @return             The result of evaluating the
     *                     <code>letrec</code> continuation.
     * @throws  BadSyntaxException
     *                     Signals a malformed body.
     */
    private Object continueLetrec(Pair l, Evaluator evaluator)
      throws BadSyntaxException {
      
      Symbol[]    formals = (Symbol[])one;
      Pair        body    = (Pair)two;
      Environment env     = evaluator.getCurrentEnvironment();
      
      // Unseal the environment.
      env.unseal();
      
      // Modify all bindings to the evaluated values.
      for (int i=0; i<formals.length; i++) {
        try {
          env.modify(formals[i], l.car);
        } catch (BindingException x) {
          throw new Bug("Unexpected exception: " + x.toString());
        }
        l = (Pair)l.cdr;
      }
      
      // Process body in the current environment.
      body = Syntax.processBody(body, env);
      
      // Evaluate the body.
      if (Pair.EMPTY_LIST == body.cdr) {
        evaluator.returnExpression();
        return body.car;
      } else {
        evaluator.returnSequence();
        return body;
      }
    }
    
    /**
     * Evaluate the <code>do</code> continuation.
     *
     * @param   l          The arguments to the <code>do</code>
     *                     continuation.
     * @param   evaluator  The calling evaluator.
     * @return             The result of evaluating the
     *                     <code>do</code> continuation.
     */
    private Object continueDo(Pair l, Evaluator evaluator) {
      Symbol[]    vars;
      Environment env;
      Pair        steps;
      Pair        test;
      Pair        cmds;
      
      switch (flag) {
        
      case 1:                                         // Set variables.
        vars = (Symbol[])((Pair)one).car;
        env  = (Environment)((Pair)one).cdr;
        test = (Pair)((Pair)three).car;
        
        // Create bindings.
        if (0 != vars.length) {
          SimpleEnvironment e = new SimpleEnvironment(env, vars.length);
          
          for (int i=0; i<vars.length; i++) {
            e.bind(vars[i], l.car);
            
            l = (Pair)l.cdr;
          }
          
          evaluator.setCurrentEnvironment(e);
        }
        
        // Evaluate test.
        Object t = test.car;
        minArgs  = 1;
        maxArgs  = 1;
        flag     = 2;
        
        evaluator.returnExpression();
        return Pair.cons(this, Pair.cons(t, Pair.EMPTY_LIST));
        
      case 2:                                         // Handle test.
        Object v = l.car;
        
        if (Cast.toBoolean(v)) {
          // Test evaluates to true, wrap up do.
          test       = (Pair)((Pair)three).car;
          Pair exprs = (Pair)test.cdr;
          
          if (Pair.EMPTY_LIST == exprs) {
            return v;
            
          } else if (Pair.EMPTY_LIST == exprs.cdr) {
            evaluator.returnExpression();
            return exprs.car;
            
          } else {
            evaluator.returnSequence();
            return exprs;
          }
          
        } else {
          // Test evaluates to false, handle commands.
          cmds = (Pair)((Pair)three).cdr;
          
          if (Pair.EMPTY_LIST == cmds) {
            vars  = (Symbol[])((Pair)one).car;
            steps = (Pair)two;
            
            // No commands, evaluate the step expressions.
            minArgs = vars.length;
            maxArgs = vars.length;
            flag    = 1;
            
            evaluator.returnExpression();
            return Pair.cons(this, steps);
            
          } else {
            // Determine number of commands.
            int cmdsLength;
            try {
              cmdsLength = cmds.length();
            } catch (BadPairStructureException x) {
              throw new Bug("Unexpected exception: " + x.toString());
            }
            
            // Evaluate commands.
            minArgs = cmdsLength;
            maxArgs = cmdsLength;
            flag    = 3;
            
            evaluator.returnExpression();
            return Pair.cons(this, cmds);
          }
        }
        
      case 3:                                         // Handle commands.
        vars  = (Symbol[])((Pair)one).car;
        steps = (Pair)two;
        
        /*
         * Ignore arguments which are the values of the executed
         * commands, and fire off another iteration by evaluating the
         * step expressions.
         */
        minArgs = vars.length;
        maxArgs = vars.length;
        flag    = 1;
        
        evaluator.returnExpression();
        return Pair.cons(this, steps);
        
      default:
        throw new Bug("Unrecognized flag for " + toString());
      }
    }
    
    /**
     * Evaluate the <code>quasiquote</code> continuation.
     *
     * @param   l          The arguments to the <code>quasiquote</code>
     *                     continuation.
     * @param   evaluator  The calling evaluator.
     * @return             The result of evaluating the
     *                     <code>quasiquote</code> continuation.
     * @throws  BadTypeException
     *                     Signals that the result of evaluating a
     *                     <code>unquote-splicing</code> expression
     *                     is not a pair.
     * @throws  BadArgumentException
     *                     Signals that the result of evaluating a
     *                     <code>unquote-splicing</code> expression
     *                     is not a list.
     */
    private Object continueQuasiquote(Pair l, Evaluator evaluator)
      throws BadTypeException, BadArgumentException {
      
      // Restore boolean for whether , and ,@ are unbound.
      boolean commaUnbound   = ((flag & 0x01) == 0x01);
      boolean commaAtUnbound = ((flag & 0x02) == 0x02);
      
      // Convert the proper list of values to an array list of values.
      ArrayList values;
      
      try {
        values = l.toArrayList();
      } catch (BadPairStructureException x) {
        throw new Bug("Unexpected exception: " + x.toString());
      }
      
      // Walk the template with the values, creating a filled in copy
      // of it.
      
      Object result = Syntax.walkTick2(one, commaUnbound, commaAtUnbound,
                                       0, values);
      
      return result;
    }
    
  }

  // =========================== Constructor ============================

  /**
   * Create a new syntax form with the specified name, opcode and
   * argument number restrictions.
   *
   * @param   name     The name for the new syntax form.
   * @param   opcode   The opcode for the new syntax form.
   * @param   minArgs  The non-negative minimum number of arguments
   *                   for the new syntax form.
   * @param   maxArgs  The non-negative maximum number of arguments
   *                   for the new syntax form, or -1 if the new
   *                   syntax form takes an unlimited number of
   *                   arguments.
   */
  private Syntax(String name, int opcode, int minArgs, int maxArgs) {
    this.name    = name;
    this.opcode  = opcode;
    this.minArgs = minArgs;
    this.maxArgs = maxArgs;
  }

  // ============================ Equality ================================

  /**
   * Determine whether this syntax form equals the specified
   * object.
   *
   * @param   o  The object with which to compare.
   * @return     <code>true</code> if this syntax form equals
   *             the specified object.
   */
  public boolean equals(Object o) {
    if (this == o) return true;
    if (! (o instanceof Syntax)) return false;

    Syntax other = (Syntax)o;

    return (   (name.equals(other.name))
            && (opcode  == other.opcode)
            && (minArgs == other.minArgs)
            && (maxArgs == other.maxArgs));
  }

  /**
   * Return a hash code for this syntax form.
   *
   * @return  A hash code for this syntax form.
   */
  public int hashCode() {
    return name.hashCode();
  }

  // ============================= Methods ==============================

  /**
   * Apply this syntax form on the specified arguments.
   *
   * @param   l          The arguments as a proper list.
   * @param   numArgs    The number of arguments in <code>l</code>.
   * @param   evaluator  The calling evaluator.
   * @return             The result of applying this syntax
   *                     form on the specified arguments.
   * @throws  EvaluatorException
   *                     Signals an exceptional condition when
   *                     applying this syntax form on the
   *                     specified arguments.
   */
  protected Object apply1(Pair l, int numArgs, Evaluator evaluator)
    throws EvaluatorException {

    Object       o;
    Symbol       s;
    
    switch(opcode) {
    case QUOTE:                                               // quote
      o = l.car;
      // Quoted pairs and vectors effectively become literals.
      if (o instanceof Pair) {
        ((Pair)o).turnIntoLiteral();
      } else if (o instanceof Vector) {
        ((Vector)o).turnIntoLiteral();
      }
      evaluator.returnValue();
      return o;
      
    case LAMBDA:                                              // lambda
      {
        Object  formals = l.car;
        Pair    body    = (Pair)l.cdr;
        Closure closure = new Closure(formals, body,
                                      evaluator.getCurrentEnvironment(),
                                      false);
        evaluator.returnValue();
        return closure;
      }

    case IF:                                                  // if
      o = l.car;
      if (evaluator.isSimpleExpression(o)) {
        boolean b = Cast.toBoolean(evaluator.evalSimpleExpression(o));
        if (b) {
          evaluator.setTopLevel(false);
          return l.cxr("ad");
        } else {
          if (2 == numArgs) {
            evaluator.returnValue();
            return Boolean.FALSE;
          } else {
            evaluator.setTopLevel(false);
            return l.cxr("add");
          }
        }
        
      } else {
        Continuation cont = new Continuation("if", CONT_IF, 1, 1);
        cont.flag         = numArgs - 1;
        cont.one          = l.cxr("ad");
        if (2 == cont.flag) {
          cont.two        = l.cxr("add");
        }

        evaluator.setTopLevel(false);
        return Pair.cons(cont, Pair.cons(o, Pair.EMPTY_LIST));
      }
      
    case SET:                                                 // set!
      o = l.car;
      if (! (o instanceof Symbol)) {
        throw new BadSyntaxException("Variable not a symbol for set!", o);
      }
      s   = (Symbol)o;
      
      // Handle new value.
      o = l.cxr("ad");
      if (evaluator.isSimpleExpression(o)) {
        o = evaluator.evalSimpleExpression(o);
        Environment env = evaluator.getCurrentEnvironment();
        try {
          o = env.modify(s, o);
        } catch (UnsupportedOperationException x) {
          throw new BindingException("Binding sealed for", s);
        }
        evaluator.returnValue();
        if (Environment.Uninitialized.MARKER == o) {
          return Boolean.FALSE;
        } else {
          return o;
        }
        
      } else {
        Continuation cont = new Continuation("set!", CONT_SET, 1, 1);
        cont.one          = s;

        evaluator.setTopLevel(false);
        return Pair.cons(cont, Pair.cons(o, Pair.EMPTY_LIST));
      }
      
    case DEFINE:                                              // define
      if (! evaluator.isTopLevel()) {
        throw new BadSyntaxException(
          "Illegal context for definition", Pair.cons(SYM_DEFINE, l));
      }
      
      o = l.car;
      if (o instanceof Pair) {
        // Handle lambda definitions here.
        Pair   q  = (Pair)o;
        Object o2 = q.car;  // Procedure name.
        if (! (o2 instanceof Symbol)) {
          throw new BadSyntaxException(
            "Procedure name not a symbol for define", o2);
        }
        s       = (Symbol)o2;
        
        // define and begin can't be redefined.
        if (SYM_DEFINE == s) {
          throw new BadSyntaxException("Illegal to redefine define",
                                       Pair.cons(SYM_DEFINE, l));
        } else if (SYM_BEGIN == s) {
          throw new BadSyntaxException("Illegal to redefine begin",
                                       Pair.cons(SYM_DEFINE, l));
        }
        
        Environment env = evaluator.getCurrentEnvironment();
        
        // Create the procedure.
        Object  formals = q.cdr;
        Pair    body    = (Pair)l.cdr;
        Closure closure = new Closure(formals, body, env, false);
        
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
        
      } else if (! (o instanceof Symbol)) {
        throw new BadSyntaxException("Variable not a symbol for define", o);
      } else if (2 < numArgs) {
        throw new BadSyntaxException("Too many forms (" + numArgs + ") for",
                                     this);
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
      
      o = l.cxr("ad");
      if (evaluator.isSimpleExpression(o)) {
        o               = evaluator.evalSimpleExpression(o);
        Environment env = evaluator.getCurrentEnvironment();
        try {
          env.bind(s, o);
        } catch (UnsupportedOperationException x) {
          throw new BindingException(
            "Unable to create binding in sealed environment", env);
        }
        evaluator.returnValue();
        return s;
      } else {
        Continuation cont = new Continuation("define", CONT_DEFINE, 1, 1);
        cont.one          = s;

        evaluator.setTopLevel(false);
        return Pair.cons(cont, Pair.cons(o, Pair.EMPTY_LIST));
      }
    case COND:
      return cond(l, evaluator);
    case CASE:
      return doCase(l, evaluator);
    case AND:
      return and(l, numArgs, evaluator);
    case OR:
      return or(l, numArgs, evaluator);
    case LET:
      return let(l, numArgs, evaluator);
    case LETSTAR:
      return letstar(l, evaluator);
    case LETREC:
      return letrec(l, evaluator);
    case BEGIN:
      return begin(l, numArgs, evaluator);
    case DO:
      return doDo(l, evaluator);
    case DELAY:
      evaluator.returnValue();
      return new Promise(l.car, evaluator.getCurrentEnvironment());
    case QUASIQUOTE:
      return quasiquote(l, evaluator);
    case LET_SYNTAX:
      
    case LETREC_SYNTAX:
      
    case SYNTAX_RULES:
      
    case DEFINE_SYNTAX:
      throw new EvaluatorException("Not implemented", this);
    default:
      throw new Bug("Invalid opcode " + opcode + " for primitive syntax "
                    + toString());
    }
  }

  // ==================== Derived expression types ======================

  /**
   * Evaluate <code>cond</code> syntax form.
   *
   * @param   p          The <code>cond</code> form.
   * @param   evaluator  The calling evaluator.
   * @return             The result of evaluating the
   *                     <code>cond</code> form.
   * @throws  EvaluatorException
   *                     Signals an exceptional condition when
   *                     evaluating the specified <code>cond</code> form.
   */
  private Object cond(Pair p, Evaluator evaluator)
    throws EvaluatorException {

    Environment env          = evaluator.getCurrentEnvironment();
    boolean     elseUnbound  = (! env.isBound(SYM_ELSE));
    boolean     arrowUnbound = (! env.isBound(SYM_ARROW));

    // Process clauses as far as possible.
    do {
      Object c = p.car;
      Pair   clause;
      if (! (c instanceof Pair)) {
        throw new BadSyntaxException("Cond clause not a list", c);
      } else if (! (clause = (Pair)c).isList()) {
        throw new BadSyntaxException("Cond clause not a list", c);
      }

      Object test = clause.car;
      if ((SYM_ELSE == test) && elseUnbound) {
        // Handle else clause.

        if (Pair.EMPTY_LIST != p.cdr) {
          // Must be the last clause.
          throw new BadSyntaxException("Else clause not last clause", p);

        } else {
          Pair exprs = (Pair)clause.cdr;
          if (Pair.EMPTY_LIST == exprs) {
            // Must be followed by at least one expression.
            throw new BadSyntaxException("Empty else clause", clause);
          }

          if (Pair.EMPTY_LIST == exprs.cdr) {
            evaluator.setTopLevel(false);
            return exprs.car;
          } else {
            evaluator.returnSequence();
            return exprs;
          }
        }

      } else if (evaluator.isSimpleExpression(test)) {
        Object value = evaluator.evalSimpleExpression(test);

        if (Cast.toBoolean(value)) {
          // True.

          Pair exprs = (Pair)clause.cdr;
          if (Pair.EMPTY_LIST == exprs) {
            // Return value of test.
            evaluator.returnValue();
            return value;

          } else if ((SYM_ARROW == exprs.car) && arrowUnbound) {
            // => expression.

            Object proc = exprs.cdr;
            if (Pair.EMPTY_LIST == proc) {
              throw new BadSyntaxException("=> not followed by expression",
                                           clause);

            } else if (Pair.EMPTY_LIST != ((Pair)proc).cdr) {
              throw new BadSyntaxException(
                "=> followed by more than one expression", clause);

            } else {
              proc = ((Pair)proc).car;

              // Create arrow continuation and fire it off.
              Continuation cont = new Continuation("=>", CONT_ARROW, 1, 1);
              cont.one          = value;

              evaluator.setTopLevel(false);
              return Pair.cons(cont, Pair.cons(proc, Pair.EMPTY_LIST));
            }

          } else if (Pair.EMPTY_LIST == exprs.cdr) {
            // Just one expression.
            evaluator.setTopLevel(false);
            return exprs.car;

          } else {
            // A sequence of expressions.
            evaluator.returnSequence();
            return exprs;
          }
        }
        // Next clause.

      } else {
        // Set up continuation and fire it off.
        Continuation cont = new Continuation("cond", CONT_COND, 1, 1);
        cont.one          = p;

        evaluator.setTopLevel(false);
        return Pair.cons(cont, Pair.cons(test, Pair.EMPTY_LIST));
      }

      p = (Pair)p.cdr;
    } while (Pair.EMPTY_LIST != p);
    
    evaluator.returnValue();
    return Boolean.FALSE;
  }

  /**
   * Evaluate <code>case</code> syntax form.
   *
   * @param   p          The <code>case</code> form.
   * @param   evaluator  The calling evaluator.
   * @return             The result of evaluating the
   *                     <code>case</code> form.
   * @throws  EvaluatorException
   *                     Signals an exceptional condition when
   *                     evaluating the <code>case</code> form.
   */
  private Object doCase(Pair p, Evaluator evaluator)
    throws EvaluatorException {

    boolean elseUnbound = (!
                           evaluator.getCurrentEnvironment().isBound(SYM_ELSE));
    Object  k           = p.car;        // The key.
    p                   = (Pair)p.cdr;  // The list of clauses.

    if (evaluator.isSimpleExpression(k)) {
      // Evaluate simple expression and match it.

      Object o = evaluator.evalSimpleExpression(k);

      do {
        Object c = p.car;
        Pair   clause;
        if (! (c instanceof Pair)) {
          throw new BadSyntaxException("Case clause not a list", c);
        } else if (! (clause = (Pair)c).isList()) {
          throw new BadSyntaxException("Case clause not a list", c);
        }

        Object d = clause.car;
        Pair   datum;
        if (Pair.EMPTY_LIST == d) {
          // Nothing to do.

        } else if ((SYM_ELSE == d) && elseUnbound) {
          // Handle else clause.
          if (Pair.EMPTY_LIST != p.cdr) {
            // Must be the last clause.
            throw new BadSyntaxException("Else clause not last clause", p);

          } else {
            Pair exprs = (Pair)clause.cdr;
            if (Pair.EMPTY_LIST == exprs) {
              // Must be followed by at least one expression.
              throw new BadSyntaxException("Empty else clause", clause);

            } else if (Pair.EMPTY_LIST == exprs.cdr) {
              evaluator.setTopLevel(false);
              return exprs.car;

            } else {
              evaluator.returnSequence();
              return exprs;
            }
          }

        } else if (! (d instanceof Pair)) {
          throw new BadSyntaxException("Datums not a list", d);

        } else if (! (datum = (Pair)d).isList()) {
          throw new BadSyntaxException("Datums not a list", d);

        } else {
          // Does one datum match?
          Object r;
          try {
            r = datum.member(o, Equivalence.EQV);
          } catch (BadPairStructureException x) {
            throw new Bug("Unexpected exception: " + x.toString());
          }

          if (Pair.EMPTY_LIST != r) {
            // Fire off expressions.
            Pair exprs = (Pair)clause.cdr;

            if (Pair.EMPTY_LIST == exprs) {
              // Must be followed by at least one expression.
              throw new BadSyntaxException("No expression in case clause",
                                           clause);

            } else if (Pair.EMPTY_LIST == exprs.cdr) {
              evaluator.setTopLevel(false);
              return exprs.car;
            
            } else {
              evaluator.returnSequence();
              return exprs;
            }
          }
        }

        // Next clause.
        p = (Pair)p.cdr;
      } while (Pair.EMPTY_LIST != p);

      evaluator.returnValue();
      return Boolean.FALSE;

    } else {
      // Set up continuation and fire it off.
      Continuation cont = new Continuation("case", CONT_CASE, 1, 1);
      cont.one          = p;

      evaluator.setTopLevel(false);
      return Pair.cons(cont, Pair.cons(k, Pair.EMPTY_LIST));
    }
  }

  /**
   * Evaluate <code>and</code> syntax form.
   *
   * @param   p          The <code>and</code> form.
   * @param   length     The length of the <code>and</code> form.
   * @param   evaluator  The calling evaluator.
   * @return             The result of evaluating the <code>and</code>
   *                     form.
   * @throws  EvaluatorException
   *                     Signals an exceptional condition when
   *                     evaluating the <code>and</code> form.
   */
  private Object and(Pair p, int length, Evaluator evaluator)
    throws EvaluatorException {

    if (0 == length) {
      // Trivial case.
      evaluator.returnValue();
      return Boolean.TRUE;

    } else {
      // Walk over the expressions and evaluate as many simple
      // expressions as possible.
      do {
        Object o = p.car;

        if (evaluator.isSimpleExpression(o)) {
          // Evaluate simple expression right here.
          Object r = evaluator.evalSimpleExpression(o);

          o = p.cdr; // The next expression.

          if (! Cast.toBoolean(r)) {
            // Return first #f value.
            evaluator.returnValue();
            return r;

          } else if (Pair.EMPTY_LIST != o) {
            // If #t value and there is more, continue.
            p = (Pair)o;
            continue;

          } else {
            // Last #t value.
            evaluator.returnValue();
            return r;
          }

        } else {
          // Set up a continuation.
          Continuation cont = new Continuation("and", CONT_AND, 1, 1);
          cont.one          = p.cdr;

          evaluator.setTopLevel(false);
          return Pair.cons(cont, Pair.cons(o, Pair.EMPTY_LIST));
        }

      } while (true);
    }
  }

  /**
   * Evaluate <code>or</code> syntax form.
   *
   * @param   p          The <code>or</code> form.
   * @param   length     The length of the <code>or</code> form.
   * @param   evaluator  The calling evaluator.
   * @return             The result of evaluating the <code>or</code>
   *                     form.
   * @throws  EvaluatorException
   *                     Signals an exceptional condition when
   *                     evaluating the <code>or</code> form.
   */
  private Object or(Pair p, int length, Evaluator evaluator)
    throws EvaluatorException {

    if (0 == length) {
      // Trivial case.
      evaluator.returnValue();
      return Boolean.FALSE;

    } else {
      // Walk over the expressions and evaluate as many simple
      // expressions as necessary.
      do {
        Object o = p.car;

        if (evaluator.isSimpleExpression(o)) {
          // Evaluate simple expression right here.
          Object r = evaluator.evalSimpleExpression(o);

          o = p.cdr; // The next expression.

          if (Cast.toBoolean(r)) {
            // Return first #t value.
            evaluator.returnValue();
            return r;

          } else if (Pair.EMPTY_LIST != o) {
            // If #f value and there is more, continue.
            p = (Pair)o;
            continue;

          } else {
            // Last #f value.
            evaluator.returnValue();
            return r;
          }

        } else {
          // Set up a continuation.
          Continuation cont = new Continuation("or", CONT_OR, 1, 1);
          cont.one          = p.cdr;

          evaluator.setTopLevel(false);
          return Pair.cons(cont, Pair.cons(o, Pair.EMPTY_LIST));
        }

      } while (true);
    }
  }

  /**
   * Evaluate <code>let</code> syntax form.
   *
   * @param   p          The <code>let</code> form.
   * @param   length     The length of the <code>let</code> form.
   * @param   evaluator  The calling evaluator.
   * @return             The result of evaluating the
   *                     <code>let</code> form.
   * @throws  BadSyntaxException
   *                     Signals an invalid <code>let</code> form.
   */
  private Object let(Pair p, int length, Evaluator evaluator)
    throws BadSyntaxException {

    // Detect named let variant.
    boolean named = false;
    Object  o     = p.car; // Name or bindings?
    Symbol  s     = null;

    if (o instanceof Symbol) {
      // Name!
      named = true;
      s     = (Symbol)o;

      if (3 > length) {
        throw new BadSyntaxException(
          "Too few forms (" + length + ") for named let", p);
      }

      p = (Pair)p.cdr; // Now pointing to list starting with bindings.
      o = p.car;       // Bindings for named let.
    }

    // Handle bindings.
    Pair     q           = processBindings(o);
    Symbol[] formals     = (Symbol[])q.car;
    Pair     expressions = (Pair)q.cdr;

    // Get body.
    Pair body = (Pair)p.cdr;

    // Handle trivial case.
    if ((null == formals) && (! named)) {
      body = processBody(body, evaluator.getCurrentEnvironment());

      if (Pair.EMPTY_LIST == body.cdr) {
        evaluator.setTopLevel(false);
        return body.car;
      } else {
        evaluator.returnSequence();
        return body;
      }
    }

    // Handle environment.
    Environment env = evaluator.getCurrentEnvironment();

    if (named) {
      Environment env2 = new SimpleEnvironment(env, 1);
      env2.bind(s);
      env = env2;
    }

    // Create procedure.
    Closure closure = new Closure(formals, body, env, false, false);
    
    if (named) {
      try {
        env.modify(s, closure);
      } catch (BindingException x) {
        // Must never happen.
        throw new Bug("Unexpected exception: " + x.toString());
      }
    }
    
    // Done.
    evaluator.setTopLevel(false);
    return Pair.cons(closure, expressions);
  }

  /**
   * Evaluate <code>let*</code> syntax form.
   *
   * @param   p          The <code>let*</code> form.
   * @param   evaluator  The calling evaluator.
   * @return             The result of evaluating the
   *                     <code>let*</code> form.
   * @throws  BadSyntaxException
   *                     Signals an invalid <code>let*</code> form.
   */
  private Object letstar(Pair p, Evaluator evaluator)
    throws BadSyntaxException {

    // Handle bindings.
    Object   o           = p.car;
    Pair     q           = processBindings(o);
    Symbol[] formals     = (Symbol[])q.car;
    Pair     expressions = (Pair)q.cdr;

    // Get body.
    Pair body = (Pair)p.cdr;
    
    // Handle trivial case.
    if (null == formals) {
      // Safe to process body right away,
      // since no bindings are introduced by this letrect form.
      body = processBody(body, evaluator.getCurrentEnvironment());

      if (Pair.EMPTY_LIST == body.cdr) {
        evaluator.setTopLevel(false);
        return body.car;
      } else {
        evaluator.returnSequence();
        return body;
      }

    } else {
      // Set up continuation to sequentially evaluate the expressions
      // and bind them.
      Continuation cont = new Continuation("let*", CONT_LETSTAR, 1, 1);
      cont.one          = formals;
      cont.two          = expressions.cdr;
      cont.three        = body;
      cont.flag         = 0;

      evaluator.setTopLevel(false);
      return Pair.cons(cont, Pair.cons(expressions.car, Pair.EMPTY_LIST));
    }
  }

  /**
   * Evaluate <code>letrec</code> syntax form.
   *
   * @param   p          The <code>letrec</code> form.
   * @param   evaluator  The calling evaluator.
   * @return             The result of evaluating the
   *                     <code>letrec</code> form.
   * @throws  BadSyntaxException
   *                     Signals an invalid <code>letrec</code> form.
   */
  private Object letrec(Pair p, Evaluator evaluator)
    throws BadSyntaxException {

    // Handle bindings.
    Object   o           = p.car;
    Pair     q           = processBindings(o);
    Symbol[] formals     = (Symbol[])q.car;
    Pair     expressions = (Pair)q.cdr;

    // Get body.
    Pair body = (Pair)p.cdr;

    // Handle trivial case.
    if (null == formals) {
      // Safe to process body right away,
      // since no bindings are introduced by this letrect form.
      body = processBody(body, evaluator.getCurrentEnvironment());

      if (Pair.EMPTY_LIST == body.cdr) {
        evaluator.setTopLevel(false);
        return body.car;
      } else {
        evaluator.returnSequence();
        return body;
      }

    } else {
      // Verify that formals are unique.
      int l = formals.length;
      for (int i=1; i<l; i++) {
        Symbol s = formals[i];
        for (int j=0; j<i; j++) {
          if (s == formals[j]) {
            throw new BadSyntaxException("Duplicate formal", s);
          }
        }
      }

      // Set up a new environment, seal it, and fire off the continuation
      // that takes the values for those bindings.

      Environment env1  = evaluator.getCurrentEnvironment();
      Environment env2  = new SimpleEnvironment(env1, formals.length);
      for (int i=0; i<l; i++) {
        env2.bind(formals[i]);
      }
      env2.seal();
      evaluator.setCurrentEnvironment(env2);

      Continuation cont = new Continuation("letrec", CONT_LETREC, l, l);
      cont.one          = formals;
      cont.two          = body;

      evaluator.setTopLevel(false);
      return Pair.cons(cont, expressions);
    }
  }

  /**
   * Process the specified binding syntax form. Deconstructs the
   * bindings specified for a <code>let</code>, <code>let*</code>, or
   * <code>letrec</code> form into an array of formals and a list of
   * expressions, and returns a pair whose car is the array of formals
   * and whose cdr is the list of expressions. If the specified
   * binding syntax is empty, it returns a pair consisting of a
   * <code>null</code> array and an empty list. This method does not
   * verify that a given symbol appears only once in the list of
   * bindings.
   *
   * @param   o  The bindings to deconstruct.
   * @return     A pair whose car is an array of formals and whose
   *             cdr is a list of expressions, corresponding to the
   *             specified binding syntax.
   * @throws  BadSyntaxException
   *             Signals an invalid bindings form.
   */
  private static Pair processBindings(Object o) throws BadSyntaxException {

    // Get the trivial cases out of the way.
    if (null == o) {
      return Pair.cons(null, Pair.EMPTY_LIST);

    } else if (! (o instanceof Pair)) {
      throw new BadSyntaxException("Bindings not a list", o);
    }

    // Make sure bindings form a list.
    Pair b = (Pair)o;
    int  l;
    try {
      l = b.length();
    } catch (BadPairStructureException x) {
      throw new BadSyntaxException("Bindings not a list", b);
    }

    // Process the individual bindings.
    Symbol[] formals = new Symbol[l];
    Pair     head    = Pair.EMPTY_LIST; // The head of the expression list.
    Pair     tail    = Pair.EMPTY_LIST; // The tail of the expression list.
    int      i       = 0;               // Current binding count.

    do {
      o = b.car;        // The current binding.
      b = (Pair)b.cdr;  // The rest of the bindings.
    
      if (! (o instanceof Pair)) {
        throw new BadSyntaxException("Binding not a list", o);
      } else {
        Pair p1 = (Pair)o;
        int  l1;
        try {
          l1 = p1.length();
        } catch (BadPairStructureException x) {
          throw new BadSyntaxException("Binding not a list", p1);
        }
        if (2 > l1) {
          throw new BadSyntaxException("Too few forms (" + l1 +
                                       ") for binding", p1);
        } else if (2 < l1) {
          throw new BadSyntaxException("Too many forms (" + l1 +
                                       ") for binding", p1);
        }

        // Collect symbol.
        o = p1.car;
        if (o instanceof Symbol) {
          formals[i] = (Symbol)o;
          i++;
        } else {
          throw new BadSyntaxException("Formal not a symbol for binding", o);
        }

        // Collect expression.
        Pair p2 = Pair.cons(((Pair)p1.cdr).car, Pair.EMPTY_LIST);
        if (Pair.EMPTY_LIST == head) {
          head = p2;
          tail = p2;
        } else {
          tail.cdr = p2;
          tail     = p2;
        }
      }

    } while (Pair.EMPTY_LIST != b);

    return Pair.cons(formals, head);
  }

  /**
   * Process the specified body. Scans the specified body for internal
   * definitions, transforms these definitions into an equivalent
   * <code>letrec</code> form, and returns the transformed body. If
   * the specified body contains no internal definitions, the body is
   * returned as is.
   *
   * <p>Definitions are only transformed if <code>define</code> has
   * its default top-level meaning in the specified
   * environment. Definitions in <code>begin</code> clauses are only
   * transformed if <code>begin</code> also has its default top-level
   * meaning in the specified environment.</p>
   *
   * @param   body    The body to process, which must be a non-empty
   *                  proper list.
   * @param   env     The current environment.
   * @return          The processed body, which is guaranteed to be
   *                  a non-empty proper list.
   * @throws  BadSyntaxException
   *                  Signals a malformed internal definition, or an
   *                  invalid occurrence of <code>define</code> or
   *                  <code>begin</code>, or that the body minus the
   *                  internal definitions is empty.
   * @throws  NullPointerException
   *                  Signals that <code>body</code> or
   *                  <code>env</code> is <code>null</code>.
   */
  public static Pair processBody(Pair body, Environment env)
    throws BadSyntaxException {

    if (null == body) {
      throw new NullPointerException("Null body");
    } else if (null == env) {
      throw new NullPointerException("Null environment");
    }

    // Check that define has its default top-level meaning.
    Object o;

    try {
      o = env.lookup(SYM_DEFINE);
    } catch (BindingException x) {
      // Define miraculously got undefined.
      return body;
    }

    if ((! (o instanceof Syntax)) ||
        (DEFINE != ((Syntax)o).opcode)) {
      return body;
    }

    // Does begin have its default top-level meaning?
    boolean topLevelBegin = true;
    try {
      o = env.lookup(SYM_BEGIN);
    } catch (BindingException x) {
      // Begin miraculously got undefined.
      topLevelBegin = false;
    }

    if ((! (o instanceof Syntax)) ||
        (BEGIN != ((Syntax)o).opcode)) {
      topLevelBegin = false;
    }

    // Scan body for defines.
    Pair b = body; // Current pointer into body.

    o = b.car;
    if (isDefine(o)) {
      // Collect definitions.
      Pair p    = Pair.cons(getBinding((Pair)o, false), Pair.EMPTY_LIST);
      Pair head = p;
      Pair tail = p;

      b = (Pair)b.cdr;

      while ((Pair.EMPTY_LIST != b) &&
             isDefine((o = b.car))) {

        p = Pair.cons(getBinding((Pair)o, false), Pair.EMPTY_LIST);
        tail.cdr = p;
        tail     = p;

        b = (Pair)b.cdr;
      }

      if (Pair.EMPTY_LIST == b) {
        // Body must have something else.
        throw new BadSyntaxException("Body empty besides internal definitions",
                                     body);

      } else if (isBegin((o = b.car))
                 && topLevelBegin) {
        // begin after internal definitions must not start with a define.
        o = ((Pair)o).cdr;
        if ((o instanceof Pair) && isDefine(((Pair)o).car)) {
          throw new BadSyntaxException(
            "Illegal internal definition inside begin", body);
        }
      }

      // Return equivalent letrec form.
      return Pair.cons(Pair.cons(LETREC_ME, Pair.cons(head, b)),
                       Pair.EMPTY_LIST);

    } else if (isBegin(o) && topLevelBegin) {
      Pair q = (Pair)o; // The begin form.

      int l;
      try {
        l = q.length();
      } catch (BadPairStructureException x) {
        throw new BadSyntaxException("Begin form not a list", q);
      }

      if (2 > l) {
        throw new BadSyntaxException("Too few forms (" + (l-1) +
                                     ") for begin", q);
      }

      Pair r = (Pair)q.cdr; // The pointer into the sequence of exprs.
      o      = r.car;       // The first expression of the begin form.

      if (isDefine(o)) {
        // Collect definitions in begin form.
        Pair p    = Pair.cons(getBinding((Pair)o, true), Pair.EMPTY_LIST);
        Pair head = p;
        Pair tail = p;

        r = (Pair)r.cdr;

        while (Pair.EMPTY_LIST != r) {
          o = r.car;

          if (isDefine(o)) {
            p = Pair.cons(getBinding((Pair)o, true), Pair.EMPTY_LIST);
            tail.cdr = p;
            tail     = p;
          } else {
            throw new BadSyntaxException(
              "Not a definition in begin form of internal definitions", q);
          }

          r = (Pair)r.cdr;
        }

        r = (Pair)b.cdr; // Rest of body.
        if (Pair.EMPTY_LIST == r) {
          // Body must have something else.
          throw new BadSyntaxException(
            "Body empty besides internal definitions", body);

        } else if (isDefine((o = r.car))) {
          // Illegal define after internal definitions.
          throw new BadSyntaxException(
            "Illegal internal definition inside body", body);

        } else if (isBegin(o)) {
          // begin after internal definitions must not start with a define.
          o = ((Pair)o).cdr;
          if ((o instanceof Pair) && isDefine(((Pair)o).car)) {
            throw new BadSyntaxException(
              "Illegal internal definition inside begin", body);
          }
        }

        // Return equivalent letrec form.
        return Pair.cons(Pair.cons(LETREC_ME, Pair.cons(head, r)),
                         Pair.EMPTY_LIST);
      }
    }

    // No internal definitions.
    return body;
  }

  /**
   * Determine if the specified object is a <code>define</code> form.
   *
   * @param   o  The object to test.
   * @return     <code>true</code> iff the specified object is
   *             a pair whose car is the symbol <code>define</code>.
   */
  private static boolean isDefine(Object o) {
    if (o instanceof Pair) {
      return (SYM_DEFINE == ((Pair)o).car);
    } else {
      return false;
    }
  }

  /**
   * Determine if the specified object is a <code>begin</code> form.
   *
   * @param   o  The object to test.
   * @return     <code>true</code> iff the specified object is a
   *             pair whose car is the symbol <code>begin</code>.
   */
  private static boolean isBegin(Object o) {
    if (o instanceof Pair) {
      return (SYM_BEGIN == ((Pair)o).car);
    } else {
      return false;
    }
  }

  /**
   * Return the binding corresponding to the specified
   * <code>define</code> form. The specified pair <code>p</code> must
   * be a <code>define</code> form.
   *
   * @param   l  The <code>define</code> form.
   * @param   inBegin
   *             <code>true</code> if the specified <code>define</code>
   *             form appears within a begin.
   * @return     The corresponding binding.
   * @throws  BadSyntaxException
   *             Signals a malformed <code>define</code> form.
   */
  private static Pair getBinding(Pair l, boolean inBegin)
    throws BadSyntaxException {

    // Make sure define form is a list that is long enough.
    int length;
    try {
      length = l.length();
    } catch (BadPairStructureException x) {
      throw new BadSyntaxException("Internal definition not a list", l);
    }

    Pair p = (Pair)l.cdr; // define form without the leading define.

    if (3 > length) {
      throw new BadSyntaxException("Too few forms (" + (length-1) +
                                   ") for internal definition", l);
    }

    Object o = p.car; // Symbol or list.

    if (o instanceof Pair) {                         // Sugared lambda define.
      Pair q = (Pair)o;
      o      = q.car;   // Variable for define.

      if (! (o instanceof Symbol)) {
        throw new BadSyntaxException(
          "Variable not a symbol for internal definition", o);

      } else if (SYM_DEFINE == o) {
        // define can't be redefined.
        throw new BadSyntaxException("Illegal to redefine define", l);
      
      } else if (inBegin && (SYM_BEGIN == o)) {
        // begin cant't be redefined inside a begin.
        throw new BadSyntaxException("Illegal to redefine begin within begin",
                                     l);

      } else {
        // Return desugared lambda binding.
        return Pair.cons(o,
                         Pair.cons(Pair.cons(SYM_LAMBDA,
                                             Pair.cons(q.cdr, p.cdr)),
                                   Pair.EMPTY_LIST));
      }

    } else if (! (o instanceof Symbol)) {            // Regular define.
      throw new BadSyntaxException(
        "Variable not a symbol for internal definition", o);

    } else if (3 < length) {
      // Verify the form is not too long.
      throw new BadSyntaxException("Too many forms (" + (length-1) +
                                   ") for internal definition", l);
    } else if (SYM_DEFINE == o) {
      // define can't be redefined.
      throw new BadSyntaxException("Illegal to redefine define", l);
      
    } else if (inBegin && (SYM_BEGIN == o)) {
      // begin can't be redefined inside a begin.
      throw new BadSyntaxException("Illegal to redefine begin within begin",
                                   l);

    } else {
      // Return binding.
      return p;
    }
  }

  /**
   * Evaluate <code>begin</code> syntax form. Internal definitions are
   * not recognized as valid <code>begin</code> forms and must be
   * pre-processed before occuring in a body.
   *
   * @param   p          The <code>begin</code> form.
   * @param   length     The length of the <code>begin</code> form.
   * @param   evaluator  The calling evaluator.
   * @return             The result of evaluating the
   *                     <code>begin</code> form, which is either
   *                     an expression or a sequence of expressions.
   * @throws  BadSyntaxException
   *                     Signals an invalid <code>begin</code> form.
   */
  private Object begin(Pair p, int length, Evaluator evaluator)
    throws BadSyntaxException {

    // Propagate top-level context to correctly handle begin forms
    // at the top-level.

    if (1 == length) {
      /*
       * Top-level is propagated automatically. The core evaluator
       * already expects an expression, and does not touch the
       * top-level flag.
       */
      return p.car;
    } else {
      boolean topLevel = evaluator.isTopLevel();
      evaluator.returnSequence();
      evaluator.setTopLevel(topLevel);
      return p;
    }
  }

  /**
   * Evaluate <code>do</code> syntax form.
   *
   * @param   p          The <code>do</code> form.
   * @param   evaluator  The calling evaluator.
   * @return             The result of evaluating the <code>do</code>
   *                     form, which is a primitive continuation.
   * @throws  BadSyntaxException
   *                     Signals an invalid <code>do</code> form.
   */
  private Object doDo(Pair p, Evaluator evaluator)
    throws BadSyntaxException {

    Symbol[]    vars;
    Environment env = evaluator.getCurrentEnvironment();
    Pair        inits;
    Pair        steps;
    Pair        test;
    Pair        cmds;

    // Process variables.
    Pair q   = processVariables(p.car);
    vars     = (Symbol[])q.car;
    q        = (Pair)q.cdr;
    inits    = (Pair)q.car;
    steps    = (Pair)q.cdr;

    // Process test.
    p        = (Pair)p.cdr;
    Object o = p.car;

    if (Pair.EMPTY_LIST == o) {
      throw new BadSyntaxException("Empty test expression for do", o);
    } else if (! (o instanceof Pair)) {
      throw new BadSyntaxException("Test expression not a list for do", o);
    } else if (! (q = (Pair)o).isList()) {
      throw new BadSyntaxException("Test expression not a list for do", o);
    }
    test     = q;

    // Process commands.
    cmds     = (Pair)p.cdr;

    // Set up continuation and fire it off.
    Continuation cont = new Continuation("do", CONT_DO,
                                         vars.length, vars.length);
    cont.one          = Pair.cons(vars, env);
    cont.two          = steps;
    cont.three        = Pair.cons(test, cmds);
    cont.flag         = 1;

    evaluator.setTopLevel(false);
    return Pair.cons(cont, inits);
  }

  /**
   * Process the specified variable syntax form. Deconstructs the
   * variable definitions, including initialization expression and
   * step expression, for a <code>do</code> form into an array of
   * variables, a a list of initialization expressions, and a list of
   * step expressions, and returns a pair whose car is the array of
   * variables and whose cdr is another pair whose car is the list of
   * initialization expressions and whose cdr is the list of step
   * expressions. If the specified variable syntax is empty, it
   * returns an empty array for the array of variables and
   * <code>Pair.EMPTY_LIST</code> for the lists of initialization and
   * step expressions. This method verifies that a given symbol
   * appears only once as a variable.
   *
   * @param   o  The variable syntax to deconstruct.
   * @return     A pair whose car is an array of formals and whose
   *             cdr is another pair whose car is a list of
   *             initialization expressions and whose cdr is a list
   *             of step expressions.
   * @throws  BadSyntaxException
   *             Signals an invalid variable syntax.
   */
  private static Pair processVariables(Object o) throws BadSyntaxException {

    // Get the trivial cases out of the way.
    if (null == o) {
      return Pair.cons(new Symbol[0],
                       Pair.cons(Pair.EMPTY_LIST, Pair.EMPTY_LIST));

    } else if (! (o instanceof Pair)) {
      throw new BadSyntaxException("Variable definitions not a list",
                                   o);
    }

    // Make sure variable definitions form a list.
    Pair b = (Pair)o;
    int  l;
    try {
      l = b.length();
    } catch (BadPairStructureException x) {
      throw new BadSyntaxException("Variable definitions not a list", b);
    }

    // Process the individual variable definitions.
    Symbol[] formals  = new Symbol[l];   // The array of variable names.
    Pair     headInit = Pair.EMPTY_LIST; // The head of the initialization list.
    Pair     tailInit = Pair.EMPTY_LIST; // The tail of the initialization list.
    Pair     headStep = Pair.EMPTY_LIST; // The head of the step list.
    Pair     tailStep = Pair.EMPTY_LIST; // The tail of the step list.
    int      i        = 0;

    do {
      o = b.car;        // The current variable definition.
      b = (Pair)b.cdr;  // The rest of the variable definitions.
    
      if (! (o instanceof Pair)) {
        throw new BadSyntaxException("Variable definition not a list", o);
      } else {
        Pair p1 = (Pair)o;
        int  l1;
        try {
          l1 = p1.length();
        } catch (BadPairStructureException x) {
          throw new BadSyntaxException("Variable definition not a list", p1);
        }
        if (2 > l1) {
          throw new BadSyntaxException("Too few forms (" + l1 +
                                       ") for variable definition", p1);
        } else if (3 < l1) {
          throw new BadSyntaxException("Too many forms (" + l1 +
                                       ") for variable definition", p1);
        }

        // Collect symbol.
        o = p1.car;
        if (o instanceof Symbol) {
          for (int j=0; j<i; j++) {
            if (o == formals[j]) {
              throw new BadSyntaxException("Duplicate variable", o);
            }
          }
          formals[i] = (Symbol)o;
          i++;
        } else {
          throw new BadSyntaxException("Variable not a symbol", o);
        }

        // Collect expressions.
        p1 = (Pair)p1.cdr;

        Pair p2 = Pair.cons(p1.car, Pair.EMPTY_LIST);
        if (Pair.EMPTY_LIST == headInit) {
          headInit = p2;
          tailInit = p2;
        } else {
          tailInit.cdr = p2;
          tailInit     = p2;
        }

        if (2 == l1) {
          p2 = Pair.cons(o, Pair.EMPTY_LIST);
        } else {
          p2 = Pair.cons(((Pair)p1.cdr).car, Pair.EMPTY_LIST);
        }
        if (Pair.EMPTY_LIST == headStep) {
          headStep = p2;
          tailStep = p2;
        } else {
          tailStep.cdr = p2;
          tailStep     = p2;
        }
      }

    } while (Pair.EMPTY_LIST != b);

    return Pair.cons(formals, Pair.cons(headInit, headStep));
  }

  /**
   * Evaluate <code>quasiquote</code> syntax form.
   *
   * @param   p          The <code>quasiquote</code> form.
   * @param   evaluator  The calling evaluator.
   * @return             The result of evaluating the
   *                     <code>quasiquote</code> form.
   * @throws  BadSyntaxException
   *                     Signals an invalid <code>quasiquote</code>
   *                     form.
   */
  private Object quasiquote(Pair l, Evaluator evaluator)
    throws BadSyntaxException {

    // The quasiquote expression itself is a literal, but not the result.
    l.turnIntoLiteral();

    Object o = l.car;

    // Get trivial cases out of the way.
    if ((! (o instanceof Pair)) && (! (o instanceof List))) {
      evaluator.returnValue();
      return o;
    }

    // Determine whether unquote and unquote-splicing are bound.
    Environment env            = evaluator.getCurrentEnvironment();
    boolean     commaUnbound   = false;
    boolean     commaAtUnbound = false;

    try {
      env.lookup(SYM_COMMA); 
    } catch (BindingException x) {
      commaUnbound = true;
    }

    try {
      env.lookup(SYM_COMMA_AT);
    } catch (BindingException x) {
      commaAtUnbound = true;
    }
    
    if ((! commaUnbound) && (! commaAtUnbound)) {
      evaluator.returnValue();
      return o;
    }

    // Collect expressions.
    ArrayList exprs = new ArrayList();

    walkTick1(o, commaUnbound, commaAtUnbound, 0, exprs);

    int size = exprs.size();

    if (0 == size) {
      evaluator.returnValue();
      return o;

    } else {
      // Set up continuation and fire it off.
      Continuation cont = new Continuation("quasiquote", CONT_QUASIQUOTE,
                                           size, size);
      cont.one          = o;
      cont.flag         = ((commaUnbound? 0x01 : 0x00) |
                           (commaAtUnbound? 0x02 : 0x00));

      evaluator.setTopLevel(false);
      return Pair.cons(cont, Pair.createList(exprs));
    }
  }

  /**
   * Structurally walk the specified object as a <code>quasiquote</code>
   * template and collect expressions that need to be evaluated into
   * the specified list.
   *
   * @param   o  The object to walk.
   * @param   commaUnbound
   *             <code>true</code> iff <code>unquote</code> is unbound.
   * @param   commaAtUnbound
   *             <code>true</code> iff <code>unquote-splicing</code> is
   *             unbound.
   * @param   nesting
   *             The current nesting level for <code>quasiquote</code>.
   *             Must be 0 initially.
   * @param   exprs
   *             The array list to collect expressions that need to be
   *             evaluated into.
   * @throws  BadSyntaxException
   *             Signals an illegal context for a
   *             <code>unquote-splicing</code> form.
   */
  private static void walkTick1(Object o,
                                boolean commaUnbound, boolean commaAtUnbound,
                                int nesting, ArrayList exprs)
    throws BadSyntaxException {

    if (o instanceof Pair) {
      Pair p = (Pair)o;

      // Check pair.
      if (isBackTick(p)) {
        walkTick1(getTickExpression(p), commaUnbound, commaAtUnbound,
                  (nesting + 1), exprs);
        return;

      } else if (isComma(p) && commaUnbound) {
        o = getTickExpression(p);

        if (0 == nesting) {
          exprs.add(o);
        } else {
          walkTick1(o, commaUnbound, commaAtUnbound, (nesting - 1), exprs);
        }
        return;

      } else if (isCommaAt(p) && commaAtUnbound) {
        throw new BadSyntaxException("Illegal context for unquote-splicing",
                                     getTickExpression(p));

      }
      
      // Check pair elements.
      do {
        // Look at car.
        o = p.car;

        if (isBackTick(o)) {
          walkTick1(getTickExpression(o), commaUnbound, commaAtUnbound,
                    (nesting + 1), exprs);

        } else if (isComma(o) && commaUnbound) {
          o = getTickExpression(o);

          if (0 == nesting) {
            exprs.add(o);
          } else {
            walkTick1(o, commaUnbound, commaAtUnbound, (nesting - 1), exprs);
          }

        } else if (isCommaAt(o) && commaAtUnbound) {
          o = getTickExpression(o);

          if (0 == nesting) {
            exprs.add(o);
          } else {
            walkTick1(o, commaUnbound, commaAtUnbound, (nesting - 1), exprs);
          }

        } else {
          walkTick1(o, commaUnbound, commaAtUnbound, nesting, exprs);
        }

        // Look at cdr.
        o = p.cdr;

        if (Pair.EMPTY_LIST == o) {
          // Done walking this list.
          return;

        } else if (! (o instanceof Pair)) {
          // Object could still be a vector.
          walkTick1(o, commaUnbound, commaAtUnbound, nesting, exprs);
          return;

        }
         
        // Continue walking this list, but check this pair first.
        if (isBackTick(o)) {
          walkTick1(getTickExpression(o), commaUnbound, commaAtUnbound,
                    (nesting + 1), exprs);
          return;

        } else if (isComma(o) && commaUnbound) {
          o = getTickExpression(o);

          if (0 == nesting) {
            exprs.add(o);
          } else {
            walkTick1(o, commaUnbound, commaAtUnbound, (nesting - 1), exprs);
          }
          return;

        } else if (isCommaAt(o) && commaAtUnbound) {
          throw new BadSyntaxException("Illegal context for unquote-splicing",
                                       getTickExpression(o));
        }

        // Now, really continue walking this list.
        p = (Pair)o;

      } while (true);

    } else if (o instanceof List) {
      // Walk the vector elements.

      List v = (List)o;
      int  l = v.size();

      for (int i=0; i<l; i++) {
        o = v.get(i);

        if (isComma(o) && commaUnbound) {
          o = getTickExpression(o);

          if (0 == nesting) {
            exprs.add(o);
          } else {
            walkTick1(o, commaUnbound, commaAtUnbound, (nesting - 1), exprs);
          }

        } else if (isCommaAt(o) && commaAtUnbound) {
          o = getTickExpression(o);

          if (0 == nesting) {
            exprs.add(o);
          } else {
            walkTick1(o, commaUnbound, commaAtUnbound, (nesting - 1), exprs);
          }

        } else {
          walkTick1(o, commaUnbound, commaAtUnbound, nesting, exprs);
        }
      }

      return;

    } else {
      // Not a pair, nor a vector, nothing to walk.
      return;
    }
  }

  /**
   * Structurally walk the specified object as a <code>quasiquote</code>
   * template and create a copy of it, filling in the specified values
   * for expressions in the template.
   *
   * @param   o  The object to walk.
   * @param   commaUnbound
   *             <code>true</code> iff <code>unquote</code> is unbound.
   * @param   commaAtUnbound
   *             <code>true</code> iff <code>unquote-splicing</code> is
   *             unbound.
   * @param   nesting
   *             The current nesting level for <code>quasiquote</code>.
   *             Must be 0 initially.
   * @param   values
   *             The array list of values to fill in for expressions in
   *             the template. This list must be in the same order as
   *             the list created by {@link #walkTick1}.
   * @return     A copy of the specified template with the specified
   *             values filled in for expressions in the template.
   * @throws  BadTypeException 
   *             Signals that the value for an
   *             <code>unquote-splicing</code> form is not a pair.
   * @throws  BadArgumentException
   *             Signals that the value for an
   *             <code>unquote-splicing</code> form is not a proper
   *             list.
   */
  static Object walkTick2(Object o,
                          boolean commaUnbound, boolean commaAtUnbound,
                          int nesting, ArrayList values)
    throws BadTypeException, BadArgumentException {

    if (o instanceof Pair) {
      Pair p = (Pair)o;

      if (isBackTick(p)) {
        o = walkTick2(getTickExpression(p), commaUnbound, commaAtUnbound,
                      (nesting + 1), values);
        return Pair.cons(SYM_BACKTICK, Pair.cons(o, Pair.EMPTY_LIST));

      } else if (isComma(p) && commaUnbound) {
        if (0 == nesting) {
          return values.remove(0);
        } else {
          o = walkTick2(getTickExpression(p), commaUnbound, commaAtUnbound,
                        (nesting - 1), values);
          return Pair.cons(SYM_COMMA, Pair.cons(o, Pair.EMPTY_LIST));
        }

      } else if (isCommaAt(p) && commaAtUnbound) {
        throw new Bug("Illegal state while creating quasiquote result");
      }
      
      // Copy the list.
      Pair head = Pair.EMPTY_LIST;  // The head of the resulting list.
      Pair tail = Pair.EMPTY_LIST;  // The tail of the resulting list.

      do {
        // Look at car.
        o = p.car;

        if (isBackTick(o)) {
          o = walkTick2(getTickExpression(o), commaUnbound, commaAtUnbound,
                        (nesting + 1), values);

          Pair r = Pair.cons(Pair.cons(SYM_BACKTICK,
                                       Pair.cons(o, Pair.EMPTY_LIST)),
                             Pair.EMPTY_LIST);
          if (Pair.EMPTY_LIST == head) {
            head = r;
            tail = r;
          } else {
            tail.cdr = r;
            tail     = r;
          }

        } else if (isComma(o) && commaUnbound) {
          Pair r;

          if (0 == nesting) {
            r = Pair.cons(values.remove(0), Pair.EMPTY_LIST);
          } else {
            o = walkTick2(getTickExpression(o), commaUnbound, commaAtUnbound,
                          (nesting - 1), values);
            r = Pair.cons(Pair.cons(SYM_COMMA, Pair.cons(o, Pair.EMPTY_LIST)),
                          Pair.EMPTY_LIST);
          }

          if (Pair.EMPTY_LIST == head) {
            head = r;
            tail = r;
          } else {
            tail.cdr = r;
            tail     = r;
          }

        } else if (isCommaAt(o) && commaAtUnbound) {
          if (0 == nesting) {
            o = values.remove(0);
            
            if (Pair.EMPTY_LIST == o) {
              // Nothing to do.
            } else if (! (o instanceof Pair)) {
              // Not a list.
              throw new BadTypeException(
                "Value of unquote-splicing expression not a list", o);

            } else {
              // Splice in list.
              Pair r = (Pair)o;

              if (! r.isList()) {
                throw new BadArgumentException(
                  "Value of unquote-splicing expression not a list", o);

              } else {
                if (Pair.EMPTY_LIST == head) {
                  head = r;
                } else {
                  tail.cdr = r;
                }

                // Find tail of list.
                while (Pair.EMPTY_LIST != (o = r.cdr)) {
                  r = (Pair)o;
                }

                tail = r;
              }
            }

          } else {
            o = walkTick2(getTickExpression(o), commaUnbound, commaAtUnbound,
                          (nesting - 1), values);

            Pair r = Pair.cons(Pair.cons(SYM_COMMA_AT,
                                         Pair.cons(o, Pair.EMPTY_LIST)),
                               Pair.EMPTY_LIST);
            if (Pair.EMPTY_LIST == head) {
              head = r;
              tail = r;
            } else {
              tail.cdr = r;
              tail     = r;
            }
          }

        } else {
          o = walkTick2(o, commaUnbound, commaAtUnbound, nesting, values);

          Pair r = Pair.cons(o, Pair.EMPTY_LIST);
          if (Pair.EMPTY_LIST == head) {
            head = r;
            tail = r;
          } else {
            tail.cdr = r;
            tail     = r;
          }
        }

        // Look at cdr.
        o = p.cdr;

        if (Pair.EMPTY_LIST == o) {
          // Done walking this list.
          return head;

        } else if (! (o instanceof Pair)) {
          // Object could still be a vector.
          o = walkTick2(o, commaUnbound, commaAtUnbound, nesting, values);
          if (Pair.EMPTY_LIST == head) {
            return o;
          } else {
            tail.cdr = o;
            return head;
          }
        }
         
        // Continue walking this list, but check this pair first.
        if (isBackTick(o)) {
          o = walkTick2(getTickExpression(o), commaUnbound, commaAtUnbound,
                        (nesting + 1), values);
          Pair r = Pair.cons(SYM_BACKTICK, Pair.cons(o, Pair.EMPTY_LIST));
          if (Pair.EMPTY_LIST == head) {
            return r;
          } else {
            tail.cdr = r;
            return head;
          }

        } else if (isComma(o) && commaUnbound) {
          if (0 == nesting) {
            o = values.remove(0);
            if (Pair.EMPTY_LIST == head) {
              return o;
            } else {
              tail.cdr = o;
              return head;
            }

          } else {
            o = walkTick2(getTickExpression(o), commaUnbound, commaAtUnbound,
                          (nesting - 1), values);
            Pair r = Pair.cons(SYM_COMMA, Pair.cons(o, Pair.EMPTY_LIST));
            if (Pair.EMPTY_LIST == head) {
              return r;
            } else {
              tail.cdr = r;
              return head;
            }
          }

        } else if (isCommaAt(o) && commaAtUnbound) {
          throw new Bug("Illegal state while creating quasiquote result");
        }

        // Now, really continue walking this list.
        p = (Pair)o;

      } while (true);

    } else if (o instanceof List) {
      // Walk the vector elements.

      List      v      = (List)o;
      int       l      = v.size();
      ArrayList result = new ArrayList();

      for (int i=0; i<l; i++) {
        o = v.get(i);

        if (isComma(o) && commaUnbound) {
          if (0 == nesting) {
            result.add(values.remove(0));

          } else {
            o = walkTick2(getTickExpression(o), commaUnbound, commaAtUnbound,
                          (nesting - 1), values);
            result.add(Pair.cons(SYM_COMMA, Pair.cons(o, Pair.EMPTY_LIST)));
          }

        } else if (isCommaAt(o) && commaAtUnbound) {
          if (0 == nesting) {
            o = values.remove(0);

            // Make sure result is a proper list.
            if (Pair.EMPTY_LIST == o) {
              // Nothing to do.
              
            } else if (! (o instanceof Pair)) {
              throw new BadTypeException(
                "Value of unquote-splicing expression not a list", o);

            } else {
              Pair p = (Pair)o;

              if (! p.isList()) {
                throw new BadArgumentException(
                  "Value of unquote-splicing expression not a list", o);

              } else {
                do {
                  result.add(p.car);
                  p = (Pair)p.cdr;
                } while (Pair.EMPTY_LIST != p);
              }
            }

          } else {
            o = walkTick2(getTickExpression(o), commaUnbound, commaAtUnbound,
                          (nesting - 1), values);
            result.add(Pair.cons(SYM_COMMA_AT, Pair.cons(o, Pair.EMPTY_LIST)));
          }

        } else {
          result.add(walkTick2(o, commaUnbound, commaAtUnbound,
                               nesting, values));
        }
      }

      // Return new vector.
      return Vector.create(result);

    } else {
      // Not a pair, nor a vector, nothing to copy.
      return o;
    }
  }

  /**
   * Determine whether the specified object represents a valid
   * <code>quasiquote</code> form.
   *
   * @param   o  The object to test.
   * @return     <code>true</code> iff the specified object
   *             represents a valid <code>quasiquote</code> form.
   */
  private static boolean isBackTick(Object o) {
    if (! (o instanceof Pair))    return false;
    Pair p = (Pair)o;
    if (SYM_BACKTICK != p.car)    return false;
    o      = p.cdr;
    if (! (o instanceof Pair))    return false;
    p      = (Pair)o;
    if (Pair.EMPTY_LIST != p.cdr) return false;
    return true;
  }

  /**
   * Determine whether the specified object represents a valid
   * <code>unquote</code> form.
   *
   * @param   o  The object to test.
   * @return     <code>true</code> iff the specified object
   *             represents a valid <code>unquote</code> form.
   */
  private static boolean isComma(Object o) {
    if (! (o instanceof Pair))    return false;
    Pair p = (Pair)o;
    if (SYM_COMMA != p.car)       return false;
    o      = p.cdr;
    if (! (o instanceof Pair))    return false;
    p      = (Pair)o;
    if (Pair.EMPTY_LIST != p.cdr) return false;
    return true;
  }

  /**
   * Determine whether the specified object represents a valid
   * <code>unquote-splicing</code> form.
   * 
   * @param   o  The object to test.
   * @return     <code>true</code> iff the specified object
   *             represents a valid <code>unquote-splicing</code>
   *             form.
   */
  private static boolean isCommaAt(Object o) {
    if (! (o instanceof Pair))    return false;
    Pair p = (Pair)o;
    if (SYM_COMMA_AT != p.car)    return false;
    o      = p.cdr;
    if (! (o instanceof Pair))    return false;
    p      = (Pair)o;
    if (Pair.EMPTY_LIST != p.cdr) return false;
    return true;
  }

  /**
   * Extract the actual expression from the specified
   * <code>quasiquote</code>, <code>unquote</code>, or
   * <code>unquote-splicing</code> form. The specified object must be
   * in a valid format for one of these three forms.
   *
   * @param   o  The form whose expression to extract.
   * @return     The actual expression of the specified
   *             form.
   */
  private static Object getTickExpression(Object o) {
    return ((Pair)((Pair)o).cdr).car;
  }

  // =========================== Initialization =========================

  /**
   * Install the syntax forms in the specified environment.
   *
   * @param   env     The environment to install the syntax
   *                  forms into.
   */
  public static void install(Environment env) {
    add(env, "quote",                      QUOTE,                1,  1);
    add(env, "lambda",                     LAMBDA,               2, -1);
    add(env, "if",                         IF,                   2,  3);
    add(env, "set!",                       SET,                  2,  2);
    add(env, "cond",                       COND,                 1, -1);
    add(env, "case",                       CASE,                 2, -1);
    add(env, "and",                        AND,                  0, -1);
    add(env, "or",                         OR,                   0, -1);
    add(env, "let",                        LET,                  2, -1);
    add(env, "let*",                       LETSTAR,              2, -1);
    env.bind(Symbol.intern("letrec"), LETREC_ME);
    add(env, "begin",                      BEGIN,                1, -1);
    add(env, "do",                         DO,                   2, -1);
    add(env, "delay",                      DELAY,                1,  1);
    add(env, "quasiquote",                 QUASIQUOTE,           1,  1);
    add(env, "define",                     DEFINE,               2, -1);

    // add(env, "let-syntax",                 LET_SYNTAX,           2, -1);
    // add(env, "letrec-syntax",              LETREC_SYNTAX,        2, -1);
    // add(env, "syntax-rules",               SYNTAX_RULES,         1, -1);
    // add(env, "define-syntax",              DEFINE_SYNTAX,        2,  2);
  }

  /**
   * Create a new syntax form as specified and add a binding
   * for it to the specified environment.
   *
   * @param   env       The environment for the new syntax form.
   * @param   name      The name of the new syntax form.
   * @param   opcode    The opcode of the new syntax form.
   * @param   minArgs   The non-negative minimum number of arguments for
   *                    the new syntax form.
   * @param   maxArgs   The non-negative maximum number of arguments for
   *                    the new syntax form, or -1 if it takes an
   *                    unlimited maximum number of arguments.
   */
  private static void add(Environment env, String name, int opcode,
                          int minArgs, int maxArgs) {
    name     = name.intern();
    Symbol s = Symbol.intern(name);
    Syntax v = new Syntax(name, opcode, minArgs, maxArgs);

    env.bind(s, v);
  }

}
