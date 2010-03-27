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

import one.fonda.TestCollection;
import one.fonda.Harness;

import one.world.Constants;

import one.world.core.*;
import one.world.env.EnvironmentEvent;
import one.world.binding.*;

import one.world.data.Name;

import one.world.io.Query;
import one.world.io.SioResource;

import one.world.util.AbstractHandler;
import one.world.util.Log;
import one.world.util.Operation;
import one.world.util.Synchronous;
import one.world.util.SystemUtilities;
import one.world.util.Timer;
import one.world.util.TimeOutException;

import one.util.Guid;

import java.net.InetAddress;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * Regression tests for RemoteManager.  
 *
 * @version  $Revision: 1.34 $
 * @author   Janet Davis
 */
public class TestREP implements TestCollection {

  /////////// Static variables ////////////

  /** Number of milliseconds in one second. */
  static final long ONE_SECOND = 1000;

  /** Number of milliseconds in one minute. */
  static final long ONE_MINUTE = 60 * ONE_SECOND;

  /** Timeout. */
  static final long TIMEOUT = ONE_MINUTE;

  /** Requested lease duration. */
  static final long LEASE_DURATION = 5*ONE_MINUTE;

  /** Host name. */
  static final String host = "localhost";

  /** Server port number. */
  static final int port = 10000;

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

  //////////// MainComponent //////////

  /** The main component; deals with environment stuff. */
  private static class MainComponent extends Component {

    /** The component descriptor. */
    private static final ComponentDescriptor SELF =
        new ComponentDescriptor(
	         "one.world.rep.TestREP.MainComponent",
	 	 "Main component for TestREP",
		 true);

    /** The environment event handler descriptor. */
    private static final ExportedDescriptor ENVIRONMENT = 
        new ExportedDescriptor("environment",
	                       "Handler for environmental events",
			       null, null, false);

    /** 
     * The descriptor for the remote managers' environment event
     * handlers.
     */
    private static final ImportedDescriptor RMENV = 
        new ImportedDescriptor(
	        "rmEnv",
	        "RemoteManager's handler for environmental events",
	        null, null, false, false);

    /** The main handler (exported). */
    EventHandler environmentHandler;

    /** The stop handler (imported). */
    Component.Importer rmEnvHandler;

    /** A lease component. */
    LeaseManager leaseManager;

    /** A timer component. */
    Timer timer;

    /** Constructs a new main component. */
    MainComponent(Environment env) {
      super(env);
     
      environmentHandler = new Handler();
      declareExported(ENVIRONMENT, environmentHandler);

      rmEnvHandler = declareImported(RMENV);

      timer = getTimer();

      // Set up the lease manager
      leaseManager = new LeaseManager(env);
    }

    /** The main event handler. */
    private class Handler extends AbstractHandler {
      public boolean handle1(Event e) {
	if (e instanceof EnvironmentEvent) {
	  EnvironmentEvent ee = (EnvironmentEvent)e;

	  if (EnvironmentEvent.STOP == ee.type) {
	    respond(ee, 
	        new EnvironmentEvent(this, null, 
	                             EnvironmentEvent.STOPPED,
			             getEnvironment().getId()));
          } else if (EnvironmentEvent.STOPPED == ee.type) {
	    // do nothing
	  } else {
	    rmEnvHandler.handle(e);
	  }
	} 

	return true;
      }
    }

    /** Gets the descriptor. */
    public ComponentDescriptor getDescriptor() {
      return (ComponentDescriptor)SELF.clone();
    }
  }

  ////////// EchoHandler //////////

  /**
   * The echo event handler.  Remote events are returned unchanged to the
   * source; exceptional events are logged as errors.
   */
  private static class EchoHandler extends AbstractHandler {
    protected boolean handle1(Event event) {
      if (event instanceof RemoteEvent) {
        RemoteEvent request = (RemoteEvent)event;

        // Swap remote event source and destination.
	SymbolicHandler destination = (SymbolicHandler)request.event.source;
	request.event.source = request.destination;

	RemoteEvent response = new RemoteEvent(this, null,
	                                       destination,
					       request.event);
	respond(request, response);
      } else if (event instanceof ExceptionalEvent) {
        log.logError(this, "Got exceptional event",
	             ((ExceptionalEvent)event).x);
      } else {
        EventHandler source = event.source;
	if (source == null) {
	  return false;
	} else {
	  event.source = this;
	  source.handle(event);
	}
      }

      return true;
    }
  }

  /**
   * An event storing event handler.  Just stores the last event it got.
   */
  private static class EventStorer extends AbstractHandler {

    /** The stored event. */
    Event event = null;

    /** 
     * Handles all events by storing them.
     */
    protected boolean handle1(Event e) {
      event = e;
      return true;
    }

    /** 
     * Waits for an event to be stored or for the timeout to elapse.
     */
    void waitForEvent() throws TimeOutException, InterruptedException {
      int time = 0;
      while (event == null) { 
	SystemUtilities.sleep(100);
	time += 100;
        if (time >= TIMEOUT) {
	  throw new TimeOutException("Timed out after " 
	                             + TIMEOUT + " ms");
        }
      }
    }
  }

  //////////// RemoteManagerWrapper  /////////////

  /** The wrapper for the RemoteManager component. */
  private static class RemoteManagerWrapper extends Component {

    /** The component descriptor. */
    private static final ComponentDescriptor SELF =
        new ComponentDescriptor(
	         "one.world.rep.TestREP.RemoteManagerWrapper",
	 	 "Wrapper for a remote manager",
		 true);

    /** The binding request handler descriptor. */
    private static final ImportedDescriptor REMOTE = 
        new ImportedDescriptor("remote",
	                       "Handler for remote requests",
			       new Class[] { BindingRequest.class,
			                     ResolutionRequest.class,
					     RemoteEvent.class},
			       new Class[] {},
			       false, false);

    /** The remote manager. */
    RemoteManager remoteMgr;

    /** The remote request handler (imported). */
    EventHandler request;

    /**
     * Create a remote manager wrapper.
     *
     * @param env   The environment to use for the new component.
     * @param main  The main component.
     * @param port  The port to listen on.
     *
     * @throws ConnectionFailedException  
     *         Indicates an error in establishing the server.
     * @throws LeaseDeniedException 
     *         Indicates that a lease was denied.
     * @throws LinkingException 
     *         
     *                 Indicates an error in linking.
     */
    RemoteManagerWrapper(Environment env, MainComponent main, int port) 
        throws ConnectionFailedException, LeaseDeniedException, 
	LinkingException {

      super(env);

      // Declare exported and imported event handlers.
      request = declareImported(REMOTE);

      remoteMgr = new RemoteManager(env, host, port);

      // Link.
      this.link("remote", "request", remoteMgr);
      main.link("rmEnv", "env", remoteMgr);
      remoteMgr.link("lease", "request", main.leaseManager);
    }

    /** Gets the component descriptor. */
    public ComponentDescriptor getDescriptor() {
      return (ComponentDescriptor)SELF.clone();
    }
  }

  /////////// TestREP non-static members ////////////

  /** The running environment. */
  Environment env;

  /** The main component. */
  MainComponent main;

  /** The server network I/O factory. */
  RemoteManagerWrapper server;

  /** The client network I/O factory. */
  RemoteManagerWrapper client;

  /** The echo event handler. */
  EventHandler echo;

  /** A remote reference to the echo event handler. */
  RemoteReference echoReference;

  /** The echo handler lease maintainer. */
  LeaseMaintainer echoLeaseMaintainer;

  /** The event storer. */
  EventStorer storer;

  /** A remote reference to the event storer. */
  RemoteReference storerReference;

  /** The event storer lease maintainer. */
  LeaseMaintainer storerLeaseMaintainer;

  /** The initialization flag. */
  boolean initialized = false;

  /** Creates a new REP test collection. */
  public TestREP() {}

  /** Gets the name of the test collection. */
  public String getName() {
    return "one.world.rep.TestREP";
  }

  /** Gets a description of the test. */
  public String getDescription() {
    return "Tests remote event passing. (This will take a few minutes.)";
  }

  /** Gets the number of tests. */
  public int getTestNumber() {
    return 32;
  }

  /** Determine whether this test collection needs an environment. */
  public boolean needsEnvironment() {
    return true;
  }

  /** Initialize this test collection. */
  public boolean initialize(Environment env) throws Throwable {
    this.env = env;

    main = new MainComponent(env);
    env.link("main", "environment", main);

    server = new RemoteManagerWrapper(env, main, port);
    client = new RemoteManagerWrapper(env, main, port + 1);

    Environment.activate(null, env.getId());

    exportEchoHandler();
    exportEventStorer();

    initialized = true;
    return true;
  }

  /** 
   * Exports an echo handler to the server and stores the handler,
   * remote reference, and lease. 
   */
  private void exportEchoHandler() throws Throwable {
    echo = new EchoHandler();
    BindingRequest request = 
        new BindingRequest(null, null, 
	                   new RemoteDescriptor(echo, new Name("echo")),
			   LEASE_DURATION);
    
    Event response = 
        Synchronous.invoke(server.request, request, TIMEOUT);

    if (response instanceof BindingResponse) {
      BindingResponse br = (BindingResponse)response;
      echoLeaseMaintainer = 
          new LeaseMaintainer(br.lease, br.duration,
	                      main.environmentHandler, null, main.timer);
      echoReference = (RemoteReference)br.resource;
    } else if (response instanceof ExceptionalEvent) {
      throw ((ExceptionalEvent)response).x;
    } else {
      throw new UnknownEventException(response.getClass().getName());
    }
  }

  /** 
   * Exports an event storer to the client and stores the handler, 
   * remote reference, and lease. 
   */
  private void exportEventStorer() throws Throwable {
    storer = new EventStorer();
    BindingRequest request = 
        new BindingRequest(null, null, 
	                   new RemoteDescriptor(storer),
			   LEASE_DURATION);
    
    Event response =
        Synchronous.invoke(client.request, request, 
	                   TIMEOUT);

    if (response instanceof BindingResponse) {
      BindingResponse br = (BindingResponse)response;
      storerLeaseMaintainer = 
          new LeaseMaintainer(br.lease, br.duration,
	                      main.environmentHandler, null, main.timer);
      storerReference = (RemoteReference)br.resource;
    } else if (response instanceof ExceptionalEvent) {
      throw ((ExceptionalEvent)response).x;
    } else {
      throw new UnknownEventException(response.getClass().getName());
    }
  }

  /** Cleans up the test collection. */
  public void cleanup() {
    Synchronous.ResultHandler h = new Synchronous.ResultHandler();
    Operation o = new Operation(main.timer, null, h);
    if (echoLeaseMaintainer != null) {
      echoLeaseMaintainer.cancel(o);
      h.getResult();
      echoLeaseMaintainer = null;
    }
    if (storerLeaseMaintainer != null) {
      h.reset();
      storerLeaseMaintainer.cancel(o);
      h.getResult();
      echoLeaseMaintainer = null;
    }
	    
    Synchronous.invoke(server.request,
        new EnvironmentEvent(null, null, 
                             EnvironmentEvent.STOP,
                             main.getEnvironment().getId()));
    Synchronous.invoke(client.request,
        new EnvironmentEvent(null, null, 
                             EnvironmentEvent.STOP,
                             main.getEnvironment().getId()));
  }

  /** 
   * Runs the specified test in the specified test harness.
   */
  public Object runTest(int number, Harness harness, boolean verbose)
    throws Throwable {

    if (!initialized) {
      throw new IllegalStateException("Initialization failed");
    }

    switch (number) {

    case 1: // Try to bind already bound name
      {
        harness.enterTest(1, 
	                  "Try to bind already bound name",
			  Boolean.TRUE);

        BindingRequest request = 
          new BindingRequest(null, null, 
      	                     new RemoteDescriptor(new EchoHandler(), 
			                          new Name("echo")),
    			     LEASE_DURATION);
        
        Event response = Synchronous.invoke(server.request, request);
    
        if (response instanceof BindingResponse) {
	  // Cancel the lease; this isn't what we expected
	  Synchronous.invoke(((BindingResponse)response).lease,
	                     new LeaseEvent(null, null, LeaseEvent.CANCEL,
			                    null, null, 0), 1000);
          return Boolean.FALSE;
        } else if (response instanceof ExceptionalEvent) {
	  Throwable x = ((ExceptionalEvent)response).x;
	  if (x instanceof AlreadyBoundException) {
	    return Boolean.TRUE;
	  } else {
	    return Boolean.FALSE;
	  }
        } else {
          throw new UnknownEventException(response.getClass().getName());
        }
      }
    case 2: // Binding removed when lease denied
      {
        harness.enterTest(2, 
	                  "Binding removed when lease denied",
			  Boolean.TRUE);

        BindingRequest request = 
          new BindingRequest(null, null, 
      	                     new RemoteDescriptor(new EchoHandler(), 
			                          new Name("blah")),
    			     Constants.LEASE_MIN_DURATION - 1);
        
        Event response = Synchronous.invoke(server.request, request);

	if (!(response instanceof ExceptionalEvent)) {
	  return Boolean.FALSE;
	}

	request = 
          new BindingRequest(null, null, 
      	                     new RemoteDescriptor(new EchoHandler(), 
			                          new Name("blah")),
    			     Constants.LEASE_MIN_DURATION + 1);
        
        response = Synchronous.invoke(server.request, request);

        if (response instanceof BindingResponse) {
	  // Cancel the lease; don't need it
	  Synchronous.invoke(((BindingResponse)response).lease,
	                     new LeaseEvent(null, null, LeaseEvent.CANCEL,
			                    null, null, 0));
          return Boolean.TRUE;
        } else if (response instanceof ExceptionalEvent) {
	  throw ((ExceptionalEvent)response).x;
        } else {
          throw new UnknownEventException(response.getClass().getName());
        }
      }
    case 3: // Resolve NamedResource to RemoteReference
      {
       harness.enterTest(3, 
	                  "Resolve NamedResource to RemoteReference",
			  echoReference);
	    
        return server.remoteMgr.resolver.resolve(
	           new NamedResource(host, port, "echo"));
      }
    case 4: // Resolve RemoteReference to RemoteReference
      {
        harness.enterTest(4, 
	                  "Resolve RemoteReference to RemoteReference",
			  echoReference);
	    
        return server.remoteMgr.resolver.resolve(echoReference);
      }
    case 5: // Resolve NamedResource to RemoteReference (unknown)
      {
        harness.enterTest(
	    5, 
	    "Resolve NamedResource to RemoteReference (unknown)",
	    Boolean.TRUE);
	try {
	  return server.remoteMgr.resolver.resolve(
	                     new NamedResource(host, port, "foo"));
	} catch (UnknownResourceException x) {
	  return Boolean.TRUE;
	}
      }
    case 6: // Resolve RemoteReference to RemoteReference (unknown)
      {
        harness.enterTest(
	    6, 
	    "Resolve RemoteReference to RemoteReference (unknown)",
	    Boolean.TRUE);
	try {
	  return server.remoteMgr.resolver.resolve(
	                     new RemoteReference(host, port, new Guid()));
	} catch (UnknownResourceException x) {
	  return Boolean.TRUE;
	}
      }
    case 7: // Resolve NamedResource to EventHandler
      {
        harness.enterTest(7, 
	                  "Resolve NamedResource to EventHandler",
			  Boolean.TRUE);
	    
  
        EventHandler h =  
	    server.remoteMgr.resolver.realResolve(
	        new NamedResource(host, port, "echo"));
        Event e = Synchronous.invoke(h, new DynamicTuple(null, null),
	                             ONE_SECOND);
        return new Boolean(e instanceof DynamicTuple);
      }
    case 8: // Resolve RemoteReference to EventHandler
      {
        harness.enterTest(8, 
	                  "Resolve RemoteReference to EventHandler",
			  Boolean.TRUE);
	    
        EventHandler h =
	    server.remoteMgr.resolver.realResolve(echoReference);
        Event e = Synchronous.invoke(h, new DynamicTuple(null, null),
	                             ONE_SECOND);
        return new Boolean(e instanceof DynamicTuple);
      }
    case 9: // Resolve NamedResource to EventHandler (unknown)
      {
        harness.enterTest(
	    9, 
	    "Resolve NamedResource to EventHandler (unknown)",
	    Boolean.TRUE);
	try {
	  return server.remoteMgr.resolver.realResolve(
	                     new NamedResource(host, port, "foo"));
	} catch (UnknownResourceException x) {
	  return Boolean.TRUE;
	}
      }
    case 10: // Resolve RemoteReference to EventHandler (unknown)
      {
        harness.enterTest(
	    10, 
	    "Resolve RemoteReference to EventHandler (unknown)",
	    Boolean.TRUE);
	try {
	  return server.remoteMgr.resolver.realResolve(
	                     new RemoteReference(host, port, new Guid()));
	} catch (UnknownResourceException x) {
	  return Boolean.TRUE;
	}
      }
    case 11: // Resolve over the network
      {
        harness.enterTest(11,
	                  "Resolve over the network",
			  echoReference);
        storer.event = null;
        client.request.handle(
	    new ResolutionRequest(storerReference, null,
	      	    new NamedResource(host, port, "echo")));

        storer.waitForEvent();
	if (storer.event instanceof ResolutionResponse) {
	  return ((ResolutionResponse)storer.event).resources[0];
	} else if (storer.event instanceof ExceptionalEvent) {
	  throw ((ExceptionalEvent)storer.event).x;
	} else {
	  throw new UnknownEventException(
	      "Unknown event type: " + storer.event.getClass().getName());
	}
      }

    case 12: // Resolve over the network (unknown)
      {
        harness.enterTest(12,
	                  "Resolve over the network (unknown)",
			  Boolean.TRUE);
        storer.event = null;
        client.request.handle(
	    new ResolutionRequest(storerReference, null,
	      	    new NamedResource(host, port, "foo")));

        storer.waitForEvent();
	if (storer.event instanceof ResolutionResponse) {
	  return ((ResolutionResponse)storer.event).resources[0];
	} else if (storer.event instanceof ExceptionalEvent) {
	  Throwable x = ((ExceptionalEvent)storer.event).x;
	  if (x instanceof UnknownResourceException) {
	    return Boolean.TRUE;
	  } else {
	    throw x;
	  }
	} else {
	  throw new UnknownEventException(
	      "Unknown event type: " + storer.event.getClass().getName());
	}
      }

    case 13: // Send remote event
      {
        Event request = new DynamicTuple(storerReference, "quux");
	Event response = new DynamicTuple(echoReference, "quux");

        harness.enterTest(13, "Send remote event", response);
			  
        storer.event = null;
        client.request.handle(
	    new RemoteEvent(storer, null, 
	                    echoReference,
			    request));

        storer.waitForEvent();
	if (storer.event instanceof RemoteEvent) {
	  return ((RemoteEvent)storer.event).event;
	} else if (storer.event instanceof ExceptionalEvent) {
	  throw ((ExceptionalEvent)storer.event).x;
	} else {
	  throw new UnknownEventException(
	      "Unknown event type: " + storer.event.getClass().getName());
	}
      }

    case 14: //Send remote event as datagram
      {
        Event request = new DynamicTuple(storerReference, "quux");
	Event response = new DynamicTuple(echoReference, "quux");

        harness.enterTest(14, "Send remote event as datagram", response);
			  
        storer.event = null;
        client.request.handle(
	    new RemoteEvent(storer, null, 
	                    echoReference,
			    request,
			    true));

        storer.waitForEvent();
	if (storer.event instanceof RemoteEvent) {
	  return ((RemoteEvent)storer.event).event;
	} else if (storer.event instanceof ExceptionalEvent) {
	  throw ((ExceptionalEvent)storer.event).x;
	} else {
	  throw new UnknownEventException(
	      "Unknown event type: " + storer.event.getClass().getName());
	}
      }

    case 15: // Send remote event (destination unknown) 
      {
        Event request = new DynamicTuple(storerReference, "quux");

        harness.enterTest(15, 
	                  "Send remote event (destination unknown)", 
			  Boolean.TRUE);
			  
        storer.event = null;
        client.request.handle(
	    new RemoteEvent(storer, null, 
	                    new RemoteReference(host, port, new Guid()),
			    request));

        storer.waitForEvent();
	if (storer.event instanceof RemoteEvent) {
	  return ((RemoteEvent)storer.event).event;
	} else if (storer.event instanceof ExceptionalEvent) {
	  Throwable x = ((ExceptionalEvent)storer.event).x;
	  if (x instanceof UnknownResourceException) {
	    return Boolean.TRUE;
	  } else {
	    throw x;
	  }
	} else {
	  throw new UnknownEventException(
	      "Unknown event type: " + storer.event.getClass().getName());
	}
      }

    case 16: // Check remote event annotations
      {
        Event request = new DynamicTuple(storerReference, "quux");

        harness.enterTest(16, "Check remote event annotations", 
	                  Boolean.TRUE);
			  
        storer.event = null;
        client.request.handle(
	    new RemoteEvent(storer, null, echoReference, request));

        storer.waitForEvent();
	if (storer.event instanceof RemoteEvent) {
	  Event e = storer.event;
	  return new Boolean(
	         (e.getMetaData(Constants.REQUESTOR_ADDRESS) != null)
	      && (e.getMetaData(Constants.REQUESTOR_PORT) != null));
	} else if (storer.event instanceof ExceptionalEvent) {
	  throw ((ExceptionalEvent)storer.event).x;
	} else {
	  throw new UnknownEventException(
	      "Unknown event type: " + storer.event.getClass().getName());
	}
      }

    case 17: // Test hook for exporting via discovery
      {
        harness.enterTest(17,
	                  "Test hook for exporting via discovery",
			  Boolean.TRUE);
        try {
          Event response =
	      Synchronous.invoke(client.request,
	                         new BindingRequest(null, null, 
	                         new RemoteDescriptor(echo, 
		  	                              new DynamicTuple()),
                                 Duration.SECOND));
          if (response instanceof BindingResponse) {
            LeaseMaintainer.cancel(((BindingResponse)response).lease);
	   }
	   return Boolean.FALSE;
	   
	} catch (NotLinkedException x) {
	  return Boolean.TRUE;
	}
      }

    case 18: // Test hook for discovered resource resolution
      {
        harness.enterTest(18,
	                  "Test hook for discovered resource resolution",
			  Boolean.TRUE);
        try {
	    client.request.handle(
	       new ResolutionRequest(storerReference, null,
				   new DiscoveredResource(new Query())));
        } catch (NotLinkedException x) {
	  return Boolean.TRUE;
	}
      }

    case 19: // Test hook for sending to a discovered resource
      {
        harness.enterTest(19,
	                  "Test hook for sending to a discovered resource",
			  Boolean.TRUE);
        try {
	  client.request.handle(
	      new RemoteEvent(storer, null, 
	                      new DiscoveredResource(new Query()),
			      new DynamicTuple(storerReference, "quux")));
        } catch (NotLinkedException x) {
	  return Boolean.TRUE;
	}
      }

    case 20: // Attempt to connect to invalid port (remote event)
      {
        harness.enterTest(20, 
	    "Attempt to connect to invalid port (remote event)",
	     Boolean.TRUE);

        Event request = new DynamicTuple(storerReference, "quux");

        storer.event = null;
        client.request.handle(
	    new RemoteEvent(storer, null, 
	                   new NamedResource("localhost", port+3, "echo"),
			   request));

        storer.waitForEvent();
	if (storer.event instanceof RemoteEvent) {
	  return ((RemoteEvent)storer.event).event;
	} else if (storer.event instanceof ExceptionalEvent) {
	  Throwable x = ((ExceptionalEvent)storer.event).x;
	  if (x instanceof ConnectionFailedException) {
	    return Boolean.TRUE;
	  } else {
	    throw x;
	  }
	} else {
	  throw new UnknownEventException(
	      "Unknown event type: " + storer.event.getClass().getName());
	}
      }

    case 21: // Attempt to connect to invalid port (resolution request)
      {
        harness.enterTest(21, 
	    "Attempt to connect to invalid port (resolution request)",
	    Boolean.TRUE);

        storer.event = null;
        client.request.handle(
	    new ResolutionRequest(storerReference, null, 
	            new NamedResource("localhost", port+3, "echo")));

        storer.waitForEvent();
	if (storer.event instanceof ResolutionResponse) {
	  return storer.event;
	} else if (storer.event instanceof ExceptionalEvent) {
	  Throwable x = ((ExceptionalEvent)storer.event).x;
	  if (x instanceof ConnectionFailedException) {
	    return Boolean.TRUE;
	  } else {
	    throw x;
	  }
	} else {
	  throw new UnknownEventException(
	      "Unknown event type: " + storer.event.getClass().getName());
	}
      }

    case 22: // Resolution request with invalid source
      {
        harness.enterTest(22, "Resolution request with invalid source",
	                  Boolean.TRUE);

        storer.event = null;
        client.request.handle(
	    new ResolutionRequest(storer, null, 
	            new NamedResource("localhost", port, "echo")));

        storer.waitForEvent();
	if (storer.event instanceof ResolutionResponse) {
	  return storer.event;
	} else if (storer.event instanceof ExceptionalEvent) {
	  Throwable x = ((ExceptionalEvent)storer.event).x;
	  if (x instanceof InvalidTupleException) {
	    return Boolean.TRUE;
	  } else {
	    throw x;
	  }
	} else {
	  throw new UnknownEventException(
	      "Unknown event type: " + storer.event.getClass().getName());
	}
      }

    case 23: // Check connection caching
      {
        Event request = new DynamicTuple(storerReference, "quux");
	Event response = new DynamicTuple(echoReference, "quux");

        log.log(this, "The next test may take several minutes!");
        harness.enterTest(23, "Check connection caching",
	                  response);

        // Send a first event (to ensure connection is in cache)
        storer.event = null;
        client.request.handle(
	    new RemoteEvent(storer, null, 
	                    echoReference,
			    request));
        storer.waitForEvent();

	// Wait 2 times the connection cache time.
	SystemUtilities.sleep((long)(2.5*Constants.REP_CACHE_TIMEOUT));
			  
        // Send another event and see if we get back the response we want.
        storer.event = null;
        client.request.handle(
	    new RemoteEvent(storer, null, 
	                    echoReference,
			    request));
        storer.waitForEvent();
	if (storer.event instanceof RemoteEvent) {
	  return ((RemoteEvent)storer.event).event;
	} else if (storer.event instanceof ExceptionalEvent) {
	  throw ((ExceptionalEvent)storer.event).x;
	} else {
	  throw new UnknownEventException(
	      "Unknown event type: " + storer.event.getClass().getName());
	}
      }

    case 24: // Stop and restart environment
      {
        harness.enterTest(24, 
	                  "Stop and restart environment",
	                  Boolean.TRUE);
        cleanup();
        Environment.terminate(null, main.getEnvironment().getId());
        Environment.activate(null, main.getEnvironment().getId());
        exportEchoHandler();
	exportEventStorer();
        storer.event = null;

        client.request.handle(
	    new RemoteEvent(storer, null, 
	                    echoReference,
			    new DynamicTuple(storerReference, null)));

        storer.waitForEvent();
	if (storer.event instanceof RemoteEvent) {
	  return Boolean.TRUE;
	} else if (storer.event instanceof ExceptionalEvent) {
	  throw ((ExceptionalEvent)storer.event).x;
	} else {
	  throw new UnknownEventException(
	      "Unknown event type: " + storer.event.getClass().getName());
	}
      }

    case 25: // Try to bind already bound name
      {
        harness.enterTest(25, 
	                  "Cancel and rebind",
			  Boolean.TRUE);

        Synchronous.ResultHandler h = new Synchronous.ResultHandler();
        Operation o = new Operation(main.timer, null, h);
        if (echoLeaseMaintainer != null) {
          echoLeaseMaintainer.cancel(o);
          h.getResult();
	  echoLeaseMaintainer = null;
        }

        BindingRequest request = 
          new BindingRequest(null, null, 
      	                     new RemoteDescriptor(new EchoHandler(), 
			                          new Name("echo")),
    			     LEASE_DURATION);
        
        Event response = Synchronous.invoke(server.request, request);
    
        if (response instanceof BindingResponse) {
	  LeaseMaintainer.cancel(((BindingResponse)response).lease);
          return Boolean.TRUE;
        } else if (response instanceof ExceptionalEvent) {
	  throw ((ExceptionalEvent)response).x;
        } else {
          throw new UnknownEventException(response.getClass().getName());
        }
      }

    case 26: // Resolve local resource
      {
        harness.enterTest(26,
	                  "Resolve local resource",
			  storerReference);
        storer.event = null;
        client.request.handle(
	    new ResolutionRequest(storerReference, null, storerReference));

        storer.waitForEvent();
	if (storer.event instanceof ResolutionResponse) {
	  return ((ResolutionResponse)storer.event).resources[0];
	} else if (storer.event instanceof ExceptionalEvent) {
	  throw ((ExceptionalEvent)storer.event).x;
	} else {
	  throw new UnknownEventException(
	      "Unknown event type: " + storer.event.getClass().getName());
	}
      }

    case 27: // Resolve local resource (unknown)
      {
        harness.enterTest(27,
	                  "Resolve local resource (unknown)",
			  Boolean.TRUE);
        storer.event = null;
        client.request.handle(
	    new ResolutionRequest(storerReference, null, 
	      	    new NamedResource(host, port + 1, "foo")));

        storer.waitForEvent();
	if (storer.event instanceof ResolutionResponse) {
	  return ((ResolutionResponse)storer.event).resources[0];
	} else if (storer.event instanceof ExceptionalEvent) {
	  Throwable x = ((ExceptionalEvent)storer.event).x;
	  if (x instanceof UnknownResourceException) {
	    return Boolean.TRUE;
	  } else {
	    throw x;
	  }
	} else {
	  throw new UnknownEventException(
	      "Unknown event type: " + storer.event.getClass().getName());
	}
      }

    case 28: // Send remote event to local resource
      {
        Event request = new DynamicTuple(storerReference, "quux");
	Event response = request;

        harness.enterTest(28, "Send remote event to local resource", 
	                  response);
			  
        storer.event = null;
        client.request.handle(
	    new RemoteEvent(storer, null, storerReference, request));

        storer.waitForEvent();
	if (storer.event instanceof RemoteEvent) {
	  return ((RemoteEvent)storer.event).event;
	} else if (storer.event instanceof ExceptionalEvent) {
	  throw ((ExceptionalEvent)storer.event).x;
	} else {
	  throw new UnknownEventException(
	      "Unknown event type: " + storer.event.getClass().getName());
	}
      }

    case 29: // Send remote event to unknown local resource
      {
        Event request = new DynamicTuple(storerReference, "quux");

        harness.enterTest(29, 
	                  "Send remote event to local resource (unknown)",
			  Boolean.TRUE);
			  
        storer.event = null;
        client.request.handle(
	    new RemoteEvent(storer, null, 
	                    new RemoteReference(host, port+1, new Guid()),
			    request));

        storer.waitForEvent();
	if (storer.event instanceof RemoteEvent) {
	  return ((RemoteEvent)storer.event).event;
	} else if (storer.event instanceof ExceptionalEvent) {
	  Throwable x = ((ExceptionalEvent)storer.event).x;
	  if (x instanceof UnknownResourceException) {
	    return Boolean.TRUE;
	  } else {
	    throw x;
	  }
	} else {
	  throw new UnknownEventException(
	      "Unknown event type: " + storer.event.getClass().getName());
	}
      }

    case 30: // Check remote event annotations (local)
      {
        Event request = new DynamicTuple(storerReference, "quux");

        harness.enterTest(30, "Check remote event annotations (local)", 
	                  Boolean.TRUE);
			  
        storer.event = null;
        client.request.handle(
	    new RemoteEvent(storer, null, storerReference, request));

        storer.waitForEvent();
	if (storer.event instanceof RemoteEvent) {
	  Event e = storer.event;
	  return new Boolean(
	         (e.getMetaData(Constants.REQUESTOR_ADDRESS) != null)
	      && (e.getMetaData(Constants.REQUESTOR_PORT) != null));
	} else if (storer.event instanceof ExceptionalEvent) {
	  throw ((ExceptionalEvent)storer.event).x;
	} else {
	  throw new UnknownEventException(
	      "Unknown event type: " + storer.event.getClass().getName());
	}
      }

    case 31: // Invalid remote event
      {
        harness.enterTest(31, "Invalid remote event", Boolean.TRUE);

	Event request = new DynamicTuple(storer, "quux");
	RemoteEvent re =
	    new RemoteEvent(storer, null, storerReference, request);

	try {
	  re.validate();
	  re.verifySymbolic();
	  return Boolean.FALSE;
	} catch (TupleException x) {
	  return Boolean.TRUE;
	}
      }

    case 32: // Invalid resolution request
      {
        harness.enterTest(32, "Invalid resolution request", Boolean.TRUE);

	storer.event = null;
	client.request.handle(
	    new ResolutionRequest(storer, null, echoReference));

        storer.waitForEvent();
	if (storer.event instanceof ExceptionalEvent) {
	  Throwable x = ((ExceptionalEvent)storer.event).x;
	  if (x instanceof TupleException) {
	    return Boolean.TRUE;
	  } else {
	    return x;
	  }
        } else {
	  return storer.event;
	}
      }
    }

    return null;
  }
}
