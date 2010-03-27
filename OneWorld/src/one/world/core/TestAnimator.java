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

import one.fonda.TestCollection;
import one.fonda.Harness;

import one.util.Guid;
import one.util.Bug;

import one.world.Constants;

/**
 * Implementation of regression tests on animators.
 *
 * @version   $Revision: 1.8 $
 * @author    Robert Grimm
 */
public class TestAnimator implements TestCollection {

  /** A counter. */
  int                counter;

  /** The lock for the counter. */
  final Object       lock;

  /** An event handler that increases the counter. */
  final EventHandler handler;

  /** Some event. */
  final Event        event;

  /** Create a new animator test. */
  public TestAnimator() {
    lock    = new Object();
    handler = new EventHandler() {
        public void handle(Event event) {
          // Ignore event.
          synchronized (lock) {
            counter++;
          }
        }
      };
    event   = new DynamicTuple();
  }

  /** Get the name of this test collection. */
  public String getName() {
    return "one.world.core.TestAnimator";
  }

  /** Get a description for this test collection. */
  public String getDescription() {
    return "Test state transitions for animators";
  }

  /** Get the number of tests. */
  public int getTestNumber() {
    return 32;
  }

  /** Determine whether this test collection needs an environment. */
  public boolean needsEnvironment() {
    return false;
  }

  /** Initialize this test collection. */
  public boolean initialize(Environment env) {
    return false;
  }

  /** Run the specified test. */
  public Object runTest(int number, Harness h, boolean verbose)
    throws Throwable {

    boolean exceptional = false;
    int     size;

    ConcurrencyDomain conc =
      Environment.createNestedConcurrency("TestAnimator");

    Animator anim;
    
    if (16 >= number) {
      anim = new EventLoop(conc);
    } else {
      anim = new ThreadPool(conc, Constants.ANIMATOR_CAPACITY, 5, 20);
    }

    reset();

    switch(number) {

    case 1:
      h.enterTest(1, "EventLoop - inactive to active", box(3));
      anim.enqueue(handler, event);
      anim.enqueue(handler, event);
      anim.enqueue(handler, event);
      anim.setStatus(Animator.ACTIVE);
      Thread.sleep(1000);
      anim.setStatus(Animator.INACTIVE);
      anim.setStatus(Animator.TERMINATED);
      return box(count());

    case 2:
      h.enterTest(2, "EventLoop - inactive to active to inactive", box(3));
      anim.enqueue(handler, event);
      anim.enqueue(handler, event);
      anim.enqueue(handler, event);
      anim.setStatus(Animator.ACTIVE);
      Thread.sleep(1000);
      anim.setStatus(Animator.INACTIVE);
      anim.enqueue(handler, event);
      anim.enqueue(handler, event);
      anim.enqueue(handler, event);
      Thread.sleep(1000);
      anim.setStatus(Animator.TERMINATED);
      return box(count());

    case 3:
      h.enterTest(3, "EventLoop - inactive to drained", box(3));
      anim.enqueue(handler, event);
      anim.enqueue(handler, event);
      anim.enqueue(handler, event);
      anim.setStatus(Animator.DRAINED);
      anim.setStatus(Animator.TERMINATED);
      return box(count());

    case 4:
      h.enterTest(4, "EventLoop - inactive to drained to inactive", box(3));
      anim.enqueue(handler, event);
      anim.enqueue(handler, event);
      anim.enqueue(handler, event);
      anim.setStatus(Animator.DRAINED);
      anim.setStatus(Animator.INACTIVE);
      anim.enqueue(handler, event);
      anim.enqueue(handler, event);
      anim.enqueue(handler, event);
      Thread.sleep(1000);
      anim.setStatus(Animator.TERMINATED);
      return box(count());

    case 5:
      h.enterTest(5, "EventLoop - drained to active", box(0));
      anim.setStatus(Animator.DRAINED);
      anim.enqueue(handler, event);
      anim.enqueue(handler, event);
      anim.enqueue(handler, event);
      anim.setStatus(Animator.ACTIVE);
      Thread.sleep(1000);
      anim.setStatus(Animator.INACTIVE);
      anim.setStatus(Animator.TERMINATED);
      return box(count());

    case 6:
      h.enterTest(6, "EventLoop - drained to active to drained", box(3));
      anim.setStatus(Animator.DRAINED);
      anim.enqueue(handler, event);
      anim.enqueue(handler, event);
      anim.enqueue(handler, event);
      anim.setStatus(Animator.ACTIVE);
      Thread.sleep(1000);
      anim.enqueue(handler, event);
      anim.enqueue(handler, event);
      anim.enqueue(handler, event);
      anim.setStatus(Animator.DRAINED);
      anim.enqueue(handler, event);
      anim.enqueue(handler, event);
      anim.enqueue(handler, event);
      Thread.sleep(1000);
      anim.setStatus(Animator.TERMINATED);
      return box(count());

    case 7:
      h.enterTest(7, "EventLoop - active to terminated", Boolean.TRUE);
      anim.setStatus(Animator.ACTIVE);
      try {
        anim.setStatus(Animator.TERMINATED);
      } catch (IllegalArgumentException x) {
        exceptional = true;
      }

      anim.setStatus(Animator.INACTIVE);
      anim.setStatus(Animator.TERMINATED);
      return box(exceptional);

    case 8:
      h.enterTest(8, "EventLoop - terminated to active", Boolean.TRUE);
      anim.setStatus(Animator.TERMINATED);
      try {
        anim.setStatus(Animator.ACTIVE);
      } catch (IllegalStateException x) {
        exceptional = true;
      }
      return box(exceptional);

    case 9:
      h.enterTest(9, "EventLoop - terminated to inactive", Boolean.TRUE);
      anim.setStatus(Animator.TERMINATED);
      try {
        anim.setStatus(Animator.INACTIVE);
      } catch (IllegalStateException x) {
        exceptional = true;
      }
      return box(exceptional);

    case 10:
      h.enterTest(10, "EventLoop - terminated to drained", Boolean.TRUE);
      anim.setStatus(Animator.TERMINATED);
      try {
        anim.setStatus(Animator.DRAINED);
      } catch (IllegalStateException x) {
        exceptional = true;
      }
      return box(exceptional);

    case 11:
      h.enterTest(11, "EventLoop - inactive to active, queue size", box(0));
      anim.enqueue(handler, event);
      anim.enqueue(handler, event);
      anim.enqueue(handler, event);
      anim.setStatus(Animator.ACTIVE);
      Thread.sleep(1000);
      anim.setStatus(Animator.INACTIVE);
      size = anim.getQueueSize();
      anim.setStatus(Animator.TERMINATED);
      return box(size);

    case 12:
      h.enterTest(12, "EventLoop - inactive to active to inactive, queue size",
                  box(3));
      anim.enqueue(handler, event);
      anim.enqueue(handler, event);
      anim.enqueue(handler, event);
      anim.setStatus(Animator.ACTIVE);
      Thread.sleep(1000);
      anim.setStatus(Animator.INACTIVE);
      anim.enqueue(handler, event);
      anim.enqueue(handler, event);
      anim.enqueue(handler, event);
      Thread.sleep(1000);
      size = anim.getQueueSize();
      anim.setStatus(Animator.TERMINATED);
      return box(size);

    case 13:
      h.enterTest(13, "EventLoop - inactive to drained, queue size", box(0));
      anim.enqueue(handler, event);
      anim.enqueue(handler, event);
      anim.enqueue(handler, event);
      anim.setStatus(Animator.DRAINED);
      size = anim.getQueueSize();
      anim.setStatus(Animator.TERMINATED);
      return box(size);

    case 14:
      h.enterTest(14, "EventLoop - inactive to drained to inactive, queue size",
                  box(3));
      anim.enqueue(handler, event);
      anim.enqueue(handler, event);
      anim.enqueue(handler, event);
      anim.setStatus(Animator.DRAINED);
      anim.setStatus(Animator.INACTIVE);
      anim.enqueue(handler, event);
      anim.enqueue(handler, event);
      anim.enqueue(handler, event);
      Thread.sleep(1000);
      size = anim.getQueueSize();
      anim.setStatus(Animator.TERMINATED);
      return box(size);

    case 15:
      h.enterTest(15, "EventLoop - drained to active, queue size", box(0));
      anim.setStatus(Animator.DRAINED);
      anim.enqueue(handler, event);
      anim.enqueue(handler, event);
      anim.enqueue(handler, event);
      anim.setStatus(Animator.ACTIVE);
      Thread.sleep(1000);
      size = anim.getQueueSize();
      anim.setStatus(Animator.INACTIVE);
      anim.setStatus(Animator.TERMINATED);
      return box(size);

    case 16:
      h.enterTest(16, "EventLoop - drained to active to drained, queue size",
                  box(0));
      anim.setStatus(Animator.DRAINED);
      anim.enqueue(handler, event);
      anim.enqueue(handler, event);
      anim.enqueue(handler, event);
      anim.setStatus(Animator.ACTIVE);
      Thread.sleep(1000);
      anim.enqueue(handler, event);
      anim.enqueue(handler, event);
      anim.enqueue(handler, event);
      anim.setStatus(Animator.DRAINED);
      anim.enqueue(handler, event);
      anim.enqueue(handler, event);
      anim.enqueue(handler, event);
      Thread.sleep(1000);
      size = anim.getQueueSize();
      anim.setStatus(Animator.TERMINATED);
      return box(size);

    case 17:
      h.enterTest(17, "ThreadPool - inactive to active", box(3));
      anim.enqueue(handler, event);
      anim.enqueue(handler, event);
      anim.enqueue(handler, event);
      anim.setStatus(Animator.ACTIVE);
      Thread.sleep(1000);
      anim.setStatus(Animator.INACTIVE);
      anim.setStatus(Animator.TERMINATED);
      return box(count());

    case 18:
      h.enterTest(18, "ThreadPool - inactive to active to inactive", box(3));
      anim.enqueue(handler, event);
      anim.enqueue(handler, event);
      anim.enqueue(handler, event);
      anim.setStatus(Animator.ACTIVE);
      Thread.sleep(1000);
      anim.setStatus(Animator.INACTIVE);
      anim.enqueue(handler, event);
      anim.enqueue(handler, event);
      anim.enqueue(handler, event);
      Thread.sleep(1000);
      anim.setStatus(Animator.TERMINATED);
      return box(count());

    case 19:
      h.enterTest(19, "ThreadPool - inactive to drained", box(3));
      anim.enqueue(handler, event);
      anim.enqueue(handler, event);
      anim.enqueue(handler, event);
      anim.setStatus(Animator.DRAINED);
      anim.setStatus(Animator.TERMINATED);
      return box(count());

    case 20:
      h.enterTest(20, "ThreadPool - inactive to drained to inactive", box(3));
      anim.enqueue(handler, event);
      anim.enqueue(handler, event);
      anim.enqueue(handler, event);
      anim.setStatus(Animator.DRAINED);
      anim.setStatus(Animator.INACTIVE);
      anim.enqueue(handler, event);
      anim.enqueue(handler, event);
      anim.enqueue(handler, event);
      Thread.sleep(1000);
      anim.setStatus(Animator.TERMINATED);
      return box(count());

    case 21:
      h.enterTest(21, "ThreadPool - drained to active", box(0));
      anim.setStatus(Animator.DRAINED);
      anim.enqueue(handler, event);
      anim.enqueue(handler, event);
      anim.enqueue(handler, event);
      anim.setStatus(Animator.ACTIVE);
      Thread.sleep(1000);
      anim.setStatus(Animator.INACTIVE);
      anim.setStatus(Animator.TERMINATED);
      return box(count());

    case 22:
      h.enterTest(22, "ThreadPool - drained to active to drained", box(3));
      anim.setStatus(Animator.DRAINED);
      anim.enqueue(handler, event);
      anim.enqueue(handler, event);
      anim.enqueue(handler, event);
      anim.setStatus(Animator.ACTIVE);
      Thread.sleep(1000);
      anim.enqueue(handler, event);
      anim.enqueue(handler, event);
      anim.enqueue(handler, event);
      anim.setStatus(Animator.DRAINED);
      anim.enqueue(handler, event);
      anim.enqueue(handler, event);
      anim.enqueue(handler, event);
      Thread.sleep(1000);
      anim.setStatus(Animator.TERMINATED);
      return box(count());

    case 23:
      h.enterTest(23, "ThreadPool - active to terminated", Boolean.TRUE);
      anim.setStatus(Animator.ACTIVE);
      try {
        anim.setStatus(Animator.TERMINATED);
      } catch (IllegalArgumentException x) {
        exceptional = true;
      }

      anim.setStatus(Animator.INACTIVE);
      anim.setStatus(Animator.TERMINATED);
      return box(exceptional);

    case 24:
      h.enterTest(24, "ThreadPool - terminated to active", Boolean.TRUE);
      anim.setStatus(Animator.TERMINATED);
      try {
        anim.setStatus(Animator.ACTIVE);
      } catch (IllegalStateException x) {
        exceptional = true;
      }
      return box(exceptional);

    case 25:
      h.enterTest(25, "ThreadPool - terminated to inactive", Boolean.TRUE);
      anim.setStatus(Animator.TERMINATED);
      try {
        anim.setStatus(Animator.INACTIVE);
      } catch (IllegalStateException x) {
        exceptional = true;
      }
      return box(exceptional);

    case 26:
      h.enterTest(26, "ThreadPool - terminated to drained", Boolean.TRUE);
      anim.setStatus(Animator.TERMINATED);
      try {
        anim.setStatus(Animator.DRAINED);
      } catch (IllegalStateException x) {
        exceptional = true;
      }
      return box(exceptional);

    case 27:
      h.enterTest(27, "ThreadPool - inactive to active, queue size", box(0));
      anim.enqueue(handler, event);
      anim.enqueue(handler, event);
      anim.enqueue(handler, event);
      anim.setStatus(Animator.ACTIVE);
      Thread.sleep(1000);
      anim.setStatus(Animator.INACTIVE);
      size = anim.getQueueSize();
      anim.setStatus(Animator.TERMINATED);
      return box(size);

    case 28:
      h.enterTest(28, "ThreadPool - inactive to active to inactive, queue size",
                  box(3));
      anim.enqueue(handler, event);
      anim.enqueue(handler, event);
      anim.enqueue(handler, event);
      anim.setStatus(Animator.ACTIVE);
      Thread.sleep(1000);
      anim.setStatus(Animator.INACTIVE);
      anim.enqueue(handler, event);
      anim.enqueue(handler, event);
      anim.enqueue(handler, event);
      Thread.sleep(1000);
      size = anim.getQueueSize();
      anim.setStatus(Animator.TERMINATED);
      return box(size);

    case 29:
      h.enterTest(29, "ThreadPool - inactive to drained, queue size", box(0));
      anim.enqueue(handler, event);
      anim.enqueue(handler, event);
      anim.enqueue(handler, event);
      anim.setStatus(Animator.DRAINED);
      size = anim.getQueueSize();
      anim.setStatus(Animator.TERMINATED);
      return box(size);

    case 30:
      h.enterTest(30,
                  "ThreadPool - inactive to drained to inactive, queue size",
                  box(3));
      anim.enqueue(handler, event);
      anim.enqueue(handler, event);
      anim.enqueue(handler, event);
      anim.setStatus(Animator.DRAINED);
      anim.setStatus(Animator.INACTIVE);
      anim.enqueue(handler, event);
      anim.enqueue(handler, event);
      anim.enqueue(handler, event);
      Thread.sleep(1000);
      size = anim.getQueueSize();
      anim.setStatus(Animator.TERMINATED);
      return box(size);

    case 31:
      h.enterTest(31, "ThreadPool - drained to active, queue size", box(0));
      anim.setStatus(Animator.DRAINED);
      anim.enqueue(handler, event);
      anim.enqueue(handler, event);
      anim.enqueue(handler, event);
      anim.setStatus(Animator.ACTIVE);
      Thread.sleep(1000);
      size = anim.getQueueSize();
      anim.setStatus(Animator.INACTIVE);
      anim.setStatus(Animator.TERMINATED);
      return box(size);

    case 32:
      h.enterTest(32, "ThreadPool - drained to active to drained, queue size",
                  box(0));
      anim.setStatus(Animator.DRAINED);
      anim.enqueue(handler, event);
      anim.enqueue(handler, event);
      anim.enqueue(handler, event);
      anim.setStatus(Animator.ACTIVE);
      Thread.sleep(1000);
      anim.enqueue(handler, event);
      anim.enqueue(handler, event);
      anim.enqueue(handler, event);
      anim.setStatus(Animator.DRAINED);
      anim.enqueue(handler, event);
      anim.enqueue(handler, event);
      anim.enqueue(handler, event);
      Thread.sleep(1000);
      size = anim.getQueueSize();
      anim.setStatus(Animator.TERMINATED);
      return box(size);

    default:
      throw new Bug("Invalid test number " + number);
    }
  }

  /** Clean up this test collection. */
  public void cleanup() {
    // Nothing to do.
  }

  /**
   * Get the value of the counter for this animator test.
   *
   * @return  The value of the counter for this animator test.
   */
  private int count() {
    synchronized (lock) {
      return counter;
    }
  }

  /** Reset the counter for this animator test. */
  private void reset() {
    synchronized (lock) {
      counter = 0;
    }
  }

  /**
   * Box the specified integer.
   *
   * @param   n  The integer to box.
   * @return     The boxed integer.
   */
  private Integer box(int n) {
    return new Integer(n);
  }

  /**
   * Box the specified boolean.
   *
   * @param   b  The boolean to box.
   * @return     The boxed boolean.
   */
  private Boolean box(boolean b) {
    return ((b)? Boolean.TRUE : Boolean.FALSE);
  }

}
