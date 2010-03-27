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
 * THIS SOFTWARE IS PROVIdED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
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

package one.world.rep;

import one.world.core.DynamicTuple;
import one.world.core.Event;
import one.world.core.EventHandler;
import one.world.core.ExceptionalEvent;
import one.world.core.Tuple;

import one.world.binding.BindingRequest;
import one.world.binding.BindingResponse;
import one.world.binding.LeaseEvent;
import one.world.binding.LeaseDeniedException;
import one.world.binding.UnknownResourceException;

import one.world.data.Name;

import one.world.util.AbstractHandler;
import one.world.util.Log;
import one.world.util.SystemUtilities;

import one.world.Constants;

import one.util.Guid;
import one.util.Bug;

import java.util.HashMap;
import java.util.Map;

/**
 * An Resolver maintains mappings from names to event handlers and from
 * Ids to event handlers for event handlers exported from a particular
 * {@link RemoteManager}.
 *
 * @version  $Revision: 1.8 $
 * @author   Janet Davis
 */
final class Resolver {

  /**
   * The closure class for lease events.
   */
  final class Closure extends Event {

    /** The id for the exported event handler. */
    public Guid exportId;

    /** The name of the exported event handler. */
    public String name;

    /** The exported event handler. */
    public EventHandler handler;

    /** The request descriptor. */
    public Tuple descriptor;

    /** The remote reference. */
    public RemoteReference ref;

    /** Creates a new, empty closure. */
    public Closure() {
    }

    /** Creates a new closure. */
    public Closure(EventHandler source, Object closure,
                   Guid exportId, String name, EventHandler handler,
		   Tuple descriptor) {
      super(source, closure);
      this.exportId = exportId;
      this.name = name;
      this.handler = handler;
      this.descriptor = descriptor;
    }
  }

  /**
   * Handles {@link one.world.binding.LeaseEvent#CANCELED} and 
   * {@link one.world.binding.LeaseEvent#ACQUIRED} events.
   */
  private final class Handler extends AbstractHandler {

    /** Handles events. */
    protected boolean handle1(Event event) {
      if (event instanceof LeaseEvent) {
        LeaseEvent le = (LeaseEvent)event;
        switch(le.type) {
        case LeaseEvent.ACQUIRED:
          acquired(le);
          return true;
        case LeaseEvent.CANCELED:
          canceled(le);
          return true;
        }
      } else if (event instanceof ExceptionalEvent) {
        Throwable x = ((ExceptionalEvent)event).x;

        if (x instanceof LeaseDeniedException) {
	  // Failed to acquire a lease.  Revoke the binding.
          Closure request = (Closure)event.closure;
	  remove(request.exportId);
    	  respond(request, x);
	  return true;
        }
      }
      return false;
    }
  
    /**
     * Handles a lease acquired event by 
     * sending a {@link BindingResponse} to the request source.
     */
    private void acquired(LeaseEvent le) {
      Closure c = (Closure)le.closure;

      if (Constants.DEBUG_REP) {
        manager.log.log(manager, "Exporting " + c.descriptor);
      }

      respond(c, new BindingResponse(handler, null, c.descriptor, c.ref,
                                     le.handler, le.duration));
    }
  
    /**
     * Handles a lease canceled event by removing the event handler from the
     * maps.
     */
    private void canceled(LeaseEvent le) {
      Closure c = (Closure)le.closure;

      if (Constants.DEBUG_REP) {
        manager.log.log(manager, "Revoking " + c.descriptor);
      }

      remove(c.exportId);
    }
  }

  /*
   * A critical invariant in this implementation is that the nameToId,
   * idToName, and idToHandler maps be consistent.  Synchronization and
   * existence checking are used to ensure that this invariant holds.
   */

  /** The mapping from names to {@link one.util.Guid}s. */
  private final Map nameToId;

  /** The reverse mapping from  {@link one.util.Guid}s to names. */
  private final Map idToName;

  /** The mapping from {@link one.util.Guid}s to event handlers. */
  private final Map idToHandler;

  /** The internal event handler. */
  private final EventHandler handler;

  /** The map access lock. */
  private final Object lock;

  /** The {@link RemoteManager} this resolver belongs to. */
  private final RemoteManager manager;

  /** 
   * Constructs a new Resolver. 
   * 
   * @param manager  The remote manager for this resolver.
   */
  public Resolver(RemoteManager manager) {
    this.manager = manager;
    nameToId = new HashMap();
    idToName = new HashMap();
    idToHandler = new HashMap();
    handler = new Handler();
    lock = new Object();
  }

  /** 
   * Exports an event handler and responds appropriately to the original
   * requester.
   *
   * @param request    The binding request to respond to.
   * @param name       The name to export as (<code>null</code> to export
   *                   anonymously).
   * @param resource   The event handler to export. 
   */
  public void export(BindingRequest request, 
                     final String name, final EventHandler resource) {
    /*
     * Note: We must acquire a lease <emph>before</emph> making any
     * modifications to the internal mappings.  Otherwise, EHs are
     * exported without necessarily having a valid lease.
     */

    Guid exportId = new Guid();

    try {
      synchronized (lock) {
        if (name != null && nameToId.get(name) != null) {
          throw new AlreadyBoundException();
        }
        if (idToName.get(exportId) != null 
  	    || idToHandler.get(exportId) != null) {
          throw new Bug("Duplicated Guid");
  	}
  
    	if (name != null) {
  	  nameToId.put(name, exportId);
    	}
    	idToName.put(exportId, name);
  	idToHandler.put(exportId, resource);
      }
    } catch (AlreadyBoundException x) {
      request.source.handle(new ExceptionalEvent(handler, request.closure, 
             new AlreadyBoundException("Name " + name + " already in use")));
      return;
    }
  
    Closure closure =
        new Closure(request.source, request.closure, exportId,
	            name, resource, request.descriptor);

    closure.ref =
          new RemoteReference(manager.getHost(), manager.getPort(), 
  	                      exportId);
	 
    manager.getLeaseManager().handle(
        new LeaseEvent(handler, closure, LeaseEvent.ACQUIRE,
	               handler, closure, request.duration));
  }

  /**
   * Removes an entry, under synchronization. 
   *
   * @param  id  The id of the entry to remove.
   */
  void remove(Guid id) {
    synchronized (lock) {
      String name = (String)idToName.remove(id);
      if (name != null) {
        nameToId.remove(name);
      }
      idToHandler.remove(id);
    }
  }

  /** 
   * Resolves a localized resource to a remote reference.
   *
   * @param resource  The resource to resolve.
   * 
   * @throws UnknownResourceException
   *         Signals that no exported handler matches the given resource.
   */
  public RemoteReference resolve(final LocalizedResource resource) 
          throws UnknownResourceException {
    if (resource instanceof NamedResource) {
      return resolve((NamedResource)resource);
    } else if (resource instanceof RemoteReference) {
      return resolve((RemoteReference)resource);
    } else {
      throw new Bug("Cannot resolve non-localized resource");
    }
  }

  /**
   * Resolves a named handler resource to a remote reference.
   *
   * @param resource   The resource to resolve.
   * 
   * @throws UnknownResourceException
   *         Signals that no exported handler matches the given resource.
   */
  private RemoteReference resolve(final NamedResource resource) 
         throws UnknownResourceException {
    Guid id;

    synchronized (lock) {
      id = (Guid)nameToId.get(resource.name);
    }

    if (id == null) {
      throw new UnknownResourceException(
                    "No handler named " + resource.name);
    }

    return new RemoteReference(manager.getHost(), 
                               manager.getPort(),
                               id);
  }

  /**
   * Resolves a remote reference to a remote reference.
   *
   * @param resource  The resource to resolve.
   *
   * @throws UnknownResourceException
   *         Signals that no exported handler matches the given resource.
   */
  private RemoteReference resolve(final RemoteReference resource) 
         throws UnknownResourceException {
    Object obj;

    synchronized (lock) {
      obj = idToHandler.get(resource.id);
    }

    if (obj == null) { 
      throw new UnknownResourceException(
                    "No handler for id " + resource.id);
    }

    return resource;
  }

  /** 
   * Resolves a localized resource to a local event handler.
   *
   * @param resource  The resource to resolve.
   * 
   * @throws UnknownResourceException
   *         Signals that no exported handler matches the given resource.
   */
  public EventHandler realResolve(final LocalizedResource resource) 
          throws UnknownResourceException {
    if (resource instanceof NamedResource) {
      return realResolve((NamedResource)resource);
    } else if (resource instanceof RemoteReference) {
      return realResolve((RemoteReference)resource);
    } else {
      throw new Bug("Cannot resolve non-localized resource");
    }
  }

  /**
   * Resolves a named handler resource to a real event handler.
   *
   * @param resource   The resource to resolve.
   *
   * @throws UnknownResourceException
   *         Signals that no exported handler matches the given resource.
   */
  private EventHandler realResolve(final NamedResource resource)
         throws UnknownResourceException {

    EventHandler handler;
    Guid id;

    synchronized (lock) {

      id = (Guid)nameToId.get(resource.name);

      if (id == null) {
        throw new UnknownResourceException(
                      "No handler named " + resource.name);
      }

      handler = (EventHandler)idToHandler.get(id);
    }

    if (handler == null) {
      throw new UnknownResourceException("No handler for id " + id); 
    }

    return handler;
  }

  /**
   * Resolves a remote reference to a real event handler.
   *
   * @param resource   The resource to resolve.
   *
   * @throws UnknownResourceException
   *         Signals that no exported handler matches the given resource.
   */
  private EventHandler realResolve(final RemoteReference resource)
         throws UnknownResourceException {
    EventHandler handler;

    synchronized (lock) {
      handler = (EventHandler)idToHandler.get(resource.id);
    }

    if (handler == null) {
      throw new UnknownResourceException(
                    "No handler for id " + resource.id); 
    }

    return handler;
  }
}
