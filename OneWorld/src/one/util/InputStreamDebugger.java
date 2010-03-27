/*
 * Copyright (c) 1999, 2000, Robert Grimm.
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
 * 3. Neither name of Robert Grimm nor the names of his contributors
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

import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;

/**
 * Implementation of a input stream debugger. An input stream debugger
 * is a utility class that dumps all bytes read from an underlying
 * input stream to a file.
 *
 * @version  $Revision: 1.1 $
 * @author   Robert Grimm
 */
public class InputStreamDebugger extends InputStream {

  /** The underlying input stream. */
  private InputStream      in;

  /** The debug file. */
  private FileOutputStream out;

  /**
   * Create a new input stream debugger for the specified input stream.
   *
   * @param   in    The input stream to debug.
   * @param   name  The name of the file to write data to.
   * @throws  IOException
   *                Signals an exceptional condition while opening the
   *                file with the specified name.
   */
  public InputStreamDebugger(InputStream in, String name) throws IOException {
    this.in = in;
    out     = new FileOutputStream(name);
  }

  /**
   * Read a byte. This method reads a byte from the underlying input
   * stream and, before returning it, writes it to the debug file.
   */
  public int read() throws IOException {
    int i = in.read();
    out.write(i);
    out.flush();

    return i;
  }

  /**
   * Return the number of bytes currently available from the
   * underlying input stream.
   */
  public int available() throws IOException {
    return in.available();
  }

  /** Close this input stream debugger. */
  public void close() throws IOException {
    out.close();
    in.close();
  }

}
