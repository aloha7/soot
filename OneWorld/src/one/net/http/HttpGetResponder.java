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
import one.world.core.Tuple;
import one.world.core.ExceptionalEvent;
import one.world.util.AbstractHandler;
import one.world.io.InputResponse;
import one.world.io.ListenResponse;
import one.world.binding.UnknownResourceException;
import one.world.binding.BindingException;

import java.util.Date;
import java.util.TimeZone;
import java.util.Locale;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * Handler for taking care of HTTP GET responses.
 *
 * @author  Daniel Cheah
 * @version $Revision: 1.2 $
 */
public final class HttpGetResponder extends ProxyHandler {
  /** 
   * Constructor.
   */
  public HttpGetResponder() {
    super();
  }
  
  /**
   * The tuple store proxy calls this method
   * to handle the relevant response
   * from the tuple store. The underlying 
   * handler properly responds to the 
   * initial request.
   */
  public boolean proxyHandle(Event e) {
    return handle1(e);
  }
  
  /** Handle the specified event. */
  protected boolean handle1(Event e) {
    if (e instanceof InputResponse) {
      handleInputResponse((InputResponse)e);
      return true;

    } else if (e instanceof ExceptionalEvent) {
      handleExceptionalEvent((ExceptionalEvent)e);
      return true;

    } else {
      return false;

    }
  }

  /** Create <code>HttpResponse</code> objects. */
  private HttpResponse createHttpResponse(Tuple t, String timeStampString, 
                                          int version) {

    HttpResponse res = new HttpResponse(version, HttpConstants.OK);

    res.body = t;
    res.header.set(HttpConstants.LAST_MODIFIED, timeStampString);
    res.header.set(HttpConstants.DATE, timeStampString);

    return res;
  }

  /** Handle <code>InputResponse</code> event. */
  private void handleInputResponse(InputResponse in) {
    TupleStoreProxy.Closure c   = (TupleStoreProxy.Closure)in.closure;
    HttpRequest             req = (HttpRequest)c.request;
    HttpResponse            res;

    if (null != in.tuple) {

      // DC: We don't check for chunks here. Leave it
      // to the client code to do the right thing.
      // That is check whether the current piece of
      // data is a chunk or a tuple.
      
      // DC: We ignore last-modified, always put the
      // current time there.
      // FIXME: Is there a way to create a new date
      // without always creating 2 new objects each time.
      Date   timeStamp       = new Date();
      String timeStampString = DateFormatter.format(timeStamp);

      res = createHttpResponse(in.tuple, timeStampString, req.version);

    } else {
      res = new HttpResponse(req.version, HttpConstants.NOT_FOUND);

    }

    respond(req, res);
  }

  /** Handle <code>ExceptionalEvent</code> event. */
  private void handleExceptionalEvent(ExceptionalEvent ee) {
    TupleStoreProxy.Closure c   = (TupleStoreProxy.Closure)ee.closure;
    HttpRequest             req = (HttpRequest)c.request;
    HttpResponse            res;

    if (ee.x instanceof UnknownResourceException) {
      res = new HttpResponse(req.version, HttpConstants.NOT_FOUND);
    } else {
      res = new HttpResponse(req.version, 
          HttpConstants.INTERNAL_SERVER_ERROR);
    }

    respond(req, res);
  }
}

