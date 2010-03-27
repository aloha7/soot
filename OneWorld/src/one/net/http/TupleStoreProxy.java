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
package one.net.http;

import one.world.core.Event;
import one.world.core.Tuple;
import one.world.core.EventHandler;
import one.world.core.ExceptionalEvent;
import one.world.util.AbstractHandler;
import one.world.util.Operation;
import one.world.util.Timer;
import one.world.util.SystemUtilities;
import one.world.io.Query;
import one.world.io.SioResource;
import one.world.io.InputResponse;
import one.world.io.ListenResponse;
import one.world.io.SimpleInputRequest;
import one.world.binding.BindingRequest;
import one.world.binding.BindingResponse;
import one.world.binding.UnknownResourceException;
import one.world.binding.LeaseMaintainer;
import one.world.binding.Duration;
import one.world.data.Chunk;
import one.world.data.BinaryData;

import one.util.Guid;
import one.util.Bug;
import java.util.Hashtable;

/**
 * Simplifies the management of the one.world TupleStore
 * from the HTTP server code.
 *
 * @author  Daniel Cheah
 * @version $Revision: 1.2 $
 */
public final class TupleStoreProxy {
  /** Constants */
  private static final int DEFAULT_BINDING_CACHE_SIZE = 8;
  private static final String NAME_FIELD = "name";
  private static final String GUID_FIELD = "id";
  
  /** For interacting with one.world kernel */
  private transient Operation    kernel;
  private transient Timer        timer;

  /** The various caches */
  private transient BindingCache bindings;

  /** Continuation event handler for binding responses */
  private final EventHandler bindCont;
  private final EventHandler inputCont;
  private final Hashtable    reqResTable;

  /**
   * Constructor. 
   *
   * @param timer      Timer object
   * @param tuplestore Event handler that handles binding requests
   */
  public TupleStoreProxy(Timer timer, EventHandler tuplestore) {
    this.bindCont    = new BindContinuation();
    this.inputCont   = new InputContinuation();
    this.reqResTable = new Hashtable();

    initTransient(timer, tuplestore);
  }

  /**
   * Initialize the transient fields.
   */
  private void initTransient(Timer timer, EventHandler tuplestore) {
    this.timer    = timer;
    this.bindings = new BindingCache(DEFAULT_BINDING_CACHE_SIZE);
    this.kernel   = new Operation(timer, tuplestore, bindCont);
  }

  /**
   * Use this at shutdown to clean up the proxy class.
   */
  public synchronized void destroy() {
    // Cancel Leases
    bindings.cancelLeases();
  }

  /**
   * Higher layers request input from the tuple store.
   * Uses the proxyHandler as a callback functor to give better suppport
   * for one.world's asynchronous model.
   *
   * @param proxyHandler The callback functor.
   * @param request      The http request object.
   * @param envName      The environment to search for the tuple.
   * @param tupleName    The name of the tuple.
   */
  public void requestInput(ProxyHandler proxyHandler,
                           HttpRequest request,
                           String envName, 
                           String tupleName) {

    BindingCache.Entry entry = bindings.getEntry(envName);

    // If binding for the environment is not found in the cache,
    // we want to create the binding.

    if (null != entry) {
      // We need to add the entry into the closure because
      // we have to release the entry afterward.

      entry.handle(createInputRequest(new Guid(), 
                                      createTupleQuery(tupleName),
                                      new Closure(proxyHandler,
                                                  request,
                                                  envName,
                                                  tupleName,
                                                  null, entry)));

    } else {
      kernel.handle(createBindingRequest(new Guid(), 
                                         new Closure(proxyHandler,
                                                     request,
                                                     envName,
                                                     tupleName,
                                                     null, null)));
    }
  }

  /**
   * Higher layers request chunks from the tuple store.
   * Uses the proxyHandler as a callback functor to give better suppport
   * for one.world's asynchronous model.
   *
   * @param proxyHandler The callback functor.
   * @param request      The http request object.
   * @param envName      The environment to search for the tuple.
   * @param tupleName    The name of the tuple.
   */ 
  public void requestChunk(ProxyHandler proxyHandler,
                           ChunkRequest request,
                           String envName) {

    BindingCache.Entry entry = bindings.getEntry(envName);

    // If binding for the environment is not found in the cache,
    // we want to create the binding.

    if (null != entry) {
      // We need to add the entry into the closure because
      // we have to release the entry afterward.

      entry.handle(createInputRequest(new Guid(), 
                                      createChunkQuery(request.chunkGuid),
                                      new Closure(proxyHandler,
                                                  request,
                                                  envName,
                                                  null,
                                                  request.chunkGuid,
                                                  entry)));

    } else {
      kernel.handle(createBindingRequest(new Guid(), 
                                         new Closure(proxyHandler,
                                                     request,
                                                     envName,
                                                     null,
                                                     request.chunkGuid,
                                                     null)));
    }
  }

  /** 
   * Creates a input request.
   */
  private SimpleInputRequest createInputRequest(Guid g, Query q, Closure c) {
    // Store this into the response table
    reqResTable.put(g, c);

    // The response for this request will dispatch
    // to the input continuation functor class.
    return new SimpleInputRequest(inputCont, g, 
                                  SimpleInputRequest.READ, q,
                                  Duration.FOREVER, false);
  }

  /**
   * Creates a binding request.
   */
  private BindingRequest createBindingRequest(Guid g, Closure c) {
    // Store this into the response table
    reqResTable.put(g, c);

    // The response for this request will dispatch
    // to the bind continuation functor class.
    return new BindingRequest(bindCont, g, 
                              new SioResource(c.envName), 
                              Duration.FOREVER);
  }

  /**
   * Creates the necessary query needed to get the
   * right tuple.
   */
  private Query createTupleQuery(String tupleName) {
    return new Query(new Query(NAME_FIELD,
                               Query.COMPARE_EQUAL,
                               tupleName),
                     Query.BINARY_AND,
                     new Query(new Query("", 
                                         Query.COMPARE_HAS_TYPE, 
                                         BinaryData.class),
                               Query.BINARY_OR,
                               new Query(new Query("", 
                                                   Query.COMPARE_HAS_TYPE, 
                                                   Chunk.class),
                                         Query.BINARY_AND,
                                         new Query("previous", 
                                                   Query.COMPARE_EQUAL, 
                                                   null))));
  }

  /**
   * Creates the necessary query needed to get the
   * right chunk.
   */
  private Query createChunkQuery(Guid g) {
    return new Query(GUID_FIELD, Query.COMPARE_EQUAL, g);
  }

  /***********************************************************
   * BindContinuation
   **********************************************************/
  private final class BindContinuation extends AbstractHandler {
    protected boolean handle1(Event e) {
      if (e instanceof BindingResponse) {
        handleBindingResponse((BindingResponse)e);
        return true;

      } else if (e instanceof ExceptionalEvent) {
        handleExceptionalEvent((ExceptionalEvent)e);
        return true;
        
      } else {
        return false;
      
      }
    }

    void handleExceptionalEvent(ExceptionalEvent ee) {
      // fixmE: Should we expose exceptional events to the 
      // higher layers for binds?
      //SystemUtilities.debug(ee.x);
    }

    void handleBindingResponse(BindingResponse res) {
      Closure c= (Closure)reqResTable.remove(res.closure);

      // Setup the lease maintainer to maintain the lease on the 
      // new binding for us.
      LeaseMaintainer lease = new LeaseMaintainer(res.lease, 
                                                  res.duration, 
                                                  LeaseMaintainer.CANCEL, 
                                                  null, 
                                                  timer);

      // Create a new binding cache entry.
      BindingCache.Entry entry = new BindingCache.Entry(c.envName, 
                                                        lease, 
                                                        res.resource);

      // Add the new lease to the cache. 
      // NOTE: This call evicts an entry from the cache and returns it.
      // We don't need to cancel the lease here, because presumably when
      // we get input, we will check and cancel it there.
      bindings.addEntry(entry);

      // The closure can now access the binding information
      c.binding = entry;

      if (null != c.tupleName) {
        // Get the tuple we are interested in.
        entry.handle(createInputRequest((Guid)res.closure, 
                                        createTupleQuery(c.tupleName),
                                        c));

      } else if (null != c.chunkGuid) {
        // Get the tuple we are interested in.
        entry.handle(createInputRequest((Guid)res.closure, 
                                        new Query(GUID_FIELD,
                                                  Query.COMPARE_EQUAL,
                                                  c.chunkGuid),
                                        c));
      } else {
        // no good
        throw new Bug();
      }
    }
  }

  /***********************************************************
   * InputContinuation
   **********************************************************/
  private final class InputContinuation extends AbstractHandler {
    protected boolean handle1(Event e) {
      // Get back the closure
      Closure c = (Closure)reqResTable.remove(e.closure);

      if (e instanceof InputResponse) {
        // SystemUtilities.debug("Got back InputResponse");
        // One less client using this binding cache entry
        c.binding.release();
      }

      // Set the event closure to the correct closure
      e.closure = c;

      // activate the call back handler
      return c.handler.proxyHandle(e);
    }
  }

  /**
   * Closure for clients of this class. On the
   * callback through the proxy handler, the
   * closure is set to this class.
   *
   * @author Daniel Cheah
   */
  public final static class Closure {
    public final ProxyHandler       handler;
    public final Event              request;
    public final String             envName;

    public final String             tupleName;
    public final Guid               chunkGuid;

    public BindingCache.Entry binding;

    public Closure(ProxyHandler handler, 
                   Event request,
                   String envName, 
                   String tupleName,
                   Guid chunkGuid,
                   BindingCache.Entry binding) {

      this.handler   = handler;
      this.request   = request;
      this.envName   = envName;
      this.tupleName = tupleName;
      this.chunkGuid = chunkGuid;
      this.binding   = binding;
    }
  }
} 
