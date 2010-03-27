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
package one.net.http;

import one.world.core.Event;
import one.world.core.EventHandler;
import one.world.core.InvalidTupleException;
import one.world.core.TupleException;
import java.util.Date;

/**
 * The <code>LogEvent</code> is sent to <code>Logger</code> which will
 * write these to the tuple store as log entries.
 *
 * @author  Daniel Cheah
 * @version $Revision: 1.3 $
 */
public class LogEvent extends Event {
  /** <code>INFO</code> Log Level. */
  public static final int INFO     = 1;

  /** <code>WARNING</code> Log Level. */
  public static final int WARNING  = 2;

  /** <code>ERROR</code> Log Level. */
  public static final int ERROR    = 3;

  /** Where the log entry came from. */
  public String from;

  /** The message to be logged. */
  public String entry;
 
  /** Timestamp when this log event was generated */
  public String when;

  /** 
   * The level at which this log event is at.
   * @see LogEvent#INFO
   * @see LogEvent#WARNING
   * @see LogEvent#ERROR
   */
  public int    level;

  /** Creates a new <code>LogEvent</code> instance. */
  public LogEvent() {
    super();
    this.source = one.world.util.NullHandler.NULL;
  }

  /**
   * Creates a new <code>LogEvent</code> instance.
   *
   * @param source  The source of the event.
   * @param closure The closure for the event.
   */
  public LogEvent(EventHandler source, Object closure) {
    super(source, closure);
  }

  /**
   * Validate this event. Makes sure that String fields are not 
   * null and that <code>level</code> is one of the valid log
   * levels.
   *
   * @exception TupleException Signals that this tuple's data is invalid.
   */
  public void validate() throws TupleException {
    super.validate();

    if (from == null) {
      throw new InvalidTupleException("from field is null");
    }

    if (entry == null) {
      throw new InvalidTupleException("message field is null");
    }

    if (when == null) {
      throw new InvalidTupleException("date field is null");
    }

    switch (level) {
    case INFO:
    case WARNING:
    case ERROR:
      break;
    default:
      throw new InvalidTupleException("level field is not valid");
    }
  }

  /**
   * Convert this log entry to a string.
   *
   * @return String representation of this log event.
   */
  public String toString() {
    StringBuffer buf = new StringBuffer();

    buf.append(when);
    buf.append(": ");
    buf.append("[");
    buf.append(level);
    buf.append("][");
    buf.append(from);
    buf.append("][");
    buf.append(entry);
    buf.append("]");
                                        
    return buf.toString();
  }

  /**
   * Helper function to create an information log entry.
   *
   * @param fm  Message From.
   * @param msg Log Entry Message.
   * @return The specified <code>LogEvent</code>.
   */
  public static LogEvent log(String fm, String msg) {
    LogEvent le  = new LogEvent(); 
    le.from  = fm;
    le.entry = msg;
    le.level = INFO;
    le.when  = (new Date()).toString();
    return le;
  }

  /**
   * Helper function to create a warning log entry.
   *
   * @param fm  Message From.
   * @param msg Log Entry Message.
   * @return The specified <code>LogEvent</code>.
   */
  public static LogEvent logWarning(String fm, String msg) {
    LogEvent le  = new LogEvent();
    le.from  = fm;
    le.entry = msg;
    le.level = WARNING;
    le.when  = (new Date()).toString();;
    return le;
  }

  /**
   * Helper function to create an error log entry.
   *
   * @param fm  Message From.
   * @param msg Log Entry Message.
   * @return The specified <code>LogEvent</code>.
   */
  public static LogEvent logError(String fm, String msg) {
    LogEvent le  = new LogEvent();
    le.from  = fm;
    le.entry = msg;
    le.level = ERROR;
    le.when  = (new Date()).toString();
    return le;
  }
}
