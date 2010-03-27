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

import one.world.Constants;

/**
 * Definition of the environment moving protocol. The protocol to move
 * environments from one node to another uses REP as its transport and
 * dynamic tuples as protocol messages. This interface defines the
 * different protocol messages as well as the payload (fields) for
 * each message type.
 *
 * <p>The ordering of protocol messages is as follows:<pre>
 *    Sender                     Receiver
 *
 *      -------- request-move -------->
 *      <-------- allow-move ----------
 *      ------ send-descriptors ------>
 *      <---- confirm-descriptors -----
 *    ( ---------- send-tuple -------->
 *      <-------- confirm-tuple ------- ) *
 *      ------- send-check-point ----->
 *      <------- complete-move --------
 * </pre>The <code>*</code> for the pair of send-tuple, confirm-tuple
 * messages indicates that zero or more such message rounds are
 * performed.</p>
 *
 * <p>The sender sends the original request-move message to the event
 * handler exported under the name {@link #MOVE_ACCEPTOR} on the
 * receiving node. Subsequent message are sent to the source of the
 * allow-move message. The receiver sends all messages to the source
 * of the request-move message. Both the sender and the receiver use
 * the closure of the previously received message for the next
 * outgoing message, thus chaining the closure originally used for the
 * request-move message throughout the protocol.</p>
 *
 * <p>Both sender and receiver may send an exceptional event
 * indicating some exceptional condition at any time, which
 * effectively cancels the move. Furthermore, both sender and receiver
 * time out after {@link #TIMEOUT} milliseconds when waiting for a
 * message, which also cancels the move.</p>
 *
 * <p>Corresponding to the different rounds of the protocol, the
 * sender and receiver go through several states. The sender starts
 * out in the {@link #STATE_INITIAL initial state}. After receiving
 * the allow-move message, it enters the {@link #STATE_DESCRIPTOR
 * descriptor state}. After receiving the confirm-descriptors message,
 * it enters the {@link #STATE_CONTENT content state}. Finally, after
 * receiving the complete-move message, it enters the {@link
 * #STATE_FINAL final state}.</p>
 *
 * <p>The receiver starts out in the {@link #STATE_INITIAL initial
 * state} when receiving a request-move message. After receiving the
 * send-descriptors message, it enters the {@link #STATE_DESCRIPTOR
 * descriptor state}. After receiving either a send-tuple or
 * send-check-point message, it enters the {@link #STATE_CONTENT
 * content state}. Finally, after completing the move, it enters the
 * {@link #STATE_FINAL final state}.</p>
 *
 * <p>On an exceptional condition, both sender and receiver enter the
 * {@link #STATE_ERROR error state}, which cancels the move.</p>
 *
 * <p>Note that the complete-move message may be lost, since REP
 * provides at-most-once message delivery guarantees. As a result, the
 * receiver may successfully complete the moving protocol, while the
 * sender thinks it has ended exceptionally and thus abort the move
 * operation. Applications that require stronger semantics need to
 * implement their own recovery protocol.</p>
 *
 * @version  $Revision: 1.4 $
 * @author   Robert Grimm 

 */
public interface MovingProtocol {

  /**
   * The REP name of the event handler accepting requests to move
   * environment(s) to this node. The name of this event handler is
   * "<code>one.world.move.acceptor</code>.
   */
  public static final String MOVE_ACCEPTOR     = "one.world.move.acceptor";

  /**
   * The field specifying the message type. The value must be one of
   * the messages specified in this interface.
   */
  public static final String MSG               = "msg";

  /**
   * The request-move message. This protocol message specifies the ID
   * and name of the root of the environment tree being moved, the
   * path of the new parent on the remote node, and whether the
   * environments are being cloned.
   *
   * @see  #ENV_ID
   * @see  #ENV_NAME
   * @see  #PATH
   * @see  #CLONE
   */
  public static final String MSG_REQUEST       = "request-move";

  /**
   * The allow-move message. This protocol message specifies the ID of
   * the new parent environment.
   *
   * @see  #ENV_ID
   */
  public static final String MSG_ALLOW         = "allow-move";

  /**
   * The send-descriptors message. This protocol message specifies the
   * descriptors for the environment tree being moved.
   *
   * @see   #DESCRIPTORS
   */
  public static final String MSG_DESCRIPTORS   = "send-descriptors";

  /**
   * The confirm-descriptors message. This protocol message is empty.
   */
  public static final String MSG_CONFIRM       = "confirm-descriptors";

  /**
   * The send-tuple message. This protocol message specifies the tuple
   * in binary form and the ID of the tuple's environment.
   *
   * @see  #TUPLE
   * @see  #ENV_ID
   */
  public static final String MSG_SEND_TUPLE    = "send-tuple";

  /**
   * The confirm-tuple message. This protocol message confirms that a
   * previously sent send-tuple message.
   *
   * @see  #TUPLE_ID
   */
  public static final String MSG_CONFIRM_TUPLE = "confirm-tuple";

  /**
   * The send-check-point message. This protocol message specifies the
   * check-point of the environment tree.
   *
   * @see  #CHECK_POINT
   */
  public static final String MSG_CHECK_POINT   = "send-check-point";

  /**
   * The complete-move message. This protocol message is empty.
   */
  public static final String MSG_COMPLETE      = "complete-move";

  /**
   * The field specifying an environment ID. The value of the field
   * must be a {@link one.util.Guid} representing the ID of the root
   * environment of the tree being moved.
   */
  public static final String ENV_ID            = "env-id";

  /**
   * The field specifying an environment name. The value of the field
   * must be a string representing the name of the root environment of
   * the tree being moved.
   */
  public static final String ENV_NAME          = "env-name";

  /**
   * The field specifying an environment path. The value of the field
   * must be a string representing the path of the new parent
   * environment on the receiving node.
   */
  public static final String PATH              = "path";

  /**
   * The field specifying whether the environment tree is being
   * cloned.  The value of the field must be a boolean representing
   * the flag for whether the environment tree is being clone.
   */
  public static final String CLONE             = "clone";

  /**
   * The field specifying environment descriptors. The value of the
   * field must be an array of {@link Environment.Descriptor
   * environment descriptors} whose entries are valid environment
   * descriptors representing all environments in the tree being
   * moved.
   */
  public static final String DESCRIPTORS       = "descriptors";

  /**
   * The field specifying a tuple in binary form. The value of the
   * field must be of type {@link one.world.data.BinaryData}. The
   * binary data must be the serialized form of a tuple and the ID of
   * the binary data tuple must be the same as the ID of the
   * serialized tuple. 
   */
  public static final String TUPLE             = "tuple";

  /**
   * The field specifying a tuple ID. The value of the field must
   * be a {@link one.util.Guid} representing the ID of a tuple.
   */
  public static final String TUPLE_ID          = "tuple-id";

  /**
   * The field specifying a check-point. The value of the field must
   * be a {@link CheckPoint} representing a valid check-point of the
   * tree being moved.
   */
  public static final String CHECK_POINT       = "check-point";

  /**
   * The timeout for waiting for a message. This constant can be
   * specified using the
   * "<code>one.world.moving.protocol.timeout</code>" system
   * property. If this system property is not defined, the default is
   * 60 seconds.
   */
  public static final long   TIMEOUT           =
    Constants.getLong("one.world.moving.protocol.timeout", 60 * 1000);

  /** The state code for the initial state. */
  public static final int    STATE_INITIAL     = 1;

  /** The state code for the descriptor state. */
  public static final int    STATE_DESCRIPTOR  = 2;

  /** The state code for the content state. */
  public static final int    STATE_CONTENT     = 3;

  /** The state code for the final state. */
  public static final int    STATE_FINAL       = 4;

  /** The state code for the error state. */
  public static final int    STATE_ERROR       = 0;

}
