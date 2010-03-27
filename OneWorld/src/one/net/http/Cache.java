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

import java.util.BitSet;

/**
 * Implementation of LRU approximation cache mechanism. Depends
 * on derived classes to do real work.
 *
 * @author  Daniel Cheah
 * @version $Revision: 1.3 $
 */
public abstract class Cache implements java.io.Serializable {
  private final BitSet    refbits;
  private final int       size;
  private final Integer[] integer;
  private       int       counter;

  /**
   * Constructor.
   *
   * @param size Size of the cache.
   */
  public Cache(int size) {
    this.size    = size;
    this.refbits = new BitSet(size);
    this.integer = new Integer[size];
    this.counter = 0;

    for (int i = 0; i < size; i++) {
      refbits.clear(i);
      integer[i] = new Integer(i);
    }
  }

  /**
   * Finds a victim to evict from the cache.
   *
   * @return Cache entry to evict.
   */
  public int findVictim() {
    int victim = -1;
    int start  = counter;

    while (victim == -1) {
      if (refbits.get(counter) == false) {
        victim = counter;
      } else {
        refbits.clear(counter);
      }

      // next entry
      counter++;

      // if past last element, wrap around
      if (counter == size) {
        counter = 0;
      }
    }

    return victim;
  }

  /**
   * Reference the LRU bit in the cache.
   *
   * @param entry Cache entry to reference. 
   */
  public void reference(int entry) {
    refbits.set(entry);
  }

  /** 
   * Get LRU bit from the cache.
   *
   * @param entry Reference bit of this Cache entry.
   */
  public boolean get(int entry) {
    return refbits.get(entry);
  }

  /**
   * Get the cache size.
   *
   * @return Size of the cache.
   */
  public int size() {
    return size;
  }

  /**
   * Converts int type to Integer object.
   * Useful utility functions for child
   * classes.
   * 
   * @param i Integer object we are interested in.
   * @return Integer object corrosponding to the int param.
   */
  public Integer getInteger(int i) {
    return integer[i];
  }
}
