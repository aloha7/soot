/*
 * Copyright (c) 1999, 2000, University of Washington, Department of
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

import java.util.Hashtable;

/**
 * <p>HttpConstants defines basic HTTP names and all HTTP/1.1 protocol
 * entity names. Specifically, it defines all HTTP/1.1 method names,
 * status codes, the mapping from status codes to reason phrases, and
 * message header field names. Message header field names are in
 * canonical form, as expected by an InternetMessageHeader.</p>
 *
 * <p>Status codes are three-digit numbers (represented as strings in
 * this class) where the first, highest-significant number determines
 * the general group a status code belongs to:
 * <ul>
 * <li> 1xx: Informational - Request received, continuing
 *                     process. </li>
 * <li> 2xx: Success - The action was successfully received,
 *                     understood, and accepted. </li>
 * <li> 3xx: Redirection - Further action must be taken in order
 *                     to complete the request. </li>
 * <li> 4xx: Client Error - The request contains bad syntax or
 *                     cannot be fulfilled. </li>
 * <li> 5xx: Server Error - The server failed to fulfill an
 *                     apparently valid request. </li>
 * </ul></p>
 *
 *
 * @author  Robert Grimm
 * @version $Revision: 1.3 $
 */

public class HttpConstants {
  /** Make constructor invisible. */
  private HttpConstants() {
    // Nothing to construct.
  }

  /** "<code>http</code>". */
  public static final String HTTP                    = "http";
  
  /** "<code>http:</code>". */
  public static final String HTTP_COLON              = "http:";

  /** "<code>http://</code>". */
  public static final String HTTP_COLON_SLASH_SLASH  = "http://";

  /** "<code>HTTP/0.9</code>". */
  public static final String HTTP09                  = "HTTP/0.9";

  /** "<code>HTTP/1.0</code>". */
  public static final String HTTP10                  = "HTTP/1.0";

  /** "<code>HTTP/1.1</code>". */
  public static final String HTTP11                  = "HTTP/1.1";

  /** The "<code>text/html</code>" MIME type. */
  public static final String TEXT_HTML               = "text/html";

  /** The "<code>unknown/unknown</code>" MIME type. */
  public static final String UNKNOWN_UNKNOWN         = "unknown/unknown";

  /** The "<code>OPTIONS</code>" method name. */
  public static final String OPTIONS                 = "OPTIONS";

  /** The "<code>GET</code>" method name. */
  public static final String GET                     = "GET";
  
  /** The "<code>HEAD</code>" method name. */
  public static final String HEAD                    = "HEAD";
  
  /** The "<code>POST</code>" method name. */
  public static final String POST                    = "POST";

  /** The "<code>PUT</code>" method name. */
  public static final String PUT                     = "PUT";

  /** The "<code>DELETE</code>" method name. */
  public static final String DELETE                  = "DELETE";

  /** The "<code>TRACE</code>" method name. */
  public static final String TRACE                   = "TRACE";

  /** The "<code>CONNECT</code>" method name. */
  public static final String CONNECT                 = "CONNECT";


  /** 100 - Continue. */
  public static final int CONTINUE                = 100;
  /** 101 - Switching Protocols. */
  public static final int SWITCHING_PROTOCOLS     = 101;

  /** 200 - OK. */
  public static final int OK                      = 200;
  /** 201 - Created. */
  public static final int CREATED                 = 201;
  /** 202 - Accepted. */
  public static final int ACCEPTED                = 202;
  /** 203 - Non-Authoritative Information. */
  public static final int NON_AUTHORITATIVE       = 203;
  /** 204 - No Content. */
  public static final int NO_CONTENT              = 204;
  /** 205 - Reset Content. */
  public static final int RESET_CONTENT           = 205;
  /** 206 - Partial Content. */
  public static final int PARTIAL_CONTENT         = 206;

  /** 300 - Multiple Choices. */
  public static final int MULTIPLE_CHOICES        = 300;
  /** 301 - Moved Permanently. */
  public static final int MOVED_PERMANENTLY       = 301;
  /** 302 - Found. */
  public static final int FOUND                   = 302;
  /** 303 - See Other. */
  public static final int SEE_OTHER               = 303;
  /** 304 - Not Modified. */
  public static final int NOT_MODIFIED            = 304;
  /** 305 - Use Proxy. */
  public static final int USE_PROXY               = 305;
  /** 307 - Temporary Redirect. */
  public static final int TEMPORARY_REDIRECT      = 307;

  /** 400 - Bad Request. */
  public static final int BAD_REQUEST             = 400;
  /** 401 - Unauthorized. */
  public static final int UNAUTHORIZED            = 401;
  /** 402 - Payment Required. */
  public static final int PAYMENT_REQUIRED        = 402;
  /** 403 - Forbidden. */
  public static final int FORBIDDEN               = 403;
  /** 404 - Not Found. */
  public static final int NOT_FOUND               = 404;
  /** 405 - Method Not Allowed. */
  public static final int METHOD_NOT_ALLOWED      = 405;
  /** 406 - Not Acceptable. */
  public static final int NOT_ACCEPTABLE          = 406;
  /** 407 - Proxy Authentication Required. */
  public static final int PROXY_AUTHENTICATION    = 407;
  /** 408 - Request Time-out. */
  public static final int REQUEST_TIMEOUT         = 408;
  /** 409 - Conflict. */
  public static final int CONFLICT                = 409;
  /** 410 - Gone. */
  public static final int GONE                    = 410;
  /** 411 - Length Required. */
  public static final int LENGTH_REQUIRED         = 411;
  /** 412 - Precondition Failed. */
  public static final int PRECONDITION_FAILED     = 412;
  /** 413 - Request Entity Too Large. */
  public static final int ENTITY_TOO_LARGE        = 413;
  /** 414 - Request URI Too Large. */
  public static final int URI_TOO_LARGE           = 414;
  /** 415 - Unsupported Media Type. */
  public static final int UNSUPPORTED_MEDIA_TYPE  = 415;
  /** 416 - Requested Range Not Satisfiable. */
  public static final int REQUEST_RANGE           = 416;
  /** 417 - Expectation Failed. */
  public static final int EXPECTATION_FAILED      = 417;

  /** 500 - Internal Server Error. */
  public static final int INTERNAL_SERVER_ERROR   = 500;
  /** 501 - Not Implemented. */
  public static final int NOT_IMPLEMENTED         = 501;
  /** 502 - Bad Gateway. */
  public static final int BAD_GATEWAY             = 502;
  /** 503 - Service Unavailable. */
  public static final int SERVICE_UNAVAILABLE     = 503;
  /** 504 - Gateway Time-out. */
  public static final int GATEWAY_TIMEOUT         = 504;
  /** 505 - HTTP Version Not Supported. */
  public static final int HTTP_VERSION            = 505;


  /** The "<code>Accept</code>" header field name. */
  public static final String ACCEPT                  = "Accept";

  /** The "<code>Accept-Charset</code>" header field name. */
  public static final String ACCEPT_CHARSET          = "Accept-Charset";

  /** The "<code>Accept-Encoding</code>" header field name. */
  public static final String ACCEPT_ENCODING         = "Accept-Encoding";

  /** The "<code>Accept-Language</code>" header field name. */
  public static final String ACCEPT_LANGUAGE         = "Accept-Language";
 
  /** The "<code>Accept-Ranges</code>" header field name. */
  public static final String ACCEPT_RANGES           = "Accept-Ranges";

  /** The "<code>Age</code>" header field name. */
  public static final String AGE                     = "Age";

  /** The "<code>Allow</code>" header field name. */
  public static final String ALLOW                   = "Allow";

  /** The "<code>Authorization</code>" header field name. */
  public static final String AUTHORIZATION           = "Authorization";

  /** The "<code>Cache-Control</code>" header field name. */
  public static final String CACHE_CONTROL           = "Cache-Control";

  /** The "<code>Connection</code>" header field name. */
  public static final String CONNECTION              = "Connection";

  /** The "<code>close</code>" value for Connection. */
  public static final String CLOSE                   = "close";

  /** The "<code>keep-alive</code>" value for Connection. */
  public static final String KEEP_ALIVE              = "keep-alive";

  /** The "<code>Content-Encoding</code>" header field name. */
  public static final String CONTENT_ENCODING        = "Content-Encoding";

  /** The "<code>Content-Language</code>" header field name. */
  public static final String CONTENT_LANGUAGE        = "Content-Language";

  /** The "<code>Content-Length</code>" header field name. */
  public static final String CONTENT_LENGTH          = "Content-Length";

  /** The "<code>Content-Location</code>" header field name. */
  public static final String CONTENT_LOCATION        = "Content-Location";

  /** The "<code>Content-MD5</code>" header field name. */
  public static final String CONTENT_MD5             = "Content-MD5";

  /** The "<code>Content-Range</code>" header field name. */
  public static final String CONTENT_RANGE           = "Content-Range";

  /** The "<code>Content-Type</code>" header field name. */
  public static final String CONTENT_TYPE            = "Content-Type";

  /** The "<code>Date</code>" header field name. */
  public static final String DATE                    = "Date";

  /** The "<code>ETag</code>" header field name. */
  public static final String ETAG                    = "ETag";

  /** The "<code>Expect</code>" header field name. */
  public static final String EXPECT                  = "Expect";

  /** The "<code>Expires</code>" header field name. */
  public static final String EXPIRES                 = "Expires";

  /** The "<code>From</code>" header field name. */
  public static final String FROM                    = "From";

  /** The "<code>Host</code>" header field name. */
  public static final String HOST                    = "Host";

  /** The "<code>If-Match</code>" header field name. */
  public static final String IF_MATCH                = "If-Match";

  /** The "<code>If-Modified-Since</code>" header field name. */
  public static final String IF_MODIFIED_SINCE       = "If-Modified-Since";

  /** The "<code>If-None-Match</code>" header field name. */
  public static final String IF_NONE_MATCH           = "If-None-Match";

  /** The "<code>If-Range</code>" header field name. */
  public static final String IF_RANGE                = "If-Range";

  /** The "<code>If-Unmodified-Since</code>" header field name. */
  public static final String IF_UNMODIFIED_SINCE     = "If-Unmodified-Since";

  /** The "<code>Last-Modified</code>" header field name. */
  public static final String LAST_MODIFIED           = "Last-Modified";

  /** The "<code>Location</code>" header field name. */
  public static final String LOCATION                = "Location";

  /** The "<code>Max-Forwards</code>" header field name. */
  public static final String MAX_FORWARDS            = "Max-Forwards";

  /** The "<code>Pragma</code>" header field name. */
  public static final String PRAGMA                  = "Pragma";

  /** The "<code>Proxy-Authenticate</code>" header field name. */
  public static final String PROXY_AUTHENTICATE      = "Proxy-Authenticate";

  /** The "<code>Proxy-Authorization</code>" header field name. */
  public static final String PROXY_AUTHORIZATION     = "Proxy-Authorization";

  /** The "<code>Range</code>" header field name. */
  public static final String RANGE                   = "Range";

  /** The "<code>Referer</code>" header field name. */
  public static final String REFERER                 = "Referer";

  /** The "<code>Retry-After</code>" header field name. */
  public static final String RETRY_AFTER             = "Retry-After";

  /** The "<code>Server</code>" header field name. */
  public static final String SERVER                  = "Server";

  /** The "<code>one.net.http</code>" value for Server. */
  public static final String ONEWORLD                = "one.world/0.1";

  /** The "<code>TE</code>" header field name. */
  public static final String TE                      = "TE";

  /** The "<code>Trailer</code>" header field name. */
  public static final String TRAILER                 = "Trailer";

  /** The "<code>Transfer-Encoding</code>" header field name. */
  public static final String TRANSFER_ENCODING       = "Transfer-Encoding";

  /** The "<code>Upgrade</code>" header field name. */
  public static final String UPGRADE                 = "Upgrade";

  /** The "<code>User-Agent</code>" header field name. */
  public static final String USER_AGENT              = "User-Agent";

  /** The "<code>Vary</code>" header field name. */
  public static final String VARY                    = "Vary";

  /** The "<code>Via</code>" header field name. */
  public static final String VIA                     = "Via";

  /** The "<code>Warning</code>" header field name. */
  public static final String WARNING                 = "Warning";

  /** The "<code>WWW-Authenticate</code>" header field name. */
  public static final String WWW_AUTHENTICATE        = "WWW-Authenticate";


  // The mapping from status codes to reason phrases.
  private static Hashtable map = new Hashtable(40);
  
  static {
    fillMap();
  }

  /**
   * Add a mapping from a status code to a reason phrase.
   *
   * @param  status  The HTTP status code.
   * @param  phrase  The corresponding reason phrase.
   */
  protected static void setReason(int status, String phrase) {
    map.put(new Integer(status), phrase);
  }

  /**
   * Initialize mapping from status codes to reason phrases.
   */
  protected static void fillMap() {
    /* 1xx */
    setReason(CONTINUE,                "Continue");
    setReason(SWITCHING_PROTOCOLS,     "Switching Protocols");

    /* 2xx */
    setReason(OK,                      "OK");
    setReason(CREATED,                 "Created");
    setReason(ACCEPTED,                "Accepted");
    setReason(NON_AUTHORITATIVE,       "Non-Authoritative Information");
    setReason(NO_CONTENT,              "No Content");
    setReason(RESET_CONTENT,           "Reset Content");
    setReason(PARTIAL_CONTENT,         "Partial Content");

    /* 3xx */
    setReason(MULTIPLE_CHOICES,        "Multiple Choices");
    setReason(MOVED_PERMANENTLY,       "Moved Permanently");
    setReason(FOUND,                   "Found");
    setReason(SEE_OTHER,               "See Other");
    setReason(NOT_MODIFIED,            "Not Modified");
    setReason(USE_PROXY,               "Use Proxy");
    setReason(TEMPORARY_REDIRECT,      "Temporary Redirect");

    /* 4xx */
    setReason(BAD_REQUEST,             "Bad Request");
    setReason(UNAUTHORIZED,            "Unauthorized");
    setReason(PAYMENT_REQUIRED,        "Payment Required");
    setReason(FORBIDDEN,               "Forbidden");
    setReason(NOT_FOUND,               "Not Found");
    setReason(METHOD_NOT_ALLOWED,      "Method Not Allowed");
    setReason(NOT_ACCEPTABLE,          "Not Acceptable");
    setReason(PROXY_AUTHENTICATION,    "Proxy Authentication Required");
    setReason(REQUEST_TIMEOUT,         "Request Time-out");
    setReason(CONFLICT,                "Conflict");
    setReason(GONE,                    "Gone");
    setReason(LENGTH_REQUIRED,         "Length Required");
    setReason(PRECONDITION_FAILED,     "Precondition Failed");
    setReason(ENTITY_TOO_LARGE,        "Request Entity Too Large");
    setReason(URI_TOO_LARGE,           "Request URI Too Large");
    setReason(UNSUPPORTED_MEDIA_TYPE,  "Unsupported Media Type");
    setReason(REQUEST_RANGE,           "Request Range No Satisfiable");
    setReason(EXPECTATION_FAILED,      "Expectation Failed");

    /* 5xx */
    setReason(INTERNAL_SERVER_ERROR,   "Internal Server Error");
    setReason(NOT_IMPLEMENTED,         "Not Implemented");
    setReason(BAD_GATEWAY,             "Bad Gateway");
    setReason(SERVICE_UNAVAILABLE,     "Service Unavailable");
    setReason(GATEWAY_TIMEOUT,         "Gateway Time-out");
    setReason(HTTP_VERSION,            "HTTP Version Not Supported");
  }

  /**
   * Map a status code into a reason phrase.
   *
   * @param   status  The status code.
   * @return          The corresponding reason phrase, or null if the
   *                  status code is not a valid HTTP/1.1 status code.
   */
  public static String getReason(int status) {
    return (String) map.get(new Integer(status));
  }
}
