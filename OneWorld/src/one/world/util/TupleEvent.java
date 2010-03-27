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

package one.world.util;

import one.world.core.Event;
import one.world.core.EventHandler;
import one.world.core.Tuple;
import one.world.core.TupleException;

/**
 * Implementation of a tuple event. A tuple event communicates
 * a tuple.
 *
 * @version  $Revision: 1.6 $
 * @author   Robert Grimm 
 */
public class TupleEvent extends Event {

  /** The serial version ID for this class. */
  static final long serialVersionUID = -4855979705836182783L;

  /**
   * The tuple for this tuple event.
   *
   */
  public Tuple tuple;

  /** Create a new, empty tuple event. */
  public TupleEvent() {
    // Nothing to do.
  }

  /**
   * Create a new tuple event.
   *
   * @param   source   The source for the new tuple event.
   * @param   closure  The closure for the new tuple event.
   * @param   tuple    The resulting tuple for the new tuple event.
   */
  public TupleEvent(EventHandler source, Object closure, Tuple tuple) {
    super(source, closure);
    this.tuple = tuple;
  }

  /** Validate this tuple event. */
  public void validate() throws TupleException {
    super.validate();
    if (null != tuple) {
      tuple.validate();
    }
  }

}
