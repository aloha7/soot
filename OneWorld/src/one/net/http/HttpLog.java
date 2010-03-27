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
package one.net.http;

import one.world.core.EventHandler;
import java.net.InetAddress;

/**
 * Various logging utility functions used by <code>one.net.http</code 
 * to format log entries.
 *
 * @author  Daniel Cheah
 * @version $Revision: 1.2 $
 */
public class HttpLog {

  /**
   * Formats a log entry that is used for logging access to the server's
   * files.
   *
   * @param logger EventHandler that understands <code>LogEvent</code>s.
   * @param req    A HTTP Request.
   * @param addr   IP Address of the request.
   * @param status Status code of the request.
   * @param size   Size of the response (in octets).
   */
  public static void access(EventHandler logger, 
                            HttpRequest req, 
                            InetAddress addr, 
                            int status, 
                            long size) {

    StringBuffer buf = new StringBuffer();
    buf.append(addr.toString());
    buf.append(" ");
    buf.append("\"");
    buf.append(HttpRequest.getMethodString(req.method));
    buf.append(" ");
    buf.append(req.uri);
    buf.append(" ");
    buf.append(HttpEvent.getVersionString(req.version));
    buf.append("\"");
    buf.append(" ");
    buf.append(status);
    buf.append(" ");
    buf.append(size);

    logger.handle(LogEvent.log("access_log", buf.toString()));
  }

  /**
   * Formats an error log entry.
   *
   * @param logger    EventHandler that understands <code>LogEvent</code>s.
   * @param addr      IP Address of the request.
   * @param className Class that generated the exception.
   * @param message   User-defined error message.
   * @param e         The exception that was throw.
   */
  public static void error(EventHandler logger, 
                           InetAddress addr, 
                           String className,
                           String message,
                           Throwable e) {

    StringBuffer buf = new StringBuffer();
    if (null != addr) {
      buf.append(addr.toString());
      buf.append(" ");
    }
    buf.append(className);
    buf.append(" [");
    buf.append(message);
    buf.append("] ");
    buf.append(e.toString());

    logger.handle(LogEvent.logError("error_log", buf.toString()));
  }

  /**
   * Formats a warning log entry.
   *
   * @param logger    EventHandler that understands <code>LogEvent</code>s.
   * @param addr      IP Address of the request.
   * @param className Class that generated the exception.
   * @param message   User-defined error message.
   * @param e         The exception that was throw.
   */
  public static void warn(EventHandler logger, 
                          InetAddress addr, 
                          String className,
                          String message,
                          Throwable e) {

    StringBuffer buf = new StringBuffer();
    if (null != addr) {
      buf.append(addr.toString());
      buf.append(" ");
    }
    buf.append(className);
    buf.append(" [");
    buf.append(message);
    buf.append("] ");
    buf.append(e.toString());

    logger.handle(LogEvent.logWarning("error_log", buf.toString()));
  }
}
