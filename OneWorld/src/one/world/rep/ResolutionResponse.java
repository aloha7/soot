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

import one.world.core.Event;

import one.world.core.EventHandler;
import one.world.core.InvalidTupleException;
import one.world.core.SymbolicHandler;
import one.world.core.Tuple;
import one.world.core.TupleException;

/**
 * Implements the result of a resolution.  The inability to
 * resolve a resource should be indicated with a 
 * {@link one.world.binding.UnknownResourceException}.
 *
 * @see      ResolutionRequest
 * @version  $Revision: 1.7 $
 * @author   Janet Davis
 */
public final class ResolutionResponse extends Event {

  /** The serial version ID for this class. */
  static final long serialVersionUID = -2284350544035051570L;

  /** 
   * The remote resource(s) that is(are) the result of resolution.
   *
   * @serial Must not be <code>null</code> */
  public SymbolicHandler[] resources; 

  /** The descriptor(s) for the resolved resource(s). */
  public Tuple[] descriptors;

  /** Constructs a new, empty ResolutionResponse. */
  public ResolutionResponse() {}

  /** 
   * Constructs a new ResolutionResponse with the given source, closure, 
   * and resource.
   *
   * @param source     The source of the new resolution event.
   * @param closure    The closure for the new resolution event.
   * @param descriptor The descriptor for the resolved resource.
   * @param resource   The resolved resource.
   */
  public ResolutionResponse(EventHandler source, Object closure, 
			    Tuple descriptor, SymbolicHandler resource) {
    super(source, closure);
    this.descriptors = new Tuple[] { descriptor };
    this.resources = new SymbolicHandler[] { resource };
  }

  /** 
   * Constructs a new ResolutionResponse with the given source and closure, 
   * as well as multiple resources and associated tuples.
   *
   * @param source      The source of the new resolution event.
   * @param closure     The closure for the new resolution event.
   * @param descriptors The descriptors for the resolved resources.
   * @param resources   The resolved resources.
   */
  public ResolutionResponse(EventHandler source, Object closure, 
			    Tuple[] descriptors, SymbolicHandler[] resources) {
    super(source, closure);
    this.descriptors = descriptors;
    this.resources = resources;
  }

  /** Validates this resolution event. */
  public void validate() throws TupleException {
    super.validate();
    
    if (resources == null) {
      throw new InvalidTupleException("Null resource for resolution response ("
                                      + this + ")");
    }
  }

}
