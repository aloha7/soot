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
import one.util.Guid;
import one.world.core.EventHandler;
import one.world.core.InvalidTupleException;
import one.world.core.TupleException;

/**
 * Representation of a chunk request.
 *
 * @author  Daniel Cheah
 * @version $Revision: 1.1 $
 */
public final class ChunkRequest extends Event {

  /** Guid of the chunk we want */
  public Guid   chunkGuid;

  /** Environment where the chunk is stored */
  public String uri;

  /**
   * Constructor.
   */
  public ChunkRequest() {
    super();
  }

  /** 
   * Constructor.
   *
   * @param source  The source for this request.
   * @param closure The closure for this request.
   */
  public ChunkRequest(EventHandler source, Object closure) {
    super(source, closure);
  }

  /** 
   * Constructor.
   *
   * @param source    The source for this request.
   * @param closure   The closure for this request.
   * @param uri       The uri of the chunk.
   * @param chunkGuid The requested chunk guid.
   */
  public ChunkRequest(EventHandler source, Object closure, 
                      String uri, Guid chunkGuid) {
    super(source, closure);
    this.uri       = uri;
    this.chunkGuid = chunkGuid;
  }

  /**
   * Validate this request.
   *
   * @exception TupleException Signals that the request is invalid.
   */
  public void validate() throws TupleException {
    super.validate();

    if (null == uri || null == chunkGuid ) {
      throw new InvalidTupleException();
    }
  }
}
