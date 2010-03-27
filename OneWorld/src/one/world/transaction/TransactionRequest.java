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

package one.world.transaction;

import one.world.util.TypedEvent;

import one.world.core.EventHandler;
import one.world.core.InvalidTupleException;
import one.world.core.TupleException;

/**
 * Implementation of a transaction request. Transaction requests are
 * used by transaction clients to request operations on a
 * transaction. The successful completion of a transaction request is
 * signalled by a transaction state event.
 *
 * <p>Requesting an operation on a transaction may result in an
 * exceptional event signalling a {@link TransactionException}. In
 * particular, committing a transaction may result in a {@link
 * CannotCommitException} and aborting may result in a {@link
 * CannotAbortException}.</p>
 *
 * @see      TransactionState
 *
 * @version  $Revision: 1.5 $
 * @author   Robert Grimm
 */
public class TransactionRequest extends TypedEvent {

  /** The type code for requesting to abort a transaction. */
  public static final int  ABORT  = 1;

  /** The type code for requesting to commit a transaction. */
  public static final int  COMMIT = 2;

  /** The type code for requesting the state of a transaction. */
  public static final int  STATE  = 3;

  /** Create a new, empty transaction request. */
  public TransactionRequest() {
    // Nothing to do.
  }

  /**
   * Create a new  transaction request.
   *
   * @param  source      The source for the new transaction request.
   * @param  closure     The closure for the new transaction request.
   * @param  type        The type for the new transaction request.
   */
  public TransactionRequest(EventHandler source, Object closure, int type) {
    super(source, closure, type);
  }

  /** Validate this transaction request. */
  public void validate() throws TupleException {
    super.validate();
    if ((ABORT > type) || (STATE < type)) {
      throw new InvalidTupleException("Invalid type (" + type + ") " +
                                      "for transaction request (" + this + ")");
    }
  }

}
