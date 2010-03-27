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
 * Performs the sender side of a throughput benchmark for the 
 * REP component.  Simply sends events until an exception is received,
 * indicating that the receiver has stopped.  This component attempts to
 * keep the event queue entirely full of {@link RemoteEvent}s.  
 * {@link one.world.core.NoBufferSpaceException}s are ignored.
 *
 * <p>The test can be run with the following command-line arguments in 
 * one.world. Note that the benchmarks are designed to be run using the
 * {@link BenchmarkREP} component as a front end.<br>
 * 
 * <dl>
 * <dt><code>hostname resourcename</code></dt>
 * <dd>Event Throughput as Sender. Measures throughput by sending to a 
 * receiver at the specified host and handler name.</dd>
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
public final class BenchmarkREPSender extends Component {

  // =======================================================================
  //                           The main handler
  // =======================================================================

  /** The main exported event handler. */
  final class MainHandler extends AbstractHandler {

    /** The event to send. */
    RemoteEvent re;

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
	  
	  if (lease != null) {
	    lease.cancel();
	    lease = null;
	  }
	  respond(e, 
		  new EnvironmentEvent(this, null, EnvironmentEvent.STOPPED,
				       getEnvironment().getId()));
	}
	return true;	

      } else if (e instanceof BindingResponse) {
	BindingResponse br = (BindingResponse)e;
	ref = (SymbolicHandler)br.resource;

        SystemUtilities.debug("Priming caches");
	re = new RemoteEvent(this, null, destination, 
	                     new EmptyEvent(ref, null));

	request.handle(re);

        return true;

      } else if (e instanceof RemoteEvent) {

        SystemUtilities.debug("Sending");
	
	sending = true;
	while (sending) {
	   request.handle(re);
	}
        SystemUtilities.debug("Done");
	return true;

      } else if (e instanceof ExceptionalEvent) {
        Throwable x = ((ExceptionalEvent)e).x;
	if(x instanceof NoBufferSpaceException) {
	  return true;
	} else {
	  x.printStackTrace();
	  sending = false;
	  stop();
	  return true;
	}
      }
      
      return false;
    }
    
    /** Handle an ACTIVATE envrionment event. */
    private void activate() {
      SystemUtilities.debug("Running REP benchmark: " 
  			  + "Tuple Throughput as Sender");

      lease = new LeaseMaintainer(
         new BindingRequest(this, null, new RemoteDescriptor(this), 
	                    Duration.FOREVER),
         request,
	 getTimer());
  
    }
    
    /** Stop the application. */
    private void stop() {
      //STOP
      if (lease != null) {
        lease.cancel();
	lease = null;
      }
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
    new ComponentDescriptor("one.world.io.BenchmarkREPSender",
                            "A component for benchmarking REP throughput",
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
  private transient LeaseMaintainer lease;

  /** Indicates whether we are sending at full speed. */
  private volatile boolean sending;
  
  // =======================================================================
  //                           Constructor
  // =======================================================================

  /**
   * Create a new instance of <code>BenchmarkREPSender</code>.
   *
   * @param  env  The environment for the new instance.
   * @param  destination  The remote resource to send events to.
   */
  public BenchmarkREPSender(Environment env, SymbolicHandler destination) {

    super(env);
    main = declareExported(MAIN, new MainHandler());
    request = declareImported(REQUEST);
    this.destination = destination;
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
    BenchmarkREPSender main = null;

    if (args.length == 2) {
      SymbolicHandler resource = 
          new NamedResource(args[0], Constants.REP_PORT, args[1]);
      main = new BenchmarkREPSender(env, resource);
    } else {
      throw new 
	IllegalArgumentException("Usage: BenchmarkREPSender hostname " 
				 + "resourcename");
    }

    env.link("main","main", main);
    main.link("request","request", env);
  }
  
}





