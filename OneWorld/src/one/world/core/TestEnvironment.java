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
import java.io.ObjectInputStream;

import one.fonda.TestCollection;
import one.fonda.Harness;

import one.util.Bug;
import one.util.Guid;

import one.world.Constants;

import one.world.binding.Duration;

import one.world.core.Event;
import one.world.core.ExceptionalEvent;

import one.world.env.EnvironmentEvent;

import one.world.util.AbstractHandler;
import one.world.util.Synchronous;
import one.world.util.Synchronous.ResultHandler;

/**
 * Implementation of regression tests on environments.
 *
 * @version   $Revision: 1.19 $
 * @author    Robert Grimm
 */
public class TestEnvironment implements TestCollection {

  /** A component descriptor. */
  private static final ComponentDescriptor SELF =
    new ComponentDescriptor("one.world.core.TestEnvironment.Comp",
                            "A test component", true);

  /** An exported event handler descriptor. */
  private static final ExportedDescriptor BINGO =
    new ExportedDescriptor("bingo", "An exported event handler",
                           null, null, false);

  // ------------------------------------------------------------------------

  /** A test component. */
  public static class Comp extends Component {

    /** The event handler for this component. */
    class Bingo extends AbstractHandler {

      /** Handle the specified event. */
      protected boolean handle1(Event e) {

        if (e instanceof EnvironmentEvent) {
          EnvironmentEvent ee = (EnvironmentEvent)e;

          if (EnvironmentEvent.ACTIVATED == ee.type) {
            add("start ");
          } else if (EnvironmentEvent.RESTORED == ee.type) {
            add("restore ");
          } else if (EnvironmentEvent.MOVED == ee.type) {
            add("move ");
          } else if (EnvironmentEvent.CLONED == ee.type) {
            add("copy ");
          } else if (EnvironmentEvent.STOP == ee.type) {
            add("stop ");
            respond(e, new
              EnvironmentEvent(this, null, EnvironmentEvent.STOPPED,
                               getEnvironment().getId()));
          } else {
            return false;
          }

          return true;

        } else if (e instanceof DynamicTuple) {
          DynamicTuple dt = new DynamicTuple(this, null);

          dt.set("msg", message());
          respond(e, dt);
          return true;
        }

        return false;
      }
    }

    /** The message. */
    String  msg;

    /** The lock protecting the message. */
    transient Object lock;

    /** Create a new test component. */
    public Comp(Environment env) {
      super(env);
      declareExported(BINGO, new Bingo());

      msg  = "create ";
      lock = new Object();
    }

    /** Deserialize a test component. */
    private void readObject(ObjectInputStream in)
      throws IOException, ClassNotFoundException {

      in.defaultReadObject();
      lock = new Object();
    }

    /**
     * Reset the message for this environment test.
     */
    void reset() {
      synchronized (lock) {
        msg = "";
      }
    }
    
    /**
     * Add the specified string to the message for this environment test.
     *
     * @param  s  The string to add.
     */
    void add(String s) {
      synchronized (lock) {
        msg = msg + s;
      }
    }
    
    /**
     * Get the message for this environment test.
     *
     * @return  The message for this environment test.
     */
    String message() {
      synchronized (lock) {
        return msg;
      }
    }

    /** Get the descriptor for this component. */
    public ComponentDescriptor getDescriptor() {
      return (ComponentDescriptor)SELF.clone();
    }

    /**
     * Initialize the specified environment. This method creates a new
     * test component in the specified environment and links its bingo
     * exported event handler to the environment's main event handler.
     *
     * @param   env      The environment to initialize.
     * @param   closure  The closure (which is ignored).
     */
    public static void init(Environment env, Object closure) {
      Comp comp = new Comp(env);

      env.link("main", "bingo", comp);
    }
  }

  // ------------------------------------------------------------------------

  /** The test environment for this test collection. */
  private Environment env;

  /** Create a new environment test. */
  public TestEnvironment() {
    // Nothing to do.
  }

  /** Get the name of this test collection. */
  public String getName() {
    return "one.world.core.TestEnvironment";
  }

  /** Get a description for this test collection. */
  public String getDescription() {
    return "Test environment operations";
  }

  /** Get the number of tests. */
  public int getTestNumber() {
    return 15;
  }

  /** Determine whether this test collection needs an environment. */
  public boolean needsEnvironment() {
    return true;
  }

  /** Initialize this test collection. */
  public boolean initialize(Environment env) {
    this.env = env;

    return false;
  }

  /** Run the specified test. */
  public Object runTest(int number, Harness h, boolean verbose)
    throws Throwable {

    Environment tmp;
    long        timestamp;

    switch(number) {

    case 1:
      h.enterTest(1, "Create", Boolean.TRUE);
      Environment.create(null, env.getId(), "one", false);
      return box(null != env.getChild("one"));
   
    case 2:
      h.enterTest(2, "Create with initializer", Boolean.TRUE);
      tmp = Environment.create(null, env.getId(), "two", false,
                               "one.world.core.TestEnvironment$Comp", null);
      return box((null != env.getChild("two")) &&
                 tmp.getMain().isLinked());

    case 3:
      h.enterTest(3, "Create and load", Boolean.TRUE);
      tmp = Environment.create(null, env.getChild("one").getId(), "three",
                               false);
      Environment.load(null, tmp.getId(),
                       "one.world.core.TestEnvironment$Comp", null);
      return box((null != env.getChild("one")) &&
                 (null != env.getChild("one").getChild("three")) &&
                 tmp.getMain().isLinked());

    case 4:
      h.enterTest(4, "Rename", Boolean.TRUE);
      tmp = env.getChild("one");
      Environment.rename(null, tmp.getChild("three").getId(), "four");
      return box((null != env.getChild("one")) &&
                 (null == tmp.getChild("three")) &&
                 (null != tmp.getChild("four")));

    case 5:
      h.enterTest(5, "Move locally", Boolean.TRUE);
      tmp = env.getChild("one");
      Environment.move(null, tmp.getChild("four").getId(), env.getId());
      return box((null != env.getChild("one")) &&
                 (null == env.getChild("one").getChild("four")) &&
                 (null != env.getChild("two")) &&
                 (null != env.getChild("four")));

    case 6:
      h.enterTest(6, "Activate", "create start ");
      tmp = env.getChild("two");
      Environment.activate(null, tmp.getId());
      Thread.sleep(1000);
      return message(tmp);

    case 7:
      h.enterTest(7, "Terminate", "create start stop start ");
      tmp = env.getChild("two");
      Environment.terminate(null, tmp.getId());
      Environment.activate(null, tmp.getId());
      Thread.sleep(1000);
      return message(tmp);

    case 8:
      h.enterTest(8, "Another round of activate and terminate",
                  "create start stop start stop start ");
      tmp = env.getChild("two");
      Environment.terminate(null, tmp.getId());
      Environment.activate(null, tmp.getId());
      Thread.sleep(1000);
      return message(tmp);

    case 9:
      h.enterTest(9, "Destroy", Boolean.FALSE);
      Environment.destroy(null, env.getChild("two").getId());
      return box(null != env.getChild("two"));

    case 10:
      h.enterTest(10, "Simple check-point", "create start restore ");
      tmp = Environment.create(null, env.getId(), "cp1", false,
                               "one.world.core.TestEnvironment$Comp", null);
      Environment.activate(null, tmp.getId());
      Thread.sleep(1000);
      timestamp = Environment.checkPoint(null, tmp.getId());
      Environment.terminate(null, tmp.getId());
      Environment.activate(null, tmp.getId());
      Thread.sleep(1000);
      Environment.restore(null, tmp.getId(), timestamp);
      Thread.sleep(1000);
      return message(tmp);

    case 11:
      h.enterTest(11, "Simple check-point restored twice",
                  "create start restore ");
      tmp = Environment.create(null, env.getId(), "cp2", false,
                               "one.world.core.TestEnvironment$Comp", null);
      Environment.activate(null, tmp.getId());
      Thread.sleep(1000);
      timestamp = Environment.checkPoint(null, tmp.getId());
      Environment.terminate(null, tmp.getId());
      Environment.activate(null, tmp.getId());
      Thread.sleep(1000);
      Environment.restore(null, tmp.getId(), timestamp);
      Thread.sleep(1000);
      Environment.terminate(null, tmp.getId());
      Environment.activate(null, tmp.getId());
      Thread.sleep(1000);
      Environment.restore(null, tmp.getId(), timestamp);
      Thread.sleep(1000);
      return message(tmp);

    case 12:
      h.enterTest(12, "Nested check-point", Boolean.TRUE);
      {
        Environment tmp1 =
          Environment.create(null, env.getId(), "cp3", false,
                             "one.world.core.TestEnvironment$Comp", null);
        Environment tmp2 =
          Environment.create(null, tmp1.getId(), "cp4", false,
                             "one.world.core.TestEnvironment$Comp", null);
        Environment tmp3 =
          Environment.create(null, tmp1.getId(), "cp5", false,
                             "one.world.core.TestEnvironment$Comp", null);
        Environment.activate(null, tmp1.getId());
        Environment.activate(null, tmp2.getId());
        Environment.activate(null, tmp3.getId());
        Thread.sleep(1000);
        Environment.terminate(null, tmp2.getId());
        Environment.terminate(null, tmp3.getId());
        Environment.activate(null, tmp2.getId());
        Environment.activate(null, tmp3.getId());
        Thread.sleep(1000);
        Environment.terminate(null, tmp3.getId());
        Thread.sleep(1000);
        timestamp = Environment.checkPoint(null, tmp1.getId());
        Environment.activate(null, tmp3.getId());
        Thread.sleep(1000);
        Environment.terminate(null, tmp1.getId());
        Environment.terminate(null, tmp2.getId());
        Environment.terminate(null, tmp3.getId());
        Thread.sleep(1000);
        Environment.restore(null, tmp1.getId(), timestamp);
        Thread.sleep(1000);
        Environment.activate(null, tmp3.getId());
        Thread.sleep(1000);
        return box(message(tmp1).equals("create start restore ") &&
                   message(tmp2).equals("create start stop start restore ") &&
                   message(tmp3).equals("create start stop start stop start "));
      }

    case 13:
      h.enterTest(13, "Most recent check-point",
                  "create start stop start stop start restore ");
      tmp = Environment.create(null, env.getId(), "cp6", false,
                               "one.world.core.TestEnvironment$Comp", null);
      Environment.activate(null, tmp.getId());
      Thread.sleep(1000);
      Environment.checkPoint(null, tmp.getId());
      Environment.terminate(null, tmp.getId());
      Environment.activate(null, tmp.getId());
      Thread.sleep(1000);
      Environment.checkPoint(null, tmp.getId());
      Environment.terminate(null, tmp.getId());
      Environment.activate(null, tmp.getId());
      Thread.sleep(1000);
      Environment.checkPoint(null, tmp.getId());
      Environment.terminate(null, tmp.getId());
      Thread.sleep(1000);
      Environment.restore(null, tmp.getId(), -1);
      Thread.sleep(1000);
      return message(tmp);

    case 14:
      h.enterTest(14, "Copy environments", Boolean.TRUE);
      {
        Environment tmp1 =
          Environment.create(null, env.getId(), "cp7", false);
        Environment tmp2 =
          Environment.create(null, tmp1.getId(), "cp8", false);
        Environment tmp3 =
          Environment.create(null, tmp2.getId(), "cp9", false);
        Environment tmp4 =
          Environment.create(null, tmp3.getId(), "cp10", false,
                             "one.world.core.TestEnvironment$Comp", null);
        Environment tmp5 =
          Environment.create(null, tmp3.getId(), "cp11", false,
                             "one.world.core.TestEnvironment$Comp", null);
        Environment.activate(null, tmp4.getId());
        Environment.activate(null, tmp5.getId());
        Thread.sleep(1000);
        Environment.terminate(null, tmp5.getId());
        Environment.activate(null, tmp5.getId());
        Thread.sleep(1000);
        Environment.copy(null, tmp3.getId(), tmp1.getId());
        Thread.sleep(1000);
        Environment tmp6 = tmp1.getChild("cp9");
        Environment tmp7 = tmp6.getChild("cp10");
        Environment tmp8 = tmp6.getChild("cp11");
        return box(message(tmp4).equals("create start ") &&
                   message(tmp5).equals("create start stop start ") &&
                   message(tmp7).equals("create start copy ") &&
                   message(tmp8).equals("create start stop start copy "));
      }

    case 15:
      h.enterTest(15, "Remote copy environments", Boolean.TRUE);
      {
        Environment tmp1 =
          Environment.create(null, env.getId(), "cp12", false);
        Environment tmp2 =
          Environment.create(null, tmp1.getId(), "cp13", false);
        Environment tmp3 =
          Environment.create(null, tmp2.getId(), "cp14", false);
        Environment tmp4 =
          Environment.create(null, tmp3.getId(), "cp15", false,
                             "one.world.core.TestEnvironment$Comp", null);
        Environment tmp5 =
          Environment.create(null, tmp3.getId(), "cp16", false,
                             "one.world.core.TestEnvironment$Comp", null);
        Environment.activate(null, tmp4.getId());
        Environment.activate(null, tmp5.getId());
        Thread.sleep(1000);
        Environment.terminate(null, tmp5.getId());
        Environment.activate(null, tmp5.getId());
        Thread.sleep(1000);
        ResultHandler rh = new ResultHandler();
        Environment.moveAway(null, rh, null, tmp3.getId(), "localhost",
                             Constants.REP_PORT, "/" + env.getName() + "/" +
                             tmp1.getName(), true);
        Event         e  = rh.getResult(Duration.YEAR);
        if (e instanceof ExceptionalEvent) {
          throw ((ExceptionalEvent)e).x;
        }
        Thread.sleep(1000);
        Environment tmp6 = tmp1.getChild("cp14");
        Environment tmp7 = tmp6.getChild("cp15");
        Environment tmp8 = tmp6.getChild("cp16");
        return box(message(tmp4).equals("create start ") &&
                   message(tmp5).equals("create start stop start ") &&
                   message(tmp7).equals("create start copy ") &&
                   message(tmp8).equals("create start stop start copy "));
      }

    default:
      throw new Bug("Invalid test number " + number);
    }
  }

  /** Clean up this test collection. */
  public void cleanup() {
    // Nothing to do.
  }

  /**
   * Get the message from the specified environment's main component.
   *
   * @param   env  The environment, whose message to retrieve.
   * @return       The message from the specified environment's main
   *               component.
   */
  private String message(Environment env) {
    EventHandler main   = env.getMain();
    Event        result = Synchronous.invoke(main, new DynamicTuple());

    if (result instanceof ExceptionalEvent) {
      System.out.println("*** " + ((ExceptionalEvent)result).x);
    }

    return (String)result.get("msg");
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
