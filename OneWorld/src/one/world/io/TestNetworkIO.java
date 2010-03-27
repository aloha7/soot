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

import one.fonda.TestCollection;
import one.fonda.Harness;

import one.world.Constants;

import one.world.binding.*;
import one.world.core.*;
import one.world.data.Name;
import one.world.env.EnvironmentEvent;

import one.world.util.Log;
import one.world.util.Synchronous;
import one.world.util.SystemUtilities;
import one.world.util.TimeOutException;

import one.util.Bug;

import java.net.*;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;
import java.io.IOException;

/**
 * Regression tests for NetworkIO.  Obtains network I/O clients and
 * servers from the environment request handler.  Sends tuples back and 
 * forth, attempts to use bogus ports, and stops and restarts server.
 *
 * @version  $Revision: 1.35 $
 * @author   Janet Davis
 */
public class TestNetworkIO implements TestCollection, EventHandler {

  /////////// Static variables ////////////

  /** Tuples used for testing. */
  static final Tuple[] tuples = { new Name("foo"),
                                  new Name("bar"),
                                  new Name("baz"),
                                  new Name("quux") };

  /** Number of milliseconds in one second. */
  static final long ONE_SECOND = 1000;

  /** Number of milliseconds in one minute. */
  static final long ONE_MINUTE = 60 * ONE_SECOND;

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


  //////////// MainComponent  /////////////

  /** The wrapper for the NetworkIO component. */
  private static class MainComponent extends Component {

    /** The component descriptor. */
    private static final ComponentDescriptor SELF =
        new ComponentDescriptor(
	         "one.world.io.TestNetworkIO.MainComponent",
	 	 "Wrapper for a network I/O factory",
		 true);

    /** The environment event handler descriptor. */
    private static final ExportedDescriptor ENVIRONMENT = 
        new ExportedDescriptor("env",
	                       "Handler for environmental events",
			       null, null, false);

    /** The binding request handler descriptor. */
    private static final ImportedDescriptor BIND =
        new ImportedDescriptor("bind",
	                       "Handler for network I/O binding requests",
			       new Class[] { BindingRequest.class },
			       new Class[] {},
			       false, false);

    /** The binding request handler (imported). */
    EventHandler bindHandler;

    /**
     * Create a new network I/O factory wrapper.
     *
     * @param env   The environment to use for the new component.
     */
    MainComponent(Environment env) {
      super(env);

      // Declare exported and imported event handlers.
      declareExported(ENVIRONMENT, new Handler());
      bindHandler = declareImported(BIND);

      // Link.
      try {
        env.link("main", "env", this);
        this.link("bind", "request", env);
      } catch (LinkingException x) {
        log.logError(this, "Unexpected linking error", x);
      }
    }

    /** Gets the component descriptor. */
    public ComponentDescriptor getDescriptor() {
      return (ComponentDescriptor)SELF.clone();
    }

    /** The main event handler. */
    private class Handler implements EventHandler {
      public void handle(Event e) {
        // Ignore all events besides stop environment events.
        if (e instanceof EnvironmentEvent) {
          EnvironmentEvent ee = (EnvironmentEvent)e;
          
          if (EnvironmentEvent.STOP == ee.type) {
            ee.source.handle(new
              EnvironmentEvent(this, null, EnvironmentEvent.STOPPED,
                               getEnvironment().getId()));
          }
        }
      }
    }
  }

  
  /////////// ClientWrapper ////////////

  /** The wrapper for the NetworkIO.Client structured I/O handler. */
  private static class ClientWrapper implements EventHandler {

    /** The network I/O client handler. */
    EventHandler handler;

    /** The network I/O client lease handler. */
    EventHandler lease;

    /** The listen operation lease handler. */
    EventHandler listenLease;

    /** The list of results. */
    ArrayList result = new ArrayList();

    /** A lock object. */
    Object lock = new Object();

    /** 
     * Creates a new network I/O client wrapper. 
     *
     * @param  factory    The network I/O factory to obtain the client 
     *                    from.
     * @param  url        The URL to use.
     * @throws Throwable  Signals an error in obtaining the resource.
     */
    ClientWrapper(MainComponent factory, String url)
            throws Throwable {
      super();

      Event request = new BindingRequest(this, null, 
                                         new SioResource(url),
					 5*ONE_MINUTE);
      Event event = Synchronous.invoke(factory.bindHandler, 
                                       request,
				       5*ONE_MINUTE);

      if (event instanceof BindingResponse) {
        BindingResponse response  = (BindingResponse)event;
        handler = response.resource;
        lease = response.lease;
      } else if (event instanceof ExceptionalEvent) {
	throw ((ExceptionalEvent)event).x;
      } else {
        throw new UnknownEventException(event.getClass().getName());
      }
    }


    /**
     * Creates a new network I/O client wrapper. 
     *
     * @param br The binding response containing the network I/O
     *           client to wrap.
     */
    ClientWrapper(BindingResponse br) throws Throwable {
      handler = br.resource;
      lease   = br.lease;
    }

    /**
     * Handle the specified event.  If the event is a 
     * {@link one.world.binding.LeaseResponse} or
     * {@link OutputResponse}, does nothing.  If the event is
     * an {@link InputResponse}, stores the tuple.  If the event is a
     * {@link ListenRespose}, stores the lease.
     * Otherwise, adds the event to {@link #result} to indicate an error.
     */
    public void handle(Event event) {
      if (event instanceof ListenResponse) {
        listenLease = ((ListenResponse)event).lease;
      } else if (event instanceof OutputResponse) {
        // Do nothing
      } else if (event instanceof InputResponse) {
        result.add(((InputResponse)event).tuple);
      } else if (event instanceof LeaseEvent) {
        // Do nothing
      } else if (event instanceof ExceptionalEvent) {
        ExceptionalEvent xevent = (ExceptionalEvent)event;
	result.add(xevent.x);
      } else {
        result.add(event);
      }
    }

    /** 
     * Adds a listen request for all tuples.
     */
    void listen() {
      handler.handle(
          new SimpleInputRequest(this, null, SimpleInputRequest.LISTEN,
	                         new Query(), ONE_MINUTE, false));
    }

    /**
     * Reads a single tuple.
     *
     * @return  The read tuple or an exception.
     */
    Object read() {
      Event result = 
          Synchronous.invoke(handler, 
            new SimpleInputRequest(this, null, SimpleInputRequest.READ,
	                           new Query(), ONE_MINUTE, false));
      
      if (result instanceof InputResponse) {
        return ((InputResponse)result).tuple;
      } else if (result instanceof ExceptionalEvent) {
        return ((ExceptionalEvent)result).x;
      } else {
        return new UnknownEventException(result.getClass().getName());
      }
    }

    /** 
     * Sends a tuple. 
     *
     * @param tuple The tuple to send.
     */
    void put(Tuple t) throws IllegalStateException {
      if (handler == null) {
        throw new IllegalStateException("No client");
      }
      handler.handle(new SimpleOutputRequest(this, null, t));
    }

    /** 
     * Gets the result. 
     *
     * @return The result.
     */
    ArrayList getResult() {
      return (ArrayList)result.clone();
    }

    /**
     * Kills the client.
     */
    void kill() throws IllegalStateException {
      
      // It is safe to do this without synchronization because a lease can
      // never go from non-null to null.
      
      if (listenLease != null) {
        LeaseMaintainer.cancel(listenLease);
        listenLease = null;
      }

      if (lease == null) {
        log.logWarning(this, "Lease is null");
      } else {
	Synchronous.invoke(lease, 
	    new LeaseEvent(null, null, LeaseEvent.CANCEL, null, null, 0),
	    Constants.SYNCHRONOUS_TIMEOUT);
        lease = null;
      } 
    }
  }

  
  //////////// ServerWrapper /////////////

  /** Wrapper for a network I/O server. */
  static class ServerWrapper implements EventHandler {
    
    /** 
     * Event we wait for during the initialization phase.  (This is
     * hopefully a binding response containing the network I/O server.)
     */
    Event initEvent;

    /** The network I/O server lease handler. */
    EventHandler lease;

    /** The network I/O client wrappers. */
    ArrayList clients = new ArrayList();

    /** The list of results from the server. */
    ArrayList serverResult = new ArrayList();

    /** 
     * Creates a new network I/O server wrapper. 
     *
     * @param  factory    The network I/O factory to obtain the server 
     *                    from.
     * @param  url        The URL to use.
     * @throws Throwable  Signals an error in obtaining the resource.
     */
    ServerWrapper(MainComponent factory, String url) 
            throws Throwable { 
      super();

      SioResource resource = new SioResource(url);
      resource.closure = "Client";

      factory.bindHandler.handle(
          new BindingRequest(this, "Server", resource, 5*ONE_MINUTE));

      while (initEvent == null) {
        synchronized (this) { 
	  wait(100); 
	}
      }
      
      if (initEvent instanceof BindingResponse 
          && initEvent.closure == "Server") {
        lease = ((BindingResponse)initEvent).lease;
      } else if (initEvent instanceof ExceptionalEvent) {
	throw ((ExceptionalEvent)initEvent).x;
      } else {
        throw new UnknownEventException(initEvent.getClass().getName());
      }
    }

    /**
     * Handles events.  For 
     * {@link one.world.binding.BindingResponseEvent}s, a new client
     * wrapper is created and stored.  
     * For {@link one.world.io.InputResponse} events, the tuple is added 
     * to the result.  Other expected events, including 
     * {@link one.world.binding.LeaseResponse} and 
     * {@link one.world.io.OutputResponse}, are ignored.  All other events
     * are added directly to result, to indicate an error.
     *
     * @param  event   The event to handle.
     */
    public void handle(Event event) {

      synchronized (this) {
        if (initEvent == null) {
          initEvent = event;
	  return;
        }
      }

      if (event instanceof BindingResponse) {
        BindingResponse br = (BindingResponse)event;
	try {
	  if (br.closure == "Client") {
	    ClientWrapper client = new ClientWrapper(br);
	    client.listen();
	    synchronized (this) {
	      clients.add(client);
	      notify();
	    }
	  } else {
	    // Do nothing
	  }
	} catch (Throwable x) {
	  log.logError(this, "Got exception when adding client", x);
	}
      } else if (event instanceof LeaseEvent) {
        // Do nothing
      } else if (event instanceof ListenResponse) {
        // Do nothing
      } else if (event instanceof OutputResponse) {
        // Do nothing
      } else if (event instanceof InputResponse) {
        serverResult.add(((InputResponse)event).tuple);
      } else if (event instanceof ExceptionalEvent) {
        ExceptionalEvent xevent = (ExceptionalEvent)event;
	serverResult.add(xevent.x);
	//log.logError(this, "Unexpected exceptional event", xevent.x);
      } else {
        serverResult.add(event);
      }
    }

    /**
     * Put a tuple to all clients.
     */
    void put(Tuple t) {

      // Wait until there are some clients to put to.
      synchronized (this) {
        while (clients.isEmpty()) {
	  try {
	    wait();
	  } catch (InterruptedException x) {
	    // Do nothing
	  }
        }
      }

      Iterator iterator = clients.iterator();
      while (iterator.hasNext()) {
        ClientWrapper client = (ClientWrapper)iterator.next();
	client.put(t);
      }
    }
    
    /**
     * Get the results.
     *
     * @return The results.
     */
    ArrayList getResult() {
      ArrayList result = new ArrayList();

      result.addAll(serverResult);

      Iterator iterator = clients.iterator();
      while (iterator.hasNext()) {
        ClientWrapper client = (ClientWrapper)iterator.next();
	result.addAll(client.getResult());
      }

      return result;
    }

    /**
     * Clear the results.
     */
    void clear() {
      synchronized (this) {
        serverResult = new ArrayList();
      }
    }

    /**
     * Close all client connections.
     */
    void killClients() {
      synchronized (this) {
        Iterator iterator = clients.iterator();
        while (iterator.hasNext()) {
          ClientWrapper client = (ClientWrapper)iterator.next();
	  client.kill();
        }

        clients.clear();
      }
    }

    /** 
     * Kill the server and close all client connections.
     */
    void killAll() {
      if (lease == null) {
        log.logWarning(this, "Lease is null");
      } else {
	Synchronous.invoke(lease, 
	                   new LeaseEvent(null, null, LeaseEvent.CANCEL,
			                  null, null, 0));
      }

      killClients();
    }
  }

  /////////// TestNetworkIO non-static members ////////////

  /** The environment to run in. */
  Environment env;

  /** The network I/O factory. */
  MainComponent factory;

  /** The network I/O server. */
  ServerWrapper server = null;

  /** To store exceptions in. */
  ArrayList exceptions = new ArrayList();

  /** Gets the name of the test collection. */
  public String getName() {
    return "one.world.io.TestNetworkIO";
  }

  /** Gets a description of the test. */
  public String getDescription() {
    return "Sends tuples back and forth, attempts to use bogus ports, and"
           + " stops and restarts server.";
  }

  /** Gets the number of tests. */
  public int getTestNumber() {
    return 14;
  }

  /** Determine whether this test collection needs an environment. */
  public boolean needsEnvironment() {
    return true;
  }

  /** Initialize this test collection. */
  public boolean initialize(Environment env) throws Throwable {
    this.env = env;
    factory = new MainComponent(env);
    Environment.activate(null, env.getId());
    server = new ServerWrapper(factory, "sio://localhost:10000?type=server");
    return false;
  }

  /** Cleans up the test collection. */
  public void cleanup() {
    if (null != server) {
      server.killAll();
      server = null;
    }
  }

  /** 
   * Runs the specified test in the specified test harness.
   */
  public Object runTest(int number, Harness harness, boolean verbose)
    throws Throwable {

    String defaultURL = "sio://localhost:10000?type=client";
    
    ArrayList expected = new ArrayList();
    ArrayList result = new ArrayList();

    if (factory == null || server == null) {
      throw new IllegalStateException("Test initialization failed");
    }

    exceptions = new ArrayList();

    server.killClients();
    server.clear();

    switch (number) {

    case 1: // Send a single tuple from client to server
      {
        expected.add(tuples[0]);
        harness.enterTest(1, "Send a single tuple from client to server",
                          expected);

        ClientWrapper client = new ClientWrapper(factory, defaultURL);
        client.put(tuples[0]);
        SystemUtilities.sleep(ONE_SECOND);
	result = server.getResult();
	client.kill();
      }
      break;

    case 2: // Send several tuples from client to server
      {
        expected.add(tuples[0]);
        expected.add(tuples[1]);
        expected.add(tuples[2]);
        expected.add(tuples[3]);
        harness.enterTest(2, "Send several tuples from client to server",
                          expected);

        ClientWrapper client = new ClientWrapper(factory, defaultURL);
        client.put(tuples[0]);
        SystemUtilities.sleep(ONE_SECOND);
        client.put(tuples[1]);
        SystemUtilities.sleep(ONE_SECOND);
        client.put(tuples[2]);
        SystemUtilities.sleep(ONE_SECOND);
        client.put(tuples[3]);
        SystemUtilities.sleep(ONE_SECOND);
	result = server.getResult();
	client.kill();
      }
      break;

    case 3: // Client and server send each other tuples
      {
        expected.add(tuples[0]);
        expected.add(tuples[1]);
        harness.enterTest(3, "Client and server send each other tuples",
                          expected);
        ClientWrapper client = new ClientWrapper(factory, defaultURL);
	client.listen();
        server.put(tuples[0]);
        SystemUtilities.sleep(ONE_SECOND);
        client.put(tuples[1]);
        SystemUtilities.sleep(ONE_SECOND);
        result.addAll(client.getResult());
        result.addAll(server.getResult());
	client.kill();
      }
      break;

    case 4: // Multiple clients
      {
        expected.add(tuples[0]);
        expected.add(tuples[0]);
        expected.add(tuples[0]);
        harness.enterTest(4, "Multiple clients", expected);
	ClientWrapper[] clients = new ClientWrapper[3];
	for (int i=0; i < 3; i++) {
	  clients[i] = new ClientWrapper(factory, defaultURL);
	  clients[i].listen();
	}

	// Wait for the server to get all the connections.
	synchronized (server) {
	  while (server.clients.size() < 3) {
	    try {
	      server.wait();
	    } catch (InterruptedException x) {
	    }
	  }
	}

	server.put(tuples[0]);
	SystemUtilities.sleep(ONE_SECOND);
	result.addAll(clients[0].getResult());
	result.addAll(clients[1].getResult());
	result.addAll(clients[2].getResult());
	for (int i=0; i < 3; i++) {
	  clients[i].kill();
	}
      }
      break;

    case 5: // Malicious client
      harness.enterTest(5, "Malicious client", Boolean.TRUE);
      try {
        Socket socket = new Socket(InetAddress.getLocalHost(), 10000);
        socket.getOutputStream().write(12345);
        socket.getOutputStream().flush();
        socket.close();
        SystemUtilities.sleep(ONE_SECOND);
        return new Boolean(server.getResult().get(0)
                           instanceof
		  	   java.io.IOException);
      } catch (java.io.IOException x) {
        return Boolean.TRUE;
      }

    case 6: // Silent server
      {
        harness.enterTest(6, "Silent server", Boolean.TRUE);
        Log.getSystemLog().log(this, "This test may take a moment.");

        final Object lock = new Object();
        Thread t = new Thread() {
          public void run() {
	    try {
              ServerSocket ss = new ServerSocket(10005);
	      Socket s = ss.accept();
	      synchronized (lock) {
	        lock.wait();
	      }
            } catch (IOException x) {
	      Log.getSystemLog().logError(this, "I/O exception on socket", x);
	    } catch (InterruptedException x) {
	    }
	    return;
	  }
        };

        t.start();
	try {
	  SystemUtilities.sleep(1000);
	} catch (InterruptedException x) {
	}

        try {
          ClientWrapper c = 
	      new ClientWrapper(factory, 
	                        "sio://localhost:10005?type=client");
	  lock.notify();
        } catch (java.io.InterruptedIOException x) {
	  synchronized (lock) {
	    lock.notify();
	  }
          return Boolean.TRUE;
        }
        return Boolean.FALSE;
      }

    case 7: // Try to open client to unbound port
      harness.enterTest(7, "Try to open client to unbound port",
                        Boolean.TRUE);
      try {
        ClientWrapper c = 
	    new ClientWrapper(factory, 
	                      "sio://localhost:10001?type=client");
      } catch (Throwable x) {
        return Boolean.TRUE;
      }
      return Boolean.FALSE;

    case 8: // Kill and restart server
      {
        expected.add(tuples[0]);
        harness.enterTest(8, "Kill and restart server", expected);
        server.killAll();
	SystemUtilities.sleep(ONE_SECOND);
        server = new ServerWrapper(factory, 
	                           "sio://localhost:10000?type=server");
        ClientWrapper client = new ClientWrapper(factory, defaultURL);
	client.put(tuples[0]);
	SystemUtilities.sleep(ONE_SECOND);
        result = server.getResult();
	client.kill();
      }
      break;

    case 9: // Client descriptor
      {
        SioResource url = 
	    new SioResource("sio://127.0.0.1:10000?type=client");
	harness.enterTest(9, "Client descriptor", Boolean.TRUE);
	BindingRequest request = 
	    new BindingRequest(null, null, url, ONE_SECOND);
        Event response = 
	    Synchronous.invoke(factory.bindHandler, request,
	                       Constants.SYNCHRONOUS_TIMEOUT);

        if (response instanceof BindingResponse) {
	  BindingResponse br = (BindingResponse)response;
          Tuple descriptor = br.descriptor;
	  Synchronous.invoke(br.lease,
	                     new LeaseEvent(null, null, LeaseEvent.CANCEL,
			                    null, null, 0));
	  return new Boolean(url.equals(descriptor));
        }

	return response;
      }

    case 10: // Connection to localhost (non-loopback)
      {
        expected.add(tuples[0]);
        harness.enterTest(10, "Connection to localhost (non-loopback)",
                          expected);

        ClientWrapper client = 
	    new ClientWrapper(factory, "sio://" 
	                      +  InetAddress.getLocalHost().getHostName()
	                      + ":10000?type=client");
        client.put(tuples[0]);
        SystemUtilities.sleep(ONE_SECOND);
	result = server.getResult();
	client.kill();
      }
      break;

    case 11: // Read and wait
      {
        expected.add(tuples[0]);
        expected.add(tuples[1]);
        expected.add(tuples[2]);
	harness.enterTest(11, "Read and wait", expected);

        ClientWrapper client = new ClientWrapper(factory, defaultURL);
	server.put(tuples[0]);
        SystemUtilities.sleep(ONE_SECOND);
	server.put(tuples[1]);
        SystemUtilities.sleep(ONE_SECOND);
	server.put(tuples[2]);
        SystemUtilities.sleep(ONE_SECOND);

        result.add(client.read());
        SystemUtilities.sleep(ONE_SECOND);
        result.add(client.read());
        SystemUtilities.sleep(ONE_SECOND);
        result.add(client.read());
	client.kill();
      }
      break;

    case 12: // Concurrent writes
      {
        int count = Constants.ANIMATOR_CAPACITY/2;

	// Construct an array of the number of tuples we expect to see.
        for (int i=0; i < count; i++) {
	  expected.add(tuples[0]);
	}
        
	// Enter the test.
        harness.enterTest(12, "Concurrent writes", expected);

        // Create a client wrapper.
	ClientWrapper client = new ClientWrapper(factory, defaultURL);

        // Flood the queue with put requests.
	for (int i=0; i < count; i++) {
	  server.put(tuples[0]);
	}

        // Read the tuples into the result.
	for (int i=0; i < count; i++) {
	  result.add(client.read());
	} 

	client.kill();
      }
      break;

    case 13: // Break connection from one side
      {
        harness.enterTest(13, 
	                  "Break connection from one side",
	                  Boolean.TRUE);
        ClientWrapper client = new ClientWrapper(factory, defaultURL);
	
	// Wait, then close all of the server's clients.
	SystemUtilities.sleep(1*ONE_SECOND);
        server.killClients();

	// I can't think of any way to be sure the connection should have
	// been broken, other than using a timeout.
	SystemUtilities.sleep(1*ONE_SECOND);

	// Put to test if the client has been killed.
	Event e = Synchronous.invoke(client.handler,
	                   new SimpleOutputRequest(null, null, tuples[1]));
	client.kill();
	return new Boolean((e instanceof ExceptionalEvent) 
	                   && (((ExceptionalEvent)e).x 
			       instanceof ResourceRevokedException));
      }

    case 14:  // Test default port
      {
        expected.add(tuples[0]);
        harness.enterTest(14, "Test default port", expected);
        ServerWrapper s2 = 
	    new ServerWrapper(factory, "sio://localhost?type=server");
        ClientWrapper client = 
	    new ClientWrapper(factory, "sio://localhost?type=client");
	client.put(tuples[0]);
	SystemUtilities.sleep(ONE_SECOND);
        result = s2.getResult();
	client.kill();
	s2.killAll();
      }
    } 

    return result;
  }

  /** Handle exceptional events by adding the exceptions to the result. */
  public void handle(Event event) {
    if (event instanceof ExceptionalEvent) {
      exceptions.add(((ExceptionalEvent)event).x);
    } 
  }
}
