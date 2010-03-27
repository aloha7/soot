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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Implementation of a digest generator. A digest generator simplifies
 * the task of generating secure hashes. It reuses an internal message
 * digest. The current implementation defaults to an MD5 message
 * digest.
 *
 * @version  $Revision: 1.1 $
 * @author   Robert Grimm
 */
public final class Digest {

  /** The lock protecting the message digest. */
  private Object        lock;

  /** The message digest generator. */
  private MessageDigest generator;

  /** Create a new digest generator. */
  public Digest() {
    lock = new Object();

    try {
      generator = MessageDigest.getInstance("MD5");
    } catch (NoSuchAlgorithmException x) {
      throw new Bug("MD5 message digest not available");
    }

    generator.reset(); // Just to be safe...
  }

  /**
   * Hash the specified byte array.
   *
   * @param   b  The byte array.
   * @return     A secure hash over the specified byte array.
   * @throws  NullPointerException
   *             Signals that <code>b</code> is <code>null</code>.
   */
  public byte[] hash(byte[] b) {
    if (null == b) {
      throw new NullPointerException("Null byte array");
    }

    // Create and return the digest.
    synchronized (lock) {
      generator.update(b);
      return generator.digest();
    }
  }

  /**
   * Hash the specified byte arrays.
   *
   * @param   b1  The first byte array.
   * @param   b2  The second byte array.
   * @return      A secure hash over the specified values.
   * @throws  NullPointerException
   *              Signals that a byte array is <code>null</code>.
   */
  public byte[] hash(byte[] b1, byte[] b2) {
    if ((null == b1) || (null == b2)) {
      throw new NullPointerException("Null byte array");
    }

    synchronized (lock) {
      generator.update(b1);
      generator.update(b2);
      return generator.digest();
    }
  }

  /**
   * Hash the specified byte arrays.
   *
   * @param   b1  The first byte array.
   * @param   b2  The second byte array.
   * @param   b3  The third byte array.
   * @return      A secure hash over the specified values.
   * @throws  NullPointerException
   *              Signals that a byte array is <code>null</code>.
   */
  public byte[] hash(byte[] b1, byte[] b2, byte[] b3) {
    if ((null == b1) || (null == b2) || (null == b3)) {
      throw new NullPointerException("Null byte array");
    }

    synchronized (lock) {
      generator.update(b1);
      generator.update(b2);
      generator.update(b3);
      return generator.digest();
    }
  }

  /**
   * Convert the specified character array into a byte array.
   *
   * @param   s  The character array.
   * @return     The corresponding byte array.
   * @throws  NullPointerException
   *             Signals that <code>s</code> is <code>null</code>.
   */
  public static byte[] toBytes(char[] s) {
    byte[] data = new byte[s.length << 1];

    int dataIdx = 0;
    for (int i=0; i<s.length; i++) {
      int c           = s[i];
      data[dataIdx++] = (byte)c;
      data[dataIdx++] = (byte)(c >> 8);
    }

    return data;
  }

  /**
   * Convert the specified string into a byte array.
   *
   * @param   s  The string.
   * @return     The corresponding byte array.
   * @throws  NullPointerException
   *             Signals that <code>s</code> is <code>null</code>.
   */
  public static byte[] toBytes(String s) {
    int    l    = s.length();
    byte[] data = new byte[l << 1];

    int dataIdx = 0;
    for (int i=0; i<l; i++) {
      int c           = s.charAt(i);
      data[dataIdx++] = (byte)c;
      data[dataIdx++] = (byte)(c >> 8);
    }

    return data;
  }

  /**
   * Determine whether the specified byte arrays are equal.
   *
   * @param   b1  The first byte array.
   * @param   b2  The second byte array.
   * @return      <code>true</code> if the two byte arrays are equal.
   * @throws  NullPointerException
   *              Signals that <code>b1</code> or
   *              <code>b2</code> is <code>null</code>.
   */
  public static boolean isEqual(byte[] b1, byte[] b2) {
    if (b1.length != b2.length) return false;
    for (int i=0; i<b1.length; i++) {
      if (b1[i] != b2[i]) return false;
    }
    return true;
  }

  /**
   * Determine whether the specified character arrays are equal.
   *
   * @param   c1  The first character array.
   * @param   c2  The second character array.
   * @return      <code>true</code> if the two character arrays
   *              are equal.
   * @throws  NullPointerException
   *              Signals that <code>c1</code> or
   *              <code>c2</code> is <code>null</code>.
   */
  public static boolean isEqual(char[] c1, char[] c2) {
    if (c1.length != c2.length) return false;
    for (int i=0; i<c1.length; i++) {
      if (c1[i] != c2[i]) return false;
    }
    return true;
  }

}


