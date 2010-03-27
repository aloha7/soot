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

package one.world.core;

import one.util.Bug;
import one.util.Guid;

import one.world.Constants;

import one.world.binding.BindingRequest;
import one.world.binding.BindingResponse;
import one.world.binding.Duration;
import one.world.binding.LeaseMaintainer;
import one.world.binding.UnknownResourceException;

import one.world.env.AcceptRequest;
import one.world.env.CreateRequest;
import one.world.env.CheckPointResponse;
import one.world.env.EnvironmentEvent;
import one.world.env.LoadRequest;
import one.world.env.MoveRequest;
import one.world.env.RenameRequest;
import one.world.env.RestoreRequest;

import one.world.io.SioResource;

import one.world.rep.RemoteDescriptor;
import one.world.rep.RemoteEvent;
import one.world.rep.RemoteReference;
import one.world.rep.ResolutionRequest;
import one.world.rep.ResolutionResponse;

import one.world.util.AbstractHandler;
import one.world.util.IOUtilities;
import one.world.util.PingPongEvent;
import one.world.util.Synchronous;
import one.world.util.SystemUtilities;
import one.world.util.Timer;

/**
 * Implementation of the root request manager. The request manager is
 * expected to be instantiated within the root environment and handles
 * requests passed up the environment hierarchy. It understands
 * requests on environments, binding requests on communication
 * channels over UDP and TCP as well as local tuple storage, and code
 * loading requests. It also understands {@link PingPongEvent ping
 * pong events}.
 *
 * <p><b>Imported and Exported Event Handlers</b></p>
 *
 * <p>Exported event handler(s):<dl>
 *    <dt>main</dt>
 *    <dd>Handles environment events; to be linked to the root
 *        environment's main imported event handler.</dd>
 *    <dt>request</dt>
 *    <dd>Handles environment events, binding requests on structured
 *        I/O communication channels and local tuple storage as well
 *        as for event handler exported to REP, remote events, and
 *        and code loading requests; to be linked to the root
 *        environment's monitor imported event handler.
 *        </dd>
 * </dl></p>
 *
 * <p>Imported event handler(s):<dl>
 *    <dt>udp</dt>
 *    <dd>Handles binding requests on structured I/O communication
 *        channels over UDP; to be linked to {@link
 *        one.world.io.DatagramIO}'s request exported event
 *        handler.
 *        </dd>
 *    <dt>tcp</dt>
 *    <dd>Handles binding requests on structured I/O communication
 *        channels over TCP; to be linked to {@link
 *        one.world.io.NetworkIO}'s request exported event
 *        handler.
 *        </dd>
 *    <dt>storage</dt>
 *    <dd>Handles binding requests on local tuple storage; to be
 *        linked to {@link one.world.io.TupleStore}'s request
 *        exported event handler.
 *        </dd>
 *    <dt>remote</dt>
 *    <dd>Handles binding requests for remotely exported resources;
 *        to be linked to {@link one.world.rep.RemoteManager}'s request
 *        exported event handler.
 *        </dd>
 *    <dt>env</dt>
 *    <dd>Handles environment events; to be linked to components
 *        that need to be notified of environment events processed
 *        by this component.
 *        </dd>
 * </dl></p>
 *
 * @version  $Revision: 1.45 $
 * @author   Robert Grimm
 */
public class RequestManager extends Component {

  // =======================================================================
  //                          The main handler
  // =======================================================================

  /** The main handler. */
  final class MainHandler extends AbstractHandler {
    
    /** Handle the specified event. */
    protected boolean handle1(Event e) {
      if (e instanceof EnvironmentEvent) {
        EnvironmentEvent ee = (EnvironmentEvent)e;

        if (EnvironmentEvent.ACTIVATED == ee.type) {
          if (Constants.DEBUG_ENVIRONMENT) {
            SystemLog.LOG.log(this, "Activating root environment");
          }

          // Pass on activated environment event.
          envv.handle(ee);

          // Export accept event handler.
          
          // =========================================================
          //
          // Note that there is a potential race condition between
          // initializing the remote manager and exporting the accept
          // event handler.  This race is avoided if above call is
          // synchronous, i.e., is not going through a concurrency
          // domain.
          //
          // Also, note that if the event handler for the remote
          // manager below is forced through a concurrency domain, the
          // system hangs with the synchronous timer interface. The
          // reason for this is that the above activated event is sent
          // within the global lock, which normally goes through a
          // concurrency domain. However, for the root environment
          // this event is sent directly and this code is executed
          // under the global lock.  Binding a remote resource
          // requires scheduling a timer, which requires getting a
          // wrapper from the environment (with the synchronous timer
          // interface), which requires the global lock held by the
          // thread executing this code. Voila, deadlock.
          //
          // Finally, note that the accept event handler exported by
          // the binding operation below must be wrapped. Otherwise,
          // environment migration will deadlock on the accepting
          // side, because environment migration captures the thread
          // processing the accept request (for the duration of the
          // moving protocol). If the accept event handler is not
          // wrapped, that thread is the thread reading events from
          // the network for REP, which prevents the system from
          // reading any more events. Voila, deadlock again.
          //
          // =========================================================

          BindingResponse br;

          try {
            br = IOUtilities.bind(remote, new
              RemoteDescriptor(wrap(accept), MovingProtocol.MOVE_ACCEPTOR),
                                  Duration.FOREVER);
          } catch (Exception x) {
            SystemLog.LOG.logError(this,
                                   "Unable to export accept event handler",
                                   x);
            return true;
          }

          remoteAccept    = (RemoteReference)br.resource;
          leaseMaintainer = new LeaseMaintainer(br.lease, br.duration,
                                                main, null, timer);

          SystemLog.LOG.log(this, "Root environment " +
                            getEnvironment().getId() + " activated");
          return true;

        } else if (EnvironmentEvent.STOP == ee.type) {
          // Release resources.
          leaseMaintainer.cancel();
          Synchronous.invoke(remote, new
            EnvironmentEvent(null, null, EnvironmentEvent.STOP,
                             getEnvironment().getId()));

          SystemLog.LOG.log(this, "Root environment " +
                            getEnvironment().getId() + " terminated");

          // Done stopping.
          respond(ee, new
            EnvironmentEvent(this, null, EnvironmentEvent.STOPPED,
                             getEnvironment().getId()));
          return true;

        } else if ((EnvironmentEvent.RESTORED == ee.type) ||
                   (EnvironmentEvent.MOVED    == ee.type) ||
                   (EnvironmentEvent.CLONED   == ee.type) ||
                   (EnvironmentEvent.STOP     == ee.type)) {
          throw new Bug("Unexpected environment event for root environment (" +
                        ee.toString() + ")");
        }
      }

      return false;
    }
  }

  
  // =======================================================================
  //                        The request handler
  // =======================================================================
  
  /** The event handler for processing requests. */
  final class RequestHandler extends AbstractHandler {

    /** Handle the specified event. */
    protected boolean handle1(Event e) {
      Guid requestorId;

      // Extract ID of requesting environment.
      DynamicTuple metaData = e.metaData;
      Object       o        = ((null == metaData)? null :
                               metaData.get(Constants.REQUESTOR_ID));
      if (o instanceof Guid) {
        requestorId = (Guid)o;
      } else {
        if (e instanceof ExceptionalEvent) {
          return false;
        } else {
          respond(e, new
            IllegalArgumentException("Request with no environment ID"));
          return true;
        }
      }

      // Handle the event.
      if ((e instanceof RemoteEvent) ||                        // Remote.
          (e instanceof ResolutionRequest)) {
        // Send a remote event or resolve a symbolic handler.
        remote.handle(e);
        return true;

      } else if (e instanceof BindingRequest) {                // Bind.
        BindingRequest request = (BindingRequest)e;

        if (request.descriptor instanceof SioResource) {
          SioResource resource = (SioResource)request.descriptor;

          switch (resource.type) {
          case SioResource.STORAGE:
            if (null != resource.remoteHost) {
              respond(e, new
                IllegalArgumentException("Unable to access remote tuple " +
                                         "storage"));
              return true;
            }

            // Resolve absolute, relative, and ID-relative paths to a
            // single ID.
            try {
              resource.ident = Environment.resolve(requestorId, resource.ident,
                                                   resource.path).getId();
              resource.path  = null;
            } catch (Exception x) {
              respond(e, x);
              return true;
            }

            storage.handle(request);
            return true;
            
          case SioResource.CLIENT:
          case SioResource.SERVER:
            tcp.handle(request);
            return true;

          case SioResource.INPUT:
          case SioResource.OUTPUT:
          case SioResource.DUPLEX:
          case SioResource.MULTICAST:
            udp.handle(request);
            return true;

          default:
            throw new Bug("Invalid type for structured I/O resource " +
                          "descriptor (" + resource.type +")");
          }
        } else if (request.descriptor instanceof RemoteDescriptor) {
	  remote.handle(request);
	  return true;
	}

        respond(e, new
          UnknownResourceException(request.descriptor.toString()));
        return true;

      } else if (e instanceof CreateRequest) {                 // Create.
        CreateRequest request = (CreateRequest)e;
        Environment   child;

        try {
          if (null != request.init) {
            child = Environment.create(requestorId, request.ident, request.name,
                                       request.inherit, request.init,
                                       request.initClosure);
          } else {
            child = Environment.create(requestorId, request.ident, request.name,
                                       request.inherit);
          }
        } catch (Exception x) {
          respond(e, x);
          return true;
        }

        respond(e, new
          EnvironmentEvent(this, null, EnvironmentEvent.CREATED, child.id));
        return true;

      } else if (e instanceof LoadRequest) {                   // Load.
        LoadRequest request = (LoadRequest)e;

        try {
          Environment.load(requestorId, request.ident,
                           request.init, request.initClosure);
        } catch (Exception x) {
          respond(e, x);
          return true;
        }

        respond(e, new
          EnvironmentEvent(this, null, EnvironmentEvent.LOADED,
                           request.ident));
        return true;

      } else if (e instanceof RestoreRequest) {                // Restore.
        RestoreRequest request = (RestoreRequest)e;
        boolean        restored;

        try {
          restored = Environment.restore(requestorId, request.ident,
                                         request.timestamp);
        } catch (Throwable x) {
          respond(e, x);
          return true;
        }

        if (! restored) {
          respond(e, new
            EnvironmentEvent(this, null, EnvironmentEvent.RESTORED,
                             request.ident));
        }
        return true;

      } else if (e instanceof RenameRequest) {                 // Rename.
        RenameRequest request = (RenameRequest)e;

        try {
          Environment.rename(requestorId, request.ident, request.name);
        } catch (Exception x) {
          respond(e, x);
          return true;
        }

        respond(e, new
          EnvironmentEvent(this, null, EnvironmentEvent.RENAMED, 
                           request.ident));
        return true;

      } else if (e instanceof MoveRequest) {                   // Move.
        MoveRequest request = (MoveRequest)e;
        SioResource location;

        // Parse location.
        try {
          location = new SioResource(request.location);
        } catch (IllegalArgumentException x) {
          respond(e, x);
          return true;
        }

        // Canonicalize remote host.
        if ((null != location.remoteHost) &&
            SystemUtilities.isLocalHost(location.remoteHost) &&
            ((-1 == location.remotePort) ||
             (Constants.REP_PORT == location.remotePort))) {
          location.remoteHost = null;
        }

        // Local and remote moving are handled differently.
        if (null != location.remoteHost) {
          // Remote move.

          String remoteHost   = location.remoteHost;
          int    remotePort   = location.remotePort;
          location.remoteHost = null;
          location.type       = SioResource.STORAGE;
          String remotePath   = location.toString();

          Environment.moveAway(requestorId, request.source, request.closure,
                               request.ident, remoteHost, remotePort,
                               remotePath, request.clone);
          return true;

        } else {
          // Local move.

          // Resolve all paths to a single ID.
          Guid newParentId;

          try {
            newParentId = Environment.resolve(requestorId, location.ident,
                                              location.path).getId();
          } catch (Exception x) {
            respond(e, x);
            return true;
          }

          if (request.clone) {
            try {
              Environment.copy(requestorId, request.ident, newParentId);
            } catch (Throwable x) {
              respond(e, x);
              return true;
            }

            respond(e, new
              EnvironmentEvent(this,null,EnvironmentEvent.MOVED,request.ident));
            return true;

          } else {
            boolean moved;
            try {
              moved = Environment.move(requestorId,request.ident,newParentId);
            } catch (Throwable x) {
              respond(e, x);
              return true;
            }

            if (! moved) {
              respond(e, new
                EnvironmentEvent(this, null, EnvironmentEvent.MOVED,
                                 request.ident));
            }
            return true;
          }
        }

      } else if (e instanceof AcceptRequest) {                 // Accept move.
        Environment.acceptMove(requestorId, (AcceptRequest)e);
        return true;
        
      } else if (e instanceof EnvironmentEvent) {
        EnvironmentEvent request = (EnvironmentEvent)e;

        switch (request.type) {

        case EnvironmentEvent.ACTIVATE:                        // Activate.
          try {
            Environment.activate(requestorId, request.ident);
          } catch (Exception x) {
            respond(e, x);
            return true;
          }

          // Only tell requesting environment if it is not the same as
          // the activated environment.
          if (! request.ident.equals(requestorId)) {
            respond(e, new
              EnvironmentEvent(this, null, EnvironmentEvent.ACTIVATED,
                               request.ident));
          }
          return true;

        case EnvironmentEvent.TERMINATE:                       // Terminate.
          try {
            Environment.terminate(requestorId, request.ident);
          } catch (Exception x) {
            respond(e, x);
            return true;
          }

          // Only tell requesting environment if it is not the same as
          // the terminated environment.
          if (! request.ident.equals(requestorId)) {
            respond(e, new
              EnvironmentEvent(this, null, EnvironmentEvent.TERMINATED,
                               request.ident));
          }
          return true;

        case EnvironmentEvent.UNLOAD:                          // Unload.
          try {
            Environment.unload(requestorId, request.ident);
          } catch (Exception x) {
            respond(e, x);
            return true;
          }

          // The requesting environment is guaranteed to be different
          // from the unloaded environment, because, to be unloaded,
          // an environment must be inactive. So, we can go ahead and
          // tell the requesting environment.
          respond(e, new
            EnvironmentEvent(this, null, EnvironmentEvent.UNLOADED,
                             request.ident));
          return true;

        case EnvironmentEvent.DESTROY:                         // Destroy.
          boolean destroyed;

          try {
            destroyed = Environment.destroy(requestorId, request.ident);
          } catch (Exception x) {
            respond(e, x);
            return true;
          }

          // Only tell requesting environment if it has not been
          // destroyed.
          if (! destroyed) {
            respond(e, new
              EnvironmentEvent(this, null, EnvironmentEvent.DESTROYED,
                               request.ident));
          }
          return true;

        case EnvironmentEvent.CHECK_POINT:                     // Check-point.
          long timestamp;

          try {
            timestamp = Environment.checkPoint(requestorId, request.ident);
          } catch (Throwable x) {
            respond(e, x);
            return true;
          }

          respond(e, new
            CheckPointResponse(this, null, request.ident, timestamp));
          return true;

        case EnvironmentEvent.CREATED:
        case EnvironmentEvent.LOADED:
        case EnvironmentEvent.ACTIVATED:
        case EnvironmentEvent.RESTORED:
        case EnvironmentEvent.CLONED:
        case EnvironmentEvent.RENAMED:
        case EnvironmentEvent.MOVED:
        case EnvironmentEvent.TERMINATED:
        case EnvironmentEvent.UNLOADED:
        case EnvironmentEvent.DESTROYED:
        case EnvironmentEvent.STOP:
        case EnvironmentEvent.STOPPED:
          break;

        default:
          throw new Bug("Invalid environment event type (" + request.type +
                        ")");
        }

      } else if (e instanceof PingPongEvent) {                 // Ping pong.
        PingPongEvent ppe = (PingPongEvent)e;

        if (ppe.pong) {
          return false;
        } else {
          respond(e, new PingPongEvent(this, null, true));
          return true;
        }
      }

      return false;
    }

  }


  // =======================================================================
  //                          The Accept Handler
  // =======================================================================
  
  /**
   * The event handler for processing requests to move environment(s)
   * to this node.
   */
  final class AcceptHandler extends AbstractHandler {

    /** Handle the specified event. */
    protected boolean handle1(Event e) {
      if (e instanceof RemoteEvent) {
        RemoteEvent re = (RemoteEvent)e;
        e              = re.event;

        if (e instanceof DynamicTuple) {
          DynamicTuple dt = (DynamicTuple)e;

          if (MovingProtocol.MSG_REQUEST.equals(dt.get(MovingProtocol.MSG))) {
            String remoteHost =
              (String)re.getMetaData(Constants.REQUESTOR_ADDRESS);
            Guid        envId;
            String      envName;
            String      path;
            Boolean     clone;
            SioResource resource;
            Environment parent;

            // Extract protocol payload and convert the path to a
            // structured I/O resource.
            try {
              envId    = (Guid)   dt.get(MovingProtocol.ENV_ID,   Guid.class,
                                         false);
              envName  = (String) dt.get(MovingProtocol.ENV_NAME, String.class,
                                         false);
              Environment.ensureName(envName);
              path     = (String) dt.get(MovingProtocol.PATH,     String.class,
                                         false);
              clone    = (Boolean)dt.get(MovingProtocol.CLONE,    Boolean.class,
                                         false);
              resource = new SioResource(path.startsWith("/") ?
                                         "sio://" + path : path);
            } catch (IllegalArgumentException x) {
              if (Constants.DEBUG_ENVIRONMENT) {
                SystemLog.LOG.logWarning(this, "Received malformed message (" +
                                         dt + ")", x);
              }
              respond(remote, dt, remoteAccept, x);
              return true;
            }

            // Validate the structured I/O resource.
            if (SioResource.STORAGE != resource.type) {
              IllegalArgumentException x = new
                IllegalArgumentException("Malformed path (" + path + ")");
              if (Constants.DEBUG_ENVIRONMENT) {
                SystemLog.LOG.logWarning(this, "Received malformed message (" +
                                         dt + ")", x);
              }
              respond(remote, dt, remoteAccept, x);
              return true;

            } else if (null != resource.remoteHost) {
              IllegalArgumentException x = new
                IllegalArgumentException("Path includes node (" + path + ")");
              if (Constants.DEBUG_ENVIRONMENT) {
                SystemLog.LOG.logWarning(this, "Received malformed message (" +
                                         dt + ")", x);
              }
              respond(remote, dt, remoteAccept, x);
              return true;
            }

            // Resolve to new parent environment.
            try {
              parent = Environment.resolve(null, resource.ident, resource.path);
            } catch (Exception x) {
              if (Constants.DEBUG_ENVIRONMENT) {
                SystemLog.LOG.logWarning(this, "Received malformed message (" +
                                         dt + ")", x);
              }
              respond(remote, dt, remoteAccept, x);
              return true;
            }

            if (Constants.DEBUG_ENVIRONMENT) {
              SystemLog.LOG.log(this, "Received request-move message (" +
                                dt + ")");
            }

            // Propagate the appropriate accept request up the
            // environment hierachy, starting with the new parent
            // environment.
            parent.propagate(new
              AcceptRequest(this, dt.closure, remoteHost, envId, envName,
                            parent.getId(), (SymbolicHandler)dt.source,
                            clone.booleanValue()));
            return true;
          }
        }
      }

      return false;
    }

  }


  // =======================================================================
  //                            Descriptors
  // =======================================================================
  
  /** The component descriptor for this request manager. */
  private static final ComponentDescriptor SELF =
    new ComponentDescriptor("one.world.core.RequestManager",
                            "The central request manager", true);

  /** The exported event handler descriptor for the main event handler. */
  private static final ExportedDescriptor MAIN =
    new ExportedDescriptor("main",
                           "Main event handler",
                           new Class[] { EnvironmentEvent.class },
                           new Class[] { TupleException.class,
                                         UnknownEventException.class },
                           false);

  /** The exported event handler descriptor for the request event handler. */
  private static final ExportedDescriptor REQUEST =
    new ExportedDescriptor("request",
                           "Event handler for processing requests",
                           new Class[] { EnvironmentEvent.class,
                                         CreateRequest.class,
                                         RestoreRequest.class,
                                         RenameRequest.class,
                                         MoveRequest.class,
                                         BindingRequest.class },
                           new Class[] { TupleException.class,
                                         IllegalStateException.class,
                                         IllegalArgumentException.class,
                                         SecurityException.class,
                                         UnknownResourceException.class,
                                         UnknownEventException.class },
                           false);

  /** The imported event handler descriptor for the UDP event handler. */
  private static final ImportedDescriptor UDP =
    new ImportedDescriptor("udp",
                           "Event handler for binding UPD-based " +
                           "communication channels",
                           new Class[] { BindingRequest.class },
                           null,
                           false,
                           true);

  /** The imported event handler descriptor for the TCP event handler. */
  private static final ImportedDescriptor TCP =
    new ImportedDescriptor("tcp",
                           "Event handler for binding TCP-based " +
                           "communication channels",
                           new Class[] { BindingRequest.class },
                           null,
                           false,
                           true);

  /** The imported event handler descriptor for the storage event handler. */
  private static final ImportedDescriptor STORAGE =
    new ImportedDescriptor("storage",
                           "Event handler for binding local tuple storage",
                           new Class[] { BindingRequest.class },
                           null,
                           false,
                           true);

  /** The imported event handler descriptor for the remote event handler. */
  private static final ImportedDescriptor REMOTE =
    new ImportedDescriptor("remote",
                           "Event handler for remote requests",
                           new Class[] { BindingRequest.class,
			                 ResolutionRequest.class,
					 RemoteEvent.class },
                           null,
                           false,
                           true);

  /** The imported event handler descriptor for the env event handler. */
  private static final ImportedDescriptor ENV =
    new ImportedDescriptor("env",
                           "Event handler for environment events",
                           new Class[] { EnvironmentEvent.class },
                           null,
                           false,
                           false);


  // =======================================================================
  //                          Instance fields
  // =======================================================================

  /**
   * The main event handler.
   *
   * @serial  Must not be <code>null</code>.
   */
  final EventHandler       main;

  /**
   * The binding request handler for UDP-based communication channels.
   *
   * @serial  Must not be <code>null</code>.
   */
  final Component.Importer udp;

  /**
   * The binding request handler for TCP-based communication channels.
   *
   * @serial  Must not be <code>null</code>.
   */
  final Component.Importer tcp;

  /**
   * The binding request handler for local tuple storage.
   *
   * @serial  Must not be <code>null</code>.
   */
  final Component.Importer storage;

  /**
   * The remote request handler.
   *
   * @serial  Must not be <code>null</code>.
   */
  final Component.Importer remote;

  /**
   * The env event handler. The field is named envv due to a name
   * conflict with Component.env.
   *
   * @serial  Must not be <code>null</code>.
   */
  final Component.Importer envv;

  /**
   * The accept event handler.
   *
   * @serial  Must not be <code>null</code>.
   */
  final AcceptHandler      accept;

  /**
   * The remote reference for the accept event handler.
   *
   * @serial
   */
  RemoteReference          remoteAccept;

  /**
   * The lease maintainer for the exported accept event handler.
   *
   * @serial
   */
  LeaseMaintainer          leaseMaintainer;

  /**
   * The timer for this request manager's environment.
   *
   * @serial
   */
  final Timer              timer;


  // =======================================================================
  //                            Constructor
  // =======================================================================

  /**
   * Create a new request manager in the specified environment.
   *
   * @param   env  The environment for the new the request manager.
   * @throws  SecurityException
   *               Signals that the caller does not have permission
   *               to manage environments.
   */
  public RequestManager(Environment env) {
    super(env);
    Environment.ensurePermission();
    main         = declareExported(MAIN,    new MainHandler());
    declareExported(REQUEST, new RequestHandler());
    udp          = declareImported(UDP);
    tcp          = declareImported(TCP);
    storage      = declareImported(STORAGE);
    remote       = declareImported(REMOTE);
    envv         = declareImported(ENV);
    accept       = new AcceptHandler();
    timer        = getTimer();
  }


  // =======================================================================
  //                   Request manager as a component
  // =======================================================================

  /** Get the component descriptor for this request manager. */
  public ComponentDescriptor getDescriptor() {
    return (ComponentDescriptor)SELF.clone();
  }

}
