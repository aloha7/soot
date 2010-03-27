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
import one.world.core.Environment;
import one.world.core.TupleException;

import one.world.util.IdEvent;

/**
 * Implementation of a create request. Create requests ask for the
 * creation of a new child environment.
 *
 * <p>The parent environment for the environment to be created is
 * specified by its ID in the {@link #ident} field. The name of the
 * new child environment is specified in the {@link #name} field.</p>
 *
 * @see      EnvironmentEvent#CREATED
 * @see      Environment#create(Guid,Guid,String,boolean)
 * @see      Environment#create(Guid,Guid,String,boolean,String,Object)
 *
 * @version  $Revision: 1.11 $
 * @author   Robert Grimm
 */
public final class CreateRequest extends IdEvent {

  /**
   * The name of the new child environment.
   *
   * @serial  Must be a valid environment name.
   */
  public String  name;

  /**
   * The name of the class providing the initializer for the
   * new environment.
   *
   * @serial
   */
  public String  init;

  /**
   * The closure for the initializer.
   *
   * @serial
   */
  public Object  initClosure;

  /** 
   * The flag for whether to inherit the parent environment's
   * protection domain.
   *
   * @serial
   */
  public boolean inherit;

  /**
   * Create a new, empty create request.
   */
  public CreateRequest() {
    // Nothing to do.
  }

  /**
   * Create a new create request.
   *
   * @param   source   The source for the new create request.
   * @param   closure  The closure for the new create request.
   * @param   ident    The environment ID for the new create request.
   * @param   name     The name for the new create request.
   * @param   inherit  The inherit flag for the new create request.
   * @param   init     The name of the class providing the initializer
   *                   for the new create request.
   * @param   initClosure
   *                   The closure for the initializer.
   */
  public CreateRequest(EventHandler source, Object closure,
                       Guid ident, String name, boolean inherit,
                       String init, Object initClosure) {
    super(source, closure, ident);
    this.name        = name;
    this.inherit     = inherit;
    this.init        = init;
    this.initClosure = initClosure;
  }

  /** Validate this create request. */
  public void validate() throws TupleException {
    super.validate();
    if (null == ident ) {
      throw new InvalidTupleException("Null ident for create request (" + this +
                                      ")");
    }

    try {
      Environment.ensureName(name);
    } catch (NullPointerException x) {
      throw new InvalidTupleException("Null environment name for create " +
                                      "request (" + this + ")");
    } catch (IllegalArgumentException x) {
      throw new InvalidTupleException("Invalid environment name for create " +
                                      "request (" + this + "): " +
                                      x.getMessage());
    }
  }

  /** Get a string representation of this create request. */
  public String toString() {
    return "#[create env \"" + name + "\" in " + ident + "]";
  }

}
