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

import java.net.URL;
import java.net.MalformedURLException;

/**
 * HttpURI provides functionality for constructing and analyzing URIs
 * for the HTTP protocol.
 *
 * @author   Robert Grimm
 * @version  $Revision: 1.3 $
 */
public class HttpURI {
  /** Make constructor invisible. */
  private HttpURI() {
    // Nothing to construct.
  }

  /**
   * Construct a URL object from the specified URI, using the HTTP
   * protocol as the default protocol. Converts the URI from
   * application/x-www-form-urlencoded format before parsing it
   * into a URL.
   *
   * @param      uri             The URI to be parsed.
   * @return                     The URI as a URL object.
   * @exception  ParseException  Signals a malformed URI.
   * @see                        MimeURLEncoded
   *
   */
  public static URL parseURI(String uri) throws ParseException {
    uri = MimeURLEncoded.decode(uri);

    try {
      URL context = new URL(HttpConstants.HTTP_COLON);
      return new URL(context, uri);
    } catch (MalformedURLException x) {
      throw new ParseException(x.getMessage());
    }
  }

  /**
   * Construct a URL object for the HTTP protocol from the specified
   * host.
   *
   * @param      host            The host name with an optional
   *                             port number separated by a colon
   *                             (':').
   * @return                     The host name as a URL object.
   * @exception  ParseException  Signals a malformed host name.
   */
  public static URL parseHost(String host) throws ParseException {
    try {
      return new URL(HttpConstants.HTTP_COLON_SLASH_SLASH + host);
    } catch (MalformedURLException x) {
      throw new ParseException(x.getMessage());
    }
  }

  /**
   * Verify a URL object. Tests whether the URL describes a
   * resource for the HTTP protocol, residing on the specified
   * host at the specified port number.
   *
   * @param   url   The URL to verify.
   * @param   host  The local host name.
   * @param   port  The port number, or -1 for the HTTP default
   *                port.
   * @return        True iff the URL object describes a resource
   *                for the HTTP protocol at the specified host
   *                and port.
   */
  public static boolean verify(URL url, String host, int port) {
    // The protocol must always be HTTP.
    if (! url.getProtocol().equalsIgnoreCase(HttpConstants.HTTP)) {
      return false;
    }
    
    // We only check the host of the URL if one is specified.
    String urlHost = url.getHost();
    
    if ((urlHost != null) && (urlHost.length() != 0)) {
      String specHost = host.toLowerCase();
      urlHost         = urlHost.toLowerCase();

      boolean specFull = (specHost.indexOf(".") != -1);
      boolean urlFull  = (urlHost.indexOf(".")  != -1);

      if ((specFull) && (! urlFull)) {
        if (! specHost.startsWith(urlHost)) {
          return false;
        }
      } else if ((! specFull) && (urlFull)) {
        if (! urlHost.startsWith(specHost)) {
          return false;
        }
      } else {
        if (! urlHost.equals(specHost)) {
          return false;
        }
      }
    }
    
    int urlPort = url.getPort();
    port    = (port    == -1) ? 80 : port;
    // FIXME: Default port is supposed to be 80, so this
    // test fails if we use a non-standard http port.
    // WORKAROUND: Use port here instead of 80.
    urlPort = (urlPort == -1) ? port : urlPort;
    return (port == urlPort);
  }
}
