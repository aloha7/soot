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

import one.world.core.EventHandler;
import one.world.core.Event;
import one.world.core.ExceptionalEvent;
import one.world.core.SymbolicHandler;
import one.world.core.Tuple;
import one.world.core.TupleException;
import one.world.core.UnknownEventException;

import one.world.rep.RemoteEvent;

/**
 * Implementation of an abstract event handler. An abstract event
 * handler provides a skeleton implementation of an event handler as
 * well as utility methods to send events and respond to events.
 *
 * @version  $Revision: 1.11 $
 * @author   Robert Grimm
 */
public abstract class AbstractHandler
  implements EventHandler, java.io.Serializable {

  /** The serial version ID for this class. */
  static final long serialVersionUID = 5282803070248303266L;

  /** Create a new abstract handler. */
  public AbstractHandler() {
    // Nothing to do.
  }

  /**
   * Handle the specified event. This method invokes
   * <code>handle1()</code> on the specified event. After that, it
   * simply returns for handled events.  For unhandled events, it logs
   * exceptional events to the system log and responds with an unknown
   * event exception to the source of the specified event for all
   * other events.
   *
   * @see     #handle1
   *
   * @param   e  The event to handle.
   */
  public void handle(Event e) {
    // Handle the event.
    if (handle1(e)) {
      return;
    }

    // Log exceptional events and respond with an unknown event
    // exception to all other events.
    if (e instanceof ExceptionalEvent) {
      SystemLog.LOG.logWarning(this, "Unexpected exceptional event",
                               ((ExceptionalEvent)e).x);
    } else {
      respond(e, new UnknownEventException(getClass() 
                                           + " does not recognize "
					   + e.toString()));
    }
  }

  /**
   * Handle the specified event. A concrete implementation of this
   * method should try to handle the specified event. If it is able to
   * handle the event, it returns <code>true</code>. Otherwise it
   * returns <code>false</code>.
   *
   * <p>Note that the specified event may not be valid. Event handlers
   * can easily validate an event by using the {@link
   * #isNotValid(Event)} method.</p>
   *
   * @param   e  The event to handle.
   * @return     <code>true</code> if the specified event was handled
   *             by this method.
   */
  protected abstract boolean handle1(Event e);

  /**
   * Validate the specified event. This method validates the specified
   * event. If the specified event is valid it returns with
   * <code>false</code>. Otherwise, it responds to the source of the
   * event with an appropriate tuple exception and returns
   * <code>true</code>.
   *
   * @param   e  The event to validate.
   * @return     <code>true</code> if the specified event is not
   *             valid.
   */
  protected boolean isNotValid(Event e) {
    try {
      e.validate();
    } catch (TupleException x) {
      respond(e, x);
      return true;
    }

    return false;
  }

  /**
   * Send the specified response to the source of the specified
   * request. Before sending the specified response, this method sets
   * the response's closure to the closure of the request.
   *
   * @param   request   The request to whose source to respond to.
   * @param   response  The response.
   * @throws  NullPointerException
   *                    Signals that either of the specified
   *                    events is <code>null</code>.
   */
  protected void respond(Event request, Event response) {
    if (null == request) {
      throw new NullPointerException("Null request");

    } else if (null == response) {
      throw new NullPointerException("Null repsonse");

    } else if (null == request.source) {
      // No-one to respond to.
      SystemLog.LOG.logError(this, "Null source handler (" + request + ")");

    } else {
      // Fix response's closure and send.
      response.closure = request.closure;
      request.source.handle(response);
    }
  }

  /**
   * Send an exceptional event to the source of the specified request.
   * This method creates a new exceptional event for the specified
   * throwable and sends it to the source of the specified request.
   * The source of the new exceptional event is this event handler and
   * the closure is the request's closure.
   *
   * @see     ExceptionalEvent
   *
   * @param   request   The request to whose source to respond to.
   * @param   x         The throwable for the new exceptional event.
   * @throws  NullPointerException
   *                    Signals that either <code>request</code> or
   *                    <code>x</code> is <code>null</code>.
   */
  protected void respond(Event request, Throwable x) {
    if (null == x) {
      throw new NullPointerException("Null throwable");
    }

    respond(request, new ExceptionalEvent(this, null, x));
  }

  /**
   * Remotely send the specified response to the source of the
   * specified request. This method sends a remote event to the
   * specified remote event handler, which asks that the specified
   * response should be sent to the source of the specified request.
   * Before doing so, this method sets the response's closure to the
   * closure of the request. The source of the newly created remote
   * event is this object and the closure is <code>null</code>.
   *
   * @param   remote    The event handler for processing remote events.
   * @param   request   The request.
   * @param   response  The response.
   * @throws  NullPointerException
   *                    Signals that <code>remote</code>,
   *                    <code>request</code>, or <code>response</code>
   *                    is <code>null</code>.
   * @throws  IllegalArgumentException
   *                    Signals that <code>request.source</code> is
   *                    not a {@link SymbolicHandler symbolic handler}.
   */
  protected void respond(EventHandler remote, Event request, Event response) {
    respond(remote, null, request, response);
  }

  /**
   * Remotely send the specified response to the source of the
   * specified request. This method sends a remote event to the
   * specified remote event handler, which asks that the specified
   * response should be sent to the source of the specified request.
   * Before doing so, this method sets the response's closure to the
   * closure of the request. The source of the newly created remote
   * event is this object.
   *
   * @param   remote    The event handler for processing remote events.
   * @param   closure   The closure for the newly created remote event.
   * @param   request   The request.
   * @param   response  The response.
   * @throws  NullPointerException
   *                    Signals that <code>remote</code>,
   *                    <code>request</code>, or <code>response</code>
   *                    is <code>null</code>.
   * @throws  IllegalArgumentException
   *                    Signals that <code>request.source</code> is
   *                    not a {@link SymbolicHandler symbolic handler}.
   */
  protected void respond(EventHandler remote, Object closure, Event request,
                         Event response) {

    if (null == remote) {
      throw new NullPointerException("Null remote handler");

    } else if (null == request) {
      throw new NullPointerException("Null request");

    } else if (null == response) {
      throw new NullPointerException("Null response");

    } else if (! (request.source instanceof SymbolicHandler)) {
      throw new IllegalArgumentException("Not a symbolic handler (" +
                                         request.source + ")");

    } else {
      // Fix response's closure and send.
      response.closure = request.closure;

      remote.handle(new
        RemoteEvent(this, closure, (SymbolicHandler)request.source, response));
    }
  }

  /**
   * Remotely send an exceptional event to the source of the specified
   * request. This method creates a new exceptional event for the
   * specified source and throwable and remotely sends it to the
   * source of the specified request. The source of the newly created
   * remote event is this object and the closure is <code>null</code>.
   *
   * @see     ExceptionalEvent
   *
   * @param   remote    The event handler for processing remote events.
   * @param   request   The request.
   * @param   source    The source for the new exceptional event.
   * @param   x         The throwable for the new exceptional event.
   * @throws  NullPointerException
   *                    Signals that <code>remote</code>,
   *                    <code>request</code>, <code>source</code>, or
   *                    <code>x</code> is <code>null</code>.
   * @throws  IllegalArgumentException
   *                    Signals that <code>request.source</code> or
   *                    <code>source</code> is not a {@link
   *                    SymbolicHandler symbolic handler}.
   */
  protected void respond(EventHandler remote, Event request,
                         SymbolicHandler source, Throwable x) {
    respond(remote, null, request, source, x);
  }

  /**
   * Remotely send an exceptional event to the source of the specified
   * request. This method creates a new exceptional event for the
   * specified source and throwable and remotely sends it to the
   * source of the specified request. The source of the newly created
   * remote event is this object.
   *
   * @see     ExceptionalEvent
   *
   * @param   remote    The event handler for processing remote events.
   * @param   closure   The closure for the newly created remote event.
   * @param   request   The request.
   * @param   source    The source for the new exceptional event.
   * @param   x         The throwable for the new exceptional event.
   * @throws  NullPointerException
   *                    Signals that <code>remote</code>,
   *                    <code>request</code>, <code>source</code>, or
   *                    <code>x</code> is <code>null</code>.
   * @throws  IllegalArgumentException
   *                    Signals that <code>request.source</code> or
   *                    <code>source</code> is not a {@link
   *                    SymbolicHandler symbolic handler}.
   */
  protected void respond(EventHandler remote, Object closure, Event request,
                         SymbolicHandler source, Throwable x) {
    if (null == source) {
      throw new NullPointerException("Null source");

    } else if (null == x) {
      throw new NullPointerException("Null throwable");

    } else if (! (source instanceof SymbolicHandler)) {
      throw new IllegalArgumentException("Not a symbolic handler (" + source +
                                         ")");
    }

    respond(remote, closure, request, new ExceptionalEvent(source, null, x));
  }

}
