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

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Toolkit;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.util.HashMap;
import java.util.Iterator;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import one.gui.Application;
import one.gui.GuiUtilities;
import one.gui.SolidIcon;

import one.util.Bug;
import one.util.Guid;

import one.world.binding.BindingRequest;
import one.world.binding.BindingResponse;
import one.world.binding.Duration;
import one.world.binding.LeaseMaintainer;
import one.world.binding.UnknownResourceException;

import one.world.core.Component;
import one.world.core.ComponentDescriptor;
import one.world.core.DynamicTuple;
import one.world.core.Environment;
import one.world.core.Event;
import one.world.core.EventHandler;
import one.world.core.ExceptionalEvent;
import one.world.core.ExportedDescriptor;
import one.world.core.ImportedDescriptor;

import one.world.env.EnvironmentEvent;

import one.world.io.Query;

import one.world.rep.DiscoveredResource;
import one.world.rep.RemoteDescriptor;
import one.world.rep.RemoteEvent;
import one.world.rep.RemoteReference;

import one.world.util.AbstractHandler;
import one.world.util.Operation;
import one.world.util.Synchronous;

/**
 * Implementation of the <i>one.radio</i> chat application.
 *
 * <p>Usage:<pre>
 *    Chat &lt;user-name&gt;
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
 *    <dt>audioSinkAudio</dt>
 *    <dd>Handles {@link AudioMessage audio messages}.
 *        </dd>
 *    <dt>audioSinkControl</dt>
 *    <dd>Handles {@link AudioSink.ControlEvent audio sink control events}.
 *        </dd>
 * </dl></p>
 *
 * @version  $Revision: 1.30 $
 * @author   Robert Grimm
 */
public final class Chat extends Application
  implements ActionListener, ChangeListener {

  // =======================================================================
  //                              Constants
  // =======================================================================

  /** The name of the meta-channel representing all channels. */
  static final String       ALL_CHANNELS          = "<All channels>";

  /** A query for a channel descriptor. */
  static final Query        IS_CHANNEL            = new
    Query("", Query.COMPARE_HAS_SUBTYPE, Channel.class);

  /** The size of the text message history buffer. */
  static final int HISTORY_SIZE                   = 1000;

  /** The silence audio source type. */
  static final int AUDIO_SILENCE                  = 0;

  /** The microphone audio source type. */
  static final int AUDIO_MICROPHONE               = 1;

  /** The environment audio source type. */
  static final int AUDIO_ENVIRONMENT              = 2;

  /** The action command for listening to a channel. */
  static final String ACTION_LISTEN               = "listen";

  /** The action command for selecting a channel to send to. */
  static final String ACTION_SELECT_SEND          = "select-send";

  /** The action command for sending a message to a channel. */
  static final String ACTION_SEND                 = "send";

  /** The action command for selecting a channel to send audio to. */
  static final String ACTION_SELECT_AUDIO         = "select-audio";

  /** The action command for adding an audio sender. */
  static final String ACTION_ADD_AUDIO            = "add-audio";

  /** The action command for deleting an audio sender. */
  static final String ACTION_DELETE_AUDIO         = "delete-audio";

  /** The action command for selecting a subscription. */
  static final String ACTION_SELECT_SUBSCRIBE     = "select-subscribe";

  /** The action command for adding or changing a subscription. */
  static final String ACTION_ADD_SUBSCRIPTION     = "add-subscription";

  /** The action command for deleting a subscription. */
  static final String ACTION_DELETE_SUBSCRIPTION  = "delete-subscription";

  /** 
   * The width in characters (minus space for the scroll pane) for the
   * text pane in the main window's listen panel.
   */
  static final int TEXT_WIDTH                     = 42;

  /**
   * The height in characters (minus space for the the scroll pane)
   * for the text pane in the main window's listen panel.
   */
  static final int TEXT_HEIGHT                    = 16;

  /**
   * The width in pixels for the solid icon showing the channel color
   * in the main window's subscribe panel.
   */
  static final int ICON_WIDTH                     = 30;

  /**
   * The height in pixels for the solid icon showing the channel color
   * in the main window's subscribe panel.
   */
  static final int ICON_HEIGHT                    = 15;

  /** The default color for new channels. */
  static final Color DEFAULT_COLOR                = Color.blue;

  /** 2 points along the x-axis. */
  static final Dimension X2                       = new Dimension(2, 0);

  /** 5 points along the x-axis. */
  static final Dimension X5                       = new Dimension(5, 0);

  /** 15 points along the x-axis. */
  static final Dimension X15                      = new Dimension(15, 0);

  /** 5 points along the y-axis. */
  static final Dimension Y5                       = new Dimension(0, 5);

  /** The closure for lease maintainer events. */
  static final Guid      LEASE_MAINTAINER_CLOSURE = new Guid();
  
  /** The closure for initializing the audio sink. */
  static final Guid      AUDIO_SINK_CLOSURE       = new Guid();


  // =======================================================================
  //                             Descriptors
  // =======================================================================

  /**
   * The imported event handler descriptor for the audio sink audio
   * handler.
   */
  private static final ImportedDescriptor AUDIO_SINK_AUDIO =
    new ImportedDescriptor("audioSinkAudio",
                           "The audio sink audio imported event handler",
                           null,
                           null,
                           false,
                           true);

  /**
   * The imported event handler descriptor for the audio sink control
   * handler.
   */
  private static final ImportedDescriptor AUDIO_SINK_CONTROL =
    new ImportedDescriptor("audioSinkControl",
                           "The audio sink control imported event handler",
                           null,
                           null,
                           false,
                           true);


  // =======================================================================
  //                           The main window
  // =======================================================================

  /** Implementation of chat's main window. */
  static final class Window extends Application.Window {

    /** The listen panel checkbox. */
    final JCheckBox     listenCheckBox;

    /** The send message panel checkbox. */
    final JCheckBox     sendMsgCheckBox;

    /** The send audio panel checkbox. */
    final JCheckBox     sendAudioCheckBox;

    /** The subscribe panel checkbox. */
    final JCheckBox     subscribeCheckBox;

    /** The collapsed listen panel. */
    final JLabel        listenNoContent;

    /** The collapsed send message panel. */
    final JLabel        sendMsgNoContent;

    /** The collapsed send audio panel. */
    final JLabel        sendAudioNoContent;

    /** The collapsed subscribe panel. */
    final JLabel        subscribeNoContent;
    
    /** The expanded listen panel. */
    final JPanel        listenContent;
 
    /** The expanded send message panel. */
    final JPanel        sendMsgContent;

    /** The expanded send audio panel. */
    final JPanel        sendAudioContent;

    /** The expanded subscribe panel. */
    final JPanel        subscribeContent;

    /** The channel selector for the listen panel. */
    final JComboBox     listenChannel;

    /** The text pane for the listen panel. */
    final JTextPane     listenText;

    /** The scroll pane for the listen panel. */
    final JScrollPane   listenScroll;

    /** The vertical scroll bar for the listen panel. */
    final JScrollBar    listenScrollBar;

    /** The slider for the listen panel. */
    final JSlider       listenVolume;

    /** The mute checkbox for the listen panel. */
    final JCheckBox     listenMute;

    /** The color chooser for the listen panel. */
    JColorChooser       listenColorChooser;
  
    /** The color chooser dialog for the listen panel. */
    JDialog             listenColorDialog;

    /** The channel selector for the send message panel. */
    final JComboBox     sendMsgChannel;

    /** The message field for the send message panel. */
    final JTextField    sendMsgMessage;

    /** The send button for the send message panel. */
    final JButton       sendMsgSend;

    /** The channel selector for the send audio panel. */
    final JComboBox     sendAudioChannel;

    /** The source selection tabbed pane for the send audio panel. */
    final JTabbedPane   sendAudioSource;

    /** The environment for the send audio panel. */
    final JTextField    sendAudioEnvironment;

    /** The add button for the send audio panel. */
    final JButton       sendAudioAdd;

    /** The delete button for the send audio panel. */
    final JButton       sendAudioDelete;

    /** The channel selector for the subscribe panel. */
    final JComboBox     subscribeChannel;

    /** The beep checkbox for the subscribe panel. */
    final JCheckBox     subscribeBeep;

    /** The color icon for the subscribe panel. */
    final SolidIcon     subscribeIcon;
  
    /** The color chooser button for the subscribe panel. */
    final JButton       subscribeColor;

    /** The color chooser for the subscribe panel. */
    JColorChooser       subscribeColorChooser;

    /** The color chooser dialog for the subscribe panel. */
    JDialog             subscribeColorDialog;

    /** The mute checkbox for the subscribe panel. */
    final JCheckBox     subscribeMute;

    /** The add button for the subscribe panel. */
    final JButton       subscribeAdd;

    /** The delete button for the subscribe panel. */
    final JButton       subscribeDelete;

    /**
     * Create a new main window.
     *
     * @param  chat  The chat application component.
     */
    Window(final Chat chat) {
      super(chat, "Chat - " + chat.user);
      //setResizable(false);
      final Container mainContent = getContentPane();

      // Create the panel check boxes.
      listenCheckBox     = new JCheckBox(null, null, chat.listenExpanded);
      sendMsgCheckBox    = new JCheckBox(null, null, chat.sendMsgExpanded);
      sendAudioCheckBox  = new JCheckBox(null, null, chat.sendAudioExpanded);
      subscribeCheckBox  = new JCheckBox(null, null, chat.subscribeExpanded);

      listenCheckBox.setToolTipText("Expand / collapse listen panel");
      sendMsgCheckBox.setToolTipText("Expand / collapse send message panel");
      sendAudioCheckBox.setToolTipText("Expand / collapse send audio panel");
      subscribeCheckBox.setToolTipText("Expand / collapse subscribe panel");

      // Create the item listener to expand / collapse panels.
      ItemListener listener = new ItemListener() {
          public void itemStateChanged(ItemEvent e) {
            final Object     source = e.getItem();
            boolean          expand =
              (ItemEvent.SELECTED == e.getStateChange());
            final int        index;
            final JComponent emptyContent;
            final JComponent content;
            
            // Determine the source of the item event.
            if (source == listenCheckBox) {
              index                  = 2;
              emptyContent           = listenNoContent;
              content                = listenContent;
              chat.listenExpanded    = expand;
            } else if (source == sendMsgCheckBox) {
              index                  = 5;
              emptyContent           = sendMsgNoContent;
              content                = sendMsgContent;
              chat.sendMsgExpanded   = expand;
            } else if (source == sendAudioCheckBox) {
              index                  = 8;
              emptyContent           = sendAudioNoContent;
              content                = sendAudioContent;
              chat.sendAudioExpanded = expand;
            } else {
              index                  = 11;
              emptyContent           = subscribeNoContent;
              content                = subscribeContent;
              chat.subscribeExpanded = expand;
            }
            
            // Collapse/expand the corresponding panel.
            mainContent.remove(index);
            if (expand) {
              emptyContent.setVisible(false);
              mainContent.add(content, index);
              content.setVisible(true);
            } else {
              content.setVisible(false);
              mainContent.add(emptyContent, index);
              emptyContent.setVisible(true);
            }
            
            // Adjust the default button.
            setDefaultButton();
            
            // Pack the window.
            pack();
          }
        };

      // Add item listener to all panel checkboxes.
      listenCheckBox.addItemListener(listener);
      sendMsgCheckBox.addItemListener(listener);
      sendAudioCheckBox.addItemListener(listener);
      subscribeCheckBox.addItemListener(listener);
      
      // Create empty panel content.
      listenNoContent    = new JLabel();
      sendMsgNoContent   = new JLabel();
      sendAudioNoContent = new JLabel();
      subscribeNoContent = new JLabel();

      // Create listen panel content.
      listenChannel   = new JComboBox();
      listenText      = new JTextPane();
      listenScroll    = new
        JScrollPane(listenText,
                    ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                    ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
      listenScrollBar = listenScroll.getVerticalScrollBar();
      listenVolume    = new JSlider();
      listenVolume.setValue(chat.volume);
      listenMute      = new JCheckBox("Mute Audio");
      listenMute.setSelected(chat.muteAudio);
      listenMute.addItemListener(new ItemListener() {
          public void itemStateChanged(ItemEvent e) {
            chat.muteAudio = (ItemEvent.SELECTED == e.getStateChange());
          }
        });
      listenContent   = createListenContent();

      // Create send message panel contents.
      String[]     labels = new String[] { "Channel", "Message" };
      int[]        types  = new int[]    { GuiUtilities.ENTRY_COMBO_BOX,
                                           GuiUtilities.ENTRY_TEXT_FIELD };
      JComponent[] comps  = GuiUtilities.createSimpleGrid(labels, types, 0);
      sendMsgChannel      = (JComboBox)comps[1];
      sendMsgMessage      = (JTextField)comps[2];
      sendMsgSend         = new JButton("Send");
      sendMsgContent      = createSendMessageContent((JPanel)comps[0]);

      // Create send audio panel contents.
      sendAudioChannel     = new JComboBox();
      sendAudioSource      = new JTabbedPane();
      sendAudioEnvironment = new JTextField();
      sendAudioAdd         = new JButton("Add");
      sendAudioDelete      = new JButton("Delete");
      sendAudioContent     = createSendAudioContent();

      // Create subscribe panel contents.
      subscribeChannel = new JComboBox();
      subscribeBeep    = new JCheckBox("Beep");
      subscribeIcon    = new SolidIcon(ICON_WIDTH, ICON_HEIGHT, DEFAULT_COLOR);
      subscribeColor   = new JButton("Color", subscribeIcon);
      subscribeMute    = new JCheckBox("Mute");
      subscribeAdd     = new JButton("Add");
      subscribeDelete  = new JButton("Delete");
      subscribeContent = createSubscribeContent();

      // Patch selection color for the text pane.
      listenText.setSelectionColor(sendMsgMessage.getSelectionColor());
      
      // Set up main window.
      mainContent.setLayout(new BoxLayout(mainContent, BoxLayout.Y_AXIS));
      mainContent.add(new JLabel());
      mainContent.add(createHeading(listenCheckBox,
                                    createHeading("Listen"),
                                    true));
      mainContent.add(listenContent);
      mainContent.add(new JSeparator());
      mainContent.add(createHeading(sendMsgCheckBox,
                                    createHeading("Send Message"),
                                    false));
      mainContent.add(sendMsgContent);
      mainContent.add(new JSeparator());
      mainContent.add(createHeading(sendAudioCheckBox,
                                    createHeading("Send Audio"),
                                    false));
      mainContent.add(sendAudioContent);
      mainContent.add(new JSeparator());
      mainContent.add(createHeading(subscribeCheckBox,
                                    createHeading("Subscribe"),
                                    false));
      mainContent.add(subscribeContent);

      // Determine the main window's maximum width with the listen panel
      // expanded and then add a rigid area to force that width.
      pack();
      mainContent.remove(0);
      mainContent.add(Box.createRigidArea(new Dimension(getWidth(), 0)), 0);

      // Clutch to get the layout right.
      sendAudioEnvironment.setPreferredSize(new
        Dimension(sendAudioEnvironment.getWidth(),
                  sendMsgMessage.getHeight()));
      pack();

      // Collapse panels as necessary.
      boolean repack = false;

      if (! chat.listenExpanded) {
        mainContent.remove(2);
        mainContent.add(listenNoContent, 2);
        repack = true;
      }
      if (! chat.sendMsgExpanded) {
        mainContent.remove(5);
        mainContent.add(sendMsgNoContent, 5);
        repack = true;
      }
      if (! chat.sendAudioExpanded) {
        mainContent.remove(8);
        mainContent.add(sendAudioNoContent, 8);
        repack = true;
      }
      if (! chat.subscribeExpanded) {
        mainContent.remove(11);
        mainContent.add(subscribeNoContent, 11);
        repack = true;
      }

      if (repack) {
        pack();
      }
      
      // Adjust the default button.
      setDefaultButton();

      // Extract the default background color on the first run.
      if (null == chat.backgroundColor) {
        chat.backgroundColor = listenText.getBackground();
      }

      // Set the background color.
      listenText.setBackground(chat.backgroundColor);

      // Fill in the channel state. Note that since we are only reading
      // the channels map, there is no need for synchronization.
      listenChannel.addItem(ALL_CHANNELS);

      Iterator       iter     = chat.channels.values().iterator();
      StyledDocument document = listenText.getStyledDocument();
      ChannelState   channel;

      while (iter.hasNext()) {
        channel       = (ChannelState)iter.next();
        
        // Fill in the channel's style.
        channel.style = document.addStyle(channel.name, null);
        StyleConstants.setForeground(channel.style, channel.color);
        
        // Add the channel to the channel combo boxes.
        listenChannel.addItem(channel);
        sendMsgChannel.addItem(channel);
        subscribeChannel.addItem(channel);
      }

      // Redisplay messages from history buffer.
      if (-1 != chat.historyTail) {
        // Create a default style.
        Style defaultStyle = document.addStyle(ALL_CHANNELS, null);
        StyleConstants.setForeground(defaultStyle, DEFAULT_COLOR);

        int idx = chat.historyHead;
        while (true) {
          TextMessage tm    = chat.history[idx];
          channel           = (ChannelState)chat.channels.get(tm.channel);
          Style       style = ((null == channel)? defaultStyle : channel.style);

          try {
            document.insertString(document.getLength(), format(tm), style);
          } catch (BadLocationException x) {
            throw new Bug("Unexpected exception (" + x + ")");
          }

          idx++;
          if (chat.history.length <= idx) {
            idx = 0;
          }

          if (idx == chat.historyTail) {
            break;
          }
        }
      }

      // Fill in the audio sender state. Note that since we are only
      // reading the sendes map, there is no need for synchronization.
      iter = chat.senders.values().iterator();

      while (iter.hasNext()) {
        sendAudioChannel.addItem(iter.next());
      }
      
      // Set the channel currently being listened to.
      listenChannel.setSelectedItem((null == chat.currentChannel)?
                                    ALL_CHANNELS :
                                    chat.channels.get(chat.currentChannel));
      
      // Set the selected item for the send message panel channel.
      channel = (ChannelState)chat.channels.get(chat.sendMsgChannel);
      sendMsgChannel.setSelectedItem(((null == channel)?
                                      (Object)chat.sendMsgChannel :
                                      (Object)channel));

      // Set the selected item for the send audio panel channel.
      AudioSender sender = (AudioSender)chat.senders.get(chat.sendAudioChannel);

      if (null == sender) {
        sendAudioChannel.setSelectedItem(chat.sendAudioChannel);
        sendAudioSource.setSelectedIndex(AUDIO_SILENCE);
      } else {
        sendAudioChannel.setSelectedItem(sender);
        sendAudioSource.setSelectedIndex(sender.type);
        if (AUDIO_ENVIRONMENT == sender.type) {
          sendAudioEnvironment.setText(sender.path);
        }
      }
      
      // Set the selected item for the subscribe panel channel.
      channel = (ChannelState)chat.channels.get(chat.subscribeChannel);
      
      if (null == channel) {
        subscribeChannel.setSelectedItem(chat.subscribeChannel);
      } else {
        subscribeChannel.setSelectedItem(channel);
        subscribeBeep.setSelected(channel.beep);
        subscribeMute.setSelected(channel.mute);
        subscribeIcon.setColor(channel.color);
      }
      
      // Set action listeners.
      listenChannel.addActionListener(chat);
      sendMsgChannel.addActionListener(chat);
      sendMsgSend.addActionListener(chat);
      subscribeChannel.addActionListener(chat);
      subscribeAdd.addActionListener(chat);
      subscribeDelete.addActionListener(chat);

      // If running with audio, set up the right listeners. Otherwise,
      // disable the audio UI elements.
      if (chat.runningWithAudio) {
        listenVolume.addChangeListener(chat);
        sendAudioChannel.addActionListener(chat);
        sendAudioAdd.addActionListener(chat);
        sendAudioDelete.addActionListener(chat);
      } else {
        listenVolume.setEnabled(false);
        listenMute.setEnabled(false);
        sendAudioChannel.setEnabled(false);
        sendAudioSource.setEnabled(false);
        sendAudioAdd.setEnabled(false);
        sendAudioDelete.setEnabled(false);
      }
    }
    
    /**
     * Create the listen panel contents.
     * 
     * @return  The listen panel contents.
     */
    JPanel createListenContent() {
      // Set up channel selector.
      listenChannel.setActionCommand(ACTION_LISTEN);

      JLabel label = new JLabel("Channel");
      label.setLabelFor(listenChannel);

      Box channel = new Box(BoxLayout.X_AXIS);
      channel.add(label);
      channel.add(Box.createRigidArea(X5));
      channel.add(listenChannel);
      
      // Set up text pane.
      listenText.setEditable(false);

      FontMetrics metrics = listenText.getFontMetrics(listenText.getFont());
      listenScroll.setPreferredSize(new
        Dimension(TEXT_WIDTH  * metrics.charWidth('m'),
                  TEXT_HEIGHT * metrics.getHeight()));

      // Set up listen options.
      label = new JLabel("Volume");
      label.setLabelFor(listenVolume);

      listenVolume.setMinorTickSpacing(25);
      listenVolume.setPaintTicks(true);
      Dimension dim = listenVolume.getPreferredSize();
      dim.width     = 80;
      listenVolume.setPreferredSize(dim);

      listenMute.setMnemonic(KeyEvent.VK_M);
      listenMute.setToolTipText("Mute all audio");
      
      JButton color = new JButton("Bg. Color");
      color.setMnemonic(KeyEvent.VK_B);

      final ActionListener actionListener = new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            // Dispose of the dialog.
            listenColorDialog.dispose();
            listenColorDialog = null;

            if ("OK".equals(e.getActionCommand())) {
              // Adjust the background color.
              Color c = listenColorChooser.getColor();

              ((Chat)app).backgroundColor = c;
              listenText.setBackground(c);
              listenText.repaint();
            }

            listenColorChooser = null;
          }
        };
      final WindowAdapter windowAdapter = new WindowAdapter() {
          public void windowClosing(WindowEvent e) {
            // Release resources.
            listenColorChooser = null;
            listenColorDialog  = null;
          }
        };
      color.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            // Create the color chooser if necessary.
            if (null == listenColorDialog) {
              // Create the color chooser.
              listenColorChooser = new JColorChooser();
              
              // Create the color chooser dialog.
              listenColorDialog = 
                JColorChooser.createDialog(Window.this,
                                           "Pick a Background Color",
                                           false,
                                           listenColorChooser,
                                           actionListener,
                                           actionListener);
              listenColorDialog.setResizable(false);
              listenColorDialog.setDefaultCloseOperation(
                                                    JDialog.DISPOSE_ON_CLOSE);
              listenColorDialog.addWindowListener(windowAdapter);
            }
            
            // Show color chooser.
            listenColorChooser.setColor(listenText.getBackground());
            listenColorDialog.show();
          }
        });
      
      Box options = new Box(BoxLayout.X_AXIS);
      options.add(label);
      options.add(Box.createRigidArea(X5));
      options.add(listenVolume);
      options.add(Box.createRigidArea(X15));
      options.add(listenMute);
      options.add(Box.createHorizontalGlue());
      options.add(color);
      
      // Set up the overall panel.
      JPanel panel = new JPanel();
      
      panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
      panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
      
      panel.add(channel);
      panel.add(Box.createRigidArea(Y5));
      panel.add(listenScroll);
      panel.add(Box.createRigidArea(Y5));
      panel.add(options);
      
      return panel;
    }
  
    /**
     * Create the send message panel contents.
     *
     * @param   top  The top panel.
     * @return       The send message panel contents.
     */
    JPanel createSendMessageContent(JPanel top) {
      // Set up the channel selector.
      sendMsgChannel.setEditable(true);
      sendMsgChannel.setActionCommand(ACTION_SELECT_SEND);
      
      // Set up the buttons.
      sendMsgSend.setMnemonic(KeyEvent.VK_S);
      sendMsgSend.setActionCommand(ACTION_SEND);

      JButton clear = new JButton("Clear");
      clear.setMnemonic(KeyEvent.VK_C);
      clear.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            sendMsgMessage.setText(null);
          }
        });
      
      JPanel buttons = new JPanel();
      buttons.add(sendMsgSend);
      buttons.add(clear);
      
      // Set up the panel.
      JPanel panel = new JPanel();
      
      panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
      panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 5));
      
      panel.add(top);
      panel.add(buttons);
      
      return panel;
    }

    /**
     * Create the send audio panel contents.
     *
     * @return  The send audio panel contents.
     */
    JPanel createSendAudioContent() {
      // Set up channel combobox.
      JLabel label = new JLabel("Channel");
      label.setLabelFor(sendAudioChannel);

      sendAudioChannel.setEditable(true);
      sendAudioChannel.setActionCommand(ACTION_SELECT_AUDIO);

      Box channel = new Box(BoxLayout.X_AXIS);
      channel.add(label);
      channel.add(Box.createRigidArea(X5));
      channel.add(sendAudioChannel);

      // Set up source panel.
      label = new JLabel("It's oh so quiet");
      label.setAlignmentX(JComponent.CENTER_ALIGNMENT);

      JPanel tab = new JPanel();
      tab.setLayout(new BoxLayout(tab, BoxLayout.Y_AXIS));
      tab.add(Box.createVerticalGlue());
      tab.add(label);
      tab.add(Box.createVerticalGlue());
      sendAudioSource.addTab("Silence", tab);

      label = new JLabel("Send sound from microphone");
      label.setAlignmentX(JComponent.CENTER_ALIGNMENT);
      
      tab = new JPanel();
      tab.setLayout(new BoxLayout(tab, BoxLayout.Y_AXIS));
      tab.add(Box.createVerticalGlue());
      tab.add(label);
      tab.add(Box.createVerticalGlue());
      sendAudioSource.addTab("Microphone", tab);

      // Clutch to get the layout right.
      sendAudioEnvironment.setPreferredSize(new Dimension(0, 0));

      label = new JLabel("Send sound tuples from environment");

      tab = new JPanel();
      tab.setLayout(new BoxLayout(tab, BoxLayout.Y_AXIS));
      tab.add(label);
      tab.add(sendAudioEnvironment);
      sendAudioSource.addTab("Environment", tab);

      // Set up buttons.
      sendAudioAdd.setToolTipText("Add / change audio sender for channel");
      sendAudioAdd.setActionCommand(ACTION_ADD_AUDIO);

      sendAudioDelete.setToolTipText("Delete audio sender for channel");
      sendAudioDelete.setActionCommand(ACTION_DELETE_AUDIO);

      JPanel buttons = new JPanel();
      buttons.add(sendAudioAdd);
      buttons.add(sendAudioDelete);

      // Set up the panel.
      JPanel panel = new JPanel();

      panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
      panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 5));

      panel.add(channel);
      panel.add(Box.createRigidArea(Y5));
      panel.add(sendAudioSource);
      panel.add(buttons);

      return panel;
    }
    
    /**
     * Create the subscribe panel contents.
     *
     * @return  The subscribe panel contents.
     */
    JPanel createSubscribeContent() {
      // Set up channel combobox.
      JLabel label = new JLabel("Channel");
      label.setLabelFor(subscribeChannel);

      subscribeChannel.setEditable(true);
      subscribeChannel.setActionCommand(ACTION_SELECT_SUBSCRIBE);
      
      Box channel = new Box(BoxLayout.X_AXIS);
      channel.add(label);
      channel.add(Box.createRigidArea(X5));
      channel.add(subscribeChannel);
      
      // Set up options.
      subscribeBeep.setMnemonic(KeyEvent.VK_E);
      subscribeBeep.setToolTipText("Beep for new text messages");

      subscribeMute.setMnemonic(KeyEvent.VK_U);
      subscribeMute.setToolTipText("Mute this channel");

      subscribeColor.setMnemonic(KeyEvent.VK_O);
      subscribeColor.setToolTipText("Color for text messages");

      final ActionListener actionListener = new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            // Dispose of the dialog.
            subscribeColorDialog.dispose();
            subscribeColorDialog = null;

            if ("OK".equals(e.getActionCommand())) {
              subscribeIcon.setColor(subscribeColorChooser.getColor());
              subscribeColor.repaint();
            }

            subscribeColorChooser = null;
          }
        };
      final WindowAdapter windowAdapter = new WindowAdapter() {
          public void windowClosing(WindowEvent e) {
            // Release resources.
            subscribeColorChooser = null;
            subscribeColorDialog  = null;
          }
        };
      subscribeColor.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            // Create the color chooser if necessary.
            if (null == subscribeColorDialog) {
              // Create the color chooser.
              subscribeColorChooser = new JColorChooser();
              
              // Create the color chooser dialog.
              subscribeColorDialog = 
                JColorChooser.createDialog(Window.this,
                                           "Pick a Text Color",
                                           false,
                                           subscribeColorChooser,
                                           actionListener,
                                           actionListener);
              subscribeColorDialog.setResizable(false);
              subscribeColorDialog.setDefaultCloseOperation(
                                                    JDialog.DISPOSE_ON_CLOSE);
              subscribeColorDialog.addWindowListener(windowAdapter);
            }
            
            // Show color chooser.
            subscribeColorChooser.setColor(subscribeIcon.getColor());
            subscribeColorDialog.show();
          }
        });
      
      // Create options panel.
      JPanel options = new JPanel();
      options.add(subscribeBeep);
      options.add(subscribeColor);
      options.add(Box.createRigidArea(X5));
      options.add(subscribeMute);
      
      // Set up buttons.
      subscribeAdd.setMnemonic(KeyEvent.VK_A);
      subscribeAdd.setToolTipText("Add / change subscription for channel");
      subscribeAdd.setActionCommand(ACTION_ADD_SUBSCRIPTION);

      subscribeDelete.setMnemonic(KeyEvent.VK_D);
      subscribeDelete.setToolTipText("Delete subscription for channel");
      subscribeDelete.setActionCommand(ACTION_DELETE_SUBSCRIPTION);
      
      JPanel buttons = new JPanel();
      buttons.add(subscribeAdd);
      buttons.add(subscribeDelete);
      
      // Set up the panel.
      JPanel panel = new JPanel();
      
      panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
      panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 5));
      
      panel.add(channel);
      panel.add(options);
      panel.add(buttons);
      
      return panel;
    }

    /**
     * Create a new label for a panel heading with the specified text.
     *
     * @param   text  The text for the new panel heading label.
     * @return        The new panel heading label.
     */
    JLabel createHeading(String text) {
      JLabel label = new JLabel("<html><font face=\"arial,helvetica," +
                                "sans-serif\" color=\"black\"><i><b>" + 
                                text + "</b></i></font></html>");
      
      return label;
    }
    
    /**
     * Create a new panel heading with the specified checkbox and label.
     *
     * @param   box     The checkbox.
     * @param   label   The label.
     * @param   source  The flag for whether the panel heading includes
     *                  a drag and drop location change source.
     * @return          The new panel heading.
     */
    JPanel createHeading(JCheckBox box, JLabel label, boolean source) {
      JPanel panel = new JPanel();
      panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
      
      panel.add(Box.createRigidArea(X5));
      panel.add(box);
      panel.add(label);
      if (source) {
        panel.add(Box.createHorizontalGlue());
        
        Environment env         = app.getEnvironment();
        JLabel      sourceLabel = 
          GuiUtilities.createLocationSource(env.getId());
        GuiUtilities.addUserPopup(sourceLabel, env);
        panel.add(sourceLabel);
        panel.add(Box.createRigidArea(X2));
      }
      
      return panel;
    }

    /** Set the default button. */
    void setDefaultButton() {
      JRootPane root = getRootPane();
      
      if (sendMsgCheckBox.isSelected()) {
        root.setDefaultButton(sendMsgSend);
      } else if (subscribeCheckBox.isSelected()) {
        root.setDefaultButton(subscribeAdd);
      } else {
        root.setDefaultButton(null);
      }
    }

  }


  // =======================================================================
  //                           The channel state
  // =======================================================================

  /** The state of a currently subscribed channel. */
  final class ChannelState extends AbstractHandler {

    /**
     * The name of the channel.
     *
     * @serial  Must not be <code>null</code>.
     */
    final     String          name;

    /**
     * The flag for whether to beep for new text messages.
     *
     * @serial
     */
    boolean                   beep;

    /**
     * The flag for whether to mute the channel.
     *
     * @serial
     */
    boolean                   mute;

    /**
     * The current color for the text messages.
     *
     * @serial  Must not be <code>null</code>.
     */
    Color                     color;

    /**
     * The query for sending messages to this channel.
     *
     * @serial  Must be a valid discovered resource for sending
     *          messages to this channel.
     */
    final DiscoveredResource  resource;

    /**
     * The style for text messages, which simply represents the
     * current color.
     */
    transient Style           style;

    /**
     * The lease maintainer for exporting the appropriate event
     * handler to discovery.
     */
    transient LeaseMaintainer leaseMaintainer;

    /**
     * Create a new channel state descriptor for the channel with the
     * specified name.
     *
     * @param   name  The name of the channel.
     */
    ChannelState(String name) {
      this.name = name;
      color     = DEFAULT_COLOR;
      resource  = new
        DiscoveredResource(new
          Query(IS_CHANNEL, Query.BINARY_AND, new
            Query("name", Query.COMPARE_EQUAL, name)), true);
    }

    /** Handle the specified event. */
    protected boolean handle1(Event e) {
      // We only handle messages sent as remote events and exceptional events.
      if (e instanceof RemoteEvent) {
        e = ((RemoteEvent)e).event;

      } else if ((e instanceof ExceptionalEvent) &&
                 LEASE_MAINTAINER_CLOSURE.equals(e.closure)) {
        signalError("Internal error in lease maintainer for channel " + name +
                    ":\n" + ((ExceptionalEvent)e).x);
        return true;

      } else {
        return false;
      }
      if (! (e instanceof Message)) {
        return false;
      }

      Message m  = (Message)e;

      // Make sure that we are listening to this channel and it isn't muted.
      String ch = currentChannel;
      if (((null != ch) && (! ch.equals(m.channel))) || mute) {
        return true;
      }

      // Process the message.
      if (e instanceof TextMessage) {
        TextMessage tm = (TextMessage)e;

        // Clear the source and closure.
        tm.source  = null;
        tm.closure = null;

        // Add the text message to the history, then get the document
        // and window.
        final StyledDocument doc;
        final Window         win;
        synchronized (lock) {
          if (ACTIVE != status) {
            return true;
          }

          // Add the message to the history.
          if (0 > historyTail) {
            historyTail = 0;
            historyHead = 0;
          } else if (historyTail == historyHead) {
            historyHead++;
            if (history.length <= historyHead) {
              historyHead = 0;
            }
          }

          history[historyTail++] = tm;
          if (history.length <= historyTail) {
            historyTail = 0;
          }

          // Get the scroll bar and the document.
          doc = document;
          win = (Window)mainWindow;
        }

        // Display the message.
        final String msg = format(tm);
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
              if (beep) {
                GuiUtilities.beep();
              }
              try {
                doc.insertString(doc.getLength(), msg, style);
              } catch (BadLocationException x) {
                throw new Bug("Unexpected exception (" + x + ")");
              }
              win.listenScrollBar.setValue(win.listenScrollBar.getMaximum());
            }
          });
        
        return true;

      } else if (e instanceof AudioMessage) {
        // Make sure we are active, running with audio, and haven't
        // muted audio.
        if (muteAudio || (! runningWithAudio) || (ACTIVE != status)) {
          return true;
        }

        // Send the message to the audio sink.
        audioSinkAudio.handle(m);
        return true;
      }
   
      return false;
    }

    /**
     * Return a string representation for this channel state
     * descriptor.
     */
    public String toString() {
      return name;
    }

  }


  // =======================================================================
  //                             The audio sender
  // =======================================================================
  
  /** An audio sender. */
  final class AudioSender extends AbstractHandler {

    /**
     * The name of the channel.
     *
     * @serial  Must not be <code>null</code>.
     */
    final String             name;

    /**
     * The query for sending messages to this channel.
     *
     * @serial  Must be a valid discovered resource for sending
     *          messages to this channel.
     */
    final DiscoveredResource resource;
    
    /**
     * The type of audio source.
     *
     * @serial  Must be <code>AUDIO_SILENCE</code>,
     *          <code>AUDIO_MICROPHONE</code>, or
     *          <code>AUDIO_ENVIRONMENT</code>.
     */
    int                      type;

    /**
     * The relative path for the environment storing sound tuples.
     *
     * @serial
     */
    String                   path;

    /**
     * The imported event handler for handling audio source control
     * messages.
     *
     * @serial  Must not be <code>null</code>.
     */
    Component.Importer       control;

    /**
     * The actual audio source. This field can only be modified while
     * holding chat's lock.
     *
     * @serial
     */
    AudioSource              source;

    /**
     * The audio sender's operation. For already existing audio
     * senders, this field can only be modified while holding chat's
     * lock.
     */
    transient Operation      operation2;

    /**
     * Create a new audio sender. This constructor allocates the
     * internal data structures for an audio sender.
     *
     * @param   channel  The audio sender's channel.
     */
    AudioSender(ChannelState channel) {
      name     = channel.name;
      resource = channel.resource;
    }

    /**
     * Create a new audio sender. This constructor allocates the
     * internal data structures for an audio sender.
     *
     * @param   name  The channel name.
     */
    AudioSender(String name) {
      this.name = name;
      resource  = new
        DiscoveredResource(new
          Query(IS_CHANNEL, Query.BINARY_AND, new
            Query("name", Query.COMPARE_EQUAL, name)), true);
    }

    /**
     * Acquire this audio sender. This method declares two new event
     * handlers for the chat component: an imported event handler to
     * control an audio source and an exported event handler (backed
     * by this audio sender) to send audio to this audio sender's
     * channel. The imported event handler has the name of this audio
     * sender's channel appended with "Control", and the exported
     * event handler has the name of this audio sender's channel
     * appended with "Audio". This method must be called while holding
     * chat's lock (or while chat is being created itself).
     */
    void acquire() {
      control = declareImported(new
        ImportedDescriptor(name + "Control",
                           "An audio source control event handler",
                           null, null, false, true));
      declareExported(new
        ExportedDescriptor(name + "Audio",
                           "An audio source audio event handler",
                           null, null, false), this);
    }

    /**
     * Connect to a microphone audio source. This method must be called
     * while holding chat's lock.
     *
     * @throws  IllegalStateException
     *                Signals that this audio sender already has an
     *                audio source.
     */
    void connect() {
      if (null != source) {
        throw new IllegalStateException("Audio sender already has source");
      } else {
        type = AUDIO_MICROPHONE;
        connect(new AudioSource(getEnvironment(), user, name));
      }
    }

    /**
     * Connect to an audio source playing sound tuples from the
     * environment with the specified path. This method must be called
     * while holding chat's lock.
     *
     * @param   path  The environment path.
     * @throws  IllegalStateException
     *                Signals that this audio sender already has an
     *                audio source.
     */
    void connect(String path) {
      if (null != source) {
        throw new IllegalStateException("Audio sender already has source");
      } else {
        type      = AUDIO_ENVIRONMENT;
        this.path = path;
        connect(new AudioSource(getEnvironment(), user, name, path));
      }
    }

    /**
     * Connect to the specified audio source. This method must be
     * called while holding chat's lock. This audio sender must not be
     * already connected to an audio source and its type and path
     * fields must have been initialized to the correct values for the
     * specified audio source.
     *
     * @param  source  The audio source to connect to.
     */
    private void connect(AudioSource source) {
      this.source = source;

      link(name + "Control", "control", source);
      source.link("audio", name + "Audio", Chat.this);
      source.link("request", "request", getEnvironment());
    }

    /**
     * Disconnect this audio sender's audio source. This method stops
     * the current audio source and unlinks it, reseting this audio
     * sender to silence as well. Calling this method on an alreaddy
     * silenced audio sender has no effect. This method must be called
     * while holding chat's lock.
     */
    void disconnect() {
      // Any source to silence?
      if (null == source) {
        return;
      }

      // We can safely use a synchronous invocation, because the same
      // thread performs the event handling in the audio source.
      if (runningWithAudio) {
        Synchronous.invoke(control, new
          AudioSource.ControlEvent(null, null, AudioSource.ControlEvent.STOP));
      }

      // Unlink the audio source.
      unlink(name + "Control", "control", source);
      source.unlink("audio", name + "Audio", Chat.this);
      source.unlink("request", "request", getEnvironment());

      // Reset the internal state.
      source = null;
      type   = AUDIO_SILENCE;
      path   = null;
    }

    /**
     * Release this audio sender. This method undeclares the event
     * handlers for this audio sender's channel. The audio source
     * behind this audio sender (if it exists) must be stopped
     * separately. This method must be called before deallocating this
     * audio sender, and it must be called while holding chat's lock.
     */
    void release() {
      undeclare(name + "Control");
      undeclare(name + "Audio");
    }

    /** Handle the specified event. */
    protected boolean handle1(Event e) {
      if (e instanceof TextMessage) {
        // Fix the source, wrap the text message in a remote event,
        // and send it off.
        e.source = remoteMessage;
        request.handle(new RemoteEvent(message, null, resource, e));
        return true;

      } else if (e instanceof AudioMessage) {
        // Fix the source, wrap the audio message in a remote event,
        // and send it off.
        e.source = remoteMessage;
        request.handle(new RemoteEvent(message, null, resource, e, true));
        return true;

      } else if (e instanceof ExceptionalEvent) {
        // The audio source is acting up. We silence it as a result.
        signalError("Silencing audio source " + name + " due to error:\n" +
                    ((ExceptionalEvent)e).x);
        synchronized (lock) {
          disconnect();
        }
        return true;
      }

      return false;
    }

    /** Return a string representation for this audio sender. */
    public String toString() {
      return name;
    }

  }


  // =======================================================================
  //                        The message event handler
  // =======================================================================
  
  /** The event handler for supporting message processing. */
  final class MessageHandler extends AbstractHandler {

    /** Handle the specified event. */
    protected boolean handle1(Event e) {
      if (e instanceof RemoteEvent) {
        e = ((RemoteEvent)e).event;

        if ((e instanceof ExceptionalEvent) &&
            (((ExceptionalEvent)e).x instanceof UnknownResourceException)) {
          // Nobody is listening. We feel alone.
          return true;
        }

      } else if ((e instanceof ExceptionalEvent) &&
                 LEASE_MAINTAINER_CLOSURE.equals(e.closure)) {
        signalError("Internal error in lease maintainer for message " +
                    "handler:\n" + ((ExceptionalEvent)e).x);
        return true;

      }

      return false;
    }

  }


  // =======================================================================
  //                           The startup handler
  // =======================================================================

  /** The startup handler. */
  final class StartupHandler extends AbstractHandler {

    /**
     * The flag for whether the audio sink has been initialized.
     *
     * @serial
     */
    boolean  initAudioSink;

    /**
     * The iterator over the audio senders.
     *
     * @serial  Must be <code>null</code> or a valid iterator over audio
     *          senders.
     */
    Iterator senderIterator;

    /**
     * The iterator over the channel state descriptors.
     *
     * @serial  Must be a valid iterator over channel state descriptors.
     */
    Iterator channelIterator;

    /** Create a new startup handler. */
    StartupHandler() {
      channelIterator  = channels.values().iterator();
    }

    /** Handle the specified event. */
    protected boolean handle1(Event e) {

      if (e instanceof BindingResponse) {
        // We successfully exported an event handler. Let's make sure
        // it stays exported.
        BindingResponse br = (BindingResponse)e;

        if (null == br.closure) {
          synchronized (lock) {
            if (INACTIVE == status) {
              LeaseMaintainer.cancel(br.lease);
              return true;
            }

            remoteMessage           = (RemoteReference)br.resource;
            remoteMessageMaintainer = new
              LeaseMaintainer(br.lease, br.duration, message,
                              LEASE_MAINTAINER_CLOSURE, timer);
          }

          // Next step in startup process.
          next();
          return true;

        } else if (br.closure instanceof ChannelState) {
          ChannelState channel = (ChannelState)br.closure;

          synchronized (lock) {
            if (INACTIVE == status) {
              LeaseMaintainer.cancel(br.lease);
              return true;
            }

            channel.leaseMaintainer = new
              LeaseMaintainer(br.lease, br.duration, channel,
                              LEASE_MAINTAINER_CLOSURE, timer);
          }

          // Next step in startup process.
          next();
          return true;

        } else {
          return false;
        }

      } else if ((e instanceof AudioSink.ControlEvent) &&
                 (AudioSink.ControlEvent.STARTED ==
                  ((AudioSink.ControlEvent)e).type)) {
        // We are running with audio.
        runningWithAudio = true;
        senderIterator   = senders.values().iterator();

        // Next step in startup process.
        next();
        return true;

      } else if ((e instanceof AudioSource.ControlEvent) &&
                 (AudioSource.ControlEvent.STARTED ==
                  ((AudioSource.ControlEvent)e).type)) {
        // Next step in startup process.
        next();
        return true;

      } else if (e instanceof ExceptionalEvent) {
        if ((null == e.closure) ||
            (e.closure instanceof ChannelState)) {
          // Something went fatally wrong. We won't run.
          if (null == e.closure) {
            signalError("Unable to run chat for " + user + ":\n" +
                        ((ExceptionalEvent)e).x);
          } else {
            signalError("Unable to run chat for " + user + ":\n" +
                        ((ExceptionalEvent)e).x +
                        "\nwhile exporting event handler for channel " +
                        ((ChannelState)e.closure).name);
          }

          // Release all resources.
          release();

          // Terminate.
          request.handle(new
            EnvironmentEvent(main, null, EnvironmentEvent.STOPPED,
                             getEnvironment().getId()));

          // Done.
          return true;

        } else if (AUDIO_SINK_CLOSURE.equals(e.closure)) {
          // The audio sink cannot be started. We are running without audio.
          signalError("Disabling audio due to initialization error:\n" +
                      ((ExceptionalEvent)e).x);
          runningWithAudio = false;

          // Next step in startup process.
          next();
          return true;

        } else if (e.closure instanceof AudioSender) {
          AudioSender sender = (AudioSender)e.closure;

          // The audio source is acting up. We silence it as a result.
          signalError("Silencing audio source " + sender.name +
                      " due to error:\n" + ((ExceptionalEvent)e).x);
          sender.disconnect();

          // Next step in startup process.
          next();
          return true;

        } else {
          return false;
        }

      }

      return false;
    }

    /** Process the next step in the startup process. */
    private void next() {
      // Do we need to start the audio sink?
      if (! initAudioSink) {
        initAudioSink     = true;
        operation.request = audioSinkControl;
        operation.handle(new
          AudioSink.ControlEvent(null, AUDIO_SINK_CLOSURE,
                                 AudioSink.ControlEvent.START));
        return;
      }

      // Any more audio senders to start?
      if (null != senderIterator) {
        boolean     initSender = false;
        AudioSender sender     = null;

        while (senderIterator.hasNext()) {
          sender = (AudioSender)senderIterator.next();

          if (null != sender.source) {
            initSender = true;
            break;
          }
        }

        if (initSender) {
          operation.request = sender.control;
          operation.handle(new
            AudioSource.ControlEvent(null, sender,
                                     AudioSource.ControlEvent.START));
          return;

        } else {
          // No more audio senders to start.
          senderIterator = null;
          // Fall through.
        }
      }
      
      // Any more channels to export?
      if (channelIterator.hasNext()) {
        ChannelState channel = (ChannelState)channelIterator.next();

        // Export the channel state under the channel descriptor.
        operation.request = request;
        operation.handle(new
          BindingRequest(null, channel, new
            RemoteDescriptor(channel, new
              Channel(channel.name)), Duration.FOREVER));

        return;
      }

      // We are done with the startup process. Reset the operation's
      // request and continuation handlers.
      operation.request      = request;
      operation.continuation = new AddChannelHandler();

      // Display the main window.
      start();
    }
  }


  // =======================================================================
  //                       The add channel handler
  // =======================================================================

  /** The add channel handler. */
  final class AddChannelHandler extends AbstractHandler {

    /** Handle the specified event. */
    protected boolean handle1(Event e) {
      // Make sure the event has the right closure.
      if (! (e.closure instanceof ChannelState)) {
        return false;
      }

      // Get the channel state descriptor.
      final ChannelState channel = (ChannelState)e.closure;

      if (e instanceof BindingResponse) {
        // We successfully exported the channel state.
        BindingResponse br = (BindingResponse)e;

        // Make sure the channel stays exported.
        final StyledDocument doc;
        final AudioSender    sender;
        final JComboBox      listenChannel;
        final JComboBox      sendMsgChannel;
        final JComboBox      sendAudioChannel;
        final JComboBox      subscribeChannel;
        
        synchronized (lock) {
          if (INACTIVE == status) {
            LeaseMaintainer.cancel(br.lease);
            return true;
          }

          channel.leaseMaintainer = new LeaseMaintainer(br.lease, br.duration,
                                                        this, null, timer);
          channels.put(channel.name, channel);

          doc              = document;
          listenChannel    = ((Window)mainWindow).listenChannel;
          sendMsgChannel   = ((Window)mainWindow).sendMsgChannel;
          sendAudioChannel = ((Window)mainWindow).sendAudioChannel;
          subscribeChannel = ((Window)mainWindow).subscribeChannel;

          // Create the corresponding audio sender descriptor if
          // necessary.
          if (senders.containsKey(channel.name)) {
            sender = null;
          } else {
            sender = new AudioSender(channel);
            sender.acquire();
            senders.put(channel.name, sender);
          }
        }

        // Update the main window.
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
              // Fill in the channel's style.
              channel.style = doc.addStyle(channel.name, null);
              StyleConstants.setForeground(channel.style, channel.color);

              // Add the channel/sender to the channel comboboxes.
              listenChannel.addItem(channel);
              sendMsgChannel.addItem(channel);
              if (null != sender) {
                sendAudioChannel.addItem(sender);
              }
              subscribeChannel.addItem(channel);
            }
          });
        
        // Done.
        return true;

      } else if (e instanceof ExceptionalEvent) {
        // Something went wrong. We can't subscribe to the channel.
        signalError("Unable to subscribe to " + channel.name + ":\n" +
                    ((ExceptionalEvent)e).x);
        return true;
      }

      return false;
    }
  }


  // =======================================================================
  //                        The add sender handler
  // =======================================================================

  /** The add audio sender handler. */
  final class AddSenderHandler extends AbstractHandler {

    /** Handle the specified event. */
    protected boolean handle1(Event e) {
      // Make sure the event has the right closure.
      if (! (e.closure instanceof AudioSender)) {
        return false;
      }

      // Get the audio sender.
      AudioSender sender = (AudioSender)e.closure;

      if ((e instanceof AudioSource.ControlEvent) &&
          (AudioSource.ControlEvent.STARTED ==
           ((AudioSource.ControlEvent)e).type)) {
        // Clear sender's operation.
        sender.operation2 = null;

        // Done.
        return true;

      } else if (e instanceof ExceptionalEvent) {
        // The audio source is acting up. We silence it as a result.
        signalError("Silencing added / changed audio source " + sender.name +
                    ":\n" + ((ExceptionalEvent)e).x);
        synchronized (lock) {
          sender.disconnect();
        }

        // Clear sender's operation.
        sender.operation2 = null;

        // Done.
        return true;
      }

      return false;
    }

  }


  // =======================================================================
  //                           Instance fields
  // =======================================================================

  /**
   * The audio sink audio imported event handler.
   *
   * @serial  Must not be <code>null</code>.
   */
  final Component.Importer     audioSinkAudio;

  /**
   * The audio sink control imported event handler.
   *
   * @serial  Must not be <code>null</code>.
   */
  final Component.Importer     audioSinkControl;

  /**
   * The message event handler.
   *
   * @serial  Must not be <code>null</code>.
   */
  final MessageHandler         message;

  /**
   * The user name.
   *
   * @serial  Must not be <code>null</code>.
   */
  String                       user;

  /**
   * The mapping from channel names to channel state descriptors.
   *
   * @serial  Must be a mapping from strings to channel state
   *          descriptors, with each key being equal to the
   *          name of the channel.
   */
  final HashMap                channels;

  /**
   * The mapping from channel names to audio sender descriptors.
   *
   * @serial  Must be a mapping from strings to audio sender
   *          descriptors, with each key being equal to the 
   *          name of the channel.
   */
  final HashMap                senders;

  /**
   * The name of the current channel or <code>null</code> when
   * listening to all channels.
   *
   * @serial
   */
  volatile String              currentChannel;

  /**
   * The history of text messages, which is a circular buffer storing
   * the up to <code>history.length</code> last text messages.
   *
   * @serial  Must be a valid array of size <code>HISTORY_SIZE</code>.
   */
  TextMessage[]                history;

  /**
   * The head of the text message history buffer. The head points to
   * the first entry in the history buffer.
   *
   * @serial  0 <= historyHead < history.length
   */
  int                          historyHead;

  /**
   * The tail into the history buffer. The tail points to the next
   * buffer entry a text message can be added to. The history is
   * empty if the tail is -1.
   *
   * @serial  -1 <= historyTail < history.length
   */
  int                          historyTail;

  /**
   * The current volume.
   *
   * @serial  Must be between 0 and 100, inclusive.
   */
  int                          volume;

  /**
   * The global mute audio flag.
   *
   * @serial
   */
  volatile boolean             muteAudio;
  
  /**
   * The name of the channel currently showing in the send
   * message panel.
   *
   * @serial  Must not be <code>null</code>.
   */
  volatile String              sendMsgChannel;

  /**
   * The name of the channel currently showing in the send
   * audio panel.
   *
   * @serial  Must not be <code>null</code>.
   */
  volatile String              sendAudioChannel;

  /**
   * The name of the channel currently showing in the subscribe
   * panel.
   *
   * @serial  Must not be <code>null</code>.
   */
  volatile String              subscribeChannel;

  /**
   * The flag for whether the listen panel is expanded.
   *
   * @serial
   */
  volatile boolean             listenExpanded;

  /**
   * The flag for whether the send message panel is expanded.
   *
   * @serial
   */
  volatile boolean             sendMsgExpanded;

  /**
   * The flag for whether the send audio panel is expanded.
   *
   * @serial
   */
  volatile boolean             sendAudioExpanded;

  /**
   * The flag for whether the subscribe panel is expanded.
   *
   * @serial
   */
  volatile boolean             subscribeExpanded;

  /**
   * The background color for the text pane in the listen panel.
   *
   * @serial  Must be a valid color.
   */
  volatile Color               backgroundColor;

  /** The document model for the listen text pane. */
  transient StyledDocument     document;

  /** The remotely exported message event handler. */
  transient RemoteReference    remoteMessage;

  /** The lease maintainer for the remote message event handler. */
  transient LeaseMaintainer    remoteMessageMaintainer;

  /** The flag for whether audio is enabled. */
  transient boolean            runningWithAudio;


  // =======================================================================
  //                           Constructor
  // =======================================================================

  /**
   * Create a new instance of <code>Chat</code>.
   *
   * @param  env   The environment for the new instance.
   * @param  user  The user name.
   */
  public Chat(Environment env, String user) {
    super(env);
    audioSinkAudio       = declareImported(AUDIO_SINK_AUDIO);
    audioSinkControl     = declareImported(AUDIO_SINK_CONTROL);
    message              = new MessageHandler();
    this.user            = user;
    channels             = new HashMap();
    ChannelState channel = new ChannelState(user);
    channels.put(user, channel);
    senders              = new HashMap();
    AudioSender sender   = new AudioSender(channel);
    sender.acquire();
    senders.put(user, sender);
    history              = new TextMessage[HISTORY_SIZE]; 
    historyTail          = -1;
    volume               = AudioSink.DEFAULT_VOLUME;
    sendMsgChannel       = user;
    sendAudioChannel     = user;
    subscribeChannel     = user;
    listenExpanded       = true;
    operation            = new Operation(timer, request, null);
  }


  // =======================================================================
  //                           Serialization
  // =======================================================================

  /**
   * Serialize this chat component.
   *
   * @serialData  The default fields while holding the lock.
   */
  private void writeObject(ObjectOutputStream out) throws IOException {
    synchronized (lock) {
      out.defaultWriteObject();
    }
  }

  /** Deserialize a chat component. */
  private void readObject(ObjectInputStream in)
    throws IOException, ClassNotFoundException {

    // Read the non-transient fields.
    in.defaultReadObject();

    // Restore the transient fields.
    operation    = new Operation(timer, request, null);
  }


  // =======================================================================
  //                           Start and stop
  // =======================================================================

  /** Acquire the resources needed by the chat application. */
  public void acquire() {
    synchronized (lock) {
      if (ACTIVATING != status) {
        return;
      }

      // Verify the user name.
      String newUser;
      try {
        newUser = getEnvironment().getParentName();
      } catch (IllegalStateException x) {
        return;
      }

      if ((! "/".equals(newUser)) && (! user.equals(newUser))) {
        // The user name changed.
        ChannelState channel;
        AudioSender  sender;
        int          type    = AUDIO_SILENCE;
        String       path    = null;
        Color        color   = DEFAULT_COLOR;

        // Remove old user channel and audio sender.
        channel = (ChannelState)channels.get(user);
        if (null != channel) {
          color = channel.color;
          channels.remove(user);
        }

        sender = (AudioSender)senders.get(user);
        if (null != sender) {
          type = sender.type;
          path = sender.path;

          senders.remove(user);
          sender.disconnect();
          sender.release();
        }

        // Create new user channel and audio sender if not already present.
        channel = (ChannelState)channels.get(newUser);
        if (null == channel) {
          channel = new ChannelState(newUser);
          channel.color = color;
          channels.put(newUser, channel);
        }

        if (! senders.containsKey(newUser)) {
          sender = new AudioSender(channel);
          sender.acquire();
          senders.put(newUser, sender);

          // Restore audio sender.
          if (AUDIO_SILENCE != type) {
            if (AUDIO_MICROPHONE == type) {
              sender.connect();
            } else {
              sender.connect(path);
            }
          }
        }

        // Adjust selected channels.
        if (user.equals(currentChannel)) {
          currentChannel = newUser;
        }
        if (user.equals(sendMsgChannel)) {
          sendMsgChannel = newUser;
        }
        if (user.equals(sendAudioChannel)) {
          sendAudioChannel = newUser;
        }
        if (user.equals(subscribeChannel)) {
          subscribeChannel = newUser;
        }

        // Clear history.
        for (int i=0; i<history.length; i++) {
          history[i] = null;
        }
        historyTail = -1;
        historyHead = 0;

        // Change user name.
        user = newUser;
      }
    }

    // Link the operation to a startup handler.
    operation.continuation = new StartupHandler();
    // Export the message event handler.
    operation.handle(new
      BindingRequest(null, null, new
        RemoteDescriptor(message), Duration.FOREVER));
  }

  /**
   * Start this chat application. This method call's the super class's
   * start method and then adjusts the vertical scroll bar of the
   * listen panels text area.
   */
  public void start() {
    super.start();

    final Window window;
    synchronized (lock) {
      if (ACTIVE != status) {
        return;
      }

      window = (Window)mainWindow;
    }

    SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          window.listenScrollBar.setValue(window.listenScrollBar.getMaximum());
        }
      });
  }

  /** Release the resources used by the chat application. */
  public void release() {
    // Note that we can safely use a synchronous invocation, because
    // the same thread performs the event handling in the audio sink
    // or source.
    if (runningWithAudio) {
      Synchronous.invoke(audioSinkControl, new
        AudioSink.ControlEvent(null, null, AudioSink.ControlEvent.STOP));

      Iterator iter = senders.values().iterator();
      while (iter.hasNext()) {
        AudioSender sender = (AudioSender)iter.next();

        if (null != sender.source) {
          Synchronous.invoke(sender.control, new
            AudioSource.ControlEvent(null, null,
                                     AudioSource.ControlEvent.STOP));
        }
      }
    }

    // Clear the document.
    document = null;

    // Release the exported message handler.
    if (null != remoteMessageMaintainer) {
      remoteMessageMaintainer.cancel();
      remoteMessageMaintainer = null;
    }

    // Release all exported channels.
    Iterator iter = channels.values().iterator();

    while (iter.hasNext()) {
      ChannelState channel = (ChannelState)iter.next();
      
      if (null != channel.leaseMaintainer) {
        channel.leaseMaintainer.cancel();
        channel.leaseMaintainer = null;
      }
    }
  }


  // =======================================================================
  //                             Managing the UI
  // =======================================================================

  /** Create the chat application's main window. */
  public Application.Window createMainWindow() {
    Window window = new Window(this);
    document      = window.listenText.getStyledDocument();

    return window;
  }

  /** Handle the specified action event. */
  public void actionPerformed(ActionEvent e) {
    String       cmd     = e.getActionCommand();
    ChannelState channel;
    AudioSender  sender;
    Window       window;

    // We ignore action events generated before the main window has
    // actually been shown.
    synchronized (lock) {
      if (ACTIVE != status) {
        return;
      }
      window = (Window)mainWindow;
    }

    if (ACTION_LISTEN.equals(cmd)) {
      Object o = window.listenChannel.getSelectedItem();

      if (ALL_CHANNELS == o) {
        currentChannel = null;
      } else {
        currentChannel = ((ChannelState)o).name;
      }

    } else if (ACTION_SELECT_SEND.equals(cmd)) {
      Object o = window.sendMsgChannel.getSelectedItem();
      channel  = resolveChannel(o);

      if (null == channel) {
        sendMsgChannel = (String)o;
      } else {
        sendMsgChannel = channel.name;
      }

    } else if (ACTION_SEND.equals(cmd)) {
      Object             o   = window.sendMsgChannel.getSelectedItem();
      channel                = resolveChannel(o);
      String             msg = window.sendMsgMessage.getText();
      String             ch;
      DiscoveredResource res;

      // Clear message.
      window.sendMsgMessage.setText(null);

      // Determine the channel name and the corresponding discovered
      // resource.
      if (null == channel) {
        ch  = (String)o;
        res = new DiscoveredResource(new
          Query(IS_CHANNEL, Query.BINARY_AND, new
            Query("name", Query.COMPARE_EQUAL, ch)), true);
      } else {
        ch  = channel.name;
        res = channel.resource;
      }

      // Create the text message event.
      TextMessage tm = new TextMessage(remoteMessage, ch, user, ch, msg);

      // Send it remotely.
      request.handle(new RemoteEvent(message, null, res, tm));

    } else if (ACTION_SELECT_AUDIO.equals(cmd)) {
      Object o = window.sendAudioChannel.getSelectedItem();
      sender   = resolveSender(o);

      if (null == sender) {
        sendAudioChannel = (String)o;
      } else {
        sendAudioChannel = sender.name;
        window.sendAudioSource.setSelectedIndex(sender.type);
        if (AUDIO_ENVIRONMENT == sender.type) {
          window.sendAudioEnvironment.setText(sender.path);
        }
      }

    } else if (ACTION_ADD_AUDIO.equals(cmd)) {
      Object o    = window.sendAudioChannel.getSelectedItem();
      sender      = resolveSender(o);
      int    type = window.sendAudioSource.getSelectedIndex();
      String path = ((AUDIO_ENVIRONMENT == type)?
                     window.sendAudioEnvironment.getText().trim():
                     null);

      // Make sure the environment path is not empty.
      if ((AUDIO_ENVIRONMENT == type) && "".equals(path)) {
        GuiUtilities.beep();
        JOptionPane.showMessageDialog(window,
                                      "Empty environment path",
                                      "Send Audio Error",
                                      JOptionPane.ERROR_MESSAGE);
        window.sendAudioEnvironment.setText(null);
        return;
      }

      if (null == sender) {
        // Make sure the channel name is valid for new audio senders.
        String ch =  ((String)o).trim();

        // We don't allow channels named "" or "<All channels>".
        if ("".equals(ch) || ALL_CHANNELS.equalsIgnoreCase(ch)) {
          GuiUtilities.beep();
          JOptionPane.showMessageDialog(window,
                                        "Illegal channel name \"" + ch + "\"",
                                        "Send Audio Error",
                                        JOptionPane.ERROR_MESSAGE);
          return;
        }

        // Set up the sender.
        sender = new AudioSender(ch);
        window.sendAudioChannel.addItem(sender);

        synchronized (lock) {
          sender.acquire();
          senders.put(ch, sender);

          // Connect to new audio source.
          if (AUDIO_SILENCE == type) {
            // We are done.
            return;
          } else if (AUDIO_MICROPHONE == type) {
            sender.connect();
          } else {
            sender.connect(path);
          }
        }

        // Set up operatioon for starting new audio source. Note that
        // only the user can cause this to happen and we currently own
        // the UI thread.
        sender.operation2 = new Operation(timer, wrap(sender.control), new
          AddSenderHandler());

        // Start the audio source.
        sender.operation2.handle(new
          AudioSource.ControlEvent(null, sender,
                                   AudioSource.ControlEvent.START));
        return;

      } else {
        if (((AUDIO_SILENCE == type) &&
             (AUDIO_SILENCE == sender.type)) ||
            ((AUDIO_MICROPHONE == type) &&
             (AUDIO_MICROPHONE == sender.type)) ||
            ((AUDIO_ENVIRONMENT == type) &&
             (AUDIO_ENVIRONMENT == sender.type) &&
             path.equals(sender.path))) {
          // No change in sender. We are done.
          return;
        }

        boolean canChange;
        synchronized (lock) {
          if (null == sender.operation2) {
            // Disconnect old audio source.
            sender.disconnect();

            // Connect to new audio source.
            if (AUDIO_SILENCE == type) {
              // We are done.
              return;
            } else if (AUDIO_MICROPHONE == type) {
              sender.connect();
            } else {
              sender.connect(path);
            }

            // Set up operation for starting new audio source.
            sender.operation2 = new Operation(timer, wrap(sender.control), new
              AddSenderHandler());

            canChange = true;
          } else {
            canChange = false;
          }
        }

        if (canChange) {
          // Start the audio source.
          sender.operation2.handle(new
            AudioSource.ControlEvent(null, sender,
                                     AudioSource.ControlEvent.START));
        } else {
          GuiUtilities.beep();
          JOptionPane.showMessageDialog(window,
                                        "Audio sender for " + sender.name +
                                        " currently being changed",
                                        "Send Audio Error",
                                        JOptionPane.ERROR_MESSAGE);
        }
      }

    } else if (ACTION_DELETE_AUDIO.equals(cmd)) {
      Object o = window.sendAudioChannel.getSelectedItem();
      sender   = resolveSender(o);

      if (null != sender) {
        if (sender.name.equals(user)) {
          // We don't allow a user to delete his own channel.
          GuiUtilities.beep();
          JOptionPane.showMessageDialog(window,
                                        "Unable to delete user's channel",
                                        "Send Audio Error",
                                        JOptionPane.ERROR_MESSAGE);
          return;
        }

        // Remove the sender from the send audio combobox.
        window.sendAudioChannel.removeItem(sender);

        // Remove the sender from the senders map.
        synchronized (lock) {
          if (senders.containsKey(sender.name)) {
            senders.remove(sender.name);
            sender.disconnect();
            sender.release();
          }
        }

      } else {
        // No such audio sender to delete.
        GuiUtilities.beep();
        JOptionPane.showMessageDialog(window,
                                      "Not sending audio to \"" + o + "\"",
                                      "Send Audio Error",
                                      JOptionPane.ERROR_MESSAGE);
      }

    } else if (ACTION_SELECT_SUBSCRIBE.equals(cmd)) {
      Object o = window.subscribeChannel.getSelectedItem();
      channel  = resolveChannel(o);

      if (null == channel) {
        subscribeChannel = (String)o;
      } else {
        subscribeChannel = channel.name;
        window.subscribeBeep.setSelected(channel.beep);
        window.subscribeMute.setSelected(channel.mute);
        window.subscribeIcon.setColor(channel.color);
        window.subscribeColor.repaint();
      }

    } else if (ACTION_ADD_SUBSCRIPTION.equals(cmd)) {
      Object o = window.subscribeChannel.getSelectedItem();
      channel  = resolveChannel(o);

      if (null == channel) {
        // Add new channel.
        String ch = (String)o;
        ch        = ch.trim();

        // We don't allow subscriptions to channels named "" or "<All
        // channels>".
        if ("".equals(ch) || ALL_CHANNELS.equalsIgnoreCase(ch)) {
          GuiUtilities.beep();
          JOptionPane.showMessageDialog(window,
                                        "Illegal channel name \"" + ch + "\"",
                                        "Subscribe Error",
                                        JOptionPane.ERROR_MESSAGE);
          return;
        }

        // Set up a new channel state descriptor.
        channel = new ChannelState(ch);

        channel.beep  = window.subscribeBeep.isSelected();
        channel.mute  = window.subscribeMute.isSelected();
        channel.color = window.subscribeIcon.getColor();

        // Fire off a binding request to export the channel.
        operation.handle(new
          BindingRequest(null, channel, new
            RemoteDescriptor(channel, new
              Channel(channel.name)), Duration.FOREVER));

      } else {
        // Change existing channel.
        channel.beep  = window.subscribeBeep.isSelected();
        channel.mute  = window.subscribeMute.isSelected();
        Color color   = window.subscribeIcon.getColor();

        if (! color.equals(channel.color)) {
          // The color changed. Repaint the window.
          channel.color = color;
          StyleConstants.setForeground(channel.style, color);
          window.listenText.repaint();
        }
      }

    } else if (ACTION_DELETE_SUBSCRIPTION.equals(cmd)) {
      Object o = window.subscribeChannel.getSelectedItem();
      channel  = resolveChannel(o);

      if (null != channel) {
        if (channel.name.equals(user)) {
          // We don't allow a user to unsubscribe from his/her own
          // channel.
          GuiUtilities.beep();
          JOptionPane.showMessageDialog(window,
                                        "Unable to unsubscribe user's channel",
                                        "Subscribe Error",
                                        JOptionPane.ERROR_MESSAGE);
          return;
        }

        if (channel.name.equals(currentChannel)) {
          // The current channel is being deleted. Listen to all
          // channels.
          window.listenChannel.setSelectedItem(ALL_CHANNELS);
          currentChannel = null;
        }

        // Remove the channel from all comboboxes.
        window.listenChannel.removeItem(channel);
        window.sendMsgChannel.removeItem(channel);
        window.subscribeChannel.removeItem(channel);

        // Cancel the lease on the exported event handler and process
        // the channel's audio sender (if it exists).
        synchronized (lock) {
          channels.remove(channel.name);
          if (null != channel.leaseMaintainer) {
            channel.leaseMaintainer.cancel();
            channel.leaseMaintainer = null;
          }

          sender = (AudioSender)senders.get(channel.name);
          if (null != sender) {
            senders.remove(channel.name);
            sender.disconnect();
            sender.release();
          }
        }
        if (null != sender) {
          window.sendAudioChannel.removeItem(sender);
        }

      } else {
        // No such channel to delete.
        GuiUtilities.beep();
        JOptionPane.showMessageDialog(window,
                                      "Not subscribed to \"" + o + "\"",
                                      "Subscribe Error",
                                      JOptionPane.ERROR_MESSAGE);
      }

    }
  }

  /**
   * Resolve the selected item from a combobox into a channel state
   * descriptor.
   *
   * @param   o  The currently selected object from a combobox.
   * @return     The corresponding channel state descriptor or
   *             <code>null</code> if no such descriptor exists.
   */
  private ChannelState resolveChannel(Object o) {
    if (o instanceof ChannelState) {
      return (ChannelState)o;
    } else {
      synchronized (lock) {
        return (ChannelState)channels.get(o);
      }
    }
  }

  /**
   * Resolve the selected item from a combobox into an audio sender
   * descriptor.
   *
   * @param   o  The currently selected object from a combobox.
   * @return     The corresponding audio sender descriptor or
   *             <code>null</code> if no such descriptor exists.
   */
  private AudioSender resolveSender(Object o) {
    if (o instanceof AudioSender) {
      return (AudioSender)o;
    } else {
      synchronized (lock) {
        return (AudioSender)senders.get(o);
      }
    }
  }

  /** The source for audio source control events that change the volume. */
  private static final EventHandler VOLUME_SOURCE = new AbstractHandler() {
      protected boolean handle1(Event e) {
        if ((e instanceof AudioSink.ControlEvent) &&
            (AudioSink.ControlEvent.CHANGED_VOLUME
             == ((AudioSink.ControlEvent)e).type)) {
          return true;
        } else {
          return false;
        }
      }
    };

  /** Handle the specified change event. */
  public void stateChanged(ChangeEvent e) {
    JSlider slider = (JSlider)e.getSource();
    if (! slider.getValueIsAdjusting()) {
      volume = slider.getValue();

      if (runningWithAudio) {
        audioSinkControl.handle(new
          AudioSink.ControlEvent(VOLUME_SOURCE, null,
                                 AudioSink.ControlEvent.CHANGE_VOLUME,
                                 volume));
      }
    }
  }


  // =======================================================================
  //                        Text message formatting
  // =======================================================================

  /**
   * Format the specified text message.
   *
   * @param   msg  The text message to format.
   * @return       The corresponding string.
   */
  static String format(TextMessage tm) {
    StringBuffer buf = new StringBuffer(tm.sender.length() +
                                        tm.channel.length() +
                                        tm.msg.length() + 7);

    buf.append(tm.sender);
    buf.append(" to ");
    buf.append(tm.channel);
    buf.append(": ");
    buf.append(tm.msg);
    buf.append('\n');

    return buf.toString();
  }


  // =======================================================================
  //                            Initialization
  // =======================================================================

  /**
   * Initialize the <i>one.radio</i> chat application. The closure
   * must be a string array specifying the current user's name.
   *
   * @param   env      The environment.
   * @param   closure  The closure.
   * @throws  IllegalArgumentException
   *                   Signals that the closure is not a string array
   *                   with at least one entry specifying the user's
   *                   name.
   */
  public static void init(Environment env, Object closure) {
    if (! (closure instanceof String[])) {
      throw new IllegalArgumentException("Closure not a string array");
    }

    String[] args = (String[])closure;
    if (1 > args.length) {
      throw new IllegalArgumentException("Closure an empty string array");
    }

    Chat      comp      = new Chat(env, args[0]);
    AudioSink audioSink = new AudioSink(env);

    env.link ("main",             "main",    comp     );
    comp.link("request",          "request", env      ); 
    comp.link("audioSinkAudio",   "audio",   audioSink);
    comp.link("audioSinkControl", "control", audioSink);
  }

}
