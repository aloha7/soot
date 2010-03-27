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

package one.world.binding;

import one.fonda.TestCollection;
import one.fonda.Harness;

import one.util.Guid;
import one.util.Bug;

import one.world.Constants;

import one.world.env.EnvironmentEvent;

import one.world.core.Component;
import one.world.core.ComponentDescriptor;
import one.world.core.DynamicTuple;
import one.world.core.Environment;
import one.world.core.Event;
import one.world.core.EventHandler;
import one.world.core.ExportedDescriptor;
import one.world.core.ImportedDescriptor;

import one.world.data.Name;

import one.world.util.AbstractHandler;

/**
 * Implementation of regression tests on the lease manager.
 *
 * @version   $Revision: 1.10 $
 * @author    Robert Grimm
 */
public class TestLeaseManager implements TestCollection {

  /** A component descriptor. */
  private static final ComponentDescriptor SELF =
    new ComponentDescriptor("one.world.binding.TestLeaseManager.Comp",
                            "A test component", true);

  /** An exported event handler descriptor. */
  private static final ExportedDescriptor MAIN =
    new ExportedDescriptor("main", "An exported event handler", null, null,
                           false);

  /** An imported event handler descriptor. */
  private static final ImportedDescriptor LEASE_MGR =
    new ImportedDescriptor("leaseMgr", "An imported event handler", null, null,
                           false, true);

  /** The main test component. */
  static final class Comp extends Component {

    /** The resource manager's event handler. */
    final class Resource extends AbstractHandler {

      /** Handle the specified event. */
      protected boolean handle1(Event e) {
        if (e instanceof LeaseEvent) {
          LeaseEvent le = (LeaseEvent)e;

          if (LeaseEvent.CANCELED == le.type) {
            if (closure.equals(le.closure)) {
              synchronized (lock) {
                active = false;
              }
            }

            return true;
          }
        }
        return false;
      }

    }

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

        } else if (e instanceof LeaseEvent) {
          LeaseEvent le = (LeaseEvent)e;

          if (LeaseEvent.ACQUIRED == le.type) {
            lease = le.handler;
            return true;

          } else if (LeaseEvent.RENEWED == le.type) {
            return true;

          } else if (LeaseEvent.CANCELED == le.type) {
            return true;
          }
        }

        return false;
      }

    }

    /** The main exported event handler. */
    final EventHandler       main;

    /** The lease manager imported event handler. */
    final Component.Importer leaseMgr;

    /** The resource's event handler. */
    final EventHandler       resource;

    /** The event handler for renewing/canceling the lease. */
    EventHandler             lease;

    /** The current state. */
    boolean active;

    /** The closure. */
    String  closure;

    /** The lock to protect the state. */
    final Object lock;

    /** Create a new test component. */
    public Comp(Environment env) {
      super(env);
      main     = declareExported(MAIN, new Handler());
      leaseMgr = declareImported(LEASE_MGR);
      resource = new Resource();
      lock     = new Object();
    }

    /** Get the descriptor for this component. */
    public ComponentDescriptor getDescriptor() {
      return (ComponentDescriptor)SELF.clone();
    }

    /** Reset. */
    public void reset(String closure) {
      synchronized (lock) {
        active       = true;
        this.closure = closure;
      }
    }

    /** Get the state. */
    public boolean isActive() {
      synchronized (lock) {
        return active;
      }
    }

  }

  /** The main component. */
  Comp  main;

  /** The lease manager component. */
  LeaseManager leaseMgr;

  /** Create a new lease manager test. */
  public TestLeaseManager() {
    // Nothing to do.
  }

  /** Get the name of this test collection. */
  public String getName() {
    return "one.world.binding.TestLeaseManager";
  }

  /** Get a description for this test collection. */
  public String getDescription() {
    return "Test lease management - this will take a while...";
  }

  /** Get the number of tests. */
  public int getTestNumber() {
    return 5;
  }

  /** Determine whether this test collection needs an environment. */
  public boolean needsEnvironment() {
    return true;
  }

  /** Initialize this test collection. */
  public boolean initialize(Environment env) {
    main     = new Comp(env);
    leaseMgr = new LeaseManager(env);

    env.link("main", "main", main);
    main.link("leaseMgr", "request", leaseMgr);

    return true;
  }

  /** Run the specified test. */
  public Object runTest(int number, Harness h, boolean verbose)
    throws Throwable {

    long    now     = System.currentTimeMillis();
    boolean active;

    switch(number) {

    case 1:
      h.enterTest(1, "Lease expiration", Boolean.FALSE);
      main.reset("one");
      main.leaseMgr.handle(new
        LeaseEvent(main.main, "one", LeaseEvent.ACQUIRE,
                   main.resource, new Name("Me"),
                   Constants.LEASE_MIN_DURATION));
      Thread.sleep(Constants.LEASE_MIN_DURATION * 2);
      return box(main.isActive());

    case 2:
      h.enterTest(2, "In the middle of the lease", Boolean.TRUE);
      main.reset("two");
      main.leaseMgr.handle(new
        LeaseEvent(main.main, "two", LeaseEvent.ACQUIRE,
                   main.resource, new Name("Me"),
                   Constants.LEASE_MIN_DURATION * 2));
      Thread.sleep(Constants.LEASE_MIN_DURATION);
      active = main.isActive();
      Thread.sleep(Constants.LEASE_MIN_DURATION * 2);
      return box(active);

    case 3:
      h.enterTest(3, "Lease cancellation", Boolean.FALSE);
      main.reset("three");
      main.leaseMgr.handle(new
        LeaseEvent(main.main, "three", LeaseEvent.ACQUIRE,
                   main.resource, new Name("Me"),
                   Constants.LEASE_MIN_DURATION * 2));
      Thread.sleep(Constants.LEASE_MIN_DURATION);
      main.lease.handle(new
        LeaseEvent(main.main, null, LeaseEvent.CANCEL, null, null, 0));
      active = main.isActive();
      Thread.sleep(Constants.LEASE_MIN_DURATION * 2);
      return box(active);

    case 4:
      h.enterTest(4, "Lease renewal", Boolean.TRUE);
      main.reset("four");
      main.leaseMgr.handle(new
        LeaseEvent(main.main, "four", LeaseEvent.ACQUIRE,
                   main.resource, new Name("Me"),
                   Constants.LEASE_MIN_DURATION * 2));
      Thread.sleep(Constants.LEASE_MIN_DURATION);
      main.lease.handle(new
        LeaseEvent(main.main, null, LeaseEvent.RENEW, null, null,
                   Constants.LEASE_MIN_DURATION * 2));
      Thread.sleep(Constants.LEASE_MIN_DURATION * 2);
      active = main.isActive();
      Thread.sleep(Constants.LEASE_MIN_DURATION * 2);
      return box(active);

    case 5:
      h.enterTest(5, "Lease expiration after renewal", Boolean.FALSE);
      main.reset("five");
      main.leaseMgr.handle(new
        LeaseEvent(main.main, "five", LeaseEvent.ACQUIRE,
                   main.resource, new Name("Me"),
                   Constants.LEASE_MIN_DURATION * 2));
      Thread.sleep(Constants.LEASE_MIN_DURATION);
      main.lease.handle(new
        LeaseEvent(main.main, null, LeaseEvent.RENEW, null, null,
                   Constants.LEASE_MIN_DURATION * 2));
      Thread.sleep(Constants.LEASE_MIN_DURATION * 4);
      return box(main.isActive());

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
