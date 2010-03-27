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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import one.util.Bug;

/**
 * Implementation of procedures that interface with the <i>eval</i>
 * runtime environment and that integrate <i>eval</i> with the
 * underlying Java environment. This class defines several procedures
 * to determine floating point non-numbers, to provide access to the
 * <i>eval</i> runtime environment, to suport multi-threading, to
 * handle Java exceptions, to access arbitrary Java objects through
 * Java reflection, and to exit from a read-eval-print loop.
 *
 * <p>This class defines the following extensions to the Scheme
 * standard:</p>
 *
 *
 * <p><b>Non-Numbers</b></p>
 *
 * <p><code>(not-a-number?</code>&nbsp;&nbsp;<i>z</i><code>)</code><br />
 * Returns <code>#t</code> if the specified number is an inexact
 * number in a IEEE flonum format representing NaN. In other words,
 * returns <code>#t</code> if the the specified number is a Java
 * <code>Float</code> or <code>Double</code> representing NaN. It is
 * an error if <i>z</i> is not a number.</p>
 *
 * <p><code>(infinite?</code>&nbsp;&nbsp;<i>z</i><code>)</code><br />
 * Returns <code>#t</code> if the specified number is an inexact
 * number in a IEEE flonum format representing either positive or
 * negative infinity. In other words, returns <code>#t</code> if the
 * specified number is a Java <code>Float</code> or
 * <code>Double</code> representing either positive or negative
 * infinity. It is an error if <i>z</i> is not a number.</p>
 *
 *
 * <p><b>Runtime Environment</b></p>
 *
 * <p><code>(gc)</code><br />
 * Run the garbage collector. <code>Gc</code> returns an unspecified
 * result.</p>
 *
 * <p><code>(current-time)</code><br />
 * Return the current time in milliseconds as returned by
 * <code>java.lang.System.currentTimeMillis()</code>.</p>
 *
 * <p>
 * <code>(property-ref</code>&nbsp;&nbsp;<i>string</i><sub><font size="-2">key</font></sub><code>)</code><br />
 * <code>(property-set!</code>&nbsp;&nbsp;<i>string</i><sub><font size="-2">key</font></sub>&nbsp;&nbsp;<i>string</i><sub><font size="-2">value</font></sub><code>)</code><br />
 * Get and set the value of the system property named
 * <i>string</i><sub><font size="-2">key</font></sub>. Property names
 * are case-sensitive. <code>Property-ref</code> returns the current
 * value of the specified system property, or <code>#f</code> if no
 * such system property exists. <code>Property-set!</code> sets the
 * specified system property to the specified value and returns an
 * unspecified value. The following system properties are used by
 * <i>eval</i>:<ul>
 *
 * <li>"<code>eval.shell.verbosity</code>" determines the verbosity of
 * the error messages printed by the shell's read-eval-print loop. For
 * a verbosity level of 0, a simple message and the cause (if
 * available) are printed. For a verbosity level of 1, the Java class
 * name and detail message of an underlying exception are also printed
 * for wrapped exceptions. For a verbosity level of 2, a backtrace of
 * evaluator contexts is also printed. The default verbosity level is
 * 1. The default verbosity level is used, if the system property does
 * not exist, if its value is not a valid integer format, or if it is
 * not 0, 1, or 2</li>
 *
 * <li>"<code>eval.recursion.limit</code>" determines after how many
 * recursive invocations of <code>write</code> a recursive walk of a
 * Scheme object is terminated and "<code>#[...]</code>" is printed
 * instead of the current sub-object. The default recursion limit is
 * 50. The default recursion limit is used, if the system property
 * does not exist, or if its value is not a valid integer format.</li>
 *
 * <li>"<code>eval.encoding</code>" specifies the character encoding
 * for newly created input and output ports. The default encoding is
 * "<code>UTF8</code>". If the current encoding is not recognized by
 * the Java runtime, attempts to create new input or output ports will
 * fail. The default encoding is used, if the system property does not
 * exist.</li>
 *
 * <li>"<code>line.separator</code>" specifies the current line
 * separator. The default line separator is "<code>\n</code>". The
 * default line separator is ued, if the system property does not
 * exist.</li>
 *
 * <li>"<code>eval.version</code>" specifies the version of
 * <i>eval</i>, such as "<code>1.0.1</code>".</li>
 *
 * <li>"<code>eval.version.major</code>" specifies the major version
 * of <i>eval</i>, such as "<code>1</code>".</li>
 *
 * <li>"<code>eval.version.minor</code>" specifies the minor version
 * of <i>eval</i>, such as "<code>0.1</code>".</li>
 *
 * </ul></p>
 *
 * <p><code>(reset-current-ports)</code><br />
 * Reset the current input and output ports to the original ports for
 * this evaluator, to restore a well-defined state after an error.
 * <code>Reset-current-ports</code> returns an unspecified value.</p>
 *
 *
 * <p><b>Multi-Threading</b></p>
 *
 * <p><code>(fork</code>&nbsp;&nbsp;<i>thunk</i><code>)</code><br />
 * Evaluate <i>thunk</i> as a top-level expression in its own thread
 * of execution and return a promise for this separate
 * evaluation. When the promise is forced for the first time, the
 * separate thread is joined with the current thread, and the value
 * resulting from the evaluation of <i>thunk</i> becomes the value of
 * the promise. If the evaluation of <i>thunk</i> results in an error,
 * forcing the returned promise results in that same error.</p>
 *
 * <p><code>(synchronized</code>&nbsp;&nbsp;<i>obj</i>&nbsp;&nbsp;<i>thunk</i><code>)</code><br />
 * Evalutate <i>thunk</i> while holding the Java monitor associated
 * with <i>obj</i>. <code>Synchronized</code> returns the result of
 * evaluating <i>thunk</i>. It is an error if <i>obj</i> is the empty
 * list, that is, <code>null</code>.</p>
 *
 * <p>Future support for <code>wait</code>, <code>notify</code>, and
 * <code>notify-all</code> is being considered.</p>
 *
 *
 * <p><b>Exceptions</b></p>
 *
 * <p>A note on the use of exceptions in <i>eval</i>: Generally,
 * <i>eval</i> uses the subclasses of {@link EvaluatorException}
 * defined in this package to indicate exceptional conditions. Where
 * appropriate, <i>eval</i> catches internal exceptions, such as an
 * <code>IndexOutOfBoundsException</code> when accessing a vector
 * element or an <code>ArithmeticException</code> when performing
 * division, and throws the appropriate
 * <code>EvaluatorException</code> (usually, a {@link
 * BadArgumentException}) with the corresponding error message and,
 * possibly, cause. However, some internal exceptions, such as an
 * <code>IOException</code> or <code>SecurityException</code> when
 * accessing some port, have no clear-cut cause, may convey important
 * additional information, and are thus wrapped in a {@link
 * WrappedException}.</p>
 * 
 * <p><code>(error</code>&nbsp;&nbsp;<i>message</i><code>)</code><br />
 * <code>(error</code>&nbsp;&nbsp;<i>message</i>&nbsp;&nbsp;<i>obj</i><code>)</code><br />
 * Indicate a user-defined error condition. <i>message</i> is the
 * error message for the error condition, and <i>obj</i>, if
 * specified, is the cause for the error condition. It is an error if
 * <i>message</i> is not a string. <code>Error</code> never
 * returns. The implementation of <code>error</code> throws a new
 * {@link UserException} with the specified detail message and,
 * optionally, the specified cause.<p>
 *
 * <p><code>(catch</code>&nbsp;&nbsp;<i>throwable</i>&nbsp;&nbsp;<i>thunk</i>&nbsp;&nbsp;<i>handler</i><code>)</code><br />
 * Evaluate <i>thunk</i> with the specified <i>handler</i> for the
 * specified type of <i>throwable</i>. If the evaluation of
 * <i>thunk</i> completes successfully, <code>catch</code> returns the
 * result of evaluating <i>thunk</i>. If the evaluation of
 * <i>thunk</i> results in an error of type <i>throwable</i>,
 * <i>handler</i> is invoked on the actual throwable and
 * <code>catch</code> returns the result of this application. If the
 * actual throwable is an instance of <code>WrappedException</code>,
 * both the <code>WrappedException</code> and the throwable wrapped by
 * the wrapped exception are matched against the type of
 * <i>throwable</i>, and the <i>handler</i> is applied on the matching
 * throwable (which is the wrapped throwable if both exceptions
 * match). It is an error if <i>throwable</i> does not specify a Java
 * class that is assignment compatible with
 * <code>java.lang.Throwable</code>.</p>
 *
 *
 * <p><b>Java Reflection</b></p>
 *
 * <p>The following operators provide access to arbitrary Java objects
 * through the use of the Java reflection API. They use the obvious
 * mapping into Java types, using {@link java.lang.Class} for class
 * objects, {@link java.lang.reflect.Constructor} for constructor
 * descriptors, {@link java.lang.reflect.Field} for field descriptors,
 * and {@link java.lang.reflect.Method} for method descriptors.</p>
 *
 * <p>Note that any <i>class</i> argument for the
 * <code>instance?</code>, <code>constructor</code>,
 * <code>field</code>, and <code>method</code> operators can either be
 * a class object or a string describing the name of a Java class as
 * defined for the <code>name->class</code> operator.</p>
 * 
 * <p><code>(class?</code>&nbsp;&nbsp;<i>obj</i><code>)</code><br />
 * Return <code>#t</code> if object <i>obj</i> is a class object.</p>
 *
 * <p><code>(name->class</code>&nbsp;&nbsp;<i>name</i>)</code><br />
 * Return the class object representing the class with name
 * <i>name</i>. The name can either be one of the primitive types, the
 * unqualified name of a class in the <code>java.lang</code> package,
 * or a fully qualified class or interface name, and may be followed
 * by some number of "<code>[]</code>" to indicate an array class of
 * as many dimensions. If the specified class has not been loaded
 * before, it is loaded and initialized. It is an error if no class
 * with the specified name can be found.</p>
 *
 * <p><code>(object->class</code>&nbsp;&nbsp;<i>obj</i>)</code><br />
 * Return the class object representing the class of object
 * <i>obj</i>. It is an error if the specified object is the empty
 * list, that is, <code>null</code>.</p>
 *
 * <p><code>(instance?</code>&nbsp;&nbsp;<i>obj</i>&nbsp;&nbsp;<i>class</i><code>)</code><br >
 * Return <code>#t</code> if object <i>obj</i> is an instance of class
 * <i>class</i> (according to <code>java.lang.Class.isInstance</code>).</p>
 *
 * <p><code>(class->name</code>&nbsp;&nbsp;<i>class</i><code>)</code><br />
 * Return the name of the class <i>class</i>.</p>
 *
 * <p><code>(constructor</code>&nbsp;&nbsp;<i>class</i><sub><font size="-2">1</font></sub>&nbsp;&nbsp;<i>class</i><sub><font size="-2">2</font></sub>&nbsp;...<code>)</code><br />
 * Return a constructor descriptor for the constructor in class
 * <i>class</i><sub><font size="-2">1</font></sub> which accepts the
 * specified parameter types. It is an error if the specified class
 * has no constructor with the specified parameter types.</p>
 *
 * <p><code>(new</code>&nbsp;&nbsp;<i>constructor</i>&nbsp;&nbsp;<i>obj</i>&nbsp;...<code>)</code><br />
 * Invoke the constructor described by constructor descriptor
 * <i>constructor</i> on the specified arguments and return the newly
 * created Java object. It is an error if any of the arguments cannot
 * be converted to a type that is assignment compatible with the type
 * of the corresponding parameter.</p>
 *
 * <p><code>(field</code>&nbsp;&nbsp;<i>class</i>&nbsp;&nbsp;<i>name</i><code>)</code><br />
 * Return a field descriptor for the field with name <i>name</i> in
 * class <i>class</i>. The field can either be an instance field or be
 * a static field. It is an error if the specified class has no field
 * with the specified name.</p>
 *
 * <p><code>(field-ref</code>&nbsp;&nbsp;<i>field</i>&nbsp;&nbsp;<i>obj</i><code>)</code><br />
 * Return the value of the field described by field descriptor
 * <i>field</i> for object <i>obj</i>. The specified object may be the
 * empty list, that is, <code>null</code>, if the specified field is a
 * static field. It is an error if the field descriptor does not
 * describe a field for the specified object.</p>
 *
 * <p><code>(field-set!</code>&nbsp;&nbsp;<i>field</i>&nbsp;&nbsp;<i>obj</i><sub><font size="-2">1</font></sub>&nbsp;&nbsp;<i>obj</i><sub><font size="-2">2</font></sub><code>)</code><br />
 * Set the value of the field described by field descriptor
 * <i>field</i> for object <i>obj</i><sub><font
 * size="-2">1</font></sub> to object <i>obj</i><sub><font
 * size="-2">2</font></sub>. Object <i>obj</i><sub><font
 * size="-2">1</font></sub> may be the empty list, that is,
 * <code>null</code>, if the specified field is a static
 * field. <code>Field-set!</code> returns an unspecified return
 * value. It is an error if the specified field descriptor does not
 * describe a field for object <i>obj</i><sub><font
 * size="-2">1</font></sub>, or if object <i>obj</i><sub><font
 * size="-2">2</font></sub> cannot be converted to a type that is
 * assignment compatible with the type of the field.</p>
 *
 * <p><code>(method</code>&nbsp;&nbsp;<i>class</i><sub><font size="-2">1</font></sub>&nbsp;&nbsp;<i>name</i>&nbsp;&nbsp;<i>class</i><sub><font size="-2">2</font></sub>&nbsp;...<code>)</code><br />
 * Return a method descriptor for the method with name <i>name</i> in
 * class <i>class</i><sub><font size="-2">1</font></sub> which accepts
 * the specified parameter types. The method can either be an instance
 * method or a static method. It is an error if the specified class
 * has no method with the specified name or parameter types.</p>
 *
 * <p><code>(invoke</code>&nbsp;&nbsp;<i>method</i>&nbsp;&nbsp;<i>obj</i><sub><font size="-2">1</font></sub>&nbsp;&nbsp;<i>obj</i><sub><font size="-2">2</font></sub>&nbsp;...<code>)</code><br />
 * Invoke the method described by method descriptor <i>method</i> for
 * object <i>obj</i><sub><font size="-2">1</font></sub> with the
 * specified arguments. <code>Invoke</code> returns the result of the
 * method invocation if the specified methodd returns a
 * result. Otherwise, <code>invoke</code> returns an unspecified
 * value. Object <i>obj</i><sub><font size="-2">1</font></sub> may be
 * the empty list, that is, <code>null</code> if the specified method
 * is a static method. It is an error if the specified method does not
 * describe a method for object <i>obj</i><sub><font
 * size="-2">1</font></sub>, or if any of the arguments cannot be
 * converted to a type that is assignment compatible with the type of
 * the corresponding parameter.</p>
 *
 *
 * <p><b>Exit</b></p>
 *
 * <p><code>(exit)</code><br />
 * <code>(exit</code>&nbsp;&nbsp;<i>n</i><code>)</code><br />
 * Exit the current session. <i>n</i>, if specified, must be an exact
 * integer which indicates the status code. If <i>n</i> is not
 * specified, the default status code is zero, indicating normal
 * termination. <code>Exit</code> never returns. The implementation of
 * <code>exit</code> throws a new {@link ExitException} with an exit
 * status of 0, if no status code was specified, and with the
 * specified status code otherwise.</p>
 *
 * @author   &copy; Copyright 1998-2000 by Robert Grimm.
 *           Written by <a href="mailto:rgrimm@alum.mit.edu">Robert
 *           Grimm</a>.
 * @version 1.0

 */
public final class Runtime extends AbstractApplicable {

  // ==================================================================
  //                            Constants
  // ==================================================================

  /** An empty array of classes. */
  private static final Class[] EMPTY_CLASSES = new Class[0];

  /** An empty array of objects. */
  private static final Object[] EMPTY_OBJECTS = new Object[0];

  /** The opcodes for the runtime operators implemented by this class. */
  private static final int
    NAN_Q = 30001, INFINITE_Q = 30002,

    GC = 30010, CURRENT_TIME = 30011, PROPERTY_REF = 30012,
    PROPERTY_SET = 30013, RESET_CURRENT_PORTS = 30014,

    FORK = 30020, SYNCHRONIZED = 30021,

    ERROR = 30030, CATCH = 30031,

    CLASS_Q = 30040, NAME_TO_CLASS = 30041, OBJECT_TO_CLASS = 30042,
    INSTANCE_Q = 30043, CLASS_TO_NAME = 30044, CONSTRUCTOR = 30045,
    NEW = 30046, FIELD = 30047, FIELD_REF = 30048, FIELD_SET = 30049,
    METHOD = 30050, INVOKE = 30051,

    INSTALL = 30060,

    EXIT = 30100;

  // ==================================================================
  //                            Constructor
  // ==================================================================

  /**
   * Create a new runtime operator with the specified name, opcode,
   * minimum number of arguments, and maximum number of arguments.
   *
   * @param   name     The name of this runtime operator.
   * @param   opcode   The opcode of this runtime operator.
   * @param   minArgs  The minimum number of arguments for this
   *                   runtime operator.
   * @param   maxArgs  The maximum number of arguments for this
   *                   runtime operator.
   */
  private Runtime(String name, int opcode, int minArgs, int maxArgs) {
    this.name    = name;
    this.opcode  = opcode;
    this.minArgs = minArgs;
    this.maxArgs = maxArgs;
  }

  // ======================================================================
  //                              Equality.
  // ======================================================================

  /**
   * Determine whether this runtime operator equals the specified object.
   *
   * @param   o  The object with which to compare.
   * @return     <code>true</code> if this runtime operator equals the
   *             specified object.
   */
  public boolean equals(Object o) {
    if (this == o) return true;
    if (! (o instanceof Runtime)) return false;

    Runtime other = (Runtime)o;

    return (   (name.equals(other.name))
            && (opcode  == other.opcode)
            && (minArgs == other.minArgs)
            && (maxArgs == other.maxArgs));
  }

  /**
   * Return a hash code for this runtime operator.
   *
   * @return  A hash code for this runtime operator.
   */
  public int hashCode() {
    return name.hashCode();
  }

  // ==================================================================
  //                               Application
  // ==================================================================

  /**
   * Apply this runtime operator on the specified arguments.
   *
   * @param   l          The arguments as a proper list.
   * @param   numArgs    The number of arguments in <code>l</code>.
   * @param   evaluator  The calling evaluator.
   * @return             The result of applying this runtime operator
   *                     on the specified arguments.
   * @throws  EvaluatorException
   *                     Signals an exceptional condition when
   *                     applying this runtime operator to the specified
   *                     arguments.
   */
  protected Object apply1(Pair l, int numArgs, Evaluator evaluator)
    throws EvaluatorException {

    Object o1 = null;
    Object o2 = null;
    Object o3 = null;

    if (1 <= numArgs) {
      o1 = l.getListRef(0);
    }
    if (2 <= numArgs) {
      o2 = l.getListRef(1);
    }
    if (3 <= numArgs) {
      o3 = l.getListRef(2);
    }

    switch (opcode) {

    case NAN_Q:
      return Cast.box(Math.isNaN(o1));
    case INFINITE_Q:
      return Cast.box(Math.isInfinite(o1));
    case GC:
      System.gc();
      return Boolean.FALSE;
    case CURRENT_TIME:
      return new Long(System.currentTimeMillis());
    case PROPERTY_REF:
      {
        String s = System.getProperty(Cast.toString(o1));
        if (null == s) return Boolean.FALSE;
        return s;
      }
    case PROPERTY_SET:
      System.setProperty(Cast.toString(o1), Cast.toString(o2));
      return Boolean.FALSE;
    case RESET_CURRENT_PORTS:
      evaluator.resetCurrentReaderWriter();
      return Boolean.FALSE;
    case FORK:
      {
        Applicable proc = Cast.toProcedure(o1);
        Evaluator  e2   = evaluator.spawn();

        e2.setUpExpression(Pair.cons(proc, Pair.EMPTY_LIST));
        return new Promise(e2);
      }
    case SYNCHRONIZED:
      {
        if (null == o1) {
          throw new BadTypeException("Not an object", o1);
        }
        Applicable thunk = Cast.toProcedure(o2);

        evaluator.setUpMonitor(o1);
        evaluator.returnExpression();
        return Pair.cons(thunk, Pair.EMPTY_LIST);
      }
    case ERROR:
      {
        String s = Cast.toString(o1);
        if (1 == numArgs) {
          throw new UserException(s);
        } else {
          throw new UserException(s, o2);
        }
      }
    case CATCH:
      {
        Class      k       = toClass(o1);
        Applicable thunk   = Cast.toProcedure(o2);
        Applicable handler = Cast.toProcedure(o3);

        try {
          evaluator.setUpExceptionHandler(handler, k);
        } catch (IllegalArgumentException x) {
          throw new BadArgumentException("Not a subtype of java.lang.Throwable",
                                         k);
        }
        evaluator.returnExpression();
        return Pair.cons(thunk, Pair.EMPTY_LIST);
      }
    case CLASS_Q:
      return Cast.box(o1 instanceof Class);
    case NAME_TO_CLASS:
      return getClass(Cast.toString(o1));
    case OBJECT_TO_CLASS:
      if (null == o1) {
        throw new BadTypeException("Not an object", o1);
      } else {
        return getClass(o1);
      }
    case INSTANCE_Q:
      return Cast.box(toClass(o2).isInstance(o1));
    case CLASS_TO_NAME:
      return Cast.getName(toClass(o1));
    case CONSTRUCTOR:
      return getConstructor(toClass(o1), toClasses((Pair)l.cdr));
    case NEW:
      o3 = l.cdr;
      return invokeConstructor(toConstructor(o1),
                               ((Pair.EMPTY_LIST == o3)? EMPTY_OBJECTS
                                : ((Pair)o3).toArray()));
    case FIELD:
      return getField(toClass(o1), Cast.toString(o2));
    case FIELD_REF:
      return getFieldValue(toField(o1), o2);
    case FIELD_SET:
      setFieldValue(toField(o1), o2, o3);
      return Boolean.FALSE;
    case METHOD:
      return getMethod(toClass(o1), Cast.toString(o2),
                       toClasses(l.getListTail(2)));
    case INVOKE:
      o3 = l.getListTail(2);
      return invokeMethod(toMethod(o1), o2,
                          ((Pair.EMPTY_LIST == o3)? EMPTY_OBJECTS
                           : ((Pair)o3).toArray()));
    case INSTALL:
      {
        Class[]  params = { one.eval.Environment.class };
        Method   m      = getMethod(toClass(o1), "install", params);
        Object[] args   = { evaluator.getTopLevelEnvironment() };
        invokeMethod(m, null, args);
        return Boolean.FALSE;
      }
    case EXIT:
      throw new ExitException(((numArgs == 0)? 0 : Cast.toInt(o1)));

    default:
      throw new Bug("Invalid opcode " + opcode + " for array operator " +
                    toString());
    }
  }

  // ==================================================================
  //                              Classes
  // ==================================================================
  
  /**
   * Return the Java class object for the class with the specified
   * name. The specified name can either be one of the primitive
   * types, the unqualified name of a class in the
   * <code>java.lang</code> package, of a fully qualified class or
   * interface name, and may be followed by some number of
   * "<code>[]</code>" to indicate an array class of as many
   * dimensions.
   *
   * <p>If a class name does not have a package name prefix, this
   * method first tries to resolve it "as is", and only then by
   * prepending "<code>java.lang.</code>" to its name. This ensures
   * that classes in the default package (that is, the package with no
   * package prefix) can always be accessed. If such a class exists,
   * the corresponding class in <code>java.lang</code> is returned by
   * using its fully qualified class name.</p>
   *
   * @param   name  The name of the class.
   * @return        The corresponding Java class.
   * @throws  BadArgumentException
   *                Signals that no class with the specified name
   *                could be found.
   */
  public static Class getClass(String name) throws BadArgumentException {
    // Get length.
    int len = name.length();

    // Basic consistency checks.
    if (0 == len) {
      throw new BadArgumentException("Empty class name", name);
    } else if ('[' == name.charAt(0)) {
      throw new BadArgumentException("Invalid class name", name);
    }

    // Count array dimensions.
    int dim = 0;
    int off = 0;

    while ((len - off - 2 >= 0) &&   // Avoid index out of bounds exceptions.
           ('[' == name.charAt(len - off - 2)) &&
           (']' == name.charAt(len - off - 1))) {
      dim += 1;
      off += 2;
    }

    // Get actual class.
    if (0 < dim) {           // For arrays.

      // The element type name must be of length at least one, b/c the
      // basic consistency check eliminated ("[]")*.

      // The external and internal element type name; the unqualified flag.
      String  external    = name.substring(0, len - off);
      String  internal    = null;
      boolean unqualified = false;

      if (-1 == external.indexOf('.')) {
        unqualified = true;

        // Element type is primitive, in the default package or in java.lang.

        char    c     = external.charAt(0);

        switch(c) {

        case 'b':
          if ("boolean".equals(external)) {
            internal = "Z";
          } else if ("byte".equals(external)) {
            internal = "B";
          }
          break;
        case 'c':
          if ("char".equals(external)) {
            internal = "C";
          }
          break;
        case 'd':
          if ("double".equals(external)) {
            internal = "D";
          }
          break;
        case 'f':
          if ("float".equals(external)) {
            internal = "F";
          }
          break;
        case 'i':
          if ("int".equals(external)) {
            internal = "I";
          }
          break;
        case 'l':
          if ("long".equals(external)) {
            internal = "J";
          }
          break;
        case 's':
          if ("short".equals(external)) {
            internal = "S";
          }
          break;
        default:
          // Nothing to do.
        }
      }

      // Build up internal class name.
      StringBuffer buf = new StringBuffer();

      for (int i=0; i<dim; i++) {
        buf.append('[');
      }

      if (null == internal) {
        // Element type is a class or interface.
        buf.append('L');

        if (unqualified) {
          boolean javaLang = true;
          try {
            Class.forName(external);
            // Element type is in default package.
            javaLang = false;
          } catch (ClassNotFoundException x) {
            // Ignore.
          }

          if (javaLang) {
            buf.append("java.lang.");
          }
        }
        buf.append(external);
        buf.append(';');

      } else {
        // Element type is primitive.
        buf.append(internal);
      }

      // Get class.
      try {
        return Class.forName(buf.toString());
      } catch (ClassNotFoundException x) {
        throw new BadArgumentException("Unable to find class", name);
      }

    } else {          // For primitive types, classes, interfaces.

      if (-1 == name.indexOf('.')) {
        // Primitive types, classes in default package or java.lang.

        // Handle primitive types.
        char c = name.charAt(0);

        switch(c) {

        case 'b':
          if ("boolean".equals(name)) return Boolean.TYPE;
          if ("byte".equals(name))    return Byte.TYPE;
          break;
        case 'c':
          if ("char".equals(name))    return Character.TYPE;
          break;
        case 'd':
          if ("double".equals(name))  return Double.TYPE;
          break;
        case 'f':
          if ("float".equals(name))   return Float.TYPE;
          break;
        case 'i':
          if ("int".equals(name))     return Integer.TYPE;
          break;
        case 'l':
          if ("long".equals(name))    return Long.TYPE;
          break;
        case 's':
          if ("short".equals(name))   return Short.TYPE;
          break;
        case 'v':
          if ("void".equals(name))    return Void.TYPE;
          break;
        default:
          // Nothing to do.
        }

        // Handle default package.
        // We do this first, b/c java.lang.* classes can always
        // be accessed through their fully qualified name.
        try {
          return Class.forName(name);
        } catch (ClassNotFoundException x) {
          // Ignore and go on to try java.lang.*.
        }

        // Handle java.lang.
        try {
          return Class.forName("java.lang." + name);
        } catch (ClassNotFoundException x) {
          throw new BadArgumentException("Unable to find class", name);
        }
      }

      // Regular, fully qualified class or interface name.
      try {
        return Class.forName(name);
      } catch (ClassNotFoundException x) {
        throw new BadArgumentException("Unable to find class", name);
      }
    }
  }

  /**
   * Return the Java class object representing the type of the specified
   * object.
   *
   * @param   o  The object to return the class for.
   * @return     The corresponding Java class.
   * @throws  NullPointerException
   *             Signals that <code>null == o</code>.
   */
  public static Class getClass(Object o) {
    return o.getClass();
  }

  // ==================================================================
  //                           Construction
  // ==================================================================

  /**
   * Return the constructor descriptor for the constructor with the
   * specified parameter types in the specified class.
   *
   * @param   k       The class.
   * @param   params  The array of declared parameter types.
   * @return          The corresponding constructor descriptor.
   * @throws  BadArgumentException
   *                  Signals that the specified constructor does not
   *                  exist.
   * @throws  WrappedException
   *                  Signals that access to the specified constructor
   *                  has been denied.
   * @throws  NullPointerException
   *                  Signals that <code>null == k</code>.
   */
  public static Constructor getConstructor(Class k, Class[] params)
    throws BadArgumentException, BadTypeException, WrappedException {

    Constructor c;

    try {
      c = k.getConstructor(params);
    } catch (NoSuchMethodException x) {
      throw new BadArgumentException("Non-existing constructor for " +
                                     k.toString(), Pair.createList(params));
    } catch (SecurityException x) {
      throw new WrappedException("Access to constructor denied for " +
                                 k.toString(), x, Pair.createList(params));
    }

    return c;
  }

  /**
   * Invoke the specified constructor with the specified
   * arguments. The specified arguments must be assignable to the
   * parameters of the specified method.
   *
   * @param   c     The constructor to invoke.
   * @param   args  The array of arguments.
   * @return        The newly created object.
   * @throws  EvaluatorException
   *                Signals that the specified constructor cannot be
   *                legally invoked on the specified arguments, or that
   *                an exceptional condition has occurred during the
   *                invocation of the constructor.
   * @throws  NullPointerException
   *                Signals that <code>c</code>  or <code>args</code>
   *                is <code>null</code>.
   */
  public static Object invokeConstructor(Constructor c, Object[] args)
    throws EvaluatorException {
    
    // Verify/convert arguments.
    Class[] types = c.getParameterTypes();

    if (args.length < types.length) {
      throw new BadArgumentException("Too few arguments (" + args.length +
                                     ") for", c);
    } else if (args.length > types.length) {
      throw new BadArgumentException("Too many arguments (" + args.length +
                                     ") for", c);
    }

    for (int i=0; i<args.length; i++) {
      args[i] = Cast.convert(args[i], types[i]);
    }

    // Invoke constructor.
    Object result;
    try {
      result = c.newInstance(args);

    } catch (IllegalAccessException x) {
      throw new WrappedException("Access denied", x, c);

    } catch (IllegalArgumentException x) {
      throw new Bug("Unexpected exception: " + x);

    } catch (InstantiationException x) {
      throw new WrappedException("Unable to instantiate abstract class", x,
                                 c.getDeclaringClass());

    } catch (InvocationTargetException x) {
      EvaluatorException.signal(x.getTargetException(),
                                "Exception during invocation",
                                c);
      return null; // Make compiler happy.

    } catch (ExceptionInInitializerError x) {
      throw new WrappedException("Initialization error during invocation",
                                 x, c);
    }

    // Return result.
    return result;
  }

  // ==================================================================
  //                             Field access
  // ==================================================================

  /**
   * Return the field descriptor for the field with the specified name
   * in the specified class.
   *
   * @param   k     The class.
   * @param   name  The name of the field.
   * @return        The corresponding field descriptor.
   * @throws  BadArgumentException
   *                Signals that the specified class does not have
   *                a field with the specified name.
   * @throws  WrappedException
   *                Signals that access to the specified field has been
   *                denied.
   * @throws  NullPointerException
   *                Signals that <code>null == k</code>.
   */
  public static Field getField(Class k, String name)
    throws BadArgumentException, WrappedException {

    Field f;

    try {
      f = k.getField(name);
    } catch (NoSuchFieldException x) {
      throw new BadArgumentException("Non-existing field " + name + " for", k);
    } catch (SecurityException x) {
      throw new WrappedException("Access to field " + name + " denied", x, k);
    }

    return f;
  }

  /**
   * Get the value of the specified field from the specified object.
   * The specified object must be an instance of the class declaring
   * the specified field, unless the field is static, in which case
   * the specified object may be <code>null</code>.
   *
   * @param   f  The field whose value to get.
   * @param   o  The object whose field value to get.
   * @return     The value of the specified field for the specified
   *             object.
   * @throws  BadTypeException
   *             Signals that the specified object is not an instance
   *             of the class declaring the specified field.
   * @throws  BadArgumentException
   *             Signals that <code>o</code> is <code>null</code> even
   *             though the specified field is not static.
   * @thows   WrappedException
   *             Signals that access to the specified field has been
   *             denied, or that initialization of the field failed.
   * @throws  NullPointerException
   *             Signals that <code>null == f</code>.
   */
  public static Object getFieldValue(Field f, Object o)
    throws BadTypeException, BadArgumentException, WrappedException {

    Object result;
    try {
      result = f.get(o);

    } catch (IllegalAccessException x) {
      throw new WrappedException("Access to " + f.toString() + " denied", x, o);

    } catch (IllegalArgumentException x) {
      throw new BadTypeException("Not an instance of " +
                                 f.getDeclaringClass().toString(), o);

    } catch (NullPointerException x) {
      throw new BadArgumentException("Field " + f.toString() + " not static");

    } catch (ExceptionInInitializerError x) {
      throw new WrappedException("Initialization error when accessing " +
                                 f.toString(), x, o);
    }

    return result;
  }

  /**
   * Set the value of the specified field from the specified object to
   * the specified value. The specified object must be an instance of
   * the class declaring the specified field, unless the field is
   * static, in which case the specified object may be
   * <code>null</code>. The specified value must be assignable to the
   * specified field.
   *
   * @param   f  The field whose value to set.
   * @param   o  The object whose field value to set.
   * @param   value
   *             The new value for the specified field.
   * @throws  BadTypeException
   *             Signals that the specified object is not an instance
   *             of the class declaring the specified field, or that
   *             the specified value has a type that is incompatible
   *             with the specified field.
   * @throws  BadArgumentException
   *             Signals that <code>o</code> is <code>null</code> even
   *             though the specified field is not static, or that
   *             the specified value, even though it is a number,
   *             cannot be assigned to the field also representing a
   *             number.
   * @thows   WrappedException
   *             Signals that access to the specified field has been
   *             denied, or that initialization of the field failed.
   * @throws  NullPointerException
   *             Signals that <code>null == f</code>.
   */
  public static void setFieldValue(Field f, Object o, Object value)
    throws BadTypeException, BadArgumentException, WrappedException {

    // Verify/convert value.
    value = Cast.convert(value, f.getType());

    // Set field.
    try {
      f.set(o, value);

    } catch (IllegalAccessException x) {
      throw new WrappedException("Access to " + f.toString() + " denied", x, o);

    } catch (IllegalArgumentException x) {
      throw new BadTypeException("Not an instance of " +
                                 f.getDeclaringClass().toString(), o);

    } catch (NullPointerException x) {
      throw new BadArgumentException("Field " + f.toString() + " not static");

    } catch (ExceptionInInitializerError x) {
      throw new WrappedException("Initialization error when accessing " +
                                 f.toString(), x, o);
    }
  }

  // ==================================================================
  //                          Method invocation
  // ==================================================================

  /**
   * Return the method descriptor for the method with the specified name
   * and the specified parameter types in the specified class.
   *
   * @param   k       The class.
   * @param   name    The name of the method.
   * @param   params  The array of declared parameter types.
   * @return          The corresponding method descriptor.
   * @throws  BadArgumentException
   *                  Signals that the specified method does not exist.
   * @throws  WrappedException
   *                  Signals that access to the specified method has been
   *                  denied.
   * @throws  NullPointerException
   *                  Signals that <code>null == k</code>.
   */
  public static Method getMethod(Class k, String name, Class[] params)
    throws BadArgumentException, WrappedException {

    Method m;

    try {
      m = k.getMethod(name, params);
    } catch (NoSuchMethodException x) {
      throw new BadArgumentException("Non-existing method for " + k.toString() +
                                     ", " + name, Pair.createList(params));
    } catch (SecurityException x) {
      throw new WrappedException("Access to method denied for " + k.toString() +
                                 ", " + name, x, Pair.createList(params));
    }

    return m;
  }

  /**
   * Invoke the specified method from the specified object with the
   * specified arguments. The specified object must be an instance of
   * the class declaring the specified method, unless the method is
   * static, in which case the specified object may be
   * <code>null</code>. The specified arguments must be assignable to
   * the parameters of the specified method.
   *
   * @param   m     The method to invoke.
   * @param   o     The object whose method to invoke.
   * @param   args  The array of arguments.
   * @return        The result of invoking the specified method on the
   *                specified arguments, or <code>#f</code> if the method
   *                has no result.
   * @throws  EvaluatorException
   *                Signals that the specified method cannot be legally
   *                invoked on the specified object or arguments, or that
   *                an exceptional condition has occurred during the
   *                invocation of the method.
   * @throws  NullPointerException
   *                Signals that <code>m</code> or <code>args</code>
   *                is <code>null</code>.
   */
  public static Object invokeMethod(Method m, Object o, Object[] args)
    throws EvaluatorException {
    
    // Verify/convert arguments.
    Class[] types = m.getParameterTypes();

    if (args.length < types.length) {
      throw new BadArgumentException("Too few arguments (" + args.length +
                                     ") for", m);
    } else if (args.length > types.length) {
      throw new BadArgumentException("Too many arguments (" + args.length +
                                     ") for", m);
    }

    for (int i=0; i<args.length; i++) {
      args[i] = Cast.convert(args[i], types[i]);
    }

    // Invoke method.
    Object result;
    try {
      result = m.invoke(o, args);

    } catch (IllegalAccessException x) {
      throw new WrappedException("Access to " + m.toString() + " denied", x, o);

    } catch (IllegalArgumentException x) {
      throw new BadTypeException("Not an instance of " +
                                 m.getDeclaringClass().toString(), o);

    } catch (InvocationTargetException x) {
      EvaluatorException.signal(x.getTargetException(),
                                "Exception during invocation of "
                                + m.toString(),
                                o);
      return null; // Make compiler happy.

    } catch (NullPointerException x) {
      throw new BadArgumentException("Method " + m.toString() + " not static");

    } catch (ExceptionInInitializerError x) {
      throw new WrappedException("Initialization error when invoking " +
                                 m.toString(), x, o);
    }

    // Return result.
    if (Void.TYPE == m.getReturnType()) {
      return Boolean.FALSE;
    } else {
      return result;
    }
  }

  // ==================================================================
  //                               Casts
  // ==================================================================

  /**
   * Ensure that the specified object is a Java class object, or a
   * Java string or string buffer describing a Java class name as
   * defined for {@link #getClass(String)}.
   *
   * @param   o  The object to cast.
   * @return     The corresponding class object.
   * @throws  BadTypeException
   *             Signals that the specified object is not a Java class
   *             nor a Java string or string buffer describing a Java
   *             class.
   * @throws  BadArgumentException
   *             Signals that the specified object is a string 
   *             describing a class that could not be found.
   */
  public static Class toClass(Object o)
    throws BadTypeException, BadArgumentException {

    if (o instanceof Class) {
      return (Class)o;
    } else if (o instanceof String) {
      return getClass((String)o);
    } else if (o instanceof StringBuffer) {
      return getClass(((StringBuffer)o).toString());
    } else {
      throw new BadTypeException("Not a class", o);
    }
  }

  /**
   * Ensure that the specified pair starts a proper list containing
   * only Java class objects or strings describing Java classes.
   *
   * @param   l  The list to cast.
   * @return     The elements of the list as an array of Java class
   *             objects.
   * @throws  BadTypeException
   *             Signals that an object on the specified list is
   *             not a class object nor a Java string or string buffer
   *             describing a Java class.
   * @thorws  BadArgumentException
   *             Signals that <code>l</code> is not a proper list,
   *             or that one of the objects on the specified list
   *             is a string describing a class that could not
   *             be found.
   */
  public static Class[] toClasses(Pair l)
    throws BadTypeException, BadArgumentException {

    if (Pair.EMPTY_LIST == l) {
      return EMPTY_CLASSES;
    } else {
      int     length  = l.length();
      Class[] classes = new Class[length];
      
      int i = 0;
      while (Pair.EMPTY_LIST != l) {
        classes[i++] = toClass(l.car);
        l            = (Pair)l.cdr;
      }

      return classes;
    }
  }

  /**
   * Ensure that the specified object is a constructor descriptor.
   *
   * @param   o  The object to cast.
   * @return     The object as a constructor descriptor.
   * @throws  BadTypeException
   *             Signals that the specified object is not a
   *             constructor descriptor.
   */
  public static Constructor toConstructor(Object o) throws BadTypeException {
    if (o instanceof Constructor) {
      return (Constructor)o;
    } else {
      throw new BadTypeException("Not a constructor", o);
    }
  }

  /**
   * Ensure that the specified object is a field descriptor.
   *
   * @param   o  The object to cast.
   * @return     The object as a field descriptor.
   * @throws  BadTypeException
   *             Signals that the specified object is not a field
   *             descriptor.
   */
  public static Field toField(Object o) throws BadTypeException {
    if (o instanceof Field) {
      return (Field)o;
    } else {
      throw new BadTypeException("Not a field descriptor", o);
    }
  }

  /**
   * Ensure that the specified object is a method descriptor.
   *
   * @param   o  The object to cast.
   * @return     The object as a method descriptor.
   * @throws  BadTypeException
   *             Signals that the specified object is not a method
   *             descriptor.
   */
  public static Method toMethod(Object o) throws BadTypeException {
    if (o instanceof Method) {
      return (Method)o;
    } else {
      throw new BadTypeException("Not a method descriptor", o);
    }
  }

  // ==================================================================
  //                            Initialization
  // ==================================================================
  
  /**
   * Install the runtime operators in the specified environment.
   *
   * @param   env     The environment to install the runtime
   *                  operators into.
   */
  public static void install(Environment env) {
    add(env, "not-a-number?",        NAN_Q,                1,  1);
    add(env, "infinite?",            INFINITE_Q,           1,  1);
    add(env, "gc",                   GC,                   0,  0);
    add(env, "current-time",         CURRENT_TIME,         0,  0);
    add(env, "property-ref",         PROPERTY_REF,         1,  1);
    add(env, "property-set!",        PROPERTY_SET,         2,  2);
    add(env, "reset-current-ports",  RESET_CURRENT_PORTS,  0,  0);
    add(env, "fork",                 FORK,                 1,  1);
    add(env, "synchronized",         SYNCHRONIZED,         2,  2);
    add(env, "error",                ERROR,                1,  2);
    add(env, "catch",                CATCH,                3,  3);
    add(env, "class?",               CLASS_Q,              1,  1);
    add(env, "name->class",          NAME_TO_CLASS,        1,  1);
    add(env, "object->class",        OBJECT_TO_CLASS,      1,  1);
    add(env, "instance?",            INSTANCE_Q,           2,  2);
    add(env, "class->name",          CLASS_TO_NAME,        1,  1);
    add(env, "constructor",          CONSTRUCTOR,          1, -1);
    add(env, "new",                  NEW,                  1, -1);
    add(env, "field",                FIELD,                2,  2);
    add(env, "field-ref",            FIELD_REF,            2,  2);
    add(env, "field-set!",           FIELD_SET,            3,  3);
    add(env, "method",               METHOD,               2, -1);
    add(env, "invoke",               INVOKE,               2, -1);
    add(env, "install",              INSTALL,              1,  1);
    add(env, "exit",                 EXIT,                 0,  1);
  }

  /**
   * Create a new runtime operator as specified and add a binding
   * for it to the specified environment.
   *
   * @param   env       The environment for the new runtime operator.
   * @param   name      The name of the new runtime operator.
   * @param   opcode    The opcode of the new runtime operator.
   * @param   minArgs   The non-negative minimum number of arguments for
   *                    the new runtime operator.
   * @param   maxArgs   The non-negative maximum number of arguments for
   *                    the new runtime operator, or -1 if it takes an
   *                    unlimited maximum number of arguments.
   */
  private static void add(Environment env, String name, int opcode,
                          int minArgs, int maxArgs) {
    name      = name.intern();
    Symbol  s = Symbol.intern(name);
    Runtime v = new Runtime(name, opcode, minArgs, maxArgs);

    env.bind(s, v);
  }

}
