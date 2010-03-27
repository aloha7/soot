/*
 * Copyright (c) 2001, University of Washington, Department of
 * Computer Science and Engineering.
 *    All rights reserved.
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

package one.net;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

import java.util.Iterator;

import one.util.Bug;

import one.world.core.DynamicTuple;

/**
 * Implementation of an Internet output stream. An Internet output
 * stream adds additional methods to an output stream to write a line
 * and to write an Internet message header, as defined in <a
 * href="http://www.ietf.org/rfc/rfc0822.txt">RFC 822</a> and as
 * clarified in the <a
 * href="http://www.ietf.org/rfc/rfc2616.txt">HTTP/1.1</a>
 * specification. Internet message headers are represented as dynamic
 * tuples.</p>
 *
 * <p>Note that Internet output streams are not thread-safe.</p>
 * 
 * @version  $Revision: 1.3 $
 * @author   Robert Grimm
 */
public class NetOutputStream
  extends FilterOutputStream implements NetConstants {

  /** The writer for writing strings. */
  private Writer out2;

  /**
   * Create a new Internet output stream with specified underlying
   * output stream.
   *
   * @param  out  The output stream for the new Internet output stream.
   */
  public NetOutputStream(OutputStream out) {
    super(out);
    try {
      out2 = new OutputStreamWriter(out, ENCODING_UTF8);
    } catch (UnsupportedEncodingException x) {
      throw new Bug("UTF-8 character encoding not supported");
    }
  }

  /**
   * Write an empty line to this Internet output stream.
   *
   * @throws  IOException
   *            Signals an exceptional condition while writing
   *            to the underlying output stream.
   */
  public void writeLine() throws IOException {
    out.write('\r');
    out.write('\n');
  }

  /**
   * Write a byte array to the output stream.  Overridden to use the 
   * <code>write(byte[])</code> method of the wrapped stream.
   *
   * @param b The data to write
   */
  public void write(byte[] b) throws IOException {
    out.write(b);
  }

  /**
   * Write a byte array to the output stream.  Overridden to use the
   * <code>write(byte[a],int,int)</code> method of the wrapped stream.
   *
   * @param b The data to write
   * @param off The write offset
   * @param len The write length 
   */
  public void write(byte[] b,int off,int len) throws IOException {
    out.write(b,off,len);
  }

  /**
   * Write the specified message followed by a carriage-return,
   * line-feed to this Internet output stream.
   *
   * @param   msg  The message to write.
   * @throws  IOException
   *               Signals an exceptional condition while writing
   *               to the underlying output stream.
   */
  public void writeLine(String msg) throws IOException {
    out2.write(msg);
    out2.write(CRLF);
    out2.flush();
  }

  /**
   * Write the specified Internet message header to this Internet
   * output stream.
   *
   * @param   header  The Internet message header to write.
   * @throws  IOException
   *               Signals an exceptional condition while writing
   *               to the underlying output stream.
   */
  public void writeHeader(DynamicTuple dt) throws IOException {
    Iterator iter = dt.fields().iterator();

    while (iter.hasNext()) {
      String name  = (String)iter.next();
      Object value = dt.get(name);

      out2.write(name);
      out2.write(": ");
      if (null == value) {
        out2.write("null");
      } else {
        out2.write(value.toString());
      }
      out2.write(CRLF);
    }

    out2.write(CRLF);
    out2.flush();
  }

}
