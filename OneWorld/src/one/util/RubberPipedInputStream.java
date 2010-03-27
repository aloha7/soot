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
 * RubberPipedInputStream and {@link RubberPipedOutputStream} allow data
 * to be piped between two or more threads, similar to {@link
 * PipedInputStream} and {@link PipedOutputStream}.
 * However, RubberPipedInputStream grows its 
 * internal buffer rather than blocking when the buffer is full.  It also
 * supports {@link #mark(int)} and {@link #reset()}.
 *
 * <p>A RubberPipedInputStream is useful when data must be written to a
 * stream in response to one event and read from a stream in response to
 * another event, for instance, when reassembling the contents of {@link
 * one.world.data.Chunk}s into a byte stream.</p>
 *
 * @see      RubberPipedOutputStream
 * @version  $Revision: 1.2 $
 * @author   Janet Davis
 */
public class RubberPipedInputStream extends InputStream {

  /** The initial buffer size. */
  protected static final int INITIAL_SIZE = 16;
  
  /**
   * The circular buffer into which incoming data is placed.
   */
  protected byte buffer[] = new byte[INITIAL_SIZE];

  /**
   * The index of the position in the circular buffer at which the
   * next byte of data will be stored when received from the connected
   * piped output stream. <code>in&lt;0</code> implies the buffer is empty,
   * <code>in==out</code> implies the buffer is full.
   */
  protected int in = -1;

  /**
   * The index of the position in the circular buffer at which the next
   * byte of data will be read by this piped input stream.
   * @since   JDK1.1
   */
  protected int out = 0;

  /** The mark position. */
  protected int mark = -1;

  /** Is this rubber piped input stream connected? */
  protected boolean connected;

  /** Is this rubber piped input stream closed? */
  protected boolean closed;

  /** 
   * Constructs a new rubber piped input stream.   It must be connected
   * using {@link #connect(RubberPipedOutputStream)} or {@link
   * RubberPipedOutputStream#RubberPipedOutputStream(RubberPipedInputStream)} 
   * before it can be used; otherwise, an {@link IOException} will result.
   */
  public RubberPipedInputStream() {}

  /** 
   * Constructs a new rubber piped input stream connected to the specified
   * rubber piped output stream.
   *
   * @param out  The output stream to connect to.
   * @throws IOException if <code>out</code> is already connected.
   */
  public RubberPipedInputStream(RubberPipedOutputStream out) 
         throws IOException {
    out.connect(this);
  }

  /** 
   * Connects this rubber piped input stream to the specified rubber piped
   * output stream.  This is equivalent to 
   * <pre>
   *   out.connect(this)
   * </pre>
   *
   * @param  out   The output stream to connect to.
   *
   * @throws IOException if either endpoint is already connected.
   */
  public void connect(RubberPipedOutputStream out) throws IOException {
    out.connect(this);
  }

  /**
   * Check to see if the pipe is connected.
   *
   * @throws IOException if the pipe is closed or is not connected.
   */
  protected void checkConnected() throws IOException {
    if (!connected) {
      throw new IOException("Pipe not connected");
    } else if (closed) {
      throw new IOException("Pipe closed");
    }
  }

  /**
   * Doubles the size of the internal buffer.
   */
  protected synchronized void grow() {
    byte[] newBuffer = new byte[2*buffer.length];

    if (in < 0) {
      // The buffer is empty, so we can throw the old data away.  No need
      // to do anything here.
      
    } else if (out < in) {
      // This is the easy case; just copy the whole darn thing.
      System.arraycopy(buffer, 0, newBuffer, 0, buffer.length);

    } else {
      // OK, this is harder.
      // There is data from out to length-1 and from 0 to in.
      
      // Copy front half.  
      System.arraycopy(buffer, 0, newBuffer, 0, in);

      // Copy back half.
      int length = buffer.length - out;
      int newOut = newBuffer.length - length;
      System.arraycopy(buffer, out, newBuffer, newOut, length);
      out = newOut;

    }

    // Change the array reference.
    buffer = newBuffer;
  }

  /** 
   * Computes the amount of free space in the buffer. 
   */
  protected synchronized int freespace() {
    if (in < 0) {
      // Empty.
      return buffer.length;
    } else if (out < in) {
      return out + (buffer.length - in);
    } else {
      return out - in;
    }
  }

  /**
   * Returns the number of bytes that can be read from this input stream
   * without blocking.
   *
   * @return  The number of bytes that can be read without blocking.
   */
  public synchronized int available() {
    return buffer.length - freespace();
  }

  /**
   * Receives a single byte.
   */
  void receive(int b) throws IOException {
    receive1(b);
  }

  /**
   * Receives a single byte.  If no space is available, the buffer will be
   * thrown.  
   *
   * @param b  The byte to receive.
   *
   * @throws IOException if the pipe is closed or is not connected.
   */
  protected synchronized void receive1(int b) throws IOException {

    checkConnected();

    if (in == out) {
      // The array is full, so grow it.
      grow();
    }

    if (in < 0) {
      // Empty.
      in = 0;
      out = 0;
    }

    
    buffer[in] = (byte)(b & 0xFF);
    in = (in + 1) % buffer.length;


    notifyAll();
  }

  /**
   * Receives data from an array of bytes. 
   */
  synchronized void receive(byte b[], int off, int len) throws IOException {
    receive1(b, off, len);
  }

  /**
   * Receives data from an array of bytes.  If no space is available, the
   * buffer will be grown.
   *
   * @param b        The buffer from which the data is received.
   * @param off      The start offset of the data.
   * @param len      The number of bytes to receive.
   *
   * @throws IOException if the pipe is closed or is not connected.
   */
  protected synchronized void receive1(byte b[], int off, int len) 
          throws IOException {

    checkConnected();

    // If there's not enough free space, grow the internal buffer.
    while (freespace() < len) {
      grow();
    }

    if (in < 0) {
      // Empty.
      in = 0;
      out = 0;
    }

    // Two cases here: free space is continuous, or is at the end and
    // beginning of the buffer.
    if (in <= out) {
      // Free space is continuous.
      System.arraycopy(b, off, buffer, in, len);
      in += len;

    } else {
      // Free space is wrapped.

      // Copy as much as possible at the end.
      int len1 = (len > buffer.length - in) ? (buffer.length - in) : len;
      System.arraycopy(b, off, buffer, in, len1);

      in = (in + len1) % buffer.length;
      off += len1;
      len -= len1;

      if (len > 0) {
        // More to write at the beginning.
	in = 0;
        System.arraycopy(b, off, buffer, in, len);
        in += len;
      }
    }

    notifyAll();
  }

  /** 
   * Reads one byte of data from this rubber piped input stream.  This
   * method blocks until at least one byte of input is available.
   *
   * @throws IOException if the pipe is closed or is not connected.
   */
  public synchronized int read() throws IOException {

    checkConnected();

    while (in < 0) {
      // There might be a writer waiting.
      notifyAll();
      try {
        wait(100);
      } catch (InterruptedException x) {
        throw new InterruptedIOException();
      }

      // May have been closed while we were waiting.
      checkConnected();
    }

    // Return value should be int between 0 and 255.
    int ret = buffer[out] & 0xFF;

    // Advance the out pointer.
    out = (out + 1) % buffer.length;

    if (in == out) {
      // Now empty.
      in = -1;
    }
    
    return ret;
  }


  /**
   * Reads up to <code>len</code> bytes of data from this rubber piped input
   * stream into an array of bytes. Less than <code>len</code> bytes
   * will be read if the end of the data stream is reached. This method
   * blocks until at least one byte of input is available.
   *
   * @param      b     the buffer into which the data is read.
   * @param      off   the start offset of the data.
   * @param      len   the maximum number of bytes read.
   * @return     the total number of bytes read into the buffer, or
   *             <code>-1</code> if there is no more data because the end of
   *             the stream has been reached.
   * @exception  IOException  if an I/O error occurs.
   */
  public synchronized int read(byte b[], int off, int len)  throws IOException {
    if (b == null) {
      throw new NullPointerException();
    } else if ((off < 0) 
               || (off > b.length) 
	       || (len < 0) 
	       || ((off + len) > b.length) 
	       || ((off + len) < 0)) {
      throw new IndexOutOfBoundsException();
    } else if (len == 0) {
      return 0;
    }

    // May need to wait on the first character.
    int c = read();
    if (c < 0) {
      return -1;
    }

    b[off] = (byte) c;
    len--;
    off++;

    if (in == out) {
      // Now empty.
      in = -1;
      return 1;
    }
    
    if (out < in) {
      // Can copy a continuous hunk of data into the output array.
      int copyLen = (len > in - out) ? (in - out) : len;
      System.arraycopy(buffer, out, b, off, copyLen);
      out += copyLen;
      if (in == out) {
        // Now empty.
        in = -1;
      }
      return copyLen + 1;

    } else {
      // May need to copy from both back and front of the buffer.
      int copyLen = (len > buffer.length - out) ? buffer.length - out : len;
      System.arraycopy(buffer, out, b, off, copyLen);

      out = (out + copyLen) % buffer.length;
      off += copyLen;
      len -= copyLen;

      if (len > 0) {
        // We consumed everything at the end of the buffer and need to 
	// copy more stuff from the beginning of the buffer.
        if (len > in - out) {
	  len = in - out;
	}
	System.arraycopy(buffer, out, b, off, len);
	out += len;
      }

      if (in == out) {
        // Now empty.
	in = -1;
      }

      return copyLen + len + 1;
    }
  }

  /**
   * Marks the current position in this input stream. A subsequent call to
   * the reset method repositions this stream at the last marked position
   * so that subsequent reads re-read the same bytes. 
   *
   * @param readlimit 
   * The maximum limit of bytes that can be read before the mark position
   * becomes invalid.
   */
  public synchronized void mark(int readlimit) {
    // Grow the internal buffer to accommodate readlimit bytes of data.
    while (buffer.length < readlimit) {
      grow();
    }

    mark = out;
  }

  /**
   * Repositions this stream to the position at the time the mark method
   * was last called on this input stream. 
   *
   * @throws IOException if no mark has been set.
   */
  public synchronized void reset() throws IOException {
    if (mark < 0) {
      throw new IOException("No mark set");
    } else {
      out = mark;
    }
  }

  /** 
   * Tests if this input stream supports the mark and reset methods. The
   * markSupported method of RubberPipedInputStream returns
   * <code>true</code>.
   */
  public boolean markSupported() {
    return true;
  }

  /**
   * Notifies all waiting threads that the last byte of data has been
   * received.
   */
  synchronized void receivedLast() {
    closed = true;
    notifyAll();
  }

  /** 
   * Closes this rubber piped input stream.
   */
  public synchronized void close() {
    in = -1;
    closed = true;
  }

  /** 
   * Prints a useful debug statement.
   */
  private void debug() {
    System.out.println("out = " + out + ", in = " + in
                       + " (" + new String(buffer) + ")");
  }

  /**
   * A simple test program.
   */
  public static void main(String args[]) throws IOException {
    RubberPipedInputStream in = new RubberPipedInputStream();
    RubberPipedOutputStream out = new RubberPipedOutputStream(in);

    String s = "The quick brown fox jumped over the lazy dog. ";
    byte[] write = s.getBytes();
    byte[] read = new byte[20];
    out.write(write);
    in.read(read);
    System.out.println(new String(read));
    in.read(read);
    System.out.println(new String(read));

    s = "Now is the time for all good men to come to the aid of the party.";
    write = s.getBytes();
    out.write(write);
    in.read(read);
    System.out.println(new String(read));
    in.read(read);
    System.out.println(new String(read));

    out.write(" My bonny lies over the ocean.".getBytes());

    read = new byte[500];
    int bytesRead = in.read(read);
    System.out.println(new String(read, 0, bytesRead));
  }
}
