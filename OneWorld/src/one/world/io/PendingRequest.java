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

package one.world.io;

import one.world.core.Event;
import one.world.core.EventHandler;
import one.world.core.ExceptionalEvent;
import one.world.core.Tuple;

import one.world.binding.LeaseEvent;
import one.world.binding.LeaseDeniedException;

import one.world.data.Name;

import one.world.util.AbstractHandler;

import one.util.Guid;


/**
 * Encapsulates a pending input request, allowing it to be leased and
 * removed.  
 *
 * @see InputRequest
 * @see SimpleInputRequest
 * 
 * @version  $Revision: 1.3 $
 * @author   Janet Davis
 */
public final class PendingRequest extends AbstractHandler {

  /** 
   * The interface required for collections of pending requests. 
   */
  public interface Collection {

    /** 
     * Removes a pending request from the collection.
     *
     * @param id    The ID of the request to remove.
     * @return      The removed pending request, or <code>null</code>
     *              if the request was not in the collection.
     */
    public PendingRequest remove(Guid id);
  }

  /** The pending request collection for this pending request. */
  final Collection collection;

  /** The input request id. */
  final Guid id;

  /** 
   * The input request type: {@link InputRequest#LISTEN} or
   * {@link InputRequest#READ}.
   */
  final int type;

  /** The query filter. */
  TupleFilter filter;

  /** Flag to indicate whether to response with the tuple id only. */
  final boolean idOnly;

  /** The original input request. */
  final Event request;

  /** The source to use for responses to this request. */
  final EventHandler handler;

  /** The revokation flag. */
  volatile boolean isRevoked = false;

  /**
   * Construct a pending request record from the given request.
   *
   * @param request  The original input request.
   * @param handler  The handler creating the input request.  This is
   *         to be used as the source of responses to the request.
   * @param collection The collection to which this request will belong.
   *
   * @throws UnsupportedOperationException Indicates that the request
   *         operation is not supported.  The only operation that is not
   *         supported is {@link InputRequest#QUERY}.
   */
  public PendingRequest(InputRequest request, EventHandler handler,
                 Collection collection)  
          throws UnsupportedOperationException {

    if (request.type == InputRequest.QUERY) {
      throw new UnsupportedOperationException("Does not support QUERY"
                                                + " input operations");
    }

    this.id = request.id;
    this.type = request.type;
    this.filter = new TupleFilter(request.query);
    this.idOnly = request.idOnly;
    this.request = request;
    this.handler = handler;
    this.collection = collection;
  }

  /**
   * Construct a pending request record from the given request.
   *
   * @param request  The original input request.
   * @param handler  The handler creating the input request.  This is
   *         to be used as the source of responses to the request.
   * @param collection The collection to which this request will belong.
   */
  public PendingRequest(SimpleInputRequest request, EventHandler handler,
                 Collection collection) {
                   
    // SimpleInputRequest types must be converted to InputRequest types.
    if (request.type == SimpleInputRequest.READ) {
      this.type = InputRequest.READ;
    } else {
      this.type = InputRequest.LISTEN;
    }

    this.id = request.id;
    this.filter = new TupleFilter(request.query);
    this.idOnly = request.idOnly;
    this.request = request;
    this.handler = handler;
    this.collection = collection;
  }

  /** 
   * Requests a lease for this pending request.  This is called after
   * the pending request is added to the collection.
   *
   * @param leaseHandler The lease request handler.
   * @param duration     The requested lease duration.
   */ 
  public void requestLease(EventHandler leaseHandler, long duration) {

    Tuple descriptor = new Name("#[Pending request " + id + "]");

    leaseHandler.handle(
        new LeaseEvent(this, null, LeaseEvent.ACQUIRE,
                         this, descriptor, duration));
  }

  /**
   * Inform the requester of a matched tuple.
   * 
   * @param tuple    The matching tuple.
   */
  public void sendResult(Tuple tuple) {
    Event response = null;

    if (idOnly) {
      response = new InputByIdResponse(handler, request.closure, 
                                       (Guid)tuple.get("id"));
    } else {
      response = new InputResponse(handler, request.closure, tuple);
    }

    if (request.source != null) {
      request.source.handle(response);
    }
  }

  /**
   * Handles {@link one.world.binding.LeaseEvent#ACQUIRED lease acquired}
   * events, {@link one.world.binding.LeaseEvent#CANCELED lease canceled}
   * events, and {@link one.world.binding.LeaseDeniedException 
   * LeaseDeniedException}s.
   */
  protected boolean handle1(Event event) {
    if (event instanceof LeaseEvent) {
      LeaseEvent levent = (LeaseEvent)event;
        if (levent.type == LeaseEvent.ACQUIRED) {
          // Send a listen response only for listen requests
          if (type == InputRequest.LISTEN) {
            respond(request, 
                    new ListenResponse(handler, null,
                                       levent.handler, levent.duration));
          }
          return true;

        } else if (levent.type == LeaseEvent.CANCELED) {
          revoke();
          return true;
        }
	
    } else if (event instanceof ExceptionalEvent) {
      Throwable x = ((ExceptionalEvent)event).x;
      if (x instanceof LeaseDeniedException) {
          isRevoked = true;
          collection.remove(this.id);
          respond(request, x);
          return true;
        }
    }
    return false;
  }

  /** Revokes this pending input request. */
  private void revoke() {
    boolean wasRevoked;

    synchronized (this) {
      wasRevoked = isRevoked;
      isRevoked = true;
    }

    if (!wasRevoked) {

      // Remove the request.
      PendingRequest removed = collection.remove(this.id);

      // Only notify of the request's expiration if it is an unsatisfied
      // read operation.
      if ((removed != null) && (type == InputRequest.READ)) {
        sendExpired();
      }
    }
  }
 
  /**
   * Inform the requester of a timed-out input request.
   * 
   * @param pRequest  The pending input request.
   */
  private void sendExpired() {
    if (request.source != null) {
      request.source.handle(
            new InputResponse(handler, request.closure, null));
    }
  }
}
