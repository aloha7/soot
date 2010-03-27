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
import one.world.binding.LeaseMaintainer;
import one.world.util.NullHandler;
import one.world.util.Synchronous;
import one.world.util.SystemUtilities;
import one.world.util.Timer;
import one.world.util.TupleEvent;

import one.world.data.Name;

import java.io.IOException;
import java.io.StreamCorruptedException;
import java.net.SocketException;
//The following classes are not available in general
import java.io.ObjectOutputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.File;


/**
 * Sender-side helper class for {@link NetworkIO} throughput
 * benchmark.  <p>This component allows specification of the number
 * of tests performed, number of tuples sent per test and the number
 * of bytes per tuple.  The average and standard deviation of the test
 * results are printed once the test has completed.  The test can be
 * run with the following command-line arguments in <i>one.world</i>.
 * Note that the benchmarks are designed to be run using the {@link
 * BenchmarkNetworkIO} component as a front end.</p>
 * 
 * <dl>
 * <dt><code>hostname port [numTests numTuples numBytes]</code></dt>
 * <dd>Tuple Throughput as Sender. Measures throughput by sending to a receiver
 * at the specified host and port. This functionality is implemented by the 
 * {@link BenchmarkNetworkIOSender} class.</dd>
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
 * @author Adam MacBeth */
public final class BenchmarkNetworkIOSender extends Component {

  /** The number of tuples received. */
  private static int numTuples = 1000;

  /** The number of tests performed. */
  private static int numTests = 100;

  /** The number of bytes in each tuple. */
  private static int numBytes = 100;

  /** The active state. */
  private static int ACTIVE = 1;

  /** The inactive state. */
  private static int INACTIVE = 2;

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
	  tcpLease.handle(new LeaseEvent(main,null,LeaseEvent.CANCEL,
					 tcpLease,null,0));
	  respond(e, 
		  new EnvironmentEvent(main, null, EnvironmentEvent.STOPPED,
				       getEnvironment().getId()));
	}
	return true;	
      } //end EnvironmentEvent handling
      
      else if (e instanceof BindingResponse) {
	//SystemUtilities.debug("Got BindingResponse");
	BindingResponse br = (BindingResponse)e;
	tcpChannel = br.resource;
	tcpLease = br.lease;
	
	t = new DynamicTuple(NullHandler.NULL,null);
	t.set("bytes",new byte[numBytes]);
	sending = true;
	//while(sending) { // send forever
	for(int i = 0; i < Constants.ANIMATOR_CAPACITY/2; i++) {
	  tcpChannel.handle(new SimpleOutputRequest(main,null,t));	  
	}
	return true;
      } //end Binding response handling

      else if (e instanceof InputResponse) {
	//SystemUtilities.debug("Got InputResponse");
	return true; 
      } //end InputResponse handling
    
      else if (e instanceof OutputResponse) {
	//SystemUtilities.debug("Got OutputResponse");
	tcpChannel.handle(new SimpleOutputRequest(main,null,t));	  
	return true;
      }
      
      else if (e instanceof ListenResponse) {
	//SystemUtilities.debug("Got ListenResponse");
	return true;
      }

      else if (e instanceof LeaseEvent) {
	return true;
      }

      else if (e instanceof ExceptionalEvent) {
	ExceptionalEvent ee = (ExceptionalEvent)e;
	if(ee.x instanceof NoBufferSpaceException) {
	  SystemUtilities.debug(ee.x.toString());
	  return true;
	}
	if(ee.x instanceof StreamCorruptedException) {
	  synchronized(lock) {
	    if(ACTIVE == state) {
	      stop();
	    }
	  }
	  return true;
	}
	if(ee.x instanceof ResourceRevokedException) {
	  //SystemUtilities.debug(ee.x.toString());  
	  synchronized(lock) {
	    if(ACTIVE == state) {
	      stop();
	    }
	  }
	  return true;
	}
	if(ee.x instanceof SocketException) {
	  SystemUtilities.debug(ee.x.toString());
	  synchronized(lock) {
	    if(ACTIVE == state) {
	      stop();
	    }
	  }
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
    new ComponentDescriptor("one.world.io.BenchmarkNetworkIOSender",
                            "A component for benchmarking the latency of the " 
			    + "NetworkIO component.",
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

  /** A timer component. */
  private final Timer timer;

  /** The remote host to which data is sent. */
  private String remoteHost;

  /** The port on the remote host to which data is sent. */
  private int remotePort;

  /** The file to store the result data in. */
  private File theFile; 

  /** The FileOutputStream. */
  private FileWriter fileOut;

  /** The bound TCP channel event handler. */
  private EventHandler tcpChannel;

  /** The lease maintainer for the TCP channel. */
  private LeaseMaintainer tcpMaintainer;

  /** The lease for the TCP channel. */
  private EventHandler tcpLease;

  /** The lease for the listen operation. */
  private EventHandler listenLease;

  /** Count the number of tuples received. */
  private int tupleCount;

  /** Count the number of tests performed. */
  private int testCount;

  /** The time at which the test was started. */
  private long startTime;

  /** Indicates whether we are sending at full speed. */
  private volatile boolean sending;

  /** The tuple to be sent. */
  private DynamicTuple t;

  /** The output count. */
  private int outputCount;

  /** The state. */
  private int state;

  /** The state lock. */
  private Object lock = new Object();

  // =======================================================================
  //                           Constructor
  // =======================================================================

  /**
   * Create a new instance of <code>BenchmarkNetworkIO</code>.
   *
   * @param  env  The environment for the new instance.
   * @param  remoteHost  The remote host which is running the tcp echo server.
   */
  public BenchmarkNetworkIOSender(Environment env, String remoteHost, 
				   int remotePort) throws IOException {
    super(env);

    main = declareExported(MAIN, new MainHandler());
    request = declareImported(REQUEST);

    timer = getTimer();
        
    //init fields
    this.remoteHost = remoteHost;
    this.remotePort = remotePort;
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
    BenchmarkNetworkIOSender main;

    if(2 == args.length) {
      main = new BenchmarkNetworkIOSender(env, args[0], 
					    Integer.parseInt(args[1]));   
    }
    else if(5 == args.length) {
      main = new BenchmarkNetworkIOSender(env, args[0],
					    Integer.parseInt(args[1]));
      numTests = Integer.parseInt(args[2]);
      numTuples = Integer.parseInt(args[3]);
      numBytes = Integer.parseInt(args[4]);
    }
     else {
       throw new 
	IllegalArgumentException("Usage: BenchmarkNetworkIOSender hostname " 
				 + "port [numTests numTuples numBytes]");
    }

    env.link("main","main",main);
    main.link("request","request",env);
  }
  
  /** Handle an ACTIVATE envrionment event. */
  private void activate() {
    SystemUtilities.debug("Running NetworkIO benchmark: " 
			  + "Tuple Throughput as Sender");
    SystemUtilities.debug("Testing with " 
			  + numTests + " tests, " 
			  + numTuples + " tuples, "
			  + "and " + numBytes + " bytes per tuple.");

    //bind NetworkIO channel
    SioResource sio = new SioResource("sio://" + remoteHost + ":" 
				      + remotePort
				      + "?type=client"); 
    BindingRequest bindreq = 
      new BindingRequest(main,null,sio,Duration.FOREVER);

    tcpMaintainer = new LeaseMaintainer(bindreq,request,timer);

    synchronized(lock) {
      state = ACTIVE;
    }
  }
  
  /** Stop the application. */
  private void stop() {
    SystemUtilities.debug("Exiting...");

    sending = false;
    synchronized(lock) {
      state = INACTIVE;
    }

    //release resources
    if (tcpLease != null) {
      Synchronous.invoke(tcpLease,
			 new LeaseEvent(main,null,LeaseEvent.CANCEL,
					tcpLease,null,0),
			 Constants.SYNCHRONOUS_TIMEOUT);
    }
    /*
    tcpLease.handle(new LeaseEvent(main,null,LeaseEvent.CANCEL,
				  tcpLease,null,0));
    */
    //STOP
    request.handle(new EnvironmentEvent(main, null, EnvironmentEvent.STOPPED,
					getEnvironment().getId()));
  }

  
}





