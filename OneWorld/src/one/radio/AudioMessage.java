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

import one.util.Guid;

/**
 * Implementation of a audio message.
 *
 * @version  $Revision: 1.5 $
 * @author   Janet Davis
 */
public class AudioMessage extends Message {

  /** The serial version ID for this class. */
  static final long serialVersionUID = 4933355178437519852L;

  /**
   * A unique identifier for this stream. 
   * 
   * @serial Must not be <code>null</code>.
   */
  public Guid streamId;

  /** 
   * The audio format descriptor.
   * 
   * @serial Must not be <code>null</code>.
   */
  public AudioDescriptor descriptor;

  /** 
   * The audio data.
   *
   * @serial Must not be <code>null</code>.
   */
  public byte[] data;

  /** 
   * The number of bytes of audio data. 
   */
  public int length;

  /** 
   * The audio stream sequence number.
   */
  public int sequenceNumber;

  /** Create a new, empty audio message. */
  public AudioMessage() {
    // Nothing to do.
  }

  /**
   * Create a new audio message.
   *
   * @param  source   The source for the new audio message.
   * @param  closure  The closure for the new audio message.
   * @param  sender   The sender for the new audio message.
   * @param  channel  The channel for the new audio message.
   * @param  streamId The id for this audio stream.
   * @param  descriptor  The audio format descriptor for the new audio
   *                  message.
   * @param  data     The data for the new audio message.
   * @param  length   The length of the new audio message.
   * @param  sequenceNumber  The sequence number for the new audio
   *                  message.
   */
  public AudioMessage(EventHandler source, Object closure, 
                     String sender, String channel, Guid streamId,
		     AudioDescriptor descriptor, 
		     byte[] data, int length, int sequenceNumber) {
    super(source, closure, sender, channel);
    this.streamId = streamId;
    this.descriptor = descriptor;
    this.data = data;
    this.length = length;
    this.sequenceNumber = sequenceNumber;
  }

  /** Validate this audio message. */
  public void validate() throws TupleException {
    super.validate();
    if (null == streamId) {
      throw new InvalidTupleException("Null stream ID for audio message (" +
                                      this + ")");
    } else if (null == data) {
    } else if (null == descriptor) {
      throw new InvalidTupleException("Null descriptor for audio message (" +
                                      this + ")");
    } else if (null == data) {
      throw new InvalidTupleException("Null data for audio message ("+this+")");
    }
  }

  /** Return a string representation for this audio message. */
  public String toString() {
    return "#[AudioMessage from " + sender + " to " + channel + " ("
           + streamId + ", " + sequenceNumber + ")";
  }

}


