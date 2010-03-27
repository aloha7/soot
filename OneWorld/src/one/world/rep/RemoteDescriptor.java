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

import one.world.core.Tuple;
import one.world.core.EventHandler;
import one.world.core.InvalidTupleException;
import one.world.core.TupleException;

import one.world.data.Name;

import one.world.io.Query;

/**
 * Implementation of a remote export descriptor.  This descriptor, used in a
 * {@link one.world.binding.BindingRequest}, includes an event handler to
 * export and the descriptor tuple under which it is to be exported.
 *
 * <p>The type of the descriptor tuple determines how the event handler
 * will be exported.
 * <ul>
 *   <li>If the tuple is <code>null</code>, the event handler will be
 *       exported anonymously.  It will be accessible only via a generated
 *       unique ID.</li>
 *   <li>If the tuple is a {@link one.world.data.Name}, the event handler 
 *       will be exported with the given name at the local host and
 *       port.</li>
 *   <li>If the tuple is a {@link one.world.io.Query}, the event handler
 *       is registered for reverse discovery:  Events passing
 *       through the discovery service will be compared to the query, and
 *       delivered to the event handler if it matches.  If the 
 *       {@link #snoop} flag is <code>true</code>, then this resource 
 *       will not be considered when counting matches for the purpose of 
 *       {@link DiscoveredResource#matchAll anycast} or sending {@link
 *       one.world.binding.UnknownResourceException}s.</li>
 *   <li>Otherwise, the event handler will be exported via the discovery
 *       service with the tuple as its descriptor.</li>
 * </ul></p>
 *
 * @version  $Revision: 1.5 $
 * @author   Janet Davis
 */
public class RemoteDescriptor extends Tuple {

  /** The serial version ID for this class. */
  static final long serialVersionUID = 4954565751425126951L;

  /**
   * The event handler to export to REP.
   *
   * @serial  Must not be <code>null</code>.
   */
  public EventHandler handler;

  /**
   * The tuple to use for the event handler's descriptor.
   */
  public Tuple descriptor;

  /**
   * If this flag is true, exports for reverse disovery will not consume 
   * events.
   *
   * @serial Must be <code>false</code> unless the {@link #descriptor} is a 
   *         {@link Query}.
   */
  public boolean snoop;

  /**
   * Constructs a new, empty remote export descriptor.
   */
  public RemoteDescriptor() {}

  /** 
   * Constructs a new anonymous event handler descriptor.  The descriptor
   * tuple is <code>null</code>.
   *
   * @param handler    The handler to export.
   */
  public RemoteDescriptor(EventHandler handler) {
    this.handler = handler;
    this.descriptor = null;
  }

  /** 
   * Constructs a new named event handler descriptor with the specified
   * event handler and name.  This constructor is provided as a
   * convenience; the name is stored as a {@link one.world.data.Name}
   * tuple.
   *
   * @param handler     The handler to export to REP.
   * @param name        The name to assign to the event handler.
   */
  public RemoteDescriptor(EventHandler handler, String name) {
    this(handler, new Name(name));
  }

  /**
   * Constructs a new event handler descriptor with the specified
   * event handler and tuple descriptor.
   *
   * @param handler     The handler to export to REP.
   * @param descriptor  The descriptor to assign to the event handler.
   */
  public RemoteDescriptor(EventHandler handler, Tuple descriptor) {
    this.handler = handler;
    this.descriptor = descriptor;
  }

  /**
   * Constructs a new event handler descriptor with the specified event
   * handler, query, and snoop flag.
   *
   * @param handler     The handler to export to REP.
   * @param query       The query to use for reverse discovery.
   * @param snoop       <code>true</code> if this should not consume
   *                    events.
   */
  public RemoteDescriptor(EventHandler handler, Query query, 
                          boolean snoop) {
    this.handler = handler;
    this.descriptor = query;
    this.snoop = snoop;
  }

  /** Validates this remote export descriptor. */
  public void validate() throws TupleException {
    super.validate();

    if (handler == null) {
      throw new
        InvalidTupleException("Null event handler for remote descriptor (" +
                              this + ")");
    } else if (snoop && !(descriptor instanceof Query)) {
      throw new
        InvalidTupleException("snoop is true for non-Query descriptor (" +
	                      this + ")");
    }
  }

  /** Returns a string representing this remote descriptor. */
  public String toString() {
    return "#[RemoteDescriptor " + descriptor + " for " + handler
           + (snoop ? " (snoop)" : "") + "]";
  }
}
