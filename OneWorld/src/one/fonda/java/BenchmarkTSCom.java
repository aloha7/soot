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
 *
 * @author Eric Lemar
 */



package one.fonda.java;

import java.io.*;
import java.net.*;
import java.util.Random;

import one.util.Stats;


import one.util.Stats;
import  com.ibm.tspaces.Tuple;
import  com.ibm.tspaces.TupleID;
import  com.ibm.tspaces.SuperTuple;
import  com.ibm.tspaces.*;
import  com.ibm.tspaces.server.*;
import com.ibm.tspaces.query.*;


/**
 * Perform tests for TSpaces operations.
 *
 * <p>See {@link one.fonda} for an overview of the tests.</p>
 *
 * <h4>Notes</h4>
 *
 * <p>NOTE: By default this test is not built.  To build, you need to download
 * the <code>tspaces.jar</code> and <code>tspaces_client.jar</code> and place
 * them in your <code>CLASSPATH</code>.  You must also add
 * <code>BenchmarkTSCom.java</code> to the list of source files in
 * <code>src/one/fonda/java/Makefile</code>.</p>
 *
 * <p>This test does not run under <i>one.world</i> and only uses a couple of
 *    peripheral <i>one.world</i> classes.</p>
 *
 * <p>These tests check communication performance.</p>
 *
 * <p>These tests use a TSpaces server started as a thread in the client VM
 *    for maximum performance.  They do NOT use the local communication
 *    mode since this seems to have a bug that caused slowdowns over
 *    time (and didn't seem to improve performance anyway)</p>
 */

public class BenchmarkTSCom {
  /** The tuple data */
  final static int SYNC_CONTINUE = 1;
  final static int SYNC_DONE =2;
  final static int SYNC_CLOSING =3;

  static TSServer localServer;
  static TupleSpace ts;
  static String host;
  static int psize;
  public static int numOperations;
  static int numTests;
  public volatile static int operationCount = 0;
  static volatile long startTime;
  static int outstanding;
  static int numOutstanding;
  static Stats stats = new Stats();
  static Object LOCK = new Object();
  static Object START = new Object();
  static Object WAKE = new Object();
  static Tuple template = new Tuple();
  static Thread threads[];
  static boolean doRemove;
    /** 
     * Collect stats for a test.
     *
     * @param diff The time taken to conduct the test(in milliseconds)
     */
  static final void collectStats(long diff) {
    System.out.println("Operations/Sec:"+ (numOperations/(diff/1000.)));
    if (operationCount>1) {
      stats.add(numOperations/(diff/1000.)); 
    }
  }

  /**
   * Thread to do writes.
   */
  static class WriteThread extends Thread {
    public void run() {
    SuperTuple t=null;
    try {
      t = new Tuple(new byte[psize]);
    } catch (Exception e) {
      exceptionExit(e);
    }
    while(true) {
      try {
        synchronized(START) {
          START.wait();
        }
      } catch (Exception x){}

      boolean again=true;
      while(again) { 
        try {
          ts.write(t);
        } catch (Exception e) {
          exceptionExit(e);
        }
        switch(doSyncEnter()) {
          case SYNC_CLOSING:
            synchronized(WAKE) {
              WAKE.notify(); 
            }

            again=false;
            break;
          case SYNC_DONE:
            again=false;
            break;
        }
      }
      }
    }
    public WriteThread() {};
  }

  /**
   * Thread to do reads
   */
  static class ReadThread extends Thread {
    public void run() {
      while(true) {
      try {
        synchronized(START) {
          START.wait();
        }
      } catch (Exception x){}

      boolean again=true;
      while(again) { 
        try {
          ts.waitToTake(template);
        } catch (Exception e) {
          exceptionExit(e);
        }
        switch(doSyncEnter()) {
          case SYNC_CLOSING:
            synchronized(WAKE) {
              WAKE.notify(); 
            }

            again=false;
            break;
          case SYNC_DONE:
            again=false;
            break;
        }
      }
      }
    }
    public ReadThread() {};
  }


  /**
   * Mutex primitive.
   */
  static private final int doSyncEnter() {
    if (numOutstanding > 1) {
      //Multiple outstanding, must synchronize
      synchronized (LOCK) {
        operationCount++;
        outstanding--;
        
        if ((operationCount+outstanding)==numOperations) {
          if (outstanding == 0) {
            //I'm the last one, so clean up
            return SYNC_CLOSING;
          } else {
            return SYNC_DONE;
          }
        } else {
          outstanding++;
        } 
      }
      
      return SYNC_CONTINUE;
    } else {
      //Only one outstanding, no synchronization
      operationCount++;
      if (operationCount == numOperations) {
        return SYNC_CLOSING;
      } else {
        return SYNC_CONTINUE;
      }
    }
  } 

 /**
  * Callback for TSpaces listens.
  */
  static class MyCallback implements Callback {

    public boolean call(String eventName,String tsName,
                        int seqNum,SuperTuple tuple,boolean isException)   {
      if (! isException) {          
        if (operationCount == 0) {
          startTime = System.currentTimeMillis();
        }
        if (doRemove) {
          try {
            ts.deleteTupleById(tuple.getTupleID());
          } catch (Exception x) {
            exceptionExit(x);
          }
        }

        operationCount++;
        if (operationCount==numOperations) {
          synchronized(finishLock) {
            finishLock.notify();
          }
        }
      } else {
        exceptionExit(null);
      }
      return false;  
    }    
    public MyCallback() {
    } 
  }

  public static Object finishLock = new Object();
 
  /**
   * Load the table if it exists.  Create a new table if it doesn't.
   *
   */
  static void createTable() {
    try {
      ts = new TupleSpace("Example1",host);
    } catch (Exception x) {
      exceptionExit(x);
    }
  }

  /**
   * Delete the given table.
   *
   */
  static void deleteTable() {
    try {
      ts.deleteAll();
    } catch (Exception x) {
      exceptionExit(x);
    }
  }

  /**
   * Do all the takes.
   */
  public static void takeMain(String args[]) {
    int i,j;
    boolean again;
    threads = new Thread[numOutstanding];
    for(int k = 0; k<numOutstanding; k++) {
      threads[k] = new ReadThread();
      threads[k].start();
    }
    for (i = 0; i<numTests; i++) {
      operationCount = 0; 
      outstanding = numOutstanding;

      try {
        Thread.sleep(2000);
      } catch (Exception x) {};

      startTime = System.currentTimeMillis();
        synchronized(WAKE) {
          synchronized(START) {
            START.notifyAll();
          }
          try {
            WAKE.wait();
          } catch (Exception x) {

          }
        }

      long diff = System.currentTimeMillis() - startTime; 
      System.out.println((new Long(diff)).toString());
      collectStats(diff);
      System.out.println(""+operationCount+" "+outstanding+" ");


     }
    System.out.println("Average = " + stats.average());
    System.out.println("Standard deviation = " + stats.stdev());      
 
  }


  /**
   * Do all the puts.
   */
  public static void senderMain(String args[]) {
    int i,j;
    boolean again;
    threads = new Thread[numOutstanding];
    for(int k = 0; k<numOutstanding; k++) {
      threads[k] = new WriteThread();
      threads[k].start();
    }
    for (i = 0; i<numTests; i++) {
      try {
        while(0!=ts.countN(template)) {
          try {
            Thread.sleep(1000);
          } catch (Exception x) {};
        } 
      } catch (Exception e) {
        exceptionExit(e);
      }

      try {
        Thread.sleep(1000);
      } catch (Exception x) {};

      operationCount = 0; 
      outstanding = numOutstanding;

      try {
        Thread.sleep(2000);
      } catch (Exception x) {};

      startTime = System.currentTimeMillis();
        synchronized(WAKE) {
          synchronized(START) {
            START.notifyAll();
          }
          try {
            WAKE.wait();
          } catch (Exception x) {

          }
        }

      long diff = System.currentTimeMillis() - startTime; 
      System.out.println((new Long(diff)).toString());
      collectStats(diff);
      System.out.println(""+operationCount+" "+outstanding+" ");


     }
    System.out.println("Average = " + stats.average());
    System.out.println("Standard deviation = " + stats.stdev());      
 
  }
  /**
   * Do all the listening.
   */
  public static void listenMain(String args[]) {
    int i;

    MyCallback callback = new MyCallback();

    boolean newThread = doRemove;  // default is false
    try {
      int seqNum = ts.eventRegister(TupleSpace.WRITE, template, callback, newThread );
      for (i=0;i<numTests;i++) {
        synchronized(finishLock) {
          finishLock.wait();
        }
        long diff = System.currentTimeMillis() - startTime; 
        System.out.println((new Long(diff)).toString());
        collectStats(diff);
        deleteTable();

        BenchmarkTSCom.operationCount=0;
      }
      System.out.println("Average = " + stats.average());
      System.out.println("Standard deviation = " + stats.stdev());      
    } catch (Exception x) {
      exceptionExit(x);
    }
  }

  /**
   * How do we run it?
   */
  public static void printUsage() {
    System.out.println("BenchmarkTSCom startStore host bts numTests numOperations numOutstanding bytes");
    System.out.println("BenchmarkTSCom startStore host btt numTests numOperations numOutstanding");
    System.out.println("BenchmarkTSCom startStore host btl numTests numOperations doRemove");
  }

  public static void main(String args[]) throws Exception {
    // Tuples Latency test
    if(args.length>0) {
      host = args[1];

      if (args[0].equals("true")) {
        startUp();
        /* Open, delete, open new.  Ensures we have an empty store */
        createTable();
        deleteTable();
      } else {
        createTable();
      }

      numTests = Integer.parseInt(args[3]);     
      numOperations = Integer.parseInt(args[4]);     

      if (args[2].equals("bts")) {
        numOutstanding = Integer.parseInt(args[5]);
        psize = Integer.parseInt(args[6]);
        senderMain(args);
      } else if (args[2].equals("btt")) {
        numOutstanding = Integer.parseInt(args[5]);
        takeMain(args);
      } else if (args[2].equals("btl")) {
        doRemove = args[5].equals("true");
        listenMain(args);
      }
    } else {
      printUsage();
    }
  }

  /**
   * Start the TSpaces server
   */

  private static void startUp() {
   try {
      localServer = new TSServer(8200,null,true,-1,"tspaces.cfg");
      Thread serverThread = new Thread( localServer, "TSServer" );
      serverThread.setDaemon(true);
      serverThread.start();
      //TupleSpace.setTSCmdImpl("com.ibm.tspaces.TSCmdLocalImpl");
    } catch (Exception x) {
      exceptionExit(x);
    }
  }

  /**
   * We caught an exception, quit.
   */
  private static void exceptionExit(Exception x) {
    System.out.println("Exception exit: "+x);
    x.printStackTrace();
    System.exit(-1);
  }
}


