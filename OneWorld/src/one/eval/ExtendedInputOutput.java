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
import java.io.BufferedInputStream;
import java.io.ObjectInputStream;

import java.io.OutputStream;
import java.io.BufferedOutputStream;
import java.io.ObjectOutputStream;
import java.io.Writer;

import java.io.IOException;

import one.util.Bug;

/**
 * Implementation of extended I/O procedures to access Java byte and
 * object streams. To support binary I/O and object serialization,
 * this class defines a set of I/O operators, which are similar to
 * those defined in the Scheme standard for ports, but which operate
 * on byte and object streams. It also defines the <code>flush</code>
 * procedure, which flushes the internal buffers of an output port.
 *
 * <p>Input streams are instances of {@link InputStream}, and output
 * streams are instances of {@link OutputStream}. Object input streams
 * are instances of {@link ObjectInputStream}, and object output
 * streams are instances of {@link ObjectOutputStream}.</p>
 *
 * <p>If the file name for any of the I/O operators defined by this
 * class starts with "<code>http:</code>", "<code>ftp:</code>",
 * "<code>file:</code>", or "<code>jar:</code>", it is treated as a
 * URL and the corresponding I/O stream accesses the contents of the
 * specified URL.</p>
 *
 * <p>This class defines the following extensions to the Scheme
 * standard:</p>
 *
 * <p><code>(flush)</code><br />
 * <code>(flush</code>&nbsp;&nbsp;<i>port</i><code>)</code><br />
 * Flush the given output <i>port</i>. Forces all characters that may
 * have been collected in internal buffers to be written to the
 * specified output port. <code>Flush</code> returns an unspecified
 * result. It is an error if <i>port</i> is not a currently open
 * output port. The <i>port</i> argument may be omitted, in which case
 * it defaults to the value returned by
 * <code>current-output-port</code>.</p>
 *
 * <p><code>(input-stream?</code>&nbsp;&nbsp;<i>obj</i><code>)</code><br />
 * <code>(output-stream?</code>&nbsp;&nbsp;<i>obj</i><code>)</code><br />
 * <code>(object-input?</code>&nbsp;&nbsp;<i>obj</i><code>)</code><br />
 * <code>(object-output?</code>&nbsp;&nbsp;<i>obj</i><code>)</code><br />
 * Returns <code>#t</code> if <i>obj</i> is an input stream, output
 * stream, object input stream, or object output stream respectively,
 * otherwise returns <code>#f</code>.</p>
 *
 * <p><code>(open-input-stream</code>&nbsp;&nbsp;<i>filename</i><code>)</code><br />
 * <code>(open-object-input</code>&nbsp;&nbsp;<i>filename</i><code>)</code><br />
 * Takes a string naming an existing file and returns an input stream
 * or object input stream capable of delivering bytes or objects from
 * the file. If the file cannot be opened, an error is signalled.</p>
 *
 * <p><code>(open-output-stream</code>&nbsp;&nbsp;<i>filename</i><code>)</code><br />
 * <code>(open-object-output</code>&nbsp;&nbsp;<i>filename</i><code>)</code><br />
 * Takes a string naming an output file to be created and returns an
 * output stream or object output stream capable of writing bytes or
 * objects to a new file by that name. If the file cannot be opened,
 * an error is signalled. If a file with the given name already
 * exists, the effect is unspecified.</p>
 *
 * <p><code>(flush-stream</code>&nbsp;&nbsp;<i>stream</i><code>)</code><br />
 * Flush the output <i>stream</i>. Forces all bytes that may have been
 * collected in internal buffers to be written to the specified output
 * stream. <code>Flush-stream</code> returns an unspecified result. It
 * is an error if <i>stream</i> is not a currently open output or
 * object output stream.</p>
 *
 * <p><code>(close-input-stream</code>&nbsp;&nbsp;<i>stream</i><code>)</code><br />
 * <code>(close-output-stream</code>&nbsp;&nbsp;<i>stream</i><code>)</code><br />
 * Closes the file associated with <i>stream</i>, rendering the
 * <i>stream</i> incapable of delivering or accepting bytes or
 * objects. These routines have no effect if the file has already been
 * closed. The returned value is unspecified.</p>
 *
 * <p><code>(read-byte</code>&nbsp;&nbsp;<i>stream</i><code>)</code><br />
 * Returns the next byte available from the input <i>stream</i>. If no
 * more bytes are available, an end of file object is returned.</p>
 *
 * <p><code>(peek-byte</code>&nbsp;&nbsp;<i>stream</i><code>)</code><br />
 * Returns the next byte available from the input <i>stream</i>
 * without consuming it. If no more bytes are available, an end of
 * file object is returned.</p>
 *
 * <p><code>(byte-ready?</code>&nbsp;&nbsp;<i>stream</i><code>)</code><br />
 * Returns the number of bytes that can be read from the input
 * <i>stream</i> without blocking if some positive number of bytes are
 * ready on the input <i>stream</i> and returns <code>#f</code>
 * otherwise.</p>
 *
 * <p><code>(read-object</code>&nbsp;&nbsp;<i>stream</i><code>)</code><br />
 * <code>Read-object</code> converts the serialized representation of
 * Java objects into the objects themselves. <code>Read-object</code>
 * returns the next object parsable from the given object input
 * <i>stream</i>. If an end of file is encountered before or while
 * reading an object, an error is signalled. It is an error to read
 * from a closed stream.</p>
 *
 * <p><code>(write-byte</code>&nbsp;&nbsp;<i>n</i>&nbsp;&nbsp;<i>stream</i><code>)</code><br />
 * Writes the byte <i>n</i> to the given output <i>stream</i> and
 * returns an unspecified value.</p>
 *
 * <p><code>(write-object</code>&nbsp;&nbsp;<i>obj</i>&nbsp;&nbsp;<i>stream</i><code>)</code><br />
 * <code>Write-object</code> writes the serialized representation of
 * the Java object <i>obj</i> to the given object output
 * <i>stream</i>. <code>Write-object</code> returns an unspecified
 * value. If the object <i>obj</i> or an object reachable through it
 * is not serializable, an error is signalled. It is an error to write
 * to a closed stream.</p>
 * 
 * @author   &copy; Copyright 1998-2000 by Robert Grimm.
 *           Written by <a href="mailto:rgrimm@alum.mit.edu">Robert
 *           Grimm</a>.
 * @version  1.0
 */
public final class ExtendedInputOutput extends AbstractApplicable {

  // =========================== Constants =============================

  private static final int
    FLUSH = 30001,

    INPUT_STREAM_Q = 30010, OUTPUT_STREAM_Q = 30011, OBJECT_INPUT_Q = 30012,
    OBJECT_OUTPUT_Q = 30013, OPEN_INPUT_STREAM = 30014,
    OPEN_OUTPUT_STREAM = 30015, OPEN_OBJECT_INPUT = 30016,
    OPEN_OBJECT_OUTPUT = 30017, FLUSH_STREAM = 30018,
    CLOSE_INPUT_STREAM = 30019, CLOSE_OUTPUT_STREAM = 30020,

    READ_BYTE = 30030, PEEK_BYTE = 30031, BYTE_READY_Q = 30032,

    WRITE_BYTE = 30040,

    READ_OBJECT = 30050, WRITE_OBJECT = 30051;

  // ========================= Constructor =============================

  /**
   * Create a new extended I/O operator with the specified name, opcode,
   * and number of arguments.
   *
   * @param  name     The name of the extended I/O operator.
   * @param  opcode   The opcode of the extended I/O operator.
   * @param  minArgs  The non-negative minimum number of arguments
   *                  for this extended I/O operator.
   * @param  maxArgs  The non-negative maximum number of arguments
   *                  for this extended I/O operator, or -1 if this
   *                  operator takes an unlimited maximum number
   *                  of arguments. In the former case,
   *                  <code>maxArgs >= minArgs</code> must be
   *                  <code>true</code>.
   */
  private ExtendedInputOutput(String name, int opcode,
                              int minArgs, int maxArgs) {
    this.name    = name;
    this.opcode  = opcode;
    this.minArgs = minArgs;
    this.maxArgs = maxArgs;
  }

  // ============================ Equality ================================

  /**
   * Determine whether this extended I/O operator equals the specified
   * object.
   *
   * @param   o  The object with which to compare.
   * @return     <code>true</code> if this extended I/O operator equals
   *             the specified object.
   */
  public boolean equals(Object o) {
    if (this == o) return true;
    if (! (o instanceof ExtendedInputOutput)) return false;

    ExtendedInputOutput other = (ExtendedInputOutput)o;

    return (   (name.equals(other.name))
            && (opcode  == other.opcode)
            && (minArgs == other.minArgs)
            && (maxArgs == other.maxArgs));
  }

  /**
   * Return a hash code for this extended I/O operator.
   *
   * @return  A hash code for this extended I/O operator.
   */
  public int hashCode() {
    return name.hashCode();
  }

  // ============================ Methods ==============================

  /**
   * Apply this extended I/O operator on the specified arguments.
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

    InputStream  is;
    OutputStream os;

    // Do the deed.
    switch (opcode) {

    case FLUSH:
      {
        Writer out = ((0 == numArgs)? evaluator.getCurrentWriter() :
                      Shell.toOutputPort(o1));
        try {
          out.flush();
        } catch (IOException x) {
          throw new WrappedException("Unable to flush", x, out);
        }
        return Boolean.FALSE;
      }
    case INPUT_STREAM_Q:
      return Cast.box(o1 instanceof InputStream);
    case OUTPUT_STREAM_Q:
      return Cast.box(o1 instanceof OutputStream);
    case OBJECT_INPUT_Q:
      return Cast.box(o1 instanceof ObjectInputStream);
    case OBJECT_OUTPUT_Q:
      return Cast.box(o1 instanceof ObjectOutputStream);
    case OPEN_INPUT_STREAM:
      is = new BufferedInputStream(Shell.createInputStream(Cast.toString(o1)));
      evaluator.markForCleanup(is);
      return is;
    case OPEN_OUTPUT_STREAM:
      os = new BufferedOutputStream(
                                  Shell.createOutputStream(Cast.toString(o1)));
      evaluator.markForCleanup(os);
      return os;
    case OPEN_OBJECT_INPUT:
      is = Shell.createObjectInputStream(
                                   Shell.createInputStream(Cast.toString(o1)));
      evaluator.markForCleanup(is);
      return is;
    case OPEN_OBJECT_OUTPUT:
      os = Shell.createObjectOutputStream(
                                  Shell.createOutputStream(Cast.toString(o1)));
      evaluator.markForCleanup(os);
      return os;
    case FLUSH_STREAM:
      os = Shell.toOutputStream(o1);
      try {
        os.flush();
      } catch (IOException x) {
        throw new WrappedException("Unable to flush", x, os);
      }
      return Boolean.FALSE;
    case CLOSE_INPUT_STREAM:
      is = Shell.toInputStream(o1);
      try {
        is.close();
      } catch (IOException x) {
        throw new WrappedException("Unable to close", x, is);
      }
      evaluator.unmarkForCleanup(is);
      return Boolean.FALSE;
    case CLOSE_OUTPUT_STREAM:
      os = Shell.toOutputStream(o1);
      try {
        os.close();
      } catch (IOException x) {
        throw new WrappedException("Unable to close", x, os);
      }
      evaluator.unmarkForCleanup(os);
      return Boolean.FALSE;
    case READ_BYTE:
      is = Shell.toInputStream(o1);
      try {
        int i = is.read();
        if (-1 == i) { return Format.EOF; } else { return new Byte((byte)i); }
      } catch (IOException x) {
        throw new WrappedException("Unable to read from", x, is);
      }
    case PEEK_BYTE:
      is = Shell.toInputStream(o1);
      if (! is.markSupported()) {
        throw new BadArgumentException("Mark/reset not supported by", is);
      }
      try {
        is.mark(1);
        int i = is.read();
        is.reset();
        if (-1 == i) { return Format.EOF; } else { return new Byte((byte)i); }
      } catch (IOException x) {
        throw new WrappedException("Unable to peek into", x, is);
      }
    case BYTE_READY_Q:
      is = Shell.toInputStream(o1);
      try {
        int i = is.available();
        if (0 < i) { return new Integer(i); } else { return Boolean.FALSE; }
      } catch (IOException x) {
        throw new WrappedException("Unable to access", x, is);
      }
    case WRITE_BYTE:
      try {
        Shell.toOutputStream(o2).write(Cast.toByte(o1));
      } catch (IOException x) {
        throw new WrappedException("Unable to write to", x, o2);
      }
      return Boolean.FALSE;
    case READ_OBJECT:
      try {
        return Shell.toObjectInputStream(o1).readObject();
      } catch (ClassNotFoundException x) {
        throw new WrappedException("Unable to find class", x);
      } catch (IOException x) {
        throw new WrappedException("Unable to read from", x, o1);
      }
    case WRITE_OBJECT:
      try {
        Shell.toObjectOutputStream(o2).writeObject(o1);
      } catch (IOException x) {
        throw new WrappedException("Unable to write to", x, o2);
      }
      return Boolean.FALSE;
    default:
      throw new Bug("Invalid opcode " + opcode +
                    " for extended I/O operator " + toString());
    }
  }

  // ========================== Installation ===========================

  /**
   * Install the extended I/O operators in the specified environment.
   *
   * @param   env     The environment to install the extended I/O
   *                  operators into.
   */
  public static void install(Environment env) {
    add(env, "flush",                FLUSH,                 0,  1);
    add(env, "input-stream?",        INPUT_STREAM_Q,        1,  1);
    add(env, "output-stream?",       OUTPUT_STREAM_Q,       1,  1);
    add(env, "object-input?",        OBJECT_INPUT_Q,        1,  1);
    add(env, "object-output?",       OBJECT_OUTPUT_Q,       1,  1);
    add(env, "open-input-stream",    OPEN_INPUT_STREAM,     1,  1);
    add(env, "open-output-stream",   OPEN_OUTPUT_STREAM,    1,  1);
    add(env, "open-object-input",    OPEN_OBJECT_INPUT,     1,  1);
    add(env, "open-object-output",   OPEN_OBJECT_OUTPUT,    1,  1);
    add(env, "flush-stream",         FLUSH_STREAM,          1,  1);
    add(env, "close-input-stream",   CLOSE_INPUT_STREAM,    1,  1);
    add(env, "close-output-stream",  CLOSE_OUTPUT_STREAM,   1,  1);
    add(env, "read-byte",            READ_BYTE,             1,  1);
    add(env, "peek-byte",            PEEK_BYTE,             1,  1);
    add(env, "byte-ready?",          BYTE_READY_Q,          1,  1);
    add(env, "write-byte",           WRITE_BYTE,            2,  2);
    add(env, "read-object",          READ_OBJECT,           1,  1);
    add(env, "write-object",         WRITE_OBJECT,          2,  2);
  }

  /**
   * Create a new extended I/O operator as specified and add a binding
   * for it to the specified environment.
   *
   * @param   env       The environment for the new extended I/O operator.
   * @param   name      The name of the new extended I/O operator.
   * @param   opcode    The opcode of the new extended I/O operator.
   * @param   minArgs   The non-negative minimum number of arguments for
   *                    the new extended I/O operator.
   * @param   maxArgs   The non-negative maximum number of arguments for
   *                    the new extended I/O operator, or -1 if it takes
   *                    an unlimited maximum number of arguments.
   */
  private static void add(Environment env, String name, int opcode,
                          int minArgs, int maxArgs) {
    name                  = name.intern();
    Symbol              s = Symbol.intern(name);
    ExtendedInputOutput v = new ExtendedInputOutput(name, opcode,
                                                    minArgs, maxArgs);

    env.bind(s, v);
  }

}
