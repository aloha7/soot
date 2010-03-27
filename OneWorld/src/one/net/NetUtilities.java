/*
 * Copyright (c) 2001, University of Washington, Department of
 * Computer Science and Engineering.
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
 * 3. Neither name of the University of Washington, Department of
 * Computer Science and Engineering nor the names of its contributors
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

package one.net;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

import one.world.core.Tuple;
import one.world.core.TupleException;

/**
 * Implementation of utility functionality to support the interaction
 * between Internet protocols and <i>one.world</i>.
 *
 * @version  $Revision: 1.7 $
 * @author   Robert Grimm
 */
public final class NetUtilities {

  /** Hide the constructor. */
  private NetUtilities() {
    // Nothing to do.
  }

  /** The size of the buffers used for puming. */
  private static final int         BUFFER_SIZE = 1024;

  /** A buffer for pumping. */
  private static final ThreadLocal buffer      = new ThreadLocal() {
      protected Object initialValue() {
        return new byte[BUFFER_SIZE];
      }
    };

  /**
   * Pump data from one stream to another. This method pumps up to the
   * specified number of bytes from the specified input stream to the
   * specified output stream. It pumps less bytes if the input stream
   * reaches the end-of-file or an exceptional condition is
   * encountered.
   *
   * @param   in      The input stream to pump from.
   * @param   out     The output stream to pump into.
   * @param   length  The number of bytes to pump.
   * @return          The number of bytes actually pumped.
   * @throws  IOException
   *                  Signals an exceptional condition while
   *                  accessing the specified streams.
   */
  public static long pump(InputStream in, OutputStream out, long length)
       throws IOException {

    byte[] buf     = (byte[])buffer.get();
    int    chunk;
    int    readin;
    long   toGo    = length;

    chunk = (int)((toGo > buf.length)? buf.length : toGo);
    while ((toGo > 0) && ((readin = in.read(buf, 0, chunk)) != -1)) {
      out.write(buf, 0, readin);
      toGo  -= readin;
      chunk  = (int)((toGo > buf.length)? buf.length : toGo);
    }

    return length - toGo;
  }

  /**
   * Select an appropriate converter for the specified tuple type from
   * the specified array of converters. This method searches for the
   * closest match (where closest is defined as the class hierarchy
   * distance between the specified class and the tuple type handled
   * by a converter) and uses the order of the array as a tie-breaker
   * for equally closely matching converters.
   *
   * @param   tupleType   The tuple type.
   * @param   converters  The converters to select from.
   * @return              A matching converter or <code>null</code>
   *                      if no converter matches.
   * @throws  NullPointerException
   *                      Signals that <code>tupleType</code>, 
   *                      <code>converters</code>, or an entry in 
   *                      <code>converters</code> is <code>null</code>.
   * @throws  IllegalArgumentException
   *                      Signals that <code>tupleType</code> is not a
   *                      tuple type.
   */
  public static Converter select(Class tupleType, Converter[] converters) {
    // Consistency checks.
    if (null == tupleType) {
      throw new NullPointerException("Null tuple type");
    } else if (null == converters) {
      throw new NullPointerException("Null converters");
    } else if (! Tuple.class.isAssignableFrom(tupleType)) {
      throw new IllegalArgumentException("Not a tuple type (" + tupleType +
                                         ")");
    }

    // Find a matching converter by trying to match k and each of its
    // supertypes up to the Tuple type.
    boolean isSuper = false;
    
    do {
      for (int i=0; i<converters.length; i++) {
        Converter c     = converters[i];
        Class[]   types = c.getTupleTypes();

        for (int j=0; j<types.length; j++) {
          if (tupleType.equals(types[j]) &&
              ((! isSuper) || (isSuper && c.convertsSubtypes()))) {
            return c;
          }
        }
      }

      tupleType = tupleType.getSuperclass();
      isSuper   = true;
    } while (! Object.class.equals(tupleType));

    // We found no match.
    return null;
  }

  /**
   * Select an appropriate converter for the specified MIME type from
   * the specified array of converters. This method searches for the
   * closest match and uses the order of the array as a tie-breaker
   * for equally closely matching converters.
   *
   * @param   mimeType    The MIME type.
   * @param   converters  The converters to select from.
   * @return              A matching converter or <code>null</code>
   *                      if no converter matches.
   * @throws  NullPointerException
   *                      Signals that <code>mimeType</code>, 
   *                      <code>converters</code>, or an entry in 
   *                      <code>converters</code> is <code>null</code>.
   * @throws  IllegalArgumentException
   *                      Signals that <code>mimeType</code> is not a
   *                      valid MIME type.
   */
  public static Converter select(String mimeType, Converter[] converters) {
    // Consistency checks.
    if (null == mimeType) {
      throw new NullPointerException("Null MIME type");
    } else if (null == converters) {
      throw new NullPointerException("Null converters");
    } else if (-1 == mimeType.indexOf('/')) {
      throw new IllegalArgumentException("Not a valid MIME type (" +
                                         mimeType + ")");
    }

    // Find a matching converter for the type/subtype.
    for (int i=0; i<converters.length; i++) {
      Converter c     = converters[i];
      String[]  types = c.getMimeTypes();

      for (int j=0; j<types.length; j++) {
        if (mimeType.equals(types[j])) {
          return c;
        }
      }
    }

    // Find a matching converter for the type.
    mimeType = mimeType.substring(0, mimeType.indexOf('/'));
    for (int i=0; i<converters.length; i++) {
      Converter c     = converters[i];
      String[]  types = c.getMimeTypes();

      for (int j=0; j<types.length; j++) {
        if (mimeType.equals(types[j])) {
          return c;
        }
      }
    }

    // Find any converter that handles all MIME types.
    for (int i=0; i<converters.length; i++) {
      Converter c     = converters[i];
      String[]  types = c.getMimeTypes();

      for (int j=0; j<types.length; j++) {
        if ("".equals(types[j])) {
          return c;
        }
      }
    }

    // We found no match.
    return null;
  }

  /** A tuple converter. */
  public static final TupleConverter  TUPLE_CONVERTER  = new TupleConverter();

  /** A class converter. */
  public static final ClassConverter  CLASS_CONVERTER  = new ClassConverter();

  /** A binary converter. */
  public static final BinaryConverter BINARY_CONVERTER = new BinaryConverter();

  /** The converters defined by this package. */
  private static final Converter[] CONVERTERS = new
    Converter[] {
      TUPLE_CONVERTER,
      CLASS_CONVERTER,
      BINARY_CONVERTER
    };

  /**
   * Select an appropriate converter for the specified tuple from
   * the converters implemented by this package. Note that the tuple
   * converter is guaranteed to convert all tuples.
   *
   * @param   t  The tuple to select a converter for.
   * @return     The appropriate converter.
   * @throws  NullPointerException
   *             Signals that <code>t</code> is <code>null</code>.
   */
  public static Converter select(Tuple t) {
    return select(t.getClass(), CONVERTERS);
  }

  /**
   * Select an appropriate converter for the specified MIME type
   * from the converters implemented by this package. Note that the
   * binary converter is guaranteed to convert all MIME types.
   *
   * @param   mimeType  The MIME type.
   * @return            The appropriate converter.
   * @throws  NullPointerException
   *                    Signals that <code>mimeType</code> is
   *                    <code>null</code>.
   */
  public static Converter select(String mimeType) {
    return select(mimeType, CONVERTERS);
  }

  /**
   * Determine whether the specified tuple is relative. This method
   * uses one of the converters implemented in this package to test
   * whether the specified tuple is relative.
   *
   * @see  Converter#isRelative(Tuple)
   */
  public static boolean isRelative(Tuple t) throws TupleException {
    return select(t.getClass(), CONVERTERS).isRelative(t);
  }

  /** 
   * Size the binary data for the specified tuple. This method 
   * uses one of the converters implemented in this package to
   * size the specified tuple.
   *
   * @see  Converter#size(Tuple)
   */
  public static long size(Tuple t) throws TupleException {
    return select(t.getClass(), CONVERTERS).size(t);
  }

  /** 
   * Type the binary data for the specified tuple. This method
   * uses one of the convertes implemented in this package to
   * type the specified tuple.
   *
   * @see  Converter#type(Tuple)
   */
  public static String type(Tuple t) throws TupleException {
    return select(t.getClass(), CONVERTERS).type(t);
  }

  /**
   * Convert the specified tuple to binary data, writing the resulting
   * data to the specified output stream. This method uses one of the
   * converters implemented in this package to convert the specified
   * tuple. Note that the tuple converter is guaranteed to convert all
   * tuples.
   *
   * @see      Converter#convert(Tuple,OutputStream,Converter.TupleReader)
   * @see      TupleConverter
   */
  public static String convert(Tuple t, OutputStream out,
                               Converter.TupleReader reader)
    throws TupleException, IOException {

    return select(t.getClass(), CONVERTERS).convert(t, out, reader);
  }

  /**
   * Convert the data accessible through the specified input stream to
   * one or more tuples. This method uses one of the converters
   * implemented in this package to convert the specified binary
   * data. Note that the binary converter is guaranteed to convert all
   * types of binary data.
   *
   * @see     Converter#convert(InputStream,long,String,String)
   * @see     BinaryConverter
   */
  public static Object convert(InputStream in, long length,
                               String name, String mimeType)
    throws IOException, ClassNotFoundException {

    return select(mimeType, CONVERTERS).convert(in, length, name, mimeType);
  }

}
