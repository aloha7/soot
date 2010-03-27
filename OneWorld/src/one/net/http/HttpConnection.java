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
import one.net.NetOutputStream;
import one.net.NetUtilities;
import one.util.Bug;
import one.world.util.SystemUtilities;
import one.world.core.TupleException;
import one.world.core.InvalidTupleException;
import one.world.core.DynamicTuple;
import one.world.core.Tuple;
import one.world.data.Chunk;
import java.net.Socket;
import java.net.SocketException;
import java.net.InetAddress;
import java.util.LinkedList;
import java.util.TreeSet;
import java.util.NoSuchElementException;
import java.util.List;
import java.util.HashMap;
import java.util.Vector;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.io.StreamCorruptedException;

/**
 * Support class for <code>HTTPServer</code> that handles 
 * connection with HTTP client.
 *
 * @author  Daniel Cheah
 * @version $Revision: 1.5 $
 */
public class HttpConnection {
  private  Socket           socket;
  private  NetInputStream   netIn;
  private  NetOutputStream  netOut;
  private  LinkedList       buffer;
  private  TreeSet          pending;
  private  TreeSet          queued;
  private  int              request;
  private  boolean          persistent;
  private  volatile boolean closed;
  private  InetAddress      address;

  /**
   * Create a new instance of <code>Connection</code>
   *
   * @param s a <code>Socket</code> value
   * @param in an <code>InputStream</code> value
   * @param out an <code>OutputStream</code> value
   * @param c a <code>HashMap</code> value
   */
  protected HttpConnection(Socket s, InputStream in, OutputStream out) {
    this.socket       = s;
    this.netIn        = new NetInputStream(in);
    this.netOut       = new NetOutputStream(out);
    this.closed       = false;
    this.buffer       = new LinkedList();
    this.pending      = new TreeSet();
    this.queued       = new TreeSet();
    this.request      = 0;
    this.persistent   = false;
    this.address      = s.getInetAddress();
  }
  
  /**
   * Creates a connection. 
   *
   * @param socket Socket to create connection from.
   *
   * @param capabilities a <code>HashMap</code> value
   * @return null if the connection cannot be created.
   */
  public static HttpConnection create(Socket socket) {
    InputStream  socketIn;
    OutputStream socketOut;

    try {
      // Get input stream.
      socketIn = socket.getInputStream();
      
      // Get output stream.
      socketOut = socket.getOutputStream();

    } catch (IOException x) {
      return null;

    }

    // Create the new object and fill the fields
    return new HttpConnection(socket, socketIn, socketOut);
  }

  /**
   * Closes the connection. Does the necessary cleanup
   * needed to return system resources. This includes
   * closing both the input and output stream as well as
   * closing the socket. IOExceptions that are throw are
   * caught and ignored.
   * @param c a <code>HttpConnection</code> value
   */
  public static void close(HttpConnection c) {
    //SystemUtilities.debug("close connection");
    try {
      if (null != c.netIn) {
        // close the input stream
        c.netIn.close();
      }

      if (null != c.netOut) {
        // close output stream
        c.netOut.close();
      }

      if (null != c.socket) {
        // close the socket
        c.socket.close();
      }
    } catch (IOException x) {
      // Ignore
    } 

    c.clear();
  }

  /**
   * Resets all instance variables to something
   * sensible.
   */
  protected void clear() {
    this.socket   = null;
    this.netIn    = null;
    this.netOut   = null;

    this.pending.clear();
    this.queued.clear();
    this.buffer.clear();

    this.request    = 0;
    this.persistent = false;
    this.closed     = true;

    this.address    = null;
  }

  /**
   * Is the connection closed? Signals that we logically
   * are closed. The actual connection may still be open.
   * @return a <code>boolean</code> value
   */
  public boolean isClosed() {
    return closed;
  }

  /**
   * Set the close flag to be true. Signals that we 
   * want to close the connection.
   */
  public void setClose() {
    closed = true;
  }

  /**
   * Is the connection persistent?
   * @return a <code>boolean</code> value
   */
  public boolean isPersistent() {
    return persistent;
  }

  /**
   * Queue a response to be sent out.
   * @param res a <code>HttpResponse</code> value
   * @param p a <code>Pair</code> value
   */
  public void queueResponse(HttpResponse res, Pair p) {
    
    p.setHttpResponse(res);

    // Look for the first element in the pending responses.
    Pair pend;

    try {
      pend = (Pair)pending.first();
    } catch (NoSuchElementException x) {
      //SystemUtilities.debug("nothing pending");
      pend = null;
    }

    // Satisfy the 1st pending request?
    if (p == pend) {
      pending.remove(pend);
      queued.add(pend);

      //SystemUtilities.debug("added one to the queue");

      // do it for others that can be satisfied
      try {
        while (true) {
          pend = (Pair)pending.first();
          if (null != pend.getHttpResponse()) {
            pending.remove(pend);
            queued.add(pend);
          } else {
            break;
          }
        }
      } catch (NoSuchElementException x) {
      }
      
      return;
    }
  }

  /**
   * Queue a response to be sent out.
   * @param res a <code>ChunkResponse</code> value
   * @param p a <code>Pair</code> value
   */
  public void queueResponse(ChunkResponse res, Pair p) {
    //SystemUtilities.debug("queueing chunks");
    p.setChunk(res.chunk);
  }
  
  /**
   * Does connection have responses queued?
   *
   * @return a <code>boolean</code> value
   */
  public boolean hasQueuedResponse() {
    return queued.size() > 0;
  }

  /**
   * Does connection have requests pending?
   *
   * @return a <code>boolean</code> value
   */
  public boolean hasPendingRequest() {
    return pending.size() > 0;
  }

  /**
   * Send responses to client.
   *
   * @param p a <code>Pair</code> value
   * @return Number of bytes sent to client.
   * @exception ParseException if an error occurs
   * @exception TupleException if an error occurs
   * @exception InterruptedIOException if an error occurs
   * @exception IOException if an error occurs
   */
  public long pump(Pair p) 
    throws ParseException, TupleException, InterruptedIOException, IOException {

      long size = 0;

      Pair queue;
      
      try { 
        queue = (Pair)queued.first();
      } catch (NoSuchElementException x) {
        return size;
      }

      if (null != queue) {
        p.clone(queue);
        
        if (null == queue.getHttpResponse()) {
          throw new Bug("no http response");
        }

        if (null != queue.getChunkSequence()) {
          if (!queue.isChunkHead()) {
            // Normal chunk case
            // Pump as many chunks as possible
            try {
              size = -pumpChunk(queue);
            } catch (ChunkSequence.NoMoreChunksException x) {
              queued.remove(queue);
            } catch (IOException x) {
              queued.remove(queue);
              throw x;
            }
          } else {
            // First chunk case
            // Pump Response
            size = pumpResponse(queue);
            try {
              queue.nextChunk();
            } catch (ChunkSequence.NoMoreChunksException x) {
              throw new Bug("only one chunk");
            }
          }
        } else {
          // Not Chunked
          // Pump Response
          queued.remove(queue);
          size = pumpResponse(queue);
        }
      }

      return size;
  }

  private long pumpChunk(Pair p) throws IOException, ChunkSequence.NoMoreChunksException {
    //SystemUtilities.debug("pumping a chunk");
    Chunk c = p.nextChunk();
    
    if (null != c) {
      writeChunk(c);
      return c.data.length;
    } else {
      return 0;
    }
  }

  private long pumpResponse(Pair p)
    throws ParseException, TupleException, InterruptedIOException, IOException {

    HttpRequest  req = p.req;
    HttpResponse res = p.getHttpResponse();

    sanitycheck: 
    {
      if (null == res.body) {
        // We pump with an empty message only if
        // Case 1:
        // 1. HEAD method
        // 2. CONTENT_LENGTH in header
        // 3. CONTENT_TYPE in header
        // Case 2:
        // 1. HEAD method
        // 2. HTTP/0.9
        if (HttpEvent.HEAD != req.method) {
          if (HttpEvent.HTTP09 == req.version) {
            break sanitycheck;

          } else if (null != res.header.get(HttpConstants.CONTENT_TYPE) &&
                     null != res.header.get(HttpConstants.CONTENT_LENGTH)) {

            Object o = res.header.get(HttpConstants.CONTENT_LENGTH);
            if (o instanceof String) {
              String s = (String)o;
              try {
                if (Long.parseLong(s) >= 0) {
                  break sanitycheck;
                }
              } catch (NumberFormatException x) {
              }
            } else if (o instanceof Number) {
              Number n = (Number)o;
              if (n.longValue() >= 0) {
                break sanitycheck;
              }
            } 
          }
        }

        throw new Bug("empty message body");
      }
    }

    // Set server id
    res.header.set(HttpConstants.SERVER, HttpConstants.ONEWORLD);

    // do we know the size already???
    long size = -1;
    if (res.header.get(HttpConstants.CONTENT_LENGTH) == null) {
      size = NetUtilities.size(res.body);
      res.header.set(HttpConstants.CONTENT_LENGTH, Long.toString(size));
    } else {
      size = Long.parseLong((String)res.header.get(HttpConstants.CONTENT_LENGTH));
    }

    // do we know the mimetype already
    String mimeType = null;
    if (res.header.get(HttpConstants.CONTENT_TYPE) == null) {
      mimeType = NetUtilities.type(res.body);
      res.header.set(HttpConstants.CONTENT_TYPE, mimeType);
    } else {
      mimeType = (String)res.header.get(HttpConstants.CONTENT_TYPE);
    }

    doConnectionManagement(req, res);

    // DC: Downgrade to HTTP1.0 - Need to read RFC 2145

    if (-1 == size || null == mimeType) {
      SystemUtilities.debug("DEBUG: " + res.body.toString());
      SystemUtilities.debug("DEBUG: " + size);
      SystemUtilities.debug("DEBUG: " + mimeType);
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      mimeType = NetUtilities.convert(res.body, out, null); // FIXME
      size     = out.size();
      res.header.set(HttpConstants.CONTENT_LENGTH, Long.toString(size));
      res.header.set(HttpConstants.CONTENT_TYPE, mimeType);
      if (HttpEvent.HEAD != req.method) {
        write(res, out);
      } else {
        writeHeader(res);
      }
    } else {
      if (HttpEvent.HEAD != req.method) {
        write(res);
      } else {
        writeHeader(res);
      }
    }

    return size;
  }
  
  private void doConnectionManagement(HttpRequest req, HttpResponse res) {
    // Check for the explicit close token
    if (null != req.header) {
      String s = (String)req.header.get(HttpConstants.CONNECTION);
      if (HttpConstants.CLOSE.equals(s)) {
        setClose();
        return;
      }
    }

    switch (res.version) {
    case HttpEvent.HTTP09:
      // Always close for this version. From the various RFCs
      // it appears that this version never supported
      // persistent connection
      setClose();
      res.header.set(HttpConstants.CONNECTION, HttpConstants.CLOSE);
      break;

    case HttpEvent.HTTP10:
      // Close the connection if the keep-alive token is not seen.
      if (null != req.header) {
        String s = (String)req.header.get(HttpConstants.CONNECTION);
        if (s != null && HttpConstants.KEEP_ALIVE.equals(s)) {
          res.header.set(HttpConstants.CONNECTION, HttpConstants.KEEP_ALIVE);
          break;
        } 
        // HTTP/1.0 defaults to non-persistent connections, so fall through
      } 
      setClose();
      res.header.set(HttpConstants.CONNECTION, HttpConstants.CLOSE);
      break;

    case HttpEvent.HTTP11:
      if (null != req.header) {
        String s = (String)req.header.get(HttpConstants.CONNECTION);
        // This is done with HTTP/1.1 to maintain compatibility with
        // older HTTP/1.0 clients.
        if (s != null && HttpConstants.KEEP_ALIVE.equals(s)) {
          res.header.set(HttpConstants.CONNECTION, HttpConstants.KEEP_ALIVE);
        }
        // HTTP/1.1 always maintains persistent connections so break
        break;
      }   
      setClose();
      res.header.set(HttpConstants.CONNECTION, HttpConstants.CLOSE);
      break;

    default:
      // This should never happen
      throw new Bug("unhandled http version");
    }
  }

  /**
   * Read HTTP request from the input stream.
   *
   * @exception  ParseException          Signals a parse error.
   * @exception StreamCorruptedException if an error occurs
   * @exception  InterruptedIOException  Signals a timeout of the
   *                                     underlying stream.
   * @exception  IOException             Signals a general I/O
   *                                     error of the underlying
   *                                     stream.
   *
   * @exception Exception if an error occurs
   */
  public void readRequest() 
    throws ParseException, StreamCorruptedException, InterruptedIOException, IOException, Exception {

    // read in a request
    HttpRequest req = readOneReq();

    if (null != req) {
      // one more request read in
      request++;
      // add it to the queue of pending requests
      buffer.add(new Pair(req, null, request));
    }
  }

  /**
   * Read HTTP request from the input stream. Use this to determine
   * if the connection is persistent; Only useful on the first read.
   *
   * @exception  ParseException          Signals a parse error.
   * @exception StreamCorruptedException if an error occurs
   * @exception  InterruptedIOException  Signals a timeout of the
   *                                     underlying stream.
   * @exception  IOException             Signals a general I/O
   *                                     error of the underlying
   *                                     stream.
   *
   * @exception Exception if an error occurs
   */
  public void readFirstTime() 
    throws ParseException, StreamCorruptedException, InterruptedIOException, IOException, Exception {

    // read in a request
    readRequest();
    Pair p = (Pair)buffer.getFirst();
    // determine if connection is persistent
    determinePersistent(p.req);
  }

  /**
   * Describe <code>getNextRequest</code> method here.
   *
   * @return a <code>Pair</code> value
   */
  public Pair getNextRequest() {
    if (hasBufferedRequest()) {
      Pair p = (Pair)buffer.removeFirst();
      pending.add(p);
      return p;
    }
    return null;
  }

  /**
   * Describe <code>hasBufferedRequest</code> method here.
   *
   * @return a <code>boolean</code> value
   */
  public boolean hasBufferedRequest() {
    return (buffer.size() > 0);
  }

  private void determinePersistent(HttpRequest req) {
    // This section disregards the HTTP version, it only looks
    // at the header to determine if it is persistent
    if (null != req.header) {
      String s = (String)req.header.get(HttpConstants.CONNECTION);

      // If we see the close token, this means that the connection
      // is not persistent.
      if (null != s && HttpConstants.CLOSE.equals(s)) {
        persistent = false;
        return;
      }

      // Presence of keep-alive token means that the connection is
      // persistent
      if (null != s && HttpConstants.KEEP_ALIVE.equals(s)) {
        persistent = true;
        return;
      }
    } 

    // If we get here it means that the request header provided
    // no information about the persistence of the connection
    // Use the version to determine the default action.

    // HTTP/1.1 always has persistent connections unless otherwise
    // signalled in the request header
    if (HttpEvent.HTTP11 == req.version) {
      persistent = true;
      return;
    }

    // HTTP/0.9 and HTTP/1.0 does not have persistent connections
    // unless otherwise signalled in the request header
    persistent = false;
  }
  
  private HttpRequest readOneReq() 
    throws ParseException, StreamCorruptedException, InterruptedIOException, IOException, Exception {

    HttpRequest req;
    try {
      req = read();
    } catch (ParseException x) {
      if (HttpRequest.isBadRequestLine(x)) {
        return new HttpRequest(HttpConstants.BAD_REQUEST);
      } else if (HttpRequest.isBadMethod(x)) {
        return new HttpRequest(HttpConstants.BAD_REQUEST);
      } else if (HttpRequest.isBadVersion(x)) {
        return new HttpRequest(HttpConstants.HTTP_VERSION);
      } else {
        throw x;
      }
    } catch (StreamCorruptedException x) {
      throw x;
    } catch (InterruptedIOException x) {
      throw x;
    } catch (IOException x) {
      throw x;
    }

    if (null == req) {
      return null;
    }

    // Depending on the method, we may need to read in
    // the rest of the message body.

    return req;
  }

  /**
   * Read the HTTP request from its input stream. The message
   * header is not read in if the protocol is HTTP/0.9.
   *
   * @exception  ParseException          Signals a parse error.
   * @exception  InterruptedIOException  Signals a timeout of the
   *                                     underlying stream.
   * @exception  IOException             Signals a general I/O
   *                                     error of the underlying
   *                                     stream.
   */
  private HttpRequest read() 
    throws ParseException, StreamCorruptedException, InterruptedIOException, IOException {

    String line = netIn.readLine();

    if (line == null) {
      // no request line found
      return null;
    } else {
      // request line read in
      line = line.trim();
    }

    //SystemUtilities.debug("debug: req line = [" + line + "]");

    HttpRequest req = new HttpRequest(HttpConstants.OK);

    // parse the request line.
    req.parseRequestLine(line);
    
    // Read subsequent header if not HTTP/0.9.
    if (HttpEvent.HTTP09 != req.version) {
      req.header = netIn.readHeader();
      //dumpHeader(req.header);
    }

    return req;
  }

  private void dumpHeader(DynamicTuple dt) {
    SystemUtilities.debug("------ Dumping Header -------");
    List list = dt.fields();
    for (int i = 0; i < list.size(); i++) {
      String s = (String)list.get(i);
      SystemUtilities.debug(s + ": " + dt.get(s));
    }
    SystemUtilities.debug("-----------------------------");
  }

  private void readBody(HttpRequest req, NetInputStream netIn) {
    String s = (String)req.header.get(HttpConstants.CONTENT_LENGTH);
    if (s == null) {
      req.status = HttpConstants.BAD_REQUEST;
    }

    //req.body = Net
  }

  private void writeHeader(HttpResponse res) 
    throws ParseException, IOException {

    if (HttpEvent.HTTP09 != res.version) {
      netOut.writeLine(res.formatStatusLine());
      netOut.writeHeader(res.header);
    }
  }

  /**
   * Write the HTTP response to its output stream. The response
   * is not written, if the protocol is HTTP/0.9.
   *
   * @exception  InterruptedIOException  Signals a timeout of the
   *                                     underlying stream.
   * @exception  IOException             Signals a general I/O
   *                                     error of the underlying
   *                                     stream.
   */
  private void write(HttpResponse res, ByteArrayOutputStream out)
    throws ParseException, TupleException, InterruptedIOException, IOException {

    writeHeader(res);
    out.writeTo(netOut);
    //netOut.flush();
  }

  private void write(HttpResponse res) 
    throws ParseException, TupleException, InterruptedIOException, IOException {

    writeHeader(res);
    if (res.body instanceof Chunk) {
      // Don't need to convert if its a chunk
      writeChunk((Chunk)res.body);
      
    } else {
      NetUtilities.convert(res.body, netOut, null); // FIXME
    }

    //netOut.flush();
  }

  private void writeChunk(Chunk chunk) throws IOException {
    netOut.write(chunk.data);
  }

  /**
   * Returns the internet address of the destination host.
   *
   * @return <code>InetAddress</code> of the destination host 
   */
  public InetAddress getInetAddress() {
    return address;
  }
}
