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

package one.world.rep;

import one.world.core.Event;
import one.world.core.EventHandler;
import one.world.rep.RemoteReference;

/**
 * Implementation of an announce event.  An announce event is an
 * advertisement of the presence of a {@link DiscoveryServer}.  The
 * goal of sending an AnnounceEvent is to communicate a {@link
 * RemoteReference} for further communication via remote event
 * passing.
 *
 * @version  $Revision: 1.3 $
 * @author Adam MacBeth 
 */
public class AnnounceEvent extends Event {

  /** The serial version ID for this class. */
  static final long serialVersionUID = -4555632729216647529L;

  /** The remote reference for the advertised server. */
  public RemoteReference ref;

  /** The capacity of the server. */
  public long capacity;

  /** Create a new, empty announce event. */
  public AnnounceEvent() {
    // Nothing to do.
  }

  /**
   * Create a new announce event.
   *
   * @param  source  The source for the new announce event.
   * @param  closure  The closure for the new announce event.
   * @param  ref  The remote reference to the server.
   * @param  capacity The value computed by the heuristic function.
   */
  public AnnounceEvent(EventHandler source, Object closure, 
		       RemoteReference ref, long capacity) {
    super(source,closure);
    this.ref = ref;
    this.capacity = capacity;
  }

   /**
   * Create a new announce event.
   *
   * @param  source  The source for the new announce event.
   * @param  closure  The closure for the new announce event.
   * @param  ref  The remote reference to the server.
   */
  public AnnounceEvent(EventHandler source, Object closure, 
		       RemoteReference ref) {
    super(source,closure);
    this.ref = ref;
    this.capacity = ElectionManager.heuristic();
  }


}
