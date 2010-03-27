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

package one.world.env;

import one.util.Guid;

import one.world.core.EventHandler;
import one.world.core.InvalidTupleException;
import one.world.core.TupleException;

import one.world.util.TypedEvent;

/**
 * Implementation of an environment event. Environment events,
 * together with create, load, move, rename, and restore requests as
 * well as check-point responses, define the interaction between
 * components and environments. All environment events, as well as the
 * requests and responses listed above, identify relevant environments
 * by their ID instead of through a direct reference. As a result,
 * environment events can be transmitted over the network.
 *
 * <p>The operations on environments are:<ul>
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
 * <p>Legal operations on environments are limited by the hierarchy of
 * environments. An environment can only see the environments
 * underneath it and it can only perform operations on these
 * environments.</p>
 *
 * @see  AcceptRequest
 * @see  CreateRequest
 * @see  CheckPointResponse
 * @see  LoadRequest
 * @see  MoveRequest
 * @see  RenameRequest
 * @see  RestoreRequest
 *
 * @version  $Revision: 1.11 $
 * @author   Robert Grimm
 */
public final class EnvironmentEvent extends TypedEvent {

  /**
   * The type code for a created environment. Created events confirm a
   * created environment and are sent to the source of the
   * corresponding create request.
   *
   * @see  CreateRequest
   */
  public static final int CREATED               =  1;

  /**
   * The type code for a loaded environment. Loaded events confirm
   * that an initializer has been loaded into an environment and are
   * sent to the source of the corresponding load request.
   *
   * @see  LoadRequest
   */
  public static final int LOADED                =  2;

  /**
   * The type code for activating an environment. Activate events
   * request to activate an environment and are sent to an
   * environment's request handler.
   *
   * @see  #ACTIVATED
   * @see  one.world.core.Environment#activate(Guid,Guid)
   */
  public static final int ACTIVATE              =  3;

  /**
   * The type code for an activated environment. Activated events
   * confirm the activation of an environment and are sent to the main
   * event handler of the environment that has been activated as well
   * as to the source of the corresponding activate event. Note that
   * environments can be implicitly activated as a result of a create
   * request.
   *
   * @see  #ACTIVATE
   */
  public static final int ACTIVATED             =  4;

  /**
   * The type code for check-pointing an environment. Check-point
   * events request to check-point an environment and all its
   * descendants and are sent to an environment's request handler.
   *
   * @see  CheckPointResponse
   * @see  one.world.core.Environment#checkPoint(Guid,Guid)
   */
  public static final int CHECK_POINT           =  5;

  /**
   * The type code for a restored environment. Restored events confirm
   * the restoration of an environment from a previously stored
   * check-point and are sent to the source of the corresponding
   * restore request as well as to all active environments that have
   * been restored from the check-point.
   *
   * @see  RestoreRequest
   */
  public static final int RESTORED              =  6;

  /**
   * The type code for a moved environment. Moved events confirm that
   * an environment and all its descendants have been moved and are
   * sent to the source of the corresponding move request. They are
   * also sent to all active environments that have been moved, if they
   * have been moved to a new node.
   *
   * @see  MoveRequest
   */
  public static final int MOVED                 =  7;

  /**
   * The type code for a cloned environment. Cloned events notify an
   * environment that it has been cloned from another and are sent to
   * all active environments that were newly generated as part of a
   * cloning move operation.
   *
   * @see MoveRequest
   */
  public static final int CLONED                =  8;

  /**
   * The type code for a renamed environment. Renamed events confirm
   * the renaming of an environment and are sent to the source of the
   * corresponding rename request.
   *
   * @see  RenameRequest
   */
  public static final int RENAMED               =  9;

  /**
   * The type code for terminating an environment. Terminate events
   * request that an environment be terminated and are sent an
   * environment's request handler.
   *
   * @see  #TERMINATED
   * @see  one.world.core.Environment#terminate(Guid,Guid)
   */
  public static final int TERMINATE             = 10;

  /**
   * The type code for a terminated environment. Terminated events
   * confirm that an environment has been terminated and are sent to
   * the source of the corresponding terminate event. If an environment
   * requested to terminate itself, no terminated event is posted.
   *
   * @see  #TERMINATE
   */
  public static final int TERMINATED            = 11;

  /**
   * The type code for unloading code from an environment. Unload
   * events request that an environment unlink its imported main
   * and monitor handlers and are sent to an environment's request
   * handler.
   *
   * @see  #UNLOADED
   * @see  one.world.core.Environment#unload(Guid,Guid)
   */
  public static final int UNLOAD                = 12;

  /**
   * The type code for an unloaded environment. Unloaded events
   * confirm that an environment has unloaded its code and are sent
   * to the source of the corresponding unload event.
   */
  public static final int UNLOADED              = 13;

  /**
   * The type code for destroying an environment. Destroy events
   * request that an environment and all its descendants be destroyed
   * and are sent to an environment's request handler.
   *
   * @see  #DESTROYED
   * @see  one.world.core.Environment#destroy(Guid,Guid)
   */
  public static final int DESTROY               = 14;

  /**
   * The type code for a destroyed environment. Destroyed events
   * confirm that an environment and all its descendants have been
   * destroyed and are sent to the source of the corresponding destroy
   * event. If the source is part of the destroyed environment
   * hierarchy, no destroyed event is posted.
   */
  public static final int DESTROYED             = 15;

  /**
   * The type code for stopping an environment. Stop events notify an
   * application running in an environment that is about to be
   * terminated or destroyed of the pending doom and are sent to that
   * environment's main event handler and therefore to the component
   * linked to the environment's main event handler. In response to a
   * stop event, an application needs to stop all computations and
   * release all resources.
   *
   * @see  #STOPPED
   */
  public static final int STOP                  = 16;

  /**
   * The type code for a stopped environment. Stopped events confirm
   * that an environment is ready to be terminated or destroyed and
   * are sent to the source of the corresponding stop event. If an
   * application posts a stopped event to the environment's request
   * handler without first receiving a stop event, it is terminated
   * right away.
   *
   * @see #STOP
   */
  public static final int STOPPED               = 17;

  /**
   * The ID of the environment for this environment event.
   *
   * @serial  Must not be <code>null</code>.
   */
  public Guid ident;

  /** Create a new, empty environment event. */
  public EnvironmentEvent() {
    // Nothing to do.
  }

  /**
   * Create a new environment event.
   *
   * @param   source      The source for the new environment event.
   * @param   closure     The closure for the new environment event.
   * @param   type        The type for the new environment event.
   * @param   ident       The ID for the new environment event.
   */
  public EnvironmentEvent(EventHandler source, Object closure,
                          int type, Guid ident) {
    super(source, closure, type);
    this.ident = ident;
  }

  /** Validate this environment event. */
  public void validate() throws TupleException {
    super.validate();
    if ((CREATED > type) || (STOPPED < type)) {
      throw new InvalidTupleException("Invalid type (" + type +
                                      ") for environment event (" + this + ")");
    } else if (null == id) {
      throw new 
        InvalidTupleException("Null environment ID for environment event (" +
                              this + ")");
    }
  }

  /** Get a string representation of this environment event. */
  public String toString() {
    StringBuffer buf = new StringBuffer();

    buf.append("#[");
    switch (type) {
    case CREATED:
      buf.append("created");
      break;
    case LOADED:
      buf.append("loaded");
      break;
    case ACTIVATE:
      buf.append("activate");
      break;
    case ACTIVATED:
      buf.append("activated");
      break;
    case CHECK_POINT:
      buf.append("check-point");
      break;
    case RESTORED:
      buf.append("restored");
      break;
    case MOVED:
      buf.append("moved");
      break;
    case CLONED:
      buf.append("cloned");
      break;
    case RENAMED:
      buf.append("renamed");
      break;
    case TERMINATE:
      buf.append("terminate");
      break;
    case TERMINATED:
      buf.append("terminated");
      break;
    case UNLOAD:
      buf.append("unload");
      break;
    case UNLOADED:
      buf.append("unloaded");
      break;
    case DESTROY:
      buf.append("destroy");
      break;
    case DESTROYED:
      buf.append("destroyed");
      break;
    case STOP:
      buf.append("stop");
      break;
    case STOPPED:
      buf.append("stopped");
      break;
    default:
      buf.append(type);
      break;
    }
    buf.append(" env ");
    buf.append(ident);
    buf.append(']');

    return buf.toString();
  }

}
