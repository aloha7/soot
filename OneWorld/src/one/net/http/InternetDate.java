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

import java.util.Date;
import java.util.TimeZone;
import java.util.Locale;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * InternetDate parses and unparses date and time
 * specifiations. Specifically, it parses date and time specifications
 * in three formats: <a
 * href="http://info.internet.isi.edu:80/in-notes/rfc/files/rfc822.txt">
 * RFC 822</a> updated by <a
 * href="http://info.internet.isi.edu:80/in-notes/rfc/files/rfc1123.txt">
 * RFC 1123</a>, <a
 * href="http://info.internet.isi.edu:80/in-notes/rfc/files/rfc850.txt">
 * RFC 850</a> obsoleted by <a
 * href="http://info.internet.isi.edu:80/in-notes/rfc/files/rfc1036.txt">
 * RFC 1036</a>, and ANSI C's asctime() format. It only unparses into
 * the RFC 822 / 1123 format, as this format is the preferred format
 * for Internet usage.
 *
 * @author   Robert Grimm
 * @version  $Revision: 1.3 $
 */

public class InternetDate {
  /** RFC 822 / 1123 format, but unspecified time zone. */
  protected static final String FORM_822 = "EEE, dd MMM yyyy HH:mm:ss zzz";
  /** RFC 850 / 1036 format, but unspecified time zone. */
  protected static final String FORM_850 = "EEEE, dd-MMM-yy HH:mm:ss zzz";
  /** asctime format, two spaces and one date digit. */
  protected static final String FORM_C_1 = "EEE MMM  d HH:mm:ss yyyy";
  /** asctime format, one space and two date digits. */
  protected static final String FORM_C_2 = "EEE MMM dd HH:mm:ss yyyy";
  /** RFC 822 / 1123 format. */
  protected static final String FORM_OUT = "EEE, dd MMM yyyy HH:mm:ss 'GMT'";

  /** The time zone object for GMT. */
  protected static final TimeZone gmt = TimeZone.getTimeZone("GMT");

  /** Date formatter for RFC 822 / 1123 with unspecified time zone. */
  protected DateFormat form822;
  /** Date formatter for RFC 850 / 1036 with unspecified time zone. */
  protected DateFormat form850;
  /** Date formatter for asctime format with one date digit. */
  protected DateFormat formC1;
  /** Date formatter for asctime format with two date digits. */
  protected DateFormat formC2;
  /** Date formatter for RFC 822 / 1123 (in GMT). */
  protected DateFormat formOut;

  /**
   * Create a new InternetDate.
   */
  public InternetDate() {
    // Create new formatters.
    form822 = new SimpleDateFormat(FORM_822, Locale.US);
    form850 = new SimpleDateFormat(FORM_850, Locale.US);
    formC1  = new SimpleDateFormat(FORM_C_1, Locale.US);
    formC2  = new SimpleDateFormat(FORM_C_2, Locale.US);
    formOut = new SimpleDateFormat(FORM_OUT, Locale.US);

    // Set their time zone to GMT.
    formC1.setTimeZone(gmt);
    formC2.setTimeZone(gmt);
    formOut.setTimeZone(gmt);
  }

  /**
   * Parse a date and time. Attempts to parse a string according to
   * RFC 822 / 1123, RFC 850 / 1036, and ANSI C's asctime() formats in
   * this order. *Not threadsafe*.
   *
   * @param   s                  The string describing the date
   *                             and time.
   * @return                     The parsed date and time.
   * @exception  ParseException  Signals that the date and time
   *                             specification is in an unrecognized
   *                             format.
   */
  public Date parse(String s) throws ParseException {
    Date date;

    try {
      date = form822.parse(s);
    } catch (java.text.ParseException x) {
      try {
        date = form850.parse(s);
      } catch (java.text.ParseException xx) {
        try {
          date = formC1.parse(s);
        } catch (java.text.ParseException xxx) {
          try {
            date = formC2.parse(s);
          } catch (java.text.ParseException xxxx) {
            throw new ParseException("Invalid date and time");
          }
        }
      }
    }
  
    return date;
  }

  /**
   * Format a date and time. *Not threadsafe*.
   *
   * @param   date  The date and time to format in RFC 822 / 1123
   *                format.
   * @return        A string representing the date and time in
   *                RFC 822 / 1123 format.
   */
  public String format(Date date) {
    return formOut.format(date);
  }
}
