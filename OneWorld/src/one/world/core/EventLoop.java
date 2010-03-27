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

import one.world.Constants;

import one.world.util.HandlerApplication;

/**
 * Implementation of an event loop. An event loop is an animator that
 * uses a single thread to process enqueued event handler
 * applications.
 *
 * @version  $Revision: 1.25 $
 * @author   Robert Grimm
 */
public final class EventLoop implements Animator, java.io.Serializable {

  /** The serial version ID for this class. */
  static final long serialVersionUID = 5114139211622343303L;

  // =======================================================================
  //                            Worker thread
  // =======================================================================

  /** The worker thread for the event loop. */
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
            running = false;
            if (INACTIVE == status) {
              monitor.notify();
            } else if ((DRAINED == status) && (-1 == tail)) {
              monitor.notify();
            }
          } else {
            looping = true;
          }

          do {
            // Check status.
            if (TERMINATED == status) {
              // Flag that we are done.
              done = true;
              // Only the thread in setStatus() is waiting for us.
              monitor.notify();
              return;
              
            } else if ((0 > tail) || (INACTIVE == status)) {
              try {
                monitor.wait();
              } catch (InterruptedException x) {
                // Ignore.
              }

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
          running = true;
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

  /** The concurrency domain for this event loop. */
  final ConcurrencyDomain   concurrency;

  /**
   * The status for this event loop.
   *
   * @serial  Must either be the active, inactive, or terminated
   *          status code.
   */
  volatile int              status;

  /**
   * The array of handlers for the circular buffer implementing this
   * event loop's queue.
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
   * event loop's queue.
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
   * The head index for the circular buffer implementing this event
   * loop's queue. The head index points to the next buffer entry
   * a queue element can be removed from.
   *
   * @serial  <code>0 <= head < events.length</code>
   */
  int                       head;

  /**
   * The tail index for the circular buffer implementing this event
   * loop's queue. The tail index points to the next buffer entry
   * a queue element can be added to. It is -1 if the buffer is empty.
   * Furthermore, <code>head == tail</code> if the buffer is full.
   *
   * @serial  <code>-1 <= tail < events.length</code>
   */
  int                       tail;

  /** The monitor for this event loop. */
  transient Object          monitor;

  /** The worker for this event loop. */
  private transient Worker  worker;

  /**
   * The flag for whether the worker is currently executing an event
   * handler application.
   */
  transient boolean         running;

  /**
   * The flag for whether the worker is done and has terminated.
   */
  transient boolean         done;


  // =======================================================================
  //                            Constructors
  // =======================================================================

  /**
   * Create a new event loop with the default queue capacity and the
   * specified concurrency domain. The newly created event loop is
   * initially inactive.  If the
   * "<code>one.world.animator.capacity</code>" system property is
   * defined and if it represents a positive number, that number is
   * used as the default capacity. Otherwise a hard-coded default is
   * used.
   *
   * @see     Constants#ANIMATOR_CAPACITY
   * 
   * @param   concurrency  The concurrency domain for the new event loop.
   * @throws  NullPointerException
   *                       Signals that <code>concurrency</code> is
   *                       <code>null</code>.
   */
  EventLoop(ConcurrencyDomain concurrency) {
    this(Constants.ANIMATOR_CAPACITY, concurrency);
  }

  /**
   * Create a new event loop with the specified queue capacity and
   * concurrency domain. The newly created event loop is initially
   * inactive.
   *
   * @see     #setStatus(int)
   *
   * @param   capacity     The capacity for the new event loop.
   * @param   concurrency  The concurrency domain for the new event
   *                       loop.
   * @throws  NullPointerException
   *                       Signals that <code>concurrency</code> is
   *                       <code>null</code>.
   * @throws  IllegalArgumentException
   *                       Signals that <code>capacity</code> is not
   *                       positive.
   */
  EventLoop(int capacity, ConcurrencyDomain concurrency) {
    if (0 >= capacity) {
      throw new IllegalArgumentException("Non-positive queue capacity for " +
                                         "event loop (" + capacity + ")");
    } else if (null == concurrency) {
      throw new NullPointerException("Null concurrency domain for event loop");
    }

    this.concurrency = concurrency;
    status           = INACTIVE;
    handlers         = new EventHandler[capacity];
    events           = new Event[capacity];
    head             =  0;
    tail             = -1;
    monitor          = new Object();
    worker           = new Worker();
    worker.start();
  }


  // =======================================================================
  //                             Serialization
  // =======================================================================

  /**
   * Serialize this event loop.
   *
   * @serialData     The default fields while holding the monitor for
   *                 this event loop.
   * @throws      IllegalStateException
   *                 Signals that this event loop is being serialized
   *                 outside a check-point.
   */
  private void writeObject(ObjectOutputStream out) throws IOException {
    synchronized (monitor) {
      out.defaultWriteObject();
    }
  }

  /** Deserialize an event loop. */
  private void readObject(ObjectInputStream in)
    throws IOException, ClassNotFoundException {

    in.defaultReadObject();
    monitor = new Object();
    if (TERMINATED != status) {
      worker = new Worker();
      worker.start();
    }
  }


  // =======================================================================
  //                            Instance methods
  // =======================================================================

  /**
   * Determine whether this event loop is concurrent, which it is not.
   */
  public boolean isConcurrent() {
    return false;
  }

  /** Get the minimum number of threads for this event loop, which is 1. */
  public int getMinThreadNumber() {
    synchronized (monitor) {
      if (TERMINATED == status) {
        throw new IllegalStateException("Event loop terminated");
      }
      
      return 1;
    }
  }

  /** Get the current number of threads for this event loop, which is 1. */
  public int getThreadNumber() {
    synchronized (monitor) {
      if (TERMINATED == status) {
        throw new IllegalStateException("Event loop terminated");
      }
      
      return 1;
    }
  }

  /** Get the maximum number of threads for this event loop, which is 1. */
  public int getMaxThreadNumber() {
    synchronized (monitor) {
      if (TERMINATED == status) {
        throw new IllegalStateException("Event loop terminated");
      }
      
      return 1;
    }
  }

  /**
   * Set the minimum number of threads for this event loop, which results
   * in an unsupported operation exception.
   */
  public void setMinThreadNumber(int number) {
    throw new UnsupportedOperationException("Unable to set minimum thread " +
                                            "number for event loop");
  }

  /**
   * Add the specified number of threads to this event loop, which results
   * in an unsupported operation exception.
   */
  public int addThreads(int number) {
    throw new UnsupportedOperationException("Unable to add threads to event " +
                                            "loop");
  }

  /**
   * Set the maximum number of threads for this event loop, which results
   * in an unsupported operation exception.
   */
  public void setMaxThreadNumber(int number) {
    throw new UnsupportedOperationException("Unable to set maximum thread " +
                                            "number for event loop");
  }

  /** Get the current status for this event loop. */
  public int getStatus() {
    return status;
  }

  /** Set the status for this event loop. */
  public void setStatus(int status) {
    synchronized (monitor) {
      if (this.status == status) {
        return;
      } else if (TERMINATED == this.status) {
        throw new IllegalStateException("Event loop terminated");
      }

      switch (status) {
      case ACTIVE:
        this.status = ACTIVE;
        monitor.notify();
        break;

      case INACTIVE:
        this.status = INACTIVE;
        // No need to notify b/c nobody is waiting for this state.
        while (running) {
          try {
            monitor.wait();
          } catch (InterruptedException x) {
            // Ignore.
          }
        }
        break;

      case DRAINED:
        this.status = DRAINED;
        monitor.notify();
        while (running || (-1 != tail)) {
          try {
            monitor.wait();
          } catch (InterruptedException x) {
            // Ignore.
          }
        }
        break;

      case TERMINATED:
        if (ACTIVE == this.status) {
          throw new IllegalArgumentException("Event loop active");
        }
        this.status = TERMINATED;
        monitor.notify();
        while (! done) {
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
        worker   = null;
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
        throw new IllegalStateException("Event loop terminated");
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
        throw new IllegalStateException("Event loop terminated");
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
        throw new IllegalStateException("Event loop terminated");
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

  /** Get the capacity of this event loop's queue. */
  public int getQueueCapacity() {
    synchronized (monitor) {
      if (TERMINATED == status) {
        throw new IllegalStateException("Event loop terminated");
      }

      return handlers.length;
    }
  }

  /** Determine whether this event loop's queue is empty. */
  public boolean isQueueEmpty() {
    synchronized (monitor) {
      if (TERMINATED == status) {
        throw new IllegalStateException("Event loop terminated");
      }

      return (-1 == tail);
    }
  }

  /** Get the current size of this event loop's queue. */
  public int getQueueSize() {
    synchronized (monitor) {
      if (TERMINATED == status) {
        throw new IllegalStateException("Event loop terminated");
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

  /** Get a list of queue entries for this event loop's queue. */
  public List getQueue() {
    synchronized (monitor) {
      if (TERMINATED == status) {
        throw new IllegalStateException("Event loop terminated");
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

  /** Get a string representation of this event loop. */
  public String toString() {
    return "#[Event loop for " + concurrency + "]";
  }

}
