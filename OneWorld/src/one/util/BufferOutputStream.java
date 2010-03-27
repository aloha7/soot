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

package one.util;

import java.io.ByteArrayOutputStream;

/** 
 * Implementation of a buffer output stream. A buffer output stream
 * simply is a byte array output stream that provides access to its
 * internal byte array.
 *
 * @version  $Revision: 1.1 $
 * @author   Robert Grimm
 */
public class BufferOutputStream extends ByteArrayOutputStream {

  /** Create a new buffer output stream. */
  public BufferOutputStream() {
    // Nothing to do.
  }

  /**
   * Create a new buffer output stream with the specified initial
   * capacity.
   *
   * @param  size  The initial capacity.
   */
  public BufferOutputStream(int size) {
    super(size);
  }

  /**
   * Get the bytes for this buffer output stream. This method returns
   * the internal buffer <i>without</i> copying it. The {@link
   * #size()} method can be used to determine how many of the bytes in
   * the returned array are actually valid data.
   *
   * @return  The actual bytes.
   */
  public byte[] getBytes() {
    return buf;
  }

}
