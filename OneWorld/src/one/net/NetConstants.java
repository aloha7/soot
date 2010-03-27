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

/**
 * Definition of constants generally useful for implementing Internet
 * protocols.
 *
 * @version  $Revision: 1.3 $
 * @author   Robert Grimm
 */
public interface NetConstants {

  /** The default telnet port. */
  int    PORT_TELNET         = 23;

  /** The seven-bit ASCII encoding. */
  String ENCODING_ASCII      = "US-ASCII";

  /** The UTF-8 encoding. */
  String ENCODING_UTF8       = "UTF-8";

  /** The character code for the bell character. */
  int    CHAR_BELL           =   7;

  /** The character code for the back-space character. */
  int    CHAR_BACKSPACE      =   8;

  /** The character code for the escape character. */
  int    CHAR_ESCAPE         =  27;

  /** The character code for the space character. */
  int    CHAR_SPACE          =  32;

  /** The charcter code for the delete character. */
  int    CHAR_DELETE         = 127;

  /** The carriage return, line-feed sequence. */
  String CRLF                = "\r\n";

  /**
   * The default file name extension for unknown MIME types,
   * "<code>bin</code>".
   */
  public static final String EXTENSION_DEFAULT = "bin";

  /**
   * The file name extension for binary tuples, "<code>btpl</code>".
   */
  public static final String EXTENSION_BTUPLE  = "btpl";

  /**
   * The file name extension for XML-based tuples, "<code>tpl</code>".
   */
  public static final String EXTENSION_TUPLE   = "tpl";    

  /**
   * The file name extension for Java class files, "<code>class</code>".
   */
  public static final String EXTENSION_CLASS   = "class";

  /**
   * The default MIME type for unknown file name extensions,
   * "<code>application/octet-stream</code>".
   */
  public static final String MIME_TYPE_DEFAULT = "application/octet-stream";

  /**
   * The MIME type for binary tuples,
   * "<code>application/x-one-world-btpl</code>".
   */
  public static final String MIME_TYPE_BTUPLE  = "application/x-one-world-btpl";

  /**
   * The MIME type for XML-based tuples,
   * "<code>application/x-one-world-tpl</code>".
   */
  public static final String MIME_TYPE_TUPLE   = "application/x-one-world-tpl";

  /**
   * The MIME type for Java class data tuples,
   * "<code>application/x-java-vm</code>".
   */
  public static final String MIME_TYPE_CLASS   = "application/x-java-vm";

}
