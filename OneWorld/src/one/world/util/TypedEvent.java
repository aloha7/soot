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

/**
 * Abstract base class for typed events. Typed events are events that
 * include a type code to distinguish between several options for the
 * same class of events.
 *
 * @version  $Revision: 1.2 $
 * @author   Robert Grimm
 */
public abstract class TypedEvent extends Event {

  /** The serial version ID for this class. */
  static final long serialVersionUID = -4313425021729997221L;  

  /**
   * The type for this typed event.
   *
   * @serial  Must be a valid type code as specified by the concrete
   *          subclass.
   */
  public int type;

  /**
   * Create a new, empty typed event.
   */
  public TypedEvent() {
    // Nothing to do.
  }

  /**
   * Create a new typed event. This constructor creates a new typed
   * event with the specified source, closure, and type. The ID field
   * is assigned a new GUID.
   *
   * @param   source   The source for the new typed event.
   * @param   closure  The closure for the new typed event.
   * @param   type     The type for the new typed event.
   */
  public TypedEvent(EventHandler source, Object closure, int type) {
    super(source, closure);
    this.type = type;
  }

}
