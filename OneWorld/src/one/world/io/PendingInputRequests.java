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

import one.world.core.Component;
import one.world.core.ComponentDescriptor;
import one.world.core.Environment;
import one.world.core.Event;
import one.world.core.EventHandler;
import one.world.core.ExceptionalEvent;
import one.world.core.ExportedDescriptor;
import one.world.core.ImportedDescriptor;
import one.world.core.Tuple;
import one.world.core.TupleException;
import one.world.core.UnknownEventException;

import one.world.binding.LeaseDeniedException;

import one.world.util.AbstractHandler;
import one.world.util.Log;
import one.world.util.TupleEvent;

import one.util.Guid;

import java.security.AccessController;
import java.security.PrivilegedAction;

import java.util.HashMap;
import java.util.Iterator;

/**
 * Manages input requests that are waiting for matching tuples.
 * This component should be used by structured I/O components when
 * they need to monitor arriving tuples for those that match existing
 * requests.
 *
 * <p><b>Imported and Exported Event Handlers</b></p>
 * 
 * <p>Exported event handlers:
 * <dl>
 *   <dt>leased request handler</dt>
 *   <dd>Handles {@link InputRequest}s and {@link SimpleInputRequest}s
 *       by obtaining a lease for the request and adding it to the
 *       collection of requests to be managed.  The request is
 *       automatically removed from the store when the lease is revoked.
 *       For <CODE>LISTEN</CODE> requests, a 
 *       {@link ListenResponse} containing the lease is
 *       sent to the request source.</dd>
 *   <dt>unleased request handler</dt>
 *   <dd>Handles <CODE>InputRequest</CODE>s and
 *       <CODE>SimpleInputRequest</CODE>s by simply adding the request to
 *       the collection of requests to be managed.  The request can by
 *       removed only by sending a {@link RemovePendingRequest} event.
 *       By linking synchronously to this handler, an I/O component gets
 *       very fine control over when requests are added and removed.</dd>
 *    <dt>tuple handler</dt>
 *    <dd>Handles {@link one.world.util.TupleEvent}s, representing new 
 *        tuples to be compared to the pending requests.  If a tuple
 *        matches a pending request, an appropriate input response is
 *        issued to the request source.</dd>
 * </dl>
 * </p>
 *
 * <p>This service is implemented as a component to allow synchronous or
 * asynchronous linkage to any of the three event handlers.  (For
 * instance, the {@link NetworkIO.Client} class requires asynchronous
 * linkage to the tuple handler to reduce the likelihood that it will
 * become a bottleneck for tuples arriving from the network.)</p>
 *
 * <p>{@link SynchronousPendingInputRequests} provides similar services,
 * but with a strictly synchronous interface.</p>
 *
 * @version  $Revision: 1.28 $
 * @author   Janet Davis
 */
public final class PendingInputRequests extends Component {

  ////////// Static members //////////

  /** The component descriptor. */
  private static final ComponentDescriptor SELF =
      new ComponentDescriptor("one.world.io.PendingInputRequests",
                              "A pending input request manager",
			      true);

  /** 
   * The exported event handler descriptor for the leased request event
   * handler.
   */
  private static final ExportedDescriptor LEASED_REQUEST_HANDLER =
      new ExportedDescriptor("leased request handler",
                             "Event handler for leased input requests",
			     new Class[] { InputRequest.class, 
			                   SimpleInputRequest.class },
                             new Class[] { TupleException.class, 
			                   UnknownEventException.class,
					   LeaseDeniedException.class },
                             false);
    
  /** 
   * The exported event handler descriptor for the unleased request event
   * handler.
   */
  private static final ExportedDescriptor UNLEASED_REQUEST_HANDLER =
      new ExportedDescriptor("unleased request handler",
                             "Event handler for unleased input requests",
			     new Class[] { InputRequest.class, 
			                   SimpleInputRequest.class,
					   RemovePendingRequest.class },
                             new Class[] { TupleException.class, 
			                   UnknownEventException.class },
                             false);

  /** 
   * The exported event handler descriptor for the tuple event handler.
   */
  private static final ExportedDescriptor TUPLE_HANDLER =
      new ExportedDescriptor("tuple handler",
                             "Event handler for tuple events",
			     new Class[] { TupleEvent.class },
                             new Class[] { TupleException.class, 
			                   UnknownEventException.class },
                             false);

  /** The system log. */
  private static final Log log;

  // Initialize the system log variable.
  static {
    log = (Log)AccessController.doPrivileged(new PrivilegedAction() {
        public Object run() {
          return Log.getSystemLog();
        }
      });
  }

  ////////// Wrapper //////////

  /**
   * <p>A component wrapper for the PendingInputRequests manager. 
   * This allows objects that do not implement the {@link Component}
   * interface to easily use a <code>PendingInputRequests</code> component.
   * Access is through the three public event handlers.</p>
   */
  public static class Wrapper extends Component {
  
    /** The component descriptor. */
    private static final ComponentDescriptor SELF =
        new ComponentDescriptor(
            "PendingInputRequestsWrapper",
            "A wrapper for the pending input request manager",
            true);
  
    /** The descriptor for the imported leased input request handler. */
    private static final ImportedDescriptor LEASED_REQUEST_HANDLER =
        new ImportedDescriptor("leased request handler",
                               "Handles leased pending input requests",
                               new Class[] { SimpleInputRequest.class,
  			                   InputRequest.class },
                               new Class[] {},
                               false,
                               false);
  
    /** The descriptor for the imported input request handler. */
    private static final ImportedDescriptor UNLEASED_REQUEST_HANDLER =
        new ImportedDescriptor("unleased request handler",
                               "Handles unleased pending input requests",
                               new Class[] { SimpleInputRequest.class,
  			                   InputRequest.class },
                               new Class[] {},
                               false,
                               false);
  
    /** 
     * The descriptor for the imported tuple handler.  It must be linked
     * through a concurrency domain.
     */
    private static final ImportedDescriptor TUPLE_HANDLER =
        new ImportedDescriptor("tuple handler",
                               "Handles tuples arriving on the network",
                               new Class[] { TupleEvent.class },
                               new Class[] {},
                               true,
                               false);
  
    /** The pending input request manager. */
    private PendingInputRequests pending;
  
    /** 
     * Accepts input requests that should be leased while waiting for a
     * match (imported).
     */
    public EventHandler leasedRequestHandler;
  
    /** 
     * Accepts input requests that are waiting for a match (imported).
     * Because the input requests are not leased, they must be explicitly
     * canceled with a {@link DeleteRequest} event.
     */
    public EventHandler unleasedRequestHandler;
  
    /** 
     * Accepts tuples for processing (imported).  This handler is linked
     * asynchronously.
     */
    public EventHandler tupleHandler;
  
    /** 
     * Creates a new pending input requests wrapper.
     *
     * @param env  The environment in which to create the component.
     * @param pending  The pending input request manager to wrap.
     */
    public Wrapper(Environment env,
                   PendingInputRequests pending) {
      super(env);
  
      // Declare the imported event handlers.
      leasedRequestHandler = declareImported(LEASED_REQUEST_HANDLER);
      unleasedRequestHandler = declareImported(UNLEASED_REQUEST_HANDLER);
      tupleHandler = declareImported(TUPLE_HANDLER);
  
      // Create the pending input request manager and link against it.
      link("leased request handler", "leased request handler", pending);
      link("unleased request handler", "unleased request handler", pending);
      link("tuple handler", "tuple handler", pending);
    }
  
    /** Gets the component descriptor. */
    public ComponentDescriptor getDescriptor() {
      return (ComponentDescriptor)SELF.clone();
    }
  }

  ////////// LeasedRequestHandler //////////

  /** 
   * The handler for unleased requests.  Adds the request to the pending
   * request repository, issuing a lease for the request.  For a listen
   * request, the lease is returned to the requester.  In all other cases,
   * the lease is used simply to force expiration of the request.
   */
  private final class LeasedRequestHandler extends AbstractHandler {
    
    /**
     * Handles an InputRequest or SimpleInputRequest.
     */
    public boolean handle1(Event event) {

      if (event instanceof InputRequest) {
        InputRequest request = (InputRequest)event;
        try {
          PendingRequest pr = 
	      new PendingRequest(request, this, collection);
          collection.add(pr);
	  pr.requestLease(leaseHandler, request.duration);
        } catch (UnsupportedOperationException x) {
          respond(event, x); 
        }
	return true;

      } else if (event instanceof SimpleInputRequest) {
        SimpleInputRequest request = (SimpleInputRequest)event;
        PendingRequest pr = 
	    new PendingRequest(request, this, collection);
        collection.add(pr);
	pr.requestLease(leaseHandler, request.duration);
	return true;
      }

      return false;
    }
  }

  ////////// UnleasedRequestHandler //////////

  /** 
   * The handler for unleased requests.  Adds the request to the pending
   * request repository without issuing a lease.  The request will remain
   * active until it is appropriately dealt with or explicitly removed
   * using the {@link RemovePendingRequest} event.
   */
  private final class UnleasedRequestHandler extends AbstractHandler {

    /**
     * Handles an InputRequest, a SimpleInputRequest, or a
     * RemovePendingRequest event.
     */
    public boolean handle1(Event event) {

      if (event instanceof InputRequest) {
        InputRequest request = (InputRequest)event;
        try {
          collection.add(new PendingRequest(request, this, collection));
        } catch (UnsupportedOperationException x) {
	  respond(event, x);
        }
	return true;

      } else if (event instanceof SimpleInputRequest) {
        SimpleInputRequest request = (SimpleInputRequest)event;
        collection.add(new PendingRequest(request, this, collection));
	return true;

      } else if (event instanceof RemovePendingRequest) {
        RemovePendingRequest request = (RemovePendingRequest)event;
        collection.remove(request.ident);
	return true;
      }

      return false;
    }
  }

  ////////// TupleHandler //////////

  /** The handler for tuple arrival events. */
  private final class TupleHandler extends AbstractHandler {

    /** 
     * Handles a TupleEvent. 
     */
    public boolean handle1(Event event) {
      if (event instanceof TupleEvent) {
        collection.filter(((TupleEvent)event).tuple);
	return true;
      }
      return false;
    }
  }

  ////////// Collection //////////

  /** The collection of pending input requests. */
  private final class Collection implements PendingRequest.Collection {
    
    /** The underlying collection. */
    HashMap map = new HashMap();

    /** The lock object. */
    Object lock = new Object();

    /** 
     * Adds a pending request to the collection. 
     * 
     * @param request The request to add.
     * @return        The added request.
     */
    public PendingRequest add(PendingRequest request) {
      synchronized (lock) {
        map.put(request.id, request);
      }
      return request;
    }

    /** 
     * Removes a pending request from the collection.
     *
     * @param id     The ID of the request to remove.
     * @return       The removed request or <code>null</code> if 
     *               the request was not in the collection.
     */
    public PendingRequest remove(final Guid id) {
      Object result;
      synchronized (lock) {
        result = map.remove(id);
      }
      return (PendingRequest)result;
    }
      
    /**
     * Processes incoming tuples by attempting to match them to pending
     * requests.  READ requests are removed after matching a tuple.
     *
     * <p>To increase concurrency, we do not lock while iterating over the
     * collection of requests.  (This is accomplished by making a copy of
     * the key set, and retrieving the value for each key under a lock.) 
     * This allows multiple tuples to be processed concurrently,
     * and also allows requests to be added and removed during
     * processing.</p>
     *
     * <p>Results returned are those for request that are present at the
     * start of processing and are not removed during processing.</p>
     */
    private void filter(Tuple tuple) {

      Iterator iterator = null;
      Guid requestId = null;
      PendingRequest pRequest = null;
  
      // Get an iterator over the keys.  
      synchronized (lock) {
        iterator = (((HashMap)map.clone()).keySet()).iterator();
      }

      // Iterate over the keys.
      while (iterator.hasNext()) {
        requestId = (Guid)iterator.next();

	synchronized (lock) {
          pRequest = (PendingRequest)map.get(requestId);
	}
  
        if (pRequest == null) {
          // Do nothing; the request has been removed.

        } else if (pRequest.filter.check(tuple)) {
  	  if (pRequest.type == InputRequest.READ) {
  	    remove(requestId);
    	  }
          pRequest.sendResult(tuple);
        }
      }
    }
  }

  ////////// PendingInputRequests non-static members //////////

  /** The pending request collection. */
  Collection collection;

  /** The event handler for leased requests (exported). */
  private final EventHandler leasedRequestHandler;

  /** The event handler for unleased requests (exported). */
  private final EventHandler unleasedRequestHandler;

  /** The event handler for tuples (exported). */
  private final EventHandler tupleHandler;

  /** 
   * The event handler for lease acquisistion requests (passed as a
   * parameter to the constructor).
   */
  private final EventHandler leaseHandler;

  /** The component wrapper. */
  private Wrapper wrapper;

  /**
   * Create a new pending input request manager. 
   *
   * @param env   The environment for this component.
   * @param leaseHandler  The lease acquisistion request handler.
   *
   * @throws NullPointerException  Signals that <CODE>env</CODE> is null.
   * @throws IllegalArgumentException Signals an attempt to add a
   *         component that is not thread-safe to a multi-threaded
   *         environment.
   * @throws IllegalStateException Signals that the specified environment
   *         is currently changing status or has been terminated.
   */
  public PendingInputRequests(Environment env, EventHandler leaseHandler)
         throws IllegalArgumentException, IllegalStateException {

    super(env);

    this.leaseHandler = leaseHandler;

    leasedRequestHandler = new LeasedRequestHandler();
    declareExported(LEASED_REQUEST_HANDLER, leasedRequestHandler);

    unleasedRequestHandler = new UnleasedRequestHandler();
    declareExported(UNLEASED_REQUEST_HANDLER, unleasedRequestHandler);

    tupleHandler = new TupleHandler();
    declareExported(TUPLE_HANDLER, tupleHandler);

    collection = new Collection();
    wrapper = new Wrapper(env, this);
  }

  /** Gets the component descriptor. */
  public ComponentDescriptor getDescriptor() {
    return (ComponentDescriptor)SELF.clone();
  }

  /** Gets the component wrapper. */
  public Wrapper getWrapper() {
    return wrapper;
  }
}
