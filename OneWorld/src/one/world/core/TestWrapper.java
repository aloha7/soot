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

import one.fonda.TestCollection;
import one.fonda.Harness;

import one.util.Bug;

import one.world.env.EnvironmentEvent;

import one.world.util.AbstractHandler;

/**
 * Implementation of regression tests on event wrapping.
 *
 * @version   $Revision: 1.6 $
 * @author    Robert Grimm
 */
public class TestWrapper implements TestCollection {

  /** A component descriptor. */
  private static final ComponentDescriptor SELF =
    new ComponentDescriptor("one.world.core.TestWrapper.Comp",
                            "A test component", true);

  /** An exported event handler descriptor. */
  private static final ExportedDescriptor FOO =
    new ExportedDescriptor("foo", "An exported event handler", null, null,
                           false);

  /** An imported event handler descriptor. */
  private static final ImportedDescriptor BAR =
    new ImportedDescriptor("bar", "An imported event handler", null, null,
                           false, false);

  // ------------------------------------------------------------------------

  /** A test component. */
  public static class Comp extends Component {

    /** The event handler for this component. */
    class Foo extends AbstractHandler {

      /** Handle the specified event. */
      protected boolean handle1(Event e) {

        if (e instanceof EnvironmentEvent) {
          EnvironmentEvent ee = (EnvironmentEvent)e;

          if (EnvironmentEvent.STOP == ee.type) {
            respond(e, new
              EnvironmentEvent(this, null, EnvironmentEvent.STOPPED,
                               getEnvironment().getId()));
          } else if (EnvironmentEvent.ACTIVATED == ee.type) {
            // Nothing to do.
          } else {
            return false;
          }

          return true;

        } else if (e instanceof DynamicTuple) {
          if ("one".equals(e.get("msg"))) {
            DynamicTuple dt = new DynamicTuple(this, null);

            dt.set("msg", "two");
            respond(e, dt);
          }

          msg = (DynamicTuple)e;

          return true;
        }

        return false;
      }
    }

    /** The foo exported event handler. */
    final EventHandler       foo;

    /** The bar imported event handler. */
    final Component.Importer bar;

    /** The message. */
    DynamicTuple             msg;

    /** Create a new test component. */
    public Comp(Environment env) {
      super(env);
      foo = declareExported(FOO, new Foo());
      bar = declareImported(BAR);
    }

    /** Get the descriptor for this component. */
    public ComponentDescriptor getDescriptor() {
      return (ComponentDescriptor)SELF.clone();
    }
  }

  // ------------------------------------------------------------------------

  /** The local root environment. */
  Environment root;

  /** The first test component. */
  Comp eins;

  /** The second test component. */
  Comp zwei;

  /** Create a new wrapper test. */
  public TestWrapper() {
    // Nothing to do.
  }

  /** Get the name of this test collection. */
  public String getName() {
    return "one.world.core.TestWrapper";
  }

  /** Get a description for this test collection. */
  public String getDescription() {
    return "Test event wrapping";
  }

  /** Get the number of tests. */
  public int getTestNumber() {
    return 2;
  }

  /** Determine whether this test collection needs an environment. */
  public boolean needsEnvironment() {
    return true;
  }

  /** Initialize this test collection. */
  public boolean initialize(Environment env) throws IOException {
    root = env;

    return false;
  }

  /** Run the specified test. */
  public Object runTest(int number, Harness h, boolean verbose)
    throws Throwable {
    
    boolean     inherit = (1 == number);
    Environment one     = Environment.create(null, root.getId(),
                                             "TestWrapper-" + number + "-one",
                                             inherit);
    Environment two     = Environment.create(null, root.getId(),
                                             "TestWrapper-" + number + "-two",
                                             inherit);
    eins                = new Comp(one);
    zwei                = new Comp(two);

    one.link("main", "foo", eins);
    two.link("main", "foo", zwei);
    eins.link("bar", "foo", zwei);

    Environment.activate(null, one.getId());
    Environment.activate(null, two.getId());

    DynamicTuple dt;

    switch(number) {
    case 1:
      h.enterTest(1, "Event ping-pong within protection domain", Boolean.TRUE);
      dt = new DynamicTuple(eins.foo, null);
      dt.set("msg", "one");
      eins.bar.handle(dt);
      Thread.sleep(1000);
      return box(ConcurrencyDomain.isWrapped(eins.msg.source) &&
                 ConcurrencyDomain.isWrapped(zwei.msg.source) &&
                 (dt == zwei.msg));

    case 2:
      h.enterTest(2, "Event ping-pong across protection domains", Boolean.TRUE);
      dt = new DynamicTuple(eins.foo, null);
      dt.set("msg", "one");
      eins.bar.handle(dt);
      Thread.sleep(1000);
      return box(ConcurrencyDomain.isWrapped(eins.msg.source) &&
                 ConcurrencyDomain.isWrapped(zwei.msg.source) &&
                 (dt != zwei.msg) && dt.equals(zwei.msg) &&
                 dt.id.equals(zwei.msg.id));

    default:
      throw new Bug("Invalid test number " + number);
    }
  }

  /** Clean up this test collection. */
  public void cleanup() {
    // Nothing to do.
  }

  /**
   * Box the specified boolean.
   *
   * @param   b  The boolean to box.
   * @return     The boxed boolean.
   */
  private static Boolean box(boolean b) {
    return (b? Boolean.TRUE : Boolean.FALSE);
  }

}
