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
import one.world.core.TupleException;

import one.world.util.TypedEvent;

/**
 * Implementation of an option event. Option events are used to set
 * and get options for tuple storage. The options for a particular
 * tuple store are always accessed as a whole and are persistently
 * associated with that tuple store. When setting options for a tuple
 * store, it is good practice to first retrieve the current options,
 * modify them, and then set the modified options.
 *
 * @version  $Revision: 1.3 $
 * @author   Robert Grimm
 */
public class OptionEvent extends TypedEvent {

  /** The type code for a set options request. */
  public static final int SET     = 1;

  /** The type code for a get options request. */
  public static final int GET     = 2;

  /**
   * The type code for an options response, which confirms both set
   * and get option requests.
   */
  public static final int OPTIONS = 3;

  /**
   * The durability flag. If a tuple store's durability flag is
   * <code>true</code> all modifications, that is, all writes and
   * deletes, are immediately written to persistent storage. If the
   * flag is <code>false</code>, modifications may be collected in
   * memory and flushed to persistent storage in periodic
   * intervals. {@link FlushEvent}s can be used to force this
   * flushing. The default for newly created tuple stores is
   * <code>true</code>.
   *
   * @serial
   */
  public boolean durable;

  /** Create a new, empty option event. */
  public OptionEvent() {
    // Nothing to do.
  }

  /**
   * Create a new option event.
   *
   * @param  source    The source for the new option event.
   * @param  closure   The closure for the new option event.
   * @param  type      The type for the new option event.
   * @param  durable   The durability flag for the new option event.
   */
  public OptionEvent(EventHandler source, Object closure, int type,
                     boolean durable) {
    super(source, closure, type);
    this.durable = durable;
  }

  /** Validate this option event. */
  public void validate() throws TupleException {
    super.validate();
    if ((SET > type) || (OPTIONS < type)) {
      throw new InvalidTupleException("Invalid type (" + type +
                                      ") for option event (" + this + ")");
    }
  }

}
