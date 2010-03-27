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
 * Implementation of an exported event handler descriptor. An exported
 * event handler descriptor describes an event handler exported by a
 * component.
 *
 * @version  $Revision: 1.5 $
 * @author   Robert Grimm
 */
public final class ExportedDescriptor extends EventHandlerDescriptor {

  /** The serial version ID for this class. */
  static final long serialVersionUID = -5356768212621253258L;

  /** Create a new, empty exported event handler descriptor. */
  public ExportedDescriptor() {
    // Nothing to do.
  }

  /**
   * Create a new exported event handler descriptor. The list of types
   * and conditions is optional and may be <code>null</code>.
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
  public ExportedDescriptor(String name, String description, Class[] types,
                            Class[] conditions, boolean linkForced) {
    super(name, description, types, conditions, linkForced);
  }

}
