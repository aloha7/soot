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

import one.util.Guid;
import one.world.Constants;

import one.world.core.*;
import one.world.binding.*;
import one.world.rep.*;
import one.world.util.*;
import one.world.io.*;
import one.world.env.*;

import java.util.List;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * A component to perform discovery server elections.
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
 *    <dd>The imported environment request handler.
 *        </dd>
*    <dt>timer</dt>
 *    <dd>The imported timer handler.
 *        </dd>
 * </dl></p>
 *
 * @version  $Revision: 1.21 $
 * @author   Adam MacBeth 
 * @author   Eric Lemar
 */
public class ElectionManager {

  // =======================================================================
  //                           Constants
  // =======================================================================

  // The Election status constants.

  /** The test status. */
  private static boolean TEST = false;

  /** The state during normal operation. */
  private static final int NORMAL = 1;

  /** The state when this process has decided to call an election. */ 
  private static final int INITIATING = 2;

  /** The state when this process is collecting announcements. */
  private static final int COLLECTING = 3;

  /** True when we are exiting one.world */
  private static boolean stopping = false;

  /** Indicates that this node is in server mode. */
  private static final int SERVER = 1;

  /** Indicates that this node is in non-server mode. */
  private static final int NON_SERVER = 2;

  /** The max value the heuristic has seen */
  private static long maxHeuristic = -1;

  /** Lock protecting maxHeuristic structures */
  private static final Object heuristicLock = new Object();

  /** Lock protecting ALL data structures */
  private final Object lock = new Object();

  // =======================================================================
  //                           The main handler
  // =======================================================================

  /** The main exported event handler. */
  final class MainHandler extends AbstractHandler {

    /** Handle the specified event. */
    protected boolean handle1(Event e) {
      if (e instanceof EnvironmentEvent) {
	if (Constants.DEBUG_DISCOVERY ) {
	  log.log(this,"Got EnvironmentEvent");
	}

	EnvironmentEvent ee = (EnvironmentEvent)e;

	if (EnvironmentEvent.ACTIVATED == ee.type) {
	  activate(); 
	}
	return true;

      } else if (e instanceof InputResponse) {
	InputResponse ir = (InputResponse)e;
	
	if (ir.tuple instanceof ElectionEvent) {
	  ElectionEvent ee = (ElectionEvent)ir.tuple;
	  
	  // Another process is calling for an election.
	  if (ElectionEvent.START == ee.type) {
            handleStart(ee);
	    return true;
	  } 
	  
	  // Announcements of capacity from other processes.
	  if (ElectionEvent.CAPACITY == ee.type) {
            handleCapacity(ee);
            return true;
	  }	    
	}
        return false;

      } else if (e instanceof DynamicTuple) {
        handleDynamicTuple((DynamicTuple)e);
        return true;

      } else if (e instanceof AnnounceEvent) {
        handleAnnounce((AnnounceEvent)e);
	return true;

      } else if (e instanceof ListenResponse) {
	if (Constants.DEBUG_DISCOVERY ) {
	  log.log(this,"Got ListenResponse");
	}
	ListenResponse lr = (ListenResponse)e;
	listenMaintainer = 
	  new LeaseMaintainer(lr.lease,
			      lr.duration,
			      this,
			      null,
			      timer);
	return true;

      } else if (e instanceof OutputResponse) {
	return true;
      } else if (e instanceof SimpleOutputRequest) {
        e.source = this;
        multicastChannel.handle(e); 
        return true;
      }
      
      return false;
    }
 
    /** 
     * Handle an announcement from a server.  Ignore if we're not
     * a server.  If we are a server, check to see whether we should stop.
     *
     * @param ae The announcement we received. 
     */
    void handleAnnounce(AnnounceEvent ae) {
      InetAddress remoteAddr;
      DiscoveryServer ds = null;

      if (Constants.DEBUG_DISCOVERY ) {
         //log.log(this,"Got AnnounceEvent");
      }

      //Need to be a little tricky since InetAddress.getByName() could 
      //block for a while.  Initial check to see whether we'll likely
      //need to do the address lookup.

      synchronized(lock) {
        lastAnnounceTime = SystemUtilities.currentTimeMillis(); 
        if (mode!=SERVER) {
          return;
        }
      }
      
      //Do the lookup outside of the lock
      try {
        remoteAddr = InetAddress.getByName(ae.ref.host);
      } catch (UnknownHostException x) {
        log.logWarning(this,"Unable to look up " + ae.ref.host,x);
        return;
      }

      synchronized(lock) {
        //Make sure this hasn't changed while not synchronized
        //If it has, we just wasted effort, but still correct.
        if (SERVER == mode) {
          // There is another server active.
          // If it has a better heuristic than this one, shutdown.
	    
          // FIXME - should probably find a better way to exchange
          // capacities.  Since capacities change over time, if server
          // capacities are received in the wrong order, both could shutdown.
          if (ae.capacity > heuristic()) {
            if (!remoteAddr.equals(localAddr)) {
              mode = NON_SERVER;
              ds = serverRemove();
            } else {
              log.logWarning(this,"Our announcement was bigger than heuristic()");
            }
          } else if (ae.capacity == heuristic()
                     && addrGreaterThan(remoteAddr,
                                        localAddr)) {
            //If we're exiting, let stop() deal with it
            if (!stopping) {
              mode = NON_SERVER;
              ds = serverRemove();
            }
          }
        }

      }

      if (ds != null) {
        serverShutdown(ds);
      }
    }

    /**
     * Handle a received capacity announcement or an election
     * start message after handleStart() is done with it.
     *
     * @param ee The ElectionEvent we received.
     */
    void handleCapacity(ElectionEvent ee) {
      ElectionEvent newEvent = null;

      if (Constants.DEBUG_DISCOVERY ) {
        log.log(this,"Got InputResponse: " + 
	        "CAPACITY election event, capacity = " 
	        + ee.capacity);
      }
      synchronized(lock) {
        if (COLLECTING == state) {
          // If the capacities are equal choose the one with the
          // higher IP address.
          InetAddress addr = ee.addr;

          if (ee.capacity == maxCapacity) {
            if (addrGreaterThan(addr,localAddr)) {
              maxAddress = addr;
            }
          } else if (ee.capacity > maxCapacity) {
            maxAddress = addr;
            maxCapacity = ee.capacity;
          }
        } else if (mode == SERVER) {
          //Somehow we missed the start, but since we're the server
          //advertise if we would to win.
          long heur;
          heur=heuristic();

          if (((ee.capacity == maxCapacity) 
                && addrGreaterThan(localAddr,ee.addr))  
               || (ee.capacity<heur)) {
             newEvent = new ElectionEvent(NullHandler.NULL,
                                          null,
                                          ElectionEvent.CAPACITY,
                                          heur,
                                          localAddr);
          }
        }
      }
      //Do the actual send unsynchronized
      if (newEvent != null) {
        multicastChannel.handle(new SimpleOutputRequest(this,null,newEvent));
      }
    }

    /**
     * Start an election.  
     *
     * Ignore if we are already electing.
     */
    void doStartElection() {
      ElectionEvent ee = null;


      synchronized(lock) {
        if (state == COLLECTING) {
          //Already electing, return.
          if (Constants.DEBUG_DISCOVERY) {
            log.log(this,"In doStartElection, not starting an election.");
          }
          return;
        } 
        lastAnnounceTime = SystemUtilities.currentTimeMillis();
        if (Constants.DEBUG_DISCOVERY) {
          log.log(this,"In doStartElection, starting an election.");
        }
        state = COLLECTING; 
  
        maxAddress         = localAddr;
        advertisedCapacity = heuristic();
        maxCapacity        = advertisedCapacity;
        
 
        ee = new ElectionEvent(NullHandler.NULL,
                                             null,
                                             ElectionEvent.START,
                                             advertisedCapacity,
                                             localAddr);


        // Setup timer to bound election time.
        DynamicTuple dt = new DynamicTuple();
        dt.set("msg","timeout");
        boundTimerNotification = 
          timer.schedule(Timer.FIXED_DELAY,
                       SystemUtilities.currentTimeMillis()
                       + Constants.DISCOVERY_ELECTION_DURATION,
                       Constants.DISCOVERY_ELECTION_DURATION, this, dt);

    
        if (mode == SERVER) {
          ElectionEvent ee2 = new ElectionEvent(NullHandler.NULL,
                                             null,
                                             ElectionEvent.CAPACITY,
                                             advertisedCapacity,
                                             localAddr);

          SimpleOutputRequest sout = new SimpleOutputRequest(this,null,ee2);



          timer.schedule(Timer.ONCE,
                       SystemUtilities.currentTimeMillis()
                       + Constants.DISCOVERY_ELECTION_DURATION/2,
                       0, this, sout);
        } 
      }

      //Send the event outside the lock
      if (ee!=null) {
        multicastChannel.handle(new SimpleOutputRequest(this,null,ee));
      }
    }

    /**
     * Handle a received message to start an election.
     *
     * @param ee The start event.
     */
    void handleStart(ElectionEvent ee) {
      ElectionEvent newEvent = null;

      if (Constants.DEBUG_DISCOVERY ) {
        log.log(this,"Got InputResponse: " + 
                     "START election event, capacity = " +
                     ee.capacity);
      }
      synchronized(lock) {	    
        if (stopping) {
          return;
        }
        if (COLLECTING == state) {
          //Already started, treat it as a capacity.
          handleCapacity(ee);
        } else {
          lastAnnounceTime = SystemUtilities.currentTimeMillis();
          // This is the first start event.
          // Initialize advertised capacity.
          advertisedCapacity = heuristic();
          state = COLLECTING;

          maxAddress  = localAddr;
          maxCapacity = advertisedCapacity;
          handleCapacity(ee);

          // Setup timer to bound election time.
          DynamicTuple dt = new DynamicTuple();
          dt.set("msg","timeout");


          boundTimerNotification = 
            timer.schedule(Timer.FIXED_DELAY,
                           SystemUtilities.currentTimeMillis()
                           + Constants.DISCOVERY_ELECTION_DURATION,
                           Constants.DISCOVERY_ELECTION_DURATION, this, dt);
 
          if (mode == SERVER) {
            if ((maxCapacity <= advertisedCapacity)) {
              ElectionEvent ee2 = new ElectionEvent(NullHandler.NULL,
                                                 null,
                                                 ElectionEvent.CAPACITY,
                                                 advertisedCapacity,
                                                 localAddr);

              SimpleOutputRequest sout = new SimpleOutputRequest(this,null,ee2);


              timer.schedule(Timer.ONCE,
                           SystemUtilities.currentTimeMillis()
                           + Constants.DISCOVERY_ELECTION_DURATION/2,
                           0, this, sout);
            }
          } 
 
          // Advertise capacity. 
          if ((maxCapacity <= advertisedCapacity)) {
            newEvent = new ElectionEvent(NullHandler.NULL,
                                         null,
                                         ElectionEvent.CAPACITY,
                                         advertisedCapacity,
                                         localAddr);
          }
        }
      }

      //Do the actual send unsynchronized
      if (newEvent != null) {
        multicastChannel.handle(new SimpleOutputRequest(this,null,newEvent));
      }
    }

    /**
     * Handle an event telling us that the election is over.
     *
     * @param e The event telling us that the election is over.
     */
    void handleTimeout(DynamicTuple e) {
      DiscoveryServer ds = null;

      synchronized(lock) {

        if (Constants.DEBUG_DISCOVERY ) {
          log.log(this,"Got DynamicTuple: msg = timeout");
        }

        if (boundTimerNotification != null) {
          boundTimerNotification.cancel();
        }

        if (state != COLLECTING) {
          return;
        }

        lastAnnounceTime = SystemUtilities.currentTimeMillis(); 
        state = NORMAL;

        if (Constants.DEBUG_DISCOVERY ) {
          SystemUtilities.debug("maxCapacity = " 
                                + maxCapacity
                                + " advertisedCapacity = " 
                                + advertisedCapacity);
        }

        if ( (maxCapacity>=0) && (maxAddress.equals(localAddr))) {
           // This node has won the election, start the server component.

          if (Constants.DEBUG_DISCOVERY ) {
            log.log(this,"This node has won a discovery server election");
          }
  
          if (SERVER == mode) {
            // Do nothing.
          } else if (NON_SERVER == mode) {
            serverStartup();
            mode = SERVER;
          }

        } else {
          //This node has not won the election. 
          //If we are stopping, let stop() deal with this.
          if (!stopping) {
            if (SERVER == mode) {
              mode = NON_SERVER;
              ds = serverRemove();
            } else if (NON_SERVER == mode) {
             // Do nothing.
            }
          }
        }
      }
      if (ds!=null) {
        serverShutdown(ds);
      }
    }


    /**
     * Handle periodic events checking to see whether we've heard from the server
     *
     * If the server has not been heard from within the last 2
     *  announcement periods, call for an election.
     * 
     * @param e
     */
    void handleElectionAnnounce(DynamicTuple e) {
      long timeDiff;

      synchronized(lock) {
        timeDiff = SystemUtilities.currentTimeMillis() - lastAnnounceTime; 
      }

      if (timeDiff > Constants.DISCOVERY_ELECTION_CALL_TIME) {
        if (Constants.DEBUG_DISCOVERY ) {
          log.log(this,"Calling for election due to missing announce");
        }
        doStartElection();
      }
    }

    /**
     * Handle an incoming dynamic tuple.
     *
     * @param e The DynamicTuple to handle.
     */
    void handleDynamicTuple(DynamicTuple e) {
      Object msgField;

      msgField = e.get("msg");

      if ("timeout".equals(msgField)) {
        handleTimeout(e);
      } else if ("announce".equals(msgField)) {
        handleElectionAnnounce(e);
      }
    }
  }
  

  /**
   * Unlink the server component from the kernel and null out references to it.
   */
  private void serverShutdown(DiscoveryServer ds) {
    if (null == ds) {
      return;
    }

    if (Constants.DEBUG_DISCOVERY) {
      log.log(this,"Shutting down the discovery server");
    }
    List theList = leaseHandler.getHandlers();
    Component leaseComponent 
      = ((Component.HandlerReference)(theList.get(0))).getComponent();
    theList = requestHandler.getHandlers();
    
    ds.stop();

    ds.unlink("request", "request", root);
    ds.unlink("lease",   "request", leaseComponent);

    //This was here when I first saw it(eric)
    System.gc();
    System.gc();
    System.gc();
  }
  
  /**
   * Null out our pointer to the server.  Return the server.  This allows
   * us to remove the server in a synchronized manner, but to shut it down
   * outside synchronization.
   */ 
  DiscoveryServer serverRemove() {
    DiscoveryServer ds = serverComponent;

    serverComponent = null;
    return ds;
  }

  /** 
   * Start a server component and link it into the kernel.
   */
  private void serverStartup() {
    if (Constants.DEBUG_DISCOVERY) {
      log.log(this,"Starting up a new discovery server.");
    }
    synchronized(lock) {
      serverComponent = new DiscoveryServer(root);

      List theList = leaseHandler.getHandlers();
      Component leaseComponent 
        = ((Component.HandlerReference)(theList.get(0))).getComponent();
      theList = requestHandler.getHandlers();
    
      serverComponent.link("request", "request", root);
      serverComponent.link("lease",   "request", leaseComponent);
      serverComponent.activate();
    }
  }

  /** 
   * Compares two objects of type InetAdress to determine if the first
   * is greater than the second.
   * 
   * @param a1 The first address.
   * @param a2 The second address.
   *
   * @return True if a1 is greater than a2, false otherwise.  */
  private static boolean addrGreaterThan(InetAddress a1, InetAddress a2) {
    byte[] b1 = a1.getAddress();
    byte[] b2 = a2.getAddress();

    for (int i = 0; i < b1.length; i++) {
      if (b1[i] > b2[i]) return true;
      else if (b1[i] < b2[i]) return false;
      //if equal continue

      if ( (b1.length - 1) == i) return false; // The two are equal.
    }
    // Should never get here.
    return false;
  }
    

  // =======================================================================
  //                           Instance fields
  // =======================================================================

  /**
   * The exported main event handler.
   *
   * @serial  Must not be <code>null</code>.
   */
  private final MainHandler       mainHandler;

  /**
   * The imported request event handler.
   *
   * @serial  Must not be <code>null</code>.
   */
  private final Component.Importer requestHandler;
 
  /**
   * The imported lease event handler.
   *
   * @serial  Must not be <code>null</code>.
   */
  private final Component.Importer leaseHandler;

  /** A timer component. */
  private final Timer timer;
 
  /** The multicast channel for elections. */
  EventHandler multicastChannel;

  /** The lease maintainer for the multicast channel. */
  LeaseMaintainer multicastMaintainer;

  /** The lease maintainer the listen operation. */
  LeaseMaintainer listenMaintainer;

  /** The timer notification handler for the upper bound on election. */
  Timer.Notification boundTimerNotification;

  /** The timer notification handler for the upper bound on election. */
  Timer.Notification electionTimerNotification;

  /** The maximum capacity seen so far. */
  long maxCapacity = 0;

  /** The address of the node with the max capacity. */
  InetAddress maxAddress;

  /** The capacity advertised in this election. */
  long advertisedCapacity = 0;

  /** The system time when a server announcement was last heard. */
  long lastAnnounceTime;

  /** The election state of this component. */
  int state;

  /** The operating mode. */
  int mode;

  /** A handle to the discovery server component. */
  DiscoveryServer serverComponent;

  /** The local IP address. */
  InetAddress localAddr;

  /** The root environment. */
  Environment root;

  /** The system log. */
  private static final Log log;

  // Initialize the system log variable.
  static {
    log = (Log)AccessController.doPrivileged(
      new PrivilegedAction() {
        public Object run() {
          return Log.getSystemLog();
        }
      });
  }

  // =======================================================================
  //                           Constructor
  // =======================================================================

  /**
   * Create a new instance of <code>ElectionManager</code>.
   *
   * @param timer           A timer component.
   * @param requestHandler  An environment request handler.
   * @param leaseHandler    A lease management request handler.
   * @param root            The root environment.
   *
   */
  public ElectionManager(Timer timer,
			 Component.Importer requestHandler,
			 Component.Importer leaseHandler,
			 Environment root) {
    mainHandler = new MainHandler();

    this.timer = timer;
    this.requestHandler = requestHandler;
    this.leaseHandler = leaseHandler;
    this.root = root;

    // Initialize fields.
    try {
      localAddr = InetAddress.getLocalHost();
    }
    catch (UnknownHostException e) {
      log.logError(this,"Localhost address not found.");
      // FIXME - We don't have an address.  What to do now?
    }
    state = NORMAL;
    mode = NON_SERVER;
  }


  // =======================================================================
  //                           Helper functions
  // ======================================================================

  /** 
   * Start the new component.
   */
  private void activate() {
  
    Event response;

    //bind DatagramIO input channel to listen for announcements
    SioResource sio = new SioResource();
    sio.remoteHost = Constants.DISCOVERY_ELECTION_ADDR;
    sio.remotePort = Constants.DISCOVERY_ELECTION_PORT;
    sio.type       = SioResource.MULTICAST;

    BindingRequest bindreq = 
      new BindingRequest(mainHandler,"multicast",sio,Duration.FOREVER);
    try {
      response = Synchronous.invoke(requestHandler,bindreq,
				    Constants.SYNCHRONOUS_TIMEOUT);
    }
    catch (TimeOutException x) {
      log.logError(this, "Could not obtain multicast channel");
      stop();
      return;
    }

    //save resource and get maintainer for the datagram channel lease
    if (response instanceof BindingResponse) {
      if (Constants.DEBUG_DISCOVERY) {
	log.log(this,"Bound datagram channel");
      }
      BindingResponse br = (BindingResponse)response;
      multicastChannel = br.resource;
      multicastMaintainer = 
	new LeaseMaintainer(br.lease,
			    br.duration,
			    mainHandler,
			    null,
			    timer);
    }
    else if (response instanceof ExceptionalEvent) {
      log.logError(this, "Could not obtain multicast channel");
      stop();
      return;
    }

    //send listen request
    SimpleInputRequest inputreq = 
      new SimpleInputRequest(mainHandler,
			     null,
			     SimpleInputRequest.LISTEN,
			     new Query(),
			     Duration.FOREVER,
			     false);
    multicastChannel.handle(inputreq);

    // Setup timer.
    // First, initialize lastAnnounceTime.
    // between this time being initialized and the first timer event coming in.
    lastAnnounceTime = SystemUtilities.currentTimeMillis();
    DynamicTuple dt = new DynamicTuple();
    dt.set("msg","announce");
    electionTimerNotification = 
        timer.schedule(Timer.FIXED_DELAY,
                       SystemUtilities.currentTimeMillis()
		           + Constants.DISCOVERY_ANNOUNCE_PERIOD,
		       Constants.DISCOVERY_ANNOUNCE_PERIOD,
	               mainHandler, dt);
  }
   
  /** 
   * This function computes a heuristic function based on properties
   * of this node.  The result of this computation is advertised to
   * other processes during an election with the goal of electing the
   * process with the highest heuristic. 
   *
   * @return  The value of the heuristic.
   */

  static long heuristic() {
    if (TEST) {
      return 10;
    } else if (stopping) {
      //Indicate that we are stopping
      return DiscoveryServer.ANNOUNCE_CLOSING;
    } else if (Constants.DISCOVERY_ANNOUNCE_VALUE!=-1) {
      return Constants.DISCOVERY_ANNOUNCE_VALUE;
    } else {
      long newHeuristic = (MEMORY_WEIGHT * (long)SystemUtilities.totalMemory() + 
		           UPTIME_WEIGHT * (long)SystemUtilities.uptime());
      synchronized(heuristicLock) {
        if (newHeuristic > maxHeuristic) {
          maxHeuristic = newHeuristic;
        } 
        return maxHeuristic;
      }
    }
  }

  /** The weight given to the uptime in the heuristic. */
  private static final long UPTIME_WEIGHT = 2;
  
  /** The weight given to the total memory in the heuristic. */
  private static final long MEMORY_WEIGHT = 1;

  /**
   * This function passes events to the main handler.
   *
   * @param  e  The event to be passed.
   */
  public void notify(Event e) {
    mainHandler.handle(e);
  }

  /**
   * Called from DiscoveryClient to ask us to start an election.
   */
  public void startElection() {
    if (Constants.DEBUG_DISCOVERY) {
      log.log(this,"Starting an election because of an error");
    }
    mainHandler.doStartElection();
  }

  /**
   * Release resources so the component can be stopped.
   */
  void stop() {
    boolean amServer1    = false;
    boolean amServer2    = false;
    boolean amCollecting = false;
    DiscoveryServer ds; 

    synchronized(lock) {
      stopping      = true;
      amCollecting  = (state == COLLECTING); 
      amServer1     = (mode == SERVER);
      if (amServer1) {
        //We are the server, tell other's we're going away.
        serverComponent.prepareToDie();
      }
    }

    //Ok if an election happens here since we are already stopping.

    if (amServer1 || amCollecting) {
      //Try to gracefully shut down if we may be the server.

      if (amServer1 && amCollecting) {
        //We are the server and might have just one anohter election.
        //Wait till this election is over 
        doSleep((long)(1.5*Constants.DISCOVERY_ELECTION_DURATION));
 
        
      } else if (!amServer1 && amCollecting) {
        //We might have just one anohter election.
        //Wait till this election is over 
        doSleep((long)(1.5*Constants.DISCOVERY_ELECTION_DURATION));

        synchronized(lock) {
          amServer2 = (mode == SERVER);
          if (amServer2) {
            serverComponent.prepareToDie();
          } 
        }
      }

      if (amServer1 || amServer2) {
        mainHandler.doStartElection();
        //Sleep through the election
        doSleep((long)(2*Constants.DISCOVERY_ELECTION_DURATION));
      } 
    } 

    synchronized(lock) {
      ds = serverRemove();
    }       
    if (ds!=null) {
      serverShutdown(ds);
    }

    synchronized(lock) {
      if (null != listenMaintainer) {
        listenMaintainer.cancel();
      }
      
      if (Constants.DEBUG_DISCOVERY ) {
        log.log(this,"Stopping");
      }
    
      if (null != multicastMaintainer) {
        multicastMaintainer.cancel();
      }
      if (null != electionTimerNotification) {
        electionTimerNotification.cancel();
      }
      if (null != boundTimerNotification) {
        boundTimerNotification.cancel();
      }
    }
  }

  /** Sleep for the given number of milliseconds.  Used in stop() and NOWHERE else. */
  private void doSleep(long millis) {
    try {
      SystemUtilities.sleep(millis);
    } catch (InterruptedException iex) {
    }
  }
}
 
