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

import one.util.Bug;
import one.util.Guid;

import one.world.Constants;

import one.world.binding.BindingRequest;
import one.world.binding.BindingResponse;
import one.world.binding.Duration;
import one.world.binding.LeaseDeniedException;
import one.world.binding.LeaseEvent;
import one.world.binding.LeaseManager;
import one.world.binding.LeaseRevokedException;
import one.world.binding.UnknownResourceException;

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
import one.world.core.SymbolicHandler;
import one.world.core.Tuple;

import one.world.data.Name;

import one.world.env.EnvironmentEvent;

import one.world.io.DatagramIO;
import one.world.io.InputResponse;
import one.world.io.SimpleOutputRequest;
import one.world.io.OutputResponse;
import one.world.io.Query;
import one.world.io.SioResource;
import one.world.io.TupleFilter;

import one.world.rep.AlreadyBoundException;
import one.world.rep.DiscoveredResource;
import one.world.rep.RemoteDescriptor;
import one.world.rep.RemoteEvent;
import one.world.rep.RemoteReference;
import one.world.rep.ResolutionRequest;
import one.world.rep.ResolutionResponse;

import one.world.util.AbstractHandler;
import one.world.binding.LeaseMaintainer;
import one.world.util.Log;
import one.world.util.NullHandler;
import one.world.util.Synchronous;
import one.world.util.SystemUtilities;
import one.world.util.TimeOutException;
import one.world.util.Timer;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.NotSerializableException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * A service discovery server.  Accepts service registrations and
 * issues leases for storing these registrations in a registry.
 * Answers client requests for service lookups and routes events to
 * services using a late binding approach.  None of the server event
 * handlers should be directly used by application code.  Interaction
 * with the server is performed by the {@link
 * one.world.rep.DiscoveryClient} component.  
 *
 * <p><b>Imported and Exported Event Handlers</b></p>
 *
 * <p>Exported event handlers:<dl>
 *    <dt>main</dt>
 *    <dd>Handles environment events.
 *        </dd>
 * </dl></p>
 *
 * <p>Imported event handlers:<dl>
 *    <dt>request</dt>
 *    <dd>The environment request handler.
 *        </dd>
 * </dl></p>
 *
 * <p>Other event handlers:<dl>
 *    <dt>server</dt>
 *    <dd>Handles {@link RemoteEvent}s from {@link DiscoveryClient}s
 *    </dd>
 * </dl></p>
 *
 * @see      DiscoveryClient
 * @version  $Revision: 1.41 $
 * @author   Adam MacBeth 
 * @author   Eric Lemar
 */
public final class DiscoveryServer extends Component {
  
  // =======================================================================
  //                           Static fields
  // =======================================================================


  /** The inactive state: the default state. */
  private static final int INACTIVE = 0;

  /** The activating state: The component is acquiring resources. */
  private static final int ACTIVATING = 1;

  /** The active state: The component is running. */
  private static final int ACTIVE = 2;

  /** Capacity value indicating we are closing */
  static final int ANNOUNCE_CLOSING = -1;

  /** Capacity value indicating that we are closed */
  static final int ANNOUNCE_CLOSED  = -2;

  /** Capacity value indicating we want to send a normal capacity */
  static final int ANNOUNCE_NORMAL  = -3;
  
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


  // =======================================================================
  //                           Instance fields
  // =======================================================================

  /** The notification handler for cancelling the announcement timer. */
  private Timer.Notification announceTimerNotification;

  /** The registrations */
  private Registry registry = new Registry();

  /** The registrations of listeners */
  private Registry listeners = new Registry();

  /** The registrations of listeners */
  private Registry consumers = new Registry();

  /** The component state. */
  private int state;

  /** The state lock. */
  private Object lock = new Object();


  // =======================================================================
  //                       Event Handlers and Maintainers 
  // =======================================================================

  /** The main exported event handler. */
  private final EventHandler       main;

  /** The bare MainHandler */
  private final MainHandler mainHandler;

  /** The server event handler. */
  private final EventHandler       server;

  /** The request imported event handler. */
  private final Component.Importer requestHandler;
  
  /** The lease imported event handler. */
  private final Component.Importer lease;


  /** A timer component. */
  private final Timer timer;

    
  /** The bound multicast udp channel for announcements. */
  private EventHandler announceChannel;

  /** The lease maintainer for the bound udp channel. */
  private LeaseMaintainer announceMaintainer;


  /** The remote reference for the server handler. */
  private RemoteReference serverHandlerRef;
  
  /** The lease maintainer for the exported server handler reference. */
  private LeaseMaintainer serverRefMaintainer;


  // =======================================================================
  //                           Descriptors
  // =======================================================================
    
  /** The component descriptor. */
  private static final ComponentDescriptor SELF =
    new ComponentDescriptor("one.world.discovery.DiscoveryServer",
                            "A service discovery server.",
                            true);

  /** The exported event handler descriptor for the main handler. */
  private static final ExportedDescriptor MAIN =
    new ExportedDescriptor("main",
                           "The exported main handler.",
                           null,   // XXX
                           null,   // XXX
                           false);

  /** The imported event handler descriptor for the request handler. */
  private static final ImportedDescriptor REQUEST =
    new ImportedDescriptor("request",
                           "The imported request handler.",
                           new Class[] { BindingRequest.class }, 
                           null,   // XXX
                           false,
                           false);

  /** The imported event handler descriptor for the lease manager. */
  private static final ImportedDescriptor LEASE =
    new ImportedDescriptor("lease",
                           "The imported lease handler.",
                           new Class[] { LeaseEvent.class },   // XXX
                           null,   // XXX
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
	EnvironmentEvent ee = (EnvironmentEvent)e;
	
	if (EnvironmentEvent.ACTIVATED == ee.type) {
	  if (Constants.DEBUG_DISCOVERY) {
	    log.log(this, "Got ACTIVATE event");
	  }
	  activate(); 

	} else if (EnvironmentEvent.STOP == ee.type) {

	  if (Constants.DEBUG_DISCOVERY) {
	    log.log(this,"Got STOP event");
	  }
	  stop();
	  respond(ee, new EnvironmentEvent(this, null, 
	                                   EnvironmentEvent.STOPPED,
					   getEnvironment().getId()));
	} else {
          log.logWarning(this,"Unexpected EnvironmentEvent type "+ee);
        }

	return true;	

      } else if (e instanceof DynamicTuple) {
	if (e.hasField("msg") && "announce".equals(e.get("msg"))) {
          //Send announcement.
          announce(ANNOUNCE_NORMAL);
          return true;
	}

      } else if (e instanceof OutputResponse) {
        //Acknowledgement for our server announcement
	return true;

      } else if (e instanceof ExceptionalEvent) {
	  log.log(this,((ExceptionalEvent)e).x.toString());
      }

      return false;
    } // end handle1
   
    /**
     *  Send an announcement.
     *
     * @param type One of the ANNOUNCE_ static types
     */ 
    private void announce(int type) {
      AnnounceEvent ae;
      if (type == ANNOUNCE_NORMAL) {
        ae = 
          new AnnounceEvent(NullHandler.NULL, null, serverHandlerRef);
      } else {
        ae = 
          new AnnounceEvent(NullHandler.NULL, null, serverHandlerRef, type);
      }

      if (announceChannel != null) {
        announceChannel.handle(new SimpleOutputRequest(main,null,ae));
      } else {
        log.logWarning(this, "No announce channel");
      }
    }
  }


  // =======================================================================
  //                           The server handler
  // =======================================================================
  
  /** The server event handler. */
  final class ServerHandler extends AbstractHandler {
    
    /** Handle the specified event. */
    protected boolean handle1(Event e) {
      
      if (state != ACTIVE) {
	respond(e,new NotActiveException("Component is not active"));
	return true;
      }
      
      if (e instanceof RemoteEvent) {
	if (Constants.DEBUG_DISCOVERY) {
	  log.log(this, "Got RemoteEvent");
	}
	Event nestedEvent = ((RemoteEvent)e).event;      
        //newstEvent.metaData = e.metaData;	
	if (nestedEvent instanceof RemoteEvent) {
          nestedEvent.metaData = e.metaData;
          return doLateBinding((RemoteEvent)nestedEvent);
	} else if (nestedEvent instanceof ResolutionRequest) {
          return doEarlyBinding((ResolutionRequest)nestedEvent);
	} else if (nestedEvent instanceof BindingRequest) {
          return doBindingRequest((RemoteEvent)e,(BindingRequest)nestedEvent);
	} else if (nestedEvent instanceof ExceptionalEvent) {
          //This is a message from a client we sent a message to telling
          //us that this binding is no longer valid.  This can happen
          //if messages are lost or if the remote side crashes and restarts.
          ExceptionalEvent ee = (ExceptionalEvent)nestedEvent;
          if (ee.x instanceof UnknownResourceException){
            if (ee.closure instanceof Guid) {
              Guid bindingId = (Guid)ee.closure;
              killEntry(bindingId);
            } else {
              log.logWarning(this,"Unexpected UnknownResourceException "+ee.closure);
            }

            return true;
          } else {
            return false;
          } 
        }
      } else if (e instanceof LeaseEvent) {
        return doLeaseEvent((LeaseEvent)e);
      } else if (e instanceof BindingResponse) {
        return doBindingResponse((BindingResponse)e);
      } else if (e instanceof ExceptionalEvent) {
	ExceptionalEvent ee = (ExceptionalEvent)e;
	if (ee.x instanceof LeaseDeniedException) {
          RemoteEvent rev = (RemoteEvent)ee.closure;

          log.logWarning(this,"Discovery Server denied a client lease.");

	  respond(requestHandler,rev,serverHandlerRef,
		  new LeaseDeniedException("Server registration failed"));
	} else if (ee.x instanceof ConnectionFailedException) {
          if (Constants.DEBUG_DISCOVERY) {
            log.logWarning(this,"Failure sending: ",ee.x);
          }
        }
	return true;
      }
      return false;
    }

    void forwardMany(RemoteEvent re,ArrayList alist) {
      Iterator i = alist.iterator();  
      while(i.hasNext()) {
        Entry entry = (Entry)i.next();
        RemoteEvent newEvent = new RemoteEvent();

        newEvent.destination = entry.serviceRef;
        newEvent.source = this;
        newEvent.event = re.event;
        newEvent.datagram = re.datagram;

        //Set metaData so that REP can forward us UnknownResource events
        newEvent.setMetaData(Constants.DISCOVERY_SOURCE_SERVER, serverHandlerRef);
        newEvent.setMetaData(Constants.DISCOVERY_BINDING, entry.id);
        if (Constants.DEBUG_DISCOVERY) {
          log.log(this,"Sending to: "+newEvent.destination);
        }
        requestHandler.handle(newEvent);
      }
    }

    void forwardOne(RemoteEvent re, Entry entry) {
      RemoteReference destination;

      destination = entry.serviceRef;

      RemoteEvent newEvent = new RemoteEvent();

      newEvent.destination = entry.serviceRef;
      newEvent.source = this;
      newEvent.event = re.event;
      newEvent.datagram = re.datagram;

      //Set metaData so that REP can forward us UnknownResource events
      newEvent.setMetaData(Constants.DISCOVERY_SOURCE_SERVER, serverHandlerRef);
      newEvent.setMetaData(Constants.DISCOVERY_BINDING, entry.id);
      if (Constants.DEBUG_DISCOVERY) {
         log.log(this,"Sending to: "+newEvent.destination);
      }

      requestHandler.handle(newEvent);
    }

    /**
     * Handle a late binding request.
     *
     * @param re The remote event to send vi late-binding
     */
    boolean doLateBinding(RemoteEvent re) {
      ///////// Late-binding ///////////
      if (Constants.DEBUG_DISCOVERY) {
        log.log(this,"Got RemoteEvent (for late-binding)");
      }

      if (re.destination instanceof DiscoveredResource) {
        ArrayList matchList;
        Entry matchEntry;
        DiscoveredResource dr = (DiscoveredResource)re.destination;
        Query query           = dr.query;

        matchList = listeners.reverseMatchAll(re.event,null);

        if (null != matchList) {
          forwardMany(re,matchList);
        }

    
        if (!dr.matchAll) {  
          // Anycast.
          matchEntry = registry.matchOne(query);
 
          if (null == matchEntry) {
            matchEntry = consumers.reverseMatchOne(re.event);
          } 
 	 
          if (null != matchEntry)  {
            forwardOne(re,matchEntry);
          } else {
            respond(requestHandler,re,serverHandlerRef,
               new UnknownResourceException("No matching service found for " 
                                            + dr.query));
          }
        } else { 
          // Multicast.
          matchList = registry.matchAll(query,null);
          matchList = consumers.reverseMatchAll(re.event,matchList);

          if (null != matchList) { 
            forwardMany(re,matchList);
          } else {
            respond(requestHandler,re,serverHandlerRef,
               new UnknownResourceException("No matching service found for " 
                                            + dr.query));
          }
        }
 
        return true;
      }
      return false;
    }

    /**
     * Service an early binding request.
     *
     * @param request The early binding request
     */
    boolean doEarlyBinding(ResolutionRequest request) {
      ///////////// Lookup ///////////////
      if (Constants.DEBUG_DISCOVERY) {
        log.log(this,"Got ResolutionRequest");  
      }

      if (request.resource instanceof DiscoveredResource) {
        DiscoveredResource dr = (DiscoveredResource)request.resource;
        ResolutionResponse response;
        
        if (false == dr.matchAll) { 
          // Match one.
          Entry entry = registry.matchOne(dr.query);
          if (null == entry) {
            respond(requestHandler,request,serverHandlerRef,
                    new UnknownResourceException(
                         "No matching service found for " + dr.query));
            return true;
          }
          response = new ResolutionResponse(serverHandlerRef,
                                            null,
                                            entry.serviceDescriptor,
                                            entry.serviceRef);
                                                
        } else { 
          // Match all. 
          ArrayList result = registry.matchAll(dr.query,null);

          if (null == result) {
            respond(requestHandler,request,serverHandlerRef,
                    new UnknownResourceException(
                         "No matching service found for " + dr.query));
          }

          Tuple[] descriptors = new Tuple[result.size()];

          for(int i = 0; i < result.size(); i++) {
            descriptors[i] = ((Entry)result.get(i)).serviceDescriptor;
          }

          SymbolicHandler[] resources = new SymbolicHandler[result.size()];

          for(int i = 0; i < result.size(); i++) {
            resources[i] = ((Entry)result.get(i)).serviceRef;
          }

          response = new ResolutionResponse(serverHandlerRef,
                                            null,
                                            descriptors,
                                            resources);
        }

        if (Constants.DEBUG_DISCOVERY) {
          log.log(this,"Sending resolution response to " + request.source);
        }
        
        respond(requestHandler,request,response);            
        return true;
      }
      return false;
    }

    /**
     * Handle a binding request from the client
     *
     * @param e  The wrapping remote event
     * @param br The binding request
     */
    boolean doBindingRequest(RemoteEvent e,BindingRequest br) {
      ////////// Service registration //////////// 
      LeaseCancellationHandler lch;
      LeaseEvent le;
      ServerClosure sc;

      if (Constants.DEBUG_DISCOVERY) {
        log.log(this,"Got BindingRequest (service registration)");
      }

      if (br.descriptor instanceof RemoteDescriptor) {
        RemoteDescriptor rd = (RemoteDescriptor)br.descriptor;


        // Request a local lease.

        sc  = new ServerClosure(br, null, null, 0, new Guid(), e);
        lch = new LeaseCancellationHandler(rd.id,sc.entryNonce);
        le  = new LeaseEvent(this, sc, LeaseEvent.ACQUIRE,
                             lch, rd.descriptor, br.duration);

        lease.handle(le);
        return true;
      }
      return false;
    }

    /**
     * Handle a lease event for a lease on a binding
     *
     * @param le The lease event.
     */
    boolean doLeaseEvent(LeaseEvent le) {
      // Receive the local lease.
                
      if (le.closure instanceof ServerClosure) { 
        ServerClosure sc       = (ServerClosure)le.closure;
        BindingRequest bindreq = (BindingRequest)sc.br;
        RemoteDescriptor ed    = (RemoteDescriptor)bindreq.descriptor;
          
        //Finish up a registration.
        if (LeaseEvent.ACQUIRED == le.type) {            
          if (Constants.DEBUG_DISCOVERY) {
            log.log(this,"Got local ACQUIRED LeaseEvent, " + 
                    "Lease granted for " + le.duration);
          }

          EventHandler discoveryLease = new DiscoveryLeaseHandler(ed.id,
                                                                  sc.entryNonce,
                                                                  le.handler);
          sc.lease = le.handler;
          sc.discoveryLease = discoveryLease;
          sc.duration = le.duration;
            
          //Export the lease handler, 
          //passing along the original binding request.
          BindingRequest exportreq = 
            new BindingRequest(this,
                               sc,
                               new RemoteDescriptor(discoveryLease),
                               Duration.FOREVER);

          requestHandler.handle(exportreq);

          return true;

        } else if (LeaseEvent.CANCELED == le.type) {
          return true;
        }
      } else {
        return true;
      }
      return false; 
    }

    /**
     * Handle the response for exporting a binding lease handler.
     *
     * @param response the BindingResponse for the lease
     */ 
    boolean doBindingResponse(BindingResponse response) {
      // Receive the response from exporting the lease handler.
      if (Constants.DEBUG_DISCOVERY) {
          log.log(this,"Got local BindingResponse");
      }

      ServerClosure sc = (ServerClosure)response.closure;  

      RemoteEvent evIn       = (RemoteEvent)sc.origEvent;
      BindingRequest bindreq = sc.br;
      RemoteDescriptor responseDescriptor 
        = (RemoteDescriptor)response.descriptor;
      RemoteDescriptor ed    = (RemoteDescriptor)bindreq.descriptor;

      //Make the entry.
      Entry entry             = new Entry();

      entry.id                = ed.id;
      entry.lease             = sc.lease;
      entry.duration          = sc.duration;
      entry.serviceDescriptor = ed.descriptor;
      entry.snoop             = ed.snoop;
      entry.serviceRef        = (RemoteReference)ed.handler;
      entry.discoveryLease    = sc.discoveryLease;
      entry.entryNonce        = sc.entryNonce;

      // Add things to the entry.
      entry.discoveryLeaseRef = (RemoteReference)response.resource;
      entry.discoveryLeaseRefMaintainer = 
        new LeaseMaintainer(response.lease,
                            response.duration,main,null,timer);
        
      // Add the entry to the registry.
      if (Constants.DEBUG_DISCOVERY) {
        log.log(this,"Adding service to the registry: \""
                +  entry.serviceDescriptor + "\"");
      }
        
      while(true) {
        boolean result;

        if (entry.serviceDescriptor instanceof Query) {
          if (entry.snoop) {
            result = listeners.addEntry(entry);
          } else {
            result = consumers.addEntry(entry);
          }
        } else {
          result = registry.addEntry(entry);
        }

        //Added to the registry
        if (result) {
          //Successfully added
          break;
        } else {
          //There is already a binding for this client registration, remove it.
          killEntry(entry.id);
        };
      }
        
      // Send the response.
      respond(requestHandler, evIn.event,
              new BindingResponse(serverHandlerRef,
                                  bindreq.closure,
                                  entry.serviceDescriptor,
                                  entry.serviceRef,
                                  entry.discoveryLeaseRef,
                                  entry.duration));

      if (Constants.DEBUG_DISCOVERY) {
        SystemUtilities.debug("\nRegistry contents:");
        registry.print(); //debug
        SystemUtilities.debug("\nSnooper contents:");
        listeners.print(); //debug
        SystemUtilities.debug("\nConsumer contents:");
        consumers.print(); //debug
      }
      return true;
    }

  }

  

  /** 
   * Handles lease cancellations by removing registry entries. 
   *
   * This is the handler passed to the LeaseManager to be called
   * when our lease expires.
   */
  class LeaseCancellationHandler extends AbstractHandler {

    /** The id of the Entry controlled by this handler */
    Guid id;

    /** The nonce for this registration */
    Guid entryNonce;

    /** 
     * Creates a new LeaseCancellationHandler with the specified
     * RemoteDescriptor.
     *
     * @param descriptor  The remote descriptor for the registry entry.
     */
    public LeaseCancellationHandler(Guid id, Guid entryNonce) {
      this.id = id;
      this.entryNonce = entryNonce;
    }

    /** Handles events. */
    protected boolean handle1(Event e) {

      if (e instanceof LeaseEvent) {
        LeaseEvent le = (LeaseEvent) e;

	if (le.type == LeaseEvent.CANCELED) {
          Entry entry;

	  if (Constants.DEBUG_DISCOVERY) {
	    log.log(this,"Got CANCELED local LeaseEvent");
	  }
            
          if (listeners.contains(id)) {
            //Remove entry from listeners.
            if (Constants.DEBUG_DISCOVERY) {
	      log.log(this,"Removing entry from listen");
            }
	    entry = listeners.removeEntry(id,entryNonce);
          } else if (consumers.contains(id)) {
            //Remove entry from consumers.
            if (Constants.DEBUG_DISCOVERY) {
	      log.log(this,"Removing entry from listen");
            }
	    entry = consumers.removeEntry(id,entryNonce);
          } else {
            //Remove entry from registry.
            if (Constants.DEBUG_DISCOVERY) {
	      log.log(this,"Removing entry from normal");
            }
	    entry = registry.removeEntry(id,entryNonce);
          }

          if (entry != null) {
            entry.discoveryLeaseRefMaintainer.cancel();
	    if (Constants.DEBUG_DISCOVERY) {
	      log.log(this,"Removed entry from registry");
              SystemUtilities.debug("\nRegistry contents:");
	      registry.print();
              SystemUtilities.debug("\nSnooper contents:");
	      listeners.print();
              SystemUtilities.debug("\nConsumer contents:");
	      listeners.print();
	    }
          }
	  return true;
        }
      }
      return false;
    } 

    /** Returns a string representing this lease cancellation handler. */
    public String toString() {
      return super.toString() + "(" + id + ")";
    }
  }
  



  // =======================================================================
  //                           Constructor
  // =======================================================================

  /**
   * Create a new instance of <code>DiscoveryServer</code>.
   *
   * @param  env    The environment for the new instance.
   */
  public DiscoveryServer(Environment env) {
    super(env);

    // Obtain a timer component.
    this.timer = getTimer();

    // Create handlers
    server = new ServerHandler();
    mainHandler = new MainHandler();
    main = declareExported(MAIN, mainHandler);
    requestHandler = declareImported(REQUEST);
    lease = declareImported(LEASE);
  }

  // =======================================================================
  //                           Component support
  // =======================================================================

  /** Get the component descriptor. */
  public ComponentDescriptor getDescriptor() {
    return (ComponentDescriptor)SELF.clone();
  }

  /** 
   * Handles the ACTIVATE environment event.  Synchronously obtains a
   * remote reference for the server handler and then sends a binding
   * request for a multicast channel. 
   */
  void activate() {

    synchronized (lock) {
      if (state != INACTIVE) {
        return;
      }
      state = ACTIVATING;
    }

    // Check that the necessary imported event handlers are linked.
    if (!lease.isLinked()) {
      log.logError(this, "Imported event handler lease not linked");
      stop();
      return;
    }

    if (!requestHandler.isLinked()) {
      log.logError(this, "Imported event handler request not linked");
      stop();
      return;
    }

    //export the server handler
    BindingRequest exportreq = 
      new BindingRequest(main,null,
			 new RemoteDescriptor(server),Duration.FOREVER);
    Event response;
    try {
      response = Synchronous.invoke(requestHandler,exportreq,
				    Constants.SYNCHRONOUS_TIMEOUT);   
    } 
    catch (TimeOutException x) {
      log.logError(this, "Could not export server handler", x);
      stop();
      return;
    }
    if (response instanceof BindingResponse) {
      BindingResponse br = (BindingResponse)response;
      serverHandlerRef = (RemoteReference)br.resource;
      serverRefMaintainer = new LeaseMaintainer(br.lease,br.duration,
						main,null,timer);
    } 
    else if (response instanceof ExceptionalEvent) {
      ExceptionalEvent ee = (ExceptionalEvent)response;
      if (ee.x instanceof AlreadyBoundException) {
	//This shouldn't happen
	log.logError(this, "Could not export server handler", ee.x);
	stop();
	return;
      } 
    }


    //bind DatagramIO input channel to send announcements
    SioResource sio = new SioResource();
    sio.remoteHost = Constants.DISCOVERY_ANNOUNCE_ADDR;
    sio.remotePort = Constants.DISCOVERY_ANNOUNCE_PORT;
    sio.type       = SioResource.MULTICAST;

    BindingRequest bindreq = 
      new BindingRequest(main,null,sio,Duration.FOREVER);
    
    try {
      response = Synchronous.invoke(requestHandler,bindreq,
				    Constants.SYNCHRONOUS_TIMEOUT);   
    } 
    catch (TimeOutException x) {
      log.logError(this, "Could not bind datagram channel handler", x);
      stop();
      return;
    }
    if (response instanceof BindingResponse) {
      BindingResponse br = (BindingResponse)response;
      announceChannel = br.resource;
      announceMaintainer = 
	new LeaseMaintainer(br.lease,br.duration,main,null,timer);
    } 
    else if (response instanceof ExceptionalEvent) {
      ExceptionalEvent ee = (ExceptionalEvent)response;
      if (ee.x instanceof AlreadyBoundException) {
	//This shouldn't happen
	log.logError(this, "Could not bind remote reference", ee.x);
	stop();
	return;
      } 
    }
    
    //setup timer so announcements can be sent
    DynamicTuple dt = new DynamicTuple();
    dt.set("msg","announce");
    synchronized(lock) {
      state = ACTIVE;
    }
    announceTimerNotification =
        timer.schedule(Timer.FIXED_RATE,
	               SystemUtilities.currentTimeMillis()
		           + Constants.DISCOVERY_ANNOUNCE_PERIOD,
                       Constants.DISCOVERY_ANNOUNCE_PERIOD-20,
		       main,
		       dt);
    mainHandler.announce(ANNOUNCE_NORMAL);


    log.log(this,"DiscoveryServer activated");
  }
  
  /** 
   * Stops the execution of the component. Releases any held resources.
   */
  void stop() {
    log.log(this,"Stopping DiscoveryServer");
    state = INACTIVE;

    Event response;
    mainHandler.announce(ANNOUNCE_CLOSED);

    if (null != announceMaintainer) {
      announceMaintainer.cancel();
    }

    if (null != serverRefMaintainer) {
      serverRefMaintainer.cancel();
    }
    
    if (null != announceTimerNotification) {
      announceTimerNotification.cancel();
    }

    Entry[] allEntries = registry.allEntries();
    int i;

    for (i = 0 ; i < allEntries.length; i++) {
      if (null != allEntries[i].discoveryLease) {
        LeaseEvent le = new LeaseEvent(nullReturn, null, LeaseEvent.CANCEL, null, null, 0);
        allEntries[i].discoveryLease.handle(le);
      }

      if (null != allEntries[i].discoveryLeaseRefMaintainer) {
        allEntries[i].discoveryLeaseRefMaintainer.cancel();
      }
    }

    allEntries = listeners.allEntries();

    for (i = 0 ; i < allEntries.length; i++) {
      if (null != allEntries[i].discoveryLease) {
        LeaseEvent le = new LeaseEvent(nullReturn, null, LeaseEvent.CANCEL, null, null, 0);
        allEntries[i].discoveryLease.handle(le);
      }

      if (null != allEntries[i].discoveryLeaseRefMaintainer) {
        allEntries[i].discoveryLeaseRefMaintainer.cancel();
      }
    }

    allEntries = consumers.allEntries();

    for (i = 0 ; i < allEntries.length; i++) {
      if (null != allEntries[i].discoveryLease) {
        LeaseEvent le = new LeaseEvent(nullReturn, null, LeaseEvent.CANCEL, null, null, 0);
        allEntries[i].discoveryLease.handle(le);
      }

      if (null != allEntries[i].discoveryLeaseRefMaintainer) {
        allEntries[i].discoveryLeaseRefMaintainer.cancel();
      }
    }


    if (Constants.DEBUG_DISCOVERY) {
      log.log(this,"Leaving stop()");
    }
  }

  /** 
   * The ElectionManager is telling us that we will be shut down soon.
   * Send an ANNOUNCE_CLOSING message.
   */
  void prepareToDie() {
    if (null != announceTimerNotification) {
      announceTimerNotification.cancel();
    }
    mainHandler.announce(ANNOUNCE_CLOSING);
  }

  // =======================================================================
  //                           The registry entry class
  // =======================================================================

  /** 
   * This class implements the entry used to populate the service
   * registry.  
   */
  private static class Entry {
    
    /** The tuple describing the service. */
    public Tuple serviceDescriptor;
    
    /** The remote reference to the event handler for the service. */
    public RemoteReference serviceRef;

    /** The event handler for the lease. */
    public EventHandler lease;

    /** The discovery lease handler. */
    public EventHandler discoveryLease;

    /** The duration of the export */
    public long duration;
    
    /** The remote reference to the handler. */
    public RemoteReference discoveryLeaseRef;

    /** The lease maintainer for the discovery lease handler export. */
    public LeaseMaintainer discoveryLeaseRefMaintainer;

    /** Is the snoop flag set */
    boolean snoop;

    /** The id of this binding */
    Guid id;

    /** 
     * The nonce for this binding.  Each time we receive a 
     * BindingRequest from a client, we execute it under a
     * under a new entryNonce.  This lets us disambiguate 
     * old/new events.
     */
    Guid entryNonce;

    public Entry() {
      //Do nothing.
    }
  } 

  /**
   * This closure is used while handling a binding request to keep 
   * track of information aquired so far.
   */
  private static class ServerClosure extends Tuple{
    /** The raw lease for this binding */
    public EventHandler lease;

    /** The duration of the raw lease */
    public long duration;

    /** The lease wrapped in a DiscoveryLeaseHandler. */
    public EventHandler discoveryLease;
 
    /** The original binding request. */
    public BindingRequest br; 

    /** 
     * The nonce for this binding.  Each time we receive a 
     * BindingRequest from a client, we execute it under a
     * under a new entryNonce.  This lets us disambiguate 
     * old/new events.
     */
    public Guid entryNonce;

    /**
     * The original RemoteEvent that held the bindingRequest.
     */
    public Event origEvent;
  
    public ServerClosure() { }

    public ServerClosure(BindingRequest br, 
                         EventHandler lease,
                         EventHandler discoveryLease, 
                         long duration,
                         Guid entryNonce,
                         Event origEvent) {
      this.discoveryLease = discoveryLease;
      this.lease          = lease;
      this.duration       = duration;
      this.br             = br;
      this.entryNonce     = entryNonce;
      this.origEvent      = origEvent;
    }
  }

  // =======================================================================
  //                   The discovery lease handler class
  // =======================================================================

  
  /** 
   * This wraps a lease for a resources so that it can be accessed
   * remotely.  This handler should be exported and it's remote 
   * reference may be passed to remote nodes.
   *
   * This lease handler accepts {@link one.world.rep.RemoteEvent}s 
   * and if the encapulated event is a {@link one.world.binding.LeaseEvent}, 
   * passes the event on to the local lease handler for the lease.  
   * It will also accept unwrapped events. If the * <code>LeaseEvent</code> 
   * is of type {@link one.world.binding.LeaseEvent#CANCELED}, 
   * the corresponding registry entry will be removed. 
   */
  final class DiscoveryLeaseHandler extends AbstractHandler {

    /** The registry entry for the lease. */ 
    //private Entry entry;

    /** The local lease handler. */
    private EventHandler leaseHandler;

    Guid id;
    Guid entryNonce;

    /**
     * Create a new discovery lease handler.
     *
     * @param entry The server registry entry for the lease.
     * @param leaseHandler The local lease handler.
     */
    DiscoveryLeaseHandler(Guid id, Guid entryNonce, EventHandler leaseHandler) {
      this.leaseHandler = leaseHandler;
      this.entryNonce   = entryNonce;
      this.id           = id;
    }

    /** Handle the specified event. */
    protected boolean handle1(Event e) {
      Entry entry;
      Event ev;
      EventHandler source;

      if (e instanceof RemoteEvent) {
	RemoteEvent re = (RemoteEvent)e;
        ev = re.event;
      } else {
        ev = e;
      }
      source = ev.source;

      if (ev instanceof LeaseEvent) {
        if (Constants.DEBUG_DISCOVERY) {
          log.log(this,"Got LeaseEvent for nonce "+entryNonce); 
        }
        LeaseEvent le = (LeaseEvent)ev;

        if (listeners.contains(id)) {
          entry = listeners.get(id,entryNonce);
        } else if (consumers.contains(id)) {
          entry = consumers.get(id,entryNonce);
        } else {
          entry = registry.get(id,entryNonce);
        }

        if (entry==null) {
          if (Constants.DEBUG_DISCOVERY) {
            log.log(this,"Didn't find entry");
          }
          le.type = LeaseEvent.CANCEL;
          le.source = nullReturn;
          leaseHandler.handle(le);	  
          return true;
        }
	  
        Event response = Synchronous.invoke(leaseHandler,le);	  
       
        if (e instanceof RemoteEvent) { 
          if (response instanceof ExceptionalEvent) {
            response.source = entry.discoveryLeaseRef;
            requestHandler.handle(new RemoteEvent(this,null,
                                                  (RemoteReference)source,
                                                  response));
          } else if (response instanceof LeaseEvent) {
            response.source = entry.discoveryLeaseRef;
            requestHandler.handle(new RemoteEvent(this,null,
                                                  (RemoteReference)source,
                                                  response));
          }
        } else {
          if (response instanceof ExceptionalEvent) {
            e.source = source;
            response.source = this;
            respond(e,response);
          } else if (response instanceof LeaseEvent) {
            e.source = source;
            response.source = this;
            respond(e,response);
          }
        }
        return true;
      } else if (e instanceof ExceptionalEvent) {
        if (Constants.DEBUG_DISCOVERY) {
          log.log(this,"Exceptional Event "+e);
        }
      }

      return false;
    } 

  }

  /**
   * Remove the Entry with the specified id and free all resources.
   *
   * This is meant as a way to kill Entries in "unusual" circumstances
   * including receiving notification of an UnknownResourceException for
   * this handler or a double registration of two Entries with the
   * same id.
   */
  void killEntry(Guid id) {
    //Remove entry from registry.
    Entry old;  
 
    if (listeners.contains(id)) {
      old = listeners.removeEntry(id,null);
    } else if (consumers.contains(id)) {
      old = consumers.removeEntry(id,null);
    } else {
      old = registry.removeEntry(id,null);
    }

    if (old!= null) {
      if (Constants.DEBUG_DISCOVERY) {
        log.log(this,"Removing "+id+
                     " due to delivery problem.");
      }
      LeaseEvent le = new LeaseEvent(nullReturn,null,LeaseEvent.CANCEL,null,null,0);
      old.lease.handle(le);
      old.discoveryLeaseRefMaintainer.cancel();
    } else {
      if (Constants.DEBUG_DISCOVERY) {
        log.log(this,"Couldn't find "+id+
                     " to remove due to delivery problem.");
      }
    }
  }

  // =======================================================================
  //                           Registry support
  // =======================================================================
     

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


  /**
   * The registry holding all of the client mappings.
   */
  static final class Registry {
    /**
     * The head of the list.
     */
    Element head = new Element(null);

    /**
     * Contains an ID -> Elememnt mapping with a 
     * pointer to the Element
     *
     * Used as the main modification synchronization for
     * the list.
     *
     * See Element.aquireWrite() for the locking rules.
     */
    HashMap hmap = new HashMap();

    /**
     * Contains a RemoteResource -> Element mapping with
     * a pointer to the element containing this resource.
     */
    HashMap elements = new HashMap();

    /**
     * The set of Entries sharing a single resource
     */
    static final class Element {
      /** 
       * Returned to the caller on every lookup.  Must be remade when we
       * add or remove an entry.
       */
      public Entry[] entries;
      
      /** 
       * The resource of all the entries in this element. 
       */
      public Tuple resource;


      /**
       * The filter version of the query.
       */
      public TupleFilter filter;

      /**
       * The next element in the list.
       */
      public Element next;

      /**
       * The previous element in the list.
       */
      public Element prev;

      /**
       * Make a new element with a given entry.
       *
       * Automatically fills in the resource and the entries array.
       *
       * @param entry The entry to make a new Element for. null for the head.
       */
      Element(Entry entry) {
        if (null != entry) {
          this.resource   = entry.serviceDescriptor; 
          this.entries    = new Entry[1];
          this.entries[0] = entry;

          if (this.resource instanceof Query) {
            filter = new TupleFilter((Query)(this.resource));
          }
        }
      } 

      /**
       * Adds an entry into this element.  Must be called while holding a write
       * lock for this element.
       * 
       * @param entry The entry to add into this element.
       */
      void addInto(Entry entry) {
        Entry[] newEntries;
     
        newEntries = new Entry[entries.length+1]; 
        System.arraycopy(entries,0,newEntries,0,entries.length);
        newEntries[entries.length] = entry;

        entries = newEntries;
      }

      /**
       * Remove the entry specified by id from this element.
       *
       * Must be called while holding a write lock on the element.
       *
       * @param id The id of the Entry to remove
       * @return The Element removed(null if none).
       */
      Entry removeFrom(Guid id) {
        int found;
        int i;
        Entry retVal;

        for (i=0;i<entries.length;i++) {  
          if (entries[i].id.equals(id)) {
            found = i;
            retVal = entries[found];
            if (entries.length>1) {
              Entry[] newEntries = new Entry[entries.length-1];
            
              System.arraycopy(entries,0,
                               newEntries,0,found);
              System.arraycopy(entries,found+1,
                               newEntries,found,entries.length-found-1);
              entries = newEntries;
            } else {
              entries = new Entry[0];
            }
            return retVal;
          }
        }
        return null;
      }

      /**
       * Return the object specified by id from the element. 
       *
       * Must be called while holding a read lock on the element.
       */
      Entry getFrom(Guid id) {
        int found;
        int i;

        for (i=0;i<entries.length;i++) {  
          if (entries[i].id.equals(id)) {
            return entries[i];
          }
        }
        return null;
      }

      /**
       * Get any entry from this element.
       *
       * Must be called while holding a read lock on the element.
       *
       * @return The entry returned.
       */
      Entry getOne() {
        return entries[0];
      }

      /**
       * Return an array of all entries in this element.
       * This array must NOT be modified.
       *
       * Must be called while holding a read lock on the element.
       * 
       * @return The array of entries in this element.
       */
      Entry[] getAll() {
        return entries;
      }

      /**
       * Return the number of entries in this element.
       *
       * Must be called while holding a read lock on the element.
       *
       * @return The number of entries in this element.
       */
      int numEntry() {
        return entries.length;
      }
     
      /** The number of readers currently executing */
      int numReaderRun;
 
      /** The number of writers currently executing */
      int numWriterRun;

      /** The number of writers executing or waiting */
      int numWriters;

      /** The number of readers executing or waiting */
      int numReaders; 

      /**
       * Get a read lock on the "next" pointer of an element.  We can have
       * multiple readers.
       *
       * See aquireWrite for the locking rules.
       */
      void aquireRead() {
        synchronized(this) {
          numReaders++;
          while(0 != numWriters) {
            try {
              wait();
            } catch (InterruptedException x) {

            }
          }  
          numReaderRun++;
        }
      }
  
      /**
       * Free a read lock on this element.
       */
      void freeRead() {
        synchronized(this) {
          numReaders--;
          numReaderRun--;
          if (0 != (numReaders+numWriters)) {
            notifyAll();   
          }     
        }
      }
  
      /**
       * Get a modify lock on the "next" pointer of an element.  We can have
       * only a single writer.  Blocks till all other readers/modifiers are
       * gone.
       *
       * Concurrently held locks MUST be obtained in the following order: 
       *
       *
       * Outside of an hmap block:
       *  Read locks must be obtained in ascending order.  
       *  Write locks may not be obtained outside hmap blocks.
       *
       * Inside of an hmap block:
       *  Read locks may be obtained in any order.
       *  Write locks must be obtained in ascending order.
       *  Read locks may be held when exiting an hmap block.
       *  Write locks must be dropped before exiting an hmap block.
       */
      void aquireWrite() {
        synchronized(this) {
          numWriters++;
          while (0 != (numWriterRun+numReaderRun)) {
            try {
              wait();
            } catch (InterruptedException x) {

            }
          }
          numWriterRun++;
        }
      }

      /**
       * Free a write lock on this element.
       */  
      void freeWrite() {
        synchronized(this) {
          numWriters--;
          numWriterRun--;
          if (0!=(numWriters+numReaders)) {
            notifyAll(); 
          }
        }
      }
    }  

    /**
     * An iterator-like construct that iterates through the
     * elements while obtaining necessary read locks.
     */
    protected final class ReadIterator {
      /**
       * The last element returned. A read lock is held
       * on this element.
       */
      Element curElement;

      /**
       * Make a new iterator at the beginning of the ist.
       */
      public ReadIterator() 
      {
        head.aquireRead();
        curElement = head;
      };
 
      /**
       * Return the next element in the list, throwing an exception if none.
       *
       * This takes care of obtaining a read lock on the element returned.
       * This read lock is freed the next time nextElement() is called or
       * when kill() is called.
       *
       * @return The next element.
       */
      public Element nextElement() {
        if (null == curElement.next) {
          //No more elements.
          curElement.freeRead(); 
          curElement = null;

          return null;
        } else {
          //We have another element.
          Element oldRead;

          curElement.next.aquireRead();

          oldRead    = curElement;
          curElement = curElement.next;

          oldRead.freeRead();

          return curElement;          
        }
      };

      /**
       * Kill the iterator.  This frees all locks held by the iterator.
       */
      public void kill() {
        if (curElement != null) {
          curElement.freeRead();
          curElement = null;
        }
      }
    }

    /**
     * Add an entry to the list.  If there is an existing Element
     * with this resource, add it to that element, otherwise create
     * a new element.
     *
     * @param entry The entry to add.
     * @return true if successful, false if already in the list.
     */
    private boolean addEntry(Entry entry) {
      Element el   = null;

      synchronized(hmap) {
        if (hmap.containsKey(entry.id)) {
          return false;
        }
        
        el = (Element)elements.get(entry.serviceDescriptor);

        if (null == el) {
          //No element to add it into.
          head.aquireWrite();

          if (head.next != null) {
            head.next.aquireWrite();
          }

          try {
            el        = new Element(entry);
            el.aquireWrite();
            el.next   = head.next;
            el.prev   = head;

            if (el.next != null) {
              el.next.prev = el;
            }

            head.next = el;

            hmap.put(entry.id,el);
            elements.put(entry.serviceDescriptor,el);

            return true;
          } finally {
            head.freeWrite();
            if (el != null) {
              el.freeWrite();
              if (el.next != null) {
                el.next.freeWrite();
              }
            } 
          }
        } else {
          try {
            el.aquireWrite();
            el.addInto(entry);
            hmap.put(entry.id,el);
            return true;
          } finally {
            el.freeWrite();
          }
        }

      }
    }

    /**
     * Remove a Entry element.  Removes the mapping in both 
     * hash maps, the entry within the Element, and the Element itself
     * if empty.
     *
     * @param id The id of the entry to remove.
     * @param entryNonce The nonce of the entry to remove(null if
     *                   we don't care)
     * @return The entry removed.
     */ 
    private Entry removeEntry(Guid id,Guid entryNonce) {
      Entry retVal;

      synchronized(hmap) {
        Element el = (Element)hmap.get(id);
 
        if (null == el) {
          return null; 
        } else {
          Element prev = el.prev;
          Element next = el.next;

          prev.aquireWrite();
          el.aquireWrite();

          if (next != null) {
            next.aquireWrite();
          }

          try {
            if (null != entryNonce) {
              Entry entry = el.getFrom(id);
              if (!(entry.entryNonce.equals(entryNonce))) {
                return null; 
              }
            }            
            hmap.remove(id);

            retVal = el.removeFrom(id);

            if (0 == el.numEntry()) {
              elements.remove(el.resource);
              prev.next = prev.next.next;

              if (next!=null) {
                next.prev = prev;
              }
            }
            return retVal;
          } finally {
            prev.freeWrite();
            el.freeWrite();
            if (next!=null) {
              next.freeWrite();
            }
          } 
        }
      }
    }

    /**
     * Does the list contain an element with this id.
     *
     * @param id The id of the element to look for.
     * @return true if the element is in the list, false otherwise.
     */
    private boolean contains(Guid id) {
      synchronized(hmap) {
        return hmap.containsKey(id);
      }
    }

    /**
     * Search the list for an element with a given id and nonce.
     *
     * @param id The id of the element to look for.
     * @param entryNonce the nonce to look for(null if don't care).
     * @return The entry if found, null otherwise.
     */
    private Entry get(Guid id,Guid entryNonce) {
      Element el;

      synchronized(hmap) {
        el = (Element)hmap.get(id);

        if (null == el) {
          return null;
        }

        el.aquireRead();
      }

      try {
        Entry entry = el.getFrom(id);
 
        if (entry == null) {
          throw new Bug("Shouldn't be null");
        }

        if ((null == entryNonce) || 
            (entryNonce.equals(entry.entryNonce))) {
          return entry;           
        } else {
          return null;
        }
      } finally {
        el.freeRead();
      }
    } 


    /** 
     * Returns a RemoteReference to the first service in the registry
     * which matches the specified query.  Because this operation is
     * not <code>synchronized</code> on the registry Object, a service
     * description that was added to the list after the operation has
     * started may not be returned.
     *
     * @param  query  The query to match.
     *
     * @return A RemoteReference to the first matching service if
     * one exists, <code>null</code> otherwise.  
     */
    Entry matchOne(Query q) {
      TupleFilter filter = new TupleFilter(q);
      ReadIterator rit = null;
      
      try {
        rit = new ReadIterator();

        while(true) {
          Element e = rit.nextElement();
          if (null == e) {
            return null;
          }

	  if (filter.check(e.resource)) {
	    return e.getOne();
	  }
        }
      } finally {
        if (null != rit) {
          rit.kill(); 
        }
      }
    }
    
 
    /** 
     * Returns an array of Entry objects for all the services which
     * match the specified query.
     *
     * @param  q  The query to match.
     * @param  array An arraylist to store results in.  if null, will create
     *
     * @return An ArrayList containing RemoteReferences to matching
     * services if they exist, <code>null</code> otherwise.  */
    ArrayList matchAll(Query q,ArrayList array) {
      int i;

      TupleFilter filter = new TupleFilter(q);
      ReadIterator rit   = null;

      
      try {
        rit = new ReadIterator();

        while(true) {
          Element e = rit.nextElement();

          if (null == e) {
            return array;
          }

	  if (filter.check(e.resource)) {
	    Entry[] el = e.getAll();

            if (null == array) {
              array = new ArrayList();
            }

            for (i=0; i<el.length;i++) {
              array.add(el[i]);
            }
	  }
        }
      } finally {
        if (null != rit) {
          rit.kill(); 
        }
      }

    }

    /** 
     * Returns an array of Entry objects for all the registrations whose
     * quries match the specified event.  
     *
     * @param  ev  The event to match against all queries.
     *
     * @return An ArrayList containing RemoteReferences to matching
     * services if they exist, <code>null</code> otherwise.  */
    ArrayList reverseMatchAll(Event ev,ArrayList array) {
      int i;

      ReadIterator rit   = null;
      
      try {
        rit = new ReadIterator();

        while(true) {
          Element e = rit.nextElement();
          if (null == e) {
            return array;
          }
	  if (e.filter.check(ev)) {
	    Entry[] el = e.getAll();
            if (null == array) {
              array    = new ArrayList();
            }
            for (i=0; i<el.length;i++) {
              array.add(el[i]);
            }
	  }
        }
      } finally {
        if (null != rit) {
          rit.kill(); 
        }
      }
    }

    /** 
     * Returns an array of Entry objects for all the registrations whose
     * quries match the specified event.  
     *
     * @param  ev  The event to match against all queries.
     *
     * @return An ArrayList containing RemoteReferences to matching
     * services if they exist, <code>null</code> otherwise.  */
    Entry reverseMatchOne(Event ev) {
      int i;

      ReadIterator rit   = null;
      
      try {
        rit = new ReadIterator();

        while(true) {
          Element e = rit.nextElement();
          if (null == e) {
            return null;
          }
	  if (e.filter.check(ev)) {
            return e.getOne();
	  }
        }
      } finally {
        if (null != rit) {
          rit.kill(); 
        }
      }
    }

    /**
     * Return an array of all of the entries in the Registry
     *
     * @return all of the entries(empty list if none) 
     */
    public Entry[] allEntries() {
      ArrayList all = matchAll(new Query(),null);
      if (all!=null) {
        return (Entry[]) all.toArray(new Entry[0]);
      } else {
        return new Entry[0];
      }
    }

    /** 
     * Prints the contents of the registry.  The "name" field of the
     * descriptor for each registry entry is printed. This method is
     * intended for debug use.  
     */
    void print() {
      int i;
      ReadIterator rit = null;
 

      try {
        rit = new ReadIterator();

        while (true) {
          Element el = rit.nextElement();
          if (null == el) {
            return;
          }
	  SystemUtilities.debug("Element for "+el.resource);

          Entry[] entries = el.getAll();

          for (i=0;i<entries.length;i++) {
            SystemUtilities.debug(" Entry "+entries[i]);
          }      
        }
      } finally {
        if (null!=rit) {
          rit.kill();
        }
      }
    }

    /**
     * Make a Registry object.
     */
    Registry() {
    }

  }

}

