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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import one.world.Constants;

import one.world.util.HandlerApplication;

/**
 * The definition of an animator. An animator executes code in a
 * concurrency domain by providing the necessary thread(s) to apply
 * event handlers on events. Since at any point in time there may be
 * more event handler applications ready for processing than threads
 * available in a concurrency domain, an animator maintains an
 * internal queue of pending event handler applications.
 *
 * <p>Animators may be concurrent, that is, use one or more threads at
 * any given time, or not, that is, always use only one thread.
 * Furthermore, animators may be able to maintain a variable number of
 * threads, limited by a low and a high watermark. An animator with a
 * variable number of threads may automatically terminate a thread if
 * that thread is idle for some predefined time. New threads are added
 * to such an animator by a {@link Controller}.</p>
 *
 * @version  $Revision: 1.11 $
 * @author   Robert Grimm
 */
public interface Animator {

  /**
   * Implementation of a controller. An animator controller
   * periodically scans all animators under its control. For each
   * animator, if the current size of the animator's queue is over the
   * busy threshold, the controller adds some number of threads to the
   * animator.
   *
   * <p>Note that an animator is removed from a controller if invoking
   * any of the animator's methods results in an illegal state
   * exception.</p>
   *
   * <p>Note that once started a controller will not terminate. A
   * controller should thus be run in its own daemon thread.</p>
   */
  public static class Controller implements Runnable {

    /** The scan time. */
    private final long      scanTime;

    /** The threshold. */
    private final int       threshold;

    /** The number of threads to add. */
    private final int       addThreads;

    /** The list of animators. */
    private final ArrayList animators;

    /** The lock. */
    private final Object    lock;

    /**
     * Create a new controller with the specified scan time,
     * threshold, and number of threads to add.
     *
     * @param   scanTime    The time between scans.
     * @param   threshold   The busy threshold.
     * @param   addThreads  The number of threads to add.
     * @throws  IllegalArgumentException
     *                      Signals that either number is not positive.
     */
    public Controller(long scanTime, int threshold, int addThreads) {
      if (0 >= scanTime) {
        throw new IllegalArgumentException("Non-positive scan time");
      } else if (0 >= threshold) {
        throw new IllegalArgumentException("Non-positive threshold");
      } else if (0 >= addThreads) {
        throw new IllegalArgumentException("Non-positive number of threads to" +
                                           " add");
      }

      this.scanTime   = scanTime;
      this.threshold  = threshold;
      this.addThreads = addThreads;
      animators       = new ArrayList();
      lock            = new Object();
    }

    /**
     * Add the specified animator to this controller. Adding an
     * animator that is already controlled by this controller has no
     * effect.
     *
     * @param   anim  The animator to add.
     * @throws  NullPointerException
     *                Signals that <code>anim</code> is <code>null</code>.
     */
    public void add(Animator anim) {
      if (null == anim) {
        throw new NullPointerException("Null animator");
      }

      synchronized (lock) {
        if (! animators.contains(anim)) {
          animators.add(anim);
        }
      }
    }

    /**
     * Remove the specified animator from this controller. Removing an
     * animator that is not controlled by this controller has no
     * effect.
     *
     * @param   anim  The animator to remove.
     */
    public void remove(Animator anim) {
      synchronized (lock) {
        animators.remove(anim);
      }
    }

    /** Run this controller. */
    public void run() {
      while (true) {
        synchronized (lock) {
          Iterator iter = animators.iterator();

          while (iter.hasNext()) {
            Animator anim = (Animator)iter.next();

            try {
              if (anim.getQueueSize() > threshold) {
                anim.addThreads(addThreads);
              }
            } catch (IllegalStateException x) {
              if (Constants.DEBUG_THREAD_CONTROL) {
                SystemLog.LOG.log(this, "Removing animator " + anim);
              }

              // The animator has terminated. We can remove it from
              // this controller.
              iter.remove();
            }
          }
        }

        // Nap between scans.
        try {
          Thread.sleep(scanTime);
        } catch (InterruptedException x) {
          // Ignore.
        }
      }
    }

  }

  /** The status code for an active animator. */
  int ACTIVE     = 1;

  /** The status code for an inactive animator. */
  int INACTIVE   = 2;

  /** The status code for a drained animator. */
  int DRAINED    = 3;

  /** The status code for a terminated animator. */
  int TERMINATED = 4;

  /**
   * Determine whether this animator is concurrent, that is, whether
   * it uses more than one thread.
   *
   * @return  <code>true</code> if this animator is concurrent.
   */
  boolean isConcurrent();

  /**
   * Get the minimum number of threads for this animator.
   *
   * @return     The minimum number of threads for this animator.
   * @throws  IllegalStateException
   *             Signals that this animator has terminated.
   */
  int getMinThreadNumber();

  /**
   * Get the current number of threads for this animator.
   *
   * @return     The number of threads for this animator.
   * @throws  IllegalStateException
   *             Signals that this animator has terminated.
   */
  int getThreadNumber();

  /**
   * Get the maximum number of threads for this animator.
   *
   * @return     The maximum numberf of threads for this animator.
   * @throws  IllegalStateException
   *             Signals that this animator has terminated.
   */
  int getMaxThreadNumber();

  /**
   * Set the minimum number of threads for this animator. If the new
   * minimum number of threads is larger than the current maximum
   * number of threads, the maximum number is also set to the
   * specified number of threads.
   *
   * @param   number  The new miminum number of threads for this
   *                  animator.
   * @throws  IllegalArgumentException
   *                  Signals that <code>number</code> is not positive.
   * @throws  IllegalStateException
   *                  Signals that this animator has terminated.
   * @throws  UnsupportedOperationException
   *                  Signals that the number of threads for this
   *                  animator is constant and cannot be changed.
   */
  void setMinThreadNumber(int number);

  /**
   * Add up to the specified number of threads to this animator. This
   * method adds zero or more threads to this animator and returns
   * that number. The actual number of threads added is the minimum of
   * the specified number and of the difference between the maximum
   * number of threads and the current number of threads for this
   * animator.
   *
   * @param   number  The number of threads to add to this animator.
   * @return          The number of threads actually added to this
   *                  animator.
   * @throws  IllegalArgumentException
   *                  Signals that <code>number</code> is not positive.
   * @throws  IllegalStateException
   *                  Signals that this animator has terminated.
   * @throws  UnsupportedOperationException
   *                  Signals that the number of threads for this
   *                  animator is constant and cannot be changed.
   */
  int addThreads(int number);

  /**
   * Set the maximum number of threads for this animator. If the new
   * maximum number of threads is smaller than the current minimum
   * number of threads, the minimum number is also set to the
   * specified number of threads.
   *
   * @param   number  The new maxmimum number of threads for this
   *                  animator.
   * @throws  IllegalArgumentException
   *                  Signals that <code>number</code> is not positive.
   * @throws  IllegalStateException
   *                  Signals that this animator has terminated.
   * @throws  UnsupportedOperationException
   *                  Signals that the number of threads for this
   *                  animator is constant and cannot be changed.
   */
  void setMaxThreadNumber(int number);

  /**
   * Get the current status for this animator.
   *
   * @see     #setStatus(int)
   *
   * @return  The current status for this animator.
   */
  int getStatus();

  /**
   * Set the status for this animator to the specified status. This
   * method changes the status of this animator if the specified
   * status is different from its current status. It returns only
   * after the status change is complete.
   *
   * <p>An active animator uses its thread(s) to process pending event
   * handler applications. An inactive animator still enqueues new
   * event handler applications, but all its threads are idle and not
   * executing pending event handler applications. A drained animator
   * does not enqueue new event handler applications and all its
   * threads are idle. Though, they only become idle after having
   * processed all pending event handler applications.  A terminated
   * animator has released its thread(s) and does not accept new event
   * handler applications.</p>
   *
   * <p>Valid state transitions are as follows: From the active state
   * to either the inactive or the drained state. From the inactive or
   * the drained state to any of the four states. An animator cannot
   * leave the terminated state, once it is in that state.</p>
   *
   * @param   status  The new status for this animator.
   * @throws  IllegalArgumentException
   *                  Signals that <code>status</code> is not
   *                  a valid status code or that the requested
   *                  status results in an invalid state transition.
   * @throws  IllegalStateException
   *                  Signals that this animator has terminated.
   */
  void setStatus(int status);

  /**
   * Enqueue the specified event handler application.
   *
   * @param   handler  The handler to apply.
   * @param   event    The event to apply the handler on.
   * @return           <code>true</code> if the event handler
   *                   application has been successfully
   *                   enqueued.
   * @throws  IllegalStateException
   *                   Signals that this animator has
   *                   terminated.
   */
  boolean enqueue(EventHandler handler, Event event);

  /**
   * Enqueue the specified event handler application in front of the
   * queue.
   *
   * @param   handler  The handler to apply.
   * @param   event    The event to apply the handler on.
   * @return           <code>true</code> if the event handler
   *                   application has been successfully
   *                   enqueued.
   * @throws  IllegalStateException
   *                   Signals that this animator has
   *                   terminated.
   */
  boolean enqueueFirst(EventHandler handler, Event event);

  /**
   * Forcibly enqueue the specified event handler application in front
   * of the queue. If this animator's queue is full, the specified
   * event handler application replaces the event handler application
   * at the head of the queue. Furthermore, if this animator is
   * drained, the specified event handler application is still
   * enqueued.
   *
   * @param   handler  The handler to apply.
   * @param   event    The event to apply the handler on.
   * @throws  IllegalStateException
   *                   Signals that this animator has
   *                   terminated.
   */
  void enqueueForced(EventHandler handler, Event event);

  /**
   * Get the capacity of this animator's queue or -1 if the capacity
   * is unlimited.
   *
   * @return     The capacity of this animator's queue.
   * @throws  IllegalStateException
   *             Signals that this animator has terminated.
   */
  int getQueueCapacity();

  /**
   * Determine whether this animator's queue is empty.
   *
   * @return     <code>true</code> if this animator's queue is empty.
   * @throws  IllegalStateException
   *             Signals that this animator has terminated.
   */
  boolean isQueueEmpty();

  /**
   * Get the current size of this animator's queue.
   *
   * @return     The current size of this animator's queue.
   * @throws  IllegalStateException
   *             Signals that this animator has terminated.
   */
  int getQueueSize();

  /**
   * Get a list of event handler applications for this animator's
   * queue. This method is useful for debugging because it enables a
   * dump of an animator's queue.
   *
   * @see     HandlerApplication
   *
   * @return     A list of event handler applications for this
   *             animator's queue.
   * @throws  IllegalStateException
   *             Signals that this animator has terminated.
   */
  List getQueue();

}
