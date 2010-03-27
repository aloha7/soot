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

import java.awt.Dimension;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import one.gui.Application;
import one.gui.GuiUtilities;

import one.util.Guid;

import one.world.binding.Duration;

import one.world.core.Environment;
import one.world.core.Event;
import one.world.core.ExceptionalEvent;

import one.world.util.AbstractHandler;
import one.world.util.Operation;
import one.world.util.PingPongEvent;
import one.world.util.SystemUtilities;

/**
 * Implementation of a local ping. Ping repeatedly pings the root
 * environment using {@link PingPongEvent ping pong events} and
 * display the overall time consumed by pinging. It is useful for
 * determining the cost of event delivery in <i>one.world</i>.
 *
 * <p>Usage:<pre>
 *    Ping
 * </pre></p>
 *
 * @version  $Revision: 1.1 $
 * @author   Robert Grimm
 */
public final class Ping extends Application implements ActionListener {

  // =======================================================================
  //                           The main window
  // =======================================================================

  /** Implementation of ping's main window. */
  static final class Window extends Application.Window {

    /** The count text field. */
    final JTextField count;

    /** The duration label. */
    final JLabel     duration;

    /** The go button. */
    final JButton    go;

    /**
     * Create a new main window.
     *
     * @param  ping     The ping component.
     */
    Window(final Ping ping) {
      super(ping, "Ping");

      Environment env     = ping.getEnvironment();
      Guid        envId   = env.getId();

      JLabel      label;

      // Create the top part of the window.
      Box top = new Box(BoxLayout.X_AXIS);

      top.add(new JLabel("Ping the root environment"));
      top.add(Box.createRigidArea(new Dimension(5, 0)));
      top.add(Box.createHorizontalGlue());
      label   = GuiUtilities.createLocationSource(envId);
      GuiUtilities.addUserPopup(label, env);
      top.add(label);

      // Create the middle part.
      Box middle = new Box(BoxLayout.X_AXIS);
      count      = new JTextField(8);
      count.setHorizontalAlignment(JTextField.TRAILING);
      count.setText("100000");
      middle.add(count);
      middle.add(new JLabel(" pings took "));
      duration   = new JLabel("n/a");
      middle.add(duration);

      // Create the bottom part of the window.
      Box buttom = new Box(BoxLayout.X_AXIS);
      go = new JButton("Go!");
      go.addActionListener(ping);
      buttom.add(go);
      
      // Set up the content.
      JPanel content = new JPanel();
      content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
      content.setBorder(BorderFactory.createEmptyBorder(3,3,3,3));

      content.add(top);
      content.add(Box.createRigidArea(new Dimension(0, 5)));
      content.add(Box.createVerticalGlue());
      content.add(middle);
      content.add(Box.createRigidArea(new Dimension(0, 5)));
      content.add(buttom);

      setContentPane(content);

      // Pack the window.
      pack();
    }

    /**
     * Update the duration. This method is thread-safe.
     *
     * @param  duration  The new duration.
     */
    void updateDuration(final String duration) {
      if (SwingUtilities.isEventDispatchThread()) {
        updateDuration1(duration);
      } else {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
              updateDuration1(duration);
            }
          });
      }
    }

    /**
     * Update the duration. This method is not thread-safe.
     *
     * @param  duration  The new duration.
     */
    void updateDuration1(String duration) {
      this.duration.setText(duration);
      count.setEnabled(true);
      go.setEnabled(true);
      pack();
    }
   
  }


  // =======================================================================
  //                          The ping handler
  // =======================================================================

  /** The ping handler. */
  final class PingHandler extends AbstractHandler {

    /** Handle the specified event. */
    protected boolean handle1(Event e) {

      if (e instanceof PingPongEvent) {
        if (! ((PingPongEvent)e).pong) {
          return false;
        } else if (ACTIVE != status) {
          return true;
        }

        pings++;
        if (totalPings > pings) {
          // Next ping.
          operation.handle(new PingPongEvent(null, null, false));

        } else {
          // Determine the stop time.
          long stop = SystemUtilities.currentTimeMillis();
          
          // Update the duration.
          Window window;

          synchronized (lock) {
            if (ACTIVE != status) {
              return true;
            }

            window = (Window)mainWindow;
          }
          window.updateDuration(Duration.format(stop - start));
        }

        return true;

      } else if (e instanceof ExceptionalEvent) {
        // Tell the user.
        signalError("Error while pinging:\n" + ((ExceptionalEvent)e).x);

        // Update the duration.
        Window window;

        synchronized (lock) {
          if (ACTIVE != status) {
            return true;
          }

          window = (Window)mainWindow;
        }
        window.updateDuration("n/a");

        // Done.
        return true;
      }

      return false;
    }

  }


  // =======================================================================
  //                          Instance fields
  // =======================================================================

  /** The total number of pings to perform. */
  transient int  totalPings;

  /** The pings already performed. */
  transient int  pings;

  /** The start time. */
  transient long start;


  // =======================================================================
  //                           Constructor
  // =======================================================================

  /**
   * Create a new instance of <code>Ping</code>.
   *
   * @param   env  The environment for the new instance.
   */
  public Ping(Environment env) {
    super(env);
    operation = new Operation(timer, request, new PingHandler());
  }


  // =======================================================================
  //                             Managing the UI
  // =======================================================================

  /** Create the counter's main window. */
  public Application.Window createMainWindow() {
    return new Window(this);
  }

  /** Handle the specified action event. */
  public void actionPerformed(ActionEvent e) {
    // Get the main window.
    Window window;
    synchronized (lock) {
      if (ACTIVE != status) {
        return;
      }

      window = (Window)mainWindow;
    }

    // Parse the number.
    int num;
    try {
      num = Integer.parseInt(window.count.getText());
    } catch (NumberFormatException x) {
      GuiUtilities.beep();
      JOptionPane.showMessageDialog(window,
                                    "Illegal number format: " +
                                    window.count.getText(),
                                    "Ping Error",
                                    JOptionPane.ERROR_MESSAGE);
      return;
    }

    // Make sure the number is positive.
    if (0 >= num) {
      GuiUtilities.beep();
      JOptionPane.showMessageDialog(window,
                                    "Non-positive number: " +
                                    window.count.getText(),
                                    "Ping Error",
                                    JOptionPane.ERROR_MESSAGE);
      return;
    }

    // We are ready to rumble.
    window.count.setEnabled(false);
    window.go.setEnabled(false);

    totalPings = num;
    pings      = 0;
    start      = SystemUtilities.currentTimeMillis();
    operation.handle(new PingPongEvent(null, null, false));
  }


  // =======================================================================
  //                            Initialization
  // =======================================================================

  /**
   * Initialize ping.
   *
   * @param   env      The environment.
   * @param   closure  The closure, which is ignored.
   */
  public static void init(Environment env, Object closure) {
    Ping comp = new Ping(env);
    env.link("main", "main", comp);
    comp.link("request", "request", env);
  }

}
