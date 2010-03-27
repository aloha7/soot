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

import one.world.core.Component;
import one.world.core.ComponentDescriptor;
import one.world.core.Environment;
import one.world.core.Event;
import one.world.core.EventHandler;
import one.world.core.ExceptionalEvent;
import one.world.core.ExportedDescriptor;
import one.world.core.ImportedDescriptor;

import one.world.env.EnvironmentEvent;

import one.world.util.AbstractHandler;
import one.world.util.Operation;
import one.world.util.SystemUtilities;
import one.world.util.Timer;

/**
 * Using {@link AudioSource} and {@link AudioSink}, plays either captured
 * audio or audio tuples in a sub-environment specified by &lt;path&gt;.
 *
 * <p>Usage: AudioPlayer &lt;path&gt;<p>
 *
 * <p><b>Imported and Exported Event Handlers</b></p>
 *
 * <p>Exported event handlers:<dl>
 *    <dt>main</dt>
 *    <dd>The main environment event handler.
 *        </dd>
 *    <dt>source</dt>
 *    <dd>Accepts {@link AudioMessage}s.
 *        </dd>
 * </dl></p>
 *
 * <p>Imported event handlers:<dl>
 *    <dt>request</dt>
 *    <dd>The environment request handler.
 *        </dd>
 *    <dt>sourceControl</dt>
 *    <dd>Accepts {@link AudioSource.ControlEvent}s.
 *        </dd>
 *    <dt>sink</dt>
 *    <dd>Accepts {@link AudioMessage}s. 
 *        </dd>
 *    <dt>sinkControl</dt>
 *    <dd>Accepts {@link AudioSink.ControlEvent}s.
 *        </dd>
 * </dl></p>
 *
 * @version  $Revision: 1.6 $
 * @author   Janet Davis
 */
public final class AudioPlayer extends Component {

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
	  // Stop the audio sink.
	  sinkOperation.handle(
	      new AudioSink.ControlEvent(this, ee, 
	                                   AudioSink.ControlEvent.STOP));
          return true;
	}
      } else if (e instanceof AudioSink.ControlEvent) {
        AudioSink.ControlEvent ce = (AudioSink.ControlEvent)e;

	switch (ce.type) {
	case AudioSink.ControlEvent.STARTED:
	  // Start the audio source.
	  sourceOperation.handle(
	      new AudioSource.ControlEvent(this, null,
	                                   AudioSink.ControlEvent.START));
          return true;

	case AudioSink.ControlEvent.STOPPED:
	  // Stop the audio source.
	  sourceOperation.handle(
	      new AudioSource.ControlEvent(this, ce.closure,
	                                   AudioSink.ControlEvent.STOP));
          return true;
	}

      } else if (e instanceof AudioSource.ControlEvent) {
        AudioSource.ControlEvent ce = (AudioSource.ControlEvent)e;

	switch (ce.type) {
	case AudioSource.ControlEvent.STARTED:
	  // All started.
	  SystemUtilities.debug("AudioPlayer started");
	  return true;

	case AudioSource.ControlEvent.STOPPED:
	  // All stopped.
	  respond((Event)ce.closure, 
	          new EnvironmentEvent(this, null, 
				       EnvironmentEvent.STOPPED,
		                       getEnvironment().getId()));
          return true;
	}
	
      } else if (e instanceof ExceptionalEvent) {
        // Log and shut down
	SystemUtilities.debug("AudioPlayer got unexpected exception; "
	                      + "shutting down");
        SystemUtilities.debug(((ExceptionalEvent)e).x);
	request.handle(new EnvironmentEvent(this, null,
	                                    EnvironmentEvent.STOPPED,
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

      if (e instanceof AudioMessage) {
        // Pass audio messages on to the audio sink.
        e.source = this;
	sink.handle(e);
	return true;

      } else if (e instanceof TextMessage) {
        // Print out text messages.
	SystemUtilities.debug(((TextMessage)e).msg);
	return true;

      } else if (e instanceof ExceptionalEvent) {
        // Log and shut down
	SystemUtilities.debug("AudioPlayer got unexpected exception; "
	                      + "shutting down");
        SystemUtilities.debug(((ExceptionalEvent)e).x);
	request.handle(new EnvironmentEvent(this, null,
	                                    EnvironmentEvent.STOPPED,
					    getEnvironment().getId()));
        return true;
      }

      return false;
    }
  }


  // =======================================================================
  //                           Descriptors
  // =======================================================================

  /** The component descriptor. */
  private static final ComponentDescriptor SELF =
    new ComponentDescriptor("one.radio.AudioPlayer",
                            "Audio player",
                            true);

  /** The exported event handler descriptor for the main handler. */
  private static final ExportedDescriptor MAIN =
    new ExportedDescriptor("main",
                           "Main handler",
                           new Class[] { EnvironmentEvent.class },   
                           null,  
                           false);

  /** The exported event handler descriptor for the source handler. */
  private static final ExportedDescriptor SOURCE =
    new ExportedDescriptor("source",
                           "Audio source handler",
                           new Class[] { AudioMessage.class },  
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
  /** 
   * The imported event handler descriptor for the sourceControl 
   * handler. 
   */
  private static final ImportedDescriptor SOURCECONTROL =
    new ImportedDescriptor("sourceControl",
                           "Audio source control handler",
                           new Class[] { AudioSource.ControlEvent.class },
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
   * The source exported event handler.
   *
   * @serial  Must not be <code>null</code>.
   */
  final EventHandler       source;

  /**
   * The request imported event handler.
   *
   * @serial  Must not be <code>null</code>.
   */
  final Component.Importer request;

  /**
   * The sourceControl imported event handler.
   *
   * @serial  Must not be <code>null</code>.
   */
  final Component.Importer sourceControl;

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

  /** A timer component. */
  final Timer timer;

  /** An operation for the audio sink. */
  final Operation sinkOperation;

  /** An operation for the audio source. */
  final Operation sourceOperation;


  // =======================================================================
  //                           Constructor
  // =======================================================================

  /**
   * Create a new instance of <code>AudioPlayer</code>.
   *
   * @param  env  The environment for the new instance.
   */
  public AudioPlayer(Environment env) {
    super(env);
    main = declareExported(MAIN, new MainHandler());
    source = declareExported(SOURCE, new SourceHandler());
    request = declareImported(REQUEST);
    sourceControl = declareImported(SOURCECONTROL);
    sink = declareImported(SINK);
    sinkControl = declareImported(SINKCONTROL);

    // Get a timer.
    timer = getTimer();

    // Set up useful operations.
    sinkOperation = new Operation(timer, sinkControl, main);
    sourceOperation = new Operation(timer, sourceControl, main);
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
   * @param closure  An array of strings, optionally containing the
   *                 relative path of an environment from which to
   *                 play sound tuples.  If no path is specified,
   *                 captured audio will be used instead.
   */
  public static void init(Environment env, Object closure) 
          throws Throwable {

    String[] args = (String[]) closure;
    if (args.length > 1) {
      throw new IllegalArgumentException("Usage: AudioPlayer <path>");
    }

    AudioPlayer player = new AudioPlayer(env);
    AudioSink   sink   = new AudioSink(env);
    AudioSource source;

    if (args.length > 0) {
      source = new AudioSource(env, "local", "direct", args[0]);
    } else {
      source = new AudioSource(env, "local", "direct");
    }

    env.link("main", "main", player);
    player.link("request", "request", env);

    player.link("sourceControl", "control", source);
    source.link("audio", "source", player);
    source.link("request", "request", env);

    player.link("sinkControl", "control", sink);
    player.link("sink", "audio", sink);
  }
}
