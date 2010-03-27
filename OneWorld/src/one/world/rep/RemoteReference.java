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

import one.world.core.InvalidTupleException;
import one.util.Guid;

/**
 * Implementation of a reference to a remote event handler.  Remote
 * references are used to name a specific instance of an event handler on
 * a remote host.
 *
 * @version  $Revision: 1.9 $
 * @author   Janet Davis
 */
public final class RemoteReference extends LocalizedResource {

  /** The serial version ID for this class. */
  static final long serialVersionUID = 5736432688752350968L;

  /** The ID of the remote event handler. */
  public final Guid id;

  /**
   * Constructs a new remote reference with the given remote endpoint and
   * event handler.
   *
   * @param host        The remote host name.
   * @param port        The remote port number.
   * @param id          The ID of the remote event handler.
   */
  public RemoteReference(final String host, final int port, final Guid id) {
    super(host, port);
    this.id = id; 
  }

  /** 
   * Determines whether this remote reference equals the specified object.
   */
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (!(o instanceof RemoteReference)) return false;

    RemoteReference r = (RemoteReference)o;
    return id.equals(r.id) && host.equals(r.host) && (port == r.port);
  }

  /**
   * Returns a hash code value for this remote reference. 
   */
  public int hashCode() {
    return id.hashCode() + host.hashCode() + port;
  }

  /** Return a string representation for this remote reference. */
  public String toString() {
    return "#[remote reference " + host + ":" + port + "/" + id + "]";
  }

}
