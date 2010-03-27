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

package one.world.core;

/**
 * Definition of an event handler. Event handlers process
 * events. Implementation of this interface are typically provided by
 * components, which export some appropriate event handler.
 *
 * <p>Every event handler must accept all events of type {@link
 * ExceptionalEvent} and implement some reasonable default response
 * for arbitrary exceptional conditions. Additionally, some types of
 * throwables may have special significance, because they convey
 * specific exceptional conditions. Implementors of this interface are
 * strongly encouraged to document these throwables in the
 * corresponding event handler descriptor.</p>
 *
 * <p>Events are passed by value. When calling an event handler, the
 * caller should create a new event. After passing the event to the
 * handler, the caller should release all references to the event and
 * the objects it points to, unless the objects are immutable.</p>
 * 
 * @see      Component
 * @see      EventHandlerDescriptor
 *
 * @version  $Revision: 1.7 $
 * @author   Robert Grimm
 */
public interface EventHandler {

  /**
   * Handle the specified event.
   *
   * @param   event  The event to handle.
   */
  void handle(Event event);

}
