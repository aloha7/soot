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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.ObjectStreamException;
import java.io.OutputStream;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import java.net.InetAddress;
import java.net.UnknownHostException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.NoSuchElementException;
import java.util.Set;

import one.util.Bug;
import one.util.Guid;

import one.world.Constants;

import one.world.binding.BindingRequest;
import one.world.binding.BindingResponse;
import one.world.binding.Duration;
import one.world.binding.LeaseMaintainer;
import one.world.binding.UnknownResourceException;

import one.world.data.BinaryData;

import one.world.env.AcceptRequest;
import one.world.env.EnvironmentEvent;

import one.world.io.Query;
import one.world.io.QueryResponse;
import one.world.io.SioResource;
import one.world.io.TupleStore;

import one.world.rep.RemoteDescriptor;
import one.world.rep.NamedResource;
import one.world.rep.RemoteEvent;
import one.world.rep.RemoteReference;

import one.world.util.AbstractHandler;
import one.world.util.ExceptionHandler;
import one.world.util.IOUtilities;
import one.world.util.IteratorElement;
import one.world.util.NullHandler;
import one.world.util.Operation;
import one.world.util.Synchronous.ResultHandler;
import one.world.util.TimeOutException;
import one.world.util.Timer;

/**
 * Implementation of an environment. Environments are containers for
 * stored tuples, active computations, and other environments, thus
 * providing a hierarchical structuring for
 * <i>one.world</i>. Furthermore, environments are associated with
 * {@link ConcurrencyDomain concurrency domains}, which provide the
 * animators for their computations, and {@link ProtectionDomain
 * protection domains}, which determine isolation boundaries. Each
 * environment has its own concurrency domain. Each environment either
 * has its own protection domain or inherits the parent environment's
 * protection domain.
 *
 * <p><b>Environments as Components</b></p>
 *
 * <p>Environments are components, albeit privileged ones. They expose
 * three handlers for linking, an imported main handler, an exported
 * request handler, and an imported monitor handler. The main handler
 * notifies an environment's main component of state changes, such as
 * when the environment is activated or terminated. The request
 * handler accepts arbitrary events, but is typically used for
 * requesting operations on environments, operations to bind a
 * resource, or operations to load code. The monitor handler lets an
 * environment monitor the requests issued by descendant
 * environments.</p>
 *
 * <p>Requests issued to an environment's request handler are
 * delivered to the first ancestral environment whose monitor handler
 * is linked. The root environment is guaranteed to have a default
 * request manager linked to its monitor handler. This request manager
 * understands requests on environments, binding requests for
 * communication channels as well as local tuple storage, and code
 * loading requests.</p>
 *
 * <p>Events issued to an environment's request handler are annotated
 * with the ID of the requesting environment. This ID is accessible
 * through the event's meta-data, using the {@link
 * Constants#REQUESTOR_ID} field. If this ID is already defined, an
 * environment's request handler ensures that it identifies either the
 * requesting environment or one of its descendants and otherwise
 * signals an exceptional event to the source of the request.</p>
 *
 * <p>Components control environments through environment events as
 * well as accept, create, load, move, rename, and restore
 * requests. The corresponding events are defined in the
 * <code>one.world.env</code> package. The operations on environments
 * are:<ul>
 * <li><code>create</code> to create a new child environment,</li>
 * <li><code>load</code> to load code into an environment,</li>
 * <li><code>activate</code> to activate an environment,</li>
 * <li><code>check-point</code> to check-point the current state
 * of an environment and all its descendants,</li>
 * <li><code>restore</code> to restore environments to a previously
 * check-pointed state,</li>
 * <li><code>move</code> to move an environment and all its
 * descendants, which optionally clones the environment(s) as
 * well,</li>
 * <li><code>rename</code> to rename an environment,</li>
 * <li><code>terminate</code> to terminate the computation in an
 * environment,</li>
 * <li><code>unload</code> to unload code from an environment,</li>
 * <li><code>destroy</code> to destroy an environment and all its
 * descendants.</li>
 * </ul></p>
 *
 * <p><b>Environment States</b></p>
 *
 * <p>Environments can be in one of the following states.<ul>
 *
 * <li>In the {@link #INACTIVE} state, an environment has been
 * created, can be linked and can store tuples, but does not execute
 * computations.</li>
 *
 * <li>In the {@link #ACTIVE} state, an environment, in addition to
 * storing tuples, is also executing a computation.</li>
 *
 * <li>In the {@link #TERMINATING} state, an environment is currently
 * terminating, that is, going back from the active to the inactive
 * state.</li>
 *
 * <li>In the {@link #DESTROYING} state, an environment is currently
 * being destroyed.</li>
 *
 * <li>In the {@link #DESTROYED} state, an environment has been
 * destroyed. A destroyed environment does not execute computations
 * nor store tuples.</li>
 *
 * </ul></p>
 *
 * <p><b>Remarks</b></p>
 *
 * <p>Invoking most of the methods implemented by this class requires
 * the system permission to manage environments. The following methods
 * can be safely called by applications in <i>one.world</i>.<ul>
 * <li>{@link #getDescriptor()}</li>
 * <li>{@link #getId()}</li>
 * <li>{@link #getName()}</li>
 * <li>{@link #getConcurrencyDomain()}</li>
 * <li>{@link #getProtectionDomain()}</li>
 * <li>{@link #getParentId()}</li>
 * <li>{@link #getParentName()}</li>
 * <li>{@link #getChild(String)}</li>
 * <li>{@link #getChildren()}</li>
 * <li>{@link #lookupLocally(Guid)}</li>
 * <li>{@link #getStatus()}</li>
 * <li>{@link #toString()}</li>
 * <li>{@link #ensureName(String)}</li>
 * <li>{@link #ensurePermission()}</li>
 * </ul></p>
 *
 * <p>When requesting to perform an operation on an environment, that
 * operation may fail with an illegal state exception signalling that
 * the environment is currently being modified. This exception means
 * that another operation on the same environment is currently under
 * way, which affects the state of that environment. Examples include
 * the environment transitioning from the active to the inactive
 * state, the environment being loaded into, being renamed, or being
 * restored from a saved check-point. We chose to expose concurrent
 * operations on the same environment, because this implementation
 * choice has enabled a larger overall degree of concurrent
 * environment operations.</p>
 *
 * <p>Note that the root environment is always active after the system
 * has started up. The root environment cannot be loaded into,
 * renamed, moved, check-pointed, terminated, unloaded, or
 * destroyed. Also note that events passed between components in
 * different environments cannot reference any components, including
 * environments.</p>
 *
 * @see      EnvironmentEvent
 * @see      one.world.env.AcceptRequest
 * @see      one.world.env.CreateRequest
 * @see      one.world.env.LoadRequest
 * @see      one.world.env.RenameRequest
 * @see      one.world.env.RestoreRequest
 * @see      one.world.env.MoveRequest
 * @see      SystemPermission
 *
 * @version  $Revision: 1.100 $
 * @author   Robert Grimm

 */
public final class Environment extends Component {

  /** The serial version ID for this class. */
  static final long serialVersionUID = 5646492963140986781L;

  // =======================================================================
  //                       The environment descriptor
  // =======================================================================

  /**
   * Implementation of an environment descriptor. Environment
   * descriptors are used to communicate the structure of the
   * environment hierarchy between {@link Environment environments}
   * and the {@link TupleStore tuple store}.
   */
  public static final class Descriptor extends Tuple {
    
    /** The serial version ID for this class. */
    static final long serialVersionUID = 7627512194175130289L;

    /**
     * The ID of the environment represented by this descriptor.
     *
     * @serial   Must not be <code>null</code>.
     */
    public Guid   ident;

    /**
     * The name of the environment represented by this descriptor.
     *
     * @serial   Must not be <code>null</code>. Must be "<code>/</code>"
     *           for the root environment.
     */
    public String name;

    /**
     * The ID of the parent for the environment represented by this
     * descriptor.
     *
     * @serial  Must not be <code>null</code> unless this descriptor
     *          represents the root environment, in which case it
     *          must be <code>null</code>.
     */
    public Guid   parent;

    /**
     * The ID of the protection domain for the environment represented
     * by this descriptor.
     *
     * @serial  Must not be <code>null</code>.
     */
    public Guid   protection;

    /** Create a new, empty descriptor. */
    public Descriptor() {
      // Nothing to do.
    }

    /**
     * Create a new environment descriptor.
     *
     * @param  ident       The ID of the environment.
     * @param  name        The name of the environment.
     * @param  parent      The ID of the parent environment.
     * @param  protection  The ID of the protection domain.
     */
    public Descriptor(Guid ident, String name, Guid parent, Guid protection) {
      this.ident      = ident;
      this.name       = name;
      this.parent     = parent;
      this.protection = protection;
    }

    /**
     * Create a new environment descriptor.
     *
     * @param   env  The environment.
     */
    Descriptor(Environment env) {
      ident      = env.id;
      name       = env.name;
      parent     = ((root == env)? null : env.parent.id);
      protection = env.protection.id;
    }

    /** Validate this environment descriptor. */
    public void validate() throws TupleException {
      super.validate();
      if (null == ident) {
        throw new InvalidTupleException("Null environment ID for environment" +
                                        " descriptor (" + this + ")");
      } else if (null == name) {
        throw new InvalidTupleException("Null environment name for " +
                                        "environment descriptor ("+this+")");
     } else if ((null == parent) && (! "/".equals(name))) {
        throw new InvalidTupleException("Null parent ID of non-root " +
                                        "environment for environment " +
                                        "descriptor (" + this + ")");
      } else if (null == protection) {
        throw new InvalidTupleException("Null protection domain ID for " +
                                        "environment descriptor ("+this+")");
      }
    }

    /** Get a string representation of this environment descriptor. */
    public String toString() {
      return "#[Environment descriptor for " + ident + " named \"" + name +
        "\"]";
    }

  }


  // =======================================================================
  //                The object output stream for checkpoints
  // =======================================================================

  static final class Output extends ObjectOutputStream {

    /**
     * Create a new object output stream for checkpoints.
     *
     * @param   out  The underlying output stream.
     * @throws  IOException
     *               Signals an exceptional condition while creating
     *               the output stream.
     */
    Output(OutputStream out) throws IOException {
      super(out);
    }

    /**
     * Annotate the specified class. This method writes the ID of the
     * protection domain that originally loaded the specified class to
     * the object output stream. If the class has not been loaded by a
     * protection domain class loader, this method writes
     * <code>null</code> as the ID.
     *
     * @param  cl  The class to annotate.
     */
    protected void annotateClass(Class cl) throws IOException {
      ClassLoader loader = cl.getClassLoader();

      if (loader instanceof ProtectionDomain.Loader) {
        // Determine the effective ID.
        Guid   id1 = ((ProtectionDomain.Loader)loader).getId();
        Object id2 = ((Map)serializationSet.get()).get(id1);
        
        // Sanity check.
        if (null == id2) {
          throw new IllegalStateException("Attempting to serialize class " +
                                          "from environment outside check-" +
                                          "point (" + cl + ")");
        }

        // Write out the ID.
        writeObject(id2);
      } else {
        writeObject(null);
      }
    }

  }


  // ======================================================================
  //                The object input stream for checkpoints
  // =======================================================================

  static final class Input extends ObjectInputStream {

    /**
     * Create a new object input stream for checkpoints.
     *
     * @param   in  The underlying input stream.
     * @throws  IOException
     *              Signals an exceptional condition while creating
     *              the input stream.
     */
    Input(InputStream in) throws IOException {
      super(in);
    }

    /**
     * Resolve the specified class. This method consumes the 
     * ID of the protection domain that originally loaded the
     * specified class and loads the class with the corresponding
     * class loader.
     *
     * @param  cl  The class to resolve.
     */
    protected Class resolveClass(ObjectStreamClass cl)
      throws IOException, ClassNotFoundException {

      Guid id = (Guid)readObject();
      
      if (null == id) {
        return super.resolveClass(cl);
      } else {
        Environment env = (Environment)((Map)serializationSet.get()).get(id);
        if (null == env) {
          throw new CheckPointException("Undeclared environment in check-point"
                                        + " (" + id + ")");
        }

        return Class.forName(cl.getName(), false, env.protection.loader);
      }
    }

  }


  // =======================================================================
  //                       The check-point exception
  // =======================================================================

  /**
   * Implementation of a check-point exception. A check-point
   * exception signals an inconsistent check-point, more specifically
   * that a check-point contains an environment that is not in the
   * serialization set. It is raised by an environment's
   * <code>readResolve()</code> method.
   */
  static final class CheckPointException extends RuntimeException {

    /** Create a new check-point exception. */
    CheckPointException() {
      // Nothing to do.
    }

    /**
     * Create a new check-point exception with the specified detail
     * message.
     *
     * @param   msg  The detail message.
     */
    CheckPointException(String msg) {
      super(msg);
    }

  }


  // =======================================================================
  //                       The environment reference
  // =======================================================================

  /**
   * Implementation of an environment reference. An environment
   * reference replaces an actual environment in check-points.
   */
  static final class Reference implements java.io.Serializable {

    /** The serial version ID for this class. */
    static final long serialVersionUID = -979182612464360716L;

    /**
     * The ID of the environment pointed to by this environment
     * reference.
     *
     * @serial  Must not be <code>null</code>, unless this reference
     *          points to the unique dummy environment.
     */
    private final Guid id;

    /**
     * Create a new environment reference for the environment with the
     * specified ID. The specified ID must not be <code>null</code>,
     * unless the new reference points to the unique dummy
     * environment.
     *
     * @param   id  The environment ID.
     */
    Reference(Guid id) {
      this.id = id;
    }

    /** Resolve this environment reference during deserialization. */
    private Object readResolve() throws ObjectStreamException {

      // Check for the dummy environment.
      if (null == id) {
        return DUMMY;
      }

      // Resolve to actual environment.
      Environment env = (Environment)((Map)serializationSet.get()).get(id);

      if (null == env) {
        // Signal an inconsistent check-point.
        throw new CheckPointException("Undeclared environment in check-point" +
                                      " (" + id + ")");
      }

      return env;
    }
  }


  // =======================================================================
  //                         The request handler
  // =======================================================================

  /** The event handler for requests. */
  final class RequestHandler extends AbstractHandler {

    /** The serial version ID for this class. */
    static final long serialVersionUID = 2692049908406869546L;

    /**
     * Get the environment for this request handler.
     *
     * @return  The environment for this request handler.
     */
    Environment getEnvironment() {
      return Environment.this;
    }

    /** Handle the specified event. */
    protected boolean handle1(Event e) {
      // Validate the event.
      if (isNotValid(e)) {
        return true;
      }

      // Immediately handle stopped environment events for this environment.
      if (e instanceof EnvironmentEvent) {
        EnvironmentEvent ee = (EnvironmentEvent)e;

        if (id.equals(ee.ident) &&
            (EnvironmentEvent.STOPPED == ee.type)) {
          try {
            terminate();
          } catch (IllegalStateException x) {
            respond(e, x);
          }
          
          return true;
        }
      }

      // Annotate the event with the ID of the originating environment.
      DynamicTuple metaData = e.metaData;
      Object       o        = ((null == metaData)? null :
                               metaData.get(Constants.REQUESTOR_ID));

      if (o instanceof Guid) {
        try {
          synchronized (GLOBAL_LOCK) {
            ensureLineage(lookup0((Guid)o), Environment.this);
          }
        } catch (SecurityException x) {
          respond(e, x);
          return true;
        } catch (IllegalArgumentException x) {
          // The original requesting environment is not on this
          // node. The current requestor takes over.
          metaData.set(Constants.REQUESTOR_ID, id);
        }
      } else {
        if (null == metaData) {
          metaData   = new DynamicTuple();
          e.metaData = metaData;
        }

        metaData.set(Constants.REQUESTOR_ID, id);
      }

      // Find the first ancestral environment which is active and
      // whose monitor handler is linked.
      Environment target = Environment.this;

      if (target != root) {
        while (true) {
          Environment child = target;
          target            = target.parent;

          if (null == target) {
            // The environment has been destroyed.
            respond(e, new
              IllegalStateException("Environment is being or has been " +
                                    "destroyed (" + child.id + ")"));
            return true;

          } else if (target == root) {
            // We reached the root environment.
            break;

          } else if ((ACTIVE == target.status) && target.monitor.isLinked()) {
            // Try to deliver the event to the monitor event handler.
            synchronized (GLOBAL_LOCK) {
              // Verify that the environment really is active.
              if (ACTIVE == target.status) {
                // Note that it is safe to deliver the event under the
                // global lock, b/c it is processed by an animator.
                try {
                  target.monitor.handle(e);
                } catch (NotLinkedException x) {
                  // Continue with parent environment.
                  continue;
                } catch (IllegalStateException x) {
                  // Continue with parent environment.
                  continue;
                } catch (NoBufferSpaceException x) {
                  // All queues are full. Drop this event.
                  return true;
                }

                // Event delivered.
                return true;
              }
            }
          }
        }
      }

      // The root environment is guaranteed to be active and its
      // monitor event handler is guaranteed to be linked.
      // Furthermore, this invocation is not going through an
      // animator.
      target.monitor.handle(e);
      return true;
    }

  }


  // =======================================================================
  //                       The root request handler
  // =======================================================================

  /** The root environment's event handler for requests. */
  final class RootRequestHandler extends AbstractHandler {

    /** Handle the specified event. */
    protected boolean handle1(Event e) {
      // Validate the event.
      if (Constants.DEBUG_EVENTS) {
        if (isNotValid(e)) {
          return true;
        }
      }

      // Annotate the event with the ID of the originating environment.
      DynamicTuple metaData = e.metaData;
      Object       o        = ((null == metaData)? null :
                               metaData.get(Constants.REQUESTOR_ID));

      if (o instanceof Guid) {
        try {
          synchronized (GLOBAL_LOCK) {
            ensureLineage(lookup0((Guid)o), Environment.this);
          }
        } catch (SecurityException x) {
          respond(e, x);
          return true;
        } catch (IllegalArgumentException x) {
          // The original requesting environment is not on this
          // node. The current requestor takes over.
          metaData.set(Constants.REQUESTOR_ID, id);
        }
      } else {
        if (null == metaData) {
          metaData   = new DynamicTuple();
          e.metaData = metaData;
        }

        metaData.set(Constants.REQUESTOR_ID, id);
      }

      // The root environment is guaranteed to be active and its
      // monitor event handler is guaranteed to be linked.
      // Furthermore, this invocation is not going through an
      // animator.
      monitor.handle(e);
      return true;
    }

  }


  // =======================================================================
  //         The super class for environment senders and receivers
  // =======================================================================

  /**
   * The super class for environment senders and receivers. Note that
   * senders and receivers are not serializable.
   *
   * @see  MovingProtocol
   */
  static abstract class Mover extends AbstractHandler {

    /** The state of this mover. */
    int             state;

    /** The operation for this mover. */
    Operation       operation;

    /** The lease maintainer for exporting the operation's response handler. */
    LeaseMaintainer leaseMaintainer;

    /** The sender as a symbolic handler. */
    SymbolicHandler sender;

    /** The receiver as a symbolic handler. */
    SymbolicHandler receiver;

    /** The root of the environment tree being moved. */
    Environment     env;

    /**
     * The serialization set of the environments being moved. Note
     * that we create a new map and fill it in when processing the
     * descriptors. That map is then copied into the appropriate
     * threadlocal when creating or restoring the checkpoint. We
     * cannot rely on the threadlocal from the beginning, because the
     * checkpoint may be processed by a different thread from the one
     * filling the set.
     */
    Map             set;

    /** The closure for the tuple store. */
    Object          tupleStoreClosure;

    /** The flag for whether the environments are being cloned. */
    boolean         clone;

    /** Create a new mover. */
    Mover() {
      state = MovingProtocol.STATE_INITIAL;
    }

    /**
     * Serialize this mover. This method throws a
     * <code>NotSerializableException</code>, as movers are not
     * serializable.
     *
     * @serialData  Nothing as movers are not serializable.
     */
    private void writeObject(ObjectOutputStream out) throws IOException {
      throw new NotSerializableException(getClass().getName());
    }

    /**
     * Get a string representation of the specified state.
     *
     * @param   state  The state code.
     * @return         The corresponding string representation.
     */
    static String format(int state) {
      switch (state) {
      case MovingProtocol.STATE_ERROR:
        return "error";
      case MovingProtocol.STATE_INITIAL:
        return "initial";
      case MovingProtocol.STATE_DESCRIPTOR:
        return "descriptor";
      case MovingProtocol.STATE_CONTENT:
        return "content";
      case MovingProtocol.STATE_FINAL:
        return "final";
      default:
        throw new Bug("Moving protocol in unknown state (" + state + ")");
      }
    }

  }


  // =======================================================================
  //                       The environment sender
  // =======================================================================

  /**
   * The sender for sending moving environments.
   *
   * @see  MovingProtocol
   */
  static final class Sender extends Mover {
    
    /** The ID of the requesting environment. */
    Guid         requestorId;

    /** The result handler. */
    EventHandler resultHandler;
    
    /** The result closure. */
    Object       resultClosure;

    /** The remote host. */
    String       remoteHost;

    /** The remote port. */
    int          remotePort;

    /** The remote path for the new parent. */
    String       remotePath;

    /** The list of environments being moved. */
    List         envList;

    /** The restart set. */
    Set          restartSet;

    /** The environment iterator. */
    Iterator     envIter;

    /** The current environment ID. */
    Guid         currentId;

    /** The tuple iterator. */
    Iterator     tupleIter;

    /** The checkpoint. */
    CheckPoint   cp;

    /**
     * Create a new sender.
     *
     * @param  requestorId    The ID of the requesting environment.
     * @param  resultHandler  The result handler.
     * @param  resultClosure  The closure for the result.
     * @param  env            The environment being moved.
     * @param  remoteHost     The remote host.
     * @param  remotePort     The remote port.
     * @param  remotePath     The remote path for the new parent.
     * @param  clone          The flag for whether the environments are
     *                        being cloned.
     */
    Sender(Guid requestorId, EventHandler resultHandler, Object resultClosure,
           Environment env, String remoteHost, int remotePort, 
           String remotePath, boolean clone) {
      this.requestorId   = requestorId;
      this.resultHandler = resultHandler;
      this.resultClosure = resultClosure;
      this.env           = env;
      this.remoteHost    = remoteHost;
      this.remotePort    = remotePort;
      this.remotePath    = remotePath;
      this.clone         = clone;
    }

    /** Handle the specified event. */
    protected boolean handle1(Event e) {
      DynamicTuple reply;

      if (e instanceof BindingResponse) {
        BindingResponse br = (BindingResponse)e;

        try {
          synchronized (this) {
            if (MovingProtocol.STATE_INITIAL != state) {
              LeaseMaintainer.cancel(br.lease);
              throw new
                IllegalStateException("Received binding response in illegal " +
                                      "moving protocol state (" +
                                      format(state) + ")");
            } else if (null != leaseMaintainer) {
              throw new
                IllegalStateException("Received duplicate binding response");
            }

            // Make sure the response handler stays exported.
            sender        = (SymbolicHandler)br.resource;
            leaseMaintainer = new
              LeaseMaintainer(br.lease,br.duration,this,e.closure,root.timer);
          }
        } catch (Exception x) {
          if (Constants.DEBUG_ENVIRONMENT) {
            SystemLog.LOG.logWarning(this, "Moving protocol error", x);
          }
          if (enterErrorState()) {
            resultHandler.handle(new ExceptionalEvent(this, resultClosure, x));
          }
        }

        if (Constants.DEBUG_ENVIRONMENT) {
          SystemLog.LOG.log(this, "Exported sender as " + sender);
        }

        // Start the moving protocol by sending a request-move message.
        reply = new DynamicTuple(sender, e.closure);
        reply.set(MovingProtocol.MSG,      MovingProtocol.MSG_REQUEST        );
        reply.set(MovingProtocol.ENV_ID,   env.id                            );
        reply.set(MovingProtocol.ENV_NAME, env.name                          );
        reply.set(MovingProtocol.PATH,     remotePath                        );
        reply.set(MovingProtocol.CLONE,    (clone?Boolean.TRUE:Boolean.FALSE));

        operation.handle(new
          RemoteEvent(this, e.closure, new
            NamedResource(remoteHost, ((-1 == remotePort) ?
                                       Constants.REP_PORT :
                                       remotePort),
                          MovingProtocol.MOVE_ACCEPTOR),
                      reply));

        if (Constants.DEBUG_ENVIRONMENT) {
          SystemLog.LOG.log(this, "Send request-move message (" + reply + ")");
        }

        return true;

      } else if (e instanceof RemoteEvent) {
        e = ((RemoteEvent)e).event;

        // Extract receiver.
        synchronized (this) {
          if (null == receiver) {
            receiver = (SymbolicHandler)e.source;
          }
        }

        if (e instanceof DynamicTuple) {
          DynamicTuple dt  = (DynamicTuple)e;
          Object       msg = dt.get(MovingProtocol.MSG);

          try {
            if (MovingProtocol.MSG_ALLOW.equals(msg)) {
              if (Constants.DEBUG_ENVIRONMENT) {
                SystemLog.LOG.log(this, "Received allow-move message (" + dt +
                                  ")");
              }

              // Extract payload.
              Guid newParentId =
                (Guid)dt.get(MovingProtocol.ENV_ID, Guid.class, false);

              // Process message.
              Descriptor[] source; // Source descriptors.
              Descriptor[] target; // Target descriptors.

              synchronized (this) {
                if (MovingProtocol.STATE_INITIAL != state) {
                  throw new
                    IllegalStateException("Illegal moving protocol state (" +
                                          format(state) + ")");
                }

                // Verify receiver.
                if (! receiver.equals(e.source)) {
                  throw new
                    IllegalStateException("Allow-move message not first message"
                                          + " received");
                }

                // Prepare environment tree for move.
                restartSet   = new HashSet();

                synchronized (GLOBAL_LOCK) {
                  // Temporarily mark environment as stable again.
                  env.stable = true;
                  
                  // Make sure we are still running and that env is
                  // not being destroyed.
                  ensureAlive();
                  ensureAlive(env);

                  // Set up serialization set.
                  set = new HashMap();
                  set.put(env.id, env);
                  collectDescendants(env, set);

                  // Set up list of environments.
                  envList = new ArrayList(set.values());

                  // Make sure environments are stable.
                  Iterator iter = envList.iterator();
                  while (iter.hasNext()) {
                    Environment tmp = (Environment)iter.next();
                    if ((TERMINATING == tmp.status) || (! tmp.stable)) {
                      throw new
                        IllegalStateException("Environment currently being " +
                                              "modified (" + tmp.id + ")");
                    }
                  }

                  // Transition to the descriptor state. We only do
                  // this at this point, because we now know that all
                  // environments in the tree to be moved are
                  // stable. Consequently, during error recovery, we
                  // can now remark them as stable again.
                  state = MovingProtocol.STATE_DESCRIPTOR;

                  // (1) Mark environments as unstable, (2) replace
                  // environments in the serialization set with their
                  // IDs, (3) deactivate active animators, (4) set up
                  // the restart set, i.e., the set of environments
                  // whose animators need to be re-activated after the
                  // checkpoint has been created, and (5) set up the
                  // arrays of source and target descriptors.
                  int i  = 0;
                  source = new Descriptor[envList.size()];
                  target = new Descriptor[source.length ];

                  iter = envList.iterator();
                  while (iter.hasNext()) {
                    Environment tmp = (Environment)iter.next();

                    // Mark as unstable.
                    tmp.stable = false;

                    // Replace environment with its ID.
                    if (clone) {
                      set.put(tmp.id, new Guid());
                    } else {
                      set.put(tmp.id, tmp.id);
                    }

                    // Deactivate active animators and set up restart set.
                    Animator anim = tmp.concurrency.anim;
                    if ((null != anim) && (Animator.ACTIVE==anim.getStatus())) {
                      anim.setStatus(Animator.INACTIVE);
                      restartSet.add(tmp.id);
                    }

                    // Set up source descriptors.
                    source[i] = new Descriptor(tmp);
                    if (! clone) {
                      if (env.id.equals(tmp.id)) {
                        target[i]        = new Descriptor(tmp);
                        target[i].parent = newParentId;
                      } else {
                        target[i]        = source[i];
                      }
                    }

                    // Increment index.
                    i++;
                  }

                  if (clone) {
                    // Set up target descriptors for cloning moves in
                    // a separate loop (the serialization set must
                    // completely map IDs to IDs for this loop to
                    // work).
                    iter = envList.iterator();
                    i    = 0;
                    while (iter.hasNext()) {
                      Environment tmp  = (Environment)iter.next();
                      Descriptor  desc = new Descriptor(tmp);
                      desc.ident       = (Guid)set.get(desc.ident);
                      if (env.id.equals(tmp.id)) {
                        desc.parent    = newParentId;
                      } else {
                        desc.parent    = (Guid)set.get(desc.parent);
                      }
                      desc.protection  = (Guid)set.get(desc.protection);
                      target[i]        = desc;
                      i++;
                    }
                  }
                }

                // Copy over serialization set.
                Map actualSet = (Map)serializationSet.get();
                actualSet.clear();
                actualSet.putAll(set);

                // Create checkpoint.
                try {
                  cp = env.checkPoint(envList, actualSet, clone);
                } finally {
                  // Clear serialization set.
                  actualSet.clear();
                }

                // Terminate moved environments.
                if (! clone) {
                  Iterator iter = envList.iterator();
                  while (iter.hasNext()) {
                    Environment tmp = (Environment)iter.next();

                    if (restartSet.contains(tmp.id)) {
                      // Re-activate animator.
                      Animator anim = tmp.concurrency.anim;
                      if (null != anim) {
                        anim.setStatus(Animator.ACTIVE);
                      }

                      // Tell main event handler about the pending termination.
                      Event result = null;
                      try {
                        ResultHandler    rh = new ResultHandler();
                        EnvironmentEvent ee = new
                          EnvironmentEvent(rh, null, EnvironmentEvent.STOP,
                                           tmp.id);
                        tmp.main.handleForced(ee);
                        if (Constants.DEBUG_ENVIRONMENT) {
                          SystemLog.LOG.log(tmp, "Sent stop environment event");
                        }
                        result = rh.getResult(Constants.SYNCHRONOUS_TIMEOUT);
                      } catch (Exception x) {
                        SystemLog.LOG.logWarning(tmp, "Unexpected exception " +
                                                 "when sending stop environ" +
                                                 "ment event", x);
                      }
                      if ((null != result) &&
                          ((! (result instanceof EnvironmentEvent)) ||
                           (EnvironmentEvent.STOPPED !=
                            ((EnvironmentEvent)result).type))) {
                        SystemLog.LOG.logWarning(tmp, "Unexpected event when " +
                                                 "terminating ("+result+")");
                      }

                      // Terminate the concurrency domain. This
                      // operation can be performed without holding a
                      // monitor because the unstable flag prevents
                      // anyone from changing the status of the
                      // environment.
                      tmp.concurrency.terminate();
                    }
                  }
                }

                // Tell the tuple store.
                tupleStoreClosure = TupleStore.startMove(source, (! clone));
              }

              // Send environment descriptors.
              reply = new DynamicTuple(sender, e.closure);
              reply.set(MovingProtocol.MSG, MovingProtocol.MSG_DESCRIPTORS);
              reply.set(MovingProtocol.DESCRIPTORS, target                );

              operation.handle(new
                RemoteEvent(this, e.closure, receiver, reply));

              if (Constants.DEBUG_ENVIRONMENT) {
                SystemLog.LOG.log(this, "Sent send-descriptors message (" +
                                  reply + ")");
              }

              return true;

            } else if (MovingProtocol.MSG_CONFIRM.equals(msg)) {
              if (Constants.DEBUG_ENVIRONMENT) {
                SystemLog.LOG.log(this, "Received confirm-descriptors " +
                                  "message (" + dt + ")");
              }

              Object tuple;
              synchronized (this) {
                if (MovingProtocol.STATE_DESCRIPTOR != state) {
                  throw new
                    IllegalStateException("Illegal moving protocol state (" +
                                          format(state) + ")");
                }
                state = MovingProtocol.STATE_CONTENT;
                tuple = nextTuple();
              }

              reply = new DynamicTuple(sender, e.closure);
              if (null != tuple) {
                reply.set(MovingProtocol.MSG,    MovingProtocol.MSG_SEND_TUPLE);
                reply.set(MovingProtocol.ENV_ID, set.get(currentId)           );
                reply.set(MovingProtocol.TUPLE,  tuple                        );

                operation.handle(new
                  RemoteEvent(this, e.closure, receiver, reply));

                if (Constants.DEBUG_ENVIRONMENT) {
                  SystemLog.LOG.log(this,"Sent send-tuple message ("+reply+")");
                }

              } else {
                reply.set(MovingProtocol.MSG, MovingProtocol.MSG_CHECK_POINT);
                reply.set(MovingProtocol.CHECK_POINT, cp                    );

                operation.handle(new
                  RemoteEvent(this, e.closure, receiver, reply));

                if (Constants.DEBUG_ENVIRONMENT) {
                  SystemLog.LOG.log(this, "Sent send-check-point message (" +
                                    reply + ")");
                }
              }

              return true;

            } else if (MovingProtocol.MSG_CONFIRM_TUPLE.equals(msg)) {
              if (Constants.DEBUG_ENVIRONMENT) {
                SystemLog.LOG.log(this, "Received confirm-tuple message (" +
                                  dt + ")");
              }

              Object tuple;
              synchronized (this) {
                if (MovingProtocol.STATE_CONTENT != state) {
                  throw new
                    IllegalStateException("Illegal moving protocol state (" +
                                          format(state) + ")");
                }
                tuple = nextTuple();
              }

              reply = new DynamicTuple(sender, e.closure);
              if (null != tuple) {
                reply.set(MovingProtocol.MSG,    MovingProtocol.MSG_SEND_TUPLE);
                reply.set(MovingProtocol.ENV_ID, set.get(currentId)           );
                reply.set(MovingProtocol.TUPLE,  tuple                        );

                operation.handle(new
                  RemoteEvent(this, e.closure, receiver, reply));

                if (Constants.DEBUG_ENVIRONMENT) {
                  SystemLog.LOG.log(this,"Sent send-tuple message ("+reply+")");
                }

              } else {
                reply.set(MovingProtocol.MSG, MovingProtocol.MSG_CHECK_POINT);
                reply.set(MovingProtocol.CHECK_POINT, cp                    );

                operation.handle(new
                  RemoteEvent(this, e.closure, receiver, reply));

                if (Constants.DEBUG_ENVIRONMENT) {
                  SystemLog.LOG.log(this, "Sent send-check-point message (" +
                                    reply + ")");
                }
              }

              return true;

            } else if (MovingProtocol.MSG_COMPLETE.equals(msg)) {
              if (Constants.DEBUG_ENVIRONMENT) {
                SystemLog.LOG.log(this, "Received complete-move message (" +
                                  dt + ")");
              }

              synchronized (this) {
                if (MovingProtocol.STATE_CONTENT != state) {
                  throw new
                    IllegalStateException("Illegal moving protocol state (" +
                                          format(state) + ")");
                }

                // The receiver has confirmed that it has correctly
                // received everything.
                state = MovingProtocol.STATE_FINAL;

                // Commit move.
                TupleStore.commitMove(tupleStoreClosure);

                // Cancel lease maintainer.
                leaseMaintainer.cancel();

                // Mark environments as stable again and clean up.
                synchronized (GLOBAL_LOCK) {
                  Iterator iter = envList.iterator();
                  while (iter.hasNext()) {
                    Environment tmp = (Environment)iter.next();

                    tmp.stable = true;

                    if (clone) {
                      // Re-activate animator.
                      if (restartSet.contains(tmp.id)) {
                        Animator anim = tmp.concurrency.anim;

                        if (null != anim) {
                          anim.setStatus(Animator.ACTIVE);
                        }
                      }
                    } else {
                      // Remove from global environment map.
                      environments.remove(tmp.id);
                    }
                  }

                  if (! clone) {
                    // Remove environment from parent and clear parent.
                    if (null != env.parent) {
                      env.parent.children.remove(env.name);
                      if (0 == env.parent.children.size()) {
                        env.parent.children = null;
                      }
                      env.parent = null;
                    }
                  }
                }
              }

              // Notify result handler.
              if (clone || (! set.containsKey(requestorId))) {
                resultHandler.handle(new
                  EnvironmentEvent(this, resultClosure, EnvironmentEvent.MOVED,
                                   env.id));
              }

              // Clear serialization set.
              set.clear();

              // Done.
              return true;
            }

          } catch (Exception x) {
            if (Constants.DEBUG_ENVIRONMENT) {
              SystemLog.LOG.logWarning(this, "Moving protocol error (" + dt +
                                       ")", x);
            }
            if (enterErrorState()) {
              root.request.handle(new
                RemoteEvent(this, e.closure, receiver, new
                  ExceptionalEvent(NullHandler.NULL, e.closure, x)));
              resultHandler.handle(new ExceptionalEvent(this,resultClosure,x));
            }
            return true;
          }

          // Fall out of if-then-else block.

        } else if (e instanceof ExceptionalEvent) {
          // Something went wrong on the receiver's side.
          ExceptionalEvent ee = (ExceptionalEvent)e;

          if (Constants.DEBUG_ENVIRONMENT) {
            SystemLog.LOG.logWarning(this, "Received remote exceptional event",
                                     ee.x);
          }
          if (enterErrorState()) {
            resultHandler.handle(new
              ExceptionalEvent(this, resultClosure, ee.x));
          }
          return true;
        }

        // The receiver sent an unrecognized message.
        if (Constants.DEBUG_ENVIRONMENT) {
          SystemLog.LOG.logWarning(this, "Received unrecognized message (" +
                                   e + ")");
        }
        if (enterErrorState()) {
          Exception x = new UnknownEventException(e.toString());

          // Notify the receiver.
          root.request.handle(new
            RemoteEvent(this, e.closure, receiver, new
              ExceptionalEvent(NullHandler.NULL, e.closure, x)));

          // Notify the result handler.
          resultHandler.handle(new ExceptionalEvent(this, resultClosure, x));
        }
        return true;

      } else if (e instanceof ExceptionalEvent) {
        // Something went wrong locally. It's a local wrap.
        ExceptionalEvent ee = (ExceptionalEvent)e;

        if (Constants.DEBUG_ENVIRONMENT) {
          SystemLog.LOG.logWarning(this, "Received exceptional event", ee.x);
        }
        if (enterErrorState()) {
          resultHandler.handle(new ExceptionalEvent(this, resultClosure, ee.x));
        }
        return true;
      }

      return false;
    }

    /**
     * Return the next tuple to move.
     *
     * @return  The next tuple to move or <code>null</code> if there
     *          are no more tuples to move.
     */
    Object nextTuple() {
      // Initialize the internal state if necessary.
      if (null == envIter) {
        // Note that since we are always moving at least one
        // environment, the environment iterator is guaranteed to
        // return at least one environment.
        envIter   = envList.iterator();
        currentId = ((Environment)envIter.next()).id;
        tupleIter = TupleStore.getTuples(currentId, tupleStoreClosure);
      }

      if (tupleIter.hasNext()) {
        return tupleIter.next();

      } else {
        // Produce the next tuple iterator that has tuples to offer.
        while (envIter.hasNext()) {
          currentId = ((Environment)envIter.next()).id;
          tupleIter = TupleStore.getTuples(currentId, tupleStoreClosure);

          if (tupleIter.hasNext()) {
            return tupleIter.next();
          }
        }

        // No more tuples.
        return null;
      }
    }

    /**
     * Enter the error state. If this sender was not in the error
     * state or the final state before invoking this method, this
     * method enters this sender into the error state and cleans up
     * all internal state.
     *
     * @return  <code>true</code> if this sender was not in the
     *          error state or final state before invoking this
     *          method.
     */
    boolean enterErrorState() {
      int oldState;

      synchronized (this) {
        if ((MovingProtocol.STATE_ERROR == state) ||
            (MovingProtocol.STATE_FINAL == state)) {
          // Already in error state or already completed.
          return false;
        }

        oldState = state;
        state    = MovingProtocol.STATE_ERROR;
      }

      // Abort the move for the tuple store.
      if (null != tupleStoreClosure) {
        TupleStore.abortMove(tupleStoreClosure);
        tupleStoreClosure = null;
      }

      // Clear serialization set.
      if (null != set) {
        set.clear();
        set = null;
      }

      // Cancel the lease maintainer.
      if (null != leaseMaintainer) {
        leaseMaintainer.cancel();
        leaseMaintainer = null;
      }

      synchronized (GLOBAL_LOCK) {
        if (MovingProtocol.STATE_INITIAL == oldState) {
          // Mark the environment as stable again.
          env.stable = true;

        } else {
          // Mark environments as stable again and reactivate
          // them. Reactivation means reactivating inactive animators
          // on a copy and restarting the environments on a move.
          Iterator iter = envList.iterator();
          while (iter.hasNext()) {
            Environment tmp = (Environment)iter.next();

            // Mark as stable.
            tmp.stable = true;

            // Reactivate.
            if (clone) {
              if (restartSet.contains(tmp.id)) {
                // Reactivate inactive animator.
                Animator anim = tmp.concurrency.anim;

                if (null != anim) {
                  anim.setStatus(Animator.ACTIVE);
                }
              }

            } else {
              if ((ACTIVE == tmp.status) &&
                  (null == tmp.concurrency.anim)) {
                // Restart terminated environment.
                tmp.concurrency.animate(tmp.singleThreaded, false);
                // Register animator with controller.
                if (tmp.concurrency.anim instanceof ThreadPool) {
                  controller.add(tmp.concurrency.anim);
                }
                try {
                  tmp.main.handleForced(new
                    EnvironmentEvent(tmp.request, null,
                                     EnvironmentEvent.ACTIVATED, tmp.id));

                  if (Constants.DEBUG_ENVIRONMENT) {
                    SystemLog.LOG.log(tmp, "Sent activated environment event");
                  }
                } catch (NotLinkedException x) {
                  SystemLog.LOG.logWarning(tmp,"main handler not linked when "+
                                           "activating", x);
                  tmp.concurrency.terminate();
                  tmp.status = INACTIVE;
                }
              }
            }
          }
        }
      }

      // Done.
      return true;
    }

  }


  // =======================================================================
  //                       The environment receiver
  // =======================================================================

  /**
   * The receiver for accepting moving environments.
   *
   * @see  MovingProtocol
   */
  static final class Receiver extends Mover {

    /** The new parent environment. */
    Environment     newParent;

    /** The ID of the root of the environment tree being received. */
    Guid            envId;

    /** The name of the root of the environment tree being received. */
    String          envName;

    /** The descriptors for the environment tree being received. */
    Descriptor[]    descriptors;

    /**
     * Create a new receiver.
     *
     * @param  sender   The sender.
     * @param  envId    The ID of the root environment.
     * @param  envName  The name of the root environment.
     * @param  clone    The flag for whether the environments are being
     *                  cloned.
     */
    Receiver(SymbolicHandler sender,Guid envId,String envName,boolean clone) {
      this.sender  = sender;
      this.envId   = envId;
      this.envName = envName;
      this.clone   = clone;
    }

    /** Handle the specified event. */
    protected boolean handle1(Event e) {
      DynamicTuple reply;

      if (e instanceof BindingResponse) {
        BindingResponse br = (BindingResponse)e;

        try {
          synchronized (this) {
            if (MovingProtocol.STATE_INITIAL != state) {
              LeaseMaintainer.cancel(br.lease);
              throw new
                IllegalStateException("Received binding response in illegal " +
                                      "moving protocol state (" +
                                      format(state) + ")");
            } else if (null != leaseMaintainer) {
              throw new
                IllegalStateException("Received duplicate binding response");
            }

            // Make sure the response handler stays exported.
            receiver        = (SymbolicHandler)br.resource;
            leaseMaintainer = new
              LeaseMaintainer(br.lease,br.duration,this,e.closure,root.timer);
          }
        } catch (Exception x) {
          if (Constants.DEBUG_ENVIRONMENT) {
            SystemLog.LOG.logWarning(this, "Moving protocol error", x);
          }
          if (enterErrorState()) {
            root.request.handle(new
              RemoteEvent(this, e.closure, sender, new
                ExceptionalEvent(NullHandler.NULL, e.closure, x)));
          }
          return true;
        }

        if (Constants.DEBUG_ENVIRONMENT) {
          SystemLog.LOG.log(this, "Exported receiver as " + receiver);
        }

        // Send allow-move message.
        reply = new DynamicTuple(receiver, e.closure);
        reply.set(MovingProtocol.MSG,    MovingProtocol.MSG_ALLOW);
        reply.set(MovingProtocol.ENV_ID, newParent.id            );

        operation.handle(new RemoteEvent(null, e.closure, sender, reply));

        if (Constants.DEBUG_ENVIRONMENT) {
          SystemLog.LOG.log(this, "Sent allow-move message (" + reply + ")");
        }

        return true;

      } else if (e instanceof RemoteEvent) {
        e = ((RemoteEvent)e).event;

        // Process the embedded event.
        if (e instanceof DynamicTuple) {
          DynamicTuple dt  = (DynamicTuple)e;
          Object       msg = dt.get(MovingProtocol.MSG);

          try {
            if (MovingProtocol.MSG_DESCRIPTORS.equals(msg)) {
              if (Constants.DEBUG_ENVIRONMENT) {
                SystemLog.LOG.log(this, "Received send-descriptors message (" +
                                  dt + ")");
              }
              
              // Get message payload.
              Descriptor[] dd = (Descriptor[])dt.get(MovingProtocol.DESCRIPTORS,
                                                     DESCRIPTOR_ARRAY, false);
              
              // Make sure all entries are valid descriptors.
              for (int i=0; i<dd.length; i++) {
                if (null == dd[i]) {
                  throw new
                    IllegalArgumentException("Null entry in descriptor array ("+
                                             dd + ")");
                } else {
                  dd[i].validate();
                }
              }
              
              // Process message.
              synchronized (this) {
                if (MovingProtocol.STATE_INITIAL != state) {
                  throw new IllegalStateException("Illegal moving protocol " +
                                                  "state ("+format(state)+")");
                }
                
                state = MovingProtocol.STATE_DESCRIPTOR;
                set   = new HashMap();
                env   = createLocalTree(newParent, dd, set);
                synchronized (GLOBAL_LOCK) {
                  // Check that environment IDs are not in use.
                  for (int i=0; i<dd.length; i++) {
                    if (environments.containsKey(dd[i].ident)) {
                      throw new
                        IllegalArgumentException("Duplicate environment ID (" +
                                                 dd[i].ident + ")");
                    }
                  }

                  // Remember descriptors. Note that we only assign
                  // descriptors after we have checked for duplicate
                  // environment IDs, since enterErrorState() removes
                  // the environments from the global environment set.
                  descriptors = dd;

                  // Mark environment IDs as used.
                  for (int i=0; i<dd.length; i++) {
                    environments.put(dd[i].ident, DUMMY);
                  }
                }
                
                // Tell tuple store about the move.
                tupleStoreClosure = TupleStore.startAccept(descriptors);
              }

              // Send confirm-descriptors message.
              reply = new DynamicTuple(receiver, e.closure);
              reply.set(MovingProtocol.MSG, MovingProtocol.MSG_CONFIRM);

              operation.handle(new RemoteEvent(this, e.closure, sender, reply));

              if (Constants.DEBUG_ENVIRONMENT) {
                SystemLog.LOG.log(this, "Sent confirm-descriptors message (" +
                                  reply + ")");
              }

              return true;

            } else if (MovingProtocol.MSG_SEND_TUPLE.equals(msg)) {
              if (Constants.DEBUG_ENVIRONMENT) {
                SystemLog.LOG.log(this, "Received send-tuple message ("+dt+")");
              }
              
              // Get message payload.
              Guid       tenvId = (Guid)dt.get(MovingProtocol.ENV_ID,
                                               Guid.class, false);
              BinaryData tuple  = (BinaryData)dt.get(MovingProtocol.TUPLE,
                                                     BinaryData.class, false);

              // Make sure environment ID is part of serialization set.
              if (! set.containsKey(tenvId)) {
                throw new
                  IllegalArgumentException("Received tuple for environment " +
                                           "that is not part of moved tree (" +
                                           tenvId + ")");
              }

              // Validate the tuple.
              tuple.validate();

              // Process message.
              synchronized (this) {
                if (MovingProtocol.STATE_DESCRIPTOR == state) {
                  state = MovingProtocol.STATE_CONTENT;
                } else if (MovingProtocol.STATE_CONTENT != state) {
                  throw new
                    IllegalStateException("Received tuple in illegal moving " +
                                          "protocol state (" + format(state) +
                                          ")");
                }

                TupleStore.writeTuple(tenvId, tuple, tupleStoreClosure);
              }

              reply = new DynamicTuple(receiver, e.closure);
              reply.set(MovingProtocol.MSG, MovingProtocol.MSG_CONFIRM_TUPLE);
              reply.set(MovingProtocol.TUPLE_ID, tuple.id                   );

              operation.handle(new RemoteEvent(null, e.closure, sender, reply));

              if (Constants.DEBUG_ENVIRONMENT) {
                SystemLog.LOG.log(this, "Sent confirm-tuple message (" + reply +
                                  ")");
              }

              return true;

            } else if (MovingProtocol.MSG_CHECK_POINT.equals(msg)) {
              if (Constants.DEBUG_ENVIRONMENT) {
                SystemLog.LOG.log(this, "Received send-check-point message (" +
                                  dt + ")");
              }

              // Get message payload.
              CheckPoint cp = (CheckPoint)dt.get(MovingProtocol.CHECK_POINT,
                                                 CheckPoint.class, false);

              // Validate checkpoint.
              cp.validate();

              // Process message.
              synchronized (this) {
                if (MovingProtocol.STATE_DESCRIPTOR == state) {
                  state = MovingProtocol.STATE_CONTENT;
                } else if (MovingProtocol.STATE_CONTENT != state) {
                  throw new
                    IllegalStateException("Received checkpoint in illegal " +
                                          "moving protocol state (" +
                                          format(state) + ")");
                }

                // Copy over serialization set.
                Map actualSet = (Map)serializationSet.get();
                actualSet.clear();
                actualSet.putAll(set);

                // Restore checkpoint.
                Environment[] newEnvironments;
                try {
                  newEnvironments = env.restore(cp, actualSet,
                                                (clone ?
                                                 EnvironmentEvent.CLONED :
                                                 EnvironmentEvent.MOVED), null);
                } finally {
                  actualSet.clear();
                }

                // Prepare to complete the move.
                synchronized (GLOBAL_LOCK) {
                  ensureAlive();
                  ensureAlive(newParent);
                  
                  // Commit accept.
                  TupleStore.commitAccept(tupleStoreClosure);
                  
                  // Mark parent as stable again and link in actual
                  // environment tree.
                  newParent.stable = true;
                  newParent.children.put(envName, env);
                  
                  // Register environments with global environment
                  // set. Note that this overrides previously
                  // establishedd dummy environments.
                  for (int i=0; i<newEnvironments.length; i++) {
                    Environment tmp = newEnvironments[i];
                    
                    environments.put(tmp.id, tmp);
                  }
                  
                  // Re-activate inactive animators.
                  for (int i=0; i<newEnvironments.length; i++) {
                    Environment tmp = newEnvironments[i];
                    
                    if (ACTIVE == tmp.status) {
                      // Register animator with controller.
                      Animator anim = tmp.concurrency.anim;
                      
                      if (anim instanceof ThreadPool) {
                        controller.add(anim);
                      }
                      if (null != anim) {
                        anim.setStatus(Animator.ACTIVE);
                      }
                    }
                  }
                }

                // Fix the state.
                state = MovingProtocol.STATE_FINAL;
              }

              // Send complete-move message.
              reply = new DynamicTuple(receiver, e.closure);
              reply.set(MovingProtocol.MSG, MovingProtocol.MSG_COMPLETE);

              root.request.handle(new
                RemoteEvent(this, e.closure, sender, reply));

              if (Constants.DEBUG_ENVIRONMENT) {
                SystemLog.LOG.log(this, "Sent complete-move message (" +
                                  reply + ")");
              }

              // Cancel lease maintainer.
              leaseMaintainer.cancel();

              // Done.
              return true;
            }

          } catch (Exception x) {
            if (Constants.DEBUG_ENVIRONMENT) {
              SystemLog.LOG.logWarning(this, "Moving protocol error (" + dt +
                                       ")", x);
            }
            if (enterErrorState()) {
              root.request.handle(new
                RemoteEvent(this, e.closure, sender, new
                  ExceptionalEvent(NullHandler.NULL, e.closure, x)));
            }
            return true;
          }

          // Fall out of if-then-else block.

        } else if (e instanceof ExceptionalEvent) {
          // Something went wrong on the sender's side.
          if (Constants.DEBUG_ENVIRONMENT) {
            SystemLog.LOG.logWarning(this, "Received remote exceptional event",
                                     ((ExceptionalEvent)e).x);
          }
          enterErrorState();
          return true;
        }

        // The sender sent an unrecognized message.
        if (Constants.DEBUG_ENVIRONMENT) {
          SystemLog.LOG.logWarning(this, "Received unrecognized message (" +
                                   e + ")");
        }
        if (enterErrorState()) {
          root.request.handle(new
            RemoteEvent(this, e.closure, sender, new
              ExceptionalEvent(NullHandler.NULL, e.closure, new
                UnknownEventException(e.toString()))));
        }
        return true;

      } else if (e instanceof ExceptionalEvent) {
        // Something went wrong locally. It's a local wrap.
        if (Constants.DEBUG_ENVIRONMENT) {
          SystemLog.LOG.logWarning(this, "Received exceptional event",
                                   ((ExceptionalEvent)e).x);
        }
        enterErrorState();
        return true;
      }

      return false;
    }

    /**
     * Enter the error state. If this receiver was not in the error
     * state or the final state before invoking this method, this
     * method enters this receiver into the error state and cleans up
     * all internal state.
     *
     * @return  <code>true</code> if this receiver was not in the error
     *          state or final state before invoking this method.
     */
    boolean enterErrorState() {
      synchronized (this) {
        if ((MovingProtocol.STATE_ERROR == state) ||
            (MovingProtocol.STATE_FINAL == state)) {
          // Already in error state or already completed.
          return false;
        }

        state = MovingProtocol.STATE_ERROR;
      }

      synchronized (GLOBAL_LOCK) {
        // Mark new parent environment as stable again and remove the
        // dummy environment from its children.
        newParent.stable = true;
        newParent.children.remove(envName);
        if (0 == newParent.children.size()) {
          newParent.children = null;
        }

        // Unmark environment IDs as used.
        if (null != descriptors) {
          for (int i=0; i<descriptors.length; i++) {
            environments.remove(descriptors[i].ident);
          }
          descriptors = null;
        }
      }

      // Cancel the lease maintainer.
      if (null != leaseMaintainer) {
        leaseMaintainer.cancel();
        leaseMaintainer = null;
      }

      // Abort the move for the tuple store.
      if (null != tupleStoreClosure) {
        TupleStore.abortAccept(tupleStoreClosure);
        tupleStoreClosure = null;
      }

      // Done.
      return true;
    }

  }


  // =======================================================================
  //                         Public constants
  // =======================================================================

  /**
   * The status code for an inactive environment. An inactive
   * environment can store tuples and is ready for linking, but does
   * not execute any computations.
   */
  public static final int    INACTIVE       = 1;

  /**
   * The status code for an active environment. An active environment
   * is actively executing code. Its main event handler must be linked
   * against some component.
   */
  public static final int    ACTIVE         = 2;

  /**
   * The status code for a terminating environment. A terminating
   * environment is transitioning from the active back to the inactive
   * state. In the terminating state, the environment is giving its
   * main imported event handler time to react to the pending
   * termination.
   */
  public static final int    TERMINATING    = 3;

  /**
   * The status code for an environment that is being destroyed.  Such
   * an environment is transitioning from the inactive or the active
   * to the destroyed state. If the environment was in the active
   * state before entering the destroying state, it is giving its main
   * imported event handler time to react to the pending destruction.
   */
  public static final int    DESTROYING     = 4;

  /**
   * The status code for a destroyed environment. A destroyed
   * environment has terminated all activity, deleted all stored
   * tuples, and is ready for reclamation by the garbage collector.
   */
  public static final int    DESTROYED      = 5;


  // =======================================================================
  //                            Descriptors
  // =======================================================================
  
  /** The component descriptor for this environment. */
  private static final ComponentDescriptor SELF =
    new ComponentDescriptor("one.world.core.Environment", "An environment",
                            true);

  /** The imported event handler descriptor for the main event handler. */
  private static final ImportedDescriptor  MAIN =
    new ImportedDescriptor("main",
                           "Event handler for environment events",
                           new Class[] { EnvironmentEvent.class },
                           null,
                           false,
                           true);

  /** The exported event handler descriptor for the request event handler. */
  private static final ExportedDescriptor REQUEST =
    new ExportedDescriptor("request",
                           "Event handler for processing requests",
                           null,
                           new Class[] { TupleException.class,
                                         IllegalStateException.class,
                                         IllegalArgumentException.class,
                                         SecurityException.class },
                           false);

  /** The imported event handler descriptor for the monitor event handler. */
  private static final ImportedDescriptor MONITOR =
    new ImportedDescriptor("monitor",
                           "Event handler for monitoring descendant " +
                           "environments",
                           null,
                           null,
                           false,
                           true);


  // =======================================================================
  //                    Global state and initialization
  // =======================================================================

  /** The status code for an initialized system. */
  static final int           INITIALIZED            = 0;

  /** The status code for a running system. */
  static final int           RUNNING                = 1;

  /** The status code for a system shutting down. */
  static final int           SHUTTING_DOWN          = 2;

  /** The status code for a shut-down system. */
  static final int           SHUTDOWN               = 3;

  /** The signature of the initializer method. */
  private static final Class[]       SIGNATURE      =
    new Class[] { Environment.class, Object.class };

  /** The global lock. Package private to avoid synthetic accessors. */
  static final Object                GLOBAL_LOCK;

  /**
   * The dummy environment. The dummy environment is not a real
   * environment and thus not part of the global environment
   * mapping. Rather, it serves as a special marker for operations on
   * environments that modify the environment hierarchy. This field is
   * package private to avoid synthetic accessors.
   *
   * @see   #environments
   */
  static final Environment           DUMMY;

  /** 
   * The class for an array of environment descriptors. This field is
   * package private to avoid synthetic accessors.
   */
  static final Class                 DESCRIPTOR_ARRAY;

  /** The flag for a local environment migration. */
  static final int                   MOVED_LOCALLY = Integer.MIN_VALUE;

  /**
   * The root environment. This variable is not final only because it
   * is initialized during start-up. This field is package private to
   * avoid synthetic accessors.
   */
  static Environment                 root;

  /** The global status. */
  private static       int           globalStatus;

  /**
   * The set of environments on this node. This set is implemented as
   * a mapping from environment IDs to the actual environments.  All
   * environments in this set must be reachable from the root
   * environment by traversing the environment hierarchy. Furthermore,
   * all environments in this set must not be in the destroying or the
   * destroyed state. This field is package private to avoid synthetic
   * accessors.
   */
  static final Map                   environments;

  /**
   * The animator controller for the root environment. This field is
   * not final only because it is initialized during start-up.
   */
  private static Animator.Controller rootController;

  /**
   * The animator controller for all other environments. This field is
   * not final only because it is initialized during start-up.  This
   * field is package private to avoid synthetic accessors.
   */
  static Animator.Controller         controller;

  /**
   * The thread local variable referencing the set of environments
   * currently being serialized or deserialized. During serialization,
   * the mapping is implemented as a mapping from environment IDs to
   * environment IDs, where the target ID is the same ID as the key if
   * the environment is check-pointer or a fresh ID if the environment
   * is cloned. During deserialization, the mapping is implemented as
   * a mapping from environment IDs to the actual environments. This
   * field is package private to avoid synthetic accessors.
   */
  static final ThreadLocal           serializationSet;

  /* Initialize global state. */
  static {
    // Set up global monitor and environment map.
    GLOBAL_LOCK        = new Object();
    DUMMY              = new Environment();
    try {
      DESCRIPTOR_ARRAY =
        Class.forName("[Lone.world.core.Environment$Descriptor;");
    } catch (ClassNotFoundException x) {
      throw new Bug("Unable to load class for one.world.core.Environment" +
                    ".Descriptor");
    }
    environments       = new HashMap();
    serializationSet   = new ThreadLocal() {
        protected Object initialValue() {
          return new HashMap();
        }
      };
  }


  // =======================================================================
  //                          Instance fields
  // =======================================================================

  // ===================== The environment hierarchy =======================

  /**
   * The ID for this environment. This field must not be
   * <code>null</code> unless this environment is the unique dummy
   * environment.
   */
  final            Guid               id;

  /**
   * The name for this environment. This field is package private to
   * avoid synthetic accessors.
   *
   * @serial  Must not be <code>null</code>.
   */
  String                              name;

  /**
   * The parent environment for this environment. This field must be
   * set to <code>null</code> during serialization if the parent
   * environment is not being serialized as well. This field is
   * package private to avoid synthetic accessors.
   */
  volatile         Environment        parent;

  /**
   * The map of children for this environment. The keys are the names
   * of the child environments and the values are the actual
   * environments. This field is package private to avoid synthetic
   * accessors.
   */
  Map                                 children;

  // ============================= Status ==================================

  /** 
   * The status of this environment. This field is package private to
   * avoid synthetic accessors.
   */
  volatile         int                status;

  /**
   * The stable flag for this environment. An environment is unstable
   * if the environment's contents or its position in the environment
   * hierarchy are currently modified. This field is package private to
   * avoid synthetic accessors.
   */
  boolean                             stable;

  // ======================= Component support =============================

  /**
   * The main event handler for this environment (an imported event
   * handler). This field is only non-final because it is reassigned
   * during check-point restoration. This field is package private to
   * avoid synthetic accessors.
   */
  Component.Importer                  main;

  /**
   * The handler for handling environment requests (an exported event
   * handler). This field is only non-final because it is reassigned
   * during check-point restoration. This field is package private to
   * avoid synthetic accessors.
   */
  EventHandler                        request;

  /**
   * The handler for monitoring descendant environments (an imported
   * event handler). This field is only non-final because it is
   * reassigned during check-point restoration. This field is package
   * private to avoid synthetic accessors.
   */
  Component.Importer                  monitor;

  /**
   * The timer for this environment. The timer is only allocated on
   * demand. This field is package private to avoid synthetic
   * accessors.
   */
  Timer                               timer;

  /**
   * The flag for whether this environment needs to be
   * single-threaded. An environment needs to be single-threaded if
   * any of its registered components is not thread-safe. This field
   * is package private to avoid synthetic accessors.
   */
  boolean                             singleThreaded;

  // ============================ Domains ==================================

  /**
   * The concurrency domain for this environment. This field is
   * reassigned during check-point restoration as well as during the
   * unload operation. This field is package private to avoid
   * synthetic accessors.
   */
  volatile ConcurrencyDomain          concurrency;

  /**
   * The protection domain for this environment. This field is
   * reassigned during the unload operation. This field is package
   * private to avoid synthetic accessors.
   */
  volatile ProtectionDomain           protection;


  // =======================================================================
  //                            Constructor
  // =======================================================================

  /** Create a new dummy environment. */
  private Environment() {
    super();

    // All instance fields of the dummy environment are null/false and
    // its status is destroyed. The id and concurrency fields are set
    // explicitly because they are final.
    id             = null;
    status         = DESTROYED;
    concurrency    = null;
  }

  /**
   * Create a new root environment.
   *
   * @param   id    The ID for the root environment.
   * @param   prot  The protection domain for the root environment.
   */
  private Environment(Guid id, ProtectionDomain prot) {
    super();

    // Patch up Component.this.env
    env            = this;

    // Initialize fields.
    this.id        = id;
    name           = "/";
    status         = INACTIVE;
    stable         = true;
    main           = declareImported(MAIN);
    request        = declareExported(REQUEST, new RootRequestHandler());
    monitor        = declareImported(MONITOR);
    singleThreaded = false;
    concurrency    = new ConcurrencyDomain(id);
    protection     = prot;
  }

  /**
   * Create a new environment.
   *
   * @param   id      The ID for the new environment, which must not be
   *                  <code>null</code> and must be unique amongst all
   *                  environments in the local environment hierarchy.
   * @param   name    The name for the new environment, which must be
   *                  "<code>/</code>" for the root environment and
   *                  must not be equal to any other child environment
   *                  for nested environments.
   * @param   parent  The parent environment for the newly created
   *                  environment.
   * @param   prot    The protection domain for the new environment.
   */
  private Environment(Guid id, String name, Environment parent,
                      ProtectionDomain prot) {
    super();

    // Patch up Component.this.env
    env           = root;

    // Initialize fields.
    this.id        = id;
    this.name      = name;
    this.parent    = parent;
    children       = null; // Dynamically created by create().
    status         = INACTIVE;
    stable         = true;
    main           = declareImported(MAIN);
    request        = declareExported(REQUEST, new RequestHandler());
    monitor        = declareImported(MONITOR);
    singleThreaded = false;
    concurrency    = new ConcurrencyDomain(id);
    protection     = prot;
  }


  // =======================================================================
  //                          Basic Serialization
  // =======================================================================

  /**
   * Replace this environment with an environment reference during
   * serialization.
   *
   * @throws      SecurityException
   *                 Signals that the caller does not have permission
   *                 to manage environments.
   * @throws      IllegalStateException
   *                 Signals that this environment is being serialized
   *                 outside a check-point.
   */
  private Object writeReplace() throws ObjectStreamException {
    ensurePermission();

    // Determine ID.
    Guid id2 = (Guid)((Map)serializationSet.get()).get(id);

    // Sanity check.
    if (null == id2) {
      throw new IllegalStateException("Attempting to serialize environment " +
                                      "outside check-point (" + this + ")");
    }

    return new Reference(id2);
  }


  // =======================================================================
  //                     Environment as a component
  // =======================================================================

  /** Get the component descriptor for this environment. */
  public ComponentDescriptor getDescriptor() {
    return (ComponentDescriptor)SELF.clone();
  }

  /**
   * Get the main imported event handler for this environment.
   *
   * @return     The main imported event handler.
   * @throws  SecurityException
   *             Signals that the caller does not have permission to
   *             manage environments.
   */
  public Component.Importer getMain() {
    ensurePermission();

    return main;
  }

  /**
   * Get the request exported event handler for this environment.
   *
   * @return     The request exported event handler.
   * @throws  SecurityException
   *             Signals that the caller does not have permission to
   *             manage environments.
   */
  public EventHandler getRequest() {
    ensurePermission();

    return request;
  }

  /**
   * Get the monitor imported event handler for this environment.
   *
   * @return     The monitor imported event handler.
   * @throws  SecurityException
   *             Signals that the caller does not have permission to
   *             manage environments.
   */
  public Component.Importer getMonitor() {
    ensurePermission();

    return monitor;
  }

  /**
   * Get the timer for this environment.
   *
   * @see     Component#getTimer()
   *
   * @return     The timer for this environment.
   * @throws  SecurityException
   *             Signals that the caller does not have permission to
   *             manage environments.
   */
  public Timer getTimer1() {
    ensurePermission();

    synchronized (this) {
      if (null == timer) {
        timer = new Timer(this);
      }

      return timer;
    }
  }


  // =======================================================================
  //                       Support for Component
  // =======================================================================

  /**
   * Register the specified component with this environment. Used by
   * <code>Component</code> to register a newly created or
   * deserialized component.
   *
   * @param   component  The component to register.
   * @throws  NullPointerException
   *                     Signals that <code>component</code> is
   *                     <code>null</code>.
   * @throws  IllegalArgumentException
   *                     Signals that the specified component is not
   *                     thread-safe while this environment represents
   *                     a multi-threaded concurrency domain, or that
   *                     the protection domain for the specified
   *                     component is not this environment's
   *                     protection domain.
   * @throws  IllegalStateException
   *                     Signals that this environment is either being
   *                     destroyed or has been destroyed, or that the
   *                     system is not running.
   */
  void register(Component component) {
    // Basic consistency checks.
    if (null == component) {
      throw new NullPointerException("Null component");
    }
    
    boolean concurrencySafe = component.getDescriptor().concurrencySafe;

    synchronized (GLOBAL_LOCK) {
      ensureAlive();
      ensureAlive(this);

      // Is component compatible with this environment's concurrency
      // domain?
      if (null == concurrency.anim) {
        // Just record the least common denominator of all components.
        if (! concurrencySafe) {
          singleThreaded = true;
        }

      } else {
        // Verify that the component is compatible with the animator.
        if (concurrency.anim.isConcurrent() &&
            (! concurrencySafe)) {
          throw new IllegalArgumentException("Component " + component +
                                             " for concurrent environment " +
                                             id + " not concurrency-safe");
        }
      }

      // Is the component compatible with this environment's
      // protection domain?
      ClassLoader loader = component.getClass().getClassLoader();

      if (loader instanceof ProtectionDomain.Loader) {
        if (! protection.id.equals(((ProtectionDomain.Loader)loader).getId())) {
          throw new IllegalArgumentException("Component " + component +
                                             " not in same protection domain" +
                                             " as environment " + id);
        }
      } else {
        // Make sure protection domain has a class loader.
        protection.initializeLoader();
      }
    }
  }

  /**
   * Get the specified wrapper. This method returns the wrapper for
   * event handlers exported by a component in the specified
   * environment when invoking them through an event handler imported
   * by a component in this environment. If the specified environment
   * is in the same concurrency domain as this environment, no wrapper
   * is necessary and this method returns <code>null</code>. However,
   * if <code>forced</code> is <code>true</code> all exported event
   * handlers are wrapped.
   *
   * @param   other   The environment for the component exporting
   *                  the event handler to be wrapped by the returned
   *                  wrapper.
   * @param   forced  The flag to indicate whether all event handlers
   *                  need to be wrapped, even if their component is
   *                  in the same concurrency domain as this
   *                  environment.
   * @return          The wrapper for sending events to a component
   *                  in the specified environment or <code>null</code>
   *                  if no wrapper is required.
   * @throws  IllegalStateException
   *                  Signals that either environment has been destroyed
   *                  or that the system is not running.
   */
  Wrapper getWrapper(Environment other, boolean forced) {
    ConcurrencyDomain targetC;
    ProtectionDomain  targetP;
    ConcurrencyDomain sourceC;
    ProtectionDomain  sourceP;

    // Get the concurrency domains.
    synchronized (GLOBAL_LOCK) {
      // Wrappers can be accessed even if the system is shutting down
      // or the environments are being terminated, so that timers can
      // be scheduled during tear-down.
      if (INITIALIZED == globalStatus) {
        throw new IllegalStateException("System not yet running");
      } else if (SHUTDOWN == globalStatus) {
        throw new IllegalStateException("System shut down");
      } else if (DESTROYED == this.status) {
        throw new IllegalStateException("Environment has been destroyed (" +
                                        this.id + ")");
      } else if (DESTROYED == other.status) {
        throw new IllegalStateException("Environment has been destroyed (" +
                                        other.id + ")");
      }

      // Copy out the needed protection and concurrency domain
      // references.
      targetC = concurrency;
      targetP = protection;
      sourceC = other.concurrency;
      sourceP = other.protection;
    }

    // Return the corresponding wrapper.
    if ((! targetC.equals(sourceC)) || forced) {
      return new Domain.Call(sourceC, sourceP, targetC, targetP);
    } else {
      return null;
    }
  }


  // =======================================================================
  //                     Public environment information
  // =======================================================================

  /**
   * Get the ID for this environment.
   *
   * @return  The ID for this environment.
   */
  public Guid getId() {
    return id;
  }

  /**
   * Get the name for this environment.
   *
   * @return  The name for this environment.
   */
  public String getName() {
    return name;
  }

  /**
   * Get the concurrency domain for this environment.
   *
   * <p>If an application retains a reference to the object returned
   * by this method, it must refresh that reference whenever a code
   * loading operation has failed, the environment has been restored
   * from a saved check-point or the environment's code has been
   * unloaded.</p>
   *
   * @see     Environment#create(Guid,Guid,String,boolean,String,Object)
   * @see     Environment#load
   * @see     Environment#restore
   * @see     Environment#unload
   *
   * @return  The concurrency domain for this environment.
   */
  public ConcurrencyDomain getConcurrencyDomain() {
    return concurrency;
  }

  /**
   * Get the protection domain for this environment.
   *
   * <p>If an application retains a reference to the object returned
   * by this method, it must refresh that reference whenever the
   * environment's code has been unloaded.</p>
   *
   * @see     Environment#unload
   *
   * @return  The protection domain for this environment.
   */
  public ProtectionDomain getProtectionDomain() {
    return protection;
  }


  // =======================================================================
  //                  Walking the environment hierarchy
  // =======================================================================

  /**
   * Get the parent environment for this environment.
   *
   * @return     The parent environment for this environment or
   *             <code>null</code> if this environment is the
   *             root environment.
   * @throws  IllegalStateException
   *             Signals that this environment is either being
   *             destroyed or has been destroyed, or that the
   *             system is not running.
   * @throws  SecurityException
   *             Signals that the caller does not have permission to
   *             manage environments.
   */
  public Environment getParent() {
    ensurePermission();

    synchronized (GLOBAL_LOCK) {
      ensureAlive();
      ensureAlive(this);

      return parent;
    }
  }

  /**
   * Get the ID of the parent environment for this environment.
   *
   * @return     The ID of the parent environment for this environment
   *             or <code>null</code> if this environment is the root
   *             environment.
   * @throws  IllegalStateException
   *             Signals that this environment is either being
   *             destroyed or has been destroyed, or that the
   *             system is not running.
   */
  public Guid getParentId() {
    if (this == root) {
      return null;
    } else {
      synchronized (GLOBAL_LOCK) {
        ensureAlive();
        ensureAlive(this);

        return parent.id;
      }
    }
  }

  /**
   * Get the name of the parent environment for this environment.
   *
   * @return     The name of the parent environment for this environment
   *             or <code>null</code> if this environment is the root
   *             environment.
   * @throws  IllegalStateException
   *             Signals that this environment is either being
   *             destroyed or has been destroyed, or that the
   *             system is not running.
   */
  public String getParentName() {
    if (this == root) {
      return null;
    } else {
      synchronized (GLOBAL_LOCK) {
        ensureAlive();
        ensureAlive(this);

        return parent.name;
      }
    }
  }

  /**
   * Get the child environment witht the specified name.
   *
   * @param   name  The name of the child environment to look up.
   * @return        The child environment with the specified name or
   *                <code>null</code> if this environment has no
   *                such child.
   * @throws  IllegalStateException
   *                Signals that this environment is either being
   *                destroyed or has been destroyed, or that the
   *                system is not running.
   */
  public Environment getChild(String name) {
    synchronized (GLOBAL_LOCK) {
      ensureAlive();
      ensureAlive(this);

      if (null == children) {
        return null;
      } else {
        Environment child = (Environment)children.get(name);

        return ((DUMMY == child)? null : child);
      }
    }
  }

  /**
   * Get the names of all child environments.
   *
   * @return     A list with the names of all child environments.
   * @throws  IllegalStateException
   *             Signals that this environment is either being
   *             destroyed or has been destroyed, or that the
   *             system is not running.
   */
  public List getChildren() {
    synchronized (GLOBAL_LOCK) {
      ensureAlive();
      ensureAlive(this);

      if (null == children) {
        return Collections.EMPTY_LIST;

      } else {
        ArrayList list = new ArrayList(children.size());
        Iterator  iter = children.values().iterator();

        while (iter.hasNext()) {
          Environment child = (Environment)iter.next();

          if (DUMMY != child) {
            list.add(child.name);
          }
        }
      
        return list;
      }
    }
  }


  // =======================================================================
  //                        String representation
  // =======================================================================

  /** Get a string representation for this environment. */
  public String toString() {
    return "#[Environment " + id + "]";
  }


  // =======================================================================
  //                       Environment factories
  // =======================================================================

  /**
   * Get the root environment. The root environment represents the
   * root of the environment hierarchy running on a node. It provides
   * the environment for trusted services and cannot accommodate any
   * other components.
   *
   * @return     The root environment.
   * @throws  SecurityException
   *             Signals that the caller does not have permission to
   *             manage environments.
   */
  public static Environment getRoot() {
    ensurePermission();
    return root;
  }

  /**
   * Create a new child environment. The newly created child environment
   * is a child of the specified parent environment and has the
   * specified name.
   *
   * @param   requestorId  The ID of the environment requesting this
   *                       operation.
   * @param   parentId     The ID of the parent environment.
   * @param   name         The name of the new child environment.
   * @param   inherit      The flag for whether to inherit the parent
   *                       environment's protection domain.
   * @return               The new environment.
   * @throws  NullPointerException
   *                       Signals that <code>parentId</code> or
   *                       <code>name</code> is <code>null</code>.
   * @throws  IllegalArgumentException
   *                       Signals that no environment with the
   *                       specified ID exists, that <code>name</code>
   *                       is an illegal name for an environment, or that
   *                       the specified parent environment already has
   *                       a child with the specified name.
   * @throws  IOException  Signals an exceptional condition when
   *                       writing the persistent environment state
   *                       for the new environment.
   * @throws  IllegalStateException
   *                       Signals that the system is not running.
   * @throws  SecurityException
   *                       Signals that the caller does not have
   *                       permission to manage environments, or that the
   *                       specified parent environment is not a descendant
   *                       of the specified requesting environment.
   */
  public static Environment create(Guid requestorId, Guid parentId,
                                   String name, boolean inherit)
    throws IOException {

    try {
      return create(requestorId, parentId, name, inherit, null, null);
    } catch (ClassNotFoundException x) {
      throw new Bug("Unexpected exception (" + x + ")");
    } catch (NoSuchMethodException x) {
      throw new Bug("Unexpected exception (" + x + ")");
    } catch (InvocationTargetException x) {
      throw new Bug("Unexpected exception (" + x + ")");
    }
  }

  /**
   * Create a new child environment. The newly created child
   * environment is a child of the specified parent environment and
   * has the specified name. 
   *
   * <p>If <code>init</code> is not <code>null</code>, it is treated
   * as the name of the class providing the initializer for the new
   * environment. The initializer has the signature <code>public
   * static void init(Environment, Object)</code>. It is invoked after
   * the new child environment has been created and is passed a
   * reference to the child environment as well as the specified
   * closure.</p>
   *
   * <p>Note that the initializer may throw any
   * <code>Throwable</code>. If the initializer terminates
   * exceptionally, all event handlers for this environment are
   * automatically unlinked, thus ensuring that objects created by the
   * initializer and linking operations performed by the initializer
   * do not persist. However, error recovery does not recreate the
   * class loader for the environment, which requires an explicit
   * {@link #unload unload} operation.</p>
   *
   * @param   requestorId  The ID of the environment requesting this
   *                       operation.
   * @param   parentId     The ID of the parent environment.
   * @param   name         The name of the new child environment.
   * @param   inherit      The flag for whether to inherit the parent
   *                       environment's protection domain.
   * @param   init         The name of the class providing the
   *                       initializer for the new child environment,
   *                       or <code>null</code> if no initialization
   *                       is to be performed.
   * @param   closure      The closure for the initializer.
   * @return               The new environment.
   * @throws  NullPointerException
   *                       Signals that <code>parentId</code> or
   *                       <code>name</code> is <code>null</code>.
   * @throws  IllegalArgumentException
   *                       Signals that no environment with the
   *                       specified ID exists, that <code>name</code>
   *                       is an illegal name for an environment, or that
   *                       the specified parent environment already has
   *                       a child with the specified name.
   * @throws  IOException  Signals an exceptional condition when
   *                       writing the persistent environment state
   *                       for the new environment.
   * @throws  ClassNotFoundException
   *                       Signals that the class with name
   *                       <code>init</code> cannot be found.
   * @throws  NoSuchMethodException
   *                       Signals that the class with name
   *                       <code>init</code> does not have an
   *                       appropriate <code>init()</code> method.
   * @throws  InvocationTargetException
   *                       Signals an exceptional condition while
   *                       executing the initializer.
   * @throws  IllegalStateException
   *                       Signals that the system is not running or
   *                       that the specified parent environment is
   *                       currently being modified.
   * @throws  SecurityException
   *                       Signals that the caller does not have
   *                       permission to manage environments, or that the
   *                       specified parent environment is not a descendant
   *                       of the specified requesting environment.
   */
  public static Environment create(Guid requestorId, Guid parentId,
                                   String name, boolean inherit,
                                   String init, Object closure)
    throws IOException, ClassNotFoundException,
           NoSuchMethodException, InvocationTargetException {

    ensurePermission();
    ensureName(name);

    if (null == parentId) {
      throw new NullPointerException("Null ID for parent environment");
    }

    Environment      parent;
    Environment      child;
    Guid             id;
    Descriptor       desc;

    synchronized (GLOBAL_LOCK) {
      ensureAlive();
      parent = lookup0(parentId);
      ensureStable(parent);
      if (null != requestorId) {
        ensureLineage(parent, lookup0(requestorId));
      }

      // Make sure no child with the specified name exists.
      if ((null != parent.children) && parent.children.containsKey(name)) {
        throw new IllegalArgumentException("Environment with same name " +
                                           "already exists (" + name + ")");
      }

      // Make sure we are not trying to inherit the protection domain
      // of the root environment.
      if (inherit && (parent == root)) {
        throw new IllegalArgumentException("Unable to inherit the protection" +
                                           " domain of the root environment");
      }

      // Create new environment ID.
      do {
        id = new Guid();
      } while (environments.containsKey(id));

      // Mark ID as used.
      environments.put(id, DUMMY);

      // Mark name as used in parent.
      if (null == parent.children) {
        parent.children = new HashMap();
      }
      parent.children.put(name, DUMMY);

      // Mark parent as unstable.
      parent.stable = false;
    }
    
    ProtectionDomain protection = (inherit? parent.protection :
                                   new ProtectionDomain(id));
    Method           method     = null;
    boolean          success    = false;

    try {
      // Process initializer.
      if (null != init) {
        protection.initializeLoader();
        method  = getInitializer(init, protection.loader);
      }

      // Create environment.
      child = new Environment(id, name, parent, protection);

      // Tell tuple store.
      TupleStore.create(new Descriptor(child));
      success = true;
    } finally {
      if (! success) {
        synchronized (GLOBAL_LOCK) {
          // Mark parent as stable again.
          parent.stable = true;

          // Unmark name as used in parent.
          parent.children.remove(name);
          if (0 == parent.children.size()) {
            parent.children = null;
          }
          
          // Unmark ID as used.
          environments.remove(id);
        }
      }
    }

    synchronized (GLOBAL_LOCK) {
      // Mark parent as stable again.
      parent.stable = true;

      // Register actual child with parent.
      parent.children.put(child.name, child);

      // Register child with global environment set.
      environments.put(child.id, child);

      // Are we done?
      if (null == method) {
        return child;
      }

      // Mark child as unstable.
      child.stable = false;
    }

    // FIXME: this should really be done in its own, new thread under
    // the watchful eyes of a timer.

    // Run initializer.
    success = false;
    try {
      method.invoke(null, new Object[] { child, closure });
      success = true;
    } catch (IllegalAccessException x) {
      throw new Bug("Unexpected exception (" + x + ")");
    } catch (IllegalArgumentException x) {
      throw new Bug("Unexpected exception (" + x + ")");
    } finally {
      synchronized (GLOBAL_LOCK) {
        if (! success) {
          // Unload child.
          child.unload(false);
        }

        // Mark child as stable again.
        child.stable = true;
      }
    }

    // Return new child.
    return child;
  }

  /**
   * Load the specified initializer and execute it within the
   * specified environment. This method loads the specified class and
   * invokes its <code>public static void init(Environment,
   * Object)</code> method on the specified environment and closure.
   *
   * <p>Note that the initializer may throw any
   * <code>Throwable</code>. If the initializer terminates
   * exceptionally, all event handlers for this environment are
   * automatically unlinked, thus ensuring that objects created by the
   * initializer and linking operations performed by the initializer
   * do not persist. However, error recovery does not recreate the
   * class loader for the environment, which requires an explicit
   * {@link #unload unload} operation.</p>
   *
   * @param   requestorId  The ID of the environment requesting this
   *                       operation.
   * @param   envId        The ID of the environment to execute
   *                       the initializer in.
   * @param   init         The name of the class providing the
   *                       initializer.
   * @param   closure      The closure for the initializer.
   * @throws  ClassNotFoundException
   *                       Signals that the class with name
   *                       <code>init</code> cannot be found.
   * @throws  NoSuchMethodException
   *                       Signals that the class with name
   *                       <code>init</code> does not have an
   *                       appropriate <code>init()</code> method.
   * @throws  InvocationTargetException
   *                       Signals an exceptional condition while
   *                       executing the initializer.
   * @throws  IllegalStateException
   *                       Signals that the system is not running, that
   *                       the specified environment is already active,
   *                       that it is currently being modified, or that
   *                       it's main event handler has already been
   *                       linked.
   * @throws  SecurityException
   *                       Signals that the caller does not have
   *                       permission to manage environments, or that the
   *                       specified parent environment is not a descendant
   *                       of the specified requesting environment.
   */
  public static void load(Guid requestorId, Guid envId,
                          String init, Object closure)
    throws ClassNotFoundException, NoSuchMethodException,
           InvocationTargetException {

    ensurePermission();

    // Basic consistency check.
    if (null == envId) {
      throw new NullPointerException("Null environment ID");
    }

    Environment env;

    // Make sure system is alive and environment is inactive.
    synchronized (GLOBAL_LOCK) {
      ensureAlive();
      env = lookup0(envId);
      ensureStable(env);
      if (null != requestorId) {
        ensureLineage(env, lookup0(requestorId));
      }
      if (INACTIVE != env.status) {
        throw new IllegalStateException("Environment not in inactive " +
                                        "state (" + env.id + ")");
      }

      // Make sure main has not been linked yet.
      if (env.main.isLinked()) {
        throw new IllegalStateException("Main event handler already " +
                                        "linked (" + env.id + ")");
      }

      // Mark environment as unstable.
      env.stable = false;
    }

    // FIXME: this should really be done in its own, new thread under
    // the watchful eyes of a timer.

    // Get and invoke the initializer.
    boolean success = false;
    env.protection.initializeLoader();

    try {
      getInitializer(init, env.protection.loader).invoke(null, new
        Object[] { env, closure });
      success = true;
    } catch (IllegalAccessException x) {
      throw new Bug("Unexpected exception (" + x + ")");
    } catch (IllegalArgumentException x) {
      throw new Bug("Unexpected exception (" + x + ")");
    } finally {
      synchronized (GLOBAL_LOCK) {
        if (! success) {
          // Unload environment.
          env.unload(false);
        }

        // Mark environment as stable again.
        env.stable = true;
      }
    }
  }

  /**
   * Get the initializer from the class with the specified name, using
   * the specified class loader to load the class.
   *
   * @param    name    The name of the class with the initializer.
   * @param    loader  The class loader to load the class with.
   * @return           The initializer.
   * @throws   ClassNotFoundException
   *                   Signals that no class with the specified name
   *                   exists.
   * @throws   NoSuchMethodException
   *                   Signals that the specified class does not have
   *                   an initializer.
   */
  private static Method getInitializer(String name, ClassLoader loader)
    throws ClassNotFoundException, NoSuchMethodException {

    Class  k = Class.forName(name, true, loader);
    Method m;
    
    try {
      m = k.getMethod("init", SIGNATURE);
    } catch (NoSuchMethodException x) {
      // Add in explicit message.
      throw new NoSuchMethodException("Class has no initializer ("+name+")");
    }
      
    if (! Void.TYPE.equals(m.getReturnType())) {
      throw new NoSuchMethodException("Return type of initializer not " +
                                      "void (" + m + ")");
    }
        
    int mod = m.getModifiers();
    if (! Modifier.isPublic(mod)) {
      throw new NoSuchMethodException("Initializer not public (" + m + ")");
    } else if (! Modifier.isStatic(mod)) {
      throw new NoSuchMethodException("Initializer not static (" + m + ")");
    }
    
    return m;
  }


  // =======================================================================
  //                         Environment lookup
  // =======================================================================

  /**
   * Look up the environment with the specified ID.
   *
   * @param   id  The ID of the environment to look up.
   * @return      The correspoding environment.
   * @throws  NullPointerException
   *              Signals that <code>id</code> is <code>null</code>.
   * @throws  IllegalArgumentException
   *              Signals that no environment with the specified
   *              ID exists.
   * @throws  IllegalStateException
   *              Signals that the system is not running.
   * @throws  SecurityException
   *              Signals that the caller does not have permission to
   *              manage environments.
   */
  public static Environment lookup(Guid id) {
    ensurePermission();
    if (null == id) {
      throw new NullPointerException("Null environment ID");
    }

    synchronized (GLOBAL_LOCK) {
      ensureAlive();
      
      return lookup0(id);
    }
  }

  /**
   * Look up the environment with the specified ID. This method must
   * be called while holding the global lock. This method is package
   * private to avoid synthetic accessors.
   *
   * @param   id  The ID of the environment to look up.
   * @return      The correspoding environment.
   * @throws  IllegalArgumentException
   *              Signals that no environment with the specified
   *              ID exists.
   */
  static Environment lookup0(Guid id) {
    Environment env = (Environment)environments.get(id);
    if ((null == env) || (DUMMY == env)) {
      throw new IllegalArgumentException("No such environment (" + id + ")");
    } else {
      return env;
    }
  }

  /**
   * Locally look up the environment with the specified ID. This
   * method looks up the environment with the specified ID in the
   * subtree of the environment hierarchy that is rooted at this
   * environment.
   *
   * @param   id  The ID of the environment to look up.
   * @return      The correspoding environment.
   * @throws  NullPointerException
   *              Signals that <code>id</code> is <code>null</code>.
   * @throws  IllegalArgumentException
   *              Signals that no environment with the specified
   *              ID exists locally.
   * @throws  IllegalStateException
   *              Signals that the system is not running.
   */
  public Environment lookupLocally(Guid id) {
    if (null == id) {
      throw new NullPointerException("Null environment ID");
    }

    synchronized (GLOBAL_LOCK) {
      ensureAlive();

      // Look up environment.
      Environment env = (Environment)environments.get(id);

      // If we found one, make sure it's a local one.
      if ((null != env) && (DUMMY != env)) {
        Environment tmp = env;
        while ((this != tmp) && (tmp != root)) {
          tmp = tmp.parent;
        }
        if (this != tmp) {
          env = null;
        }
      }

      // Only throw one exception as to not distinguish the case of no
      // environment at all to no local environment.
      if ((null == env) || (DUMMY == env)) {
        throw new IllegalArgumentException("No such environment (" + id + ")");
      } else {
        return env;
      }
    }
  }

  /**
   * Resolve the specified local root and path. The specified local
   * root and path specify either an absolute path, a relative path,
   * or an ID-relative path, and correspond to the <code>ident</code>
   * and <code>path</code> fields of a structured I/O resource
   * descriptor. 
   *
   * <p>This method assumes that the specified path has been
   * normalized.  In other words, path segments other than
   * "<code>.</code>" and "<code>..</code>" as well as a leading
   * "<code>/</code>" segment are always treated as names of child
   * environments. Furthermore, this method assumes that the specified
   * local root and path have been validate. It signals an illegal
   * argument exception if they are not well-formed.</p>
   *
   * @see     one.world.io.SioResource
   *
   * @param   requestorId  The ID of the environment requesting this
   *                       operation.
   * @param   localRootId  The ID of the local root for ID-relative
   *                       paths.
   * @param   path         The path.
   * @return               The corresponding environment.
   * @throws  NullPointerException
   *                       Signals that both <code>localRootId</code>
   *                       and <code>path</code> are <code>null</code>,
   *                       or that an entry in <code>path</code> is
   *                       <code>null</code>.
   * @throws  IllegalArgumentException
   *                       Signals that the specified requesting
   *                       environment does not exist, or that the
   *                       specified local root and path are not
   *                       valid.
   * @throws  UnknownResourceException
   *                       Signals that the specified local root
   *                       and path describe a non-existent environment.
   * @throws  IllegalStateException
   *                       Signals that the system is not running.
   * @throws  SecurityException
   *                       Signals that the caller does not have
   *                       permission to manage environments, or that
   *                       the specified local root and path specify
   *                       an environment which is not a descendant
   *                       of the requesting environment.
   */
  public static Environment resolve(Guid requestorId, Guid localRootId,
                                    String[] path)
    throws UnknownResourceException {

    ensurePermission();
    if ((null == localRootId) && (null == path)) {
      throw new NullPointerException("Null local root and path");
    }

    Environment requestor;
    Environment result;

    synchronized (GLOBAL_LOCK) {
      requestor = ((null == requestorId)? root : lookup0(requestorId));

      // Fill in local root environment.
      if (null != localRootId) {
        try {
          result  = lookup0(localRootId);
        } catch (IllegalArgumentException x) {
          throw new UnknownResourceException(x.getMessage());
        }
      } else if (0 == path.length) {
        throw new IllegalArgumentException("Empty path");
      } else if ("/".equals(path[0])) {
        result  = root;
      } else {
        result  = requestor;
      }

      // Make sure local root is a descendant of the requesting
      // environment.
      ensureLineage(result, requestor);

      // Process the path.
      if (null != path) {
        for (int i=0; i<path.length; i++) {
          String segment = path[i];

          if (null == segment) {
            throw new NullPointerException("Null path segment");

          } else if ("/".equals(segment)) {
            if (0 == i) {
              continue;
            } else {
              throw new IllegalArgumentException("Invalid \"/\" path segment");
            }

          } else if (".".equals(segment)) {
            continue;

          } else if ("..".equals(segment)) {
            if (result == root) {
              throw new IllegalArgumentException("Unable to access parent " +
                                                 "of root environment");
            } else if (requestor == result) {
              throw new SecurityException("Unable to access parent of " +
                                          "requesting environment " +
                                          requestor.id);
            } else {
              result = result.parent;
            }

          } else {
            Environment child = null;

            if (null != result.children) {
              child = (Environment)result.children.get(segment);
            }

            if ((null == child) || (DUMMY == child)) {
              throw new UnknownResourceException("Environment " +
                                                 result.id +
                                                 " has no child \"" +
                                                 segment + "\"");
            }
            result = child;
          }
        }
      }
    }

    return result;
  }


  // =======================================================================
  //                         Environment moving
  // =======================================================================

  /**
   * Rename the specified environment to the specified name.
   *
   * @param   requestorId  The ID of the environment requesting this
   *                       operation.
   * @param   envId        The ID of the environment to be renamed.
   * @param   name         The new name.
   * @throws  NullPointerException
   *                       Signals that <code>envId</code> or
   *                       <code>name</code> is <code>null</code>.
   * @throws  IllegalArgumentException
   *                       Signals that no environment with the
   *                       specified ID exists, that <code>name</code>
   *                       is an illegal name for an environment, or that
   *                       the environment's parent already has a child
   *                       with the specified name.
   * @throws  IllegalStateException
   *                       Signals that the system is not running or that
   *                       the specified environment is being modified.
   * @throws  SecurityException
   *                       Signals that the caller does not have permission
   *                       to manage environments, or that the specified
   *                       environment is not a descendant of the
   *                       specified requesting environment.
   */
  public static void rename(Guid requestorId, Guid envId, String name) {
    ensurePermission();
    ensureName(name);

    // Basic consistency checks.
    if (null == envId) {
      throw new NullPointerException("Null environment ID");
    } else if (envId.equals(root.id)) {
      throw new IllegalArgumentException("Unable to rename root environment (" +
                                         root.id + ")");
    }

    Environment env     = null;
    boolean     release = false;

    synchronized (GLOBAL_LOCK) {
      ensureAlive();
      env = lookup0(envId);
      ensureStable(env);
      if (null != requestorId) {
        ensureLineage(env, lookup0(requestorId));
      }
      
      // Consistency checks.
      if (env.name.equals(name)) {
        return;
      } else if (env.parent.children.containsKey(name)) {
        throw new IllegalArgumentException("Environment with same name " +
                                           "already exists (" + name + ")");
      }
      
      // Rename this environment.
      env.parent.children.remove(env.name);
      env.parent.children.put(name, env);
      env.name = name;
      
      // Mark environment as unstable.
      env.stable = false;
    }

    // Tell the tuple store.
    TupleStore.rename(env.id, name);

    synchronized (GLOBAL_LOCK) {
      // Mark environment as stable again.
      env.stable = true;
    }
  }

  /**
   * Move the specified environment to the specified new parent. This
   * method moves the specified environment and all its descendants to
   * the specified new parent. To move active environments, this method
   * uses a check-point.
   *
   * @param   requestorId  The ID of the environment requesting this
   *                       operation.
   * @param   envId        The ID of the environment to move.
   * @param   newParentId  The ID of the new parent environment.
   * @return               <code>true</code> if the requesting environment
   *                       has been moved.
   * @throws  NullPointerException
   *                       Signals that <code>envId</code> or
   *                       <code>newParentId</code> is
   *                       <code>null</code>.
   * @throws  IllegalArgumentException
   *                       Signals that no environment with the specified
   *                       ID exists, that the new parent already has
   *                       a child with the same name as the environment
   *                       to be moved, or that the environment to be
   *                       moved is the root environment.
   * @throws  IOException  Signals an exceptional condition when
   *                       creating the check-point for the environments
   *                       to be moved.
   * @throws  InvalidTupleException
   *                       Signals an exceptional condition while
   *                       restoring the check-point for the moved
   *                       environment(s).
   * @throws  ClassNotFoundException
   *                       Signals that a class was not found while
   *                       restoring the check-point for the moved
   *                       environment(s).
   * @throws  IllegalStateException
   *                       Signals that the system is not running, or that
   *                       one of the specified environments is currently
   *                       being modified.
   * @throws  SecurityException
   *                       Signals that the caller does not have
   *                       permission to manage environments, or that
   *                       either the environment to be moved or the
   *                       new parent environment is not a descendant
   *                       of the specified requesting environment.
   */
  public static boolean move(Guid requestorId, Guid envId, Guid newParentId)
    throws IOException, InvalidTupleException, ClassNotFoundException {

    ensurePermission();

    // Basic consistency checks.
    if (null == envId) {
      throw new NullPointerException("Null environment ID");
    } else if (null == newParentId) {
      throw new NullPointerException("Null environment ID");
    } else if (envId.equals(root.id)) {
      throw new IllegalArgumentException("Unable to move root environment (" +
                                         root.id + ")");
    }

    Environment env;
    Environment requestor = null;
    Environment newParent;
    Map         set;                        // The serialization set.
    List        envList;                    // The list of environments.
    Set         restartSet = new HashSet(); // The restart set.

    synchronized (GLOBAL_LOCK) {
      ensureAlive();
      env       = lookup0(envId);
      newParent = lookup0(newParentId);
      ensureStable(env);
      ensureStable(newParent);
      if (null != requestorId) {
        requestor = lookup0(requestorId);
        ensureLineage(env,       requestor);
        ensureLineage(newParent, requestor);
      }

      // Consistency checks.
      if (env.protection.equals(env.parent.protection)) {
        throw new IllegalArgumentException("Unable to move partial " + 
                                           "protection domain (" +
                                           env.protection + ")");
      } else if (newParentId.equals(env.parent.id)) {
        return false;
      } else if (null == newParent.children) {
        newParent.children = new HashMap();
      } else if (newParent.children.containsKey(env.name)) {
        throw new IllegalArgumentException("Environment with same name " +
                                           "already exists (" + env.name +
                                           ")");
      }

      // Set up serialization set.
      set = (Map)serializationSet.get();
      set.clear();
      set.put(env.id, env);
      collectDescendants(env, set);

      // Set up list of environments.
      envList = new ArrayList(set.values());

      // Make sure environments are stable.
      Iterator iter = envList.iterator();
      while (iter.hasNext()) {
        Environment tmp = (Environment)iter.next();
        if ((TERMINATING == tmp.status) || (! tmp.stable)) {
          set.clear();
          throw new IllegalStateException("Environment currently being " +
                                          "modified (" + tmp.id + ")");
        }
      }

      // (1) Mark environments as unstable, (2) replace environments
      // in the serialization set with their IDs, (3) deactivate
      // active animators, and (4) set up the restart set, i.e., the
      // set of environments whose animators need to be re-activated
      // after the check-point has been created and restored.
      iter = envList.iterator();
      while (iter.hasNext()) {
        Environment tmp = (Environment)iter.next();

        // Mark as unstable.
        tmp.stable = false;

        // Replace environment with its ID.
        set.put(tmp.id, tmp.id);

        // Deactivate active animators.
        Animator anim = tmp.concurrency.anim;
        if ((null != anim) && (Animator.ACTIVE == anim.getStatus())) {
          anim.setStatus(Animator.INACTIVE);
          restartSet.add(tmp.id);
        }
      }

      // Mark name as used under new parent and mark new parent as
      // unstable.
      newParent.children.put(env.name, DUMMY);
      newParent.stable = false;
    }

    // Create check-point.
    boolean    success = false;
    CheckPoint cp;
    try {
      cp      = env.checkPoint(envList, set, false);
      success = true;
    } finally {
      synchronized (GLOBAL_LOCK) {
        if (! success) {
          // Clear serialization set.
          set.clear();

          // Unmark name as used.
          newParent.children.remove(env.name);
          if (0 == newParent.children.size()) {
            newParent = null;
          }

          // Mark new parent as stable again.
          newParent.stable = true;
        }

        // Re-activate de-activated animators. In case of a successful
        // checkpoint, also recreate the serialization set with the
        // actual environments as values. In case of an exception,
        // mark environments as stable again.
        Iterator iter = envList.iterator();
        while (iter.hasNext()) {
          Environment tmp = (Environment)iter.next();
          
          if (success) {
            set.put(tmp.id, tmp);
          } else {
            tmp.stable = true;
          }

          if (restartSet.contains(tmp.id)) {
            Animator anim = tmp.concurrency.anim;

            if (null != anim) {
              anim.setStatus(Animator.ACTIVE);
            }
          }
        }
      }
    }

    success = false;
    try {
      // Restore check-point.
      env.restore(cp, set, MOVED_LOCALLY, newParent);

      // Tell the tuple store.
      TupleStore.move(envId, newParentId);

      success = true;
    } finally {
      if (! success) {
        synchronized (GLOBAL_LOCK) {
          // Unmark name as used.
          newParent.children.remove(env.name);
          if (0 == newParent.children.size()) {
            newParent = null;
          }
          
          // Mark new parent as stable again.
          newParent.stable = true;
        }
      }
    }

    synchronized (GLOBAL_LOCK) {
      // Mark new parent as stable again.
      newParent.stable = true;

      // Mark environments as stable again and enqueue notification of
      // the move.
      Iterator iter = envList.iterator();
      while (iter.hasNext()) {
        Environment tmp = (Environment)iter.next();

        // Mark as stable again.
        tmp.stable = true;

        // Enqueue notification of the move. It is safe to do inside
        // the global lock, b/c the event is processed by an animator
        // (which is currently inactive).
        if (ACTIVE == tmp.status) {
          try {
            tmp.main.handleForced(new
              EnvironmentEvent(tmp.request,null,EnvironmentEvent.MOVED,tmp.id));
            
            if (Constants.DEBUG_ENVIRONMENT) {
              SystemLog.LOG.log(tmp, "Sent moved environment event");
            }

          } catch (NotLinkedException x) {
            SystemLog.LOG.logError(tmp,
                                   "main handler not linked when moving",
                                   x);
            tmp.concurrency.terminate();
            tmp.status = INACTIVE;
          }
        }
      }

      // Reactivate deactivated animators.
      iter = envList.iterator();
      while (iter.hasNext()) {
        Environment tmp = (Environment)iter.next();

        if (ACTIVE == tmp.status) {
          // Register animator with controller.
          if (tmp.concurrency.anim instanceof ThreadPool) {
            controller.add(tmp.concurrency.anim);
          }

          // Activate!
          tmp.concurrency.anim.setStatus(Animator.ACTIVE);
        }
      }
    }

    // Done.
    return envList.contains(requestor);
  }

  /**
   * Copy the specified environment to the specified new parent. This
   * method copies the specified environment and all its descendants
   * to the specified new parent. To copy active environments, this
   * method uses a modified check-point of the original
   * environment(s).
   *
   * @param   requestorId  The ID of the environment requesting this
   *                       operation.
   * @param   envId        The ID of the environment to copy.
   * @param   newParentId  The ID of the new parent environment.
   * @throws  NullPointerException
   *                       Signals that <code>envId</code> or
   *                       <code>newParentId</code> is
   *                       <code>null</code>.
   * @throws  IllegalArgumentException
   *                       Signals that no environment with the specified
   *                       ID exists, that the new parent already has
   *                       a child with the same name as the environment
   *                       to be copied, or that the environment to be
   *                       copied is the root environment.
   * @throws  IOException  Signals an exceptional condition when
   *                       creating the check-point for the environments
   *                       to be copied or when copying the persistent
   *                       environment state.
   * @throws  InvalidTupleException
   *                       Signals an exceptional condition while
   *                       restoring the check-point for the copied
   *                       environment(s).
   * @throws  ClassNotFoundException
   *                       Signals that a class was not found while
   *                       restoring the check-point for the copied
   *                       environment(s).
   * @throws  IllegalStateException
   *                       Signals that the system is not running, or that
   *                       one of the specified environments is currently
   *                       being modified.
   * @throws  SecurityException
   *                       Signals that the caller does not have
   *                       permission to manage environments, or that
   *                       either the environment to be copied or the
   *                       new parent environment is not a descendant
   *                       of the specified requesting environment.
   */
  public static void copy(Guid requestorId, Guid envId, Guid newParentId) 
    throws IOException, InvalidTupleException, ClassNotFoundException {

    ensurePermission();

    // Basic consistency checks.
    if (null == envId) {
      throw new NullPointerException("Null environment ID");
    } else if (null == newParentId) {
      throw new NullPointerException("Null environment ID");
    } else if (envId.equals(root.id)) {
      throw new IllegalArgumentException("Unable to copy root environment (" +
                                         root.id + ")");
    }

    Environment  env;
    Environment  newParent;
    Map          set;                        // The serialization set.
    List         envList;                    // The list of environments.
    Set          restartSet = new HashSet(); // The restart set.
    Descriptor[] source;                     // List of source descriptors.
    Descriptor[] target;                     // List of target descriptors.

    synchronized (GLOBAL_LOCK) {
      ensureAlive();
      env       = lookup0(envId);
      newParent = lookup0(newParentId);
      ensureStable(env);
      ensureStable(newParent);
      if (null != requestorId) {
        Environment requestor = lookup0(requestorId);
        ensureLineage(env,       requestor);
        ensureLineage(newParent, requestor);
      }

      // Consistency checks.
      if (env.protection.equals(env.parent.protection)) {
        throw new IllegalArgumentException("Unable to copy partial " + 
                                           "protection domain (" +
                                           env.protection + ")");
      } else if (newParentId.equals(env.parent.id)) {
        throw new IllegalArgumentException("Unable to copy environment " +
                                           "over itself");
      } else if (null == newParent.children) {
        newParent.children = new HashMap();
      } else if (newParent.children.containsKey(env.name)) {
        throw new IllegalArgumentException("Environment with same name " +
                                           "already exists (" + env.name +
                                           ")");
      }

      // Set up serialization set.
      set = (Map)serializationSet.get();
      set.clear();
      set.put(env.id, env);
      collectDescendants(env, set);

      // Set up list of environments.
      envList = new ArrayList(set.values());

      // Make sure environments are stable.
      Iterator iter = envList.iterator();
      while (iter.hasNext()) {
        Environment tmp = (Environment)iter.next();
        if ((TERMINATING == tmp.status) || (! tmp.stable)) {
          set.clear();
          throw new IllegalStateException("Environment currently being " + 
                                          "modified (" + tmp.id + ")");
        }
      }

      // (1) Mark environments as unstable, (2) replace environments
      // in the serialization set with their IDs, (3) deactivate
      // active animators, (4) set up the restart set, i.e., the set
      // of environments whose animators need to be re-activated after
      // the check-point has been created, and (5) set up the array of
      // source descriptors.
      int i  = 0;
      source = new Descriptor[envList.size()];

      iter = envList.iterator();
      while (iter.hasNext()) {
        Environment tmp  = (Environment)iter.next();

        // Mark as unstable.
        tmp.stable = false;

        // Replace environment with its ID.
        Guid id2;
        do {
          id2 = new Guid();
        } while (environments.containsKey(id2));

        set.put(tmp.id, id2);

        // Mark ID as used.
        environments.put(id2, DUMMY);

        // Deactive active animators and set up restart set.
        Animator anim = tmp.concurrency.anim;
        if ((null != anim) && (Animator.ACTIVE == anim.getStatus())) {
          anim.setStatus(Animator.INACTIVE);
          restartSet.add(tmp.id);
        }

        // Set up source descriptors.
        source[i] = new Descriptor(tmp);

        // Increment index.
        i++;
      }

      // Set up the array of target descriptors. This must be done in
      // a separate loop, b/c the serialization set must map IDs to
      // IDs for this to work (and not IDs to environments).
      target = new Descriptor[source.length];
      iter   = envList.iterator();
      i      = 0;
      while (iter.hasNext()) {
        Environment tmp  = (Environment)iter.next();
        Descriptor  desc = new Descriptor(tmp);
        desc.ident       = (Guid)set.get(desc.ident);
        if (envId.equals(tmp.id)) {
          desc.parent    = newParentId;
        } else {
          desc.parent    = (Guid)set.get(desc.parent);
        }
        desc.protection  = (Guid)set.get(desc.protection);
        target[i]        = desc;
        i++;
      }

      // Mark name as used under new parent and mark new parent as
      // unstable.
      newParent.children.put(env.name, DUMMY);
      newParent.stable = false;
    }

    boolean       success = false;
    Environment   newRoot;
    Environment[] newEnvironments;

    try {
      // Create check-point.
      CheckPoint cp = env.checkPoint(envList, set, true);

      // Create new environment tree.
      set.clear();
      newRoot = createLocalTree(newParent, target, set);

      // Restore check-point into new tree.
      newEnvironments = env.restore(cp, set, EnvironmentEvent.CLONED, null);
      success = true;
    } finally {
      // Clear serialization set.
      set.clear();

      if (! success) {
        synchronized (GLOBAL_LOCK) {
          // Mark original environments as stable again and
          // re-activate inactive animators.
          Iterator iter = envList.iterator();
          while (iter.hasNext()) {
            Environment tmp = (Environment)iter.next();

            tmp.stable = true;
            if (restartSet.contains(tmp.id)) {
              Animator anim = tmp.concurrency.anim;

              if (null != anim) {
                anim.setStatus(Animator.ACTIVE);
              }
            }
          }

          // Unmark name as used and mark new parent as stable again.
          newParent.children.remove(env.name);
          if (0 == newParent.children.size()) {
            newParent.children = null;
          }
          newParent.stable = true;

          // Unmark IDs as used.
          for (int i=0; i<target.length; i++) {
            environments.remove(target[i].ident);
          }
        }
      }
    }

    // Copy the tuple store(s).
    Object closure;
    try {
      closure = TupleStore.startCopy(source, target);
    } catch (IOException x) {
      synchronized (GLOBAL_LOCK) {
        // Mark original environments as stable again and
        // re-activate inactive animators.
        Iterator iter = envList.iterator();
        while (iter.hasNext()) {
          Environment tmp = (Environment)iter.next();

          tmp.stable = true;
          if (restartSet.contains(tmp.id)) {
            Animator anim = tmp.concurrency.anim;

            if (null != anim) {
              anim.setStatus(Animator.ACTIVE);
            }
          }
        }

        // Unmark name as used and mark new parent as stable again.
        newParent.children.remove(env.name);
        if (0 == newParent.children.size()) {
          newParent.children = null;
        }
        newParent.stable = true;

        // Unmark IDs as used and terminate inactive animators.
        for (int i=0; i<newEnvironments.length; i++) {
          environments.remove(newEnvironments[i].id);
          newEnvironments[i].concurrency.terminate();
        }
      }
      throw x;
    }

    synchronized (GLOBAL_LOCK) {
      Guid dead = null;

      // Mark original environments as stable again and re-activate
      // inactive animators. Also, make sure that the original
      // environments still exist. Note that success is still true.
      Iterator iter = envList.iterator();
      while (iter.hasNext()) {
        Environment tmp = (Environment)iter.next();

        tmp.stable = true;
        if (restartSet.contains(tmp.id)) {
          Animator anim = tmp.concurrency.anim;

          if (null != anim) {
            anim.setStatus(Animator.ACTIVE);
          }
        }

        if ((DESTROYING == tmp.status) || (DESTROYED == tmp.status)) {
          success = false;
          dead    = tmp.id;
        }
      }

      // Mark new parent as stable again.
      newParent.stable = true;
      if ((DESTROYING == newParent.status) || (DESTROYED == newParent.status)) {
        success = false;
        dead    = newParent.id;
      }

      if ((SHUTDOWN      == globalStatus) ||
          (SHUTTING_DOWN == globalStatus) ||
          (! success)) {
        // Unmark name as used.
        newParent.children.remove(env.name);
        if (0 == newParent.children.size()) {
          newParent.children = null;
        }

        // Unmark IDs as used and terminate inactive animators.
        for (int i=0; i<newEnvironments.length; i++) {
          environments.remove(newEnvironments[i].id);
          newEnvironments[i].concurrency.terminate();
        }

        // Abort tuple store copy operation.
        TupleStore.abortCopy(closure);

        // Signal exceptional condition.
        if (null == dead) {
          throw new IllegalStateException("System shut down while copying " +
                                          "environment(s)");
        } else {
          throw new IllegalStateException("Environment (" + dead + ") " +
                                          "destroyed while being copied");
        }
      }

      // Commit the tuple store copy operation.
      TupleStore.commitCopy(closure);

      // Link in actual environment tree.
      newParent.children.put(env.name, newRoot);

      // Register environments with global environment set.
      for (int i=0; i<newEnvironments.length; i++) {
        Environment tmp = newEnvironments[i];

        environments.put(tmp.id, tmp);
      }

      // Re-activate inactive animators.
      for (int i=0; i<newEnvironments.length; i++) {
        Environment tmp = newEnvironments[i];

        if (ACTIVE == tmp.status) {
          // Register animator with controller.
          if (tmp.concurrency.anim instanceof ThreadPool) {
            controller.add(tmp.concurrency.anim);
          }
          tmp.concurrency.anim.setStatus(Animator.ACTIVE);
        }
      }
    }
  }

  /**
   * Move the specified environment to the new parent environment with
   * the specified path on the specified remote host.  This method
   * remotely moves, or copies if <code>clone</code> is
   * <code>true</code>, the specified environment to the specified new
   * parent environment.
   *
   * <p>When this method returns, the move is <i>not</i>
   * complete. Rather, the move operation continues asynchronously and
   * the specified result handler is notified upon completion, be it
   * normal or exceptional. Note, however, that the result handler is
   * <i>not</i> notified of a successful move (but not copy) operation
   * if the requesting environment has been moved itself.</p>
   *
   * <p>The following exceptional conditions may be signalled to the
   * specified result handler:<dl>
   *
   * <dt>{@link IllegalArgumentException}</dt>
   * <dd>Signals that no environment with the specified ID exists,
   * that the specified environment is the root environment, that the
   * specified environment represents a partial protection domain, or
   * that the new parent already has a child with the same name as the
   * environment to be moved.</dd>
   *
   * <dt>{@link UnknownResourceException}</dt>
   * <dd>Signals that no such remote host or remote environment
   * exists.</dd>
   *
   * <dt>{@link IOException}</dt>
   * <dd>Signals an exceptional condition while moving the persistent
   * environment state.</dd>
   *
   * <dt>{@link TupleException}</dt>
   * <dd>Signals an exceptional condition while restoring the
   * check-point for the moved environments.</dd>
   *
   * <dt>{@link ClassNotFoundException}</dt>
   * <dd>Signals that a class was not found while restoring the
   * check-point for the moved environments.</dd>
   *
   * <dt>{@link IllegalStateException}</dt>
   * <dd>Signals that the system is not running (or being shut down
   * while the move is in progress), or that one of the specified
   * environments is currently being modified.</dd>
   *
   * </dl></p>
   *
   * <p>Note that using this method to move an environment and its
   * descendants to some location on the same node will always fail,
   * because the environment IDs are already taken (by the original
   * environments). Local moves must be performed with the {@link
   * #move move} method.</p>
   *
   * @see     MovingProtocol
   *
   * @param   requestorId    The ID of the environment requesting this
   *                         operation.
   * @param   resultHandler  The result handler.
   * @param   resultClosure  The closure for the event delivered to the
   *                         result handler.
   * @param   envId          The ID of the environment to move.
   * @param   remoteHost     The host to move the environment to.
   * @param   remotePort     The port on which the remote node is
   *                         exporting the REP event handler for
   *                         moving environments, or -1 if the default
   *                         REP port is to be used.
   * @param   remotePath     The path for the new parent environment.
   * @param   clone          The flag for whether to copy the
   *                         environment(s).
   * @throws  NullPointerException
   *                         Signals that <code>resultHandler</code>,
   *                         <code>envId</code>,
   *                         <code>remoteHost</code>, or
   *                         <code>remotePath</code> is
   *                         <code>null</code>.
   * @throws  IllegalArgumentException
   *                         Signals that the remote port is invalid.
   * @throws  SecurityException
   *                         Signals that the caller does not have
   *                         permission to manage environments.
   */
  public static void moveAway(Guid requestorId, EventHandler resultHandler,
                              Object resultClosure, Guid envId,
                              String remoteHost, int remotePort,
                              String remotePath, boolean clone) {
    ensurePermission();

    // Basic consistency checks.
    if (null == resultHandler) {
      throw new NullPointerException("Null result handler");
    } else if (null == envId) {
      throw new NullPointerException("Null environment ID");
    } else if (null == remoteHost) {
      throw new NullPointerException("Null remote host");
    } else if (null == remotePath) {
      throw new NullPointerException("Null remote path");
    } else if ((-1 != remotePort) &&
               ((0 >= remotePort) || (65536 <= remotePort))) {
      throw new IllegalArgumentException("Invalid port number ("+remotePort+
                                         ")");
    }

    // Start the move.
    Environment env;
      
    try {
      // We can't move the root environment.
      if (envId.equals(root.id)) {
        throw new IllegalArgumentException("Unable to move root environment (" +
                                           root.id + ")");
      }

      // Resolve the local root of the environment tree being moved.
      synchronized (GLOBAL_LOCK) {
        ensureAlive();
        env = lookup0(envId);
        ensureStable(env);
        if (null != requestorId) {
          ensureLineage(env, lookup0(requestorId));
        }
        
        // Consistency check.
        if (env.protection.equals(env.parent.protection)) {
          throw new IllegalArgumentException("Unable to move partial " +
                                             "protection domain (" + 
                                             env.protection + ")");
        }
        
        // Mark environment as unstable.
        env.stable = false;
      }
    } catch (Exception x) {
      resultHandler.handle(new
        ExceptionalEvent(ExceptionHandler.HANDLER, resultClosure, x));
      return;
    }

    // Set up the sender and its operation.
    Sender sender    = new
      Sender(requestorId, resultHandler, resultClosure, env,
             remoteHost, remotePort, remotePath, clone);
    sender.operation = new
      Operation(0, MovingProtocol.TIMEOUT, root.timer, root.request, sender);

    // Export the operation's response handler.
    sender.operation.handle(new
      BindingRequest(null, new Operation.ChainingClosure(), new
        RemoteDescriptor(sender.operation.getResponseHandler()),
                     Duration.FOREVER));
  }

  /**
   * Propagate the specified accept request up the environment
   * hierarchy, starting with this environment. The processing of
   * accept requests differs from the regular request/monitor
   * mechanism in that event delivery is attempted first to the new
   * parent environment's monitor handler.
   *
   * <p>The specified accept request must be a valid accept
   * request.</p>
   *
   * @param   request  The request to propagate.
   */
  void propagate(AcceptRequest request) {
    // Annotate the event with the ID of the originating environment,
    // which is the new parent environment.
    request.setMetaData(Constants.REQUESTOR_ID, id);

    // Find the first ancestral environment which is active and whose
    // monitor handler is linked. Unlike the regular request/monitor
    // event propagation, the new parent environment is included in
    // this search.
    Environment target = this;

    if (target != root) {
      while (true) {
        if ((ACTIVE == target.status) && target.monitor.isLinked()) {
          // Try to deliver the event to the monitor event handler.
          synchronized (GLOBAL_LOCK) {
            // Verify that the environment really is active.
            if (ACTIVE == target.status) {
              // Note that it is safe to delive the event under the
              // global lock, b/c it is processed by an animator.
              try {
                target.monitor.handle(request);
                return;
              } catch (NotLinkedException x) {
                // Continue with parent environment.
              } catch (IllegalStateException x) {
                // Continue with parent environment.
              } catch (NoBufferSpaceException x) {
                // All queues are full. Drop this event.
                return;
              }
            }
          }
        }

        Environment child = target;
        target            = target.parent;

        if (null == target) {
          // The environment has been destroyed.
          request.source.handle(new
            ExceptionalEvent(ExceptionHandler.HANDLER, request.closure, new
              IllegalStateException("Environment is being or has been " +
                                    "destroyed (" + child.id + ")")));
          return;

        } else if (target == root) {
          // We reached the root environment.
          break;
        }
      }
    }

    // The root environment is guaranteed to be active and its monitor
    // event handler is guaranteed to be linked.  Furthermore, this
    // invocation is not going through an animator.
    target.monitor.handle(request);
  }

  /**
   * Accept the migrating environment hierarchy described by the
   * specified accept request. The accept request must be a valid
   * accept request.
   *
   * @param   requestorId  The ID of the requesting environment.
   * @param   request      The accept request.
   * @throws  SecurityException
   *                       Signals that the caller does not have
   *                       permission to manage environments.
   */
  static void acceptMove(Guid requestorId, AcceptRequest request) {
    ensurePermission();

    // Set up the receiver.
    Receiver receiver = new Receiver(request.sender,request.ident,request.name,
                                     request.clone);

    // Prepare new parent to receive environment subtree.
    try {
      synchronized (GLOBAL_LOCK) {
        ensureAlive();
        receiver.newParent = lookup0(request.newParent);
        ensureStable(receiver.newParent);
        if (null != requestorId) {
          ensureLineage(receiver.newParent, lookup0(requestorId));
        }

        // Consistency checks.
        if ((null != receiver.newParent.children) &&
            receiver.newParent.children.containsKey(receiver.envName)) {
          throw new IllegalArgumentException("Environment with same name " +
                                             "already exists (" +
                                             receiver.envName + ")");
        }

        // Mark name as used under new parent and mark new parent as
        // unstable.
        if (null == receiver.newParent.children) {
          receiver.newParent.children = new HashMap();
        }
        receiver.newParent.children.put(receiver.envName, DUMMY);
        receiver.newParent.stable = false;
      }
    } catch (RuntimeException x) {
      // Enter error state directly (no clean-up to do) and notify sender.
      receiver.state      = MovingProtocol.STATE_ERROR;
      ExceptionalEvent ee = new
        ExceptionalEvent(NullHandler.NULL, request.closure, x);
      root.request.handle(new
        RemoteEvent(receiver, request.closure, receiver.sender, ee));
      if (Constants.DEBUG_ENVIRONMENT) {
        SystemLog.LOG.logWarning(receiver, "Sent exceptional event", x);
      }
      return;
    }

    // Set up operation and export its response handler.
    receiver.operation = new Operation(0, MovingProtocol.TIMEOUT, root.timer,
                                       root.request, receiver);
    receiver.operation.handle(new
      BindingRequest(null, request.closure, new
        RemoteDescriptor(receiver.operation.getResponseHandler()),
                     Duration.FOREVER));
  }


  // =======================================================================
  //                    Status, activation, termination
  // =======================================================================

  /**
   * Get the current status for this environment.
   *
   * @see     #INACTIVE
   * @see     #ACTIVE
   * @see     #TERMINATING
   * @see     #DESTROYING
   * @see     #DESTROYED
   *
   * @return     The current status for this environment.
   * @throws  IllegalStateException
   *             Signals that the system is not running.
   */
  public int getStatus() {
    synchronized (GLOBAL_LOCK) {
      ensureAlive();

      return status;
    }
  }

  /**
   * Activate the specified environment. Activating an already active
   * environment has no effect.
   *
   * @param   requestorId  The ID of the environment requesting this
   *                       operation.
   * @param   envId        The ID of the environment to be activated.
   * @throws  NullPointerException
   *                       Signals that <code>envId</code> is
   *                       <code>null</code>.
   * @throws  IllegalArgumentException
   *                       Signals that no environment with the
   *                       specified ID exists.
   * @throws  NotLinkedException
   *                       Signals that the specified environment has
   *                       not been linked against a main event handler.
   * @throws  IllegalStateException
   *                       Signals that the system is not running,
   *                       that the specified environment is not in
   *                       the active or inactive state, or that the
   *                       specified environment is currently being
   *                       modified.
   * @throws  SecurityException
   *                       Signals that the caller does not have
   *                       permission to manage environments, or that
   *                       the specified environment is not a descendant
   *                       of the specified requesting environment.
   */
  public static void activate(Guid requestorId, Guid envId) {
    ensurePermission();

    // Basic consistency checks.
    if (null == envId) {
      throw new NullPointerException("Null environment ID");
    }

    Environment env;

    synchronized (GLOBAL_LOCK) {
      ensureAlive();
      env = lookup0(envId);
      ensureStable(env);
      if (null != requestorId) {
        ensureLineage(env, lookup0(requestorId));
      }
      if (ACTIVE == env.status) {
        // Done already.
        return;
      } else if (INACTIVE != env.status) {
        throw new IllegalStateException("Environment not in inactive or " +
                                        "active state (" + env.id + ")");
      }

      // Activate!
      boolean isRoot = (env == root);
      env.concurrency.animate(env.singleThreaded, isRoot);
      env.status = ACTIVE;

      // Register animator with controller.
      Animator anim = env.concurrency.anim;

      if (anim instanceof ThreadPool) {
        if (isRoot) {
          rootController.add(anim);
        } else {
          controller.add(anim);
        }
      }

      // Tell main event handler about the activation. We do this
      // inside the global lock, b/c doing so avoids races between
      // activations and terminations. It is safe, b/c the event is
      // processed by an animator. The animator has just been created,
      // so a normal event handler invocation will do.
      try {
        env.main.handle(new
          EnvironmentEvent(env.request, null, EnvironmentEvent.ACTIVATED,
                           env.id));
      } catch (NotLinkedException x) {
        // Undo activation.
        env.concurrency.terminate();
        env.status = INACTIVE;

        // Rethrow exception.
        throw x;
      }
    }

    if (Constants.DEBUG_ENVIRONMENT) {
      SystemLog.LOG.log(env, "Sent activated environment event");
    }
  }

  /**
   * Terminate the specified environment. Terminating an already
   * inactive or currently terminating environment has no effect.
   *
   * <p>Note that terminating an application does not unload the
   * application. Its state remains loaded within the environment and
   * it can be restarted through the {@link #activate activate}
   * operation. To unload an application, use the {@link #unload
   * unload} operation after terminating the application.</p>
   *
   * @param   requestorId  The ID of the environment requesting this
   *                       operation.
   * @param   envId        The ID of the environment to be terminated.
   * @throws  NullPointerException
   *                       Signals that <code>envId</code> is
   *                       <code>null</code>.
   * @throws  IllegalArgumentException
   *                       Signals that no environment with the
   *                       specified ID exists or that the specified
   *                       environment is the root environment.
   * @throws  IllegalStateException
   *                       Signals that the system is not running, or
   *                       that the specified environment is currently
   *                       being modified.
   * @throws  SecurityException
   *                       Signals that the caller does not have
   *                       permission to manage environments, or that
   *                       the specified environment is not a descendant
   *                       of the specified requesting environment.
   */
  public static void terminate(Guid requestorId, Guid envId) {
    ensurePermission();

    // Basic consistency checks.
    if (null == envId) {
      throw new NullPointerException("Null environment ID");
    } else if (envId.equals(root.id)) {
      throw new IllegalArgumentException("Unable to terminate root " +
                                         "environment (" + root.id + ")");
    }
    
    Environment env;

    synchronized (GLOBAL_LOCK) {
      ensureAlive();
      env = lookup0(envId);
      ensureStable(env);
      if (null != requestorId) {
        ensureLineage(env, lookup0(requestorId));
      }
      if ((INACTIVE == env.status) || (TERMINATING == env.status)) {
        // Done already.
        return;
      } else if (ACTIVE != env.status) {
        throw new Bug("Environment that is being or has been destroyed " +
                      "still part of map of all environments");
      } else {
        env.status = TERMINATING;
      }
    }

    terminate(env);
  }

  /**
   * Terminate the specified environment. The specified environment
   * must have the terminating status.
   *
   * @param   env  The environment to terminate.
   */
  private static void terminate(Environment env) {
    // Tell main event handler about the pending termination.
    Event result = null;
    try {
      ResultHandler    rh = new ResultHandler();
      EnvironmentEvent ee = new
        EnvironmentEvent(rh, null, EnvironmentEvent.STOP, env.id);
      if (env != root) {
        env.main.handleForced(ee);
      } else {
        // For the root environment this call does not go through the
        // concurrency domain. Therefore, we can't use handleForced().
        env.main.handle(ee);
      }
      if (Constants.DEBUG_ENVIRONMENT) {
        SystemLog.LOG.log(env, "Sent stop environment event");
      }
      result = rh.getResult(Constants.SYNCHRONOUS_TIMEOUT);
    } catch (Exception x) {
      SystemLog.LOG.logWarning(env, "Unexpected exception when " +
                               "sending stop environment event", x);
    }
    if ((null != result) &&
        ((! (result instanceof EnvironmentEvent)) ||
         (EnvironmentEvent.STOPPED != ((EnvironmentEvent)result).type))) {
      SystemLog.LOG.logWarning(env, "Unexpected event when terminating (" +
                               result + ")");
    }

    // Terminate concurrency domain. This operation can be performed
    // without holding a monitor because the terminating status
    // prevents anyone from changing the status of the environment.
    env.concurrency.terminate();

    // Adjust status.
    synchronized (GLOBAL_LOCK) {
      if (TERMINATING == env.status) {
        env.status = INACTIVE;
      } else if (DESTROYING == env.status) {
        // While this environment was terminating, somebody else may
        // have been destroying it.
        env.status = DESTROYED;
      } else if (DESTROYED != env.status) {
        throw new Bug("Illegal environment status when terminating (" +
                      env.status + ")");
      }
    }
  }

  /**
   * Terminate this environment. This method only gets called as a result
   * of a stopped event for this environment's request handler. This
   * method is package private to avoid synthetic accessors.
   *
   * @throws  IllegalStateException
   *             Signals that this environment is not in the active,
   *             terminating, or inactive state, or that the system
   *             is not running.
   */
  void terminate() {
    synchronized (GLOBAL_LOCK) {
      ensureAlive();

      // Note that if this environment is currenlty terminating,
      // somebody else is already making sure that it actually
      // terminates.
      if ((INACTIVE == status) || (TERMINATING == status)) {
        return;
      } else if (ACTIVE != status) {
        throw new IllegalStateException("Environment is being or has been " +
                                        "destroyed (" + this + ")");
      }

      status = TERMINATING;
    }

    concurrency.terminate();

    synchronized (GLOBAL_LOCK) {
      if (TERMINATING == status) {
        status = INACTIVE;
      } else if (DESTROYING == status) {
        // While this environment was terminating, somebody else
        // started destroying it.
        status = DESTROYED;
      } else if (DESTROYED != status) {
        throw new Bug("Illegal environment status when terminating (" +
                      status + ")");
      }
    }
  }


  // =======================================================================
  //                          Tearing things down
  // =======================================================================

  /**
   * Unload the code from the protection domain rooted at the
   * specified environment. This method unlinks all exported event
   * handlers from the main and monitor event handlers of the
   * environments in the specified protection domain. It recreates the
   * environments' concurrency domains, thus ensuring that other
   * environments cannot call code in the specified protection domain
   * anymore. It also recreates the environments' protection domain,
   * thus ensuring that future loading operations load all code from
   * scratch.
   *
   * @param   requestorId  The ID of the environment requesting this
   *                       operation.
   * @param   envId        The ID of the environment rooting the
   *                       protection domain to be unloaded.
   * @throws  NullPointerException
   *                       Signals that <code>envId</code> is
   *                       <code>null</code>.
   * @throws  IllegalArgumentException
   *                       Signals that no environment with the
   *                       specified ID exists.
   * @throws  IllegalStateException
   *                       Signals that the system is not running,
   *                       or that an environment in the specified
   *                       protection domain is not in the inactive
   *                       state or is currently being modified.
   * @throws  SecurityException
   *                       Signals that the caller does not have
   *                       permission to manage environments, or that
   *                       the specified environment is not a descendant
   *                       of the specified requesting environment.
   */
  public static void unload(Guid requestorId, Guid envId) {
    ensurePermission();

    // Basic consistency checks.
    if (null == envId) {
      throw new NullPointerException("Null environment ID");
    } else if (envId.equals(root.id)) {
      throw new IllegalArgumentException("Unable to unload root " +
                                         "environment (" + root.id + ")");
    }
    
    Environment env;
    List        list = new ArrayList();

    synchronized (GLOBAL_LOCK) {
      ensureAlive();
      env = lookup0(envId);
      if (null != requestorId) {
        ensureLineage(env, lookup0(requestorId));
      }
      // Make sure the specified environment is the root of its
      // protection domain.
      if (env.protection.equals(env.parent.protection)) {
        throw new IllegalArgumentException("Unable to unload partial " +
                                           "protection doman (" +
                                           env.protection + ")");
      }

      // Collect all environments in the protection domain.
      list.add(env);
      collectProtection(env, list);

      // Make sure all environments are stable and inactive.
      Iterator iter = list.iterator();
      while (iter.hasNext()) {
        env = (Environment)iter.next();
        ensureStable(env);
        if (INACTIVE != env.status) {
          throw new IllegalStateException("Environment to be unloaded not in " +
                                          "inactive state (" + env.id + ")");
        }
      }

      // Unload the environments.
      iter = list.iterator();
      while (iter.hasNext()) {
        env = (Environment)iter.next();
        env.unload(true);
      }
    }
  }

  /**
   * Unload the code from this environment. This method unlinks all
   * exported event handlers from this environment's main and monitor
   * event handlers. It also recreates this environment's concurrency
   * domain, thus ensuring that other environments cannot call the
   * code in this environment anymore. If <code>prot</code> is
   * <code>true</code>, this method also recreates this environment's
   * protection domain. If the environment is the root of the
   * protection domain, this method creates a new one. If it is not,
   * this method copies the parent environment's protection
   * domain. This method must be called while holding the global lock.
   *
   * @param   prot  Flag for whether to also recreate the protection
   *                domain for the environment.
   */
  private void unload(boolean prot) {
    // Unlink all exported event handlers from main.
    main.clear();

    // Unlink all exported event handlers from monitor.
    monitor.clear();

    // Create a new concurrency domain.
    concurrency    = new ConcurrencyDomain(id);

    // Create a new protection domain.
    if (prot) {
      if (protection.id.equals(parent.protection.id)) {
        protection = parent.protection;
      } else {
        protection = new ProtectionDomain(id);
      }
    }
  }

  /**
   * Destroy the specified environment and all its
   * descendants. Destroying an environment that is already being
   * destroyed or that has already been destroyed has no effect.
   *
   * @param   requestorId  The ID of the environment requesting this
   *                       operation.
   * @param   envId        The ID of the environment to be destroyed.
   * @return               <code>true</code> if the environment
   *                       requesting the operation has been destroyed.
   * @throws  NullPointerException
   *                       Signals that <code>envId</code> is
   *                       <code>null</code>.
   * @throws  IllegalArgumentException
   *                       Signals that no environment with the
   *                       specified ID exists or that the specified
   *                       environment is the root environment.
   * @throws  IOException  Signals an exceptional condition when
   *                       deleting the persistent environment state
   *                       for the destroyed environment(s).
   * @throws  IllegalStateException
   *                       Signals that the system has shut down.
   * @throws  SecurityException
   *                       Signals that the caller does not have
   *                       permission to manage environments, or that
   *                       the specified environment is not a descendant
   *                       of the specified requesting environment.
   */
  public static boolean destroy(Guid requestorId, Guid envId)
    throws IOException {

    ensurePermission();

    // Basic consistency checks.
    if (null == envId) {
      throw new NullPointerException("Null environment ID");
    } else if (envId.equals(root.id)) {
      throw new IllegalArgumentException("Unable to destroy root " +
                                         "environment (" + root.id + ")");
    }

    Environment requestor = null;
    Environment parent;
    Environment env;

    // The list of doomed environments and the list of flags whether
    // the corresponding environment needs to be stopped.
    ArrayList     doomed  = new ArrayList();
    ArrayList     stopped = new ArrayList();

    synchronized (GLOBAL_LOCK) {
      ensureAlive();
      env       = lookup0(envId);
      if (null != requestorId) {
        requestor = lookup0(requestorId);
        ensureLineage(env, requestor);
      }

      // Unlink from parent.
      parent = env.parent;

      parent.children.remove(env.name);
      if (0 == parent.children.size()) {
        parent.children = null;
      }

      // Mark subtree rooted at the specified environment as being
      // destroyed.
      markDestroying(env, doomed, stopped);
    }

    // Clean up environments, including stopping environments that
    // need to be stopped.
    int l = doomed.size();
    for (int i=0; i<l; i++) {
      Environment tmp  = (Environment)doomed.get(i);
      boolean     stop = ((Boolean)stopped.get(i)).booleanValue();

      // Release link to parent.
      tmp.parent = null;

      // Tell environment to stop.
      if (stop) {
        Event result = null;
        try {
          ResultHandler    rh = new ResultHandler();
          EnvironmentEvent ee = new
            EnvironmentEvent(rh, null, EnvironmentEvent.STOP, tmp.id);
          tmp.main.handleForced(ee);
          if (Constants.DEBUG_ENVIRONMENT) {
            SystemLog.LOG.log(tmp, "Sent stop environment event");
          }
          result = rh.getResult(Constants.SYNCHRONOUS_TIMEOUT);
        } catch (Exception x) {
          SystemLog.LOG.logWarning(tmp, "Unexpected exception when " +
                                   "sending stop environment event", x);
        }
        if ((null != result) &&
            ((! (result instanceof EnvironmentEvent)) ||
             (EnvironmentEvent.STOPPED != ((EnvironmentEvent)result).type))) {
          SystemLog.LOG.logWarning(tmp, "Unexpected event when destroying (" +
                                   result + ")");
        }

        tmp.concurrency.terminate();
      }
      
      synchronized (GLOBAL_LOCK) {
        // Set status to destroyed.
        tmp.status = DESTROYED;
      }
    }

    // Tell tuple store.
    Guid[] ids = new Guid[l];
    for (int i=0; i<l; i++) {
      ids[i] = ((Environment)doomed.get(i)).id;
    }
    try {
      TupleStore.delete(ids);
    } catch (IOException x) {
      System.out.println("PANIC Unexpected exception while destroying " +
                         "environments");
      System.out.println("PANIC " + x);
      System.exit(-1);
    }

    // Done.
    return (doomed.contains(requestor));
  }

  /**
   * Mark the specified environment and its descendants as being
   * destroyed. This method recursively walks the environment
   * hierarchy starting at the specified environment and collects all
   * environments in the specified list of doomed environments. It
   * sets the status of each environment to <code>DESTROYING</code>,
   * removes the environment from the global set of environments, and
   * collects a flag for whether the environment needs to be stopped.
   * This method must be called while holding the global lock.
   *
   * @param   env      The root of the local environment subtree to
   *                   mark as being destroyed.
   * @param   doomed   The list of doomed environments.
   * @param   stopped  The corresponding list of flags for whether
   *                   the environment needs to be stopped.
   */
  private static void markDestroying(Environment env, List doomed,
                                     List stopped) {

    // Add to list of doomed environments.
    doomed.add(env);
    stopped.add((ACTIVE == env.status)? Boolean.TRUE : Boolean.FALSE);

    // Set status to destroying.
    env.status = DESTROYING;

    // Remove the environment from the global set of environments.
    environments.remove(env.id);

    // Recursively process children.
    if (null != env.children) {
      Iterator iter = env.children.values().iterator();
      while (iter.hasNext()) {
        Environment child = (Environment)iter.next();

        if (DUMMY != child) {
          markDestroying(child, doomed, stopped);
        }
      }
    }
  }


  // =======================================================================
  //                       Environment serialization
  // =======================================================================

  /**
   * Test whether the environment with the specified ID is in the
   * serialization set.
   *
   * @param   id  The ID to test for.
   * @return      <code>true</code> if an environment with the
   *              specified ID is in the serialization set.
   */
  static boolean isInSerializationSet(Guid id) {
    return ((Map)serializationSet.get()).containsKey(id);
  }

  /**
   * Resolve the specified environment ID during serialization.  This
   * method returns the specified ID if the corresponding environment
   * is being check-pointed, or the ID of the new environment if the
   * corresponding environment is being cloned. It returns
   * <code>null</code>, if the corresponding environment is not being
   * serialized.
   *
   * @param   id  The ID of the environment being serialized.
   * @return      The corresponding ID for the serialized form.
   */
  static Guid resolveId(Guid id) {
    return (Guid)((Map)serializationSet.get()).get(id);
  }

  /**
   * Resolve the specified protection domain ID during deserialization.
   *
   * @param   id  The ID of the protection domain being deserialized.
   * @return      The corresponding protection domain.
   * @throws  CheckPointException
   *              Signals that no protection domain with the specified
   *              ID is part of the check-point currently being
   *              deserialized.
   */
  static ProtectionDomain resolveProtection(Guid id) {
    Environment env = (Environment)((Map)serializationSet.get()).get(id);

    if (null == env) {
      // Signal an inconsistent check-point.
      throw new CheckPointException("Undeclared environment in check-point" +
                                    " (" + id + ")");
    } else {
      return env.protection;
    }
  }

  /**
   * Get the root environment's concurrency domain.
   *
   * @return  The root environment's concurrency domain.
   */
  static ConcurrencyDomain getRootConcurrency() {
    return root.concurrency;
  }

  /**
   * Check-point the specified environment and all its
   * descendants. The root environment cannot be check-pointed.
   *
   * @param   requestorId  The ID of the environment requesting
   *                       this operation.
   * @param   envId        The ID of the environment to be
   *                       check-pointed.
   * @return               The timestamp for the check-point.
   * @throws  NullPointerException
   *                       Signals that <code>envId</code> is
   *                       <code>null</code>.
   * @throws  IllegalArgumentException
   *                       Signals that no environment with the
   *                       specified ID exists, that the
   *                       specified environment is the root
   *                       environment, or that the specified
   *                       environment's parent is in the same
   *                       protection domain.
   * @throws  IOException  Signals an exceptional condition while
   *                       actually creating the check-point.
   * @throws  IllegalStateException
   *                       Signals that the system has shut down, that
   *                       the specified environment or one of its
   *                       descendants is currently being modified, or
   *                       that this method could not write to the
   *                       specified environment's tuple store.
   * @throws  SecurityException
   *                       Signals that the caller does not have
   *                       permission to manage environments, or
   *                       that the specified environment is not a
   *                       descendant of the specified requesting
   *                       environment.
   */
  public static long checkPoint(Guid requestorId, Guid envId)
    throws IOException {

    ensurePermission();

    // Basic consistency checks.
    if (null == envId) {
      throw new NullPointerException("Null environment ID");
    } else if (envId.equals(root.id)) {
      throw new IllegalArgumentException("Unable to check-point root " +
                                         "environment (" + root.id + ")");
    }

    Environment env;
    Map         set;                        // The serialization set.
    List        envList;                    // The list of environments.
    Set         restartSet = new HashSet(); // The restart set.
    
    synchronized (GLOBAL_LOCK) {
      ensureAlive();
      env = lookup0(envId);
      if (null != requestorId) {
        ensureLineage(env, lookup0(requestorId));
      }
      if (env.protection.equals(env.parent.protection)) {
        throw new IllegalArgumentException("Unable to check-point partial " +
                                           "protection domain (" +
                                           env.protection + ")");
      }

      // Set up serialization set.
      set = (Map)serializationSet.get();
      set.clear();
      set.put(env.id, env);
      collectDescendants(env, set);

      // Set up list of environments.
      envList = new ArrayList(set.values());

      // Make sure environments are stable.
      Iterator iter = envList.iterator();
      while (iter.hasNext()) {
        Environment tmp = (Environment)iter.next();
        if ((TERMINATING == tmp.status) || (! tmp.stable)) {
          set.clear();
          throw new IllegalStateException("Environment currently being " + 
                                          "modified (" + tmp.id + ")");
        }
      }

      // (1) Mark environments as unstable, (2) replace environments
      // in the serialization set with their IDs, (3) temporarily
      // deactivate active animators, and (4) set up the restart set,
      // i.e., the set of environments whose animators need to be
      // re-activated.
      iter = envList.iterator();
      while (iter.hasNext()) {
        Environment tmp  = (Environment)iter.next();

        // Mark as unstable.
        tmp.stable = false;

        // Replace environment with its ID.
        set.put(tmp.id, tmp.id);

        // Deactivate active animators.
        Animator anim = tmp.concurrency.anim;
        if ((null != anim) && (Animator.ACTIVE == anim.getStatus())) {
          anim.setStatus(Animator.INACTIVE);
          restartSet.add(tmp.id);
        }
      }
    }

    // Create check-point.
    boolean    success = false;
    CheckPoint cp;

    try {
      cp      = env.checkPoint(envList, set, false);
      success = true;
    } finally {
      // Re-activate de-activated animators. This must be done even if
      // the environment is currently being destroyed, so that the
      // environment can correctly process the stop event. We also mark
      // environments as stable again.
      synchronized (GLOBAL_LOCK) {
        Guid dead = null;
        
        Iterator iter = envList.iterator();
        while (iter.hasNext()) {
          Environment tmp = (Environment)iter.next();
          
          // Re-activate.
          if (restartSet.contains(tmp.id)) {
            Animator anim = tmp.concurrency.anim;

            if (null != anim) {
              anim.setStatus(Animator.ACTIVE);
            }
          }
          
          // Mark as stable again.
          tmp.stable = true;
          
          // Remember environments that are being destroyed.
          if ((DESTROYING == tmp.status) || (DESTROYED == tmp.status)) {
            dead = tmp.id;
          }
        }
        
        // Clear serialization set.
        set.clear();

        if (success) {
          if ((SHUTDOWN == globalStatus) || (SHUTTING_DOWN == globalStatus)) {
            throw new IllegalStateException("System shut down while creating " +
                                            "check-point");
          } else if (null != dead) {
            throw new IllegalStateException("Environment (" + dead + ") " +
                                            "destroyed while creating " +
                                            "check-point");
          }
        }
      }
    }

    // Bind tuple store.
    BindingResponse response;
    SioResource     resource = new SioResource();

    resource.type  = SioResource.STORAGE;
    resource.ident = env.id;

    try {
      response = IOUtilities.bind(env.request, resource);
    } catch (Exception x) {
      throw new IllegalStateException("Exception (" + x + ") while binding " +
                                      "tuple store for environment (" +
                                      env.id + ")");
    }
    LeaseMaintainer lm = new LeaseMaintainer(response.lease, response.duration,
                                             ExceptionHandler.HANDLER, null,
                                             root.timer);

    // Write the check-point.
    try {
      IOUtilities.put(response.resource, cp, false);
    } catch (Exception x) {
      throw new IllegalStateException("Exception (" + x + ") while writing " +
                                      "to tuple store for environment (" +
                                      env.id + ")");
    } finally {
      // Cancel the lease.
      lm.cancel();
    }

    // Done.
    return cp.timestamp;
  }

  /**
   * Restore the specified environment and all its descendants from
   * the specified check-point. The root environment cannot be
   * restored.
   *
   * <p>A saved check-point can only be restored if no environments
   * have been added or destroyed in the local environment subtree
   * underneath the specified environment. At the same time, if
   * environments have been moved <i>within</i> the subtree, the
   * check-point can be restored.</p>
   *
   * <p>Note that restoring a check-point does not reload the
   * application's code. To reload an application's code, the
   * application needs to be {@link #terminate terminated} and {@link
   * #unload unloaded} before restoring the check-point.</p>
   *
   * @param   requestorId  The ID of the environment requesting this
   *                       operation.
   * @param   envId        The ID of the environment to be restored.
   * @param   timestamp    The timestamp of the check-point or -1
   *                       if the latest check-point is to be used.
   * @return               <code>true</code> if the environment
   *                       requesting the operation has been restored.
   * @throws  NullPointerException
   *                       Signals that <code>envId</code> is
   *                       <code>null</code>.
   * @throws  IllegalArgumentException
   *                       Signals that no environment with the
   *                       specified ID exists, that the specified
   *                       environment is the root environment, that
   *                       the specified timestamp is negative,
   *                       or that no check-point with the specified
   *                       timestamp exists.
   * @throws  TupleException
   *                       Signals that the requested check-point
   *                       is corrupted or that the local environment
   *                       subtree has changed since the check-point.
   * @throws  ClassNotFoundException
   *                       Signals that a class was not found while
   *                       reading the requested check-point.
   * @throws  IllegalStateException
   *                       Signals that the system has shut down, that
   *                       the specified environment or one of its
   *                       descendants is currently being modified,
   *                       that this method could not read from the
   *                       specified environment's tuple store, or
   *                       that the specified environment or one of
   *                       its descendants is being destroyed while
   *                       restoring the requested check-point.
   * @throws  SecurityException
   *                       Signals that the caller does not have
   *                       permission to manage environments or
   *                       that the specified environment is not
   *                       a descendant of the specified requesting
   *                       environment.
   */
  public static boolean restore(Guid requestorId, Guid envId, long timestamp)
    throws TupleException, ClassNotFoundException {

    ensurePermission();

    // Basic consistency checks.
    if (null == envId) {
      throw new NullPointerException("Null environment ID");
    } else if (envId.equals(root.id)) {
      throw new IllegalArgumentException("Unable to restore root " +
                                         "environment (" + root.id + ")");
    } else if (-1 > timestamp) {
      throw new IllegalArgumentException("Invalid timestamp ("+timestamp+")");
    }

    Environment requestor;
    Environment env;
    CheckPoint  cp = null;

    // Basic security check.
    synchronized (GLOBAL_LOCK) {
      ensureAlive();
      env = lookup0(envId);
      if (null != requestorId) {
        requestor = lookup0(requestorId);
        ensureLineage(env, requestor);
      } else {
        requestor = env;
      }
    }

    // Bind tuple store.
    BindingResponse response;
    SioResource     resource = new SioResource();

    resource.type  = SioResource.STORAGE;
    resource.ident = env.id;

    try {
      response = IOUtilities.bind(env.request, resource);
    } catch (Exception x) {
      throw new IllegalStateException("Exception (" + x + ") while binding " +
                                      "tuple store for environment (" +
                                      env.id + ")");
    }
    LeaseMaintainer lm = new LeaseMaintainer(response.lease, response.duration,
                                             ExceptionHandler.HANDLER, null,
                                             root.timer);

    // Read the check-point.
    Query hasType = new Query("", Query.COMPARE_HAS_TYPE, CheckPoint.class);

    if (-1 == timestamp) {
      // Query for the check-point with the maximum timestamp.
      QueryResponse queryResponse;

      // Perform the query.
      try {
        queryResponse = IOUtilities.query(response.resource, hasType);
      } catch (Exception x) {
        lm.cancel();
        throw new IllegalStateException("Exception (" + x + ") while " +
                                        "accessing tuple store for " +
                                        "environment (" + env.id + ")");
      }
      LeaseMaintainer lm2 = new LeaseMaintainer(queryResponse.lease,
                                                queryResponse.duration,
                                                ExceptionHandler.HANDLER, null,
                                                root.timer);

      // Iterate over the results.
      IteratorElement iter;
      do {
        try {
          iter = IOUtilities.next(queryResponse.iter);
        } catch (NoSuchElementException x) {
          if (null == cp) {
            lm2.cancel();
            lm.cancel();
            throw new IllegalArgumentException("No check-point");
          } else {
            break;
          }
        } catch (Exception x) {
          lm2.cancel();
          lm.cancel();
          throw new IllegalStateException("Exception (" + x + ") while " +
                                          "accessing tuple store for " +
                                          "environment (" + env.id + ")");
        }

        CheckPoint tmp = (CheckPoint)iter.element;
        if ((null == cp) || (cp.timestamp < tmp.timestamp)) {
          cp = tmp;
        }
      } while (iter.hasNext);

      // Cancel all leases.
      lm2.cancel();
      lm.cancel();

    } else {
      // Read the check-point with the specified timestamp.
      Query q = new Query(hasType, Query.BINARY_AND, new
        Query("timestamp", Query.COMPARE_EQUAL, new Long(timestamp)));

      try {
        cp = (CheckPoint)IOUtilities.read(response.resource, q);
      } catch (Exception x) {
        throw new IllegalStateException("Exception (" + x + ") while " +
                                        "accessing tuple store for " +
                                        "environment (" + env.id + ")");
      } finally {
        // Cancel the lease.
        lm.cancel();
      }

      if (null == cp) {
        throw new IllegalArgumentException("No such check-point (" +
                                           timestamp + ")");
      }
    }

    // Validate check-point.
    cp.validate();
    if (! cp.root.equals(env.id)) {
      throw new InvalidTupleException("Check-point's local root (" +
                                      cp.root + ") differs from local root (" +
                                      env.id + ")");
    }

    // Prepare serialization set.
    Map set = (Map)serializationSet.get();
    set.clear();

    // Prepare for restoration.
    synchronized (GLOBAL_LOCK) {
      ensureAlive();
      
      // Collect environments into serialization set.
      set.put(env.id, env);
      collectDescendants(env, set);

      // Make sure the serialization set is the same as the set of
      // check-pointed environments. Also make sure that none of the
      // environments is currently being modified.
      final int size = 1 + cp.descendants.length;
      if (set.size() != size) {
        set.clear();
        throw new InvalidTupleException("Environment hierarchy changed since " +
                                        "check-point (" + cp + ")");
      } else {
        for (int i=0; i<size; i++) {
          Environment tmp = (Environment)set.get((0 == i)? cp.root :
                                                 cp.descendants[i-1]);
          
          if (null == tmp) {
            set.clear();
            throw new InvalidTupleException("Environment hierarchy changed " +
                                            "since check-point (" +
                                            cp + ")");
          } else if ((TERMINATING == tmp.status) || (! tmp.stable)) {
            set.clear();
            throw new IllegalStateException("Environment currently being " +
                                            "modified (" + tmp.id + ")");
          }
        }
      }

      // Mark environments as unstable.
      for (int i=0; i<size; i++) {
        Environment tmp = (Environment)set.get((0 == i)? cp.root :
                                               cp.descendants[i-1]);
        tmp.stable = false;
      }
    }

    // Restore the check-point.
    Environment[] aEnvironment =
      env.restore(cp, set, EnvironmentEvent.RESTORED, null);

    // Determine whether the requesting environment has been restored.
    for (int i=0; i<aEnvironment.length; i++) {
      if (aEnvironment[i].id.equals(requestorId)) {
        return true;
      }
    }

    return false;
  }

  /**
   * Create a check-point for this environment and all its
   * descendants. This method should be called outside the global
   * lock. This environment and all its descendants must have been
   * marked as unstable, so that no other operation can modify the
   * environment(s) during check-point creation. All active
   * environments must be quiescent, i.e., their animators must be in
   * the inactive state.
   *
   * @param   envList      The list of environments for the check-point,
   *                       which must list this environment and all its
   *                       descendants.
   * @param   set          The current serialization set, which must map
   *                       the ID(s) of the environment(s) being
   *                       check-pointed to the target ID(s).
   * @param   clone        The flag for whether the environment(s) are
   *                       being cloned.
   * @return               A check-point for this environment and all its
   *                       descendants.
   * @throws  IOException  Signals an exceptional condition while
   *                       actually writing the check-point.
   */
  CheckPoint checkPoint(List envList, Map set, boolean clone)
    throws IOException {

    // Create list of descendants.
    ArrayList l = new ArrayList(set.keySet());
    l.remove(id);
    
    // Create check-point.
    CheckPoint cp = new CheckPoint(System.currentTimeMillis(),
                                   id,
                                   (Guid[])l.toArray(new Guid[l.size()]));
    
    // Fix up IDs when cloning.
    if (clone) {
      cp.root = (Guid)set.get(cp.root);
      
      for (int i=0; i<cp.descendants.length; i++) {
        cp.descendants[i] = (Guid)set.get(cp.descendants[i]);
      }
    }
    
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    /*
     * The serialized state of the local environment subtree looks
     * as follows:
     *
     * for each environment env
     *    env.protection
     * for each environment env
     *    env.concurrency
     * for each environment env
     *    env
     *    env.status
     *    env.main
     *    env.request
     *    env.monitor
     *    env.timer
     *    env.singleThreaded
     */
    ObjectOutputStream out = new Output(bytes);

    Iterator iter = envList.iterator();
    while (iter.hasNext()) {
      Environment tmp = (Environment)iter.next();
      out.writeObject(tmp.protection);
    }

    iter = envList.iterator();
    while (iter.hasNext()) {
      Environment tmp = (Environment)iter.next();
      out.writeObject(tmp.concurrency);
    }
    
    iter = envList.iterator();
    while (iter.hasNext()) {
      Environment tmp = (Environment)iter.next();
      out.writeObject(tmp);
      out.writeInt(tmp.status);
      out.writeObject(tmp.main);
      out.writeObject(tmp.request);
      out.writeObject(tmp.monitor);
      out.writeObject(tmp.timer);
      out.writeBoolean(tmp.singleThreaded);
    }
    
    out.flush();
    out.close();
    
    cp.state = bytes.toByteArray();

    // Return check-point.
    return cp;
  }

  /**
   * Restore the specified check-point. This method should be called
   * outside the global lock. This environment and all its descendants
   * must have been marked as unstable, so that no other operation
   * can modify the environment(s) during check-point restoration.
   *
   * <p>If <code>flag</code> is
   * <code>EnvironmentEvent.RESTORED</code>, this method fully
   * restores the specified check-point. Otherwise, it only sets up
   * the restored environments, but does not re-activate inactive
   * animators.</p>
   *
   * <p>The specified serialization set is cleared after use. Also,
   * the environments in the serialization set are marked as stable
   * before this method returns (either normally or
   * exceptionally).</p>
   *
   * @param   cp    The check-point to restore.
   * @param   set   The current serialization set, which must map
   *                the ID(s) of the environment(s) being restored
   *                to the actual environment(s).
   * @param   flag  The flag for whether the environments were
   *                restored, moved, or cloned, as defined by
   *                <code>EnvironmentEvent</code>, or 
   *                <code>MOVED_LOCALLY</code> if the restoration
   *                is part of a local move.
   * @param   newParent
   *                If this restoration is done as a local move, the
   *                new parent environment.
   * @return        The list of environments that were restored.
   * @throws  InvalidTupleException
   *                Signals a malformed check-point or that the local
   *                environment subtree has changed since the
   *                check-point.
   * @throws  ClassNotFoundException
   *                Signals that a class was not found while reading
   *                the state of the specified check-point.
   * @throws  IllegalStateException
   *                Signals that this environment or one of its
   *                descendants is being or has been destroyed.
   */
  Environment[] restore(CheckPoint cp, Map set, int flag,
                        Environment newParent)
    throws InvalidTupleException, ClassNotFoundException {

    // Set-up arrays to hold environment state during deserialization.
    final int            size            = 1 + cp.descendants.length;

    Environment[]        aEnvironment    = new Environment[size];
    int[]                aStatus         = new int[size];
    Component.Importer[] aMain           = new Component.Importer[size];
    EventHandler[]       aRequest        = new EventHandler[size];
    Component.Importer[] aMonitor        = new Component.Importer[size];
    Timer[]              aTimer          = new Timer[size];
    boolean[]            aSingleThreaded = new boolean[size];
    ConcurrencyDomain[]  aConcurrency    = new ConcurrencyDomain[size];

    // FIXME: this should really be done in its own, new thread under
    // the watchful eyes of a timer.

    boolean success = false;
    try {
      // Deserialize environment state.
      try {
        ByteArrayInputStream bytes = new ByteArrayInputStream(cp.state);
        ObjectInputStream    in    = new Input(bytes);

        for (int i=0; i<size; i++) {
          // Read the protection domains. There is no need to hang
          // onto them, since they are resolved to the already
          // existing one.
          ProtectionDomain prot = (ProtectionDomain)in.readObject();
        }

        for (int i=0; i<size; i++) {
          aConcurrency[i]    = (ConcurrencyDomain)in.readObject();
        }
        
        for (int i=0; i<size; i++) {
          aEnvironment[i]    = (Environment)in.readObject();
          aStatus[i]         = in.readInt();
          aMain[i]           = (Component.Importer)in.readObject();
          aRequest[i]        = (EventHandler)in.readObject();
          aMonitor[i]        = (Component.Importer)in.readObject();
          aTimer[i]          = (Timer)in.readObject();
          aSingleThreaded[i] = in.readBoolean();
        }
        
        in.close();
      } catch (IOException x) {
        throw new InvalidTupleException("Malformed check-point ("+cp+"): " + x);
      } catch (ClassCastException x) {
        throw new InvalidTupleException("Malformed check-point ("+cp+"): " + x);
      } catch (CheckPointException x) {
        throw new InvalidTupleException("Malformed check-point ("+cp+"): " + x);
      }
    
      // Make sure that the set of read in environments is the same as
      // the one the check-point claims to contain and that each
      // concurrency domain has the ID of its environment.
      for (int i=0; i<size; i++) {
        if (null == set.get(aEnvironment[i].id)) {
          throw new InvalidTupleException("Malformed check-point: Undeclared " +
                                          "environment (" + aEnvironment[i].id +
                                          ") in check-point (" + cp + ")");
        } else if (! aConcurrency[i].id.equals(aEnvironment[i].id)) {
          throw new InvalidTupleException("Malformed check-point: ID of " +
                                          "concurrency domain (" +
                                          aConcurrency[i].id + ") does not " +
                                          " match ID of environment (" +
                                          aEnvironment[i].id +
                                          ") in checkpoint (" + cp + ")");
        }
      }

      // We successfully read in the check-point.
      success = true;

    } finally {
      // We are in the process of throwing an exception; mark
      // environments as stable again.
      if (! success) {
        for (int i=0; i<size; i++) {
          Environment tmp = (Environment)set.get((0 ==i)? cp.root :
                                                 cp.descendants[i-1]);

          tmp.stable = true;
        }
      }

      // Clear serialization set.
      set.clear();
    }
    
    if ((EnvironmentEvent.RESTORED == flag) || (MOVED_LOCALLY == flag)) {
      // Check-point overwrites existing environments.

      // Terminate any running animators.
      for (int i=0; i<size; i++) {
        Environment tmp = aEnvironment[i];
        
        if (ACTIVE == tmp.status) {
          // Tell main event handler about the pending termination.
          Event result = null;
          try {
            ResultHandler    rh = new ResultHandler();
            EnvironmentEvent ee = new
              EnvironmentEvent(rh, null, EnvironmentEvent.STOP, tmp.id);
            tmp.main.handleForced(ee);
            if (Constants.DEBUG_ENVIRONMENT) {
              SystemLog.LOG.log(tmp, "Sent stop environment event");
            }
            result = rh.getResult(Constants.SYNCHRONOUS_TIMEOUT);
          } catch (Exception x) {
            SystemLog.LOG.logWarning(tmp, "Unexpected exception when " +
                                     "sending stop environment event", x);
          }
          if ((null != result) &&
              ((! (result instanceof EnvironmentEvent)) ||
               (EnvironmentEvent.STOPPED != ((EnvironmentEvent)result).type))) {
            SystemLog.LOG.logWarning(tmp,"Unexpected event when terminating (" +
                                     result + ")");
          }
          
          // Terminate concurrency domain. This operation can be performed
          // without holding a monitor because the unstable flag prevents
          // anyone from changing the status of the environment.
          tmp.concurrency.terminate();
        }
      }

      synchronized (GLOBAL_LOCK) {
        // Make sure the system is still running and no environment
        // has been destroyed.
        success = false;

        try {
          ensureAlive();
          for (int i=0; i<size; i++) {
            ensureAlive(aEnvironment[i]);
          }
          success = true;

        } finally {
          if (! success) {
            // Mark environments as stable again and reactivate
            // terminated environments.
            for (int i=0; i<size; i++) {
              Environment tmp = aEnvironment[i];
            
              tmp.stable = true;
            
              if (ACTIVE == tmp.status) {
                tmp.concurrency.animate(tmp.singleThreaded, false);
                // Register animator with controller.
                if (tmp.concurrency.anim instanceof ThreadPool) {
                  controller.add(tmp.concurrency.anim);
                }
                try {
                  tmp.main.handleForced(new
                    EnvironmentEvent(tmp.request, null,
                                     EnvironmentEvent.ACTIVATED, tmp.id));

                  if (Constants.DEBUG_ENVIRONMENT) {
                    SystemLog.LOG.log(tmp, "Sent activated environment event");
                  }

                } catch (NotLinkedException x) {
                  SystemLog.LOG.logWarning(tmp,"main handler not linked when "+
                                           "activating", x);
                  tmp.concurrency.terminate();
                  tmp.status = INACTIVE;
                }
              }
            }
          }
        }
        
        // We are now ready to (1) restore the environments, (2)
        // reactivate deactivated animators, and (3) notify
        // reactivated environments about their restoration. We do
        // this in two separate loops over all environments in the
        // check-point, so that if an environment depends on another
        // environment in the check-point, that other environment is
        // in the same consistent state.
        
        // Restore actual environments.
        for (int i=0; i<size; i++) {
          Environment tmp    = aEnvironment[i];
          
          tmp.status         = aStatus[i];
          tmp.main           = aMain[i];
          tmp.request        = aRequest[i];
          tmp.monitor        = aMonitor[i];
          tmp.timer          = aTimer[i];
          tmp.singleThreaded = aSingleThreaded[i];
          tmp.concurrency    = aConcurrency[i];
          
          // Replace component descriptors.
          tmp.replace(tmp.main, tmp.request, tmp.monitor);

          if (EnvironmentEvent.RESTORED == flag) {
            // Mark as stable again.
            tmp.stable         = true;

            // Enqueue notification of the restoration. It is safe to do
            // inside the global lock, b/c the event is processed by an
            // animator (which is currently inactive).
            if (ACTIVE == tmp.status) {
              try {
                tmp.main.handleForced(new
                  EnvironmentEvent(tmp.request, null, EnvironmentEvent.RESTORED,
                                   tmp.id));
                
                if (Constants.DEBUG_ENVIRONMENT) {
                  SystemLog.LOG.log(tmp, "Sent restored environment event");
                }
                
              } catch (NotLinkedException x) {
                SystemLog.LOG.logError(tmp,
                                       "main handler not linked when restoring",
                                       x);
                tmp.concurrency.terminate();
                tmp.status  = INACTIVE;
              }
            }
          }
        }

        if (EnvironmentEvent.RESTORED == flag) {
          // Reactivate deactivated animators.
          for (int i=0; i<size; i++) {
            if (ACTIVE == aEnvironment[i].status) {
              // Register animator with controller.
              if (aConcurrency[i].anim instanceof ThreadPool) {
                controller.add(aConcurrency[i].anim);
              }
              aConcurrency[i].anim.setStatus(Animator.ACTIVE);
            }
          }
        } else {
          // Remove from old parent.
          this.parent.children.remove(this.name);
          if (0 == this.parent.children.size()) {
            this.parent.children = null;
          }

          // Add to new parent.
          newParent.children.put(this.name, this);
          this.parent = newParent;
        }
      }

    } else {
      // Check-point fills in environments newly created on this node.

      // Restore the environments.
      for (int i=0; i<size; i++) {
        Environment tmp    = aEnvironment[i];
        
        tmp.status         = aStatus[i];
        tmp.main           = aMain[i];
        tmp.request        = aRequest[i];
        tmp.monitor        = aMonitor[i];
        tmp.timer          = aTimer[i];
        tmp.singleThreaded = aSingleThreaded[i];
        tmp.concurrency    = aConcurrency[i];
        
        // Replace component descriptors.
        tmp.replace(tmp.main, tmp.request, tmp.monitor);

        // Newly created environments are stable by default.

        // Enqueue notification of the restoration.
        if (ACTIVE == tmp.status) {
          try {
            tmp.main.handleForced(new
              EnvironmentEvent(tmp.request, null, flag, tmp.id));

            if (Constants.DEBUG_ENVIRONMENT) {
              if (EnvironmentEvent.MOVED == flag) {
                SystemLog.LOG.log(tmp, "Sent moved environment event");
              } else {
                SystemLog.LOG.log(tmp, "Sent cloned environment event");
              }
            }

          } catch (NotLinkedException x) {
            SystemLog.LOG.logError(tmp,
                                   "main handler not linked when restoring",
                                   x);
            tmp.concurrency.terminate();
            tmp.status  = INACTIVE;
          }
        }
      }
    }

    return aEnvironment;
  }


  // =======================================================================
  //                   Global start-up and shut-down
  // =======================================================================

  /**
   * Start up the environment hierarchy. This method performs internal
   * initialization, notably by reading the existing environment
   * hierarchy from tuple storage.
   *
   * @throws  IOException
   *             Signals an exceptional condition while accessing
   *             persistent environment state.
   * @throws  IllegalStateException
   *             Signals that the environment hierarchy has already
   *             been started up or that is has been shut down.
   * @throws  SecurityException
   *             Signals that the caller does not have permission to
   *             start up and shut down the system.
   */
  public static void startUp() throws IOException {
    SecurityManager security = System.getSecurityManager();
    if (null != security) {
      security.checkPermission(SystemPermission.START_UP);
    }

    synchronized (GLOBAL_LOCK) {
      if (RUNNING == globalStatus) {
        throw new IllegalStateException("Environment hierarchy already " +
                                        "running");
      } else if (SHUTTING_DOWN == globalStatus) {
        throw new IllegalStateException("Environment hierarchy shutting down");
      } else if (SHUTDOWN == globalStatus) {
        throw new IllegalStateException("Environment hierarchy has shut down");
      }

      // Initialize root environment.
      Descriptor desc = TupleStore.rootDescriptor();
      if (null == desc) {
        Guid id = new Guid();
        desc    = new Descriptor(id, "/", null, id);
        TupleStore.create(desc);
      }

      root = new Environment(desc.ident, new ProtectionDomain(desc.protection));

      // Patch the core class loader.
      root.protection.loader = root.getClass().getClassLoader();

      // Enter root into environment map.
      environments.put(root.id, root);

      // Fill in all descendants.
      fillInDescendants(root);

      // Set up the animator controllers.
      rootController = new
        Animator.Controller(Constants.ANIMATOR_SCAN_TIME,
                            Constants.ANIMATOR_ROOT_THRESHOLD,
                            Constants.ANIMATOR_ROOT_ADD_THREADS);
      controller     = new
        Animator.Controller(Constants.ANIMATOR_SCAN_TIME,
                            Constants.ANIMATOR_THRESHOLD,
                            Constants.ANIMATOR_ADD_THREADS);

      Thread t = new Thread(rootController, "Animator.Controller for root");
      t.setDaemon(true);
      t.start();

      t = new Thread(controller, "Animator.Controller for environments");
      t.setDaemon(true);
      t.start();

      // Done.
      globalStatus = RUNNING;
    }
  }

  /**
   * Fill in all descendants of the specified environment. This method
   * recursively fills in the environment hierarchy rooted at the
   * specified environment by reading the persistent environment state
   * from storage. This method must be called while holding the global
   * lock.
   *
   * @param   env  The environment whose descendants to fill in.
   */
  private static void fillInDescendants(Environment env) {
    Descriptor[] descriptors = TupleStore.getChildren(env.id);

    for (int i=0; i<descriptors.length; i++) {
      Descriptor  desc  = descriptors[i];
      Environment child = new Environment(desc.ident,
                                          desc.name,
                                          env,
                                          new
                                            ProtectionDomain(desc.protection));
      if (null == env.children) {
        env.children = new HashMap();
      } else if (null != env.children.get(child.name)) {
        throw new Bug("Child environment with duplicate name (" + desc + ")");
      }
      env.children.put(child.name, child);
      environments.put(child.id, child);
      fillInDescendants(child);
    }
  }

  /**
   * Shut down all environments. This method shuts down all
   * environments without notifying anyone of the pending shutdown.
   * It must not be called with a thread that belongs to any
   * concurrency domain in the environment hierarchy.
   *
   * @throws  IllegalStateException
   *             Signals that the environment hierarchy has already
   *             been started up or that is has been shut down.
   * @throws  SecurityException
   *             Signals that the caller does not have permission to
   *             start up and shut down the system.
   */
  public static void shutDown() {
    SecurityManager security = System.getSecurityManager();
    if (null != security) {
      security.checkPermission(SystemPermission.START_UP);
    }

    // The serialization set, used to collect all descendants of the
    // root environment.
    Map set;

    synchronized (GLOBAL_LOCK) {
      if (INITIALIZED == globalStatus) {
        throw new IllegalStateException("Environment hierarchy not yet " +
                                        "running");
      } else if (SHUTTING_DOWN == globalStatus) {
        throw new IllegalStateException("Environment hierarchy already " +
                                        "shutting down");
      } else if (SHUTDOWN == globalStatus) {
        throw new IllegalStateException("Environment hierarchy already " +
                                        "shut down");
      }

      // Set up the serialization set.
      set = (Map)serializationSet.get();
      set.clear();
      collectDescendants(root, set);

      // Mark system as shutting down.
      globalStatus = SHUTTING_DOWN;
    }

    // Shut down all environments but the root environment.
    Iterator iter = set.values().iterator();

    while (iter.hasNext()) {
      Environment env = (Environment)iter.next();

      // Nothing to do for the dummy environment.
      if (DUMMY == env) {
        continue;
      }

      // Terminate the environment.
      synchronized (GLOBAL_LOCK) {
        if ((INACTIVE == env.status) || (TERMINATING == env.status)) {
          // Nothing to shut down.
          continue;
        } else if (ACTIVE != env.status) {
          throw new Bug("Environment that is being or has been destroyed " +
                        "still part of map of all environments (" + env.id +
                        ")");
        }
        env.status = TERMINATING;
      }
      terminate(env);
    }

    // Shut down the root environment as the last environment.
    synchronized (GLOBAL_LOCK) {
      if (ACTIVE != root.status) {
        throw new Bug("Root environment not active during shutdown");
      }
      root.status = TERMINATING;
    }
    terminate(root);

    // We are finished.
    synchronized (GLOBAL_LOCK) {
      globalStatus = SHUTDOWN;
    }
  }


  // =======================================================================
  //                     Nested concurrency domains
  // =======================================================================

  /**
   * Create a new, nested concurrency domain.
   *
   * @see     NestedConcurrencyDomain
   *
   * @param   service  The name of the service associated with the
   *                   newly created nested concurrency domain.
   * @return           A new, nested concurrency domain.
   * @throws  NullPointerException
   *                   Signals that <code>service</code> is
   *                   <code>null</code>.
   * @throws  SecurityException
   *                   Signals that the caller does not have permission
   *                   to manage environments.
   */
  public static NestedConcurrencyDomain
    createNestedConcurrency(String service) {

    ensurePermission();
    
    if (null == service) {
      throw new NullPointerException("Null service");
    }

    return new NestedConcurrencyDomain(new Guid(), service);
  }

  /**
   * Wrap the specified event handler as a wrapped handler for the
   * specified nested concurrency domain. If the specified event
   * handler is not already wrapped, it is wrapped for the specified
   * nested concurrency domain and returned.
   *
   * @see     #createNestedConcurrency(String)
   *
   * @param   handler      The event handler.
   * @param   concurrency  The nested concurrency domain.
   * @return               The wrapped event handler.
   * @throws  NullPointerException
   *                       Signals that <code>handler</code> or
   *                       <code>concurrency</code> is
   *                       <code>null</code>.
   * @throws  IllegalArgumentException
   *                       Signals that <code>handler</code> is
   *                       already wrapped.
   * @throws  IllegalStateException
   *                       Signals that the system is not running.
   * @throws  SecurityException
   *                       Signals that the caller does not have
   *                       permission to manage environments.
   */
  public static EventHandler
    wrapForNested(EventHandler handler, NestedConcurrencyDomain concurrency) {

    ensurePermission();

    // Basic consistency checks.
    if (null == handler) {
      throw new NullPointerException("Null event handler");
    } else if (null == concurrency) {
      throw new NullPointerException("Null concurrency domain");
    } else if (Domain.isWrapped(handler)) {
      throw new IllegalArgumentException("Event handler already wrapped (" +
                                         handler + ")");
    }

    // Make sure that the system is running.
    synchronized (GLOBAL_LOCK) {
      ensureAlive();
    }

    return new Domain.Call(concurrency,      root.protection,
                           root.concurrency, root.protection).wrap(handler);
  }


  // =======================================================================
  //                          Static helpers
  // =======================================================================

  /**
   * Ensure that the specified name is a legal environment name. Legal
   * environment names are between 1 and 64 characters long. They may
   * only contain ASCII letters and numbers as well as the following
   * other characters: '<code>$</code>', '<code>(</code>',
   * '<code>)</code>', '<code>,</code>', '<code>-</code>',
   * '<code>_</code>', and '<code>.</code>'. They must not be
   * "<code>.</code>" or "<code>..</code>". Environment names are case
   * sensitive.
   *
   * @param   name  The name to check.
   * @throws  NullPointerException
   *                Signals that the specified name is <code>null</code>.
   * @throws  IllegalArgumentException
   *                Signals that the specified name is not legal.

   */
  public static void ensureName(final String name) {
    if (null == name) {
      throw new NullPointerException("Null environment name");
    }

    int length = name.length();
    if (0 >= length) {
      throw new IllegalArgumentException("Environment name (" + name +
                                         ") too short");
    } else if (64 < length) {
      throw new IllegalArgumentException("Environment name (" + name +
                                         ") too long");
    } else if (".".equals(name) || "..".equals(name)) {
      throw new IllegalArgumentException("Illegal environment name (" +
                                         name + ")");
    }

    for (int i=0; i<length; i++) {
      char c = name.charAt(i);

      if (! ((('a' <= c) && ('z' >= c)) ||
             (('A' <= c) && ('Z' >= c)) ||
             (('0' <= c) && ('9' >= c)) ||
             ('$' == c) ||
             ('(' == c) ||
             (')' == c) ||
             (',' == c) ||
             ('-' == c) ||
             ('_' == c) ||
             ('.' == c))) {
        throw new IllegalArgumentException("Environment name (" + name +
                                           ") contains illegal character(s)");
      }
    }
  }

  /**
   * Ensure that the caller has the permission to manage environments.
   *
   * @throws  SecurityException
   *             Signals that the caller does not have permission to
   *             manage environments.
   */
  public static void ensurePermission() {
    SecurityManager security = System.getSecurityManager();
    if (null != security) {
      security.checkPermission(SystemPermission.MANAGE_ENV);
    }
  }

  /**
   * Ensure that the system is alive. This method must be called while
   * holding the global lock.
   *
   * @throws  IllegalStateException
   *             Signals that the system is not yet running or
   *             that the system has already shut down.
   */
  static void ensureAlive() {
    if (INITIALIZED == globalStatus) {
      throw new IllegalStateException("System not yet running");
    } else if (SHUTTING_DOWN == globalStatus) {
      throw new IllegalStateException("System shutting down"); 
    } else if (SHUTDOWN == globalStatus) {
      throw new IllegalStateException("System shut down");
    }
  }

  /**
   * Ensure that the specified environment is alive. This method must
   * be called while holding the global lock.
   *
   * @param   env  The environment.
   * @throws  IllegalStateException
   *               Signals that the specified environment is either
   *               currently being destroyed or has been destroyed.
   */
  static void ensureAlive(Environment env) {
    if (DESTROYING == env.status) {
      throw new IllegalStateException("Environment being destroyed (" +
                                      env.id + ")");
    } else if (DESTROYED == env.status) {
      throw new IllegalStateException("Environment has been destroyed (" +
                                      env.id + ")");
    }
  }

  /**
   * Ensure that there is a lineage from the specified ancestor to the
   * specified descendant. This method must be called while holding
   * the global lock. The specified descendant must be inactive,
   * active, or terminating. This method is package private to avoid
   * synthetic accessors.
   *
   * @param   descendant  The descendant environment.
   * @param   ancestor    The ancestor environment.
   * @throws  SecurityException
   *                      Signals that there is no lineage for the
   *                      specified descendant from the specified
   *                      ancestor.
   */
  static void ensureLineage(Environment descendant,
                            Environment ancestor) {
    // While the descendant is not the same as the ancestor and also
    // not the root environment, walk up the environment hierarchy.
    Environment tmp = descendant;

    while ((tmp != ancestor) && (tmp != root)) {
      tmp = tmp.parent;
    }

    if (tmp == ancestor) {
      return;
    } else {
      throw new SecurityException("Requesting environment " +
                                  ancestor.id +
                                  " not an ancestor of operating " +
                                  "environment " + descendant.id);
    }
  }

  /**
   * Ensure that the specified environment is stable. This method
   * must be called while holding the global lock.
   *
   * @param   env  The environment.
   * @throws  IllegalStateException
   *               Signals that the specified environment is unstable.
   */
  static void ensureStable(Environment env) {
    if (! env.stable) {
      throw new IllegalStateException("Environment currently being modified (" +
                                      env.id + ")");
    }
  }

  /**
   * Collect the descendants of the specified environment into the
   * specified set. This method recursively collects all descendants
   * of the specified environment into the specified map, using an
   * environment's ID as the key and the environment itself as the
   * value. This method must be called while holding the global
   * lock.
   *
   * @param  env     The environment, whose descendants to collect.
   * @param  set     The set to collect descendants into.
   */
  static void collectDescendants(Environment env, Map set) {
    if (null != env.children) {
      Iterator iter = env.children.values().iterator();
      while (iter.hasNext()) {
        Environment child = (Environment)iter.next();

        if (DUMMY != child) {
          set.put(child.id, child);
          collectDescendants(child, set);
        }
      }
    }
  }

  /**
   * Collect the descendants of the specified environment into the
   * specified list as long as they are in the same protection domain.
   * This method must be called while holding the global lock.
   * 
   * @param  env   The environment, whose descendants to collect.
   * @param  list  The list to collect descendants into.
   */
  static void collectProtection(Environment env, List list) {
    if (null != env.children) {
      Iterator iter = env.children.values().iterator();
      while (iter.hasNext()) {
        Environment child = (Environment)iter.next();

        if ((DUMMY != child) && env.protection.equals(child.protection)) {
          list.add(child);
          collectProtection(child, list);
        }
      }
    }
  }

  /**
   * Create a local environment subtree from the specified array of
   * environment descriptors. Note that the root of the local
   * environment subtree is not linked in as a child of the specified
   * parent (though, the root's parent is set to the specified
   * parent).
   *
   * @param   parent       The parent environment of the environment
   *                       tree.
   * @param   descriptors  The list of environment descriptors.
   * @param   set          A map, to be filled in just like the
   *                       serialization set when used for
   *                       deserialization.
   * @return               The local root of the environment tree.
   * @throws  IllegalArgumentException
   *                       Signals that the specified list of
   *                       environment descriptors is inconsistent,
   *                       that is, does not describe a well-formed
   *                       tree rooted at the new parent.
   */
  static Environment createLocalTree(Environment  parent,
                                     Descriptor[] descriptors,
                                     Map          set) {
    Environment localRoot = null;

    // Create the environments.
    for (int i=0; i<descriptors.length; i++) {
      Descriptor desc = descriptors[i];
      set.put(desc.ident, new
        Environment(desc.ident, desc.name, null, new
          ProtectionDomain(desc.protection)));
    }

    // Patch up child and parent references.
    for (int i=0; i<descriptors.length; i++) {
      Descriptor  desc = descriptors[i];
      Environment tmp  = (Environment)set.get(desc.ident);
      tmp.parent       = (Environment)set.get(desc.parent);

      if (null == tmp.parent) {
        if (null != localRoot) {
          throw new
            IllegalArgumentException("More than one root for list of " +
                                     "environment descriptors");
        } else if (! desc.parent.equals(parent.id)) {
          throw new
            IllegalArgumentException("Parent ID for root of environment " +
                                     "descriptors (" + desc.parent + ") not " +
                                     "equal to actual parent ID (" +
                                     parent.id + ")");
        }
        tmp.parent = parent;
        localRoot  = tmp;
      } else {
        if (null == tmp.parent.children) {
          tmp.parent.children = new HashMap();
        }
        tmp.parent.children.put(desc.name, tmp);
      }
    }

    if (null == localRoot) {
      throw new IllegalArgumentException("No root for list of environment " +
                                         "descriptors");
    }

    return localRoot;
  }

}
