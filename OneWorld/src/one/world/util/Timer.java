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

package one.world.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.security.AccessController;
import java.security.PrivilegedAction;

import java.util.Date;

import one.util.Bug;

import one.world.Constants;

import one.world.binding.Duration;

import one.world.core.Component;
import one.world.core.ComponentDescriptor;
import one.world.core.Domain;
import one.world.core.Environment;
import one.world.core.Event;
import one.world.core.EventHandler;
import one.world.core.ExceptionalEvent;
import one.world.core.ExportedDescriptor;
import one.world.core.ImportedDescriptor;
import one.world.core.InvalidTupleException;
import one.world.core.NoBufferSpaceException;
import one.world.core.NotActiveException;
import one.world.core.TupleException;

/**
 * Implementation of a timer. A timer is a component that provides
 * event notification based on time. Notification can be once or
 * periodically, in which case it can be either fixed rate or fixed
 * delay, as described for <code>java.util.Timer</code>.
 *
 * <p>This component exports a request event handler that accepts
 * timer events requesting to schedule timed notification(s). This
 * event handler is forcibly linked through a concurrency domain to
 * ensure that timer notifications complete quickly. This component
 * also provides a synchronous interface through the {@link #schedule}
 * method.</p>
 *
 * <p>Unlike other resources in <i>one.world</i>, the timed
 * notification provided by this component is not leased and, if it is
 * periodic, must be explicitly canceled. As a result, this component
 * can be used to implement leases.</p>
 *
 * <p>If a timed notification results in an exceptional condition
 * other than a {@link NoBufferSpaceException}, any future
 * notification is automatically canceled. Furthermore, if the event
 * handler receiving timed notification is currently not active (as
 * determined by {@link Domain#isActive}), the notification is
 * silently dropped.</p>
 *
 * <p>Note that each environment has its own timer component, which
 * can be accessed through {@link Component#getTimer}.</p>
 *
 * <p><b>Imported and Exported Event Handlers</b></p>
 *
 * <p>Exported event handler(s):<dl>
 *    <dt>request</dt>
 *    <dd>Handles timer events requesting the scheduling of timed
 *        notifications.
 *        </dd>
 * </dl></p>
 *
 * @version  $Revision: 1.25 $
 * @author   Robert Grimm
 */
public final class Timer extends Component {

  /** The serial version ID for this class. */
  static final long serialVersionUID = 1502484244540970018L;  

  // =======================================================================
  //                             Public constants
  // =======================================================================

  /** The type code for scheduling a timer. */
  public static final int SCHEDULE    = 1;

  /** The type code for a scheduled timer. */
  public static final int SCHEDULED   = 2;

  /** The type code for canceling a timer. */
  public static final int CANCEL      = 3;

  /** The type code for a canceled timer. */
  public static final int CANCELED    = 4;

  /** The frequency code for a one-time timer. */
  public static final int ONCE        = 1;

  /** The frequency code for a fixed rate timer. */
  public static final int FIXED_RATE  = 2;

  /** The frequency code for a fixed delay timer. */
  public static final int FIXED_DELAY = 3;


  // =======================================================================
  //                             Timer events
  // =======================================================================

  /** Definition of a timer event. */
  public static final class Event extends TypedEvent {

    /** The serial version ID for this class. */
    //static final long serialVersionUID = -5762503868173552350L;

    /**
     * The frequency. This field specifies the frequency code, which
     * must be {@link Timer#ONCE}, {@link Timer#FIXED_RATE}, or {@link
     * Timer#FIXED_DELAY}.
     *
     * @serial  Must be one of the frequency codes defined in
     *          {@link Timer}.
     */
    public int                  frequency;

    /**
     * The first time. This field specifies the first time at which to
     * send timed notification as an absolute time in milliseconds.
     *
     * @serial  Must be a non-negative time in milliseconds.
     */
    public long                 firstTime;

    /**
     * The period. This field specifies the period for repeated
     * notifications in milliseconds.
     *
     * @serial  Must be a positive duration for fixed rate and fixed
     *          delay frequencies.
     */
    public long                 period;

    /**
     * The handler. This handler is the event handler to send timed
     * notification(s) to for scheduling a timer and it is the event
     * handler to cancel the notification(s) for a scheduled timer.
     *
     * @serial  Must not be <code>null</code> for scheduling and
     *          scheduled timers.
     */
    public EventHandler         handler;

    /**
     * The event to express the timed notification. The source of this
     * event is changed before notification to reference the event
     * handler that can be used to cancel the timer.
     *
     * @serial  Must be a valid event for scheduling timers.
     */
    public one.world.core.Event event;

    /** Create a new, empty timer event. */
    public Event() {
      // Nothing to do.
    }

    /**
     * Create a new timer event to signal the canceling of a timer or
     * a canceled timer.
     *
     * @param  source   The source for the new timer event.
     * @param  closure  The closure for the new timer event.
     * @param  cancel   The flag for whether the new timer event
     *                  signal the canceling of a timer
     *                  (<code>true</code>) or a canceled timer
     *                  (<code>false</code>).
     */
    public Event(EventHandler source, Object closure, boolean cancel) {
      super(source, closure, (cancel? CANCEL : CANCELED));
    }

    /**
     * Create a new timer event.
     *
     * @param  source     The source for the new timer event.
     * @param  closure    The closure for the new timer event.
     * @param  type       The type for the new timer event.
     * @param  frequency  The frequency for the new timer event.
     * @param  firstTime  The first time for the new timer event.
     * @param  period     The period for the new timer event.
     * @param  handler    The event handler for the new timer event.
     * @param  event      The event for the new timer event.
     */
    public Event(EventHandler source, Object closure, int type,
                 int frequency, long firstTime, long period,
                 EventHandler handler, one.world.core.Event event) {
      
      super(source, closure, type);
      this.frequency = frequency;
      this.firstTime = firstTime;
      this.period    = period;
      this.handler   = handler;
      this.event     = event;
    }

    /** Validate this timer event. */
    public void validate() throws TupleException {
      super.validate();

      switch (type) {
      case SCHEDULE:
        if (null == event) {
          throw new InvalidTupleException("Null event for timer event (" +
                                          this + ")");
        }
        event.validate();
        // Fall through.

      case SCHEDULED:
        if ((ONCE > frequency) || (FIXED_DELAY < frequency)) {
          throw new InvalidTupleException("Invalid frequency for timer event ("
                                          + this + ")");
        } else if (0 > firstTime) {
          throw new InvalidTupleException("Negative first time for " +
                                          "timer event (" + this + ")");
        } else if ((ONCE != frequency) && (0 >= period)) {
          throw new InvalidTupleException("Non-positive period for timer " +
                                          "event (" + this + ")");
        } else if (null == handler) {
          throw new InvalidTupleException("Null event handler for timer " +
                                          "event (" + this + ")");
        }
        break;

      case CANCEL:
      case CANCELED:
        break;

      default:
        throw new InvalidTupleException("Invalid type for timer event (" +
                                        this + ")");
      }
    }

  }


  // =======================================================================
  //                        The notification handler
  // =======================================================================

  /**
   * The notification handler. A notification handler handles the
   * timed notification(s) for a single schedule.
   */
  public final static class Notification extends AbstractHandler {

    /** The serial version ID for this class. */
    static final long serialVersionUID = 5780998194444789292L;

    /** The actual timer task. */
    final class Task extends java.util.TimerTask {

      /** Run this timer task. */
      public void run() {
        if (ONCE == frequency) {
          frequency = CANCELED;
        }

        if (Constants.DEBUG_TIMER) {
          SystemLog.LOG.log(Notification.this, "Sending timer notification " + 
                            event + " to " + handler);
        }

        try {
          if (! Domain.isActive(handler)) {
            if (Constants.DEBUG_TIMER) {
              SystemLog.LOG.log(Notification.this, "Dropping timer " +
                                "notification " + event + " to " + handler);
            }
            return;
          }
          handler.handle(event);
        } catch (NoBufferSpaceException x) {
          if (Constants.DEBUG_TIMER) {
            SystemLog.LOG.logWarning(Notification.this,
                                     "No buffer space while sending timer " +
                                     "notification", x);
          }
        } catch (Throwable x) {
          Notification.this.cancel();

          boolean notify = false;
          if (x instanceof NotActiveException) {
            if (Constants.DEBUG_TIMER) {
              notify = true;
            }
          } else {
            notify = true;
          }

          if (notify) {
            EventHandler h;
            if (Domain.isWrapped(handler)) {
              h = (EventHandler)AccessController.doPrivileged(new
                PrivilegedAction() {
                  public Object run() {
                    return Domain.unwrap(handler);
                  }
                });
            } else {
              h = handler;
            }

            SystemLog.LOG.logWarning(Notification.this,
                                     "Unexpected exception while sending " +
                                     "timer notification to " + h, x);
          }
        }
      }

    }

    /**
     * The frequency.
     *
     * @serial  Must be a valid frequency.
     */
    private volatile int            frequency;

    /**
     * The first time.
     *
     * @serial  Must be a valid time.
     */
    private long                    firstTime;

    /**
     * The period.
     *
     * @serial  Must be a positive period for fixed rate and fixed
     *          delay frequencies.
     */
    private long                    period;

    /**
     * The event handler to send notifications to.
     *
     * @serial  Must not be <code>null</code>.
     */
    private EventHandler            handler;

    /**
     * The event to send as notification.
     *
     * @serial  Must be a valid event.
     */
    private one.world.core.Event    event;

    /** The actual timer task. */
    private transient Task          task;

    /**
     * Create a new notification handler. All arguments must be valid.
     *
     * @param  frequency  The frequency.
     * @param  firstTime  The first time.
     * @param  period     The period.
     * @param  handler    The handler to receive notification(s).
     * @param  event      The event to signal notification(s).
     */
    Notification(int frequency, long firstTime, long period,
                 EventHandler handler, one.world.core.Event event) {
      this.frequency    = frequency;
      this.firstTime    = firstTime;
      this.period       = period;
      this.handler      = handler;
      this.event        = event;
      this.event.source = this;
      task              = new Task();

      schedule();
    }

    /** Deserialize a notification handler. */
    private void readObject(ObjectInputStream in)
      throws IOException, ClassNotFoundException {

      in.defaultReadObject();

      // Only schedule timer task if notification has not been
      // canceled.
      if (CANCELED != frequency) {
        task = new Task();

        // Adjust first time for fixed rate and fixed delay
        // frequencies.
        if (ONCE != frequency) {
          long now = System.currentTimeMillis();

          if (firstTime < now) {
            firstTime += (((now - firstTime) / period) + 1) * period;
          }
        }

        schedule();
      }
    }

    /** Schedule the actual timer task for this notification handler. */
    private void schedule() {
      switch (frequency) {

      case ONCE:
        if (Constants.DEBUG_TIMER) {
          SystemLog.LOG.log(this, "Scheduling once at " +
                            SystemUtilities.format(firstTime));
        }
        timer.schedule(task, new Date(firstTime));
        break;

      case FIXED_RATE:
        if (Constants.DEBUG_TIMER) {
          SystemLog.LOG.log(this, "Scheduling fixed rate " +
                            Duration.format(period) + " starting at " +
                            SystemUtilities.format(firstTime));
        }
        timer.scheduleAtFixedRate(task, new Date(firstTime), period);
        break;

      case FIXED_DELAY:
        if (Constants.DEBUG_TIMER) {
          SystemLog.LOG.log(this, "Scheduling fixed delay " +
                            Duration.format(period) + " starting at " +
                            SystemUtilities.format(firstTime));
        }
        timer.schedule(task, new Date(firstTime), period);
        break;

      default:
        throw new Bug("Invalid frequency for notification handler (" + this +
                      ")");
      }
    }

    /** Handle the specified event. */
    protected boolean handle1(one.world.core.Event e) {

      // Validate the event.
      if (isNotValid(e)) {
        return true;
      }

      // Process the event.
      if (e instanceof Timer.Event) {
        Timer.Event event = (Timer.Event)e;

        if (CANCEL == event.type) {
          cancel();
          respond(event, new Timer.Event(this, null, false));
          return true;
        }

      } else if (e instanceof ExceptionalEvent) {
        ExceptionalEvent ee = (ExceptionalEvent)e;

        if (ee.x instanceof NoBufferSpaceException) {
          if (Constants.DEBUG_TIMER) {
            SystemLog.LOG.logWarning(this, "No buffer space while sending " +
                                     "timer notification", ee.x);
          }
          return true;

        } else {
          boolean notify = false;
          if (ee.x instanceof NotActiveException) {
            if (Constants.DEBUG_TIMER) {
              notify = true;
            }
          } else {
            notify = true;
          }

          if (notify) {
            SystemLog.LOG.logWarning(this, "Unexpected exceptional event",
                                     ee.x);
          }

          cancel();
          return true;
        }
      }

      return false;
    }

    /**
     * Cancel the timed notification(s) for this notification
     * handler. Calling this method on an already canceled
     * notification handler has no effect.
     */
    public void cancel() {
      if (Constants.DEBUG_TIMER) {
        SystemLog.LOG.log(this, "Canceling notification");
      }
      frequency = CANCELED;
      task.cancel();
    }

  }


  // =======================================================================
  //                           The request handler
  // =======================================================================

  /** The request exported event handler. */
  final static class RequestHandler extends AbstractHandler {

    /** The serial version ID for this class. */
    static final long serialVersionUID = 2711511834825087847L;

    /** Handle the specified event. */
    protected boolean handle1(one.world.core.Event e) {

      // Validate the event.
      if (isNotValid(e)) {
        return true;
      }

      // Process the event.
      if (e instanceof Timer.Event) {
        Timer.Event event = (Timer.Event)e;

        if (SCHEDULE == event.type) {
          respond(event, new
            Timer.Event(this, null, SCHEDULED,
                        event.frequency, event.firstTime, event.period,
                        new Notification(event.frequency, event.firstTime,
                                         event.period, event.handler,
                                         event.event), null));
          return true;
        }
      }

      return false;
    }

  }


  // =======================================================================
  //                           Descriptors
  // =======================================================================

  /** The component descriptor. */
  private static final ComponentDescriptor SELF =
    new ComponentDescriptor("one.world.util.Timer",
                            "A time-dependent generator of events",
                            true);

  /** The exported event handler descriptor for the request handler. */
  private static final ExportedDescriptor REQUEST =
    new ExportedDescriptor("request",
                           "The request handler",
                           new Class[] { Timer.Event.class },
                           null,
                           true);


  // =======================================================================
  //                        The underlying timer
  // =======================================================================

  /** The underlying timer. */
  private static final java.util.Timer timer = new java.util.Timer(true);


  // =======================================================================
  //                           Constructor
  // =======================================================================

  /**
   * Create a new instance of <code>Timer</code>.
   *
   * @param  env  The environment for the new instance.
   */
  public Timer(Environment env) {
    super(env);
    declareExported(REQUEST, new RequestHandler());
  }


  // =======================================================================
  //                           Component support
  // =======================================================================

  /** Get the component descriptor. */
  public ComponentDescriptor getDescriptor() {
    return (ComponentDescriptor)SELF.clone();
  }


  // =======================================================================
  //                        Synchronous scheduling
  // =======================================================================

  /**
   * Synchronously schedule timed notification(s). The event to signal
   * notification(s) must be a valid event.
   *
   * @param   frequency  The frequency.
   * @param   firstTime  The first time.
   * @param   period     The period.
   * @param   handler    The handler to receive notification(s).
   * @param   event      The event to signal notification(s).
   * @return             The notification handler managing the scheduled
   *                     notification(s).
   * @throws  NullPointerException
   *                     Signals that <code>handler</code> or
   *                     <code>event</code> is <code>null</code>.
   * @throws  IllegalArgumentException
   *                     Signals an invalid frequency, a negative first
   *                     time, or a non-positive period.
   */
  public Notification schedule(int frequency, long firstTime, long period,
                               EventHandler handler, 
                               one.world.core.Event event) {
    // Validate arguments.
    if (null == event) {
      throw new NullPointerException("Null event");
    }
    if ((ONCE > frequency) || (FIXED_DELAY < frequency)) {
      throw new IllegalArgumentException("Invalid frequency ("+frequency+")");
    } else if (0 > firstTime) {
      throw new IllegalArgumentException("Negative first time");
    } else if ((ONCE != frequency) && (0 >= period)) {
      throw new IllegalArgumentException("Non-positive period");
    } else if (null == handler) {
      throw new NullPointerException("Null event handler");
    }
     
    // Actually schedule timed notification(s).
    return new Notification(frequency, firstTime, period, wrap(handler), event);
  }

}
