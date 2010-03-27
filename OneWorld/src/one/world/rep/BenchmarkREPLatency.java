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

import one.world.Constants;

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
import one.world.util.TupleEvent;

import one.fonda.EmptyEvent;

import java.io.IOException;


/**
 * Performs the latency benchmark for remote event passing.  Each test
 * measures the time to send a fixed number of events and receive
 * responses to those events, not sending the next event until a response
 * is received.  Before beginning testing, a single event is sent and the
 * response waited for in order to ensure that a network connection has
 * been cached.  Thus, we are measuring steady-state latency.
 *
 * <p> This component 
 * allows specification of the number of tests performed and the number 
 * of events sent per test.  The
 * average and standard deviation of the test results are printed once the 
 * test has completed.  The test can be run with the following command-line
 * arguments in one.world. 
 * Note that the benchmarks are designed to be run using the
 * {@link BenchmarkREP} component as a front end.<br>
 * 
 * <dl>
 * <dt><code>hostname resourcename numTests numEvents</code></dt>
 * <dd>Event Latency. Measures latency by sending to an
 * echo server at the specified host and handler name.  The number of
 * tests is specified by <code>numTests</code>, defaulting to 100,
 * and the number of events in each test by <code>numEvents</code>,
 * defaulting to 1000.</dd>
 * </dl>
 *
 * <p>Exported event handlers:<dl>
 *    <dt>main</dt>
 *    <dd>Accepts environment events.
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
public final class BenchmarkREPLatency extends Component {

  // =======================================================================
  //                           The main handler
  // =======================================================================

  /** The main exported event handler. */
  final class MainHandler extends AbstractHandler {

    /** The event to send. */
    transient RemoteEvent re;

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
	  if (lease != null) {
	    lease.cancel();
	    lease = null;
	  }
	  SystemUtilities.debug("Got STOP environment event");
	  
	  respond(e, 
		  new EnvironmentEvent(main, null, EnvironmentEvent.STOPPED,
				       getEnvironment().getId()));
	}
	return true;	

      } else if (e instanceof BindingResponse) {
	BindingResponse br = (BindingResponse)e;
	ref = (SymbolicHandler)br.resource;

	re = new RemoteEvent(this, null, destination, 
	                     new EmptyEvent(ref, null));
        startTime = SystemUtilities.currentTimeMillis();
        request.handle(re);
	return true;

      } else if (e instanceof RemoteEvent) {

        eventCount++;
	
	if (0 == eventCount) {
	  SystemUtilities.debug("Cache primed");
	  startTime = SystemUtilities.currentTimeMillis();
	} else if (numEvents == eventCount) {
	  long diff = System.currentTimeMillis() - startTime;
	  SystemUtilities.debug((new Long(diff)).toString());
          stats.add(diff);
	  testCount++;
	  if (numTests == testCount) {
	    SystemUtilities.debug("Round-trip latency for " + numEvents 
	                          + " remote events  (averaged over "
				  + numTests + " tests)");
	    SystemUtilities.debug("Average = " + stats.average()
	                          + " ms");
	    SystemUtilities.debug("Standard deviation = " + stats.stdev()
	                          + " ms");
	    stop();
	  }
	  eventCount = 0;
	  startTime = SystemUtilities.currentTimeMillis();
	} 

	request.handle(re);
	return true;

      } else if (e instanceof ExceptionalEvent) {
        Throwable x = ((ExceptionalEvent)e).x;
	if (x instanceof NoBufferSpaceException) {
	  x.printStackTrace();
	  System.exit(-1);
	  return true;
	}
      }
      
      return false;
    }
    
    /** Handle an ACTIVATE envrionment event. */
    private void activate() {
      SystemUtilities.debug("Running REP benchmark: Event Latency");
      SystemUtilities.debug("Testing with "
                          + numTests + " tests and "
                          + numEvents + " events per test. ");

      eventCount = -1;
      testCount = 0;
      stats = new Stats();

      lease = new LeaseMaintainer(
                 new BindingRequest(this, null, new RemoteDescriptor(this), 
	                            Duration.FOREVER),
                 request, 
	         getTimer());
  
    }
    
    /** Stop the application. */
    private void stop() {
      if (lease != null) {
        lease.cancel();
	lease = null;
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
    new ComponentDescriptor("one.world.io.BenchmarkREPLatency",
                            "A component for benchmarking REP latency",
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

  /** The remote resource to which data is sent. */
  private SymbolicHandler destination;

  /** The remote reference to the local event handler. */
  private SymbolicHandler ref;

  /** The lease for the remote reference. */
  transient private LeaseMaintainer lease;

  /** The number of tests to perform. */
  private final int numTests;

  /** The number of events in each test. */
  private final int numEvents;

  /** Count the number of events received. */
  private int eventCount;

  /** Count the number of tests performed. */
  private int testCount;

  /** The test start time. */
  private long startTime;

  /** The stats calculator. */
  private transient Stats stats;

  
  // =======================================================================
  //                           Constructor
  // =======================================================================

  /**
   * Create a new instance of <code>BenchmarkREPLatency</code>.
   *
   * @param  env  The environment for the new instance.
   * @param  destination  The remote resource to send events to.
   */
  public BenchmarkREPLatency(Environment env, SymbolicHandler destination) {
    this(env, destination, 100, 1000);
  }

  /**
   * Create a new instance of <code>BenchmarkREPLatency</code>.
   *
   * @param  env  The environment for the new instance.
   * @param  destination  The remote resource to send events to.
   * @param  numTests The number of tests to perform.
   * @param  numEvents The number of events in each test.
   */
  public BenchmarkREPLatency(Environment env, SymbolicHandler destination,
                             int numTests, int numEvents) {

    super(env);
    main = declareExported(MAIN, new MainHandler());
    request = declareImported(REQUEST);
    this.destination = destination;
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
    BenchmarkREPLatency main = null;

    if (args.length == 2) {
      SymbolicHandler resource = 
          new NamedResource(args[0], Constants.REP_PORT, args[1]);
      main = new BenchmarkREPLatency(env, resource);
    } else if (args.length == 4) {
      SymbolicHandler resource = 
          new NamedResource(args[0], Constants.REP_PORT, args[1]);
      int numTests = Integer.parseInt(args[2]);
      int numEvents = Integer.parseInt(args[3]);
      main = new BenchmarkREPLatency(env, resource, numTests, numEvents);
    } else {
      throw new 
	IllegalArgumentException("Usage: BenchmarkREPLatency hostname " 
				 + "resourcename [numTests numEvents]");
    }

    env.link("main","main", main);
    main.link("request","request", env);
  }
  
}
