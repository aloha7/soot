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
 * Implementation of a participant request. Participants requests are
 * used by transaction managers to request operations from a
 * transaction participant. The successful completion of a participant
 * request is signalled by a transaction state event.
 *
 * <p>Requesting an operation from a participant may result in an
 * exceptional event signalling a {@link TransactionException},
 * particularly a {@link UnknownTransactionException}.</p>
 *
 * @see      TransactionState
 *
 * @version  $Revision: 1.5 $
 * @author   Robert Grimm
 */
public class ParticipantRequest extends TypedEvent {

  /** The type code for requesting to prepare. */
  public static final int PREPARE            = 1;

  /** The type code for requesting to prepare and commit. */
  public static final int PREPARE_AND_COMMIT = 2;

  /** The type code for requesting to commit. */
  public static final int COMMIT             = 3;

  /** The type code for requesting to abort. */
  public static final int ABORT              = 4;

  /** Create a new, empty participant request. */
  public ParticipantRequest() {
    // Nothing to do.
  }

  /**
   * Create a new  participant request.
   *
   * @param  source      The source for the new participant request.
   * @param  closure     The closure for the new participant request.
   * @param  type        The type for the new participant request.
   */
  public ParticipantRequest(EventHandler source, Object closure, int type) {
    super(source, closure, type);
  }

  /** Validate this participant request. */
  public void validate() throws TupleException {
    super.validate();
    if ((PREPARE > type) || (ABORT < type)) {
      throw new InvalidTupleException("Invalid type (" + type + ") " +
                                      "for participant request (" + this + ")");
    }
  }

}
