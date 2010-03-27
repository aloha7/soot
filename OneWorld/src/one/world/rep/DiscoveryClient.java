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

package one.world.rep;


import one.util.Guid;
import one.util.Bug;


import one.world.Constants;

import one.world.binding.BindingRequest;
import one.world.binding.BindingResponse;
import one.world.binding.Duration;
import one.world.binding.LeaseEvent;
import one.world.binding.LeaseManager;
import one.world.binding.LeaseRevokedException;
import one.world.binding.LeaseDeniedException;
import one.world.binding.ResourceRevokedException;
import one.world.binding.UnknownResourceException;
import one.world.binding.LeaseMaintainer;

import one.world.core.Component;
import one.world.core.ComponentDescriptor;
import one.world.core.DynamicTuple;
import one.world.core.Environment;
import one.world.core.Event;
import one.world.core.EventHandler;
import one.world.core.ExceptionalEvent;
import one.world.core.ExportedDescriptor;
import one.world.core.ImportedDescriptor;
import one.world.core.NotActiveException;
import one.world.core.NoBufferSpaceException;
import one.world.core.Tuple;

import one.world.env.EnvironmentEvent;

import one.world.io.DatagramIO;
import one.world.io.InputResponse;
import one.world.io.ListenResponse;
import one.world.io.OutputResponse;
import one.world.io.Query;
import one.world.io.SimpleInputRequest;
import one.world.io.SimpleOutputRequest;
import one.world.io.SioResource;
import one.world.io.TupleFilter;

import one.world.rep.DiscoveredResource;
import one.world.rep.RemoteDescriptor;
import one.world.rep.RemoteReference;
import one.world.rep.RemoteEvent;
import one.world.rep.ResolutionRequest;
import one.world.rep.ResolutionResponse;

import one.world.util.NullHandler;
import one.world.util.AbstractHandler;
import one.world.util.Log;
import one.world.util.Synchronous;
import one.world.util.SystemUtilities;
import one.world.util.Timer;
import one.world.util.Operation;
import one.world.util.TimeOutException;
import one.world.util.TypedEvent;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.NotSerializableException;

import java.security.AccessController;
import java.security.PrivilegedAction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.LinkedList;


/**
 * A client for service discovery.  This component listens for server
 * advertisments, registers services with servers, and performs
 * queries on behalf of the application.  Application code should not
 * directly interact with this component's event handlers.  Instead,
 * applications can send {@link one.world.rep.ResolutionRequest}s
 * (service lookup), {@link one.world.binding.BindingRequest}s
 * (service registration), or {@link one.world.rep.RemoteEvent}s
 * (late-bound discovery) to the environment <i>request</i> handler,
 * and if appropriate the events will be sent to this component.  The
 * client component is always active (even when the {@link
 * one.world.rep.DiscoveryServer} component is active).
 *
 * DiscoveryClient submits bindings in all servers it can find, but resolves
 * bother early and late binding requests using only a single server.
 *
 *
 *
 * FIXME: list of handlers
 * <p><b>Imported and Exported Event Handlers</b></p>
 *
 * <p>Exported event handlers:<dl>
 *    <dt>main</dt>
 *    <dd>Handles environment events.
 *        </dd>
 *    <dt>input</dt>
 *    <dd>Handles discovery requests.
 *        </dd>
 * </dl></p>
 *
 * <p>Imported event handlers:<dl>
 *    <dt>request</dt>
 *    <dd>The imported environment request handler.
 *        </dd>
 * </dl></p>
 *
 * @see      DiscoveryServer
 * @author   Adam MacBeth 
 * @author   Eric Lemar
 */
public final class DiscoveryClient extends Component {

  // =======================================================================
  //                           Static fields
  // =======================================================================
  
  /** The inactive state: the default state. */
  private static final int INACTIVE = 0;

  /** The activating state: The component is acquiring resources. */
  private static final int ACTIVATING = 1;

  /** The active state: The component and is running. */
  private static final int ACTIVE = 2;

  /** The closing state: The component is shutting down. */
  private static final int CLOSING = 3;

  /** The system log. */
  private static final Log log;

  // Initialize the system log variable.
  static {
    log = (Log)AccessController.doPrivileged(
      new PrivilegedAction() {
        public Object run() {
          return Log.getSystemLog();
        }
      });
  }


  /** A template for the .toArray() function */
  private static final BoundServer templateBoundServerArray[] = 
                               new BoundServer[0];    

  /** A template for the .toArray() function */
  private static final ServerEntry templateServerEntryArray[] = 
                               new ServerEntry[0];    

  /** A template for the .toArray() function */
  private static final MapEntry templateMapEntryArray[] = 
                               new MapEntry[0];    




  // =======================================================================
  //                      Event Handlers and related Maintainers 
  // =======================================================================

  /** The main exported event handler. */
  private final EventHandler       mainHandler;

  /** The error exported event handler. */
  private final EventHandler       errorHandler;

  /** The input exported event handler. */
  private final EventHandler       inputHandler;

  /** The remote reference for the local input handler. */
  private RemoteReference inputHandlerRef;

  /** The lease maintainer for the input handler export. */
  private LeaseMaintainer inputHandlerMaintainer;


  /** The handler that manages our bindings in servers. */
  ServerManager serverManager;

  /** The remote reference for the local input handler. */
  private RemoteReference serverManagerRef;

  /** The lease maintainer for the listen request. */
  private LeaseMaintainer serverManagerMaintainer;


  /** The request imported event handler. */
  private final Component.Importer requestHandler;

  /** The lease imported event handler. */
  private Component.Importer leaseHandler;


  /** An Operation pattern to try to bind to a server */
  private Operation bindingOperations; 

  /** A remote reference to the response handler for bindingOperations */
  private RemoteReference bindingRef;

  /** The lease maintainer for exported bindingRef event handler. */
  private LeaseMaintainer bindingMaintainer;


  /** The udp channel on which to listen for announcements. */
  private EventHandler announceChannel;
  
  /** The lease maintainer for the announce channel. */
  private LeaseMaintainer announceMaintainer;


  /** The lease maintainer for the listen request. */
  private LeaseMaintainer listenMaintainer;


  /** A timer component. */
  private Timer timer;



  // =======================================================================
  //                           Instance fields
  // =======================================================================

  /** The mapping from guids to leases. */
  private Map idToMapEntry;

  /** The component state. */
  private int state;

  /** The lock. */
  private Object lock;

  /** The election manager object. */
  private ElectionManager electionManager;

  /** The list of servers to send to. */ 
  private ServerList serverList = new ServerList();


  // =======================================================================
  //                           Descriptors
  // =======================================================================

  /** The component descriptor. */
  private static final ComponentDescriptor SELF =
    new ComponentDescriptor("one.world.discovery.DiscoveryClient",
                            "A component the implements the client side of discovery.",
                            true);

  /** The exported event handler descriptor for the main handler. */
  private static final ExportedDescriptor MAIN =
    new ExportedDescriptor("main",
                           "The exported main handler.",
                           new Class[] { EnvironmentEvent.class },
                           null,
			   false);

  /** The exported event handler descriptor for the input handler. */
  private static final ExportedDescriptor INPUT =
    new ExportedDescriptor("input",
                           "The exported input handler.",
                           new Class[] { BindingRequest.class,
					 RemoteEvent.class,
					 ResolutionRequest.class },
			   new Class[] { UnknownResourceException.class },
                           false);

  /** The exported event handler descriptor for the input handler. */
  private static final ExportedDescriptor ERRORS =
    new ExportedDescriptor("deliveryErrors",
                           "The exported error handler.",
                           new Class[] { RemoteEvent.class },
                           null,
                           false);

  /** The imported event handler descriptor for the lease manager. */
  private static final ImportedDescriptor LEASE =
    new ImportedDescriptor("lease",
                           "The imported lease manager handler.",
                           new Class[] { LeaseEvent.class },
			   null,
                           false,
                           false);

 /** The imported event handler descriptor for the request handler. */
  private static final ImportedDescriptor REQUEST =
    new ImportedDescriptor("request",
                           "The imported request handler.",
                           new Class[] { BindingRequest.class },  
			   null,
                           false,
                           false);



  // =======================================================================
  //                           The main handler
  // =======================================================================

  /** The main exported event handler. */
  final class MainHandler extends AbstractHandler {

    /** Handle the specified event. */
    protected boolean handle1(Event e) {

      if (e instanceof EnvironmentEvent) {
        //The component is starting
	EnvironmentEvent ee = (EnvironmentEvent)e;
        
        switch(ee.type) {	
	  case EnvironmentEvent.ACTIVATED: 
            if (Constants.DEBUG_DISCOVERY) {
              log.log(this,"Got ACTIVATE event");
            }
            activate(); 
            electionManager.notify(ee);
            break;
          default:
            log.logWarning(this,"Unexpected EnvironmentEvent type in MainHandler: "+ee.type);
            break;
	}
	return true;	

      } else if (e instanceof BindingResponse) {
        //We've bound a DiscoveryClient resource of some sort
	return true;

      } else if (e instanceof InputResponse) {
        //We heard a server ann ouncement
	InputResponse ir = (InputResponse)e;

	if (ir.tuple instanceof AnnounceEvent) {
          //We received notice that a server exists.
	  AnnounceEvent ae = (AnnounceEvent)(ir.tuple);

	  if (Constants.DEBUG_DISCOVERY) {
	    //  log.log(this,"Found server: " + ae.ref + " , with capacity " +
	    //	    ae.capacity);
	  }

          //The the electionManager about the server.          
	  electionManager.notify(ae);

          //If we're exiting, ignore 
          synchronized(lock) { 
  	    if (state == CLOSING) {
              return true;
            }
          } 

          //See if we already knew about this
	  ServerEntry entry = serverList.getServer(ae.ref);

	  if (null != entry) {
            //We did know about it.
            if (ae.capacity>=0) {
              //It's not closing or closed.
              synchronized(entry.lock) {
                entry.sawServer = 0;
              }
              //It's not bad or closing.
              serverList.markBadServer(entry.ref,false);
              serverList.markCancellingServer(entry.ref,false);
            } else if (ae.capacity == DiscoveryServer.ANNOUNCE_CLOSING) {
              //It's closing, mark it as such.
              serverList.markBadServer(entry.ref,false);
              serverList.markCancellingServer(entry.ref,true);
            } else if (ae.capacity == DiscoveryServer.ANNOUNCE_CLOSED) {
              //It's closed, remove it from our list.
              entry.killServer();
            }
	  } else { 
            //We didn't know about it.
	    if (Constants.DEBUG_DISCOVERY) {
	      log.log(this,"Server is not in list, acquiring lease");
	    }

            if (ae.capacity>=0) {
              //It's a good server
              entry = new ServerEntry(ae.ref,true);
              serverList.addServer(entry);
              //Add our bindings to this server.
              EntryEvent ee = new EntryEvent(this,null,EntryEvent.ADDSERVER,
                                             null,ae.ref);
              serverManager.handle(ee);
            } else {
              //Not worth adding a server that's going away, ignore
            }
	  }
	}

	return true;

      } else if (e instanceof ListenResponse) {
	ListenResponse lr = (ListenResponse)e;
	listenMaintainer = new LeaseMaintainer(lr.lease, lr.duration,
					       this, null, timer);
	return true;
      }

      return false;
    }
  }

  // =======================================================================
  //                           The Error handler
  // =======================================================================

  /** 
   * The exported error event handler. 
   *
   * This receives events that REP tried to deliver for discovery, but which
   * were for an unknown resource.  It hands them off to us, and we forward
   * the info on to the server.
   */
  final class ErrorHandler extends AbstractHandler {
    /** Handle the specified event. */
    protected boolean handle1(Event e) {

      if (e instanceof RemoteEvent) {
        RemoteEvent re = (RemoteEvent)e;

        if (re.event instanceof ExceptionalEvent) {
          return true;
        }
        RemoteReference dss = (RemoteReference)e.metaData.remove(Constants.DISCOVERY_SOURCE_SERVER);

        if (dss != null) {
          Object db  = e.metaData.remove(Constants.DISCOVERY_BINDING);
          if (Constants.DEBUG_DISCOVERY) {
            log.log(this,"Got an undeliverable for "+dss);
          }
          Exception x = new UnknownResourceException(
                                 "Unknown destination for discovery delivery");
          ExceptionalEvent ee = new ExceptionalEvent(inputHandlerRef,db,x);
          RemoteEvent rev = new RemoteEvent(this,null,dss,ee);
          requestHandler.handle(rev);
        } else {
          log.logWarning(this,"Got an undeliverable with unknown metaData");
        }
        return true;
      } else if (e instanceof ExceptionalEvent) {
        if (Constants.DEBUG_DISCOVERY) {
          log.log(this,"Got an exception "+e);
        }
        return true;
      }
      return false;
    }
  }

  // =======================================================================
  //                           The input handler
  // =======================================================================

  /** The input exported event handler. */
  final class InputHandler extends AbstractHandler {

    /** Handle the specified event. */
    protected boolean handle1(Event e) {

      if (state != ACTIVE) {
	// FIXME: This is a temporary, stupid fix.  Really, all of the lease
	// handling stuff should be in a separate event handler.
        if (e.source != this) {
          respond(e,new NotActiveException()); //FIXME: only for environments?
	}
	return true;
      }

      if (e instanceof EnvironmentEvent) {
	EnvironmentEvent ee = (EnvironmentEvent)e;

        switch(ee.type) {	
          case EnvironmentEvent.STOP:
            //We're shutting down
            if (Constants.DEBUG_DISCOVERY) {
              log.log(this,"Got STOP event");
            }

            // release resources
            stop();
            respond(e, new EnvironmentEvent(mainHandler, 
                                            null, 
                                            EnvironmentEvent.STOPPED,
                                            getEnvironment().getId()));
            break;
          default:
            log.logWarning(this,"Unexpected EnvironmentEvent type in InputHandler: "+ee.type);
            break;
        }
        return true;	

      } else if (e instanceof RemoteEvent) {   
        //We got a late binding to pass on

	RemoteEvent re = (RemoteEvent)e;
	re.source = re.event.source; //replace local handler with remote ref.
	if (re.destination instanceof DiscoveredResource) {
          ServerEntry sentry = serverList.defaultResolver();
          if (sentry != null) {
            RemoteEvent newEv = 
                  new RemoteEvent(this,
                                  sentry.ref,sentry.ref,
				  re,
				  re.datagram);
            newEv.metaData = re.metaData;
            newEv.metaData.remove(Constants.REQUESTOR_ID);
            re.metaData    = null;
            re.closure     = null;
            requestHandler.handle(newEv);
          }
	  return true;
	} else if (re.destination instanceof RemoteReference) {
	  // handle responses here
	  if (re.event instanceof ExceptionalEvent) {
            ExceptionalEvent xEv = (ExceptionalEvent)re.event;
	    if (xEv.x instanceof UnknownResourceException) {
	      if (Constants.DEBUG_DISCOVERY) {
		log.log(this, e.toString());
	      }
	    } else if (xEv.x instanceof NotActiveException) {
            } else {
              log.log(this, e.toString());
            }
	  }
          return true;
	}  
      } else if (e instanceof ResolutionRequest) {

        ///////// Lookup /////////////
        // Forward resolution requests to the server.  Responses get sent back
        // to the requesting handler.

	if (Constants.DEBUG_DISCOVERY) {
	  log.log(this,"Got ResolutionRequest");
	}

	ResolutionRequest request = (ResolutionRequest)e;
	
	if (request.resource instanceof DiscoveredResource) {
          ServerEntry sentry        = serverList.defaultResolver();
          if (sentry != null) { 
            requestHandler.handle(
	      new RemoteEvent(this, null, sentry.ref, request));
          } else {
	    respond(requestHandler,e,inputHandlerRef,
  		    new UnknownResourceException("No discovery server found"));
          }
	} else {
          log.logWarning(this,"Illegal resource type: "+request.resource);
        } 
	return true;

      } else if (e instanceof BindingRequest) {
        //Register a new service with discovery

	BindingRequest br = (BindingRequest)e;

	if (Constants.DEBUG_DISCOVERY) {
	  log.log(this,"Got BindingRequest");
	}

	if (br.descriptor instanceof RemoteDescriptor) {
	  RemoteDescriptor ed = (RemoteDescriptor)br.descriptor;

          // Create a new, anonymous remote descriptor for the handler to
	  // be exported through discovery.
	  RemoteDescriptor newED = new RemoteDescriptor(ed.handler);
							
	  // Create a new map entry
	  MapEntry entry = new MapEntry();

	  // Create a closure for the lease maintainer
          LocalClosure lc = new LocalClosure(entry.id,br); 

          //add the map entry to the map
          //What if the BindingRequest is dropped? Can that happen?
	  idToMapEntry.put(entry.id,entry);
	  
	  // Export the handler locally using a lease maintainer.
	  entry.refMaintainer = new LeaseMaintainer( 
                                       new BindingRequest(this, lc, newED, Duration.FOREVER), 
                                       requestHandler, timer);
	} else {
          log.logWarning(this,"Illegal descriptor type: "+br.descriptor);
        }

	return true;

      } else if (e instanceof BindingResponse) {
        //The local binding was completed
	
        BindingResponse response = (BindingResponse)e;

	if (Constants.DEBUG_DISCOVERY) {
	  log.log(this,"Got BindingResponse");
	}

	if (response.closure instanceof LocalClosure) {
          //This really is a response to a local binding.
	  LocalClosure lc = (LocalClosure)response.closure; 

	  // Handler is locally exported; now we have to export it to the 
	  // discovery server.
	    
          if (Constants.DEBUG_DISCOVERY) {
            log.log(this,"Got the remote ref lease " 
                    + response.lease + ", duration " 
                    + response.duration);
	  }

	  RemoteReference ref = (RemoteReference)response.resource;

	  // Get the map entry and add the lease and request to it.
	  MapEntry entry = (MapEntry)idToMapEntry.get(lc.id);

	  if (null == entry) {
	    throw new Bug("Entry not found in hash");
	  }

	  BindingRequest br   = lc.breq;
          RemoteDescriptor rd = (RemoteDescriptor)br.descriptor.clone();
          synchronized(entry.lock) { 
            rd.id               = entry.id;
            rd.handler          = ref;

            entry.refLease      = response.lease;
	    entry.remoteRef     = ref;
	    entry.bindreq       = new BindingRequest(br.source,br.closure,
                                                     rd,br.duration);
          }
          EntryEvent ee       = new EntryEvent(inputHandler,null,
                                               EntryEvent.ADD,
                                               lc.id, null); 
          serverManager.handle(ee);

          leaseHandler.handle(
              new LeaseEvent(this,lc,
                             LeaseEvent.ACQUIRE,
                             new LeaseCancellationHandler(lc.id),
                             response.descriptor,
                             br.duration));
          return true;
        } else {
          log.logWarning(this,"Unexpected BindingResponse closure "+response.closure);
        }

      } else if (e instanceof LeaseEvent) {
        //We got a message about a lease for the discovery entry

	LeaseEvent le = (LeaseEvent)e;

	switch(le.type) {
          case LeaseEvent.ACQUIRED:
            //We got the lease for the discovery entry, pass it back to the user
            if (le.closure instanceof LocalClosure) {
              LocalClosure lc = (LocalClosure)le.closure;

              if (Constants.DEBUG_DISCOVERY) {
                log.log(this,"Got local lease " + le.handler 
                        + ", duration = " + le.duration);
              }

              // Add lease to map
              MapEntry entry   = (MapEntry)idToMapEntry.get(lc.id);

              synchronized(entry.lock) {
                entry.localLease = le.handler;
              }

              // Respond to original binding request.
              respond(entry.bindreq,
                      new BindingResponse(this,
                                          entry.bindreq.closure,
                                          entry.bindreq.descriptor,
                                          entry.remoteRef,
                                          le.handler,
                                          le.duration));
	      return true;
	    }

          case LeaseEvent.CANCELED:
            //FIXME: Deal with this better.  IE, check to see if it should be cancelled
            if (Constants.DEBUG_DISCOVERY) {
              LocalClosure lc = (LocalClosure)le.closure;
              log.log(this, "Lease for " + lc.id + " canceled");
            }
            return true;
        }
      } else if (e instanceof ExceptionalEvent) {

	ExceptionalEvent ee = (ExceptionalEvent)e;

	if (ee.x instanceof LeaseRevokedException) {
	  //Do nothing.  This results from canceling a lease twice.
	  return true;
	} else if (ee.x instanceof ConnectionFailedException) {
          if (Constants.DEBUG_DISCOVERY) {
            log.log(this,"Connection failed exception: "+ee.x);
          }
          if (ee.closure instanceof RemoteReference) {
            ServerEntry sentry = serverList.getServer((RemoteReference)ee.closure);
            if (sentry != null) {
              sentry.killServer();
            }
            return true;
          }
        } else if (ee.x instanceof UnknownResourceException) {
          if (ee.closure instanceof RemoteReference) {
            ServerEntry sentry = serverList.getServer((RemoteReference)ee.closure);
            if (sentry != null) {
              sentry.killServer();
            }
            return true;
          }
        } else if (ee.x instanceof NotActiveException) {
          if (ee.closure instanceof RemoteReference) {
            serverList.markBadServer((RemoteReference)ee.closure,true);
            return true; 
          }
        } else if (ee.x instanceof LeaseRevokedException) {
          //This wasn't the bug since it would have complained.
          //FIXME: handle this right
        } else if (ee.x instanceof LeaseDeniedException) {
          if (ee.closure instanceof LocalClosure) { 
            //There was a problem either binding the handler for the
            //exported client handler, or obtaining the lease for the discovery
            //binding itself.  
            MapEntry mentry;
            LocalClosure lc;

            lc     = (LocalClosure)ee.closure;
            mentry = (MapEntry)idToMapEntry.remove(lc.id);

            if (null != mentry) {
              if (null != mentry.refMaintainer) {
                mentry.refMaintainer.cancel();
              }
            }

            ee.source = this;

            respond(lc.breq, ee); 
            return true;
          } 
        }
      }
      return false;
    }
  }

  // =======================================================================
  //                           The lease cancellation event handler
  // =======================================================================

  /** 
   * Handles registration lease cancellations.
   */
  final class LeaseCancellationHandler extends AbstractHandler {
    
    public LeaseCancellationHandler(Guid id) {
      this.id = id;
    }
    
    /** The guid to look up in the hash table. */
    Guid id;

    /** Handle the specified event. */
    protected boolean handle1(Event e) {
      if (e instanceof LeaseEvent) {
	LeaseEvent le = (LeaseEvent)e;
	if (LeaseEvent.CANCELED == le.type) {
	  // Cancel the lease and all associated leases.
	  if (Constants.DEBUG_DISCOVERY) {
	    log.log(this, "InterceptHandler for " + id 
	                  + " got CANCELED LeaseEvent, source is " 
	                  + le.source 
		          + ", lease handler is " + le.handler);
	  }
	  revokeMapEntry(id);
	  return true;
	}
      }
      return false;
    }
  }


  // =======================================================================
  //                           Helper methods
  // =======================================================================

  /**
   * Remove the MapEntry pointed to by id and revoke all leases related to this
   * MapEntry.
   */
  private void revokeMapEntry(Guid id) {
    MapEntry entry;
    java.util.Iterator it; 

    if (Constants.DEBUG_DISCOVERY) {
      log.log(this,"Revoking all leases for " + id);
    }
 
    if (null == id) {
      throw new Bug("null id passed to function");
    }

    synchronized(idToMapEntry) {
      // Find associated leases in map
      entry = (MapEntry)idToMapEntry.get(id);
      idToMapEntry.remove(id);
    }

    if (null == entry) {
      //No such map entry
      if (Constants.DEBUG_DISCOVERY) {
        log.logWarning(this,"Entry " + id + " not found in registry");
      }
      return;
    }

    synchronized(entry.lock) {
      it = entry.allServers.values().iterator(); 
      //Hackish way to avoid concurrent operations
      entry.allServers = new HashMap();
    }   

    while (it.hasNext()) {
      //Cancel each binding
      BoundServer bs = (BoundServer)it.next();
      if (bs != null) {
        bs.doCancel();
      }
    }
 
    //Cancel the leases
    
    LeaseEvent le = new LeaseEvent(nullReturn,null,LeaseEvent.CANCEL,null,null,0);
    entry.refLease.handle(le); 

    le = new LeaseEvent(nullReturn,null,LeaseEvent.CANCEL,null,null,0);
    if (entry.localLease != null) {
      entry.localLease.handle(le); 
    }
  }






  // =======================================================================
  //                           Constructor
  // =======================================================================

  /**
   * Create a new instance of <code>DiscoveryClient</code>.
   * This does NOT issue and binding requests.
   *
   * @param  env  The environment for the new instance.
   */
  public DiscoveryClient(Environment env) {
    super(env);

    // Obtain a timer component.
    this.timer = getTimer();
    
    // Declare imported and exported event handlers.
    mainHandler = declareExported(MAIN, new MainHandler());
    errorHandler = declareExported(ERRORS, new ErrorHandler());
    inputHandler = declareExported(INPUT, new InputHandler());
    requestHandler = declareImported(REQUEST);
    leaseHandler = declareImported(LEASE);

    // Initialize fields.
    idToMapEntry = java.util.Collections.synchronizedMap(new HashMap());
    lock       = new Object();
    state      = INACTIVE;
    electionManager   = new ElectionManager(timer,requestHandler,leaseHandler,
				     getEnvironment());
    serverManager = new ServerManager();
  }


  // =======================================================================
  //                           Component support
  // =======================================================================

  /** Get the component descriptor. */
  public ComponentDescriptor getDescriptor() {
    return (ComponentDescriptor)SELF.clone();
  }

  /** 
   * Activates the component.  Synchronously binds needed resources
   * and modifies internal state variable as appropriate.  
   */
  private void activate() {
    BindingRequest bindreq;

    synchronized (lock) {
      if (state != INACTIVE) {
        return;
      }
      state = ACTIVATING;
    }

    // Check that the necessary imported event handlers are linked.
    if (!leaseHandler.isLinked()) {
      log.logError(this, "Imported event handler lease not linked");
      stop();
      return;
    }

    if (!requestHandler.isLinked()) {
      log.logError(this, "Imported event handler request not linked");
      stop();
      return;
    }

    //bind remote reference for input handler.
    bindreq = new BindingRequest(mainHandler,"input", 
                                 new RemoteDescriptor(inputHandler),
                                 Duration.FOREVER);
    Event response;
    try {
      response = Synchronous.invoke(requestHandler,bindreq,
				    Constants.SYNCHRONOUS_TIMEOUT);
    } catch (TimeOutException x) {
      log.logError(this, 
                  "Could not obtain remote reference for input handler",
		  x);
      stop();
      return;
    }
    
    if (response instanceof BindingResponse) {
      BindingResponse br = (BindingResponse)response;
      inputHandlerRef = (RemoteReference)br.resource;
      inputHandlerMaintainer = 
	new LeaseMaintainer(br.lease,
			    br.duration,
			    mainHandler,
			    null,
			    timer);
    } else if (response instanceof ExceptionalEvent) {
      log.logError(this, "Could not obtain remote reference",
                   ((ExceptionalEvent)response).x);
      stop();
      return;
    }

    //bind remote reference for server handler.
    bindreq = new BindingRequest(mainHandler,"server", 
				 new RemoteDescriptor(serverManager),
				   Duration.FOREVER);
    try {
      response = Synchronous.invoke(requestHandler,bindreq,
				    Constants.SYNCHRONOUS_TIMEOUT);
    } catch (TimeOutException x) {
      log.logError(this, 
                  "Could not obtain remote reference for server handler",
		  x);
      stop();
      return;
    }
    
    if (response instanceof BindingResponse) {
      BindingResponse br = (BindingResponse)response;
      serverManagerRef = (RemoteReference)br.resource;
      serverManagerMaintainer = 
	new LeaseMaintainer(br.lease,
			    br.duration,
			    mainHandler,
			    null,
			    timer);
    } else if (response instanceof ExceptionalEvent) {
      log.logError(this, "Could not obtain remote reference",
                   ((ExceptionalEvent)response).x);
      stop();
      return;
    }

    //Start the serverManager
    serverManager.start();

    //bind DatagramIO input channel to listen for announcements
    SioResource sio = new SioResource();
    sio.remoteHost = Constants.DISCOVERY_ANNOUNCE_ADDR;
    sio.remotePort = Constants.DISCOVERY_ANNOUNCE_PORT;
    sio.type       = SioResource.MULTICAST;

    bindreq = new BindingRequest(mainHandler, "announce",
                                 sio, Duration.FOREVER);

    try {
      response = Synchronous.invoke(requestHandler,bindreq,
				    Constants.SYNCHRONOUS_TIMEOUT);
    } catch (TimeOutException x) {
      log.logError(this, "Could not obtain multicast channel", x);
      stop();
      return;
    }

    //save resource and get maintainer for the datagram channel lease
    if (response instanceof BindingResponse) {
      if (Constants.DEBUG_DISCOVERY) {
	log.log(this,"Bound datagram channel");
      }
      BindingResponse br = (BindingResponse)response;
      announceChannel = br.resource;
      announceMaintainer = 
	new LeaseMaintainer(br.lease,
			    br.duration,
			    mainHandler,
			    null,
			    timer);
    } else if (response instanceof ExceptionalEvent) {
      log.logError(this, "Could not obtain multicast channel",
                         ((ExceptionalEvent)response).x);
      stop();
      return;
    }

    //send listen request
    SimpleInputRequest inputreq = 
      new SimpleInputRequest(mainHandler,
			     null,
			     SimpleInputRequest.LISTEN,
			     new Query(),
			     Duration.FOREVER,
			     false);
    announceChannel.handle(inputreq);

    synchronized(lock) {
      state = ACTIVE;
    }
  }

  
  /** 
   * Stops the component.  Changes the internal state to INACTIVE,
   * releases resources and sends a STOP event to the environment
   * request handler. 
   */
  private void stop() {
    int i;

    // change state
    if (Constants.DEBUG_DISCOVERY) {
      log.log(this,"Stopping discovery client");
    }

    synchronized(lock) {
      state = CLOSING;
    }

    //Revoke all of the client bindings
    Object allKeys[] = idToMapEntry.keySet().toArray();

    for (i=0; i<allKeys.length; i++) {    
      Guid entryId=(Guid)allKeys[i];
      if (entryId != null) {
        revokeMapEntry(entryId);
      }
    }

    //Remove all the servers
    ServerEntry[] allServers = serverList.allServers();

    for (i=0; i<allServers.length; i++) {
      allServers[i].killServer();
    } 

    // Also stop the election manager!
    electionManager.stop();

    serverManager.stop();
  }

  // =======================================================================
  //                           MapEntry class
  // =======================================================================

  /** 
   * Groups together the fields associated with a client binding.
   *
   */
  final class MapEntry  {
    
    /** The lease maintainer for a remote reference. */
    /** EML: The lease for the local export of the handler */
    public EventHandler refLease;

    /** The lease given to the application. */
    public EventHandler localLease;

    /** The original binding request. */
    public BindingRequest bindreq;

    /** The exported remote reference. */
    public RemoteReference remoteRef;

    /** The lease maintainer for the remoteRef. */
    public LeaseMaintainer refMaintainer;

    /** 
     * Mapping ServerRef -> BoundServer entry.  Entry is null if not 
     * yet acquired. 
     */
    public HashMap allServers;

    /** The lock */
    Object lock;
 
    /** 
     * The id of this binding.  This identifier is used  to
     * Identify requests pertaining to the same binding.
     */
    Guid id;
 
    /** Create an new empty MapEntry. */
    public MapEntry() {
      allServers = new HashMap();
      lock = new Object();
      id = new Guid();
      //Do nothing.
    }

    /** 
     * There was some sort of error indicating that entry is no longer 
     * registered in serverRef.  
     *
     * Make sure that we should still be registered there, and if so 
     * reaquire the binding.
     */
    public void errorCheckAgain(MapEntry entry,BindingClosure bc) {
      BoundServer bs;
      RemoteEvent rev = null;
      if (Constants.DEBUG_DISCOVERY) {
        log.logWarning(this,"In errorCheckAgain() entry "+entry+" server "+bc.serverRef);
      }
      //Find this BoundServer
      synchronized(entry.lock) {
        bs = (BoundServer)entry.allServers.get(bc.serverRef);
 
        if (null != bs) {
          if (bs.bindingNonce.equals(bc.bindingNonce)) {
            entry.allServers.put(bc.serverRef,null);
          } else {
            //The server binding is here but it's not the one we were expecting.
            return;
          }
        }

        //if the BoundServer entry still exists, cancel the timer
        if (bs!=null && bs.tn!=null) {
          bs.tn.cancel();
        }

        if (serverList.containsServer(bc.serverRef)) {
          //We were canceled, but shouldn't be.  Reaquire.
          BindingRequest br = (BindingRequest)entry.bindreq.clone();
          br.source = bindingRef;
          br.closure = new BindingClosure(BindingClosure.BIND,entry.id,
                                          bc.serverRef,
                                          SystemUtilities.currentTimeMillis(),
                                          bc.bindingNonce);

          rev = new RemoteEvent(serverManager,br.closure,
                                            bc.serverRef,br);
           
          entry.allServers.put(bc.serverRef,null);
          if (Constants.DEBUG_DISCOVERY) {
            log.logWarning(this,"rebinding");
          }
        } else {
          //The server went away so we should be canceled.  Remove our binding.
          entry.allServers.remove(bc.serverRef);
          if (Constants.DEBUG_DISCOVERY) {
            log.logWarning(this,"unbinding");
          }
        }
      }
      if (rev != null) {
        serverManager.handle(rev); 
      }
    }
  }


  // =======================================================================
  //                           ServerEntry class
  // =======================================================================

  /**
   * An entry associating a server we know about.
   */
  final class ServerEntry implements java.io.Serializable {

    /** The server reference. */
    public RemoteReference ref;

    /** Timer to decide a server has disappeared */
    Timer.Notification tn;

    /** 
     * Set to true each time we see an announceEvent for this server.
     * Cleared each time we check for activity.
     * 
     * How many timesteps ago did we see the server?(zero means we'd seen
     * it last timeout, 1 the timeout before that, etc)
     */
    public int sawServer;

    /**
     * This is a "bad" server that we would prefer not to use.
     */
    public boolean badServer;
 
    /**
     * This is server is about to stop, so we would prefer not to use it.
     */
    public boolean cancellingServer;

    /** The lock. */
    Object lock;

    /** Remove this server from our list of servers */    
    public void killServer() {
      synchronized(lock) {
        tn.cancel();
      }
      serverList.removeServer(ref);
    }

    /**
     * Create a new ServerEntry and set up a timer to check that this server 
     * remains active.
     */
    public ServerEntry(RemoteReference ref,boolean isReal) {
      this.ref = ref;
      lock = new Object();
      if (this.ref == null) {
        log.logWarning(this,"ServerEntry with null reference");
        throw new Bug("Problem!!!");
      } 
      if(isReal) {
        synchronized(lock) {
          sawServer = 0;
          tn = timer.schedule(Timer.FIXED_DELAY,
                              SystemUtilities.currentTimeMillis()+
                              Constants.DISCOVERY_ANNOUNCE_PERIOD,
                              Constants.DISCOVERY_ANNOUNCE_PERIOD,
                              serverManager,
                              new ServerCheck(serverManager,null,ref));
        }
      }
       
    }

    /** The hashCode of the entry is the hashCode of the reference */
    public int hashCode() {
      if (ref == null) {
        return 0;
      }
      return ref.hashCode();     
    }

    /** The equality check checks the remote references */
    public boolean equals(Object obj) {
      if (obj instanceof ServerEntry) {
        ServerEntry entry = (ServerEntry)obj;
        if (this.ref == null) {
          log.logWarning(this,"null reference value");
          return (entry.ref == null) ;
        }
        return (this.ref.equals(entry.ref));
      } else  { 
        log.logWarning(this,"Warning: comparing unexpected type"); 
        return false;
      }
    }
  }


  // =======================================================================
  //                           ServerList class
  // =======================================================================

  /**
   * Maintain a list of all servers I know about.
   */
  final class ServerList implements java.io.Serializable {
    //NOTE: Should switch over to a Map rather than a List

    /** 
     * A list of all known servers.  Currently the first element is the
     * default resolver, but this will eventually be moved to an external
     * variable to improve performance.
     */ 
    private LinkedList allServers;

    /**
     * The server we are using as our default resolver
     */
    private ServerEntry defResolver;

    /** 
     * The data structure lock.
     */
    private Object lock;

    /**
     * Return the server we should use to query discovery
     *
     * @return the server to use for a discovery query
     */
    public ServerEntry defaultResolver() {
      synchronized(this.lock) {
        return defResolver;
      }
    }

    /**
     * Suggest that this server not be used as the default resolver.
     */
    public void markBadServer(RemoteReference ref,boolean value) {
      boolean noServer = false;
      ServerEntry sentry;
      boolean changed = false;


      synchronized(lock) {
        if (containsServer(ref)) {
          sentry = getServer(ref);
          synchronized(sentry.lock) {
            if (sentry.badServer != value) {
              sentry.badServer = value;
              changed = true;
              if (Constants.DEBUG_DISCOVERY) {
                log.log(this,"changed server marking to bad="+value+": "+ref); 
              }
            }
          }
          if ((changed && defResolver.ref.equals(ref)) || (value==false)) {
            noServer = chooseDefaultResolver();
          }
        }
      }
      if (noServer) {
        if (state!=CLOSING) {
          electionManager.startElection();
        }
      }
    }


    /**
     * Suggest that this server not be used as the default resolver.
     */
    public void markCancellingServer(RemoteReference ref,boolean value) {
      boolean noServer = false;
      boolean changed = true;
      ServerEntry sentry;

      synchronized(lock) {
        if (containsServer(ref)) {
          sentry = getServer(ref);
          synchronized(sentry.lock) {
            if (sentry.cancellingServer != value) {
              sentry.cancellingServer = true;
              changed = true;
            }
          }
          if ((changed && defResolver.ref.equals(ref)) || (value==false)) {
            noServer = chooseDefaultResolver();
          }
        }
      }
      if (noServer) {
        if (state!=CLOSING) {
          electionManager.startElection();
        }
      }
    }


    /** 
     * Choose a new defResolver.  
     *
     * @return True if there were no good resolvers found, false otherwise.
     */ 
    private boolean chooseDefaultResolver() {
      java.util.Iterator it;
      ServerEntry ent;
      RemoteReference ref;

      ServerEntry backup = null;
      boolean backupCancelling = true;
      boolean backupBad = true; 

      if (allServers.size() == 0) {
        defResolver = null;
        return true;
      } else if (defResolver != null) {
        backup = defResolver;
        backupBad = defResolver.badServer;
        backupCancelling = defResolver.cancellingServer; 
      }

      
      it = allServers.iterator();
      
 
      while (it.hasNext()) {
        ent = (ServerEntry)it.next();

        if ((backupBad && backupCancelling) && 
            ((backup==null) || !(ent.badServer && ent.cancellingServer))) {
          backup = ent;
          backupBad = ent.badServer;
          backupCancelling = ent.cancellingServer;
        } else if (backupBad && !ent.badServer) {
          backup = ent;
          backupBad = ent.badServer;
          backupCancelling = ent.cancellingServer; 
        } else if (backupCancelling && !ent.badServer && 
          !ent.cancellingServer) {
          backup = ent;
          backupBad = ent.badServer;
          backupCancelling = ent.cancellingServer; 
        } 
        if ((ent.badServer != true) && (ent.cancellingServer != true) && 
            (ent.sawServer <2 )) {
          defResolver = ent;
          return false;
        }
      }

      defResolver = backup;

      //No good choices
      return true;
    }

    /**
     * Add the server to the list.
     *
     * @return true if the server was added, false if already exists
     */
    public boolean addServer(ServerEntry entry) {
      boolean noServer = false;

      synchronized(this.lock) {
        if (containsServer(entry.ref)) {
          markBadServer(entry.ref,false); // new
          chooseDefaultResolver();//new
          return false;
        }
        allServers.addLast(entry);
        if ((defResolver == null)  || 
             defResolver.badServer || 
             defResolver.cancellingServer || 
             (defResolver.sawServer>1)) {
          noServer = chooseDefaultResolver();
        }
      }
      if (noServer) {
        if (state!=CLOSING) {
          electionManager.startElection();
        }
      }
      return true;
    }

    /**
     * Remove the server from the list
     *
     * @param ref The server to remove
     * @return true if the server was in the list, false otherwise
     */
    public ServerEntry removeServer(RemoteReference ref) {
      boolean noServer = false;
      ServerEntry returnVal;
      ServerEntry dummy = new ServerEntry(ref,false);

      synchronized(this.lock) {
        returnVal = getServer(ref);
        allServers.remove(dummy);
        if (defResolver != null) {
          if (defResolver.equals(returnVal)) {
            defResolver = null;
            noServer = chooseDefaultResolver();
          }
        } 
      }
      if (noServer) {
        if (state!=CLOSING) {
          electionManager.startElection();
        }
      }
      return returnVal;
    }

    /**
     * See whether the server is in the list
     *
     * @param ref The server to check
     * @return true if the server is in the list, false otherwise
     */
    public boolean containsServer(RemoteReference ref) {
      ServerEntry dummy = new ServerEntry(ref,false);
      synchronized(this.lock) {
        return allServers.contains(dummy);
      }
    }

    /**
     * Retrieve the ServerEntry for the designated server
     *
     * @param ref The server to look up
     * @return The ServerEntry for the reference, null if not found
     */
    public ServerEntry getServer(RemoteReference ref) {
      ServerEntry dummy = new ServerEntry(ref,false);
      synchronized(this.lock) {
        int index;
        index = allServers.indexOf(dummy);
        
        if (index==-1) {
          return null;
        } else {
          return (ServerEntry)allServers.get(index);
        }
      }
    }

    /**
     * Return an array of all known servers.  
     *
     * Changes to the list or to the array will not be reflected in each other.
     *
     * @return the rray of servers
     */
    public ServerEntry[] allServers() {
      synchronized(this.lock) {
        return (ServerEntry[])allServers.toArray(templateServerEntryArray);
      }
    }

    /**
     * Return the number of servers in the list.  
     *
     * @return the rray of servers
     */
    public int size() {
      synchronized(lock) {
        return allServers.size();
      }
    } 

    public ServerList() {
      allServers  = new LinkedList();
      lock        = new Object();
      defResolver = null;
    }
  }

 
  // =======================================================================
  //                    BoundServer class 
  // =======================================================================

 
  /**
   * The structure holding information about a single client binding in a server.
   */
  final class BoundServer {

    /** The lease on our binding in the server */
    public RemoteReference lease;

    /** What is the remaining duration?  This is the  */
    public long duration;

    /** When did we set this duration? (best guess) */
    public long startTime;

    /** The requested duration */
    public long nominalDuration;
     
    /** The lease renewal timer */
    public Timer.Notification tn;

    /** The lock */
    public Object lock;

    /** The lease renew event */
    public LeaseRenew lrenew;
 
    /** Differentiates sequential rebindings*/    
    public Guid bindingNonce;

    /**
     * Create a new BoundServer entry.  This starts a lease renewal timer, but 
     * does NOT place the BoundServer entry in the MapEntry.
     */ 
    public BoundServer(RemoteReference lease, 
                       long startTime,
                       long duration, 
                       long nominalDuration,
                       Guid entryId,
                       RemoteReference serverRef,
                       Guid bindingNonce) {
      lock = new Object();
      synchronized(lock) {
        this.lease     = lease;
        this.duration  = duration;
        this.startTime = startTime;
        this.nominalDuration = nominalDuration;
        this.bindingNonce = bindingNonce;
      }

      lrenew           = new LeaseRenew(serverManager,null,entryId,serverRef,bindingNonce);
      Timer.Notification tmp_tn = timer.schedule(Timer.FIXED_DELAY,
                          startTime+duration/2,
                          duration/8,
                          serverManager,
                          lrenew);

      synchronized(lock) {
        this.tn = tmp_tn;
      }
    };

    /**
     * Reset the renewal timer to 1/2 the current lease duration(measured from startTime)
     */   
    private void resetTimer() {
      Timer.Notification old_tn;

      synchronized(this.lock) {
        old_tn = this.tn;
      };

      if (old_tn != null) {
        old_tn.cancel();
      }

      Timer.Notification new_tn = 
                          timer.schedule(Timer.FIXED_DELAY,
                          startTime+duration/2,
                          duration/8,
                          serverManager,
                          lrenew);

      synchronized(this.lock) {
        this.tn = new_tn;
      };
    }

    /**
     * Deal with a LeaseEvent related to this ServerEntry
     */
    public void doLeaseEvent(LeaseEvent lev,
                             MapEntry entry,BindingClosure bc) {
      
      switch(lev.type) {
        case LeaseEvent.ACQUIRED:
          resetTimer();
          break;
        case LeaseEvent.RENEWED:
          if (Constants.DEBUG_DISCOVERY) {
            log.logWarning(this,"Got lease renewal for "+entry+" in "+bc.serverRef);
          }
          resetTimer();
          break;
        case LeaseEvent.CANCELED:
          if (Constants.DEBUG_DISCOVERY) {
            log.logWarning(this,"Got lease cancellation for "+entry+" in "+bc.serverRef);
          }
          entry.errorCheckAgain(entry,bc);
          break;
        default:
          log.logWarning(this,"Got unexepected lease event "+lev.type+
                              " for "+entry+" in "+bc.serverRef);
          break;
      }
    }

    /**
     * Renews this server binding.  mentry is the client binding entry this 
     * BoundServer is allocated for.
     *
     * If there is no longer a ServerEntry for this server, this binding is
     * revoked and this BoundServer structure is removed.
     */
    public void doLeaseRenew(MapEntry mentry) {
      long time;
      long elapsedTime;
      long requestDuration;
      long remainingDuration;
      BindingClosure closure;
      ServerEntry sentry;
      sentry = serverList.getServer(lrenew.serverRef);

      if (sentry != null) {
        //This server still exists, so attempt to renew the lease.
        time        = SystemUtilities.currentTimeMillis();
        elapsedTime = time-startTime;

        //How much time remains on the list
        remainingDuration = duration - elapsedTime;
        if (remainingDuration < 0) {
          //Probably won't be able to renew, but try anyway using the nominalDuration
          requestDuration = nominalDuration;
        } else {
          //Request only enough extra time to reach our nominalDuration
          if (remainingDuration <nominalDuration) {
            requestDuration = nominalDuration - remainingDuration;
          } else {
            requestDuration = 0;
          }
        }

        closure = new BindingClosure(BindingClosure.RENEW_LEASE,
                                     mentry.id,lrenew.serverRef,0,lrenew.bindingNonce);
        LeaseEvent lev  = new LeaseEvent(serverManagerRef,
                                         closure,
                                         LeaseEvent.RENEW,null,null,
                                         requestDuration);

        RemoteEvent rev = new RemoteEvent(serverManager,closure,lease,lev);

        synchronized(this.lock) {
          startTime = time;
        }

        requestHandler.handle(rev);
      } else {
        //stop the renewal timer
        //Start a timer to remove after lease expires
        //For now, just remove the server from the list
        synchronized(mentry.lock) {
         //Still a race here...
          mentry.allServers.remove(lrenew.serverRef);
        }
        if (tn != null) {
          tn.cancel();
        }
      }         
    }

    /** 
     * Cancel the renewal timer and the binding in the serverManager for this entry.
     */
    public void doCancel(){
      tn.cancel();
      serverManager.cancelServerLease(lease,lrenew.entryId,lrenew.serverRef,
                                      lrenew.bindingNonce);
    } 
  }


  //==========================================================
  //                   ServerManager
  //==========================================================

  /**
   * Manages the client bindings in all of the servers
   */
  final class ServerManager extends AbstractHandler {
    /**
     * Set up all necessary bindings.
     */
    public void start() {
      BindingRequest bindreq;
      Event response;

      /** Create a bindingOperations structure */

      bindingOperations = new Operation(timer, requestHandler, this);

      /** Export the bindingOperations handler */
      
      bindreq = new BindingRequest(this,"binding", 
                                   new RemoteDescriptor(bindingOperations.getResponseHandler()),
                                   Duration.FOREVER);
      try {
        response = Synchronous.invoke(requestHandler,bindreq,
				      Constants.SYNCHRONOUS_TIMEOUT);
      } catch (TimeOutException x) {
        log.logError(this, 
                    "Could not obtain remote reference for bindingOperations handler",
		    x);
        stop();
        return;
      }
      
      if (response instanceof BindingResponse) {
        BindingResponse br = (BindingResponse)response;
        bindingRef = (RemoteReference)br.resource;
        bindingMaintainer = 
	  new LeaseMaintainer(br.lease,
			      br.duration,
			      mainHandler,
			      null,
			      timer);
      }
      else if (response instanceof ExceptionalEvent) {
        log.logError(this, "Could not obtain remote reference",
                     ((ExceptionalEvent)response).x);
        stop();
        return;
      }
    }
  
    public void stop() {
      //FIXME: Is the bindingMaintainer killed?
    }

    /**
     * Cancel the lease for the given entry.
     */ 
    private void cancelServerLease(RemoteReference lease, 
                                   Guid entryId,
                                   RemoteReference serverRef,
                                   Guid bindingNonce) {
      BindingClosure closure;
      LeaseEvent lev;
      RemoteEvent rev;

      closure = new BindingClosure(BindingClosure.CANCEL_LEASE,
                                   entryId,serverRef,0,bindingNonce);
      lev     = new LeaseEvent(serverManagerRef,
                               closure,
                               LeaseEvent.CANCEL,null,null,
                               0);
      rev     = new RemoteEvent(serverManager,closure,lease,lev);

      requestHandler.handle(rev); 
    }

    /**
     * Handle a binding response
     */
    private void handleBindingResponse(EventHandler src, 
                                       BindingResponse bresp) {
      //We're bound to a server
      if (bresp.closure != null) {
        //This is something we sent.

        MapEntry mentry;
        BindingClosure bc = (BindingClosure)bresp.closure;          

        mentry  = (MapEntry)idToMapEntry.get(bc.id);

        if (mentry == null) {
          //Can't find the MapEntry.  Must have been removed.  Remove kill
          //this lease.
          cancelServerLease((RemoteReference)bresp.lease,bc.id,bc.serverRef,bc.bindingNonce);
        } else {
          //The client binding still exists.

          if (Constants.DEBUG_DISCOVERY) {
            log.logWarning(this,"Checking server:"+bc.serverRef);
          }

          if (serverList.containsServer(bc.serverRef)) {
            synchronized(mentry.lock) {
            //This server still exists, so add the binding to the MapEntry
              BoundServer bs;
               
              bs = new BoundServer((RemoteReference)bresp.lease,
                                 bc.startTime,
                                 bresp.duration,
                                 mentry.bindreq.duration,
                                 bc.id,
                                 bc.serverRef,
                                 bc.bindingNonce);
 
              if (Constants.DEBUG_DISCOVERY) {
                log.logWarning(this,"Adding bound server "+bs+" to "+mentry);
              }

              mentry.allServers.put(bc.serverRef,bs);
            }
          } else {
            //This server is gone, so ignore response.
            //put the lease in the MapEntry
            //Start a timer to remove after lease expires
            //**For now, just drop it
          }         
        }
      }
    }

    /**
     *  We got a ServerCheck event.  Make sure this server is still around.
     *  If it is gone, kill the ServerEntry.  Don't kill the bindings in
     *  the server(they will kill themselves when their leases expire).
     */ 
    private void handleServerCheck(ServerCheck sc) {
      ServerEntry sentry = serverList.getServer(sc.serverRef); 

      if (sentry == null){
        if (Constants.DEBUG_DISCOVERY) {
          log.logWarning(this,"ServerCheck for nonexistant server "+sc.serverRef); 
        }
      } else {
        synchronized(sentry.lock) {
          int max = 4*(int)(Constants.DISCOVERY_ELECTION_CALL_TIME/
                            Constants.DISCOVERY_ANNOUNCE_PERIOD);
          if(sentry.sawServer<max) {
            sentry.sawServer++;
            return;
          }
        }
        sentry.killServer();
      }
    }

    /** 
     * We got a LeaseRenew event.  Make sure that this client mapping still that the 
     * ServerEntry for this server exists.  If still here, renew the binding.  Otherwise
     * ignore.
     */
    private void handleLeaseRenew(LeaseRenew lrenew) {
      MapEntry mentry;
      Guid id;
      BoundServer bs;


      mentry  = (MapEntry)idToMapEntry.get(lrenew.entryId);
      if (Constants.DEBUG_DISCOVERY) {
        log.logWarning(this,"Lease renew id " + lrenew.entryId); 
        log.logWarning(this,"Lease renew for " + mentry); 
      }
      if (mentry == null) {
        //Do nothing, someone else will have dealt with this. 
      } else {
        synchronized(mentry.lock) {
          bs = (BoundServer)mentry.allServers.get(lrenew.serverRef);
        }

        if (bs != null) {
          //This function renews the lease if the server still exists,
          //and removes it if the server is gone.
          bs.doLeaseRenew(mentry);
        } 
      }
    }

    /**
     * Expects a BindingClosure as a closure.
     */
    private void handleLeaseEvent(EventHandler src,LeaseEvent lev) {
      BoundServer bs;

      if (lev.closure instanceof BindingClosure) {
        BindingClosure bc = (BindingClosure)lev.closure;
        MapEntry entry    = (MapEntry)idToMapEntry.get(bc.id);
        if (Constants.DEBUG_DISCOVERY) {
          log.log(this,"Lease event for "+entry);
          log.log(this,"Lease event server "+bc.serverRef);
        }
        if (entry==null) {
          //Entry no longer exists
          return;
        } else {
          synchronized(entry.lock) {
            bs = (BoundServer)entry.allServers.get(bc.serverRef);
          }

          if (bs == null) {
            //FIXME: Attempt to rebind.
          } else {
            bs.doLeaseEvent(lev,entry,bc);
          }
        }
      } else {
        log.logWarning(this,"Unknown closure type "+lev.closure);
      }
    }

    private void handleEntryEvent(EntryEvent ee) {
      int i;

      switch (ee.type) {
        case EntryEvent.ADD: {
          MapEntry entry        = (MapEntry)idToMapEntry.get(ee.entryId);
      
          ServerEntry[] servers = serverList.allServers();

          if (entry!=null) {
            for (i=0;i<servers.length;i++) {
              BindingRequest br = (BindingRequest)entry.bindreq.clone();
              br.source = bindingRef;
              br.closure = new BindingClosure(BindingClosure.BIND,ee.entryId,
                                              servers[i].ref,
                                              SystemUtilities.currentTimeMillis(),
                                              new Guid());
              RemoteEvent rev = new RemoteEvent(serverManager,br.closure,
                                                servers[i].ref,br);
              if (Constants.DEBUG_DISCOVERY) {
                log.logWarning(this,"Handling client add");
              }
              synchronized(entry.lock) {
                entry.allServers.put(servers[i].ref,null);
              }
              bindingOperations.handle(rev); 
            }
          }
          break;
        }
        case EntryEvent.REMOVE: {
          MapEntry entry        = (MapEntry)idToMapEntry.remove(ee.entryId);
          BoundServer[] bound;

          if (entry != null) {
            synchronized (entry.lock) {
              bound = 
                (BoundServer[])entry.allServers.values().toArray(templateBoundServerArray); 
            }
            for (i = 0; i < bound.length; i++) {
//FIXME: shouldn't be null
              cancelServerLease(bound[i].lease,ee.entryId,
                                null,bound[i].bindingNonce);
            }
          }
          break;
        }
        case EntryEvent.ADDSERVER: {
          //Add the new server
          //The serverList entry has already been added.
          MapEntry allEntries[];
          Guid allKeys[];
          boolean addit;

          if (Constants.DEBUG_DISCOVERY) {
            log.log(this,"Adding a new server"+ee.serverRef);
          }
          synchronized(idToMapEntry) { 
            allEntries = (MapEntry[])idToMapEntry.values().toArray(templateMapEntryArray);
            allKeys    = (Guid[])idToMapEntry.keySet().toArray(new Guid[0]);
          }
          for (i = 0; i < allEntries.length; i++) {
            RemoteEvent rev;

            synchronized(allEntries[i].lock) {
              if (null == allEntries[i].bindreq) {
                continue;
              }

              BindingRequest br = (BindingRequest)allEntries[i].bindreq.clone();

              br.source  = bindingRef;
              br.closure = new BindingClosure(BindingClosure.BIND,allKeys[i],
                                              ee.serverRef,
                                              SystemUtilities.currentTimeMillis(),
                                              new Guid());
              rev = new RemoteEvent(serverManager,br.closure,
                                                ee.serverRef,br);
              addit = false;
              if (!allEntries[i].allServers.containsKey(ee.serverRef)) {
                addit = true;
                if (Constants.DEBUG_DISCOVERY) {
                  log.logWarning(this,"Handling server add");
                }
                allEntries[i].allServers.put(ee.serverRef,null);
              }
            }
            if (addit) {
              bindingOperations.handle(rev);
            }
          } 
          break;
        }
      }
    }

    private void handleExceptionalEvent(ExceptionalEvent e) {
      if (Constants.DEBUG_DISCOVERY) {
        log.log(this,"Got exceptional event: "+e.x);
      }
      if (e.x instanceof UnknownResourceException) {
      }
    }

    protected boolean handle1(Event e) {
      if (e instanceof RemoteEvent) {
        RemoteEvent re = (RemoteEvent)e;

        if (re.event instanceof BindingResponse) {
          handleBindingResponse(re.source,(BindingResponse)re.event);
          return true;
        } else if (re.event instanceof LeaseEvent) {
          handleLeaseEvent(re.source,(LeaseEvent)re.event);
          return true;
        } else if (re.event instanceof ExceptionalEvent) {
          if (Constants.DEBUG_DISCOVERY) {
            log.logWarning(this,"Got exceptional event "+e);
          }
          ExceptionalEvent eev = (ExceptionalEvent)re.event;
          if (eev.x instanceof LeaseDeniedException ||
              eev.x instanceof LeaseRevokedException ||
              eev.x instanceof ResourceRevokedException ||
              eev.x instanceof UnknownResourceException) {
            if (e.closure instanceof BindingClosure) {
              BindingClosure bc = (BindingClosure)e.closure;

              MapEntry entry    = (MapEntry)idToMapEntry.get(bc.id);
              if (entry != null) {
                entry.errorCheckAgain(entry,bc);
              }
            }
            return true;
          } 

        }
        return false;

      } else if (e instanceof LeaseRenew) {
        handleLeaseRenew((LeaseRenew)e);
        return true;
      } else if (e instanceof ServerCheck) {
        handleServerCheck((ServerCheck)e);
        return true;
      } else if (e instanceof EntryEvent) {
        handleEntryEvent((EntryEvent)e);
        return true;
      } else if (e instanceof ExceptionalEvent) {
        handleExceptionalEvent((ExceptionalEvent)e);
        return true;
      } else {
        return false;
      } 
    }
  }


  //==========================================================
  //                      Events
  //==========================================================
  /**
   * Event used to inform the ServerManager that something happened.
   */
  static class EntryEvent extends TypedEvent {
    /** This is a new client binding: add it to all servers */
    public static final int ADD       = 1;

    /** Remove this client binding from all servers */
    public static final int REMOVE    = 2;

    /** Remove this server from all bindings when they expire*/
    public static final int ADDSERVER = 3; 

    

    /** The client binding for ADD or REMOVE */
    public Guid entryId;

    /** The server for ADDSERVER */
    public RemoteReference serverRef;

    public EntryEvent(EventHandler source, Object closure, int type,
                      Guid entryId, RemoteReference serverRef) {
      super(source,closure,type);
      this.entryId       = entryId;
      this.serverRef = serverRef;
      if (this.entryId != null && this.serverRef != null) {
        log.logError(this,"entry and serverRef are both non-null");
      }
    }
  }

  /**
   * Event sent periodically to tell us to renew a client binding in a
   * server.
   */
  final static class LeaseRenew extends Event {
    /** The id of the entry to check */
    public Guid entryId;
 
    /** The server we should make sure it is still bound in */
    public RemoteReference serverRef;
  
    /** The nonce for the binding */
    public Guid bindingNonce;

    public LeaseRenew() {
    }

    public LeaseRenew(EventHandler source, Object closure, 
                      Guid entryId, RemoteReference serverRef, Guid bindingNonce) {
      super(source,closure);
      this.entryId   = entryId;
      this.serverRef = serverRef;
      this.bindingNonce = bindingNonce;
    }
  }

  /**
   * Event sent periodically to tell us to check that a server is still around
   */
  final static class ServerCheck extends Event {
    /** The server to check */
    public RemoteReference serverRef;
    public ServerCheck() {
    }
    public ServerCheck(EventHandler source, Object closure, 
                      RemoteReference serverRef) {
      super(source,closure);
      this.serverRef = serverRef;
    }
  }

  //==========================================================
  //                      Closures
  //==========================================================

  /**
   * Closure included in BindingRequest events sent to the servers
   */
  final static class BindingClosure extends Tuple {
    /** The server we are interacting with */
    public RemoteReference serverRef;
  
    /** The time when we started the interaction */
    public long startTime;

    /** The type of the interaction */
    public int type;

    /** 
     * The nonce for this particular binding.  This is used to
     * ignore messages from the server about stale bindings.  IE,
     * if the server goes away, we remove it's entry, then comes back
     * and we rebind, the old and new bindings have different nonces.
     */
    public Guid bindingNonce;

    /** This is the closure to a binding */
    public static final int BIND         = 1;

    /** This is the closure to a lease renewal attempt*/
    public static final int RENEW_LEASE  = 2;

    /** This is the closure to a lease cancellation attempt*/
    public static final int CANCEL_LEASE = 3;
  
    public BindingClosure() {

    };

    public BindingClosure(int type, Guid id, 
                          RemoteReference serverRef,long startTime,
                          Guid bindingNonce) {
      this.id           = id;
      this.serverRef    = serverRef;
      this.startTime    = startTime;
      this.type         = type;
      this.bindingNonce = bindingNonce;
    }    
  }


  /**
   * Closure included when binding the reference for export
   */
  final static class LocalClosure extends Tuple {
    public BindingRequest breq;
    
    public LocalClosure() {};
    public LocalClosure(Guid id,BindingRequest breq) {
      this.id   = id;
      this.breq = breq;
    }
  }


  /**
   * An event handler that eats any events sent to it without doing anything.
   * Used mainly when cancelling leases for entries that we don't know how to
   * do anything about if they fail.
   */
  EventHandler nullReturn = new AbstractHandler() {
    public boolean handle1(Event e) {
      if (Constants.DEBUG_DISCOVERY) {
        log.log(this, "Null return got" + e);
      }
      return true;
    }
  };
}

