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

import one.net.NetInputStream;
import one.world.util.SystemUtilities;
import one.world.data.BinaryData;
import java.io.Serializable;

/**
 * Implementation of the HTTP post method.
 *
 * @author  Daniel Cheah
 * @version $Revision: 1.2 $
 */
public class PostMethod implements HttpMethod, Serializable {
  /**
   * Constructor.
   */
  public PostMethod() {
  }

  /**
   * Read the body of a POST request.
   *
   * @param netIn  The input stream for reading in the body.
   * @param req    The HTTP request that was read.
   * @exception Exception if an error occurs
   */
  public void readBody(NetInputStream netIn, HttpRequest req) throws Exception {
    SystemUtilities.debug("reading post body");

    // HTTP/0.9 does not support the post method
    if (HttpEvent.HTTP09 == req.version) {
      // FIXME: Is this the correct status? Need to read up.
      req.status = HttpConstants.METHOD_NOT_ALLOWED;
      return;
    }

    // Get the content type and length.
    String t = (String)req.header.get(HttpConstants.CONTENT_TYPE);
    String l = (String)req.header.get(HttpConstants.CONTENT_LENGTH);

    // FIXME: What to do with the rest of the body.
    // Need the length.
    if (null == l) {
      req.status = HttpConstants.LENGTH_REQUIRED;
      return;
    }

    // Parse the length value.
    int length = 0;
    try {
      length = Integer.parseInt(l);
    } catch (NumberFormatException x) {
      req.status = HttpConstants.BAD_REQUEST;
      return;
    }

    if (length > 4096) {
      req.status = HttpConstants.ENTITY_TOO_LARGE;
      return;
    }

    // Store the body in as binary data.
    BinaryData bd = new BinaryData();
    bd.data = new byte[length];
    netIn.read(bd.data, 0, bd.data.length);
    netIn.readLine();

    req.body = bd;
  }
}
