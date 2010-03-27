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

package one.tools;

import one.world.core.MalformedTupleException;
import one.world.core.Tuple;
import one.world.core.Type;

/**
 * Implementation of a tuple validator. The tuple validator is a
 * simple tool to test whether classes conform with the tuple
 * specification. It is invoked from the command line and treats all
 * arguments as the names of classes to be validated as tuples.
 *
 * @see      Tuple
 * @see      Type#validate(Class)
 *
 * @version  $Revision: 1.3 $
 * @author   Robert Grimm
 */
public class Validator {

  /** Hide constructor. */
  private Validator() {
    // Nothing to do.
  }

  /**
   * Validate the specified classes. All arguments passed to this
   * method are treated as the names of classes to be validated as
   * tuples.
   *
   * @param  args  The names of the classes to be validated.
   */
  public static void main(String[] args) {
    System.out.println();
    
    for (int i=0; i<args.length; i++) {
      String name = args[i];
      Class  k;

      System.out.print("Validating " + name);

      try {
        k = Class.forName(name);
      } catch (ClassNotFoundException x) {
        System.out.println("\n   *** No such class");
        continue;
      }

      try {
        Type.validate(k);
        System.out.println();
        continue;
      } catch (MalformedTupleException x) {
        System.out.println("\n   *** Malformed: " + x.getMessage());
        continue;
      }
    }
  }

}
