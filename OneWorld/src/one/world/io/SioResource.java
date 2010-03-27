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

package one.world.io;

import java.net.URLDecoder;
import java.net.URLEncoder;

import java.util.ArrayList;
import java.util.StringTokenizer;

import one.util.Bug;
import one.util.Guid;

import one.world.Constants;

import one.world.binding.BindingRequest;
import one.world.binding.BindingResponse;

import one.world.core.Tuple;
import one.world.core.InvalidTupleException;
import one.world.core.TupleException;

/**
 * Implementation of a descriptor for a structured I/O
 * resource. Structured I/O resources can be specified in two ways,
 * (1) through a descriptor as implemented by this class and (2)
 * through a structured I/O URL. For convenience, this class
 * implements a constructor that takes a structured I/O URL and
 * generates the corresponding descriptor. It also implements a
 * <code>toString()</code> method that returns a structured I/O URL
 * corresponding to the structured I/O descriptor represented by an
 * instance of this class.
 *
 * <p>Structured I/O URLs describe either tuple storage or the
 * end-point of a communication channel. With the exception of
 * relative paths for tuple storage, all structured I/O URLs start
 * with the "<code>sio:</code>" protocol identifier, followed by
 * "<code>//</code>". Relative paths start with the name of the first
 * path segment.</p>
 *
 * <p><b>Communication Channels</b></p>
 *
 * <p>Structured I/O URLs for communication channels specify either
 * a client or server end-point for a communication channel over TCP
 * or an input-only, output-only, duplex, or multicast end-point for
 * a communication channel over UDP. Structured I/O URLs for
 * communication channels have the following form:
 * <pre>
 *      sio://host[:port][?type=&lt;client|server|input|output|
 *                               duplex|multicast&gt;
 *                        [&local=host[:port]]
 *                        [&closure=text]
 *                        [&duration=number]]
 * </pre></p>
 *
 * <p>The <code>type</code> attribute distinguishes between the six
 * types of communication channel end-points. It may be omitted for
 * client channels.</p>
 *
 * <p>The host (and optional port number) for a communication channel
 * specifies the local host (and port) for server and input channels
 * and the remote host (and port) for client, output, duplex, and
 * multicast channels. The local host (and port) for client and output
 * channels can optionally be specified using the <code>local</code>
 * attribute. The local host (and port) for duplex channels must be
 * specified using the <code>local</code> attribute.
 * If the local host is "localhost" for a server, input, or duplex
 * channel, the channel will accept connections to any local IP address.
 * To accept connections at the loopback interface, use "127.0.0.1" for the 
 * local host.</p>
 *
 * <p>The <code>closure</code> and <code>duration</code> attributes
 * specify the closure and initial lease duration for communication
 * channels accepted on a server end-point. The value of the
 * <code>closure</code> attribute is a string (even though the
 * descriptor accepts arbitrary objects) and is encoded in the
 * "<code>x-www-form-urlencoded</code>" MIME format. Both attributes
 * are optional.</p>
 * 
 * <p>Omitted ports for communication channels are treated as the
 * {@link Constants#PORT default port}.</p>
 *
 * <p><b>Storage</b></p>
 *
 * <p>Structured I/O URLs for storage specify environments. Structured
 * I/O URLs for storage can either be absolute, ID relative, or
 * relative. Absolute and ID relative URLs have the following
 * structure:
 * <pre>
 *      sio://[host[:port]]/[path][?type=storage]
 * </pre>
 * Relative URLs have the following structure:
 * <pre>
 *      path
 * </pre></p>
 *
 * <p>Paths are composed of one or more path segments, where each path
 * segment is the name of an environment, the ID of an environment,
 * "<code>.</code>" for the current environment, or "<code>..</code>"
 * for the parent environment, followed by a "<code>/</code>". When
 * parsing structured I/O URLs for storage, path segments that
 * represent an ID are treated as an ID while all other names (besides
 * "<code>.</code>" and "<code>..</code>") are treated as environment
 * names.
 *
 * <p>Paths can be normalized by removing embedded "<code>.</code>"
 * path segments, collapsing "<code>name/..</code>" compound segments,
 * and removing all segments leading to an ID. The normalized path for
 * an absolute URL must not contain "<code>.</code>",
 * "<code>..</code>", or an ID. The normalized path for an ID relative
 * URL must start with an ID, followed by zero or more
 * "<code>..</code>" path segments, followed by zero or more
 * environment names. The normalized path for a relative URL either is
 * "<code>.</code>" or consists of zero or more "<code>..</code>" path
 * segments followed by zero or more environment names.</p>
 *
 * <p>The <code>type</code> attribute can generally be omitted, though
 * it must be present for structured I/O URLs of the form
 * <code>sio://host[:port]</code>, because such URLs are otherwise
 * interpreted as URLs describing client end-points for communication
 * channels.</p>
 *
 * <p><b>Binding Requests and Responses</b></p>
 *
 * <p>When using a structured I/O resource descriptor in a {@link
 * BindingRequest binding request}, the corresponding {@link
 * BindingResponse binding response} returns an event handler for the
 * structured I/O resource. The types of structured I/O requests
 * accepted by the returned event handler depend on the type of
 * structured I/O resource. They are as following:
 *
 * <ul>
 * <li>A client communication channel accepts {@link
 * SimpleInputRequest simple input requests} and {@link
 * SimpleOutputRequest simple output requests}.</li>
 *
 * <li>A server communication channel accepts no structured I/O
 * requests.  Rather, additional binding responses are generated when
 * the underlying TCP server socket accepts a connection and the
 * server is bound to a client. The event handler returned through
 * such an additional binding response accepts simple input and output
 * requests.</li>
 *
 * <li>An input communication channel accepts only simple input
 * requests.</li>
 *
 * <li>An output communication channel accepts only simple output
 * requests.</li>
 *
 * <li>Duplex and multicast communication channels accept both simple
 * input and output requests.</li>
 *
 * <li>Storage not only accepts simple input and output requests, but
 * also {@link InputRequest input requests}, {@link OutputRequest
 * output requests}, and {@link DeleteRequest delete requests}.</li>
 * </ul>
 * 
 * @version  $Revision: 1.13 $
 * @author   Robert Grimm
 */
public class SioResource extends Tuple {

  /** The serial version ID for this class. */
  static final long serialVersionUID = 1991999172827981528L;

  /**
   * The type code for tuple storage as provided by an environment.  A
   * descriptor of this type must specify at least an environment by
   * its ID, an absolute path, a relative path, or a path relative to
   * an ID. It may also specify a remote host and port.
   */
  public static final int STORAGE      =    1;

  /**
   * The type code for a client end-point of a structured I/O
   * communication channel over TCP. A descriptor of this type must at
   * least specify the remote host and port, though it may also
   * specify the local host and port.
   */
  public static final int CLIENT       =    2;

  /**
   * The type code for a server end-point of a structured I/O
   * communication channel over TCP. A descriptor of this type must
   * specify the local host and port.
   */
  public static final int SERVER       =    3;

  /**
   * The type code for an input-only end-point of a structured I/O
   * communication channel over UDP. A descriptor of this type must
   * specify the local host and port.
   */
  public static final int INPUT        =    4;

  /**
   * The type code for an output-only end-point of a structured I/O
   * communication channel over UDP. A descriptor of this type must
   * specify at least the remote host and port, though it may also
   * specify the local host and port.
   */
  public static final int OUTPUT       =    5;

  /**
   * The type code for a duplex end-point of a structured I/O
   * communication channel over UDP. A descriptor of this type must
   * specify both the local and the remote hosts and ports.
   */
  public static final int DUPLEX       =    6;

  /**
   * The type code for a multicast end-point of a structured I/O
   * communication channel over UDP. A descriptor of this type must
   * specify the remote host and port.
   */
  public static final int MULTICAST    =    7;

  /**
   * The type of structured I/O resource described by this structured
   * I/O resource descriptor.
   *
   * @serial  Must be one of the type codes defined in this class. 
   */
  public int      type;

  /**
   * The environment ID for tuple storage.
   *
   * @serial  If <code>path</code> is <code>null</code>,
   *          <code>ident</code> must not be <code>null</code>.
   */
  public Guid     ident;

  /**
   * The path for tuple storage, broken up into individual path
   * segments.  If this path is absolute, the first entry in the array
   * must be "<code>/</code>".
   *
   * @serial  If not <code>null</code>, <code>path</code> must be
   *          an array of at least one entry. All entries must not
   *          be <code>null</code>. Only the first entry may be
   *          "<code>/</code>", and only if <code>ident</code>
   *          is <code>null</code>. Must be a valid array if
   *          <code>ident</code> is <code>null</code>.
   */
  public String[] path;

  /**
   * The local host.
   *
   * @serial  Must not be <code>null</code> for structured I/O
   *          resource descriptors specifying server, input,
   *          and duplex communication channel end-points.
   *
   */
  public String   localHost;

  /**
   * The local port.
   *
   * @serial  If <code>localHost</code> is not <code>null</code>,
   *          <code>localPort</code> must be a valid TCP/IP port
   *          or -1 for the default port.
   */
  public int      localPort;

  /**
   * The remote host.
   *
   * @serial  Must not be <code>null</code> for structured I/O
   *          resource descriptors specifying client, output,
   *          duplex, and multicast communication channel
   *          end-points.
   */
  public String   remoteHost;

  /**
   * The remote port.
   *
   * @serial  If <code>remoteHost</code> is not <code>null</code>,
   *          <code>remotePort</code> must be a valid TCP/IP port
   *          or -1 for the default port.
   */
  public int      remotePort;

  /**
   * The closure for server end-points.
   *
   * @serial
   */
  public Object   closure;

  /**
   * The default lease duration for communication channels accepted
   * on a server end-point.
   *
   * @serial  Must be a positive number for structured I/O resource
   *          descriptors specifying a server communication channel
   *          end-point.
   */
  public long     duration;

  /** Create a new, empty structured I/O resource descriptor. */
  public SioResource() {
    // Nothing to do.
  }

  /**
   * Create a new structured I/O resource descriptor from the
   * specified string specification.
   *
   * @param   url  The structured I/O URL for the new structured
   *               I/O resource descriptor.
   * @throws  IllegalArgumentException
   *               Signals a malformed structured I/O URL.
   */
  public SioResource(String url) {
    // We process the specified URL through a simple state machine
    // that is fed tokens produced by a string tokenizer.

    // Variables to hold the results.
    int             state;      // 0 for host/port, 1 for path, 2 for query.
    boolean         relative;
    String          host1       = null;
    int             port1       = -1;
    String          host2       = null;
    int             port2       = -1;
    ArrayList       segments    = null;
    boolean         hasDuration = false;
    StringTokenizer tokenizer; 

    // Create the string tokenizer and get going.
    if (url.startsWith("sio://")) {
      tokenizer = new StringTokenizer(url.substring(6), "/?&", true);
      state     = 0;
      relative  = false;
    } else {
      tokenizer = new StringTokenizer(url,              "/?&", true);
      state     = 1;
      relative  = true;
    }

    while (tokenizer.hasMoreTokens()) {
      String token = tokenizer.nextToken();

      switch (state) {
      case 0:
        if ("/".equals(token)) {
          // Go to path state.
          state = 1;
        } else if ("?".equals(token)) {
          // Skip path and go to query state.
          state = 2;
        } else if ("&".equals(token)) {
          throw new IllegalArgumentException("Invalid occurrence of \'&\' (" +
                                             url + ")");
        } else {
          int pos = token.indexOf(':');
          if (-1 == pos) {
            host1   = token;
            port1   = -1;
          } else {
            host1   = token.substring(0, pos);
            try {
              port1 = Integer.parseInt(token.substring(pos + 1));
            } catch (NumberFormatException x) {
              throw new IllegalArgumentException("Invalid port number (" +
                                                 url + ")");
            }
          }
        }
        break;

      case 1:
        if ("/".equals(token)) {
          if (relative && (null == segments)) {
            throw new IllegalArgumentException("Relative path starting with " +
                                               "\'/\' (" + url + ")");
          }
        } else if ("?".equals(token)) {
          // Go to query state.
          state = 2;
        } else if ("&".equals(token)) {
          throw new IllegalArgumentException("Invalid occurrence of \'&\' (" +
                                             url + ")");
        } else {
          if (null == segments) {
            segments = new ArrayList();
          }
          segments.add(token);
        }
        break;

      case 2:
        if ("/".equals(token)) {
          throw new IllegalArgumentException("Invalid occurrency of \'/\' (" +
                                             url + ")");
        } else if ("?".equals(token)) {
          throw new IllegalArgumentException("Invalid occurrency of \'?\' (" +
                                             url + ")");
        } else if ("&".equals(token)) {
          // Ignore.
        } else {
          // Process attribute/value pairs.
          int pos = token.indexOf('=');
          if (-1 == pos) {
            throw new IllegalArgumentException("Attribute without value (" +
                                               url + ")");
          }
          String attr  = token.substring(0, pos);
          String value = token.substring(pos + 1);

          if (attr.equals("type")) {                              // type
            if ("storage".equals(value)) {
              type = STORAGE;
            } else if ("client".equals(value)) {
              type = CLIENT;
            } else if ("server".equals(value)) {
              type = SERVER;
            } else if ("input".equals(value)) {
              type = INPUT;
            } else if ("output".equals(value)) {
              type = OUTPUT;
            } else if ("duplex".equals(value)) {
              type = DUPLEX;
            } else if ("multicast".equals(value)) {
              type = MULTICAST;
            } else {
              throw new IllegalArgumentException("Unrecognized type (" +
                                                 url + ")");
            }

          } else if (attr.equals("local")) {                      // local
            pos = value.indexOf(':');
            if (-1 == pos) {
              host2   = value;
              port2   = -1;
            } else {
              host2   = value.substring(0, pos);
              try {
                port2 = Integer.parseInt(value.substring(pos + 1));
              } catch (NumberFormatException x) {
                throw new IllegalArgumentException("Invalid port number (" + 
                                                   url + ")");
              }
            }

          } else if (attr.equals("closure")) {                    // closure
            closure = URLDecoder.decode(value);

          } else if (attr.equals("duration")) {                   // duration
            try {
              duration = Long.parseLong(value);
            } catch (NumberFormatException x) {
              throw new IllegalArgumentException("Invalid duration (" +
                                                 url + ")");
            }
            hasDuration = true;

          } else {                                                // ERROR
            throw new IllegalArgumentException("Unrecognized attribute (" +
                                               url + ")");
          }
        }
        break;

      default:
        throw new Bug("Invalid state when parsing structured I/O URL (" +
                      state + ")");
      }
    }

    // Fix type if not explicitly specified.
    if (0 == type) {
      // No type has been explicitly specified
      if ((null == host1) || (null != segments)) {
        type = STORAGE;
      } else {
        type = CLIENT;
      }
    }

    // Check that duration and closure are only specified for server
    // communication channels.
    if (SERVER != type) {
      if (hasDuration) {
        throw new IllegalArgumentException("Lease duration specified for " +
                                           "non-server structured I/O " +
                                           "resource (" + url + ")");
      } else if (null != closure) {
        throw new IllegalArgumentException("Closure specified for non-" +
                                           "server structured I/O resource " +
                                           "(" + url + ")");
      }
    }

    // Fill in rest of descriptor.
    if (STORAGE == type) {
      // Storage.
      if (null != host2) {
        throw new IllegalArgumentException("Local host for storage (" + url + 
                                           ")");
      } else if (relative && (null == segments)) {
        throw new IllegalArgumentException("Empty relative path (" + url + ")");
      } else if (null == segments) {
        // The root.
        path = new String[] { "/" };
      } else {
        int l = segments.size();

        // Replace path segments representing IDs with the actual IDs.
        for (int i=0; i<l; i++) {
          Guid id;

          try {
            id = new Guid((String)segments.get(i));
          } catch (NumberFormatException x) {
            continue;
          }

          segments.set(i, id);
        }

        // Normalize path: remove all occurrences of ".", remove as
        // many occurrences of ".." as possible, and trim all segments
        // before an ID.
        Guid root = null;
        for (int i=0; i<segments.size(); i++) {
          Object o = segments.get(i);

          if (".".equals(o)) {
            // Remove ".".
            segments.remove(i);
            i--;
          } else if ("..".equals(o)) {
            // Remove ".." if the previous element is not also "..".
            if ((1 <= i) && (! "..".equals(segments.get(i - 1)))) {
              segments.remove(i - 1);
              segments.remove(i - 1);
              i = i - 2;
            }
          } else if (o instanceof Guid) {
            // Push ID into root and eliminate all prior segments.
            root = (Guid)o;
            for (int j=0; j<=i; j++) {
              segments.remove(0);
            }
            i = -1;
          }
        }

        // Fix-up path segments.
        if (relative) {
          if (null != root) {
            throw new IllegalArgumentException("ID in relative path (" + url +
                                               ")");
          } else if ((null != segments) && (0 == segments.size())) {
            // Add this directory back in.
            segments.add(".");
          }
        } else {
          if (null == root) {
            // Add "/" as marker for absolute path.
            segments.add(0, "/");

            // Make sure no ".." path segments are left.
            l = segments.size();
            for (int i=0; i<l; i++) {
              if ("..".equals(segments.get(i))) {
                throw new IllegalArgumentException("Absolute path contains " +
                                                   "non-redundant \"..\"" +
                                                   "path segment (" + url +
                                                   ")");
              }
            }
          }
        }

        // Fill path into descriptor.
        l      = segments.size();
        ident  = root;
        if (0 < l) {
          path = (String[])segments.toArray(new String[l]);
        } else {
          path = null;
        }
      }

      // Fill in host and port.
      remoteHost = host1;
      remotePort = port1;

    } else {
      // Communication channels.
      if (null == host1) {
        throw new IllegalArgumentException("No host for communication channel "
                                           + "(" + url + ")");
      } else if (null != segments) {
        throw new IllegalArgumentException("Path specified for communication" +
                                           " channel (" + url + ")");
      } else if (relative) {
        throw new IllegalArgumentException("Incomplete URL for communication" +
                                           " channel (" + url + ")");
      }

      switch (type) {
      case CLIENT:
      case OUTPUT:
        remoteHost  = host1;
        remotePort  = port1;
        if (null != host2) {
          localHost = host2;
          localPort = port2;
        }
        break;

      case SERVER:
        localHost   = host1;
        localPort   = port1;
        if (hasDuration) {
          if (0 >= duration) {
            throw new IllegalArgumentException("Non-positive lease duration " +
                                               "(" + url + ")");
          }
        } else {
          duration  = Constants.LEASE_DEFAULT_DURATION;
        }
        if (null != host2) {
          throw new IllegalArgumentException("Local host for server " +
                                             "communication channel (" + url +
                                             ")");
        }
        break;

      case INPUT:
        localHost   = host1;
        localPort   = port1;
        if (null != host2) {
          throw new IllegalArgumentException("Local host for input " +
                                             "communication channel (" + url +
                                             ")");
        }
        break;
       
      case DUPLEX:
        remoteHost  = host1;
        remotePort  = port1;
        if (null == host2) {
          throw new IllegalArgumentException("No local host for communication" +
                                             " channel (" + url + ")");
        }
        localHost   = host2;
        localPort   = port2;
        break;

      case MULTICAST:
        remoteHost  = host1;
        remotePort  = port1;
        if (null != host2) {
          throw new IllegalArgumentException("Local host for multicast " +
                                             "communication channel (" + url +
                                             ")");
        }
        break;

      default:
        throw new Bug("Invalid type when parsing structured I/O URL (" +
                      type + ")");
      }
    }
  }
  
  /**
   * Determine whether this structured I/O resource descriptor
   * represents an absolute path to tuple storage.
   *
   * @return   <code>true</code> if this structured I/O resource
   *           descriptor represents an absolute path to tuple
   *           storage.
   */
  public boolean isAbsolute() {
    return ((STORAGE == type)        &&    // Must be storage
            (null    == ident)       &&    // Not ID-relative
            (null    != path)        &&    // Must have a path
            (0       <  path.length) &&
            "/".equals(path[0]));          // First segment "/"
  }

  /**
   * Determine whether this structured I/O resource descriptor
   * represents a relative path to tuple storage.
   *
   * @return  <code>true</code> if this structured I/O resource
   *          descriptor represents a relative path to tuple
   *          storage.
   */
  public boolean isRelative() {
    return ((STORAGE == type)        &&
            (null    == ident)       &&
            (null    != path)        &&
            (0       <  path.length) &&
            (! "/".equals(path[0])));
  }

  /**
   * Determine whether this structured I/O resource descriptor
   * represents an ID relative path to tuple storage.
   *
   * @return  <code>true</code> if this structured I/O resource
   *          descriptor represents an ID relative path to
   *          tuple storage.
   */
  public boolean isIdRelative() {
    return ((STORAGE == type) &&
            (null    != ident));
  }

  /** Validate this structured I/O resource descriptor. */
  public void validate() throws TupleException {
    super.validate();

    if ((SERVER != type) && (null != closure)) {
      throw new InvalidTupleException("Closure for non-server structured I/O " +
                                      "resource descriptor (" + this + ")");
    }

    switch (type) {
    case STORAGE:
      if (null == ident) {
        if (null == path) {
          throw new InvalidTupleException("Null ID and path for structured " +
                                          "I/O resource descriptor ("+this+")");
        } else if (0 == path.length) {
          throw new InvalidTupleException("Empty path for structured I/O " +
                                          "resource descriptor ("+this+")");
        }
        for (int i=0; i<path.length; i++) {
          if (null == path[i]) {
            throw new InvalidTupleException("Null path segment for structured" +
                                            " I/O resource descriptor (" +
                                            this + ")");
          } else if ((0 != i) && ("/".equals(path[i]))) {
            throw new InvalidTupleException("Invalid use of root name for " +
                                            "structured I/O resource " +
                                            "descriptor (" + this + ")");
          }
        }

      } else {
        if (null != path) {
          for (int i=0; i<path.length; i++) {
            if (null == path[i]) {
              throw new InvalidTupleException("Null path segment for " +
                                              "structured I/O resource " +
                                              "descriptor (" + this + ")");
            } else if ("/".equals(path[i])) {
              throw new InvalidTupleException("Invalid use of root name for " +
                                              "structured I/O resource " +
                                              "descriptor (" + this + ")");
            }
          }
        }
      }
      if (null != remoteHost) {
        validatePort(remotePort);
      }
      break;

    case CLIENT:
      validateHost(remoteHost);
      validatePort(remotePort);
      if (null != localHost) {
        validatePort(localPort);
      }
      break;

    case SERVER:
      validateHost(localHost);
      validatePort(localPort);
      if (0 >= duration) {
        throw new InvalidTupleException("Non-positive lease duration for " +
                                        "structured I/O resource" +
                                        "descriptor (" + this + ")");
      }
      break;

    case INPUT:
      validateHost(localHost);
      validatePort(localPort);
      break;

    case OUTPUT:
      validateHost(remoteHost);
      validatePort(remotePort);
      if (null != localHost) {
        validatePort(localPort);
      }

      break;

    case DUPLEX:
      validateHost(localHost);
      validatePort(localPort);
      validateHost(remoteHost);
      validatePort(remotePort);
      break;

    case MULTICAST:
      validateHost(remoteHost);
      validatePort(remotePort);
      break;

    default:
      throw new InvalidTupleException("Invalid type code (" + type +
                                      ") for structured I/O " +
                                      "resource descriptor (" + this + ")");
    }
  }

  /**
   * Validate the specified host name.
   *
   * @param   host  The host.
   * @throws  InvalidTupleException
   *                Signals that the specified host is <code>null</code> or
   *                contains an invalid character.
   */
  public static void validateHost(String host) throws InvalidTupleException {
    if (null == host) {
      throw new InvalidTupleException("Null host");
    }
    
    int l = host.length();
    for (int i=0; i<l; i++) {
      char c = host.charAt(i);

      if (! ((('A' <= c) && ('Z' >= c)) ||
             (('a' <= c) && ('z' >= c)) ||
             (('0' <= c) && ('9' >= c)) ||
             ('.' == c) ||
             ('-' == c))) {
        throw new InvalidTupleException("Invalid character \'" + c + "\' in" +
                                        " host name");
      }
    }
  }

  /**
   * Validate the specified port number.
   *
   * @param   port  The port number.
   * @throws  InvalidTupleException
   *                Signals that the specified port number is invalid.
   */
  public static void validatePort(int port) throws InvalidTupleException {
    if ((-1 != port) &&
        ((0 >= port) ||
         (65536 <= port))) {
      throw new InvalidTupleException("Invalid port number (" + port + ")");
    }
  }

  /**
   * Get a structured I/O URL corresponding to this descriptor for a
   * structured I/O resource. Invoking this method on an invalid
   * descriptor may produce a malformed structured I/O URL.
   */
  public String toString() {
    StringBuffer buf = new StringBuffer();
    
    // Append protocol identifier if this descriptor does not specify
    // a relative path.
    if ((STORAGE != type) ||
        (null != ident) ||
        ((null != path) &&
         (0 < path.length) &&
         ("/".equals(path[0])))) {
      buf.append("sio://");
    }

    // Do the real work.
    switch (type) {
    case STORAGE:
      // Append remote host if necessary.
      if (null != remoteHost) {
        buf.append(remoteHost);
        appendPort(remotePort, buf);
        if ((null != path) &&
            (1 == path.length) &&
            "/".equals(path[0])) {
          // This is the only type of storage URL that needs an
          // explicit type specifier.
          buf.append("/?type=storage");
          break;
        }
      }

      if (null != ident) {
        // Path relative to an ID.
        buf.append('/');
        buf.append(ident.toString());
        if (null != path) {
          for (int i=0; i<path.length; i++) {
            buf.append('/');
            buf.append(path[i]);
          }
        }

      } else if ((null != path) && (0 < path.length)) {
        if ("/".equals(path[0])) {
          // Absolute path.
          buf.append('/');
          for (int i=1; i<path.length; i++) {
            if (1 != i) {
              buf.append('/');
            }
            buf.append(path[i]);
          }

        } else {
          // Relative path.
          for (int i=0; i<path.length; i++) {
            if (0 != i) {
              buf.append('/');
            }
            buf.append(path[i]);
          }
        }
      }
      break;

    case CLIENT:
      buf.append(remoteHost);
      appendPort(remotePort, buf);
      if (null != localHost) {
        buf.append("?local=");
        buf.append(localHost);
        appendPort(localPort, buf);
      }

      break;

    case SERVER:
      buf.append(localHost);
      appendPort(localPort, buf);
      buf.append("?type=server");
      if (null != closure) {
        buf.append("&closure=");
        buf.append(URLEncoder.encode(closure.toString()));
      }
      if (Constants.LEASE_DEFAULT_DURATION != duration) {
        buf.append("&duration=");
        buf.append(duration);
      }
      break;

    case INPUT:
      buf.append(localHost);
      appendPort(localPort, buf);
      buf.append("?type=input");
      break;

    case OUTPUT:
      buf.append(remoteHost);
      appendPort(remotePort, buf);
      buf.append("?type=output");
      if (null != localHost) {
        buf.append("&local=");
        buf.append(localHost);
        appendPort(localPort, buf);
      }
      break;

    case DUPLEX:
      buf.append(remoteHost);
      appendPort(remotePort, buf);
      buf.append("?type=duplex&local=");
      buf.append(localHost);
      appendPort(localPort, buf);
      break;

    case MULTICAST:
      buf.append(remoteHost);
      appendPort(remotePort, buf);
      buf.append("?type=multicast");
      break;

    default:
      // Ignore.
      break;
    }

    return buf.toString();
  }

  /**
   * Append the specified port to the specified string buffer. This
   * method appends the specified port preceded by a '<code>:</code>'
   * to the specified string buffer if the port is not the default
   * port or -1.
   *
   * @param   port  The port to append.
   * @param   buf   The string buffer to append to.
   */
  private static void appendPort(int port, StringBuffer buf) {
    if ((-1 != port) && (Constants.PORT != port)) {
      buf.append(':');
      buf.append(port);
    }
  }

}
