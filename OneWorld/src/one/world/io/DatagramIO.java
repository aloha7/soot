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

import one.world.core.*;

import one.world.binding.BindingRequest;
import one.world.binding.BindingResponse;
import one.world.binding.LeaseEvent;
import one.world.binding.LeaseManager;
import one.world.binding.LeaseDeniedException;
import one.world.binding.ResourceRevokedException;

import one.world.util.AbstractHandler;
import one.world.util.Log;
import one.world.util.TupleEvent;

import one.util.BufferOutputStream;
import one.util.Guid;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OptionalDataException;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.MulticastSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * A factory for datagram I/O clients and servers.  
 *
 * <p><b>Imported and exported event handlers</b></p>
 *
 * <p>Exported event handler:
 * <dl>
 *   <dt>bind</dt>
 *   <dd>Handles {@link BindingRequest}s by binding a new datagram I/O
 *   client. The binding request must have a {@link SioResource} as its 
 *   descriptor.</dd>
 * </dl></p>
 * 
 * @version  $Revision: 1.46 $
 * @author   Janet Davis
 */
public class DatagramIO extends Component {
	
	//2010-03-27:
	public static int getPacketWriteCount(){
		return 1;
	}
	
	public static int getNormalPacketWriteCount(){
		return 1;
	}
	
	public static void clearWriteCounts(){
		
	}
	
	public static int getPacketReadCount(){
		return 1;
	}
	
	public static int getNormalPacketReadCount(){
		return 1;
	}
	
	public static void clearReadCounts(){
		
	}
  // FIXME:
  // See <A
  // HREF="http://developer.java.sun.com/developer/bugParade/bugs/4361783.html">
  // Bug #436178</A>, which states that ICMP port unreachable messages are
  // incorrectly reflected via the socket interface as SocketExceptions.
  // (This will be fixed in the JDK 1.4.)  I believe what I've got here
  // works, but keep an eye on it.

  /////////////// Static members //////////////////////

  /** The component descriptor. */
  private static final ComponentDescriptor SELF = 
      new ComponentDescriptor("one.world.io.DatagramIO",
                              "A datagram I/O factory",
                              true);

  /** Descriptor for the exported bind handler. */
  private static final ExportedDescriptor BIND = 
      new ExportedDescriptor(
              "bind",
              "Event handler for the datagram I/O factory",
              new Class[] { BindingRequest.class },
              new Class[] { TupleException.class,
                            UnknownEventException.class,
                            UnsupportedOperationException.class,
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
   * The socket timeout in milliseconds.  A socket timeout is used to poll
   * for thread interruption.
   */
  private static final int SOCKET_TIMEOUT = 100;

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


  ///////////// DatagramIO members /////////////////

  /** The datagram I/O factory event handler (exported). */
  private EventHandler bindHandler;

  /** The lease request handler (imported). */
  protected Component.Importer leaseHandler;

  /** Constructs a new datagram I/O factory. */
  public DatagramIO(Environment env) {
    super(env);

    // Export the binding handler.
    bindHandler = new BindHandler();
    declareExported(BIND, bindHandler);

    // Import the lease request handler.  It is linked in one.world.Main.
    leaseHandler = declareImported(LEASE);
  }

  /** Gets the component descriptor. */
  public ComponentDescriptor getDescriptor() {
    return (ComponentDescriptor)SELF.clone();
  }

  ///////////// Handler ///////////////

  /**
   * The event handler for the datagram I/O factory.  Handles binding
   * requests.
   */
  protected class BindHandler extends AbstractHandler {
  
    /**
     * Handles events.  Accepts 
     * {@link one.world.binding.BindingRequest}s 
     * containing {@link SioResource}s.  Normally responds with a
     * {@link one.world.binding.BindingResponse} containing the new
     * datagram I/O client resource.
     */
    public boolean handle1(Event event) {
  
      if (event instanceof BindingRequest) {
        bind((BindingRequest)event);
	return true;
      }

      return false;
    }

    /**
     * Binds a new unreliable network client according to the specified
     * binding request.
     *
     * @param request  The binding request.
     */
    protected void bind(BindingRequest request) {
  
      // Make sure descriptor is OK.
      if (!(request.descriptor instanceof SioResource)) {
        respond(request,
            new UnknownEventException("Descriptor is not a SioResource"));
        return;
      }
  
      SioResource url = (SioResource)request.descriptor;
  
      try {
        url.validate();
      } catch (TupleException x) {
        respond(request, x);
        return;
      }

      int localPort = url.localPort;
      if (localPort == -1) {
        localPort = Constants.PORT;
      }

      int remotePort = url.remotePort;
      if (remotePort == -1) {
        remotePort = Constants.PORT;
      }
  
      bind(request, url.type, 
           url.localHost, localPort,
           url.remoteHost, remotePort);
    }
  
    /**
     * Binds a new unreliable network client according to the specified
     * channel type, local endpoint, and remote endpoint.  These are
     * assumed to be consistent with the given binding request.  Normally,
     * a {@link one.world.binding.BindingResponse} is sent to the
     * requester.
     *
     * <p>Responds with an {@link one.world.core.ExceptionalEvent}
     * containing an {@link java.net.UnknownHostException} if unable to
     * get an IP address for the host, or an {@link java.io.IOException} 
     * if unable to create or connect a socket.</p>
     *
     * <p>Channel types are {@link SioResource#OUTPUT},
     * {@link SioResource#INPUT},
     * {@link SioResource#DUPLEX}, and
     * {@link SioResource#MULTICAST}.</p>
     */
    protected void bind(BindingRequest request, int type,
                        String localHost, int localPort,
  		        String remoteHost, int remotePort) {
  
      // Do the binding according to the request type.
      try {
        switch (type) {
        case SioResource.OUTPUT:
          {
            InetAddress remoteAddress = InetAddress.getByName(remoteHost);
            DatagramSocket socket = new DatagramSocket();
            socket.connect(remoteAddress, remotePort);
            bind(request, type, socket, remoteAddress, remotePort);
          }
          break;

        case SioResource.INPUT:
          {
	    DatagramSocket socket;
	    if ("localhost".equalsIgnoreCase(localHost)) {
	      // Special case for localhost so that it will be any local
	      // IP address rather than loopback
	      socket = new DatagramSocket(localPort);
	    } else {
              InetAddress localAddress = InetAddress.getByName(localHost);
              socket = new DatagramSocket(localPort, localAddress);
            }
            socket.setReceiveBufferSize(100000);
	    bind(request, type, socket, null, -1);
          }
          break;

        case SioResource.DUPLEX:
          {
            DatagramSocket socket;
            InetAddress remoteAddress = InetAddress.getByName(remoteHost);
	    if ("localhost".equalsIgnoreCase(localHost)) {
	      // Special case for localhost so that it will be any local
	      // IP address rather than loopback
	      socket = new DatagramSocket(localPort);
	    } else {
              InetAddress localAddress = InetAddress.getByName(localHost);
              socket = new DatagramSocket(localPort, localAddress);
            }
            socket.connect(remoteAddress, remotePort);
            bind(request, type, socket, remoteAddress, remotePort);
          }
          break;

        case SioResource.MULTICAST:
          {
            InetAddress groupAddress = InetAddress.getByName(remoteHost);
            MulticastSocket socket = new MulticastSocket(remotePort);
            socket.joinGroup(groupAddress);
            bind(request, type, socket, groupAddress, remotePort);
          }
          break;

        }
      } catch (IOException x) {
        respond(request, x);
      }
    }
  
    /**
     * Binds a new unreliable network client to the specified socket 
     * and leases it.  Either a binding response or an exceptional event
     * will be sent to the requester.
     * 
     * @param request      The binding request.
     * @param type         The resource type (see {@link SioResource}.)
     * @param socket       The UDP socket.
     * @param address      The remote IP address.
     * @param port         The remote UDP port.
     * @see   DatagramIO.Client
     */
    protected void bind(BindingRequest request, int type,
                        DatagramSocket socket,
                        InetAddress address, int port) {
      Client client;
  
      // Set up the client.
      try {
        client = new Client(getEnvironment(), leaseHandler, 
	                    request.source, (SioResource)request.descriptor, 
			    socket, address, port);
      } catch (IOException x) {
        respond(request, x);
        return;
      }

      // Attempt to obtain a lease.
      LeaseManager.acquire(request, client, leaseHandler);
    }
  }


  ///////////// Client ///////////////

  /**
   * An unreliable network I/O client.  Implements structured I/O over
   * a UDP socket.  The communication channel is accessed by passing 
   * {@link SimpleInputRequest}s and {@link SimpleOutputRequest}s
   * to the <code>handle</code> method.
   *
   * <p>Datagram I/O clients generally should be obtained via a binding 
   * request to the environment's request handler.</p>
   *
   * <p>Internal fields and methods are protected so that a subclass can
   * access or override any method, for instance to build an encrypted
   * network I/O client.</p>
   *
   * @see SioResource
   * @see one.world.binding.BindingRequest
   * @see one.world.core.Environment
   */
  public static class Client extends AbstractHandler {

    /** The original requester of this resource. */
    protected EventHandler requester;

    /** The wrapper for the pending input requests manager. */
    protected PendingInputRequests.Wrapper pending;

    /**
     * The UDP socket for network communication.  This is protected so that
     * descendents may configure the socket differently or directly access
     * the socket. 
     */
    protected DatagramSocket socket;

    /** The remote IP address. */
    protected InetAddress address;

    /** The remote UDP port number. */
    protected int port;

    /** The SioResource (for debugging only). */
    protected SioResource sioResource;

//Uncomment for profilling
/*
    static Object countOutLock = new Object();
    static int packetWrites;
    static int normalPacketWrites;

    static Object countInLock = new Object();
    static int packetReads;
    static int normalPacketReads;

    boolean isNormal;  
*/

    /** 
     * The listener thread.  Subclasses should be able to manage the
     * thread using constructors and the {@link #revoke} method.
     */
    private Thread listenerThread;

    /**
     * Indicates whether the resource has been revoked.  This should only
     * be set by the {@link #revoke} method, but subclasses may need to
     * read it to decide whether the resource is active.
     */
    volatile protected boolean isRevoked = false;

    /**
     * The lock for the {@link #isRevoked} flag and the {@link #socket}.
     */
    protected Object lock = new Object();
 
    /** 
     * Create a new unreliable network channel on top of the specified
     * datagram socket, with the specified requester.
     *
     * @param env        The environment in which to create the 
     *                   pending input request manager.
     * @param leaseHandler The lease request handler.
     * @param requester  The original requester of this resource.
     * @param sioResource The sio resource.
     * @param socket     The socket to use.
     * @param address    The remote IP address.
     * @param port       The remote UDP port number.
     * @throws IOException Indicates an error in configuring the socket.
     */
    protected Client(Environment env, EventHandler leaseHandler,
                     EventHandler requester,  SioResource sioResource,
                     DatagramSocket socket, InetAddress address, int port)
          throws IOException {

      // Obtain a pending input request manager.
      pending = new PendingInputRequests(env, leaseHandler).getWrapper();

      // Initialize member variables.
      this.requester = requester;
      this.address = address;
      this.port = port;
      this.sioResource = sioResource;
     
      // Configure the socket.
      this.socket = socket;
      this.socket.setSoTimeout(SOCKET_TIMEOUT);

//Uncomment for profilling
/*       
      if ((port != Constants.DISCOVERY_ANNOUNCE_PORT) && 
          (port != Constants.DISCOVERY_ELECTION_PORT)) {
        isNormal = true;
      } else {
        isNormal = false;
      } 
*/
      // Set up the listener thread.
      if (sioResource.type != SioResource.OUTPUT) {
        listenerThread = new Thread(new Listener(),
                                    "DatagramIO listener, localhost:"
				    + socket.getLocalPort() + ", "
				    + address + ":" + port);
        listenerThread.setDaemon(true);
        listenerThread.start();
      }

      if (Constants.DEBUG_NETWORK) {
        log.log(this, "Started");
      }
    }

    /**
     * Handles {@link SimpleInputRequest} and 
     * {@link SimpleOutputRequest} events.   A 
     * <code>SimpleOutputRequest</code> results in the tuple being sent 
     * over the network channel.  <code>SimpleInputRequests</code> will 
     * wait for results until the timeout expires.
     *
     * <p>Also handles {@link one.world.binding.LeaseEvent#CANCELED} 
     * events by revoking the resource.</p>
     *
     * @param event The event to handle.
     */
    protected boolean handle1(Event event) {
     
      // Handle lease cancelation events first so that we won't send a
      // ResourceRevokedException in response.
      if (event instanceof LeaseEvent) {
        if (((LeaseEvent)event).type == LeaseEvent.CANCELED) {
	  revoke();
	}
        return true;
      }

      // Validate the event. 
      if (isNotValid(event)) {
        return true;
      }

      // Make sure this has not been revoked.
      // Otherwise, handle the event.
      if (isRevoked) {
        respond(event, new ResourceRevokedException());
        return true;

      } else if (event instanceof SimpleOutputRequest) {
        put((SimpleOutputRequest)event);
	return true;

      } else if (event instanceof SimpleInputRequest) {
        pending.leasedRequestHandler.handle(event);
	return true;
      } 

      return false;
    }

    /** 
     * Handles a put request by writing the tuple to the network.  Any I/O
     * exceptions are sent to the requester as ExceptionalEvents.
     * Exceptions on the underlying socket are assumed to be fatal; the
     * resource is revoked.
     *
     * @param     request The output request.
     */
    void put(final SimpleOutputRequest request) {

      // If socket isn't connected, can't send.
      if ((address == null) || (port < 0)) {
        Exception x = new UnsupportedOperationException(
                              "Cannot send tuple on input-only channel");
	respond(request, x);
        return;
      }
      
      // Send the tuple over the network.
      BufferOutputStream outBuffer;
      ObjectOutputStream out;
      DatagramPacket packet;

      try {
        // Serialize the data.  (We use a BufferOutputStream rather than a
	// ByteArrayOutputStream in order to avoid copying the byte
	// array.)
        outBuffer = new BufferOutputStream();
        out = new ObjectOutputStream(outBuffer);
        out.writeObject(request.tuple);
	out.flush();
      } catch (Throwable x) {
        // Send exception to requester.
        respond(request, x);
	return;
      }
//Uncomment if profilling
/*
      synchronized(countOutLock) {
        packetWrites++; 
        if (isNormal) {
          normalPacketWrites++; 
        }
      } 
*/      
      // Construct the packet
      packet = new DatagramPacket(outBuffer.getBytes(), 
                                  outBuffer.size(),
                                  address, port);

      // Get a reference to the socket.
      DatagramSocket s = null;
      synchronized (this) {
        if (!isRevoked) {
	  s = socket;
        }
      }

      if (s == null) {
        respond(request, new ResourceRevokedException());
	return;
      }

      try {
        // Send the packet.
	s.send(packet);

        // Raise the response event.
        Event response = new OutputResponse(this, request.closure,
                                            (Guid)request.tuple.get("id"));
        respond(request, response);

        if (Constants.DEBUG_NETWORK) {
          log.log(this, "Sent " + request.tuple);
        }

      } catch (IOException x) {
        if (isRevoked) {
          respond(request, new ResourceRevokedException());
	} else {
          respond(request, x);
	  revoke();
	}
      }
    }

    /**
     * Revokes the datagram I/O resource.
     * 
     * <p>Clean-up includes joining the listener thread, closing the
     * socket, and setting all member variables to null.  This method may
     * be called repeatedly without ill effects.</p>
     */
    protected void revoke() {
      boolean wasRevoked;

      synchronized (lock) {
        wasRevoked = isRevoked;
        isRevoked = true;
      }

      if (!wasRevoked) {

        // Stop and nullify the listener thread.
	if (listenerThread != null) {
          listenerThread.interrupt();
	  try {
	    listenerThread.join();
	  } catch (InterruptedException x) {
	  }
          listenerThread = null;
	}

        // Close and nullify the socket.
	synchronized (lock) {
          socket.close();
          socket = null;
	}

        if (Constants.DEBUG_NETWORK) {
          log.log(this, "Stopped");
        }
      }
    }
  
    /**
     * Listens for data arriving from the network.  Arriving tuples are
     * instantiated and passed to the incoming tuple handler
     * using a {@link one.world.util.TupleEvent}.  Exceptions are logged;
     * any exceptions from the underlying socket are assumed to be fatal
     * and cause the resource to be revoked.
     *
     * <p>This is a protected inner class because it requires access to 
     * the socket ({@link #socket}) and the tuple handler contained in 
     * the outer object.  It also uses the outer datagram I/O client as
     * the source of the events that it generates.</p>
     */
    protected class Listener implements Runnable {
  
      /**
       * Listens on the network for arriving tuples.  These tuples are 
       * passed to the pending query store for processing.
       */
      public void run() {
        byte[] buffer = new byte[65000]; // Almost the max UDP payload size

        DatagramSocket s;
        DatagramPacket packet;
        ObjectInputStream in;
        Tuple tuple;
        TupleEvent event;
        packet = new DatagramPacket(buffer, buffer.length);
  
        while (true) {
          // Safely get reference to socket
          s = null;
          synchronized (lock) {
            if (!isRevoked) {
              s = socket;
            }
          }

          if (s == null) {
            // The resource has been revoked.
            return;
          }

          try {
            // Receive packet
            packet.setLength(buffer.length);
            s.receive(packet);
//Uncomment if profilling
/*
            synchronized(Client.countInLock) {
              Client.packetReads++;
              if (isNormal) {
                normalPacketReads++; 
              }
            }
*/
          } catch (InterruptedIOException x) {
            if (isRevoked) {
              // The resource has been revoked.
              return;
            }

            // Otherwise, just a socket timeout; continue on.
	    continue;

          } catch (IOException x) {
	    
	    // Indicates a failure at the socket level.
	    if (!isRevoked) {
              log.logError(this, "I/O exception while receiving packet", x);
	      revoke();
	    }
	    return;
	  }
	    
          try {
            // Read tuple from packet
            in = new ObjectInputStream(
                    new ByteArrayInputStream(packet.getData()));
            tuple = (Tuple)in.readObject();


            if (Constants.DEBUG_NETWORK) {
              log.log(this, "Received " + tuple);
            }

            // Construct tuple arrival event
            event = new TupleEvent(Client.this, null, tuple);
  
            // Queue the new tuple for processing
            pending.tupleHandler.handle(event);
	    Thread.yield();

          } catch (ClassNotFoundException x) {
            // Could not instantiate object because class was not found
            log.logWarning(this, 
                         "Could not instantiate object of unknown class", 
                         x);

          } catch (ClassCastException x) {
	    log.logWarning(this, 
	                   "Received non-tuple data or invalid tuple",
			   x);
	  } catch (IOException x) {
	    log.logWarning(this,
	                   "Exception while deserializing object",
			   x);
	  } catch (Throwable x) {
	    log.logWarning(this,
	                   "Exception while delivering tuple",
			   x);
	  }
        }
      }
    }

    /** Returns a string representation of this datagram I/O client. */
    public String toString() {
      return "#[" + super.toString() + " for " + sioResource + "]";
    }
  }

//Uncomment for profilling
/*
  public static int getPacketWriteCount() {
    synchronized(Client.countOutLock) {
      return Client.packetWrites;
    }
  }

  public static int getNormalPacketWriteCount() {
    synchronized(Client.countOutLock) {
      return Client.normalPacketWrites;
    }
  }

  public static void clearWriteCounts() {
    synchronized(Client.countOutLock) {
      Client.packetWrites = 0; 
      Client.normalPacketWrites = 0; 
    } 
  }

  public static int getPacketReadCount() {
    synchronized(Client.countInLock) {
      return Client.packetReads;
    }
  }

  public static int getNormalPacketReadCount() {
    synchronized(Client.countInLock) {
      return Client.normalPacketReads;
    }
  }


  public static void clearReadCounts() {
    synchronized(Client.countInLock) {
      Client.packetReads = 0; 
      Client.normalPacketReads = 0; 
    } 
  }
*/
}
