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

package one.world.util;

import one.world.core.EventHandler;
import one.world.core.Event;

/**
 * Implementation of an event handler application. An event handler
 * application references an event handler and an event that the
 * event handler can be applied to.
 *
 * @version  1.0
 * @author   Robert Grimm
 */
public class HandlerApplication implements Runnable, java.io.Serializable {
  
  /**
   * The event handler for this event handler application.
   *
   * @serial  Must not be <code>null</code>.
   */
  private EventHandler handler;
  
  /**
   * The event for this event handler application.
   *
   * @serial  Must not be <code>null</code>.
   */
  private Event        event;
  
  /**
   * Create a new event handler application with the specified event
   * handler and event.
   *
   * @param   handler  The event handler for the new event handler
   *                   application.
   * @param   event    The event for the new event handler application.
   * @throws  NullPointerException
   *                   Signals that <code>handler</code> or
   *                   <code>event</code> is <code>null</code>.
   */
  public HandlerApplication(EventHandler handler, Event event) {
    if (null == handler) {
      throw new NullPointerException("Null event handler");
    } else if (null == event) {
      throw new NullPointerException("Null event");
    }

    this.handler = handler;
    this.event   = event;
  }

  /**
   * Get the event handler for this event handler application.
   *
   * @return  The event handler for this event handler application.
   */
  public EventHandler getHandler() {
    return handler;
  }

  /**
   * Get the event for this event handler application.
   *
   * @return  The event for this event handler application.
   */
  public Event getEvent() {
    return event;
  }

  /** Perform this event handler application. */
  public void run() {
    handler.handle(event);
  }
  
}

