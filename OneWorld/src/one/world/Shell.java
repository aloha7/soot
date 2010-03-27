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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintStream;

import java.lang.reflect.InvocationTargetException;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import java.text.DecimalFormat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.StringTokenizer;

import one.net.Converter;
import one.net.MimeTypes;
import one.net.NetConstants;
import one.net.NetUtilities;
import one.net.Telnet;

import one.util.Bug;
import one.util.Guid;

import one.world.binding.BindingResponse;
import one.world.binding.Duration;
import one.world.binding.LeaseMaintainer;
import one.world.binding.UnknownResourceException;

import one.world.core.CheckPoint;
import one.world.core.Environment;
import one.world.core.Event;
import one.world.core.ExceptionalEvent;
import one.world.core.SystemPermission;
import one.world.core.Tuple;

import one.world.data.Data;
import one.world.data.ClassData;

import one.world.io.NoSuchTupleException;
import one.world.io.Query;
import one.world.io.QueryResponse;
import one.world.io.SioResource;

import one.world.util.ExceptionHandler;
import one.world.util.IOUtilities;
import one.world.util.IteratorElement;
import one.world.util.Synchronous.ResultHandler;
import one.world.util.SystemUtilities;
import one.world.util.TimeOutException;

/**
 * Implementation of the root shell. The root shell is a relatively
 * simple shell, which can control any environment in the local
 * environment hierarchy.
 *
 * <p>By default, the root shell reads from and writes to the local
 * console. However, if the "<code>one.world.shell.pwd</code>" system
 * property is defined and represents a non-empty string, the root
 * shell is accessible via telnet (and not locally). In that case, the
 * root shell accepts a single telnet connection at a time. The port
 * can be specified using the "<code>one.world.shell.port</code>"
 * system property and defaults to the default telnet port
 * (23). Before granting access, the root shell asks for a password,
 * which is the string specified by the
 * "<code>one.world.shell.pwd</code>" system property. Note that
 * terminating a telnet connection does not terminate
 * <i>one.world</i>; executing <code>exit</code> within the root shell
 * does. Further note that if no telnet client is connected to the
 * root shell, console output is re-directed to the local console.</p>
 *
 * <p>The following commands are implemented by this class and
 * available by default:<ul>
 * <li><code>cat</code> to print the contents of a tuple store.</li>
 * <li><code>cd</code> to change the current environment.</li>
 * <li><code>checkpoint</code> to check-point environment(s).</li>
 * <li><code>clean</code> to delete tuples from a tuple store.</li>
 * <li><code>cp</code> to copy environment(s).</li>
 * <li><code>echo</code> to print a message.</li>
 * <li><code>error</code> to exit a script (and all calling
 * scripts).</li>
 * <li><code>exit</code> to exit the shell.</li>
 * <li><code>export</code> to export tuples to the file
 * system.</li>
 * <li><code>gc</code> to invoke the garbage collector.</li>
 * <li><code>help</code> to provide help.</li>
 * <li><code>if</code> for conditional execution.</li>
 * <li><code>import</code> to import data from the file
 * system.</li>
 * <li><code>info</code> to print system information.</li>
 * <li><code>load</code> to load code into an environment.</li>
 * <li><code>ls</code> to list the environment hierarchy.</li>
 * <li><code>mk</code> to create an environment.</li>
 * <li><code>mv</code> to move or rename environment(s).</li>
 * <li><code>pwd</code> to print the current environment.</li>
 * <li><code>ps</code> to print all active threads.</li>
 * <li><code>restore</code> to restore environment(s) from
 * a saved check-point.</li>
 * <li><code>rm</code> to destroy environment(s).</li>
 * <li><code>run</code> to activate an environment.</li>
 * <li><code>set</code> to access system properties.</li>
 * <li><code>source</code> to execute a script.</li>
 * <li><code>stop</code> to terminate an environment.</li>
 * <li><code>unload</code> to unload code from an environment.</li>
 * <li><code>wait</code> to wait for a condition.</li>
 * </ul></p>
 *
 * <p>Executing <code>help</code> at the shell prompt will print a
 * list of all available commands, specifying the syntax for each
 * command and providing a short description.</p>
 *
 * <p>The root shell recognizes comments: all characters following a
 * '<code>#</code>' until the end of the line are treated as part of
 * the comment.</p>
 *
 * <p>The root shell supports arguments to scripts. Tokens of the form
 * "<code>%&lt;number&gt;</code>" are replaced with the actual script
 * argument. The first argument has index 1. Note that substitution
 * for the <code>if</code> command is conditional; the command
 * specified for <code>if</code> is only substituted when it is about
 * to be executed. For example, the "<code>%1</code>" in "<code>if
 * defined 1 echo %1</code>" is only substituted if the corresponding
 * script has been invoked with at least one argument.</p>
 *
 * <p>Note that both <code>mk</code> and <code>load</code> always pass
 * a string array as the initializer's closure. This array may be
 * empty, but it is never <code>null</code>.</p>
 *
 * <p>Also note that the root shell is not thread-safe.</p>
 *
 * @see      Constants#SHELL_PORT
 *
 * @version  $Revision: 1.56 $
 * @author   Robert Grimm
 */
public final class Shell {

  // =======================================================================

  /** Implementation of console output. */
  static final class Output extends PrintStream {
    
    /** The local print stream. */
    private  PrintStream local;

    /** The telnet connection. */
    volatile Telnet      telnet;

    /** Create a new console output. */
    Output() {
      super(System.out, false);
      local = new PrintStream(System.out, true);
    }

    /** Flush console output. */
    public void flush() {
      local.flush();
    }

    /** Close console output. */
    public void close() {
      // Ignore.
    }

    /** Check console output's error state. */
    public boolean checkError() {
      local.flush();
      return false;
    }

    /** Write a character. */
    public void write(int c) {
      if (null != telnet) {
        try {
          telnet.write(c);
        } catch (Exception x) {
          local.write(c);
        }
      } else {
        local.write(c);
      }
    }

    /** Write a portion of an array of characters. */
    public void write(char[] buf, int off, int len) {
      if (null != telnet) {
        try {
          for (int i=0; i<len; i++) {
            telnet.write(buf[off + i]);
          }
          return;
        } catch (Exception x) {
          // Fall through.
        }
      }

      for (int i=0; i<len; i++) {
        local.write(buf[off + i]);
      }
    }

    /** Print a boolean. */
    public void print(boolean b) {
      print(b ? "true" : "false");
    }

    /** Print a character. */
    public void print(char c) {
      print(String.valueOf(c));
    }

    /** Print an integer. */
    public void print(int i) {
      print(String.valueOf(i));
    }

    /** Print a long. */
    public void print(long l) {
      print(String.valueOf(l));
    }

    /** Print a float. */
    public void print(float f) {
      print(String.valueOf(f));
    }

    /** Print a double. */
    public void print(double d) {
      print(String.valueOf(d));
    }

    /** Print an array of characters. */
    public void print(char[] buf) {
      print(String.valueOf(buf));
    }

    /** Print a string. */
    public void print(String s) {
      if (null != telnet) {
        try {
          telnet.write(s);
        } catch (Exception x) {
          local.print(s);
        }
      } else {
        local.print(s);
      }
    }

    /** Print an object. */
    public void print(Object o) {
      print(String.valueOf(o));
    }

    /** Terminate the current line. */
    public void println() {
      if (null != telnet) {
        try {
          telnet.writeLine();
        } catch (Exception x) {
          local.println();
        }
      } else {
        local.println();
      }
    }

    /** Print a boolean and then terminate the line. */
    public void println(boolean b) {
      println(b ? "true" : "false");
    }

    /** Print a character and then terminate the line. */
    public void println(char c) {
      println(String.valueOf(c));
    }

    /** Print an integer and then terminate the line. */
    public void println(int i) {
      println(String.valueOf(i));
    }

    /** Print a long and then terminate the line. */
    public void println(long l) {
      println(String.valueOf(l));
    }

    /** Print a float and then terminate the line. */
    public void println(float f) {
      println(String.valueOf(f));
    }

    /** Print a double and then terminate the line. */
    public void println(double d) {
      println(String.valueOf(d));
    }

    /** Print an array of characters and then terminate the line. */
    public void println(char[] buf) {
      println(String.valueOf(buf));
    }

    /** Print a string and then terminate the line. */
    public void println(String s) {
      if (null != telnet) {
        try {
          telnet.writeLine(s);
        } catch (Exception x) {
          local.println(s);
        }
      } else {
        local.println(s);
      }
    }

    /** Print an object and then terminate the line. */
    public void println(Object o) {
      println(String.valueOf(o));
    }

  }

  // =======================================================================

  /** Implementation of an exit exception. */
  public static class ExitException extends Exception {

    /** Create a new exit exception. */
    public ExitException() {
      // Nothing to do.
    }

  }

  // =======================================================================

  /** Implementation of a user exception. */
  public static class UserException extends Exception {

    /** Create a new user exception with no detail message. */
    public UserException() {
      // Nothing to do.
    }

    /**
     * Create a new user exception with the specified detail message.
     *
     * @param   msg  The detail message.
     */
    public UserException(String msg) {
      super(msg);
    }
    
  }

  // =======================================================================

  /** Definition of a shell command. */
  public static interface Command {

    /**
     * Get the name of this command.
     *
     * @return  The name of this command.
     */
    String getName();

    /**
     * Get a description of the arguments for this command.
     *
     * @return  A description of the arguments for this command.
     */
    String getArguments();

    /**
     * Get a short description for this command.
     *
     * @return  A description for this command.
     */
    String getDescription();

    /**
     * Get the minimum number of arguments for this command.
     *
     * @return  The minimum number of arguments.
     */
    int getMinArgs();

    /**
     * Get the maximum number of arguments for this command.
     *
     * @return  The maximum number of arguments.
     */
    int getMaxArgs();

    /**
     * Execute this command in the specified shell with the specified
     * arguments.
     *
     * <p>The arguments are specified as a list of strings. Their
     * number is guaranteed to be consistent with the minimum and
     * maximum number returned by {@link #getMinArgs} and {@link
     * #getMaxArgs}.</p>
     *
     * <p>The list of substitutions passed to this method provides the
     * arguments for replacing substitution tokens of the form
     * "<code>%&lt;number&gt;</code>" with an actual argument. This
     * method must perform substitution before using any argument.</p>
     *
     * @see     Shell#substitute(List,List)
     * @see     Shell#substitute(String,List)
     *
     * @param   shell  The shell.
     * @param   args   The arguments.
     * @param   subst  The substitutions (already performed).
     * @throws  Throwable
     *                 Signals an exceptional condition when
     *                 executing this command.
     */
    void run(Shell shell, List args, List subst) throws Throwable;

  }

  // =======================================================================

  /** Implementation of "<code>cat</code>". */
  static final class Cat implements Command {

    public String getName() {
      return "cat";
    }

    public String getArguments() {
      return "[<path>]";
    }

    public String getDescription() {
      return "Print contents of tuple store";
    }

    public int getMinArgs() {
      return 0;
    }

    public int getMaxArgs() {
      return 1;
    }

    public void run(Shell shell, List args, List subst) throws Throwable {
      Environment env;

      if (0 == args.size()) {
        env = shell.getEnvironment();
      } else {
        env = shell.resolve(substitute((String)args.get(0), subst));
      }

      boolean success = false;

      // Get the event handler for the environment's tuple store.
      SioResource     resource = new SioResource();

      resource.type  = SioResource.STORAGE;
      resource.ident = env.getId();

      BindingResponse binding =
        IOUtilities.bind(Environment.getRoot().getRequest(), resource,
                         Duration.FOREVER);
      LeaseMaintainer lm = new
        LeaseMaintainer(binding.lease, binding.duration,
                        ExceptionHandler.HANDLER, null,
                        Environment.getRoot().getTimer1());

      // Query for all tuples.
      QueryResponse   query;
      LeaseMaintainer lm2;

      try {
        query = IOUtilities.query(binding.resource, new Query(),
                                  Duration.FOREVER);
        lm2   = new LeaseMaintainer(query.lease, query.duration,
                                    ExceptionHandler.HANDLER, null,
                                    Environment.getRoot().getTimer1());
        success = true;
      } finally {
        if (! success) {
          lm.cancel();
        }
      }

      // Iterate over tuples and print them.
      IteratorElement iter;
      boolean         first = true;  // Flag for first iteration.

      try {
        do {
          try {
            iter = IOUtilities.next(query.iter);
          } catch (NoSuchElementException x) {
            if (first) {
              // Nothing to print.
              return;
            } else {
              throw x;
            }
          }

          shell.print(iter.element.toString());
          first = false;
        } while (iter.hasNext);
      } finally {
        lm2.cancel();
        lm.cancel();
      }
    }

  }

  // =======================================================================

  /** Implementation of "<code>cd</code>". */
  static final class Cd implements Command {

    public String getName() {
      return "cd";
    }

    public String getArguments() {
      return "[<path>]";
    }

    public String getDescription() {
      return "Change current environment";
    }

    public int getMinArgs() {
      return 0;
    }

    public int getMaxArgs() {
      return 1;
    }

    public void run(Shell shell, List args, List subst) throws Throwable {
      Environment env;

      if (0 == args.size()) {
        env = Environment.getRoot();
      } else {
        env = shell.resolve(substitute((String)args.get(0), subst));
      }

      shell.setEnvironment(env);
    }

  }

  // =======================================================================

  /** Implementation of "<code>clean</code>". */
  static final class Clean implements Command {

    public String getName() {
      return "clean";
    }

    public String getArguments() {
      return "[-all|-checkpoint|-class] [<path>]";
    }

    public String getDescription() {
      return "Delete all / checkpoint / class data tuples from environment";
    }

    public int getMinArgs() {
      return 0;
    }

    public int getMaxArgs() {
      return 2;
    }

    public void run(Shell shell, List args, List subst) throws Throwable {
      substitute(args, subst);

      int         size       = args.size();

      boolean     checkpoint = false;
      boolean     klass      = false;
      Environment env;

      // Parse command line arguments.
      if (0 == size) {
        env          = shell.getEnvironment();

      } else if (1 == size) {
        String s     = (String)args.get(0);

        if ("-all".equals(s)) {
          env        = shell.getEnvironment();
        } else if ("-checkpoint".equals(s)) {
          checkpoint = true;
          env        = shell.getEnvironment();
        } else if ("-class".equals(s)) {
          klass      = true;
          env        = shell.getEnvironment();
        } else {
          env        = shell.resolve(s);
        }

      } else {
        String s     = (String)args.get(0);

        if ("-all".equals(s)) {
          // Nothing to do.
        } else if ("-checkpoint".equals(s)) {
          checkpoint = true;
        } else if ("-class".equals(s)) {
          klass      = true;
        } else {
          throw new IllegalArgumentException("Invalid flag (" + s + ")");
        }

        env          = shell.resolve((String)args.get(1));
      }

      boolean success = false;

      // Get the event handler for the environment's tuple store.
      SioResource     resource = new SioResource();

      resource.type  = SioResource.STORAGE;
      resource.ident = env.getId();

      BindingResponse binding =
        IOUtilities.bind(Environment.getRoot().getRequest(), resource,
                         Duration.FOREVER);
      LeaseMaintainer lm = new
        LeaseMaintainer(binding.lease, binding.duration,
                        ExceptionHandler.HANDLER, null,
                        Environment.getRoot().getTimer1());

      // Query for matching tuples.
      Query           query;

      if (checkpoint) {
        query = new Query("", Query.COMPARE_HAS_TYPE, CheckPoint.class);
      } else if (klass) {
        query = new Query("", Query.COMPARE_HAS_SUBTYPE, ClassData.class);
      } else {
        query = new Query();
      }

      QueryResponse   qr;
      LeaseMaintainer lm2;

      try {
        qr  = IOUtilities.query(binding.resource, query, Duration.FOREVER,
                                ! checkpoint);
        lm2 = new LeaseMaintainer(qr.lease, qr.duration,
                                  ExceptionHandler.HANDLER, null,
                                  Environment.getRoot().getTimer1());
        success = true;
      } finally {
        if (! success) {
          lm.cancel();
        }
      }

      // Iterate over tuples and collect their IDs.
      ArrayList       idList     = new ArrayList();
      Guid            latestId   = null;  // ID for latest checkpoint.
      long            latestTime = -1;    // Timestamp for latest checkpoint.
      IteratorElement iter;
      boolean         first      = true;  // Flag for first iteration.

      success                    = false;

      try {
        do {
          try {
            iter = IOUtilities.next(qr.iter);
          } catch (NoSuchElementException x) {
            if (first) {
              break;
            } else {
              throw x;
            }
          }

          if (checkpoint) {
            CheckPoint cp = (CheckPoint)iter.element;

            idList.add(cp.id);

            if (cp.timestamp > latestTime) {
              latestId    = cp.id;
              latestTime  = cp.timestamp;
            }
          } else {
            idList.add(iter.element);
          }
          first = false;
        } while (iter.hasNext);
        success = true;
      } finally {
        lm2.cancel();
        if (! success) {
          lm.cancel();
        }
      }

      // Delete tuples.
      if (null != latestId) {
        idList.remove(latestId);
      }

      Iterator iter2 = idList.iterator();
      
      while (iter2.hasNext()) {
        try {
          IOUtilities.delete(binding.resource, (Guid)iter2.next());
        } catch (NoSuchTupleException x) {
          // Tuple has already been deleted. Nothing to do.
        } catch (Throwable x) {
          lm.cancel();
          throw x;
        }
      }

      lm.cancel();
    }

  }

  // =======================================================================

  /** Implementation of "<code>checkpoint</code>". */
  static final class Checkpoint implements Command {

    public String getName() {
      return "checkpoint";
    }

    public String getArguments() {
      return "[<path>]";
    }

    public String getDescription() {
      return "Check-point environment(s)";
    }

    public int getMinArgs() {
      return 0;
    }

    public int getMaxArgs() {
      return 1;
    }

    public void run(Shell shell, List args, List subst) throws Throwable {
      Environment env;

      if (0 == args.size()) {
        env = shell.getEnvironment();
      } else {
        env = shell.resolve(substitute((String)args.get(0), subst));
      }

      long time = Environment.checkPoint(null, env.getId());
      shell.print(Long.toString(time)+" ("+SystemUtilities.format(time)+")");
    }

  }

  // =======================================================================

  /** Implementation of "<code>cp</code>". */
  static final class Cp implements Command {

    public String getName() {
      return "cp";
    }

    public String getArguments() {
      return "[<path>] <new-location>";
    }

    public String getDescription() {
      return "Copy environment(s)";
    }

    public int getMinArgs() {
      return 1;
    }

    public int getMaxArgs() {
      return 2;
    }

    public void run(Shell shell, List args, List subst) throws Throwable {
      substitute(args, subst);

      int size = args.size();

      Environment env;
      String      target;

      if (1 == size) {
        env    = shell.getEnvironment();
        target = (String)args.get(0);

      } else {
        env    = shell.resolve((String)args.get(0));
        target = (String)args.get(1);
      }

      // Handle complete structured I/O URLs first.
      if (target.startsWith("sio://")) {
        SioResource resource = new SioResource(target);
        if (SioResource.STORAGE != resource.type) {
          throw new IllegalArgumentException("Invalid path (" + target + ")");
        } else if ((null != resource.remoteHost) &&
                   SystemUtilities.isLocalHost(resource.remoteHost) &&
                   ((-1 == resource.remotePort) ||
                    (Constants.REP_PORT == resource.remotePort))) {
          resource.remoteHost = null;
          target              = resource.toString();
        }

        if (null != resource.remoteHost) {
          String        remoteHost = resource.remoteHost;
          int           remotePort = resource.remotePort;
          resource.remoteHost      = null;
          ResultHandler rh         = new ResultHandler();
          Environment.moveAway(null, rh, null, env.getId(), remoteHost,
                               remotePort, resource.toString(), true);
          Event         result     = rh.getResult(Duration.YEAR);
          if (result instanceof ExceptionalEvent) {
            throw ((ExceptionalEvent)result).x;
          }
          return;
        }
      }

      Environment.copy(null, env.getId(), shell.resolve(target).getId());
    }

  }

  // =======================================================================

  /** Implementation of "<code>echo</code>". */
  static final class Echo implements Command {

    public String getName() {
      return "echo";
    }

    public String getArguments() {
      return "<message>*";
    }

    public String getDescription() {
      return "Print message";
    }

    public int getMinArgs() {
      return 0;
    }

    public int getMaxArgs() {
      return Integer.MAX_VALUE;
    }

    public void run(Shell shell, List args, List subst) throws Throwable {
      substitute(args, subst);

      StringBuffer buf   = new StringBuffer();
      Iterator     iter  = args.iterator();
      boolean      first = true;

      while (iter.hasNext()) {
        if (first) {
          first = false;
        } else {
          buf.append(' ');
        }
      
        buf.append((String)iter.next());
      }

      shell.print(buf.toString());
    }

  }

  // =======================================================================

  /** Implementation of "<code>error</code>". */
  static final class Errorr implements Command {

    public String getName() {
      return "error";
    }

    public String getArguments() {
      return "<message>*";
    }

    public String getDescription() {
      return "Exit a script with a user exception";
    }

    public int getMinArgs() {
      return 0;
    }

    public int getMaxArgs() {
      return Integer.MAX_VALUE;
    }

    public void run(Shell shell, List args, List subst) throws Throwable {
      if (0 == args.size()) {
        throw new UserException();

      } else {
        substitute(args, subst);

        StringBuffer buf   = new StringBuffer();
        Iterator     iter  = args.iterator();
        boolean      first = true;

        while (iter.hasNext()) {
          if (first) {
            first = false;
          } else {
            buf.append(' ');
          }
          
          buf.append((String)iter.next());
        }

        throw new UserException(buf.toString());
      }
    }

  }

  // =======================================================================

  /** Implementation of "<code>exit</code>". */
  static final class Exit implements Command {

    public String getName() {
      return "exit";
    }

    public String getArguments() {
      return "";
    }

    public String getDescription() {
      return "Exit shell";
    }

    public int getMinArgs() {
      return 0;
    }

    public int getMaxArgs() {
      return 0;
    }

    public void run(Shell shell, List args, List subst) throws Throwable {
      throw new ExitException();
    }

  }

  // =======================================================================

  /** Implementation of "<code>export</code>". */
  static final class Export implements Command {

    public String getName() {
      return "export";
    }

    public String getArguments() {
      return "[<path>] <file-system-directory>";
    }

    public String getDescription() {
      return "Recursively export tuples to files";
    }

    public int getMinArgs() {
      return 1;
    }

    public int getMaxArgs() {
      return 2;
    }

    public void run(Shell shell, List args, List subst) throws Throwable {
      substitute(args, subst);

      int size = args.size();

      Environment env;
      File        directory;

      // Parse command line arguments.
      if (1 == size) {
        env       = shell.getEnvironment();
        directory = new File((String)args.get(0));

      } else {
        env       = shell.resolve((String)args.get(0));
        directory = new File((String)args.get(1));
      }

      // Make sure the specified file system entity is a writeable
      // directory.
      if (! directory.isDirectory()) {
        throw new IllegalArgumentException("Not a directory (" +
                                           directory.getAbsolutePath() + ")");
      } else if (! directory.canWrite()) {
        throw new IllegalStateException("Unable to access directory (" +
                                        directory.getAbsolutePath() + ")");
      }

      // Export tuples.
      export(shell, env, directory);
    }

    /**
     * Export all tuples from the specified environment to the
     * specified parent directory. This method recursively exports all
     * tuples from the specified environment to the specified
     * directory. The specified environment must be a valid
     * environment; the specified directory must be a writeable
     * directory.
     *
     * @param   shell   The shell.
     * @param   env     The environment.
     * @param   parent  The directory.
     * @throws  Throwable
     *                  Signals some exceptional condition while
     *                  exporting.
     */
    private void export(Shell shell, Environment env, File parent)
      throws Throwable {

      // Process tuples in environment env.
      SioResource           resource = new SioResource();
      resource.type                  = SioResource.STORAGE;
      resource.ident                 = env.getId();
      final BindingResponse br;
      LeaseMaintainer       lm       = null;
      QueryResponse         query;
      LeaseMaintainer       lm2      = null;

      // Bind to tuple store for environment and query for all tuples.
      try {
        br    = IOUtilities.bind(Environment.getRoot().getRequest(), resource,
                                 Duration.FOREVER);
        lm    = new LeaseMaintainer(br.lease, br.duration,
                                    ExceptionHandler.HANDLER, null,
                                    Environment.getRoot().getTimer1());
        query = IOUtilities.query(br.resource, new Query(), Duration.FOREVER);
        lm2   = new LeaseMaintainer(query.lease, query.duration,
                                    ExceptionHandler.HANDLER, null,
                                    Environment.getRoot().getTimer1());
      } catch (Throwable x) {
        shell.print("Unable to access environment " + env.getId() + " (" +
                    x + "); skipping it...");
        if (null != lm) {
          lm.cancel();
        }
        if (null != lm2) {
          lm2.cancel();
        }
        return;
      }

      // Create a tuple reader for the environment.
      Converter.TupleReader reader = new Converter.TupleReader() {
          public Tuple read(Guid id) throws IOException {
            Query q = new Query("id", Query.COMPARE_EQUAL, id);
            Tuple t;

            try {
              t = IOUtilities.read(br.resource, q);
            } catch (Throwable x) {
              throw new IOException(x.getMessage());
            }

            return t;
          }
        };

      // Iterate over tuples.
      IteratorElement iter;
      boolean         first = true; // Flag for first iteration.

      try {
        do {
          try {
            iter  = IOUtilities.next(query.iter);
            first = false;
          } catch (NoSuchElementException x) {
            if (! first) {
              throw x;
            }
            break;
          }

          // Get the tuple.
          Tuple            t    = (Tuple)iter.element;
          
          // Select a converter for the tuple and determine whether it
          // is relative.
          Converter        con  = NetUtilities.select(t);
          if (con.isRelative(t)) {
            // Skip this tuple.
            continue;
          }

          // Create a temporary file and write the tuple to it.
          File             temp = File.createTempFile("tuple-conversion",
                                                      null, parent);
          FileOutputStream out  = new FileOutputStream(temp);
          String           type;
          try {
            type                = con.convert(t, out, reader);
          } finally {
            out.close();
          }

          // Rename temporary file.
          String name;

          if (t instanceof ClassData) {
            name = ((ClassData)t).name + ".class";

          } else if (t instanceof Data) {
            name = ((Data)t).name;

          } else {
            name = t.id.toString() + "." + MimeTypes.getExtension(type);
          }

          if (! temp.renameTo(new File(parent, name))) {
            shell.print("Unable to rename temporary file " +
                        temp.getAbsolutePath() + " to " + name +
                        "; continuing...");
          }

        } while (iter.hasNext);
      } finally {
        lm2.cancel();
        lm.cancel();
      }

      // Process child environments.
      Iterator iter2 = env.getChildren().iterator();
      while (iter2.hasNext()) {
        String      name  = (String)iter2.next();
        Environment child = env.getChild(name);

        if (null == child) {
          // The environment must have been renamed or removed.
          shell.print("Environment " + name + " has been renamed or removed " +
                      "while exporting; skipping it...");
          continue;
        }

        // Access directory for environment, creating it if necessary.
        File dir = new File(parent, name);

        if ((! dir.exists()) && (! dir.mkdir())) {
          shell.print("Unable to create directory " + dir.getAbsolutePath() +
                      "; skipping corresponding environment...");
          continue;
        }
        if (! dir.isDirectory()) {
          shell.print("File " + dir.getAbsolutePath() + " prevents creation " +
                      "of directory with same name; skipping corresponding " +
                      "environment...");
          continue;
        } else if (! dir.canWrite()) {
          shell.print("Unable to access " + dir.getAbsolutePath() +
                      "; skipping corresponding environment...");
          continue;
        }

        export(shell, child, dir);
      }
    }

  }

  // =======================================================================

  /** Implementation of "<code>gc</code>". */
  static final class Gc implements Command {

    public String getName() {
      return "gc";
    }

    public String getArguments() {
      return "";
    }

    public String getDescription() {
      return "Perform garbage collection";
    }

    public int getMinArgs() {
      return 0;
    }

    public int getMaxArgs() {
      return 0;
    }

    public void run(Shell shell, List args, List subst) throws Throwable {
      System.gc();
    }

  }

  // =======================================================================

  /** Implementation of "<code>help</code>". */
  static final class Help implements Command {

    public String getName() {
      return "help";
    }

    public String getArguments() {
      return "[<command>]";
    }

    public String getDescription() {
      return "Print help on command(s)";
    }

    public int getMinArgs() {
      return 0;
    }

    public int getMaxArgs() {
      return 1;
    }

    public void run(Shell shell, List args, List subst) throws Throwable {
      if (0 == args.size()) {
        Iterator iter = shell.commands().iterator();

        while (iter.hasNext()) {
          print((Command)iter.next(), shell);
        }

      } else {
        Command cmd = shell.getCommand(substitute((String)args.get(0), subst));

        if (null == cmd) {
          throw new IllegalArgumentException("Invalid command name");
        }

        print(cmd, shell);
      }
    }

    /**
     * Print a description for the specified command to the specified
     * shell's console.
     *
     * @param   cmd    The command.
     * @param   shell  The shell.
     */
    private void print(Command cmd, Shell shell) {
      String args = cmd.getArguments();

      if ("".equals(args)) {
        shell.print(cmd.getName());

      } else {
        shell.print(cmd.getName() + " " + args);
      }

      shell.print("    " + cmd.getDescription());
    }

  }

  // =======================================================================

  /** Implementation of "<code>if</code>". */
  static final class If implements Command {

    public String getName() {
      return "if";
    }

    public String getArguments() {
      return "(([not-]exists|[in]active) <path>) | ([un]defined " +
        "<number>) <command>";
    }

    public String getDescription() {
      return "Conditionally execute command";
    }

    public int getMinArgs() {
      return 3;
    }

    public int getMaxArgs() {
      return Integer.MAX_VALUE;
    }

    public void run(Shell shell, List args, List subst) throws Throwable {
      String      type   = substitute((String)args.get(0), subst);
      String      path   = substitute((String)args.get(1), subst);
      String      name   = (String)args.get(2);
      boolean     exists = true;
      boolean     active = false;
      Environment env    = null;

      // Check type.
      if ((! "defined".equals(type))    &&
          (! "undefined".equals(type))  &&
          (! "exists".equals(type))     &&
          (! "not-exists".equals(type)) &&
          (! "active".equals(type))     &&
          (! "inactive".equals(type))) {
        throw new IllegalArgumentException("Unrecognized condition for if (" +
                                           type + ")");
      }

      // Trim consumed arguments.
      args.remove(0);
      args.remove(0);
      args.remove(0);

      if ("defined".equals(type) || "undefined".equals(type)) {
        // Handle defined and undefined tests.
        int n;

        try {
          n = Integer.parseInt(path);
        } catch (NumberFormatException x) {
          throw new IllegalArgumentException("Malformed number (" + path + ")");
        }
        if (0 >= n) {
          throw new IllegalArgumentException("Number non-positive (" + n + ")");
        }
        
        if (("defined".equals(type)   && (subst.size() >= n)) ||
            ("undefined".equals(type) && (subst.size() <  n))) {
          // Execute the embedded command.
          shell.execute(name, args, subst);
        }

      } else {
        // Determine whether path exists.
        try {
          env = shell.resolve(path);
        } catch (UnknownResourceException x) {
          exists = false;
        }

        // Determine whether environment is active.
        if (exists && (Environment.ACTIVE == env.getStatus())) {
          active = true;
        }
        
        if (("exists".equals(type)     && exists    ) ||
            ("not-exists".equals(type) && (! exists)) ||
            ("active".equals(type)     && active    ) ||
            ("inactive".equals(type)   && (! active))) {
          
          // Execute the embedded command.
          shell.execute(name, args, subst);
        }
      }
    }

  }

  // =======================================================================

  /** Implementation of "<code>import</code>". */
  static final class Import implements Command {

    public String getName() {
      return "import";
    }

    public String getArguments() {
      return "[-flatten] [<path>] <file-system-directory>";
    }

    public String getDescription() {
      return "Recursively import tuples from files";
    }

    public int getMinArgs() {
      return 1;
    }

    public int getMaxArgs() {
      return 3;
    }

    public void run(Shell shell, List args, List subst) throws Throwable {
      substitute(args, subst);

      int size = args.size();

      boolean     flatten    = false;
      Environment env;
      File        directory;

      // Parse command line arguments.
      if (1 == size) {
        env       = shell.getEnvironment();
        directory = new File((String)args.get(0));

      } else if (2 == size) {
        String s  = (String)args.get(0);

        if ("-flatten".equals(s)) {
          flatten = true;
          env     = shell.getEnvironment();
        } else {
          env     = shell.resolve(s);
        }
        directory = new File((String)args.get(1));

      } else {
        if (! "-flatten".equals((String)args.get(0))) {
          throw new IllegalArgumentException("Invalid flag (" +
                                             (String)args.get(0) + ")");
        }
        flatten   = true;
        env       = shell.resolve((String)args.get(1));
        directory = new File((String)args.get(2));
      }

      // Make sure the specified file system entity is a readable
      // directory.
      if (! directory.isDirectory()) {
        throw new IllegalArgumentException("Not a directory (" +
                                           directory.getAbsolutePath() + ")");
      } else if (! directory.canRead()) {
        throw new IllegalStateException("Unable to access directory (" +
                                        directory.getAbsolutePath() + ")");
      }

      // Import tuples.
      importt(shell, directory, env, flatten);
    }

    /**
     * Import all files from the specified parent directory into the
     * specified environment. This method recursively imports all
     * files from the specified directory into the specified
     * environment. The specified directory must be a readable
     * directory; the specified environment must be a valid
     * environment.
     *
     * @param   shell    The shell.
     * @param   parent   The directory.
     * @param   env      The environment.
     * @param   flatten  Flag for whether to flatten the file system
     *                   hierarchy into a single environment or to
     *                   create an equivalent environment hierarchy.
     * @throws  Throwable
     *                   Signals some exceptional condition while
     *                   importing.
     */
    private void importt(Shell shell, File parent, Environment env,
                         boolean flatten)
      throws Throwable {

      BindingResponse br      = null;
      LeaseMaintainer lm      = null;
      File[]          content = parent.listFiles();

      if (null == content) {
        shell.print("Unable to access " + parent.getAbsolutePath() +
                    "; skipping it...");
        return;
      }

      // Process regular files.
      try {
        for (int i=0; i<content.length; i++) {
          File file = content[i];

          // Only process files that we can read.
          if (! file.isFile()) {
            continue;
          } else if (! file.canRead()) {
            shell.print("Unable to access " + file.getAbsolutePath() +
                        "; skipping it...");
            continue;
          }
          
          // Determine MIME type.
          String name = file.getName();
          String type;
          int    idx  = name.lastIndexOf('.');
          if (-1 == idx) {
            type = NetConstants.MIME_TYPE_DEFAULT;
          } else {
            type = MimeTypes.getMimeType(name.substring(idx + 1));
          }
          
          // Open file.
          FileInputStream in;
          try {
            in = new FileInputStream(file);
          } catch (Throwable x) {
            shell.print("Unable to open " + file.getAbsolutePath() +
                        "; skipping it...");
            continue;
          }

          // Convert contents to tuple(s).
          try {
            Object o = NetUtilities.convert(in, file.length(), name, type);

            // Do we have a connection to the store?
            if (null == br) {
              SioResource resource = new SioResource();
              resource.type        = SioResource.STORAGE;
              resource.ident       = env.getId();
              
              br = IOUtilities.bind(Environment.getRoot().getRequest(),
                                    resource, Duration.FOREVER);
              lm = new LeaseMaintainer(br.lease, br.duration,
                                       ExceptionHandler.HANDLER,
                                       null, Environment.getRoot().getTimer1());
            }

            if (o instanceof Tuple) {
              // Write the tuple.
              IOUtilities.put(br.resource, (Tuple)o, true);

            } else if (o instanceof Iterator) {
              // Write the tuples.
              Iterator iter = (Iterator)o;

              while (iter.hasNext()) {
                IOUtilities.put(br.resource, (Tuple)iter.next(), true);
              }

            } else {
              throw new Bug("Tuple conversion result " + o +
                            " has unrecognized type");
            }
          } catch (Throwable x) {
            shell.print("Unable to convert " + file.getAbsolutePath() +
                        " to tuple(s); skipping it...");
            continue;

          } finally {
            try {
              in.close();
            } catch (IOException x) {
              // Ignore.
            }
          }
        }
      } finally {
        // Cancel lease.
        if (null != lm) {
          lm.cancel();
        }
      }

      // Process directories.
      for (int i=0; i<content.length; i++) {
        File dir = content[i];

        // Only process directories we can read.
        if (! dir.isDirectory()) {
          continue;
        } else if (! dir.canRead()) {
          shell.print("Unable to access " + dir.getAbsolutePath() +
                      "; skipping it...");
          continue;
        }

        // Get or create the child environment of the same name.
        Environment child;

        if (flatten) {
          child = env;
        } else {
          child = env.getChild(dir.getName());
          if (null == child) {
            try {
              child = Environment.create(null, env.getId(), dir.getName(),
                                         false);
            } catch (Throwable x) {
              shell.print("Unable to create environment " + dir.getName() +
                          " as child of " + env.getId() + " (" + x +
                          "); skipping it...");
              continue;
            }
          }
        }

        importt(shell, dir, child, flatten);
      }
    }

  }

  // =======================================================================

  /** Implementation of "<code>info</code>". */
  static final class Info implements Command {

    public String getName() {
      return "info";
    }

    public String getArguments() {
      return "";
    }

    public String getDescription() {
      return "Print system information";
    }

    public int getMinArgs() {
      return 0;
    }

    public int getMaxArgs() {
      return 0;
    }

    public void run(Shell shell, List args, List subst) throws Throwable {
      DecimalFormat f = new DecimalFormat("###,###,###,###,###,###");

      shell.print("one.world                    version " +
                  Constants.MAJOR_VERSION + "." +
                  Constants.MINOR_VERSION +  "." +
                  Constants.MICRO_VERSION + ", built " +
                  Constants.BUILD_TIME + " GMT");
      shell.print();

      shell.print("Java                         version " +
                  System.getProperty("java.version"));
      shell.print("Java virtual machine         " +
                  System.getProperty("java.vm.name") + ", version " +
                  System.getProperty("java.vm.version"));
      if (-1 == Constants.CAFFEINE_MARK_RATING) {
        shell.print("CaffeineMark rating          (not available)");
      } else {
        shell.print("CaffeineMark rating          " +
                    Constants.CAFFEINE_MARK_RATING);
      }
      shell.print("Operating system             " +
                  System.getProperty("os.name") + ", version " +
                  System.getProperty("os.version"));
      shell.print("Architecture                 " +
                  System.getProperty("os.arch"));
      shell.print();

      shell.print("System time                  " +
                  SystemUtilities.format(System.currentTimeMillis()));
      shell.print("Uptime                       " +
                  Duration.format(SystemUtilities.uptime()));
      shell.print("Total memory                 " +
                  f.format(SystemUtilities.totalMemory()) + " bytes");
      shell.print("Free memory                  " + 
                  f.format(SystemUtilities.freeMemory()) + " bytes");
      shell.print("Active threads               " +
                  f.format(SystemUtilities.activeThreads()));
      shell.print();

      String ipAddress = SystemUtilities.ipAddress();
      shell.print("IP address                   " +
                  ((null == ipAddress)? "(not available)" : ipAddress));
      String hostName;
      try {
        hostName = InetAddress.getLocalHost().getHostName();
      } catch (UnknownHostException x) {
        hostName = null;
      }
      shell.print("Host name                    " +
                  ((null == hostName)? "(not available)" : hostName));
      shell.print();

      shell.print("Structured I/O default port  " + Constants.PORT);
      shell.print("REP port                     " + Constants.REP_PORT);
      shell.print("Discovery announce address   " +
                  Constants.DISCOVERY_ANNOUNCE_ADDR + ":" +
                  Constants.DISCOVERY_ANNOUNCE_PORT);
      shell.print("Discovery election address   " +
                  Constants.DISCOVERY_ELECTION_ADDR + ":" +
                  Constants.DISCOVERY_ELECTION_PORT);
    }

  }

  // =======================================================================

  /** Implementation of "<code>load</code>". */
  static final class Load implements Command {

    public String getName() {
      return "load";
    }

    public String getArguments() {
      return "[-path <path>] <class-name> <closure>*";
    }

    public String getDescription() {
      return "Load application into environment";
    }

    public int getMinArgs() {
      return 1;
    }

    public int getMaxArgs() {
      return Integer.MAX_VALUE;
    }

    public void run(Shell shell, List args, List subst) throws Throwable {
      substitute(args, subst);

      Environment env     = shell.getEnvironment();
      String      init    = (String)args.get(0);
      ArrayList   closure = null;

      if ("-path".equals(init)) {
        if (3 > args.size()) {
          throw new IllegalArgumentException("Invalid number of arguments (" +
                                             args.size() + ")");
        } else {
          env  = shell.resolve((String)args.get(1));
          init = (String)args.get(2);
          args.remove(0);
          args.remove(0);
        }
      }

      args.remove(0);

      Environment.load(null, env.getId(), init,
                       args.toArray(new String[args.size()]));
    }

  }

  // =======================================================================

  /** Implementation of "<code>ls</code>". */
  static final class Ls implements Command {

    public String getName() {
      return "ls";
    }

    public String getArguments() {
      return "[-R] [<path>]";
    }

    public String getDescription() {
      return "List child environments";
    }

    public int getMinArgs() {
      return 0;
    }

    public int getMaxArgs() {
      return 2;
    }

    public void run(Shell shell, List args, List subst) throws Throwable {
      substitute(args, subst);

      boolean     recurse = false;
      Environment env;

      // Process arguments.
      if (0 == args.size()) {
        // Just list the current environment.
        env = shell.getEnvironment();

      } else if (1 == args.size()) {
        String s = (String)args.get(0);

        if ("-R".equals(s)) {
          // Recursively list the current environment.
          env     = shell.getEnvironment();
          recurse = true;

        } else {
          // List the specified environment.
          env = shell.resolve(s);
        }

      } else {
        // Recursively list the specified environment.
        if (! "-R".equals(args.get(0))) {
          throw new IllegalArgumentException("Invalid flag (" +
                                             (String)args.get(0) + ")");
        }
        recurse = true;
        env     = shell.resolve((String)args.get(1));
      }

      // List the specified environment.
      List     children = env.getChildren();
      Iterator iter     = children.iterator();

      while (iter.hasNext()) {
        Environment child = env.getChild((String)iter.next());
        if (null != child) {
          shell.print(child, false);
        }
      }
      
      // Recursively list children.
      if (recurse && (! children.isEmpty())) {
        shell.print();

        iter = children.iterator();

        while (iter.hasNext()) {
          Environment child = env.getChild((String)iter.next());
          if (null != child) {
            list(child, env, shell);
          }
        }
      }

    }

    /**
     * Print the children of the specified environment to the specified
     * shell.
     *
     * @param   env       The environment, whose children to print.
     * @param   ancestor  The ancestor for the relative path.
     * @param   shell     The shell to print to.
     */
    private void list(Environment env, Environment ancestor, Shell shell) {
      List children = env.getChildren();

      // Print the contents of env.
      shell.print(shell.getPath(ancestor, env) + ":");
      Iterator iter = children.iterator();
      while (iter.hasNext()) {
        Environment child = env.getChild((String)iter.next());
        if (null != child) {
          shell.print(child, false);
        }
      }
      shell.print();

      // Recurse to the children.
      iter = children.iterator();
      while (iter.hasNext()) {
        Environment child = env.getChild((String)iter.next());
        if (null != child) {
          list(child, ancestor, shell);
        }
      }
    }

  }

  // =======================================================================

  /** Implementation of "<code>mk</code>". */
  static final class Mk implements Command {

    public String getName() {
      return "mk";
    }

    public String getArguments() {
      return "[-path <path>] [-inherit] <env-name> [<class-name> <closure>*]";
    }

    public String getDescription() {
      return "Make new environment";
    }

    public int getMinArgs() {
      return 1;
    }

    public int getMaxArgs() {
      return Integer.MAX_VALUE;
    }

    public void run(Shell shell, List args, List subst) throws Throwable {
      substitute(args, subst);

      Environment env     = shell.getEnvironment();
      String      name    = (String)args.get(0);
      boolean     inherit = false;
      String      init    = null;

      if ("-path".equals(name)) {
        if (3 > args.size()) {
          throw new IllegalArgumentException("Invalid number of arguments (" +
                                             args.size() + ")");
        } else {
          env  = shell.resolve((String)args.get(1));
          name = (String)args.get(2);
          args.remove(0);
          args.remove(0);
        }
      }

      if ("-inherit".equals(name)) {
        if (2 > args.size()) {
          throw new IllegalArgumentException("Missing environment name");
        } else {
          inherit = true;
          name    = (String)args.get(1);
          args.remove(0);
        }
      }
      
      args.remove(0);

      if (1 <= args.size()) {
        init = (String)args.get(0);
        args.remove(0);
      }

      if (null == init) {
        Environment.create(null, env.getId(), name, inherit);
      } else {
        Environment.create(null, env.getId(), name, inherit, init,
                           args.toArray(new String[args.size()]));
      }
    }

  }

  // =======================================================================

  /** Implementation of "<code>mv</code>". */
  static final class Mv implements Command {

    public String getName() {
      return "mv";
    }

    public String getArguments() {
      return "[<path>] (<new-name>|<new-location>)";
    }

    public String getDescription() {
      return "Rename or move environment";
    }

    public int getMinArgs() {
      return 1;
    }

    public int getMaxArgs() {
      return 2;
    }

    public void run(Shell shell, List args, List subst) throws Throwable {
      substitute(args, subst);

      int size = args.size();

      Environment env;
      String      name;

      if (1 == size) {
        env  = shell.getEnvironment();
        name = (String)args.get(0);

      } else {
        env  = shell.resolve((String)args.get(0));
        name = (String)args.get(1);

      }

      // Handle complete structured I/O URLs first.
      if (name.startsWith("sio://")) {
        SioResource resource = new SioResource(name);
        if (SioResource.STORAGE != resource.type) {
          throw new IllegalArgumentException("Invalid path (" + name + ")");
        } else if ((null != resource.remoteHost) &&
                   SystemUtilities.isLocalHost(resource.remoteHost) &&
                   ((-1 == resource.remotePort) ||
                    (Constants.REP_PORT == resource.remotePort))) {
          resource.remoteHost = null;
          name                = resource.toString();
        }

        if (null != resource.remoteHost) {
          String        remoteHost = resource.remoteHost;
          int           remotePort = resource.remotePort;
          resource.remoteHost      = null;
          ResultHandler rh         = new ResultHandler();
          Environment.moveAway(null, rh, null, env.getId(), remoteHost,
                               remotePort, resource.toString(), false);
          Event         result     = rh.getResult(Duration.YEAR);
          if (result instanceof ExceptionalEvent) {
            throw ((ExceptionalEvent)result).x;
          }
          return;
        }
      }

      // If the name denotes an existing environment, move there;
      // otherwise, treat is as a new name.
      if (".".equals(name)) {
        // "." exists and we are done.
        return;

      } else if ("..".equals(name) || (-1 != name.indexOf('/'))) {
        // ".." and names containing '/' denote existing environments.
        Environment.move(null, env.getId(), shell.resolve(name).getId());
        return;

      } else {
        try {
          shell.resolve(name);
        } catch (Throwable x) {
          Environment.rename(null, env.getId(), name);
          return;
        }

        Environment.move(null, env.getId(), shell.resolve(name).getId());
      }
    }

  }

  // =======================================================================

  /** Implementation of "<code>ps</code>". */
  static final class Ps implements Command {

    public String getName() {
      return "ps";
    }

    public String getArguments() {
      return "";
    }

    public String getDescription() {
      return "Print active threads";
    }

    public int getMinArgs() {
      return 0;
    }

    public int getMaxArgs() {
      return 0;
    }

    public void run(Shell shell, List args, List subst) throws Throwable {
      ThreadGroup group   = Thread.currentThread().getThreadGroup();
      Thread[]    threads = new Thread[group.activeCount()];
      int         size    = group.enumerate(threads);

      for (int i=0; i<size; i++) {
        shell.print(threads[i].toString());
      }
    }

  }

  // =======================================================================

  /** Implementation of "<code>pwd</code>". */
  static final class Pwd implements Command {

    public String getName() {
      return "pwd";
    }

    public String getArguments() {
      return "";
    }

    public String getDescription() {
      return "Print ID and path of current environment";
    }

    public int getMinArgs() {
      return 0;
    }

    public int getMaxArgs() {
      return 0;
    }

    public void run(Shell shell, List args, List subst) throws Throwable {
      shell.print(shell.getEnvironment(), true);
    }

  }

  // =======================================================================

  /** Implementation of "<code>restore</code>". */
  static final class Restore implements Command {

    public String getName() {
      return "restore";
    }

    public String getArguments() {
      return "[<path>] <timestamp>";
    }

    public String getDescription() {
      return "Restore environment(s) from check-point";
    }

    public int getMinArgs() {
      return 1;
    }

    public int getMaxArgs() {
      return 2;
    }

    public void run(Shell shell, List args, List subst) throws Throwable {
      substitute(args, subst);

      int size = args.size();

      Environment env;
      long        timestamp;

      if (1 == size) {
        env       = shell.getEnvironment();
        timestamp = Long.parseLong((String)args.get(0));

      } else {
        env       = shell.resolve((String)args.get(0));
        timestamp = Long.parseLong((String)args.get(1));
      }

      Environment.restore(null, env.getId(), timestamp);
    }

  }

  // =======================================================================

  /** Implementation of "<code>rm</code>". */
  static final class Rm implements Command {

    public String getName() {
      return "rm";
    }

    public String getArguments() {
      return "[<path>]";
    }

    public String getDescription() {
      return "Destroy environment";
    }

    public int getMinArgs() {
      return 0;
    }

    public int getMaxArgs() {
      return 1;
    }

    public void run(Shell shell, List args, List subst) throws Throwable {
      Environment env;

      if (0 == args.size()) {
        env = shell.getEnvironment();
      } else {
        env = shell.resolve(substitute((String)args.get(0), subst));
      }

      Environment.destroy(null, env.getId());
    }

  }

  // =======================================================================

  /** Implementation of "<code>run</code>". */
  static final class Run implements Command {

    public String getName() {
      return "run";
    }

    public String getArguments() {
      return "[<path>]";
    }

    public String getDescription() {
      return "Activate environment";
    }

    public int getMinArgs() {
      return 0;
    }

    public int getMaxArgs() {
      return 1;
    }

    public void run(Shell shell, List args, List subst) throws Throwable {
      Environment env;

      if (0 == args.size()) {
        env = shell.getEnvironment();
      } else {
        env = shell.resolve(substitute((String)args.get(0), subst));
      }

      Environment.activate(null, env.getId());
    }

  }

  // =======================================================================

  /** Implementation of "<code>set</code>". */
  static final class Set implements Command {

    public String getName() {
      return "set";
    }

    public String getArguments() {
      return "[<key> [<value>]]";
    }

    public String getDescription() {
      return "Access system properties";
    }

    public int getMinArgs() {
      return 0;
    }

    public int getMaxArgs() {
      return 2;
    }

    public void run(Shell shell, List args, List subst) throws Throwable {
      substitute(args, subst);

      int size = args.size();

      if (0 == size) {
        Properties props = System.getProperties();
        Iterator   iter  = props.keySet().iterator();

        while (iter.hasNext()) {
          String key   = (String)iter.next();
          String value = props.getProperty(key);

          shell.print(key + " " + value);
        }

      } else if (1 == size) {
        String key = (String)args.get(0);

        shell.print(key + " " + System.getProperty(key));

      } else if (2 == size) {
        System.setProperty((String)args.get(0), (String)args.get(1));
      }
    }

  }

  // =======================================================================

  /** Implementation of "<code>source</code>". */
  static final class Source implements Command {

    public String getName() {
      return "source";
    }

    public String getArguments() {
      return "<script> <arg>*";
    }

    public String getDescription() {
      return "Execute script";
    }

    public int getMinArgs() {
      return 1;
    }

    public int getMaxArgs() {
      return Integer.MAX_VALUE;
    }

    public void run(Shell shell, List args, List subst) throws Throwable {
      substitute(args, subst);

      String name = (String)args.get(0);
      args.remove(0);

      shell.source(name, args);
    }

  }

  // =======================================================================

  /** Implementation of "<code>stop</code>". */
  static final class Stop implements Command {

    public String getName() {
      return "stop";
    }

    public String getArguments() {
      return "[<path>]";
    }

    public String getDescription() {
      return "Terminate environment";
    }

    public int getMinArgs() {
      return 0;
    }

    public int getMaxArgs() {
      return 1;
    }

    public void run(Shell shell, List args, List subst) throws Throwable {
      Environment env;

      if (0 == args.size()) {
        env = shell.getEnvironment();
      } else {
        env = shell.resolve(substitute((String)args.get(0), subst));
      }

      Environment.terminate(null, env.getId());
    }

  }

  // =======================================================================

  /** Implementation of "<code>unload</code>". */
  static final class Unload implements Command {

    public String getName() {
      return "unload";
    }

    public String getArguments() {
      return "[<path>]";
    }

    public String getDescription() {
      return "Unload code from environment";
    }

    public int getMinArgs() {
      return 0;
    }

    public int getMaxArgs() {
      return 1;
    }

    public void run(Shell shell, List args, List subst) throws Throwable {
      Environment env;

      if (0 == args.size()) {
        env = shell.getEnvironment();
      } else {
        env = shell.resolve(substitute((String)args.get(0), subst));
      }

      Environment.unload(null, env.getId());
    }

  }

  // =======================================================================

  /** Implementation of "<code>wait</code>". */
  static final class Wait implements Command {

    public String getName() {
      return "wait";
    }

    public String getArguments() {
      return "([not-]exists|[in]active) <path> [<timeout>]";
    }

    public String getDescription() {
      return "Wait for a condition";
    }

    public int getMinArgs() {
      return 2;
    }

    public int getMaxArgs() {
      return 3;
    }

    public void run(Shell shell, List args, List subst) throws Throwable {
      substitute(args, subst);

      String      type    = (String)args.get(0);
      String      path    = (String)args.get(1);
      long        timeout = Constants.SYNCHRONOUS_TIMEOUT;
      long        end;

      // Check type.
      if ((! "exists".equals(type))     &&
          (! "not-exists".equals(type)) &&
          (! "active".equals(type))     &&
          (! "inactive".equals(type))) {
        throw new IllegalArgumentException("Unrecognized condition for wait (" +
                                           type + ")");
      }

      // Parse timeout if specified.
      if (3 == args.size()) {
        try {
          timeout = Long.parseLong((String)args.get(2));
        } catch (NumberFormatException x) {
          throw new IllegalArgumentException("Malformed timeout (" +
                                             args.get(2) + ")");
        }
        if (0 > timeout) {
          throw new IllegalArgumentException("Negative timeout (" +
                                             args.get(2) + ")");
        }
      }

      // Set up ending time.
      end = System.currentTimeMillis() + timeout;

      // Do the waiting.
      do {
        boolean     exists = true;
        boolean     active = false;
        Environment env    = null;

        // Determine whether path exists.
        try {
          env = shell.resolve(path);
        } catch (UnknownResourceException x) {
          exists = false;
        }

        // Determine whether environment is active.
        if (exists && (Environment.ACTIVE == env.getStatus())) {
          active = true;
        }

        // Are we done waiting?
        if (("exists".equals(type)     && exists    ) ||
            ("not-exists".equals(type) && (! exists)) ||
            ("active".equals(type)     && active    ) ||
            ("inactive".equals(type)   && (! active))) {
          return;
        }

        // Wait a little.
        try {
          Thread.sleep(100);
        } catch (InterruptedException x) {
          // Ignore.
        }

      } while (System.currentTimeMillis() < end);

      // Signal timeout.
      throw new TimeOutException("Wait timed out after " +
                                 Duration.format(timeout));
    }

  }

  // =======================================================================

  /** The current environment. */
  private Environment current;

  /** The commands. */
  private HashMap     commands;

  // =======================================================================

  /**
   * Create a new shell.
   */
  private Shell() {
    current  = Environment.getRoot();
    commands = new HashMap();

    addCommand(new Cat());
    addCommand(new Cd());
    addCommand(new Checkpoint());
    addCommand(new Clean());
    addCommand(new Cp());
    addCommand(new Echo());
    addCommand(new Errorr());
    addCommand(new Exit());
    addCommand(new Export());
    addCommand(new Gc());
    addCommand(new Help());
    addCommand(new If());
    addCommand(new Import());
    addCommand(new Info());
    addCommand(new Load());
    addCommand(new Ls());
    addCommand(new Mk());
    addCommand(new Mv());
    addCommand(new Pwd());
    addCommand(new Ps());
    addCommand(new Restore());
    addCommand(new Rm());
    addCommand(new Run());
    addCommand(new Set());
    addCommand(new Source());
    addCommand(new Stop());
    addCommand(new Unload());
    addCommand(new Wait());
  }

  /**
   * Add the specified command to this shell.
   *
   * @param   cmd  The command to add.
   * @throws  NullPointerException
   *               Signals that <code>cmd</code> is <code>null</code>.
   * @throws  IllegalArgumentException
   *               Signals that a command with the same name already
   *               exists.
   */
  public void addCommand(Command cmd) {
    if (null == cmd) {
      throw new NullPointerException("Null command");
    }

    String name = cmd.getName();

    if (commands.containsKey(name)) {
      throw new IllegalArgumentException("Command already exists ("+name+")");
    }

    commands.put(name, cmd);
  }

  /**
   * Get the specified command.
   *
   * @param   name  The name of the command.
   * @return        The command or <code>null</code> if no such command
   *                exists.
   */
  public Command getCommand(String name) {
    return (Command)commands.get(name);
  }
  
  /**
   * Get the commands for this shell.
   *
   * @return  The commands for this shell.
   */
  public Collection commands() {
    return commands.values();
  }

  /** Run the loop for this shell. */
  public void loop() {
    // Get the root shell password.
    String pwd = System.getProperty("one.world.shell.pwd");
    if ("".equals(pwd)) {
      pwd = null;
    }

    if (null == pwd) {
      // Run the local loop.
      loopLocally();

    } else {
      // Run the root shell through telnet.
      ServerSocket serverSocket;

      try {
        serverSocket = new ServerSocket(Constants.SHELL_PORT);
      } catch (IOException x) {
        print("Unable to open telnet socket (" + x + ")");
        print("Running local shell...");
        loopLocally();
        return;
      }

      loopRemotely(serverSocket, pwd);
    }
  }

  /** Run the local loop for this shell. */
  private void loopLocally() {
    BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

    while (true) {
      consoleOutput.print(">");
      consoleOutput.flush();
        
      // Read a line.
      String line;
      
      try {
        line = in.readLine();
      } catch (IOException x) {
        print(x);
        print();
        flush();
        continue;
      }
      
      // Handle EOF.
      if (null == line) {
        print();
        flush();
        return;
      }
      
      try {
        execute(line, Collections.EMPTY_LIST);
      } catch (ExitException x) {
        return;
      } catch (Throwable x) {
        print(x);
      }
      
      print();
      flush();
    }
  }

  /**
   * Run the remote loop for this shell.
   *
   * @param   serverSocket  The server socket to accept remote
   *                        connections.
   * @param   pwd           The password.
   */
  private void loopRemotely(ServerSocket serverSocket, String pwd) {
    do {
      // Accept a TCP connection.
      Socket socket;

      try {
        socket = serverSocket.accept();
      } catch (IOException x) {
        // Try again.
        continue;
      }

      // Turn it into a telnet connection.
      Telnet telnet;
      try {
        telnet = new Telnet(socket, 20);
      } catch (IOException x) {
        // Close socket.
        try {
          socket.close();
        } catch (IOException xx) {
          // Ignore.
        }
        // Try again.
        continue;
      }

      // Fix console output.
      consoleOutput.telnet = telnet;

      // Read in password.
      String pwd2 = null;

      for (int i=0; i<3; i++) {
        // We give the other side three attempts to get it right.
        try {
          telnet.write("Password:");
          pwd2 = telnet.readPassword();
          telnet.writeLine();
        } catch (IOException x) {
          consoleOutput.telnet = null;
          telnet.close();
          break;
        }

        if (pwd.equals(pwd2)) {
          break;
        }
      }

      // Verify password.
      if (! pwd.equals(pwd2)) {
        consoleOutput.telnet = null;
        telnet.close();
        
        // Try again.
        continue;
      }

      // Do the actual looping.
      while (true) {
        // Write prompt.
        try {
          telnet.write(">");
        } catch (IOException x) {
          consoleOutput.telnet = null;
          telnet.close();
          break;
        }
        
        // Read a line.
        String line;
        
        try {
          line = telnet.readLine();
        } catch (IOException x) {
          consoleOutput.telnet = null;
          telnet.close();
          break;
        }
        
        // Handle EOF.
        if (null == line) {
          consoleOutput.telnet = null;
          telnet.close();
          break;
        }
        
        // Terminate read-in line.
        try {
          telnet.writeLine();
        } catch (IOException x) {
          consoleOutput.telnet = null;
          telnet.close();
          break;
        }
        
        // Execute line.
        try {
          execute(line, Collections.EMPTY_LIST);
        } catch (ExitException x) {
          consoleOutput.telnet = null;
          telnet.close();
          return;
        } catch (Throwable x) {
          print(x);
        }
        
        print();
        flush();
      }
    } while (true);
    
  }

  /**
   * Source the specified file. This method opens the file with the
   * specified name and treats its contents as a script, to be
   * executed line by line.
   *
   * @param   name   The name of the file to source.
   * @throws  Throwable
   *                 Signals an exceptional condition while
   *                 processing the specified script file.
   */
  public void source(String name) throws Throwable {
    source(name, Collections.EMPTY_LIST);
  }

  /**
   * Source the specified file. This method opens the file with the
   * specified name and treats its contents as a script, to be
   * executed line by line.
   *
   * <p>The list of substitutions <code>subst</code> specifies the
   * substitutions to be performed while executing the script.</p>
   * 
   * @param   name   The name of the file to source.
   * @param   subst  The substitutions (to be performed).
   * @throws  Throwable
   *                 Signals an exceptional condition while
   *                 processing the specified script file.
   */
  public void source(String name, List subst) throws Throwable {
    BufferedReader in = new
      BufferedReader(new InputStreamReader(new FileInputStream(name)));

    try {
      String line;

      while (null != (line = in.readLine())) {
        execute(line, subst);
      }
    } finally {
      in.close();
    }
  }

  /**
   * Execute the specified line. This method parses the specified line
   * and applies the resulting command on the resulting arguments.
   *
   * <p>Comments (which start with '<code>#</code>') are stripped
   * during parsing. Effectively empty lines are ignored.</p>
   *
   * <p>The list of substitutions <code>subst</code> specifies the
   * substitutions to be performed while executing the script.</p>
   *
   * @param   line   The line to execute.
   * @param   subst  The substitutions (to be performed).
   * @throws  Throwable
   *                 Signals an exceptional condition while parsing the
   *                 specified line or while executing the specified
   *                 command.
   */
  public void execute(String line, List subst) throws Throwable {
    // Eliminate comments.
    int idx = line.indexOf('#');
    if (-1 != idx) {
      line = line.substring(0, idx);
    }

    // Tim line.
    line = line.trim();

    // Handle empty lines.
    if ("".equals(line)) {
      return;
    }

    // Extract command name and arguments.
    StringTokenizer tok  = new StringTokenizer(line);
    String          name = null;
    ArrayList       args = null;
    
    while (tok.hasMoreTokens()) {
      String token = tok.nextToken();
      
      if ("".equals(token)) {
        continue;
      } else if (null == name) {
        name = token;
      } else {
        if (null == args) {
          args = new ArrayList();
        }
        
        args.add(token);
      }
    }
    
    // Get command and make sure the arguments are right.
    execute(name, args, subst);
  }

  /**
   * Execute the command with the specified name on the specified
   * arguments. This method resolves the specified name and verifies
   * that the command exists as well as that the specified number of
   * arguments is a legal number for that command. It then applies the
   * command on the specified arguments.
   *
   * <p>The list of substitutions <code>subst</code> specified the
   * substitutions to be performed while executing the script. This
   * method substitutes the command name before resolving it.</p>
   * 
   * @param   name   The name of the command.
   * @param   args   The arguments for the command, or <code>null</code>
   *                 if there are no arguments.
   * @param   subst  The substitutions.
   * @throws  Throwable
   *                 Signals an exceptional condition while executing
   *                 the specified command.
   */
  public void execute(String name, List args, List subst) throws Throwable {
    Command cmd = (Command)commands.get(substitute(name, subst));

    if (null == cmd) {
      throw new IllegalArgumentException("Unrecognized command (" + name + ")");
    }

    int size = ((null == args)? 0 : args.size());
    if ((cmd.getMinArgs() > size) || (cmd.getMaxArgs() < size)) {
      throw new IllegalArgumentException("Invalid number of arguments (" +
                                         size + ")");
    }

    cmd.run(this, ((null == args)? Collections.EMPTY_LIST : args), subst);
  }

  /**
   * Get the current environment.
   *
   * @return     The current environment.
   * @throws  UnknownResourceException
   *             Signals that the current environment does not exist
   *             anymore, because, for example, it has been deleted
   *             or moved.
   */
  public Environment getEnvironment() throws UnknownResourceException {
    // Make sure that the current environment is still the current
    // environment. This is important for cases where the environment
    // was moved to another node and has returned.
    try {
      current = Environment.lookup(current.getId());
    } catch (IllegalArgumentException x) {
      throw new UnknownResourceException(x.getMessage());
    }

    return current;
  }

  /**
   * Set the current environment.
   *
   * @param   env  The new current environment.
   * @throws  NullPointerException
   *               Signals that <code>env</code> is <code>null</code>.
   * @throws  IllegalArgumentException
   *               Signals that the specified environment is not a
   *               valid environment anymore (for example, because it
   *               has been deleted).
   */
  public void setEnvironment(Environment env) {
    if (null == env) {
      throw new NullPointerException("Null environment");
    }

    // Make sure env still exists.
    Environment.lookup(env.getId());

    current = env;
  }

  /**
   * Resolve the specified path. The specified path may be absolute,
   * ID relative, or relative. Relative paths are resolved relative to
   * the current environment for this shell.
   *
   * @param   path  The path to resolve.
   * @return        The corresponding environment.
   * @throws  UnknownResourceException
   *                Signals that the specified environment does not
   *                exist.
   */
  public Environment resolve(String path) throws UnknownResourceException {
    return resolve(path, current);
  }

  /**
   * Resolve the specified path. The specified path may be absolute,
   * ID relative, or relative. Relative paths are resolved relative to
   * the specified environment.
   *
   * @param   path  The path to resolve.
   * @param   env   The environment for relative paths.
   * @return        The corresponding environment.
   * @throws  UnknownResourceException
   *                Signals that the specified environment does not
   *                exist.
   */
  public Environment resolve(String path, Environment env)
    throws UnknownResourceException {

    SioResource res;

    // Construct a structured I/O resource descriptor from the
    // specified path.
    if ('/' == path.charAt(0)) {
      // Absolute path.
      res = new SioResource("sio://" + path);
    } else {
      // Relative path.
      res = new SioResource(path);
    }

    // Fill in the specified environment for relative paths.
    if ((null == res.ident) &&
        (null != res.path) &&
        (0 < res.path.length) &&
        (! "/".equals(res.path[0]))) {
      res.ident = env.getId();
    }

    // Do the actual resolution.
    return Environment.resolve(null, res.ident, res.path);
  }

  /**
   * Get the path from the specified ancestor to the specified
   * descendant. If the specified ancestor is not an ancestor of the
   * specified descendant, the returned path is relative to the root
   * environment.
   *
   * @param   ancestor    The ancestor environment.
   * @param   descendant  The descendant environment.
   * @return              The path between the environments, excluding
   *                      the ancestor environment.
   * @throws  NullPointerException
   *                      Signals that <code>ancestor</code> or
   *                      <code>descendant</code> is <code>null</code>.
   */
  public static String getPath(Environment ancestor, Environment descendant) {
    if ((null == ancestor) || (null == descendant)) {
      throw new NullPointerException("Null environment");
    }
    
    return getPath(ancestor, descendant, new StringBuffer()).toString();
  }

  /**
   * Fill the path from the specified ancestor to the specified
   * descendant into the specified string buffer.
   *
   * @param   ancestor    The ancestor environment.
   * @param   descendant  The descendant environment.
   * @param   buf         The string buffer for the path.
   * @return              <code>buf</code>.
   */
  private static StringBuffer getPath(Environment ancestor,
                                      Environment descendant,
                                      StringBuffer buf) {
    boolean recursed = false;

    if (ancestor == descendant) {
      return buf;
    }

    Environment parent = descendant.getParent();

    if (null == parent) {
      return buf;
    } else if (ancestor != parent) {
      getPath(ancestor, parent, buf);
      recursed = true;
    }

    if (recursed) {
      buf.append('/');
    }
    buf.append(descendant.getName());

    return buf;
  }

  /**
   * Perform substitution on the specified list of arguments. If any
   * of the specified arguments starts with '<code>%</code>', the rest
   * of the argument is treated as a number and the argument is
   * replaced by the corresponding actual argument from the specified
   * substitutions. The first substitution index is 1.
   *
   * @param   args   The arguments.
   * @param   subst  The substitions.
   * @throws  IllegalArgumentException
   *                 Signals a malformed substitution token or that
   *                 no entry with the corresponding number exists in
   *                 the list of substitutions.
   */
  public static void substitute(List args, List subst) {
    int size = args.size();

    for (int i=0; i<size; i++ ) {
      args.set(i, substitute((String)args.get(i), subst));
    }
  }

  /**
   * Perform substitution on the specified token. If the specified
   * token starts with '<code>%</code>', this method treats the rest
   * of the token as a number and returns the corresponding entry from
   * the specified list of substitutions. The first substitution index
   * is 1. If the specified token does not start with
   * '<code>%</code>', it is simply returned.
   *
   * @param   token  The token to substitute.
   * @param   subst  The substitutions.
   * @return         The substituted token.
   * @throws  IllegalArgumentException
   *                 Signals a malformed substitution token or that
   *                 no entry with the corresponding number exists in
   *                 the list of substitutions.
   */
  public static String substitute(String token, List subst) {
    if ("".equals(token)) {
      return token;

    } else if ('%' == token.charAt(0)) {
      int n;

      try {
        n = Integer.parseInt(token.substring(1));
      } catch (NumberFormatException x) {
        throw new IllegalArgumentException("Malformed number for substitution" +
                                           " token (" + token + ")");
      }

      try {
        return (String)subst.get(n - 1);
      } catch (IndexOutOfBoundsException x) {
        throw new IllegalArgumentException("Invalid number for substitution" +
                                           " token (" + token + ")");
      }

    } else {
      return token;
    }
  }

  /** Print a new line to this shell's console. */
  public void print() {
    consoleOutput.println();
  }

  /**
   * Print the specified message to this shell's console.
   *
   * @param   msg  The message to print.
   */
  public void print(String msg) {
    consoleOutput.println(msg);
  }

  /**
   * Print a description of the specified environment to this shell's
   * console.
   *
   * @param   env       The environment to print.
   * @param   fullPath  Flag for whether to print the full path
   *                    of the specified environment.
   */
  public void print(Environment env, boolean fullPath) {
    StringBuffer buf = new StringBuffer();
    
    buf.append(env.getId().toString());

    if (env.getId().equals(env.getProtectionDomain().getId())) {
      buf.append(" (prot)");
    } else {
      buf.append("       ");
    }

    int status = env.getStatus();
    switch (status) {
    case Environment.INACTIVE:
      buf.append(" (inact)   ");
      break;

    case Environment.ACTIVE:
      buf.append(" (act)     ");
      break;
      
    case Environment.TERMINATING:
      buf.append(" (term)    ");
      break;

    case Environment.DESTROYING:
      buf.append(" (desting) ");
      break;

    case Environment.DESTROYED:
      buf.append(" (dested)  ");
      break;

    default:
      throw new Bug("Invalid environment status (" + status + ")");
    }

    buf.append(fullPath? "/" + getPath(Environment.getRoot(), env)
               : env.getName());

    consoleOutput.println(buf.toString());
  }

  /**
   * Print the specified throwable to this shell's console. A
   * description of the specified throwable is written in a new line.
   *
   * @param  x  The throwable to print.
   */
  public void print(Throwable x) {
    // Special handling for user exceptions.
    if (x instanceof UserException) {
      String s = x.getMessage();

      if (null != s) {
        consoleOutput.println(" *** " + s);
      }

      return;
    }

    consoleOutput.println(" *** " + x.toString());

    Throwable xx = null;
    if (x instanceof ExceptionInInitializerError) {
      xx = ((ExceptionInInitializerError)x).getException();
    } else if (x instanceof InvocationTargetException) {
      xx = ((InvocationTargetException)x).getTargetException();
    }

    if (null != xx) {
      consoleOutput.print(" *** ");
      xx.printStackTrace(consoleOutput);
    }
  }

  /** Flush this shell's console. */
  public void flush() {
    consoleOutput.flush();
  }

  // =======================================================================

  /** The root shell. */
  private static Shell rootShell;

  /**
   * Get the root shell.
   *
   * @return     The root shell.
   * @throws  SecurityException
   *             Signals that the caller does not have permission to
   *             access the system's console.
   */
  public static Shell getRootShell() {
    SecurityManager security = System.getSecurityManager();
    if (null != security) {
      security.checkPermission(SystemPermission.USE_CONSOLE);
    }

    if (null == rootShell) {
      rootShell = new Shell();
    }
    return rootShell;
  }


  // =======================================================================

  /** The actual system console. */
  private static final Output consoleOutput = new Output();

  /**
   * The system console.  This stream is already open and ready to
   * accept output data.  It replaces Java's <code>System.out</code>
   * and <code>System.err</code> streams and should be used
   * sparringly.  Applications can also use {@link
   * one.world.util.SystemUtilities#debug(String)} and {@link
   * one.world.util.SystemUtilities#debug(Throwable)}.
   */
  public static final PrintStream console = consoleOutput;

}
