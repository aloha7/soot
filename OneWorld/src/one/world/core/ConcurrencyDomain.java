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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import one.util.Guid;

import one.world.Constants;

/**
 * Implementation of a concurrency domain. A concurrency domain
 * provides an animator for an environment. It is associated with an
 * environment when the environment is created and cannot be changed
 * afterwards. The ID of a concurrency domain is the same ID as the ID
 * of the corresponding environment.
 *
 * @see      Animator
 * @see      Environment
 *
 * @version  $Revision: 1.29 $
 * @author   Robert Grimm
 */
public class ConcurrencyDomain extends Domain implements java.io.Serializable {

  /** Serial version ID for this class. */
  static final long serialVersionUID = -5031437292160898095L;

  // =======================================================================
  //                           Instance fields
  // =======================================================================

  /**
   * The animator for this concurrency domain.
   *
   * @serial  Must not be <code>null</code> if this concurrency
   *          domain's environment is active.
   */
  volatile Animator anim;


  // =======================================================================
  //                            Constructor
  // =======================================================================

  /**
   * Create a new concurrency domain. The specified ID must be the
   * same ID as the ID of the corresponding environment.
   *
   * @param   id  The ID for the newly created concurrency domain.
   */
  ConcurrencyDomain(Guid id) {
    super(id);
  }
  

  // =======================================================================
  //                             Serialization
  // =======================================================================

  /**
   * Serialize this concurrency domain.
   *
   * @serialData     The default fields followed by this concurrency
   *                 domain's ID.
   * @throws      IllegalStateException
   *                 Signals that this concurrency domain is being
   *                 serialized outside a check-point.
   */
  private void writeObject(ObjectOutputStream out) throws IOException {
    out.defaultWriteObject();

    // Resolve ID of concurrency domain.
    Guid id2 = Environment.resolveId(id);

    // Sanity check.
    if (null == id2) {
      throw new IllegalStateException("Attempting to serialize concurrency " +
                                      "domain outside check-point ("+this+")");
    }
    
    // Write out the resolved ID.
    out.writeObject(id2);
  }

  /** Deserialize a concurrency domain. */
  private void readObject(ObjectInputStream in)
    throws IOException, ClassNotFoundException {

    in.defaultReadObject();

    id = (Guid)in.readObject();
  }


  // =======================================================================
  //                     Functionality for environments
  // =======================================================================

  /**
   * Animate this concurrency domain. If this concurrency domain has
   * not been associated with an animator, this method creates a new
   * animator in the active state.
   *
   * <p>Invocations of this method must be synchronized
   * externally.</p>
   *
   * @param   singleThreaded  Flag for whether the newly created animator
   *                          should be single-threaded.
   * @param   isRoot          Flag for whether this is the root
   *                          environment's main concurrency domain.
   * @throws  SecurityException
   *                          Signals that the caller does not have
   *                          permission to manage environments.
   */
  public final void animate(boolean singleThreaded, boolean isRoot) {
    Environment.ensurePermission();

    if (null == anim) {
      if (singleThreaded) {
        anim = new EventLoop(this);
      } else {
        if (isRoot) {
          anim = new ThreadPool(this,
                                Constants.ANIMATOR_CAPACITY,
                                Constants.ANIMATOR_ROOT_MIN_THREADS,
                                Constants.ANIMATOR_ROOT_MAX_THREADS);
        } else {
          anim = new ThreadPool(this);
        }
      }

      anim.setStatus(Animator.ACTIVE);
    }
  }

  /**
   * Terminate this concurrency domain. If this concurrency domain has
   * not been terminated, this method terminates the concurrency
   * domain and releases the concurrency domain's animator.
   *
   * <p>Invocations of this method must be synchronized
   * externally.</p>
   *
   * @throws  SecurityException
   *             Signals that the caller does not have permission
   *             to manage environments.
   */
  public final void terminate() {
    Environment.ensurePermission();

    Animator a = anim;

    if (null != a) {
      int status = a.getStatus();
      if (Animator.ACTIVE == status) {
        a.setStatus(Animator.INACTIVE);
        status = Animator.INACTIVE;
      }
      if (Animator.TERMINATED != status) {
        a.setStatus(Animator.TERMINATED);
      }

      anim = null;
    }
  }


  // =======================================================================
  //                          Public functionality
  // =======================================================================

  /**
   * Determine whether this concurrency domain's animator is
   * concurrent, that is, if it can use more than one thread.
   *
   * @see     Animator#isConcurrent()
   *
   * @return     <code>true</code> if this concurrency domain's
   *             animator is concurrent.
   * @throws  IllegalStateException
   *             Signals that this concurrency domain is not active.
   */
  public final boolean isConcurrent() {
    Animator a = anim;

    if (null == a) {
      throw new IllegalStateException("Concurrency domain inactive (" +
                                      id + ")");
    } else {
      return a.isConcurrent();
    }
  }

  /**
   * Get the current number of threads for this concurrency domain's
   * animator.
   *
   * @see     Animator#getThreadNumber()
   * 
   * @return     The number of threads for this concurrency domain's
   *             animator.
   * @throws  IllegalStateException
   *             Signals that this concurrency domain is not active.
   */
  public final int getThreadNumber() {
    Animator a = anim;

    if (null == a) {
      throw new IllegalStateException("Concurrency domain inactive (" +
                                      id + ")");
    } else {
      return a.getThreadNumber();
    }
  }

  /** Return a string representation of this concurrency domain. */
  public String toString() {
    return "#[Concurrency domain " + id.toString() + "]";
  }

}
