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
import java.io.NotSerializableException;
import java.io.ObjectOutputStream;

import one.world.Constants;

import one.world.binding.Duration;

import one.world.core.Event;
import one.world.core.EventHandler;
import one.world.core.ExceptionalEvent;
import one.world.core.QueuedEventHandler;

/**
 * Implementation of synchronous event handling. This class implements
 * static helper methods to support synchronous event handling for
 * simple request-response interactions. The methods implemented by
 * this class can only be used on event handlers that generate exactly
 * one event as a result of their application. Furthermore, the
 * methods in this class should only be used sparingly, as they
 * capture the executing thread.
 *
 * <p><b>Warning:</b> <i>Synchronous invocations may result in
 * deadlock.  In particular, performing n concurrent synchronous
 * invocations in an environment with n threads results in deadlock,
 * if all n invocations go through the environment's animator (for
 * example, by interacting with the root environment). As a result,
 * components that are not thread-safe must not perform synchronous
 * invocations.</i></p>
 *
 * <p><i>Instead of using a synchronous invocation for a
 * request/response interaction consider using an {@link Operation}.
 * Operations do not capture the executing thread and are more
 * flexible since they support retries for timed out request/response
 * interactions. To use an operation, you need to write an additional
 * event handler, called the continuation, that handles the result of
 * the request/response interaction.</i></p>
 * 
 * @version  $Revision: 1.14 $
 * @author   Robert Grimm
 */
public class Synchronous {

  /**
   * Implementation of the result handler for synchronous event
   * handler invocations. Note that result handlers are <i>not</i>
   * serializable.
   */
  public static class ResultHandler extends AbstractHandler {

    /** The skew for timeouts. */
    private static final long SKEW = 100;
    
    /**
     * Flag for whether the result handler has already seen a result.
     *
     * @serial
     */
    private boolean hasResult;

    /**
     * The resulting event.
     *
     * @serial  Must not be <code>null</code> if <code>hasResult</code>
     *          is <code>true</code>.
     */
    private Event   result;

    /** Create a new result handler. */
    public ResultHandler() {
      // Nothing to construct.
    }

    /**
     * Serialize this result handler. This method throws a
     * <code>NotSerializableException</code>, as result handlers are
     * not serializable.
     *
     * @serialData  Nothing as result handlers are not serializable.
     */
    private void writeObject(ObjectOutputStream out) throws IOException {
      throw new NotSerializableException(getClass().getName());
    }

    /** Handle the specified event. */
    protected boolean handle1(Event e) {
      synchronized (this) {
        if (! hasResult) {
          result    = e;
          hasResult = true;
          notify();
          return true;
        }
      }

      // Log error and respond with an illegal state exception.
      SystemLog.LOG.logError(this, "Duplicate result for synchronous event " +
                             "invocation (" + e + ")");
      respond(e, new
        ExceptionalEvent(this, null, new
          IllegalStateException("Duplicate result for synchronous event " +
                                "invocation (" + e + ")")));
      return true;
    }

    /**
     * Get the result for this result handler. This method times out
     * after {@link Constants#SYNCHRONOUS_TIMEOUT} milliseconds.
     *
     * @return     The resulting event.
     * @throws  TimeOutException
     *             Signals that the synchronous invocation has timed
     *             out without receiving a result.
     */
    public Event getResult() {
      return getResult1(Constants.SYNCHRONOUS_TIMEOUT);
    }

    /**
     * Get the result for this result handler. This method times out
     * after the specified duration.
     *
     * @param   timeout  The timeout for waiting for the result.
     * @return           The resulting event.
     * @throws  IllegalArgumentException
     *                   Signals that <code>timeout</code> is negative.
     * @throws  TimeOutException
     *                   Signals that the synchronous invocation has
     *                   timed out without receiving a result.
     */
    public Event getResult(long timeout) {
      if (0 > timeout) {
        throw new IllegalArgumentException("Negative timeout ("+timeout+")");
      }

      return getResult1(timeout);
    }

    /**
     * Get the result for this result handler. This method times out
     * after the specified duration, which must be non-negative.
     *
     * @param   timeout  The timeout for waiting for the result.
     * @return           The resulting event.
     * @throws  TimeOutException
     *                   Signals that the synchronous invocation has
     *                   timed out without receiving a result.
     */
    Event getResult1(long timeout) {
      long now   = System.currentTimeMillis();
      long delay = timeout;
      long end   = now + timeout - SKEW;

      synchronized (this) {
        while ((! hasResult) && (now < end)) {
          try {
            wait(delay);
          } catch (InterruptedException x) {
            // Ignore.
          }

          now   = System.currentTimeMillis();
          delay = end - now + SKEW;
        }

        if (hasResult) {
          return result;
        } else {
          throw new TimeOutException("Synchronous invocation timed out " +
                                     "after " + Duration.format(timeout));
        }
      }
    }

    /**
     * Reset this result handler. This method resets this result
     * handler so that it can be re-used for another synchronous
     * invocation. This method should only be called if no thread is
     * waiting for a result on this result handler.
     */
    public void reset() {
      synchronized (this) {
        hasResult = false;
        result    = null;
      }
    }

  }

  /** Hide all constructors. */
  private Synchronous() {
    // Nothing to construct.
  }

  /**
   * Synchronously invoke the specified event handler on the specified
   * event and return the resulting event. This method times out after
   * {@link Constants#SYNCHRONOUS_TIMEOUT} milliseconds. Note that
   * this method overrides the source of the specified event.
   *
   * <p>Modulo error checking, this method is equivalent to:<pre>
   *   ResultHandler r = new ResultHandler();
   *   event.source    = r;
   *   handler.handle(event);
   *   return r.getResult();
   * </pre></p>
   *
   * @param   handler  The event handler.
   * @param   event    The event to invoke the event handler on.
   * @param   timeout  The timeout for waiting for the result.
   * @return           The resulting event.
   * @throws  NullPointerException
   *                   Signals that <code>handler</code> or
   *                   <code>event</code> is <code>null</code>.
   * @throws  TimeOutException
   *                   Signals that the synchronous invocation has
   *                   timed out without receiving a result.
   */
  public static Event invoke(EventHandler handler, Event event) {
    if (null == handler) {
      throw new NullPointerException("Null event handler");
    } else if (null == event) {
      throw new NullPointerException("Null event");
    }

    // Set up event with a new result handler.
    ResultHandler r = new ResultHandler();
    event.source    = r;

    // Invoke the event handler on the event.
    handler.handle(event);

    // Wait for the result.
    return r.getResult1(Constants.SYNCHRONOUS_TIMEOUT);
  }

  /**
   * Synchronously invoke the specified event handler on the specified
   * event and return the resulting event. This method times out after
   * the specified duration. Note that this method overrides the
   * source of the specified event.
   *
   * <p>Modulo error checking, this method is equivalent to:<pre>
   *   ResultHandler r = new ResultHandler();
   *   event.source    = r;
   *   handler.handle(event);
   *   return r.getResult(timeout);
   * </pre></p>
   *
   * @param   handler  The event handler.
   * @param   event    The event to invoke the event handler on.
   * @param   timeout  The timeout for waiting for the result.
   * @return           The resulting event.
   * @throws  NullPointerException
   *                   Signals that <code>handler</code> or
   *                   <code>event</code> is <code>null</code>.
   * @throws  IllegalArgumentException
   *                   Signals that <code>timeout</code> is
   *                   negative.
   * @throws  TimeOutException
   *                   Signals that the synchronous invocation has
   *                   timed out without receiving a result.
   */
  public static Event invoke(EventHandler handler, Event event, long timeout) {
    if (null == handler) {
      throw new NullPointerException("Null event handler");
    } else if (null == event) {
      throw new NullPointerException("Null event");
    } else if (0 > timeout) {
      throw new IllegalArgumentException("Negative timeout (" + timeout + ")");
    }

    // Set up event with a new result handler.
    ResultHandler r = new ResultHandler();
    event.source    = r;

    // Invoke the event handler on the event.
    handler.handle(event);

    // Wait for the result.
    return r.getResult1(timeout);
  }

}
