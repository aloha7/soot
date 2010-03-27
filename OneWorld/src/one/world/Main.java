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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import java.util.Iterator;
import java.util.Properties;

import javax.swing.UIManager;

import one.world.binding.LeaseManager;

import one.world.core.Component;
import one.world.core.Environment;
import one.world.core.RequestManager;
import one.world.core.SystemPermission;

import one.world.io.DatagramIO;
import one.world.io.NetworkIO;
import one.world.io.TupleStore;

import one.world.rep.DiscoveryClient;
import one.world.rep.DiscoveryServer;
import one.world.rep.RemoteManager;

import one.world.util.Log;
import one.world.util.SystemUtilities;
import one.world.util.Timer;


/**
 * The main class for <i>one.world</i>. This class defines static
 * methods to startup and shutdown the system. The methods defined by
 * this class must be called with a thread that is not provided by the
 * architecture itself.
 *
 * <p>Configuration parameters for <i>one.world</i> are stored in a
 * Java properties file. By default, this file is named
 * "<code>one.world.config</code>" and must be located in the current
 * working directory. Though, the "<code>one.world.config.name</code>"
 * system property can be used to specify an alternative name and
 * directory. During startup, the properties file is read and all its
 * properties are added to the system properties.</p>
 *
 * @version  $Revision: 1.36 $
 * @author   Robert Grimm
 */
public final class Main {

  /** The status code for the initialized state. */
  public static final  int     INITIALIZED = 0;

  /** The status code for the running state. */
  public static final  int     RUNNING     = 1;

  /** The status code for the shutdown state. */
  public static final  int     SHUTDOWN    = 2;

  /** The monitor for this class. */
  private static final Object  MONITOR     = new Object();

  /** The current status of <i>one.world</i>. */
  private static       int     status      = INITIALIZED;

  /** Hide constructor. */
  private Main() {
    // Nothing to do.
  }

  /**
   * Get the current system status.
   *
   * @return  The current system status.
   */
  public static int getStatus() {
    synchronized (MONITOR) {
      return status;
    }
  }

  /**
   * Start up <i>one.world</i>.
   *
   * @throws  IOException
   *             Signals an exceptional condition while accessing the
   *             configuration file or persistent environment state.
   * @throws  IllegalStateException
   *             Signals that the system has already been started up
   *             or that it is has been shut down.
   * @throws  SecurityException
   *             Signals that the caller does not have the system
   *             permission to start up and shut down the system.
   */
  public static void startUp() throws IOException {
    SecurityManager security = System.getSecurityManager();
    if (null != security) {
      security.checkPermission(SystemPermission.START_UP);
    }

    synchronized (MONITOR) {
      if (INITIALIZED != status) {
        if (RUNNING == status) {
          throw new IllegalStateException("System already running");
        } else {
          throw new IllegalStateException("System has shut down");
        }
      }

      status = RUNNING;
    }

    // Log version and build time.
    Log log = (Log)AccessController.doPrivileged(new PrivilegedAction() {
        public Object run() {
          return Log.getSystemLog();
        }
      });
    log.log(null, "Starting one.world, version " +
            Constants.MAJOR_VERSION + "." +
            Constants.MINOR_VERSION + "." +
            Constants.MICRO_VERSION + ", built " +
            Constants.BUILD_TIME + " GMT");

    // Touch SystemUtilities so that the start-up time is correctly
    // initialized.
    SystemUtilities.uptime();

    // Set up system properties through configuration file.
    Properties prop = new Properties();

    prop.load(new
      FileInputStream(System.getProperty("one.world.config.name",
                                         "one.world.config")));

    Iterator iter = prop.keySet().iterator();
    while (iter.hasNext()) {
      String key   = (String)iter.next();
      String value = prop.getProperty(key);
      System.setProperty(key, value);
    }

    // Set up look and feel.
    if (null != System.getProperty("one.world.swing.native.laf")) {
      try {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
      } catch (Exception x) {
        // Ignore.
      }
    }

    // Set up tuple store.
    String     s    = System.getProperty("one.world.store.root");
    if (null == s) {
      throw new IOException("Null root directory");
    }

    final File root = new File(s);

    try {
      AccessController.doPrivileged(new PrivilegedExceptionAction() {
          public Object run() throws IOException {
            TupleStore.startUp(root);
            Environment.startUp();

            // Make sure LostAndFound exists.
            if (null == Environment.getRoot().getChild("LostAndFound")) {
              Environment.create(null,
                                 Environment.getRoot().getId(),
                                 "LostAndFound", false);
            }

            // Make sure User exists.
            if (null == Environment.getRoot().getChild("User")) {
              Environment.create(null,
                                 Environment.getRoot().getId(),
                                 "User", false);
            }

            return null;
          }
        });
    } catch (PrivilegedActionException x) {
      Exception xx = x.getException();
      if (xx instanceof IOException) {
        throw (IOException)xx;
      } else if (xx instanceof RuntimeException) {
        throw (RuntimeException)xx;
      } else {
        throw new UnknownError(x.toString());
      }
    }

    // Populate the root environment and activate it.
    AccessController.doPrivileged(new PrivilegedAction() {
        public Object run() {
          Environment    root    = Environment.getRoot();
          Timer          timer   = root.getTimer1();
          LeaseManager   lease   = new LeaseManager(root);
          RequestManager request = new RequestManager(root);
          DatagramIO     udp     = new DatagramIO(root);
          NetworkIO      tcp     = new NetworkIO(root);
          TupleStore     storage = new TupleStore(root);
	  RemoteManager  remote  = new RemoteManager(root, "localhost",
						     Constants.REP_PORT);

          root.link("main",            "main",    request);
          root.link("monitor",         "request", request);
	  udp.link("lease",            "request", lease); 
	  tcp.link("lease",            "request", lease); 
          storage.link("lease",        "request", lease); 
	  remote.link("lease",         "request", lease);
          request.link("udp",          "bind",    udp);
          request.link("tcp",          "bind",    tcp);
          request.link("storage",      "bind",    storage);
	  request.link("remote",       "request", remote);
          request.link("env",          "env",     remote);

	  //The discovery client is always active.
	  DiscoveryClient client = new DiscoveryClient(root);

	  remote.link("discovery", "input",   client);
	  remote.link("discoveryError", "deliveryErrors", client);
	  client.link("request",   "request", root);
	  client.link("lease",     "request", lease);
	  request.link("env",      "main",    client);

	  //The discovery server is only active if specified.
	  if (SystemUtilities.getProperty("one.world.discovery.server",
					  "false").equalsIgnoreCase("true")) {
	    DiscoveryServer server = new DiscoveryServer(root);

	    server.link("request", "request", root);
	    server.link("lease",   "request", lease);
	    request.link("env",    "main",    server);
	  }

	  Environment.activate(null, root.getId());
	  
	  return null;
	}
      });
  }

  /**
   * Shut down <i>one.world</i>.
   *
   * @throws  IllegalStateException
   *             Signals that the system has not yet been
   *             started or that it has already been shut
   *             down.
   * @throws  SecurityException
   *             Signals that the caller does not have the
   *             system permission to start up and shut down
   *             the system.
   */
  public static void shutDown() {
    SecurityManager security = System.getSecurityManager();
    if (null != security) {
      security.checkPermission(SystemPermission.START_UP);
    }

    synchronized (MONITOR) {
      if (RUNNING != status) {
        if (INITIALIZED == status) {
          throw new IllegalStateException("System not yet running");
        } else {
          throw new IllegalStateException("System already shut down");
        }
      }

      status = SHUTDOWN;
    }

    Environment.shutDown();
    TupleStore.shutDown();
  }

  /**
   * Run the root shell for <i>one.world</i>.
   *
   * @param  args  The arguments for this method, which are treated
   *               as the file names of scripts to be executed when
   *               starting the root shell.
   */
  public static void main(final String[] args) {
    // Make sure a security manager has been established.
    if (null == System.getSecurityManager()) {
      System.out.println("one.world requires a security manager and cannot " +
                         "run without it.");
      System.out.println("Bye.");
      return;
    }

    // Start up system.
    try {
      Main.startUp();
    } catch (IOException x) {
      System.out.println("Can't start up system: " + x.toString());
      x.printStackTrace();
      System.out.println("Bye.");
      return;
    }
    
    // Get root shell.
    Shell shell = Shell.getRootShell();
    
    // Source scripts.
    for (int i=0; i<args.length; i++) {
      try {
        shell.source(args[i]);
      } catch (Throwable x) {
        shell.print(x);
        shell.print();
        shell.flush();
      }
    }
    
    // Run root shell.
    shell.loop();
    
    // Shut down system.
    Main.shutDown();
    System.out.println();
    System.out.println("Bye.");

    // Exit Java. System.exit() is called explicitly so that the JVM
    // exits even if AWT/Swing has been used.
    System.exit(0);
  }

}
