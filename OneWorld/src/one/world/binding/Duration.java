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

package one.world.binding;

/**
 * The definition of duration constants. Durations of leases are
 * expresed in milliseconds, consistent with Java's use of
 * milliseconds, with the exception of {@link #FOREVER}, which has the
 * maximum long value and denotes an infinite duration, and {@link
 * #ANY}, which has -1 as its value and denotes any duration. As a
 * result, all non-negative long numbers, besides the maximum value,
 * can be used as valid, specific durations.
 *
 * @version  $Revision: 1.6 $
 * @author   Robert Grimm
 */
public final class Duration {

  /** Hide constructor. */
  private Duration() {
  }

  /** A second. */
  public static final long SECOND   = 1000;

  /** A minute. */
  public static final long MINUTE   = 60 * SECOND;

  /** An hour. */
  public static final long HOUR     = 60 * MINUTE;

  /** A day. */
  public static final long DAY      = 24 * HOUR;

  /** A year. */
  public static final long YEAR     = 365 * DAY;

  /** Forever. */
  public static final long FOREVER  = Long.MAX_VALUE;

  /** Any duration. */
  public static final long ANY      = -1;

  /**
   * Format the specified duration in milliseconds as years, days,
   * hours, minutes, and seconds.
   *
   * @param   millis  The duration in milliseconds.
   * @return          The string representation in years, days, hours,
   *                  minutes, and seconds.
   */
  public static String format(long millis) {
    long years    = millis / YEAR;
    millis       -= years * YEAR;

    long days     = millis / DAY;
    millis       -= days * DAY;

    long hours    = millis / HOUR;
    millis       -= hours * HOUR;

    long minutes  = millis / MINUTE;
    millis       -= minutes * MINUTE;

    long seconds  = millis / SECOND;
    millis       -= seconds * SECOND;

    StringBuffer buf = new StringBuffer();
    if (0 < years) {
      buf.append(years);
      buf.append(" year");
      if (1 != years) {
        buf.append('s');
      }
      buf.append(' ');
    }
    if (0 < days) {
      buf.append(days);
      buf.append(" day");
      if (1 != days) {
        buf.append('s');
      }
      buf.append(' ');
    }
    if (0 < hours) {
      buf.append(hours);
      buf.append(" hour");
      if (1 != hours) {
        buf.append('s');
      }
      buf.append(' ');
    }
    if (0 < minutes) {
      buf.append(minutes);
      buf.append(" minute");
      if (1 != minutes) {
        buf.append('s');
      }
      buf.append(' ');
    }
    buf.append(seconds);
    if (0 != millis) {
      if (99 < millis) {
        buf.append(".");
      } else if (9 < millis) {
        buf.append(".0");
      } else {
        buf.append(".00");
      }
      buf.append(millis);
    }
    buf.append(" second");
    if ((1 != seconds) || (0 != millis)) {
      buf.append('s');
    }

    return buf.toString();
  }

}
