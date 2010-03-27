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

import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.IOException;
import java.io.NotSerializableException;

import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;

import one.world.Constants;

import one.world.util.HandlerApplication;

/**
 * Implementation of a thread pool. A thread pool is an animator that
 * uses one or more threads to process enqueued event handler
 * applications. Note that thread pools are always considered to be
 * concurrent, even if the current number of threads in the thread
 * pool is 1.
 *
 * @version  $Revision: 1.24 $
 * @author   Robert Grimm
 */
public final class ThreadPool implements Animator, java.io.Serializable {
  
  /** The serial version ID for this class. */
  static final long serialVersionUID = 4578900683834992038L;

  // =======================================================================
  //                            Worker thread
  // =======================================================================

  /** The worker thread for the thread pool. */
  final class Worker extends Thread {

    /** Create a new worker. */
    public Worker() {
      super(concurrency.toString());
    }

    /** Run this worker. */
    public void run() {
      boolean looping = false;

      do {
        EventHandler handler;
        Event        event;

        synchronized (monitor) {
          if (looping) {
            // Flag that we are not running and notify a waiting thread.
            busy.remove(this);
            idle.add(this);

            // Notify thread in setStatus() that status transition is
            // complete.
            if ((INACTIVE == status) &&
                (0 == busy.size())) {
              // Need to notify all to make sure that thread in
              // setStatus() gets woken up.
              monitor.notifyAll();
            } else if ((DRAINED == status) &&
                       (-1 == tail)) {
              // Need to notify all to make sure that thread in
              // setStatus() gets woken up.
              monitor.notifyAll();
            }
          } else {
            looping = true;
          }

          boolean hasWaited = false;
          do {
            // Check status.
            if (TERMINATED == status) {
              // Flag that we are done.
              idle.remove(this);
              if ((0 == idle.size()) && (0 == busy.size())) {
                monitor.notify();
              }
              return;

            } else if (threadNumber > maxThreadNumber) {
              // The number of threads has been reduced.
              idle.remove(this);
              threadNumber--;
              return;

            } else if ((0 > tail) || (INACTIVE == status)) {
              if (hasWaited && (threadNumber > minThreadNumber)) {
                // We are bored and therefore commit suicide.
                idle.remove(this);
                threadNumber--;
                if (Constants.DEBUG_THREAD_CONTROL) {
                  SystemLog.LOG.log(ThreadPool.this, "Exiting idle thread");
                }
                return;
              }

              try {
                monitor.wait(Constants.ANIMATOR_IDLE_TIME);
              } catch (InterruptedException x) {
                // Ignore.
              }
              hasWaited = true;

              continue;
            }

            break;
          } while (true);

          // Get event handler and event for application.
          handler        = handlers[head];
          event          = events[head];
          handlers[head] = null;            // Let GC do its work.
          events[head]   = null;
          
          head++;
          
          if (handlers.length <= head) {
            head = 0;
          }
          if (tail == head) {
            tail = -1;
          }

          // Flag that we are running.
          idle.remove(this);
          busy.add(this);
        }
        
        // Apply event handler on event.
        try {
          handler.handle(event);
        } catch (Throwable x) {
          EventHandler h;
          if (Domain.isWrapped(handler)) {
            h = Domain.unwrap(handler);
          } else {
            h = handler;
          }

          SystemLog.LOG.logError(this, "Unexpected exception when applying " +
                                 "event handler " + h + " on " + event, x);
        }

      } while (true);
    }

  }


  // =======================================================================
  //                             Instance fields
  // =======================================================================

  /** The concurrency domain for this thread pool. */
  final ConcurrencyDomain   concurrency;

  /**
   * The status for this thread pool.
   *
   * @serial  Must either be the active, inactive, or terminated
   *          status code.
   */
  volatile int              status;

  /**
   * The array of handlers for the circular buffer implementing this
   * thread pool's queue.
   *
   * @serial  If <code>status</code> is not terminated,
   *          <code>handlers</code> must be an array with at least one
   *          entry and the same length as
   *          <code>events</code>. Entries that are between the head
   *          and tail of the circular buffer must not be
   *          <code>null</code>.
   */
  EventHandler[]            handlers;

  /**
   * The array of events for the circular buffer implementing this
   * thread pool's queue.
   *
   * @serial  If <code>status</code> is not terminated,
   *          <code>events</code> must be an array with at least one
   *          entry and the same length as
   *          <code>handlers</code>. Entries that are between the head
   *          and tail of the circular buffer must not be
   *          <code>null</code>.
   */
  Event[]                   events;

  /**
   * The head index for the circular buffer implementing this thread
   * pool's queue. The head index points to the next buffer entry
   * a queue element can be removed from.
   *
   * @serial  <code>0 <= head < events.length</code>
   */
  int                       head;

  /**
   * The tail index for the circular buffer implementing this thread
   * pool's queue. The tail index points to the next buffer entry
   * a queue element can be added to. It is -1 if the buffer is empty.
   * Furthermore, <code>head == tail</code> if the buffer is full.
   *
   * @serial  <code>-1 <= tail < events.length</code>
   */
  int                       tail;

  /** The monitor for this thread pool. */
  transient Object          monitor;

  /** The minimum number of threads for this thread pool. */
  int                       minThreadNumber;

  /** The maximum number of threads for this thread pool. */
  int                       maxThreadNumber;

  /** The current number of threads for this thread pool. */
  transient int             threadNumber;

  /** The idle workers for this thread pool. */
  transient HashSet         idle;

  /** The busy workers for this thread pool. */
  transient HashSet         busy;


  // =======================================================================
  //                            Constructors
  // =======================================================================

  /**
   * Create a new thread pool with the specified concurrency domain
   * and the default queue capacity as well as the default minimum and
   * maximum thread numbers.
   *
   * @see     Constants#ANIMATOR_CAPACITY
   * @see     Constants#ANIMATOR_MIN_THREADS
   * @see     Constants#ANIMATOR_MAX_THREADS
   *
   * @param   concurrency  The concurrency domain for this thread
   *                       pool.
   * @throws  NullPointerException
   *                       Signals that <code>concurrency</code> is
   *                       <code>null</code>.
   */
  ThreadPool(ConcurrencyDomain concurrency) {
    this(concurrency, Constants.ANIMATOR_CAPACITY,
         Constants.ANIMATOR_MIN_THREADS, Constants.ANIMATOR_MAX_THREADS);
  }

  /**
   * Create a new thread pool with the specified concurrency domain,
   * queue capacity, as well as specified minimum and maximum thread
   * numbers. The newly created thread pool is initially inactive.
   *
   * @see     #setStatus(int)
   *
   * @param   concurrency  The concurrency domain for the new thread
   *                       pool.
   * @param   capacity     The capacity for the new thread pool.
   * @param   minThreads   The minimum thread number for the new thread
   *                       pool.
   * @param   maxThreads   The maximum thread number for the new thread
   *                       pool.
   * @throws  NullPointerException
   *                       Signals that <code>concurrency</code> is
   *                       <code>null</code>.
   * @throws  IllegalArgumentException
   *                       Signals that either of the specified numbers 
   *                       is not positive.
   */
  ThreadPool(ConcurrencyDomain concurrency, int capacity,
                    int minThreads, int maxThreads) {
    if (null == concurrency) {
      throw new NullPointerException("Null concurrency domain for thread pool");
    } else if (0 >= capacity) {
      throw new IllegalArgumentException("Non-positive queue capacity for " +
                                         "thread pool (" + capacity + ")");
    } else if (0 >= minThreads) {
      throw new IllegalArgumentException("Non-positive minimum thread number " +
                                         "for thread pool ("+minThreads+")");
    } else if (0 >= maxThreads) {
      throw new IllegalArgumentException("Non-positive maximum thread number " +
                                         "for thread pool ("+maxThreads+")");
    }

    this.concurrency = concurrency;
    status           = INACTIVE;
    handlers         = new EventHandler[capacity];
    events           = new Event[capacity];
    head             =  0;
    tail             = -1;
    monitor          = new Object();
    minThreadNumber  = minThreads;
    maxThreadNumber  = maxThreads;
    threadNumber     = minThreads;
    idle             = new HashSet(maxThreads * 4 / 3 + 2);
    busy             = new HashSet(maxThreads * 4 / 3 + 2);
    for (int i=0; i<minThreads; i++) {
      Worker worker = new Worker();
      worker.start();
      idle.add(worker);
    }
  }
    

  // =======================================================================
  //                             Serialization
  // =======================================================================

  /**
   * Serialize this thread pool.
   *
   * @serialData     The default fields while holding the monitor for this
   *                 thread pool.
   * @throws      IllegalStateException
   *                 Signals that this thread pool is being serialized
   *                 outside a check-point.
   */
  private void writeObject(ObjectOutputStream out) throws IOException {
    synchronized (monitor) {
      out.defaultWriteObject();
    }
  }

  /** Deserialize a thread pool. */
  private void readObject(ObjectInputStream in)
    throws IOException, ClassNotFoundException {

    in.defaultReadObject();
    monitor      = new Object();
    threadNumber = minThreadNumber;
    idle         = new HashSet(maxThreadNumber * 4 / 3 + 2);
    busy         = new HashSet(maxThreadNumber * 4 / 3 + 2);
    if (TERMINATED != status) {
      synchronized (monitor) {
        for (int i=0; i<minThreadNumber; i++) {
          Worker worker = new Worker();
          worker.start();
          idle.add(worker);
        }
      }
    }
  }


  // =======================================================================
  //                            Instance methods
  // =======================================================================

  /**
   * Determine whether this thread pool is concurrent, which it is.
   */
  public boolean isConcurrent() {
    return true;
  }

  /** Get the minimum number of threads for this thread pool. */
  public int getMinThreadNumber() {
    synchronized (monitor) {
      if (TERMINATED == status) {
        throw new IllegalStateException("Thread pool terminated");
      }

      return minThreadNumber;
    }
  }

  /** Get the current number of threads for this thread pool. */
  public int getThreadNumber() {
    synchronized (monitor) {
      if (TERMINATED == status) {
        throw new IllegalStateException("Thread pool terminated");
      } 

      return threadNumber;
    }
  }

  /* Get the maximum number of threads for this thread pool. */
  public int getMaxThreadNumber() {
    synchronized (monitor) {
      if (TERMINATED == status) {
        throw new IllegalStateException("Thread pool terminated");
      }

      return maxThreadNumber;
    }
  }

  /** Set the minimum number of threads for this thread pool. */
  public void setMinThreadNumber(int number) {
    if (0 >= number) {
      throw new IllegalArgumentException("Non-positive thread number (" +
                                         number + ")");
    }

    synchronized (monitor) {
      if (TERMINATED == status) {
        throw new IllegalStateException("Thread pool terminated");
      }

      minThreadNumber = number;
      if (maxThreadNumber < number) {
        maxThreadNumber = number;
      }

      if (threadNumber < minThreadNumber) {
        // We need to create additional workers.
        int diff      = minThreadNumber - threadNumber;
        threadNumber += diff;
        for (int i=0; i<diff; i++) {
          Worker worker = new Worker();
          worker.start();
          idle.add(worker);
        }
      }
    }
  }

  /** Add the specified number of threads to this thread pool. */
  public int addThreads(int number) {
    if (0 >= number) {
      throw new IllegalArgumentException("Non-positive thread number (" +
                                         number + ")");
    }

    synchronized (monitor) {
      if (TERMINATED == status) {
        throw new IllegalStateException("Thread pool terminated");
      } else if (INACTIVE == status) {
        // There is no point in adding any threads b/c this animator
        // is inactive.
        return 0;
      }

      // Adjust the number so that we never create more threads than
      // the maximum number.
      if (threadNumber + number > maxThreadNumber) {
        number = maxThreadNumber - threadNumber;
      }

      if (0 < number) {
        if (Constants.DEBUG_THREAD_CONTROL) {
          SystemLog.LOG.log(this, "Adding " + number + " threads");
        }
        threadNumber += number;
        for (int i=0; i<number; i++) {
          Worker worker = new Worker();
          worker.start();
          idle.add(worker);
        }
      }
    }

    return number;
  }

  /** Set the maximum number of threads for this thread pool. */
  public void setMaxThreadNumber(int number) {
    if (0 >= number) {
      throw new IllegalArgumentException("Non-positive thread number (" +
                                         number + ")");
    }

    synchronized (monitor) {
      if (TERMINATED == status) {
        throw new IllegalStateException("Thread pool terminated");
      }

      maxThreadNumber = number;
      if (minThreadNumber > number) {
        minThreadNumber = number;
      }
      
      if (threadNumber > maxThreadNumber) {
        // We need to exit some workers.
        monitor.notifyAll();
      }
    }
  }

  /** Get the current status for this thread pool. */
  public int getStatus() {
    return status;
  }

  /** Set the status for this thread pool. */
  public void setStatus(int status) {
    synchronized (monitor) {
      if (this.status == status) {
        return;
      } else if (TERMINATED == this.status) {
        throw new IllegalStateException("Thread pool terminated");
      }

      switch (status) {
      case ACTIVE:
        this.status = ACTIVE;
        monitor.notifyAll();
        break;

      case INACTIVE:
        this.status = INACTIVE;
        // No need to notify b/c nobody is waiting for this state.
        while (0 < busy.size()) {
          try {
            monitor.wait();
          } catch (InterruptedException x) {
            // Ignore.
          }
        }
        break;

      case DRAINED:
        this.status = DRAINED;
        monitor.notifyAll();
        while ((0 < busy.size()) || (-1 != tail)) {
          try {
            monitor.wait();
          } catch (InterruptedException x) {
            // Ignore.
          }
        }
        break;

      case TERMINATED:
        if (ACTIVE == this.status) {
          throw new IllegalArgumentException("Thread pool active");
        }
        this.status = TERMINATED;
        monitor.notifyAll();
        while ((0 < idle.size()) || (0 < busy.size())) {
          try {
            monitor.wait();
            // FIXME time out and signal a thread death to worker.
          } catch (InterruptedException x) {
            // Ignore.
          }
        }
        // Let GC do its work.
        handlers = null;
        events   = null;
        break;

      default:
        throw new IllegalArgumentException("Invalid animator status (" +
                                           status + ")");
      }
    }
  }

  /** Enqueue the specified event handler application. */
  public boolean enqueue(EventHandler handler, Event event) {
    synchronized (monitor) {
      if (TERMINATED == status) {
        throw new IllegalStateException("Thread pool terminated");
      } else if (DRAINED == status) {
        return false;
      }

      // Enqueue the event handler application.
      if (head == tail) { return false; }

      if (0 > tail) {
        tail = 0;
        head = 0;
      }

      handlers[tail] = handler;
      events[tail]   = event;

      tail++;

      if (handlers.length <= tail) {
        tail = 0;
      }

      monitor.notify();
    }
    
    return true;
  }

  /**
   * Enqueue the specified event handler application at the front of
   * the queue.
   */
  public boolean enqueueFirst(EventHandler handler, Event event) {
    synchronized (monitor) {
      if (TERMINATED == status) {
        throw new IllegalStateException("Thread pool terminated");
      } else if (DRAINED == status) {
        return false;
      }

      // Enqueue the event handler application.
      if (head == tail) { return false; }

      if (0 > tail) {
        tail = 1;
        head = 1;
      }

      head--;

      if (0 > head) {
        head = handlers.length - 1;
      }

      handlers[head] = handler;
      events[head]   = event;

      monitor.notify();
    }
    
    return true;
  }

  /**
   * Forcibly enqueue the specified event handler application at the
   * front of the queue.
   */
  public void enqueueForced(EventHandler handler, Event event) {
    synchronized (monitor) {
      if (TERMINATED == status) {
        throw new IllegalStateException("Thread pool terminated");
      }

      // Enqueue the event handler application.
      if (head == tail) {
        handlers[head] = handler;
        events[head]   = event;

        monitor.notify();
        return;
      }

      if (0 > tail) {
        tail = 1;
        head = 1;
      }

      head--;

      if (0 > head) {
        head = handlers.length - 1;
      }

      handlers[head] = handler;
      events[head]   = event;

      monitor.notify();
    }
  }

  /** Get the capacity of this thread pool's queue. */
  public int getQueueCapacity() {
    synchronized (monitor) {
      if (TERMINATED == status) {
        throw new IllegalStateException("Thread pool terminated");
      }

      return handlers.length;
    }
  }

  /** Determine whether this thread pool's queue is empty. */
  public boolean isQueueEmpty() {
    synchronized (monitor) {
      if (TERMINATED == status) {
        throw new IllegalStateException("Thread pool terminated");
      }

      return (-1 == tail);
    }
  }

  /** Get the current size of this thread pool's queue. */
  public int getQueueSize() {
    synchronized (monitor) {
      if (TERMINATED == status) {
        throw new IllegalStateException("Thread pool terminated");
      }

      if (-1 == tail) {
        return 0;
      } else if (head == tail) {
        return handlers.length;
      } else if (tail > head) {
        return (tail - head);
      } else {
        return (handlers.length - (head - tail));
      }
    }
  }

  /** Get a list of queue entries for this thread pool's queue. */
  public List getQueue() {
    synchronized (monitor) {
      if (TERMINATED == status) {
        throw new IllegalStateException("Thread pool terminated");
      }

      int       size  = getQueueSize();
      ArrayList list  = new ArrayList(size);
      int       index = head;

      for (int i=0; i<size; i++) {
        list.add(new HandlerApplication(handlers[index], events[index]));
        index++;
        if (handlers.length >= index) {
          index = 0;
        }
      }

      return list;
    }
  }

  /** Get a string representation of this thread pool. */
  public String toString() {
    return "#[Thread pool for " + concurrency + "]";
  }

}
