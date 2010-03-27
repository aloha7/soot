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

package one.world.data;

import one.util.Guid;

import one.world.core.InvalidTupleException;
import one.world.core.TupleException;

/**
 * Implementation of a binary data tuple. A binary data tuple holds an
 * arbitrary byte array.
 *
 * @version  $Revision: 1.8 $
 * @author   Eric Lemar and Robert Grimm
 */
public class BinaryData extends Data {

  /** The serial version ID for this class. */
  static final long serialVersionUID = 5407195492932135038L;

  /**
   * The actual data.
   *
   * @serial  Must not be <code>null</code>.
   */
  public byte[] data;

  /** Create a new, empty binary data tuple. */
  public BinaryData() {
    // Nothing to do.
  }

  /**
   * Create a new binary data tuple with the specified name, type, and
   * data.
   *
   * @param  name  The name for the new binary data tuple.
   * @param  type  The MIME type for the new binary data tuple.
   * @param  data  The data for the new binary data tuple, which is 
   *               <i>not</i> copied.
   */
  public BinaryData(String name, String type, byte[] data) {
    super(name, type);
    this.data = data;
  } 

  /**
   * Create a new binary data tuple with the specified ID, name, type,
   * and data.
   *
   * @param  id    The ID for the new binary data tuple.
   * @param  name  The name for the new binary data tuple.
   * @param  type  The MIME type for the new binary data tuple.
   * @param  data  The data for the new binary data tuple, which is 
   *               <i>not</i> copied.
   */
  public BinaryData(Guid id, String name, String type, byte[] data) {
    super(id, name, type);
    this.data = data;
  } 

  /** Validate this binary data tuple. */
  public void validate() throws TupleException {
    super.validate();
    if (null == data) {
      throw new InvalidTupleException("Null actual data for binary data " +
                                      "tuple (" + this + ")");
    }
  }

  /** Get a string representation for this binary data tuple. */
  public String toString() {
    if (null == data) {
      return "#[BinaryData name=\"" + name + "\" type=\"" + type +
        "\" size=<none>]";
    } else {
      return "#[BinaryData name=\"" + name + "\" type=\"" + type +
        "\" size=" + data.length + "]";
    }
  }
}
