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

import java.io.FilterInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.StreamCorruptedException;
import java.io.UnsupportedEncodingException;

import one.util.Bug;

import one.world.core.DynamicTuple;

/**
 * Implementation of an Internet input stream. An Internet input
 * stream adds additional methods to an input stream to read a line
 * and to read an Internet message header, as defined in <a
 * href="http://www.ietf.org/rfc/rfc0822.txt">RFC 822</a> and as
 * clarified in the <a
 * href="http://www.ietf.org/rfc/rfc2616.txt">HTTP/1.1</a>
 * specification. Internet message headers are represented as
 * dynamic tuples.</p>
 *
 * <p>Note that Internet input streams are not thread-safe.</p>
 * 
 * @version  $Revision: 1.1 $
 * @author   Robert Grimm
 */
public class NetInputStream extends FilterInputStream implements NetConstants {

  /** The default length of a newly created buffer. */
  private static final int DEFAULT_LENGTH = 80;
      
  /** The bytes for buffering read-in data. */
  private byte[] bytes;

  /** The length, that is, the number of valid bytes in the buffer. */
  private int    len;

  /**
   * Create a new Internet input stream with the specified underlying
   * input stream.
   *
   * @param  in  The input stream for the new Internet input stream.
   */
  public NetInputStream(InputStream in) {
    super(in);
    bytes = new byte[DEFAULT_LENGTH];
  }

  /**
   * Read a line from this Internet input stream.
   *
   * @return    The read-in line without line termination characters,
   *            or <code>null</code> if the underlying input stream
   *            has reached the end-of-file.
   * @throws  IOException
   *            Signals an exceptional condition while reading from
   *            the underlying input stream.
   */
  public String readLine() throws IOException {
    // Reset length.
    len = 0;

    do {
      int i = in.read();

      switch (i) {

      case -1:
        // We reached the end-of-file. If there is any buffered data,
        // we return it.
        if (0 == len) {
          return null;
        } else {
          try {
            return new String(bytes, 0, len, ENCODING_UTF8);
          } catch (UnsupportedEncodingException x) {
            throw new Bug("UTF-8 character encoding not supported");
          }
        }

      case '\r':
        // CR followed by a LF character or the end-of-file is treated
        // as a line termination. Otherwise, it is just a character.
        i = in.read();
        if (('\n' != i) && (-1 != i)) {
          ensureSpace();
          bytes[len] = '\r';
          len++;
          ensureSpace();
          bytes[len] = (byte)i;
          len++;
          break;
        }
        // Fall through.

      case '\n':
        // LF by itself is also treated as a line termination.
        if (0 == len) {
          return "";
        } else {
          try {
            return new String(bytes, 0, len, ENCODING_UTF8);
          } catch (UnsupportedEncodingException x) {
            throw new Bug("UTF-8 character encoding not supported");
          }
        }

      default:
        ensureSpace();
        bytes[len] = (byte)i;
        len++;
      }

    } while (true);
  }

  /** Ensure that the line buffer has space for adding one more byte. */
  private void ensureSpace() {
    if (bytes.length <= len) {
      byte[] newBytes = new byte[bytes.length * 2];
      System.arraycopy(bytes, 0, newBytes, 0, bytes.length);
      bytes           = newBytes;
    }
  }

  /**
   * Read an Internet message header from this Internet input stream.
   *
   * @return     The Internet message header contained in this Internet
   *             input stream or <code>null</code> if the underlying
   *             input stream has reached the end-of-file.
   * @throws  StreamCorruptedException
   *             Signals that the Internet message header contained
   *             in this Internet input stream is malformed.
   * @throws  IOException
   *             Signals an exceptional condition while reading from
   *             the underlying input stream.
   */
  public DynamicTuple readHeader() throws IOException {
    DynamicTuple dt    = null;
    String       name  = null;
    String       value = "";
    String       line;
    int          length;

    // Parse message header into dynamic tuple.
    line = readLine();
    while ((null != line) && ((length = line.length()) > 0)) {

      if (Character.isSpaceChar(line.charAt(0))) {
	// Continuing field definition.
	if (null == name) {
          throw new StreamCorruptedException("Internet message header field " +
                                             "value without name (" + line +
                                             ")");
        }

        String trimmedLine = line.trim();
        if (0 < trimmedLine.length()) {
          value = value + " " + trimmedLine;
        }

      } else {
        // New field definition.
        if (null != name) {
          // Add value to field.
          if (null == dt) {
            dt = new DynamicTuple();
          } else if (dt.hasField(name)) {
            value = dt.get(name) + ", " + value;
          }
          dt.set(name, value);

          // Reset name and value.
          name  = null;
          value = "";
        }

        // Determine index of name, value separator.
        int idx = line.indexOf(':');
        if (-1 == idx) {
          throw new StreamCorruptedException("Internet message header field " +
                                             "name without value (" + line +
                                             ")");
        }

        // Fill in field name and value.
        name  = line.substring(0, idx).trim();
        value = line.substring(idx + 1).trim();
      }

      // Read next line.
      line = readLine();
    }

    if (null == name) {
      // Handle empty headers.
      if ("".equals(line)) {
        dt = new DynamicTuple();
      }

    } else {
      // Add value to field.
      if (null == dt) {
        dt = new DynamicTuple();
      } else if (dt.hasField(name)) {
        value = dt.get(name) + ", " + value;
      }
      dt.set(name, value);
    }

    // Done.
    return dt;
  }

  /**
   * Convert an Internet message header field name to its canonical
   * form. Canonical form capitalizes the first letter of each
   * separate word in the field name, where words are separated by any
   * non-letter character, and ensures that all other letters in a
   * word are not capitalized.
   *
   * <p>Note that to be nice to HTTP, the names
   * "<code>Content-MD5</code>", "<code>ETag</code>",
   * "<code>TE</code>", and "<code>WWW-Authenticate</code>" are
   * canonicalized as shown.</p>
   *
   * @param   name  The field name to be converted into canonical form.
   * @return        The canonical form of the field name.
   */
  public static String canonicalize(String name) {
    int           len      = name.length();
    StringBuffer  buf      = new StringBuffer(len);
    boolean       newWord  = true;

    for(int i = 0; i < len; i++) {
      char c = name.charAt(i);

      if (! Character.isLetter(c)) {
	newWord = true;
	buf.append(c);
      } else if (newWord) {
	newWord = false;
	buf.append(Character.toUpperCase(c));
      } else {
	buf.append(Character.toLowerCase(c));
      }
    }

    String canon = buf.toString();

    // Fix the special cases: Content-MD5, ETag, TE, WWW-Authenticate
    if (canon.equals("Content-Md5")) {
      return "Content-MD5";
    } else if (canon.equals("Etag")) {
      return "ETag";
    } else if (canon.equals("Te")) {
      return "TE";
    } else if (canon.equals("Www-Authenticate")) {
      return "WWW-Authenticate";
    }

    return canon;
  }

}
