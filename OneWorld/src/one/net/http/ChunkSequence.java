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

/**
 * Maintains a sequence of chunks. Ordering of chunks is 
 * time of insertion. This is mainly used by the
 * HttpServer and Pair class.
 *
 * @author  Daniel Cheah
 * @version $Revision: 1.1 $
 */
public final class ChunkSequence {
  /** Chunks of the body */
  private Chunk[] chunks;

  /** Total number of chunks in the sequence */
  private final int totalChunks;

  /** Current chunk being read */
  private int readChunk;

  /** Current chunk being written */
  private int writeChunk;

  /**
   * Constructor.
   *
   * @param head This chunk should be the head of the list.
   */
  public ChunkSequence(Chunk head) {
    if (null != head.previous) {
      throw new Bug("ChunkSequence must be constructed with the chunk head");
    }
    
    // Store total number of chunks
    totalChunks  = calcNumChunks(head);

    // Create array big enough to store all the chunks
    chunks       = new Chunk[totalChunks];
    chunks[0]    = head;

    // Pointer to the current chunk
    readChunk  = 0;
    writeChunk = 1;
  }

  /** 
   * Is the next chunk to be read the head?
   *
   * @return True if the next chunk is the head.
   */
  public synchronized boolean isChunkHead() {
    return (0 == readChunk);
  }

  /**
   * Set the next chunk in the sequence.
   *
   * @param chunk Chunk to be set.
   */
  public synchronized void setChunk(Chunk chunk) {
    if (null == chunks) {
      throw new Bug("Chunk array is null");
    }

    if (null != chunks[writeChunk]) {
      throw new Bug("Chunk is not null");
    }

    chunks[writeChunk] = chunk;

    ++writeChunk;
  }

  /**
   * Get the next unread chunk in the sequence.
   *
   * @return Next chunk in sequence, null if chunk has not been set yet.
   * @exception NoMoreChunksException Signals nore more chunks in the sequence.
   */
  public synchronized Chunk nextChunk() throws NoMoreChunksException {
    if (null == chunks) {
      throw new Bug("Chunk array is null");
    }

    if (readChunk >= totalChunks) {
      throw new NoMoreChunksException();
    }

    // Chunk to return
    Chunk c = chunks[readChunk];

    if (null != c) {
      // Set the current chunk to be empty
      chunks[readChunk] = null;

      // Next chunk
      ++readChunk;
    }

    return c;
  }

  private static int calcNumChunks(Chunk c) {
    if (-1 == c.length) {
      return 0;
    }

    // This might not always be true. Right now
    // only the last tuple may be a different size.
    // It may be in the future chunks are of different
    // sizes.
    long l = c.length / c.data.length;
    if ((c.length % Constants.CHUNKING_THRESHOLD) > 0) {
      ++l;
    }

    return (int)l;
  }

  /**
   * Signals that all chunks have been read.
   *
   * @author  Daniel Cheah
   */
  public static final class NoMoreChunksException extends Exception {
    /**
     * Constructor.
     */
    public NoMoreChunksException() {
      super();
    }

    /**
     * Constructor.
     *
     * @param s The message for this exception.
     */
    public NoMoreChunksException(String s) {
      super(s);
    }
  }
}

