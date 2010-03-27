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

/**
 * Definition of an environment. An environment provides both
 * syntactic and variable bindings. Both map a symbol, representing a
 * syntactic keyword for a syntactic binding and a variable's name for
 * a variable binding. to some value represented by a Java object.
 *
 * <p>Environments support three primary operations:<ol>
 * <li><code>bind</code>: Establishes a binding for a symbol. If no
 * value is specified for a binding at bind time, it is initialized to
 * {@link Uninitialized#MARKER}.</li> <li><code>lookup</code>: Looks up
 * the value of a binding.</li> <li><code>modify</code>: Changes the
 * value of a binding.</li></ol></p>
 *
 * <p>An implementation of this interface should provide at least two
 * constructors, namely a no argument constructor and one that takes
 * another environment as its only argument. If another environment is
 * supplied as an argument to a constructor, that environment becomes
 * the newly created environment's parent environment. Such a parent
 * environment may have a parent environment of its own, leading to a
 * chain of ancestral environments that ends with some top-level
 * environment.</p>
 *
 * <p>If an environment has no binding for a symbol when trying to
 * perform a <code>lookup</code> or <code>modify</code> operation, but
 * has a parent environment, it must return the result of invoking the
 * method of the same signature on the parent environment.
 * <code>bind</code> operations are never propagated this way.</p>
 *
 * <p>Environments may be sealed by calling the {@link #seal()}
 * method. After an environment has been sealed, the bindings local to
 * that environment cannot be changed, unless it is unsealed again by
 * calling {@link #unseal()}. A sealed environment must throw a
 * <code>UnsupportedOperationException</code> on <code>bind</code>
 * operations and on <code>modify</code> operations that would modify
 * a binding local to that environment. The {@link #isSealed()} method
 * can be used to determine whether a given environment is sealed.</p>
 * 
 * <p>Note that all bindings in ancestral environments of a sealed
 * environment must be modifiable, unless the ancestral environment is
 * also sealed. In particular, a <code>modify</code> operation that
 * has been propagated to an unsealed ancestral environment, which has
 * a binding for the specified symbol and is not sealed, must
 * succeed.</p>
 *
 * <p>Access to the bindings of a particular instance of an
 * implementation of this interface must be atomic, that is,
 * synchronized. At the same time, an implementation of this interface
 * must not hold any locks while deferring an operation to a parent
 * environment.</p>
 *
 * <p>Environments are serializable but may contain references to
 * unserializable objects, such as input and output
 * ports. Consequently, a particular instance of an implementation of
 * this interface may not be serializable, depending on the bindings
 * it or one of its ancestral envrionments contains.</p>
 *
 * <p>Note that symbols must not be <code>null</code> and an
 * implementation of this interface must throw a
 * <code>NullPointerException</code> when a <code>null</code> symbol
 * is passed to a <code>bind</code> operation.</p>
 *
 * @author   &copy; Copyright 1998-2000 by Robert Grimm.
 *           Written by <a href="mailto:rgrimm@alum.mit.edu">Robert
 *           Grimm</a>.
 * @version  1.0
 */
public interface Environment extends java.io.Serializable {

  /** The uninitialized object. */
  class Uninitialized
    implements java.io.Serializable, NotAValue {

    /**
     * The uninitialized object, which is guaranteed to be unique
     * within a given Java Virtual Machine. Implementations of the
     * <code>Environment</code> interface must use this object as the
     * "value" for uninitialized bindings.
     */
    public static final Uninitialized MARKER = new Uninitialized();

    /** Create a new uninitialized object. */
    private Uninitialized() {
      // Nothing to construct.
    }

    /**
     * Resolve the uninitialized object during deserialization.
     *
     * @return  <code>MARKER</code>.
     */
    private Object readResolve() throws java.io.ObjectStreamException {
      return MARKER;
    }
    
    /**
     * Return a string representing this uninitialized object.
     *
     * @return  "<code>#[Uninitialized-Binding]</code>".
     */
    public String toString() {
      return "#[uninitialized-binding]";
    }
  }
  
  /**
   * Return a copy of this environment, that has the same local
   * bindings and parent environment as this environment, but that is
   * not sealed, independent of whether this environment is sealed or
   * not.
   *
   * @return  A new environment with the same local bindings and
   *          parent environment as this environment, but that is
   *          not sealed.
   */
  Environment copy();

  /**
   * Create a new binding in this environment for the specified symbol
   * with {@link Uninitialized#MARKER} as its value. If a binding for
   * this symbol already exists in this environment, it is replaced by
   * the specified binding.
   *
   * @param   s  The symbol to bind.
   * @throws  UnsupportedOperationException
   *             Signals that this environment is sealed.
   * @throws  NullPointerException
   *             Signals that <code>null == s</code>.
   */
  void bind(Symbol s);

  /**
   * Create a new binding in this environment for the specified
   * symbol and the specified value. If a binding for this symbol
   * already exists in this environment, it is replaced by the
   * specified binding.
   *
   * @param   s  The symbol to bind.
   * @param   o  The value of the binding.
   * @throws  UnsupportedOperationException
   *             Signals that this environment is sealed.
   * @throws  NullPointerException
   *             Signals that <code>null == s</code>.
   */
  void bind(Symbol s, Object o);

  /**
   * Look up the value of the specified binding.
   *
   * @param   s  The symbol to look up.
   * @return     The value of the specified binding, or
   *             {@link Environment.Uninitialized#MARKER} if the
   *             specified symbol is bound in this environment
   *             or any of its ancestral environments, but its
   *             value has not yet been initialized.
   * @throws  BindingException
   *             Signals that the symbol is not bound in this
   *             environment nor any of its ancestral environments.
   */
  Object lookup(Symbol s) throws BindingException;

  /**
   * Modify the value of the specified binding.
   *
   * @param   s  The symbol for the binding.
   * @param   o  The new value for the specified binding.
   * @return     The old value for the specified binding, or
   *             {@link Uninitialized#MARKER} if the binding
   *             was not initialized before.
   * @throws  BindingException
   *             Signals that the specified symbol is not
   *             bound in this environment or any of its
   *             ancestral environments.
   * @throws  UnsupportedOperationException
   *             Signals that that specified symbol is
   *             bound in this environment, but this environment
   *             is sealed.
   */
  Object modify(Symbol s, Object o) throws BindingException;

  /**
   * Return the parent environment for this environment.
   *
   * @return  The parent environment for this environment, or
   *          <code>null</code> if this environment has no
   *          parent environment.
   */
  Environment getParent();

  /**
   * Determine whether the specified symbol is bound in this
   * environment
   *
   * @param   s  The symbol to test for.
   * @return     <code>true</code> iff the symbol is bound in this
   *             environment.
   */
  boolean isLocallyBound(Symbol s);

  /**
   * Determine whether the specified symbol is bound in this
   * environment or any of its ancestral environments.
   *
   * @param   s  The symbol to test for.
   * @return     <code>true</code> iff the symbol is bound in this
   *             environment or any of its ancestral environments.
   */
  boolean isBound(Symbol s);

  /**
   * Seal this environment. After this environment has been sealed,
   * all <code>bind</code> operations on this environment and all
   * <code>modify</code> operations on bindings local to this
   * environment must fail with an
   * <code>UnsupportedOperationException</code>, unless the
   * environment is unsealed again. Calling this method on an already
   * sealed environment has no effect and <code>null</code> is
   * returned.
   *
   * @see     #unseal()
   */
  void seal();

  /**
   * Unseal this environment. If this environment has been sealed
   * before, it is unsealed and the bindings local to this environment
   * are modifiable again. Calling this method on an unsealed
   * environment has no effect.
   *
   * @see     #seal()
   */
  void unseal();

  /**
   * Determine whether this environment is sealed.
   *
   * @return  <code>true</code> iff this environment is sealed.
   */
  boolean isSealed();

  /**
   * Return a string representation of this environment of the form
   * <blockquote><pre>
   * #[environment &lt;<i>name</i>&gt;]
   * </pre></blockquote>
   *
   * @return  A string representation of this environment.
   */
  String toString();

}
