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

import one.world.core.Event;
import one.world.core.EventHandler;
import one.world.core.DynamicTuple;
import one.world.core.Tuple;
import one.world.core.InvalidTupleException;
import one.world.core.TupleException;
import java.util.StringTokenizer;

/**
 * Representation of a HTTP request.
 *
 * @author  Daniel Cheah
 * @version $Revision: 1.3 $
 */
public class HttpRequest extends HttpEvent {

  /** The HTTP method token. */
  public int     method;

  /** The URI. */
  public String  uri;

  /**
   * Creates a new instance of <code>HttpRequest</code>.
   */
  public HttpRequest() {
    super();
    this.method = 0;
    this.uri    = null;
  }

  /**
   * Creates a new instance of <code>HttpRequest</code>.
   *
   * @param source  Source for this event.
   * @param closure Closure for this event.
   */
  public HttpRequest(EventHandler source, Object closure) {
    super(source, closure);
    this.method = 0;
    this.uri    = null;
  }

  /**
   * Creates a new instance of <code>HttpRequest</code>.
   *
   * @param status Status code of this request.
   */
  public HttpRequest(int status) {
    this.status = status;
  }

  /**
   * Validate this event.
   *
   * @exception TupleException Signals that the data is invalid.
   */
  public void validate() throws TupleException {
    super.validate();

    if (getMethodString(method) == null) {
      throw new InvalidTupleException(INVALID_METHOD);
    }
  }

  /**
   * Format the request line from a method token, a URI,
   * and a protocol identifier.
   *
   * @return    The formatted request line.
   * @exception ParseException Signals that the request line cannot be formatted.
   */
  public String formatRequestLine() throws ParseException {
    String m; // method
    String v; // version

    // get the method string
    m = getMethodString(method);
    if (m == null) {
      throw new ParseException(INVALID_METHOD);
    }
   
    // get the version string
    v = getVersionString(version);
    if (v == null) {
      throw new ParseException(INVALID_VERSION);
    }

    // calculate how large the buffer needs to be and create it
    int          len = m.length() + uri.length() + v.length() + 2;
    StringBuffer buf = new StringBuffer(len);

    // format the request line
    buf.append(m);
    buf.append(' ');
    buf.append(uri);
    if (HTTP09 != version) {
      buf.append(' ');
      buf.append(v);
    }

    return buf.toString();
  }

  /**
   * Is exception indicative of a bad method?
   *
   * @param x Exception to check.
   * @return true if bad method, false otherwise.
   */
  public static boolean isBadMethod(Exception x) {
    return INVALID_METHOD.equals(x.getMessage());
  }

  /**
   * Is exception indicative of a bad request line?
   *
   * @param x Exception to check.
   * @return true if bad method, false otherwise.
   */
  public static boolean isBadRequestLine(Exception x) {
    return INVALID_REQUEST_LINE.equals(x.getMessage());
  }

  /**
   * Parse a request line into a method token, a URI, and a protocol
   * identifier.Assumes HTTP/0.9, if no protocol identifier is present. 
   *
   * @param      line            Request line to be parsed.
   * @exception  ParseException  Signals that the request line cannot be parsed.
   */
  public void parseRequestLine(String line) throws ParseException {
    String m; // method
    String u; // uri
    String v; // version

    StringTokenizer tok = new StringTokenizer(line, " \t");
   
    // read the method
    if (!tok.hasMoreElements()) {
      throw new ParseException(INVALID_REQUEST_LINE);
    }
    m = tok.nextToken().trim();

    // read the URI
    if (!tok.hasMoreElements()) {
      throw new ParseException(INVALID_REQUEST_LINE);
    }
    u = tok.nextToken().trim();
    
    // read the version
    if (!tok.hasMoreElements()) {
      v = HttpConstants.HTTP09;
    } else {
      v = tok.nextToken().trim();
    }

    // convert the method string into a number
    method = getMethodNumber(m);
    if (method == -1) {
      throw new ParseException(INVALID_METHOD);
    }
   
    // convert the version string into a number
    version = getVersionNumber(v);
    if (version == -1) {
      throw new ParseException(INVALID_VERSION);
    }

    uri = u;
  }


  /**
   * Convert HTTP method number to HTTP method string.
   *
   * @param  m HTTP method number.
   * @return   <code>null</code> if version number is not valid.
   */
  public static String getMethodString(int m) {
    switch (m) {
    case OPTIONS:
      return HttpConstants.OPTIONS;
    case GET:
      return HttpConstants.GET;
    case HEAD:
      return HttpConstants.HEAD;
    case POST:
      return HttpConstants.POST;
    case PUT:
      return HttpConstants.PUT;
    case DELETE:
      return HttpConstants.DELETE;
    case TRACE:
      return HttpConstants.TRACE;
    case CONNECT:
      return HttpConstants.CONNECT;
    default:
      return null;
    }
  }

  /**
   * Convert HTTP method string to HTTP method number.
   *
   * @param  m HTTP method string.
   * @return   <code>-1</code> if method string is not valud.
   */
  public static int getMethodNumber(String m) {
    if (m.equals(HttpConstants.OPTIONS)) {
      return OPTIONS;
    } else if (m.equals(HttpConstants.GET)) {
      return GET;
    } else if (m.equals(HttpConstants.HEAD)) {
      return HEAD;
    } else if (m.equals(HttpConstants.POST)) {
      return POST;
    } else if (m.equals(HttpConstants.PUT)) {
      return PUT;
    } else if (m.equals(HttpConstants.DELETE)) {
      return DELETE;
    } else if (m.equals(HttpConstants.TRACE)) {
      return TRACE;
    } else if (m.equals(HttpConstants.CONNECT)) {
      return CONNECT;
    } else {
      return -1;
    }
  }
}
