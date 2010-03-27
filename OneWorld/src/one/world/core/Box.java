/*
 * Copyright (c) 2001, University of Washington, Department of
 * Computer Science and Engineering.
 * All rights reserved.
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

package one.world.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

/**
 * Implementation of a box. A box is a container for a serialized
 * object that can be used to embed an arbritrary object in a
 * tuple. The contents of a box can be retrieved using the {@link
 * Component#unbox} method.
 *
 * @see      Tuple
 *
 * @version  $Revision: 1.1 $
 * @author   Robert Grimm
 */
public class Box implements java.io.Serializable {

  /** The serial version ID for this class. */
  static final long serialVersionUID = -955505327794930507L;

  /**
   * The binary representation of the object encapsulated by this
   * box.
   *
   * @serial  Must be a valid serialized object.
   */
  byte[] bytes;

  /**
   * The hash code for this box.
   *
   * @serial  Must be the sum of all bytes, where the intermediate sum
   *          is shifted left by 8 bits before each step.
   */
  int   hash;

  /**
   * Create a new box with the specified object. The specified object
   * must be serializable. It may be <code>null</code>.
   *
   * @param   o  The object for the new box.
   * @throws  IOException
   *             Signals an exceptional condition while serializing
   *             the specified object.
   */
  public Box(Object o) throws IOException {
    ByteArrayOutputStream b     = new ByteArrayOutputStream();
    ObjectOutputStream    out   = new ObjectOutputStream(b);

    out.writeObject(o);
    out.flush();
    out.close();

    bytes = b.toByteArray();
    hash  = 0;

    for (int i=0; i<bytes.length; i++) {
      hash = (hash << 8) + bytes[i];
    }
  }

  /**
   * Determine whether this box equals the specified object. This box
   * equals the specified object, if the specified object also is a
   * box containing an object whose serialized representation is the
   * same as the serialized representation of the object carried by
   * this box.
   */
  public boolean equals(Object o) {
    if (this == o) return true;
    if (! (o instanceof Box)) return false;
    Box other = (Box)o;
    if (bytes.length != other.bytes.length) return false;
    if (hash != other.hash) return false;
    for (int i=0; i<bytes.length; i++) {
      if (bytes[i] != other.bytes[i]) {
        return false;
      }
    }
    return true;
  }
  
  /** Return a hash code for this box. */
  public int hashCode() {
    return hash;
  }

}
