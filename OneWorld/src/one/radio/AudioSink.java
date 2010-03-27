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

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.LineUnavailableException;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;

import java.security.AccessController;
import java.security.PrivilegedAction;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import one.util.Guid;

import one.world.core.Component;
import one.world.core.ComponentDescriptor;
import one.world.core.Environment;
import one.world.core.Event;
import one.world.core.EventHandler;
import one.world.core.ExceptionalEvent;
import one.world.core.ExportedDescriptor;
import one.world.core.ImportedDescriptor;
import one.world.core.InvalidTupleException;
import one.world.core.TupleException;

import one.world.util.AbstractHandler;
import one.world.util.SystemUtilities;
import one.world.util.TypedEvent;

/**
 * Plays audio in the form of {@link AudioMessage}s to the system speakers.
 * Audio streams are demultiplexed based on {@link AudioMessage#streamId}.
 *
 * <p><b>Imported and Exported Event Handlers</b></p>
 *
 * <p>Exported event handlers:<dl>
 *    <dt>audio</dt>
 *    <dd>Handles {@link AudioMessage}s by playing the audio data to the 
 *        system speakers. </dt>
 *    <dt>control</dt>
 *    <dd>Handles {@link ControlEvent}s.</dd>
 *        </dd>
 * </dl></p>
 *
 * <p><b>Implementation note:</b> AudioSink does not perform general message
 * reordering, but it will correct inversions (a pair of messages received
 * out of order) and drop late messages.</p>
 *
 * @see      AudioSource
 * @version  $Revision: 1.17 $
 * @author   Janet Davis
 */
public final class AudioSink extends Component {

  // =======================================================================
  //                           Control event
  // =======================================================================
  /** The audio sink control event. */
  public static final class ControlEvent extends TypedEvent {

    /** The start event type. */
    public static final int START = 1;

    /** The started event type. */
    public static final int STARTED = 2;

    /** The stop event type. */
    public static final int STOP = 3;

    /** The stopped event type. */
    public static final int STOPPED = 4;

    /** The change volume event type. */
    public static final int CHANGE_VOLUME = 5;

    /** The changed volume event type. */
    public static final int CHANGED_VOLUME = 6;

    /** The volume, on a scale of 0-100, for volume events. */
    public int volume;

    /** Creates a new, empty control event. */
    public ControlEvent() {}

    /** 
     * Creates a new control event with the specified source, closure,
     * and type.
     *
     * @param source    The source of the new control event.
     * @param closure   The closure of the new control event.
     * @param type      The type of the new control event.
     */
    public ControlEvent(EventHandler source, Object closure, int type) {
      super(source, closure, type);
    }

    /** 
     * Creates a new control event with the specified source, closure,
     * type, and volume setting.
     *
     * @param source    The source of the new control event.
     * @param closure   The closure of the new control event.
     * @param type      The type of the new control event.
     * @param volume    The volume setting for the new control event.
     */
    public ControlEvent(EventHandler source, Object closure, int type,
                        int volume) {
      super(source, closure, type);
      this.volume = volume;
    }

    /**
     * Validates this control event. 
     */
    public void validate() throws TupleException {
      super.validate();
      if (type < 1 || type > 6) {
        throw new InvalidTupleException("Invalid type (" + type +
                                        ") for control event (" + this + ")");
      } else if (volume < 0 || volume > 100) {
        throw new InvalidTupleException("Invalid volume (" + volume +
                                        ") for control event (" + this + ")");
      }
    }
  }

  // =======================================================================
  //                           The audio handler
  // =======================================================================

  /** The audio exported event handler. */
  final class AudioHandler extends AbstractHandler {

    /** Handle the specified event. */
    protected boolean handle1(Event e) {
      if (e instanceof AudioMessage) {
        AudioMessage message = (AudioMessage)e;
	if (isNotValid(message)) {
	  return true;
	}

	// Obtain a line manager for this channel and sender.
	LineManager line;
	synchronized (lock) {
	  line = (LineManager)lines.get(message.streamId);
	  if (line == null) {
	    line = new LineManager(message.streamId,
	                           message.channel, message.sender);
	    lines.put(message.streamId, line);
          }
	}

	// Tell the line manager to play the audio data.
	line.play(message);

	return true;
      }
      return false;
    }
  }
 

  // =======================================================================
  //                           The control handler
  // =======================================================================
   

  /** The control handler. */
  final class ControlHandler extends AbstractHandler {

    /** Handles events. */
    protected boolean handle1(Event e) {
      if (e instanceof ControlEvent) {
        ControlEvent ce = (ControlEvent) e;
	if (isNotValid(ce)) {
	  return true;
	}

        switch (ce.type) {
	case ControlEvent.START:
	  // On a START event, set the state to ACTIVE and respond with a
	  // STARTED event.
	  try {
	    synchronized (lock) {
	      openPlaceholderLine();
	      state = ACTIVE;
	    }
	  } catch (SecurityException x) {
	    respond(ce, x);
	    return true;
	  } catch (IllegalArgumentException x) {
	    respond(ce, x);
	    return true;
	  } catch (LineUnavailableException x) {
	    respond(ce, x);
	    return true;
	  }

	  respond(ce, new ControlEvent(this, null, ControlEvent.STARTED));
	  return true;

	case ControlEvent.STOP:
	  // On a STOP event, set the state to INACTIVE, clear the set of
	  // audio lines, and respond with a STOPPED event.
	  synchronized (lock) {
	    if (state != INACTIVE) {
	      state = INACTIVE;
	      clearLines();
	    }
	  }
	  respond(ce, new ControlEvent(this, null, ControlEvent.STOPPED));
	  return true;

	case ControlEvent.CHANGE_VOLUME:
	  // On a CHANGE_VOLUME event, set the volume and respond with a 
	  // CHANGED_VOLUME event.
	  synchronized (lock) {
	    if (state == ACTIVE) {
	      setVolume(ce.volume);
	    }
	  }
	  respond(ce, new ControlEvent(this, null,
	                               ControlEvent.CHANGED_VOLUME,
				       ce.volume));
          return true;
	}
      } 
      return false;
    }
  }

  // =======================================================================
  //                           LineManager and related methods
  // =======================================================================

  /** Manages a single audio line. */
  final class LineManager {

    /** The id of the stream this line is for. */
    final Guid streamId;

    /** The channel this line is for. */
    final String channel;

    /** The sender this line is for. */
    final String sender; 

    /** The current audio format descriptor. */
    AudioDescriptor descriptor;

    /** The audio line for the speaker. */
    SourceDataLine speakerLine;

    /** The gain control. */
    FloatControl gainControl;

    /** True if the speaker line is invalid.  */
    boolean invalid;

    /** If the speaker line is invalid, this exception is why. */
    Throwable causeOfInvalidity;

    /** The number of bytes to buffer before starting playback. */
    int bufferLength;

    /** True if the line is currently buffering. */
    boolean buffering;

    /** The current sequence number. */
    int sequence;

    /** 
     * An out-of-order audio message (used to correct delivery
     * inversions) 
     */ 
    AudioMessage outOfOrder;

    /** A lock for this line. */
    Object lineLock;


    /** 
     * Constructs a new line manager for the specified channel and sender.
     *
     * @param streamId  The streamId this line is for.
     * @param channel   The channel this line is for.
     * @param sender    The sender this line is for.
     */
    LineManager(Guid streamId, String channel, String sender) {
      this.streamId = streamId;
      this.channel = channel;
      this.sender = sender;
      lineLock = new Object();
    }

    /** 
     * Plays the contents of the specified audio message.
     *
     * @param  message   
     *         The audio message to play.
     */
    void play(AudioMessage message) {
      synchronized (lineLock) {
        if ((descriptor == null)
	    || (!message.descriptor.id.equals(descriptor.id)
	        && !message.descriptor.equals(descriptor))) {
	  makeSpeakerLine(message.descriptor);
	}

	if (invalid) {
	  message.source.handle(
	     new ExceptionalEvent(audio, message.closure,
	                          causeOfInvalidity));
	} else {
	  if (sequence > 0 
	      && message.sequenceNumber > sequence + 1) {

            // This message arrived too early.
	    
            // Play any already delayed messages.
	    if (outOfOrder != null) {
	      if (outOfOrder.sequenceNumber < message.sequenceNumber) {
	        // If the stored message comes before the new message,
		// play the stored message.
	        AudioMessage m = outOfOrder;
		sequence = outOfOrder.sequenceNumber - 1;
		outOfOrder = null;
	        play(m);
	      } else {
	        // Otherwise, assume a message has been lost and skip to
		// the new message.
	        sequence = message.sequenceNumber - 1;
	      }
	    }

            // If it's still too early, keep it for later.
            if (message.sequenceNumber > sequence + 1) {
	      if (DEBUG) {
	        SystemUtilities.debug(this + " delaying buffer " 
	                              + message.sequenceNumber
		    		      + " (" + message.length
				      + " bytes)");
              }
	      outOfOrder = message;
	      return;
	    }

	  } else if (message.sequenceNumber < sequence) { 
	    // The message is late; throw it away.
	    if (DEBUG) {
	      SystemUtilities.debug(this + " discarding late buffer " 
	                            + message.sequenceNumber);
            }
	    return;
	  }

	  if (DEBUG) {
	    SystemUtilities.debug(this + " playing buffer " 
	                          + message.sequenceNumber
				  + " (" + message.length
				  + " bytes)");
	  }

          // Check to see if we are buffering and need to start the line.
	  if (buffering && (speakerLine.available() >= bufferLength)) {
	    speakerLine.start();
	    buffering = false;
	  }

          // Write the data and advance the sequence number.
	  try {
	    speakerLine.write(message.data, 0, message.length);  
	    sequence = message.sequenceNumber;
	  } catch (IllegalArgumentException x) {
	    invalidate(x);
	    message.source.handle(
	       new ExceptionalEvent(audio, message.closure, x));
            return;
	  }
	}
      }
    }
    
    /** 
     * Makes a new speaker line with the specified audio format
     * descriptor. 
     *
     * @param descriptor An audio format descriptor. 
     */
    void makeSpeakerLine(AudioDescriptor descriptor) {

      final AudioFormat format = descriptor.getAudioFormat();
      final DataLine.Info info =
          new DataLine.Info(SourceDataLine.class, format);

      if (DEBUG) {
        SystemUtilities.debug(this + " started.");
      }

      synchronized (lineLock) {

        // Clear up whatever is still in the buffer.
        if (speakerLine != null) {
	  if (buffering) {
	    speakerLine.start();
	  }
	  speakerLine.drain();
	}

	releaseResources();
        this.descriptor = descriptor;

	// Start buffering.
	bufferLength = (int)(BUFFER_DURATION * descriptor.frameRate)
	                 * descriptor.frameSize;
	buffering = true;

	if (DEBUG) {
	  SystemUtilities.debug(this + " buffering " + bufferLength 
	                          + " bytes.");
	}

	try {
	  // Open a source data line.
	  speakerLine = (SourceDataLine)AudioSystem.getLine(info);

          // Opening the line requires the "play" permission.
          LineUnavailableException x = (LineUnavailableException)
              AccessController.doPrivileged(
                  new PrivilegedAction() {
                      public Object run() {
                        try {
                          speakerLine.open(format, 2*bufferLength);
                        } catch (LineUnavailableException x) {
  		          return x;
                        }
                        return null;
                      }
                  });

	  // Respond to STOP events by releasing resources. 
	  speakerLine.addLineListener(
	      new LineListener() {
	        public void update(LineEvent event) {
		  if (event.getType().equals(LineEvent.Type.STOP)) {
		    if (DEBUG) {
		      SystemUtilities.debug(this + " stopped.");
		    }
		  
		    // Release all resources.
		    releaseResources();
		  }
		}
	      });

          // Get a gain control for the line.
	  gainControl = (FloatControl)
	      speakerLine.getControl(FloatControl.Type.MASTER_GAIN);

	  // Set the gain.
	  setGain();

	} catch (LineUnavailableException x) {
	  invalidate(x);
	} catch (IllegalArgumentException x) {
	  invalidate(x);
	} catch (SecurityException x) {
	  invalidate(x);
	}
      }
    }

    /** Sets the gain on this line. */
    void setGain() {
      if (gainControl != null) {
        gainControl.setValue(gain);
      }
    }


    /** Makes this line manager invalid. */
    void invalidate(Throwable x) {
      synchronized (lineLock) {
	releaseResources();
        invalid = true;
        causeOfInvalidity = x;
      }
    }

    /** Releases all resources. */
    void releaseResources() {
      synchronized (lineLock) {
	descriptor = null;
        invalid = false;
	causeOfInvalidity = null;
	gainControl = null;
	outOfOrder = null;
	sequence = 0;
	
	if (speakerLine != null) {
	  AccessController.doPrivileged(
	      new PrivilegedAction() {
	        public Object run() {
	          speakerLine.stop();
	          speakerLine.close();
		  return null;
		}
	      });
	  speakerLine = null;
	}
      }
    }

    /** Returns a string representation of this line manager. */
    public String toString() {
      return "#[LineManager for " + sender + " -> " + channel
             + " (" + streamId + ")]";
    }
  }

  /** Sets the volume on all the audio lines. */
  void setVolume(double volume) {
    // Change volume on scale of 0-100 to scale of 0.0-1.0. 
    volume = volume/100.0;

    // If volume is 0, bump it up so logarithms don't break.
    if (volume == 0.0) {
      volume = 0.0001;
    }

    synchronized (lock) {
      // Compute gain, which is stored globally for use by new LineManagers.
      gain = (float)(Math.log(volume) / Math.log(10.0) * 20.0);

      // Set the gain on each audio line.
      Iterator iter = lines.values().iterator();
      while (iter.hasNext()) {
        ((LineManager)iter.next()).setGain();
      }
    }
  }

  /** Clears the audio lines. */
  void clearLines() {
    synchronized (lock) {
      // Stop each audio line.
      Iterator iter = lines.values().iterator();
      while (iter.hasNext()) {
        ((LineManager)iter.next()).releaseResources();
      }

      // Clear the hashmap.
      lines.clear();
    }
  }

  // =======================================================================
  //                           Descriptors
  // =======================================================================

  /** The component descriptor. */
  private static final ComponentDescriptor SELF =
    new ComponentDescriptor("one.radio.AudioSink",
                            "An audio sink component",
                            true);

  /** The exported event handler descriptor for the audio handler. */
  private static final ExportedDescriptor AUDIO =
    new ExportedDescriptor("audio",
                           "The audio message handler",
                           new Class[] { AudioMessage.class },
                           new Class[] { LineUnavailableException.class,
			                 IllegalArgumentException.class,
			                 SecurityException.class },
                           false);

  /** The exported event handler descriptor for the control handler. */
  private static final ExportedDescriptor CONTROL =
    new ExportedDescriptor("control",
                           "The audio sink control handler",
                           new Class[] { ControlEvent.class },
                           null,
                           false);


  // =======================================================================
  //                           Constants
  // =======================================================================
  
  /** The inactive state. */
  static final int INACTIVE = 0;

  /** The active state. */
  static final int ACTIVE = 1;

  /** Debug or not? */
  static final boolean DEBUG = false;

  /** The default volume is 50, on a scale of 0-100. */
  public static final int DEFAULT_VOLUME = 50;

  /** 
   * The default buffer duration, in fractions of a second. 
   * This constant controls how much data is buffered before playback
   * starts.  The smaller it is, the less delay; the larger it is, the
   * smoother the playback.
   */
  public static final double BUFFER_DURATION = 0.1;

  // =======================================================================
  //                           Instance fields
  // =======================================================================

  /**
   * The audio exported event handler.
   *
   * @serial  Must not be <code>null</code>.
   */
  final EventHandler       audio;

  /**
   * The control exported event handler.
   *
   * @serial  Must not be <code>null</code>.
   */
  final EventHandler       control;

  /** The current gain. */
  float gain;

  /** The set of active audio line managers.  */
  transient Map lines;

  /** 
   * The audio line used to ensure that audio capture in a known format 
   * will be possible. (See <A
   * HREF="http://developer.java.sun.com/developer/bugParade/bugs/4347309.html">JDK
   * bug #4347309</A>.)
   */
  transient SourceDataLine capturePlaceholderLine;

  /** The lock for component state. */
  transient Object lock;

  /** The component state. */
  transient int state;

  // =======================================================================
  //                           Constructor
  // =======================================================================

  /**
   * Create a new instance of <code>AudioSink</code>.
   *
   * @param  env  The environment for the new instance.
   */
  public AudioSink(Environment env) {
    super(env);
    audio = declareExported(AUDIO, new AudioHandler());
    control = declareExported(CONTROL, new ControlHandler());

    lines = new HashMap();
    lock = new Object();
    setVolume(DEFAULT_VOLUME);
  }

  /**
   * Open the placeholder line.
   */
  void openPlaceholderLine() 
      throws SecurityException, LineUnavailableException, 
             IllegalArgumentException {

    // Open the placeholder source data line to ensure audio capture in a
    // reasonable format will be possible.

    DataLine.Info info = new DataLine.Info(SourceDataLine.class,
                                           AudioSource.CAPTURE_FORMAT);

    capturePlaceholderLine = (SourceDataLine) AudioSystem.getLine(info);

    // Opening the line requires the "play" permission.
    LineUnavailableException x = (LineUnavailableException)
          AccessController.doPrivileged(
              new PrivilegedAction() {
                  public Object run() {
                    try {
                      capturePlaceholderLine.open(AudioSource.CAPTURE_FORMAT);
                    } catch (LineUnavailableException x) {
		      return x;
                    }
                    return null;
                  }
              });

    if (x != null) {
        throw x;
    }
  }

  /** Finalize this audio sink. */
  protected void finalize() {
    if (capturePlaceholderLine != null) {
      capturePlaceholderLine.close();
    }
  }
  
  static final void open(final SourceDataLine line, 
                         final AudioFormat format) 
         throws LineUnavailableException {
  }


  // =======================================================================
  //                           Component support
  // =======================================================================

  /** Get the component descriptor. */
  public ComponentDescriptor getDescriptor() {
    return (ComponentDescriptor)SELF.clone();
  }

  /** 
   * Serialize this audio sink.
   *
   * @serialData    The default fields while holding the lock.
   */
  protected void writeObject(ObjectOutputStream out) throws IOException {
    synchronized (lock) {
      out.defaultWriteObject();
    }
  }

  /** Deserialize this audio sink. */
  private void readObject(ObjectInputStream in) 
      throws IOException, ClassNotFoundException {

    // Read the non-transient fields.
    in.defaultReadObject();

    // Restore the transient data.
    lines = new HashMap();
    lock = new Object();
  }
}
