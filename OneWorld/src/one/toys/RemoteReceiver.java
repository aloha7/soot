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
import one.world.core.Tuple;
import one.world.core.UnknownEventException;

import one.world.data.Name;

import one.world.env.EnvironmentEvent;

import one.world.rep.RemoteDescriptor;
import one.world.rep.RemoteEvent;

import one.world.util.AbstractHandler;
import one.world.util.Operation;
import one.world.util.SystemUtilities;
import one.world.util.Timer;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;


/**
 * Receives and logs remote events.  
 *
 * <p>Usage:
 * <pre>
 *     RemoteReceiver [name] [local]
 * </pre>
 * </p>
 *
 * <p>The name is the name to receive remote events as; the default 
 * is "receiver".  
 * If the optional local flag is "true", the handler will
 * be exported as a localized event handler rather than through the
 * discovery service.</p>
 *
 * <p><b>Imported and Exported Event Handlers</b></p>
 *
 * <p>Exported event handlers:<dl>
 *    <dt>main</dt>
 *    <dd> Handles environment events.</dd>
 * </dl></p>
 *
 * <p>Imported event handlers:<dl>
 *    <dt>request</dt>
 *    <dd>The environment request handler.</dd>
 * </dl></p>
 *
 * @see      RemoteSender
 * @see      one.world.rep
 * @version  $Revision: 1.4 $
 * @author   Janet Davis
 */
public final class RemoteReceiver extends Component {

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

          // If we get an activated, restored, moved, or cloned event,
	  // we've been started or restarted.  Acquire resources and go.
          start();
	}
        return true;

      } else if (e.closure == "binding") {
        // Attempt to activate the component. 
	activate(e);
        return true;
      }

      return false;
    }
  }

  // =======================================================================
  //                           The remote event logger
  // =======================================================================
  final class RemoteLogger extends AbstractHandler {
    /** Handles the specified event. */
    protected boolean handle1(Event e) {
      if (e instanceof RemoteEvent) {

        // Extract the nested event.
        Event nested = ((RemoteEvent)e).event;

	// Log the nested event and its source.
        SystemUtilities.debug(
	    this + " received " + nested + " from " + nested.source);
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
    new ComponentDescriptor("one.world.rep.RemoteReceiver",
                            "Receives and logs remote events",
                            true);

  /** The exported event handler descriptor for the main handler. */
  private static final ExportedDescriptor MAIN =
    new ExportedDescriptor("main",
                           "Environment event handler",
                           new Class[] { EnvironmentEvent.class },  
                           null, 
                           false);

  /** The imported event handler descriptor for the remote handler. */
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

  /** States. */
  private static final int INACTIVE = 0;
  private static final int ACTIVATING = 1;
  private static final int ACTIVE = 2;

  // =======================================================================
  //                           Instance fields
  // =======================================================================

  /** 
   * My remote event handler. 
   *
   * @serial  Must not be <code>null</code>.
   */
  private final EventHandler       logger;

  /**
   * The main exported event handler.
   *
   * @serial  Must not be <code>null</code>.
   */
  private final EventHandler       main;

  /**
   * The imported environment request handler.
   *
   * @serial  Must not be <code>null</code>.
   */
  private final Component.Importer requestHandler;

  /** A timer component. */
  private final Timer timer;

  /** A remote descriptor for the exported logging handler. */
  private final Tuple descriptor;

  /** A lease maintainer for the exported logging handler. */
  private transient LeaseMaintainer leaseMaintainer;

  /** The component state. */
  private transient int state;

  /** The lock. */
  private transient Object lock;


  // =======================================================================
  //                           Constructor
  // =======================================================================

  /**
   * Create a new instance of <code>RemoteReceiver</code>.
   *
   * @param  env  The environment for the new instance.
   * @param  descriptor  A descriptor for the exported logging handler.
   */
  public RemoteReceiver(Environment env, Tuple descriptor) {
    super(env);

    timer          = getTimer();
    logger         = new RemoteLogger();

    main           = declareExported(MAIN, new MainHandler());
    requestHandler = declareImported(REQUEST);

    state          = INACTIVE;
    lock            = new Object();
    this.descriptor = descriptor;
  }


  // =======================================================================
  //                           Component support
  // =======================================================================

  /** Get the component descriptor. */
  public ComponentDescriptor getDescriptor() {
    return (ComponentDescriptor)SELF.clone();
  }

  /** Export the logger. */
  private void start() {
    // Check and set the state.
    synchronized (lock) {
      if (state != INACTIVE) {
        return;
      }
      state = ACTIVATING;
    }

    // Export the logger so that it can receive remote events.
    Event request = 
        new BindingRequest(null, "binding",
	                   new RemoteDescriptor(logger, descriptor),
			   Duration.FOREVER);
    new Operation(timer, requestHandler, main).handle(request);
  }	 

  /**
   * Attempt to activate the component based on the response to the
   * binding request.
   *
   * @param response  The response to the binding request for the remote
   *                  export.
   */
  void activate(Event response) {
    if (response instanceof BindingResponse) {
      BindingResponse br = (BindingResponse)response;

      // Set up a lease maintainer to presvent the export lease from
      // expiring.
      leaseMaintainer = new LeaseMaintainer(br.lease, br.duration,
                                            main, null,
					    timer);

      // Mark the state as active.
      synchronized (lock) {
        state = ACTIVE;
      }

      SystemUtilities.debug(this + " activated");

    } else {
      // Log an exception and stop the component.
      Throwable x;
      if (response instanceof ExceptionalEvent) {
        x = ((ExceptionalEvent)response).x;
      } else {
        x = new UnknownEventException(
	        "Unexpected response to binding request: "
		+ response.getClass());
      }
      SystemUtilities.debug(this + " could not export receiver: ");
      x.printStackTrace();
      stop();
    }
  }

  /** Unexport the logger. */
  private void stop() {
    synchronized (lock) {
      if (state == INACTIVE) {
        return;
      }
      else state = INACTIVE;
    }

    // Cancel the export lease.
    if (leaseMaintainer != null) {
      leaseMaintainer.cancel();
    }

    // Notify the environment that this component has stopped.
    requestHandler.handle(new EnvironmentEvent(main, null,
                                               EnvironmentEvent.STOPPED,
					       getEnvironment().getId()));
  }

  /** 
   * Serialize this receiver. 
   *
   * @serialData  The default fields while holding the lock.
   */
  private void writeObject(ObjectOutputStream out) throws IOException {
    synchronized (lock) {
      out.defaultWriteObject();
    }
  }

  /** Deserialize this receiver. */
  private void readObject(ObjectInputStream in) 
          throws IOException, ClassNotFoundException {
    
    // Read in the non-transient fields.
    in.defaultReadObject();

    // Restore the lock.
    lock = new Object();
  }

  /** 
   * Initialize the event receiver. 
   *
   * @param env   The environment to run in.
   * @param closure  The closure should be an array of strings.  The
   *              The optional first argument is the name for the exported
   *              event handler.  The optional second argument should be
   *              "true" if the handler is to be exported as a localized
   *              resource.
   */
  public static void init(Environment env, Object closure) {
    String[] args = (String[]) closure;
    
    if (args.length > 2) {
      throw new IllegalArgumentException(
                "Usage: RemoteReceiver [name] [local]");
    }

    String name = "receiver";
    if (args.length > 0) {
      name = args[0];
    }

    Tuple descriptor;
    if (args.length > 1 && "true".equalsIgnoreCase(args[1])) {
      descriptor = new Name(name);
    } else {
      descriptor = new DynamicTuple();
      descriptor.set("receiver", name);
    }

    RemoteReceiver  receiver  = new RemoteReceiver(env, descriptor);

    env.link("main", "main", receiver);
    receiver.link("request", "request", env);
  }
}
