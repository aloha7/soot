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
 * Implementation of a load request. Load requests ask for an
 * initializer to be executed within an environment.
 *
 * @see      EnvironmentEvent#LOADED
 * @see      one.world.core.Environment#load(Guid,Guid,String,Object)
 * 
 * @version  $Revision: 1.6 $
 * @author   Robert Grimm
 */
public final class LoadRequest extends IdEvent {

  /**
   * The name of the class providing the initializer for the
   * new environment.
   *
   * @serial  Must not be <code>null</code>.
   */
  public String init;

  /**
   * The closure for the initializer.
   *
   * @serial
   */
  public Object initClosure;

  /**
   * Create a new, empty load request.
   */
  public LoadRequest() {
    // Nothing to do.
  }

  /**
   * Create a new load request.
   *
   * @param   source   The source for the new load request.
   * @param   closure  The closure for the new load request.
   * @param   ident    The environment ID for the new load request.
   * @param   init     The name of the class providing the initializer
   *                   for the new load request.
   * @param   initClosure
   *                   The closure for the initializer.
   */
  public LoadRequest(EventHandler source, Object closure, Guid ident,
                     String init, Object initClosure) {
    super(source, closure, ident);
    this.init        = init;
    this.initClosure = initClosure;
  }

  /** Validate this load request. */
  public void validate() throws TupleException {
    super.validate();
    if (null == ident ) {
      throw new InvalidTupleException("Null ident for load request (" + this +
                                      ")");
    }

    if (null == init) {
      throw new InvalidTupleException("Null initializer for load request (" +
                                      this + ")");
    }
  }

  /** Get a string representation of this load request. */
  public String toString() {
    return "#[load \"" + init + "\" into env " + ident + "]";
  }

}
