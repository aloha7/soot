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
import java.awt.Container;
import java.awt.Dimension;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.text.DateFormat;

import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import one.gui.Application;
import one.gui.GuiUtilities;

import one.world.binding.Duration;

import one.world.core.DynamicTuple;
import one.world.core.Environment;
import one.world.core.Event;

import one.world.util.AbstractHandler;
import one.world.util.SystemUtilities;
import one.world.util.Timer;

/**
 * Implementation of a simple clock.
 *
 * <p>Usage:<pre>
 *    Clock [font-size [style]]
 * </pre>where the font size must be a number between 1 and 7
 * (inclusive) and the style must be either "short", "medium", "long",
 * or "full".</p>
 *
 * <p><b>Imported and Exported Event Handlers</b></p>
 *
 * <p>Exported event handlers:<dl>
 *    <dt>main</dt>
 *    <dd>Handles environment events, linked to an environment's
 *        main imported event handler.
 *        </dd>
 * </dl></p>
 *
 * <p>Imported event handlers:<dl>
 *    <dt>request</dt>
 *    <dd>Handles environment events and REP requests, linked to
 *        an environment's request exported event handler.
 *        </dd>
 * </dl></p>
 *
 * @version  $Revision: 1.4 $
 * @author   Robert Grimm
 */
public final class Clock extends Application {

  // =======================================================================
  //                           The main window
  // =======================================================================

  /** Implementation of the clock's main window. */
  static final class Window extends Application.Window {

    /** The date for holding the current time. */
    final Date   currentTime;
    
    /** The time label. */
    final JLabel time;

    /**
     * Create a new main window.
     *
     * @param  clock  The clock component.
     */
    Window(final Clock clock) {
      super(clock, "Clock");

      // Create the date.
      currentTime = new Date();

      // Set up the window elements.
      JPanel mainContent = new JPanel(new BorderLayout());
      mainContent.setBackground(Color.white);

      time = new JLabel(clock.format(currentTime));
      time.setForeground(Color.black);
      time.setHorizontalAlignment(SwingConstants.CENTER);

      Environment env = clock.getEnvironment();
      JLabel      loc = GuiUtilities.createLocationSource(env.getId());
      GuiUtilities.addUserPopup(loc, env);

      // Arrange the window elements.
      if (3 < clock.fontSize) {
        mainContent.setBorder(BorderFactory.createEmptyBorder(3,3,3,3));
        mainContent.add(time, BorderLayout.CENTER);
        Box box = new Box(BoxLayout.X_AXIS);
        box.add(Box.createHorizontalGlue());
        box.add(loc);
        mainContent.add(box, BorderLayout.NORTH);
      } else {
        Box box = new Box(BoxLayout.X_AXIS);
        box.add(Box.createRigidArea(new Dimension(3, 0)));
        box.add(time);
        mainContent.add(box, BorderLayout.WEST);
        box = new Box(BoxLayout.X_AXIS);
        box.add(Box.createRigidArea(new Dimension(5, 0)));
        box.add(loc);
        box.add(Box.createRigidArea(new Dimension(3, 0)));
        mainContent.add(box, BorderLayout.EAST);
      }
      setContentPane(mainContent);

      if ((0 != clock.width) || (0 != clock.height)) {
        setSize(clock.width, clock.height);
      } else {
        pack();
      }
    }

    /** Update the time. This method is thread-safe. */
    void updateTime() {
      if (SwingUtilities.isEventDispatchThread()) {
        updateTime1();
      } else {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
              updateTime1();
            }
          });
      }
    }

    /** Update the time. This method is not thread-safe. */
    void updateTime1() {
      currentTime.setTime(SystemUtilities.currentTimeMillis());
      time.setText(((Clock)app).format(currentTime));
    }
   
  }


  // =======================================================================
  //                         The time update handler
  // =======================================================================

  /** Implementation of the time update handler. */
  final class TimeUpdateHandler extends AbstractHandler {

    /** Handle the specified event. */
    protected boolean handle1(Event e) {
      if (! (e instanceof DynamicTuple)) {
        return false;
      }

      // We only update the time if the clock application is actually
      // running.
      Window window;

      synchronized (lock) {
        if (ACTIVE != status) {
          return true;
        }

        window = (Window)mainWindow;
      }

      // Update the time.
      window.updateTime();

      // Done.
      return true;
    }
  }


  // =======================================================================
  //                         Instance fields
  // =======================================================================

  /**
   * The style for formatting the time.
   *
   * @serial  Must be <code>DateFormat.SHORT</code>,
   *          <code>DateFormat.MEDIUM</code>, <code>DateFormat.LONG</code>,
   *          or <code>DateFormat.FULL</code>.
   */
  final     int                style;

  /**
   * The font size for the time.
   *
   * @serial
   */
  final     int                fontSize;

  /**
   * The timer notification for updating the time.
   *
   * @serial
   */
  Timer.Notification           timeUpdate;

  /** The date format used to format the time. */
  transient DateFormat         format;


  // =======================================================================
  //                           Constructor
  // =======================================================================

  /**
   * Create a new instance of <code>Clock</code>.
   *
   * @param   env       The environment for the new instance.
   * @param   style     The style for the time.
   * @param   fontSize  The font size for the time.
   * @throws  IllegalArgumentException
   *                    Signals that <code>style</code> is not a valid
   *                    style constant or that <code>fontSize</code>
   *                    is not a valid font size.
   */
  public Clock(Environment env, int style, int fontSize) {
    super(env);
    if ((1 > fontSize) || (7 < fontSize)) {
      throw new IllegalArgumentException("Invalid font size ("+fontSize+")");
    } else if ((DateFormat.SHORT  != style) &&
               (DateFormat.MEDIUM != style) &&
               (DateFormat.LONG   != style) &&
               (DateFormat.FULL   != style)) {
      throw new IllegalArgumentException("Invalid style constant ("+style+")");
    }
    this.style    = style;
    this.fontSize = fontSize;
    format        = DateFormat.getTimeInstance(style);
  }


  // =======================================================================
  //                           Serialization
  // =======================================================================

  /**
   * Serialize this clock component.
   *
   * @serialData  The default fields while holding the lock.
   */
  private void writeObject(ObjectOutputStream out) throws IOException {
    synchronized (lock) {
      out.defaultWriteObject();
    }
  }

  /** Deserialize a clock component. */
  private void readObject(ObjectInputStream in)
    throws IOException, ClassNotFoundException {

    // Read the non-transient fields.
    in.defaultReadObject();

    // Restore the locale-specific date format.
    format = DateFormat.getTimeInstance(style);
  }


  // =======================================================================
  //                           Start and stop
  // =======================================================================

  /** Acquire the resources needed by the clock application. */
  public void acquire() {
    synchronized (lock) {
      if (ACTIVATING != status) {
        return;
      }

      // Set up time update notification.
      if (null == timeUpdate) {
        timeUpdate = timer.schedule(Timer.FIXED_RATE,
                                    SystemUtilities.currentTimeMillis(),
                                    Duration.SECOND,
                                    new TimeUpdateHandler(),
                                    new DynamicTuple());
      }
    }

    start();
  }

  /** Release the resources used by the clock application. */
  public void release() {
    // Cancel time update notifications.
    if (null != timeUpdate) {
      timeUpdate.cancel();
      timeUpdate = null;
    }
  }


  // =======================================================================
  //                             Managing the UI
  // =======================================================================

  /** Create the clock's main window. */
  public Application.Window createMainWindow() {
    return new Window(this);
  }

  /**
   * Format the specified time using this clock's date format.
   *
   * @param   time  The time.
   * @return        A string representation.
   */
  String format(Date time) {
    return "<html><font face=\"arial,helvetica,sans-serif\" size=\"" +
      fontSize + "\">" + format.format(time) + "</font></html>";
  }


  // =======================================================================
  //                            Initialization
  // =======================================================================

  /**
   * Initialize the clock.
   *
   * @param   env      The environment.
   * @param   closure  The closure.
   * @throws  IllegalArgumentException
   *                   Signals that the closure is not a string array
   *                   or a string array with illegal arguments.
   */
  public static void init(Environment env, Object closure) {
    if (! (closure instanceof String[])) {
      throw new IllegalArgumentException("Closure not a string array");
    }

    String[] args     = (String[])closure;
    int      style    = DateFormat.SHORT;
    int      fontSize = 3;

    if (1 <= args.length) {
      try {
        fontSize = Integer.parseInt(args[0]);
      } catch (NumberFormatException x) {
        throw new IllegalArgumentException(x.getMessage());
      }
    }
    if (2 <= args.length) {
      if ("short".equals(args[1])) {
        style = DateFormat.SHORT;
      } else if ("medium".equals(args[1])) {
        style = DateFormat.MEDIUM;
      } else if ("long".equals(args[1])) {
        style = DateFormat.LONG;
      } else if ("full".equals(args[1])) {
        style = DateFormat.FULL;
      } else {
        throw new IllegalArgumentException("Unrecognized style ("+args[1]+")");
      }
    }

    Clock comp = new Clock(env, style, fontSize);
    env.link("main", "main", comp);
    comp.link("request", "request", env);
  }

}
