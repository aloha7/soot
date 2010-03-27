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

import one.util.Guid;

/**
 * Implementation of a nested concurrency domain. Nested concurrency
 * domains are used within the root environment to isolate some
 * services such as {@link one.world.io.NetworkIO structured I/O
 * communication channels} from the root environment's main
 * concurrency domain. Their use avoids breaking the root environment
 * into several environments and thus introduces a form of lightweight
 * environment.
 *
 * <p>Note that applications can also be partitioned across several
 * concurrency domains. However, they need to use several different
 * environments, which are all part of the same protection domain.</p>
 *
 * @version  $Revision: 1.1 $
 * @author   Robert Grimm 
 */
public final class NestedConcurrencyDomain extends ConcurrencyDomain {

  /**
   * The name of the service associated with this nested concurrency
   * domain.
   *
   * @serial  Must not be <code>null</code>.
   */
  String service;

  /**
   * Create a new nested concurrency domain. The specified ID must be
   * a fresh ID that is not used for any environment.
   *
   * @param   id       The ID for the newly created nested concurrency
   *                   domain.
   * @param   service  The name of the service associated with this
   *                   nested concurrency domain.
   */
  NestedConcurrencyDomain(Guid id, String service) {
    super(id);
    this.service = service;
  }
  
  /** Return a string representation for this nested concurrency domain. */
  public String toString() {
    return "#[Nested concurrency domain " + id.toString() + " for " + 
      service + "]";
  }

}
