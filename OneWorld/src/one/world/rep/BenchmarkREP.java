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

package one.world.rep;

import one.world.Constants;

import one.world.core.Component;
import one.world.core.ComponentDescriptor;
import one.world.core.Environment;
import one.world.core.Event;
import one.world.core.EventHandler;
import one.world.core.ExportedDescriptor;
import one.world.core.ImportedDescriptor;
import one.world.core.DynamicTuple;
import one.world.core.SymbolicHandler;
import one.world.core.NoBufferSpaceException;
import one.world.core.ExceptionalEvent;

import one.world.env.EnvironmentEvent;

import one.world.util.AbstractHandler;
import one.world.util.NullHandler;
import one.world.util.SystemUtilities;
import one.world.util.TupleEvent;

/**
 * This class performs remote event passing benchmarks.
 * Currently, this class simply dispatches to the appropriate test
 * components in the <code>one.world.rep</code> package.
 * 
 * <p>Two tests can be performed using this class, each requiring two
 * components on different hosts.  The latency test consists of the test
 * component itself and an echo server.  The throughput test requires a
 * sender and a receiver.  Both the latency test component and the
 * throughput receiver allow the number of tests and the number of events
 * sent in each test to be specified.</p>
 *
 * <p>The various components are run from the <i>one.world</i> shell.  
 * For example, to load the
 * event latency test in a new environment named "latency", 
 * with the echo server running as "echo" on a machine named "remotehost",
 * with the default number of tests and events per test,
 * type the following in the shell:
 * <blockquote><code>mk latency one.world.rep.BenchmarkREP 
 * remotehost echo</code></blockquote></p>
 *
 * <p>The command-line arguments for the tests are as follows.</p>
 *
 * <dl>
 * <dt><code>el hostname resourcename [numTests numEvents]</code></dt>
 * <dd><p>Event Latency. Performs a latency test by timing the total round trip 
 * when sending to the echo server at the host and resource name specified.  
 * The average and standard deviation of the total time for
 * <code>numEvents</code> round trips are printed when the tests
 * have completed.  The number of tests to average over is specified by 
 * <code>numTests</code>.
 * See {@link one.world.rep.BenchmarkREPLatency} for more
 * information.</p></dd>
 *
 * <dt><code>echo</code></dt>
 * <dd><p>Echo server for the latency test.  Remotely exports itself under
 * its environment name and echos remote events back to the sender.  See 
 * {@link one.world.rep.BenchmarkREPEcho} for more information.</p></dd>
 * 
 * <dt><code>ets hostname resourcename</code></dt>
 * <dd><p>Tuple Throughput as Sender. Measures throughput by sending to a 
 * receiver at the specified host and resource name.  See
 * {@link one.world.rep.BenchmarkREPSender} for more information.</p></dd>
 * 
 * <dt><code>etr [numTests numEvents]</code></dt>
 * <dd><p>Tuple Throughput as Receiver. Exports itself under its environment 
 * name and measures throughput by receiving events from a sender started 
 * on another machine.  The average and standard deviation of the total time for
 * <code>numEvents</code> events to be received are printed when the tests
 * have completed.  The number of tests to average over is specified by 
 * <code>numTests</code>.
 * See {@link one.world.rep.BenchmarkREPReceiver} for more
 * information.</p></dd>
 * </dl>
 *
 * <p>Note: Because arguments are dispatched to other classes which can 
 * be run independently, error messages may include the aforementioned 
 * class names instead of the name of this class.</p>
 *
 * @version  $Revision 1.2 $
 * @author   Janet Davis
 */
public final class BenchmarkREP {

  private static final String USAGE_MSG = 
      "Usage: BenchmarkREP el/echo/ets/etr";

  // =======================================================================
  //                           Initializer
  // =======================================================================

  public static void init(Environment env, Object closure) throws Exception {
    String[] args = (String[])closure;
      
    if(args.length < 1) {
      throw new IllegalArgumentException(USAGE_MSG);
    }

    String[] myArgs = new String[args.length - 1];
    System.arraycopy((Object)args, 1, (Object)myArgs, 0, args.length-1);

    if(args.length >= 1) {
      if("el".equals(args[0])) { // Event Latency test
	BenchmarkREPLatency.init(env, myArgs);
      } else if ("echo".equals(args[0])) { // Echo server
        BenchmarkREPEcho.init(env, myArgs);
      } else if("ets".equals(args[0])) { // Event Throughput as Sender
	BenchmarkREPSender.init(env, myArgs);
      } else if("etr".equals(args[0])) { // Event Throughput as Receiver
	BenchmarkREPReceiver.init(env, myArgs);
      } else {
	throw new IllegalArgumentException(USAGE_MSG);
      }
    }
  }
}





