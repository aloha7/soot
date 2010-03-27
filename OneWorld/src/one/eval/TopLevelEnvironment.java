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

import java.io.InvalidObjectException;

import java.util.HashMap;
import java.util.Iterator;

/**
 * Implementation of a top-level environment. Like a hash environment,
 * a top-level environment is expected to hold a large number of
 * bindings and is optimized for time using some form of hash table to
 * keep its bindings. However, unlike a hash environment, a top-level
 * environment is specifically designed to serve as the very top-most
 * environment.
 *
 * <p>Top-level environments cannot be created, but, rather, this
 * class defines a publically accessible set of top-level environments
 * with well-known bindings. Top-level environments are always sealed
 * and cannot be modified. Because this class implements a set of
 * predefined environments and top-level environments are always
 * sealed, the serialized representation of a top-level environment is
 * very compact. This compactness is the primary reason for the
 * introduction of top-level environments into <i>eval</i>.</p>
 * 
 * <p>Within the evaluator, a top-level environment should be wrapped
 * by another, modifiable environment, such as a hash
 * environment. This wrapping makes it possible for users to introduce
 * their own bindings, as well as bindings which shadow bindings in
 * the top-level environment.</p>
 *
 * <p>Note that, since top-level environments are not modifiable,
 * access to their bindings is not synchronized.</p>
 *
 * @author   &copy; Copyright 1998-2000 by Robert Grimm.
 *           Written by <a href="mailto:rgrimm@alum.mit.edu">Robert
 *           Grimm</a>.
 * @version  1.0
 */
public final class TopLevelEnvironment implements Environment {

  // ============================ Constants ==============================

  /**
   * The R<sup><font size="-1">5</font></sup>RS null environment. This
   * environment is empty except for the bindings for all syntactic
   * keywords defined by R<sup><font size="-1">5</font></sup>RS.
   */
  public static final TopLevelEnvironment R5RS_NULL;

  /**
   * The R<sup><font size="-1">5</font></sup>RS scheme report
   * environment.  This environment is empty except for the bindings
   * for all syntactic keywords and operators defined by R<sup><font
   * size="-1">5</font></sup>RS.
   */
  public static final TopLevelEnvironment R5RS_REPORT;

  /**
   * The <i>eval</i> environment. This environment contains bindings
   * for all syntactic keywords and operators defined by R<sup><font
   * size="-1">5</font></sup>RS and those defined by <i>eval</i> as
   * extensions to the Scheme standard.
   */
  public static final TopLevelEnvironment EVAL;

  // Static initialization.

  static {
    HashMap             map = new HashMap();
    TopLevelEnvironment env = new TopLevelEnvironment(map);

    // R5RS null.
    Syntax.install(env);

    env.modifiable = false;
    env.name       = "r5rs-null";
    R5RS_NULL      = env;

    // R5RS report.
    map = (HashMap)map.clone();
    env = new TopLevelEnvironment(map);

    Data.install(env);
    Control.install(env);
    Eval.install(env);
    InputOutput.install(env);

    env.modifiable = false;
    env.name       = "r5rs-report";
    R5RS_REPORT    = env;

    // eval.
    map = (HashMap)map.clone();
    env = new TopLevelEnvironment(map);

    SimpleMacro.install(env);
    Runtime.install(env);
    ExtendedInputOutput.install(env);

    env.modifiable = false;
    env.name       = "eval";
    EVAL           = env;
  }

  // ========================== Internal state ===========================

  /**
   * The name of this top-level environment.
   *
   * @serial Must be "<code>r5rs-null</code>", "<code>r5rs-report</code>",
   *         or "<code>eval</code>".
   */
  private           String  name;

  /** The actual mapping for this top-level environment. */
  private transient HashMap map;

  /** Flag to indicate whether this environment is modifiable. */
  private transient boolean modifiable;

  // =========================== Serialization ===========================

  /**
   * Resolve a top-level environment during deserialization.
   *
   * @return      The predefined top-level environment corresponding
   *              to the name of this top-level environment.
   * @throws   InvalidObjectException
   *              Signals that the name of this top-level environment
   *              is not recognized.
   */
  private Object readResolve() throws java.io.ObjectStreamException {
    if ("r5rs-null".equals(name)) {
      return R5RS_NULL;
    } else if ("r5rs-report".equals(name)) {
      return R5RS_REPORT;
    } else if ("eval".equals(name)) {
      return EVAL;
    } else {
      throw new InvalidObjectException("Invalid name \"" + name +
                                       "\" for top-level environment");
    }
  }

  // =========================== Constructors ============================

  /**
   * Create a new top-level environment with the specified hash map
   * as it internal mapping.
   *
   * @param   map  The mapping for the new top-level environment.
   */
  private TopLevelEnvironment(HashMap map) {
    this.map   = map;
    modifiable = true;
  }

  // ============================== Methods ==============================

  /**
   * Return a copy of this top-level environment. The returned
   * environment contains the same bindings as this environment and is
   * not sealed. Since top-level environments are always sealed, this
   * method returns a hash environment with the same bindings as this
   * top-level environment.
   *
   * @return  A copy of this environment.
   */
  public Environment copy() {
    return new HashEnvironment((HashMap)map.clone());
  }

  /**
   * Create a new binding in this top-level environment for the
   * specified symbol, which is impossible.
   *
   * @throws  UnsupportedOperationException
   *             Signals that this environment is sealed.
   */
  public void bind(Symbol s) {
    if (modifiable) {
      if (null == s) {
        throw new NullPointerException("Null symbol");
      } else {
        map.put(s, Environment.Uninitialized.MARKER);
      }
    } else {
      throw new UnsupportedOperationException("Environment sealed");
    }
  }

  /**
   * Create a new binding in this environment for the specified
   * symbol and the specified value, which is impossible.
   *
   * @throws  UnsupportedOperationException
   *             Signals that this environment is sealed.
   */
  public void bind(Symbol s, Object o) {
    if (modifiable) {
      if (null == s) {
        throw new NullPointerException("Null symbol");
      } else {
        map.put(s, o);
      }
    } else {
      throw new UnsupportedOperationException("Environment sealed");
    }
  }

  /**
   * Look up the value of the specified binding.
   *
   * @param   s  The symbol to look up.
   * @return     The value of the specified binding.
   * @throws  BindingException
   *             Signals that the specified symbol is not bound
   *             in this environment.
   */
  public Object lookup(Symbol s) throws BindingException {
    if (map.containsKey(s)) {
      return map.get(s);
    } else {
      throw new BindingException("Symbol not bound", s);
    }
  }

  /**
   * Modify the value of the specified binding, which is impossible.
   *
   * @throws  UnsupportedOperationException
   *             Signals that this environment is sealed.
   */
  public Object modify(Symbol s, Object o) throws BindingException {
    if (map.containsKey(s)) {
      if (modifiable) {
        return map.put(s, o);
      } else {
        throw new UnsupportedOperationException("Environment sealed");
      }
    } else {
      throw new BindingException("Symbol not bound", s);
    }
  }

  /**
   * Return the parent environment for this top-level environment.
   *
   * @return  <code>null</code> as top-level environments have no
   *          parent environment.
   */
  public Environment getParent() {
    return null;
  }

  /**
   * Determine whether the specified symbol is bound in this
   * top-level environment
   *
   * @param   s  The symbol to test for.
   * @return     <code>true</code> iff the symbol is bound in this
   *             environment.
   */
  public boolean isLocallyBound(Symbol s) {
    return map.containsKey(s);
  }    

  /**
   * Determine whether the specified symbol is bound in this
   * top-level environment.
   *
   * @param   s  The symbol to test for.
   * @return     <code>true</code> iff the symbol is bound in this
   *             environment.
   */
  public boolean isBound(Symbol s) {
    return map.containsKey(s);
  }

  /**
   * Seal this top-level environment, which is already sealed.
   */
  public void seal() {
    // Nothing to do.
  }

  /**
   * Unseal this top-level environment, which is impossible.
   */
  public void unseal() {
    // Nothing to do.
  }

  /**
   * Determine whether this environment is sealed.
   *
   * @return  <code>true</code>.
   */
  public boolean isSealed() {
    return (! modifiable);
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
