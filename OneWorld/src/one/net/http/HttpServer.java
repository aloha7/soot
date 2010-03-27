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

import one.world.Constants;
import one.world.core.Component;
import one.world.core.ComponentDescriptor;
import one.world.core.Environment;
import one.world.core.Event;
import one.world.core.EventHandler;
import one.world.core.ExportedDescriptor;
import one.world.core.ImportedDescriptor;
import one.world.core.TupleException;
import one.world.core.ExceptionalEvent;
import one.world.env.EnvironmentEvent;
import one.world.data.BinaryData;
import one.world.data.Chunk;
import one.world.util.AbstractHandler;
import one.world.util.Operation;
import one.world.util.Timer;
import one.world.util.SystemUtilities;
import one.world.util.HandlerApplication;
import one.world.util.TimeOutException;
import one.net.NetInputStream;
import one.net.NetOutputStream;
import one.net.NetUtilities;
import one.util.Guid;
import one.util.Bug;

import java.net.Socket;
import java.net.ServerSocket;
import java.net.SocketException;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.ObjectInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StreamCorruptedException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * <code>HttpServer</code> handles the parsing of HTTP requests as well
 * as various connection management. Processing of HTTP requests are 
 * passed off to the <code>Responder</code> component.
 *
 * <p><b>Imported and Exported Event Handlers</b></p>
 *
 * <p>Exported event handlers:<dl>
 *    <dt>main</dt>
 *    <dd> The main exported event handler.
 *        </dd>
 * </dl></p>
 *
 * <p>Imported event handlers:<dl>
 *    <dt>responder</dt>
 *    <dd> The responder imported event handler.
 *        </dd>
 * </dl></p>
 *
 * @author   Daniel Cheah
 * @version  $Revision: 1.4 $
 */
public final class HttpServer extends Component {
  private static final int     NUM_THREADS            = 10;
  private static final int     QUEUE_CAPACITY         = 200;
  private static final int     DEFAULT_CLIENT_TIMEOUT = 20000;
  /** Default timeout for the server socket */
  private static final int     SERVER_SOCKET_TIMEOUT  = 5000;
  private static final int     WAIT_PERIOD            = 1000;
  private static final String  HTML_ERROR_ENCODING    = "8859_1";
  private static final String  CLASSNAME              = HttpServer.class.getName();
  private static final int     DEFAULT_CACHE_SIZE     = 64;

  // =======================================================================
  //                           The httpserver handler
  // =======================================================================

  /** 
   * The httpserver exported event handler. 
   */
  final class MainHandler extends AbstractHandler {
    /** Handle the specified event. */
    protected boolean handle1(Event e) {
      if (e instanceof EnvironmentEvent) {
        EnvironmentEvent ee = (EnvironmentEvent)e;
        switch (ee.type) {
        case EnvironmentEvent.ACTIVATE:
          threadPool.setThreadNumber(NUM_THREADS);
          threadPool.activateThreads();
          threadPool.setStatus(ThreadPool.ACTIVE);
          try {
            listener.startServer();
          } catch (Exception x) {
            respond(ee, x);
            return true;
          }
          respond(ee, new EnvironmentEvent(null, null, EnvironmentEvent.ACTIVATED,
                                           getEnvironment().getId()));
          return true;
        case EnvironmentEvent.STOP:
          stat.dumpStatistics();
          listener.stopServer();
          threadPool.setStatus(ThreadPool.INACTIVE);
          threadPool.setThreadNumber(0);
          respond(ee, new EnvironmentEvent(null, null, EnvironmentEvent.STOPPED,
                                           getEnvironment().getId()));

          return true;
        default:
          return false;
        }
      } 
   
      return false;
    }
  }

  private void startConnection(Socket socket) {
    try {
      socket.setSoTimeout(DEFAULT_CLIENT_TIMEOUT);
    } catch (SocketException x) {
      // what to do? we should not really fail since
      // we can still go on
      HttpLog.warn(responder, socket.getInetAddress(), CLASSNAME,
                   "Socket exception trying to set timeout", x);
    }

    HttpConnection c = HttpConnection.create(socket);
    if (null != c) {
      try {
        c.readFirstTime();
      } catch (Exception x) {
        HttpConnection.close(c);
      }

      if (c.isPersistent()) {
        persistent.connectionLoop(c);
             
      } else {
        Pair p = c.getNextRequest();
        if (null != p) {
          handleRequest(p, c);
        } else {
          HttpConnection.close(c);
        }
      }
      
    } else {
      // We cannot really do anything at this point, since 
      // we are unable to create the connection. 
      // The sensible thing is to close the connection. This
      // will signal to the client that something bad has happened
      // causing it to recover on the other end. On our end,
      // we are cleaning up any resources used during creation.
      HttpConnection.close(c);
    }
  }

  final class Listener implements Runnable, Serializable {
    private transient Object       lock;
    private transient ServerSocket serverSocket;
    private volatile  boolean      running;

    Listener() {
      running      = false;
      lock         = new Object();
      serverSocket = null;
    }

    /**
     * Start the HTTP listener listening for connections.
     * This will capture the current thread. Thread will
     * not return until it is stopped.
     * 
     * @param ee <code>EnvironmentEvent</code that we 
     *           respond to if an exception occurs.
     */
    void startServer() throws IOException, SocketException {
      // Check that the server is not running.
      if (running) {
        return;
      }

      // Set the server status to be running.
      running = true;
  
      // Create the server socket.
      serverSocket = new ServerSocket(validator.getPort());

      // Set the server socket timeout.
      serverSocket.setSoTimeout(SERVER_SOCKET_TIMEOUT);

      // Run the listening loop - captures the current thread.
      HttpServer.this.runInNewThread(this);
    }

    /** 
     * Stop the HTTP listener. It does this by setting the
     * <code>running</code> flag to be false. Listening may
     * not stop immediately.
     */
    void stopServer() {
      // Check that we not already trying to stop the 
      // server.
      if (!running) {
        return;
      }

      synchronized (lock) {
        // Attempt to stop the server.
        running = false;

        try {
          lock.wait();
        } catch (InterruptedException x) {
          // ignore
        }
      } 
    }

    /** 
     * Captures the current thread and uses it for listening to
     * incoming connections. It then passes the incoming connections
     * to the parser component to handle.
     */
    public void run() {
      while (true) {
        // Socket client is connected to.
        Socket clientSocket = null;
        
        try {
          // Listen for connections on specifed port
          clientSocket = serverSocket.accept();
        } catch (InterruptedIOException x) {
          // Server socket timed out.
          if (!running) {
            break;
          }
          continue;
        } catch (IOException x) {
          // Not sure what could cause this exception.
          // Drop this connection and listen for another
          // connection
          HttpLog.error(responder, null, CLASSNAME,
                        "Unexpected I/O exception", x);
          continue;
        }
    
        // Get the parser component to handle the connection. Since
        // this component is forcibly linked to other components, this
        // request will be queued up. The intent is that the current thread
        // is only responsible for listening to connections. By not doing any
        // of the parsing, it will be able to accept connections at a much 
        // faster rate.
        threadPool.enqueue(clientSocket);
      }

      try {
        serverSocket.close();
      } catch (IOException x) {
        HttpLog.error(responder, null, CLASSNAME,
                      "Exception while closing the server socket", x);
      }

      serverSocket = null;

      synchronized (lock) {
        lock.notify();
      }
    }
  }
    
  final class PersistentConnection implements EventHandler, Serializable {
    public void connectionLoop(HttpConnection c) {
      int    ticks = 0;
      Object wait  = new Object();

      while (true) {
        try {
          if (!c.hasBufferedRequest()) {
            c.readRequest();
          }
        } catch (Exception x) {
          c.setClose();
          break;
        }

        Pair p = c.getNextRequest();

        if (null == p) {
          // This is hacky but it is necessary for some clients
          // which appear brain dead - hopefully it does not
          // happen too often.
          try {
            synchronized (wait) {
              wait.wait(WAIT_PERIOD);
            }
          } catch (InterruptedException x) {
          }

          // increment the timer tick by 1
          ticks++;

          // we should time out the socket
          if (ticks >= (DEFAULT_CLIENT_TIMEOUT/WAIT_PERIOD)) {
            c.setClose();
            break;
          }
          continue;

        } else {
          handleRequest(p, c);
        }
      }

      // If the connection has no pending requests or
      // queued responses, and we have no new requests
      // being read for the time out period, this
      // should be an indication that we want to 
      // close already
      if (!c.hasQueuedResponse() &&
          !c.hasPendingRequest()) {
        HttpConnection.close(c);
      }
    }

    /*
    public void connectionLoop(HttpConnection c) {
      int    ticks = 0;
      Object wait  = new Object();

      while (!c.isClosed()) {
        try {
          if (!c.hasBufferedRequest()) {
            c.readRequest();
          }
        } catch (Exception x) {
          stat.timeout1++;
          SystemUtilities.debug("close 1");
          HttpConnection.close(c);
          return;
        }

        Pair p = c.getNextRequest();

        if (null == p) {
          // This is hacky but it is necessary for some clients
          // which appear brain dead - hopefully it does not
          // happen too often.
          try {
            synchronized (wait) {
              wait.wait(WAIT_PERIOD);
            }
          } catch (InterruptedException x) {
          }

          // increment the timer tick by 1
          ticks++;

          // we should time out the socket
          if (ticks >= (DEFAULT_CLIENT_TIMEOUT/WAIT_PERIOD)) {
            stat.timeout2++;
            SystemUtilities.debug("close 2");
            HttpConnection.close(c);
          }
          continue;

        } else {
          ticks = 0;
          handleRequest(p, c);
        }
      }
    }
    */

    public void handle(Event e) {
      HttpConnection c  = (HttpConnection)e.closure;

      connectionLoop(c);
    }
  }

  /** 
   * say something 
   */
  final class HttpResponseHandler extends AbstractHandler {
    protected boolean handle1(Event e) {
      HttpConnection c = connections.get((Key)e.closure);
      Pair           p = pending.get((Key)e.closure);

      if (e instanceof HttpResponse) {
        HttpResponse res = (HttpResponse)e;

        // Handle the error condition
        if (HttpConstants.OK != res.status && null == res.body) {
          // Don't add to the cache if not found or error
          createResponseError(res);
        } else {
          // Add to the cache
          tupleCache.addEntry(new TupleCache.Entry(res.body, p.req.uri));
        }

        // Queue the response to handle pipelined requests
        c.queueResponse(res, p);
        handleResponse(c);

        // To be on the safe side, I'm requesting the next
        // chunk after I've handled the request.
        // TODO: think more about synchronization
        if (null != res.body && res.body instanceof Chunk) {
          // We have a chunk, request another
          handleChunk(p, c, (Key)e.closure, p.req.uri, ((Chunk)res.body).next);
        } 

        return true;

      } else if (e instanceof ChunkResponse) {
        ChunkResponse res = (ChunkResponse)e;

        c.queueResponse(res, p);
        handleResponse(c);

        // This is not the last chunk, request another
        if (null != res.chunk.next) {
          handleChunk(p, c, (Key)e.closure, p.req.uri, res.chunk.next);
        } 

        return true;
        
      } else if (e instanceof ExceptionalEvent) {
        ExceptionalEvent ee = (ExceptionalEvent)e;
        HttpResponse   res  = new HttpResponse();

        // TODO: Check also for chunk not found exception

        if (ee.x instanceof TimeOutException) {
          res.status = HttpConstants.REQUEST_TIMEOUT;
          HttpLog.error(responder, c.getInetAddress(), CLASSNAME,
                        "Request Timeout", ee.x);
        } else {
          res.status = HttpConstants.INTERNAL_SERVER_ERROR;
          HttpLog.error(responder, c.getInetAddress(), CLASSNAME,
                        "Internal Server Error", ee.x);
        }

        createResponseError(res);
        c.queueResponse(res, p);
        handleResponse(c);
        return true;
      }

      return false;
    }
  }

  private void runInNewThread(final Runnable r) {
    AccessController.doPrivileged(new PrivilegedAction() {
        public Object run() {
          HttpServer.this.run(r);
          return null;
        }
      });
  }

  private HttpResponse createCachedResponse(HttpRequest req, 
                                            TupleCache.Entry entry) {

    HttpResponse res = new HttpResponse(req);

    res.status  = HttpConstants.OK;
    res.body    = entry.tuple;

    return res;
  }

  /** 
   * We are dealing with an error of some sort
   * create the html error page
   */
  private HttpResponse createResponseError(HttpRequest req) {
    HttpResponse res = new HttpResponse();

    // Fix up bad requests
    if (HttpRequest.getVersionString(req.version) == null) {
      res.version = HttpEvent.HTTP09;
    } else {
      res.version = req.version;
    }

    res.status  = req.status;
    createResponseError(res);

    return res;
  }

  private void createResponseError(HttpResponse res) {
    BinaryData bd = ResponderInfo.getHtmlError().createPage(res);

    res.body = bd;
    res.header.set(HttpConstants.CONTENT_LENGTH, Long.toString(bd.data.length));
    res.header.set(HttpConstants.CONTENT_TYPE, HttpConstants.TEXT_HTML);
  }

  private void handleChunk(Pair p, HttpConnection c, Key k, String uri, Guid id) {
    ChunkRequest req = new ChunkRequest(null, k, uri, id);
    connections.set(k, c);
    pending.set(k, p);
    responderOp.handle(req);
  }

  private StringBuffer validateRequest(HttpRequest req) {
    URL url;

    // Check that we can parse the uri
    try {
      url = HttpURI.parseURI(req.uri);
    } catch (ParseException e) {
      return null;
    }
      
    // Verify the url describes local resource
    if (!validator.verify(url)) {
      return null;
    }
      
    // Validate the uri and get back the resource name
    StringBuffer buf = validator.validate(url);
    if (RequestValidator.contains2Dots(buf)) {
      return null;
    }

    return buf;
  }

  private void handleRequest(Pair p, HttpConnection c) {
    // Validate the URL Here, once it is
    // validated we can let it into the system
    // We use to do this in the Responder. But
    // that makes less sense because we have
    // to do more work when we can find out
    // if something is an error.
    StringBuffer buf = validateRequest(p.req);
    if (null == buf) {
      p.req.status = HttpConstants.BAD_REQUEST;
    } else {
      p.req.uri = buf.toString();
    }
    
    if (HttpConstants.OK == p.req.status) {
      // Does this exist in the cache
      TupleCache.Entry entry = tupleCache.getEntry(p.req.uri);
      
      if (null != entry) {
        HttpResponse res = createCachedResponse(p.req, entry);
        c.queueResponse(res, p);
        handleResponse(c);

        // To be on the safe side, I'm requesting the next
        // chunk after I've handled the request.
        // TODO: think more about synchronization
        if (null != res.body && res.body instanceof Chunk) {
          // We have a chunk, request another
          handleChunk(p, c, new Key(), p.req.uri, ((Chunk)res.body).next);
        } 
        
      } else {
        // Do this so that when we get back the response
        // we will be able to get back the request as well
        // as the required connection information.
        p.req.closure = new Key();
        connections.set((Key)p.req.closure, c);
        pending.set((Key)p.req.closure, p);
        responderOp.handle(p.req);
      }

    }  else if (HttpConstants.OK != p.req.status) {
      // Handle errors synchronously
      HttpResponse res  = createResponseError(p.req);
      c.queueResponse(res, p);
      handleResponse(c);

    }
  }

  private void handleResponse(HttpConnection c) {
    Pair p = new Pair();

    try {
      while (true) {
        long size = c.pump(p);
        if (size > 0) {
          HttpLog.access(responder, p.req, 
                         c.getInetAddress(), 
                         p.getHttpResponse().status, size);

        } else if (size == 0) {
          // if there is nothing to pump then 
          // we want to break out of the loop
          // and do other stuff
          break;

        } else if (size < 0) {
          // This is HACKY!!! I'm overloading the
          // return value. If it is negative, then
          // a chunk of -size was pumped.
        }
      }

    } catch (InterruptedIOException x) {
      HttpLog.error(responder, c.getInetAddress(), CLASSNAME, 
                    "Socket timed out", x);

    } catch (IOException x) {
      HttpLog.error(responder, c.getInetAddress(), CLASSNAME,
                    "Unexpected I/O exception while writing", x);

    } catch (TupleException x) {
      HttpLog.error(responder, c.getInetAddress(), CLASSNAME,
                    "Invalid tuple", x); 

    } catch (ParseException x) {
      HttpLog.error(responder, c.getInetAddress(), CLASSNAME,
                    "Parsing exception", x);

    } finally {
      if (c.isClosed() && !c.hasQueuedResponse() && !c.hasPendingRequest()) {
        HttpConnection.close(c);
      }
    }
  }

  // =======================================================================
  //                           Descriptors
  // =======================================================================

  /** The component descriptor. */
  private static final ComponentDescriptor SELF =
    new ComponentDescriptor("one.net.http.HttpParser",
                            "The HTTP parser component",
                            true);

  /** The exported event handler descriptor for the httpserver handler. */
  private static final ExportedDescriptor MAIN =
    new ExportedDescriptor("main",
                           "The http server event handle",
                           null,   // XXX
                           null,   // XXX
                           false);

  /** THe imported event handler descriptor for the responder handler. */
  private static final ImportedDescriptor RESPONDER =
    new ImportedDescriptor("responder",
                           "The responder imported event handle",
                           null,
                           null,
                           false,
                           true);

  /** The imported event handler descriptor for the request handler. */
  private static final ImportedDescriptor REQUEST =
    new ImportedDescriptor("request",
                           "The request imported event handle",
                           null,   // XXX
                           null,   // XXX
                           false,
                           true);
  
  // =======================================================================
  //                           Instance fields
  // =======================================================================

  /**
   * The httpserver exported event handler.
   *
   * @serial  Must not be <code>null</code>.
   */
  private final EventHandler        main;

  /**
   * The request imported event handler.
   *
   * @serial  Must not be <code>null</code>.
   */
  private final Component.Importer request;

  /**
   * The responder imported event handler.
   *
   * @serial  Must not be <code>null</code>.
   */
  private final Component.Importer  responder;

  /** The http responder handler. */
  private final HttpResponseHandler  response;

  /** The persistent connection handler. */
  private final PersistentConnection persistent;

  /** For maintaining various statistics */
  private final Stat                stat;
  
  // =======================================================================
  //  Transient Fields 
  // =======================================================================  
  
  /** For use with Operation */
  private transient Timer      timer;

  /** For interacting with the one.world kernel. */
  private transient Operation  responderOp;

  /** The table of pending request/response interactions. */
  private transient PendingRequestsTable pending;
  
  /** The table of active connections */
  private transient ConnectionTable      connections;

  /** The internal threadpool */
  private transient ThreadPool           threadPool;

  /** Listener for HTTP connections. */
  private transient Listener             listener;

  private transient TupleCache           tupleCache;

  private final RequestValidator validator;

  // =======================================================================
  //                           Constructor
  // =======================================================================

  /**
   * Create a new instance of <code>HttpServer</code>.
   *
   * @param env       The environment for the new instance.
   * @param port      Port to listen for HTTP connections on.
   * @param errorPage Error page generation.
   * @param cap       Methods serviced by this responder.
   */
  public HttpServer(Environment env, String docRoot, int port) throws UnknownHostException {
    super(env);
    main         = declareExported(MAIN, new MainHandler());
    responder    = declareImported(RESPONDER);
    request      = declareImported(REQUEST);
    response     = new HttpResponseHandler();
    persistent   = new PersistentConnection();

    stat = new Stat();
    validator = new RequestValidator(docRoot, port);
    
    initTransient();
  }

  /** Initialize the transient fields */
  private void initTransient() {
    timer       = getTimer();
    responderOp = new Operation(0, Constants.OPERATION_TIMEOUT, 
                                timer, responder, response);
    pending     = new PendingRequestsTable();
    connections = new ConnectionTable();
    threadPool  = new ThreadPool(QUEUE_CAPACITY, NUM_THREADS);
    listener    = new Listener();
    tupleCache  = new TupleCache(DEFAULT_CACHE_SIZE);
  }

  // =======================================================================
  //                           Serialization
  // =======================================================================

  /** Deserialize a main component. */
  private void readObject(ObjectInputStream in)
    throws IOException, ClassNotFoundException {

    // Read the non-transient fields.
    in.defaultReadObject();

    // Restore the transient fields.
    initTransient();
  }

  // =======================================================================
  //                           Component support
  // =======================================================================

  /** Get the component descriptor. */
  public ComponentDescriptor getDescriptor() {
    return (ComponentDescriptor)SELF.clone();
  }

  // =======================================================================
  //  Support Classes
  // =======================================================================

  /**
   * Statistics
   */
  final class Stat implements Serializable {
    int timeout1;
    int timeout2;

    void dumpStatistics() {
      /*
      SystemUtilities.debug("Timeout 1: " + timeout1);
      SystemUtilities.debug("Timeout 2: " + timeout2);
      */
    }
  }

  /**
   * Pending HTTP Request Table.
   */
  final class PendingRequestsTable {
    private final HashMap table;

    /** Constructor */
    PendingRequestsTable() {
      table = new HashMap();
    }

    /** get */
    synchronized Pair get(Key k) {
      return (Pair)table.remove(k);
    }

    /** set */
    synchronized void set(Key k, Pair p) {
      table.put(k, p);
    }
  }

  /**
   * Open Connection Table.
   */
  final class ConnectionTable {
    private final HashMap table;

    /** Constructor */
    ConnectionTable() {
      table = new HashMap();
    }

    /** get */
    synchronized HttpConnection get(Key k) {
      return (HttpConnection)table.remove(k);
    }

    /** set */
    synchronized void set(Key k, HttpConnection c) {
      table.put(k, c);
    }
  }

  /**
   * Key
   */
  final class Key {
    /**
     * The ID for this key.
     *
     * @serial  Must not be <code>null</code>.
     */
    private final Guid id;
    
    /** Create a new key. */
    Key() {
      id = new Guid();
    }
    
    /** Determine whether this key equals the specified object. */
    public boolean equals(Object o) {
      if (this == o) 
        return true;
      if (! (o instanceof Key)) 
        return false;
    
      return id.equals(((Key)o).id);
    }
    
    /** Return a hashcode for this key. */
    public int hashCode() {
      return id.hashCode();
    }
  }

  /**
   * ThreadPool - adapted from one.world.util.ThreadPool.
   */
  final class ThreadPool {
    final class Worker implements Runnable {
      public void run() {
        boolean looping = false;

        do {
          Object obj;

          synchronized (monitor) {
            if (looping) {
              // Flag that we are not running and notify a waiting thread.
              busy.remove(this);
              idle.add(this);

              // Notify thread in setStatus() that status transition is
              // complete.
              if ((INACTIVE == status) && (0 == busy.size())) {
                // Need to notify all to make sure that thread in
                // setStatus() gets woken up.
                monitor.notifyAll();

              } else if ((DRAINED == status) && (-1 == tail)) {
                // Need to notify all to make sure that thread in
                // setStatus() gets woken up.
                monitor.notifyAll();
              }

            } else {
              looping = true;
            }

            do {
              // Check status.
              if (TERMINATED == status) {
                // Flag that we are done.
                idle.remove(this);
                if ((0 == idle.size()) && (0 == busy.size())) {
                  monitor.notify();
                }
                return;

              } else if (idle.size() + busy.size() > threadNumber) {
                // The number of threads has been reduced.
                idle.remove(this);
                return;
              
              } else if ((0 > tail) || (INACTIVE == status)) {
                try {
                  monitor.wait();
                } catch (InterruptedException x) {
                  // Ignore.
                }

                continue;
              }

              break;
            } while (true);

            obj = objects[head];
            objects[head] = null;

            head++;
          
            if (objects.length <= head) {
              head = 0;
            }
            if (tail == head) {
              tail = -1;
            }

            // Flag that we are running.
            idle.remove(this);
            busy.add(this);
          }

          // handle the event outside the monitor
          dispatch(obj);

        } while (true);
      }
    }

    public void dispatch(Object obj) {
      if (obj instanceof Socket) {
        startConnection((Socket)obj);
      } else if (obj instanceof HttpConnection) {
        persistent.connectionLoop((HttpConnection)obj);
      } else {
        throw new Bug("don't know how to dispatch object");
      }
    }

    /** The status code for an active animator. */
    public final static int ACTIVE     = 1;

    /** The status code for an inactive animator. */
    public final static int INACTIVE   = 2;

    /** The status code for a drained animator. */
    public final static int DRAINED    = 3;

    /** The status code for a terminated animator. */
    public final static int TERMINATED = 4;


    private transient Object monitor;

    private final HashSet busy;
    private final HashSet idle;

    private Object[] objects;

    private int head;
    private int tail;
    private int status;
    private int threadNumber;


    public ThreadPool(int capacity, int number) {
      if (0 >= capacity) {
        throw new IllegalArgumentException("Non-positive queue capacity for " +
                                           "thread pool (" + capacity + ")");
      } else if (0 >= number) {
        throw new IllegalArgumentException("Non-positive thread number for " +
                                           "thread pool (" + number + ")");
      }

      status       = INACTIVE;
      objects      = new Object[capacity];
      head         =  0;
      tail         = -1;
      monitor      = new Object();
      threadNumber = number;
      idle         = new HashSet(threadNumber * 4 / 3 + 2);
      busy         = new HashSet(threadNumber * 4 / 3 + 2);
    }

    void activateThreads() {
      for (int i = 0; i < threadNumber; i++) {
        Worker worker = new Worker();
        runInNewThread(worker);
        idle.add(worker);
      }
    }
    
    /** Enqueue the specified event handler application. */
    public boolean enqueue(Object object) {
      synchronized (monitor) {
        if (TERMINATED == status) {
          throw new IllegalStateException("Thread pool terminated");
        } else if (DRAINED == status) {
          return false;
        }
        
        // Enqueue the event handler application.
        if (head == tail) { return false; }
        
        if (0 > tail) {
          tail = 0;
          head = 0;
        }
        
        objects[tail] = object;
        
        tail++;
        
        if (objects.length <= tail) {
          tail = 0;
        }
        
        monitor.notify();
      }
    
      return true;
    }

    /**
   * Enqueue the specified event handler application at the front of
   * the queue.
   */
    public boolean enqueueFirst(Object obj) {
      synchronized (monitor) {
        if (TERMINATED == status) {
          throw new IllegalStateException("Thread pool terminated");
        } else if (DRAINED == status) {
          return false;
        }
        
        // Enqueue the event handler application.
        if (head == tail) { return false; }
        
        if (0 > tail) {
          tail = 1;
          head = 1;
        }

        head--;

        if (0 > head) {
          head = objects.length - 1;
        }

        objects[head] = obj;

        monitor.notify();
      }
    
      return true;
    }

    /**
     * Get the number of threads for this thread pool.
     *
     * @return     The number of threads for this thread pool.
     * @throws  IllegalStateException
     *             Sginals that this thread pool has terminated.
     */
    public int getThreadNumber() {
      synchronized (monitor) {
        if (TERMINATED == status) {
          throw new IllegalStateException("Thread pool terminated");
        }

        return threadNumber;
      }
    }

    /**
     * Set the thread number for this thread pool to the specified
     * number.
     *
     * @param   number  The new number of threads for this thread
     *                  pool.
     * @throws  IllegalArgumentException
     *                  Signals that <code>number</code> is not
     *                  positive.
     * @throws  IllegalStateException
     *                  Signals that this thread pool has terminated.
     */
    public void setThreadNumber(int number) {
      if (0 > number) {
        throw new IllegalArgumentException("Non-positive thread number (" +
                                           number + ")");
      }

      synchronized (monitor) {
        if (TERMINATED == status) {
          throw new IllegalStateException("Thread pool terminated");
        }

        if (threadNumber == number) {
          // Nothing to do.
          return;

        } else if (threadNumber > number) {
          // Need to exit some workers.
          threadNumber = number;
          if ((INACTIVE == status) || (DRAINED == status)) {
            monitor.notifyAll();
          }

        } else {
          // Create additional workers.
          int diff = number - threadNumber;
          threadNumber = number;
          for (int i=0; i<diff; i++) {
            Worker worker = new Worker();
            idle.add(worker);
            runInNewThread(worker);
          }
        }
      }
    }

    /** Get the current status for this thread pool. */
    public int getStatus() {
      synchronized (monitor) {
        return status;
      }
    }

    /** Set the status for this thread pool. */
    public void setStatus(int status) {
      synchronized (monitor) {
        if (this.status == status) {
          return;
        } else if (TERMINATED == this.status) {
          throw new IllegalStateException("Thread pool terminated");
        }

        switch (status) {
        case ACTIVE:
          this.status = ACTIVE;
          monitor.notifyAll();
          break;

        case INACTIVE:
          this.status = INACTIVE;
          // No need to notify b/c nobody is waiting for this state.
          while (0 < busy.size()) {
            try {
              monitor.wait();
            } catch (InterruptedException x) {
              // Ignore.
            }
          }
          break;

        case DRAINED:
          this.status = DRAINED;
          monitor.notifyAll();
          while ((0 < busy.size()) || (-1 != tail)) {
            try {
              monitor.wait();
            } catch (InterruptedException x) {
              // Ignore.
            }
          }
          break;

        case TERMINATED:
          if (ACTIVE == this.status) {
            throw new IllegalArgumentException("Thread pool active");
          }
          this.status = TERMINATED;
          monitor.notifyAll();
          while ((0 < idle.size()) || (0 < busy.size())) {
            try {
              monitor.wait();
              // FIXME time out and signal a thread death to worker.
            } catch (InterruptedException x) {
              // Ignore.
            }
          }
          // Let GC do its work.
          objects = null;
          break;

        default:
          throw new IllegalArgumentException("Invalid animator status (" +
                                             status + ")");
        }
      }
    }
  }
}
