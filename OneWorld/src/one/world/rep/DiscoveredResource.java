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
import one.world.core.SymbolicHandler;
import one.world.io.Query;

/**
 * Implementation of a discovered resource.  A discovered resource
 * contains a query to be resolved by a discovery service and a
 * multicast flag indicating that the resource includes all matches to
 * the query.
 *
 * @version  $Revision: 1.8 $
 * @author Janet Davis 
 */
public class DiscoveredResource extends SymbolicHandler {

  /** The serial version ID for this class. */
  static final long serialVersionUID = -7317467970657236353L;

  /** The query that describes the desired resource. */
  public final Query query;

  /** 
   * Indicates that the query should be resolved to all matching
   * resources.  For early binding, all matching resources will be
   * returned. For late binding, the event will be routed to all
   * matching resources.  
   */
  public final boolean matchAll;

  /**
   * Constructs a new unicast discovered resource with the given query.
   * The {@link #matchAll} flag will be <code>false</code>.
   *
   * @param query   The query that describes the desired resource.
   */
  public DiscoveredResource(final Query query) {
    super();
    this.query = query;
    this.matchAll = false;
  }

  /**
   * Constructs a new discovered resource with the given query and
   * multicast mode.
   *
   * @param  query  The query that describes the desired resource.  
   * @param  matchAll  The multicast mode. 
   */
  public DiscoveredResource(final Query query, final boolean matchAll) {
    super();
    this.query = query;
    this.matchAll = matchAll;
  } 

  /** 
   * Determines whether this discovered resource equals the specified 
   * object.
   */
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (!(o instanceof DiscoveredResource)) return false;

    DiscoveredResource r = (DiscoveredResource)o;
    return (query.equals(r.query) && (matchAll == r.matchAll));
  }

  /**
   * Returns a hash code value for this discovered resource.
   */
  public int hashCode() {
    return query.hashCode() + (matchAll ? 1 : 0);
  }

  /** Returns a string representation of this discovered resource. */
  public String toString() {
    return "#[discovered resource " + query + "]";
  }
}
