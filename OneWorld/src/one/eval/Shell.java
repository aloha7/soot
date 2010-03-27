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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StreamCorruptedException;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownServiceException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Iterator;

import one.world.Build;

/**
 * Implementation of the read-eval-print loop. This class implements
 * the basic read-eval-print loop for <i>eval</i>. Based on this
 * read-eval-print loop, it supports a <code>main()</code> method that
 * runs the eval-print-loop in the command console. Additionally, it
 * defines reasonable defaults for creating evaluators as well as
 * several helper methods for working with byte, object, as well as
 * character streams.
 *
 * @author   &copy; Copyright 1998-2000 by Robert Grimm.
 *           Written by <a href="mailto:rgrimm@alum.mit.edu">Robert
 *           Grimm</a>.
 * @version  1.0
 */
public final class Shell {

  private Shell() {
    // Nothing to construct.
  }

  // ==================================================================
  //                           Encoding
  // ==================================================================

  /**
   * Return the current encoding for newly created readers and
   * writers.  This method retrieves the "<code>eval.encoding</code>"
   * system property and returns it. If the property does not exist,
   * this method returns the default encoding (which is
   * "<code>UTF8</code>").
   *
   * @return  The current encoding.
   */
  public static String getEncoding() {
    return System.getProperty("eval.encoding", "UTF8");
  }

  // ==================================================================
  //                         Stream Creation
  // ==================================================================

  /**
   * Create a new input stream for the resource with the specified
   * name. The resource name is interpreted as a URL if it starts with
   * "<code>http:</code>", "<code>ftp:</code>", "<code>file:</code>",
   * or "<code>jar:</code>", and otherwise as a file name.
   *
   * @param   name       The name of the resource.
   * @return             An input stream for the specified resource.
   * @throws  WrappedException
   *                     Signals that the specified resources does not
   *                     exist, or that read access to the specified
   *                     resource has been denied.
   */
  public static InputStream createInputStream(String name)
    throws WrappedException {

    InputStream in;

    // Open input stream.

    if (name.startsWith("http:") ||                          // URLs.
        name.startsWith("ftp:") ||
        name.startsWith("file:") ||
        name.startsWith("jar:")) {

      try {
        URLConnection connection = (new URL(name)).openConnection();
        connection.setAllowUserInteraction(false);
        // The defaults for doInput and doOutput are just fine.
        connection.connect();
        in = connection.getInputStream();

      } catch (MalformedURLException x) {
        throw new WrappedException("Malformed URL", x, name);

      } catch (UnknownServiceException x) {
        throw new WrappedException("Read access not supported for", x, name);

      } catch (IOException x) {
        throw new WrappedException("Unable to read from", x, name);
      }

    } else {                                                 // Files.
      try {
        in = new FileInputStream(name);

      } catch (SecurityException x) {
        throw new WrappedException("Read access denied for", x, name);

      } catch (FileNotFoundException x) {
        throw new WrappedException("File \"" + name + "\" not found", x);
      }
    }

    return in;
  }

  /**
   * Create a new object input stream for the specified input stream.
   * In the case of an exceptional condition, this method tries to
   * close the specified input stream.
   *
   * @param   in  The input stream for the new object input stream.
   * @return      A new object input stream, reading from the specified
   *              input stream.
   * @throws  WrappedException
   *              Signals that the specified input stream cannot be
   *              treated as an object input stream or cannot be read
   *              from.
   */
  public static ObjectInputStream createObjectInputStream(InputStream in)
    throws WrappedException {
    
    ObjectInputStream in2;

    try {
      in2 = new ObjectInputStream(in);
    } catch (StreamCorruptedException x) {
      try { in.close(); } catch (IOException xx) { /* Ignore. */ }
      throw new WrappedException("Not a valid object input stream", x, in);
    } catch (IOException x) {
      try { in.close(); } catch (IOException xx) { /* Ignore. */ }
      throw new WrappedException("Unable to read from", x, in);
    }

    return in2;
  }

  /**
   * Create a new reader with the specified encoding for the specified
   * input stream. The newly created reader is guaranteed to support
   * {@link java.io.Reader#mark(int)} and {link
   * java.io.Reader#reset()}.
   *
   * @param   in         The input stream for the new reader.
   * @param   encoding   The character encoding for the new reader.
   * @return             A new reader with the specified encoding for
   *                     the specified input stream.
   * @throws  WrappedException
   *                     Signals that the specified encoding is not
   *                     supported.
   */
  public static Reader createReader(InputStream in, String encoding)
    throws WrappedException {

    try {
      return new BufferedReader(new InputStreamReader(in, encoding));
    } catch (UnsupportedEncodingException x) {
      throw new WrappedException("\"" + encoding + "\" encoding not supported",
                                 x);
    }
  }

  /**
   * Create a new output stream for the resource with the specified
   * name.  The resource name is interpreted as a URL if it starts
   * with "<code>http:</code>", "<code>ftp:</code>",
   * "<code>file:</code>", or "<code>jar:</code>", and otherwise as a
   * file name.
   *
   * @param   name       The name of the resource.
   * @return             An output stream for the specified
   *                     resource.
   * @throws  WrappedException
   *                     Signals that the specified resources does not
   *                     exist, or that write access to the specified
   *                     resource has been denied.
   */
  public static OutputStream createOutputStream(String name)
    throws WrappedException {

    OutputStream out;

    // Open output stream.

    if (name.startsWith("http:") ||                          // URLs.
        name.startsWith("ftp:") ||
        name.startsWith("file:") ||
        name.startsWith("jar:")) {

      try {
        URLConnection connection = (new URL(name)).openConnection();
        connection.setAllowUserInteraction(false);
        connection.setDoInput(false);
        connection.setDoOutput(true);
        connection.connect();
        out = connection.getOutputStream();

      } catch (MalformedURLException x) {
        throw new WrappedException("Malformed URL", x, name);

      } catch (UnknownServiceException x) {
        throw new WrappedException("Write access not supported for", x, name);

      } catch (IOException x) {
        throw new WrappedException("Unable to write to", x, name);
      }

    } else {                                                 // Files.
      try {
        out = new FileOutputStream(name);

      } catch (SecurityException x) {
        throw new WrappedException("Write access denied for", x, name);

      } catch (FileNotFoundException x) {
        throw new WrappedException("File \"" + name + "\" not found", x);
      }
    }

    return out;
  }

  /**
   * Create a new object output stream for the specified output stream.
   * In the case of an exceptional condition, this method tries to
   * close the specified output stream.
   *
   * @param   in  The output stream for the new object output stream.
   * @return      A new object output stream, writing to the specified
   *              output stream.
   * @throws  WrappedException
   *              Signals that the specified output stream cannot be
   *              written to.
   */
  public static ObjectOutputStream createObjectOutputStream(OutputStream out)
    throws WrappedException {

    ObjectOutputStream out2;

    try {
      out2 = new ObjectOutputStream(out);
    } catch (IOException x) {
      try { out.close(); } catch (IOException xx) { /* Ignore. */ }
      throw new WrappedException("Unable to write to", x, out);
    }

    return out2;
  }

  /**
   * Create a new writer with the specified encoding for the specified
   * output stream.
   *
   * @param   out        The output stream for the new writer.
   * @param   encoding   The character encoding for the new writer.
   * @return             A new writer with the specified encoding for
   *                     the specified output stream.
   * @throws  WrappedException
   *                     Signals that the specified encoding is not
   *                     supported.
   */
  public static Writer createWriter(OutputStream out, String encoding)
    throws WrappedException {

    try {
      return new BufferedWriter(new OutputStreamWriter(out, encoding));
    } catch (UnsupportedEncodingException x) {
      throw new WrappedException("\"" + encoding + "\" encoding not supported",
                                 x);
    }
  }

  // ==================================================================
  //                              Casts
  // ==================================================================

  /**
   * Cast the specified object to an input stream.
   *
   * @param   o  The object to cast.
   * @return     The specified object as an input stream.
   * @throws  BadTypeException
   *             Signals that <code>o</code> is not an input stream.
   */
  public static InputStream toInputStream(Object o) throws BadTypeException {
    if (o instanceof InputStream) {
      return (InputStream)o;
    } else {
      throw new BadTypeException("Not an input stream", o);
    }
  }

  /**
   * Cast the specified object to an object input stream.
   *
   * @param   o  The object to cast.
   * @return     The specified object as an object input stream.
   * @throws  BadTypeException
   *             Signals that <code>o</code> is not an object input
   *             stream.
   */
  public static ObjectInputStream toObjectInputStream(Object o)
    throws BadTypeException {

    if (o instanceof ObjectInputStream) {
      return (ObjectInputStream)o;
    } else {
      throw new BadTypeException("Not an object input stream", o);
    }
  }

  /**
   * Cast the specified object to an input port, that is, reader.
   *
   * @param   o  The object to cast.
   * @return     The specified object as a reader.
   * @throws  BadTypeException
   *             Signals that <code>o</code> is not an input port.
   */
  public static Reader toInputPort(Object o) throws BadTypeException {
    if (o instanceof Reader) {
      return (Reader)o;
    } else {
      throw new BadTypeException("Not an input port", o);
    }
  }

  /**
   * Cast the specified object to an output stream.
   *
   * @param   o  The object to cast.
   * @return     The specified object as an output stream.
   * @throws  BadTypeException
   *             Signals that <code>o</code> is not an output stream.
   */
  public static OutputStream toOutputStream(Object o) throws BadTypeException {
    if (o instanceof OutputStream) {
      return (OutputStream)o;
    } else {
      throw new BadTypeException("Not an output stream", o);
    }
  }

  /**
   * Cast the specified object to an object output stream.
   *
   * @param   o  The object to cast.
   * @return     The specified object as an object output stream.
   * @throws  BadTypeException
   *             Signals that <code>o</code> is not an object output
   *             stream.
   */
  public static ObjectOutputStream toObjectOutputStream(Object o)
    throws BadTypeException {

    if (o instanceof ObjectOutputStream) {
      return (ObjectOutputStream)o;
    } else {
      throw new BadTypeException("Not an object output stream", o);
    }
  }

  /**
   * Cast the specified object to an output port, that is, a writer.
   *
   * @param   o  The object to cast.
   * @return     The specified object as a writer.
   * @throws  BadTypeException
   *             Signals that <code>o</code> is not an output port.
   */
  public static Writer toOutputPort(Object o) throws BadTypeException {
    if (o instanceof Writer) {
      return (Writer)o;
    } else {
      throw new BadTypeException("Not an output port", o);
    }
  }

  // ==================================================================
  //                         Read-eval-print loop
  // ==================================================================

  /**
   * Return the current shell verbosity. This methods retrieves the
   * "<code>eval.shell.verbosity</code>" system property and parses it
   * as an integer. If the property does not exist, or if its value is
   * not a valid integer format, or if the parsed integer is not 0, 1,
   * or 2, this method returns the default shell verbosity (which is
   * 1).
   *
   * @return  The current shell verbosity.
   */
  public static int getVerbosity() {
    int    result = 1;
    String s      = System.getProperty("eval.shell.verbosity");
    if (null != s) {
      try {
        result = Integer.parseInt(s);
      } catch (NumberFormatException x) {
        // Ignore.
      }
    }

    if ((0 > result) || (2 < result)) {
      result = 1;
    }

    return result;
  }

  /**
   * Run a read-eval-print loop. This method reads one expression at
   * at time from the specified reader and evaluates it using the
   * specified evaluator. The result of evaluating each expression is
   * printed to the specified writer if <code>verbose</code> is
   * <code>true</code>. The read-eval-print loop terminates normally
   * when reaching the end-of-file of the specified reader and returns
   * the value of the last expression that was evaluated.
   *
   * <p>If <code>catchThreshold</code> is negative, evaluator
   * exceptions are caught by the read-eval-print loop. If
   * <code>catchThreshold</code> is non-negative, as many evaluator
   * exceptions are caught by the read-eval-print loop, and the
   * read-eval-print loop terminates exceptionally with the last
   * thrown exception after that threshold has been exhausted.</p>
   *
   * <p>If <code>catchRead</code> is <code>true</code>, evaluator
   * exceptions encountered when reading an expression from the
   * specified reader count towards the catch threshold. If
   * <code>catchRuntime</code> is <code>true</code>, runtime
   * exceptions that are not an exit exception are treated as
   * evaluator exceptions.</p>
   *
   * <p>Like values, exceptions caught by the read-eval-print loop are
   * printed to the specified writer if <code>verbose</code> is
   * <code>true</code>.</p>
   *
   * @see     EvaluatorException
   * @see     ExitException
   *
   * @param   in    The reader to read expressions from.
   * @param   eval  The evaluator to evaluate expressions with.
   * @param   out   The writer to write results to.
   * @param   verbose
   *                <code>true</code> iff values and exceptions
   *                should be written to the specified writer.
   * @param   catchThreshold
   *                The non-negative number of evaluator exceptions
   *                to be caught by the read-eval-print loop, or
   *                any negative number if any number of evalutor
   *                exceptions should be caught by the
   *                read-eval-print loop.
   * @param   catchRead
   *                <code>true</code> iff evaluator exceptions
   *                encountered when reading an expression should
   *                be caught by the read-eval-print loop.
   * @param   catchRuntime
   *                <code>true</code> iff runtime exceptions that
   *                are not an exit exception should be caught
   *                by the read-eval-print loop.
   * @return        The result of evaluating the last expression.
   * @throws  EvaluatorException
   *                Signals an exceptional condition during the
   *                execution of the read-eval-print loop.
   */
  public static Object loop(Reader in, Evaluator eval, Writer out,
                            boolean verbose, int catchThreshold,
                            boolean catchRead, boolean catchRuntime)
    throws EvaluatorException {

    Format  format     = eval.getFormat();
    String  comment    = format.getCommentStart();
    String  eol        = Format.getLineSeparator();
    Object  result     = Format.EOF;
    boolean catchSome  = (0 <= catchThreshold);
    int     catchCount = 0;


    try {

      // Loop.
      while (true) {

        // Read expression.
        Object expr;
        try {
          expr = format.read(in);

        } catch (EvaluatorException x) {
          // A bad argument exception means in does not support mark/reset.
          if (x instanceof BadArgumentException) {
            throw x;

          } else if (catchSome && (catchCount >= catchThreshold)) {
            throw x;

          } else {
            catchCount++;

            if (verbose) {
              // Write out error message.
              int verbosity = getVerbosity();
              format.write(x, out, (1 <= verbosity));
              if (2 <= verbosity) {
                out.write(comment);
                out.write(eol);
                eval.printBackTrace(out);
              }
              out.write(eol);
              out.flush();
            }

            continue;
          }

        } catch (RuntimeException x) {
          if (x instanceof ExitException) {
            throw x;
          } else if (catchRuntime) {
            if (catchSome && (catchCount >= catchThreshold)) {
              throw x;

            } else {
              catchCount++;

              if (verbose) {
                // Write out error message.
                int verbosity = getVerbosity();
                format.write(x, out, (1 <= verbosity));
                if (2 <= verbosity) {
                  out.write(comment);
                  out.write(eol);
                  eval.printBackTrace(out);
                }
                out.write(eol);
                out.flush();
              }

              continue;
            }

          } else {
            throw x;
          }
        }
        
        // End-of-file?
        if (Format.EOF == expr) return result;
        
        // Evaluate expression.
        try {
          result = eval.eval(expr);
        } catch (EvaluatorException x) {
          if (catchSome && (catchCount >= catchThreshold)) {
            throw x;

          } else {
            catchCount++;

            if (verbose) {
              // Write out error message.
              int verbosity = getVerbosity();
              format.write(x, out, (1 <= verbosity));
              if (2 <= verbosity) {
                out.write(comment);
                out.write(eol);
                eval.printBackTrace(out);
              }
              out.write(eol);
              out.flush();
            }

            continue;
          }

        } catch (RuntimeException x) {
          if (x instanceof ExitException) {
            throw x;

          } else if (catchRuntime) {
            if (catchSome && (catchCount >= catchThreshold)) {
              throw x;

            } else {
              catchCount++;

              if (verbose) {
                // Write out error message.
                int verbosity = getVerbosity();
                format.write(x, out, (1 <= verbosity));
                if (2 <= verbosity) {
                  out.write(comment);
                  out.write(eol);
                  eval.printBackTrace(out);
                }
                out.write(eol);
                out.flush();
              }

              continue;
            }

          } else {
            throw x;
          }
        }
        
        // Print result.
        if (verbose) {
          out.write(comment);
          out.write(" Value: ");
          format.write(result, out, true);
          out.write(eol);
          out.write(eol);
          out.flush();
        }
      } // Continue loop.

    } catch (IOException x) {
      throw new WrappedException("Unable to write to", x, out);
    }
  }

  // ==================================================================
  //                          Shell Defaults
  // ==================================================================

  /** The default environment. */
  private static Environment env;

  /** The default writer. */
  private static Writer      out;

  /** The default reader. */
  private static Reader      in;

  /** The default format. */
  private static Format      format;

  static {
    env    = new HashEnvironment(TopLevelEnvironment.EVAL);
    out    = new BufferedWriter(new OutputStreamWriter(System.out));
    in     = new BufferedReader(new InputStreamReader(System.in));
    format = new SchemeFormat();
  }

  /**
   * Get the current default environment. The initial default
   * environment is the eval environment wrapped in a hash
   * environment.
   *
   * @see     TopLevelEnvironment#EVAL
   * @see     HashEnvironment
   * 
   * @return  The current default environment.
   */
  public static Environment getEnvironment() {
    return env;
  }

  /**
   * Set the current default environment. This method signals a
   * security exception if the caller does not have the
   * "<code>setEnvironment</code>" shell permission.
   *
   * @see     ShellPermission
   *
   * @param   env  The new default environment.
   * @throws  NullPointerException
   *               Signals that <code>env</code> is <code>null</code>.
   * @throws  SecurityException
   *               Signals that the caller does not have
   *               the appropriate shell permission.
   */
  public static void setEnvironment(Environment env) {
    if (null == env) {
      throw new NullPointerException("Null environment");
    }

    SecurityManager security = System.getSecurityManager();
    if (null != security) {
      security.checkPermission(ShellPermission.SET_ENVIRONMENT);
    }

    Shell.env = env;
  }

  /**
   * Get the current default writer. The initial default writer is a
   * buffered writer writing to <code>System.out</code>.
   * 
   * @return  The current default writer.
   */
  public static Writer getWriter() {
    return out;
  }

  /**
   * Set the current default writer. This method signals a security
   * exception if the caller does not have the
   * "<code>setWriter</code>" shell permission.
   *
   * @see     ShellPermission
   *
   * @param   out  The new default writer.
   * @throws  NullPointerException
   *               Signals that <code>out</code> is <code>null</code>.
   * @throws  SecurityException
   *               Signals that the caller does not have
   *               the appropriate shell permission.
   */
  public static void setWriter(Writer out) {
    if (null == out) {
      throw new NullPointerException("Null writer");
    }

    SecurityManager security = System.getSecurityManager();
    if (null != security) {
      security.checkPermission(ShellPermission.SET_WRITER);
    }

    Shell.out = out;
  }

  /**
   * Get the current default reader. The initial default reader is a
   * buffered reader reading from <code>System.in</code>.
   * 
   * @return  The current default reader.
   */
  public static Reader getReader() {
    return in;
  }

  /**
   * Set the current default reader. This method signals a security
   * exception if the caller does not have the
   * "<code>setReader</code>" shell permission.
   *
   * @see     ShellPermission
   *
   * @param   in   The new default reader.
   * @throws  NullPointerException
   *               Signals that <code>in</code> is <code>null</code>.
   * @throws  SecurityException
   *               Signals that the caller does not have
   *               the appropriate shell permission.
   */
  public static void setReader(Reader in) {
    if (null == in) {
      throw new NullPointerException("Null reader");
    }

    SecurityManager security = System.getSecurityManager();
    if (null != security) {
      security.checkPermission(ShellPermission.SET_READER);
    }

    Shell.in = in;
  }

  /**
   * Get the current default format. The initial default format is a
   * scheme format.
   *
   * @see     SchemeFormat
   * 
   * @return  The current default format.
   */
  public static Format getFormat() {
    return format;
  }

  /**
   * Set the current default format. This method signals a security
   * exception if the caller does not have the
   * "<code>setFormat</code>" shell permission.
   *
   * @see     ShellPermission
   *
   * @param   format  The new default format.
   * @throws  NullPointerException
   *                  Signals that <code>format</code> is <code>null</code>.
   * @throws  SecurityException
   *                  Signals that the caller does not have
   *                  the appropriate shell permission.
   */
  public static void setFormat(Format format) {
    if (null == format) {
      throw new NullPointerException("Null format");
    }

    SecurityManager security = System.getSecurityManager();
    if (null != security) {
      security.checkPermission(ShellPermission.SET_FORMAT);
    }

    Shell.format = format;
  }

  // ==================================================================
  //                        Command line shell
  // ==================================================================

  /**
   * Run a simple, command line based read-eval-print loop for
   * <i>eval</i>. The read-eval-print loop reads expressions from
   * standard input, evaluates them, and prints the results to
   * standard output. An expression may span several input lines, and
   * several expressions may appear on one input line. Hitting return
   * commits typed input, and, even if a particular user shell
   * supports editing input from previously types lines, such changes
   * are not seen by the read-eval-print loop.
   *
   * @param   args  The arguments for this invocation of <i>eval</i>.
   *                The <code>-help</code> option as the only argument
   *                to this method prints an explanation of available
   *                options to standard output.
   */
  public static void main(String[] args) {

    // ========================= Initialize properties.
    
    AccessController.doPrivileged(new PrivilegedAction() {
        public Object run() {
          System.setProperty("eval.version",       "1.0b2");
          System.setProperty("eval.version.major", "1");
          System.setProperty("eval.version.minor", "0b2");
          return null;
        }
      });


    // ========================= Verify VM.

    if (! one.util.JavaPlatform.includesJDK12()) {
      System.out.println("eval " + System.getProperty("eval.version") +
                         " - (C) 1998-2000 by Robert Grimm, all rights " +
                         "reserved.");
      System.out.println("Built " + Build.get() + " GMT");
      System.out.println();
      System.out.println("   eval requires a Java virtual machine which " +
                         "complies with the Java 2");
      System.out.println("   platform specification!");
      return;
    }


    // ========================= Parse command line flags.

    boolean   help     = false;  // Print help info.
    boolean   version  = false;  // Print version info.
    ArrayList files    = null;   // Files to load.

    if (null != args) {
      for (int i=0; i<args.length; i++) {

        if (args[i].startsWith("--")) {          // Options.
          if (args[i].equals("--help")) {
            help = true;

          } else if (args[i].equals("--version")) {
            version = true;

          } else {
            // Unrecognized option.
            help = true;
          }

        } else if (args[i].startsWith("-")) {
          // Tell people that we don't support the old style - options.
          help = true;

        } else {                                 // File to load.
          if (null == files) {
            files = new ArrayList();
          }
          files.add(args[i]);
        }
      }
    }


    // ========================= Set up.

    String      comment = format.getCommentStart();
    String      eol     = Format.getLineSeparator();
    Evaluator   eval    = new Evaluator(env, format, in, out);
    int         exit    = -1;


    // ========================= Print header.

    try {
      out.write(comment);
      out.write(" eval ");
      out.write(System.getProperty("eval.version"));
      out.write(" - (C) 1998-2000 by Robert Grimm, all rights reserved.");
      out.write(eol);
      out.write(comment);
      out.write(" Built ");
      out.write(Build.get());
      out.write(" GMT");
      out.write(eol);
      out.write(comment);
      out.write(eol);
      out.write(eol);
      out.flush();

      if (help) {
        out.write(comment);
        out.write(" The following command line options are recognized:");
        out.write(eol);
        out.write(comment);
        out.write(eol);
        out.write(comment);
        out.write(" --version     Print header and terminate.");
        out.write(eol);
        out.write(comment);
        out.write(" --help        Print this help message and terminate.");
        out.write(eol);
        out.write(comment);
        out.write(" <filename>    Load file before read-eval-print loop.");
        out.write(eol);
        out.write(comment);
        out.write(eol);
        out.write(eol);
        out.flush();
        return;

      } else if (version) {
        return;
      }

    } catch (IOException x) {
      System.err.println(comment + " Error during output: " + x.toString());
      return;
    }


    // ========================= Evaluation.

    try {
      // Load files.
      if (null != files) {
        Iterator iter = files.iterator();

        while (iter.hasNext()) {
          String name = (String)iter.next();

          out.write(comment);
          out.write(" Loading \"");
          out.write(name);
          out.write("\"");
          out.write(eol);
          out.flush();

          Object result;

          try {
            Reader in2 = createReader(createInputStream(name), getEncoding());
            result     = loop(in2, eval, out, false, 0, false, false);

          } catch (EvaluatorException x) {
            int verbosity = getVerbosity();
            format.write(x, out, (1 <= verbosity));
            if (2 <= verbosity) {
              out.write(comment);
              out.write(eol);
              eval.printBackTrace(out);
            }
            out.write(eol);
            out.flush();
            continue;

          } catch (RuntimeException x) {
            if (x instanceof ExitException) {
              throw x;
            }

            int verbosity = getVerbosity();
            format.write(x, out, (1 <= verbosity));
            if (2 <= verbosity) {
              out.write(comment);
              out.write(eol);
              eval.printBackTrace(out);
            }
            out.write(eol);
            out.flush();
            continue;
          }

          out.write(comment);
          out.write(" Value: ");
          format.write(result, out, true);
          out.write(eol);
          out.flush();
        }

        out.write(eol);
        out.flush();

        files = null; // Let GC do its magic.
      }

      // Run actual read-eval-print loop.
      loop(in, eval, out, true, -1, true, true);

    } catch (IOException x) {
      System.err.println(comment + " Error during output: " + x.toString());

    } catch (WrappedException x) {
      System.err.println(comment + " Error during output: " +
                         x.getThrowable().toString());

    } catch (EvaluatorException x) {
      System.err.println(comment + " Evaluator exception: " + x.toString());

    } catch (ExitException x) {
      try {
        out.write(comment);
        out.write(" Happy, happy, joy, joy!");
        out.write(eol);
        out.flush();
      } catch (IOException xx) {
        System.err.println(comment + " Error during output: " + xx.toString());
      }
      exit = x.getStatus();

    } finally {
      eval.resetCurrentReaderWriter();
      eval.cleanUpMarkedStreams();
    }

    try {
      System.exit(exit);
    } catch (SecurityException x) {
      // Ignore.
    }
  }

}
