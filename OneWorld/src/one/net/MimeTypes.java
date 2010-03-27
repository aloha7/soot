/*
 * Copyright (c) 2001, University of Washington, Department of
 * Computer Science and Engineering.
 *    All rights reserved.
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

package one.net;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;

import java.security.AccessController;
import java.security.PrivilegedExceptionAction;

import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Implementation of a mapping between MIME types and file name
 * extensions.
 *
 * <p>The mapping used by this class is initialized through a
 * configuration file.  By default, this file is named
 * "<code>mime.config</code>" and must be located in the current
 * working directory. Though, the "<code>mime.config.name</code>"
 * system property can be used to specify an alternative name and
 * directory.  The configuration file contains a line for each MIME
 * type, which first specifies the MIME type followed by a list of one
 * or more file name extensions. All file name extensions map to the
 * specified MIME type; though, the MIME type only maps to the first
 * file name extension. Empty lines or lines starting with a hash mark
 * ('<code>#</code>') are treated as comments.</p>
 *
 * @version  $Revision: 1.3 $
 * @author   Robert Grimm
 */
public final class MimeTypes implements NetConstants {

  /** Hide the constructor. */
  private MimeTypes() {
    // Nothing to do.
  }

  /** The mapping from MIME types to file name extensions. */
  private static final Map mimeTypeToExtension;

  /** The mapping from file name extensions to MIME types. */
  private static final Map extensionToMimeType;

  /** Initialize the mappings. */
  static {
    // Creating mappings.
    mimeTypeToExtension = new HashMap();
    extensionToMimeType = new HashMap();

    // Get name for configuration file.
    String       tmp  = System.getProperty("mime.config.name");
    final String name = ((null == tmp)? "mime.config" : tmp);

    // Read in configuration file.
    BufferedReader in = null;

    try {
      in = new BufferedReader(new
        InputStreamReader((InputStream)AccessController.doPrivileged(new
          PrivilegedExceptionAction() {
            public Object run() throws FileNotFoundException {
              return new FileInputStream(name);
            }
          })));

      do {
        String line = in.readLine();

        // Are we done?
        if (null == line) {
          break;
        }

        line = line.trim();
        if ("".equals(line) || line.startsWith("#")) {
          // Skip empty lines and comments.
          continue;
        }

        StringTokenizer tok  = new StringTokenizer(line);
        boolean         ext  = false;
        String          type = null;
        
        while (tok.hasMoreTokens()) {
          String token = tok.nextToken();

          // Process MIME type.
          if (null == type) {
            if (mimeTypeToExtension.containsKey(token)) {
              throw new IllegalStateException("Duplicate MIME type (" + type +
                                              ")");
            } else {
              type = token;
              continue;
            }
          }

          // Process file name extension.
          if (extensionToMimeType.containsKey(token)) {
            throw new IllegalStateException("Duplicate file name extension (" +
                                            token + ")");
          }

          // Map MIME type to first file name extension.
          if (! ext) {
            mimeTypeToExtension.put(type, token);
            ext = true;
          } 
          extensionToMimeType.put(token, type);
        }

        // Have we seen at least one file name extension?
        if (! ext) {
          throw new IllegalStateException("Missing file name extensions for " +
                                          "MIME type (" + type + ")");
        }
      } while (true);

    } catch (Exception x) {
      throw new IllegalStateException("Unable to initialize MIME mappings (" +
                                      x + ")");
    } finally {
      try {
        in.close();
      } catch (Exception x) {
        // Ignore.
      }
    }
  }

  /**
   * Get the MIME type for the specified file name extension. This
   * method returns the appropriate MIME type for the specified file
   * name extension. If no explicit mapping is specified in the
   * configuration file for this class, this method returns {@link
   * #MIME_TYPE_DEFAULT}.
   *
   * @param   extension  The file name extension.
   * @return             The corresponding MIME type.
   */
  public static String getMimeType(String extension) {
    String type = (String)extensionToMimeType.get(extension.toLowerCase());

    return ((null == type)? MIME_TYPE_DEFAULT : type);
  }

  /**
   * Get the file name extension for the specified MIME type. This
   * method returns the appropriate file name extension for the
   * specified MIME type. If no explicit mapping is specified in the
   * configuration file for this class, this method returns {@link
   * #EXTENSION_DEFAULT}.
   *
   * @param   mimeType  The MIME type.
   * @return            The corresponding file name extension.
   */
  public static String getExtension(String mimeType) {
    String ext = (String)mimeTypeToExtension.get(mimeType.toLowerCase());

    return ((null == ext)? EXTENSION_DEFAULT : ext);
  }

}
