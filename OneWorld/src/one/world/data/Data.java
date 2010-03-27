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
import one.world.core.Tuple;
import one.world.core.TupleException;

/**
 * The superclass of data containers that are named and typed.
 *
 * @version  $Revision: 1.5 $
 * @author   Eric Lemar and Robert Grimm
 */
public abstract class Data extends Tuple {

  /** The serial version ID for this class. */
  static final long serialVersionUID = 4056628114678782502L;

  /** 
   * The name of the data.
   *
   * @serial  Must not be <code>null</code>
   */
  public String name;

  /**
   * The type of the data, which must be a valid MIME type.
   *
   * @see     one.net.MimeTypes
   *
   * @serial  Must not be <code>null</code>.
   */
  public String type;

  /** Create a new, empty data tuple. */
  public Data() {
    // Nothing to do.
  }
 
  /**
   * Create a new data tuple with the specified name and type.
   *
   * @param  name  The name for the new data tuple.
   * @param  type  The MIME type for the new data tuple.
   */
  public Data(String name, String type) {
    this.name = name;
    this.type = type;
  } 

  /**
   * Create a new data tuple with the specified ID, name, and type.
   *
   * @param  id    The ID for the new data tuple.
   * @param  name  The name for the new data tuple.
   * @param  type  The MIME type for the new data tuple.
   */
  public Data(Guid id, String name, String type) {
    super(id);
    this.name = name;
    this.type = type;
  } 

  /** Validate this data tuple. */
  public void validate() throws TupleException {
    super.validate();
    if (null == name) {
      throw new InvalidTupleException("Null name for data tuple ("+this+")");
    } else if (null == type) {
      throw new InvalidTupleException("Null type for data tuple ("+this+")");
    }
  }

}


