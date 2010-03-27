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

package one.radio;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Rectangle;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.lang.reflect.InvocationTargetException;

import java.text.Collator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.Scrollable;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import one.gui.Application;
import one.gui.EnvironmentDropEvent;
import one.gui.GuiUtilities;

import one.util.BufferOutputStream;
import one.util.Bug;
import one.util.Digest;
import one.util.Guid;

import one.world.Constants;

import one.world.binding.BindingRequest;
import one.world.binding.BindingResponse;
import one.world.binding.Duration;
import one.world.binding.LeaseMaintainer;

import one.world.core.Component;
import one.world.core.ComponentDescriptor;
import one.world.core.DynamicTuple;
import one.world.core.Environment;
import one.world.core.Event;
import one.world.core.EventHandler;
import one.world.core.ExceptionalEvent;
import one.world.core.ExportedDescriptor;
import one.world.core.ImportedDescriptor;
import one.world.core.InvalidTupleException;
import one.world.core.SymbolicHandler;

import one.world.data.Name;

import one.world.env.CheckPointResponse;
import one.world.env.CreateRequest;
import one.world.env.EnvironmentEvent;
import one.world.env.MoveRequest;
import one.world.env.RestoreRequest;

import one.world.io.InputResponse;
import one.world.io.OutputResponse;
import one.world.io.Query;
import one.world.io.SimpleInputRequest;
import one.world.io.SimpleOutputRequest;
import one.world.io.SioResource;

import one.world.rep.DiscoveredResource;
import one.world.rep.NamedResource;
import one.world.rep.RemoteDescriptor;
import one.world.rep.RemoteEvent;
import one.world.rep.RemoteReference;

import one.world.util.AbstractHandler;
import one.world.util.NullHandler;
import one.world.util.Operation;
import one.world.util.SystemUtilities;
import one.world.util.Timer;

/**
 * Implementation of the <i>one.radio</i> emcee. Emcee provides a
 * simple user interface for managing users and their applications.
 * It supports pushing or pulling all of a user's applications to/from
 * some machine, managing a user's local applications, and exchanging
 * applications between users. Emcee's per-user functionality is
 * accessed by activating a user's popup menu (typically, by
 * right-clicking on the user's name). Applications are moved or
 * copied between users by (copy) dragging an application's flag icon
 * (<img src="flag.gif" alt="Flag icon" border="0" height="15"
 * width="15" />) and dropping it onto a user's name.
 *
 * <p>Emcee relies on the following layout of the environment
 * hierarchy.  The emcee application itself must run in the
 * "<code>/User</code>" environment. Each child environment of the
 * user environment is treated as a user's root environment, with the
 * user having the same name as that environment. For example,
 * "<code>/User/rgrimm</code>" is the root environment for user
 * rgrimm. Each user application, in turn, runs in a direct child
 * environment of the user's root environment. For example,
 * "<code>/User/rgrimm/Chat</code>" may contain rgrimm's {@link Chat
 * chat} application.</p>
 *
 * <p>The "<code>/User/local</code>" environment is treated
 * differently in that it hosts local applications, which are shared
 * between users.  The local environment is shown as "&lt;Local&gt;" in
 * emcee's window and cannot be moved between machines.</p>
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
 * @version  $Revision: 1.8 $
 * @author   Robert Grimm 
 */
public final class Emcee extends Application
  implements FetcherProtocol, ActionListener {

  // =======================================================================
  //                              Constants
  // =======================================================================

  /** The initial width for the emcee's main window. */
  static final int      WINDOW_WIDTH             = 130;

  /** The initial height for the emcee's main window. */
  static final int      WINDOW_HEIGHT            = 120;

  /** The field width for text fields. */
  static final int      FIELD_WIDTH              = 20;

  /** The name of the environment for local applications. */
  static final String   LOCAL                    = "local";

  /** The display name of the environment for local applications. */
  static final String   DISPLAY_LOCAL            = "<Local>";

  /** The create action command. */
  static final String   ACTION_CREATE            = "create";

  /** The fetch action command. */
  static final String   ACTION_FETCH             = "fetch";

  /** The initialize action command. */
  static final String   ACTION_INITIALIZE        = "initialize";

  /** The run action command. */
  static final String   ACTION_RUN               = "run";

  /** The delete environment action command. */
  static final String   ACTION_DELETE_ENV        = "delete environment";

  /** The checkpoint action command. */
  static final String   ACTION_CHECKPOINT        = "checkpoint";

  /** The restore action command. */
  static final String   ACTION_RESTORE           = "restore";

  /** The restore latest action command. */
  static final String   ACTION_RESTORE_LATEST    = "restore latest";

  /** The move action command. */
  static final String   ACTION_MOVE              = "move";

  /** The delete user action command. */
  static final String   ACTION_DELETE_USER       = "delete user";

  /** The change password action command. */
  static final String   ACTION_CHANGE_PASSWORD   = "change password";

  /** The move application action command. */
  static final String   ACTION_MOVE_APPLICATION  = "move application";

  /** The copy application action command. */
  static final String   ACTION_COPY_APPLICATION  = "copy application";

  /** The query for a user's user preferences tuple. */
  static final Query    QUERY_USER_PREFERENCES   = new
    Query("id", Query.COMPARE_EQUAL, UserPreferences.DEFAULT_ID);

  /** The closure for getting started on the fetcher protocol. */
  static final Guid    FETCHER_CLOSURE           = new Guid();

  /** The closure for completing the fetcher protocol. */
  static final Guid    FETCHEE_CLOSURE           = new Guid();

  /** The closure for lease maintainer error messages. */
  static final Guid     LEASE_MAINTAINER_CLOSURE = new Guid();

  /** The collator used for sorting strings. */
  static final Collator COLLATOR                 = Collator.getInstance();

  /** The class for a byte array. */
  static final Class    TYPE_BYTE_ARRAY;

  static {
    try {
      TYPE_BYTE_ARRAY = Class.forName("[B");
    } catch (ClassNotFoundException x) {
      throw new Bug("Unexpected exception (" + x + ")");
    }
  }


  // =======================================================================
  //                           The main window
  // =======================================================================

  /** Implementation of the emcee's main window. */
  static final class Window extends Application.Window {

    /** The fetch menu item. */
    final JMenuItem   fetch;

    /** The user panel. */
    final UserPanel   userPanel;
    
    /** The scroll pane for the user panel. */
    final JScrollPane scroll;

    /**
     * Create a new main window.
     *
     * @param  emcee  The emcee component.
     */
    Window(final Emcee emcee) {
      super(emcee, "Emcee");

      // Create the menu bar.
      JMenuBar mbar = new JMenuBar();
      setJMenuBar(mbar);

      JMenu menu = new JMenu("User");
      menu.setMnemonic(KeyEvent.VK_U);
      mbar.add(menu);
      
      JMenuItem mitem = new JMenuItem("Create...");
      mitem.setActionCommand(ACTION_CREATE);
      mitem.addActionListener(emcee);
      mitem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C,
                                                  ActionEvent.ALT_MASK));
      menu.add(mitem);

      fetch = new JMenuItem("Fetch...");
      fetch.setActionCommand(ACTION_FETCH);
      fetch.addActionListener(emcee);
      fetch.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F,
                                                  ActionEvent.ALT_MASK));
      menu.add(fetch);

      // Create the user panel.
      userPanel  = new UserPanel();
      userPanel.setLayout(new GridLayout(0, 1));
      scroll = new JScrollPane(userPanel);
      getContentPane().add(scroll);
      scroll.validate();
    }

    /**
     * Add the specified user to the user panel. Calling this method
     * is thread-safe.
     *
     * @param  user  The user to add.
     */
    void add(final User user) {
      if (SwingUtilities.isEventDispatchThread()) {
        add1(user);
        scroll.validate();
        scroll.repaint();
      } else {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
              add1(user);
              scroll.validate();
              scroll.repaint();
            }
          });
      }
    }

    /**
     * Add the specified user to the user panel. This method performs
     * the actual addition while maintaining a sorted order of all
     * users appearing in the panel. It is not thread-safe and does
     * not revalidate the panel.
     *
     * @param  user  The user to add.
     */
    void add1(User user) {
      if (user.local) {
        userPanel.add(user.label, 0);
      } else {
        int l = userPanel.getComponentCount();

        for (int i=0; i<l; i++) {
          String other = ((JLabel)userPanel.getComponent(i)).getText();

          if (COLLATOR.compare(user.name, other) < 0) {
            userPanel.add(user.label, i);
            return;
          }
        }

        userPanel.add(user.label);
      }
    }

    /**
     * Remove the specified user from the user panel. Calling this
     * method is thread-safe.
     *
     * @param  user  The user to remove.
     */
    void remove(final User user) {
      if (SwingUtilities.isEventDispatchThread()) {
        userPanel.remove(user.label);
        scroll.validate();
        scroll.repaint();
      } else {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
              userPanel.remove(user.label);
              scroll.validate();
              scroll.repaint();
            }
          });
      }
    }

    /**
     * Enable/disable the fetch menu item. This method is thread-safe.
     *
     * @param  b  The flag for whether to enable or disable the
     *            fetch menu item.
     *
     */
    void setFetchEnabled(final boolean b) {
      if (SwingUtilities.isEventDispatchThread()) {
        fetch.setEnabled(b);
      } else {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
              fetch.setEnabled(b);
            }
          });
      }
    }

  }


  // =======================================================================
  //                               The user panel
  // =======================================================================

  /**
   * Implementation of a user panel. A user panel is a
   * <code>JPanel</code> that also implements the
   * <code>Scrollable</code> interface, so that the scrolling behavior
   * is smoother than if it was just a panel.
   */
  static final class UserPanel extends JPanel implements Scrollable {

    /** Create a new user panel. */
    UserPanel() {
      setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
    }
    
    /** Get the preferred viewport size. */
    public Dimension getPreferredScrollableViewportSize() {
      return getPreferredSize();
    }

    /** Get the unit increment. */
    public int getScrollableUnitIncrement(Rectangle rect, int o, int d) {
      if (SwingConstants.HORIZONTAL == o) {
        Font f = getFont();
        return (f != null) ? f.getSize() : 1;
      } else {
        if (0 == getComponentCount()) {
          return 0;
        } else {
          return getComponent(0).getHeight();
        }
      }
    }

    /** Get the block increment. */
    public int getScrollableBlockIncrement(Rectangle rect, int o, int d) {
      return ((SwingConstants.VERTICAL == o) ? rect.height : rect.width);
    }

    /** Determine whether the panel tracks the viewport width. */
    public boolean getScrollableTracksViewportWidth() {
      if (getParent() instanceof JViewport) {
        return (((JViewport)getParent()).getWidth() > getPreferredSize().width);
      }
      return false;
    }

    /** Determine whether the panel tracks the viewport height. */
    public boolean getScrollableTracksViewportHeight() {
      if (getParent() instanceof JViewport) {
        return (((JViewport)getParent()).getHeight()>getPreferredSize().height);
      }
      return false;
    }
  }


  // =======================================================================
  //                           The user manager
  // =======================================================================

  /**
   * Implementation of the user manager. The user manager provides the
   * UI and logic for managing a single user.
   */
  final class User extends AbstractHandler
    implements ActionListener, MouseListener {

    /**
     * The flag for whether this user record represents the local
     * environment.
     */
    final boolean     local;

    /** The user name. */
    final String      name;

    /** The user environment. */
    Environment       env;

    /** The user password. */
    byte[]            password;

    /** The new user password when changing it. */
    byte[]            newPassword;

    /** The current action for the user. */
    String            action;

    /** The label for the main window. */
    JLabel            label;

    /** The popup menu for the user. */
    JPopupMenu        popup;

    /** The list of applications. */
    List              apps;

    /** The run menu item. */
    JMenuItem         run;

    /** The delete environment menu item. */
    JMenuItem         delete;

    /** The checkpoint menu item. */
    JMenuItem         checkpoint;

    /** The restore menu item. */
    JMenuItem         restore;

    /** The restore latest menu item. */
    JMenuItem         restoreLatest;

    /** The move menu item. */
    JMenuItem         move;

    /** The delete user menu item. */
    JMenuItem         deleteUser;

    /** The change password menu item. */
    JMenuItem         changePassword;

    /** The user's operation. */
    Operation         operation2;

    /** The lease maintainer. */
    LeaseMaintainer   lm2;

    /** The lease maintainer for exporting this user manager locally. */
    LeaseMaintainer   lmLocal;

    /**
     * The lease maintainer for exporting this user manager through
     * discovery.
     */
    LeaseMaintainer   lmDiscovery;

    /**
     * The operation for processing the fetchee side of the fetcher
     * protocol.
     */
    Operation         fetcheeOperation;

    /** The exported response handler. */
    RemoteReference   fetcheeResponseHandler;

    /** The lease maintainer for exporting the response handler. */
    LeaseMaintainer   fetcheeLM;

    /** The source to reply to in the fetcher protocol. */
    SymbolicHandler   fetcheeSource;

    /** The closure in the fetcher protocol. */
    Object            fetcheeClosure;

    /** The challenge in the fetcher protocol. */
    byte[]            fetcheeChallenge;
    
    /**
     * Create a new user record for the user with the specified name.
     *
     * @param   name  The user name.
     * @param   env   The local root environment.
     */
    User(String name, Environment env) {
      local     = LOCAL.equals(name);
      this.name = name;
      this.env  = env;
      action    = (local? null : ACTION_INITIALIZE);
    }

    /** User managers are not serializable. Sorry. */
    private void writeObject(ObjectOutputStream out) throws IOException {
      throw new NotSerializableException(getClass().getName());
    }

    /** Fill in this user's record. */
    void fill() {
      // Create the label for the user.
      if (local) {
        label = new JLabel(DISPLAY_LOCAL);
      } else {
        label = new JLabel(name);
      }
      GuiUtilities.turnIntoEnvironmentTarget(label, this);
      label.addMouseListener(this);
      
      // Create the popup menu.
      popup            = new JPopupMenu();

      run              = new JMenuItem("Run...");
      run.setEnabled(local);
      run.setActionCommand(ACTION_RUN);
      run.addActionListener(this);
      popup.add(run);

      popup.add(new JLabel());

      delete           = new JMenuItem("Delete Environment...");
      delete.setEnabled(local);
      delete.setActionCommand(ACTION_DELETE_ENV);
      delete.addActionListener(this);
      popup.add(delete);

      popup.addSeparator();

      checkpoint       = new JMenuItem("Checkpoint");
      checkpoint.setEnabled(local);
      checkpoint.setActionCommand(ACTION_CHECKPOINT);
      checkpoint.addActionListener(this);
      popup.add(checkpoint);

      restore          = new JMenuItem("Restore...");
      //restore.setEnabled(local);
      restore.setEnabled(false);
      restore.setActionCommand(ACTION_RESTORE);
      restore.addActionListener(this);
      popup.add(restore);

      restoreLatest    = new JMenuItem("Restore Latest");
      restoreLatest.setEnabled(local);
      restoreLatest.setActionCommand(ACTION_RESTORE_LATEST);
      restoreLatest.addActionListener(this);
      popup.add(restoreLatest);

      if (! local) {
        popup.addSeparator();
        move = new JMenuItem("Move...");
        move.setEnabled(false);
        move.setActionCommand(ACTION_MOVE);
        move.addActionListener(this);
        popup.add(move);

        deleteUser = new JMenuItem("Delete...");
        deleteUser.setEnabled(false);
        deleteUser.setActionCommand(ACTION_DELETE_USER);
        deleteUser.addActionListener(this);
        popup.add(deleteUser);

        popup.addSeparator();
        changePassword = new JMenuItem("Change Password...");
        changePassword.setEnabled(false);
        changePassword.setActionCommand(ACTION_CHANGE_PASSWORD);
        changePassword.addActionListener(this);
        popup.add(changePassword);
      }

    }

    /**
     * Enable/disable the popup menu actions. This method is
     * thread-safe even if the popup menu is currently displayed.
     *
     * @param  b  The flag for whether to enable or disable the
     *            actions.
     */
    void setEnabled(final boolean b) {
      if (SwingUtilities.isEventDispatchThread()) {
        setEnabled1(b);
      } else {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
              setEnabled1(b);
            }
          });
      }
    }

    /**
     * Enable/disable the popup menu actions. This method is
     * not thread-safe.
     *
     * @param  b  The flag for whether to enable or disable the
     *            actions.
     */
    void setEnabled1(boolean b) {
      run.setEnabled(b);
      delete.setEnabled(b);
      checkpoint.setEnabled(b);
      //restore.setEnabled(b);
      restoreLatest.setEnabled(b);
      if (! local) {
        move.setEnabled(b);
        deleteUser.setEnabled(b);
        changePassword.setEnabled(b);
      }
    }

    /**
     * Release all resources held by this user manager. This method
     * must be called while holding the application's lock.
     */
    void release() {
      if (null != lm2) {
        lm2.cancel();
        lm2        = null;
        operation2 = null;
      }
      if (null != lmLocal) {
        lmLocal.cancel();
        lmLocal = null;
      }
      if (null != lmDiscovery) {
        lmDiscovery.cancel();
        lmDiscovery = null;
      }
      if (null != fetcheeLM) {
        fetcheeLM.cancel();
        fetcheeLM              = null;
        fetcheeOperation       = null;
        fetcheeResponseHandler = null;
        fetcheeSource          = null;
        fetcheeClosure         = null;
        fetcheeChallenge       = null;
      }
    }

    // ======================= Mouse event handling =======================

    /** Handle the mouse clicked event. */
    public void mouseClicked(MouseEvent e) {
    }

    /** Handle the mouse entered event. */
    public void mouseEntered(MouseEvent e) {
    }

    /** Handle the mouse exited event. */
    public void mouseExited(MouseEvent e) {
    }

    /** Handle the mouse pressed event. */
    public void mousePressed(MouseEvent e) {
      maybeShowPopup(e);
    }

    /** Handle the mouse release event. */
    public void mouseReleased(MouseEvent e) {
      maybeShowPopup(e);
    }

    /** Show the popup menu if the mouse event was a trigger. */
    private void maybeShowPopup(MouseEvent e) {
      if (e.isPopupTrigger()) {
        // Get the list of child environments.
        List children;
        try {
          children = env.getChildren();
        } catch (IllegalStateException x) {
          return;
        }

        if (! children.equals(apps)) {
          apps = children;

          // Sort the list.
          Collections.sort(children, COLLATOR);

          // Create the environments submenu.
          JMenu menu = new JMenu("Environments");
          if (children.isEmpty()) {
            menu.add(new JMenuItem("<none>"));
          } else {
            Iterator iter = children.iterator();
            while (iter.hasNext()) {
              menu.add(new JMenuItem((String)iter.next()));
            }
          }

          // Add the submenu to the popup menu.
          popup.remove(1);
          popup.add(menu, 1);
        }

        // Make sure that the delete environment item is only
        // enabled if there are environments to delete.
        if (apps.isEmpty()) {
          delete.setEnabled(false);
        } else if (run.isEnabled()) {
          delete.setEnabled(true);
        }
          
        // Show the popup menu.
        popup.show(e.getComponent(), e.getX(), e.getY());
      }
    }

    // ======================= Action event handling ======================

    /** Handle the action event. */
    public void actionPerformed(ActionEvent e) {
      String cmd = e.getActionCommand();

      // Make sure that the emcee is active.
      synchronized (lock) {
        if (ACTIVE != status) {
          return;
        }
      }

      // Make sure we are not already within an action.
      if (null != action) {
        return;
      }

      // Handle the action event.
      if (ACTION_RUN.equals(cmd)) {
        action = ACTION_RUN;
        setEnabled(false);

        new RunDialog(this).show();

      } else if (ACTION_DELETE_ENV.equals(cmd)) {
        action = ACTION_DELETE_ENV;
        setEnabled(false);

        new DeleteEnvDialog(this, apps).show();

      } else if (ACTION_CHECKPOINT.equals(cmd)) {
        action = ACTION_CHECKPOINT;
        setEnabled(false);

        operation.handle(new
          EnvironmentEvent(null, this, EnvironmentEvent.CHECK_POINT,
                           env.getId()));

      } else if (ACTION_RESTORE.equals(cmd)) {
        // Not implemented yet.

      } else if (ACTION_RESTORE_LATEST.equals(cmd)) {
        action = ACTION_RESTORE_LATEST;
        setEnabled(false);

        operation.handle(new
          RestoreRequest(null, this, env.getId(), -1));

      } else if (ACTION_MOVE.equals(cmd)) {
        action = ACTION_MOVE;
        setEnabled(false);

        new MoveDialog(this).show();

      } else if (ACTION_DELETE_USER.equals(cmd)) {
        action = ACTION_DELETE_USER;
        setEnabled(false);

        new DeleteUserDialog(this).show();

      } else if (ACTION_CHANGE_PASSWORD.equals(cmd)) {
        action = ACTION_CHANGE_PASSWORD;
        setEnabled(false);

        new ChangePasswordDialog(this).show();
      }
    }

    // ===================== one.world event handling =====================

    /** Handle the specified one.world event. */
    protected boolean handle1(Event e) {
      if (e instanceof EnvironmentDropEvent) {
        EnvironmentDropEvent ede = (EnvironmentDropEvent)e;

        if (null != action) {
          return true;
        }
        action = (ede.copy ? ACTION_COPY_APPLICATION : ACTION_MOVE_APPLICATION);
        setEnabled(false);

        operation.handle(new
          MoveRequest(null, this, ede.ident, "sio:///"+env.getId(), ede.copy));
        return true;

      } else if ((e instanceof ExceptionalEvent) &&
                 (LEASE_MAINTAINER_CLOSURE.equals(e.closure))) {

        signalError("Internal error in lease maintainer for " +
                    (local? "user " + name : "local applications") +
                    ":\n" + ((ExceptionalEvent)e).x);
        return true;

      }

      // The user manager serves as the fetchee in the fetcher
      // protocol. This event handler starts the protocol and, from
      // here on, we only handle remote events carrying the
      // appropriate dynamic tuple.
      if (! (e instanceof RemoteEvent)) {
        return false;
      }
      e = ((RemoteEvent)e).event;
      if (! (e instanceof DynamicTuple)) {
        return false;
      }

      DynamicTuple    dt      = (DynamicTuple)e;
      SymbolicHandler source  = (SymbolicHandler)dt.source;
      Object          closure = dt.closure;

      // Extract the message.
      String msg;
      try {
        msg = (String)dt.get(MSG, String.class, false);
      } catch (IllegalArgumentException x) {
        request.handle(new
          RemoteEvent(this, closure, source, new
            ExceptionalEvent(NullHandler.NULL, closure, new
              IllegalArgumentException("No message field for fetcher " +
                                       "protocol message " + dt))));
        return true;
      }
      if (! MSG_COME.equals(msg)) {
        request.handle(new
          RemoteEvent(this, closure, source, new
            ExceptionalEvent(NullHandler.NULL, closure, new
              IllegalArgumentException("Unrecognized fetcher protocol " +
                                       "message " + msg))));
        return true;
      }

      // Are we busy?
      boolean busy = false;
      synchronized (lock) {
        if (null != fetcheeOperation) {
          busy = true;
        } else {
          fetcheeOperation = new
            Operation(0, Constants.OPERATION_TIMEOUT, timer, request, new
              ProtocolHandler());
        }
      }

      if (busy) {
        request.handle(new
          RemoteEvent(this, closure, source, new
            ExceptionalEvent(NullHandler.NULL, closure, new
              IllegalStateException(name + " already being fetched"))));
        return true;
      }

      // Get started with the actual fetcher protocol by exporting the
      // fetchee operation's reponse handler.
      fetcheeSource  = source;
      fetcheeClosure = closure;
      fetcheeOperation.handle(new
        BindingRequest(null, null, new
          RemoteDescriptor(fetcheeOperation.getResponseHandler()),
                       Duration.FOREVER));
      return true;
    }

    /** Implementation of the actual fetcher protocol handler. */
    final class ProtocolHandler extends AbstractHandler {

      /** Handle the specified event. */
      protected boolean handle1(Event e) {
        
        if (e instanceof BindingResponse) {
          BindingResponse br = (BindingResponse)e;
          
          // We exported the resonse handler. Let's make sure it stays
          // that way.
          synchronized (lock) {
            if (ACTIVE != status) {
              LeaseMaintainer.cancel(br.lease);
              return true;
            }
            fetcheeLM = new LeaseMaintainer(br.lease, br.duration, User.this,
                                            LEASE_MAINTAINER_CLOSURE, timer);
          }
          fetcheeResponseHandler = (RemoteReference)br.resource;

          // Create the challenge.
          byte[]             addr   = SystemUtilities.rawIpAddress();
          int                length = 24 + addr.length;
          BufferOutputStream bytes  = new BufferOutputStream(length);
          DataOutputStream   out    = new DataOutputStream(bytes);
          
          try {
            // The current time provides a monotonically increasing
            // value (we hope), the uptime (with the current time) and
            // the IP address provide a node-identifying value, and the
            // amount of free memory provides some randomization.
            out.writeLong(SystemUtilities.currentTimeMillis());
            out.writeLong(SystemUtilities.uptime());
            out.writeLong(SystemUtilities.freeMemory());
            out.write(addr);
            out.flush();
          } catch (IOException x) {
            throw new Bug("Unexpected exception (" + x + ")");
          }

          fetcheeChallenge = bytes.getBytes();
          if (bytes.size() != fetcheeChallenge.length) {
            throw new Bug("Buffer length inconsistent");
          }
          
          // Create the "I challenge you message".
          DynamicTuple dt = new DynamicTuple(fetcheeResponseHandler,
                                             fetcheeClosure);
          dt.set(MSG,     MSG_CHALLENGE);
          dt.set(PAYLOAD, fetcheeChallenge);
          
          fetcheeOperation.handle(new
            RemoteEvent(this, fetcheeClosure, fetcheeSource, dt));
          return true;
        }
        
        // Extract remote events.
        if (e instanceof RemoteEvent) {
          e = ((RemoteEvent)e).event;
        }

        if (e instanceof DynamicTuple) {
          DynamicTuple dt = (DynamicTuple)e;
          fetcheeSource   = (SymbolicHandler)dt.source;
          fetcheeClosure  = dt.closure;

          // Process the message field.
          String msg;
          try {
            msg = (String)dt.get(MSG, String.class, false);
          } catch (IllegalArgumentException x) {
            request.handle(new
              RemoteEvent(this, fetcheeClosure, fetcheeSource, new
                ExceptionalEvent(NullHandler.NULL, fetcheeClosure, new
                  IllegalArgumentException("No message field for fetcher " +
                                           "protocol message " + dt))));
            finish();
            return true;
          }
          if (! MSG_RESPONSE.equals(msg)) {
            request.handle(new
              RemoteEvent(this, fetcheeClosure, fetcheeSource, new
                ExceptionalEvent(NullHandler.NULL, fetcheeClosure, new
                  IllegalArgumentException("Unrecognized fetcher protocol " +
                                           "message " + msg))));
            finish();
            return true;
          }

          // Process the address.
          String address;
          try {
            address = (String)dt.get(ADDRESS, String.class, false);
          } catch (IllegalArgumentException x) {
            request.handle(new
              RemoteEvent(this, fetcheeClosure, fetcheeSource, new
                ExceptionalEvent(NullHandler.NULL, fetcheeClosure, new
                  IllegalArgumentException("No address field for fetcher " +
                                           "protocol message " + dt))));
            finish();
            return true;
          }
          try {
            SioResource.validateHost(address);
          } catch (InvalidTupleException x) {
            request.handle(new
              RemoteEvent(this, fetcheeClosure, fetcheeSource, new
                ExceptionalEvent(NullHandler.NULL, fetcheeClosure, new
                  IllegalArgumentException("Invalid address for fetcher " +
                                           "protocol message " + dt))));
            finish();
            return true;
          }
          
          // Process the payload.
          byte[] response;
          try {
            response = (byte[])dt.get(PAYLOAD, TYPE_BYTE_ARRAY, false);
          } catch (IllegalArgumentException x) {
            request.handle(new
              RemoteEvent(this, fetcheeClosure, fetcheeSource, new
                ExceptionalEvent(NullHandler.NULL, fetcheeClosure, new
                  IllegalArgumentException("No payload for 'I accept the " +
                                           "challenge' fetcher protocol " +
                                           "message " + dt))));
            finish();
            return true;
          }

          // Make sure the response matches the challenge.
          if (! Digest.isEqual(response,
                               digest.hash(fetcheeChallenge,
                                           Digest.toBytes(address),
                                           password))) {
            request.handle(new
              RemoteEvent(this, fetcheeClosure, fetcheeSource, new
                ExceptionalEvent(NullHandler.NULL, fetcheeClosure, new
                  IllegalArgumentException("Invalid password"))));
            finish();
            return true;
          }
          
          // We have been convinced, it's time to move.
          dt = new DynamicTuple(NullHandler.NULL, fetcheeClosure);
          dt.set(MSG, MSG_COMING);
          
          request.handle(new
            RemoteEvent(this, fetcheeClosure, fetcheeSource, dt));
          finish();
          request.handle(new
            MoveRequest(continuation, FETCHEE_CLOSURE, env.getId(),
                        "sio://" + address + "/User", false));
          return true;
          
        } else if (e instanceof ExceptionalEvent) {
          // Just wrap up.
          finish();
        }
        
        return false;
      }

      /** Finish the fetching protocol. */
      private void finish() {
        synchronized (lock) {
          if (null != fetcheeLM) {
            fetcheeLM.cancel();
            fetcheeLM              = null;
            fetcheeOperation       = null;
            fetcheeResponseHandler = null;
            fetcheeSource          = null;
            fetcheeClosure         = null;
            fetcheeChallenge       = null;
          }
        }
      }
    }
    
  }


  // =======================================================================
  //                           The user dialog
  // =======================================================================

  /** Implementation of a user dialog. */
  abstract class UserDialog extends JDialog
    implements ActionListener, Runnable {

    /** The user manager for this user dialog. */
    final User user;

    /**
     * Create a new user dialog for the specified user.
     *
     * @param  user  The user.
     */
    UserDialog(final User user) {
      super(mainWindow);

      this.user = user;

      setLocationRelativeTo(mainWindow);

      addWindowListener(new WindowAdapter() {
          public void windowClosing(WindowEvent e) {
            dispose();
            user.action = null;
            user.setEnabled(true);
          }
        });
    }

    /** Handle the specified action event. */
    public void actionPerformed(ActionEvent e) {
      String cmd = e.getActionCommand();

      if (GuiUtilities.ACTION_OK.equals(cmd)) {
        // Make sure we are still running.
        boolean running;
        synchronized (lock) {
          running = (ACTIVE == status);
        }

        if (running) {
          run();
        } else {
          dispose();
        }
      } else if (GuiUtilities.ACTION_CANCEL.equals(cmd)) {
        dispose();
        user.action = null;
        user.setEnabled(true);
      }
    }

    /** Run the OK action. */
    public abstract void run();

  }


  // =======================================================================
  //                           The run dialog
  // =======================================================================

  /** Implementation of the run dialog. */
  final class RunDialog extends UserDialog {

    /** The application combo box. */
    final JComboBox  application;

    /** The environment text field. */
    final JTextField environment;

    /** Create a new run dialog for the specified user. */
    RunDialog(User user) {
      super(user);

      if (user.local) {
        setTitle("Run Local Application");
      } else {
        setTitle("Run Application for " + user.name);
      }

      JComponent[] comps =
        GuiUtilities.createSimpleGrid(new
          String[] { "Application", "Environment" }, new
            int[] { GuiUtilities.ENTRY_COMBO_BOX,
                    GuiUtilities.ENTRY_TEXT_FIELD } , FIELD_WIDTH);
      application = (JComboBox)comps[1];
      application.setEditable(true);
      if (! user.local) {
        application.addItem("Chat");
      }
      application.addItem("Clock");
      application.addItem("Counter");
      application.setSelectedItem(null);
      application.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            // Provide a guess for the environment name.
            String s = ((String)application.getSelectedItem()).trim();
            int    i = s.indexOf(' ');
            if (-1 != i) {
              s = s.substring(0, i);
            }
            i = s.lastIndexOf('.');
            if (-1 != i) {
              s = s.substring(i+1);
            }
            environment.setText(s);
          }
        });

      environment = (JTextField)comps[2];

      ((JPanel)comps[0]).setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

      GuiUtilities.layout(this, comps[0], this, this);
    }

    /** Run the OK action. */
    public void run() {
      String s1 = (String)application.getSelectedItem();
      String s2 = environment.getText().trim();

      s1 = ((null == s1)? "" : s1.trim());

      // Perform consistency checks.
      if ("".equals(s1)) {
        GuiUtilities.beep();
        JOptionPane.showMessageDialog(this,
                                      "No application",
                                      "Run Application Error",
                                      JOptionPane.ERROR_MESSAGE);
        application.setSelectedItem(null);
        return;
      } else if ("".equals(s2)) {
        GuiUtilities.beep();
        JOptionPane.showMessageDialog(this,
                                      "No environment",
                                      "Run Application Error",
                                      JOptionPane.ERROR_MESSAGE);
        environment.setText(null);
        return;
      }
      try {
        Environment.ensureName(s2);
      } catch (IllegalArgumentException x) {
        GuiUtilities.beep();
        JOptionPane.showMessageDialog(this,
                                      "Environment name contains illegal " +
                                      "characters",
                                      "Run Application Error",
                                      JOptionPane.ERROR_MESSAGE);
        return;
      }
      Environment env;
      try {
        env = user.env.getChild(s2);
      } catch (IllegalStateException x) {
        dispose();
        return;
      }
      if (null != env) {
        GuiUtilities.beep();
        JOptionPane.showMessageDialog(this,
                                      "Environment " + s2 + " already exists",
                                      "Run Application Error",
                                      JOptionPane.ERROR_MESSAGE);
        return;
      }

      // Extract initializer class and parameters.
      String   init;
      String[] initClosure;

      if ("Chat".equals(s1)) {
        init        = "one.radio.Chat";
        initClosure = new String[] { user.name };

      } else if ("Clock".equals(s1)) {
        init        = "one.toys.Clock";
        initClosure = new String[] {};

      } else if ("Counter".equals(s1)) {
        init        = "one.toys.Counter2";
        initClosure = null;

      } else {
        StringTokenizer tok = new StringTokenizer(s1);
        ArrayList       lst = null;
        init                = null;

        while (tok.hasMoreTokens()) {
          String t = tok.nextToken();

          if (null == init) {
            init = t;
          } else {
            if (null == lst) {
              lst = new ArrayList();
            }
            lst.add(t);
          }
        }

        if (null == lst) {
          initClosure = new String[] {};
        } else {
          initClosure = (String[])lst.toArray(new String[lst.size()]);
        }
      }

      // Create the environment and load the application into it.
      operation.handle(new
        CreateRequest(null, user, user.env.getId(), s2, false, init,
                      initClosure));

      // Clean up dialog, but leave the action in tact.
      dispose();
    }

  }


  // =======================================================================
  //                   The delete environment dialog
  // =======================================================================

  /** Implementation of the delete environment dialog. */
  final class DeleteEnvDialog extends UserDialog {

    /** The environments combo box. */
    final JComboBox  environments;

    /** Create a new delete environment dialog for the specified user. */
    DeleteEnvDialog(User user, List children) {
      super(user);

      if (user.local) {
        setTitle("Delete Local Environment");
      } else {
        setTitle("Delete Environment for " + user.name);
      }

      JComponent[] comps =
        GuiUtilities.createSimpleGrid(new
          String[] { "Environment" }, new
            int[] { GuiUtilities.ENTRY_COMBO_BOX }, FIELD_WIDTH);
      environments = (JComboBox)comps[1];

      // Populate the combo box.
      Iterator iter = children.iterator();
      while (iter.hasNext()) {
        environments.addItem(iter.next());
      }

      ((JPanel)comps[0]).setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

      getContentPane().add(Box.createRigidArea(new Dimension(260, 0)),
                           BorderLayout.NORTH);
      GuiUtilities.layout(this, comps[0], this, this);
    }

    /** Run the OK action. */
    public void run() {
      String nm = (String)environments.getSelectedItem();

      // Resolve as an environment.
      Environment env;
      try {
        env = user.env.getChild(nm);
      } catch (IllegalStateException x) {
        dispose();
        return;
      }

      if (null == env) {
        // The environment has already been deleted.
        dispose();
        return;
      }

      // Delete the selected environment.
      operation.handle(new
        EnvironmentEvent(null, user, EnvironmentEvent.DESTROY, env.getId()));

      // Clean up dialog, but leave the action in tact.
      dispose();
    }

  }


  // =======================================================================
  //                           The move dialog
  // =======================================================================

  /** Implementation of the move dialog. */
  final class MoveDialog extends UserDialog {
    
    /** The location field. */
    final JTextField     location;

    /** The password field. */
    final JPasswordField password;

    /** Create a new move dialog for the specified user. */
    MoveDialog(User user) {
      super(user);

      setTitle("Move " + user.name);

      JComponent[] comps =
        GuiUtilities.createSimpleGrid(new
          String[] { "Location", "Password" }, new
            int[] { GuiUtilities.ENTRY_TEXT_FIELD,
                    GuiUtilities.ENTRY_PASSWORD_FIELD } , FIELD_WIDTH);
      location = (JTextField)comps[1];
      password = (JPasswordField)comps[2];

      ((JPanel)comps[0]).setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

      GuiUtilities.layout(this, comps[0], this, this);
    }

    /** Run the OK action. */
    public void run() {
      // Check the password.
      char[] pwd = password.getPassword();
      if (0 == pwd.length) {
        GuiUtilities.beep();
        JOptionPane.showMessageDialog(this,
                                      "Empty password",
                                      "Move User Error",
                                      JOptionPane.ERROR_MESSAGE);
        return;
      } else if (! Digest.isEqual(user.password,
                                  digest.hash(Digest.toBytes(pwd)))) {
        GuiUtilities.beep();
        JOptionPane.showMessageDialog(this,
                                      "Invalid password",
                                      "Move User Error",
                                      JOptionPane.ERROR_MESSAGE);
        password.setText(null);
        return;
      }

      // Check the location.
      String loc = location.getText().trim();
      if ("".equals(loc)) {
        GuiUtilities.beep();
        JOptionPane.showMessageDialog(this,
                                      "No location",
                                      "Move User Error",
                                      JOptionPane.ERROR_MESSAGE);
        location.setText(null);
        return;
      }
      try {
        SioResource.validateHost(loc);
      } catch (InvalidTupleException x) {
        GuiUtilities.beep();
        JOptionPane.showMessageDialog(this,
                                      x.getMessage(),
                                      "Move User Error",
                                      JOptionPane.ERROR_MESSAGE);
        return;
      }

      // Move away.
      operation.handle(new
        MoveRequest(null, user, user.env.getId(), "sio://"+loc+"/User", false));

      // Clean up dialog, but leave the action in tact.
      dispose();
    }

  }


  // =======================================================================
  //                       The delete user dialog
  // =======================================================================

  /** Implementation of the delete user dialog. */
  final class DeleteUserDialog extends UserDialog {
    
    /** The password field. */
    final JPasswordField password;

    /** Create a new delete user dialog for the specified user. */
    DeleteUserDialog(User user) {
      super(user);

      setTitle("Delete " + user.name);

      JComponent[] comps =
        GuiUtilities.createSimpleGrid(new
          String[] { "Password" }, new
            int[] { GuiUtilities.ENTRY_PASSWORD_FIELD } , FIELD_WIDTH);
      password = (JPasswordField)comps[1];

      ((JPanel)comps[0]).setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

      GuiUtilities.layout(this, comps[0], this, this);
    }

    /** Run the OK action. */
    public void run() {
      // Check the password.
      char[] pwd = password.getPassword();
      if (0 == pwd.length) {
        GuiUtilities.beep();
        JOptionPane.showMessageDialog(this,
                                      "Empty password",
                                      "Delete User Error",
                                      JOptionPane.ERROR_MESSAGE);
        return;
      } else if (! Digest.isEqual(user.password,
                                  digest.hash(Digest.toBytes(pwd)))) {
        GuiUtilities.beep();
        JOptionPane.showMessageDialog(this,
                                      "Invalid password",
                                      "Delete User Error",
                                      JOptionPane.ERROR_MESSAGE);
        password.setText(null);
        return;
      }

      // Delete the user.
      operation.handle(new
        EnvironmentEvent(null, user, EnvironmentEvent.DESTROY,
                         user.env.getId()));

      // Clean up dialog, but leave the action in tact.
      dispose();
    }

  }


  // =======================================================================
  //                      The change password dialog
  // =======================================================================

  /** Implementation of the change password dialog. */
  final class ChangePasswordDialog extends UserDialog {
    
    /** The old password field. */
    final JPasswordField old;

    /** The first new password field. */
    final JPasswordField new1;

    /** The second new password field. */
    final JPasswordField new2;

    /** Create a new change password dialog for the specified user. */
    ChangePasswordDialog(User user) {
      super(user);

      setTitle("Change Password for " + user.name);

      JComponent[] comps =
        GuiUtilities.createSimpleGrid(new
          String[] {"Old password","New password","New password (again)"}, new
            int[] { GuiUtilities.ENTRY_PASSWORD_FIELD,
                    GuiUtilities.ENTRY_PASSWORD_FIELD,
                    GuiUtilities.ENTRY_PASSWORD_FIELD } , FIELD_WIDTH);
      old  = (JPasswordField)comps[1];
      new1 = (JPasswordField)comps[2];
      new2 = (JPasswordField)comps[3];

      ((JPanel)comps[0]).setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

      GuiUtilities.layout(this, comps[0], this, this);
    }

    /** Run the OK action. */
    public void run() {
      user.newPassword = null;
      char[] o         = old.getPassword();
      char[] n1        = new1.getPassword();
      char[] n2        = new2.getPassword();

      // Consistency checks.
      if (! Digest.isEqual(n1, n2)) {
        GuiUtilities.beep();
        JOptionPane.showMessageDialog(this,
                                      "Different new passwords",
                                      "Change Password Error",
                                      JOptionPane.ERROR_MESSAGE);
      } else if (0 == n1.length) {
        GuiUtilities.beep();
        JOptionPane.showMessageDialog(this,
                                      "Empty new password",
                                      "Change Password Error",
                                      JOptionPane.ERROR_MESSAGE);
      } else if (! Digest.isEqual(user.password,
                                  digest.hash(Digest.toBytes(o)))) {
        GuiUtilities.beep();
        JOptionPane.showMessageDialog(this,
                                      "Invalid old password",
                                      "Change Password Error",
                                      JOptionPane.ERROR_MESSAGE);
      } else {
        user.newPassword = digest.hash(Digest.toBytes(n1));

        // Bind the user's root environment.
        SioResource resource = new SioResource();
        resource.type        = SioResource.STORAGE;
        resource.ident       = user.env.getId();

        operation.handle(new
          BindingRequest(null, user, resource, Duration.FOREVER));

        dispose();
      }

      new1.setText(null);
      new2.setText(null);
      old.setText(null);
    }

  }


  // =======================================================================
  //                           The create dialog
  // =======================================================================

  /** Implementation of the create dialog. */
  final class CreateDialog extends JDialog implements ActionListener {
    
    /** The user field. */
    final JTextField     user;

    /** The first password field. */
    final JPasswordField password1;

    /** The second password field. */
    final JPasswordField password2;

    /** Create a new create dialog. */
    CreateDialog() {
      super(mainWindow, "Create User");

      setDefaultCloseOperation(DISPOSE_ON_CLOSE);

      setLocationRelativeTo(mainWindow);

      JComponent[] comps =
        GuiUtilities.createSimpleGrid(new
          String[] { "User name", "Password", "Password (again)" }, new
            int[] { GuiUtilities.ENTRY_TEXT_FIELD,
                    GuiUtilities.ENTRY_PASSWORD_FIELD,
                    GuiUtilities.ENTRY_PASSWORD_FIELD } , FIELD_WIDTH);
      user      = (JTextField)comps[1];
      password1 = (JPasswordField)comps[2];
      password2 = (JPasswordField)comps[3];

      ((JPanel)comps[0]).setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

      GuiUtilities.layout(this, comps[0], this, this);
    }

    /** Handle the specified action. */
    public void actionPerformed(ActionEvent e) {
      String cmd = e.getActionCommand();

      if (GuiUtilities.ACTION_CANCEL.equals(cmd)) {
        dispose();
      } else if (GuiUtilities.ACTION_OK.equals(cmd)) {
        // Make sure we are still running.
        boolean running;
        synchronized (lock) {
          running = (ACTIVE == status);
        }
        if (! running) {
          dispose();
          return;
        }

        // Get the new user name and perform consistency checks.
        String name = user.getText().trim();

        if ("".equals(name)) {
          GuiUtilities.beep();
          JOptionPane.showMessageDialog(this,
                                        "Empty user name",
                                        "Create User Error",
                                        JOptionPane.ERROR_MESSAGE);
          user.setText(null);
          return;
        }
        try {
          Environment.ensureName(name);
        } catch (IllegalArgumentException x) {
          GuiUtilities.beep();
          JOptionPane.showMessageDialog(this,
                                        "User name contains illegal characters",
                                        "Create User Error",
                                        JOptionPane.ERROR_MESSAGE);
          return;
        }
        Environment env;
        try {
          env = getEnvironment().getChild(name);
        } catch (IllegalStateException x) {
          dispose();
          return;
        }
        if (null != env) {
          GuiUtilities.beep();
          JOptionPane.showMessageDialog(this,
                                        "User " + name + " already exists",
                                        "Create User Error",
                                        JOptionPane.ERROR_MESSAGE);
          return;
        }

        char[] pwd1 = null;
        if (! LOCAL.equals(name)) {
          // Get the password and make sure it's not empty.
          pwd1        = password1.getPassword();
          char[] pwd2 = password2.getPassword();
          
          if (! Digest.isEqual(pwd1, pwd2)) {
            GuiUtilities.beep();
            JOptionPane.showMessageDialog(this,
                                          "Different passwords",
                                          "Create User Error",
                                          JOptionPane.ERROR_MESSAGE);
            password1.setText(null);
            password2.setText(null);
            return;
          } else if (0 == pwd1.length) {
            GuiUtilities.beep();
            JOptionPane.showMessageDialog(this,
                                          "Empty password",
                                          "Create User Error",
                                          JOptionPane.ERROR_MESSAGE);
            password1.setText(null);
            password2.setText(null);
            return;
          }
        }

        // Create the user manager and add it to the table of user
        // managers.
        User user          = new User(name, null);
        if (! user.local) {
          user.newPassword = digest.hash(Digest.toBytes(pwd1));
        }
        user.action        = ACTION_CREATE;
        boolean added      = true;

        synchronized (lock) {
          if (ACTIVE != status) {
            return;
          } else if (users.containsKey(name)) {
            added = false;
          } else {
            users.put(name, user);
          }
        }

        if (! added) {
          GuiUtilities.beep();
          JOptionPane.showMessageDialog(this,
                                        "User " + name + " already exists",
                                        "Create User Error",
                                        JOptionPane.ERROR_MESSAGE);
          return;
        }

        // Fill in the user manager.
        user.fill();

        // Start the process of creating the user's root environment.
        operation.handle(new
          CreateRequest(null, user, getEnvironment().getId(), name,
                        false, null, null));

        // Dispose the dialog.
        dispose();
      }
    }
  }


  // =======================================================================
  //                           The fetch dialog
  // =======================================================================

  /** Implementation of the create dialog. */
  final class FetchDialog extends JDialog implements ActionListener {
    
    /** The user field. */
    final JTextField     user;

    /** The password field. */
    final JPasswordField password;

    /** Create a new fetch dialog. */
    FetchDialog() {
      super(mainWindow, "Fetch User");

      setDefaultCloseOperation(DISPOSE_ON_CLOSE);

      addWindowListener(new WindowAdapter() {
          public void windowClosing(WindowEvent e) {
            Window window;
            synchronized (lock) {
              if (ACTIVE == status) {
                window = (Window)mainWindow;
              } else {
                return;
              }
            }
            window.setFetchEnabled(true);
          }
        });

      setLocationRelativeTo(mainWindow);

      JComponent[] comps =
        GuiUtilities.createSimpleGrid(new
          String[] { "User", "Password" }, new
            int[] { GuiUtilities.ENTRY_TEXT_FIELD,
                    GuiUtilities.ENTRY_PASSWORD_FIELD } , FIELD_WIDTH);
      user     = (JTextField)comps[1];
      password = (JPasswordField)comps[2];

      ((JPanel)comps[0]).setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

      GuiUtilities.layout(this, comps[0], this, this);
    }

    /** Handle the specified action. */
    public void actionPerformed(ActionEvent e) {
      String cmd = e.getActionCommand();

      if (GuiUtilities.ACTION_CANCEL.equals(cmd)) {
        dispose();
        Window window;
        synchronized (lock) {
          if (ACTIVE == status) {
            window = (Window)mainWindow;
          } else {
            return;
          }
        }
        window.setFetchEnabled(true);

      } else if (GuiUtilities.ACTION_OK.equals(cmd)) {
        // Make sure we are not already fetching a user.
        boolean abort = false;
        synchronized (lock) {
          if (-1 != fetchRound) {
            abort      = true;
          } else {
            fetchRound = 0;
          }
        }
        if (abort) {
          GuiUtilities.beep();
          JOptionPane.showMessageDialog(this,
                                        "Already fetching a user",
                                        "Fetch User Error",
                                        JOptionPane.ERROR_MESSAGE);
          dispose();
        }

        // Extract user information.
        fetchUser     = user.getText().trim();
        char[] pwd    = password.getPassword();
        fetchLocation = null;

        // Break up user into user and location.
        int idx = fetchUser.indexOf('@');
        if (-1 != idx) {
          fetchLocation   = fetchUser.substring(idx + 1);
          fetchUser       = fetchUser.substring(0, idx);
          if ("".equals(fetchLocation)) {
            fetchLocation = null;
          }
        }

        // Consistency checks.
        if ("".equals(fetchUser)) {
          fetchRound = -1;
          GuiUtilities.beep();
          JOptionPane.showMessageDialog(this,
                                        "Empty user name",
                                        "Fetch User Error",
                                        JOptionPane.ERROR_MESSAGE);
          return;
        } 
        try {
          Environment.ensureName(fetchUser);
        } catch (IllegalArgumentException x) {
          fetchRound = -1;
          GuiUtilities.beep();
          JOptionPane.showMessageDialog(this,
                                        "User name contains illegal characters",
                                        "Fetch User Error",
                                        JOptionPane.ERROR_MESSAGE);
          return;
        }
        if (null != fetchLocation) {
          try {
            SioResource.validateHost(fetchLocation);
          } catch (InvalidTupleException x) {
            fetchRound = -1;
            GuiUtilities.beep();
            JOptionPane.showMessageDialog(this,
                                          x.getMessage(),
                                          "Fetch User Error",
                                          JOptionPane.ERROR_MESSAGE);
            return;
          }
        }
        if (0 == pwd.length) {
          fetchRound = -1;
          GuiUtilities.beep();
          JOptionPane.showMessageDialog(this,
                                        "Empty password",
                                        "Fetch User Error",
                                        JOptionPane.ERROR_MESSAGE);
          password.setText(null);
          return;
        }

        // Remember the password.
        fetchPassword = digest.hash(Digest.toBytes(pwd));

        // Begin the fetcher protocol by exporting the operation's
        // response handler.
        operation.handle(new
          BindingRequest(null, FETCHER_CLOSURE, new
            RemoteDescriptor(operation.getResponseHandler()),
                         Duration.FOREVER));

        // Dispose the dialog.
        dispose();
      }
    }
  }


  // =======================================================================
  //                           The scan handler
  // =======================================================================

  /** Implementation of the scan handler. */
  final class ScanHandler extends AbstractHandler {

    /** Handle the specified event. */
    protected boolean handle1(Event e) {
      if (! (e instanceof DynamicTuple)) {
        return false;
      }

      Environment root     = getEnvironment(); // The emcee's environment.
      ArrayList   add      = null;             // Users to add.
      ArrayList   remove   = null;             // Users to remove.
      List        children;                    // The user environments.
      Iterator    iter;                        // An iterator.
      Window      window;                      // The main window.

      try {
        children = root.getChildren();
      } catch (IllegalStateException x) {
        return true;
      }
      iter = children.iterator();

      synchronized (lock) {
        // Make sure that the emcee is active.
        if (ACTIVE != status) {
          return true;
        }

        // Scan for users to be added.
        while (iter.hasNext()) {
          String name = (String)iter.next();

          if (! users.containsKey(name)) {
            Environment env;

            try {
              env = root.getChild(name);
            } catch (IllegalStateException x) {
              return true;
            }
            if (null == env) {
              continue;
            } else {
              User user = new User(name, env);
              users.put(name, user);
              if (null == add) {
                add = new ArrayList();
              }
              add.add(user);
            }
          }
        }

        // Scan for users to be deleted.
        iter = users.values().iterator();
        while (iter.hasNext()) {
          User user = (User)iter.next();
          
          if ((! children.contains(user.name)) &&
              (! ACTION_CREATE.equals(user.action))) {
            iter.remove();  // Remove from users.
            user.release(); // Release all resources.
            if (null == remove) {
              remove = new ArrayList();
            }
            remove.add(user);
          }
        }

        window = (Window)mainWindow;
      }

      // Any users to remove from the main window?
      if (null != remove) {
        iter = remove.iterator();
        while (iter.hasNext()) {
          window.remove((User)iter.next());
        }
      }

      // Any users to add to the main window?
      if (null != add) {
        iter = add.iterator();
        while (iter.hasNext()) {
          User user = (User)iter.next();

          // Fill in the user manager.
          user.fill();
          
          if (! user.local) {
            // Bind the user's root environment.
            SioResource resource = new SioResource();
            resource.type        = SioResource.STORAGE;
            resource.ident       = user.env.getId();
            
            operation.handle(new
              BindingRequest(null, user, resource, Duration.FOREVER));
          }

          // Make the user visible.
          window.add(user);
        }
      }

      // Done.
      return true;
    }
  }


  // =======================================================================
  //                     The main continuation
  // =======================================================================

  /** Implementation of main continuation. */
  final class MainContinuation extends AbstractHandler {

    /** Handle the specified event. */
    protected boolean handle1(Event e) {
      if (e.closure instanceof User) {
        return handleUserEvent(e, (User)e.closure);
      } else if ((e.closure instanceof Operation.ChainingClosure) ||
                 FETCHER_CLOSURE.equals(e.closure)) {
        return handleFetcherEvent(e);
      } else if (FETCHEE_CLOSURE.equals(e.closure)) {
        // Not much we can do.
        return true;
      }

      return false;
    }

    /** Handle the specified event for the specified user. */
    private boolean handleUserEvent(Event e, User user) {

      // Handle exceptional conditions first.
      if (e instanceof ExceptionalEvent) {
        if (ACTION_CREATE.equals(user.action)) {
          // Remove the user from the users map again.
          synchronized (lock) {
            users.remove(user.name);
            user.release();
          }

        } else if (ACTION_INITIALIZE.equals(user.action)) {
          // The user still needs some password.
          user.password = digest.hash(Digest.toBytes(user.name));
          synchronized (lock) {
            user.release();
          }
        }

        // Extract the exception.
        Throwable x = ((ExceptionalEvent)e).x;

        if (x instanceof ExceptionInInitializerError) {
          x = ((ExceptionInInitializerError)x).getException();
        } else if (x instanceof InvocationTargetException) {
          x = ((InvocationTargetException)x).getTargetException();
        }

        // Tell the human user about it.
        signalError("Unable to perform " + user.action +
                    (user.local? " operation for local applications" :
                     " operation for user " + user.name) + ":\n" + x);

        // Re-enable user popup.
        user.action = null;
        user.setEnabled(true);

        // Done.
        return true;
      }

      // Now, handle regular events based on the user's
      // action. Dispatching by event type first would avoid
      // duplication of code. But, the control flow becomes much less
      // readable. Therefore, we dispatch on user action first.

      // ===================================================================
      if (ACTION_CREATE.equals(user.action)) {
        
        if ((e instanceof EnvironmentEvent) &&
            (EnvironmentEvent.CREATED == ((EnvironmentEvent)e).type)) {
          // Fill in the environment.
          try {
            user.env = getEnvironment().getChild(user.name);
          } catch (IllegalStateException x) {
            // We are shutting down.
            return true;
          }
          if (null == user.env) {
            // Somehow the environment got removed again.
            synchronized (lock) {
              if (ACTIVE == status) {
                users.remove(user.name);
              }
            }
            return true;
          }

          if (user.local) {
            // Show the user. Note that the popup menu does not need
            // to be enabled, b/c it is created that way.
            Window window;
            synchronized (lock) {
              if (ACTIVE != status) {
                return true;
              }
              window = (Window)mainWindow;
            }
            user.action = null;
            window.add(user);

            // Done.
            return true;
          }
          
          // Bind the user's tuple store, so that we can write the
          // user's preferences.
          SioResource resource = new SioResource();
          resource.type        = SioResource.STORAGE;
          resource.ident       = user.env.getId();

          operation.handle(new
            BindingRequest(null, user, resource, Duration.FOREVER));
          return true;

        } else if ((e instanceof BindingResponse) &&
                   (((BindingResponse)e).descriptor instanceof SioResource)) {
          BindingResponse br = (BindingResponse)e;

          // We bound the user's root environment. Let's make sure it
          // stays that way.
          synchronized (lock) {
            if (ACTIVE != status) {
              LeaseMaintainer.cancel(br.lease);
              return true;
            }
            user.lm2 = new LeaseMaintainer(br.lease, br.duration, user,
                                           LEASE_MAINTAINER_CLOSURE, timer);
          }
          user.operation2 = new Operation(0, Constants.OPERATION_TIMEOUT,
                                          timer, br.resource, this);

          // Write out the user preferences.
          user.operation2.handle(new
            SimpleOutputRequest(null, user, new
              UserPreferences(UserPreferences.DEFAULT_ID, user.name,
                              user.newPassword)));
          return true;

        } else if (e instanceof OutputResponse) {
          // Record the password.
          user.password    = user.newPassword;
          user.newPassword = null;

          // Release the bound tuple store.
          synchronized (lock) {
            if (null != user.lm2) {
              user.lm2.cancel();
              user.lm2        = null;
              user.operation2 = null;
            }
          }

          // Export the user manager locally.
          operation.handle(new
            BindingRequest(null, user, new
              RemoteDescriptor(user, "/User/" + user.name),
                           Duration.FOREVER));
          return true;

        } else if ((e instanceof BindingResponse) &&
                   (((BindingResponse)e).descriptor instanceof
                    RemoteDescriptor)) {
          BindingResponse  br   = (BindingResponse)e;
          RemoteDescriptor desc = (RemoteDescriptor)br.descriptor;

          if (desc.descriptor instanceof Name) {
            // We exported the user manager locally. Let's make sure
            // it stays that way.
            synchronized (lock) {
              if (ACTIVE != status) {
                LeaseMaintainer.cancel(br.lease);
                return true;
              }
              user.lmLocal = new LeaseMaintainer(br.lease, br.duration, user,
                                                 LEASE_MAINTAINER_CLOSURE,
                                                 timer);
            }

            // Export the user manager to discovery.
            operation.handle(new
              BindingRequest(null, user, new
                RemoteDescriptor(user, new
                  FetcherProtocol.UserDescriptor(user.name)),
                             Duration.FOREVER));
            return true;

          } else if (desc.descriptor instanceof
                     FetcherProtocol.UserDescriptor) {
            // We exported the user manager to discovery. Let's make sure
            // it stays that way.
            Window window;
            synchronized (lock) {
              if (ACTIVE != status) {
                LeaseMaintainer.cancel(br.lease);
                return true;
              }
              user.lmDiscovery = new LeaseMaintainer(br.lease,br.duration,user,
                                                     LEASE_MAINTAINER_CLOSURE,
                                                     timer);
              window           = (Window)mainWindow;
            }

            // Re-enable the user popup.
            user.action = null;
            user.setEnabled(true);

            // Show the user.
            window.add(user);

            // Done.
            return true;
          } else {
            return false;
          }

        } else {
          return false;
        }

      // ===================================================================
      } else if (ACTION_INITIALIZE.equals(user.action)) {

        if ((e instanceof BindingResponse) &&
            (((BindingResponse)e).descriptor instanceof SioResource)) {
          BindingResponse br = (BindingResponse)e;

          // We bound the user's root environment. Let's make sure it
          // stays that way.
          synchronized (lock) {
            if (ACTIVE != status) {
              LeaseMaintainer.cancel(br.lease);
              return true;
            }
            user.lm2 = new LeaseMaintainer(br.lease, br.duration, user,
                                           LEASE_MAINTAINER_CLOSURE, timer);
          }
          user.operation2 = new Operation(0, Constants.OPERATION_TIMEOUT,
                                          timer, br.resource, this);

          // Read in the user preferences.
          user.operation2.handle(new
            SimpleInputRequest(null, user, SimpleInputRequest.READ,
                               QUERY_USER_PREFERENCES, 0, false));
          return true;

        } else if (e instanceof InputResponse) {
          InputResponse ir = (InputResponse)e;

          // Release the bound tuple store.
          synchronized (lock) {
            if (null != user.lm2) {
              user.lm2.cancel();
              user.lm2        = null;
              user.operation2 = null;
            }
          }

          // Set user password.
          if (ir.tuple instanceof UserPreferences) {
            UserPreferences up = (UserPreferences)ir.tuple;

            if (up.name.equals(user.name)) {
              user.password = up.organization;
            } else {
              user.password = digest.hash(Digest.toBytes(user.name));
            }
          } else {
            user.password   = digest.hash(Digest.toBytes(user.name));
          }

          // Export the user manager locally.
          operation.handle(new
            BindingRequest(null, user, new
              RemoteDescriptor(user, "/User/" + user.name),
                           Duration.FOREVER));
          return true;

        } else if ((e instanceof BindingResponse) &&
                   (((BindingResponse)e).descriptor instanceof
                    RemoteDescriptor)) {
          BindingResponse  br   = (BindingResponse)e;
          RemoteDescriptor desc = (RemoteDescriptor)br.descriptor;

          if (desc.descriptor instanceof Name) {
            // We exported the user manager locally. Let's make sure it stays
            // that way.
            synchronized (lock) {
              if (ACTIVE != status) {
                LeaseMaintainer.cancel(br.lease);
                return true;
              }
              user.lmLocal = new LeaseMaintainer(br.lease, br.duration, user,
                                                 LEASE_MAINTAINER_CLOSURE,
                                                 timer);
            }

            // Export the user manager to discovery.
            operation.handle(new
              BindingRequest(null, user, new
                RemoteDescriptor(user, new
                  FetcherProtocol.UserDescriptor(user.name)),
                             Duration.FOREVER));
            return true;

          } else if (desc.descriptor instanceof
                     FetcherProtocol.UserDescriptor) {
            // We exported the user manager to discovery. Let's make
            // sure it stays that way.
            synchronized (lock) {
              if (ACTIVE != status) {
                LeaseMaintainer.cancel(br.lease);
                return true;
              }
              user.lmDiscovery = new LeaseMaintainer(br.lease,br.duration,user,
                                                     LEASE_MAINTAINER_CLOSURE,
                                                     timer);
            }

            // Re-enable user popup.
            user.action = null;
            user.setEnabled(true);

            // Done.
            return true;
          } else {
            return false;
          }

        } else {
          return false;
        }

      // ===================================================================
      } else if (ACTION_CHANGE_PASSWORD.equals(user.action)) {

        if (e instanceof BindingResponse) {
          BindingResponse br = (BindingResponse)e;

          // We bound the user's root environment. Let's make sure it
          // stays that way.
          synchronized (lock) {
            if (ACTIVE != status) {
              LeaseMaintainer.cancel(br.lease);
              return true;
            }
            user.lm2 = new LeaseMaintainer(br.lease, br.duration, user,
                                           LEASE_MAINTAINER_CLOSURE, timer);
          }
          user.operation2 = new Operation(0, Constants.OPERATION_TIMEOUT,
                                          timer, br.resource, this);
          
          // Write out the modified user preferences.
          user.operation2.handle(new
            SimpleOutputRequest(null, user, new
              UserPreferences(UserPreferences.DEFAULT_ID, user.name,
                              user.newPassword)));
          return true;

        } else if (e instanceof OutputResponse) {
          // Complete the password change.
          user.password    = user.newPassword;
          user.newPassword = null;

          // Release the bound tuple store.
          synchronized (lock) {
            if (null != user.lm2) {
              user.lm2.cancel();
              user.lm2        = null;
              user.operation2 = null;
            }
          }

          // Re-enable the user popup.
          user.action = null;
          user.setEnabled(true);

          // Done.
          return true;

        } else {
          return false;
        }

      // ===================================================================
      } else if (ACTION_CHECKPOINT.equals(user.action)) {
        if ((! (e instanceof CheckPointResponse)) ||
            (! user.env.getId().equals(((CheckPointResponse)e).ident))) {
          return false;
        }

        // Re-enable user popup.
        user.action = null;
        user.setEnabled(true);

        // Done.
        return true;

      // ===================================================================
      } else if (ACTION_RESTORE.equals(user.action) ||
                 ACTION_RESTORE_LATEST.equals(user.action)) {

        if ((! (e instanceof EnvironmentEvent)) ||
            (EnvironmentEvent.RESTORED != ((EnvironmentEvent)e).type)) {
          return false;
        }

        // Re-enable user popup.
        user.action = null;
        user.setEnabled(true);

        // Done.
        return true;

      // ===================================================================
      } else if (ACTION_MOVE.equals(user.action) ||
                 ACTION_MOVE_APPLICATION.equals(user.action) ||
                 ACTION_COPY_APPLICATION.equals(user.action)) {

        if ((! (e instanceof EnvironmentEvent)) ||
            (EnvironmentEvent.MOVED != ((EnvironmentEvent)e).type)) {
          return false;
        }

        // Re-enable user popup.
        user.action = null;
        user.setEnabled(true);

        // Done.
        return true;

      // ===================================================================
      } else if (ACTION_RUN.equals(user.action)) {

        if (! (e instanceof EnvironmentEvent)) {
          return false;
        }

        EnvironmentEvent ee = (EnvironmentEvent)e;

        if (EnvironmentEvent.CREATED == ee.type) {
          // Activate the environment.
          operation.handle(new
            EnvironmentEvent(null, user, EnvironmentEvent.ACTIVATE,
                             ee.ident));
          return true;

        } else if (EnvironmentEvent.ACTIVATED == ee.type) {
          // Re-enable user popup.
          user.action = null;
          user.setEnabled(true);

          // Done.
          return true;

        } else {
          return false;
        }

      // ===================================================================
      } else if (ACTION_DELETE_ENV.equals(user.action)) {

        if ((! (e instanceof EnvironmentEvent)) ||
            (EnvironmentEvent.DESTROYED != ((EnvironmentEvent)e).type)) {
          return false;
        }

        // Re-enable user popup.
        user.action = null;
        user.setEnabled(true);

        // Done.
        return true;
      // ===================================================================
      } else if (ACTION_DELETE_USER.equals(user.action)) {
        if ((! (e instanceof EnvironmentEvent)) ||
            (EnvironmentEvent.DESTROYED != ((EnvironmentEvent)e).type)) {
          return false;
        }

        // We don't re-enable the user popup, because it is going away
        // anyway. In fact, it may have already been destroyed.
        return true;
      }
      // ===================================================================

      // We know nothing about this action.
      return false;
    }

    /** Handle the specified fetcher protocol event. */
    private boolean handleFetcherEvent(Event e) {

      // ========================= Round 0 =================================
      if (e instanceof BindingResponse) {
        if (0 != fetchRound) {
          return false;
        }

        // We successfully rexported the response handler. Let's make
        // sure it stays exported.
        BindingResponse br = (BindingResponse)e;

        synchronized (lock) {
          if (ACTIVE != status) {
            LeaseMaintainer.cancel(br.lease);
            return true;
          }
          lm = new LeaseMaintainer(br.lease, br.duration, this,
                                   LEASE_MAINTAINER_CLOSURE, timer);
        }
        remoteResponseHandler = (RemoteReference)br.resource;

        // Start the first (actual) fetcher protocol round.
        fetchRound = 1;

        // Determine the destination.
        SymbolicHandler destination;
        if (null == fetchLocation) {
          Query q1    = new Query("", Query.COMPARE_HAS_SUBTYPE,
                                  FetcherProtocol.UserDescriptor.class);
          Query q2    = new Query("user", Query.COMPARE_EQUAL, fetchUser);
          Query q3    = new Query(q1, Query.BINARY_AND, q2);
          destination = new DiscoveredResource(q3);
        } else {
          destination = new NamedResource(fetchLocation,
                                          "/User/" + fetchUser);
        }

        // Create a new chaining closure.
        Operation.ChainingClosure closure = new Operation.ChainingClosure();

        // Create the actual message.
        DynamicTuple dt = new DynamicTuple(remoteResponseHandler, closure);
        dt.set(MSG, MSG_COME);

        // Fire off the message.
        operation.handle(new RemoteEvent(this, closure, destination, dt));

        // Done.
        return true;
      }

      // ================= Getting ready for rounds 1 and 2 ================
      // From here on, we only understand remote events and
      // exceptional events.
      if ((! (e instanceof RemoteEvent)) &&
          (! (e instanceof ExceptionalEvent))) {
        return false;
      }

      // Extract the actual event for rounds 1 and 2.
      if (e instanceof RemoteEvent) {
        e = ((RemoteEvent)e).event;
      }

      if (e instanceof DynamicTuple) {
        DynamicTuple    dt      = (DynamicTuple)e;
        SymbolicHandler source  = (SymbolicHandler)dt.source;
        Object          closure = dt.closure;
        String          msg;

        // Extract the message.
        try {
          msg = (String)dt.get(MSG, String.class, false);
        } catch (IllegalArgumentException x) {
          request.handle(new
            RemoteEvent(this, closure, source, new
              ExceptionalEvent(NullHandler.NULL, closure, new
                IllegalStateException("No message field for fetcher " +
                                      "protocol message " + dt))));
          signalError("Unable to fetch " + fetchUser + ":\n" +
                      "Unrecognized fetcher protocol message");
          stopFetching();
          return true;
        }

        // Make sure the message is valid.
        if (((! MSG_CHALLENGE.equals(msg)) || (1 != fetchRound)) &&
            ((! MSG_COMING.equals(msg))    || (2 != fetchRound))) {
          request.handle(new
            RemoteEvent(this, closure, source, new
              ExceptionalEvent(NullHandler.NULL, closure, new
                IllegalStateException("Unrecognized fetcher protocol " +
                                      "message " + msg + " for round " +
                                      fetchRound))));
          signalError("Unable to fetch " + fetchUser + ":\n" +
                      "Unrecognized fetcher protocol message");
          stopFetching();
          return true;
        }

        // ========================= Round 1 ===============================
        if (1 == fetchRound) {
          // Extract the challenge.
          byte[] challenge;

          try {
            challenge = (byte[])dt.get(PAYLOAD, TYPE_BYTE_ARRAY, false);
          } catch (IllegalArgumentException x) {
            request.handle(new
              RemoteEvent(this, closure, source, new
                ExceptionalEvent(NullHandler.NULL, closure, new
                  IllegalStateException("No payload for 'I challenge you' " +
                                        "fetcher protocol message " + dt))));
            signalError("Unable to fetch " + fetchUser + ":\n" +
                        "Malformed fetcher protocol message");
            stopFetching();
            return true;
          }

          // Determine the address.
          String address = SystemUtilities.ipAddress();

          // Compute the response.
          byte[] response = digest.hash(challenge, Digest.toBytes(address),
                                        fetchPassword);
          
          // Respond.
          fetchRound = 2;
          dt    = new DynamicTuple(remoteResponseHandler, null);
          dt.set(MSG,     MSG_RESPONSE);
          dt.set(PAYLOAD, response);
          dt.set(ADDRESS, address);

          operation.handle(new RemoteEvent(this, closure, source, dt));
          return true;

          // ========================= Round 2 =============================
        } else {
          // The fetcher protocol completed successfully. Clear the UI
          // and re-enable it.
          stopFetching();
          return true;
        }

      } else if (e instanceof ExceptionalEvent) {
        ExceptionalEvent ee = (ExceptionalEvent)e;

        if (LEASE_MAINTAINER_CLOSURE.equals(e.closure)) {
          signalError("Unable to fetch " + fetchUser + " due to\ninternal " +
                      "error in lease maintainer:\n" + ee.x);

        } else {
          Throwable        x  = ee.x;
          
          if (x instanceof ExceptionInInitializerError) {
            x = ((ExceptionInInitializerError)x).getException();
          } else if (x instanceof InvocationTargetException) {
            x = ((InvocationTargetException)x).getTargetException();
          }
          
          // Tell user about the exceptional condition.
          signalError("Unable to fetch " + fetchUser + ":\n" + x);
        }
          
        // Done.
        stopFetching();
        return true;
      }

      return false;
    }

    /**
     * Stop the fetcher protocol. This method release all resources,
     * resets the <code>fetchRound</code> field, and re-enables the
     * fetch menu item.
     */
    private void stopFetching() {
      Window window;
      synchronized (lock) {
        if (null != lm) {
          lm.cancel();
          lm = null;
        }

        fetchRound    = -1;
        fetchUser     = null;
        fetchLocation = null;
        fetchPassword = null;

        if (ACTIVE != status) {
          return;
        }
        window = (Window)mainWindow;
      }

      window.setFetchEnabled(true);
    }

  }


  // =======================================================================
  //                           Instance fields
  // =======================================================================

  /** The main continuation. */
  transient EventHandler       continuation;

  /**
   * The user map, which maps user names to user records.
   */
  transient HashMap            users;

  /**
   * The timer notification for scanning the user environment's
   * children.
   *
   * @serial 
   */
  Timer.Notification           scan;

  /** The digest generator. */
  transient Digest             digest;

  /** The remote reference for the operation's response handler. */
  transient RemoteReference    remoteResponseHandler;

  /** The lease maintainer for exporting the response handler. */
  transient LeaseMaintainer    lm;

  /**
   * The current round of the fetcher protocol. -1 marks that no user
   * is currently being fetched. 0, 1, and 2 are the actual fetcher
   * protocol rounds. The transition from -1 to 0 must be done under
   * the lock for this application.
   */
  transient int                fetchRound;

  /** The name of the user to be fetched. */
  transient String             fetchUser;

  /** The user's location. */
  transient String             fetchLocation;

  /** The digest of the user's password. */
  transient byte[]             fetchPassword;
 

  // =======================================================================
  //                           Constructor
  // =======================================================================

  /**
   * Create a new instance of <code>Emcee</code>.
   *
   * @param  env   The environment for the new instance.
   */
  public Emcee(Environment env) {
    super(env);
    width        = WINDOW_WIDTH;
    height       = WINDOW_HEIGHT;
    continuation = new MainContinuation();
    operation    = new Operation(0, Constants.OPERATION_TIMEOUT,
                                 timer, request, continuation);
    users        = new HashMap();
    digest       = new Digest();
    fetchRound   = -1;
  }


  // =======================================================================
  //                           Serialization
  // =======================================================================

  /**
   * Serialize this emcee component.
   *
   * @serialData  The default fields while holding the lock.
   */
  private void writeObject(ObjectOutputStream out) throws IOException {
    synchronized (lock) {
      out.defaultWriteObject();
    }
  }

  /** Deserialize an emcee component. */
  private void readObject(ObjectInputStream in)
    throws IOException, ClassNotFoundException {

    // Read the non-transient fields.
    in.defaultReadObject();

    // Restore the transient fields.
    continuation = new MainContinuation();
    operation    = new Operation(0, Constants.OPERATION_TIMEOUT,
                                 timer, request, continuation);
    users        = new HashMap();
    digest       = new Digest();
    fetchRound   = -1;
  }


  // =======================================================================
  //                           Start and stop
  // =======================================================================

  /** Acquire the resources needed by the emcee application. */
  public void acquire() {
    synchronized (lock) {
      if (ACTIVATING != status) {
        return;
      }

      // Reset the fetcher protocol.
      fetchRound = -1;

      // Set up scan notifications.
      if (null == scan) {
        scan = timer.schedule(Timer.FIXED_RATE,
                              SystemUtilities.currentTimeMillis(),
                              Duration.SECOND,
                              new ScanHandler(),
                              new DynamicTuple());
      }
    }

    start();
  }

  /** Release the resources used by the emcee application. */
  public void release() {
    // Cancel scan notifications.
    if (null != scan) {
      scan.cancel();
      scan = null;
    }

    // Release user resources.
    Iterator iter = users.values().iterator();
    while (iter.hasNext()) {
      User user = (User)iter.next();

      user.release();
    }
    users.clear();

    // Release main resources.
    if (null != lm) {
      lm.cancel();
      lm = null;
    }
  }


  // =======================================================================
  //                             Managing the UI
  // =======================================================================

  /** Create the emcee's main window. */
  public Application.Window createMainWindow() {
    Window window = new Window(this);
    window.setSize(width, height);
    return window;
  }

  /**
   * Handle the specified action event.
   *
   * @param  e  The action event.
   */
  public void actionPerformed(ActionEvent e) {
    String cmd = e.getActionCommand();

    if (ACTION_CREATE.equals(cmd)) {
      new CreateDialog().show();

    } else if (ACTION_FETCH.equals(cmd)) {
      Window window;
      synchronized (lock) {
        if (ACTIVE == status) {
          window = (Window)mainWindow;
        } else {
          return;
        }
      } 
      window.setFetchEnabled(false);
      new FetchDialog().show();
    }
  }


  // =======================================================================
  //                            Initialization
  // =======================================================================

  /**
   * Initialize the <i>one.radio</i> emcee.
   *
   * @param   env      The environment.
   * @param   closure  The closure, which is ignored.
   */
  public static void init(Environment env, Object closure) {
    Emcee comp = new Emcee(env);

    env.link("main", "main", comp);
    comp.link("request", "request", env);
  }

}
