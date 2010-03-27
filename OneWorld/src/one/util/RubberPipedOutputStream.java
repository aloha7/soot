/*
 * copyright (c) 2001, University of Washington, Department of
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

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;

/**
 *
 * RubberPipedOutputStream and {@link RubberPipedInputStream} allow data
 * to be piped between two or more threads, similar to {@link
 * PipedInputStream} and {@link PipedOutputStream}.
 * However, RubberPipedInputStream grows its
 * internal buffer rather than blocking when the buffer is full.  It also
 * supports {@link RubberPipedInputStream#mark(int)} and 
 * {@link RubberPipedInputStream#reset()}.
 *
 * <p>A RubberPipedOutputStream is useful when data must be written to a
 * stream in response to one event and read from a stream in response to
 * another event, for instance, when reassembling the contents of {@link
 * one.world.data.Chunk}s into a byte stream.</p>
 *
 * @see      RubberPipedInputStream
 * @version  $Revision: 1.1 $
 * @author   Janet Davis
 */
public class RubberPipedOutputStream extends OutputStream {
  
  /** The input stream this is connected to. */
  RubberPipedInputStream sink;

  /** 
   * Constructs a new rubber piped output stream.  It must be connected
   * using {@link #connect(RubberPipedInputStream)} or {@link
   * RubberPipedInputStream#RubberPipedInputStream(RubberPipedOutputStream)}
   * before it can be used;
   * otherwise, an {@link IOException} will result.
   */
  public RubberPipedOutputStream() {}

  /** 
   * Constructs a new rubber piped output stream connected to the specified
   * rubber piped input stream.
   *
   * @param  in  The input stream to connect to.
   * @throws IOException if <code>in</code> is already connected.
   */
  public RubberPipedOutputStream(RubberPipedInputStream in) 
         throws IOException {
    connect(in);
  }

  /** 
   * Connects this rubber piped output stream to the specified rubber
   * piped input stream.  This is equivalent to
   * <pre>
   *   in.connect(this)
   * </pre>
   *
   * @param in  The rubber piped input stream to connect to.
   * @throws IOException if either endpoint is already connected. 
   */
  public synchronized void connect(RubberPipedInputStream in) 
         throws IOException {
    if (in == null) {
      throw new NullPointerException();
    } else if (sink != null || in.connected) {
      throw new IOException("Already connected");
    }

    sink = in;
    in.connected = true;
  }

  /** 
   * Writes the specified byte to the output stream.  
   *
   * @param b    The byte to write.
   * @throws IOException if the output stream is not connected
   *                     or if the sink cannot receive data.
   */
  public synchronized void write(int b) throws IOException {
    if (sink == null) {
      throw new IOException("Pipe not connected");
    }
    sink.receive(b);
  }

  /** 
   * Writes <code>len</code> bytes from the byte array <code>b</code>,
   * starting at offset <code>off</code>.
   *
   * @param b    The data to write.
   * @param off  The offset to start writing from.
   * @param len  The number of bytes to write.
   *
   * @throws IOException if the output stream is not connected or if
   *                     the sink cannot receive data.
   */
  public synchronized void write(byte[] b, int off, int len) 
          throws IOException {
    if (sink == null) {
      throw new IOException("Pipe not connected");
    } else if (b == null) {
      throw new NullPointerException();
    } else if ((off < 0)
               || (off > b.length) 
               || (len < 0) 
	       || ((off + len) > b.length) 
	       || ((off + len) < 0)) {
      throw new IndexOutOfBoundsException();
    } else if (len == 0) { 
      return;
    } 

    sink.receive(b, off, len); 
  }

  /**
   * Flushes this output stream and forces any buffered output bytes to be
   * written.  This will notify any readers that data is waiting.
   */
  public synchronized void flush() {
    if (sink != null) {
      synchronized (sink) {
        sink.notifyAll();
      }
    }
  }

  /**
   * Closes this output stream and releases any associated system
   * resources.
   *
   * @throws IOException  if an I/O error occurs.
   */
  public synchronized void close() throws IOException {
    if (sink != null) {
      sink.receivedLast();
    }
  }
}
