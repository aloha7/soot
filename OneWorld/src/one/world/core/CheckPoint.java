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

import one.util.Guid;

import one.world.util.SystemUtilities;

/**
 * Implementation of a check-point. A check-point represents the
 * serialized state of all environments under a local root.
 *
 * @version  $Revision: 1.10 $
 * @author   Robert Grimm
 */
public final class CheckPoint extends Tuple {

  /** The serial version ID for this class. */
  static final long serialVersionUID = -963602460070914323L;

  /**
   * The timestamp for this check-point.
   *
   * @serial  Must be a non-negative number.
   */
  public long   timestamp;


  /**
   * The ID of the check-point's local root environment.
   *
   * @serial  Must not be <code>null</code>.
   */
  public Guid   root;

  /**
   * The IDs of the descendant environments.
   *
   * @serial  Must be an array with non-<code>null</code> entries.
   */
  public Guid[] descendants;

  /**
   * The actual state.
   *
   * @serial  Must be a valid serialized representation of the
   *          serialized state of this check-point's environment
   *          tree.
   */
  public byte[] state;

  /** Create a new, empty check-point. */
  public CheckPoint() {
    // Nothing to do.
  }

  /**
   * Create a new check-point.
   *
   * @param   timestamp    The timestamp for the new check-point.
   * @param   root         The ID of the check-point's root
   *                       environment.
   * @param   descendants  The IDs of the descendant environments.
   */
  public CheckPoint(long timestamp, Guid root, Guid[] descendants) {
    this.timestamp   = timestamp;
    this.root        = root;
    this.descendants = descendants;
  }

  /** Validate this check-point. */
  public void validate() throws TupleException {
    super.validate();

    if (0 > timestamp) {
      throw new InvalidTupleException("Negative timestamp for checkpoint (" +
                                      this + ")");
    } else if (null == root) {
      throw new InvalidTupleException("Null root environment ID for " +
                                      "checkpoint (" + this + ")");
    } else if (null == descendants) {
      throw new InvalidTupleException("Null list of descendant environment "+
                                      "IDs for checkpoint (" + this + ")");
    }

    for (int i=0; i<descendants.length; i++) {
      if (null == descendants[i]) {
        throw new InvalidTupleException("Null descendant environment ID " +
                                        "for checkpoint (" + this + ")");
      }
    }
  }

  /** Get a string representation of this check-point. */
  public String toString() {
    StringBuffer buf = new StringBuffer();

    buf.append("#[Check-point for environment ");
    buf.append((null == root)? "<null>" : root.toString());
    buf.append(" at ");
    buf.append(timestamp);
    buf.append(" (");
    buf.append(SystemUtilities.format(timestamp));
    buf.append("), size ");
    buf.append((null == state)? "<none>" : String.valueOf(state.length));
    buf.append(']');

    return buf.toString();
  }

}
