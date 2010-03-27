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
import java.io.InvalidObjectException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import one.world.core.Tuple;
import one.world.core.TupleException;

/**
 * Implementation of a tuple converter. A tuple conterter converts
 * between arbitrary tuples and streams of serialized
 * tuples. Currently, this class uses Java's binary serialization
 * format as the outside representation (the
 * "<code>application/x-one-world-btpl</code>" MIME type), though this
 * will be changed to an XML-based format.
 *
 * @version  $Revision: 1.7 $
 * @author   Robert Grimm
 */
public final class TupleConverter implements Converter {

  /** The array of tuple types. */
  private Class[]  tupleTypes;

  /** The array of MIME types. */
  private String[] mimeTypes;

  /** Create a new tuple converter. */
  public TupleConverter() {
    tupleTypes = new Class[]  { Tuple.class                   };
    mimeTypes  = new String[] { NetConstants.MIME_TYPE_BTUPLE };
  }

  /**
   * Get the types of tuples this converter can convert to binary
   * data, which are all tuple types.
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
   * is the "<code>application/x-one-world-btpl</code>" MIME type.
   *
   * @see  NetConstants#MIME_TYPE_BTUPLE
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

  /** Size the specified tuple. This method always returns -1. */
  public long size(Tuple t) {
    return -1;
  }
  
  /**
   * Type the specified tuple. This method always return the
   * "<code>application/x-one-world-btpl</code>" MIME type.
   *
   * @see  NetConstants#MIME_TYPE_BTUPLE
   */
  public String type(Tuple t) {
    return NetConstants.MIME_TYPE_BTUPLE;
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
    }

    t.validate();

    // Write out the tuple.
    ObjectOutputStream out2 = new ObjectOutputStream(out);
    out2.writeObject(t);
    out2.flush();

    return NetConstants.MIME_TYPE_BTUPLE;
  }

  /**
   * Convert the data accessible through the specified input stream to
   * a tuple.
   */
  public Object convert(InputStream in, long length,
                        String name, String mimeType)
    throws IOException, ClassNotFoundException {

    // Consistency checks.
    if (null == in) {
      throw new NullPointerException("Null input stream");
    } else if (null == mimeType) {
      throw new NullPointerException("Null MIME type");
    } else if (0 > length) {
      throw new IllegalArgumentException("Negative length (" + length + ")");
    } else if (! NetConstants.MIME_TYPE_BTUPLE.equals(mimeType)) {
      throw new IllegalArgumentException("MIME type not application/x-one" +
                                         "-world-btpl (" + mimeType + ")");
    }

    // Read in the tuple.
    ObjectInputStream in2 = new ObjectInputStream(in);
    Object            o   = in2.readObject();

    if (! (o instanceof Tuple)) {
      throw new InvalidObjectException("Not a tuple (" + o + ")");
    } else {
      return (Tuple)o;
    }
  }

}
