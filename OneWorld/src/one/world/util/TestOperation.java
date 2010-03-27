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

package one.world.util;

import one.fonda.TestCollection;
import one.fonda.Harness;

import one.util.Bug;
import one.util.Guid;

import one.world.core.Component;
import one.world.core.ComponentDescriptor;
import one.world.core.DynamicTuple;
import one.world.core.Environment;
import one.world.core.Event;
import one.world.core.ExceptionalEvent;
import one.world.core.ExportedDescriptor;

import one.world.env.EnvironmentEvent;

import one.world.rep.RemoteEvent;

/**
 * Implementation of regression tests on operations.
 *
 * @version   $Revision: 1.1 $
 * @author    Robert Grimm
 */
public class TestOperation implements TestCollection {

  /** The operation time-out. */
  static final long TIMEOUT  = 10 * 1000;

  /** The closure. */
  static final Guid CLOSURE  = new Guid();

  /** The second closure. */
  static final Guid CLOSURE2 = new Guid();

  /** A component descriptor. */
  private static final ComponentDescriptor SELF =
    new ComponentDescriptor("one.world.util.TestOperation.Comp",
                            "A test component", true);

  /** An exported event handler descriptor. */
  private static final ExportedDescriptor MAIN =
    new ExportedDescriptor("main", "An exported event handler", null, null,
                           false);

  /** The main test component. */
  static final class Comp extends Component {

    /** The main exported event handler. */
    final class Handler extends AbstractHandler {

      /** Handle the specified event. */
      protected boolean handle1(Event e) {

        if (e instanceof EnvironmentEvent) {
          EnvironmentEvent ee = (EnvironmentEvent)e;

          if (EnvironmentEvent.STOP == ee.type) {
            respond(e, new
              EnvironmentEvent(this, null, EnvironmentEvent.STOPPED,
                               getEnvironment().getId()));
          }
          return true;

        }

        return false;
      }

    }

    /** Create a new test component. */
    public Comp(Environment env) {
      super(env);
      declareExported(MAIN, new Handler());
    }

    /** Get the descriptor for this component. */
    public ComponentDescriptor getDescriptor() {
      return (ComponentDescriptor)SELF.clone();
    }

  }

  /** The request handler. */
  static class RequestHandler extends AbstractHandler {
    
    /** The number of events to silently drop. */
    int ignore;

    /**
     * Create a new request handler with the specified number of
     * events to ignore.
     */
    RequestHandler(int ignore) {
      this.ignore = ignore;
    }

    /** Handle the specified event. */
    protected boolean handle1(Event e) {
      synchronized (this) {
        if (0 < ignore) {
          ignore--;
          return true;
        }
      }

      e.source.handle(e);
      return true;
    }

  }

  /** The second request handler. */
  static class RequestHandler2 extends AbstractHandler {

    /** Handle the specified event. */
    protected boolean handle1(Event e) {
      Event inner = e;
      if (e instanceof RemoteEvent) {
        inner = ((RemoteEvent)e).event;
      }
      e.source.handle(new RemoteEvent(this, null, null, inner));
      return true;
    }

  }

  /** The continuation event handler. */
  static class Continuation extends AbstractHandler {

    /** The flag for whether the continuation has seen an event. */
    boolean done;

    /** The returned event. */
    Event   result;

    /** Handle the specified event. */
    protected boolean handle1(Event e) {
      synchronized (this) {
        if (! done) {
          done   = true;
          result = e;

          return true;
        }
      }

      throw new Bug("Duplicate result for operation");
    }

  }

  /** The request handler. */
  final RequestHandler req;

  /** The continuation. */
  final Continuation   cont;

  /** The operation. */
  Operation            op;

  /** Create a new operation test. */
  public TestOperation() {
    req  = new RequestHandler(0);
    cont = new Continuation();
  }

  /** Get the name of this test collection. */
  public String getName() {
    return "one.world.util.TestOperation";
  }

  /** Get a description for this test collection. */
  public String getDescription() {
    return "Test operations - this will take a while...";
  }

  /** Get the number of tests. */
  public int getTestNumber() {
    return 10;
  }

  /** Determine whether this test collection needs an environment. */
  public boolean needsEnvironment() {
    return true;
  }

  /** Initialize this test collection. */
  public boolean initialize(Environment env) {
    // Set up the operation.
    op = new Operation(2, TIMEOUT, new Timer(env), req, cont);

    // Set up a trivial component.
    Comp c = new Comp(env);
    env.link("main", "main", c);

    // The environment needs to be active for the timer to work.
    return true;
  }

  /** Run the specified test. */
  public Object runTest(int number, Harness h, boolean verbose)
    throws Throwable {

    // Reset continuation.
    cont.done   = false;
    cont.result = null;

    // Create an event.
    DynamicTuple              dt = new DynamicTuple(null, CLOSURE);
    Operation.ChainingClosure cl;
    RemoteEvent               re;

    switch(number) {

    case 1:
      h.enterTest(1, "Normal event flow", Boolean.TRUE);
      op.handle(dt);
      return box((cont.result instanceof DynamicTuple) &&
                 CLOSURE.equals(cont.result.closure));

    case 2:
      h.enterTest(2, "One retry", Boolean.TRUE);
      req.ignore = 1;
      op.handle(dt);
      Thread.sleep(2 * TIMEOUT);
      return box(cont.result instanceof DynamicTuple);

    case 3:
      h.enterTest(3, "Two retries", Boolean.TRUE);
      req.ignore = 2;
      op.handle(dt);
      Thread.sleep(3 * TIMEOUT);
      return box(cont.result instanceof DynamicTuple);

    case 4:
      h.enterTest(4, "Three retries", Boolean.TRUE);
      req.ignore = 3;
      op.handle(dt);
      Thread.sleep(4 * TIMEOUT);
      return box((cont.result instanceof ExceptionalEvent) &&
                 (((ExceptionalEvent)cont.result).x instanceof
                  TimeOutException) &&
                 CLOSURE.equals(cont.result.closure));

    case 5:
      h.enterTest(5, "Using a chaining closure", Boolean.TRUE);
      cl         = new Operation.ChainingClosure();
      dt.closure = cl;
      op.handle(dt);
      Thread.sleep(2 * TIMEOUT);
      return box((cont.result instanceof DynamicTuple) &&
                 cl.equals(cont.result.closure));

    case 6:
      h.enterTest(6, "Using the same chaining closure twice in a row",
                  Boolean.TRUE);
      cl          = new Operation.ChainingClosure();
      dt.closure  = cl;
      op.handle(dt);
      cont.done   = false;
      cont.result = null;
      op.handle(dt);
      return box((cont.result instanceof DynamicTuple) &&
                 cl.equals(cont.result.closure));

    case 7:
      h.enterTest(7, "Receiving a remote event", Boolean.TRUE);
      op.request = new RequestHandler2();
      op.handle(dt);
      return box((cont.result instanceof RemoteEvent) &&
                 CLOSURE.equals(cont.result.closure));

    case 8:
      h.enterTest(8, "Sending and receiving a remote event", Boolean.TRUE);
      re = new RemoteEvent(null, CLOSURE2, null, dt);
      op.handle(re);
      return box((cont.result instanceof RemoteEvent) &&
                 CLOSURE2.equals(cont.result.closure) &&
                 CLOSURE.equals(((RemoteEvent)cont.result).event.closure));

    case 9:
      h.enterTest(9, "Receiving a remote event with a chaining closure",
                  Boolean.TRUE);
      cl         = new Operation.ChainingClosure();
      dt.closure = cl;
      op.handle(dt);
      return box((cont.result instanceof RemoteEvent) &&
                 cl.equals(cont.result.closure));

    case 10:
      h.enterTest(10, "Sending and receiving a remote event with a chaining " +
                  "closure", Boolean.TRUE);
      cl         = new Operation.ChainingClosure();
      dt.closure = cl;
      re         = new RemoteEvent(null, CLOSURE2, null, dt);
      op.handle(re);
      return box((cont.result instanceof RemoteEvent) &&
                 CLOSURE2.equals(cont.result.closure) &&
                 cl.equals(((RemoteEvent)cont.result).event.closure));

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
