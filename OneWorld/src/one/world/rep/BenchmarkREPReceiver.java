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

import one.util.Stats;

import one.world.binding.BindingRequest;
import one.world.binding.BindingResponse;
import one.world.binding.Duration;
import one.world.binding.LeaseMaintainer;

import one.world.core.Component;
import one.world.core.ComponentDescriptor;
import one.world.core.Environment;
import one.world.core.Event;
import one.world.core.EventHandler;
import one.world.core.ExportedDescriptor;
import one.world.core.ImportedDescriptor;
import one.world.core.SymbolicHandler;
import one.world.core.NoBufferSpaceException;
import one.world.core.ExceptionalEvent;

import one.world.env.EnvironmentEvent;

import one.world.util.AbstractHandler;
import one.world.util.NullHandler;
import one.world.util.SystemUtilities;

import one.fonda.EmptyEvent;

/**
 * Performs the receiver side of a throughput benchmark for remote event
 * passing.  Throughput is measured by measuring the time required to
 * receive some fixed number of events.  The results are averaged over
 * several tests.  Before starting the tests, a single event is received
 * and responded to.  This ensures that a network connection has been
 * cached, so we can measure steady-state throughput.
 *
 * <p>The name of the remotely exported event handler will be the name of
 * the environment in which this component is run.</p>
 *
 * <p> This component 
 * allows specification of the number of tests performed and the number 
 * of events received per test. The
 * average and standard deviation of the test results are printed once the 
 * test has completed.  The test can be run with the following command-line
 * arguments in one.world. 
 * Note that the benchmarks are designed to be run using the
 * {@link BenchmarkREP} component as a front end.</p>
 * 
 * <dl>
 * <dt><code>[numTests numEvents]</code></dt>
 * <dd>Event Throughput as Receiver. Measures throughput by receiving
 * events from a {@link BenchmarkREPSender sender} started on another 
 * machine.  
 * </dd>
 * </dl>
 *
 * <p><b>Imported and Exported Event Handlers</b></p>
 *
 * <p>Exported event handlers:<dl>
 *    <dt>main</dt>
 *    <dd>Accepts environment events and remote events.
 *        </dd>
 * </dl></p>
 *
 * <p>Imported event handlers:<dl>
 *    <dt>request</dt>
 *    <dd>The imported request handler.
 *        </dd>
 * </dl></p>
 *
 * @version  $Revision 1.1 $
 * @author   Janet Davis
 */
public final class BenchmarkREPReceiver extends Component {

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
	  SystemUtilities.debug("Got ACTIVATE environment event");
	  activate(); 

	} else if ( (EnvironmentEvent.RESTORED == ee.type) ||
	     (EnvironmentEvent.MOVED == ee.type) ) {
	  activate();

	} else if (EnvironmentEvent.STOP == ee.type) {
	  SystemUtilities.debug("Got STOP environment event");
	  
	  //release resources
	  if (nameLease != null) {
	    nameLease.cancel();
	    nameLease = null;
	  }

	  respond(e, 
		  new EnvironmentEvent(main, null, EnvironmentEvent.STOPPED,
				       getEnvironment().getId()));
	}
	return true;	

      } else if (e instanceof BindingResponse) {
	SystemUtilities.debug("Got BindingResponse");
	BindingResponse br = (BindingResponse)e;
	return true;

      } else if (e instanceof RemoteEvent) {

	synchronized (this) {
          if (-1 == eventCount) {
	    // Respond to the event so the connection caches will be primed.
	    SystemUtilities.debug("Got initial event");
            respond(request, ((RemoteEvent)e).event, 
	            new EmptyEvent(NullHandler.NULL, null));
	    startTime = System.currentTimeMillis();
	  }
	  eventCount++;

	  if(numEvents == eventCount) {
	    long diff = System.currentTimeMillis() - startTime; 
	    SystemUtilities.debug((new Long(diff)).toString());
	    stats.add(diff);
	    testCount++;

	    if (numTests == testCount) {
	      SystemUtilities.debug("Time to receive " + numEvents
	                            + " remote events (averaged over " 
				    + numTests + " tests)");
	      SystemUtilities.debug("Average = " + stats.average()
	                            + " ms");
	      SystemUtilities.debug("Standard deviation = " + stats.stdev()
	                            + " ms");
	      stop();
	    }

	    eventCount = 0;
	    startTime = System.currentTimeMillis();
	  }
	}
	return true; 

      } else if (e instanceof ExceptionalEvent) {
	if( ((ExceptionalEvent)e).x instanceof NoBufferSpaceException) {
	  ((ExceptionalEvent)e).x.printStackTrace();
	  System.exit(-1);
	  return true;
	}
      }
      
      return false;
    }
    
    /** Handle an ACTIVATE envrionment event. */
    private void activate() {
      SystemUtilities.debug("Running REP benchmark: " 
  			  + "Tuple Throughput as Receiver");
      SystemUtilities.debug("Testing with " 
  			  + numTests + " tests and " 
  			  + numEvents + " events per test. ");

      eventCount = -1;
      testCount = 0;
      stats = new Stats();
  
      //bind name for exported handler.
      RemoteDescriptor desc = 
         new RemoteDescriptor(this, getEnvironment().getName());
      BindingRequest bindreq = 
        new BindingRequest(this, null, desc, Duration.FOREVER);

      nameLease = new LeaseMaintainer(bindreq, request, getTimer());
    }
    
    /** Stop the application. */
    private void stop() {
      //release resources
      if (nameLease != null) {
        nameLease.cancel();
	nameLease = null;
      }
  
      //STOP
      request.handle(
          new EnvironmentEvent(this, null, EnvironmentEvent.STOPPED,
  			       getEnvironment().getId()));
    }
  }
  

  // =======================================================================
  //                           Descriptors
  // =======================================================================

  /** The component descriptor. */
  private static final ComponentDescriptor SELF =
    new ComponentDescriptor("one.world.io.BenchmarkREPReceiver",
                            "A component for benchmarking REP throughput",
                            false);

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
                           null,   // XXX
                           null,   // XXX
                           false,
                           false);


  // =======================================================================
  //                           Instance fields
  // =======================================================================

  /**
   * The main exported event handler.
   *
   * @serial  Must not be <code>null</code>.
   */
  private final EventHandler       main;

  /**
   * The request imported event handler.
   *
   * @serial  Must not be <code>null</code>.
   */
  private final Component.Importer request;

  /** The lease for the exported name. */
  private transient LeaseMaintainer nameLease;

  /** The number of tests performed. */
  private final int numTests;

  /** The number of events received. */
  private final int numEvents;

  /** Count the number of events received. */
  private int eventCount;

  /** Count the number of tests performed. */
  private int testCount;

  /** The stats calculator. */
  private transient Stats stats;

  /** The time at which the test was started. */
  private long startTime;

  // =======================================================================
  //                           Constructor
  // =======================================================================

  /**
   * Create a new instance of <code>BenchmarkREPReceiver</code>.
   *
   * @param  env  The environment for the new instance.
   */
  public BenchmarkREPReceiver(Environment env) {
    this(env, 100, 1000);
  }

  /**
   * Create a new instance of <code>BenchmarkREPReceiver</code>.
   *
   * @param  env  The environment for the new instance.
   * @param  numTests  The number of tests to perform.
   * @param  numEvents The number of events in each test.
   */
  public BenchmarkREPReceiver(Environment env, 
                              int numTests, int numEvents) {
    super(env);
    main = declareExported(MAIN, new MainHandler());
    request = declareImported(REQUEST);
    this.numTests = numTests;
    this.numEvents = numEvents;
  }


  // =======================================================================
  //                           Component support
  // =======================================================================

  /** Get the component descriptor. */
  public ComponentDescriptor getDescriptor() {
    return (ComponentDescriptor)SELF.clone();
  }


  // =======================================================================
  //                           Initializer
  // =======================================================================

  /** Initialize the application.
   * @param  env  The enclosing environment.
   * @param  closure  The closure.
   */
  public static void init(Environment env, Object closure) throws Exception {
    String[] args = (String[])closure;
    BenchmarkREPReceiver main;

    if(0 == args.length) {
      main = new BenchmarkREPReceiver(env);

    } else if(2 == args.length) {
      int numTests = Integer.parseInt(args[0]);
      int numEvents = Integer.parseInt(args[1]);
      main = new BenchmarkREPReceiver(env, numTests, numEvents);

    } else {
       throw new 
	IllegalArgumentException("Usage: BenchmarkREPReceiver " 
				 + "[numTests numEvents]");
    }

    env.link("main","main",main);
    main.link("request","request",env);
  }

}
