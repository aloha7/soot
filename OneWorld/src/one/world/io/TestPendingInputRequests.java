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

import one.world.binding.*;

import one.world.data.Name;

import one.world.env.EnvironmentEvent;

import one.world.util.Log;
import one.world.util.Synchronous;
import one.world.util.SystemUtilities;
import one.world.util.TupleEvent;

import one.util.Bug;
import one.util.Guid;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.io.IOException;

/**
 * Regression tests for {@link PendingInputRequests}.  This includes tests
 * of single requests, combinations of requests, request expiration,
 * and management of leases for listen requests.
 *
 * @version  $Revision: 1.28 $
 * @author   Janet Davis
 */
public class TestPendingInputRequests 
        implements TestCollection, EventHandler {
 
  /** Tuples used for testing. */
  static Tuple[] tuples = { new Name("foo"),
                            new Name("bar"),
		            new Name("baz"),
		            new Name("quux") };

  /** Number of milliseconds in one second. */
  static long ONE_SECOND = 1000;

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

  private static class MyComponent extends Component {
  
    /** The descriptor for this component. */
    private static final ComponentDescriptor SELF = 
        new ComponentDescriptor("one.world.io.TestPendingInputRequests",
                                "Test class fort the pending input request"
  			       + " manager",
  			       true);
  
    /**
     * The exported event handler, for environment events.
     */
    private static final ExportedDescriptor ENVIRONMENT = 
        new ExportedDescriptor("environment",
                               "Handler for environment events",
  			     null, null, false);
    /**
     * The imported event handler descriptor for the leased request event
     * handler.
     */
    private static final ImportedDescriptor LEASED_REQUEST_HANDLER = 
        new ImportedDescriptor(
	        "leased request handler",
                "Pending request handler for leased requests",
  	  	new Class[] { InputRequest.class,
  		  	      SimpleInputRequest.class },
                null, false, false);
  
    /**
     * The imported event handler descriptor for the unleased request event
     * handler.
     */
    private static final ImportedDescriptor UNLEASED_REQUEST_HANDLER =
        new ImportedDescriptor("unleased request handler",
                               "Pending request handler for unleased "
  			       + "requests",
                               new Class[] { InputRequest.class,
  			                   SimpleInputRequest.class,
  					   RemovePendingRequest.class },
  			     null, false, false);
  
    /**
     * The imported event handler descriptor for the tuple event handler.
     */
    private static final ImportedDescriptor TUPLE_HANDLER = 
        new ImportedDescriptor("tuple handler",
                               "Tuple handler for pending input requests",
  			     new Class[] { TupleEvent.class },
  			     null, false, false);
  
    /**
     * The imported event handler descriptor for the lease event handler.
     */
    private static final ImportedDescriptor LEASE_HANDLER = 
        new ImportedDescriptor("lease handler",
                               "Handler for lease events",
  			     new Class[] { LeaseEvent.class },
  			     null, false, false);
    /** 
     * Handler for leased pending input requests (imported from
     * <CODE>PendingInputRequests</CODE>). 
     */
    EventHandler leased;
  
    /** 
     * Handler for unleased pending input requests (imported from
     * <CODE>PendingInputRequests</CODE>).
     */
    EventHandler unleased;
  
    /**
     * Handler for sending tuples to pending input manager (imported from
     * <CODE>PendingInputRequests</CODE>).
     */
    EventHandler tupleHandler;

    /**
     * Handler for lease acquisistion events. 
     */
    EventHandler leaseHandler;
  
    /** 
     * Construct a new test component. 
     *
     * @param env  The environment.
     */
    public MyComponent(Environment env) {
      super(env);
  
      leased = declareImported(LEASED_REQUEST_HANDLER);
      unleased = declareImported(UNLEASED_REQUEST_HANDLER);
      tupleHandler = declareImported(TUPLE_HANDLER);
      leaseHandler = declareImported(LEASE_HANDLER);
  
      declareExported(ENVIRONMENT, new Handler());
  
      PendingInputRequests pending = 
          new PendingInputRequests(env, leaseHandler);
      LeaseManager leaseMgr = new LeaseManager(env);
  
      try {
	env.link("main", "environment", this);
        link("leased request handler", "leased request handler", pending);
        link("unleased request handler", "unleased request handler",
             pending);
        link("tuple handler", "tuple handler", pending);
        link("lease handler", "request", leaseMgr);
      } catch (LinkingException x) {
        log.logError(this, "Unexpected linking error", x);
      }
    }

    /** Gets a descriptor for this component. */
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

  /** The component for testing. */
  private MyComponent myComponent;

  /** Gets the name of the test collection. */
  public String getName() {
    return "one.world.io.TestPendingInputRequests";
  }

  /** Gets a description of the test. */
  public String getDescription() {
    return "Tests of single requests, combinations of requests, request"
         + " expiration, and management of leases for listen requests";
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
    myComponent = new MyComponent(env);
    return true;
  }

  /** Runs the specified test in the specified test harness. */
  public Object runTest(int number, Harness harness, boolean verbose)
    throws Throwable {

    Receiver receiver = null;
    ArrayList expected = new ArrayList();

    if (myComponent == null) {
      throw new IllegalStateException("Test initialization failed");
    }

    switch (number) {
    case 1: // READ (simple)
      {
        expected.add(tuples[0]);
        harness.enterTest(1, "READ (simple)", expected);
  
        receiver = new Receiver();
  
        Event request = 
            new SimpleInputRequest(receiver, null, SimpleInputRequest.READ,
    	                  	 new Query(), ONE_SECOND, false);
        myComponent.unleased.handle(request);
        putTuples();
        myComponent.unleased.handle(
            new RemovePendingRequest(this, null, (Guid)request.get("id")));
      }
      break;

    case 2: // READ by ID (simple)
      {
        expected.add(tuples[0].get("id"));
        harness.enterTest(2, "READ by ID (simple)", expected);
  
        receiver = new Receiver();
  
        Event request = 
            new SimpleInputRequest(receiver, null, SimpleInputRequest.READ,
    	                  	 new Query(), ONE_SECOND, true);
        myComponent.unleased.handle(request);
        putTuples();
        myComponent.unleased.handle(
            new RemovePendingRequest(this, null, (Guid)request.get("id")));
      }
      break;

    case 3: // READ
      {
        expected.add(tuples[0]);
        harness.enterTest(3, "READ", expected);
  
        receiver = new Receiver();
  
        Event request = 
            new InputRequest(receiver, null, InputRequest.READ,
    			   new Query(), ONE_SECOND, false, null);
        myComponent.unleased.handle(request);
        putTuples();
        myComponent.unleased.handle(
            new RemovePendingRequest(this, null, (Guid)request.get("id")));
      }
      break; 

    case 4: // READ by ID
      {
        expected.add(tuples[0].get("id"));
        harness.enterTest(4, "READ by ID", expected);
  
        receiver = new Receiver();
  
        Event request =
            new InputRequest(receiver, null, InputRequest.READ,
    			   new Query(), ONE_SECOND, true, null);
        myComponent.unleased.handle(request);
        putTuples();
        myComponent.unleased.handle(
            new RemovePendingRequest(this, null, (Guid)request.get("id")));
      }
      break; 

    case 5: // READ + READ
      {
        expected.add(tuples[0]);
        expected.add(tuples[0]);
        harness.enterTest(5, "READ+READ", expected);
  
        receiver = new Receiver();
  
        Event request1 = 
            new InputRequest(receiver, null, InputRequest.READ,
    			   new Query(), ONE_SECOND, false, null);
        Event request2 = 
            new InputRequest(receiver, null, InputRequest.READ,
    			   new Query(), ONE_SECOND, false, null);
        myComponent.unleased.handle(request1);
        myComponent.unleased.handle(request2);
        putTuples();
        myComponent.unleased.handle(
            new RemovePendingRequest(this, null, (Guid)request1.get("id")));
        myComponent.unleased.handle(
            new RemovePendingRequest(this, null, (Guid)request2.get("id")));
      }
      break; 

    case 6: // LISTEN
      {
        expected.add(tuples[0]);
        expected.add(tuples[1]);
        expected.add(tuples[2]);
        expected.add(tuples[3]);
        harness.enterTest(6, "LISTEN", expected);
  
        receiver = new ListenReceiver();
  
        Event request = 
            new InputRequest(receiver, null, InputRequest.LISTEN,
    			   new Query(), ONE_SECOND, false, null);
        myComponent.unleased.handle(request);
        putTuples();
        myComponent.unleased.handle(
            new RemovePendingRequest(this, null, (Guid)request.get("id")));
      }
      break; 
      
    case 7: // LISTEN + READ
      {
        expected.add(tuples[0]);
        expected.add(tuples[0]);
        expected.add(tuples[1]);
        expected.add(tuples[2]);
        expected.add(tuples[3]);
        harness.enterTest(7, "LISTEN+READ", expected);
  
        receiver = new ListenReceiver();
  
        Event request1 =
            new InputRequest(receiver, null, InputRequest.LISTEN,
    			   new Query(), ONE_SECOND, false, null);
        Event request2 =
            new InputRequest(receiver, null, InputRequest.READ,
    			   new Query(), ONE_SECOND, false, null);
        myComponent.unleased.handle(request1);
        myComponent.unleased.handle(request2);
        putTuples();
        myComponent.unleased.handle(
            new RemovePendingRequest(this, null, (Guid)request1.get("id")));
        myComponent.unleased.handle(
            new RemovePendingRequest(this, null, (Guid)request2.get("id")));
      }
      break; 
      
    case 8: // READ w/ string equals query
      {
        expected.add(tuples[2]);
        harness.enterTest(8, "READ w/ string equals query", expected);
  
        receiver = new Receiver();
  
        Event request = 
            new InputRequest(receiver, 
  	                   null, 
  			   InputRequest.READ,
    			   new Query("name", Query.COMPARE_EQUAL, "baz"),
  			   ONE_SECOND, 
  			   false,
  			   null);
        myComponent.unleased.handle(request);
        putTuples();
        myComponent.unleased.handle(
            new RemovePendingRequest(this, null, (Guid)request.get("id")));
      }
      break;
      
    case 9: // READ w/ string begins-with query
      {
        expected.add(tuples[1]);
        harness.enterTest(9, "READ w/ string begins-with query", expected);
  
        receiver = new Receiver();
        
        Event request =
            new InputRequest(receiver, 
  	                   null, 
  			   InputRequest.READ,
    			   new Query("name", 
  			             Query.COMPARE_BEGINS_WITH,
  			             "b"),
  			   ONE_SECOND, 
  			   false,
  			   null);
        myComponent.unleased.handle(request);
        putTuples();
        myComponent.unleased.handle(
            new RemovePendingRequest(this, null, (Guid)request.get("id")));
      }
      break;
      
    case 10: // LISTEN w/ string begins-with query
      {
        expected.add(tuples[1]);
        expected.add(tuples[2]);
        harness.enterTest(10, "LISTEN w/ string begins-with query", expected);
  
        receiver = new ListenReceiver();
  
        Event request = 
            new InputRequest(receiver, 
  	                   null, 
  			   InputRequest.LISTEN,
    			   new Query("name", 
  			             Query.COMPARE_BEGINS_WITH,
  			             "b"),
  			   ONE_SECOND, 
  			   false,
  			   null);
        myComponent.unleased.handle(request);
        putTuples();
        myComponent.unleased.handle(
            new RemovePendingRequest(this, null, (Guid)request.get("id")));
      }
      break;
      
    case 11: // READ expiration
      expected.add(null);
      harness.enterTest(11, "READ expiration", expected);
      receiver = new Receiver();
      myComponent.leased.handle(
          new InputRequest(receiver, null, InputRequest.READ,
  			   new Query(), ONE_SECOND, false, null));
      SystemUtilities.sleep((long)(1.5*ONE_SECOND));
      putTuples();
      SystemUtilities.sleep(ONE_SECOND);
      break;
  
    case 12: // LISTEN expiration
      expected.add(tuples[0]);
      expected.add(tuples[1]);
      harness.enterTest(12, "LISTEN expiration", expected);

      receiver = new ListenReceiver();

      myComponent.leased.handle(
          new InputRequest(receiver, null, InputRequest.LISTEN,
  			   new Query(), ONE_SECOND, false, null));
      myComponent.tupleHandler.handle(
          new TupleEvent(this, null, tuples[0]));
      SystemUtilities.sleep(ONE_SECOND);
      myComponent.tupleHandler.handle(
          new TupleEvent(this, null, tuples[1]));
      SystemUtilities.sleep(ONE_SECOND);
      myComponent.tupleHandler.handle(
          new TupleEvent(this, null, tuples[2]));
      SystemUtilities.sleep(ONE_SECOND);
      break;

    case 13: // LISTEN renewal
      expected.add(tuples[0]);
      expected.add(tuples[1]);
      expected.add(tuples[2]);
      harness.enterTest(13, "LISTEN renewal", expected);

      receiver = new ListenReceiver();

      myComponent.leased.handle(
          new InputRequest(receiver, null, InputRequest.LISTEN,
  			   new Query(), ONE_SECOND, false, null));
      myComponent.tupleHandler.handle(
          new TupleEvent(this, null, tuples[0]));
      SystemUtilities.sleep(3*ONE_SECOND/4);
      myComponent.tupleHandler.handle(
          new TupleEvent(this, null, tuples[1]));
      ((ListenReceiver)receiver).renew(ONE_SECOND);
      SystemUtilities.sleep(3*ONE_SECOND/4);
      myComponent.tupleHandler.handle(
          new TupleEvent(this, null, tuples[2]));
      SystemUtilities.sleep(ONE_SECOND);
      break;

    case 14: // LISTEN cancellation
      expected.add(tuples[0]);
      harness.enterTest(14, "LISTEN cancellation", expected);

      receiver = new ListenReceiver();

      myComponent.leased.handle(
          new InputRequest(receiver, null, InputRequest.LISTEN,
  			   new Query(), ONE_SECOND, false, null));
      myComponent.tupleHandler.handle(
          new TupleEvent(this, null, tuples[0]));
      ((ListenReceiver)receiver).cancel();
      SystemUtilities.sleep(3*ONE_SECOND/4);
      myComponent.tupleHandler.handle(
          new TupleEvent(this, null, tuples[1]));
      myComponent.tupleHandler.handle(
          new TupleEvent(this, null, tuples[2]));
      SystemUtilities.sleep(ONE_SECOND);
      break;
    }

    return receiver.getResult();
  }

  /** Clean up this test collection. */
  public void cleanup() {
    // Nothing to do
  }

  /**
   * Adds the test tuples to the pending request manager.
   */
  void putTuples() {
    for( int i=0; i < tuples.length; i++ ) {
      myComponent.tupleHandler.handle(
          new TupleEvent(this, null, tuples[i]));
    }
  }

  /** Do nothing; we don't expect to get any events. */
  public void handle(Event event) {
     System.out.println(event);
  }

  /**
   * Stores received tuples and events.
   */
  static class Receiver implements EventHandler {

    /** Used to store the tuples and events. */
    ArrayList result = new ArrayList();

    /**
     * Handles events.  For InputResponse events, the tuple is added to 
     * the result.  For InputByIdResponse events, the id is added to the
     * result.  For all other events, the event is added directly to the
     * result, to indicate an error.
     *
     * @param  event   The event to handle.
     */
    public void handle(Event event) {
      if (event instanceof InputResponse) {
        result.add(((InputResponse)event).tuple);
      } else if (event instanceof InputByIdResponse) {
        result.add(((InputByIdResponse)event).ident);
      } else if (event instanceof ExceptionalEvent) {
        ExceptionalEvent xevent = (ExceptionalEvent)event;
	//xevent.x.printStackTrace();
	result.add(xevent.x);
      } else {
        result.add(event);
      }
    }
    
    /**
     * Get the results.
     *
     * @return The results.
     */
    Object getResult() {
      return result;
    }
  }

  /**
   * Stores received tuples and events, and manages the lease for listen
   * requests.
   */
  static class ListenReceiver extends Receiver {

    /** The lease obtained from the listen request. */
    EventHandler lease = null;

    /**
     * Handles events.  For InputResponse events, the tuple is added to 
     * the result.  For BindingResponse events, the lease is saved to use 
     * later.  For all other events, the event is added directly to 
     * result, to indicate an error.
     *
     * @param  event   The event to handle.
     */
    public void handle(Event event) {
      if (event instanceof InputResponse) {
        result.add(((InputResponse)event).tuple);
      } else if (event instanceof ListenResponse) {
        lease = ((ListenResponse)event).lease;
      } else if (event instanceof LeaseEvent) {
      } else if (event instanceof ExceptionalEvent) {
        ExceptionalEvent xevent = (ExceptionalEvent)event;
	//xevent.x.printStackTrace();
	result.add(xevent.x);
      } else {
        result.add(event);
      }
    }

    /**
     * Attempts to extend the lease by the specified duration.
     *
     * @param  duration The length of time to extend the lease by.
     */
    public void renew(long duration) {
      lease.handle(
          new LeaseEvent(this, null, LeaseEvent.RENEW, 
	                 null, null, duration));
    }

    /**
     * Cancels the lease.
     */
    public void cancel() {
      Synchronous.invoke(lease, 
                         new LeaseEvent(this, null, LeaseEvent.CANCEL, 
	                                null, null, 0));
    }
  }
}
