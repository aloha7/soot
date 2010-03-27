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

import one.world.util.TypedEvent;

import one.world.core.EventHandler;
import one.world.core.InvalidTupleException;
import one.world.core.Tuple;
import one.world.core.TupleException;

/**
 * Implementation of a lease event. Lease events are used to perform
 * operations on leases and to notify of a revoked lease.
 *
 * <p>Operations on leases may result in an exceptional event
 * signalling a {@link LeaseException}. In particular, renewing or
 * timing a lease may result in a {@link LeaseRevokedException} and
 * acquiring or renewing a lease may result in a {@link
 * LeaseDeniedException}.</p>
 *
 * <p>Note that renewing a lease means requesting additional time on
 * the lease, beyond the current end of the lease. As a result, a
 * lease can be renewed at any time, effectively adding more time.
 * However, a renewal may be denied if the overall remaining duration
 * of the lease would be too long. In this case, trying to renew again
 * at a later time may still be successful.</p>
 * 
 * @version  $Revision: 1.9 $
 * @author   Robert Grimm
 */
public class LeaseEvent extends TypedEvent {

  /** The serial version ID for this class. */
  //static final long serialVersionUID = 1008930006271484717L;

  /**
   * The type code for acquiring a lease. <code>duration</code>
   * indicates the requested initial lease duration.
   */
  public static final int ACQUIRE  = 1;

  /**
   * The type code for an acquired lease. <code>duration</code>
   * indicates the granted initial lease duration.
   */
  public static final int ACQUIRED = 2;

  /**
   * The type code for requesting a lease renewal.
   * <code>duration</code> indicates the requested renewal time.
   */
  public static final int RENEW    = 3;

  /**
   * The type code for a renewed lease. <code>duration</code>
   * indicates the remaining lease duration.
   */
  public static final int RENEWED  = 4;

  /** The type code for requesting a lease cancellation. */
  public static final int CANCEL   = 5;

  /** The type code for a canceled lease. */
  public static final int CANCELED = 6;

  /** The type code for requesting a lease's remaining time. */
  public static final int TIME     = 7;

  /**
   * The type code for notifying about a lease's remaining time.
   * <code>duration</code> indicates the remaining lease duration.
   */
  public static final int TIMED    = 8;

  /**
   * The event handler for managing the actual resource when
   * requesting to acquire a lease or the event handler for
   * managing the lease itself for an acquired lease.
   *
   * @serial  Must not be <code>null</code> if the type of
   *          this lease event is <code>ACQUIRE</code> or
   *          <code>ACQUIRED</code>.
   */
  public EventHandler handler;

  /**
   * The description of the resource for which to acquire a
   * lease.
   *
   * @serial  Must not be <code>null</code> if the type of this
   *          lease event is <code>ACQUIRED</code>.
   */
  public Tuple        descriptor;

  /**
   * The duration for the lease.
   *
   * @see     Duration
   *
   * @serial  Must be a valid lease duration.
   */
  public long         duration;

  /** Create a new, empty lease event. */
  public LeaseEvent() {
    // Nothing to do.
  }

  /**
   * Create a new lease event.
   *
   * @param   source       The source for the new lease event.
   * @param   closure      The closure for the new lease event.
   * @param   type         The type for the new lease event.
   * @param   handler      The handler for the new lease event.
   * @param   descriptor   The descriptor for the new lease event.
   * @param   duration     The duration for the new lease event.
   */
  public LeaseEvent(EventHandler source, Object closure, int type,
                    EventHandler handler, Tuple descriptor, long duration) {
    super(source, closure, type);
    this.handler    = handler;
    this.descriptor = descriptor;
    this.duration   = duration;
  }

  /** Validate this lease event. */
  public void validate() throws TupleException {
    super.validate();
    switch (type) {
    case ACQUIRE:
      if (null == descriptor) {
        throw new InvalidTupleException("Null descriptor for lease event (" +
                                        this + ")");
      }

      descriptor.validate();
      // Fall through.
    case ACQUIRED:
      if (null == handler) {
        throw new InvalidTupleException("Null handler for lease event (" +
                                        this + ")");
      }
      // Fall through.
    case RENEW:
    case RENEWED:
    case TIMED:
      if (Duration.ANY > duration) {
        throw new InvalidTupleException("Invalid duration (" + duration +
                                        ") for lease event (" + this + ")");
      }
      break;
    case CANCEL:
    case CANCELED:
    case TIME:
      break;
    default:
      throw new InvalidTupleException("Invalid type (" + type +
                                      ") for lease event (" + this + ")");
    }
  }

}
