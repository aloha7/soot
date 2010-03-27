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

import one.world.Constants;

import one.world.core.Component;
import one.world.core.Environment;
import one.world.core.Event;
import one.world.core.EventHandler;
import one.world.core.ExceptionalEvent;
import one.world.core.Tuple;
import one.world.core.DynamicTuple;
import one.world.core.ComponentDescriptor;
import one.world.core.ExportedDescriptor;
import one.world.core.ImportedDescriptor;
import one.world.core.SymbolicHandler;
import one.world.core.InvalidTupleException;
import one.world.core.LinkingException;
import one.world.core.NoBufferSpaceException;
import one.world.core.TupleException;
import one.world.core.UnknownEventException;

import one.world.binding.BindingRequest;
import one.world.binding.BindingResponse;
import one.world.binding.Duration;
import one.world.binding.LeaseEvent;
import one.world.binding.LeaseMaintainer;
import one.world.binding.LeaseDeniedException;
import one.world.binding.LeaseRevokedException;
import one.world.binding.ResourceRevokedException;
import one.world.binding.UnknownResourceException;

import one.world.data.Name;

import one.world.env.EnvironmentEvent;

import one.world.io.SioResource;
import one.world.io.NetworkIO;
import one.world.io.Query;
import one.world.io.SimpleInputRequest;
import one.world.io.InputResponse;
import one.world.io.ListenResponse;
import one.world.io.SimpleOutputRequest;
import one.world.io.OutputResponse;

import one.world.util.AbstractHandler;
import one.world.util.HandlerApplication;
import one.world.util.Log;
import one.world.util.NullHandler;
import one.world.util.Operation;
import one.world.util.SystemUtilities;
import one.world.util.Synchronous;
import one.world.util.Timer;
import one.world.util.TimeOutException;

import one.util.Bug;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.NotSerializableException;

import java.net.UnknownHostException;
import java.net.InetAddress;

import java.security.AccessController;
import java.security.PrivilegedAction;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.StringTokenizer;

/**
 * A manager for remote event passing.  The 
 * <a href="#request">request</a> handler is used to export event
 * handlers, resolve resources, and send remote events.
 *
 * <p><b>Imported and Exported Event Handlers</b></p>
 *
 * <p>Imported handlers:
 * <dl>
 *   <dt>lease</dt>
 *   <dd>Handles {@link one.world.binding.LeaseEvent LeaseEvents}.</dd>
 *
 *   <dt>discovery</dt>
 *   <dd>The request handler for a discovery service.  This handler is
 *   expected to handle {@link one.world.binding.BindingRequest
 *   BindingRequests} where the descriptor is an 
 *   {@link RemoteDescriptor}, {@link ResolutionRequest}s
 *   for {@link DiscoveredResource DiscoveredResources}, and 
 *   as {@link RemoteEvent RemoteEvents} that are destined for discovered
 *   resources.  This handler need not be linked if discovery services 
 *   will not be used.</dd>
 *
 *   <dt>discoveryError</dt>
 *   <dd>Used to report delivery errors, in the form of the RemoteEvents
 *   that that cannot be delivered to unknown resources.</dd> 
 * </dl>
 * </p>
 *
 * <p>Exported handlers:
 * <dl>
 *   <dt>env</dt>
 *   <dd>Handles {@link one.world.env.EnvironmentEvent}s.  This is
 *   required to acquire and release bound resources.</dd> 
 *   
 *   <dt><a name="request">request</a></dt>
 *   <dd>The exported request handler is used to export event handlers, 
 *   resolve resources, and send remote events.  Therefore, it handles
 *   three types of events:
 *   <p>
 *   <ol>
 *      <li><p>{@link one.world.binding.BindingRequest BindingRequests} 
 *      are used to export
 *      event handlers.  The descriptor for the binding request should be
 *      an {@link RemoteDescriptor}.  If the operation is successful, the
 *      resulting {@link one.world.binding.BindingResponse} will contain a
 *      {@link RemoteReference} for the newly exported event handler.  The
 *      operation may fail with a {@link AlreadyBoundException}.</p></li>
 *
 *      <li><p>{@link ResolutionRequest}s are used to resolve remote 
 *      resources.  If resolution is successful, the response will be a
 *      {@link ResolutionResponse}.  Otherwise, the result
 *      will be an {@link one.world.binding.UnknownResourceException} or a 
 *      {@link ConnectionFailedException}.</p>
 *
 *      <p>Because resolution may require communication with a remote host,
 *      the source of a <code>ResolutionRequest</code> <emph>must</emph> be a 
 *      {@link RemoteReference} in order to receive a result.  An
 *      {@link one.world.core.InvalidTupleException} will be returned if
 *      this is not the case.
 *      </p></li>
 *
 *      <li><p>{@link RemoteEvent RemoteEvents} 
 *      are used to send events to a remote event
 *      handler.  A failure may be indicated with an
 *      <code>UnknownResourceException</code> or a
 *      <code>ConnectionFailedException</code>.</p></li>
 *   </ol>
 *   </p>
 * </dl>
 * </p>
 *
 * @version  $Revision: 1.67 $
 * @author   Janet Davis
 */
public final class RemoteManager extends Component {

  ////////// RemoteManager class members //////////

  /** The component descriptor. */
  private static final ComponentDescriptor SELF =
      new ComponentDescriptor("one.world.rep.RemoteManager",
                              "A manager for remote event passing",
                              true);

  /** The descriptor for the exported request handler. */
  private static final ExportedDescriptor REQUEST =
      new ExportedDescriptor(
          "request", 
          "Handler for remote requests",
          new Class[] { BindingRequest.class, 
                        ResolutionRequest.class,
                        RemoteEvent.class,
                        ExceptionalEvent.class },
          new Class[] { AlreadyBoundException.class, 
                        ConnectionFailedException.class,
                        UnknownResourceException.class},
          false);

  /** The descriptor for the exported main environment handler. */
  private static final ExportedDescriptor ENV = 
      new ExportedDescriptor(
          "env",
          "Handler for environment events",
          new Class[] { EnvironmentEvent.class },
          null,
          false);

  /** The descriptor for the imported environment request handler. */
  private static final ImportedDescriptor ENVREQUEST =
      new ImportedDescriptor(
          "envRequest",
          "The environment's request handler",
          new Class[] { BindingRequest.class },
          null,
          false,
          true);

  /** The descriptor for the imported discovery handler. */
  private static final ImportedDescriptor DISCOVERY =
      new ImportedDescriptor(
          "discovery",
          "The discovery request handler", 
          new Class[] { BindingRequest.class, 
                        ResolutionRequest.class,
                        RemoteEvent.class },
          null,
          false,
          true);

  /** The descriptor for the imported discovery error handler. */
  private static final ImportedDescriptor DISCOVERYERROR =
      new ImportedDescriptor(
          "discoveryError",
          "The discovery error handler", 
          new Class[] { RemoteEvent.class },
          null,
          false,
          true);

  /** The descriptor for the imported lease acquisistion handler. */
  private static final ImportedDescriptor LEASE =
      new ImportedDescriptor(
          "lease",
          "The lease manager's event handler", 
          new Class[] { LeaseEvent.class },
          null,
          false,
          true);

  /** 
   * The descriptor for the imported re-request handler.  Used to
   * requeue requests; linked forced to guarantee the requests will be
   * queued.
   */
  private static final ImportedDescriptor REREQUEST = 
      new ImportedDescriptor(
          "rerequest",
          "My own request handler",
          new Class[] { Timer.Event.class },
          null, 
          true, 
          true);


  /** The inactive state: the default state. */
  private static final int INACTIVE = 0;

  /** The activating state: The component is acquiring resources. */
  private static final int ACTIVATING = 1;

  /** 
   * The active state: The component has all required resources 
   * and is running. 
   */
  private static final int ACTIVE = 2;

  /** The lease renewal time (ms). */
  private static final long TIME_TO_LEASE = Duration.FOREVER;

  /** The system log. */
  static final Log log;

  // Initialize the system log variable.
  static {
    log = (Log)AccessController.doPrivileged(
      new PrivilegedAction() {
        public Object run() {
          return Log.getSystemLog();
        }
      });
  }

  ////////// RemoteManager instance members //////////

  /** The exported request handler. */
  final RequestHandler requestHandler;

  /** The exported environment event handler. */
  final EventHandler envHandler;

  /** The imported environment request handler. */
  final Component.Importer envRequestHandler;

  /** The imported discovery request handler. */
  final Component.Importer discoveryHandler;

  /** The imported discovery error handler. */
  final Component.Importer discoveryError;

  /** The imported lease event handler. */
  final Component.Importer leaseHandler;

  /** The imported re-request handler. */
  final Component.Importer rerequestHandler;

  /** A timer component. */
  final Timer timer;

  /** The requested host name. */
  final String host;

  /** The TCP/UDP port number. */
  final int port;

  /** Indicates whether the requested host name is "localhost". */
  final boolean localhost;

  /** 
   * The internal event handler.  This is transient because the first
   * event it receives is special and it must be recreated each time the
   * component is started.  There's no sense in serializing it.
   */
  transient Handler handler;

  /** 
   * Maintains the mapping from symbolic names to event handlers.   
   * This is package protected so that resolution can be tested 
   * independently of the event handling system.  It's transient because
   * it manages bound resources -- which must be reacquired anyway when
   * the component is restarted.
   */
  transient Resolver resolver;

  /** The connection cache. */
  transient ConnectionCache connections;

  /** 
   * Listens for NetworkIO connections.  This is transient because it is 
   * a bound resource.
   */
  transient EventHandler server;

  /** A maintainer for the server lease. */
  private transient LeaseMaintainer serverMaintainer;

  /** 
   * Listens for events on a datagram connection. 
   */
  transient EventListener datagramListener;

  /** A maintainer for the datagram input channel. */
  private transient LeaseMaintainer datagramInputMaintainer;

  /** 
   * The real host name. This will be different from the requested host 
   * name if the requested host name is 'localhost'.
   */
  private transient String realHost;

  /** The component state. */
  transient int state;

  /** A lock for the component state. */
  private transient Object lock;

  /**
   * Creates a new remote event passing manager.
   *
   * @param env       The environment in which to create the component.
   *                  This environment <emph>will be activated</emph>
   *                  by this constructor.
   * @param host      The local host name or IP address at which to run 
   *                  the server.  "localhost" or the empty string 
   *                  indicates that any local name or address may be
   *                  used.
   * @param port      The local TCP port on which to accept connections.
   *
   * @throws LinkingException
   *         Indicates an error in linking against the environment request
   *         handler.
   */
  public RemoteManager(Environment env, String host, int port)
          throws LinkingException {
    super(env);

    this.timer = getTimer();
    this.host = host;
    this.port = port;

    if (host == null || "".equals(host) 
        || "localhost".equalsIgnoreCase(host)) {
      localhost = true;
    } else {
      localhost = false;
    }
    
    // Set up the transient state.
    connections = new ConnectionCache();
    lock = new Object();
    state = INACTIVE;

    // Set up the non-tranient instance variables.
    requestHandler = new RequestHandler();
    envHandler = new EnvHandler();

    // Declare imported and exported event handlers.
    declareExported(REQUEST, requestHandler);
    declareExported(ENV, envHandler);
    envRequestHandler = declareImported(ENVREQUEST);
    discoveryHandler = declareImported(DISCOVERY);
    discoveryError = declareImported(DISCOVERYERROR);
    leaseHandler = declareImported(LEASE);
    rerequestHandler = declareImported(REREQUEST);

    link("envRequest", "request", env); 
    link("rerequest", "request", this);
  }
  
  /** 
   * Gets the component descriptor.
   */
  public ComponentDescriptor getDescriptor() {
    return (ComponentDescriptor)SELF.clone();
  }

  /** 
   * Gets the lease manager.  (Package private -- used by 
   * {@link Resolver}.)
   */
  EventHandler getLeaseManager() {
    return leaseHandler;
  }

  /** 
   * Gets the host name.  (Package private -- used by 
   * {@link Resolver}.)
   */
  String getHost() {
    return realHost;
  }

  /** 
   * Gets the TCP port number.  (Package private -- used by 
   * {@link Resolver}.)
   */
  int getPort() {
    return port;
  }

  /** Returns true if the specified localized resource is local. */
  boolean isLocal(LocalizedResource resource) {
    return (!Constants.REP_FORCE_NETWORK 
            && (resource.port == port)
	    && (resource.host.equals(realHost)
	        || (localhost 
	            && SystemUtilities.isLocalHost(resource.host))));

  }

  /** 
   * Sends an event locally.  If the resource is unknown, a warning will
   * be logged.
   *
   * @param resource   A localized resource describing the event
   *                   destination.
   * @param event      The event to send.
   */
  void sendLocally(final LocalizedResource r, Event e) {
    try {
      resolver.realResolve(r).handle(e);  
    } catch (UnknownResourceException x) {
      log.logWarning(this, 
                     "Attempted to send event to unknown resource ("
                       + e + ")",
                     x);
    }
  }

  /**
   * Requeues an event by passing it to the self-linked re-request
   * handler.   This is used to requeue events while the remote manager is
   * starting up.
   */
  void requeue(Event e) {
    try {
      SystemUtilities.sleep(1);
    } catch (InterruptedException x) {
      Thread.currentThread().interrupt();
    }
    rerequestHandler.handle(e);
  }

  /** 
   * Sends an event on the channel specified by the given resource.
   *
   * @param resource  The destination resource.
   * @param datagram  Whether to use an unreliable datagram channel.
   * @param closure   The output request closure.
   * @param event     The event to send.
   *
   * @throws ConnectionFailedException
   *         If no connection can be established.
   */
  void send(LocalizedResource resource, boolean datagram,
	    Object closure, Event event)
      throws ConnectionFailedException {
    ConnectionManager connection = connections.get(resource, datagram);
    connection.send(new SimpleOutputRequest(requestHandler, closure, event));
  }

  /**
   * Processes a resolution request by sending back a resolution
   * response or an exception.
   * 
   * @param request   A resolution request.
   */
  void process(ResolutionRequest request) {
    if (request.resource instanceof LocalizedResource) {
      try {
        SymbolicHandler resource = 
              resolver.resolve((LocalizedResource)request.resource);

        Event response =
              new ResolutionResponse(request.source, 
                                     request.closure,
                                     new Tuple[] { null },
                                     new SymbolicHandler[] { resource });

        LocalizedResource destination = (LocalizedResource)request.source;
        if (isLocal(destination)) {
	  response.setMetaData(Constants.REQUESTOR_ID,
	                       getEnvironment().getId());
          response.setMetaData(Constants.REQUESTOR_ADDRESS, 
                               "localhost");
          response.setMetaData(Constants.REQUESTOR_PORT, 
                               new Integer(port));
	  resolver.realResolve(destination).handle(response);
	} else {
          send(destination, false, null, response);
	}

      } catch (UnknownResourceException x) {
        sendException(request.source, request.closure, x);
      } catch (ConnectionFailedException x) {
        log.logWarning(this, 
	               "Connection failed while resolving resource",
		       x);
      }

    } else {
      Exception x = new UnknownResourceException(
                               "Cannot resolve unknown resource type:"
			        + request.resource.getClass());
      sendException(request.source, request.closure, x);
    }
  }

  /**
   * Processes a resolution response by sending the result to the
   * requester (if possible).
   * 
   * @param response   A resolution response.
   */
  void process(ResolutionResponse response) {

    if (response.source instanceof RemoteReference) {
      RemoteReference destination = (RemoteReference)response.source;
      response.source = requestHandler;
      sendLocally(destination, response);
    } else {
      log.logWarning(this,
                  "Resolution event source is not a remote reference");
    }
  }

  /**
   * Processes an exceptional event by sending the result to the
   * event source (if possible).
   * 
   * @param event   An exceptional event.
   */
  void process(ExceptionalEvent event) {
    if (event.source instanceof LocalizedResource) {
      LocalizedResource destination = (LocalizedResource)event.source;
      event.source = requestHandler;
      sendLocally(destination, event);
    } else if (event.source instanceof DiscoveredResource) {
      log.logError(this, "Got exception for discovered resource"
                           + " over the network");
    } else {
      log.logWarning(this, "Got exception for unknown resource type: "
                              + event.source);
    }
  }

  /**
   * Processes a remote event received from the network.  
   * Forwards the event to the appropriate local event handler.
   *
   * @param event   The remote event to process.
   */
  void process(RemoteEvent event) {
    if (event.destination instanceof LocalizedResource) {
      Object dss = null;
      Object db  = null;

      if (event.metaData != null) {
        if (event.hasMetaData(Constants.DISCOVERY_SOURCE_SERVER)) {
          dss = event.metaData.remove(Constants.DISCOVERY_SOURCE_SERVER);
          db  = event.metaData.remove(Constants.DISCOVERY_BINDING);
        }
      }

      try {
        EventHandler h = 
            resolver.realResolve((LocalizedResource)event.destination);
        event.source = requestHandler;
        h.handle(event);
      } catch (UnknownResourceException x) {
        if (dss != null) {
          event.setMetaData(Constants.DISCOVERY_SOURCE_SERVER,dss);
          event.setMetaData(Constants.DISCOVERY_BINDING,db);
	  discoveryError.handle(event);
	} else {
	  sendException(event.event.source, event.closure, x);
	}
      }
    } else {
      sendException(event.event.source, event.closure,
            new UnknownResourceException("Unknown remote resource type: "
	                                 + event.destination.getClass()));
    }
  }

  /** 
   * Sends the specified exception to the specified resource, with the
   * specified closure.
   *
   * @param resource The resource to send to.
   * @param closure  The closure to use.
   * @param x        The exception to send.
   */
  void sendException(EventHandler resource, Object closure,
                     Throwable x) {
    try {
      LocalizedResource lr = (LocalizedResource)resource;
      if (isLocal(lr)) {
        sendLocally(lr, new ExceptionalEvent(resource, closure, x));
      } else {
        send(lr, false, closure, 
             new ExceptionalEvent(resource, closure, x));
      }
    } catch (ClassCastException xx) {
      log.logWarning(this, 
                     "Event source not localized resource",
		     xx);
    } catch (ConnectionFailedException xx) {
      log.logWarning(this, 
                     "Connection failed while sending exceptional event",
		     xx);
    } 
  }

  /**
   * Starts the activation process.  If the component is inactive,
   * constructs transient members and asks for a server.  Otherwise, does
   * nothing.
   */
  void start() {

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
                         
    // If the host is unusefully "localhost", get a real local IP
    // address.
    if (localhost) {
      try {
        realHost = InetAddress.getLocalHost().getHostAddress();
      } catch (UnknownHostException x) {
        log.logError(this, "Failed to obtain local IP address", x);
        synchronized (this) {
          state = INACTIVE;
        }
        return;
      }
    } else {
      realHost = host;
    }

    // Construct other members.
    handler = new Handler();
    resolver = new Resolver(this);
    connections = new ConnectionCache();

    // Ask for a network I/O server at the specified host and port.
    SioResource serverResource = 
        new SioResource("sio://" + host + ":" + port + "?type=server");
    serverResource.closure = "Client";

    BindingRequest request = 
        new BindingRequest(handler, "Server", 
                           serverResource, Duration.FOREVER);

    // Make the request through a lease maintainer.
    serverMaintainer =
        new LeaseMaintainer(request, envRequestHandler, timer);

    // Ask for a datagram input connection at the specified host and port.
    SioResource inputResource =
        new SioResource("sio://" + host + ":" + port + "?type=input");
    request = new BindingRequest(handler, "Input",
                                 inputResource, Duration.FOREVER);
    datagramInputMaintainer =
        new LeaseMaintainer(request, envRequestHandler, timer);
  }

  /** 
   * If the current state is ACTIVATING, completes the activation process.
   */
  void run() {
    synchronized (lock) {
      if (state == ACTIVATING) {
        lock.notifyAll();
        state = ACTIVE;
      } else {
        throw new Bug("Invalid state transition: (!ACTIVATING)->ACTIVE");
      }
    }
  }

  /** 
   * Set the state to INACTIVE and release all resources. 
   */
  void stop() {

    if (discoveryHandler.isLinked()) {
      Synchronous.invoke(discoveryHandler, 
                         new EnvironmentEvent(null, null, 
			                      EnvironmentEvent.STOP,
					      getEnvironment().getId()));
    }

    synchronized (lock) {
      if (state == INACTIVE) {
        return;
      }
      state = INACTIVE; 
    }

    Synchronous.ResultHandler h = new Synchronous.ResultHandler();
    Operation o = new Operation(3, 10*Duration.SECOND, timer, null, h);

    if (serverMaintainer != null) {
      serverMaintainer.cancel(o);
      h.getResult();
      server = null;
      serverMaintainer = null;
    }

    if (datagramInputMaintainer != null) {
      h.reset();
      datagramInputMaintainer.cancel(o);
      h.getResult();
      datagramInputMaintainer = null;
    }

    if (datagramListener != null) {
      datagramListener.cancel(h, o);
      datagramListener = null;
    }

    if (connections != null) {
      connections.revoke(h, o);
    }

    if (Constants.DEBUG_REP) {
      log.log(this, "Stopped");
    }
  }

  /** 
   * Serialize this remote manager.
   *
   * @serialData    The default fields while holding the lock.
   */
  protected void writeObject(ObjectOutputStream out) 
          throws IOException {
    synchronized (lock) {
      out.defaultWriteObject();
    }
  }

  /** Deserialize the remote manager. */
  private void readObject(ObjectInputStream in) 
          throws IOException, ClassNotFoundException {
    
    // Read the non-transient fields.
    in.defaultReadObject();

    // Restore the lock.
    lock = new Object();

    // Make sure the state is inactive so that resources will be 
    // re-acquired.
    state = INACTIVE;
  }

  ////////// Internal event handler //////////
  /**
   * Internal event handler for the RemoteManager component.  This event
   * handler is never exported, but it is used as the source of some
   * events.
   */
  private final class Handler extends AbstractHandler {

    /**
     * Handles events.
     */
    protected boolean handle1(Event event) {

      if (event instanceof BindingResponse) {
        BindingResponse br = (BindingResponse)event;
        if (br.closure == "Client") {
          connections.add(br);
        } else if (br.closure == "Server") {
	  server = br.resource;
	  if (datagramListener != null) {
	    run();
	  }
	} else if (br.closure == "Input") {
	  datagramListener = new EventListener(br.resource);
	  if (server != null) {
	    run();
	  }
        } else {
          log.logError(this, 
                         "Got BindingResponse with unknown closure");
        }
        return true;

      } else if (event instanceof ExceptionalEvent) {
        Throwable x = ((ExceptionalEvent)event).x;
        if (event.closure == "Server" || event.closure == "Input") {
            log.logError(RemoteManager.this, "Fatal exception", x);
          stop();
	  return true;
        }
        return true;
      }
      return false;
    }
  }

  ////////// Request handler //////////
  /**
   * Request handler for the RemoteManager component.
   */
  private final class RequestHandler extends AbstractHandler {

    /**
     * Handles binding requests, resolution requests, and remote events.
     */
    protected boolean handle1(Event event) {

      if (state != ACTIVE) {
        requeue(event);
        return true;
      }

      if (event instanceof RemoteEvent) {
        RemoteEvent re = (RemoteEvent)event;
	try {
	  re.verifySymbolic();
	} catch (TupleException x) {
	  respond(re, x);
	}
        return send(re);

      } else if (event instanceof BindingRequest) {
        BindingRequest br = (BindingRequest)event;
        if (br.descriptor instanceof RemoteDescriptor) {
          return export(br, (RemoteDescriptor)br.descriptor);
        }

      } else if (event instanceof ResolutionRequest) {
        return resolve((ResolutionRequest)event);

      } else if (event instanceof EnvironmentEvent) {
        EnvironmentEvent ee = (EnvironmentEvent)event;

        if (ee.type == EnvironmentEvent.STOP) {
          // Stop and release all resources.
          stop();
          respond(ee, new EnvironmentEvent(this, null,
                                           EnvironmentEvent.STOPPED,
                                           getEnvironment().getId()));
          return true;
        }
        
      } else if (event.source instanceof SymbolicHandler) {
        respond(event, 
                new UnknownEventException(event.getClass().getName()));
        return true;
      }

      return false;
    }

    /**
     * Retries a failed send.
     */
    void retry(Event event, Throwable x) {

      // We need to deal with exceptional events generated by failed
      // connections.  The original event is nested in the exceptional
      // event.

      if (event instanceof RemoteEvent) {

        // If it's a connection failed exception and we can retry, 
        // retry.  Otherwise, respond to the event with an exception.

        RemoteEvent re = (RemoteEvent)event;
        if (canRetry(re)) {
          send(re);
        } else {
          respond(re, new ConnectionFailedException(
                                 "Connection failed after "
                                 + Constants.REP_MAX_RETRIES
                                 + " retries",
				 x));
        }

      } else if (event instanceof ResolutionRequest) {
        ResolutionRequest rq = (ResolutionRequest)event;
        if (canRetry(rq)) {
          resolve(rq);
        } else {
          try {
            sendLocally((RemoteReference)rq.source, 
                        new ExceptionalEvent(this, rq.closure,
                        new ConnectionFailedException(
                                 "Connection failed after "
                                 + Constants.REP_MAX_RETRIES
                                 + " retries",
                                 x)));
          } catch (ClassCastException x2) {
            log.logWarning(this, "Resolution event source not a"
                                   + " remote reference", x2);
          }
        }
      }
    }

    /** 
     * Exports an event handler anonymously, by name, or via the discovery
     * service, as required by the given binding request and descriptor.
     *
     * @param br    The original binding request.
     * @param desc  The export descriptor encapsulated in the binding
     *              request.
     *
     * @return      <code>true</code> if the event was handled.
     */
    private boolean export(BindingRequest br, RemoteDescriptor desc) {
    
      if (desc.descriptor == null) {
        // Export anonymously.  The resolver will respond with a
        // BindingResponse or the exception, as necessary.
        resolver.export(br, null, desc.handler);

      } else if (desc.descriptor instanceof Name) {
        // Export by name.
        resolver.export(br, ((Name)desc.descriptor).name, desc.handler);

      } else {
        // Export through discovery service.
        discoveryHandler.handle(br);
      }

      return true;
    }

    /** 
     * Handles a resolution request.
     * 
     * @param event  The resolution request.
     *
     * @return      <code>true</code> if the event was handled.
     */
    private boolean resolve(ResolutionRequest event) {

      if (!(event.source instanceof LocalizedResource)) {
        respond(event, new InvalidTupleException(
            "Resolution request source is not a localized resource (" +
            event.source + ")"));
        return true;
      } 

      if (Constants.DEBUG_REP) {
        log.log(RemoteManager.this, 
                "Sending resolution request (" + event + ")");
      }

      if (event.resource instanceof LocalizedResource) {
        LocalizedResource resource = (LocalizedResource)event.resource;

        if (isLocal(resource)) {
	  process(event);
	  return true;
	}

        incrementRetries(event);

        try {
          // Attempt to send.  The event is the closure of the output
          // request so we can retry it if the attempt fails.
	  RemoteManager.this.send(resource, false, event, event);

        } catch (ConnectionFailedException x) {
          // Report back exceptions.
	  sendLocally((LocalizedResource)event.source,
                      new ExceptionalEvent(this, event.closure, x));

        }

        return true;

      } else if (event.resource instanceof DiscoveredResource) {
        discoveryHandler.handle(event); 
        return true;
      } 

      return false;
    }

    /**
     * Sends a remote event. 
     *
     * @param event   The remote event to send.
     *
     * @return        <code>true</code> if the event was handled. 
     */
    private boolean send(RemoteEvent event) {
      EventHandler source = event.source;

      if (Constants.DEBUG_REP) {
        log.log(RemoteManager.this, "Sending remote event (" + event + ")");
      }

      if (event.destination instanceof LocalizedResource) {
        LocalizedResource resource = (LocalizedResource)event.destination;

        if (isLocal(resource)) {
          event.setMetaData(Constants.REQUESTOR_ADDRESS, 
                            "localhost");
          event.setMetaData(Constants.REQUESTOR_PORT, 
                            new Integer(port));
          try {
	    resolver.realResolve(resource).handle(event);
	  } catch (UnknownResourceException x) {
	    source.handle(new ExceptionalEvent(this, event.closure, x));
	  }
	  return true;
	}

        incrementRetries(event);

        // Make a copy of the remote event which differs in the source
        // field.
        Event newEvent = (Event)event.clone();
	newEvent.source = NullHandler.NULL;
         
        // Find a connection manager and tell it to send the event.
        try {

          // Attempt to send.   The original event is included in case a
	  // retry is necessary.
	  RemoteManager.this.send((LocalizedResource)event.destination, 
	                          event.datagram, event, newEvent);

        } catch (ConnectionFailedException x) {
          // Report back exceptions.
          event.source.handle(new ExceptionalEvent(this, event.closure, x));
        }
        return true;

      } else if (event.destination instanceof DiscoveredResource) {
        // Hand the event off to the discovery service to take care of.
        discoveryHandler.handle(event);
        return true;
      }
      
      return false;
    }

    /** Increment the number of retries on a tuple. */
    private void incrementRetries(Tuple t) {
      Object obj = t.getMetaData(Constants.REP_RETRIES);
      int tries = (null == obj) ? 0 : 1 + ((Integer)obj).intValue();
      t.setMetaData(Constants.REP_RETRIES, new Integer(tries));
    }

    /** 
     * Check the number of retries; return true only if another retry is
     * allowed.
     */
    private boolean canRetry(final Tuple t) {
      Object obj = t.getMetaData(Constants.REP_RETRIES);
      return ((obj != null)
         && (((Integer)obj).intValue() < Constants.REP_MAX_RETRIES));
    }
  }

  ////////// Environment handler //////////

  /**
   * Environment handler for the RemoteManager component.  Reacts to a
   * STOP event by releasing all leases. 
   */
  private final class EnvHandler extends AbstractHandler {
    
    /** 
     * Handles environment events. 
     */
    protected boolean handle1(Event event) {
      if (event instanceof EnvironmentEvent) {
        EnvironmentEvent ee = (EnvironmentEvent)event;

        if ((ee.type == EnvironmentEvent.ACTIVATED)
            || (ee.type == EnvironmentEvent.RESTORED)
            || (ee.type == EnvironmentEvent.MOVED)
            || (ee.type == EnvironmentEvent.CLONED)) {

          // Start the activation process.
          start();

        } else if (ee.type == EnvironmentEvent.STOP) {
          // Stop and release all resources.
          stop();
          respond(ee, new EnvironmentEvent(this, null,
                                           EnvironmentEvent.STOPPED,
                                           getEnvironment().getId()));
        }
      } 

      return true;
    }
  }

  ////////// Event listener //////////
  /**
   * Listens for events on a structured I/O channel.
   */
  private final class EventListener extends AbstractHandler {

    /** The query to use when listening for events. */
    Query query = 
        new Query("", Query.COMPARE_HAS_SUBTYPE, Event.class);

    /** The structured I/O client. */
    EventHandler channel;

    /** A lease maintainer for the listen operation. */
    LeaseMaintainer listenMaintainer;

    /**
     * Constructs a new event listener for the specified structured I/O
     * channel.
     */
    public EventListener(EventHandler channel) {
      this.channel = channel;
      channel.handle(new SimpleInputRequest(this, null,
                                            SimpleInputRequest.LISTEN,
                                            query, 
                                            Duration.FOREVER,
                                            false));
    }

    /** Handles events. */
    public boolean handle1(Event event) {

      if (event instanceof InputResponse) {

        Tuple tuple = ((InputResponse)event).tuple;
        
	try {
	  tuple.validate();
	  if (tuple instanceof RemoteEvent) {
	    ((RemoteEvent)tuple).verifySymbolic();
	  }
	} catch (TupleException x) {
	  log.logWarning(this, "Got invalid tuple from network", x);
	}
  
        if (Constants.DEBUG_REP) {
          log.log(RemoteManager.this, "Received event (" + tuple + ")");
        }
  
        // Demultiplex on the tuple type.
        if (tuple instanceof RemoteEvent) {
          process((RemoteEvent)tuple);
        } else if (tuple instanceof ExceptionalEvent) {
          process((ExceptionalEvent)tuple);
        } else if (tuple instanceof ResolutionRequest) {
          process((ResolutionRequest)tuple);
        } else if (tuple instanceof ResolutionResponse) {
          process((ResolutionResponse)tuple);
        } else {
          log.logWarning(this, "Got unexpected input response: " 
                               + tuple.toString());
        }
        return true;
        
      } else if (event instanceof ListenResponse) {
        ListenResponse lr = (ListenResponse) event;
        listenMaintainer = new LeaseMaintainer(lr.lease, lr.duration,
                                                   this, null, timer);
        return true;
      }
      
      return false;
    }

    /**
     * Cancels this event listener. 
     *
     * @param result          A result handler.
     * @param operation       An operation, not being used concurrently 
     *                        for anything else.
     */
    public void cancel(Synchronous.ResultHandler resultHandler,
                       Operation operation) {
      if (listenMaintainer != null) {
        resultHandler.reset();
        listenMaintainer.cancel(operation);
        resultHandler.getResult();
      }
    }
  }

  ////////// Connection manager //////////
  /**
   * Manages a single SIO client connection.
   */
  private final class ConnectionManager extends AbstractHandler {

    /** The state where an I/O channel is being bound. */
    private static final int BINDING = 0;

    /** The active state. */
    private static final int ACTIVE = 1;

    /** The revoked state. */
    private static final int REVOKED = 2;

    /** The state of this connection manager. */
    private volatile int state;

    /** 
     * Has this connection manager been recached with the correct remote
     * port?
     */
    private volatile boolean recached;

    /** The remote IP address for this connection. */
    private String remoteHost;

    /** The url for this connection, used as a map key. */
    private String url;

    /** 
     * The queue of waiting send requests (used in the BINDING state).
     */
    private ArrayList queue;

    /** The network I/O client. */
    private EventHandler client;

    /** The leases for the client and the listen requests. */
    private LinkedList leaseMaintainers;

    /** 
     * The dirty flag; used to decide when to uncache the connection.
     */
    private boolean dirty;

    /** The lock object. */
    private Object lock;

    /** The timer cancelation handler. */
    private Timer.Notification timerNotification;

    /**
     * Creates a new connection manager with the given binding response.
     *
     * @param br        The binding response.
     */
    public ConnectionManager(BindingResponse br) {

      this.url = br.descriptor.toString();
      this.lock = new Object();
      this.leaseMaintainers = new LinkedList();
      this.queue = null;

      activate(br);
    }

    /**
     * Creates a new connection manager with the given url.  Events will
     * be queued until the connection is established using {@link bind}.
     *
     * @param url       An SioResource for the desired connection.
     */
    public ConnectionManager(String url) {

      if (Constants.DEBUG_REP) {
        log.log(RemoteManager.this, "Binding connection (" + url + ")");
      }
      
      this.state = BINDING;
      this.recached = true; // No need to recache; the remote port is correct
      this.url = url;
      this.lock = new Object();
      this.leaseMaintainers = new LinkedList();
      this.queue = new ArrayList();
    }

    /**
     * Starts the binding process for this connection.
     */
    public void bind() {
      synchronized (lock) {
        if (state != BINDING) {
          return;
        }
        long time = SystemUtilities.currentTimeMillis() 
                  + Constants.REP_CACHE_TIMEOUT;
        Event timeOut = new ExceptionalEvent(this, new Integer(BINDING),
                                             new TimeOutException());
        timerNotification = timer.schedule(Timer.FIXED_DELAY, 
                                           time, 
                                           Constants.REP_CACHE_TIMEOUT,
                                           this, timeOut);
      }

      BindingRequest br = 
          new BindingRequest(this, new Integer(BINDING), 
	                     new SioResource(url),
                             Duration.FOREVER);
      envRequestHandler.handle(br);
    }

    /**
     * Handles events.  
     *
     * @param event   The event to handle.
     */
    protected boolean handle1(Event event) {
      
      if (event instanceof InputResponse) {
        // We got something from the network
        dirty();

        Tuple tuple = ((InputResponse)event).tuple;
        
	try {
	  tuple.validate();
	  if (tuple instanceof RemoteEvent) {
	    ((RemoteEvent)tuple).verifySymbolic();
	  }
	} catch (TupleException x) {
	  log.logWarning(this, "Got invalid tuple from network", x);
	}
  
        if (Constants.DEBUG_REP) {
          log.log(RemoteManager.this, "Received event (" + tuple + ")");
        }
  
        // Add the requestor address to the metadata.
        tuple.setMetaData(Constants.REQUESTOR_ADDRESS, remoteHost);

        // Does this need to be recached with the correct remote port?
	boolean wasRecached;
	Integer port = (Integer)tuple.getMetaData(Constants.REQUESTOR_PORT);
	if (port != null) {
	  wasRecached = recached;
	  recached = true;
	  
	  if (!wasRecached) {
	    connections.recache(this, port.intValue());
	  }
        }
  
        // Demultiplex on the tuple type.
        if (tuple instanceof RemoteEvent) {
          process((RemoteEvent)tuple);
        } else if (tuple instanceof ExceptionalEvent) {
          process((ExceptionalEvent)tuple);
        } else if (tuple instanceof ResolutionRequest) {
          process((ResolutionRequest)tuple);
        } else if (tuple instanceof ResolutionResponse) {
          process((ResolutionResponse)tuple);
        } else {
          log.logWarning(this, "Got unexpected input response: " 
                               + tuple.toString());
        }
        return true;

      } else if (event instanceof OutputResponse) {
        // Nothing to do
        return true;

      } else if (event instanceof ExceptionalEvent) {
        Throwable x = ((ExceptionalEvent)event).x;

        if (new Integer(BINDING).equals(event.closure)) {
          // Signals an error in binding the network I/O client

          int oldState;
          synchronized (lock) {
            oldState = state;
            if (state == BINDING) {
	      remove();
            }
          }
        
          if ((oldState != REVOKED) && (state == REVOKED)) {
	    if (Constants.DEBUG_REP) {
	      log.logWarning(this, "Connection failed; removing", x);
	    }
            // Drain the queue, sending exceptions in response to all
            // requests.
            Iterator iter = queue.iterator();
	    SimpleOutputRequest request;
            while (iter.hasNext()) {
	      request = (SimpleOutputRequest)iter.next();
	      requestHandler.retry((Event)request.closure, x);
            }
          }
          return true;

        } else if (x instanceof NoBufferSpaceException) {
          if (event.closure instanceof Event) {
            // Send the exception back to the original requester
            respond((Event)event.closure, x);
            return true;

          } else if (event.closure instanceof Query) {
            // Try to establish listen again
            if (Constants.DEBUG_REP) {
              log.logWarning(RemoteManager.this, 
                             "Got NoBufferSpace exception while"
                             + " establishing listen; trying again"
                             + " (" + url + ")");
            }

            Query q = (Query)event.closure;
            if (state == ACTIVE) {
                client.handle(
                    new SimpleInputRequest(this, q, 
                                           SimpleInputRequest.LISTEN,
                                           q, Duration.FOREVER, false));
            }
            return true;
          }

        } else if ((x instanceof InvalidClassException)
            || (x instanceof NotSerializableException)) {
          if (event.closure instanceof Event) {
            // Non-fatal, but won't succeed on a retry
	    // Inform the sender of the exception
            respond((Event)event.closure, x);
            return true;
          } else {
	    log.logWarning(this, 
	                  "Tuple not serializable",
	                  x);
	  }

        } else if ((x instanceof LeaseDeniedException)
                || (x instanceof LeaseRevokedException)
                || (x instanceof ResourceRevokedException)
		|| (x instanceof IllegalStateException)
                || (x instanceof IOException)) {
          // These exceptions are fatal to the connection.
	  if (Constants.DEBUG_REP) {
	    log.logWarning(this, "Connection failed; removing", x);
	  }
          remove();
          if (event.closure instanceof Event) {
	    requestHandler.retry((Event)event.closure, x);
          } 
          return true;

        } else if (event.closure instanceof Query) {
          log.logError(this, "Could not establish listen", x);
          remove();
          return true;
        }

      } else if (event instanceof DynamicTuple
                 && ("clean" == event.get("msg"))) {
        // Time to clean the connection
        clean();
        return true;

      } else if (event instanceof BindingResponse) {
        // Initial binding response containing the network I/O client
        int oldState;
        synchronized (lock) {
          oldState = state;
          if (state == BINDING) {
            state = ACTIVE;
          }
        }

        if (oldState == BINDING) {
          activate((BindingResponse)event);
        } else {
          LeaseMaintainer.cancel(((BindingResponse)event).lease);
        }

        return true;

      } else if (event instanceof ListenResponse) {
        // Listen established
        ListenResponse lr = (ListenResponse)event;
	boolean added = false;
	synchronized (lock) {
	  if (state != REVOKED) {
            leaseMaintainers.add(
                new LeaseMaintainer(lr.lease, lr.duration,
                                    this, null, timer));
            added = true;
          }
        }
	if (!added) {
	  LeaseMaintainer.cancel(lr.lease);
	}
	return true;
      }

      return false;
    }

    /** 
     * Activate this connection manager.
     *
     * @param br   The binding response containing the network I/O client.
     */
    private void activate(final BindingResponse br) {

      if (Constants.DEBUG_REP) {
        log.log(RemoteManager.this, "Activating connection (" + url + ")");
      }

      synchronized (lock) {
        state = ACTIVE;
        dirty = true;
      }

      SioResource sioResource = (SioResource)br.descriptor;
      remoteHost = sioResource.remoteHost;
      client = br.resource;

      leaseMaintainers.add(
          new LeaseMaintainer(br.lease, br.duration,
                              this, null,
                              timer));

      // Set up listen request, if the SIO resource is a TCP client
      // (Otherwise, there is nothing to listen for)
      if (sioResource.type == SioResource.CLIENT) {
        Query q = new Query("", Query.COMPARE_HAS_SUBTYPE, Event.class);
        client.handle(new SimpleInputRequest(this, q, 
                                             SimpleInputRequest.LISTEN,
                                             q, Duration.FOREVER, false));
      }

      // Set up cache timer
      if (timerNotification != null) { 
        timerNotification.cancel();
      }
      Event cleanEvent = new DynamicTuple(this, null);
      cleanEvent.set("msg", "clean");
      timerNotification = timer.schedule(Timer.FIXED_DELAY,
                                         SystemUtilities.currentTimeMillis()
                                         + Constants.REP_CACHE_TIMEOUT,
                                         Constants.REP_CACHE_TIMEOUT,
                                         this,
                                         cleanEvent);

      // Empty the queue.
      if (queue != null) {
        Iterator iter = queue.iterator();
        queue = null;

        SimpleOutputRequest r;
        while (iter.hasNext()) {
          r = (SimpleOutputRequest)iter.next();
          realSend(r);
        }
      }
    }

    /** Sets the dirty flag. */
    private void dirty() {
      synchronized (lock) {
        dirty = true;
      }
    }   

    /** 
     * Resets the dirty flag and removes the connection from the cache if
     * it was clean.
     */
    private void clean() {
      boolean wasDirty;
      synchronized (lock) {
        wasDirty = dirty;
        dirty = false;
      }
      if (!wasDirty) {
        remove();
      }
    }

    /**
     * Removes the connection from the cache. 
     */
    private void remove() {
      synchronized (lock) {
        if (state != REVOKED) {
          state = REVOKED;
	  if (connections != null) {
            connections.remove(url);
	  }
          revoke();
        }
      }
    }

    /**
     * Cancels the leases and timer and nullifies the resources. 
     */
    public void revoke() {
      if (Constants.DEBUG_REP) {
        log.log(RemoteManager.this, "Revoking connection (" + url + ")");
      }

      synchronized (lock) {
        state = REVOKED;
        client = null;

        Iterator it = leaseMaintainers.iterator();
        while(it.hasNext()) {
            LeaseMaintainer m = (LeaseMaintainer)it.next();
          m.cancel();
        }

        // Cancel the timer.
        if (timerNotification != null) {
          timerNotification.cancel();
        } 
	timerNotification = null;
      }
    }

    /**
     * Synchronously cancels the leases and timer and nullifies the 
     * resources. 
     *
     * @param resultHandler   A result handler.
     * @param operation       An operation, not being used concurrently
     *                        for anything else.
     */
    public void revoke(Synchronous.ResultHandler resultHandler,
                       Operation operation) {
      if (Constants.DEBUG_REP) {
        log.log(RemoteManager.this, 
	        "Synchronously revoking connection (" + url + ")");
      }

      synchronized (lock) {
        state = REVOKED;
        client = null;

        Iterator it = leaseMaintainers.iterator();
        while(it.hasNext()) {
	  resultHandler.reset();
          LeaseMaintainer m = (LeaseMaintainer)it.next();
          m.cancel(operation);
	  resultHandler.getResult();
        }

        // Cancel the timer.
        if (timerNotification != null) {
          timerNotification.cancel();
        } 
	timerNotification = null;
      }
    }

    /**
     * This overloaded respond method copes with {@link LocalizedResource}s
     * as event sources.
     */
    protected void respond(Event e, Throwable x) {
      if (e.source instanceof LocalizedResource) {
        // Resolve localized resources to real event handlers.
        sendLocally((LocalizedResource)e.source, 
                    new ExceptionalEvent(this, e.closure, x));
      } else {
        // Call the superclass's respond method.
        super.respond(e, x);
      }
    }

    /** 
     * Uses the specified simple output request to forward a tuple to the
     * remote endpoint.  The source of the output request will receive all
     * {@link ExceptionalEvent}s send in response to the request.
     *
     * <p>If this connection is {@link #ACTIVE}, the tuple will be sent
     * immediately.  If the connection is {@link #BINDING}, it will be
     * temporarily enqueued.  If the connection is {@link #REVOKED}, the
     * response will be a <code>ConnectionFailedException.</p>
     */
    public void send(SimpleOutputRequest r) {
      switch (state) {
        case ACTIVE:
          realSend(r);
          break;
        case BINDING:
          enqueue(r);
          break;
        case REVOKED:
	  remove();
	  requestHandler.retry((Event)r.closure,
	          new ConnectionFailedException("Connection failed"));
          break;
      }
    }

    /**
     * Enqueues an output request, or sends it directly if the state is 
     * no longer <code>BINDING</code>.
     */
    private void enqueue(SimpleOutputRequest r) {
      boolean queued = false;
      boolean overflow = false;

      synchronized (lock) {
        if (state == BINDING) {
          if (queue.size() < Constants.REP_QUEUE_CAPACITY) {
              queue.add(r);
            queued = true;
          } else {
            overflow = true;
          }
        }
      }

      if (!queued) {
        realSend(r);
      } else if (overflow) {
        respond((Event)r.closure, 
	        new NoBufferSpaceException("Cannot enqueue event for"
                                           + " pending connection ("
                                           + url + ")"));
      } else {
        if (Constants.DEBUG_REP) {
	  log.log(this, "Enqueued " + r.tuple);
	}
      }
    }

    /** 
     * Uses the specified simple output request to forward a tuple to the
     * remote endpoint.  The source of the output request will receive all
     * {@link ExceptionalEvent}s send in response to the request.
     *
     * @param request   An output request wrapping the tuple to send.
     */
    private void realSend(SimpleOutputRequest request) {

      // Annotate the tuple with the requestor id, address, and port.
      request.tuple.setMetaData(Constants.REQUESTOR_ADDRESS, 
                                realHost);
      request.tuple.setMetaData(Constants.REQUESTOR_PORT, 
                                new Integer(port));

      // Safely get a reference to the client.
      EventHandler myClient = null;
      synchronized (lock) {
        if (state == ACTIVE) {
          myClient = client;
        } 
      }

      // Really send the request.
      if (myClient != null) {
        dirty();
        // We want to intercept the responses.
	request.source = this;

        myClient.handle(request);

	if (Constants.DEBUG_REP) {
	  log.log(this, "Sent to network channel: " + request.tuple);
	}
      } else {
        remove();
        requestHandler.retry((Event)request.closure,
                  new ConnectionFailedException("Connection failed"));
      }
    }

    /** Returns true if the connection state is {@link #REVOKED}. */
    public boolean isRevoked() {
      // This isn't under a lock because it seems to cause deadlock.
      // state will never go from REVOKED to !REVOKED.
      return (state == REVOKED);
    }

    /** Gets the url of this connection. */
    public String getUrl() {
      return url;
    }

    /** Sets the url of this connection. */
    public void setUrl(String url) {
      synchronized (lock) {
        this.url = url;
      }
    }

    /** Returns a string representation of this connection manager. */
    public String toString() {
      return "#[ConnectionManager for " + url + "]";
    }
  }

  ////////// ConnectionCache //////////
  
  /**
   * A cache of connection managers.
   */
  private final class ConnectionCache {

    /** 
     * The map from {@link SioResource}s to <code>ConnectionManager</code>s.
     */
    private Map map;

    /** A lock for the map. */
    private Object lock;

    /**
     * Creates a new connection cache.
     */
    public ConnectionCache() {
      map = new HashMap();
      lock = new Object();
    }

    /** 
     * Add a connection to the connection cache.
     *
     * @param br      A binding response containing the Network I/O client
     *                and its lease.
     * @return        The added connection.
     */
    public ConnectionManager add(BindingResponse br) {
  
      if (!(br.descriptor instanceof SioResource)) {
        throw new Bug("Got `" + br.descriptor
                      + "' instead of SioResource");
      }

      ConnectionManager connection = new ConnectionManager(br);
      String url = br.descriptor.toString();

      synchronized (lock) {
        if (map.get(url) != null) {
	  connection.revoke();
	  throw new Bug("Attempt to replace existing connection: " + url);
	}
        map.put(url, connection);
      }

      return connection;
    }

    /**
     * Attempt to recache a connection.  If there is already a valid
     * connection for the connection's host and the given port, don't do
     * anything.
     *
     * @param connection  The connection to recache.
     * @param port        The new port number.
     */
    void recache(ConnectionManager connection, int port) {
      String oldUrl = connection.getUrl();
      SioResource resource = new SioResource(oldUrl);
      
      // This will never be a datagram connection, since we don't receive
      // tuples on output-only connections.
      String newUrl;
      try {
        newUrl = getUrl(resource.remoteHost, port, false);
      } catch (UnknownHostException x) {
        return;
      }

      // Move the connection within the cache.
      synchronized (lock) {
        if (null == map.get(newUrl)) {
	  map.remove(oldUrl);
	  connection.setUrl(newUrl);
	  map.put(newUrl, connection);
	}
      }
    }

    /** 
     * Removes a connection with the given url.
     *
     * @param url   The url of the connection to remove.
     */
    public void remove(String url) {
      synchronized (lock) {
        map.remove(url);
      }
    }

    /**
     * Gets a connection manager for the specifed localized resource. 
     * The result will be a connection manager connected to the 
     * resource's host and port.  If no connection is cached, one will be
     * created.
     *
     * @param resource  The localized resource to get the connection 
     *                  manager for.
     * @param datagram  True if an unreliable datagram channel is desired.
     *
     * @throws ConnectionFailedException
     *         Indicates a failure to connect to the remote endpoint.
     */
    public ConnectionManager get(final LocalizedResource resource,
                                 final boolean datagram)
            throws ConnectionFailedException {
      return get(resource.host, resource.port, datagram);
    }
  
    /** 
     * Gets a connection manager with the specified remote endpoint.  If
     * no connection is cached, one will be created.
     * 
     * @param host     The remote host name.
     * @param port     The remote port number.
     * @param datagram True if an unreliable datagram channel is desired.
     * 
     * @return A connection manager connected to the given remote endpoint.
     *
     * @throws ConnectionFailedException
     *         Indicates a failure to connect to the remote endpoint.
     */
    public ConnectionManager get(final String host, final int port,
                                 final boolean datagram) 
            throws ConnectionFailedException {

      ConnectionManager connection;
      boolean bindConnection = false;
      String url = null;

      try {
         url = getUrl(host, port, datagram);
      } catch (UnknownHostException x) {
        throw new ConnectionFailedException(
                "Connection to " + host + ":" + port + " failed: "
                + "Unknown host", x);
      }

      synchronized (lock) {
        connection = (ConnectionManager) map.get(url);
      
        if (connection == null) {
	  // Get and put a connection under the lock to avoid the creation
	  // of multiple connections to sand endpoint.

          connection = new ConnectionManager(url);

	  // Only add the connection if it wasn't already revoked.  The
	  // particular concern here is that the entire connection
	  // creation process, including binding a NetworkIO channel, was
	  // performed in this thread and failed.
	  
	  if (!(connection.isRevoked())) {
            map.put(url, connection);
	  }

	  // The new connection needs binding.
	  bindConnection = true;
        }
      }

      // If necessary, we bind the connection outside of the cache lock.
      // (Connection binding may take a long time, for instance if
      // connecting to a listening but unresponsive port such as 23
      // (reserved for telnet).
      if (bindConnection) {
        connection.bind();
      }

      return connection;
    }
  
    /** 
     * Revokes all connections synchronously.
     *
     * @param resultHandler   A result handler.
     * @param operation        An operation, not being used concurrently
     *                         for anything else.
     */
    public void revoke(Synchronous.ResultHandler resultHandler,
                       Operation operation) {
      synchronized(map) {
        Iterator it = map.values().iterator();
        while (it.hasNext()) {
            ((ConnectionManager)it.next()).revoke(resultHandler, operation);
        }
	map.clear();
      }
    }
    
    /**
     * Generates a hash key for the given host name, port number, and
     * connection type.  The host name is first mapped to an IP address, 
     * to ensure that there is one connection per IP address.
     *
     * @param host   The host name.
     * @param port   The port number.
     * @param datagram  True for an unreliable datagram connection.
     * 
     * @return A string that is unique for each IP address and 
     *         port number.
     *
     * @throws UnknownHostException
     *         Indicates that the host name is unknown.
     */
    private String getUrl(final String host, final int port,
                          final boolean datagram) 
            throws UnknownHostException {
      return "sio://" 
             + (isIP(host) ? host
	                   : InetAddress.getByName(host).getHostAddress())
	     + ":" + port
	     + (datagram ? "?type=output" : "?type=client");
    }
  }

  /** Returns a string representation of this network I/O manager. */
  public String toString() {
    return "#[" + super.toString() + " on " + host + ":" + port + "]";
  }

  public static boolean isIP(String inputString) {  
    StringTokenizer tokenizer = new StringTokenizer(inputString, ".");  
      
    // Make sure there are 4 tokens
    if (tokenizer.countTokens() != 4) {
      return false;
    }
      
    // Make sure each token is a number between 0 and 255
    try {
      for (int i=0; i<4; i++) {
        String t = tokenizer.nextToken();       
	int chunk = Integer.parseInt(t);

        // assures that 0 <= chunk <= 255       
	if ((chunk & 255) != chunk) {
          return false;
	}
      }
    } catch (NumberFormatException e) {
      return false;
    }
    
    // Make sure there aren't any repeated "."s
    if (inputString.indexOf("..") >= 0) {
      return false;
    }

    return true;  
  }
}
