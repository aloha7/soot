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

import java.io.*;
import java.net.*;

import one.world.core.DynamicTuple;
import one.world.core.Tuple;
import one.world.Constants;
import one.util.Stats;

/** 
 * Benchmarks for Tuple and byte IO over TCP in plain Java. 
 * The results of this test are meant to be compared 
 * to the benchmark results for the {@link one.world.io.NetworkIO} component in
 * {@link one.world.io.BenchmarkNetworkIO}.  
 * <p>
 * A total of six functions can be performed with this class.
 * Tests use either bytes or tuples to test the latency or throughput of the
 * system.  The latency test sends a number of tuples to an echo server and 
 * times the round-trip, waiting to receive the echoed tuple before sending
 * the next.  The throughput test sends a number of tuples as fast as possible
 * to a receiver which times the receipt of a number of tuples.
 * If bytes are used instead of tuples, the number of bytes sent is equal
 * to the number of bytes in the Tuple payload.
 * When a test completes, the average and standard deviation of the results are
 * printed to standard output.
 * <p>
 * Since this is not a one.world application, it can be run directly from the
 * command line with the command:<br>
 * <code>java one.world.fonda.BenchmarkTcp tl/bl/tts/ttr/bts/btr 
 * [hostname port] [numTests numTuples numBytes]</code><br>
 * The number of tests, number of tuples per test, and number of bytes per
 * tuple can be controlled using the <code>numTests</code>, 
 * <code>numTests</code>, and
 * <code>numBytes</code> arguments respectively.
 *
 * <p>
 * The first command line argument is required and specifies the type of test
 * as follows:<br>
 * tl - Tuple Latency.<br>
 * bl - Bytes Latency.<br>
 * tts - Tuple Throughput as Sender.<br>
 * ttr - Tuple Throughput as Receiver.<br>
 * bts - Bytes Throughput as Sender.<br>
 * btr - Bytes Throughput as Receiver.<br>
 * 
 * <p>Only latency and sender functions require hostname and port arguments.  
 * To run the latency test for tuples, pass the "tl" argument with the host
 * and port of a running echo server. To run the throughput tests for tuples, 
 * run this
 * class on one machine with the "ttr" argument. On another machine run this
 * class with the "tts" argument as well the as the hostname and port of the
 * first machine (default port is 5101).  Note that the sender and receiver 
 * must use the same form of data (tuples or bytes). 
 * 
 *
 * @version  $Revision: 1.6 $
 * @author Adam MacBeth */
public class BenchmarkTcp {
 
  /** Runs the test. */
  public static void main(String args[]) throws Exception {

    /** The number of tuples to be sent in each test. */
    int numTuples = 1000;
    
    /** The number of tests to perform. */
    int numTests = 100;
    
    /** The number of bytes to be sent in each tuple. */  
    int numBytes = 100;
 
    // Tuples Latency test
    if("tl".equals(args[0])) {
   
      if(6 == args.length) {
	numTests = Integer.parseInt(args[3]);
	numTuples = Integer.parseInt(args[4]);
	numBytes = Integer.parseInt(args[5]);
      }
      else if (3 != args.length) {
	System.out.println("Usage: BenchmarkTcp tl " 
			   + "remote_host port "
			   + "[numTests numTuples numBytes]");
	System.exit(-1);
      }
      System.out.println("BenchmarkTcp test: Tuples Latency test");
      System.out.println("Testing with " 
			    + numTests + " tests, " 
			    + numTuples + " tuples per test, "
			    + "and " + numBytes + " bytes per tuple.");

      Stats stats = new Stats();
      Socket client_socket = new Socket(args[1],Integer.parseInt(args[2]));

      ObjectOutputStream out = 
	new ObjectOutputStream(client_socket.getOutputStream());
      System.out.println("Got output stream");

      ObjectInputStream in = 
	new ObjectInputStream(client_socket.getInputStream());
      System.out.println("Got input stream");

      DynamicTuple t = new DynamicTuple();
      t.set("bytes",new byte[numBytes]);
      
      for(int j = 0; j<numTests; j++) {
	long startTime = System.currentTimeMillis();
	for(int i = 0; i<numTuples; i++) {
	  out.writeObject(t);
	  out.reset();
	  Tuple tt = (Tuple)in.readObject();
	}
	long diff = System.currentTimeMillis() - startTime; 
	System.out.println((new Long(diff)).toString());
	stats.add(diff);
      }
      System.out.println("Average = " + stats.average());
      System.out.println("Standard deviation = " + stats.stdev()); 
    }

    // Bytes Latency test
    else if("bl".equals(args[0])) {
 
      if(6 == args.length) {
	numTests = Integer.parseInt(args[3]);
	numTuples = Integer.parseInt(args[4]);
	numBytes = Integer.parseInt(args[5]);
      }
      else if (3 != args.length) {
	System.out.println("Usage: BenchmarkTcp bl " 
			   + "remote_host port "
			   + "[numTests numTuples numBytes]");
	System.exit(-1);
      }

      System.out.println("BenchmarkTcp test: Bytes Latency test");
      System.out.println("Testing with " 
			    + numTests + " tests, " 
			    + numTuples + " tuples per test, "
			    + "and " + numBytes + " bytes per tuple.");

      Stats stats = new Stats();
      Socket socket = new Socket(args[1],Integer.parseInt(args[2]));
      OutputStream out = socket.getOutputStream();
      InputStream in = socket.getInputStream();
      
      byte[] t = new byte[numBytes];
      byte[] tt = new byte[numBytes];
      
      for(int j = 0; j < numTests; j++) {
	long startTime = System.currentTimeMillis();
	for(int i = 0; i < numTuples; i++) {
	  out.write(t);
	  int num = in.read(tt);
	}
	long diff = System.currentTimeMillis() - startTime; 
	System.out.println(diff);
	stats.add(diff);
      }
      System.out.println("Average = " + stats.average());
      System.out.println("Standard deviation = " + stats.stdev());
    }

    // Tuples Throughput test as Sender
    else if("tts".equals(args[0])) {

      if(6 == args.length) {
	numTests = Integer.parseInt(args[3]);
	numTuples = Integer.parseInt(args[4]);
	numBytes = Integer.parseInt(args[5]);
      }
      else if (3 != args.length) {
	System.out.println("Usage: BenchmarkTcp tts " 
			   + "remote_host port "
			   + "[numTests numTuples numBytes]");
	System.exit(-1);
      }

      System.out.println("BenchmarkTcp test: Tuples Throughput test as Sender");
      System.out.println("Testing with " 
			    + numTests + " tests, " 
			    + numTuples + " tuples per test, "
			    + "and " + numBytes + " bytes per tuple.");

      Socket client_socket = new Socket(args[1],Integer.parseInt(args[2]));
      ObjectOutputStream out = 
	new ObjectOutputStream(
	      new BufferedOutputStream(client_socket.getOutputStream()));
      DynamicTuple t = new DynamicTuple();
      t.set("bytes", new byte[numBytes]);
 
      //send forever
      while(true) {
	out.writeObject(t);
	out.reset();
      }
    }

    // Tuples Throughput test as Receiver
    else if("ttr".equals(args[0])) {
      
      if(4 == args.length) {
	numTests = Integer.parseInt(args[1]);
	numTuples = Integer.parseInt(args[2]);
	numBytes = Integer.parseInt(args[3]);
      }
      else if (1 != args.length) {
	System.out.println("Usage: BenchmarkTcp ttr " 
			   + "[numTests numTuples numBytes]");
	System.exit(-1);
      }
      
      System.out.println("BenchmarkTcp test: Tuples Throughput test as Receiver");
      System.out.println("Testing with " 
			 + numTests + " tests, " 
			 + numTuples + " tuples per test, "
			 + "and " + numBytes + " bytes per tuple.");

      Stats stats = new Stats();
      ServerSocket ss = new ServerSocket(Constants.PORT);
      Socket sock = ss.accept();
      ObjectInputStream in = 
	new ObjectInputStream(new BufferedInputStream(sock.getInputStream()));
      for(int j = 0; j < numTests; j++) {
	long startTime = 0;
	for(int i = 0; i < numTuples; i++) {
	  Tuple t = (Tuple)in.readObject();
	  if (0 == i) {
	    startTime = System.currentTimeMillis();
	  }
	}
	long diff = System.currentTimeMillis() - startTime;
	System.out.println(diff);
	if (j > 2) {
	  stats.add(diff);
	}
      }
      System.out.println("Average = " + stats.average());
      System.out.println("Standard deviation = " + stats.stdev()); 
      System.out.println("Throughput = " 
			 + (numTuples*numBytes) / (stats.average()/1000)
			 + " Bps");
    }

    // Bytes Throughput test as Sender
    else if("bts".equals(args[0])) {
 
      if(6 == args.length) {
	numTests = Integer.parseInt(args[3]);
	numTuples = Integer.parseInt(args[4]);
	numBytes = Integer.parseInt(args[5]);
      }
      else if (3 != args.length) {
	System.out.println("Usage: BenchmarkTcp bts " 
			   + "remote_host port "
			   + "[numTests numTuples numBytes]");
	System.exit(-1);
      }

      System.out.println("BenchmarkTcp test: Bytes Throughput test as Sender");
      System.out.println("Testing with " 
			    + numTests + " tests, " 
			    + numTuples + " tuples per test, "
			    + "and " + numBytes + " bytes per tuple.");

      Socket socket = new Socket(args[1],Integer.parseInt(args[2]));
      OutputStream out = socket.getOutputStream();
      byte[] b = new byte[numBytes];
     
      //send forever
      while(true) {
	out.write(b);
      }    
    }
    
    // Bytes Throughput test as Receiver
    else if("btr".equals(args[0])) {
      
      if(4 == args.length) {
	numTests = Integer.parseInt(args[1]);
	numTuples = Integer.parseInt(args[2]);
	numBytes = Integer.parseInt(args[3]);
      }
      else if (1 != args.length) {
	System.out.println("Usage: BenchmarkTcp btr " 
			   + "[numTests numTuples numBytes]");
	System.exit(-1);
      }

      System.out.println("BenchmarkTcp test: Bytes Throughput test as Receiver");
      System.out.println("Testing with " 
			    + numTests + " tests, " 
			    + numTuples + " tuples per test, "
			    + "and " + numBytes + " bytes per tuple.");

      Stats stats = new Stats();
      ServerSocket ss = new ServerSocket(Constants.PORT);
      Socket sock = ss.accept();
      InputStream in = sock.getInputStream();
      byte[] buf = new byte[numBytes];
      for(int j = 0; j < numTests; j++) {
	long startTime = 0;

	int count = 0;
	while (count < numTuples*numBytes) {
	  if (0 == count) {
	    startTime = System.currentTimeMillis();
	  }
	  count += in.read(buf);
	}
	/*
	for (int i = 0; i < numTuples; i++) {
	  int count = 0;
	  while(count < numBytes) {
	    count += in.read(buf,count,numBytes - count);
	  }
	  if (0 == i) {
	    startTime = System.currentTimeMillis();
	  }
	}
	*/

	long diff = System.currentTimeMillis() - startTime;
	System.out.println(diff);
	if (j > 2) {
	  stats.add(diff);
	}
      }
      System.out.println("Average = " + stats.average());
      System.out.println("Standard deviation = " + stats.stdev()); 
      System.out.println("Throughput = " 
			 + (numTuples*numBytes) / (stats.average()/1000)
			 + " Bps");
    }

    else {
      System.out.println("Usage: BenchmarkTcp tl/bl/tts/ttr " 
			 + "[remote_host port] "
			 + "[numTests numTuples numBytes]");
      System.exit(-1);
    }
    
  }
}

