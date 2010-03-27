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

/**
 * The base class of event handler descriptors. An event handler
 * descriptor describes an event handler imported or exported by a
 * component. In addition to the name of the event handler and a short
 * description, it provides an array of classes that describe the
 * events handled by the event handler, and an array of throwables
 * that signal specific exceptional conditions. The arrays of classes
 * and throwables are optional and may be <code>null</code>. It also
 * specifies a flag for whether the event handler must always be
 * forcibly linked through a concurrency domain.
 *
 * @version  $Revision: 1.9 $
 * @author   Robert Grimm
 */
public abstract class EventHandlerDescriptor extends Descriptor {
  
  /** The serial version ID for this class. */
  static final long serialVersionUID = 2927686283046655027L;

  /**
   * The types of events handled by the event handler. All event
   * handlers must implicitly process exceptional
   * events. Consequently, the class for {@link ExceptionalEvent}
   * should not be included in this list.
   *
   * @serial  If <code>types</code> is not <code>null</code>,
   *          all entries in the array must represent subclasses
   *          of type {@link Event}.
   */
  public Class[] types;

  /**
   * The throwables for this event handler. The types of throwables
   * listed in this field signal specific exceptional conditions
   * communicated through exceptional events.
   *
   * @see     EventHandler
   *
   * @serial  If <code>conditions</code> is not <code>null</code>,
   *          all entries in the array must be classes representing
   *          a subtype of {@link Throwable}.
   */
  public Class[] conditions;

  /**
   * The flag for whether the event handler must be forcibly linked
   * through a concurrency domain. If this flag is <code>true</code>,
   * all linking operations for the event handler implicitly assume
   * that the <code>forced</code> flag to the <code>link()</code>
   * method is <code>true</code>, even if the <code>link()</code>
   * method is called with a <code>true</code> value.
   *
   * @see     Component#link(String,String,Component,boolean)
   *
   * @serial
   */
  public boolean linkForced;

  /** Create a new, empty event handler descriptor. */
  public EventHandlerDescriptor() {
    // Nothing to do.
  }

  /**
   * Create a new event handler descriptor. The list of types and
   * conditions is optional and may be <code>null</code>.
   *
   * @param   name         The name of the event handler.
   * @param   description  A short description of the event handler.
   * @param   types        The types of the events handled by
   *                       the event handler.
   * @param   conditions   The exceptional conditions of the
   *                       event handler.
   * @param   linkForced   The flag for whether the event handler
   *                       must be forcibly linked through a
   *                       concurrency domain.
   */
  public EventHandlerDescriptor(String name, String description,
                                Class[] types, Class[] conditions,
                                boolean linkForced) {
    super(name, description);
    this.types      = types;
    this.conditions = conditions;
    this.linkForced = linkForced;
  }

  /** Validate this event handler descriptor. */
  public void validate() throws TupleException {
    // Validate name and description.
    super.validate();

    // Validate types.
    if (null != types) {
      for (int i=0; i<types.length; i++) {
        Class k = types[i];
        if (null == k) {
          throw new
            InvalidTupleException("Null type for event handler descriptor (" +
                                  this + ")");
        } else if (! Type.CLASS_EVENT.isAssignableFrom(k)) {
          throw new
            InvalidTupleException("Not an event class for type of event " +
                                  "handler descriptor (" + this + ")");
        }
      }
    }

    // Validate conditions.
    if (null != conditions) {
      for (int i=0; i<conditions.length; i++) {
        Class k = conditions[i];
        if (null == k) {
          throw new
            InvalidTupleException("Null condition for event handler " +
                                  "descriptor (" + this + ")");
        } else if (! Type.CLASS_THROWABLE.isAssignableFrom(k)) {
          throw new
            InvalidTupleException("Not a throwable class for condition of " +
                                  "event handler descriptor (" + this + ")");
        }
      }
    }
  }

}
