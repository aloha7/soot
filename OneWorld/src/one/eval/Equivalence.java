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
 * Definition of the Scheme equivalence predicates. This interface
 * defines the signature for equivalence predicates and provides three
 * constants that implement the three Scheme equivalence predicates
 * {@link #EQ eq?}, {@link #EQV eqv?}, and {@link #EQUAL equal?}.
 *
 * @author   &copy; Copyright 1998-2000 by Robert Grimm.
 *           Written by <a href="mailto:rgrimm@alum.mit.edu">Robert
 *           Grimm</a>.
 * @version  1.0
 */
public interface Equivalence extends java.io.Serializable {

  /**
   * The implementation of the Scheme predicate <code>eq?</code>.
   *
   * @see EqEquivalence
   */
  Equivalence EQ = new EqEquivalence();

  /**
   * The implementation of the Scheme predicate <code>eqv?</code>.
   *
   * @see EqvEquivalence
   */
  Equivalence EQV = new EqvEquivalence();

  /**
   * The implementation of the Scheme predicate <code>equal?</code>.
   *
   * @see EqualEquivalence
   */
  Equivalence EQUAL = new EqualEquivalence();

  /**
   * Determine whether the specified objects are equivalent according
   * to some definition of equivalence.
   *
   * @param   o1  The first object to test for equivalence.
   * @param   o2  The second object to test for equivalence.
   * @return      True iff the two specified objects are
   *              equivalent.
   */
  boolean compare(Object o1, Object o2);
  
}
