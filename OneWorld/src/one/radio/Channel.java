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

package one.radio;

import one.util.Guid;

import one.world.core.Tuple;
import one.world.core.TupleException;
import one.world.core.InvalidTupleException;

/**
 * Implementation of a channel descriptor.
 *
 * @version  $Revision: 1.3 $
 * @author   Robert Grimm
 */
public class Channel extends Tuple {

  /** The serial version ID for this class. */
  static final long serialVersionUID = -7405776811934959492L;

  /** 
   * The name of the channel.
   *
   * @serial  Must not be <code>null</code>
   */
  public String name;

  /** Create a new, empty channel descriptor. */
  public Channel() {
    // Nothing to do.
  }
 
  /**
   * Create a new channel descriptor with the specified name.
   *
   * @param  name  The name for the new channel descriptor.
   */
  public Channel(String name) {
    this.name = name;
  } 

  /**
   * Create a new channel descriptor with the specified ID and name.
   *
   * @param  id    The ID for the new channel descriptor.
   * @param  name  The name for the new channel descriptor.
   */
  public Channel(Guid id, String name) {
    super(id);
    this.name = name;
  } 

  /** Validate this channel descriptor. */
  public void validate() throws TupleException {
    super.validate();
    if (null == name) {
      throw new InvalidTupleException("Null name for channel descriptor (" +
                                      this + ")");
    }
  }

}


