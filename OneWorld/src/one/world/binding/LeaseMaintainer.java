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
 * INCIdENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package one.world.binding;

import one.world.Constants;

import one.world.core.Event;
import one.world.core.EventHandler;
import one.world.core.ExceptionalEvent;
import one.world.core.Tuple;

import one.world.util.AbstractHandler;
import one.world.util.ExceptionHandler;
import one.world.util.Log;
import one.world.util.Operation;
import one.world.util.Synchronous;
import one.world.util.SystemUtilities;
import one.world.util.Timer;
import one.world.util.TimeOutException;

import one.util.Guid;

/**
 * A LeaseMaintainer automatically renews a lease until it is explicitly 
 * canceled.  The LeaseMaintainer class also includes a static {@link
 * #cancel} method that can be used to cancel any lease.
 *
 * @version  $Revision: 1.10 $
 * @author   Janet Davis
 */
public class LeaseMaintainer {

  /** The requested lease length. */
  private static final long DURATION = Duration.FOREVER;

  /** A guid to use for my closure. */
  private Guid guid = new Guid();

  /** My internal event handler. */
  private Handler handler = new Handler();

  /** The event handler that wants the leased resource and exceptions. */
  private final EventHandler notify;

  /** The closure for the original binding request. */
  private final Object closure;

  /** The lease to renew. */
  private EventHandler lease;

  /** The timer component. */
  private final Timer timer;

  /** The starting time for lease acquisition/renewal requests. */
  private long startTime;

  /** The timer notification handler. */
  private Timer.Notification timerNotification;

  /** 
   * A flag that indicates whether the lease has been acquired.
   */
  private volatile boolean acquired;

  /** 
   * A flag that indicates if we are to cancel the lease when we get it.
   * This starts out false, but may become true if the {@link #cancel}
   * method is called before a binding response is received.
   */ 
  private volatile boolean cancel;

  /** A cancel operation, to go with the above {@link #cancel} flag. */
  private volatile Operation cancelOperation;

  /** A closure for the {@link #cancelOperation}. */
  private volatile Object cancelClosure;

  /** A flag that indicates if the lease has been canceled. */
  private volatile boolean canceled = false;

  /** A lock object. */
  private transient Object lock = new Object();

  /** 
   * Constructs a new LeaseMaintainer.  All events generated in response
   * to the binding request will be passed on to the request source.
   *
   * @param   request The binding request for the desired resource.
   * @param   factory The event handler to ask for the resource.
   * @param   timer   A timer component.
   */
  public LeaseMaintainer(BindingRequest request, EventHandler factory,
                         Timer timer) {

    this.notify = request.source;
    this.closure = request.closure;
    this.timer = timer;

    request.source = this.handler;
    request.duration = DURATION;

    this.startTime = SystemUtilities.currentTimeMillis();
    factory.handle(request);
  }

  /**
   * Constructs a new lease maintainer.  The other constructor is
   * prefereable because it has a more conservative idea of when the lease
   * started and thus is more able to keep the lease from expiring.
   * However, this constructor is sometimes necessary.
   * 
   * @param  lease     The lease to maintain.
   * @param  duration  The initial lease duration.
   * @param  notify    The event handler to be notified of any exceptions.
   * @param  closure   The closure to use for any such notifications.
   * @param  timer     A timer component.
   */
  public LeaseMaintainer(EventHandler lease, long duration,
                         EventHandler notify, Object closure,
                         Timer timer) {
    this.lease = lease;
    this.notify = notify;
    this.closure = closure;
    this.timer = timer;

    acquired = true;

    startTime = SystemUtilities.currentTimeMillis();
    handler.scheduleRenewal(duration);
  }

  /** Finalize this lease maintainer. */
  protected void finalize() {
    cancel();
  }

  /**
   * Cancels the lease asynchronously.
   */
  public void cancel() {
    Timer.Notification t;
    EventHandler l;

    synchronized (lock) {

      // If we haven't yet acquired the resource, we'll have to cancel
      // after we get it.
      if (!acquired) {
        cancel = true;
        return;
      } 

      if (canceled) {
        return;
      }
      canceled = true;

      t = timerNotification;
      timerNotification = null;

      l = lease;
      lease = null;
    }

    // Cancel the timer.  
    if (t != null) {
      t.cancel();
    }

    // Cancel the lease.
    if (l != null) {
      cancel(l);
    }
  }

  /**
   * Cancels the lease using an operation.  Note that this method sets the 
   * {@link Operation#request} handler; do not use the operation
   * concurrently for anything else.
   *
   * @param operation  The operation to use.
   */
  public void cancel(Operation operation) {
    cancel(operation, null);
  }

  /**
   * Cancels the lease using an operation.  Note that this method sets the 
   * {@link Operation#request} handler; do not use the operation
   * concurrently for anything else.
   *
   * @param operation  The operation to use.
   * @param closure    The closure to use for the lease cancellation
   *                   event.
   */
  public void cancel(Operation operation, Object closure) {

    boolean wasCanceled;
    Timer.Notification t;
    EventHandler l;
   
    synchronized (lock) {

      // If we haven't yet acquired the resource, we'll have to cancel
      // after we get it.
      if (!acquired) {
        cancel = true;
	cancelOperation = operation;
	cancelClosure = closure;
        return;
      } 

      wasCanceled = canceled;
      canceled = true;

      t = timerNotification;
      timerNotification = null;

      l = lease;
      lease = null;
    }

    // We need to do something special if the lease was already canceled.
    if (wasCanceled) {
      if (operation.continuation != null) {
        operation.continuation.handle(
            new ExceptionalEvent(ExceptionHandler.HANDLER, closure, 
  	                         new LeaseRevokedException()));
      }
      return;
    }

    // Cancel the timer.  
    if (t != null) {
      t.cancel();
      t = null;
    }

    // Cancel the lease.
    if (l != null) {
      operation.request = l;
      try {
        operation.handle(new LeaseEvent(null, closure, LeaseEvent.CANCEL,
	                                null, null, 0));
      } catch (IllegalStateException x) {
        operation.continuation.handle(
	   new ExceptionalEvent(ExceptionHandler.HANDLER, closure,
	                        new ResourceRevokedException()));
      }
    } 
  }


  /** 
   * Gets the lease. 
   *
   * @return  The managed lease.
   */
  public EventHandler getLease() {
    return lease;
  }

  /** The internal event handler class. */
  private class Handler extends AbstractHandler {

    /** Handles events. */
    protected boolean handle1(Event event) {

      if (event instanceof BindingResponse) {

        boolean wasAcquired;
        synchronized (lock) {
	  wasAcquired = acquired;
	  if (!acquired) {
	    lease = ((BindingResponse)event).lease;
	    acquired = true;
	    lock.notify();
	  }
	}

	if (!wasAcquired) {
  	  if (cancel) {
	    if (cancelOperation != null) { 
	      cancel(cancelOperation, cancelClosure);
	    } else {
	      cancel();
	    }
  	  } else {
            long duration = ((BindingResponse)event).duration;
            if (duration != Duration.FOREVER) {
	      scheduleRenewal(duration);
            }
	  }
        }

        notify.handle(event);
        return true;

      } else if (event instanceof ExceptionalEvent) {
	Throwable x = ((ExceptionalEvent)event).x;

        if (acquired) {
	  if (x instanceof LeaseDeniedException) {
	    if (guid.equals(event.closure)) {
	      // Time the lease in case the renewed event got lost.
	      lease.handle(new LeaseEvent(this, guid, LeaseEvent.TIME, 
		                          null, null, 0));
	      return true;
	    } else {
	      // Nothing to do.
	      return true;
	    }

	  } else if (x instanceof ResourceRevokedException
	          || x instanceof LeaseRevokedException) {

            if (!canceled) {
              cancel();
	      notify.handle(event);
	    }
	    return true;
	  }

	} else {
          // Set acquired to true, in case this indicates that the binding
  	  // failed.
          synchronized (lock) {
	    acquired = true;
	  }

	  notify.handle(event);
	  return true;
	}

	return false;


      } else if (event instanceof LeaseEvent) {
        LeaseEvent le = (LeaseEvent)event;
	switch (le.type) {
	
	  case LeaseEvent.RENEW:
	  
	    if (cancel) {
	      return true;
	    }

            synchronized (lock) {
              if (!le.closure.equals(guid)) { 
	        // This is an old timer notification; nothing to do.
	        return true;
	      }
	    }

	    le.source = this;
	    startTime = SystemUtilities.currentTimeMillis();
	    lease.handle(le);
	    return true;

	  case LeaseEvent.RENEWED:
	    scheduleRenewal(le.duration);
	    return true;

          case LeaseEvent.TIMED:
	    // We may be recovering from a lost RENEWED event.
	    if (guid.equals(event.closure)) {
	      scheduleRenewal(le.duration);
	    }
	    return true;

          case LeaseEvent.CANCELED:
	    // Do nothing.
	    return true;
	}
      }

      // If we don't know what the event is, pass it on to the
      // notification handler.
      notify.handle(event);
      return true;
    }

    /** Schedule the reminder to renew the lease. */
    void scheduleRenewal(long duration) {

      synchronized (lock) {

        // We use a guid to keep track of which renewal we are on.
        guid = new Guid();

        Event timedEvent =
	    new LeaseEvent(this, guid, LeaseEvent.RENEW, 
		           null, null, DURATION);

        if (timerNotification != null) {
          timerNotification.cancel();
        }
        
        timerNotification =
          timer.schedule(Timer.FIXED_DELAY, 
                         startTime + duration/2,
                         Constants.TIMER_PERIOD,
                         this, 
                         timedEvent);
      }
    }
  }

  // ==================================================================
  //                     Static lease cancelation
  // ==================================================================


  /**
   * Cancel the lease managed by the specified event handler. This
   * method sends a cancel lease event to the specified event handler
   * managing a lease and <i>asynchronously</i> processes the
   * response.
   *
   * <p>Calling this method is equivalent to calling<pre>
   *   lease.handle(new
   *     LeaseEvent(CANCEL, null, LeaseEvent.CANCEL,
   *                null, null, 0));
   * </pre></p>
   *
   * <p>The specified event handler must be an event handler managing
   * a lease.</p>
   *
   * @see    Cancel
   * @see    #CANCEL
   * 
   * @param  lease The event handler managing the lease to cancel.
   */
  public static void cancel(EventHandler lease) {
    try {
      lease.handle(new
        LeaseEvent(CANCEL, null, LeaseEvent.CANCEL, null, null, 0));
    } catch (IllegalStateException x) {
      // Nothing to worry about.
      return;
    }
  }  

  // ===================================================================
  //                     The cancel handler
  // ===================================================================

  /**
   * The event handler for processing lease cancellation. This event
   * handler simply consumes canceled lease events without taking any
   * action. It also consumes exceptional events signalling that the
   * lease has already been canceled.
   *
   * @see  LeaseEvent
   */
  public static final class Cancel extends AbstractHandler {

    /** Handle the specified event. */
    protected boolean handle1(Event e) {
      if (((e instanceof LeaseEvent) &&
           (LeaseEvent.CANCELED == ((LeaseEvent)e).type)) ||
          ((e instanceof ExceptionalEvent) &&
           ((((ExceptionalEvent)e).x instanceof LeaseRevokedException) || 
	   (((ExceptionalEvent)e).x instanceof ResourceRevokedException)))) {
        return true;
      } else {
        return false;
      }
    }
  }

  /** The event handler for processing lease cancellations. */
  public static final Cancel CANCEL = new Cancel();
}
