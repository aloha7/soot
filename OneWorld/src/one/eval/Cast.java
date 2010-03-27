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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import java.math.BigInteger;
import java.math.BigDecimal;

import one.math.Rational;

import one.util.Bug;
import one.util.JavaPlatform;

import java.util.Collection;
import java.util.List;

/**
 * Implementation of casts, conversions, and boxing. This class
 * provides static helper methods to cast objects to a specific type,
 * to perform conversions when interfacing <i>eval</i> with arbitrary
 * Java code, and to box primitive types.
 *
 * <p>The methods in this class consider only instances of
 * <code>java.lang.Byte</code>, <code>java.lang.Short</code>,
 * <code>java.lang.Integer</code>, <code>java.lang.Long</code>, {@link
 * java.math.BigInteger}, and {@link one.math.Rational} as exact
 * numbers. All other instances of <code>java.lang.Number</code>, in
 * particular, <code>java.lang.Float</code>,
 * <code>java.lang.Double</code>, and {@link java.math.BigDecimal},
 * are considered inexact numbers.</p>
 *
 * @author   &copy; Copyright 1998-2000 by Robert Grimm.
 *           Written by <a href="mailto:rgrimm@alum.mit.edu">Robert
 *           Grimm</a>.
 * @version  1.0
 */
public final class Cast {

  // ==================================================================
  //                          Procedure Adaptor
  // ==================================================================

  /**
   * Implementation of a procedure adaptor. Procedure adaptors are
   * invocation handlers that make it possible to treat procedures as
   * Java interfaces. On a method invocation, they apply the procedure
   * to itself, the symbol representing the method name, and the
   * actual method arguments, in that order. Procedure adaptors handle
   * the required <code>hashCode()</code>, <code>equals()</code>, and
   * <code>toString()</code> invocations internally.
   */
  private static final class ProcedureAdaptor
    implements InvocationHandler, java.io.Serializable {

    /**
     * The procedure.
     *
     * @serial  Must not be <code>null</code>
     */
    private           Applicable proc;

    /**
     * The evaluator for applying the procedure.
     */
    private transient Evaluator  evaluator;

    /**
     * Create a new procedure adapter with the specified procedure.
     * Procedure applications are evaluated using an evaluator with
     * the default environment, writer, reader, and format.
     *
     * @see     Shell
     *
     * @param   proc       The procedure for the new procedure adaptor.
     * @throws  NullPointerException
     *                     Signals that either <code>proc</code>
     *                     is <code>null</code>.
     */
    public ProcedureAdaptor(Applicable proc) {
      this.proc = proc;
    }

    /** Process a method invocation. */
    public Object invoke(Object proxy, Method method, Object[] args)
      throws Throwable {

      // Create evaluator if necessary.
      if (null == evaluator) {
        synchronized(this) {
          if (null == evaluator) {
            evaluator = new Evaluator(Shell.getEnvironment(),
                                      Shell.getFormat(),
                                      Shell.getReader(),
                                      Shell.getWriter());
          }
        }
      }

      String   name   = method.getName();
      Object[] params = method.getParameterTypes();

      // Handle hashCode(), equals(), and toString().
      if ("hashCode".equals(name) && (0 == params.length)) {
        return new Integer(proc.hashCode());

      } else if ("equals".equals(name) &&
                 (1 == params.length) &&
                 params[0].equals(Object.class)) {
        return box(proc.equals(args[0]));

      } else if ("toString".equals(name) && (0 == params.length)) {
        return proc.toString();
      }

      // Apply procedure.
      Pair l = Pair.cons(proc,
                         Pair.cons(Symbol.intern(name),
                                   Pair.createList(args)));

      // Use evaluator if nobody is currently using it.
      if (! evaluator.isRunning()) {
        synchronized(evaluator) {
          if (! evaluator.isRunning()) {
            evaluator.setUpApplication(proc, l);
            evaluator.run();
            if (! evaluator.isResultValue()) {
              EvaluatorException.signal((Throwable)evaluator.getResult());
            }
            return evaluator.getResult();
          }
        }
      }

      // Spawn a fresh evaluator and use it instead.
      Evaluator e2 = evaluator.spawn();

      e2.setUpApplication(proc, l);
      e2.run();
      if (! e2.isResultValue()) {
        EvaluatorException.signal((Throwable)evaluator.getResult());
      }
      return e2.getResult();
    }
  }

  // ==================================================================
  //                             Constructor
  // ==================================================================

  private Cast() {
    // Nothing to construct.
  }

  // ==================================================================
  //                             Constants
  // ==================================================================

  /** Zero as an integer. */
  public static final Integer ZERO        = new Integer(0);

  /** One as an integer. */
  public static final Integer ONE         = new Integer(1);

  /** The maximum value of Java characters as a big integer. */
  public static final BigInteger BIG_CHAR_MAX_VALUE =
    BigInteger.valueOf(Character.MAX_VALUE);

  /** The minimum value of Java characters as a big integer. */
  public static final BigInteger BIG_CHAR_MIN_VALUE =
    BigInteger.valueOf(Character.MIN_VALUE);

  /** The maximum value of Java bytes as a big integer. */
  public static final BigInteger BIG_BYTE_MAX_VALUE =
    BigInteger.valueOf(Byte.MAX_VALUE);

  /** The minimum value of Java bytes as a big integer. */
  public static final BigInteger BIG_BYTE_MIN_VALUE = 
    BigInteger.valueOf(Byte.MIN_VALUE);

  /** The maximum value of Java shorts as a big integer. */
  public static final BigInteger BIG_SHORT_MAX_VALUE =
    BigInteger.valueOf(Short.MAX_VALUE);

  /** The minimum value of Java shorts as a big integer. */
  public static final BigInteger BIG_SHORT_MIN_VALUE = 
    BigInteger.valueOf(Short.MIN_VALUE);

  /** The maximum value of Java ints as a big integer. */
  public static final BigInteger BIG_INT_MAX_VALUE =
    BigInteger.valueOf(Integer.MAX_VALUE);

  /** The minimum value of Java ints as a big integer. */
  public static final BigInteger BIG_INT_MIN_VALUE = 
    BigInteger.valueOf(Integer.MIN_VALUE);

  /** The maximum value of Java longs as a big integer. */
  public static final BigInteger BIG_LONG_MAX_VALUE =
    BigInteger.valueOf(Long.MAX_VALUE);

  /** The minimum value of Java longs as a big integer. */
  public static final BigInteger BIG_LONG_MIN_VALUE =
    BigInteger.valueOf(Long.MIN_VALUE);

  // ==================================================================
  //                               Casts
  // ==================================================================

  /**
   * Convert the specified object to a boolean. This method returns
   * the boolean value for objects of type
   * <code>java.lang.Boolean</code>, and <code>true</code> otherwise.
   *
   * @param   o  The object to cast.
   * @return     The object as a boolean.
   */
  public static boolean toBoolean(Object o) {
    if (o instanceof Boolean) {
      return ((Boolean)o).booleanValue();
    } else {
      return true;
    }
  }

  /**
   * Convert the specified object to a character. Returns a Java
   * <code>char</code> value if the specified object is a
   * <code>java.lang.Character</code>.
   *
   * @param   o  The object to convert into a character.
   * @return     The corresponding character.
   * @throws  BadTypeException
   *             Signals that <code>o</code> is not
   *             of type <code>java.lang.Character</code>.
   */
  public static char toChar(Object o) throws BadTypeException {
    if (o instanceof Character) {
      return ((Character)o).charValue();
    } else {
      throw new BadTypeException("Not a character", o);
    }
  }

  /**
   * Convert the specified number to a <code>byte</code>. Returns a
   * Java byte value if the specified number represents an exact
   * number that is an integer and that fits into the range of Java
   * bytes.
   *
   * @param   n  The number to convert.
   * @return     The corresponding <code>byte</code>.
   * @throws  BadTypeException
   *             Signals that the specified object does not
   *             represent a number.
   * @throws  BadArgumentException
   *             Signals that the specified number represents
   *             a number that cannot be converted to a Java
   *             byte, for example, because it is a rational
   *             or because it does not fit into the range of
   *             Java bytes.
   */
  public static byte toByte(Object o)
    throws BadTypeException, BadArgumentException {

    if (o instanceof Byte) {
      return ((Byte)o).byteValue();

    } else if ((o instanceof Integer) ||
               (o instanceof Short) ||
               (o instanceof Long)) {
      long l = ((Number)o).longValue();

      if ((Byte.MIN_VALUE <= l) && (Byte.MAX_VALUE >= l)) {
        return (byte)l;
      } else {
        throw new BadArgumentException("Out of byte range", o);
      }

    } else if ((o instanceof BigInteger) ||
               (o instanceof Rational)) {
      BigInteger i;

      if (o instanceof BigInteger) {
        i = (BigInteger)o;
      } else {
        Rational r = (Rational)o;
        if (r.isInteger()) {
          i = r.numerator();
        } else {
          throw new BadArgumentException("Not an integer", o);
        }
      }

      if ((i.compareTo(BIG_BYTE_MAX_VALUE) <= 0) &&
          (i.compareTo(BIG_BYTE_MIN_VALUE) >= 0)) {
        return i.byteValue();
      } else {
        throw new BadArgumentException("Out of int range", o);
      }

    } else if (o instanceof Number) {
      throw new BadArgumentException("Not an exact number", o);

    } else {
      throw new BadTypeException("Not a number", o);
    }
  }

  /**
   * Convert the specified object to an <code>int</code>. Returns a
   * Java int value if the specified object represents an exact number
   * that is an integer and that fits into the range of Java ints.
   *
   * @param   o  The object to convert.
   * @return     The corresponding <code>int</code>.
   * @throws  BadTypeException
   *             Signals that the specified object does not
   *             represent a number.
   * @throws  BadArgumentException
   *             Signals that the specified object represents
   *             a number that cannot be converted to a Java
   *             int, for example, because it is a rational
   *             or because it does not fit into the range of
   *             Java ints.
   */
  public static int toInt(Object o)
    throws BadTypeException, BadArgumentException {

    if (! (o instanceof Number)) {
      throw new BadTypeException("Not a number", o);
    }

    Number n = (Number)o;

    if ((n instanceof Integer) ||
        (n instanceof Byte) ||
        (n instanceof Short)) {
      return n.intValue();

    } else if (n instanceof Long) {
      long l = n.longValue();

      if ((Integer.MIN_VALUE <= l) && (Integer.MAX_VALUE >= l)) {
        return (int)l;
      } else {
        throw new BadArgumentException("Out of int range", n);
      }

    } else if ((n instanceof BigInteger) ||
               (n instanceof Rational)) {
      BigInteger i;

      if (n instanceof BigInteger) {
        i = (BigInteger)n;
      } else {
        Rational r = (Rational)n;
        if (r.isInteger()) {
          i = r.numerator();
        } else {
          throw new BadArgumentException("Not an integer", n);
        }
      }

      if ((i.compareTo(BIG_INT_MAX_VALUE) <= 0) &&
          (i.compareTo(BIG_INT_MIN_VALUE) >= 0)) {
        return i.intValue();
      } else {
        throw new BadArgumentException("Out of int range", n);
      }

    } else {
      throw new BadArgumentException("Not an exact number", n);
    }
  }

  /**
   * Convert the specified object to a Java string. If the specified
   * object is a string, that object is returned. If the specified
   * object is a string buffer, the corresponding string is returned.
   * Otherwise, an exception is signalled.
   *
   * @param   o  The object to convert.
   * @return     The specified object as a Java string.
   * @throws  BadTypeException
   *             Signals that <code>o</code> is neither a Java
   *             string nor a string buffer.
   */
  public static String toString(Object o) throws BadTypeException {
    if (o instanceof String) {
      return (String)o;
    } else if (o instanceof StringBuffer) {
      return ((StringBuffer)o).toString();
    } else {
      throw new BadTypeException("Not a string", o);
    }
  }

  /**
   * Ensure that the specified object is a procedure, that is, a
   * non-syntactic applicable entity.
   *
   * @param   o  The object to cast.
   * @return     The object as a non-syntactic applicable entity.
   * @throws  BadTypeException
   *             Signals that the specified object is not an
   *             applicable entity.
   * @throws  BadArgumentException
   *             Signals that the specified object is a syntactic
   *             applicable entity.
   */
  public static Applicable toProcedure(Object o)
    throws BadTypeException, BadArgumentException {

    if (o instanceof Applicable) {
      Applicable proc = (Applicable)o;

      if (proc.isSyntactic()) {
        throw new BadArgumentException("Not a procedure", o);
      } else {
        return proc;
      }

    } else {
      throw new BadTypeException("Not a procedure", o);
    }
  }

  // ==================================================================
  //                               Tests
  // ==================================================================

  /**
   * Determine whether the specified object is a Java array.
   *
   * @param   o  The object to test.
   * @return     <code>true</code> if the specified object is a Java
   *             array.
   */
  public static boolean isArray(Object o) {
    return ((null != o) && (o.getClass().isArray()));
  }

  // ==================================================================
  //                             Conversion
  // ==================================================================

  /** Flag for types that do not represent a number. */
  private static final int FLAG_NAN         = -1;

  /** Flag for type <code>char</code>. */
  private static final int FLAG_PRIM_CHAR   =  0;

  /** Flag for type <code>java.lang.Character</code>. */
  private static final int FLAG_CHAR        =  1;

  /** Flag for type <code>byte</code>. */
  private static final int FLAG_PRIM_BYTE   =  2;

  /** Flag for type <code>java.lang.Byte</code>. */
  private static final int FLAG_BYTE        =  3;

  /** Flag for type <code>short</code>. */
  private static final int FLAG_PRIM_SHORT  =  4;

  /** Flag for type <code>java.lang.Short</code>. */
  private static final int FLAG_SHORT       =  5;

  /** Flag for type <code>int</code>. */
  private static final int FLAG_PRIM_INT    =  6;

  /** Flag for type <code>java.lang.Integer</code>. */
  private static final int FLAG_INT         =  7;

  /** Flag for type <code>long</code>. */
  private static final int FLAG_PRIM_LONG   =  8;

  /** Flag for type <code>java.lang.Long</code>. */
  private static final int FLAG_LONG        =  9;

  /** Flag for type <code>float</code>. */
  private static final int FLAG_PRIM_FLOAT  = 10;

  /** Flag for type <code>java.lang.Float</code>. */
  private static final int FLAG_FLOAT       = 11;

  /** Flag for type <code>double</code>. */
  private static final int FLAG_PRIM_DOUBLE = 12;

  /** Flag for type <code>java.lang.Double</code>. */
  private static final int FLAG_DOUBLE      = 13;

  /** Flag for type <code>java.math.BigInteger</code>. */
  private static final int FLAG_BIG_INTEGER = 14;

  /** Flag for type <code>one.math.Rational</code>. */
  private static final int FLAG_RATIONAL    = 15;

  /** Flag for type <code>java.math.BigDecimal</code>. */
  private static final int FLAG_BIG_DECIMAL = 16;

  /** Flag for type <code>java.lang.Number</code>. */
  private static final int FLAG_NUMBER      = 17;

  /**
   * Convert the specified object to an object that can be assigned to
   * a field or argument of the specified class.
   *
   * <p>The intent behind this method is to be able to convert Scheme
   * objects into objects with a specific Java type, as long as the
   * conversion "makes sense". Roughly, a conversion makes sense, if
   * <i>eval</i> uses several Java types internally to represent a
   * Scheme type, the specified class is one of these types, and the
   * conversion does not loose any semantic information.</p>
   *
   * <p>As this method is typically used in combination with the Java
   * reflection API, it exploits knowledge about how the Java
   * reflection API does its own conversions, in order to avoid
   * unnecessarily creating new objects.</p>
   *
   * <p>This method works as follows. First, it simply returns the
   * specified object, if one of the following conditions holds:
   * <ul>
   * <li>The specified object is an instance of the specified
   *     class.</li>
   * <li>The specified object is the empty list, that is,
   *     <code>null</code>, and the specified class does not
   *     represent a primitive type.</li>
   * <li>The specified class represents a primitive type and the
   *     specified object is an instance of the corresponding
   *     reference type.</li>
   * <li>The specified class represents a primitive type and the
   *     specified object is an instance of a reference type, whose
   *     corresponding primitive type can be converted to the
   *     specified primitive type by a widening conversion as
   *     described in &sect;5.1.2 of the Java language
   *     specification.</li>
   * </ul></p>
   *
   * <p>Next, this method attempts to perform one of the following
   * conversions:
   * <ul>
   * <li>For classes representing booleans, that is, for both the
   *     primitive type <code>boolean</code> and the corresponding
   *     reference type <code>java.lang.Boolean</code>, any object
   *     other than a boolean value, is converted to the boolean truth
   *     value.</li>
   * <li>For classes representing characters, that is, for both the
   *     primitive type <code>char</code> and the corresponding
   *     reference type <code>java.lang.Character</code>, all exact
   *     numbers that are integers and fit into the numerical range of
   *     a character are converted to the corresponding
   *     character.</li>
   * <li>For classes representing exact numbers, that is,
   *     <code>byte</code>, <code>java.lang.Byte</code>,
   *     <code>short</code>, <code>java.lang.Short</code>,
   *     <code>int</code>, <code>java.lang.Integer</code>,
   *     <code>long</code>, <code>java.lang.Long</code>,
   *     <code>java.math.BigInteger</code>,
   *     <code>one.math.Rational</code>, all exact numbers that are
   *     integers and fit into the numerical range of the specified
   *     type are converted to an instance of that type.</li>
   * <li>For classes representing inexact numbers, that is,
   *     <code>float</code>, <code>java.lang.Float</code>,
   *     <code>double</code>, <code>java.lang.Double</code>, and
   *     <code>java.math.BigDecimal</code>, numbers and characters are
   *     converted to an instance of the specified type, even if this
   *     entails a loss of precision.</li>
   * <li>For <code>java.lang.Number</code>, characters are converted
   *     to an instance of <code>java.lang.Integer</code>.</li>
   * <li>For Java strings, string buffers are converted to the
   *     corresponding string. But, Java strings are not converted to
   *     string buffers, as they are assumed to be literals.</li>
   * <li>For Java arrays, Java arrays with a different component type
   *     as well as lists in the Java collections framework are
   *     converted to an array of the specified array type by
   *     recursively converting the elements of the specified array or
   *     list.</li>
   * <li>For collections in the Java collections framework, Java
   *     arrays are converted to instances of
   *     <code>one.eval.Vector</code>.</li>
   * <li>For lists in the Java collections framework, Java arrays are
   *     converted to instances of <code>one.eval.Vector</code>.</li>
   * <li>For <code>one.eval.Vector</code>, Java arrays and other lists
   *     in the Java collections framework are converted to an
   *     instance of <code>one.eval.Vector</code>.</li>
   * <li>For arbitrary Java interfaces, procedures that take at least
   *     two arguments and at most an unlimited number of arguments
   *     are wrapped with an object implementing the specified
   *     interface. On an invocation of any of the methods defined by
   *     the specified interface, the procedure is applied to, in
   *     order, itself, the symbol representing the name of the
   *     invoked method, and the actual arguments. The evaluator used
   *     for evaluating the application uses the default environment,
   *     writer, reader, and format provided by the shell.</li>
   * </ul></p>
   *
   * <p>Finally, if the specified object neither is an instance of the
   * specified class, nor convertible to the specified class, this
   * method throws a <code>BadTypeException</code>.</p>
   *
   * @param   o  The object to convert.
   * @param   k  The class of the object to convert to.
   * @return     An object that can be assigned to a field or argument
   *             of the specified class.
   * @throws  BadTypeException
   *             Signals that the object cannot be converted to the
   *             specified class.
   * @throws  BadArgumentException
   *             Signals either that the specified object is an
   *             inexact number and the specified class represents
   *             an exact number or character, or that the
   *             specified object is an exact number out of the
   *             range of the specified class representing another
   *             exact number or character.
   */
  public static Object convert(Object o, Class k)
    throws BadTypeException, BadArgumentException {

    if (k.isInstance(o)) {                         // Instances.
      // If Java thinks so, fine.
      return o;

    } else if ((null == o) &&                      // Null.
               (! k.isPrimitive())) {
      // Any reference type can be null.
      return o;

    } else if ((Boolean.TYPE  == k) ||             // Booleans.
               (Boolean.class == k)) {
      if (o instanceof Boolean) {
        return o;
      } else {
        return Boolean.TRUE;
      }

    } else if (String.class == k) {                // Strings.
      if (o instanceof StringBuffer) {
        return ((StringBuffer)o).toString();
      } else {
        throw new BadTypeException("Unable to convert to a string", o);
      }

    } else if (k.isArray()) {                      // Java arrays.
      if (o instanceof List) {
        List   l    = (List)o;
        Class  comp = k.getComponentType();
        int    len  = l.size();
        Object a    = java.lang.reflect.Array.newInstance(comp, len);

        for (int i=0; i<len; i++) {
          java.lang.reflect.Array.set(a, i, convert(l.get(i), comp));
        }

        return a;

      } else if (isArray(o)) {
        Class  comp = k.getComponentType();
        int    len  = java.lang.reflect.Array.getLength(o);
        Object a    = java.lang.reflect.Array.newInstance(comp, len);

        for (int i=0; i<len; i++) {
          Object el = java.lang.reflect.Array.get(o, i);
          java.lang.reflect.Array.set(a, i, convert(el, comp));
        }

        return a;

      } else {
        throw new BadTypeException("Unable to convert to " + getName(k), o);
      }

    } else if (Collection.class == k) {            // Collections.
      if (isArray(o)) {
        return Vector.create(o);
      } else {
        throw new BadTypeException("Unable to convert to java.util.Collection",
                                   o);
      }

    } else if (List.class == k) {                  // Lists.
      if (isArray(o)) {
        return Vector.create(o);
      } else {
        throw new BadTypeException("Unable to convert to java.util.List", o);
      }

    } else if (Vector.class == k) {                // one.eval.Vector.
      if (isArray(o)) {
        return Vector.create(o);
      } else if (o instanceof List) {
        return Vector.create(((Collection)o));
      } else {
        throw new BadTypeException("Unable to convert to one.eval.Vector", o);
      }

    } else if (k.isInterface() &&                  // Lambdas as interfaces.
               (o instanceof Applicable) &&
               JavaPlatform.includesJDK13()) {

      Applicable proc = (Applicable)o;
      if ((! proc.isSyntactic()) &&
          (2  == proc.getMinArgs()) &&
          (-1 == proc.getMaxArgs())) {

        return Proxy.newProxyInstance(k.getClassLoader(),
                                      new Class[] { k },
                                      new ProcedureAdaptor(proc));
      }
    }

    /*
     * Handle characters and numbers.
     *
     * Note that for reference types, we must produce an instance of
     * exactly the right type. Also, note that the instances test
     * above took care of objects that already are instances of a
     * reference types. So, for example, there is no need to handle a
     * <code>Long</code>, when a <code>Long</code> is expected.
     */

    // Determine flag for target class.
    int flag = categorize(k);

    // We can only possibly convert objects that are characters or numbers.
    if (! ((o instanceof Number) || (o instanceof Character))) {
      if ((FLAG_PRIM_CHAR == flag) || (FLAG_CHAR == flag)) {
        throw new BadTypeException("Unable to convert to a character", o);
      } else {
        throw new BadTypeException("Unable to convert to " + getName(k),
                                   o);
      }
    }
    
    // Handle inexact numbers, as well as big integers and rationals.
    switch(flag) {

    case FLAG_NAN:
      throw new BadTypeException("Unable to convert to " + getName(k), o);

    case FLAG_PRIM_FLOAT:
      if ((o instanceof Float) || (o instanceof Long) ||
          (o instanceof Integer) || (o instanceof Character) ||
          (o instanceof Short) || (o instanceof Byte)) {
        return o;
      } else {
        return box(((Number)o).floatValue());
      }

    case FLAG_FLOAT:
      if (o instanceof Character) {
        return new Float(((Character)o).charValue());
      } else {
        return box(((Number)o).floatValue());
      }

    case FLAG_PRIM_DOUBLE:
      if ((o instanceof Double) || (o instanceof Float) ||
          (o instanceof Long) || (o instanceof Integer) ||
          (o instanceof Character) || (o instanceof Short) ||
          (o instanceof Byte)) {
        return o;
      } else {
        return new Double(((Number)o).doubleValue());
      }
      
    case FLAG_DOUBLE:
      if (o instanceof Character) {
        return new Double(((Character)o).charValue());
      } else {
        return box(((Number)o).doubleValue());
      }
      
    case FLAG_BIG_INTEGER:
      // Preserve exactness.
      if ((o instanceof Integer) || (o instanceof Long) ||
          (o instanceof Byte) || (o instanceof Short)) {
        return BigInteger.valueOf(((Number)o).longValue());
        
      } else if (o instanceof Character) {
        return BigInteger.valueOf(((Character)o).charValue());

      } else if (o instanceof Rational) {
        Rational r = (Rational)o;
        if (r.isInteger()) {
          return r.numerator();
        } else {
          throw new BadArgumentException("Not an integer", o);
        }
        
      } else {
        throw new BadArgumentException("Not an exact number", o);
      }
      
    case FLAG_RATIONAL:
      // Preserve exactness.
      if ((o instanceof Integer) || (o instanceof Long) ||
          (o instanceof BigInteger) || (o instanceof Byte) ||
          (o instanceof Short)) {
        return Rational.valueOf((Number)o);

      } else if (o instanceof Character) {
        return Rational.valueOf(((Character)o).charValue());
        
      } else {
        throw new BadArgumentException("Not an exact number", o);
      }
      
    case FLAG_BIG_DECIMAL:
      if (o instanceof BigInteger) {
        return new BigDecimal((BigInteger)o);
        
      } else if ((o instanceof Integer) || (o instanceof Long) ||
                 (o instanceof Byte) || (o instanceof Short)) {
        return BigDecimal.valueOf(((Number)o).longValue());

      } else if (o instanceof Character) {
        return BigDecimal.valueOf(((Character)o).charValue());
        
      } else {
        return new BigDecimal(((Number)o).doubleValue());
      }
      
    case FLAG_NUMBER:
      // o must be a character.
      return new Integer(((Character)o).charValue());
      
    default:
      // Fall through.
    }

    // Target class must represent a byte, short, char, int, or long,
    // or the corresponding reference type.
    if ((o instanceof Rational) || (o instanceof BigInteger)) {
      // We need an integer.
      BigInteger i;
      if (o instanceof Rational) {
        Rational r = (Rational)o;
        if (r.isInteger()) {
          i = r.numerator();
        } else {
          throw new BadArgumentException("Not an integer", o);
        }
      } else {
        i = (BigInteger)o;
      }
      
      switch (flag) {
        
      case FLAG_PRIM_CHAR:
      case FLAG_CHAR:
        if ((i.compareTo(BIG_CHAR_MAX_VALUE) <= 0) &&
            (i.compareTo(BIG_CHAR_MIN_VALUE) >= 0)) {
          return new Character((char)i.intValue());
        } else {
          throw new BadArgumentException("Out of char range", o);
        }

      case FLAG_PRIM_BYTE:
      case FLAG_BYTE:
        if ((i.compareTo(BIG_BYTE_MAX_VALUE) <= 0) &&
            (i.compareTo(BIG_BYTE_MIN_VALUE) >= 0)) {
          return new Byte(i.byteValue());
        } else {
          throw new BadArgumentException("Out of byte range", o);
        }
        
      case FLAG_PRIM_SHORT:
      case FLAG_SHORT:
        if ((i.compareTo(BIG_SHORT_MAX_VALUE) <= 0) &&
            (i.compareTo(BIG_SHORT_MIN_VALUE) >= 0)) {
          return new Short(i.shortValue());
        } else {
          throw new BadArgumentException("Out of short range", o);
        }
        
      case FLAG_PRIM_INT:
      case FLAG_INT:
        if ((i.compareTo(BIG_INT_MAX_VALUE) <= 0) &&
            (i.compareTo(BIG_INT_MIN_VALUE) >= 0)) {
          return new Integer(i.intValue());
        } else {
          throw new BadArgumentException("Out of int range", o);
        }
        
      case FLAG_PRIM_LONG:
      case FLAG_LONG:
        if ((i.compareTo(BIG_LONG_MAX_VALUE) <= 0) &&
            (i.compareTo(BIG_LONG_MIN_VALUE) >= 0)) {
          return new Long(i.longValue());
        } else {
          throw new BadArgumentException("Out of long range", o);
        }
        
      default:
        throw new Bug("Invalid internal state when converting to class "
                      + getName(k));
      }
    } else if ((o instanceof Long) || (o instanceof Integer) ||
               (o instanceof Character) || (o instanceof Short) ||
               (o instanceof Byte)) {
      long l;
      if (o instanceof Character) {
        l = ((Character)o).charValue();
      } else {
        l = ((Number)o).longValue();
      }
      
      switch (flag) {

      case FLAG_PRIM_CHAR:
        if (o instanceof Character) {
          return o;
        } else if ((Character.MIN_VALUE <= l) && (Character.MAX_VALUE >= l)) {
          return new Character((char)l);
        } else {
          throw new BadArgumentException("Out of char range", o);
        }

      case FLAG_CHAR:
        if ((Character.MIN_VALUE <= l) && (Character.MAX_VALUE >= l)) {
          return new Character((char)l);
        } else {
          throw new BadArgumentException("Out of char range", o);
        }
        
      case FLAG_PRIM_BYTE:
        if (o instanceof Byte) {
          return o;
        } else if ((Byte.MIN_VALUE <= l) && (Byte.MAX_VALUE >= l)) {
          return new Byte((byte)l);
        } else {
          throw new BadArgumentException("Out of byte range", o);
        }
        
      case FLAG_BYTE:
        if ((Byte.MIN_VALUE <= l) && (Byte.MAX_VALUE >= l)) {
          return new Byte((byte)l);
        } else {
          throw new BadArgumentException("Out of byte range", o);
        }
        
      case FLAG_PRIM_SHORT:
        if ((o instanceof Short) || (o instanceof Byte)) {
          return o;
        } else if ((Short.MIN_VALUE <= l) && (Short.MAX_VALUE >= l)) {
          return new Short((short)l);
        } else {
          throw new BadArgumentException("Out of short range", o);
        }
        
      case FLAG_SHORT:
        if ((Short.MIN_VALUE <= l) && (Short.MAX_VALUE >= l)) {
          return new Short((short)l);
        } else {
          throw new BadArgumentException("Out of short range", o);
        }
        
      case FLAG_PRIM_INT:
        if (! (o instanceof Long)) {
          return o;
        } else if ((Integer.MIN_VALUE <= l) && (Integer.MAX_VALUE >= l)) {
          return new Integer((int)l);
        } else {
          throw new BadArgumentException("Out of int range", o);
        }
        
      case FLAG_INT:
        if ((Integer.MIN_VALUE <= l) && (Integer.MAX_VALUE >= l)) {
          return new Integer((int)l);
        } else {
          throw new BadArgumentException("Out of int range", o);
        }
        
      case FLAG_PRIM_LONG:
        return o;
        
      case FLAG_LONG:
        return new Long(l);
        
      default:
        throw new Bug("Invalid internal state when converting to class "
                      + getName(k));
      }

    } else {
      throw new BadArgumentException("Not an exact number", o);
    }
  }

  /**
   * Categorize the specified class. Returns one of the flags defined
   * by this class, that is, one of the fields starting with
   * <code>FLAG</code>, to indicate the type represented by the
   * specified class.
   *
   * @param   k  The class to categorize.
   * @return     The corresponding flag.
   */
  private static int categorize(Class k) {
    if (Character.TYPE == k) {
      return FLAG_PRIM_CHAR;
    } else if (Character.class == k) {
      return FLAG_CHAR;
    } else if (Byte.TYPE == k) {
      return FLAG_PRIM_BYTE;
    } else if (Byte.class == k) {
      return FLAG_BYTE;
    } else if (Short.TYPE == k) {
      return FLAG_PRIM_SHORT;
    } else if (Short.class == k) {
      return FLAG_SHORT;
    } else if (Integer.TYPE == k) {
      return FLAG_PRIM_INT;
    } else if (Integer.class == k) {
      return FLAG_INT;
    } else if (Long.TYPE == k) {
      return FLAG_PRIM_LONG;
    } else if (Long.class == k) {
      return FLAG_LONG;
    } else if (Float.TYPE == k) {
      return FLAG_PRIM_FLOAT;
    } else if (Float.class == k) {
      return FLAG_FLOAT;
    } else if (Double.TYPE == k) {
      return FLAG_PRIM_DOUBLE;
    } else if (Double.class == k) {
      return FLAG_DOUBLE;
    } else if (BigInteger.class == k) {
      return FLAG_BIG_INTEGER;
    } else if (Rational.class == k) {
      return FLAG_RATIONAL;
    } else if (BigDecimal.class == k) {
      return FLAG_BIG_DECIMAL;
    } else if (Number.class == k) {
      return FLAG_NUMBER;
    } else {
      return FLAG_NAN;
    }
  }

  // ==================================================================
  //                             Class names
  // ==================================================================

  /**
   * Return the name of the specified class. The returned name is
   * simply the name for primitive types, the fully qualified class
   * name for interfaces and classes, and a combination of the first
   * two with trailing <code>[]</code>s, one for each dimension, for
   * arrays.
   *
   * @param   k  The class to return the name for.
   * @return     The corresponding name.
   * @throws  NullPointerException
   *             Signals that <code>null == o</code>.
   */
  public static String getName(Class k) {
    String s = k.getName();

    if ('[' == s.charAt(0)) {             // Unmangle array names.

      // Count dimensions.
      int dim = 1;
      while('[' == s.charAt(dim)) {
        dim++;
      }

      StringBuffer buf = new StringBuffer();
      char         c   = s.charAt(dim);

      // Get element type name.
      switch(c) {

      case 'B':
        buf.append("byte");
        break;
      case 'C':
        buf.append("char");
        break;
      case 'D':
        buf.append("double");
        break;
      case 'F':
        buf.append("float");
        break;
      case 'I':
        buf.append("int");
        break;
      case 'J':
        buf.append("long");
        break;
      case 'L':
        buf.append(s.substring(dim + 1, s.length() - 1));
        break;
      case 'S':
        buf.append("short");
        break;
      case 'Z':
        buf.append("boolean");
        break;
      default:
        throw new Bug("Invalid element type \'" + c + "\' for array");
      }

      // Add []s.
      for (int i=0; i<dim; i++) {
        buf.append("[]");
      }

      return buf.toString();

    } else {                              // Just the name for all others.
      return s;
    }
  }

  // ==================================================================
  //                               Boxes
  // ==================================================================

  /**
   * Box the specified boolean.
   *
   * @param   b  The boolean to box.
   * @return     The boxed boolean.
   */
  public static Boolean box(boolean b) {
    return (b ? Boolean.TRUE : Boolean.FALSE);
  }

  /**
   * Box the specified character.
   *
   * @param   c  The character to box.
   * @return     The boxed character.
   */
  public static Character box(char c) {
    return new Character(c);
  }

  /**
   * Box the specified integer.
   *
   * @param   n  The integer to box.
   * @return     The boxed integer.
   */
  public static Number box(int n) {
    switch (n) {
    case 0:
      return ZERO;
    case 1:
      return ONE;
    default:
      return new Integer(n);
    }
  }

  /**
   * Box the specified long.
   *
   * @param   n  The long to box.
   * @return     An exact number representing the specified
   *             long.
   */
  public static Number box(long n) {
    if ((Integer.MIN_VALUE <= n) && (Integer.MAX_VALUE >= n)) {
      return box((int)n);
    } else {
      return new Long(n);
    }
  }

  /**
   * Box the specified float.
   *
   * @param   f  The float to box.
   * @return     An inexact number representing the specified
   *             float.
   */
  public static Number box(float f) {
    return new Float(f);
  }

  /**
   * Box the specified double.
   *
   * @param   d  The double to box.
   * @return     An inexact number representing the specified
   *             double.
   */
  public static Number box(double d) {
    return new Double(d);
  }

}
