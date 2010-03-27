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

import one.world.util.TypedEvent;

import one.world.core.EventHandler;
import one.world.core.InvalidTupleException;
import one.world.core.TupleException;

import one.world.binding.Duration;

/**
 * Implementation of a flush event. Flush events are used to request
 * and confirm the flushing of a tuple store, whose durability flag is
 * <code>false</code>. Flush requests on tuple stores, whose
 * durability flag is <code>true</code>, have no effect and are
 * immediately confirmed.
 *
 * @see      OptionEvent
 *
 * @version  $Revision: 1.3 $
 * @author   Robert Grimm
 */
public class FlushEvent extends TypedEvent {

  /** The type code for a flush request. */
  public static final int REQUEST  = 1;

  /** The type code for a flush response. */
  public static final int RESPONSE = 2;

  /** Create a new, empty flush event. */
  public FlushEvent() {
    // Nothing to do.
  }

  /**
   * Create a new flush event.
   *
   * @param  source    The source for the new flush event.
   * @param  closure   The closure for the new flush event.
   * @param  type      The type for the new flush event.
   */
  public FlushEvent(EventHandler source, Object closure, int type) {
    super(source, closure, type);
  }

  /** Validate this flush event. */
  public void validate() throws TupleException {
    super.validate();
    if ((REQUEST != type) && (RESPONSE != type)) {
      throw new InvalidTupleException("Invalid type (" + type + ") " +
                                      "for flush event (" + this + ")");
    }
  }

}
