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

package one.world.util;

import java.io.IOException;

import java.net.InetAddress;
import java.net.UnknownHostException;

import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.TimeZone;

import java.util.Properties;

import one.world.Shell;

/**
 * Implementation of system utilities. This class provides
 * functionality available through <code>java.lang.System</code> or
 * <code>java.lang.Runtime</code> to code running in a protection
 * domain that limits access to the Java platform libraries. It also
 * provides information about the local node.
 *
 * @version  $Revision: 1.15 $
 * @author   Robert Grimm
 */
public final class SystemUtilities {

  /** Make constructor invisible. */
  private SystemUtilities() {
    // Nothing to do.
  }

  // =======================================================================
  //                       General utility functions
  // =======================================================================

  /**
   * Copy an array. This method simply calls the corresponding method
   * in <code>java.lang.System</code>.
   */
  public static void arraycopy(Object src, int src_position, Object dst,
                               int dst_position, int length) {
    System.arraycopy(src, src_position, dst, dst_position, length);
  }

  /**
   * Get the identity hashcode. This method simply calls the
   * corresponding method in <code>java.lang.System</code>.
   */
  public static int identityHashCode(Object o) {
    return System.identityHashCode(o);
  }


  // =======================================================================
  //                             System properties
  // =======================================================================

  /**
   * Get the current system propeties. This method simply calls the
   * corresponding method in <code>java.lang.System</code>.
   */
  public static Properties getProperties() {
    return System.getProperties();
  }

  /**
   * Get the system property. This method simply calls the
   * corresponding method in <code>java.lang.System</code>.
   */
  public static String getProperty(String key) {
    return System.getProperty(key);
  }

  /**
   * Get the system property. This method simply calls the
   * corresponding method in <code>java.lang.System</code>.
   */
  public static String getProperty(String key, String def) {
    return System.getProperty(key, def);
  }


  // =======================================================================
  //                                    Time
  // =======================================================================

  /**
   * Get the current time. This method simply calls the corresponding
   * method in <code>java.lang.System</code>.
   */
  public static long currentTimeMillis() {
    return System.currentTimeMillis();
  }

  /**
   * The time one.world was started on this node. Note that to ensure
   * that this field is initialized when one.world is started,
   * SystemUtilities must be loaded at start-up.
   */
  private static final long start = System.currentTimeMillis();

  /**
   * Get the current uptime. This method returns the current uptime of
   * this instance of <i>one.world</i> in milliseconds.
   *
   * @return  The current uptime.
   */
  public static long uptime() {
    return (System.currentTimeMillis() - start);
  }

  /**
   * Sleep for the specified time. This method simply calls {@link
   * Thread#sleep(long)} with the specified duration.
   *
   * @param   duration  The duration in milliseconds.
   * @throws  InterruptedException
   *                    Signals that the sleep was interrupted.
   */
  public static void sleep(long duration) throws InterruptedException {
    Thread.sleep(duration);
  }

  /**
   * Sleep for the specified time. This method simply calls {@link
   * Thread#sleep(long,int)} with the specified duration.
   *
   * @param   duration  The duration in milliseconds.
   * @param   nanos     The additional nanoseconds to sleep.
   * @throws  InterruptedException
   *                    Signals that the sleep was interrupted.
   */
  public static void sleep(long duration, int nanos)
    throws InterruptedException {

    Thread.sleep(duration, nanos);
  }

  /** The time format. */
  private static SimpleDateFormat fmt = null;

  static {
    fmt = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy");
    fmt.setTimeZone(TimeZone.getTimeZone("GMT"));
  }

  /**
   * Format the specified time.
   *
   * @param   time  The time to format.
   * @return        A string representation of the specified time.
   */
  public static String format(long time) {
    if (null == fmt) {
      fmt = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy");
      fmt.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    return (fmt.format(new Date(time)) + " GMT");
  }


  // =======================================================================
  //                             Memory
  // =======================================================================

  /**
   * Get the current amount of free memory. This method simply calls the
   * corresponding method in <code>java.lang.Runtime</code>.
   *
   * @return  The current amount of free memory.
   */
  public static long freeMemory() {
    return Runtime.getRuntime().freeMemory();
  }

  /**
   * Get the total amount of memory. This method simply calls the
   * corresponding method in <code>java.lang.Runtime</code>.
   *
   * @return  The total amount of memory.
   */
  public static long totalMemory() {
    return Runtime.getRuntime().totalMemory();
  }


  // =======================================================================
  //                           Active threads
  // =======================================================================

  /**
   * Get the current number of threads. This method returns an
   * estimate of the number of threads currently active in
   * <i>one.world</i>. Note that this method needs to internally
   * allocate an array to determine the actual number of threads in
   * the system and thus is somewhat expensive.
   *
   * @return  The current number of threads.
   */
  public static int activeThreads() {
    ThreadGroup group   = Thread.currentThread().getThreadGroup();
    Thread[]    threads = new Thread[group.activeCount()];
    return group.enumerate(threads);
  }


  // =======================================================================
  //                              Networking
  // =======================================================================

  /**
   * Get the IP address for this node.
   *
   * @return  The IP address for this node, or <code>null</code> if
   *          this node has no IP address.
   */
  public static String ipAddress() {
    try {
      return InetAddress.getLocalHost().getHostAddress();
    } catch (UnknownHostException x) {
      return null;
    }
  }
  
  /**
   * Get the IP address for this node as an array of bytes.
   *
   * @return  The IP address for this node as an array of bytes, or
   *          <code>null</code> if this node has no IP address.
   */
  public static byte[] rawIpAddress() {
    try {
      return InetAddress.getLocalHost().getAddress();
    } catch (UnknownHostException x) {
      return null;
    }
  }

  /**
   * Determine whether the specified host is the local host. The
   * specified host may be a DNS name or an IP address. The name
   * "<code>localhost</code>" is always treated as this host.
   *
   * @param   host  The host to test.
   * @return        <code>true</code> if the specified host
   *                is the local host.
   */
  public static boolean isLocalHost(String host) {
    if ("localhost".equals(host)) { return true; }

    // Try to resolve specified host.
    String host2;
    try {
      host2 = InetAddress.getByName(host).getHostAddress();
    } catch (UnknownHostException x) {
      host2 = host;
    }

    return host2.equals(ipAddress());
  }


  // =======================================================================
  //                          Program execution
  // =======================================================================

  /**
   * Execute the specified command in a separate process. This method
   * simply calls the corresponding method in
   * <code>java.lang.Runtime</code>.
   *
   * <p>This method is provided for legacy code that requires access
   * to outside processes. Note that such processes execute outside
   * <i>one.world</i> and cannot be check-pointed or
   * migrated. Furthermore, to use this method, a node's security
   * policy has to be configured so that the calling code as well as
   * <i>one.world</i>'s core code have the appropriate
   * <code>FilePermission</code>.</p>
   */
  public static Process exec(String command) throws IOException {
    return Runtime.getRuntime().exec(command);
  }


  // =======================================================================
  //                             Debugging
  // =======================================================================

  /**
   * Print the specified debug message to the console.
   *
   * @param  msg  The debug message to print to the console.
   */
  public static void debug(String msg) {
    Shell.console.println(msg);
  }

  /**
   * Print the specified throwable to the console. This method prints
   * a stack trace for the specified throwable to the console.
   *
   * @param  x   The throwable to print to the console.
   */
  public static void debug(Throwable x) {
    x.printStackTrace(Shell.console);
  }

}
