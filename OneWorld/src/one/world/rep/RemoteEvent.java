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
import one.world.core.InvalidTupleException;
import one.world.core.SymbolicHandler;
import one.world.core.Tuple;
import one.world.core.TupleException;

import java.util.Iterator;

/**
 * Implements a remote event, an event which is to be delivered to a
 * remote host.   
 *
 * @version  $Revision: 1.12 $
 * @author   Janet Davis
 */
public final class RemoteEvent extends Event {

  /** The serial version ID for this class. */
  static final long serialVersionUID = -1837152200354138140L;

  /**
   * A descriptor for the destination event handler.
   *
   * @serial Must not be <code>null</code>
   */
  public SymbolicHandler destination;

  /**
   * The event to send to the destination handler.  All event
   * handlers refered to in the event should be 
   * {@link one.world.core.SymbolicHandler symbolic}.
   *
   * @serial Must not be <code>null</code>
   */
  public Event event;

  /**
   * If the datagram flag is true, the event should be sent via an
   * unreliable datagram.  Otherwise, the event will be sent via TCP.
   */
  public boolean datagram;

  /** Constructs a new, empty remote event. */
  public RemoteEvent() {}

  /** 
   * Constructs a new remote event to be sent via reliable transport 
   * with the given source, closure, destination, and event.  All event
   * handlers refered to in the event should be 
   * {@link one.world.core.SymbolicHandler symbolic}.
   *
   * @param source  The source of the new remote event.
   * @param closure The closure for the new remote event.
   * @param destination  The destination for the new remote event.
   * @param event   The event to be encapsulated by the new remote event.
   */
  public RemoteEvent(EventHandler source, Object closure,
                     SymbolicHandler destination, Event event) {
    super(source, closure);
    this.destination = destination;
    this.event = event;
    this.datagram = false;
  }

  /** 
   * Constructs a new remote event with the given source, closure,
   * destination, event, and datagram flag. All event
   * handlers refered to in the event should be 
   * {@link one.world.core.SymbolicHandler symbolic}.
   *
   * @param source  The source of the new remote event.
   * @param closure The closure for the new remote event.
   * @param destination  The destination for the new remote event.
   * @param event   The event to be encapsulated by the new remote event.
   * @param datagram True if the event should be sent via an unreliable
   *                datagram.
   */
  public RemoteEvent(EventHandler source, Object closure,
                     SymbolicHandler destination, Event event,
		     boolean datagram) {
    super(source, closure);
    this.destination = destination;
    this.event = event;
    this.datagram = datagram;
  }

  /** Validate this remote event. */
  public void validate() throws TupleException {
    super.validate();

    if (destination == null) {
      throw new InvalidTupleException("Null destination for remote event (" +
                                      this + ")");
    } else if (event == null) {
      throw new InvalidTupleException("Null event for remote event (" + this +
                                      ")");
    }

    // Validate the embedded event.
    event.validate();
  }

  /** 
   * Verify that all event handlers in this remote event (other than the
   * source) are symbolic.
   *
   * @throws InvalidTupleException if any nested event handler is not
   *                               symbolic.
   */
  public void verifySymbolic() throws InvalidTupleException {
    // Verify that all fields other than the source (which will be set to
    // NullHandler.NULL) and the destination (which is of type
    // SymbolicHandler) are symbolic.

    if (   (event.containsNonSymbolicHandler())
        || (metaData != null && metaData.containsNonSymbolicHandler())
	|| ((closure instanceof Tuple)
	      && (((Tuple)closure).containsNonSymbolicHandler()))
	|| ((closure instanceof EventHandler)
	      && !(closure instanceof SymbolicHandler))) {

      throw new InvalidTupleException(
	      "Remote event contains non-symbolic handler");
    }
  }

  /** Get a string represention for this remote event. */
  public String toString() {
    return "#[remote event " + destination + ".handle(" + event + ")"
           + (datagram ? " via datagram" : "") + "]";
  }
}
