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

import java.util.List;

import one.util.ReferenceList;
import one.util.Bug;

/**
 * Implementation of the Scheme predicate <code>equal?</code>.
 *
 * @author   &copy; Copyright 1998-2000 by Robert Grimm.
 *           Written by <a href="mailto:rgrimm@alum.mit.edu">Robert
 *           Grimm</a>.
 * @version  1.0
 */
public final class EqualEquivalence implements Equivalence {

  /** Create a new instance. */
  EqualEquivalence() {
    // Nothing to do.
  }

  /**
   * Resolve this equivalence predicate during deserialization.
   * 
   * @return  {@link #EQUAL}.
   */
  private Object readResolve() throws java.io.ObjectStreamException {
    return EQUAL;
  }

  /**
   * Return <code>true</code> iff the two objects are equivalent
   * according to the Scheme predicate <code>equal?</code>. This
   * method compares all types by value and structure. Note that
   * circular data structures result in an infinite recursion.
   *
   * @param   o1  The first object to compare.
   * @param   o2  The second object to compare.
   * @return      <code>true</code> iff the two objects are
   *              equivalent according to the Scheme predicate
   *              <code>equal?</code>.
   */
  public boolean compare(Object o1, Object o2) {
    if (o1 == o2) {                                     // Trivial case.
      return true;
    } else if ((null == o1) || (null == o2)) {          // Empty list.
      return false;
    } else if ((o1 instanceof String)                   // Strings.
               || (o1 instanceof StringBuffer)) {
      if ((o2 instanceof String)
          || (o2 instanceof StringBuffer)) {
        try {
          return (Cast.toString(o1).compareTo(Cast.toString(o2)) == 0);
        } catch (BadTypeException x) {
          throw new Bug("Unexpected exception: " + x.toString());
        }
      } else {
        return false;
      }
    } else if (o1 instanceof List) {                    // Vectors.
      if (o2 instanceof List) {
        List v1  = (List)o1;
        List v2  = (List)o2;
        int  len = v1.size();
        if (v2.size() != len) return false;
        for (int i=0; i<len; i++) {
          if (! compare(v1.get(i), v2.get(i))) {
            return false;
          }
        }
        return true;

      } else if (o2.getClass().isArray()) {
        List v1  = (List)o1;
        int  len = v1.size();
        if (java.lang.reflect.Array.getLength(o2) != len) return false;
        for (int i=0; i<len; i++) {
          if (! compare(v1.get(i), java.lang.reflect.Array.get(o2, i))) {
            return false;
          }
        }
        return true;

      } else {
        return false;
      }

    } else if (o1.getClass().isArray()) {
      if (o2 instanceof List) {
        List v2  = (List)o2;
        int  len = v2.size();
        if (java.lang.reflect.Array.getLength(o1) != len) return false;
        for (int i=0; i<len; i++) {
          if (! compare(java.lang.reflect.Array.get(o1, i), v2.get(i))) {
            return false;
          }
        }
        return true;

      } else if (o2.getClass().isArray()) {
        int len = java.lang.reflect.Array.getLength(o1);
        if (java.lang.reflect.Array.getLength(o2) != len) return false;
        for (int i=0; i<len; i++) {
          if (! compare(java.lang.reflect.Array.get(o1, i),
                        java.lang.reflect.Array.get(o2, i))) {
            return false;
          }
        }
        return true;

      } else {
        return false;
      }
    } else if (o1 instanceof Pair) {                    // Pairs.
      if (! (o2 instanceof Pair)) {
        return false;
      } else {
        Pair p1 = (Pair)o1;
        Pair p2 = (Pair)o2;

        while (true) {
          if (! compare(p1.car, p2.car)) {
            return false;
          }

          o1 = p1.cdr;
          o2 = p2.cdr;

          if (o1 == o2) {
            return true;
          } else  if ((o1 instanceof Pair) && (o2 instanceof Pair)) {
            p1 = (Pair)o1;
            p2 = (Pair)o2;
            continue;
          } else {
            return compare(o1, o2);
          }
        }
      }
    } else if (Math.isNumber(o1)) {                     // Numbers.
      return Math.equalExact(o1, o2);
    } else {                                            // Chars, bools, etc.
      return o1.equals(o2);
    }
  }

}
