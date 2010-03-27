/*
 * Copyright (c) 1999, 2000, University of Washington, Department of
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

import one.world.core.Event;
import one.world.core.EventHandler;

/**
 * Implementation of a join request. Join requests are used by a
 * future transaction participant to join a transaction.
 *
 * <p>Requesting to join a transaction may result in an exceptional
 * event signalling a {@link TransactionException}, particularly a
 * {@link CannotJoinException} or a {@link CrashCountException}.</p>
 *
 * @see      JoinResponse
 * 
 * @version  1.0
 * @author   Robert Grimm
 */
public class JoinRequest extends Event {

  /**
   * The crash count for the participant.
   *
   * @serial
   */
  public long crashCount;

  /** Create a new, empty join request. */
  public JoinRequest() {
    // Nothing to do.
  }

  /**
   * Create a new join request.
   *
   * @param   source      The source for the new join request.
   * @param   closure     The closure for the new join request.
   * @param   crashCount  The crash count for the new join request.
   */
  public JoinRequest(EventHandler source, Object closure, long crashCount) {
    super(source,closure);
    this.crashCount = crashCount;
  }

}
