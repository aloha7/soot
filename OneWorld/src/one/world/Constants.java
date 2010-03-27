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

package one.world;

import one.world.binding.Duration;

/**
 * Definition of global constants for <i>one.world</i>. Note that most
 * constants defined by this class can be customized through system
 * properties.
 *
 * @version  $Revision: 1.38 $
 * @author   Robert Grimm
 */
public final class Constants {

  /** Hide constructor. */
  private Constants() {
    // Nothing to do.
  }


  /**
   * The flag for whether event processing in the core system should
   * be use extra validation and print debug messages. This constant
   * can be specified using the "<code>one.world.debug.events</code>"
   * system property. If this system property is not defined or not
   * "<code>true</code>" (case insensitive), the default is
   * <code>false</code>.  Note that this flag can also be set by
   * specifying the "<code>one.world.debug</code>" system property as
   * "<code>true</code>".
   */
  public static final boolean DEBUG_EVENTS;

  /**
   * The flag for whether {@link one.world.core.Environment
   * environment operations} should print debug messages. This
   * constant can be specified using the
   * "<code>one.world.debug.environment</code>" system property. If
   * this system property is not defined or not "<code>true</code>"
   * (case insensitive), the default is <code>false</code>.  Note that
   * this flag can also be set by specifying the
   * "<code>one.world.debug</code>" system property as
   * "<code>true</code>".
   */
  public static final boolean DEBUG_ENVIRONMENT;

  /**
   * The flag for whether the {@link one.world.io.TupleStore tuple
   * store} should print debug messages. This constant can be
   * specified using the "<code>one.world.debug.tuple.store</code>"
   * system property. If this system property is not defined or not
   * "<code>true</code>" (case insensitive), the default is
   * <code>false</code>.  Note that this flag can also be set by
   * specifying the "<code>one.world.debug</code>" system property as
   * "<code>true</code>".
   */
  public static final boolean DEBUG_TUPLE_STORE;

  /**
   * The flag for whether {@link one.world.core.Animator.Controller
   * animator controllers} should print debug messages. This constant
   * can be specified using the
   * "<code>one.world.debug.thread.control</code>" system property. If
   * this system property is not defined or not "<code>true</code>"
   * (case insensitive), the default is <code>false</code>.  Note that
   * this flag can also be set by specifying the
   * "<code>one.world.debug</code>" system property as
   * "<code>true</code>".
   */
  public static final boolean DEBUG_THREAD_CONTROL;

  /**
   * The flag for whether {@link one.world.util.Timer timers} should
   * print debug messages. This constant can be specified using the
   * "<code>one.world.debug.timer</code>" system property. If this
   * system property is not defined or not "<code>true</code>" (case
   * insensitive), the default is <code>false</code>.  Note that this
   * flag can also be set by specifying the
   * "<code>one.world.debug</code>" system property as
   * "<code>true</code>".
   */
  public static final boolean DEBUG_TIMER;

  /**
   * The flag for whether structured I/O networking should print debug
   * messages. This constant can be specified using the
   * "<code>one.world.debug.network</code>" system property. If this
   * system property is not defined or not "<code>true</code>" (case
   * insensitive), the default is <code>false</code>.  Note that this
   * flag can also be set by specifying the
   * "<code>one.world.debug</code>" system property as
   * "<code>true</code>".
   */
  public static final boolean DEBUG_NETWORK;

  /**
   * The flag for whether basic REP should print debug messages. This
   * constant can be specified using the
   * "<code>one.world.debug.rep</code>" system property. If this
   * system property is not defined or not "<code>true</code>" (case
   * insensitive), the default is <code>false</code>.  Note that this
   * flag can also be set by specifying the
   * "<code>one.world.debug</code>" system property as
   * "<code>true</code>".
   */
  public static final boolean DEBUG_REP;

  /**
   * The flag for whether discovery should print debug messages. This
   * constant can be specified using the
   * "<code>one.world.debug.discovery</code>" system property. If this
   * system property is not defined or not "<code>true</code>" (case
   * insensitive), the default is <code>false</code>.  Note that this
   * flag can also be set by specifying the
   * "<code>one.world.debug</code>" system property as
   * "<code>true</code>".
   */
  public static final boolean DEBUG_DISCOVERY;

  /** The major version of this release of <i>one.world</i>. */
  public static final int     MAJOR_VERSION = 0;

  /** The minor version of this release of <i>one.world</i>. */
  public static final int     MINOR_VERSION = 7;

  /** The micro version of this release of <i>one.world</i>. */
  public static final int     MICRO_VERSION = 1;

  /**
   * The GMT build time for this release of <i>one.world</i>, represented
   * as a human readable string.
   */
  public static final String  BUILD_TIME    = Build.get();
  
  /**
   * The embedded <a
   * href="http://www.pendragon-software.com/pendragon/cm3/index.html">
   * CaffeineMark 3.0 </a> performance rating for the Java
   * virtual machine running <i>one.world</i>. This constant can be
   * specified using the "<code>caffeine.mark.rating</code>" system
   * property. If this system property is not defined, the default is
   * -1 (which is not a legal CaffeineMark rating).
   *
   * <p>A re-packaged version of the benchmark is available <a
   * href="http://one.cs.washington.edu/stuff/bench.jar">here</a>.
   * Simply download the file and execute<pre>
   *   java -jar bench.jar
   * </pre>in the same directory, which will run the benchmark and
   * print the overall performance rating.</p>
   */
  public static final long    CAFFEINE_MARK_RATING;

  /**
   * The default timeout for synchronous invocations in
   * milliseconds. This constant can be specified using the
   * "<code>one.world.synchronous.timeout</code>" system property.  If
   * this system property is not defined, the default is 1 minute.
   */
  public static final long    SYNCHRONOUS_TIMEOUT;

  /**
   * The default timeout for receiving responses from remote events
   * sent over the network. This constant can be specified using the
   * "<code>one.world.network.timeout</code>" system property.  If
   * this system property is not defined, the default is 1 minute.
   */
  public static final long    NETWORK_TIMEOUT;

  /**
   * The default capacity for event queues used by animators. This
   * constant can be specified using the
   * "<code>one.world.animator.capacity</code>" system property. If
   * this system property is not defined, the default is 100.
   *
   * @see  one.world.core.Animator
   */
  public static final int     ANIMATOR_CAPACITY;

  /**
   * The minimum number of threads used by concurrent animators. This
   * constant can be specified using the
   * "<code>one.world.animator.min.threads</code>" system property. If
   * this system property is not defined, the default is 1.
   *
   * @see  one.world.core.Animator
   */
  public static final int     ANIMATOR_MIN_THREADS;

  /**
   * The maximum number of threads used by concurrent animators. This
   * constant can be specified using the
   * "<code>one.world.animator.max.threads</code>" system property. If
   * this system property is not defined, the default is 20.
   *
   * @see  one.world.core.Animator
   */
  public static final int     ANIMATOR_MAX_THREADS;

  /**
   * The idle time for concurrent animators. When a concurrent
   * animator's thread is idle for this time and has more threads than
   * the minimum number of threads, the thread is terminated. This
   * constant can be specified using the
   * "<code>one.world.animator.idle.time</code>" system property. If
   * this system property is not defined, the default is 5 seconds.
   *
   * @see  one.world.core.Animator
   */
  public static final long    ANIMATOR_IDLE_TIME;

  /**
   * The time between scans for the animator controller. This constant
   * can be specified using the
   * "<code>one.world.animator.scan.time</code>" system property. If
   * this system property is not defined, the default is 2 seconds.
   *
   * @see  one.world.core.Animator.Controller
   */
  public static final long    ANIMATOR_SCAN_TIME;

  /**
   * The busy threshold for adding new threads to a concurrent
   * animator. This constant can be specified using the
   * "<code>one.world.animator.threshold</code>" system property. If
   * this system property is not defined, the default is half of the
   * animator capacity.
   *
   * @see   #ANIMATOR_CAPACITY
   * @see   one.world.core.Animator.Controller
   */
  public static final int     ANIMATOR_THRESHOLD;

  /**
   * The number of threads to add to a concurrent animator if its
   * queue is over the busy threshold. This constant can be specified
   * using the "<code>one.world.animator.add.threads</code>" system
   * property. If this system property is not defined, the default is
   * 1.
   *
   * @see   #ANIMATOR_THRESHOLD
   * @see   one.world.core.Animator.Controller
   */
  public static final int     ANIMATOR_ADD_THREADS;

  /**
   * The minimum number of threads used by the root environment. This
   * constant can be specified using the
   * "<code>one.world.animator.root.min.threads</code>" system
   * property. If this system property is not defined, the default is
   * 5.
   *
   * @see  one.world.core.Animator
   */
  public static final int     ANIMATOR_ROOT_MIN_THREADS;

  /**
   * The maximum number of threads used by the root environment. This
   * constant can be specified using the
   * "<code>one.world.animator.root.max.threads</code>" system
   * property. If this system property is not defined, the default is
   * 50.
   *
   * @see  one.world.core.Animator
   */
  public static final int     ANIMATOR_ROOT_MAX_THREADS;

  /**
   * The busy threshold for adding new threads to the root
   * environment's animator. This constant can be specified using the
   * "<code>one.world.animator.root.threshold</code>" system
   * property. If this system property is not defined, the default is
   * a third of the animator capacity.
   *
   * @see   #ANIMATOR_CAPACITY
   * @see   one.world.core.Animator.Controller
   */
  public static final int     ANIMATOR_ROOT_THRESHOLD;

  /**
   * The number of threads to add to the root environment's animator
   * if its queue is over the busy threshold. This constant can be
   * specified using the
   * "<code>one.world.animator.root.add.threads</code>" system
   * property. If this system property is not defined, the default is
   * 2.
   *
   * @see   #ANIMATOR_ROOT_THRESHOLD
   * @see   one.world.core.Animator.Controller
   */
  public static final int     ANIMATOR_ROOT_ADD_THREADS;

  /**
   * The period for repeated timer notifications. This constants is
   * used for setting up repeated timer notifications so that timed
   * notifications are not lost, even if all event queues are full.
   * It can be specified using the
   * "<code>one.world.timer.period</code>" system property. If this
   * system propery is not defined, the default is 1 second.
   *
   * @see  one.world.util.Timer
   */
  public static final long    TIMER_PERIOD;

  /**
   * The minimum duration for leases. This constant can be specified
   * using the "<code>one.world.lease.min.duration</code>" system
   * property. If this system property is not defined, the default
   * is 1 second.
   *
   * @see  one.world.binding.LeaseManager
   */
  public static final long    LEASE_MIN_DURATION;

  /**
   * The default duration for leases. This constant can be specified
   * using the "<code>one.world.lease.duration</code>" system
   * property. If this system property is not defined, the default
   * is 1 minute.
   *
   * @see  one.world.binding.LeaseManager
   */
  public static final long    LEASE_DEFAULT_DURATION;

  /**
   * The maximum duration for leases. This constant can be specified
   * using the "<code>one.world.lease.max.duration</code>" system
   * property.  If this system property is not defined, the default is
   * 10 minutes.
   *
   * @see  one.world.binding.LeaseManager
   */
  public static final long    LEASE_MAX_DURATION;

  /**
   * The default number of retries when performing an operation. This
   * constant can be specified using the
   * "<code>one.world.operation.retries</code>" system property. The
   * default is 3.
   *
   * @see  one.world.util.Operation
   */
  public static final int     OPERATION_RETRIES;

  /**
   * The default timeout when performing an operation. This constant
   * can be specified using the
   * "<code>one.world.operation.timeout</code>" system property. The
   * default is the synchronous timeout.
   *
   * @see  #SYNCHRONOUS_TIMEOUT
   * @see  one.world.util.Operation
   */
  public static final long    OPERATION_TIMEOUT;

  /**
   * The name of the meta-data field specifying the requesting
   * environment's ID. Events sent to an environment's request
   * exported event handler are annotated with the sending
   * environment's ID. The name of this meta-data field is
   * "<code>one.world.requestor.id</code>".
   */
  public static final String  REQUESTOR_ID = "one.world.requestor.id";

  /**
   * The name of the meta-data field specifying the requesting
   * environment's IP address. Events sent through REP are annotated
   * with the sending environment's ID, IP address, and port number. The 
   * name of this meta-data field is
   * "<code>one.world.requestor.address</code>".
   *
   * @see  #REQUESTOR_ID
   * @see  #REQUESTOR_PORT
   */
  public static final String  REQUESTOR_ADDRESS = "one.world.requestor.address";

  /**
   * The name of the meta-data field specifying the requesting
   * environment's port number. Events sent through REP are annotated
   * with the sending environment's ID, IP address, and port number. 
   * The name of this meta-data field is
   * "<code>one.world.requestor.port</code>".
   *
   * @see  #REQUESTOR_ID
   * @see  #REQUESTOR_ADDRESS
   */
  public static final String  REQUESTOR_PORT = "one.world.requestor.port";
  
  /**
   * The default port for structured I/O communication channels. This
   * constant can be specified using the "<code>one.world.port</code>"
   * system property. If this system property is not defined, the
   * default port is 5101.
   *
   * @see  one.world.io.SioResource
   */
  public static final int     PORT;

  /**
   * The port on which the root shell accepts telnet connections. This
   * constant can be specified using the
   * "<code>one.world.shell.port</code>" system property. If this
   * system property is not defined, the default port is the default
   * telnet port (23). Note that the root shell only accepts telnet
   * connections if the "<code>one.world.shell.pwd</code>" system
   * property is defined.
   *
   * @see   Shell
   */
  public static final int     SHELL_PORT;

  /** 
   * The port on which to receive remote events.  This constant can be
   * specified using the "<code>one.world.rep.port</code>" system
   * property.  If this system property is not defined, the default
   * port is 5102.
   *
   * @see one.world.rep
   */
  public static final int     REP_PORT;

  /** 
   * Indicates whether local events must be delivered via the network
   * stack rather than directly.  This constant can be
   * specified using the "<code>one.world.rep.force.network</code>" system
   * property.  If this system property is not defined, the default
   * port is <code>false</code>.
   *
   * @see one.world.rep
   */
  public static final boolean REP_FORCE_NETWORK;

  /**
   * The frequency, in milliseconds, with which cached REP connections
   * will be checked to see if they are still active.  This constant
   * can be specified using the
   * "<code>one.world.rep.cache.timeout</code>" system property.  If
   * this system property is not defined, the default cache timeout is
   * one minute.
   *
   * @see one.world.rep
   */
  public static final long    REP_CACHE_TIMEOUT;

  /**
   * The maximum number of retries for sending a remote event or a
   * resolution request. This constant can be specified using the
   * "<code>one.world.rep.max.retries</code>" system property.  If
   * this system property is not defined, the default maximum number
   * of retries is 1.
   *
   * @see one.world.rep
   */
  public static final int     REP_MAX_RETRIES;

  /** 
   * The number of remote events to queue while waiting to establish a
   * connection to a remote endpoint.  If any more events are received
   * before the connection is established, the response will be a
   * {@link one.world.core.NoBufferSpaceException}.  This constant can
   * be specified using the
   * "<code>one.world.rep.queue.capacity</code>" system property.  If
   * this system property is not defined, the default queue size is
   * {@link #ANIMATOR_CAPACITY}.
   *
   * @see one.world.rep
   */
  public static final int     REP_QUEUE_CAPACITY;

  /** 
   * The name of the meta-data field specifying the number of retries
   * when sending a remote event or a resolution request.  The name of
   * this meta-data field is "<code>one.world.rep.retries</code>".
   *
   * @see one.world.rep
   */
  public static final String  REP_RETRIES = "one.world.rep.retries";

  /**
   * The IP multicast address for server announcements from a
   * discovery server.  This address should be used with the {@link
   * #DISCOVERY_ANNOUNCE_PORT}. This constant can be specified using
   * the "<code>one.world.discovery.announce.addr</code>" system
   * property. The default is 230.0.0.1.
   */
  public static final String  DISCOVERY_ANNOUNCE_ADDR;

  /**
   * The port number for periodic advertisements from a discovery
   * server. This port number should be used with the {@link
   * #DISCOVERY_ANNOUNCE_ADDR}. This constant can be specified using
   * the "<code>one.world.discovery.announce.port</code> system
   * property. The default is 5104. 
   *
   * <p>This must not conflict with any other port number.  Changing
   * the discovery server announcement port without changing the
   * {@link #DISCOVERY_ELECTION_PORT election port} could result in
   * strange behavior such as the elected server not being visible to
   * the other discovery clients on the network.</p>
   */
  public static final int     DISCOVERY_ANNOUNCE_PORT;

  /**
   * The IP multicast address for calling and performing elections.
   * This address should be used with the {@link
   * #DISCOVERY_ELECTION_PORT}.  This constant can be specified using
   * the "<code>one.world.discovery.election.addr</code>" system
   * property. The default is 230.0.0.1.
   */
  public static final String  DISCOVERY_ELECTION_ADDR;

  /**
   * The port number for election. This address should be used with
   * the {@link #DISCOVERY_ELECTION_ADDR}. This constant can be
   * specified using the
   * "<code>one.world.discovery.election.port</code>" system
   * property. The default is 5105.
   */
  public static final int     DISCOVERY_ELECTION_PORT;

  /**
   * The period between advertisments from a discovery server in
   * milliseconds.  The advertisements occur on the multicast channel
   * specified by the <{@link #DISCOVERY_ANNOUNCE_ADDR}, {@link
   * #DISCOVERY_ANNOUNCE_PORT}> pair. This constant can be specified
   * using the "<code>one.world.discovery.announce.period</code>"
   * system property. The default is two seconds.
   */
  public static final long    DISCOVERY_ANNOUNCE_PERIOD;

  /**
   * The value sent by this discovery server for elections.  This
   * should normally be -1 to indicate that a heuristic based on
   * the node uptime and memory should be used.  If any other value,
   * this value will be used during discovery elections.  This is
   * primarily useful as a testing aid. This constant can be sepecified
   * using the "<code>one.world.discovery.announce.value</code>"
   * system property.  The default is -1.
   */
  public static final long    DISCOVERY_ANNOUNCE_VALUE;
 
  /** 
   * The length of time to wait for the election to complete (in
   * milliseconds). This constant can be specified using the
   * "<code>one.world.discovery.election.duration</code>" system
   * property. The default is one second.
   */
  public static final long    DISCOVERY_ELECTION_DURATION;

  /** 
   * The length of time to wait before calling a discovery server
   * election (in milliseconds). This constant can be specified using
   * the "<code>one.world.discovery.election.calltime</code>" system
   * property. The default is twice the period of an announcemnt.
   *
   * @see #DISCOVERY_ANNOUNCE_PERIOD
   */
  public static final long    DISCOVERY_ELECTION_CALL_TIME;

  /**
   * The name of the meta-data field specifying which discovery server
   * forwarded this event.
   *
   * This is used to squelch UnknownResourceException events for stale
   * bindings.
   */
  public static final String    DISCOVERY_SOURCE_SERVER = 
                                      "one.world.discovery.server";

  /**
   * The name of the meta-data field specifying which binding
   * was the source of this forwarded event.
   *
   * This is used to squelch UnknownResourceException events for stale
   * bindings.
   */
  public static final String    DISCOVERY_BINDING =
                                      "one.world.discovery.binding";

  /**
   * The threshold for representing arbitrary byte arrays as a list of
   * {@link one.world.data.Chunk chunks} instead of a single {@link
   * one.world.data.BinaryData binary data tuple}. This constant can
   * be specified using the
   * "<code>one.world.chunking.threshold</code>" system property.
   */
  public static final int       CHUNKING_THRESHOLD;

  /** Initialize the constants. */
  static {
    if (getBoolean("one.world.debug")) {
      DEBUG_EVENTS            = true;
      DEBUG_ENVIRONMENT       = true;
      DEBUG_TUPLE_STORE       = true;
      DEBUG_THREAD_CONTROL    = true;
      DEBUG_TIMER             = true;
      DEBUG_NETWORK           = true;
      DEBUG_REP               = true;
      DEBUG_DISCOVERY         = true;
    } else {
      DEBUG_EVENTS            = getBoolean("one.world.debug.events");
      DEBUG_ENVIRONMENT       = getBoolean("one.world.debug.environment");
      DEBUG_TUPLE_STORE       = getBoolean("one.world.debug.tuple.store");
      DEBUG_THREAD_CONTROL    = getBoolean("one.world.debug.thread.control");
      DEBUG_TIMER             = getBoolean("one.world.debug.timer");
      DEBUG_NETWORK           = getBoolean("one.world.debug.network");
      DEBUG_REP               = getBoolean("one.world.debug.rep");
      DEBUG_DISCOVERY         = getBoolean("one.world.debug.discovery");
    }
    CAFFEINE_MARK_RATING      = getLong("caffeine.mark.rating",          -1);
    SYNCHRONOUS_TIMEOUT       = getLong("one.world.synchronous.timeout",
                                        Duration.MINUTE);
    NETWORK_TIMEOUT           = getLong("one.world.network.timeout",
                                        Duration.MINUTE);
    ANIMATOR_CAPACITY         = getInt("one.world.animator.capacity",    100);
    ANIMATOR_MIN_THREADS      = getInt("one.world.animator.min.threads",  1);
    int number                = getInt("one.world.animator.max.threads", 20);
    if (ANIMATOR_MIN_THREADS > number) {
      number                  = ANIMATOR_MIN_THREADS;
    }
    ANIMATOR_MAX_THREADS      = number;
    ANIMATOR_IDLE_TIME        = getLong("one.world.animator.idle.time",
                                        5 * Duration.SECOND);
    ANIMATOR_SCAN_TIME        = getLong("one.world.animator.scan.time",
                                        2 * Duration.SECOND);
    ANIMATOR_THRESHOLD        = getInt("one.world.animator.threshold",
                                       ANIMATOR_CAPACITY / 2);
    ANIMATOR_ADD_THREADS      = getInt("one.world.animator.add.threads", 1);
    ANIMATOR_ROOT_MIN_THREADS = getInt("one.world.animator.root.min.threads",
                                       5);
    number                    = getInt("one.world.animator.root.max.threads",
                                       50);
    if (ANIMATOR_ROOT_MIN_THREADS > number) {
      number                  = ANIMATOR_ROOT_MIN_THREADS;
    }
    ANIMATOR_ROOT_MAX_THREADS = number;
    ANIMATOR_ROOT_THRESHOLD   = getInt("one.world.animator.root.threshold",
                                       ANIMATOR_CAPACITY / 3);
    ANIMATOR_ROOT_ADD_THREADS = getInt("one.world.animator.root.add.threads",
                                       2);
    TIMER_PERIOD              = getLong("one.world.timer.period",
                                        Duration.SECOND);
    LEASE_MIN_DURATION        = getLong("one.world.lease.min.duration",
                                        Duration.SECOND);
    long duration             = getLong("one.world.lease.duration",
                                        Duration.MINUTE);
    if (LEASE_MIN_DURATION > duration) {
      duration                = LEASE_MIN_DURATION;
    }
    LEASE_DEFAULT_DURATION    = duration;
    duration                  = getLong("one.world.lease.max.duration",
                                        10 * Duration.MINUTE);
    if (LEASE_DEFAULT_DURATION > duration) {
      duration                = LEASE_DEFAULT_DURATION;
    }
    LEASE_MAX_DURATION        = duration;
    OPERATION_RETRIES         = getInt("one.world.operation.retries",    3);
    OPERATION_TIMEOUT         = getLong("one.world.operation.timeout",
                                        SYNCHRONOUS_TIMEOUT);
    int port                  = getInt("one.world.port",                 5101);
    if (65536 <= port) {
      port                    = 5101;
    } 
    PORT                      = port;
    port                      = getInt("one.world.shell.port",           23);
    if (65536 <= port) {
      port                    = 23;
    }
    SHELL_PORT                = port;
    port                      = getInt("one.world.rep.port",             5102);
    if (65536 <= port) {
      port                    = 5102;
    } 
    REP_PORT                  = port;
    REP_FORCE_NETWORK         = getBoolean("one.world.rep.force.network");
    REP_CACHE_TIMEOUT         = getLong("one.world.rep.cache.timeout",
                                        Duration.MINUTE);
    REP_MAX_RETRIES           = getInt("one.world.rep.max.retries",     1);
    REP_QUEUE_CAPACITY        = getInt("one.world.rep.queue.capacity",
                                       ANIMATOR_CAPACITY);
    DISCOVERY_ANNOUNCE_ADDR   =
      System.getProperty("one.world.discovery.announce.addr", "230.0.0.1");
    DISCOVERY_ANNOUNCE_PORT   = getInt("one.world.discovery.announce.port",
                                       5104);
    DISCOVERY_ANNOUNCE_PERIOD = getLong("one.world.discovery.announce.period",
                                        Duration.SECOND);
    DISCOVERY_ANNOUNCE_VALUE  = getLong("one.world.discovery.announce.value",
                                        -1);
    DISCOVERY_ELECTION_ADDR   =
      System.getProperty("one.world.discovery.election.addr", "230.0.0.1");
    DISCOVERY_ELECTION_PORT   = getInt("one.world.discovery.election.port",
                                       5105);
    DISCOVERY_ELECTION_DURATION = 
      getLong("one.world.discovery.election.duration", Duration.SECOND);
    DISCOVERY_ELECTION_CALL_TIME = 
      getLong("one.world.discovery.election.calltime", 
	      2 * DISCOVERY_ANNOUNCE_PERIOD);
    CHUNKING_THRESHOLD        = getInt("one.world.chunking.threshold",
                                       100 * 1024);
  }

  /**
   * Get the boolean value from the specified system property. This
   * method attempts to parse the system property with the specified
   * name. If the system property exists and if it is "<code>true</code>"
   * (case insensitive), then this method returns <code>true</code>.
   * Otherwise it returns <code>false</code>.
   *
   * @param   name  The name of the system property.
   * @return        <code>true</code> if the system property is
   *                defined and is "<code>true</code>" (case
   *                insensitive.
   */
  public static boolean getBoolean(String name) {
    return "true".equalsIgnoreCase(System.getProperty(name));
  }

  /**
   * Get the integer value from the specified system property.  This
   * method attempts to parse the system property with the specified
   * name. If the system property exists and if it represents a valid
   * positive integer, that integer is returned.  Otherwise, the
   * specified default value is returned.
   *
   * <p>The specified default must be a positive integer.</p>
   *
   * @param   name     The name of the system property.
   * @param   value    The default value.
   * @return           The corresponding positive integer.
   */
  public static int getInt(String name, int value) {
    String s = System.getProperty(name);
    int    n;

    try {
      n = Integer.parseInt(s);
    } catch (NumberFormatException x) {
      n = value;
    } catch (NullPointerException x) {
      n = value;
    }

    if (0 >= n) {
      n = value;
    }

    return n;
  }

  /**
   * Get the long value from the specified system property.  This
   * method attempts to parse the system property with the specified
   * name. If the system property exists and if it represents a valid
   * positive long, that long is returned.  Otherwise, the
   * specified default value is returned.
   *
   * <p>The specified default must be a positive long.</p>
   *
   * @param   name     The name of the system property.
   * @param   value    The default value.
   * @return           The corresponding positive long.
   */
  public static long getLong(String name, long value) {
    String s = System.getProperty(name);
    long   l;

    try {
      l = Long.parseLong(s);
    } catch (NumberFormatException x) {
      l = value;
    } catch (NullPointerException x) {
      l = value;
    }

    if (0 >= l) {
      l = value;
    }

    return l;
  }

}
