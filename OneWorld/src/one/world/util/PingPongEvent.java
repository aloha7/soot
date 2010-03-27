/*
 * Copyright (c) 2001 University of Washington, Department of
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
 * Implementation of a ping pong event. A ping pong event can be used
 * to ping services. If a service understands ping pong events, it
 * responds to each ping with a pong. The root environment's {@link
 * one.world.core.RequestManager}, for example, understands ping pong
 * events.
 *
 * @version  $Revision: 1.1 $
 * @author   Robert Grimm
 */
public class PingPongEvent extends Event {

  /**
   * The flag for whether this ping pong event represents a pong.
   *
   * @serial
   */
  public boolean pong;

  /** Create a new, empty ping pong event. */
  public PingPongEvent() {
    // Nothing to do.
  }

  /**
   * Create a new ping pong event.
   *
   * @param  source   The source.
   * @param  closure  The closure.
   * @param  pong     The flag for whether this ping pong event
   *                  represents a pong.
   */
  public PingPongEvent(EventHandler source, Object closure, boolean pong) {
    super(source, closure);
    this.pong = pong;
  }

}

