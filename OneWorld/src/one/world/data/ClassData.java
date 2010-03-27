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

import one.net.NetConstants;

import one.world.core.InvalidTupleException;
import one.world.core.TupleException;

/**
 * Implementation of a class data tuple. A class data tuple holds the
 * data of a Java class file. Its name is the fully qualified class
 * name and its MIME type is {@link NetConstants#MIME_TYPE_CLASS}.
 *
 * @version  $Revision: 1.3 $
 * @author   Robert Grimm
 */
public class ClassData extends BinaryData {

  /** Create a new, empty class data tuple. */
  public ClassData() {
    // Nothing to do.
  }

  /**
   * Create a new class data tuple with the specified name and
   * data.
   *
   * @param  name  The name for the new class data tuple.
   * @param  data  The data for the new class data tuple, which is 
   *               <i>not</i> copied.
   */
  public ClassData(String name, byte[] data) {
    super(name, NetConstants.MIME_TYPE_CLASS, data);
  } 

  /** Validate this class data tuple. */
  public void validate() throws TupleException {
    super.validate();
    if (! NetConstants.MIME_TYPE_CLASS.equals(type)) {
      throw new InvalidTupleException("Invalid MIME type (" + type +
                                      ") for class data tuple (" + this + ")");
    }
  }

  /** Get a string representation for this class data tuple. */
  public String toString() {
    return "#[Java class data for " + name + "]";
  }
}
