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

import java.io.IOException;

import java.util.List;

import one.fonda.TestCollection;
import one.fonda.Harness;

import one.util.Guid;
import one.util.Bug;

import one.world.util.TupleEvent;

/**
 * Implementation of regression tests on statically typed tuples. The
 * regression tests exercise field access, equality testing and
 * validation for statically typed tuples.
 *
 * @version   $Revision: 1.19 $
 * @author    Robert Grimm
 */
public class TestTuple implements TestCollection {

  /** A valid tuple. */
  static class One extends Tuple {
    public String name;
    public int    age;

    public One() {
    }
  }

  /** Non-public instance field. */
  static class Two extends Tuple {
    public  String name;
    private int    age;

    public Two() {
    }
  }

  /** Duplicate static field (which is a valid tuple). */
  static class Three extends One {
    public String bingo;
    private static final String name = "Hello";

    public Three() {
    }
  }

  /** Non-final static field. */
  static class Four extends Tuple {
    public String name;
    public int    age;
    private static String hello;

    public Four() {
    }
  }

  /** Duplicate field in subtype. */
  static class Five extends One {
    public String name;

    public Five() {
    }
  }

  /** Customized serialization using writeObject(). */
  static class Six extends Tuple {
    public String name;
    public int    age;

    public Six() {
    }

    private void writeObject(java.io.ObjectOutputStream s) throws IOException {
      // Nothing to do.
    }
  }

  /** Customized serialization using writeExternal(). */
  static class Seven extends Tuple {
    public String name;
    public int    age;

    public Seven() {
    }

    public void writeExternal(java.io.ObjectOutput s) throws IOException {
      // Nothing to do.
    }
  }

  /** Customized serialization through writeReplace(). */
  static class Eight extends Tuple {
    public String name;
    public int    age;

    public Eight() {
    }

    Object writeReplace() throws java.io.ObjectStreamException {
      return null;
    }
  }

  /** Customized serialization through readObject(). */
  static class Nine extends Tuple {
    public String name;
    public int    age;

    public Nine() {
    }

    private void readObject(java.io.ObjectInputStream s)
      throws IOException, ClassNotFoundException {
      // Nothing to do.
    }
  }

  /** Customized serialization through readExternal(). */
  static class Ten extends Tuple {
    public String name;
    public int    age;

    public Ten() {
    }

    public void readExternal(java.io.ObjectInput s) throws IOException {
      // Nothing to do.
    }
  }

  /** Customized serialization through readResolve(). */
  static class Eleven extends Tuple {
    public String name;
    public int    age;

    public Eleven() {
    }

    Object readResolve() throws java.io.ObjectStreamException {
      return null;
    }
  }

  /** No no-arg constructor. */
  static class Twelve extends One {
    public String hello;

    public Twelve(String name) {
      hello = name;
    }
  }

  /** Valid static type. */
  static class Thirteen extends Tuple {
    public String name;

    public static final java.net.InetAddress addr;

    static {
      java.net.InetAddress addr2 = null;
      try {
         addr2 = java.net.InetAddress.getLocalHost();
      } catch (java.net.UnknownHostException x) {
      }
      addr = addr2;
    }

    public Thirteen() {
    }
  }

  /** Transient instance field. */
  static class Fourteen extends Tuple {
    public String name;
    public transient int age;

    public Fourteen() {
    }
  }

  /** Nested tuple. */
  static class Fifteen extends Tuple {
    public String name;
    public int    age;

    public Fifteen() {
    }
  }

  /** Nesting tuple. */
  static class Sixteen extends Tuple {
    public Fifteen fifteen;

    public Sixteen() {
    }
  }

  /** Invalid nested tuple. */
  static class Seventeen extends Tuple {
    public Two two;

    public Seventeen() {
    }
  }

  /** Invalid static type. */
  static class Eighteen extends Tuple {
    public static final RuntimePermission PERMISSION = null;

    public Eighteen() {
    }
  }

  // ========================================================================

  /** Create a new tuple test. */
  public TestTuple() {
    // Nothing to do.
  }

  /** Get the name of this test collection. */
  public String getName() {
    return "one.world.core.TestTuple";
  }

  /** Get a description for this test collection. */
  public String getDescription() {
    return "Field access, equality, and validity for statically typed tuples";
  }

  /** Get the number of tests. */
  public int getTestNumber() {
    return 38;
  }

  /** Determine whether this test collection needs an environment. */
  public boolean needsEnvironment() {
    return false;
  }

  /** Initialize this test collection. */
  public boolean initialize(Environment env) {
    return false;
  }

  /** Run the specified test. */
  public Object runTest(int number, Harness h, boolean verbose)
    throws Throwable {

    switch(number) {

    case 1:
      h.enterTest(1, "isValidTuple(One.class)", Boolean.TRUE);
      return isValid(One.class);

    case 2:
      h.enterTest(2, "isValidTuple(Two.class)", Boolean.FALSE);
      return isValid(Two.class);

    case 3:
      h.enterTest(3, "isValidTuple(Three.class)", Boolean.TRUE);
      return isValid(Three.class);

    case 4:
      h.enterTest(4, "isValidTuple(Four.class)", Boolean.FALSE);
      return isValid(Four.class);

    case 5:
      h.enterTest(5, "isValidTuple(Five.class)", Boolean.FALSE);
      return isValid(Five.class);

    case 6:
      h.enterTest(6, "isValidTuple(Six.class)", Boolean.FALSE);
      return isValid(Six.class);

    case 7:
      h.enterTest(7, "isValidTuple(Seven.class)", Boolean.FALSE);
      return isValid(Seven.class);

    case 8:
      h.enterTest(8, "isValidTuple(Eight.class)", Boolean.FALSE);
      return isValid(Eight.class);

    case 9:
      h.enterTest(9, "isValidTuple(Nine.class)", Boolean.FALSE);
      return isValid(Nine.class);

    case 10:
      h.enterTest(10, "isValidTuple(Ten.class)", Boolean.FALSE);
      return isValid(Ten.class);

    case 11:
      h.enterTest(11, "isValidTuple(Eleven.class)", Boolean.FALSE);
      return isValid(Eleven.class);

    case 12:
      h.enterTest(12, "isValidTuple(Twelve.class)", Boolean.FALSE);
      return isValid(Twelve.class);

    case 13:
      h.enterTest(13, "isValidTuple(Thirteen.class)", Boolean.TRUE);
      return isValid(Thirteen.class);

    case 14:
      h.enterTest(14, "isValidTuple(Fourteen.class)", Boolean.FALSE);
      return isValid(Fourteen.class);

    case 15:
      h.enterTest(15, "isValidTuple(Sixteen.class)", Boolean.TRUE);
      return isValid(Sixteen.class);

    case 16:
      h.enterTest(16, "isValidTuple(Seventeen.class)", Boolean.FALSE);
      return isValid(Seventeen.class);

    case 17:
      h.enterTest(17, "isValidTuple(Object.class)", Boolean.FALSE);
      return isValid(Object.class);

    case 18:
    case 19:
    case 20:
    case 21:
      {
        One t1 = new One();
        One t2 = new One();

        t1.name = "one";
        t1.age  = 10;

        t2.name = "two";
        t2.age  = 10;

        if (18 == number) {
          h.enterTest(18, "tuple1.equals(tuple1)", Boolean.TRUE);
          return new Boolean(t1.equals(t1));

        } else if (19 == number) {
          h.enterTest(19, "tuple1.equals(new Object())", Boolean.FALSE);
          return new Boolean(t1.equals(new Object()));

        } else if (20 == number) {
          h.enterTest(20, "tuple1.equals(tuple2)", Boolean.FALSE);
          return new Boolean(t1.equals(t2));

        } else if (21 == number) {
          h.enterTest(21, "tuple1.equals(tuple1\')", Boolean.TRUE);
          t2.name = "o" + "ne";
          return new Boolean(t1.equals(t2));
        }
      }

      throw new Bug("Invalid test number " + number);

    case 22:
    case 23:
    case 24:
    case 25:
    case 26:
      {
        One t1  = new One();

        t1.name = "one";
        t1.age  = 10;

        if (22 == number) {
          h.enterTest(22, "tuple.get(\"name\")", "one");
          return t1.get("name");

        } else if (23 == number) {
          h.enterTest(23, "tuple.get(\"age\")", new Integer(10));
          return t1.get("age");

        } else if (24 == number) {
          h.enterTest(24, "tuple.hasField(\"id\")", Boolean.TRUE);
          return new Boolean(t1.hasField("id"));

        } else if (25 == number) {
          h.enterTest(25, "tuple.hasField(\"two\")", Boolean.FALSE);
          return new Boolean(t1.hasField("two"));

        } else if (26 == number) {
          h.enterTest(26, "tuple.set(\"name\",\"two\"); tuple.get(\"name\")",
                      "two");
          t1.set("name", "two");
          return t1.get("name");
        }
      }

      throw new Bug("Invalid test number " + number);

    case 27:
      h.enterTest(27, "isValidTuple(Tuple.class)", Boolean.TRUE);
      return isValid(Tuple.class);

    case 28:
      h.enterTest(28, "isValidTuple(TupleEvent.class)", Boolean.TRUE);
      return isValid(one.world.util.TupleEvent.class);

    case 29:
    case 30:
      {
        One t1 = new One();
        One t2 = new One();

        t1.name = "one";
        t2.name = "o" + "ne";

        t1.metaData = new DynamicTuple();
        t1.metaData.set("one.world", "Yes");
        t2.metaData = new DynamicTuple();
        t2.metaData.id = t1.metaData.id;

        if (29 == number) {
          t2.metaData.set("one.world", "Yes");
          h.enterTest(29, "tuple1.equals(tuple2)", Boolean.TRUE);
          return new Boolean(t1.equals(t2));
        } else if (30 == number) {
          t2.metaData.set("one.world", "No");
          h.enterTest(30, "tuple1.equals(tuple2)", Boolean.TRUE);
          return new Boolean(t1.equals(t2));
        }
      }

      throw new Bug("Invalid test number " + number);

    case 31:
      {
        One t = new One();
        t.name = "one";
        h.enterTest(31, "tuple1.hashCode()", box("one".hashCode()));
        return box(t.hashCode());
      }

    case 32:
      {
        One t = new One();
        h.enterTest(32, "tuple1.hashCode()", box(0));
        return box(t.hashCode());
      }

    case 33:
      {
        One t = new One();
        t.metaData = new DynamicTuple();
        t.metaData.set("hello", "world");
        h.enterTest(33, "tuple1.hashCode()", box(0));
        return box(t.hashCode());
      }

    case 34:
      {
        One t  = new One();
        t.name = "one";
        t.age  = 4;
        h.enterTest(34, "tuple1.hashCode()", box("one".hashCode() + 4));
        return box(t.hashCode());
      }

    case 35:
      {
        h.enterTest(35, "tuple1.clone() == tuple1", Boolean.FALSE);
        One t = new One();
        One u = (One)t.clone();
        return box(t == u);
      }

    case 36:
      {
        h.enterTest(36, "tuple1.clone().name == tuple1.name", Boolean.TRUE);
        One t  = new One();
        t.name = "one";
        One u  = (One)t.clone();
        return box(t.name == u.name);
      }

    case 37:
      {
        h.enterTest(37, "tuple1.clone().id.equals(tuple1.id)", Boolean.TRUE);
        One t = new One();
        One u = (One)t.clone();
        return box(t.id.equals(u.id));
      }

    case 38:
      h.enterTest(38, "isValidTuple(Eighteen.class)", Boolean.FALSE);
      return isValid(Eighteen.class);

    default:
      throw new Bug("Invalid test number " + number);
    }
  }

  /** Clean up this test collection. */
  public void cleanup() {
    // Nothing to do.
  }

  /**
   * Determine whether the specified class is a valid tuple type.
   *
   * @param   k  The type to test.
   * @return     <code>Boolean.TRUE</code> if the specified class is
   *             a valid tuple type.
   */
  private Boolean isValid(Class k) {
    try {
      Type.validate(k);
      return Boolean.TRUE;
    } catch (MalformedTupleException x) {
      return Boolean.FALSE;
    }
  }
  
  /**
   * Box the specified boolean.
   *
   * @param   b  The boolean to box.
   * @return     The boxed boolean.
   */
  private Boolean box(boolean b) {
    return (b? Boolean.TRUE : Boolean.FALSE);
  }

  /**
   * Box the specified integer.
   *
   * @param   n  The integer to box.
   * @return     The boxed integer.
   */
  private Integer box(int n) {
    return new Integer(n);
  }

}
