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
import one.world.binding.LeaseMaintainer;
import one.world.core.EventHandler;
import one.world.core.Event;
import one.world.util.SystemUtilities;
import one.util.Bug;

/**
 * Cache for tuple store bindings.
 *
 * @author  Daniel Cheah
 * @version $Revision: 1.3 $
 */
public class BindingCache extends Cache {

  /** Array of cache entries */
  private final Entry[] entries;

  /** Lookup into the cache entries */
  private final HashMap lookup;

  /**
   * Entry in the binding cache.
   */
  public static final class Entry {
    /** Path of the binding */
    private final String          path;

    /** Binding lease */
    private final LeaseMaintainer lease;

    /** Event handler of the bound resource */
    private final EventHandler    resource;

    /** Reference counter for this entry */
    private int refs;

    /**
     * Constructor. Assumes that the client maintains a reference
     * to the constructed object i.e. reference count is 1.
     *
     * @param path     Path to the binding.
     * @param lease    Binding lease.
     * @param resource Event handler of the bound resource.
     */
    public Entry(String path, LeaseMaintainer lease, EventHandler resource) {
      // Reference count is set to 1, because there will
      // be at least one reference to this object 
      // when it is constructed
      this(path, lease, resource, 1);
    }

    /**
     * Constructor. Allows reference count to be manipulated.
     *
     * @param path     Path to the binding.
     * @param lease    Binding lease.
     * @param resource Event handler of the bound resource.
     * @param refs     Initial value of the reference count.
     */
    protected Entry(String path, LeaseMaintainer lease, EventHandler resource, 
                    int refs) {
      this.path     = path;
      this.lease    = lease;
      this.resource = resource;
      this.refs     = refs;
    }

    /**
     * Passes through the event to the event handler of the bound
     * resource.
     *
     * @param e Event to pass to the event handler of the bound resource.
     */
    public void handle(Event e) {
      if (null != resource) {
        resource.handle(e);
      }
    }

    /**
     * References the cache entry.
     */
    private synchronized void reference() {
      // Sanity check
      if (refs < 0) {
        throw new Bug();
      }

      // one more client is referencing this entry
      ++refs;
    }

    /**
     * Cancels the entry if there are no more references to the entry
     */
    public synchronized void release() {
      // Sanity check
      if (refs == 0) {
        throw new Bug();
      }

      // one less client is referencing this entry
      --refs;

      // no clients we can cancel this lease now
      if (null != lease && 0 == refs) {
        lease.cancel();
      }
    }

    /**
     * Show the details about this cache entry.
     */
    public void showEntry() {
      SystemUtilities.debug("--BindingCache.Entry--");
      SystemUtilities.debug("path " + path);
      SystemUtilities.debug("refs " + refs);
    }
  }

  /**
   * Constructor.
   *
   * @param size The cache size.
   */
  public BindingCache(int size) {
    super(size);

    this.entries = new Entry[size];
    this.lookup  = new HashMap(size);
  }

  /**
   * Add the entry to the cache.
   *
   * @param entry Cache entry to add. 
   */
  public synchronized Entry addEntry(Entry entry) {
    // find a victim to evict 
    int victim = findVictim();
    
    // save the old lease maintainer
    Entry oldEntry = entries[victim];
      
    // replace victim entry
    entries[victim] = entry;

    // add ourselves to the lookup table
    lookup.put(entry.path, getInteger(victim));

    // set the reference bits
    reference(victim);

    // cache now references this entry
    entry.reference();

    // Check that an entry has been created
    if (null != oldEntry) {
      // remove the victim from the lookup table
      lookup.remove(oldEntry.path);

      // cache no longer references this old entry
      oldEntry.release();
    }

    return oldEntry;
  }

  /**
   * Get the entry from the cache.
   *
   * @param path Path of the binding whose entry we want.
   * @return Cache entry, null if entry not found.
   */
  public synchronized Entry getEntry(String path) {
    Integer i = (Integer)lookup.get(path);
    if (null != i) {
      reference(i.intValue());
      entries[i.intValue()].reference();
      return entries[i.intValue()];
    }
    return null;
  }

  /**
   * Cancel all the leases.
   */
  public synchronized void cancelLeases() {
    for (int i = 0 ; i < entries.length; i++) {
      if (null != entries[i] && null != entries[i].lease) {
        entries[i].lease.cancel();
      }
    }
  }

  /**
   * Print out the entries in the cache.
   */
  public void showEntries() {
    SystemUtilities.debug("--Entries in BindingCache--");
    for (int i = 0; i < entries.length; ++i) {
      SystemUtilities.debug(i + " " + entries[i]);
    }
  }
}
