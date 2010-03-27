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

import one.util.Guid;

import one.world.core.EventHandler;
import one.world.core.InvalidTupleException;
import one.world.core.TupleException;

import one.world.util.IdEvent;

/**
 * Implementation of a delete request. A delete request deletes the
 * tuple with the specified ID from a tuple store. The transaction is
 * optional, that is, may be <code>null</code>. If no tuple with the
 * specified ID exists in a tuple store, a
 * <code>NoSuchTupleException</code> is signalled.
 *
 * @see      OutputResponse
 * @see      NoSuchTupleException
 *
 * @version  $Revision: 1.10 $
 * @author   Robert Grimm
 */
public class DeleteRequest extends IdEvent {

  /**
   * The transaction for the delete operation.
   *
   * @serial
   */
  public EventHandler txn;

  /** Create a new, empty delete request. */
  public DeleteRequest() {
    // Nothing to do.
  }

  /**
   * Create a new delete request.
   *
   * @param   source   The source for the new delete request.
   * @param   closure  The closure for the new delete request.
   * @param   ident    The tuple ID for the new delete request.
   * @param   txn      The transaction for the new delete request.
   */
  public DeleteRequest(EventHandler source, Object closure,
                       Guid ident, EventHandler txn) {
    super(source, closure, ident);
    this.txn     = txn;
  }

  /** Validate this output response. */
  public void validate() throws TupleException {
    super.validate();
    if (null==ident) {
      throw new InvalidTupleException("Null ident for delete request (" + this +
                                      ")");
    }
  }


}
