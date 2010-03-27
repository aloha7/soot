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

import one.util.Guid;

import one.world.core.Event;
import one.world.core.EventHandler;
import one.world.core.InvalidTupleException;
import one.world.core.TupleException;

/**
 * Implementation of an IDs event. An IDs event communicates one or
 * more GUIDs.
 *
 * @version  $Revision: 1.3 $
 * @author   Robert Grimm 
 */
public class IdsEvent extends Event {

  /**
   * The IDs for this IDs event.
   *
   * @serial  Must be a non-<code>null</code> array with
   *          non-<code>null</code> entries.
   */
  public Guid[] idents;

  /** Create a new, empty IDs event. */
  public IdsEvent() {
    // Nothing to do.
  }

  /**
   * Create a new IDs event.
   *
   * @param   source   The source for the new IDs event.
   * @param   closure  The closure for the new IDs event.
   * @param   ident    The single ID for the new IDs event.
   */
  public IdsEvent(EventHandler source, Object closure, Guid ident) {
    super(source, closure);
    this.idents = new Guid[] { ident };
  }

  /**
   * Create a new IDs event.
   *
   * @param   source   The source for the new IDs event.
   * @param   closure  The closure for the new IDs event.
   * @param   idents   The array of IDs for the new IDs event.
   */
  public IdsEvent(EventHandler source, Object closure, Guid[] idents) {
    super(source, closure);
    this.idents = idents;
  }

  /** Validate this IDs event. */
  public void validate() throws TupleException {
    super.validate();
    if (null == idents) {
      throw new InvalidTupleException("Null identifier array for IDs event (" +
                                      this + ")");
    }
    for (int i=0; i<idents.length; i++) {
      if (null == idents[i]) {
        throw new InvalidTupleException("Null identifier for IDs event (" +
                                        this + ")");
      }
    }
  }

}
