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
 * Implementation of a transaction state event. Transaction state
 * events are used to notify of the completion of a transaction
 * request or a participant request. They are also used to notify of
 * the state of a transaction in response to a get state transaction
 * request.
 *
 * @see      TransactionRequest
 * @see      ParticipantRequest
 *
 * @version  $Revision: 1.5 $
 * @author   Robert Grimm
 */
public class TransactionState extends TypedEvent {

  /** The type code for the active state. */
  public static final int  ACTIVE      = 1;

  /** The type code for the voting state. */
  public static final int  VOTING      = 2;

  /** The type code for the not changed state. */
  public static final int  NOT_CHANGED = 3;

  /** The type code for the aborted state. */
  public static final int  ABORTED     = 4;

  /** The type code for the prepared state. */
  public static final int  PREPARED    = 5;

  /** The type code for the committed state. */
  public static final int  COMMITTED   = 6;

  /** Create a new, empty transaction state event. */
  public TransactionState() {
    // Nothing to do.
  }

  /**
   * Create a new transaction state event.
   *
   * @param   source    The source for the new transaction state event.
   * @param   closure   The closure for the new transaction state event.
   * @param   type      The type for the new transaction state event.
   */
  public TransactionState(EventHandler source, Object closure, int type) {
    super(source, closure, type);
  }

  /** Validate this transaction state event. */
  public void validate() throws TupleException {
    super.validate();
    if ((ACTIVE > type) || (COMMITTED < type)) {
      throw new InvalidTupleException("Invalid type (" + type + ") " +
                                      "for transaction state event (" +
                                      this + ")");
    }
  }

}
