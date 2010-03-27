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

import one.world.core.Event;
import one.world.core.ExceptionalEvent;
import one.world.util.AbstractHandler;
import one.world.env.EnvironmentEvent;

/**
 * Environment Handler. Help to simplify handlers
 * that handle various <code>EnvironmentEvent</code>.
 */
public class EnvironmentHandler extends AbstractHandler {
  /** Handle the specified event. */
  protected boolean handle1(Event e) {
    if (e instanceof EnvironmentEvent) {
      EnvironmentEvent ee = (EnvironmentEvent)e;

      switch (ee.type) {
        case EnvironmentEvent.ACTIVATED:
          return handleActivated(ee);

        case EnvironmentEvent.STOPPED:
          return handleStopped(ee);

        default:
          return handleEnvDefault(ee);
      }
    } 
    
    if (e instanceof ExceptionalEvent) {
      return handleExceptionalEvent((ExceptionalEvent)e);
    }

    return genericHandle(e);
  }

  /** Handle ACTIVATED environment events. */
  protected boolean handleActivated(EnvironmentEvent ee) {
    return false;
  }

  /** Handle STOPPED environment events. */
  protected boolean handleStopped(EnvironmentEvent ee) {
    return false;
  }

  /** Handle other type of environment events. */
  protected boolean handleEnvDefault(EnvironmentEvent ee) {
    return false;
  }

  /** Handle exceptional events. */
  protected boolean handleExceptionalEvent(ExceptionalEvent ee) {
    return false;
  }

  /** Handle other events. */
  protected boolean genericHandle(Event e) {
    return false;
  }
}
