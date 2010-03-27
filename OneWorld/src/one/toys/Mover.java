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

package one.toys;

import java.net.InetAddress;
import java.net.UnknownHostException;

import one.util.Stats;

import one.world.binding.Duration;

import one.world.Constants;

import one.world.core.Component;
import one.world.core.ComponentDescriptor;
import one.world.core.Environment;
import one.world.core.Event;
import one.world.core.EventHandler;
import one.world.core.ExceptionalEvent;
import one.world.core.ExportedDescriptor;
import one.world.core.ImportedDescriptor;

import one.world.env.EnvironmentEvent;
import one.world.env.MoveRequest;

import one.world.util.AbstractHandler;
import one.world.util.SystemUtilities;

/**
 * Implementation of the mover application. The
 * mover application repeatedly moves itself across two or more nodes
 * in a round-robin fashion. The node on which this application is
 * originally activated is the application's home node. When
 * encountering an error or when being restored from a saved
 * check-point, the mover application always returns to the home node
 * and starts the next iteration. Clones of the mover application kill
 * themselves immediately.
 *
 * <p>The best way to stop the mover application is to set up a
 * "roadblock": Create an environment with the same name as the mover
 * application's environment in the root environment of one of the
 * nodes the mover application is not currently running on. This may
 * take a few attempts, especially when running the mover application
 * on only two nodes. When the mover application tries to move to the
 * node with the roadblock, it will fail, because an environment with
 * the same name already exists. After several attempts (with
 * exponentially increasing nap times), the mover application will
 * give up trying and exit.</p>
 *
 * <p><b>Imported and Exported Event Handlers</b></p>
 *
 * <p>Exported event handlers:<dl>
 *    <dt>main</dt>
 *    <dd>Handles environment events, linked to an environment's
 *        main imported event handler.
 *        </dd>
 * </dl></p>
 *
 * <p>Imported event handlers:<dl>
 *    <dt>request</dt>
 *    <dd>Handles environment events, linked to an environment's
 *        request exported event handler.
 *        </dd>
 * </dl></p>
 *
 * @version  $Revision: 1.3 $
 * @author   Robert Grimm 

 */
public final class Mover extends Component {

  // =======================================================================
  //                              Constants
  // =======================================================================

  /**
   * The maximum number of tries when moving to a node that already
   * has an environment with the same name. The mover application
   * retries, because when migrating an environment, the sending node
   * may still have remaining state of the migrated environment, even
   * if the environment has already been activated on the receiving
   * node.
   */
  private static final int  MAX_TRIES = 4;

  /** The minimum nap time. */
  private static final long MIN_NAP   = 1000;

 
  // =======================================================================
  //                           The main handler
  // =======================================================================

  /** The main exported event handler. */
  final class MainHandler extends AbstractHandler {

    /** Handle the specified event. */
    protected boolean handle1(Event e) {

      if (e instanceof EnvironmentEvent) {
        EnvironmentEvent ee = (EnvironmentEvent)e;

        if ((EnvironmentEvent.ACTIVATED == ee.type) ||
            (EnvironmentEvent.RESTORED  == ee.type)) {

          // Reset retry count and nap time.
          retryCount = 0;
          nap        = 0;

          if (isHome()) {
            // Start a new round-trip.
            location = 0;
            failure  = null;
            start    = SystemUtilities.currentTimeMillis();
            target   = 1;
            moveTo(nodes[1], ports[1]);
            return true;

          } else {
            // Get back home first.
            failure = new IllegalStateException(name + " not at home when " +
                                                "activated or restored");
            target  = 0;
            moveTo(nodes[0], ports[0]);
            return true;
          }

        } else if (EnvironmentEvent.MOVED == ee.type) {
          SystemUtilities.debug(name + " has arrived");

          // Reset retry count and nap time.
          retryCount = 0;
          nap        = 0;

          if (isHome()) {
            if (null == failure) {
              // We completed a successful round-trip.
              time();

            } else {
              // We saw some exceptional condition.
              failedTrips++;

              SystemUtilities.debug("Round-trip failed");
              SystemUtilities.debug(failure);
            }
            
            // Start a new round-trip.
            location = 0;
            failure  = null;
            start    = SystemUtilities.currentTimeMillis();
            target   = 1;
            moveTo(nodes[1], ports[1]);
            return true;

          } else {
            // Update location.
            location++;

            // Consistency check.
            if (! SystemUtilities.ipAddress().equals(nodes[location])) {
              SystemUtilities.debug(name + " has inconsistent internal state");
              SystemUtilities.debug("I'm committing suicide!");
              request.handle(new
                EnvironmentEvent(this, null, EnvironmentEvent.STOPPED,
                                 getEnvironment().getId()));
              return true;
            }

            // Move to next node.
            int next = location + 1;
            if (nodes.length <= next) {
              next = 0;
            }

            target = next;
            moveTo(nodes[next], ports[next]);
            return true;
          }

        } else if (EnvironmentEvent.CLONED == ee.type) {
          // We won't be cloned; commit suicide.
          request.handle(new
            EnvironmentEvent(this, null, EnvironmentEvent.STOPPED,
                             getEnvironment().getId()));
          return true;

        } else if (EnvironmentEvent.STOP == ee.type) {
          // We are done.
          respond(e, new
            EnvironmentEvent(this, null, EnvironmentEvent.STOPPED,
                             getEnvironment().getId()));
          return true;
        }

      } else if (e instanceof ExceptionalEvent) {
        ExceptionalEvent ee = (ExceptionalEvent)e;

        SystemUtilities.debug("Encountered exceptional condition " +
                              ee.x.toString());
        SystemUtilities.debug(ee.x);

        // Test if the move failed because of an environment with the
        // same name and, if so, try again.
        if ((ee.x instanceof IllegalArgumentException) &&
            ee.x.getMessage().equals("Environment with same name already " +
                                     "exists (" + getEnvironment().getName() +
                                     ")")) {
          retryCount++;
          if (MAX_TRIES > retryCount) {
            SystemUtilities.debug("Trying again...");

            moveTo(nodes[target], ports[target]);
            return true;

          } else {
            SystemUtilities.debug("Tried " + retryCount +
                                  " times, giving up...");

            request.handle(new
              EnvironmentEvent(this, null, EnvironmentEvent.STOPPED,
                               getEnvironment().getId()));
            return true;
          }

        } else if (isHome()) {
          failedTrips++;

          // Start a new round-trip.
          location = 0;
          failure  = null;
          start    = SystemUtilities.currentTimeMillis();
          target   = 1;
          moveTo(nodes[1], ports[1]);
          return true;

        } else {
          // Get back home first.
          failure = ee.x;
          target  = 0;
          moveTo(nodes[0], ports[0]);
          return true;
        }
      }

      return false;
    }

    /**
     * Update the time statistics. This method updates the round-trip
     * timing statistics and prints the result to the console. This
     * method may only be invoked on the home node after a successful
     * round-trip.
     */
    private void time() {
      long trip = SystemUtilities.currentTimeMillis() - start;

      duration += trip;
      roundTrips++;

      if (recordStats) {
        double avg;
        double stddev;

        stats.add(trip);
        avg    = stats.average();
        stddev = stats.stdev();
        SystemUtilities.debug("All trips: " + avg + " +- "+stddev);
      }

      SystemUtilities.debug("Last round-trip took " + Duration.format(trip));
      SystemUtilities.debug("Average of " + roundTrips + " round-trips is " +
                            Duration.format(duration / roundTrips));
      SystemUtilities.debug(failedTrips + " round-trips failed");
    }

    /**
     * Determine whether the current node is the home node. This node
     * is the home node if it has the same IP address as the current
     * node and REP is exporting event handlers on the same port.
     *
     * @return   <code>true</code> if this node is the home node.
     */
    private boolean isHome() {
      return (nodes[0].equals(SystemUtilities.ipAddress()) &&
              (ports[0] == Constants.REP_PORT));
    }

    /**
     * Move to the specified node and port. The specified node and
     * port combination must be different from this node and REP port.
     *
     * @see     Constants#REP_PORT
     *
     * @param   node  The node to move to.
     * @param   port  The port number.
     */
    private void moveTo(String node, int port) {
      // Set up nap time.
      if (0 == retryCount) {
        if (3 > nodes.length) {
          // We always nap for two nodes.
          nap = MIN_NAP;
        }
      } else {
        if (0 == nap) {
          nap = MIN_NAP / 2;
        }
        nap *= 2;
      }

      if (0 != nap) {
        SystemUtilities.debug("Napping for " + Duration.format(nap));
        try {
          SystemUtilities.sleep(nap);
        } catch (InterruptedException x) {
          // Ignore.
        }
      }

      SystemUtilities.debug(name + " moving to " + node + ":" + port);

      String url = "sio://" + node + ":" + port + "/?type=storage";

      request.handle(new
        MoveRequest(this, null, getEnvironment().getId(), url, false));
    }

  }


  // =======================================================================
  //                           Descriptors
  // =======================================================================

  /** The component descriptor. */
  private static final ComponentDescriptor SELF =
    new ComponentDescriptor("one.toys.Mover",
                            "The main mover application component",
                            false);

  /** The exported event handler descriptor for the main handler. */
  private static final ExportedDescriptor MAIN =
    new ExportedDescriptor("main",
                           "The main exported event handler",
                           new Class[] { EnvironmentEvent.class },
                           null,
                           false);

  /** The imported event handler descriptor for the request handler. */
  private static final ImportedDescriptor REQUEST =
    new ImportedDescriptor("request",
                           "The request imported event handler",
                           new Class[] { MoveRequest.class },
                           null,
                           false,
                           true);


  // =======================================================================
  //                           Instance fields
  // =======================================================================

  /**
   * The main exported event handler.
   *
   * @serial  Must not be <code>null</code>.
   */
  final EventHandler       main;

  /**
   * The request imported event handler.
   *
   * @serial  Must not be <code>null</code>.
   */
  final Component.Importer request;

  /**
   * The name of this mover application.
   *
   * @serial  Must not be <code>null</code>.
   */
  final String             name;

  /**
   * The IP addresses of the nodes visited by this mover application.
   *
   * @serial  Must be an array with two or more entries and of the same
   *          size as the ports field. The combination of node address
   *          and port at a given index must not be the same as the
   *          previous entry (or the first entry for the last entry).
   */
  final String[]           nodes;

  /**
   * The ports of the nodes visited by this mover application. These
   * are the ports REP exports event handlers on.
   *
   * @serial  Must be an array with two or more entries that are
   *          valid port numbers and of the same size as the nodes
   *          field. The combination of node address and port
   *          at a given index must not be the same as the previous
   *          entry (or the first entry for the last entry).
   */
  final int[]              ports;

  /**
   * The current location.
   *
   * @serial  Must be a valid index into <code>nodes</code>.
   */
  int                      location;

  /**
   * The total time. This is the sum of the durations of all
   * successfully completed round-trips.
   *
   * @serial  Must be a valid duration in milliseconds.
   */
  long                     duration;

  /**
   * The number of successfully completed round-trips.
   *
   * @serial  Must be a non-negative number.
   */
  int                      roundTrips;
  
  /**
   * The start time of the current round-trip.
   *
   * @serial  Must be a valid time in milliseconds.
   */
  long                     start;

  /**
   * Hold round trip timing statistics.  
   *
   * This field is not used unless <code>recordStats == true</code>
   * (<code>false</code> by default).
   */
  Stats                    stats;

  /** 
   * Should round trip statistics be kept.
   * 
   * If <code>true</code>, round strip statistics are recorded in 
   * <code>stats</code>.
   */
  boolean                  recordStats = false;

  /**
   * The cause of a failure. This field is set by a node upon
   * encountering an exceptional condition and before moving back to
   * the home node.
   *
   * @serial
   */
  Throwable                failure;

  /**
   * The number of failed round-trips.
   *
   * @serial  Must be a non-negative number.
   */
  int                      failedTrips;

  /**
   * The current number of retries when moving to a node that already
   * has an environment with the same name.
   *
   * @serial  Must be smaller than <code>MAX_RETRIES</code>.
   */
  int                      retryCount;

  /**
   * The nap time before trying to move to a node.
   *
   * @serial  Must be a valid duration in milliseconds.
   */
  long                     nap;

  /**
   * The index into <code>nodes</code> when retrying to move to a 
   * node.
   *
   * @serial  Must be a valid index into <code>nodes</code>.
   */
  int                      target;


  // =======================================================================
  //                           Constructor
  // =======================================================================

  /**
   * Create a new instance of <code>Mover</code>.
   *
   * @param   env    The environment for the new instance.
   * @param   hosts  The hosts (in standard URL notation
   *                 <code>&lt;address&gt;[:&lt;port&gt;]</code>) to be
   *                 visited by this mover.
   * @throws  IllegalStateException
   *                 Signals that this node does not have an IP
   *                 address.
   * @throws  IllegalArgumentException
   *                 Signals that the list of nodes is empty, that
   *                 an invalid port number has been specified, or
   *                 that one of the specified hosts is the same
   *                 as the previous host.
   * @throws  UnknownHostException
   *                 Signals that one of the host addresses cannot be
   *                 resolved.
   */
  public Mover(Environment env, String[] hosts)
    throws UnknownHostException {

    super(env);
    main = declareExported(MAIN, new MainHandler());
    request = declareImported(REQUEST);

    // Set up name.
    name = "Mover " + env.getId();

    if (recordStats) {
      stats = new Stats();
    }

    // Prepare to process hosts.
    if (1 > hosts.length) {
      throw new IllegalArgumentException("List of hosts empty");
    }

    String address = SystemUtilities.ipAddress();
    if (null == address) {
      throw new IllegalStateException("Home node without IP address");
    }

    nodes    = new String[hosts.length + 1];
    ports    = new int   [hosts.length + 1];
    nodes[0] = address;
    ports[0] = Constants.REP_PORT;

    // Iterator over hosts.
    for (int i=0; i<hosts.length; i++) {
      String node = hosts[i];
      int    port = Constants.REP_PORT;
      int    idx  = node.indexOf(':');

      // Parse port number.
      if (-1 != idx) {
        try {
          port = Integer.parseInt(node.substring(idx + 1));
        } catch (NumberFormatException x) {
          throw new IllegalArgumentException("Not a valid port number (" +
                                             node.substring(idx + 1) + ")");
        }

        if ((0 >= port) || (65536 <= port)) {
          throw new IllegalArgumentException("Not a valid port number (" +
                                             port + ")");
        }

        node = node.substring(0, idx);
      }

      // Resolve address.
      node = InetAddress.getByName(node).getHostAddress();

      // Make sure the current host is not the same as the previous host.
      if (((hosts.length != i + 1) &&
           nodes[i].equals(node)   &&
           (ports[i] == port)) ||
          ((hosts.length == i + 1) &&
           nodes[0].equals(node)   &&
           (ports[0] == port))) {
        throw new IllegalArgumentException("Host " + node + ":" + port +
                                           "same as previous host");
      }

      // Store away.
      nodes[i + 1] = node;
      ports[i + 1] = port;
    }
  }


  // =======================================================================
  //                           Component support
  // =======================================================================

  /** Get the component descriptor. */
  public ComponentDescriptor getDescriptor() {
    return (ComponentDescriptor)SELF.clone();
  }


  // =======================================================================
  //                              Initializer
  // =======================================================================

  /**
   * Initialize the mover application. The closure must be a string
   * array that specifies the hosts to be visited by the newly created
   * mover application (omitting the current, that is, home
   * host). Each host is specified using the standard URL notation of
   * <code>&lt;address&gt;[:&lt;port&gt;]</code>. The optional port
   * specifies the default REP port for that instance of
   * <i>one.world</i>.
   *
   * @param   env      The environment for the new mover application.
   * @param   closure  The closure for the new mover application, which
   *                   specifies the hosts to be visited.
   * @throws  IllegalStateException
   *                   Signals that this node does not have an IP
   *                   address.
   * @throws  IllegalArgumentException
   *                   Signals that the closure is not a string array
   *                   or an empty array or that one of the specified
   *                   hosts is the same as the previous host.
   * @throws  UnknownHostException
   *                   Signals that one of the hosts cannot be resolved.
   */
  public static void init(Environment env, Object closure) 
    throws UnknownHostException {

    if (! (closure instanceof String[])) {
      throw new IllegalArgumentException("Closure not a string array");
    }

    Mover comp = new Mover(env, (String[])closure);

    env.link("main", "main", comp);
    comp.link("request", "request", env);
  }

}
