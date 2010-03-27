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

import one.world.Constants;
import one.world.Main;

import one.world.binding.BindingRequest;
import one.world.binding.BindingResponse;
import one.world.binding.Duration;
import one.world.binding.LeaseEvent;
import one.world.binding.LeaseDeniedException;
import one.world.binding.LeaseManager;
import one.world.binding.ResourceRevokedException;

import one.world.core.Component;
import one.world.core.ComponentDescriptor;
import one.world.core.Environment;
import one.world.core.Event;
import one.world.core.EventHandler;
import one.world.core.ExceptionalEvent;
import one.world.core.ExportedDescriptor;
import one.world.core.ImportedDescriptor;
import one.world.core.NestedConcurrencyDomain;
import one.world.core.Tuple;
import one.world.core.TupleException;
import one.world.core.UnknownEventException;

import one.world.util.AbstractHandler;
import one.world.util.Log;
import one.world.util.NullHandler;
import one.world.util.Operation;
import one.world.util.SystemUtilities;
import one.world.util.Timer;
import one.world.util.TupleEvent;
import one.world.util.TypedEvent;
//Uncomment for network profilling
/*
import one.util.CountOutputStream;
import one.util.CountInputStream;
*/

import one.util.Guid;

import java.io.*;

import java.net.*;

import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * A factory for network I/O clients and servers.  
 *
 * <p><b>Imported and Exported Event Handlers</b></p>
 * 
 * <p>Exported event handler:
 * <dl>
 *   <dt>bind</dt>
 *   <dd>Handles {@link BindingRequest}s by binding a new network I/O
 *   resource, either a {@link Client} or a {@link Server}.  The binding
 *   request must have a {@link SioResource} as its descriptor.  The
 *   resulting {@link BindingResponse} will have a new <code>Client</code>
 *   or <code>Server</code> as the resource and the
 *   <code>SioResource</code> as the descriptor.  The construction of a
 *   server may spin off further <code>BindingResponse</code>s containing
 *   <code>Client</code>s.  The descriptors for each of the
 *   clients will give the remote address and port in the form of a
 *   <code>SioResource</code>.  Exceptional conditions include 
 *   {@link java.net.UnknownHostException} and 
 *   {@link java.io.IOException}.</dd>
 * </dl></p>
 *
 * @version  $Revision: 1.61 $
 * @author   Janet Davis
 */
public class NetworkIO extends Component {
 
  ////////////// Static members ///////////////

  /** The component descriptor. */
  private static final ComponentDescriptor SELF = 
      new ComponentDescriptor("one.world.io.NetworkIO",
                              "A network I/O factory",
			      true);

  /** The exported event handler descriptor. */
  private static final ExportedDescriptor BIND = 
      new ExportedDescriptor(
              "bind",
	      "Event handler for the network I/O factory",
	      new Class[] { BindingRequest.class },
	      new Class[] { TupleException.class,
	                    UnknownEventException.class,
			    UnknownHostException.class,
			    IOException.class },
              false );

  /** Descriptor for the imported lease request handler. */
  private static final ImportedDescriptor LEASE =
      new ImportedDescriptor(
              "lease",
              "Lease request handler",
              new Class[] { LeaseEvent.class },
              new Class[] { LeaseDeniedException.class },
              false,
              true );

  /** 
   * The socket accept timeout.  Closing a socket or interrupting the
   * thread does not interrupt a call to the socket's accept() method.
   * There is a <a
   * href="http://developer.java.sun.com/developer/bugParade/bugs/4344135.html">bug
   * report</a> outstanding for this problem.
   */
  static final int SOCKET_ACCEPT_TIMEOUT = 100;

  /** The system log. */
  static final Log log;

  // Initialize the system log and lease manager variables.
  static {
    log = (Log)AccessController.doPrivileged(
      new PrivilegedAction() {
        public Object run() {
          return Log.getSystemLog();
        }
      });
  }

  //////////////// NetworkIO members  /////////////////////

  /** The network I/O factory event handler (exported). */
  final BindHandler bindHandler;

  /** The lease request handler (imported). */
  protected final Component.Importer leaseHandler;

  /** The timer component. */
  final Timer timer;

  /** Constructs a new network I/O factory. */
  public NetworkIO(Environment env) {
    super(env);

    // Export the binding handler.
    bindHandler = new BindHandler();
    declareExported(BIND, bindHandler);
 
    // Import the lease request handler.  This is linked in
    // one.world.Main.
    leaseHandler = declareImported(LEASE);

    // Get the timer component.
    timer = getTimer();
  }

  /** Gets the component descriptor. */
  public ComponentDescriptor getDescriptor() {
    return (ComponentDescriptor)SELF.clone();
  }


  /////////////// BindHandler ///////////////

  /** 
   * The event handler for the network I/O factory.  Handles
   * BindingRequests.
   */
  protected class BindHandler extends AbstractHandler {
  
    /**
     * <p>Handles events.  Accepts 
     * {@link one.world.binding.BindingRequest}s.  The resource descriptor
     * must be a {@link SioResource}.  Normally responds with a
     * {@link one.world.binding.BindingResponse} containing the new
     * datagram I/O client resource.</p>
     */
    public boolean handle1(Event event) {

      if (event instanceof BindingRequest) {
	bind((BindingRequest)event);
	return true;
      } 

      return false;
    }
  
    /** 
     * Binds a new reliable network resource and sends a {@link
     * one.world.binding.BindingResponse} to the requester.
     *
     * @param request       The original binding request.
     */
    protected void bind(BindingRequest request) {
      if (request.descriptor instanceof SioResource) {
        SioResource url = (SioResource)request.descriptor;

	int remotePort = url.remotePort;
	if (remotePort == -1) {
	  remotePort = Constants.PORT;
	}

	int localPort = url.localPort;
	if (localPort == -1) {
	  localPort = Constants.PORT;
	}

        switch (url.type) {
        case SioResource.CLIENT:
          bindClient(request, url.remoteHost, remotePort);
  	  return;
        case SioResource.SERVER:
          bindServer(request, url.localHost, localPort, 
  	                    url.closure, url.duration);
  	  return;
        default:
          Exception x = new UnknownEventException("Unsupported SIO type");
	  respond(request, x);
  	  return;
        }
      } else {
        Exception x = new UnknownEventException(
            "Unsupported descriptor type: " 
  	  + request.descriptor.getClass());
        respond(request, x);
      }
    }
  
  
    /** 
     * Binds a new reliable network client to the specified remote endpoint
     * and sends a {@link one.world.binding.BindingResponse} to the
     * requester.  Assumes that the request has been validated and that 
     * the address and port correspond to the requested resource descriptor.
     *
     * @param request       The original binding request.
     * @param host          The remote hostname or IP address.
     * @param port          The remote TCP port number.
     * @see   NetworkIO.Client
     */
    protected void bindClient(BindingRequest request, 
                              String host, int port) {
  
      Socket socket;
  
      try {
        socket = new Socket(InetAddress.getByName(host), port);
      } catch (IOException x) {
        respond(request, x);
        return;
      }
  
      bindClient(request, socket);
    }
  
    /** 
     * Binds a new reliable network client to the specified socket and 
     * sends a {@link one.world.binding.BindingResponse} to the
     * requester.
     *
     * <p>If the client was spun off a server, the descriptor contained in
     * the binding request is meaningless and a new one must be
     * constructed based on the socket's remote address and port.</p>
     *
     * @param request       The binding request.
     * @param socket        The TCP client socket.
     * @see   NetworkIO.Client
     */
    protected void bindClient(BindingRequest request,
			      Socket socket) {

      // If necessary, construct a new resource descriptor for the client.
      if (((SioResource)request.descriptor).type == SioResource.SERVER) {
        SioResource descriptor = new SioResource();
        descriptor.type = SioResource.CLIENT;
        descriptor.remoteHost = socket.getInetAddress().getHostAddress();
        descriptor.remotePort = socket.getPort();
        request = new BindingRequest(request.source, request.closure,
                                     descriptor, request.duration);
      }
  
      try {
        // Each client runs in its own nested concurrency domain.
        NestedConcurrencyDomain cDomain =
	    Environment.createNestedConcurrency(
	        "Network I/O client "
		+ socket.getInetAddress().getHostAddress() + ":"
		+ socket.getPort());
	                                        
	cDomain.animate(true, false);

        // Create the client.
        Client client = new Client(request.source,
	                           (SioResource)request.descriptor, 
				   socket, 
				   leaseHandler,
				   cDomain);
				   
        try {
          LeaseManager.acquire(request, 
	                       Environment.wrapForNested(client, cDomain),
			       client,
			       leaseHandler);
        } catch (IllegalStateException x) {
	  // System shutting down.  Let the network I/O client run in 
	  // the main thread.
	  LeaseManager.acquire(request, client, leaseHandler);
	}

      } catch (IOException x) {
        respond(request, x);
      }
    }
  
  
    /** 
     * Binds a new reliable network server to the specified local endpoint
     * and sends a {@link one.world.binding.BindingResponse} to the
     * requester.  Assumes that the request has been validated and that the 
     * address and port correspond to the requested resource descriptor.
     *
     * @param request       The original binding request.
     * @param host          The local hostname or IP address.
     * @param port          The local TCP port number.
     * @param clientClosure The closure to use when creating clients.
     * @param clientDuration  The default lease duration for the
     *                      clients spawned by the server.
     * @see   NetworkIO.Client
     * @see   NetworkIO.Server
     */
    protected void bindServer(BindingRequest request, 
                              String host, int port,
                              Object clientClosure, long clientDuration) {

      // Wrap the client request source to ensure the Acceptor thread
      // does not run code in any other component and is continuously 
      // available to accept new TCP connections.
      EventHandler source = wrap(request.source);

      try {
        // Create a new NetworkIO server, using the given BindingRequest
	// to generate BindingResponses.
        Server server = 
	    new Server(NetworkIO.this, 
	               (SioResource)request.descriptor,
                       new BindingRequest(source,
                                          clientClosure,
                                          request.descriptor,
                                          clientDuration),
                       host, port);

        // Acquire a lease for the server, returning the response to the 
	// original requestor.  (The original request is used here, not
	// the client binding request with the wrapped source.)
        LeaseManager.acquire(request, server, leaseHandler);

      } catch (IOException x) {
        respond(request, x);
      }
    }
  }


  ///////////// CloseMessage //////////
  
  /**
   * This empty class is used as a marker to indicate that the socket 
   * should be closed.  This is a workaround for the 
   * <a href="http://developer.java.sun.com/developer/bugParade/bugs/4344135.html">Linux 
   * HotSpot JVM socket bug</a>.
   */
  private static final class CloseMessage implements Serializable {};

  /** 
   * The revoke event is used internally to signal that the connection 
   * should be revoked.  It is necessary because the CloseMessage must be
   * written and the socket closed from the nested concurrency domain,
   * except during one.world shutdown.
   */
  private static final class RevokeEvent extends TypedEvent {

    public static final int REVOKE = 1;
    public static final int REVOKED = 2;
    public static final int TERMINATE = 3;
    public static final int TERMINATED = 4;

    public RevokeEvent() {}

    public RevokeEvent(EventHandler source, Object closure, int type) {
      super(source, closure, type);
    }
  }

  ///////////// Client //////////////

  /** 
   * A reliable network I/O client.  Implements structured I/O over a TCP
   * client socket, providing reliable, in-order delivery. 
   * Handles {@link SimpleInputRequest}s and {@link SimpleOutputRequest}s.
   *
   * <p>Network I/O clients generally should be obtained via a binding
   * request to the environment's request handler.</p>
   *
   * <p>Note that any exception on the underlying object stream or socket
   * will cause the resource to be revoked.</p>
   *
   * <p>Internal fields and methods are protected so that a subclass can
   * access or override any method, for instance to build an encrypted
   * network I/O client.</p> 
   *
   * @see SioResource
   * @see one.world.binding.BindingRequest
   * @see one.world.core.Environment
   */
  protected class Client extends AbstractHandler {

    /** 
     * The synchronous pending input requests manager.  This lets us
     * process tuples in order and only while someone wants the results.
     */
    SynchronousPendingInputRequests pending;

    /** The original requester of the resource. */
    protected EventHandler requester;

    /** The SioResource (for debugging only). */
    protected final SioResource sioResource;

    /** The lease manager to use for requesting leases. */
    protected EventHandler leaseHandler;
  
    /** The socket. */
    protected Socket socket;
  
    /** The object output stream for sending tuples. */
    protected ObjectOutputStream outputStream;

    /** 
     * The object input stream for receiving tuples.  We do NOT wrap this
     * stream in a lock because it is only read from in the 
     * {@link Listener} thread.  Furthermore, we must be able to interrupt
     * this thread by closing the socket.
     */
    protected volatile ObjectInputStream inputStream;

    /** The lock object. */
    protected Object lock; 

    /**
     * The concurrency domain for this client. 
     */
    private NestedConcurrencyDomain cDomain;

    /** 
     * The listener thread.  Subclasses should be able to manage the
     * thread using constructors and the {@link #revoke} method. 
     */
    private Thread listenerThread;

    /** The revokation event handler. */
    private final EventHandler revokationHandler;

    /** 
     * Indicates whether the resource has been revoked.  This should only
     * be set by the {@link #revoke} method, but subclasses may need to
     * read it to decide whether the resource is active.
     */
    protected volatile boolean isRevoked;

    /**
     * Create a new reliable network channel using the specified
     * socket, in the specified environment.  A thread is created to 
     * listen on the socket.
     *
     * @param  requester The source of the original resource request.
     * @param  sioResource  The SIO resource descriptor.
     * @param  socket    The TCP socket to use for the channel.
     * @param  leaseHandler  The lease manager used to acquire leases.
     * @param  cDomain        The nested concurrency domain for the client.
     * @throws IOException Signals an error in configuring the socket or
     *                   obtaining the I/O streams.
     */
    protected Client(EventHandler requester, 
                     SioResource sioResource,
                     Socket socket,
                     EventHandler leaseHandler,
		     NestedConcurrencyDomain cDomain) throws IOException { 

      // Set instance variables and obtain I/O streams.
      this.pending = new SynchronousPendingInputRequests();
      this.requester = requester;
      this.sioResource = sioResource;
      this.socket = socket;
      this.leaseHandler = leaseHandler;
      this.cDomain = cDomain;
      this.revokationHandler = new RevokationHandler();

      // To reduce latency when sending small amounts of data
      socket.setTcpNoDelay(true);
//Uncomment for network profilling.
/*
      outputStream = 
         new ObjectOutputStream(
	     new BufferedOutputStream(new CountOutputStream(socket.getOutputStream())));
*/
      outputStream = 
         new ObjectOutputStream(
	     new BufferedOutputStream(socket.getOutputStream()));
      outputStream.flush();
      lock = new Object();
   
      // Temporarily set the socket timeout so that the ObjectInputStream 
      // constructor will eventually time out if the other endpoint isn't
      // cooperating.
      // FIXME: Shorter timeout?
      socket.setSoTimeout((int)Constants.SYNCHRONOUS_TIMEOUT);
//Uncomment for network profilling.
/*
      inputStream = 
        new ObjectInputStream(
	    new BufferedInputStream(new CountInputStream(socket.getInputStream())));
*/
      inputStream = 
        new ObjectInputStream(
	    new BufferedInputStream(socket.getInputStream()));
      socket.setSoTimeout(0);

      isRevoked = false;

      // Start the listener.
      listenerThread = new Thread(new Listener(),
	                       "NetworkIO listener " 
                               + cDomain.getId() + ", "
			       + socket.getInetAddress().getHostAddress() 
			       + ":" + socket.getPort());
      listenerThread.setDaemon(true);
      listenerThread.start();

      if (Constants.DEBUG_NETWORK) {
        log.log(this, "Started");
      }
    }

    /**
     * Handles {@link SimpleInputRequest} and {@link SimpleOutputRequest} 
     * events.  A <code>SimpleOutputRequest</code> results in the tuple
     * being sent over the network channel.
     * <code>SimpleInputRequests</code> will wait for results until the
     * timeout expires.
     *
     * <p>Also handles {@link one.world.binding.LeaseEvent#CANCELED}
     * events by revoking the resource.</p> 
     *
     * @param event   The event to handle.
     */
    public boolean handle1(Event event) {

      // Deal with lease cancelation events first so that repeated cancels
      // won't trigger a ResourceRevokedException.
      if (event instanceof LeaseEvent) {
        if (((LeaseEvent)event).type == LeaseEvent.CANCELED) {
	  revoke();
	  return true;
	}
      }

      // Validate the event.
      if (isNotValid(event)) {
        return true;
      }

      // If the resource has been revoked, respond with a 
      // ResourceRevokedException.
      // Otherwise, handle the event.
      
      if (isRevoked) {
        respond(event, new ResourceRevokedException());
	return true;

      } else if (event instanceof SimpleOutputRequest) {
        put((SimpleOutputRequest)event);
	return true;

      } else if (event instanceof SimpleInputRequest) {
        SimpleInputRequest request = (SimpleInputRequest)event;
        PendingRequest pr = new PendingRequest(request, this, pending);
	pending.add(pr);
	pr.requestLease(leaseHandler, request.duration);
	return true;
      } 

      return false;
    }
  
    /** 
     * Handles a put request by writing the tuple to the network
     * channel.
     *
     * <p>An I/O exception indicates that the output stream,
     * {@link #outputStream}, has been closed.  This is an abnormal 
     * condition. An error is logged and the resource is revoked.</p>
     *
     * @param     request The output request.
     */
    protected void put(final SimpleOutputRequest request) {

      // THIS IS NOT THREAD SAFE.  This and revoke1() are assumed to
      // always run in the thread for the nested concurrency domain
      // (except during shutdown, when only the main thread is active).

      if (isRevoked) {
        respond(request, new ResourceRevokedException());

      } else {
        try {
	
          // Write the tuple to the output stream.  This always happens in
	  // the same thread, so no lock is necessary.
          outputStream.writeObject(request.tuple);
  	  outputStream.flush();

	  // Reset the stream to ensure the tuple is actually written
	  // each time.
	  outputStream.reset();

	  if (Constants.DEBUG_NETWORK) {
	    log.log(this, "Sent " + request.tuple);
	  } 

          respond(request, 
                  new OutputResponse(this, request.closure,
                                     (Guid)request.tuple.get("id")));

        } catch (InvalidClassException x) {
          // The class cannot be serialized.
          respond(request, x);

        } catch (NotSerializableException x) {
          // The class is not serializable.
          respond(request, x);

        } catch (Throwable x) {
          // Who knows what happened?  Probably nothing good.
	  // It's safe to revoke in this thread.
          revoke1(); 
          respond(request, x);
        }
      }
    }

    /** 
     * The revokation handler.  This is triggered from the {@link
     * #revoke()} method.  It is necessary for two reasons:
     *
     * <ol>
     *   <li>The {@link #revoke1()} method must be run from within the 
     *       nested concurrency domain to safely access the socket.</li>
     *   <li>The {@link #terminate()} method, which terminates the nested
     *       concurrency domain, must be run outside the nested concurrency
     *       domain to avoid deadlock.</li>
     * </ol>
     */ 
    protected final class RevokationHandler extends AbstractHandler {
      /** Handles events. */
      protected boolean handle1(Event e) {
        if (e instanceof RevokeEvent) {
	  RevokeEvent re = (RevokeEvent)e;
	  switch (re.type) {

	  case RevokeEvent.REVOKE:
	    // Really revoke the resource.
	    revoke1();
	    respond(re, new RevokeEvent(this, null, RevokeEvent.REVOKE));

	    // Request that the nested concurrency domain be terminated,
	    // using an event sent into the environment's main concurrency
	    // domain.  We do this from here instead of from the REVOKED
	    // case below because there is the possibility of the REVOKED
	    // event getting lost.
	    requestTermination();

	    return true;

	  case RevokeEvent.REVOKED:
	    // Nothing to do.
	    return true;
             
	  case RevokeEvent.TERMINATE:
	    // Terminate the nested concurrency domain.  (This should be
	    // done from the main environment's thread!)
	    terminate();
	    respond(re, new RevokeEvent(this, null, RevokeEvent.TERMINATED));
	    return true;
	    
	  case RevokeEvent.TERMINATED:
	    // Nothing to do.
	    return true;
	  }

	} else if (e instanceof ExceptionalEvent) {
	  Throwable x = ((ExceptionalEvent)e).x;

	  if (x instanceof ResourceRevokedException) {
	    // No sweat.
	    return true;
	  }
	}

	return false;
      }
    }

    /**
     * Revokes the network I/O client resource.  Thread safe.
     *
     * <p>If the system is shutting down, actually revokes the resource.  
     * Otherwise, queues a <code>RevokeEvent</code> in the nested concurrency 
     * domain.</p>
     */
    protected void revoke() {
    
      if (Main.SHUTDOWN == Main.getStatus()) {
        // If the system is shutting down, really revoke the resource.
	// Note that there is no need to terminate the nested concurrency
	// domain.
	
        revoke1();

      } else {
        EventHandler h = null;
        synchronized (lock) {
	  if (!isRevoked) {
	    // Wrap the revokation handler to run within the nested
	    // concurrency domain (where it is safe to handle the socket).
	    h = Environment.wrapForNested(revokationHandler, cDomain);
          }
	}

	if (h != null) {
	  if (Constants.DEBUG_NETWORK) {
	    log.log(this, "Queueing revoke event");
	  }

          // Use an operation with many retries and a short timeout, to
	  // virtually ensure delivery of the event.
	  new Operation(Integer.MAX_VALUE, Duration.SECOND, timer, h,
	                revokationHandler)
	      .handle(new RevokeEvent(this, null, RevokeEvent.REVOKE));
	}
      }
    }
    
    /**
     * Revokes the network I/O client resource.  Not thread safe; writes
     * to and closes the socket without a lock.  This method must be
     * executed either from the nested concurrency domain thread or during
     * system shutdown
     *
     * <p>Clean-up includes interrupting and nullifying the listener 
     * thread, and closing and nullifying the streams and socket. May be
     * called repeatedly without ill effects.</p>
     */
    protected void revoke1() {
      
      if (isRevoked) {
        return;
      }

      isRevoked = true;

      // Nullify the listener thread.
      listenerThread = null;

      // Close and nullify the output stream.
      try {
	// Send a close message to break the other socket out of its 
	// read before closing the socket.
	outputStream.writeObject(new CloseMessage());
	outputStream.flush();
	outputStream.close();

      } catch (SocketException x) {
	// Under Windows, SocketExceptions indicate that the socket is
	// already closed.  No need to do anything.

      } catch (IOException x) {
	// The broken pipe exception indicates under Linux that the
	// socket is already closed.  StreamCorruptedException does
	// the same on Windows.  If it's anything else, log a warning.
	if (!(("Broken pipe".equalsIgnoreCase(x.getMessage()))
	     || (x instanceof StreamCorruptedException))) {
	  log.logWarning(this, 
	                 "Unexpected exception while closing socket", x);
        }
      }

      outputStream = null;
      inputStream = null;

      // Close and nullify the socket.  Closing the socket will cause
      // the socket read in the {@link Listener} thread to be
      // interrupted.
      try {
        socket.close();
      } catch (IOException x) {
        log.logError(this, 
                     "Exception while closing the socket",
                     x);
      } 
      socket = null;

      if (Constants.DEBUG_NETWORK) {
        log.log(this, "Revoked");
      }
    }


    /**
     * Request that the nested concurrency domain be revoked.  We do this 
     * using an event because we are currently running from the nested 
     * concurrency domain, and the termination must happen within the 
     * environment's concurrency domain to avoid deadlock. 
     */
    protected void requestTermination() {
      if (Main.getStatus() != Main.SHUTDOWN) {
	
	// Wrap the revokation handler for the environment's concurrency
	// domain.
        EventHandler h = wrap(revokationHandler);
  
        if (Constants.DEBUG_NETWORK) {
          log.log(this, "Queueing terminate event");
	}

	// Use an operation with many retries and a short timeout to
	// virtually ensure the event will be received.
        new Operation(Integer.MAX_VALUE, Duration.SECOND, timer, h, h)
	    .handle(new RevokeEvent(null, null, RevokeEvent.TERMINATE));
      }
    }

    /** 
     * Terminates the nested concurrency domain.  This should run in the
     * main environment's concurrency domain; i.e., outside the nested
     * concurrency domain.
     */
    protected void terminate() {
      synchronized (lock) {
        if (cDomain != null) {
	  cDomain.terminate();
	  cDomain = null;
          if (Constants.DEBUG_NETWORK) {
	    log.log(this, "Terminated nested concurrency domain");
	  }
        }
      }
    }

    /**
     * Listens for data arriving from the network.  Arriving tuples are
     * instantiated and passed to the tuple handler.
     * 
     * <p>This is a protected inner class because it requires access to the
     * {@link #socket} and the pending input request manager handlers
     * contained in the <code>Client</code> object.</p>
     */
    protected class Listener implements Runnable {
      
      /**
       * Listens on the network for arriving tuples.  These tuples are 
       * passed to the pending query store for processing.
       *
       * <p>Any exception on the {@link java.io.ObjectInputStream} is
       * fatal and causes the resource to be revoked.</p>
       */
      public void run() {
        ObjectInputStream in;
	Object object;
        TupleEvent event;

        while (true) {
          try {
	    // Get a reference to the input stream.
            in = null;

	    if (!isRevoked) {
              in = inputStream;
            }

            if (in == null) {
	      // The resource has been revoked.
	      return;
	    } else {
              // Otherwise, read object from socket.
              // If the socket is closed, we will be interrupted with a
              // SocketException.
              object = in.readObject();
	    }

          } catch (SocketException x) {
	    // The socket has been closed.
	    revoke();
	    return;

	  } catch (EOFException x) {
            // The connection was closed from the other end.  Revoke the
	    // resource.
	    revoke();
	    return;

          } catch (ClassNotFoundException x) {
            // Could not instantiate object because class was not found.
            log.logError(Client.this, 
                   "Received object with unknown class; revoking resource",
                   x);
            revoke();
	    return;

          } catch (IOException x) {
	    // All other I/O exceptions.
            log.logError(Client.this, 
	                 "Unexpected I/O exception; revoking resource",
			 x);
            revoke();
	    return;

          } catch (Throwable x) {
	    log.logError(Client.this,
	                 "Unexpected exception; revoking resource",
			 x);
            revoke();
	    return;
	  }

	  if (Constants.DEBUG_NETWORK) {
	    log.log(Client.this, "Received " + object);
	  }

          // Run the tuple through the pending requests.  Only one tuple
	  // may be processed at a time.
          try {
	    pending.filter((Tuple)object);
	    Thread.yield();
          } catch (ClassCastException x) {
	    if (object instanceof CloseMessage) {
	      /** The remote endpoint is telling us to close. */
	      revoke();
	      return;
	    } else {
	      log.logWarning(this, "Received non-tuple object", x);
	    }
          }
        }
      }
    }

    /** Returns a string representation of this network I/O client. */
    public String toString() {
      return "#[" + super.toString() + " for " + sioResource + "]";
    }
  }

  /** 
   * Accepts connections at a local IP address and TCP port.  Each
   * connection is bound to a new {@link NetworkIO.Client}.
   *
   * <p>This implements <CODE>EventHandler</CODE> so that it may return
   * itself as the source of events.  It is a component so that it may
   * run in a more privileged environment than the user of the 
   * service.</p>
   *
   * <p>Member variables and methods are protected so that a subclasses
   * may be implemented to correspond with subclasses of 
   * {@link NetworkIO} and {@link NetworkIO.Client}.</p>
   */
  protected static class Server extends AbstractHandler {

    /** The network I/O factory that created this. */
    protected NetworkIO factory;

    /**
     * A binding request to use when binding new {@link NetworkIO.Client}s.
     * It contains the original binding request source and
     * descriptor, along with the client closure and the default client 
     * lease length. 
     */
    protected BindingRequest clientRequest;

    /** The server socket. */
    protected ServerSocket serverSocket;

    /** The server socket thread. */
    protected Thread serverThread;

    /** The SioResource (for debugging only). */
    protected final SioResource sioResource;

    /** Indicates whether the resource has been revoked. */
    protected volatile boolean isRevoked;
    
    /** 
     * The revokation lock.  This is used to protect the 
     * {@link #isRevoked} flag and the {@link #serverSocket}. 
     */
    protected Object lock;

    /** The host name (for debugging) */
    private String host;

    /** The port number (for debugging) */
    private int port;


    /** 
     * Creates a new network I/O server at the specified address and port,
     * with the specified client binding request.
     * Starts a thread which accepts connections and spawns new 
     * {@link NetworkIO.Client}s.
     *
     * @param  networkIO The network I/O factory that created this object.
     * @param  sioResource The SIO resource descriptor.
     * @param  clientRequest   The binding request for new clients.
     * @param  host      The local host name to use.  (localhost 
     *                   signifies any local host name.)
     * @param  port      The local TCP port number.
     *
     * @throws UnknownHostException 
     *                   Signals that the host name is unknown.
     * @throws IOException Signals an error in establishing the server
     *                     socket.
     */
    protected Server(NetworkIO networkIO, 
                     SioResource sioResource,
                     BindingRequest clientRequest, 
                     String host, int port)
            throws IOException, UnknownHostException {

      // Keep a reference to the network I/O factory and the  new client 
      // request; we will use them when binding new clients on accepted 
      // connections.
      this.factory = networkIO;
      this.sioResource = sioResource;
      this.clientRequest = clientRequest;

      isRevoked = false;
      lock = new Object();

      // Obtain and configure the socket.
      if ("localhost".equalsIgnoreCase(host)) {
        // Special case for localhost so that it will be any local
	// IP address rather than loopback
        serverSocket = new ServerSocket(port, 50);
      } else {
        serverSocket = 
	    new ServerSocket(port, 50, InetAddress.getByName(host));
      }
      serverSocket.setSoTimeout(SOCKET_ACCEPT_TIMEOUT);

      this.host = host;
      this.port = port;

      // Start a thread to accept connections.
      serverThread = new Thread(new Acceptor(),
                                "NetworkIO acceptor, " + host + ":"
				+ serverSocket.getLocalPort());
      serverThread.setDaemon(true);
      serverThread.start();

      if (Constants.DEBUG_NETWORK) {
	log.log(this, "Started");
      }
    }

    /**
     * Handles {@link one.world.binding.LeaseEvent#CANCELED} events. 
     */
    public boolean handle1(Event event) {

      // Check to see if this resource is revoked.
      if (isRevoked) {
        respond(event, new ResourceRevokedException());
	return true;
      }

      if (event instanceof LeaseEvent) {
        if (((LeaseEvent)event).type == LeaseEvent.CANCELED) {
	  revoke();
	  return true;
	}
      }

      return false;
    }

    /**
     * Revokes the server resource.  Interrupts and nullifies the server
     * thread; closes and nullifies the socket.
     */
    protected void revoke() {

      boolean wasRevoked;

      if (isRevoked) {
        return;
      }

      synchronized (lock) {
        wasRevoked = isRevoked;
	isRevoked = true;
      }

      if (!wasRevoked) {

	serverThread.interrupt();

        // Close the server socket.
        try {
          serverSocket.close();
        } catch (IOException x) {
          log.logError(this, "Got I/O exception while closing socket", x);
        }
        serverSocket = null;

        // Wait for the acceptor thread to die before returning so that we
	// know the socket is really closed.
        try {
	  serverThread.join();
        } catch (InterruptedException x) {
	}
        serverThread = null;

	if (Constants.DEBUG_NETWORK) {
	  log.log(this, "Revoked");
	}
      }
    }
  
    /**
     * <p>Accepts connections on the {@link #serverSocket} and
     * creates new {@link NetworkIO.Client}s.  Requires access to 
     * {@link NetworkIO.bindClient}.</p>
     */
    private class Acceptor implements Runnable {

      /**
       * Accept connections and bind new {@link NetworkIO.Client}s.
       */
      public void run() {

        ServerSocket s;
        Socket socket;

        while (true) {

          s = null;
          synchronized (lock) {
            if (!isRevoked) {
              s = serverSocket;
            }
          }

          if (s == null) {
            // The resource has been revoked.
            return;
          }

          try {
            // Accept a connection.  If the socket is closed, we will
            // be interrupted with a SocketException.
            socket = s.accept();

	    if (Constants.DEBUG_NETWORK) {
	      log.log(Server.this,
	              "Accepted connection from "
	              + socket.getInetAddress().getHostAddress()
		      + ":" + socket.getPort());
	    }
	    
            factory.bindHandler.bindClient(clientRequest, socket);
         
	  } catch (InterruptedIOException x) {
	    if (isRevoked) {
	      return;
            }
	    continue;

          } catch (SocketException x) {
            // The server socket is closed.
            return;

          } catch (IOException x) {
            // I have no idea what other I/O exceptions I could get.
            log.logError(Server.this, "Unexpected I/O exception", x);
            revoke();
            return;
          }
        }
      }
    }

    /** Returns a string representation of this network I/O server. */
    public String toString() {
      return "#[" + super.toString() + " for " + sioResource + "]";
    }
  }
}
