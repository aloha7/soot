/*
 * ========================================================================
 *  Copyright 1998-2000 Robert Grimm.
 *  All rights reserved.
 *  See COPYRIGHT file for a full description.
 * ========================================================================
 */

package one.eval.stuff;

import java.util.List;

import one.eval.BadTypeException;
import one.eval.Cast;
import one.eval.Equivalence;
import one.eval.Pair;

import one.eval.Symbol;
import one.eval.Vector;
import one.util.ReferenceList;
import one.eval.Math;

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
   * according to the Scheme predicate <code>equal?</code>. In
   * general, compares the specified objects by value (and
   * structure). That is, <code>compare</code> invokes the
   * <code>equals(Object)</code> method on the first object
   * <code>o1</code> providing the second object <code>o2</code> as an
   * argument and subsequently returns the result.
   *
   * <p>Strings, that is any combination of two instances of {@link
   * java.lang.String} and {@link StringBuffer}, are equivalent
   * according to this predicate, if they both have the same length
   * and the same characters in the same positions.</p>
   *
   * <p>For pairs including lists and for vectors, that is, Java
   * collections framework lists, <code>compare</code> implements an
   * exact structural comparison that takes possibly circular
   * structures into account. For this reason, the implementations of
   * {@link Pair#equals(Object)} and {@link Vector#equals(Object)}
   * call on this method to determine equivalence.</p>
   *
   * <p>The exact structural comparison of pairs and vectors keeps a
   * trace of all pairs and vectors visited during the equivalence
   * comparison. When visiting a new pair of nodes, it checks whether
   * the nodes have been visited before and, if so, checks whether the
   * back edges point to equivalent locations. Consequently, the exact
   * structural comparison can detect circular structures and
   * determine their equivalence. The runtime performance of this
   * exact structural comparison is O(<i>n</i><sup><font
   * size="-2">2</font></sup>) and the storage overhead is
   * O(<i>n</i>), where <i>n</i> is the number of nodes (that is,
   * pairs and vectors) encountered during the comparison.</p>
   *
   * <p>The exact structural comparison has the disadvantage that it
   * exerts a rather high performance and storage overhead for the
   * comparison of pairs and vectors, even though most pairs and
   * vectors do not contain any circular structures. To amend this
   * overhead in the common case, <code>compare</code> first performs
   * a heuristic comparison based on a simple, recursive equivalence
   * comparison. The heuristic comparison is limited in its recursion
   * depth by a (large) constant and scans for simple circular cdr
   * chains. If the two data structures to be compared contain
   * circular structures, the heuristic comparison will fail, and
   * <code>compare</code> consequently executes the exact structural
   * comparison to determine a result. The heuristic comparison adds
   * no storage overhead, but scans cdr chains twice, thus exacting a
   * reasonable performance overhead over a simple, recursive
   * equivalence comparison.</p>
   *
   * <p>To further reduce any comparison overhead, both the heuristic
   * and exact comparison will dispatch to a simple, recursive
   * comparison, if the two data structures to be compared are known
   * to not contain any circular structures. This is the case, when
   * both structures are literals (see {@link Pair#isLiteral()} and
   * {@link Vector#isLiteral()}).</p>
   *
   * @param   o1  The first object to compare.
   * @param   o2  The second object to compare.
   * @return      <code>true</code> iff the two objects are
   *              equivalent according to the Scheme predicate
   *              <code>equal?</code>.
   */
  public boolean compare(Object o1, Object o2) {
    try {
      return heuristicEqual(o1, o2, 0);
    } catch (Exception x) {
      return exactEqual(o1, o2, new ReferenceList(), new ReferenceList());
    }
  }

  /** The recursion limit for heuristic comparisons. */
  private static final int RECURSION_LIMIT = 50;

  /**
   * The heuristic comparison.
   *
   * @param   o1   The first object to compare.
   * @param   o2   The second object to compare.
   * @param   rec  The current recursion depth.
   * @return       <code>true</code> iff the two objects are equivalent
   *               according to the Scheme predicate <code>equal?</code>
   * @throws  Exception
   *               Signals that the heuristic comparison can not compare
   *               the specified objects because they may contain
   *               circular data structures.
   */
  private static boolean heuristicEqual(Object o1, Object o2, int rec)
    throws Exception {
    
    if (rec > RECURSION_LIMIT) {
      throw new Exception("recursion too deep");
    }

    if (o1 == o2) {                                 // Trivial case.
      return true;

    } else if (! isPairOrVector(o1)) {              // Not a pair nor vector.
      return recursiveEqual(o1, o2);

    } else if (o1 instanceof List) {                // Vectors.
      if (! (o2 instanceof List)) {
        return false;
      } else {
        List v1 = (List)o1;
        List v2 = (List)o2;

        if (((v1 instanceof Vector) && ((Vector)v1).isLiteral()) ||
            ((v2 instanceof Vector) && ((Vector)v2).isLiteral())) {
          // Recursion is fine for literals.
          return recursiveEqual(v1, v2);

        } else {
          // The general case.
          int l  = v1.size();
          if (v2.size() != l) return false;
          for (int i=0; i<l; i++) {
            if (! heuristicEqual(v1.get(i), v2.get(i), rec + 1)) {
              return false;
            }
          }
          return true;
        }
      }

    } else {                                        // Pairs.
      if (! (o2 instanceof Pair)) {
        return false;
      } else {
        Pair p1 = (Pair)o1;
        Pair p2 = (Pair)o2;

        if (p1.isLiteral() || p2.isLiteral()) {
          // Recursion is fine for literals.
          return recursiveEqual(p1, p2);

        } else if (p1.isCircular()) {
          // If both are circular we need the exact comparison,
          // otherwise we know they are not equal.
          if (p2.isCircular()) {
            throw new Exception("circular structure");
          } else {
            return false;
          }
        } else if (p2.isCircular()) {
          return false;

        } else {
          // The general case.
          while (true) {
            if (! heuristicEqual(p1.car(), p2.car(), rec + 1)) {
              return false;
            }

            o1 = p1.cdr();
            o2 = p2.cdr();

            if ((o1 instanceof Pair) && (o2 instanceof Pair)) {
              p1 = (Pair)o1;
              p2 = (Pair)o2;
              continue;
            } else {
              return heuristicEqual(o1, o2, rec + 1);
            }
          }
        }
      }
    }
  }

  /**
   * The exact comparison.
   *
   * @param   o1                    The first object to compare.
   * @param   o2                    The second object to compare.
   * @param   trace1                The comparison trace for the
   *                                first object.
   * @param   trace2                The comparison trace for the
   *                                second object.
   * @param   rec                   The current recursion depth.
   * @return                        <code>true</code> iff the two
   *                                objects are equivalent according
   *                                to the Scheme predicate
   *                                <code>equal?</code>
   */
  private static boolean exactEqual(Object o1, Object o2,
                                    ReferenceList trace1,
                                    ReferenceList trace2) {
    if (o1 == o2) {                                 // Trivial case.
      return true;
    } else if (! isPairOrVector(o1)) {              // Not a pair nor vector.
      return recursiveEqual(o1, o2);
    }

    int i1 = trace1.find(o1);                       // Circular structures.
    int i2 = trace2.find(o2);

    if (-1 != i1) {
      if (-1 != i2) {
        return (i1 == i2);
      } else {
        return false;
      }
    } else if (-1 != i2) {
      return false;
    } else if (o1 instanceof List) {                // Vectors in general.
      if (! (o2 instanceof List)) {
        return false;
      } else {
        List v1 = (List)o1;
        List v2 = (List)o2;

        if (((v1 instanceof Vector) && ((Vector)v1).isLiteral()) ||
            ((v2 instanceof Vector) && ((Vector)v2).isLiteral())) {
          // Recursion is fine for literals.
          return recursiveEqual(v1, v2);

        } else {
          int l  = v1.size();
          if (v2.size() != l) return false;
          trace1.add(v1);
          trace2.add(v2);
          for (int i=0; i<l; i++) {
            if (! exactEqual(v1.get(i), v2.get(i), trace1, trace2)) {
              return false;
            }
          }
          return true;
        }
      }

    } else {                                        // Pairs in general.
      if (! (o2 instanceof Pair)) {
        return false;
      } else {
        Pair p1 = (Pair)o1;
        Pair p2 = (Pair)o2;

        if (p1.isLiteral() || p2.isLiteral()) {
          // Recursion is fine for literals.
          return recursiveEqual(p1, p2);

        } else {
          while (true) {
            trace1.add(p1);
            trace2.add(p2);
            if (! exactEqual(p1.car(), p2.car(), trace1, trace2)) {
              return false;
            }

            o1 = p1.cdr();
            o2 = p2.cdr();

            if ((o1 instanceof Pair) && (o2 instanceof Pair)) {
              p1 = (Pair)o1;
              p2 = (Pair)o2;

              // Check for circular cdr chains, otherwise continue.
              i1 = trace1.find(o1);
              i2 = trace2.find(o2);

              if (-1 != i1) {
                if (-1 != i2) {
                  return (i1 == i2);
                } else {
                  return false;
                }
              } else if (-1 != i2) {
                return false;
              } else {
                continue;
              }
            } else {
              return exactEqual(o1, o2, trace1, trace2);
            }
          }
        }
      }
    }
  }

  /**
   * The recursive comparsion.
   *
   * @param   o1  The first object to compare.
   * @param   o2  The second object to compare.
   * @return      <code>true</code> iff the two objects are
   *              equivalent according to the Scheme predicate
   *              <code>equal?</code>.
   */
  private static boolean recursiveEqual(Object o1, Object o2) {
    if (o1 == o2) {                                     // Trivial case.
      return true;
    } else if ((null == o1) || (null == o2)) {          // Empty list.
      return false;
    } else if (o1 instanceof Symbol) {                  // Symbols.
      return (o1 == o2);
    } else if ((o1 instanceof String)                   // Strings.
               || (o1 instanceof StringBuffer)) {
      if ((o2 instanceof String)
          || (o2 instanceof StringBuffer)) {
        try {
          return (Cast.toString(o1).compareTo(Cast.toString(o2)) == 0);
        } catch (BadTypeException x) {
          throw new Error("Unexpected exception: " + x.toString());
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
          if (! recursiveEqual(v1.get(i), v2.get(i))) {
            return false;
          }
        }
        return true;

      } else if (o2.getClass().isArray()) {
        List v1  = (List)o1;
        int  len = v1.size();
        if (java.lang.reflect.Array.getLength(o2) != len) return false;
        for (int i=0; i<len; i++) {
          if (! recursiveEqual(v1.get(i), java.lang.reflect.Array.get(o2, i))) {
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
          if (! recursiveEqual(java.lang.reflect.Array.get(o1, i), v2.get(i))) {
            return false;
          }
        }
        return true;

      } else if (o2.getClass().isArray()) {
        int len = java.lang.reflect.Array.getLength(o1);
        if (java.lang.reflect.Array.getLength(o2) != len) return false;
        for (int i=0; i<len; i++) {
          if (! recursiveEqual(java.lang.reflect.Array.get(o1, i),
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
          if (! recursiveEqual(p1.car(), p2.car())) {
            return false;
          }

          o1 = p1.cdr();
          o2 = p2.cdr();

          if (o1 == o2) {
            return true;
          } else  if ((o1 instanceof Pair) && (o2 instanceof Pair)) {
            p1 = (Pair)o1;
            p2 = (Pair)o2;
            continue;
          } else {
            return recursiveEqual(o1, o2);
          }
        }
      }
    } else if (Math.isNumber(o1)) {                     // Numbers.
      return Math.equalExact(o1, o2);
    } else {                                            // Chars, bools, etc.
      return o1.equals(o2);
    }
  }
  
  /**
   * Test whether the specified object is a pair or a vector, that is,
   * a Java collections framework list.
   *
   * @param   o  The object to test.
   * @return     <code>true</code> iff the specified object
   *             is a pair or vector.
   */
  private static boolean isPairOrVector(Object o) {
    return ((o instanceof Pair) || (o instanceof List));
  }
}
