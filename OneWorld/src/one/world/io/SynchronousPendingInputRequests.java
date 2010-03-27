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

package one.world.io;

import one.world.core.Component;
import one.world.core.ComponentDescriptor;
import one.world.core.Environment;
import one.world.core.Event;
import one.world.core.EventHandler;
import one.world.core.ExceptionalEvent;
import one.world.core.ExportedDescriptor;
import one.world.core.ImportedDescriptor;
import one.world.core.Tuple;
import one.world.core.TupleException;
import one.world.core.UnknownEventException;

import one.world.binding.LeaseDeniedException;

import one.world.util.AbstractHandler;
import one.world.util.Log;
import one.world.util.TupleEvent;

import one.util.Guid;

import java.security.AccessController;
import java.security.PrivilegedAction;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;

/**
 * Manages input requests that are waiting for matching tuples.  
 * This class provides a synchronous interface:  The {@link #filter(Tuple)}
 * method waits until there is an input request present before processing 
 * the tuple.  Furthermore, the request collection is synchronized while
 * processing the tuple; no requests may be added or removed during
 * processing, and only one tuple may be processed at a time.  
 *
 * <p>This class should be used for services such as 
 * {@link NetworkIO.Client}, which provides reliable, ordered delivery
 * semantics.  Unless a synchronous interface is required, the use of
 * {@link PendingInputRequests} is preferable.</p> 
 *
 * @version  $Revision: 1.2 $
 * @author   Janet Davis
 */
public final class SynchronousPendingInputRequests 
        implements PendingRequest.Collection {

  /** The underlying collection. */
  private volatile LinkedList list;

  /** The lock object. */
  private Object lock;

  /** Constructs a new synchronous pending input request manager. */
  public SynchronousPendingInputRequests() {
    list = new LinkedList();
    lock = new Object();
  }

  /** 
   * Adds a pending request to the collection. 
   * 
   * @param request The request to add.
   * @return        The added request.
   */
  public PendingRequest add(PendingRequest request) {
    synchronized (lock) {
      list.addLast(request);

        // Notify, in case filter() is waiting for a request to be added.
        lock.notifyAll();
    }
    return request;
  }

  /** 
   * Removes a pending request from the collection.
   *
   * @param id     The ID of the request to remove.
   * @return       The removed request or <code>null</code> if 
   *               the request was not in the collection.
   */
  public PendingRequest remove(final Guid id) {
    Object result = null;

    synchronized (lock) {
      ListIterator iter = list.listIterator(0);
      Object obj;

      while (iter.hasNext()) {
        obj = iter.next();
        if (id == ((PendingRequest)obj).id) {
	  iter.remove();
          result = obj;
          break;
        }
      }
    }
    return (PendingRequest)result;
  }
    
  /**
   * Processes incoming tuples by attempting to match them to pending
   * requests.  READ requests are removed after matching a tuple.
   *
   * <p>If the collection is empty, this will wait until the collection
   * is not empty.  Furthermore, processing is performed under a lock, so
   * that no requests may be added or removed during processing.</p>
   *
   * <p>Matches are reported with {@link InputResponse}s  outside of the 
   * lock, to avoid deadlock.</p>
   *
   * @param tuple   The tuple to process.
   */
  public void filter(Tuple tuple) {

    // Collect matching requests in a separate list so that they can all 
    // be responded to outside the lock.
    LinkedList matches = new LinkedList();

    synchronized (lock) {

      // Wait for there to be something in the collection.
      while (list.isEmpty()) {
        try {
	  lock.wait();
	} catch (InterruptedException x) {
	  // Do nothing
	}
      }

      // Look for matches.
      ListIterator iter = list.listIterator(0);
      PendingRequest request;
      while (iter.hasNext()) {
        request = (PendingRequest)iter.next();
	if (request.filter.check(tuple)) {
	  if (request.type == InputRequest.READ) {
	    // READ requests should only match one tuple.
	    iter.remove();
	  }
	  matches.addLast(request);
	}
      }
    }

    // Outside the lock, report matches.
    Iterator iter = matches.iterator();
    while (iter.hasNext()) {
      ((PendingRequest)iter.next()).sendResult(tuple);
    }
  }
}
