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

import java.awt.*;
import java.awt.dnd.*;
import java.awt.event.*;
import javax.swing.*;

import java.awt.datatransfer.Transferable;

import javax.swing.border.Border;

import one.util.Guid;

import one.world.core.Environment;
import one.world.core.EventHandler;

import one.world.util.NullHandler;

/**
 * Implementation of utility methods for creating and managing user
 * interfaces.
 *
 * @version  $Revision: 1.7 $
 * @author   Robert Grimm
 */
public final class GuiUtilities {

  /** Hide the constructor. */
  private GuiUtilities() {
    // Nothing to do.
  }

  /** The OK action command. */
  public  static final String ACTION_OK     = "OK";

  /** The cancel action command. */
  public  static final String ACTION_CANCEL = "cancel";

  /** The border width for drop targets. */
  private static final int    BORDER_WIDTH  = 2;

  /** The empty drop target border. */
  private static final Border EMPTY_BORDER;

  /** The highlighted drop target border. */
  private static final Border HIGHLIGHT_BORDER;

  /**
   * The flag icon. The flag icon shows a little flag within a
   * box. Its background is transparent.
   */
  public  static final Icon   FLAG_ICON;

  static {
    byte[] flagData = new byte[] {
      0x47, 0x49, 0x46, 0x38, 0x39, 0x61, 0x0f, 0x00,
      0x0f, 0x00, (byte)0x80, 0x00, 0x00, 0x00, 0x00, 0x00,
      (byte)0xff, (byte)0xff, (byte)0xff, 0x21, (byte)0xf9, 0x04, 0x01, 0x00,
      0x00, 0x01, 0x00, 0x2c, 0x00, 0x00, 0x00, 0x00,
      0x0f, 0x00, 0x0f, 0x00, 0x00, 0x02, 0x26, (byte)0x84,
      (byte)0x8f, 0x09, (byte)0xc1, (byte)0xed, 0x6d, (byte)0x9e, 0x64, 0x31,
      (byte)0xd4, 0x0a, (byte)0x97, (byte)0xb5, 0x1a, 0x71, 0x28, 0x75,
      (byte)0xda, (byte)0xe6, (byte)0x8c, 0x18, 0x79, (byte)0xa6, 0x64, 0x39,
      0x52,(byte)0xd3,(byte)0xb9,(byte)0xb2, 0x61, (byte)0xd8, (byte)0xba,0x0f,
      0x3c, 0x71, (byte)0xca, 0x0e, 0x14, 0x00, 0x3b };

    FLAG_ICON = new ImageIcon(flagData, "The flag icon") {
        public Image getImage() { return null; }
        public void setDescription(String s) { }
        public void setImage(Image i) { }
        public void setImageObserver(java.awt.image.ImageObserver io) { }
      };

    EMPTY_BORDER = BorderFactory.createEmptyBorder(BORDER_WIDTH, BORDER_WIDTH,
                                                   BORDER_WIDTH, BORDER_WIDTH);
    HIGHLIGHT_BORDER =
      BorderFactory.createLineBorder(UIManager.getColor("textHighlight"),
                                     BORDER_WIDTH);
  }

  /**
   * Create a label that serves as the drag and drop source for
   * location changes of the environment with the specified ID. The
   * returned label displays the {@link #FLAG_ICON} and transfers
   * {@link EnvironmentSelection environment selections} for the
   * specified environment.
   *
   * @see     #addUserPopup
   *
   * @param   id  The environment ID.
   * @return      The corresponding label.
   * @throws  NullPointerException
   *              Signals that <code>id</code> is <code>null</code>.
   */
  public static JLabel createLocationSource(Guid id) {
    JLabel label = new JLabel(FLAG_ICON);
    turnIntoEnvironmentSource(label, id);
    return label;
  }

  /**
   * Turn the specified component into a drag and drop source for
   * environment selections for the environment with the specified
   * ID.
   *
   * @see     EnvironmentSelection
   *
   * @param   c    The component.
   * @param   id   The environment ID.
   * @throws  NullPointerException
   *               Signals that <code>c</code> or <code>id</code>
   *               is <code>null</code>.
   */
  public static void turnIntoEnvironmentSource(Component c, final Guid id) {
    if (null == id) {
      throw new NullPointerException("Null environment ID");
    }

    DragSource          ds  = DragSource.getDefaultDragSource();
    DragGestureListener dgl = new DragGestureListener() {
        public void dragGestureRecognized(DragGestureEvent e) {
          final boolean            cp  = isDragCopy(e.getDragAction());
          final DragSourceListener dsl = new DragSourceListener() {
              boolean copy = cp;
              boolean drop = false;

              public void dropActionChanged(DragSourceDragEvent e) {
                copy = isDragCopy(e.getDropAction());
                e.getDragSourceContext().setCursor(getDragCursor(copy,drop));
              }
              public void dragEnter(DragSourceDragEvent e) {
                drop = true;
                e.getDragSourceContext().setCursor(getDragCursor(copy,drop));
              }
              public void dragOver(DragSourceDragEvent e) {
                copy = isDragCopy(e.getDropAction());
                drop = true;
                e.getDragSourceContext().setCursor(getDragCursor(copy,drop));
              }
              public void dragExit(DragSourceEvent e) {
                drop = false;
                e.getDragSourceContext().setCursor(getDragCursor(copy,drop));
              }
              public void dragDropEnd(DragSourceDropEvent e) {}
            };
          e.startDrag(getDragCursor(cp,false),
                      new EnvironmentSelection(id),
                      dsl);
        }
      };
    ds.createDefaultDragGestureRecognizer(c,
                                          DnDConstants.ACTION_COPY_OR_MOVE,
                                          dgl);
  }    

  /**
   * Turn the specified label into a drag and drop target for
   * environment selections. Upon accepting an environment selection,
   * the label sends an environment drop event to the specified event
   * handler.
   * 
   * <p>Note that this method adds a border to the specified label,
   * which must not be changed.</p>
   *
   * @see     #turnIntoEnvironmentSource
   * @see     EnvironmentSelection
   * @see     EnvironmentDropEvent
   *
   * @param   label    The label.
   * @param   handler  The environment drop event handler.
   * @throws  NullPointerException
   *                   Signals that <code>label</code> or
   *                   <code>handler</code> is <code>null</code>.
   */
  public static void turnIntoEnvironmentTarget(final JLabel label,
                                               final EventHandler handler) {
    if (null == label) {
      throw new NullPointerException("Null label");
    } else if (null == handler) {
      throw new NullPointerException("Null event handler");
    }

    label.setBorder(EMPTY_BORDER);

    DropTargetListener dtl = new DropTargetListener() {
        boolean copy = false;

        public void dropActionChanged(DropTargetDragEvent e) {
          process(e);
        }
        public void dragEnter(DropTargetDragEvent e) {
          process(e);
        }
        public void dragOver(DropTargetDragEvent e) {
          process(e);
        }
        public void dragExit(DropTargetEvent e) {
          setHighlight(false);
        }
        public void drop(DropTargetDropEvent e) {
          if (e.isDataFlavorSupported(EnvironmentSelection.PREFERRED_FLAVOR)) {
            e.acceptDrop(copy ?
                         DnDConstants.ACTION_COPY :
                         DnDConstants.ACTION_MOVE);
            Transferable         t  = e.getTransferable();
            EnvironmentSelection es;
            if (t instanceof EnvironmentSelection) {
              es = (EnvironmentSelection)t;
            } else {
              try {
                es = (EnvironmentSelection)
                  t.getTransferData(EnvironmentSelection.PREFERRED_FLAVOR);
              } catch (Exception x) {
                e.dropComplete(false);
                return;
              }
            }
            handler.handle(new
              EnvironmentDropEvent(NullHandler.NULL, null, es.getId(), copy));
            e.dropComplete(true);
          } else {
            e.rejectDrop();
          }
        }
        private void process(DropTargetDragEvent e) {
          if (e.isDataFlavorSupported(EnvironmentSelection.PREFERRED_FLAVOR)) {
            copy = isDragCopy(e.getDropAction());
            setHighlight(true);
          } else {
            e.rejectDrag();
          }
        }
        private void setHighlight(boolean drop) {
          Border border = drop? HIGHLIGHT_BORDER : EMPTY_BORDER;
          if (label.getBorder().equals(border)) return;
          label.setBorder(border);
          label.repaint();
        }
      };

    new DropTarget(label, DnDConstants.ACTION_COPY_OR_MOVE, dtl);
  }

  /**
   * Determine whether the specified drag action represents a copy
   * operation.
   *
   * @param   action  The action.
   * @return          <code>true</code> if the specified action
   *                  represents a copy operation.
   */
  static boolean isDragCopy(int action) {
    return (DnDConstants.ACTION_COPY == action);
  }

  /**
   * Get the appropriate drag and drop cursor.
   *
   * @param   copy  The flag for whether this is a copy operation.
   * @param   drop  The flag for whether a drop is currently possible.
   * @return        The corresponding cursor.
   */
  static Cursor getDragCursor(boolean copy, boolean drop) {
    if (copy) {
      if (drop) {
        return DragSource.DefaultCopyDrop;
      } else {
        return DragSource.DefaultCopyNoDrop;
      }
    } else {
      if (drop) {
        return DragSource.DefaultMoveDrop;
      } else {
        return DragSource.DefaultMoveNoDrop;
      }
    }
  }

  /**
   * Add a user popup to the specified component. This method adds a
   * popup menu to the specified component, which displays the name of
   * the user the application is running for. To determine the current
   * user, the popup menu dynamically interprets the name of the
   * parent environment of the specified environment as the user's
   * local root environment.
   *
   * <p>By user interface convention, this method should be invoked on
   * the label returned by {link #createLocationSource} before adding
   * the label to the application's UI.</p>
   *
   * @param   c    The component to add the user popup to.
   * @param   env  The environment in which the component's
   *               application is running in.
   * @throws  NullPointerException
   *               Signals that <code>env</code> is <code>null</code>.
   */
  public static void addUserPopup(Component c, final Environment env) {
    if (null == env) {
      throw new NullPointerException("Null environment");
    }

    final JPopupMenu popup    = new JPopupMenu();
    popup.add(new JLabel());

    MouseListener    listener = new MouseAdapter() {
        public void mousePressed(MouseEvent e) {
          maybeShowPopup(e);
        }
        public void mouseReleased(MouseEvent e) {
          maybeShowPopup(e);
        }
        private void maybeShowPopup(MouseEvent e) {
          if (! e.isPopupTrigger()) {
            return;
          }

          // Get the name of the parent environment.
          String name;
          try {
            name = env.getParentName();
          } catch (IllegalStateException x) {
            return;
          }

          // Construct the running message.
          String msg;
          if ("local".equals(name)) {
            msg = "Running as a local application";
          } else {
            msg = "Running for user " + name;
          }

          // Add the running message to the popup menu.
          popup.remove(0);
          popup.add(new JMenuItem(msg));
          
          // Show the popup menu.
          popup.show(e.getComponent(), e.getX(), e.getY());
        }
      };

    c.addMouseListener(listener);
  }

  /** The entry code for a text field. */
  public static final int ENTRY_TEXT_FIELD     = 1;

  /** The entry code for a password field. */
  public static final int ENTRY_PASSWORD_FIELD = 2;

  /** The entry code for a combo box. */
  public static final int ENTRY_COMBO_BOX      = 3;

  /**
   * Create a simple grid. A simple grid for the purposes of this
   * method is a grid-like panel with essentially two columns: The
   * first column contains labels, and the second column contains
   * entries with either a text field, password field, or combo box.
   *
   * <p>The layout created by this method adds some filler space
   * between the labels and the entries. It aligns the first
   * column to the left. Entries in the second column are stretched
   * across the entire column. If <code>columns</code> is positive,
   * test fields and password fields are set to the specified
   * number of character columns.</p>
   *
   * <p><code>labels</code> and <code>types</code> must be valid
   * arrays of the same length. Each entry of <code>types</code> must
   * be {@link #ENTRY_TEXT_FIELD}, {@link #ENTRY_PASSWORD_FIELD}, or
   * {@link #ENTRY_COMBO_BOX}.</p>
   *
   * @param   labels   The labels.
   * @param   types    The entry types.
   * @param   columns  The positive minimum number of character columns
   *                   for text and password fields, or a non-positive
   *                   number if text and password fields should be
   *                   arbitrarily stretched.
   * @return           An array of components; the first being the
   *                   overall panel, followed in order by the text
   *                   fields, password fields, and combo boxes as
   *                   specified by <code>types</code>.
   */
  public static JComponent[] createSimpleGrid(String[] labels,
                                              int[]    types,
                                              int      columns) {
    boolean            hasColumns = (0 < columns);
    Dimension          x5         = new Dimension(5, 0);
    Dimension          y5         = new Dimension(0, 5);
    GridBagLayout      layout     = new GridBagLayout();
    GridBagConstraints c          = new GridBagConstraints();
    JPanel             panel      = new JPanel();
    JComponent[]       comps      = new JComponent[labels.length + 1];

    c.anchor = GridBagConstraints.WEST;
    comps[0] = panel;
    panel.setLayout(layout);

    for (int i=0; i<labels.length; i++) {
      JLabel     label;
      Component  filler;
      JComponent entry;

      // Add vertical filler.
      if (0 < i) {
        filler  = Box.createRigidArea(y5);
        c.gridx = 2;
        c.gridy = (i << 1) - 1;
        layout.setConstraints(filler, c);
        panel.add(filler);
      }

      // Create the label and entry.
      label = new JLabel(labels[i]);
      if (ENTRY_TEXT_FIELD == types[i]) {
        if (hasColumns) {
          entry    = new JTextField(columns);
        } else {
          entry    = new JTextField();
        }
      } else if (ENTRY_PASSWORD_FIELD == types[i]) {
        if (hasColumns) {
          entry    = new JPasswordField(columns);
        } else {
          entry    = new JPasswordField();
        }
      } else {
        entry      = new JComboBox();
      }
      comps[i + 1] = entry;
      label.setLabelFor(entry);

      c.gridx      = 0;
      c.gridy      = i << 1;
      layout.setConstraints(label, c);
      panel.add(label);

      filler       = Box.createRigidArea(x5);
      c.gridx      = 1;
      layout.setConstraints(filler, c);
      panel.add(filler);

      c.gridx      = 2;
      c.weightx    = 1.0;
      c.fill       = GridBagConstraints.HORIZONTAL;
      layout.setConstraints(entry, c);
      panel.add(entry);
      c.weightx    = 0.0;
      c.fill       = GridBagConstraints.NONE;
    }

    return comps;
  }

  /**
   * Create the layout for the specified dialog. This method adds the
   * specified content to the dialog and adds a row of two additional
   * buttons underneath the content. The first button is the "OK"
   * button.  It is the default button for the dialog and notifies the
   * specified OK listener with the {@link #ACTION_OK} command. The
   * second button is the "Cancel" button. It notifies the specified
   * cancel listener with the {@link #ACTION_CANCEL} command.
   *
   * @param  dialog          The dialog.
   * @param  content         The main dialog contents.
   * @param  okListener      The action listener for the OK button.
   * @param  cancelListener  The action listener for the cancel button.
   */
  public static void layout(JDialog dialog, Component content,
                            ActionListener okListener,
                            ActionListener cancelListener) {
    JButton ok     = new JButton("OK");
    ok.setMnemonic(KeyEvent.VK_O);
    ok.setActionCommand(ACTION_OK);
    ok.addActionListener(okListener);

    JButton cancel = new JButton("Cancel");
    cancel.setMnemonic(KeyEvent.VK_C);
    cancel.setActionCommand(ACTION_CANCEL);
    cancel.addActionListener(cancelListener);

    JPanel panel = new JPanel();
    panel.add(ok);
    panel.add(cancel);

    Container contentPane = dialog.getContentPane();
    contentPane.add(content, BorderLayout.CENTER);
    contentPane.add(panel,   BorderLayout.SOUTH);

    dialog.getRootPane().setDefaultButton(ok);
    dialog.pack();
  }

  /**
   * Place the specified component at the specified location on the
   * default screen. This method adjusts the location to fit as much
   * as possible of the component onto the screen.
   *
   * @param   component  The component to place.
   * @param   x          The x coordinate of the location.
   * @param   y          The y coordinate of the location.
   */
  public static void place(Component component, int x, int y) {
    Dimension size   = Toolkit.getDefaultToolkit().getScreenSize();
    int       height = component.getHeight();
    int       width  = component.getWidth();
 
    // Move the component to the left and up, if necessary...
    if (x + width  > size.width ) x = size.width  - width;
    if (y + height > size.height) y = size.height - height;

    // ... but never move it too far left or up.
    if (0 > x) x = 0;
    if (0 > y) y = 0;

    // Done.
    component.setLocation(x, y);
  }

  /** Emit an audible beep. */
  public static void beep() {
    Toolkit.getDefaultToolkit().beep();
  }

}


