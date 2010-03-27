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

import de.fub.bytecode.classfile.ClassParser;
import de.fub.bytecode.classfile.JavaClass;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StreamCorruptedException;

import one.util.BufferOutputStream;

import one.world.core.Tuple;
import one.world.core.TupleException;

import one.world.data.ClassData;

/**
 * Implementation of a class converter. A class converter converts
 * between Java class files and class data tuples.
 *
 * @see      ClassData
 *
 * @version  $Revision: 1.5 $
 * @author   Robert Grimm
 */
public final class ClassConverter implements Converter {

  /** The array of tuple types. */
  private Class[]  tupleTypes;

  /** The array of MIME types. */
  private String[] mimeTypes;

  /** Create a new class converter. */
  public ClassConverter() {
    tupleTypes = new Class[]  { ClassData.class              };
    mimeTypes  = new String[] { NetConstants.MIME_TYPE_CLASS };
  }

  /**
   * Get the types of tuples this converter can convert to binary
   * data, which are class data tuples.
   */
  public Class[] getTupleTypes() {
    return tupleTypes;
  }

  /**
   * Determine whether this converter can convert subtypes of the
   * specified tuple types, which it can.
   */
  public boolean convertsSubtypes() {
    return true;
  }

  /**
   * Get the MIME types this converter can convert to tuples, which
   * is "<code>application/x-java-vm</code>".
   *
   * @see NetConstants#MIME_TYPE_CLASS
   */
  public String[] getMimeTypes() {
    return mimeTypes;
  }

  /**
   * Determine whether the specified tuple is relative. This method
   * always returns <code>false</code>.
   */
  public boolean isRelative(Tuple t) {
    return false;
  }

  /** Size the specified tuple. */
  public long size(Tuple t) throws TupleException {
    if (null == t) {
      throw new NullPointerException("Null tuple");
    } else if (! (t instanceof ClassData)) {
      throw new IllegalArgumentException("Not a class data tuple (" + t + ")");
    }

    ClassData cd = (ClassData)t;
    cd.validate();
    return cd.data.length;
  }
  
  /** Type the specified tuple. */
  public String type(Tuple t) throws TupleException {
    if (null == t) {
      throw new NullPointerException("Null tuple");
    } else if (! (t instanceof ClassData)) {
      throw new IllegalArgumentException("Not a class data tuple (" + t + ")");
    }

    return NetConstants.MIME_TYPE_CLASS;
  }

  /**
   * Convert the specified tuple to binary data, writing the resulting
   * data to the specified output stream.
   */
  public String convert(Tuple t, OutputStream out, Converter.TupleReader reader)
    throws TupleException, IOException {

    // Consistency checks.
    if (null == t) {
      throw new NullPointerException("Null tuple");
    } else if (null == out) {
      throw new NullPointerException("Null output stream");
    } else if (! (t instanceof ClassData)) {
      throw new IllegalArgumentException("Not a class data tuple (" + t + ")");
    }

    // Write out the class data tuple.
    ClassData cd = (ClassData)t;
    cd.validate();
    out.write(cd.data);
    out.flush();

    return NetConstants.MIME_TYPE_CLASS;
  }

  /**
   * Convert the data accessible through the specified input stream to
   * a class data tuple. The name of the returned class data tuple is
   * the fully qualified class name of the Java class.
   */
  public Object convert(InputStream in, long length,
                        String name, String mimeType)
    throws IOException {

    // Consistency checks.
    if (null == in) {
      throw new NullPointerException("Null input stream");
    } else if (null == mimeType) {
      throw new NullPointerException("Null MIME type");
    } else if (! NetConstants.MIME_TYPE_CLASS.equals(mimeType)) {
      throw new IllegalArgumentException("MIME type not application/x-java-" +
                                         "vm (" + mimeType + ")");
    } else if (0 > length) {
      throw new IllegalArgumentException("Negative length (" + length + ")");
    } else if (Integer.MAX_VALUE < length) {
      throw new IllegalArgumentException("Too much data for class data " +
                                         "tuple (" + length + ")");
    }

    // Pump the bytes from the input stream into a buffer and create a
    // new class data tuple with that buffer.
    BufferOutputStream out    = new BufferOutputStream((int)length);
    long               pumped = NetUtilities.pump(in, out, length);

    if (pumped != length) {
      throw new EOFException("Unexpected end-of-file after " + pumped + 
                             " bytes");
    } else {
      ClassData cd = new ClassData(null, out.getBytes());

      // Patch name.
      ClassParser parser = new ClassParser(new ByteArrayInputStream(cd.data),
                                           "Some class");
      JavaClass   klass;

      try {
        klass = parser.parse();
      } catch (ClassFormatError x) {
        throw new StreamCorruptedException(x.getMessage());
      }

      cd.name = klass.getClassName();

      // Done.
      return cd;
    }
  }

}
