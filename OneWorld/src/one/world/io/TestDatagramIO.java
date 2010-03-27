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

import one.fonda.*;

import one.world.core.*;
import one.world.data.Name;
import one.world.binding.*;
import one.world.env.EnvironmentEvent;

import one.world.Constants;

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
 * Regression tests for DatagramIO.  Obtains datagram I/O clients from the
 * environment request handler and tests the combinations of channel
 * types.
 *
 * @version  $Revision: 1.23 $
 * @author   Janet Davis
 */
public class TestDatagramIO implements TestCollection, EventHandler {

  /////////////// Static variables ///////////////////

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


  // Initialize the IP addresses and log variable.
  static {

    log = (Log)AccessController.doPrivileged(
      new PrivilegedAction() {
        public Object run() {
          return Log.getSystemLog();
        }
      });

  }

  //////////////// MainComponent ////////////////////

  /** The wrapper for the DatagramIO component. */
  private static class MainComponent extends Component {

    /** The descriptor for this component. */
    private static final ComponentDescriptor SELF =
        new ComponentDescriptor(
	        "one.world.io.TestDatagramIO.MainComponent",
		"Main component for testing datagram I/O",
		true);

    /** The environment event handler descriptor. */
    private static final ExportedDescriptor ENVIRONMENT =
        new ExportedDescriptor("environment",
	                       "Handler for environment events",
			       null, null, false);
    
    /** The binding request handler descriptor. */
    private static final ImportedDescriptor BINDING_HANDLER =
        new ImportedDescriptor("bind",
	                       "Handles binding requests for "
			         + "datagram I/O resources",
			       new Class[] { BindingRequest.class },
			       new Class[] {},
			       false, false);

    /** The binding request handler (imported). */
    EventHandler bindHandler;

    /**
     * Create a new datagram I/O wrapper.
     * 
     * @param env    The environment to use for the new comonent.
     */
    MainComponent(Environment env) {
      super(env);

      // Declare exported and imported event handlers.
      declareExported(ENVIRONMENT, new Handler());
      bindHandler = declareImported(BINDING_HANDLER);

      // Link.
      try {
	env.link("main", "environment", this);
        this.link("bind", "request", env);
      } catch (LinkingException x) {
        log.logError(this, "Unexpected linking error", x);
      }
    }

    /** Gets the component descriptor. */
    public ComponentDescriptor getDescriptor() {
      return (ComponentDescriptor)SELF.clone();
    }

    /**
     * Get a new ClientWrapper component.
     *
     * @param  url         The SIO url for the client.
     * @param  duration    The requested lease duration.
     * @throws Throwable   Signals an error in getting the datagram I/O
     *                     client.
     */
    ClientWrapper getClient(String url)
            throws Throwable {

      Event request = new BindingRequest(null, null, 
                                         new SioResource(url),
	                                 ONE_MINUTE);
      Event event = Synchronous.invoke(bindHandler, 
                                       request,
				       Constants.SYNCHRONOUS_TIMEOUT);
      if (event instanceof BindingResponse) {
        return new ClientWrapper((BindingResponse)event);
      } else if (event instanceof ExceptionalEvent) {
        throw ((ExceptionalEvent)event).x;
      } else {
        throw new UnknownEventException(event.getClass().getName());
      }
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

  /////////////// ClientWrapper ////////////////

  /** The wrapper for the DatagramIO.Client structured I/O handler. */
  private static class ClientWrapper implements EventHandler {

    /** The datagram I/O handler. */
    EventHandler handler;

    /** The datagram I/O lease handler. */
    EventHandler lease;

    /** The listen operation lease handler */
    EventHandler listenLease;

    /** The list of results. */
    ArrayList result = new ArrayList();

    /** 
     * Creates a new datagram I/O client wrapper.
     *
     * @param br     The binding response containing the datagram I/O 
     *               client to wrap.
     * @throws TimeOutException
     *         If the attempt to establish a listen operation times out.
     */
    ClientWrapper(BindingResponse br) 
            throws TimeOutException, InterruptedException {
      handler = br.resource;
      lease   = br.lease;

      handler.handle(
          new SimpleInputRequest(this, null, SimpleInputRequest.LISTEN,
	                         new Query(), ONE_MINUTE, false));

      // Spin until the listen operation is established.
      long time = 0;
      while (listenLease == null) {
        SystemUtilities.sleep(100);
        time += 100;
	if (time >= Constants.SYNCHRONOUS_TIMEOUT) {
	  throw new TimeOutException(
	      "Attempt to establish a listen operation timed out");
	}
      }
    }

    /**
     * Handle the specified event.  If the event is a 
     * {@link one.world.io.ListenResponse},
     * {@link one.world.binding.LeaseResponse}, or
     * {@link one.world.io.OutputResponse}, does nothing.  If the event is
     * an {@link one.world.io.InputResponse}, stores the tuple.
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
     * Sends a tuple. 
     *
     * @param tuple The tuple to send.
     */
    void put(Tuple t) throws IllegalStateException {
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
    void kill() {

      // It is safe to do this without synchronization b/c the leases
      // should never go from non-null to null.

      if (lease == null) {
        log.logWarning(this, "Lease is null");
      } else {
	Synchronous.invoke(lease, 
	                   new LeaseEvent(null, null, LeaseEvent.CANCEL,
			                  null, null, 0));
      }
      
      if (listenLease == null) {
        log.logWarning(this, "Listen lease is null");
      } else {
	Synchronous.invoke(listenLease, 
	                   new LeaseEvent(null, null, LeaseEvent.CANCEL,
			                  null, null, 0));
      }
    }
  }


  ////////////////// TestDatagramIO non-static members /////////////////

  /** The environment to run in. */
  Environment env;

  /** The datagram I/O factory wrapper. */
  MainComponent factory = null;

  /** To store exceptions in. */
  ArrayList exceptions = new ArrayList();

  /** Gets the name of the test collection. */
  public String getName() {
    return "one.world.io.TestDatagramIO";
  }

  /** Gets a description of the test. */
  public String getDescription() {
    return "Test the various permutations of datagram channels";
  }

  /** Gets the number of tests. */
  public int getTestNumber() {
    return 10;
  }

  /** Determine whether this test collection needs an environment. */
  public boolean needsEnvironment() {
    return true;
  }

  /** Initialize this test collection. */
  public boolean initialize(Environment env) {
    this.env = env;
    factory = new MainComponent(env);
    return true;
  }

  /** Cleans up the test collection. */
  public void cleanup() {
    // Nothing to do.
  }

  /** 
   * Runs the specified test in the specified test harness.
   */
  public Object runTest(int number, Harness harness, boolean verbose)
    throws Throwable {

    ArrayList expected = new ArrayList();
    ArrayList result = new ArrayList();

    if (factory == null) {
      throw new IllegalStateException("Test initialization failed");
    }

    switch (number) {
    case 1: // Input-only/output-only pair
      {
        expected.add(tuples[0]);
        harness.enterTest(1, "Input-only/output-only pair", expected);

        ClientWrapper input = 
            factory.getClient("sio://localhost:10000?type=input");
        ClientWrapper output =
            factory.getClient("sio://localhost:10000?type=output");

        output.put(tuples[0]);
	SystemUtilities.sleep(ONE_SECOND);
	result = input.getResult();

	input.kill();
	output.kill();
        SystemUtilities.sleep(ONE_SECOND);
      }
      break;

    case 2: // Input-only with multiple senders
      {
        for (int i=0; i<3; i++) {
          expected.add(tuples[i]);
	}
        harness.enterTest(2, "Input-only with multiple senders", expected);

        ClientWrapper input = 
            factory.getClient("sio://localhost:10000?type=input");
        ClientWrapper[] output = new ClientWrapper[3];
        for (int i=0; i<3; i++) {
          output[i] = 
            factory.getClient("sio://localhost:10000?type=output");
	}

        for (int i=0; i<3; i++) {
	  output[i].put(tuples[i]);
	  SystemUtilities.sleep(ONE_SECOND);
	}
	result = input.getResult();

        for (int i=0; i<3; i++) {
	  output[i].kill();
	}
	input.kill();
	SystemUtilities.sleep(ONE_SECOND);
      }
      break;

    case 3: // Duplex pair
      {
        expected.add(tuples[0]);
        expected.add(tuples[1]);
        harness.enterTest(3, "Duplex pair", expected);

        ClientWrapper client1 = factory.getClient(
            "sio://localhost:10000?type=duplex&local=localhost:10001");
        ClientWrapper client2 = factory.getClient(
            "sio://localhost:10001?type=duplex&local=localhost:10000");

        client1.put(tuples[0]);
	client2.put(tuples[1]);
        SystemUtilities.sleep(ONE_SECOND);
	result.addAll(client2.getResult());
	result.addAll(client1.getResult());

	client1.kill();
	client2.kill();
	SystemUtilities.sleep(ONE_SECOND);
      }
      break;

    case 4: // Duplex pair; wrong source
      {
        harness.enterTest(4, "Duplex pair; wrong source", expected);

        ClientWrapper client1 = factory.getClient(
            "sio://localhost:10000?type=duplex&local=localhost:10001");
        ClientWrapper client2 = factory.getClient(
            "sio://localhost:10001?type=duplex&local=localhost:10002");

	client2.put(tuples[1]);
        SystemUtilities.sleep(ONE_SECOND);
	result.addAll(client1.getResult());

	client1.kill();
	client2.kill();
	SystemUtilities.sleep(ONE_SECOND);
      }
      break;

    case 5: // Output-only sends to duplex
      {
        harness.enterTest(5, "Output-only sends to duplex; wrong source", 
	                  expected);

        ClientWrapper input = factory.getClient(
            "sio://localhost:10001?type=duplex&local=localhost:10000");
        ClientWrapper output = factory.getClient(
            "sio://localhost:10000?type=output");


        output.put(tuples[0]);
	SystemUtilities.sleep(ONE_SECOND);
	result = input.getResult();

	input.kill();
	output.kill();
	SystemUtilities.sleep(ONE_SECOND);
      }
      break;
      
    case 6: // Input-only receives from duplex
      {
        expected.add(tuples[0]);
        harness.enterTest(6, "Input-only receives from duplex", expected);

        ClientWrapper input = factory.getClient(
            "sio://localhost:10000?type=input");
        ClientWrapper output = factory.getClient(
            "sio://localhost:10000?type=duplex&local=localhost:10001");

        output.put(tuples[0]);
	SystemUtilities.sleep(ONE_SECOND);
	result = input.getResult();

	input.kill();
	output.kill();
	SystemUtilities.sleep(ONE_SECOND);
      }
      break;

    case 7: // Multicast group
      {
        for (int i=0; i<3; i++) {
          expected.add(tuples[i]);
	}
        harness.enterTest(7, "Multicast group", expected);

        ClientWrapper[] client = new ClientWrapper[3];
        for (int i=0; i<3; i++) {
          client[i] = factory.getClient(
              "sio://230.0.0.1:10000?type=multicast");
	}

        for (int i=0; i<3; i++) {
	  client[i].put(tuples[i]);
	  SystemUtilities.sleep(ONE_SECOND);
	}
	result = client[0].getResult();

        for (int i=0; i<3; i++) {
	  client[i].kill();
	}
	SystemUtilities.sleep(ONE_SECOND);
      }
      break;

    case 8: // Output-only sends to multicast group
      {
        for (int i=0; i<3; i++) {
          expected.add(tuples[0]);
	}
        harness.enterTest(8, "Output-only sends to multicast group", 
	                  expected);

	ClientWrapper output = factory.getClient(
            "sio://230.0.0.1:10000?type=output");
        ClientWrapper[] input = new ClientWrapper[3];
        for (int i=0; i<3; i++) {
          input[i] = factory.getClient(
              "sio://230.0.0.1:10000?type=multicast");
	}

        output.put(tuples[0]);
	SystemUtilities.sleep(ONE_SECOND);
        for (int i=0; i<3; i++) {
	  result.addAll(input[i].getResult());
	}

        for (int i=0; i<3; i++) {
	  input[i].kill();
	}
	output.kill();
	SystemUtilities.sleep(ONE_SECOND);
      }
      break;

    case 9: // Listen at localhost (input)
      {
        expected.add(tuples[0]);
        harness.enterTest(9, "Listen at localhost (input)", expected);

	ClientWrapper input = factory.getClient(
	    "sio://localhost:10000?type=input");
        ClientWrapper output = factory.getClient( 
	    "sio://" + InetAddress.getLocalHost().getHostAddress()
	    + ":10000?type=output");

        output.put(tuples[0]);
	SystemUtilities.sleep(ONE_SECOND);
	result = input.getResult();

        input.kill();
	output.kill();
	SystemUtilities.sleep(ONE_SECOND);
      }
      break;

    case 10: // Test default port
      {
        expected.add(tuples[0]);
        harness.enterTest(10, "Test default port", expected);

	ClientWrapper input = 
	    factory.getClient("sio://localhost?type=input");
        ClientWrapper output = 
	    factory.getClient("sio://localhost?type=output");

        output.put(tuples[0]);
	SystemUtilities.sleep(ONE_SECOND);
	result = input.getResult();

        input.kill();
	output.kill();
	SystemUtilities.sleep(ONE_SECOND);
      }
      break;
    }

    return result;
  }

  /** Handle exceptional events by adding the exceptions to the result. */
  public void handle(Event event) {
    if (event instanceof ExceptionalEvent) {
      log.logWarning(this, "Got unexpected exception",
                     ((ExceptionalEvent)event).x);
    } 
  }
}
