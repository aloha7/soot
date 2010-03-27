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

/**
 * MimeURLEncoded implements encoders and decoders for the
 * "<code>application/x-www-form-urlencoded</code>" MIME type.
 *
 * <p>The javadoc documentation for class java.net.URLEncoder (whose
 * encode method is used internally to encode strings) specifies the
 * encoding as follows:
 * <ul>
 * <li> The ASCII characters '<code>a</code>' through
 * '<code>z</code>', '<code>A</code>' through '<code>Z</code>', and
 * '<code>0</code>' through '<code>9</code>' remain the same. </li>
 * <li> The space character '<code> </code>' is converted into a plus
 * sign '<code>+</code>'. </li>
 * <li> All other characters are converted into the 3-character string
 * "<code>%<i>xy</i></code>", where <i>xy</i> is the two-digit
 * hexadecimal representation of the lower 8-bits of the
 * character. </li>
 * </ul>
 * </p>
 *
 * <p>In actuality (as of JDK 1.1), java.net.URLEncoder.encode does
 * not translate the star sign '<code>*</code>', the underscore
 * '<code>_</code>', the minus sign '<code>-</code>' and the full stop
 * '<code>.</code>' into "<code>%<i>xy</i></code>" form, but leaves
 * them as is. Similarily, both Netscape Communicator 4.0 and Internet
 * Explorer 4.0 do not translate these characters into
 * "<code>%<i>xy</i></code>" form, with the addition of the at sign
 * '<code>@</code>' which is not translated into
 * "<code>%<i>xy</i></code>" form as well.</p>
 *
 * <p>In addition to decoding and encoding strings from and into
 * "<code>application/x-www-form-urlencoded</code>" format, the class
 * MimeURLEncoded provides functionality to decode raw byte arrays
 * from "<code>application/x-www-form-urlencoded</code>" format. This
 * is safely possible as the
 * "<code>application/x-www-form-urlencoded</code>" format specifies a
 * 8-bit encoding.</p>
 *
 * <p>The decoders in this class correctly handle all variations of
 * the "<code>application/x-www-form-urlencoded</code>" format (as
 * described above). Furthermore, they not only handle all 8-bit
 * character encodings that include the 7-bit ASCII characters (such
 * as the family of ISO 8859 encodings), but also handle the UTF-8
 * encoding.</p>
 *
 * @author   Robert Grimm
 * @version  $Revision: 1.3 $ 
 */
public class MimeURLEncoded {
  // Make constructor invisible.
  private MimeURLEncoded() {
    // Nothing to construct.
  }

  /**
   * Translate a string into
   * "<code>application/x-www-form-urlencoded</code>" format.
   *
   * @param   s  The string to be translated.
   * @return     The string in 
   *             "<code>application/x-www-form-urlencoded</code>"
   *             format.
   */
  public static String encode(String s) {
    return java.net.URLEncoder.encode(s);
  }

  /**
   * Translate a string from
   * "<code>application/x-www-form-urlencoded</code>" format.
   *
   * @param      s               The string to be translated.
   * @return                     The decoded string.
   * @exception  ParseException  Signals an exceptional condition
   *                             when parsing an encoded character,
   *                             that is when attempting to convert
   *                             the two characters following a '%'.  */
  public static String decode(String s) throws ParseException {
    int           length  = s.length();
    StringBuffer  buf     = new StringBuffer(length);

    int i = 0;

    while (i < length) {
      char c = s.charAt(i);
      if (c == '%') {
        try {
          buf.append((char)Integer.parseInt(
                                            s.substring(i+1, i+3), 16));
        } catch (NumberFormatException x) {
          throw new ParseException("Invalid character encoding");
        } catch (StringIndexOutOfBoundsException x) {
          throw new ParseException("Incomplete character encoding");
        }
        i += 3;
      } else if (c== '+') {
        buf.append(' ');
        i++;
      } else {
        buf.append(c);
        i++;
      }
    }

    return buf.toString();
  }

  /**
   * Return the index within the specified byte buffer of the first
   * occurrence of the specified byte, starting the search at the
   * specified index.
   *
   * @param   buf     The byte buffer.
   * @param   b       The byte to scan for.
   * @param   start   The start index.
   * @param   length  The absolute length of the region to scan,
   *                  as counted from the beginning of the byte
   *                  buffer.
   * @return          The index of the byte to scan for, or -1 if the
   *                  byte does not occur.
   */
  protected static int indexOf(byte[] buf, int b, int start, int length) {
    for (int i = start; i < length; i++) {
      if (buf[i] == b) {
        return i;
      }
    }
    
    return -1;
  }

  /**
   * Determine the length of a form field name. Scans the byte buffer
   * starting at the specified position until it encounters an equals
   * sign ('=') indicating the end of the form field name.
   *
   * @param      buf             The byte buffer.
   * @param      start           The start index of the form field
   *                             name.
   * @param      length          The absolute length of the region to
   *                             scan, as counted from the beginning of
   *                             the byte buffer.
   * @return                     The length of the form field name.
   * @exception  ParseException  Signals malformed form data.
   */
  public static int lengthOfName(byte[] buf, int start, int length)
    throws ParseException {
    length = Math.min(buf.length, length);

    int i = indexOf(buf, '=', start, length);

    if (i == -1) {
      throw new ParseException("Name field not followed by value");
    }

    return i - start;
  }

  /**
   * Determine the length of a form field value. Scans the byte buffer
   * starting at the specified position until it encounters an and
   * sign ('&') or the end of the buffer region, where either of the
   * two indicates the end of the form field value.
   *
   * @param      buf             The byte buffer.
   * @param      start           The start index of the from field
   *                             value.
   * @param      length          The absolute length of the region
   *                             to scan, as counted from the
   *                             beginning of the byte buffer.
   * @return                     The length of the form field value.
   */
  public static int lengthOfValue(byte[] buf, int start, int length) {
    length = Math.min(buf.length, length);

    int i = indexOf(buf, '&', start, length);
    
    if (i == -1) {
      return length - start;
    }

    return i - start;
  }

  /**
   * Test if a region within a byte buffer, starting at the specified
   * index, matches a sequence of bytes.
   *
   * @param   buf     The byte buffer.
   * @param   bytes   The sequence of bytes to be matched.
   * @param   start   The index in the byte buffer for the comparison.
   * @param   length  The absolute length of valid data in the byte
   *                  buffer, as counter from the beginning of the
   *                  byte buffer.
   * @return          True iff the region in the byte buffer starting
   *                  at the specified index matches the specified
   *                  sequence of bytes.
   */
  public static boolean regionMatches(byte[] buf,
                                      byte[] bytes,
                                      int start,
                                      int length) {
    length = Math.min(buf.length, length);

    if (start + bytes.length > length) {
      return false;
    }

    for (int i = start; i < start + bytes.length; i++) {
      if (buf[i] != bytes[i-start]) {
        return false;
      }
    }

    return true;
  }

  /**
   * Find the index of the form field value with the specified
   * name. Scans the form data starting from the specified index of
   * the byte buffer to find a form field with the specified
   * name. Returns the index within the byte buffer of the
   * corresponding form field value.
   *
   * @param      buf             The byte buffer.
   * @param      bytes           The form field name.
   * @param      start           The start index.
   * @param      length          The absolute length of the region to
   *                             scan, as counted from the beginning
   *                             of the byte buffer.
   * @return                     The index of the form field value
   *                             for the specified form field name,
   *                             or -1 if no such form field was
   *                             found.
   * @exception  ParseException  Signals malformed form data.
   */
  public static int indexOfValue(byte[] buf,
                                 byte[] bytes,
                                 int    start,
                                 int    length)
    throws ParseException {
    length = Math.min(buf.length, length);

    int index = start;

    while (index < length) {
      int nLength  = lengthOfName(buf, index, length);
      int iValue   = index + nLength + 1;

      if ((nLength == bytes.length)
          && (regionMatches(buf, bytes, index, length))) {
        return iValue;
      }

      index += (nLength + lengthOfValue(buf, iValue, length) + 2);
    }

    return -1;
  }

  /**
   * Convert from hexadecimal form. Converts a byte representing a
   * hexadecimal number into the corresponding, actual number.
   *
   * @param     b               The byte to be converted from
   *                            hexadecimal form.
   * @return                    The corresponding number.
   * @exception ParseException  Signals an invalid character.
   */
  protected static int fromHexByte(byte b) throws ParseException {
    int i = Character.digit((char) b, 16);

    if (i == -1) {
      throw new ParseException("Invalid hexadecimal character");
    }

    return i;
  }

  /**
   * Translate a section of a byte array from
   * "<code>application/x-www-form-urlencoded</code>" format. The
   * translation occurs <i>in place</i>, that is the section of the
   * byte array to be translated will be overwritten with the result.
   *
   * @param      buf             The byte array.
   * @param      start           Index of the first byte to be
   *                             translated.
   * @param      count           Number of bytes to convert.
   * @return                     The number of valid bytes of the
   *                             translated section (starting from
   *                             the specified index).
   * @exception  ParseException  Signals an exceptional condition
   *                             during translation.
   */
  public static int decode(byte[] buf, int start, int count)
    throws ParseException {
    int i   = start;                               // To be translated index.
    int end = Math.min(buf.length, start + count); // End of region
    int l   = start;                               // Translated index.

    // Boundary condition checking.
    if (((count >= 1) && (buf[end-1] == '%'))
        || ((count >= 2) && (buf[end-2] == '%'))) {
      throw new ParseException("Incomplete encoding");
    }

    // Translate.
    while (i < end) {
      byte b = buf[i];
      if (b == '%') {
        int num = (fromHexByte(buf[++i]) << 4) + fromHexByte(buf[++i]);
        buf[l]  = (byte) num;
      } else if (b == '+') {
        buf[l]  = (byte) ' ';
      } else {
        buf[l]  = b;
      }
      i++;
      l++;
    }

    return l - start;
  }
}
