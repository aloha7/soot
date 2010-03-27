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

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.io.IOException;

import java.util.ArrayList;

import one.util.Monitor;
import one.util.ReferenceList;
import one.util.Bug;

/**
 * Implementation of the core evaluator. The core evaluator implements
 * the basic evaluation logic for <i>eval</i> and maintains the state
 * associated with (recursively) evaluating expressions. It also
 * manages I/O streams and facilitates their clean-up.
 *
 *
 * <p><b>Using the Evaluator</b></p>
 *
 * <p>To evaluate expressions, an application, such as a
 * read-eval-print loop, typically creates an evaluator and evaluates
 * expressions using the {@link #eval(Object) <code>eval()</code>}
 * method. Between calls to <code>eval()</code>, the application may
 * clear the state of the last evaluation ({@link
 * #resetEvaluationState()}) and reset the current readers and writers
 * to their top-level defaults ({@link
 * #resetCurrentReaderWriter()}).</p>
 *
 * <p>If the application needs to evaluate several expressions
 * concurrently, it must use separate evaluators since (most of) the
 * methods defined in this class are not thread-safe. The application
 * should still only create one main evaluator object and {@link
 * #spawn() spawn} additional evaluators from this main evaluator as
 * required.</p>
 *
 * <p>To ensure that I/O streams (that is, readers, writers, input
 * streams, and output streams) used during evaluation are correctly
 * cleaned up, the application <em>must</em> call {@link
 * #cleanUpMarkedStreams()} before relinquishing the reference to its
 * main evaluator object (that is, before exiting), but not before all
 * evaluators have finished their evaluations.</p>
 *
 *
 * <p><b>Basic Evaluation</b></p>
 *
 * <p>Basically, the core evaluator applies {@link Applicable
 * applicable entities} to their arguments. Application is represented
 * by {@link Pair lists}, with the applicable entity in prefix
 * position. The arguments for syntactic applicable entities are
 * passed to an applicable entity's {@link
 * Applicable#apply(Pair,Evaluator) <code>apply()</code>} method in
 * unevaluated form. And, the arguments for non-syntactic applicable
 * entities are (recursively) evaluated from left to right before
 * being passed to an applicable entity's <code>apply()</code>
 * method.</p>
 *
 * <p>When evaluating an object, all Java objects that are not lists
 * are treated as self-evaluating objects, with the exception of
 * {@link Symbol symbols}, syntactic applicable entities, and objects
 * implementing the {@link NotAValue} interface. Symbols result in a
 * look-up of the corresponding binding in the current
 * environment. Syntactic applicable entities result in a {@link
 * NotAValueException}, unless they appear in the operator position of
 * an application. And, instances of <code>NotAValue</code> always
 * result in a not-a-value exception. Note that the result of a symbol
 * look-up must be a valid value (that is, it must not be a syntactic
 * applicable entity that is not in the operator position, nor an
 * instance of <code>NotAValue</code>).</p>
 *
 * <p>To support first-class continuations, the evaluator explicitly
 * maintains the execution state associated with (recursively)
 * evaluating expressions. Not very surprisingly, it internally uses a
 * form of activation frame to represent recursive evaluation. In
 * general, an activation frame is added to the stack of activation
 * frames when recursively evaluting an argument for an
 * application. Activation frames cannot be accessed directly, but an
 * applicable entity's <code>apply()</code> method can access all
 * relevant state through a set of up-calls.</p>
 *
 *
 * <p><b>Applicable Entities and Evaluation</b></p>
 *
 * <p>Applicable entities use a set of up-calls to interact with the
 * calling evaluator. These up-calls let an applicable entity access
 * and modify the evaluation state, control how the evaluator treats
 * the result of an applicable entity's <code>apply()</code> method,
 * and manage I/O streams.</p>
 *
 * <p>The evaluator provides access to the top-level flag of the
 * current activation frame ({@link #isTopLevel} and {@link
 * #setTopLevel(boolean) <code>setTopLevel()</code>}) as well as the
 * environment of the current activation frame ({@link
 * #getCurrentEnvironment()} and {@link
 * #setCurrentEnvironment(Environment)
 * <code>setCurrentEnvironment()</code>}). It also provides access to
 * the top-level environment ({@link #getTopLevelEnvironment()}).</p>
 * 
 * <p>An applicable entity's <code>apply()</code> method can modify
 * the execution environment for sub-expressions, by setting up a
 * procedure that accepts multiple values ({@link
 * #setUpValues(Applicable) <code>setUpValues()</code>}), and by
 * setting up dynamic extents (defined below) for {@link
 * #setUpMonitor(Object) monitors}, {@link
 * #setUpExceptionHandler(Applicable,Class) exception handlers}, as
 * well as {@link #setUpBeforeAfter(Object,Object) before and after
 * expressions}. Furthermore, it can {@link #getCurrentContinuation
 * capture} the current continuation, which is reified as an
 * applicable entity.</p>
 *
 * <p>By default, the evaluator treats the object returned from an
 * applicable entity's <code>apply()</code> method as a value for
 * non-syntactic applicable entities and as an expression to be
 * further evaluated for syntactic applicable entities. An applicable
 * entity's <code>apply()</code> method can override these defaults,
 * to return a value ({@link #returnValue()}), a sequence of values
 * ({@link #returnValues()}), an expression ({@link
 * #returnExpression()}), a sequence of expressions ({@link
 * #returnSequence()}), or an application ({@link
 * #returnApplication(Applicable,Pair,Applicable)
 * <code>returnApplication()</code>}).</p>
 *
 * <p>Note that returned expressions, sequences of expressions, and
 * applications are evaluated in the current activation frame, thus,
 * automatically ensuring that evaluation is tail-recursive. Further,
 * note that, by convention, an applicable entity's
 * <code>apply()</code> method should invoke any of the
 * <code>return???()</code> methods only after it has performed all
 * error checking and will not raise an exception anymore.</p>
 *
 * <p>The evaluator facilitates I/O management by providing stacks of
 * readers and writers and by keeping track of I/O streams for later
 * clean-up. It provides up-calls to access and modify the stacks of
 * readers and writers, and to mark and unmark I/O streams for
 * clean-up. An applicable entity should mark any I/O stream it
 * creates for clean-up, unless it always closes that I/O stream
 * itself. And, an applicable entity should unmark any I/O stream it
 * closes, unless it has opened that I/O stream itself and has not
 * marked it.</p>
 *
 * <p>An applicable entity, from within its <code>apply()</code>
 * method, <em>must not</em> recursively invoke the evaluator (which
 * results in an <code>IllegalStateException</code>). Rather, if some
 * operation requires that some expression is recursively evaluated,
 * the part of the operation that requires the result of the recursive
 * evaluation must be factored into a separate applicable entity. This
 * separate applicable entity is referred to as a primitive
 * continuation.</p>
 *
 * <p>For example, to implement the <code>if</code> syntax in Scheme,
 * the syntactic applicable entity for <code>if</code> might verify
 * that the supplied form is syntactically correct, create a primitive
 * continuation which takes one argument and internally stores both
 * the consequent and alternate, and return an expression to the
 * evaluator which applies this primitive continuation to the
 * test. The evaluator then evaluates the test expression and invokes
 * the primitive continuation's <code>apply()</code> method on the
 * result of evaluating the test. The primitive continuation then
 * returns either the consequent or alternate as an expression to the
 * evaluator, depending on the truth-value of the test.</p>
 *
 *
 * <p><b>Dynamic Extents</b></p>
 *
 * <p>Applicable entities set up monitors, exception handlers, as well
 * as before and after expressions in the core evaluator by using the
 * {@link #setUpMonitor(Object) <code>setUpMonitor()</code>}, {@link
 * #setUpExceptionHandler(Applicable,Class)
 * <code>setUpExceptionHandler()</code>}, and {@link
 * #setUpBeforeAfter(Object,Object) <code>setUpBeforeAfter()</code>}
 * up-calls, respectively. The definition of these methods relies on
 * the notion of a "dynamic extent", modeled after the concept of the
 * same name in R<sup><font size="-1">5</font></sup>RS for
 * <code>dynamic-wind</code>. It is defined as follows:<ul>
 *
 * <li>The dynamic extent is entered when the applicable entity's
 * <code>apply()</code> method that called such a method returns.</li>
 *
 * <li>The dynamic extent is also entered when control is not within
 * the dynamic extent and a continuation is invoked that was captured
 * (using {@link #getCurrentContinuation()}) during the dynamic
 * extent.</li>
 *
 * <li>It is exited when control returns from the activation frame
 * that was the current activation frame when the applicable entity's
 * <code>apply()</code> method that called such a method returns.</li>
 *
 * <li>It is also exited when control is within the dynamic extent and
 * a continuation is invoked that was captured while not within the
 * dynamic extent.</li>
 *
 * </ul>At most one dynamic extent can be associated with any given
 * activation frame. That is, an invocation of an applicable entity's
 * <code>apply()</code> method can, at most, call one of these three
 * methods.</p>
 *
 *
 * <p><b>Serialization, Thread-Safety</b></p>
 *
 * <p>An evaluator maintains references to non-serializable objects,
 * notably I/O streams. Consequently, instances of this class are not
 * serializable. Furthermore, as an evaluator, by its very definition,
 * tracks the state of a single thread of execution, most methods of
 * this class are not thread-safe. The exception are the methods that
 * allow a caller to determine whether the current evaluation has
 * terminated and to access the state of a terminated evaluation.</p>
 *
 * @author   &copy; Copyright 1998-2000 by Robert Grimm.
 *           Written by <a href="mailto:rgrimm@alum.mit.edu">Robert
 *           Grimm</a>.
 * @version  1.0
 */
public final class Evaluator implements Runnable {

  // ======================== Activation Frame ==========================

  /**
   * Implementation of an activation frame, that is, the data structure
   * used by the evaluator to explicitly manage recursion.
   */
  private static class ActivationFrame implements java.io.Serializable {
    
    /**
     * The parent activation frame, or <code>null</code> if this
     * activation frame has no parent.
     *
     * @serial
     */
    ActivationFrame parent;
    
    /**
     * The current environment.
     *
     * @serial  Must not be <code>null</code>.
     */
    Environment     env;
    
    /**
     * The current expression or sequence of expressions.
     *
     * @serial  Must be <code>null</code> if this activation frame
     *          is an extent frame.
     */
    Object          expr;
    
    /**
     * The current operator.
     *
     * @serial  Must be <code>null</code> if this activation frame
     *          is an extent frame.
     */
    Applicable      operator;
    
    /**
     * The head of the list that collects evaluated operands.
     *
     * @serial  If <code>head</code> is not the empty list, it must
     *          point to a proper list and <code>tail</code> must
     *          point to the last pair in that list. Must be
     *          <code>null</code> if this activation frame is an
     *          extent frame.
     */
    Pair            head;
    
    /**
     * The tail of the list that collects evaluated operands.
     *
     * @serial  If <code>head</code> is not the empty list,
     *          <code>tail</code> must point to the last pair in
     *          the proper list starting at the pair pointed to by
     *          <code>head</code>. Must be <code>null</code> if
     *          this activation frame is an extent frame.
     */
    Pair            tail;
    
    /**
     * The evaluator state for this activation frame, that is, the
     * opcode for the next evaluator operation.
     *
     * @serial  Must be one of the opcodes (<code>OPCODE_</code>*)
     *          defined in <code>Evaluator</code>. If this activation
     *          frame is an extent frame, this field must be
     *          <code>OPCODE_RETURN_VALUE</code>.
     */
    byte            opcode;
    
    /**
     * Flag to indicate whether this activation frame is a top-level
     * activation frame.
     *
     * @serial
     */
    boolean         topLevel;
    
    /**
     * Flag to indicate whether this activation frame has been
     * captured.  A captured activation frame must be copied before
     * modification, that is, a captured activation frame must be
     * treated as immutable. Note that extent frames must always be
     * treated as immutable.
     *
     * @serial  If <code>captured</code> is <code>true</code>,
     *          <code>captured</code> of any ancestral activation
     *          frame must also be <code>true</code>.
     */
    boolean         captured;

    /** Create a new activation frame. */
    ActivationFrame() {
      // Nothing to construct.
    }

    /**
     * Create a new top-level activation frame with the specified
     * environment and the specified expression.
     *
     * @param   env   The environment for the new activation frame.
     * @param   expr  The expression for the new activation frame.
     */
    ActivationFrame(Environment env, Object expr) {
      /* parent is automatically initialized to null. */
      this.env  = env;
      this.expr = expr;
      /*
       * operator is automatically initialized to null.
       * head and tail are automatically initialized to null,
       *   which is identical to Pair.EMPTY_LIST.
       * opcode is automatically initialized to 0, that is,
       *   OPCODE_EVAL_EXPR.
       */
      topLevel  = true;
      /* captured is automatically initialized to false. */
    }

    /**
     * Create a new top-level activation frame with the specified
     * environment, procedure, and arguments. When evaluated, this
     * activation frame causes the specified procedure to be applied
     * onto the specified arguments, without evaluating either.
     *
     * @param  env   The environment for the new activation frame.
     * @param  proc  The procedure for the new activation frame.
     *               <code>proc</code> must not be <code>null</code>.
     * @param  args  The arguments for the new activation frame.
     *               <code>args</code> must be a proper list,
     *               including the empty list.
     */
    ActivationFrame(Environment env, Applicable proc, Pair args) {
      /* parent is automatically initialized to null. */
      this.env = env;
      /* expr does not matter. */
      operator = proc;
      head     = args;
      /*
       * tail is automatically initialized to null,
       *   which is identical to Pair.EMPTY_LIST.
       */
      opcode   = OPCODE_APPLY;
      topLevel = true;
      /* captured is automatically initialized to false. */
    }
  
    /**
     * Create a new activation frame with the specified parent
     * activation frame and the specified expression.
     *
     * @param   parent  The parent activation frame for the new
     *                  activation frame.
     * @param   expr    The expression for the new activation frame.
     * @throws  NullPointerException
     *                  Signals that <code>null == parent</code>.
     */
    ActivationFrame(ActivationFrame parent, Object expr) {
      this.parent = parent;
      env         = parent.env;
      this.expr   = expr;
      /*
       * operator is automatically initialized to null.
       * head and tail are automatically initialized to null,
       *   which is identical to Pair.EMPTY_LIST.
       * opcode is automatically initialized to 0, that is,
       *   OPCODE_EVAL_EXPR.
       * topLevel and captured are automatically initialized to
       *   false.
       */
    }
  
    /**
     * Return a copy of this activation frame that is ready for
     * evaluation.
     *
     * <p>The returned activation frame has the same field values as
     * this activation frame, with the exception that
     * <code>captured</code> is <code>false</code> and that
     * <code>head</code> and <code>tail</code> point to a list that is
     * a copy of the list in this activation frame.</p>
     *
     * @return  A copy of this activation frame that is ready
     *          for evaluation.
     */
    ActivationFrame copy() {
      ActivationFrame f = new ActivationFrame();

      f.parent          = parent;
      f.env             = env;
      f.expr            = expr;
      f.operator        = operator;
      /*
       * f.head and f.tail are automatically initialized to
       * null, which is identical to Pair.EMPTY_LIST.
       */
      if (Pair.EMPTY_LIST != head) {
        Pair current    = head;
        
        Pair p          = Pair.cons(current.car, Pair.EMPTY_LIST);
        f.head          = p;
        f.tail          = p;
        
        current         = (Pair)current.cdr;
        while (Pair.EMPTY_LIST != current) {
          p             = Pair.cons(current.car, Pair.EMPTY_LIST);
          f.tail.cdr    = p;
          f.tail        = p;
          
          current       = (Pair)current.cdr;
        }
      }
      f.opcode          = opcode;
      f.topLevel        = topLevel;
      /* f.captured is automatically initialized to false. */

      return f;
    }

    /**
     * Capture this activation frame and return it. This method marks
     * this activation frame and all ancestral activation frames as
     * captured.
     *
     * @return  This activation frame.
     */
    ActivationFrame capture() {
      ActivationFrame f = this;
    
      while (null != f) {
        /*
         * If f is already captured, all its ancestral frames must be
         * captured as well.
         */
        if (f.captured) {
          break;
        } else {
          f.captured = true;
          f          = f.parent;
        }
      }

      return this;
    }

    /**
     * Determine whether the continuation represented by the chain of
     * activation frames starting at this activation frame accepts
     * multiple values.
     *
     * @return  <code>true</code> iff multiple values are accepted.
     */
    boolean acceptsValues() {
      ActivationFrame f = this;

      while (null != f) {
        
        switch (f.opcode) {
          
        case OPCODE_EVAL_EXPR:
        case OPCODE_EVAL_OPERATOR:
        case OPCODE_EVAL_OPERANDS:
        case OPCODE_APPLY:
        case OPCODE_EVAL_SEQUENCE:
        case OPCODE_EVAL_IN_SEQUENCE:
          return false;
          
        case OPCODE_EXPECT_VALUES:
          return true;
          
        case OPCODE_RETURN_VALUES:
        case OPCODE_RETURN_VALUE:
          break;
          
        default:
          throw new Bug("Illegal evaluator opcode " + f.opcode);
        }
        
        f = f.parent;
      }
      
      return false;
    }

    /**
     * Set up the specified extent frame. This method inserts the
     * specified extent frame between this activation frame and the
     * parent frame. It also copies the <code>env</code> and
     * <code>topLevel</code> fields into the specified extent frame.
     *
     * <p>If this activation frame is captured, this method copies it
     * before modification (as the parent of this activation frame is
     * changed to be the specified extent frame) and returns the
     * copy. Otherwise, this activation frame is returned.</p>
     *
     * @param   f  The extent frame to set up.
     * @return     A copy of this activation frame, if this activation
     *             frame is captured, otherwise this activation
     *             frame.
     */
    ActivationFrame setUpExtent(ExtentFrame f) {
      // Set up extent frame.
      f.parent   = parent;
      f.env      = env;
      /*
       * f.expr, f.operator, f.head, f.tail are irrelevant for
       *   extent frames.
       * f.opcode is initialized to OPCODE_RETURN_VALUE by the
       *   default constructor for ExtentFrame.
       */
      f.topLevel = topLevel;
      /* f.captured is initialized to false. */
      
      // Fix parent for this activation frame...
      ActivationFrame af = this;
      if (captured) {
        af = copy();
      }
      af.parent  = f;

      // ...and return this activation frame (or its copy).
      return af;
    }

  }

  // ========================= Extent Frame =============================

  /**
   * Definition of an extent frame, that is, the data structure used
   * used by the evaluator to mark dynamic extents for monitors,
   * exception handlers, and before/after procedures.  Note that
   * instances of this class, once created, must be treated as
   * immutable, even though its fields are directly accessible.
   */
  private static abstract class ExtentFrame extends ActivationFrame {

    /**
     * The next extent frame in the chain of frames starting at this
     * frame.
     *
     * @serial  <code>next</code> must be equal (in the sense of
     *          <code>==</code>) to the extent frame which is the
     *          first extent frame reachable through the chain of
     *          ancestral frames of this frame, or <code>null</code>
     *          if there is no extent frame in the chain of ancestral
     *          frames.
     */
    ExtentFrame     next;

    /**
     * Create a new extent frame.
     */
    ExtentFrame() {
      opcode = OPCODE_RETURN_VALUE;
    }

    /**
     * Return the length of the chain of extent frames starting at
     * this extent frame.
     *
     * @return  The length of the chain of extent frames starting
     *          this extent frame.
     */
    int length() {
      int         l = 0;
      ExtentFrame f = this;

      do {
        l++;
        f = f.next;
      } while (null != f);

      return l;
    }

  }

  // ========================== Monitor Frame ===========================

  /**
   * Implementation of a monitor frame, that is, the data structure
   * used by the evaluator to mark the dynamic extent of a monitor.
   * Note that instances of this class, once created, must be treated
   * as immutable, even though its fields are directly accessible.
   */
  private static final class MonitorFrame extends ExtentFrame {

    /**
     * The object, whose monitor this monitor frame represents.
     *
     * @serial  Must not be <code>null</code>.
     */
    Object o;

    /**
     * Create a new monitor frame for the monitor of the
     * specified object.
     *
     * @param  o  The object, whose monitor the newly created
     *            monitor frame represents. <code>o</code> must
     *            not be <code>null</code>.
     */
    MonitorFrame(Object o) {
      this.o = o;
    }

  }

  // ========================= Exception Frame ==========================

  /**
   * Implementation of an exception frame, that is, the data structure
   * used by the evaluator to mark the dynamic extent of an exception
   * handler. Note that instances of this class, once created, must be
   * treated as immutable, even though its fields are directly
   * accessible.
   */
  private static final class ExceptionFrame extends ExtentFrame {

    /**
     * The exception handler of this exception frame.
     *
     * @serial  Must not be <code>null</code>.
     */
    Applicable handler;

    /**
     * The exception type of this exception frame.
     *
     * @serial  Must be a class representing a subtype of
     *          <code>java.lang.Throwable</code>.
     */
    Class      type;

    /**
     * Create a new exception frame for the specified exception
     * handler and exception type.
     *
     * @param  handler  The exception handler for the newly created
     *                  exception frame. <code>handler</code> must not
     *                  be <code>null</code>.
     * @param  type     The exception type for the newly created
     *                  excception frame. <code>type</code> must
     *                  represent a subtype of
     *                  <code>java.lang.Throwable</code>.
     */
    ExceptionFrame(Applicable handler, Class type) {
      this.handler = handler;
      this.type    = type;
    }

  }

  // ======================== Before/After Frame ========================

  /**
   * Implementation of a before/after frame, that is, the data
   * structure used by the evaluator to mark the dynamic extent of
   * before and after procedures. Note that instances of this class,
   * once created, must be treated as immutable, even though its
   * fields are directly accessible.
   */
  private static final class BeforeAfterFrame extends ExtentFrame {

    /**
     * The before expression of this before/after frame, or
     * <code>null</code> if this before/after frame has no before
     * expression.
     *
     * @serial
     */
    Object before;

    /**
     * The after expression of this before/after frame, or
     * <code>null</code> if this before/after frame has no after
     * expression.
     *
     * @serial
     */
    Object after;
    
    /**
     * Create a new before/after frame for the specified before and
     * after expressions. Note that if any of the specified
     * expressions is <code>null</code> it is treated as such and not
     * as the empty list. In other words, if any of the specified
     * expressions is <code>null</code>, it is treated as no
     * expression.
     *
     * @param  before  The before expression, or <code>null</code>
     *                 if the newly created before/after frame
     *                 has no before expression.
     * @param  after   The after expression, or <code>null</code>
     *                 if the newly created before/after frame
     *                 has no after expression.
     */
    BeforeAfterFrame(Object before, Object after) {
      this.before = before;
      this.after  = after;
    }

  }

  // ====================== First-Class Continuation ====================

  /**
   * Implementation of a first-class continuation.
   */
  private static final class Continuation extends AbstractApplicable {

    /**
     * The chain of captured activation frames for this first-class
     * continuation.
     *
     * @serial  Must not be <code>null</code>. All activation
     *          frames in the referenced chain of activation
     *          frames must be captured.
     */
    private ActivationFrame aframe;

    /**
     * The corresponding chain of extent frames.
     *
     * @serial  <code>eframe</code> must point to the first extent
     *          frame reachable through <code>aframe</code>, or
     *          must be <code>null</code> if no extent frame is
     *          reachable through <code>aframe</code>.
     */
    private ExtentFrame     eframe;

    /**
     * The length of the chain of extent frames starting at
     * <code>eframe</code>, or -1 if the length has not yet been
     * computed.
     *
     * @serial
     */
    private int             length = -1;

    /**
     * Create a new first-class continuation for the specified
     * activation frame and the corresponding extent frame. The
     * specified activation frame (and all its ancestral activation
     * frames) must be captured.
     *
     * @param  aframe  The activation frame starting the chain of
     *                 captured activation frames.
     * @param  eframe  The corresponding chain of extent frames, that
     *                 is, <code>eframe</code> must point to the
     *                 first extent frame reachable through
     *                 <code>aframe</code>, or must be
     *                 <code>null</code> if no extent frame is
     *                 reachable through <code>aframe</code>.
     */
    Continuation(ActivationFrame aframe, ExtentFrame eframe) {
      // Does this continuation accept multiple values,
      // or expect a sequence of expressions?
      if (aframe.acceptsValues() ||
          (OPCODE_EVAL_SEQUENCE == aframe.opcode)) {
        minArgs   =  0;
        maxArgs   = -1;
      } else {
        minArgs   =  1;
        maxArgs   =  1;
      }
      opcode      = MAX_OPCODE_CONTINUATION;
      this.aframe = aframe;
      this.eframe = eframe;
    }

    /**
     * Apply this first-class continuation on the specified arguments.
     *
     * @param   l          The arguments as a proper list.
     * @param   numArgs    The number of arguments in <code>l</code>.
     * @param   evaluator  The calling evaluator.
     * @return             The result of applying this first-class
     *                     continuation on the specified arguments.
     * @throws  EvaluatorException
     *                     Signals an exceptional condition when
     *                     applying this first-class continuation on
     *                     the specified arguments.
     */
    protected Object apply1(Pair l, int numArgs, Evaluator evaluator)
      throws EvaluatorException {

      /*
       * Before installing this continuation instead of the current
       * continuation, all extent frames in the chain of activation
       * frames for the current continuation that represent a monitor
       * to be released or an after expression to be executed, as well
       * as all extent frames in the chain of activation frame for
       * this continuation that represent a monitor to be acquired or
       * a before expression to be executed, need to be
       * processed. This method collects such extent frames and, if
       * there are any, hands them off to an extent continuation for
       * processing.
       */

      /*
       * Determine length of the chain of extent frames for this
       * continuation.
       */
      if (-1 == length) {
        if (null == eframe) {
          length = 0;
        } else {
          length = eframe.length();
        }
      }

      /*
       * Determine length of the chain of extent frames for the
       * current continuation.
       */
      int currentLength = ((null == evaluator.eframe) ? 0
                           : evaluator.eframe.length());

      /*
       * The list of monitors to be released and after expressions to
       * be executed.
       */
      ArrayList afterList = null;

      /*
       * The list of monitors to be acquired and before expressions to
       * be executed.
       */
      ArrayList beforeList = null;

      // Any extent frames to look at?
      if ((0 < length) || (0 < currentLength)) {
        // Build up lists of extent frames to be processed.

        /*
         * after is the extent frame under consideration from the
         * current continuation, and before is the extent frame under
         * consideration from the new continuation.
         */
        ExtentFrame after  = evaluator.eframe;
        ExtentFrame before = eframe;

        if (length > currentLength) {
          /*
           * Process extent frames for the new continuation until both
           * chains of extent frames are of the same length.
           */
          int numIter = length - currentLength;

          for (int i=0; i<numIter; i++) {
            if (process(before, false)) {
              if (null == beforeList) {
                beforeList = new ArrayList();
              }
              beforeList.add(before);
            }

            before = before.next;
          }

        } else if (length < currentLength) {
          /*
           * Process extent frames for the current continuation until
           * both chains of extent frames are of the same length.
           */
          int numIter = currentLength - length;

          for (int i=0; i<numIter; i++) {
            if (process(after, true)) {
              if (null == afterList) {
                afterList = new ArrayList();
              }
              afterList.add(after);
            }

            after = after.next;
          }
        }

        // Both chains of extent frames are of equal length.
        while (null != before) {
          // Both extent frames are equal, we are done.
          if (after == before) {
            break;
          }

          // Process after?
          if (process(after, true)) {
            if (null == afterList) {
              afterList = new ArrayList();
            }
            afterList.add(after);
          }

          // Process before?
          if (process(before, false)) {
            if (null == beforeList) {
              beforeList = new ArrayList();
            }
            beforeList.add(before);
          }

          // Get the next pair of extent frames.
          after  = after.next;
          before = before.next;
        }
      }

      // Are we done?
      if ((null == afterList) && (null == beforeList)) {
        // No extent frames to process.

        if (1 != numArgs) {
          // This continuation is applied to multiple values.
          ActivationFrame aframe = this.aframe;

          if (aframe.opcode == OPCODE_RETURN_VALUE) {
            // aframe must be captured.
            aframe        = aframe.copy();
            aframe.opcode = OPCODE_RETURN_VALUES;
          }

          evaluator.aframe = aframe;
          evaluator.eframe = eframe;
          return l;

        } else {
          // This continuation is applied to one value.
          evaluator.aframe = aframe;
          evaluator.eframe = eframe;
          return l.car;
        }

      } else {
        // Set up extent continuation.
        ExtentContinuation cont =
          new ExtentContinuation(aframe, eframe, l, numArgs,
                                 afterList, beforeList);

        evaluator.returnApplication(cont, Pair.EMPTY_LIST, null);
        return null;
      }
    }

    /**
     * Determine whether the specified extent frame must be processed.
     *
     * @param  eframe  The extent frame to test.
     * @param  after   <code>true</code> iff the specified extent
     *                 frame is an extent frame of the current
     *                 continuation.
     */
    private boolean process(ExtentFrame eframe, boolean after) {
      if (eframe instanceof MonitorFrame) {
        return true;

      } else if (eframe instanceof BeforeAfterFrame) {
        BeforeAfterFrame baf = (BeforeAfterFrame)eframe;

        if (after && (null != baf.after)) {
          return true;
        } else if ((! after) && (null != baf.before)) {
          return true;
        }
      }

      return false;
    }

  }

  // ======================= Extent Continuation ========================

  /**
   * Implementation of an extent continuation, that is, the primitive
   * continuation used by the evaluator to evaluate before and after
   * expressions as well as release and acquire monitors when
   * replacing the old (that is, current) continuation with a new
   * (that is, previously captured) continuation.
   */
  private static final class ExtentContinuation extends AbstractApplicable {

    /**
     * The chain of captured activation frames for the new
     * continuation.
     *
     * @serial  Must not be <code>null</code>.
     */
    private ActivationFrame aframe;

    /**
     * The corresponding chain of extent frames.
     *
     * @serial  <code>eframe</code> must point to the first extent
     *          frame reachable through <code>aframe</code>, or
     *          must be <code>null</code> if no extent frame is
     *          reachable through <code>aframe</code>.
     */
    private ExtentFrame     eframe;

    /**
     * The arguments for the new continuation.
     *
     * @serial  Must be a proper list, including the empty list.
     */
    private Pair            l;

    /**
     * The number of arguments for the new continuation.
     *
     * @serial  Must be the length of <code>l</code>.
     */
    private int             numArgs;

    /**
     * The list of after expressions to be evaluated and monitors to
     * be released from innermost to outermost dynamic extent, or
     * <code>null</code> if there are no after expressions and
     * monitors associated with the old continuation.
     *
     * @serial  All elements of <code>afterList</code> must be
     *          instances of <code>BeforeAfterFrame</code> or
     *          <code>MonitorFrame</code>.
     */
    private ArrayList       afterList;

    /**
     * The current index into <code>afterList</code>, that is, the
     * next after expression or monitor to be processed.
     *
     * @serial
     */
    private int             afterIndex;

    /**
     * The list of before expressions to be evaluated and monitors to
     * be acquired from innermost to outermost dynamic extent, or
     * <code>null</code> if there are no before expressions and
     * monitors associated with the new continuation.
     *
     * @serial  All elements of <code>beforeList</code> must be
     *          instances of <code>BeforeAfterFrame</code> or
     *          <code>MonitorFrame</code>.
     */
    private ArrayList       beforeList;

    /**
     * Create a new extent continuation for the specified new
     * continuation and lists of before/after expressions and
     * monitors.
     *
     * @param  aframe   The chain of activation frames for the new
     *                  continuation, which must not be
     *                  <code>null</code>.
     * @param  eframe   The corresponding chain of extent frames,
     *                  that is, the first extent frame reachable
     *                  through <code>aframe</code>, or
     *                  <code>null</code> if no extent frame is
     *                  reachable through <code>aframe</code>.
     * @param  l        The list of arguments for the new
     *                  continuation, which must be a proper list.
     * @param  numArgs  The number of arguments in <code>l</code>,
     *                  which must be the length of <code>l</code>.
     * @param  afterList
     *                  The list of after expressions and monitors
     *                  to be processed for the old continuation.
     * @param  beforeList
     *                  The list of before expressions and monitors
     *                  to be processed for the new continuation.
     */
    ExtentContinuation(ActivationFrame aframe, ExtentFrame eframe,
                       Pair l, int numArgs,
                       ArrayList afterList, ArrayList beforeList) {

      this.aframe     = aframe;
      this.eframe     = eframe;
      this.l          = l;
      this.numArgs    = numArgs;
      this.afterList  = afterList;
      afterIndex      = 0;
      this.beforeList = beforeList;
    }

    /**
     * Apply extent after continuation on the specified arguments.
     *
     * @param   l          The arguments as a proper list.
     * @param   numArgs    The number of arguments in <code>l</code>.
     * @param   evaluator  The calling evaluator.
     * @return             The result of applying this extent
     *                     continuation on the specified arguments.
     * @throws  EvaluatorException
     *                     Signals an exceptional condition when
     *                     applying this extent continuation on
     *                     the specified arguments.
     */
    protected Object apply1(Pair l, int numArgs, Evaluator evaluator) 
      throws EvaluatorException {

      if (null != afterList) {
        // Process next extent frame for old continuation.
        ExtentFrame     eframe = (ExtentFrame)afterList.get(afterIndex);
        ActivationFrame aframe = eframe.copy();

        // Fix afterIndex to point to next extent frame.
        afterIndex++;
        if (afterList.size() <= afterIndex) {
          // Done with extent frames for old continuation.
          afterList = null;
        }

        if (eframe instanceof MonitorFrame) {
          // Set up this as primitive continuation.
          aframe.operator    = this;
          aframe.opcode      = OPCODE_APPLY;

          // Release monitor.
          evaluator.temp     = ((MonitorFrame)eframe).o;
          evaluator.tempFlag = TEMP_FLAG_RELEASE_MONITOR;

          // Go.
          evaluator.aframe   = aframe;
          evaluator.eframe   = eframe.next;
          return null;
          
        } else {                      // eframe must be a BeforeAfterFrame.
          // Set up this as primitive continuation.
          aframe.operator    = this;
          aframe.opcode      = OPCODE_APPLY;

          // Set up a new activation frame for the after expression.
          aframe             = new ActivationFrame(aframe, Pair.EMPTY_LIST);

          // Go.
          evaluator.aframe   = aframe;
          evaluator.eframe   = eframe.next;
          return ((BeforeAfterFrame)eframe).after;
        }

      } else if (null != beforeList) {
        // Process next extent frame for new continuation.
        ExtentFrame     eframe =
          (ExtentFrame)beforeList.remove(beforeList.size() - 1);
        ActivationFrame aframe = eframe.copy();

        if (0 == beforeList.size()) {
          // Done with extent frames for new continuation.
          beforeList = null;
        }

        if (eframe instanceof MonitorFrame) {
          // Set up this as primitive continuation.
          aframe.operator    = this;
          aframe.opcode      = OPCODE_APPLY;

          // Acquire monitor.
          evaluator.temp     = ((MonitorFrame)eframe).o;
          evaluator.tempFlag = TEMP_FLAG_ACQUIRE_MONITOR;

          // Go.
          evaluator.aframe   = aframe;
          evaluator.eframe   = eframe.next;
          return null;

        } else {                      // eframe must be a BeforeAfterFrame.
          // Set up this as primitive continuation.
          aframe.operator    = this;
          aframe.opcode      = OPCODE_APPLY;

          // Set up a new activation frame for the before expression.
          aframe             = new ActivationFrame(aframe, Pair.EMPTY_LIST);

          // Go.
          evaluator.aframe   = aframe;
          evaluator.eframe   = eframe.next;
          return ((BeforeAfterFrame)eframe).before;
        }

      } else {
        // We are done processing extent frames.
        if (1 != this.numArgs) {
          ActivationFrame aframe = this.aframe;

          if (aframe.opcode == OPCODE_RETURN_VALUE) {
            // aframe must be captured.
            aframe        = aframe.copy();
            aframe.opcode = OPCODE_RETURN_VALUES;
          }

          evaluator.aframe = aframe;
          evaluator.eframe = eframe;
          return this.l;

        } else {
          evaluator.aframe = aframe;
          evaluator.eframe = eframe;
          return this.l.car;
        }
      }
    }

  }

  // ======================= After Continuation =========================

  /**
   * Implementation of an after continuation, that is, the primitive
   * continuation used by the evaluator to execute after procedures
   * when regularily returning from an activation frame or when
   * propagating an exception.
   */
  private static final class AfterContinuation extends AbstractApplicable {

    /** Flag to indicate a value. */
    private static final int FLAG_VALUE     = 0;

    /** Flag to indicate a sequence of values. */
    private static final int FLAG_VALUES    = 1;

    /** Flag to indicate a throwable. */
    private static final int FLAG_THROWABLE = 2;

    /**
     * Flag for the type of result which is returned from this after
     * continuation.
     *
     * @serial  Must be <code>FLAG_VALUE</code>, <code>FLAG_VALUES</code>,
     *          or <code>FLAG_THROWABLE</code>.
     */
    private int    flag;

    /**
     * The result to return from this after continuation.
     *
     * @serial  If <code>flag</code> is <code>FLAG_VALUES</code>,
     *          <code>result</code> must be a proper list (including
     *          the empty list). If <code>flag</code> is
     *          <code>FLAG_THROWABLE</code>, <code>result</code>
     *          must be an instance of <code>java.lang.Throwable</code>.
     */
    private Object result;

    /**
     * Create a new after continuation for the specified result. Upon
     * application, the newly created after continuation ignores its
     * argument and returns the specified result. If
     * <code>values</code> is <code>true</code>, the specified result
     * must be a proper list (including the empty list) and is treated
     * as a sequence of values. Otherwise, it is treated as a single
     * value.
     *
     * @param  result  The value(s) to return on application.
     * @param  values  <code>true</code> iff the specified result
     *                 is a sequence of values, in which case
     *                 <code>result</code> must be a proper list
     *                 (including the empty list).
     */
    AfterContinuation(Object result, boolean values) {
      minArgs     = 0;
      maxArgs     = 0;
      opcode      = MAX_OPCODE_PRIMITIVE_CONTINUATION;
      if (values) {
        flag      = FLAG_VALUES;
      } else {
        flag      = FLAG_VALUE;
      }
      this.result = result;
    }

    /**
     * Create a new after continuation for the specified
     * throwable. Upon application, the newly created after
     * continuation ignores its argument and signals the specified
     * throwable.
     *
     * @param  x  The throwable to signal on application.
     */
    AfterContinuation(Throwable x) {
      minArgs = 0;
      maxArgs = 0;
      opcode  = MAX_OPCODE_PRIMITIVE_CONTINUATION;
      flag    = FLAG_THROWABLE;
      result  = x;
    }

    /**
     * Apply this after continuation on the specified arguments.
     *
     * @param   l          The arguments as a proper list.
     * @param   numArgs    The number of arguments in <code>l</code>.
     * @param   evaluator  The calling evaluator.
     * @return             The result of applying this after
     *                     continuation on the specified arguments.
     * @throws  EvaluatorException
     *                     Signals an exceptional condition when
     *                     applying this after continuation on
     *                     the specified arguments.
     */
    protected Object apply1(Pair l, int numArgs, Evaluator evaluator) 
      throws EvaluatorException {

      if (FLAG_THROWABLE == flag) {
        EvaluatorException.signal((Throwable)result); // Never returns.

      } else if (FLAG_VALUES == flag) {
        evaluator.returnValues();                     // Must not signal.
      }

      return result;
    }

  }

  // ============================ Constants =============================

  /** Opcode: evaluate an expression. Must be 0. */
  static final int OPCODE_EVAL_EXPR          = 0;

  /** Opcode: evaluate the operator. */
  static final int OPCODE_EVAL_OPERATOR      = 1;

  /** Opcode: evaluate the operands. */
  static final int OPCODE_EVAL_OPERANDS      = 2;

  /** Opcode: apply non-syntactic operator on operands. */
  static final int OPCODE_APPLY              = 3;

  /** Opcode: evaluate expression as sequence of expressions. */
  static final int OPCODE_EVAL_SEQUENCE      = 4;

  /** Opcode: evaluate next expression in a sequence of expressions. */
  static final int OPCODE_EVAL_IN_SEQUENCE   = 5;

  /** Opcode: operator accepts values, that is, zero or more values. */
  static final int OPCODE_EXPECT_VALUES      = 6;

  /** Opcode: return values. */
  static final int OPCODE_RETURN_VALUES      = 7;

  /** Opcode: return value. */
  static final int OPCODE_RETURN_VALUE       = 8;

  /** Flag to install monitor extent frame. */
  static final int TEMP_FLAG_INSTALL_MONITOR = 0;

  /** Flag to install non-monitor extent frame. */
  static final int TEMP_FLAG_INSTALL_EXTENT  = 1;

  /** Flag to acquire monitor. */
  static final int TEMP_FLAG_ACQUIRE_MONITOR = 2;

  /** Flag to release monitor. */
  static final int TEMP_FLAG_RELEASE_MONITOR = 3;

  // ======================= Instance variables =========================

  /* The reference list of I/O streams to close on clean up. */
  private ReferenceList      markedList;

  /** The language format. */
  private Format             format;

  /** The stack of readers. */
  private ArrayList          readers;

  /** The stack of writers. */
  private ArrayList          writers;

  /** The top-level environment. */
  private Environment        topLevelEnvironment;

  /** The current activation frame. */
  private ActivationFrame    aframe;

  /**
   * The current extent frame, which must be the first extent frame
   * reachable through <code>aframe</code>, or <code>null</code> if no
   * extent frame is reachable through <code>aframe</code>.
   */
  private ExtentFrame        eframe;

  /** The new extent to install, or the monitor to acquire/release. */
  private Object             temp;

  /** Flag for temp object. */
  private int                tempFlag;

  /** Flag to indicate whether the evaluator is running. */
  private volatile boolean   running;

  /** Flag to indicate whether the evaluator has produced a result. */
  private volatile boolean   resultReady;

  /** Flag to indicate whether the result is a value. */
  private boolean            successful;

  /**
   * The result of the previous evaluation. This field only holds a
   * valid reference if <code>running</code> is <code>false</code>. If
   * <code>successful</code> is <code>true</code>, it references a
   * value, otherwise it references a throwable.
   */
  private Object             result;

  // ========================== Constructors ============================

  /** Create a new evaluator. */
  private Evaluator() {
    // Nothing to construct.
  }
  
  /**
   * Create a new evaluator with the specified top-level environment,
   * language format, top-level reader, and top-level writer.
   *
   * @param   env     The top-level environment for the new evaluator.
   * @param   format  The language format for the new evaluator.
   * @param   in      The top-level reader for the new evaluator.
   * @param   out     The top-level writer for the new evaluator.
   * @throws  NullPointerException
   *                  Signals that any of the arguments is
   *                  <code>null</code>.
   */
  public Evaluator(Environment env, Format format, Reader in, Writer out) {
    if (null == env) {
      throw new NullPointerException("Null top-level environment");
    } else if (null == format) {
      throw new NullPointerException("Null language format");
    } else if (null == in) {
      throw new NullPointerException("Null top-level reader");
    } else if (null == out) {
      throw new NullPointerException("Null top-level writer");
    }

    markedList          = new ReferenceList();
    this.format         = format;
    readers             = new ArrayList();
    readers.add(in);
    writers             = new ArrayList();
    writers.add(out);
    topLevelEnvironment = env;
    /*
     * aframe is automatically initialized to null.
     * eframe is automatically initialized to null.
     * temp is automatically initialized to null.
     * tempFlag is automatically initialized to zero.
     * running is automatically initialized to false.
     * resultReady is automatically initialized to false.
     * successful doesn't matter.
     * result doesn't matter.
     */
  }

  // ============================== Methods =============================

  /**
   * Mark the specified input stream for clean up. If the specified
   * input stream is not unmarked again by a call to
   * <code>unmarkForCleanup()</code>, the evaluator guarantees that
   * the specified input stream will be closed before exiting the
   * system. Marking an already marked input stream has no effect.
   *
   * @see     #unmarkForCleanup(InputStream)
   *
   * @param   in  The input stream to mark.
   */
  public void markForCleanup(InputStream in) {
    if ((null != in) && (-1 == markedList.find(in))) {
      markedList.add(in);
    }
  }

  /**
   * Unmark the specified input stream for clean up. Unmarking an
   * already unmarked input stream or an input stream that has never
   * been marked has no effect.
   *
   * @see     #markForCleanup(InputStream)
   *
   * @param   in  The input stream to unmark.
   */
  public void unmarkForCleanup(InputStream in) {
    int index = markedList.find(in);

    if (-1 != index) {
      markedList.remove(index);
    }
  }

  /**
   * Mark the specified output stream for clean up. If the specified
   * output stream is not unmarked again by a call to
   * <code>unmarkForCleanup()</code>, the evaluator guarantees that
   * the specified output stream will be closed before exiting the
   * system. Marking an already marked output stream has no effect.
   *
   * @see     #unmarkForCleanup(OutputStream)
   *
   * @param   in  The Output stream to mark.
   */
  public void markForCleanup(OutputStream out) {
    if ((null != out) && (-1 == markedList.find(out))) {
      markedList.add(out);
    }
  }

  /**
   * Unmark the specified output stream for clean up. Unmarking an
   * already unmarked output stream or an output stream that has never
   * been marked has no effect.
   *
   * @see     #markForCleanup(OutputStream)
   *
   * @param   in  The output stream to unmark.
   */
  public void unmarkForCleanup(OutputStream out) {
    int index = markedList.find(out);

    if (-1 != index) {
      markedList.remove(index);
    }
  }

  /**
   * Return the language format for this evaluator.
   *
   * @return  The language format for this evaluator.
   */
  public Format getFormat() {
    return format;
  }

  /**
   * Return the top-level reader. The top-level reader typically
   * accepts user input from either the console or some user window.
   *
   * @return  The top-level reader.
   */
  public Reader getTopLevelReader() {
    return (Reader)readers.get(0);
  }

  /**
   * Return the current reader. Initially, the current reader is
   * identical to the top-level reader. However, to facilitate I/O
   * redirection, the current reader can be changed by pushing and
   * popping readers onto and from the stack of readers.
   *
   * @see     #pushReader(Reader)
   * @see     #popReader()
   *
   * @return  The current reader.
   */
  public Reader getCurrentReader() {
    return (Reader)readers.get(readers.size() - 1);
  }

  /**
   * Push the specified reader. This method pushes the specified
   * reader onto the stack of readers and makes it the current reader.
   *
   * @param   in  The reader to serve as the new current reader.
   * @throws  NullPointerException
   *              Signals that <code>in</code> is
   *              <code>null</code>.
   */
  public void pushReader(Reader in) {
    if (null == in) {
      throw new NullPointerException("Null reader");
    } else {
      readers.add(in);
    }
  }

  /**
   * Pop the current reader. This method pops the current reader from
   * the stack of readers and restores the current reader to the
   * reader that was the current reader before the last call to
   * <code>pushReader()</code>. If the current reader before calling
   * this method is the top-level reader, calling this method has no
   * effect.
   *
   * @see     #pushReader(Reader)
   *
   * @return  The current reader before calling this method,
   *          or <code>null</code> if the current reader before
   *          calling this method is the top-level reader.
   */
  public Reader popReader() {
    int size = readers.size();
    if (1 == size) {
      return null;
    } else {
      return (Reader)readers.remove(size - 1);
    }
  }

  /**
   * Mark the specified reader for clean up. If the specified reader
   * is not unmarked again by a call to
   * <code>unmarkForCleanup()</code>, the evaluator guarantees that
   * the specified reader will be closed before exiting the
   * system. Marking an already marked reader has no effect.
   *
   * @see     #unmarkForCleanup(Reader)
   *
   * @param   in  The reader to mark.
   */
  public void markForCleanup(Reader in) {
    if ((null != in) && (-1 == markedList.find(in))) {
      markedList.add(in);
    }
  }

  /**
   * Unmark the specified reader for clean up. Unmarking an already
   * unmarked reader or a reader that has never been marked has no
   * effect.
   *
   * @see     #markForCleanup(Reader)
   *
   * @param   in  The reader to unmark.
   */
  public void unmarkForCleanup(Reader in) {
    int index = markedList.find(in);

    if (-1 != index) {
      markedList.remove(index);
    }
  }

  /**
   * Return the top-level writer. The top-level writer typically
   * provides user output to either the console or some user window.
   *
   * @return  The top-level writer.
   */
  public Writer getTopLevelWriter() {
    return (Writer)writers.get(0);
  }

  /**
   * Return the current writer. Initially, the current writer is
   * identical to the top-level writer. However, to facilitate I/O
   * redirection, the current writer can be changed by pushing and
   * popping writers onto and from the stack of writers.
   *
   * @return  The current writer.
   */
  public Writer getCurrentWriter() {
    return (Writer)writers.get(writers.size() - 1);
  }

  /**
   * Push the specified writer. This method pushes the specified
   * writer onto the stack of writers and makes it the current writer.
   *
   * @param   out  The writer to serve as the new current writer.
   * @throws  NullPointerException
   *               Signals that <code>out</code> is
   *               <code>null</code>.
   */
  public void pushWriter(Writer out) {
    if (null == out) {
      throw new NullPointerException("Null writer");
    } else {
      writers.add(out);
    }
  }

  /**
   * Pop the current writer. This method pops the top-most writer from
   * the stack of writers and restores the current writer to the
   * writer that was the current writer before the last call to
   * <code>pushWriter()</code>. If the current writer before calling
   * this methodis the top-level writer, calling this method has no
   * effect.
   *
   * @see     #pushWriter(Writer)
   *
   * @return  The current writer before calling this method, or
   *          <code>null</code> if the current writer before
   *          calling this method is the top-level writer.
   */
  public Writer popWriter() {
    int size = writers.size();
    if (1 == size) {
      return null;
    } else {
      return (Writer)writers.remove(size - 1);
    }
  }

  /**
   * Mark the specified writer for clean up. If the specified writer
   * is not unmarked again by a call to
   * <code>unmarkForCleanup()</code>, the evaluator guarantees that
   * the specified writer will be closed before exiting the
   * system. Marking an already marked writer has no effect.
   *
   * @see     #unmarkForCleanup(Writer)
   *
   * @param   out  The writer to mark.
   */
  public void markForCleanup(Writer out) {
    if ((null != out) && (-1 == markedList.find(out))) {
      markedList.add(out);
    }
  }

  /**
   * Unmark the specified writer. Unmarking an already unmarked writer
   * or a writer that has never been marked has no effect.
   *
   * @see     #markForCleanup(Writer)
   *
   * @param   out  The writer to unmark.
   */
  public void unmarkForCleanup(Writer out) {
    int index = markedList.find(out);

    if (-1 != index) {
      markedList.remove(index);
    }
  }

  /**
   * Determine whether the specified expression is a simple
   * expression.
   *
   * <p>A simple expression is an expression that, when evaluated,
   * never results in a recursive descent of the evaluator, that is,
   * in the creation of new activation frames. Consequently, a simple
   * expression can be evaluated directly, by calling
   * <code>evalSimpleExpression()</code>, instead of returning it to
   * the evaluator for explicit evaluation.</p>
   *
   * <p>Any Java object that is not a <code>Pair</code> is a simple
   * expression.</p>
   *
   * @see     #evalSimpleExpression(Object)
   *
   * @param   expr  The expression to test.
   * @return        <code>true</code> iff the specified expression
   *                is a simple expression.
   */
  public boolean isSimpleExpression(Object expr) {
    return (! (expr instanceof Pair));
  }

  /**
   * Evaluate the specified simple expression in the current
   * activation frame.
   *
   * <p>The behavior of this method is unspecified if it is not
   * invoked as an up-call from within an applicable entity's
   * <code>apply()</code> method.</p>
   *
   * @see     #isSimpleExpression(Object)
   *
   * @param   expr  The simple expression to evaluate.
   * @return        The result of evaluating the specified simple
   *                expression.
   * @throws  EvaluatorException
   *                Signals an exceptional condition when evaluating
   *                the specified simple expression.
   * @throws  IllegalArgumentException
   *                Signals that <code>expr</code> is not a simple
   *                expression.
   */
  public Object evalSimpleExpression(Object expr) throws EvaluatorException {
    if (expr instanceof Pair) {
      throw new IllegalArgumentException("Not a simple expression");
    } else {
      return evalSimpleExpression1(expr);
    }
  }

  /**
   * Evaluate the specified simple expression in the current
   * activation frame. The specified expression must be a simple
   * expression.
   *
   * <p>The behavior of this method is unspecified if it is not
   * invoked as an up-call from within an applicable entity's
   * <code>apply()</code> method.</p>
   *
   * @param   expr  The simple expression to evaluate.
   * @return        The result of evaluating the specified simple
   *                expression.
   * @throws  EvaluatorException
   *                Signals an exceptional condition when evaluating
   *                the specified simple expression.
   */
  private Object evalSimpleExpression1(Object expr) throws EvaluatorException {
    if (expr instanceof Symbol) {
      Object o = aframe.env.lookup((Symbol)expr);

      if ((o instanceof NotAValue) ||
          ((o instanceof Applicable) && ((Applicable)o).isSyntactic())) {
        throw new NotAValueException(o);
      }

      return o;

    } else {
      return expr;
    }
  }
  
  /**
   * Return the top-level environment.
   *
   * @return  The top-level environment.
   */
  public Environment getTopLevelEnvironment() {
    return topLevelEnvironment;
  }

  /**
   * Return the current environment.
   *
   * <p>Note that the current environment or any of its ancestral
   * environments may be sealed. Consequently, a <code>bind</code> or
   * <code>modify</code> operation on the current environment may fail
   * with a <code>UnsupportedOperationException</code>. Applicable
   * entities must catch this exception and throw a corresponding
   * <code>BindingException</code>.</p>
   *
   * <p>The behavior of this method is unspecified if it is not
   * invoked as an up-call from within an applicable entity's
   * <code>apply()</code> method.</p>
   *
   * @see     Environment
   * @see     BindingException
   *
   * @return  The current environment.
   */
  public Environment getCurrentEnvironment() {
    return aframe.env;
  }

  /**
   * Set the current environment to the specified environment. The
   * specified environment may be sealed.
   *
   * <p>The behavior of this method is unspecified if it is not
   * invoked as an up-call from within an applicable entity's
   * <code>apply()</code> method.</p>
   *
   * @see     #getCurrentEnvironment()
   *
   * @param   env  The new current environment.
   * @throws  NullPointerException
   *               Signals that <code>env</code> is
   *               <code>null</code>.
   */
  public void setCurrentEnvironment(Environment env) {
    if (null == env) {
      throw new NullPointerException("Null environment");
    } else {
      if (aframe.captured) {
        aframe = aframe.copy();
      }

      aframe.env = env;
    }
  }

  /**
   * Determine whether the current activation frame is a top-level
   * frame.
   *
   * <p>The behavior of this method is unspecified if it is not
   * invoked as an up-call from within an applicable entity's
   * <code>apply()</code> method.</p>
   *
   * @return  <code>true</code> iff the current activation frame
   *          is a top-level frame.
   */
  public boolean isTopLevel() {
    return aframe.topLevel;
  }

  /**
   * Mark or unmark the current activation frame as a top-level frame.
   *
   * <p>The behavior of this method is unspecified if it is not
   * invoked as an up-call from within an applicable entity's
   * <code>apply()</code> method.</p>
   *
   * @see    #isTopLevel()
   *
   * @param  topLevel  <code>true</code> iff the current frame should
   *                   be marked as a top-level frame.
   */
  public void setTopLevel(boolean topLevel) {
    if (aframe.captured) {
      aframe = aframe.copy();
    }

    aframe.topLevel = topLevel;
  }

  /**
   * Mark the result of the current invocation of an applicable
   * entity's <code>apply()</code> method as a value to be returned
   * from the current activation frame.
   *
   * <p>The behavior of this method is unspecified if it is not
   * invoked as an up-call from within an applicable entity's
   * <code>apply()</code> method.</p>
   *
   * @see  Applicable#apply(Pair,Evaluator)
   */
  public void returnValue() {
    if (aframe.captured) {
      aframe = aframe.copy();
    }

    aframe.opcode = OPCODE_RETURN_VALUE;
  }

  /**
   * Determine whether the current continuation accepts multiple
   * values. This method determines whether the current continuation
   * as represented by the current chain of activation frames accepts
   * multiple values.
   *
   * <p>If this method returns <code>true</code>, the current
   * invocation of an applicable entity's <code>apply()</code> method
   * can return either a single value or a sequence of values. A
   * single return value is marked using <code>returnValue()</code>,
   * and a sequence of return values is marked using
   * <code>returnValues()</code>.</p>
   *
   * <p>Note that only activation frames set up with a call to
   * <code>setUpValues()</code> can accept a sequence of values.</p>
   *
   * <p>The behavior of this method is unspecified if it is not
   * invoked as an up-call from within an applicable entity's
   * <code>apply()</code> method.</p>
   *
   * @see     #returnValue()
   * @see     #returnValues()
   * @see     #setUpValues(Applicable)
   * 
   * @return  <code>true</code> iff the current continuation accepts
   *          multiple values.
   */
  public boolean acceptsValues() {
    return aframe.acceptsValues();
  }

  /**
   * Mark the result of the current invocation of an applicable
   * entity's <code>apply()</code> method as a sequence of values to
   * be returned from the current activation frame.
   *
   * <p>The result returned from the current invocation of an
   * applicable entity's <code>apply()</code> method must be a proper
   * list, including the empty list.</p>
   *
   * <p>Note that only activation frames set up with a call to
   * <code>setUpValues()</code> can accept a sequence of values.</p>
   *
   * <p>The behavior of this method is unspecified if it is not
   * invoked as an up-call from within an applicable entity's
   * <code>apply()</code> method.</p>
   *
   * @see     Applicable#apply(Pair,Evaluator)
   * @see     #setUpValues(Applicable)
   * @see     #acceptsValues()
   *
   * @throws  IllegalStateException
   *             Signals that the current continuation does not
   *             accept multiple values.
   */
  public void returnValues() {
    if (! aframe.acceptsValues()) {
      throw new IllegalStateException("Multiple values not acceptable");
    }

    if (aframe.captured) {
      aframe = aframe.copy();
    }

    aframe.opcode = OPCODE_RETURN_VALUES;
  }

  /**
   * Mark the result of the current invocation of an applicable
   * entity's <code>apply()</code> method as an expression to be
   * evaluated in the current activation frame.
   *
   * <p>If the current activation frame is a top-level frame, it is
   * demoted to a regular activation frame.</p>
   *
   * <p>The behavior of this method is unspecified if it is not
   * invoked as an up-call from within an applicable entity's
   * <code>apply()</code> method.</p>
   *
   * @see  Applicable#apply(Pair,Evaluator)
   */
  public void returnExpression() {
    if (aframe.captured) {
      aframe = aframe.copy();
    }

    aframe.operator = null;
    aframe.head     = Pair.EMPTY_LIST;
    aframe.tail     = Pair.EMPTY_LIST;
    aframe.opcode   = OPCODE_EVAL_EXPR;
    aframe.topLevel = false;
  }

  /**
   * Mark the result of the current invocation of an applicable
   * entity's <code>apply()</code> method as a sequence of expressions
   * to be evaluated in the current activation frame.
   *
   * <p>The result returned from the current invocation of an
   * applicable entity's <code>apply()</code> method must be a proper
   * list of length at least 1.</p>
   *
   * <p>If the current activation frame is a top-level frame, it is
   * demoted to a regular activation frame.</p>
   *
   * <p>The behavior of this method is unspecified if it is not
   * invoked as an up-call from within an applicable entity's
   * <code>apply()</code> method.</p>
   *
   * @see  Applicable#apply(Pair,Evaluator)
   */
  public void returnSequence() {
    if (aframe.captured) {
      aframe = aframe.copy();
    }

    aframe.operator = null;
    aframe.head     = Pair.EMPTY_LIST;
    aframe.tail     = Pair.EMPTY_LIST;
    aframe.opcode   = OPCODE_EVAL_SEQUENCE;
    aframe.topLevel = false;
  }

  /**
   * Return the application of the specified procedure on the
   * specified arguments and, possibly, the application of the
   * specified continuation on the result of the first application.
   *
   * <p>This method modifies the current activation frame so that, on
   * return from the current invocation of an applicable entity's
   * <code>apply()</code> method, <code>proc</code> is applied onto
   * <code>args</code> without evaluting the arguments. If
   * <code>cont</code> is not <code>null</code>, <code>cont</code>, in
   * turn, is applied on the result of the first application.</p>
   *
   * <p>If <code>cont</code> is <code>null</code>, the result of
   * applying <code>proc</code> on <code>args</code> becomes the
   * result returned from the current activation frame. If
   * <code>cont</code> is not <code>null</code>, the result of
   * applying <code>cont</code> on the result of the first application
   * becomes the result returned from the current activation
   * frame.</p>
   *
   * <p>In the latter case, this method creates an additional
   * activation frame which is the current activation frame when this
   * method returns.</p>
   *
   * <p>The actual result returned from the current invocation of an
   * applicable entity's <code>apply()</code> method, that is, the
   * result of the <code>apply()</code> method that makes this
   * up-call, is ignored.</p>
   *
   * <p>If the current activation frame is a top-level frame, it is
   * demoted to a regular activation frame. If an additional
   * activation frame is created, it is a regular activation
   * frame.</p>
   *
   * <p>This method makes it possible to apply a procedure on
   * arguments that should not be evaluated because, for example, they
   * already have been evaluated.</p>
   *
   * <p>The behavior of this method is unspecified if it is not
   * invoked as an up-call from within an applicable entity's
   * <code>apply()</code> method.</p>
   *
   * @param   proc  The applicable entity to apply on the specified
   *                arguments.
   * @param   args  The proper list of arguments to apply
   *                <code>proc</code> on.
   * @param   cont  The (primitive) continuation to apply on the result
   *                of applying <code>proc</code>, or <code>null</code>
   *                if no continuation is required.
   * @throws  NullPointerException
   *                Signals that <code>null == proc</code>.
   * @throws  IllegalArgumentException
   *                Signals that <code>args</code> is not a proper
   *                list.
   */
  public void returnApplication(Applicable proc, Pair args, Applicable cont) {
    // Verify arguments.
    if (null == proc) {
      throw new NullPointerException("Null procedure");
    } else if ((Pair.EMPTY_LIST != args) &&
               (! args.isList())) {
      throw new IllegalArgumentException("Arguments not a list");
    }

    // Copy-on-write.
    if (aframe.captured) {
      aframe = aframe.copy();
    }

    // Set up continuation.
    if (null != cont) {
      aframe.operator = cont;
      aframe.head     = Pair.EMPTY_LIST;
      aframe.tail     = Pair.EMPTY_LIST;
      aframe.opcode   = OPCODE_EVAL_OPERANDS;
      aframe.topLevel = false;
      
      aframe          = new ActivationFrame(aframe, Pair.EMPTY_LIST);
    }

    // Set up application.
    aframe.operator   = proc;
    aframe.head       = args;
    aframe.tail       = Pair.EMPTY_LIST;
    aframe.opcode     = OPCODE_APPLY;
    aframe.topLevel   = false;
  }

  /**
   * Set up the specified procedure as an applicable entity to be
   * applied on a sequence of values.
   *
   * <p>This method sets up the specified applicable entity within the
   * current activation frame as a procedure to be applied on a
   * sequence of values. It then creates a new activation frame that
   * expects an expression to be returned from the current invocation
   * of an applicable entity's <code>apply()</code> method. The
   * value(s) returned from that activation frame become(s) the
   * value(s) the specified procedure is applied on.</p>
   *
   * <p>Note that it is possible to return a <em>single</em> value to
   * the specified procedure by calling <code>returnValue()</code>
   * from an applicable entity's <code>apply()</code> method, and to
   * return a sequence of values by calling
   * <code>returnValues()</code>. In the first case, the specified
   * procedure is applied on that single value and, in the second
   * case, on all values (in order).</p>
   *
   * <p>If the current activation frame is a top-level frame, it is
   * demoted to a regular activation frame. The newly created
   * activation frame is a regular activation frame.</p>
   *
   * <p>This method makes it possible to create a continuation that,
   * when passed some values, calls the specified procedure with those
   * values as arguments. In fact, it is the <em>only</em> way to
   * create a continuation that accepts multiple values.</p>
   *
   * <p>The behavior of this method is unspecified if it is not
   * invoked as an up-call from within an applicable entity's
   * <code>apply()</code> method.</p>
   *
   * @param   proc  The applicable entity to set up for application
   *                on a sequence of values.
   * @throws  NullPointerException
   *                Signals that <code>null == proc</code>.
   */
  public void setUpValues(Applicable proc) {
    // Verify arguments.
    if (null == proc) {
      throw new NullPointerException("Null procedure");
    }

    // Copy-on-write.
    if (aframe.captured) {
      aframe = aframe.copy();
    }

    // Set up procedure.
    aframe.operator = proc;
    aframe.head     = Pair.EMPTY_LIST;
    aframe.tail     = Pair.EMPTY_LIST;
    aframe.opcode   = OPCODE_EXPECT_VALUES;
    aframe.topLevel = false;

    // Set up new activation frame.
    aframe          = new ActivationFrame(aframe, Pair.EMPTY_LIST);
  }

  /**
   * Set up a monitor for the specified object. This method marks a
   * new dynamic extent, during which the current thread holds the
   * Java monitor for the specified object. The monitor is
   * automatically acquired when control enters the dynamic extent,
   * and it is automatically released when control leaves the dynamic
   * extent.
   *
   * <p>The behavior of this method is unspecified if it is not
   * invoked as an up-call from within an applicable entity's
   * <code>apply()</code> method.</p>
   *
   * <p>If the current continuation is captured through a call to
   * <code>getCurrentContinuation()</code> after calling this method
   * but before returning form the same invocation of an applicable
   * entity's <code>apply()</code> method, the returned continuation
   * does <em>not</em> reflect the dynamic extent set up by this
   * method.</p>
   *
   * @see     #setUpExceptionHandler(Applicable,Class)
   * @see     #setUpBeforeAfter(Object,Object)
   *
   * @see     #getCurrentContinuation() 
   *
   * @param   o  The object whose monitor to acquire.
   * @throws  NullPointerException
   *             Signals that <code>null == o</code>.
   * @throws  IllegalStateException
   *             Signals that the current invocation of an
   *             applicable entity's <code>apply()</code> method
   *             has already created a new dynamic extent.
   */
  public void setUpMonitor(Object o) {
    // Verify argument.
    if (null == o) {
      throw new NullPointerException("Null object");
    }

    // Verify state.
    if (null != temp) {
      throw new IllegalStateException("Second dynamic extent");
    }

    // Create new monitor frame.
    temp     = new MonitorFrame(o);
    tempFlag = TEMP_FLAG_INSTALL_MONITOR;
  }

  /**
   * Set up the specified procedure as an exception handler for
   * throwables of the specified type. This method marks a new dynamic
   * extent, during which the specified procedure serves as an
   * exception handler for throwables of the specified type. The
   * exception handler is automatically installed when control enters
   * the dynamic extent, and it is automatically de-installed when
   * control leaves the dynamic extent.
   *
   * <p>If a Java throwable is thrown in the new dynamic extent, and
   * if that throwable matches the specified type, the specified
   * handler is applied on the actual throwable in the state valid
   * before the call to this method.</p>
   *
   * <p>An actual throwable matches the specified type as follows: If
   * the throwable is not an instance of a wrapped exception, it
   * matches the specified type if it is an instance of that type. If
   * the throwable is an instance of a wrapped exception, it matches
   * the specified type if it is an instance of that type, or if the
   * wrapped throwable is an instance of that type. The specified
   * handler is always applied on the <em>matching</em> throwable,
   * which is the wrapped throwable if both the wrapped exception and
   * the wrapped throwable match the specified type.</p>
   *
   * <p>The behavior of this method is unspecified if it is not
   * invoked as an up-call from within an applicable entity's
   * <code>apply()</code> method.</p>
   *
   * <p>If the current continuation is captured through a call to
   * <code>getCurrentContinuation()</code> after calling this method
   * but before returning form the same invocation of an applicable
   * entity's <code>apply()</code> method, the returned continuation
   * does <em>not</em> reflect the dynamic extent set up by this
   * method.</p>
   *
   * @see     WrappedException
   *
   * @see     #setUpMonitor(Object)
   * @see     #setUpBeforeAfter(Object,Object)
   *
   * @see     #getCurrentContinuation()
   *
   * @param   handler  The handler to apply on the actual throwable.
   * @param   type     The type of throwables to catch.
   * @throws  NullPointerException
   *                   Signals that <code>handler</code> or
   *                   <code>type</code> is <code>null</code>.
   * @throws  IllegalArgumentException
   *                   Signals that <code>type</code> is not a
   *                   subtype of <code>java.lang.Throwable</code>.
   * @throws  IllegalStateException
   *                   Signals that the current invocation of an
   *                   applicable entity's <code>apply()</code> method
   *                   has already created a new dynamic extent.
   */
  public void setUpExceptionHandler(Applicable handler, Class type) {
    // Verify arguments.
    if (null == handler) {
      throw new NullPointerException("Null exception handler");
    } else if (null == type) {
      throw new NullPointerException("Null exception type");
    } else if (! java.lang.Throwable.class.isAssignableFrom(type)) {
      throw new IllegalArgumentException(type.toString() +
                                    " not a subclass of java.lang.Throwable");
    }

    // Verify state.
    if (null != temp) {
      throw new IllegalStateException("Second dynamic extent");
    }

    // Create new exception frame.
    temp     = new ExceptionFrame(handler, type);
    tempFlag = TEMP_FLAG_INSTALL_EXTENT;
  }

  /**
   * Set up the specified expressions as before and after
   * expressions. This method marks a new dynamic
   * extent. <code>Before</code> is evaluated whenever control enters
   * the newly created dynamic extent, and <code>after</code> is
   * evaluated whenever control leaves that dynamic extent. Because of
   * the structure of <i>eval</i>, the before expression must be
   * evaluated explicitly before calling this method (for example, by
   * evaluating a sequence of appropriate expressions).
   *
   * <p>If <code>before</code> is <code>null</code> no expression is
   * evaluated when control enters the dynamic extent. If
   * <code>after</code> is <code>null</code> no expression is
   * evaluated when control leaves the dynamic extent. In other words,
   * a <code>null</code> expression is treated as <code>null</code>
   * and not as the empty list.</p>
   *
   * <p>The behavior of this method is unspecified if it is not
   * invoked as an up-call from within an applicable entity's
   * <code>apply()</code> method.</p>
   *
   * <p>If the current continuation is captured through a call to
   * <code>getCurrentContinuation()</code> after calling this method
   * but before returning form the same invocation of an applicable
   * entity's <code>apply()</code> method, the returned continuation
   * does <em>not</em> reflect the dynamic extent set up by this
   * method.</p>
   *
   * @see     #setUpMonitor(Object)
   * @see     #setUpExceptionHandler(Applicable,Class)
   *
   * @see     #getCurrentContinuation()
   *
   * @param   before  The before expression, or <code>null</code>
   *                  if the newly created dynamic extent has no
   *                  before expression.
   * @param   after   The after expression, or <code>null</code>
   *                  if the newly created dynamic extent has no
   *                  after expression.
   * @throws  NullPointerException
   *                  Signals that both <code>before</code> and
   *                  <code>after</code> are <code>null</code>.
   * @throws  IllegalStateException
   *                   Signals that the current invocation of an
   *                   applicable entity's <code>apply()</code> method
   *                   has already created a new dynamic extent.
   */
  public void setUpBeforeAfter(Object before, Object after) {
    // Verify arguments.
    if ((null == before) && (null == after)) {
      throw new NullPointerException("Null before/after expressions");
    }

    // Verify state.
    if (null != temp) {
      throw new IllegalStateException("Second dynamic extent");
    }

    // Create new exception frame.
    temp     = new BeforeAfterFrame(before, after);
    tempFlag = TEMP_FLAG_INSTALL_EXTENT;
  }

  /**
   * Return the current continuation as an applicable entity.
   *
   * <p>The behavior of this method is unspecified if it is not
   * invoked as an up-call from within an applicable entity's
   * <code>apply()</code> method.</p>
   *
   * @return  The current continuation.
   */
  public Applicable getCurrentContinuation() {
    return new Continuation(aframe.capture(), eframe);
  }

  /**
   * Evaluate the specified expression. This method evaluates the
   * specified expression as a top-level expression in the top-level
   * environment of this evaluator. The activation frames and result
   * from the previous evaluation are reset before evaluating the
   * specified expression, but not the stacks of readers and writers.
   *
   * <p>This method is typically called to evaluate an expression
   * without spawning a separate thread.</p>
   *
   * <p>Note that this method throws an illegal state exception if it
   * is invoked as an up-call from within an applicable entity's
   * <code>apply()</code> method.</p>
   *
   * @param    expr  The expression to evaluate.
   * @return         The result of evaluating the specified
   *                 expression.
   * @throws   EvaluatorException
   *                 Signals some exceptional condition when
   *                 evaluating the specified expression.
   * @throws   IllegalStateException
   *                 Signals that this evaluator is running, that
   *                 is, currently evaluating an expression.
   */
  public Object eval(Object expr) throws EvaluatorException {
    // Set up the expression.
    setUpExpression(expr);

    // Run the evaluator.
    run();

    // Return the result.
    if (! successful) {
      EvaluatorException.signal((Throwable)result);
    }
    return result;
  }
  
  /**
   * Create a new evaluator based on the state of this evaluator.
   *
   * <p>The newly created evaluator shares the top-level environment,
   * top-level reader, top-level writer, the list(s) of marked I/O
   * streams, as well as the language format with this
   * evaluator. Operations that affect the state of these entities are
   * visible to both evaluators.</p>
   *
   * <p>The newly created evaluator does not share any readers or
   * writers that are not top-level. Note that this implies that the
   * stacks of readers and writers themselves are not shared between
   * this evaluator and the newly created evaluator.</p>
   *
   * <p>The newly created evaluator does not share any activation
   * frames or results with this evaluator.</p>
   *
   * <p>This method is typically called to create a new evaluator
   * ready for evaluating an expression in its own thread. After
   * spawning a new evaluator, the caller sets up an expression in the
   * spawned evaluator using <code>setUpExpression()</code> or
   * <code>setUpApplication()</code> and then evaluates that
   * expression using <code>run()</code>.</p>
   *
   * @see     #setUpExpression(Object)
   * @see     #setUpApplication(Applicable,Pair)
   * @see     #run()
   *
   * @return  The newly created evaluator.
   */
  public Evaluator spawn() {
    Evaluator e2           = new Evaluator();
    
    e2.markedList          = markedList;
    e2.format              = format;
    e2.readers             = new ArrayList();
    e2.readers.add(readers.get(0));
    e2.writers             = new ArrayList();
    e2.writers.add(writers.get(0));
    e2.topLevelEnvironment = topLevelEnvironment;
    /*
     * e2.aframe is automatically initialized to null.
     * e2.eframe is automatically initialized to null.
     * e2.temp is automatically initialized to null.
     * e2.tempFlag is automatically initialized to zero.
     * e2.running is automatically initialized to false.
     * e2.resultReady is automatically initialized to false.
     * e2.successful doesn't matter.
     * e2.result doesn't matter.
     */

    return e2;
  }

  /**
   * Set up the specified expression for evaluation. This method sets
   * up the specified expression for evaluation. It resets the
   * activation frames and result from the previous evaluation, but not
   * the stacks of readers and writers.
   *
   * <p>This method is typically called on a freshly spawned evaluator
   * to set up an expression for evaluation. The expression is then
   * evaluated by a call to <code>run()</code>.</p>
   *
   * <p>Note that this method throws an illegal state exception if it
   * is invoked as an up-call from within an applicable entity's
   * <code>apply()</code> method.</p>
   *
   * @see     #spawn()
   * @see     #run()
   *
   * @param   expr  The expression to evaluate.
   * @throws  IllegalStateException
   *                Signals that this evaluator is running, that
   *                is, currently evaluating an expression.
   */
  public void setUpExpression(Object expr) {
    if (running) {
      throw new IllegalStateException("Evaluator is running");
    }

    // Clear previous result (successful doesn't matter).
    resultReady = false;
    result      = null;

    // Set up expression.
    aframe      = new ActivationFrame(topLevelEnvironment, expr);
    eframe      = null;
  }

  /**
   * Set up the specified procedure for application on the specified
   * arguments. This method sets up the specified procedure for
   * application on the specified arguments. It resets the activation
   * frames and result from the previous evaluation, but not the
   * stacks of readers and writers.
   *
   * <p>This method is typically called on a freshly spawned
   * evaluator. The application is then evaluated by a call to
   * <code>run()</code>.</p>
   *
   * <p>Note that this method throws an illegal state exception if it
   * is invoked as an up-call from within an applicable entity's
   * <code>apply()</code> method.</p>
   *
   * @see     #spawn()
   * @see     #run()
   *
   * @param   proc  The procedure to apply.
   * @param   args  The arguments to apply the procedure on.
   * @throws  IllegalStateException
   *                Signals that this evaluator is running, that
   *                is, currently evaluating an expression.
   * @throws  NullPointerException
   *                Signals that <code>proc</code> is
   *                <code>null</code>.
   * @throws  IllegalArgumentException
   *                Signals that <code>args</code> is not a list.
   */
  public void setUpApplication(Applicable proc, Pair args) {
    if (running) {
      throw new IllegalStateException("Evaluator is running");
    } else if (null == proc) {
      throw new NullPointerException("Null procedure");
    } else if ((Pair.EMPTY_LIST != args) &&
               (! args.isList())) {
      throw new IllegalArgumentException("Arguments not a list");
    }

    // Clear previous result (successful doesn't matter).
    resultReady = false;
    result      = null;

    // Set up expression.
    aframe          = new ActivationFrame(topLevelEnvironment, proc, args);
    eframe          = null;
  }

  /**
   * Run this evaluator. This method runs the evaluator, starting with
   * the current activation frame.
   *
   * <p>On return of this method, the evaluation has terminated,
   * <code>isRunning()</code> returns <code>false</code>, and
   * <code>isResultReady()</code> returns <code>true</code>. The
   * result of the evaluation, which either is a value representing
   * the result of a successful evaluation or a throwable representing
   * an exceptional condition, can be accessed by calling
   * <code>getResult()</code>. Successful and exceptional termination
   * can be distinguished by calling <code>isResultValue()</code>.</p>
   *
   * <p>This method is typically called to evaluate an expression
   * which has been set up (using <code>setUpExpression()</code> or
   * <code>setUpApplication()</code>) in a freshly spawned
   * evaluator.</p>

   * <p>Note that this method throws an illegal state exception if it
   * is invoked as an up-call from within an applicable entity's
   * <code>apply()</code> method.</p>
   *
   * @see     #spawn()
   * @see     #setUpExpression(Object)
   * @see     #setUpApplication(Applicable,Pair)
   * @see     #isRunning()
   * @see     #isResultReady()
   * @see     #isResultValue()
   * @see     #getResult()
   *
   * @throws  IllegalStateException
   *             Signals that either this evaluator is running,
   *             or that no expression has been set up for
   *             evaluation.
   */
  public void run() {
    // Consistency checks.
    if (running) {
      throw new IllegalStateException("Evaluator is running");
    } else if (null == aframe) {
      throw new IllegalStateException("Nothing to evaluate");
    }

    // We are running, people...
    running = true;

  LOOP:
    while (true) {
      
      try {
        
      INNER_LOOP:
        do {
          /*
           * Reference to temporary values. When entering the second
           * switch statement, this field holds the value to be placed
           * into an activation frame.
           */
          Object o;

          /*
           * Process the current activation frame.
           *
           * The current activation frame must not be captured. This
           * invariant must be enforced by the second switch statement
           * (following this switch) which places the result of an
           * evaluation into the next expecting activation frame.
           */
          switch (aframe.opcode) {
            
          case OPCODE_EVAL_EXPR:
            if (aframe.expr instanceof Pair) {
              // This is an application.
              Pair p = (Pair)aframe.expr;
              if (! p.isList()) {
                throw new BadSyntaxException(
                  "Application form not a proper list", p);
              }

              o             = p.car;
              aframe.opcode = OPCODE_EVAL_OPERATOR;
              if (o instanceof Pair) {
                // Recursively evaluate the operator.
                aframe = new ActivationFrame(aframe, o);
                continue INNER_LOOP;

              } else {
                /*
                 * Simple expressions can either be a self-evaluating
                 * object or a symbol to look up in the current
                 * environment. Here, and only here, we allow
                 * syntactic applicable entities, because this is the
                 * operator position.
                 */
                if (o instanceof Symbol) {
                  o = aframe.env.lookup((Symbol)o);

                  if (o instanceof NotAValue) {
                    throw new NotAValueException(o);
                  }
                }
                if (! (o instanceof Applicable)) {
                  throw new BadArgumentException(
                    "Operator not an applicable entity", o);
                }
                aframe.operator = (Applicable)o;
                // Fall through.
              }

            } else {
              // Simple expressions are evaluated in place.
              o = evalSimpleExpression1(aframe.expr);
              
              aframe.opcode = OPCODE_RETURN_VALUE;
              break; // Place o into next expecting activation frame.
            }
            
          case OPCODE_EVAL_OPERATOR:
            if (aframe.operator.isSyntactic()) {
              // Apply a syntactic applicable entity right away.
              // FIXME what about clearing operator ???
              aframe.opcode = OPCODE_EVAL_EXPR;
              o = aframe.operator.apply((Pair)((Pair)aframe.expr).cdr, this);
              break; // Place o into next expecting activation frame.
              
            } else {
              // Remove syntax from operator position and go to eval operands.
              aframe.expr   = ((Pair)aframe.expr).cdr;
              aframe.opcode = OPCODE_EVAL_OPERANDS;
              // Fall through.
            }
            
          case OPCODE_EVAL_OPERANDS:
            if (Pair.EMPTY_LIST != aframe.expr) {
              // Next operand.
              Pair p      = (Pair)aframe.expr;
              o           = p.car;
              aframe.expr = p.cdr;
              
              if (o instanceof Pair) {
                // Recursively evaluate the operand.
                aframe = new ActivationFrame(aframe, o);
                continue INNER_LOOP;

              } else {
                // Evaluate simple expression in place.
                o = evalSimpleExpression1(o);
                p = new Pair(o, Pair.EMPTY_LIST);
                if (Pair.EMPTY_LIST == aframe.head) {
                  aframe.head = p;
                  aframe.tail = p;
                } else {
                  aframe.tail.cdr = p;
                  aframe.tail     = p;
                }
                continue INNER_LOOP;
              }
            }
            aframe.opcode = OPCODE_APPLY;
            // Fall through.
            
          case OPCODE_APPLY:
            // Done evaluating the operands, fire off apply
            aframe.opcode = OPCODE_RETURN_VALUE;
            o = aframe.operator.apply(aframe.head, this);
            break; // Place o into next expecting activation frame.
            
          case OPCODE_EVAL_IN_SEQUENCE:
            Pair p = (Pair)aframe.expr;
            Pair q = (Pair)p.cdr;
            
            if (Pair.EMPTY_LIST == q) {
              // Last expression is evaluated tail recursively.
              aframe.expr   = p.car;
              aframe.opcode = OPCODE_EVAL_EXPR;
              continue INNER_LOOP;
              
            } else {
              aframe.expr = q;
              
              o = p.car;
              if (o instanceof Pair) {
                boolean level   = aframe.topLevel; // Propagate top level.
                aframe          = new ActivationFrame(aframe, o);
                aframe.topLevel = level;
                continue INNER_LOOP;

              } else {
                // Throw away result.
                evalSimpleExpression1(o);
                continue INNER_LOOP;
              }
            }

          case OPCODE_EVAL_SEQUENCE:
          case OPCODE_EXPECT_VALUES:
          case OPCODE_RETURN_VALUES:
          case OPCODE_RETURN_VALUE:
            throw new Bug("Unreachable evaluator opcode " + aframe.opcode);
          default:
            throw new Bug("Illegal evaluator opcode " + aframe.opcode);
          }

          // Verify that o is a value.
          if ((o instanceof NotAValue) ||
              ((o instanceof Applicable) && ((Applicable)o).isSyntactic())) {
            throw new NotAValueException(o);
          }

          // Install dynamic extent, or acquire/release monitor.
          if (null != temp) {
            switch (tempFlag) {

            case TEMP_FLAG_INSTALL_MONITOR:
              Monitor.acquire(((MonitorFrame)temp).o);
              // Fall through.

            case TEMP_FLAG_INSTALL_EXTENT:
              ExtentFrame f = (ExtentFrame)temp;

              aframe        = aframe.setUpExtent(f);
              f.next        = eframe;
              eframe        = f;
              break;

            case TEMP_FLAG_ACQUIRE_MONITOR:
              Monitor.acquire(temp);
              break;

            case TEMP_FLAG_RELEASE_MONITOR:
              Monitor.release(temp);
              break;

            default:
              throw new Bug("Illegal temporary flag " + tempFlag);
            }

            temp = null;
          }

          /*
           * Starting from the current activation frame, place
           * <code>o</code> within the next activation frame that is
           * expecting a result and set <code>aframe</code> to that
           * activation frame.
           *
           * <p>If no activation frame in the chain of activation
           * frames is expecting a result, the overall expression
           * currently being evaluated is fully evaluated and the
           * evaluation terminates. In that case, the
           * <code>result</code> field of this evaluator is set to
           * <code>o</code> and <code>run()</code> returns.</p>
           *
           * <p>Note that <code>o</code> may represent a value, a
           * sequence of values, an expression, or a sequence of
           * expressions. Further note that the activation frame in
           * <code>aframe</code> after executing the switch statement
           * may be the same as the current one (for tail recursive
           * invocations).</p>
           *
           * <p>Before continuing, the activation frame is copied if
           * it is captured. This ensures that the first switch
           * statement always works on activation frames that are not
           * captured.</p>
           *
           * <p><code>o</code> is treated as a sequence of values if
           * any visited activation frame has
           * <code>OPCODE_RETURN_VALUES</code> as its opcode. Before
           * marking an activation frame as returning a sequence of
           * values, the marker must verify that the continuation
           * represented by the chain of activation frames starting at
           * the current activation frame does, in fact, accept
           * multiple values.</p>
           */

          boolean         values = false;  // Flag: o is a sequence of values.

          /*
           * Walk the chain of frames until we find an activation
           * frame to place o into.
           */
          while (null != aframe) {
            // Make sure that the returned activation frame is not captured!
            if (aframe.captured &&
                (OPCODE_RETURN_VALUES != aframe.opcode) &&
                (OPCODE_RETURN_VALUE  != aframe.opcode)) {
              aframe = aframe.copy();
            }
              
            switch (aframe.opcode) {
                
            case OPCODE_EVAL_EXPR:
              // Evaluate o in the current activation frame.
              aframe.expr = o;
              continue INNER_LOOP;
                
            case OPCODE_EVAL_OPERATOR:
              // Operator is finished.
              if (! (o instanceof Applicable)) {
                throw new BadArgumentException(
                  "Operator not an applicable entity", o);
              }
              aframe.operator = (Applicable)o;
              continue INNER_LOOP;
              
            case OPCODE_EVAL_OPERANDS:
              // One of the operands is finished.
              Pair p = Pair.cons(o, Pair.EMPTY_LIST);
              
              if (Pair.EMPTY_LIST == aframe.head) {
                aframe.head = p;
                aframe.tail = p;
              } else {
                aframe.tail.cdr = p;
                aframe.tail     = p;
              }
              
              continue INNER_LOOP;
              
            case OPCODE_APPLY:
              // Apply some procedure on some arguments, loose o.
              continue INNER_LOOP;
              
            case OPCODE_EVAL_SEQUENCE:
              if (Pair.EMPTY_LIST == o) {
                throw new BadSyntaxException(
                  "Unable to evaluate empty sequence of expressions", o);
              } else if (! (o instanceof Pair)) {
                throw new BadSyntaxException(
                  "Sequence of expressions not a pair", o);
              } else if (! ((Pair)o).isList()) {
                throw new BadSyntaxException(
                  "Sequence of expressions not a proper list", o);
              }
              aframe.expr   = o;
              aframe.opcode = OPCODE_EVAL_IN_SEQUENCE;
              continue INNER_LOOP;
              
            case OPCODE_EVAL_IN_SEQUENCE:
              // One of the sequence expressions is done, loose o.
              continue INNER_LOOP;
              
            case OPCODE_EXPECT_VALUES:
              // o may be a sequence of values or a single value.
              if (values) {
                aframe.head = (Pair)o;
              } else {
                aframe.head = Pair.cons(o, Pair.EMPTY_LIST);
              }
              
              aframe.opcode = OPCODE_APPLY;
              continue INNER_LOOP;
              
            case OPCODE_RETURN_VALUES:
              // Make sure o is the empty list or a proper list.

              // FIXME: we should also make sure that none of the cars
              // contains a non-value!!!

              if (Pair.EMPTY_LIST != o) {
                if (! (o instanceof Pair)) {
                  throw new BadSyntaxException(
                    "Sequence of values not a pair", o);
                } else if (! ((Pair)o).isList()) {
                  throw new BadSyntaxException(
                    "Sequence of values not a proper list", o);
                }
              }
              
              // We got us a sequence of values.
              values = true;
              
              // Continue with parent.
              aframe = aframe.parent;
              continue;
              
            case OPCODE_RETURN_VALUE:
              /*
               * Before leaving this activation frame, process the
               * extent frame if present and necessary. In particular,
               * release a held monitor and evaluate a pending after
               * expression. Note that for an activation frame to be
               * associated with a dynamic extent, its opcode must be
               * <code>OPCODE_RETURN_VALUE</code>.
               */
              if (aframe instanceof ExtentFrame) {
                // aframe == eframe must be true.

                eframe = eframe.next; // Fix eframe.
                
                if (aframe instanceof MonitorFrame) {
                  // We found a monitor.
                  Monitor.release(((MonitorFrame)aframe).o);
                  
                } else if (aframe instanceof BeforeAfterFrame) {
                  BeforeAfterFrame baf = (BeforeAfterFrame)aframe;
                  
                  if (null != baf.after) {
                    // We found an after expression.
                    ActivationFrame   af   = aframe.copy();
                    AfterContinuation cont = new AfterContinuation(o, values);
                    
                    // Directly apply primitive after continuation.
                    af.operator = cont;
                    af.opcode   = OPCODE_APPLY;
                    
                    // Simply evaluate after expression and then apply cont.
                    aframe = new ActivationFrame(af, baf.after);
                    continue INNER_LOOP;
                  }
                }
              }
              
              // Continue with parent.
              aframe = aframe.parent;
              continue;
              
            default:
              throw new Bug("Illegal evaluator opcode " + aframe.opcode);
            }
          }
            
          // We are done evaluating.
          result      = o;
          successful  = true;
          resultReady = true;
          running     = false;  // Set last to be thread-safe.
          return;

        } while (true);

      } catch (Throwable x) {
        /*
         * Walk the chain of extent frames to the end or until a
         * matching exception handler is found. On the way, release
         * monitors and evaluate after expressions.
         */
        ExtentFrame f  = eframe;
        Throwable   xx = ((x instanceof WrappedException) ?
                          ((WrappedException)x).getThrowable() : null);

        while (null != f) {
          if (f instanceof MonitorFrame) {
            // We found a monitor.
            Monitor.release(((MonitorFrame)f).o);

          } else if (f instanceof ExceptionFrame) {
            ExceptionFrame ef = (ExceptionFrame)f;

            if (ef.type.isInstance(x) || ef.type.isInstance(xx)) {
              // We found a matching exception handler.

              ActivationFrame af = ef.copy();

              // Directly apply handler onto exception.
              af.operator = ef.handler;
              if (ef.type.isInstance(xx)) {
                af.head   = Pair.cons(xx, Pair.EMPTY_LIST);
              } else {
                af.head   = Pair.cons(x,  Pair.EMPTY_LIST);
              }
              af.opcode   = OPCODE_APPLY;

              aframe      = af;
              eframe      = ef.next;

              continue LOOP;
            }

          } else {
            BeforeAfterFrame baf = (BeforeAfterFrame)f;

            if (null != baf.after) {
              // We found an after expression.

              ActivationFrame   af   = baf.copy();
              AfterContinuation cont = new AfterContinuation(x);

              // Directly apply primitive after continuation.
              af.operator = cont;
              af.opcode   = OPCODE_APPLY;

              // Evaluate after expression and then apply cont.
              aframe      = new ActivationFrame(af, baf.after);
              eframe      = baf.next;

              continue LOOP;
            }
          }

          f = f.next;
        } // End while loop for exception handling.

        // Set up exceptional result and return.
        result      = x;
        successful  = false;
        resultReady = true;
        running     = false;
        break LOOP;

      } // End of catch block.
      
    } // End of while loop LOOP.
    
    // Done.
  }

  /**
   * Determine whether the evaluator is running, that is, currently
   * evaluating an expression. An evaluator is running iff control is
   * inside its <code>run()</code> method. This implies that this
   * method, when called on the evaluator passed to an applicable
   * entity's <code>apply()</code> method, always returns
   * <code>true</code>.
   *
   * @see     #run()
   *
   * @return  <code>true</code> iff the evaluator is running.
   */
  public boolean isRunning() {
    return running;
  }

  /**
   * Determine whether the result is ready. This method returns
   * <code>true</code> if the previous evaluation has terminated and a
   * result is ready. Unlike <code>isRunning()</code>, it does not
   * return <code>true</code> if this evaluator has not yet evaluated
   * an expression.
   *
   * <p>The result can be accessed using <code>getResult()</code>, and
   * successful and exceptional termination of the previous evaluation
   * can be distinguished using <code>isResultValue()</code>.</p>
   *
   * @see     #isRunning()
   * @see     #isResultValue()
   * @see     #getResult()
   *
   * @return  <code>true</code> iff a result is ready.
   */
  public boolean isResultReady() {
    return resultReady;
  }

  /**
   * Determine whether the result of the previous evaluation is a
   * value. If the previous evaluation has terminated successfully,
   * this method returns <code>true</code> and
   * <code>getResult()</code> returns a value. If the previous
   * evaluation has terminated exceptionally, this method returns
   * <code>false</code> and <code>getResult()</code> returns an
   * instance of <code>java.lang.Throwable</code> indicating the
   * exceptional condition.
   *
   * <p>Note that this method throws an illegal state exception if it
   * is invoked as an up-call from within an applicable entity's
   * <code>apply()</code> method.</p>
   *
   * @see     #run()
   * @see     #isRunning()
   * @see     #getResult()
   *
   * @return     <code>true</code> iff the result of the previous
   *             evaluation is a value.
   * @throws  IllegalStateException
   *             Signals that no result is ready, either because
   *             the evaluator is currently running or because
   *             the result from the previous evaluation has been
   *             reset.
   */
  public boolean isResultValue() {
    if (resultReady) {
      return successful;
    } else {
      throw new IllegalStateException("No result ready");
    }
  }

  /**
   * Return the result from the previous evaluation. This method
   * returns a value if the previous evaluation has terminated
   * successfully, and it returns an instance of
   * <code>java.lang.Throwable</code> if the previous evaluation has
   * terminated exceptionally. Successful and exceptional termination
   * can be distinguished by calling <code>isResultValue()</code>.
   *
   * <p>Note that this method throws an illegal state exception if it
   * is invoked as an up-call from within an applicable entity's
   * <code>apply()</code> method.</p>
   *
   * @see     #run()
   * @see     #isResultReady()
   * @see     #isResultValue()
   *
   * @return     The result of the previous evaluation. 
   * @throws  IllegalStateException
   *             Signals that no result is ready, either because
   *             the evaluator is currently running or because
   *             the result from the previous evaluation has been
   *             reset.
   */
  public Object getResult() {
    if (resultReady) {
      return result;
    } else {
      throw new IllegalStateException("No result ready");
    }
  }

  /**
   * Reset the stacks of readers and writers. All readers and writers,
   * besides the top-level reader and writer, that are on the stacks
   * of readers and writers are removed from the respective stack and
   * marked for clean-up.
   *
   * <p>This method is typically called between evaluations to
   * restore I/O to a well-defined state.</p>
   */
  public void resetCurrentReaderWriter() {
    int size;

    // Reset stack of readers.
    while ((size = readers.size()) > 1) {
      Reader in = (Reader)readers.remove(size - 1);
      markForCleanup(in);
    }

    // Reset stack of writers.
    while ((size = writers.size()) > 1) {
      Writer out = (Writer)writers.remove(size - 1);
      markForCleanup(out);
    }
  }

  /**
   * Reset the state from the previous evaluation. This method resets
   * the activation frames and result from the previous
   * evaluation. Subsequent calls to <code>getResult()</code> fail
   * with an illegal state exception, and subsequent calls to
   * <code>printBackTrace()</code> will not print anything.
   *
   * <p>Note that this method throws an illegal state exception if it
   * is invoked as an up-call from within an applicable entity's
   * <code>apply()</code> method.</p>
   *
   * @see     #getResult()
   * @see     #printBackTrace(Writer)
   *
   * @throws  IllegalStateException
   *             Signals that this evaluator is running, that
   *             is, currently evaluating an expression.
   */
  public void resetEvaluationState() {
    if (running) {
      throw new IllegalStateException("Evaluator is running");
    }

    aframe      = null;
    eframe      = null;
    temp        = null;
    resultReady = false;
    result      = null;
  }

  /**
   * Clean up marked I/O streams. This method closes all input
   * streams, output streams, readers, and writers that are marked for
   * clean-up and unmarks them. It <em>must</em> be called before
   * exiting the system.
   *
   * <p>Any exceptions that occur during clean-up are caught, ignored,
   * and clean up continues.</p>
   */
  public void cleanUpMarkedStreams() {
    // Clean up.
    int size;
    while ((size = markedList.size()) > 0) {
      Object o = markedList.remove(size - 1);
      try {
        if (o instanceof InputStream) {
          ((InputStream)o).close();
        } else if (o instanceof OutputStream) {
          ((OutputStream)o).close();
        } else if (o instanceof Reader) {
          ((Reader)o).close();
        } else if (o instanceof Writer) {
          ((Writer)o).close();
        }
      } catch (IOException x) {
        // Ignore.
      }
    }
  }

  /**
   * Print a backtrace of activation frames to the specified writer,
   * using this evaluator's language format.
   *
   * @param   out  The writer to write the backtrace to.
   * @throws  WrappedException
   *               Signals an exceptional condition when writing
   *               to the specified writer.
   */
  public void printBackTrace(Writer out) throws WrappedException {
    if (null == aframe) {
      return;
    }

    String comment = format.getCommentStart();
    String eol     = Format.getLineSeparator();

    try {
      ActivationFrame f = aframe;

      while (null != f) {
        out.write(comment);
        switch (f.opcode) {
        case OPCODE_EVAL_EXPR:
          out.write(" OPCODE_EVAL_EXPR");
          break;
        case OPCODE_EVAL_OPERATOR:
          out.write(" OPCODE_EVAL_OPERATOR");
          break;
        case OPCODE_EVAL_OPERANDS:
          out.write(" OPCODE_EVAL_OPERANDS");
          break;
        case OPCODE_APPLY:
          out.write(" OPCODE_APPLY");
          break;
        case OPCODE_EXPECT_VALUES:
          out.write(" OPCODE_EXPECT_VALUES");
          break;
        case OPCODE_EVAL_SEQUENCE:
          out.write(" OPCODE_EVAL_SEQUENCE");
          break;
        case OPCODE_EVAL_IN_SEQUENCE:
          out.write(" OPCODE_EVAL_IN_SEQUENCE");
          break;
        case OPCODE_RETURN_VALUE:
          out.write(" OPCODE_RETURN_VALUE");
          break;
        case OPCODE_RETURN_VALUES:
          out.write(" OPCODE_RETURN_VALUES");
          break;
        default:
          out.write(" illegal opcode " + f.opcode);
          break;
        }
        out.write(eol);
        
        out.write(comment);
        out.write("   expression  : ");
        format.write(f.expr, out, true);
        out.write(eol);
        
        out.write(comment);
        out.write("   operator    : ");
        format.write(f.operator, out, true);
        out.write(eol);
        
        out.write(comment);
        out.write("   eval-rands  : ");
        format.write(f.head, out, true);
        out.write(eol);
        
        out.write(comment);
        out.write("   environment : ");
        format.write(f.env, out, true);
        out.write(eol);
        
        if (f instanceof MonitorFrame) {
          MonitorFrame mf = (MonitorFrame)f;
          
          out.write(comment);
          out.write("   monitor     : ");
          format.write(mf.o, out, true);
          out.write(eol);
            
        } else if (f instanceof ExceptionFrame) {
          ExceptionFrame ef = (ExceptionFrame)f;

          out.write(comment);
          out.write("   x handler   : ");
          format.write(ef.handler, out, true);
          out.write(eol);
          out.write(comment);
          
          out.write("   x type      : ");
          format.write(ef.type, out, true);
          out.write(eol);
          
        } else if (f instanceof BeforeAfterFrame) {
          BeforeAfterFrame baf = (BeforeAfterFrame)f;
          
          if (null != baf.before) {
            out.write(comment);
            out.write("   before      : ");
            format.write(baf.before, out, true);
            out.write(eol);
          }
          
          if (null != baf.after) {
            out.write(comment);
            out.write("   after       : ");
            format.write(baf.after, out, true);
            out.write(eol);
          }
        }
      
        if (f.topLevel) {
          out.write(comment);
          out.write("   top-level");
          out.write(eol);
        }
        
        if (f.captured) {
          out.write(comment);
          out.write("   captured");
          out.write(eol);
        }
        
        f = f.parent;
      }

      out.flush();

    } catch (IOException x) {
      throw new WrappedException("Unable to write to output port", x, out);
    }
  }

  /**
   * Print the specified message to the top-level writer. Writes the
   * specified message followed by a line separator to the top-level
   * writer and flushes the top-level writer. Any exceptions are
   * caught and ignored.
   *
   * @param  msg  The debug message
   */
  public void debug(String msg) {
    Writer out = getTopLevelWriter();

    try {
      out.write(msg);
      out.write(Format.getLineSeparator());
      out.flush();
    } catch (IOException x) {
      // Ignore.
    }
  }
}
