/*
 * Copyright (c) 1999, 2000, Robert Grimm.
 *    All rights reserved.
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
 * 3. Neither name of Robert Grimm nor the names of his contributors
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

package one.util;

import java.util.ArrayList;

/**
 * Implementation of a reference list. A reference list is an ordered
 * list of objects or, more precisely, object references where a
 * particular object reference appears in the list at most once. This
 * class simply provides the minimal necessary functionality to
 * implement such a reference list on top of an array list. However,
 * it does not enforce the invariant that a particular object
 * reference should appear in a reference list at most once.
 * 
 * @author   &copy; Copyright 1999 by Robert Grimm.
 *           Written by <a href="mailto:rgrimm@alum.mit.edu">Robert
 *           Grimm</a>.
 * @version  1.0
 */
public class ReferenceList extends ArrayList {

  /** Create a new reference list. */
  public ReferenceList() {
    super();
  }

  /**
   * Determine the index of the specified object within this
   * reference list. Uses <code>==</code> to compare the specified
   * object against the objects in this reference list. The list
   * is searched starting from index 0.
   *
   * @param   o  The object to search for.
   * @return     The index of the specified object within this
   *             reference list, or -1 if the object does not appear
   *             in this reference list.
   */
  public int find(Object o) {
    int l = size();

    for (int i=0; i<l; i++) {
      if (get(i) == o) {
        return i;
      }
    }

    return -1;
  }

}
