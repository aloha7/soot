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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import one.util.Guid;

import one.world.Constants;

import one.world.core.Event;
import one.world.core.EventHandler;
import one.world.core.ExceptionalEvent;
import one.world.core.SymbolicHandler;
import one.world.core.Tuple;

import one.world.rep.RemoteEvent;

/**
 * Implementation of an operation. An operation manages a
 * request/response interaction by setting a timeout and retrying if
 * it does not receive a response within the timeout period.
 *
 * <p>An actual operation is started by invoking an operation on some
 * event. The operation uses the {@link #request} event handler for
 * the actual event processing. It sends the eventual result to the
 * {@link #continuation} event handler. A single instance of this
 * class can manage several concurrent request/response
 * interactions.</p>
 *
 * <p>To make sure that an operation sees the result of a
 * request/response interaction, it overrides the source of the
 * original event with the event handler returned by {@link
 * #getResponseHandler}.</p>
 *
 * <p>Furthermore, to maintain its internal state, an operation
 * replaces the event's closure with its own; it then restores the
 * original closure before passing the response to the
 * <code>continuation</code>. As a result, it is possible to use
 * <i>any</i> object as the closure for a request managed by an
 * operation, not just objects that are legal as values for a tuple
 * field. However, closure replacement fails for ping-pong-like
 * interactions, where both sides use operations. Applications that
 * want to use operations for ping-pong-like interactions need to use
 * {@link ChainingClosure chaining closures}.
 *
 * <p>To use an operation with remote events, an application needs to
 * first export the event handler returned by
 * <code>getResponseHandler()</code> through REP and then set the
 * source of all events embedded in a remote event to the
 * corresponding symbolic handler.</p>
 *
 * <p>For remote events, an operation replaces both the remote event's
 * closure and the embedded event's closure with its own closure and
 * restores them independently. Note that it does <i>not</i> unwrap
 * the remote event carrying the response, but rather passes the
 * entire remote event to the <code>continuation</code>.</p>
 *
 * @see      Tuple
 * @see      one.world.rep
 *
 * @version  $Revision: 1.16 $
 * @author   Robert Grimm
 */
public final class Operation extends AbstractHandler {

  /** The serial version ID for this class. */
  static final long serialVersionUID = 1191009112833187836L;

  /**
   * The maximum number of timed notifications finding an idle
   * operation before canceling the timed notifications.
   */
  static final int MAX_IDLE  = 11;

  /**
   * The frequency relative to the timeout period for timed
   * notifications to detect timed out request/response interactions.
   */
  static final int FREQUENCY = 10;

  /**
   * Implementation of a chaining closure. Chaining closures let
   * applications avoid closure replacement when using operations.
   * They thus support chained usage of operations in contrast to the
   * nested usage with closure replacement.
   *
   * <p>To maintain its internal state, an operation normally replaces
   * the event's closure with its own and then restores the original
   * closure before passing the result to the
   * <code>continuation</code>. This works fine when operations are
   * used for request/response interactions that mirror possibly
   * nested or recursive method invocations in synchronous
   * systems. However, when operations are used for ping-pong-like
   * interactions, such as those found in the {@link
   * one.radio.FetcherProtocol}, closure replacement does not
   * work.</p>
   *
   * <p>To illustrate the problem with closure replacement, here is the
   * summary of the fetcher protocol:<pre>
   *    Fetcher                     Fetchee
   *
   *      --------- Come here! --------->
   *      <----- I challenge you! -------
   *      --- I accept the challenge --->
   *      <--------- Coming... ----------
   * </pre>Consider an implementation of the fetcher protocol, where
   * the fetcher uses operations to send the "Come here!" and the "I
   * accept the challenge" messages and the fetchee uses an operation
   * to send the "I challenge you!" message. Even if the fetchee
   * correctly copies the closure from the "Come here!" message into
   * the "I challenge you!" message, its use of an operation results
   * in the closure being replaced and fetcher's operation never seing
   * an appropriate closure.</p>
   *
   * <p>Applications can use chaining closures to avoid closure
   * replacement and thus implement ping-pong-like interactions while
   * still relying on operations to detect timeouts and perform
   * retries. The contract is that when the closure of a request is a
   * chaining closure, an operation does not replace the
   * closure. Rather, it passes the provided closure through and
   * maintains its interal state based on the ID of the chaining
   * closure.</p>
   *
   * <p>In above example, the fetcher creates a new chaining closure
   * for the "Come here!" message and both the fetcher and the fetchee
   * then copy the closure of the previously received message into the
   * next message they are sending.</p>
   *
   * <p>Note that one instance of an operation can only manage one
   * concurrent interaction for a given chaining closure ID. If an
   * operation receives a request with a chaining closure ID it is
   * already managing, it silently drops the request (based on the
   * assumption that this event represents a retried event from within
   * earlier in the chain). For example, if the fetcher and the
   * fetchee in above example use retries, the fetcher must use two
   * different operations for sending the "Come here!" and the "I
   * accept the challenge" messages. However, if the fetcher and the
   * fetchee do not use retries, the fetcher can use the same
   * operation for both messages.</p>
   *
   * <p>Further note that for remote events, the chaining closure
   * must be the closure of the nested event.</p>
   *
   * <p>Finally note that when mixing chained and nested interaction
   * patterns, applications must replace chaining closures with some
   * other closure that is not a chaining closure before entering
   * nested interactions (and vice versa).</p> */
  public static class ChainingClosure extends Tuple {

    /** The serial version ID for this class. */
    static final long serialVersionUID = 1150383982627832267L;

    /** Create a new chaining closure. */
    public ChainingClosure() {
      // Nothing to do.
    }

    /**
     * Create a new chaining closure with the specified ID.
     *
     * @param   id  The ID for the new chaining closure.
     */
    public ChainingClosure(Guid id) {
      super(id);
    }

  }

  /**
   * Implementation of a ping event. A ping event indicates that we
   * need to check for timed out request/response interactions.
   */
  static final class Ping extends Event {
    
    /** The serial version ID for this class. */
    static final long serialVersionUID = -7219591662458670106L;

    /** Create a new, empty ping event. */
    public Ping() {
      // Nothing to do.
    }

  }

  /** The value for the table of pending request/response interactions. */
  static final class Value implements java.io.Serializable {

    /** The serial version ID for this class. */
    static final long serialVersionUID = 5217768673859720005L;

    /**
     * The key for this value.
     *
     * @serial  Must not be <code>null</code>.
     */
    final Guid          key;

    /**
     * The previous value in the doubly-linked list of pending
     * request/response interactions.
     *
     * @serial
     */
    Value               previous;
     
    /**
     * The next value in the doubly-linked list of pending
     * request/response interactions.
     *
     * @serial
     */
    Value               next;

    /**
     * The original closure.
     * 
     * @serial
     */
    final Object        closure;

    /**
     * The original closure of an event embedded in a remote event.
     *
     * @serial
     */
    final Object        innerClosure;

    /**
     * The request.
     *
     * @serial  Must be a valid event.
     */
    final Event         request;

    /**
     * The time for timing out this request/response interaction.
     *
     * @serial  Must be a valid time.
     */
    long                timeout;

    /**
     * The number of retries left for this request/response
     * interaction.
     *
     * @serial
     */
    int                 retries;

    /** Create a new dummy value. */
    Value() {
      key          = null;
      closure      = null;
      innerClosure = null;
      request      = null;
    }

    /**
     * Create a new value. The original closure is set to the closure
     * of the specified event. For remote events the inner closure is
     * set to the closure of the embedded event.
     *
     * @param   key      The key.
     * @param   request  The request.
     * @param   timeout  The timeout.
     * @param   retries  The number of retries left.
     */
    Value(Guid key, Event request, long timeout, int retries) {
      this.key       = key;
      closure        = request.closure;
      if (request instanceof RemoteEvent) {
        innerClosure = ((RemoteEvent)request).event.closure;
      } else {
        innerClosure = null;
      }
      this.request   = request;
      this.timeout   = timeout;
      this.retries   = retries;
    }

    /**
     * Insert this value in the doubly-linked list of pending
     * request/response interactions after the specified value.
     *
     * @param  before  The value before this value.
     */
    void insert(Value before) {
      next          = before.next;
      previous      = before;
      next.previous = this;
      before.next   = this;
    }

    /**
     * Remove this value from the doubly-linked list of pending
     * request/response interactions.
     */
    void remove() {
      previous.next = next;
      next.previous = previous;
      previous      = null;
      next          = null;
    }

  }

  /**
   * The event handler for processing responses.
   *
   * <p>Note that responses MUST be handled by a different event
   * handler than the one handling the original requests. In an
   * earlier version of this class requests and responses were handled
   * by the same event handler.  However, requests and responses were
   * distinguished by the type of closure. This can result in
   * unexpected behavior when applications pass in an original event
   * with the same closure type as the closure type used
   * internally. Even if we used an internal, private class to
   * represent the closure, this would result in unexpected behavior
   * for nested operations. Therefore, requests and responses are
   * managed by separate event handlers.</p>
   */
  final class ResponseHandler extends AbstractHandler {

    /** Handle the specified event. */
    protected boolean handle1(Event e) {
      // Handle ping events.
      if (e instanceof Ping) {
        checkForTimeOuts();
        return true;
      }

      Object closure;
      Object key;
      Value  value;

      // Extract the right closure.
      if (e instanceof RemoteEvent) {
        closure = ((RemoteEvent)e).event.closure;
      } else {
        closure = e.closure;
      }

      // Determine the key for the closure.
      if (closure instanceof Guid) {
        key = closure;
      } else if (closure instanceof ChainingClosure) {
        key = ((ChainingClosure)closure).id;
      } else {
        // This event does not relate to an on-going request/response
        // interaction.
        return false;
      }

      // We got a response. It can either be the first response
      // (which we pass to the continuation) or a repeated response
      // (which we silently drop).
      synchronized (pending) {
        value = (Value)pending.get(key);

        if (null == value) {
          return true;
        }

        pending.remove(key);
        value.remove();
      }

      // Restore the actual closure and invoke the continuation.
      e.closure = value.closure;
      if (e instanceof RemoteEvent) {
        ((RemoteEvent)e).event.closure = value.innerClosure;
      }
      continuation.handle(e);

      // Done.
      return true;
    }

    /** Check for timed out request/response interactions. */
    private void checkForTimeOuts() {
      long      now     = System.currentTimeMillis();
      ArrayList retry   = null;
      ArrayList timeOut = null;      

      synchronized (pending) {
        Value next = first.next;

        // Any work to do?
        if (next == last) {
          idle++;

          // If we have been idle for a long time, we cancel timed
          // notifications.
          if (MAX_IDLE <= idle) {
            if (null != notification) {
              notification.cancel();
              notification = null;
            }
            idle = 0;
          }

          // Done.
          return;
        }

        // Process all request/response interactions that have timed
        // out.
        while ((next != last) && (now > next.timeout)) {
          Value value = next;
          next        = value.next;

          if (0 == value.retries) {
            // No more retries left, we need to generate a time out
            // exception.
            pending.remove(value.key);
            value.remove();

            if (null == timeOut) {
              timeOut = new ArrayList();
            }
            timeOut.add(value);

          } else {
            // Retry.
            value.retries--;

            // Remove and re-insert value into the doubly-linked list
            // of pending request/response interactions.
            value.remove();
            value.timeout = now + timeout;
            value.insert(last.previous);

            // Reset idle count.
            idle = 0;

            if (null == retry) {
              retry = new ArrayList();
            }
            retry.add(value);
          }
        }
      }

      // Fire off retries.
      if (null != retry) {
        Iterator iter = retry.iterator();
        
        while (iter.hasNext()) {
          Value value = (Value)iter.next();
          request.handle(value.request);
        }
      }

      // Fire off time out exceptions.
      if (null != timeOut) {
        Iterator iter = timeOut.iterator();

        while (iter.hasNext()) {
          Value value = (Value)iter.next();
          continuation.handle(new
            ExceptionalEvent(Operation.this, value.closure, new
              TimeOutException("Operation timed out ("+Operation.this+")")));
        }
      }
    }

  }

  /**
   * The event handler that processes the request.
   *
   * @serial
   */
  public        EventHandler request;

  /**
   * The event handler that processes the response.
   *
   * @serial
   */
  public        EventHandler continuation;

  /**
   * The event handler that processes the response before passing it
   * to <code>continuation</code>.
   *
   * @serial  Must be a valid instance of <code>ResponseHandler</code>.
   */
  private final EventHandler response;

  /**
   * The table of pending request/response interactions. This field
   * is only package private to avoid synthetic accessors.
   *
   * @serial  Must not be <code>null</code>.
   */
  final         HashMap      pending;

  /**
   * The first (dummy) value for the doubly-linked list of pending
   * request/response interactions.
   *
   * @serial  Must not be <code>null</code>.
   */
  final         Value        first;

  /**
   * The last (dummy) value for the doubly-linked list of pending
   * request/response interactions.
   *
   * @serial  Must not be <code>null</code>.
   */
  final         Value        last;

  /**
   * The total number of retries.
   *
   * @serial  Must not be negative.
   */
  private final int          retries;

  /**
   * The timeout period.
   *
   * @serial  Must be positive.
   */
  private final long         timeout;

  /**
   * The timer. This field is only package private to avoid synthetic
   * accessors.
   *
   * @serial  Must not be <code>null</code>
   */
  final         Timer        timer;

  /**
   * The timer notification handler for timing out request/response
   * interactions.
   *
   * @serial
   */
  Timer.Notification         notification;

  /**
   * The idle count.
   *
   * @serial
   */
  int                        idle;

  /**
   * Create a new operation with the default number of retries and the
   * default timeout. Note that the request and continuation event
   * handlers may be <code>null</code>.
   *
   * @see     Constants#OPERATION_RETRIES
   * @see     Constants#OPERATION_TIMEOUT
   *
   * @param   timer         The timer.
   * @param   request       The request event handler.
   * @param   continuation  The continuation event handler.
   * @throws  NullPointerException
   *                        Signals that <code>timer</code> is
   *                        <code>null</code>.
   */
  public Operation(Timer timer, EventHandler request,
                   EventHandler continuation) {
    this(Constants.OPERATION_RETRIES, Constants.OPERATION_TIMEOUT,
         timer, request, continuation);
  }

  /**
   * Create a new operation. Note that the request and continuation
   * event handlers may be <code>null</code>.
   *
   * @param   retries       The number of retries.
   * @param   timeout       The timeout.
   * @param   timer         The timer.
   * @param   request       The request event handler.
   * @param   continuation  The continuation event handler.
   * @throws  NullPointerException
   *                        Signals that <code>timer</code> is
   *                        <code>null</code>.
   * @throws  IllegalArgumentException
   *                        Signals that <code>retries</code> is
   *                        negative or that <code>timeout</code> is
   *                        non-positive.
   */
  public Operation(int retries, long timeout, Timer timer,
                   EventHandler request, EventHandler continuation) {
    if (0 > retries) {
      throw new IllegalArgumentException("Negative number of retries (" +
                                         retries + ")");
    } else if (0 >= timeout) {
      throw new IllegalArgumentException("Non-positive timeout (" +
                                         timeout + ")");
    } else if (null == timer) {
      throw new NullPointerException("Null timer");
    }
    
    pending           = new HashMap();
    first             = new Value();
    last              = new Value();
    first.next        = last;
    last.previous     = first;
    this.retries      = retries;
    this.timeout      = timeout;
    this.timer        = timer;
    this.request      = request;
    this.response     = new ResponseHandler();
    this.continuation = continuation;
  }

  /** Finalize this operation. */
  protected void finalize() {
    if (null != notification) {
      notification.cancel();
    }
  }

  /**
   * Get the response event handler for this operation.
   *
   * @return   The response event handler for this operation.
   */
  public EventHandler getResponseHandler() {
    return response;
  }

  /** 
   * Handle the specified event.
   * 
   * <p>Note that the specified event must be a valid event with the
   * exception of the source, which is overridden by this method.
   * Furthermore, for remote events, the embedded event must have as
   * its source a symbolic handler obtained by exporting the event
   * handler returned by {@link #getResponseHandler}.</p>
   */
  protected boolean handle1(Event e) {
    if (e instanceof ExceptionalEvent) {
      // We don't handle exceptional events.
      return false;
    }

    ChainingClosure chain       = null;
    boolean         isRemote    = (e instanceof RemoteEvent);
    
    Object          newClosure;
    Guid            key;
    Value           value;

    // Determine the key and new closure.
    if (e.closure instanceof ChainingClosure) {
      chain      = (ChainingClosure)e.closure;
      key        = chain.id;
      newClosure = chain;
    } else if (isRemote &&
               (((RemoteEvent)e).event.closure instanceof ChainingClosure)) {
      chain      = (ChainingClosure)((RemoteEvent)e).event.closure;
      key        = chain.id;
      newClosure = chain;
    } else {
      key        = new Guid();
      newClosure = key;
    }

    // Determine the value.
    long check   = System.currentTimeMillis() + timeout;
    value        = new Value(key, e, check, retries);

    // Fix the source and closure of the original event.
    e.source  = response;
    e.closure = newClosure;
    if (isRemote) {
      ((RemoteEvent)e).event.closure = newClosure;
    }
    
    // Record the pending interaction.
    synchronized (pending) {
      if ((null != chain) && pending.containsKey(key)) {
        // We silently drop repeated requests for an on-going chained
        // interaction.
        return true;
      }

      pending.put(key, value);
      value.insert(last.previous);

      // Reset the idle count.
      idle = 0;

      // Schedule timed notifications if necessary.
      if (null == notification) {
        notification = timer.schedule(Timer.FIXED_DELAY, check,
                                      timeout / FREQUENCY, response,
                                      new Ping());
      }
    }

    // Start the actual request/response interaction.
    request.handle(e);
    return true;
  }

}
