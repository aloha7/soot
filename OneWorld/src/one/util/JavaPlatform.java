/*
 * Copyright (c) 2001, Robert Grimm.
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

/**
 * Implementation of utility methods to determine the supported Java
 * specification.
 *
 * @author   &copy; Copyright 2001 by Robert Grimm.
 *           Written by <a href="mailto:rgrimm@alum.mit.edu">Robert
 *           Grimm</a>.
 * @version  $Revision: 1.4 $
 */
public class JavaPlatform {

  private JavaPlatform() {
    // Nothing to construct.
  }

  /** Flag for JDK 0.x/1.0/1.1. */
  private static final int JDK_11 = 1;

  /** Flag for JDK 1.2. */
  private static final int JDK_12 = 2;

  /** Flag for JDK 1.3. */
  private static final int JDK_13 = 3;

  /** Flag for JDK 1.4. */
  private static final int JDK_14 = 4;

  /**
   * The platform supported by the Java virtual machine running this
   * code.
   */
  private static final int version;

  static {
    String s = System.getProperty("java.specification.version");

    if ((null == s) ||
        s.startsWith("0.") || s.startsWith("1.0") || s.startsWith("1.1")) {
      version = JDK_11;
    } else if (s.startsWith("1.2")) {
      version = JDK_12;
    } else if (s.startsWith("1.3")) {
      version = JDK_13;
    } else {
      version = JDK_14;
    }
  }

  /**
   * Determine whether this Java virtual machine includes support for
   * at least the JDK 1.2 platform.
   *
   * @return  <code>true</code> if this virtual machine supports the
   *          JDK 1.2 paltform.
   */
  public static boolean includesJDK12() {
    return (JDK_12 <= version);
  }

  /**
   * Determine whether this Java virtual machine includes support for
   * at least the JDK 1.3 platform.
   *
   * @return  <code>true</code> if this virtual machine supports the
   *          JDK 1.3 paltform.
   */
  public static boolean includesJDK13() {
    return (JDK_13 <= version);
  }

  /**
   * Determine whether this Java virtual machine includes support for
   * at least the JDK 1.4 platform.
   *
   * @return  <code>true</code> if this virtual machine supports the
   *          JDK 1.4 paltform.
   */
  public static boolean includesJDK14() {
    return (JDK_14 <= version);
  }

}
