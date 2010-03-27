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

import java.io.ObjectOutputStream;
import java.io.IOException;

import one.util.ThreadManager;

/**
 * Implementation of a promise. A promise holds an expression for
 * future evaluation. If the promise is forced for the first time, the
 * expression is evaluated and the promise memoizes the resulting
 * value for future reference. As a generalization of the concept of a
 * promise, <i>eval</i> also uses promises to support multi-threading.
 * 
 * <p>A promise can be in one of four states. The expression state
 * represents an unforced promise, and a promise in this state
 * references an unevaluated expression together with the
 * corresponding environment for future evaluation. The thread state
 * represents an unforced promise whose expression is concurrently
 * evaluated in a separate thread. A promise in this state references
 * the evaluator used to evaluate the expression. The value state
 * represents a forced promise, and a promise in this state references
 * the value resulting from evaluating the promise's expression. The
 * throwable state represents a forced promise, and a promise in this
 * state references some <code>java.lang.Throwable</code> indicating
 * an exceptional condition resulting from evaluating the promise's
 * expression.</p>
 *
 * <p>Promises generally are serializable. However, the expression,
 * environment, or value referenced by a promise may not be
 * serializable or may in turn reference an unserializable object. A
 * particular instance of this class may thus not be
 * serializable. Furthermore, promises in the thread state reference
 * an evaluator and are never serializable.</p>
 *
 * <p>By default, newly created promises that evaluate an expression
 * in a separate thread simply create a new thread animating the
 * corresponding evaluator. However, by defining a thread manager,
 * more complicated thread management policies can be used.</p>
 *
 * @see      #setThreadManager(ThreadManager)
 *
 * @author   &copy; Copyright 1998-2000 by Robert Grimm.
 *           Written by <a href="mailto:rgrimm@alum.mit.edu">Robert
 *           Grimm</a>.
 * @version  1.0
 */
public final class Promise implements java.io.Serializable {

  // ============================= Constants ============================

  /**
   * Flag for expression state. The promise references an unevaluated
   * expression and the corresponding environment.
   */
  public static final int STATE_EXPRESSION  = 1;

  /**
   * Flag for thread state. The promise references an
   * evaluator. Promises in this state are not serializable.
   */
  public static final int STATE_THREAD      = 2;

  /**
   * Flag for value state. The promise references a proper value.
   */
  public static final int STATE_VALUE       = 3;

  /**
   * Flag for throwable state. The promise references an instance of
   * <code>java.lang.Throwable</code>.
   */
  public static final int STATE_THROWABLE   = 4;

  // ========================== Internal State ==========================

  /**
   * The state of this promise.
   *
   * @serial  Must be <code>STATE_EXPRESSION</code>,
   *          <code>STATE_THREAD</code>, <code>STATE_VALUE</code>, or
   *          <code>STATE_THROWABLE</code>. Note that, if
   *          <code>state</code> is <code>STATE_THREAD</code>, this
   *          promise is not serializable.
   */
  private int    state;

  /**
   * The first reference for this promise.
   *
   * @serial  The value for <code>one</code> depends on the state of
   *          this promise:<ul>
   *          <li>If <code>state</code> is <code>STATE_EXPRESSION</code>,
   *          <code>one</code> must reference an unevaluated expression.</li>
   *          <li>If <code>state</code> is <code>STATE_THREAD</code>,
   *          <code>one</code> must reference an instance of
   *          <code>one.eval.Evaluator</code>.</li>
   *          <li>If <code>state</code> is <code>STATE_VALUE</code>,
   *          <code>one</code> must reference a proper value.</li>
   *          <li>If <code>state</code> is <code>STATE_THROWABLE</code>,
   *          <code>one</code> must reference an instance of
   *          <code>java.lang.Throwable</code>.</li>
   *          </ul>
   *          Note that this promise is not serializable if
   *          <code>state</code> is <code>STATE_THREAD</code>.
   */
  private Object one;

  /**
   * The second reference for this promise.
   *
   * @serial  The value for <code>two</code> depends on the state of this
   *          promise.
   *          <li>If <code>state</code> is <code>STATE_EXPRESSION</code>,
   *          <code>two</code> must reference an instance of
   *          <code>one.eval.Environment</code>.</li>
   *          <li>If <code>state</code> is <code>STATE_THREAD</code>,
   *          <code>two</code> must reference an instance of
   *          <code>java.lang.Thread</code>.</li>
   *          <li>Otherwise, <code>two</code> is <code>null</code>.</li>
   *          </ul>.
   *          Note that this promise is not serializable if
   *          <code>state</code> is <code>STATE_THREAD</code>.
   */
  private Object two;

  // =========================== Constructors ===========================

  /**
   * Create a new promise in the <code>STATE_EXPRESSION</code> state
   * with the specified expression and environment.
   *
   * @param   expr  The expression for the new promise.
   * @param   env   The environment for evaluating the specified
   *                expression.
   * @throws  NullPointerException
   *                Signals that <code>null == env</code>.
   */
  public Promise(Object expr, Environment env) {
    if (null == env) {
      throw new NullPointerException("Null environment");
    }

    one   = expr;
    two   = env;
    state = STATE_EXPRESSION;
  }

  /**
   * Create a new promise in the <code>STATE_THREAD</code> state
   * with the specified evaluator.
   *
   * @param   evaluator  The evaluator for the new promise.
   * @throws  NullPointerException
   *                     Signals that <code>null == eval</code>.
   */
  public Promise(Evaluator evaluator) {
    if (null == evaluator) {
      throw new NullPointerException("Null evaluator");
    }

    ThreadManager m = threadManager;

    one   = evaluator;
    if (null == m) {
      two = new Thread(evaluator);
      ((Thread)two).start();
    } else {
      two = m.run(evaluator);
    }
    state = STATE_THREAD;
  }

  // =========================== Serialization ==========================

  /**
   * Write the state of this promise to a stream, that is, serialize
   * it. Note that promises in the thread state are never serializable
   * and invoking this method on such a promise will always result in
   * a <code>NotSerializableException</code>. Though, for promises in
   * the thread state, this method tries to transition the promise to
   * the value or throwable state before serializing its state (which
   * will fail if the transition was not successful).
   */
  private synchronized void writeObject(ObjectOutputStream out)
    throws IOException {

    // Transition thread state into value/throwable state if possible.
    if (STATE_THREAD == state) {
      Evaluator evaluator = (Evaluator)one;

      if (evaluator.isResultReady()) {
        one = evaluator.getResult();
        two = null;

        if (evaluator.isResultValue()) {
          state = STATE_VALUE;
        } else {
          state = STATE_THROWABLE;
        }
      }
    }

    // Serialize object.
    out.defaultWriteObject();
  }

  // ============================== Methods =============================
  
  /**
   * Return the state of this promise.
   *
   * <p>If this promise is in the thread state, this method checks
   * whether the referenced evaluation has terminated and, if so,
   * transitions this promise into the value or throwable state,
   * depending on the result of the evaluation. For all other states,
   * this method simply returns the corresponding state flag.</p>
   *
   * <p>Note that a promise may transition from the expression state
   * or thread state to the value of throwable state between an
   * invocation of <code>getState()</code> and the invocation of the
   * corresponding state accessor methods. However, once a promise is
   * in the value or throwable state, it cannot change its state.</p>
   * 
   * @see     #STATE_EXPRESSION
   * @see     #STATE_THREAD
   * @see     #STATE_VALUE
   * @see     #STATE_THROWABLE
   *
   * @return  The state of this promise.
   */
  public int getState() {
    if (STATE_THREAD == state) {
      synchronized(this) {
        if (STATE_THREAD == state) {
          Evaluator evaluator = (Evaluator)one;

          if (evaluator.isResultReady()) {
            one = evaluator.getResult();
            two = null;

            if (evaluator.isResultValue()) {
              state = STATE_VALUE;
            } else {
              state = STATE_THROWABLE;
            }
          }
        }
      }
    }

    return state;
  }

  /**
   * Return the expression for this promise. If this promise is in the
   * expression state, this method returns the unevaluated expression
   * referenced by this promise. Otherwise, it throws an
   * <code>IllegalStateException</code>.
   *
   * @see     #getState()
   *
   * @return     The expression for this promise.
   * @throws  IllegalStateException
   *             Signals that this promise is not in the
   *             expression state.
   */
  public Object getExpression() {
    if (STATE_EXPRESSION == state) {
      synchronized(this) {
        if (STATE_EXPRESSION == state) {
          return one;
        }
      }
    }

    throw new IllegalStateException("Promise not in expression state");
  }

  /**
   * Return the environment for this promise. If this promise is in
   * the expression state, this method returns the environment to
   * evaluate the corresponding expression in. Otherwise, it throws
   * an <code>IllegalStateException</code>.
   *
   * @see     #getState()
   *
   * @return     The environment for this promise.
   * @throws  IllegalStateException
   *             Signals that this promise is not in the
   *             expression state.
   */
  public Environment getEnvironment() {
    if (STATE_EXPRESSION == state) {
      synchronized(this) {
        if (STATE_EXPRESSION == state) {
          return (Environment)two;
        }
      }
    }

    throw new IllegalStateException("Promise not in expression state");
  }

  /**
   * Return the evaluator for this promise. If this promise is in the
   * thread state, this method returns the referenced
   * evaluator. Otherwise, it throws an
   * <code>IllegalStateException</code>.
   *
   * @see     #getState()
   * 
   * @return     The evaluator for this promise.
   * @throws  IllegalStateException
   *             Signals that this promise is not in the thread
   *             state.
   */
  public Evaluator getEvaluator() {
    if (STATE_THREAD == state) {
      synchronized(this) {
        if (STATE_THREAD == state) {
          return (Evaluator)one;
        }
      }
    }

    throw new IllegalStateException("Promise not in thread state");
  }

  /**
   * Return the thread for this promise. If this promise is in the
   * thread state, this method returns the referenced thread.
   * Otherwise, it throws an <code>IllegalStateException</code>.
   *
   * @see     #getState()
   *
   * @return     The thread for this promise.
   * @throws  IllegalStateException
   *             Signals that this promise is not in the thread
   *             state.
   */
  public Thread getThread() {
    if (STATE_THREAD == state) {
      synchronized(this) {
        if (STATE_THREAD == state) {
          return (Thread)two;
        }
      }
    }

    throw new IllegalStateException("Promise not in thread state");
  }

  /**
   * Return the value for this promise. If this promise is in the
   * value state, this method returns the referenced value.
   * Otherwise, it throws an <code>IllegalStateException</code>.
   *
   * @see     #getState()
   *
   * @return     The value for this promise.
   * @throws   IllegalStateException
   *             Signals that this promise is not in the value
   *             state.
   */
  public Object getValue() {
    if (STATE_VALUE == state) {
      return one;
    } else {
      throw new IllegalStateException("Promise not in value state");
    }
  }

  /**
   * Return the throwable for this promise. If this promise is in the
   * throwable state, this method returns the referenced throwable.
   * Otherwise, it throws an <code>IllegalStateException</code>.
   *
   * @see     #getState()
   *
   * @return     The throwable for this promise.
   * @throws  IllegalStateException
   *             Signals that this promise is not in the throwable
   *             state.
   */
  public Throwable getThrowable() {
    if (STATE_THROWABLE == state) {
      return (Throwable)one;
    } else {
      throw new IllegalStateException("Promise not in throwable state");
    }
  }

  /**
   * Return the result of forcing this promise. If this promise is in
   * the value state, this method returns the referenced value.  If
   * this promise is in the throwable state, this method throws the
   * corresponding throwable. The throwable is thrown directly if the
   * referenced throwable is an instance of
   * <code>EvaluatorException</code>, <code>RuntimeException</code>,
   * or <code>Error</code>, and otherwise is wrapped in a newly
   * created <code>WrappedException</code>. If this promise is in
   * either the expression or thread state, this method throws an
   * <code>IllegalStateException</code>.
   *
   * @return     The value of this promise
   * @throws  EvaluatorException
   *             Signals some exceptional condition when forcing this
   *             promise.
   * @throws  IllegalStateException
   *             Signals that this promise is neither in the value
   *             nor throwable state.
   */
  public Object getResult() throws EvaluatorException {
    if (STATE_VALUE == state) {
      return one;

    } else if (STATE_THROWABLE == state) {
      EvaluatorException.signal((Throwable)one);
      return null; // Unreachable - here to make compiler happy.

    } else {
      throw new IllegalStateException(
                             "Promise neither in value nor throwable state");
    }
  }

  /**
   * Force this promise. If this promise is in the expression state,
   * calling this method transitions this promise in the value state,
   * using the specified value as the value for this promise. For all
   * other states, calling this method has no effect.
   *
   * @param  value  The value for this promise.
   */
  public void force(Object value) {
    if (STATE_EXPRESSION == state) {
      synchronized(this) {
        if (STATE_EXPRESSION == state) {
          one   = value;
          two   = null;
          state = STATE_VALUE;
        }
      }
    }
  }

  /**
   * Force this promise. If this promise is in the expression state,
   * calling this method transitions this promise into the throwable
   * state, using the specified throwable as the throwable for this
   * promise. For all other states, calling this method has no effect.
   *
   * @param  x  The throwable for this promise.
   */
  public void force(Throwable x) {
    if (STATE_EXPRESSION == state) {
      synchronized(this) {
        if (STATE_EXPRESSION == state) {
          one   = x;
          two   = null;
          state = STATE_THROWABLE;
        }
      }
    }
  }

  /**
   * Return a string representation of this promise.
   *
   * @return  A string representation of this promise.
   */
  public String toString() {
    return "#[promise " + super.toString() + "]";
  }

  // ========================= Thread Management ========================

  /**
   * The current thread manager. If this field is not
   * <code>null</code>, promises that evaluate an expression in a
   * separate thread use the current thread manager to fork new
   * threads.
   */
  private static ThreadManager threadManager;

  /**
   * Return the current thread manager. If the current thread manager
   * is not <code>null</code>, newly created promises that evaluate
   * an expression in a separate thread use the returned thread manager
   * to create new threads.
   *
   * @return  The current thread manager.
   */
  public static ThreadManager getThreadManager() {
    return threadManager;
  }

  /**
   * Set the current thread manager to the specified thread
   * manager. If the specified thread manager is not
   * <code>null</code>, newly created promises that evaluate an
   * expression in a separate thread use the specified thread manager
   * to create new threads. If the specified thread manager is
   * <code>null</code>, such promises simply create a new thread which
   * dies when the evaluation terminates.
   * 
   * @param   m  The thread manager for threaded promises.
   */
  public static void setThreadManager(ThreadManager m) {
    threadManager = m;
  }

}
