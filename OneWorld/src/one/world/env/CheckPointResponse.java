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

import one.world.core.EventHandler;
import one.world.core.InvalidTupleException;
import one.world.core.TupleException;

import one.world.util.IdEvent;

/**
 * Implementation of a check-point response. Check-point responses
 * confirm a check-point. They specify the local root of the
 * environment subtree that was check-pointed as well as the timestamp
 * of the check-point.
 *
 * @see      EnvironmentEvent#CHECK_POINT
 *
 * @version  $Revision: 1.5 $
 * @author   Robert Grimm
 */
public final class CheckPointResponse extends IdEvent {

  /**
   * The timestamp of the check-point.
   *
   * @serial  Must not be negative.
   */
  public long timestamp;

  /**
   * Create a new, empty check-point response.
   */
  public CheckPointResponse() {
    // Nothing to do.
  }

  /**
   * Create a new check-point response.
   *
   * @param   source     The source for the new check-point response.
   * @param   closure    The closure for the new check-point response.
   * @param   ident      The environment ID for the new check-point response.
   * @param   timestamp  The timestamp for the new check-point response.
   */
  public CheckPointResponse(EventHandler source, Object closure,
                            Guid ident, long timestamp) {
    super(source, closure, ident);
    this.timestamp = timestamp;
  }

  /** Validate this check-point response. */
  public void validate() throws TupleException {
    super.validate();
    if (null == ident ) {
      throw new InvalidTupleException("Null ident for check-point request (" +
                                      this + ")");
    }
    if (0 > timestamp) {
      throw new InvalidTupleException("Negative timestamp for check-point " +
                                      "response (" + this + ")");
    }
  }

  /** Get a string representation of this check-point response. */
  public String toString() {
    return "#[check-pointed env " + ident + " at " + timestamp + "]";
  }

}
