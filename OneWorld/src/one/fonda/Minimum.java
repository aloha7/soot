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

package one.fonda;

import one.world.core.Component;
import one.world.core.ComponentDescriptor;
import one.world.core.Environment;
import one.world.core.Event;
import one.world.core.EventHandler;
import one.world.core.ExceptionalEvent;
import one.world.core.ExportedDescriptor;
import one.world.core.ImportedDescriptor;
import one.world.core.Tuple;
import one.world.core.TupleException;
import one.world.core.UnknownEventException;

import one.world.env.EnvironmentEvent;

import one.world.util.SystemUtilities;

/**
 * Implementation of a minimal main application component. This
 * component is the smallest main application component possible. It
 * only processes the required environment events and does no other
 * useful work. Its main purpose is for debugging and measuring
 * <i>one.world</i>.
 *
 * <p><b>Imported and Exported Event Handlers</b></p>
 *
 * <p>Exported event handler(s):<dl>
 *    <dt>main</dt>
 *    <dd>Handles environment events.
 *        </dd>
 * </dl></p>
 *
 * @version  $Revision: 1.4 $
 * @author   Robert Grimm
 */
public final class Minimum extends Component implements EventHandler {

  // =======================================================================
  //                           Descriptors
  // =======================================================================

  /** The component descriptor. */
  private static final ComponentDescriptor SELF =
    new ComponentDescriptor("one.world.util.Minimum",
                            "A minimal main application component",
                            true);

  /** The exported event handler descriptor for the request handler. */
  private static final ExportedDescriptor MAIN =
    new ExportedDescriptor("main",
                           "The main event handler",
                           new Class[] { EnvironmentEvent.class },
                           null,
                           false);


  // =======================================================================
  //                           Constructor
  // =======================================================================

  /**
   * Create a new instance of <code>Minimum</code>.
   *
   * @param  env  The environment for the new instance.
   */
  public Minimum(Environment env) {
    super(env);
    declareExported(MAIN, this);
  }


  // =======================================================================
  //                           Component support
  // =======================================================================

  /** Get the component descriptor. */
  public ComponentDescriptor getDescriptor() {
    return (ComponentDescriptor)SELF.clone();
  }

  // =======================================================================
  //                         The main event handler
  // =======================================================================

  /**
   * Handle the specified event. This component does not implement the
   * main event handler as an inner class, because inner classes
   * introduce additional serialized state, which is inconsistent with
   * the goal of providing the smallest possible main component.
   */
  public void handle(Event e) {
    // Validate the event.
    try {
      e.validate();
    } catch (TupleException x) {
      respond(e, x);
      return;
    }

    if (e instanceof EnvironmentEvent) {
      EnvironmentEvent ee = (EnvironmentEvent)e;

      if ((EnvironmentEvent.ACTIVATED == ee.type) ||
          (EnvironmentEvent.RESTORED == ee.type) ||
          (EnvironmentEvent.MOVED    == ee.type)) {

        return;

      } else if (EnvironmentEvent.STOP == ee.type) {
        respond(e, new
                EnvironmentEvent(this, null, EnvironmentEvent.STOPPED,
                                 getEnvironment().getId()));
        return;
      }

    } else if (e instanceof ExceptionalEvent) {
      SystemUtilities.debug("Unexpected exceptional event (" +
                            ((ExceptionalEvent)e).x + ") in " + this);
      return;
    }

    // We don't know about this event.
    respond(e, new UnknownEventException(e.toString()));
  }

  /**
   * Send the specified response to the source of the specified
   * request. Before sending the specified response, this method sets
   * the response's closure to the closure of the request. This code
   * has been copied from <code>AbstractHandler</code>.
   *
   * @param   request   The request to whose source to respond to.
   * @param   response  The response.
   * @throws  NullPointerException
   *                    Signals that either of the specified
   *                    events is <code>null</code>.
   */
  private void respond(Event request, Event response) {
    if (null == request) {
      throw new NullPointerException("Null request");

    } else if (null == response) {
      throw new NullPointerException("Null repsonse");

    } else if (null == request.source) {
      // No-one to respond to.
      SystemUtilities.debug("Null source handler ("+request+") in "+this);

    } else {
      // Fix response's closure and send.
      response.closure = request.closure;
      request.source.handle(response);
    }
  }

  /**
   * Send an exceptional event to the source of the specified request.
   * This method creates a new exceptional event for the specified
   * throwable and sends it to the source of the specified request.
   * The source of the new exceptional event is this event handler and
   * the closure is the request's closure. This method has been
   * copied from <code>AbstractHandler</code>.
   *
   * @see     ExceptionalEvent
   *
   * @param   request   The request to whose source to respond to.
   * @param   x         The throwable for the new exceptional event.
   * @throws  NullPointerException
   *                    Signals that either <code>request</code> or
   *                    <code>x</code> is <code>null</code>.
   */
  private void respond(Event request, Throwable x) {
    if (null == x) {
      throw new NullPointerException("Null throwable");
    }

    respond(request, new ExceptionalEvent(this, null, x));
  }


  // =======================================================================
  //                              Initializer
  // =======================================================================

  /**
   * Initialize the minimal application component.
   *
   * @param   env      The environment.
   * @param   closure  The closure, which is ignored.
   */
  public static void init(Environment env, Object closure) {
    Minimum min = new Minimum(env);

    env.link("main", "main", min);
  }

}
