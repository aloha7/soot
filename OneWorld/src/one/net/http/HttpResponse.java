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
import one.world.core.DynamicTuple;
import java.util.StringTokenizer;

/**
 * Representation of a HTTP response.
 * 
 * @author  Daniel Cheah
 * @version $Revision: 1.2 $
 */
public class HttpResponse extends HttpEvent {

  /**
   * Creates a new instance of <code>HttpResponse</code>.
   */
  public HttpResponse() {
    super();
    this.header = new DynamicTuple();
  }

  /**
   * Creates a new instance of <code>HttpResponse</code>.
   *
   * @param source  Source for this event.
   * @param closure Closure for this event.
   */
  public HttpResponse(EventHandler source, Object closure) {
    super(source, closure);
    this.header = new DynamicTuple();
  }

  /**
   * Creates a new instance of <code>HttpResponse</code>.
   *
   * @param he <code>HttpEvent</code> to construct a response from.
   */
  public HttpResponse(HttpEvent he) {
    this(he.version, he.status);
  }

  public HttpResponse(int version, int status) {
    super();
    this.version = version;
    this.status  = status;
    this.header  = new DynamicTuple();
  }

  /**
   * Format the status line from a protocol
   * identifier, a status code, and a reason phrase.
   *
   * @return    The formatted status line.
   * @exception ParseException Signals that the status line cannot be formatted.
   */
  public String formatStatusLine() throws ParseException {
    String reason, v;

    // get the version string
    v = getVersionString(version);
    if (v == null) {
      throw new ParseException(INVALID_VERSION);
    }

    // get the reason string for our status
    reason = HttpConstants.getReason(status);

    // calculate how big the buffer needs to be and create it
    int          len = v.length() + 3 + reason.length() + 2;
    StringBuffer buf = new StringBuffer(len);

    // format the status line
    buf.append(v);
    buf.append(' ');
    buf.append(status);
    buf.append(' ');
    buf.append(reason);

    return buf.toString();
  }

  /** 
   * Is the exception indicative of a bad status line?
   * 
   * @param x Exception to check.
   * @return true if is bad status line, false otherwise.
   */
  public static boolean isBadStatusLine(Exception x) {
    return INVALID_STATUS_LINE.equals(x.getMessage());
  }

  /** 
   * Is the exception indicative of a bad status code?
   * 
   * @param x Exception to check.
   * @return true if is bad status code, false otherwise.
   */
  public static boolean isBadStatus(Exception x) {
    return INVALID_STATUS.equals(x.getMessage());
  }

  /**
   * Parse a status line into a protocol identifier, a status code,
   * and a reason phrase.
   *
   * @param      line            Status line to be parsed.
   * @exception  ParseException  Signals that the status line cannot be parsed.
   */
  public void parseStatusLine(String line) throws ParseException {

    String v; // version
    String s; // status
    String r; // reason
    
    StringTokenizer tok = new StringTokenizer(line, " \t");

    // read the version
    if (! tok.hasMoreElements()) {
      throw new ParseException(INVALID_STATUS_LINE);
    }
    v = tok.nextToken().trim();

    // read the status
    if (! tok.hasMoreElements()) {
      throw new ParseException(INVALID_STATUS_LINE);
    }
    s = tok.nextToken().trim();

    // read the reason
    if (! tok.hasMoreElements()) {
      throw new ParseException(INVALID_STATUS_LINE);
    }
    r = tok.nextToken("\n").trim();

    
    status  = Integer.parseInt(s);
    if (HttpConstants.getReason(status) == null) {
      throw new ParseException(INVALID_STATUS);
    }

    version = getVersionNumber(v);
    if (version == -1) {
      throw new ParseException(INVALID_VERSION);
    }      
  }
}
