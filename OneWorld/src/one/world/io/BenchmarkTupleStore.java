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

package one.world.io;

import one.world.Constants;
import one.world.binding.*;
import one.world.core.*;
import one.world.env.*;
import one.world.util.*;
import one.util.Guid;

import one.world.data.Name;

import one.util.Stats;

import java.io.IOException;
import java.util.Random;
import java.util.NoSuchElementException;

/**
 * Perform tests for TupleStore operations.
 *
 * <p>See {@link one.fonda} for an overview of the tests.</p>
 *
 * <h4>Notes</h4>
 *
 * <p>These tests check raw read/write/query performance.</p>
 *
 * <p>Tests are logically synchronous, but are structured with normal
 *    asynchronous event handlers.  When using concurrent operations, an
 *    extra event handler is made for each allowable concurrent operation.</p>
 *
 * <p>The PIM tuple used is a <code>DynamicTuple</code>.</p>
 *
 * <h4>Usage</h4> 
 * <pre>
 *       load one.world.io.BenchmarkTupleStore   
 *                 r/w/q/qname isLarge 
 *                 numTests numTuples 
 *                 numOperations numBytes 
 *                 numOutstanding</pre>
 *
 *
 * <p>There are a number of parameters that must be set:</p>  
 * 
 * <p>The type of test must be selected (read, write, query, query by name).
 * See {@link one.fonda} for more information on these test types.</p>
 * 
 * <p>If <code>isLarge</code> is true, the read test uses synthetic IDs to
 * avoid having to store the IDs of all tuples in memory.</p>
 *
 * <p>The number of tests, the number of tuples available to the tests, the
 * number of operations conducted per test, the number of extra bytes to add to
 * each tuple, and the number of concurrent operations must also be
 * specified.</p>
 *
 * @see one.fonda
 * @see one.fonda.java.BenchmarkBDB
 * @see one.fonda.java.BenchmarkTSpaces
 */
public final class BenchmarkTupleStore extends Component {

  /** The number of tuples received. */
  private int numTuples;

  /** The number of tests performed. */
  private int numTests;

  /** The number of bytes in each tuple. */
  private int numBytes;       

  private int numOutstanding;

  /**
   * The number of operations to perform.  For read/write it is the 
   * number of reads or writes per test.  For query it is the number
   * of queries to perform.
   */ 
  private int numOperations;

  /**
   * The first names used to initialize the pim database.
   */
  public final static String firstNames[] = {"Eric","Bob","Robert","Adam","Tom","Janet","Ann","Christopher","Linda","Earl","Thomas","Justin","Sarah","Gerome","Carl","Ryfie","Lisa","Alan","Jean","Ruth","Adrian","Brooks","Wendy","David","Kristi","Tali","Jason","Kelly","Suzanne","Heather","Joshua","Ryan","Isaac","Neva","Swati","Stefan","Bethany","Larry","Patrick","VS","Amy","Steve","Ed"};


  /** A lease maintainer to maintain the store lease */
  private LeaseMaintainer storelm;

  /** Statistics on the number of tuples returned by the queries */
  private Stats queryStats;

  /** Statistics on the number of tuples returned by the queries */
  private Stats overallQueryStats;

  /** Count the number of operations completed so far. */
  private int operationCount;

  /** Count the number of tests performed so far. */
  private int testCount;
  
  /** The time at which the test was started. */
  private long startTime;
  
  /** The Stats object for tracking statistics. */
  private Stats latencyStats;
  /** The Stats object for tracking statistics. */
  private Stats throughputStats;

  /** The tuple to send. */
  private Tuple[] allData;

  /** The guids of all the tuples. */
  private Guid[] allKeys;
 
  /** Are we writing? */
  boolean isWrite     = false;
 
  /** Are we querying */
  boolean isQuery     = false;
 
  /** Is the query pattern a name? */
  boolean isNameQuery = false; 

  /** is this a "large" test */
  boolean isLarge = false; 

  /** How many operations have been dispatched, but not returned */
  int outstanding;

  /** Main lock */
  Object LOCK = new Object();

  /** Random number generator for reads/queries/inserts */
  Random r = new Random();

  /** 
   * Random number generator for making tuples.  Guarantees we always
   * have the same set of tuples. 
   */
  Random myRand;

  /** A random reording of the indices */
  int randValues[];

  boolean activated = false; 

  /**
   * Marker closure to indicate we are destroying our child.  Lets us 
   * pick up both success and failure just by checking the closure.
   */
  private final static Integer CLOSURE_DESTROY  = new Integer(-1); 

  // =======================================================================
  //                           The main handler
  // =======================================================================

  /** The main exported event handler. */
  private final class MainHandler extends AbstractHandler {
    /** A lease maintainer to maintain the store lease */
    private LeaseMaintainer lm;

    /** When conducting a query, this is the iterator handler */ 
    private EventHandler query; 

    /** For a query, the number of results */
    int numRead;

    
    /** 
     * Collect stats for a test.
     *
     * @param diff The time taken to conduct the test(in milliseconds)
     */
    final void collectStats(long diff) {
      SystemUtilities.debug("Operations/Sec:"+ (numOperations/(diff/1000.)));

      if (numOutstanding == 1) {
        SystemUtilities.debug("Latency(all operations): "+diff);
      }
      if (testCount != 0) {
        latencyStats.add(diff);
        throughputStats.add(numOperations/(diff/1000.));
      }
    }

    /**
     * Output the stats after completing all tests.
     */
    final void outputStats() {
       //We've finished all sets of queries.
      SystemUtilities.debug("");
      if (isWrite) {
        SystemUtilities.debug("Benchmark: testing writes");
      } else if (isQuery) {
        if (isNameQuery) {
          SystemUtilities.debug("Benchmark: testing queries (by first name)");
          SystemUtilities.debug(""+firstNames.length + " distinct first names ");
        } else {
          SystemUtilities.debug("Benchmark: testing queries (all records)");
        }
      } else {
        SystemUtilities.debug("Benchmark: testing reads");
      }

      if (isLarge) {
        SystemUtilities.debug("Used fake guids" );
      } else {
        SystemUtilities.debug("Used normal guids" );
      }

      if (isWrite) {
        SystemUtilities.debug("1 operation is a single write of a pre-made tuple"
                              +" (no repeats)");
        SystemUtilities.debug("The store is emptied (destroyed and recreated) before"
                              + " each test ");
      } else if (isQuery) {
        if (isNameQuery) {
          SystemUtilities.debug("1 operation is a single query matching all tuples"
			     + " with a specific firstName and an iteration through" 
                             + " these tuples");
        } else {
          SystemUtilities.debug("1 operation is a single query matching all tuples"
			     + " and an iteration through these tuples");
        }
      } else {
        SystemUtilities.debug("All tuples written to store prior to first test.");
        SystemUtilities.debug("1 operation is a single read of a random tuple"
                              +" (may repeat)");
      }
      SystemUtilities.debug(""+numTests+" tests.  "+numOperations+
                            " operations per test.");
      SystemUtilities.debug("" + numTuples + " total tuples,"); 

      SystemUtilities.debug(""+numOutstanding+" Concurrent operations");
      SystemUtilities.debug(""+numBytes+" byte array elements added to each tuple.");
 

      SystemUtilities.debug("");
      SystemUtilities.debug("Averages over all tests:");
      if (numOutstanding == 1) {
        SystemUtilities.debug("Average latency (ms/operation)    = " + 
                              latencyStats.average()/numOperations);
      } 
     
      SystemUtilities.debug("Average Operations/Sec            = " + 
                               throughputStats.average());
      SystemUtilities.debug("Standard deviation (amoung tests) = " + 
                               throughputStats.stdev());

      if (isNameQuery) {
        SystemUtilities.debug("Average tuples matched per query   = " + 
                              overallQueryStats.average());
      }
    }

    /**
     * Perform a Guid keyed read.
     *
     * @param id The Guid of the tuple to read.
     */
    final void doIDRead(Guid id) {
      Query q = new Query("id",Query.COMPARE_EQUAL,id);
      InputRequest ir = new InputRequest(this,null,InputRequest.READ,q,
                                         0,false,null);
      store.handle(ir);
    }


    /**
     * Perform a Query on a random firstName
     */
    final void doNameQuery() {
      numRead = 0;
      Query q = new Query("firstName",
                          Query.COMPARE_EQUAL,
                          firstNames[r.nextInt(firstNames.length)]);
      InputRequest ir = new InputRequest(this,null,InputRequest.QUERY,
                                         q,100000,false,null);
      store.handle(ir); 

    }

    /**
     * Perform a Query on the entire store.  
     *
     * This query matches ALL tuples in the store.
     */
    final void doAllQuery() {
      Query q = new Query();
      InputRequest ir = new InputRequest(this,null,InputRequest.QUERY,
                                         q,100000,false,null);
      store.handle(ir); 
    }

    /* Enumeration of the synchronization states */

    /** We are done, someone else will close.  Exit. */ 
    private final static int SYNC_DONE = 1;

    /** We are done and we must close things up. */
    private final static int SYNC_CLOSING = 2;

    /** We are not done */
    private final static int SYNC_CONTINUE = 3;

    int nextNum;
    /** 
     *When returning from an operation, should we continue or stop? 
     *
     * @return The SYNC_* enumeration value.
     */
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

    /**
     * Secondary handler if we're benchmarking reads.
     */
    private final boolean readHandle(Event e) {
      int res;

      if (e instanceof InputResponse) {
        InputResponse ir = (InputResponse)e;
        if (ir.tuple == null) {
          SystemUtilities.debug("ERROR: read resulted in null value");
          System.exit(0);
        }
        res = doSyncEnter();
        switch(res) {
          case SYNC_CONTINUE:
            //Do another read
            doIDRead(nextReadGuid());
            return true; 
          case SYNC_DONE: 
            return true; 
          case SYNC_CLOSING: {
            //We're done another set of tests
            long diff = System.currentTimeMillis() - startTime; 
            collectStats(diff);
            testCount++;
            if(numTests == testCount) {
              //We're done all sets of tests
              outputStats();
              stop();
            } else {
              //Start up another test.
              int i;
              operationCount = 0;
              startTime = System.currentTimeMillis();
              synchronized(LOCK) {
                outstanding=numOutstanding;
              }         

              for (i=0;i<numOutstanding;i++) {
                main[i].doIDRead(nextReadGuid());
              }
            }
            return true;
          }
          default:
            return true;
        }
      } else if (e instanceof BindingResponse) {
        //We're bound to the empty store.  Initialize with data.  Randomize the 
        //Insertion order.
        myRand = new Random(1);
        operationCount = 0;
        randValues = new int[numTuples];
        randomize(randValues);
        if (!isLarge) {
          allKeys[operationCount] = allData[randValues[0]].id;
        }
        store.handle(new OutputRequest(main[0],null,nextWriteTuple(operationCount),null));
        return true;
      } else if (e instanceof OutputResponse) {
        operationCount++;

        if(numTuples == operationCount) {
          int i;
          allData = null;
          //We've inserted all the tuples.  Start timing and reading.
          operationCount = 0;
          startTime = System.currentTimeMillis();
          synchronized(LOCK) {
            outstanding=numOutstanding;
          }
          for (i=0;i<numOutstanding;i++) {
            main[i].doIDRead(nextReadGuid());
          }
        } else {
          //Insert the next tuple.
          Tuple t=nextWriteTuple(operationCount);
          if (!isLarge) {
            allKeys[operationCount] = t.id;
          }
          store.handle(new OutputRequest(main[0],null, t, null));
        }
        return true;
      }
      return false;
    }

    /**
     * We're done with the current query.
     *
     * @param numRead The number of items read.
     */
    private final void doneQuery(int numRead) {
      int res;

      if (isNameQuery) {
        //Add statistics saying how many tuples we matched this time.
        synchronized(LOCK) {
          overallQueryStats.add(numRead);
        }
      }
      res = doSyncEnter();
 
      switch(res) {
        case SYNC_DONE:
          return; 
      
        case SYNC_CLOSING: {
          //We've finished another set of queries.
          long diff = System.currentTimeMillis() - startTime; 

          collectStats(diff);

          testCount++;

          if(numTests == testCount) {
            //We've finished all sets of queries.
            outputStats();
            stop();
          } else {
            int i;
            operationCount = 0;
            startTime = System.currentTimeMillis();
            synchronized(LOCK) {
              outstanding=numOutstanding;
            }
            for (i=0;i<numOutstanding;i++) {
              if (isNameQuery) {
                main[i].doNameQuery();
              } else {
                main[i].doAllQuery();
              }
            }
          }
          return;
        }

        case SYNC_CONTINUE:
          if (isNameQuery) {
            doNameQuery();
          } else {
            doAllQuery();
          }
          return;
        default:
          return;
      }
    }

    /**
     * Secondary handler to deal with queries.
     */
    private final boolean queryHandle(Event e) {
      if (e instanceof IteratorElement) {
        IteratorElement ie = (IteratorElement)e;

        if (isNameQuery) { 
          numRead++;
        }
        if (ie.hasNext) {
          //Continue iterating.
          query.handle(new IteratorRequest(this,null));
        } else {
          //We're done iterating over tuples.
          lm.cancel();
          lm = null;
          doneQuery(numRead);
        }
        return true;
      } else if (e instanceof QueryResponse) {
        //Start iterating over the results.
        QueryResponse qr = (QueryResponse)e;
        lm = new LeaseMaintainer(qr.lease,qr.duration,this,null,timer);
        query = qr.iter; 
        query.handle(new IteratorRequest(this,qr.closure));

        return true;
      } else if (e instanceof BindingResponse) {
        //We're bound to the store.  Start filling the store.
        operationCount = 0;
        randValues = new int[numTuples];
        randomize(randValues);
        Tuple t = nextWriteTuple(operationCount);
        store.handle(new OutputRequest(main[0],null,t,null));
        return true;
      } else if (e instanceof OutputResponse) {
        operationCount++;

        if(numTuples == operationCount) {
          //We're done initializing the store, start the query.
          operationCount = 0;
          allData = null;
          allKeys = null;
          startTime = System.currentTimeMillis();
          synchronized(LOCK) {
            outstanding=numOutstanding;
          }
          for (int i=0;i<numOutstanding;i++) {
            if (isNameQuery) {
              main[i].doNameQuery();
            } else {
              main[i].doAllQuery();
            }
          }
        } else {
            //Continue initializing the store.
          Tuple t = nextWriteTuple(operationCount);
          store.handle(new OutputRequest(main[0],null, t,null));
          return true;
        } 
      } else if (e instanceof ExceptionalEvent) {
        ExceptionalEvent ee=(ExceptionalEvent)e;
        if (ee.x instanceof NoSuchElementException) {
          if (lm != null) {
            lm.cancel();
            lm = null;
            doneQuery(numRead);
            return true;
          } else {
            doneQuery(numRead);
            return true;
          }
        }
      }
      return false;
    }


    /**
     * Secondary handler to deal with writes.
     */
    private final boolean writeHandle(Event e) {
      int i;
      int res;

      if (e instanceof OutputResponse) {
        res = doSyncEnter();        

        switch (res) {
          case SYNC_CONTINUE: 
            store.handle(new OutputRequest(this,null,allData[randValues[nextNum]],null));
            return true; 
          case SYNC_DONE:
             return true; 
          case SYNC_CLOSING: {
            //We're done another set of tests.
            long diff = System.currentTimeMillis() - startTime; 
            collectStats(diff);

            testCount++;
            if(numTests == testCount) {
              outputStats(); 
              stop();
            } else {
              //This will result in a BindingResponse
              cleanupData();
            }
            return true;
          }
          default:
            return true;
        }
      } else if (e instanceof BindingResponse) {
        //We're bound to the empty store.
        
        operationCount = 0;
        randValues = new int[numTuples];
        randomize(randValues);
        startTime = System.currentTimeMillis();
        outstanding = numOutstanding;
        for (i=0;i<numOutstanding;i++) {
          store.handle(new OutputRequest(main[i],null,
                                         allData[randValues[i]],
                                         null));
        }
        return true;
      }
      return false;
    }

    /** Handle the specified event. */
    protected boolean handle1(Event e) {
      if (e.closure instanceof Integer) {
        //Handle a delete of a the child environment whether or not
        //it existed.
        if (e.closure.equals(CLOSURE_DESTROY)) {
          createEnvironment();
          return true;
        }
      } 
      if (e instanceof EnvironmentEvent) {
        EnvironmentEvent ee = (EnvironmentEvent)e;
        
        if (EnvironmentEvent.ACTIVATED == ee.type) {
          if (activated == true) {
            System.out.println("Must unload and reload before re-running.");
            return true;
          }
          activated = true;
          activate(); 
        } else if ( (EnvironmentEvent.RESTORED == ee.type) ||
                  (EnvironmentEvent.MOVED == ee.type) ) {
          return true;
        } else if (EnvironmentEvent.STOP == ee.type) {
          respond(e, new EnvironmentEvent(this, null, 
                                       EnvironmentEvent.STOPPED,
                                       getEnvironment().getId()));
        } else if (EnvironmentEvent.CREATED == ee.type) {
          //Bind the new empty store.
          bindStore();
        }
        return true;        
      } else if (e instanceof BindingResponse) {
        BindingResponse br = (BindingResponse)e;
        store = br.resource;
        //Maintain the lease.
        storelm = new LeaseMaintainer(br.lease,br.duration,this,null,timer);
        //No return.  The test specific handler has to deal with this as well.
      } else if (e instanceof LeaseEvent) {

        return true;
      } else if (e instanceof ExceptionalEvent) {
        if( ((ExceptionalEvent)e).x instanceof NoBufferSpaceException) {
          ((ExceptionalEvent)e).x.printStackTrace();
          System.exit(-1);
          return true;
        }
      }

      //Secondary handlers for each test type.
      if (isWrite) {
        return writeHandle(e);  
      } else if (isQuery) {
        return queryHandle(e);
      } else {
        return readHandle(e);
      }
    }
  }
  

  // =======================================================================
  //                           Descriptors
  // =======================================================================

  /** The component descriptor. */
  private static final ComponentDescriptor SELF =
    new ComponentDescriptor("one.world.io.BenchmarkTupleStore",
                            "A component for benchmarking the latency of the " 
                            + "TupleStore component.",
                            false);

  /** The exported event handler descriptor for the main handler. */
  private static final ExportedDescriptor MAIN =
    new ExportedDescriptor("main",
                           "The exported main handler.",
                           null,   // XXX
                           null,   // XXX
                           false);

  /** The imported event handler descriptor for the request handler. */
  private static final ImportedDescriptor REQUEST =
    new ImportedDescriptor("request",
                           "The imported request handler.",
                           null,   // XXX
                           null,   // XXX
                           false,
                           false);
  

  // =======================================================================
  //                           Instance fields
  // =======================================================================

  /**
   * The main exported event handler.
   *
   * @serial  Must not be <code>null</code>.
   */
  private final MainHandler       main[];

  /**
   * The request imported event handler.
   *
   * @serial  Must not be <code>null</code>.
   */
  private final Component.Importer request;
 
  /** A timer component. */
  private final Timer timer;

  /** The bound store handler. */
  private EventHandler store;


  // =======================================================================
  //                           Constructor
  // =======================================================================

  /**
   * Create a new instance of <code>BenchmarkTupleStore</code>.
   *
   */
  public BenchmarkTupleStore(Environment env, boolean isWrite, 
                                    boolean isQuery, boolean isNameQuery,boolean isLarge,
                                    int numTests,int numTuples,
                                    int numOperations, 
                                    int numBytes, 
                                    int numOutstanding) throws IOException {
    super(env);
    int i;
    main = new MainHandler[numOutstanding];
    for (i=0;i<numOutstanding;i++) {
      main[i] = new MainHandler();
    }
    declareExported(MAIN, main[0]);
    request = declareImported(REQUEST);

    timer = getTimer();
    
    this.isWrite         = isWrite;
    this.isQuery         = isQuery;
    this.isNameQuery     = isNameQuery; 
    this.numTests        = numTests;
    this.numTuples       = numTuples;
    this.numOperations   = numOperations;
    this.numBytes        = numBytes; 
    this.numOutstanding  = numOutstanding;
    this.isLarge         = isLarge; 
    this.throughputStats = new Stats();
    this.latencyStats    = new Stats();
    this.overallQueryStats = new Stats();
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

  /** Initialize the application.
   * @param  env  The enclosing environment.
   * @param  closure  The closure.
   */
  public static void init(Environment env, Object closure) throws Exception {
    String[] args = (String[])closure;
    BenchmarkTupleStore main;
    boolean isWrite = false;
    boolean isQuery = false;
    boolean isNameQuery = false;
    boolean isLarge = false;
    if(7 == args.length) {
      if (args[0].equals("r")) {
      } else if (args[0].equals("q")) {
        isQuery = true;
      } else if (args[0].equals("qname")) {
        isQuery = true;
        isNameQuery = true;
      } else if (args[0].equals("w")) {
        isWrite = true;
      } else {
       throw new 
        IllegalArgumentException("Usage: BenchmarkTupleStore " 
                                 + "w/r/q/qname isLarge numTests numTuples "
                                 + "numOperations numBytes numOutstanding");
      } 

      if (args[1].equals("true")) {
        isLarge = true;
        if (isWrite) {
          SystemUtilities.debug("isLarge == true only valid for reads");
        }
      } else if (args[1].equals("false")) {
        isLarge = false;
      }

      int numTests = Integer.parseInt(args[2]);
      int numTuples = Integer.parseInt(args[3]);
      int numOperations = Integer.parseInt(args[4]);
      int numBytes = Integer.parseInt(args[5]);
      int numOutstanding = Integer.parseInt(args[6]);

      main = new BenchmarkTupleStore(env, isWrite, isQuery, isNameQuery, isLarge,
                                            numTests,numTuples,
                                            numOperations,numBytes,numOutstanding);
    } else {
       throw new 
        IllegalArgumentException("Usage: BenchmarkTupleStore " 
                                 + "w/r/q/qname isLarge numTests numTuples "
                                 + "numOperations numBytes numOutstanding");
    }
    env.link("main","main",main);
    main.link("request","request",env);
  }
 
  /** Handle an ACTIVATE envrionment event. */
  private void activate() {
    if (isWrite) {
      SystemUtilities.debug("Running TupleStore benchmark: " 
                            + "testing writes");
    } else if (isQuery) {
      if (isNameQuery) {
        SystemUtilities.debug("Running TupleStore benchmark: " 
                              + "testing queries (by name)");
      
      } else {
        SystemUtilities.debug("Running TupleStore benchmark: " 
                              + "testing queries (all records)");
     
      }
    } else {
      SystemUtilities.debug("Running TupleStore benchmark: " 
                            + "testing reads");
    }
    if (isLarge) {
      SystemUtilities.debug("Using fake guids");
    } else {
      SystemUtilities.debug("Using normal guids");
    }
    SystemUtilities.debug("Testing with:");
    SystemUtilities.debug("              "+ numTests + " tests,"); 
    SystemUtilities.debug("              " + numTuples + " tuples,"); 
    SystemUtilities.debug("              " + numOperations +" operations per test,");
    SystemUtilities.debug("              " + numOutstanding +" outstanding operations,");
    SystemUtilities.debug("              " + numBytes + " extra byte elements per tuple.");
    
    initWriteTuples();
    cleanupData();
  }
  
  /** Stop the application. */
  private void stop() {
    int i;
    //release resources 
    if (storelm!=null) {
      storelm.cancel();
      storelm = null;
    }
  }

  /**
   * Create an empty child environment.
   */
  void createEnvironment() {
    CreateRequest creq;
    Environment curEnv;

    curEnv = getEnvironment();


    creq = new CreateRequest(main[0], null, curEnv.getId(), "data", false,
                             null, null);
    request.handle(creq); 
  }

  /**
   * Destroy the child environment.
   */
  void cleanupData() {
    EnvironmentEvent ee;
    Guid childID;
    Environment env;
    Environment curEnv;

    curEnv = getEnvironment();

    env = curEnv.getChild("data");       
    if (storelm != null) {
      storelm.cancel();
      storelm = null;
    }
    if (env == null) {
      createEnvironment();
    } else {
      ee = new EnvironmentEvent(main[0], CLOSURE_DESTROY, 
                                EnvironmentEvent.DESTROY,env.getId());
      request.handle(ee);
    }
  }

  /**
   * Produce all the tuples we will use.
   * If <code>isLarge == true</code>, this does not actually produce the
   * tuples.  It does, however, reset the <code>myRand</code> random number 
   * generator.
   */
  void initWriteTuples() {
    myRand = new Random(1);
    if (!isLarge) {
      allData = new Tuple[numTuples];
      allKeys = new Guid[numTuples];
      for (int i = 0; i<numTuples; i++) {
        allData[i] = newTuple(i);
      }
    } else {
      allData = null;
      allKeys = null;
    }
  }

  /**
   * Bind to the store.
   */
  void bindStore() {
    SioResource sio;
    BindingRequest bindreq;
    stop(); 
  
    //Bind to the tuple store
    sio     = new SioResource("data"); 
    bindreq = new BindingRequest(main[0],null,sio,Duration.FOREVER);

    request.handle(bindreq);
  }

  /**
   * fill randValues with a random list of tuple indices(note, no repeated reads).
   * This is used to randomize the order tuples are written to the store.
   */
  private final void randomize(int rands[])
  {
    int i;

    for (i=0;i<rands.length;i++) {
      rands[i] = -1;
    }

    for (i=0;i<rands.length;i++) {
      int randnum;
      //No repeated values
      do {
        randnum=r.nextInt(rands.length);
      } while (rands[randnum] != -1);
      rands[randnum] = i;   
    }
  }

  /**
   * The next guid to read for in a read query.
   */
  final Guid nextReadGuid() {
    if (isLarge) {
      int rand;
      rand = r.nextInt(numTuples);
      return new Guid(rand,rand);
    } else { 
      return allKeys[r.nextInt(numTuples)];  
    }
  }

  /**
   * The next guid to write into the store when initializing 
   * a read or query.  NOTE: this is not used for writes.
   */
  final Tuple nextWriteTuple(int num) {
    if (isLarge) {
      return newTuple(randValues[num]);
    } else {
      return allData[randValues[num]];
    }
  }

  /**
   * Create a new tuple.
   *
   * @param guidNum The guid number for the new tuple(ignored unless 
   *                <code>isLarge == true</code>.
   * @return The new tuple.
   */
  Tuple newTuple(int guidNum) {
    Tuple t;
    int num;
    int rand;
    int i;

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
}

