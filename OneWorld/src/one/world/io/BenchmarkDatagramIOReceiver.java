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

import one.world.binding.*;

import one.world.Constants;

import one.world.core.Component;
import one.world.core.ComponentDescriptor;
import one.world.core.Environment;
import one.world.core.Event;
import one.world.core.EventHandler;
import one.world.core.ExportedDescriptor;
import one.world.core.ImportedDescriptor;
import one.world.core.DynamicTuple;
import one.world.core.NoBufferSpaceException;
import one.world.core.ExceptionalEvent;

import one.world.env.EnvironmentEvent;

import one.world.util.AbstractHandler;
import one.world.util.NullHandler;
import one.world.util.Synchronous;
import one.world.util.SystemUtilities;
import one.world.util.TupleEvent;

import one.world.data.Name;
import one.util.Stats;

import java.io.IOException;
//The following classes are not available in general
import java.io.ObjectOutputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.File;

/**
 * Receiver-side helper class for {@link DatagramIO} throughput benchmark. 
 * <p>
 * This component 
 * allows specification of the number of tests performed, number 
 * of tuples sent per test and the number of bytes per tuple.  The
 * average and standard deviation of the test results are printed once the 
 * test has completed.  The test can be run with the following command-line
 * arguments in one.world. 
 * Note that the benchmarks are designed to be run using the
 * {@link BenchmarkDatagramIO} component as a front end.<br>
 * 
 * <dl>
 * <dt><code>[numTests numTuples numBytes]</code></dt>
 * <dd>Tuple Throughput as Receiver. Measures throughput by receiving tuple
 * from a sender started on another machine.
 * This functionality is implemented by the 
 * {@link one.world.io.BenchmarkDatagramIOReceiver} class.</dd>
 * </dl>
 *
 * <p><b>Imported and Exported Event Handlers</b></p>
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
 * @author   Adam MacBeth
 */
public final class BenchmarkDatagramIOReceiver extends Component {

  /** The number of tuples received. */
  private static int numTuples = 1000;

  /** The number of tests performed. */
  private static int numTests = 100;

  /** The number of bytes in each tuple. */
  private static int numBytes = 100;

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
	  //SystemUtilities.debug("Got ACTIVATE environment event");
	  activate(); 
	}
	if ( (EnvironmentEvent.RESTORED == ee.type) ||
	     (EnvironmentEvent.MOVED == ee.type) ) {
	  activate();
	}
	if (EnvironmentEvent.STOP == ee.type) {
	  //SystemUtilities.debug("Got STOP environment event");
	  
	  //release resources
	  listenLease.handle(new LeaseEvent(main,null,LeaseEvent.CANCEL,
					    listenLease,null,0));
	  udpLease.handle(new LeaseEvent(main,null,LeaseEvent.CANCEL,
					 udpLease,null,0));
	  respond(e, 
		  new EnvironmentEvent(main, null, EnvironmentEvent.STOPPED,
				       getEnvironment().getId()));
	}
	return true;	
      } //end EnvironmentEvent handling
      
      else if (e instanceof BindingResponse) {
	//SystemUtilities.debug("Got BindingResponse");
	udpChannel = ((BindingResponse)e).resource;
	udpLease = ((BindingResponse)e).lease;
	udpChannel.handle(new SimpleInputRequest(main,
						 null,
						 SimpleInputRequest.LISTEN,
						 new Query(),
						 Duration.FOREVER,
						 false));
	return true;
      } //end Binding response handling

      else if (e instanceof InputResponse) {
	//SystemUtilities.debug("Got InputResponse");
	if(0 == tupleCount) {
	  startTime = System.currentTimeMillis();
	}
	tupleCount++;
	if(numTuples == tupleCount) {
	  long diff = System.currentTimeMillis() - startTime; 
	  SystemUtilities.debug((new Long(diff)).toString());
	  stats.add(diff);
	  testCount++;
	  if(numTests == testCount) {
	    SystemUtilities.debug("Average = " + stats.average());
	    SystemUtilities.debug("Standard deviation = " + stats.stdev());
	    System.out.println("Throughput = " 
			       + (numTuples*numBytes) / (stats.average()/1000)
			       + " Bps");
	    stop();
	  }
	  tupleCount = 0;
	}
	return true; 
      } //end InputResponse handling
    
      else if (e instanceof OutputResponse) {
	//SystemUtilities.debug("Got OutputResponse");
	return true;
      }
      
      else if (e instanceof ListenResponse) {
	//SystemUtilities.debug("Got ListenResponse");
	listenLease = ((ListenResponse)e).lease;
	return true;
      }

      else if (e instanceof LeaseEvent) {
	return true;
      }
      
      else if (e instanceof ExceptionalEvent) {
	ExceptionalEvent ee = (ExceptionalEvent)e;
	if(ee.x instanceof NoBufferSpaceException) {
	  SystemUtilities.debug(ee.x.toString());
	  //((ExceptionalEvent)e).x.printStackTrace();
	  return true;
	}
	if(ee.x instanceof ResourceRevokedException) {
	  SystemUtilities.debug(ee.x.toString());
	  stop();
	  return true;
	}
      }
      
      return false;
    }
  }
  

  // =======================================================================
  //                           Descriptors
  // =======================================================================

  /** The component descriptor. */
  private static final ComponentDescriptor SELF =
    new ComponentDescriptor("one.world.io.BenchmarkDatagramIOReceiver",
                            "A component for benchmarking the throughput"
			    + "of the DatagramIO component.",
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

  /** The remote host to which data is sent. */
  private String remoteHost;

  /** The port on the remote host to which data is sent. */
  private int remotePort;

  /** The file to store the result data in. */
  private File theFile; 

  /** The FileOutputStream. */
  private FileWriter fileOut;

  /** The bound UDP channel event handler. */
  private EventHandler udpChannel;

  /** The lease for the UDP channel. */
  private EventHandler udpLease;

  /** The lease for the listen operation. */
  private EventHandler listenLease;

  /** Count the number of tuples received. */
  private int tupleCount;

  /** Count the number of tests performed. */
  private int testCount;

  /** The time at which the test was started. */
  private long startTime;

  /** The stats object. */
  private Stats stats;
  
  // =======================================================================
  //                           Constructor
  // =======================================================================

  /**
   * Create a new instance of <code>BenchmarkDatagramIO</code>.
   *
   * @param  env  The environment for the new instance.
   * @param  remoteHost  The remote host which is running the udp echo server.
   */
  public BenchmarkDatagramIOReceiver(Environment env) throws IOException {
    super(env);
    main = declareExported(MAIN, new MainHandler());
    request = declareImported(REQUEST);

    stats = new Stats();
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
    BenchmarkDatagramIOReceiver main;

    if(0 == args.length) {
      main = new BenchmarkDatagramIOReceiver(env);
    }
    else if(3 == args.length) {
      main = new BenchmarkDatagramIOReceiver(env);
      numTests = Integer.parseInt(args[0]);
      numTuples = Integer.parseInt(args[1]);
      numBytes = Integer.parseInt(args[2]);
    }
     else {
       throw new 
	IllegalArgumentException("Usage: BenchmarkDatagramIOReceiver "
				 + "[numTests numTuples numBytes]");
    }

    env.link("main","main",main);
    main.link("request","request",env);
  }
  
  /** Handle an ACTIVATE envrionment event. */
  private void activate() {
    SystemUtilities.debug("Running DatagramIO benchmark: " 
			  + "Tuple Throughput as Receiver");
    SystemUtilities.debug("Testing with " 
			  + numTests + " tests, " 
			  + numTuples + " tuples, "
			  + "and " + numBytes + " bytes per tuple.");

    //bind DatagramIO channel
    SioResource sio = new SioResource("sio://localhost:" 
				      + Constants.PORT
				      + "?type=input"); 
    BindingRequest bindreq = 
      new BindingRequest(main,null,sio,Duration.FOREVER);
    request.handle(bindreq);
  }
  
  /** Stop the application. */
  private void stop() {
    //release resources
    listenLease.handle(new LeaseEvent(main,null,LeaseEvent.CANCEL,
				  listenLease,null,0));
    udpLease.handle(new LeaseEvent(main,null,LeaseEvent.CANCEL,
				  udpLease,null,0));

    Synchronous.invoke(listenLease,
		       new LeaseEvent(main,null,LeaseEvent.CANCEL,
				      listenLease,null,0),
		       Constants.SYNCHRONOUS_TIMEOUT);
    Synchronous.invoke(udpLease,
		       new LeaseEvent(main,null,LeaseEvent.CANCEL,
				      udpLease,null,0),
		       Constants.SYNCHRONOUS_TIMEOUT);
    //STOP
    request.handle(new EnvironmentEvent(main, null, EnvironmentEvent.STOPPED,
					getEnvironment().getId()));
  }

  
}





