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

import one.world.core.DynamicTuple;
import one.world.core.Tuple;
import one.world.core.Event;
import one.world.core.EventHandler;
import one.world.core.InvalidTupleException;
import one.world.core.TupleException;

/**
 * Implementation of a HTTP event. It contains
 * common fields that pertain to HTTP requests 
 * and responses.
 *
 * @author   Daniel Cheah
 * @version  $Revision: 1.4 $
 */
public class HttpEvent extends Event {
  /**
   * Internal representation of <code>HTTP09</code> within events.
   * @see HttpConstants#HTTP09
   */
  public static final int HTTP09  = 9;
  
  /**
   * Internal representation of <code>HTTP10</code> within events.
   * @see HttpConstants#HTTP10
   */
  public static final int HTTP10  = 10;

  /**
   * Internal representation of <code>HTTP11</code> within events.
   * @see HttpConstants#HTTP11
   */
  public static final int HTTP11  = 11;

  /**
   * Internal representation of <code>OPTIONS</code> within events.
   * @see HttpConstants#OPTIONS
   *
   */
  public static final int OPTIONS = 1;
  /**
   * Internal representation of <code>GET</code> within events.
   * @see HttpConstants#GET
   *
   */
  public static final int GET     = 2;


  /**
   * Internal representation of <code>HEAD</code> within events.
   * @see HttpConstants#HEAD
   *
   */
  public static final int HEAD    = 3;


  /**
   * Internal representation of <code>POST</code> within events.
   * @see HttpConstants#POST
   *
   */
  public static final int POST    = 4;


  /**
   * Internal representation of <code>PUT</code> within events.
   * @see HttpConstants#PUT
   *
   */
  public static final int PUT     = 5;


  /**
   * Internal representation of <code>DELETE</code> within events.
   * @see HttpConstants#DELETE
   *
   */
  public static final int DELETE  = 6;



  /**
   * Internal representation of <code>TRACE</code> within events.
   * @see HttpConstants#TRACE
   *
   */
  public static final int TRACE   = 7;


  /**
   * Internal representation of <code>CONNECT</code> within events.
   * @see HttpConstants#CONNECT
   *
   */
  public static final int CONNECT = 8;


  /**
   * Integer object representation of <code>OPTIONS</code>
   * @see HttpEvent#OPTIONS
   *
   */
  public static final Integer I_OPTIONS = new Integer(OPTIONS);

  /**
   * Integer object representation of <code>GET</code>
   * @see HttpEvent#GET
   *
   */
  public static final Integer I_GET     = new Integer(GET);

  /**
   * Integer object representation of <code>HEAD</code>
   * @see HttpEvent#HEAD
   *
   */
  public static final Integer I_HEAD    = new Integer(HEAD);

  /**
   * Integer object representation of <code>POST</code>
   * @see HttpEvent#POST
   *
   */
  public static final Integer I_POST    = new Integer(POST);

  /**
   * Integer object representation of <code>PUT</code>
   * @see HttpEvent#PUT
   *
   */
  public static final Integer I_PUT     = new Integer(PUT);

  /**
   * Integer object representation of <code>DELETE</code>
   * @see HttpEvent#DELETE
   *
   */
  public static final Integer I_DELETE  = new Integer(DELETE);

  /**
   * Integer object representation of <code>TRACE</code>
   * @see HttpEvent#TRACE
   *
   */
  public static final Integer I_TRACE   = new Integer(TRACE);

  /**
   * Integer object representation of <code>CONNECT</code>
   * @see HttpEvent#CONNECT
   *
   */
  public static final Integer I_CONNECT = new Integer(CONNECT);

  /** <code>INVALID_VERSION</code> string. */
  public static final String INVALID_VERSION      = "HTTP Version Not Valid";

  /** <code>INVALID_STATUS</code> string. */
  public static final String INVALID_STATUS       = "Status Code Not Valid";

  /** <code>INVALID_METHOD</code> string. */
  public static final String INVALID_METHOD       = "HTTP Method Not Valid";

  /** Request line parser errror message. */
  public static final String INVALID_REQUEST_LINE = "Invalid Request Line";

  /** Response line parser errror message. */
  public static final String INVALID_STATUS_LINE  = "Invalid Status Line";


  /** HTTP Version */
  public int          version;
  /** Status Code */
  public int          status;
  /** Message Header */
  public DynamicTuple header;
  /** Message Body */
  public Tuple        body;

  /**
   * Creates a new instance of <code>HttpEvent</code>.
   */
  public HttpEvent() {
    super();
    this.version = 0;
    this.status  = 0;
    this.header  = null;
    this.body    = null;
  }

  /**
   * Creates a new instance of <code>HttpEvent</code>.
   *
   * @param source  Source for this event.
   * @param closure Closure for this event.
   */
  public HttpEvent(EventHandler source, Object closure) {
    super(source, closure);
    this.version = 0;
    this.status  = 0;
    this.header  = null;
    this.body    = null;
  }

  /**
   * Validate this event.
   *
   * @exception TupleException Signals that the data is invalid.
   */
  public void validate() throws TupleException {
    super.validate();

    if (getVersionString(version) == null) {
      throw new InvalidTupleException(INVALID_VERSION);
    }

    if (HttpConstants.getReason(status) == null) {
      throw new InvalidTupleException(INVALID_STATUS);
    }
  }

  /**
   * Describe <code>isBadVersion</code> method here.
   *
   * @param x an <code>Exception</code> value
   * @return a <code>boolean</code> value
   */
  public static boolean isBadVersion(Exception x) {
    return INVALID_VERSION.equals(x.getMessage());
  }

  /**
   * Converts HTTP version number to HTTP version string.
   *
   * @param  v HTTP version number.
   * @return   <code>null</code> if version number is not valid.
   */
  public static String getVersionString(int v) {
    switch(v) {
    case HTTP09:
      return HttpConstants.HTTP09;
    case HTTP10:
      return HttpConstants.HTTP10;
    case HTTP11:
      return HttpConstants.HTTP11;
    default:
      return null;
    }
  }

  /**
   * Converts HTTP version string to HTTP version number.
   *
   * @param  v HTTP version string.
   * @return   <code>-1</code> if version string is not valid.
   */
  public static int getVersionNumber(String v) {
    if (v.equals(HttpConstants.HTTP09)) {
      return HTTP09;
    } else if (v.equals(HttpConstants.HTTP10)) {
      return HTTP10;
    } else if (v.equals(HttpConstants.HTTP11)) {
      return HTTP11;
    } else {
      return -1;
    }
  }
}
