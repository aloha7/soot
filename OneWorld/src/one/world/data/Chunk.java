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

package one.world.data;

import one.util.Guid;

/**
 * Implementation of a chunk. A chunk holds part of a large, arbitrary
 * byte array. The entire byte array can be accessed by following the
 * {@link #previous} and {@link #next} symbolic references (which form
 * a doubly-linked list). The name and type of each chunk in the list
 * of chunks must be the same. The data array of each chunk may be of
 * any size, including 0 (making it possible to store the head of a
 * list of chunks after all chunks have been stored and the total
 * length is actually known). However, by convention, the data for
 * every chunk in the list of chunks should be of size {@link
 * one.world.Constants#CHUNKING_THRESHOLD}, with exception of the last
 * chunk, which holds the rest of the data and therefore may be
 * smaller than the <code>CHUNKING_THRESHOLD</code>.
 *
 * @version  $Revision: 1.2 $
 * @author   Robert Grimm 
 */
public class Chunk extends BinaryData {

  /** The serial version ID for this class. */
  static final long serialVersionUID = 8815137729444711906L;

  /**
   * The previous chunk or <code>null</code> if this chunk is the
   * first chunk in the list of chunks.
   *
   * @serial
   */
  public Guid previous;

  /**
   * The next chunk or <code>null</code> if this chunk is the last
   * chunk in the list of chunks.
   *
   * @serial
   */
  public Guid next;

  /**
   * The total length of the binary data in the list of chunks.  This
   * field may be -1 if this chunk is not the first chunk in the list
   * of chunks.
   *
   * @serial  Must be the sum of the lengths of the data arrays
   *          in the list of chunks.
   */
  public long length;

  /** Create a new, empty chunk. */
  public Chunk() {
    // Nothing to do.
  }

  /**
   * Create a new chunk.
   *
   * @param  name      The name for the new chunk.
   * @param  type      The MIME type for the new chunk.
   * @param  data      The data for the new chunk, which is
   *                   <i>not</i> copied.
   * @param  previous  The ID of the previous chunk in the list of
   *                   chunks.
   * @param  next      The ID of the next chunk in the list of chunks.
   */
  public Chunk(String name, String type, byte[] data,
               Guid previous, Guid next, long length) {
    super(name, type, data);
    this.previous = previous;
    this.next     = next;
    this.length   = length;
  } 

  /**
   * Create a new chunk.
   *
   * @param  id        The ID for the new chunk.
   * @param  name      The name for the new chunk.
   * @param  type      The MIME type for the new chunk.
   * @param  data      The data for the new chunk, which is
   *                   <i>not</i> copied.
   * @param  previous  The ID of the previous chunk in the list of
   *                   chunks.
   * @param  next      The ID of the next chunk in the list of chunks.
   */
  public Chunk(Guid id, String name, String type, byte[] data,
               Guid previous, Guid next, long length) {
    super(id, name, type, data);
    this.previous = previous;
    this.next     = next;
    this.length   = length;
  } 

  /** Return a string representation for this chunk. */
  public String toString() {
    if (null == data) {
      return "#[Chunk name=\"" + name + "\" type=\"" + type +
        "\" size=<none> total-length=" + length + "]";
    } else {
      return "#[Chunk name=\"" + name + "\" type=\"" + type +
        "\" size=" + data.length + " total-length=" + length + "]";
    }
  }

}
