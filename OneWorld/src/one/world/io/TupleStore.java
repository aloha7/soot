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

/*

****NOTE: when using a cursor with DB_SET, you cannot specify realloc for
the key or the search won't work.

*/

package one.world.io;

import java.io.IOException;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;

import one.world.core.*;
import one.world.util.Log;
import one.util.Guid;
import one.util.Bug;
import one.world.binding.*;
import one.world.util.*;
import one.world.data.Name;
import one.world.data.BinaryData;

import java.util.NoSuchElementException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.io.*;
import java.util.LinkedList;

import com.sleepycat.db.*;
import com.sleepycat.db.Db;
import com.sleepycat.db.DbException;
import com.sleepycat.db.Dbc;
import com.sleepycat.db.Dbt;
import com.sleepycat.db.DbEnv;

class ResourceInfo extends Tuple {
  public EventHandler lease;
  public EventHandler requestor;
  public Object requestClosure;

  public ResourceInfo( ) {}
  public ResourceInfo(EventHandler lease, 
                      EventHandler requestor,
                      Object requestClosure,
                      Guid id) {
    this.lease          = lease;
    this.requestor      = requestor;
    this.requestClosure = requestClosure;
    this.id          = id;
  } 
  
}

class TimerResponseEvent extends TypedEvent 
{
  public final static int TYPE_CLEANLOGS = 1;
  public TimerResponseEvent(EventHandler src,Object cls,int type) {
    super(src,cls,type);
  }
  public TimerResponseEvent() {
  }
}

/**
 * A implementation of persitant storage though structured IO.
 *
 * <p>NOTE: this class is for internal use by one.world.  User code
 *    cannot create instances of this class, or call functions
 *    specified in this class.</p>
 *
 * <p>TupleStore is a factory for connections to actual Tuple stores.
 * This class holds a reference to the overall berkeley DB env and
 * returns event handlers to the tuple spaces within this env.</p>
 *
 * <p>Generally errors are reported as follows: </p>
 *   <ul>
 *   <li>Errors related to a 
 *   corrupted indices(inconsistent table entries) or general berkeley
 *   db errors (DbRunRecovery or DBException where none should be expected)
 *   are announced as thrown Bug's.</li>
 *   <li> Errors due to bad arguments passed into static methods are 
 *        thrown as Bug()</li>  
 *   <li> Recoverable errors which cause the
 *        function to fail but otherwise leave the database active
 *        and in the state before the call throw IOException</li> 
 *   </ul>
 *
 *
 * @author   Ben Hendrickson
 * @author   Eric Lemar
 */

public class TupleStore extends Component {
  // =======================================================
  //                     Private Constants
  // =======================================================

  /** Major version of the current disk format */
  private static final long LATEST_DISK_MAJOR_VERSION=2;
  /** Minor version of the current disk format */
  private static final long LATEST_DISK_MINOR_VERSION=1;

  /** Info table key for the disk major version */
  private static final byte[] MAJOR_VERSION_KEY =new byte[] {'1'};
  /** Info table key for the disk minor version */
  private static final byte[] MINOR_VERSION_KEY =new byte[] {'2'};

  /** No initialization has been performed */
  private static final int STATE_INITIAL = 1;

  /** startUp has been called successfully */
  private static final int STATE_LOADED = 2;

  /** The TupleStore object has been created and we are running */
  private static final int STATE_RUNNING = 3;

  /** shutDown has been called and we are stopped */
  private static final int STATE_CLOSED = 4;

  /** True if we should try to delete logs when no longer
      needed */
  private static boolean cleanLogs = true;

  // =======================================================
  //                     Global State
  // =======================================================

  /** The state of the component */
  private static int state = STATE_INITIAL;

  /** The tuple store global lock */
  private static final Object TSTORE_LOCK = new Object();

  /** The lock for modifying the directory structure */
  private static final Object PATH_LOCK = new Object();

  /** A set of File objects representing paths that can't be deleted*/
  Set lockedPaths = new HashSet();

  /** The version numbers of the disk database that we read */
  private static long diskMajor, diskMinor;

  /*****************************************************
   * Berkeley DB Internal settings
   *****************************************************/
   
  /** The maximum number of concurrent transactions */
  private static int maxTransactions = 100;

  /** The maximum number of locks */ 
  private static int maxLocks = 100000;

  /** The maximum number of lockers */ 
  private static int maxLockers = 3000;
 
  /** The maximum size of a single log file(in bytes) */
  private static int logFileSize = 1024*1024;


  /**
   * We want to split the stores into multiple directories so that
   * we don't have directory lookup issues(for instance in linux,
   * a directory lookup is O(n^2) where n is the number of files in
   * a directory.
   *
   * This tells us how to split the directory structure for storing tuple
   * stores.  Each number is how many characters in the Guid to use for the
   * next directory label.  These must add up to at most the length of 
   * of the string representation of the Guid(32 characters).
   *
   * In selecting, chose as follows: want to support at least 500,000 
   * environments(fairly arbitrary choice).  Want at most a couple thousand
   * files per directory.  Split at the first level using two characters.  Have
   * at most 256 entries at this level.  Assuming uniform distribution in the
   * first two digits(these are lower order time, so reasonable), This gives
   * 500,000/256=2000 Guids mapped to each second level directory, giving us an
   * expected 800 different directories per second level.  The next two levels
   * are just to provide extra splits in case somthing makes Guid's in an
   * unexpected manner.   
   *
   * To change the split, all you have to do is change this array.
   */

  private static int DIR_SPLITS[] = new int[] {2,3,4,4};
  /**
   * A GUID to TupleStore map of the handlers for all of the tuple 
   * spaces currently loaded.
   */
  private static HashMap allStores;
 

  /**
   * A guid to Environment.descriptor mapping.
   * The format of the key is:
   * |16 byte guid|
   *
   * The format of the data is:
   * |16 byte parent guid|16 byte protection guid|name bytes|
   */
  private static Db mappingTable;

  /**
   * A parent to child mapping for the environments.  Each child
   * is a seperate entry.
   *
   * In addition to the normal entries, there are entries with
   * a key of '0' and a mapping of a Guid.  These signal 
   * environments currently being created.  If we crash before
   * deleting these entries, these environments will be removed
   * the next time we <code>startUp()</code>.
   */
  private static Db childrenTable;

  /**
   * Path to the berkeley DB environment directory
   */
  private static File rootDBEnvPath;

  /**
   * The berkeley DB environment that holds our tuple stores.
   */
  private static DbEnv dbenv = null;

  /**
   * Ensuress only a single one.world is in the store.
   */
  private static DbLock mutexLock = null;

  /**
   * The root one.world environment(NOT the berkeley db env)
   */
  private static Environment rootEnv;

  /** The system log. */
  private static final Log log;
  
  /* Initialize the system log variable. */
  static {
    log = (Log)AccessController.doPrivileged(new PrivilegedAction() {
      public Object run() {
        return Log.getSystemLog();
      }
    });
  }

  /** Hold the TupleStore instance for debugging */ 
  private static TupleStore tsf;

  /** The lease manager handler*/
  private static Component.Importer leaseHandler;

  /** The timer */
  private static Timer timer;

  /** The notification class for the periodic timer */
  private static Timer.Notification periodicTimerNotification;   

  /** Hold a pointer to the BindingRequest event handler */
  final EventHandler bindHandler;

  /** Receive events from the timer component */
  private static EventHandler timeResponseHandler = new AbstractHandler() {
    private boolean toggle=true;

    public boolean handle1(Event ev) {
      if (ev instanceof TimerResponseEvent) {
        TimerResponseEvent tev=(TimerResponseEvent)ev;
        toggle=!toggle;
        switch(tev.type) {
          case TimerResponseEvent.TYPE_CLEANLOGS:
            if (toggle) {
              try {
                dbenv.txn_checkpoint(0,0,0);
              } catch (DbException x) {
              }
            }
            cleanUpLogs(dbenv);
            break;
        }
        return true;
      }
      return false;
    }
  };


  /**
   * Debugging interfaces used by TestTupleStore
   *
   * @throws SecurityException
   *                 Signals that the caller does not have permission
   *                 to manage environments.
   */
  static TupleStore getTupleStoreInstance() {
    Environment.ensurePermission();
    return tsf;
  }
  /**
   * Debugging.  Provides external access to kill all binds
   * on the named environment.
   *
   * @param id The environment to undbind.
   * @throws SecurityException
   *                 Signals that the caller does not have permission
   *                 to manage environments.
   */
  static void revokeAll(Guid id) {
    Environment.ensurePermission();
    ActualStore tEnv;
  
    synchronized(TSTORE_LOCK) {
      tEnv=(ActualStore)allStores.get(id);
    }
    if (tEnv!=null) {
      tEnv.revokeAll(true);
    }

  }

  /**
   * Debugging. Find the number of people bound to the
   * given environment.
   *
   * @param id The environment to Query.
   * @return The number of bindings. 
   * @throws SecurityException
   *                 Signals that the caller does not have permission
   *                 to manage environments.
   */
  static int getNumberBound(Guid id) {
    Environment.ensurePermission();
    synchronized(TSTORE_LOCK) {
      ActualStore tEnv=(ActualStore)allStores.get(id);
      if (tEnv==null) {
        return 0;
      } else {
        return tEnv.managers.size();
      }
    }
  } 
  // =======================================================
  //                     Descriptors
  // =======================================================

  /** The Tuplestore component descriptor*/
  private static final ComponentDescriptor SELF = 
    new ComponentDescriptor("one.world.io.TupleStore",
                            "The tuple store",
                            true);

  /** The binding request handler for TupleStore */
  private static final ExportedDescriptor HANDLER = 
    new ExportedDescriptor ("bind",
                            "Binding request handler for TupleStore",
                            new Class [] { BindingRequest.class},
                            new Class[0],
                            false
                            );

  /** Descriptor for the imported lease request handler. */
  private static final ImportedDescriptor LEASE =
      new ImportedDescriptor(
              "lease",
              "Lease request handler",
              new Class[] { LeaseEvent.class },
              new Class[] { LeaseDeniedException.class },
              false,
              false );


  public ComponentDescriptor getDescriptor() {
    return (ComponentDescriptor)SELF.clone();
  }

  // =========================================================
  //        Public Tuple Store initialization/freeing
  // ============================================================

  /**
   * Creates an instance of a TupleStore. 
   *
   * <p>This holds the state of the berkeley db system.
   * Only one of these should be made.</p>
   *
   * @param env The environment we are running under.
   * @throws SecurityException
   *                 Signals that the caller does not have permission
   *                 to manage environments.
   */
  public TupleStore(Environment env) 
  {
    super(env);
    Environment.ensurePermission();

    synchronized (TSTORE_LOCK) {
      if (state!=STATE_LOADED)
        throw new Bug("Wrong state: not STATE_LOADED in TupleStore");
    }

    rootEnv = env;
    tsf     = this;

    //Export the handler on which we receive binding requests.
    bindHandler=declareExported(HANDLER,new BindHandler());

    //Get the handler for the lease manager.
    leaseHandler = declareImported(LEASE); 


    timer = getTimer();


    synchronized (TSTORE_LOCK) {
      if (state==STATE_LOADED) {
        state=STATE_RUNNING;
      } else {
        throw new Bug("State changed when it shouldn't in TupleStore: "
                      + state);
      }
    }
  }


  /**
   * Remove any log files not involved in an active transaction.
   */
  private static void cleanUpLogs(DbEnv dbenv)  {
    String[] oldLogs=null;
    int i;
    if (!cleanLogs) {
      return;
    }

    try {
      synchronized(TSTORE_LOCK) {
        if ((state==STATE_RUNNING) || (state==STATE_INITIAL)) {
          //Crashes on unpatched bdb(3.1.17)
          oldLogs=dbenv.log_archive(Db.DB_ARCH_ABS);
        }
      }
      if (oldLogs!=null) {
        for (i=0;i<oldLogs.length;i++) {
          File f=new File(oldLogs[i]);
          f.delete();
        }
      }
    } catch (DbException x) {
    }
  }

  /**
   * Start the timer events that cause old log files to be removed
   */
  private static void startLogTrim()  {
    TimerResponseEvent respEvent;
//    Timer.Event tEvent;
    respEvent = new TimerResponseEvent(timeResponseHandler,
                                       null,
                                       TimerResponseEvent.TYPE_CLEANLOGS);
    periodicTimerNotification = timer.schedule(Timer.FIXED_DELAY,
                   SystemUtilities.currentTimeMillis()+60000,
                   60000,timeResponseHandler,respEvent);
    
  }
  /**
   * Start up the berkeley db system.  
   *
   * <p> If a tuple store hierarchy already exists in the given directory,
   * this tuple store is loaded.  If the hierarchy does not exist, a new
   * completely empty hierarchy is created.  The user is responsible for
   * creating the root environment.  If the tuple store hierarchy is new,
   * getRootDescriptor() will return null</p>
   *
   * <p> Internally, this opens the berkeley DB env as well as the
   * environment index files.</p>
   *
   * <p>Throws an IOException if there is an error opening the Berkeley DB
   * system. If startUp throws an exception, you do not need to call 
   * {@link #shutDown() shutDown()}.</p>
   *
   *
   * @param rootDBEnvPath The directory Berkeley DB should use for its
   *                      environment.  A subdirectory named data will be
   *                      made that holds all of the one.world environments
   *
   * @throws IOException Signals there was a problem opening the Hierarchy so
   *                     the hierarchy was not opened.
   * @throws SecurityException
   *                 Signals that the caller does not have permission
   *                 to manage environments.
   */

  public static void startUp(java.io.File rootDBEnvPath) throws IOException {
    Environment.ensurePermission();
    File mainFile;
    File dataPath;

    synchronized (TSTORE_LOCK) {
      if (state!=STATE_INITIAL)
        throw new Bug("Wrong state: not STATE_INITIAL in startUp");
    }

    allStores=new HashMap();
    try {
      
      TupleStore.rootDBEnvPath=rootDBEnvPath;
      dbenv = new DbEnv(0);

      //Run deadlock detection every time there is a lock conflict
      dbenv.set_lk_detect(Db.DB_LOCK_YOUNGEST);

//      dbenv.set_lk_max(maxLocks);
      dbenv.set_lk_max_objects(maxLocks);
      dbenv.set_lk_max_locks(maxLocks);
      dbenv.set_lk_max_lockers(maxLocks);


      dbenv.set_tx_max(maxTransactions);
      dbenv.set_lg_max(logFileSize);
      //Open the Berkeley DB environment
      dbenv.open(rootDBEnvPath.getAbsolutePath(), 
                 Db.DB_INIT_MPOOL | 
                 Db.DB_INIT_LOCK | 
                 Db.DB_INIT_LOG | 
                 Db.DB_INIT_TXN | 
                 Db.DB_CREATE | Db.DB_RECOVER | Db.DB_THREAD,  
                 0);

      if (cleanLogs) {
        cleanUpLogs(dbenv);
      }

      //Read the info table, throws an IOException if there is an error
      checkInfoTable();

      mainFile=new File(rootDBEnvPath, "tuples.db");

      //Open the Guid -> Environment.Descriptor table
      mappingTable  = new Db(dbenv, 0);
      mappingTable.set_error_stream(System.err);
      mappingTable.set_errpfx("mappingTable for env rooted at " 
                              + rootDBEnvPath);

      mappingTable.open(mainFile.getCanonicalPath(), "mapping",
                        Db.DB_BTREE, Db.DB_CREATE | Db.DB_THREAD, 0644);

      //Open the Guid -> child Guid table
      childrenTable = new Db(dbenv, 0);
      childrenTable.set_error_stream(System.err);
      childrenTable.set_errpfx("childrenTable for env rooted at " 
                               + rootDBEnvPath);
      childrenTable.set_flags(Db.DB_DUP);
     
      childrenTable.open(mainFile.getCanonicalPath(), "children",
                         Db.DB_BTREE, Db.DB_CREATE|Db.DB_THREAD, 0644);

      dataPath=new File(rootDBEnvPath,"data");

      //Make the data subdirectory
      dataPath.mkdirs();
 
      //Remove any environments that were partially created when we
      //exited.
      removePartiallyCreated();

      //Delete empty directories below the data directory
      cleanupDirectories(dataPath);
      try {
        dbenv.txn_checkpoint(0,0,0);
        //dbenv.txn_checkpoint(0,0,Db.DB_FORCE);
      } catch (DbException x) {
        //Non-fatal, ignore.
      }
    } catch(DbException e) {
      errorCloseTable(mappingTable);
      mappingTable  = null;

      errorCloseTable(childrenTable);
      childrenTable = null;

      errorCloseEnv(dbenv);
      dbenv=null;
      throw new IOException("Can't open the database\n "+e);
    } catch(IOException e) {
      errorCloseTable(mappingTable);
      mappingTable  = null;

      errorCloseTable(childrenTable);
      childrenTable = null;

      errorCloseEnv(dbenv);
      dbenv=null;
      throw new IOException("Can't open the database\n "+e);
    }

      
    synchronized (TSTORE_LOCK) {
      if (state==STATE_INITIAL) {
        state=STATE_LOADED;
      } else {
        throw new Bug("State changed when it shouldn't in startUp:"+state);
      }
    }
  }

  /**
   * Shut down the berkeley DB system.
   *
   * <p> This closes all stores, the environment index files, and the Berkeley
   * DB env.</p>
   *
   * @throws SecurityException
   *                 Signals that the caller does not have permission
   *                 to manage environments.
   */

  public static void shutDown() {
    Environment.ensurePermission();
    int enteredState;
    synchronized (TSTORE_LOCK) {
      enteredState=state;
      state=STATE_CLOSED;
    }

    if (periodicTimerNotification != null) {
      periodicTimerNotification.cancel();
    }

    if (enteredState==STATE_RUNNING) {
      //Close all open tuple stores. 
      java.util.Iterator it;
      synchronized(TSTORE_LOCK) {
        Set entries = allStores.entrySet();
        it = entries.iterator();
      }
      while (it.hasNext()) {
        Map.Entry mentry  = (Map.Entry)it.next();
        ActualStore ts = (ActualStore)mentry.getValue();
        ts.revokeAll(true);    
      }
    } 

    if ((enteredState==STATE_LOADED) || (enteredState==STATE_RUNNING)) {
      try {
        mappingTable.close(0);
      } catch (DbException x) {
        log.logError(null,"Unable to close mappingTable in shutDown()",x);
      }

      try {
        childrenTable.close(0);
      } catch (DbException x) {
        log.logError(null,"Unable to close childrenTable in shutDown()",x);
      }

      try {
        dbenv.txn_checkpoint(0,0,Db.DB_FORCE);
        dbenv.txn_checkpoint(0,0,Db.DB_FORCE);
      } catch (DbException x) {
        log.logError(null,"Unable write checkpoint in shutDown()",x);
      }
 
 
      try {
        dbenv.close(0);
      } catch (DbException x) {
        log.logError(null,"Unable to close dbenv in shutDown()",x);
      }
      dbenv = null;
    }
  }

  // =========================================================
  //              Public Environment Management
  // =========================================================


  /**
   * Find the descriptor of the root environment.
   *
   * @return The descriptor belonging to the root environment.  null if 
   *         the descriptor does not exist.
   * @throws SecurityException
   *                 Signals that the caller does not have permission
   *                 to manage environments.
   */
  public static Environment.Descriptor rootDescriptor() {
    Environment.ensurePermission();
    int i;
    boolean foundDescriptor=false;
    int response;
    byte keyBytes[];
    Dbt key, data;
    Dbt dataDescriptor;

    //Make the zero byte string to use as a key to find the root
    keyBytes       = new byte[16];
    java.util.Arrays.fill(keyBytes,(byte)0);
    key            = new Dbt(keyBytes);
    key.set_flags(Db.DB_DBT_REALLOC);

    //Make a 16 byte data array to hold the looked up root guid 
    data           = new Dbt(new byte[16]);
    data.set_flags(Db.DB_DBT_REALLOC);

    //Make a data descriptor array to hold the looked up descriptor
    dataDescriptor = new Dbt();
    dataDescriptor.set_flags(Db.DB_DBT_MALLOC);
    
    DbTxn txn         = null;

    while (true) {
      try {
        txn=dbenv.txn_begin(null,0);

        //Find the root Guid
        response=childrenTable.get(txn,key,data,0);
        
        if (response!=Db.DB_NOTFOUND) {
          //Find the root Descriptor
          response=mappingTable.get(txn,data,dataDescriptor,0);
        } 

        txn.commit(0);
        foundDescriptor=(response!=Db.DB_NOTFOUND);
        break;
      } catch (DbDeadlockException x) {
        errorAbortTxn(txn);
        txn=null;
        continue;
      } catch (DbException x) {
        errorAbortTxn(txn);
        txn=null;
        log.logError(null,"Db exception in rootDescriptor()",x);
        throw new Bug("Unhandleable dbException in rootDescriptor()" +x);
      }
    }

    if (foundDescriptor) {
      return bytesToDescriptor(data.get_data(),
                               dataDescriptor.get_data(),
                               dataDescriptor.get_size());
    } else {
      return null;
    }
  }
 
  /**
   * Find the Descriptor of an environment given its guid.  
   *
   * @param g The guid of the environment to look up
   *
   * @return The descriptor for the environment with this Guid.  
   *         null if there is no such environment.
   * @throws SecurityException
   *                 Signals that the caller does not have permission
   *                 to manage environments.
   */
  public static Environment.Descriptor guidToDescriptor(Guid g) {
    Environment.ensurePermission();
    DbTxn txn=null;

    int response=Db.DB_NOTFOUND;

    Dbt key  = new Dbt(g.toBytes());
    key.set_flags(Db.DB_DBT_REALLOC);

    Dbt data = new Dbt();
    data.set_flags(Db.DB_DBT_MALLOC);

    //Only loop if it is locked.   
    while (true) {
      try {
        // Do the descriptor lookup
        txn=dbenv.txn_begin(null,0);
        response=mappingTable.get(null,key,data,0);
        txn.commit(0);
        if (response!=Db.DB_NOTFOUND) {
          // Found a descriptor
          return bytesToDescriptor(g.toBytes(),data.get_data(),data.get_size());
        } else {
          // No such descriptor
          return null;
        }
      } catch (DbDeadlockException x) {
        errorAbortTxn(txn);
        continue;
      } catch (DbException x) {
        errorAbortTxn(txn);
        log.logError(null,"DbException in guidToDescriptor()",x);
        throw new Bug("Unhandleable DbException in guidToDescriptor()" +x);
      } 
    } 
  }

  /**
   * Find the child environments of the specified environment.  
   *
   * @param g The environment who's children we are looking for.  
   * @return An array of the descriptors of all the children.  It
   *         will be a zero size array if there are no children.
   * @throws SecurityException
   *                 Signals that the caller does not have permission
   *                 to manage environments.
   *
   */
  public static Environment.Descriptor[] getChildren(Guid g) {
    Environment.ensurePermission();
    LinkedList descriptorList=null;
    int i;
    int response;
    Dbt keyParent,dataParent;
    Dbt dataDescriptor;
 
    //The guid key/child guid data for the Guid we are looking up
    //NOTE: berkeley DB doesn't like it if we have Db.DB_DBT_REALLOC set
    //      for the initial lookup, but does want it for the later lookups
   
    while(true) {
      DbTxn txn=null;

      //A cursor into this guid's children
      Dbc cursor=null;
 
      //Linked list to hold the child descriptors
      descriptorList=new LinkedList();

      try {
        keyParent  = new Dbt(g.toBytes());
        dataParent = new Dbt(new byte[16]);
        dataParent.set_flags(Db.DB_DBT_REALLOC);

        txn=dbenv.txn_begin(null,0);
        //Open a cursor into the child list
        cursor=childrenTable.cursor(txn,0);
        response=cursor.get(keyParent,dataParent,Db.DB_SET);
      
        dataDescriptor = new Dbt(new byte[16]);

        //Look at all the children
        while(response!=Db.DB_NOTFOUND) {
          Environment.Descriptor d;

          dataDescriptor.set_flags(Db.DB_DBT_REALLOC);

          //Get the child guid -> descriptor mapping
          response=mappingTable.get(txn,dataParent,
                                    dataDescriptor,0);

          if (response!=Db.DB_NOTFOUND) {
            d=bytesToDescriptor(dataParent.get_data(),
                                dataDescriptor.get_data(),
                                dataDescriptor.get_size());
            descriptorList.add(d);    
          } else {
            errorCloseCursor(cursor);
            errorAbortTxn(txn);

            log.logError(null,
                           "lookup of child returned NOTFOUND:" +
                           " should never happen");
            throw new Bug("lookup of child returned NOTFOUND:" +
                          " should never happen");
          }

          //Make berkeley DB happy
          keyParent.set_flags(Db.DB_DBT_REALLOC);
          //Get the next parent -> child guid mapping
          response=cursor.get(keyParent,dataParent,Db.DB_NEXT_DUP);
        }
        
        cursor.close();
        cursor=null;
        txn.commit(0);
        break;
      } catch (DbDeadlockException x) {
        errorAbortTxn(txn);
        txn=null;
        errorCloseCursor(cursor);
        cursor=null;
        continue;
      } catch (DbRunRecoveryException x) {
        errorCloseCursor(cursor);
        errorAbortTxn(txn);
        txn=null;
        log.logError(null,"Must run recovery on db ("+g+") in getChildren");

        throw new Bug("Must run recovery on db (" +g+") in getChildren");
      } catch (DbException x) {
        errorCloseCursor(cursor);
        errorAbortTxn(txn);
        txn=null;
        log.logError(null,"DbException in  ("+g+") in getChildren");
        throw new Bug("DbException on db (" +g+") in getChildren");
      }
    };

    //Make an array of children from our list of children
    java.util.Iterator it;
    Environment.Descriptor[] descriptors;

    descriptors = new Environment.Descriptor[descriptorList.size()];
    it          = descriptorList.iterator();
    i           = 0;
    while (it.hasNext()) {
      descriptors[i++] = (Environment.Descriptor)it.next();
    }
    return descriptors;
  }

  /**
   * Make an entry in the childrenTable saying that we are about to
   * create an environment.  If we crash before calling endCreate(),
   * the environment will be deleted the next time we initialize the
   * tuple store.  This is important so we don't restart with a 
   * partially restored clone.
   */
  private static void beginCreate(Environment.Descriptor newDescriptor) {
    DbTxn txn=null;

    while (true) {   
      try {
        /*Key with the new guid*/
        Dbt guidDbt      = new Dbt(newDescriptor.ident.toBytes());

        /*Key with the parent guid*/
        byte keyBytes[]  = new byte[] {0};
        Dbt key          = new Dbt(keyBytes);
        txn              = dbenv.txn_begin(null,0); 

        if ( 0!= childrenTable.put(txn,key,guidDbt,0)) { 
          log.logError(null,"Error inserting map in beginCreate");
          throw new Bug("Error inserting map in beginCreate");
        }
        txn.commit(0);
        return;
      } catch (DbDeadlockException x) {
        errorAbortTxn(txn);
        continue;
      } catch (DbException x) {
        errorAbortTxn(txn);
        log.logError(null,"Error inserting map in beginCreate "+x);
        throw new Bug("Error inserting map in beginCreate "+x);
      }
    }
  }

  /**
   * Remove the marker we put in the childrenTable by calling beginCreate.
   */
  private static void endCreate(DbTxn dbt,Environment.Descriptor newDescriptor)
                                                       throws DbException
{
    Dbt key,data;
    int ret;

    while(true) {
      byte keyBytes[] = new byte[] {0};
      key  = new Dbt(keyBytes);
      data = new Dbt(newDescriptor.ident.toBytes()); 
      Dbc iterator=null;

      try {
        iterator = childrenTable.cursor(dbt, 0);
  
        ret = iterator.get(key, data, Db.DB_GET_BOTH|Db.DB_RMW);
  
        if (ret!=Db.DB_NOTFOUND) {
          iterator.del(0); 
        }
        iterator.close();
        return;
      } catch (DbDeadlockException x) {
        errorCloseCursor(iterator);
        continue; 
      } catch (DbException x) {
        errorCloseCursor(iterator);
        throw x; 
      }
    }
  }
  /**
   * Called from startUp()
   * Remove any created databases that we weren't finished creating.
   * This is mainly important so that unfinished clones don't show up.
   *
   * NOTE: since we are called in startUp(), don't worry about 
   *       deadlock.
   */
  private static void removePartiallyCreated() 
  { 
    Dbc iterator=null;
    Dbt key,data;
    int ret;
    java.util.Iterator jIt;
    int i;
  
 
    try {
      //List of Guid's all the databases to delete
      LinkedList dbToDelete=new LinkedList();
      //After the list is made, make it an array for delete() 
      Guid toDelete[]; 

      //Find all databases to delete
      iterator = childrenTable.cursor(null, 0);
      key  = new Dbt(new byte[] {0});   
      key.set_flags(Db.DB_DBT_MALLOC);

      data = new Dbt(); 
      data.set_flags(Db.DB_DBT_MALLOC);

      ret = iterator.get(key, data, Db.DB_SET);
      
      while (ret!=Db.DB_NOTFOUND) {
        dbToDelete.add(new Guid(data.get_data()));
        
        key  = new Dbt();   
        key.set_flags(Db.DB_DBT_MALLOC);
        data = new Dbt(); 
        data.set_flags(Db.DB_DBT_MALLOC);
        ret  = iterator.get(key, data, Db.DB_NEXT_DUP);
      }
      iterator.close();

      if (dbToDelete.size()==0) {
        //No partially created stores, return.
        return;
      } else {
        log.log("one.world.io.TupleStore", "Detected a partial create");
      }
      jIt=dbToDelete.iterator();
      toDelete=new Guid[dbToDelete.size()];
      i=0;
      while (jIt.hasNext()) {
        toDelete[i]=(Guid)jIt.next();
        log.log("one.world.io.TupleStore", "Deleting " +toDelete[i]);
        i++;
      }
      //Delete the databases.  Don't signal an error
      //if one of the databases doesn't exist.
      doDelete(toDelete,false,false);

      //Run through the partially created databases again
      //to delete the markers
      iterator = childrenTable.cursor(null, 0);
      key  = new Dbt(new byte[] {0});   

      data = new Dbt(); 
      data.set_flags(Db.DB_DBT_MALLOC);

      ret = iterator.get(key, data, Db.DB_SET);
      
      while (ret!=Db.DB_NOTFOUND) {
        iterator.del(0);

        key  = new Dbt();   
        key.set_flags(Db.DB_DBT_MALLOC);
        data = new Dbt(); 
        data.set_flags(Db.DB_DBT_MALLOC);
        ret = iterator.get(key, data, Db.DB_NEXT_DUP);
      }
      iterator.close();
    } catch (DbException e) {
      errorCloseCursor(iterator);
      throw new Bug("Couldn't get next element " +e);
    } catch (IOException x) {
      throw new Bug("Should never happen " +x);
    }
  }

  /**
   * This does the actual tuplestore creation and adds it to the
   * indices.  This does NOT call beginCreate or endCreate(this is
   * the difference from create().
   *
   * <p>Throws an IOException if there is an error creating the environment.
   * This function currently does some sanity checks on its arguments, but
   * these checks are incomplete(by design) and should not be relied on.</p>
   *
   * @throws IOException Throws an IOException if there is an error creating
   *                     the database.  The environment hierarchy remains
   *                     as it was before the call.   
   * @throws SecurityException
   *                 Signals that the caller does not have permission
   *                 to manage environments.
   */
  static void internalCreate(Environment.Descriptor newDescriptor) 
         throws IOException {
    Environment.ensurePermission();
    Db newTable;
    byte parentIdBytes[];

    Guid parentId = newDescriptor.parent;
    Guid newId    = newDescriptor.ident; 
    /*Create the new berkeley db*/
    try {
      newTable = new Db(dbenv, 0);
      newTable.set_error_stream(System.err);
      newTable.set_errpfx("Environment DB");

      createDbPath(rootDBEnvPath,newId);
      newTable.open(
        (dbName(rootDBEnvPath, newId)).getCanonicalPath(), null,
        Db.DB_BTREE, Db.DB_CREATE|Db.DB_EXCL|Db.DB_THREAD, 0644);
      unlockDbPath(rootDBEnvPath,newId);
      newTable.close(0);

    } catch (DbException x) {
      log.logWarning(null, "Couldn't create new bdb database:"+newId, x);
      throw new IOException(x.toString());  
    }

    /*Create the links linking the new DB into our environment space*/
    try {
      byte[] d=descriptorToBytes(newDescriptor);
      createIndex(null,newId,d,d.length);
    } catch (DbException x) {
      log.logError(null,"DbException in createIndex",x);
      throw new Bug("DbException in createIndex"+x);
    }
  }

  /** 
   * Creates a new environment.
   *
   * <p>This creates a new environment with the given descriptor.</p>
   *
   * <p>Throws an IOException if there is an error creating the environment.
   * This function currently does some sanity checks on its arguments, but
   * these checks are incomplete(by design) and should not be relied on.</p>
   *
   * @throws IOException Throws an IOException if there is an error creating
   *                     the database.  The environment hierarchy remains
   *                     as it was before the call.   
   * @throws SecurityException
   *                 Signals that the caller does not have permission
   *                 to manage environments.
   */
  public static void create(Environment.Descriptor newDescriptor) 
         throws IOException {

    //Mark that we are trying to create a database.
    beginCreate(newDescriptor);
    internalCreate(newDescriptor);
    try {
      //mark that we have finished creating the database
      endCreate(null,newDescriptor);
    } catch (DbException x) {
      throw new Bug("Error: "+x);
    } 

    try {
      dbenv.txn_checkpoint(0,0,0);
    } catch (DbException x) {
      //non-fatal, ignore
    }
  }

  /**
   * Delete full trees of environments.  
   * 
   * <p>The ids can be from multiple subtrees, but they MUST contain each
   * element of the subtrees.</p>  
   * 
   * <p>NOTE: the array MUST contain every element in each subtree.
   *       If any parts of the tree are left out, there will be
   *       dangling entries in the index databases and dangling
   *       db files for the parts left out.</p>
   *
   * @param ids All of the environments to delete.
   *
   * @throws IOException Signals there as a problem deleting the
   *                     Environments.  The tuplestore remains
   *                     as it was before the call.
   * @throws SecurityException
   *                 Signals that the caller does not have permission
   *                 to manage environments.
   */
  public static void delete(Guid ids[]) throws IOException {
    doDelete(ids,true,false);  
  }

  /**
   * Delete full trees of environments.  
   * 
   * <p>The ids can be from multiple subtrees, but they MUST contain each
   * element of the subtrees.</p>  
   * 
   * <p>NOTE: the array MUST contain every element in each subtree.
   *       If any parts of the tree are left out, there will be
   *       dangling entries in the index databases and dangling
   *       db files for the parts left out.</p>
   *
   * @param ids All of the environments to delete.
   * @param warn If true, abort and throw exceptions for missing table
   *             entries/databases.  If false, don't abort and don't throw
   *             exceptions.               
   * @param isMove If true, this is a move so we shouldn't try to undbind.
   *
   * @throws IOException Signals there as a problem deleting the
   *                     Environments.  The tuplestore remains
   *                     as it was before the call.
   * @throws SecurityException
   *                 Signals that the caller does not have permission
   *                 to manage environments.
   */
  static void doDelete(Guid ids[],boolean warn,boolean isMove) 
                                                throws IOException {
    Environment.ensurePermission();

    int numId;


    //If we aren't moving, lock the environment.
    if (!isMove) {
      for (numId=0;numId<ids.length;numId++) {
        ActualStore ts = (ActualStore)allStores.get(ids[numId]);
        if (ts!=null) {
          if (ts.envState == ActualStore.ENVSTATE_READONLY) {
            throw new IOException("Environment busy");
          }
        } 
      }
    }

    //Delete the entries for each database
    while(true) {
      DbTxn txn = null;
      try {
        txn = dbenv.txn_begin(null,0);
        for (numId = 0; numId < ids.length; numId++) {
          delId(txn,ids[numId],false); 
        }
        txn.commit(0);
        break;
      } catch (DbDeadlockException x) {
        errorAbortTxn(txn);
        txn = null;
        continue;  
      } catch (DbException x) {
        errorAbortTxn(txn);
        txn = null;
        log.logError(null,"Unhandleable DbException in delete()",x);
        throw new Bug("Unhandleable DbException in delete()" +x);
      }
    } 

    if (!isMove) {
      //If we're not moving, revoke all binds to the stores.
      for (numId = 0; numId < ids.length; numId++) {
        ActualStore ts = (ActualStore)allStores.get(ids[numId]);
        if (ts != null) {
          ts.revokeAll(false);
        } 
      }
    } else {
      //If we're moving, remove each store from allStores
      for (numId = 0; numId < ids.length; numId++) {
        ActualStore ts = (ActualStore)allStores.get(ids[numId]);
        if (ts != null) {
          ts.finishMoving(false);
        } 
      }
    }
      //Delete each tuplestore
    for (numId = 0; numId < ids.length; numId++) {
      File name = dbName(rootDBEnvPath,ids[numId]); 
      try {
        ActualStore ts;

        synchronized(TSTORE_LOCK) {
          if (name.exists()) {
            Db table = new Db(dbenv,0);
            table.remove(name.getCanonicalPath(),null,0);
           try {
              dbenv.txn_checkpoint(0,0,0);
            } catch (DbException x) {
              //Non-fatal, ignore.
            }
          }

          ts = (ActualStore)allStores.get(ids[numId]);
          if (ts != null) {
            ts.removeMap();
          } 
        }

      } catch (DbException x){
        if (warn) {
          throw new Bug("Unable to delete environment "+x);
        }
      } catch (IOException x){
        if (warn) {
          throw new Bug("Unable to delete environment "+x);
        }
      }
    }
    for (numId = 0; numId < ids.length; numId++) {
      removePath(rootDBEnvPath,ids[numId]);
    }
  }

  /**
   * Move an environment to a new parent.
   *
   * @param id The Guid of the environment to move
   * @param newParent The Guid of the newParent of the environment
   * @throws SecurityException
   *                 Signals that the caller does not have permission
   *                 to manage environments.
   */
  public static void move(Guid id,Guid newParent) {
    Environment.ensurePermission();

    while(true) {
      DbTxn txn = null;
      byte[] descriptor;
      int size;
      int i;
      boolean found;
      int ret;

      try {
        Dbt dataDescriptor = new Dbt();
        dataDescriptor.set_flags(Db.DB_DBT_MALLOC);
    
        //Find the old this -> parent mapping
        Dbt keyId = new Dbt(id.toBytes());
        keyId.set_flags(Db.DB_DBT_REALLOC);  

        txn = dbenv.txn_begin(null,0);

        ret = mappingTable.get(txn,keyId,dataDescriptor,0);

        if (ret == Db.DB_NOTFOUND) {
          txn.commit(0);
          return;
        }
        descriptor = dataDescriptor.get_data();
        size       = dataDescriptor.get_size();

        //delete the current main entry and the parent -> this link 
        delId(txn,id,true);

        //Add a new main entry and parent -> this link
        System.arraycopy(newParent.toBytes(),0,descriptor,0,16);
        createIndex(txn,id,descriptor,size); 

        txn.commit(0);
        return;
      } catch (DbDeadlockException x) {
        errorAbortTxn(txn);
        txn = null;
        continue;
      } catch (DbRunRecoveryException x) {
        errorAbortTxn(txn);
        txn = null;
        log.logError(null,"RecoveryException in move",x);
        throw new Bug("RecoveryException in move()" +x);
      } catch (DbException x) {
        errorAbortTxn(txn);
        txn = null;
        log.logError(null,"DbException error in move",x);
        throw new Bug("Unhandleable DbException in move()" +x);
      }
    }
  }

  /**
   * Rename an environment
   *
   * @param id the id of the environment to rename
   * @param name the new name of the environment 
   * @throws SecurityException
   *                 Signals that the caller does not have permission
   *                 to manage environments.
   */
  public static void rename(Guid id,String name) {
    Environment.ensurePermission();
    DbTxn txn=null; 
    byte[] nameBytes;
    int response;
    Dbt key;
    Dbt dataDescriptor;

    while(true) {
      nameBytes=name.getBytes();

      try {
        txn            = dbenv.txn_begin(null,0);
        
        key            = new Dbt(id.toBytes());
        key.set_flags(Db.DB_DBT_REALLOC);

        dataDescriptor = new Dbt();
        dataDescriptor.set_flags(Db.DB_DBT_MALLOC);
        
        //The original descriptor for this guid
        response = mappingTable.get(txn,key,dataDescriptor,0);

        if (response != Db.DB_NOTFOUND) {
          //Create a new descriptor with the same parent/protection guids,
          //but a different name. 
          byte[] desc=new byte[32+nameBytes.length];

          System.arraycopy(dataDescriptor.get_data(),0,desc,0,32);
          System.arraycopy(nameBytes,0,desc,32,nameBytes.length);

          dataDescriptor=new Dbt(desc);

          //store the new descriptor(no duplicates, so erases the old one)
          mappingTable.put(txn,key,dataDescriptor,0);
          txn.commit(0);
          return;
        } else {
          txn.commit(0);
          return;
//          throw new Bug("No such environment in rename");
        }
      } catch (DbDeadlockException x) {
        errorAbortTxn(txn);
        txn=null;
        continue;
      } catch (DbRunRecoveryException x) {
        errorAbortTxn(txn);
        txn=null;
        log.logError(null,"RecoveryException in rename",x);
        throw new Bug("RecoveryException in rename()" +x);
      } catch (DbException x) {
        errorAbortTxn(txn);
        txn=null;
        log.logError(null,"DbException error in rename",x);
        throw new Bug("Unhandleable dbException in rename()" +x);
      }
    }
  }

  // ============================================================= 
  //               Internal initialization functions
  // ============================================================= 

  /**
   *  Opens the information table associated with the Berkeley DB environment,
   *  and creates it if it doesn't already exist.  
   *
   * <p> Currently this table only contains the file format version
   *     numbers.</p>
   *
   *  @throws IOException
   *          An IOException is thrown if the initialization failed.  
   *          The reasons for failure are an incorrect version number
   *          for the file format or an error reading or writing the
   *          table.
   */
  private static void checkInfoTable() throws IOException {
    Db storeInfoTable=null;
    Dbt majorKey, majorData;
    Dbt minorKey, minorData;
    File storeInfoFile;
    int response;

    //Transaction to assure atomic write of version data
    DbTxn txn=null;

    try {
      //Open the store information DB
      storeInfoTable = new Db(dbenv,0);
      storeInfoTable.set_error_stream(System.err);
      storeInfoTable.set_errpfx("Root DB for env " + rootDBEnvPath);
  
      //Open the table, creating it if it doesn't exist
      storeInfoFile=new File(rootDBEnvPath, "tuples.db");
      storeInfoTable.open(storeInfoFile.getCanonicalPath(), "info",
                          Db.DB_BTREE, Db.DB_CREATE|Db.DB_THREAD, 0644);
         
      //Keys and data for the minor/major version queries and puts
      majorKey  = new Dbt((byte[])MAJOR_VERSION_KEY.clone());
      majorKey.set_flags(Db.DB_DBT_REALLOC);
  
      majorData = new Dbt();
      majorData.set_flags(Db.DB_DBT_MALLOC);
  
      minorKey  = new Dbt((byte[])MINOR_VERSION_KEY.clone());
      minorKey.set_flags(Db.DB_DBT_REALLOC);
  
      minorData = new Dbt();
      minorData.set_flags(Db.DB_DBT_MALLOC);

      //Get the major version of the database(if it exists).
      response=storeInfoTable.get(null,majorKey,majorData,0);
 
      if (response==Db.DB_NOTFOUND) {
        //Ok, should be a new database
        log.logWarning(null,"WARNING: creating a log info file "+
                            "(hopefully a new store)");    

        //The version numbers will be the latest version numbers
        String majorString = Long.toString(LATEST_DISK_MAJOR_VERSION,10);
        String minorString = Long.toString(LATEST_DISK_MINOR_VERSION,10);

        majorData          = new Dbt(majorString.getBytes());
        minorData          = new Dbt(minorString.getBytes());

        diskMajor = LATEST_DISK_MAJOR_VERSION;
        diskMinor = LATEST_DISK_MINOR_VERSION;

        /*
         * Write the major/minor versions.
         * Don't worry about retrying this transaction: 
         * this is the only outstanding operation.
         */
        txn = dbenv.txn_begin(null,0);

        storeInfoTable.put(txn,majorKey,majorData,0);
        storeInfoTable.put(txn,minorKey,minorData,0);

        txn.commit(0);
      
      } else {
        //Ok, we have a major version, get the minor
        response=storeInfoTable.get(null,minorKey,minorData,0);

        if (response==Db.DB_NOTFOUND) {
          throw new IOException("Corrupted tuplestore: " +
                                "no minor version in info file");
        } else {
          //Find the minor and major numbers
          String minorString = new String(minorData.get_data(),0,
                                          minorData.get_size());  
          String majorString = new String(majorData.get_data(),0,
                                          majorData.get_size());
          diskMajor          = Long.parseLong(majorString);
          diskMinor          = Long.parseLong(minorString);
        }
      }
      storeInfoTable.close(0);   
    } catch (DbException x) {
      //We got some sort of error while opening the file.  

      errorAbortTxn(txn);
      errorCloseTable(storeInfoTable);

      throw new IOException("Error checking store info table("  + x + ")"); 
    }

    if (diskMajor!=LATEST_DISK_MAJOR_VERSION) {
      throw new IOException("Unrecognized disk version: " +
                            diskMajor + "." + diskMinor); 
    } else { 
      if (diskMinor == 0) {
        log.logWarning(null,"WARNING: you are using an old tuplestore." + 
                            "  Stored protection domains may be null");
      }
    }
  }


  // =======================================================
  //            Internal environment functions
  // =======================================================

  /**
   * Set up the necessary mappings after creating a new tuplestore.
   * This function adds parent to child and child to parent entries
   * in the mappingTable.
   *
   * @param txnIn Use this transaction.  If null, it uses its own transaction.
   * @param parentId the parent environment of the new environment.
   * @param newId the child environment of the new environment.
   * @param newName the non fully qualified name of the new environemnt.
   */
  private static void createIndex(DbTxn txnIn, Guid newId, 
                                  byte[] newDescriptor,int dlen) throws
                                  DbException,
                                  DbRunRecoveryException,
                                  DbDeadlockException {

    DbTxn txn         = null;
    int i;
    boolean externalTransaction;
 
    externalTransaction = txnIn!=null;
    
    /*Key with the new guid*/
    Dbt guidDbt            = new Dbt(newId.toBytes());
   
    /*Data for the new guid(parent and name)*/
    byte dataMainBytes[]   = new byte[dlen];
    System.arraycopy(newDescriptor,0,dataMainBytes,0,dlen);
    Dbt dataMain           = new Dbt(dataMainBytes);

    /*Key with the parent guid*/
    byte keyParentBytes[]  = new byte[16];
    System.arraycopy(newDescriptor,0,keyParentBytes,0,16);
    Dbt keyParent          = new Dbt(keyParentBytes);
  
    /*Do the transaction to add the store and the parent link*/
    while(true) {
      try {
        //If we weren't passed in a transaction, create one.
        if (externalTransaction) { 
          txn = txnIn;
        } else {
          txn = dbenv.txn_begin(null,0);
        }
 
        //Insert the Guid -> descriptor mapping*/
        if (0 != mappingTable.put(txn,guidDbt,dataMain,0)) { 
          errorAbortTxn(txn);

          log.logError(null,"Error inserting map in createIndex");
          throw new Bug("Error inserting map in createIndex");
        }

        //Insert the parent -> this guid mapping*/
        if ( 0!= childrenTable.put(txn,keyParent,guidDbt,0)) { 
          errorAbortTxn(txn);

          log.logError(null,"Error inserting map in createIndex");
          throw new Bug("Error inserting map in createIndex");
        } 

        //If we created the transaction, commit the transaction.
        if (!externalTransaction) {
          txn.commit(0);
        } 
        return;
      } catch (DbDeadlockException x) {
        if (!externalTransaction) {
          errorAbortTxn(txn);
          txn=null;
          continue;
        } else {
          throw x;
        }
      } catch (DbRunRecoveryException x) {
        if (!externalTransaction) {
          errorAbortTxn(txn);
          log.logError(null,"Recovery error in createIndex",x);
          throw new Bug("Must run recovery in createIndex"+x);
        } else { 
          throw x;
        }
      } catch (DbException x) {
        if (!externalTransaction) {
          errorAbortTxn(txn);
          log.logError(null,"DbException in createIndex",x);
          throw new Bug("DbException in createIndex"+x);
        } else {
          throw x;
        }
      }
    }
  }

  /**
   * Delete the information about the tuplestore from berkeley db.
   *  
   * <p>This function deletes the main info for a store as well as the
   * link to the store in the parent.  It does NOT delete the links
   * to the children of this store. </p>
   *
   * @param txn The transaction in which this delete is occuring.
   * @param id The Guid of the environment to delete.
   * @param warn If true, throw exceptions for missing entries. 
   *             If false, don't throw exceptions.
   */
  private static void delId(DbTxn txn,Guid id,boolean warn) 
                                       throws DbException {
    int i;
    int ret;
    boolean found;
    byte idBytes[]=id.toBytes();
    
    Dbt keyMain  = new Dbt((byte[])idBytes.clone());
    keyMain.set_flags(Db.DB_DBT_REALLOC);  
 
    //Will hold the descriptor of id 
    Dbt dataMain = new Dbt();
    dataMain.set_flags(Db.DB_DBT_MALLOC);

    /*Find the descriptor for this environment*/
    ret=mappingTable.get(txn,keyMain,dataMain,0);

    if (ret==Db.DB_NOTFOUND) {
      if (warn) {
        throw new Bug("Mapping not found");
      } else {
        return;
      } 
    }
    /*Delete the descriptor entry*/
    mappingTable.del(txn,keyMain,0);

    /*Make a key to the parent child list from the value in dataMain*/
    byte keyParentBytes[]  = new byte[16];
    System.arraycopy(dataMain.get_data(),0,keyParentBytes,0,16);
    Dbt keyParent          = new Dbt(keyParentBytes);
          
    byte dataParentBytes[] = new byte[16];
    Dbt dataParent         = new Dbt(dataParentBytes);
    dataParent.set_flags(Db.DB_DBT_REALLOC);

    /*Open a cursor into the child list of our parent*/
    Dbc cursor = childrenTable.cursor(txn,0);
    cursor.get(keyParent,dataParent,Db.DB_SET);
   
    /*Stop when we find the entry in our parent about us*/
    found=byteMatch(idBytes,dataParent.get_data(),16);
    while (!found) {
      int j;
      keyParent.set_flags(Db.DB_DBT_REALLOC);  
      if (Db.DB_NOTFOUND==cursor.get(keyParent,dataParent,Db.DB_NEXT_DUP)) { 
        cursor.close();
        if (warn) {
          throw new DbException(new String("Couldn't find child pointer"));
        } else {
          return;
        }
      }
      found=byteMatch(idBytes,dataParent.get_data(),16);
    }
    /*Delete the child entry in the parent*/
    cursor.del(0);

    cursor.close();
  }

  // =================================================================
  //                     Helper Functions
  // =================================================================

  /**
   * Find the fully qualified filename of the database for an 
   * environment with Guid g.
   *
   * @param root The path to the berkely db env directory.  Does NOT 
                 contain the final data directory path element
   * @param g The guid for the environment for which we are 
   *          calculating a name.
   * @return The fully qualified file name
   */

  private static File dbName(File root,Guid g) {
    int i;
    int curpos=0;
    File newpath;
    String filename=guidToHexString(g);

    curpos=0;
    newpath=new File(root,"data");

    for (i=0;i<DIR_SPLITS.length;i++) {
      newpath=new File(newpath,filename.substring(curpos,
                                                  curpos+DIR_SPLITS[i]));
      curpos+=DIR_SPLITS[i];
    }

    filename=filename+".db";
    
    newpath=new File(newpath,filename);
    return newpath;
  }

  /**
   * Create the directory structure necessary to store the 
   * environment database. This also makes a ".LOCK" file to
   * keep the directory from being cleaned up. 
   * 
   * @param root The path to the berkely db env directory.  Does 
   *             NOT contain the final data directory path element
   * @param g The guid for the environment for which we are making directries.
   */
  private static void createDbPath(File root,Guid g) throws IOException {
    int i;
    int curpos=0;
    File newpath,filepath;
    String filename=guidToHexString(g);

    newpath=new File(root,"data");
    for (i=0;i<DIR_SPLITS.length;i++) {
      newpath=new File(newpath,filename.substring(curpos,
                                                  curpos+DIR_SPLITS[i]));
      curpos+=DIR_SPLITS[i];
    }

    filepath=new File(newpath,filename+".LOCK");

    synchronized(PATH_LOCK) {
      if (false==newpath.mkdirs()) {
        throw new IOException("Unable to make directories for new db:"+newpath);
      }

      if (false==filepath.createNewFile()) {
        throw new IOException("Unable to create lock path.");
      }
    }
  }

  /**
   * Get rid of the ".LOCK" file that kept the directory from being 
   * cleaned up while we were making the database. 
   * 
   * @param root The path to the berkely db env directory.  Does 
   *             NOT contain the final data directory path element
   * @param g The guid for the environment for which we are making directries.
   */
  private static void unlockDbPath(File root,Guid g) {
    int i;
    File newpath;
    String filename;
    int curpos=0;
 
    filename = guidToHexString(g);
    newpath  = new File(root,"data");

    for (i=0;i<DIR_SPLITS.length;i++) {
      newpath=new File(newpath,filename.substring(curpos,
                                                  curpos+DIR_SPLITS[i]));
      curpos+=DIR_SPLITS[i];
    }

    newpath=new File(newpath,filename+".LOCK");

    synchronized(PATH_LOCK) {
      newpath.delete();
    }
  }


  /**
   * Get rid all of the(empty) directories for the database with this guid.
   * 
   * @param root The path to the berkely db env directory.  Does 
   *             NOT contain the final data directory path element
   * @param g The guid for the environment for which we are making directries.
   */
  private static void removePath(File root,Guid g) {
    int i;
    int j;
    File newpath[];
    String filename;
    int curpos=0;

    newpath  = new File[DIR_SPLITS.length+1];
    filename = guidToHexString(g);

    newpath[0] = new File(root,"data");


    for (i=0;i<DIR_SPLITS.length;i++) {
      newpath[i+1]=new File(newpath[i],filename.substring(curpos,
                                                  curpos+DIR_SPLITS[i]));
      curpos+=DIR_SPLITS[i];
    }

    synchronized(PATH_LOCK) {
      for (j=DIR_SPLITS.length;j>0;j--) {
        String name=newpath[j].getName();
        if (!newpath[j].isDirectory()) {
          return;
        } 
        newpath[j].delete();
      }
    }
  }



  /**
   * Convert a Descriptor to a byte array representation
   *
   * <p>Create the byte array that is stored in the berkeley db
   *    database.  The byte array hold the parent Guid, the
   *    protection Guid, and the name </p> 
   *
   *
   * @param d The descriptor to convert 
   * @return The new byte array
   */
  private static byte[] descriptorToBytes(Environment.Descriptor d) {
    byte[] nameBytes=d.name.getBytes();
    byte[] overall=new byte[32+nameBytes.length];
    byte[] protectionBytes=d.protection.toBytes();
   
    if (d.parent==null) {
      int i;
      for (i=0;i<16;i++) {
        overall[i]=0;
      }
    } else {
      System.arraycopy(d.parent.toBytes(),0,overall,0,16);
    }
    System.arraycopy(protectionBytes,0,overall,16,16);
    System.arraycopy(nameBytes,0,overall,32,nameBytes.length);
    
    return overall;
  };

   /**
   * Create a Descriptor from its byte array representation
   *
   * <p>Create the byte array that is stored in the berkeley db
   *    database.  The byte array hold the parent Guid, the
   *    Protection guid, and the name </p> 
   *
   *
   * @param identBytes The bytes of the Guid of the descriptor 
   *                   to convert 
   * @param b          The bytes of the rest of the byte array 
   *                   descriptor
   * @param blen       The length of the b
   *
   * @return The new byte array
   */

  private static Environment.Descriptor bytesToDescriptor(byte[] identBytes,
                                                          byte[] b,int blen) {
    Environment.Descriptor descriptor;
    int i;
    boolean validParent;
    Guid parent;

    //In case berkeley db gave us a long key
    if (identBytes.length!=16) {
      byte[] oldbytes;

      oldbytes    = identBytes;
      identBytes  = new byte[16];

      System.arraycopy(oldbytes,0,identBytes,0,16);
    }

    Guid ident  = new Guid(identBytes);
   
    //If all of the bytes are 0, then this is a marker for the 
    //parent of the root guid. 
    validParent = false;
    for (i = 0; i < 16; i++) {
      if (b[i] != 0) {
        validParent = true;
        break;
      }
    }

    if (validParent) {
      byte[] parentBytes;

      parentBytes = new byte[16];

      System.arraycopy(b,0,parentBytes,0,16);
      parent      = new Guid(parentBytes); 
    } else {
      //The all zero guid is replaced with a null guid.
      parent      = null;
    }

    //Make the protection Guid
    byte[] protectionBytes = new byte[16];
    System.arraycopy(b,16,protectionBytes,0,16);

    Guid protection        = new Guid(protectionBytes); 

    //Make the name string
    String name            = new String(b,32,blen-32);

    //The new Environment.Descriptor
    Environment.Descriptor returnedDescriptor = 
                new Environment.Descriptor(ident,name,parent,protection);
    return returnedDescriptor;
  }



  // =================================================================
  //                   General Helper Functions
  // =================================================================

  /**
   * Helper function to determine whether or not the prefix of two byte
   * arrays are equal.  Assumes both arrays have sufficient characters.
   *
   * @param b1 The first array
   * @param b2 The second array
   * @param len The length of the prefix to test
   * @return true if the first len characters of the arrays match
   */
  private static boolean byteMatch(byte b1[],byte b2[],int len) {
    int i;
    
    for (i = 0; i < len; i++) {
      if (b1[i] != b2[i]) {
        return false;
      }  
    }
    return true;
  }

  /**
   * Helper function to try aborting a transaction.  Don't throw an exception
   * if there is an error.  This is meant to be used when recovering from 
   * other exceptions.
   *
   * @param cursor A possibly executing(and possibly null) transaction that 
   *               we want to try to abort.
   */

  private static void errorAbortTxn(DbTxn txn) {
    try {
      if (txn != null) {
        txn.abort();
      }
    } catch (DbException dbe) {
      //Do nothing
    }
  }

  /**
   * Helper function to try closing a cursor.  Don't throw an exception
   * if there is an error.  This is meant to be used when recovering 
   * from other exceptions.
   *
   * @param cursor A possibly open(and possibly null) cursor that 
   *               we want to try to close.
   */
  private static void errorCloseCursor(Dbc cursor) {
    try {
      if (cursor != null) {
        cursor.close();
      }
    } catch (DbException dbe) {
      //Do nothing
    }
  }

  /**
   * Helper function to try closing a table.  Don't throw an exception
   * if there is an error.  This is meant to be used when recovering 
   * from other exceptions.
   *
   * @param table A possibly open(and possibly null) table that 
   *               we want to try to close.
   */
  private static void errorCloseTable(Db table) {
    try {
      if (table != null) {
        table.close(0);
      }
    } catch (DbException dbe) {
      //Do nothing
    }
  }
  /**
   * Helper function to try closing an env .  Don't throw an exception
   * if there is an error.  This is meant to be used when recovering 
   * from other exceptions.
   *
   * @param env A possibly open(and possibly null) env that 
   *               we want to try to close.
   */
  private static void errorCloseEnv(DbEnv env) {
    try {
      if (env != null) {
        env.close(0);
      }
    } catch (DbException dbe) {
      //Do nothing
    }
  }

  /**
   * Convert a guid to a hex representation without seperators.
   *
   */
  private static String guidToHexString(Guid g) {
    String s;
    long hi,lo;

    //We write over this, default to 0
    StringBuffer buf=new StringBuffer("00000000000000000000000000000000");

    s = Long.toHexString(g.getHigh());  
    buf.replace( 16 - s.length(), 16, s);

    s = Long.toHexString(g.getLow());  
    buf.replace(32 - s.length(), 32, s);

    return buf.toString();
  }

  /**
   * Recursively remove all empty subdirectories below root(after first
   * deleteing any .LOCK files in each directory).
   *
   * @param root The parent of the cleanup.  root is NOT deleted.
   * @return True if root is now empty
   */
  private static boolean cleanupDirectories(File root) {
    //Flag for whether to delete the directory.  
    //Should be redundant since delete() isn't supposed to
    //delete directories containing data, but...
    boolean isEmpty=true;
    int i;

    if (!root.isDirectory()) {
      throw new Bug("Invalid parameter for cleanupDirectories:"+root+
                    " is not a directory");
               
    }
    File[] subFiles=root.listFiles();
    for (i=0;i<subFiles.length;i++) {
      if (subFiles[i].isDirectory()) {
        boolean retval;
        retval=cleanupDirectories(subFiles[i]);
        if (retval) {
          retval=subFiles[i].delete();
          if (!retval) {
            isEmpty=false; 
          }
        } else {
          isEmpty=false;
        }
      } else {
        String name = subFiles[i].getName();
        if ((name.length()==(32+5)) && 
            (name.endsWith(".LOCK") || name.endsWith(".lock"))) {
          //Is an old .LOCK file
          subFiles[i].delete();
        } else {
          isEmpty=false;
        }
      }
    }
    return isEmpty;
  }


  // =============================================================
  //              Internal binding functions
  // =============================================================

  /**
   * Open the Berkeley DB database for the environment with Guid id.
   *
   * <p> Throws IOException if it is unable to open the DB.</p>
   *
   * @param id The Guid of the database to open
   * @return The berkeley db for that database
   */
  private static Db openGuidDB(Guid id) throws IOException {
    Db newTable;

    try { 
      newTable = new Db(dbenv, 0);
      newTable.set_error_stream(System.err);
      newTable.set_errpfx("Table for " + id);

      newTable.open(
           (dbName(rootDBEnvPath, id)).getCanonicalPath(), 
           null, Db.DB_BTREE, Db.DB_THREAD, 0644);
    } catch (DbException x) {
      throw new IOException("Unable to open DB");
    }
    return newTable;
  }


  /**
   * Get an event handler for the tuplestore of a particular one.world
   * environment.
   *
   * @param tsId The Guid of the environment we are opening.
   */
  private void bind(BindingRequest breq, Guid tsId) {
    ActualStore ts;
    ActualStore.Manager rm = null;
    boolean tryAgain;

    try {

      do { 
        //We may have to loop if the current store
        //is in the process of shutting down. 
  
        synchronized(TSTORE_LOCK) {
          tryAgain = false;
          ts       = (ActualStore)allStores.get(tsId);

          if (ts == null) {
            ts     = new ActualStore(rootEnv,tsId);
            allStores.put(tsId,ts);
          } 
        }
     
        try {
          rm = ts.newManager();
        } catch (ResourceRevokedException x) {
          tryAgain=true;
        }
      } while (tryAgain);

      //Store the person who requested this so we know who to
      //notify if we revoke it.
      rm.leaseRequestor=breq.source;
  
      leaseHandler.handle(new LeaseEvent(rm,breq,LeaseEvent.ACQUIRE,rm,
                                         breq.descriptor,
                                         breq.duration));
                                         
    } catch (IOException x) {
      (breq.source).handle(new ExceptionalEvent(bindHandler,breq.closure,
                           new UnknownResourceException("Unknown resource"))); 
      return;
    }

  }



  // ==============================================================
  // Cloning functions
  // Source
  // ==============================================================
  
  /**
   * Do an internal bind in preperation for cloning.
   *
   * @param tsId The Guid of the environment we are opening.
   */
  private static ActualStore.Manager cloneBind(Guid tsId) throws IOException {
    ActualStore ts;
    ActualStore.Manager rm = null;
    boolean tryAgain;

    //We may have to loop if the current store
    //is in the process of shutting down. 
    do { 
      synchronized(TSTORE_LOCK) {
        tryAgain = false;
        ts       = (ActualStore)allStores.get(tsId);

        if (ts == null) {
          try {
            ts = new ActualStore(rootEnv,tsId);
          } catch (IOException ioex) {
            throw new Bug("Unable to open ActualStore");
          } 
          allStores.put(tsId,ts);
        } 
      }

      try {
        rm = ts.newManager();
      } catch (ResourceRevokedException x) {
        tryAgain = true;
      } 
    } while (tryAgain);

    return rm;
  }

  /**
   * This is the closure returned by startMove.
   */
  private static class SourceMove {
    /** A guid to manager map for each of the stores being cloned */  
    java.util.HashMap managers;

    /** So we can close any iterators inadvertantly left open */
    java.util.HashSet javaIterators;

    /** True if a move, false if a copy */
    boolean isMove;


    /**
     * Create a new SourceMove object.
     *
     * @param isMove <code>true</code> if we are moving, 
     *               <code>false</code> if copying.
     * @param managers A hashmap of <code>Guid</guid> to Manager.
     */
    SourceMove(boolean isMove,java.util.HashMap managers) {
      this.managers = managers;
      this.isMove   = isMove;
 
      javaIterators = new java.util.HashSet();
    }
  }

 /**
   * This is the opaque closure returned by <code>startMove()</code>
   */
  private static class DestMove {
    /** A map of guid to berkeley db */
    public java.util.HashMap dbMap;

    /** The array of the destination environments */
    Environment.Descriptor[] envs;

    /** 
     * Create a new structure to hold new copy data
     *
     * @param envs The environments being copied.
     */
    public DestMove(Environment.Descriptor[] envs) {
      this.envs = (Environment.Descriptor[])envs.clone();

      dbMap     = new java.util.HashMap();
    }
  }

 /**
   * This is the opaque closure returned by <code>startCopy()</code>
   */
  private static class CopyClosure {
    //The closure returned by startMove
    public Object srcClosure;

    //The closure returned by startAccept
    public Object destClosure;
   
    public CopyClosure(Object srcClosure,Object destClosure) {
      this.srcClosure  = srcClosure;
      this.destClosure = destClosure;
    }
  }

  /**
   * Prepare a group of tuple stores to be copied or moved to another machine.
   *
   * <p>This locks all the tuple stores referenced and returns an opaque object
   * to keep track of the move.  </p> 
   *
   * <p>If we are copying, reads are still allowed to complete, 
   *    but writes and deletes are queued until the clone is completed</p>
   *
   * <p>If we are moving, all bindings are broken and are NOT automatically
   *    reattached even if the operation aborts.</p>
   *
   * @param environments An array of the environments to clone.  The ordering
   *                     of the environments must be the same as in
   *                     <code>startAccept()</code>
   * 
   * @param isMove <code>true</code> if we are moving the environments, false
   *        if we are copying.
   * @return An opaque object to be passed into other clone functions.
   * @throws IOException Thrown in the case of copies if we cannot read
   *                     some element(perhaps due to an ongoing move).  Thrown
   *                     for moves if we can't read the element or get a delete
   *                     lock on the element(perhaps due to an ongoing move or
   *                     copy). 
   *                     
   * @throws SecurityException
   *                 Signals that the caller does not have permission
   *                 to manage environments.
   */
  public static Object startMove(Environment.Descriptor[] environments,
                                     boolean isMove) throws IOException
  {
    Environment.ensurePermission();
    int i;
    SourceMove sm;
    ActualStore as;

    HashMap sourceManagers = new java.util.HashMap();

    try {
      for (i = 0; i < environments.length; i++) {
        ActualStore.Manager m = cloneBind(environments[i].ident);

        if (isMove) {
          //break all binds to the store
          as = m.getStore();
          as.startMoving();
        } else {
          //mark the store read only
          as = m.getStore();
          as.doReadOnly();
        }

        sourceManagers.put(environments[i].ident,m);
      }
    } catch (IOException x) {
      //Should only occur for moves
      if (isMove) {
        Map.Entry mentry;
        ActualStore.Manager m;

        //unlock the stores
        java.util.Iterator it = sourceManagers.entrySet().iterator();
        while(it.hasNext()) {
          mentry = (Map.Entry)it.next();
          m      = (ActualStore.Manager)mentry.getValue();
          as     = m.getStore();
          as.abortMoving();
        }
      } else {
        throw new Bug("Error: "+x);
      }
      throw x;
    }
  
    return new SourceMove(isMove,sourceManagers);
  } 


  /**
   * During a clone or move, return a Java iterator of the tuples in one 
   * of the source environments. 
   *
   * <p>The iterator returns <code>BinaryData</code> tuples where the
   * <code>data</code> field contains the serialized version of the tuple and
   * the <code>Guid</code> of the <code>BinaryData</code> tuple is the 
   * <code>Guid</code> of the tuple it contains.</p>
   *
   * @param envId The source clone environment
   * @param closure The opaque object from startMove()
   * @return A java iterator over the serialized tuples
   * @throws SecurityException
   *                 Signals that the caller does not have permission
   *                 to manage environments.
   */
  public static java.util.Iterator getTuples(Guid envId, Object closure) 
  {
    Environment.ensurePermission();

    SourceMove moveInfo;
    ActualStore.Manager m;
    ActualStore.Manager.JavaIterator j; 

    moveInfo   = (SourceMove)closure;
    m          = (ActualStore.Manager)moveInfo.managers.get(envId);
    j          = m.getJavaIterator();

    moveInfo.javaIterators.add(j);
    return j; 
  }

  /**
   * Commit a store move or copy at the source side.  
   *
   * <p>This frees any resources required to maintain state for
   * the move.</p>
   *
   * <p>If this was a copy operation, any pending writes and deletes 
   * are executed on the source.</p>
   *
   * <p>If this was a move operation, the source tuplestores are 
   *    deleted. </p>
   * 
   * @param closure The object returned by <code>startMove()</code>.
   * @throws SecurityException
   *                 Signals that the caller does not have permission
   *                 to manage environments.
   */
  public static void commitMove(Object closure) {
    Environment.ensurePermission();

    Guid glist[];
    int glistEntry;

    SourceMove moveInfo;
    Set entrySet;
    java.util.Iterator it;

    moveInfo   = (SourceMove)closure;
    entrySet   = moveInfo.managers.entrySet();
    it         = entrySet.iterator();

    glist      = new Guid[entrySet.size()];
    glistEntry = 0;
   
    while (it.hasNext()) {
      java.util.Map.Entry mentry;
      ActualStore.Manager m;

      mentry = (java.util.Map.Entry)it.next();
      m      = (ActualStore.Manager)mentry.getValue();

      if (moveInfo.isMove) {
        glist[glistEntry] = (Guid)mentry.getKey();
      } else  { 
        m.getStore().doStopReadOnly();
        m.revoke(false);
      }

      glistEntry++;
    }

    //Delete all the source databases
    if (moveInfo.isMove) {
      try {
        doDelete(glist,true,true);
      } catch (IOException iox) {
        throw new Bug("Error deleting after move" +iox);
      }
    }  
  }

  /**
   * Abort a store move or copy at the source side.  
   *
   * <p>This frees any resources required to maintain state for
   * the move.</p>
   *
   * <p>If this was a copy operation, any pending writes and deletes 
   * are executed on the source.</p>
   *
   * <p>If this was a move operation, the source tuplestores are 
   *    as they were before the move, however binds are not 
   *    restored </p>
   *
   * @param closure The object returned by <code>startMove()</code>.
   * @throws SecurityException
   *                 Signals that the caller does not have permission
   *                 to manage environments.
   */

  public static void abortMove(Object closure) {
    Environment.ensurePermission();

    ActualStore as; 
    SourceMove moveInfo;
    Set entrySet;
    java.util.Iterator it;

    moveInfo  = (SourceMove)closure;
    entrySet = moveInfo.managers.entrySet();
    it       = entrySet.iterator();

    while (it.hasNext()) {
      java.util.Map.Entry mentry;
      ActualStore.Manager m;

      mentry = (java.util.Map.Entry)it.next();
      m      = (ActualStore.Manager)mentry.getValue();

      if (moveInfo.isMove) {
        as = m.getStore();
        as.abortMoving();
      } else {
        m.getStore().doStopReadOnly();
        m.revoke(false);
      }
    }  
  }



  // ==============================================================
  // Cloning functions
  // Destination 
  // ==============================================================

  /**
   * Initialize a store move or copy on the destination machine. 
   *
   * <p> The environments in the list will be created by this function and
   *     should NOT already exist</p>
   *
   * @param environments An array of the environments to create.The order of
   * the environments must be the same as in the <code>startMove()</code>
   * call on the server.  Additionally, the parent pointer of the Descriptor
   * for the head of the destination hierarchy must point at the desired parent\
   * of the new tree.
   *
   * @return An opaque object to be passed into other clone functions.
   * @throws IOException If it could not complete.  The tuplestore
   *                     is unchanged. 
   * @throws SecurityException
   *                 Signals that the caller does not have permission
   *                 to manage environments.
   */
  public static Object startAccept(Environment.Descriptor[] environments) 
                                                            throws IOException {
    Environment.ensurePermission();
    int i;
    DestMove dm;
    Db db;

    dm = new DestMove(environments);

    for (i = 0; i < environments.length; i++) {
      //Mark that we are beginning a create
      beginCreate(environments[i]);

      //Create the new tuplestore
      create(environments[i]);
      //Open the new tuplestore
      db = openGuidDB(environments[i].ident);
      dm.dbMap.put(environments[i].ident,db); 
    }
    return dm;    
  }
 
  /**
   * Write a tuple into the destination environment of a move or clone.  
   * 
   * @param env the destination environment
   * @param t   the serialized tuple being written
   * @param closure the opaque closure returned by <code>startAccept()</code>
   * @throws IOException There was an error writing.  The tuplestore 
   *                     is unchanged.  You still must call
   *                     <code>abortAccept()</code> on the destination. 
   * @throws SecurityException
   *                 Signals that the caller does not have permission
   *                 to manage environments.
   */
  public static void writeTuple(Guid env, BinaryData t, Object closure)
                                           throws IOException {
    Environment.ensurePermission();
    DestMove moveInfo;
    DbTxn txn = null;

    moveInfo  = (DestMove)closure;
    Db db     = (Db)moveInfo.dbMap.get(env);
    while (true) {
      Dbt key   = new Dbt(t.id.toBytes());   
      Dbt data  = new Dbt(t.data);
 
      try {
        txn = dbenv.txn_begin(null,Db.DB_TXN_NOSYNC);
        db.put(txn,key,data,0); 
        txn.commit(Db.DB_TXN_NOSYNC);
        return;
      } catch (DbDeadlockException x) {
        errorAbortTxn(txn);
        continue;
      } catch (DbException dbx) {
        errorAbortTxn(txn);
        throw new Bug("put failed"+dbx);
      }
    }
  }

  /**
   * Commit a store move or copy at the destination side.
   * 
   * <p>After this completes, the destination stores may be used as normal</p>
   *
   * @param closure The opaque object called by startAccept
   * 
   * @throws SecurityException
   *                 Signals that the caller does not have permission
   *                 to manage environments.
   */
  public static void commitAccept(Object closure)  {
    Environment.ensurePermission();

    int i;
    DestMove moveInfo;
    Db db;

    moveInfo  = (DestMove)closure;
    DbTxn txn = null;

    for (i = 0; i < moveInfo.envs.length; i++) {
      db=(Db)moveInfo.dbMap.get(moveInfo.envs[i].ident); 

      if (db!=null) {
        try {
          db.close(0);
        } catch (DbException ex) {
          throw new Bug("received DbException "+ex);
        }
      }
    }

    try {
      while (true) {
        txn=dbenv.txn_begin(null,Db.DB_TXN_NOSYNC);

        try {
          for (i=0;i<moveInfo.envs.length;i++) {
            endCreate(txn,moveInfo.envs[i]);
          }
          txn.commit(Db.DB_TXN_NOSYNC);
          dbenv.log_flush(null);
          return;
        } catch (DbDeadlockException x) {
          errorAbortTxn(txn);
        }
      }
    } catch (DbException x) {
      throw new Bug("Error: "+x);
    }
  }

  /**
   * Abort a store move or copy at the destination side.
   *
   * <p>This removes all effects of <code>startAccept()</code>.  All created 
   * tuplestores are removed from the destinateion. </p>
   *
   * @param closure The opaque object called by <code>startAccept()</code>.
   * @throws SecurityException
   *                 Signals that the caller does not have permission
   *                 to manage environments.
   */
  public static void abortAccept(Object closure) {
    Environment.ensurePermission();

    int i;
    DestMove moveInfo;
    Guid ids[];
    Db db;

    moveInfo = (DestMove)closure;
    ids      = new Guid[moveInfo.envs.length];

    for (i = 0; i<moveInfo.envs.length; i++) {
      ids[i] = moveInfo.envs[i].ident;
      db     = (Db)moveInfo.dbMap.get(moveInfo.envs[i].ident); 

      if (db != null) {
        try {
          db.close(0);
        } catch (DbException x) {
          throw new Bug("received DbException " + x);
        }
      }
    }

    try {
      tsf.doDelete(ids,false,false);
    } catch (IOException x) {
    }

    //Ok if we crash partway through this list.  
    //We'll complete it on restart.
    for (i = 0; i < moveInfo.envs.length; i++) {
      try { 
        endCreate(null,moveInfo.envs[i]);
      } catch (DbException x) {
        throw new Bug("error: "+x);
      }
    }
  }

  /**
   * Make a local copy of an environment hierarchy. 
   *
   * <p>This is meant to allow any possible local optimizations
   *    over a normal machine to machine copy using 
   *    <code>startMove()</code> and <code>startAccept()</code> 
   *
   * <p>The destination environments are created and the source environments
   * are copied into the new destination environments.  The source environments
   * queue all write and delete requests while the clone is in progress to
   * insure a consistent clone.</p>
   *
   * <p> This function performs most all of the work of the copy, but 
   *     the results aren't visible until the commitCopy() </p>
   * 
   *
   * @param src An array of source environments
   * @param dest An array of destination environments
   * @throws IOException An error occured.  The copy was not performed, the
   *                     source stores are as the were and the destinations are
   *                     deleted. 
   * @throws SecurityException
   *                 Signals that the caller does not have permission
   *                 to manage environments.
   */

  public static Object startCopy(Environment.Descriptor[] src,
                                 Environment.Descriptor[] dest) 
                                           throws IOException {
    Environment.ensurePermission();

    int i;     
    Object srcClosure, destClosure;

    srcClosure  = startMove(src,false);

    try {
      destClosure = startAccept(dest);
    } catch (IOException x) {
      abortMove(srcClosure);
      throw x;
    } 

    try {
      for (i=0;i<src.length;i++) {
        java.util.Iterator it;
        it = getTuples(src[i].ident,srcClosure);
        while (it.hasNext()) {
          BinaryData data;
          data=(BinaryData)it.next(); 
          writeTuple(dest[i].ident,data,destClosure); 
        }
      }
    } catch (IOException x) {
      abortAccept(destClosure);
      abortMove(srcClosure);
      throw x;
    }
    return new CopyClosure(srcClosure,destClosure);
  }

  /** 
   * Commit a local copy started with <code>startCopy()</code>.
   *
   * <p>After calling this function, the copy is complete </p>
   * @param closure The opaque closure returned by <code>startCopy()</code>
   * @throws SecurityException
   *                 Signals that the caller does not have permission
   *                 to manage environments.
   */
  public static void commitCopy(Object closure) {
    Environment.ensurePermission();
    CopyClosure cc = (CopyClosure)closure;

    commitAccept(cc.destClosure);
    commitMove(cc.srcClosure);
  }

  /** 
   * Abort a local copy started with <code>startCopy()</code>.
   *
   * <p>The stores will be as they were before the
   * <code>startCopy()</code>.</p>
   *
   * @param closure The opaque closure returned by <code>startCopy()</code>
   * @throws SecurityException
   *                 Signals that the caller does not have permission
   *                 to manage environments.
   */
  public static void abortCopy(Object closure) {
    Environment.ensurePermission();

    CopyClosure cc = (CopyClosure)closure;

    abortMove(cc.srcClosure);
    abortAccept(cc.destClosure);
  }

  // ==============================================================
  //               The binding request handler
  // ==============================================================
  // =======================================================================
   //                           The bind handler
  // =======================================================================

  /** The bind exported event handler. */
  private final class BindHandler extends AbstractHandler {
    boolean isFirst = true;

    public boolean handle1(Event event) {
      //HACK: need to start a timer after the environment starts, but
      //I receive no notification of this.  For now start when we get
      //the first binding request.
      if (isFirst) {
        boolean doFirst;
        synchronized(TSTORE_LOCK) {
          doFirst = isFirst;
          isFirst = false;
        }
        if (doFirst) {
          startLogTrim();
        }
      }
      if (event instanceof BindingRequest) {
        BindingRequest breq;

        breq = (BindingRequest)event;

        if (breq.descriptor instanceof SioResource) {
          SioResource sio;

          sio = (SioResource)breq.descriptor;

          if (sio.ident!=null) {
            bind(breq,sio.ident);
          } else {
            Exception x = new BindingException("No guid supplied");
            respond(breq,new ExceptionalEvent(this,null,x));
          }
        } else {
          Exception x = new BindingException("Unknown descriptor type");
          respond(breq,new ExceptionalEvent(this,null,x));
        } 

        return true; 
      } else {
        return false;
      }
    }
  }
  // =====================================================================
  //              The tuple store backing for an Environment 
  // =====================================================================

  static class ActualStore extends Component
  {

    /** The tuplestore is open and accepting operations */
    static final int ENVSTATE_OPEN=3;

    /** The tuplestore is in the process of closing */
    static final int ENVSTATE_CLOSING=1;

    /** The tuplestore is closed */
    static final int ENVSTATE_CLOSED=2;

    /** 
     * The tuplestore is open and accepting operations, but is queueing outputs
     */
    static final int ENVSTATE_READONLY=5;

    /** 
     * The tuplestore is open and accepting operations, but is queueing outputs
     */
    static final int ENVSTATE_MOVING=6;


    /** The state of this environment */
    int envState;

    /** All of the resource managers using this environment */
    Set managers;

    /** The environment guid of this environment */ 
    private Guid thisGuid;

    /** 
     * The number of input/output requests currently executing
     * in the tuple store.  Modified with doEnterXXX(), doExitXXX().  
     */
    private long outstanding = 0; 

    /**
     * The number of outstanding output requests.
     */
    private long writing = 0;

    /**
     * The number of clones causing the store to be marked readonly.
     */
    private long readonly = 0;


    /** Lock for this environment */
    private final Object LOCAL_LOCK = new Object();

    /** Pending input request query subscription handler */
    private ImportedDescriptor INPUT_REQUEST_HANDLER =
      new ImportedDescriptor("input request handler",
                             "keeps track of handling input requests",
                             new Class [] { InputRequest.class },
                             new Class [] {},
                             false,
                             false);

    /** Pending input requests tuple match handler*/
    private ImportedDescriptor TUPLE_HANDLER = 
      new ImportedDescriptor("tuple handler",
                             "Before a tuples is stored one"
                             + " checks for a match here",
                             new Class[] { TupleEvent.class },
                             new Class [] {},
                             false,
                             false);

    /** Accepts input requests so new arrivals may be checked*/
    private final Component.Importer inputRequestHandler;

    /** Checks new arrivals against list of active queries*/
    private final Component.Importer tupleHandler;


    /** The database for this TupleStore */
    private Db table;

    /**
     * A list of outstanding write/delete requests when we
     * are in read only mode.
     */
    java.util.LinkedList queuedList = new java.util.LinkedList(); 

    /**
     * A queued read or write while in read only mode.
     */
    private static class QueuedOperation {
      Manager manager;
      Event ev;

      QueuedOperation(Manager manager,Event ev) {
        this.manager=manager;
        this.ev=ev;  
      } 
    }



    /**
     * Start a read operation on the store  
     *
     * <p> Check to see whether the tuplestore is open.  If not, throw an
     * exception.  If yes, bump the outstanding counter to hold it open. </p>
     *
     * <p> If doEnter() throws an exception, do NOT call doExit() </p>
     *
     * @throws ResourceRevokedException Thrown if the tuplestore is not open.
     */
    private final void doEnterRead() throws ResourceRevokedException,
                                            IOException {
      synchronized (LOCAL_LOCK) {
        switch(envState) {
          case ENVSTATE_OPEN:
          case ENVSTATE_READONLY:
            outstanding++;
            break;

          case ENVSTATE_MOVING:
            throw new IOException("Environment moving");

          default:
            throw new ResourceRevokedException();
        }
      }
    }

    /**
     * Start a write operation on the store  
     *
     * <p> Check to see whether the tuplestore is open for writing.  
     * If the tuplestore is closed, throw an exception.   If it is open
     * only for reads, queue the write. If it is open for read and write, bump
     * the outstanding and writing counters to hold it open for a write. </p>
     *
     * <p> If doEnterWrite() throws an exception, do NOT call doExit() </p>
     * <p> If doEnterWrite() returns false, do NOT call doExit() </p>
     *
     * @param m The manager the write is being executed on.
     * @param ev The event being executed.
     * @return True if successful.  False if the event was queued.
     * @throws ResourceRevokedException Thrown if the tuplestore is not open.
     */
     private final boolean doEnterWrite(Manager m,Event ev) 
                                           throws ResourceRevokedException,
                                                  IOException {

      synchronized (LOCAL_LOCK) {
        switch (envState) {
          case ENVSTATE_OPEN:
            outstanding++;
            writing++;
            return true;
          
          case ENVSTATE_READONLY:
            QueuedOperation qop=new QueuedOperation(m,ev);
            queuedList.addLast(qop);
            return false;

          case ENVSTATE_MOVING:
            throw new IOException("Environment moving");

          default:
            throw new ResourceRevokedException();
        }    
      }
    }
    /**
     * Convert the store to Read Only mode for a clone.
     */
    protected final void doReadOnly() {
      boolean tryAgain;

      synchronized(LOCAL_LOCK) {
        switch(envState) {
          case ENVSTATE_OPEN:
            envState=ENVSTATE_READONLY; 
            readonly++;
            break;

          case ENVSTATE_READONLY:
            readonly++;
            break;

          default:
            throw new Bug("Error: bad state in doReadOnly");
        }

        if (writing!=0) {
          tryAgain=true;
        } else {
          tryAgain=false;
        }
      }

      //Wait till all writers have departed
      while(tryAgain) {
        synchronized(LOCAL_LOCK) {
          switch(envState) {
            case ENVSTATE_READONLY:
              if (writing!=0) {
                tryAgain=true;
              } else {
                tryAgain=false;
              }
              break;
            default:
              readonly--;
              throw new Bug("Error: bad state in doReadOnly");
          }
        }
      }
    }


    /**
     *  End an operation on the store 
     *
     * <p> Ends an operation started using doEnter() by decrementing the
     *     outstanding counter </p>
     */
    private final void doExitRead() {
      synchronized (LOCAL_LOCK) {
        outstanding--;
      }
    }

    /**
     * End a write operation on the store 
     *
     * <p> Ends an operation started using doEnterWrite() by decrementing the
     *     outstanding and writing counters </p>
     */
    private final void doExitWrite() {
      synchronized (LOCAL_LOCK) {
        outstanding--;
        writing--;
      }
    }

    /**
     * End a Read Only operation on the store 
     *
     * <p> Ends an operation started using doReadOnly() by decrementing the
     *     readonly counter.  If the counter reaches 0, the store is
     *     marked writeable and queued events are executed </p>
     *
     */
    public final void doStopReadOnly() {
      QueuedOperation qop;
      java.util.LinkedList internalList = null;
      LinkedList newList;

      //We'll probably need it, so make it outside the lock.
      newList = new LinkedList();

      synchronized (LOCAL_LOCK) {
        readonly--;
        if (readonly==0) {
          envState=ENVSTATE_OPEN;          
          internalList = queuedList; 
          queuedList   = newList;
        }
      }

      if (internalList!=null) {
        while(true) {
          if (internalList.size()==0) {
            break;
          } else {
            qop = (QueuedOperation)internalList.removeFirst();  
          }      

          //FIXME:should run in a different thread?.
          qop.manager.handle(qop.ev);
        }
      }
    }

    /** Descriptor for the sio handler */
    private static final ComponentDescriptor TS_SELF = 
      new ComponentDescriptor("one.world.io.TupleStore.ActualStore",
                              "Tuple store handler",
                              true);

    public ComponentDescriptor getDescriptor() {
      return TS_SELF;
    }

    /**
     * Revoke all of the resource managers currently running on 
     * this environment. 
     *
     */
    public void revokeAll(boolean removeMap) {
      int i;
      boolean amClosing = false;
       

      synchronized(LOCAL_LOCK) {
        if ((envState == ENVSTATE_OPEN) ||
            (envState == ENVSTATE_READONLY)) {
          envState = ENVSTATE_CLOSING;
          amClosing=true;
        }     
      } 

   
     
      if (amClosing) {
        Object allManager[];
    
        allManager = managers.toArray();
        
        for (i = 0; i < allManager.length; i++) { 
          Manager rm = (Manager)allManager[i];
          rm.revoke(false);
        }

        do {
          //Don't leave till all operations have finished
          synchronized(LOCAL_LOCK) {
            if (outstanding==0)
              break;
          }
          delayThread(1);
        } while(true);

        doClose();
        if (removeMap) {
          synchronized(TSTORE_LOCK) {
            allStores.remove(thisGuid);     
          }
        }
        synchronized(LOCAL_LOCK) {
          envState=ENVSTATE_CLOSED;
        }
      } else {
        do {
          //Don't leave till all operations have finished
          synchronized(LOCAL_LOCK) {
            if (envState==ENVSTATE_CLOSED)
              break;
          }
          delayThread(1);
        } while(true);
      }
    } 


    /**
     * Revoke all of the resource managers currently running on 
     * this environment and mark it as MOVING. 
     *
     */
    public void startMoving() throws IOException {
      boolean amClosing;
      Object allManager[];
      int i;
      
      synchronized(LOCAL_LOCK) {
        if (envState == ENVSTATE_OPEN) {
          envState=ENVSTATE_MOVING;
        } else {
          throw new IOException("Environment busy");
        }     
      } 

      allManager = managers.toArray();
        
      for (i = 0; i < allManager.length; i++) { 
        Manager rm = (Manager)allManager[i];
        rm.revoke(false);
      }

      do {
        synchronized(LOCAL_LOCK) {
          if (outstanding==0)
            break;
        }
        delayThread(1);
      } while(true);
    } 

    /**
     * Abort an attempted move.  Close the database.
     */
    public void abortMoving() {
      doClose();
      synchronized(TSTORE_LOCK) {
        allStores.remove(thisGuid);     
      }
      synchronized(LOCAL_LOCK) {
        envState=ENVSTATE_CLOSED;
      }
    } 
 
    /**
     * Finish a move.  Close the database.
     */
    public void finishMoving(boolean removeMap) {
      doClose();
      if (removeMap) {
        synchronized(TSTORE_LOCK) {
          allStores.remove(thisGuid);     
        }
      }
      synchronized(LOCAL_LOCK) {
        envState=ENVSTATE_CLOSED;
      }
    } 
 
    /**
     * Remove the store from the map.
     */
    public void removeMap() {
      synchronized(TSTORE_LOCK) {
        allStores.remove(thisGuid);     
      }
    } 
 

    /**
     * Get the Tuple with the specified Guid.  
     *
     * @param g The Guid of the Tuple to retrieve
     * @return The tuple with this guid.  null if no such tuple
     */  
    Tuple getTupleGuid(Guid g) {
      int response;
      DbTxn txn=null;
 
      while (true) {
        try {
          Dbt key = new Dbt(g.toBytes());   
          key.set_flags(Db.DB_DBT_REALLOC);
        
          Dbt data = new Dbt(); 
          data.set_flags(Db.DB_DBT_MALLOC);

          txn=dbenv.txn_begin(null,0);
          response=table.get(txn,key,data,0);
          txn.commit(0);

          if (response==Db.DB_NOTFOUND) {
            return null;         
          } else {
            ObjectInputStream ois = 
              new ObjectInputStream(new ByteArrayInputStream(data.get_data()));
            try {
              Tuple t = (Tuple)ois.readObject();
              return t;
            } catch (ClassNotFoundException e) {
              Guid newGuid;
              byte[] thisKey=key.get_data();

              if (thisKey.length == 16) {
                newGuid=new Guid(thisKey);
              } else {
                byte[] newGuidBytes = new byte[16];
          
                System.arraycopy(thisKey,0,newGuidBytes,0,16);
                newGuid = new Guid(newGuidBytes);
              }

              log.logWarning(null,"Class not found for item in store. Guid: "
                                  + newGuid+", Exception: ",e);
              return null;
            } catch (Throwable x) {
              log.logWarning(null,"Exception while deserializing from store.",
                                   x);
              return null; 
            }
          }
        } catch (DbDeadlockException x) {
          errorAbortTxn(txn);
          continue;
        } catch (DbException x) {
          errorAbortTxn(txn);
          log.logError(null,"DbException in guidToDescriptor()",x);
          throw new Bug("Unhandleable DbException in getTupleGuid()" +x);
        } catch (IOException e) {
          errorAbortTxn(txn);
          throw new Bug("stream threw " + e);
        }
      }
    }

    /**
     * Get any tuple matching the specified filter.  
     *
     * @param filter The tuple filter to match against
     * @param idOnly if true, return only the id of the matching tuple
     * @return The matching tuple if idOnly is false.  The Guid of the
     *         matching tuple if idOnly is true.  null if there is no
     *         matching tuple.
     */  
    Object getTuple(TupleFilter filter,boolean idOnly) {
      Tuple retrievedTuple;
      DbTxn txn=null;
      int cnt;
      boolean isFirst;
      if (filter.matchGuid!=null) {
        if (idOnly) {
          retrievedTuple = getTupleGuid(filter.matchGuid);

          if ((retrievedTuple == null) || 
              (!filter.check(retrievedTuple))) {
            return null;
          } else {
            return retrievedTuple.id;
          }
        } else {             
          retrievedTuple = getTupleGuid(filter.matchGuid);
          if ((retrievedTuple == null) || 
              (!filter.check(retrievedTuple))) {
            return null;
          } else {
            return retrievedTuple;
          }
        }
      }

      Dbc iterator = null;
      while(true) {
        try {
          txn=dbenv.txn_begin(null,0);
          iterator = table.cursor(txn, 0); 
          isFirst  = false;
          Dbt data = new Dbt(); 
          Dbt key = new Dbt(); 
          while (true) {
            data.set_flags(Db.DB_DBT_MALLOC);
      
            key.set_flags(Db.DB_DBT_MALLOC);  
      
  
            int ret;
            if (isFirst) {
              ret = iterator.get(key, data, Db.DB_FIRST);
              isFirst=false;
            } else {
              ret = iterator.get(key, data, Db.DB_NEXT);
            }  
            if (ret == Db.DB_NOTFOUND) {
              iterator.close();
              txn.abort();

              return null;
            }
                 
            ObjectInputStream ois = 
              new ObjectInputStream(new ByteArrayInputStream(data.get_data()));

            try {
              retrievedTuple = (Tuple)ois.readObject();

              if (filter.check(retrievedTuple)) {
                iterator.close();
                txn.commit(0);
                if (idOnly) {
                  return retrievedTuple.id;
                } else {
                  return retrievedTuple;
                }
              }
            } catch (ClassNotFoundException e) {
              Guid newGuid;
              byte[] thisKey=key.get_data();

              if (thisKey.length == 16) {
                newGuid=new Guid(thisKey);
              } else {
                byte[] newGuidBytes = new byte[16];
          
                System.arraycopy(thisKey,0,newGuidBytes,0,16);
                newGuid = new Guid(newGuidBytes);
              }

              log.logWarning(null,"Class not found for item in store. Guid: "
                                  +newGuid+", Exception: "+e);
            } catch (Throwable x) {
              log.logWarning(null,"Exception while deserializing from store.",
                             x);
              return null; 
            }
          }
        } catch (DbDeadlockException x) {
          errorCloseCursor(iterator);
          errorAbortTxn(txn);
          continue;
        } catch (DbException e) {
          errorCloseCursor(iterator);
          errorAbortTxn(txn);
          e.printStackTrace();
          throw new Bug("dbc.get throw exception " + e);
         
        } catch (IOException e) {
          errorCloseCursor(iterator);
          errorAbortTxn(txn);
          throw new Bug("stream threw " + e);
        } 
      }
    }

    /**
     * Get a QueryIterator containing all of the tuples matching
     * the specified filter(or their id's if idOnly is true).  
     *
     * @param filter The tuple filter to match against
     * @param idOnly if true, return only the ids of the matching tuples
     * @return A QueryIterator of matching tuples if idOnly is false.  
     *         A QueryIterator of the Guids of the matching tuples if idOnly is
     *         true.  An empty iterator if there is no match.
     */  
    LinkedList getAllTuples(TupleFilter filter,boolean idOnly) {
      QueryIterator resultIter;
      Object obj;
      Dbc cursor = null;
      DbTxn txn  = null;
      boolean isFirst;
      LinkedList hits = new LinkedList();
      //int count = 0;
      //int matchCount = 0;

      Dbt key    = new Dbt(new byte[16]); 
      /* Since the size of all GUIDs are 16, it would be intuitive
         to use DB_DBT_USERMEM for the key.  However, this produces
         not enough space exceptions, even with sufficent space.  
         This appears to be a problem with libdb. */
      key.set_flags(Db.DB_DBT_REALLOC);  
      
      Dbt data   = new Dbt(); 
      data.set_flags(Db.DB_DBT_MALLOC);
  
      if (filter.matchGuid!=null) {
         //There can be at most one match if a guid is specified
         obj=getTuple(filter,idOnly);
         if (obj!=null) {
           hits.add(obj);
         }
         return hits;
      } else {
        //System.out.println("Starting query");
        while (true) {
          try {
            txn=dbenv.txn_begin(null,0);
            cursor = table.cursor(txn, 0); 
          
            isFirst=true;
            while (true) { 
              int ret;
              if (isFirst) {
                ret = cursor.get(key, data, Db.DB_FIRST);
                isFirst=false;
              } else {
                ret = cursor.get(key, data, Db.DB_NEXT);
              }

              if (ret == Db.DB_NOTFOUND) {
                break;
              }
                                 
              ObjectInputStream ois = 
                new ObjectInputStream(
                       new ByteArrayInputStream(data.get_data()));
              try { 
                Tuple t = (Tuple)ois.readObject();
                if (filter.check(t)) {
                  if (idOnly) {
                    hits.add(t.id);
                  } else {
                    hits.add(t);
                  }
                  //matchCount++;
                }
                //count++;
              } catch (ClassNotFoundException e) {
                Guid newGuid;
                byte[] thisKey=key.get_data();

                if (thisKey.length == 16) {
                  newGuid=new Guid(thisKey);
                } else {
                  byte[] newGuidBytes = new byte[16];
           
                  System.arraycopy(thisKey,0,newGuidBytes,0,16);
                  newGuid = new Guid(newGuidBytes);
                }

                log.logWarning(null,"Class not found for item in store. Guid: "
                                    + newGuid+", Exception: "+e);
              } catch (Throwable x) {
                log.logWarning(null,"Exception while deserializing from store." 
	  			   ,x);
                return null; 
              }
            }
            cursor.close();
            txn.commit(0);
            //System.out.println("Done query: count="+count+
            //                   ", match="+matchCount);
            return hits;
          } catch (DbDeadlockException x) {
            errorCloseCursor(cursor);
            errorAbortTxn(txn);
            continue;
          } catch (DbException e) {
            errorCloseCursor(cursor);
            errorAbortTxn(txn);
            throw new Bug("dbc.get throw exception " + e);
          } catch (IOException e) {
            errorCloseCursor(cursor);
            errorAbortTxn(txn);
            throw new Bug("stream threw " + e);
          }
        }
      }
    }
    

    /**
     * Managers are the entities responsible for managing
     * leased bindings to an ActualStore.  They also
     * serve as the garbage collection management for
     * an ActualStore instances.  When the last manager for
     * a store is revoked, its ActualStore is closed.
     */

    private class Manager extends AbstractHandler {
      //The handler for the lease representing this object
      EventHandler myLeaseHandler;

      //The person who bound this resource
      EventHandler leaseRequestor;

      //The closure form the person who bound this resource
      Object leaseRequestorClosure;

      //All the query iterators
      HashSet queryIterators = new HashSet();

      //info about all pending requests for this store
      HashMap listenRequests = new HashMap();
 
      //Lock the listenRequests, queryIterators, and state objects 
      Object MANAGER_LOCK = new Object();

      final int MANAGER_STATE_RUNNING = 1;
      final int MANAGER_STATE_STOPPED = 2;

      int managerState = MANAGER_STATE_RUNNING;

      /**
       * Event handler to deal with internal messages.
       *
       * <p>Talks to the lease manager on behalf of listens.</p>
       */
      EventHandler internalHandler = new AbstractHandler() {
        public boolean handle1(Event ev) {
          if (ev instanceof LeaseEvent) {
            LeaseEvent lev = (LeaseEvent)ev;

            switch(lev.type) {
              case LeaseEvent.ACQUIRED: { 
                ListenResponse lresp;
                ResourceInfo rinfo;

                synchronized(MANAGER_LOCK) {
                  rinfo = (ResourceInfo)listenRequests.get((Guid)lev.closure);
                  rinfo.lease = lev.handler;
                }

                if (rinfo != null) {
                  lresp = new ListenResponse(Manager.this, rinfo.requestClosure,
                                             lev.handler, lev.duration);
                  rinfo.requestor.handle(lresp);
                } else {
                  //there was a race between revoking the manager and starting
                  //the listen.  
                  LeaseEvent cancelEvent = new LeaseEvent(this,null,
                                                          LeaseEvent.CANCEL,
                                                          null,null,0);
                  lev.handler.handle(cancelEvent);  
                }
                return true;
              }

              case LeaseEvent.CANCELED: {
                ResourceInfo rinfo;
                RemovePendingRequest removeRequest;

                if (lev.closure!=null) {
                  synchronized(MANAGER_LOCK) {
                    rinfo = (ResourceInfo)listenRequests.remove((Guid)lev.closure);
                  }
                
                  if (rinfo != null) {
                    removeRequest = new RemovePendingRequest(this,null,rinfo.id);
                    inputRequestHandler.handle(removeRequest);
                  }
                }
                
                return true;
              }
            }
          } 
          return false;
        }
      };

      /**
       * Return the ActualStore we are associated with.
       */
      ActualStore getStore() {
        return ActualStore.this;
      }

      final boolean handleListen(Manager m,InputRequest inevent) {
        LeaseEvent lev;
        ResourceInfo rInfo;
        boolean doListen;

        //We're relying on this being unique, so insure it.
        inevent.id = new Guid();
        rInfo      = new ResourceInfo(null,
                                      inevent.source,
                                      inevent.closure,
                                      inevent.id);

        synchronized(MANAGER_LOCK) { 
          //Check state to avoid race if we cancel the outer bind 
          //in the middle of getting a new lease.
          //It's ok if we get cancel the bind before stating the listen or
          //getting the bind, but we must make sure it is put in
          //listenRequests before the close.
          if (managerState == MANAGER_STATE_RUNNING) {
            listenRequests.put(inevent.id,rInfo);
            doListen = true;
          } else {
            doListen = false;
          } 
        }

        if (doListen == false) {
          Exception newx=new ResourceRevokedException();
          respond(inevent,newx);

          return true;
        }

        inputRequestHandler.handle(inevent);

        lev = new LeaseEvent(internalHandler, inevent.id,
                             LeaseEvent.ACQUIRE, internalHandler, 
                             new Name(""), inevent.duration);

        leaseHandler.handle(lev); 

        return true;
      }

      final boolean handleQuery(Manager m,InputRequest inevent) {
        boolean doQuery;
        TupleFilter filter;
        LinkedList hits;

        filter           = new TupleFilter(inevent.query);
        hits             = getAllTuples(filter,inevent.idOnly);   

        QueryIterator qi = new QueryIterator(m,hits);
 
        if (managerState == MANAGER_STATE_RUNNING) {
          queryIterators.add(qi);
          doQuery = true;
        } else {
          doQuery = false;
        } 

        if (doQuery == false) {
          Exception newx=new ResourceRevokedException();

          respond(inevent,newx);
          return true;
        }

        leaseHandler.handle(new LeaseEvent(qi,inevent,
                                           LeaseEvent.ACQUIRE,qi,
                                           new Name("Query iterator"),
                                           inevent.duration));
        return true;
      }

      final boolean handleGet(Manager m,InputRequest inevent) {
        TupleFilter filter;

        /** May be a tuple or a tuple Id */
        Object returnValue;

        filter = new TupleFilter(inevent.query);

        returnValue = getTuple(filter,inevent.idOnly);

        if (inevent.idOnly) {
          respond(inevent,new InputByIdResponse(m, null, 
                                                (Guid)returnValue));
        } else {
          respond(inevent,new InputResponse(m, null, (Tuple)returnValue));
        }
        return true;
      }

      /**
       * Handles InputRequest events.
       *
       * @Param  inevent   The InputRequest to Handle
       */
      final boolean handleInput(Manager m,InputRequest inevent) {

        try {
          doEnterRead();
        } catch (ResourceRevokedException x) {
          respond(inevent,x);

          return true;
        } catch (IOException x) {
          Exception newx = new ResourceRevokedException();
          respond(inevent,newx);

          return true;
        }

        try {
          switch(inevent.type) {
            case InputRequest.LISTEN:
              return handleListen(m,inevent);
            case InputRequest.QUERY:
              return handleQuery(m,inevent);
            default: 
              return handleGet(m,inevent);
          }
        } finally {
          doExitRead();
        }
      }
 
      /**
       * Handles DeleteRequest events.  
       *
       * @param outevent  output request to handle.
       */
      boolean handleDelete(Manager m,DeleteRequest devent) {
        DbTxn txn = null;
      
        try {
          if (false == doEnterWrite(m,devent)) {
            return true;
          }
        } catch (ResourceRevokedException x) {
          respond(devent,new ExceptionalEvent(m, null, x));

          return true;
        } catch (IOException x) {
          Exception newx=new ResourceRevokedException();
          respond(devent,new ExceptionalEvent(m, null, newx));

          return true;
        }

        try {
          int resp          = 0; 

          while(true) {
            Dbt key=new Dbt(devent.ident.toBytes());
            key.set_flags(Db.DB_DBT_REALLOC);

            try {
              txn  = dbenv.txn_begin(null,0);
              resp = table.del(txn,key,0);
              txn.commit(0);
              break;
            } catch (DbDeadlockException x) {
              errorAbortTxn(txn);
              txn=null;
            } catch (DbException x) {
              errorAbortTxn(txn);
              txn=null;
              throw new Bug("Error deleting tuple"+x); 
            }
          }

          if (resp!=Db.DB_NOTFOUND) {
            respond(devent,new OutputResponse(m,null,devent.ident));
          } else {
            NoSuchTupleException noSuch;
            
            noSuch = new NoSuchTupleException("No such tuple to delete(" +
                                              devent.ident.toString() + ")");
            respond(devent,new ExceptionalEvent(m, null, noSuch));
          }

          return true;
        } finally {
          doExitWrite();
        }
      }
   
      /**
       * Handles OutputRequest events.  
       *
       * @param outevent  output request to handle.
       */
      boolean handleOutput(Manager m,OutputRequest outevent) {
        DbTxn txn=null;
        try {
          if (false == m.getStore().doEnterWrite(m,outevent)) {
            return true;
          }
        } catch (ResourceRevokedException x) {
          respond(outevent,new ExceptionalEvent(m, null, x));
  
          return true;
        } catch (IOException x) {
          Exception newx = new ResourceRevokedException();

          respond(outevent,new ExceptionalEvent(m, null, newx));

          return true;
        }

        try {
     
          Guid tupleId = outevent.tuple.id;
          Dbt key      = new Dbt(tupleId.toBytes());
          Dbt data     = null;
    
          try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            ObjectOutputStream oos       = new ObjectOutputStream(buffer);

            oos.writeObject(outevent.tuple);

            data = new Dbt(buffer.toByteArray());
          } catch (NotSerializableException x) {
            respond(outevent,new ExceptionalEvent(m,null,x)); 
            return true;
          } catch (IOException e) {
            throw new Bug("Serialization exception" + e);
          } catch (Throwable t) {
            respond(outevent,new ExceptionalEvent(m,null,t)); 
            return true;
          }
          while (true) {
            try {
              txn = dbenv.txn_begin(null,0);
              table.put(txn, key, data, 0);
              txn.commit(0);
              break;
            } catch (DbDeadlockException x) {
              errorAbortTxn(txn);
            } catch (DbException e) {
              errorAbortTxn(txn);
              throw new Bug("error adding tuple to database");
            }
          }

          respond(outevent,new OutputResponse(m, null, tupleId)); 

          //So any responses come to us(shouldn't be any).
          outevent.source=m;
          outevent.closure=null;
 
          tupleHandler.handle(outevent);
          return true;
        } finally {
          doExitWrite();
        }
      }
   

      protected boolean handle1(Event event) {
        if (isNotValid(event)) {
          return true;
        }
       
        if (event instanceof LeaseEvent) {
          LeaseEvent lev = (LeaseEvent)event;

          switch (lev.type) {
            case LeaseEvent.ACQUIRED:
              BindingRequest breq = (BindingRequest)lev.closure;
              myLeaseHandler      = lev.handler;

              respond(breq,new BindingResponse(this,null,
                                               breq.descriptor,
                                               this,
                                               lev.handler,
                                               lev.duration));
              break; 

            case LeaseEvent.CANCELED:
              revoke(true);             
              break;

            default:
              break;
          }

          return true;
        } else if (event instanceof ExceptionalEvent) {
          if (((ExceptionalEvent)event).x instanceof LeaseDeniedException) {
            BindingRequest breq = (BindingRequest)event.closure;
            //Kill the resource
            revoke(true);
       
            //Return the exception
            respond(breq,event);
            return true;
          } else {
            return true;
          }
        } else if (event instanceof InputRequest) {
          return handleInput(this,(InputRequest) event);
        } else if (event instanceof SimpleInputRequest) {
          SimpleInputRequest sinevent = (SimpleInputRequest)event;

          InputRequest inevent = new InputRequest(event.source, 
                                                  event.closure,
                                                  sinevent.type,
                                                  sinevent.query,
                                                  sinevent.duration,
                                                  false,
                                                  null);

          return handleInput(this,inevent);
        } else if (event instanceof OutputRequest) {
          return handleOutput(this,(OutputRequest)event);
        } else if (event instanceof SimpleOutputRequest) {
          SimpleOutputRequest soutevent = (SimpleOutputRequest)event;

          OutputRequest outevent = new OutputRequest(event.source,
                                                   event.closure,
                                                   soutevent.tuple,
                                                   null);

          return handleOutput(this,outevent);
        } else if (event instanceof DeleteRequest) {
          return handleDelete(this,(DeleteRequest)event);
        } else if (event instanceof ExceptionalEvent) {
          log.logWarning(this, "Unexpected exceptional event",
                         ((ExceptionalEvent)event).x);

          return true;
        }

        return false;
      }

      private void revokeQueries() {
        java.util.Iterator it;

        synchronized(MANAGER_LOCK) {
          if (managerState != MANAGER_STATE_STOPPED) {
            throw new Bug("revokeListens called from state "+managerState);
          }

          it=queryIterators.iterator();
        }
        while (it.hasNext()) {
          QueryIterator qi=(QueryIterator)it.next();
          it.remove();
          qi.revoke(false);
        }
      } 

      private void revokeListens() {
        Set s; 
        java.util.Iterator it;

        synchronized(MANAGER_LOCK) {
          if (managerState != MANAGER_STATE_STOPPED) {
            throw new Bug("revokeListens called from state "+managerState);
          }
          s=listenRequests.entrySet();
        }

        while(true) {
          Map.Entry mentry;
          ResourceInfo rinfo;
          RemovePendingRequest removeRequest;
          HashSet revokedSet=new HashSet();

          synchronized(MANAGER_LOCK) {
            it=s.iterator();
            do {
              if (it.hasNext()==false) {
                return;
              }
              mentry=(Map.Entry)it.next();
            } while (revokedSet.contains(mentry));
            revokedSet.add(mentry);
          }

          rinfo=(ResourceInfo)mentry.getValue();

          if (rinfo.lease!=null) {
             LeaseEvent cancelEvent;

             ExceptionalEvent rEvent;
             ResourceRevokedException rExcept;

             cancelEvent = new LeaseEvent(this,null,
                                          LeaseEvent.CANCEL,
                                          null,null,0);
             rinfo.lease.handle(cancelEvent); 

             rExcept     = new ResourceRevokedException(); 
             rEvent      = new ExceptionalEvent(Manager.this,
                                                rinfo.requestClosure,
                                                rExcept);
             rinfo.requestor.handle(rEvent);
          }
        }
      } 

      /**
       * Revoke my bind.  Tell the lease manager it is revoked if 
       * this isn't coming from the lease manager.
       *
       * @param fromManager <code>true</code> if this is being called
       *                    due to a message from the LeaseManager 
       *                    telling us the lease was cancelled. 
       */ 
      public void revoke(boolean fromManager) {
        //Am I responsible for shutting down the ActualStore?
        ExceptionalEvent rEvent;
        ResourceRevokedException rExcept;

        boolean amClosing    = false;
        LeaseEvent lev       = null;
        EventHandler lhandle = null;
        boolean amStopper;

        synchronized(MANAGER_LOCK) {
          amStopper    = (managerState == MANAGER_STATE_RUNNING);
          managerState = MANAGER_STATE_STOPPED;
          
        }
        if (amStopper) {
          revokeListens();
          revokeQueries();
        }

        //Tell the lease manager.  Use the leaseRequest as the source
        //so they know the lease is no longer good.
        synchronized(LOCAL_LOCK) {
          if ((!fromManager) && (myLeaseHandler!=null) && amStopper) {
            lev = new LeaseEvent(this,null,
                                LeaseEvent.CANCEL, null,null,0);
            lhandle        = myLeaseHandler;
            myLeaseHandler = null;  
          }

          managers.remove(this);

          if (0 == managers.size()) {
            if (envState == ENVSTATE_OPEN) {
              envState  = ENVSTATE_CLOSING;
              amClosing = true;
            }     
          }
        } 

        if (lev!=null) {
          lhandle.handle(lev);
        }  

        if (amClosing) {
          //Kill the ActualStore
          do {
            //Wait for all ongoing queries to complete.
            synchronized(LOCAL_LOCK) {
              if (outstanding==0)
                break;
            }
            delayThread(1);
          } while(true);

          //Close the BDB table
          doClose();

          //Remove this ActualStore from the list of open stores
          synchronized(TSTORE_LOCK) {
            allStores.remove(thisGuid);     
          }

          //We are done closing
          synchronized(LOCAL_LOCK) {
            envState=ENVSTATE_CLOSED;
          }
        }
      }

      /**
       * A java iterator of a TupleStore.  This is used when cloning.
       * The iterator is over serialized tuples.
       */
      class JavaIterator extends Thread implements java.util.Iterator {
        //The next element to be returned.
        private byte[] nextElement;

        //The next key to be returned
        private byte[] nextKey;

        public Object next() {
          int ret;
          Dbt data;
          Dbt key;
          byte[] thisElement;
          byte[] thisKey;
          Dbc iterator=null;
          Guid newGuid;

          //doEnterRead();

          if (nextElement==null) {
            throw new NoSuchElementException();
          } else {
            thisElement = nextElement;
            thisKey     = nextKey;
          }
    
          while(true) {
            try {
              //Get the element for the next read(ie, not what we'll 
              //return this time.
              iterator = table.cursor(null, 0);
              key  = new Dbt((byte[])nextKey.clone());   
              key.set_flags(Db.DB_DBT_REALLOC);
  
              data = new Dbt(); 
              data.set_flags(Db.DB_DBT_MALLOC);
  
              ret = iterator.get(key, data, Db.DB_SET);
  
              if (ret != Db.DB_NOTFOUND) {
                key  = new Dbt((byte[])nextKey.clone());  
                key.set_flags(Db.DB_DBT_REALLOC);
  
                data = new Dbt(); 
                data.set_flags(Db.DB_DBT_MALLOC);
  
                ret = iterator.get(key, data, Db.DB_NEXT);
              }
    
              if (ret == Db.DB_NOTFOUND) {
                nextElement = null;
                nextKey     = null;
              } else {
                nextElement = data.get_data();     
                nextKey     = key.get_data(); 
              }

              //Must close it since we might return in a different thread.
              iterator.close();
              iterator = null;
              break;
            } catch (DbDeadlockException x) {
              errorCloseCursor(iterator);
              continue;
            } catch (DbException e) {
              errorCloseCursor(iterator);
              throw new Bug("Couldn't get next element " +e);
            }
          }

          if (thisKey.length == 16) {
            newGuid=new Guid(thisKey);
          } else {
            byte[] newGuidBytes = new byte[16];
           
            System.arraycopy(thisKey,0,newGuidBytes,0,16);
            newGuid = new Guid(newGuidBytes);
          }
        //  doExitRead();
          return new BinaryData(newGuid,
                                "A serialized tuple",
                                "application/x-one-world-btpl",
                                thisElement);
        }

        public boolean hasNext() {
          return (nextElement!=null);
        }

        public void remove() {
          throw new UnsupportedOperationException(); 

        }

        public JavaIterator() {
          int ret;
          Dbt data;
          Dbt key;
          Dbc iterator = null;


          //doEnterRead();
          while(true) {
            try {
              iterator = table.cursor(null, 0);
  
              key  = new Dbt();  
              key.set_flags(Db.DB_DBT_MALLOC);
  
              data = new Dbt(); 
              data.set_flags(Db.DB_DBT_MALLOC);
             
              ret = iterator.get(key, data, Db.DB_FIRST);

              //Must close it since next use may be a different thread
              iterator.close();
               
              if (ret == Db.DB_NOTFOUND) {
                nextElement = null;
                nextKey     = null;
              } else {
                nextElement = data.get_data();    
                nextKey     = key.get_data(); 
              }

              break;

            } catch (DbDeadlockException x) {
              errorCloseCursor(iterator);
              continue;
            } catch (DbException e) {
              errorCloseCursor(iterator);
              throw new Bug("couldn't make JavaIterator " +e);
            }
          }
         // doExitRead();
        }
      }

      /**
       * Get a JavaIterator for this store.
       */
      JavaIterator getJavaIterator() {
        return new JavaIterator();
      }
    }


    /**
     * Create a new Manager object to add another bind onto the tuplestore.
     */
    public Manager newManager() throws ResourceRevokedException,IOException {
      Manager m;

      //Get a lock on the store (or throw an exception)
      doEnterRead();

      try {
        m=new Manager();
        managers.add(m);
      } finally {
        doExitRead();
      }
      return m;
    }

    /**
     * Close the bdb table.
     */
    private void doClose() {
      try {
        table.close(0);
      } catch (DbException x) {
        throw new Bug("Error in doClose(): can't close table"+x);
      }
    }

    public ActualStore(Environment env,Guid envId) throws IOException {
      super(env);
      PendingInputRequests pending;

      thisGuid            = envId;

      inputRequestHandler = declareImported(INPUT_REQUEST_HANDLER);
      tupleHandler        = declareImported(TUPLE_HANDLER);
 
      pending             = new PendingInputRequests(rootEnv,leaseHandler);

      link("input request handler", "unleased request handler", pending);
      link("tuple handler", "tuple handler", pending);

      managers = new HashSet(); 
      table    = openGuidDB(envId);
      synchronized(LOCAL_LOCK) {
        envState = ENVSTATE_OPEN;
      }
    };

    /** 
     * The query iterator that we return as response to queries.
     */
    class QueryIterator extends AbstractHandler {
      //The handler for the lease representing this object
      EventHandler myLeaseHandler;

      //The person who bound this resource
      EventHandler leaseRequestor;

      //The closure form the person who bound this resource
      Object leaseRequestorClosure;
      
      /** Has this iterator been revoked*/
      boolean revoked = false;

      /** The lock on this object */
      Object ITERATOR_LOCK = new Object();

      /** list of results */
      LinkedList hits;

      boolean isEmpty;

      /*The manager we belong to*/
      Manager m;

      QueryIterator(Manager m, LinkedList hits) {
        this.m    = m;
        this.hits = hits;

        if (hits.size()==0) {
          isEmpty = true;
        } else {
          isEmpty = false;
        }
      } 
      /**
       * Handles {@link IteratorRequest} events.  
       *
       * @param event   The event to handle.
       */
      public boolean handle1(Event event) {
        Object obj;
        boolean hasAnother;

        if (isNotValid(event)) {
          return true;
        }
 
        if (event instanceof IteratorRequest) {
          IteratorRequest irevent = (IteratorRequest)event;

          synchronized(ITERATOR_LOCK) {
            if (revoked) { 
              Exception x = new ResourceRevokedException();

              respond(event,new ExceptionalEvent(this, null, x));

              return true;
            } else {
              if (isEmpty || (0 == hits.size())) {
                obj = null;
                hasAnother = false;
              } else {
                obj = hits.removeFirst();
                hasAnother = (0!=hits.size());
              }
            }
          }


          if (isEmpty) {
            respond(event,new IteratorEmpty(this, null));
	  } else if (null == obj) {
            respond(event,new ExceptionalEvent(this, null, 
                                               new NoSuchElementException()));
          } else {
            respond(event,new IteratorElement(this, null, 
                                              obj,
                                              hasAnother));
          }

          return true;
        } else if (event instanceof LeaseEvent) {
          LeaseEvent lEvent=(LeaseEvent)event;

          switch (lEvent.type) {
            case LeaseEvent.ACQUIRED:
              InputRequest iRequest =(InputRequest)event.closure;
              myLeaseHandler        = lEvent.handler;
              leaseRequestor        = iRequest.source;
              leaseRequestorClosure = iRequest.closure;

              respond(iRequest,new QueryResponse(this,
                                                 null,
                                                 this,
                                                 lEvent.handler,
                                                 lEvent.duration));
              break;

            case LeaseEvent.CANCELED:  
              revoke(true);
              break;

            default:
              break;
          }
          return true;
        } else { 
          return false;
        }
      }

      /**
       * Revoke this iterator
       *
       * @param fromManager <code>true</code> if this is the result of a 
       *                    lease termination from the lease manager.
       */
      public void revoke(boolean fromManager) {
        boolean amRevoking;

        synchronized(ITERATOR_LOCK) {
          amRevoking=!revoked;

          revoked = true;
          //Let garbage collection work immediately
          hits=null;
        }
        synchronized(m.MANAGER_LOCK) {
          //So we don't interfere with the iterator
          if (m.managerState == m.MANAGER_STATE_RUNNING) {
            m.queryIterators.remove(this);
          }
        } 

        //Tell the lease manager we are revoked
        if ((!fromManager) && amRevoking) {
          LeaseEvent lev;
          myLeaseHandler.handle(new LeaseEvent(this,null,
                                LeaseEvent.CANCEL, null,null,0));
        } 
      }
    }
  }

  /**
   * Delay a thread for a given interval.
   */
  private static void delayThread(int milli) {
    try {
      Thread.sleep(milli);
    } catch (InterruptedException x) {
    }
  }
}


