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

package one.eval;

/**
 * Implementation of a shell permission. A shell permission grants
 * the right to set a shell default.
 *
 * <p>The following names are defined:<ul>
 * <li><code>setEnvironment</code> &#150; to set the default
 * environment</li>
 * <li><code>setWriter</code> &#150; to set the default
 * writer</li>
 * <li><code>setReader</code> &#150; to set the default
 * reader</li>
 * <li><code>setFormat</code> &#150; to set the default
 * format</li>
 * </ul></p>
 *
 * @see      Shell
 * 
 * @author   &copy; Copyright 1998-2000 by Robert Grimm.
 *           Written by <a href="mailto:rgrimm@alum.mit.edu">Robert
 *           Grimm</a>.
 * @version  1.0
 */
public final class ShellPermission extends java.security.BasicPermission {

  /** The shell permission to set the default environment. */
  public static final ShellPermission SET_ENVIRONMENT =
    new ShellPermission("setEnvironment");

  /** The shell permission to set the defaul writer. */
  public static final ShellPermission SET_WRITER      =
    new ShellPermission("setWriter");

  /** The shell permission to set the default reader. */
  public static final ShellPermission SET_READER      =
    new ShellPermission("setReader");

  /** The shell permission to set the default format. */
  public static final ShellPermission SET_FORMAT      =
    new ShellPermission("setFormat");

  /**
   * Create a new shell permission with the specified name.
   *
   * @param  name  The name of the shell permission.
   */
  public ShellPermission(String name) {
    super(name);
  }

  /**
   * Create a new shell permission with the specified name and
   * action. The actions string is unused and should be
   * <code>null</code>.
   *
   * @param  name     The name of the shell permission.
   * @param  actions  The actions, which should be <code>null</code>.
   */
  public ShellPermission(String name, String actions) {
    super(name, actions);
  }

}
