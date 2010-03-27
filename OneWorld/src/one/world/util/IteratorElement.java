/*
 * Copyright (c) 2000, 2001 University of Washington, Department of
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

package one.world.util;

import one.world.core.Event;
import one.world.core.EventHandler;
/**
 * Implementation of an iterator element. Iterator elements are used
 * to return elements from an iterator.
 *
 * @see      IteratorRequest
 *
 * @version  $Revision: 1.1 $
 * @author   Robert Grimm
 */
public class IteratorElement extends IteratorResponse {

  /**
   * The element from the iterator.
   *
   * @serial
   */
  public Object  element;

  /**
   * Flag to indicate whether the iterator has another element.
   *
   * @serial
   */
  public boolean hasNext;

  /** Create a new, empty iterator element. */
  public IteratorElement() {
    // Nothing to do.
  }

  /**
   * Create a new iterator element.
   *
   * @param   source      The source for the new iterator element.
   * @param   closure     The closure for the new iterator element.
   * @param   element     The element for the new iterator element.
   * @param   hasNext     The hasNext flag for the new iterator element.
   */
  public IteratorElement(EventHandler source, Object closure,
                          Object element, boolean hasNext) {
    super(source,closure);
    this.element = element;
    this.hasNext = hasNext;
  }

}
