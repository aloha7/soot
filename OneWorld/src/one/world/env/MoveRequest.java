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

package one.world.env;

import one.util.Guid;

import one.world.core.Event;
import one.world.core.EventHandler;
import one.world.core.InvalidTupleException;
import one.world.core.TupleException;

import one.world.util.IdEvent;

/**
 * Implementation of a move request. Move requests ask for the moving
 * of an environment and all its descendants. The move operation
 * either moves the actual environment(s) or moves a clone of the
 * environment(s), depending on the {@link #clone} flag.
 *
 * <p>The environment to be moved is specified by its ID in the {@link
 * #ident} field. The new parent environment is specified by a
 * structured I/O URL in the {@link #location} field. The {@link
 * #clone} flag specified whether to move or copy an environment and
 * its descendants.</p>
 *
 * @see      EnvironmentEvent#MOVED
 * @see      EnvironmentEvent#CLONED
 *
 * @version  $Revision: 1.9 $
 * @author   Robert Grimm
 */
public final class MoveRequest extends IdEvent {

  /**
   * The new location for the environment and its descendants. This
   * string must have the same format as a structured I/O URL for
   * tuple storage.
   *
   * @see     one.world.io.SioResource
   *
   * @serial  Must not be <code>null</code>.
   */
  public String  location;

  /**
   * The flag indicating whether the environment and its descendants
   * should be cloned. If this flag is <code>true</code> the
   * environment and its descendants remain in their original location
   * and a clone of the environment subtree is moved to the new
   * location.
   *
   * @serial
   */
  public boolean clone;

  /**
   * Create a new, empty move request.
   */
  public MoveRequest() {
    // Nothing to do.
  }

  /**
   * Create a new move request.
   *
   * @param   source    The source for the new move request.
   * @param   closure   The closure for the new move request.
   * @param   ident     The environment ID for the new move request.
   * @param   location  The location for the new move request.
   * @param   clone     The clone flag for the new move request.
   */
  public MoveRequest(EventHandler source, Object closure,
                       Guid ident, String location, boolean clone) {
    super(source, closure, ident);
    this.location = location;
    this.clone    = clone;
  }

  /** Validate this move request. */
  public void validate() throws TupleException {
    super.validate();
    if (null == ident ) {
      throw new InvalidTupleException("Null ident for move request (" + this +
                                      ")");
    }

    if (null == location) {
      throw new InvalidTupleException("Null location for move request (" +
                                      this + ")");
    }
  }

  /** Get a string representation of this move request. */
  public String toString() {
    return "#[" + (clone? "clone" : "move") + " env " + ident + " to \"" +
      location + "\"";
  }

}
