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

package one.world.core;

/**
 * Implementation of a system permission. A system permission grants
 * the right to perform an operation necessary to implement the core
 * <i>one.world</i> architecture.
 *
 * <p>The following names are defined:<ul>
 * <li><code>startUp</code> &#150; to start up and shut down the
 * system</i>
 * <li><code>manageEnvironment</code> &#150; to create and manage an
 * environment</li>
 * <li><code>useConsole</code> &#150; to use the system console</li>
 * <li><code>useLog</code> &#150; to use the system log</li>
 * <li><code>spawnThread</code> &#150; to spawn threads in
 * addition to the threads provided by a concurrency domain</li>
 * </ul></p>
 *
 * @version  $Revision: 1.3 $
 * @author   Robert Grimm
 */
public class SystemPermission extends java.security.BasicPermission {

  /** The system permission to start up and shut down the system. */
  public static final SystemPermission START_UP =
    new SystemPermission("startUp");

  /** The system permission to manage environments. */
  public static final SystemPermission MANAGE_ENV =
    new SystemPermission("manageEnvironment");

  /** The system permission to use the system console. */
  public static final SystemPermission USE_CONSOLE =
    new SystemPermission("useConsole");

  /** The system permission to use the system log. */
  public static final SystemPermission USE_LOG =
    new SystemPermission("useLog");

  /** The system permission to spawn threads. */
  public static final SystemPermission SPAWN_THREAD =
    new SystemPermission("spawnThread");

  /**
   * Create a new system permission with the specified name.
   *
   * @param  name  The name of the system permission.
   */
  public SystemPermission(String name) {
    super(name);
  }

  /**
   * Create a new system permission with the specified name and
   * action. The actions string is unused and should be
   * <code>null</code>.
   *
   * @param  name     The name of the system permission.
   * @param  actions  The actions, which should be <code>null</code>.
   */
  public SystemPermission(String name, String actions) {
    super(name, actions);
  }

}
