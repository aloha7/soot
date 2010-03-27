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
package one.net.http;

import one.world.core.Component;
import one.world.core.ComponentDescriptor;
import one.world.core.Environment;
import one.world.core.Event;
import one.world.core.EventHandler;
import one.world.core.ExportedDescriptor;
import one.world.core.ImportedDescriptor;
import one.world.core.ExceptionalEvent;
import one.world.util.AbstractHandler;
import one.world.util.Timer;
import one.world.util.Operation;
import one.world.util.SystemUtilities;
import one.world.binding.BindingRequest;
import one.world.binding.BindingResponse;
import one.world.binding.LeaseMaintainer;
import one.world.binding.Duration;
import one.world.io.SimpleOutputRequest;
import one.world.io.OutputResponse;
import one.world.io.SioResource;
import one.world.env.EnvironmentEvent;
import java.io.IOException;
import java.io.ObjectInputStream;

/**
 * Uses an environment's tuple store as the backend
 * for storing log entries.
 *
 * <p><b>Imported and Exported Event Handlers</b></p>
 *
 * <p>Exported event handlers:<dl>
 *    <dt>log</dt>
 *    <dd> Accept <code>LogEvent</code>s and store them into
 *         the environment specified during construction.
 *        </dd>
 * </dl></p>
 *
 * <p>Imported event handlers:<dl>
 *    <dt>request</dt>
 *    <dd> The environment request handler.
 *        </dd>
 * </dl></p>
 *
 * @author   Daniel Cheah
 * @version  $Revision: 1.2 $
 */
public final class Logger extends Component {

  // =======================================================================
  //                           The log handler
  // =======================================================================

  /** The log exported event handler. */
  final class LogHandler extends AbstractHandler {
    /** Handle the specified event. */
    protected boolean handle1(Event e) {
      if (e instanceof LogEvent) {
        LogEvent le = (LogEvent)e;
        if (null != resourceOp) {
          // Write the log entry to the tuple store.
          resourceOp.handle(new SimpleOutputRequest(null, null, le));

          // If the debug flag and the level is appropriate to
          // log to the console.
          if (debug && le.level >= level) {
            SystemUtilities.debug(le.toString());
          }

        } else {
          // Dump to console if the resource is not available yet.
          SystemUtilities.debug(le.toString());
        }

        return true;

      } else if (e instanceof EnvironmentEvent) {
        EnvironmentEvent ee = (EnvironmentEvent)e;

        switch (ee.type) {
          case EnvironmentEvent.ACTIVATE:
            // Bind to the log resource.
            operation.handle(new BindingRequest(null, ee,
                                                new SioResource(logRoot), 
                                                Duration.FOREVER));
            return true;
         
          case EnvironmentEvent.STOP:
            // Cancel the lease of the log resource.
            if (null != lease) {
              lease.cancel();
            }
        
            // Tell whoever that we have stopped.
            respond(ee, new EnvironmentEvent(null, null, 
                                             EnvironmentEvent.STOPPED,
                                             getEnvironment().getId()));
            return true;

          default:
            return false;
        }

      } else if (e instanceof ExceptionalEvent) {
        // To prevent us from looping infinitely, we return false
        // to get an error message and a stack trace.
        return false;

      }

      return false;
    }
  }

  /** Continuation handler for the bound resource. */
  final class ResourceHandler extends AbstractHandler {
    /** Handle the specified event */
    protected boolean handle1(Event e) {
      if (e instanceof OutputResponse) {
        // The log entry was successfully written to the tuple
        // store
        return true;

      } else if (e instanceof ExceptionalEvent) {
        // To prevent us from looping infinitely, we
        // return false to give us an error message and a stack trace.
        return false;
      }

      return false;
    }
  }

  /** Continuation handler for request. */
  final class RequestHandler extends AbstractHandler {
    /** Handle the specified event */
    protected boolean handle1(Event e) {
      if (e instanceof BindingResponse) {
        BindingResponse res = (BindingResponse)e;

        // Create the lease maintainer to maintain our lease.
        lease      = new LeaseMaintainer(res.lease, res.duration, 
                                         resourceHandler, null, timer);

        // Create the operation to make using the resource easier.
        resourceOp = new Operation(timer, res.resource, resourceHandler);

        // Tell whomever activate us that we are activated.
        respond((EnvironmentEvent)res.closure,
                new EnvironmentEvent(null, null, 
                                     EnvironmentEvent.ACTIVATED,
                                     getEnvironment().getId()));
        return true;

      } else if (e instanceof ExceptionalEvent) {
        // Tell whomever activate us that an exception has
        // occurred.
        respond((EnvironmentEvent)e.closure, ((ExceptionalEvent)e).x);
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
    new ComponentDescriptor("one.net.http.Logger",
                            "Logger that uses tuplestore for storage",
                            true);

  /** The exported event handler descriptor for the log handler. */
  private static final ExportedDescriptor LOG =
    new ExportedDescriptor("log",
                           "The log exported event handle",
                           null,   // XXX
                           null,   // XXX
                           false);

  /** The imported event handler descriptor for the request handler. */
  private static final ImportedDescriptor REQUEST =
    new ImportedDescriptor("request",
                           "The request imported event handle",
                           null,   // XXX
                           null,   // XXX
                           false,
                           true);

  // =======================================================================
  //                           Instance fields
  // =======================================================================

  /**
   * The log exported event handler.
   *
   * @serial  Must not be <code>null</code>.
   */
  final EventHandler       log;

  /**
   * The request imported event handler.
   *
   * @serial  Must not be <code>null</code>.
   */
  final Component.Importer request;

  /** 
   * Name of the environment to use for our logs 
   * 
   * @serial  Must not be <code>null</code>
   */
  final String             logRoot;
  
  /**
   * Continuation handler for the bound resource.
   *
   * @serial  Must not be <code>null</code>
   */
  final ResourceHandler    resourceHandler;
  
  /**
   * Continuation handler for request.
   *
   * @serial  Must not be <code>null</code>
   */
  final RequestHandler      requestHandler;

  /** 
   * If debug is true, 
   * log entries above <code>level</code> are dumped to the console.
   */
  final boolean             debug;
  
  /** Level at which log entries are dumped to the console. */
  final int                 level;

  // =======================================================================
  //  Transient Fields
  // =======================================================================

  /** For use with one.world.util.Operation */
  transient Timer           timer;

  /** For interacting with one.world kernel */
  transient Operation       operation;

  /** For interacting with the bound resounce */
  transient Operation       resourceOp;

  /** For maintaining the lease on the bound resource */
  transient LeaseMaintainer lease;

  // =======================================================================
  //                           Constructor
  // =======================================================================

  /**
   * Create a new instance of <code>Logger</code>.
   *
   * @param env     The environment for the new instance.
   * @param logRoot The environment to use to store log entries.
   */
  public Logger(Environment env, String logRoot) {
    this(env, logRoot, false, 0);
  }

  /**
   * Create a new instance of <code>Logger</code>.
   *
   * @param env     The environment for the new instance.
   * @param logRoot The environment to use to store log entries.
   * @param debug   Send log entries to the system console.
   * @param level   The level at which log entries are sent to the system console.
   * @see LogEvent#INFO
   * @see LogEvent#WARNING
   * @see LogEvent#ERROR
   */
  public Logger(Environment env, String logRoot, boolean debug, int level) {
    super(env);

    log             = declareExported(LOG, new LogHandler());
    request         = declareImported(REQUEST);

    requestHandler  = new RequestHandler();
    resourceHandler = new ResourceHandler();

    this.logRoot    = logRoot;
    this.debug      = debug;
    this.level      = level;

    initTransient();
  }

  /** Initialize tranisent fields. */
  private void initTransient() {
    // Restore the transient fields.
    timer     = getTimer();
    operation = new Operation(timer, request, requestHandler);
  }

  // =======================================================================
  //                           Serialization
  // =======================================================================

  /** Deserialize a main component. */
  private void readObject(ObjectInputStream in)
    throws IOException, ClassNotFoundException {

    // Read the non-transient fields.
    in.defaultReadObject();

    initTransient();
  }

  // =======================================================================
  //                           Component support
  // =======================================================================

  /** Get the component descriptor. */
  public ComponentDescriptor getDescriptor() {
    return (ComponentDescriptor)SELF.clone();
  }

}
