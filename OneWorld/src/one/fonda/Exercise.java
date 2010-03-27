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

package one.fonda;

import java.io.IOException;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;

import one.util.Bug;

import one.world.Main;

import one.world.core.Environment;

/**
 * Implementation of the exercise program.
 * 
 * @version  $Revision: 1.34 $
 * @author Robert Grimm
 */
public class Exercise implements Harness {
  
  /** Implementation of a failure, that is, a failed test. */
  final static class Failure {

    /** The collection of the failed test. */
    String    collection;

    /**
     * The number of the failed test or -2 if initialization failed or
     * -1 if clean-up failed.
     */
    int       number;

    /** The operation of the failed test. */
    String    operation;

    /** The expected result of the failed test. */
    Object    expectedResult;

    /**
     * The exception of the failed test or <code>null</code> if the
     * failed test has completed without signalling.
     */
    Throwable x;
    
    /** The actual result of the failed test. */
    Object    actualResult;

    /**
     * Create a new failure during initialization or clean-up.
     *
     * @param  collection  The collection for the new failure.
     * @param  init        <code>true</code> if the failure happened
     *                     during initialization.
     * @param  x           The throwable for the new failure.
     */
    Failure(String collection, boolean init, Throwable x) {
      this.collection     = collection;
      this.number         = (init? -2 : -1);
      this.operation      = null;
      this.expectedResult = null;
      this.x              = x;
      this.actualResult   = null;
    }

    /**
     * Create a new failure.
     *
     * @param  collection      The collection for the new failure.
     * @param  number          The number for the new failure.
     * @param  operation       The operation for the new failure.
     * @param  expectedResult  The expected result for the new failure.
     * @param  x               The throwable for the new failure.
     * @param  actualResult    The actual result for the new failure.
     */
    Failure(String collection, int number, String operation,
            Object expectedResult, Throwable x, Object actualResult) {
      this.collection     = collection;
      this.number         = number;
      this.operation      = operation;
      this.expectedResult = expectedResult;
      this.x              = x;
      this.actualResult   = actualResult;
    }

    /**
     * Convert this failure into a string.
     *
     * @return  A string representation of this failure.
     */
    public String toString() {
      if (-2 == number) {
        return ("***** " + collection + ".initialize -throws-> " + x);
      } else if (-1 == number) {
        return ("***** " + collection + ".cleanup -throws-> " + x);
      } else {
        return ("***** " + collection + "." + number + " " + operation +
                "\n      -expected-> " + expectedResult +
                ((null == x)?
                 (" -actual-> " + actualResult) : (" -throws-> " + x)));
      }
    }

  }

  // =======================================================================

  /** A line of dashes. */
  private static final String LINE =
    "-------------------------------------------------------------------------";

  /** The current test collection. */
  private String    collection;

  /** The current test number. */
  private int       number;

  /** The current test operation. */
  private String    operation;

  /** The current expected result. */
  private Object    expectedResult;

  /** The test collections for this exercise. */
  private HashMap   testCollections;

  /** The collection of failures. */
  private ArrayList failures;

  /** Create a new exercise. */
  public Exercise() {
    testCollections = new HashMap();
    failures        = new ArrayList();
  }

  /**
   * Add the specified test collection to this exercise.
   *
   * @param   c  The test collection to add.
   * @throws  NullPointerException
   *             Signals that <code>c</code> is <code>null</code>.
   * @throws  IllegalArgumentException
   *             Signals that a test collection with the same name
   *             as the specified test collection has already been
   *             added.
   */
  public void add(TestCollection c) {
    if (null == c) {
      throw new NullPointerException("Null test collection");
    } else if (testCollections.containsKey(c.getName())) {
      throw new IllegalArgumentException("Test collection " + c.getName() +
                                         " has already been added");
    } else {
      testCollections.put(c.getName(), c);
    }
  }

  /**
   * Get the test collection with the specified name.
   *
   * @param   name  The name of the test collection.
   * @return        The corresponding test collection or <code>null</code>
   *                if it has not been added before.
   */
  public TestCollection get(String name) {
    return (TestCollection)testCollections.get(name);
  }

  /** Clear all failure information. */
  public void clearFailures() {
    failures.clear();
  }

  /**
   * Perform all tests. 
   *
   * @param  verbose  Flag for whether failure output should be verbose.
   */
  public void runTests(boolean verbose) {
    // Run all test collections.
    Iterator iter = testCollections.values().iterator();

    while (iter.hasNext()) {
      runTests((TestCollection)iter.next(), verbose);
    }

    // Summarize exercises.

    System.out.println(LINE);
    System.out.println(LINE);
    
    if (0 == failures.size()) {
      System.out.println("AOK!");

    } else {
      System.out.println("Failures:");
      System.out.println();

      iter = failures.iterator();
      while(iter.hasNext()) {
        Failure f = (Failure)iter.next();
        System.out.println(f);
        if (verbose && (null != f.x)) {
          f.x.printStackTrace(System.out);
        }
      }
    }

    System.out.flush();
  }

  /**
   * Run the specified collection of tests and print the results to
   * the console.
   *
   * @param  c        The collection of tests to run.
   * @param  verbose  Flag for whether failure output should be verbose.
   */
  public void runTests(TestCollection c, boolean verbose) {
    System.out.println(LINE);
    System.out.println(c.getName() + " - " + c.getDescription());
    System.out.println();
    System.out.flush();

    collection = c.getName();

    Environment env      = null;
    Failure     f        = null;
    boolean     activate = false;
    boolean     cleanup  = true;

    // Set up environment if necessary.
    if (c.needsEnvironment()) {
      try {
        Environment root = Environment.getRoot();
        env = root.getChild("one.fonda.Exercise");
        if (null != env) {
          Environment.destroy(null, env.getId());
        }
        env = Environment.create(null, root.getId(), "one.fonda.Exercise", 
                                 false);
    } catch (Throwable x) {
        f = new Failure(collection, true, x);
        failures.add(f);
        System.out.println(f);
        if (verbose) {
          f.x.printStackTrace(System.out);
        }
        System.out.flush();
        cleanup = false;
      }
    }

    // Initialize test collection.
    if (null == f) {
      try {
        activate = c.initialize(env);
      } catch (Throwable x) {
        f = new Failure(collection, true, x);
        failures.add(f);
        System.out.println(f);
        if (verbose) {
          f.x.printStackTrace(System.out);
        }
        System.out.flush();
      }
    }

    if ((null != env) && activate) {
      try {
        Environment.activate(null, env.getId());
      } catch (Throwable x) {
        f = new Failure(collection, true, x);
        failures.add(f);
        System.out.println(f);
        if (verbose) {
          f.x.printStackTrace(System.out);
        }
        System.out.flush();
      }
    }

    // If initialization was successful, run tests.
    if (null == f) {
      int size = c.getTestNumber();
      for (int i=1; i<=size; i++) {
        runTest(c, i, verbose);
      }
    }
    System.out.flush();

    // Clean-up test collection.
    if (cleanup) {
      try {
        c.cleanup();
      } catch (Throwable x) {
        f = new Failure(collection, false, x);
        failures.add(f);
        System.out.println(f);
        if (verbose) {
          f.x.printStackTrace(System.out);
        }
        System.out.flush();
      }
    }

    // Destroy environment if necessary.
    if (null != env) {
      try {
        Environment.destroy(null, env.getId());
      } catch (Throwable x) {
        f = new Failure(collection, false, x);
        failures.add(f);
        System.out.println(f);
        if (verbose) {
          f.x.printStackTrace(System.out);
        }
        System.out.flush();
      }
    }

    System.out.println();
    System.out.flush();
  }

  /**
   * Run the specified test and print the result to the console.
   *
   * @param  c        The collection of the test.
   * @param  i        The number of the test.
   * @param  verbose  Flag for whether failure output should be verbose.
   */
  private void runTest(TestCollection c, int i, boolean verbose) {
    collection = c.getName();

    Object  r  = null;
    Failure f  = null;

    number     = i;
      
    // Perform the actual test.
    try {
      r = c.runTest(i, this, verbose);
    } catch (Throwable x) {
      f = new Failure(collection, i, operation, expectedResult, x, null);
    }
    
    // Make sure that the expected and actual results equal.
    if (null == f) {
      if (null == expectedResult) {
        if (null != r) {
          f = new Failure(collection, i, operation, expectedResult, null, r);
        }
      } else if (! expectedResult.equals(r)) {
        f = new Failure(collection, i, operation, expectedResult, null, r);
      }
    }
    
    // Print up and collect failure.
    if (null == f) {
      System.out.println(collection + "." + i + " " + operation + " -> " + r);
    } else {
      failures.add(f);
      System.out.println(f);
      if (verbose && (null != f.x)) {
        f.x.printStackTrace(System.out);
      }
    }
    System.out.flush();
  }

  /** Enter the specified test. */
  public void enterTest(int number, String operation, Object result) {
    if (this.number != number) {
      throw new Bug("Actual test (" + number + ") not the same as "
                    + "requested test (" + this.number + ")");
    }
    this.operation = (null == operation)? "" : operation;
    expectedResult = result;
  }

  /**
   * And one, and two, and three, and four, perform all exercises!
   *
   * @param  The arguments for the exercise.
   */
  public static void main(String[] args) {
    // Parse command line arguments.
    boolean verbose = false;
    String  test    = null;

    if (0 < args.length) {
      if ("-help".equals(args[0])) {
        System.out.println("Exercise command line arguments:");
        System.out.println("  [-help | -verbose] [Name of test collection]");

        // Done.
        return;
      } else if ("-verbose".equals(args[0])) {
        verbose = true;

        if (1 < args.length) {
          test = args[1];
        }
      } else {
        test = args[0];
      }
    }

    // Start up system.
    try {
      Main.startUp();
    } catch (IOException x) {
      System.out.println("Can't start up system: " + x.toString());
      return;
    }

    // Exercise set-up.
    Exercise fonda = new Exercise();

    // Individual tests go here (e.g., add "fonda.add(new Jane());").
    fonda.add(new one.world.core.TestTuple());
    fonda.add(new one.world.core.TestDynamicTuple());
    fonda.add(new one.world.core.TestAnimator());
    fonda.add(new one.world.core.TestEnvironment());
    fonda.add(new one.world.core.TestWrapper());
    fonda.add(new one.world.util.TestOperation());
    fonda.add(new one.world.util.TestTimer());
    fonda.add(new one.world.binding.TestLeaseManager());
    fonda.add(new one.world.io.TestSioResource());
    fonda.add(new one.world.io.TestTupleFilter());
    fonda.add(new one.world.io.TestPendingInputRequests());
    fonda.add(new one.world.io.TestNetworkIO());
    fonda.add(new one.world.io.TestDatagramIO());
    fonda.add(new one.world.io.TestTupleStore());
    fonda.add(new one.world.rep.TestREP());

    // Run tests.
    if (null == test) {
      fonda.runTests(verbose);
    } else {
      TestCollection c = fonda.get(test);

      if (null != c) {
        fonda.runTests(c, verbose);
      } else {
        System.out.println("Test collection " + test + " not registered");
      }
    }

    // Shut down system.
    Main.shutDown();

    // Exit Java. System.exit() is called explicitly so that the JVM
    // exists even if AWT/Swing has been initialized through the
    // one.world.swing.native.laf property.
    System.exit(0);
  }

}
