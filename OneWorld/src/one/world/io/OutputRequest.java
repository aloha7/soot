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

import one.world.core.EventHandler;
import one.world.core.InvalidTupleException;
import one.world.core.Tuple;
import one.world.core.TupleException;

import one.world.util.TupleEvent;

/**
 * Implementation of an output request. An output request represents a
 * <i>put</i> operation on a tuple store. If a tuple with the same ID
 * as the specified tuple exists in the tuple store, that tuple is
 * overwritten by the specified tuple. If no tuple with the same ID as
 * the specified tuple exists in the tuple store, the specified tuple
 * is added to the tuple store. The transaction is optional, that is,
 * may be <code>null</code>.
 *
 * @see OutputResponse
 *
 * @version  $Revision: 1.11 $
 * @author   Robert Grimm
 */
public class OutputRequest extends TupleEvent {

  /**
   * The transaction for the put operation.
   *
   * @serial
   */
  public EventHandler txn;

  /** Create a new, empty output request. */
  public OutputRequest() {
    // Nothing to do.
  }

  /**
   * Create a new  output request.
   *
   * @param  source    The source for the new output request.
   * @param  closure   The closure for the new output request.
   * @param  tuple     The tuple for the new output request.
   * @param  txn       The transaction for the new output request.
   */
  public OutputRequest(EventHandler source, Object closure, Tuple tuple,
                       EventHandler txn) {
    super(source, closure, tuple);
    this.txn   = txn;
  }
 
  /** Validate this output request. */
  public void validate() throws TupleException {
    super.validate();
    if (null==tuple) {
      throw new InvalidTupleException("Null tuple for output request (" +
                                      this + ")");
    }
  }

}
