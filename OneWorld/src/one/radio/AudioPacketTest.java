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

import one.world.io.Query;

import one.world.rep.DiscoveredResource;
import one.world.rep.RemoteEvent;
import one.world.rep.RemoteReference;

import one.world.util.NullHandler;

import one.tools.Serializer;
import one.util.Guid;

/**
 * Determines the size of on-the-wire audio packets.
 *
 * @version  $Revision: 1.2 $
 * @author   Janet Davis
 */
public final class AudioPacketTest {

  /** 
   * Prints out the size of an audio packet with the specified sender,
   * channel, and number of bytes of audio data.
   *
   * <p>Usage: 
   * <pre>java one.radio.AudioPacketTest <sender> <channel> <numBytes>
   * </pre></p>
   */
  public static void main(String[] args) throws Throwable {
    String usage = 
        "java one.radio.AudioPacketTest <sender> <channel> <numBytes>";

    if (args.length != 3) {
      throw new IllegalArgumentException(usage);
    }

    String sender = args[0];
    String channel = args[1];
    int numBytes = Integer.parseInt(args[2]);

    RemoteReference ref =
        new RemoteReference("127.127.127.127", 0, new Guid());

    AudioMessage message = new AudioMessage(ref, null, 
                                            sender, channel,
					    new Guid(),
					    new AudioDescriptor(),
					    new byte[numBytes],
					    0, 0);

    DiscoveredResource resource = 
        new DiscoveredResource(
	  new Query(new Query("", Query.COMPARE_HAS_SUBTYPE, Channel.class),
	            Query.BINARY_AND,
		    new Query("name", Query.COMPARE_EQUAL, channel)),
		    true);

    RemoteEvent re = new RemoteEvent(NullHandler.NULL, null, 
                                     resource, message);

    System.out.println("Empty RemoteEvent:  " 
                       + Serializer.serialize(new RemoteEvent()).length);
    System.out.println("Raw RemoteEvent:    " 
                       + Serializer.serialize(re).length);
    System.out.println("RemoteEvent:        " 
                       + Serializer.wireData(re).length);
    System.out.println("MetaData:           " 
                       + Serializer.serialize(re.metaData).length);
    System.out.println("DiscoveredResource: " 
                       + Serializer.serialize(resource).length);
    System.out.println("Query:              " 
                       + Serializer.serialize(resource.query).length);
    System.out.println("AudioMessage:       " 
                       + Serializer.serialize(message).length);
    System.out.println("RemoteReference:    " 
                       + Serializer.serialize(ref).length);
    System.out.println("AudioDescriptor:    " 
                       + Serializer.serialize(message.descriptor).length);
    System.out.println("NullHandler.NULL:   " 
                       + Serializer.serialize(NullHandler.NULL).length);
  }
}


