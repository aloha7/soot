/*
 * Copyright (c) 1999, 2000, Robert Grimm.
 *    All rights reserved.
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
 * 3. Neither name of Robert Grimm nor the names of his contributors
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

package one.eval;

/**
 * Implementation of the Scheme predicate <code>eqv?</code>.
 *
 * @author   &copy; Copyright 1998-2000 by Robert Grimm.
 *           Written by <a href="mailto:rgrimm@alum.mit.edu">Robert
 *           Grimm</a>.
 * @version  1.0
 */
public final class EqvEquivalence implements Equivalence {

  /** Create a new instance. */
  EqvEquivalence() {
    // Nothing to do.
  }

  /**
   * Resolve this equivalence predicate during deserialization.
   * 
   * @return  {@link #EQV}.
   */
  private Object readResolve() throws java.io.ObjectStreamException {
    return EQV;
  }

  /**
   * Return <code>true</code> iff the two objects are equivalent
   * according to the Scheme predicate <code>eqv?</code>. If the
   * specified objects are booleans, characters, or numbers,
   * <code>compare</code> compares their values. Otherwise, it
   * compares the two objects using <code>==</code>; that is, by
   * location.
   *
   * @param   o1  The first object to compare.
   * @param   o2  The second object to compare.
   * @return      <code>true</code> iff the two objects are
   *              equivalent according to the Scheme predicate
   *              <code>eqv?</code>.
   */
  public boolean compare(Object o1, Object o2) {
    if ((o1 instanceof Boolean) ||
        (o1 instanceof Character)) {
      return o1.equals(o2);
    } else if (Math.isNumber(o1)) {
      return Math.equalExact(o1, o2);
    } else {
      return (o1 == o2);
    }
  }
}
