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

import one.util.Guid;

import one.world.core.Tuple;
import one.world.core.InvalidTupleException;

/**
 * Definition of the fetcher protocol. The fetcher protocol makes it
 * possible to fetch a user's root environment from one node to
 * another. It consists of two rounds of messages and uses dynamic
 * tuples over REP.
 *
 * <p>The four messages are:<pre>
 *    Fetcher                      Fetchee
 *
 *      --------- Come here! --------->
 *      <----- I challenge you! -------
 *      --- I accept the challenge --->
 *      <--------- Coming... ----------

 * </pre>The type of message is specified by the value of the {@link
 * #MSG message field}. The "Come here!" and the "Coming..." messages
 * have no other fields. The "I challenge you!" and "I accept the
 * challenge" messages have a {@link #PAYLOAD payload field}. The
 * value of the payload field for the "I challenge you!" message is
 * the fetchee's challenge (represented as a byte array). The value of
 * the payload field for the "I accept the challenge" message is the
 * MD5 hash over, in this order, the fetchee's challenge, the
 * fetcher's address, and the MD5 hash over the user's password. The
 * "I accept the challenge" message also has a {@link #ADDRESS address
 * field} specifying the address to migrate to (which typically is the
 * fetcher's address).</p>
 *
 * <p>The general idea of the protocol is that if the hash in the "I
 * accept the challenge" message matches the fetchee's calculation of
 * the same hash, the fetcher has been authenticated to the fetchee
 * (without exchanging passwords in the clear).  The fetchee then
 * sends the "Coming..." message and initiates a migration to the
 * fetcher's node (as specified by the "I accept the challenge"
 * message's address).</p>
 *
 * <p>Note that the fetcher protocol can be terminated by an
 * exceptional event at any time.</p>
 *
 * <p>Also note that when receiving a message, the receiver must use
 * the closure of the received message as the closure of the next
 * out-going message. This does not hold if the received message
 * terminates the fetcher protocol.</p>
 *
 * <p>A node that makes a user environment fetchable should export the
 * corresponding fetcher protocol handler both locally and through
 * discovery. The locally exported event handler should use
 * "<code>/User</code>" concatenated with the user's name as the name
 * of the event handler.  The event handler exported to discovery
 * should use an appropriate {@link UserDescriptor} as the
 * descriptor.</p>
 *
 * @see      one.util.Digest
 *
 * @version  $Revision: 1.3 $
 * @author   Robert Grimm
 */
public interface FetcherProtocol {

  /**
   * The user descriptor. A user descriptor is used by a node that
   * wants to make a user environment fetchable to export its fetcher
   * protocol handler to the discovery service.
   */
  public static class UserDescriptor extends Tuple {

    /**
     * The user name.
     *
     * @serial  Must not be <code>null</code>.
     */
    public String user;

    /** Create a new, empty user descriptor. */
    public UserDescriptor() {
      super();
    }

    /**
     * Create a new user descriptor with the specified name.
     *
     * @param   user  The user name.
     */
    public UserDescriptor(String user) {
      this.user = user;
    }
  }

  /**
   * The field specifying the message type. The value must be one of
   * the messages specified in this interface.
   */
  public static final String MSG           = "msg";

  /** The "Come here!" message. */
  public static final String MSG_COME      = "Come here!";

  /**
   * The "I challenge you!" message. This message has a payload
   * containing the desktop's challenge.
   */
  public static final String MSG_CHALLENGE = "I challenge you!";
  
  /**
   * The "I accept the challenge" message. This message has a payload
   * containing the MD5 hash over the server's challenge and the MD5
   * hash of the user's password.
   */
  public static final String MSG_RESPONSE  = "I accept the challenge";

  /** The "Coming..." message. */
  public static final String MSG_COMING    = "Coming...";

  /**
   * The field specifying the payload. The value must be an array of
   * bytes.
   */
  public static final String PAYLOAD       = "Payload";

  /**
   * The field specifying the node address to migrate to. The
   * value must be a valid string.
   */
  public static final String ADDRESS       = "Address";

}


