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

package one.world.util;

import one.world.core.SymbolicHandler;

/**
 * Implementation of a null handler. A null handler is an event
 * handler that does not accept any events. When check-pointing or
 * moving environments, all event handlers provided by components
 * outside the environments currently being check-pointed or moved are
 * replaced by a reference to the canonical null handler. The
 * canonical null handler is used instead of a <code>null</code>
 * value, because events with a <code>null</code> source do not
 * validate.
 *
 * @version  $Revision: 1.3 $
 * @author   Robert Grimm
 */
public final class NullHandler extends SymbolicHandler {

  /** The serial version ID for this class. */
  static final long serialVersionUID = -8227742583641904659L;  

  /** The canonical null handler. */
  public static final NullHandler NULL = new NullHandler();

  /** Create a new null handler. */
  private NullHandler() {
    // Nothing to do.
  }

  /** Resolve this null handler. */
  private Object readResolve() throws java.io.ObjectStreamException {
    return NULL;
  }

}
