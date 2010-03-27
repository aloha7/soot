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

import one.world.Constants;
import one.world.data.Chunk;
import one.world.util.SystemUtilities;
import one.util.Bug;
import java.util.ArrayList;

/**
 * Represents a HTTP request/response pair.
 *
 * @author  Daniel Cheah
 * @version $Revision: 1.2 $
 */
public final class Pair extends Ordering {
  /** The HTTP request of this pair. */
  public HttpRequest  req;

  /** The HTTP response of this pair. */
  private HttpResponse res;

  /** The sequence of chunks */
  private ChunkSequence seq;

  /**
   * Constructor.
   */
  public Pair() {
    this(null, null, -1);
  }
  
  /**
   * Constructor.
   *
   * @param req The HTTP request.
   * @param res The HTTP response.
   * @param i   The order of the request.
   */
  public Pair(HttpRequest req, HttpResponse res, int i) {
    super(i);
    this.req         = req;
    this.res         = res;
    this.seq         = null;
  }  

  /**
   * Make a shallow copy of the <code>Pair</code>.
   *
   * @param other The <code>Pair</code> to be cloned.
   */
  public void clone(Pair other) {
    order = other.order;

    req = other.req;
    res = other.res;
    seq = other.seq;
  }

  /**
   * Set the response of this pair.
   *
   * @param res The <code>HttpResponse</code> to set.
   */
  public void setHttpResponse(HttpResponse res) {
    this.res = res;

    // If the body is part of a chunk, then we want to add
    // it to the chunk sequence
    if (this.res.body instanceof Chunk) {
      seq = new ChunkSequence((Chunk)this.res.body);
    } 
  }

  /**
   * Get the response of this pair.
   *
   * @return <code>HttpResponse</code> of this request/response pair.
   */
  public HttpResponse getHttpResponse() {
    return res;
  }

  /**
   * Get the sequence of chunks.
   */
  public ChunkSequence getChunkSequence() {
    return seq;
  }

  /**
   * Is the chunk at the head of the list? 
   *
   * @return True chunk is head of the list, false otherwise.
   */
  public boolean isChunkHead() {
    return (null != seq && seq.isChunkHead());
  }

  /**
   * Set chunk in the underlying sequence.
   *
   * @param chunk Chunk to set.
   */
  public void setChunk(Chunk chunk) {
    if (seq != null) {
      seq.setChunk(chunk);
    }
  }

  /**
   * Next chunk in the sequence. 
   *
   * @return Next chunk in the sequence.
   */
  public Chunk nextChunk() throws ChunkSequence.NoMoreChunksException {
    return seq != null ? seq.nextChunk() : null;
  }
}
