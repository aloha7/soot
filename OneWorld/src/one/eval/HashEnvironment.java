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

import java.util.HashMap;

/**
 * Implementation of a hash environment. A hash environment is
 * expected to be hold a large number of bindings and is optimized for
 * time by internally using some form of hash table to keep its
 * bindings.
 *
 * @author   &copy; Copyright 1998-2000 by Robert Grimm.
 *           Written by <a href="mailto:rgrimm@alum.mit.edu">Robert
 *           Grimm</a>.
 * @version  1.0
 */
public final class HashEnvironment implements Environment {

  // ========================== Internal state ===========================

  /**
   * The actual mapping for this environment.
   *
   * @serial Must not be <code>null</code>.
   */
  private HashMap     map;

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
   * Write the state of this hash environment to a stream, that is,
   * serialize it.
   *
   * @serialData  The default fields while holding the monitor for
   *              <code>map</code>, followed by the parent environment,
   *              <code>parent</code>, which is transient so that it can
   *              be serialized without holding any locks.
   */
  private void writeObject(ObjectOutputStream out) throws IOException {
    // map, sealed.
    synchronized(map) {
      out.defaultWriteObject();
    }
    out.writeObject(parent);
  }

  /**
   * Read the state of this hash environment from a stream, that is,
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
   * Create a new hash environment with the specified hash
   * map as its internal mapping.
   *
   * @param   map     The hash map for the new environment.
   */
  HashEnvironment(HashMap map) {
    this.map    = map;
  }

  /**
   * Create a new empty hash environment.
   */
  public HashEnvironment() {
    map    = new HashMap();
  }

  /**
   * Create a new empty hash environment with the specified parent
   * environment.
   *
   * @param  env  The parent environment for the new hash
   *              environment.
   */
  public HashEnvironment(Environment env) {
    map    = new HashMap();
    parent = env;
  }

  // ============================== Methods ==============================

  /**
   * Return a copy of this environment. The returned environment
   * contains the same local bindings as this environment and is not
   * sealed.
   *
   * @return  A copy of this environment.
   */
  public Environment copy() {
    HashMap m;
    synchronized(map) {
      m = (HashMap)this.map.clone();
    }

    HashEnvironment env = new HashEnvironment(m);
    env.parent          = this.parent;

    return env;
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
      synchronized(map) {
        if (sealed) {
          throw new UnsupportedOperationException("Environment is sealed");
        }

        map.put(s, Environment.Uninitialized.MARKER);
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
      synchronized(map) {
        if (sealed) {
          throw new UnsupportedOperationException("Environment is sealed");
        }

        map.put(s, o);
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
    synchronized(map) {
      if (map.containsKey(s)) {
        return map.get(s);
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
   *             bound in this environment.
   * @throws  UnsupportedOperationException
   *             Signals that this environment is sealed.
   */
  public Object modify(Symbol s, Object o) throws BindingException {
    synchronized(map) {
      if (map.containsKey(s)) {
        if (sealed) {
          throw new UnsupportedOperationException("Environment is sealed");
        } else {
          return map.put(s, o);
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
   *          <code>null</code> if this environment has
   *          no parent environment.
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
    synchronized(map) {
      return map.containsKey(s);
    }
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
   * Seal this hash environment. After this hash environment has been
   * sealed, all <code>bind</code> operations on this environment and
   * all <code>modify</code> operations on bindings local to this
   * environment will fail with an
   * <code>UnsupportedOperationException</code>, unless this
   * environment is unsealed again.
   */
  public void seal() {
    synchronized(map) {
      sealed = true;
    }
  }

  /**
   * Unseal this hash environment. This environment is unsealed and
   * the bindings local to this environment are modifiable again.
   */
  public void unseal() {
    synchronized(map) {
      sealed = false;
    }
  }

  /**
   * Determine whether this environment is sealed.
   *
   * @return  <code>true</code> iff this environment is sealed.
   */
  public boolean isSealed() {
    synchronized(map) {
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
