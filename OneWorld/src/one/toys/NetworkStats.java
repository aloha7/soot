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

import java.io.IOException;
import java.io.ObjectOutputStream;

import one.gui.Application;
import one.gui.GuiUtilities;

import one.util.Guid;

import one.world.binding.Duration;

import one.world.core.DynamicTuple;
import one.world.core.Environment;
import one.world.core.Event;
import one.world.core.EventHandler;
import one.world.core.Component;
import one.world.core.*;
import one.world.env.*;
import one.world.io.DatagramIO;
import one.util.CountOutputStream;
import one.util.CountInputStream;

import one.world.util.AbstractHandler;
import one.world.util.SystemUtilities;
import one.world.util.Timer;
import one.util.Stats;

/**
 * Record statistics about network throughput of this node.  This 
 * program requires that special hooks(commented out by default) be
 * included in the one.world kernel.
 *
 * <p>Usage:<pre>
 *    NetworkStats INTERVAL DURATION 
 *
 *    Interval is the sampling interval in milliseconds and
 *    Duration is the total length of time to sample(in milliseconds).     
 * </pre></p>
 *
 *
 * @version  $$
 * @author  Eric Lemar 
 */

public final class NetworkStats extends one.world.core.Component {

  EventHandler main;
  EventHandler request;

  private static final ComponentDescriptor SELF =
    new ComponentDescriptor("one.toys.NetworkStats",
                            "The main mover application component",
                            false);

  public ComponentDescriptor getDescriptor() {
    return (ComponentDescriptor)SELF.clone();
  }


 private static final ExportedDescriptor MAIN =
   new ExportedDescriptor("main",
                          "The main exported event handler",
                          new Class[] { },
                          null,
                          false);

 /** The imported event handler descriptor for the request handler. */
 private static final ImportedDescriptor REQUEST =
   new ImportedDescriptor("request",
                          "The request imported event handler",
                          new Class[] { },
                          null,
                          false,
                          true);




  /** Implementation of the count update handler. */
  final class UpdateHandler extends AbstractHandler {

    /** Handle the specified event. */
    protected boolean handle1(Event e) {
      if (e instanceof DynamicTuple) {
        if (curEntry<totEntry) {
          stats[curEntry].timeMillis    = SystemUtilities.currentTimeMillis();
          stats[curEntry].tcpWriteBytes = CountOutputStream.clearWriteCount();
          stats[curEntry].udpWritePackets = DatagramIO.getPacketWriteCount();
          stats[curEntry].udpNormalWritePackets = DatagramIO.getNormalPacketWriteCount();
          DatagramIO.clearWriteCounts();

          stats[curEntry].tcpReadBytes = CountInputStream.clearReadCount();
          stats[curEntry].udpReadPackets = DatagramIO.getPacketReadCount();
          stats[curEntry].udpNormalReadPackets = DatagramIO.getNormalPacketReadCount();
          DatagramIO.clearReadCounts();
        }
        curEntry++;
        if (curEntry>=totEntry) {
          outputData();
          stopTimer();
        }
      } else if (e instanceof EnvironmentEvent) {
        EnvironmentEvent ee = (EnvironmentEvent)e;
        switch(ee.type) {
          case EnvironmentEvent.ACTIVATED:
            start();
            break;
          case EnvironmentEvent.MOVED:
            CountOutputStream.clearWriteCount();
            CountInputStream.clearReadCount();
            DatagramIO.clearWriteCounts();
            DatagramIO.clearReadCounts();
    
            break;
          case EnvironmentEvent.STOP:
            if (curEntry<totEntry) {
              long curTime = SystemUtilities.currentTimeMillis();
              SystemUtilities.debug("\nStart move at " + curTime);
            }
            stop();
            respond(e, new
              EnvironmentEvent(this, null, EnvironmentEvent.STOPPED,
                               getEnvironment().getId()));
            break;
          default:
            return false;
        } 
      } else {
        return false;
      }
      // Done.
      return true;
    }
  }


  // =======================================================================
  //                         Instance fields
  // =======================================================================

  /**
   * The timer notification for updating the count.
   *
   * @serial
   */
  Timer.Notification           countUpdate;

  StatInfo stats[];

  int interval;
  long totTime;
  int curEntry;
  int totEntry;

  // =======================================================================
  //                           Constructor
  // =======================================================================

  /**
   * Create a new instance of <code>NetworkStats</code>.
   *
   * @param   env  The environment for the new instance.
   */
  public NetworkStats(Environment env,int interval, long totTime) {
    super(env);
    this.interval = interval;
    this.totTime  = totTime;

    curEntry = 0;
    totEntry = 1+(int)(((long)totTime)/(long)interval);
    stats = new StatInfo[totEntry];
    int i;
    for (i=0;i<totEntry;i++) {
      stats[i] = new StatInfo();
    }
    main    = declareExported(MAIN,new UpdateHandler());
    request = declareImported(REQUEST); 
  }



  // =======================================================================
  //                           Start and stop
  // =======================================================================

  /** Acquire the resources needed by the counter application. */
  public void start() {
    
    CountOutputStream.clearWriteCount();
    DatagramIO.clearWriteCounts();

    CountInputStream.clearReadCount();
    DatagramIO.clearReadCounts();

    Timer timer = getTimer();
    // Set up count update notification.
    if (null == countUpdate) {
      countUpdate = timer.schedule(Timer.FIXED_RATE,
                                   interval+SystemUtilities.currentTimeMillis(),
                                   interval,
                                   new UpdateHandler(),
                                   new DynamicTuple());
    }
  }

  /** Release the resources used by the counter application. */
  public void stopTimer() {
    // Cancel time update notifications.
    if (null != countUpdate) {
      countUpdate.cancel();
      countUpdate = null;
    }
  }

  void outputData() {
    int i;

    Stats statsUdpNormalWrite = new Stats();
    Stats statsUdpNormalRead = new Stats();

    

    for (i=0; i<curEntry; i++) {
      SystemUtilities.debug(i+" "+(stats[i].timeMillis)
                            +" "+stats[i].tcpWriteBytes
                            +" "+stats[i].udpWritePackets 
                            +" "+stats[i].udpNormalWritePackets 
                            +" "+stats[i].udpReadPackets 
                            +" "+stats[i].udpNormalReadPackets); 
      statsUdpNormalWrite.add(stats[i].udpNormalWritePackets);
      statsUdpNormalRead.add(stats[i].udpNormalReadPackets);
      
    }

    SystemUtilities.debug("NormalWrite:  "+statsUdpNormalWrite.average()
                       +"+-"+statsUdpNormalWrite.stdev());
    SystemUtilities.debug("NormalRead:   "+statsUdpNormalRead.average()
                       +"+-"+statsUdpNormalRead.stdev());
  }

  public void stop() {
    stopTimer();
  }

  // =======================================================================
  //                            Initialization
  // =======================================================================

  /**
   * Initialize the counter.
   *
   * @param   env      The environment.
   * @param   closure  The closure, which is ignored.
   */
  public static void init(Environment env, Object closure) {
    String[] strClosure = (String[])closure;
    int interval;
    long totTime;

    interval = Integer.parseInt(strClosure[0]);
    totTime  = Long.parseLong(strClosure[1]);
    NetworkStats comp = new NetworkStats(env,interval,totTime);
 

    env.link("main", "main", comp);
    comp.link("request", "request", env);
  }

}

  
/**
 * Network usage statistics.
 */
class StatInfo implements java.io.Serializable {
  /** The number of TCP bytes read */
  public int tcpReadBytes;

  /** The number of UDP packets read */
  public int udpReadPackets;

  /** The number of non-multicast UDP packets read */
  public int udpNormalReadPackets;

  /** The number of TCP bytes written */
  public int tcpWriteBytes;

  /** The number of UDP packets written */
  public int udpWritePackets;

  /** The number of non-multicast UDP packets written */
  public int udpNormalWritePackets;

  /** The time at which the sample was taken */
  public long timeMillis;
}

