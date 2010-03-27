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

import one.util.Guid;
import one.util.Bug;

import one.world.env.EnvironmentEvent;

import one.world.core.Component;
import one.world.core.ComponentDescriptor;
import one.world.core.DynamicTuple;
import one.world.core.Environment;
import one.world.core.Event;
import one.world.core.EventHandler;
import one.world.core.ExportedDescriptor;
import one.world.core.ImportedDescriptor;

/**
 * Implementation of regression tests on timers.
 *
 * @version   $Revision: 1.8 $
 * @author    Robert Grimm
 */
public class TestTimer implements TestCollection {

  /** A component descriptor. */
  private static final ComponentDescriptor SELF =
    new ComponentDescriptor("one.world.util.TestTimer.Comp",
                            "A test component", true);

  /** An exported event handler descriptor. */
  private static final ExportedDescriptor MAIN =
    new ExportedDescriptor("main", "An exported event handler", null, null,
                           false);

  /** An imported event handler descriptor. */
  private static final ImportedDescriptor TIMER =
    new ImportedDescriptor("timer", "An imported event handler", null, null,
                           false, true);

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

        } else if (e instanceof Timer.Event) {
          Timer.Event te = (Timer.Event)e;

          if (Timer.SCHEDULED == te.type) {
            cancel = te.handler;
          }

          return true;

        } else if (e instanceof DynamicTuple) {
          synchronized (lock) {
            counter++;
          }
          return true;
        }

        return false;
      }

    }

    /** The main exported event handler. */
    final EventHandler       main;

    /** The timer imported event handler. */
    final Component.Importer timer;

    /** The event handler for canceling the timer. */
    EventHandler             cancel;

    /** The counter. */
    int counter;

    /** The lock to protect the counter. */
    final Object lock;

    /** Create a new test component. */
    public Comp(Environment env) {
      super(env);
      main  = declareExported(MAIN, new Handler());
      timer = declareImported(TIMER);
      lock  = new Object();
    }

    /** Get the descriptor for this component. */
    public ComponentDescriptor getDescriptor() {
      return (ComponentDescriptor)SELF.clone();
    }

    /** Reset the counter and cancel handler. */
    public void reset() {
      synchronized (lock) {
        counter = 0;
        cancel  = null;
      }
    }

    /** Get the counter value. */
    public int counter() {
      synchronized (lock) {
        return counter;
      }
    }

  }

  /** The main component. */
  Comp  main;

  /** The timer component. */
  Timer timer;

  /** Create a new timer test. */
  public TestTimer() {
    // Nothing to do.
  }

  /** Get the name of this test collection. */
  public String getName() {
    return "one.world.util.TestTimer";
  }

  /** Get a description for this test collection. */
  public String getDescription() {
    return "Test timed notification - this will take a while...";
  }

  /** Get the number of tests. */
  public int getTestNumber() {
    return 6;
  }

  /** Determine whether this test collection needs an environment. */
  public boolean needsEnvironment() {
    return true;
  }

  /** Initialize this test collection. */
  public boolean initialize(Environment env) {
    main  = new Comp(env);
    timer = new Timer(env);

    env.link("main", "main", main);
    main.link("timer", "request", timer);

    return true;
  }

  /** Run the specified test. */
  public Object runTest(int number, Harness h, boolean verbose)
    throws Throwable {

    main.reset();

    long               now = System.currentTimeMillis();
    Timer.Notification not;

    switch(number) {

    case 1:
      h.enterTest(1, "Notify once", box(1));
      main.timer.handle(new
        Timer.Event(main.main, null, Timer.SCHEDULE, Timer.ONCE,
                    now + 1000, 0, main.main, new DynamicTuple()));
      Thread.sleep(2000);
      main.cancel.handle(new Timer.Event(main.main, null, true));
      Thread.sleep(1000);
      return box(main.counter());

    case 2:
      h.enterTest(2, "Notify fixed rate", box(3));
      main.timer.handle(new
        Timer.Event(main.main, null, Timer.SCHEDULE, Timer.FIXED_RATE,
                    now + 1000, 1000, main.main, new DynamicTuple()));
      Thread.sleep(3500);
      main.cancel.handle(new Timer.Event(main.main, null, true));
      Thread.sleep(1000);
      return box(main.counter());

    case 3:
      h.enterTest(3, "Notify fixed delay", box(3));
      main.timer.handle(new
        Timer.Event(main.main, null, Timer.SCHEDULE, Timer.FIXED_DELAY,
                    now + 1000, 1000, main.main, new DynamicTuple()));
      Thread.sleep(3500);
      main.cancel.handle(new Timer.Event(main.main, null, true));
      Thread.sleep(1000);
      return box(main.counter());

    case 4:
      h.enterTest(4, "Schedule synchronously, notify once", box(1));
      not = timer.schedule(Timer.ONCE, now + 1000, 0, main.main,
                           new DynamicTuple());
      Thread.sleep(2000);
      not.cancel();
      Thread.sleep(1000);
      return box(main.counter());

    case 5:
      h.enterTest(5, "Schedule synchronously, notify fixed rate", box(3));
      not = timer.schedule(Timer.FIXED_RATE, now + 1000, 1000, main.main,
                           new DynamicTuple());
      Thread.sleep(3500);
      not.cancel();
      Thread.sleep(1000);
      return box(main.counter());

    case 6:
      h.enterTest(6, "Schedule synchronously, notify fixed delay", box(3));
      not = timer.schedule(Timer.FIXED_DELAY, now + 1000, 1000, main.main,
                           new DynamicTuple());
      Thread.sleep(3500);
      not.cancel();
      Thread.sleep(1000);
      return box(main.counter());

    default:
      throw new Bug("Invalid test number " + number);
    }
  }

  /** Clean up this test collection. */
  public void cleanup() {
    // Nothing to do.
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
