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

import one.world.core.EventHandler;
import one.world.core.InvalidTupleException;
import one.world.core.TupleException;

/**
 * Implementation of a text message.
 *
 * @version  $Revision: 1.5 $
 * @author   Robert Grimm
 */
public class TextMessage extends Message {

  /** The serial version ID for this class. */
  static final long serialVersionUID = -8972751989254179759L;

  /** 
   * The actual message.
   *
   * @serial  Must not be <code>null</code>
   */
  public String msg;

  /** Create a new, empty text message. */
  public TextMessage() {
    // Nothing to do.
  }

  /**
   * Create a new text message.
   *
   * @param  source   The source for the new text message.
   * @param  closure  The closure for the new text message.
   * @param  sender   The sender for the new text message.
   * @param  channel  The channel for the new text message.
   * @param  msg      The actual message for the new text message.
   */
  public TextMessage(EventHandler source, Object closure, 
                     String sender, String channel, String msg) {
    super(source, closure, sender, channel);
    this.msg = msg;
  }

  /** Validate this text message. */
  public void validate() throws TupleException {
    super.validate();
    if (null == msg) {
      throw new InvalidTupleException("Null actual message for text message ("
                                      + this + ")");
    }
  }

  /** Return a string representation for this text message. */
  public String toString() {
    return "#[TextMessage from " + sender + " to " + channel + ": " + msg + "]";
  }

}


