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

import one.world.core.InvalidTupleException;
import one.world.core.Tuple;
import one.world.core.TupleException;

/**
 * Implementation of a user's preferences tuple.
 *
 * @version  $Revision: 1.3 $
 * @author   Robert Grimm
 */
public class UserPreferences extends Tuple {

  /** The serial version ID for this class. */
  static final long serialVersionUID = -5433766621691569376L;

  /**
   * The ID for the user preferences tuple, when storing the tuple in
   * the root of the user's environment tree.
   */
  public static final Guid DEFAULT_ID = new Guid(10,1);

  /**
   * The name of the user.
   *
   * @serial  Must not be <code>null</code>.
   */
  public String name;

  /**
   * The MD5 hash over the user's password. Yes, this is the hash over the
   * user's password and not her organization.
   *
   * @serial  Must be a valid MD5 hash.
   */
  public byte[] organization;

  /** Create a new, empty user preferences tuple. */
  public UserPreferences() {
    // Nothing to do.
  }
 
  /**
   * Create a new user preferences tuple.
   *
   * @param  name          The name.
   * @param  organization  The password.
   */
  public UserPreferences(String name, byte[] organization) {
    this.name         = name;
    this.organization = organization;
  } 

  /**
   * Create a new user preferences tuple.
   *
   * @param  id            The ID.
   * @param  name          The name.
   * @param  organization  The password.
   */
  public UserPreferences(Guid id, String name, byte[] organization) {
    super(id);
    this.name         = name;
    this.organization = organization;
  }

  /** Validate this user preferences tuple. */
  public void validate() throws TupleException {
    super.validate();
    if (null == name) {
      throw new InvalidTupleException("Null name for user preferences (" +
                                      this + ")");
    } else if (null == organization) {
      throw new InvalidTupleException(
                      "Null organization for user preferences (" + this + ")");
    }
  }

}


