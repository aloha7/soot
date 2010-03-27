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

package one.world.io;

import one.fonda.TestCollection;
import one.fonda.Harness;

import one.util.Bug;

import one.world.Constants;

import one.world.core.Environment;

/**
 * Implementation of regression tests for structured I/O resource
 * descriptors.
 *
 * @version   $Revision: 1.7 $
 * @author    Robert Grimm
 */
public class TestSioResource implements TestCollection {

  /** Create a new structured I/O resource descriptor test. */
  public TestSioResource() {
    // Nothing to do.
  }

  /** Get the name of this test collection. */
  public String getName() {
    return "one.world.io.TestSioResource";
  }

  /** Get a description for this test collection. */
  public String getDescription() {
    return "Creation of structured I/O resource descriptors from URLs";
  }

  /** Get the number of tests. */
  public int getTestNumber() {
    return 81;
  }

  /** Determine whether this test collection needs an environment. */
  public boolean needsEnvironment() {
    return false;
  }

  /** Initialize this test collection. */
  public boolean initialize(Environment env) throws Throwable {
    return false;
  }

  /** Run the specified test. */
  public Object runTest(int number, Harness h, boolean verbose)
    throws Throwable {

    switch(number) {

    case 1:
      return test(h, 1, "a", "a");
    case 2:
      return test(h, 2, ".", ".");
    case 3:
      return test(h, 3, "..", "..");
    case 4:
      return test(h, 4, "./a", "a");
    case 5:
      return test(h, 5, "a/.", "a");
    case 6:
      return test(h, 6, "../a", "../a");
    case 7:
      return test(h, 7, "a/..", ".");
    case 8:
      return test(h, 8, "a/", "a");
    case 9:
      return test(h, 9, "a////", "a");
    case 10:
      return test(h, 10, "a/b", "a/b");
    case 11:
      return test(h, 11, "a/b/", "a/b");
    case 12:
      return test(h, 12, "sio://", "sio:///");
    case 13:
      return test(h, 13, "sio:///", "sio:///");
    case 14:
      return test(h, 14, "sio:////", "sio:///");
    case 15:
      return test(h, 15, "sio:///.", "sio:///");
    case 16:
      return test(h, 16, "sio:///a", "sio:///a");
    case 17:
      return test(h, 17, "sio:///./a", "sio:///a");
    case 18:
      return test(h, 18, "sio:///a/.", "sio:///a");
    case 19:
      return test(h, 19, "sio:///a/..", "sio:///");
    case 20:
      return test(h, 20, "sio:///..", null);
    case 21:
      return test(h, 21, "sio:///././././///////////././././.", "sio:///");
    case 22:
      return test(h, 22, "sio:///00000000-0000-0000-0000-000000000000",
                  "sio:///00000000-0000-0000-0000-000000000000");
    case 23:
      return test(h, 23, "a/", "a");
    case 24:
      return test(h, 24, "sio:///a/", "sio:///a");
    case 25:
      return test(h, 25, "sio:///00000000-0000-0000-0000-000000000000/",
                  "sio:///00000000-0000-0000-0000-000000000000");
    case 26:
      return test(h, 26,
                  "sio:///a/00000000-0000-0000-0000-000000000000/b",
                  "sio:///00000000-0000-0000-0000-000000000000/b");
    case 27:
      return test(h, 27,
                  "sio:///a/00000000-0000-0000-0000-000000000000/b/",
                  "sio:///00000000-0000-0000-0000-000000000000/b");
    case 28:
      return test(h, 28,
                  "sio:///a/00000000-0000-0000-0000-000000000000/b" +
                  "/11111111-1111-1111-1111-111111111111",
                  "sio:///11111111-1111-1111-1111-111111111111");
    case 29:
      return test(h, 29,
                  "sio:///a/00000000-0000-0000-0000-000000000000/b" +
                  "/11111111-1111-1111-1111-111111111111/c",
                  "sio:///11111111-1111-1111-1111-111111111111/c");
    case 30:
      return test(h, 30, "a/00000000-0000-0000-0000-000000000000", null);
    case 31:
      return test(h, 31,
                  "sio:///a/00000000-0000-0000-0000-000000000000/b" +
                  "/11111111-1111-1111-1111-111111111111/c/../..",
                  "sio:///11111111-1111-1111-1111-111111111111/..");
    case 32:
      return test(h, 32, "a?type=storage", "a");
    case 33:
      return test(h, 33, "sio:///?type=storage", "sio:///");
    case 34:
      return test(h, 34, "sio://?type=storage", "sio:///");
    case 35:
      return test(h, 35, "sio://a/b", "sio://a/b");
    case 36:
      return test(h, 36, "sio://a/b?type=storage", "sio://a/b");
    case 37:
      return test(h, 37, "sio://a:80/b", "sio://a:80/b");
    case 38:
      return test(h, 38, "sio://a:" + Constants.PORT + "/b", "sio://a/b");
    case 39:
      return test(h, 39, "sio:///?duration=1", null);
    case 40:
      return test(h, 40, "sio:///?closure=hello", null);
    case 41:
      return test(h, 41, "sio:///?local=one", null);
    case 42:
      return test(h, 42, "", null);
    case 43:
      return test(h, 43, "sio://one&type=server", null);
    case 44:
      return test(h, 44, "sio://one:12a/", null);
    case 45:
      return test(h, 45, "/", null);
    case 46:
      return test(h, 46, "sio:///&type=client", null);
    case 47:
      return test(h, 47, "sio:///?type=server/", null);
    case 48:
      return test(h, 48, "sio:///?type=server?", null);
    case 49:
      return test(h, 49, "sio:///?type", null);
    case 50:
      return test(h, 50, "sio:///?type=weird-type", null);
    case 51:
      return test(h, 51, "sio://one/?local=two:50a", null);
    case 52:
      return test(h, 52, "sio://one?duration=50a", null);
    case 53:
      return test(h, 53, "sio://one?weird-attribute=one", null);
    case 54:
      return test(h, 54, "sio://one.cs.washington.edu",
                  "sio://one.cs.washington.edu");
    case 55:
      return test(h, 55, "sio://one.cs.washington.edu?type=client",
                  "sio://one.cs.washington.edu");
    case 56:
      return test(h, 56, "sio://one.cs.washington.edu?" +
                  "local=two.cs.washington.edu:80&type=client",
                  "sio://one.cs.washington.edu?local=two.cs.washington.edu:80");
    case 57:
      return test(h, 57, "sio://one?type=server", "sio://one?type=server");
    case 58:
      return test(h, 58, "sio://one?duration=1&type=server",
                  "sio://one?type=server&duration=1");
    case 59:
      return test(h, 59, "sio://one?closure=Hello World!&type=server",
                  "sio://one?type=server&closure=Hello+World%21");
    case 60:
      return test(h, 60, "sio://one?duration=" +
                  Constants.LEASE_DEFAULT_DURATION + "&type=server",
                  "sio://one?type=server");
    case 61:
      return test(h, 61, "sio://one?closure=&type=server",
                  "sio://one?type=server&closure=");
    case 62:
      return test(h, 62, "sio://?type=server", null);
    case 63:
      return test(h, 63, "sio://one/a?type=server", null);
    case 64:
      return test(h, 64, "a/b?type=server", null);
    case 65:
      return test(h, 65, "sio://one?type=server&duration=-1", null);
    case 66:
      return test(h, 66, "sio://one?type=server&local=two", null);
    case 67:
      return test(h, 67, "sio://one?type=input", "sio://one?type=input");
    case 68:
      return test(h, 68, "sio://one?type=input&local=two", null);
    case 69:
      return test(h, 69, "sio://one?type=output", "sio://one?type=output");
    case 70:
      return test(h, 70, "sio://one?local=two&type=output",
                  "sio://one?type=output&local=two");
    case 71:
      return test(h, 71, "sio://one?local=two&type=duplex",
                  "sio://one?type=duplex&local=two");
    case 72:
      return test(h, 72, "sio://one?type=duplex", null);
    case 73:
      h.enterTest(73, "(a).isRelative()", Boolean.TRUE);
      return new Boolean((new SioResource("a")).isRelative());
    case 74:
      h.enterTest(74, "(sio:///).isRelative()", Boolean.FALSE);
      return new Boolean((new SioResource("sio:///")).isRelative());
    case 75:
      h.enterTest(75, "(sio:///00000000-0000-0000-0000-000000000000/a)." +
                  "isRelative()", Boolean.FALSE);
      return new Boolean((new
        SioResource("sio:///00000000-0000-0000-0000-000000000000" +
                    "/a").isRelative()));
    case 76:
      h.enterTest(76, "(a).isAbsolute()", Boolean.FALSE);
      return new Boolean((new SioResource("a")).isAbsolute());
    case 77:
      h.enterTest(77, "(sio:///).isAbsolute()", Boolean.TRUE);
      return new Boolean((new SioResource("sio:///")).isAbsolute());
    case 78:
      h.enterTest(78, "(sio:///00000000-0000-0000-0000-000000000000/a)." +
                  "isAbsolute()", Boolean.FALSE);
      return new Boolean((new
        SioResource("sio:///00000000-0000-0000-0000-000000000000" +
                    "/a").isAbsolute()));
    case 79:
      h.enterTest(79, "(a).isIdRelative()", Boolean.FALSE);
      return new Boolean((new SioResource("a")).isIdRelative());
    case 80:
      h.enterTest(80, "(sio:///).isIdRelative()", Boolean.FALSE);
      return new Boolean((new SioResource("sio:///")).isIdRelative());
    case 81:
      h.enterTest(81, "(sio:///00000000-0000-0000-0000-000000000000/a)." +
                  "isIdRelative()", Boolean.TRUE);
      return new Boolean((new
        SioResource("sio:///00000000-0000-0000-0000-000000000000" +
                    "/a").isIdRelative()));
    default:
      throw new Bug("Invalid test number " + number);
    }
  }

  /** Clean up this test collection. */
  public void cleanup() {
    // Nothing to do.
  }

  /**
   * Create a new structured I/O resource descriptor from the
   * specified URL and return the result of converting the newly
   * created resource descriptor to a string.
   *
   * @param   h         The test harness.
   * @param   n         The test number.
   * @param   url       The URL to test.
   * @param   expected  The expected result.
   * @return            The result of converting the newly created
   *                    resource descriptor to a string, or
   *                    <code>null</code> if creating the resource
   *                    descriptor resulted in an exception.
   */
  private String test(Harness h, int n, String url, String result) {
    h.enterTest(n, url, result);

    String r;
    try {
      r = (new SioResource(url)).toString();
    } catch (IllegalArgumentException x) {
      return null;
    }
    return r;
  }

}
