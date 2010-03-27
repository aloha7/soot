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

import java.util.HashMap;
import one.world.core.Tuple;
import one.world.util.SystemUtilities;
import one.util.Bug;

/**
 * Cache for tuples.
 *
 * @author  Daniel Cheah
 * @version $Revision: 1.3 $
 */
public class TupleCache extends Cache {

  /** Cache entrires */
  private final Entry[] entries;

  /** Lookup table to find the correct cache entries. */
  private final HashMap lookup;

  /**
   * Creates a new instance.
   *
   * @param size The size (in entries) of the cache.
   */
  public TupleCache(int size) {
    super(size);
    entries = new Entry[size];
    lookup  = new HashMap(size);
  }

  /**
   * Adds an entry to the cache. Evicts an old entry if necessary.
   *
   * @param entry Entry to add to the cache.
   *
   * @return Entry that was evicted, null if no entry was evicted.
   */
  public synchronized Entry addEntry(Entry entry) {
    // find a victim to evict 
    int victim = findVictim();
    
    // save the old lease maintainer
    Entry oldEntry = entries[victim];

    // replace victim entry
    entries[victim] = entry;
      
    // add ourselves to the lookup table
    lookup.put(entry.uri, getInteger(victim));

    // set the reference bits
    reference(victim);

    // Check that an entry has been created
    if (null != oldEntry) {
      // remove the victim from the lookup table
      lookup.remove(oldEntry.uri);
    }

    return oldEntry;
  }

  /**
   * Get an entry from the cache.
   *
   * @param uri URI of the tuple we are trying to find in the cache.
   *
   * @return Entry that was found in the cache, null if no entry was found.
   */
  public synchronized Entry getEntry(String uri) {
    Integer i = (Integer)lookup.get(uri);
    if (null != i) {
      reference(i.intValue());
      return entries[i.intValue()];
    }
    return null;
  }

  /**
   * Print out the entries in the cache.
   */
  public void showEntries() {
    SystemUtilities.debug("--Entries in TupleCache--");
    for (int i = 0; i < entries.length; ++i) {
      SystemUtilities.debug(i + " " + entries[i]);
    }
  }

  /**
   * Entry in the tuple cache.
   */
  public static final class Entry {

    /** URI of the tuple we have cached in this entry. */
    public final String uri;

    /** Tuple we have stored in this cache entry. */
    public final Tuple  tuple;

    /**
     * Creates a new instance.
     *
     * @param tuple     Tuple we want stored in the cache.
     * @param uri       URI of the tuple.
     */
    public Entry(Tuple tuple, String uri) {
      this.tuple = tuple;
      this.uri   = uri;
    }

    /**
     * Show the details about this cache entry.
     */
    public void showEntry() {
      SystemUtilities.debug("--TupleCache.Entry--");
      SystemUtilities.debug("uri   " + uri);
      SystemUtilities.debug("tuple " + tuple);
    }
  }
}
