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

package one.gui;

import one.util.Guid;

import one.world.core.Event;
import one.world.core.EventHandler;
import one.world.core.InvalidTupleException;
import one.world.core.TupleException;

import one.world.util.IdEvent;

/**
 * Implementation of an environment drop event. An environment
 * drop event indicates that an environment selection has been
 * dropped into a drag and drop target accepting environment
 * selections.
 *
 * @see      GuiUtilities#turnIntoEnvironmentTarget
 *
 * @version  $Revision: 1.2 $
 * @author   Robert Grimm
 */
public class EnvironmentDropEvent extends IdEvent {

  /**
   * The flag for whether the drop operation represents a copy
   * (or a move) operation.
   *
   * @serial
   */
  public boolean copy;

  /**
   * Create a new, empty environment drop event.
   */
  public EnvironmentDropEvent() {
    // Nothing to do.
  }

  /**
   * Create a new environment drop event.
   *
   * @param   source   The source.
   * @param   closure  The closure.
   * @param   ident    The environment ID.
   * @param   copy     The flag for whether the drop represents a
   *                   copy or move operation.
   */
  public EnvironmentDropEvent(EventHandler source, Object closure,
                              Guid ident, boolean copy) {
    super(source, closure, ident);
    this.copy = copy;
  }

  /** Validate this environment drop event. */
  public void validate() throws TupleException {
    super.validate();

    if (null == ident ) {
      throw new InvalidTupleException("Null ident for environment drop event ("
                                      + this + ")");
    }
  }

  /** Get a string representation for this environment drop event. */
  public String toString() {
    return "#[" + (copy? "Copy" : "Move") + " environment " + ident + "]";
  }

}
