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
import one.world.core.Tuple;
import one.world.core.ExceptionalEvent;
import one.world.util.AbstractHandler;
import one.world.io.InputResponse;
import one.world.io.ListenResponse;
import one.world.data.Chunk;
import one.util.Bug;

/**
 * Handler for taking care of chunk responses.
 *
 * @author  Daniel Cheah
 * @version $Revision: 1.1 $
 */
public final class ChunkResponder extends ProxyHandler {
  /** Tuple store proxy used */
  private final TupleStoreProxy tsProxy;
  
  /**
   * Constructor.
   *
   * @param tsProxy Set the tuple store proxy to be used.
   */
  public ChunkResponder(TupleStoreProxy tsProxy) {
    super();

    this.tsProxy = tsProxy;
  }

  /**
   * The tuple store proxy calls this method
   * to handle the relevant response
   * from the tuple store. The underlying 
   * handler properly responds to the 
   * initial request.
   *
   * @param e Event received by proxy from the tuple store.
   */
  public boolean proxyHandle(Event e) {
    return handle1(e);
  }

  /** Handle the specified event. */
  protected boolean handle1(Event e) {
    if (e instanceof InputResponse) {
      handleInputResponse((InputResponse)e);
      return true;

    } else if (e instanceof ExceptionalEvent) {
      handleExceptionalEvent((ExceptionalEvent)e);
      return true;

    } else {
      return false;

    }
  }

  /** Handle InputResponse. */
  private void handleInputResponse(InputResponse in) {
    // TODO: add hints to closure
    // To tell what to do when we have more chunks
    TupleStoreProxy.Closure c   = (TupleStoreProxy.Closure)in.closure;
    ChunkRequest            req = (ChunkRequest)c.request;
    ChunkResponse           res;

    if (in.tuple instanceof Chunk) {
      // We have a chunk
      Chunk chunkData = (Chunk)in.tuple;

      // respond to the chunk request
      res = new ChunkResponse(null, null, chunkData);

    } else {
      // We have a standard tuple
      throw new Bug("ChunkResponder did not receive Chunk");
    }

    if (false /*&& req.hint*/) {
      // TODO: Do read ahead optimization with hints from the
      // request object.
    }

    respond(req, res);
  }

  /** Handle ExceptionalEvent. */
  private void handleExceptionalEvent(ExceptionalEvent ee) {
    TupleStoreProxy.Closure c   = (TupleStoreProxy.Closure)ee.closure;
    ChunkRequest            req = (ChunkRequest)c.request;

    respond(req, ee.x);
  }
}
