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

package one.world.binding;

import one.world.core.Event;
import one.world.core.EventHandler;
import one.world.core.InvalidTupleException;
import one.world.core.Tuple;
import one.world.core.TupleException;

/**
 * Implementation of a binding response. Binding responses are used to
 * return the bound resource and lease resulting from a successful
 * binding request.
 *
 * @see      BindingRequest
 * 
 * @version  $Revision: 1.12 $
 * @author   Robert Grimm
 */
public class BindingResponse extends Event {

  /** The serial version ID for this class. */
  static final long serialVersionUID = 7709716116884318946L;

  /**
   * The descriptor for the resource that has been bound.
   *
   * @serial  Must not be <code>null</code>.
   */
  public Tuple        descriptor;

  /**
   * The event handler for the bound resource.
   *
   * @serial  Must not be <code>null</code>.
   */
  public EventHandler resource;

  /**
   * The event handler for the lease that controls the binding.
   *
   * @serial  Must not be <code>null</code>.
   */
  public EventHandler lease;

  /**
   * The initial lease duration for the lease controlling the binding.
   *
   * @see     Duration
   *
   * @serial  Must be a valid lease duration.
   */
  public long         duration;

  /** Create a new, empty binding response. */
  public BindingResponse() {
    // Nothing to do.
  }

  /**
   * Create a new binding response.
   *
   * @param   source      The source for the new binding response.
   * @param   closure     The closure for the new binding response.
   * @param   descriptor  The descriptor for the new binding response.
   * @param   resource    The resource's event handler for the new binding
   *                      response.
   * @param   lease       The lease's event handler for the new binding
   *                      response.
   * @param   duration    The duration of the lease for the new binding
   *                      response.
   */
  public BindingResponse(EventHandler source, Object closure,
                         Tuple descriptor, EventHandler resource,
                         EventHandler lease, long duration) {
    super(source,closure);
    this.descriptor = descriptor;
    this.resource   = resource;
    this.lease      = lease;
    this.duration   = duration;
  }

  /** Validate this binding response. */
  public void validate() throws TupleException {
    super.validate();
    if (null == descriptor) {
      throw new InvalidTupleException("Null descriptor for binding response ("
                                      + this + ")");
    } else if (null == resource) {
      throw new InvalidTupleException("Null resource handler for binding " +
                                      "response (" + this + ")");
    } else if (null == lease) {
      throw new InvalidTupleException("Null lease handler for binding " +
                                      "response (" + this + ")");
    } else if (Duration.ANY >= duration) {
      throw new InvalidTupleException("Invalid lease duration (" +
                                      duration + ") for binding " +
                                      "response (" + this + ")");
    } else if (null != descriptor) {
      descriptor.validate();
    }
  }

}
