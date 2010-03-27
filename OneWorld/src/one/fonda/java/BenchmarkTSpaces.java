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
 * <code>BenchmarkTSpaces.java</code> to the list of source files in
 * <code>src/one/fonda/java/Makefile</code>.</p>
 *
 * <p>This test does not run under <i>one.world</i> and only uses a couple of
 *    peripheral <i>one.world</i> classes.</p>
 *
 * <p>These tests check raw read/write/query performance.</p>
 *
 * <p>These tests use a TSpaces server started as a thread in the client VM
 *    for maximum performance.  They do NOT use the local communication
 *    mode since this seems to have a bug that caused slowdowns over
 *    time (and didn't seem to improve performance anyway)</p>
 *
 * <p> Reads are cnducted using <code>TupleSpace.readTupleById()</code>.  Writes are performed using <code>TupleSpace.write().</code>  Queries are performed using <code>TupleSpace.scan()</code>.
 * <h4>Usage</h4> 
 * <p>You must have the <code>tspaces.cfg</code> config file in the directory
 * from which you start this test (this file is included in
 * <code>src/one/fonda/java</code>.  </p>
 *
 * <pre>
 *       java BenchmarkTSpaces PATH  r/w/q/qname  
 *                             numTests numTuples 
 *                             numOperations numBytes 
 *                             numOutstanding</pre>
 *
 * <p>There are a number of parameters that must be set.</p>  
 *
 * <p>The type of test must be selected (read, write, query, or query by name).
 * See {@link one.fonda} for more information on these test types.</p>
 *
 * <p>A path in which to place the Berkeley DB databases must be specified.</p>
 * 
 * <p>The number of tests, the number of tuples available to the tests, the
 * number of operations conducted per test, the number of extra bytes to add to
 * each tuple, and the number of concurrent operations must also be
 * specified.</p> 
 *
 * @see one.fonda
 * @see one.fonda.java.BenchmarkBDB
 * @see one.world.io.BenchmarkTupleStore
 */
public class BenchmarkTSpaces {
  /** The tuple data */
  static TupleID[] allKeys; 
  static Tuple[] allData;

  static TSServer localServer;
  static TupleSpace ts;

  /** An array of first names for making PIM tuples */ 
  public final static String firstNames[] = {"Eric","Bob","Robert","Adam","Tom","Janet","Ann","Christopher","Linda","Earl","Thomas","Justin","Sarah","Gerome","Carl","Ryfie","Lisa","Alan","Jean","Ruth","Adrian","Brooks","Wendy","David","Kristi","Tali","Jason","Kelly","Suzanne","Heather","Joshua","Ryan","Isaac","Neva","Swati","Stefan","Bethany","Larry","Patrick","VS","Amy","Steve","Ed"};

  /** A random number generator */
  static Random r = new Random();

  /** A random reording of the allKeys indices */
  static int randValues[];

  /** Random number generator for making tuples */
  static Random myRand;

  /** 
   * The number of tuples to be created.  For read/query tests,  
   * this is the number of tuples in the store.  For write tests,
   * this is the number of distinct tuples available to be written.
   */
  static int numTuples;

  /**
   * The number of operations to perform.  For read/write it is the 
   * number of reads or writes per test.  Ignored for query.
   */ 
  static int numOperations;
 
  static int numOutstanding; 
  /**
   * True if we are doing a write test.  False if a read or query test. 
   */ 
  static boolean isWrite   = false;

  static Stats overallQueryStats = new Stats();
 
  /**
   * True if we are doing a query test.  False if a read or write test. 
   */ 
  static boolean isQuery   = false;

  static boolean isNameQuery   = false;
 
  /** The number of tests to perform. */
  static int numTests;
    
  /** The number of bytes to be sent in each tuple. */  
  static int numBytes;

  static Object LOCK = new Object();
  static Object START = new Object();
  static Object WAKE  = new Object();
  static int outstanding;
  static int operationCount;
  static Thread threads[];
  static Stats throughputStats = new Stats();
  static Stats latencyStats = new Stats();
  static String checkpointPath;
  /**
   * fill randValues with a random list of tuple indices(note, no repeated reads).
   */
  private static final void randomize()
  {
    int i;
    if (randValues == null) {
      randValues = new int[allKeys.length];
    }

    for (i=0;i<allKeys.length;i++) {
      randValues[i] = -1;
    }

    for (i=0;i<allKeys.length;i++) {
      int randnum;
      //No repeated values
      do {
        randnum=r.nextInt(allKeys.length);
      } while (randValues[randnum] != -1);
      randValues[randnum] = i;   
    }
  }

  /**
   * Add an entry to the database.
   */ 
  private static final TupleID doWrite(Tuple t) {
    try {
      return ts.write(t);
    } catch (Exception x) {
      exceptionExit(x);
      return null;
    }
  } 

  /**
   * Retrieve an entry from the database.
   *
   */
  private static final Tuple doReadId(TupleID id) {
    SuperTuple ret;
    try {
      ret = ts.readTupleById(id);
    } catch (Exception x) {
      exceptionExit(x);
      return null;
    }
    if (ret == null) {
       System.out.println("Warning: read not found");
    }
    return (Tuple)ret;
  }

  /**
   * Retrieve an entry from the database. (not currently used)
   */
  private static final Tuple doRead(Tuple pattern) {
    Tuple ret;
    
    try {
      ret = ts.read(pattern);
    } catch (Exception x) {
      exceptionExit(x);
      return null;
    }
    if (ret == null) {
       System.out.println("Warning: read not found");
    }
    return ret;
  }

  /**
   * Iterate over the database. 
   * 
   * <p>This function iterates over all matching tuples in the database</p>
   *
   * @param table The table to iterate over.
   */
  private static final Tuple doQuery(SuperTuple pattern) {
    Tuple ret;
   
    try {
      ret = ts.scan(pattern);
    } catch (Exception x) {
      exceptionExit(x);
      return null;
    }
    if (ret == null) {
       System.out.println("Warning: read not found");
    }
    return ret;
  }

  private static void startUp() {
   try {
      localServer = new TSServer(8200,checkpointPath,true,-1,"tspaces.cfg");
      Thread serverThread = new Thread( localServer, "TSServer" );
      serverThread.setDaemon(true);
      serverThread.start();
      //TupleSpace.setTSCmdImpl("com.ibm.tspaces.TSCmdLocalImpl");

    } catch (Exception x) {
      exceptionExit(x);
    }
  }

  /**
   * Load the table if it exists.  Create a new table if it doesn't.
   *
   */
  static void createTable() {
    try {
      ts = new TupleSpace("Example1","localhost");
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
   * Create a new tuple.
   */
  static Tuple newTuple() {
    Tuple t;
    int num;
    int rand;
    int i;
    try {
      num = myRand.nextInt(firstNames.length);
      byte[] newArray;
      t        = new Tuple();
      newArray = new byte[numBytes];
      
      t.add("firstName",firstNames[num]);
      t.add("lastName","hi");
      t.add("title","My title");
      rand = myRand.nextInt(3);

      for (i=0;i<rand;i++) {
        t.add("phone"+(i+1),""+(100+myRand.nextInt(900))+"-"+(1000+myRand.nextInt(9000)));
      }

      t.add("birthMonth",new Integer(myRand.nextInt(12)));
      t.add("birthDay",new Integer(myRand.nextInt(30)));
      t.add("birthYear",new Integer(1900+myRand.nextInt(100)));

      rand = myRand.nextInt(2);

      for (i = 0;i <rand;i++) {
        t.add("email"+(i+1),""+firstNames[num]+"@mytest.com");
      }
      t.add("streetAddress",myRand.nextInt(10000)+firstNames[myRand.nextInt(firstNames.length)]+" street");
      t.add("city","New york");
      t.add("state","NY");
      t.add("zip",new Integer(10000+myRand.nextInt(90000)));
  
      t.add("homepage",""+"http://www.mycompany.com/"+firstNames[num]);
      t.add(newArray);
  
      rand = myRand.nextInt(2);
      TupleID g[]=new TupleID[rand];
      for (i=0;i<rand;i++) {
        g[i] = new TupleID(r.nextInt(100000));
      }
      t.add(g);
    } catch (Exception x) {
      exceptionExit(x);
      return null;
    }
    return t;
  }


  /**
   * Create all of the tuples we will be using in the test.  Save
   * the serialized versions in allKeys and allData.
   */
  static void makeTuples() {
    int i;
    myRand  = new Random(1); 
    allKeys = new TupleID[numTuples]; 
    allData = new Tuple[numTuples]; 

    for (i = 0 ; i < numTuples ; i++) {
      allData[i] = newTuple();
    }
  }

  /**
   * Write all the tuples which are in allData into the table.
   */
  static void addTuples() {
    int i;

    for (i=0;i<randValues.length;i++) {
      allKeys[randValues[i]] = doWrite(allData[randValues[i]]);
    } 
    allData = null;
  }

  /** 
   * Print usage info.
   */
  static void printUsage() {
    System.out.println("You must have a TSpaces config file named tspaces.cfg");
    System.out.println("in the current directory when starting\n");
    System.out.println("Usage: BenchmarkTSpaces PATH " 
                       + "r/w/q/qname "
                       + "numTests numTuples numOperations numBytes numOutstanding");
  }

  static class doit extends Thread {

    int nextNum;
    int firstNum;
    public doit(int nextNum) throws Exception{} {
      this.nextNum = nextNum;
      this.firstNum = nextNum;
      System.out.println("Creating a thread");
    }

    /** We are done, someone else will close.  Exit. */ 
    private final static int SYNC_DONE = 1;

    /** We are done and we must close things up. */
    private final static int SYNC_CLOSING = 2;

    /** We are not done */
    private final static int SYNC_CONTINUE = 3;



    private final int doSyncEnter() {
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
            nextNum=operationCount+outstanding;
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


    public void run() {
      while(true) {
      nextNum = firstNum;
   
      try {
        synchronized(START) {
          START.wait();
        }
      } catch (Exception x){}
      while(true) {
        int res;
        if (isQuery) {
          if (isNameQuery) {
            Tuple pattern = null;
            nextNum = r.nextInt(firstNames.length);
            try {
              Field f1 = new Field(new IndexQuery("firstName",firstNames[nextNum]));
              pattern = new Tuple(f1);
            } catch (Exception e) { 
              exceptionExit(e);
            }
            Tuple results;
            results = doQuery(pattern);

            synchronized(LOCK) {
              overallQueryStats.add(results.numberOfFields());
            }

            res = doSyncEnter();
           
            if (res==SYNC_CONTINUE) {
              continue;
            } else if (res == SYNC_CLOSING) {
              synchronized(WAKE) {
                WAKE.notify();
              } 
              break;
            } else {
              break;
            }

              //frequencyStats.add(results.numberOfFields()); 
          } else {
            Tuple results;
            results = doQuery(new Tuple());

            if (numTuples!=results.numberOfFields()) {
              System.out.println("Warning: Query saw " + 
                                 results.numberOfFields() +
                                 "fields.");
            }

            res = doSyncEnter();
           
            if (res==SYNC_CONTINUE) {
              continue;
            } else if (res == SYNC_CLOSING) {
              synchronized(WAKE) {
                WAKE.notify();
              } 
              break;
            } else {
              break;
            }
          }
        } else if (isWrite) {
          doWrite(allData[nextNum]);

          res = doSyncEnter();
           
          if (res==SYNC_CONTINUE) {
            continue;
          } else if (res == SYNC_CLOSING) {
            synchronized(WAKE) {
              WAKE.notify(); 
            }
            break;
          } else {
            break;
          }
        } else {
          Tuple data;
          data = doReadId(allKeys[r.nextInt(numTuples)]);
          //data = doRead(new Tuple());
          if (data == null) {
            System.out.println("Warning: read not found");
          }
          res = doSyncEnter();
           
          if (res==SYNC_CONTINUE) {
            continue;
          } else if (res == SYNC_CLOSING) {
            synchronized(WAKE) {
              WAKE.notify(); 
            }
            break;
          } else {
            break;
          }
        }
      }
    }
   }
  } 

    /** 
     * Collect stats for a test.
     *
     * @param diff The time taken to conduct the test(in milliseconds)
     */
    static final void collectStats(int curTest,long diff) {
      if (curTest>=1) {
        System.out.println("Operations/Sec:"+ (numOperations/(diff/1000.)));
        if (numOutstanding == 1) {
          System.out.println("Latency(all operations): "+diff);
        }
        latencyStats.add(diff);
        throughputStats.add(numOperations/(diff/1000.));
      }
    }

    /**
     * Output the stats after completing all tests.
     */
    static final void outputStats() {
       //We've finished all sets of queries.
      System.out.println("");
      if (isWrite) {
        System.out.println("Benchmark: testing writes");
      } else if (isQuery) {
        if (isNameQuery) {
          System.out.println("Benchmark: testing queries (by first name)");
          System.out.println(""+firstNames.length + " distinct first names ");
        } else {
          System.out.println("Benchmark: testing queries (all records)");
        }
      } else {
        System.out.println("Benchmark: testing reads");
      }

      if (isWrite) {
        System.out.println("1 operation is a single write of a pre-made tuple"
                              +" (no repeats)");
        System.out.println("The store is emptied (destroyed and recreated) before"
                              + " each test ");
      } else if (isQuery) {
        if (isNameQuery) {
          System.out.println("1 operation is a single query matching all tuples"
			     + " with a specific firstName and an iteration through" 
                             + " these tuples");
        } else {
          System.out.println("1 operation is a single query matching all tuples"
			     + " and an iteration through these tuples");
        }
      } else {
        System.out.println("All tuples written to store prior to first test.");
        System.out.println("1 operation is a single read of a random tuple"
                              +" (may repeat)");
      }
      System.out.println(""+numTests+" tests.  "+numOperations+
                            " operations per test.");
      System.out.println("" + numTuples + " total tuples,"); 

      System.out.println(""+numOutstanding+" Concurrent operations");
      System.out.println(""+numBytes+" byte array elements added to each tuple.");
 

      System.out.println("");
      System.out.println("Averages over all tests:");
      if (numOutstanding == 1) {
        System.out.println("Average latency (ms/operation)    = " + 
                              latencyStats.average()/numOperations);
      } 
     
      System.out.println("Average Operations/Sec            = " + 
                               throughputStats.average());
      System.out.println("Standard deviation (amoung tests) = " + 
                               throughputStats.stdev());

      if (isNameQuery) {
        System.out.println("Average tuples matched per query   = " + 
                              overallQueryStats.average());
      }
    }



  /** 
   * Run the test. 
   *
   * @param args The arguments.
   */
  public static void main(String args[]) throws Exception {

     
    // Tuples Latency test
    if(args.length>0) {

      if(7 == args.length) {
	numTests = Integer.parseInt(args[2]);
	numTuples = Integer.parseInt(args[3]);
	numOperations = Integer.parseInt(args[4]);
	numBytes = Integer.parseInt(args[5]);
	numOutstanding = Integer.parseInt(args[6]);
        threads = new Thread[numOutstanding];
      } else {
        printUsage();
	System.exit(-1);
      }
      checkpointPath = args[0];
      if (args[1].equals("r")) {
      } else if (args[1].equals("w")) {
        isWrite     = true;
      } else if (args[1].equals("q")) {
        isQuery     = true;
      } else if (args[1].equals("qname")) {
        isQuery     = true;
        isNameQuery = true;
      } else {
        printUsage();
	System.exit(-1);
      }
      System.out.println("Running test: Tuples Latency test");
      System.out.println("Testing with " 
			    + numTests + " tests, " 
			    + numTuples + " tuples per test, "
                            + numOperations + " operations per test, "
			    + "and " + numBytes + " bytes per tuple.");


      startUp();
      makeTuples();

      /* Open, delete, open new.  Ensures we have an empty store */
      createTable();
      deleteTable();


      if (!isWrite) {
        randomize();
        addTuples();
      }
      for(int i = 0; i<numOutstanding; i++) {
        threads[i] = new doit(i);
        threads[i].start();
      }

      for(int j = 0; j<numTests; j++) {
        Thread.sleep(500);
        if (isWrite) {
          randomize();
          deleteTable();
        }
        operationCount = 0; 
        outstanding = numOutstanding;
        Thread.sleep(500);
	long startTime = System.currentTimeMillis();
        //threads[0].run();
        synchronized(WAKE) {
          synchronized(START) {
            START.notifyAll();
          }
          WAKE.wait();
        }
	long diff = System.currentTimeMillis() - startTime; 
        collectStats(j,diff);
      }
      outputStats();
      System.exit(-1);
    } else {
      printUsage();
      System.exit(-1);
    }
    
  }

  private static void exceptionExit(Exception x) {
    System.out.println("Exception exit: "+x);
    x.printStackTrace();
    System.exit(-1);
  }
}

