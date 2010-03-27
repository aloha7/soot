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

package one.fonda.java;

import java.rmi.Naming;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.RMISecurityManager;
import java.rmi.server.UnicastRemoteObject;

import one.world.core.Event;
import one.world.core.EventHandler;

import one.world.rep.NamedResource;
import one.world.rep.RemoteEvent;
import one.world.rep.RemoteReference;

import one.world.util.NullHandler;

import one.util.Guid;
import one.util.Stats;

/** 
 * Benchmarks for call invocation with Java RMI.
 * The results of this test are meant to be compared 
 * to the benchmark results for remote event passing in
 * {@link one.world.rep.BenchmarkREP}.  
 *
 * <p>Four tests are performed with this class.
 * <ol>
 *   <li>The call latency test measures the latency when making calls to a
 *   no-argument, void function of a remote object.</li>
 *   <li>The call throughput test measures the throughput when making 
 *   calls to a no-argument, void function of a remote object.</li>
 *   <li>The event latency test measures the latency when making calls to a
 *   remote object method that accepts and returns a {@link
 *   one.world.rep.RemoteEvent}.</li>
 *   <li>The event throughput test measures the throughput when making 
 *   calls to a remote object method that accepts and returns a 
 *   <code>RemoteEvent</code>.</li>
 * </ol>
 * </p>
 *
 * <p>Each test requires a client and a server running on two separate
 * machines.
 * The call latency client makes a number of calls to a remote object and
 * computes latency statistics.  The call latency server provides the
 * remote object for this test.  The call throughput server is a remote
 * object which counts the number of times it is called and computes
 * throughput statistics.  The call throughput client makes those calls.
 * The event latency and throughput tests are similar.
 * When a test completes, the average and standard deviation of the results 
 * are printed to standard output.</p>
 *
 * <p>Each server or client may be run from the command line as follows.
 * <dl>
 *   <dt><code>java
 *   -Djava.security.policy=${JAVA_DEV_ROOT}/src/one/fonda/java/rmi.policy
 *   one.fonda.java.BenchmarkRmi cls objectName</code></dt>
 *   
 *   <dd><p>Runs the Call Latency Server, binding to the specified name in a
 *   registry running on the local host.  The server exports a method
 *   which is a no-op.</p></dd>
 *
 *   <dt><code>java one.fonda.java.BenchmarkRmi 
 *   clc remoteHost objectName [numTests numCalls]</code></dt>
 *
 *   <dd><p>Runs the Call Latency Client, locating a remote object with the
 *   given name in the registry on the given remote host.  The default
 *   number of tests is 100, with 1000 calls per test.  The calls are
 *   made sequentially; the time reported is that required for
 *   <code>numCalls</code> calls to be completed.</p></dd>
 *
 *   <dt><code>java
 *   -Djava.security.policy=${JAVA_DEV_ROOT}/src/one/fonda/java/rmi.policy
 *   one.fonda.java.BenchmarkRmi cts objectName [numTests numCalls]</code></dt>
 *
 *   <dd><p>Runs the Call Throughput Server, binding to the specified name in
 *   a registry running on the local host.  the default number of tests
 *   is 100, with 1000 calls per test.  Each time the server's exported
 *   method is invoked, it counts that as a method call.  The time
 *   reported is that required for <code>numCalls</code> calls to be
 *   received.</p></dd>
 *
 *   <dt><code>java one.fonda.java.BenchmarkRmi 
 *   ctc remoteHost objectName [numThreads]</code></dt>
 *   
 *   <dd><p>Runs the Call Throughtput Client, locating a remote object with
 *   the given name in the registry on the given remote host.  The number
 *   of threads from which to make remote method invokations is given by
 *   numThreads; the default is 1.  Because RMI method calls are
 *   synchronous, multiple threads may be required to stress the server.
 *   The number of threads required should be tuned by the person
 *   performing the benchmark.</p></dd>
 *
 *   <dt><code>java
 *   -Djava.security.policy=${JAVA_DEV_ROOT}/src/one/fonda/java/rmi.policy
 *   one.fonda.java.BenchmarkRmi els objectName</code></dt>
 *   
 *   <dd><p>Runs the Event Latency Server, binding to the specified name in a
 *   registry running on the local host.  The server exports a method
 *   which accepts and returns a <code>RemoteEvent</code>.</p></dt>
 *
 *   <dt><code>java one.fonda.java.BenchmarkRmi 
 *   elc remoteHost objectName [numTests numCalls]</code></dt>
 *
 *   <dd><p>Runs the Event Latency Client, locating a remote object with the
 *   given name in the registry on the given remote host.  The default
 *   number of tests is 100, with 1000 calls per test.  The calls are
 *   made sequentially; the time reported is that required for
 *   <code>numCalls</code> calls to be completed.</p></dd>
 *
 *   <dt><code>java
 *   -Djava.security.policy=${JAVA_DEV_ROOT}/src/one/fonda/java/rmi.policy
 *   one.fonda.java.BenchmarkRmi ets objectName [numTests numCalls]</code></dt>
 *
 *   <dd><p>Runs the Event Throughput Server, binding to the specified 
 *   name in
 *   a registry running on the local host.  the default number of tests
 *   is 100, with 1000 calls per test.  Each time the server's exported
 *   method is invoked, it counts that as a method call.  The time
 *   reported is that required for <code>numCalls</code> calls to be
 *   received.</p></dd>
 *
 *   <dt><code>java one.fonda.java.BenchmarkRmi 
 *   etc remoteHost objectName [numThreads]</code></dt>
 *   
 *   <dd><p>Runs the Event Throughtput Client, locating a remote object with
 *   the given name in the registry on the given remote host.  The number
 *   of threads from which to make remote method invokations is given by
 *   numThreads; the default is 1.  Because RMI method calls are
 *   synchronous, multiple threads may be required to stress the server.
 *   The number of threads required should be tuned by the person
 *   performing the benchmark.</p></dd>
 * </dl>
 * </p>
 *
 * @version  $Revision: 1.4 $
 * @author Janet Davis
 */
public class BenchmarkRmi {
 
  /** Runs the test. */
  public static void main(String args[]) throws Exception {
    
    /** The number of tests to perform. */
    int numTests = 100;

    /** The number of calls to be made in each test. */
    int numCalls = 1000;

    // Call Latency Client
    if ((args.length > 0)&&("clc".equals(args[0]))) {
   
      if (5 == args.length) {
	numTests = Integer.parseInt(args[3]);
	numCalls = Integer.parseInt(args[4]);
      } else if (3 != args.length) {
	System.out.println("Usage: BenchmarkRmi clc " 
			   + "remoteHost objectName "
			   + "[numTests numCalls]");
	System.exit(-1);
      }
      System.out.println("Running test: Call Latency Client");
      System.out.println("Testing with " 
			    + numTests + " tests and " 
			    + numCalls + " calls per test.");

      Stats stats = new Stats();
      CallServer server = null;
      server = (CallServer) Naming.lookup("//" + args[1] + "/" + args[2]);

      // To remove cost of class loading, etc. from results.
      server.call();

      for(int j = 0; j < numTests; j++) {
	long startTime = System.currentTimeMillis();
	for (int i = 0; i < numCalls; i++) {
          server.call();
	}
	long diff = System.currentTimeMillis() - startTime; 
	System.out.println((new Long(diff)).toString());
	stats.add(diff);
      }
      System.out.println("Time to perform " + numCalls
                         + " remote method invocations"
			 + " (averaged over " + numTests + " tests):");
      System.out.println("Average = " + stats.average() + " ms");
      System.out.println("Standard deviation = " + stats.stdev() + " ms"); 

    // Call Latency Server
    } else if ((args.length > 0) && ("cls".equals(args[0]))) {
      if (2 != args.length) {
	System.out.println("Usage: BenchmarkRmi cls " 
			   + "name");
	System.exit(-1);
      }

      if (System.getSecurityManager() == null) {
        System.setSecurityManager(new RMISecurityManager());
      }
      Naming.rebind(args[1], new CallLatencyServer());
      System.out.println("Running test: Call Latency Server");
   
    // Call Throughput Client
    } else if ((args.length > 0) && ("ctc".equals(args[0]))) {
      int numThreads = 1;

      if (4 == args.length) {
        try {
          numThreads = Integer.parseInt(args[3]);
	} catch (NumberFormatException x) {
	  System.out.println("Usage: BenchmarkRmi ctc " 
		  	     + "remoteHost objectName [numThreads]");
	  System.exit(-1);
	}
      } else if (3 != args.length) {
	System.out.println("Usage: BenchmarkRmi ctc " 
			   + "remoteHost objectName [numThreads]");
	System.exit(-1);
      }
      System.out.println("Running test: Call Throughput Client");
      System.out.println("Testing with " + numThreads + " threads");

      CallServer server = null;
      server = (CallServer) Naming.lookup("//" + args[1] + "/" + args[2]);
      
      for (int i = 0; i < numThreads; i++) {
        Thread t = new Thread(new CallThroughputClient(server), 
	                      new Integer(i).toString());
        t.start();
      }

    // Call Throughput Server
    } else if ((args.length > 0) && ("cts".equals(args[0]))) {
   
      if(4 == args.length) {
	numTests = Integer.parseInt(args[2]);
	numCalls = Integer.parseInt(args[3]);
      } else if (2 != args.length) {
	System.out.println("Usage: BenchmarkRmi cts objectName" 
			   + "[numTests numCalls]");
	System.exit(-1);
      }

      if (System.getSecurityManager() == null) {
        System.setSecurityManager(new RMISecurityManager());
      }
      Naming.rebind(args[1], new CallThroughputServer());
      System.out.println("Running test: Call Throughput Server");
      System.out.println("Testing with " 
			    + numTests + " tests and " 
			    + numCalls + " calls per test.");

    // Event Latency Client
    } else if ((args.length > 0)&&("elc".equals(args[0]))) {
   
      if (5 == args.length) {
	numTests = Integer.parseInt(args[3]);
	numCalls = Integer.parseInt(args[4]);
      } else if (3 != args.length) {
	System.out.println("Usage: BenchmarkRmi elc " 
			   + "remoteHost objectName "
			   + "[numTests numCalls]");
	System.exit(-1);
      }
      System.out.println("Running test: Event Latency Client");
      System.out.println("Testing with " 
			    + numTests + " tests and " 
			    + numCalls + " calls per test.");

      Stats stats = new Stats();
      EventServer server = null;
      server = (EventServer) Naming.lookup("//" + args[1] + "/" + args[2]);

      RemoteReference ref = new RemoteReference("", 0, new Guid());
      RemoteEvent re =
	    new RemoteEvent(NullHandler.NULL, null, 
	                    new NamedResource("", 0, ""),
			    new EmptyEvent(ref, null));

      // To remove cost of class loading, etc. from results.
      server.call(re);

      for(int j = 0; j < numTests; j++) {
	long startTime = System.currentTimeMillis();
	for (int i = 0; i < numCalls; i++) {
          server.call(re);
	}
	long diff = System.currentTimeMillis() - startTime; 
	System.out.println((new Long(diff)).toString());
	stats.add(diff);
      }
      System.out.println("Time to perform " + numCalls
                         + " remote method invocations"
			 + " (averaged over " + numTests + " tests):");
      System.out.println("Average = " + stats.average() + " ms");
      System.out.println("Standard deviation = " + stats.stdev() + " ms"); 

    // Event Latency Server
    } else if ((args.length > 0) && ("els".equals(args[0]))) {
      if (2 != args.length) {
	System.out.println("Usage: BenchmarkRmi els name");
	System.exit(-1);
      }

      // Seems to fix problems with the class loader.
      new Guid();

      if (System.getSecurityManager() == null) {
        System.setSecurityManager(new RMISecurityManager());
      }
      Naming.rebind(args[1], new EventLatencyServer());
      System.out.println("Running test: Event Latency Server");
   
    // Event Throughput Client
    } else if ((args.length > 0) && ("etc".equals(args[0]))) {
      int numThreads = 1;

      if (4 == args.length) {
        try {
          numThreads = Integer.parseInt(args[3]);
	} catch (NumberFormatException x) {
	  System.out.println("Usage: BenchmarkRmi etc " 
		  	     + "remoteHost objectName [numThreads]");
	  System.exit(-1);
	}
      } else if (3 != args.length) {
	System.out.println("Usage: BenchmarkRmi etc " 
			   + "remoteHost objectName [numThreads]");
	System.exit(-1);
      }
      System.out.println("Running test: Event Throughput Client");
      System.out.println("Testing with " + numThreads + " threads");

      EventServer server = null;
      server = (EventServer) Naming.lookup("//" + args[1] + "/" + args[2]);
      
      for (int i = 0; i < numThreads; i++) {
        Thread t = new Thread(new EventThroughputClient(server), 
	                      new Integer(i).toString());
        t.start();
      }

    // Event Throughput Server
    } else if ((args.length > 0) && ("ets".equals(args[0]))) {
   
      if(4 == args.length) {
	numTests = Integer.parseInt(args[2]);
	numCalls = Integer.parseInt(args[3]);
      } else if (2 != args.length) {
	System.out.println("Usage: BenchmarkRmi ets objectName" 
			   + "[numTests numCalls]");
	System.exit(-1);
      }

      // This seems to make the class loader work.
      new Guid();

      if (System.getSecurityManager() == null) {
        System.setSecurityManager(new RMISecurityManager());
      }
      Naming.rebind(args[1], new EventThroughputServer());
      System.out.println("Running test: Event Throughput Server");
      System.out.println("Testing with " 
			    + numTests + " tests and " 
			    + numCalls + " calls per test.");
    } else {
      System.out.println("Usage:BenchmarkRmi clc/cls/ctc/cts"
                                         + "/elc/ets/etc/ets");
      System.exit(-1);
    }
  }

  /** The interface for the remote object used in call tests. */
  public static interface CallServer extends Remote {
    /** The call. */
    void call() throws RemoteException;
  }

  /** 
   * The implementation for the remote object used in call latency tests. 
   */
  public static class CallLatencyServer extends UnicastRemoteObject
          implements CallServer {

    /** Constructs a new latency server. */
    public CallLatencyServer() throws RemoteException {
      super();
    }

    /** Does nothing. */
    public void call() throws RemoteException {
    }
  }

  /**
   * The runnable objects used for the multiple threads in throughput
   * tests.  Each simply makes calls to the server in a tight loop.
   */
  private static class CallThroughputClient implements Runnable {

    /** The server to make method calls on. */
    private CallServer server;

    /** 
     * Constructs a new call throughput client with the given server. 
     *
     * @param server  The server to make method calls on.
     */
    public CallThroughputClient(CallServer server) {
      this.server = server;
    }

    /** Makes calls to the server until an exception occurs. */
    public void run() {
      try {
        while (true) {
          server.call();
	}
      } catch (RemoteException x) {
        return;
      }
    }
    
  }

  /** 
   * The implementation for the remote object used in call throughput 
   * tests.
   */
  public static class CallThroughputServer extends UnicastRemoteObject 
          implements CallServer {
    
    /** The number of tests to perform. */
    int numTests;

    /** The number of calls in each test. */
    int numCalls;

    /** The number of tests performed so far. */
    int testCount;

    /** The number of calls performed in this test. */
    int callCount;

    /** The starting time for each test. */
    long startTime;

    /** The stats computer. */
    Stats stats;

    /** 
     * Constructs a call throughput server.
     *
     * @param numTests  The number of tests to perform.
     * @param numCalls  The number of calls in each test.
     */
    public CallThroughputServer(int numTests, int numCalls) 
            throws RemoteException {
      this.numTests = numTests;
      this.numCalls = numCalls;
      this.testCount = 0;
      this.callCount = -1;
      this.stats = new Stats();
    }

    /** 
     * Constructs a throughput server with 100 tests and 1000 calls
     * per test.
     */
    public CallThroughputServer() throws RemoteException {
      this(100, 1000);
    }

    /** 
     * Counts the number of calls and computes statistics.
     */
    public void call() throws RemoteException {
      callCount++;
      if (callCount == 0) {
        startTime = System.currentTimeMillis();
      } else if (callCount == numCalls) {
        long diff = System.currentTimeMillis() - startTime;
	System.out.println(diff);
	stats.add(diff);
        testCount++;
	if (testCount == numTests) {
          System.out.println("Time to receive " + numCalls
                             + " remote method invocations"
		   	     + " (averaged over " + numTests + " tests):");
          System.out.println("Average = " + stats.average() + " ms");
          System.out.println("Standard deviation = " + stats.stdev() 
	                     + " ms");   
	  System.exit(0);
	}
	callCount = 0;
        startTime = System.currentTimeMillis();
      }
    }
  }

  /** The interface for the remote object for the event tests. */
  public static interface EventServer extends Remote {
    /** The call. */
    RemoteEvent call(RemoteEvent re) throws RemoteException;
  }

  /** The implementation for the remote object used in latency tests. */
  public static class EventLatencyServer extends UnicastRemoteObject
          implements EventServer {

    /** Constructs a new latency server. */
    public EventLatencyServer() throws RemoteException {
      super();
    }

    /** Echos the event. */
    public RemoteEvent call(RemoteEvent re) throws RemoteException {
      return re;
    }
  }

  /**
   * The runnable objects used for the multiple threads in event 
   * throughput tests.  Each simply makes calls to the server in a 
   * tight loop.
   */
  private static class EventThroughputClient implements Runnable {

    /** The server to make method calls on. */
    private EventServer server;

    /** 
     * Constructs a new throughput client with the given server. 
     *
     * @param server  The server to make method calls on.
     */
    public EventThroughputClient(EventServer server) {
      this.server = server;
    }

    /** Makes calls to the server until an exception occurs. */
    public void run() {
      try {
        RemoteEvent re =
	    new RemoteEvent(NullHandler.NULL, null, 
	                    new NamedResource("", 0, ""),
			    new EmptyEvent(
			        new RemoteReference("", 0, new Guid()),
				null));
        while (true) {
          server.call(re);
	}
      } catch (RemoteException x) {
        return;
      }
    }
    
  }

  /** 
   * The implementation for the remote object used in throughput tests.
   */
  public static class EventThroughputServer extends UnicastRemoteObject 
          implements EventServer {
    
    /** The number of tests to perform. */
    int numTests;

    /** The number of calls in each test. */
    int numCalls;

    /** The number of tests performed so far. */
    int testCount;

    /** The number of calls performed in this test. */
    int callCount;

    /** The starting time for each test. */
    long startTime;

    /** The stats computer. */
    Stats stats;

    /** 
     * Constructs an event throughput server.
     *
     * @param numTests  The number of tests to perform.
     * @param numCalls  The number of calls in each test.
     */
    public EventThroughputServer(int numTests, int numCalls) 
            throws RemoteException {
      this.numTests = numTests;
      this.numCalls = numCalls;
      this.testCount = 0;
      this.callCount = -1;
      this.stats = new Stats();
    }

    /** 
     * Constructs an event throughput server with 100 tests and 1000 
     * calls per test.
     */
    public EventThroughputServer() throws RemoteException {
      this(100, 1000);
    }

    /** 
     * Counts the number of calls and computes statistics.
     */
    public RemoteEvent call(RemoteEvent re) throws RemoteException {
      callCount++;
      if (callCount == 0) {
        startTime = System.currentTimeMillis();
      } else if (callCount == numCalls) {
        long diff = System.currentTimeMillis() - startTime;
	System.out.println(diff);
	stats.add(diff);
        testCount++;
	if (testCount == numTests) {
          System.out.println("Time to receive " + numCalls
                             + " remote method invocations"
		   	     + " (averaged over " + numTests + " tests):");
          System.out.println("Average = " + stats.average() + " ms");
          System.out.println("Standard deviation = " + stats.stdev() 
	                     + " ms");   
	  System.exit(0);
	}
	callCount = 0;
        startTime = System.currentTimeMillis();
      }
      return null;
    }
  }

  /**
   * An empty event is an event with no data.  It is used in the benchmarks
   * to assure that the event we are sending is absolutely minimal.
   * 
   * @version  $Revision 1.2 $
   * @author   Janet Davis
   */
  static final class EmptyEvent extends Event {

    /** Constructs a new empty event. */
      public EmptyEvent() {
          super();
      }

    /** 
     * Constructs a new empty event.
     *
     * @param source  The source of the event.
     * @param closure The closure for the event.
     */
    public EmptyEvent(EventHandler source, Object closure) {
      super(source, closure);
    }
  }
}

