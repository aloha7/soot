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

package one.toys;

import one.world.Constants;

import one.world.binding.BindingRequest;
import one.world.binding.BindingResponse;
import one.world.binding.Duration;
import one.world.binding.LeaseMaintainer;

import one.world.core.Component;
import one.world.core.ComponentDescriptor;
import one.world.core.DynamicTuple;
import one.world.core.Environment;
import one.world.core.Event;
import one.world.core.EventHandler;
import one.world.core.ExceptionalEvent;
import one.world.core.ExportedDescriptor;
import one.world.core.ImportedDescriptor;
import one.world.core.SymbolicHandler;
import one.world.core.UnknownEventException;

import one.world.env.EnvironmentEvent;

import one.world.io.Query;

import one.world.rep.DiscoveredResource;
import one.world.rep.NamedResource;
import one.world.rep.RemoteDescriptor;
import one.world.rep.RemoteEvent;
import one.world.rep.RemoteReference;

import one.world.util.AbstractHandler;
import one.world.util.Operation;
import one.world.util.SystemUtilities;
import one.world.util.Timer;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;

/**
 * Sends a remote event to a receiver every {@link #DELAY} ms.  
 *
 * <p>
 * Usage:
 * <pre>
 *      RemoteSender [name] [hostname] [port]
 * </pre>
 * </p>
 *
 * <p>
 * The name is the name of the event handler to send to.  If no name is
 * specified, "receiver" will be used.  A hostname and
 * port may optionally be specified.  If no hostname is specified, the event 
 * will be routed through discovery rather than to a particular host.  
 * </p>
 *
 * <p><b>Imported and Exported Event Handlers</b></p>
 *
 * <p>Exported event handlers:<dl>
 *    <dt>main</dt>
 *    <dd>Handles environment events.</dd>
 * </dl></p>
 *
 * <p>Imported event handlers:<dl>
 *    <dt>request</dt>
 *    <dd>The environment's request handler.</dd>
 * </dl></p>
 *
 * @see      RemoteReceiver
 * @see      one.world.rep
 * @version  $Revision: 1.4 $
 * @author   Janet Davis
 */
public final class RemoteSender extends Component {

  // =======================================================================
  //                           The main handler
  // =======================================================================

  /** The main exported event handler. */
  final class MainHandler extends AbstractHandler {

    /** Handle the specified event. */
    protected boolean handle1(Event e) {

      if (e instanceof EnvironmentEvent) {
        EnvironmentEvent ee = (EnvironmentEvent)e;
	if (ee.type == EnvironmentEvent.STOP) {
	  // If we get a STOP event, stop and respond with a STOPPED
	  // event.
	  stop();
	  respond(ee,
	          new EnvironmentEvent(this, null, 
		                       EnvironmentEvent.STOPPED,
	                               getEnvironment().getId()));

        } else if ((ee.type == EnvironmentEvent.ACTIVATED)
	    || (ee.type == EnvironmentEvent.RESTORED)
	    || (ee.type == EnvironmentEvent.MOVED)
	    || (ee.type == EnvironmentEvent.CLONED)) {
          // If we have been activated, restored, moved, or cloned,
	  // acquire resources and go.
          start();
	}
        return true;

      } else if (e.closure == "binding") {
        // Deal with the results of binding requests
	activate(e);
	return true;
      }

      return false;
    }
  }

  // =======================================================================
  //                           The remote event sender
  // =======================================================================
  final class Sender extends AbstractHandler {
    /** Handles the specified event. */
    protected boolean handle1(Event e) {
      // Forward dynamic tuples to the remote resource.
      if (e instanceof DynamicTuple) {
        e.source = myReference;
	requestHandler.handle(
	    new RemoteEvent(this, null, receiverResource, e));
	return true;
      }
      return false;
    }
  }


  // =======================================================================
  //                           Descriptors
  // =======================================================================

  /** The component descriptor. */
  private static final ComponentDescriptor SELF =
    new ComponentDescriptor("one.world.rep.RemoteSender",
                            "Sends remote events",
                            true);

  /** The exported event handler descriptor for the main handler. */
  private static final ExportedDescriptor MAIN =
    new ExportedDescriptor("main",
                           "Environment event handler",
                           new Class[] { EnvironmentEvent.class },  
                           null, 
                           false);

  /** The imported event handler descriptor for the request handler. */
  private static final ImportedDescriptor REQUEST =
    new ImportedDescriptor("request",
                           "Handles remote requests",
                           new Class[] { BindingRequest.class,
			                 RemoteEvent.class },
                           null,  
                           false,
                           false);

  // =======================================================================
  //                           Constants
  // =======================================================================

  /** The inactive state. */
  private static final int INACTIVE = 0;

  /** The activating state. */
  private static final int ACTIVATING = 1;

  /** The active state. */
  private static final int ACTIVE = 2;

  /** The delay between sending remote events (1000 ms). */
  protected static final int DELAY = 1000;

  // =======================================================================
  //                           Instance fields
  // =======================================================================

  /** 
   * My remote event sender. 
   *
   * @serial  Must not be <code>null</code>.
   */
  private final EventHandler       sender;

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
  private final Component.Importer requestHandler;

  /** A timer component. */
  private final Timer timer;

  /** The remote resource to send events to. */
  private final SymbolicHandler receiverResource;

  /**
   * A reference to the exported event sender.  This must be reacquired
   * whenever the environment is activated, moved, cloned, or restored.
   */ 
  private transient RemoteReference myReference;

  /** A lease maintainer for the exported event sender. */
  private transient LeaseMaintainer leaseMaintainer;

  /** The timer notification object. */
  private transient Timer.Notification timerNotification;

  /** The application state. */
  private transient int state;

  /** The lock. */
  private transient Object lock;


  // =======================================================================
  //                           Constructor
  // =======================================================================

  /**
   * Creates a new remote event sender.
   *
   * @param  env  The environment for the new instance.
   * @param  resource  The remote resource to send events to.
   */
  public RemoteSender(Environment env, SymbolicHandler resource) {
    super(env);
    receiverResource = resource;

    timer            = getTimer();
    sender           = new Sender();

    main             = declareExported(MAIN, new MainHandler());
    requestHandler   = declareImported(REQUEST);

    state            = INACTIVE;
    lock             = new Object();
  }


  // =======================================================================
  //                           Component support
  // =======================================================================

  /** Get the component descriptor. */
  public ComponentDescriptor getDescriptor() {
    return (ComponentDescriptor)SELF.clone();
  }

  /** Export the sender. */
  private void start() {
    
    // Check and update the state.
    synchronized (lock) {
      if (state != INACTIVE) {
        return;
      }
      state = ACTIVATING;
    }

    // Export the remote event sender anonymously so that it may 
    // send events.  In the RemoteDescriptor, we do not specify a
    // descriptor for the event handler.
    Event request = 
        new BindingRequest(null, "binding",
	                   new RemoteDescriptor(sender),
			   Duration.FOREVER);
    new Operation(timer, requestHandler, main).handle(request);
  }
	 
  /**
   * Deal with the results of exporting the sender by attempting to
   * activate the component.  
   *
   * @param response  The response from the binding request.
   */
  void activate(Event response) {
    if (response instanceof BindingResponse) {
      BindingResponse br = (BindingResponse)response;

      // Store the reference to the exported event handler.
      myReference = (RemoteReference)br.resource;

      // Use a lease maintainer to prevent the lease from expiring.
      leaseMaintainer = new LeaseMaintainer(br.lease, br.duration,
                                            main, null,
					    timer);
  
      // Schedule a timer to tell the event sender when to send an event.
      Event timerEvent = new DynamicTuple(sender, "timer");
      timerNotification =
          timer.schedule(Timer.FIXED_DELAY,
  	               SystemUtilities.currentTimeMillis() + DELAY,
  		       DELAY,
  		       sender,
  		       timerEvent);
      
      // Since all resources have been acquired, set the state to active.
      synchronized (lock) {
        state = ACTIVE;
      }
      SystemUtilities.debug(this + " activated");

    } else {
      // If we don't get a binding response, log an error and stop,
      Throwable x;
      if (response instanceof ExceptionalEvent) {
        x = ((ExceptionalEvent)response).x;
      } else {
        x = new UnknownEventException(
	        "Unexpected response to binding request: "
	        + response.getClass());
      }
      SystemUtilities.debug(this + " could not export sender:");
      x.printStackTrace();
      stop(); 
    }
  }

  /** Unexport the event sender and cancel the timer. */
  private void stop() {

    // Check and update the state.
    synchronized (lock) {
      if (state == INACTIVE) {
        return;
      }
      state = INACTIVE;
    }

    // Cancel the timer.
    if (timerNotification != null) {
      timerNotification.cancel();
    }

    // Cancel the remote export.
    if (leaseMaintainer != null) {
      leaseMaintainer.cancel();
    }

    // Tell the environment we have stopped.
    requestHandler.handle(new EnvironmentEvent(main, null, 
                                               EnvironmentEvent.STOPPED,
					       getEnvironment().getId()));
  }

  /** 
   * Serialize this sender. 
   *
   * @serialData  The default fields while holding the lock.
   */
  private void writeObject(ObjectOutputStream out) throws IOException {
    synchronized (lock) {
      out.defaultWriteObject();
    }
  }

  /** Deserialize this sender. */
  private void readObject(ObjectInputStream in)
          throws IOException, ClassNotFoundException {

    // Read in the non-transient fields.
    in.defaultReadObject();

    // Restore the lock.
    lock = new Object();
  }

  /** 
   * Initialize the event sender.  The closure must be a string array 
   * that specifies 
   *
   * <p>
   * <ol>
   *   <li>The remote event receiver name (default is "receiver")</li>
   *   <li>The remote host name (discovery is used if no hostname is
   *        provided)</li>
   *   <li>The remote port (default is 
   *       {@link one.world.Constants#REP_PORT})</li>
   * </ol>
   * </p>
   *
   * @param env     The environment to run in.
   * @param closure The closure.
   *
   * @throws IllegalArgumentException
   *         Signals that the closure is not a string array or 
   *         that the port is not an integer.
   */
  public static void init(Environment env, Object closure) {

    String remoteHost = "localhost";
    String remoteName = "receiver";
    int    remotePort = Constants.REP_PORT;

    String[] args = (String[]) closure;

    if (args.length > 3) {
      throw new IllegalArgumentException(
          "Usage: RemoteSender [name] [hostname] [port]");
    }
    
    try {
      if (args.length > 0) {
        remoteName = args[0];
      } 
      if (args.length > 1) {
        remoteHost = args[1];
      }
      if (args.length > 2) {
        remotePort = Integer.parseInt(args[2]);
      }
      if (remotePort >= 65536) {
        remotePort = Constants.REP_PORT;
      }
    } catch (NumberFormatException x) {
      throw new IllegalArgumentException("Port is not an integer");
    }

    // Generate the remote resource descriptor from the remote host name,
    // the remote port number, and the remote event handler name.
    SymbolicHandler resource;
    if (args.length > 1) {
      // Host was specifed; use a NamedResource
      resource = new NamedResource(remoteHost, remotePort, remoteName);
    } else {
      // No host specified; use discovery
      resource = 
          new DiscoveredResource(
               new Query("receiver", Query.COMPARE_EQUAL, remoteName));
    }

    // Create a main component.
    RemoteSender    sender    = new RemoteSender(env, resource);

    // Link in the main component, the timer, and the environment request
    // handler.
    env.link("main", "main", sender);
    sender.link("request", "request", env);
  }
}
