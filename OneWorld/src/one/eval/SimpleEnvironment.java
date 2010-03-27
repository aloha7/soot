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

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;

import one.util.NegativeSizeException;

/**
 * Implementation of a simple environment. A simple environment is
 * expected to only hold a few bindings and is optimized for space and
 * not time.
 *
 * @author   &copy; Copyright 1998-2000 by Robert Grimm.
 *           Written by <a href="mailto:rgrimm@alum.mit.edu">Robert
 *           Grimm</a>.
 * @version  1.0
 */
public final class SimpleEnvironment implements Environment {

  // ========================== Internal state ===========================

  /** The initial size of a simple environment. */
  private static final int INITIAL_SIZE = 5;

  /** The size increment of a simple environment. */
  private static final int SIZE_INCR    = 5;

  /**
   * The number of valid entries in this environment.
   *
   * @serial  Must not be negative.
   */
  private int length;

  /**
   * The array of symbols for bindings in this environment.
   *
   * @serial  Must have exactly <code>length</code>
   *          non-null entries.
   */
  private Symbol[] symbols;

  /**
   * The array of values for bindings in this environment.
   *
   * @serial  Must have exactly <code>length</code> entries.
   */
  private Object[] values;

  /**
   * Flag to indicate whether this environment is sealed.
   *
   * @serial <code>true</code> iff this environment is sealed.
   */
  private boolean     sealed;

  /**
   * The parent environment of this environment. This field is
   * transient so that it can be serialized without holding any locks.
   */
  private transient Environment parent;

  // =========================== Serialization ===========================

  /**
   * Write the state of this simple environment to a stream, that is,
   * serialize it.
   *
   * @serialData  The default fields while holding the monitor for
   *              <code>symbols</code>, followed by the parent environment,
   *              <code>parent</code>, which is transient so that it can
   *              be serialized without holding any locks.
   */
  private void writeObject(ObjectOutputStream out) throws IOException {
    // length, symbols, values, sealed.
    synchronized(symbols) {
      out.defaultWriteObject();
    }
    out.writeObject(parent);
  }

  /**
   * Read the state of this simple environment from a stream, that is,
   * deserialize it.
   *
   * @serialData  The default fields, followed by the parent environment,
   *              <code>parent</code>.
   */
  private void readObject(ObjectInputStream in)
    throws IOException, ClassNotFoundException {

    in.defaultReadObject();
    parent = (Environment)in.readObject();
  }

  // =========================== Constructors ============================

  /**
   * Create a new simple environment with the specified length and
   * arrays of symbols and values.
   *
   * @param   length   The number of valid entries in the new
   *                   environment.
   * @param   symbols  The array of symbols for bindings in the
   *                   new environment, which is at least as
   *                   long as <code>length</code> and contains
   *                   at least <code>length</code>
   *                   leading entries that are not
   *                   <code>null</code>.
   * @param   values   The array of values for bindings in the
   *                   new environment, which has the same
   *                   length as <code>symbols</code>.
   */
  private SimpleEnvironment(int length, Symbol[] symbols, Object[] values) {
    this.length  = length;
    this.symbols = symbols;
    this.values  = values;
  }

  /**
   * Create a new simple environment with no parent environment.
   */
  public SimpleEnvironment() {
    length  = 0;
    symbols = new Symbol[INITIAL_SIZE];
    values  = new Object[INITIAL_SIZE];
  }

  /**
   * Create a new simple environment with the specified environment
   * as its parent environment.
   *
   * @param   env  The parent environment for the new environment.
   */
  public SimpleEnvironment(Environment env) {
    this();
    parent = env;
  }

  /**
   * Create a new simple environment with the specified environment
   * as its parent environment and the specified initial capacity.
   *
   * @param   env  The parent environment for the new environment.
   * @param   capacity
   *               The initial capacity for this environment.
   * @throws  NegativeSizeException
   *               Signals that <code>capacity < 0</code>.
   */
  public SimpleEnvironment(Environment env, int capacity) {
    if (0 > capacity) {
      throw new NegativeSizeException("Negative capacity: " + capacity);
    }
    length  = 0;
    symbols = new Symbol[capacity];
    values  = new Object[capacity];
    parent  = env;
  }

  // ============================== Methods ==============================

  /**
   * Return a copy of this environment. The returned environment
   * contains the same local bindings as this environment, has the
   * same parent environment as this environment and is not sealed.
   *
   * @return  A copy of this environment.
   */
  public Environment copy() {
    int      l;
    Symbol[] s;
    Object[] v;

    synchronized(symbols) {
      l = length;
      s = (Symbol[])symbols.clone();
      v = (Symbol[])values.clone();
    }

    SimpleEnvironment env = new SimpleEnvironment(l, s, v);
    env.parent            = this.parent;

    return env;
  }

  /**
   * Ensure that there is enough space in the internal data structures
   * to add another binding, and if not make some. Caller must hold
   * the lock for this environment.
   */
  private void ensureSpace() {
    if (length < symbols.length) {
      return;
    } else {
      Symbol[] s = new Symbol[symbols.length + SIZE_INCR];
      Object[] v = new Object[symbols.length + SIZE_INCR];

      System.arraycopy(symbols, 0, s, 0, length);
      System.arraycopy(values,  0, v, 0, length);
      
      symbols = s;
      values  = v;
    }
  }

  /**
   * Create a new binding in this environment for the specified
   * symbol.
   *
   * @param   s  The symbol to bind.
   * @throws  UnsupportedOperationException
   *             Signals that this environment is sealed.
   * @throws  NullPointerException
   *             Signals that <code>null == s</code>.
   */
  public void bind(Symbol s) {
    if (null == s) {
      throw new NullPointerException("Null symbol");
    } else {
      synchronized(symbols) {
        if (sealed) {
          throw new UnsupportedOperationException("Environment is sealed");
        }

        for (int i=0; i<length; i++) {
          if (symbols[i] == s) {
            values[i] = Environment.Uninitialized.MARKER;
            return;
          }
        }

        ensureSpace();
        symbols[length] = s;
        values[length]  = Environment.Uninitialized.MARKER;
        length++;
      }
    }
  }

  /**
   * Create a new binding in this environment for the specified
   * symbol and the specified value.
   *
   * @param   s  The symbol to bind.
   * @param   o  The value of the binding.
   * @throws  UnsupportedOperationException
   *             Signals that this environment is sealed.
   * @throws  NullPointerException
   *             Signals that <code>null == s</code>.
   */
  public void bind(Symbol s, Object o) {
    if (null == s) {
      throw new NullPointerException("Null symbol");
    } else {
      synchronized(symbols) {
        if (sealed) {
          throw new UnsupportedOperationException("Environment is sealed");
        }

        for (int i=0; i<length; i++) {
          if (symbols[i] == s) {
            values[i] = o;
            return;
          }
        }

        ensureSpace();
        symbols[length] = s;
        values[length]  = o;
        length++;
      }
    }
  }

  /**
   * Look up the value of the specified binding.
   *
   * @param   s  The symbol to look up.
   * @return     The value of the specified binding.
   * @throws  BindingException
   *             Signals that the specified symbol is not bound
   *             in this environment or any of its ancestral
   *             environments.
   */
  public Object lookup(Symbol s) throws BindingException {
    synchronized(symbols) {
      for (int i=0; i<length; i++) {
        if (symbols[i] == s) {
          return values[i];
        }
      }
    }

    if (null != parent) {
      return parent.lookup(s);
    } else {
      throw new BindingException("Symbol not bound", s);
    }
  }

  /**
   * Modify the value of the specified binding.
   *
   * @param   s  The symbol for the binding.
   * @param   o  The new value for the specified binding.
   * @return     The old value for the specified binding, or
   *             {@link Environment.Uninitialized#MARKER} if
   *             the binding not been initialized.
   * @throws  BindingException
   *             Signals that the specified symbol is not
   *             bound in this environment or any of its
   *             ancestral environments.
   * @throws  UnsupportedOperationException
   *             Signals that the specified symbol is
   *             bound in this environment and this environment
   *             is sealed.
   */
  public Object modify(Symbol s, Object o) throws BindingException {
    synchronized(symbols) {
      for (int i=0; i<length; i++) {
        if (symbols[i] == s) {
          if (sealed) {
            throw new UnsupportedOperationException("Environment is sealed");
          } else {
            Object o2 = values[i];
            values[i] = o;
            return o2;
          }
        }
      }
    }
    
    if (null != parent) {
      return parent.modify(s, o);
    } else {
      throw new BindingException("Symbol not bound", s);
    }
  }

  /**
   * Return the parent environment for this environment.
   *
   * @return  The parent environment for this environment, or
   *          <code>null</code> if this environment has no
   *          parent environment.
   */
  public Environment getParent() {
    return parent;
  }

  /**
   * Determine whether the specified symbol is bound in this
   * environment
   *
   * @param   s  The symbol to test for.
   * @return     <code>true</code> iff the symbol is bound in this
   *             environment.
   */
  public boolean isLocallyBound(Symbol s) {
    synchronized(symbols) {
      for (int i=0; i<length; i++) {
        if (symbols[i] == s) {
          return true;
        }
      }
    }

    return false;
  }    

  /**
   * Determine whether the specified symbol is bound in this
   * environment.
   *
   * @param   s  The symbol to test for.
   * @return     <code>true</code> iff the symbol is bound in this
   *             environment.
   */
  public boolean isBound(Symbol s) {
    if (isLocallyBound(s)) {
      return true;
    } else if (null != parent) {
      return parent.isBound(s);
    } else {
      return false;
    }
  }

  /**
   * Seal this simple environment. After this simple environment has
   * been sealed, all <code>bind</code> operations on this environment
   * and all <code>modify</code> operations on bindings local to this
   * environment will fail with an
   * <code>UnsupportedOperationException</code>, unless this
   * environment is unsealed again.
   */
  public void seal() {
    synchronized(symbols) {
      sealed = true;
    }
  }

  /**
   * Unseal this simple environment. This environment is unsealed and
   * the bindings local to this environment are modifiable again.
   */
  public void unseal() {
    synchronized(symbols) {
      sealed = false;
    }
  }

  /**
   * Determine whether this environment is sealed.
   *
   * @return  <code>true</code> iff this environment is sealed.
   */
  public boolean isSealed() {
    synchronized(symbols) {
      return sealed;
    }
  }

  /**
   * Return a string representation of this environment.
   *
   * @return  A string representation of this environment.
   */
  public String toString() {
    return "#[environment " + super.toString() + "]";
  }

}
