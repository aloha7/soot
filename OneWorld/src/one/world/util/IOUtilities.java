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

import java.util.NoSuchElementException;

import one.util.Bug;
import one.util.Guid;

import one.world.Constants;

import one.world.binding.BindingRequest;
import one.world.binding.BindingResponse;
import one.world.binding.LeaseDeniedException;
import one.world.binding.LeaseEvent;
import one.world.binding.LeaseRevokedException;
import one.world.binding.ResourceRevokedException;
import one.world.binding.UnknownResourceException;

import one.world.core.Event;
import one.world.core.EventHandler;
import one.world.core.ExceptionalEvent;
import one.world.core.InvalidTupleException;
import one.world.core.Tuple;
import one.world.core.UnknownEventException;

import one.world.io.DeleteRequest;
import one.world.io.InputRequest;
import one.world.io.InputResponse;
import one.world.io.NoSuchTupleException;
import one.world.io.OutputRequest;
import one.world.io.OutputResponse;
import one.world.io.Query;
import one.world.io.QueryResponse;
import one.world.io.SimpleOutputRequest;
import one.world.io.SimpleInputRequest;

/**
 * Implementation of I/O utilities. This class provides supporting
 * functionality for binding resources, managing leases, and
 * performing structured I/O. It includes several synchronous helper
 * methods, which should be used with caution: They are intended for
 * utilities, but should only see limited use in applications. All
 * synchronous invocations performed by the methods in this class time
 * out after {@link Constants#SYNCHRONOUS_TIMEOUT}.
 *
 * <p><b>Note that this class will be removed in a future
 * release.</b></p>
 *
 * <p><b>Warning:</b> <i>Synchronous invocations may result in
 * deadlock.  In particular, performing n concurrent synchronous
 * invocations in an environment with n threads results in deadlock.
 * As a result, components that are not thread-safe must not perform
 * synchronous invocations.</i></p>
 * 
 * <p><i>Instead of using a synchronous invocation for a
 * request/response interaction consider using an {@link Operation}.
 * Operations do not capture the executing thread and are more
 * flexible since they support retries for timed out request/response
 * interactions. To use an operation, you need to write an additional
 * event handler, called the continuation, that handles the result of
 * the request/response interaction.</i></p>
 * 
 * @version  $Revision: 1.6 $
 * @author   Robert Grimm, Janet Davis
 */
public final class IOUtilities {

  // =======================================================================
  //                             Constructor
  // =======================================================================

  /** Hide constructor. */
  private IOUtilities() {
    // Nothing to do.
  }


  // =======================================================================
  //                         Public functionality
  // =======================================================================

  /**
   * Bind the specified resource. This method <i>synchronously</i>
   * binds the specified resource by issueing a binding request to the
   * specified request handler. The requested lease duration is the
   * {@link Constants#LEASE_DEFAULT_DURATION lease default duration}.
   *
   * @param   handler     The request handler.
   * @param   descriptor  The resource descriptor.
   * @return              The corresponding binding response.
   * @throws  IllegalArgumentException
   *                      Signals that <code>descriptor</code> is not
   *                      a valid tuple, that <code>handler</code>
   *                      does not accept binding requests, or that
   *                      <code>handler</code> responds to binding
   *                      requests with an unrecognized event.
   * @throws  TimeOutException
   *                      Signals that the synchronous invocation of
   *                      <code>handler</code> has timed out.
   * @throws  UnknownResourceException
   *                      Signals that the specified resource is not
   *                      recognized by the specified request handler.
   * @throws  LeaseDeniedException
   *                      Signals that the lease for the specified
   *                      resource has been denied.
   */
  public static BindingResponse bind(EventHandler handler, Tuple descriptor)
    throws UnknownResourceException, LeaseDeniedException {

    return bind(handler, descriptor, Constants.LEASE_DEFAULT_DURATION);
  }

  /**
   * Bind the specified resource. This method <i>synchronously</i>
   * binds the specified resource by issueing a binding request to the
   * specified request handler.
   *
   * @param   handler     The request handler.
   * @param   descriptor  The resource descriptor.
   * @param   duration    The initial lease duration.
   * @return              The corresponding binding response.
   * @throws  IllegalArgumentException
   *                      Signals that <code>descriptor</code> is not
   *                      a valid tuple, that <code>duration</code> is
   *                      an invalid lease duration, that
   *                      <code>handler</code> does not accept binding
   *                      requests, or that <code>handler</code>
   *                      responds to binding requests with an
   *                      unrecognized event.
   * @throws  TimeOutException
   *                      Signals that the synchronous invocation of
   *                      <code>handler</code> has timed out.
   * @throws  UnknownResourceException
   *                      Signals that the specified resource is not
   *                      recognized by the specified request handler.
   * @throws  LeaseDeniedException
   *                      Signals that the lease for the specified
   *                      resource has been denied.
   */
  public static BindingResponse bind(EventHandler handler, Tuple descriptor,
                                     long duration)
    throws UnknownResourceException, LeaseDeniedException {

    // Fire off the binding request.
    Event e = Synchronous.invoke(handler,
                                 new BindingRequest(null, null, descriptor,
                                                    duration));

    // Process result.
    if (e instanceof BindingResponse) {
      return (BindingResponse)e;

    } else if (e instanceof ExceptionalEvent) {
      Throwable x = ((ExceptionalEvent)e).x;

      if (x instanceof UnknownResourceException) {
        throw (UnknownResourceException)x;
      } else if (x instanceof LeaseDeniedException) {
        throw (LeaseDeniedException)x;
      } else if ((x instanceof InvalidTupleException) ||
                 (x instanceof UnknownEventException)) {
        throw new IllegalArgumentException(x.getMessage());
      } else {
        throw new IllegalArgumentException("Unexpected exceptional " + 
                                           "condition (" + x + ")");
      }
    }

    e.source.handle(new
      ExceptionalEvent(NullHandler.NULL,
                       e.closure,
                       new UnknownEventException(e.toString())));
    throw new IllegalArgumentException("Unknown event (" + e + ")");
  }

  /**
   * Put the specified tuple. This method <i>synchronously</i> writes
   * the specified tuple to the specified structured I/O resource.
   *
   * @param   handler  The event handler for the structured I/O
   *                   resource.
   * @param   t        The tuple to put.
   * @param   simple   <code>true</code> if a simple output request
   *                   should be used.
   * @throws  IllegalArgumentException
   *                   Signals that <code>t</code> is invalid,
   *                   that <code>handler</code> does not accept
   *                   (simple) output requests, or that
   *                   <code>handler</code> responds with an
   *                   unrecognized event.
   * @throws  TimeOutException
   *                   Signals that the synchronous invocation of
   *                   <code>handler</code> has timed out.
   * @throws  ResourceRevokedException
   *                   Signals that the resource managed by the
   *                   specified event handler has been revoked.
   */
  public static void put(EventHandler handler, Tuple t, boolean simple)
    throws ResourceRevokedException {

    // Write the tuple.
    Event e;
    if (simple) {
      e = Synchronous.invoke(handler, new SimpleOutputRequest(null, null, t));
    } else {
      e = Synchronous.invoke(handler, new OutputRequest(null, null, t, null));
    }

    // Process the result.
    if (e instanceof OutputResponse) {
      if (((OutputResponse)e).ident.equals(t.get("id"))) {
        return;
      } else {
        throw new Bug("ID mismatch for put operation");
      }

    } else if (e instanceof ExceptionalEvent) {
      Throwable x = ((ExceptionalEvent)e).x;

      if (x instanceof ResourceRevokedException) {
        throw (ResourceRevokedException)x;
      } else if ((x instanceof InvalidTupleException) ||
                 (x instanceof UnknownEventException)) {
        throw new IllegalArgumentException(x.getMessage());
      } else {
        throw new IllegalArgumentException("Unexpected exceptional " + 
                                           "condition (" + x + ")");
      }
    }

    e.source.handle(new
      ExceptionalEvent(NullHandler.NULL,
                       e.closure,
                       new UnknownEventException(e.toString())));
    throw new IllegalArgumentException("Unknown event (" + e + ")");
  }

  /**
   * Delete the tuple with the specified ID. This method
   * <i>synchronously</i> deletes the tuple with the specified ID from
   * the specified tuple store.
   *
   * @param   handler  The event handler for the tuple store.
   * @param   id       The ID of the tuple to delete.
   * @throws  IllegalArgumentException
   *                   Signals that <code>handler</code> does not
   *                   accept delete requests or that
   *                   <code>handler</code> responds with an
   *                   unrecognized event.
   * @throws  TimeOutException
   *                   Signals that the synchronous invocation of
   *                   <code>handler</code> has timed out.
   * @throws  ResourceRevokedException
   *                   Signals that the resource managed by the
   *                   specified event handler has been revoked.
   * @throws  NoSuchTupleException
   *                   Signals that no tuple with the specified
   *                   ID exists.
   */
  public static void delete(EventHandler handler, Guid id)
    throws ResourceRevokedException, NoSuchTupleException {

    Event e;
    e = Synchronous.invoke(handler, new DeleteRequest(null, null, id, null));

    // Process the result.
    if (e instanceof OutputResponse) {
      if (((OutputResponse)e).ident.equals(id)) {
        return;
      } else {
        throw new Bug("ID mismatch for delete operation");
      }

    } else if (e instanceof ExceptionalEvent) {
      Throwable x = ((ExceptionalEvent)e).x;

      if (x instanceof ResourceRevokedException) {
        throw (ResourceRevokedException)x;
      } else if (x instanceof NoSuchTupleException) {
        throw (NoSuchTupleException)x;
      } else {
        throw new IllegalArgumentException("Unexpected exceptional " +
                                           "condition (" + x + ")");
      }
    }

    e.source.handle(new
      ExceptionalEvent(NullHandler.NULL,
                       e.closure,
                       new UnknownEventException(e.toString())));
    throw new IllegalArgumentException("Unknown event (" + e + ")");
  }

  /**
   * Read a tuple matching the specified query. This method
   * <i>synchronously</i> reads a tuple matching the specified query
   * from the specified tuple store. The read request times out
   * immediately, thus only reading a tuple if that tuple already
   * exists in the tuple store.
   *
   * @param   handler  The event handler for the structured I/O
   *                   resource.
   * @param   q        The query to match.
   * @return           The corresponding tuple or <code>null</code>
   *                   if no tuple matches the query.
   * @throws  IllegalArgumentException
   *                   Signals that <code>q</code> is invalid,
   *                   that <code>handler</code> does not accept
   *                   input requests, or that <code>handler</code>
   *                   responds with an unrecognized event.
   * @throws  TimeOutException
   *                   Signals that the synchronous invocation of
   *                   <code>handler</code> has timed out.
   * @throws  ResourceRevokedException
   *                   Signals that the resource managed by the
   *                   specified event handler has been revoked.
   */
  public static Tuple read(EventHandler handler, Query q)
    throws ResourceRevokedException {

    return read(handler, q, 0, false);
  }

  /**
   * Read a tuple matching the specified query. This method
   * <i>synchronously</i> reads a tuple matching the specified query
   * from the specified structured I/O resource.
   *
   * <p>Note that the specified time-out is not the time-out of the
   * synchronous invocation, but rather the time-out of the structured
   * I/O read operation.</p>
   *
   * @param   handler  The event handler for the structured I/O
   *                   resource.
   * @param   q        The query to match.
   * @param   timeout  The time-out for the read operation.
   * @param   simple   <code>true</code> if a simple input request
   *                   should be used.
   * @return           The corresponding tuple or <code>null</code>
   *                   if no tuple matches the query.
   * @throws  IllegalArgumentException
   *                   Signals that <code>q</code> is invalid,
   *                   that <code>handler</code> does not accept
   *                   (simple) input requests, or that
   *                   <code>handler</code> responds with an
   *                   unrecognized event.
   * @throws  TimeOutException
   *                   Signals that the synchronous invocation of
   *                   <code>handler</code> has timed out.
   * @throws  ResourceRevokedException
   *                   Signals that the resource managed by the
   *                   specified event handler has been revoked.
   */
  public static Tuple read(EventHandler handler, Query q,
                           long timeout, boolean simple)
    throws ResourceRevokedException {

    // Read the tuple.
    Event e;
    if (simple) {
      e = Synchronous.invoke(handler, new
        SimpleInputRequest(null, null, SimpleInputRequest.READ,
                           q, timeout, false));
    } else {
      e = Synchronous.invoke(handler, new
        InputRequest(null, null, InputRequest.READ, q, timeout, false, null));
    }

    // Process the result.
    if (e instanceof InputResponse) {
      return ((InputResponse)e).tuple;

    } else if (e instanceof ExceptionalEvent) {
      Throwable x = ((ExceptionalEvent)e).x;

      if (x instanceof ResourceRevokedException) {
        throw (ResourceRevokedException)x;
      } else if ((x instanceof InvalidTupleException) ||
                 (x instanceof UnknownEventException)) {
        throw new IllegalArgumentException(x.getMessage());
      } else {
        throw new IllegalArgumentException("Unexpected exceptional " + 
                                           "condition (" + x + ")");
      }
    }

    e.source.handle(new
      ExceptionalEvent(NullHandler.NULL,
                       e.closure,
                       new UnknownEventException(e.toString())));
    throw new IllegalArgumentException("Unknown event (" + e + ")");
  }

  /**
   * Query for all tuples matching the specified query. This method
   * <i>synchronously</i> queries the specified tuple store.
   *
   * @param   handler   The event handler for the tuple store.
   * @param   q         The query to match.
   * @return            The corresponding result.
   * @throws  IllegalArgumentException
   *                    Signals that <code>q</code> is invalid,
   *                    that <code>handler</code> does not accept
   *                    input requests, or that <code>handler</code>
   *                    responds with an unrecognized event.
   * @throws  TimeOutException
   *                    Signals that the synchronous invocation of
   *                    <code>handler</code> has timed out.
   * @throws  ResourceRevokedException
   *                    Signals that the resource managed by the
   *                    specified event handler has been revoked.
   * @throws  LeaseDeniedException
   *                    Signals that the lease for the iterator over
   *                    the query's results has been denied.
   */
  public static QueryResponse query(EventHandler handler, Query q)
    throws ResourceRevokedException, LeaseDeniedException {

    return query(handler, q, Constants.LEASE_DEFAULT_DURATION, false);
  }

  /**
   * Query for all tuples matching the specified query. This method
   * <i>synchronously</i> queries the specified tuple store.
   *
   * @param   handler   The event handler for the tuple store.
   * @param   q         The query to match.
   * @param   duration  The requested duration for the query iterator.
   * @return            The corresponding result.
   * @throws  IllegalArgumentException
   *                    Signals that <code>q</code> is invalid,
   *                    that <code>duration</code> is invalid,
   *                    that <code>handler</code> does not accept
   *                    input requests, or that <code>handler</code>
   *                    responds with an unrecognized event.
   * @throws  TimeOutException
   *                    Signals that the synchronous invocation of
   *                    <code>handler</code> has timed out.
   * @throws  ResourceRevokedException
   *                    Signals that the resource managed by the
   *                    specified event handler has been revoked.
   * @throws  LeaseDeniedException
   *                    Signals that the lease for the iterator over
   *                    the query's results has been denied.
   */
  public static QueryResponse query(EventHandler handler, Query q,
                                    long duration)
    throws ResourceRevokedException, LeaseDeniedException {

    return query(handler, q, duration, false);
  }

  /**
   * Query for all tuples matching the specified query. This method
   * <i>synchronously</i> queries the specified tuple store.
   *
   * @param   handler   The event handler for the tuple store.
   * @param   q         The query to match.
   * @param   duration  The requested duration for the query iterator.
   * @param   idOnly    The flag for whether to only query for the
   *                    tuple IDs.
   * @return            The corresponding result.
   * @throws  IllegalArgumentException
   *                    Signals that <code>q</code> is invalid,
   *                    that <code>duration</code> is invalid,
   *                    that <code>handler</code> does not accept
   *                    input requests, or that <code>handler</code>
   *                    responds with an unrecognized event.
   * @throws  TimeOutException
   *                    Signals that the synchronous invocation of
   *                    <code>handler</code> has timed out.
   * @throws  ResourceRevokedException
   *                    Signals that the resource managed by the
   *                    specified event handler has been revoked.
   * @throws  LeaseDeniedException
   *                    Signals that the lease for the iterator over
   *                    the query's results has been denied.
   */
  public static QueryResponse query(EventHandler handler, Query q,
                                    long duration, boolean idOnly)
    throws ResourceRevokedException, LeaseDeniedException {

    // Perform the query.
    Event e = Synchronous.invoke(handler, new
      InputRequest(null, null, InputRequest.QUERY, q, duration, idOnly, null));

    // Process the result.
    if (e instanceof QueryResponse) {
      return (QueryResponse)e;

    } else if (e instanceof ExceptionalEvent) {
      Throwable x = ((ExceptionalEvent)e).x;

      if (x instanceof ResourceRevokedException) {
        throw (ResourceRevokedException)x;
      } else if (x instanceof LeaseDeniedException) {
        throw (LeaseDeniedException)x;
      } else if ((x instanceof InvalidTupleException) ||
                 (x instanceof UnknownEventException)) {
        throw new IllegalArgumentException(x.getMessage());
      } else {
        throw new IllegalArgumentException("Unexpected exceptional " + 
                                           "condition (" + x + ")");
      }
    }

    e.source.handle(new
      ExceptionalEvent(NullHandler.NULL,
                       e.closure,
                       new UnknownEventException(e.toString())));
    throw new IllegalArgumentException("Unknown event (" + e + ")");
  }

  /**
   * Get the next element from the specified iterator. This method
   * <i>synchronously</i> gets the next element from the specified
   * iterator.
   *
   * @param   iterator  The event handler for the iterator.
   * @return            The next element.
   * @throws  IllegalArgumentException
   *                      Signals that <code>handler</code> does not
   *                      accept iterator requests, or that
   *                      <code>iterator</code> responds to iterator
   *                      requests with an unrecognized event.
   * @throws  TimeOutException
   *                      Signals that the synchronous invocation of
   *                      <code>iterator</code> has timed out.
   * @throws  NoSuchElementException
   *                      Signals that the iterator has no elements
   *                      left.
   */
  public static IteratorElement next(EventHandler iterator) {
    // Fire off the iterator request.
    Event e = Synchronous.invoke(iterator, new IteratorRequest(null, null));

    // Process the result.
    if (e instanceof IteratorElement) {
      return (IteratorElement)e;

    } else if (e instanceof IteratorEmpty) {
      throw new NoSuchElementException("Iterator empty (" + iterator + ")");

    } else if (e instanceof ExceptionalEvent) {
      Throwable x = ((ExceptionalEvent)e).x;

      if (x instanceof NoSuchElementException) {
        throw (NoSuchElementException)x;
      } else if (x instanceof UnknownEventException) {
        throw new IllegalArgumentException(x.getMessage());
      } else {
        throw new IllegalArgumentException("Unexpected exceptional " +
                                           "condition (" + x + ")");
      }

    }

    e.source.handle(new
      ExceptionalEvent(NullHandler.NULL,
                       e.closure,
                       new UnknownEventException(e.toString())));
    throw new IllegalArgumentException("Unknown event (" + e + ")");
  }

}
