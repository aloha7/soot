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

import one.world.core.Component;
import one.world.core.ComponentDescriptor;
import one.world.core.Environment;
import one.world.core.Event;
import one.world.core.EventHandler;
import one.world.core.ExportedDescriptor;
import one.world.core.ImportedDescriptor;
import one.world.core.ExceptionalEvent;
import one.world.core.DynamicTuple;
import one.world.core.Tuple;
import one.world.env.EnvironmentEvent;
import one.world.util.AbstractHandler;
import one.world.core.ExceptionalEvent;
import one.world.io.SimpleInputRequest;
import one.world.io.InputResponse;
import one.world.io.SioResource;
import one.world.io.Query;
import one.world.util.Operation;
import one.world.util.AbstractHandler;
import one.world.util.Timer;
import one.world.util.SystemUtilities;
import one.world.binding.Duration;
import one.world.binding.BindingRequest;
import one.world.binding.BindingResponse;
import one.world.binding.LeaseMaintainer;
import one.world.binding.UnknownResourceException;
import one.world.binding.BindingException;
import one.world.data.BinaryData;
import one.util.Bug;

import java.net.UnknownHostException;
import java.util.HashMap;

/**
 * <code>Responder</code> works as the backend for the <code>HttpServer</code>
 * component. Functionality for HTTP methods will be serviced within this
 * component.
 *
 * <p><b>Imported and Exported Event Handlers</b></p>
 *
 * <p>Exported event handlers:<dl>
 *    <dt>main</dt>
 *    <dd> The main event handler.
 *        </dd>
 *    <dt>server_exported</dt>
 *    <dd> The exported server event handler.
 *        </dd>
 * </dl></p>
 *
 * <p>Imported event handlers:<dl>
 *    <dt>request</dt>
 *    <dd> The environment request handler.
 *        </dd>
 *    <dt>server_imported</dt>
 *    <dd> The imported server event handler.
 *        </dd>
 *    <dt>logger</dt>
 *    <dd> The logger event handler.
 *        </dd>
 * </dl></p>
 *
 * @author   Daniel Cheah
 * @version  $Revision: 1.5 $
 */
public final class Responder extends Component {
  private static final String CLASSNAME          = Responder.class.getName();

  // =======================================================================
  //                           The main handler
  // =======================================================================

  /** The main exported event handler. */
  final class MainHandler extends AbstractHandler {
    /** Handle the specified event. */
    protected boolean handle1(Event e) {
      if (e instanceof EnvironmentEvent) {
        EnvironmentEvent ee = (EnvironmentEvent)e;
        switch (ee.type) {
        case EnvironmentEvent.ACTIVATED:
          // tell logger to start logging
          logger.handle(new EnvironmentEvent(loggerHandler, ee,
                                             EnvironmentEvent.ACTIVATE,
                                             getEnvironment().getId()));
          // tell httpserver to start
          serverImp.handle(new EnvironmentEvent(serverImpHandler, ee,
                                                EnvironmentEvent.ACTIVATE,
                                                getEnvironment().getId()));
          return true;
          
        case EnvironmentEvent.STOP:
          tsProxy.destroy();

          // tell httpserver to stop
          serverImp.handle(new EnvironmentEvent(serverImpHandler, ee,
                                                EnvironmentEvent.STOP,
                                                getEnvironment().getId()));
          // tell logger to stop logging
          logger.handle(new EnvironmentEvent(loggerHandler, ee,
                                             EnvironmentEvent.STOP, 
                                             getEnvironment().getId()));
          return true;

        default:
          return false;
        }

      } else if (e instanceof ExceptionalEvent) {
        HttpLog.error(logger, null, CLASSNAME,
                      "ExceptionalEvent", ((ExceptionalEvent)e).x);
        return true;

      } else {
        return false;

      }
    }
  }


  // ==============================================================
  //  LoggerHandler
  // ==============================================================
  final class LoggerHandler extends EnvironmentHandler {
    protected boolean handleActivated(EnvironmentEvent ee) {
      //SystemUtilities.debug("debug: logger activated");
      return true;
    }

    protected boolean handleStopped(EnvironmentEvent ee) {
      //SystemUtilities.debug("debug: logger stopped");
      loggerStop((EnvironmentEvent)ee.closure);
      return true;
    }

    protected boolean handleEnvDefault(EnvironmentEvent ee) {
      SystemUtilities.debug("strange error 1");
      return true;
    }

    protected boolean handleExceptionalEvent(ExceptionalEvent ee) {
      EnvironmentEvent env = (EnvironmentEvent)ee.closure;

      switch (env.type) {
        case EnvironmentEvent.ACTIVATED:
          SystemUtilities.debug("debug: logger activated");
          return true;

        case EnvironmentEvent.STOP:
          SystemUtilities.debug("debug: logger stopped");
          loggerStop(env);
          return true;

        default:
          SystemUtilities.debug("strange error 2");
          return true;
      }
    }

    private void loggerStop(EnvironmentEvent ee) {
      if (serverStopped) {
        respond(ee, new EnvironmentEvent(null, null, EnvironmentEvent.STOPPED,
                                         getEnvironment().getId()));
      } else {
        loggerStopped = true;
      }
    }
  }

  // ==============================================================
  //  ServerHandler
  // ==============================================================
  final class ServerImpHandler extends EnvironmentHandler {
    protected boolean handleActivated(EnvironmentEvent ee) {
      //SystemUtilities.debug("debug: parser activated");
      return true;
    }

    protected boolean handleStopped(EnvironmentEvent ee) {
      //SystemUtilities.debug("debug: parser stopped");
      parserStop((EnvironmentEvent)ee.closure);
      return true;
    }

    protected boolean handleEnvDefault(EnvironmentEvent ee) {
      SystemUtilities.debug("strange error 3");
      return true;
    }

    protected boolean handleExceptionalEvent(ExceptionalEvent ee) {
      EnvironmentEvent env = (EnvironmentEvent)ee.closure;

      switch (env.type) {
        case EnvironmentEvent.ACTIVATED:
          SystemUtilities.debug("debug: parser activated");
          HttpLog.error(logger, null, CLASSNAME,
                        "Could not start listening - stopping service",
                        ee.x);

          request.handle(new EnvironmentEvent(main, null,
                                              EnvironmentEvent.STOPPED,
                                              getEnvironment().getId()));
          return true;

        case EnvironmentEvent.STOP:
          SystemUtilities.debug("debug: parser stopped");
          parserStop(env);
          return true;

        default:
          SystemUtilities.debug("strange error 4");
          return true;
      }
    }

    private void parserStop(EnvironmentEvent ee) {
      if (loggerStopped) {
        respond(ee, new EnvironmentEvent(null, null, EnvironmentEvent.STOPPED,
                                         getEnvironment().getId()));
      } else {
        serverStopped = true;
      }
    }
  }

  // =======================================================================
  //                           The parser handler
  // =======================================================================

  /** The parser exported event handler. */
  final class ServerHandler extends AbstractHandler {

    /** Handle the specified event. */
    protected boolean handle1(Event e) {
      if (e instanceof HttpRequest) {
        HttpRequest req = (HttpRequest)e;

        switch (req.method) {
        case HttpEvent.GET:
        case HttpEvent.HEAD:
          handleGetRequest(req);
          return true;
        default:
          handleNotImplemented(req);
          return true;
        }

      } else if (e instanceof ChunkRequest) {
        handleChunkRequest((ChunkRequest)e);
        return true;
        
      } else if (e instanceof ExceptionalEvent) {
        HttpLog.error(logger, null, CLASSNAME,
                      "ExceptionalEvent", ((ExceptionalEvent)e).x);
        return true;

      } else if (e instanceof LogEvent) {
        logger.handle((LogEvent)e);
        return true;

      } else {
        return false;

      }
    }

    void handleNotImplemented(HttpRequest req) {
      respond(req,
              new HttpResponse(req.version, HttpConstants.NOT_IMPLEMENTED));
    }

    void handleChunkRequest(ChunkRequest req) {
      // Break the resource name into its component parts, tuple and
      // environment
      int    index     = RequestValidator.getSeparatorIndex(req.uri);
      String envName   = req.uri.substring(0, index);

      tsProxy.requestChunk(chunkResponder, req, envName);
    }

    void handleGetRequest(HttpRequest req) {
      // If an error has already occurred then respond
      if (HttpConstants.OK != req.status) {
        respond(req, new HttpResponse(req));
        return;
      }

      // Break the resource name into its component parts, tuple and
      // environment
      int    index     = RequestValidator.getSeparatorIndex(req.uri);
      String tupleName = req.uri.substring(index + 1);
      String envName   = req.uri.substring(0, index);

      tsProxy.requestInput(getResponder, req, envName, tupleName);
    }
 }

  // =======================================================================
  //                           Descriptors
  // =======================================================================

  /** The component descriptor. */
  private static final ComponentDescriptor SELF =
    new ComponentDescriptor("one.net.http.Responder",
                            "The HTTP responder component",
                            true);

  /** The exported event handler descriptor for the main handler. */
  private static final ExportedDescriptor MAIN =
    new ExportedDescriptor("main",
                           "The main exported event handler",
                           null,   // XXX
                           null,   // XXX
                           false);

  /** The exported event handler descriptor for the server handler. */
  private static final ExportedDescriptor SERVER_EXPORTED =
    new ExportedDescriptor("server_exported",
                           "The server exported event handle",
                           null,   // XXX
                           null,   // XXX
                           false);

  /** The imported event handler descriptor for the server handler. */
  private static final ImportedDescriptor SERVER_IMPORTED =
    new ImportedDescriptor("server_imported",
                           "The server imported event handle",
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

  /** The imported event handler descriptor for the logger handler. */
  private static final ImportedDescriptor LOGGER =
    new ImportedDescriptor("logger",
                           "The logger imported imported event handle",
                           null,
                           null,
                           false,
                           true);


  // =======================================================================
  //                           Instance fields
  // =======================================================================

  /**
   * The main exported event handler.
   *
   * @serial  Must not be <code>null</code>.
   */
  private final EventHandler       main;

  /**
   * The server exported event handler.
   *
   * @serial  Must not be <code>null</code>.
   */
  private final EventHandler       server;

  /**
   * The request imported event handler.
   *
   * @serial  Must not be <code>null</code>.
   */
  private final Component.Importer request;


  /**
   * The logger imported event handler.
   *
   * @serial  Must not be <code>null</code>.
   */
  private final Component.Importer logger;

  /**
   * The server imported event handler.
   *
   * @serial   Must not be <code>null</code>.
   */
  private final Component.Importer serverImp;
  
  /** Handle to the http server handler. */
  private final EventHandler serverImpHandler;

  /** Handle to the http logger handler. */
  private final EventHandler loggerHandler;

  /** Handle to the get responder */
  private final HttpGetResponder getResponder;

  private final ChunkResponder   chunkResponder;

  /** Tuple store proxy */
  private final TupleStoreProxy tsProxy;

  /** For use with Operation */
  private transient Timer     timer;

  /** Is the logger stopped? */
  private volatile boolean loggerStopped;

  /** Is the http server stopped? */
  private volatile boolean serverStopped;

  private final RequestValidator validator;


  // =======================================================================
  //                           Constructor
  // =======================================================================

  /**
   * Create a new instance of <code>Responder</code>.
   *
   * @param  env  The environment for the new instance.
   */
  public Responder(Environment env, String docRoot, int port) 
    throws UnknownHostException {

    super(env);
    main      = declareExported(MAIN, new MainHandler());
    server    = declareExported(SERVER_EXPORTED, new ServerHandler());
    serverImp = declareImported(SERVER_IMPORTED);
    request   = declareImported(REQUEST);
    logger    = declareImported(LOGGER);

    serverImpHandler = new ServerImpHandler();
    loggerHandler    = new LoggerHandler();

    initTransient();

    tsProxy        = new TupleStoreProxy(timer, request);
    chunkResponder = new ChunkResponder(tsProxy);
    getResponder   = new HttpGetResponder();
    validator      = new RequestValidator(docRoot, port);
  }

  // Initialize the transient variables
  private void initTransient() {
    timer = getTimer();
  }

  // =======================================================================
  //                           Component support
  // =======================================================================

  /** Get the component descriptor. */
  public ComponentDescriptor getDescriptor() {
    return (ComponentDescriptor)SELF.clone();
  }

  // =======================================================================
  //                            Initialization
  // =======================================================================

  /**
   * Initialize <i>one.net.http.Responder</i>. 
   * 
   * @param   env      The environment.
   * @param   closure  The closure.
   *
   */
  public static void init(Environment env, Object closure) 
    throws UnknownHostException {

    // Set the parameters to the defaults.
    int     port    = 80;
    String  docRoot = "htdocs";
    String  logRoot = "logs";
    boolean debug   = false;
    int     level   = 0;

    // FIXME: I'll probably want to use a properties file later
    // in the furture for now command line arguments will be
    // fine.
    if (closure instanceof String[]) {
      String[] s = (String[]) closure;

      // 1st Parameter - debug flag
      if (s.length >= 1) {
        if ("true".equals((String)s[0])) {
          debug = true;
        } else {
          debug = false;
        }
      }

      // 2nd Parameter - debug level
      if (s.length >= 2) {
        try {
          level = Integer.parseInt((String)s[1]);
        } catch (Exception x) {
          level = 0;
        }
      }

      // 3rd Parameter - Listening Port
      if (s.length >= 3) {
        port = Integer.parseInt(s[2]);
      } 

      // 4th Parameter - Document Root
      if (s.length >= 4) {
        docRoot = s[3];
      }

      // 5th Parameter - Log Root
      if (s.length >= 5) {
        logRoot = s[4];
      }
    }

    if (debug) {
      // Information about the parameters
      SystemUtilities.debug("Port         : " + port);
      SystemUtilities.debug("Document Root: " + docRoot);
      SystemUtilities.debug("Log      Root: " + logRoot);
      SystemUtilities.debug("Debug        : " + debug);
      SystemUtilities.debug("Level        : " + level);
    }

    // Components
    HttpServer   server    = new HttpServer(env, docRoot, port);
    Responder    responder = new Responder(env, docRoot, port);
    Logger       logger    = new Logger(env, logRoot, debug, level);

    // link myself w/ environment
    env.link("main", "main", responder);

    // link to environment request handler
    responder.link("request", "request", env);
    logger.link("request", "request", env);

    // link to httpserver main handler
    responder.link("server_imported", "main", server);
    // link myself w/ httpserver
    server.link("responder", "server_exported", responder);

    // link logger
    responder.link("logger", "log", logger);    
  }
}
