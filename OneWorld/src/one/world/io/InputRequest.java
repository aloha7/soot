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

import one.world.binding.Duration;

import one.world.core.EventHandler;
import one.world.core.InvalidTupleException;
import one.world.core.Tuple;
import one.world.core.TupleException;

import one.world.util.TypedEvent;

/**
 * Implementation of an input request. An input request requests to
 * either read some tuple matching the specified query from a tuple
 * store, to repeatedly read incoming, that is, newly added, tuples
 * matching the specified query from a tuple store, or to read all
 * tuples currently matching the specified query from a tuple
 * store. For <i>read</i> operations, the duration is the time-out if
 * the request cannot be satisfied immediately. For <i>listen</i>
 * operations, the duration is the requested lease duration for the
 * lease on the listener. For <i>query</i> operations, the duration is
 * the requested lease duration for the lease on the result
 * iterator. The <code>idOnly</code> flag specifies whether to return
 * the actual tuples or only their IDs. The transaction is optional,
 * that is, may be <code>null</code>. If a <i>read</i> operation
 * cannot be satisfied within the specified time-out, a
 * <code>NoSuchTupleException</code> is signalled.
 *
 * @see      InputResponse
 * @see      InputByIdResponse
 * @see      QueryResponse
 * @see      ListenResponse
 * @see      NoSuchTupleException
 *
 * @version  $Revision: 1.11 $
 * @author   Robert Grimm
 */
public class InputRequest extends TypedEvent {

  /** The type code for a read request. */
  public static final int READ   = 1;

  /** The type code for a listen request. */
  public static final int LISTEN = 2;

  /** The type code for a query request. */
  public static final int QUERY  = 3;

  /**
   * The query for this input request.
   *
   * @serial  Must not be <code>null</code>.
   */
  public Query query;

  /**
   * The duration for this input request.
   *
   * @see     Duration
   *
   * @serial  Must be a valid duration.
   */
  public long  duration;

  /**
   * The flag for whether to only return the tuple ID(s).
   *
   * @serial
   */
  public boolean idOnly;

  /**
   * The transaction for the read, listen, or query operation.
   *
   * @serial
   */
  public EventHandler txn;

  /** Create a new, empty input request. */
  public InputRequest() {
    // Nothing to do.
  }

  /**
   * Create a new input request.
   *
   * @param  source    The source for the new input request.
   * @param  closure   The closure for the new input request.
   * @param  type      The type for the new input request.
   * @param  query     The query for the new input request.
   * @param  duration  The duration for the new input request.
   * @param  idOnly    The ID only flag for the new input request.
   * @param  txn       The transaction for the new input request.
   */
  public InputRequest(EventHandler source, Object closure, int type,
                      Query query, long duration, boolean idOnly,
                      EventHandler txn) {
    super(source, closure, type);
    this.query    = query;
    this.duration = duration;
    this.idOnly   = idOnly;
    this.txn      = txn;
  }

  /** Validate this input request. */
  public void validate() throws TupleException {
    super.validate();
    if ((READ > type) || (QUERY < type)) {
      throw new InvalidTupleException("Invalid type (" + type +
                                      ") for input request (" + this + ")");
    } else if (null == query) {
      throw new InvalidTupleException("Null query for input request (" + this +
                                      ")");
    } else if (Duration.ANY > duration) {
      throw new InvalidTupleException("Invalid duration (" + duration +
                                      ") for input request (" + this + ")");
    }
    query.validate();
  }

}
