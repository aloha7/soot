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

package one.eval;

import java.lang.ref.WeakReference;
import java.lang.ref.ReferenceQueue;

import java.io.ObjectStreamException;

import java.util.HashMap;

/**
 * Implementation of a symbol. A symbol simply encapsulates a Java
 * string. The implementation uses an internal symbol table to ensure
 * that two symbols <code>s1</code> and <code>s2</code> are equal (in
 * any sense of Scheme or Java equality) iff <code>s1 == s2</code>.
 * The implementation of the symbol table is thread-safe.
 *
 * @author   &copy; Copyright 1998-2000 by Robert Grimm.
 *           Written by <a href="mailto:rgrimm@alum.mit.edu">Robert
 *           Grimm</a>.
 * @version  1.0
 */
public final class Symbol implements java.io.Serializable {

  /*
   * Symbol uses an internal symbol table to ensure the property that
   * two symbols s1 and s2 are equal iff s1 == s2. The symbol table is
   * represented as a java.util.HashMap where the keys are Java
   * strings (the symbol names) and the values are weak references of
   * the inner class TableEntry, where the weak reference encapsulates
   * a Symbol object. The symbol table uses weak references to ensure
   * that unreachable symbols can be properly reclaimed by the garbage
   * collector even if they are still present in the symbol table. A
   * symbol table entry keeps a pointer to the symbol's name (the key
   * into the hash map) to facilitate removal of symbols once they
   * become unreachable. All pointers to strings are strong and can
   * only become unreachable after the corresponding symbol table has
   * been cleaned up.
   *
   * The implementation relies on the following invariances:
   *   (1) A symbol's name must not be null.
   *   (2) A symbol's name is an interned Java string.
   *   (3) All values in the symbol table are of class
   *       TableEntry.
   */

  // ========================== Symbol Table Entry =========================

  /** Implementation of a symbol table entry. */
  static class TableEntry extends WeakReference {

    /** The name of the symbol. */
    String name;

    /**
     * Create a new symbol table entry for the specified symbol.
     *
     * @param  s  The symbol.
     */
    TableEntry(Symbol s) {
      super(s);
      name = s.toString();
    }
  
    /**
     * Create a new symbol table entry for the specified symbol and
     * register the new weak reference with the specified reference
     * queue.
     *
     * @param  s  The symbol.
     * @param  q  The reference queue.
     */
    TableEntry(Symbol s, ReferenceQueue q) {
      super(s, q);
      name = s.toString();
    }
  }

  // ============================= Symbol Table ============================

  /** The symbol table. */
  private static HashMap symbolTable = new HashMap();

  /** The reference queue for reclaiming unused entries. */
  private static ReferenceQueue queue = new ReferenceQueue();

  /**
   * Process current entries in the reference queue. The caller must
   * hold a lock on the symbol table.
   */
  private static void processQueue() {
    TableEntry e;
    while (null != (e = (TableEntry)queue.poll())) {
      /*
       * To avoid race conditions between the garbage collection
       * mechanism (including its support for weak references) and the
       * creation of new symbols of the same name, only remove a
       * symbol table entry if it is in fact the same.
       */
      if (symbolTable.get(e.name) == e) {
        symbolTable.remove(e.name);
      }
    }
  }

  /**
   * Intern a symbol with the specified name. The symbol returned for
   * repeated invocations with the same Java string <code>name</code>
   * or with different Java strings <code>name1</code> and
   * <code>name2</code>, where
   * <code>name1.equals(name2)&nbsp;==&nbsp;true</code>, is always the
   * same object, unless that symbol became unreachable in the runtime
   * and was garbage collected, in which case a new symbol is created.
   *
   * @param   name  The symbol's name.
   * @return        The corresponding symbol.
   * @throws  java.lang.NullPointerException
   *                Signals that <code>null&nbsp;==&nbsp;name</code>.
   */
  public static Symbol intern(String name) {
    /*
     * Internalize name. Ensures invariance 1 (by dereferencing name)
     * and 2 (by interning it).
     */
    name = name.intern();

    synchronized(symbolTable) {
      processQueue(); /* Remove useless entries. */

      Symbol s;
      TableEntry e = (TableEntry)symbolTable.get(name);
      if ((null != e) && (null != (s = (Symbol)e.get()))) {
	return s;
      }

      /* Need to create a new symbol. */
      s = new Symbol(name);
      e = new TableEntry(s, queue);
      symbolTable.put(name, e); /* We only add here. Ensures invariance 3. */

      return s;
    }
  }

  // =========================== Internal State ============================

  /**
   * The symbol's name.
   *
   * @serial The string representing the symbol's name. For an actual
   *         symbol object, <code>name</code> must point to an
   *         interned Java string. Furthermore, for any given name,
   *         only one symbol object may exist in a given Java
   *         runtime. To ensure these two invariances, class Symbol
   *         implements its own <code>readResolve()</code> method
   *         which correctly interns the Java string representing a
   *         symbol's name and ensures that the symbol object is
   *         unique.
   */
  private String name;

  // ============================ Serialization ============================

  /**
   * Resolve a symbol during deserialization by interning it.
   *
   * @return  The unique symbol corresponding to this' name.
   */
  private Object readResolve() throws ObjectStreamException {
    return intern(this.name);
  }

  // ============================= Constructor =============================

  /** Create a new symbol with a given name. */
  private Symbol(String name) {
    this.name = name;
  }

  // =============================== Methods ===============================

  /**
   * Return a string representation of this symbol. Simply returns
   * this symbol's name.
   *
   * @return  A string representing this symbol.
   */
  public String toString() {
    return name;
  }

  // ================================ Casts ================================

  /**
   * Cast the specified object to a symbol.
   * 
   * @param   o  The object to cast.
   * @return     The specified object as a symbol.
   * @throws  BadTypeException
   *             Signals that <code>o</code> is not a symbol.
   */
  public static Symbol toSymbol(Object o) throws BadTypeException {
    if (o instanceof Symbol) {
      return (Symbol)o;
    } else {
      throw new BadTypeException("Not a symbol", o);
    }
  }

}
