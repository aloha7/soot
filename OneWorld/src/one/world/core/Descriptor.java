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

/**
 * The superclass of all descriptors. Descriptors are used to provide
 * automatically discoverable information on tuples and components.
 *
 * @version  $Revision: 1.10 $
 * @author   Robert Grimm
 */
public abstract class Descriptor extends Tuple {
  
  /** The serial version ID for this class. */
  static final long serialVersionUID = 7709979449311082400L;  

  /**
   * The name of the entity.
   *
   * @serial  Must not be <code>null</code>.
   */
  public String name;

  /**
   * A short description of the entity.
   *
   * @serial  Must not be <code>null</code>.
   */
  public String description;

  /** Create a new, empty descriptor. */
  public Descriptor() {
    // Nothing to do.
  }

  /**
   * Create a new descriptor with the specified name and
   * description.
   *
   * @param   name         The name of the entity.
   * @param   description  A short description of the entity.
   */
  public Descriptor(String name, String description) {
    this.name        = name;
    this.description = description;
  }

  /** Validate this descriptor. */
  public void validate() throws TupleException {
    super.validate();

    if (null == name) {
      throw new InvalidTupleException("Null name for descriptor ("+this+")");
    } else if (null == description) {
      throw new InvalidTupleException("Null description for descriptor (" +
                                      this + ")");
    }
  }

}
