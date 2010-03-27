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

package one.world.io;

import one.world.binding.*;

import one.world.Constants;

import one.world.core.Component;
import one.world.core.ComponentDescriptor;
import one.world.core.Environment;
import one.world.core.Event;
import one.world.core.EventHandler;
import one.world.core.ExportedDescriptor;
import one.world.core.ImportedDescriptor;
import one.world.core.DynamicTuple;
import one.world.core.NoBufferSpaceException;
import one.world.core.ExceptionalEvent;

import one.world.env.EnvironmentEvent;

import one.world.util.AbstractHandler;
import one.world.util.NullHandler;
import one.world.util.SystemUtilities;
import one.world.util.TupleEvent;

import one.world.data.Name;

import java.io.IOException;
//The following classes are not available in general
import java.io.ObjectOutputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.File;


/**
 * Benchmarks {@link DatagramIO}. 
 * Currently, this component simply dispatches to the appropriate test
 * classes in the {@link one.world.io} package.
 * <p>
 * Three tests can be performed using this component.  All 
 * of the tests allow specification of the number of tests performed, number 
 * of tuples sent per test and the number of bytes per tuple.  The
 * average and standard deviation of the test results are printed once the 
 * test has completed.  The tests can be run with the following command-line
 * arguments.</p>
 *
 * <dl>
 * <dt><code>tl hostname port [numTests numTuples numBytes]</code></dt>
 * <dd>Tuple Latency. Performs a latency test by timing the total round trip 
 * when sending to the echo server on the host and port specified.  This 
 * functionality is implemented by the 
 * {@link one.world.io.BenchmarkDatagramIOLatency} class.</dd>
 * </dl>
 * 
 * <dl>
 * <dt><code>tts hostname port [numTests numTuples numBytes]</code></dt>
 * <dd>Tuple Throughput as Sender. Measures throughput by sending to a receiver
 * at the specified host and port. This functionality is implemented by the 
 * {@link one.world.io.BenchmarkDatagramIOSender} class.</dd>
 * </dl>
 * 
 * <dl>
 * <dt><code>ttr [numTests numTuples numBytes]</code></dt>
 * <dd>Tuple Throughput as Receiver. Measures throughput by receiving tuple
 * from a sender started on another machine.
 * This functionality is implemented by the 
 * {@link one.world.io.BenchmarkDatagramIOReceiver} class.</dd>
 * </dl>
 *
 * Note: because arguments are dispatched to class which can be run 
 * independently, error messages may sometimes include the aforementioned class
 * names instead of simply <code>one.world.io.BenchmarkDatagramIO</code>.
 *
 *
 * <p><b>Imported and Exported Event Handlers</b></p>
 *
 * <p>Exported event handlers:<dl>
 *    <dt>main</dt>
 *    <dd>Handles environment events.
 *        </dd>
 * </dl></p>
 *
 * <p>Imported event handlers:<dl>
 *    <dt>request</dt>
 *    <dd>The imported request handler.
 *        </dd>
 * </dl></p>
 *
 * @version  $Revision 1.2 $
 * @author   Adam MacBeth
 */
public final class BenchmarkDatagramIO extends Component {

  // =======================================================================
  //                           Descriptors
  // =======================================================================

  /** The component descriptor. */
  private static final ComponentDescriptor SELF =
    new ComponentDescriptor("one.world.io.BenchmarkDatagramIO",
                            "A component for benchmarking the "
			    + "one.world.io.DatagramIO component.",
                            true);


  // =======================================================================
  //                           Constructor
  // =======================================================================

  /**
   * Create a new instance of <code>BenchmarkDatagramIO</code>.
   *
   * @param  env  The environment for the new instance.
   * @param  remoteHost  The remote host which is running the udp echo server.
   */
  public BenchmarkDatagramIO(Environment env, String testType)
    throws IOException {
    super(env);

  }


  // =======================================================================
  //                           Component support
  // =======================================================================

  /** Get the component descriptor. */
  public ComponentDescriptor getDescriptor() {
    return (ComponentDescriptor)SELF.clone();
  }


  // =======================================================================
  //                           Initializer
  // =======================================================================

  public static void init(Environment env, Object closure) throws Exception {
    String[] args = (String[])closure;
      
    if(args.length < 1) {
      throw new 
	IllegalArgumentException("Usage: BenchmarkDatagramIO tl/tts/ttr " 
				 + "[hostname port]"
				 + "[numTests numTuples numBytes]");
    }

    String[] myArgs = new String[args.length - 1];
    System.arraycopy((Object)args,1,(Object)myArgs,0,args.length-1);
    if(args.length >= 1) {
      if("tl".equals(args[0])) { // Tuple Latency test
	BenchmarkDatagramIOLatency.init(env,myArgs);
      }
      else if("tts".equals(args[0])) { // Tuple Throughput as Sender
	BenchmarkDatagramIOSender.init(env,myArgs);
      }
      else if("ttr".equals(args[0])) { // Tuple Throughput as Receiver
	BenchmarkDatagramIOReceiver.init(env,myArgs);
      }
    }
    
  }

}





