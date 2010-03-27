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

package one.world.io;

import one.world.util.IdEvent;

import one.world.core.EventHandler;
import one.world.core.InvalidTupleException;
import one.world.core.TupleException;

import one.util.Guid;

/**
 * Implementation of a request to remove a pending input request from a
 * {@link PendingInputRequests} manager.  The {@link #ident} field is the
 * ID of the tuple to remove.
 *
 * @version  $Revision: 1.6 $
 * @author   Janet Davis
 */
public class RemovePendingRequest extends IdEvent {

  /** Create a new, empty request to remove a pending input request. */
  public RemovePendingRequest() {
    // Nothing to do.
  }

  /**
   * Create a new request to remove a pending input request.
   *
   * @param   source   The source for the new request.
   * @param   closure  The closure for the new request.
   * @param   ident    The tuple ID for the new request.
   */
  public RemovePendingRequest(EventHandler source, Object closure, Guid ident) {
    super(source, closure, ident);
  }

  /** Validate this remove PendingRequest. */
  public void validate() throws TupleException {
    super.validate();
    if (null == ident ) {
      throw new InvalidTupleException("Null ident for RemovePendingRequest (" +
                                      this + ")");
    }
  }
}
