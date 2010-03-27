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

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;

import one.world.binding.BindingRequest;
import one.world.binding.BindingResponse;
import one.world.binding.Duration;
import one.world.binding.LeaseMaintainer;

import one.world.core.Component;
import one.world.core.ComponentDescriptor;
import one.world.core.Environment;
import one.world.core.Event;
import one.world.core.EventHandler;
import one.world.core.ExceptionalEvent;
import one.world.core.ExportedDescriptor;
import one.world.core.ImportedDescriptor;

import one.world.env.EnvironmentEvent;

import one.world.rep.RemoteDescriptor;
import one.world.rep.RemoteEvent;

import one.world.util.AbstractHandler;
import one.world.util.Operation;
import one.world.util.SystemUtilities;
import one.world.util.Timer;

/**
 * Receives audio on a set of one.radio channels and plays it using an 
 * {@link AudioSink}.
 *
 * <p>Usage: AudioReceiver [channel]*</p>
 *
 * <p><b>Imported and Exported Event Handlers</b></p>
 *
 * <p>Exported event handlers:<dl>
 *    <dt>main</dt>
 *    <dd>The main environment event handler.
 *        </dd>
 * </dl></p>
 *
 * <p>Imported event handlers:<dl>
 *    <dt>request</dt>
 *    <dd>The environment request handler.
 *        </dd>
 *    <dt>sourceControl</dt>
 *    <dd>Accepts {@link AudioMessage}s and {@link TextMessage}s. 
 *        </dd>
 *    <dt>sinkControl</dt>
 *    <dd>Accepts {@link AudioSink.ControlEvent}s.
 *        </dd>
 * </dl></p>
 *
 * @version  $Revision: 1.7 $
 * @author   Janet Davis
 */
public final class AudioReceiver extends Component {

  // =======================================================================
  //                           The main handler
  // =======================================================================

  /** The main exported event handler. */
  final class MainHandler extends AbstractHandler {

    /** Handle the specified event. */
    protected boolean handle1(Event e) {

      if (e instanceof EnvironmentEvent) {
        EnvironmentEvent ee = (EnvironmentEvent)e;

        if (EnvironmentEvent.ACTIVATED == ee.type
	    || EnvironmentEvent.CLONED == ee.type
	    || EnvironmentEvent.MOVED  == ee.type
	    || EnvironmentEvent.RESTORED == ee.type) {

          // Start the audio sink.
          sinkOperation.handle(
	      new AudioSink.ControlEvent(this, null,
	                                 AudioSink.ControlEvent.START));
          return true;

	} else if (EnvironmentEvent.STOP == ee.type) {
	  stop(ee);
          return true;
	}
      } else if (e instanceof AudioSink.ControlEvent) {
        AudioSink.ControlEvent ce = (AudioSink.ControlEvent)e;

	switch (ce.type) {
	case AudioSink.ControlEvent.STARTED:
	  new BindLogic().start();
          return true;

	case AudioSink.ControlEvent.STOPPED:
	  // All stopped.
	  respond((Event)ce.closure, 
	          new EnvironmentEvent(this, null, 
				       EnvironmentEvent.STOPPED,
		                       getEnvironment().getId()));
          return true;
	}

      } else if (e instanceof ExceptionalEvent) {
        // Log and shut down
	SystemUtilities.debug("AudioReceiver got unexpected exception; "
	                      + "shutting down");
        SystemUtilities.debug(((ExceptionalEvent)e).x);
	stop(new EnvironmentEvent(request, null,
	                          EnvironmentEvent.STOP,
				  getEnvironment().getId()));
        return true;
      }

      return false;
    }
  }

  /** 
   * Release resources.
   *
   * @param event  The environmentEvent to respond to when everything is
   *               stopped.
   */
  void stop(Event event) {
    synchronized (lock) {
      for (int i = 0; i < leaseMaintainers.length; i++) {
        if (leaseMaintainers[i] != null) {
	  leaseMaintainers[i].cancel();
	  leaseMaintainers[i] = null;
	}
      }
    }

    sinkOperation.handle(
        new AudioSink.ControlEvent(null, event,
	                           AudioSink.ControlEvent.STOP));
  }


  // =======================================================================
  //                           Bind logic
  // =======================================================================
  
  /** Deals with binding channels. */
  final class BindLogic extends AbstractHandler {

    /** An operation. */
    Operation operation;

    /** Creates a new bind logic. */
    BindLogic() {
      operation = new Operation(timer, request, this);
    }

    /** Requests to bind a channel. */
    void bind(int i) {
      operation.handle(
          new BindingRequest(null, new Integer(i), 
	                     new RemoteDescriptor(source, 
			                          new Channel(channels[i])),
			     Duration.FOREVER));
    }

    /** Starts this bind logic. */
    void start() {
      bind(0);
    }

    /** Handles events. */
    protected boolean handle1(Event e) {
      if (e instanceof BindingResponse) {
        BindingResponse br = (BindingResponse)e;
        int i = ((Integer)br.closure).intValue();

	synchronized (lock) {
	  leaseMaintainers[i] = new LeaseMaintainer(br.lease, br.duration,
	                                            main, null, timer);
	}

        i++;
	if (i < channels.length) {
	  bind(i);
	} else {
	  // Done binding; print out an informative message
	  StringBuffer sb = new StringBuffer();
	  sb.append("AudioReceiver listening on ");
	  for (int j=0; j < channels.length - 1; j++) {
	    sb.append(channels[j]);
	    sb.append(", ");
	  }
	  sb.append(channels[channels.length-1]);
	  SystemUtilities.debug(sb.toString());
	}
	return true;

      } else if (e instanceof ExceptionalEvent) {
        SystemUtilities.debug("AudioReceiver got exception while binding "
	                      + "channels; stopping");
        SystemUtilities.debug(((ExceptionalEvent)e).x);
        stop(new EnvironmentEvent(request, null, 
				  EnvironmentEvent.STOP,
	                          getEnvironment().getId())); 
	return true;
      }

      return false;
    }
  }

  // =======================================================================
  //                           The source handler
  // =======================================================================

  /** The source exported event handler. */
  final class SourceHandler extends AbstractHandler {

    /** Handle the specified event. */
    protected boolean handle1(Event e) {

      if (e instanceof RemoteEvent) {
        RemoteEvent re = (RemoteEvent)e;

	if (re.event instanceof AudioMessage) { 
          // Pass audio messages on to the audio sink.
	  AudioMessage message = (AudioMessage)re.event;
          message.source = this;
	  sink.handle(message);
	  return true;

	} else if (re.event instanceof TextMessage) {
	  // Display the message.
	  TextMessage message = (TextMessage)re.event;
	  SystemUtilities.debug(message.sender + " to " 
	                        + message.channel + ": "
				+ message.msg);
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
    new ComponentDescriptor("one.radio.AudioReceiver",
                            "Audio player",
                            true);

  /** The exported event handler descriptor for the main handler. */
  private static final ExportedDescriptor MAIN =
    new ExportedDescriptor("main",
                           "Main handler",
                           new Class[] { EnvironmentEvent.class },   
                           null,  
                           false);

  /** The imported event handler descriptor for the request handler. */
  private static final ImportedDescriptor REQUEST =
    new ImportedDescriptor("request",
                           "Environment request handler",
                           new Class[] { EnvironmentEvent.class },
                           null,   
                           false,
                           false);

  /** The imported event handler descriptor for the sink handler. */
  private static final ImportedDescriptor SINK =
    new ImportedDescriptor("sink",
                           "Audio sink handler",
                           new Class[] { AudioMessage.class },
                           null,   
                           false,
                           false);

  /** 
   * The imported event handler descriptor for the sinkControl
   * handler. 
   */
  private static final ImportedDescriptor SINKCONTROL =
    new ImportedDescriptor("sinkControl",
                           "Audio sink control handler",
                           new Class[] { AudioSink.ControlEvent.class },
                           null,   
                           false,
                           false);


  // =======================================================================
  //                           Instance fields
  // =======================================================================

  /**
   * The main exported event handler.
   *
   * @serial  Must not be <code>null</code>.
   */
  final EventHandler       main;

  /**
   * The request imported event handler.
   *
   * @serial  Must not be <code>null</code>.
   */
  final Component.Importer request;

  /**
   * The sink imported event handler.
   *
   * @serial  Must not be <code>null</code>.
   */
  final Component.Importer sink;

  /**
   * The sinkControl imported event handler.
   *
   * @serial  Must not be <code>null</code>.
   */
  final Component.Importer sinkControl;

  /**
   * The audio source event handler.
   *
   * @serial Must not be <code>null</code>
   */
  final EventHandler source;

  /** A timer component. */
  final Timer timer;

  /** An operation for the audio sink. */
  final Operation sinkOperation;

  /** The channels to listen to. */
  final String[] channels;

  /** The lease maintainers for the channels. */
  transient LeaseMaintainer[] leaseMaintainers;

  /** A lock object. */
  transient Object lock;


  // =======================================================================
  //                           Constructor
  // =======================================================================

  /**
   * Create a new instance of <code>AudioReceiver</code>.
   *
   * @param  env  The environment for the new instance.
   * @param  channels  The channels to listen to.
   */
  public AudioReceiver(Environment env, String[] channels) {
    super(env);
    main = declareExported(MAIN, new MainHandler());
    request = declareImported(REQUEST);
    sink = declareImported(SINK);
    sinkControl = declareImported(SINKCONTROL);

    source = new SourceHandler();

    this.channels = channels;
  
    // Get a timer.
    timer = getTimer();

    // Set up useful operations.
    sinkOperation = new Operation(timer, sinkControl, main);

    // Transients.
    initializeTransients();
  }

  /** Initialize transient members. */
  void initializeTransients() {
    leaseMaintainers = new LeaseMaintainer[channels.length];
    lock = new Object();
  }

  /**
   * Serialize this audio receiver.
   *
   * @serialData    The default fields while holding the lock.
   */
  protected void writeObject(ObjectOutputStream out) throws IOException {
    synchronized (lock) {
      out.defaultWriteObject();
    }
  }

  /** Deserialize this audio receiver. */
  private void readObject(ObjectInputStream in)
      throws IOException, ClassNotFoundException {

    // Read the non-transient fields.
    in.defaultReadObject();
    
    // Restore transients.
    initializeTransients();
  }

  // =======================================================================
  //                           Component support
  // =======================================================================

  /** Get the component descriptor. */
  public ComponentDescriptor getDescriptor() {
    return (ComponentDescriptor)SELF.clone();
  }

  /** 
   * Initialize an environment with this component.
   *
   * @param env      The environment to initialize.
   * @param closure  A string array containing a list of channels to
   *                 listen to.
   */
  public static void init(Environment env, Object closure) 
          throws Throwable {
    String[] args = (String[])closure;

    if (args.length < 1) {
      throw new IllegalArgumentException("Usage: AudioReceiver [channel]*");
    }

    AudioReceiver player = new AudioReceiver(env, args);
    AudioSink   sink   = new AudioSink(env);

    env.link("main", "main", player);
    player.link("request", "request", env);

    player.link("sinkControl", "control", sink);
    player.link("sink", "audio", sink);
  }
}
