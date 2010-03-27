/*
 * Copyright (c) 1999, 2000, University of Washington, Department of
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

import one.fonda.TestCollection;
import one.fonda.Harness;

import one.util.Guid;
import one.util.Bug;

import one.world.util.NullHandler;

/**
 * Implementation of regression tests on dynamically typed tuples. The
 * regression tests exercise field access and equality testing for
 * dynamically typed tuples.
 *
 * @version   $Revision: 1.11 $
 * @author    Robert Grimm
 */
public class TestDynamicTuple implements TestCollection {

  /** Some ID. */
  public static final Guid         ID      = new Guid();

  /** Some source. */
  public static final EventHandler SOURCE  = new EventHandler() {
      public void handle(Event e) { /* Nothing to do. */ }
    };

  /** Some closure. */
  public static final Object       CLOSURE = new Object();

  /** Create a new dynamic tuple test. */
  public TestDynamicTuple() {
    // Nothing to do.
  }

  /** Get the name of this test collection. */
  public String getName() {
    return "one.world.core.TestDynamicTuple";
  }

  /** Get a description for this test collection. */
  public String getDescription() {
    return "Field access and equality for dynamically typed tuples";
  }

  /** Get the number of tests. */
  public int getTestNumber() {
    return 26;
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

    DynamicTuple t1 = new DynamicTuple();
    DynamicTuple t2 = new DynamicTuple();

    switch(number) {

    case 1:
      h.enterTest(1, "tuple.id = ID; tuple.get(\"id\");", ID);
      t1.id = ID;
      return t1.get("id");

    case 2:
      h.enterTest(2, "tuple.source = SOURCE; tuple.get(\"source\");", SOURCE);
      t1.source = SOURCE;
      return t1.get("source");

    case 3:
      h.enterTest(3, "tuple.closure = CLOSURE; tuple.get(\"closure\");",
                  CLOSURE);
      t1.closure = CLOSURE;
      return t1.get("closure");

    case 4:
      h.enterTest(4, "tuple.set(\"id\", ID); tuple.get(\"id\");", ID);
      t1.set("id", ID);
      return t1.get("id");

    case 5:
      h.enterTest(5, "tuple.set(\"source\", SOURCE); tuple.get(\"source\");",
                  SOURCE);
      t1.set("source", SOURCE);
      return t1.get("source");

    case 6:
      h.enterTest(6, "tuple.set(\"closure\", CLOSURE); tuple.get(\"closure\");",
                  CLOSURE);
      t1.set("closure", CLOSURE);
      return t1.get("closure");

    case 7:
      h.enterTest(7, "tuple.hasField(\"id\")", Boolean.TRUE);
      return box(t1.hasField("id"));

    case 8:
      h.enterTest(8, "tuple.hasField(\"source\")", Boolean.TRUE);
      return box(t1.hasField("source"));

    case 9:
      h.enterTest(9, "tuple.hasField(\"closure\")", Boolean.TRUE);
      return box(t1.hasField("closure"));

    case 10:
      h.enterTest(10, "tuple.set(\"name\", \"one\"); tuple.get(\"name\");",
                  "one");
      t1.set("name", "one");
      return t1.get("name");

    case 11:
      h.enterTest(11, "tuple.set(\"name\", \"one\"); tuple.hasField(\"name\");",
                  Boolean.TRUE);
      t1.set("name", "one");
      return box(t1.hasField("name"));
   
    case 12:
      h.enterTest(12, "tuple.set(\"name\", \"one\"); tuple.remove(\"name\"); "
                  + "tuple.hasField(\"name\");", Boolean.FALSE);
      t1.set("name", "one");
      t1.remove("name");
      return box(t1.hasField("name"));

    case 13:
      h.enterTest(13, "tuple1.equals(tuple2)", Boolean.TRUE);
      return box(t1.equals(t2));

    case 14:
      h.enterTest(14, "tuple1.equals(tuple2)", Boolean.TRUE);
      t1.id      = ID;
      t1.source  = SOURCE;
      t1.closure = CLOSURE;
      t2.id      = ID;
      t2.source  = SOURCE;
      t2.closure = CLOSURE;
      return box(t1.equals(t2));

    case 15:
      h.enterTest(15, "tuple1.equals(tuple2)", Boolean.TRUE);
      t1.source  = SOURCE;
      t1.closure = CLOSURE;
      t2.source  = SOURCE;
      t2.closure = CLOSURE;
      return box(t1.equals(t2));

    case 16:
      h.enterTest(16, "tuple1.equals(tuple2)", Boolean.TRUE);
      t1.source  = SOURCE;
      t1.closure = CLOSURE;
      t1.set("name", "one");
      t2.source  = SOURCE;
      t2.closure = CLOSURE;
      t2.set("name", "one");
      return box(t1.equals(t2));

    case 17:
      h.enterTest(17, "tuple1.equals(tuple2)", Boolean.FALSE);
      t1.source  = SOURCE;
      t1.closure = CLOSURE;
      t1.set("name", "one");
      t2.source  = SOURCE;
      t2.closure = CLOSURE;
      t2.set("name", "two");
      return box(t1.equals(t2));

    case 18:
      h.enterTest(18, "tuple1.hashCode()", box(NullHandler.NULL.hashCode()));
      return box(t1.hashCode());

    case 19:
      h.enterTest(19, "tuple1.hashCode()", box(NullHandler.NULL.hashCode()));
      t1.metaData = new DynamicTuple();
      t1.metaData.set("one", "eins");
      return box(t1.hashCode());

    case 20:
      h.enterTest(20, "tuple1.hashCode()",
                  box(NullHandler.NULL.hashCode() + "eins".hashCode() +
                      "zwei".hashCode()));
      t1.set("one", "eins");
      t1.set("two", "zwei");
      return box(t1.hashCode());

    case 21:
      h.enterTest(21, "tuple1.hashCode()",
                  box("one".hashCode() + NullHandler.NULL.hashCode() + 4));
      t1.set("one", "one");
      t1.set("two", box(4));
      return box(t1.hashCode());

    case 22:
      h.enterTest(22, "tuple1.hashCode()",
                  box("one".hashCode() + NullHandler.NULL.hashCode() + 4));
      t1.set("one", "one");
      t1.set("two", box(4));
      t1.metaData = new DynamicTuple();
      t1.metaData.set("one", "one");
      return box(t1.hashCode());

    case 23:
      h.enterTest(23, "tuple1.equals(tuple2)", Boolean.TRUE);
      t1.source    = SOURCE;
      t1.set("name", "one");
      t2.source    = SOURCE;
      t2.set("name", "o" + "ne");
      t1.metaData  = new DynamicTuple();
      t1.metaData.set("name", "two");
      t2.metaData  = new DynamicTuple();
      return box(t1.equals(t2));

    case 24:
      h.enterTest(24, "tuple1.clone() == tuple1", Boolean.FALSE);
      t2 = (DynamicTuple)t1.clone();
      return box(t1 == t2);

    case 25:
      h.enterTest(25, "tuple1.clone().name == tuple1.name", Boolean.TRUE);
      t1.set("name", "one");
      t2 = (DynamicTuple)t1.clone();
      return box(t1.get("name") == t2.get("name"));

    case 26:
      h.enterTest(26, "tuple1.clone().id.equals(tuple1.id)", Boolean.TRUE);
      t2 = (DynamicTuple)t1.clone();
      return box(t1.id.equals(t2.id));

    default:
      throw new Bug("Invalid test number " + number);
    }
  }

  /** Clean up this test collection. */
  public void cleanup() {
    // Nothing to do.
  }

  /**
   * Box the specified boolean value.
   *
   * @param   b  The boolean to box.
   * @return     The boxed boolean.
   */
  private static Boolean box(boolean b) {
    return (b) ? Boolean.TRUE : Boolean.FALSE;
  }

  /**
   * Box the specified int value.
   *
   * @param   n  The int to box.
   * @return     The boxed int.
   */
  private static Integer box(int n) {
    return new Integer(n);
  }

}
