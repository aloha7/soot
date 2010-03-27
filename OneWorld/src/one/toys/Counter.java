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

package one.toys;

import java.awt.BorderLayout;
import java.awt.Color;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.lang.reflect.InvocationTargetException;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import one.util.Bug;

import one.world.Constants;

import one.world.core.Component;
import one.world.core.ComponentDescriptor;
import one.world.core.DynamicTuple;
import one.world.core.Environment;
import one.world.core.Event;
import one.world.core.EventHandler;
import one.world.core.ExportedDescriptor;
import one.world.core.ImportedDescriptor;

import one.world.env.EnvironmentEvent;

import one.world.util.AbstractHandler;
import one.world.util.Synchronous;
import one.world.util.SystemUtilities;
import one.world.util.Timer;

/**
 * Implementation of the counter application.
 * This component is discussed in detail in the <i>one.world</i> <a
 * target="_top"
 * href="http://one.cs.washington.edu/tutorial/counter.html">tutorial
 * part I</a>.
 *
 * <p><b>Imported and Exported Event Handlers</b></p>
 *
 * <p>Exported event handler(s):<dl>
 *    <dt>main</dt>
 *    <dd>Handles environment events, linked to an environment's
 *        main imported event handler.
 *        </dd>
 * </dl></p>
 *
 * <p>Imported event handler(s):<dl>
 *    <dt>timer</dt>
 *    <dd>Handles timer events, linked to a {@link Timer} component's
 *        request exported event handler and used to schedule
 *        notifications.
 *        </dd>
 *    <dt>request</dt>
 *    <dd>Handles environment events, linked to an environment's
 *        request exported event handler and used to terminate the
 *        counter application.
 *        </dd>
 * </dl></p>
 *
 * @version  $Revision: 1.2 $
 * @author   Robert Grimm
 */
public final class Counter extends Component {

  // =======================================================================
  //                               Constants
  // =======================================================================

  /**
   * The status code for the inactive status. When the counter is
   * inactive, it is not doing anything.
   */
  private static final int INACTIVE     = 1;

  /**
   * The status code for the activating status. When the counter is
   * activating, it is waiting for the timer to confirm a scheduled
   * notification.
   */
  private static final int ACTIVATING   = 2;

  /**
   * The status code for the active status. When the counter is
   * active, it is displaying the current count in a window.
   */
  private static final int ACTIVE       = 3;


  // =======================================================================
  //                           The main handler
  // =======================================================================

  /** The main exported event handler. */
  final class MainHandler extends AbstractHandler {

    /** Handle the specified event. */
    protected boolean handle1(Event e) {

      if (e instanceof EnvironmentEvent) {
        EnvironmentEvent ee = (EnvironmentEvent)e;

        if (EnvironmentEvent.ACTIVATED == ee.type) {
          // Start the activation process.
          start();
          return true;

        } else if ((EnvironmentEvent.RESTORED == ee.type) ||
                   (EnvironmentEvent.MOVED    == ee.type)) {
          // Re-diplay window.
          show();
          return true;

        } else if (EnvironmentEvent.CLONED == ee.type) {
          // Reset name.
          name = getEnvironment().getId().toString();

          // Re-display window.
          show();
          return true;

        } else if (EnvironmentEvent.STOP == ee.type) {
          // Deactivate counter.
          stop();
          respond(e, new
            EnvironmentEvent(this, null, EnvironmentEvent.STOPPED,
                             getEnvironment().getId()));
          return true;
        }

      } else if (e instanceof Timer.Event) {
        Timer.Event te = (Timer.Event)e;

        if (Timer.SCHEDULED == te.type) {
          // Display window.
          run(te.handler);
          return true;
        }

      } else if (e instanceof DynamicTuple) {
        if (e.hasField("msg") && "incr".equals(e.get("msg"))) {
          // Increment count.
          incr();
          return true;
        }
      }

      return false;
    }
  }


  // =======================================================================
  //                           Descriptors
  // =======================================================================

  /** The component descriptor. */
  private static final ComponentDescriptor SELF =
    new ComponentDescriptor("one.toys.Counter",
                            "The main counter application component",
                            true);

  /** The exported event handler descriptor for the main handler. */
  private static final ExportedDescriptor MAIN =
    new ExportedDescriptor("main",
                           "The main exported event handler",
                           new Class[] { EnvironmentEvent.class,
                                         Timer.Event.class,
                                         DynamicTuple.class },
                           null,
                           false);

  /** The imported event handler descriptor for the timer handler. */
  private static final ImportedDescriptor TIMER =
    new ImportedDescriptor("timer",
                           "The timer imported event handler",
                           new Class[] { Timer.Event.class },
                           null,
                           false,
                           true);

  /** The imported event handler descriptor for the request handler. */
  private static final ImportedDescriptor REQUEST =
    new ImportedDescriptor("request",
                           "The request imported event handler",
                           new Class[] { EnvironmentEvent.class },
                           null,
                           false,
                           true);


  // =======================================================================
  //                           Instance fields
  // =======================================================================

  /** The lock. */
  private transient Object             lock;

  /**
   * The name.
   *
   * @serial  Must not be <code>null</code>.
   */
  private           String             name;

  /**
   * The current status.
   *
   * @serial  Must be <code>INACTIVE</code>, <code>ACTIVATING</code>,
   *          or <code>ACTIVE</code>.
   */
  private           int                status;

  /**
   * The current count.
   *
   * @serial
   */
  private           long               count;

  /**
   * The main exported event handler.
   *
   * @serial  Must not be <code>null</code>.
   */
  private final     EventHandler       main;

  /**
   * The timer imported event handler.
   *
   * @serial  Must not be <code>null</code>.
   */
  private final     Component.Importer timer;

  /**
   * The request imported event handler.
   *
   * @serial  Must not be <code>null</code>.
   */
  final             Component.Importer request;

  /**
   * The event handler for canceling the timer.
   *
   * @serial  Must not be <code>null</code> if the status is
   *          <code>ACTIVE</code>.
   */
  private           EventHandler       cancel;

  /** The window frame. */
  private transient JFrame             frame;

  /** The label displaying the current count. */
  private transient JLabel             label;


  // =======================================================================
  //                           Constructor
  // =======================================================================

  /**
   * Create a new instance of <code>Counter</code>.
   *
   * @param   env   The environment for the new instance.
   * @param   name  The name for the new counter.
   * @throws  NullPointerException
   *                Signals that <code>name</code> is <code>null</code>.
   */
  public Counter(Environment env, String name) {
    super(env);
    lock      = new Object();
    this.name = name;
    status    = INACTIVE;
    main      = declareExported(MAIN, new MainHandler());
    timer     = declareImported(TIMER);
    request   = declareImported(REQUEST);
  }


  // =======================================================================
  //                              Serialization
  // =======================================================================

  /** 
   * Serialize this counter.
   *
   * @serialData  The default fields while holding the lock.
   */
  private void writeObject(ObjectOutputStream out) throws IOException {
    synchronized (lock) {
      out.defaultWriteObject();
    }
  }

  /** Deserialize a counter. */
  private void readObject(ObjectInputStream in)
    throws IOException, ClassNotFoundException {

    // Read the non-transient fields.
    in.defaultReadObject();

    // Restore the lock.
    lock = new Object();
  }


  // =======================================================================
  //                           Component support
  // =======================================================================

  /** Get the component descriptor. */
  public ComponentDescriptor getDescriptor() {
    return (ComponentDescriptor)SELF.clone();
  }


  // =======================================================================
  //                       Start, run, increment, and stop
  // =======================================================================

  /**
   * Start the activation process for this counter. If this counter is
   * inactive, this method starts the activation process by scheduling
   * a timer. Otherwise, this method does nothing.
   */
  void start() {
    synchronized (lock) {
      // Only start the activation process if the counter is inactive.
      if (INACTIVE != status) {
        return;
      }

      // Adjust status.
      status = ACTIVATING;
    }

    // Schedule new timer outside lock.
    DynamicTuple dt = new DynamicTuple();
    dt.set("msg", "incr");

    timer.handle(new
      Timer.Event(main, null, Timer.SCHEDULE, Timer.FIXED_RATE,
                  SystemUtilities.currentTimeMillis() + 1000, 1000, main, dt));
  }

  /**
   * Complete the activation process for this counter. If this counter
   * is activating, this method complete the activation process by
   * displaying the counter's window. Otherwise, this method does
   * nothing.
   *
   * @param   cancel  The event handler for canceling the timer.
   */
  void run(EventHandler cancel) {
    synchronized (lock) {
      // Only complete the activation process if the status is activating.
      if (ACTIVATING != status) {
        return;
      }

      // Remember the cancel event handler.
      this.cancel = cancel;      

      // Show the window.
      show1();

      // Tell any waiting threads that we are done.
      lock.notifyAll();

      // Adjust status.
      status = ACTIVE;
    }
  }

  /**
   * Show the window for this counter. If this counter is active, this
   * method displays the window. Otherwise, this method does nothing.
   */
  void show() {
    synchronized (lock) {
      // Only show the window if the status is active.
      if (ACTIVE != status) {
        return;
      }

      // Actually show the window.
      show1();
    }
  }

  /**
   * Show the window for this counter. This method must be called
   * while holding this counter's lock.
   */
  void show1() {
    // The Swing tutorial suggests that window initialization be done
    // in an application's main method. After the main window is shown
    // (that is, after frame.setVisible(true)), all operations need to
    // be performed in the AWT event dispatching thread. Since this
    // application may not be the only one using Swing, we do even the
    // initial creation inside the AWT event dispatching thread.
    try {
      SwingUtilities.invokeAndWait(new Runnable() {
          public void run() {
            // The ID of the hosting environment.
            JLabel heading = new JLabel(format(name));
            heading.setHorizontalAlignment(JLabel.CENTER);
            
            // The count.
            label = new JLabel(format(count));
            label.setHorizontalAlignment(JLabel.CENTER);
            
            // The background.
            JPanel content = new JPanel();
            content.setLayout(new BorderLayout());
            content.setBackground(Color.white);
            content.add(heading, BorderLayout.NORTH);
            content.add(label, BorderLayout.CENTER);
            
            // The window frame.
            frame = new JFrame("Counter");
            frame.addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                  // Stop...
                  if (stop()) {
                    // ... and tell environment about it.
                    request.handle(new
                      EnvironmentEvent(main, null, EnvironmentEvent.STOPPED,
                                       getEnvironment().getId()));
                  }
                }
              });
            frame.setContentPane(content);
            frame.pack();
            frame.setVisible(true);
          }
        });
    } catch (InterruptedException x) {
      throw new Bug(x.toString());
    } catch (InvocationTargetException x) {
      throw new Bug(x.getTargetException().toString());
    }
  }

  /**
   * Increment this counter's count. If this counter is active, this
   * method increments its count by one and updates the window.
   * Otherwise, this method does nothing.
   */
  void incr() {
    synchronized (lock) {
      // Only increment if the status is active.
      if (ACTIVE != status) {
        return;
      }

      // Increment the count.
      count++;

      // When the counter application is serialized in the active
      // state and then deserialized again, the main event handler may
      // not have yet received and processed the corresponding
      // restored, moved, or cloned environment event. As a result,
      // the window may not have been restored yet. In that case, the
      // label is null and we don't update it.
      if (null == label) {
        return;
      }

      // Update the label. We need to do this in the AWT event
      // dispatching thread.
      try {
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
              label.setText(format(count));
            }
          });
      } catch (InterruptedException x) {
        throw new Bug(x.toString());
      } catch (InvocationTargetException x) {
        throw new Bug(x.getTargetException().toString());
      }
    }
  }

  /**
   * Stop the counter. This method stops the counter by removing the
   * counter window and cancelling the timer. Calling this method on
   * an already stopped counter has no effect.
   *
   * @return  <code>true</code> if this method actually stopped
   *          the counter.
   */
  boolean stop() {
    EventHandler c;

    synchronized (lock) {
      // If the counter is inactive, we are done.
      if (INACTIVE == status) {
        return false;
      }

      // If the counter is activating, we wait until it is active
      // before we stop it.
      while (ACTIVATING == status) {
        try {
          lock.wait();
        } catch (InterruptedException x) {
          // Ignore.
        }

        // This should not happen, but better safe than sorry.
        if (INACTIVE == status) {
          return false;
        }
      }

      // Hide the window. Test whether we are running with the AWT event
      // dispatching thread. If so, just hide the frame. Otherwise, tell
      // the AWT event dispatching thread to do it.
      if (SwingUtilities.isEventDispatchThread()) {
        frame.setVisible(false);
      } else {
        try {
          SwingUtilities.invokeAndWait(new Runnable() {
              public void run() {
                frame.setVisible(false);
              }
            });
        } catch (InterruptedException x) {
          throw new Bug(x.toString());
        } catch (InvocationTargetException x) {
          throw new Bug(x.getTargetException().toString());
        }
      }

      // Null out frame and label, so that GC can do its work.
      label = null;
      frame = null;

      // Copy cancel and null it out.
      c      = cancel;
      cancel = null;

      // Adjust the status.
      status = INACTIVE;
    }

    // Cancel timer notifications. We ignore the result.
    Synchronous.invoke(c,
                       new Timer.Event(null, null, true),
                       Constants.SYNCHRONOUS_TIMEOUT);

    // Done.
    return true;
  }


  // =======================================================================
  //                                Helpers
  // =======================================================================

  /**
   * Format the specified number.
   *
   * @param   l      The number to format.
   * @return         The corresponding string.
   */
  private static String format(long l) {
    return "<html><font face=\"arial,helvetica,sans-serif\" color=\"black\"" +
      " size=\"+4\">" + Long.toString(l) + "</font>";
  }

  /**
   * Format the specified message.
   *
   * @param   msg  The message to format.
   * @return       The formatted message.
   */
  private static String format(String msg) {
    return "<html><font face=\"arial,helvetica,sans-serif\" color=\"black\">" +
      msg + "</font>";
  }


  // =======================================================================
  //                              Initializer
  // =======================================================================

  /**
   * Initialize the counter application. This method creates a new
   * instance of this class as well as a new instance of a timer
   * component. It links the environment's main imported event handler
   * to this class's main exported event handler, so that the counter
   * application receives environment events. It also links this
   * class's timer imported event handler to the timer component's
   * request exported event handler, so that the counter application
   * can schedule timer notifications. Finally, it links this class's
   * request imported event handler to the environment's request
   * exported event handler, so that the counter application can
   * terminate when the user closes its window.
   *
   * @param   env      The environment for the new counter application.
   * @param   closure  The closure for the new counter application, which
   *                   is ignored.
   */
  public static void init(Environment env, Object closure) {
    Counter comp  = new Counter(env, env.getId().toString());
    Timer   timer = new Timer(env);

    env.link("main", "main", comp);
    comp.link("timer", "request", timer);
    comp.link("request", "request", env);
  }

}
