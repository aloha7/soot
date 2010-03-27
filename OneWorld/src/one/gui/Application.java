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

package one.gui;

import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import one.util.Bug;

import one.world.core.Component;
import one.world.core.ComponentDescriptor;
import one.world.core.Environment;
import one.world.core.Event;
import one.world.core.EventHandler;
import one.world.core.ExportedDescriptor;
import one.world.core.ImportedDescriptor;

import one.world.env.EnvironmentEvent;

import one.world.util.AbstractHandler;
import one.world.util.Timer;
import one.world.util.Operation;

/**
 * Implementation of an application's main component. This class
 * serves as a userful base class for building <i>one.world</i>
 * applications with a Swing UI.
 *
 * <p>An application has three states. In the {@link #INACTIVE
 * inactive} state, it is not running. In the {@link #ACTIVATING
 * activating} state, the application has received an activated,
 * moved, cloned, or restored {@link EnvironmentEvent environment
 * event} and is acquiring the resources it requires to run.  Finally,
 * in the {@link #ACTIVE active} state, an application is showing its
 * main window and reacting to user input.</p>
 *
 * <p>The state transition from the inactive to the activating state
 * is performed by the main exported event handler, which starts the
 * resource acquisition process by invoking {@link #acquire()}.  The
 * state transition from the activating to the active state is
 * performed by the {@link #start()} method. Finally, the state
 * transition from the active to the inactive state is performed by
 * the {@link #stop(boolean)} method, which relies on {@link
 * #release()} to release an application's resources.</p>
 *
 * <p>Subclasses typically need to override only the {@link
 * #acquire()} and {@link #release()} methods to manage an
 * application's resources and the {@link #createMainWindow()} method
 * to create the application's main window.</p>
 *
 * @version  $Revision: 1.11 $
 * @author   Robert Grimm 
 */
public class Application extends Component {

  // =======================================================================
  //                              Constants
  // =======================================================================

  /** The component descriptor. */
  private static final ComponentDescriptor SELF =
    new ComponentDescriptor("one.gui.Application",
                            "An application's main component",
                            true);

  /** The exported event handler descriptor for the main handler. */
  private static final ExportedDescriptor MAIN =
    new ExportedDescriptor("main",
                           "The main exported event handler",
                           null,
                           null,
                           false);

  /** The imported event handler descriptor for the request handler. */
  private static final ImportedDescriptor REQUEST =
    new ImportedDescriptor("request",
                           "The request imported event handler",
                           null,
                           null,
                           false,
                           true);

  /** The flag for the inactive {@link #status}. */
  public static final int INACTIVE   = 0;

  /** The flag for the activating {@link #status}. */
  public static final int ACTIVATING = 1;

  /** The flag for the active {@link #status}. */
  public static final int ACTIVE     = 2;


  // =======================================================================
  //                           The main window
  // =======================================================================

  /**
   * Implementation of an application's main window. An application's
   * main window automatically terminates the application when the
   * window is closed. It also keeps track of the window's location as
   * well as its height and width.
   */
  public static class Window extends JFrame {

    /** The application owning the window. */
    protected Application app;

    /**
     * Create a new main window for the specified application with the
     * specified title.
     *
     * @param   application  The application.
     * @param   title        The window title.
     * @throws  NullPointerException
     *                       Signals that <code>app</code> is
     *                       <code>null</code>.
     */
    public Window(Application application, String title) {
      super(title);

      if (null == application) {
        throw new NullPointerException("Null application");
      }
      app = application;

      setDefaultCloseOperation(DISPOSE_ON_CLOSE);

      addWindowListener(new WindowAdapter() {
          public void windowClosing(WindowEvent e) {
            app.stop(true);
          }
        });
      addComponentListener(new ComponentAdapter() {
          public void componentResized(ComponentEvent e) {
            app.width  = getWidth();
            app.height = getHeight();
          }
          public void componentMoved(ComponentEvent e) {
            app.locationX = getX();
            app.locationY = getY();
          }
        });
    }      

    /**
     * Show this window at the specified location. This method adjusts
     * this window's location to the specified coordinates and then
     * makes the window visible. This method is thread-safe.
     *
     * @param   x  The x coordinate.
     * @param   y  The y coordinate.
     */
    public void openAt(final int x, final int y) {
      if (SwingUtilities.isEventDispatchThread()) {
        GuiUtilities.place(this, x, y);
        setVisible(true);
      } else {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
              GuiUtilities.place(Window.this, x, y);
              setVisible(true);
            }
          });
      }
    }

    /**
     * Close the application's main window. This method disposes the
     * application's main window. This method is thread-safe.
     */
    public void close() {
      if (SwingUtilities.isEventDispatchThread()) {
        dispose();
      } else {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
              dispose();
            }
          });
      }
    }

  }


  // =======================================================================
  //                                 Fields
  // =======================================================================

  /**
   * The name of this application. This field is initialized to the
   * application's class name (without the package name).
   *
   * @serial  Must not be <code>null</code>.
   */
  public String                    appName;

  /**
   * The main exported event handler. This field is automatically
   * initialized when the application is created.
   *
   * @serial  Must not be <code>null</code>.
   */
  public final EventHandler        main;

  /**
   * The request imported event handler. This field is automatically
   * initialized when the application is created.
   *
   * @serial  Must not be <code>null</code>.
   */
  public final Component.Importer  request;

  /**
   * The timer for this application. This field is automatically
   * initialized to the environment's timer when the application is
   * created.
   *
   * @serial  Must not be <code>null</code>.
   */
  public final Timer               timer;

  /**
   * The x coordinate for the main window location. This field is
   * automatically updated as the application's main window is moved
   * across the screen.
   *
   * @serial
   */
  public volatile int              locationX;

  /**
   * The y coordinate for the main window location. This field is
   * automatically updated as the application's main window is moved
   * across the screen.
   *
   * @serial
   */
  public volatile int              locationY;

  /**
   * The width of the main window. This field is automatically
   * updated as the application's main window is resized.
   *
   * @serial
   */
  public volatile int              width;

  /**
   * The height of the main window. This field is automatically
   * updated as the application's main window is resized.
   *
   * @serial
   */
  public volatile int              height;

  /**
   * The application's current status. The value of this field must be
   * either {@link #INACTIVE}, {@link #ACTIVATING}, or {@link
   * #ACTIVE}. Access to this field must be synchronized by the
   * application's {@link #lock}. Upon deserialization of an
   * application, this field is restored to the inactive state.
   */
  public transient int             status;

  /**
   * The lock protecting the application's internal state. This field
   * is automatically initialized when the application is created and
   * when it is deserialized.
   *
   * @see  #status
   */
  public transient Object          lock;

  /**
   * The application's main window. This field is initialized to the
   * application's main window by {@link #start()} and set to
   * <code>null</code> again by {@link #stop(boolean)}.
   */
  public transient Window          mainWindow;

  /** The application's main operation. */
  public transient Operation       operation;


  // =======================================================================
  //                               Constructor
  // =======================================================================

  /**
   * Create a new application in the specified environment.
   *
   * @param  env   The environment.
   */
  public Application(Environment env) {
    super(env);
    appName   = getClass().getName();
    int idx   = appName.lastIndexOf('.');
    if (-1 != idx) {
      appName = appName.substring(idx + 1);
    }
    this.main = declareExported(MAIN, createMainHandler());
    request   = declareImported(REQUEST);
    timer     = getTimer();
    lock      = new Object();
  }


  // =======================================================================
  //                              Serialization
  // =======================================================================

  /** 
   * Serialize this application.
   *
   * @serialData  The default fields while holding the lock.
   */
  private void writeObject(ObjectOutputStream out) throws IOException {
    synchronized (lock) {
      out.defaultWriteObject();
    }
  }

  /** Deserialize an application component. */
  private void readObject(ObjectInputStream in)
    throws IOException, ClassNotFoundException {

    in.defaultReadObject();

    lock = new Object();
  }


  // =======================================================================
  //                     The main exported event handler
  // =======================================================================

  /**
   * Create the main exported event handler.
   *
   * <p>The default implementation returns an event handler that
   * handles activated, restored, moved, cloned, and stop environment
   * events. On an activated, restored, moved, or cloned environment
   * event, the returned event handler enters the activating state and
   * then invokes {@link #acquire()}. On a stop environment event, the
   * returned event handler, invokes {@link #stop(boolean)
   * stop(false)} and then responds to the stop environment event with
   * a stopped environment event.</p>
   *
   * @see     EnvironmentEvent
   *
   * @return  The main exported event handler
   */
  public EventHandler createMainHandler() {
    return new AbstractHandler() {
        protected boolean handle1(Event e) {
          if (e instanceof EnvironmentEvent) {
            EnvironmentEvent ee = (EnvironmentEvent)e;

            if ((EnvironmentEvent.ACTIVATED == ee.type) ||
                (EnvironmentEvent.RESTORED  == ee.type) ||
                (EnvironmentEvent.MOVED     == ee.type) ||
                (EnvironmentEvent.CLONED    == ee.type)) {
              synchronized (lock) {
                status = ACTIVATING;
              }
              acquire();
              return true;
              
            } else if (EnvironmentEvent.STOP == ee.type) {
              stop(false);
              respond(e, new
                EnvironmentEvent(this, null, EnvironmentEvent.STOPPED,
                                 getEnvironment().getId()));
              return true;
            }
          }
          
          return false;
        }
      };
  }


  // =======================================================================
  //                           Component support
  // =======================================================================

  /** 
   * Get the component descriptor.
   *
   * <p>This method returns a component descriptor suitable for
   * thread-safe applications. Subclasses are encouraged to overwrite
   * this method, as the descriptor returned by this method uses the
   * name of this component. However, they need not do so, because the
   * component name is not currently utilized. Note that UI-based
   * applications must be thread-safe, because Swing UI events are
   * generated by a thread separate from all <i>one.world</i>
   * threads.</p>
   */
  public ComponentDescriptor getDescriptor() {
    return (ComponentDescriptor)SELF.clone();
  }


  // =======================================================================
  //                          Starting and stopping
  // =======================================================================

  /**
   * Acquire the resources needed by this application. This method
   * starts the process of acquiring the resources needed by this
   * application and is usually invoked by the {@link
   * #createMainHandler() main exported event handler} after entering
   * the {@link #ACTIVATING activating} state.
   *
   * <p>An application must be prepared to stop while it is still
   * acquiring the resources it needs. It can detect whether it has
   * been concurrently stopped by checking for the {@link #INACTIVE
   * inactive} state.</p>
   *
   * <p>The default implementation directly transitions to the {@link
   * #ACTIVE active} state by calling {@link #start()}.</p>
   */
  public void acquire() {
    start();
  }

  /**
   * Start the application. This method completes the process of
   * resource acquisition and displays the application's main window
   * at the current location.
   *
   * <p>The default implementation creates a new main window,
   * transitions to the {@link #ACTIVE active} state, and then
   * displays the window (if it is not <code>null</code>). Calling
   * this method has no effect, if this application is not in the
   * {@link #ACTIVATING activating} state or already has a main
   * window.</p>
   *
   * <p>Applications typically need not override this method.</p>
   *
   * @see   #locationX
   * @see   #locationY
   * @see   #createMainWindow()
   */
  public void start() {
    synchronized (lock) {
      if ((ACTIVATING != status) || (null != mainWindow)) {
        return;
      }
    }

    // Create the main window outisde the application's lock.
    Window window = createMainWindow();

    synchronized (lock) {
      if ((ACTIVATING != status) || (null != mainWindow)) {
        // Somebody was faster. This should never happen, but we want
        // to be well-behaved.
        return;
      }

      // Transition to the active state.
      status = ACTIVE;

      // Set the window.
      if (null != window) {
        mainWindow = window;
        mainWindow.openAt(locationX, locationY);
      }
    }
  }

  /**
   * Stop the application. This method stops the application by
   * closing the application's main window and by releasing its
   * resources.
   *
   * <p>The default implementation transitions to the {@link #INACTIVE
   * inactive} state, closes the application's main window, releases
   * all resources by calling {@link #release()}, and, if specified,
   * notifies the application's environment of having stopped, all
   * while holding the application's lock. Calling this method has no
   * effect, if the application already is in the {@link #INACTIVE
   * inactive} state.</p>
   *
   * <p>Applications typically need not override this method.</p>
   *
   * @param  notify  The flag for whether to notify this
   *                 application's environment of having stopped.
   */
  public void stop(boolean notify) {
    synchronized (lock) {
      // Enter the inactive state.
      if (INACTIVE == status) {
        return;
      } else {
        status = INACTIVE;
      }

      // Dispose of the main window.
      if (null != mainWindow) {
        mainWindow.close();
        mainWindow = null;
      }

      // Release all resources.
      release();

      // Notify the environment, if necessary.
      if (notify) {
        request.handle(new
          EnvironmentEvent(main, null, EnvironmentEvent.STOPPED,
                           getEnvironment().getId()));
      }
    }
  }

  /**
   * Release all resources. This method releases all resources held by
   * this application and is typically invoked by {@link
   * #stop(boolean)} after transitioning to the {@link #INACTIVE
   * inactive} state and closing the main window, while holding the
   * application's lock.
   *
   * <p>An application must be prepared for the fact that not all
   * of its resource have been acquired.</p>
   * 
   * <p>The default implementation does nothing.</p>
   */
  public void release() {
    // Nothing to do.
  }


  // =======================================================================
  //                              Managing the UI
  // =======================================================================

  /**
   * Create the application's main window. This method is called by
   * {@link #start()} to create the application's main window. It can
   * return <code>null</code> if the application does not have a main
   * window. This method should only create the main window, but not
   * make it visible.
   *
   * <p>The default implementation creates an empty window, titled
   * "Main Window", with the current {@link #width} and {@link
   * #height}.</p>
   *
   * @return  The application's main window, or <code>null</code>
   *          if the application does not have a main window.
   */
  public Window createMainWindow() {
    Window window = new Window(this, "Main Window");
    window.setSize(width, height);
    return window;
  }

  /**
   * Signal an error condition to the user. This method displays an
   * error dialog box with the specified message to the user, if the
   * application is not inactive. It only returns after the user has
   * dismissed the dialog box. This method is thread-safe and must be
   * called outside the application's lock.
   *
   * @param   msg  The error message.
   */
  public void signalError(final String msg) {
    // Make sure we are actually running.
    synchronized (lock) {
      if (INACTIVE == status) {
        return;
      }
    }

    if (SwingUtilities.isEventDispatchThread()) {
      GuiUtilities.beep();
      JOptionPane.showMessageDialog(mainWindow, msg, appName + " Error",
                                    JOptionPane.ERROR_MESSAGE);
    } else {
      try {
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
              GuiUtilities.beep();
              JOptionPane.showMessageDialog(mainWindow, msg, appName + " Error",
                                            JOptionPane.ERROR_MESSAGE);
            }
          });
      } catch (Exception x) {
        throw new Bug("Unexpected exception (" + x + ")");
      }
    }
  }


  // =======================================================================
  //                            Initialization
  // =======================================================================

  /**
   * Initialize an application. The resulting application handles
   * environment events and displays an empty, resizable window.
   *
   * @param   env      The environment.
   * @param   closure  The closure, which is ignored.
   */
  public static void init(Environment env, Object closure) {
    Application app = new Application(env);

    env.link("main",    "main",    app);
    app.link("request", "request", env);
  }

}
