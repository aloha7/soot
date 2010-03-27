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
import one.world.util.SystemUtilities;

/**
 * Implements the echo server required for a latency benchmark for remote 
 * event passing.  Upon receipt of a remote event, that event is sent back
 * to the sender unchanged except for the source and destination.
 * 
 * <p>The name of the remotely exported event handler will be the name of
 * the environment in which this component is run.</p>
 *
 * <p> This component may be run from the one.world shell with no
 * command-line arguments.  Note that the benchmarks are designed to be 
 * run using the {@link BenchmarkREP} component as a front end.</p>
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
 *    <dd>The imported environment request handler.
 *        </dd>
 * </dl></p>
 *
 * @version  $Revision 1.1 $
 * @author   Janet Davis
 */
public final class BenchmarkREPEcho extends Component {

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
	return true;

      } else if (e instanceof RemoteEvent) {

	RemoteEvent re = (RemoteEvent)e;

	// Swap remote event source and destination.
	SymbolicHandler destination = (SymbolicHandler)re.event.source;
	re.event.source = re.destination;
	request.handle(
	    new RemoteEvent(this, re.closure, destination, re.event));
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
  			  + "Echo server");
  
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
    new ComponentDescriptor("one.world.io.BenchmarkREPEcho",
                            "An echo server for use in benchmarking"
			    + " REP latency",
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
  transient private LeaseMaintainer nameLease;

  
  // =======================================================================
  //                           Constructor
  // =======================================================================

  /**
   * Create a new instance of <code>BenchmarkREPEcho</code>.
   *
   * @param  env  The environment for the new instance.
   */
  public BenchmarkREPEcho(Environment env) {
    super(env);
    main = declareExported(MAIN, new MainHandler());
    request = declareImported(REQUEST);
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

    if (args.length > 0) {
       throw new 
	IllegalArgumentException("Usage: BenchmarkREPEcho"); 
    }

    Component main = new BenchmarkREPEcho(env);

    env.link("main","main",main);
    main.link("request","request",env);
  }

}
