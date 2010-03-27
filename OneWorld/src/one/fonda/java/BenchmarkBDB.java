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
import one.world.core.*;
import one.util.Guid;
import one.world.io.BenchmarkTupleStore;
import com.sleepycat.db.*;
import com.sleepycat.db.Db;
import com.sleepycat.db.DbException;
import com.sleepycat.db.Dbc;
import com.sleepycat.db.Dbt;
import com.sleepycat.db.DbEnv;


/**
 * Perform latency tests for basic Berkeley DB operations.
 *
 * <p>See {@link one.fonda} for an overview of the tests.</p>
 *
 * <h4>Notes</h4>
 *
 * <p>This test does not run under <i>one.world</i> and only uses a couple of
 *    peripheral <i>one.world</i> classes.</p>
 *
 * <p>These tests check raw latency for read/write/query.  All tuples and 
 * keys are pre-serialized versions of <i>one.world</i> objects.</p>
 *
 * <p>Each read/write/query is individually transactionally protected (as would
 * be required for any concurrent use of Berkeley DB).</p>
 *
 * <h4>Usage</h4> 
 * <pre>
 *       java BenchmarkBDB PATH  r/w/q isLarge 
 *                         numTests numTuples 
 *                         numOperations numBytes 
 *                         doDeserialize</pre>
 * 
 *
 * <p>There are a number of parameters that must be set:</p>  
 *
 * <p>The type of test must be selected (read, write, or query).  See
 * {@link one.fonda} for more information on these test types.</p>
 *
 * <p>A path in which to place the Berkeley DB databases must be specified.</p>
 * 
 * <p>If <code>isLarge</code> is <code>true</code>, the read test uses
 * synthetic IDs to avoid having to store the IDs of all tuples in
 * memory.</p>
 *
 * <p>The number of tests, the number of tuples available to the tests, the
 * number of operations conducted per test, and the number of extra bytes to
 * add to each tuple must also be specified.</p> 
 *
 * <p> <code>doSerialize</code> is a boolean flag to specify whether or not the
 * serialization cost of entries being read or written should be included in
 * the measurements.</p> 
 *
 *
 * @see one.fonda
 * @see one.fonda.java.BenchmarkTSpaces
 * @see one.world.io.BenchmarkTupleStore
 */
public class BenchmarkBDB {
  /** The tuple data */
  static byte[][] allKeys; 
  static byte[][] allData;
  static Tuple[] allTuples;

  /** The path for the berkeley db environment */
  static String envPath; 

  /** The berkeley db environment */
  static DbEnv dbenv;

  /** A random number generator */
  static Random r = new Random();
  static Random myRand;

  /** A random reording of the allKeys indices */
  static int randValues[];

  /** The berkeley db table */
  static Db table;

  /** 
   * The number of tuples to be created.  For read/query tests,  
   * this is the number of tuples in the store.  For write tests,
   * this is the number of distinct tuples available to be written.
   */
  static int numTuples;

  /**
   * The number of operations to perform.  For read/write it is the 
   * number of reads or writes per test.  For query, the number of
   * times the whole store is queried.
   */ 
  static int numOperations;
  
  /**
   * True if we are doing a write test.  False if a read or query test. 
   */ 
  static boolean isWrite   = false;

  /**
   * True if we are doing a query test.  False if a read or write test. 
   */ 
  static boolean isQuery     = false;

  static boolean isLarge     = false;
  
  static boolean doSerialize = false;
  
  /** The number of tests to perform. */
  static int numTests;
    
  /** The number of bytes to be sent in each tuple. */  
  static int numBytes;

  static Stats throughputStats = new Stats();
  static Stats latencyStats = new Stats();

  /**
   * fill randValues with a random list of tuple indices 
   * (note, no repeated reads).
   */
  private static final void randomize()
  {
    int i;
    if (randValues == null) {
      randValues = new int[numTuples];
    }

    for (i=0;i<numTuples;i++) {
      randValues[i] = -1;
    }

    for (i=0;i<numTuples;i++) {
      int randnum;
      //No repeated values
      do {
        randnum=r.nextInt(numTuples);
      } while (randValues[randnum] != -1);
      randValues[randnum] = i;   
    }
  }

  /**
   * Add an entry to the database.
   *
   * @param table The table to insert into.
   * @param keyBytes The key for the data.
   * @param dataBytes The data.
   */ 
  private static final void doWrite(Db table,
                                   byte[] keyBytes, 
                                   byte[] dataBytes) {
    DbTxn txn = null;
    Dbt key  = new Dbt(keyBytes);
    Dbt data = new Dbt(dataBytes);

    do {
      try {
        txn = dbenv.txn_begin(null,0);
        table.put(txn,key,data,0); 
        txn.commit(0);
        return;
      } catch (DbDeadlockException x) {
        try {
          txn.abort();
        } catch (DbException x2) {
          exceptionExit(x2);
        }
        continue;
      } catch (DbException x) {
        exceptionExit(x);
      }
    } while(true);  
  } 

  /**
   * Retrieve an entry from the database.
   *
   * @param table The table to read from.
   * @param keyBytes The key to use for the read.
   * @return The data cooresponding to the key.  <code>null</code> if not found.
   */
  private static final byte[] doRead(Db table, byte[] keyBytes) {
    Dbt key, data;
    int response;
    DbTxn txn = null;

    key  = new Dbt(keyBytes);
    key.set_flags(Db.DB_DBT_REALLOC);

    data = new Dbt();
    data.set_flags(Db.DB_DBT_MALLOC);
  
    do {
      try {

        txn = dbenv.txn_begin(null,0);
        response=table.get(txn,key,data,0);
        txn.commit(0);

        if (response==Db.DB_NOTFOUND) {
          return null;
        } else {
          return data.get_data();
        }
      } catch (DbDeadlockException x) {
        try {
          txn.abort();
        } catch (DbException x2) {
          exceptionExit(x2);
        }
        continue;
      } catch (DbException x) {
        exceptionExit(x);
      }

    } while(true);
  }

  /**
   * Iterate over the database. 
   * 
   * <p>This function iterates over all tuples in the database</p>
   *
   * @param table The table to iterate over.
   */
  private static final void doQuery(Db table) {
    Dbt key, data;
    int response;
    DbTxn txn = null;
    Dbc cursor = null;
    boolean isFirst;
    int ret;
    int count;

    key  = new Dbt();
    key.set_flags(Db.DB_DBT_MALLOC);

    data = new Dbt();
    data.set_flags(Db.DB_DBT_MALLOC);
  
    do {
      isFirst = true;
      count   = 0;
      try {
        txn = dbenv.txn_begin(null,0);
        cursor = table.cursor(txn,0);
        while (true) {
          if (isFirst) {
            ret = cursor.get(key, data, Db.DB_FIRST);
            isFirst=false;
          } else {
            ret = cursor.get(key, data, Db.DB_NEXT);
          }

          if (ret == Db.DB_NOTFOUND) {
            break;
          }
          if (data.get_data() == null) {
            System.out.println("Null data on read");
            System.exit(0);
          } else {
            if (doSerialize) {
              try {
                ObjectInputStream ois = 
                  new ObjectInputStream(new ByteArrayInputStream(data.get_data()));
                Tuple t = (Tuple)ois.readObject();
              } catch (Exception e) {
                exceptionExit(e);
              }
            }
          }

          count++;
        }
        txn.commit(0);
        if (count!=numTuples) {
          System.out.println("Query didn't see everything");
          System.exit(-1);
        }

        return;
      } catch (DbDeadlockException x) {
        try {
          cursor.close();
          txn.abort();
 
        } catch (DbException x2) {
          exceptionExit(x2);
        }
        continue;
      } catch (DbException x) {
        exceptionExit(x);
      }

    } while(true);
  }

  /**
   * Setup the berkeley db environment.
   */
  private static void startUp() {
    try {
      
      dbenv = new DbEnv(0);

      //Run deadlock detection every time there is a lock conflict
      dbenv.set_lk_detect(Db.DB_LOCK_YOUNGEST);
      dbenv.set_lk_max_locks(100000);
      dbenv.set_lk_max_objects(100000);
      dbenv.set_lk_max_lockers(3000);
      dbenv.set_tx_max(100);
      dbenv.set_lg_max(1024*1024);

      //Open the Berkeley DB environment
      dbenv.open(envPath, 
                 Db.DB_INIT_MPOOL | 
                 Db.DB_INIT_LOCK | 
                 Db.DB_INIT_LOG | 
                 Db.DB_INIT_TXN | 
                 Db.DB_CREATE | Db.DB_RECOVER | Db.DB_THREAD,  
                 0);
      try {
        dbenv.txn_checkpoint(0,0,Db.DB_FORCE);
        dbenv.txn_checkpoint(0,0,Db.DB_FORCE);
      } catch (DbException x) {
        //Non-fatal, ignore.
      }
    } catch(DbException e) {
      exceptionExit(e);
    } catch(IOException e) {
      exceptionExit(e);
    }
  }

  /**
   * Load the table if it exists.  Create a new table if it doesn't.
   *
   * @return The new table.
   */
  static Db createTable() {
    Db newTable = null;
    try {
      newTable = new Db(dbenv, 0);
      newTable.set_error_stream(System.err);
      newTable.set_errpfx("Environment DB");

      newTable.open("testtable", null, Db.DB_BTREE, 
                    Db.DB_CREATE|Db.DB_THREAD, 0644);
    } catch (Exception e) {
      exceptionExit(e);
    }
    return newTable;
  }

  /**
   * Delete the given table.
   *
   * @param table The table to remove.
   */
  static void deleteTable(Db table) {
    try  {
      table.close(0);
      try {
        Db dbr = new Db(dbenv,0); 
        dbr.remove("testtable",null,0);
      } catch (FileNotFoundException e) {
        exceptionExit(e);
      }
      dbenv.txn_checkpoint(0,0,Db.DB_FORCE);
      dbenv.txn_checkpoint(0,0,Db.DB_FORCE);
    } catch (DbException e) {
      exceptionExit(e);
    }
  }

  /**
   * The next guid to read for in a read query.
   */
  static final byte[] nextReadGuid() { 
    Guid g;
    if (isLarge) {
      int rand;
      rand = r.nextInt(numTuples);
      g = new Guid(rand,rand);
      return g.toBytes();
    } else { 
      return allKeys[r.nextInt(numTuples)];  
    }
  }

  /**
   * The next guid to write into the store when initializing 
   * a read or query.  NOTE: this is not used for writes.
   */
  static final byte[][] nextWriteTuple(int num) {
    if (isLarge) {
      Tuple newT = newTuple(randValues[num]);
      byte[] newKey  = null;
      byte[] newData = null;

      try {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        ObjectOutputStream oos       = new ObjectOutputStream(buffer);

        oos.writeObject(newT);

        newData = buffer.toByteArray();
      } catch (Exception x) {
        exceptionExit(x);
      }
      newKey = newT.id.toBytes();
      return new byte[][] {newKey,newData};
    } else {
      return new byte[][] {allKeys[randValues[num]],allData[randValues[num]]};
    }
  }

 
  /**
   * Create a new tuple.
   */
  static Tuple newTuple(int guidNum) {
    Tuple t;
    int num;
    int rand;
    int i;
    byte[] newKey  = null;
    byte[] newData = null;
    String[] firstNames = BenchmarkTupleStore.firstNames;

    num = myRand.nextInt(firstNames.length);
    byte[] newArray;
    t        = new DynamicTuple();
    newArray = new byte[numBytes];
      
    t.set("firstName",firstNames[num]);
    t.set("lastName","hi");
    t.set("title","My title");
    rand = myRand.nextInt(3);

    for (i=0;i<rand;i++) {
      t.set("phone"+(i+1),""+(100+myRand.nextInt(900))+"-"+(1000+myRand.nextInt(9000)));
    }

    t.set("birthMonth",new Integer(myRand.nextInt(12)));
    t.set("birthDay",new Integer(myRand.nextInt(30)));
    t.set("birthYear",new Integer(1900+myRand.nextInt(100)));

    rand = myRand.nextInt(2);

    for (i = 0;i <rand;i++) {
      t.set("email"+(i+1),""+firstNames[num]+"@mytest.com");
    }
    t.set("streetAddress",myRand.nextInt(10000)+firstNames[myRand.nextInt(firstNames.length)]+" street");
    t.set("city","New york");
    t.set("state","NY");
    t.set("zip",new Integer(10000+myRand.nextInt(90000)));

    t.set("homepage",""+"http://www.mycompany.com/"+firstNames[num]);
    t.set("notes",newArray); 
    rand = myRand.nextInt(2);
    Guid g[]=new Guid[rand];
    for (i=0;i<rand;i++) {
      g[i] = new Guid();
    }
    t.set("references",g);

    if (isLarge) {
      t.id = new Guid(guidNum,guidNum);
    } 
    return t;
  }


  /**
   * Create all of the tuples we will be using in the test.  Save
   * the serialized versions in allKeys and allData.
   */
  static void makeTuples() {
    int i;
    myRand = new Random();
    Tuple newT;
    byte[] newData =null;

    if (!isLarge) { 
      if (isWrite && doSerialize) {
        allTuples = new Tuple[numTuples];
      } else {
        allKeys = new byte[numTuples][]; 
        allData = new byte[numTuples][]; 
      }

      for (i = 0 ; i < numTuples ; i++) {
        newT = newTuple(i);

        if (isWrite && doSerialize) {
          allTuples[i] = newT;
        } else {
          try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            ObjectOutputStream oos       = new ObjectOutputStream(buffer);
 
            oos.writeObject(newT);
  
            newData = buffer.toByteArray();
          } catch (Exception x) {
            exceptionExit(x);
          }


          allKeys[i] = newT.id.toBytes(); 
          allData[i] = newData;
        }
      }
    }
  }

  /**
   * Write all the tuples which are in allData into the table.
   */
  static void addTuples() {
    int i;
    byte[][] nextTuple;    

    for (i=0;i<randValues.length;i++) {
      nextTuple = nextWriteTuple(i);
      doWrite(table,nextTuple[0],nextTuple[1]);
    }
  }

  /** 
   * Print usage info.
   */
  static void printUsage() {
    System.out.println("Usage: BenchmarkBDB PATH " 
                       + "r/w/q isLarge "
                       + "numTests numTuples numOperations numBytes "
                       + "doDeserialize");
  }

  /** 
   * Collect stats for a test.
   *
   * @param diff The time taken to conduct the test(in milliseconds)
   */
  static final void collectStats(int curTest,long diff) {
    if (curTest>=1) {
      System.out.println("Operations/Sec:"+ (numOperations/(diff/1000.)));
      System.out.println("Latency(all operations): "+diff);
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
      System.out.println("Benchmark: testing queries (all records)");
    } else {
      System.out.println("Benchmark: testing reads");
    }

    if (isLarge) {
      System.out.println("Used fake guids" );
    } else {
      System.out.println("Used normal guids" );
    }

    if (isWrite) {
      System.out.println("1 operation is a single write of a pre-made tuple"
                            +" (no repeats)");
      System.out.println("The store is emptied (destroyed and recreated) before"
                             + " each test ");
      if (doSerialize) {
        System.out.println("Tuples serialized immediatly before each write.");
      } else {
        System.out.println("Tuples pre-serialized before test");
      }
    } else if (isQuery) {
      System.out.println("1 operation is a single query matching all tuples"
	              + " and an iteration through these tuples");
      if (doSerialize) {
        System.out.println("Each tuple deserialized after each read.");
      }
    } else {
      System.out.println("All tuples written to store prior to first test.");
      System.out.println("1 operation is a single read of a random tuple"
                            +" (may repeat)");
      if (doSerialize) {
        System.out.println("Tuple deserialized after each read.");
      }
    }
    System.out.println(""+numTests+" tests.  "+numOperations+
                          " operations per test.");
    System.out.println("" + numTuples + " total tuples,"); 

    System.out.println(""+numBytes+" byte array elements added to each tuple.");


    System.out.println("");
    System.out.println("Averages over all tests:");
    System.out.println("Average latency (ms/operation)    = " + 
                          latencyStats.average()/numOperations);
   
    System.out.println("Average Operations/Sec            = " + 
                             throughputStats.average());
    System.out.println("Standard deviation (amoung tests) = " + 
                             throughputStats.stdev());
  }
 
  /** 
   * Run the test. 
   *
   * @param args The arguments.
   */
  public static void main(String args[]) throws Exception {
    // Tuples Latency test
    if(args.length>0) {
      envPath = args[0];

      if(8 != args.length) {
        printUsage();
	System.exit(-1);
      }

      if (args[1].equals("r")) {
        isWrite = false;
        isQuery = false;
      } else if (args[1].equals("w")) {
        isWrite = true;
        isQuery = false;
      } else if (args[1].equals("q")) {
        isWrite = false;
        isQuery = true;
      } else {
        printUsage();
	System.exit(-1);
      }

      if (args[2].equals("true")) {
        isLarge = true;
        if (isWrite) {
          System.out.println("isLarge == true only valid for reads");
          System.exit(0);
        }
      } else if (args[1].equals("false")) {
        isLarge = false;
      }



      numTests = Integer.parseInt(args[3]);
      numTuples = Integer.parseInt(args[4]);
      numOperations = Integer.parseInt(args[5]);
      numBytes = Integer.parseInt(args[6]);

      if (args[7].equals("true")) {
        doSerialize = true;
      } else if (args[7].equals("false")) {
        doSerialize = false;
      }
      System.out.println("doSerialize == "+doSerialize);

      System.out.println("Running test: Tuples Latency test");
      System.out.println("Testing with " 
			    + numTests + " tests, " 
			    + numTuples + " tuples per test, "
                            + numOperations + " operations per test, "
			    + "and " + numBytes + " bytes per tuple.");



      startUp();
      makeTuples();
      
      /* Open, delete, open new.  Ensures we have an empty store */
      table = createTable();
      deleteTable(table);
      table = createTable();


      if (!isWrite) {
        randomize();
        addTuples();
      }
      if (!isWrite) {
        allTuples = null;
        allData   = null;
      }

      if (isLarge) {
        allKeys   = null;
      }

      for(int j = 0; j<numTests; j++) {
        if (isWrite || isLarge) {
          randomize();
        }
	long startTime = System.currentTimeMillis();
        for(int i = 0; i<numOperations; i++) {
          int nextNum;
          if (isQuery) {
            doQuery(table);
          } else if (isWrite) {
            nextNum = randValues[i];
            if (doSerialize) {
              byte[] newKey  = null;
              byte[] newData = null;

              try {
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                ObjectOutputStream oos       = new ObjectOutputStream(buffer);
        
                oos.writeObject(allTuples[nextNum]);
        
                newData = buffer.toByteArray();
              } catch (Exception x) {
                exceptionExit(x);
              }
              newKey = allTuples[nextNum].id.toBytes();

              doWrite(table,newKey,newData);
            } else {
              doWrite(table,allKeys[nextNum],allData[nextNum]);
            }
          } else {
            byte[] data;
            nextNum = r.nextInt(numTuples);
            data = doRead(table,nextReadGuid());
            if (data == null) {
              System.out.println("Warning: read not found");
              System.exit(0);
            } else {
              if (doSerialize) {
                ObjectInputStream ois = 
                  new ObjectInputStream(new ByteArrayInputStream(data));
                try {
                  Tuple t = (Tuple)ois.readObject();
                } catch (ClassNotFoundException e) {
                  exceptionExit(e);
                }
              }
            }
          }
	}

	long diff = System.currentTimeMillis() - startTime; 
        collectStats(j,diff);
        if (isWrite) {
          deleteTable(table);
          table = createTable();  
        }
      }
      outputStats();
      try {
        dbenv.close(0); 
      } catch (DbException x) {
        exceptionExit(x);
      }
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

