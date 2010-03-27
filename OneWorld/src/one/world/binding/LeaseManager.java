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

import one.world.Constants;

import one.world.core.Component;
import one.world.core.ComponentDescriptor;
import one.world.core.Environment;
import one.world.core.Event;
import one.world.core.EventHandler;
import one.world.core.ExceptionalEvent;
import one.world.core.ExportedDescriptor;
import one.world.core.ImportedDescriptor;
import one.world.core.NotActiveException;
import one.world.core.Tuple;

import one.world.util.AbstractHandler;
import one.world.util.NullHandler;
import one.world.util.Timer;

/**
 * Implementation of a lease manager. A lease manager is responsible
 * for managing the leases governing access to leased resources.
 *
 * <p>Leased resources are provided by resource managers. When a
 * resource manager receives a binding request for one of the
 * resources it manages, it acquires the corresponding lease from its
 * lease manager through an acquire lease event. The lease manager
 * then handles lease renewal and cancellation. It notifies the
 * resource manager of cancellation through a canceled lease
 * event. Resource managers may receive several canceled lease events
 * for the same resource and should simply ignore repeated canceled
 * lease events.</p>
 *
 * <p>Note that when the lease manager sends a canceled lease event to
 * the resource manager, the canceled lease event's closure is the
 * same as the closure of the corresponding acquire lease event.</p>
 *
 * <p><b>Imported and Exported Event Handlers</b></p>
 *
 * <p>Exported event handler(s):<dl>
 *    <dt>request</dt>
 *    <dd>Handles lease events requesting to acquire a lease.
 *        </dd>
 * </dl></p>
 *
 * @see      BindingRequest
 * @see      LeaseEvent
 *
 * @version  $Revision: 1.22 $
 * @author   Robert Grimm
 */
public final class LeaseManager extends Component {

  /*
   * Some notes on concurrency:
   *
   * Lease renewal is performed under a monitor for the lease handler
   * as to serialize concurrent renewal requests.
   *
   * Lease cancellation is not performed under a monitor. As a result
   * it is possible for concurrent cancellations to succeed and the
   * resource manager may be repeatedly notified of the cancellation.
   *
   * If a lease renewal competes with a lease cancellation, the
   * renewal may appear to succeed even though the lease has actually
   * been canceled. While seemingly undesirable, this is possible even
   * if lease cancellation was serialized under a monitor.
   * 
   * To minimize race conditions, a lease handler's status and
   * duration are declared to be volatile and durations are copied for
   * repeated access.
   */


  // =======================================================================
  //                               Constants
  // =======================================================================

  /**
   * The skew constant, which is used to make sure that timers actually
   * fire after a lease has expired.
   */
  static final long        SKEW         = 100;

  /** The active lease status. */
  static final int         ACTIVE       = 1;

  /** The revoked lease status. */
  static final int         REVOKED      = 2;

  /** A cancel event. */
  static final Cancel      CANCEL_EVENT = new Cancel(NullHandler.NULL);


  // =======================================================================
  //                            The cancel event
  // =======================================================================

  /** Implementation of a cancel event. */
  final static class Cancel extends Event {

    /** Create a new, empty cancel event. */
    public Cancel() {
      // Nothing to do.
    }

    /**
     * Create a new cancel event.
     *
     * @param  source  The source for the new cancel event.
     */
    public Cancel(EventHandler source) {
      super(source, null);
    }

  }


  // =======================================================================
  //                           The cancel source
  // =======================================================================

  /** The event handler serving as a source for lease canceled events. */
  static final class CancelSource extends AbstractHandler {

    /** The canonical cancel source. */
    static final CancelSource SOURCE = new CancelSource();

    /** Create a new cancel source. */
    private CancelSource() {
      // Nothing to do.
    }

    /** Resolve this cancel source. */
    private Object readResolve() throws java.io.ObjectStreamException {
      return SOURCE;
    }

    /** Handle the specified event. */
    protected boolean handle1(Event e) {
      if (e instanceof ExceptionalEvent) {
        ExceptionalEvent ee = (ExceptionalEvent)e;
        
        if ((ee.x instanceof NotActiveException) ||
            (ee.x instanceof IllegalStateException)) {
          // We silently eat not active and illegal state exceptions
          // based on the theory that the receiving environment has
          // been terminated.
          return true;
        }
      }

      return false;
    }

  }


  // =======================================================================
  //                           The lease handler
  // =======================================================================

  /** The event handler for a single lease. */
  final class LeaseHandler extends AbstractHandler {

    /**
     * The resource descriptor for the leased resource.
     *
     * @serial  Must not be <code>null</code>.
     */
    final Tuple        descriptor;

    /**
     * The resource manager's event handler to receive notification of
     * a canceled lease.
     *
     * @serial  Must not be <code>null</code>.
     */
    final EventHandler manager;

    /**
     * The closure for the lease event notifying the resource manager
     * of the canceled lease.
     *
     * @serial
     */
    final Object       closure;

    /**
     * The status of this lease.
     *
     * @serial  Must be <code>ACTIVE</code> or <code>REVOKED</code>.
     */
    volatile int       status;

    /**
     * The start time of the lease.
     *
     * @serial  Must be a valid time.
     */
    final long         start;

    /**
     * The duration of the lease.
     *
     * @serial  Must be a valid duration.
     */
    volatile long      duration;

    /**
     * The timer notification handler for leases that aren't forever.
     *
     * @serial  Must not be <code>null</code>
     */
    Timer.Notification notification;

    /**
     * Create a new lease handler.
     *
     * <p>The specified resource descriptor must be a valid resource
     * descriptor. The specified resource manager must not be
     * <code>null</code>. The specified duration must be a valid
     * duration and must not be <code>Duration.ANY</code>.</p>
     *
     * @param  descriptor  The resource descriptor.
     * @param  manager     The resource manager.
     * @param  closure     The closure for the canceled lease event.
     * @param  duration    The initial duration of the lease.
     */
    LeaseHandler(Tuple descriptor, EventHandler manager,
                 Object closure, long duration) {
      this.descriptor = descriptor;
      this.manager    = manager;
      this.closure    = closure;
      this.status     = ACTIVE;
      this.start      = System.currentTimeMillis();
      this.duration   = duration;

      if (Duration.FOREVER != duration) {
        notification = timer.schedule(Timer.FIXED_DELAY,
                                      start + duration + SKEW,
                                      Constants.TIMER_PERIOD,
                                      this, CANCEL_EVENT);
      }
    }

    /** Handle the specified event. */
    protected boolean handle1(Event e) {

      if (e instanceof Cancel) {                   // Cancel the lease.
        // Copy duration to have a consistent value.
        long dur = duration;

        if ((ACTIVE == status) &&
            (Duration.FOREVER != dur) &&
            (System.currentTimeMillis() > start + dur)) {
          status = REVOKED;
          notification.cancel();
          manager.handle(new LeaseEvent(this, closure, LeaseEvent.CANCELED,
                                        null, null, 0));
        }

        return true;

      } else if (e instanceof LeaseEvent) {        // Handle the lease event.
        LeaseEvent le = (LeaseEvent)e;

        if (LeaseEvent.CANCEL == le.type) {        // Cancel the lease.
          if (ACTIVE == status) {
            status = REVOKED;
            notification.cancel();
            manager.handle(new LeaseEvent(this, closure, LeaseEvent.CANCELED,
                                          null, null, 0));
            // We silently eat not active and illegal state exceptions
            // based on the theory that the environment containing the
            // lease manager has been terminated.
            try {
              respond(le, new LeaseEvent(CancelSource.SOURCE, null,
                                         LeaseEvent.CANCELED, null, null, 0));
            } catch (NotActiveException x) {
            } catch (IllegalStateException x) {
            }
            return true;
          } else {
            respond(le, new LeaseRevokedException());
            return true;
          }

        } else if (LeaseEvent.TIME == le.type) {   // Time the lease.
          if (ACTIVE == status) {
            respond(le, new
              LeaseEvent(this, null, LeaseEvent.TIMED, null, null,
                         start + duration - System.currentTimeMillis()));
            return true;
          } else {
            respond(le, new LeaseRevokedException());
            return true;
          }

        } else if (LeaseEvent.RENEW == le.type) {  // Renew the lease.
          if (ACTIVE == status) {
            do {
              // Copy duration to have a consistent value.
              long dur = duration;

              // Forever is forever is forever.
              if (Duration.FOREVER == dur) {
                respond(le, new LeaseEvent(this, null, LeaseEvent.RENEWED,
                                           null, null, dur));
                return true;
              }

              // Get the actual renewal duration.
              long renewBy;
              try {
                renewBy = getDuration(descriptor, start, dur, le.duration);
              } catch (LeaseDeniedException x) {
                respond(le, x);
                return true;
              }

              // Try to increment duration by the granted renewal duration.
              long remaining;
              synchronized (this) {
                // Check that the duration has not been changed.
                if (dur != duration) {
                  // Try again.
                  continue;
                }

                // Update total duration.
                if (Duration.FOREVER == renewBy) {
                  dur  = renewBy;
                } else {
                  dur += renewBy;
                }
                duration = dur;

                // Cancel any pending notifications for the old
                // expiration.
                if (null != notification) {
                  notification.cancel();
                }

                // Update remaining time.
                if (Duration.FOREVER == dur) {
                  remaining    = dur;
                  notification = null;
                } else {
                  // Cancel old notifications and set up new ones.
                  long now  = System.currentTimeMillis();
                  remaining = start + dur - now;
                  notification = timer.schedule(Timer.FIXED_DELAY,
                                                now + remaining + SKEW,
                                                Constants.TIMER_PERIOD,
                                                this, CANCEL_EVENT);
                }

                // Make sure lease has not been explicitly canceled.
                if (ACTIVE != status) {
                  if (null != notification) {
                    notification.cancel();
                  }

                  break;
                }
              }

              // Done.
              respond(le, new LeaseEvent(this, null, LeaseEvent.RENEWED,
                                         null, null, remaining));
              return true;

            } while (true);
          }

          respond(le, new LeaseRevokedException());
          return true;
        }
      }

      return false;
    }

  }


  // =======================================================================
  //                           The request handler
  // =======================================================================

  /** The request exported event handler. */
  final class RequestHandler extends AbstractHandler {

    /** Handle the specified event. */
    protected boolean handle1(Event e) {

      if (e instanceof LeaseEvent) {
        LeaseEvent le = (LeaseEvent)e;

        if (LeaseEvent.ACQUIRE != le.type) {
          return false;
        }

        // Determine actual duration.
        long duration;
        try {
          duration = getDuration(le.descriptor, le.duration);
        } catch (LeaseDeniedException x) {
          respond(le, x);
          return true;
        }

        // Create new lease handler.
        LeaseHandler lease =
          new LeaseHandler(le.descriptor, le.handler, le.closure, duration);

        // Lease has been acquired.
        respond(le, new
          LeaseEvent(this, null, LeaseEvent.ACQUIRED, lease, null, duration));
        return true;
      }

      return false;
    }
  }

  // =======================================================================
  //                          The acquire handler
  // =======================================================================

  /**
   * The event handler for processing lease acquisition. This event
   * handler provides a convenient source for an acquire lease event
   * sent to a lease manager.
   *
   * <p>If this event handler receives an acquired lease event, it
   * sends the appropriate binding response to the source of the
   * binding request specified to its constructor in order to indicate
   * a sucessfully acquired resource. If it receives a lease denied
   * exception (wrapped as an exceptional event), it sends a canceled
   * lease event to the resource specified to its constructor in order
   * to revoke the resource. Duplicate events after the first event
   * result in the sending of an illegal state exception (wrapped as
   * an exceptional event) to the source of the event.</p>
   *
   * @see   LeaseEvent
   * @see   one.world.binding.LeaseManager
   * @see   BindingRequest
   * @see   BindingResponse
   * @see   LeaseDeniedException
   */
  public static final class Acquire extends AbstractHandler {

    /**
     * The binding request for this acquire event handler.
     *
     * @serial  Must be a valid binding request, or <code>null</code>
     *          if this acquire event handler has already processed
     *          an event.
     */
    private BindingRequest request;

    /**
     * The event handler for the corresponding resource.
     *
     * @serial  Must be a non-<code>null</code> event handler that
     *          correctly handles canceled lease events.
     */
    private EventHandler   resource;

    /**
     * The flag to indicate whether this acquire event handler
     * has already seen an event.
     *
     * @serial
     */
    private boolean        gotResponse;

    /**
     * Create a new acquire event handler. The specified binding
     * request must be a valid binding request and the specified
     * resource must correctly handle canceled lease events.
     *
     * @param   request   The binding request requesting the resource.
     * @param   resource  The event handler for the corresponding
     *                    resource.
     */
    public Acquire(BindingRequest request, EventHandler resource) {
      this.request  = request;
      this.resource = resource;
    }

    /** Handle the specified event. */
    protected boolean handle1(Event e) {
      boolean error;

      // Determine if this event is a duplicate event.
      synchronized (this) {
        if (gotResponse) {
          error = true;
        } else {
          error       = false;
          gotResponse = true;
        }
      }

      if (error) {
        // Indicate a duplicate response.
        respond(e, new IllegalStateException("Duplicate response to lease " +
                                             "acquisition (" + e + ")"));
        return true;

      } else {
        // Handle the event.
        if (e instanceof LeaseEvent) {
          LeaseEvent le = (LeaseEvent)e;

          if (LeaseEvent.ACQUIRED == le.type) {
            // Confirm binding.
            respond(request, new
              BindingResponse(this, null, request.descriptor, resource,
                              le.handler, le.duration));

            // Let GC do its magic.
            request  = null;
            resource = null;

            // Done.
            return true;
          }

        } else if (e instanceof ExceptionalEvent) {
          if (((ExceptionalEvent)e).x instanceof LeaseDeniedException) {
            // Cancel the resource.
            resource.handle(new
              LeaseEvent(this, null, LeaseEvent.CANCELED, null, null, 0));

            // Forward the exception.
            respond(request, e);

            // Let GC do its magic.
            request  = null;
            resource = null;

            // Done.
            return true;
          }
        }

        // Let GC do its magic.
        request  = null;
        resource = null;

        // Done.
        return false;
      }
    }

  }



  // =======================================================================
  //                           Descriptors
  // =======================================================================

  /** The component descriptor. */
  private static final ComponentDescriptor SELF =
    new ComponentDescriptor("one.world.binding.LeaseManager",
                            "A lease manager",
                            true);

  /** The exported event handler descriptor for the request handler. */
  private static final ExportedDescriptor REQUEST =
    new ExportedDescriptor("request",
                           "The handler to process lease events",
                           new Class[] { LeaseEvent.class },
                           new Class[] { LeaseDeniedException.class },
                           false);


  // =======================================================================
  //                           Instance fields
  // =======================================================================

  /**
   * The timer.
   *
   * @serial  Must not be <code>null</code>.
   */
  private final Timer timer;


  // =======================================================================
  //                           Constructor
  // =======================================================================

  /**
   * Create a new instance of <code>LeaseManager</code>.
   *
   * @param   env    The environment for the new instance.
   */
  public LeaseManager(Environment env) {
    super(env);
    declareExported(REQUEST, new RequestHandler());
    this.timer = getTimer();
  }


  // =======================================================================
  //                           Component support
  // =======================================================================

  /** Get the component descriptor. */
  public ComponentDescriptor getDescriptor() {
    return (ComponentDescriptor)SELF.clone();
  }


  // =======================================================================
  //                            Policy Decisions
  // =======================================================================

  /**
   * Determine the actual lease duration when initially acquiring a
   * lease to the specified resource. This method performs the policy
   * decision as to whether to lease the specified resource and, if
   * so, for how long.
   *
   * <p>The resource descriptor must be a valid resource
   * descriptor.</p>
   *
   * <p>The requested duration may be <code>Duration.FOREVER</code> or
   * <code>Duration.ANY</code>. The actual duration is guaranteed to
   * not be <code>Duration.ANY</code>.</p>
   *
   * @param   descriptor  The resource descriptor.
   * @param   duration    The requested lease duration.
   * @return              The actual lease duration.
   * @throws  LeaseDeniedException
   *                      Signals that access to the resource has been
   *                      denied.
   */
  long getDuration(Tuple descriptor, long duration)
    throws LeaseDeniedException {

    long granted;

    if (Duration.ANY > duration) {
      throw new LeaseDeniedException("Invalid lease duration ("+duration+")");
    } else if (Duration.ANY == duration) {
      granted = Constants.LEASE_DEFAULT_DURATION;
    } else if (Constants.LEASE_MIN_DURATION > duration) {
      throw new LeaseDeniedException("Lease duration too short ("+duration+")");
    } else if (Constants.LEASE_MAX_DURATION < duration) {
      granted = Constants.LEASE_MAX_DURATION;
    } else {
      granted = duration;
    }

    // Sanity-check on actual duration.
    long now = System.currentTimeMillis();
    if (now > now + granted + SKEW) {
      throw new LeaseDeniedException("Lease duration too far into future");
    }

    // Return actual lease duration.
    return granted;
  }

  /**
   * Determine the additional lease duration when renewing the lease
   * to the specified resource. This method performs the policy
   * decision as to whether to renew the lease for the specified
   * resource and, if so, for how long.
   *
   * <p>This method assumes that the lease has not been canceled.</p>
   *
   * <p>The resource descriptor must be a valid resource
   * descriptor.</p>
   *
   * <p>The requested additional duration may be
   * <code>Duration.ANY</code> or <code>Duration.FOREVER</code>. The
   * actual additional duration is guaranteed to not be
   * <code>Duration.ANY</code>.</p>
   *
   * @param   descriptor  The resource descriptor.
   * @param   start       The time at which the lease was originally
   *                      acquired.
   * @param   current     The current overall duration of the lease.
   * @param   additional  The additional requested duration.
   * @throws  LeaseDeniedException
   *                      Signals that the renewal has been denied.
   */
  long getDuration(Tuple descriptor, long start, long current, long additional)
    throws LeaseDeniedException {

    long granted;

    // Normalize requested additional duration.
    if (Duration.ANY > additional) {
      throw new LeaseDeniedException("Invalid lease duration ("+additional+")");
    } else if (Duration.ANY == additional) {
      granted = Constants.LEASE_DEFAULT_DURATION;
    } else if (Constants.LEASE_MIN_DURATION > additional) {
      throw new LeaseDeniedException("Lease duration too short (" +
                                     additional + ")");
    } else if (Constants.LEASE_MAX_DURATION < additional) {
      granted = Constants.LEASE_MAX_DURATION;
    } else {
      granted = additional;
    }

    // Sanity-check on actual duration.
    if (start > start + current + granted + SKEW) {
      throw new LeaseDeniedException("Lease duration too far into future");
    }

    // Normalize relative to time remaining on lease.
    long remaining = start + current - System.currentTimeMillis() + granted;

    if (Constants.LEASE_MAX_DURATION < remaining) {
      granted -= (remaining - Constants.LEASE_MAX_DURATION);
      if (Constants.LEASE_MIN_DURATION > granted) {
        throw new LeaseDeniedException("Lease duration currently long enough");
      }
    }

    return granted;
  }
  

  // =======================================================================
  //                            Static methods
  // =======================================================================

  /**
   * Acquire a lease. Based on the specified binding request, this
   * method sends an acquire lease event to the specified lease
   * manager. It <i>asynchronously</i> processes the response. If the
   * lease is granted, it sends an appropriate binding response to the
   * source of the binding request. If the lease is not granted, it
   * sends a lease canceled event to the specified resource.
   *
   * <p>Calling this method is equivalent to calling<pre>
   *   leaseManager.handle(new
   *     LeaseEvent(new Acquire(request, resource), null,
   *                LeaseEvent.ACQUIRE, resource,
   *                request.descriptor, request.duration));
   * </pre></p>
   *
   * <p>The specified binding request must be a valid binding request.
   * The specified resource must correctly handle canceled lease
   * events.  The specified lease manager must correctly handle
   * acquire lease events.</p>
   *
   * @see     Acquire
   *
   * @param   request       The binding request.
   * @param   resource      The corresponding resource.
   * @param   leaseManager  The lease manager.
   */
  public static void acquire(BindingRequest request, EventHandler resource,
                             EventHandler leaseManager) {

    leaseManager.handle(new
      LeaseEvent(new Acquire(request, resource), null, LeaseEvent.ACQUIRE,
                 resource, request.descriptor, request.duration));
  }


  /**
   * Acquire a lease. Based on the specified binding request, this
   * method sends an acquire lease event to the specified lease
   * manager. It <i>asynchronously</i> processes the response. If the
   * lease is granted, it sends an appropriate binding response to the
   * source of the binding request. If the lease is not granted, it
   * sends a lease canceled event to the specified resource.
   *
   * <p>Calling this method is equivalent to calling<pre>
   *   leaseManager.handle(new
   *     LeaseEvent(new Acquire(request, resource), null,
   *                LeaseEvent.ACQUIRE, resourceManager,
   *                request.descriptor, request.duration));
   * </pre></p>
   *
   * <p>The specified binding request must be a valid binding request.
   * The specified resource manager must correctly handle canceled lease
   * events.  The specified lease manager must correctly handle
   * acquire lease events.</p>
   *
   * @see     Acquire
   *
   * @param   request          The binding request.
   * @param   resource         The corresponding resource.
   * @param   resourceManager  The resource manager.
   * @param   leaseManager     The lease manager.
   */
  public static void acquire(BindingRequest request, 
                             EventHandler resource,
                             EventHandler resourceManager, 
                             EventHandler leaseManager) {

    leaseManager.handle(new
      LeaseEvent(new Acquire(request, resource), null, LeaseEvent.ACQUIRE,
                 resourceManager, request.descriptor, request.duration));
  }

}
