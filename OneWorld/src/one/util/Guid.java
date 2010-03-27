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

package one.util;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * Implementation of a globally unique identifier (GUID). Globally
 * unique identifiers are 128-bit values as defined by the Open
 * Group's DCE standard and as used in Microsoft's COM and DCOM.
 *
 * <p>GUIDs are created by a native, platform-dependent
 * library. Currently, Windows and Linux are supported. The Windows
 * library works on Windows 95 (DCOM release), Windows 98, Windows Me,
 * Windows NT 4.0, and Windows 2000.</p>
 *
 * @see      <a href="http://www.opengroup.org/onlinepubs/9629399/apdxa.htm">DCE Specification for GUIDs</a>
 * @see      <a href="http://www.opennc.org/dce/info/draft-leach-uuids-guids-01.txt">Internet draft specification for GUIDs and UUIDs</a>
 *
 * @author   <a href="mailto:rgrimm@cs.washington.edu">Robert Grimm</a>.
 * @version  $Revision: 1.13 $
 */
public class Guid implements java.io.Serializable {

  /** The serial version ID for this class. */
  static final long serialVersionUID = -6798123864977760588L;

  /**
   * A dummy object to be used as a mutex by the native create()
   * function in Linux.
   */
  private static Object lock;

  /**
   * The most significant 64-bits of this GUID.
   *
   * @serial
   */
  private long hi;

  /**
   * The least significant 64-bits of this GUID.
   *
   * @serial
   */
  private long lo;

  /**
   * Create a new GUID. Due to limitations of the Java standard APIs,
   * it is impossible to correctly create a new GUID in Java. This
   * constructor thus requires a native library to perform the actual
   * GUID creation.
   */
  public Guid() {
    create();
  }

  /**
   * Create a new GUID. Actually creates a new GUID and fills in the
   * fields.
   */
  private native void create();

  static {
    AccessController.doPrivileged(new PrivilegedAction() {
        public Object run() {
          System.loadLibrary("one_util_Guid");
          return null;
        }
      });
    lock=new Object();
  }

  /**
   * Create a new GUID from the specified string. This constructor
   * parses the string representation of a GUID, for example, as
   * returned by the {@link #toString()} method of this class.
   *
   * @param   s  The string representation of the GUID.
   * @throws  NumberFormatException
   *             Signals that the specified string does not have
   *             the expected format.
   */
  public Guid(String s) throws NumberFormatException {
    if ((36  != s.length()) ||
        ('-' != s.charAt( 8)) ||
        ('-' != s.charAt(13)) ||
        ('-' != s.charAt(18)) ||
        ('-' != s.charAt(23))) {
      throw new NumberFormatException(
        "Invalid structure for string representation of GUID");
    }

    hi = ((Long.parseLong(s.substring( 0,  8), 16) << 32) |
          (Long.parseLong(s.substring( 9, 13), 16) << 16) |
          (Long.parseLong(s.substring(14, 18), 16)));

    lo = ((Long.parseLong(s.substring(19, 23), 16) << 48) |
          (Long.parseLong(s.substring(24, 36), 16)));
  }

  /**
   * Create a new GUID from the specified string using the specified
   * separator. This constructor parses the string representation of a
   * GUID, for example, as returned by the {@link #toString(char)}
   * method of this class.
   *
   * @param   s    The string representation of the GUID.
   * @param   sep  The separator.
   * @throws  NumberFormatException
   *             Signals that the specified string does not have
   *             the expected format.
   */
  public Guid(String s, char sep) throws NumberFormatException {
    if ((36  != s.length()) ||
        (sep != s.charAt( 8)) ||
        (sep != s.charAt(13)) ||
        (sep != s.charAt(18)) ||
        (sep != s.charAt(23))) {
      throw new NumberFormatException(
        "Invalid structure for string representation of GUID");
    }

    hi = ((Long.parseLong(s.substring( 0,  8), 16) << 32) |
          (Long.parseLong(s.substring( 9, 13), 16) << 16) |
          (Long.parseLong(s.substring(14, 18), 16)));

    lo = ((Long.parseLong(s.substring(19, 23), 16) << 48) |
          (Long.parseLong(s.substring(24, 36), 16)));
  }

  /**
   * Create a new GUID from the specified byte array. This constructor
   * creates a new GUID by reading the binary representation of the
   * GUID from the specified byte array. The byte array must be 16
   * bytes long and the bytes must be in standard network byte order.
   *
   * @see     #toBytes
   *
   * @param   bytes  The binary representation of new GUID.
   * @throws  IllegalArgumentException
   *                 Signals that <code>bytes</code> is not 16
   *                 bytes long.
   */
  public Guid(byte[] bytes) {
    if (16 != bytes.length) {
      throw new IllegalArgumentException("Invalid byte array (length not 16)");
    }

    hi = ((bytes[ 0] & 0xFFL) << 56) |
         ((bytes[ 1] & 0xFFL) << 48) |
         ((bytes[ 2] & 0xFFL) << 40) |
         ((bytes[ 3] & 0xFFL) << 32) |
         ((bytes[ 4] & 0xFFL) << 24) |
         ((bytes[ 5] & 0xFFL) << 16) |
         ((bytes[ 6] & 0xFFL) <<  8) |
         ((bytes[ 7] & 0xFFL) <<  0);
    lo = ((bytes[ 8] & 0xFFL) << 56) |
         ((bytes[ 9] & 0xFFL) << 48) |
         ((bytes[10] & 0xFFL) << 40) |
         ((bytes[11] & 0xFFL) << 32) |
         ((bytes[12] & 0xFFL) << 24) |
         ((bytes[13] & 0xFFL) << 16) |
         ((bytes[14] & 0xFFL) <<  8) |
         ((bytes[15] & 0xFFL) <<  0);
  }

  /**
   * Create a new GUID from the specified high and low value.
   *
   * @see     #getHigh
   * @see     #getLow
   *
   * @param   high  The most significant 64 bits for the new GUID
   *                as a <code>long</code>.
   * @param   low   The least significant 64 bits for the new GUID
   *                as a <code>long</code>.
   */
  public Guid(long high, long low) {
    hi = high;
    lo = low;
  }

  /**
   * Create a new GUID from the specified data input. This constructor
   * creates a new GUID by reading 16 bytes in standard network byte
   * order from the specified data input.
   *
   * @param   in  The data input to read the new GUID from.
   * @throws  IOException
   *              Signals an exceptional condition when reading from
   *              the specified data input.
   */
  public Guid(DataInput in) throws IOException {
    hi = in.readLong();
    lo = in.readLong();
  }

  /**
   * Determine wheter this GUID equals the specified object. This GUID
   * equals the specified object, iff the specified object is a GUID
   * with the same 128-bit value.
   *
   * @return  <code>true</code> if <code>o</code> is a GUID with
   *          the same 128-bit value.
   */
  public boolean equals(Object o) {
    if (o instanceof Guid) {
      Guid other = (Guid)o;

      return ((hi == other.hi) && (lo == other.lo)) ;
    }

    return false;
  }

  /**
   * Get a hash code for this GUID.
   *
   * @return  A hash code for this GUID.
   */
  public int hashCode() {
    long temp = hi ^ lo;
    return (((int)temp) ^ ((int)(temp>>32)));
  }

  /**
   * Write this GUID to the specified data output. This method writes
   * this GUID to the specified data output by writing 16 bytes in
   * standard network byte order.
   *
   * @param   out  The data output to write the GUID to.
   * @throws  IOException
   *               Signals an exceptional condition when writing
   *               to the specified data output.
   */
  public void writeBytes(DataOutput out) throws IOException {
    out.writeLong(hi);
    out.writeLong(lo);
  }

  /**
   * Get the high double word for this GUID. This method returns the
   * most significant 64 bits of this GUID as a <code>long</code>.
   *
   * @return  The high double word for this GUID.
   */
  public long getHigh() {
    return hi;
  }

  /**
   * Get the low double word for this GUID. This method returns the
   * least significant 64 bits of this GUID as a <code>long</code>.
   *
   * @return  The low double word for this GUID.
   */
  public long getLow() {
    return lo;
  }

  /**
   * Get the bytes for this GUID. This method returns a byte array of
   * length 16 containing the binary representation for this GUID. The
   * bytes are in standard network byte order.
   *
   * @return  The binary representation of this GUID.
   */
  public byte[] toBytes() {
    byte[] bytes = new byte[16];

    bytes[ 0] = (byte)(hi >>> 56);
    bytes[ 1] = (byte)(hi >>> 48);
    bytes[ 2] = (byte)(hi >>> 40);
    bytes[ 3] = (byte)(hi >>> 32);
    bytes[ 4] = (byte)(hi >>> 24);
    bytes[ 5] = (byte)(hi >>> 16);
    bytes[ 6] = (byte)(hi >>>  8);
    bytes[ 7] = (byte)(hi >>>  0);
    bytes[ 8] = (byte)(lo >>> 56);
    bytes[ 9] = (byte)(lo >>> 48);
    bytes[10] = (byte)(lo >>> 40);
    bytes[11] = (byte)(lo >>> 32);
    bytes[12] = (byte)(lo >>> 24);
    bytes[13] = (byte)(lo >>> 16);
    bytes[14] = (byte)(lo >>>  8);
    bytes[15] = (byte)(lo >>>  0);
    
    return bytes;
  }

  /** The template for a string representation of a GUID. */
  protected static final String STRING_TEMPLATE
    = "00000000-0000-0000-0000-000000000000";

  /**
   * Get a string representation of this GUID.
   *
   * @return  A string representation of this GUID.
   */
  public String toString() {
    return fill(new StringBuffer(STRING_TEMPLATE));
  }

  /**
   * Get a string representation of this GUID. The returned string
   * uses the underscore character instead of the dash character
   * specified for the standard string format.
   *
   * @return  A string representation of this GUID.
   */
  public String toUnderscoredString() {
    return fill(new StringBuffer("00000000_0000_0000_0000_000000000000"));
  }

  /**
   * Get a string representation of this GUID. The returned string
   * uses the specified separator instead of the dash character
   * specified for the standard string format.
   *
   * @param   sep  The separator.
   * @return       A string representation of this GUID.
   */
  public String toString(char sep) {
    return fill(new StringBuffer(STRING_TEMPLATE.replace('-', sep)));
  }

  /**
   * Fill the specified string buffer with a string representation of
   * this GUID. The specified string buffer must have a length of 35
   * and must contain
   * "<code>00000000-0000-0000-0000-000000000000</code>", where
   * '<code>-</code>' may be replaced with any other character used as
   * a separator.
   *
   * @param   buf  The string buffer to fill.
   * @return       The corresponding string.
   */
  protected String fill(StringBuffer buf) {
    String s;

    s = Integer.toHexString((int)(hi >> 32));           // time_low
    buf.replace( 8 - s.length(),  8, s);

    s = Integer.toHexString((int)(hi >> 16) & 0xFFFF);  // time_mid
    buf.replace(13 - s.length(), 13, s);

    s = Integer.toHexString((int)hi         & 0xFFFF);  // version, time_hi
    buf.replace(18 - s.length(), 18, s);

    s = Integer.toHexString((int)(lo >> 48) & 0xFFFF);  // variant, clock_seq
    buf.replace(23 - s.length(), 23, s);

    s = Long.toHexString(lo & 0xFFFFFFFFFFFFL);         // node
    buf.replace(36 - s.length(), 36, s);

    return buf.toString();
  }

}
