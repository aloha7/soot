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
import java.net.InetAddress;
import java.net.UnknownHostException;
import one.world.util.SystemUtilities;

/**
 * Various utility functions to validate HTTP requests.
 *
 * @author  Daniel Cheah
 * @version $Revision: 1.2 $
 */
public final class RequestValidator {
  /** Constants */
  private static final char   SEPARATOR  = '/';
  private static final String INDEX_FILE = "index.html";

  /** InetAddress object for our localhost */
  private InetAddress  localhost;
  /** Hostname of our server */
  private String       hostname;
  /** Document root of our server */
  private String       docRoot;
  /** Port server is running on */
  private int          port;

  /**
   * Constructor.
   *
   * @param docRoot Document root.
   * @param port    Port we listen to requests on.
   * @throws UnknownHostException
   */
  public RequestValidator(String docRoot, int port) throws UnknownHostException {
    init();
    setDocumentRoot(docRoot);
    setPort(port);
  }

  /** Initialize class variables. */
  private void init() throws UnknownHostException {
    localhost = InetAddress.getLocalHost();
    hostname  = localhost.getHostName();
  }

  /**
   * Get the document root.
   *
   * @return The document root.
   */
  public String getDocumentRoot() {
    return docRoot;
  }

  /**
   * Set the document root.
   *
   * @param The document root.
   */
  public void setDocumentRoot(String docRoot) {
    this.docRoot = docRoot;
  }

  /**
   * Get the port number.
   *
   * @return The port number.
   */
  public int getPort() {
    return port;
  }

  /**
   * Set the port number.
   *
   * @param port The port number.
   */
  public void setPort(int port) {
    this.port = port;
  }

  /**
   * Verify that URL refers to a local resource.
   *
   * @param url  The URL
   * @param port Port request came in on
   * @return true if verified, false otherwise
   */
  public boolean verify(URL url) {
    return HttpURI.verify(url, hostname, port);
  }

  /**
   * Creates a validated path from the URL.
   * 
   * @param url The URL to create validated path from.
   * @return Buffer containing the validated path.
   */
  public StringBuffer validate(URL url) {
    // Generate file name
    StringBuffer name = new StringBuffer(url.getFile());
    
    if (SEPARATOR == name.charAt(name.length() - 1)) {
      name.append(INDEX_FILE);
    }

    if (SEPARATOR != name.charAt(0)) {
      name.insert(0, SEPARATOR);
    }

    name.insert(0, docRoot);

    // Return the validated name
    return name;
  }

  /** 
   * Does the path contain 2 consecutive dots?
   *
   * @param path Path to check.
   * @return True if path contains 2 dots, false otherwise.
   */
  public static boolean contains2Dots(StringBuffer path) {
    for (int i = 0; i < path.length() - 1; ++i) {
      if (path.charAt(i) == '.' && path.charAt(i + 1) == '.') {
        return true;
      }
    }

    return false;
  }

  /**
   * Get the seperator index that seperates path from the tuple name.
   *
   * @param path Buffer containing validated path.
   * @return Index of the seperator.
   */
  public static int getSeparatorIndex(String path) {
    for (int i = path.length() - 1; i > 0; --i) {
      if (SEPARATOR == path.charAt(i)) {
        return i;
      }
    }
    
    return 0;
  }
}
