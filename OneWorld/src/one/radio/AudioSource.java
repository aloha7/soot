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
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.IOException;

import java.security.AccessController;
import java.security.PrivilegedAction;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Map;

import one.world.binding.BindingRequest;
import one.world.binding.BindingResponse;
import one.world.binding.Duration;
import one.world.binding.LeaseMaintainer;
import one.world.binding.LeaseException;
import one.world.binding.ResourceRevokedException;
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
import one.world.core.InvalidTupleException;
import one.world.core.TupleException;

import one.world.data.BinaryData;
import one.world.data.Chunk;

import one.world.io.InputRequest;
import one.world.io.InputResponse;
import one.world.io.Query;
import one.world.io.QueryResponse;
import one.world.io.SioResource;
import one.world.io.NoSuchTupleException;

import one.world.util.AbstractHandler;
import one.world.util.IteratorElement;
import one.world.util.IteratorEmpty;
import one.world.util.IteratorRequest;
import one.world.util.Operation;
import one.world.util.SystemUtilities;
import one.world.util.Timer;
import one.world.util.TypedEvent;

import one.util.Guid;
import one.util.RubberPipedInputStream;
import one.util.RubberPipedOutputStream;

/**
 * Sends {@link AudioMessage}s to a one.radio channel.  Currently, the
 * audio is captured from a microphone.
 *
 * <p><b>Imported and Exported Event Handlers</b></p>
 *
 * <p>Exported event handlers:<dl>
 *    <dt>control</dt>
 *    <dd>Handles {@link ControlEvent}s, which are used to start, stop,
 *        pause, and unpause the audio source.  Note that the response to 
 *        a start event may be a {@link SecurityException}, {@link
 *        IllegalArgumentException}, {@link LineUnavailableException}, 
 *        {@link UnknownResourceException}, or {@link LeaseException}
 *        rather than a started event.
 * </dl></p>
 *
 * <p>Imported event handlers:<dl>
 *    <dt>audio</dt>
 *    <dd>An event handler that accepts {@link AudioMessage}s and 
 *        {@link TextMessage}s.  The audio handler is also informed of 
 *        {@link NoSuchTupleException}s, {@link LeaseException}s, and
 *        {@link ResourceRevokedException}s, all of which also result in
 *        streaming being stopped.
 *        </dd>
 *    <dt>request</dt>
 *    <dd>The environment request handler.
 *        </dd>
 * </dl></p>
 *
 * @see      AudioSink
 * @version  $Revision: 1.27 $
 * @author   Janet Davis
 */
public final class AudioSource extends Component {

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

    /** The pause event type. */
    public static final int PAUSE = 5;

    /** The paused event type. */
    public static final int PAUSED = 6;

    /** The unpause event type. */
    public static final int UNPAUSE = 7;

    /** The unpaused event type. */
    public static final int UNPAUSED = 8;

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
     * Validates this control event. 
     */
    public void validate() throws TupleException {
      super.validate();
      if (type < 1 || type > 8) {
        throw new InvalidTupleException("Invalid type (" + type +
                                        ") for control event (" + this + ")");
      } 
    }
  }

  // =======================================================================
  //                           The control handler
  // =======================================================================

  /** The audio exported event handler. */
  final class ControlHandler extends AbstractHandler {

    /** Handle the specified event. */
    protected boolean handle1(Event e) {
 
      if (e instanceof ControlEvent) {
        ControlEvent ce = (ControlEvent) e;
        if (isNotValid(ce)) {
          return true;
        }
        
        switch (ce.type) {
        case ControlEvent.START:
	  start(ce);
          return true;
          
        case ControlEvent.STOP:
	  stop(ce);
          return true;

        case ControlEvent.PAUSE:
          pause(ce);
          return true;

        case ControlEvent.UNPAUSE:
	  unpause(ce);
          return true;
        }
      }

      return false;
    }

    /** Starts the audio source logic. */
    protected void start(Event e) {
      
      synchronized (lock) {
	if (state == ACTIVE) {
	  respond(e, new ControlEvent(this, null, ControlEvent.STARTED));
	  return;
        }
      }

      sourceLogic.start(e);
    }

    /** Signals that the audio source logic is active. */
    protected void activate(Event e) {
      synchronized (lock) {
        state = ACTIVE;
      }

      if (DEBUG) {
        SystemUtilities.debug("Audio source started");
      }

      if (e != null) {
        respond(e, new ControlEvent(this, null, ControlEvent.STARTED));
      }
    }

    /** Stops the audio source logic. */
    protected void stop(Event e) {
      synchronized (lock) {
        if (state != INACTIVE) {
	  sourceLogic.stop();
	  state = INACTIVE;
	}
      }

      if (DEBUG) {
        SystemUtilities.debug("Audio source stopped");
      }

      if (e != null) {
        respond(e, new ControlEvent(this, null, ControlEvent.STOPPED));
      }
    }

    /** Pause the audio source logic. */
    protected void pause(Event e) {
      boolean paused = false;
      synchronized (lock) {
        if (state == ACTIVE) {
          state = PAUSED;
          sourceLogic.pause();
	  paused = true;
        } else if (state == PAUSED) {
	  paused = true;
	}
      }

      if (paused) {
        respond(e, new ControlEvent(this, null, ControlEvent.PAUSED));
      } else {
        respond(e, new IllegalStateException("Not active"));
      }
    }          
    
    /** Unpause the audio source logic. */
    protected void unpause(Event e) {
      boolean unpaused = false;
      synchronized (lock) {
        if (state == PAUSED) {
          sourceLogic.unpause();
	  unpaused = true;
        } else if (state == ACTIVE) {
	  unpaused = true;
	}
      }
      
      if (unpaused) {
        respond(e, new ControlEvent(this, null, ControlEvent.UNPAUSED));
      } else {
        respond(e, new IllegalStateException("Not active"));
      }
    }          
  }

  // =======================================================================
  //                     SourceLogic
  // =======================================================================

  /** Abstract base class for audio source logics.  */
  abstract class SourceLogic extends AbstractHandler {

    /** The name of the current source. */
    protected String sourceName;

    /** The stream id, which is transient to avoid duplication. */
    protected transient Guid streamId;

    /** The audio input stream. */
    protected transient AudioInputStream inputStream;

    /** The current audio format descriptor. */
    protected transient AudioDescriptor descriptor;

    /** The buffer of bytes read from the input stream. */
    protected transient byte[] buffer;

    /** The period with which to send audio data, in ms. */
    protected transient long period;

    /** The timer notification object. */
    protected Timer.Notification timerNotification;

    /** The sequence number. */
    protected int sequenceNumber;

    /** The current position. */
    protected int curPosition;

    /** Am I actively sending data? */
    protected volatile boolean sending;

    /** Constructs a new source logic. */
    SourceLogic() {
      streamId = new Guid();
      buffer = new byte[BUFFER_LENGTH];
    }

    /** Deserialize this source logic. */
    private void readObject(ObjectInputStream in)
        throws IOException, ClassNotFoundException {

      // Read the non-transient fields.
      in.defaultReadObject();

      // Restore transients.
      streamId = new Guid();
      buffer   = new byte[BUFFER_LENGTH];
      sending = false;
    }

    /** 
     * Starts this SourceLogic.  
     *
     * @param  e   
     *         The event to respond to when the source logic has been
     *         started or has failed.
     */
    abstract void start(Event e);

    /** Stops this source logic. */
    void stop() {
      releaseResources();
    }

    /** Pauses this source logic. */
    void pause() {
      cancelTimer();
    }

    /** Continues this source logic. */
    void unpause() {
      scheduleTimer();
    }

    /** 
     * Sets up audio descriptor, after the input stream exists. 
     */
    protected void init() {
      synchronized (lock) {
        // Make sure the input stream is not null.
        if (inputStream == null) {
          throw new IllegalStateException("Input stream is null");
        }

        AudioFormat format = inputStream.getFormat();

        // Set the descriptor.
        descriptor = new AudioDescriptor(format);

        // Determine the actual period in ms between sending audio 
	// messages, from the buffer size. 
        period = (long)(1000 * buffer.length / 
	               (descriptor.frameRate * descriptor.frameSize));
      }
    } 

    /** Starts streaming, once {@link #init} has been called . */
    protected void startStreaming() {

      // Announce the audio track.
      audio.handle(new TextMessage(this, null, sender, channel, 
                                   "You're listening to " 
				   + sourceName));

      // Send an initial message with the format but no data, to avoid
      // skipping at the start of the track.
      audio.handle(new AudioMessage(this, null, sender, channel, streamId,
	                            descriptor, new byte[]{}, 0,
			            sequenceNumber));

      // Set up a timer for sending audio data.
      scheduleTimer();

      if (DEBUG) {
        SystemUtilities.debug("Streaming " + sourceName 
	                        + " with period " + period);
      }
    }

    /** Schedules the timer notification. */
    protected void scheduleTimer() {
      Event e = new DynamicTuple(null, null);
      e.set("send", Boolean.TRUE);
      synchronized (lock) {
        if (null == timerNotification) {
          timerNotification = 
              timer.schedule(Timer.FIXED_RATE,
                             SystemUtilities.currentTimeMillis() + period,
                             period, this, e);
	}
      }
    }

    /** Cancels the timer notification. */
    void cancelTimer() {
      synchronized (lock) {
        if (timerNotification != null) {
          timerNotification.cancel();
          timerNotification = null;
        }
      }
    }

    /** Releases resources. */
    protected void releaseResources() {
      synchronized (lock) {
        cancelTimer();

        // Close and null out the input stream
        if (inputStream != null) {
          try {
            inputStream.close();
          } catch (IOException x) {
          }
          inputStream = null;
        }

	sending = false;
      }
    }

    /** 
     * Sends audio data. 
     *
     * @return False if no data was sent.
     */
    protected boolean sendData() {
      if (sending) {
        return true;
      }

      synchronized (lock) {
        try {
	  // Use sending condition variable to avoid piling up lots of
	  // threads wanting to send under this lock.
	  sending = true;

	  if (null == inputStream) {
	    sending = false;
	    return true;
	  }

          // Avoid blocking on read
	  if (inputStream.available() < descriptor.frameSize) {
	    sending = false;
	    return false;
          }

	  // Read data from the input stream.
          int numBytesRead = inputStream.read(buffer);

	  // Make sure some data was read.
	  if (-1 == numBytesRead) {
	    sending = false;
	    return false;
	  }

	  // Increment the sequence number.
	  sequenceNumber++;
	  curPosition++;

          // Send the message.
	  Event message =
	      new AudioMessage(this, null, sender, channel, streamId,
	                       descriptor, buffer, numBytesRead,
			       sequenceNumber);

          if (DEBUG) {
            SystemUtilities.debug("Sending " + message);
	  }

	  audio.handle(message);

	} catch (IOException x) {
	  SystemUtilities.debug("IOException while reading " 
	                        + sourceName);
          SystemUtilities.debug(x);
	  sending = false;
	  return false;
	}

	sending = false;
      }
      return true;
    }
  }

  // =======================================================================
  //                     CaptureLogic
  // =======================================================================

  /** Logic for audio capture. */
  final class CaptureLogic extends SourceLogic {

    /** The capture target data line. */
    transient TargetDataLine line;
    
    /** Starts the capture logic. */
    public void start(Event e) {
      synchronized (lock) {
        sourceName = "captured audio";
  
        try {
          // Obtain and open target data line.
          DataLine.Info captureInfo =
              new DataLine.Info(TargetDataLine.class, CAPTURE_FORMAT);
          line = (TargetDataLine) AudioSystem.getLine(captureInfo);
  
          // Opening and starting this line requires permission to record audio.
          LineUnavailableException x = (LineUnavailableException)
              AccessController.doPrivileged(
                  new PrivilegedAction() {
  	          public Object run() {
  		    try {
                      line.open(CAPTURE_FORMAT);
  		    } catch (LineUnavailableException x) {
  		      return x;
  		    }
                      line.start();
  	            return null;
  	          }
                  });
          if (x != null) {
            throw x;
          }
	  
        } catch (SecurityException x) {
	  respond(e, x);
	  control.stop(null);
  	  return;
        } catch (LineUnavailableException x) {
          respond(e, x);
	  control.stop(null);
    	  return;
        } catch (IllegalArgumentException x) {
          respond(e, x);
	  control.stop(null);
  	  return;
        }
  
        // Create the audio input stream.
        inputStream = new AudioInputStream(line);
  
        // Start streaming.
        init();
        startStreaming();

	// Inform that we have started.
        control.activate(e);
      }
    }

    /** Pauses the capture logic. */
    public void pause() {
      synchronized (lock) {
        cancelTimer();
        if (line != null) {
	  AccessController.doPrivileged(
	      new PrivilegedAction() {
	        public Object run() {
		  line.stop();
		  return null;
		}
	      });
	}
      }
    }

    /** Unpauses the capture logic. */
    public void unpause() {
      synchronized (lock) {
        if (line != null) {
	  AccessController.doPrivileged(
	      new PrivilegedAction() {
	        public Object run() {
		  line.start();
		  return null;
		}
	      });
	}
	startStreaming();
      }
    }

    /** Releases resources. */
    protected void releaseResources() {
      synchronized (lock) {
	if (line != null) {
	  AccessController.doPrivileged(
	      new PrivilegedAction() {
	        public Object run() {
		  line.stop();
		  line.close();
		  return null;
		}
	      });
	  line = null;
	}
        super.releaseResources();
      }
    }

    /** Handles events. */
    protected boolean handle1(Event e) {
      if (e.hasField("send")) {

        // If we're already sending right now, go away.
	// (This avoid piling up a whole bunch of timer events waiting on
	// this lock.)
	if (sending || (state != ACTIVE)) {
	  return true;
	}

        // If we are active, send data.
        synchronized (lock) {
	  if (state == ACTIVE) {
	    sendData();
	  }
	  return true;
	}
      }
      return false;
    }
  }

  // =======================================================================
  //                     AudioTupleLogic
  // =======================================================================

  /** Logic for streaming audio tuples. */
  final class AudioTupleLogic extends SourceLogic {
    
    /** The maxiumum header length we can cope with, in bytes. */
    final int headerLength = 2048;

    /** The relative path of the environment from which to read tuples. */
    String path;

    /** The ids of the tuples to play. */
    LinkedList tupleIds;

    /** The id of head the current audio track. */
    Guid currentHead;

    /** Has this logic been initialized? */
    boolean initialized;

    /** The tuple store event handler. */
    transient EventHandler tupleStore;

    /** The lease maintainer for above. */
    transient LeaseMaintainer leaseMaintainer;

    /** An operation for the tuple store. */
    transient Operation tupleOp;

    /** The start event to respond to after starting up. */
    transient Event startEvent;

    /** 
     * The pipe for reading audio data into.  This lets us get data from
     * many different Chunks into an AudioInputStream.
     */
    transient OutputStream pipe;

    /** 
     * The pipe input stream, which will be connected to {@link #pipe}
     * and wrapped in an AudioInputStream. 
     */
    transient RubberPipedInputStream pipeIn;

    /** The desired amount of buffered audio data, in bytes. */
    transient int pipeSize = Integer.MAX_VALUE;

    /** Number of bytes to skip at the beginning of the track. */
    transient int skipBytes;

    /** The next chunk to read. */
    transient Guid nextChunk;

    /** Are we currently reading the next chunk? */
    transient boolean reading;

    /** Logic for initialization. */
    final class InitializationLogic extends AbstractHandler {

      /** The initial event to reply to when initialization is done. */
      Event initialEvent;

      /** The query iterator. */
      EventHandler iterator;

      /** A lease maintainer for the query iterator. */
      LeaseMaintainer queryMaintainer;

      /** Handles events. */
      public boolean handle1(Event e) {
        if (e instanceof DynamicTuple && "initialize".equals(e.closure)) {
	  synchronized (lock) {
	    initialEvent = e;
	  }

          new Operation(timer, tupleStore, this)
	         .handle(new InputRequest(this, "query",
	                                  InputRequest.QUERY,
					  IS_AUDIO,
					  Duration.FOREVER,
					  true, 
					  null));
	  return true;

	} else if (e instanceof QueryResponse) {
	  QueryResponse qr = (QueryResponse)e;

	  synchronized (lock) {
	    iterator = qr.iter;
	    queryMaintainer =
	        new LeaseMaintainer(qr.lease, qr.duration, 
	                            this, "lease", timer);
          }

          // Start iterating.
	  iterator.handle(new IteratorRequest(this, null));
	  return true;

	} else if (e instanceof IteratorElement) {
	  IteratorElement ie = (IteratorElement)e;

	  synchronized (lock) {
	    tupleIds.add(ie.element);
	  }
	  
	  if (ie.hasNext) {
	    iterator.handle(new IteratorRequest(this, null));
	  } else {
	    // All done!
	    queryMaintainer.cancel();
	    respond(initialEvent, new DynamicTuple(this, null));
	  }
	  
	  return true;

	} else if (e instanceof IteratorEmpty) {
	  queryMaintainer.cancel();
	  audio.handle(new ExceptionalEvent(this, null,
	      new NoSuchTupleException("No audio files in " + path)));
          control.stop(null);
	  return true;
	}
        return false;
      }
    }

    /** Creates a new audio tuple logic. */
    AudioTupleLogic(String path) {
      this.path = path;
      this.tupleIds = new LinkedList();
    }

    /** Starts this audio tuple logic. */
    void start(Event e) {
      synchronized (lock) {
        startEvent = e;
      }

      // Cancel old timer.
      cancelTimer();

      // Start by binding the tuple store
      new Operation(timer, request, this).handle(
          new BindingRequest(this, "bind", 
	                     new SioResource(path),
			     Duration.FOREVER));
    }
    
    /** Releases all resources. */
    protected void releaseResources() {
      releaseResources(true);
    }

    /** 
     * Releases resources.
     *
     * @param releaseTupleStore  True if the tuple store binding should be
     *                            released.
     */
    protected void releaseResources(boolean releaseTupleStore) {
      synchronized (lock) {
        super.releaseResources();

	if (pipe != null) {
	  try {
	    pipe.close();
	  } catch (IOException x) {
	  }
	  pipe = null;
	}
        
	if (pipeIn != null) {
	  pipeIn.close();
	  pipeIn = null;
	}

        startEvent = null;
	nextChunk = null;
	reading = false;

	pipeSize = Integer.MAX_VALUE;

	if (releaseTupleStore && leaseMaintainer != null) {
	  leaseMaintainer.cancel();
	  leaseMaintainer = null;
	}
      }
    }

    /** 
     * Goes on to the next audio track.
     *
     * @param remove  True if the current track's tuple id should be 
     *                removed from the list.
     */
    protected void next(boolean remove) {
      synchronized (lock) {

        // Release any resources we are holding for the current audio
	// tuple (but not the tuple store).
        releaseResources(false);

        // Get a list iterator for the tuple ids.
        ListIterator iterator = tupleIds.listIterator();

	// Skip to the appropriate place in the list.
        while (iterator.hasNext()) {
          if (iterator.next().equals(currentHead)) {
	    break;
	  }
	}

        // Remove the id, if necessary.
	if (remove) {
	  iterator.remove();
	} 

        // Get the next id.
        if (iterator.hasNext()) {
	  // Go to next tuple id
	  currentHead = (Guid)iterator.next();

	} else if (tupleIds.size() > 0) {
	  // Wrap around to start
	  currentHead = (Guid)tupleIds.getFirst();

	} else {
	  // Nothing left to play!
	  audio.handle(new ExceptionalEvent(this, null, 
	               new NoSuchTupleException(
	                  "No audio files left to play in " + path)));
          control.stop(null);
	  return;
	}

	// Reset the sequence number.
	//sequenceNumber = 0;
        curPosition = 0;
	// Read the tuple. 
	Query q = new Query("id", Query.COMPARE_EQUAL, currentHead);
	tupleOp.handle(new InputRequest(this, "read head",
	                                InputRequest.READ,
					q, 0, false, null));
      }
    }

    /** 
     * Opens an audio input stream and handles all exceptions. 
     *
     * @param in  The input stream to open as an audio input stream.
     * @return true on success.
     */
    protected boolean openInputStream(InputStream in) {
      try {
        synchronized (lock) {
	  inputStream = AudioSystem.getAudioInputStream(in);
	}
      } catch (IOException x) {
	SystemUtilities.debug("Cannot read " + sourceName
	                      + "; skipping");
        SystemUtilities.debug(x);
	next(true);
	return false;

      } catch (UnsupportedAudioFileException x) {
	SystemUtilities.debug(sourceName + " has unsupported "
	                      + "audio file format; skipping");
        SystemUtilities.debug(x);
	next(true);
	return false;
      }
      return true;
    }

    /** Starts streaming audio, skipping data as necessary. */
    protected void startStreaming() {
      try {
        if (skipBytes > 0) {
          inputStream.skip(skipBytes);
	  skipBytes = 0;
        }
        super.startStreaming();
      } catch (IOException x) {
        SystemUtilities.debug(x);
	next(false);
      }
    }

    /** Attempts to read enough data to fill the pipe. */
    protected void fillPipe() {
      boolean start = false;

      try {
        synchronized (lock) {
	  int available = pipeIn.available();

	  if (inputStream == null 
	        && (available > headerLength || nextChunk == null)) {

            if (openInputStream(pipeIn)) {
	      init();

	      // The number of bytes to skip at the beginning of the
	      // track.
	      skipBytes = curPosition * buffer.length;

	      // The number of bytes in one second worth of data.
	      pipeSize = 
	          (int)(descriptor.frameRate * descriptor.frameSize);
              if (DEBUG) {
                SystemUtilities.debug("pipeSize " + pipeSize);
	      }

            } else {
	      // Opening the audio input stream failed.
	      return;
	    }
	  }

	  if (inputStream != null) {

            if (nextChunk == null) {
	      // There's no more data to read.
	      start = true;
	    }

	    // Get how many bytes are really available.
	    available = inputStream.available();
           
	    // Skip as much as possible, to avoid using too much memory to
	    // hold stuff we are skipping.
          
	    if (skipBytes > 0 && inputStream != null) {
	      int skipNow;
	      if (skipBytes < available) {
	        skipNow = skipBytes;
	      } else {
	        skipNow = available;
	      }
	      if (DEBUG) {
	        SystemUtilities.debug("Skipping " + skipNow);
	      }

	      try {
	        inputStream.skip(skipNow);
	        available -= skipNow;
	        skipBytes -= skipNow;
	      } catch (IOException x) {
	        SystemUtilities.debug(x);
	        next(false);
	        return;
	      }
	    }

	    if (available >= pipeSize) {
              // We read enough data to get started.
	      start = true;
	    } 
	  }
        }
      } catch (IOException x) {
        // Why on earth does InputStream.available() throw an IOException?
	SystemUtilities.debug(x);
	next(false);
	return;
      }

      if (start) {
	startStreaming();
      } else {
        readTuple(nextChunk, "fill pipe");
      }
    }

    /** Reads the head of an audio track. */
    protected void readHead(Guid g) {
      readTuple(g, "read head");
    }

    /** Attempts to read the next chunk in an audio track. */
    protected void readChunk(Guid g) {
      if (!reading && (g != null)) {
        synchronized (lock) {
	  if (reading) {
	    return;
	  }

	  reading = true;
	}

        if (DEBUG) {
          SystemUtilities.debug(this + " reading");
        }

        readTuple(g, "read chunk");
      }
    }

    /** Reads a tuple. */
    protected void readTuple(Guid g, Object closure) {
      if (g != null) {
        Query q = new Query("id", Query.COMPARE_EQUAL, g);
	tupleOp.handle(new InputRequest(this, closure,
	                                InputRequest.READ,
					q, 0, false, null));
      }
    }

    /** Handles events. */
    protected boolean handle1(Event e) {
      if ("initialize".equals(e.closure)) {
        Guid head;
        synchronized (lock) {
	  initialized = true;
	  head = currentHead = (Guid) tupleIds.getFirst();
	}
	readHead(head);
	return true;

      } else if ("bind".equals(e.closure)) {
      
        if (e instanceof BindingResponse) {

	  BindingResponse br = (BindingResponse)e;
          Event r;
          Guid head;

	  synchronized (lock) {
	    // Keep the tuple store resource and make a lease maintainer 
	    // and operation for it.
	    tupleStore = br.resource;
	    leaseMaintainer =
	        new LeaseMaintainer(br.lease, br.duration, 
	                            this, "lease", timer);
            tupleOp = new Operation(timer, tupleStore, this);

	    r = startEvent;
	    startEvent = null;

	    head = currentHead;
	  }

          // Report a successful startup.
	  if (r != null) {
	    control.activate(r);
	  }

          if (initialized) {
	    // Ready to start.
	    readHead(head);
	    
	  } else {
	    // Need to initialize.
	    new InitializationLogic().handle(
	        new DynamicTuple(this, "initialize"));
	  }

	  return true;

	} else if (e instanceof ExceptionalEvent) {
	
	  // Report exceptions in binding to the start requestor.
	  Event r;
	  synchronized (lock) {
	    r = startEvent;
	    startEvent = null;
	  }
	  if (r != null) {
	    respond(r, ((ExceptionalEvent)e).x);
	  }

	  control.stop(null);

	  return true;
	}

      } else if ("fill pipe".equals(e.closure)) {
        if (e instanceof InputResponse) {
	  Chunk chunk = (Chunk) ((InputResponse)e).tuple;
	  try {
	    synchronized (lock) {
	      pipe.write(chunk.data);
	      nextChunk = chunk.next;
	    }
          } catch (IOException x) {
	    // IOException while writing data to pipe
	    SystemUtilities.debug(x);
	    next(false);
	  }

	  fillPipe();
	  return true;

	} 

      } else if ("read head".equals(e.closure)) {
        if (e instanceof InputResponse) {
	  BinaryData data = (BinaryData) ((InputResponse)e).tuple;
          synchronized (lock) {
	    sourceName = data.name;

	    if (data instanceof Chunk) { 

	      try {
	        pipeIn = new RubberPipedInputStream();
	        pipe = new RubberPipedOutputStream(pipeIn);
	        pipe.write(data.data);
	        nextChunk = ((Chunk)data).next;
	        fillPipe();

	      } catch (IOException x) {
	        // This shouldn't happen, as far as I can tell.  Grumble.
		SystemUtilities.debug(x);
		next(false);
	      }

	    } else {
	      nextChunk = null;
	      if (openInputStream(new ByteArrayInputStream(data.data))) {
                init();
		skipBytes = curPosition * buffer.length;
		startStreaming();
	      }
	    }
	    return true;
	  }

	} else if (e instanceof ExceptionalEvent) {
	  ExceptionalEvent xe = (ExceptionalEvent)e;
	  if (xe.x instanceof NoSuchTupleException) {
	    SystemUtilities.debug("Cannot find tuple " + currentHead
	                          + "; skipping");
            SystemUtilities.debug(xe.x);
	    next(true);
	    return true;

	  } else if (xe.x instanceof LeaseException
	             || xe.x instanceof ResourceRevokedException) {
            // Stop and forward the exception to the audio handler.
            xe.source = this;
            audio.handle(xe);
	    control.stop(null);
	    return true;
	  }
	}
	
      } else if (e.hasField("send")) {

        // If we're already sending right now, go away.
	// (This avoid piling up a whole bunch of timer events waiting on
	// this lock.)
        if (sending || (state != ACTIVE)) {
	  return true;
	}

        // Initialize to true so we won't try to next() if not active
	boolean sent = true;

	// Next chunk to read (if any)
	Guid n = null;

        try {
          synchronized (lock) {
            // If we are active, send data and see if we need more.
	    if (state == ACTIVE) {
	      sent = sendData();

	      if (DEBUG && inputStream != null) {
	        SystemUtilities.debug("Available: " +
	                              inputStream.available());
              }

	      if ((inputStream != null) 
	          && (inputStream.available() < pipeSize)) {
	        n = nextChunk;
	      }

	    }
	  }
	} catch (IOException x) {
	  // Why does InputStream.available() throw an IOException?
	  SystemUtilities.debug(x);
	  next(false);
	}

	readChunk(n);
	
	if (!sent) {
	  // If no data was sent, then we're out of data.
	  audio.handle(new TextMessage(this, null, sender, channel,
	                               "You've been listening to "
				       + sourceName));

	  // Go to the next tuple.
	  next(false);
	}
	return true;

      } else if ("read chunk".equals(e.closure)) {

        if (e instanceof InputResponse) {
	  Chunk chunk = (Chunk) ((InputResponse)e).tuple;
	  Guid n = null;

	  if (!chunk.id.equals(nextChunk)) {
	    return true;
	  }

	  try {
	    synchronized (lock) {
	      if (chunk.id.equals(nextChunk)) {
	        if (DEBUG) {
	          SystemUtilities.debug(this + " done reading");
		}
		reading = false;
	        pipe.write(chunk.data);
	        nextChunk = chunk.next;
	      }
	    }
	  } catch (IOException x) {
	    // IOException while writing data to pipe or checking for
	    // available data
	    SystemUtilities.debug(x);
	    next(false);
	  }
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
    new ComponentDescriptor("one.radio.AudioSource",
                            "An audio sink component",
                            true);

  /** The exported event handler descriptor for the control handler. */
  private static final ExportedDescriptor CONTROL =
    new ExportedDescriptor("control",
                           "The audio source control handler",
                           new Class[] { ControlEvent.class },
                           new Class[] { LineUnavailableException.class,
                                         IllegalArgumentException.class,
                                         SecurityException.class },
                           false);

  /** 
   * The imported event handler descriptor for the audio handler.
   */
  private static final ImportedDescriptor AUDIO =
    new ImportedDescriptor("audio",
                           "The audio message handler",
                           new Class[] { AudioMessage.class,
			                 TextMessage.class },
		           new Class[] { NoSuchTupleException.class },
                           false, 
			   false);

  /** 
   * The imported event handler descriptor for the environment's request 
   * handler. 
   */
  private static final ImportedDescriptor REQUEST =
    new ImportedDescriptor("request",
                           "The environment request handler",
                           new Class[] { ControlEvent.class },
                           null,
                           false, 
			   false);


  // =======================================================================
  //                           Component states
  // =======================================================================
  
  /** The inactive state. */
  static final int INACTIVE = 0;

  /** The active state. */
  static final int ACTIVE = 1;

  /** The paused state. */
  static final int PAUSED = 2;

  // =======================================================================
  //                           Other constants
  // =======================================================================

  /**
   * The audio format to use for capturing audio.
   *
   * <p>This is used by {@link AudioSink} to open a source data line (and
   * not play anything to it) to ensure that we can capture in this
   * format.  See <A
   * HREF="http://developer.java.sun.com/developer/bugParade/bugs/4347309.html">JDK
   * bug #4347309</A>.</p>
   *
   * <p>We generously use a higher sampling rate, 16-bit resolution, and
   * stereo so that music will sound good as well.  If, for instance, we
   * sampled at 8000 Hz, 44100 Hz music would be sampled down and would
   * sound terrible.</p>
   */
  static final AudioFormat CAPTURE_FORMAT =
      new AudioFormat(44100, 16, 2, true, true);

  /** 
   * The number of bytes to read at a time.
   */
  // FIXME: On a local network, larger results in fewer drops; this
  // suggests a problem in our system.
  public static final int BUFFER_LENGTH = 8192;

  /** 
   * A query that is true only for audio tuples:  The tuple is a {@link
   * BinaryTuple} or the head of a list of chunks and its mime type is 
   * "audio/basic" or "audio/x-aiff" or "audio/x-wav".
   *
   * <pre>
   * ((("" HAS_TYPE BinaryData) 
   *    || (("" HAS_TYPE Chunk) && ("previous" == null)))
   * && (("type" == "audio/basic") 
   *       || ("type" == "audio/x-aiff")
   *       || ("type" == "audio/x-wav")))
   * </pre>
   */
  static final Query IS_AUDIO = 
      new Query(
        new Query(
          new Query("", Query.COMPARE_HAS_TYPE, BinaryData.class),
	  Query.BINARY_OR,
	  new Query(
	    new Query("", Query.COMPARE_HAS_TYPE, Chunk.class),
	    Query.BINARY_AND,
	    new Query("previous", Query.COMPARE_EQUAL, null))),
        Query.BINARY_AND,
	new Query(
	  new Query("type", Query.COMPARE_EQUAL, "audio/basic"),
	    Query.BINARY_OR,
	    new Query(new Query("type", Query.COMPARE_EQUAL, "audio/x-aiff"),
	              Query.BINARY_OR,
	              new Query("type", Query.COMPARE_EQUAL, "audio/x-wav"))));

  /** Debug or not? */
  static final boolean DEBUG = false;

  // =======================================================================
  //                           Instance fields
  // =======================================================================

  /**
   * The control exported event handler.
   *
   * @serial  Must not be <code>null</code>.
   */
  final ControlHandler    control;

  /** 
   * The audio imported event handler. 
   *
   * @serial  Must not be <code>null</code>.
   */
  final Component.Importer audio;

  /** 
   * The request imported event handler. 
   *
   * @serial  Must not be <code>null</code>.
   */
  final Component.Importer request;

  /** The sender name. */
  final String sender;

  /** The channel name. */
  final String channel; 

  /** The source logic. */
  final SourceLogic sourceLogic;

  /** The timer component. */
  final Timer timer;

  /** The lock for component state. */
  transient Object lock;

  /** The component state. */
  transient int state;

  // =======================================================================
  //                           Constructors
  // =======================================================================

  /**
   * Create a new instance of <code>AudioSource</code> that streams
   * captured audio.
   *
   * @param  env      The environment for the new instance.
   * @param  sender   The sender name to use in the generated messages.
   * @param  channel  The channel name to use in the generated messages.
   */
  public AudioSource(Environment env, String sender, String channel) {
    super(env);

    control = new ControlHandler();
    declareExported(CONTROL, control);

    audio   = declareImported(AUDIO);
    request = declareImported(REQUEST);

    this.sender = sender;
    this.channel = channel;

    sourceLogic = new CaptureLogic();
    timer = getTimer();
    lock = new Object();
  }

  /**
   * Create a new instance of <code>AudioSource</code> that streams audio
   * from tuples in an environment.
   *
   * @param  env      The environment for the new instance.
   * @param  sender   The sender name to use in the generated messages.
   * @param  channel  The channel name to use in the generated messages.
   * @param  path     The relative path of the environment to play 
   *                  sound tuples from.
   */
  public AudioSource(Environment env, String sender, String channel,
                     String path) {
    super(env);

    control = new ControlHandler();
    declareExported(CONTROL, control);

    audio   = declareImported(AUDIO);
    request = declareImported(REQUEST);

    this.sender = sender;
    this.channel = channel;

    sourceLogic = new AudioTupleLogic(path);
    timer = getTimer();
    lock = new Object();
  }


  // =======================================================================
  //                           Component support
  // =======================================================================

  /** Get the component descriptor. */
  public ComponentDescriptor getDescriptor() {
    return (ComponentDescriptor)SELF.clone();
  }

  /**
   * Serialize this audio source.
   *
   * @serialData    The default fields while holding the lock.
   */
  protected void writeObject(ObjectOutputStream out) throws IOException {
    synchronized (lock) {
      out.defaultWriteObject();
    }
  }

  /** Deserialize this audio source. */
  private void readObject(ObjectInputStream in)
      throws IOException, ClassNotFoundException {

    // Read the non-transient fields.
    in.defaultReadObject();

    // Restore transients.
    lock = new Object();
    state = INACTIVE;
  }
}
