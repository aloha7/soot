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
import one.util.BufferOutputStream;

/** 
 * Benchmarks for Tuple and byte IO over UDP in plain Java.  The
 * results of this test are meant to be compared to the benchmark
 * results for the {@link one.world.io.NetworkIO} component in {@link
 * one.world.io.BenchmarkNetworkIO}.  <p> This benchmark sends a
 * number of tuples to a UDP echo server and records the time it takes
 * to receive the echo.  The resulting times are then written to
 * standard output.  Since this is not a one.world application, it can
 * be run directly from the command line with the command:<br>
 * <code>java one.world.fonda.BenchmarkUdp hostname port</code><br>
 * where hostname:port specifies the location of a running echo
 * server.</p>
 *
 * The first command line argument is required and specifies the type of test
 * as follows:<br>
 * tl - Tuple Latency.<br>
 * bl - Bytes Latency.<br>
 * tts - Tuple Throughput as Sender.<br>
 * ttr - Tuple Throughput as Receiver.<br>
 * bts - Bytes Throughput as Sender.<br>
 * btr - Bytes Throughput as Receiver.<br>
 * 
 * <p>Only latency and sender functions require hostname and port
 * arguments.  To run the latency test for tuples, pass the "tl"
 * argument with the host and port of a running echo server. To run
 * the throughput tests for tuples, run this class on one machine with
 * the "ttr" argument. On another machine run this class with the
 * "tts" argument as well the as the hostname and port of the first
 * machine (default port is 5101).  Note that the sender and receiver
 * must use the same form of data (tuples or bytes).</p>
 * 
 * @version  $Revision: 1.6 $
 * @author Adam MacBeth 
 */
public class BenchmarkUdp {
 
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
	System.out.println("Usage: BenchmarkUdp tl " 
			   + "remote_host port "
			   + "[numTests numTuples numBytes]");
	System.exit(-1);
      }
      System.out.println("BenchmarkUdp test: Tuples Latency test");
      System.out.println("Testing with " 
			    + numTests + " tests, " 
			    + numTuples + " tuples per test, "
			    + "and " + numBytes + " bytes per tuple.");

      InetAddress address = InetAddress.getByName(args[1]);
      int port = Integer.parseInt(args[2]);

      Stats stats = new Stats();
      DatagramSocket socket = new DatagramSocket(Constants.PORT);      
      BufferOutputStream outBuffer;
      ObjectOutputStream out;
      DatagramPacket sendPacket, receivePacket;
      DynamicTuple t = new DynamicTuple();
      ObjectInputStream in;
      
      t.set("data",new byte[numBytes]);
       
      int psize = 65000;

      for(int j = 0; j<numTests; j++) {
	long startTime = System.currentTimeMillis();
	for(int i = 0; i<numTuples; i++) {
	  outBuffer = new BufferOutputStream();
	  out = new ObjectOutputStream(outBuffer);

	  // Serialize the data
	  out.writeObject(t);
	  out.flush();

	  // Construct the packet
	  sendPacket = new DatagramPacket(outBuffer.getBytes(), 
					  outBuffer.size(),
					  address, 
					  port);	

	  // send the packet
	  socket.send(sendPacket);

	  // receive a new packet
	  receivePacket = new DatagramPacket(new byte[psize], psize);
	  socket.receive(receivePacket);

	  // read tuple from packet
	  in = new ObjectInputStream(new ByteArrayInputStream(receivePacket.getData()));
	  Tuple tuple = (Tuple)in.readObject();
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
	System.out.println("Usage: BenchmarkUdp bl " 
			   + "remote_host port "
			   + "[numTests numTuples numBytes]");
	System.exit(-1);
      }

      System.out.println("BenchmarkUdp test: Bytes Latency test");
      System.out.println("Testing with " 
			    + numTests + " tests, " 
			    + numTuples + " tuples per test, "
			    + "and " + numBytes + " bytes per tuple.");

      Stats stats = new Stats();
      InetAddress address = InetAddress.getByName(args[1]);
      int port = Integer.parseInt(args[2]);
      
      DatagramSocket socket = new DatagramSocket(Constants.PORT);
      byte[] outBuffer = new byte[numBytes];
      DatagramPacket sendPacket;
      
      for(int j = 0; j<numTests; j++) {
	long startTime = System.currentTimeMillis();
	for(int i = 0; i<numTuples; i++) {
	  // Construct the packet
	  DatagramPacket packet = new DatagramPacket(outBuffer, 
						     outBuffer.length,
						     address, 
						     port);
	  socket.send(packet);
	  socket.receive(new DatagramPacket(new byte[numBytes], numBytes));
	}
	long diff = System.currentTimeMillis() - startTime; 
	System.out.println((new Long(diff)).toString());
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
	System.out.println("Usage: BenchmarkUdp tts " 
			   + "remote_host port "
			   + "[numTests numTuples numBytes]");
	System.exit(-1);
      }

      System.out.println("BenchmarkUdp test: Tuples Throughput test as Sender");
      System.out.println("Testing with " 
			    + numTests + " tests, " 
			    + numTuples + " tuples per test, "
			    + "and " + numBytes + " bytes per tuple.");
      
      InetAddress address = InetAddress.getByName(args[1]);
      int port = Integer.parseInt(args[2]);
      DatagramSocket socket = new DatagramSocket(Constants.PORT); 
      DynamicTuple t = new DynamicTuple();
      t.set("data",new byte[numBytes]);
      
      while(true) { // send forever
	BufferOutputStream outBuffer = new BufferOutputStream();
	ObjectOutputStream out = new ObjectOutputStream(outBuffer);

	// Serialize the data
	out.writeObject(t);
	out.flush();
	
	// Construct the packet
	DatagramPacket sendPacket = new DatagramPacket(outBuffer.getBytes(), 
						       outBuffer.size(),
						       address, 
						       port);	
	// send the packet
	socket.send(sendPacket);
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
	System.out.println("Usage: BenchmarkUdp ttr " 
			   + "[numTests numTuples numBytes]");
	System.exit(-1);
      }
      
      System.out.println("BenchmarkUdp test: Tuples Throughput test as Receiver");
      System.out.println("Testing with " 
			 + numTests + " tests, " 
			 + numTuples + " tuples per test, "
			 + "and " + numBytes + " bytes per tuple.");
      Stats stats = new Stats();
      DatagramSocket sock = new DatagramSocket(Constants.PORT);
 
      byte[] buffer = new byte[65000];
      for(int j = 0; j < numTests; j++) {
	long startTime = 0;
	for(int i = 0; i < numTuples; i++) {
	  DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
	  sock.receive(packet);
	  if (0 == i) {
	    startTime = System.currentTimeMillis();
	  }
	  ObjectInputStream in = 
	    new ObjectInputStream(new ByteArrayInputStream(packet.getData()));
	  Tuple tuple = (Tuple)in.readObject();
	  
	}
	long diff = System.currentTimeMillis() - startTime;
	System.out.println(diff);

	//Don't add the first two results, b/c it takes time to get up to speed
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
	System.out.println("Usage: BenchmarkUdp bts " 
			   + "remote_host port "
			   + "[numTests numTuples numBytes]");
	System.exit(-1);
      }

      System.out.println("BenchmarkUdp test: Bytes Throughput test as Sender");
      System.out.println("Testing with " 
			    + numTests + " tests, " 
			    + numTuples + " tuples per test, "
			    + "and " + numBytes + " bytes per tuple.");

      Stats stats = new Stats();
      InetAddress address = InetAddress.getByName(args[1]);
      int port = Integer.parseInt(args[2]);
      DatagramSocket socket = new DatagramSocket(Constants.PORT);
      
      byte[] outBuffer = new byte[numBytes];
      DatagramPacket sendPacket;
      
      while(true) {
	// Construct the packet
	DatagramPacket packet = new DatagramPacket(outBuffer, 
						   outBuffer.length,
						   address, 
						   port);
	socket.send(packet);
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
	System.out.println("Usage: BenchmarkUdp btr " 
			   + "[numTests numTuples numBytes]");
	System.exit(-1);
      }

      System.out.println("BenchmarkUdp test: Bytes Throughput test as Receiver");
      System.out.println("Testing with " 
			    + numTests + " tests, " 
			    + numTuples + " tuples per test, "
			    + "and " + numBytes + " bytes per tuple.");
      Stats stats = new Stats();
      DatagramSocket socket = new DatagramSocket(Constants.PORT);
      DatagramPacket sendPacket;

      for(int i = 0; i < numTests; i++) {
	long startTime = System.currentTimeMillis();
	for(int j = 0; j < numTuples; j++) {
	  socket.receive(new DatagramPacket(new byte[numBytes], numBytes));
	}
	long diff = System.currentTimeMillis() - startTime; 
	System.out.println((new Long(diff)).toString());
	
	//Don't add the first two results, b/c it takes time to get up to speed
	if (i > 2) {
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
      System.out.println("Usage: BenchmarkUdp tl/bl/tts/ttr " 
			 + "[remote_host port] "
			 + "[numTests numTuples numBytes]");
      System.exit(-1);
    }
    
  }
}

