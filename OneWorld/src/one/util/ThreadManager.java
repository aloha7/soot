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

/**
 * Definition of a thread manager. A thread manager is responsible for
 * managing an application's threads, for example, by implementing a
 * pool of preallocated and unused threads. This interface defines a
 * single method to fork a logically fresh thread, that is, a thread
 * that is not currently executing any application code but is alive.
 *
 * @see      Thread
 *
 * @author   &copy; Copyright 1999 by Robert Grimm.
 *           Written by <a href="mailto:rgrimm@alum.mit.edu">Robert
 *           Grimm</a>.
 * @version  1.0
 */
public interface ThreadManager {

  /**
   * Run the specified runnable in a logically fresh thread. A
   * logically fresh thread is a thread that is not currently
   * executing any application code but is alive. An implementation of
   * this method must execute the <code>run()</code> method of the
   * specified <code>Runnable</code> in its own thread of execution
   * and return a reference to this thread.
   *
   * <p>An implementation of this interface may reuse the returned
   * thread as soon as the <code>run()</code> method of the specified
   * <code>Runnable</code> returns.
   * 
   * <p>If a client of this interface needs to determine whether a
   * particular computation has terminated, that is, whether the
   * <code>run()</code> method of a <code>Runnable</code> has
   * returned, it must implement a separate interface as part of the
   * <code>Runnable</code> object. It can not rely on the
   * <code>isAlive()</code> method of the returned
   * thread. Furthermore, clients of this interface must not use any
   * depracted methods on the returned thread object.
   *
   * @param   r  The runnable to run.
   * @return     The thread animating the specified runnable.
   */
  Thread run(Runnable r);

}
