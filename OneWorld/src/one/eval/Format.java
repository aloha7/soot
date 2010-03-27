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

/**
 * The abstract base class for formatting language-specific values and
 * expressions. 
 *
 * @author   &copy; Copyright 1998-2000 by Robert Grimm.
 *           Written by <a href="mailto:rgrimm@alum.mit.edu">Robert
 *           Grimm</a>.
 * @version  1.0
 */
public abstract class Format implements java.io.Serializable {

  // ============================ End-of-File =============================

  /** Implementation of the end-of-file object. */
  private static class EndOfFile implements java.io.Serializable {

    /** The unique end-of-file object. */
    static final EndOfFile VALUE = new EndOfFile();

    /** Create a new end-of-file object. */
    private EndOfFile() {
      // Nothing to construct.
    }

    /**
     * Resolve this end-of-file object during deserialization.
     *
     * @return <code>VALUE</code>.
     */
    private Object readResolve() throws java.io.ObjectStreamException {
      return VALUE;
    }

    /**
     * Return a string representation of this end-of-file object.
     *
     * @return  "<code>#[EOF]</code>".
     */
    public String toString() {
      return "#[EOF]";
    }
  }

  /**
   * The end-of-file object. The end-of-file object is guaranteed to
   * be serializable as well as to be unique within a given Java
   * Virtual Machine.
   */
  public static final Object EOF            = EndOfFile.VALUE;

  // =============================== Fields ===============================

  /**
   * The start sequence for a comment that is terminated by a line
   * separator. Initially, "<code>;</code>". Subclasses of this class
   * may need to reassign this field in their constructor.
   * 
   * @serial  Must not be <code>null</code>.
   */
  protected String           commentStart = ";";

  // ================================= I/O ================================

  /**
   * Read an object from the specified reader. Reads the external
   * representation of the next object from the specified reader and
   * returns that object. Whitespace and comments as appropriate for a
   * particular format must be ignored. Returns <code>EOF</code>, if
   * the end of file position is reached before encountering the
   * external representation of an object.
   *
   * @see     #EOF
   *
   * @param   in  The reader to read the external representation of
   *              an object from.
   * @return      The corresponding object, or <code>EOF</code> if the
   *              end of file position is reached before encountering
   *              the external representation of an object.
   * @throws  BadArgumentException
   *              Signals that the specified reader does not provide
   *              necessary functionality, for example, if it does not
   *              support {@link java.io.Reader#mark(int)} and
   *              {@link java.io.Reader#reset()}.
   * @throws  BadFormatException
   *              Signals that the external representation of an object
   *              is not as expected.
   * @throws  WrappedException
   *              Signals an exceptional condition when accessing the
   *              specified reader.
   */
  public abstract Object read(Reader in)
    throws BadArgumentException, BadFormatException, WrappedException;

  /**
   * Write the external representation of the specified object to the
   * specified writer. If <code>quoted</code> is <code>false</code>,
   * characters and strings must be written verbatim, that is, they
   * must not be quoted or escaped. The meaning of the
   * <code>quoted</code> flag for other objects is format-dependent.
   *
   * <p>Concrete implementations of this class must use the current
   * recursion limit to limit recursive invocations of this method
   * (or, the actual method that writes objects to the specified
   * writer).</p>
   *
   * @see     #getRecursionLimit()
   *
   * @param   o       The object whose external representation to
   *                  write.
   * @param   out     The writer to write the external representation
   *                  to.
   * @param   quoted  <code>false</code> iff characters and strings
   *                  are to be written verbatim.
   * @throws  WrappedException
   *                  Signals an exceptional condition when accessing
   *                  the specified writer.
   */
  public abstract void write(Object o, Writer out, boolean quoted)
    throws WrappedException;

  /**
   * Write a canoncial description of the specified throwable to the
   * specified writer. The message has the form
   * <blockquote><pre>
   * &lt<i>cs</i>&gt; &lt;<i>descriptor</i>&gt - &lt;<i>message</i>&gt;: &lt;<i>cause</i>&gt;&lt;<i>nl</i>&gt;
   * </pre></blockquote> where <i>descriptor</i> is a short
   * description of the type of throwable, <i>message</i> is the
   * detail message of the specified throwable, and <i>cause</i> is
   * the cause of the specified throwable if that throwable is an
   * evaluator exception. If a throwable has no detail message, the
   * message is omitted. If an evaluator exception has no cause, the
   * cause is omitted.
   *
   * <p>If <code>verbose</code> is <code>true</code> and the specified
   * throwable is a <code>WrappedException</code>, the short
   * description of the type of throwable is followed by the
   * parenthesized class name of the contained exception, and if the
   * contained exception has a detail message, that detail message is
   * printed in a second line: <blockquote><pre>
   * &lt;<i>cs</i>&gt;    &lt;<i>message</i>&gt;&lt;<i>nl</i>&gt;
   * </pre></blockquote></p>
   *
   * <p><i>cs</i> is the value of <code>commentStart</code>, and
   * <i>nl</i> is the current line separator.</p>
   *
   * @see     #commentStart
   *
   * @param   x    The throwable to write a description of.
   * @param   out  The writer to write a description to.
   * @param   verbose
   *               <code>true</code> iff wrapped exceptions are to
   *               be followed with the detail message of the
   *               contained exception.
   * @throws  WrappedException
   *               Signals an exceptional condition when writing
   *               to the specified writer.
   */
  public void write(Throwable x, Writer out, boolean verbose)
    throws WrappedException {
    
    try {
      Throwable xx = null;

      out.write(commentStart);

      if (x instanceof BadPairStructureException) {
        out.write(" Bad pair structure");
      } else if (x instanceof BadArgumentException) {
        out.write(" Bad argument");
      } else if (x instanceof BadFormatException) {
        out.write(" Bad format");
      } else if (x instanceof BadSyntaxException) {
        out.write(" Bad syntax");
      } else if (x instanceof BadTypeException) {
        out.write(" Bad type");
      } else if (x instanceof BindingException) {
        out.write(" Binding exception");
      } else if (x instanceof UserException) {
        out.write(" User exception");
      } else if (x instanceof WrappedException) {
        out.write(" Basic exception");
        xx = ((WrappedException)x).getThrowable();
        if ((null != xx) && verbose) {
          out.write(" (");
          out.write(xx.getClass().getName());
          out.write(')');
        }
      } else if (x instanceof NotAValueException) {
        out.write(" Not a value");
      } else if (x instanceof EvaluatorException) {
        out.write(" General exception");
      } else if (x instanceof RuntimeException) {
        out.write(" Runtime exception (");
        out.write(x.getClass().getName());
        out.write(')');
      } else if (x instanceof Error) {
        out.write(" Error (");
        out.write(x.getClass().getName());
        out.write(')');
      } else {
        out.write(" Unknown exception (");
        out.write(x.getClass().getName());
        out.write(')');
      }

      String s = x.getMessage();
      if (null != s) {
        out.write(" - ");
        out.write(s);
      }

      if (x instanceof EvaluatorException) {
        EvaluatorException ex = (EvaluatorException)x;

        if (ex.hasCausingObject()) {
          out.write(": ");
          write(ex.getCausingObject(), out, true);
        }
      }

      out.write(getLineSeparator());

      if (verbose && (x instanceof WrappedException)) {
        s = xx.getMessage();
        if (null != s) {
          out.write(commentStart);
          out.write("    ");
          out.write(s);
          out.write(getLineSeparator());
        }
      }

      out.flush();

    } catch (IOException xxx) {
      throw new WrappedException("Unable to write to", xxx, out);
    }
  }

  // ============================= Properties =============================

  /**
   * Return the start sequence of a comment that is terminated by a line
   * separator.
   *
   * @return  The string beginning a comment that is terminated by a
   *          line separator.
   */
  public String getCommentStart() {
    return commentStart;
  }

  /**
   * Return the current line separator. This method retrieves the
   * "<code>line.separator</code>" system property and returns it. If
   * the property does not exist, this method returns the default line
   * separator (which is "<code>\n</code>").
   *
   * @return  The current line separator.
   */
  public static String getLineSeparator() {
    return System.getProperty("line.separator", "\n");
  }

  /**
   * Return the current recursion limit. This method retrieves the
   * "<code>eval.recursion.limit</code>" system property and parses it
   * as an integer. If the property does not exist, or if its value is
   * not a valid integer format, this method returns the default
   * recursion limit (which is 50).
   *
   * @see     #write(Object,Writer,boolean)
   *
   * @return  The current recursion limit.
   */
  public static int getRecursionLimit() {
    int    result = 50;
    String s      = System.getProperty("eval.recursion.limit");
    if (null != s) {
      try {
        result = Integer.parseInt(s);
      } catch (NumberFormatException x) {
        // Ignore.
      }
    }

    return result;
  }

}
