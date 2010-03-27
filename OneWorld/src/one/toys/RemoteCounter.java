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
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERemoteCounterHANTABILITY AND FITNESS
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
import java.awt.GridLayout;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.IOException;
import java.io.ObjectInputStream;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import one.gui.Application;
import one.gui.GuiUtilities;

import one.world.Constants;

import one.world.binding.BindingRequest;
import one.world.binding.BindingResponse;
import one.world.binding.Duration;
import one.world.binding.LeaseMaintainer;
import one.world.binding.UnknownResourceException;

import one.world.core.DynamicTuple;
import one.world.core.Environment;
import one.world.core.Event;
import one.world.core.EventHandler;
import one.world.core.ExceptionalEvent;
import one.world.core.UnknownEventException;

import one.world.rep.NamedResource;
import one.world.rep.RemoteDescriptor;
import one.world.rep.RemoteEvent;
import one.world.rep.RemoteReference;
import one.world.rep.ConnectionFailedException;

import one.world.util.AbstractHandler;
import one.world.util.Operation;
import one.world.util.SystemUtilities;
import one.world.util.Timer;

/**
 * Implementation of a resettable, synchronizable counter.
 *
 * <p>Usage:<pre>
 *    RemoteCounter
 * </pre></p>
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
 * @version  $Revision: 1.12 $
 * @author   Janet Davis
 */
public final class RemoteCounter extends Application {

  // =======================================================================
  //                           The main window
  // =======================================================================

  /** Implementation of the remote counter's main window. */
  static final class Window extends Application.Window {
    
    /** A label containing the current count. */
    JLabel countLabel;

    /** The text field for the remote host name. */
    JTextField hostField;

    /** The text field for the remote port number. */
    JTextField portField;

    /** The text field for the remote counter name. */
    JTextField nameField;

    /**
     * Create a new main window.
     *
     * @param  counter  The remote counter component.
     */
    Window(final RemoteCounter counter) {
      super(counter, "Counter");

      // The location source icon.
      Environment env = counter.getEnvironment();
      JLabel      loc = GuiUtilities.createLocationSource(env.getId());
      GuiUtilities.addUserPopup(loc, env);

      // A label with the name of the hosting environment.
      JLabel heading = new JLabel(counter.format(env.getName()));
      heading.setAlignmentY(java.awt.Component.CENTER_ALIGNMENT);

      // A label with the count.
      countLabel = new JLabel(counter.getFormattedCount());
      countLabel.setHorizontalAlignment(JLabel.CENTER);
      countLabel.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);

      // The input fields for synchronizing with another counter.
      int etf = GuiUtilities.ENTRY_TEXT_FIELD;
      JComponent[] synchComponents =
          GuiUtilities.createSimpleGrid(
	      new String[] {"Host", "Port", "Name"},
	      new int[] {etf, etf, etf},
	      -1);
	      
      JComponent synchInputs = synchComponents[0];
      hostField = (JTextField) synchComponents[1];
      portField = (JTextField) synchComponents[2];
      nameField = (JTextField) synchComponents[3];

      hostField.setText(counter.host);
      portField.setText(new Integer(counter.port).toString());
      nameField.setText(counter.name);

      // The synchronize button.
      JButton synchButton = new JButton("Synchronize");
      synchButton.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
      synchButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
	      synchronize();
	    }
          });
      
      // The reset button.
      JButton resetButton = new JButton("Reset count");
      resetButton.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
      resetButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
	      resetCount();
	    }
          });

      // Pack the window.
      JPanel mainContent = new JPanel(new BorderLayout());
      mainContent.setBorder(BorderFactory.createEmptyBorder(3,3,3,3));

      Box locBox = new Box(BoxLayout.X_AXIS);
      locBox.add(heading);
      locBox.add(Box.createHorizontalGlue());
      locBox.add(loc);

      Box buttonBox = new Box(BoxLayout.X_AXIS);
      buttonBox.add(resetButton);
      buttonBox.add(Box.createHorizontalStrut(10));
      buttonBox.add(synchButton);

      Box box = new Box(BoxLayout.Y_AXIS);
      box.add(locBox);
      box.add(Box.createVerticalStrut(10));
      box.add(countLabel);
      box.add(Box.createVerticalStrut(20));
      box.add(synchInputs);
      box.add(Box.createVerticalStrut(10));
      box.add(buttonBox);

      mainContent.add(box);
      setContentPane(mainContent);

      if ((0 != counter.width) || (0 != counter.height)) {
        setSize(counter.width, counter.height);
      } else {
        pack();
      }
    }

    /** Update the count label. This method is thread-safe. */
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

    /** Update the count label. This method is not thread-safe. */
    void updateCount1() {
      countLabel.setText(((RemoteCounter)app).getFormattedCount());
      countLabel.repaint();
    }

    /** Reset the count to 0. */
    void resetCount() {
      ((RemoteCounter)app).setCount(0);
    }
    
    /** 
     * Synchronize this counter with the user-specified remote counter.
     */
    void synchronize() {
      RemoteCounter counter = (RemoteCounter)app;
      try {
        counter.setRemoteCounter(hostField.getText(),
                                 Integer.parseInt(portField.getText()),
	                         nameField.getText());
        counter.synchronize();
      } catch (NumberFormatException x) {
        // Show an error message to the user.
	JOptionPane.showMessageDialog(this,
	              "Please enter an integer for the remote port number",
	              "RemoteCounter Runtime Error",
		      JOptionPane.ERROR_MESSAGE);
      }
    }
  }

  // =======================================================================
  //                         The count update handler
  // =======================================================================

  /** Implementation of the count update handler. */
  final class UpdateHandler extends AbstractHandler {

    /** Handle the specified event. */
    protected boolean handle1(Event e) {
      if (! (e instanceof DynamicTuple)) {
        return false;
      }

      Window window;
      synchronized (lock) {

        // We only update the count if the counter application is actually
        // running.
        if (ACTIVE != status) {
          return true;
        }

        // Update the count value.
	count++;

        // Grab a reference to the main window.
        window = (Window)mainWindow;
      }

      // Update the count label.
      window.updateCount();

      // Done.
      return true;
    }
  }


  // =======================================================================
  //                         The synchronization request handler
  // =======================================================================

  /** Implementation of the synchronization request handler. */
  final class SyncRequestHandler extends AbstractHandler {

    /** Handle the specified event. */
    protected boolean handle1(Event e) {
      if (e instanceof RemoteEvent) {
        RemoteEvent re = (RemoteEvent)e;

	// If the nested event has a field named "getCount",
	// reply with the actual count.  We'll respond using
	// AbstractHandler's response method rather than an operation,
	// leaving it up to the requestor to retry if our message doesn't
	// get there.
	if (re.event.hasField("getCount")) {
	  DynamicTuple response = new DynamicTuple(syncReference, null);
	  response.set("count", new Integer(count));
	  respond(request, re.closure, re.event, response);
	  return true;
	}
      } 
      return false;
    }
  }

  // =======================================================================
  //                         Instance fields
  // =======================================================================

  /** 
   * The count update handler. 
   *
   * @serial Must not be <code>null</code>.
   */
  protected final EventHandler updateHandler;

  /** 
   * The synchronization request handler.
   *
   * @serial Must not be <code>null</code>.
   */
  protected final EventHandler syncRequestHandler;

  /** The counter value. */
  protected int count = 0;

  /** The host name of the remote counter to synchronize with. */
  protected String host = "localhost";

  /** The port number of the remote counter to synchronize with. */
  protected int port = Constants.REP_PORT;

  /** The name of the remote counter to synchronize with. */
  protected String name = "counter";

  /** The count update timer notification. */
  protected Timer.Notification countUpdate;

  /** 
   * The remote reference for the exported synchronization request
   * handler.  This is transient because it is the result of a local
   * binding.
   */
  protected transient RemoteReference syncReference;

  /** 
   * A lease maintainer for the exported synchronization request handler.   
   * This is transient because it contains state related to a local 
   * binding.
   */
  protected transient LeaseMaintainer leaseMaintainer;

  // =======================================================================
  //                           Constructor
  // =======================================================================

  /**
   * Create a new instance of <code>RemoteCounter</code>.
   *
   * @param   env       The environment for the new instance.
   */
  public RemoteCounter(Environment env) {
    super(env);

    updateHandler = new UpdateHandler();
    syncRequestHandler = new SyncRequestHandler();
  }

  // =======================================================================
  //                           Start and stop
  // =======================================================================

  /** Acquire the resources needed by the remote counter application. */
  public void acquire() {
    synchronized (lock) {
      if (INACTIVE == status) {
        return;
      }

      // Set up the timed count update notification.
      setUpdateTimer();
    }

    // We will export the synchronization request handler under this
    // environment's name on the local machine.
    RemoteDescriptor descriptor = 
        new RemoteDescriptor(syncRequestHandler,
	                     getEnvironment().getName());
 
    // This is the event to send to establish the binding.
    BindingRequest bindingRequest = 
        new BindingRequest(null, null, descriptor, Duration.FOREVER);

    // Using an operation, attempt to establish the binding.
    Operation op = 
        new Operation(timer, request, new AbstractHandler() {
	      protected boolean handle1(Event e) {
	      
	        if (e instanceof BindingResponse) {
		  // The binding succeeded.
		  BindingResponse response = (BindingResponse)e;

		  // Hang on to the resource.
		  syncReference = (RemoteReference)response.resource;

		  // Maintain the binding.
		  leaseMaintainer = 
		      new LeaseMaintainer(response.lease,
		                          response.duration,
					  syncRequestHandler,
					  null, 
					  timer);
					 
		  // Start the application.
                  start();

		  // All done.
		  return true;
		} 

		// If we didn't get a binding response, the binding
		// failed.
		Throwable x;
		if (e instanceof ExceptionalEvent) {
		  x = ((ExceptionalEvent)e).x;
		} else {
		  x = new UnknownEventException(e.getClass().getName());
		}
	        JOptionPane.showMessageDialog(mainWindow,
		      "Unable to start RemoteCounter:\n" + x,
	              "RemoteCounter Startup Error",
		      JOptionPane.ERROR_MESSAGE);
		stop(true);
		return true;
	      }
            });
	    
    // Start the operation.
    op.handle(bindingRequest);
  }

  /** Release the resources used by the remote counter application. */
  public void release() {
    cancelUpdateTimer();
    if (leaseMaintainer != null) {
      leaseMaintainer.cancel();
      leaseMaintainer = null;
   }
  }

  /** Set up the timed count update notification. */
  public void setUpdateTimer() {
    synchronized (lock) {
      // Schedule the notification for every second, beginning one second
      // from now, if there isn't already an active timer notification.
      if (countUpdate == null) {
        countUpdate = timer.schedule(Timer.FIXED_RATE,
                                     SystemUtilities.currentTimeMillis()
		  		       + Duration.SECOND,
                                     Duration.SECOND,
                                     updateHandler,
                                     new DynamicTuple());

      }
    }
  }

  /** Cancel the count update notifications. */
  public void cancelUpdateTimer() {
    synchronized (lock) {
      if (null != countUpdate) {
        countUpdate.cancel();
	countUpdate = null;
      }
    }
  }

  // =======================================================================
  //                             Managing the UI
  // =======================================================================

  /** Create the remote counter's main window. */
  public Application.Window createMainWindow() {
    return new Window(this);
  }

  /**
   * Format the specified number.
   *
   * @param  l   The number to format.
   * @return     The formatted message.
   */
  String format(long l) {
    return "<html><font face=\"arial,helvetica,sans-serif\""
          + "color=\"black\" size=\"+4\">" + Long.toString(l) + "</font>";
  }

  /**
   * Format the specified message.
   *
   * @param  m   The message to format.
   * @return     The formatted message.
   */
  String format(String m) {
    return "<html><font face=\"arial,helvetica,sans-serif\""
          + "color=\"black\">" + m + "</font>";
  }

  /**
   * Gets the current count, formatted for display. 
   *
   * @param The formatted count.
   */
  String getFormattedCount() {
    return format(count);
  }

  // =======================================================================
  //                             Managing the data
  // =======================================================================

  /** 
   * Sets the count to the specified value.
   *
   * @param count  The new count value.
   */
  public void setCount(int newCount) {
    Window window;

    synchronized (lock) {

      // Don't do anything unless the application is active.
      if (ACTIVE != status) {
        return;
      }

      // Set the count value.
      count = newCount;

      // Reschedule the timer, so it will remain at the new value for a
      // full second.
      cancelUpdateTimer();
      setUpdateTimer();

      // Grab a reference to the main window.
      window = (Window)mainWindow;
    }

    // Update the count label in the main window.
    window.updateCount();
  }

  /** 
   * Sets the {@link #host}, {@link #port}, and {@link #name} fields.
   *
   * @param host  The new remote host name.
   * @param port  The new remote port number.
   * @param name  The new remote counter name.
   */
  public void setRemoteCounter(String host, int port, String name) {
    synchronized (lock) {
      this.host = host;
      this.port = port;
      this.name = name;
    }
  }

  /**
   * Synchronizes this counter with the remote counter given by the
   * {@link #host}, {@link #port}, and {@link #name} fields.
   */
  public void synchronize() {

    // Disable the main window.
    mainWindow.setEnabled(false);

    // Create an operation with a short timeout and only one retry. 
    // (Users are impatient.)
    final Operation op = new Operation(1, 5*Duration.SECOND, 
                                       timer, request, null);
    
    // The operation will need to start by exporting its response handler
    // as an anonymous remote resource, to obtain a remote reference.
    Event bindingRequest = 
        new BindingRequest(null, null, 
	                   new RemoteDescriptor(op.getResponseHandler()),
			   Duration.MINUTE);
  
    // Set the operation's contintuation.
    op.continuation = new AbstractHandler() {
          protected boolean handle1(Event e) {
	    if (e instanceof BindingResponse) {
	      BindingResponse response = (BindingResponse)e;

	      // A remote reference for this operation.
	      RemoteReference ref = 
	          (RemoteReference)response.resource;

              // The remote counter resource.
	      NamedResource remote =
	          new NamedResource(host, port, name);
 
              // The event we will send to the remote counter.
	      DynamicTuple dt = new DynamicTuple(ref, null);
	      dt.set("getCount", Boolean.TRUE);
              
	      // Send the value request.
	      op.handle(new RemoteEvent(null, null, remote, dt));

	      // Done for now.
	      return true;
	                   
	    } else if (e instanceof RemoteEvent) {
	      RemoteEvent re = (RemoteEvent)e;

	      // If the nested event has an integer field named "count",
	      // that is the new count value.
	      Object o = re.event.get("count");
	      if (o instanceof Integer) {
	        setCount(((Integer)o).intValue());

                // Reenable the main window.
                mainWindow.setEnabled(true);
		return true;
	      }

	    } else if (e instanceof ExceptionalEvent) {
	      ExceptionalEvent xe = (ExceptionalEvent)e;
	      JOptionPane.showMessageDialog(mainWindow,
                  "Unable to synchronize with " + host + ":" + port + "/"
		      + name + ":\n" + xe.x,
		  "RemoteCounter Runtime Error",
		   JOptionPane.ERROR_MESSAGE);
              // Reenable the main window.
              mainWindow.setEnabled(true);
	      return true;
	    }
	    return false;
	  }
      };

    // Start the operation.
    op.handle(bindingRequest);
  }

  // =======================================================================
  //                            Initialization
  // =======================================================================

  /**
   * Initialize the remote counter..
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

    RemoteCounter comp = new RemoteCounter(env);
    env.link("main", "main", comp);
    comp.link("request", "request", env);
  }
}
