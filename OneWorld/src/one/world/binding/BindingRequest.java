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
 * Implementation of a binding request. Binding requests are used to
 * request resources. Resources are described in the form of a tuple.
 * For example, structured I/O resource descriptors are used to
 * describe structured I/O communications channels and tuple storage.
 *
 * <p>Requesting a binding may result in a {@link BindingException},
 * notably a {@link UnknownResourceException} if the resource
 * description is not recognized, and a {@link LeaseException},
 * notably a {@link LeaseDeniedException} if the resource cannot be
 * leased.</p>
 *
 * @see      one.world.io.SioResource
 * @see      BindingResponse
 * 
 * @version  $Revision: 1.8 $
 * @author   Robert Grimm
 */
public class BindingRequest extends Event {

  /** The serial version ID for this class. */
  static final long serialVersionUID = 8982720344547313194L;

  /**
   * The descriptor for the resource to be bound.
   *
   * @serial  Must not be <code>null</code>.
   */
  public Tuple        descriptor;

  /**
   * The requested duration for the lease.
   *
   * @see     Duration
   *
   * @serial  Must be a valid duration
   */
  public long         duration;

  /** Create a new, empty binding request. */
  public BindingRequest() {
    // Nothing to do.
  }

  /**
   * Create a new binding request.
   *
   * @param   source      The source for the new binding request.
   * @param   closure     The closure for the new binding request.
   * @param   descriptor  The descriptor for the new binding request.
   * @param   duration    The duration for the new binding request.
   */
  public BindingRequest(EventHandler source, Object closure,
                        Tuple descriptor, long duration) {
    super(source,closure);
    this.descriptor = descriptor;
    this.duration   = duration;
  }

  /** Validate this binding request. */
  public void validate() throws TupleException {
    super.validate();
    if (null == descriptor) {
      throw new InvalidTupleException("Null descriptor for binding request ("
                                      + this + ")");
    } else if (Duration.ANY > duration) {
      throw new InvalidTupleException("Invalid duration (" + duration +
                                      ") for binding request (" + this + ")");
    } else if (null != descriptor) {
      descriptor.validate();
    }
  }

}
