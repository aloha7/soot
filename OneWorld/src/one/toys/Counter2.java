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
import java.awt.Dimension;

import java.io.IOException;
import java.io.ObjectOutputStream;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import one.gui.Application;
import one.gui.GuiUtilities;

import one.util.Guid;

import one.world.binding.Duration;

import one.world.core.DynamicTuple;
import one.world.core.Environment;
import one.world.core.Event;

import one.world.util.AbstractHandler;
import one.world.util.SystemUtilities;
import one.world.util.Timer;

/**
 * Implementation of a counter. This class is a re-implementation of
 * the {@link Counter counter application} discussed in the
 * <i>one.world</i> tutorial. Unlike the original counter, it uses the
 * synchronous timer interface and the application base class, which
 * greatly simplifies this re-implementation.
 *
 * <p>Usage:<pre>
 *    Counter2
 * </pre></p>
 *
 * @see      Timer
 * @see      Application
 *
 * @version  $Revision: 1.4 $
 * @author   Robert Grimm
 */
public final class Counter2 extends Application {

  // =======================================================================
  //                           The main window
  // =======================================================================

  /** Implementation of the counter's main window. */
  static final class Window extends Application.Window {

    /** The count label. */
    final JLabel count;

    /**
     * Create a new main window.
     *
     * @param  counter  The counter component.
     */
    Window(final Counter2 counter) {
      super(counter, "Counter");

      Environment env     = counter.getEnvironment();
      Guid        envId   = env.getId();

      JPanel      content = new JPanel(new BorderLayout());
      content.setBackground(Color.white);
      content.setBorder(BorderFactory.createEmptyBorder(0,3,3,3));

      JLabel label;

      // Create the top part of the window.
      JPanel top = new JPanel(new BorderLayout());
      top.setBackground(Color.white);
      label      = new
        JLabel("<html><font face=\"arial,helvetica,sans-serif\">" +
               envId.toString() + "</font></html>");
      label.setForeground(Color.black);
      top.add(label, BorderLayout.WEST);
      top.add(Box.createRigidArea(new Dimension(5, 0)), BorderLayout.CENTER);
      label   = GuiUtilities.createLocationSource(envId);
      GuiUtilities.addUserPopup(label, env);
      top.add(label, BorderLayout.EAST);
      content.add(top, BorderLayout.NORTH);

      // Create the part showing the current count.
      count = new JLabel(formatCount());
      count.setForeground(Color.black);
      count.setHorizontalAlignment(SwingConstants.CENTER);
      content.add(count, BorderLayout.CENTER);

      // Set the content.
      setContentPane(content);

      // Adjust the window size.
      if ((0 != counter.width) || (0 != counter.height)) {
        setSize(counter.width, counter.height);
      } else {
        pack();
      }
    }

    /** 
     * Format the current count.
     *
     * @return  A string representing the current count.
     */
    private String formatCount() {
      return "<html><font face=\"arial,helvetica,sans-serif\" size=\"7\">" +
        Long.toString(((Counter2)app).count) + "</font></html>";
    }

    /** Update the count. This method is thread-safe. */
    void updateCount() {
      if (SwingUtilities.isEventDispatchThread()) {
        updateCount1();
      } else {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
              updateCount1();
            }
          });
      }
    }

    /** Update the count. This method is not thread-safe. */
    void updateCount1() {
      count.setText(formatCount());
    }
   
  }


  // =======================================================================
  //                         The count update handler
  // =======================================================================

  /** Implementation of the count update handler. */
  final class CountUpdateHandler extends AbstractHandler {

    /** Handle the specified event. */
    protected boolean handle1(Event e) {
      if (! (e instanceof DynamicTuple)) {
        return false;
      }

      // We only update the count if the counter application is
      // actually running.
      Window window;

      synchronized (lock) {
        if (ACTIVE != status) {
          return true;
        }

        count++;
        window = (Window)mainWindow;
      }

      // Update the count.
      window.updateCount();

      // Done.
      return true;
    }
  }


  // =======================================================================
  //                         Instance fields
  // =======================================================================

  /**
   * The current count.
   *
   * @serial
   */
  long                         count;

  /**
   * The timer notification for updating the count.
   *
   * @serial
   */
  Timer.Notification           countUpdate;


  // =======================================================================
  //                           Constructor
  // =======================================================================

  /**
   * Create a new instance of <code>Counter2</code>.
   *
   * @param   env  The environment for the new instance.
   */
  public Counter2(Environment env) {
    super(env);
  }


  // =======================================================================
  //                           Serialization
  // =======================================================================

  /**
   * Serialize this counter component.
   *
   * @serialData  The default fields while holding the lock.
   */
  private void writeObject(ObjectOutputStream out) throws IOException {
    synchronized (lock) {
      out.defaultWriteObject();
    }
  }


  // =======================================================================
  //                           Start and stop
  // =======================================================================

  /** Acquire the resources needed by the counter application. */
  public void acquire() {
    synchronized (lock) {
      if (ACTIVATING != status) {
        return;
      }

      // Set up count update notification.
      if (null == countUpdate) {
        countUpdate = timer.schedule(Timer.FIXED_RATE,
                                     SystemUtilities.currentTimeMillis(),
                                     Duration.SECOND,
                                     new CountUpdateHandler(),
                                     new DynamicTuple());
      }
    }

    start();
  }

  /** Release the resources used by the counter application. */
  public void release() {
    // Cancel time update notifications.
    if (null != countUpdate) {
      countUpdate.cancel();
      countUpdate = null;
    }
  }


  // =======================================================================
  //                             Managing the UI
  // =======================================================================

  /** Create the counter's main window. */
  public Application.Window createMainWindow() {
    return new Window(this);
  }


  // =======================================================================
  //                            Initialization
  // =======================================================================

  /**
   * Initialize the counter.
   *
   * @param   env      The environment.
   * @param   closure  The closure, which is ignored.
   */
  public static void init(Environment env, Object closure) {
    Counter2 comp = new Counter2(env);
    env.link("main", "main", comp);
    comp.link("request", "request", env);
  }

}
