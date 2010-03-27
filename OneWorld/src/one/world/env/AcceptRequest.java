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

import one.world.core.Environment;
import one.world.core.EventHandler;
import one.world.core.SymbolicHandler;
import one.world.core.InvalidTupleException;
import one.world.core.TupleException;

import one.world.util.IdEvent;

/**
 * Implementation of an accept request. Accept requests ask for
 * accepting an environment and its descendants from a remote node.
 *
 * <p>Accept requests are delivered to the request exported event
 * handler of the new parent environment and, just like all events
 * delivered to the request exported event handler, are propagated up
 * the environment hierarchy. Intermediate environments whose monitor
 * imported event handler is linked can either reject the accept
 * request by sending a security exception to the sender or change
 * the new parent environment. The actual accept operation is
 * performance by the root environment.</p>
 *
 * <p>Note that when rejecting an accept request, the security
 * exception needs to be wrapped by an exceptional event, which, in
 * turn, is wrapped by a remote event. The closure of the exceptional
 * event must be the same as the closure of the accept request.</p>
 *
 * <p>Futher note that the ID of the environment being sent is
 * provided by the <code>ident</code> field of the superclass.</p>
 *
 * @version  $Revision: 1.8 $
 * @author   Robert Grimm
 */
public final class AcceptRequest extends IdEvent {

  /**
   * The sending host.
   *
   * @serial  Must not be <code>null</code>.
   */
  public String          sendingHost;

  /**
   * The name of the environment being sent.
   *
   * @serial  Must not be <code>null</code>.
   */
  public String          name;

  /**
   * The ID of the new parent environment.
   *
   * @serial  Must not be <code>null</code>.
   */
  public Guid            newParent;

  /**
   * The remote resource for the sender.
   *
   * @serial  Must not be <code>null</code>.
   */
  public SymbolicHandler  sender;

  /**
   * The flag for whether the environments are being cloned.
   *
   * @serial
   */
  public boolean         clone;

  /**
   * Create a new, empty accept request.
   */
  public AcceptRequest() {
    // Nothing to do.
  }

  /**
   * Create a new accept request.
   *
   * @param   source       The source for the new accept request.
   * @param   closure      The closure for the new accept request.
   * @param   sendingHost  The IP address of the sending host.
   * @param   ident        The ID of the environment being sent.
   * @param   name         The name of the environment being sent.
   * @param   newParent    The ID of the new parent environment.
   * @param   sender       The remote resource for the sender.
   * @param   clone        The flag for whether the environments
   *                       are being cloned.
   */
  public AcceptRequest(EventHandler source, Object closure,
                       String sendingHost, Guid ident, String name,
                       Guid newParent, SymbolicHandler sender,
                       boolean clone) {
    super(source, closure, ident);
    this.sendingHost = sendingHost;
    this.name        = name;
    this.newParent   = newParent;
    this.sender      = sender;
    this.clone       = clone;
  }

  /** Validate this accept request. */
  public void validate() throws TupleException {
    super.validate();
    if (null == ident) {
      throw new InvalidTupleException("Null ident for accept request (" + this
                                      + ")");
    } else if (null == sendingHost) {
      throw new InvalidTupleException("Null sending host for accept request ("
                                      + this + ")");
    } else if (null == name) {
      throw new InvalidTupleException("Null environment name for accept " +
                                      "request (" + this + ")");
    } else if (null == newParent) {
      throw new InvalidTupleException("Null parent environment ID for accept " +
                                      "request (" + this + ")");
    } else if (null == sender) {
      throw new InvalidTupleException("Null sender for accept request (" +
                                      this + ")");
    }
    try {
      Environment.ensureName(name);
    } catch (IllegalArgumentException x) {
      throw new InvalidTupleException(x.getMessage());
    }
  }

  /** Get a string representation of this accept request. */
  public String toString() {
    return "#[accept env " + ident + " named \"" + name + "\" from " +
      sendingHost + " at " + newParent + "]";
  }

}
