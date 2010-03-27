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

package one.radio;

import one.world.core.Event;
import one.world.core.EventHandler;
import one.world.core.InvalidTupleException;
import one.world.core.TupleException;

/**
 * The superclass of all <i>one.radio</i> messages.
 *
 * @version  $Revision: 1.4 $
 * @author   Robert Grimm
 */
public abstract class Message extends Event {

  /** The serial version ID for this class. */
  static final long serialVersionUID = 7590618671810347083L;

  /**
   * The name of the sender sending this message.
   *
   * @serial  Must not be <code>null</code>.
   */
  public String sender;

  /** 
   * The name of the channel this message is being sent to.
   *
   * @serial  Must not be <code>null</code>
   */
  public String channel;

  /** Create a new, empty message. */
  public Message() {
    // Nothing to do.
  }

  /**
   * Create a new message.
   *
   * @param  source   The source for the new message.
   * @param  closure  The closure for the new message.
   * @param  sender   The sender for the new message.
   * @param  channel  The channel for the new message.
   */
  public Message(EventHandler source, Object closure,
                 String sender, String channel) {
    super(source, closure);
    this.sender  = sender;
    this.channel = channel;
  }

  /** Validate this message. */
  public void validate() throws TupleException {
    super.validate();
    if (null == sender) {
      throw new InvalidTupleException("Null sender for message ("+this+")");
    } else if (null == channel) {
      throw new InvalidTupleException("Null channel for message ("+this+")");
    }
  }

}


