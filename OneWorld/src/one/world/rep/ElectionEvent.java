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

package one.world.rep;

import one.util.Guid;
import one.world.Constants;

import one.world.core.*;
import one.world.binding.*;
import one.world.rep.*;
import one.world.util.*;
import one.world.io.*;
import one.world.env.*;

import java.net.InetAddress;

/**
 * An event to perform  
 *
 * @version  $Revision: 1.3 $
 * @author   Adam MacBeth 
 */
public final class ElectionEvent extends Event {
  
  // =======================================================================
  //                           Constants
  // =======================================================================

  /** The serial version ID for this class. */
  static final long serialVersionUID = -3782454519592549610L;

  /** The type for calling an election. */
  public static final int START = 1;

  /** The type for announcing a capacity. */
  public static final int CAPACITY = 2;
  
  // =======================================================================
  //                           Instance fields
  // =======================================================================

  /** The type of this event. */
  public int type;

  /** 
   * The capacity of the sending process as determined by its
   * heuristic function.  
   */
  public long capacity;

  /** The IP address of the sending process. Used to break capacity ties. */
  public InetAddress addr;

  /**
   * Create a new empty election event.
   */
  public ElectionEvent() {
    
  }

  /**
   * Create a new election event.
   *
   * @param  source  The source for the new election event.
   * @param  closure The closure for the new election event.
   * @param  type    The type for the new election event.
   * @param  capacity  The capacity of the sending process. 
   */
  public ElectionEvent(EventHandler source, Object closure, 
		       int type, long capacity, InetAddress addr) {
    super(source,closure);
    this.type = type;
    this.capacity = capacity;
    this.addr = addr;
  }

}


