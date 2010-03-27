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

import one.world.core.Environment;

/**
 * Definition of a collection of regression tests.
 *
 * @version  $Revision: 1.5 $
 * @author   Robert Grimm
 */
public interface TestCollection {

  /**
   * Get the name of this test collection. The name of a test
   * collection typically is the fully qualified class or package name
   * of the class or package being exercised by this test collection.
   *
   * @return  The name of this test collection.
   */
  String getName();

  /**
   * Get a short description for this test collection.
   *
   * @return  A short description for this test collection.
   */
  String getDescription();

  /**
   * Get the number of tests in this test collection. Tests are
   * numbered starting at 1 up to and including the number returned by
   * this method.
   *
   * @return  The number of tests in this test collection.
   */
  int getTestNumber();

  /**
   * Determine whether this test collection needs an environment. If
   * this method returns <code>true</code>, the testing harness
   * creates a new environment before calling {@link #initialize}
   * and destroys that environment after calling {@link #cleanup}.
   *
   * @return  <code>true</code> if this test collection needs an
   *          environment for performing its tests.
   */
  boolean needsEnvironment();

  /**
   * Initialize this test collection. This method is called before any
   * of the tests in this test collection are performed. If this
   * method signals an exceptional condition, the tests in this test
   * collection are not executed, though {@link #cleanup} is invoked.
   *
   * @see     #needsEnvironment
   *
   * @param   env  The environment for this test collection, or
   *               <code>null</code> if this test collection does not
   *               need an environment.
   * @return       <code>true</code> if the specified environment needs
   *               to be activated before running the actual tests.
   * @throws  Throwable
   *               Signals some exceptional condition when
   *               initializing the specified test.
   */
  boolean initialize(Environment env) throws Throwable;

  /**
   * Run the specified test in the specified testing harness and
   * return the result.
   *
   * @param   number     The number of the test to run.
   * @param   harness    The testing harness to run the test in.
   * @param   verbose    Flag for whether the test should be verbose,
   *                     i.e. whether it should print intermediate
   *                     steps to the console.
   * @return             The result of running the specified test.
   * @throws  Throwable  Signals some exceptional condition when
   *                     performing the specified test.
   */
  Object runTest(int number, Harness harness, boolean verbose) throws Throwable;

  /**
   * Clean up this test collection. This method is called after all
   * requested tests in this collection have been performed.
   *
   * @throws  Throwable  Signals some exceptional condition when
   *                     initializing the specified test.
   */
  void cleanup() throws Throwable;

}
