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

package one.tools;

import java.io.ObjectOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import one.world.Constants;
import one.world.core.Tuple;
import one.world.rep.RemoteEvent;
import one.world.rep.RemoteReference;
import one.world.util.NullHandler;

import one.util.Guid;

/**
 * Includes static methods for the serialization of Tuples and
 * RemoteEvents.
 *
 * @version  $Revision: 1.1 $
 * @author   Janet Davis
 */
public final class Serializer {

  /**
   * Serializes the specified object into a byte array.
   *
   * @param object  The object to serialize.
   * @return        A byte array containing the serialized object.
   *
   * @throws IOException if a serialization error occurs.
   */
  public static byte[] serialize(Object object) throws IOException {
    ByteArrayOutputStream bs = new ByteArrayOutputStream();
    ObjectOutputStream os = new ObjectOutputStream(bs);
    os.writeObject(object);
    os.flush();
    byte[] result = bs.toByteArray();

    os.close();
    bs.close();

    return result;
  }

  /** 
   * Annotates and serializes the specified remote event into a byte
   * array.
   *
   * @see   Constants
   *
   * @param re    The remote event to annotate and serialize.
   * @return      A byte array containing the annotated, serialized remote 
   *              event.
   *
   * @throws IOException if a serialization error occurs.
   */
  public static byte[] wireData(RemoteEvent re) throws IOException {
    re.source = NullHandler.NULL;
    re.setMetaData(Constants.DISCOVERY_BINDING,
                   new RemoteReference("127.127.127.127", 0, new Guid()));
    re.setMetaData(Constants.DISCOVERY_SOURCE_SERVER, "127.127.127.127");
    re.setMetaData(Constants.REP_RETRIES, new Integer(0));
    re.setMetaData(Constants.REQUESTOR_ADDRESS, "127.127.127.127");
    re.setMetaData(Constants.REQUESTOR_ID, new Guid());
    re.setMetaData(Constants.REQUESTOR_PORT, new Integer(0));
    return serialize(re);
  }
}
