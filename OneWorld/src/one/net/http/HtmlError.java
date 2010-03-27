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

import one.world.data.BinaryData;
import one.util.Bug;
import java.io.Serializable;

/**
 * Encapsulates the generation of HTML error pages. 
 *
 * @author  Daniel Cheah
 * @version $Revision: 1.3 $
 */
public class HtmlError implements Serializable {
  /**
   * Constructor.
   */
  public HtmlError() {
  }

  /**
   * Send an HTML error message. Creates an error message in the
   * current output buffer, formatted in HTML and based on the
   * specified status code, explanatory message and exception. 
   *
   * @param e  Valid <code>HttpEvent</code> object.
   * @return HTML page as a <code>BinaryData</code> object.
   */
  public BinaryData createPage(HttpEvent e) {
    return createPage(e.status);
  }

  /**
   * Creates a properly formatted HTML error page.
   *
   * @param status HTTP status code
   * @return HTML page as a BinaryData object.
   */
  protected BinaryData createPage(int status) { 
    // just a little sanity check
    if (HttpConstants.OK == status) {
      new Bug("this should not get used");
    } 
    
    StringBuffer buffer = new StringBuffer();
    String       msg    = HttpConstants.getReason(status);
    String       reason = HttpConstants.getReason(status);
    
    // create the message body
    buffer.append("<html>\r\n<head><title>");
    buffer.append(status);
    buffer.append("</title></head>\r\n<body>\r\n<h3>");
    buffer.append(status);
    if (reason.length() != 0) {
      buffer.append(" ");
      buffer.append(reason);
    }
    buffer.append("</h3>\r\n");
    buffer.append(msg);
    buffer.append(".\r\n</body>\r\n</html>\r\n");
    
    // create the message body as a tuple
    return new BinaryData("errorpage.html", 
                          HttpConstants.TEXT_HTML, 
                          buffer.toString().getBytes());
  }
}
