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
 * The superclass of all evaluator related exceptions. An evaluator
 * exception can hold a pointer to the cause for the particular
 * exception. Even though some methods, such as {@link
 * Applicable#apply(Pair,Evaluator)}, declare that they throw this
 * exception, an evaluator exception should never be thrown directly,
 * but rather one of its subclasses, indicating a specific type of
 * exception.
 *
 * @author   &copy; Copyright 1998-2000 by Robert Grimm.
 *           Written by <a href="mailto:rgrimm@alum.mit.edu">Robert
 *           Grimm</a>.
 * @version  1.0
 */
public class EvaluatorException extends Exception {

  /**
   * Pointer to the object that caused this exception.
   *
   * @serial
   */
  protected Object cause = null;

  /**
   * Flag to indicate whether a cause was specified for this
   * exception; useful to distinguish <code>null</code> as a
   * cause from the default value for <code>cause</code>.
   *
   * @serial  <code>true</code> iff <code>cause</code> has been
   *          specified during creation of the particular instance
   *          of this evaluator exception.
   */
  protected boolean hasCause = false;

  /**
   * Create a new evaluator exception with no detail message and no cause.
   */
  public EvaluatorException() {
    super();
  }

  /**
   * Create a new evaluator exception with the specified detail message
   * and no cause.
   *
   * @param  s  The detail message.
   */
  public EvaluatorException(String s) {
    super(s);
  }

  /**
   * Create a new evaluator exception with the specified detail message
   * and the specified cause.
   *
   * @param   s  The detail message.
   * @param   o  The cause.
   */
  public EvaluatorException(String s, Object o) {
    super(s);
    cause    = o;
    hasCause = true;
  }

  /**
   * Test whether this evaluator exception includes a cause.
   *
   * @return  <code>true</code> iff this evaluator exception was
   *          created with a specific cause by {@link
   *          #EvaluatorException(String,Object)}.
   */
  public boolean hasCausingObject() {
    return hasCause;
  }

  /**
   * Return the cause. The {@link #hasCausingObject()} method can be
   * used to distinguish <code>null</code> as a cause for this
   * exception from <code>null</code> as an indication that no cause
   * has been specified.
   *
   * @return  The cause for this evaluator exception, or
   *          <code>null</code> if no cause has been specified.
   */
  public Object getCausingObject() {
    return cause;
  }

  /**
   * Set the cause if unspecified before. If no cause has been set
   * before the specified object becomes the cause for this exception.
   * Otherwise the argument is ignored.
   *
   * @param   o  The cause for this exception.
   */
  public void setCausingObject(Object o) {
    if (! hasCause) {
      cause    = o;
      hasCause = true;
    }
  }

  /**
   * Return a string describing this exception. The returned string is
   * the same string as the one returned by calling
   * <code>super.toString()</code>, unless a cause has been specified.
   * In the latter case the returned string is of the form:
   * <blockquote><pre>
   * super.toString() + " - " + String.valueOf(cause)
   * </pre></blockquote>
   *
   * @see     #cause
   * @see     Exception#toString()
   *
   * @return  A string describing this exception.
   */
  public String toString() {
    String s = super.toString();

    if (hasCause) {
      return s + " - " + String.valueOf(cause);
    } else {
      return s;
    }
  }

  /**
   * Signal the specified exceptional condition. If the specified
   * throwable is an instance of an evaluator exception, a runtime
   * exception, or an error, this method simply throws the specified
   * throwable. Otherwise, it throws a new wrapped exception which
   * wraps the specified throwable and has <code>"Unexpected
   * exception"</code> as its message and no cause.
   *
   * @see     WrappedException
   *
   * @param   x  The throwable to throw.
   */
  public static void signal(Throwable x) throws EvaluatorException {
    signal(x, "Unexpected exception");
  }

  /**
   * Signal the specified exceptional condition. If the specified
   * throwable is an instance of an evaluator exception, a runtime
   * exception, or an error, this method simply throws the specified
   * throwable. Otherwise, it throws a new wrapped exception which
   * wraps the specified throwable and has the specified message and
   * no cause.
   *
   * @see     WrappedException
   *
   * @param   x      The throwable to throw.
   * @param   msg    The message for a newly created wrapped exception.
   */
  public static void signal(Throwable x, String msg)
    throws EvaluatorException {

    if (x instanceof EvaluatorException) {
      throw (EvaluatorException)x;

    } else if (x instanceof RuntimeException) {
      throw (RuntimeException)x;

    } else if (x instanceof Error) {
      throw (Error)x;

    } else {
      throw new WrappedException(msg, x);
    }
  }

  /**
   * Signal the specified exceptional condition. If the specified
   * throwable is an instance of an evaluator exception, a runtime
   * exception, or an error, this method simply throws the specified
   * throwable. Otherwise, it throws a new wrapped exception which
   * wraps the specified throwable and has the specified message and
   * specified cause.
   *
   * @see     WrappedException
   *
   * @param   x      The throwable to throw.
   * @param   msg    The message for a newly created wrapped exception.
   * @param   cause  The cause of the exception.
   */
  public static void signal(Throwable x, String msg, Object cause)
    throws EvaluatorException {

    if (x instanceof EvaluatorException) {
      throw (EvaluatorException)x;

    } else if (x instanceof RuntimeException) {
      throw (RuntimeException)x;

    } else if (x instanceof Error) {
      throw (Error)x;

    } else {
      throw new WrappedException(msg, x, cause);
    }
  }

}
