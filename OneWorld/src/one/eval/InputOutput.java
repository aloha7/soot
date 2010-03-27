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

import java.io.Reader;
import java.io.Writer;
import java.io.IOException;

import one.util.Bug;

/**
 * Implementation of the Scheme standard procedures for input and
 * output. This class implements the operators defined in &sect;6.6 of
 * R<sup><font size="-1">5</font></sup>RS.
 *
 * <p>Scheme input ports are mapped to objects of type {@link
 * java.io.Reader java.io.Reader}, and output ports are mapped to
 * objects of type {@link java.io.Writer java.io.Writer}. The end of
 * file object is {@link Format#EOF}.</p>
 *
 * <p>As a result of this mapping, the Scheme primitive operators
 * <code>read</code> and <code>peek-char</code> may fail on some input
 * ports, namely those that do not support the {@link
 * java.io.Reader#mark(int)} and {@link java.io.Reader#reset()}
 * methods. However, input ports created by the operators defined in
 * this class are guaranteed to support all input operators.</p>

 * <p>All input and output ports created by the operators defined in
 * this class use the current character encoding defined by the
 * "<code>eval.encoding</code>" system property.</p>
 *
 * <p>If the file name for any of the I/O operators defined by this
 * class starts with "<code>http:</code>", "<code>ftp:</code>",
 * "<code>file:</code>", or "<code>jar:</code>", it is treated as a
 * URL and the corresponding I/O stream accesses the contents of the
 * specified URL.</p>
 *
 * <p>The <code>load</code> procedure implemented by this class takes
 * a second, optional argument, which can be any object. If the second
 * argument is specified and not <code>#f</code>, the results of
 * evaluating all expressions in the specified file are printed to the
 * top-level writer.</p>
 *
 * @author   &copy; Copyright 1998-2000 by Robert Grimm.
 *           Written by <a href="mailto:rgrimm@alum.mit.edu">Robert
 *           Grimm</a>.
 * @version  1.0
 */
public final class InputOutput extends AbstractApplicable {

  // =========================== Constants =============================

  private static final int
    CALL_INPUT = 30001, CALL_OUTPUT = 30002,
    INPUT_PORT_Q = 30003, OUTPUT_PORT_Q = 30004, CURRENT_INPUT_PORT = 30005,
    CURRENT_OUTPUT_PORT = 30006, WITH_INPUT = 30007, WITH_OUTPUT = 30008,
    OPEN_INPUT = 30009, OPEN_OUTPUT = 30010, CLOSE_INPUT = 30011,
    CLOSE_OUTPUT = 30012, READ = 30013, READ_CHAR = 30014, PEEK_CHAR = 30015,
    EOF_OBJECT_Q = 30016, CHAR_READY_Q = 30017, WRITE = 30018, DISPLAY = 30019,
    NEWLINE = 30020, WRITE_CHAR = 30021, LOAD = 30022;

  // Primitive continuations.
  static final int
    CONT_CALL_FILE = 60001, CONT_WITH_FILE = 60002;

  // ===================== Primitive Continuation ======================

  /** Implementation of a primitive I/O continuation. */
  static final class Continuation extends AbstractApplicable {

    /**
     * Reference to the port, that is reader or writer, to close.
     *
     * @serial  If not <code>null</code>, must be a reference to a
     *          <code>java.io.Reader</code> or
     *          <code>java.io.Writer</code> object. In this case,
     *          this I/O operator is not serializable, because
     *          readers and writers are not serializable.
     */
    Object port;
    
    /**
     * Create a new I/O continuation with the specified name, opcode
     * and number of arguments.
     *
     * @param  name     The name of the I/O continuation.
     * @param  opcode   The opcode of the I/O continuation.
     * @param  minArgs  The non-negative minimum number of arguments
     *                  for this I/O continuation.
     * @param  maxArgs  The non-negative maximum number of arguments
     *                  for this I/O continuation, or -1 if this
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
     * Apply this I/O continuation on the specified arguments.
     *
     * @param   l          The arguments as a proper list.
     * @param   numArgs    The number of arguments in <code>l</code>.
     * @param   evaluator  The calling evaluator.
     * @return             The result of applying this continuation
     *                     on the specified arguments.
     * @throws  EvaluatorException
     *                     Signals an exceptional condition when
     *                     applying this continuation to the specified
     *                     arguments.
     */
    protected Object apply1(Pair l, int numArgs, Evaluator evaluator)
      throws EvaluatorException {

      if (CONT_CALL_FILE == opcode) {
        if (null != port) {
          Object o = port;
          port     = null;

          if (o instanceof Reader) {
            Reader in = (Reader)o;
            try {
              in.close();
            } catch (IOException x) {
              throw new WrappedException("Unable to close", x, in);
            }
            evaluator.unmarkForCleanup(in);
            
          } else if (o instanceof Writer) {
            Writer out = (Writer)o;
            try {
              out.close();
            } catch (IOException x) {
              throw new WrappedException("Unable to close", x, out);
            }
            evaluator.unmarkForCleanup(out);
            
          } else {
            // Must never happen.
            throw new Bug("Invalid internal state for " + toString());
          }
        }

      } else if (CONT_WITH_FILE == opcode) {
        if (null != port) {
          Object o = port;
          port     = null;
          
          if (o instanceof Reader) {
            Reader in1 = (Reader)o;
            Reader in2 = evaluator.popReader();
            if ((in1 != in2) && (null != in2)) {
              // Return port. Somebody did some clean up in the mean time.
              evaluator.pushReader(in2);
            }
            try {
              in1.close();
            } catch (IOException x) {
              throw new WrappedException("Unable to close", x, in1);
            }
            
          } else if (o instanceof Writer) {
            Writer out1 = (Writer)o;
            Writer out2 = evaluator.popWriter();
            if ((out1 != out2) && (null != out2)) {
              // Return port. Somebody did some clean up in the mean time.
              evaluator.pushWriter(out2);
            }
            try {
              out1.close();
            } catch (IOException x) {
              throw new WrappedException("Unable to close", x, out1);
            }
          }
        }

      } else {
        throw new Bug("Invalid opcode " + opcode + " for I/O continuation "
                      + toString());
      }

      // Pass through value(s).
      if (1 == numArgs) {
        return l.car;
      } else {
        evaluator.returnValues();
        return l;
      }
    }
  }

  // ========================= Constructor =============================

  /**
   * Create a new I/O operator with the specified name, opcode,
   * and number of arguments.
   *
   * @param  name     The name of the I/O operator.
   * @param  opcode   The opcode of the I/O operator.
   * @param  minArgs  The non-negative minimum number of arguments
   *                  for this I/O operator.
   * @param  maxArgs  The non-negative maximum number of arguments
   *                  for this I/O operator, or -1 if this
   *                  operator takes an unlimited maximum number
   *                  of arguments. In the former case,
   *                  <code>maxArgs >= minArgs</code> must be
   *                  <code>true</code>.
   */
  private InputOutput(String name, int opcode, int minArgs, int maxArgs) {
    this.name    = name;
    this.opcode  = opcode;
    this.minArgs = minArgs;
    this.maxArgs = maxArgs;
  }

  // ============================ Equality ================================

  /**
   * Determine whether this I/O operator equals the specified
   * object.
   *
   * @param   o  The object with which to compare.
   * @return     <code>true</code> if this I/O operator equals
   *             the specified object.
   */
  public boolean equals(Object o) {
    if (this == o) return true;
    if (! (o instanceof InputOutput)) return false;

    InputOutput other = (InputOutput)o;

    return (   (name.equals(other.name))
            && (opcode  == other.opcode)
            && (minArgs == other.minArgs)
            && (maxArgs == other.maxArgs));
  }

  /**
   * Return a hash code for this I/O operator.
   *
   * @return  A hash code for this I/O operator.
   */
  public int hashCode() {
    return name.hashCode();
  }

  // ============================ Methods ==============================

  /**
   * Apply this I/O operator on the specified arguments.
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
    Object o1   = null;
    Object o2   = null;

    {
      Pair temp = l;
      
      switch (numArgs) {

      case 1:
        o1 = temp.car;
        break;
        
      case 2:
        o1   = temp.car;
        temp = (Pair)temp.cdr;
        o2   = temp.car;
        break;
        
      default:
        // Nothing to do.
      }
    }    

    Reader       in;
    Writer       out;

    // Do the deed.
    switch (opcode) {

    case CALL_INPUT:
      return callFile(Cast.toString(o1),
                      Cast.toProcedure(o2), false, evaluator);
    case CALL_OUTPUT:
      return callFile(Cast.toString(o1), 
                      Cast.toProcedure(o2), true,  evaluator);
    case INPUT_PORT_Q:
      return Cast.box(o1 instanceof Reader);
    case OUTPUT_PORT_Q:
      return Cast.box(o1 instanceof Writer);
    case CURRENT_INPUT_PORT:
      return evaluator.getCurrentReader();
    case CURRENT_OUTPUT_PORT:
      return evaluator.getCurrentWriter();
    case WITH_INPUT:
      return withFile(Cast.toString(o1),
                      Cast.toProcedure(o2), false, evaluator);
    case WITH_OUTPUT:
      return withFile(Cast.toString(o1),
                      Cast.toProcedure(o2), true,  evaluator);
    case OPEN_INPUT:
      in = Shell.createReader(Shell.createInputStream(Cast.toString(o1)),
                              Shell.getEncoding());
      evaluator.markForCleanup(in);
      return in;
    case OPEN_OUTPUT:
      out = Shell.createWriter(Shell.createOutputStream(Cast.toString(o1)),
                               Shell.getEncoding());
      evaluator.markForCleanup(out);
      return out;
    case CLOSE_INPUT:
      in = Shell.toInputPort(o1);
      try {
        in.close();
      } catch (IOException x) {
        throw new WrappedException("Unable to close", x, in);
      }
      evaluator.unmarkForCleanup(in);
      return Boolean.FALSE;
    case CLOSE_OUTPUT:
      out = Shell.toOutputPort(o1);
      try {
        out.close();
      } catch (IOException x) {
        throw new WrappedException("Unable to close", x, out);
      }
      evaluator.unmarkForCleanup(out);
      return Boolean.FALSE;
    case READ:
      in = ((0 == numArgs)?
            evaluator.getCurrentReader() :
            Shell.toInputPort(o1));
      return evaluator.getFormat().read(in);
    case READ_CHAR:
      in = ((0 == numArgs)?
            evaluator.getCurrentReader() :
            Shell.toInputPort(o1));
      try {
        int i = in.read();
        if (-1 == i) { return Format.EOF; } else { return Cast.box((char)i); }
      } catch (IOException x) {
        throw new WrappedException("Unable to read from", x, in);
      }
    case PEEK_CHAR:
      in = ((0 == numArgs)?
            evaluator.getCurrentReader() :
            Shell.toInputPort(o1));
      if (! in.markSupported()) {
        throw new BadArgumentException("Mark/reset not supported by", in);
      }
      try {
        in.mark(1);
        int i = in.read();
        in.reset();
        if (-1 == i) { return Format.EOF; } else { return Cast.box((char)i); }
      } catch (IOException x) {
        throw new WrappedException("Unable to peek into", x, in);
      }
    case EOF_OBJECT_Q:
      return Cast.box(Format.EOF == o1);
    case CHAR_READY_Q:
      in = ((0 == numArgs)?
            evaluator.getCurrentReader() :
            Shell.toInputPort(o1));
      try {
        return Cast.box(in.ready());
      } catch (IOException x) {
        throw new WrappedException("Unable to access", x, in);
      }
    case WRITE:
    case DISPLAY:
      evaluator.getFormat().write(o1,
                                  ((1 == numArgs)? evaluator.getCurrentWriter()
                                   : Shell.toOutputPort(o2)),
                                  (WRITE == opcode));
      return Boolean.FALSE;
    case NEWLINE:
      out = ((0 == numArgs)?
             evaluator.getCurrentWriter() :
             Shell.toOutputPort(o1));
      try {
        out.write(Format.getLineSeparator());
      } catch (IOException x) {
        throw new WrappedException("Unable to write to", x, out);
      }
      return Boolean.FALSE;
    case WRITE_CHAR:
      out = ((1 == numArgs)?
             evaluator.getCurrentWriter() :
             Shell.toOutputPort(o2));
      try {
        out.write(Cast.toChar(o1));
      } catch (IOException x) {
        throw new WrappedException("Unable to write to", x, out);
      }
      return Boolean.FALSE;
    case LOAD:
      {
        in = Shell.createReader(Shell.createInputStream(Cast.toString(o1)),
                                Shell.getEncoding());
        Evaluator eval = evaluator.spawn();
        try {
          return Shell.loop(in, eval, eval.getTopLevelWriter(),
                            ((1 == numArgs)? false : Cast.toBoolean(o2)),
                            0, false, false);
        } finally {
          try {
            in.close();
          } catch (IOException x) {
            throw new WrappedException("Unable to close", x, in);
          }
        }
      }
    default:
      throw new Bug("Invalid opcode " + opcode + " for I/O operator " +
                    toString());
    }
  }

  /**
   * Execute <code>call-with-input-file</code> and
   * <code>call-with-output-file</code>.
   *
   * @param   name       The name of the file.
   * @param   proc       The procedure to apply to the port.
   * @param   output     <code>true</code> if operator is
   *                     <code>call-with-output-file</code>.
   * @param   evaluator  The calling evaluator.
   * @return             The result of executing the specified
   *                     operation.
   * @throws  BadArgumentException
   *                  Signals that <code>proc</code> does not
   *                  accept exactly one argument.
   * @throws  WrappedException
   *                  Signals an exceptional condition when
   *                  creating the port for file <code>name</code>.
   */
  private Object callFile(String name, Applicable proc, boolean output,
                          Evaluator evaluator)
    throws BadArgumentException, WrappedException {

    // Verify proc accepts exactly one argument.
    if (1 < proc.getMinArgs()) {
      throw new BadArgumentException("Procedure expects too many (" +
                                     proc.getMinArgs() + ") arguments", proc);
    }

    // Set up the port, the continuation, and continue the evaluation.
    String enc = Shell.getEncoding();

    if (output) {
      Writer       out  = Shell.createWriter(Shell.createOutputStream(name),
                                             enc);
      Pair         expr = new Pair(proc, new Pair(out, Pair.EMPTY_LIST));
      Continuation cont;

      if (evaluator.acceptsValues()) {
        cont            = new Continuation("call-with-output-file",
                                           CONT_CALL_FILE, 0, -1);
        evaluator.setUpValues(cont);
      } else {
        cont            = new Continuation("call-with-output-file",
                                           CONT_CALL_FILE, 1,  1);
        expr            = new Pair(cont, new Pair(expr, Pair.EMPTY_LIST));
        evaluator.returnExpression();
      }

      cont.port         = out;
      evaluator.markForCleanup(out);
      return expr;

    } else {
      Reader       in   = Shell.createReader(Shell.createInputStream(name),
                                             enc);
      Pair         expr = new Pair(proc, new Pair(in, Pair.EMPTY_LIST));
      Continuation cont;

      if (evaluator.acceptsValues()) {
        cont            = new Continuation("call-with-input-file",
                                           CONT_CALL_FILE, 0, -1);
        evaluator.setUpValues(cont);
      } else {
        cont            = new Continuation("call-with-input-file",
                                           CONT_CALL_FILE, 1,  1);
        expr            = new Pair(cont, new Pair(expr, Pair.EMPTY_LIST));
        evaluator.returnExpression();
      }

      cont.port         = in;
      evaluator.markForCleanup(in);
      return expr;
    }
  }

  /**
   * Execute <code>with-input-from-file</code> and
   * <code>with-output-to-file</code>.
   *
   * @param   name       The name of the file.
   * @param   proc       The thunk to apply.
   * @param   output     <code>true</code> if operator is
   *                     <code>with-output-to-file</code>.
   * @param   evaluator  The calling evaluator.
   * @return             The result of executing the specified
   *                     operation.
   * @throws  BadArgumentException
   *                     Signals that <code>proc</code> can not be
   *                     called with no arguments.
   * @throws  WrappedException
   *                     Signals an exceptional condition when
   *                     creating the port for file <code>name</code>.
   */
  private Object withFile(String name, Applicable proc, boolean output,
                          Evaluator evaluator)
    throws BadArgumentException, WrappedException {

    // Verify proc accepts no arguments.
    if (0 < proc.getMinArgs()) {
      throw new BadArgumentException("Procedure expects too many (" +
                                     proc.getMinArgs() + ") arguments", proc);
    }

    // Set up the port, the continuation, and continue the evaluation.
    String enc = Shell.getEncoding();

    if (output) {
      Writer       out  = Shell.createWriter(Shell.createOutputStream(name),
                                             enc);
      Pair         expr = new Pair(proc, Pair.EMPTY_LIST);
      Continuation cont;

      if (evaluator.acceptsValues()) {
        cont            = new Continuation("with-output-to-file",
                                           CONT_WITH_FILE, 0, -1);
        evaluator.setUpValues(cont);
      } else {
        cont            = new Continuation("with-output-to-file",
                                           CONT_WITH_FILE, 1,  1);
        expr            = new Pair(cont, new Pair(expr, Pair.EMPTY_LIST));
        evaluator.returnExpression();
      }

      cont.port         = out;
      evaluator.pushWriter(out);
      return expr;

    } else {
      Reader       in   = Shell.createReader(Shell.createInputStream(name),
                                             enc);
      Pair         expr = new Pair(proc, Pair.EMPTY_LIST);
      Continuation cont;

      if (evaluator.acceptsValues()) {
        cont            = new Continuation("with-input-from-file",
                                           CONT_WITH_FILE, 0, -1);
        evaluator.setUpValues(cont);
      } else {
        cont            = new Continuation("with-input-from-file",
                                           CONT_WITH_FILE, 1,  1);
        expr            = new Pair(cont, new Pair(expr, Pair.EMPTY_LIST));
        evaluator.returnExpression();
      }

      cont.port         = in;
      evaluator.pushReader(in);
      return expr;
    }
  }

  // ========================== Installation ===========================

  /**
   * Install the I/O operators in the specified environment.
   *
   * @param   env     The environment to install the I/O
   *                  operators into.
   */
  public static void install(Environment env) {
    add(env, "call-with-input-file", CALL_INPUT,            2,  2);
    add(env, "call-with-output-file",CALL_OUTPUT,           2,  2);
    add(env, "input-port?",          INPUT_PORT_Q,          1,  1);
    add(env, "output-port?",         OUTPUT_PORT_Q,         1,  1);
    add(env, "current-input-port",   CURRENT_INPUT_PORT,    0,  0);
    add(env, "current-output-port",  CURRENT_OUTPUT_PORT,   0,  0);
    add(env, "with-input-from-file", WITH_INPUT,            2,  2);
    add(env, "with-output-to-file",  WITH_OUTPUT,           2,  2);
    add(env, "open-input-file",      OPEN_INPUT,            1,  1);
    add(env, "open-output-file",     OPEN_OUTPUT,           1,  1);
    add(env, "close-input-port",     CLOSE_INPUT,           1,  1);
    add(env, "close-output-port",    CLOSE_OUTPUT,          1,  1);
    add(env, "read",                 READ,                  0,  1);
    add(env, "read-char",            READ_CHAR,             0,  1);
    add(env, "peek-char",            PEEK_CHAR,             0,  1);
    add(env, "eof-object?",          EOF_OBJECT_Q,          1,  1);
    add(env, "char-ready?",          CHAR_READY_Q,          0,  1);
    add(env, "write",                WRITE,                 1,  2);
    add(env, "display",              DISPLAY,               1,  2);
    add(env, "newline",              NEWLINE,               0,  1);
    add(env, "write-char",           WRITE_CHAR,            1,  2);
    add(env, "load",                 LOAD,                  1,  2);
  }

  /**
   * Create a new I/O operator as specified and add a binding
   * for it to the specified environment.
   *
   * @param   env       The environment for the new I/O operator.
   * @param   name      The name of the new I/O operator.
   * @param   opcode    The opcode of the new I/O operator.
   * @param   minArgs   The non-negative minimum number of arguments for
   *                    the new I/O operator.
   * @param   maxArgs   The non-negative maximum number of arguments for
   *                    the new I/O operator, or -1 if it takes an
   *                    unlimited maximum number of arguments.
   */
  private static void add(Environment env, String name, int opcode,
                          int minArgs, int maxArgs) {
    name          = name.intern();
    Symbol      s = Symbol.intern(name);
    InputOutput v = new InputOutput(name, opcode, minArgs, maxArgs);

    env.bind(s, v);
  }

}
