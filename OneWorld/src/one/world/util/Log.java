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

import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.PrintStream;

import java.security.AccessController;
import java.security.PrivilegedAction;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import java.util.Date;

import one.world.Shell;

import one.world.core.SystemPermission;

/**
 * Implementation of an event log.
 *
 * @version  $Revision: 1.13 $
 * @author   Robert Grimm
 */
public class Log {
  
  /** The stream to log to. */
  protected PrintStream out;
  
  /** The date format used for formatting the current time. */
  protected DateFormat  formatter;

  /** Create a new system log. */
  private Log() {
    FileOutputStream stream = null;
    try {
      final String name = System.getProperty("one.world.log.name");
      if (null != name) {
        stream = (FileOutputStream)AccessController.doPrivileged(new
          PrivilegedAction() {
            public Object run() {
              try {
                return new FileOutputStream(name, true);
              } catch (FileNotFoundException x) {
                // Ignore.
                return null;
              }
            }
          });
      }
    } catch (SecurityException x) {
      // Ignore.
    }
    
    if (null == stream) {
      out = Shell.console;
    } else {
      out = new PrintStream(stream, true);
    }
    formatter = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
  }

  /**
   * Create a new log.
   *
   * @param  out  The print stream for the new log.
   */
  public Log(PrintStream out) {
    this.out  = out;
    formatter = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
  }

  /**
   * Log the specified informational message.
   *
   * @param  source  The source of the message.
   * @param  msg     The message.
   */
  public synchronized void log(Object source, String msg) {
    StringBuffer buf = new StringBuffer();

    buf.append(formatter.format(new Date()));
    buf.append(' ');
    buf.append("Info ");
    if (null != source) {
      buf.append(source);
      buf.append(' ');
    }
    buf.append('\"');
    int l = msg.length();
    for (int i=0; i<l; i++) {
      char c = msg.charAt(i);
      
      if ('\"' == c) {
        buf.append("\\\"");
      } else if ('\\' == c) {
        buf.append("\\\\");
      } else {
        buf.append(c);
      }
    }
    buf.append('\"');

    out.println(buf.toString());
  }

  /**
   * Log the specified warning message.
   *
   * @param  source  The source of the warning message.
   * @param  msg     The warning message.
   */
  public synchronized void logWarning(Object source, String msg) {
    StringBuffer buf = new StringBuffer();

    buf.append(formatter.format(new Date()));
    buf.append(' ');
    buf.append("Warning ");
    if (null != source) {
      buf.append(source);
      buf.append(' ');
    }
    buf.append('\"');
    int l = msg.length();
    for (int i=0; i<l; i++) {
      char c = msg.charAt(i);
      
      if ('\"' == c) {
        buf.append("\\\"");
      } else if ('\\' == c) {
        buf.append("\\\\");
      } else {
        buf.append(c);
      }
    }
    buf.append('\"');

    out.println(buf.toString());
  }

  /**
   * Log the specified warning message.
   *
   * @param   source  The source of the warning.
   * @param   msg     The warning message.
   * @param   x       The offending throwable.
   */
  public synchronized void logWarning(Object source, String msg, Throwable x) {
    logWarning(source, msg);
    out.print("      Cause ");
    x.printStackTrace(out);
  }

  /**
   * Log the specified error message.
   *
   * @param   source  The source of the error.
   * @param   msg     The error message.
   */
  public synchronized void logError(Object source, String msg) {
    StringBuffer buf = new StringBuffer();

    buf.append(formatter.format(new Date()));
    buf.append(' ');
    buf.append("Error ");
    if (null != source) {
      buf.append(source);
      buf.append(' ');
    }
    buf.append('\"');
    int l = msg.length();
    for (int i=0; i<l; i++) {
      char c = msg.charAt(i);

      if ('\"' == c) {
        buf.append("\\\"");
      } else if ('\\' == c) {
        buf.append("\\\\");
      } else {
        buf.append(c);
      }
    }
    buf.append('\"');

    out.println(buf.toString());
    out.print("      ");
    (new Throwable("Just to get a stack trace...")).printStackTrace(out);
  }

  /**
   * Log the specified error message.
   *
   * @param   source  The source of the error.
   * @param   msg     The error message.
   * @param   x       The offending throwable.
   */
  public synchronized void logError(Object source, String msg, Throwable x) {
    logError(source, msg);
    out.print("      Cause ");
    x.printStackTrace(out);
  }

  /** Finalize this log. This method closes the log. */
  protected void finalize() {
    close();
  }

  /**
   * Close this log. This method should only be called when a log is
   * guaranteed to not be used anymore, such as during a planned
   * shutdown of the system.
   */
  public synchronized void close() {
    out.flush();
    out.close();
  }

  // ======================================================================

  /** The system log. */
  private static Log systemLog = new Log();

  /**
   * Get the system log. If the "<code>one.world.log.name</code>"
   * system property is defined and a file with the correspding name
   * can be opened for writing, the system log writes to that
   * file. Otherwise, it writes to the console.
   *
   * @see     SystemPermission
   * 
   * @return     The current system log.
   * @throws  SecurityException
   *             Signals that the caller does not have permission
   *             to access the system log.
   */
  public static Log getSystemLog() {
    SecurityManager security = System.getSecurityManager();
    if (null != security) {
      security.checkPermission(SystemPermission.USE_LOG);
    }

    return systemLog;
  }

}
