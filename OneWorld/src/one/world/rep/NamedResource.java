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

package one.world.rep;

import one.world.Constants;
import one.world.core.InvalidTupleException;

/**
 * Implementation of a name for a remote event handler.  The actual event
 * handler which is bound to the name may change over time.
 *
 * @version  $Revision: 1.5 $
 * @author   Janet Davis
 */
public final class NamedResource extends LocalizedResource {

  /** The serial version ID for this class. */
  static final long serialVersionUID = 9061881947929882296L;

  /** The name of the remote event handler. */
  public final String name;

  /** 
   * Constructs a new named resource with the given remote host and name.
   * {@link Constants#REP_PORT} will be used for the port number.
   *
   * @param host        The remote host name.
   * @param name        The name of the remote event handler. 
   */
  public NamedResource(final String host, final String name) {
    this(host, Constants.REP_PORT, name);
  }

  /**
   * Constructs a new named resource with the given remote endpoint and 
   * name.
   *
   * @param host        The remote host name.
   * @param port        The remote port number.
   * @param name        The name of the remote event handler.
   */
  public NamedResource(final String host, final int port,
                       final String name) {
    super(host, port);
    this.name = name; 
  }

  /** 
   * Determines whether this named resource equals the specified object.
   */
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (!(o instanceof NamedResource)) return false;

    NamedResource r = (NamedResource)o;
    return (name.equals(r.name) && host.equals(r.host) && (port == r.port));
  }

  /**
   * Returns a hash code value for this named resource. 
   */
  public int hashCode() {
    return name.hashCode() + host.hashCode() + port;
  }

  /** Returns a string representation of this named resource. */
  public String toString() {
    return "#[named resource " + host + ":" + port + "/" + name + "]";
  }
}
