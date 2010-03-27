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

package one.world.env;

import one.util.Guid;

import one.world.core.Event;
import one.world.core.EventHandler;
import one.world.core.InvalidTupleException;
import one.world.core.TupleException;

import one.world.util.IdEvent;

/**
 * Implementation of a restore request. Restore requests ask for the
 * restoring of an environment from a saved check-point.
 *
 * @see      EnvironmentEvent#RESTORED
 * @see      one.world.core.Environment#restore(Guid,Guid,long)
 *
 * @version  $Revision: 1.7 $
 * @author   Robert Grimm
 */
public final class RestoreRequest extends IdEvent {

  /**
   * The timestamp for the check-point to be restored, or -1
   * if the latest check-point is to be used.
   *
   * @serial  Must be a non-negative number or -1.
   */
  public long timestamp;

  /**
   * Create a new, empty restore request.
   */
  public RestoreRequest() {
    // Nothing to do.
  }

  /**
   * Create a new restore request.
   *
   * @param   source     The source for the new restore request.
   * @param   closure    The closure for the new restore request.
   * @param   ident      The environment ID for the new restore request.
   * @param   timestamp  The timestamp for the new restore request.
   */
  public RestoreRequest(EventHandler source, Object closure,
                       Guid ident, long timestamp) {
    super(source, closure, ident);
    this.timestamp = timestamp;
  }

  /** Validate this restore request. */
  public void validate() throws TupleException {
    super.validate();

    if (null == ident ) {
      throw new InvalidTupleException("Null ident for restore request (" +
                                      this + ")");
    }

    if (-1 > timestamp) {
      throw new InvalidTupleException("Invalid timestamp (" + timestamp +
                                      ") for restore request (" + this + ")");
    }
  }

  /** Get a string representation of this restore request. */
  public String toString() {
    return "#[restore env " + ident + " to " + timestamp + "]";
  }

}
