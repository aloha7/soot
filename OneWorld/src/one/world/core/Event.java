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

package one.world.core;

import one.util.Guid;

/**
 * The superclass of all events. Event objects are tuples that
 * encapsulate all necessary state when notifying an {@link
 * EventHandler} of the occurence of some logical event.
 *
 * @version  $Revision: 1.14 $
 * @author   Robert Grimm
 */
public abstract class Event extends Tuple {

  /** The serial version ID for this class. */
  static final long serialVersionUID = 4540609274049819186L; 

  /**
   * The source for this event. The source of an event indicates the
   * resource that originally creates the event. Typically, this field
   * points to an event handler that can be used for call-backs in
   * request-response interactions. The source of an event must not be
   * <code>null</code>.
   *
   * @serial  Must not be <code>null</code>.
   */
  public EventHandler source;

  /**
   * The closure for this event. The closure of an event is an opaque
   * object that is simply passed through. Typically, a "client" in a
   * request-response interaction uses the closure to distinguish
   * between several response events processed by the same event
   * handler.
   *
   * @serial
   */
  public Object       closure;

  /**
   * Create a new, empty event.
   */
  public Event() {
    // Nothing to do.
  }

  /**
   * Create a new event with the specified source and closure. This
   * constructor creates a new event with the specified source and
   * closure. The ID field is assigned a new GUID.
   *
   * @param   source    The source for the new event.
   * @param   closure   The closure for the new event.
   */
  public Event(EventHandler source, Object closure) {
    super();
    this.source  = source;
    this.closure = closure;
  }

  /**
   * Create a new event with the specified ID, source, and closure.
   *
   * @param   id       The ID for the new event.
   * @param   source   The source for the new event.
   * @param   closure  The closure for the new event.
   */
  public Event(Guid id, EventHandler source, Object closure) {
    super(id);
    this.source  = source;
    this.closure = closure;
  }

  /**
   * Validate this event. This method ensures that the source of an
   * event is not <code>null</code>. It also validates the closure, if
   * that object is validatable. Classes that override this method
   * must first call the corresponding method in their superclass.
   */
  public void validate() throws TupleException {
    super.validate();

    if (null == source) {
      throw new InvalidTupleException("Null source for event (" + this + ")");
    }

    if (closure instanceof Tuple) {
      ((Tuple)closure).validate();
    }
  }

}
