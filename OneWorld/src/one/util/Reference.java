/*
 * Copyright (c) 1999, 2000, University of Washington, Department of
 * Computer Science and Engineering.
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

package one.util;

/**
 * Implementation of a reference. This class wraps an object and
 * provides a <code>hashCode()</code> method based on the object's
 * identity hash code and <code>equals()</code> method based on
 * pointer equality instead of the notions of hash code and equality
 * implemented by the object.
 * 
 * @version  $Revision: 1.1 $
 * @author   Robert Grimm
 */
public class Reference implements java.io.Serializable {

  /** 
   * The object for this reference.
   *
   * @serial
   */
  private final Object obj;
  
  /**
   * Create a new reference for the specified object.
   *
   * @param  The object for the new reference.
   */
  public Reference(Object obj) {
    this.obj = obj;
  }
  
  /**
   * Get the object for this reference.
   *
   * @return  The object for this reference.
   */
  public final Object get() {
    return obj;
  }
  
  /** Get a hash code for this reference. */
  public final int hashCode() {
    return System.identityHashCode(obj);
  }
  
  /** Determine whether this reference equals the specified object. */
  public final boolean equals(Object o) {
    if (this == o) return true;
    if (! (o instanceof Reference)) return false;
    return (obj == ((Reference)o).obj);
  }

}
